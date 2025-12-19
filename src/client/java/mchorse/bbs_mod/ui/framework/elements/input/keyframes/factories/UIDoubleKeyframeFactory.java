package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIDoubleKeyframeFactory extends UIKeyframeFactory<Double>
{
    private UITrackpad value;
    private UIBezierHandles handles;

    public UIDoubleKeyframeFactory(Keyframe<Double> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.value = new UITrackpad(this::setValue);
        this.value.setValue(keyframe.getValue());
        this.handles = new UIBezierHandles(keyframe);

        this.scroll.add(this.value, this.handles.createColumn());
    }

    @Override
    public void update()
    {
        super.update();

        this.value.setValue(this.keyframe.getValue());
        this.handles.update();
    }
}