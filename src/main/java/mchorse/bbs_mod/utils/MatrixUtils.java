package mchorse.bbs_mod.utils;

import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.Math;

/**
 * @author Christian Fritz (also known as Chryfi)
 */
public class MatrixUtils {
    public static Matrix3f getRotationFromTransformation(Matrix4f transformation) {
        Matrix3f rotation = new Matrix3f();
        Vector3f rx = new Vector3f(transformation.m00(), transformation.m10(), transformation.m20());
        Vector3f ry = new Vector3f(transformation.m01(), transformation.m11(), transformation.m21());
        Vector3f rz = new Vector3f(transformation.m02(), transformation.m12(), transformation.m22());

        rx.normalize();
        ry.normalize();
        rz.normalize();
        rotation.setRow(0, rx);
        rotation.setRow(1, ry);
        rotation.setRow(2, rz);
        return rotation;
    }

    public static Vector3f cast3dTo3f(Vector3d vec) {
        return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
    }

    public enum Axis {
        X, Y, Z;

        /**
         * Index of the vector component<br>
         * X = 0 <br>
         * Y = 1 <br>
         * Z = 2 <br>
         */
        public final int index;

        Axis() {
            this.index = this.name().toUpperCase().charAt(0) - 'X';
        }

        /**
         * @param vecIndex
         * @return the corresponding axis that represents the vector component specified by the index
         *         e.g. index = 0 is axis X, index = 2 is axis Z and index > 2 is null.
         */
        @Nullable
        public static Axis axisOfVectorIndex(int vecIndex) {
            return switch (vecIndex) {
                case 0 -> X;
                case 1 -> Y;
                case 2 -> Z;
                default -> null;
            };
        }

        /**
         * @param radians angle in radians around this axis
         * @return the rotation around this axis
         */
        public Matrix3d getRotationMatrix(double radians)
        {
            return this.getRotationMatrix(new Matrix3d(), radians);
        }

        /**
         *
         * @param mat the matrix to set the result to. The original matrix is overwritten.
         * @param radians angle in radians around this axis
         * @return the rotation around this axis
         */
        public Matrix3d getRotationMatrix(Matrix3d mat, double radians)
        {
            mat.identity();

            switch (this)
            {
                case X:
                    return mat.rotateX(radians);
                case Y:
                    return mat.rotateY(radians);
                case Z:
                    return mat.rotateZ(radians);
                default:
                    return mat;
            }
        }

        /**
         * @return the unit vector representing this axis.
         */
        public Vector3d getAxisVector() {
            return new Vector3d(0, 0, 0).setComponent(this.index, 1);
        }

        /**
         * Projects the given vector onto a plane where this axis is the normal.
         * The result is written into the given vector.
         * @param v the vector to project and write the result to.
         * @return v
         */
        public Vector3d projectOntoAxisPlane(Vector3d v) {
            return v.setComponent(this.index, 0);
        }

        /**
         * Calculates the angle around this axis needed to achieve the given rotation
         * matrix.
         * @param forwardAxis the forward axis to test for flips and rotation
         * @param rotation the rotation matrix to calculate the angle from
         * @return angle in radians around this axis
         */
        public double getAngleAround(Axis forwardAxis, Matrix3d rotation) {
            return this.getAngleAround(forwardAxis, 1, rotation);
        }

        /**
         * @param forwardAxis the forward axis to test for flips and rotation
         * @return angle in radians around this axis
         */
        public double getAngleAround(Axis forwardAxis, int sign, Matrix3d rotation) {
            if (forwardAxis == this) return 0;
            sign = sign == 0 ? 1 : (int) Math.signum(sign);

            Vector3d forward = forwardAxis.getAxisVector().mul(sign);
            Vector3d flipTest = this.getAxisVector().cross(forward);
            Vector3d rotatedForward = rotation.transform(new Vector3d(forward));
            rotatedForward = this.projectOntoAxisPlane(rotatedForward);
            /* projecting might make the vector too small, in that case there is no rotation information */
            if (rotatedForward.lengthSquared() < 1E-07) return 0;

            double angleUnsigned = rotatedForward.angle(forward);
            return rotatedForward.dot(flipTest) < 0 ? -angleUnsigned : angleUnsigned;
        }
    }

    public enum RotationOrder {
        XYZ, XZY, YXZ, YZX, ZXY, ZYX;

        public final Axis rotAxis0;
        public final Axis rotAxis1;
        public final Axis rotAxis2;

        RotationOrder() {
            String order = this.name().toUpperCase();
            this.rotAxis0 = Axis.axisOfVectorIndex(order.charAt(0) - 'X');
            this.rotAxis1 = Axis.axisOfVectorIndex(order.charAt(1) - 'X');
            this.rotAxis2 = Axis.axisOfVectorIndex(order.charAt(2) - 'X');
        }

        /**
         * @param x euler angle in radians around local x axis
         * @param y euler angle in radians around local y axis
         * @param z euler angle in radians around local z axis
         * @return rotation matrix produced by this rotation order and reordering the angles provided to fit
         * the axis.
         */
        public Matrix3d getRotationMatrixFromXYZ(double x, double y, double z) {
            Vector3d angles = new Vector3d(x, y, z);
            return this.rotAxis0.getRotationMatrix(angles.get(this.rotAxis0.index))
                    .mul(this.rotAxis1.getRotationMatrix(angles.get(this.rotAxis1.index)))
                    .mul(this.rotAxis2.getRotationMatrix(angles.get(this.rotAxis2.index)));
        }

        /**
         * @param angle0 angle in radians around the first rotation axis
         * @param angle1 angle in radians around the second rotation axis
         * @param angle2 angle in radians around the third rotation axis
         * @return rotation matrix produced by the angles and this rotation order.
         */
        public Matrix3d getRotationMatrix(double angle0, double angle1, double angle2) {
            return this.rotAxis0.getRotationMatrix(angle0)
                    .mul(this.rotAxis1.getRotationMatrix(angle1))
                    .mul(this.rotAxis2.getRotationMatrix(angle2));
        }

        /**
         * Orders the given angles that correspond to this rotation order back to a XYZ order.
         * @param angle0 around first rotation axis
         * @param angle1 around second rotation axis
         * @param angle2 around third rotation axis
         */
        public Vector3d orderAnglesToXYZ(double angle0, double angle1, double angle2) {
            Vector3d angles = new Vector3d();
            angles.setComponent(this.rotAxis0.index, angle0);
            angles.setComponent(this.rotAxis1.index, angle1);
            angles.setComponent(this.rotAxis2.index, angle2);
            return angles;
        }

        /**
         *
         * @param axis the axis around which an angle should be calculated
         * @return the forward axis, orthogonal to the given axis, that is used for testing flips.
         */
        public Axis getForwardForRotationAxis(Axis axis) {
            /* +z and +y are nice forwards, is more standard */
            return axis == Axis.Z ? Axis.Y : Axis.Z;
        }

        /**
         * Calculates the euler angles needed to achieve the given transformation matrix with this rotation order.
         * @param transform
         * @return euler xyz angles in radians
         */
        public Vector3d getEulerAngles(Matrix3d transform) {
            /* copy since we will modify this in the process */
            transform = new Matrix3d(transform);

            double angle0 = this.extractAngleAroundAxis(this.rotAxis0, transform);
            double angle1 = this.extractAngleAroundAxis(this.rotAxis1, transform);
            double angle2 = this.extractAngleAroundAxis(this.rotAxis2, transform);

            return this.orderAnglesToXYZ(angle0, angle1, angle2);
        }

        private double extractAngleAroundAxis(Axis axis, Matrix3d transform) {
            Matrix3d removeAxisRot = new Matrix3d().identity();
            double angle = axis.getAngleAround(this.getForwardForRotationAxis(axis), transform);
            axis.getRotationMatrix(removeAxisRot, -angle);
            transform.mulLocal(removeAxisRot);
            return angle;
        }
    }
}
