package mchorse.bbs_mod.forms.values;

import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueActionsConfig extends BaseKeyframeFactoryValue<ActionsConfig>
{
    public ValueActionsConfig(String id, ActionsConfig value)
    {
        super(id, KeyframeFactories.ACTIONS_CONFIG, value);
    }
}