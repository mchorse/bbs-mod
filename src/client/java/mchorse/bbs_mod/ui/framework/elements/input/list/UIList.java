package mchorse.bbs_mod.ui.framework.elements.input.list;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.ui.utils.keys.KeyCodes;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Abstract GUI list element
 * 
 * This element allows managing scrolling vertical lists much easier
 */
public abstract class UIList <T> extends UIElement
{
    /**
     * List of elements 
     */
    protected List<T> list = new ArrayList<>();

    /**
     * List for copying
     */
    private List<T> copy = new ArrayList<>();

    /**
     * Scrolling area
     */
    public Scroll scroll;

    /**
     * Callback which gets invoked when user selects an element
     */
    public Consumer<List<T>> callback;

    /**
     * Callback which gets invoked when user dropped a list element (when sorting option is enabled)
     */
    public Consumer<UIList<T>> beforeDropCallback;

    /**
     * Callback which gets invoked when user dropped a list element (when sorting option is enabled)
     */
    public Consumer<UIList<T>> afterDropCallback;

    /**
     * Selected elements
     */
    public List<Integer> current = new ArrayList<>();

    /**
     * Whether this list supports multi selection
     */
    public boolean multi;

    /**
     * Whether this list supports reordering
     */
    public boolean sorting;

    public int background;

    private String filter = "";
    private List<Pair<T, Integer>> filtered = new ArrayList<>();

    private int dragging = -1;
    private long dragTime;

    public UIList(Consumer<List<T>> callback)
    {
        super();

        this.callback = callback;
        this.scroll = new Scroll(this.area, 20);
    }

    /* List element settings */

    public UIList<T> background()
    {
        return this.background(Colors.A50);
    }

    public UIList<T> background(int color)
    {
        this.background = color;

        return this;
    }

    public UIList<T> multi()
    {
        this.multi = true;

        return this;
    }

    public UIList<T> sorting()
    {
        this.sorting = true;

        return this;
    }

    public UIList<T> beforeDrop(Consumer<UIList<T>> beforeDropCallback)
    {
        this.beforeDropCallback = beforeDropCallback;

        return this;
    }

    public UIList<T> afterDrop(Consumer<UIList<T>> afterDropCallback)
    {
        this.afterDropCallback = afterDropCallback;

        return this;
    }

    public UIList<T> cancelScrollEdge()
    {
        this.scroll.cancelScrollEdge = true;

        return this;
    }

    /* Filtering elements */

    public void filter(String filter)
    {
        filter = filter.toLowerCase();

        if (this.filter.equals(filter))
        {
            return;
        }

        this.filter = filter;
        this.filtered.clear();

        if (filter.isEmpty())
        {
            this.update();

            return;
        }

        String qwerty = KeyCodes.cyrillicToQwerty(filter);

        for (int i = 0; i < this.list.size(); i ++)
        {
            T element = this.list.get(i);
            String target = this.elementToString(this.getContext(), i, element).toLowerCase();

            if (target.contains(filter) || target.contains(qwerty))
            {
                this.filtered.add(new Pair<>(element, i));
            }
        }

        this.update();
        this.scroll.updateTarget();
    }

    public boolean isFiltering()
    {
        return !this.filter.isEmpty();
    }

    /* Index and current value(s) methods */

    public boolean isSelected()
    {
        return !this.isDeselected();
    }

    public boolean isDeselected()
    {
        if (this.current.isEmpty())
        {
            return true;
        }

        for (Integer index : this.current)
        {
            if (this.exists(index))
            {
                return false;
            }
        }

        return true;
    }

    public List<Integer> getCurrentIndices()
    {
        return this.current;
    }

    public List<T> getCurrent()
    {
        this.copy.clear();

        for (Integer integer : this.current)
        {
            if (this.exists(integer))
            {
                this.copy.add(this.list.get(integer));
            }
        }

        return this.copy;
    }

    public T getCurrentFirst()
    {
        if (!this.current.isEmpty())
        {
            int index = this.current.get(0);

            if (this.exists(index))
            {
                return this.list.get(index);
            }
        }

        return null;
    }

    public int getIndex()
    {
        if (this.current.isEmpty())
        {
            return -1;
        }

        int index = this.current.get(0);

        return this.exists(index) ? index : -1;
    }

    public int getHoveredIndex(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return -1;
        }

        return (context.mouseY - this.area.y + (int) this.scroll.getScroll()) / this.scroll.scrollItemSize;
    }

    public void deselect()
    {
        this.setIndex(-1);
    }

    public void setIndex(int index)
    {
        this.current.clear();
        this.addIndex(index);
    }

    public void addIndex(int index)
    {
        if (this.exists(index) && !this.current.contains(index))
        {
            this.current.add(index);
        }
    }

    public void toggleIndex(int index)
    {
        if (this.exists(index))
        {
            int i = this.current.indexOf(index);

            if (i == -1)
            {
                this.current.add(index);
            }
            else
            {
                this.current.remove(i);
            }
        }
    }

    public void setCurrent(T element)
    {
        this.current.clear();

        int index = this.list.indexOf(element);

        if (this.exists(index))
        {
            this.current.add(index);
        }
    }

    public void setCurrentDirect(T element)
    {
        this.current.clear();

        for (int i = 0; i < this.list.size(); i ++)
        {
            if (this.list.get(i) == element)
            {
                this.current.add(i);

                return;
            }
        }
    }

    public void setCurrent(List<T> elements)
    {
        if (!this.multi && !elements.isEmpty())
        {
            this.setCurrent(elements.get(0));

            return;
        }

        this.current.clear();

        for (T element : elements)
        {
            int index = this.list.indexOf(element);

            if (this.exists(index))
            {
                this.current.add(index);
            }
        }
    }

    public void setCurrentScroll(T element)
    {
        this.setCurrent(element);

        if (!this.current.isEmpty())
        {
            this.scroll.setScroll(this.current.get(0) * this.scroll.scrollItemSize);
        }
    }

    public boolean pick(int index)
    {
        if (index < 0 || index >= this.list.size())
        {
            return false;
        }

        this.setIndex(index);

        if (this.callback != null)
        {
            this.callback.accept(this.getCurrent());
        }

        return true;
    }

    public void selectAll()
    {
        if (!this.multi)
        {
            return;
        }

        this.current.clear();

        for (int i = 0; i < this.list.size(); i ++)
        {
            this.current.add(i);
        }
    }

    public List<T> getList()
    {
        return this.list;
    }

    /* Content management */

    public void clear()
    {
        this.filter("");

        this.current.clear();
        this.list.clear();
        this.update();
    }

    public void add(T element)
    {
        this.list.add(element);
        this.update();
    }

    public void add(Collection<T> elements)
    {
        this.list.addAll(elements);
        this.update();
    }

    public void replace(T element)
    {
        int index = this.current.size() == 1 ? this.current.get(0) : -1;

        if (this.exists(index))
        {
            this.list.set(index, element);
        }
    }

    public void setList(List<T> list)
    {
        if (list == null)
        {
            return;
        }

        this.list = list;
        this.update();
    }

    public void remove(T element)
    {
        this.list.remove(element);
        this.update();
    }

    /**
     * Sort elements in this array, the subsclasses should implement
     * the other sorting method in order for it to work
     */
    public final void sort()
    {
        List<T> current = this.getCurrent();

        if (this.sortElements())
        {
            this.current.clear();

            for (T element : current)
            {
                this.current.add(this.list.indexOf(element));
            }
        }
    }

    /**
     * Sort elements
     */
    protected boolean sortElements()
    {
        return false;
    }

    /* Miscellaneous methods */

    public void update()
    {
        this.scroll.setSize(this.isFiltering() ? this.filtered.size() : this.list.size());
        this.scroll.clamp();
    }

    public boolean exists(int index)
    {
        return this.exists(this.list, index);
    }

    public boolean exists(List list, int index)
    {
        return index >= 0 && index < list.size();
    }

    public boolean isDragging()
    {
        return this.exists(this.dragging) && System.currentTimeMillis() - this.dragTime > 100;
    }

    public int getDraggingIndex()
    {
        return this.dragging;
    }

    @Override
    public void resize()
    {
        super.resize();

        this.scroll.clamp();
        this.scroll.updateTarget();
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.scroll.mouseClicked(context))
        {
            return true;
        }

        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            int index = this.scroll.getIndex(context.mouseX, context.mouseY);
            boolean filtering = this.isFiltering();

            if (filtering)
            {
                index = this.exists(this.filtered, index) ? this.filtered.get(index).b : -1;
            }

            if (this.exists(index))
            {
                if (this.multi && Window.isShiftPressed() && this.isSelected())
                {
                    int first = this.current.get(0);
                    int increment = first > index ? -1 : 1;

                    for (int i = first + increment; i != index + increment; i += increment)
                    {
                        this.addIndex(i);
                    }
                }
                else if (this.multi && Window.isCtrlPressed())
                {
                    this.toggleIndex(index);
                }
                else
                {
                    this.setIndex(index);
                }

                if (!filtering && this.sorting && this.current.size() == 1)
                {
                    this.dragging = index;
                    this.dragTime = System.currentTimeMillis();
                }

                List<T> current = this.getCurrent();

                if (this.callback != null)
                {
                    this.callback.accept(current);

                    return true;
                }
            }
        }

        return super.subMouseClicked(context);
    }

    @Override
    public boolean subMouseScrolled(UIContext context)
    {
        return this.scroll.mouseScroll(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.sorting && !this.isFiltering())
        {
            if (this.isDragging())
            {
                int index = this.scroll.getIndex(context.mouseX, context.mouseY);

                if (index == -2)
                {
                    index = this.getList().size() - 1;
                }

                if (index != this.dragging && this.exists(index))
                {
                    if (this.beforeDropCallback != null)
                    {
                        this.beforeDropCallback.accept(this);
                    }

                    T value = this.list.remove(this.dragging);

                    this.list.add(index, value);
                    this.setIndex(index);

                    if (this.afterDropCallback != null)
                    {
                        this.afterDropCallback.accept(this);
                    }
                }
            }

            this.dragging = -1;
        }

        this.scroll.mouseReleased(context);

        return super.subMouseReleased(context);
    }

    @Override
    public void render(UIContext context)
    {
        this.scroll.drag(context);

        if (Colors.getA(this.background) > 0)
        {
            this.area.render(context.batcher, this.background);
        }

        context.batcher.clip(this.area, context);
        this.renderList(context);
        this.scroll.renderScrollbar(context.batcher);
        context.batcher.unclip(context);

        this.renderLockedArea(context);

        super.render(context);

        if (this.exists(this.dragging) && this.isDragging())
        {
            this.renderListElement(context, this.list.get(this.dragging), this.dragging, context.mouseX + 6, context.mouseY - this.scroll.scrollItemSize / 2, true, true);
        }
    }

    public void renderList(UIContext context)
    {
        int i = 0;

        if (this.isFiltering())
        {
            for (Pair<T, Integer> element : this.filtered)
            {
                i = this.renderElement(context, element.a, i, element.b, false);

                if (i == -1)
                {
                    break;
                }
            }
        }
        else
        {
            for (T element : this.list)
            {
                i = this.renderElement(context, element, i, i, false);

                if (i == -1)
                {
                    break;
                }
            }
        }
    }

    public int renderElement(UIContext context, T element, int i, int index, boolean postDraw)
    {
        int mouseX = context.mouseX;
        int mouseY = context.mouseY;
        int s = this.scroll.scrollItemSize;

        int xSide = this.area.w;
        int ySide = this.scroll.scrollItemSize;

        int x = this.area.x;
        int y = this.area.y + i * s - (int) this.scroll.getScroll();

        int low = this.area.y;
        int high =this.area.ey();

        if (y + s < low || (!this.isFiltering() && this.isDragging() && this.dragging == i))
        {
            return i + 1;
        }

        if (y >= high)
        {
            return -1;
        }

        boolean hover = mouseX >= x && mouseY >= y && mouseX < x + xSide && mouseY < y + ySide;
        boolean selected = this.current.contains(index);

        if (postDraw)
        {
            this.renderPostListElement(context, element, index, x, y, hover, selected);
        }
        else
        {
            this.renderListElement(context, element, index, x, y, hover, selected);
        }

        return i + 1;
    }

    /**
     * Draw second pass of individual list element
     */
    public void renderPostListElement(UIContext context, T element, int i, int x, int y, boolean hover, boolean selected)
    {}

    /**
     * Draw individual element (with selection)
     */
    public void renderListElement(UIContext context, T element, int i, int x, int y, boolean hover, boolean selected)
    {
        if (selected)
        {
            context.batcher.box(x, y, x + this.area.w, y + this.scroll.scrollItemSize, Colors.A50 | BBSSettings.primaryColor.get());
        }

        this.renderElementPart(context, element, i, x, y, hover, selected);
    }

    /**
     * Draw only the main part (without selection or any hover elements)
     */
    protected void renderElementPart(UIContext context, T element, int i, int x, int y, boolean hover, boolean selected)
    {
        context.batcher.textShadow(this.elementToString(context, i, element), x + 4, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, hover ? Colors.HIGHLIGHT : Colors.WHITE);
    }

    /**
     * Convert element to string
     */
    protected String elementToString(UIContext context, int i, T element)
    {
        return element.toString();
    }
}