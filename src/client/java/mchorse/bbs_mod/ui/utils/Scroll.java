package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.client.MinecraftClient;

/**
 * Scroll
 * 
 * This class is responsible for storing information for scrollable one 
 * directional objects. 
 */
public class Scroll
{
    private static Area temporary = new Area();

    /**
     * Size of an element/item in the scroll area
     */
    public int scrollItemSize;

    /**
     * Size of the scrolling area 
     */
    public int scrollSize;

    /**
     * Scroll position 
     */
    private double scroll;

    /**
     * Whether this scroll area gets dragged 
     */
    public boolean dragging;

    /**
     * Speed of how fast shit's scrolling  
     */
    public int scrollSpeed = 10;

    /**
     * Scroll direction
     */
    public ScrollDirection direction = ScrollDirection.VERTICAL;

    /**
     * Whether the scrollbar should be on opposite side (default is right
     * for vertical and bottom for horizontal)
     */
    public boolean opposite;

    /**
     * Whether this scroll area should cancel mouse events when mouse scroll
     * reaches the end
     */
    public boolean cancelScrollEdge = false;

    /**
     * Whether the scrollbar should be rendered and handled by input methods
     */
    public boolean scrollbar = true;

    public final Area area;

    private float scrollbarRatio;
    private double targetScroll;

    public static void bar(Batcher2D batcher, int x1, int y1, int x2, int y2, int color)
    {
        if (x2 - x1 == 0 || y2 - y1 == 0)
        {
            return;
        }

        batcher.dropShadow(x1, y1, x2, y2, 5, color, Colors.setA(color, 0F));

        batcher.box(x1, y1, x2, y2, 0xffeeeeee);
        batcher.box(x1 + 1, y1 + 1, x2, y2, 0xff666666);
        batcher.box(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xffaaaaaa);

        int dx = x2 - x1;
        int dy = y2 - y1;

        if (dx + dy < 30)
        {
            return;
        }

        int x = (x2 + x1) / 2;
        int y = (y2 + y1) / 2;

        /* Little handle */
        if (dx > dy)
        {
            batcher.box(x - 3, y - 1, x - 2, y + 1, Colors.GRAY);
            batcher.box(x, y - 1, x + 1, y + 1, Colors.GRAY);
            batcher.box(x + 3, y - 1, x + 4, y + 1, Colors.GRAY);
        }
        else
        {
            batcher.box(x - 1, y - 3, x + 1, y - 2, Colors.GRAY);
            batcher.box(x - 1, y, x + 1, y + 1, Colors.GRAY);
            batcher.box(x - 1, y + 3, x + 1, y + 4, Colors.GRAY);
        }
    }

    public Scroll(Area area)
    {
        this.area = area;
        this.area.scroll = this;
    }

    public Scroll(Area area, int itemSize)
    {
        this(area);

        this.scrollItemSize = itemSize;
    }

    public Scroll(Area area, int itemSize, ScrollDirection direction)
    {
        this(area, itemSize);

        this.direction = direction;
    }

    public Scroll cancelScrolling()
    {
        this.cancelScrollEdge = true;

        return this;
    }

    public Scroll opposite()
    {
        this.opposite = true;

        return this;
    }

    public Scroll noScrollbar()
    {
        this.scrollbar = false;

        return this;
    }

    public int getScrollbarWidth()
    {
        return BBSSettings.scrollbarWidth.get();
    }

    public void setSize(int items)
    {
        this.scrollSize = items * this.scrollItemSize;
    }

    public double getScroll()
    {
        return this.scroll;
    }

    /**
     * Force set scroll
     */
    public void setScroll(double x)
    {
        this.scroll = this.targetScroll = x;

        this.clamp();
    }

    /**
     * Scroll by relative amount 
     */
    public void scrollBy(double x)
    {
        this.scrollTo(this.targetScroll + x);
    }

    /**
     * Scroll to the position in the scroll area 
     */
    public void scrollTo(double x)
    {
        this.targetScroll = x;

        this.clamp();
    }

    public void scrollToEnd()
    {
        this.setScroll(Integer.MAX_VALUE);
    }

    public void scrollIntoView(int x)
    {
        this.scrollIntoView(x, this.scrollItemSize, 0);
    }

    public void scrollIntoView(int x, int bottomOffset)
    {
        this.scrollIntoView(x, bottomOffset, 0);
    }

    public void scrollIntoView(int x, int bottomOffset, int topOffset)
    {
        if (this.scroll + topOffset > x)
        {
            this.scrollTo(x - topOffset);
        }
        else if (x > this.scroll + this.direction.getSide(this.area) - bottomOffset)
        {
            this.scrollTo(x - this.direction.getSide(this.area) + bottomOffset);
        }
    }

    /**
     * Clamp scroll to the bounds of the scroll size; 
     */
    public void clamp()
    {
        int size = this.direction.getSide(this.area);

        if (this.scrollSize <= size)
        {
            this.scroll = this.targetScroll = 0;
        }
        else
        {
            this.scroll = MathUtils.clamp(this.scroll, 0, this.scrollSize - size);
            this.targetScroll = MathUtils.clamp(this.targetScroll, 0, this.scrollSize - size);
        }
    }

    public void updateTarget()
    {
        this.scroll = this.targetScroll;
    }

    public void copy(Scroll scroll)
    {
        this.scroll = scroll.scroll;
        this.targetScroll = scroll.targetScroll;
        this.scrollSize = scroll.scrollSize;
    }

    /**
     * Get index of the cursor based on the {@link #scrollItemSize}.  
     */
    public int getIndex(int x, int y)
    {
        int axis = this.direction.getScroll(this.area, this, x, y);
        int index = axis / this.scrollItemSize;

        if (axis < 0)
        {
            return -1;
        }
        else if (axis > this.scrollSize)
        {
            return -2;
        }

        return index > this.scrollSize / this.scrollItemSize ? -1 : index;
    }

    public boolean hasScrollbar()
    {
        return this.scrollSize > this.direction.getSide(this.area);
    }

    /**
     * Calculates scroll bar's height 
     */
    public int getScrollbar()
    {
        int maxSize = this.direction.getSide(this.area);

        if (this.scrollSize < maxSize)
        {
            return 0;
        }

        float finalSize = (1F - ((this.scrollSize - maxSize) / (float) this.scrollSize)) * maxSize;

        return Math.max(4, (int) finalSize);
    }

    public Area getScrollArea()
    {
        int maxSize = this.direction.getSide(this.area);

        if (this.scrollSize < maxSize)
        {
            temporary.set(0, 0, 0, 0);
        }
        else
        {
            int width = this.getScrollbarWidth();

            if (this.direction == ScrollDirection.VERTICAL)
            {
                temporary.set(this.area.x + (this.opposite ? 0 : this.area.w - width), this.area.y, width, this.area.h);
            }
            else
            {
                temporary.set(this.area.x, this.area.y + (this.opposite ? 0 : this.area.h - width), this.area.w, width);
            }
        }

        return temporary;
    }

    public Area getScrollbarArea()
    {
        int maxSize = this.direction.getSide(this.area);

        if (this.scrollSize < maxSize)
        {
            temporary.set(0, 0, 0, 0);
        }
        else
        {
            int scrollbar = this.getScrollbarWidth();
            int h = this.getScrollbar();

            if (this.direction == ScrollDirection.HORIZONTAL)
            {
                int y = this.opposite ? this.area.y : this.area.ey() - scrollbar;
                int x = this.area.x + (int) ((this.scroll / (float) (this.scrollSize - this.area.w)) * (this.area.w - h));

                temporary.set(x, y, h, scrollbar);
            }
            else
            {
                int x = this.opposite ? this.area.x : this.area.ex() - scrollbar;
                int y = this.area.y + (int) ((this.scroll / (float) (this.scrollSize - this.area.h)) * (this.area.h - h));

                temporary.set(x, y, scrollbar, h);
            }
        }

        return temporary;
    }

    /* GUI code for easier manipulations */

    public boolean mouseClicked(UIContext context)
    {
        return context.mouseButton == 0 && this.mouseClicked(context.mouseX, context.mouseY);
    }

    /**
     * This method should be invoked to register dragging 
     */
    public boolean mouseClicked(int x, int y)
    {
        if (!this.scrollbar)
        {
            return false;
        }

        boolean isInside = this.hasScrollbar() && this.getScrollArea().isInside(x, y);

        if (isInside)
        {
            this.dragging = true;

            Area area = this.getScrollbarArea();

            if (area.isInside(x, y))
            {
                this.scrollbarRatio = (this.direction.getMouse(x, y) - this.direction.getPosition(area, 0F)) / (float) this.direction.getSide(area);
            }
            else
            {
                this.scrollbarRatio = 0.5F;
            }
        }

        return isInside;
    }

    public boolean mouseScroll(UIContext context)
    {
        boolean canceled = this.mouseScroll(context.mouseX, context.mouseY, context.mouseWheel);

        if (canceled)
        {
            context.markUpdateScroll();
        }

        return canceled;
    }

    /**
     * This method should be invoked when mouse wheel is scrolling 
     */
    public boolean mouseScroll(int x, int y, double scroll)
    {
        scroll = -scroll;

        boolean isInside = this.area.isInside(x, y);
        double lastScroll = this.targetScroll;

        if (isInside)
        {
            if (MinecraftClient.IS_SYSTEM_MAC)
            {
                this.scrollBy(scroll * BBSSettings.scrollingSensitivity.get());
            }
            else if (scroll != 0D)
            {
                this.scrollBy((int) (Math.copySign(this.scrollSpeed, scroll) * BBSSettings.scrollingSensitivity.get()));
            }
        }

        return isInside && ((this.cancelScrollEdge && this.scrollSize > this.direction.getSide(this.area)) || lastScroll != this.targetScroll);
    }

    public void mouseReleased(UIContext context)
    {
        this.mouseReleased(context.mouseX, context.mouseY);
    }

    /**
     * When mouse button gets released
     */
    public void mouseReleased(int x, int y)
    {
        this.dragging = false;
    }

    public void drag(UIContext context)
    {
        this.drag(context.mouseX, context.mouseY);
    }

    /**
     * This should be invoked in a rendering or and update method. It's
     * responsible for scrolling through this view when dragging. 
     */
    public void drag(int x, int y)
    {
        if (BBSSettings.scrollingSmoothness.get())
        {
            float delta = MinecraftClient.getInstance().getLastFrameDuration();

            /* The higher the FPS, the smaller the lerp factor is,
             * the lower the FPS, the bigger the factor is */
            this.scroll = Lerps.lerp(this.scroll, this.targetScroll, Math.min(1F, delta / 2.5F));
        }
        else
        {
            this.scroll = this.targetScroll;
        }

        if (this.dragging)
        {
            int scrollbar = this.getScrollbar();
            int h = this.direction.getSide(this.area) - scrollbar;
            float progress = (this.direction.getMouse(x, y) - (this.direction.getPosition(this.area, 0F) + scrollbar * this.scrollbarRatio)) / (float) h;
            float to = progress * (this.scrollSize - this.direction.getSide(this.area));

            this.scrollTo(to);
        }
    }

    /**
     * This method is responsible for render a scroll bar
     */
    public void renderScrollbar(Batcher2D batcher)
    {
        if (!this.hasScrollbar())
        {
            return;
        }

        int side = this.direction.getSide(this.area);
        int shadow = Colors.mulRGB(Colors.A50 | BBSSettings.primaryColor.get(), 0.75F);

        if (this.scrollbar)
        {
            Area scrollbar = this.getScrollbarArea();
            int color = BBSSettings.scrollbarShadow.get();

            bar(batcher, scrollbar.x, scrollbar.y, scrollbar.ex(), scrollbar.ey(), color);
        }
        else if (this.direction == ScrollDirection.VERTICAL)
        {
            if (this.scroll > 0)
            {
                batcher.gradientVBox(this.area.x, this.area.y, this.area.ex(), this.area.y + 20, shadow, 0);
            }

            if (this.scroll < this.scrollSize - side)
            {
                batcher.gradientVBox(this.area.x, this.area.ey() - 20, this.area.ex(), this.area.ey(), 0, shadow);
            }
        }
        else if (this.direction == ScrollDirection.HORIZONTAL)
        {
            if (this.scroll > 0)
            {
                batcher.gradientHBox(this.area.x, this.area.y, this.area.x + 20, this.area.ey(), shadow, 0);
            }

            if (this.scroll < this.scrollSize - side)
            {
                batcher.gradientHBox(this.area.ex() - 20, this.area.y, this.area.ex(), this.area.ey(), 0, shadow);
            }
        }
    }
}