package mchorse.bbs_mod.ui.forms.editors;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.AnchorForm;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.BodyPartManager;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.FramebufferForm;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.forms.states.AnimationState;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.ICursor;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.forms.IUIFormList;
import mchorse.bbs_mod.ui.forms.UIFormList;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.forms.editors.forms.UIAnchorForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIBillboardForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIBlockForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIExtrudedForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIFramebufferForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIItemForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UILabelForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIMobForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIParticleForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIStructureForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UITrailForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIVanillaParticleForm;
import mchorse.bbs_mod.ui.forms.editors.states.UIAnimationStatesOverlayPanel;
import mchorse.bbs_mod.ui.forms.editors.states.keyframes.UIAnimationStateEditor;
import mchorse.bbs_mod.ui.forms.editors.utils.UIPickableFormRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UICopyPasteController;
import mchorse.bbs_mod.ui.utils.presets.UIPresetContextMenu;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.presets.PresetManager;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIFormEditor extends UIElement implements IUIFormList, ICursor
{
    private static Map<Class, Supplier<UIForm>> panels = new HashMap<>();

    private static float treeWidth = 0F;
    private static boolean TOGGLED = true;

    /* Palette for picking a form for body parts */
    public UIFormPalette palette;

    /* Main form editor element */
    public UIElement formEditor;
    public UIPickableFormRenderer renderer;
    public UIForm editor;

    /* States editor */
    public UIElement statesEditor;
    public UIAnimationStateEditor statesKeyframes;
    public UIIcon openStates;
    public UIIcon plause;

    /* Forms sidebar */
    public UIElement forms;
    public UIForms formsList;
    public UIBodyPartEditor bodyPartEditor;

    /* Sidebar icons */
    public UIElement icons;
    public UIIcon finish;
    public UIIcon toggleSidebar;
    public UIIcon openStateEditor;

    public Form form;

    private Consumer<Form> callback;
    private UICopyPasteController copyPasteController;
    private UIFormUndoHandler undoHandler;

    private int lastTick;
    private int cursor;
    private boolean playing;

    static
    {
        register(BillboardForm.class, UIBillboardForm::new);
        register(ExtrudedForm.class, UIExtrudedForm::new);
        register(LabelForm.class, UILabelForm::new);
        register(ModelForm.class, UIModelForm::new);
        register(ParticleForm.class, UIParticleForm::new);
        register(BlockForm.class, UIBlockForm::new);
        register(ItemForm.class, UIItemForm::new);
        register(AnchorForm.class, UIAnchorForm::new);
        register(MobForm.class, UIMobForm::new);
        register(VanillaParticleForm.class, UIVanillaParticleForm::new);
        register(TrailForm.class, UITrailForm::new);
        register(StructureForm.class, UIStructureForm::new);
        register(FramebufferForm.class, UIFramebufferForm::new);
    }

    public static void register(Class clazz, Supplier<UIForm> supplier)
    {
        panels.put(clazz, supplier);
    }

    public static UIForm createPanel(Form form)
    {
        if (form == null)
        {
            return null;
        }

        Supplier<UIForm> supplier = panels.get(form.getClass());

        return supplier == null ? null : supplier.get();
    }

    public UIFormEditor(UIFormPalette palette)
    {
        this.palette = palette;

        this.undoHandler = new UIFormUndoHandler(this);
        this.copyPasteController = new UICopyPasteController(PresetManager.BODY_PARTS, "_FormEditorBodyPart")
            .supplier(this::copyBodyPart)
            .consumer(this::pasteBodyPart)
            .canCopy(() ->
            {
                UIForms.FormEntry current = this.formsList.getCurrentFirst();

                return current != null && current.part != null;
            })
            .canPaste(() ->
            {
                UIForms.FormEntry current = this.formsList.getCurrentFirst();

                return current != null && current.getForm() != null;
            });

        this.forms = new UIElement();
        this.forms.relative(this).x(20).w(treeWidth).minW(140).h(1F);

        this.formsList = new UIForms((l) -> this.pickForm(l.get(0)));
        this.formsList.relative(this.forms).w(1F).h(0.5F);
        this.formsList.context(this::createFormContextMenu);
        this.bodyPartEditor = new UIBodyPartEditor(this);
        this.bodyPartEditor.relative(this.forms).w(1F).y(0.5F).h(0.5F);

        this.formEditor = new UIElement();
        this.formEditor.full(this);

        this.statesEditor = new UIElement();
        this.statesEditor.full(this);
        this.statesEditor.setVisible(false);
        this.statesKeyframes = new UIAnimationStateEditor(this);
        this.statesKeyframes.relative(this.statesEditor).x(20).y(1F).w(1F, -20).h(0.25F).anchorY(1F);

        this.openStates = new UIIcon(Icons.MORE, (b) ->
        {
            UIAnimationStatesOverlayPanel panel = new UIAnimationStatesOverlayPanel(this.form.states, this.statesKeyframes.getState(), (state) -> this.pickState(state));

            panel.setUndoId("animation_states_overlay_panel");
            UIOverlay.addOverlay(this.getContext(), panel, 280, 0.5F).eventPropagataion(EventPropagation.PASS);
        });
        this.openStates.relative(this.statesEditor);
        this.openStates.tooltip(IKey.raw("Open animation states manager"), Direction.RIGHT);
        this.plause = new UIIcon(() -> this.playing ? Icons.PLAY : Icons.PAUSE, (b) -> this.plause());
        this.plause.relative(this.openStates).y(1F);
        this.plause.tooltip(IKey.raw("Play/pause"), Direction.RIGHT);

        this.renderer = new UIPickableFormRenderer(this);
        this.renderer.full(this);

        this.finish = new UIIcon(Icons.IN, (b) -> this.palette.exit());
        this.finish.tooltip(UIKeys.FORMS_EDITOR_FINISH, Direction.RIGHT).relative(this.formEditor).xy(0, 1F).anchorY(1F);
        this.toggleSidebar = new UIIcon(() -> this.forms.isVisible() ? Icons.LEFTLOAD : Icons.RIGHTLOAD, (b) ->
        {
            this.toggleSidebar();

            TOGGLED = !TOGGLED;
        });
        this.toggleSidebar.tooltip(UIKeys.FORMS_EDITOR_TOGGLE_TREE, Direction.RIGHT);
        this.openStateEditor = new UIIcon(Icons.GALLERY, (b) -> this.toggleStateEditor());
        this.openStateEditor.tooltip(IKey.raw("Toggle animation state editor"), Direction.RIGHT);
        this.icons = UI.column(this.openStateEditor, this.toggleSidebar, this.finish);
        this.icons.relative(this).y(1F).w(20).anchorY(1F);

        UIRenderable background = new UIRenderable((context) ->
        {
            if (this.forms.isVisible())
            {
                this.forms.area.render(context.batcher, Colors.A50);
            }
        });

        UIRenderable backgroundStates = new UIRenderable((context) ->
        {
            context.batcher.box(this.area.x, this.area.y, this.area.x + 20, this.area.ey(), Colors.A100);
        });

        UIDraggable draggable = new UIDraggable((context) ->
        {
            int diff = context.mouseX - this.forms.area.x;
            float f = diff / (float) this.area.w;

            treeWidth = MathUtils.clamp(f, 0F, 0.5F);

            this.forms.w(treeWidth).resize();
        });

        draggable.relative(this.forms).x(1F).y(0.5F).w(6).h(40).anchor(0.5F, 0.5F);

        this.forms.add(background, this.formsList, this.bodyPartEditor, draggable);
        this.formEditor.add(this.forms);
        this.statesEditor.add(backgroundStates, this.openStates, this.plause, this.statesKeyframes);
        this.add(this.renderer, this.formEditor, this.statesEditor, this.icons);

        this.keys().register(Keys.UNDO, this::undo);
        this.keys().register(Keys.REDO, this::redo);
        this.plause.keys().register(Keys.PLAUSE, () ->
        {
            this.plause();
            UIUtils.playClick();
        });

        this.setUndoId("form_editor");
    }

    public boolean clickViewport(UIContext context, StencilFormFramebuffer stencil)
    {
        if (this.statesEditor.isVisible() && this.statesKeyframes.clickViewport(context, stencil))
        {
            return true;
        }
        else if (stencil.hasPicked() && context.mouseButton == 0)
        {
            Pair<Form, String> pair = stencil.getPicked();

            if (pair != null)
            {
                this.pickFormFromRenderer(pair);

                return true;
            }
        }

        return false;
    }

    public void pickFormFromRenderer(Pair<Form, String> pair)
    {
        if (Window.isCtrlPressed() && !pair.b.isEmpty()) this.bodyPartEditor.pickBone(pair);
        else if (Window.isAltPressed()) UIReplaysEditorUtils.offerAdjacent(this.getContext(), pair.a, pair.b, (bone) -> this.pickFormBone(pair.a, bone));
        else if (Window.isShiftPressed()) UIReplaysEditorUtils.offerHierarchy(this.getContext(), pair.a, pair.b, (bone) -> this.pickFormBone(pair.a, bone));
        else this.pickFormBone(pair.a, pair.b);
    }

    private void pickFormBone(Form form, String bone)
    {
        this.formsList.setCurrentForm(form);
        this.pickForm(this.formsList.getCurrentFirst());

        if (!bone.isEmpty())
        {
            this.editor.pickBone(bone);
        }
    }

    private void pickState(AnimationState state)
    {
        this.form.resetValues();
        this.statesKeyframes.setState(state);
    }

    private void plause()
    {
        this.playing = !this.playing;
    }

    private void toggleStateEditor()
    {
        this.form.resetValues();
        this.formEditor.toggleVisible();
        this.statesEditor.toggleVisible();
    }

    private void toggleSidebar()
    {
        this.forms.toggleVisible();
    }

    private void createFormContextMenu(ContextMenuManager menu)
    {
        UIForms.FormEntry current = this.formsList.getCurrentFirst();

        if (current != null)
        {
            menu.custom(new UIPresetContextMenu(this.copyPasteController)
                .labels(UIKeys.FORMS_EDITOR_CONTEXT_COPY, UIKeys.FORMS_EDITOR_CONTEXT_PASTE));

            if (current.getForm() != null)
            {
                menu.action(Icons.ADD, UIKeys.FORMS_EDITOR_CONTEXT_ADD, () -> this.addBodyPart(new BodyPart("")));
            }

            if (current.part != null)
            {
                List<BodyPart> all = current.part.getManager().getAllTyped();

                if (all.size() > 1)
                {
                    int index = -1;

                    for (int i = 0; i < all.size(); i++)
                    {
                        if (all.get(i) == current.part)
                        {
                            index = i;

                            break;
                        }
                    }

                    if (index > 0) menu.action(Icons.ARROW_UP, UIKeys.FORMS_EDITOR_CONTEXT_MOVE_UP, () -> this.moveBodyPart(current, -1));
                    if (index < all.size() - 1) menu.action(Icons.ARROW_DOWN, UIKeys.FORMS_EDITOR_CONTEXT_MOVE_DOWN, () -> this.moveBodyPart(current, 1));
                }
            }

            if (current.part != null)
            {
                menu.action(Icons.REMOVE, UIKeys.FORMS_EDITOR_CONTEXT_REMOVE, this::removeBodyPart);
            }
        }
    }

    private void moveBodyPart(UIForms.FormEntry current, int direction)
    {
        BodyPartManager manager = current.part.getManager();
        List<BodyPart> all = manager.getAllTyped();
        int index = all.indexOf(current.part);
        int newIndex = MathUtils.clamp(index + direction, 0, all.size() - 1);

        if (newIndex != index)
        {
            manager.moveBodyPart(current.part, newIndex);
            this.formsList.setForm(this.form);

            UIForms.FormEntry selection = null;

            for (UIForms.FormEntry entry : this.formsList.getList())
            {
                if (entry.part == current.part)
                {
                    selection = entry;

                    break;
                }
            }

            if (selection != null)
            {
                this.formsList.setCurrentScroll(selection);
                this.pickForm(selection);
            }
        }
    }

    private void addBodyPart(BodyPart part)
    {
        UIForms.FormEntry current = this.formsList.getCurrentFirst();

        current.getForm().parts.addBodyPart(part);
        this.refreshFormList();
    }

    private MapType copyBodyPart()
    {
        return this.formsList.getCurrentFirst().part.toData().asMap();
    }

    private void pasteBodyPart(MapType data, int mouseX, int mouseY)
    {
        BodyPart part = new BodyPart("");

        part.fromData(data);
        this.addBodyPart(part);
    }

    private void removeBodyPart()
    {
        int index = this.formsList.getIndex();
        UIForms.FormEntry current = this.formsList.getCurrentFirst();

        current.form.parts.removeBodyPart(current.part);

        this.refreshFormList();
        this.formsList.setIndex(index - 1);
        this.pickForm(this.formsList.getCurrentFirst());
    }

    private void pickForm(UIForms.FormEntry entry)
    {
        this.bodyPartEditor.setVisible(entry.part != null);

        if (entry.part != null)
        {
            this.bodyPartEditor.setPart(entry.part, entry.form);
        }

        this.switchEditor(entry.getForm());
    }

    public void openFormList(Form current, Consumer<Form> callback)
    {
        UIFormEditorList list = new UIFormEditorList(this);

        list.setSelected(current);
        this.callback = callback;

        list.full(this);
        list.resize();
        this.add(list);
    }

    public boolean isEditing()
    {
        return this.form != null;
    }

    public boolean edit(Form form)
    {
        this.form = null;

        if (form == null)
        {
            return false;
        }

        form = FormUtils.copy(form);

        this.bodyPartEditor.setVisible(false);

        if (this.switchEditor(form))
        {
            this.undoHandler.reset();

            this.form = form;
            this.form.setId("form");
            this.form.preCallback(this.undoHandler::handlePreValues);

            this.pickState(form.states.getMain());

            if (TOGGLED != this.forms.isVisible())
            {
                this.toggleSidebar();
            }

            this.palette.accept(form);
            this.renderer.reset();
            this.renderer.form = form;
            this.refreshFormList();
            this.formsList.setIndex(0);

            return true;
        }

        return false;
    }

    public void undo()
    {
        if (this.form != null && this.undoHandler.getUndoManager().undo(this.form)) UIUtils.playClick();
    }

    public void redo()
    {
        if (this.form != null && this.undoHandler.getUndoManager().redo(this.form)) UIUtils.playClick();
    }

    public void refreshFormList()
    {
        UIForms.FormEntry current = this.formsList.getCurrentFirst();

        this.formsList.setForm(this.form);
        this.formsList.setCurrentScroll(current);
    }

    public boolean switchEditor(Form form)
    {
        UIForm editor = createPanel(form);

        if (editor == null)
        {
            return false;
        }

        editor.setUndoId("form_panel");

        if (this.editor != null)
        {
            this.editor.removeFromParent();
        }

        this.editor = editor;

        this.formEditor.prepend(this.editor);

        this.editor.setEditor(this);
        this.editor.startEdit(form);
        this.editor.full(this.formEditor).resize();

        return true;
    }

    public Form finish()
    {
        Form form = this.form;

        this.form.setId("");
        this.form.resetCallbacks();
        this.form.states.cleanUp();
        this.exit();

        this.editor.finishEdit();
        this.editor.removeFromParent();
        this.editor = null;
        this.form = null;

        return form;
    }

    @Override
    public void exit()
    {
        this.callback = null;

        List<UIFormList> children = this.getChildren(UIFormList.class);

        if (!children.isEmpty())
        {
            children.get(0).removeFromParent();
        }
    }

    @Override
    public void toggleEditor()
    {}

    @Override
    public void accept(Form form)
    {
        if (this.callback != null)
        {
            this.callback.accept(form);
        }
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        data.putInt("body_part", this.formsList.getIndex());
    }

    @Override
    public void applyAllUndoData(MapType data)
    {
        if (this.editor != null && this.form != null)
        {
            this.switchEditor(this.form);
        }

        super.applyAllUndoData(data);
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        this.refreshFormList();

        UIForms.FormEntry bodyPart = CollectionUtils.getSafe(this.formsList.getList(), data.getInt("body_part"));

        if (bodyPart != null)
        {
            this.formsList.setCurrentScroll(bodyPart);
            this.pickForm(bodyPart);
        }
    }

    public void preFormRender(UIContext context, Form form)
    {
        int tick = (int) context.getTick();

        if (this.statesEditor.isVisible())
        {
            AnimationState state = this.statesKeyframes.getState();

            if (state != null)
            {
                if (this.playing)
                {
                    if (tick != this.lastTick)
                    {
                        this.cursor += 1;
                    }

                    if (this.cursor >= state.duration.get())
                    {
                        this.playing = false;
                        this.cursor = 0;
                    }
                }

                state.properties.applyProperties(this.cursor + (this.playing ? context.getTransition() : 0), form);
            }
        }

        this.lastTick = tick;
    }

    @Override
    public void render(UIContext context)
    {
        if (this.undoHandler != null)
        {
            this.undoHandler.submitUndo();
        }

        super.render(context);
    }

    public Matrix4f getOrigin(float transition)
    {
        if (this.statesEditor.isVisible())
        {
            return this.statesKeyframes.getOrigin(transition);
        }

        return this.editor.getOrigin(transition);
    }

    @Override
    public int getCursor()
    {
        return this.cursor;
    }

    @Override
    public void setCursor(int tick)
    {
        this.cursor = tick;
    }
}