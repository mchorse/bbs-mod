package mchorse.bbs_mod.camera.clips.overwrite;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.joml.Matrices;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import org.joml.Vector3f;

/**
 * Keyframe fixture
 * 
 * This fixture provides a much flexible control over camera, allowing setting 
 * up different transitions between points with different easing.
 */
public class KeyframeClip extends CameraClip
{
    public final KeyframeChannel<Double> x = new KeyframeChannel<>("x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> y = new KeyframeChannel<>("y", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> z = new KeyframeChannel<>("z", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> yaw = new KeyframeChannel<>("yaw", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> pitch = new KeyframeChannel<>("pitch", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> roll = new KeyframeChannel<>("roll", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> fov = new KeyframeChannel<>("fov", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> distance = new KeyframeChannel<>("distance", KeyframeFactories.DOUBLE);
    public final ValueBoolean additive = new ValueBoolean("additive", false);

    public KeyframeChannel<Double>[] channels;

    public KeyframeClip()
    {
        super();

        this.channels = new KeyframeChannel[] {this.x, this.y, this.z, this.yaw, this.pitch, this.roll, this.fov, this.distance};

        for (KeyframeChannel<Double> channel : this.channels)
        {
            this.add(channel);
        }

        this.add(this.additive);
    }

    @Override
    public void fromCamera(Camera camera)
    {
        Position pos = new Position(camera);

        this.x.insert(0, pos.point.x);
        this.y.insert(0, pos.point.y);
        this.z.insert(0, pos.point.z);
        this.yaw.insert(0, (double) pos.angle.yaw);
        this.pitch.insert(0, (double) pos.angle.pitch);
        this.roll.insert(0, (double) pos.angle.roll);
        this.fov.insert(0, (double) pos.angle.fov);
    }

    @Override
    public void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;

        if (this.additive.get())
        {
            if (!this.x.isEmpty()) position.point.x += this.x.interpolate(0F) - this.x.interpolate(t);
            if (!this.y.isEmpty()) position.point.y += this.y.interpolate(0F) - this.y.interpolate(t);
            if (!this.z.isEmpty()) position.point.z += this.z.interpolate(0F) - this.z.interpolate(t);
            if (!this.yaw.isEmpty()) position.angle.yaw     += this.yaw.interpolate(0F).floatValue()   - this.yaw.interpolate(t).floatValue();
            if (!this.pitch.isEmpty()) position.angle.pitch += this.pitch.interpolate(0F).floatValue() - this.pitch.interpolate(t).floatValue();
            if (!this.roll.isEmpty()) position.angle.roll   += this.roll.interpolate(0F).floatValue()  - this.roll.interpolate(t).floatValue();
            if (!this.fov.isEmpty()) position.angle.fov     += this.fov.interpolate(0F).floatValue()   - this.fov.interpolate(t).floatValue();
        }
        else
        {
            if (!this.x.isEmpty()) position.point.x = this.x.interpolate(t);
            if (!this.y.isEmpty()) position.point.y = this.y.interpolate(t);
            if (!this.z.isEmpty()) position.point.z = this.z.interpolate(t);
            if (!this.yaw.isEmpty()) position.angle.yaw = this.yaw.interpolate(t).floatValue();
            if (!this.pitch.isEmpty()) position.angle.pitch = this.pitch.interpolate(t).floatValue();
            if (!this.roll.isEmpty()) position.angle.roll = this.roll.interpolate(t).floatValue();
            if (!this.fov.isEmpty()) position.angle.fov = this.fov.interpolate(t).floatValue();

            if (!this.distance.isEmpty())
            {
                double distance = this.distance.interpolate(t);

                if (distance != 0D)
                {
                    Vector3f rotation = Matrices.rotation(
                        MathUtils.toRad(position.angle.pitch),
                        MathUtils.toRad(-position.angle.yaw - 180)
                    );

                    position.point.x += rotation.x * distance;
                    position.point.y += rotation.y * distance;
                    position.point.z += rotation.z * distance;
                }
            }
        }
    }

    @Override
    public Clip create()
    {
        return new KeyframeClip();
    }

    @Override
    protected void breakDownClip(Clip original, int offset)
    {
        super.breakDownClip(original, offset);

        /* Clean up keyframes prior to broken apart */
        for (KeyframeChannel<Double> channel : this.channels)
        {
            channel.moveX(-offset);

            KeyframeSegment<Double> segment = channel.find(0);

            if (segment != null)
            {
                while (segment.a != channel.get(0)) channel.remove(0);
            }
        }

        KeyframeClip keyframeClip = (KeyframeClip) original;

        /* Clean up keyframes prior to broken apart */
        for (KeyframeChannel<Double> channel : keyframeClip.channels)
        {
            KeyframeSegment<Double> segment = channel.find(offset);

            if (segment != null)
            {
                while (segment.b != channel.get(channel.getKeyframes().size() - 1))
                {
                    channel.remove(channel.getKeyframes().size() - 1);
                }
            }
        }
    }
}