package mchorse.bbs_mod.ui.film.clips.actions;

import mchorse.bbs_mod.actions.types.item.ItemDropActionClip;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;

public class UIItemDropActionClip extends UIActionClip<ItemDropActionClip>
{
    public UITrackpad posX;
    public UITrackpad posY;
    public UITrackpad posZ;
    public UITrackpad velocityX;
    public UITrackpad velocityY;
    public UITrackpad velocityZ;
    public UIToggle relative;
    public UIItemStack itemStack;

    public UIItemDropActionClip(ItemDropActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.posX = new UITrackpad((v) -> this.editor.editMultiple(this.clip.posX, (posX) -> posX.set(v)));
        this.posY = new UITrackpad((v) -> this.editor.editMultiple(this.clip.posY, (posY) -> posY.set(v)));
        this.posZ = new UITrackpad((v) -> this.editor.editMultiple(this.clip.posZ, (posZ) -> posZ.set(v)));
        this.velocityX = new UITrackpad((v) -> this.editor.editMultiple(this.clip.velocityX, (velocityX) -> velocityX.set(v.floatValue())));
        this.velocityY = new UITrackpad((v) -> this.editor.editMultiple(this.clip.velocityY, (velocityY) -> velocityY.set(v.floatValue())));
        this.velocityZ = new UITrackpad((v) -> this.editor.editMultiple(this.clip.velocityZ, (velocityZ) -> velocityZ.set(v.floatValue())));
        this.relative = new UIToggle(UIKeys.CAMERA_PANELS_RELATIVE, (v) -> this.editor.editMultiple(this.clip.relative, (relative) -> relative.set(v.getValue())));
        this.itemStack = new UIItemStack((stack) -> this.editor.editMultiple(this.clip.itemStack, (itemStack) -> itemStack.set(stack)));
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.label(UIKeys.ACTIONS_ITEM_POSITION).marginTop(12));
        this.panels.add(UI.row(this.posX, this.posY, this.posZ));
        this.panels.add(UI.row(this.relative));
        this.panels.add(UI.label(UIKeys.ACTIONS_ITEM_VELOCITY).marginTop(12));
        this.panels.add(UI.row(this.velocityX, this.velocityY, this.velocityZ));
        this.panels.add(UI.label(UIKeys.ACTIONS_ITEM_STACK).marginTop(12), this.itemStack);
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.posX.setValue(this.clip.posX.get());
        this.posY.setValue(this.clip.posY.get());
        this.posZ.setValue(this.clip.posZ.get());
        this.velocityX.setValue(this.clip.velocityX.get());
        this.velocityY.setValue(this.clip.velocityY.get());
        this.velocityZ.setValue(this.clip.velocityZ.get());
        this.relative.setValue(this.clip.relative.get());
        this.itemStack.setStack(this.clip.itemStack.get());
    }
}