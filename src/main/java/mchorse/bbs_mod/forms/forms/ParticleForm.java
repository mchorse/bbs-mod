package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;

public class ParticleForm extends Form
{
    public final ValueString effect = new ValueString("effect", null);
    public final ValueBoolean paused = new ValueBoolean("paused", false);
    public final ValueLink texture = new ValueLink("texture", null);

    public final ValueFloat user1 = new ValueFloat("user1", 0F);
    public final ValueFloat user2 = new ValueFloat("user2", 0F);
    public final ValueFloat user3 = new ValueFloat("user3", 0F);
    public final ValueFloat user4 = new ValueFloat("user4", 0F);
    public final ValueFloat user5 = new ValueFloat("user5", 0F);
    public final ValueFloat user6 = new ValueFloat("user6", 0F);

    public ParticleForm()
    {
        super();

        this.effect.invisible();

        this.add(this.effect);
        this.add(this.paused);
        this.add(this.texture);

        this.add(this.user1);
        this.add(this.user2);
        this.add(this.user3);
        this.add(this.user4);
        this.add(this.user5);
        this.add(this.user6);
    }

    @Override
    public String getDefaultDisplayName()
    {
        String effect = this.effect.get();

        return effect == null || effect.isEmpty() ? "none" : effect.toString();
    }
}