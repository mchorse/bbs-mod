package mchorse.bbs_mod.settings.value;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;

public class ValueKeyCombo extends BaseValueBasic<KeyCombo>
{
    public ValueKeyCombo(String id, KeyCombo combo)
    {
        super(id, combo);
    }

    @Override
    public void set(KeyCombo value, int flag)
    {
        this.preNotify(flag);
        this.value.copy(value);
        this.postNotify(flag);
    }

    @Override
    public BaseType toData()
    {
        ListType list = new ListType();

        for (int key : this.value.keys)
        {
            list.addInt(key);
        }

        return list;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (!data.isList())
        {
            return;
        }

        this.value.keys.clear();

        ListType list = data.asList();

        for (int i = 0; i < list.size(); i++)
        {
            this.value.keys.add(list.getInt(i));
        }
    }
}