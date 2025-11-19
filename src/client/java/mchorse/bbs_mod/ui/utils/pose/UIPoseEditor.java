package mchorse.bbs_mod.ui.utils.pose;

import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UIDataContextMenu;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseManager;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.utils.resources.LinkUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIPoseEditor extends UIElement
{
    private static String lastLimb = "";

    public UIStringList groups;
    public UITrackpad fix;
    public UIButton pickTexture;
    public UIColor color;
    public UIToggle lighting;
    public UIPropTransform transform;

    private String group = "";
    private Pose pose;
    protected IModel model;
    protected Map<String, String> flippedParts;
    /** Proveedor opcional para obtener la textura base del modelo cuando no hay override por hueso. */
    protected Supplier<Link> defaultTextureSupplier;

    public UIPoseEditor()
    {
        this.groups = new UIStringList((l) -> this.pickBone(l.get(0)));
        this.groups.background().h(UIStringList.DEFAULT_HEIGHT * 8 - 8);
        this.groups.scroll.cancelScrolling();
        this.groups.context(() ->
        {
            UIDataContextMenu menu = new UIDataContextMenu(PoseManager.INSTANCE, this.group, () -> this.pose.toData(), this::pastePose);
            UIIcon flip = new UIIcon(Icons.CONVERT, (b) -> this.flipPose());

            flip.tooltip(UIKeys.POSE_CONTEXT_FLIP_POSE);
            menu.row.addBefore(menu.save, flip);

            return menu;
        });
        this.fix = new UITrackpad((v) ->
        {
            if (this.transform.getTransform() instanceof PoseTransform poseTransform)
            {
                this.setFix(poseTransform, v.floatValue());
            }
        });
        this.fix.limit(0D, 1D).increment(1D).values(0.1, 0.05D, 0.2D);
        this.fix.tooltip(UIKeys.POSE_CONTEXT_FIX_TOOLTIP);
        this.fix.context((menu) ->
        {
            menu.action(Icons.DOWNLOAD, UIKeys.POSE_CONTEXT_APPLY, () ->
            {
                this.applyChildren((p) -> this.setFix(p, (float) this.fix.getValue()));
            });
        });
        /* Botón para elegir textura de hueso (etiqueta fija ES/EN) */
        this.pickTexture = new UIButton(UIKeys.TEXTURE_PICK_BONE_TEXTURE, (b) ->
        {
            PoseTransform poseTransform = (PoseTransform) this.transform.getTransform();
            Link current = null;

            if (poseTransform != null && poseTransform.texture != null)
            {
                current = poseTransform.texture;
            }
            else if (this.defaultTextureSupplier != null)
            {
                current = this.defaultTextureSupplier.get();
            }

            UITexturePicker.open(this.getContext(), current, (l) ->
            {
                if (this.transform.getTransform() instanceof PoseTransform pt)
                {
                    this.setTexture(pt, l);
                }
            });
        });
        this.pickTexture.context((menu) ->
        {
            menu.action(Icons.DOWNLOAD, UIKeys.POSE_CONTEXT_APPLY, () ->
            {
                PoseTransform t = (PoseTransform) this.transform.getTransform();
                Link chosen = t != null ? t.texture : null;
                this.applyChildren((p) -> this.setTexture(p, chosen));
            });

            menu.action(Icons.CLOSE, UIKeys.GENERAL_NONE, () ->
            {
                PoseTransform t = (PoseTransform) this.transform.getTransform();
                if (t != null)
                {
                    this.setTexture(t, null);
                }
            });
        });
        this.color = new UIColor((c) ->
        {
            if (this.transform.getTransform() instanceof PoseTransform poseTransform)
            {
                this.setColor(poseTransform, c);
            }
        });
        this.color.withAlpha();
        this.color.context((menu) ->
        {
            menu.action(Icons.DOWNLOAD, UIKeys.POSE_CONTEXT_APPLY, () ->
            {
                this.applyChildren((p) -> this.setColor(p, this.color.picker.color.getARGBColor()));
            });
        });
        this.lighting = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING, (b) ->
        {
            if (this.transform.getTransform() instanceof PoseTransform poseTransform)
            {
                this.setLighting(poseTransform, b.getValue());
            }
        });
        this.lighting.h(20);
        this.lighting.context((menu) ->
        {
            menu.action(Icons.DOWNLOAD, UIKeys.POSE_CONTEXT_APPLY, () ->
            {
                this.applyChildren((p) -> this.setLighting(p, this.lighting.getValue()));
            });
        });
        this.transform = this.createTransformEditor();
        this.transform.setModel();

        this.column().vertical().stretch();
        this.add(this.groups, UI.label(UIKeys.POSE_CONTEXT_FIX), this.fix, this.pickTexture, UI.row(this.color, this.lighting), this.transform);
    }

    /**
     * Establece un proveedor de textura por defecto para usar cuando no exista
     * una textura específica del hueso. Devuelve this para permitir chaining.
     */
    public UIPoseEditor setDefaultTextureSupplier(Supplier<Link> supplier)
    {
        this.defaultTextureSupplier = supplier;

        return this;
    }

    private void applyChildren(Consumer<PoseTransform> consumer)
    {
        if (this.model == null)
        {
            return;
        }

        PoseTransform t = (PoseTransform) this.transform.getTransform();
        Collection<String> keys = this.model.getAllChildrenKeys(CollectionUtils.getKey(this.pose.transforms, t));

        for (String key : keys)
        {
            consumer.accept(this.pose.get(key));
        }
    }

    public Pose getPose()
    {
        return this.pose;
    }

    public String getGroup()
    {
        return this.groups.getCurrentFirst();
    }

    protected void pastePose(MapType data)
    {
        String current = this.groups.getCurrentFirst();

        this.pose.fromData(data);
        this.pickBone(current);
    }

    protected void flipPose()
    {
        String current = this.groups.getCurrentFirst();

        this.pose.flip(this.flippedParts);
        this.pickBone(current);
    }

    public void setPose(Pose pose, String group)
    {
        this.pose = pose;
        this.group = group;
    }

    public void fillGroups(Collection<String> groups, boolean reset)
    {
        this.model = null;
        this.flippedParts = null;

        this.fillInGroups(groups, reset);
    }

    public void fillGroups(IModel model, Map<String, String> flippedParts, boolean reset)
    {
        this.model = model;
        this.flippedParts = flippedParts;

        this.fillInGroups(model == null ? Collections.emptyList() : model.getAllGroupKeys(), reset);
    }

    private void fillInGroups(Collection<String> groups, boolean reset)
    {
        this.groups.clear();
        this.groups.add(groups);
        this.groups.sort();

        this.fix.setVisible(!groups.isEmpty());
        this.color.setVisible(!groups.isEmpty());
        this.transform.setVisible(!groups.isEmpty());

        List<String> list = this.groups.getList();
        int i = reset ? 0 : list.indexOf(lastLimb);

        this.groups.setCurrentScroll(CollectionUtils.getSafe(list, i));
        this.pickBone(this.groups.getCurrentFirst());
    }

    public void selectBone(String bone)
    {
        lastLimb = bone;

        this.groups.setCurrentScroll(bone);
        this.pickBone(bone);
    }

    /* Subclass overridable methods */

    protected UIPropTransform createTransformEditor()
    {
        return new UIPropTransform().enableHotkeys();
    }

    protected void pickBone(String bone)
    {
        lastLimb = bone;

        PoseTransform poseTransform = this.pose.get(bone);

        if (poseTransform != null)
        {
            this.fix.setValue(poseTransform.fix);
            this.color.setColor(poseTransform.color.getARGBColor());
            this.lighting.setValue(poseTransform.lighting == 0F);
            this.transform.setTransform(poseTransform);
        }
        else
        {
            this.fix.setValue(0F);
            this.color.setColor(Colors.WHITE);
            this.lighting.setValue(false);
            this.transform.setTransform(null);
        }
    }

    protected void setFix(PoseTransform transform, float value)
    {
        transform.fix = value;
    }

    protected void setColor(PoseTransform transform, int value)
    {
        transform.color.set(value);
    }

    protected void setLighting(PoseTransform poseTransform, boolean value)
    {
        poseTransform.lighting = value ? 0F : 1F;
    }

    protected void setTexture(PoseTransform transform, Link value)
    {
        transform.texture = LinkUtils.copy(value);
    }
}