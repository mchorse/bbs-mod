package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.SubtitleClip;
import mchorse.bbs_mod.settings.values.IValueNotifier;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;

public class UISubtitleClip extends UIClip<SubtitleClip>
{
    public UITrackpad x;
    public UITrackpad y;
    public UITrackpad size;
    public UITrackpad anchorX;
    public UITrackpad anchorY;
    public UIColor color;
    public UIToggle textShadow;
    public UITrackpad windowX;
    public UITrackpad windowY;
    public UIColor background;
    public UITrackpad backgroundOffset;
    public UITrackpad shadow;
    public UIToggle shadowOpaque;
    public UIPropTransform transform;
    public UITrackpad lineHeight;
    public UITrackpad maxWidth;

    public UISubtitleClip(SubtitleClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.x = new UITrackpad((v) -> this.clip.x.set(v.intValue()));
        this.x.integer();
        this.y = new UITrackpad((v) -> this.clip.y.set(v.intValue()));
        this.y.integer();

        this.size = new UITrackpad((v) -> this.editor.editMultiple(this.clip.size, (value) ->
        {
            value.set(v.floatValue());
        }));
        this.anchorX = new UITrackpad((v) -> this.editor.editMultiple(this.clip.anchorX, (value) ->
        {
            value.set(v.floatValue());
        }));
        this.anchorY = new UITrackpad((v) -> this.editor.editMultiple(this.clip.anchorY, (value) ->
        {
            value.set(v.floatValue());
        }));
        this.color = new UIColor((c) -> this.editor.editMultiple(this.clip.color, (value) ->
        {
            value.set(c);
        }));
        this.color.withAlpha();
        this.textShadow = new UIToggle(UIKeys.CAMERA_PANELS_SUBTITLE_TEXT_SHADOW, (b) -> this.editor.editMultiple(this.clip.textShadow, (value) ->
        {
            value.set(b.getValue());
        }));

        this.windowX = new UITrackpad((v) -> this.editor.editMultiple(this.clip.windowX, (value) ->
        {
            value.set(v.floatValue());
        }));
        this.windowY = new UITrackpad((v) -> this.editor.editMultiple(this.clip.windowY, (value) ->
        {
            value.set(v.floatValue());
        }));

        this.background = new UIColor((c) -> this.editor.editMultiple(this.clip.background, (value) ->
        {
            value.set(c);
        })).withAlpha();
        this.backgroundOffset = new UITrackpad((v) -> this.editor.editMultiple(this.clip.backgroundOffset, (value) ->
        {
            value.set(v.floatValue());
        }));
        this.shadow = new UITrackpad((v) -> this.editor.editMultiple(this.clip.shadow, (value) ->
        {
            value.set(v.floatValue());
        })).limit(0);
        this.shadowOpaque = new UIToggle(UIKeys.CAMERA_PANELS_SUBTITLE_OPAQUE, (b) -> this.editor.editMultiple(this.clip.shadowOpaque, (value) ->
        {
            value.set(b.getValue());
        }));

        this.transform = new UIPropTransform().callbacks(
            () -> this.editor.editMultiple(this.clip.transform, IValueNotifier::preNotify),
            () -> this.editor.editMultiple(this.clip.transform, (t) ->
            {
                t.set(this.transform.getTransform().copy());
                t.postNotify();
            })
        );

        this.lineHeight = new UITrackpad((v) -> this.editor.editMultiple(this.clip.lineHeight, (value) ->
        {
            value.set(v.intValue());
        }));
        this.lineHeight.limit(0).integer().tooltip(UIKeys.CAMERA_PANELS_SUBTITLE_LINE_HEIGHT, Direction.BOTTOM);
        this.maxWidth = new UITrackpad((v) -> this.editor.editMultiple(this.clip.maxWidth, (value) ->
        {
            value.set(v.intValue());
        }));
        this.maxWidth.limit(0).integer().tooltip(UIKeys.CAMERA_PANELS_SUBTITLE_MAX_WIDTH, Direction.BOTTOM);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_OFFSET), UI.row(this.x, this.y)).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_SIZE), this.size, this.color, this.textShadow).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_ANCHOR), UI.row(this.anchorX, this.anchorY)).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_WINDOW), UI.row(this.windowX, this.windowY)).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_BACKGROUND), this.background, this.backgroundOffset).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_SHADOW), this.shadow, this.shadowOpaque).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_TRANSFORM), this.transform).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_CONSTRAINT), UI.row(this.lineHeight, this.maxWidth)).marginTop(6));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.x.setValue(this.clip.x.get());
        this.y.setValue(this.clip.y.get());
        this.size.setValue(this.clip.size.get());
        this.anchorX.setValue(this.clip.anchorX.get());
        this.anchorY.setValue(this.clip.anchorY.get());
        this.color.setColor(this.clip.color.get());
        this.textShadow.setValue(this.clip.textShadow.get());
        this.windowX.setValue(this.clip.windowX.get());
        this.windowY.setValue(this.clip.windowY.get());
        this.background.setColor(this.clip.background.get());
        this.backgroundOffset.setValue(this.clip.backgroundOffset.get());
        this.shadow.setValue(this.clip.shadow.get());
        this.shadowOpaque.setValue(this.clip.shadowOpaque.get());
        this.transform.setTransform(this.clip.transform.get());
        this.lineHeight.setValue(this.clip.lineHeight.get());
        this.maxWidth.setValue(this.clip.maxWidth.get());
    }
}