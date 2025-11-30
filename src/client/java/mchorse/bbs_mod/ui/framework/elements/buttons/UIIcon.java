package mchorse.bbs_mod.ui.framework.elements.buttons;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIIcon extends UIClickable<UIIcon>
{
    private Icon icon;
    private Supplier<Icon> iconSupplier;

    public int iconColor = Colors.WHITE;
    public int hoverColor = Colors.LIGHTEST_GRAY;
    public int activeColor = Colors.LIGHTEST_GRAY;

    public int disabledColor = 0x80404040;
    
    private boolean active;

    public UIIcon(Icon icon, Consumer<UIIcon> callback)
    {
        super(callback);

        this.icon = icon;
        this.wh(20, 20);
    }

    public UIIcon(Supplier<Icon> iconSupplier, Consumer<UIIcon> callback)
    {
        super(callback);

        this.iconSupplier = iconSupplier;
        this.wh(20, 20);
    }

    public Icon getIcon()
    {
        if (this.iconSupplier != null)
        {
            Icon icon = this.iconSupplier.get();

            if (icon != null)
            {
                return icon;
            }
        }

        return this.icon;
    }

    public UIIcon both(Icon icon)
    {
        this.icon = icon;

        return this;
    }

    public UIIcon both(Supplier<Icon> icon)
    {
        this.iconSupplier = icon;

        return this;
    }

    public UIIcon iconColor(int color)
    {
        this.iconColor = color;

        return this;
    }

    public UIIcon hoverColor(int color)
    {
        this.hoverColor = color;

        return this;
    }

    public UIIcon disabledColor(int color)
    {
        this.disabledColor = color;

        return this;
    }

    public UIIcon activeColor(int color)
    {
        this.activeColor = color;

        return this;
    }

    public UIIcon active(boolean active)
    {
        this.active = active;

        return this;
    }

    public boolean isActive()
    {
        return this.active;
    }

    @Override
    protected UIIcon get()
    {
        return this;
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        Icon icon = this.getIcon();
        int color;
        
        if (this.isEnabled())
        {
            if (this.active)
            {
                color = this.activeColor;
            }
            else
            {
                color = this.hover ? this.hoverColor : this.iconColor;
            }
        }
        else
        {
            color = this.disabledColor;
        }

        context.batcher.icon(icon, color, this.area.mx(), this.area.my(), 0.5F, 0.5F);
    }
}