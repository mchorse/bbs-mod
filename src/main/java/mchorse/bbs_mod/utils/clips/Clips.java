package mchorse.bbs_mod.utils.clips;

import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.factory.IFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Clips extends ValueGroup
{
    private List<Clip> clips = new ArrayList<>();
    private IFactory<Clip, ClipFactoryData> factory;

    public Clips(String id, IFactory<Clip, ClipFactoryData> factory)
    {
        super(id);

        this.factory = factory;
    }

    public IFactory<Clip, ClipFactoryData> getFactory()
    {
        return this.factory;
    }

    public int findFreeLayer(Clip clip)
    {
        int layer = clip.layer.get();

        main: while (true)
        {
            for (Clip newClip : this.clips)
            {
                float a1 = clip.tick.get();
                float a2 = a1 + clip.duration.get();
                float b1 = newClip.tick.get();
                float b2 = b1 + newClip.duration.get();

                if (layer == newClip.layer.get() && MathUtils.isInside(a1, a2, b1, b2))
                {
                    layer += 1;

                    continue main;
                }
            }

            break;
        }

        return layer;
    }

    public void sortLayers()
    {
        for (Clip clip : this.clips)
        {
            clip.layer.set(0);
        }

        for (Clip clip : this.clips)
        {
            for (Clip otherClip : this.clips)
            {
                if (clip == otherClip)
                {
                    continue;
                }

                boolean sameLayer = clip.layer.get() == otherClip.layer.get();
                boolean intersects = MathUtils.isInside(clip.tick.get(), clip.tick.get() + clip.duration.get(), otherClip.tick.get(), otherClip.tick.get() + otherClip.duration.get());

                if (sameLayer && intersects)
                {
                    otherClip.layer.set(otherClip.layer.get() + 1);
                }
            }
        }
    }

    public int getTopLayer()
    {
        int layer = 0;

        for (Clip clip : this.clips)
        {
            layer = Math.max(layer, clip.layer.get());
        }

        return layer;
    }

    /**
     * Calculate total duration of this camera work.
     */
    public int calculateDuration()
    {
        int max = 0;

        for (Clip clip : this.clips)
        {
            max = Math.max(max, clip.tick.get() + clip.duration.get());
        }

        return max;
    }

    public Clip get(int index)
    {
        return index >= 0 && index < this.clips.size() ? this.clips.get(index) : null;
    }

    public Clip getClipAt(int tick, int layer)
    {
        for (Clip clip : this.clips)
        {
            if (clip.isInside(tick) && clip.layer.get() == layer)
            {
                return clip;
            }
        }

        return null;
    }

    public List<Clip> getClips(int tick)
    {
        return this.getClips(tick, Integer.MAX_VALUE);
    }

    public List<Clip> getClips(int tick, int maxLayer)
    {
        List<Clip> clipList = new ArrayList<>();

        for (Clip clip : this.clips)
        {
            boolean isGlobal = clip.isGlobal() && maxLayer == Integer.MAX_VALUE;

            if ((clip.isInside(tick) || isGlobal) && clip.layer.get() < maxLayer)
            {
                clipList.add(clip);
            }
        }

        clipList.sort(Comparator.comparingInt((a) -> a.layer.get()));

        return clipList;
    }

    /**
     * Get index of a given clip.
     *
     * @return index of a clip in the thing
     */
    public int getIndex(Clip clip)
    {
        return this.clips.indexOf(clip);
    }

    public void addClip(Clip clip)
    {
        this.preNotify();

        this.clips.add(clip);
        this.sync();

        this.postNotify();
    }

    public void remove(Clip clip)
    {
        this.preNotify();

        this.clips.remove(clip);
        this.sync();

        this.postNotify();
    }

    public void copyOver(Clips clips, int tick)
    {
        this.preNotify();

        this.clips.removeIf((next) -> next.tick.get() >= tick);

        for (Clip clip : clips.clips)
        {
            Clip copy = clip.copy();

            copy.tick.set(tick + copy.tick.get());
            this.addClip(copy);
        }

        this.sortLayers();
        this.sync();
        this.postNotify();
    }

    /* New value methods */

    public void sync()
    {
        this.removeAll();

        for (int i = 0, c = this.clips.size(); i < c; i++)
        {
            Clip clip = this.clips.get(i);

            clip.setId(String.valueOf(i));
            this.add(clip);
        }
    }

    public List<Clip> get()
    {
        return Collections.unmodifiableList(this.clips);
    }

    public int findNextTick(int tick)
    {
        int output = Integer.MAX_VALUE;

        for (Clip clip : this.clips)
        {
            int left = clip.tick.get() - tick;
            int right = left + clip.duration.get();

            int a = Math.max(left, 0);
            int b = Math.max(right, 0);

            if (a > 0)
            {
                output = Math.min(output, a);
            }
            else if (b > 0)
            {
                output = Math.min(output, b);
            }
        }

        return tick + (output != Integer.MAX_VALUE ? output : 0);
    }

    public int findPreviousTick(int tick)
    {
        int output = Integer.MIN_VALUE;

        for (Clip clip : this.clips)
        {
            int left = clip.tick.get() - tick;
            int right = left + clip.duration.get();

            int a = Math.min(left, -0);
            int b = Math.min(right, -0);

            if (b < -0)
            {
                output = Math.max(output, b);
            }
            else if (a < -0)
            {
                output = Math.max(output, a);
            }
        }

        return tick + (output != Integer.MIN_VALUE ? output : 0);
    }

    public void shift(float tick)
    {
        for (Clip clip : this.clips)
        {
            clip.tick.set(Math.round(clip.tick.get() + tick));
        }
    }

    public void shift(double dx, double dy, double dz)
    {
        for (Clip clip : this.clips)
        {
            clip.shift(dx, dy, dz);
        }
    }

    /* Value implementation */

    @Override
    public BaseType toData()
    {
        ListType list = new ListType();

        for (Clip clip : this.clips)
        {
            list.add(this.factory.toData(clip));
        }

        return list;
    }

    @Override
    public void fromData(BaseType base)
    {
        this.clips.clear();

        for (BaseType type : base.asList())
        {
            if (!type.isMap())
            {
                continue;
            }

            Clip clip = this.factory.fromData(type.asMap());

            if (clip != null)
            {
                this.clips.add(clip);
            }
        }

        this.sync();
    }
}