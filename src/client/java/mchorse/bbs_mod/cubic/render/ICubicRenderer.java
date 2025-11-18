package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.MathUtils;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

public interface ICubicRenderer
{
    public static void translateGroup(MatrixStack stack, ModelGroup group)
    {
        Vector3f translate = group.current.translate;
        Vector3f pivot = group.current.pivot;

        stack.translate(-(translate.x - pivot.x) / 16F, (translate.y - pivot.y) / 16F, (translate.z - pivot.z) / 16F);
    }

    public static void moveToGroupPivot(MatrixStack stack, ModelGroup group)
    {
        Vector3f pivot = group.current.pivot;

        stack.translate(pivot.x / 16F, pivot.y / 16F, pivot.z / 16F);
    }

    public static void rotateGroup(MatrixStack stack, ModelGroup group)
    {
        if (group.current.rotate.z != 0F) stack.multiply(RotationAxis.POSITIVE_Z.rotation(MathUtils.toRad(group.current.rotate.z)));
        if (group.current.rotate.y != 0F) stack.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.toRad(group.current.rotate.y)));
        if (group.current.rotate.x != 0F) stack.multiply(RotationAxis.POSITIVE_X.rotation(MathUtils.toRad(group.current.rotate.x)));

        if (group.current.rotate2.z != 0F) stack.multiply(RotationAxis.POSITIVE_Z.rotation(MathUtils.toRad(group.current.rotate2.z)));
        if (group.current.rotate2.y != 0F) stack.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.toRad(group.current.rotate2.y)));
        if (group.current.rotate2.x != 0F) stack.multiply(RotationAxis.POSITIVE_X.rotation(MathUtils.toRad(group.current.rotate2.x)));
    }

    public static void scaleGroup(MatrixStack stack, ModelGroup group)
    {
        Vector3f scale = group.current.scale;

        MatrixStackUtils.scaleStack(stack, scale.x, scale.y, scale.z);
    }

    public static void moveBackFromGroupPivot(MatrixStack stack, ModelGroup group)
    {
        Vector3f pivot = group.current.pivot;

        stack.translate(-pivot.x / 16F, -pivot.y / 16F, -pivot.z / 16F);
    }

    public default void applyGroupTransformations(MatrixStack stack, ModelGroup group)
    {
        translateGroup(stack, group);
        moveToGroupPivot(stack, group);
        rotateGroup(stack, group);
        scaleGroup(stack, group);
        moveBackFromGroupPivot(stack, group);
    }

    public boolean renderGroup(BufferBuilder builder, MatrixStack stack, ModelGroup group, Model model);
}