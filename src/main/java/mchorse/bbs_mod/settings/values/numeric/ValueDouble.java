package mchorse.bbs_mod.settings.values.numeric;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.DoubleType;
import mchorse.bbs_mod.settings.values.base.BaseValueNumber;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueDouble extends BaseValueNumber<Double>
{
    public ValueDouble(String id, Double defaultValue)
    {
        this(id, defaultValue, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public ValueDouble(String id, Double defaultValue, Double min, Double max)
    {
        super(id, KeyframeFactories.DOUBLE, defaultValue, min, max);
    }

    @Override
    protected Double clamp(Double value)
    {
        return MathUtils.clamp(value, this.min, this.max);
    }

    @Override
    public BaseType toData()
    {
        return new DoubleType(this.value);
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isNumeric())
        {
            this.value = data.asNumeric().doubleValue();
        }
    }

    @Override
    public String toString()
    {
        return Double.toString(this.value);
    }
}