package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

import java.util.function.Predicate;

public class AudioClip extends CameraClip
{
    public static final Predicate<Clip> NO_AUDIO = (clip) -> !(clip instanceof AudioClip);

    public ValueLink audio = new ValueLink("audio", null);
    public ValueInt offset = new ValueInt("offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public AudioClip()
    {
        super();

        this.add(this.audio);
        this.add(this.offset);
    }

    @Override
    public void shiftLeft(int tick)
    {
        super.shiftLeft(tick);

        this.offset.set(this.offset.get() - (this.tick.get() - tick));
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new AudioClip();
    }

    @Override
    protected void breakDownClip(Clip original, int offset)
    {
        super.breakDownClip(original, offset);

        this.offset.set(this.offset.get() + offset);
    }
}