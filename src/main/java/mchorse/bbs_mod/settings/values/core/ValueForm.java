package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

public class ValueForm extends BaseValueBasic<Form>
{
    public ValueForm(String id)
    {
        super(id, null);
    }

    @Override
    public BaseType toData()
    {
        return this.value == null ? null : FormUtils.toData(this.value);
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data != null && data.isMap())
        {
            this.value = FormUtils.fromData(data.asMap());
        }
        else
        {
            this.value = null;
        }
    }
}