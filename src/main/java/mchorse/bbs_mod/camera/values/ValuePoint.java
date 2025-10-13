package mchorse.bbs_mod.camera.values;

import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

public class ValuePoint extends BaseValueBasic<Point>
{
    public ValuePoint(String id, Point point)
    {
        super(id, point);
    }

    @Override
    public void set(Point value, int flag)
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