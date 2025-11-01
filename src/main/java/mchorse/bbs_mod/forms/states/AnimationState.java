package mchorse.bbs_mod.forms.states;

import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public class AnimationState extends ValueGroup
{
    public final FormProperties properties = new FormProperties("properties");
    public final ValueBoolean main = new ValueBoolean("main", false);
    public final ValueInt keybind = new ValueInt("keybind", 0);
    public final ValueInt duration = new ValueInt("duration", 100);

    public AnimationState(String id)
    {
        super(id);

        this.add(this.properties);
        this.add(this.main);
        this.add(this.keybind);
        this.add(this.duration);
    }
}