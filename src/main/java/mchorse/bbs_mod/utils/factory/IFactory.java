package mchorse.bbs_mod.utils.factory;

import mchorse.bbs_mod.data.IDataSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface IFactory <T, D>
{
    public Link getType(T object);

    public T create(Link type);

    public default MapType toData(T object)
    {
        MapType data = new MapType();

        if (object instanceof IDataSerializable)
        {
            BaseType baseData = ((IDataSerializable) object).toData();

            if (baseData.isMap())
            {
                data = baseData.asMap();
            }

            this.appendId(object, data);
        }

        return data;
    }

    public default void appendId(T object, MapType data)
    {
        data.putString(this.getTypeKey(), this.getType(object).toString());
    }

    public default T fromData(MapType data)
    {
        if (data == null)
        {
            return null;
        }

        Link type = Link.create(data.getString(this.getTypeKey()));
        T object = this.create(type);

        if (object instanceof IDataSerializable)
        {
            ((IDataSerializable) object).fromData(data);
        }

        return object;
    }

    public default String getTypeKey()
    {
        return "type";
    }

    public D getData(T object);

    public D getData(Link type);

    public Collection<Link> getKeys();

    public default Collection<String> getStringKeys()
    {
        Set<String> keys = new HashSet<>();

        for (Link link : this.getKeys())
        {
            keys.add(link.toString());
        }

        return keys;
    }
}