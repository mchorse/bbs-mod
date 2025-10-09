package mchorse.bbs_mod.ui.forms.editors.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.forms.UIModelForm;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
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
    
    // Hover state tracking
    private Axis hoveredRing = null;
    private Axis hoveredArrow = null;
    private Axis hoveredCube = null;

    public UIPickableFormRenderer(UIFormEditor formEditor)
    {
        this.formEditor = formEditor;
    }

    public void updatable()
    {
        this.update = true;
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
        /* Try picking gizmo axes first */
        if (context.mouseButton == 0 && UIBaseMenu.renderAxes && this.tryPickGizmoAxis(context))
        {
            return true;
        }

        if (this.stencil.hasPicked() && context.mouseButton == 0)
        {
            Pair<Form, String> pair = this.stencil.getPicked();

            if (pair != null)
            {
                this.formEditor.pickFormFromRenderer(pair);

                return true;
            }
        }

        return super.subMouseClicked(context);
    }

    private boolean tryPickGizmoAxis(UIContext context)
    {
        if (!(this.formEditor.editor instanceof UIModelForm))
        {
            return false;
        }

        // Use stencil-based picking for pixel-perfect accuracy
        return this.tryPickGizmoWithStencil(context);
    }

    private boolean tryPickGizmoWithStencil(UIContext context)
    {
        if (!(this.formEditor.editor instanceof UIModelForm uiModelForm))
        {
            return false;
        }

        Matrix4f origin = this.formEditor.editor.getOrigin(context.getTransition());
        if (origin == null)
        {
            return false;
        }

        // Set up stencil rendering for gizmo picking
        this.ensureFramebuffer();
        
        // Render gizmo elements with unique IDs to stencil buffer
        this.stencilMap.setup();
        this.stencil.apply();
        
        MatrixStack stack = context.render.batcher.getContext().getMatrices();
        stack.push();
        
        if (origin != null)
        {
            MatrixStackUtils.multiply(stack, origin);
        }

        float scale = BBSSettings.axesScale.get();
        
        // Render gizmo elements with unique stencil IDs
        this.renderGizmoForPicking(stack, scale);
        
        stack.pop();
        
        // Pick at mouse position
        this.stencil.pickGUI(context, this.area);
        this.stencil.unbind(this.stencilMap);
        
        // Check what was picked
        if (this.stencil.hasPicked())
        {
            int pickedId = this.stencil.getIndex();
            return this.handleGizmoPick(pickedId, uiModelForm);
        }
        
            return false;
        }

    private void renderGizmoForPicking(MatrixStack stack, float scale)
    {
        // Define unique IDs for each gizmo element
        final int RING_X_ID = 1001; // Red ring (X rotation)
        final int RING_Y_ID = 1002; // Green ring (Y rotation)  
        final int RING_Z_ID = 1003; // Blue ring (Z rotation)
        final int ARROW_X_ID = 2001; // X arrow (translation)
        final int ARROW_Y_ID = 2002; // Y arrow (translation)
        final int ARROW_Z_ID = 2003; // Z arrow (translation)

        // Set up picking shader
        ShaderProgram pickingProgram = BBSShaders.getPickerModelsProgram();
        RenderSystem.setShader(() -> pickingProgram);
        
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION);

        // Render rings with unique IDs
        this.stencilMap.objectIndex = RING_X_ID;
        this.renderRingForPicking(builder, stack, 0.35F * scale, 0.015F * scale, 64, 0, 90, 0); // YZ plane (X rotation)
        
        this.stencilMap.objectIndex = RING_Y_ID;
        this.renderRingForPicking(builder, stack, 0.35F * scale, 0.015F * scale, 64, 90, 0, 0); // XZ plane (Y rotation)
        
        this.stencilMap.objectIndex = RING_Z_ID;
        this.renderRingForPicking(builder, stack, 0.35F * scale, 0.015F * scale, 64, 0, 0, 0); // XY plane (Z rotation)

        // Render arrows with unique IDs - increased size for easier selection
        this.stencilMap.objectIndex = ARROW_X_ID;
        this.renderArrowForPicking(builder, stack, 0.8F * scale, 0.016F * scale, 0.16F * scale, 0.06F * scale, 0, 0, 0); // X axis
        
        this.stencilMap.objectIndex = ARROW_Y_ID;
        stack.push();
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
        this.renderArrowForPicking(builder, stack, 0.8F * scale, 0.016F * scale, 0.16F * scale, 0.06F * scale, 0, 0, 0); // Y axis
        stack.pop();
        
        this.stencilMap.objectIndex = ARROW_Z_ID;
        stack.push();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90F));
        this.renderArrowForPicking(builder, stack, 0.8F * scale, 0.016F * scale, 0.16F * scale, 0.06F * scale, 0, 0, 0); // Z axis
        stack.pop();

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private void renderRingForPicking(BufferBuilder builder, MatrixStack stack, float radius, float thickness, int segments, float rotX, float rotY, float rotZ)
    {
        Matrix4f m = stack.peek().getPositionMatrix();
        
        // Apply rotations
        if (rotX != 0) stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
        if (rotY != 0) stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        if (rotZ != 0) stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
        
        m = stack.peek().getPositionMatrix();
        
        float halfThickness = thickness / 2F;
        
        for (int i = 0; i < segments; i++)
        {
            float t1 = (float) (2 * Math.PI * i / segments);
            float t2 = (float) (2 * Math.PI * (i + 1) / segments);
            
            float x1 = (float) Math.cos(t1) * radius;
            float y1 = (float) Math.sin(t1) * radius;
            float x2 = (float) Math.cos(t2) * radius;
            float y2 = (float) Math.sin(t2) * radius;
            
            // Create a thick ring by rendering quads
            builder.vertex(m, x1 - halfThickness, y1, 0).next();
            builder.vertex(m, x1 + halfThickness, y1, 0).next();
            builder.vertex(m, x2 + halfThickness, y2, 0).next();
            builder.vertex(m, x2 - halfThickness, y2, 0).next();
        }
    }

    private void renderArrowForPicking(BufferBuilder builder, MatrixStack stack, float length, float thickness, float arrowLength, float arrowWidth, float rotX, float rotY, float rotZ)
    {
        Matrix4f m = stack.peek().getPositionMatrix();
        
        // Apply rotations
        if (rotX != 0) stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
        if (rotY != 0) stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        if (rotZ != 0) stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
        
        m = stack.peek().getPositionMatrix();
        
        float halfThickness = thickness / 2F;
        float shaftEnd = length - arrowLength;
        
        // Render arrow shaft
        builder.vertex(m, -length * 0.3F, -halfThickness, -halfThickness).next();
        builder.vertex(m, shaftEnd, -halfThickness, -halfThickness).next();
        builder.vertex(m, shaftEnd, halfThickness, -halfThickness).next();
        builder.vertex(m, -length * 0.3F, halfThickness, -halfThickness).next();
        
        // Render arrow head (simplified as a triangle)
        builder.vertex(m, shaftEnd, -arrowWidth, -arrowWidth).next();
        builder.vertex(m, length, 0, 0).next();
        builder.vertex(m, shaftEnd, arrowWidth, -arrowWidth).next();
    }

    private boolean handleGizmoPick(int pickedId, UIModelForm uiModelForm)
    {
        UIPropTransform transform = uiModelForm.modelPanel.poseEditor.transform;
        
        // Handle ring picks (rotation)
        if (pickedId >= 1001 && pickedId <= 1003)
        {
            Axis axis = pickedId == 1001 ? Axis.X : (pickedId == 1002 ? Axis.Y : Axis.Z);
            transform.beginRotate();
            transform.setAxis(axis);
            return true;
        }
        
        // Handle arrow picks (translation)
        if (pickedId >= 2001 && pickedId <= 2003)
        {
            Axis axis = pickedId == 2001 ? Axis.X : (pickedId == 2002 ? Axis.Y : Axis.Z);
            transform.beginTranslate();
            transform.setAxis(axis);
            return true;
        }
        
        return false;
    }



    @Override
    protected void renderUserModel(UIContext context)
    {
        if (this.form == null)
        {
            return;
        }

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

        if (this.area.isInside(context))
        {
            GlStateManager._disableScissorTest();

            this.stencilMap.setup();
            this.stencil.apply();
            FormUtilsClient.render(this.form, formContext.stencilMap(this.stencilMap));

            this.stencil.pickGUI(context, this.area);
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
        Matrix4f matrix = this.formEditor.editor.getOrigin(context.getTransition());
        MatrixStack stack = context.render.batcher.getContext().getMatrices();

        stack.push();

        if (matrix != null)
        {
            MatrixStackUtils.multiply(stack, matrix);
        }

        /* Proper 3D transformation gizmo with axis arrows, rotation rings, and central origin */
        if (UIBaseMenu.renderAxes)
        {
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();

            // Apply the same scale as the X/Y/Z axes
            float scale = BBSSettings.axesScale.get();

            // Check for hover state on the gizmo
            Matrix4f mvp = new Matrix4f(this.camera.projection).mul(this.camera.view);
            Matrix4f origin = this.formEditor.editor.getOrigin(context.getTransition());
            if (origin != null) mvp.mul(origin);

            // Render the complete transformation gizmo
            Draw.renderTransformationGizmo(stack, scale, 1F, 1F, 1F, 0.95F);

            // Enhanced hover feedback - highlight hovered elements
            if (this.hoveredRing != null || this.hoveredArrow != null || this.hoveredCube != null)
            {
                // Highlight the origin when any element is hovered
                Draw.renderSphere(stack, 0.06F * scale, 12, 16, 1F, 1F, 0F, 0.8F);
                
                // Add additional hover effects
                if (this.hoveredRing != null)
                {
                    // Highlight the hovered ring with a brighter version
                    float r = this.hoveredRing == Axis.X ? 1F : 0F;
                    float g = this.hoveredRing == Axis.Y ? 1F : 0F;
                    float b = this.hoveredRing == Axis.Z ? 1F : 0F;
                    Draw.renderRing(stack, 0.35F * scale, 0.025F * scale, 64, r, g, b, 1F);
                }
            }

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
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
            String label = pair.a.getIdOrName();

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