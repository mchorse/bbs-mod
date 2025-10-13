package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.values.ValuePoint;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

public class TrackerClip extends EntityClip
{
    public final ValuePoint angle = new ValuePoint("angle", new Point(0, 0, 0));
    public final ValueFloat fov = new ValueFloat("fov", 70F);
    public final ValueString group = new ValueString("group", "");
    public final ValueBoolean lookAt = new ValueBoolean("look_at", false);
    public final ValueBoolean relative = new ValueBoolean("relative");
    public final ValueInt active = new ValueInt("active", 0b1111111, 0, 0b1111111);

    public TrackerClip()
    {
        super();

        this.add(this.angle);
        this.add(this.fov);
        this.add(this.group);
        this.add(this.lookAt);
        this.add(this.relative);
        this.add(this.active);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new TrackerClip();
    }
}
