package mchorse.bbs_mod.ui.framework.elements.context;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.color.UIColorPicker;
import mchorse.bbs_mod.utils.colors.Color;

public class UIColorContextMenu extends UIContextMenu {

    public UIColorPicker colorPicker;
    private Runnable callback;

    private Color color;

    private final int WIDTH = 300;
    private final int HEIGHT = 150 ;

    public UIColorContextMenu(Color color)
    {
        this.color = color;

        this.colorPicker = new UIColorPicker(integer ->
                callback.run());
        this.colorPicker.setColor(color.getRGBAColor());
        this.colorPicker.relative(this).xy(0, 0).wh(WIDTH, HEIGHT);

        this.add(colorPicker);

        this.wh(WIDTH, HEIGHT);
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public void setMouse(UIContext context)
    {
        this.xy(context.mouseX(), context.mouseY()).bounds(context.menu.overlay, 5);
    }


    public UIColorContextMenu callback(Runnable callback)
    {
        this.callback = callback;
        return this;
    }
}
