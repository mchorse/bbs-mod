package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

public class BreakBlockActionClip extends BlockActionClip
{
    public final ValueInt progress = new ValueInt("progress", 0);

    public BreakBlockActionClip()
    {
        super();

        this.add(this.progress);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        player.getWorld().setBlockBreakingInfo(player.getId(), new BlockPos(this.x.get(), this.y.get(), this.z.get()), this.progress.get());
    }

    @Override
    protected Clip create()
    {
        return new BreakBlockActionClip();
    }
}