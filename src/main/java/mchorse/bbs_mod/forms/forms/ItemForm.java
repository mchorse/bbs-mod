package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.forms.values.ValueModelTransformationMode;
import mchorse.bbs_mod.settings.values.mc.ValueItemStack;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.registry.Registries;

public class ItemForm extends Form
{
    public final ValueItemStack stack = new ValueItemStack("item_stack");
    public final ValueModelTransformationMode modelTransform = new ValueModelTransformationMode("modelTransform", ModelTransformationMode.NONE);
    public final ValueColor color = new ValueColor("color", Color.white());

    public ItemForm()
    {
        this.add(this.stack);
        this.add(this.modelTransform);
        this.add(this.color);
    }

    @Override
    protected String getDefaultDisplayName()
    {
        return Registries.ITEM.getId(this.stack.get().getItem()).toString();
    }
}