package mchorse.bbs_mod.forms.states;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueGroup;

public class AnimationStates extends ValueGroup
{
    public AnimationStates(String id)
    {
        super(id);
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        if (data instanceof MapType map)
        {
            for (String key : map.keys())
            {
                MapType mapMap = map.getMap(key);
                AnimationState state = new AnimationState(key);

                state.fromData(mapMap);
                this.add(state);
            }
        }
    }
}