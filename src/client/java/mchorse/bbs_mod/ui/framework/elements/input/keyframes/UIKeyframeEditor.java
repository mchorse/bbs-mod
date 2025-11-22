package mchorse.bbs_mod.ui.framework.elements.input.keyframes;

import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.ui.film.utils.CameraAxisConverter;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UITransformKeyframeFactory;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class UIKeyframeEditor extends UIElement
{
    public static final int[] COLORS = {Colors.RED, Colors.GREEN, Colors.BLUE, Colors.CYAN, Colors.MAGENTA, Colors.YELLOW, Colors.LIGHTEST_GRAY & 0xffffff, Colors.DEEP_PINK};
    public static final CameraAxisConverter CONVERTER = new CameraAxisConverter();

    public UIKeyframes view;
    public UIKeyframeFactory editor;

    private UIElement target;

    public UIKeyframeEditor(Function<Consumer<Keyframe>, UIKeyframes> factory)
    {
        this.view = factory.apply(this::pickKeyframe);

        this.view.changed(() ->
        {
            if (this.editor != null)
            {
                this.editor.update();
            }
        });

        this.add(this.view.full(this));
    }

    public UIKeyframeEditor target(UIElement target)
    {
        this.target = target;

        return this;
    }

    private void pickKeyframe(Keyframe keyframe)
    {
        UIKeyframeFactory.saveScroll(this.editor);

        if (this.editor != null)
        {
            this.editor.removeFromParent();
            this.editor = null;
        }

        if (keyframe != null)
        {
            this.editor = UIKeyframeFactory.createPanel(keyframe, this.view);

            if (this.target != null)
            {
                this.editor.full(this.target);

                this.target.resize();
            }
            else
            {
                this.editor.relative(this).x(1F, -140).w(140).h(1F);
            }

            this.add(this.editor);
            this.resize();
        }

        this.view.w(1F, keyframe == null || this.target != null ? 0 : -140);
        this.resize();
    }

    public void updateConverter()
    {
        this.view.axisConverter(CONVERTER);
    }

    public void setChannel(KeyframeChannel channel, int color)
    {
        this.view.removeAllSheets();
        this.view.addSheet(new UIKeyframeSheet(color, false, channel, null));

        this.pickKeyframe(null);
    }

    public void setClip(KeyframeClip clip)
    {
        this.view.removeAllSheets();

        for (int i = 0; i < clip.channels.length; i++)
        {
            KeyframeChannel channel = clip.channels[i];

            this.view.addSheet(new UIKeyframeSheet(COLORS[i], false, channel, null));
        }

        this.pickKeyframe(null);
    }

    public UIKeyframeSheet getSheet(Keyframe keyframe)
    {
        if (keyframe == null)
        {
            return null;
        }

        for (UIKeyframeSheet sheet : this.view.getGraph().getSheets())
        {
            if (sheet.channel == keyframe.getParent())
            {
                return sheet;
            }
        }

        return null;
    }

    public Pair<String, Boolean> getBone()
    {
        UIKeyframeFactory editor = this.editor;
        String bone = null;
        boolean local = false;

        if (editor instanceof UIPoseKeyframeFactory pose)
        {
            UIKeyframeSheet sheet = this.getSheet(editor.getKeyframe());
            String currentFirst = pose.poseEditor.groups.getCurrentFirst();

            if (sheet != null)
            {
                boolean isPose = sheet.id.endsWith("pose") || sheet.id.contains("pose_overlay");

                if (isPose)
                {
                    String targetBone = sheet.anchoredBone != null && !sheet.anchoredBone.isEmpty() ? sheet.anchoredBone : currentFirst;

                    /* Si el id incluye una ruta de propiedad (p.ej. formPath/pose o formPath/pose_overlayX),
                     * mantener el prefijo del formulario para ubicar correctamente el hueso en el renderer. */
                    if (sheet.id.contains("/pose") || sheet.id.contains("/pose_overlay"))
                    {
                        bone = sheet.id.substring(0, sheet.id.lastIndexOf('/') + 1) + targetBone;
                    }
                    else
                    {
                        bone = targetBone;
                    }

                    local = pose.poseEditor.transform.isLocal();
                }
            }
        }
        else if (editor instanceof UITransformKeyframeFactory)
        {
            UIKeyframeSheet sheet = this.getSheet(editor.getKeyframe());

            if (sheet != null)
            {
                if (sheet.id.endsWith("transform"))
                {
                    bone = sheet.id.endsWith("/transform") ? sheet.id.substring(0, sheet.id.lastIndexOf('/')) : "";
                }
                else if (sheet.id.contains("transform_overlay"))
                {
                    int slash = sheet.id.lastIndexOf('/');
                    bone = slash >= 0 ? sheet.id.substring(0, slash) : "";
                }
            }
        }

        if (bone != null)
        {
            return new Pair<>(bone, local);
        }

        return null;
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        KeyframeState state = new KeyframeState();

        state.extra = data.getMap("extra");

        for (BaseType type : data.getList("selection"))
        {
            state.selected.add(DataStorageUtils.intListFromData(type));
        }

        this.view.applyState(state);
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        KeyframeState keyframeState = this.view.cacheState();
        ListType selection = new ListType();

        for (List<Integer> integers : keyframeState.selected)
        {
            selection.add(DataStorageUtils.intListToData(integers));
        }

        data.put("extra", keyframeState.extra);
        data.put("selection", selection);
    }
}