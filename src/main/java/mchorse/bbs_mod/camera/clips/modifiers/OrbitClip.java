package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Matrices;
import org.joml.Vector3f;

import java.util.List;

/**
 * Orbit modifier
 * 
 * This modifier is responsible for making the camera orbit around
 * the given entity with given yaw and pitch.
 */
public class OrbitClip extends EntityClip
{
    /**
     * In addition, copy yaw and pitch from entity
     */
    public final ValueBoolean copy = new ValueBoolean("copy");

    /**
     * How far away to orbit from the entity
     */
    public final ValueFloat distance = new ValueFloat("distance", 0F);

    /**
     * Yaw to be added to orbit
     */
    public final ValueFloat yaw = new ValueFloat("yaw", 0F);

    /**
     * Pitch to be added to orbit
     */
    public final ValueFloat pitch = new ValueFloat("pitch", 0F);

    /**
     * Absolute
     */
    public final ValueBoolean absolute = new ValueBoolean("absolute", false);

    public OrbitClip()
    {
        super();

        this.add(this.copy);
        this.add(this.distance);
        this.add(this.yaw);
        this.add(this.pitch);
        this.add(this.absolute);
    }

    @Override
    public void applyClip(ClipContext context, Position position)
    {
        List<IEntity> entities = this.getEntities(context);

        if (entities.isEmpty())
        {
            return;
        }

        if (!context.applyUnderneath(this.tick.get(), 0F, this.position))
        {
            this.position.copy(position);
        }

        float yaw = this.yaw.get() + (position.angle.yaw - this.position.angle.yaw);
        float pitch = this.pitch.get() + (position.angle.pitch - this.position.angle.pitch);
        float distance = this.distance.get() + (float) (position.point.z - this.position.point.z);

        if (this.absolute.get())
        {
            yaw = position.angle.yaw;
            pitch = position.angle.pitch;
            distance = (float) position.point.z;
        }

        IEntity entity = entities.get(0);
        Vector3f vector = Matrices.rotation(MathUtils.toRad(pitch), MathUtils.toRad(-yaw));

        if (this.copy.get())
        {
            float entityYaw = Lerps.lerp(entity.getPrevHeadYaw(), entity.getHeadYaw(), context.transition);
            float entityPitch = -Lerps.lerp(entity.getPrevPitch(), entity.getPitch(), context.transition);

            Matrices.rotate(vector, MathUtils.toRad(-entityPitch), MathUtils.toRad(-entityYaw));
        }

        Point offset = this.offset.get();
        double x = Lerps.lerp(entity.getPrevX(), entity.getX(), context.transition) + offset.x;
        double y = Lerps.lerp(entity.getPrevY(), entity.getY(), context.transition) + offset.y;
        double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), context.transition) + offset.z;

        vector.mul(distance);

        double fX = x + vector.x;
        double fY = y + vector.y;
        double fZ = z + vector.z;
        Angle angle = Angle.angle(x - fX, y - fY, z - fZ);

        position.point.set(fX, fY, fZ);
        position.angle.set(angle.yaw, angle.pitch);
    }

    @Override
    public Clip create()
    {
        return new OrbitClip();
    }
}