package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.modifiers.LookClip;
import mchorse.bbs_mod.camera.clips.modifiers.TrackerClip;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.modules.UIAngleModule;
import mchorse.bbs_mod.ui.film.clips.modules.UIPointModule;
import mchorse.bbs_mod.ui.film.clips.widgets.UIBitToggle;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;

public class UITrackerClip extends UIClip<TrackerClip> {
    public UIButton selector;
    public UIPointModule offset;
    public UIPointModule offsetAngle;
    public UIToggle lookat;
    public UIToggle relative;
    public UIBitToggle active;

    public UITrackerClip(TrackerClip clip, IUIClipsDelegate editor) {
        super(clip, editor);
    }

    public void registerUI() {
        super.registerUI();

        this.selector = new UIButton(UIKeys.CAMERA_PANELS_TARGET_TITLE, (b) ->
        {
            UIFilmPanel panel = this.getParent(UIFilmPanel.class);

            if (panel != null)
            {
                UIAnchorKeyframeFactory.displayActors(this.getContext(), panel.getController().entities, this.clip.selector.get(), (i) -> this.clip.selector.set(i));
            }
        });
        this.selector.tooltip(UIKeys.CAMERA_PANELS_TARGET_TOOLTIP);

        this.offset = new UIPointModule(editor, UIKeys.CAMERA_PANELS_OFFSET).contextMenu();
        this.offsetAngle = new UIPointModule(editor, UIKeys.CAMERA_PANELS_OFFSET).contextMenu();
        this.lookat = new UIToggle(IKey.raw("Lookat"), b -> this.clip.lookat.set(b.getValue()));
        this.relative = new UIToggle(IKey.raw("Relative"), b -> this.clip.relative.set(b.getValue()));
        this.active = new UIBitToggle((value) -> this.clip.active.set(value)).all();
        this.active.bits.remove(this.active.bits.size() - 1);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UIClip.label(UIKeys.CAMERA_PANELS_TARGET).marginTop(12), this.selector);
        this.panels.add(this.offset.marginTop(6));
        this.panels.add(this.offsetAngle.marginTop(6));
        this.panels.add(this.lookat.marginTop(6));
        this.panels.add(this.relative.marginTop(6));
        this.panels.add(this.active.margin(6));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.offset.fill(this.clip.offset);
        this.offsetAngle.fill(this.clip.offsetAngle);
        this.lookat.setValue(this.clip.lookat.get());
        this.relative.setValue(this.clip.relative.get());
        this.active.setValue(this.clip.active.get());
    }
}
