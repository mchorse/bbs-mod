package mchorse.bbs_mod.ui.forms.editors.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

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
        if (!(this.formEditor.editor instanceof UIModelForm uiModelForm))
        {
            System.out.println("[Gizmo] Not in UIModelForm, skipping gizmo pick");
            return false;
        }

        Matrix4f origin = this.formEditor.editor.getOrigin(context.getTransition());
        if (origin == null)
        {
            System.out.println("[Gizmo] Origin matrix is null, skipping gizmo pick");
            return false;
        }

        /* Build MVP = projection * view * origin */
        Matrix4f mvp = new Matrix4f(this.camera.projection).mul(this.camera.view).mul(origin);

        // Ring picking in screen space (reliable from any angle)
        float pickTolBase = 20F; // pixels

        Vector2f p0 = projectToScreen(mvp, 0, 0, 0);
        if (p0 == null)
        {
            return false;
        }

        Vector2f mouse = new Vector2f(context.mouseX, context.mouseY);

        // Determine on-screen radius to scale tolerance with size
        float R = 0.35F; // keep in sync with render
        float tube = 0.06F; // torus tube radius used in render
        Vector2f pR = projectToScreen(mvp, R, 0, 0);
        Vector2f pRt = projectToScreen(mvp, R + tube * 0.9F, 0, 0);
        float pickTol = pickTolBase;
        if (p0 != null && pR != null)
        {
            float rpx = p0.distance(pR);
            float tpx = pRt != null ? Math.abs(pR.distance(pRt)) : 0F;
            pickTol = Math.max(pickTolBase, Math.max(rpx * 0.14F, tpx * 0.9F));
        }

        // Sample three rings (XY -> Z, XZ -> Y, YZ -> X)
        Axis ringHit = null;
        int samples = 128;
        int period = 8; // must match render dashes
        float duty = 0.55F;
        int onCount = Math.max(1, Math.round(period * duty));

        // Camera-relative weighting: prefer ring whose plane is most perpendicular to view (most visible)
        Matrix4f viewOrigin = new Matrix4f(this.camera.view).mul(origin);
        org.joml.Matrix3f normalMat = new org.joml.Matrix3f();
        viewOrigin.normal(normalMat);
        org.joml.Vector3f nx = new org.joml.Vector3f(1, 0, 0).mul(normalMat).normalize();
        org.joml.Vector3f ny = new org.joml.Vector3f(0, 1, 0).mul(normalMat).normalize();
        org.joml.Vector3f nz = new org.joml.Vector3f(0, 0, 1).mul(normalMat).normalize();
        float wX = Math.abs(nx.z), wY = Math.abs(ny.z), wZ = Math.abs(nz.z);
        float bias = 8F; // px advantage for fully face-on ring (gentler)

        // Helper to test one ring defined by function producing (x,y,z)
        java.util.function.Function<Float, Vector2f> projectPoint = (t) ->
        {
            Vector4f v = new Vector4f((float) Math.cos(t) * R, (float) Math.sin(t) * R, 0, 1);
            Vector4f vv = new Vector4f(v);
            mvp.transform(vv);
            if (Math.abs(vv.w) < 1e-5f) return null;
            float nxp = vv.x / vv.w;
            float nyp = vv.y / vv.w;
            float sx = this.area.x + (nxp * 0.5F + 0.5F) * this.area.w;
            float sy = this.area.y + ((-nyp) * 0.5F + 0.5F) * this.area.h;
            return new Vector2f(sx, sy);
        };

        // For XZ ring (rotate around Y): rotate base ring by +90 around X
        java.util.function.Function<Float, Vector2f> projectPointXZ = (t) ->
        {
            Vector4f v = new Vector4f((float) Math.cos(t) * R, 0, (float) Math.sin(t) * R, 1);
            Vector4f vv = new Vector4f(v);
            mvp.transform(vv);
            if (Math.abs(vv.w) < 1e-5f) return null;
            float nxp = vv.x / vv.w;
            float nyp = vv.y / vv.w;
            float sx = this.area.x + (nxp * 0.5F + 0.5F) * this.area.w;
            float sy = this.area.y + ((-nyp) * 0.5F + 0.5F) * this.area.h;
            return new Vector2f(sx, sy);
        };

        // For YZ ring (rotate around X): rotate base ring by +90 around Y
        java.util.function.Function<Float, Vector2f> projectPointYZ = (t) ->
        {
            Vector4f v = new Vector4f(0, (float) Math.cos(t) * R, (float) Math.sin(t) * R, 1);
            Vector4f vv = new Vector4f(v);
            mvp.transform(vv);
            if (Math.abs(vv.w) < 1e-5f) return null;
            float nxp = vv.x / vv.w;
            float nyp = vv.y / vv.w;
            float sx = this.area.x + (nxp * 0.5F + 0.5F) * this.area.w;
            float sy = this.area.y + ((-nyp) * 0.5F + 0.5F) * this.area.h;
            return new Vector2f(sx, sy);
        };

        // Test a ring by polyline distance
        final float[] bestRef = new float[] { Float.MAX_VALUE };
        java.util.function.BiFunction<java.util.function.Function<Float, Vector2f>, Axis, Float> testRing = (proj, axis) ->
        {
            float localBest = Float.MAX_VALUE;
            Vector2f prev = null;
            for (int i = 0; i <= samples; i++)
            {
                float t = (float) (2 * Math.PI * i / samples);
                int seg = (int) Math.floor((float) i / samples * period) % period;
                if (seg >= onCount) { prev = null; continue; } // respect gaps
                Vector2f cur = proj.apply(t);
                if (cur == null)
                {
                    prev = null;
                    continue;
                }
                if (prev != null)
                {
                    float d = distanceToSegment(mouse, prev, cur);
                    if (d < localBest) localBest = d;
                }
                prev = cur;
            }
            if (localBest < bestRef[0])
            {
                bestRef[0] = localBest;
                return localBest;
            }
            return localBest;
        };

        float dZ = testRing.apply(projectPoint, Axis.Z);
        float dY = testRing.apply(projectPointXZ, Axis.Y);
        float dX = testRing.apply(projectPointYZ, Axis.X);

        float scoreZ = dZ - bias * wZ;
        float scoreY = dY - bias * wY;
        float scoreX = dX - bias * wX;

        ringHit = null;
        float bestScore = Float.MAX_VALUE;
        if (dZ <= pickTol && scoreZ < bestScore) { ringHit = Axis.Z; bestScore = scoreZ; }
        if (dY <= pickTol && scoreY < bestScore) { ringHit = Axis.Y; bestScore = scoreY; }
        if (dX <= pickTol && scoreX < bestScore) { ringHit = Axis.X; bestScore = scoreX; }

        if (ringHit != null)
        {
            UIPropTransform transform = uiModelForm.modelPanel.poseEditor.transform;
            transform.beginRotate();
            transform.setAxis(ringHit);
            System.out.println("[Gizmo] Picked rotation ring axis=" + ringHit + " best=" + bestRef[0]);
            return true;
        }

        System.out.println("[Gizmo] Rotation ring not hit");

        return false;
    }

    private Vector2f projectToScreen(Matrix4f mvp, float x, float y, float z)
    {
        Vector4f v = new Vector4f(x, y, z, 1);
        mvp.transform(v);

        if (Math.abs(v.w) < 1e-5f)
        {
            return null;
        }

        float nx = v.x / v.w;
        float ny = v.y / v.w;

        // NDC [-1,1] -> viewport pixels
        float sx = this.area.x + (nx * 0.5F + 0.5F) * this.area.w;
        float sy = this.area.y + ((-ny) * 0.5F + 0.5F) * this.area.h;

        return new Vector2f(sx, sy);
    }

    private float distanceToSegment(Vector2f p, Vector2f a, Vector2f b)
    {
        float vx = b.x - a.x;
        float vy = b.y - a.y;
        float wx = p.x - a.x;
        float wy = p.y - a.y;

        float c1 = vx * wx + vy * wy;
        if (c1 <= 0) return p.distance(a);

        float c2 = vx * vx + vy * vy;
        if (c2 <= c1) return p.distance(b);

        float t = c1 / c2;
        float px = a.x + t * vx;
        float py = a.y + t * vy;
        float dx = p.x - px;
        float dy = p.y - py;

        return (float) Math.sqrt(dx * dx + dy * dy);
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

        /* Sphere-only gizmo with clear hover highlight, always visible (no depth test) */
        if (UIBaseMenu.renderAxes)
        {
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();

            float baseRadius = 0.35F; // match on-model ring size
            float hoverRadius = baseRadius + 0.02F;

            Matrix4f mvp = new Matrix4f(this.camera.projection).mul(this.camera.view);
            Matrix4f origin = this.formEditor.editor.getOrigin(context.getTransition());
            if (origin != null) mvp.mul(origin);

            Vector2f center = projectToScreen(mvp, 0, 0, 0);
            boolean hover = false;
            if (center != null)
            {
                float pxTol = 28F;
                hover = center.distance(new Vector2f(context.mouseX, context.mouseY)) <= pxTol;
            }

            // 3 colored rotation rings (3D torus)
            float ringRadius = baseRadius;
            float band = 0.06F; // torus tube radius

            // XY plane (Z rotation) - blue
            Draw.renderDashedTorus(stack, ringRadius, band, 96, 16, 8, 0.55F, 0F, 0F, 1F, 0.95F);
            // XZ plane (Y rotation) - green
            stack.push();
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
            Draw.renderDashedTorus(stack, ringRadius, band, 96, 16, 8, 0.55F, 0F, 1F, 0F, 0.95F);
            stack.pop();
            // YZ plane (X rotation) - red
            stack.push();
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F));
            Draw.renderDashedTorus(stack, ringRadius, band, 96, 16, 8, 0.55F, 1F, 0F, 0F, 0.95F);
            stack.pop();

            // center sphere hover feedback
            if (hover)
            {
                Draw.renderSphere(stack, 0.09F, 14, 22, 1F, 1F, 0F, 0.8F);
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