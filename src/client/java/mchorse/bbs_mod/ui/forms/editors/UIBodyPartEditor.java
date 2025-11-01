package mchorse.bbs_mod.ui.forms.editors;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Pair;

public class UIBodyPartEditor extends UIScrollView
{
    public UIButton pick;
    public UIToggle useTarget;
    public UIStringList bone;
    public UIPropTransform transform;

    private final UIFormEditor editor;

    private BodyPart part;

    public UIBodyPartEditor(UIFormEditor editor)
    {
        this.editor = editor;

        this.pick = new UIButton(UIKeys.FORMS_EDITOR_PICK_FORM, (b) ->
        {
            UIForms.FormEntry current = this.editor.formsList.getCurrentFirst();

            this.editor.openFormList(current.part.getForm(), (f) ->
            {
                current.part.setForm(FormUtils.copy(f));

                Form partForm = current.part.getForm();

                if (partForm != null && partForm.getFormId().contains("particle"))
                {
                    current.part.useTarget.set(true);

                    this.useTarget.setValue(true);
                }

                this.editor.refreshFormList();
                this.editor.switchEditor(partForm);
            });
        });

        this.useTarget = new UIToggle(UIKeys.FORMS_EDITOR_USE_TARGET, (b) ->
        {
            this.part.useTarget.set(b.getValue());
        });

        this.bone = new UIStringList((l) -> this.part.bone.set(l.get(0)));
        this.bone.background().h(16 * 6);

        this.transform = new UIPropTransform().callbacks(() -> this.part.transform);

        this.pick.keys().register(Keys.FORMS_EDIT, this.pick::clickItself);

        this.column(5).vertical().stretch().scroll().padding(10);
        this.scroll.cancelScrolling();
    }

    public void setPart(BodyPart part, Form form)
    {
        this.part = part;

        this.removeAll();

        this.useTarget.setValue(part.useTarget.get());
        this.bone.clear();
        this.bone.add(FormUtilsClient.getBones(form));
        this.bone.sort();
        this.bone.setCurrentScroll(part.bone.get());

        if (!this.bone.getList().isEmpty())
        {
            this.add(this.pick, this.useTarget, UI.label(UIKeys.FORMS_EDITOR_BONE).marginTop(8), this.bone, this.transform);
        }
        else
        {
            this.add(this.pick, this.useTarget, this.transform);
        }

        this.transform.setTransform(part.transform.get());

        this.scroll.setScroll(0);
        this.resize();
    }

    public void pickBone(Pair<Form, String> pair)
    {
        /* Ctrl + clicking to pick the parent bone to attach to */
        if (this.part != null && this.bone.getList().contains(pair.b) && this.part.getManager().getOwner() == pair.a)
        {
            this.part.bone.set(pair.b);
            this.bone.setCurrentScroll(pair.b);
        }
    }
}