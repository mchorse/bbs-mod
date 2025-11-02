package mchorse.bbs_mod.forms.states;

import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class AnimationStates extends ValueList<AnimationState>
{
    public AnimationStates(String id)
    {
        super(id);
    }

    public AnimationState getById(String triggerId)
    {
        for (AnimationState state : this.list)
        {
            if (state.id.get().equals(triggerId))
            {
                return state;
            }
        }

        return null;
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

    public AnimationState getMainRandom()
    {
        List<AnimationState> states = new ArrayList<>();

        for (AnimationState state : this.list)
        {
            if (state.main.get())
            {
                states.add(state);
            }
        }

        if (!states.isEmpty())
        {
            int index = (int) (Math.random() * (states.size() + 1));

            return CollectionUtils.getSafe(states, index);
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

    public void cleanUp()
    {
        for (AnimationState state : this.list)
        {
            state.properties.cleanUp();
        }
    }

    @Override
    protected AnimationState create(String id)
    {
        return new AnimationState(id);
    }
}