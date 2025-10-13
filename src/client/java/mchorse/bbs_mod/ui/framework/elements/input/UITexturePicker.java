package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.importers.IImportPathProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.URLSourcePack;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.textures.UITexturePainter;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIFileLinkList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIFilteredLinkList;
import mchorse.bbs_mod.ui.framework.elements.input.multilink.UIMultiLinkEditor;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UICopyPasteController;
import mchorse.bbs_mod.ui.utils.presets.UIPresetContextMenu;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.presets.PresetManager;
import mchorse.bbs_mod.utils.resources.FilteredLink;
import mchorse.bbs_mod.utils.resources.LinkUtils;
import mchorse.bbs_mod.utils.resources.MultiLink;
import org.apache.commons.io.IOUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Texture picker GUI
 * 
 * This bad boy allows picking a texture from the file browser, and also 
 * it allows creating multi-skins. See {@link MultiLink} for more information.
 */
public class UITexturePicker extends UIElement implements IImportPathProvider
{
    public UIElement right;
    public UITextbox text;
    public UIIcon close;
    public UIIcon folder;
    public UIIcon pixelEdit;
    public UIFileLinkList picker;

    public UIButton multi;
    public UIFilteredLinkList multiList;
    public UIMultiLinkEditor editor;
    public UITexturePainter pixelEditor;

    public UIElement buttons;
    public UIIcon add;
    public UIIcon remove;
    public UIIcon edit;

    public UIElement options;
    public UIToggle linear;
    public UIToggle mipmap;

    public Consumer<Link> callback;

    public MultiLink multiLink;
    public FilteredLink currentFiltered;
    public Link current;

    private Timer lastTyped = new Timer(1000);
    private Timer lastChecked = new Timer(1000);
    private String typed = "";
    private boolean canBeClosed = true;

    private UICopyPasteController copyPasteController;

    public static UITexturePicker open(UIContext context, Link current, Consumer<Link> callback)
    {
        return open(context.menu.overlay, current, callback);
    }

    public static UITexturePicker open(UIElement parent, Link current, Consumer<Link> callback)
    {
        if (!parent.getChildren(UITexturePicker.class).isEmpty())
        {
            return null;
        }

        UITexturePicker picker = new UITexturePicker(callback);

        picker.full(parent);
        picker.resize();
        picker.fill(current);

        parent.add(picker);

        return picker;
    }

    public static void findAllTextures(UIContext context, Link current, Consumer<String> callback)
    {
        List<String> list = new ArrayList<>();

        for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("")))
        {
            String string = link.toString();

            if (string.endsWith(".png") && !string.contains(":textures/banners/")) list.add(string);
        }

        for (Link link : BBSMod.getProvider().getLinksFromPath(new Link("http", "")))
        {
            String string = link.toString();

            if (string.contains(".png")) list.add(string);
        }

        for (Link link : BBSMod.getProvider().getLinksFromPath(new Link("https", "")))
        {
            String string = link.toString();

            if (string.contains(".png")) list.add(string);
        }

        UIListOverlayPanel panel = new UIListOverlayPanel(UIKeys.TEXTURE_FIND_TITLE, callback);

        panel.addValues(list);
        panel.list.list.sort();

        if (current != null)
        {
            panel.setValue(current.toString());
        }

        UIOverlay.addOverlay(context, panel);
    }

    public UITexturePicker(Consumer<Link> callback)
    {
        super();

        this.copyPasteController = new UICopyPasteController(PresetManager.TEXTURES, "_CopyTexture")
            .supplier(this::copyLink)
            .consumer((data, x, y) -> this.pasteLink(this.parseLink(data)))
            .canCopy(() -> this.current != null);

        this.right = new UIElement();
        this.text = new UITextbox(1000, (str) -> this.selectCurrent(str.isEmpty() ? null : LinkUtils.create(str)));
        this.text.delayedInput().context((menu) ->
        {
            menu.custom(new UIPresetContextMenu(this.copyPasteController)
                .labels(UIKeys.TEXTURE_EDITOR_CONTEXT_COPY, UIKeys.TEXTURE_EDITOR_CONTEXT_PASTE));

            if (this.current != null)
            {
                menu.action(Icons.COPY, UIKeys.TEXTURES_COPY, () -> Window.setClipboard(this.current.toString()));
            }

            menu.action(Icons.DOWNLOAD, UIKeys.TEXTURES_DOWNLOAD, () -> this.download(""));
        });
        this.close = new UIIcon(Icons.CLOSE, (b) -> this.close());
        this.folder = new UIIcon(Icons.FOLDER, (b) -> this.openFolder());
        this.folder.tooltip(UIKeys.TEXTURE_OPEN_FOLDER, Direction.BOTTOM);
        this.pixelEdit = new UIIcon(Icons.EDIT, (b) -> this.togglePixelEditor());
        this.picker = new UIFileLinkList(this::selectCurrent)
        {
            @Override
            public void setPath(Link folder, boolean fastForward)
            {
                super.setPath(folder, fastForward);
                UITexturePicker.this.updateFolderButton();
            }
        };
        this.picker.filter((l) -> l.path.endsWith("/") || l.path.endsWith(".png")).cancelScrollEdge();

        this.linear = new UIToggle(UIKeys.TEXTURES_LINEAR, (b) ->
        {
            Link link = this.current;

            /* Draw preview */
            if (link != null)
            {
                Texture texture = BBSModClient.getTextures().getTexture(link);
                int filter = b.getValue() ? GL11.GL_LINEAR : GL11.GL_NEAREST;

                if (texture.isReallyMipmap())
                {
                    filter = b.getValue() ? GL30.GL_LINEAR_MIPMAP_NEAREST : GL30.GL_NEAREST_MIPMAP_NEAREST;
                }

                texture.bind();
                texture.setFilter(filter);
            }
        });

        this.mipmap = new UIToggle(UIKeys.TEXTURES_MIPMAP, (b) ->
        {
            Link link = this.current;

            /* Draw preview */
            if (link != null)
            {
                Texture texture = BBSModClient.getTextures().getTexture(link);

                texture.bind();

                if (!texture.isMipmap())
                {
                    texture.generateMipmap();
                }

                texture.setParameter(GL30.GL_TEXTURE_MAX_LEVEL, b.getValue() ? 4 : 0);
            }
        });
        this.options = UI.column(5, 10, this.linear, this.mipmap);
        this.options.relative(this).xy(1F, 1F).w(148).anchor(1F, 1F);

        this.multi = new UIButton(UIKeys.TEXTURE_MULTISKIN, (b) -> this.toggleMulti());
        this.multiList = new UIFilteredLinkList((list) -> this.setFilteredLink(list.get(0)));
        this.multiList.sorting();

        this.editor = new UIMultiLinkEditor(this);
        this.editor.setVisible(false);

        this.buttons = new UIElement();
        this.add = new UIIcon(Icons.ADD, (b) -> this.addMulti());
        this.remove = new UIIcon(Icons.REMOVE, (b) -> this.removeMulti());
        this.edit = new UIIcon(Icons.EDIT, (b) -> this.toggleEditor());

        UIElement icons = UI.row(0, this.pixelEdit, this.folder, this.close);

        icons.row().preferred(0);
        icons.relative(this).x(1F, -10).y(10).w(60).h(20).anchorX(1F);

        this.right.full(this);
        this.text.relative(this.multi).x(1F, 20).wTo(icons.area).h(20);
        this.picker.relative(this.right).set(10, 30, 0, 0).w(1, -10).h(1, -30);

        this.multi.relative(this).set(10, 10, 100, 20);
        this.multiList.relative(this).set(10, 35, 100, 0).hTo(this.buttons.getFlex());
        this.editor.relative(this).set(120, 0, 0, 0).w(1F, -120).h(1F);

        this.buttons.relative(this).y(1F, -20).wTo(this.right.area).h(20);
        this.add.relative(this.buttons).set(0, 0, 20, 20);
        this.remove.relative(this.add).set(20, 0, 20, 20);
        this.edit.relative(this.buttons).wh(20, 20).x(1F, -20);

        this.right.add(icons, this.text, this.picker);
        this.buttons.add(this.add, this.remove, this.edit);
        this.add(this.multi, this.multiList, this.right, this.editor, this.buttons, this.options);

        this.callback = callback;

        this.keys().register(Keys.TEXTURE_PICKER_FIND, () ->
        {
            findAllTextures(this.getContext(), this.current, (s) ->
            {
                this.selectCurrent(Link.create(s));
                this.displayCurrent(Link.create(s), true);
            });
        });

        this.fill(null);
        this.markContainer().eventPropagataion(EventPropagation.BLOCK);
    }

    public UITexturePicker cantBeClosed()
    {
        this.close.removeFromParent();
        this.eventPropagataion(EventPropagation.PASS);

        this.canBeClosed = false;

        return this;
    }

    private Link parseLink(MapType map)
    {
        return map == null ? null : LinkUtils.create(map.get("link"));
    }

    private MapType copyLink()
    {
        BaseType base = LinkUtils.toData(this.multiLink != null ? this.multiLink : this.current);

        if (base == null)
        {
            return null;
        }

        MapType map = new MapType();

        map.put("link", base);

        return map;
    }

    private void pasteLink(Link location)
    {
        this.setMulti(location, true);
    }

    private void download(String inputUrl)
    {
        Link path = this.picker.path;

        if (!Link.isAssets(path))
        {
            return;
        }

        UITextbox textboxFilename = new UITextbox();
        UITextbox textboxUrl = new UITextbox(1000, (t) ->
        {
            String newFilename = StringUtils.fileName(t).replaceAll("[^\\w\\d_\\-.]+", "");

            textboxFilename.setText(newFilename);
        });
        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(UIKeys.TEXTURES_DOWNLOAD_TITLE, UIKeys.TEXTURES_DOWNLOAD_DESCRIPTION, (b) ->
        {
            if (b)
            {
                String url = textboxUrl.getText();
                String filename = textboxFilename.getText();
                Link urlLink = path.combine(filename);

                try (InputStream stream = URLSourcePack.downloadImage(Link.create(url)))
                {
                    File file = BBSMod.getProvider().getFile(urlLink);

                    try (OutputStream outputStream = new FileOutputStream(file))
                    {
                        IOUtils.copy(stream, outputStream);
                    }
                }
                catch (Exception e)
                {}
            }
        });

        if (!inputUrl.isEmpty())
        {
            String newFilename = StringUtils.fileName(inputUrl).replaceAll("[^\\w\\d_\\-.]+", "");

            textboxUrl.setText(inputUrl);
            textboxFilename.setText(newFilename);
            textboxFilename.textbox.selectFilename();
        }

        textboxFilename.placeholder(UIKeys.TEXTURES_DOWNLOAD_FILENAME);
        textboxUrl.placeholder(UIKeys.TEXTURES_DOWNLOAD_URL);

        textboxFilename.relative(panel.confirm).y(-5).w(1F).anchorY(1F);
        textboxUrl.relative(textboxFilename).y(-5).w(1F).anchorY(1F);
        panel.confirm.w(1F, -10);
        panel.content.add(textboxFilename, textboxUrl);

        UIContext context = this.getContext();

        UIOverlay.addOverlay(context, panel);
        context.focus(textboxFilename);
    }

    public void close()
    {
        boolean wasVisible = this.hasParent();

        this.editor.close();
        this.removeFromParent();

        if (this.callback != null && wasVisible)
        {
            if (this.multiLink != null)
            {
                this.multiLink.recalculateId();
            }

            this.callback.accept(this.multiLink != null ? this.multiLink : this.current);
        }
    }

    @Override
    public File getImporterPath()
    {
        File target = BBSMod.getProvider().getFile(this.picker.path);

        if (target == null || !target.isDirectory())
        {
            return null;
        }

        return target;
    }

    public void refresh()
    {
        this.picker.update();
        this.updateFolderButton();
    }

    public void openFolder()
    {
        File target = BBSMod.getProvider().getFile(this.picker.path);

        if (target != null && target.isDirectory())
        {
            UIUtils.openFolder(target);
        }
    }

    public void togglePixelEditor()
    {
        if (this.current == null || this.multiLink != null)
        {
            return;
        }

        if (this.pixelEditor == null)
        {
            this.pixelEditor = new UITexturePainter((l) ->
            {
                this.selectCurrent(l);
                this.displayCurrent(l);
            });
            this.pixelEditor.fillTexture(this.current);

            UIIcon close = new UIIcon(Icons.CLOSE, (b) -> this.togglePixelEditor());

            this.pixelEditor.savebar.add(close);
            this.pixelEditor.full(this);
            this.pixelEditor.resize();

            this.add(this.pixelEditor);
        }
        else
        {
            this.pixelEditor.fillTexture(null);
            this.pixelEditor.removeFromParent();
            this.pixelEditor = null;
        }

        this.right.setVisible(this.pixelEditor == null);
        this.multi.setVisible(this.pixelEditor == null);
        this.options.setVisible(this.pixelEditor == null);
    }

    public void updateFolderButton()
    {
        File target = BBSMod.getProvider().getFile(this.picker.path);

        this.folder.setEnabled(target != null && target.isDirectory());
    }

    public void fill(Link link)
    {
        this.setMulti(link, false, true);
    }

    /**
     * Add a {@link Link} to the MultiLink
     */
    private void addMulti()
    {
        FilteredLink filtered = this.currentFiltered.copyFiltered();

        this.multiList.add(filtered);
        this.multiList.setIndex(this.multiList.getList().size() - 1);
        this.setFilteredLink(this.multiList.getCurrent().get(0));
    }

    /**
     * Remove currently selected {@link Link} from multiLink
     */
    private void removeMulti()
    {
        int index = this.multiList.getIndex();

        if (index >= 0 && this.multiList.getList().size() > 1)
        {
            this.multiList.getList().remove(index);
            this.multiList.update();
            this.multiList.setIndex(index - 1);

            if (this.multiList.getIndex() >= 0)
            {
                this.setFilteredLink(this.multiList.getCurrent().get(0));
            }
        }
    }

    private void setFilteredLink(FilteredLink location)
    {
        this.setFilteredLink(location, false);
    }

    private void setFilteredLink(FilteredLink location, boolean scroll)
    {
        this.currentFiltered = location;
        this.displayCurrent(location.path);
        this.editor.setLink(location);
    }

    private void toggleEditor()
    {
        this.editor.toggleVisible();
        this.right.setVisible(!this.editor.isVisible());

        if (this.editor.isVisible())
        {
            this.editor.resetView();
        }
    }

    protected void displayCurrent(Link link)
    {
        this.displayCurrent(link, false);
    }

    /**
     * Display current resource location (it's just for visual, not 
     * logic)
     */
    protected void displayCurrent(Link link, boolean scroll)
    {
        this.current = link;

        this.text.setText(link == null ? "" : link.toString());
        this.text.textbox.moveCursorToStart();

        this.picker.setPath(link == null ? null : link.parent());
        this.picker.setCurrent(link, scroll);

        this.updateOptions();
    }

    /**
     * Select current resource location
     */
    protected void selectCurrent(Link link)
    {
        if (link != null && !BBSModClient.getTextures().has(link))
        {
            return;
        }

        this.current = link;

        if (this.multiLink != null)
        {
            if (link == null && this.multiLink.children.size() == 1)
            {
                this.currentFiltered.path = null;
                this.toggleMulti();
            }
            else
            {
                this.currentFiltered.path = link;
            }
        }
        else if (this.callback != null)
        {
            this.callback.accept(link);
        }

        this.picker.setCurrent(link);
        this.text.setText(link == null ? "" : link.toString());
        this.updateOptions();
    }

    protected void updateOptions()
    {
        Texture texture = BBSModClient.getTextures().getTexture(this.current);

        this.options.setVisible(this.current != null);

        if (texture != null)
        {
            texture.bind();

            this.linear.setValue(texture.isLinear());
            this.mipmap.setValue(texture.isReallyMipmap());
        }
    }

    protected void toggleMulti()
    {
        if (this.multiLink != null)
        {
            this.setMulti(this.multiLink.children.get(0).path, true);
        }
        else if (this.current != null)
        {
            this.setMulti(new MultiLink(this.current.toString()), true);
        }
        else
        {
            UIFileLinkList.FileLink link = this.picker.getCurrentFirst();

            if (link != null)
            {
                this.setMulti(link.link, true);
            }
        }
    }

    protected void setMulti(Link skin, boolean notify)
    {
        this.setMulti(skin, notify, false);
    }

    protected void setMulti(Link skin, boolean notify, boolean scroll)
    {
        if (this.editor.isVisible())
        {
            this.toggleEditor();
        }

        boolean show = skin instanceof MultiLink;

        if (show)
        {
            this.multiLink = (MultiLink) ((MultiLink) skin).copy();
            this.setFilteredLink(this.multiLink.children.get(0), scroll);

            this.multiList.setIndex(this.multiLink.children.isEmpty() ? -1 : 0);
            this.multiList.setList(this.multiLink.children);

            if (this.current != null)
            {
                this.multiList.setIndex(0);
            }

            this.right.x(120).w(1F, -120);
        }
        else
        {
            this.multiLink = null;

            this.right.x(0).w(1F);
            this.displayCurrent(skin, scroll);
        }

        if (notify)
        {
            if (show && this.callback != null)
            {
                this.multiLink.recalculateId();
                this.callback.accept(skin);
            }
            else
            {
                this.selectCurrent(skin);
            }
        }

        this.multiList.setVisible(show);
        this.buttons.setVisible(show);

        this.resize();
        this.updateFolderButton();
    }

    @Override
    public boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ENTER))
        {
            UIFileLinkList.FileLink link = this.picker.getCurrentFirst();

            if (link != null && link.folder)
            {
                this.picker.setPath(link.link);
            }
            else if (link != null)
            {
                this.selectCurrent(link.link);
            }

            this.typed = "";

            return true;
        }
        else if (context.isHeld(GLFW.GLFW_KEY_UP))
        {
            return this.moveCurrent(-1, Window.isShiftPressed());
        }
        else if (context.isHeld(GLFW.GLFW_KEY_DOWN))
        {
            return this.moveCurrent(1, Window.isShiftPressed());
        }
        else if (context.isPressed(GLFW.GLFW_KEY_ESCAPE) && this.canBeClosed)
        {
            this.close();

            return true;
        }
        else if (context.isPressed(Keys.PASTE.getMainKey()) && Window.isCtrlPressed())
        {
            this.download(Window.getClipboard());

            return true;
        }

        return super.subKeyPressed(context);
    }

    protected boolean moveCurrent(int factor, boolean top)
    {
        int index = this.picker.getIndex() + factor;
        int length = this.picker.getList().size();

        if (index < 0) index = length - 1;
        else if (index >= length) index = 0;

        if (top) index = factor > 0 ? length - 1 : 0;

        this.picker.setIndex(index);
        this.picker.scroll.scrollIntoView(index * this.picker.scroll.scrollItemSize);
        this.typed = "";

        return true;
    }

    @Override
    public boolean subTextInput(UIContext context)
    {
        return this.pickByTyping(context, context.getInputCharacter());
    }

    protected boolean pickByTyping(UIContext context, char inputChar)
    {
        if (this.lastTyped.checkReset())
        {
            this.typed = "";
        }

        this.typed += Character.toString(inputChar);
        this.lastTyped.mark();

        for (UIFileLinkList.FileLink entry : this.picker.getList())
        {
            String name = entry.title;

            if (name.startsWith(this.typed))
            {
                this.picker.setCurrentScroll(entry);

                return true;
            }
        }

        return true;
    }

    @Override
    public void render(UIContext context)
    {
        /* Refresh the list */
        if (this.lastChecked.checkRepeat())
        {
            File file = BBSMod.getProvider().getFile(this.picker.path);
            int scroll = (int) this.picker.scroll.getScroll();

            if (file != null)
            {
                UIFileLinkList.FileLink selected = this.picker.getCurrentFirst();

                this.picker.setPath(this.picker.path, false);

                if (selected != null)
                {
                    this.picker.setCurrent(selected.link);
                }
            }

            this.picker.scroll.setScroll(scroll);
        }

        /* Draw the background */
        context.batcher.gradientVBox(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50, Colors.A100);

        if (this.multiList.isVisible())
        {
            context.batcher.box(this.area.x, this.area.y, this.area.x + 120, this.area.ey(), 0xff181818);
            context.batcher.box(this.area.x, this.area.y, this.area.x + 120, this.area.y + 30, Colors.A25);
            context.batcher.gradientVBox(this.area.x, this.area.ey() - 20, this.buttons.area.ex(), this.area.ey(), 0, Colors.A50);
        }

        if (this.editor.isVisible())
        {
            this.edit.area.render(context.batcher, Colors.A50 | BBSSettings.primaryColor.get());
        }

        super.render(context);

        /* Draw the overlays */
        if (this.right.isVisible())
        {
            FontRenderer font = context.batcher.getFont();

            if (this.picker.getList().isEmpty())
            {
                String label = UIKeys.TEXTURE_NO_DATA.get();
                int w = font.getWidth(label);

                context.batcher.text(label, this.picker.area.mx(w), this.picker.area.my() - 8);
            }

            if (!this.lastTyped.check() && this.lastTyped.enabled)
            {
                int w = font.getWidth(this.typed);
                int x = this.text.area.x;
                int y = this.text.area.ey();

                context.batcher.box(x, y, x + w + 4, y + 4 + font.getHeight(), Colors.A50 | BBSSettings.primaryColor.get());
                context.batcher.textShadow(this.typed, x + 2, y + 2);
            }

            Link link = this.current;

            /* Draw preview */
            if (link != null)
            {
                Texture texture = context.render.getTextures().getTexture(link);

                int w = texture.width;
                int h = texture.height;

                int x = this.area.ex();
                int y = this.options.area.y;
                int fw = w;
                int fh = h;

                if (fw > 128 || fh > 128)
                {
                    fw = fh = 128;

                    if (w > h)
                    {
                        fh = (int) ((h / (float) w) * fw);
                    }
                    else if (h > w)
                    {
                        fw = (int) ((w / (float) h) * fh);
                    }
                }

                x -= fw + 10;
                y -= fh;

                context.batcher.iconArea(Icons.CHECKBOARD, x, y, fw, fh);
                context.batcher.fullTexturedBox(texture, x, y, fw, fh);
            }
        }
    }
}