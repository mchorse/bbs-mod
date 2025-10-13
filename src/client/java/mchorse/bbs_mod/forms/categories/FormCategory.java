package mchorse.bbs_mod.forms.categories;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.forms.categories.UIFormCategory;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FormCategory implements IMapSerializable
{
    public IKey title;
    public final ValueBoolean visible;

    private final List<Form> forms = new ArrayList<>();

    public FormCategory(IKey title, ValueBoolean visible)
    {
        this.title = title;
        this.visible = visible;
    }

    public String getProcessedTitle()
    {
        return StringUtils.processColoredText(this.title.get());
    }

    public boolean canModify(Form form)
    {
        return false;
    }

    public List<Form> getForms()
    {
        return Collections.unmodifiableList(this.forms);
    }

    public List<Form> getDirectForms()
    {
        return this.forms;
    }

    public void addForm(Form form)
    {
        if (form != null)
        {
            this.forms.add(form);
        }
    }

    public void replaceForm(int index, Form form)
    {
        if (form != null && CollectionUtils.inRange(this.forms, index))
        {
            this.forms.set(index, form);
        }
    }

    public void removeForm(Form form)
    {
        this.forms.remove(form);
    }

    public UIFormCategory createUI(UIFormList list)
    {
        return new UIFormCategory(this, list);
    }

    @Override
    public void fromData(MapType data)
    {
        if (data.has("title"))
        {
            this.title = IKey.constant(data.getString("title"));
        }

        if (data.has("id"))
        {
            this.visible.setId(data.getString("id"));
        }

        for (BaseType formData : data.getList("forms"))
        {
            if (formData.isMap())
            {
                Form form = FormUtils.fromData(formData.asMap());

                if (form != null)
                {
                    this.forms.add(form);
                }
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        ListType forms = new ListType();

        data.putString("title", this.title.get());
        data.putString("id", this.visible.getId());
        data.put("forms", forms);

        for (int i = 0; i < this.forms.size(); i++)
        {
            MapType formData = FormUtils.toData(this.forms.get(i));

            if (formData != null)
            {
                forms.add(formData);
            }
            else
            {
                System.err.println("Form at index " + i + " is null in \"" + this.title.get() + "\" category!");
            }
        }
    }
}