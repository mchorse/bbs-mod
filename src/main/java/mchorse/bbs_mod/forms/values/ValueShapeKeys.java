package mchorse.bbs_mod.forms.values;

import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class ValueShapeKeys extends BaseKeyframeFactoryValue<ShapeKeys>
{
    public ValueShapeKeys(String id, ShapeKeys value)
    {
        super(id, KeyframeFactories.SHAPE_KEYS, value);
    }
}