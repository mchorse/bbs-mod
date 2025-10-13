package mchorse.bbs_mod.settings.values.numeric;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.FloatType;
import mchorse.bbs_mod.settings.values.base.BaseValueNumber;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueFloat extends BaseValueNumber<Float>
{
    public ValueFloat(String id, Float defaultValue)
    {
        this(id, defaultValue, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    public ValueFloat(String id, Float defaultValue, Float min, Float max)
    {
        super(id, KeyframeFactories.FLOAT, defaultValue, min, max);
    }

    @Override
    protected Float clamp(Float value)
    {
        return MathUtils.clamp(value, this.min, this.max);
    }

    @Override
    public BaseType toData()
    {
        return new FloatType(this.value);
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isNumeric())
        {
            this.value = data.asNumeric().floatValue();
        }
    }

    @Override
    public String toString()
    {
        return Float.toString(this.value);
    }
}