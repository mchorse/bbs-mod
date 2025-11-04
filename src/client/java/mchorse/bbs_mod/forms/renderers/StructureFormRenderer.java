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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
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
import net.minecraft.util.math.random.Random;
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

        // Detectar shaders (Iris) para alternar buffers/capas apropiadas
        boolean shadersActive = isShadersActive();

        net.minecraft.client.render.VertexConsumerProvider.Immediate consumers;
        boolean useEntityLayers;

        if (shadersActive)
        {
            // Buffers del WorldRenderer para compatibilidad con shaders
            consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
            useEntityLayers = true;
        }
        else
        {
            // En vanilla, el proveedor custom da una iluminación más consistente
            consumers = FormUtilsClient.getProvider();
            useEntityLayers = false;
        }

        // Ajuste de gráfica para capas (hojas, etc.)
        try
        {
            net.minecraft.client.option.GraphicsMode gm = MinecraftClient.getInstance().options.getGraphicsMode().getValue();
            net.minecraft.client.render.RenderLayers.setFancyGraphicsOrBetter(gm != net.minecraft.client.option.GraphicsMode.FAST);
        }
        catch (Throwable ignored) {}

        renderStructureCulledWorld(context, context.stack, consumers, light, context.overlay, useEntityLayers);

        consumers.draw();
        /* Asegurar que no se filtren estados GL hacia el render de partes de cuerpo.
         * El pipeline de bloques/entidades puede ajustar blend/depth, así que
         * restablecemos a valores seguros antes de renderizar formularios hijos. */
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        // Mantener la función de profundidad estándar usada en el mundo
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);

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

    /**
     * Render con culling usando BlockRenderView virtual para aprovechar la lógica vanilla.
     * Mantiene el mismo centrado y paridad que renderStructure.
     */
    private void renderStructureCulledWorld(FormRenderingContext context, MatrixStack stack, net.minecraft.client.render.VertexConsumerProvider consumers, int light, int overlay, boolean useEntityLayers)
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

        float parityX = 0f;
        float parityZ = 0f;
        if (boundsMin != null && boundsMax != null)
        {
            int widthX = boundsMax.getX() - boundsMin.getX() + 1;
            int widthZ = boundsMax.getZ() - boundsMin.getZ() + 1;
            parityX = (widthX % 2 == 1) ? -0.5f : 0f;
            parityZ = (widthZ % 2 == 1) ? -0.5f : 0f;
        }

        // Construir vista virtual con todos los bloques
        java.util.ArrayList<mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView.Entry> entries = new java.util.ArrayList<>();
        for (BlockEntry be : blocks)
        {
            entries.add(new mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView.Entry(be.state, be.pos));
        }
        mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView view = new mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView(entries)
            .setBiomeOverride(this.form.biomeId.get());

        BlockEntityRenderDispatcher beDispatcher = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

        // Posición ancla en el mundo: el formulario se renderiza relativo a su entidad
        net.minecraft.util.math.BlockPos anchor = new net.minecraft.util.math.BlockPos(
            (int)Math.floor(context.entity.getX()),
            (int)Math.floor(context.entity.getY()),
            (int)Math.floor(context.entity.getZ())
        );

        for (BlockEntry entry : blocks)
        {
            stack.push();
            stack.translate(entry.pos.getX() - cx + parityX, entry.pos.getY() - cy, entry.pos.getZ() - cz + parityZ);

            // Usar la capa de entidad para bloques cuando se renderiza con el proveedor
            // de vértices de entidad del WorldRenderer. Esto asegura compatibilidad
            // con shaders (Iris/Sodium) para capas translúcidas y especiales.
            RenderLayer layer = useEntityLayers
                ? RenderLayers.getEntityBlockLayer(entry.state, false)
                : RenderLayers.getBlockLayer(entry.state);

            VertexConsumer vc = consumers.getBuffer(layer);
            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, view, stack, vc, true, Random.create());

            // Renderizar bloques con entidad (cofres, camas, carteles, cráneos, etc.)
            Block block = entry.state.getBlock();
            if (block instanceof BlockEntityProvider)
            {
                // Alinear la posición del BE con la ubicación real donde se dibuja
                int dx = (int)Math.floor(entry.pos.getX() - cx + parityX);
                int dy = (int)Math.floor(entry.pos.getY() - cy);
                int dz = (int)Math.floor(entry.pos.getZ() - cz + parityZ);
                net.minecraft.util.math.BlockPos worldPos = anchor.add(dx, dy, dz);

                BlockEntity be = ((BlockEntityProvider) block).createBlockEntity(worldPos, entry.state);
                if (be != null)
                {
                    // Asociar mundo real para que el renderer pueda consultar luz y efectos
                    if (MinecraftClient.getInstance().world != null)
                    {
                        be.setWorld(MinecraftClient.getInstance().world);
                    }

                    // Diagnóstico: verificar si existe renderer para este BE
                    net.minecraft.client.render.block.entity.BlockEntityRenderer<?> renderer = beDispatcher.get(be);

                    // Render del BE directamente con el renderer para evitar traducciones internas
                    // basadas en cámara/posición mundial que desalinean el dibujo respecto a la matriz local.
                    int beLight = (MinecraftClient.getInstance().world != null)
                        ? net.minecraft.client.render.WorldRenderer.getLightmapCoordinates(MinecraftClient.getInstance().world, worldPos)
                        : light;

                    if (renderer != null)
                    {
                        @SuppressWarnings({"rawtypes", "unchecked"})
                        net.minecraft.client.render.block.entity.BlockEntityRenderer raw = (net.minecraft.client.render.block.entity.BlockEntityRenderer) renderer;
                        raw.render(be, 0F, stack, consumers, beLight, overlay);
                    }
                }
            }

            stack.pop();
        }
    }

    /**
     * Detecta si hay shaders activos (Iris). Evita dependencias duras usando reflexión.
     */
    private boolean isShadersActive()
    {
        try
        {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("isShaderPackInUse").invoke(api);
            return result instanceof Boolean && (Boolean) result;
        }
        catch (Throwable ignored)
        {
        }

        return false;
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
                return;
            }
            catch (IOException e)
            {
                
            }
        }

        /* Si no hay File (assets internos), leer vía InputStream del provider */
        try (InputStream is = BBSMod.getProvider().getAsset(Link.assets(file)))
        {
            try
            {
                NbtCompound root = NbtIo.readCompressed(is, NbtTagSizeTracker.ofUnlimitedBytes());
                parseStructure(root);
            }
            catch (IOException e)
            {
                
            }
        }
        catch (Exception e)
        {
            
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
                block = net.minecraft.block.Blocks.AIR;
            }
        }
        catch (Exception e)
        {
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