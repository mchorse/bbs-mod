package mchorse.bbs_mod.utils.keyframes;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

import java.util.Objects;

public class Keyframe <T> extends BaseValue
{
    private float tick;
    private T value;

    public float lx = 5;
    public float ly;
    public float rx = 5;
    public float ry;

    private KeyframeShape shape = KeyframeShape.SQUARE;
    private Color color;

    /**
     * Forced duration that would be used instead of the difference
     * between two keyframes, if not 0
     */
    private float duration;
    private final Interpolation interp = new Interpolation("interp", Interpolations.MAP);

    private final IKeyframeFactory<T> factory;

    public Keyframe(String id, IKeyframeFactory<T> factory, float tick, T value)
    {
        this(id, factory);

        this.tick = tick;
        this.value = value;
    }

    public Keyframe(String id, IKeyframeFactory<T> factory)
    {
        super(id);

        this.factory = factory;
    }

    public IKeyframeFactory<T> getFactory()
    {
        return this.factory;
    }

    public float getTick()
    {
        return this.tick;
    }

    public void setTick(float tick)
    {
        this.setTick(tick, false);
    }

    public void setTick(float tick, boolean dirty)
    {
        if (dirty) this.preNotify();

        this.tick = tick;

        if (dirty) this.postNotify();
    }

    public float getDuration()
    {
        return this.duration;
    }

    public void setDuration(float duration)
    {
        this.preNotify();
        this.duration = Math.max(0, duration);
        this.postNotify();
    }

    public T getValue()
    {
        return this.value;
    }

    public double getY(int index)
    {
        return this.factory.getY(this.value);
    }

    public void setValue(T value)
    {
        this.setValue(value, false);
    }

    public void setValue(T value, boolean dirty)
    {
        if (dirty) this.preNotify();

        this.value = value;

        if (dirty) this.postNotify();
    }

    public Interpolation getInterpolation()
    {
        return this.interp;
    }

    public KeyframeShape getShape()
    {
        return this.shape;
    }

    public void setShape(KeyframeShape shape)
    {
        this.preNotify();
        this.shape = shape;
        this.postNotify();
    }

    public Color getColor()
    {
        return this.color;
    }

    public void setColor(Color color)
    {
        this.preNotify();
        this.color = color;
        this.postNotify();
    }

    public void copy(Keyframe<T> keyframe)
    {
        this.tick = keyframe.tick;
        this.duration = keyframe.duration;
        this.value = this.factory.copy(keyframe.value);
        this.interp.copy(keyframe.interp);
        this.shape = keyframe.shape;
        this.color = keyframe.color;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof Keyframe<?> kf)
        {
            return this.tick == kf.tick
                && Objects.equals(this.value, kf.value)
                && this.lx == kf.lx
                && this.ly == kf.ly
                && this.rx == kf.rx
                && this.ry == kf.ry
                && this.duration == kf.duration
                && Objects.equals(this.interp, kf.interp);
        }

        return false;
    }

    @Override
    public BaseType toData()
    {
        MapType data = new MapType();

        data.putFloat("tick", this.tick);
        data.put("value", this.factory.toData(this.value));

        if (this.duration != 0F) data.putFloat("duration", this.duration);
        if (this.interp.getInterp() != Interpolations.LINEAR) data.put("interp", this.interp.toData());
        if (this.lx != 5F) data.putFloat("lx", this.lx);
        if (this.ly != 0F) data.putFloat("ly", this.ly);
        if (this.rx != 5F) data.putFloat("rx", this.rx);
        if (this.ry != 0F) data.putFloat("ry", this.ry);
        if (this.color != null) data.putInt("color", this.color.getRGBColor());
        if (this.shape != KeyframeShape.SQUARE) data.putString("shape", this.shape.toString().toUpperCase());

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

        this.shape = KeyframeShape.SQUARE;
        this.color = null;

        if (map.has("tick")) this.tick = map.getFloat("tick");
        if (map.has("duration")) this.duration = map.getFloat("duration");
        if (map.has("value")) this.value = this.factory.fromData(map.get("value"));
        if (map.has("interp")) this.interp.fromData(map.get("interp"));
        if (map.has("lx")) this.lx = map.getFloat("lx");
        if (map.has("ly")) this.ly = map.getFloat("ly");
        if (map.has("rx")) this.rx = map.getFloat("rx");
        if (map.has("ry")) this.ry = map.getFloat("ry");
        if (map.has("shape")) this.shape = KeyframeShape.fromString(map.getString("shape"));
        if (map.has("color")) this.color = Color.rgb(map.getInt("color"));
    }

    public void copyOverExtra(Keyframe<T> a)
    {
        this.getInterpolation().copy(a.getInterpolation());
        this.setShape(a.getShape());
        this.setColor(a.getColor());
    }
}