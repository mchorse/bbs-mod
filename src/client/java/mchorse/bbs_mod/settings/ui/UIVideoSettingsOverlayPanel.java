package mchorse.bbs_mod.settings.ui;

import mchorse.bbs_mod.settings.values.ui.ValueVideoSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIVideoSettingsOverlayPanel extends UIOverlayPanel
{
    private ValueVideoSettings value;

    private UIScrollView editor;
    private UITextbox arguments;
    private UITextbox argumentsAudio;
    private UIToggle audio;
    private UIIcon flip;
    private UITrackpad width;
    private UITrackpad height;
    private UITrackpad frameRate;
    private UITrackpad motionBlur;
    private UITrackpad heldFrames;
    private UITextbox path;

    public UIVideoSettingsOverlayPanel(ValueVideoSettings value)
    {
        super(UIKeys.VIDEO_SETTINGS_TITLE);

        this.value = value;

        this.arguments = new UITextbox(1024, (s) -> this.value.arguments.set(s));
        this.argumentsAudio = new UITextbox(1024, (s) -> this.value.argumentsAudio.set(s));
        this.audio = new UIToggle(UIKeys.VIDEO_SETTINGS_AUDIO, (b) -> this.value.audio.set(b.getValue()));
        this.audio.tooltip(UIKeys.VIDEO_SETTINGS_AUDIO_TOOLTIP);
        this.flip = new UIIcon(Icons.REFRESH, (b) ->
        {
            int w = this.value.width.get();
            int h = this.value.height.get();

            this.value.width.set(h);
            this.value.height.set(w);

            this.fill();
        });
        this.width = new UITrackpad((v) -> this.value.width.set(v.intValue()));
        this.width.limit(2, 8096, true);
        this.width.tooltip(UIKeys.VIDEO_SETTINGS_WIDTH);
        this.height = new UITrackpad((v) -> this.value.height.set(v.intValue()));
        this.height.limit(2, 8096, true);
        this.height.tooltip(UIKeys.VIDEO_SETTINGS_HEIGHT);
        this.frameRate = new UITrackpad((v) -> this.value.frameRate.set(v.intValue()));
        this.frameRate.limit(10, 1000, true);
        this.motionBlur = new UITrackpad((v) -> this.value.motionBlur.set(v.intValue()));
        this.motionBlur.limit(this.value.motionBlur.getMin(), this.value.motionBlur.getMax(), true);
        this.motionBlur.tooltip(UIKeys.VIDEO_SETTINGS_MOTION_BLUR_TOOLTIP);
        this.heldFrames = new UITrackpad((v) -> this.value.heldFrames.set(v.intValue()));
        this.heldFrames.limit(this.value.heldFrames.getMin(), this.value.heldFrames.getMax(), true);
        this.heldFrames.tooltip(UIKeys.VIDEO_SETTINGS_HELD_FRAMES_TOOLTIP);
        this.path = new UITextbox(1024, (s) -> this.value.path.set(s));

        this.editor = UI.scrollView(5, 6,
            UI.label(UIKeys.VIDEO_SETTINGS_ARGS),
            this.arguments,
            UI.label(UIKeys.VIDEO_SETTINGS_AUDIO_ARGS),
            this.argumentsAudio, this.audio,
            UI.label(UIKeys.VIDEO_SETTINGS_RESOLUTION).marginTop(6),
            UI.row(this.width, this.flip, this.height),
            UI.label(UIKeys.VIDEO_SETTINGS_FRAME_RATE).marginTop(6),
            this.frameRate,
            UI.label(UIKeys.VIDEO_SETTINGS_MOTION_BLUR).marginTop(6),
            this.motionBlur,
            UI.label(UIKeys.VIDEO_SETTINGS_HELD_FRAMES).marginTop(6),
            this.heldFrames,
            UI.label(UIKeys.VIDEO_SETTINGS_PATH).marginTop(6),
            this.path
        );

        this.content.add(this.editor.full(this.content));

        UIIcon icon = new UIIcon(Icons.FILM, (b) ->
        {
            this.getContext().replaceContextMenu((menu) ->
            {
                menu.action(Icons.FILM, UIKeys.VIDEO_SETTINGS_PRESETS_720p, () -> this.setPreset(1280, 720));
                menu.action(Icons.FILM, UIKeys.VIDEO_SETTINGS_PRESETS_1080P, () -> this.setPreset(1920, 1080));
                menu.action(Icons.FILM, UIKeys.VIDEO_SETTINGS_PRESETS_SHORTS_1080P, () -> this.setPreset(1080, 1920));
                menu.action(Icons.FILM, UIKeys.VIDEO_SETTINGS_PRESETS_1440P, () -> this.setPreset(2560, 1440));
                menu.action(Icons.FILM, UIKeys.VIDEO_SETTINGS_PRESETS_4K, () -> this.setPreset(3840, 2160));
            });
        });

        this.icons.add(icon);

        this.fill();
    }

    private void setPreset(int w, int h)
    {
        this.value.arguments.set(ValueVideoSettings.DEFAULT_FFMPEG_ARGUMENTS);
        this.value.width.set(w);
        this.value.height.set(h);
        this.value.frameRate.set(60);
        this.fill();
    }

    private void fill()
    {
        this.arguments.setText(this.value.arguments.get());
        this.argumentsAudio.setText(this.value.argumentsAudio.get());
        this.audio.setValue(this.value.audio.get());
        this.width.setValue(this.value.width.get());
        this.height.setValue(this.value.height.get());
        this.frameRate.setValue(this.value.frameRate.get());
        this.motionBlur.setValue(this.value.motionBlur.get());
        this.heldFrames.setValue(this.value.heldFrames.get());
        this.path.setText(this.value.path.get());
    }
}