package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueString extends BaseKeyframeFactoryValue<String>
{
    public ValueString(String id, String defaultValue)
    {
        super(id, KeyframeFactories.STRING, defaultValue);
    }

    @Override
    public String toString()
    {
        return this.value;
    }
}