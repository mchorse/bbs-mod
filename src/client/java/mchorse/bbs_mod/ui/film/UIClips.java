package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.camera.clips.converters.IClipConverter;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.clips.renderer.IUIClipRenderer;
import mchorse.bbs_mod.ui.film.clips.renderer.UIClipRenderers;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Scale;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UICopyPasteController;
import mchorse.bbs_mod.ui.utils.presets.UIPresetContextMenu;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.factory.IFactory;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.presets.PresetManager;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class UIClips extends UIElement
{
    /* Constants */
    public static final IKey KEYS_CATEGORY = UIKeys.CAMERA_EDITOR_KEYS_CLIPS_TITLE;

    private static final int MARGIN = 10;
    private static final int LAYER_HEIGHT = 20;

    private static final Area CLIP_AREA = new Area();

    /* Main objects */
    private IUIClipsDelegate delegate;
    private Clips clips;
    private IFactory<Clip, ClipFactoryData> factory;

    /* Navigation */
    public Scale scale = new Scale(this.area, ScrollDirection.HORIZONTAL);
    public Scroll vertical = new Scroll(new Area());

    private boolean canGrab;
    private boolean grabbing;
    private boolean scrubbing;
    private boolean scrolling;
    private int lastX;
    private int lastY;
    private int initialX;
    private int initialY;
    private int grabMode;

    /* Looping */
    public int loopMin = 0;
    public int loopMax = 0;
    private int selectingLoop = -1;

    /* Selection */
    private boolean selecting;
    private List<Integer> selection = new ArrayList<>();

    /* Embedded view */
    private UIIcon embeddedClose;
    private UIElement embedded;

    private Vector3i addPreview;
    private int layers;

    private UIClipRenderers renderers = new UIClipRenderers();

    private List<Clip> grabbedClips = Collections.emptyList();
    private List<Clip> otherClips = Collections.emptyList();
    private Set<Integer> snappingPoints = new HashSet<>();
    private List<Vector3i> grabbedData = new ArrayList<>();

    private UICopyPasteController copyPasteController;

    /**
     * Render cursor that displays the full duration of the camera work,
     * and also current tick within the camera work.
     */
    public static void renderCursor(UIContext context, String label, Area area, int x)
    {
        /* Draw the marker */
        FontRenderer font = context.batcher.getFont();
        int width = font.getWidth(label) + 3;

        context.batcher.box(x, area.y, x + 2, area.ey(), Colors.CURSOR);

        /* Move the tick line left, so it won't overflow the timeline */
        if (x + 2 + width > area.ex())
        {
            x -= width + 1;
        }

        /* Draw the tick label */
        context.batcher.textCard(label, x + 4, area.ey() - 2 - font.getHeight(), Colors.WHITE, Colors.setA(Colors.CURSOR, 0.75F), 2);
    }

    public UIClips(IUIClipsDelegate delegate, IFactory<Clip, ClipFactoryData> factory)
    {
        super();

        this.copyPasteController = new UICopyPasteController(PresetManager.CLIPS, "_CopyClips")
            .supplier(this::copyClips)
            .consumer(this::pasteClips)
            .canCopy(() -> this.delegate.getClip() != null);

        this.delegate = delegate;
        this.factory = factory;

        this.embeddedClose = new UIIcon(Icons.CLOSE, (b) -> this.embedView(null));
        this.embeddedClose.relative(this);

        this.context((menu) ->
        {
            UIContext context = this.getContext();
            int mouseX = context.mouseX;
            int mouseY = context.mouseY;
            boolean hasSelected = this.delegate.getClip() != null;

            menu.custom(new UIPresetContextMenu(this.copyPasteController, mouseX, mouseY)
                .labels(UIKeys.CAMERA_TIMELINE_CONTEXT_COPY, UIKeys.CAMERA_TIMELINE_CONTEXT_PASTE));

            if (this.fromLayerY(mouseY) < 0)
            {
                return;
            }

            menu.action(Icons.ADD, UIKeys.CAMERA_TIMELINE_CONTEXT_ADD, () -> this.showAdds(mouseX, mouseY));

            if (hasSelected)
            {
                this.addConverters(menu, context);
                menu.action(Icons.CUT, UIKeys.CAMERA_TIMELINE_CONTEXT_CUT, this::cut);
                menu.action(Icons.MOVE_TO, UIKeys.CAMERA_TIMELINE_CONTEXT_SHIFT, this::shiftToCursor);
                menu.action(Icons.SHIFT_TO, UIKeys.CAMERA_TIMELINE_CONTEXT_SHIFT_DURATION, this::shiftDurationToCursor);
            }

            menu.action(Icons.EXCHANGE, UIKeys.CAMERA_TIMELINE_CONTEXT_REORGANIZE, () -> this.clips.sortLayers());

            if (hasSelected)
            {
                menu.action(Icons.REMOVE, UIKeys.CAMERA_TIMELINE_CONTEXT_REMOVE_CLIPS, Colors.NEGATIVE, this::removeSelected);
            }
        });

        Supplier<Boolean> canUseKeybinds = () -> this.delegate.canUseKeybinds() && !this.hasEmbeddedView();
        Supplier<Boolean> canUseKeybindsSelected = () -> this.delegate.getClip() != null && canUseKeybinds.get();

        this.keys().register(Keys.KEYFRAMES_MAXIMIZE, this::resetView).category(KEYS_CATEGORY);
        this.keys().register(Keys.DESELECT, () -> this.pickClip(null)).category(KEYS_CATEGORY).active(canUseKeybindsSelected);
        this.keys().register(Keys.ADD_ON_TOP, this::showAddsOnTop).category(KEYS_CATEGORY).active(canUseKeybindsSelected);
        this.keys().register(Keys.ADD_AT_CURSOR, this::showAddsAtCursor).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.ADD_AT_TICK, this::showAddsAtTick).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.COPY, () ->
        {
            if (this.copyPasteController.copy()) UIUtils.playClick();
        }).category(KEYS_CATEGORY).active(canUseKeybindsSelected);
        this.keys().register(Keys.PASTE, () ->
        {
            UIContext context = this.getContext();

            if (this.copyPasteController.paste(context.mouseX, context.mouseY)) UIUtils.playClick();
        }).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.PRESETS, () ->
        {
            UIContext context = this.getContext();

            if (this.copyPasteController.canPreviewPresets())
            {
                this.copyPasteController.openPresets(context, context.mouseX, context.mouseY);
                UIUtils.playClick();
            }
        }).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_CUT, this::cut).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_SHIFT, this::shiftToCursor).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_DURATION, this::shiftDurationToCursor).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.DELETE, this::removeSelected).label(UIKeys.CAMERA_TIMELINE_CONTEXT_REMOVE_CLIPS).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_ENABLE, this::toggleEnabled).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_SELECT_AFTER, this::selectAfter).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.CLIP_SELECT_BEFORE, this::selectBefore).category(KEYS_CATEGORY).active(canUseKeybinds);
        this.keys().register(Keys.FADE_IN, () ->
        {
            Clip clip = this.delegate.getClip();
            int tick = Math.max(0, this.delegate.getCursor() - clip.tick.get());

            clip.envelope.fadeIn.set((float) tick);
            this.delegate.fillData();
        }).category(KEYS_CATEGORY).active(canUseKeybindsSelected);
        this.keys().register(Keys.FADE_OUT, () ->
        {
            Clip clip = this.delegate.getClip();
            int tick = Math.max(0, clip.tick.get() + clip.duration.get() - this.delegate.getCursor());

            clip.envelope.fadeOut.set((float) tick);
            this.delegate.fillData();
        }).category(KEYS_CATEGORY).active(canUseKeybindsSelected);
    }

    public UIClipRenderers getRenderers()
    {
        return this.renderers;
    }

    public IFactory<Clip, ClipFactoryData> getFactory()
    {
        return this.factory;
    }

    /* Tools */

    private void showAdds(int mouseX, int mouseY)
    {
        UIContext context = this.getContext();

        context.replaceContextMenu((add) ->
        {
            add.action(Icons.CURSOR, UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_AT_CURSOR, () -> this.showAddsAtCursor(context, mouseX, mouseY));
            add.action(Icons.SHIFT_TO, UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_AT_TICK, () -> this.showAddsAtTick(context, mouseX, mouseY));

            if (this.delegate.getClip() != null)
            {
                add.action(Icons.UPLOAD, UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_ON_TOP, this::showAddsOnTop);
            }

            if (this.factory.getKeys().contains(Link.bbs("keyframe")))
            {
                add.action(Icons.EDITOR, UIKeys.CAMERA_TIMELINE_CONTEXT_FROM_PLAYER_RECORDING, () -> this.fromReplay(mouseX, mouseY));
            }
        });
    }

    private void showAddsAtCursor()
    {
        UIContext context = this.getContext();

        this.showAddsAtCursor(context, context.mouseX, context.mouseY);
    }

    private void showAddsAtCursor(UIContext context, int mouseX, int mouseY)
    {
        this.showAddClips(context, this.checkSize(this.fromGraphX(mouseX), this.fromLayerY(mouseY), BBSSettings.getDefaultDuration()));
    }

    private void showAddsAtTick()
    {
        UIContext context = this.getContext();

        this.showAddsAtTick(context, context.mouseX, context.mouseY);
    }

    private void showAddsAtTick(UIContext context, int mouseX, int mouseY)
    {
        this.showAddClips(context, this.checkSize(this.delegate.getCursor(), this.fromLayerY(mouseY), BBSSettings.getDefaultDuration()));
    }

    private void showAddsOnTop()
    {
        Clip clip = this.delegate.getClip();
        UIContext context = this.getContext();

        this.showAddClips(context, this.checkSize(clip.tick.get(), clip.layer.get() + 1, clip.duration.get()));
    }

    private Vector3i checkSize(int tick, int layer, int duration)
    {
        for (Clip clip : this.clips.get())
        {
            if (clip.layer.get() == layer)
            {
                int l1 = clip.tick.get();
                int r1 = l1 + clip.duration.get();
                int l2 = tick;
                int r2 = l2 + duration;

                if (MathUtils.isInside(l1, r1, l2, r2))
                {
                    if (l1 < r2 && r2 <= r1)
                    {
                        int diff = r2 - l1;

                        duration -= diff;
                    }
                    else if (l2 < r1 && r1 <= r2)
                    {
                        int diff = r1 - l2;

                        tick = r1;
                        duration -= diff;
                    }
                }
            }
        }

        if (duration <= 0)
        {
            return null;
        }

        return new Vector3i(tick, layer, duration);
    }

    private void showAddClips(UIContext context, Vector3i preview)
    {
        if (preview == null)
        {
            this.addPreview = null;

            this.getContext().notifyError(UIKeys.CAMERA_TIMELINE_CANT_FIT_NOTIFICATION);

            return;
        }

        context.replaceContextMenu((add) ->
        {
            add.autoKeys(UIKeys.CAMERA_TIMELINE_KEYS_CLIPS);

            for (Link type : this.factory.getKeys())
            {
                IKey typeKey = UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_CLIP_TYPE.format(UIKeys.C_CLIP.get(type));
                ClipFactoryData data = this.factory.getData(type);

                add.action(data.icon, typeKey, data.color, () -> this.addClip(type, preview.x, preview.y, preview.z));
            }

            add.onClose((m) -> this.addPreview = null);
        });

        this.addPreview = preview;
    }

    private void addClip(Link type, int tick, int layer, int duration)
    {
        Clip clip = this.factory.create(type);

        if (clip instanceof CameraClip)
        {
            ((CameraClip) clip).fromCamera(this.delegate.getCamera());
        }

        this.addClip(clip, tick, layer, duration);
    }

    /**
     * Add a new clip of given type at mouse coordinates.
     */
    private void addClip(Clip clip, int tick, int layer, int duration)
    {
        clip.layer.set(layer);
        clip.tick.set(tick);
        clip.duration.set(duration);

        this.clips.addClip(clip);
        this.pickClip(clip);
    }

    private MapType copyClips()
    {
        MapType data = new MapType();
        ListType clips = new ListType();

        data.put("clips", clips);

        for (Clip clip : this.getClipsFromSelection())
        {
            clips.add(this.factory.toData(clip));
        }

        return data;
    }

    private void pasteClips(MapType data, int mouseX, int mouseY)
    {
        this.pasteClips(data, this.fromGraphX(mouseX));
    }

    /**
     * Paste given clip data to timeline.
     */
    private void pasteClips(MapType data, int tick)
    {
        this.clearSelection();

        ListType clipsList = data.getList("clips");
        List<Clip> newClips = new ArrayList<>();
        int min = Integer.MAX_VALUE;

        try
        {
            for (BaseType type : clipsList)
            {
                MapType typeMap = type.asMap();
                Clip clip = this.factory.fromData(typeMap);

                min = Math.min(min, clip.tick.get());

                newClips.add(clip);
            }

            for (Clip clip : newClips)
            {
                clip.tick.set(tick + (clip.tick.get() - min));
                clip.layer.set(this.clips.findFreeLayer(clip));
                this.clips.addClip(clip);
                this.addSelected(clip);
            }

            this.pickLastSelectedClip();
        }
        catch (Exception e)
        {
            e.printStackTrace();

            this.getContext().notifyError(UIKeys.CAMERA_TIMELINE_INCOMPATIBLE_PASTE);
        }
    }

    /**
     * Breakdown currently selected clip into two.
     */
    private void cut()
    {
        List<Clip> selectedClips = this.isSelecting() ? this.getClipsFromSelection() : new ArrayList<>(this.clips.get());
        Clip original = this.delegate.getClip();
        int offset = this.delegate.getCursor();

        this.clips.preNotify();

        for (Clip clip : selectedClips)
        {
            if (!clip.isInside(offset))
            {
                continue;
            }

            Clip copy = clip.breakDown(offset - clip.tick.get());

            if (copy != null)
            {
                clip.duration.set(clip.duration.get() - copy.duration.get());
                copy.tick.set(copy.tick.get() + clip.duration.get());
                this.clips.addClip(copy);
                this.addSelected(copy);
            }
        }

        this.clips.postNotify();

        this.addSelected(original);
    }

    /**
     * Add available converters to context menu.
     */
    private void addConverters(ContextMenuManager menu, UIContext context)
    {
        ClipFactoryData data = this.factory.getData(this.delegate.getClip());
        Collection<Link> converters = data.converters.keySet();

        if (converters.isEmpty())
        {
            return;
        }

        menu.action(Icons.REFRESH, UIKeys.CAMERA_TIMELINE_CONTEXT_CONVERT, () ->
        {
            context.replaceContextMenu((add) ->
            {
                for (Link type : converters)
                {
                    IKey label = UIKeys.CAMERA_TIMELINE_CONTEXT_CONVERT_TO.format(UIKeys.C_CLIP.get(type));

                    add.action(Icons.REFRESH, label, this.factory.getData(type).color, () -> this.convertTo(type));
                }
            });
        });
    }

    /**
     * Convert currently editing camera clip into given type.
     */
    private void convertTo(Link type)
    {
        List<Clip> clipsFromSelection = this.getClipsFromSelection();

        if (clipsFromSelection.isEmpty())
        {
            return;
        }

        for (Clip clip : clipsFromSelection)
        {
            if (clip.getClass() != clipsFromSelection.get(0).getClass())
            {
                return;
            }
        }

        ClipFactoryData data = this.factory.getData(clipsFromSelection.get(clipsFromSelection.size() - 1));
        IClipConverter converter = data.converters.get(type);
        List<Clip> newClips = new ArrayList<>();

        for (Clip clip : clipsFromSelection)
        {
            Clip converted = converter.convert(clip);

            if (converted == null)
            {
                continue;
            }

            this.clips.remove(clip);
            this.clips.addClip(converted);
            newClips.add(converted);
        }

        if (newClips.isEmpty())
        {
            return;
        }

        this.clearSelection();

        for (Clip newClip : newClips)
        {
            this.addSelected(newClip);
        }

        this.pickLastSelectedClip();
    }

    private void fromReplay(int mouseX, int mouseY)
    {
        Film film = this.delegate.getFilm();

        this.getContext().replaceContextMenu((menu) ->
        {
            for (Replay replay : film.replays.getList())
            {
                Form form = replay.form.get();

                menu.action(Icons.EDITOR, IKey.constant(form == null ? "-" : form.getFormIdOrName()), () ->
                {
                    KeyframeClip clip = new KeyframeClip();

                    clip.fov.insert(0, 50D);

                    clip.x.copyKeyframes(replay.keyframes.x);
                    clip.y.copyKeyframes(replay.keyframes.y);
                    clip.z.copyKeyframes(replay.keyframes.z);

                    clip.yaw.copyKeyframes(replay.keyframes.yaw);
                    clip.pitch.copyKeyframes(replay.keyframes.pitch);

                    for (Keyframe<Double> keyframe : clip.yaw.getKeyframes())
                    {
                        keyframe.setValue(180D + keyframe.getValue());
                        // keyframe.setLy(180F + keyframe.getLy());
                        // keyframe.setRy(180F + keyframe.getRy());
                    }

                    double size = Math.max(
                        clip.x.getLength(),
                        Math.max(
                            clip.y.getLength(),
                            Math.max(
                                clip.z.getLength(),
                                Math.max(clip.yaw.getLength(), clip.pitch.getLength())
                            )
                        )
                    );

                    this.addClip(clip, this.fromGraphX(mouseX), this.fromLayerY(mouseY), (int) size);
                });
            }
        });
    }

    /**
     * Move clips to cursor.
     */
    private void shiftToCursor()
    {
        List<Clip> clips = this.getClipsFromSelection();

        if (clips.isEmpty())
        {
            return;
        }

        int min = Integer.MAX_VALUE;

        for (Clip clip : clips)
        {
            min = Math.min(min, clip.tick.get());
        }

        int diff = this.delegate.getCursor() - min;

        for (Clip clip : clips)
        {
            clip.tick.set(clip.tick.get() + diff);
        }

        this.delegate.fillData();
    }

    /**
     * Move duration of currently selected clip(s) to cursor.
     */
    private void shiftDurationToCursor()
    {
        List<Clip> clips = this.getClipsFromSelection();

        if (clips.isEmpty())
        {
            return;
        }

        for (Clip clip : clips)
        {
            int offset = clip.tick.get();

            if (this.delegate.getCursor() > offset)
            {
                clip.duration.set(this.delegate.getCursor() - offset);
            }
            else if (this.delegate.getCursor() < offset + clip.duration.get())
            {
                clip.tick.set(this.delegate.getCursor());
                clip.duration.set(clip.duration.get() + offset - this.delegate.getCursor());
            }
        }

        this.delegate.fillData();
    }

    /**
     * Remove currently selected camera clip(s) from the camera work.
     */
    private void removeSelected()
    {
        List<Clip> selectedClips = this.getClipsFromSelection();

        if (selectedClips.isEmpty())
        {
            return;
        }

        for (Clip clip : selectedClips)
        {
            this.clips.remove(clip);
        }

        this.pickClip(null);
    }

    /**
     * Toggle enabled option of all selected clips
     */
    private void toggleEnabled()
    {
        List<Clip> clips = this.getClipsFromSelection();

        if (clips.isEmpty())
        {
            return;
        }

        for (Clip clip : clips)
        {
            clip.enabled.set(!clip.enabled.get());
        }

        this.delegate.fillData();
    }

    private void selectBefore()
    {
        int i = 0;

        this.clearSelection();

        for (Clip clip : this.clips.get())
        {
            if (clip.tick.get() < this.delegate.getCursor())
            {
                this.selection.add(i);
            }

            i += 1;
        }

        this.delegate.pickClip(this.selection.isEmpty() ? null : this.clips.get(this.selection.get(0)));
    }

    private void selectAfter()
    {
        int i = 0;

        this.clearSelection();

        for (Clip clip : this.clips.get())
        {
            if (clip.tick.get() + clip.duration.get() > this.delegate.getCursor())
            {
                this.selection.add(i);
            }

            i += 1;
        }

        this.delegate.pickClip(this.selection.isEmpty() ? null : this.clips.get(this.selection.get(0)));
    }

    /* Selection */

    private boolean isSelecting()
    {
        return !this.selection.isEmpty();
    }

    public List<Integer> getSelection()
    {
        return Collections.unmodifiableList(this.selection);
    }

    public List<Clip> getClipsFromSelection()
    {
        List<Clip> clips = new ArrayList<>();

        for (int index : this.selection)
        {
            Clip clip = this.clips.get(index);

            if (clip != null)
            {
                clips.add(clip);
            }
        }

        return clips;
    }

    public Clip getLastSelectedClip()
    {
        if (!this.isSelecting())
        {
            return null;
        }

        return this.clips.get(this.selection.get(this.selection.size() - 1));
    }

    public void setSelection(List<Integer> selection)
    {
        this.clearSelection();
        this.selection.addAll(selection);
    }

    public void clearSelection()
    {
        this.selection.clear();
    }

    public void pickClip(Clip clip)
    {
        this.setSelected(clip);
        this.delegate.pickClip(clip);
    }

    private void pickLastSelectedClip()
    {
        this.delegate.pickClip(this.getLastSelectedClip());
    }

    public void setSelected(Clip clip)
    {
        this.clearSelection();
        this.addSelected(clip);
    }

    public void addSelected(Clip clip)
    {
        int index = this.clips.getIndex(clip);

        if (index >= 0)
        {
            this.selection.remove((Integer) index);
            this.selection.add(index);
        }
    }

    public boolean hasSelected(int clip)
    {
        return this.selection.contains(clip);
    }

    /* Getters and setters */

    public Clips getClips()
    {
        return this.clips;
    }

    public void setClips(Clips clips)
    {
        this.clips = clips;
        this.addPreview = null;

        this.vertical.scrollToEnd();
        this.vertical.updateTarget();
        this.clearSelection();
        this.embedView(null);

        this.resetView();
    }

    private void resetView()
    {
        this.scale.anchor(0F);

        if (clips != null)
        {
            int duration = clips.calculateDuration();

            if (duration > 0)
            {
                this.scale.view(0, duration);
            }
            else
            {
                this.scale.set(0, 1);
            }
        }
    }

    public int fromLayerY(int mouseY)
    {
        int bottom = this.area.ey() - MARGIN;

        if (mouseY > bottom)
        {
            return -1;
        }

        mouseY -= this.getScroll();

        return (bottom - mouseY) / LAYER_HEIGHT;
    }

    public int toLayerY(int layer)
    {
        int h = LAYER_HEIGHT;

        return this.area.ey() - MARGIN - (layer + 1) * h + this.getScroll();
    }

    private int getScroll()
    {
        if (this.vertical.scrollSize < this.vertical.area.h)
        {
            return 0;
        }

        return this.vertical.scrollSize - this.vertical.area.h - (int) this.vertical.getScroll();
    }

    public void updateLayers()
    {
        this.layers = 20;

        for (Clip clip : this.clips.get())
        {
            this.layers = Math.max(this.layers, clip.layer.get() + 1);
        }
    }

    public int fromGraphX(int mouseX)
    {
        return (int) Math.round(this.scale.from(mouseX));
    }

    public int toGraphX(int value)
    {
        return (int) (this.scale.to(value));
    }

    public void setLoopMin()
    {
        this.loopMin = this.delegate.getCursor();
    }

    public void setLoopMax()
    {
        this.loopMax = this.delegate.getCursor();
    }

    private void verifyLoopMinMax()
    {
        int min = this.loopMin;
        int max = this.loopMax;

        this.loopMin = Math.min(min, max);
        this.loopMax = Math.max(min, max);
    }

    /* Embedded view */

    public boolean hasEmbeddedView()
    {
        return this.embedded != null;
    }

    public void embedView(UIElement element)
    {
        this.embeddedClose.removeFromParent();

        if (this.embedded != null)
        {
            this.embedded.removeFromParent();
        }

        this.embedded = element;

        if (this.embedded != null)
        {
            this.embedded.resetFlex().full(this);

            this.prepend(this.embedded);
            this.add(this.embeddedClose);
            this.embedded.resize();
            this.embeddedClose.resize();
        }
    }

    /* Handling user input */

    @Override
    protected void afterResizeApplied()
    {
        super.afterResizeApplied();

        this.vertical.area.copy(this.area);
        this.vertical.area.h -= MARGIN;
    }

    public void updateScrollSize()
    {
        this.updateLayers();

        this.vertical.scrollSize = this.clips == null ? 0 : this.layers * LAYER_HEIGHT;
        this.vertical.clamp();
    }

    private void setMouse(int x, int y)
    {
        this.lastX = this.initialX = x;
        this.lastY = this.initialY = y;
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.vertical.mouseClicked(context))
        {
            return true;
        }

        if (this.area.isInside(context) && !this.hasEmbeddedView())
        {
            int mouseX = context.mouseX;
            int mouseY = context.mouseY;
            boolean ctrl = Window.isCtrlPressed();
            boolean shift = Window.isShiftPressed();
            boolean alt = Window.isAltPressed();

            if (context.mouseButton == 0 && this.handleLeftClick(context, mouseX, mouseY, ctrl, shift, alt))
            {
                return true;
            }
            else if (context.mouseButton == 1 && this.handleRightClick(mouseX, mouseY, ctrl, shift, alt))
            {
                return true;
            }
            else if (context.mouseButton == 2 && this.handleMiddleClick(mouseX, mouseY, ctrl, shift, alt))
            {
                return true;
            }
        }

        return super.subMouseClicked(context);
    }

    private boolean handleLeftClick(UIContext context, int mouseX, int mouseY, boolean ctrl, boolean shift, boolean alt)
    {
        if (!this.hasEmbeddedView())
        {
            int tick = (int) Math.floor(this.scale.from(mouseX));
            int layerIndex = this.fromLayerY(mouseY);
            Clip original = this.delegate.getClip();
            Clip clip = this.clips.getClipAt(tick, layerIndex);

            if (clip != null)
            {
                if (clip != original)
                {
                    if (shift || this.selection.contains(this.clips.getIndex(clip)))
                    {
                        this.addSelected(clip);

                        Clip last = this.getLastSelectedClip();

                        if (last != original)
                        {
                            this.delegate.pickClip(last);
                        }
                    }
                    else
                    {
                        this.delegate.pickClip(clip);
                        this.setSelected(clip);
                    }
                }

                this.grabMode = this.getClipHandle(clip, context, LAYER_HEIGHT);
                this.canGrab = false;
                this.grabbing = true;
                this.grabbedClips = this.getClipsFromSelection();
                this.otherClips = new ArrayList<>(this.clips.get());
                this.otherClips.removeIf(this.grabbedClips::contains);
                this.snappingPoints.clear();
                this.snappingPoints.add(this.delegate.getCursor());

                if (BBSSettings.editorSnapToMarkers.get())
                {
                    /* TODO: generalize this code. Check also other places getMult() */
                    int mult = this.scale.getMult() * 2;
                    int start = (int) this.scale.getMinValue();
                    int end = (int) this.scale.getMaxValue();
                    int max = Integer.MAX_VALUE;

                    start -= start % mult;
                    end -= end % mult;

                    start = MathUtils.clamp(start, 0, max);
                    end = MathUtils.clamp(end, mult, max);

                    for (int j = start; j <= end; j += mult)
                    {
                        this.snappingPoints.add(j);
                    }
                }
                else
                {
                    this.snappingPoints.add(0);
                }

                for (Clip otherClip : this.otherClips)
                {
                    this.snappingPoints.add(otherClip.tick.get());
                    this.snappingPoints.add(otherClip.tick.get() + otherClip.duration.get());
                }

                this.setMouse(mouseX, mouseY);

                for (Clip selectedClip : this.getClipsFromSelection())
                {
                    this.grabbedData.add(new Vector3i(selectedClip.tick.get(), selectedClip.layer.get(), selectedClip.duration.get()));
                }

                return true;
            }
        }

        if (shift && !this.hasEmbeddedView())
        {
            this.selecting = true;

            this.setMouse(mouseX, mouseY);

            return true;
        }
        else if (alt)
        {
            this.selectingLoop = 0;
            this.loopMin = this.fromGraphX(mouseX);
            this.verifyLoopMinMax();
        }
        else
        {
            this.scrubbing = true;
            this.delegate.setCursor(this.fromGraphX(mouseX));

            return true;
        }

        return false;
    }

    private boolean handleRightClick(int mouseX, int mouseY, boolean ctrl, boolean shift, boolean alt)
    {
        if (alt)
        {
            boolean same = this.loopMin == this.loopMax;

            this.selectingLoop = 1;
            this.loopMax = this.fromGraphX(mouseX);

            if (same)
            {
                this.loopMin = this.loopMax;
            }
            else
            {
                this.verifyLoopMinMax();
            }

            return true;
        }

        return false;
    }

    private boolean handleMiddleClick(int mouseX, int mouseY, boolean ctrl, boolean shift, boolean alt)
    {
        if (alt)
        {
            this.loopMin = this.loopMax = 0;
        }
        else
        {
            this.scrolling = true;
            this.setMouse(mouseX, mouseY);

            return true;
        }

        return false;
    }

    @Override
    public boolean subMouseScrolled(UIContext context)
    {
        if (this.area.isInside(context) && !this.scrolling && !this.hasEmbeddedView())
        {
            if (context.mouseWheelHorizontal != 0D)
            {
                this.scale.setShift(this.scale.getShift() - (25F * BBSSettings.scrollingSensitivityHorizontal.get() * context.mouseWheelHorizontal) / this.scale.getZoom());
            }
            else if (Window.isShiftPressed())
            {
                this.vertical.mouseScroll(context);
            }
            else if (context.mouseWheel != 0D)
            {
                this.scale.zoomAnchor(Scale.getAnchorX(context, this.area), Math.copySign(this.scale.getZoomFactor(), context.mouseWheel));
            }

            return true;
        }

        return super.subMouseScrolled(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.hasEmbeddedView())
        {
            return super.subMouseReleased(context);
        }

        this.vertical.mouseReleased(context);

        if (this.selecting)
        {
            this.pickLastSelectedClip();
        }

        this.grabMode = 0;
        this.grabbing = false;
        this.selecting = false;
        this.scrubbing = false;
        this.scrolling = false;
        this.selectingLoop = -1;

        this.grabbedClips = Collections.emptyList();
        this.otherClips = Collections.emptyList();
        this.snappingPoints.clear();
        this.grabbedData.clear();

        return super.subMouseReleased(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.embedded != null && context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.embedView(null);
            UIUtils.playClick();

            return true;
        }

        return super.subKeyPressed(context);
    }

    @Override
    public void render(UIContext context)
    {
        this.updateScrollSize();

        if (this.clips != null && !this.hasEmbeddedView())
        {
            this.vertical.drag(context);
            this.handleInput(context.mouseX, context.mouseY);
            this.handleScrolling(context.mouseX, context.mouseY);
            this.renderCameraWork(context);
        }

        super.render(context);
    }

    private void handleInput(int mouseX, int mouseY)
    {
        if (this.scrubbing)
        {
            this.delegate.setCursor(this.fromGraphX(mouseX));
        }
        else if (this.selectingLoop == 0)
        {
            this.loopMin = MathUtils.clamp(this.fromGraphX(mouseX), 0, this.loopMax);
        }
        else if (this.selectingLoop == 1)
        {
            this.loopMax = MathUtils.clamp(this.fromGraphX(mouseX), this.loopMin, Integer.MAX_VALUE);
        }
        else if (this.selecting)
        {
            Area selection = new Area();

            selection.setPoints(this.lastX, this.lastY, mouseX, mouseY);
            this.captureSelection(selection);
        }
        else if (this.grabbing)
        {
            if (this.canGrab)
            {
                this.dragClips(mouseX, mouseY);

                this.lastX = mouseX;
                this.lastY = mouseY;
            }
            else if (Math.abs(mouseX - this.initialX) > 1 || Math.abs(mouseY - this.initialY) > 1 || Window.isAltPressed())
            {
                this.canGrab = true;
            }
        }
    }

    private void dragClips(int mouseX, int mouseY)
    {
        List<Clip> others = Window.isAltPressed() ? Collections.emptyList() : this.otherClips;
        int dx = this.fromGraphX(mouseX) - this.fromGraphX(this.initialX);
        int dy = this.fromLayerY(mouseY) - this.fromLayerY(this.initialY);

        if (this.grabMode == 0) this.moveClips(others, dx, dy);
        else if (this.grabMode == 1) this.dragLeftEdge(others, dx, dy);
        else if (this.grabMode == 2) this.dragRightEdge(others, dx, dy);

        this.delegate.fillData();
    }

    private void moveClips(List<Clip> others, int dx, int dy)
    {
        Anchor anchor = this.findClosestAnchor(this.grabbedData);

        if (anchor != null)
        {
            Vector3i ref = this.grabbedData.get(anchor.clipIndex());
            int edgeTick = ref.x() + (anchor.isLeft() ? 0 : ref.z());
            int snapped = this.snap(edgeTick + dx);

            dx += snapped - (edgeTick + dx);
        }

        int[] adjusted = this.resolveCollisions(others, this.grabbedData, dx, dy);

        for (int i = 0; i < this.grabbedClips.size(); i++)
        {
            Vector3i v = this.grabbedData.get(i);

            this.setClipData(this.grabbedClips.get(i), v.x() + adjusted[0], v.y() + adjusted[1], v.z());
        }
    }

    private void dragLeftEdge(List<Clip> others, int dx, int dy)
    {
        Vector3i data = grabbedData.get(grabbedData.size() - 1);
        Clip clip = grabbedClips.get(grabbedClips.size() - 1);
        int tick = data.x();
        int duration = data.z();
        int newTick = tick + dx;
        int newDuration = duration - dx;
        int snapped = this.snap(newTick);
        int minLeft = others.stream()
            .filter((o) -> this.sameLayer(o, clip) && o.tick.get() + o.duration.get() <= tick)
            .mapToInt((o) -> o.tick.get() + o.duration.get())
            .max()
            .orElse(0);

        newDuration += newTick - snapped;
        newTick = Math.max(minLeft, snapped);

        if (newDuration < 1)
        {
            newDuration = 1;
            newTick = tick + duration - 1;
        }

        this.setClipData(clip, newTick, data.y(), newDuration);
    }

    private void dragRightEdge(List<Clip> others, int dx, int dy)
    {
        Vector3i data = grabbedData.get(grabbedData.size() - 1);
        Clip clip = grabbedClips.get(grabbedClips.size() - 1);
        int tick = data.x();
        int duration = data.z();
        int newDuration = duration + dx;
        int snapped = this.snap(tick + newDuration);
        int maxRight = others.stream()
            .filter((o) -> this.sameLayer(o, clip) && o.tick.get() >= tick + duration)
            .mapToInt((o) -> o.tick.get())
            .min()
            .orElse(Integer.MAX_VALUE);

        newDuration = snapped - tick;

        if (tick + newDuration >= maxRight)
        {
            newDuration = maxRight - tick;
        }

        if (newDuration < 1)
        {
            newDuration = 1;
        }

        this.setClipData(clip, tick, data.y(), newDuration);
    }

    private Anchor findClosestAnchor(List<Vector3i> data)
    {
        return IntStream.range(0, data.size())
            .boxed()
            .flatMap((i) ->
            {
                Vector3i v = data.get(i);
                int left = this.toGraphX(v.x());
                int right = this.toGraphX(v.x() + v.z());

                return Stream.of(new Anchor(i, true, left), new Anchor(i, false, right));
            })
            .min(Comparator.comparingInt((a) -> Math.abs(a.graphX() - this.initialX)))
            .orElse(null);
    }

    private int[] resolveCollisions(List<Clip> others, List<Vector3i> data, int dx, int dy)
    {
        int dir = 0;

        while (this.collisionExists(others, data, dx, dy))
        {
            if (dir % 2 == 0 && dx != 0) dx -= Integer.signum(dx);
            if (dir % 2 == 1 && dy != 0) dy -= Integer.signum(dy);

            dir += 1;
        }

        return new int[]{dx, dy};
    }

    private boolean collisionExists(List<Clip> others, List<Vector3i> data, int dx, int dy)
    {
        for (int i = 0; i < data.size(); i++)
        {
            Vector3i v = data.get(i);

            int newTick = v.x() + dx;
            int newLayer = v.y() + dy;
            int newDuration = newTick + v.z();

            if (newTick < 0 || newLayer < 0)
            {
                return true;
            }

            for (Clip other : others)
            {
                if (other.layer.get() == newLayer && MathUtils.isInside(newTick, newDuration, other.tick.get(), other.tick.get() + other.duration.get()))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean sameLayer(Clip a, Clip b)
    {
        return a.layer.get().equals(b.layer.get());
    }

    private void setClipData(Clip clip, int newTick, int newLayer, int newDuration)
    {
        if (clip.tick.get() != newTick && clip.duration.get() != newDuration)
        {
            clip.shiftLeft(newTick);
        }

        clip.tick.set(newTick);
        clip.duration.set(newDuration);
        clip.layer.set(newLayer);
    }

    private int snap(int tick)
    {
        if (Window.isAltPressed())
        {
            return tick;
        }

        int diff = 11;
        int closest = tick;

        for (int point : this.snappingPoints)
        {
            int pointX = this.toGraphX(point);
            int abs = Math.abs(this.toGraphX(tick) - pointX);

            if (abs <= 10 && abs < diff)
            {
                closest = point;
                diff = abs;
            }
        }

        return closest;
    }

    private void captureSelection(Area area)
    {
        this.clearSelection();

        for (Clip clip : this.clips.get())
        {
            Area clipArea = new Area();

            int x = this.toGraphX(clip.tick.get());
            int y = this.toLayerY(clip.layer.get());

            clipArea.set(x, y, this.toGraphX(clip.tick.get() + clip.duration.get()) - x, LAYER_HEIGHT);

            if (area.intersects(clipArea))
            {
                this.addSelected(clip);
            }
        }
    }

    private void handleScrolling(int mouseX, int mouseY)
    {
        if (this.scrolling)
        {
            this.scale.setShift(this.scale.getShift() - (mouseX - this.lastX) / this.scale.getZoom());
            this.vertical.scrollBy(this.lastY - mouseY);
            this.vertical.clamp();

            this.lastX = mouseX;
            this.lastY = mouseY;

            this.scale.setShift(this.scale.getShift());
            this.scale.calculateMultiplier();
        }
    }

    /**
     * Render camera work (layers, clips, envelope previews, looping region, cursor, etc.)
     */
    private void renderCameraWork(UIContext context)
    {
        Batcher2D batcher = context.batcher;
        Area area = this.area;
        int h = LAYER_HEIGHT;
        int leftEdge = this.toGraphX(0);

        if (leftEdge > this.area.x)
        {
            batcher.box(this.area.x, this.area.y, Math.min(leftEdge, this.area.ex()), this.area.ey(), Colors.A75);
        }

        area.render(batcher, Colors.A50);
        batcher.clip(this.vertical.area, context);

        for (int i = 0; i < this.layers; i++)
        {
            int ly = this.toLayerY(i);

            if (i % 2 != 0)
            {
                batcher.box(leftEdge, ly, this.area.ex(), ly + h, Colors.A50);
            }
        }

        batcher.unclip(context);
        batcher.clip(this.area, context);

        this.renderTickMarkers(context, area.y, area.h);

        batcher.unclip(context);
        batcher.clip(this.vertical.area, context);

        List<Clip> clips = this.clips.get();

        for (int i = 0, c = clips.size(); i < c; i++)
        {
            Clip clip = clips.get(i);
            IUIClipRenderer renderer = this.renderers.get(clip);

            Area clipArea = this.getClipArea(clip, CLIP_AREA, h);
            boolean selected = this.hasSelected(i);

            if (!this.hasEmbeddedView())
            {
                clipArea.y += 1;
                clipArea.h -= 2;
            }

            renderer.renderClip(context, this, clip, clipArea, selected, this.delegate.getClip() == clip);

            int clipHandle = this.getClipHandle(clip, context, h);
            int color = this.grabMode != 0 ? Colors.WHITE : Colors.A50;

            if (clipHandle == 1 || (selected && this.grabMode == 1))
            {
                context.batcher.icon(Icons.CLIP_HANLDE_LEFT, color, clipArea.x, clipArea.y + 10, 0F, 0.5F);
            }
            else if (clipHandle == 2 || (selected && this.grabMode == 2))
            {
                context.batcher.icon(Icons.CLIP_HANLDE_RIGHT, color, clipArea.ex(), clipArea.y + 10, 1F, 0.5F);
            }
        }

        this.renderAddPreview(context, h);
        this.renderLoopingRegion(context, area.y);

        batcher.unclip(context);
        batcher.clip(this.area, context);

        String label = TimeUtils.formatTime(this.delegate.getCursor()) + "/" + TimeUtils.formatTime(this.clips.calculateDuration());

        renderCursor(context, label, area, this.toGraphX(this.delegate.getCursor()));
        this.renderSelection(context);

        batcher.unclip(context);
        batcher.clip(this.vertical.area, context);

        this.vertical.renderScrollbar(batcher);

        batcher.unclip(context);
    }

    private Area getClipArea(Clip clip, Area area, int h)
    {
        int tick = clip.tick.get();
        int x = this.toGraphX(tick);
        int y = this.toLayerY(clip.layer.get());
        int w = this.toGraphX(tick + clip.duration.get()) - x;

        area.set(x, y, w, h);

        return area;
    }

    private int getClipHandle(Clip clip, UIContext context, int h)
    {
        Area clipArea = this.getClipArea(clip, CLIP_AREA, h);
        int separation = Math.min(clipArea.w / 2, 5);

        if (clipArea.isInside(context))
        {
            if (Window.isCtrlPressed())
            {
                return 0;
            }

            if (context.mouseX - clipArea.x < separation)
            {
                return 1;
            }
            else if (context.mouseX - clipArea.ex() >= -separation)
            {
                return 2;
            }

            return 0;
        }

        return -1;
    }

    private void renderAddPreview(UIContext context, int h)
    {
        if (this.addPreview == null)
        {
            return;
        }

        int x = this.toGraphX(this.addPreview.x);
        int y = this.toLayerY(this.addPreview.y);
        int d = this.toGraphX(this.addPreview.x + this.addPreview.z);

        context.batcher.outline(x, y, d, y + h, Colors.WHITE);
    }

    /**
     * Render tick markers that help orient within camera work.
     */
    private void renderTickMarkers(UIContext context, int y, int h)
    {
        int mult = this.scale.getMult() * 2;
        int start = (int) this.scale.getMinValue();
        int end = (int) this.scale.getMaxValue();
        int max = Integer.MAX_VALUE;

        start -= start % mult;
        end -= end % mult;

        start = MathUtils.clamp(start, 0, max);
        end = MathUtils.clamp(end, mult, max);

        for (int j = start; j <= end; j += mult)
        {
            int xx = this.toGraphX(j);
            String value = TimeUtils.formatTime(j);

            context.batcher.box(xx, y, xx + 1, y + h, Colors.setA(Colors.WHITE, 0.2F));
            context.batcher.textShadow(value, xx + 3, this.area.y + 4, Colors.WHITE);
        }
    }

    /**
     * Render selection box.
     */
    private void renderSelection(UIContext context)
    {
        if (this.selecting)
        {
            context.batcher.normalizedBox(this.lastX, this.lastY, context.mouseX, context.mouseY, Colors.setA(Colors.ACTIVE, 0.25F));
        }
    }

    /**
     * Render looping region
     */
    private void renderLoopingRegion(UIContext context, int y)
    {
        if (this.loopMin == this.loopMax)
        {
            return;
        }

        int min = Math.min(this.loopMin, this.loopMax);
        int max = Math.max(this.loopMin, this.loopMax);

        int minX = this.toGraphX(min);
        int maxX = this.toGraphX(max);

        if (maxX >= this.area.x + 1 && minX < this.area.ex() - 1)
        {
            minX = MathUtils.clamp(minX, this.area.x + 1, this.area.ex() - 1);
            maxX = MathUtils.clamp(maxX, this.area.x + 1, this.area.ex() - 1);

            float alpha = BBSSettings.editorLoop.get() ? 1 : 0.4F;
            int color = Colors.mulRGB(0xff88ffff, alpha);

            context.batcher.gradientVBox(minX, y, maxX, this.area.ey(), Colors.mulRGB(0x0000ffff, alpha), Colors.mulRGB(0xaa0088ff, alpha));
            context.batcher.box(minX, y, minX + 1, this.area.ey(), color);
            context.batcher.box(maxX - 1, y, maxX, this.area.ey(), color);
        }
    }

    private record Anchor(int clipIndex, boolean isLeft, int graphX)
    {}

    private interface ClipTransformStrategy
    {
        public void apply(List<Clip> others, List<Clip> grabbedClips, List<Vector3i> grabbedData, int dx, int dy);
    }
}