package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import org.joml.Vector2i;

import java.util.function.Consumer;

public class UITexturePainter extends UIElement
{
    public UITrackpad brightness;
    public UIElement savebar;

    public UIColor primary;
    public UIColor secondary;

    public UITextureEditor main;
    public UITextureEditor reference;

    public UITexturePainter(Consumer<Link> saveCallback)
    {
        this.brightness = new UITrackpad();
        this.brightness.limit(0, 1).setValue(0.7);
        this.brightness.tooltip(UIKeys.TEXTURES_VIEWER_BRIGHTNESS, Direction.TOP);
        this.brightness.relative(this).x(1F, -10).y(1F, -10).w(130).anchor(1F, 1F);

        this.savebar = new UIElement();
        this.savebar.relative(this).x(1F).h(30).anchorX(1F).row(0).resize().padding(5);

        this.primary = new UIColor((c) -> {}).noLabel();
        this.primary.direction(Direction.RIGHT).w(20);
        this.secondary = new UIColor((c) -> {}).noLabel();
        this.secondary.direction(Direction.RIGHT).w(20);

        this.primary.setColor(0);
        this.secondary.setColor(Colors.WHITE);

        UIIcon open = new UIIcon(Icons.SEARCH, (b) ->
        {
            UITexturePicker.findAllTextures(this.getContext(), this.main.getTexture(), (s) ->
            {
                if (this.reference != null)
                {
                    this.reference.fillTexture(Link.create(s));
                    this.reference.setEditing(true);
                    this.resize();
                }
                else
                {
                    this.reference = new UITextureEditor();
                    this.reference.fillTexture(Link.create(s));
                    this.reference.setEditing(true);
                    this.reference
                        .colorSupplier(() -> this.primary.picker.color)
                        .backgroundSupplier(() -> (float) this.brightness.getValue());

                    this.reference.full(this).x(0.5F).wTo(this.area, 1F);
                    this.main.w(0.5F);

                    this.addAfter(this.main, this.reference);
                    this.resize();
                }
            });
        });

        this.savebar.add(open);

        this.main = new UITextureEditor().saveCallback(saveCallback);
        this.main
            .colorSupplier(() -> this.primary.picker.color)
            .backgroundSupplier(() -> (float) this.brightness.getValue());
        this.main.full(this);
        this.main.toolbar.prepend(this.secondary.marginRight(10));
        this.main.toolbar.prepend(this.primary);

        this.add(this.main, this.savebar);
        this.add(this.brightness);

        IKey category = UIKeys.TEXTURES_KEYS_CATEGORY;

        this.keys().register(Keys.PIXEL_SWAP, this::swapColors).inside().category(category);
        this.keys().register(Keys.PIXEL_PICK, this::pickColor).inside().category(category);
        this.keys().register(Keys.PIXEL_FILL, this::fillColor).inside().category(category);
    }

    private void swapColors()
    {
        int swap = this.primary.picker.color.getRGBColor();

        this.primary.setColor(this.secondary.picker.color.getRGBColor());
        this.secondary.setColor(swap);
    }

    private UITextureEditor getHoverEditor(UIContext context)
    {
        return this.main.area.isInside(context) ? this.main : (this.reference != null && this.reference.area.isInside(context) ? this.reference : null);
    }

    private void pickColor()
    {
        UIContext context = this.getContext();
        UITextureEditor editor = this.getHoverEditor(context);

        if (editor != null)
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);
            Color color = editor.getPixels().getColor(pixel.x, pixel.y);

            if (color != null)
            {
                this.primary.setColor(color.getRGBColor());
            }
        }
    }

    private void fillColor()
    {
        UIContext context = this.getContext();
        UITextureEditor editor = this.getHoverEditor(context);

        if (editor != null)
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);

            editor.fillColor(pixel, this.primary.picker.color, Window.isShiftPressed());
        }
    }

    public void fillTexture(Link current)
    {
        this.main.fillTexture(current);
        this.main.setEditing(true);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        UITextureEditor editor = this.getHoverEditor(context);

        if (editor != null)
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);
            Color color = editor.getPixels().getColor(pixel.x, pixel.y);

            int r = 0;
            int g = 0;
            int b = 0;
            int a = 0;

            if (color != null)
            {
                r = (int) Math.floor(color.r * 255);
                g = (int) Math.floor(color.g * 255);
                b = (int) Math.floor(color.b * 255);
                a = (int) Math.floor(color.a * 255);
            }

            String[] information = {
                editor.getPixels().width + "x" + editor.getPixels().height + " (" + pixel.x + ", " + pixel.y + ")",
                "\u00A7cR\u00A7aG\u00A79B\u00A7rA (" + r + ", " + g + ", " + b + ", " + a + ")",
            };

            int x = this.area.x + 10;
            int y = this.area.ey() - context.batcher.getFont().getHeight() - 10 - (information.length - 1)* 14;

            for (String line : information)
            {
                context.batcher.textCard(line, x, y);

                y += 14;
            }
        }
    }
}