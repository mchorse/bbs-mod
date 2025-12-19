package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.utils.colors.Colors;
import org.joml.Vector2i;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIDraggable extends UIElement
{
    private Consumer<UIContext> callback;
    private Consumer<UIContext> render;
    private Supplier<Vector2i> reference;
    private boolean dragging;
    private boolean hover;

    private int mouseX;
    private int mouseY;
    private Vector2i referenceMouse;

    public UIDraggable(Consumer<UIContext> callback)
    {
        this.callback = callback;
    }

    public UIDraggable hoverOnly()
    {
        this.hover = true;

        return this;
    }

    public UIDraggable rendering(Consumer<UIContext> render)
    {
        this.render = render;

        return this;
    }

    public UIDraggable reference(Supplier<Vector2i> reference)
    {
        this.reference = reference;

        return this;
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            this.mouseX = context.mouseX;
            this.mouseY = context.mouseY;
            this.dragging = true;

            if (this.reference != null)
            {
                this.referenceMouse = this.reference.get();
            }

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        this.dragging = false;

        return super.subMouseReleased(context);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (!this.hover || this.area.isInside(context) || this.dragging)
        {
            if (this.render != null)
            {
                this.render.accept(context);
            }
            else
            {
                Scroll.bar(context.batcher, this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50);
            }
        }

        if (this.dragging && this.callback != null)
        {
            int mouseX = context.mouseX;
            int mouseY = context.mouseY;

            if (this.referenceMouse != null)
            {
                context.mouseX = this.referenceMouse.x + (mouseX - this.mouseX);
                context.mouseY = this.referenceMouse.y + (mouseY - this.mouseY);
            }

            this.callback.accept(context);

            context.mouseX = mouseX;
            context.mouseY = mouseY;
        }
    }
}