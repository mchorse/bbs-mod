package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

import java.util.List;

public class TrackerClip extends EntityClip {

    @Override
    protected void applyClip(ClipContext context, Position position) { }

    @Override
    protected Clip create() {
        return new TrackerClip();
    }
}
