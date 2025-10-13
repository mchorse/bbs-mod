package mchorse.bbs_mod.camera.values;

import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

public class ValueAngle extends BaseValueBasic<Angle>
{
    public ValueAngle(String id, Angle angle)
    {
        super(id, angle);
    }

    @Override
    public void set(Angle value, int flag)
    {
        this.preNotify();
        this.value.set(value);
        this.postNotify();
    }

    @Override
    public BaseType toData()
    {
        return this.value.toData();
    }

    @Override
    public void fromData(BaseType data)
    {
        this.value.fromData(data.asMap());
    }
}