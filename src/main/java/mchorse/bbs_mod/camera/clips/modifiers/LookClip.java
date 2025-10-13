package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.values.ValuePoint;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.interps.Lerps;

import java.util.List;

/**
 * Look modifier
 * 
 * This modifier locks fixture's angle so it would always look in the 
 * direction of entity. Relative yaw and pitch is also supported.
 */
public class LookClip extends EntityClip
{
    public final ValueBoolean relative = new ValueBoolean("relative");
    public final ValueBoolean atBlock = new ValueBoolean("atBlock");
    public final ValueBoolean forward = new ValueBoolean("forward");
    public final ValuePoint block = new ValuePoint("block", new Point(0, 0, 0));

    public LookClip()
    {
        super();

        this.add(this.relative);
        this.add(this.atBlock);
        this.add(this.forward);
        this.add(this.block);
    }

    @Override
    public void applyClip(ClipContext context, Position position)
    {
        List<IEntity> entities = this.getEntities(context);

        boolean atBlock = this.atBlock.get();
        boolean forward = this.forward.get();

        if (entities.isEmpty() && !(atBlock || forward))
        {
            return;
        }

        boolean wasModified = false;

        if (forward && context.applyUnderneath(context.ticks - 1, context.transition, this.position))
        {
            wasModified = true;
        }

        if (!wasModified)
        {
            this.position.copy(position);
        }

        double x = 0;
        double y = 0;
        double z = 0;

        if (atBlock)
        {
            Point block = this.block.get();

            x = block.x;
            y = block.y;
            z = block.z;
        }
        else if (!forward)
        {
            double size = entities.size();

            for (IEntity entity : entities)
            {
                x += Lerps.lerp(entity.getPrevX(), entity.getX(), context.transition) / size;
                y += Lerps.lerp(entity.getPrevY(), entity.getY(), context.transition) / size;
                z += Lerps.lerp(entity.getPrevZ(), entity.getZ(), context.transition) / size;
            }
        }

        Point point = this.offset.get();

        x += point.x;
        y += point.y;
        z += point.z;

        double dX = x - position.point.x;
        double dY = y - position.point.y;
        double dZ = z - position.point.z;

        if (forward)
        {
            dX = position.point.x - this.position.point.x;
            dY = position.point.y - this.position.point.y;
            dZ = position.point.z - this.position.point.z;
        }

        Angle angle = Angle.angle(dX, dY, dZ);

        float yaw = angle.yaw;
        float pitch = angle.pitch;

        if (this.relative.get() && !forward)
        {
            yaw += position.angle.yaw - this.position.angle.yaw;
            pitch += position.angle.pitch - this.position.angle.pitch;
        }

        position.angle.set(yaw, pitch);
    }

    @Override
    public Clip create()
    {
        return new LookClip();
    }
}