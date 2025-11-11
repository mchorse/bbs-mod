package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.IModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.cubic.render.vao.StructureVAOCollector;
import mchorse.bbs_mod.cubic.render.vao.ModelVAOData;
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
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.TexturedRenderLayers;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.FluidState;
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
import org.joml.Vector4f;

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
    private IModelVAO structureVao = null;
    private boolean vaoDirty = true;
    private boolean capturingVAO = false;
    // VAO dedicado para picking (incluye bloques animados y con tinte por bioma)
    private IModelVAO structureVaoPicking = null;
    private boolean vaoPickingDirty = true;
    // Controla si, durante la captura del VAO, se deben incluir bloques especiales
    private boolean capturingIncludeSpecialBlocks = false;

    public StructureFormRenderer(StructureForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        // Asegurar que el batch de UI actual se vacíe antes de dibujar 3D
        context.batcher.getContext().draw();

        ensureLoaded();

        MatrixStack matrices = context.batcher.getContext().getMatrices();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        // Para dibujar contenido 3D dentro de UI, usar prueba de profundidad estándar
        // y restaurarla al finalizar para evitar que otros paneles se vean afectados.
        com.mojang.blaze3d.systems.RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
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

        boolean optimize = mchorse.bbs_mod.BBSSettings.structureOptimization.get();
        if (!optimize)
        {
            // Modo BufferBuilder: mejor iluminación, peor rendimiento
            boolean shaders = this.isShadersActive();
            net.minecraft.client.render.VertexConsumerProvider consumers = shaders
                ? net.minecraft.client.MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
                : net.minecraft.client.render.VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

            try
            {
                FormRenderingContext uiContext = new FormRenderingContext()
                    .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);
                renderStructureCulledWorld(uiContext, matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, shaders);
                if (consumers instanceof net.minecraft.client.render.VertexConsumerProvider.Immediate immediate)
                {
                    immediate.draw();
                }
            }
            catch (Throwable ignored) {}
        }
        else
        {
            // Preparar VAO si es necesario y dibujar con shader compatible con animaciones
            if (this.structureVao == null || this.vaoDirty)
            {
                buildStructureVAO();
            }

            if (this.structureVao != null)
            {
                Color tint = this.form.color.get();
                net.minecraft.client.render.GameRenderer gameRenderer = net.minecraft.client.MinecraftClient.getInstance().gameRenderer;
                gameRenderer.getLightmapTextureManager().enable();
                gameRenderer.getOverlayTexture().setupOverlayColor();

                // Volver al shader de modelo propio en vanilla para asegurar compatibilidad del VAO
                net.minecraft.client.gl.ShaderProgram shader = BBSShaders.getModel();

                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
                // Habilitar blending para soportar capas translúcidas (vidrios, portal, hojas, etc.)
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                ModelVAORenderer.render(shader, this.structureVao, matrices, tint.r, tint.g, tint.b, tint.a, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

                // Pase de Block Entities tras VAO
                try
                {
                    net.minecraft.client.render.VertexConsumerProvider beConsumers = net.minecraft.client.MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                    FormRenderingContext beContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);
                    renderBlockEntitiesOnly(beContext, matrices, beConsumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
                    if (beConsumers instanceof net.minecraft.client.render.VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }
                }
                catch (Throwable ignored) {}

                // Pase adicional: bloques con tinte por bioma (hojas/grass/vines/lily pad)
                try
                {
                    boolean shadersEnabled = mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled() && mchorse.bbs_mod.client.BBSRendering.isRenderingWorld();
                    net.minecraft.client.render.VertexConsumerProvider consumersTint = shadersEnabled
                        ? net.minecraft.client.MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
                        : net.minecraft.client.render.VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

                    FormRenderingContext tintContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);
                    renderBiomeTintedBlocksVanilla(tintContext, matrices, consumersTint, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

                    if (consumersTint instanceof net.minecraft.client.render.VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }
                }
                catch (Throwable ignored) {}

                // Pase adicional: bloques animados (portal/fluido) con capa moving block
                try
                {
                    boolean shadersEnabled = mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled() && mchorse.bbs_mod.client.BBSRendering.isRenderingWorld();
                    net.minecraft.client.render.VertexConsumerProvider consumersAnim = shadersEnabled
                        ? net.minecraft.client.MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
                        : net.minecraft.client.render.VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

                    FormRenderingContext animContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);
                    renderAnimatedBlocksVanilla(animContext, matrices, consumersAnim, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

                    if (consumersAnim instanceof net.minecraft.client.render.VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }
                }
                catch (Throwable ignored) {}

                gameRenderer.getLightmapTextureManager().disable();
                gameRenderer.getOverlayTexture().teardownOverlayColor();
                RenderSystem.disableBlend();
            }
        }

        matrices.pop();
        // Restaurar el estado de profundidad esperado por el sistema de UI
        com.mojang.blaze3d.systems.RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_ALWAYS);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        ensureLoaded();
        context.stack.push();

        boolean optimize = mchorse.bbs_mod.BBSSettings.structureOptimization.get();
        boolean picking = context.isPicking();
        if (optimize && (this.structureVao == null || this.vaoDirty))
        {
            buildStructureVAO();
        }

        if (!optimize)
        {
            // Si estamos en picking, renderizar con VAO (picking) y el shader de picking para obtener la silueta completa
            if (picking)
            {
                if (this.structureVaoPicking == null || this.vaoPickingDirty)
                {
                    buildStructureVAOPicking();
                }

                Color tint3D = this.form.color.get();
                int light = 0;

                net.minecraft.client.render.GameRenderer gameRenderer = net.minecraft.client.MinecraftClient.getInstance().gameRenderer;
                gameRenderer.getLightmapTextureManager().enable();
                gameRenderer.getOverlayTexture().setupOverlayColor();

                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
                RenderSystem.enableBlend();
                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
                ModelVAORenderer.render(BBSShaders.getPickerModelsProgram(), this.structureVaoPicking, context.stack, tint3D.r, tint3D.g, tint3D.b, tint3D.a, light, context.overlay);

                gameRenderer.getLightmapTextureManager().disable();
                gameRenderer.getOverlayTexture().teardownOverlayColor();

                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            }
            else
            {
                // Modo BufferBuilder: usar pipeline vanilla/culling con mejor iluminación
                int light = context.light;
                boolean shaders = this.isShadersActive();
                net.minecraft.client.render.VertexConsumerProvider consumers = shaders
                    ? net.minecraft.client.MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
                    : net.minecraft.client.render.VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

                // Alinear el manejo de estados con el camino VAO para evitar fugas
                // de estado que afectan al primer modelo renderizado después.
                net.minecraft.client.render.GameRenderer gameRenderer = net.minecraft.client.MinecraftClient.getInstance().gameRenderer;
                gameRenderer.getLightmapTextureManager().enable();
                gameRenderer.getOverlayTexture().setupOverlayColor();
                // Asegurar atlas de bloques activo al iniciar el pase
                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

                try
                {
                    renderStructureCulledWorld(context, context.stack, consumers, light, context.overlay, shaders);
                    if (consumers instanceof net.minecraft.client.render.VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }
                }
                catch (Throwable ignored) {}

                // Restaurar estado tras el pase de BufferBuilder para no contaminar
                // el siguiente render (modelos, UI, etc.)
                gameRenderer.getLightmapTextureManager().disable();
                gameRenderer.getOverlayTexture().teardownOverlayColor();

                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            }
        }
        else if (this.structureVao != null)
        {
            Color tint3D = this.form.color.get();
            int light = context.isPicking() ? 0 : context.light;

            net.minecraft.client.render.GameRenderer gameRenderer = net.minecraft.client.MinecraftClient.getInstance().gameRenderer;
            gameRenderer.getLightmapTextureManager().enable();
            gameRenderer.getOverlayTexture().setupOverlayColor();

            if (context.isPicking())
            {
                if (this.structureVaoPicking == null || this.vaoPickingDirty)
                {
                    buildStructureVAOPicking();
                }
                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
                RenderSystem.enableBlend();
                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
                ModelVAORenderer.render(BBSShaders.getPickerModelsProgram(), this.structureVaoPicking, context.stack, tint3D.r, tint3D.g, tint3D.b, tint3D.a, light, context.overlay);
            }
            else
            {
                // VAO con shader compatible con packs: usar programa de entidad translúcida cuando Iris está activo
                net.minecraft.client.gl.ShaderProgram shader = (mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled() && mchorse.bbs_mod.client.BBSRendering.isRenderingWorld())
                    ? net.minecraft.client.render.GameRenderer.getRenderTypeEntityTranslucentCullProgram()
                    : BBSShaders.getModel();

                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                ModelVAORenderer.render(shader, this.structureVao, context.stack, tint3D.r, tint3D.g, tint3D.b, tint3D.a, light, context.overlay);

                // Pase de Block Entities tras VAO
                try
                {
                    net.minecraft.client.render.VertexConsumerProvider beConsumers = net.minecraft.client.MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                    renderBlockEntitiesOnly(context, context.stack, beConsumers, light, context.overlay);
                    if (beConsumers instanceof net.minecraft.client.render.VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }
                }
                catch (Throwable ignored) {}

                // Pase adicional: bloques con tinte por bioma
                try
                {
                    net.minecraft.client.render.VertexConsumerProvider.Immediate tintConsumers = net.minecraft.client.render.VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
                    renderBiomeTintedBlocksVanilla(context, context.stack, tintConsumers, light, context.overlay);
                    tintConsumers.draw();
                }
                catch (Throwable ignored) {}

                // Pase adicional: bloques animados (portal/fluido) con capa moving block
                try
                {
                    net.minecraft.client.render.VertexConsumerProvider.Immediate animConsumers = net.minecraft.client.render.VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
                    renderAnimatedBlocksVanilla(context, context.stack, animConsumers, light, context.overlay);
                    animConsumers.draw();
                }
                catch (Throwable ignored) {}
            }

            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();

            // Restaurar estado si se usó VAO
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        }

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

        // Determinar pivote efectivo
        float pivotX;
        float pivotY;
        float pivotZ;
        mchorse.bbs_mod.forms.forms.utils.PivotSettings pivotSettingsRuntime = this.form.pivot.getRuntimeValue();
        boolean useAuto = pivotSettingsRuntime != null ? pivotSettingsRuntime.auto : this.form.autoPivot.get();
        if (useAuto)
        {
            // Ajuste de paridad: igualar el comportamiento de render de un bloque
            // - Bloque único se traduce -0.5 en X/Z para centrar visualmente sobre el grid
            // - Para estructuras impares: su centro coincide con el centro de un bloque => usar -0.5
            // - Para estructuras pares: su centro está entre dos bloques => usar 0.0 (ya está a mitad de arista)
            float parityXAuto = 0f;
            float parityZAuto = 0f;
            if (boundsMin != null && boundsMax != null)
            {
                int widthX = boundsMax.getX() - boundsMin.getX() + 1;
                int widthZ = boundsMax.getZ() - boundsMin.getZ() + 1;
                parityXAuto = (widthX % 2 == 1) ? -0.5f : 0f;
                parityZAuto = (widthZ % 2 == 1) ? -0.5f : 0f;
            }
            pivotX = cx - parityXAuto;
            pivotY = cy;
            pivotZ = cz - parityZAuto;
        }
        else
        {
            if (pivotSettingsRuntime != null)
            {
                pivotX = pivotSettingsRuntime.pivot.x;
                pivotY = pivotSettingsRuntime.pivot.y;
                pivotZ = pivotSettingsRuntime.pivot.z;
            }
            else
            {
                pivotX = this.form.pivotX.get();
                pivotY = this.form.pivotY.get();
                pivotZ = this.form.pivotZ.get();
            }
        }

        for (BlockEntry entry : blocks)
        {
            stack.push();
            stack.translate(entry.pos.getX() - pivotX, entry.pos.getY() - pivotY, entry.pos.getZ() - pivotZ);
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

        float pivotX;
        float pivotY;
        float pivotZ;
        mchorse.bbs_mod.forms.forms.utils.PivotSettings pivotSettingsRuntime2 = this.form.pivot.getRuntimeValue();
        boolean useAuto2 = pivotSettingsRuntime2 != null ? pivotSettingsRuntime2.auto : this.form.autoPivot.get();
        if (useAuto2)
        {
            float parityXAuto = 0f;
            float parityZAuto = 0f;
            if (boundsMin != null && boundsMax != null)
            {
                int widthX = boundsMax.getX() - boundsMin.getX() + 1;
                int widthZ = boundsMax.getZ() - boundsMin.getZ() + 1;
                parityXAuto = (widthX % 2 == 1) ? -0.5f : 0f;
                parityZAuto = (widthZ % 2 == 1) ? -0.5f : 0f;
            }
            pivotX = cx - parityXAuto;
            pivotY = cy;
            pivotZ = cz - parityZAuto;
        }
        else
        {
            if (pivotSettingsRuntime2 != null)
            {
                pivotX = pivotSettingsRuntime2.pivot.x;
                pivotY = pivotSettingsRuntime2.pivot.y;
                pivotZ = pivotSettingsRuntime2.pivot.z;
            }
            else
            {
                pivotX = this.form.pivotX.get();
                pivotY = this.form.pivotY.get();
                pivotZ = this.form.pivotZ.get();
            }
        }

        // Construir vista virtual con todos los bloques
        java.util.ArrayList<mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView.Entry> entries = new java.util.ArrayList<>();
        for (BlockEntry be : blocks)
        {
            entries.add(new mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView.Entry(be.state, be.pos));
        }
        // Resolve unified structure light settings with legacy fallback
        boolean lightsEnabled;
        int lightIntensity;
        mchorse.bbs_mod.forms.forms.utils.StructureLightSettings slRuntime = this.form.structureLight.getRuntimeValue();
        if (slRuntime != null)
        {
            lightsEnabled = slRuntime.enabled;
            lightIntensity = slRuntime.intensity;
        }
        else
        {
            lightsEnabled = this.form.emitLight.get();
            lightIntensity = this.form.lightIntensity.get();
        }

        mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView view = new mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView(entries)
            .setBiomeOverride(this.form.biomeId.get())
            .setLightsEnabled(lightsEnabled)
            .setLightIntensity(lightIntensity);

        BlockEntityRenderDispatcher beDispatcher = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

        // Posición ancla en el mundo: el formulario se renderiza relativo a su entidad
        // Si no hay entidad (UI), usar origen como ancla
        net.minecraft.util.math.BlockPos anchor;
        if (context.entity != null)
        {
            anchor = new net.minecraft.util.math.BlockPos(
                (int)Math.floor(context.entity.getX()),
                (int)Math.floor(context.entity.getY()),
                (int)Math.floor(context.entity.getZ())
            );
        }
        else
        {
            anchor = net.minecraft.util.math.BlockPos.ORIGIN;
        }

        // Definir offset base desde el centro/paridad para que el BlockRenderView
        // pueda traducir las consultas de luz/color a coordenadas de mundo reales.
        int baseDx = (int)Math.floor(-pivotX);
        int baseDy = (int)Math.floor(-pivotY);
        int baseDz = (int)Math.floor(-pivotZ);
        view.setWorldAnchor(anchor, baseDx, baseDy, baseDz);

        for (BlockEntry entry : blocks)
        {
            stack.push();
            stack.translate(entry.pos.getX() - pivotX, entry.pos.getY() - pivotY, entry.pos.getZ() - pivotZ);

            // Durante la captura del VAO normal, omitir bloques con texturas animadas
            // o tinte por bioma para evitar doble dibujo y parpadeos.
            // En captura para picking (capturingIncludeSpecialBlocks=true), incluirlos.
            if (this.capturingVAO && !this.capturingIncludeSpecialBlocks && (isAnimatedTexture(entry.state) || isBiomeTinted(entry.state)))
            {
                stack.pop();
                continue;
            }

            // Usar la capa de entidad para bloques cuando se renderiza con el proveedor
            // de vértices de entidad del WorldRenderer. Esto asegura compatibilidad
            // con shaders (Iris/Sodium) para capas translúcidas y especiales.
            RenderLayer layer = useEntityLayers
                ? RenderLayers.getEntityBlockLayer(entry.state, false)
                : RenderLayers.getBlockLayer(entry.state);

            // Si hay opacidad global (<1), forzar capa translúcida para todos los bloques
            // de la estructura, de modo que el alpha se aplique incluso a geometría sólida/cutout.
            float globalAlpha = this.form.color.get().a;
            if (globalAlpha < 0.999f)
            {
                layer = RenderLayer.getTranslucent();
            }

            VertexConsumer vc = consumers.getBuffer(layer);
            // Envolver el consumidor con tinte/opacidad para garantizar coloración
            // también cuando se usan buffers de entidad (compatibilidad con shaders).
            Color tint = this.form.color.get();
            java.util.function.Function<VertexConsumer, VertexConsumer> recolor = BBSRendering.getColorConsumer(tint);
            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }
            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, view, stack, vc, true, Random.create());

            // Renderizar bloques con entidad (cofres, camas, carteles, cráneos, etc.)
            Block block = entry.state.getBlock();
            if (!this.capturingVAO && block instanceof BlockEntityProvider)
            {
                // Alinear la posición del BE con la ubicación real donde se dibuja
                int dx = (int)Math.floor(entry.pos.getX() - pivotX);
                int dy = (int)Math.floor(entry.pos.getY() - pivotY);
                int dz = (int)Math.floor(entry.pos.getZ() - pivotZ);
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

        // Importante: si está activo Sodium/Iris, el wrapper de recolor usa
        // estado estático global (RecolorVertexConsumer.newColor). Asegurar
        // que se restablece tras este pase para que la UI no herede el tinte.
        RecolorVertexConsumer.newColor = null;
    }

    /**
     * Render especializado: dibuja únicamente bloques con texturas animadas (portal, agua, lava)
     * usando la capa TranslucentMovingBlock de vanilla para obtener animación continua.
     * Reutiliza el mismo cálculo de centrado/paridad y vista virtual del mundo.
     */
    private void renderAnimatedBlocksVanilla(FormRenderingContext context, MatrixStack stack, net.minecraft.client.render.VertexConsumerProvider consumers, int light, int overlay)
    {
        // Centrado basado en límites reales (min/max)
        float cx;
        float cy;
        float cz;

        if (boundsMin != null && boundsMax != null)
        {
            cx = (boundsMin.getX() + boundsMax.getX()) / 2f;
            cz = (boundsMin.getZ() + boundsMax.getZ()) / 2f;
            cy = boundsMin.getY();
        }
        else
        {
            cx = size.getX() / 2f;
            cy = 0f;
            cz = size.getZ() / 2f;
        }

        float pivotX;
        float pivotY;
        float pivotZ;
        mchorse.bbs_mod.forms.forms.utils.PivotSettings pivotSettingsRuntime3 = this.form.pivot.getRuntimeValue();
        boolean useAuto3 = pivotSettingsRuntime3 != null ? pivotSettingsRuntime3.auto : this.form.autoPivot.get();
        if (useAuto3)
        {
            float parityXAuto = 0f;
            float parityZAuto = 0f;
            if (boundsMin != null && boundsMax != null)
            {
                int widthX = boundsMax.getX() - boundsMin.getX() + 1;
                int widthZ = boundsMax.getZ() - boundsMin.getZ() + 1;
                parityXAuto = (widthX % 2 == 1) ? -0.5f : 0f;
                parityZAuto = (widthZ % 2 == 1) ? -0.5f : 0f;
            }
            pivotX = cx - parityXAuto;
            pivotY = cy;
            pivotZ = cz - parityZAuto;
        }
        else
        {
            if (pivotSettingsRuntime3 != null)
            {
                pivotX = pivotSettingsRuntime3.pivot.x;
                pivotY = pivotSettingsRuntime3.pivot.y;
                pivotZ = pivotSettingsRuntime3.pivot.z;
            }
            else
            {
                pivotX = this.form.pivotX.get();
                pivotY = this.form.pivotY.get();
                pivotZ = this.form.pivotZ.get();
            }
        }

        // Vista virtual para culling/colores/luz correctos
        java.util.ArrayList<mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView.Entry> entries = new java.util.ArrayList<>();
        for (BlockEntry be : blocks)
        {
            entries.add(new mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView.Entry(be.state, be.pos));
        }
        // Resolve unified structure light settings with legacy fallback
        boolean lightsEnabled2;
        int lightIntensity2;
        mchorse.bbs_mod.forms.forms.utils.StructureLightSettings slRuntime2 = this.form.structureLight.getRuntimeValue();
        if (slRuntime2 != null)
        {
            lightsEnabled2 = slRuntime2.enabled;
            lightIntensity2 = slRuntime2.intensity;
        }
        else
        {
            lightsEnabled2 = this.form.emitLight.get();
            lightIntensity2 = this.form.lightIntensity.get();
        }

        mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView view = new mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView(entries)
            .setBiomeOverride(this.form.biomeId.get())
            .setLightsEnabled(lightsEnabled2)
            .setLightIntensity(lightIntensity2);

        // Ancla mundial
        net.minecraft.util.math.BlockPos anchor;
        if (context.entity != null)
        {
            anchor = new net.minecraft.util.math.BlockPos(
                (int)Math.floor(context.entity.getX()),
                (int)Math.floor(context.entity.getY()),
                (int)Math.floor(context.entity.getZ())
            );
        }
        else
        {
            anchor = net.minecraft.util.math.BlockPos.ORIGIN;
        }

        int baseDx = (int)Math.floor(-pivotX);
        int baseDy = (int)Math.floor(-pivotY);
        int baseDz = (int)Math.floor(-pivotZ);
        view.setWorldAnchor(anchor, baseDx, baseDy, baseDz);

        for (BlockEntry entry : blocks)
        {
            if (!isAnimatedTexture(entry.state))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - pivotX, entry.pos.getY() - pivotY, entry.pos.getZ() - pivotZ);

            // Selección de capa: en shaders usar variante de entidad para que el pack procese la animación
            boolean shadersEnabled = mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled() && mchorse.bbs_mod.client.BBSRendering.isRenderingWorld();
            RenderLayer layer = shadersEnabled
                ? RenderLayers.getEntityBlockLayer(entry.state, true)
                : RenderLayer.getTranslucentMovingBlock();

            // Aplicar alpha global como recolor
            VertexConsumer vc = consumers.getBuffer(layer);
            Color tint = this.form.color.get();
            java.util.function.Function<VertexConsumer, VertexConsumer> recolor = BBSRendering.getColorConsumer(tint);
            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, view, stack, vc, true, Random.create());
            stack.pop();
        }

        // Reset del estado global de color (Sodium/Iris) tras el pase animado
        RecolorVertexConsumer.newColor = null;
    }

    /** Renderiza bloques que requieren tinte por bioma (hojas, césped, lianas, nenúfar) usando capas vanilla. */
    private void renderBiomeTintedBlocksVanilla(FormRenderingContext context, MatrixStack stack, net.minecraft.client.render.VertexConsumerProvider consumers, int light, int overlay)
    {
        // Asegurar estado de blending correcto para capas translúcidas
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Centrado basado en límites reales (min/max)
        float cx;
        float cy;
        float cz;

        if (boundsMin != null && boundsMax != null)
        {
            cx = (boundsMin.getX() + boundsMax.getX()) / 2f;
            cz = (boundsMin.getZ() + boundsMax.getZ()) / 2f;
            cy = boundsMin.getY();
        }
        else
        {
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

        // Vista virtual para culling/colores/luz correctos
        java.util.ArrayList<mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView.Entry> entries = new java.util.ArrayList<>();
        for (BlockEntry be : blocks)
        {
            entries.add(new mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView.Entry(be.state, be.pos));
        }
        mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView view = new mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView(entries)
            .setBiomeOverride(this.form.biomeId.get())
            .setLightsEnabled(this.form.emitLight.get())
            .setLightIntensity(this.form.lightIntensity.get());

        // Ancla mundial
        net.minecraft.util.math.BlockPos anchor;
        if (context.entity != null)
        {
            anchor = new net.minecraft.util.math.BlockPos(
                (int)Math.floor(context.entity.getX()),
                (int)Math.floor(context.entity.getY()),
                (int)Math.floor(context.entity.getZ())
            );
        }
        else
        {
            anchor = net.minecraft.util.math.BlockPos.ORIGIN;
        }

        int baseDx = (int)Math.floor(-cx + parityX);
        int baseDy = (int)Math.floor(-cy);
        int baseDz = (int)Math.floor(-cz + parityZ);
        view.setWorldAnchor(anchor, baseDx, baseDy, baseDz);

        for (BlockEntry entry : blocks)
        {
            if (!isBiomeTinted(entry.state))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - cx + parityX, entry.pos.getY() - cy, entry.pos.getZ() - cz + parityZ);

            // Capa según el estado; hojas suelen ser cutout_mipped, césped/plantas cutout
            RenderLayer layer = RenderLayers.getBlockLayer(entry.state);

            // Si hay opacidad global (<1), forzar capa translúcida para que el alpha
            // se aplique en materiales originalmente cutout/cull y no "desaparezcan".
            float globalAlpha = this.form.color.get().a;
            if (globalAlpha < 0.999f)
            {
                layer = RenderLayer.getTranslucent();
            }

            VertexConsumer vc = consumers.getBuffer(layer);
            Color tint = this.form.color.get();
            java.util.function.Function<VertexConsumer, VertexConsumer> recolor = BBSRendering.getColorConsumer(tint);
            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, view, stack, vc, true, Random.create());
            stack.pop();
        }

        // Restaurar estado
        RenderSystem.disableBlend();
        // Reset del estado global de color (Sodium/Iris) para evitar que la UI se tiña
        RecolorVertexConsumer.newColor = null;
    }

    /** Determina si el bloque requiere animación de textura (portal/agua/lava). */
    private boolean isAnimatedTexture(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        // Portal del Nether
        if (state.isOf(Blocks.NETHER_PORTAL))
        {
            return true;
        }

        // Fluidos: agua y lava (incluyendo variantes en movimiento)
        FluidState fs = state.getFluidState();
        if (fs != null)
        {
            if (fs.getFluid() == Fluids.WATER || fs.getFluid() == Fluids.FLOWING_WATER ||
                fs.getFluid() == Fluids.LAVA || fs.getFluid() == Fluids.FLOWING_LAVA)
            {
                return true;
            }
        }

        return false;
    }

    /** Heurística: determina si el bloque usa tinte por bioma (foliage/grass/vine/lily pad). */
    private boolean isBiomeTinted(BlockState state)
    {
        if (state == null) return false;
        Block b = state.getBlock();
        return (b instanceof net.minecraft.block.LeavesBlock)
            || (b instanceof net.minecraft.block.GrassBlock)
            || (b instanceof net.minecraft.block.VineBlock)
            || (b instanceof net.minecraft.block.LilyPadBlock);
    }

    /**
     * Renderiza únicamente Block Entities (cofres, camas, carteles, cráneos, etc.) sobre la estructura ya dibujada vía VAO.
     * Reutiliza el mismo cálculo de centrado/paridad y ancla mundial que el render culleado.
     */
    private void renderBlockEntitiesOnly(FormRenderingContext context, MatrixStack stack, net.minecraft.client.render.VertexConsumerProvider consumers, int light, int overlay)
    {
        // Centrado basado en límites reales (min/max)
        float cx;
        float cy;
        float cz;

        if (boundsMin != null && boundsMax != null)
        {
            cx = (boundsMin.getX() + boundsMax.getX()) / 2f;
            cz = (boundsMin.getZ() + boundsMax.getZ()) / 2f;
            cy = boundsMin.getY();
        }
        else
        {
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

        // Ancla mundial
        net.minecraft.util.math.BlockPos anchor;
        if (context.entity != null)
        {
            anchor = new net.minecraft.util.math.BlockPos(
                (int)Math.floor(context.entity.getX()),
                (int)Math.floor(context.entity.getY()),
                (int)Math.floor(context.entity.getZ())
            );
        }
        else
        {
            anchor = net.minecraft.util.math.BlockPos.ORIGIN;
        }

        BlockEntityRenderDispatcher beDispatcher = net.minecraft.client.MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

        for (BlockEntry entry : blocks)
        {
            Block block = entry.state.getBlock();
            if (!(block instanceof BlockEntityProvider))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - cx + parityX, entry.pos.getY() - cy, entry.pos.getZ() - cz + parityZ);

            int dx = (int)Math.floor(entry.pos.getX() - cx + parityX);
            int dy = (int)Math.floor(entry.pos.getY() - cy);
            int dz = (int)Math.floor(entry.pos.getZ() - cz + parityZ);
            net.minecraft.util.math.BlockPos worldPos = anchor.add(dx, dy, dz);

            BlockEntity be = ((BlockEntityProvider) block).createBlockEntity(worldPos, entry.state);
            if (be != null)
            {
                if (net.minecraft.client.MinecraftClient.getInstance().world != null)
                {
                    be.setWorld(net.minecraft.client.MinecraftClient.getInstance().world);
                }

                net.minecraft.client.render.block.entity.BlockEntityRenderer<?> renderer = beDispatcher.get(be);
                int beLight = (net.minecraft.client.MinecraftClient.getInstance().world != null)
                    ? net.minecraft.client.render.WorldRenderer.getLightmapCoordinates(net.minecraft.client.MinecraftClient.getInstance().world, worldPos)
                    : light;

                if (renderer != null)
                {
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    net.minecraft.client.render.block.entity.BlockEntityRenderer raw = (net.minecraft.client.render.block.entity.BlockEntityRenderer) renderer;

                    // Aplicar tinte global siempre a Block Entities, aislando el provider
                    CustomVertexConsumerProvider beProvider = FormUtilsClient.getProvider();
                    beProvider.setSubstitute(BBSRendering.getColorConsumer(this.form.color.get()));
                    try
                    {
                        raw.render(be, 0F, stack, beProvider, beLight, overlay);
                    }
                    finally
                    {
                        beProvider.draw();
                        beProvider.setSubstitute(null);
                        CustomVertexConsumerProvider.clearRunnables();
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
            vaoDirty = true;
            vaoPickingDirty = true;
            if (structureVao instanceof ModelVAO)
            {
                ((ModelVAO) structureVao).delete();
            }
            structureVao = null;
            if (structureVaoPicking instanceof ModelVAO)
            {
                ((ModelVAO) structureVaoPicking).delete();
            }
            structureVaoPicking = null;
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
        vaoDirty = true;
        vaoPickingDirty = true;
        if (structureVao instanceof ModelVAO)
        {
            ((ModelVAO) structureVao).delete();
        }
        structureVao = null;
        if (structureVaoPicking instanceof ModelVAO)
        {
            ((ModelVAO) structureVaoPicking).delete();
        }
        structureVaoPicking = null;

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

    private void buildStructureVAO()
    {
        // Capturar geometría en un VAO usando el pipeline vanilla pero sustituyendo el consumidor
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        StructureVAOCollector collector = new StructureVAOCollector();

        // Sustituir cualquier consumidor por nuestro colector
        provider.setSubstitute(vc -> collector);

        MatrixStack captureStack = new MatrixStack();
        FormRenderingContext captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

        try
        {
            net.minecraft.client.option.GraphicsMode gm = MinecraftClient.getInstance().options.getGraphicsMode().getValue();
            net.minecraft.client.render.RenderLayers.setFancyGraphicsOrBetter(gm != net.minecraft.client.option.GraphicsMode.FAST);
        }
        catch (Throwable ignored) {}

        boolean useEntityLayers = false; // captura con capas de bloque
        // Evitar renderizar BlockEntities durante la captura para no mezclar atlases.
        this.capturingVAO = true;
        this.capturingIncludeSpecialBlocks = false; // para VAO normal, omitir animados/bioma
        try
        {
            renderStructureCulledWorld(captureContext, captureStack, provider, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, useEntityLayers);
        }
        finally
        {
            this.capturingVAO = false;
            this.capturingIncludeSpecialBlocks = false;
        }

        provider.draw();
        provider.setSubstitute(null);

        ModelVAOData data = collector.toData();
        this.structureVao = new ModelVAO(data);
        this.vaoDirty = false;
    }

    /**
     * Construye un VAO para picking que incluye bloques animados y con tinte por bioma,
     * de modo que la silueta de selección cubra toda la estructura.
     */
    private void buildStructureVAOPicking()
    {
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        StructureVAOCollector collector = new StructureVAOCollector();
        provider.setSubstitute(vc -> collector);

        MatrixStack captureStack = new MatrixStack();
        FormRenderingContext captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

        try
        {
            net.minecraft.client.option.GraphicsMode gm = MinecraftClient.getInstance().options.getGraphicsMode().getValue();
            net.minecraft.client.render.RenderLayers.setFancyGraphicsOrBetter(gm != net.minecraft.client.option.GraphicsMode.FAST);
        }
        catch (Throwable ignored) {}

        boolean useEntityLayers = false;
        this.capturingVAO = true;
        this.capturingIncludeSpecialBlocks = true; // incluir animados y bioma para picking
        try
        {
            renderStructureCulledWorld(captureContext, captureStack, provider, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, useEntityLayers);
        }
        finally
        {
            this.capturingVAO = false;
            this.capturingIncludeSpecialBlocks = false;
        }

        provider.draw();
        provider.setSubstitute(null);

        ModelVAOData data = collector.toData();
        this.structureVaoPicking = new ModelVAO(data);
        this.vaoPickingDirty = false;
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