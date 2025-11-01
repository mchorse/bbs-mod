package mchorse.bbs_mod.ui.forms.editors;

import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.utils.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class UIForms extends UIList<UIForms.FormEntry>
{
    public UIForms(Consumer<List<FormEntry>> callback)
    {
        super(callback);

        this.scroll.cancelScrolling();
    }

    public void setCurrentForm(Form form)
    {
        FormEntry toSelect = null;

        for (FormEntry entry : this.list)
        {
            if (entry.getForm() == form)
            {
                toSelect = entry;

                break;
            }
        }

        if (toSelect != null)
        {
            this.setCurrentScroll(toSelect);
        }
    }

    public void setForm(Form form)
    {
        this.clear();

        this.add(new FormEntry(form, null, 0));

        for (BodyPart part : form.parts.getAllTyped())
        {
            this.setupRecursively(form, part, 1);
        }
    }

    private void setupRecursively(Form parent, BodyPart part, int depth)
    {
        this.add(new FormEntry(parent, part, depth));

        if (part.getForm() == null)
        {
            return;
        }

        for (BodyPart childPart : part.getForm().parts.getAllTyped())
        {
            this.setupRecursively(part.getForm(), childPart, depth + 1);
        }
    }

    @Override
    protected void renderElementPart(UIContext context, FormEntry element, int i, int x, int y, boolean hover, boolean selected)
    {
        super.renderElementPart(context, element, i, x, y, hover, selected);

        Form form = element.getForm();

        if (form != null)
        {
            x += this.area.w - 40;

            context.batcher.clip(x, y, 40, 20, context);

            y -= 10;

            FormUtilsClient.renderUI(form, context, x, y, x + 40, y + 40);

            context.batcher.unclip(context);
        }
    }

    @Override
    protected String elementToString(UIContext context, int i, FormEntry element)
    {
        return StringUtils.repeat("  ", element.depth * 2) + element.toString();
    }

    public static class FormEntry
    {
        public Form form;
        public BodyPart part;
        public int depth;

        public FormEntry(Form form, BodyPart part, int depth)
        {
            this.form = form;
            this.part = part;
            this.depth = depth;
        }

        public Form getForm()
        {
            return this.part == null ? this.form : this.part.getForm();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (super.equals(obj))
            {
                return true;
            }

            if (obj instanceof FormEntry)
            {
                FormEntry entry = (FormEntry) obj;

                return Objects.equals(this.form, entry.form)
                    && Objects.equals(this.part, entry.part)
                    && this.depth == entry.depth;
            }

            return false;
        }

        @Override
        public String toString()
        {
            if (this.part == null)
            {
                return this.form.getFormIdOrName();
            }
            else if (this.part.getForm() == null)
            {
                return "-";
            }

            return this.part.getForm().getFormIdOrName();
        }
    }
}