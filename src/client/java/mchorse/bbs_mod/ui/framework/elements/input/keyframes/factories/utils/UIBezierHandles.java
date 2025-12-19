package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIBezierHandles
{
    private UITrackpad lx;
    private UITrackpad ly;
    private UITrackpad rx;
    private UITrackpad ry;

    private Keyframe<?> keyframe;

    public UIBezierHandles(Keyframe<?> keyframe)
    {
        this.keyframe = keyframe;

        this.lx = new UITrackpad((v) -> BaseValue.edit(this.keyframe, (kf) -> kf.lx = (float) TimeUtils.fromTime(v.floatValue())));
        this.ly = new UITrackpad((v) -> BaseValue.edit(this.keyframe, (kf) -> kf.ly = v.floatValue()));
        this.rx = new UITrackpad((v) -> BaseValue.edit(this.keyframe, (kf) -> kf.rx = (float) TimeUtils.fromTime(v.floatValue())));
        this.ry = new UITrackpad((v) -> BaseValue.edit(this.keyframe, (kf) -> kf.ry = v.floatValue()));
        this.lx.setValue(TimeUtils.toTime(this.keyframe.lx));
        this.ly.setValue(this.keyframe.ly);
        this.rx.setValue(TimeUtils.toTime(this.keyframe.rx));
        this.ry.setValue(this.keyframe.ry);
    }

    public UIElement createColumn()
    {
        return UI.column(
            UI.row(new UIIcon(Icons.LEFT_HANDLE, null).tooltip(UIKeys.KEYFRAMES_LEFT_HANDLE), this.lx, this.ly),
            UI.row(new UIIcon(Icons.RIGHT_HANDLE, null).tooltip(UIKeys.KEYFRAMES_RIGHT_HANDLE), this.rx, this.ry)
        );
    }

    public void update()
    {
        this.lx.setValue(TimeUtils.toTime(this.keyframe.lx));
        this.ly.setValue(this.keyframe.ly);
        this.rx.setValue(TimeUtils.toTime(this.keyframe.rx));
        this.ry.setValue(this.keyframe.ry);
    }
}