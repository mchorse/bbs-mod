package mchorse.bbs_mod.ui.framework.elements.context;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.IUIKeyframeShape;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.UIKeyframeShapeFactory;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.KeyframeShape;

import java.util.Map;

public class UIShapeContextMenu extends UIContextMenu {

    public UIElement grid;
    private Runnable callback;

    private Color color;
    public KeyframeShape choosenShape;

    private final int WIDTH = 100;
    private final int HEIGHT = 50;
    private final int SPACING = 5;
    private final int ITEMS_PER_ROW = 5;

    public UIShapeContextMenu(KeyframeShape shape)
    {
        this.choosenShape = shape;

        this.grid = new UIElement();
        grid.grid(SPACING).items(ITEMS_PER_ROW);
        this.grid.relative(this).wh(WIDTH, HEIGHT);
        this.grid.margin(25);

        Map<KeyframeShape, IUIKeyframeShape> keyframes = UIKeyframeShapeFactory.getAllShapes();
        for(Map.Entry<KeyframeShape, IUIKeyframeShape> sh : keyframes.entrySet())
        {
            UIIcon icon = new UIIcon(sh.getValue().getIcon(), b -> {
                choosenShape = sh.getKey();
                if(callback != null) callback.run();
            });
            this.grid.add(icon);
        }

        this.add(this.grid);
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


    public UIShapeContextMenu callback(Runnable callback)
    {
        this.callback = callback;
        return this;
    }
}
