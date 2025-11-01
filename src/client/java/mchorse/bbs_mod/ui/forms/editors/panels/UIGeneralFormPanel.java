package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIKeybind;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;

public class UIGeneralFormPanel extends UIFormPanel
{
    public UIKeybind hotkey;

    public UIToggle visible;
    public UIToggle animatable;
    public UITextbox trackName;
    public UIToggle lighting;
    public UIToggle shaderShadow;
    public UITrackpad uiScale;
    public UITextbox name;
    public UIPropTransform transform;

    public UIToggle hitbox;
    public UITrackpad hitboxWidth;
    public UITrackpad hitboxHeight;
    public UITrackpad hitboxSneakMultiplier;
    public UITrackpad hitboxEyeHeight;

    public UITrackpad hp;
    public UITrackpad speed;
    public UITrackpad stepHeight;

    public UIGeneralFormPanel(UIForm editor)
    {
        super(editor);

        this.hotkey = new UIKeybind((combo) ->
        {
            this.form.hotkey.set(combo.keys.isEmpty() ? 0 : combo.keys.get(0));
        });
        this.hotkey.single().tooltip(UIKeys.FORMS_EDITORS_GENERAL_HOTKEY);

        this.visible = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_VISIBLE, (b) -> this.form.visible.set(b.getValue()));
        this.animatable = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_ANIMATABLE, (b) -> this.form.animatable.set(b.getValue()));
        this.animatable.tooltip(UIKeys.FORMS_EDITORS_GENERAL_ANIMATABLE_TOOLTIP);
        this.trackName = new UITextbox(120, (t) -> this.form.trackName.set(t));
        this.trackName.tooltip(UIKeys.FORMS_EDITORS_GENERAL_TRACK_NAME_TOOLTIP);
        this.lighting = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING, (b) -> this.form.lighting.set(b.getValue() ? 1F : 0F));
        this.lighting.tooltip(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING_TOOLTIP);
        this.shaderShadow = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_SHADER_SHADOW, (b) -> this.form.shaderShadow.set(b.getValue()));
        this.uiScale = new UITrackpad((v) -> this.form.uiScale.set(v.floatValue()));
        this.uiScale.limit(0.01D, 100D);
        this.name = new UITextbox(120, (t) -> this.form.name.set(t));

        this.transform = new UIPropTransform().callbacks(() -> this.form.transform);
        this.transform.enableHotkeys().relative(this).x(0.5F).y(1F, -10).anchor(0.5F, 1F);

        this.hitbox = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_HITBOX, (b) -> this.form.hitbox.set(b.getValue()));
        this.hitboxWidth = new UITrackpad((v) -> this.form.hitboxWidth.set(v.floatValue()));
        this.hitboxWidth.limit(0).tooltip(UIKeys.FORMS_EDITORS_GENERAL_HITBOX_WIDTH);
        this.hitboxHeight = new UITrackpad((v) -> this.form.hitboxHeight.set(v.floatValue()));
        this.hitboxHeight.limit(0).tooltip(UIKeys.FORMS_EDITORS_GENERAL_HITBOX_HEIGHT);
        this.hitboxSneakMultiplier = new UITrackpad((v) -> this.form.hitboxSneakMultiplier.set(v.floatValue()));
        this.hitboxSneakMultiplier.limit(0, 1);
        this.hitboxEyeHeight = new UITrackpad((v) -> this.form.hitboxEyeHeight.set(v.floatValue()));
        this.hitboxEyeHeight.limit(0, 1);

        this.hp = new UITrackpad((v) -> this.form.hp.set(v.floatValue()));
        this.hp.limit(1F);
        this.speed = new UITrackpad((v) -> this.form.speed.set(v.floatValue()));
        this.speed.limit(0F);
        this.stepHeight = new UITrackpad((v) -> this.form.stepHeight.set(v.floatValue()));
        this.stepHeight.limit(0F);

        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_DISPLAY), this.name);
        this.options.add(this.hotkey, this.visible, this.animatable, this.trackName, this.lighting, this.shaderShadow);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_UI_SCALE), this.uiScale);
        this.options.add(this.transform.marginTop(8));
        this.options.add(this.hitbox.marginTop(12), UI.row(this.hitboxWidth, this.hitboxHeight));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_HITBOX_SNEAK_MULTIPLIER), this.hitboxSneakMultiplier);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_HITBOX_EYE_HEIGHT), this.hitboxEyeHeight);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_HP).marginTop(12), this.hp);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_MOVEMENT_SPEED), this.speed.tooltip(UIKeys.FORMS_EDITORS_GENERAL_MOVEMENT_SPEED_TOOLTIP));
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_GENERAL_STEP_HEIGHT), this.stepHeight);
    }

    @Override
    public void startEdit(Form form)
    {
        super.startEdit(form);

        this.hotkey.setKeyCombo(new KeyCombo(IKey.EMPTY, form.hotkey.get()));

        this.visible.setValue(form.visible.get());
        this.animatable.setValue(form.animatable.get());
        this.trackName.setText(form.trackName.get());
        this.lighting.setValue(form.lighting.get() > 0F);
        this.shaderShadow.setValue(form.shaderShadow.get());
        this.uiScale.setValue(form.uiScale.get());
        this.name.setText(form.name.get());
        this.transform.setTransform(form.transform.get());

        this.hitbox.setValue(form.hitbox.get());
        this.hitboxWidth.setValue(form.hitboxWidth.get());
        this.hitboxHeight.setValue(form.hitboxHeight.get());
        this.hitboxSneakMultiplier.setValue(form.hitboxSneakMultiplier.get());
        this.hitboxEyeHeight.setValue(form.hitboxEyeHeight.get());

        this.hp.setValue(form.hp.get());
        this.speed.setValue(form.speed.get());
        this.stepHeight.setValue(form.stepHeight.get());
    }
}