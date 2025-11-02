package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.panels.UIStructureFormPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIStructureForm extends UIForm<StructureForm>
{
    public UIStructureForm()
    {
        super();

        this.defaultPanel = new UIStructureFormPanel(this);

        this.registerPanel(this.defaultPanel, IKey.raw("Structure"), Icons.POSE);
        this.registerDefaultPanels();
    }
}