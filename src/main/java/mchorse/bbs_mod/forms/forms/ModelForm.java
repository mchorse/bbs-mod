package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.properties.ActionsConfigProperty;
import mchorse.bbs_mod.forms.properties.ColorProperty;
import mchorse.bbs_mod.forms.properties.LinkProperty;
import mchorse.bbs_mod.forms.properties.PoseProperty;
import mchorse.bbs_mod.forms.properties.ShapeKeysProperty;
import mchorse.bbs_mod.forms.properties.StringProperty;
import mchorse.bbs_mod.forms.triggers.StateTriggers;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;

public class ModelForm extends Form
{
    public final LinkProperty texture = new LinkProperty(this, "texture", null);
    public final StringProperty model = new StringProperty(this, "model", "");
    public final PoseProperty pose = new PoseProperty(this, "pose", new Pose());
    public final PoseProperty poseOverlay = new PoseProperty(this, "pose_overlay", new Pose());
    public final PoseProperty poseOverlay1 = new PoseProperty(this, "pose_overlay_1", new Pose());
    public final PoseProperty poseOverlay2 = new PoseProperty(this, "pose_overlay_2", new Pose());
    public final PoseProperty poseOverlay3 = new PoseProperty(this, "pose_overlay_3", new Pose());
    public final PoseProperty poseOverlay4 = new PoseProperty(this, "pose_overlay_4", new Pose());
    public final PoseProperty poseOverlay5 = new PoseProperty(this, "pose_overlay_5", new Pose());
    public final PoseProperty poseOverlay6 = new PoseProperty(this, "pose_overlay_6", new Pose());
    public final PoseProperty poseOverlay7 = new PoseProperty(this, "pose_overlay_7", new Pose());
    public final ActionsConfigProperty actions = new ActionsConfigProperty(this, "actions", new ActionsConfig());
    public final ColorProperty color = new ColorProperty(this, "color", Color.white());
    public final ShapeKeysProperty shapeKeys = new ShapeKeysProperty(this, "shape_keys", new ShapeKeys());
    public final StateTriggers triggers = new StateTriggers();

    public ModelForm()
    {
        super();

        this.register(this.texture);
        this.register(this.model);
        this.register(this.pose);
        this.register(this.poseOverlay);
        this.register(this.poseOverlay1);
        this.register(this.poseOverlay2);
        this.register(this.poseOverlay3);
        this.register(this.poseOverlay4);
        this.register(this.poseOverlay5);
        this.register(this.poseOverlay6);
        this.register(this.poseOverlay7);
        this.register(this.actions);
        this.register(this.color);
        this.register(this.shapeKeys);
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean result = super.equals(obj);

        if (result && obj instanceof ModelForm form)
        {
            result = result && this.triggers.equals(form.triggers);
        }

        return result;
    }

    @Override
    public String getDefaultDisplayName()
    {
        return this.model.get();
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);

        this.triggers.fromData(data.getMap("stateTriggers"));
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);

        data.put("stateTriggers", this.triggers.toData());
    }
}