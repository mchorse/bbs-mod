package mchorse.bbs_mod.settings.values.base;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

public class BaseKeyframeFactoryValue<T> extends BaseValueBasic<T>
{
    private final IKeyframeFactory<T> factory;

    public BaseKeyframeFactoryValue(String id, IKeyframeFactory<T> factory, T value)
    {
        super(id, value);

        this.factory = factory;
    }

    public IKeyframeFactory<T> getFactory()
    {
        return this.factory;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof BaseKeyframeFactoryValue<?> property && property.factory == this.factory)
        {
            return this.factory.compare(this.value, property.value);
        }

        return super.equals(obj);
    }

    @Override
    public BaseType toData()
    {
        return this.factory.toData(this.value);
    }

    @Override
    public void fromData(BaseType data)
    {
        this.value = this.factory.fromData(data);
    }
}