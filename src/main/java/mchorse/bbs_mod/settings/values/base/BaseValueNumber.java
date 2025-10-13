package mchorse.bbs_mod.settings.values.base;

import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

public abstract class BaseValueNumber <T extends Number> extends BaseKeyframeFactoryValue<T>
{
    protected T min;
    protected T max;

    public BaseValueNumber(String id, IKeyframeFactory<T> factory, T defaultValue, T min, T max)
    {
        super(id, factory, defaultValue);

        this.min = min;
        this.max = max;
    }

    public T getMin()
    {
        return this.min;
    }

    public T getMax()
    {
        return this.max;
    }

    @Override
    public void set(T value, int flag)
    {
        if (this.min != null && this.max != null)
        {
            value = this.clamp(value);
        }

        super.set(value, flag);
    }

    protected abstract T clamp(T value);
}