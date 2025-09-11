package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.textures.undo.PixelsUndo;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageFolderOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.PNGEncoder;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.resources.Pixels;
import org.joml.Vector2i;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class UITextureEditor extends UIPixelsEditor
{
    public UIIcon save;
    public UIIcon resize;
    public UIIcon extract;

    private Link texture;
    private boolean dirty;

    private Consumer<Link> saveCallback;

    public UITextureEditor()
    {
        super();

        this.save = new UIIcon(() -> this.dirty ? Icons.SAVE : Icons.SAVED, (b) -> this.saveTexture());
        this.resize = new UIIcon(Icons.FULLSCREEN, (b) ->
        {
            Pixels pixels = this.getPixels();
            UIResizeTextureOverlayPanel overlayPanel = new UIResizeTextureOverlayPanel(pixels.width, pixels.height, (size) ->
            {
                boolean editing = this.isEditing();
                Pixels newPixels = Pixels.fromSize(
                    MathUtils.clamp(size.x, 1, 4096),
                    MathUtils.clamp(size.y, 1, 4096)
                );

                newPixels.draw(pixels, 0, 0, newPixels.width, newPixels.height);
                pixels.delete();

                this.fillPixels(newPixels);
                this.setDirty(false);
                this.setEditing(editing);
            });

            UIOverlay.addOverlay(this.getContext(), overlayPanel);
        });
        this.resize.tooltip(UIKeys.TEXTURES_RESIZE);
        this.extract = new UIIcon(Icons.UPLOAD, (b) ->
        {
            UIOverlay.addOverlay(this.getContext(), new UITextureExtractOverlayPanel(this.getTexture(), this.getPixels()), 200, 231);
        });
        this.extract.tooltip(UIKeys.TEXTURES_EXTRACT_FRAMES_TITLE);

        this.toolbar.add(this.resize, this.extract, this.save);
    }

    public UITextureEditor saveCallback(Consumer<Link> saveCallback)
    {
        this.saveCallback = saveCallback;

        return this;
    }

    public Link getTexture()
    {
        return this.texture;
    }

    public boolean isDirty()
    {
        return this.dirty;
    }

    public void dirty()
    {
        this.setDirty(true);
    }

    public void setDirty(boolean dirty)
    {
        this.dirty = dirty;
    }

    @Override
    protected void wasChanged()
    {
        this.dirty();
    }

    public void fillColor(Vector2i pixel, Color color, boolean colorReplace)
    {
        PixelsUndo pixelsUndo = new PixelsUndo();
        Pixels pixels = this.getPixels();
        Color target = pixels.getColor(pixel.x, pixel.y);

        if (target == null)
        {
            return;
        }

        target = target.copy();

        if (colorReplace)
        {
            for (int x = 0; x < pixels.width; x++)
            {
                for (int y = 0; y < pixels.height; y++)
                {
                    Color current = pixels.getColor(x, y);

                    if (current.getARGBColor() == target.getARGBColor())
                    {
                        pixelsUndo.setColor(pixels, x, y, color);
                    }
                }
            }
        }
        else
        {
            this.floodFill(new HashSet<>(), pixelsUndo, pixels, pixel.x, pixel.y, target.getARGBColor(), color.getARGBColor());
        }

        this.undoManager.pushUndo(pixelsUndo);
        this.updateTexture();
    }

    private void floodFill(Set<Vector2i> set, PixelsUndo undo, Pixels pixels, int x, int y, int targetColor, int replacementColor)
    {
        if (x < 0 || y < 0 || x >= pixels.width || y >= pixels.height)
        {
            return;
        }

        int current = pixels.getColor(x, y).getARGBColor();

        if (current != targetColor)
        {
            return;
        }

        Vector2i v = new Vector2i(x, y);

        if (set.contains(v))
        {
            return;
        }

        set.add(v);
        undo.setColor(pixels, x, y, new Color().set(replacementColor, true));

        this.floodFill(set, undo, pixels, x + 1, y, targetColor, replacementColor);
        this.floodFill(set, undo, pixels, x - 1, y, targetColor, replacementColor);
        this.floodFill(set, undo, pixels, x, y + 1, targetColor, replacementColor);
        this.floodFill(set, undo, pixels, x, y - 1, targetColor, replacementColor);
    }

    private void saveTexture()
    {
        UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
            UIKeys.GENERAL_EXPORT,
            UIKeys.TEXTURES_SAVE,
            this::saveTexture
        );

        UIOverlay.addOverlay(this.getContext(), panel);

        String text = this.texture.toString();

        panel.text.setText(text);
        panel.text.textbox.selectFilename();
    }

    private void saveTexture(String path)
    {
        Link link = Link.create(path);

        if (!Link.isAssets(link) || !link.path.endsWith(".png"))
        {
            this.getContext().notifyError(UIKeys.TEXTURES_SAVE_WRONG_PATH);

            return;
        }

        File file = BBSMod.getAssetsPath(link.path);

        if (path.contains("/"))
        {
            file.getParentFile().mkdirs();
        }

        Pixels pixels = this.getPixels();

        try
        {
            PNGEncoder.writeToFile(pixels, file);
            UIMessageFolderOverlayPanel panel = new UIMessageFolderOverlayPanel(
                UIKeys.TEXTURES_EXPORT_OVERLAY_TITLE,
                UIKeys.TEXTURES_EXPORT_OVERLAY_SUCCESS.format(file.getName()),
                file.getParentFile()
            );

            panel.folder.tooltip(UIKeys.TEXTURES_EXPORT_OVERLAY_OPEN_FOLDER, Direction.LEFT);

            UIOverlay.addOverlay(this.getContext(), panel);

            this.setDirty(false);

            if (this.saveCallback != null)
            {
                this.saveCallback.accept(link);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();

            this.getContext().notifyError(UIKeys.TEXTURES_EXPORT_OVERLAY_ERROR.format(file.getName()));
        }
    }

    public void fillTexture(Link texture)
    {
        if (this.getPixels() != null)
        {
            this.getPixels().delete();
        }

        this.texture = texture;

        if (texture != null)
        {
            Texture t = BBSModClient.getTextures().getTexture(texture);

            this.fillPixels(Texture.pixelsFromTexture(t));
            this.setDirty(false);
        }
    }

    @Override
    protected Texture getRenderTexture(UIContext context)
    {
        return this.isEditing() ? super.getRenderTexture(context) : context.render.getTextures().getTexture(this.texture);
    }
}