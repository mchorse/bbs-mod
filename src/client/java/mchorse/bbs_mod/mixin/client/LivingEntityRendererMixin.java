package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.forms.renderers.MobFormRenderer;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin
{
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V", ordinal = 0, shift = At.Shift.AFTER))
    public void onSetAngles(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info)
    {
        Pose pose = MobFormRenderer.getCurrentPose();
        Pose poseOverlay = MobFormRenderer.getCurrentPoseOverlay();

        if (pose != null)
        {
            pose = pose.copy();

            for (Map.Entry<String, PoseTransform> transformEntry : poseOverlay.transforms.entrySet())
            {
                PoseTransform poseTransform = pose.get(transformEntry.getKey());
                PoseTransform value = transformEntry.getValue();

                if (value.fix != 0)
                {
                    poseTransform.translate.lerp(value.translate, value.fix);
                    poseTransform.scale.lerp(value.scale, value.fix);
                    poseTransform.rotate.lerp(value.rotate, value.fix);
                    poseTransform.rotate2.lerp(value.rotate2, value.fix);
                }
                else
                {
                    poseTransform.translate.add(value.translate);
                    poseTransform.scale.add(value.scale).sub(1, 1, 1);
                    poseTransform.rotate.add(value.rotate);
                    poseTransform.rotate2.add(value.rotate2);
                }
            }

            Map<String, ModelPart> parts = MobFormRenderer.getParts().get(livingEntity.getClass());

            if (parts != null)
            {
                for (Map.Entry<String, ModelPart> entry : parts.entrySet())
                {
                    String key = entry.getKey();
                    ModelPart value = entry.getValue();
                    PoseTransform poseTransform = pose.transforms.get(key);

                    if (poseTransform != null)
                    {
                        Transform transform = new Transform();

                        transform.translate.x = value.pivotX;
                        transform.translate.y = value.pivotY;
                        transform.translate.z = value.pivotZ;
                        transform.rotate.x = value.pitch;
                        transform.rotate.y = value.yaw;
                        transform.rotate.z = value.roll;
                        transform.scale.x = value.xScale;
                        transform.scale.y = value.yScale;
                        transform.scale.z = value.zScale;

                        value.pivotX += poseTransform.translate.x;
                        value.pivotY += poseTransform.translate.y;
                        value.pivotZ += poseTransform.translate.z;
                        value.pitch += poseTransform.rotate.x;
                        value.yaw += poseTransform.rotate.y;
                        value.roll += poseTransform.rotate.z;
                        value.xScale += poseTransform.scale.x - 1F;
                        value.yScale += poseTransform.scale.y - 1F;
                        value.zScale += poseTransform.scale.z - 1F;

                        MobFormRenderer.getCache().put(value, transform);
                    }
                }
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void onRenderEnd(LivingEntity livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo info)
    {
        for (Map.Entry<ModelPart, Transform> entry : MobFormRenderer.getCache().entrySet())
        {
            Transform transform = entry.getValue();
            ModelPart value = entry.getKey();

            value.pivotX = transform.translate.x;
            value.pivotY = transform.translate.y;
            value.pivotZ = transform.translate.z;
            value.pitch = transform.rotate.x;
            value.yaw = transform.rotate.y;
            value.roll = transform.rotate.z;
            value.xScale = transform.scale.x;
            value.yScale = transform.scale.y;
            value.zScale = transform.scale.z;
        }

        MobFormRenderer.getCache().clear();
    }
}