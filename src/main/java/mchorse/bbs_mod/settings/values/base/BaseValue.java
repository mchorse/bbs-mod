package mchorse.bbs_mod.settings.values.base;

import mchorse.bbs_mod.data.IDataSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.IValueNotifier;
import mchorse.bbs_mod.utils.DataPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class BaseValue implements IDataSerializable<BaseType>, IValueNotifier
{
    protected String id;
    protected BaseValue parent;

    private boolean visible = true;
    private List<IValueListener> preCallbacks;
    private List<IValueListener> postCallbacks;

    public static <T extends BaseValue> void edit(T value, Consumer<T> callback)
    {
        edit(value, 0, callback);
    }

    public static <T extends BaseValue> void edit(T value, int flag, Consumer<T> callback)
    {
        if (callback == null)
        {
            return;
        }

        value.preNotify(flag);
        callback.accept(value);
        value.postNotify(flag);
    }

    public BaseValue(String id)
    {
        this.setId(id);
    }

    /**
     * Don't use it without a reason!
     */
    public void setId(String id)
    {
        this.id = id;
    }

    public BaseValue invisible()
    {
        this.visible = false;

        return this;
    }

    public BaseValue preCallback(IValueListener callback)
    {
        if (this.preCallbacks == null)
        {
            this.preCallbacks = new ArrayList<>();
        }

        this.preCallbacks.add(callback);

        return this;
    }

    public BaseValue postCallback(IValueListener callback)
    {
        if (this.postCallbacks == null)
        {
            this.postCallbacks = new ArrayList<>();
        }

        this.postCallbacks.add(callback);

        return this;
    }

    public boolean isVisible()
    {
        boolean visible = true;
        BaseValue value = this;

        while (value != null)
        {
            visible = visible && value.visible;
            value = value.getParent();
        }

        return visible;
    }

    public BaseValue getRoot()
    {
        BaseValue value = this;

        while (true)
        {
            if (value.getParent() == null)
            {
                return value;
            }

            value = value.getParent();
        }
    }

    public void setParent(BaseValue parent)
    {
        this.parent = parent;
    }

    public String getId()
    {
        return this.id;
    }

    public void resetCallbacks()
    {
        this.preCallbacks = this.postCallbacks = null;
    }

    @Override
    public void preNotify(int flag)
    {
        this.preNotify(this, flag);
    }

    @Override
    public void preNotify(BaseValue value, int flag)
    {
        IValueNotifier.super.preNotify(value, flag);

        if (this.preCallbacks != null)
        {
            for (IValueListener callback : this.preCallbacks)
            {
                callback.accept(value, flag);
            }
        }
    }

    @Override
    public void postNotify(int flag)
    {
        this.postNotify(this, flag);
    }

    @Override
    public void postNotify(BaseValue value, int flag)
    {
        IValueNotifier.super.postNotify(value, flag);

        if (this.postCallbacks != null)
        {
            for (IValueListener callback : this.postCallbacks)
            {
                callback.accept(value, flag);
            }
        }
    }

    @Override
    public BaseValue getParent()
    {
        return this.parent;
    }

    public List<String> getPathSegments()
    {
        List<String> strings = new ArrayList<>();
        BaseValue value = this;

        while (value != null)
        {
            String id = value.getId();

            if (!id.isEmpty())
            {
                strings.add(id);
            }

            value = value.getParent();
        }

        Collections.reverse(strings);

        return strings;
    }

    public DataPath getPath()
    {
        List<String> segments = this.getPathSegments();
        DataPath path = new DataPath(false);

        path.strings.addAll(segments);

        return path;
    }

    public DataPath getRelativePath(BaseValue ancestor)
    {
        DataPath strings = new DataPath(false);
        BaseValue value = this;

        while (value != null)
        {
            String id = value.getId();

            if (!id.isEmpty())
            {
                strings.strings.add(id);
            }

            value = value.getParent();

            if (value == ancestor)
            {
                strings.strings.add(value.getId());

                Collections.reverse(strings.strings);

                return strings;
            }
        }

        return null;
    }

    public void copy(BaseValue value)
    {
        this.copy(value, IValueListener.FLAG_DEFAULT);
    }

    public void copy(BaseValue value, int flag)
    {
        this.preNotify(flag);

        if (value != null)
        {
            this.fromData(value.toData());
        }

        this.postNotify(flag);
    }
}