package mchorse.bbs_mod.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MatrixStackUtils
{
    private static Matrix3f normal = new Matrix3f();

    private static Matrix4f oldProjection = new Matrix4f();
    private static Matrix4f oldMV = new Matrix4f();
    private static Matrix3f oldInverse = new Matrix3f();

    public static void scaleStack(MatrixStack stack, float x, float y, float z)
    {
        stack.peek().getPositionMatrix().scale(x, y, z);
        stack.peek().getNormalMatrix().scale(x < 0F ? -1F : 1F, y < 0F ? -1F : 1F, z < 0F ? -1F : 1F);
    }

    public static void cacheMatrices()
    {
        /* Cache the global stuff */
        oldProjection.set(RenderSystem.getProjectionMatrix());
        oldMV.set(RenderSystem.getModelViewMatrix());
        oldInverse.set(RenderSystem.getInverseViewRotationMatrix());

        MatrixStack renderStack = RenderSystem.getModelViewStack();

        renderStack.push();
        renderStack.loadIdentity();
        RenderSystem.applyModelViewMatrix();
        renderStack.pop();
    }

    public static void restoreMatrices()
    {
        /* Return back to orthographic projection */
        RenderSystem.setProjectionMatrix(oldProjection, VertexSorter.BY_Z);
        RenderSystem.setInverseViewRotationMatrix(oldInverse);

        MatrixStack renderStack = RenderSystem.getModelViewStack();

        renderStack.push();
        renderStack.loadIdentity();
        MatrixStackUtils.multiply(renderStack, oldMV);
        RenderSystem.applyModelViewMatrix();
        renderStack.pop();
    }

    public static void applyTransform(MatrixStack stack, Transform transform)
    {
        stack.translate(transform.translate.x, transform.translate.y, transform.translate.z);

        if (transform.pivot.x != 0F || transform.pivot.y != 0F || transform.pivot.z != 0F)
        {
            stack.translate(transform.pivot.x, transform.pivot.y, transform.pivot.z);
        }

        stack.multiply(RotationAxis.POSITIVE_Z.rotation(transform.rotate.z));
        stack.multiply(RotationAxis.POSITIVE_Y.rotation(transform.rotate.y));
        stack.multiply(RotationAxis.POSITIVE_X.rotation(transform.rotate.x));
        stack.multiply(RotationAxis.POSITIVE_Z.rotation(transform.rotate2.z));
        stack.multiply(RotationAxis.POSITIVE_Y.rotation(transform.rotate2.y));
        stack.multiply(RotationAxis.POSITIVE_X.rotation(transform.rotate2.x));
        scaleStack(stack, transform.scale.x, transform.scale.y, transform.scale.z);

        if (transform.pivot.x != 0F || transform.pivot.y != 0F || transform.pivot.z != 0F)
        {
            stack.translate(-transform.pivot.x, -transform.pivot.y, -transform.pivot.z);
        }
    }

    public static void multiply(MatrixStack stack, Matrix4f matrix)
    {
        normal.set(matrix);
        normal.getScale(Vectors.TEMP_3F);

        Vectors.TEMP_3F.x = Vectors.TEMP_3F.x == 0F ? 0F : 1F / Vectors.TEMP_3F.x;
        Vectors.TEMP_3F.y = Vectors.TEMP_3F.y == 0F ? 0F : 1F / Vectors.TEMP_3F.y;
        Vectors.TEMP_3F.z = Vectors.TEMP_3F.z == 0F ? 0F : 1F / Vectors.TEMP_3F.z;

        normal.scale(Vectors.TEMP_3F);

        stack.peek().getPositionMatrix().mul(matrix);
        stack.peek().getNormalMatrix().mul(normal);
    }

    public static void scaleBack(MatrixStack matrices)
    {
        Matrix4f position = matrices.peek().getPositionMatrix();

        float scaleX = (float) Math.sqrt(position.m00() * position.m00() + position.m10() * position.m10() + position.m20() * position.m20());
        float scaleY = (float) Math.sqrt(position.m01() * position.m01() + position.m11() * position.m11() + position.m21() * position.m21());
        float scaleZ = (float) Math.sqrt(position.m02() * position.m02() + position.m12() * position.m12() + position.m22() * position.m22());

        float max = Math.max(scaleX, Math.max(scaleY, scaleZ));

        position.m00(position.m00() / max);
        position.m10(position.m10() / max);
        position.m20(position.m20() / max);

        position.m01(position.m01() / max);
        position.m11(position.m11() / max);
        position.m21(position.m21() / max);

        position.m02(position.m02() / max);
        position.m12(position.m12() / max);
        position.m22(position.m22() / max);
    }

    /**
     * Devuelve una copia de la matriz con la escala normalizada a 1 en cada eje
     * (X, Y, Z), preservando la traslación y la rotación.
     *
     * Útil para renderizar/pickear gizmos que no deben deformarse con la
     * escala del hueso/parte.
     */
    public static Matrix4f stripScale(Matrix4f matrix)
    {
        Matrix4f out = new Matrix4f(matrix);

        float sx = (float) Math.sqrt(out.m00() * out.m00() + out.m10() * out.m10() + out.m20() * out.m20());
        float sy = (float) Math.sqrt(out.m01() * out.m01() + out.m11() * out.m11() + out.m21() * out.m21());
        float sz = (float) Math.sqrt(out.m02() * out.m02() + out.m12() * out.m12() + out.m22() * out.m22());

        if (sx != 0F)
        {
            out.m00(out.m00() / sx);
            out.m10(out.m10() / sx);
            out.m20(out.m20() / sx);
        }

        if (sy != 0F)
        {
            out.m01(out.m01() / sy);
            out.m11(out.m11() / sy);
            out.m21(out.m21() / sy);
        }

        if (sz != 0F)
        {
            out.m02(out.m02() / sz);
            out.m12(out.m12() / sz);
            out.m22(out.m22() / sz);
        }

        return out;
    }
}