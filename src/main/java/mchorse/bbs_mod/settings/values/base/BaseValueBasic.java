package mchorse.bbs_mod.settings.values.base;

import mchorse.bbs_mod.settings.values.IValueListener;

import java.util.Objects;

public abstract class BaseValueBasic <T> extends BaseValue
{
    protected T value;
    protected T runtimeValue;

    public BaseValueBasic(String id, T value)
    {
        super(id);

        this.value = value;
    }

    public T get()
    {
        if (this.runtimeValue != null)
        {
            return this.runtimeValue;
        }

        return this.value;
    }

    public T getOriginalValue()
    {
        return this.value;
    }

    public T getRuntimeValue()
    {
        return this.runtimeValue;
    }

    public void set(T value)
    {
        this.set(value, IValueListener.FLAG_DEFAULT);
    }

    public void set(T value, int flag)
    {
        this.preNotify(flag);
        this.value = value;
        this.postNotify(flag);
    }

    public void setRuntimeValue(T value)
    {
        this.runtimeValue = value;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof BaseValueBasic)
        {
            BaseValueBasic baseValue = (BaseValueBasic) obj;

            return Objects.equals(this.value, baseValue.value);
        }

        return false;
    }
}