package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector3d;

import java.util.List;
import java.util.function.Consumer;

public class UIPoseKeyframeFactory extends UIKeyframeFactory<Pose>
{
    public UIPoseFactoryEditor poseEditor;

    public UIPoseKeyframeFactory(Keyframe<Pose> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.poseEditor = new UIPoseFactoryEditor(editor, keyframe);

        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);

        if (FormUtils.getForm(sheet.property) instanceof ModelForm modelForm)
        {
            ModelInstance model = ((ModelFormRenderer) FormUtilsClient.getRenderer(modelForm)).getModel();

            if (model != null)
            {
                this.poseEditor.setPose(keyframe.getValue(), model.poseGroup);
                this.poseEditor.fillGroups(model.model, model.flippedParts, false);
            }
        }
        else if (FormUtils.getForm(sheet.property) instanceof MobForm mobForm)
        {
            List<String> bones = FormUtilsClient.getRenderer(mobForm).getBones();

            this.poseEditor.setPose(keyframe.getValue(), "");
            this.poseEditor.fillGroups(bones, false);
        }

        this.scroll.add(this.poseEditor);
    }

    @Override
    public void resize()
    {
        this.poseEditor.removeAll();

        if (this.getFlex().getW() > 240)
        {
            this.poseEditor.add(UI.row(
                UI.column(UI.label(UIKeys.POSE_CONTEXT_FIX), this.poseEditor.fix, UI.row(this.poseEditor.color, this.poseEditor.lighting), this.poseEditor.transform),
                UI.column(UI.label(UIKeys.FORMS_EDITOR_BONE), this.poseEditor.groups)
            ));
        }
        else
        {
            this.poseEditor.add(UI.label(UIKeys.FORMS_EDITOR_BONE), this.poseEditor.groups, UI.label(UIKeys.POSE_CONTEXT_FIX), this.poseEditor.fix, UI.row(this.poseEditor.color, this.poseEditor.lighting), this.poseEditor.transform);
        }

        /* Ew... */
        for (UIElement child : this.scroll.getChildren(UIElement.class))
        {
            child.noCulling();
        }

        super.resize();
    }

    public static class UIPoseFactoryEditor extends UIPoseEditor
    {
        private UIKeyframes editor;
        private Keyframe<Pose> keyframe;

        public static void apply(UIKeyframes editor, Keyframe keyframe, Consumer<Pose> consumer)
        {
            for (UIKeyframeSheet sheet : editor.getGraph().getSheets())
            {
                if (sheet.channel.getFactory() != keyframe.getFactory()) continue;

                for (Keyframe kf : sheet.selection.getSelected())
                {
                    if (kf.getValue() instanceof Pose pose)
                    {
                        kf.preNotify();
                        consumer.accept(pose);
                        kf.postNotify();
                    }
                }
            }
        }

        public static void apply(UIKeyframes editor, Keyframe keyframe, String group, Consumer<PoseTransform> consumer)
        {
            apply(editor, keyframe, (pose) -> consumer.accept(pose.get(group)));
        }

        public UIPoseFactoryEditor(UIKeyframes editor, Keyframe<Pose> keyframe)
        {
            super();

            this.editor = editor;
            this.keyframe = keyframe;

            ((UIPoseTransforms) this.transform).setKeyframe(this);
        }

        private String getGroup(PoseTransform transform)
        {
            return CollectionUtils.getKey(this.getPose().transforms, transform);
        }

        @Override
        protected UIPropTransform createTransformEditor()
        {
            return new UIPoseTransforms().enableHotkeys();
        }

        @Override
        protected void pastePose(MapType data)
        {
            String current = this.groups.getCurrentFirst();

            apply(this.editor, this.keyframe, (pose) -> pose.fromData(data));
            this.pickBone(current);
        }

        @Override
        protected void flipPose()
        {
            String current = this.groups.getCurrentFirst();

            apply(this.editor, this.keyframe, (pose) -> pose.flip(this.flippedParts));
            this.pickBone(current);
        }

        @Override
        protected void setFix(PoseTransform transform, float value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) -> poseT.fix = value);
        }

        @Override
        protected void setColor(PoseTransform transform, int value)
        {
            apply(this.editor, this.keyframe, this.getGroup(transform), (poseT) -> poseT.color.set(value));
        }

        @Override
        protected void setLighting(PoseTransform poseTransform, boolean value)
        {
            apply(this.editor, this.keyframe, this.getGroup(poseTransform), (poseT) -> poseT.lighting = value ? 0F : 1F);
        }
    }

    public static class UIPoseTransforms extends UIPropTransform
    {
        private UIPoseFactoryEditor editor;

        public void setKeyframe(UIPoseFactoryEditor editor)
        {
            this.editor = editor;
        }

        @Override
        protected void reset()
        {
            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) ->
            {
                poseT.translate.set(0F, 0F, 0F);
                poseT.scale.set(1F, 1F, 1F);
                poseT.rotate.set(0F, 0F, 0F);
                poseT.rotate2.set(0F, 0F, 0F);
            });
            this.refillTransform();
        }

        @Override
        public void pasteTranslation(Vector3d translation)
        {
            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) -> poseT.translate.set(translation));
            this.refillTransform();
        }

        @Override
        public void pasteScale(Vector3d scale)
        {
            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) -> poseT.scale.set(scale));
            this.refillTransform();
        }

        @Override
        public void pasteRotation(Vector3d rotation)
        {
            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) -> poseT.rotate.set(Vectors.toRad(rotation)));
            this.refillTransform();
        }

        @Override
        public void pasteRotation2(Vector3d rotation)
        {
            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) -> poseT.rotate2.set(Vectors.toRad(rotation)));
            this.refillTransform();
        }

        @Override
        public void setT(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = (float) (x - transform.translate.x);
            float dy = (float) (y - transform.translate.y);
            float dz = (float) (z - transform.translate.z);

            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) ->
            {
                poseT.translate.x += dx;
                poseT.translate.y += dy;
                poseT.translate.z += dz;
            });
        }

        @Override
        public void setS(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = (float) (x - transform.scale.x);
            float dy = (float) (y - transform.scale.y);
            float dz = (float) (z - transform.scale.z);

            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) ->
            {
                poseT.scale.x += dx;
                poseT.scale.y += dy;
                poseT.scale.z += dz;
            });
        }

        @Override
        public void setR(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate.z;

            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) ->
            {
                poseT.rotate.x += dx;
                poseT.rotate.y += dy;
                poseT.rotate.z += dz;
            });
        }

        @Override
        public void setR2(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate2.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate2.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate2.z;

            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) ->
            {
                poseT.rotate2.x += dx;
                poseT.rotate2.y += dy;
                poseT.rotate2.z += dz;
            });
        }
    }
}