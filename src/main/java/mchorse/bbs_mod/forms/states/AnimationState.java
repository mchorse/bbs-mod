package mchorse.bbs_mod.forms.states;

import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.settings.values.core.ValueGroup;

public class AnimationState extends ValueGroup
{
    public final FormProperties properties = new FormProperties("properties");

    public AnimationState(String id)
    {
        super(id);

        this.add(this.properties);
    }
}