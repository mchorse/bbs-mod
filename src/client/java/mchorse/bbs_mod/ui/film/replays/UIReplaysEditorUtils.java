package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.AnimationPart;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.film.ICursor;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UITransformKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.IUIKeyframeGraph;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class UIReplaysEditorUtils
{
    public static UIPropTransform getEditableTransform(UIKeyframeEditor editor)
    {
        if (editor == null || editor.editor == null)
        {
            return null;
        }

        if (editor.editor instanceof UITransformKeyframeFactory transformKeyframeFactory)
        {
            return transformKeyframeFactory.transform;
        }
        else if (editor.editor instanceof UIPoseKeyframeFactory keyframeFactory)
        {
            return keyframeFactory.poseEditor.transform;
        }

        return null;
    }

    /* Picking form and form properties */

    public static void pickFormProperty(UIContext context, UIKeyframeEditor editor, ICursor cursor, Form form, String bone)
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

            manager.action(UIReplaysEditor.getIcon(formProperty.getId()), IKey.constant(formProperty.getId()), () ->
            {
                pickProperty(editor, cursor, bone, StringUtils.combinePaths(path, formProperty.getId()), shift);
            });
        }

        context.replaceContextMenu(manager.create());
    }

    public static void pickForm(UIKeyframeEditor keyframeEditor, ICursor cursor, Form form, String bone)
    {
        String path = FormUtils.getPath(form);

        if (keyframeEditor == null || bone.isEmpty())
        {
            return;
        }

        Keyframe selected = keyframeEditor.view.getGraph().getSelected();
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
        else
        {
            UIKeyframeSheet lastSheet = keyframeEditor.view.getGraph().getLastSheet();

            if (lastSheet != null && lastSheet.property != null)
            {
                if (FormUtils.getPath((Form) lastSheet.property.getParent()).equals(FormUtils.getPath(form)) && lastSheet.property.getId().startsWith("pose") && !lastSheet.channel.isEmpty())
                {
                    type = lastSheet.id;
                }
            }
        }

        UIKeyframeSheet sheet = keyframeEditor.view.getGraph().getSheet(StringUtils.combinePaths(path, type));

        if (sheet != null && sheet.channel.isEmpty())
        {
            type = "pose";
        }

        pickProperty(keyframeEditor, cursor, bone, StringUtils.combinePaths(path, type), false);
    }

    private static void pickProperty(UIKeyframeEditor keyframeEditor, ICursor cursor, String bone, String key, boolean insert)
    {
        UIKeyframeSheet sheet = keyframeEditor.view.getGraph().getSheet(key);

        if (sheet != null)
        {
            pickProperty(keyframeEditor, cursor, bone, sheet, insert);
        }
    }

    private static void pickProperty(UIKeyframeEditor keyframeEditor, ICursor filmPanel, String bone, UIKeyframeSheet sheet, boolean insert)
    {
        IUIKeyframeGraph graph = keyframeEditor.view.getGraph();
        int tick = filmPanel.getCursor();

        if (insert)
        {
            Keyframe keyframe = graph.addKeyframe(sheet, tick, null);

            graph.selectKeyframe(keyframe);

            return;
        }

        KeyframeSegment segment = sheet.channel.find(tick);

        if (segment != null)
        {
            Keyframe closest = segment.getClosest();

            if (graph.getSelected() != closest)
            {
                boolean select = true;

                for (UIKeyframeSheet graphSheet : graph.getSheets())
                {
                    if (graphSheet.selection.getSelected().contains(closest))
                    {
                        select = false;

                        break;
                    }
                }

                if (select) graph.selectKeyframe(closest);
                else graph.pickKeyframe(closest);
            }

            if (keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory)
            {
                poseFactory.poseEditor.selectBone(bone);
            }

            filmPanel.setCursor((int) closest.getTick());
        }
    }

    /* Converting Blockbench model keyframes to pose keyframes */

    public static void animationToPoseKeyframes(
        UIKeyframeEditor keyframeEditor, UIKeyframeSheet sheet,
        ModelForm modelForm, IEntity entity,
        int tick, String animationKey, boolean onlyKeyframes, int length, int step
    ) {
        ModelInstance model = ModelFormRenderer.getModel(modelForm);
        Animation animation = model.animations.get(animationKey);

        if (animation != null)
        {
            keyframeEditor.view.getDopeSheet().clearSelection();

            if (onlyKeyframes)
            {
                List<Float> list = getTicks(animation);

                for (float i : list)
                {
                    fillAnimationPose(sheet, i, model, entity, animation, tick);
                }
            }
            else
            {
                for (int i = 0; i < length; i += step)
                {
                    fillAnimationPose(sheet, i, model, entity, animation, tick);
                }
            }

            keyframeEditor.view.getDopeSheet().pickSelected();
        }
    }

    private static List<Float> getTicks(Animation animation)
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

    private static void fillAnimationPose(UIKeyframeSheet sheet, float i, ModelInstance model, IEntity entity, Animation animation, int current)
    {
        model.model.resetPose();
        model.model.apply(entity, animation, i, 1F, 0F, false);

        int insert = sheet.channel.insert(current + i, model.model.createPose());

        sheet.selection.add(insert);
    }

    /* Offer bone hierarchy options */

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
}