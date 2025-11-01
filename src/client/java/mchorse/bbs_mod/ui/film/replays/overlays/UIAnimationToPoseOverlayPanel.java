package mchorse.bbs_mod.ui.film.replays.overlays;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;

public class UIAnimationToPoseOverlayPanel extends UIOverlayPanel
{
    public UIStringList list;
    public UIToggle onlyKeyframes;
    public UITrackpad length;
    public UITrackpad step;
    public UIButton generate;

    private final IUIAnimationPoseCallback callback;
    private final ModelForm modelForm;

    public UIAnimationToPoseOverlayPanel(IUIAnimationPoseCallback callback, ModelForm modelForm, UIKeyframeSheet sheet)
    {
        super(UIKeys.FILM_REPLAY_ANIMATION_TO_POSE_TITLE);

        this.callback = callback;
        this.modelForm = modelForm;

        ModelInstance model = ModelFormRenderer.getModel(modelForm);

        this.list = new UIStringList((l) -> this.pickAnimation(l.get(0)));
        this.list.h(UIStringList.DEFAULT_HEIGHT * 6);
        this.list.background();
        this.list.add(model.animations.animations.keySet());
        this.list.sort();
        this.list.setIndex(0);

        this.onlyKeyframes = new UIToggle(UIKeys.FILM_REPLAY_ANIMATION_TO_POSE_ONLY_KEYFRAMES, (b) -> {});
        this.onlyKeyframes.tooltip(UIKeys.FILM_REPLAY_ANIMATION_TO_POSE_ONLY_KEYFRAMES_TOOLTIP);
        this.onlyKeyframes.setValue(true);
        this.length = new UITrackpad();
        this.length.integer();
        this.length.tooltip(UIKeys.FILM_REPLAY_ANIMATION_TO_POSE_LENGTH);
        this.step = new UITrackpad();
        this.step.integer().setValue(1D);
        this.step.tooltip(UIKeys.FILM_REPLAY_ANIMATION_TO_POSE_STEP);
        this.generate = new UIButton(UIKeys.FILM_REPLAY_ANIMATION_TO_POSE_GENERATE, (b) ->
        {
            this.callback.animationToPoseKeyframes(
                this.list.getCurrentFirst(),
                this.onlyKeyframes.getValue(),
                (int) this.length.getValue(),
                (int) this.step.getValue()
            );

            this.close();
        });

        UIScrollView scroll = UI.scrollView(5, 6, this.list, this.onlyKeyframes, UI.row(this.length, this.step), this.generate);

        scroll.full(this.content);
        this.content.add(scroll);
    }

    private void pickAnimation(String s)
    {
        ModelInstance model = ModelFormRenderer.getModel(this.modelForm);
        Animation animation = model.animations.get(s);

        this.length.setValue(animation.getLengthInTicks());
    }

    public static interface IUIAnimationPoseCallback
    {
        public void animationToPoseKeyframes(String animationKey, boolean onlyKeyframes, int length, int step);
    }
}