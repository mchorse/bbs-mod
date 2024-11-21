package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.modifiers.TrackerClip;
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

        Vector3f globalEulerAngles = MatrixUtils.cast3dTo3f(MatrixUtils.RotationOrder.YXZ.getEulerAngles(new Matrix3d(formTransform)));
        position.angle.yaw = (float) Math.toDegrees(-globalEulerAngles.y) - 180;
        position.angle.pitch = (float) Math.toDegrees(globalEulerAngles.x);
        position.angle.roll = (float) Math.toDegrees(globalEulerAngles.z);

        position.point.x += relativeFormPos.x;
        position.point.y += relativeFormPos.y;
        position.point.z += relativeFormPos.z;
    }

    @Override
    protected Clip create()
    {
        return new TrackerClientClip();
    }
}
