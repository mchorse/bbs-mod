package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.values.ValueAngle;
import mchorse.bbs_mod.camera.values.ValuePoint;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.ValueBoolean;
import mchorse.bbs_mod.settings.values.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

import java.util.List;

public class TrackerClip extends EntityClip {
    public final ValuePoint offsetAngle = new ValuePoint("offset_angle", new Point(0, 0, 0));
    public final ValueBoolean lookat = new ValueBoolean("lookat", false);
    public final ValueBoolean relative = new ValueBoolean("relative");
    public final ValueInt active = new ValueInt("active", 0, 0, 0b11111111);

    public TrackerClip()
    {
        super();

        this.add(this.offsetAngle);
        this.add(this.lookat);
        this.add(this.relative);
        this.add(this.active);
    }

    @Override
    protected void applyClip(ClipContext context, Position position) { }

    @Override
    protected Clip create() {
        return new TrackerClip();
    }
}
