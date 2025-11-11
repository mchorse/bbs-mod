package mchorse.bbs_mod.settings.values.misc;

import mchorse.bbs_mod.forms.forms.utils.PivotSettings;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValuePivotSettings extends BaseKeyframeFactoryValue<PivotSettings>
{
    public ValuePivotSettings(String id, PivotSettings value)
    {
        super(id, KeyframeFactories.PIVOT_SETTINGS, value);
    }
}