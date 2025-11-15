package mchorse.bbs_mod.ui.forms.editors.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.gizmos.BoneGizmoSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public class UIPickableFormRenderer extends UIFormRenderer
{
    public UIFormEditor formEditor;

    private boolean update;

    private StencilFormFramebuffer stencil = new StencilFormFramebuffer();
    private StencilMap stencilMap = new StencilMap();

    private IEntity target;
    private Supplier<Boolean> renderForm;

    public UIPickableFormRenderer(UIFormEditor formEditor)
    {
        this.formEditor = formEditor;
    }

    public void updatable()
    {
        this.update = true;
    }

    public StencilFormFramebuffer getStencil()
    {
        return this.stencil;
    }

    public void setRenderForm(Supplier<Boolean> renderForm)
    {
        this.renderForm = renderForm;
    }

    public IEntity getTargetEntity()
    {
        return this.target == null ? this.entity : this.target;
    }

    public void setTarget(IEntity target)
    {
        this.target = target;
    }

    private void ensureFramebuffer()
    {
        this.stencil.setup(Link.bbs("stencil_form"));
        this.stencil.resizeGUI(this.area.w, this.area.h);
    }

    @Override
    public void resize()
    {
        super.resize();

        this.ensureFramebuffer();
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        /* Dar prioridad al gizmo: si el cursor está sobre un handle del gizmo,
         * consumimos el clic para evitar seleccionar otro hueso o arrastrar la cámara. */
        if (
            BBSSettings.modelBlockGizmosEnabled.get() &&
            this.area.isInside(context) &&
            BoneGizmoSystem.get().isHoveringHandle()
        )
        {
            /* Consumir clic izquierdo, derecho y botón medio */
            if (context.mouseButton == 0 || context.mouseButton == 1 || context.mouseButton == 2)
            {
                return true;
            }
        }

        /* Si no está sobre el gizmo, permitimos el pick de huesos en el viewport */
        if (this.formEditor.clickViewport(context, this.stencil))
        {
            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected void renderUserModel(UIContext context)
    {
        if (this.form == null)
        {
            return;
        }

        this.formEditor.preFormRender(context, this.form);

        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, this.target == null ? this.entity : this.target, context.batcher.getContext().getMatrices(), LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, context.getTransition())
            .camera(this.camera)
            .modelRenderer();

        if (this.renderForm == null || this.renderForm.get())
        {
            FormUtilsClient.render(this.form, formContext);

            if (this.form.hitbox.get())
            {
                this.renderFormHitbox(context);
            }
        }

        this.renderAxes(context);

        /* Gizmo en el editor de modelos: posicionar en el pivote del hueso seleccionado */
        Matrix4f originRaw = this.formEditor.getOrigin(context.getTransition());
        Matrix4f origin = originRaw != null ? MatrixStackUtils.stripScale(originRaw) : null;
        UIPropTransform activeTransform = null;

        /* Priorizar el transform del editor de estados cuando está visible y la pista es de pose/pose_overlay */
        if (this.formEditor.statesEditor.isVisible()
            && this.formEditor.statesKeyframes != null
            && this.formEditor.statesKeyframes.keyframeEditor != null
            && this.formEditor.statesKeyframes.keyframeEditor.editor instanceof UIPoseKeyframeFactory poseFactory)
        {
            activeTransform = poseFactory.poseEditor.transform;
        }
        else if (this.formEditor.editor instanceof UIModelForm uiModelForm)
        {
            /* Fallback al transform del editor de modelo cuando no está el de estados */
            activeTransform = uiModelForm.modelPanel.poseEditor.transform;
        }

        /* La vista usada para renderizar el modelo aplica rotación y luego traslada
         * por la posición de la cámara en el MatrixStack. Para alinear el gizmo con
         * lo que se ve en pantalla, debemos incorporar también esa traslación en la
         * matriz de vista que pasamos al gizmo. */
        Matrix4f viewWithTranslation = new Matrix4f(this.camera.view)
            .translate(-(float) this.camera.position.x, -(float) this.camera.position.y, -(float) this.camera.position.z);

        if (BBSSettings.modelBlockGizmosEnabled.get())
        {
            BoneGizmoSystem.get().update(context, this.area, origin, this.camera.projection, viewWithTranslation, activeTransform);
        }

        if (this.area.isInside(context))
        {
            GlStateManager._disableScissorTest();

            this.stencilMap.setup();
            this.stencil.apply();
            FormUtilsClient.render(this.form, formContext.stencilMap(this.stencilMap));

            /* Evitar el picking de huesos cuando el mouse está sobre un gizmo.
             * Esto permite manipular el gizmo sin seleccionar otro hueso por accidente. */
            boolean blockPicking = BBSSettings.modelBlockGizmosEnabled.get() && BoneGizmoSystem.get().isHoveringHandle();

            if (!blockPicking)
            {
                this.stencil.pickGUI(context, this.area);
            }
            else
            {
                this.stencil.clearPicking();
            }
            this.stencil.unbind(this.stencilMap);

            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);

            GlStateManager._enableScissorTest();
        }
        else
        {
            this.stencil.clearPicking();
        }
    }

    private void renderAxes(UIContext context)
    {
        Matrix4f matrixRaw = this.formEditor.getOrigin(context.getTransition());
        Matrix4f matrix = matrixRaw != null ? MatrixStackUtils.stripScale(matrixRaw) : null;
        MatrixStack stack = context.render.batcher.getContext().getMatrices();

        stack.push();

        if (matrix != null)
        {
            MatrixStackUtils.multiply(stack, matrix);
        }

        /* Draw axes (desactivar cuando el gizmo nuevo está activo) */
        if (UIBaseMenu.renderAxes && !BBSSettings.modelBlockGizmosEnabled.get())
        {
            RenderSystem.disableDepthTest();
            Draw.coolerAxes(stack, 0.25F, 0.01F, 0.26F, 0.02F);
            RenderSystem.enableDepthTest();
        }

        /* Render gizmos 3D en el origen del modelo cuando están habilitados */
        if (BBSSettings.modelBlockGizmosEnabled.get())
        {
            BoneGizmoSystem.get().render3D(stack);
        }

        stack.pop();
    }

    private void renderFormHitbox(UIContext context)
    {
        float hitboxW = this.form.hitboxWidth.get();
        float hitboxH = this.form.hitboxHeight.get();
        float eyeHeight = hitboxH * this.form.hitboxEyeHeight.get();

        /* Draw look vector */
        final float thickness = 0.01F;
        Draw.renderBox(context.batcher.getContext().getMatrices(), -thickness, -thickness + eyeHeight, -thickness, thickness, thickness, 2F, 1F, 0F, 0F);

        /* Draw hitbox */
        Draw.renderBox(context.batcher.getContext().getMatrices(), -hitboxW / 2, 0, -hitboxW / 2, hitboxW, hitboxH, hitboxW);
    }

    @Override
    protected void update()
    {
        super.update();

        if (this.update && this.target != null)
        {
            this.form.update(this.entity);
        }
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        /* Render overlay del gizmo al final para que siempre quede por encima
         * de cualquier overlay de previsualización del picker. */
        if (BBSSettings.modelBlockGizmosEnabled.get())
        {
            BoneGizmoSystem.get().renderOverlay(context.render, this.area);
        }

        if (!this.stencil.hasPicked())
        {
            return;
        }

        int index = this.stencil.getIndex();
        Texture texture = this.stencil.getFramebuffer().getMainTexture();
        Pair<Form, String> pair = this.stencil.getPicked();
        int w = texture.width;
        int h = texture.height;

        ShaderProgram previewProgram = BBSShaders.getPickerPreviewProgram();
        GlUniform target = previewProgram.getUniform("Target");

        if (target != null)
        {
            target.set(index);
        }

        RenderSystem.enableBlend();
        context.batcher.texturedBox(BBSShaders::getPickerPreviewProgram, texture.id, Colors.WHITE, this.area.x, this.area.y, this.area.w, this.area.h, 0, h, w, 0, w, h);

        if (pair != null)
        {
            String label = pair.a.getFormIdOrName();

            if (!pair.b.isEmpty())
            {
                label += " - " + pair.b;
            }

            context.batcher.textCard(label, context.mouseX + 12, context.mouseY + 8);
        }
    }

    @Override
    protected void renderGrid(UIContext context)
    {
        if (this.renderForm == null || this.renderForm.get())
        {
            super.renderGrid(context);
        }
    }
}