package mchorse.bbs_mod.utils.clips;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class Envelope extends ValueGroup
{
    public final ValueBoolean enabled = new ValueBoolean("enabled");
    public final ValueInt mode = new ValueInt("mode", 0, 0, 2);

    public final ValueFloat fadeIn = new ValueFloat("fadeIn", 10F);
    public final ValueFloat fadeOut = new ValueFloat("fadeOut", 10F);

    public final Interpolation pre = new Interpolation("pre", Interpolations.MAP);
    public final Interpolation post = new Interpolation("post", Interpolations.MAP);

    public final ValueBoolean keyframes = new ValueBoolean("keyframes");
    public final KeyframeChannel<Double> channel = new KeyframeChannel<>("channel", KeyframeFactories.DOUBLE);

    public Envelope(String id)
    {
        super(id);

        this.add(this.enabled);
        this.add(this.mode);
        this.add(this.fadeIn);
        this.add(this.fadeOut);
        this.add(this.pre);
        this.add(this.post);
        this.add(this.keyframes);
        this.add(this.channel);

        this.channel.insert(0, 0D);
        this.channel.insert(BBSSettings.getDefaultDuration(), 1D);
    }

    public float getStartX(int duration)
    {
        return 0;
    }

    public float getStartDuration(int duration)
    {
        if (this.mode.get() == 1) return this.fadeIn.get() <= 0 ? duration : this.fadeIn.get();
        else if (this.mode.get() == 2) return 0;

        return this.fadeIn.get();
    }

    public float getEndX(int duration)
    {
        return duration;
    }

    public float getEndDuration(int duration)
    {
        if (this.mode.get() == 1) return duration;
        else if (this.mode.get() == 2) return duration - (this.fadeOut.get() <= 0 ? duration : this.fadeOut.get());

        return duration - this.fadeOut.get();
    }

    public float factorEnabled(int duration, float tick)
    {
        if (!this.enabled.get())
        {
            return 1;
        }

        return this.factor(duration, tick);
    }

    public float factor(int duration, float tick)
    {
        float envelope = 0;

        if (this.keyframes.get())
        {
            if (!this.channel.isEmpty())
            {
                envelope = this.channel.interpolate(tick).floatValue();
            }
        }
        else
        {
            float lowOut = this.getStartDuration(duration);

            envelope = Lerps.envelope(tick, this.getStartX(duration), lowOut, this.getEndDuration(duration), this.getEndX(duration));
            envelope = (float) (tick <= lowOut ? this.pre : this.post).interpolate(IInterp.context.set(0, 1, envelope));
        }

        return envelope;
    }

    public void breakDown(Clip original, int offset)
    {
        this.fadeIn.set(0F);
        original.envelope.fadeOut.set(0F);

        this.channel.moveX(-offset);
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        if (data.isMap())
        {
            MapType map = data.asMap();

            if (map.has("interpolation"))
            {
                BaseType interpolation = map.get("interpolation");

                this.pre.fromData(interpolation);
                this.post.fromData(interpolation);
            }
        }
    }
}