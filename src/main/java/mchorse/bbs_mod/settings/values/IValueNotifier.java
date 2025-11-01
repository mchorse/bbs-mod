package mchorse.bbs_mod.settings.values;

import mchorse.bbs_mod.settings.values.base.BaseValue;

public interface IValueNotifier
{
    public default void preNotify()
    {
        this.preNotify(IValueListener.FLAG_DEFAULT);
    }

    public void preNotify(int flag);

    public default void preNotify(BaseValue value, int flag)
    {
        if (this.getParent() != null)
        {
            this.getParent().preNotify(value, flag);
        }
    }

    public default void postNotify()
    {
        this.postNotify(IValueListener.FLAG_DEFAULT);
    }

    public void postNotify(int flag);

    public default void postNotify(BaseValue value, int flag)
    {
        if (this.getParent() != null)
        {
            this.getParent().postNotify(value, flag);
        }
    }

    public BaseValue getParent();
}