package mchorse.bbs_mod.forms.states;

import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

import java.util.UUID;

public class AnimationState extends ValueGroup
{
    public final ValueString id = new ValueString("id", UUID.randomUUID().toString());
    public final ValueString customId = new ValueString("custom_id", "");
    public final FormProperties properties = new FormProperties("properties");
    public final ValueBoolean main = new ValueBoolean("main", false);
    public final ValueInt keybind = new ValueInt("keybind", 0);
    public final ValueInt duration = new ValueInt("duration", 100);
    public final ValueInt fadeIn = new ValueInt("fade_in", 0);
    public final ValueInt fadeOut = new ValueInt("fade_out", 0);
    public final ValueBoolean looping = new ValueBoolean("looping", false);
    public final ValueInt offset = new ValueInt("offset", 0);

    public AnimationState(String id)
    {
        super(id);

        this.add(this.id);
        this.add(this.customId);
        this.add(this.properties);
        this.add(this.main);
        this.add(this.keybind);
        this.add(this.duration);
        this.add(this.fadeIn);
        this.add(this.fadeOut);
        this.add(this.looping);
        this.add(this.offset);
    }
}