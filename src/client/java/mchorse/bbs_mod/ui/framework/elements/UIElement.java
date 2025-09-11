package mchorse.bbs_mod.ui.framework.elements;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.events.EventManager;
import mchorse.bbs_mod.ui.framework.elements.events.UIAddedEvent;
import mchorse.bbs_mod.ui.framework.elements.events.UIRemovedEvent;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.framework.tooltips.ITooltip;
import mchorse.bbs_mod.ui.framework.tooltips.LabelTooltip;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeybindManager;
import mchorse.bbs_mod.ui.utils.resizers.Flex;
import mchorse.bbs_mod.ui.utils.resizers.IResizer;
import mchorse.bbs_mod.ui.utils.resizers.Margin;
import mchorse.bbs_mod.ui.utils.resizers.constraint.BoundsResizer;
import mchorse.bbs_mod.ui.utils.resizers.layout.ColumnResizer;
import mchorse.bbs_mod.ui.utils.resizers.layout.GridResizer;
import mchorse.bbs_mod.ui.utils.resizers.layout.RowResizer;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.undo.IUndoElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIElement implements IUIElement, IUndoElement
{
    private String undoId = "";

    /**
     * Area of this element (i.e. position and size) 
     */
    public Area area = new Area();

    /**
     * Element's margin (it's used only by layout resizers)
     */
    public final Margin margin = new Margin();

    /**
     * Flex resizer of this class
     */
    protected Flex flex = new Flex();

    /**
     * Resizer of this class
     */
    protected IResizer resizer = this.flex;

    /**
     * Tooltip instance
     */
    public ITooltip tooltip;

    /**
     * Keybind manager
     */
    private KeybindManager keybinds;

    /**
     * Context menu supplier
     */
    private Supplier<UIContextMenu> contextSupplier;

    /**
     * Context menu options
     */
    private List<Consumer<ContextMenuManager>> contextOptions;

    /**
     * Whether this element can be culled if it's out of viewport
     */
    public boolean culled = true;

    /**
     * Whether this element is a container
     */
    protected boolean container;

    /**
     * Determines how mouse events will be propagated
     */
    protected EventPropagation mousePropagation = EventPropagation.PASS;

    /**
     * Determines how keyboard events will be propagated
     */
    protected EventPropagation keyboardPropagation = EventPropagation.PASS;

    /**
     * Parent GUI element
     */
    protected UIElement parent;

    /**
     * Children elements
     */
    private List<IUIElement> children = new ArrayList<>();

    /**
     * Whether this element is enabled (can handle any input) 
     */
    protected boolean enabled = true;

    /**
     * Whether this element is visible 
     */
    protected boolean visible = true;

    protected EventManager events = new EventManager();

    /**
     * Custom data that can be stored within this UI element
     */
    private Map<String, Object> customData;

    public EventManager getEvents()
    {
        return this.events;
    }

    /* Hierarchy management */

    public UIBaseMenu.UIRootElement getRoot()
    {
        UIElement element = this;

        while (element.getParent() != null)
        {
            element = element.getParent();
        }

        return element instanceof UIBaseMenu.UIRootElement ? (UIBaseMenu.UIRootElement) element : null;
    }

    public UIContext getContext()
    {
        UIBaseMenu.UIRootElement root = this.getRoot();

        return root == null ? null : root.getContext();
    }

    public UIElement getParent()
    {
        return this.parent;
    }

    public <T extends UIElement> T getParent(Class<T> clazz)
    {
        UIElement element = this;

        while (element != null)
        {
            element = element.getParent();

            if (element.getClass() == clazz)
            {
                return (T) element;
            }
        }

        return null;
    }

    public boolean hasParent()
    {
        return this.parent != null;
    }

    public boolean isDescendant(UIElement element)
    {
        if (this == element)
        {
            return false;
        }

        while (element != null)
        {
            if (element.parent == this)
            {
                return true;
            }

            element = element.parent;
        }

        return false;
    }

    public List<IUIElement> getChildren()
    {
        return this.children;
    }

    public <T> List<T> getChildren(Class<T> clazz)
    {
        return this.getChildren(clazz, new ArrayList<>());
    }

    public <T> List<T> getChildren(Class<T> clazz, List<T> list)
    {
        return this.getChildren(clazz, list, false);
    }

    public <T> List<T> getChildren(Class<T> clazz, List<T> list, boolean includeItself)
    {
        if (includeItself && clazz.isAssignableFrom(this.getClass()))
        {
            list.add(clazz.cast(this));
        }

        for (IUIElement element : this.getChildren())
        {
            if (clazz.isAssignableFrom(element.getClass()))
            {
                list.add(clazz.cast(element));
            }

            if (element instanceof UIElement)
            {
                ((UIElement) element).getChildren(clazz, list, includeItself);
            }
        }

        return list;
    }

    public <T> void visitChildren(Class<T> clazz, boolean includeItself, Consumer<T> consumer)
    {
        if (consumer == null)
        {
            return;
        }

        if (includeItself && clazz.isAssignableFrom(this.getClass()))
        {
            consumer.accept(clazz.cast(this));
        }

        for (IUIElement element : this.getChildren())
        {
            if (clazz.isAssignableFrom(element.getClass()))
            {
                consumer.accept(clazz.cast(element));
            }

            if (element instanceof UIElement)
            {
                ((UIElement) element).visitChildren(clazz, includeItself, consumer);
            }
        }
    }

    public void prepend(IUIElement element)
    {
        if (element != null)
        {
            this.children.add(0, element);
            this.markChild(element);
        }
    }

    public void add(IUIElement element)
    {
        if (element != null)
        {
            this.children.add(element);
            this.markChild(element);
        }
    }

    public void add(IUIElement... elements)
    {
        for (IUIElement element : elements)
        {
            if (element != null)
            {
                this.children.add(element);
                this.markChild(element);
            }
        }
    }

    public void addAfter(IUIElement target, IUIElement element)
    {
        int index = this.children.indexOf(target);

        if (index != -1 && element != null)
        {
            if (index + 1 >= this.children.size())
            {
                this.children.add(element);
            }
            else
            {
                this.children.add(index + 1, element);
            }

            this.markChild(element);
        }
    }

    public void addBefore(IUIElement target, IUIElement element)
    {
        int index = this.children.indexOf(target);

        if (index != -1 && element != null)
        {
            this.children.add(index, element);

            this.markChild(element);
        }
    }

    private void markChild(IUIElement element)
    {
        if (element instanceof UIElement)
        {
            UIElement child = (UIElement) element;

            child.parent = this;
            child.onAdd(this);

            if (this.resizer != null)
            {
                this.resizer.add(this, child);
            }
        }
    }

    public void removeAll()
    {
        for (IUIElement uiElement : this.children)
        {
            if (uiElement instanceof UIElement)
            {
                UIElement element = (UIElement) uiElement;

                if (this.resizer != null)
                {
                    this.resizer.remove(this, element);
                }

                element.onRemove(element.parent);
                element.parent = null;
            }
        }

        this.children.clear();
    }

    public void removeFromParent()
    {
        if (this.hasParent())
        {
            this.parent.remove(this);
        }
    }

    public void remove(IUIElement element)
    {
        this.children.remove(element);
    }

    public void remove(UIElement element)
    {
        if (this.children.remove(element))
        {
            if (this.resizer != null)
            {
                this.resizer.remove(this, element);
            }

            element.onRemove(element.parent);
            element.parent = null;
        }
    }

    protected void onAdd(UIElement parent)
    {
        this.events.emit(new UIAddedEvent(this));

        for (IUITreeEventListener listener : this.getChildren(IUITreeEventListener.class))
        {
            listener.onAddedToTree(this);
        }
    }

    protected void onRemove(UIElement parent)
    {
        this.events.emit(new UIRemovedEvent(this));

        for (IUITreeEventListener listener : this.getChildren(IUITreeEventListener.class))
        {
            listener.onRemovedFromTree(this);
        }
    }

    public UIElement eventPropagataion(EventPropagation propagation)
    {
        return this.mouseEventPropagataion(propagation).keyboardEventPropagataion(propagation);
    }

    public UIElement mouseEventPropagataion(EventPropagation propagation)
    {
        this.mousePropagation = propagation;

        return this;
    }

    public UIElement keyboardEventPropagataion(EventPropagation propagation)
    {
        this.keyboardPropagation = propagation;

        return this;
    }

    /* Custom data */

    public Object getCustomValue(String key)
    {
        return this.customData == null ? null : this.customData.get(key);
    }

    public void setCustomValue(String key, Object value)
    {
        if (this.customData == null)
        {
            this.customData = new HashMap<>();
        }

        this.customData.put(key, value);
    }

    /* Setters */

    public UIElement removeTooltip()
    {
        this.tooltip = null;

        return this;
    }

    public UIElement tooltip(ITooltip tooltip)
    {
        this.tooltip = tooltip;

        return this;
    }

    public UIElement tooltip(IKey label)
    {
        return this.tooltip(label, Direction.BOTTOM);
    }

    public UIElement tooltip(IKey label, Direction direction)
    {
        return this.tooltip(new LabelTooltip(label, direction));
    }

    public UIElement tooltip(IKey label, int width, Direction direction)
    {
        return this.tooltip(new LabelTooltip(label, width, direction));
    }

    public UIElement noCulling()
    {
        this.culled = false;

        return this;
    }

    /* Keybind manager */

    public KeybindManager keys()
    {
        if (this.keybinds == null)
        {
            this.keybinds = new KeybindManager();
        }

        return this.keybinds;
    }

    /* Container stuff */

    public UIElement markContainer()
    {
        this.container = true;

        return this;
    }

    public boolean isContainer()
    {
        return this.container;
    }

    public UIElement getParentContainer()
    {
        UIElement element = this.getParent();

        while (element != null && !element.isContainer())
        {
            element = element.getParent();
        }

        return element;
    }

    public void resetContext()
    {
        this.contextOptions = null;
    }

    public UIElement context(Supplier<UIContextMenu> supplier)
    {
        if (supplier != null)
        {
            this.contextSupplier = supplier;
        }

        return this;
    }

    public UIElement context(Consumer<ContextMenuManager> consumer)
    {
        if (consumer != null)
        {
            if (this.contextOptions == null)
            {
                this.contextOptions = new ArrayList<>();
            }

            this.contextOptions.add(consumer);
        }

        return this;
    }

    /**
     * Create a context menu instance
     *
     * Some subclasses of UIElement might want to override this method in order to create their
     * own context menus.
     */
    public UIContextMenu createContextMenu(UIContext context)
    {
        if (this.contextSupplier != null)
        {
            return this.contextSupplier.get();
        }

        if (this.contextOptions == null)
        {
            return null;
        }

        ContextMenuManager manager = new ContextMenuManager();

        for (Consumer<ContextMenuManager> consumer : this.contextOptions)
        {
            consumer.accept(manager);
        }

        return manager.create();
    }

    /* Resizer methods */

    public Flex getFlex()
    {
        return this.flex;
    }

    public IResizer resizer()
    {
        return this.resizer;
    }

    public UIElement resizer(IResizer resizer)
    {
        this.resizer = resizer;

        return this;
    }

    public UIElement resetFlex()
    {
        this.flex.x.reset();
        this.flex.y.reset();
        this.flex.w.reset();
        this.flex.h.reset();

        this.flex.relative = this.flex.post = null;

        return this;
    }

    public UIElement set(int x, int y, int w, int h)
    {
        this.flex.x.set(0, x);
        this.flex.y.set(0, y);
        this.flex.w.set(0, w);
        this.flex.h.set(0, h);

        return this;
    }

    /* X */

    public UIElement x(int offset)
    {
        this.flex.x.set(0, offset);

        return this;
    }

    public UIElement x(float value)
    {
        this.flex.x.set(value, 0);

        return this;
    }

    public UIElement x(float value, int offset)
    {
        this.flex.x.set(value, offset);

        return this;
    }

    /* Y */

    public UIElement y(int offset)
    {
        this.flex.y.set(0, offset);

        return this;
    }

    public UIElement y(float value)
    {
        this.flex.y.set(value, 0);

        return this;
    }

    public UIElement y(float value, int offset)
    {
        this.flex.y.set(value, offset);

        return this;
    }

    /* Width */

    public UIElement w(int offset)
    {
        this.flex.w.set(0, offset);

        return this;
    }

    public UIElement w(float value)
    {
        this.flex.w.set(value, 0);

        return this;
    }

    public UIElement w(float value, int offset)
    {
        this.flex.w.set(value, offset);

        return this;
    }

    public UIElement wTo(IResizer flex)
    {
        this.flex.w.target = flex;

        return this;
    }

    public UIElement wTo(IResizer flex, int offset)
    {
        this.flex.w.target = flex;
        this.flex.w.offset = offset;

        return this;
    }

    public UIElement wTo(IResizer flex, float anchor)
    {
        this.flex.w.target = flex;
        this.flex.w.targetAnchor = anchor;

        return this;
    }

    public UIElement wTo(IResizer flex, float anchor, int offset)
    {
        this.flex.w.target = flex;
        this.flex.w.targetAnchor = anchor;
        this.flex.w.offset = offset;

        return this;
    }

    /* Height */

    public UIElement h(int offset)
    {
        this.flex.h.set(0, offset);

        return this;
    }

    public UIElement h(float value)
    {
        this.flex.h.set(value, 0);

        return this;
    }

    public UIElement h(float value, int offset)
    {
        this.flex.h.set(value, offset);

        return this;
    }

    public UIElement hTo(IResizer target)
    {
        return this.hTo(target, 0);
    }

    public UIElement hTo(IResizer target, int offset)
    {
        return this.hTo(target, 0F, offset);
    }

    public UIElement hTo(IResizer target, float anchor)
    {
        return this.hTo(target, anchor, 0);
    }

    public UIElement hTo(IResizer target, float anchor, int offset)
    {
        this.flex.h.target = target;
        this.flex.h.targetAnchor = anchor;
        this.flex.h.offset = offset;

        return this;
    }

    /* Other variations */

    public UIElement xy(int x, int y)
    {
        this.flex.x.set(0, x);
        this.flex.y.set(0, y);

        return this;
    }

    public UIElement xy(float x, float y)
    {
        this.flex.x.set(x);
        this.flex.y.set(y);

        return this;
    }

    public UIElement wh(int w, int h)
    {
        this.flex.w.set(0, w);
        this.flex.h.set(0, h);

        return this;
    }

    public UIElement full(IResizer relative)
    {
        return this.relative(relative).wh(1F, 1F);
    }

    public UIElement full(UIElement relative)
    {
        return this.relative(relative).wh(1F, 1F);
    }

    public UIElement wh(float w, float h)
    {
        this.flex.w.set(w);
        this.flex.h.set(h);

        return this;
    }

    public UIElement minW(int max)
    {
        this.flex.w.min = max;

        return this;
    }

    public UIElement minH(int max)
    {
        this.flex.h.min = max;

        return this;
    }

    public UIElement maxW(int max)
    {
        this.flex.w.max = max;

        return this;
    }

    public UIElement maxH(int max)
    {
        this.flex.h.max = max;

        return this;
    }

    public UIElement anchor(float x)
    {
        return this.anchor(x, x);
    }

    public UIElement anchor(float x, float y)
    {
        this.flex.x.anchor = x;
        this.flex.y.anchor = y;

        return this;
    }

    public UIElement anchorX(float x)
    {
        this.flex.x.anchor = x;

        return this;
    }

    public UIElement anchorY(float y)
    {
        this.flex.y.anchor = y;

        return this;
    }

    /* Post resizers convenience methods */

    public RowResizer row()
    {
        return this.row(5);
    }

    public RowResizer row(int margin)
    {
        if (this.flex.post instanceof RowResizer)
        {
            return (RowResizer) this.flex.post;
        }

        return RowResizer.apply(this, margin);
    }

    public ColumnResizer column()
    {
        return this.column(5);
    }

    public ColumnResizer column(int margin)
    {
        if (this.flex.post instanceof ColumnResizer)
        {
            return (ColumnResizer) this.flex.post;
        }

        return ColumnResizer.apply(this, margin);
    }

    public GridResizer grid(int margin)
    {
        if (this.flex.post instanceof GridResizer)
        {
            return (GridResizer) this.flex.post;
        }

        return GridResizer.apply(this, margin);
    }

    public BoundsResizer bounds(UIElement target, int padding)
    {
        if (this.flex.post instanceof BoundsResizer boundsResizer)
        {
            boundsResizer.target = target;
            boundsResizer.padding = padding;

            return boundsResizer;
        }

        return BoundsResizer.apply(this, target, padding);
    }

    /* Hierarchy */

    public UIElement relative(UIElement element)
    {
        this.flex.relative = element.area;

        return this;
    }

    public UIElement relative(IResizer relative)
    {
        this.flex.relative = relative;

        return this;
    }

    public UIElement post(IResizer post)
    {
        this.flex.post = post;

        return this;
    }

    /* Margin */

    public UIElement margin(int all)
    {
        return this.margin(all, all);
    }

    public UIElement margin(int horizontal, int vertical)
    {
        return this.margin(horizontal, vertical, horizontal, vertical);
    }

    public UIElement margin(int left, int top, int right, int bottom)
    {
        this.margin.all(left, top, right, bottom);

        return this;
    }

    public UIElement marginLeft(int left)
    {
        this.margin.left(left);

        return this;
    }

    public UIElement marginTop(int top)
    {
        this.margin.top(top);

        return this;
    }

    public UIElement marginRight(int right)
    {
        this.margin.right(right);

        return this;
    }

    public UIElement marginBottom(int bottom)
    {
        this.margin.bottom(bottom);

        return this;
    }

    /* Enabled methods */

    @Override
    public boolean isEnabled()
    {
        return this.enabled && this.visible;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public boolean isVisible()
    {
        return this.visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    public void toggleVisible()
    {
        this.visible = !this.visible;
    }

    /**
     * Whether element can be seen on the screen
     */
    public boolean canBeSeen()
    {
        if (!this.hasParent() || !this.isVisible())
        {
            return false;
        }

        UIElement element = this;

        while (true)
        {
            if (!element.isVisible())
            {
                return false;
            }

            UIElement parent = element.getParent();

            if (parent == null)
            {
                break;
            }

            element = parent;
        }

        return element instanceof UIBaseMenu.UIRootElement;
    }

    /* Overriding those methods so it would be much easier to 
     * override only needed methods in subclasses */

    @Override
    public void resize()
    {
        if (this.resizer != null)
        {
            this.resizer.apply(this.area);
        }

        this.afterResizeApplied();

        for (IUIElement element : this.children)
        {
            element.resize();
        }

        if (this.resizer != null)
        {
            this.resizer.postApply(this.area);
        }
    }

    protected void afterResizeApplied()
    {}

    public void clickItself()
    {
        this.clickItself(this.getContext());
    }

    public void clickItself(int mouseButton)
    {
        this.clickItself(this.getContext(), mouseButton);
    }

    public void clickItself(UIContext context)
    {
        this.clickItself(context, 0);
    }

    public void clickItself(UIContext context, int mouseButton)
    {
        if (!this.isEnabled())
        {
            return;
        }

        int mouseX = context.mouseX;
        int mouseY = context.mouseY;
        int button = context.mouseButton;

        context.mouseX = this.area.x + 1;
        context.mouseY = this.area.y + 1;
        context.mouseButton = mouseButton;

        this.mouseClicked(context);

        context.mouseX = mouseX;
        context.mouseY = mouseY;
        context.mouseButton = button;
    }

    /* Handling input events
     *
     * These methods are final to prevent changing the pipeline. You're free to
     * subclass children*, sub* or misc. event handling methods! */

    @Override
    public final IUIElement mouseClicked(UIContext context)
    {
        IUIElement element = this.childrenMouseClicked(context);

        if (element != null)
        {
            return element;
        }

        return this.subMouseClicked(context) || this.keybindsMouseClicked(context) || this.mouseClickedContextMenu(context) || this.cantPropagate(this.mousePropagation, context) ? this : null;
    }

    @Override
    public final IUIElement mouseScrolled(UIContext context)
    {
        IUIElement element = this.childrenMouseScrolled(context);

        if (element != null)
        {
            return element;
        }

        return this.subMouseScrolled(context) || this.cantPropagate(this.mousePropagation, context) ? this : null;
    }

    @Override
    public final IUIElement mouseReleased(UIContext context)
    {
        IUIElement element = this.childrenMouseReleased(context);

        if (element != null)
        {
            return element;
        }

        return this.subMouseReleased(context) || this.cantPropagate(this.mousePropagation, context) ? this : null;
    }

    @Override
    public final IUIElement keyPressed(UIContext context)
    {
        IUIElement element = this.childrenKeyPressed(context);

        if (element != null)
        {
            return element;
        }

        return this.subKeyPressed(context) || this.keybindsKeyPressed(context) || this.cantPropagate(this.keyboardPropagation, context) ? this : null;
    }

    @Override
    public final IUIElement textInput(UIContext context)
    {
        IUIElement element = this.childrenTextInput(context);

        if (element != null)
        {
            return element;
        }

        return this.subTextInput(context) || this.cantPropagate(this.keyboardPropagation, context) ? this : null;
    }

    /* Handling children input events */

    protected IUIElement childrenMouseClicked(UIContext context)
    {
        for (int i = this.children.size() - 1; i >= 0; i--)
        {
            IUIElement element = this.children.get(i);

            if (element.isEnabled())
            {
                IUIElement anElement = element.mouseClicked(context);

                if (anElement != null)
                {
                    return anElement;
                }
            }
        }

        return null;
    }

    protected IUIElement childrenMouseScrolled(UIContext context)
    {
        for (int i = this.children.size() - 1; i >= 0; i--)
        {
            IUIElement element = this.children.get(i);

            if (element.isEnabled())
            {
                IUIElement anElement = element.mouseScrolled(context);

                if (anElement != null)
                {
                    return anElement;
                }
            }
        }

        return null;
    }

    protected IUIElement childrenMouseReleased(UIContext context)
    {
        for (int i = this.children.size() - 1; i >= 0; i--)
        {
            IUIElement element = this.children.get(i);

            if (element.isEnabled())
            {
                IUIElement anElement = element.mouseReleased(context);

                if (anElement != null)
                {
                    return anElement;
                }
            }
        }

        return null;
    }

    protected IUIElement childrenKeyPressed(UIContext context)
    {
        for (int i = this.children.size() - 1; i >= 0; i--)
        {
            IUIElement element = this.children.get(i);

            if (element.isEnabled())
            {
                IUIElement anElement = element.keyPressed(context);

                if (anElement != null)
                {
                    return anElement;
                }
            }
        }

        return null;
    }

    protected IUIElement childrenTextInput(UIContext context)
    {
        for (int i = this.children.size() - 1; i >= 0; i--)
        {
            IUIElement element = this.children.get(i);

            if (element.isEnabled())
            {
                IUIElement anElement = element.textInput(context);

                if (anElement != null)
                {
                    return anElement;
                }
            }
        }

        return null;
    }

    /* Subclasses' input event handling */

    protected boolean subMouseClicked(UIContext context)
    {
        return false;
    }

    protected boolean subMouseScrolled(UIContext context)
    {
        return false;
    }

    protected boolean subMouseReleased(UIContext context)
    {
        return false;
    }

    protected boolean subKeyPressed(UIContext context)
    {
        return false;
    }

    protected boolean subTextInput(UIContext context)
    {
        return false;
    }

    /* Misc. input event handling */

    /**
     * Handle creating a context menu (when right clicked in the area, a context
     * menu may appear, if configured)
     */
    protected boolean mouseClickedContextMenu(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 1 && !context.hasContextMenu())
        {
            UIContextMenu menu = this.createContextMenu(context);

            if (menu != null && !menu.isEmpty())
            {
                context.setContextMenu(menu);

                return true;
            }
        }

        return false;
    }

    /**
     * Handle keybind manager's keybinds
     */
    protected boolean keybindsMouseClicked(UIContext context)
    {
        return this.keybinds != null && this.keybinds.checkMouse(context, this.area.isInside(context));
    }

    /**
     * Handle keybind manager's keybinds
     */
    protected boolean keybindsKeyPressed(UIContext context)
    {
        return this.keybinds != null && this.keybinds.check(context, this.area.isInside(context));
    }

    /**
     * Checks whether an input event can be propagated
     */
    protected boolean cantPropagate(EventPropagation propagation, UIContext context)
    {
        if (propagation == EventPropagation.BLOCK)
        {
             return true;
        }

        return propagation == EventPropagation.BLOCK_INSIDE && this.area.isInside(context);
    }

    /* Rendering */

    @Override
    public boolean canBeRendered(Area viewport)
    {
        return !this.culled || viewport.intersects(this.area);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.keybinds != null && this.isEnabled())
        {
            this.keybinds.add(context, this.area.isInside(context));
        }

        if (this.tooltip != null && this.area.isInside(context))
        {
            context.tooltip.set(context, this);
        }
        else if ((this.container || this.mousePropagation != EventPropagation.PASS) && this.area.isInside(context))
        {
            context.resetTooltip();
        }

        for (IUIElement element : this.children)
        {
            if (element.isVisible() && element.canBeRendered(context.getViewport()))
            {
                element.render(context);
            }
        }
    }

    public void renderTooltip(UIContext context, Area area)
    {
        context.tooltip.render(this.tooltip, context);
    }

    /**
     * Generic method for rendering locked (disabled) state of an input field
     */
    public void renderLockedArea(UIContext context)
    {
        if (!this.isEnabled())
        {
            this.area.render(context.batcher, Colors.A50);

            context.batcher.outlinedIcon(Icons.LOCKED, this.area.mx(), this.area.my(), 0.5F, 0.5F);
        }
    }

    /* IUndoElement implementation */

    @Override
    public String getUndoId()
    {
        return this.undoId;
    }

    public void setUndoId(String undoId)
    {
        this.undoId = undoId;
    }

    @Override
    public void applyUndoData(MapType data)
    {}

    public void applyAllUndoData(MapType data)
    {
        this.visitChildren(IUndoElement.class, true, (child) ->
        {
            String id = child.getUndoId();

            if (!id.isEmpty() && data.has(id))
            {
                child.applyUndoData(data.getMap(id));
            }
        });
    }

    @Override
    public void collectUndoData(MapType data)
    {}

    public MapType collectAllUndoData()
    {
        MapType uiData = new MapType();

        this.visitChildren(IUndoElement.class, true, (child) ->
        {
            String id = child.getUndoId();

            if (!id.isEmpty())
            {
                MapType data = new MapType();

                child.collectUndoData(data);
                uiData.put(id, data);
            }
        });

        return uiData;
    }
}