package mchorse.bbs_mod.forms.values;

import mchorse.bbs_mod.forms.forms.utils.ParticleSettings;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueParticleSettings extends BaseKeyframeFactoryValue<ParticleSettings>
{
    public ValueParticleSettings(String id, ParticleSettings value)
    {
        super(id, KeyframeFactories.PARTICLE_SETTINGS, value);
    }
}