package mchorse.bbs_mod.utils.keyframes;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.Collections;
import java.util.List;

/**
 * Keyframe channel
 *
 * This class is responsible for storing individual keyframes and also
 * interpolating between them.
 */
public class KeyframeChannel <T> extends ValueList<Keyframe<T>>
{
    private IKeyframeFactory<T> factory;

    public KeyframeChannel(String id, IKeyframeFactory<T> factory)
    {
        super(id);

        this.factory = factory;
    }

    public IKeyframeFactory<T> getFactory()
    {
        return this.factory;
    }

    /* Read only */

    public double getLength()
    {
        return this.list.isEmpty() ? 0 : (int) this.list.get(this.list.size() - 1).getTick();
    }

    public boolean isEmpty()
    {
        return this.list.isEmpty();
    }

    public List<Keyframe<T>> getKeyframes()
    {
        return Collections.unmodifiableList(this.list);
    }

    public boolean has(int index)
    {
        return index >= 0 && index < this.list.size();
    }

    public Keyframe<T> get(int index)
    {
        return this.has(index) ? this.list.get(index) : null;
    }

    public KeyframeSegment<T> find(float ticks)
    {
        KeyframeSegment<T> segment = this.findSegment(ticks);

        if (segment == null)
        {
            return null;
        }

        segment.setup(ticks);

        return segment;
    }

    public T interpolate(float ticks)
    {
        T orDefault = null;

        if (this.factory == KeyframeFactories.FLOAT) orDefault = (T) Float.valueOf(0F);
        else if (this.factory == KeyframeFactories.DOUBLE) orDefault = (T) Double.valueOf(0D);
        else if (this.factory == KeyframeFactories.INTEGER) orDefault = (T) Integer.valueOf(0);

        return this.interpolate(ticks, orDefault);
    }

    public T interpolate(float ticks, T orDefault)
    {
        KeyframeSegment<T> segment = this.findSegment(ticks);

        if (segment == null)
        {
            return orDefault;
        }

        segment.setup(ticks);

        return segment.createInterpolated();
    }

    /**
     * Find a keyframe segment at given ticks
     */
    public KeyframeSegment<T> findSegment(float ticks)
    {
        /* No keyframes, no values */
        if (this.list.isEmpty())
        {
            return null;
        }

        /* Check whether given ticks are outside keyframe channel's range */
        Keyframe<T> prev = this.list.get(0);
        int size = this.list.size();

        if (size == 1 || ticks < prev.getTick())
        {
            return new KeyframeSegment<>(prev, prev);
        }

        Keyframe<T> last = this.list.get(size - 1);

        if (ticks >= last.getTick())
        {
            return new KeyframeSegment<>(last, last);
        }

        /* Use binary search to find the proper segment */
        int low = 0;
        int high = size - 1;

        while (low <= high)
        {
            int mid = low + (high - low) / 2;

            if (this.list.get(mid).getTick() < ticks)
            {
                low = mid + 1;
            }
            else
            {
                high = mid - 1;
            }
        }

        Keyframe<T> b = this.list.get(low);

        if (b.getTick() == Math.floor(ticks) && low < size - 1)
        {
            low += 1;
            b = this.list.get(low);
        }

        Keyframe<T> a = low - 1 >= 0 ? this.list.get(low - 1) : b;
        KeyframeSegment<T> segment = new KeyframeSegment<>(a, b);

        segment.setup(ticks);

        return segment;
    }

    /* Write only */

    public void removeAll()
    {
        this.preNotify();
        this.list.clear();
        this.postNotify();
    }

    public void remove(int index)
    {
        if (index < 0 || index > this.list.size() - 1)
        {
            return;
        }

        this.preNotify();
        this.list.remove(index);
        this.sync();
        this.postNotify();
    }

    public void insertSpace(int where, int ticks)
    {
        KeyframeSegment<T> segment = this.findSegment(where);

        if (segment == null || where > segment.b.getTick())
        {
            return;
        }

        if (where < segment.a.getTick())
        {
            this.moveX(ticks);
        }
        else
        {
            BaseValue.edit(this, (__) ->
            {
                T copy = this.factory.copy(segment.createInterpolated());
                List<Keyframe<T>> keyframes = this.getKeyframes();

                for (int i = keyframes.indexOf(segment.b); i < keyframes.size(); i++)
                {
                    Keyframe<T> kf = keyframes.get(i);

                    kf.setTick(kf.getTick() + ticks);
                }

                Keyframe<T> kfA = keyframes.get(this.insert(where, copy));
                Keyframe<T> kfB = keyframes.get(this.insert(where + ticks, this.factory.copy(copy)));

                kfA.getInterpolation().setInterp(Interpolations.CONST);
                kfB.getInterpolation().copy(segment.a.getInterpolation());
            });
        }
    }

    /**
     * Insert a keyframe at given tick with given value
     *
     * This method is useful as it's not creating keyframes every time you
     * need to add some value, but rather inserts in correct order or
     * overwrites existing keyframe.
     *
     * Also, it returns index at which it was inserted.
     */
    public int insert(float tick, T value)
    {
        this.preNotify();

        Keyframe<T> prev;

        if (!this.list.isEmpty())
        {
            prev = this.list.get(0);

            if (tick < prev.getTick())
            {
                this.add(0, new Keyframe<>("", this.factory, tick, value));
                this.sort();

                this.postNotify();

                return 0;
            }
        }

        prev = null;
        int index = 0;

        for (Keyframe<T> frame : this.list)
        {
            if (frame.getTick() == tick)
            {
                frame.setValue(value);
                this.postNotify();

                return index;
            }

            if (prev != null && tick > prev.getTick() && tick < frame.getTick())
            {
                break;
            }

            index++;
            prev = frame;
        }

        this.add(index, new Keyframe<T>("", this.factory, tick, value));
        this.sort();
        this.postNotify();

        return index;
    }

    public void sort()
    {
        this.list.sort((a, b) -> (int) (a.getTick() - b.getTick()));

        this.sync();
    }

    public void simplify()
    {
        if (this.list.size() <= 2)
        {
            return;
        }

        this.preNotify();

        for (int i = 1; i < this.list.size() - 1; i++)
        {
            Keyframe<T> prev = this.list.get(i - 1);
            Keyframe<T> current = this.list.get(i);
            Keyframe<T> next = this.list.get(i + 1);

            if (this.factory.compare(current.getValue(), prev.getValue()) && this.factory.compare(current.getValue(), next.getValue()))
            {
                this.list.remove(i);

                i -= 1;
            }
        }

        int size = this.list.size();

        if (this.factory.compare(this.list.get(size - 1).getValue(), this.list.get(size - 2).getValue()))
        {
            this.list.remove(size - 1);
        }

        this.sync();
        this.postNotify();
    }

    public void moveX(float offset)
    {
        this.preNotify();

        for (Keyframe<T> keyframe : this.list)
        {
            keyframe.setTick(keyframe.getTick() + offset);
        }

        this.postNotify();
    }

    @Override
    protected Keyframe<T> create(String id)
    {
        return new Keyframe<>(id, this.factory);
    }

    @Override
    public BaseType toData()
    {
        MapType data = new MapType();

        data.put("keyframes", super.toData());
        data.putString("type", CollectionUtils.getKey(KeyframeFactories.FACTORIES, this.factory));

        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (!data.isMap())
        {
            return;
        }

        MapType map = data.asMap();
        IKeyframeFactory<T> factory = KeyframeFactories.FACTORIES.get(map.getString("type"));

        this.factory = factory;

        super.fromData(map.getList("keyframes"));

        this.sort();
    }

    public void copyKeyframes(KeyframeChannel<T> channel)
    {
        this.list.clear();

        for (Keyframe<T> keyframe : channel.getKeyframes())
        {
            Keyframe<T> value = new Keyframe<>(keyframe.getId(), keyframe.getFactory());

            value.copy(keyframe);
            this.add(value);
        }

        this.sort();
    }

    public void copyOver(KeyframeChannel channel, int tick)
    {
        if (this.factory != channel.factory || channel.isEmpty())
        {
            return;
        }

        this.preNotify();

        double start = tick + ((Keyframe) channel.getKeyframes().get(0)).getTick();

        this.list.removeIf((next) -> next.getTick() >= start);

        for (Object o : channel.getKeyframes())
        {
            Keyframe keyframe = (Keyframe) o;
            Keyframe value = new Keyframe<>(keyframe.getId(), keyframe.getFactory());

            value.fromData(keyframe.toData());
            value.setTick(tick + value.getTick());
            this.list.add(value);
        }

        this.sync();
        this.postNotify();
    }
}