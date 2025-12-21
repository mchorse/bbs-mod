package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.UIGeneralFormPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIPanelBase;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Matrices;
import org.joml.Matrix4f;

public abstract class UIForm <T extends Form> extends UIPanelBase<UIFormPanel<T>>
{
    public UIFormEditor editor;

    public T form;
    public UIFormPanel<T> defaultPanel;
    public UIGeneralFormPanel generalPanel;

    private UIPropTransform general;

    public UIForm()
    {
        super(Direction.LEFT);

        this.keys().register(Keys.FILM_CONTROLLER_CYCLE_EDITORS, this::cyclePanels);
    }

    public UIPropTransform getEditableTransform()
    {
        this.setPanel(this.generalPanel);

        return this.general;
    }

    private void cyclePanels()
    {
        int index = this.panels.indexOf(this.view);
        int newIndex = MathUtils.cycler(index + (Window.isShiftPressed() ? -1 : 1), this.panels);

        this.setPanel(this.panels.get(newIndex));
        UIUtils.playClick();
    }

    public Matrix4f getOrigin(float transition)
    {
        return this.getOrigin(transition, FormUtils.getPath(this.form), this.generalPanel != null && this.generalPanel.transform.isLocal());
    }

    protected Matrix4f getOrigin(float transition, String path, boolean local)
    {
        Form root = FormUtils.getRoot(this.form);
        MatrixCache map = FormUtilsClient.getRenderer(root).collectMatrices(this.editor.renderer.getTargetEntity(), transition);
        Matrix4f matrix = local ? map.get(path).matrix() : map.get(path).origin();

        return matrix == null ? Matrices.EMPTY_4F : matrix;
    }

    protected void registerDefaultPanels()
    {
        UIGeneralFormPanel panel = new UIGeneralFormPanel(this);

        this.registerPanel(panel, UIKeys.FORMS_EDITORS_GENERAL, Icons.GEAR);

        this.generalPanel = panel;
        this.general = panel.transform;
    }

    public void setEditor(UIFormEditor editor)
    {
        this.editor = editor;
    }

    public void startEdit(T form)
    {
        this.form = form;

        for (UIFormPanel<T> panel : this.panels)
        {
            panel.startEdit(form);
        }

        this.setPanel(this.defaultPanel);
    }

    public void finishEdit()
    {
        for (UIFormPanel<T> panel : this.panels)
        {
            panel.finishEdit();
        }
    }

    public void pickBone(String bone)
    {
        if (this.view != null)
        {
            this.view.pickBone(bone);
        }
    }

    @Override
    protected void renderBackground(UIContext context, int x, int y, int w, int h)
    {
        context.batcher.box(x, y, x + w, y + h, Colors.A100);
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        data.putInt("panel", this.panels.indexOf(this.view));
        data.putDouble("scroll", this.view.options.scroll.getScroll());
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        this.setPanel(this.panels.get(data.getInt("panel")));
        this.view.options.scroll.setScroll(data.getDouble("scroll"));
    }
}