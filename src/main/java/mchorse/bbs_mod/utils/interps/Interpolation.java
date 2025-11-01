package mchorse.bbs_mod.utils.interps;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.interps.easings.EasingArgs;

import java.util.Map;

public class Interpolation extends BaseValue implements IInterp
{
    private final Map<String, IInterp> map;

    private IInterp interp;
    private EasingArgs args = new EasingArgs();

    public Interpolation(String id, Map<String, IInterp> map)
    {
        this(id, map, Interpolations.LINEAR);
    }

    public Interpolation(String id, Map<String, IInterp> map, IInterp interp)
    {
        super(id);

        this.interp = interp;
        this.map = map;
    }

    @Override
    public boolean has(IInterp interp)
    {
        return this.interp.has(interp);
    }

    @Override
    public double interpolate(InterpContext context)
    {
        return this.interp.interpolate(context.extra(this.args));
    }

    @Override
    public String getKey()
    {
        return this.interp.getKey();
    }

    @Override
    public int getKeyCode()
    {
        return this.interp.getKeyCode();
    }

    public Map<String, IInterp> getMap()
    {
        return this.map;
    }

    public IInterp getInterp()
    {
        return this.interp;
    }

    public void setInterp(IInterp interp)
    {
        this.preNotify();
        this.interp = interp;
        this.postNotify();
    }

    public double getV1()
    {
        return this.args.v1;
    }

    public void setV1(double v1)
    {
        this.preNotify();
        this.args.v1 = v1;
        this.postNotify();
    }

    public double getV2()
    {
        return this.args.v2;
    }

    public void setV2(double v2)
    {
        this.preNotify();
        this.args.v2 = v2;
        this.postNotify();
    }

    public double getV3()
    {
        return this.args.v3;
    }

    public void setV3(double v3)
    {
        this.preNotify();
        this.args.v3 = v3;
        this.postNotify();
    }

    public double getV4()
    {
        return this.args.v4;
    }

    public void setV4(double v4)
    {
        this.preNotify();
        this.args.v4 = v4;
        this.postNotify();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof Interpolation i)
        {
            return this.interp == i.interp
                && this.args.v1 == i.args.v1
                && this.args.v2 == i.args.v2
                && this.args.v3 == i.args.v3
                && this.args.v4 == i.args.v4;
        }

        return false;
    }

    @Override
    public BaseType toData()
    {
        if (this.args.v1 == 0 && this.args.v2 == 0 && this.args.v3 == 0 && this.args.v4 == 0)
        {
            return new StringType(CollectionUtils.getKey(this.map, this.interp));
        }

        MapType map = new MapType();
        ListType list = new ListType();

        map.putString("key", CollectionUtils.getKey(this.map, this.interp));
        map.put("args", list);
        list.addDouble(this.args.v1);
        list.addDouble(this.args.v2);
        list.addDouble(this.args.v3);
        list.addDouble(this.args.v4);

        return map;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data == null)
        {
            return;
        }

        if (data.isList())
        {
            ListType list = data.asList();

            if (list.size() >= 5)
            {
                this.interp = this.map.getOrDefault(list.getString(0), Interpolations.LINEAR);
                this.args.v1 = list.getDouble(1);
                this.args.v2 = list.getDouble(2);
                this.args.v3 = list.getDouble(3);
                this.args.v4 = list.getDouble(4);
            }
        }
        else if (data.isMap())
        {
            MapType map = data.asMap();
            ListType args = map.getList("args");

            this.interp = this.map.getOrDefault(map.getString("key"), Interpolations.LINEAR);

            if (args.size() >= 4)
            {
                this.args.v1 = args.getDouble(0);
                this.args.v2 = args.getDouble(1);
                this.args.v3 = args.getDouble(2);
                this.args.v4 = args.getDouble(3);
            }
        }
        else if (data.isString())
        {
            this.interp = this.map.getOrDefault(data.asString(), Interpolations.LINEAR);
            this.args.v1 = this.args.v2 = this.args.v3 = this.args.v4 = 0;
        }
    }
}