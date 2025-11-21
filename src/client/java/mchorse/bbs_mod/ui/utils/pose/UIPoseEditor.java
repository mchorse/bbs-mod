package mchorse.bbs_mod.ui.utils.pose;

import mchorse.bbs_mod.BBSSettings;
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
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseManager;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.utils.resources.LinkUtils;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;

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
    public UIStringList categories;
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
    /** Gestor de categorías de huesos (por grupo de pose). */
    protected BoneCategoriesManager boneCategories = new BoneCategoriesManager();

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
        /* Lista de categorías a la derecha */
        this.categories = new UIStringList((l) -> {});
        this.categories.background().h(UIStringList.DEFAULT_HEIGHT * 8 - 8);
        this.categories.scroll.cancelScrolling();
        this.categories.context((menu) ->
        {
            String selectedCategory = this.categories.getCurrentFirst();

            menu.action(Icons.ADD, L10n.lang("bbs.ui.forms.categories.context.add_category"), () ->
            {
                UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                    L10n.lang("bbs.ui.pose.categories.manage_title"),
                    L10n.lang("bbs.ui.pose.categories.manage_category_name"),
                    (str) ->
                    {
                        if (str != null && !str.isEmpty())
                        {
                            this.boneCategories.addCategory(this.group, str);
                            this.refreshCategories();
                        }
                    }
                );
                UIOverlay.addOverlay(this.getContext(), panel);
            });

            if (selectedCategory != null && !selectedCategory.isEmpty())
            {
                menu.action(Icons.EDIT, L10n.lang("bbs.ui.forms.categories.context.rename_category"), () ->
                {
                    UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                        L10n.lang("bbs.ui.pose.categories.manage_title"),
                        L10n.lang("bbs.ui.pose.categories.manage_new_name"),
                        (str) ->
                        {
                            if (str != null && !str.isEmpty())
                            {
                                this.boneCategories.renameCategory(this.group, selectedCategory, str);
                                this.refreshCategories();
                            }
                        }
                    );
                    UIOverlay.addOverlay(this.getContext(), panel);
                });

                menu.action(Icons.TRASH, L10n.lang("bbs.ui.forms.categories.context.remove_category"), Colors.RED, () ->
                {
                    this.boneCategories.removeCategory(this.group, selectedCategory);
                    this.refreshCategories();
                });

                /* Ver huesos que pertenecen a la categoría seleccionada */
                menu.action(Icons.LIST, L10n.lang("bbs.ui.pose.categories.context.view_bones"), () ->
                {
                    String group = this.group;
                    java.util.List<String> bones = this.boneCategories.getBones(group, selectedCategory);

                    UISearchList<String> search = new UISearchList<>(new UIStringList(null));
                    UIList<String> list = search.list;

                    for (String g : bones) { list.add(g); }

                    UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(
                        L10n.lang("bbs.ui.pose.categories.view_bones_title"),
                        L10n.lang("bbs.ui.pose.categories.view_bones_description"),
                        (confirm) ->
                        {
                            if (confirm)
                            {
                                int index = list.getIndex();
                                String bone = CollectionUtils.getSafe(bones, index);
                                if (bone != null)
                                {
                                    this.selectBone(bone);
                                }
                            }
                        }
                    );

                    list.background();
                    /* Lista más alta y sin botones adicionales */
                    search.relative(panel.confirm).y(-5).w(1F).h(UIStringList.DEFAULT_HEIGHT * 12 + 20).anchor(0F, 1F);

                    /* Click derecho para eliminar el hueso de la categoría */
                    list.context((ctx) ->
                    {
                        ctx.action(Icons.TRASH, IKey.constant("Eliminar hueso"), Colors.RED, () ->
                        {
                            int idx = list.getIndex();
                            String bone = CollectionUtils.getSafe(bones, idx);
                            if (bone != null)
                            {
                                this.boneCategories.removeBone(group, selectedCategory, bone);
                                list.remove(bone);
                            }
                        });
                        ctx.autoKeys();
                    });

                    panel.content.add(search);
                    UIOverlay.addOverlay(this.getContext(), panel, 340, 360);
                });

                /* Separador visual no soportado por ContextMenuManager; omitido */

                String selectedBone = this.groups.getCurrentFirst();
                if (selectedBone != null && !selectedBone.isEmpty())
                {
                    menu.action(Icons.ADD, IKey.constant("Añadir hueso seleccionado"), () ->
                    {
                        this.boneCategories.addBone(this.group, selectedCategory, selectedBone);
                    });
                    menu.action(Icons.REMOVE, IKey.constant("Quitar hueso seleccionado"), () ->
                    {
                        this.boneCategories.removeBone(this.group, selectedCategory, selectedBone);
                    });
                }
            }

            menu.autoKeys();
        });
        this.fix = new UITrackpad((v) ->
        {
            String selectedCategory = this.categories != null ? this.categories.getCurrentFirst() : null;
            if (selectedCategory != null && !selectedCategory.isEmpty())
            {
                this.applyCategory((p) -> this.setFix(p, v.floatValue()));
            }
            else if (this.transform.getTransform() instanceof PoseTransform poseTransform)
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

            menu.action(Icons.DOWNLOAD, IKey.constant("Aplicar a categoría"), () ->
            {
                this.applyCategory((p) -> this.setFix(p, (float) this.fix.getValue()));
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
                String selectedCategory = this.categories != null ? this.categories.getCurrentFirst() : null;
                if (selectedCategory != null && !selectedCategory.isEmpty())
                {
                    this.applyCategory((p) -> this.setTexture(p, l));
                }
                else if (this.transform.getTransform() instanceof PoseTransform pt)
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
            menu.action(Icons.DOWNLOAD, IKey.constant("Aplicar a categoría"), () ->
            {
                PoseTransform t = (PoseTransform) this.transform.getTransform();
                Link chosen = t != null ? t.texture : null;
                this.applyCategory((p) -> this.setTexture(p, chosen));
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
            String selectedCategory = this.categories != null ? this.categories.getCurrentFirst() : null;
            if (selectedCategory != null && !selectedCategory.isEmpty())
            {
                this.applyCategory((p) -> this.setColor(p, c));
            }
            else if (this.transform.getTransform() instanceof PoseTransform poseTransform)
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
            menu.action(Icons.DOWNLOAD, IKey.constant("Aplicar a categoría"), () ->
            {
                this.applyCategory((p) -> this.setColor(p, this.color.picker.color.getARGBColor()));
            });
        });
        this.lighting = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING, (b) ->
        {
            String selectedCategory = this.categories != null ? this.categories.getCurrentFirst() : null;
            if (selectedCategory != null && !selectedCategory.isEmpty())
            {
                this.applyCategory((p) -> this.setLighting(p, b.getValue()));
            }
            else if (this.transform.getTransform() instanceof PoseTransform poseTransform)
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
            menu.action(Icons.DOWNLOAD, IKey.constant("Aplicar a categoría"), () ->
            {
                this.applyCategory((p) -> this.setLighting(p, this.lighting.getValue()));
            });
        });
        this.transform = this.createTransformEditor();
        this.transform.setModel();

        this.column().vertical().stretch();
        boolean categoriesEnabled = BBSSettings.modelBlockCategoriesPanelEnabled != null && BBSSettings.modelBlockCategoriesPanelEnabled.get();
        if (categoriesEnabled)
        {
            this.add(UI.row(this.groups, this.categories), UI.label(UIKeys.POSE_CONTEXT_FIX), this.fix, this.pickTexture, UI.row(this.color, this.lighting), this.transform);
        }
        else
        {
            this.add(this.groups, UI.label(UIKeys.POSE_CONTEXT_FIX), this.fix, this.pickTexture, UI.row(this.color, this.lighting), this.transform);
        }
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
        this.refreshCategories();
    }

    /* Accesor público del grupo de pose (para fábricas y pistas) */
    public String getPoseGroupKey()
    {
        return this.group;
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
        this.refreshCategories();
    }

    public void selectBone(String bone)
    {
        lastLimb = bone;

        this.groups.setCurrentScroll(bone);
        this.pickBone(bone);

        /* Si el hueso pertenece a alguna categoría del grupo actual, seleccionarla automáticamente */
        if (this.categories != null && this.model != null)
        {
            List<String> cats = this.boneCategories.getCategories(this.group);
            for (String cat : cats)
            {
                List<String> bones = this.boneCategories.getBones(this.group, cat);
                if (bones.contains(bone))
                {
                    this.categories.setCurrentScroll(cat);
                    break;
                }
            }
        }
    }

    /* Subclass overridable methods */

    protected UIPropTransform createTransformEditor()
    {
        return new CategoryPropTransform(this).enableHotkeys();
    }

    /* Transformaciones aplicables por categoría */
    private static class CategoryPropTransform extends UIPropTransform
    {
        private final UIPoseEditor editor;

        private CategoryPropTransform(UIPoseEditor editor)
        {
            this.editor = editor;
        }

        private List<String> targets()
        {
            boolean categoriesEnabled = BBSSettings.modelBlockCategoriesPanelEnabled != null && BBSSettings.modelBlockCategoriesPanelEnabled.get();
            String selectedCategory = (categoriesEnabled && this.editor.categories != null) ? this.editor.categories.getCurrentFirst() : null;
            if (selectedCategory == null || selectedCategory.isEmpty())
            {
                String current = this.editor.groups.getCurrentFirst();
                return current == null ? java.util.Collections.emptyList() : java.util.Collections.singletonList(current);
            }

            return this.editor.boneCategories.getBones(this.editor.group, selectedCategory);
        }

        @Override
        public void setT(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = (float) (x - transform.translate.x);
            float dy = (float) (y - transform.translate.y);
            float dz = (float) (z - transform.translate.z);

            for (String key : this.targets())
            {
                PoseTransform t = this.editor.pose.get(key);
                if (t != null)
                {
                    t.translate.x += dx;
                    t.translate.y += dy;
                    t.translate.z += dz;
                }
            }
        }

        @Override
        public void setS(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = (float) (x - transform.scale.x);
            float dy = (float) (y - transform.scale.y);
            float dz = (float) (z - transform.scale.z);

            for (String key : this.targets())
            {
                PoseTransform t = this.editor.pose.get(key);
                if (t != null)
                {
                    t.scale.x += dx;
                    t.scale.y += dy;
                    t.scale.z += dz;
                }
            }
        }

        @Override
        public void setR(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate.z;

            for (String key : this.targets())
            {
                PoseTransform t = this.editor.pose.get(key);
                if (t != null)
                {
                    t.rotate.x += dx;
                    t.rotate.y += dy;
                    t.rotate.z += dz;
                }
            }
        }

        @Override
        public void setR2(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate2.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate2.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate2.z;

            for (String key : this.targets())
            {
                PoseTransform t = this.editor.pose.get(key);
                if (t != null)
                {
                    t.rotate2.x += dx;
                    t.rotate2.y += dy;
                    t.rotate2.z += dz;
                }
            }
        }

        @Override
        public void setP(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = (float) x - transform.pivot.x;
            float dy = (float) y - transform.pivot.y;
            float dz = (float) z - transform.pivot.z;

            for (String key : this.targets())
            {
                PoseTransform t = this.editor.pose.get(key);
                if (t != null)
                {
                    t.pivot.x += dx;
                    t.pivot.y += dy;
                    t.pivot.z += dz;
                }
            }
        }
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

    /* Categorías */

    protected void refreshCategories()
    {
        if (this.categories == null)
        {
            return;
        }

        this.categories.clear();
        if (this.group != null)
        {
            this.categories.add(this.boneCategories.getCategories(this.group));
            this.categories.sort();
        }
    }

    protected void applyCategory(java.util.function.Consumer<PoseTransform> consumer)
    {
        boolean categoriesEnabled = BBSSettings.modelBlockCategoriesPanelEnabled != null && BBSSettings.modelBlockCategoriesPanelEnabled.get();
        String selectedCategory = categoriesEnabled ? this.categories.getCurrentFirst() : null;
        if (this.model == null || selectedCategory == null || selectedCategory.isEmpty())
        {
            return;
        }

        List<String> bones = this.boneCategories.getBones(this.group, selectedCategory);
        for (String key : bones)
        {
            PoseTransform t = this.pose.get(key);
            if (t != null)
            {
                consumer.accept(t);
            }
        }
    }
}