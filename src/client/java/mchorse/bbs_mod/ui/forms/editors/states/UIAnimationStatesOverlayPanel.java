package mchorse.bbs_mod.ui.forms.editors.states;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.states.AnimationState;
import mchorse.bbs_mod.forms.states.AnimationStates;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIKeybind;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

public class UIAnimationStatesOverlayPanel extends UIOverlayPanel
{
    public UIList<AnimationState> list;
    public UIScrollView editor;

    public UIToggle main;
    public UIKeybind keybind;
    public UITrackpad duration;

    protected AnimationStates states;
    protected AnimationState state;

    private Consumer<AnimationState> callback;

    public UIAnimationStatesOverlayPanel(AnimationStates states, AnimationState current, Consumer<AnimationState> consumer)
    {
        super(IKey.raw("Animation states"));

        this.states = states;
        this.callback = consumer;

        this.list = this.createList();
        this.list.context((menu) ->
        {
            menu.action(Icons.ADD, IKey.raw("Add an animation state"), this::addState);

            if (!this.list.getList().isEmpty())
            {
                menu.action(Icons.REMOVE, IKey.raw("Remove animation state"), Colors.NEGATIVE, this::removeState);
            }
        });
        this.list.background();

        this.main = new UIToggle(IKey.raw("Main"), (b) ->
        {
            /* There can be only one main */
            for (AnimationState state : this.states.getList())
            {
                state.main.set(false);
            }

            this.state.main.set(b.getValue());
        });
        this.keybind = new UIKeybind((keybind) -> this.state.keybind.set(keybind.getMainKey()));
        this.keybind.single();
        this.duration = new UITrackpad((v) -> this.state.duration.set(v.intValue())).integer().limit(0D);

        this.editor = UI.scrollView(this.main, this.keybind, UI.label(IKey.raw("Duration")).marginTop(6), this.duration);

        this.list.relative(this.content).w(120).h(1F);
        this.list.setList(states.getList());
        this.list.setCurrentScroll(current);
        this.editor.relative(this.content).x(120).w(1F, -120).h(1F).column(5).vertical().stretch().scroll().padding(10);

        this.content.add(this.editor, this.list);

        this.pickItem(this.list.getCurrentFirst(), false);
    }

    protected UIList<AnimationState> createList()
    {
        return new UIAnimationStateList((l) -> this.pickItem(l.get(0), false));
    }

    protected void addState()
    {
        this.pickItem(this.states.addState(), true);
        this.list.update();
    }

    protected void removeState()
    {
        int index = this.list.getIndex();

        this.states.removeState(index);

        this.pickItem(CollectionUtils.getSafe(this.list.getList(), Math.max(index - 1, 0)), true);
        this.list.update();
    }

    protected void pickItem(AnimationState state, boolean select)
    {
        this.state = state;

        if (this.callback != null)
        {
            this.callback.accept(state);
        }

        this.editor.setVisible(state != null);

        if (state != null)
        {
            this.fillData(state);

            if (select)
            {
                this.list.setCurrentScroll(state);
            }

            this.resize();
        }
        else
        {
            this.list.deselect();
        }
    }

    protected void fillData(AnimationState state)
    {
        this.main.setValue(state.main.get());
        this.keybind.setKeyCombo(new KeyCombo(IKey.EMPTY, state.keybind.get()));
        this.duration.setValue(state.duration.get());
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        int selected = data.getInt("selected");

        this.pickItem(CollectionUtils.getSafe(this.states.getList(), selected), true);
        this.list.update();
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        data.putInt("selected", this.list.getIndex());
    }
}