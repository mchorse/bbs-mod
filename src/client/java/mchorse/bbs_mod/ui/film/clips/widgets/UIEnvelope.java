package mchorse.bbs_mod.ui.film.clips.widgets;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.context.UIInterpolationContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.tooltips.InterpolationTooltip;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.TimeUtilsClient;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.clips.Envelope;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIEnvelope extends UIElement
{
    public UIClip<? extends Clip> panel;

    public UIToggle enabled;
    public UICirculate mode;
    public UIButton pre;
    public UIButton post;
    public UITrackpad fadeIn;
    public UITrackpad fadeOut;

    public UIToggle keyframes;
    public UIButton editKeyframes;
    public UIKeyframeEditor channel;

    public UIEnvelope(UIClip<? extends Clip> panel)
    {
        super();

        this.panel = panel;

        InterpolationTooltip preTooltip = new InterpolationTooltip(0F, 0.5F, () -> this.get().pre);
        InterpolationTooltip postTooltip = new InterpolationTooltip(0F, 0.5F, () -> this.get().post);

        this.enabled = new UIToggle(UIKeys.CAMERA_PANELS_ENABLED, (b) ->
        {
            this.panel.editor.editMultiple(this.get().enabled, (value) -> value.set(b.getValue()));
        });
        this.mode = new UICirculate((b) ->
        {
            this.panel.editor.editMultiple(this.get().mode, (value) -> value.set(b.getValue()));
        });
        this.mode.addLabel(UIKeys.CAMERA_PANELS_ENVELOPES_MODES_NORMAL);
        this.mode.addLabel(UIKeys.CAMERA_PANELS_ENVELOPES_START_D);
        this.mode.addLabel(UIKeys.CAMERA_PANELS_ENVELOPES_END_D);
        this.pre = new UIButton(UIKeys.CAMERA_PANELS_ENVELOPES_PRE, (b) ->
        {
            this.getContext().replaceContextMenu(new UIInterpolationContextMenu(this.get().pre));
        });
        this.pre.tooltip(preTooltip);
        this.post = new UIButton(UIKeys.CAMERA_PANELS_ENVELOPES_POST, (b) ->
        {
            this.getContext().replaceContextMenu(new UIInterpolationContextMenu(this.get().post));
        });
        this.post.tooltip(postTooltip);

        this.fadeIn = new UITrackpad((v) ->
        {
            this.panel.editor.editMultiple(this.get().fadeIn, (value) -> value.set((float) TimeUtils.fromTime(v.floatValue())));
        });
        this.fadeIn.tooltip(UIKeys.CAMERA_PANELS_ENVELOPES_START_D, Direction.TOP);
        this.fadeOut = new UITrackpad((v) ->
        {
            this.panel.editor.editMultiple(this.get().fadeOut, (value) -> value.set((float) TimeUtils.fromTime(v.floatValue())));
        });
        this.fadeOut.tooltip(UIKeys.CAMERA_PANELS_ENVELOPES_END_D, Direction.TOP);

        this.keyframes = new UIToggle(UIKeys.CAMERA_PANELS_KEYFRAMES, (b) ->
        {
            this.panel.editor.editMultiple(this.get().keyframes, (value) -> value.set(b.getValue()));
            this.toggleKeyframes(b.getValue());
        });
        this.editKeyframes = new UIButton(UIKeys.CAMERA_PANELS_EDIT_KEYFRAMES, (b) ->
        {
            this.panel.editor.embedView(this.channel);
            this.channel.view.resetView();
            this.channel.view.editSheet(this.channel.view.getGraph().getSheets().get(0));
        });
        this.channel = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.panel.editor, consumer));
        this.channel.view.backgroundRenderer((context) ->
        {
            UIReplaysEditor.renderBackground(context, this.channel.view, (Clips) this.panel.clip.getParent(), this.panel.clip.tick.get());
        });
        this.channel.view.duration(() -> this.panel.clip.duration.get());

        this.column().vertical().stretch();
    }

    private void toggleKeyframes(boolean toggled)
    {
        this.removeAll();

        this.add(this.enabled, this.mode);

        if (toggled)
        {
            this.add(this.editKeyframes);
        }
        else
        {
            this.add(UI.row(this.pre, this.post), UI.row(this.fadeIn, this.fadeOut));
        }

        this.add(this.keyframes);

        if (this.hasParent())
        {
            this.getParent().getParent().resize();

            if (toggled)
            {
                this.initiate();
            }
        }
    }

    public void initiate()
    {
        this.channel.view.resetView();
        this.channel.updateConverter();

        TimeUtilsClient.configure(this.fadeIn, 0);
        TimeUtilsClient.configure(this.fadeOut, 0);

        this.fillIntervals();
    }

    public void fillData()
    {
        Envelope envelope = this.get();

        this.enabled.setValue(envelope.enabled.get());
        this.mode.setValue(envelope.mode.get());
        this.fillIntervals();
        this.keyframes.setValue(envelope.keyframes.get());
        this.channel.setChannel(envelope.channel, Colors.ACTIVE);

        this.toggleKeyframes(envelope.keyframes.get());
    }

    private void fillIntervals()
    {
        Envelope envelope = this.get();

        this.fadeIn.setValue(TimeUtils.toTime(envelope.fadeIn.get().intValue()));
        this.fadeOut.setValue(TimeUtils.toTime(envelope.fadeOut.get().intValue()));
    }

    public Envelope get()
    {
        return this.panel.clip.envelope;
    }
}