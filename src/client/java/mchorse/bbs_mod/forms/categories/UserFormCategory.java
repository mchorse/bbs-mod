package mchorse.bbs_mod.forms.categories;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.sections.UserFormSection;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.forms.categories.UIFormCategory;
import mchorse.bbs_mod.ui.forms.categories.UIUserFormCategory;

public class UserFormCategory extends FormCategory
{
    private UserFormSection section;

    public UserFormCategory(IKey title, ValueBoolean visibility, UserFormSection section)
    {
        super(title, visibility);

        this.section = section;
    }

    @Override
    public boolean canModify(Form form)
    {
        return true;
    }

    @Override
    public UIFormCategory createUI(UIFormList list)
    {
        return new UIUserFormCategory(this, list);
    }

    @Override
    public void addForm(Form form)
    {
        super.addForm(form);

        this.section.writeUserCategories(this);
    }

    @Override
    public void replaceForm(int index, Form form)
    {
        super.replaceForm(index, form);

        this.section.writeUserCategories(this);
    }

    @Override
    public void removeForm(Form form)
    {
        super.removeForm(form);

        this.section.writeUserCategories(this);
    }
}