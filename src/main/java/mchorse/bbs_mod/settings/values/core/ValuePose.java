package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Pose;

public class ValuePose extends BaseKeyframeFactoryValue<Pose>
{
    public ValuePose(String id, Pose value)
    {
        super(id, KeyframeFactories.POSE, value);
    }
}