package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UIStructureOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;

public class UIStructureFormPanel extends UIFormPanel<StructureForm>
{
    public UIButton pickStructure;
    public UITextbox structureFile;
    public UIColor color;

    public UIStructureFormPanel(UIForm editor)
    {
        super(editor);

        this.pickStructure = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE, (b) -> this.pickStructure());
        this.structureFile = new UITextbox(100, (s) -> this.form.structureFile.set(s)).path().border();
        this.color = new UIColor((c) -> this.form.color.set(Color.rgba(c))).withAlpha();

        /* Quitar etiquetas; mostrar solo los controles */
        this.options.add(this.color);
        this.options.add(this.pickStructure);
    }

    private void pickStructure()
    {
        UIStructureOverlayPanel overlay = new UIStructureOverlayPanel(
                UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE,
                (link) -> this.setStructure(link)
        );

        String current = this.form.structureFile.get();
        if (current == null || current.isEmpty())
        {
            overlay.set("");
        }
        else
        {
            try
            {
                overlay.set(Link.assets(current));
            }
            catch (Exception e)
            {
                overlay.set("");
            }
        }
        UIOverlay.addOverlay(this.getContext(), overlay, 0.4F, 0.8F);
    }

    private void setStructure(Link link)
    {
        String path = link == null ? "" : link.path;

        this.form.structureFile.set(path);
        this.structureFile.setText(path);
    }

    @Override
    public void startEdit(StructureForm form)
    {
        super.startEdit(form);

        this.structureFile.setText(form.structureFile.get());
        this.color.setColor(form.color.get().getARGBColor());
    }
}