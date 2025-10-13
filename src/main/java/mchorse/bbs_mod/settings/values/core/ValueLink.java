package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueLink extends BaseKeyframeFactoryValue<Link>
{
    public ValueLink(String id, Link defaultValue)
    {
        super(id, KeyframeFactories.LINK, defaultValue);
    }

    @Override
    public String toString()
    {
        return this.value == null ? "" : this.value.toString();
    }
}