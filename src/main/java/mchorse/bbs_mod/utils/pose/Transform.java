package mchorse.bbs_mod.utils.pose;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.joml.Matrices;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Transform implements IMapSerializable
{
    private static final Vector3f DEFAULT_SCALE = new Vector3f(1F, 1F, 1F);

    public static final Transform DEFAULT = new Transform();

    public final Vector3f translate = new Vector3f();
    public final Vector3f scale = new Vector3f(DEFAULT_SCALE);
    public final Vector3f rotate = new Vector3f();
    public final Vector3f rotate2 = new Vector3f();
    public final Vector3f pivot = new Vector3f();

    public void lerp(Transform transform, float a)
    {
        this.translate.lerp(transform.translate, a);
        this.scale.lerp(transform.scale, a);
        this.rotate.lerp(transform.rotate, a);
        this.rotate2.lerp(transform.rotate2, a);
        this.pivot.lerp(transform.pivot, a);
    }

    public void lerp(Transform preA, Transform a, Transform b, Transform postB, IInterp interp, float x)
    {
        this.lerp(this.translate, preA.translate, a.translate, b.translate, postB.translate, interp, x);
        this.lerp(this.scale, preA.scale, a.scale, b.scale, postB.scale, interp, x);
        this.lerp(this.rotate, preA.rotate, a.rotate, b.rotate, postB.rotate, interp, x);
        this.lerp(this.rotate2, preA.rotate2, a.rotate2, b.rotate2, postB.rotate2, interp, x);
        this.lerp(this.pivot, preA.pivot, a.pivot, b.pivot, postB.pivot, interp, x);
    }

    private void lerp(Vector3f target, Vector3f preA, Vector3f a, Vector3f b, Vector3f postB, IInterp interp, float x)
    {
        target.x = (float) interp.interpolate(IInterp.context.set(preA.x, a.x, b.x, postB.x, x));
        target.y = (float) interp.interpolate(IInterp.context.set(preA.y, a.y, b.y, postB.y, x));
        target.z = (float) interp.interpolate(IInterp.context.set(preA.z, a.z, b.z, postB.z, x));
    }

    public void identity()
    {
        this.translate.set(0, 0, 0);
        this.scale.set(1, 1, 1);
        this.rotate.set(0, 0, 0);
        this.rotate2.set(0, 0, 0);
        this.pivot.set(0, 0, 0);
    }

    public Matrix3f createRotationMatrix()
    {
        Matrix3f matrix = new Matrix3f();

        matrix.rotateZ(this.rotate.z);
        matrix.rotateY(this.rotate.y);
        matrix.rotateX(this.rotate.x);
        matrix.rotateZ(this.rotate2.z);
        matrix.rotateY(this.rotate2.y);
        matrix.rotateX(this.rotate2.x);

        return matrix;
    }

    public Matrix4f createMatrix()
    {
        return this.setupMatrix(Matrices.TEMP_4F.identity());
    }

    public Matrix4f setupMatrix(Matrix4f matrix)
    {
        matrix.translate(this.translate);

        if (this.pivot.x != 0F || this.pivot.y != 0F || this.pivot.z != 0F)
        {
            matrix.translate(this.pivot);
        }

        matrix.rotateZ(this.rotate.z);
        matrix.rotateY(this.rotate.y);
        matrix.rotateX(this.rotate.x);
        matrix.rotateZ(this.rotate2.z);
        matrix.rotateY(this.rotate2.y);
        matrix.rotateX(this.rotate2.x);
        matrix.scale(this.scale);

        if (this.pivot.x != 0F || this.pivot.y != 0F || this.pivot.z != 0F)
        {
            matrix.translate(-this.pivot.x, -this.pivot.y, -this.pivot.z);
        }

        return matrix;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof Transform)
        {
            Transform transform = (Transform) obj;

            return this.translate.equals(transform.translate)
                && this.scale.equals(transform.scale)
                && this.rotate.equals(transform.rotate)
                && this.rotate2.equals(transform.rotate2)
                && this.pivot.equals(transform.pivot);
        }

        return false;
    }

    public Transform copy()
    {
        Transform transform = new Transform();

        transform.copy(this);

        return transform;
    }

    public void copy(Transform transform)
    {
        this.translate.set(transform.translate);
        this.scale.set(transform.scale);
        this.rotate.set(transform.rotate);
        this.rotate2.set(transform.rotate2);
        this.pivot.set(transform.pivot);
    }

    public boolean isDefault()
    {
        return this.equals(DEFAULT);
    }

    public void toRad()
    {
        this.rotate.x = MathUtils.toRad(this.rotate.x);
        this.rotate.y = MathUtils.toRad(this.rotate.y);
        this.rotate.z = MathUtils.toRad(this.rotate.z);
        this.rotate2.x = MathUtils.toRad(this.rotate2.x);
        this.rotate2.y = MathUtils.toRad(this.rotate2.y);
        this.rotate2.z = MathUtils.toRad(this.rotate2.z);
    }

    @Override
    public void toData(MapType data)
    {
        if (!this.isDefault())
        {
            data.put("t", DataStorageUtils.vector3fToData(this.translate));
            data.put("s", DataStorageUtils.vector3fToData(this.scale));
            data.put("r", DataStorageUtils.vector3fToData(this.rotate));
            data.put("r2", DataStorageUtils.vector3fToData(this.rotate2));
            data.put("p", DataStorageUtils.vector3fToData(this.pivot));
        }
    }

    @Override
    public void fromData(MapType data)
    {
        this.identity();

        this.translate.set(DataStorageUtils.vector3fFromData(data.getList("t")));
        this.scale.set(DataStorageUtils.vector3fFromData(data.getList("s"), DEFAULT_SCALE));
        this.rotate.set(DataStorageUtils.vector3fFromData(data.getList("r")));
        this.rotate2.set(DataStorageUtils.vector3fFromData(data.getList("r2")));
        this.pivot.set(DataStorageUtils.vector3fFromData(data.getList("p")));
    }
}