package mchorse.bbs_mod.actions.types.chat;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class ChatActionClip extends ActionClip
{
    public final ValueString message = new ValueString("message", "");

    public ChatActionClip()
    {
        this.add(this.message);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        for (PlayerEntity entity : player.getWorld().getPlayers())
        {
            entity.sendMessage(Text.literal(StringUtils.processColoredText(this.message.get())));
        }
    }

    @Override
    protected Clip create()
    {
        return new ChatActionClip();
    }
}