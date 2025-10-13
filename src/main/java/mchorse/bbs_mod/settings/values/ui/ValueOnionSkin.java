package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.colors.Colors;

public class ValueOnionSkin extends ValueGroup
{
    public final ValueBoolean enabled = new ValueBoolean("enabled", false);

    public final ValueInt preFrames = new ValueInt("pre_frames", 1, 0, 10);
    public final ValueInt preColor = new ValueInt("pre_color", Colors.NEGATIVE | Colors.A75);

    public final ValueInt postFrames = new ValueInt("post_frames", 1, 0, 10);
    public final ValueInt postColor = new ValueInt("post_color", Colors.POSITIVE | Colors.A75);

    public final ValueBoolean all = new ValueBoolean("all", false);
    public final ValueString group  = new ValueString("group", "pose");

    public ValueOnionSkin(String id)
    {
        super(id);

        this.add(this.enabled);
        this.add(this.preFrames);
        this.add(this.preColor);
        this.add(this.postFrames);
        this.add(this.postColor);
        this.add(this.all);
        this.add(this.group);
    }
}