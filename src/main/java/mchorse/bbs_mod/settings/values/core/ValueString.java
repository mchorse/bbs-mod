package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

public class ValueString extends BaseValueBasic<String>
{
    public ValueString(String id, String defaultValue)
    {
        super(id, defaultValue);
    }

    @Override
    public BaseType toData()
    {
        return new StringType(this.value);
    }

    @Override
    public void fromData(BaseType data)
    {
        if (BaseType.isString(data))
        {
            this.value = data.asString();
        }
    }

    @Override
    public String toString()
    {
        return this.value;
    }
}