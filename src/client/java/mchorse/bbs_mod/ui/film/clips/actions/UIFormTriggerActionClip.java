package mchorse.bbs_mod.ui.film.clips.actions;

import mchorse.bbs_mod.actions.types.FormTriggerActionClip;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.triggers.StateTrigger;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.utils.keys.KeyCodes;

public class UIFormTriggerActionClip extends UIActionClip<FormTriggerActionClip>
{
    public UIButton trigger;

    public UIFormTriggerActionClip(FormTriggerActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.trigger = new UIButton(UIKeys.ACTIONS_FORM_TRIGGER_PICK, (b) -> this.showActions());
    }

    private void showActions()
    {
        this.getContext().replaceContextMenu((menu) ->
        {
            if (this.clip.getParentValue().getParentValue() instanceof Replay replay && replay.form.get() instanceof ModelForm modelForm)
            {
                for (StateTrigger stateTrigger : modelForm.triggers.triggers)
                {
                    menu.action(IKey.constant(stateTrigger.action + " " + KeyCodes.getName(stateTrigger.hotkey)), () ->
                    {
                        this.editor.editMultiple(this.clip.trigger, (trigger) -> trigger.set(stateTrigger.id));
                    });
                }
            }
        });
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.trigger);
    }
}