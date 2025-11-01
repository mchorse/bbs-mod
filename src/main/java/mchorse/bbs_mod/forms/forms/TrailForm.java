package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.core.ValueLink;

public class TrailForm extends Form
{
    public final ValueLink texture = new ValueLink("texture", Link.assets("textures/default_trail.png"));
    public final ValueFloat length = new ValueFloat("length", 10F);
    public final ValueBoolean loop = new ValueBoolean("loop", false);
    public final ValueBoolean paused = new ValueBoolean("paused", false);
    
    public TrailForm()
    {
        this.add(this.texture);
        this.add(this.length);
        this.add(this.loop);
        this.add(this.paused);
    }
}