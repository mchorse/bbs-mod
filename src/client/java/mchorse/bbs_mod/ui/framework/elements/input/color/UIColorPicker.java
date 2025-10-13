package mchorse.bbs_mod.ui.framework.elements.input.color;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.values.ui.ValueColors;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.MathUtils;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Color picker element
 *
 * This is the one that is responsible for picking colors
 */
public class UIColorPicker extends UIElement
{
    public static final int COLOR_SLIDER_HEIGHT = 50;

    public static ValueColors recentColors = new ValueColors("recent");

    public Color color = new Color();
    public Consumer<Integer> callback;

    public UITextbox input;
    public UIColorPalette recent;
    public UIColorPalette favorite;

    public boolean editAlpha;

    public Area red = new Area();
    public Area green = new Area();
    public Area blue = new Area();
    public Area alpha = new Area();

    private int dragging = -1;
    private Color hsv = new Color();

    public static void renderAlphaPreviewQuad(Batcher2D batcher, int x1, int y1, int x2, int y2, Color color)
    {
        Matrix4f matrix4f = batcher.getContext().getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        builder.vertex(matrix4f, x1, y1, 0F).color(color.r, color.g, color.b, 1).next();
        builder.vertex(matrix4f, x1, y2, 0F).color(color.r, color.g, color.b, 1).next();
        builder.vertex(matrix4f, x2, y1, 0F).color(color.r, color.g, color.b, 1).next();
        builder.vertex(matrix4f, x2, y1, 0F).color(color.r, color.g, color.b, color.a).next();
        builder.vertex(matrix4f, x1, y2, 0F).color(color.r, color.g, color.b, color.a).next();
        builder.vertex(matrix4f, x2, y2, 0F).color(color.r, color.g, color.b, color.a).next();

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public UIColorPicker(Consumer<Integer> callback)
    {
        super();

        this.callback = callback;

        this.input = new UITextbox(7, (string) ->
        {
            this.setValue(Colors.parse(string));
            this.callback();
        });
        this.input.context((menu) -> menu.action(Icons.FAVORITE, UIKeys.COLOR_CONTEXT_FAVORITES_ADD, () -> this.addToFavorites(this.color)));

        this.recent = new UIColorPalette((color) ->
        {
            this.setColor(color.getARGBColor());
            this.updateColor();
        }).colors(recentColors.getCurrentColors());

        this.recent.context((menu) ->
        {
            int index = this.recent.getIndex(this.getContext());

            if (this.recent.hasColor(index))
            {
                menu.action(Icons.FAVORITE, UIKeys.COLOR_CONTEXT_FAVORITES_ADD, () -> this.addToFavorites(this.recent.colors.get(index)));
            }
        });

        this.favorite = new UIColorPalette((color) ->
        {
            this.setColor(color.getARGBColor());
            this.updateColor();
        }).colors(BBSSettings.favoriteColors.getCurrentColors());

        this.favorite.context((menu) ->
        {
            int index = this.favorite.getIndex(this.getContext());

            if (this.favorite.hasColor(index))
            {
                menu.action(Icons.REMOVE, UIKeys.COLOR_CONTEXT_FAVORITES_REMOVE, () -> this.removeFromFavorites(index));
            }
        });

        this.input.relative(this).set(5, 5, 0, 20).w(1, -35);
        this.favorite.relative(this).xy(5, 95).w(1F, -10);
        this.recent.relative(this.favorite).w(1F);

        this.eventPropagataion(EventPropagation.BLOCK_INSIDE).add(this.input, this.favorite, this.recent);
    }

    public UIColorPicker editAlpha()
    {
        this.editAlpha = true;
        this.input.textbox.setLength(9);

        return this;
    }

    public void updateField()
    {
        this.input.setText(this.color.stringify(this.editAlpha));
    }

    public void updateColor()
    {
        this.updateField();
        this.callback();
    }

    protected void callback()
    {
        if (this.callback != null)
        {
            this.callback.accept(this.editAlpha ? this.color.getARGBColor() : this.color.getRGBColor());
        }
    }

    public void setColor(int color)
    {
        this.setValue(color);
        this.updateField();
    }

    public void setValue(int color)
    {
        this.color.set(color, this.editAlpha);
        Colors.RGBtoHSV(this.hsv, this.color.r, this.color.g, this.color.b);
        this.hsv.a = this.color.a;
    }

    public void setup(int x, int y)
    {
        this.xy(x, y);
        this.setupSize();
    }

    private void setupSize()
    {
        int width = 200;
        int recent = this.recent.colors.isEmpty() ? 0 : this.recent.getHeight(width - 10);
        int favorite = this.favorite.colors.isEmpty() ? 0 : this.favorite.getHeight(width - 10);
        int base = 85;

        base += favorite > 0 ? favorite + 15 : 0;
        base += recent > 0 ? recent + 15 : 0;

        this.h(base);
        this.favorite.h(favorite);
        this.recent.h(recent);

        if (favorite > 0)
        {
            this.recent.y(1F, 15);
        }
        else
        {
            this.recent.y(0);
        }
    }

    /* Managing recent and favorite colors */

    private void addToRecent()
    {
        recentColors.addColor(this.color);
    }

    private void addToFavorites(Color color)
    {
        BBSSettings.favoriteColors.addColor(color);

        this.setupSize();
        this.resize();
    }

    private void removeFromFavorites(int index)
    {
        BBSSettings.favoriteColors.remove(index);

        this.setupSize();
        this.resize();
    }

    /* GuiElement overrides */

    @Override
    public void resize()
    {
        super.resize();

        int c = this.editAlpha ? 4 : 3;
        int h = COLOR_SLIDER_HEIGHT / c;
        int w = this.area.w - 10;
        int remainder = COLOR_SLIDER_HEIGHT - h * c;
        int y = this.area.y + 30;

        this.red.set(this.area.x + 5, y, w, h);

        if (this.editAlpha)
        {
            this.green.set(this.area.x + 5, y + h, w, h);
            this.blue.set(this.area.x + 5, y + h + h, w, h + remainder);
            this.alpha.set(this.area.x + 5, y + COLOR_SLIDER_HEIGHT - h, w, h);
        }
        else
        {
            this.green.set(this.area.x + 5, y + h, w, h + remainder);
            this.blue.set(this.area.x + 5, y + COLOR_SLIDER_HEIGHT - h, w, h);
        }
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.red.isInside(context))
        {
            this.dragging = 1;

            return true;
        }
        else if (this.green.isInside(context))
        {
            this.dragging = 2;

            return true;
        }
        else if (this.blue.isInside(context))
        {
            this.dragging = 3;

            return true;
        }
        else if (this.alpha.isInside(context) && this.editAlpha)
        {
            this.dragging = 4;

            return true;
        }

        if (!this.area.isInside(context))
        {
            this.removeFromParent();
            this.addToRecent();
        }

        return super.subMouseClicked(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        this.dragging = -1;

        return super.subMouseReleased(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.removeFromParent();
            this.addToRecent();

            return true;
        }

        return super.subKeyPressed(context);
    }

    @Override
    public void render(UIContext context)
    {
        boolean isHsv = BBSSettings.hsvColorPicker.get();
        Color color = isHsv ? this.hsv : this.color;

        if (this.dragging >= 0)
        {
            float factor = (context.mouseX - (this.red.x + 7)) / (float) (this.red.w - 14);

            color.set(MathUtils.clamp(factor, 0, 1), this.dragging);

            if (isHsv)
            {
                Colors.HSVtoRGB(this.color, this.hsv.r, this.hsv.g, this.hsv.b);
                this.color.a = this.hsv.a;
            }

            this.updateColor();
        }

        this.area.render(context.batcher, Colors.LIGHTEST_GRAY);
        this.renderRect(context.batcher, this.area.ex() - 25, this.area.y + 5, this.area.ex() - 5, this.area.y + 25);

        context.batcher.outline(this.area.ex() - 25, this.area.y + 5, this.area.ex() - 5, this.area.y + 25, Colors.A25);

        if (this.editAlpha)
        {
            context.batcher.iconArea(Icons.CHECKBOARD, this.alpha.x, this.red.y, this.alpha.w, this.alpha.ey() - this.red.y);
        }

        Color temp = new Color();
        int left = 0;
        int right = 0;

        if (isHsv)
        {
            temp.a = color.a;

            /* Draw hue slider */
            for (int i = 0; i < 6; i++)
            {
                Colors.HSVtoRGB(temp, i / 6F, 1F, 1F);
                left = temp.getARGBColor();
                Colors.HSVtoRGB(temp, (i + 1) / 6F, 1F, 1F);
                right = temp.getARGBColor();

                context.batcher.gradientHBox(this.red.x(i / 6F), this.red.y, this.red.x((i + 1) / 6F), this.red.ey(), left, right);
            }

            /* Draw green slider */
            Colors.HSVtoRGB(temp, this.hsv.r, 0F, this.hsv.b);
            left = temp.getARGBColor();
            Colors.HSVtoRGB(temp, this.hsv.r, 1F, this.hsv.b);
            right = temp.getARGBColor();

            context.batcher.gradientHBox(this.green.x, this.green.y, this.green.ex(), this.green.ey(), left, right);

            /* Draw blue slider */
            Colors.HSVtoRGB(temp, this.hsv.r, this.hsv.g, 0F);
            left = temp.getARGBColor();
            Colors.HSVtoRGB(temp, this.hsv.r, this.hsv.g, 1F);
            right = temp.getARGBColor();
            context.batcher.gradientHBox(this.blue.x, this.blue.y, this.blue.ex(), this.blue.ey(), left, right);

            if (this.editAlpha)
            {
                /* Draw alpha slider */
                Colors.HSVtoRGB(temp, this.hsv.r, this.hsv.g, this.hsv.b);
                temp.a = 0F;
                left = temp.getARGBColor();
                Colors.HSVtoRGB(temp, this.hsv.r, this.hsv.g, this.hsv.b);
                temp.a = 1F;
                right = temp.getARGBColor();

                context.batcher.gradientHBox(this.alpha.x, this.alpha.y, this.alpha.ex(), this.alpha.ey(), left, right);
            }
        }
        else
        {
            /* Draw red slider */
            temp.copy(color).r = 0;
            left = temp.getARGBColor();
            temp.copy(color).r = 1;
            right = temp.getARGBColor();

            context.batcher.gradientHBox(this.red.x, this.red.y, this.red.ex(), this.red.ey(), left, right);

            /* Draw green slider */
            temp.copy(color).g = 0;
            left = temp.getARGBColor();
            temp.copy(color).g = 1;
            right = temp.getARGBColor();

            context.batcher.gradientHBox(this.green.x, this.green.y, this.green.ex(), this.green.ey(), left, right);

            /* Draw blue slider */
            temp.copy(color).b = 0;
            left = temp.getARGBColor();
            temp.copy(color).b = 1;
            right = temp.getARGBColor();
            context.batcher.gradientHBox(this.blue.x, this.blue.y, this.blue.ex(), this.blue.ey(), left, right);

            if (this.editAlpha)
            {
                /* Draw alpha slider */
                temp.copy(color).a = 0;
                left = temp.getARGBColor();
                temp.copy(color).a = 1;
                right = temp.getARGBColor();

                context.batcher.gradientHBox(this.alpha.x, this.alpha.y, this.alpha.ex(), this.alpha.ey(), left, right);
            }
        }

        context.batcher.outline(this.red.x, this.red.y, this.red.ex(), this.editAlpha ? this.alpha.ey() : this.blue.ey(), 0x44000000);

        this.renderMarker(context.batcher, this.red.x + 7 + (int) ((this.red.w - 14) * color.r), this.red.my());
        this.renderMarker(context.batcher, this.green.x + 7 + (int) ((this.green.w - 14) * color.g), this.green.my());
        this.renderMarker(context.batcher, this.blue.x + 7 + (int) ((this.blue.w - 14) * color.b), this.blue.my());

        if (this.editAlpha)
        {
            this.renderMarker(context.batcher, this.alpha.x + 7 + (int) ((this.alpha.w - 14) * color.a), this.alpha.my());
        }

        if (!this.favorite.colors.isEmpty())
        {
            context.batcher.text(UIKeys.COLOR_FAVORITE.get(), this.favorite.area.x, this.favorite.area.y - 10, Colors.GRAY);
        }

        if (!this.recent.colors.isEmpty())
        {
            context.batcher.text(UIKeys.COLOR_RECENT.get(), this.recent.area.x, this.recent.area.y - 10, Colors.GRAY);
        }

        super.render(context);
    }

    public void renderRect(Batcher2D batcher, int x1, int y1, int x2, int y2)
    {
        if (this.editAlpha)
        {
            batcher.iconArea(Icons.CHECKBOARD, x1, y1, x2 - x1, y2 - y1);
            renderAlphaPreviewQuad(batcher, x1, y1, x2, y2, this.color);
        }
        else
        {
            batcher.box(x1, y1, x2, y2, this.color.getARGBColor());
        }
    }

    private void renderMarker(Batcher2D batcher, int x, int y)
    {
        batcher.box(x - 4, y - 4, x + 4, y + 4, Colors.A100);
        batcher.box(x - 3, y - 3, x + 3, y + 3, Colors.WHITE);
        batcher.box(x - 2, y - 2, x + 2, y + 2, Colors.LIGHTEST_GRAY);
    }
}