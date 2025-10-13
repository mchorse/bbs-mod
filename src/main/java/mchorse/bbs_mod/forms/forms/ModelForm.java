package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.triggers.StateTriggers;
import mchorse.bbs_mod.forms.values.ValueActionsConfig;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.forms.values.ValueShapeKeys;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;

public class ModelForm extends Form
{
    public final ValueLink texture = new ValueLink("texture", null);
    public final ValueString model = new ValueString("model", "");
    public final ValuePose pose = new ValuePose("pose", new Pose());
    public final ValuePose poseOverlay = new ValuePose("pose_overlay", new Pose());
    public final ValueActionsConfig actions = new ValueActionsConfig("actions", new ActionsConfig());
    public final ValueColor color = new ValueColor("color", Color.white());
    public final ValueShapeKeys shapeKeys = new ValueShapeKeys("shape_keys", new ShapeKeys());
    public final StateTriggers triggers = new StateTriggers();

    public ModelForm()
    {
        super();

        this.register(this.texture);
        this.register(this.model);
        this.register(this.pose);
        this.register(this.poseOverlay);
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