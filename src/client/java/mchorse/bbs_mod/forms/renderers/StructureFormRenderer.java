package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Renderer de StructureForm
 *
 * Implementa carga NBT y renderizado básico iterando bloques.
 * Para minimizar archivos, el loader de NBT está integrado aquí.
 */
public class StructureFormRenderer extends FormRenderer<StructureForm>
{
    private static class BlockEntry
    {
        final BlockState state;
        final BlockPos pos;

        BlockEntry(BlockState state, BlockPos pos)
        {
            this.state = state;
            this.pos = pos;
        }
    }

    private final List<BlockEntry> blocks = new ArrayList<>();
    private String lastFile = null;
    private BlockPos size = BlockPos.ORIGIN;
    private BlockPos boundsMin = null;
    private BlockPos boundsMax = null;

    public StructureFormRenderer(StructureForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.getContext().draw();

        ensureLoaded();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        MatrixStack matrices = context.batcher.getContext().getMatrices();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        /* Autoescala: ajustar para que la estructura quepa en la celda sin recortarse */
        float cellW = x2 - x1;
        float cellH = y2 - y1;
        float baseScale = cellH / 2.5F; /* igual que en ModelFormRenderer#getUIMatrix */

        int wUnits = 1, hUnits = 1, dUnits = 1;
        if (this.boundsMin != null && this.boundsMax != null)
        {
            wUnits = Math.max(1, this.boundsMax.getX() - this.boundsMin.getX() + 1);
            hUnits = Math.max(1, this.boundsMax.getY() - this.boundsMin.getY() + 1);
            dUnits = Math.max(1, this.boundsMax.getZ() - this.boundsMin.getZ() + 1);
        }
        else
        {
            wUnits = Math.max(1, this.size.getX());
            hUnits = Math.max(1, this.size.getY());
            dUnits = Math.max(1, this.size.getZ());
        }

        int maxUnits = Math.max(wUnits, Math.max(hUnits, dUnits));
        float targetPixels = Math.min(cellW, cellH) * 0.9F; /* margen del 10% */
        float auto = maxUnits > 0 ? targetPixels / (baseScale * maxUnits) : 1F;
        /* No exceder la escala definida por el usuario; sólo reducir si es necesario */
        float finalScale = this.form.uiScale.get() * Math.min(1F, auto);
        matrices.scale(finalScale, finalScale, finalScale);

        matrices.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
        matrices.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

        Color tint = this.form.color.get();
        consumers.setSubstitute(BBSRendering.getColorConsumer(tint));
        consumers.setUI(true);

        renderStructure(matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

        consumers.draw();
        consumers.setUI(false);
        consumers.setSubstitute(null);

        matrices.pop();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        ensureLoaded();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;

        context.stack.push();

        if (context.isPicking())
        {
            CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
            {
                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
                RenderSystem.enableBlend();
            });

            light = 0;
        }
        else
        {
            CustomVertexConsumerProvider.hijackVertexFormat((l) -> RenderSystem.enableBlend());
        }

        Color tint = this.form.color.get();
        consumers.setSubstitute(BBSRendering.getColorConsumer(tint));

        renderStructure(context.stack, consumers, light, context.overlay);

        consumers.draw();
        consumers.setSubstitute(null);
        CustomVertexConsumerProvider.clearRunnables();
        context.stack.pop();
    }

    private void renderStructure(MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay)
    {
        // Centrado basado en límites reales (min/max) para compensar offsets del NBT
        float cx;
        float cy;
        float cz;

        if (boundsMin != null && boundsMax != null)
        {
            cx = (boundsMin.getX() + boundsMax.getX()) / 2f;
            cz = (boundsMin.getZ() + boundsMax.getZ()) / 2f;
            // Mantener apoyado sobre el suelo: usar el mínimo Y como base
            cy = boundsMin.getY();
        }
        else
        {
            // Fallback si no hay límites calculados
            cx = size.getX() / 2f;
            cy = 0f;
            cz = size.getZ() / 2f;
        }

        // Ajuste de paridad: igualar el comportamiento de render de un bloque
        // - Bloque único se traduce -0.5 en X/Z para centrar visualmente sobre el grid
        // - Para estructuras impares: su centro coincide con el centro de un bloque => usar -0.5
        // - Para estructuras pares: su centro está entre dos bloques => usar 0.0 (ya está a mitad de arista)
        float parityX = 0f;
        float parityZ = 0f;
        if (boundsMin != null && boundsMax != null)
        {
            int widthX = boundsMax.getX() - boundsMin.getX() + 1;
            int widthZ = boundsMax.getZ() - boundsMin.getZ() + 1;
            parityX = (widthX % 2 == 1) ? -0.5f : 0f;
            parityZ = (widthZ % 2 == 1) ? -0.5f : 0f;
        }

        for (BlockEntry entry : blocks)
        {
            stack.push();
            stack.translate(entry.pos.getX() - cx + parityX, entry.pos.getY() - cy, entry.pos.getZ() - cz + parityZ);
            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(entry.state, stack, consumers, light, overlay);
            stack.pop();
        }
    }

    private void ensureLoaded()
    {
        String file = form.structureFile.get();

        if (file == null || file.isEmpty())
        {
            // Nada seleccionado; limpiar para evitar render fantasma
            blocks.clear();
            size = BlockPos.ORIGIN;
            boundsMin = null;
            boundsMax = null;
            lastFile = null;
            return;
        }

        if (file.equals(lastFile))
        {
            return;
        }

        File nbtFile = BBSMod.getProvider().getFile(Link.assets(file));

        try
        {
            System.out.println("[BBS][Structure] Intentando cargar '" + file + "' => " + (nbtFile != null ? nbtFile.getAbsolutePath() : "null"));
        }
        catch (Throwable ignored)
        {
        }

        blocks.clear();
        size = BlockPos.ORIGIN;
        boundsMin = null;
        boundsMax = null;
        lastFile = file;

        /* Intentar leer como archivo externo si existe; si no, usar InputStream de assets internos */
        if (nbtFile != null && nbtFile.exists())
        {
            try
            {
                NbtCompound root = NbtIo.readCompressed(nbtFile.toPath(), NbtTagSizeTracker.ofUnlimitedBytes());
                parseStructure(root);
                System.out.println("[BBS][Structure] Cargado comprimido desde File OK: '" + file + "'");
                return;
            }
            catch (IOException e)
            {
                System.out.println("[BBS][Structure] Lectura comprimida desde File falló, probando sin comprimir: '" + file + "'");
                e.printStackTrace();

                try (DataInputStream dis = new DataInputStream(new java.io.FileInputStream(nbtFile)))
                {
                    NbtElement elem = NbtIo.read(dis, NbtTagSizeTracker.ofUnlimitedBytes());
                    if (elem instanceof NbtCompound)
                    {
                        parseStructure((NbtCompound) elem);
                        System.out.println("[BBS][Structure] Cargado sin comprimir desde File OK: '" + file + "'");
                        return;
                    }
                }
                catch (Exception ex)
                {
                    System.out.println("[BBS][Structure] Lectura sin comprimir desde File también falló: '" + file + "'");
                    ex.printStackTrace();
                }
            }
        }

        /* Si no hay File (assets internos), leer vía InputStream del provider */
        try (InputStream is = BBSMod.getProvider().getAsset(Link.assets(file)))
        {
            try
            {
                NbtCompound root = NbtIo.readCompressed(is, NbtTagSizeTracker.ofUnlimitedBytes());
                parseStructure(root);
                System.out.println("[BBS][Structure] Cargado comprimido vía InputStream OK: '" + file + "'");
            }
            catch (IOException e)
            {
                System.out.println("[BBS][Structure] Lectura comprimida vía InputStream falló, probando sin comprimir: '" + file + "'");
                e.printStackTrace();

                /* Reiniciar stream para lectura sin comprimir */
                try (InputStream is2 = BBSMod.getProvider().getAsset(Link.assets(file)); DataInputStream dis = new DataInputStream(is2))
                {
                    NbtElement elem = NbtIo.read(dis, NbtTagSizeTracker.ofUnlimitedBytes());
                    if (elem instanceof NbtCompound)
                    {
                        parseStructure((NbtCompound) elem);
                        System.out.println("[BBS][Structure] Cargado sin comprimir vía InputStream OK: '" + file + "'");
                    }
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("[BBS][Structure] No se pudo abrir asset '" + file + "' vía provider");
            e.printStackTrace();
        }
        
    }

    private void parseStructure(NbtCompound root)
    {
        // Tamaño
        if (root.contains("size", NbtElement.INT_ARRAY_TYPE))
        {
            int[] sz = root.getIntArray("size");
            if (sz.length >= 3)
            {
                size = new BlockPos(sz[0], sz[1], sz[2]);
            }
        }

        // Paleta -> lista de estados
        List<BlockState> paletteStates = new ArrayList<>();
        if (root.contains("palette", NbtElement.LIST_TYPE))
        {
            NbtList palette = root.getList("palette", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < palette.size(); i++)
            {
                NbtCompound entry = palette.getCompound(i);
                BlockState state = readBlockState(entry);
                paletteStates.add(state);
            }
        }

        // Bloques
        if (root.contains("blocks", NbtElement.LIST_TYPE))
        {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

            NbtList list = root.getList("blocks", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++)
            {
                NbtCompound be = list.getCompound(i);

                BlockPos pos = readBlockPos(be.getList("pos", NbtElement.INT_TYPE));
                int stateIndex = be.getInt("state");

                if (stateIndex >= 0 && stateIndex < paletteStates.size())
                {
                    BlockState state = paletteStates.get(stateIndex);
                    blocks.add(new BlockEntry(state, pos));

                    // Actualizar límites
                    if (pos.getX() < minX) minX = pos.getX();
                    if (pos.getY() < minY) minY = pos.getY();
                    if (pos.getZ() < minZ) minZ = pos.getZ();
                    if (pos.getX() > maxX) maxX = pos.getX();
                    if (pos.getY() > maxY) maxY = pos.getY();
                    if (pos.getZ() > maxZ) maxZ = pos.getZ();
                }
            }

            if (!blocks.isEmpty())
            {
                boundsMin = new BlockPos(minX, minY, minZ);
                boundsMax = new BlockPos(maxX, maxY, maxZ);

                try
                {
                    System.out.println("[BBS][Structure] Bounds min=" + boundsMin + ", max=" + boundsMax);
                }
                catch (Throwable ignored) {}
            }
        }
    }

    private BlockPos readBlockPos(NbtList list)
    {
        if (list == null || list.size() < 3)
        {
            return BlockPos.ORIGIN;
        }

        int x = list.getInt(0);
        int y = list.getInt(1);
        int z = list.getInt(2);

        return new BlockPos(x, y, z);
    }

    private BlockState readBlockState(NbtCompound entry)
    {
        String name = entry.getString("Name");
        Block block;
        BlockState state;

        try
        {
            Identifier id = new Identifier(name);
            block = Registries.BLOCK.get(id);
            if (block == null)
            {
                // Identificador válido pero bloque no encontrado
                System.out.println("[BBS][Structure] Bloque no encontrado para id: " + id + ", usando aire");
                block = net.minecraft.block.Blocks.AIR;
            }
        }
        catch (Exception e)
        {
            // Identificador inválido (por ejemplo sin namespace); usar aire
            System.out.println("[BBS][Structure] Identificador inválido en paleta: '" + name + "', usando aire");
            block = net.minecraft.block.Blocks.AIR;
        }

        state = block.getDefaultState();

        if (entry.contains("Properties", NbtElement.COMPOUND_TYPE))
        {
            NbtCompound props = entry.getCompound("Properties");
            for (String key : props.getKeys())
            {
                String value = props.getString(key);
                Property<?> property = block.getStateManager().getProperty(key);

                if (property != null)
                {
                    Optional<?> parsed = property.parse(value);
                    if (parsed.isPresent())
                    {
                        try
                        {
                            @SuppressWarnings({"rawtypes", "unchecked"})
                            Property raw = property;
                            @SuppressWarnings("unchecked")
                            Comparable c = (Comparable) parsed.get();
                            state = state.with(raw, c);
                        }
                        catch (Exception ignored)
                        {}
                    }
                }
            }
        }

        return state;
    }
}