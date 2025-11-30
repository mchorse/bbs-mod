package mchorse.bbs_mod.ui.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.Axis;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

public class Gizmo
{
    public final static int STENCIL_X = 1;
    public final static int STENCIL_Y = 2;
    public final static int STENCIL_Z = 3;
    public final static int STENCIL_XZ = 4;
    public final static int STENCIL_XY = 5;
    public final static int STENCIL_ZY = 6;

    public final static Gizmo INSTANCE = new Gizmo();

    private Mode mode = Mode.TRANSLATE;

    private int index;
    /* TODO: I'm too lazy to figure out right now the plane intersection algorithm for
     * proper transforms, but for now, it appears, this implementation works as well
     * not even that poorly! */
    private int mouseX;
    private int mouseY;

    private UIPropTransform currentTransform;

    private Gizmo()
    {}

    public Mode getMode()
    {
        return this.mode;
    }

    public boolean setMode(Mode mode)
    {
        boolean same = this.mode == mode;

        this.mode = mode;

        return !same;
    }

    public boolean start(int index, int mouseX, int mouseY, UIPropTransform transform)
    {
        if (index >= STENCIL_X && index <= STENCIL_ZY)
        {
            this.index = index;
            this.mouseX = mouseX;
            this.mouseY = mouseY;

            this.currentTransform = transform;

            if (transform != null)
            {
                if (this.index == STENCIL_X) transform.enableMode(this.mode.ordinal(), Axis.X);
                else if (this.index == STENCIL_Y) transform.enableMode(this.mode.ordinal(), Axis.Y);
                else if (this.index == STENCIL_Z) transform.enableMode(this.mode.ordinal(), Axis.Z);
            }

            return true;
        }

        return false;
    }

    public void stop()
    {
        this.index = -1;

        if (this.currentTransform != null)
        {
            this.currentTransform.acceptChanges();
        }

        this.currentTransform = null;
    }

    public void render(MatrixStack stack)
    {
        this.drawAxes(stack, 0.25F, 0.015F, 0.26F, 0.025F);
    }

    private void drawAxes(MatrixStack stack, float axisSize, float axisOffset, float outlineSize, float outlineOffset)
    {
        float scale = BBSSettings.axesScale.get();

        axisSize *= scale;
        axisOffset *= scale;
        outlineSize *= scale;
        outlineOffset *= scale;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        if (this.mode == Mode.ROTATE)
        {
            float outlinePad = 0.015F;
            float radius = 0.22F;
            float thicknessRing = 0.025F;

             Draw.arc3D(builder, stack, Axis.Z, radius, thicknessRing + outlinePad, 0F, 0F, 0F);
             Draw.arc3D(builder, stack, Axis.Z, radius, thicknessRing, 0F, 0F, 1F);

             Draw.arc3D(builder, stack, Axis.X, radius, thicknessRing + outlinePad, 0F, 0F, 0F);
             Draw.arc3D(builder, stack, Axis.X, radius, thicknessRing, 1F, 0F, 0F);

             Draw.arc3D(builder, stack, Axis.Y, radius, thicknessRing + outlinePad, 0F, 0F, 0F);
             Draw.arc3D(builder, stack, Axis.Y, radius, thicknessRing, 0F, 1F, 0F);
        }
        else
        {
            Draw.fillBox(builder, stack, 0, -outlineOffset, -outlineOffset, outlineSize, outlineOffset, outlineOffset, 0F, 0F, 0F);
            Draw.fillBox(builder, stack, -outlineOffset, 0, -outlineOffset, outlineOffset, outlineSize, outlineOffset, 0F, 0F, 0F);
            Draw.fillBox(builder, stack, -outlineOffset, -outlineOffset, 0, outlineOffset, outlineOffset, outlineSize, 0F, 0F, 0F);
            Draw.fillBox(builder, stack, -outlineOffset, -outlineOffset, -outlineOffset, outlineOffset, outlineOffset, outlineOffset, 0F, 0F, 0F);

            if (this.mode == Mode.SCALE)
            {
                float scaleStart = axisSize + axisOffset / 2F - outlineOffset / 2F;
                float scaleEnd = axisSize + axisOffset / 2F + outlineOffset / 2F;
                float offset = axisOffset * 2.75F;

                Draw.fillBox(builder, stack, scaleStart, -offset, -offset, scaleEnd, offset, offset, 0F, 0F, 0F);
                Draw.fillBox(builder, stack, -offset, scaleStart, -offset, offset, scaleEnd, offset, 0F, 0F, 0F);
                Draw.fillBox(builder, stack, -offset, -offset, scaleStart, offset, offset, scaleEnd, 0F, 0F, 0F);
            }

            Draw.fillBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize, axisOffset, axisOffset, 1F, 0F, 0F);
            Draw.fillBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize, axisOffset, 0F, 1F, 0F);
            Draw.fillBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize, 0F, 0F, 1F);
            Draw.fillBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, 1F, 1F, 1F);

            if (this.mode == Mode.SCALE)
            {
                float scaleEnd = axisSize + axisOffset;

                Draw.fillBox(builder, stack, axisSize, -axisOffset * 2F, -axisOffset * 2F, scaleEnd, axisOffset * 2F, axisOffset * 2F, 1F, 0F, 0F);
                Draw.fillBox(builder, stack, -axisOffset * 2F, axisSize, -axisOffset * 2F, axisOffset * 2F, scaleEnd, axisOffset * 2F, 0F, 1F, 0F);
                Draw.fillBox(builder, stack, -axisOffset * 2F, -axisOffset * 2F, axisSize, axisOffset * 2F, axisOffset * 2F, scaleEnd, 0F, 0F, 1F);
            }

            /* float l = axisSize * 0.25F;
            float o = 0.001F;
            float rr = axisSize * 0.65F;

            Draw.fillBox(builder, stack, l, -o, l, rr, o, rr, 1F, 0F, 1F);
            Draw.fillBox(builder, stack, l, l, -o, rr, rr, o, 1F, 1F, 0F);
            Draw.fillBox(builder, stack, -o, l, l, o, rr, rr, 0F, 1F, 1F); */
        }

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public void renderStencil(MatrixStack stack, StencilMap map)
    {
        this.drawAxes(stack, map, 0.25F, 0.015F);
    }

    private void drawAxes(MatrixStack stack, StencilMap map, float axisSize, float axisOffset)
    {
        float scale = BBSSettings.axesScale.get();

        axisSize *= scale;
        axisOffset *= scale;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        if (this.mode == Mode.ROTATE)
        {
            float outlinePad = 0.015F;
            float radius = 0.22F;
            float thicknessRing = 0.025F;

            Draw.arc3D(builder, stack, Axis.Z, radius, thicknessRing + outlinePad, STENCIL_Z / 255F, 0F, 0F);
            Draw.arc3D(builder, stack, Axis.X, radius, thicknessRing + outlinePad, STENCIL_X / 255F, 0F, 0F);
            Draw.arc3D(builder, stack, Axis.Y, radius, thicknessRing + outlinePad, STENCIL_Y / 255F, 0F, 0F);
        }
        else
        {
            Draw.fillBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize, axisOffset, axisOffset, STENCIL_X / 255F, 0F, 0F);
            Draw.fillBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize, axisOffset, STENCIL_Y / 255F, 0F, 0F);
            Draw.fillBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize, STENCIL_Z / 255F, 0F, 0F);
            Draw.fillBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, 0F, 0F, 0F);

            if (this.mode == Mode.SCALE)
            {
                float scaleEnd = axisSize + axisOffset;

                Draw.fillBox(builder, stack, axisSize, -axisOffset * 2F, -axisOffset * 2F, scaleEnd, axisOffset * 2F, axisOffset * 2F, STENCIL_X / 255F, 0F, 0F);
                Draw.fillBox(builder, stack, -axisOffset * 2F, axisSize, -axisOffset * 2F, axisOffset * 2F, scaleEnd, axisOffset * 2F, STENCIL_Y / 255F, 0F, 0F);
                Draw.fillBox(builder, stack, -axisOffset * 2F, -axisOffset * 2F, axisSize, axisOffset * 2F, axisOffset * 2F, scaleEnd, STENCIL_Z / 255F, 0F, 0F);
            }

            /* float l = axisSize * 0.25F;
            float o = 0.001F;
            float rr = axisSize * 0.65F;

            Draw.fillBox(builder, stack, l, -o, l, rr, o, rr, STENCIL_XZ / 255F, 0F, 0F);
            Draw.fillBox(builder, stack, l, l, -o, rr, rr, o, STENCIL_XY / 255F, 0F, 0F);
            Draw.fillBox(builder, stack, -o, l, l, o, rr, rr, STENCIL_ZY / 255F, 0F, 0F); */
        }

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public static enum Mode
    {
        TRANSLATE, SCALE, ROTATE;
    }
}