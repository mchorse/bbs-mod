package mchorse.bbs_mod.settings.values.numeric;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ByteType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

public class ValueBoolean extends BaseValueBasic<Boolean>
{
    public ValueBoolean(String id)
    {
        this(id, false);
    }

    public ValueBoolean(String id, boolean defaultValue)
    {
        super(id, defaultValue);
    }

    public void toggle()
    {
        this.set(!this.get());
    }

    @Override
    public BaseType toData()
    {
        return new ByteType(this.value);
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isNumeric())
        {
            this.value = data.asNumeric().boolValue();
        }
    }

    @Override
    public String toString()
    {
        return Boolean.toString(this.value);
    }
}
