package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.mc.ValueBlockState;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;

public class BlockForm extends Form
{
    public final ValueBlockState blockState = new ValueBlockState("block_state");
    public final ValueColor color = new ValueColor("color", Color.white());

    public BlockForm()
    {
        this.add(this.blockState);
        this.add(this.color);
    }

    @Override
    protected String getDefaultDisplayName()
    {
        return Registries.BLOCK.getId(this.blockState.get().getBlock()).toString();
    }
}