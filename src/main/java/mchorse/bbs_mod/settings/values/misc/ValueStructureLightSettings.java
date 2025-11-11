package mchorse.bbs_mod.settings.values.misc;

import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueStructureLightSettings extends BaseKeyframeFactoryValue<StructureLightSettings>
{
    public ValueStructureLightSettings(String id, StructureLightSettings value)
    {
        super(id, KeyframeFactories.STRUCTURE_LIGHT_SETTINGS, value);
    }
}