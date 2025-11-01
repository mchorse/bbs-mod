package mchorse.bbs_mod.utils.colors;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.StringUtils;

public class Color
{
    public float r;
    public float g;
    public float b;
    public float a = 1F;

    public static Color rgb(int rgb)
    {
        return new Color().set(rgb, false);
    }

    public static Color rgba(int rgba)
    {
        return new Color().set(rgba);
    }

    public static Color white()
    {
        return new Color(1F, 1F, 1F, 1F);
    }

    public Color()
    {}

    public Color(float r, float g, float b)
    {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public Color(float r, float g, float b, float a)
    {
        this(r, g, b);

        this.a = a;
    }

    public Color set(float r, float g, float b)
    {
        return this.set(r, g, b, 1);
    }

    public Color set(float r, float g, float b, float a)
    {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;

        return this;
    }

    public Color set(float value, int component)
    {
        switch (component)
        {
            case 1:
                this.r = value;
            break;

            case 2:
                this.g = value;
            break;

            case 3:
                this.b = value;
            break;

            default:
                this.a = value;
            break;
        }

        return this;
    }

    public Color set(int color)
    {
        return this.set(color, true);
    }

    public Color set(int color, boolean alpha)
    {
        float r = (color >> 16 & 0xff) / 255F;
        float g = (color >> 8 & 0xff) / 255F;
        float b = (color & 0xff) / 255F;
        float a = alpha ? (color >> 24 & 0xff) / 255F : 1F;

        this.set(r, g, b, a);

        return this;
    }

    public Color copy()
    {
        return new Color().copy(this);
    }

    public Color copy(Color color)
    {
        this.set(color.r, color.g, color.b, color.a);

        return this;
    }

    public int getARGBColor()
    {
        float r = MathUtils.clamp(this.r, 0, 1);
        float g = MathUtils.clamp(this.g, 0, 1);
        float b = MathUtils.clamp(this.b, 0, 1);
        float a = MathUtils.clamp(this.a, 0, 1);

        return ((int) (a * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public int getRGBAColor()
    {
        return this.getRGBColor() << 8 + ((int) (this.a * 255) & 0xff);
    }

    public int getRGBColor()
    {
        return this.getARGBColor() & Colors.RGB;
    }

    public String stringify()
    {
        return this.stringify(false);
    }

    public String stringify(boolean alpha)
    {
        if (alpha)
        {
            return "#" + StringUtils.leftPad(Integer.toHexString(this.getARGBColor()), 8, "0");
        }

        return "#" + StringUtils.leftPad(Integer.toHexString(this.getRGBColor()), 6, "0");
    }

    public void mul(int color)
    {
        Color newColor = new Color().set(color, true);

        this.r *= newColor.r;
        this.g *= newColor.g;
        this.b *= newColor.b;
        this.a *= newColor.a;
    }

    public void mul(Color set)
    {
        this.r *= set.r;
        this.g *= set.g;
        this.b *= set.b;
        this.a *= set.a;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Color)
        {
            Color color = (Color) obj;

            return color.getARGBColor() == this.getARGBColor();
        }

        return super.equals(obj);
    }

    @Override
    public String toString()
    {
        int r = (int) (MathUtils.clamp(this.r, 0F, 1F) * 255F);
        int g = (int) (MathUtils.clamp(this.g, 0F, 1F) * 255F);
        int b = (int) (MathUtils.clamp(this.b, 0F, 1F) * 255F);
        int a = (int) (MathUtils.clamp(this.a, 0F, 1F) * 255F);

        return "rgba(" + r + ", " + g + ", " + b + ", " + a + ")";
    }
}