package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.mc.ValueBlockState;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

public class PlaceBlockActionClip extends BlockActionClip
{
    public final ValueBlockState state = new ValueBlockState("state");
    public final ValueBoolean drop = new ValueBoolean("drop", false);

    public PlaceBlockActionClip()
    {
        super();

        this.add(this.state);
        this.add(this.drop);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        BlockPos pos = new BlockPos(this.x.get(), this.y.get(), this.z.get());

        if (this.state.get().getBlock() == Blocks.AIR)
        {
            player.getWorld().breakBlock(pos, this.drop.get());
        }
        else
        {
            player.getWorld().setBlockState(pos, this.state.get());
        }
    }

    @Override
    protected Clip create()
    {
        return new PlaceBlockActionClip();
    }
}