package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIModelPoseEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.shapes.UIShapeKeys;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;

import java.util.Set;

public class UIModelFormPanel extends UIFormPanel<ModelForm>
{
    public UIColor color;
    public UIModelPoseEditor poseEditor;
    public UIShapeKeys shapeKeys;

    public UIButton pickModel;
    public UIButton pick;

    public UIModelFormPanel(UIForm editor)
    {
        super(editor);

        this.pickModel = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_MODEL, (b) ->
        {
            UIListOverlayPanel list = new UIListOverlayPanel(UIKeys.FORMS_EDITOR_MODEL_MODELS, (l) ->
            {
                this.form.model.set(l);

                if (Window.isCtrlPressed())
                {
                    ModelInstance model = ModelFormRenderer.getModel(this.form);

                    if (model != null)
                    {
                        this.form.texture.set(model.texture);
                    }
                }

                this.editor.startEdit(this.form);
            });

            list.addValues(BBSModClient.getModels().getAvailableKeys());
            list.list.list.sort();
            list.setValue(this.form.model.get());

            UIOverlay.addOverlay(this.getContext(), list);
        });
        this.color = new UIColor((c) -> this.form.color.set(new Color().set(c))).withAlpha();
        this.color.direction(Direction.LEFT);
        this.poseEditor = new UIModelPoseEditor();
        /* Proveedor de textura por defecto: usa la textura del formulario si existe,
         * de lo contrario la textura del modelo instanciado. */
        this.poseEditor.setDefaultTextureSupplier(() ->
        {
            Link base = this.form.texture.get();
            if (base != null)
            {
                return base;
            }

            ModelInstance model = ModelFormRenderer.getModel(this.form);
            return model != null ? model.texture : null;
        });
        this.shapeKeys = new UIShapeKeys();
        this.pick = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_TEXTURE, (b) ->
        {
            Link link = this.form.texture.get();
            ModelInstance model = ModelFormRenderer.getModel(this.form);

            if (model != null && link == null)
            {
                link = model.texture;
            }

            UITexturePicker.open(this.getContext(), link, (l) -> this.form.texture.set(l));
        });

        this.options.add(this.pickModel, this.pick, this.color, this.poseEditor);
    }

    private void pickGroup(String group)
    {
        this.poseEditor.selectBone(group);
    }

    @Override
    public void startEdit(ModelForm form)
    {
        super.startEdit(form);

        ModelInstance model = ModelFormRenderer.getModel(this.form);

        this.poseEditor.setValuePose(form.pose);
        this.poseEditor.setPose(form.pose.get(), model == null ? this.form.model.get() : model.poseGroup);
        this.poseEditor.fillGroups(model == null ? null : model.model, model == null ? null : model.flippedParts, true);
        this.color.setColor(form.color.get().getARGBColor());

        this.shapeKeys.removeFromParent();

        if (model != null)
        {
            Set<String> modelShapeKeys = model.model.getShapeKeys();

            if (!modelShapeKeys.isEmpty())
            {
                this.options.add(this.shapeKeys);
                this.shapeKeys.setShapeKeys(model.poseGroup, modelShapeKeys, this.form.shapeKeys.get());
            }
        }

        this.options.resize();
    }

    @Override
    public void pickBone(String bone)
    {
        super.pickBone(bone);

        this.pickGroup(bone);
    }
}