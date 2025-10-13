package mchorse.bbs_mod.camera.values;

import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.settings.values.core.ValueGroup;

public class ValuePosition extends ValueGroup
{
    private Position position;
    private ValuePoint pointDelegate;
    private ValueAngle angleDelegate;

    public ValuePosition(String id)
    {
        this(id, new Position());
    }

    public ValuePosition(String id, Position position)
    {
        super(id);

        this.position = position;
        this.pointDelegate = new ValuePoint("point", this.position.point);
        this.angleDelegate = new ValueAngle("angle", this.position.angle);

        this.add(this.pointDelegate);
        this.add(this.angleDelegate);
    }

    public ValuePoint getPoint()
    {
        return this.pointDelegate;
    }

    public ValueAngle getAngle()
    {
        return this.angleDelegate;
    }

    public Position get()
    {
        return this.position;
    }

    public void set(Position position)
    {
        this.preNotify();
        this.position.set(position);
        this.postNotify();
    }

    @Override
    public BaseType toData()
    {
        return this.position.toData();
    }

    @Override
    public void fromData(BaseType data)
    {
        this.position.fromData(data.asMap());
    }
}