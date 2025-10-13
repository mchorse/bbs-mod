package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector3d;

import java.util.function.Consumer;

public class UITransformKeyframeFactory extends UIKeyframeFactory<Transform>
{
    private UIPropTransform transform;

    public UITransformKeyframeFactory(Keyframe<Transform> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.transform = new UIPoseTransforms(this);
        this.transform.enableHotkeys();
        this.transform.setTransform(keyframe.getValue());

        this.scroll.add(this.transform);
    }

    public static class UIPoseTransforms extends UIPropTransform
    {
        private UITransformKeyframeFactory editor;

        public UIPoseTransforms(UITransformKeyframeFactory editor)
        {
            this.editor = editor;
        }

        public static void apply(UIKeyframes editor, Keyframe keyframe, Consumer<Transform> consumer)
        {
            for (UIKeyframeSheet sheet : editor.getGraph().getSheets())
            {
                if (sheet.channel.getFactory() != keyframe.getFactory())
                {
                    continue;
                }

                for (Keyframe kf : sheet.selection.getSelected())
                {
                    if (kf.getValue() instanceof Transform transform)
                    {
                        kf.preNotify();
                        consumer.accept(transform);
                        kf.postNotify();
                    }
                }
            }
        }

        @Override
        public void pasteTranslation(Vector3d translation)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.translate.set(translation));
            this.setTransform(this.getTransform());
        }

        @Override
        public void pasteScale(Vector3d scale)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.scale.set(scale));
            this.setTransform(this.getTransform());
        }

        @Override
        public void pasteRotation(Vector3d rotation)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.rotate.set(Vectors.toRad(rotation)));
            this.setTransform(this.getTransform());
        }

        @Override
        public void pasteRotation2(Vector3d rotation)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.rotate2.set(Vectors.toRad(rotation)));
            this.setTransform(this.getTransform());
        }

        @Override
        public void setT(Axis axis, double x, double y, double z)
        {
            float dx = (float) (x - this.getTransform().translate.x);
            float dy = (float) (y - this.getTransform().translate.y);
            float dz = (float) (z - this.getTransform().translate.z);

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.translate.x += dx;
                poseT.translate.y += dy;
                poseT.translate.z += dz;
            });
        }

        @Override
        public void setS(Axis axis, double x, double y, double z)
        {
            float dx = (float) (x - this.getTransform().scale.x);
            float dy = (float) (y - this.getTransform().scale.y);
            float dz = (float) (z - this.getTransform().scale.z);

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.scale.x += dx;
                poseT.scale.y += dy;
                poseT.scale.z += dz;
            });
        }

        @Override
        public void setR(Axis axis, double x, double y, double z)
        {
            float dx = MathUtils.toRad((float) x) - this.getTransform().rotate.x;
            float dy = MathUtils.toRad((float) y) - this.getTransform().rotate.y;
            float dz = MathUtils.toRad((float) z) - this.getTransform().rotate.z;

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.rotate.x += dx;
                poseT.rotate.y += dy;
                poseT.rotate.z += dz;
            });
        }

        @Override
        public void setR2(Axis axis, double x, double y, double z)
        {
            float dx = MathUtils.toRad((float) x) - this.getTransform().rotate2.x;
            float dy = MathUtils.toRad((float) y) - this.getTransform().rotate2.y;
            float dz = MathUtils.toRad((float) z) - this.getTransform().rotate2.z;

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.rotate2.x += dx;
                poseT.rotate2.y += dy;
                poseT.rotate2.z += dz;
            });
        }
    }
}