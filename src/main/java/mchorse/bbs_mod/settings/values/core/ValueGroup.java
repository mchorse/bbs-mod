package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ValueGroup extends BaseValueGroup
{
    private Map<String, BaseValue> children = new LinkedHashMap<>();

    public ValueGroup(String id)
    {
        super(id);
    }

    public void removeAll()
    {
        this.children.clear();
    }

    public void add(BaseValue value)
    {
        if (value != null)
        {
            this.children.put(value.getId(), value);
            value.setParent(this);
        }
    }

    public void remove(BaseValue child)
    {
        BaseValue baseValue = this.children.get(child.getId());

        if (baseValue == child)
        {
            this.children.remove(child.getId());
        }
    }

    @Override
    public List<BaseValue> getAll()
    {
        return new ArrayList<>(this.children.values());
    }

    public Map<String, BaseValueBasic> getAllMap()
    {
        Map<String, BaseValueBasic> map = new HashMap<>();

        for (BaseValue value : this.children.values())
        {
            if (value instanceof BaseValueBasic<?> basic)
            {
                map.put(basic.getId(), basic);
            }
        }

        return map;
    }

    @Override
    public BaseValue get(String key)
    {
        return this.children.get(key);
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        for (BaseValue groupValue : group.getAll())
        {
            BaseValue value = this.children.get(groupValue.getId());

            if (value != null)
            {
                value.copy(groupValue);
            }
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean equals = super.equals(obj);

        if (equals)
        {
            return equals;
        }

        if (obj instanceof ValueGroup group)
        {
            return this.children.equals(group.children);
        }

        return false;
    }

    @Override
    public BaseType toData()
    {
        MapType data = new MapType();

        for (BaseValue value : this.children.values())
        {
            data.put(value.getId(), value.toData());
        }

        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (!data.isMap())
        {
            return;
        }

        for (Map.Entry<String, BaseType> entry : data.asMap())
        {
            BaseValue value = this.children.get(entry.getKey());

            if (value != null)
            {
                value.setParent(this);
                value.fromData(entry.getValue());
            }
        }
    }
}