package mchorse.bbs_mod.forms.values;

import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueAnchor extends BaseKeyframeFactoryValue<Anchor>
{
    public ValueAnchor(String id, Anchor value)
    {
        super(id, KeyframeFactories.ANCHOR, value);
    }
}