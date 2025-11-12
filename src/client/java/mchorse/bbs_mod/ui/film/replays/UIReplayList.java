package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.modifiers.EntityClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.film.replays.Replays;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIReplaysOverlayPanel;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UINumberOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.RayTracing;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This GUI is responsible for drawing replays available in the 
 * director thing
 */
public class UIReplayList extends UIList<Replay>
{
    private static String LAST_PROCESS = "v";
    private static String LAST_OFFSET = "0";
    private static List<String> LAST_PROCESS_PROPERTIES = Arrays.asList("x");

    public UIFilmPanel panel;
    public UIReplaysOverlayPanel overlay;

    /* Modo categorías de grupos */
    private boolean groupCategoriesEnabled = true;
    private java.util.Map<String, Boolean> expandedGroups = new java.util.HashMap<>();
    private java.util.List<Row> groupedRows = new java.util.ArrayList<>();
    private static final String EMPTY_GROUP_LABEL = "(sin grupo)";
    /* Estado de arrastre en modo agrupado */
    private int groupedDragFrom = -1;
    private long groupedDragTime;
    private int groupedDragStartX;
    private int groupedDragStartY;
    /* Verdadero mientras el botón izquierdo está sostenido desde el click inicial */
    private boolean groupedDragHolding;

    private static class Row
    {
        final boolean header;
        final String group;
        final Replay replay; // null si es header

        Row(String group, boolean header, Replay replay)
        {
            this.group = group;
            this.header = header;
            this.replay = replay;
        }
    }

    public UIReplayList(Consumer<List<Replay>> callback, UIReplaysOverlayPanel overlay, UIFilmPanel panel)
    {
        super(callback);

        this.overlay = overlay;
        this.panel = panel;

        this.multi().sorting();
        this.context((menu) ->
        {
            menu.action(Icons.ADD, UIKeys.SCENE_REPLAYS_CONTEXT_ADD, this::addReplay);

            if (this.isSelected())
            {
                menu.action(Icons.COPY, UIKeys.SCENE_REPLAYS_CONTEXT_COPY, this::copyReplay);
            }

            MapType copyReplay = Window.getClipboardMap("_CopyReplay");

            if (copyReplay != null)
            {
                menu.action(Icons.PASTE, UIKeys.SCENE_REPLAYS_CONTEXT_PASTE, () -> this.pasteReplay(copyReplay));
            }

            int duration = this.panel.getData().camera.calculateDuration();

            if (duration > 0)
            {
                menu.action(Icons.PLAY, UIKeys.SCENE_REPLAYS_CONTEXT_FROM_CAMERA, () -> this.fromCamera(duration));
            }

            menu.action(Icons.BLOCK, UIKeys.SCENE_REPLAYS_CONTEXT_FROM_MODEL_BLOCK, this::fromModelBlock);

            if (this.isSelected())
            {
                boolean shift = Window.isShiftPressed();
                MapType data = Window.getClipboardMap("_CopyKeyframes");

                menu.action(Icons.ALL_DIRECTIONS, UIKeys.SCENE_REPLAYS_CONTEXT_PROCESS, this::processReplays);
                menu.action(Icons.TIME, UIKeys.SCENE_REPLAYS_CONTEXT_OFFSET_TIME, this::offsetTimeReplays);

                // Submenú de opciones de grupo entre desfase y duplicar
                menu.action(Icons.FOLDER, UIKeys.SCENE_REPLAYS_GROUP_OPTIONS, () ->
                {
                    this.getContext().replaceContextMenu((submenu) ->
                    {
                        // 1. Asignar grupo a las reproducciones seleccionadas
                        submenu.action(Icons.ADD, UIKeys.SCENE_REPLAYS_GROUP_ASSIGN, () ->
                        {
                            UIPromptOverlayPanel prompt = new UIPromptOverlayPanel(
                                UIKeys.SCENE_REPLAYS_GROUP_ASSIGN_TITLE,
                                UIKeys.SCENE_REPLAYS_GROUP_ASSIGN_DESCRIPTION,
                                (text) ->
                                {
                                    String g = text == null ? "" : text.trim();
                                    for (Replay replay : this.getCurrent())
                                    {
                                        replay.group.set(g);
                                    }
                                    this.update();
                                }
                            );

                            UIOverlay.addOverlay(this.getContext(), prompt);
                        });

                        // 2. Cambiar nombre de grupo…
                        submenu.action(Icons.EDIT, UIKeys.SCENE_REPLAYS_GROUP_RENAME, () ->
                        {
                            String current = this.getCurrentFirst() != null ? this.getCurrentFirst().group.get() : "";
                            java.util.function.Consumer<String> doRename = (oldName) ->
                            {
                                UIPromptOverlayPanel renamePrompt = new UIPromptOverlayPanel(
                                    UIKeys.SCENE_REPLAYS_GROUP_RENAME_TITLE,
                                    UIKeys.SCENE_REPLAYS_GROUP_RENAME_DESCRIPTION,
                                    (newName) ->
                                    {
                                        String nn = newName == null ? "" : newName.trim();
                                        this.renameGroup(oldName, nn);
                                    }
                                );
                                UIOverlay.addOverlay(this.getContext(), renamePrompt);
                            };

                            if (current != null && !current.isEmpty())
                            {
                                doRename.accept(current);
                            }
                            else
                            {
                                this.openGroupPickerPanel(UIKeys.SCENE_REPLAYS_GROUP_PICK_TITLE, doRename);
                            }
                        });

                        // (Eliminado) Buscar grupo y Quitar filtro de grupo

                        // 4. Desvincular reproducciones seleccionadas del grupo
                        submenu.action(Icons.X, UIKeys.SCENE_REPLAYS_GROUP_UNLINK, () ->
                        {
                            for (Replay r : this.getCurrent())
                            {
                                r.group.set("");
                            }
                            this.update();
                        });

                        // 5. Borrar grupo (mantener reproducciones)
                        submenu.action(Icons.CLOSE, UIKeys.SCENE_REPLAYS_GROUP_DELETE_KEEP, () ->
                        {
                            String current = this.getCurrentFirst() != null ? this.getCurrentFirst().group.get() : "";
                            java.util.function.Consumer<String> deleteOnly = (g) ->
                            {
                                UIConfirmOverlayPanel confirmPanel = new UIConfirmOverlayPanel(
                                    UIKeys.SCENE_REPLAYS_GROUP_DELETE_KEEP_TITLE,
                                    UIKeys.SCENE_REPLAYS_GROUP_DELETE_KEEP_DESCRIPTION.format(g),
                                    (confirm) ->
                                    {
                                        if (confirm) { this.deleteGroupOnly(g); }
                                    }
                                );
                                UIOverlay.addOverlay(this.getContext(), confirmPanel);
                            };

                            if (current != null && !current.isEmpty())
                            {
                                deleteOnly.accept(current);
                            }
                            else
                            {
                                this.openGroupPickerPanel(UIKeys.SCENE_REPLAYS_GROUP_PICK_TITLE, deleteOnly);
                            }
                        });

                        // 6. Eliminar grupo de reproducción (borrar grupo y sus reproducciones)
                        submenu.action(Icons.REMOVE, UIKeys.SCENE_REPLAYS_GROUP_DELETE_ALL, () ->
                        {
                            String current = this.getCurrentFirst() != null ? this.getCurrentFirst().group.get() : "";
                            java.util.function.Consumer<String> deleteWithReplays = (g) ->
                            {
                                UIConfirmOverlayPanel confirmPanel = new UIConfirmOverlayPanel(
                                    UIKeys.SCENE_REPLAYS_GROUP_DELETE_ALL_TITLE,
                                    UIKeys.SCENE_REPLAYS_GROUP_DELETE_ALL_DESCRIPTION.format(g),
                                    (confirm) ->
                                    {
                                        if (confirm) { this.deleteGroupWithReplays(g); }
                                    }
                                );
                                UIOverlay.addOverlay(this.getContext(), confirmPanel);
                            };

                            if (current != null && !current.isEmpty())
                            {
                                deleteWithReplays.accept(current);
                            }
                            else
                            {
                                this.openGroupPickerPanel(UIKeys.SCENE_REPLAYS_GROUP_PICK_TITLE, deleteWithReplays);
                            }
                        });
                    });
                });
                if (data != null)
                {
                    menu.action(Icons.PASTE, UIKeys.SCENE_REPLAYS_CONTEXT_PASTE_KEYFRAMES, () -> this.pasteToReplays(data));
                }

                menu.action(Icons.DUPE, UIKeys.SCENE_REPLAYS_CONTEXT_DUPE, () ->
                {
                    if (Window.isShiftPressed() || shift)
                    {
                        this.dupeReplay();
                    }
                    else
                    {
                        UINumberOverlayPanel numberPanel = new UINumberOverlayPanel(UIKeys.SCENE_REPLAYS_CONTEXT_DUPE, UIKeys.SCENE_REPLAYS_CONTEXT_DUPE_DESCRIPTION, (n) ->
                        {
                            for (int i = 0; i < n; i++)
                            {
                                this.dupeReplay();
                            }
                        });

                        numberPanel.value.limit(1).integer();
                        numberPanel.value.setValue(1D);

                        UIOverlay.addOverlay(this.getContext(), numberPanel);
                    }
                });
                menu.action(Icons.REMOVE, UIKeys.SCENE_REPLAYS_CONTEXT_REMOVE, this::removeReplay);
            }
        });

        /* inicializar expansión por defecto */
        for (String g : this.collectGroups())
        {
            this.expandedGroups.putIfAbsent(g, true);
        }
        this.expandedGroups.putIfAbsent(EMPTY_GROUP_LABEL, true);
    }

    @Override
    protected void handleSwap(int from, int to)
    {
        Film data = this.panel.getData();
        Replays replays = data.replays;
        Replay value = replays.getList().get(from);

        data.preNotify(IValueListener.FLAG_UNMERGEABLE);

        replays.remove(value);
        replays.add(to, value);
        replays.sync();

        /* Readjust tracker and anchor indices */
        for (Replay replay : replays.getList())
        {
            if (replay.properties.get("anchor") instanceof KeyframeChannel<?> channel && channel.getFactory() == KeyframeFactories.ANCHOR)
            {
                KeyframeChannel<Anchor> keyframeChannel = (KeyframeChannel<Anchor>) channel;

                for (Keyframe<Anchor> keyframe : keyframeChannel.getKeyframes())
                {
                    keyframe.getValue().replay = MathUtils.remapIndex(keyframe.getValue().replay, from, to);
                }
            }
        }

        for (Clip clip : data.camera.get())
        {
            if (clip instanceof EntityClip entityClip)
            {
                entityClip.selector.set(MathUtils.remapIndex(entityClip.selector.get(), from, to));
            }
        }

        data.postNotify(IValueListener.FLAG_UNMERGEABLE);

        this.setList(replays.getList());
        this.updateFilmEditor();
        this.pick(to);
    }

    private void pasteToReplays(MapType data)
    {
        UIReplaysEditor replayEditor = this.panel.replayEditor;
        List<Replay> selectedReplays = replayEditor.replays.replays.getCurrent();

        if (data == null)
        {
            return;
        }

        Map<String, UIKeyframes.PastedKeyframes> parsedKeyframes = UIKeyframes.parseKeyframes(data);

        if (parsedKeyframes.isEmpty())
        {
            return;
        }

        UINumberOverlayPanel offsetPanel = new UINumberOverlayPanel(UIKeys.SCENE_REPLAYS_CONTEXT_PASTE_KEYFRAMES_TITLE, UIKeys.SCENE_REPLAYS_CONTEXT_PASTE_KEYFRAMES_DESCRIPTION, (n) ->
        {
            int tick = this.panel.getCursor();

            for (Replay replay : selectedReplays)
            {
                int randomOffset = (int) (n.intValue() * Math.random());

                for (Map.Entry<String, UIKeyframes.PastedKeyframes> entry : parsedKeyframes.entrySet())
                {
                    String id = entry.getKey();
                    UIKeyframes.PastedKeyframes pastedKeyframes = entry.getValue();
                    KeyframeChannel channel = (KeyframeChannel) replay.keyframes.get(id);

                    if (channel == null || channel.getFactory() != pastedKeyframes.factory)
                    {
                        channel = (KeyframeChannel) replay.properties.get(id);
                    }

                    float min = Integer.MAX_VALUE;

                    for (Keyframe kf : pastedKeyframes.keyframes)
                    {
                        min = Math.min(kf.getTick(), min);
                    }

                    for (Keyframe kf : pastedKeyframes.keyframes)
                    {
                        float finalTick = tick + (kf.getTick() - min) + randomOffset;
                        int index = channel.insert(finalTick, kf.getValue());
                        Keyframe inserted = channel.get(index);

                        inserted.copy(kf);
                        inserted.setTick(finalTick);
                    }

                    channel.sort();
                }
            }
        });

        UIOverlay.addOverlay(this.getContext(), offsetPanel);
    }

    private void processReplays()
    {
        UITextbox expression = new UITextbox((t) -> LAST_PROCESS = t);
        UIStringList properties = new UIStringList(null);
        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(UIKeys.SCENE_REPLAYS_CONTEXT_PROCESS_TITLE, UIKeys.SCENE_REPLAYS_CONTEXT_PROCESS_DESCRIPTION, (b) ->
        {
            if (b)
            {
                MathBuilder builder = new MathBuilder();
                int min = Integer.MAX_VALUE;

                builder.register("i");
                builder.register("o");
                builder.register("v");
                builder.register("ki");

                IExpression parse;

                try
                {
                    parse = builder.parse(expression.getText());
                }
                catch (Exception e)
                {
                    return;
                }

                LAST_PROCESS_PROPERTIES = new ArrayList<>(properties.getCurrent());

                for (int index : this.current)
                {
                    min = Math.min(min, index);
                }

                for (int index : this.current)
                {
                    Replay replay = this.list.get(index);

                    builder.variables.get("i").set(index);
                    builder.variables.get("o").set(index - min);

                    for (String s : properties.getCurrent())
                    {
                        KeyframeChannel channel = (KeyframeChannel) replay.keyframes.get(s);
                        List keyframes = channel.getKeyframes();

                        for (int i = 0; i < keyframes.size(); i++)
                        {
                            Keyframe kf = (Keyframe) keyframes.get(i);

                            builder.variables.get("v").set(kf.getFactory().getY(kf.getValue()));
                            builder.variables.get("ki").set(i);

                            kf.setValue(kf.getFactory().yToValue(parse.doubleValue()), true);
                        }
                    }
                }
            }
        });

        for (KeyframeChannel<?> channel : this.getCurrentFirst().keyframes.getChannels())
        {
            if (KeyframeFactories.isNumeric(channel.getFactory()))
            {
                properties.add(channel.getId());
            }
        }

        properties.background().multi().sort();
        properties.relative(expression).y(-5).w(1F).h(16 * 9).anchor(0F, 1F);

        if (!LAST_PROCESS_PROPERTIES.isEmpty())
        {
            properties.setCurrentScroll(LAST_PROCESS_PROPERTIES.get(0));
        }

        for (String property : LAST_PROCESS_PROPERTIES)
        {
            properties.addIndex(properties.getList().indexOf(property));
        }

        expression.setText(LAST_PROCESS);
        expression.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_PROCESS_EXPRESSION_TOOLTIP);
        expression.relative(panel.confirm).y(-1F, -5).w(1F).h(20);

        panel.confirm.w(1F, -10);
        panel.content.add(expression, properties);

        UIOverlay.addOverlay(this.getContext(), panel, 240, 300);
    }

    private void offsetTimeReplays()
    {
        UITextbox tick = new UITextbox((t) -> LAST_OFFSET = t);
        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(UIKeys.SCENE_REPLAYS_CONTEXT_OFFSET_TIME_TITLE, UIKeys.SCENE_REPLAYS_CONTEXT_OFFSET_TIME_DESCRIPTION, (b) ->
        {
            if (b)
            {
                MathBuilder builder = new MathBuilder();
                int min = Integer.MAX_VALUE;

                builder.register("i");
                builder.register("o");

                IExpression parse = null;

                try
                {
                    parse = builder.parse(tick.getText());
                }
                catch (Exception e)
                {}

                for (int index : this.current)
                {
                    min = Math.min(min, index);
                }

                for (int index : this.current)
                {
                    Replay replay = this.list.get(index);

                    builder.variables.get("i").set(index);
                    builder.variables.get("o").set(index - min);

                    float tickv = parse == null ? 0F : (float) parse.doubleValue();

                    BaseValue.edit(replay, (r) -> r.shift(tickv));
                }
            }
        });

        tick.setText(LAST_OFFSET);
        tick.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_OFFSET_TIME_EXPRESSION_TOOLTIP);
        tick.relative(panel.confirm).y(-1F, -5).w(1F).h(20);

        panel.confirm.w(1F, -10);
        panel.content.add(tick);

        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void copyReplay()
    {
        MapType replays = new MapType();
        ListType replayList = new ListType();

        replays.put("replays", replayList);

        for (Replay replay : this.getCurrent())
        {
            replayList.add(replay.toData());
        }

        Window.setClipboard(replays, "_CopyReplay");
    }

    private void pasteReplay(MapType data)
    {
        Film film = this.panel.getData();
        ListType replays = data.getList("replays");
        Replay last = null;

        for (BaseType replayType : replays)
        {
            Replay replay = film.replays.addReplay();

            BaseValue.edit(replay, (r) -> r.fromData(replayType));

            last = replay;
        }

        if (last != null)
        {
            this.update();
            this.panel.replayEditor.setReplay(last);
            this.updateFilmEditor();
        }
    }

    public void openFormEditor(ValueForm form, boolean editing, Consumer<Form> consumer)
    {
        UIElement target = this.panel;

        if (this.getRoot() != null)
        {
            target = this.getParentContainer();
        }

        UIFormPalette palette = UIFormPalette.open(target, editing, form.get(), (f) ->
        {
            for (Replay replay : this.getCurrent())
            {
                replay.form.set(FormUtils.copy(f));
            }

            this.updateFilmEditor();

            if (consumer != null)
            {
                consumer.accept(f);
            }
            else
            {
                this.overlay.pickEdit.setForm(f);
            }
        });

        palette.updatable();
    }

    private void addReplay()
    {
        World world = MinecraftClient.getInstance().world;
        Camera camera = this.panel.getCamera();

        BlockHitResult blockHitResult = RayTracing.rayTrace(world, camera, 64F);
        Vec3d p = blockHitResult.getPos();
        Vector3d position = new Vector3d(p.x, p.y, p.z);

        if (blockHitResult.getType() == HitResult.Type.MISS)
        {
            position.set(camera.getLookDirection()).mul(5F).add(camera.position);
        }

        this.addReplay(position, camera.rotation.x, camera.rotation.y + MathUtils.PI);
    }

    private void fromCamera(int duration)
    {
        Position position = new Position();
        Clips camera = this.panel.getData().camera;
        CameraClipContext context = new CameraClipContext();

        Film film = this.panel.getData();
        Replay replay = film.replays.addReplay();

        context.clips = camera;

        for (int i = 0; i < duration; i++)
        {
            context.clipData.clear();
            context.setup(i, 0F);

            for (Clip clip : context.clips.getClips(i))
            {
                context.apply(clip, position);
            }

            context.currentLayer = 0;

            float yaw = position.angle.yaw - 180;

            replay.keyframes.x.insert(i, position.point.x);
            replay.keyframes.y.insert(i, position.point.y);
            replay.keyframes.z.insert(i, position.point.z);
            replay.keyframes.yaw.insert(i, (double) yaw);
            replay.keyframes.headYaw.insert(i, (double) yaw);
            replay.keyframes.bodyYaw.insert(i, (double) yaw);
            replay.keyframes.pitch.insert(i, (double) position.angle.pitch);
        }

        this.update();
        this.panel.replayEditor.setReplay(replay);
        this.updateFilmEditor();

        this.openFormEditor(replay.form, false, null);
    }

    private void fromModelBlock()
    {
        ArrayList<ModelBlockEntity> modelBlocks = new ArrayList<>(BBSRendering.capturedModelBlocks);
        UISearchList<String> search = new UISearchList<>(new UIStringList(null));
        UIList<String> list = search.list;
        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(UIKeys.SCENE_REPLAYS_CONTEXT_FROM_MODEL_BLOCK_TITLE, UIKeys.SCENE_REPLAYS_CONTEXT_FROM_MODEL_BLOCK_DESCRIPTION, (b) ->
        {
            if (b)
            {
                int index = list.getIndex();
                ModelBlockEntity modelBlock = CollectionUtils.getSafe(modelBlocks, index);

                if (modelBlock != null)
                {
                    this.fromModelBlock(modelBlock);
                }
            }
        });

        modelBlocks.sort(Comparator.comparing(ModelBlockEntity::getName));

        for (ModelBlockEntity modelBlock : modelBlocks)
        {
            list.add(modelBlock.getName());
        }

        list.background();
        search.relative(panel.confirm).y(-5).w(1F).h(16 * 9 + 20).anchor(0F, 1F);

        panel.confirm.w(1F, -10);
        panel.content.add(search);

        UIOverlay.addOverlay(this.getContext(), panel, 240, 300);
    }

    private void fromModelBlock(ModelBlockEntity modelBlock)
    {
        Film film = this.panel.getData();
        Replay replay = film.replays.addReplay();
        BlockPos blockPos = modelBlock.getPos();
        ModelProperties properties = modelBlock.getProperties();
        Transform transform = properties.getTransform().copy();
        double x = blockPos.getX() + transform.translate.x + 0.5D;
        double y = blockPos.getY() + transform.translate.y;
        double z = blockPos.getZ() + transform.translate.z + 0.5D;

        transform.translate.set(0, 0, 0);

        replay.shadow.set(properties.isShadow());
        replay.form.set(FormUtils.copy(properties.getForm()));
        replay.keyframes.x.insert(0, x);
        replay.keyframes.y.insert(0, y);
        replay.keyframes.z.insert(0, z);

        if (!transform.isDefault())
        {
            if (
                transform.rotate.x == 0 && transform.rotate.z == 0 &&
                transform.rotate2.x == 0 && transform.rotate2.y == 0 && transform.rotate2.z == 0 &&
                transform.scale.x == 1 && transform.scale.y == 1 && transform.scale.z == 1
            ) {
                double yaw = -Math.toDegrees(transform.rotate.y);

                replay.keyframes.yaw.insert(0, yaw);
                replay.keyframes.headYaw.insert(0, yaw);
                replay.keyframes.bodyYaw.insert(0, yaw);
            }
            else
            {
                AnchorForm form = new AnchorForm();
                BodyPart part = new BodyPart("");

                part.setForm(replay.form.get());
                form.transform.set(transform);
                form.parts.addBodyPart(part);

                replay.form.set(form);
            }
        }

        this.update();
        this.panel.replayEditor.setReplay(replay);
        this.updateFilmEditor();
    }

    public void addReplay(Vector3d position, float pitch, float yaw)
    {
        Film film = this.panel.getData();
        Replay replay = film.replays.addReplay();

        replay.keyframes.x.insert(0, position.x);
        replay.keyframes.y.insert(0, position.y);
        replay.keyframes.z.insert(0, position.z);

        replay.keyframes.pitch.insert(0, (double) pitch);
        replay.keyframes.yaw.insert(0, (double) yaw);
        replay.keyframes.headYaw.insert(0, (double) yaw);
        replay.keyframes.bodyYaw.insert(0, (double) yaw);

        this.update();
        this.panel.replayEditor.setReplay(replay);
        this.updateFilmEditor();

        this.openFormEditor(replay.form, false, null);
    }

    private void updateFilmEditor()
    {
        this.panel.getController().createEntities();
        this.panel.replayEditor.updateChannelsList();
    }

    private void dupeReplay()
    {
        if (this.isDeselected())
        {
            return;
        }

        Replay last = null;

        for (Replay replay : this.getCurrent())
        {
            Film film = this.panel.getData();
            Replay newReplay = film.replays.addReplay();

            newReplay.copy(replay);

            last = newReplay;
        }

        if (last != null)
        {
            this.update();
            this.panel.replayEditor.setReplay(last);
            this.updateFilmEditor();
        }
    }

    private void removeReplay()
    {
        if (this.isDeselected())
        {
            return;
        }

        Film film = this.panel.getData();
        int index = this.getIndex();

        for (Replay replay : this.getCurrent())
        {
            film.replays.remove(replay);
        }

        int size = this.list.size();
        index = MathUtils.clamp(index, 0, size - 1);

        this.update();
        this.panel.replayEditor.setReplay(size == 0 ? null : this.list.get(index));
        this.updateFilmEditor();
    }

    @Override
    public void renderList(UIContext context)
    {
        /* En modo filtrado o sin grupos reales, usar comportamiento base */
        if (this.isFiltering() || !this.hasAnyGroup())
        {
            super.renderList(context);
            return;
        }

        /* Construir filas agrupadas y dibujar */
        buildGroupedRows();

        int itemH = this.scroll.scrollItemSize;
        int start = Math.max(0, (int) (this.scroll.getScroll() / itemH));
        int visible = Math.max(0, this.area.h / itemH + 2);
        int end = Math.min(groupedRows.size(), start + visible);

        int y = this.area.y - (int) (this.scroll.getScroll() % itemH);

        for (int i = start; i < end; i++)
        {
            Row row = groupedRows.get(i);
            int x = this.area.x;
            boolean hover = this.area.isInside(context) && context.mouseY >= y && context.mouseY < y + itemH;

            if (row.header)
            {
                /* Encabezado de grupo */
                context.batcher.textCard(row.group, x + 26, y + 6);

                boolean expanded = this.expandedGroups.getOrDefault(row.group, true);
                context.batcher.icon(expanded ? Icons.MOVE_DOWN : Icons.MOVE_UP, x + 16, y + (expanded ? 5 : 4), 0.5F, 0F);
            }
            else
            {
                int indexInList = this.identityIndex(row.replay);
                boolean selected = indexInList >= 0 && this.current.contains(indexInList);

                if (selected)
                {
                    context.batcher.box(x, y, x + this.area.w, y + itemH, Colors.A50 | mchorse.bbs_mod.BBSSettings.primaryColor.get());
                }

                this.renderElementPart(context, row.replay, indexInList, x, y, hover, selected);
            }

            y += itemH;
        }
    }

    @Override
    public void render(UIContext context)
    {
        /* Mantener render base */
        super.render(context);

        /* Dibujar ghost de arrastre cuando estamos en modo agrupado */
        if (this.isGroupedDragging(context) && this.exists(this.groupedDragFrom))
        {
            this.renderListElement(
                context,
                this.list.get(this.groupedDragFrom),
                this.groupedDragFrom,
                context.mouseX + 6,
                context.mouseY - this.scroll.scrollItemSize / 2,
                true,
                true
            );
        }
    }

    @Override
    protected String elementToString(UIContext context, int i, Replay element)
    {
        String name = element.getName();
        /* Ocultar visualmente el nombre de grupo: mostrar solo el nombre del replay */
        return context.batcher.getFont().limitToWidth(name, this.area.w - 20);
    }

    @Override
    protected void renderElementPart(UIContext context, Replay element, int i, int x, int y, boolean hover, boolean selected)
    {
        if (element.enabled.get())
        {
            super.renderElementPart(context, element, i, x, y, hover, selected);
        }
        else
        {
            context.batcher.textShadow(this.elementToString(context, i, element), x + 4, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, hover ? Colors.mulRGB(Colors.HIGHLIGHT, 0.75F) : Colors.GRAY);
        }

        Form form = element.form.get();

        if (form != null)
        {
            x += this.area.w - 30;

            context.batcher.clip(x, y, 40, 20, context);

            y -= 10;

            FormUtilsClient.renderUI(form, context, x, y, x + 40, y + 40);

            context.batcher.unclip(context);

            if (element.fp.get())
            {
                context.batcher.outlinedIcon(Icons.ARROW_UP, x, y + 20, 0.5F, 0.5F);
            }
        }
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        /* En modo categorías, manejar clicks para expandir/collapse y selección.
         * No consumir botón derecho para que funcione el menú contextual. */
        if (!this.isFiltering() && this.hasAnyGroup() && this.area.isInside(context))
        {
            if (context.mouseButton != 0)
            {
                return super.subMouseClicked(context);
            }

            int itemH = this.scroll.scrollItemSize;
            int localY = context.mouseY - this.area.y + (int) this.scroll.getScroll();
            int rowIndex = localY / itemH;

            buildGroupedRows();

            if (rowIndex >= 0 && rowIndex < this.groupedRows.size())
            {
                Row row = this.groupedRows.get(rowIndex);

                if (row.header)
                {
                    boolean expanded = this.expandedGroups.getOrDefault(row.group, true);
                    this.expandedGroups.put(row.group, !expanded);
                    this.update();
                    return true;
                }
                else if (row.replay != null)
                {
                    int indexInList = this.identityIndex(row.replay);
                    if (indexInList >= 0)
                    {
                        /* Selección múltiple: Ctrl para toggle y Shift para rango */
                        if (Window.isCtrlPressed())
                        {
                            this.toggleIndex(indexInList);
                        }
                        else if (this.multi && Window.isShiftPressed() && this.isSelected())
                        {
                            int first = this.current.get(0);
                            int increment = first > indexInList ? -1 : 1;

                            for (int i = first + increment; i != indexInList + increment; i += increment)
                            {
                                this.addIndex(i);
                            }
                        }
                        else
                        {
                            this.setIndex(indexInList);
                        }

                        if (this.callback != null)
                        {
                            this.callback.accept(this.getCurrent());
                        }

                        /* Iniciar arrastre propio en modo agrupado */
                        if (this.sorting && this.current.size() == 1)
                        {
                            this.groupedDragFrom = indexInList;
                            this.groupedDragTime = System.currentTimeMillis();
                            this.groupedDragStartX = context.mouseX;
                            this.groupedDragStartY = context.mouseY;
                            this.groupedDragHolding = true;
                        }

                        return true;
                    }
                }
            }
        }

        return super.subMouseClicked(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (!this.isFiltering() && this.hasAnyGroup() && this.sorting && this.isGroupedDragging(context))
        {
            /* Determinar fila destino para saber si es encabezado o elemento */
            int itemH = this.scroll.scrollItemSize;
            int localY = context.mouseY - this.area.y + (int) this.scroll.getScroll();
            int rowIndex = localY / itemH;

            buildGroupedRows();

            int to = this.listIndexFromMouse(context);
            boolean changeGroup = false;
            String targetGroup = null;

            /* Si la fila es válida, comprobamos si es encabezado o elemento */
            if (rowIndex >= 0 && rowIndex < this.groupedRows.size())
            {
                Row targetRow = this.groupedRows.get(rowIndex);
                if (targetRow.header)
                {
                    targetGroup = EMPTY_GROUP_LABEL.equals(targetRow.group) ? "" : targetRow.group;
                    changeGroup = true;
                    /* Soltar sobre encabezado: insertar al final del grupo destino */
                    to = this.lastIndexOfGroup(targetRow.group);
                }
                else if (targetRow.replay != null)
                {
                    targetGroup = EMPTY_GROUP_LABEL.equals(targetRow.group) ? "" : targetRow.group;
                    /* Si suelto sobre un elemento, mover cerca de ese índice y ajustar grupo */
                    changeGroup = true;
                    to = this.identityIndex(targetRow.replay);
                }
            }

            if (to == -2)
            {
                to = this.getList().size() - 1;
            }
            else if (to == -1)
            {
                to = 0;
            }

            /* Ajustar grupo si es necesario antes de reordenar */
            if (changeGroup && this.groupedDragFrom >= 0 && this.groupedDragFrom < this.list.size())
            {
                Replay moved = this.list.get(this.groupedDragFrom);
                String currentGroup = moved.group.get();
                currentGroup = currentGroup == null ? "" : currentGroup;
                if (targetGroup != null && !currentGroup.equals(targetGroup))
                {
                    moved.group.set(targetGroup);
                }
            }

            if (to >= 0 && to < this.getList().size() && to != this.groupedDragFrom)
            {
                this.handleSwap(this.groupedDragFrom, to);
            }

            this.groupedDragFrom = -1;
            this.groupedDragHolding = false;
            return true;
        }

        /* Si no hubo arrastre efectivo, limpiar estado y delegar a la lógica base */
        this.groupedDragHolding = false;
        this.groupedDragFrom = -1;
        return super.subMouseReleased(context);
    }

    private int listIndexFromMouse(UIContext context)
    {
        int itemH = this.scroll.scrollItemSize;
        int localY = context.mouseY - this.area.y + (int) this.scroll.getScroll();
        int rowIndex = localY / itemH;

        if (rowIndex < 0)
        {
            return -1;
        }

        buildGroupedRows();

        if (rowIndex >= this.groupedRows.size())
        {
            return -2; /* debajo del último elemento */
        }

        Row row = this.groupedRows.get(rowIndex);

        if (row.replay != null)
        {
            return this.identityIndex(row.replay);
        }

        /* Si es encabezado, colocar al INICIO de ese grupo para arrastre libre */
        return this.firstIndexOfGroup(row.group);
    }

    /**
     * Devuelve el índice del replay comparando por identidad (==),
     * para evitar colisiones cuando equals() considera nombre/propiedades.
     */
    private int identityIndex(Replay r)
    {
        if (r == null)
        {
            return -1;
        }

        for (int i = 0; i < this.list.size(); i++)
        {
            if (this.list.get(i) == r)
            {
                return i;
            }
        }

        return -1;
    }

    /* Selección por identidad para evitar colisiones por equals() */
    @Override
    public void setCurrentScroll(Replay element)
    {
        this.setCurrent(element);

        if (!this.current.isEmpty())
        {
            this.scroll.setScroll(this.current.get(0) * this.scroll.scrollItemSize);
        }
    }

    @Override
    public void setCurrent(Replay element)
    {
        this.current.clear();

        int index = this.identityIndex(element);

        if (this.exists(index))
        {
            this.current.add(index);
        }
    }

    /* Selección múltiple por identidad para List<Replay> */
    @Override
    public void setCurrent(java.util.List<Replay> elements)
    {
        this.current.clear();

        if (elements == null || elements.isEmpty())
        {
            return;
        }

        for (Replay r : elements)
        {
            int idx = this.identityIndex(r);
            if (this.exists(idx))
            {
                this.current.add(idx);
            }
        }
    }

    private int lastIndexOfGroup(String group)
    {
        int last = -1;
        for (int i = 0; i < this.list.size(); i++)
        {
            Replay r = this.list.get(i);
            String g = r.group.get();
            if (g != null && g.equals(group))
            {
                last = i;
            }
        }
        return last == -1 ? this.list.size() - 1 : last;
    }

    /**
     * Devuelve el primer índice de un grupo dado.
     * Si no hay elementos de ese grupo, se devuelve 0 para permitir inserción al principio.
     */
    private int firstIndexOfGroup(String group)
    {
        for (int i = 0; i < this.list.size(); i++)
        {
            Replay r = this.list.get(i);
            String g = r.group.get();
            if (g != null && g.equals(group))
            {
                return i;
            }
        }

        /* Si el grupo está vacío (caso raro), insertar al principio */
        return 0;
    }

    private boolean isGroupedDragging(UIContext context)
    {
        /* Arrastre diferido con umbral de movimiento para evitar falsos positivos */
        if (this.groupedDragFrom < 0 || !this.groupedDragHolding)
        {
            return false;
        }

        long elapsed = System.currentTimeMillis() - this.groupedDragTime;
        int dx = Math.abs(context.mouseX - this.groupedDragStartX);
        int dy = Math.abs(context.mouseY - this.groupedDragStartY);

        return elapsed >= 150 && (dx >= 3 || dy >= 3);
    }

    private void buildGroupedRows()
    {
        this.groupedRows.clear();

        /* Construir mapa grupo->replays */
        java.util.Map<String, java.util.List<Replay>> byGroup = new java.util.HashMap<>();
        for (Replay r : this.list)
        {
            String g = r.group.get();
            String key = (g == null || g.isEmpty()) ? EMPTY_GROUP_LABEL : g;
            byGroup.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(r);
            this.expandedGroups.putIfAbsent(key, true);
        }

        java.util.List<String> groups = new java.util.ArrayList<>(byGroup.keySet());
        groups.sort(String::compareToIgnoreCase);

        for (String g : groups)
        {
            this.groupedRows.add(new Row(g, true, null));
            if (this.expandedGroups.getOrDefault(g, true))
            {
                for (Replay r : byGroup.get(g))
                {
                    this.groupedRows.add(new Row(g, false, r));
                }
            }
        }

        /* Ajustar tamaño del scroll basado en filas visibles */
        int total = this.groupedRows.size();
        this.scroll.setSize(total);
    }

    @Override
    public void update()
    {
        /* Usar la lógica base. Ajustar el scroll únicamente cuando el modo
         * agrupado esté activo (hay grupos reales y no hay filtro). */
        super.update();

        if (!this.isFiltering() && this.hasAnyGroup())
        {
            buildGroupedRows();
            this.scroll.setSize(this.groupedRows.size());
            this.scroll.clamp();
        }
    }

    /** Determina si existen grupos reales en la lista */
    private boolean hasAnyGroup()
    {
        for (Replay r : this.list)
        {
            String g = r.group.get();
            if (g != null && !g.isEmpty())
            {
                return true;
            }
        }
        return false;
    }

    /* =====================
     * Helpers de grupos
     * ===================== */
    private List<String> collectGroups()
    {
        List<String> groups = new ArrayList<>();
        /* Evitar NPE si el panel o sus datos aún no están disponibles */
        if (this.panel == null || this.panel.getData() == null)
        {
            return groups;
        }

        Replays replays = this.panel.getData().replays;
        if (replays == null)
        {
            return groups;
        }

        List<Replay> list = replays.getList();
        for (Replay r : list)
        {
            String g = r.group.get();
            if (g != null && !g.isEmpty() && !groups.contains(g))
            {
                groups.add(g);
            }
        }
        groups.sort(String::compareToIgnoreCase);
        return groups;
    }

    private void openGroupPickerPanel(mchorse.bbs_mod.l10n.keys.IKey title, java.util.function.Consumer<String> callback)
    {
        List<String> groups = this.collectGroups();
        UISearchList<String> search = new UISearchList<>(new UIStringList(null));
        UIList<String> list = search.list;
        UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(title, UIKeys.SCENE_REPLAYS_GROUP_PICK_DESCRIPTION, (b) ->
        {
            if (b)
            {
                int index = list.getIndex();
                String g = CollectionUtils.getSafe(groups, index);
                if (g != null)
                {
                    callback.accept(g);
                }
            }
        });

        for (String g : groups) { list.add(g); }
        list.background();
        search.relative(panel.confirm).y(-5).w(1F).h(16 * 9 + 20).anchor(0F, 1F);
        panel.confirm.w(1F, -10);
        panel.content.add(search);
        UIOverlay.addOverlay(this.getContext(), panel, 240, 300);
    }

    private void renameGroup(String oldName, String newName)
    {
        if (oldName == null || oldName.isEmpty()) { return; }
        if (this.panel == null || this.panel.getData() == null || this.panel.getData().replays == null) { return; }
        List<Replay> list = this.panel.getData().replays.getList();
        for (Replay r : list)
        {
            if (oldName.equals(r.group.get()))
            {
                r.group.set(newName == null ? "" : newName);
            }
        }
        this.update();
    }

    private void deleteGroupOnly(String group)
    {
        if (group == null || group.isEmpty()) { return; }
        if (this.panel == null || this.panel.getData() == null || this.panel.getData().replays == null) { return; }
        List<Replay> list = this.panel.getData().replays.getList();
        for (Replay r : list)
        {
            if (group.equals(r.group.get()))
            {
                r.group.set("");
            }
        }
        this.update();
    }

    private void deleteGroupWithReplays(String group)
    {
        if (group == null || group.isEmpty()) { return; }
        if (this.panel == null || this.panel.getData() == null || this.panel.getData().replays == null) { return; }
        Film film = this.panel.getData();
        List<Replay> list = new ArrayList<>(film.replays.getList());
        for (Replay r : list)
        {
            if (group.equals(r.group.get()))
            {
                film.replays.remove(r);
            }
        }
        this.update();
        this.panel.replayEditor.updateChannelsList();
    }
}