package mchorse.bbs_mod.actions.types.chat;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.ServerCommandSource;

public class CommandActionClip extends ActionClip
{
    public final ValueString command = new ValueString("command", "");

    public CommandActionClip()
    {
        this.add(this.command);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        this.applyPositionRotation(player, replay, tick);

        String command = this.command.get();
        ServerCommandSource source = actor == null
            ? player.getCommandSource()
            : actor.getCommandSource();

        player.getServer().getCommandManager().executeWithPrefix(source, command);
    }

    @Override
    protected Clip create()
    {
        return new CommandActionClip();
    }
}