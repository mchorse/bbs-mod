package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public class FramebufferForm extends Form
{
    public final ValueInt width = new ValueInt("width", 512);
    public final ValueInt height = new ValueInt("height", 512);
    public final ValueFloat scale = new ValueFloat("scale", 0.5F);

    public FramebufferForm()
    {
        this.width.invisible();
        this.height.invisible();

        this.add(this.width);
        this.add(this.height);
        this.add(this.scale);
    }
}