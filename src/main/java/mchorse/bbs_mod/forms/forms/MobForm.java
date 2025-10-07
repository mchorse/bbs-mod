package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.forms.properties.BooleanProperty;
import mchorse.bbs_mod.forms.properties.LinkProperty;
import mchorse.bbs_mod.forms.properties.PoseProperty;
import mchorse.bbs_mod.forms.properties.StringProperty;
import mchorse.bbs_mod.utils.pose.Pose;

public class MobForm extends Form
{
    public final StringProperty mobID = new StringProperty(this, "mobId", "minecraft:chicken");
    public final StringProperty mobNBT = new StringProperty(this, "mobNbt", "");

    public final LinkProperty texture = new LinkProperty(this, "texture", null);
    public final BooleanProperty slim = new BooleanProperty(this, "slim", false);

    public final PoseProperty pose = new PoseProperty(this, "pose", new Pose());
    public final PoseProperty poseOverlay = new PoseProperty(this, "pose_overlay", new Pose());
    public final PoseProperty poseOverlay1 = new PoseProperty(this, "pose_overlay_1", new Pose());
    public final PoseProperty poseOverlay2 = new PoseProperty(this, "pose_overlay_2", new Pose());
    public final PoseProperty poseOverlay3 = new PoseProperty(this, "pose_overlay_3", new Pose());
    public final PoseProperty poseOverlay4 = new PoseProperty(this, "pose_overlay_4", new Pose());
    public final PoseProperty poseOverlay5 = new PoseProperty(this, "pose_overlay_5", new Pose());
    public final PoseProperty poseOverlay6 = new PoseProperty(this, "pose_overlay_6", new Pose());
    public final PoseProperty poseOverlay7 = new PoseProperty(this, "pose_overlay_7", new Pose());

    public MobForm()
    {
        this.slim.cantAnimate();

        this.register(this.mobID);
        this.register(this.mobNBT);
        this.register(this.pose);
        this.register(this.poseOverlay);
        this.register(this.poseOverlay1);
        this.register(this.poseOverlay2);
        this.register(this.poseOverlay3);
        this.register(this.poseOverlay4);
        this.register(this.poseOverlay5);
        this.register(this.poseOverlay6);
        this.register(this.poseOverlay7);
        this.register(this.texture);
        this.register(this.slim);
    }

    @Override
    protected String getDefaultDisplayName()
    {
        return this.mobID.get().isEmpty() ? super.getDefaultDisplayName() : this.mobID.get();
    }

    public boolean isPlayer()
    {
        return this.mobID.get().equals("minecraft:player");
    }
}