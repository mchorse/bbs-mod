package mchorse.bbs_mod.utils.joml;

import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Matrices
{
    public static final Matrix3f EMPTY_3F = new Matrix3f();
    public static final Matrix3d EMPTY_3D = new Matrix3d();
    public static final Matrix4f EMPTY_4F = new Matrix4f();
    public static final Matrix4d EMPTY_4D = new Matrix4d();

    /* Temporary matrices that can be used to avoid allocations */
    public static final Matrix3f TEMP_3F = new Matrix3f();
    public static final Matrix3d TEMP_3D = new Matrix3d();
    public static final Matrix4f TEMP_4F = new Matrix4f();
    public static final Matrix4d TEMP_4D = new Matrix4d();

    private static final Matrix3f rotation = new Matrix3f();
    private static final Vector3f forward = new Vector3f();

    private static final Matrix3f lerpA = new Matrix3f();
    private static final Matrix3f lerpB = new Matrix3f();
    private static final Quaternionf lerpQa = new Quaternionf();
    private static final Quaternionf lerpQb = new Quaternionf();
    private static final Vector3f lerpVa = new Vector3f();
    private static final Vector3f lerpVb = new Vector3f();

    public static Vector3f rotate(Vector3f vector, float pitch, float yaw)
    {
        rotation.identity();
        rotation.rotateY(yaw);
        rotation.rotateX(pitch);
        rotation.transform(vector);

        return vector;
    }

    public static Vector3f rotation(float pitch, float yaw)
    {
        return rotate(forward.set(0, 0, 1), pitch, yaw);
    }

    public static Matrix3f direction(Vector3f forward)
    {
        Matrix3f direction = new Matrix3f();
        Vector3f right = new Vector3f(0, 1, 0);
        Vector3f up = new Vector3f(forward);

        if (right.equals(forward))
        {
            right.set(1, 0, 0);
        }

        right.cross(forward);
        up.cross(right);

        direction.m00 = right.x;
        direction.m01 = right.y;
        direction.m02 = right.z;
        direction.m10 = forward.x;
        direction.m11 = forward.y;
        direction.m12 = forward.z;
        direction.m20 = up.x;
        direction.m21 = up.y;
        direction.m22 = up.z;

        return direction;
    }

    public static Matrix4f lerp(Matrix4f a, Matrix4f b, float t)
    {
        return lerp(a, b, t, new Matrix4f());
    }

    public static Matrix4f lerp(Matrix4f a, Matrix4f b, float t, Matrix4f dest)
    {
        Quaternionf q1 = lerpQa.setFromNormalized(lerpA.set(a));
        Quaternionf q2 = lerpQb.setFromNormalized(lerpB.set(b));

        q1.slerp(q2, t);

        dest.identity().rotate(q1);
        dest.setTranslation(a.getTranslation(lerpVa).lerp(b.getTranslation(lerpVb), t));

        return dest;
    }

    public static String toString(Matrix3f m)
    {
        return m.m00() + ", " + m.m10() + ", " + m.m20() + "\n" +
            m.m01() + ", " + m.m11() + ", " + m.m21() + "\n" +
            m.m02() + ", " + m.m12() + ", " + m.m22();
    }

    public static String toString(Matrix4f m)
    {
        return m.m00() + ", " + m.m10() + ", " + m.m20() + ", " + m.m30() + "\n" +
            m.m01() + ", " + m.m11() + ", " + m.m21() + ", " + m.m31() + "\n" +
            m.m02() + ", " + m.m12() + ", " + m.m22() + ", " + m.m32() + "\n" +
            m.m03() + ", " + m.m13() + ", " + m.m23() + ", " + m.m33() + "\n";
    }

    public static Vector3f getEulerXYZ(Matrix3f m)
    {
        double yaw = Math.atan2(m.m02, Math.sqrt(m.m00 * m.m00 + m.m01 * m.m01));
        double pitch = Math.atan2(m.m12, m.m22);
        double roll = Math.atan2(m.m01, m.m00);

        if (m.m00 < 0)
        {
            yaw = -yaw + Math.PI;
            pitch = -pitch;
        }
        else
        {
            yaw += Math.PI;
        }

        return new Vector3f((float) pitch, (float) yaw, (float) roll);
    }
}