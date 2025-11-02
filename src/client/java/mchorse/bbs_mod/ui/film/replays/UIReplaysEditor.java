package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.SoundBuffer;
import mchorse.bbs_mod.audio.Waveform;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.CameraUtils;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.AnimationPart;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.ReplayKeyframes;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.renderer.IUIClipRenderer;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIReplaysOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIKeyframeSheetFilterOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIAnimationToPoseOverlayPanel;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Scale;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class UIReplaysEditor extends UIElement
{
    private static final Map<String, Integer> COLORS = new HashMap<>();
    private static final Map<String, Icon> ICONS = new HashMap<>();
    private static String lastFilm = "";
    private static int lastReplay;

    public UIReplaysOverlayPanel replays;

    /* Keyframes */
    public UIKeyframeEditor keyframeEditor;

    /* Clips */
    private UIFilmPanel filmPanel;
    private Film film;
    private Replay replay;
    private Set<String> keys = new LinkedHashSet<>();

    static
    {
        COLORS.put("x", Colors.RED);
        COLORS.put("y", Colors.GREEN);
        COLORS.put("z", Colors.BLUE);
        COLORS.put("vX", Colors.RED);
        COLORS.put("vY", Colors.GREEN);
        COLORS.put("vZ", Colors.BLUE);
        COLORS.put("yaw", Colors.YELLOW);
        COLORS.put("pitch", Colors.CYAN);
        COLORS.put("bodyYaw", Colors.MAGENTA);

        COLORS.put("stick_lx", Colors.RED);
        COLORS.put("stick_ly", Colors.GREEN);
        COLORS.put("stick_rx", Colors.RED);
        COLORS.put("stick_ry", Colors.GREEN);
        COLORS.put("trigger_l", Colors.RED);
        COLORS.put("trigger_r", Colors.GREEN);
        COLORS.put("extra1_x", Colors.RED);
        COLORS.put("extra1_y", Colors.GREEN);
        COLORS.put("extra2_x", Colors.RED);
        COLORS.put("extra2_y", Colors.GREEN);

        COLORS.put("visible", Colors.WHITE & Colors.RGB);
        COLORS.put("pose", Colors.RED);
        COLORS.put("pose_overlay", Colors.ORANGE);
        COLORS.put("transform", Colors.GREEN);
        COLORS.put("transform_overlay", 0xaaff00);
        COLORS.put("color", Colors.INACTIVE);
        COLORS.put("lighting", Colors.YELLOW);
        COLORS.put("shape_keys", Colors.PINK);
        COLORS.put("actions", Colors.MAGENTA);

        COLORS.put("item_main_hand", Colors.ORANGE);
        COLORS.put("item_off_hand", Colors.ORANGE);
        COLORS.put("item_head", Colors.ORANGE);
        COLORS.put("item_chest", Colors.ORANGE);
        COLORS.put("item_legs", Colors.ORANGE);
        COLORS.put("item_feet", Colors.ORANGE);

        COLORS.put("user1", Colors.RED);
        COLORS.put("user2", Colors.ORANGE);
        COLORS.put("user3", Colors.GREEN);
        COLORS.put("user4", Colors.BLUE);
        COLORS.put("user5", Colors.RED);
        COLORS.put("user6", Colors.ORANGE);

        COLORS.put("frequency", Colors.RED);
        COLORS.put("count", Colors.GREEN);

        COLORS.put("settings", Colors.MAGENTA);
        COLORS.put("offset_x", Colors.RED);
        COLORS.put("offset_y", Colors.GREEN);
        COLORS.put("offset_z", Colors.BLUE);

        ICONS.put("x", Icons.X);
        ICONS.put("y", Icons.Y);
        ICONS.put("z", Icons.Z);

        ICONS.put("visible", Icons.VISIBLE);
        ICONS.put("texture", Icons.MATERIAL);
        ICONS.put("pose", Icons.POSE);
        ICONS.put("transform", Icons.ALL_DIRECTIONS);
        ICONS.put("color", Icons.BUCKET);
        ICONS.put("lighting", Icons.LIGHT);
        ICONS.put("actions", Icons.CONVERT);
        ICONS.put("shape_keys", Icons.HEART_ALT);
        ICONS.put("text", Icons.FONT);

        ICONS.put("stick_lx", Icons.LEFT_STICK);
        ICONS.put("stick_rx", Icons.RIGHT_STICK);
        ICONS.put("trigger_l", Icons.TRIGGER);
        ICONS.put("extra1_x", Icons.CURVES);
        ICONS.put("extra2_x", Icons.CURVES);
        ICONS.put("item_main_hand", Icons.LIMB);

        ICONS.put("user1", Icons.PARTICLE);

        ICONS.put("paused", Icons.TIME);
        ICONS.put("frequency", Icons.STOPWATCH);
        ICONS.put("count", Icons.BUCKET);

        ICONS.put("settings", Icons.GEAR);

        /* Structure selection icon for structure_file property */
        ICONS.put("structure_file", Icons.FILE);
    }

    public static Icon getIcon(String key)
    {
        String topLevel = StringUtils.fileName(key);

        return ICONS.getOrDefault(topLevel, Icons.NONE);
    }

    public static int getColor(String key)
    {
        String topLevel = StringUtils.fileName(key);

        if (topLevel.startsWith("pose_overlay")) return COLORS.get("pose_overlay");
        if (topLevel.startsWith("transform_overlay")) return COLORS.get("transform_overlay");

        return COLORS.getOrDefault(topLevel, Colors.ACTIVE);
    }

    public static void offerAdjacent(UIContext context, Form form, String bone, Consumer<String> consumer)
    {
        if (!bone.isEmpty() && form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model == null)
            {
                return;
            }

            context.replaceContextMenu((menu) ->
            {
                for (String modelGroup : model.model.getAdjacentGroups(bone))
                {
                    menu.action(Icons.LIMB, IKey.constant(modelGroup), () -> consumer.accept(modelGroup));
                }

                menu.autoKeys();
            });
        }
    }

    public static void offerHierarchy(UIContext context, Form form, String bone, Consumer<String> consumer)
    {
        if (!bone.isEmpty() && form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model == null)
            {
                return;
            }

            context.replaceContextMenu((menu) ->
            {
                for (String modelGroup : model.model.getHierarchyGroups(bone))
                {
                    menu.action(Icons.LIMB, IKey.constant(modelGroup), () -> consumer.accept(modelGroup));
                }

                menu.autoKeys();
            });
        }
    }

    public static boolean renderBackground(UIContext context, UIKeyframes keyframes, Clips camera, int clipOffset)
    {
        if (!BBSSettings.audioWaveformVisible.get())
        {
            return false;
        }

        Scale scale = keyframes.getXAxis();
        boolean renderedOnce = false;

        for (Clip clip : camera.get())
        {
            if (clip instanceof AudioClip audioClip)
            {
                Link link = audioClip.audio.get();

                if (link == null)
                {
                    continue;
                }

                SoundBuffer buffer = BBSModClient.getSounds().get(link, true);

                if (buffer == null || buffer.getWaveform() == null)
                {
                    continue;
                }

                Waveform wave = buffer.getWaveform();

                if (wave != null)
                {
                    int audioOffset = audioClip.offset.get();
                    float offset = audioClip.tick.get() - clipOffset;
                    int duration = Math.min((int) (wave.getDuration() * 20), clip.duration.get());

                    int x1 = (int) scale.to(offset);
                    int x2 = (int) scale.to(offset + duration);

                    wave.render(context.batcher, Colors.WHITE, x1, keyframes.area.y + 15, x2 - x1, 20, TimeUtils.toSeconds(audioOffset), TimeUtils.toSeconds(audioOffset + duration));

                    renderedOnce = true;
                }
            }
        }

        return renderedOnce;
    }

    public UIReplaysEditor(UIFilmPanel filmPanel)
    {
        this.filmPanel = filmPanel;
        this.replays = new UIReplaysOverlayPanel(filmPanel, (replay) -> this.setReplay(replay, false, true));

        this.markContainer();
    }

    public void setFilm(Film film)
    {
        this.film = film;

        if (film != null)
        {
            List<Replay> replays = film.replays.getList();
            int index = film.getId().equals(lastFilm) ? lastReplay : 0;

            if (!CollectionUtils.inRange(replays, index))
            {
                index = 0;
            }

            this.replays.replays.setList(replays);
            this.setReplay(replays.isEmpty() ? null : replays.get(index));
        }
    }

    public Replay getReplay()
    {
        return this.replay;
    }

    public void setReplay(Replay replay)
    {
        this.setReplay(replay, true, true);
    }

    public void setReplay(Replay replay, boolean select, boolean resetOrbit)
    {
        this.replay = replay;

        if (resetOrbit)
        {
            this.filmPanel.getController().orbit.reset();
        }

        this.replays.setReplay(replay);
        this.filmPanel.actionEditor.setClips(replay == null ? null : replay.actions);
        this.updateChannelsList();

        if (select)
        {
            this.replays.replays.setCurrentScroll(replay);
        }
    }

    public void moveReplay(double x, double y, double z)
    {
        if (this.replay != null)
        {
            int cursor = this.filmPanel.getCursor();

            this.replay.keyframes.x.insert(cursor, x);
            this.replay.keyframes.y.insert(cursor, y);
            this.replay.keyframes.z.insert(cursor, z);
        }
    }

    public void updateChannelsList()
    {
        UIKeyframes lastEditor = null;

        if (this.keyframeEditor != null)
        {
            this.keyframeEditor.removeFromParent();

            lastEditor = this.keyframeEditor.view;
        }

        if (this.replay == null)
        {
            return;
        }

        /* Replay keyframes */
        List<UIKeyframeSheet> sheets = new ArrayList<>();

        for (String key : ReplayKeyframes.CURATED_CHANNELS)
        {
            BaseValue value = this.replay.keyframes.get(key);
            KeyframeChannel channel = (KeyframeChannel) value;

            String customTitle = this.replay.getCustomSheetTitle(key);
            UIKeyframeSheet sheet = customTitle != null && !customTitle.isEmpty()
                ? new UIKeyframeSheet(key, IKey.constant(customTitle), getColor(key), false, channel, null)
                : new UIKeyframeSheet(getColor(key), false, channel, null);

            /* Restaurar estado de anclaje desde título personalizado para canales de pose */
            if (customTitle != null && !customTitle.isEmpty() && channel.getFactory() == KeyframeFactories.POSE)
            {
                sheet.anchoredBone = customTitle;
            }

            sheets.add(sheet.icon(ICONS.get(key)));
        }

        /* Form properties */
        for (String key : FormUtils.collectPropertyPaths(this.replay.form.get()))
        {
            KeyframeChannel property = this.replay.properties.getOrCreate(this.replay.form.get(), key);

            if (property != null)
            {
                BaseValueBasic formProperty = FormUtils.getProperty(this.replay.form.get(), key);
                String customTitle = this.replay.getCustomSheetTitle(key);
                UIKeyframeSheet sheet = customTitle != null && !customTitle.isEmpty()
                    ? new UIKeyframeSheet(key, IKey.constant(customTitle), getColor(key), false, property, formProperty)
                    : new UIKeyframeSheet(getColor(key), false, property, formProperty);

                /* Restaurar estado de anclaje desde título personalizado para propiedades de pose */
                if (customTitle != null && !customTitle.isEmpty() && property.getFactory() == KeyframeFactories.POSE)
                {
                    sheet.anchoredBone = customTitle;
                }

                sheets.add(sheet.icon(getIcon(key)));
            }
        }

        this.keys.clear();

        for (UIKeyframeSheet sheet : sheets)
        {
            this.keys.add(StringUtils.fileName(sheet.id));
        }

        sheets.removeIf((v) ->
        {
            for (String s : BBSSettings.disabledSheets.get())
            {
                if (v.id.equals(s) || v.id.endsWith("/" + s))
                {
                    return true;
                }
            }

            return false;
        });

        Object lastForm = null;

        for (UIKeyframeSheet sheet : sheets)
        {
            Object form = sheet.property == null ? null : FormUtils.getForm(sheet.property);

            if (!Objects.equals(lastForm, form))
            {
                sheet.separator = true;
            }

            lastForm = form;
        }

        if (!sheets.isEmpty())
        {
            this.keyframeEditor = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.filmPanel.cameraEditor, consumer).absolute()).target(this.filmPanel.editArea);
            this.keyframeEditor.full(this);
            this.keyframeEditor.setUndoId("replay_keyframe_editor");

            /* Reset */
            if (lastEditor != null)
            {
                this.keyframeEditor.view.copyViewport(lastEditor);
            }

            this.keyframeEditor.view.backgroundRenderer((context) ->
            {
                UIKeyframes view = this.keyframeEditor.view;
                boolean yes = renderBackground(context, view, this.film.camera, 0);
                int shift = yes ? 35 : 15;

                UIClipsPanel cameraEditor = this.filmPanel.cameraEditor;
                Clip clip = cameraEditor.getClip();

                if (clip != null && BBSSettings.editorClipPreview.get())
                {
                    IUIClipRenderer<Clip> renderer = cameraEditor.clips.getRenderers().get(clip);
                    Scale scale = view.getXAxis();
                    Area area = new Area();

                    float offset = clip.tick.get();
                    int duration = clip.duration.get();
                    int x1 = (int) scale.to(offset);
                    int x2 = (int) scale.to(offset + duration);

                    area.setPoints(x1, view.area.y + shift, x2, view.area.y + shift + 20);
                    renderer.renderClip(context, cameraEditor.clips, clip, area, true, true);
                }
            });
            this.keyframeEditor.view.duration(() -> this.film.camera.calculateDuration());
            this.keyframeEditor.view.context((menu) ->
            {
                if (this.replay.form.get() instanceof ModelForm modelForm)
                {
                    int mouseY = this.getContext().mouseY;
                    UIKeyframeSheet sheet = this.keyframeEditor.view.getGraph().getSheet(mouseY);

                    if (sheet != null && sheet.channel.getFactory() == KeyframeFactories.POSE && sheet.id.equals("pose"))
                    {
                        menu.action(Icons.POSE, UIKeys.FILM_REPLAY_CONTEXT_ANIMATION_TO_KEYFRAMES, () -> this.animationToPoses(modelForm, sheet));
                    }
                }

                int mouseY2 = this.getContext().mouseY;
                UIKeyframeSheet clickedSheet = this.keyframeEditor.view.getGraph().getSheet(mouseY2);
                if (clickedSheet != null)
                {
                    menu.action(Icons.FONT, UIKeys.FILM_REPLAY_RENAME_SHEET, () ->
                    {
                        UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                            UIKeys.FILM_REPLAY_RENAME_SHEET_TITLE,
                            UIKeys.FILM_REPLAY_RENAME_SHEET_MESSAGE,
                            (str) ->
                            {
                                this.replay.setCustomSheetTitle(clickedSheet.id, str);
                                this.updateChannelsList();
                            }
                        );

                        panel.text.setText(clickedSheet.title.get());
                        UIOverlay.addOverlay(this.getContext(), panel, 300, 0.25F);
                    });
                }

                if (this.keyframeEditor.view.getGraph() instanceof UIKeyframeDopeSheet)
                {
                    menu.action(Icons.FILTER, UIKeys.FILM_REPLAY_FILTER_SHEETS, () ->
                    {
                        UIKeyframeSheetFilterOverlayPanel panel = new UIKeyframeSheetFilterOverlayPanel(BBSSettings.disabledSheets.get(), this.keys);

                        UIOverlay.addOverlay(this.getContext(), panel, 240, 0.9F);

                        panel.onClose((e) ->
                        {
                            this.updateChannelsList();
                            BBSSettings.disabledSheets.set(BBSSettings.disabledSheets.get());
                        });
                    });
                }
            });

            for (UIKeyframeSheet sheet : sheets)
            {
                this.keyframeEditor.view.addSheet(sheet);
            }

            this.add(this.keyframeEditor);
        }

        this.resize();

        if (this.keyframeEditor != null && lastEditor == null)
        {
            this.keyframeEditor.view.resetView();
        }
    }

    private void animationToPoses(ModelForm modelForm, UIKeyframeSheet sheet)
    {
        ModelInstance model = ModelFormRenderer.getModel(modelForm);

        if (model != null)
        {
            UIAnimationToPoseOverlayPanel.IUIAnimationPoseCallback cb = (animationKey, onlyKeyframes, length, step) ->
                this.animationToPoseKeyframes(modelForm, sheet, animationKey, onlyKeyframes, length, step);

            UIOverlay.addOverlay(this.getContext(), new UIAnimationToPoseOverlayPanel(cb, modelForm, sheet), 200, 197);
        }
    }

    public void animationToPoseKeyframes(ModelForm modelForm, UIKeyframeSheet sheet, String animationKey, boolean onlyKeyframes, int length, int step)
    {
        ModelInstance model = ModelFormRenderer.getModel(modelForm);
        Animation animation = model.animations.get(animationKey);

        if (animation != null)
        {
            int current = this.filmPanel.getCursor();
            IEntity entity = this.filmPanel.getController().getCurrentEntity();

            this.keyframeEditor.view.getDopeSheet().clearSelection();

            if (onlyKeyframes)
            {
                List<Float> list = this.getTicks(animation);

                for (float i : list)
                {
                    this.fillAnimationPose(sheet, i, model, entity, animation, current);
                }
            }
            else
            {
                for (int i = 0; i < length; i += step)
                {
                    this.fillAnimationPose(sheet, i, model, entity, animation, current);
                }
            }

            this.keyframeEditor.view.getDopeSheet().pickSelected();
        }
    }

    private List<Float> getTicks(Animation animation)
    {
        Set<Float> integers = new HashSet<>();

        for (AnimationPart value : animation.parts.values())
        {
            for (KeyframeChannel<MolangExpression> channel : value.channels)
            {
                for (Keyframe<MolangExpression> keyframe : channel.getKeyframes())
                {
                    integers.add(keyframe.getTick());
                }
            }
        }

        ArrayList<Float> ticks = new ArrayList<>(integers);

        Collections.sort(ticks);

        return ticks;
    }

    private void fillAnimationPose(UIKeyframeSheet sheet, float i, ModelInstance model, IEntity entity, Animation animation, int current)
    {
        model.model.resetPose();
        model.model.apply(entity, animation, i, 1F, 0F, false);

        int insert = sheet.channel.insert(current + i, model.model.createPose());

        sheet.selection.add(insert);
    }

    public void pickForm(Form form, String bone)
    {
        String path = FormUtils.getPath(form);

        if (this.keyframeEditor == null || bone.isEmpty())
        {
            return;
        }

        Keyframe selected = this.keyframeEditor.view.getGraph().getSelected();
        String type = "pose";

        if (selected != null)
        {
            String id = selected.getParent().getId();
            int index = id.indexOf("pose_overlay");

            if (index >= 0)
            {
                type = id.substring(index);
            }
        }

        this.pickProperty(bone, StringUtils.combinePaths(path, type), false);
    }

    public void pickFormProperty(Form form, String bone)
    {
        String path = FormUtils.getPath(form);
        boolean shift = Window.isShiftPressed();
        ContextMenuManager manager = new ContextMenuManager();

        manager.autoKeys();

        for (BaseValueBasic formProperty : form.getAllMap().values())
        {
            if (!formProperty.isVisible())
            {
                continue;
            }

            manager.action(getIcon(formProperty.getId()), IKey.constant(formProperty.getId()), () ->
            {
                this.pickProperty(bone, StringUtils.combinePaths(path, formProperty.getId()), shift);
            });
        }

        this.getContext().replaceContextMenu(manager.create());
    }

    private void pickProperty(String bone, String key, boolean insert)
    {
        /* Redirección al sheet anclado si el hueso seleccionado está anclado y no hay override */
        if (bone != null && !bone.isEmpty() && BBSSettings.boneAnchoringEnabled.get() && !BBSSettings.anchorOverrideEnabled.get())
        {
            for (UIKeyframeSheet s : this.keyframeEditor.view.getGraph().getSheets())
            {
                if (s.anchoredBone != null && s.anchoredBone.equals(bone))
                {
                    this.pickProperty(bone, s, insert);
                    return;
                }
            }
        }

        for (UIKeyframeSheet sheet : this.keyframeEditor.view.getGraph().getSheets())
        {
            BaseValueBasic property = sheet.property;

            if (property != null && FormUtils.getPropertyPath(property).equals(key))
            {
                this.pickProperty(bone, sheet, insert);

                break;
            }
        }
    }

    private void pickProperty(String bone, UIKeyframeSheet sheet, boolean insert)
    {
        int tick = this.filmPanel.getRunner().ticks;

        if (insert)
        {
            Keyframe keyframe = this.keyframeEditor.view.getGraph().addKeyframe(sheet, tick, null);

            this.keyframeEditor.view.getGraph().selectKeyframe(keyframe);

            return;
        }

        KeyframeSegment segment = sheet.channel.find(tick);

        if (segment != null)
        {
            Keyframe closest = segment.getClosest();

            if (this.keyframeEditor.view.getGraph().getSelected() != closest)
            {
                this.keyframeEditor.view.getGraph().selectKeyframe(closest);
            }

            if (this.keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory)
            {
                String targetBone = bone;

                if (BBSSettings.boneAnchoringEnabled.get() && sheet.anchoredBone != null)
                {
                    /* Redirigir siempre al hueso anclado cuando la pista está anclada */
                    targetBone = sheet.anchoredBone;
                }

                poseFactory.poseEditor.selectBone(targetBone);
            }

            this.filmPanel.setCursor((int) closest.getTick());
        }
    }

    public boolean clickViewport(UIContext context, Area area)
    {
        if (this.filmPanel.isFlying())
        {
            return false;
        }

        StencilFormFramebuffer stencil = this.filmPanel.getController().getStencil();

        if (stencil.hasPicked())
        {
            Pair<Form, String> pair = stencil.getPicked();

            if (pair != null && context.mouseButton < 2)
            {
                if (!this.isVisible())
                {
                    this.filmPanel.showPanel(this);
                }

                if (context.mouseButton == 0)
                {
                    if (Window.isCtrlPressed()) offerAdjacent(this.getContext(), pair.a, pair.b, (bone) -> this.pickForm(pair.a, bone));
                    else if (Window.isShiftPressed()) offerHierarchy(this.getContext(), pair.a, pair.b, (bone) -> this.pickForm(pair.a, bone));
                    else this.pickForm(pair.a, pair.b);

                    return true;
                }
                else if (context.mouseButton == 1)
                {
                    this.pickFormProperty(pair.a, pair.b);

                    return true;
                }
            }
        }
        else if (context.mouseButton == 1 && this.isVisible())
        {
            World world = MinecraftClient.getInstance().world;
            Camera camera = this.filmPanel.getCamera();

            BlockHitResult blockHitResult = RayTracing.rayTrace(
                world,
                RayTracing.fromVector3d(camera.position),
                RayTracing.fromVector3f(CameraUtils.getMouseDirection(camera.projection, camera.view, context.mouseX, context.mouseY, area.x, area.y, area.w, area.h)),
                256F
            );

            if (blockHitResult.getType() != HitResult.Type.MISS)
            {
                Vector3d vec = new Vector3d(blockHitResult.getPos().x, blockHitResult.getPos().y, blockHitResult.getPos().z);

                if (Window.isShiftPressed())
                {
                    vec = new Vector3d(Math.floor(vec.x) + 0.5D, Math.round(vec.y), Math.floor(vec.z) + 0.5D);
                }

                final Vector3d finalVec = vec;

                context.replaceContextMenu((menu) ->
                {
                    float pitch = 0F;
                    float yaw = MathUtils.toDeg(camera.rotation.y);

                    menu.action(Icons.ADD, UIKeys.FILM_REPLAY_CONTEXT_ADD, () -> this.replays.replays.addReplay(finalVec, pitch, yaw));
                    menu.action(Icons.POINTER, UIKeys.FILM_REPLAY_CONTEXT_MOVE_HERE, () -> this.moveReplay(finalVec.x, finalVec.y, finalVec.z));
                });

                return true;
            }
        }

        if (area.isInside(context) && this.filmPanel.getController().orbit.enabled)
        {
            this.filmPanel.getController().orbit.start(context);

            return true;
        }

        return false;
    }

    public void close()
    {
        if (this.film != null)
        {
            lastFilm = this.film.getId();
            lastReplay = this.replays.replays.getIndex();
        }
    }

    public void teleport()
    {
        if (this.filmPanel.getData() == null)
        {
            return;
        }

        Replay replay = this.getReplay();

        if (replay != null)
        {
            int tick = this.filmPanel.getCursor();
            double x = replay.keyframes.x.interpolate(tick);
            double y = replay.keyframes.y.interpolate(tick);
            double z = replay.keyframes.z.interpolate(tick);
            float yaw = replay.keyframes.yaw.interpolate(tick).floatValue();
            float headYaw = replay.keyframes.headYaw.interpolate(tick).floatValue();
            float bodyYaw = replay.keyframes.bodyYaw.interpolate(tick).floatValue();
            float pitch = replay.keyframes.pitch.interpolate(tick).floatValue();
            ClientPlayerEntity player = MinecraftClient.getInstance().player;

            PlayerUtils.teleport(x, y, z, headYaw, pitch);
            player.setYaw(yaw);
            player.setHeadYaw(headYaw);
            player.setBodyYaw(bodyYaw);
            player.setPitch(pitch);
        }
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        List<Integer> selection = DataStorageUtils.intListFromData(data.getList("selection"));
        List<Integer> currentIndices = this.replays.replays.getCurrentIndices();

        this.setReplay(CollectionUtils.getSafe(this.film.replays.getList(), data.getInt("replay")), true, false);

        currentIndices.clear();
        currentIndices.addAll(selection);
        this.replays.replays.update();
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        int index = this.film.replays.getList().indexOf(this.getReplay());

        data.putInt("replay", index);
        data.put("selection", DataStorageUtils.intListToData(this.replays.replays.getCurrentIndices()));
    }
}