package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

import java.util.HashSet;
import java.util.Set;

public class ValueStringKeys extends BaseValueBasic<Set<String>>
{
    public ValueStringKeys(String id)
    {
        super(id, new HashSet<>());
    }

    @Override
    public BaseType toData()
    {
        ListType list = new ListType();

        for (String s : this.value)
        {
            list.addString(s);
        }

        return list;
    }

    @Override
    public void fromData(BaseType data)
    {
        this.value.clear();

        if (!data.isList())
        {
            return;
        }

        for (BaseType type : data.asList())
        {
            if (type.isString()) this.value.add(type.asString());
        }
    }
}