package mchorse.bbs_mod.ui.forms.editors.states;

import mchorse.bbs_mod.forms.states.AnimationState;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;

import java.util.List;
import java.util.function.Consumer;

public class UIAnimationStateList extends UIList<AnimationState>
{
    public UIAnimationStateList(Consumer<List<AnimationState>> callback)
    {
        super(callback);

        this.scroll.scrollItemSize = 16;
    }

    @Override
    protected String elementToString(UIContext context, int i, AnimationState element)
    {
        return element.getId();
    }
}