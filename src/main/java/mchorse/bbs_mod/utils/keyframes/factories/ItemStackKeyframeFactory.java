package mchorse.bbs_mod.utils.keyframes.factories;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

import java.util.Optional;

public class ItemStackKeyframeFactory implements IKeyframeFactory<ItemStack>
{
    @Override
    public ItemStack fromData(BaseType data)
    {
        DataResult<Pair<ItemStack, NbtElement>> decode = ItemStack.CODEC.decode(NbtOps.INSTANCE, DataStorageUtils.toNbt(data));
        Optional<Pair<ItemStack, NbtElement>> result = decode.result();

        return result.map(Pair::getFirst).orElse(ItemStack.EMPTY);
    }

    @Override
    public BaseType toData(ItemStack value)
    {
        Optional<NbtElement> result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, value).result();

        return result.map(DataStorageUtils::fromNbt).orElse(new MapType());
    }

    @Override
    public ItemStack createEmpty()
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean compare(Object a, Object b)
    {
        if (a instanceof ItemStack itemA && b instanceof ItemStack itemB)
        {
            return ItemStack.areEqual(itemA, itemB);
        }

        return false;
    }

    @Override
    public ItemStack copy(ItemStack value)
    {
        return value.copy();
    }

    @Override
    public ItemStack interpolate(ItemStack preA, ItemStack a, ItemStack b, ItemStack postB, IInterp interpolation, float x)
    {
        return a;
    }
}