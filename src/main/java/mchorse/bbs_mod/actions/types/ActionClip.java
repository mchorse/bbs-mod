package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public abstract class ActionClip extends Clip
{
    public final ValueInt frequency = new ValueInt("frequency", 0, 0, 1000);

    public ActionClip()
    {
        this.add(this.frequency);
    }

    public boolean isClient()
    {
        return false;
    }

    public final void applyClient(IEntity entity, Film film, Replay replay, int tick)
    {
        if (!this.enabled.get())
        {
            return;
        }

        int relaive = tick - this.tick.get();
        int frequency = this.frequency.get();

        if (frequency == 0)
        {
            if (relaive == 0)
            {
                this.applyClientAction(entity, film, replay, tick);
            }
        }
        else if (relaive % frequency == 0)
        {
            this.applyClientAction(entity, film, replay, tick);
        }
    }

    protected void applyClientAction(IEntity entity, Film film, Replay replay, int tick)
    {}

    public final void apply(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        if (!this.enabled.get())
        {
            return;
        }

        int relaive = tick - this.tick.get();
        int frequency = this.frequency.get();

        if (frequency == 0)
        {
            if (relaive == 0)
            {
                this.applyAction(actor, player, film, replay, tick);
            }
        }
        else if (relaive % frequency == 0)
        {
            this.applyAction(actor, player, film, replay, tick);
        }
    }

    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {}

    protected void applyPositionRotation(SuperFakePlayer player, Replay replay, int tick)
    {
        ReplayKeyframes keyframes = replay.keyframes;

        player.setPosition(keyframes.x.interpolate(tick), keyframes.y.interpolate(tick), keyframes.z.interpolate(tick));
        player.setYaw(keyframes.yaw.interpolate(tick).floatValue());
        player.setHeadYaw(keyframes.headYaw.interpolate(tick).floatValue());
        player.setBodyYaw(keyframes.bodyYaw.interpolate(tick).floatValue());
        player.setPitch(keyframes.pitch.interpolate(tick).floatValue());
        player.setStackInHand(Hand.MAIN_HAND, keyframes.mainHand.interpolate(tick, ItemStack.EMPTY).copy());
        player.setStackInHand(Hand.OFF_HAND, keyframes.offHand.interpolate(tick, ItemStack.EMPTY).copy());
    }
}