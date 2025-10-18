package mchorse.bbs_mod.ui.forms.editors;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
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
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
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
import mchorse.bbs_mod.ui.forms.editors.forms.UITrailForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIVanillaParticleForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UIPickableFormRenderer;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UICopyPasteController;
import mchorse.bbs_mod.ui.utils.presets.UIPresetContextMenu;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.presets.PresetManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIFormEditor extends UIElement implements IUIFormList
{
    private static Map<Class, Supplier<UIForm>> panels = new HashMap<>();

    private static float treeWidth = 0F;
    private static boolean TOGGLED = true;

    public UIFormPalette palette;

    public UIElement formsArea;
    public UIForms forms;
    public UIScrollView bodyPartData;

    public UIButton pick;
    public UIToggle useTarget;
    public UIStringList bone;
    public UIPropTransform transform;

    public UIElement editArea;
    public UIPickableFormRenderer renderer;
    public UIForm editor;

    public UIIcon finish;
    public UIIcon toggleSidebar;

    public Form form;

    private Consumer<Form> callback;
    private UICopyPasteController copyPasteController;

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

        this.copyPasteController = new UICopyPasteController(PresetManager.BODY_PARTS, "_FormEditorBodyPart")
            .supplier(this::copyBodyPart)
            .consumer(this::pasteBodyPart)
            .canCopy(() ->
            {
                UIForms.FormEntry current = this.forms.getCurrentFirst();

                return current != null && current.part != null;
            })
            .canPaste(() ->
            {
                UIForms.FormEntry current = this.forms.getCurrentFirst();

                return current != null && current.getForm() != null;
            });

        this.formsArea = new UIElement();
        this.formsArea.relative(this).x(20).w(treeWidth).minW(140).h(1F);

        this.forms = new UIForms((l) -> this.pickForm(l.get(0)));
        this.forms.relative(this.formsArea).w(1F).h(0.5F);
        this.forms.context(this::createFormContextMenu);

        this.bodyPartData = UI.scrollView(5, 10);
        this.bodyPartData.scroll.cancelScrolling();
        this.bodyPartData.relative(this.formsArea).w(1F).y(0.5F).h(0.5F);

        this.pick = new UIButton(UIKeys.FORMS_EDITOR_PICK_FORM, (b) ->
        {
            UIForms.FormEntry current = this.forms.getCurrentFirst();

            this.openFormList(current.part.getForm(), (f) ->
            {
                current.part.setForm(FormUtils.copy(f));

                Form partForm = current.part.getForm();

                if (partForm != null && partForm.getId().contains("particle"))
                {
                    current.part.useTarget = true;

                    this.useTarget.setValue(true);
                }

                this.refreshFormList();
                this.switchEditor(partForm);
            });
        });

        this.useTarget = new UIToggle(UIKeys.FORMS_EDITOR_USE_TARGET, (b) ->
        {
            this.forms.getCurrentFirst().part.useTarget = b.getValue();
        });

        this.bone = new UIStringList((l) -> this.forms.getCurrentFirst().part.bone = l.get(0));
        this.bone.background().h(16 * 6);

        this.transform = new UIPropTransform();

        this.editArea = new UIElement();
        this.editArea.full(this);

        this.renderer = new UIPickableFormRenderer(this);
        this.renderer.full(this.editArea);

        this.finish = new UIIcon(Icons.IN, (b) -> this.palette.exit());
        this.finish.tooltip(UIKeys.FORMS_EDITOR_FINISH, Direction.RIGHT).relative(this.editArea).xy(0, 1F).anchorY(1F);

        this.toggleSidebar = new UIIcon(() -> this.formsArea.isVisible() ? Icons.LEFTLOAD : Icons.RIGHTLOAD, (b) ->
        {
            this.toggleSidebar();

            TOGGLED = !TOGGLED;
        });
        this.toggleSidebar.tooltip(UIKeys.FORMS_EDITOR_TOGGLE_TREE, Direction.RIGHT).relative(this.finish).y(-1F);

        UIRenderable background = new UIRenderable((context) ->
        {
            if (this.formsArea.isVisible())
            {
                this.formsArea.area.render(context.batcher, Colors.A50);
            }
        });

        this.formsArea.add(background, this.forms, this.bodyPartData);
        this.editArea.add(this.finish, this.toggleSidebar);
        this.add(this.editArea, this.formsArea);

        UIDraggable draggable = new UIDraggable((context) ->
        {
            int diff = context.mouseX - this.formsArea.area.x;
            float f = diff / (float) this.area.w;

            treeWidth = MathUtils.clamp(f, 0F, 0.5F);

            this.formsArea.w(treeWidth).resize();
        });

        draggable.relative(this.formsArea).x(1F).y(0.5F).w(6).h(40).anchor(0.5F, 0.5F);
        this.formsArea.add(draggable);

        this.pick.keys().register(Keys.FORMS_EDIT, this.pick::clickItself);
    }

    public void pickFormFromRenderer(Pair<Form, String> pair)
    {
        if (Window.isCtrlPressed() && !pair.b.isEmpty())
        {
            /* Ctrl + clicking to pick the parent bone to attach to */
            BodyPart part = this.forms.getCurrentFirst().part;

            if (part != null && this.bone.getList().contains(pair.b) && part.getManager().getOwner() == pair.a)
            {
                this.bone.setCurrentScroll(part.bone = pair.b);
            }
        }
        else if (Window.isAltPressed()) UIReplaysEditor.offerAdjacent(this.getContext(), pair.a, pair.b, (bone) -> this.pickFormBone(pair.a, bone));
        else if (Window.isShiftPressed()) UIReplaysEditor.offerHierarchy(this.getContext(), pair.a, pair.b, (bone) -> this.pickFormBone(pair.a, bone));
        else this.pickFormBone(pair.a, pair.b);
    }

    private void pickFormBone(Form form, String bone)
    {
        this.forms.setCurrentForm(form);
        this.pickForm(this.forms.getCurrentFirst());

        if (!bone.isEmpty())
        {
            this.editor.pickBone(bone);
        }
    }

    private void toggleSidebar()
    {
        this.formsArea.toggleVisible();
    }

    private void createFormContextMenu(ContextMenuManager menu)
    {
        UIForms.FormEntry current = this.forms.getCurrentFirst();

        if (current != null)
        {
            menu.custom(new UIPresetContextMenu(this.copyPasteController)
                .labels(UIKeys.FORMS_EDITOR_CONTEXT_COPY, UIKeys.FORMS_EDITOR_CONTEXT_PASTE));

            if (current.getForm() != null)
            {
                menu.action(Icons.ADD, UIKeys.FORMS_EDITOR_CONTEXT_ADD, () -> this.addBodyPart(new BodyPart()));
            }

            if (current.part != null)
            {
                List<BodyPart> all = current.part.getManager().getAll();

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
        List<BodyPart> all = manager.getAll();
        int index = all.indexOf(current.part);
        int newIndex = MathUtils.clamp(index + direction, 0, all.size() - 1);

        if (newIndex != index)
        {
            manager.moveBodyPart(current.part, newIndex);
            this.forms.setForm(this.form);

            UIForms.FormEntry selection = null;

            for (UIForms.FormEntry entry : this.forms.getList())
            {
                if (entry.part == current.part)
                {
                    selection = entry;

                    break;
                }
            }

            if (selection != null)
            {
                this.forms.setCurrentScroll(selection);
                this.pickForm(selection);
            }
        }
    }

    private void addBodyPart(BodyPart part)
    {
        UIForms.FormEntry current = this.forms.getCurrentFirst();

        current.getForm().parts.addBodyPart(part);
        this.refreshFormList();
    }

    private MapType copyBodyPart()
    {
        return this.forms.getCurrentFirst().part.toData();
    }

    private void pasteBodyPart(MapType data, int mouseX, int mouseY)
    {
        BodyPart part = new BodyPart();

        part.fromData(data);
        this.addBodyPart(part);
    }

    private void removeBodyPart()
    {
        int index = this.forms.getIndex();
        UIForms.FormEntry current = this.forms.getCurrentFirst();

        current.form.parts.removeBodyPart(current.part);

        this.refreshFormList();
        this.forms.setIndex(index - 1);
        this.pickForm(this.forms.getCurrentFirst());
    }

    private void pickForm(UIForms.FormEntry entry)
    {
        this.bodyPartData.setVisible(entry.part != null);

        if (entry.part != null)
        {
            this.bodyPartData.removeAll();

            this.useTarget.setValue(entry.part.useTarget);
            this.bone.clear();
            this.bone.add(FormUtilsClient.getBones(entry.form));
            this.bone.sort();
            this.bone.setCurrentScroll(entry.part.bone);

            if (!this.bone.getList().isEmpty())
            {
                this.bodyPartData.add(this.pick, this.useTarget, UI.label(UIKeys.FORMS_EDITOR_BONE).marginTop(8), this.bone, this.transform);
            }
            else
            {
                this.bodyPartData.add(this.pick, this.useTarget, this.transform);
            }

            this.transform.setTransform(entry.part.getTransform());

            this.bodyPartData.scroll.setScroll(0);
            this.bodyPartData.resize();
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

        form = form.copy();

        this.bodyPartData.setVisible(false);

        if (this.switchEditor(form))
        {
            this.form = form;

            if (TOGGLED != this.formsArea.isVisible())
            {
                this.toggleSidebar();
            }

            this.palette.accept(form);
            this.renderer.reset();
            this.renderer.form = form;
            this.refreshFormList();
            this.forms.setIndex(0);

            return true;
        }

        return false;
    }

    public void refreshFormList()
    {
        UIForms.FormEntry current = this.forms.getCurrentFirst();

        this.forms.setForm(this.form);
        this.forms.setCurrentScroll(current);
    }

    public boolean switchEditor(Form form)
    {
        UIForm editor = createPanel(form);

        if (editor == null)
        {
            return false;
        }

        if (this.editor != null)
        {
            this.editor.removeFromParent();
        }

        this.editor = editor;

        this.editArea.prepend(this.editor);

        this.editor.setEditor(this);
        this.editor.startEdit(form);
        this.editor.full(this.editArea).resize();

        this.renderer.removeFromParent();
        this.renderer.resize();
        this.editArea.prepend(this.renderer);

        return true;
    }

    public Form finish()
    {
        Form form = this.form;

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

}