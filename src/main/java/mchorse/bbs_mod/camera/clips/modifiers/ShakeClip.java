package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/**
 * Shake modifier
 * 
 * This modifier shakes the camera depending on the given component 
 * flags.
 */
public class ShakeClip extends ComponentClip
{
    public final ValueFloat shake = new ValueFloat("shake", 0F);
    public final ValueFloat shakeAmount = new ValueFloat("shakeAmount", 0F);

    public ShakeClip()
    {
        super();

        this.add(this.shake);
        this.add(this.shakeAmount);

        /* Yaw and pitch should be enabled by default */
        this.active.set(0b0011000);
    }

    @Override
    public void applyClip(ClipContext context, Position position)
    {
        float shake = this.shake.get();
        float amount = this.shakeAmount.get();
        float x = (context.ticks + context.transition) / (shake == 0 ? 1 : shake);

        boolean isX = this.isActive(0);
        boolean isY = this.isActive(1);
        boolean isZ = this.isActive(2);
        boolean isYaw = this.isActive(3);
        boolean isPitch = this.isActive(4);
        boolean isRoll = this.isActive(5);
        boolean isFov = this.isActive(6);

        double sin = Math.sin(x);
        double cos = Math.cos(x);

        if (isYaw && isPitch && !isX && !isY && !isZ && !isRoll && !isFov)
        {
            float swingX = (float) (sin * sin * cos * Math.cos(x / 2));
            float swingY = (float) (cos * sin * sin);

            position.angle.yaw += swingX * amount;
            position.angle.pitch += swingY * amount;
        }
        else
        {
            if (isX)
            {
                position.point.x += sin * amount;
            }

            if (isY)
            {
                position.point.y -= sin * amount;
            }

            if (isZ)
            {
                position.point.z += cos * amount;
            }

            if (isYaw)
            {
                position.angle.yaw += sin * amount;
            }

            if (isPitch)
            {
                position.angle.pitch += cos * amount;
            }

            if (isRoll)
            {
                position.angle.roll += sin * amount;
            }

            if (isFov)
            {
                position.angle.fov += cos * amount;
            }
        }
    }

    @Override
    public Clip create()
    {
        return new ShakeClip();
    }
}