package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.utils.interps.IInterp;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InventoryKeyframeFactory implements IKeyframeFactory<List<ItemStack>>
{
    @Override
    public List<ItemStack> fromData(BaseType data)
    {
        if (data instanceof ListType listType)
        {
            List<ItemStack> inventory = new ArrayList<>();
            for (BaseType item : listType.elements)
            {
                ItemStack stack = KeyframeFactories.ITEM_STACK.fromData(item);
                inventory.add(stack != null ? stack : ItemStack.EMPTY);
            }
            return inventory;
        }
        return new ArrayList<>();
    }

    @Override
    public BaseType toData(List<ItemStack> value)
    {
        ListType listType = new ListType();
        for (ItemStack stack : value)
        {
            listType.add(KeyframeFactories.ITEM_STACK.toData(stack));
        }
        return listType;
    }

    @Override
    public List<ItemStack> createEmpty()
    {
        return new ArrayList<>();
    }

    @Override
    public boolean compare(Object a, Object b)
    {
        if (a instanceof List<?> listA && b instanceof List<?> listB)
        {
            if (listA.size() != listB.size())
            {
                return false;
            }
            
            for (int i = 0; i < listA.size(); i++)
            {
                Object itemA = listA.get(i);
                Object itemB = listB.get(i);
                
                if (itemA instanceof ItemStack stackA && itemB instanceof ItemStack stackB)
                {
                    if (!ItemStack.areEqual(stackA, stackB))
                    {
                        return false;
                    }
                }
                else if (!Objects.equals(itemA, itemB))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public List<ItemStack> copy(List<ItemStack> value)
    {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : value)
        {
            copy.add(stack.copy());
        }
        return copy;
    }

    @Override
    public List<ItemStack> interpolate(List<ItemStack> preA, List<ItemStack> a, List<ItemStack> b, List<ItemStack> postB, IInterp interpolation, float x)
    {
        return a;
    }
}
