package mchorse.bbs_mod.settings.values.mc;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.item.ItemStack;

public class ValueItemStack extends BaseValueBasic<ItemStack>
{
    public ValueItemStack(String id)
    {
        super(id, ItemStack.EMPTY);
    }

    @Override
    public BaseType toData()
    {
        return KeyframeFactories.ITEM_STACK.toData(this.value);
    }

    @Override
    public void fromData(BaseType data)
    {
        this.set(KeyframeFactories.ITEM_STACK.fromData(data));
    }
}