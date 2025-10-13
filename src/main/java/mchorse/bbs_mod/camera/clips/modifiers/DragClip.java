package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.interps.Lerps;

/**
 * Drag modifier
 * 
 * This modifier is responsible for creating follow like 
 * behavior by memorizing previous position/angle and then 
 * linearly interpolating it using given factor.
 */
public class DragClip extends ComponentClip
{
    public final ValueBoolean deterministic = new ValueBoolean("deterministic", true);
    public final ValueFloat factor = new ValueFloat("factor", 0.5F, 0F, 1F);
    public final ValueInt rate = new ValueInt("rate", 60, 1, 300);

    private Position original = new Position();
    private Position current = new Position();

    private boolean cached;
    private double prevX;
    private double prevY;
    private double prevZ;
    private float prevYaw;
    private float prevPitch;
    private float prevRoll;
    private float prevFov;

    public DragClip()
    {
        super();

        this.add(this.deterministic);
        this.add(this.factor);
        this.add(this.rate);
    }

    public void resetCache()
    {
        this.cached = false;
    }

    @Override
    public void applyClip(ClipContext context, Position position)
    {
        if (this.active.get() == 0)
        {
            return;
        }

        if (!this.cached)
        {
            this.cached = true;
            this.prevX = position.point.x;
            this.prevY = position.point.y;
            this.prevZ = position.point.z;
            this.prevYaw = position.angle.yaw;
            this.prevPitch = position.angle.pitch;
            this.prevRoll = position.angle.roll;
            this.prevFov = position.angle.fov;
        }

        float factor = this.factor.get();
        boolean isX = this.isActive(0);
        boolean isY = this.isActive(1);
        boolean isZ = this.isActive(2);
        boolean isYaw = this.isActive(3);
        boolean isPitch = this.isActive(4);
        boolean isRoll = this.isActive(5);
        boolean isFov = this.isActive(6);

        if (this.deterministic.get())
        {
            int offset = this.tick.get();

            this.original.copy(position);
            context.applyUnderneath(offset, 0F, this.current);
            position.copy(this.current);

            float rate = this.rate.get() / 20F;
            float duration = (context.relativeTick + context.transition) * rate;

            for (int i = 1; i <= duration; i++)
            {
                float tick = i / rate;

                context.applyUnderneath(offset + (int) tick, tick % 1F, this.current);

                if (isX) position.point.x = Lerps.lerp(position.point.x, this.current.point.x, factor);
                if (isY) position.point.y = Lerps.lerp(position.point.y, this.current.point.y, factor);
                if (isZ) position.point.z = Lerps.lerp(position.point.z, this.current.point.z, factor);
                if (isYaw) position.angle.yaw = (float) Lerps.lerpYaw(position.angle.yaw, this.current.angle.yaw, factor);
                if (isPitch) position.angle.pitch = Lerps.lerp(position.angle.pitch, this.current.angle.pitch, factor);
                if (isRoll) position.angle.roll = Lerps.lerp(position.angle.roll, this.current.angle.roll, factor);
                if (isFov) position.angle.fov = Lerps.lerp(position.angle.fov, this.current.angle.fov, factor);
            }

            if (!isX) position.point.x = this.original.point.x;
            if (!isY) position.point.y = this.original.point.y;
            if (!isZ) position.point.z = this.original.point.z;
            if (!isYaw) position.angle.yaw = this.original.angle.yaw;
            if (!isPitch) position.angle.pitch = this.original.angle.pitch;
            if (!isRoll) position.angle.roll = this.original.angle.roll;
            if (!isFov) position.angle.fov = this.original.angle.fov;
        }
        else
        {
            context.applyUnderneath(context.ticks, context.transition, this.current);

            if (isX) position.point.x = this.prevX = Lerps.lerp(this.prevX, this.current.point.x, factor);
            if (isY) position.point.y = this.prevY = Lerps.lerp(this.prevY, this.current.point.y, factor);
            if (isZ) position.point.z = this.prevZ = Lerps.lerp(this.prevZ, this.current.point.z, factor);
            if (isYaw) position.angle.yaw = this.prevYaw = (float) Lerps.lerpYaw(this.prevYaw, this.current.angle.yaw, factor);
            if (isPitch) position.angle.pitch = this.prevPitch = Lerps.lerp(this.prevPitch, this.current.angle.pitch, factor);
            if (isRoll) position.angle.roll = this.prevRoll = Lerps.lerp(this.prevRoll, this.current.angle.roll, factor);
            if (isFov) position.angle.fov = this.prevFov = Lerps.lerp(this.prevFov, this.current.angle.fov, factor);
        }
    }

    @Override
    public Clip create()
    {
        return new DragClip();
    }
}