package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import org.joml.Vector2i;

public abstract class UICanvasEditor extends UICanvas
{
    private static Area processed = new Area();

    public UIElement editor;

    /* Width and height of the frame that being currently edited */
    protected int w;
    protected int h;

    public UICanvasEditor()
    {
        super();

        this.editor = new UIElement();
        this.editor.relative(this).xy(1F, 1F).w(130).anchor(1F, 1F).column().stretch().vertical().padding(10);
        this.add(this.editor);
    }

    public int getWidth()
    {
        return this.w;
    }

    public int getHeight()
    {
        return this.h;
    }

    public void setSize(int w, int h)
    {
        this.w = w;
        this.h = h;

        int x = -this.w / 2;
        int y = -this.h / 2;
        this.scaleX.set(0, 2);
        this.scaleY.set(0, 2);
        this.scaleX.viewOffset(x, x + w, 20);
        this.scaleY.viewOffset(y, y + h, 20);

        double min = Math.min(this.scaleX.getZoom(), this.scaleY.getZoom());

        this.scaleX.setZoom(min);
        this.scaleY.setZoom(min);
    }

    public Vector2i getHoverPixel(int x, int y)
    {
        return new Vector2i(
            (int) Math.floor(this.scaleX.from(x)) + this.w / 2,
            (int) Math.floor(this.scaleY.from(y)) + this.h / 2
        );
    }

    @Override
    protected void renderCanvas(UIContext context)
    {
        this.renderBackground(context);

        int x = -this.w / 2;
        int y = -this.h / 2;
        Area area = this.calculate(x, y, x + this.w, y + this.h);

        context.batcher.box(area.x - 1, area.y - 1, area.ex() + 1, area.ey() + 1, 0xff181818);

        if (!this.shouldDrawCanvas(context))
        {
            return;
        }

        context.batcher.clip(area, context);

        int ox = (this.area.x - area.x) % 16;
        int oy = (this.area.y - area.y) % 16;

        processed.copy(this.area);
        processed.offsetX(ox < 0 ? 16 + ox : ox);
        processed.offsetY(oy < 0 ? 16 + oy : oy);
        processed.clamp(area);
        this.renderCheckboard(context, area);
        this.renderCanvasFrame(context);

        context.batcher.unclip(context);

        this.renderForeground(context);
    }

    protected void renderCheckboard(UIContext context, Area processed)
    {
        context.batcher.iconArea(Icons.CHECKBOARD, processed.x, processed.y, processed.w, processed.h);
    }

    protected void renderBackground(UIContext context)
    {
        this.area.render(context.batcher, 0xff2f2f2f);
    }

    protected void renderForeground(UIContext context)
    {}

    protected abstract void renderCanvasFrame(UIContext context);

    protected boolean shouldDrawCanvas(UIContext context)
    {
        return true;
    }

    protected Area calculate(int ix1, int iy1, int ix2, int iy2)
    {
        int x1 = this.toX(ix1);
        int y1 = this.toY(iy1);
        int x2 = this.toX(ix2);
        int y2 = this.toY(iy2);

        int x = x1;
        int y = y1;
        int fw = x2 - x;
        int fh = y2 - y;

        Area.SHARED.set(x, y, fw, fh);

        return Area.SHARED;
    }
}
