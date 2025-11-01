package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.misc.ValueVector4f;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.utils.colors.Color;
import org.joml.Vector4f;

public class BillboardForm extends Form
{
    public final ValueLink texture = new ValueLink("texture", null);
    public final ValueBoolean billboard = new ValueBoolean("billboard", false);
    public final ValueBoolean linear = new ValueBoolean("linear", false);
    public final ValueBoolean mipmap = new ValueBoolean("mipmap", false);
    public final ValueVector4f crop = new ValueVector4f("crop", new Vector4f(0, 0, 0, 0));
    public final ValueBoolean resizeCrop = new ValueBoolean( "resizeCrop", false);
    public final ValueColor color = new ValueColor("color", Color.white());
    public final ValueFloat offsetX = new ValueFloat("offsetX", 0F);
    public final ValueFloat offsetY = new ValueFloat("offsetY", 0F);
    public final ValueFloat rotation = new ValueFloat("rotation", 0F);
    public final ValueBoolean shading = new ValueBoolean("shading", true);

    public BillboardForm()
    {
        super();

        this.linear.invisible();
        this.mipmap.invisible();
        this.resizeCrop.invisible();
        this.shading.invisible();

        this.add(this.texture);
        this.add(this.billboard);
        this.add(this.linear);
        this.add(this.mipmap);
        this.add(this.crop);
        this.add(this.resizeCrop);
        this.add(this.color);
        this.add(this.offsetX);
        this.add(this.offsetY);
        this.add(this.rotation);
        this.add(this.shading);
    }

    @Override
    public String getDefaultDisplayName()
    {
        Link link = this.texture.get();

        return link == null ? "none" : link.toString();
    }
}