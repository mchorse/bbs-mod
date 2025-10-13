package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;

public class ValueTransform extends BaseKeyframeFactoryValue<Transform>
{
    public ValueTransform(String id, Transform transform)
    {
        super(id, KeyframeFactories.TRANSFORM, transform);
    }
}