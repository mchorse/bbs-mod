package mchorse.bbs_mod.forms.values;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import net.minecraft.client.render.model.json.ModelTransformationMode;

public class ValueModelTransformationMode extends BaseValueBasic<ModelTransformationMode>
{
    public ValueModelTransformationMode(String id, ModelTransformationMode value)
    {
        super(id, value);
    }

    @Override
    public BaseType toData()
    {
        return new StringType((this.value == null ? ModelTransformationMode.NONE : this.value).asString());
    }

    @Override
    public void fromData(BaseType data)
    {
        String string = data.isString() ? data.asString() : "";

        this.set(ModelTransformationMode.NONE);

        for (ModelTransformationMode value : ModelTransformationMode.values())
        {
            if (value.asString().equals(string))
            {
                this.set(value);

                break;
            }
        }
    }
}