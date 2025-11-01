package mchorse.bbs_mod.forms.states;

import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.core.ValueList;

public class AnimationStates extends ValueList<AnimationState>
{
    public AnimationStates(String id)
    {
        super(id);
    }

    public AnimationState getMain()
    {
        for (AnimationState state : this.list)
        {
            if (state.main.get())
            {
                return state;
            }
        }

        return null;
    }

    public AnimationState addState()
    {
        this.preNotify(IValueListener.FLAG_UNMERGEABLE);

        AnimationState state = this.create(String.valueOf(this.list.size()));

        this.add(state);
        this.postNotify(IValueListener.FLAG_UNMERGEABLE);

        return state;
    }

    public void removeState(int index)
    {
        this.preNotify(IValueListener.FLAG_UNMERGEABLE);

        if (this.list.remove(index) != null)
        {
            this.sync();
        }

        this.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected AnimationState create(String id)
    {
        return new AnimationState(id);
    }
}