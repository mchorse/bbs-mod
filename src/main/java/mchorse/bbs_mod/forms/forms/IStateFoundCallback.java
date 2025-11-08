package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.forms.states.AnimationState;

public interface IStateFoundCallback
{
    public void acceptState(Form form, AnimationState state);
}