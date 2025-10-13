package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.shapes.UIShapeKeys;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.Set;

public class UIShapeKeysKeyframeFactory extends UIKeyframeFactory<ShapeKeys>
{
    private UIShapeKeys shapeKeys;

    public UIShapeKeysKeyframeFactory(Keyframe<ShapeKeys> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
        ModelForm form = (ModelForm) FormUtils.getForm(sheet.property);
        ModelInstance model = ((ModelFormRenderer) FormUtilsClient.getRenderer(form)).getModel();
        Set<String> shapeKeys = model.model.getShapeKeys();

        this.shapeKeys = new UIShapeKeysEditor(this);

        if (!shapeKeys.isEmpty())
        {
            this.shapeKeys.setShapeKeys(model.poseGroup, shapeKeys, keyframe.getValue());
            this.scroll.add(this.shapeKeys);
        }
    }

    public static class UIShapeKeysEditor extends UIShapeKeys
    {
        private UIShapeKeysKeyframeFactory editor;

        public UIShapeKeysEditor(UIShapeKeysKeyframeFactory editor)
        {
            this.editor = editor;
        }

        @Override
        protected void changedShapeKeys(Runnable runnable)
        {
            super.changedShapeKeys(runnable);
        }

        @Override
        protected void setValue(float v)
        {
            this.editor.keyframe.preNotify();
            super.setValue(v);
            this.editor.keyframe.postNotify();
        }
    }
}