package mchorse.bbs_mod.film;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.film.replays.Inventory;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.Replays;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clips;

public class Film extends ValueGroup
{
    public final Clips camera = new Clips("camera", BBSMod.getFactoryCameraClips());
    public final Replays replays = new Replays("replays");

    public final Inventory inventory = new Inventory("inventory");
    public final ValueFloat hp = new ValueFloat("hp", 20F);
    public final ValueFloat hunger = new ValueFloat("hunger", 20F);
    public final ValueInt xpLevel = new ValueInt("xp_level", 0);
    public final ValueFloat xpProgress = new ValueFloat("xp_progress", 0F);

    public Film()
    {
        super("");

        this.add(this.camera);
        this.add(this.replays);

        this.add(this.inventory);
        this.add(this.hp);
        this.add(this.hunger);
        this.add(this.xpLevel);
        this.add(this.xpProgress);
    }

    public Replay getFirstPersonReplay()
    {
        for (Replay replay : this.replays.getList())
        {
            if (replay.fp.get())
            {
                return replay;
            }
        }

        return null;
    }

    public boolean hasFirstPerson()
    {
        return this.getFirstPersonReplay() != null;
    }
}