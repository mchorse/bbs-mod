package mchorse.bbs_mod.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.data.Angle;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class Draw
{
    public static void renderBox(MatrixStack stack, double x, double y, double z, double w, double h, double d)
    {
        renderBox(stack, x, y, z, w, h, d, 1, 1, 1);
    }

    public static void renderBox(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b)
    {
        renderBox(stack, x, y, z, w, h, d, r, g, b, 1F);
    }

    public static void renderBox(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        stack.push();
        stack.translate(x, y, z);
        float fw = (float) w;
        float fh = (float) h;
        float fd = (float) d;
        float t = 1 / 96F + (float) (Math.sqrt(w * w + h + h + d + d) / 2000);

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        /* Pillars: fillBox(builder, -t, -t, -t, t, t, t, r, g, b, a); */
        fillBox(builder, stack, -t, -t, -t, t, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t + fd, t, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t + fd, t + fw, t + fh, t + fd, r, g, b, a);

        /* Top */
        fillBox(builder, stack, -t, -t + fh, -t, t + fw, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t, -t + fh, -t + fd, t + fw, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t, -t + fh, -t, t, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t + fh, -t, t + fw, t + fh, t + fd, r, g, b, a);

        /* Bottom */
        fillBox(builder, stack, -t, -t, -t, t + fw, t, t, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t + fd, t + fw, t, t + fd, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t, t, t, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t, t + fd, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        stack.pop();
    }

    /**
     * Fill a quad for {@link net.minecraft.client.render.VertexFormats#POSITION_TEXTURE_COLOR_NORMAL}. Points should
     * be supplied in this order:
     *
     *     3 -------> 4
     *     ^
     *     |
     *     |
     *     2 <------- 1
     *
     * I.e. bottom left, bottom right, top left, top right, where left is -X and right is +X,
     * in case of a quad on fixed on Z axis.
     */
    public static void fillTexturedNormalQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, float r, float g, float b, float a, float nx, float ny, float nz)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BL, 2 - BR, 3 - TR, 4 - TL */
        builder.vertex(matrix4f, x2, y2, z2).texture(u1, v2).color(r, g, b, a).normal(nx, ny, nz).next();
        builder.vertex(matrix4f, x1, y1, z1).texture(u2, v2).color(r, g, b, a).normal(nx, ny, nz).next();
        builder.vertex(matrix4f, x4, y4, z4).texture(u2, v1).color(r, g, b, a).normal(nx, ny, nz).next();

        builder.vertex(matrix4f, x2, y2, z2).texture(u1, v2).color(r, g, b, a).normal(nx, ny, nz).next();
        builder.vertex(matrix4f, x4, y4, z4).texture(u2, v1).color(r, g, b, a).normal(nx, ny, nz).next();
        builder.vertex(matrix4f, x3, y3, z3).texture(u1, v1).color(r, g, b, a).normal(nx, ny, nz).next();
    }

    /**
     * Fill a quad for {@link net.minecraft.client.render.VertexFormats#POSITION_TEXTURE_COLOR}. Points should
     * be supplied in this order:
     *
     *     3 -------> 4
     *     ^
     *     |
     *     |
     *     2 <------- 1
     *
     * I.e. bottom left, bottom right, top left, top right, where left is -X and right is +X,
     * in case of a quad on fixed on Z axis.
     */
    public static void fillTexturedQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BL, 2 - BR, 3 - TR, 4 - TL */
        builder.vertex(matrix4f, x2, y2, z2).texture(u1, v2).color(r, g, b, a).next();
        builder.vertex(matrix4f, x1, y1, z1).texture(u2, v2).color(r, g, b, a).next();
        builder.vertex(matrix4f, x4, y4, z4).texture(u2, v1).color(r, g, b, a).next();

        builder.vertex(matrix4f, x2, y2, z2).texture(u1, v2).color(r, g, b, a);
        builder.vertex(matrix4f, x4, y4, z4).texture(u2, v1).color(r, g, b, a);
        builder.vertex(matrix4f, x3, y3, z3).texture(u1, v1).color(r, g, b, a);
    }

    public static void fillQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BR, 2 - BL, 3 - TL, 4 - TR */
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).next();
        builder.vertex(matrix4f, x2, y2, z2).color(r, g, b, a).next();
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).next();
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).next();
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).next();
        builder.vertex(matrix4f, x4, y4, z4).color(r, g, b, a).next();
    }

    public static void fillBoxTo(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a)
    {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Angle angle = Angle.angle(dx, dy, dz);

        stack.push();

        stack.translate(x1, y1, z1);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle.yaw));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle.pitch));

        fillBox(builder, stack, -thickness / 2, -thickness / 2, 0, thickness / 2, thickness / 2, (float) distance, r, g, b, a);

        stack.pop();
    }

    public static void fillBox(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b)
    {
        fillBox(builder, stack, x1, y1, z1, x2, y2, z2, r, g, b, 1F);
    }

    public static void fillBox(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a)
    {
        /* X */
        fillQuad(builder, stack, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, r, g, b, a);
        fillQuad(builder, stack, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);

        /* Y */
        fillQuad(builder, stack, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
        fillQuad(builder, stack, x2, y2, z1, x1, y2, z1, x1, y2, z2, x2, y2, z2, r, g, b, a);

        /* Z */
        fillQuad(builder, stack, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, r, g, b, a);
        fillQuad(builder, stack, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a);
    }

    public static void axes(MatrixStack stack, float length, float thickness)
    {
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Draw.axes(builder, stack, length, thickness);

        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public static void axes(BufferBuilder builder, MatrixStack stack, float length, float thickness)
    {
        fillBox(builder, stack, thickness, -thickness, -thickness, length, thickness, thickness, 1, 0, 0, 1);
        fillBox(builder, stack, -thickness, -thickness, -thickness, thickness, length, thickness, 0, 1, 0, 1);
        fillBox(builder, stack, -thickness, -thickness, thickness, thickness, thickness, length, 0, 0, 1, 1);
    }

    public static void coolerAxes(MatrixStack stack, float axisSize, float axisOffset)
    {
        final float outlineSize = axisSize + 0.005F;
        final float outlineOffset = axisOffset + 0.005F;

        coolerAxes(stack, axisSize, axisOffset, outlineSize, outlineOffset);
    }

    public static void coolerAxes(MatrixStack stack, float axisSize, float axisOffset, float outlineSize, float outlineOffset)
    {
        float scale = BBSSettings.axesScale.get();

        axisSize *= scale;
        axisOffset *= scale;
        outlineSize *= scale;
        outlineOffset *= scale;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        fillBox(builder, stack, 0, -outlineOffset, -outlineOffset, outlineSize, outlineOffset, outlineOffset, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, 0, -outlineOffset, outlineOffset, outlineSize, outlineOffset, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, -outlineOffset, 0, outlineOffset, outlineOffset, outlineSize, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, -outlineOffset, -outlineOffset, outlineOffset, outlineOffset, outlineOffset, 0, 0, 0);

        fillBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize, axisOffset, axisOffset, 1, 0, 0);
        fillBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize, axisOffset, 0, 1, 0);
        fillBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize, 0, 0, 1);
        fillBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, 1, 1, 1);

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    /**
     * Render a chunkier 3D-style gizmo with thicker shafts and box arrowheads.
     * axisLen: length of each axis
     * shaft: half-thickness of axis shaft
     * headLen: length of the arrowhead (along axis)
     * headSize: half-size of the arrowhead box cross-section
     */
    public static void gizmoAxes(MatrixStack stack, float axisLen, float shaft, float headLen, float headSize)
    {
        float scale = BBSSettings.axesScale.get();

        axisLen *= scale;
        shaft *= scale;
        headLen *= scale;
        headSize *= scale;

        float r = 1, g = 0, b = 0, a = 1;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        // X axis (red)
        fillBox(builder, stack, 0, -shaft, -shaft, Math.max(axisLen - headLen, 0.0001F), shaft, shaft, r, g, b, a);
        fillBox(builder, stack, axisLen - headLen, -headSize, -headSize, axisLen, headSize, headSize, r, g, b, a);

        // Y axis (green)
        r = 0; g = 1; b = 0;
        fillBox(builder, stack, -shaft, 0, -shaft, shaft, Math.max(axisLen - headLen, 0.0001F), shaft, r, g, b, a);
        fillBox(builder, stack, -headSize, axisLen - headLen, -headSize, headSize, axisLen, headSize, r, g, b, a);

        // Z axis (blue)
        r = 0; g = 0; b = 1;
        fillBox(builder, stack, -shaft, -shaft, 0, shaft, shaft, Math.max(axisLen - headLen, 0.0001F), r, g, b, a);
        fillBox(builder, stack, -headSize, -headSize, axisLen - headLen, headSize, headSize, axisLen, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    /**
     * Render a simple UV-sphere at the current origin using POSITION_COLOR triangles.
     * Radius is in the current matrix units. Segments/rings control tessellation.
     */
    public static void renderSphere(MatrixStack stack, float radius, int rings, int segments, float r, float g, float b, float a)
    {
        if (rings < 3) rings = 3;
        if (segments < 6) segments = 6;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Matrix4f m = stack.peek().getPositionMatrix();

        for (int i = 0; i < rings; i++)
        {
            float v0 = (float) i / rings;
            float v1 = (float) (i + 1) / rings;

            float theta0 = (float) Math.PI * v0 - (float) Math.PI / 2F;
            float theta1 = (float) Math.PI * v1 - (float) Math.PI / 2F;

            float y0 = (float) Math.sin(theta0) * radius;
            float y1 = (float) Math.sin(theta1) * radius;
            float r0 = (float) Math.cos(theta0) * radius;
            float r1 = (float) Math.cos(theta1) * radius;

            for (int j = 0; j < segments; j++)
            {
                float u0 = (float) j / segments;
                float u1 = (float) (j + 1) / segments;

                float phi0 = (float) (2 * Math.PI) * u0;
                float phi1 = (float) (2 * Math.PI) * u1;

                float x00 = (float) Math.cos(phi0) * r0;
                float z00 = (float) Math.sin(phi0) * r0;
                float x01 = (float) Math.cos(phi1) * r0;
                float z01 = (float) Math.sin(phi1) * r0;

                float x10 = (float) Math.cos(phi0) * r1;
                float z10 = (float) Math.sin(phi0) * r1;
                float x11 = (float) Math.cos(phi1) * r1;
                float z11 = (float) Math.sin(phi1) * r1;

                // Quad as two triangles (upper strip segment)
                builder.vertex(m, x00, y0, z00).color(r, g, b, a).next();
                builder.vertex(m, x10, y1, z10).color(r, g, b, a).next();
                builder.vertex(m, x11, y1, z11).color(r, g, b, a).next();

                builder.vertex(m, x00, y0, z00).color(r, g, b, a).next();
                builder.vertex(m, x11, y1, z11).color(r, g, b, a).next();
                builder.vertex(m, x01, y0, z01).color(r, g, b, a).next();
            }
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    /**
     * Render a flat ring (annulus) in the XY plane centered at origin.
     * Radius is the centerline radius; thickness is half-width of the band.
     */
    public static void renderRing(MatrixStack stack, float radius, float thickness, int segments, float r, float g, float b, float a)
    {
        if (segments < 16) segments = 16;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Matrix4f m = stack.peek().getPositionMatrix();

        float inner = Math.max(radius - thickness, 0.0001F);
        float outer = radius + thickness;

        for (int i = 0; i < segments; i++)
        {
            float t0 = (float) (2 * Math.PI * i / segments);
            float t1 = (float) (2 * Math.PI * (i + 1) / segments);

            float ci0 = (float) Math.cos(t0) * inner;
            float si0 = (float) Math.sin(t0) * inner;
            float co0 = (float) Math.cos(t0) * outer;
            float so0 = (float) Math.sin(t0) * outer;

            float ci1 = (float) Math.cos(t1) * inner;
            float si1 = (float) Math.sin(t1) * inner;
            float co1 = (float) Math.cos(t1) * outer;
            float so1 = (float) Math.sin(t1) * outer;

            // front-facing
            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();
            builder.vertex(m, ci0, si0, 0).color(r, g, b, a).next();
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();

            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();
            builder.vertex(m, co1, so1, 0).color(r, g, b, a).next();

            // back-facing (reverse winding) for double-sided ring
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();
            builder.vertex(m, ci0, si0, 0).color(r, g, b, a).next();
            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();

            builder.vertex(m, co1, so1, 0).color(r, g, b, a).next();
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();
            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    /**
     * Dashed version of renderRing. period is the number of segments per on+off cycle,
     * dutyCycle in [0,1] is the fraction of the period that is rendered (e.g. 0.5 = even dashes).
     */
    public static void renderDashedRing(MatrixStack stack, float radius, float thickness, int segments, int period, float dutyCycle, float r, float g, float b, float a)
    {
        if (segments < 16) segments = 16;
        if (period < 2) period = 2;
        if (dutyCycle < 0F) dutyCycle = 0F; else if (dutyCycle > 1F) dutyCycle = 1F;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Matrix4f m = stack.peek().getPositionMatrix();

        float inner = Math.max(radius - thickness, 0.0001F);
        float outer = radius + thickness;

        int onCount = Math.max(1, Math.round(period * dutyCycle));

        for (int i = 0; i < segments; i++)
        {
            int idx = i % period;
            if (idx >= onCount) continue; // gap

            float t0 = (float) (2 * Math.PI * i / segments);
            float t1 = (float) (2 * Math.PI * (i + 1) / segments);

            float ci0 = (float) Math.cos(t0) * inner;
            float si0 = (float) Math.sin(t0) * inner;
            float co0 = (float) Math.cos(t0) * outer;
            float so0 = (float) Math.sin(t0) * outer;

            float ci1 = (float) Math.cos(t1) * inner;
            float si1 = (float) Math.sin(t1) * inner;
            float co1 = (float) Math.cos(t1) * outer;
            float so1 = (float) Math.sin(t1) * outer;

            // front-facing
            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();
            builder.vertex(m, ci0, si0, 0).color(r, g, b, a).next();
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();

            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();
            builder.vertex(m, co1, so1, 0).color(r, g, b, a).next();

            // back-facing
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();
            builder.vertex(m, ci0, si0, 0).color(r, g, b, a).next();
            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();

            builder.vertex(m, co1, so1, 0).color(r, g, b, a).next();
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();
            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    /**
     * Render a 3D torus (tube ring) centered at origin in the XY plane.
     * radius: distance from center to tube center. tubeRadius: radius of tube cross-section.
     * segments: around the main ring. tubeSegments: around the tube.
     */
    public static void renderTorus(MatrixStack stack, float radius, float tubeRadius, int segments, int tubeSegments, float r, float g, float b, float a)
    {
        if (segments < 24) segments = 24;
        if (tubeSegments < 8) tubeSegments = 8;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Matrix4f m = stack.peek().getPositionMatrix();

        for (int i = 0; i < segments; i++)
        {
            float theta0 = (float) (2 * Math.PI * i / segments);
            float theta1 = (float) (2 * Math.PI * (i + 1) / segments);

            float cos0 = (float) Math.cos(theta0);
            float sin0 = (float) Math.sin(theta0);
            float cos1 = (float) Math.cos(theta1);
            float sin1 = (float) Math.sin(theta1);

            for (int j = 0; j < tubeSegments; j++)
            {
                float phi0 = (float) (2 * Math.PI * j / tubeSegments);
                float phi1 = (float) (2 * Math.PI * (j + 1) / tubeSegments);

                float c0 = (float) Math.cos(phi0);
                float s0 = (float) Math.sin(phi0);
                float c1t = (float) Math.cos(phi1);
                float s1t = (float) Math.sin(phi1);

                // Four points on tube quad
                float x00 = (radius + tubeRadius * c0) * cos0;
                float y00 = (radius + tubeRadius * c0) * sin0;
                float z00 = tubeRadius * s0;

                float x01 = (radius + tubeRadius * c1t) * cos0;
                float y01 = (radius + tubeRadius * c1t) * sin0;
                float z01 = tubeRadius * s1t;

                float x10 = (radius + tubeRadius * c0) * cos1;
                float y10 = (radius + tubeRadius * c0) * sin1;
                float z10 = tubeRadius * s0;

                float x11 = (radius + tubeRadius * c1t) * cos1;
                float y11 = (radius + tubeRadius * c1t) * sin1;
                float z11 = tubeRadius * s1t;

                // Two triangles per quad, double-sided
                builder.vertex(m, x00, y00, z00).color(r, g, b, a).next();
                builder.vertex(m, x10, y10, z10).color(r, g, b, a).next();
                builder.vertex(m, x11, y11, z11).color(r, g, b, a).next();

                builder.vertex(m, x00, y00, z00).color(r, g, b, a).next();
                builder.vertex(m, x11, y11, z11).color(r, g, b, a).next();
                builder.vertex(m, x01, y01, z01).color(r, g, b, a).next();

                // back faces
                builder.vertex(m, x11, y11, z11).color(r, g, b, a).next();
                builder.vertex(m, x10, y10, z10).color(r, g, b, a).next();
                builder.vertex(m, x00, y00, z00).color(r, g, b, a).next();

                builder.vertex(m, x01, y01, z01).color(r, g, b, a).next();
                builder.vertex(m, x11, y11, z11).color(r, g, b, a).next();
                builder.vertex(m, x00, y00, z00).color(r, g, b, a).next();
            }
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    /**
     * Dashed version of renderTorus: skips whole main segments to create dashed 3D tube.
     * period: number of main segments per on+off cycle. dutyCycle: fraction rendered per cycle.
     */
    public static void renderDashedTorus(MatrixStack stack, float radius, float tubeRadius, int segments, int tubeSegments, int period, float dutyCycle, float r, float g, float b, float a)
    {
        if (segments < 24) segments = 24;
        if (tubeSegments < 8) tubeSegments = 8;
        if (period < 2) period = 2;
        if (dutyCycle < 0F) dutyCycle = 0F; else if (dutyCycle > 1F) dutyCycle = 1F;

        int onCount = Math.max(1, Math.round(period * dutyCycle));

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Matrix4f m = stack.peek().getPositionMatrix();

        for (int i = 0; i < segments; i++)
        {
            int idx = i % period;
            if (idx >= onCount) continue; // gap segment

            float theta0 = (float) (2 * Math.PI * i / segments);
            float theta1 = (float) (2 * Math.PI * (i + 1) / segments);

            float cos0 = (float) Math.cos(theta0);
            float sin0 = (float) Math.sin(theta0);
            float cos1 = (float) Math.cos(theta1);
            float sin1 = (float) Math.sin(theta1);

            for (int j = 0; j < tubeSegments; j++)
            {
                float phi0 = (float) (2 * Math.PI * j / tubeSegments);
                float phi1 = (float) (2 * Math.PI * (j + 1) / tubeSegments);

                float c0 = (float) Math.cos(phi0);
                float s0 = (float) Math.sin(phi0);
                float c1t = (float) Math.cos(phi1);
                float s1t = (float) Math.sin(phi1);

                float x00 = (radius + tubeRadius * c0) * cos0;
                float y00 = (radius + tubeRadius * c0) * sin0;
                float z00 = tubeRadius * s0;

                float x01 = (radius + tubeRadius * c1t) * cos0;
                float y01 = (radius + tubeRadius * c1t) * sin0;
                float z01 = tubeRadius * s1t;

                float x10 = (radius + tubeRadius * c0) * cos1;
                float y10 = (radius + tubeRadius * c0) * sin1;
                float z10 = tubeRadius * s0;

                float x11 = (radius + tubeRadius * c1t) * cos1;
                float y11 = (radius + tubeRadius * c1t) * sin1;
                float z11 = tubeRadius * s1t;

                builder.vertex(m, x00, y00, z00).color(r, g, b, a).next();
                builder.vertex(m, x10, y10, z10).color(r, g, b, a).next();
                builder.vertex(m, x11, y11, z11).color(r, g, b, a).next();

                builder.vertex(m, x00, y00, z00).color(r, g, b, a).next();
                builder.vertex(m, x11, y11, z11).color(r, g, b, a).next();
                builder.vertex(m, x01, y01, z01).color(r, g, b, a).next();

                builder.vertex(m, x11, y11, z11).color(r, g, b, a).next();
                builder.vertex(m, x10, y10, z10).color(r, g, b, a).next();
                builder.vertex(m, x00, y00, z00).color(r, g, b, a).next();

                builder.vertex(m, x01, y01, z01).color(r, g, b, a).next();
                builder.vertex(m, x11, y11, z11).color(r, g, b, a).next();
                builder.vertex(m, x00, y00, z00).color(r, g, b, a).next();
            }
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    /**
     * Render a refined 3D transformation gizmo with thin arrows and slender rings.
     * Arrows are used for translation, rings for rotation.
     */
    public static void renderTransformationGizmo(MatrixStack stack, float scale, float r, float g, float b, float a)
    {
        float axisLength = 0.5F * scale;
        float axisThickness = 0.008F * scale;  // Thin shafts
        float cubeSize = 0.04F * scale;        // Small cubes at base
        float arrowLength = 0.08F * scale;     // Short arrowheads
        float arrowWidth = 0.03F * scale;      // Thin arrowheads
        float ringRadius = 0.35F * scale;
        float ringThickness = 0.015F * scale;  // Thin rings
        float originSize = 0.04F * scale;      // Small origin sphere

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        // X axis (red) - horizontal with negative extension
        renderAxisArrowWithNegative(builder, stack, axisLength, axisThickness, cubeSize, arrowLength, arrowWidth, 1F, 0F, 0F, a);
        
        // Y axis (green) - vertical with negative extension
        stack.push();
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
        renderAxisArrowWithNegative(builder, stack, axisLength, axisThickness, cubeSize, arrowLength, arrowWidth, 0F, 1F, 0F, a);
        stack.pop();
        
        // Z axis (blue) - depth with negative extension
        stack.push();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90F));
        renderAxisArrowWithNegative(builder, stack, axisLength, axisThickness, cubeSize, arrowLength, arrowWidth, 0F, 0F, 1F, a);
        stack.pop();

        BufferRenderer.drawWithGlobalProgram(builder.end());

        // Rotation rings - thin appearance
        // XY plane (Z rotation) - blue
        renderRing(stack, ringRadius, ringThickness, 64, 0F, 0F, 1F, a);
        
        // XZ plane (Y rotation) - green
        stack.push();
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
        renderRing(stack, ringRadius, ringThickness, 64, 0F, 1F, 0F, a);
        stack.pop();
        
        // YZ plane (X rotation) - red
        stack.push();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F));
        renderRing(stack, ringRadius, ringThickness, 64, 1F, 0F, 0F, a);
        stack.pop();

        // Central origin point - smaller and brighter
        renderSphere(stack, originSize, 12, 16, 1F, 1F, 1F, a);
    }

    /**
     * Render a single axis arrow with cube at base, arrowhead at tip, and negative axis line.
     * Creates a refined, thin appearance matching the reference image.
     */
    private static void renderAxisArrowWithNegative(BufferBuilder builder, MatrixStack stack, float length, float thickness, float cubeSize, float arrowLength, float arrowWidth, float r, float g, float b, float a)
    {
        Matrix4f m = stack.peek().getPositionMatrix();

        // Small cube at the base (origin)
        float halfCube = cubeSize / 2F;
        fillBox(builder, stack, -halfCube, -halfCube, -halfCube, halfCube, halfCube, halfCube, r, g, b, a);

        // Positive axis shaft (thinner)
        float shaftEnd = length - arrowLength;
        fillBox(builder, stack, 0, -thickness, -thickness, shaftEnd, thickness, thickness, r, g, b, a);

        // Negative axis line (even thinner)
        float negThickness = thickness * 0.6F;
        fillBox(builder, stack, -length * 0.3F, -negThickness, -negThickness, 0, negThickness, negThickness, r, g, b, a);

        // Sharp, thin arrowhead (pyramid)
        float halfArrow = arrowWidth / 2F;
        
        // Arrowhead faces - much sharper and thinner
        // Front face
        builder.vertex(m, shaftEnd, -halfArrow, -halfArrow).color(r, g, b, a).next();
        builder.vertex(m, shaftEnd, halfArrow, -halfArrow).color(r, g, b, a).next();
        builder.vertex(m, length, 0, 0).color(r, g, b, a).next();

        // Back face
        builder.vertex(m, shaftEnd, halfArrow, halfArrow).color(r, g, b, a).next();
        builder.vertex(m, shaftEnd, -halfArrow, halfArrow).color(r, g, b, a).next();
        builder.vertex(m, length, 0, 0).color(r, g, b, a).next();

        // Top face
        builder.vertex(m, shaftEnd, halfArrow, -halfArrow).color(r, g, b, a).next();
        builder.vertex(m, shaftEnd, halfArrow, halfArrow).color(r, g, b, a).next();
        builder.vertex(m, length, 0, 0).color(r, g, b, a).next();

        // Bottom face
        builder.vertex(m, shaftEnd, -halfArrow, halfArrow).color(r, g, b, a).next();
        builder.vertex(m, shaftEnd, -halfArrow, -halfArrow).color(r, g, b, a).next();
        builder.vertex(m, length, 0, 0).color(r, g, b, a).next();

        // Left face
        builder.vertex(m, shaftEnd, -halfArrow, halfArrow).color(r, g, b, a).next();
        builder.vertex(m, shaftEnd, halfArrow, halfArrow).color(r, g, b, a).next();
        builder.vertex(m, length, 0, 0).color(r, g, b, a).next();

        // Right face
        builder.vertex(m, shaftEnd, halfArrow, -halfArrow).color(r, g, b, a).next();
        builder.vertex(m, shaftEnd, -halfArrow, -halfArrow).color(r, g, b, a).next();
        builder.vertex(m, length, 0, 0).color(r, g, b, a).next();
    }

}