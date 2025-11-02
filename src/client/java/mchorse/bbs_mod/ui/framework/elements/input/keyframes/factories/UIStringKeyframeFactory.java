package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.utils.UIStructureOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIStringKeyframeFactory extends UIKeyframeFactory<String>
{
    private UITextbox string;

    public UIStringKeyframeFactory(Keyframe<String> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        /* Campo de texto por defecto */
        this.string = new UITextbox(1000, this::setValue);
        this.string.setText(keyframe.getValue());

        /* Si la pista es structure_file, añadir botón de selección de estructura */
        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
        boolean isStructureFile = sheet != null && ("structure_file".equals(sheet.id) || sheet.id.endsWith("/structure_file"));

        if (isStructureFile)
        {
            UIButton pickStructure = new UIButton(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE, (b) ->
            {
                UIStructureOverlayPanel panel = new UIStructureOverlayPanel(UIKeys.FORMS_EDITORS_STRUCTURE_PICK_STRUCTURE, (link) ->
                {
                    String value = link == null ? "" : link.path;
                    this.editor.getGraph().setValue(value, true);
                });

                String current = this.keyframe.getValue();
                if (current != null && !current.isEmpty())
                {
                    panel.set(Link.assets(current));
                }

                UIOverlay.addOverlay(this.getContext(), panel, 280, 0.5F);
            });

            this.scroll.add(pickStructure);
        }

        this.scroll.add(this.string);
    }
}