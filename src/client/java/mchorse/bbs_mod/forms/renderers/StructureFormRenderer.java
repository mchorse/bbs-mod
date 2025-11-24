package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.IModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAOData;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.cubic.render.vao.StructureVAOCollector;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class StructureFormRenderer extends FormRenderer<StructureForm>
{
    private final List<BlockEntry> blocks = new ArrayList<>();
    private String lastFile = null;

    private BlockPos size = BlockPos.ORIGIN;
    private BlockPos boundsMin = null;
    private BlockPos boundsMax = null;

    private IModelVAO structureVao = null;
    private boolean vaoDirty = true;
    private boolean capturingVAO = false;
    private IModelVAO structureVaoPicking = null;
    private boolean vaoPickingDirty;
    private boolean capturingIncludeSpecialBlocks;

    public StructureFormRenderer(StructureForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.getContext().draw();

        this.ensureLoaded();

        MatrixStack matrices = context.batcher.getContext().getMatrices();
        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();

        MatrixStackUtils.multiply(matrices, uiMatrix);
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);

        float cellW = x2 - x1;
        float cellH = y2 - y1;
        float baseScale = cellH / 2.5F;

        int w = Math.max(1, this.size.getX());
        int h = Math.max(1, this.size.getY());
        int d = Math.max(1, this.size.getZ());

        if (this.boundsMin != null && this.boundsMax != null)
        {
            w = Math.max(1, this.boundsMax.getX() - this.boundsMin.getX() + 1);
            h = Math.max(1, this.boundsMax.getY() - this.boundsMin.getY() + 1);
            d = Math.max(1, this.boundsMax.getZ() - this.boundsMin.getZ() + 1);
        }

        int maxUnits = Math.max(w, Math.max(h, d));
        float targetPixels = Math.min(cellW, cellH) * 0.9F;
        float auto = targetPixels / (baseScale * maxUnits);
        float finalScale = this.form.uiScale.get() * Math.min(1F, auto);
        matrices.scale(finalScale, finalScale, finalScale);

        matrices.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
        matrices.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

        boolean optimize = BBSSettings.structureOptimization.get();
        boolean lightsEnabled = this.form.emitLight.get();

        if (lightsEnabled)
        {
            optimize = false;
        }

        if (!optimize)
        {
            boolean shaders = BBSRendering.isIrisShadersEnabled();
            VertexConsumerProvider consumers = shaders
                ? MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
                : VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

            try
            {
                FormRenderingContext uiContext = new FormRenderingContext().set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                this.renderStructureCulledWorld(uiContext, matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, shaders);

                if (consumers instanceof VertexConsumerProvider.Immediate immediate)
                {
                    immediate.draw();
                }
            }
            catch (Throwable ignored) {}
        }
        else
        {
            if (this.structureVao == null || this.vaoDirty)
            {
                buildStructureVAO();
            }

            if (this.structureVao != null)
            {
                Color tint = this.form.color.get();
                GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

                gameRenderer.getLightmapTextureManager().enable();
                gameRenderer.getOverlayTexture().setupOverlayColor();

                ShaderProgram shader = BBSShaders.getModel();

                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                ModelVAORenderer.render(shader, this.structureVao, matrices, tint.r, tint.g, tint.b, tint.a, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

                try
                {
                    VertexConsumerProvider beConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                    FormRenderingContext beContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                    this.renderBlockEntitiesOnly(beContext, matrices, beConsumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

                    if (beConsumers instanceof net.minecraft.client.render.VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }
                }
                catch (Throwable ignored) {}

                try
                {
                    boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                    VertexConsumerProvider consumersTint = shadersEnabled
                        ? MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
                        : VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

                    FormRenderingContext tintContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                    this.renderBiomeTintedBlocksVanilla(tintContext, matrices, consumersTint, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

                    if (consumersTint instanceof net.minecraft.client.render.VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }
                }
                catch (Throwable ignored) {}

                try
                {
                    boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
                    VertexConsumerProvider consumersAnim = shadersEnabled
                        ? MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
                        : VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

                    FormRenderingContext animContext = new FormRenderingContext()
                        .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

                    this.renderAnimatedBlocksVanilla(animContext, matrices, consumersAnim, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

                    if (consumersAnim instanceof VertexConsumerProvider.Immediate immediate)
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

        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_ALWAYS);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        ensureLoaded();
        context.stack.push();

        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
        boolean optimize = BBSSettings.structureOptimization.get();
        boolean picking = context.isPicking();

        if (optimize && (this.structureVao == null || this.vaoDirty))
        {
            buildStructureVAO();
        }

        if (!optimize)
        {
            if (picking)
            {
                if (this.structureVaoPicking == null || this.vaoPickingDirty)
                {
                    this.buildStructureVAOPicking();
                }

                Color tint3D = this.form.color.get();
                int light = 0;

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
                int light = context.light;
                boolean shaders = BBSRendering.isIrisShadersEnabled();
                VertexConsumerProvider consumers = shaders
                    ? MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
                    : VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

                gameRenderer.getLightmapTextureManager().enable();
                gameRenderer.getOverlayTexture().setupOverlayColor();

                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

                try
                {
                    this.renderStructureCulledWorld(context, context.stack, consumers, light, context.overlay, shaders);

                    if (consumers instanceof VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }
                }
                catch (Throwable ignored) {}

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

            gameRenderer.getLightmapTextureManager().enable();
            gameRenderer.getOverlayTexture().setupOverlayColor();

            if (context.isPicking())
            {
                if (this.structureVaoPicking == null || this.vaoPickingDirty)
                {
                    this.buildStructureVAOPicking();
                }

                this.setupTarget(context, BBSShaders.getPickerModelsProgram());

                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
                RenderSystem.enableBlend();
                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
                ModelVAORenderer.render(BBSShaders.getPickerModelsProgram(), this.structureVaoPicking, context.stack, tint3D.r, tint3D.g, tint3D.b, tint3D.a, light, context.overlay);
            }
            else
            {
                ShaderProgram shader = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld()
                    ? GameRenderer.getRenderTypeEntityTranslucentCullProgram()
                    : BBSShaders.getModel();

                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                ModelVAORenderer.render(shader, this.structureVao, context.stack, tint3D.r, tint3D.g, tint3D.b, tint3D.a, light, context.overlay);

                try
                {
                    VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

                    this.renderBlockEntitiesOnly(context, context.stack, consumers, light, context.overlay);

                    if (consumers instanceof VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }
                }
                catch (Throwable e) {}

                try
                {
                    VertexConsumerProvider.Immediate tintConsumers = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

                    this.renderBiomeTintedBlocksVanilla(context, context.stack, tintConsumers, light, context.overlay);
                    tintConsumers.draw();
                }
                catch (Throwable e)
                {}

                try
                {
                    VertexConsumerProvider.Immediate animConsumers = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

                    renderAnimatedBlocksVanilla(context, context.stack, animConsumers, light, context.overlay);
                    animConsumers.draw();
                }
                catch (Throwable e)
                {}
            }

            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();

            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        }

        CustomVertexConsumerProvider.clearRunnables();
        context.stack.pop();
    }

    private void renderStructureCulledWorld(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay, boolean useEntityLayers)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        float cx;
        float cy;
        float cz;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            cx = (this.boundsMin.getX() + this.boundsMax.getX()) / 2F;
            cz = (this.boundsMin.getZ() + this.boundsMax.getZ()) / 2F;
            cy = this.boundsMin.getY();
        }
        else
        {
            cx = this.size.getX() / 2F;
            cy = 0F;
            cz = this.size.getZ() / 2F;
        }

        float parityXAuto2 = 0F;
        float parityZAuto2 = 0F;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            int widthX = this.boundsMax.getX() - this.boundsMin.getX() + 1;
            int widthZ = this.boundsMax.getZ() - this.boundsMin.getZ() + 1;

            parityXAuto2 = (widthX % 2 == 1) ? -0.5F : 0f;
            parityZAuto2 = (widthZ % 2 == 1) ? -0.5F : 0f;
        }

        float pivotX = cx - parityXAuto2;
        float pivotY = cy;
        float pivotZ = cz - parityZAuto2;

        List<VirtualBlockRenderView.Entry> entries = new ArrayList<>();

        for (BlockEntry block : this.blocks)
        {
            entries.add(new VirtualBlockRenderView.Entry(block.state, block.pos));
        }

        boolean lightsEnabled = this.form.emitLight.get();
        int lightIntensity = this.form.lightIntensity.get();

        VirtualBlockRenderView view = new VirtualBlockRenderView(entries)
            .setBiomeOverride(this.form.biomeId.get())
            .setLightsEnabled(lightsEnabled)
            .setLightIntensity(lightIntensity);

        BlockEntityRenderDispatcher renderDispatcher = mc.getBlockEntityRenderDispatcher();
        BlockPos anchor = BlockPos.ORIGIN;

        boolean isItemContext = (context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_FP
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_TP
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_INVENTORY);

        if (isItemContext || context.entity == null)
        {
            if (mc.player != null)
            {
                anchor = mc.player.getBlockPos();
            }
        }
        else
        {
            anchor = new BlockPos(
                (int) Math.floor(context.entity.getX()),
                (int) Math.floor(context.entity.getY()),
                (int) Math.floor(context.entity.getZ())
            );
        }

        int baseDx = (int) Math.floor(-pivotX);
        int baseDy = (int) Math.floor(-pivotY);
        int baseDz = (int) Math.floor(-pivotZ);

        view.setWorldAnchor(anchor, baseDx, baseDy, baseDz).setForceMaxSkyLight(context.ui
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.PREVIEW
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_INVENTORY);

        for (BlockEntry entry : this.blocks)
        {
            stack.push();
            stack.translate(entry.pos.getX() - pivotX, entry.pos.getY() - pivotY, entry.pos.getZ() - pivotZ);

            if (this.capturingVAO && !this.capturingIncludeSpecialBlocks && (this.isAnimatedTexture(entry.state) || isBiomeTinted(entry.state)))
            {
                stack.pop();

                continue;
            }

            RenderLayer layer = useEntityLayers
                ? RenderLayers.getEntityBlockLayer(entry.state, false)
                : RenderLayers.getBlockLayer(entry.state);

            float globalAlpha = this.form.color.get().a;

            if (globalAlpha < 0.999F)
            {
                layer = useEntityLayers
                    ? TexturedRenderLayers.getEntityTranslucentCull()
                    : RenderLayer.getTranslucent();
            }

            VertexConsumer vc = consumers.getBuffer(layer);
            Color tint = this.form.color.get();
            Function<VertexConsumer, VertexConsumer> recolor = BBSRendering.getColorConsumer(tint);

            vc = recolor.apply(vc);

            mc.getBlockRenderManager().renderBlock(entry.state, entry.pos, view, stack, vc, true, Random.create());

            Block block = entry.state.getBlock();

            if (!this.capturingVAO && block instanceof BlockEntityProvider)
            {
                int dx = (int) Math.floor(entry.pos.getX() - pivotX);
                int dy = (int) Math.floor(entry.pos.getY() - pivotY);
                int dz = (int) Math.floor(entry.pos.getZ() - pivotZ);
                BlockPos worldPos = anchor.add(dx, dy, dz);
                BlockEntity be = ((BlockEntityProvider) block).createBlockEntity(worldPos, entry.state);

                if (be != null)
                {
                    if (mc.world != null)
                    {
                        be.setWorld(mc.world);
                    }

                    BlockEntityRenderer<?> renderer = renderDispatcher.get(be);

                    int skyLight = view.getLightLevel(net.minecraft.world.LightType.SKY, entry.pos);
                    int blockLight = view.getLightLevel(net.minecraft.world.LightType.BLOCK, entry.pos);
                    int beLight = LightmapTextureManager.pack(blockLight, skyLight);

                    if (renderer != null)
                    {
                        BlockEntityRenderer raw = renderer;
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
            }

            stack.pop();
        }

        RecolorVertexConsumer.newColor = null;
    }

    private void renderAnimatedBlocksVanilla(FormRenderingContext context, MatrixStack stack, net.minecraft.client.render.VertexConsumerProvider consumers, int light, int overlay)
    {
        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        float cx;
        float cy;
        float cz;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            cx = (this.boundsMin.getX() + this.boundsMax.getX()) / 2F;
            cz = (this.boundsMin.getZ() + this.boundsMax.getZ()) / 2F;
            cy = this.boundsMin.getY();
        }
        else
        {
            cx = size.getX() / 2F;
            cy = 0F;
            cz = size.getZ() / 2F;
        }

        float parityXAuto3 = 0F;
        float parityZAuto3 = 0F;

        if (this.boundsMin != null && boundsMax != null)
        {
            int widthX = boundsMax.getX() - boundsMin.getX() + 1;
            int widthZ = boundsMax.getZ() - boundsMin.getZ() + 1;
            parityXAuto3 = (widthX % 2 == 1) ? -0.5F : 0F;
            parityZAuto3 = (widthZ % 2 == 1) ? -0.5F : 0F;
        }

        float pivotX = cx - parityXAuto3;
        float pivotY = cy;
        float pivotZ = cz - parityZAuto3;

        List<VirtualBlockRenderView.Entry> entries = new ArrayList<>();

        for (BlockEntry be : blocks)
        {
            entries.add(new VirtualBlockRenderView.Entry(be.state, be.pos));
        }

        boolean lightsEnabled2 = this.form.emitLight.get();
        int lightIntensity2 = this.form.lightIntensity.get();

        VirtualBlockRenderView view = new VirtualBlockRenderView(entries)
            .setBiomeOverride(this.form.biomeId.get())
            .setLightsEnabled(lightsEnabled2)
            .setLightIntensity(lightIntensity2);

        BlockPos anchor;
        boolean isItemContextAnim = (context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_FP
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_TP
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_INVENTORY);

        if (isItemContextAnim || context.entity == null)
        {
            MinecraftClient mc2 = MinecraftClient.getInstance();
            anchor = (mc2.player != null) ? mc2.player.getBlockPos() : net.minecraft.util.math.BlockPos.ORIGIN;
        }
        else
        {
            anchor = new net.minecraft.util.math.BlockPos(
                (int) Math.floor(context.entity.getX()),
                (int) Math.floor(context.entity.getY()),
                (int) Math.floor(context.entity.getZ())
            );
        }

        int baseDx = (int) Math.floor(-pivotX);
        int baseDy = (int) Math.floor(-pivotY);
        int baseDz = (int) Math.floor(-pivotZ);
        view.setWorldAnchor(anchor, baseDx, baseDy, baseDz)
            .setForceMaxSkyLight(context.ui
                || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.PREVIEW
                || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_INVENTORY);

        for (BlockEntry entry : blocks)
        {
            if (!isAnimatedTexture(entry.state))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - pivotX, entry.pos.getY() - pivotY, entry.pos.getZ() - pivotZ);

            boolean shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
            RenderLayer layer = shadersEnabled
                ? RenderLayers.getEntityBlockLayer(entry.state, true)
                : RenderLayer.getTranslucentMovingBlock();

            float globalAlphaAnim = this.form.color.get().a;

            if (globalAlphaAnim < 0.999F)
            {
                layer = shadersEnabled ? TexturedRenderLayers.getEntityTranslucentCull() : RenderLayer.getTranslucentMovingBlock();
            }

            VertexConsumer vc = consumers.getBuffer(layer);
            Color tint = this.form.color.get();
            Function<VertexConsumer, VertexConsumer> recolor = BBSRendering.getColorConsumer(tint);

            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, view, stack, vc, true, Random.create());
            stack.pop();
        }

        RecolorVertexConsumer.newColor = null;
    }

    private void renderBiomeTintedBlocksVanilla(FormRenderingContext context, MatrixStack stack, net.minecraft.client.render.VertexConsumerProvider consumers, int light, int overlay)
    {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

        float cx = this.size.getX() / 2F;
        float cy = 0F;
        float cz = this.size.getZ() / 2F;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            cx = (this.boundsMin.getX() + this.boundsMax.getX()) / 2f;
            cz = (this.boundsMin.getZ() + this.boundsMax.getZ()) / 2f;
            cy = this.boundsMin.getY();
        }

        float parityXAuto4 = 0F;
        float parityZAuto4 = 0F;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            int widthX = this.boundsMax.getX() - this.boundsMin.getX() + 1;
            int widthZ = this.boundsMax.getZ() - this.boundsMin.getZ() + 1;

            parityXAuto4 = (widthX % 2 == 1) ? -0.5f : 0f;
            parityZAuto4 = (widthZ % 2 == 1) ? -0.5f : 0f;
        }

        float pivotX = cx - parityXAuto4;
        float pivotY = cy;
        float pivotZ = cz - parityZAuto4;

        List<VirtualBlockRenderView.Entry> entries = new ArrayList<>();

        for (BlockEntry be : blocks)
        {
            entries.add(new VirtualBlockRenderView.Entry(be.state, be.pos));
        }

        VirtualBlockRenderView view = new VirtualBlockRenderView(entries)
            .setBiomeOverride(this.form.biomeId.get())
            .setLightsEnabled(this.form.emitLight.get())
            .setLightIntensity(this.form.lightIntensity.get());

        BlockPos anchor;
        boolean isItemContextTint = context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_FP
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_TP
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_INVENTORY;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (isItemContextTint || context.entity == null)
        {
            MinecraftClient mc3 = mc;

            anchor = (mc3.player != null) ? mc3.player.getBlockPos() : net.minecraft.util.math.BlockPos.ORIGIN;
        }
        else
        {
            anchor = new net.minecraft.util.math.BlockPos(
                (int) Math.floor(context.entity.getX()),
                (int) Math.floor(context.entity.getY()),
                (int) Math.floor(context.entity.getZ())
            );
        }

        int baseDx = (int) Math.floor(-pivotX);
        int baseDy = (int) Math.floor(-pivotY);
        int baseDz = (int) Math.floor(-pivotZ);
        view.setWorldAnchor(anchor, baseDx, baseDy, baseDz).setForceMaxSkyLight(context.ui
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.PREVIEW
            || context.type == mchorse.bbs_mod.forms.renderers.FormRenderType.ITEM_INVENTORY);

        for (BlockEntry entry : blocks)
        {
            if (!isBiomeTinted(entry.state))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - pivotX, entry.pos.getY() - pivotY, entry.pos.getZ() - pivotZ);

            boolean shadersEnabledTint = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
            RenderLayer layer = shadersEnabledTint
                ? RenderLayers.getEntityBlockLayer(entry.state, false)
                : RenderLayers.getBlockLayer(entry.state);

            float globalAlpha = this.form.color.get().a;

            if (globalAlpha < 0.999F)
            {
                layer = shadersEnabledTint ? TexturedRenderLayers.getEntityTranslucentCull() : RenderLayer.getTranslucent();
            }

            Color tint = this.form.color.get();
            VertexConsumer consumer = BBSRendering.getColorConsumer(tint).apply(consumers.getBuffer(layer));

            mc.getBlockRenderManager().renderBlock(entry.state, entry.pos, view, stack, consumer, true, Random.create());
            stack.pop();
        }

        RenderSystem.disableBlend();

        RecolorVertexConsumer.newColor = null;
    }

    private boolean isAnimatedTexture(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        if (state.isOf(Blocks.NETHER_PORTAL))
        {
            return true;
        }

        FluidState fs = state.getFluidState();

        if (fs != null)
        {
            return fs.getFluid() == Fluids.WATER || fs.getFluid() == Fluids.FLOWING_WATER
                || fs.getFluid() == Fluids.LAVA || fs.getFluid() == Fluids.FLOWING_LAVA;
        }

        return false;
    }

    private boolean isBiomeTinted(BlockState state)
    {
        if (state == null)
        {
            return false;
        }

        Block b = state.getBlock();

        return b instanceof LeavesBlock
            || b instanceof GrassBlock
            || b instanceof VineBlock
            || b instanceof LilyPadBlock;
    }

    private void renderBlockEntitiesOnly(FormRenderingContext context, MatrixStack stack, net.minecraft.client.render.VertexConsumerProvider consumers, int light, int overlay)
    {
        float cx = this.size.getX() / 2F;
        float cy = 0F;
        float cz = this.size.getZ() / 2F;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            cx = (this.boundsMin.getX() + this.boundsMax.getX()) / 2F;
            cz = (this.boundsMin.getZ() + this.boundsMax.getZ()) / 2F;
            cy = this.boundsMin.getY();
        }

        float pivotX = this.form.pivotX.get();
        float pivotY = this.form.pivotY.get();
        float pivotZ = this.form.pivotZ.get();
        boolean useAuto = this.form.autoPivot.get();

        if (useAuto)
        {
            float parityXAuto = 0F;
            float parityZAuto = 0F;

            if (this.boundsMin != null && boundsMax != null)
            {
                int widthX = boundsMax.getX() - boundsMin.getX() + 1;
                int widthZ = boundsMax.getZ() - boundsMin.getZ() + 1;
                parityXAuto = (widthX % 2 == 1) ? -0.5F : 0F;
                parityZAuto = (widthZ % 2 == 1) ? -0.5F : 0F;
            }

            pivotX = cx - parityXAuto;
            pivotY = cy;
            pivotZ = cz - parityZAuto;
        }

        BlockPos anchor = BlockPos.ORIGIN;

        if (context.entity != null)
        {
            anchor = new net.minecraft.util.math.BlockPos(
                (int) Math.floor(context.entity.getX()),
                (int) Math.floor(context.entity.getY()),
                (int) Math.floor(context.entity.getZ())
            );
        }

        BlockEntityRenderDispatcher renderDispatcher = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

        for (BlockEntry entry : blocks)
        {
            Block block = entry.state.getBlock();

            if (!(block instanceof BlockEntityProvider))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - pivotX, entry.pos.getY() - pivotY, entry.pos.getZ() - pivotZ);

            int dx = (int) Math.floor(entry.pos.getX() - pivotX);
            int dy = (int) Math.floor(entry.pos.getY() - pivotY);
            int dz = (int) Math.floor(entry.pos.getZ() - pivotZ);

            BlockPos worldPos = anchor.add(dx, dy, dz);
            BlockEntity blockEntitye = ((BlockEntityProvider) block).createBlockEntity(worldPos, entry.state);

            if (blockEntitye != null)
            {
                if (MinecraftClient.getInstance().world != null)
                {
                    blockEntitye.setWorld(MinecraftClient.getInstance().world);
                }

                BlockEntityRenderer<?> renderer = renderDispatcher.get(blockEntitye);
                List<VirtualBlockRenderView.Entry> entries = new ArrayList<>();

                for (BlockEntry beEntry : blocks)
                {
                    entries.add(new VirtualBlockRenderView.Entry(beEntry.state, beEntry.pos));
                }

                boolean lightsEnabledBE = this.form.emitLight.get();
                int lightIntensityBE = this.form.lightIntensity.get();

                VirtualBlockRenderView beView = new VirtualBlockRenderView(entries)
                    .setBiomeOverride(this.form.biomeId.get())
                    .setLightsEnabled(lightsEnabledBE)
                    .setLightIntensity(lightIntensityBE)
                    .setWorldAnchor(anchor, (int) Math.floor(-pivotX), (int) Math.floor(-pivotY), (int) Math.floor(-pivotZ));

                int skyLight = beView.getLightLevel(net.minecraft.world.LightType.SKY, entry.pos);
                int blockLight = beView.getLightLevel(net.minecraft.world.LightType.BLOCK, entry.pos);
                int beLight = LightmapTextureManager.pack(blockLight, skyLight);

                if (renderer != null)
                {
                    CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();

                    provider.setSubstitute(BBSRendering.getColorConsumer(this.form.color.get()));

                    try
                    {
                        ((BlockEntityRenderer) renderer).render(blockEntitye, 0F, stack, provider, beLight, overlay);
                    }
                    finally
                    {
                        provider.draw();
                        provider.setSubstitute(null);
                        CustomVertexConsumerProvider.clearRunnables();
                    }
                }
            }

            stack.pop();
        }
    }

    private void ensureLoaded()
    {
        String file = this.form.structureFile.get();

        if (file == null || file.isEmpty())
        {
            this.blocks.clear();
            this.size = BlockPos.ORIGIN;
            this.boundsMin = null;
            this.boundsMax = null;
            this.lastFile = null;
            this.vaoDirty = true;
            this.vaoPickingDirty = true;

            if (this.structureVao instanceof ModelVAO vao)
            {
                vao.delete();
            }

            this.structureVao = null;

            if (this.structureVaoPicking instanceof ModelVAO vao)
            {
                vao.delete();
            }

            this.structureVaoPicking = null;

            return;
        }

        if (file.equals(this.lastFile))
        {
            return;
        }

        File nbtFile = BBSMod.getProvider().getFile(Link.assets(file));

        this.blocks.clear();
        this.size = BlockPos.ORIGIN;
        this.boundsMin = null;
        this.boundsMax = null;
        this.lastFile = file;
        this.vaoDirty = true;
        this.vaoPickingDirty = true;

        if (this.structureVao instanceof ModelVAO vao)
        {
            vao.delete();
        }

        this.structureVao = null;

        if (this.structureVaoPicking instanceof ModelVAO vao)
        {
            vao.delete();
        }

        this.structureVaoPicking = null;

        if (nbtFile != null && nbtFile.exists())
        {
            try
            {
                this.parseStructure(NbtIo.readCompressed(nbtFile.toPath(), NbtTagSizeTracker.ofUnlimitedBytes()));

                return;
            }
            catch (IOException e)
            {}
        }

        try (InputStream is = BBSMod.getProvider().getAsset(Link.assets(file)))
        {
            this.parseStructure(NbtIo.readCompressed(is, NbtTagSizeTracker.ofUnlimitedBytes()));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void buildStructureVAO()
    {
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        StructureVAOCollector collector = new StructureVAOCollector();

        provider.setSubstitute((consumer) -> collector);

        MatrixStack captureStack = new MatrixStack();
        FormRenderingContext captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

        try
        {
            GraphicsMode gm = MinecraftClient.getInstance().options.getGraphicsMode().getValue();
            RenderLayers.setFancyGraphicsOrBetter(gm != GraphicsMode.FAST);
        }
        catch (Throwable ignored)
        {}

        boolean useEntityLayers = false;

        this.capturingVAO = true;
        this.capturingIncludeSpecialBlocks = false;

        try
        {
            this.renderStructureCulledWorld(captureContext, captureStack, provider, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, useEntityLayers);
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

    private void buildStructureVAOPicking()
    {
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        StructureVAOCollector collector = new StructureVAOCollector();
        provider.setSubstitute(vc -> collector);

        MatrixStack captureStack = new MatrixStack();
        FormRenderingContext captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

        MinecraftClient mc = MinecraftClient.getInstance();

        try
        {
            GraphicsMode gm = mc.options.getGraphicsMode().getValue();

            RenderLayers.setFancyGraphicsOrBetter(gm != net.minecraft.client.option.GraphicsMode.FAST);
        }
        catch (Throwable ignored)
        {}

        boolean useEntityLayers = false;

        this.capturingVAO = true;
        this.capturingIncludeSpecialBlocks = true;

        try
        {
            this.renderStructureCulledWorld(captureContext, captureStack, provider, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, useEntityLayers);
        }
        finally
        {
            this.capturingVAO = false;
            this.capturingIncludeSpecialBlocks = false;
        }

        provider.draw();
        provider.setSubstitute(null);

        this.structureVaoPicking = new ModelVAO(collector.toData());
        this.vaoPickingDirty = false;
    }

    private void parseStructure(NbtCompound root)
    {
        if (root.contains("size", NbtElement.INT_ARRAY_TYPE))
        {
            int[] size = root.getIntArray("size");

            if (size.length >= 3)
            {
                this.size = new BlockPos(size[0], size[1], size[2]);
            }
        }

        List<BlockState> paletteStates = new ArrayList<>();

        if (root.contains("palette", NbtElement.LIST_TYPE))
        {
            NbtList palette = root.getList("palette", NbtElement.COMPOUND_TYPE);

            for (int i = 0; i < palette.size(); i++)
            {
                paletteStates.add(this.readBlockState(palette.getCompound(i)));
            }
        }

        if (root.contains("blocks", NbtElement.LIST_TYPE))
        {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            NbtList list = root.getList("blocks", NbtElement.COMPOUND_TYPE);

            for (int i = 0; i < list.size(); i++)
            {
                NbtCompound be = list.getCompound(i);
                BlockPos pos = readBlockPos(be.getList("pos", NbtElement.INT_TYPE));
                int stateIndex = be.getInt("state");

                if (stateIndex >= 0 && stateIndex < paletteStates.size())
                {
                    BlockState state = paletteStates.get(stateIndex);

                    this.blocks.add(new BlockEntry(state, pos));

                    minX = Math.min(minX, pos.getX());
                    minY = Math.min(minY, pos.getY());
                    minZ = Math.min(minZ, pos.getZ());
                    maxX = Math.max(maxX, pos.getX());
                    maxY = Math.max(maxY, pos.getY());
                    maxZ = Math.max(maxZ, pos.getZ());
                }
            }

            if (!this.blocks.isEmpty())
            {
                this.boundsMin = new BlockPos(minX, minY, minZ);
                this.boundsMax = new BlockPos(maxX, maxY, maxZ);
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
        Block block = Registries.BLOCK.get(new Identifier(name));;
        BlockState state = block.getDefaultState();

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
                            Property raw = property;
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

    private static class BlockEntry
    {
        final BlockState state;
        final BlockPos pos;

        public BlockEntry(BlockState state, BlockPos pos)
        {
            this.state = state;
            this.pos = pos;
        }
    }
}