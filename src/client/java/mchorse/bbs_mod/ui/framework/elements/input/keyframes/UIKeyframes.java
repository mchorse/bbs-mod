package mchorse.bbs_mod.ui.framework.elements.input.keyframes;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.math.Operation;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.KeyframeType;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeGraph;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Scale;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UICopyPasteController;
import mchorse.bbs_mod.ui.utils.presets.UIPresetContextMenu;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.presets.PresetManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIKeyframes extends UIElement
{
    /* Editing states */

    private boolean selecting;
    private boolean navigating;
    private int dragging = -1;
    private Pair<Keyframe, KeyframeType> draggingData;
    private boolean scaling;
    private float scalingAnchor;
    private Map<Keyframe, Float> scaleTicks = new HashMap<>();

    private boolean stacking;
    private float stackOffset;

    private int lastX;
    private int lastY;
    private int originalX;
    private int originalY;
    private float originalT;
    private Object originalV;

    private Runnable changeCallback;

    /* Fields */

    private final UIKeyframeDopeSheet dopeSheet = new UIKeyframeDopeSheet(this);
    private IUIKeyframeGraph currentGraph = this.dopeSheet;

    private final Scale xAxis = new Scale(this.area, ScrollDirection.HORIZONTAL);

    private final Consumer<Keyframe> callback;
    private Consumer<UIContext> backgroundRender;
    private Supplier<Integer> duration;

    private SheetCache cache;

    private IAxisConverter converter;

    private UICopyPasteController copyPasteController;

    public UIKeyframes(Consumer<Keyframe> callback)
    {
        this.callback = callback;

        this.copyPasteController = new UICopyPasteController(PresetManager.KEYFRAMES, "_CopyKeyframes")
            .supplier(this::serializeKeyframes)
            .consumer((data, mouseX, mouseY) ->
            {
                double offset = Math.round(this.fromGraphX(mouseX));

                this.pasteKeyframes(parseKeyframes(data), (float) offset, mouseY);
            })
            .canCopy(() -> this.currentGraph.getSelected() != null);

        /* Context menu items */
        this.context((menu) ->
        {
            UIContext context = this.getContext();
            int mouseX = context.mouseX;
            int mouseY = context.mouseY;
            boolean hasSelected = this.currentGraph.getSelected() != null;

            menu.custom(new UIPresetContextMenu(this.copyPasteController, mouseX, mouseY)
                .labels(UIKeys.KEYFRAMES_CONTEXT_COPY, UIKeys.KEYFRAMES_CONTEXT_PASTE));

            if (this.isEditing())
            {
                menu.action(Icons.CLOSE, UIKeys.KEYFRAMES_CONTEXT_EXIT_TRACK, () -> this.editSheet(null));
            }
            else
            {
                UIKeyframeSheet sheet = this.dopeSheet.getSheet(this.getContext().mouseY);

                if (sheet != null && KeyframeFactories.isNumeric(sheet.channel.getFactory()))
                {
                    menu.action(Icons.EDIT, UIKeys.KEYFRAMES_CONTEXT_EDIT_TRACK.format(sheet.id), () -> this.editSheet(sheet));
                }
            }

            menu.action(Icons.SEARCH, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES, () -> this.adjustValues());
            menu.action(Icons.ARROW_LEFT, UIKeys.KEYFRAMES_KEYS_SELECT_LEFT, () -> this.selectAfter(mouseX, mouseY, -1));
            menu.action(Icons.ARROW_RIGHT, UIKeys.KEYFRAMES_KEYS_SELECT_RIGHT, () -> this.selectAfter(mouseX, mouseY, 1));

            menu.action(Icons.MAXIMIZE, UIKeys.KEYFRAMES_CONTEXT_MAXIMIZE, this::resetView);
            menu.action(Icons.FULLSCREEN, UIKeys.KEYFRAMES_CONTEXT_SELECT_ALL, () -> this.currentGraph.selectAll());

            if (hasSelected)
            {
                menu.action(Icons.CONVERT, UIKeys.KEYFRAMES_CONTEXT_SPREAD, this::spreadKeyframes);
                menu.action(Icons.OUTLINE_SPHERE, UIKeys.KEYFRAMES_CONTEXT_ROUND, () ->
                {
                    for (UIKeyframeSheet sheet : this.getGraph().getSheets())
                    {
                        List<Keyframe> selected = sheet.selection.getSelected();

                        if (selected.isEmpty())
                        {
                            continue;
                        }

                        sheet.channel.preNotify();

                        for (Keyframe kf : selected)
                        {
                            kf.setTick(Math.round(kf.getTick()), false);
                        }

                        sheet.channel.postNotify();
                    }
                });
                menu.action(Icons.REMOVE, UIKeys.KEYFRAMES_CONTEXT_REMOVE, () -> this.currentGraph.removeSelected());
            }
        });

        /* Keys */
        IKey category = UIKeys.KEYFRAMES_KEYS_CATEGORY;
        Supplier<Boolean> canModify = () -> !this.scaling;

        this.keys().register(Keys.KEYFRAMES_MAXIMIZE, this::resetView).inside().category(category);
        this.keys().register(Keys.KEYFRAMES_SELECT_ALL, () -> this.currentGraph.selectAll()).inside().category(category).active(canModify);
        this.keys().register(Keys.COPY, () ->
        {
            if (this.copyPasteController.copy()) UIUtils.playClick();
        }).inside().category(category);
        this.keys().register(Keys.PASTE, () ->
        {
            UIContext context = this.getContext();

            if (this.copyPasteController.paste(context.mouseX, context.mouseY)) UIUtils.playClick();
        }).inside().category(category).active(canModify);
        this.keys().register(Keys.PRESETS, () ->
        {
            UIContext context = this.getContext();

            if (this.copyPasteController.canPreviewPresets())
            {
                this.copyPasteController.openPresets(context, context.mouseX, context.mouseY);
                UIUtils.playClick();
            }
        }).category(category).active(canModify);
        this.keys().register(Keys.DELETE, () -> this.currentGraph.removeSelected()).inside().category(category).active(canModify);
        this.keys().register(Keys.KEYFRAMES_SELECT_LEFT, () ->
        {
            UIContext context = this.getContext();

            this.selectAfter(context.mouseX, context.mouseY, -1);
        }).category(category).active(canModify);
        this.keys().register(Keys.KEYFRAMES_SELECT_RIGHT, () ->
        {
            UIContext context = this.getContext();

            this.selectAfter(context.mouseX, context.mouseY, 1);
        }).category(category).active(canModify);
        this.keys().register(Keys.KEYFRAMES_SELECT_SAME, this::selectSame).category(category).active(canModify);
        this.keys().register(Keys.KEYFRAMES_SCALE_TIME, this::scaleTime).inside().category(category);
        this.keys().register(Keys.KEYFRAMES_STACK_KEYFRAMES, () -> this.stackKeyframes(false)).inside().category(category);
        this.keys().register(Keys.KEYFRAMES_SELECT_PREV, () -> this.selectNextKeyframe(-1)).category(category);
        this.keys().register(Keys.KEYFRAMES_SELECT_NEXT, () -> this.selectNextKeyframe(1)).category(category);
        this.keys().register(Keys.KEYFRAMES_SPREAD, this::spreadKeyframes).category(category);
        this.keys().register(Keys.KEYFRAMES_ADJUST_VALUES, this::adjustValues).category(category);
    }

    private void adjustValues()
    {
        this.getContext().replaceContextMenu((menu2) ->
        {
            menu2.autoKeys();
            menu2.action(Icons.ARROW_LEFT, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_LEFT, () -> this.adjustValues(false));
            menu2.action(Icons.ARROW_RIGHT, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_RIGHT, () -> this.adjustValues(true));
        });
    }

    private void adjustValues(boolean last)
    {
        for (UIKeyframeSheet sheet : this.getGraph().getSheets())
        {
            List<Keyframe> selected = sheet.selection.getSelected();
            IKeyframeFactory factory = sheet.channel.getFactory();

            if (selected.size() < 2 || !KeyframeFactories.isNumeric(factory))
            {
                continue;
            }

            sheet.channel.preNotify();

            int index = last ? selected.size() - 1 : 0;
            int previous = last ? selected.size() - 2 : 1;

            Keyframe kf = selected.get(index);
            Keyframe prevKf = selected.get(previous);

            double difference = factory.getY(kf.getValue()) - factory.getY(prevKf.getValue());

            selected.remove(index);

            for (Keyframe keyframe : selected)
            {
                keyframe.setValue(factory.yToValue(factory.getY(keyframe.getValue()) + difference));
            }

            sheet.channel.postNotify();
        }
    }

    public UIKeyframes changed(Runnable runnable)
    {
        this.changeCallback = runnable;

        return this;
    }

    public void triggerChange()
    {
        if (this.changeCallback != null)
        {
            this.changeCallback.run();

        }
    }

    public UIKeyframeDopeSheet getDopeSheet()
    {
        return this.dopeSheet;
    }

    protected void selectNextKeyframe(int direction)
    {
        IUIKeyframeGraph graph = this.getGraph();
        Keyframe keyframe = graph.getSelected();

        if (keyframe == null)
        {
            UIContext context = this.getContext();
            UIKeyframeSheet sheet = this.getGraph().getSheet(context.mouseY);
            KeyframeSegment segment = sheet.channel.find((float) this.fromGraphX(context.mouseX));

            if (segment != null)
            {
                keyframe = direction < 0 ? segment.a : segment.b;

                graph.clearSelection();
                graph.selectKeyframe(keyframe);

                return;
            }
        }

        if (keyframe != null)
        {
            KeyframeChannel channel = (KeyframeChannel) keyframe.getParent();
            int existingIndex = channel.getKeyframes().indexOf(keyframe);
            int index = MathUtils.cycler(existingIndex + direction, channel.getAll());
            Keyframe nextKeyframe = channel.get(index);

            graph.clearSelection();
            graph.selectKeyframe(nextKeyframe);
        }
    }

    private void selectAfter(int mouseX, int mouseY, int direction)
    {
        float tick = (float) this.fromGraphX(mouseX);

        if (!Window.isShiftPressed())
        {
            this.currentGraph.selectAfter(tick, direction);
        }
        else
        {
            this.currentGraph.getSheet(mouseY).selection.after(tick, direction);
            this.currentGraph.pickSelected();
        }
    }

    private void selectSame()
    {
        UIContext context = this.getContext();
        Pair<Keyframe, KeyframeType> keyframe = this.currentGraph.findKeyframe(context.mouseX, context.mouseY);

        if (keyframe != null)
        {
            if (!Window.isShiftPressed())
            {
                this.currentGraph.clearSelection();
            }

            for (UIKeyframeSheet sheet : this.currentGraph.getSheets())
            {
                List<Keyframe> list = sheet.channel.getList();

                for (int i = 0; i < list.size(); i++)
                {
                    Keyframe kf = list.get(i);

                    if (kf.getFactory().compare(keyframe.a.getValue(), kf.getValue()))
                    {
                        sheet.selection.add(i);
                    }
                }
            }

            this.currentGraph.pickSelected();
        }
    }

    private void scaleTime()
    {
        if (this.scaling)
        {
            this.scaling = false;

            return;
        }

        UIContext context = this.getContext();

        this.scaling = true;
        this.scaleTicks.clear();
        this.scalingAnchor = Integer.MAX_VALUE;
        this.originalX = context.mouseX;
        this.originalY = context.mouseY;

        for (UIKeyframeSheet sheet : this.currentGraph.getSheets())
        {
            for (Keyframe keyframe : sheet.selection.getSelected())
            {
                this.scaleTicks.put(keyframe, keyframe.getTick());
                this.scalingAnchor = Math.min(this.scalingAnchor, keyframe.getTick());
            }
        }
    }

    private void stackKeyframes(boolean cancel)
    {
        if (this.stacking)
        {
            this.stacking = false;

            if (!cancel)
            {
                UIContext context = this.getContext();
                List<UIKeyframeSheet> sheets = new ArrayList<>();
                float currentTick = (float) this.fromGraphX(context.mouseX);

                for (UIKeyframeSheet sheet : this.getGraph().getSheets())
                {
                    if (sheet.selection.hasAny())
                    {
                        sheets.add(sheet);
                    }
                }

                for (UIKeyframeSheet current : sheets)
                {
                    List<Keyframe> selected = current.selection.getSelected();
                    float mMin = Integer.MAX_VALUE;
                    float mMax = Integer.MIN_VALUE;

                    for (Keyframe keyframe : selected)
                    {
                        mMin = Math.min(keyframe.getTick(), mMin);
                        mMax = Math.max(keyframe.getTick(), mMax);
                    }

                    float length = mMax - mMin + this.getStackOffset();
                    int times = (int) Math.max(1, Math.ceil((currentTick - mMax) / length));
                    float x = 0;

                    current.selection.clear();

                    for (int i = 0; i < times; i++)
                    {
                        for (Keyframe keyframe : selected)
                        {
                            float tick = mMax + this.getStackOffset() + (keyframe.getTick() - mMin) + x;
                            int index = current.channel.insert(tick, keyframe.getFactory().copy(keyframe.getValue()));
                            Keyframe kf = current.channel.get(index);
                            
                            kf.copy(keyframe);
                            kf.setTick(tick);
                            current.selection.add(index);
                        }

                        x += length;
                    }
                }
            }

            return;
        }

        this.stacking = true;
        this.stackOffset = 1;
    }

    public boolean isStacking()
    {
        return this.stacking;
    }

    public float getStackOffset()
    {
        return this.stackOffset;
    }

    private void spreadKeyframes()
    {
        for (UIKeyframeSheet sheet : this.getGraph().getSheets())
        {
            List<Keyframe> selected = sheet.selection.getSelected();

            if (selected.isEmpty())
            {
                continue;
            }

            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            for (Keyframe keyframe : selected)
            {
                int index = sheet.channel.getKeyframes().indexOf(keyframe);

                min = Math.min(min, index);
                max = Math.max(max, index);
            }

            Keyframe minKf = sheet.channel.get(min);
            Keyframe maxKf = sheet.channel.get(max);
            int count = max - min;
            float distance = (maxKf.getTick() - minKf.getTick()) / count;

            sheet.channel.preNotify();

            for (int i = 1; i < count; i++)
            {
                int index = i + min;
                Keyframe kf = sheet.channel.get(index);

                kf.setTick(minKf.getTick() + i * distance);
            }

            sheet.channel.postNotify();

            sheet.selection.clear();

            for (int i = min; i <= max; i++)
            {
                sheet.selection.add(i);
            }
        }

        this.getGraph().pickSelected();
    }

    /* Sheet editing */

    public boolean isEditing()
    {
        return this.currentGraph != this.dopeSheet;
    }

    public void editSheet(UIKeyframeSheet sheet)
    {
        if (sheet == null)
        {
            this.currentGraph = this.dopeSheet;
        }
        else
        {
            this.dopeSheet.clearSelection();
            this.dopeSheet.pickSelected();

            this.currentGraph = new UIKeyframeGraph(this, sheet);

            this.resetView();
        }
    }

    /* Caching keyframes */

    public void cacheKeyframes()
    {
        this.cache = new SheetCache(this.currentGraph.getSheets());
    }

    public void submitKeyframes()
    {
        /* Cache selection indices */
        Map<UIKeyframeSheet, Pair<List<Integer>, List<Integer>>> selection = new HashMap<>();

        for (UIKeyframeSheet sheet : this.currentGraph.getSheets())
        {
            List<Integer> last = sheet.sort();

            selection.put(sheet, new Pair<>(last, new ArrayList<>(sheet.selection.getIndices())));
        }

        /* Apply the data in order and submit to pre-/post-handlers */
        SheetCache cache = new SheetCache(this.currentGraph.getSheets());

        for (Pair<BaseType, UIKeyframeSheet> pair : this.cache.data)
        {
            pair.b.channel.fromData(pair.a);
            pair.b.selection.clear();
            pair.b.selection.addAll(selection.get(pair.b).a);
            pair.b.channel.preNotify(IValueListener.FLAG_UNMERGEABLE);
        }

        for (Pair<BaseType, UIKeyframeSheet> pair : cache.data)
        {
            pair.b.channel.fromData(pair.a);
            pair.b.selection.clear();
            pair.b.selection.addAll(selection.get(pair.b).b);
            pair.b.channel.postNotify(IValueListener.FLAG_UNMERGEABLE);
        }

        this.cache = null;
    }

    /* Copy-pasting */

    /**
     * Parses keyframe data from the given map structure into a map of keyframes corresponding to their keys.
     *
     * @param data the map data containing keyframe information. Each key in the map represents
     *             a collection of keyframe data, where the associated value contains type and keyframe details.
     *             If null, the method returns an empty map.
     * @return a map where each key corresponds to a collection of parsed {@link PastedKeyframes}.
     *         If no keyframe data is provided or the input is null, an empty map is returned.
     */
    public static Map<String, PastedKeyframes> parseKeyframes(MapType data)
    {
        if (data == null)
        {
            return Collections.emptyMap();
        }

        Map<String, PastedKeyframes> temp = new HashMap<>();

        for (String key : data.keys())
        {
            MapType map = data.getMap(key);
            ListType list = map.getList("keyframes");
            IKeyframeFactory serializer = KeyframeFactories.FACTORIES.get(map.getString("type"));

            for (int i = 0, c = list.size(); i < c; i++)
            {
                PastedKeyframes pastedKeyframes = temp.computeIfAbsent(key, k -> new PastedKeyframes(serializer));
                Keyframe keyframe = new Keyframe("", serializer);

                keyframe.fromData(list.getMap(i));
                pastedKeyframes.keyframes.add(keyframe);
            }
        }

        return temp;
    }

    private MapType serializeKeyframes()
    {
        MapType keyframes = new MapType();

        for (UIKeyframeSheet property : this.currentGraph.getSheets())
        {
            List<Keyframe> selected = property.selection.getSelected();

            if (selected.isEmpty())
            {
                continue;
            }

            MapType data = new MapType();
            ListType list = new ListType();

            data.putString("type", CollectionUtils.getKey(KeyframeFactories.FACTORIES, property.channel.getFactory()));
            data.put("keyframes", list);

            for (Keyframe keyframe : selected)
            {
                list.add(keyframe.toData());
            }

            if (!list.isEmpty())
            {
                keyframes.put(property.id, data);
            }
        }

        return keyframes;
    }

    /**
     * Paste copied keyframes to clipboard
     */
    protected void pasteKeyframes(Map<String, PastedKeyframes> keyframes, float offset, int mouseY)
    {
        List<UIKeyframeSheet> sheets = this.currentGraph.getSheets();

        this.currentGraph.clearSelection();

        if (keyframes.size() == 1)
        {
            UIKeyframeSheet current = this.currentGraph.getSheet(mouseY);

            if (current == null)
            {
                current =  sheets.get(0);
            }

            this.pasteKeyframesTo(current, keyframes.get(keyframes.keySet().iterator().next()), offset);
        }
        else
        {
            float min = Float.MAX_VALUE;

            for (Map.Entry<String, PastedKeyframes> entry : keyframes.entrySet())
            {
                if (entry.getValue().keyframes.isEmpty())
                {
                    continue;
                }

                entry.getValue().keyframes.sort((a, b) -> Float.compare(a.getTick(), b.getTick()));

                min = Math.min(min, entry.getValue().keyframes.get(0).getTick());
            }

            for (Map.Entry<String, PastedKeyframes> entry : keyframes.entrySet())
            {
                float entryMin = entry.getValue().keyframes.get(0).getTick();

                for (UIKeyframeSheet property : sheets)
                {
                    if (!property.id.equals(entry.getKey()))
                    {
                        continue;
                    }

                    float d = min == Float.MAX_VALUE ? 0F : entryMin - min;

                    this.pasteKeyframesTo(property, entry.getValue(), offset + d);
                }
            }
        }

        this.currentGraph.pickSelected();
    }

    private void pasteKeyframesTo(UIKeyframeSheet sheet, PastedKeyframes pastedKeyframes, float offset)
    {
        if (sheet.channel.getFactory() != pastedKeyframes.factory)
        {
            return;
        }

        float firstX = pastedKeyframes.keyframes.get(0).getTick();
        List<Keyframe> toSelect = new ArrayList<>();

        for (Keyframe keyframe : pastedKeyframes.keyframes)
        {
            keyframe.setTick(keyframe.getTick() - firstX + offset);

            int index = sheet.channel.insert(keyframe.getTick(), keyframe.getValue());
            Keyframe inserted = sheet.channel.get(index);

            inserted.copy(keyframe);
            toSelect.add(inserted);
        }

        for (Keyframe select : toSelect)
        {
            sheet.selection.add(sheet.channel.getKeyframes().indexOf(select));
        }
    }

    /* Getters & setters */

    public UIKeyframes backgroundRenderer(Consumer<UIContext> backgroundRender)
    {
        this.backgroundRender = backgroundRender;

        return this;
    }

    public UIKeyframes duration(Supplier<Integer> duration)
    {
        this.duration = duration;

        return this;
    }

    public UIKeyframes axisConverter(IAxisConverter converter)
    {
        this.converter = converter;

        return this;
    }

    public IAxisConverter getConverter()
    {
        return this.converter;
    }

    public IUIKeyframeGraph getGraph()
    {
        return this.currentGraph;
    }

    public Scale getXAxis()
    {
        return this.xAxis;
    }

    public int getDuration()
    {
        return this.duration == null ? 0 : this.duration.get();
    }

    public boolean isSelecting()
    {
        return this.selecting;
    }

    public boolean isNavigating()
    {
        return this.navigating;
    }

    /* Sheet management */

    public void removeAllSheets()
    {
        this.dopeSheet.removeAllSheets();
    }

    public void addSheet(UIKeyframeSheet sheet)
    {
        this.dopeSheet.addSheet(sheet);
    }

    public void pickKeyframe(Keyframe keyframe)
    {
        if (this.callback != null)
        {
            this.callback.accept(keyframe);
        }
    }

    /* Graphing */

    public int toGraphX(double tick)
    {
        return (int) this.xAxis.to(tick);
    }

    public double fromGraphX(int mouseX)
    {
        return this.xAxis.from(mouseX);
    }

    public void resetView()
    {
        this.currentGraph.resetView();
    }

    public void resetViewX()
    {
        int c = 0;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        /* Find minimum and maximum */
        for (UIKeyframeSheet property : this.currentGraph.getSheets())
        {
            List keyframes = property.channel.getKeyframes();

            for (Object object : keyframes)
            {
                Keyframe frame = (Keyframe) object;

                min = Integer.min((int) frame.getTick(), min);
                max = Integer.max((int) frame.getTick(), max);
            }

            c = Math.max(c, keyframes.size());
        }

        if (c <= 1)
        {
            min = 0;
            max = this.getDuration();
        }

        if (Math.abs(max - min) > 0.01F)
        {
            this.xAxis.viewOffset(min, max, this.area.w, 30);
        }
        else
        {
            this.xAxis.set(0, 2);
        }
    }

    public Area getGrabbingArea(UIContext context)
    {
        Area area = new Area();

        area.setPoints(this.originalX, this.originalY, context.mouseX, context.mouseY, 3);

        return area;
    }

    /* User input */

    @Override
    public void resize()
    {
        /* Save horizontal view range, and restore it after resize */
        double minValue = this.xAxis.getMinValue();
        double maxValue = this.xAxis.getMaxValue();

        super.resize();

        this.currentGraph.resize();

        if (!Operation.equals(minValue, maxValue))
        {
            this.xAxis.view(minValue, maxValue);
        }
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.currentGraph.mouseClicked(context))
        {
            return true;
        }

        if (this.scaling)
        {
            this.scaling = false;

            return true;
        }

        if (this.stacking)
        {
            this.stackKeyframes(context.mouseButton == 1);

            return true;
        }

        if (this.area.isInside(context))
        {
            this.lastX = this.originalX = context.mouseX;
            this.lastY = this.originalY = context.mouseY;

            if (Window.isCtrlPressed() && context.mouseButton == 0)
            {
                this.removeOrCreateKeyframe(context);
            }
            else if (Window.isAltPressed() && context.mouseButton == 0)
            {
                this.duplicateOrSelectColumn(context);
            }
            else if (context.mouseButton == 0)
            {
                this.pickOrStartSelectingKeyframes(context);
            }
            else if (context.mouseButton == 2)
            {
                this.navigating = true;
            }

            return context.mouseButton != 1;
        }

        return super.subMouseClicked(context);
    }

    private void removeOrCreateKeyframe(UIContext context)
    {
        Pair<Keyframe, KeyframeType> keyframe = this.currentGraph.findKeyframe(context.mouseX, context.mouseY);

        if (keyframe != null)
        {
            this.currentGraph.removeKeyframe(keyframe.a);
        }
        else
        {
            this.currentGraph.addKeyframe(context.mouseX, context.mouseY);
        }
    }

    private void duplicateOrSelectColumn(UIContext context)
    {
        if (this.currentGraph.getSelected() != null && !Window.isShiftPressed())
        {
            /* Duplicate */
            int tick = (int) Math.round(this.fromGraphX(context.mouseX));

            this.pasteKeyframes(this.parseKeyframes(this.serializeKeyframes()), tick, context.mouseY);

            return;
        }

        /* Select a column */
        this.currentGraph.selectByX(context.mouseX);
    }

    private void pickOrStartSelectingKeyframes(UIContext context)
    {
        /* Picking keyframe or initiating selection */
        Pair<Keyframe, KeyframeType> pair = this.currentGraph.findKeyframe(context.mouseX, context.mouseY);
        Keyframe found = pair == null ? null : pair.a;
        boolean shift = Window.isShiftPressed();

        if (shift && found == null)
        {
            this.selecting = true;
        }

        if (found != null)
        {
            UIKeyframeSheet sheet = this.currentGraph.getSheet(found);

            if (!shift && !sheet.selection.has(found))
            {
                this.currentGraph.clearSelection();
            }

            sheet.selection.add(found);

            found = this.currentGraph.getSelected();

            this.pickKeyframe(found);
        }
        else if (!this.selecting)
        {
            this.currentGraph.clearSelection();
            this.pickKeyframe(null);
        }

        if (!this.selecting)
        {
            this.dragging = 0;
            this.draggingData = pair;

            if (pair != null && pair.b != KeyframeType.REGULAR)
            {
                found = pair.a;
            }

            this.cacheKeyframes();

            if (found != null)
            {
                this.originalT = found.getTick();
                this.originalV = found.getFactory().copy(found.getValue());
            }
        }
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        this.currentGraph.mouseReleased(context);

        if (this.selecting)
        {
            this.currentGraph.selectInArea(this.getGrabbingArea(context));
        }

        if (this.dragging > 0)
        {
            this.submitKeyframes();
            this.currentGraph.pickSelected();
        }

        this.navigating = false;
        this.selecting = false;
        this.dragging = -1;

        return super.subMouseReleased(context);
    }

    @Override
    protected boolean subMouseScrolled(UIContext context)
    {
        if (this.area.isInside(context) && this.stacking)
        {
            this.stackOffset = (float) Math.max(0.05F, this.stackOffset + Math.copySign(Window.isShiftPressed() ? 0.05F : 1, context.mouseWheel));

            return true;
        }

        if (this.area.isInside(context) && !this.navigating && !this.scaling)
        {
            this.currentGraph.mouseScrolled(context);

            return true;
        }

        return super.subMouseScrolled(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.currentGraph != this.dopeSheet && context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.editSheet(null);

            return true;
        }

        if ((this.scaling || this.stacking) && context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            /* Reset scaling */
            this.scaling = false;

            if (this.stacking)
            {
                this.stackKeyframes(true);
            }

            for (Map.Entry<Keyframe, Float> entry : this.scaleTicks.entrySet())
            {
                entry.getKey().setTick(entry.getValue(), true);
            }

            return true;
        }

        return super.subKeyPressed(context);
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        this.handleMouse(context);

        context.batcher.clip(this.area, context);

        this.renderBackground(context);
        this.currentGraph.render(context);

        if (this.selecting)
        {
            context.batcher.normalizedBox(this.originalX, this.originalY, context.mouseX, context.mouseY, Colors.setA(Colors.ACTIVE, 0.25F));
        }

        this.currentGraph.postRender(context);

        context.batcher.unclip(context);
    }

    /**
     * Handle any related mouse logic during rendering
     */
    protected void handleMouse(UIContext context)
    {
        this.currentGraph.handleMouse(context, this.lastX, this.lastY);

        int mouseX = context.mouseX;
        int mouseY = context.mouseY;
        boolean mouseHasMoved = Math.abs(mouseX - this.originalX) > 2 || Math.abs(mouseY - this.originalY) > 2;

        if (this.scaling)
        {
            float tick = (float) this.fromGraphX(context.mouseX);
            float originalTick = (float) this.fromGraphX(this.originalX);
            float ratio = (tick - this.scalingAnchor) / (originalTick - this.scalingAnchor);

            for (Map.Entry<Keyframe, Float> entry : this.scaleTicks.entrySet())
            {
                Keyframe keyframe = entry.getKey();
                float oldTick = entry.getValue();
                float newTick = this.scalingAnchor + (oldTick - this.scalingAnchor) * ratio;

                if (Window.isCtrlPressed())
                {
                    newTick = Math.round(newTick);
                }

                keyframe.setTick(newTick, true);
            }
        }
        else if (this.dragging == 0 && mouseHasMoved)
        {
            this.dragging = 1;
        }
        else if (this.dragging == 1)
        {
            if (this.currentGraph.getSelected() != null)
            {
                this.currentGraph.dragKeyframes(context, this.draggingData, this.originalX, this.originalY, this.originalT, this.originalV);
            }
            else
            {
                this.moveNoKeyframes(context);
            }
        }

        this.lastX = mouseX;
        this.lastY = mouseY;
    }

    protected void moveNoKeyframes(UIContext context)
    {}

    /**
     * Render background, specifically backdrop and borders if the duration is present
     */
    protected void renderBackground(UIContext context)
    {
        this.area.render(context.batcher, Colors.A50);

        int duration = this.getDuration();

        if (duration > 0)
        {
            int leftBorder = this.toGraphX(0);
            int rightBorder = this.toGraphX(duration);

            if (leftBorder > this.area.x) context.batcher.box(this.area.x, this.area.y, Math.min(this.area.ex(), leftBorder), this.area.y + this.area.h, Colors.A50);
            if (rightBorder < this.area.ex()) context.batcher.box(Math.max(this.area.x, rightBorder), this.area.y, this.area.ex() , this.area.y + this.area.h, Colors.A50);
        }

        if (this.backgroundRender != null)
        {
            this.backgroundRender.accept(context);
        }
    }

    /* Caching state */

    public KeyframeState cacheState()
    {
        KeyframeState state = new KeyframeState();

        state.extra.putDouble("x_min", this.xAxis.getMinValue());
        state.extra.putDouble("x_max", this.xAxis.getMaxValue());
        this.currentGraph.saveState(state.extra);

        for (UIKeyframeSheet property : this.currentGraph.getSheets())
        {
            state.selected.add(new ArrayList<>(property.selection.getIndices()));
        }

        return state;
    }

    public void applyState(KeyframeState state)
    {
        this.xAxis.view(state.extra.getDouble("x_min"), state.extra.getDouble("x_max"));
        this.currentGraph.restoreState(state.extra);

        List<UIKeyframeSheet> properties = this.currentGraph.getSheets();

        for (int i = 0; i < properties.size(); i++)
        {
            if (CollectionUtils.inRange(state.selected, i))
            {
                properties.get(i).selection.clear();
                properties.get(i).selection.addAll(state.selected.get(i));
            }
        }

        this.currentGraph.pickSelected();
    }

    public void copyViewport(UIKeyframes lastEditor)
    {
        this.getDopeSheet().setTrackHeight(lastEditor.getDopeSheet().getTrackHeight());
        this.getXAxis().copy(lastEditor.getXAxis());
        this.getDopeSheet().getYAxis().copy(lastEditor.getDopeSheet().getYAxis());
    }

    public static class PastedKeyframes
    {
        public IKeyframeFactory factory;
        public List<Keyframe> keyframes = new ArrayList<>();

        public PastedKeyframes(IKeyframeFactory factory)
        {
            this.factory = factory;
        }
    }

    private static class SheetCache
    {
        public List<Pair<BaseType, UIKeyframeSheet>> data = new ArrayList<>();

        public SheetCache(Collection<UIKeyframeSheet> sheets)
        {
            for (UIKeyframeSheet sheet : sheets)
            {
                if (sheet.selection.hasAny())
                {
                    this.data.add(new Pair<>(sheet.channel.toData(), sheet));
                }
            }
        }
    }
}