package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.panels.UIActionsFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.UIModelFormPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.StringUtils;
import org.joml.Matrix4f;

public class UIModelForm extends UIForm<ModelForm>
{
    public UIModelFormPanel modelPanel;

    public UIModelForm()
    {
        this.modelPanel = new UIModelFormPanel(this);
        this.defaultPanel = this.modelPanel;

        this.registerPanel(this.defaultPanel, UIKeys.FORMS_EDITORS_MODEL_POSE, Icons.POSE);
        this.registerPanel(new UIActionsFormPanel(this), UIKeys.FORMS_EDITORS_ACTIONS_TITLE, Icons.MORE);
        this.registerDefaultPanels();

        this.defaultPanel.keys().register(Keys.FORMS_PICK_TEXTURE, () ->
        {
            if (this.view != this.modelPanel)
            {
                this.setPanel(this.modelPanel);
            }

            this.modelPanel.pick.clickItself();
        });
    }

    @Override
    public Matrix4f getOrigin(float transition)
    {
        String path = FormUtils.getPath(this.form);
        UIPoseEditor poseEditor = this.modelPanel.poseEditor;

        return this.getOrigin(transition, StringUtils.combinePaths(path, poseEditor.groups.getCurrentFirst()), poseEditor.transform.isLocal());
    }
}