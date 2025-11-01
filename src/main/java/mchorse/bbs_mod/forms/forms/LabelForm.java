package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.colors.Color;

public class LabelForm extends Form
{
    public final ValueString text = new ValueString("text", "Hello, World!");
    public final ValueBoolean billboard = new ValueBoolean("billboard", false);
    public final ValueColor color = new ValueColor("color", Color.white());

    public final ValueInt max = new ValueInt("max", -1);
    public final ValueFloat anchorX = new ValueFloat("anchorX", 0.5F);
    public final ValueFloat anchorY = new ValueFloat("anchorY", 0.5F);
    public final ValueBoolean anchorLines = new ValueBoolean("anchorLines", false);

    /* Shadow properties */
    public final ValueFloat shadowX = new ValueFloat("shadowX", 1F);
    public final ValueFloat shadowY = new ValueFloat("shadowY", 1F);
    public final ValueColor shadowColor = new ValueColor("shadowColor", new Color(0, 0, 0, 0));

    /* Background */
    public final ValueColor background = new ValueColor("background", new Color(0, 0, 0, 0));
    public final ValueFloat offset = new ValueFloat("offset", 3F);

    public LabelForm()
    {
        super();

        this.add(this.text);
        this.add(this.billboard);
        this.add(this.color);
        this.add(this.max);
        this.add(this.anchorX);
        this.add(this.anchorY);
        this.add(this.anchorLines);
        this.add(this.shadowX);
        this.add(this.shadowY);
        this.add(this.shadowColor);
        this.add(this.background);
        this.add(this.offset);
    }

    @Override
    public String getDefaultDisplayName()
    {
        return this.text.get();
    }
}