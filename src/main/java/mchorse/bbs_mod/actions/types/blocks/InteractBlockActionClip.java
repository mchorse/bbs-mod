package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.actions.values.ValueBlockHitResult;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public class InteractBlockActionClip extends ActionClip
{
    public final ValueBlockHitResult hit = new ValueBlockHitResult("hit");
    public final ValueBoolean hand = new ValueBoolean("hand", true);

    public InteractBlockActionClip()
    {
        super();

        this.add(this.hit);
        this.add(this.hand);
    }

    @Override
    public void shift(double dx, double dy, double dz)
    {
        super.shift(dx, dy, dz);

        this.hit.shift(dx, dy, dz);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        this.applyPositionRotation(player, replay, tick);

        BlockHitResult result = this.hit.getHitResult();

        player.getWorld().getBlockState(result.getBlockPos()).onUse(player.getWorld(), player, this.hand.get() ? Hand.MAIN_HAND : Hand.OFF_HAND, result);
    }

    @Override
    protected Clip create()
    {
        return new InteractBlockActionClip();
    }
}