package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.context.UIInterpolationContextMenu;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragEndEvent;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragStartEvent;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.IAxisConverter;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.IKeyframeShapeRenderer;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.KeyframeShapeRenderers;
import mchorse.bbs_mod.ui.framework.tooltips.InterpolationTooltip;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeShape;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.HashMap;
import java.util.Map;

public abstract class UIKeyframeFactory <T> extends UIElement
{
    private static final Map<IKeyframeFactory, IUIKeyframeFactoryFactory> FACTORIES = new HashMap<>();
    private static final Map<IKeyframeFactory, Integer> SCROLLS = new HashMap<>();

    public UIScrollView scroll;
    public UITrackpad tick;
    public UITrackpad duration;
    public UIIcon interp;

    public UIIcon shape;
    public UIColor color;

    protected Keyframe<T> keyframe;
    protected UIKeyframes editor;

    static
    {
        register(KeyframeFactories.ANCHOR, UIAnchorKeyframeFactory::new);
        register(KeyframeFactories.BOOLEAN, UIBooleanKeyframeFactory::new);
        register(KeyframeFactories.COLOR, UIColorKeyframeFactory::new);
        register(KeyframeFactories.FLOAT, UIFloatKeyframeFactory::new);
        register(KeyframeFactories.DOUBLE, UIDoubleKeyframeFactory::new);
        register(KeyframeFactories.INTEGER, UIIntegerKeyframeFactory::new);
        register(KeyframeFactories.LINK, UILinkKeyframeFactory::new);
        register(KeyframeFactories.POSE, UIPoseKeyframeFactory::new);
        register(KeyframeFactories.STRING, UIStringKeyframeFactory::new);
        register(KeyframeFactories.TRANSFORM, UITransformKeyframeFactory::new);
        register(KeyframeFactories.VECTOR4F, UIVector4fKeyframeFactory::new);
        register(KeyframeFactories.BLOCK_STATE, UIBlockStateKeyframeFactory::new);
        register(KeyframeFactories.ITEM_STACK, UIItemStackKeyframeFactory::new);
        register(KeyframeFactories.ACTIONS_CONFIG, UIActionsConfigKeyframeFactory::new);
        register(KeyframeFactories.SHAPE_KEYS, UIShapeKeysKeyframeFactory::new);
        register(KeyframeFactories.PARTICLE_SETTINGS, UIParticleSettingsKeyframeFactory::new);
        register(KeyframeFactories.PIVOT_SETTINGS, UIPivotSettingsKeyframeFactory::new);
        register(KeyframeFactories.STRUCTURE_LIGHT_SETTINGS, UIStructureLightSettingsKeyframeFactory::new);
    }

    public static <T> void register(IKeyframeFactory<T> clazz, IUIKeyframeFactoryFactory<T> factory)
    {
        FACTORIES.put(clazz, factory);
    }

    public static void saveScroll(UIKeyframeFactory editor)
    {
        if (editor != null)
        {
            SCROLLS.put(editor.keyframe.getFactory(), (int) editor.scroll.scroll.getScroll());
        }
    }

    public static <T> UIKeyframeFactory createPanel(Keyframe<T> keyframe, UIKeyframes editor)
    {
        IUIKeyframeFactoryFactory<T> factory = FACTORIES.get(keyframe.getFactory());
        UIKeyframeFactory uiEditor = factory == null ? null : factory.create(keyframe, editor);

        if (uiEditor != null)
        {
            uiEditor.scroll.scroll.setScroll(SCROLLS.getOrDefault(keyframe.getFactory(), 0));
        }

        return uiEditor;
    }

    public UIKeyframeFactory(Keyframe<T> keyframe, UIKeyframes editor)
    {
        this.keyframe = keyframe;
        this.editor = editor;

        this.scroll = UI.scrollView(5, 10);
        this.scroll.scroll.cancelScrolling();
        this.scroll.full(this);

        this.tick = new UITrackpad(this::setTick);
        this.tick.limit(Float.MIN_VALUE, Float.MAX_VALUE).tooltip(UIKeys.KEYFRAMES_TICK);
        this.tick.getEvents().register(UITrackpadDragStartEvent.class, (e) -> this.editor.cacheKeyframes());
        this.tick.getEvents().register(UITrackpadDragEndEvent.class, (e) -> this.editor.submitKeyframes());
        this.duration = new UITrackpad((v) -> this.setDuration(v.floatValue()));
        this.duration.limit(0, Float.MAX_VALUE).tooltip(UIKeys.KEYFRAMES_FORCED_DURATION);
        this.interp = new UIIcon(Icons.GRAPH, (b) ->
        {
            Interpolation interp = this.keyframe.getInterpolation();
            UIInterpolationContextMenu menu = new UIInterpolationContextMenu(interp);

            this.getContext().replaceContextMenu(menu.callback(() -> this.editor.getGraph().setInterpolation(interp)));
        });
        this.interp.tooltip(new InterpolationTooltip(0F, 0.5F, () -> this.keyframe.getInterpolation()));
        this.interp.keys().register(Keys.KEYFRAMES_INTERP, this.interp::clickItself).category(UIKeys.KEYFRAMES_KEYS_CATEGORY);

        this.color = new UIColor((c) ->
        {
            for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
            {
                for (Keyframe kf : sheet.selection.getSelected()) kf.setColor(new Color().set(c));
            }
        });
        this.color.setColor(keyframe.getColor() == null ? 0 : keyframe.getColor().getRGBColor());
        this.color.noLabel().w(40).tooltip(UIKeys.KEYFRAMES_CHANGE_COLOR);
        this.color.context((menu) ->
        {
            menu.action(Icons.COLOR, UIKeys.KEYFRAMES_RESET_COLOR, () ->
            {
                for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
                {
                    for (Keyframe kf : sheet.selection.getSelected()) kf.setColor(null);
                }

                this.color.setColor(0);
            });
        });

        this.shape = new UIIcon(Icons.SHAPES, (b) ->
        {
            KeyframeShape currentShape = keyframe.getShape() == null ? KeyframeShape.SQUARE : keyframe.getShape();

            this.getContext().replaceContextMenu((menu) ->
            {
                for (KeyframeShape shape : KeyframeShape.values())
                {
                    IKeyframeShapeRenderer shapeRenderer = KeyframeShapeRenderers.SHAPES.get(shape);

                    menu.action(shapeRenderer.getIcon(), shapeRenderer.getLabel(), shape == currentShape, () ->
                    {
                        for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
                        {
                            for (Keyframe kf : sheet.selection.getSelected())
                            {
                                kf.setShape(shape);
                            }
                        }
                    });
                }
            });
        });
        this.shape.tooltip(UIKeys.KEYFRAMES_CHANGE_SHAPE);

        this.scroll.add(UI.row(this.interp, this.tick, this.duration, this.color, this.shape));

        this.add(this.scroll);

        /* Fill data */
        IAxisConverter converter = this.editor.getConverter();

        this.tick.setValue(converter == null ? keyframe.getTick() : converter.to(keyframe.getTick()));
        this.duration.setValue(converter == null ? keyframe.getDuration() : converter.to(keyframe.getDuration()));
    }

    public Keyframe<T> getKeyframe()
    {
        return keyframe;
    }

    public void setTick(double tick)
    {
        IAxisConverter converter = this.editor.getConverter();
        float time = (float) (converter == null ? tick : converter.from(tick));

        this.editor.getGraph().setTick(time, false);
    }

    public void setDuration(float value)
    {
        this.editor.getGraph().setDuration(value);
    }

    public void setValue(Object value)
    {
        this.editor.getGraph().setValue(value, true);
    }

    public void update()
    {
        IAxisConverter converter = this.editor.getConverter();

        this.tick.setValue(converter == null ? this.keyframe.getTick() : converter.to(this.keyframe.getTick()));
    }

    public static interface IUIKeyframeFactoryFactory <T>
    {
        public UIKeyframeFactory<T> create(Keyframe<T> keyframe, UIKeyframes editor);
    }
}