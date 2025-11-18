package mchorse.bbs_mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.mixin.client.EntityRendererDispatcherInvoker;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ModelBlockEntityRenderer implements BlockEntityRenderer<ModelBlockEntity>
{
    private static ActorEntity entity;

    public static void renderShadow(VertexConsumerProvider provider, MatrixStack matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz)
    {
        renderShadow(provider, matrices, tickDelta, x, y, z, tx, ty, tz, 0.5F, 1F);
    }

    public static void renderShadow(VertexConsumerProvider provider, MatrixStack matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz, float radius, float opacity)
    {
        ClientWorld world = MinecraftClient.getInstance().world;

        if (entity == null || entity.getWorld() != world)
        {
            entity = new ActorEntity(BBSMod.ACTOR_ENTITY, world);
        }

        entity.setPos(x, y, z);
        entity.lastRenderX = x;
        entity.lastRenderY = y;
        entity.lastRenderZ = z;
        entity.prevX = x;
        entity.prevY = y;
        entity.prevZ = z;

        double distance = MinecraftClient.getInstance().getEntityRenderDispatcher().getSquaredDistanceToCamera(x, y, z);

        opacity = (float) ((1D - distance / 256D) * opacity);

        matrices.push();
        matrices.translate(tx, ty, tz);

        EntityRendererDispatcherInvoker.bbs$renderShadow(matrices, provider, entity, opacity, tickDelta, entity.getWorld(), radius);

        matrices.pop();
    }

    public ModelBlockEntityRenderer(BlockEntityRendererFactory.Context ctx)
    {}

    @Override
    public boolean rendersOutsideBoundingBox(ModelBlockEntity blockEntity)
    {
        return blockEntity.getProperties().isGlobal();
    }

    @Override
    public void render(ModelBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
    {
        ModelProperties properties = entity.getProperties();
        Transform transform = properties.getTransform();
        BlockPos pos = entity.getPos();
        Pose previousOverlay = null;

        matrices.push();
        matrices.translate(0.5F, 0F, 0.5F);

        if (properties.getForm() != null && this.canRender(entity))
        {
            matrices.push();

            /* If look_at is enabled, align yaw (and optional pitch) toward camera */
            Transform applied = transform;
            if (!properties.isLookAt())
            {
                properties.resetLookYaw();
            }
            if (properties.isLookAt())
            {
                Camera cam = MinecraftClient.getInstance().gameRenderer.getCamera();
                Vec3d camPos = cam.getPos();
                Vec3d targetPos = camPos;
                try
                {
                    if (!MinecraftClient.getInstance().options.getPerspective().isFirstPerson())
                    {
                        if (MinecraftClient.getInstance().player != null)
                        {
                            targetPos = MinecraftClient.getInstance().player.getCameraPosVec(tickDelta);
                        }
                    }
                }
                catch (Exception ignore) {}

                double ox = pos.getX() + 0.5D + transform.translate.x;
                double oy = pos.getY() + transform.translate.y;
                double oz = pos.getZ() + 0.5D + transform.translate.z;

                double dx = targetPos.x - ox;
                double dz = targetPos.z - oz;
                double horiz = Math.sqrt(dx * dx + dz * dz);

                float baseYaw = transform.rotate.y;
                float yawAbs = (float) Math.atan2(dx, dz);
                float yawCont = properties.updateLookYawContinuous(yawAbs);
                float yawDelta = yawCont - baseYaw;
                /* Distancia recorrida 0–360° independiente de dirección para sincronización */
                float twoPi = (float) (Math.PI * 2);
                float travel = Math.abs(yawDelta);
                while (travel >= twoPi) { travel -= twoPi; }
                /* Usar delta continuo para reparto cabeza/anchor (evita salto en 180°) */

                applied = transform.copy();

                /* If the form has bones and look_at is configured, split rotation */
                if (properties.getForm() instanceof ModelForm modelForm)
                {
                    java.util.List<String> bones = FormUtilsClient.getBones(modelForm);
                    mchorse.bbs_mod.cubic.ModelInstance mi = mchorse.bbs_mod.forms.renderers.ModelFormRenderer.getModel(modelForm);
                    String headKey = mi != null ? mi.lookAtHeadBone : "head";
                    String anchorKey = mi != null ? mi.lookAtAnchorBone : "anchor";
                    boolean hasHead = bones.contains(headKey);
                    boolean hasAnchor = bones.contains(anchorKey);

                    boolean lookAtEnabled = mi != null && mi.lookAtConfigured;

                    if (lookAtEnabled && hasHead)
                    {
                        /* Calcular pitch basado en altura real del hueso cabeza */
                        float approxHeadHeight = 1.5F * applied.scale.y;
                        try
                        {
                            java.util.Map<String, Matrix4f> mats = new java.util.HashMap<>();
                            mi.captureMatrices(mats, headKey);
                            Matrix4f mat = mats.get(headKey);
                            if (mat != null)
                            {
                                Vector3f tr = new Vector3f();
                                mat.getTranslation(tr);
                                approxHeadHeight = tr.y * applied.scale.y;
                            }
                        }
                        catch (Exception ignore) {}

                        double dyHead = targetPos.y - (oy + approxHeadHeight);
                        float pitch = (float) Math.atan2(dyHead, horiz);
                        float pitchLimit = (float) Math.toRadians(90.0);
                        if (pitch > pitchLimit) pitch = pitchLimit;
                        if (pitch < -pitchLimit) pitch = -pitchLimit;

                        float headLimit = (float) Math.toRadians(mi != null ? mi.lookAtHeadLimitDeg : 45.0F);
                        float headYawBase = Math.max(-headLimit, Math.min(yawDelta, headLimit));

                        /* Sincronizar 315–360°: desvanecer cabeza a 0 */
                        float syncStart = (float) Math.toRadians(315.0);
                        float syncRange = (float) Math.toRadians(45.0);
                        float t = 0F;
                        if (travel >= syncStart) {
                            t = Math.min(1F, (travel - syncStart) / syncRange);
                        }
                        float headYaw = headYawBase * (1F - t);

                        /* Anchor toma el resto del delta continuo */
                        float anchorYaw = yawDelta - headYaw;

                        /* Cerca de 360°: reset visual a 0 para ambos y rebase continuo */
                        if (travel >= (float) Math.toRadians(359.0)) {
                            headYaw = 0F;
                            anchorYaw = 0F;
                            properties.snapLookYawToBase(yawAbs, baseYaw);
                        }

                        Pose overlayPose = new Pose();
                        PoseTransform head = overlayPose.get(headKey);
                        head.fix = 1F;
                        head.rotate.y = headYaw;
                        head.rotate.x = (mi == null || mi.lookAtAllowPitch) ? pitch : 0F;

                        if (hasAnchor)
                        {
                            PoseTransform anchor = overlayPose.get(anchorKey);
                            anchor.fix = 1F;
                            anchor.rotate.y = anchorYaw;
                        }
                        else
                        {
                            /* Sin anchor: aplicar el resto al giro global del cuerpo */
                            applied.rotate.y = baseYaw + anchorYaw;
                        }

                        // Temporarily override pose overlay for this render
                        previousOverlay = modelForm.poseOverlay.get().copy();
                        modelForm.poseOverlay.set(overlayPose);
                    }
                }

                /* If we didn't apply a head/anchor overlay, rotate globally using continuous yaw */
                if (previousOverlay == null)
                {
                    applied.rotate.y = yawCont;
                }
            }

            MatrixStackUtils.applyTransform(matrices, applied);

            int lightAbove = WorldRenderer.getLightmapCoordinates(entity.getWorld(), pos.add((int) transform.translate.x, (int) transform.translate.y, (int) transform.translate.z));
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

            RenderSystem.enableDepthTest();
            FormUtilsClient.render(properties.getForm(), new FormRenderingContext()
                .set(FormRenderType.MODEL_BLOCK, entity.getEntity(), matrices, lightAbove, overlay, tickDelta)
                .camera(camera));
            RenderSystem.disableDepthTest();

            if (this.canRenderAxes(entity) && UIBaseMenu.renderAxes)
            {
                matrices.push();
                MatrixStackUtils.scaleBack(matrices);
                Draw.coolerAxes(matrices, 0.5F, 0.01F, 0.51F, 0.02F);
                matrices.pop();
            }

            matrices.pop();
        }

        RenderSystem.disableDepthTest();

        if (MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud())
        {
            Draw.renderBox(matrices, -0.5D, 0, -0.5D, 1, 1, 1, 0, 0.5F, 1F, 0.5F);
        }

        matrices.pop();

        if (properties.isShadow())
        {
            float tx = 0.5F + transform.translate.x;
            float ty = transform.translate.y;
            float tz = 0.5F + transform.translate.z;
            double x = pos.getX() + tx;
            double y = pos.getY() + ty;
            double z = pos.getZ() + tz;

            renderShadow(vertexConsumers, matrices, tickDelta, x, y, z, tx, ty, tz);
        }

        /* Restore pose overlay if it was temporarily overridden */
        if (previousOverlay != null && properties.getForm() instanceof ModelForm modelForm)
        {
            modelForm.poseOverlay.set(previousOverlay);
        }
    }

    @Override
    public int getRenderDistance()
    {
        return 196;
    }

    private boolean canRenderAxes(ModelBlockEntity entity)
    {
        if (UIScreen.getCurrentMenu() instanceof UIDashboard dashboard)
        {
            return dashboard.getPanels().panel instanceof UIModelBlockPanel modelBlockPanel;
        }

        return false;
    }

    private boolean canRender(ModelBlockEntity entity)
    {
        if (!entity.getProperties().isEnabled())
        {
            return false;
        }

        if (!BBSSettings.renderAllModelBlocks.get())
        {
            return false;
        }

        if (UIScreen.getCurrentMenu() instanceof UIDashboard dashboard)
        {
            if (dashboard.getPanels().panel instanceof UIModelBlockPanel modelBlockPanel)
            {
                return !modelBlockPanel.isEditing(entity) || UIModelBlockPanel.toggleRendering;
            }
        }

        return true;
    }
}