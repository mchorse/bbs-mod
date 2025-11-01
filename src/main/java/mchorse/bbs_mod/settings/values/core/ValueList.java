package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ValueList <T extends BaseValue> extends BaseValueGroup
{
    protected final List<T> list = new ArrayList<T>();

    public ValueList(String id)
    {
        super(id);
    }

    public List<T> getList()
    {
        return Collections.unmodifiableList(this.list);
    }

    public void add(T value)
    {
        this.list.add(value);
        value.setParent(this);
    }

    public void add(int index, T value)
    {
        if (!CollectionUtils.inRange(this.list, index))
        {
            this.add(value);

            return;
        }

        this.list.add(index, value);
        value.setParent(this);
    }

    @Override
    public List<BaseValue> getAll()
    {
        return (List<BaseValue>) this.list;
    }

    public List<T> getAllTyped()
    {
        return this.list;
    }

    @Override
    public BaseValue get(String key)
    {
        try
        {
            int index = Integer.parseInt(key);

            return CollectionUtils.getSafe(this.list, index);
        }
        catch (Exception e)
        {}

        return null;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj.getClass() == this.getClass())
        {
            ValueList<?> list = (ValueList<?>) obj;

            return list.list.equals(this.list);
        }

        return super.equals(obj);
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        this.list.clear();

        for (BaseValue value : group.getAll())
        {
            this.list.add(this.create(value.getId()));
        }
    }

    public void sync()
    {
        int i = 0;

        for (T value : this.list)
        {
            value.setId(String.valueOf(i));
            value.setParent(this);

            i += 1;
        }
    }

    protected abstract T create(String id);

    @Override
    public BaseType toData()
    {
        ListType list = new ListType();

        for (T value : this.list)
        {
            list.add(value.toData());
        }

        return list;
    }

    @Override
    public void fromData(BaseType data)
    {
        this.list.clear();

        if (!data.isList())
        {
            return;
        }

        ListType list = data.asList();

        for (int i = 0; i < list.size(); i++)
        {
            T value = this.create(String.valueOf(i));

            this.add(value);
            value.fromData(list.get(i));
        }
    }
}