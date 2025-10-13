package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueColor extends BaseKeyframeFactoryValue<Color>
{
    public ValueColor(String id, Color value)
    {
        super(id, KeyframeFactories.COLOR, value);
    }
}