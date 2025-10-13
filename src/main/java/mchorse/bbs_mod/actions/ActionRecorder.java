package mchorse.bbs_mod.actions;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.actions.types.AttackActionClip;
import mchorse.bbs_mod.actions.types.SwipeActionClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.utils.clips.Clips;
import net.minecraft.server.network.ServerPlayerEntity;

public class ActionRecorder
{
    private Film film;
    private ServerPlayerEntity entity;
    private Clips clips = new Clips("...", BBSMod.getFactoryActionClips());
    private int tick;
    private int countdown;
    private int initialTick;

    public ActionRecorder(Film film, ServerPlayerEntity entity, int tick, int countdown)
    {
        this.film = film;
        this.entity = entity;
        this.tick = tick;
        this.countdown = countdown;
        this.initialTick = tick;
    }

    public Film getFilm()
    {
        return this.film;
    }

    public Clips getClips()
    {
        return this.clips;
    }

    public int getInitialTick()
    {
        return this.initialTick;
    }

    public Clips composeClips()
    {
        Clips clips = this.clips;

        clips.sortLayers();

        return clips;
    }

    public void add(ActionClip clip)
    {
        if (this.countdown > 0)
        {
            return;
        }

        clip.tick.set(this.tick);
        clip.duration.set(1);

        this.clips.addClip(clip);
    }

    public void tick(ServerPlayerEntity player)
    {
        if (this.countdown > 0)
        {
            this.countdown -= 1;

            return;
        }

        if (player.handSwingTicks == -1)
        {
            this.add(new SwipeActionClip());

            if (BBSSettings.recordingSwipeDamage.get())
            {
                AttackActionClip clip = new AttackActionClip();

                clip.damage.set(2F);
                this.add(clip);
            }
        }

        this.tick += 1;
    }
}