package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.resources.Link;
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

            /* Hacer que el selector de textura del hueso abra la carpeta
             * de la textura base del modelo cuando no haya override */
            this.poseEditor.setDefaultTextureSupplier(() ->
            {
                Link base = modelForm.texture.get();
                if (base != null)
                {
                    return base;
                }

                ModelInstance m = ((ModelFormRenderer) FormUtilsClient.getRenderer(modelForm)).getModel();
                return m != null ? m.texture : null;
            });

            if (model != null)
            {
                this.poseEditor.setPose(keyframe.getValue(), model.poseGroup);
                this.poseEditor.fillGroups(model.model, model.flippedParts, false);

                /* Si la pista está anclada, seleccionar el hueso anclado al crear el editor */
                if (BBSSettings.boneAnchoringEnabled.get() && sheet != null && sheet.anchoredBone != null)
                {
                    this.poseEditor.selectBone(sheet.anchoredBone);
                }
            }
        }
        else if (FormUtils.getForm(sheet.property) instanceof MobForm mobForm)
        {
            List<String> bones = FormUtilsClient.getRenderer(mobForm).getBones();

            this.poseEditor.setPose(keyframe.getValue(), "");
            this.poseEditor.fillGroups(bones, false);

            if (BBSSettings.boneAnchoringEnabled.get() && sheet != null && sheet.anchoredBone != null)
            {
                this.poseEditor.selectBone(sheet.anchoredBone);
            }
        }

        this.scroll.add(this.poseEditor);
    }

    @Override
    public void resize()
    {
        this.poseEditor.removeAll();

        /* Construir disposición según estado de anclaje y ajuste global */
        UIKeyframeSheet sheet = this.editor.getGraph().getSheet(this.keyframe);
        boolean anchoringEnabled = BBSSettings.boneAnchoringEnabled.get();
        boolean isAnchored = anchoringEnabled && sheet != null && sheet.anchoredBone != null;

        if (isAnchored)
        {
            this.poseEditor.anchoredLegend.setVisible(true);
            this.poseEditor.anchoredLegend.label = mchorse.bbs_mod.l10n.keys.IKey.constant("Hueso anclado: " + sheet.anchoredBone);
            this.poseEditor.selectBone(sheet.anchoredBone);
        }
        else
        {
            this.poseEditor.anchoredLegend.setVisible(false);
            /* Si el anclaje está deshabilitado globalmente, ocultar botones de anclaje */
            if (!anchoringEnabled)
            {
                this.poseEditor.anchorBone.setVisible(false);
                this.poseEditor.unanchorBone.setVisible(false);
            }
            else
            {
                this.poseEditor.anchorBone.setVisible(true);
                this.poseEditor.unanchorBone.setVisible(true);
            }
        }

        if (this.getFlex().getW() > 240)
        {
            UIElement left = UI.column(UI.label(UIKeys.POSE_CONTEXT_FIX), this.poseEditor.fix, UI.row(this.poseEditor.color, this.poseEditor.lighting), this.poseEditor.transform);

            UIElement right;
            if (isAnchored)
            {
                /* En anclado, mantener visible el botón de textura usando el hueso anclado */
                this.poseEditor.pickTexture.w(1F);
                right = UI.column(UI.label(UIKeys.FORMS_EDITOR_BONE), this.poseEditor.anchoredLegend, this.poseEditor.pickTexture, this.poseEditor.unanchorBone);
            }
            else
            {
                /* Insertar botón de textura de hueso entre la lista y el anclaje */
                this.poseEditor.pickTexture.w(1F);
                if (anchoringEnabled)
                {
                    right = UI.column(UI.label(UIKeys.FORMS_EDITOR_BONE), this.poseEditor.groups, this.poseEditor.pickTexture, this.poseEditor.anchorBone);
                }
                else
                {
                    /* Anclaje deshabilitado: no mostrar botón de anclar */
                    right = UI.column(UI.label(UIKeys.FORMS_EDITOR_BONE), this.poseEditor.groups, this.poseEditor.pickTexture);
                }
            }

            this.poseEditor.add(UI.row(left, right));
        }
        else
        {
            if (isAnchored)
            {
                /* En estrecho y anclado, conservar el botón de textura */
                this.poseEditor.add(UI.label(UIKeys.FORMS_EDITOR_BONE), this.poseEditor.anchoredLegend, this.poseEditor.pickTexture, this.poseEditor.unanchorBone,
                    UI.label(UIKeys.POSE_CONTEXT_FIX), this.poseEditor.fix, UI.row(this.poseEditor.color, this.poseEditor.lighting), this.poseEditor.transform);
            }
            else
            {
                /* En modo estrecho, también colocar el botón antes del anclaje */
                if (anchoringEnabled)
                {
                    this.poseEditor.add(UI.label(UIKeys.FORMS_EDITOR_BONE), this.poseEditor.groups, this.poseEditor.pickTexture, this.poseEditor.anchorBone,
                        UI.label(UIKeys.POSE_CONTEXT_FIX), this.poseEditor.fix, UI.row(this.poseEditor.color, this.poseEditor.lighting), this.poseEditor.transform);
                }
                else
                {
                    /* Anclaje deshabilitado: ocultar botón de anclar */
                    this.poseEditor.add(UI.label(UIKeys.FORMS_EDITOR_BONE), this.poseEditor.groups, this.poseEditor.pickTexture,
                        UI.label(UIKeys.POSE_CONTEXT_FIX), this.poseEditor.fix, UI.row(this.poseEditor.color, this.poseEditor.lighting), this.poseEditor.transform);
                }
            }
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
        public UIButton anchorBone;
        public UIButton unanchorBone;
        public mchorse.bbs_mod.ui.framework.elements.utils.UILabel anchoredLegend;

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

            /* Leyenda para indicar hueso anclado */
            this.anchoredLegend = UI.label(mchorse.bbs_mod.l10n.keys.IKey.constant("Hueso anclado: -"));
            this.anchoredLegend.h(20);
            this.anchoredLegend.setVisible(false);

            /* Bone anchoring buttons (optional) */
            this.anchorBone = new UIButton(UIKeys.POSE_TRACKS_ANCHOR_SELECT_BONE, (b) ->
            {
                if (!BBSSettings.boneAnchoringEnabled.get()) return;

                UIKeyframeSheet sheet = this.editor.getGraph().getSheet(this.keyframe);
                if (sheet == null) return;

                /* Overlay para elegir el hueso */
                java.util.List<String> bones = this.groups.getList();
                UISearchList<String> search = new UISearchList<>(new UIStringList(null));
                UIList<String> list = search.list;
                UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(
                    UIKeys.POSE_TRACKS_ANCHOR_SELECT_BONE_TITLE,
                    UIKeys.POSE_TRACKS_ANCHOR_SELECT_BONE_DESCRIPTION,
                    (confirm) ->
                    {
                        if (confirm)
                        {
                            int index = list.getIndex();
                            String bone = mchorse.bbs_mod.utils.CollectionUtils.getSafe(bones, index);

                            if (bone != null)
                            {
                                sheet.anchoredBone = bone;
                                /* Refrescar estado inmediato */
                                this.selectBone(bone);
                                this.anchoredLegend.setVisible(true);
                                this.anchoredLegend.label = mchorse.bbs_mod.l10n.keys.IKey.constant("Hueso anclado: " + bone);

                                /* Reacomodar el panel para evitar huecos */
                                UIPoseKeyframeFactory factory = this.getParent(UIPoseKeyframeFactory.class);
                                if (factory != null) { factory.resize(); }

                                /* Renombrar automáticamente la pista usando títulos personalizados del Replay */
                                mchorse.bbs_mod.ui.film.UIFilmPanel filmPanel = this.getParent(mchorse.bbs_mod.ui.film.UIFilmPanel.class);
                                if (filmPanel != null && filmPanel.replayEditor != null && filmPanel.replayEditor.getReplay() != null)
                                {
                                    filmPanel.replayEditor.getReplay().setCustomSheetTitle(sheet.id, bone);
                                    /* Evitar refresco pesado que cierra el editor; el título se reflejará en render */
                                }
                            }
                        }
                    }
                );

                for (String g : bones) { list.add(g); }

                /* Preseleccionar */
                String current = sheet.anchoredBone != null ? sheet.anchoredBone : this.groups.getCurrentFirst();
                int idx = bones.indexOf(current);
                list.setIndex(Math.max(idx, 0));

                list.background();
                search.relative(panel.confirm).y(-5).w(1F).h(16 * 9 + 20).anchor(0F, 1F);
                panel.confirm.w(1F, -10);
                panel.content.add(search);
                UIOverlay.addOverlay(this.getContext(), panel, 240, 300);
            });

            this.unanchorBone = new UIButton(UIKeys.POSE_TRACKS_ANCHOR_UNANCHOR, (b) ->
            {
                UIKeyframeSheet sheet = this.editor.getGraph().getSheet(this.keyframe);
                if (sheet != null)
                {
                    sheet.anchoredBone = null;
                    this.anchoredLegend.setVisible(false);

                    /* Quitar el título personalizado al desanclar y refrescar lista */
                    mchorse.bbs_mod.ui.film.UIFilmPanel filmPanel = this.getParent(mchorse.bbs_mod.ui.film.UIFilmPanel.class);
                    if (filmPanel != null && filmPanel.replayEditor != null && filmPanel.replayEditor.getReplay() != null)
                    {
                        filmPanel.replayEditor.getReplay().setCustomSheetTitle(sheet.id, null);
                        /* Sin refresco inmediato para no cerrar el panel actual */
                    }

                    /* Reacomodar el panel para que la lista regrese a su sitio */
                    UIPoseKeyframeFactory factory = this.getParent(UIPoseKeyframeFactory.class);
                    if (factory != null) { factory.resize(); }
                }
            });

            /* Asegurar que los botones ocupen el ancho completo cuando estén visibles */
            this.anchorBone.w(1F);
            this.unanchorBone.w(1F);
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
                poseT.pivot.set(0F, 0F, 0F);
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
        public void pastePivot(Vector3d pivot)
        {
            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) -> poseT.pivot.set((float) pivot.x, (float) pivot.y, (float) pivot.z));
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

        @Override
        public void setP(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getTransform();
            float dx = (float) x - transform.pivot.x;
            float dy = (float) y - transform.pivot.y;
            float dz = (float) z - transform.pivot.z;

            UIPoseFactoryEditor.apply(this.editor.editor, this.editor.keyframe, this.editor.getGroup(), (poseT) ->
            {
                poseT.pivot.x += dx;
                poseT.pivot.y += dy;
                poseT.pivot.z += dz;
            });
        }
    }
}