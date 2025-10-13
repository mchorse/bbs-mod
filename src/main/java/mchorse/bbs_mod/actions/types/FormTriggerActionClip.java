package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.clips.Clip;

public class FormTriggerActionClip extends ActionClip
{
    public final ValueString trigger = new ValueString("trigger", "");

    public FormTriggerActionClip()
    {
        this.add(this.trigger);
    }

    @Override
    public boolean isClient()
    {
        return true;
    }

    @Override
    protected void applyClientAction(IEntity entity, Film film, Replay replay, int tick)
    {}

    @Override
    protected Clip create()
    {
        return new FormTriggerActionClip();
    }
}