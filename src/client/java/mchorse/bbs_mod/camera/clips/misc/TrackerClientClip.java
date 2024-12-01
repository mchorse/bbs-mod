package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.modifiers.TrackerClip;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.film.FilmController;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.MatrixUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.*;

import java.lang.Math;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackerClientClip extends TrackerClip {
    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        List<IEntity> entities = this.getEntities(context);

        if (entities.isEmpty()) return;

        if (!context.applyUnderneath(this.tick.get(), 0, this.position))
        {
            this.position.copy(position);
        }

        //TODO target entity
        IEntity entity = entities.get(0);
        Form form;
        if ((form = entity.getForm()) == null) return;

        MatrixStack tempStack = new MatrixStack();
        Map<String, Matrix4f> map = new HashMap<>();
        FormUtilsClient.getRenderer(form).collectMatrices(entity, null, tempStack, map, "", context.transition);
        Vector3f relativeFormPos = new Vector3f();

        //TODO select target tracker
        if (!map.containsKey("0")) return;
        Matrix4f formTransform = FilmController.getMatrixForRenderWithRotation(entity, position.point.x, position.point.y, position.point.z, context.transition);
        formTransform.mul(map.get("0"));
        formTransform.getTranslation(relativeFormPos);

        Matrix3d trackerRot = new Matrix3d(formTransform);
        /* offset the tracker morph in local space so the offset is rotated/scaled by the tracker transformation */
        Vector3d relativeTrackerPos = new Vector3d(this.offset.get().x, this.offset.get().y, this.offset.get().z);
        trackerRot.transform(relativeTrackerPos);
        relativeTrackerPos.add(relativeFormPos);

        Vector3d firstCamPos = new Vector3d(this.position.point.x, this.position.point.y, this.position.point.z);
        Vector3d currentCamPos = new Vector3d(position.point.x, position.point.y, position.point.z);
        Vector3d newAngle = new Vector3d();
        Vector3d newPosition = new Vector3d();

        if (this.lookat.get()) {
            /* for lookat the position offset is also local to the tracker, i.e. it's like offsetting the tracker form */
            Angle lookAtAngle = Angle.angle(relativeTrackerPos.x, relativeTrackerPos.y, relativeTrackerPos.z);
            newAngle.set(lookAtAngle.pitch, lookAtAngle.yaw, lookAtAngle.roll)
                    .add(this.offsetAngle.get().x, this.offsetAngle.get().y, this.offsetAngle.get().z);
        } else {
            /* +----------+
             * | Position |
             * +----------+ */
            /*make tracker pos global*/
            relativeTrackerPos.add(currentCamPos);

            if (this.relative.get()) {
                /* transform camera movement into tracker space */
                Vector3d camPosDelta = new Vector3d(currentCamPos).sub(firstCamPos);
                trackerRot.transform(camPosDelta);
                relativeTrackerPos.add(camPosDelta);
            }

            /* +-------+
             * | Angle |
             * +-------+ */
            /* use offset angle to offset the tracker rotation in tracker space */
            Vector3d offsetAngle = new Vector3d(this.offsetAngle.get().x, this.offsetAngle.get().y, this.offsetAngle.get().z);
            offsetAngle.set(Math.toRadians(offsetAngle.x), Math.toRadians(offsetAngle.y), Math.toRadians(offsetAngle.z));
            trackerRot.mul(MatrixUtils.RotationOrder.YXZ.getRotationMatrix(offsetAngle.y, offsetAngle.x, offsetAngle.z));

            if (this.relative.get()) {
                /* camera angle movement is local in the rotated tracker space */
                double angleDeltaX = Math.toRadians(position.angle.pitch - this.position.angle.pitch);
                double angleDeltaY = Math.toRadians(position.angle.yaw - this.position.angle.yaw);
                double angleDeltaZ = Math.toRadians(position.angle.roll - this.position.angle.roll);
                trackerRot.mul(MatrixUtils.RotationOrder.YXZ.getRotationMatrix(angleDeltaY, angleDeltaX, angleDeltaZ));
            }

            Vector3f globalEulerAngles = MatrixUtils.cast3dTo3f(MatrixUtils.RotationOrder.YXZ.getEulerAngles(trackerRot));

            newAngle.set(Math.toDegrees(globalEulerAngles.x), Math.toDegrees(-globalEulerAngles.y) - 180, Math.toDegrees(globalEulerAngles.z));
            newPosition.set(relativeTrackerPos);
        }

        if (!this.lookat.get()) {
            position.point.x = this.isActive(0) ? newPosition.x : position.point.x;
            position.point.y = this.isActive(1) ? newPosition.y : position.point.y;
            position.point.z = this.isActive(2) ? newPosition.z : position.point.z;
        }

        position.angle.yaw = this.isActive(3) ? (float) newAngle.y : position.angle.yaw;
        position.angle.pitch = this.isActive(4) ? (float) newAngle.x : position.angle.pitch;
        position.angle.roll = this.isActive(5) ? (float) newAngle.z : position.angle.roll;
    }

    public boolean isActive(int bit)
    {
        return (this.active.get() >> bit & 1) == 1;
    }

    @Override
    protected Clip create()
    {
        return new TrackerClientClip();
    }
}
