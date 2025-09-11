package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.textures.undo.PixelsUndo;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.UICanvasEditor;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.interps.rasterizers.LineRasterizer;
import mchorse.bbs_mod.utils.resources.Pixels;
import mchorse.bbs_mod.utils.undo.IUndo;
import mchorse.bbs_mod.utils.undo.UndoManager;
import org.joml.Vector2d;
import org.joml.Vector2i;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class UIPixelsEditor extends UICanvasEditor
{
    public UIElement toolbar;

    /* Tools */
    public UIIcon undo;
    public UIIcon redo;

    private Texture temporary;
    private Pixels pixels;

    private boolean editing;
    private Color drawColor;
    private Vector2i lastPixel;

    protected UndoManager<Pixels> undoManager;
    private PixelsUndo pixelsUndo;

    private Supplier<Float> backgroundSupplier = () -> 0.7F;
    private Supplier<Color> colorSupplier = Color::white;

    public UIPixelsEditor()
    {
        super();

        this.toolbar = new UIElement();
        this.toolbar.relative(this).w(1F).h(30).row(0).resize().padding(5);

        this.undo = new UIIcon(Icons.UNDO, (b) -> this.undo());
        this.undo.tooltip(UIKeys.TEXTURES_KEYS_UNDO, Direction.BOTTOM);
        this.redo = new UIIcon(Icons.REDO, (b) -> this.redo());
        this.redo.tooltip(UIKeys.TEXTURES_KEYS_REDO, Direction.BOTTOM);

        this.toolbar.add(this.undo, this.redo);

        this.add(this.toolbar);

        IKey category = UIKeys.TEXTURES_KEYS_CATEGORY;
        Supplier<Boolean> texture = () -> this.pixels != null;
        Supplier<Boolean> editing = () -> this.editing;

        this.keys().register(Keys.COPY, this::copyPixel).label(UIKeys.TEXTURES_VIEWER_CONTEXT_COPY_HEX).inside().active(texture).category(category);
        this.keys().register(Keys.UNDO, this::undo).inside().active(editing).category(category);
        this.keys().register(Keys.REDO, this::redo).inside().active(editing).category(category);

        this.setEditing(false);
    }

    public UIPixelsEditor colorSupplier(Supplier<Color> supplier)
    {
        this.colorSupplier = supplier;

        return this;
    }

    public UIPixelsEditor backgroundSupplier(Supplier<Float> supplier)
    {
        this.backgroundSupplier = supplier;

        return this;
    }

    public Pixels getPixels()
    {
        return this.pixels;
    }

    protected void wasChanged()
    {}

    public boolean isEditing()
    {
        return this.editing;
    }

    public void toggleEditor()
    {
        this.setEditing(!this.editing);
    }

    public void setEditing(boolean editing)
    {
        this.editing = editing;

        this.toolbar.setVisible(editing);

        if (editing)
        {
            this.undoManager = new UndoManager<>();
            this.undoManager.setCallback(this::handleUndo);
        }
        else
        {
            this.undoManager = null;
        }

        this.pixelsUndo = null;
    }

    private void handleUndo(IUndo<Pixels> pixelsIUndo, boolean redo)
    {
        this.updateTexture();
    }

    private void copyPixel()
    {
        UIContext context = this.getContext();
        int pixelX = (int) Math.floor(this.scaleX.from(context.mouseX)) + this.w / 2;
        int pixelY = (int) Math.floor(this.scaleY.from(context.mouseY)) + this.h / 2;
        Color color = this.pixels.getColor(pixelX, pixelY);

        if (color != null)
        {
            Window.setClipboard(color.stringify());

            UIUtils.playClick();
        }
    }

    protected void updateTexture()
    {
        this.pixels.rewindBuffer();
        this.temporary.bind();
        this.temporary.updateTexture(this.pixels);
    }

    private void undo()
    {
        if (this.undoManager.undo(this.pixels))
        {
            UIUtils.playClick();
        }
    }

    private void redo()
    {
        if (this.undoManager.redo(this.pixels))
        {
            UIUtils.playClick();
        }
    }

    public void fillPixels(Pixels pixels)
    {
        this.lastPixel = null;

        if (this.temporary != null)
        {
            this.temporary.delete();
            this.temporary = null;
        }

        this.setEditing(false);

        this.pixels = pixels;

        if (pixels != null)
        {
            this.temporary = new Texture();
            this.temporary.setFilter(GL11.GL_NEAREST);

            this.updateTexture();
            this.setSize(pixels.width, pixels.height);
        }
    }

    @Override
    protected boolean isMouseButtonAllowed(int mouseButton)
    {
        return super.isMouseButtonAllowed(mouseButton) || mouseButton == 1;
    }

    @Override
    protected void startDragging(UIContext context)
    {
        super.startDragging(context);

        if (this.editing && (this.mouse == 0 || this.mouse == 1) && this.pixelsUndo == null)
        {
            this.pixelsUndo = new PixelsUndo();
            this.drawColor = this.mouse == 1 ? new Color(0, 0, 0, 0) : this.colorSupplier.get();

            Vector2i pixel = this.getHoverPixel(context.mouseX, context.mouseY);

            this.pixelsUndo.setColor(this.pixels, pixel.x, pixel.y, this.drawColor);
            this.updateTexture();

            this.wasChanged();
        }
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.dragging && this.pixelsUndo != null)
        {
            Vector2i hoverPixel = this.getHoverPixel(context.mouseX, context.mouseY);

            if (Window.isShiftPressed() && this.lastPixel != null)
            {
                LineRasterizer rasterizer = new LineRasterizer(
                    new Vector2d(this.lastPixel.x, this.lastPixel.y),
                    new Vector2d(hoverPixel.x, hoverPixel.y)
                );
                Set<Vector2i> pixels = new HashSet<>();

                rasterizer.setupRange(0F, 1F, 1F / (float) this.lastPixel.distance(hoverPixel));
                rasterizer.solve(pixels);

                for (Vector2i pixel : pixels)
                {
                    this.pixelsUndo.setColor(this.pixels, pixel.x, pixel.y, this.drawColor);
                }

                this.updateTexture();
            }

            this.undoManager.pushUndo(this.pixelsUndo);

            this.pixelsUndo = null;
            this.lastPixel = hoverPixel;
        }

        return super.subMouseReleased(context);
    }

    @Override
    protected void renderBackground(UIContext context)
    {}

    @Override
    protected void renderCanvasFrame(UIContext context)
    {
        int x = -this.w / 2;
        int y = -this.h / 2;
        Area area = this.calculate(x, y, x + this.w, y + this.h);
        Texture texture = this.getRenderTexture(context);

        context.batcher.fullTexturedBox(texture, area.x, area.y, area.w, area.h);

        /* Draw current pixel */
        int pixelX = (int) Math.floor(this.scaleX.from(context.mouseX));
        int pixelY = (int) Math.floor(this.scaleY.from(context.mouseY));

        context.batcher.outline(
            (int) Math.round(this.scaleX.to(pixelX)), (int) Math.round(this.scaleY.to(pixelY)),
            (int) Math.round(this.scaleX.to(pixelX + 1)), (int) Math.round(this.scaleY.to(pixelY + 1)),
            Colors.A50
        );

        if (this.editing && this.dragging && (this.lastX != context.mouseX || this.lastY != context.mouseY) && (this.mouse == 0 || this.mouse == 1))
        {
            Vector2i last = this.getHoverPixel(this.lastX, this.lastY);
            Vector2i current = this.getHoverPixel(context.mouseX, context.mouseY);

            double distance = Math.max(new Vector2d(current.x, current.y).distance(last.x, last.y), 1);

            for (int i = 0; i <= distance; i++)
            {
                int xx = (int) Lerps.lerp(last.x, current.x, i / distance);
                int yy = (int) Lerps.lerp(last.y, current.y, i / distance);

                this.pixelsUndo.setColor(this.pixels, xx, yy, this.drawColor);
            }

            this.wasChanged();
            this.updateTexture();

            this.lastX = context.mouseX;
            this.lastY = context.mouseY;
        }
    }

    protected Texture getRenderTexture(UIContext context)
    {
        return this.temporary;
    }

    @Override
    protected void renderCheckboard(UIContext context, Area area)
    {
        int brightness = (int) (this.backgroundSupplier.get() * 255);
        int color = Colors.setA(brightness << 16 | brightness << 8 | brightness, 1F);

        context.batcher.iconArea(Icons.CHECKBOARD, color, area.x, area.y, area.w, area.h);
    }

    @Override
    protected void renderForeground(UIContext context)
    {
        super.renderForeground(context);

        if (this.editing)
        {
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + 10, Colors.A50);
            context.batcher.gradientVBox(this.area.x, this.area.y + 10, this.area.ex(), this.area.y + 30, Colors.A50, 0);
        }
    }
}