package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.utils.pose.UIActionsConfigEditor;

public class UIActionsFormPanel extends UIFormPanel<ModelForm>
{
    public UIActionsConfigEditor editor;

    public UIActionsFormPanel(UIForm editor)
    {
        super(editor);

        this.editor = new UIActionsConfigEditor(() ->
        {
            this.form.actions.preNotify();
        }, () ->
        {
            ((ModelFormRenderer) FormUtilsClient.getRenderer(this.form)).resetAnimator();
            this.form.postNotify();
        });
        this.editor.setUndoId("model_action_editor");

        this.options.add(this.editor);
    }

    @Override
    public void startEdit(ModelForm form)
    {
        super.startEdit(form);

        this.editor.setConfigs(form.actions.get(), this.form);
    }

    @Override
    public void finishEdit()
    {
        super.finishEdit();

        ActionsConfig.removeDefaultActions(this.form.actions.get().actions);
    }
}