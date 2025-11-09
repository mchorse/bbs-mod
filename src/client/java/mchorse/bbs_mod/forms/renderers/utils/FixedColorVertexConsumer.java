package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.render.VertexConsumer;

/**
 * VertexConsumer que fija un color constante (incluido alpha) en el
 * Buffer subyacente mediante {@link VertexConsumer#fixedColor}.
 *
 * Útil para casos donde el renderer nunca llama a {@link VertexConsumer#color},
 * como muchos Block Entity renderers; así la transparencia global se aplica
 * igualmente.
 */
public class FixedColorVertexConsumer implements VertexConsumer
{
    private final VertexConsumer delegate;
    private final Color color;

    public FixedColorVertexConsumer(VertexConsumer delegate, Color color)
    {
        this.delegate = delegate;
        this.color = color;

        // Fijar color/alpha global al iniciar
        int r = (int)(color.r * 255f);
        int g = (int)(color.g * 255f);
        int b = (int)(color.b * 255f);
        int a = (int)(color.a * 255f);
        this.delegate.fixedColor(r, g, b, a);
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        return this.delegate.vertex(x, y, z);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        // Con fixedColor activo, este valor no se usará; delegar por seguridad
        return this.delegate.color(red, green, blue, alpha);
    }

    @Override
    public VertexConsumer texture(float u, float v)
    {
        return this.delegate.texture(u, v);
    }

    @Override
    public VertexConsumer overlay(int u, int v)
    {
        return this.delegate.overlay(u, v);
    }

    @Override
    public VertexConsumer light(int u, int v)
    {
        return this.delegate.light(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        return this.delegate.normal(x, y, z);
    }

    @Override
    public void next()
    {
        this.delegate.next();
    }

    @Override
    public void fixedColor(int red, int green, int blue, int alpha)
    {
        this.delegate.fixedColor(red, green, blue, alpha);
    }

    @Override
    public void unfixColor()
    {
        this.delegate.unfixColor();
    }
}