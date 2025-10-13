package mchorse.bbs_mod.settings.values.misc;

import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import org.joml.Vector4f;

public class ValueVector4f extends BaseKeyframeFactoryValue<Vector4f>
{
    public ValueVector4f(String id, Vector4f value)
    {
        super(id, KeyframeFactories.VECTOR4F, value);
    }
}