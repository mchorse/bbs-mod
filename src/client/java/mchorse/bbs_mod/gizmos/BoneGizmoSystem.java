package mchorse.bbs_mod.gizmos;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.UIRenderingContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.Timer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Sistema de gizmo para transformaciones básicas de huesos/modelos.
 *
 * Implementación inicial en 2D (screen-space):
 * - Modos: traducir, rotar, escalar
 * - Ejes: X (rojo), Y (verde), Z (azul)
 * - Picking: selección de eje por hover/click
 * - Arrastre: modifica el Transform activo según el modo y eje
 *
 * Nota: Esta versión no proyecta geometría 3D; se renderiza y opera en overlay 2D.
 * Más adelante se integrará picking 3D con rayos y/o ImGuizmo.
 */
public class BoneGizmoSystem
{
    public enum Mode { TRANSLATE, ROTATE, SCALE }

    private static final BoneGizmoSystem INSTANCE = new BoneGizmoSystem();

    /* Estado */
    private Mode mode = Mode.TRANSLATE;
    private Axis hoveredAxis = null;
    private Axis activeAxis = null;
    private boolean dragging = false;
    private boolean lastMouseDown = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private Transform dragStart = new Transform();
    private UIPropTransform target;

    /* Soporte de "mouse en bucle" durante el arrastre (como en UIPropTransform) */
    private static final double[] CURSOR_X = new double[1];
    private static final double[] CURSOR_Y = new double[1];
    private final Timer wrapChecker = new Timer(30);
    private int lastX = 0;
    private int lastY = 0;
    private float accumDx = 0F;
    private float accumDy = 0F;

    /* Layout del gizmo 2D */
    private int centerX;
    private int centerY;
    private int handleLen = 100;
    private int handleThickness = 5;
    private int hitRadius = 10;

    /* Radios para anillos de rotación (esferas lineales) */
    private int ringRX = 50; /* rojo */
    private int ringRY = 70; /* verde */
    private int ringRZ = 90; /* azul */

    /* Endpoints proyectados en pantalla para cada eje */
    private int endXx, endXy;
    private int endYx, endYy;
    private int endZx, endZy;

    public static BoneGizmoSystem get()
    {
        return INSTANCE;
    }

    /** Establece el modo del gizmo directamente (T/R/S) */
    public void setMode(Mode mode)
    {
        this.mode = mode;
    }

    public void update(UIContext input, Area viewport, UIPropTransform target)
    {
        this.target = target;
        if (viewport == null)
        {
            return;
        }

        this.centerX = viewport.mx();
        this.centerY = viewport.my();

        /* Detectar eje hovered */
        this.hoveredAxis = detectHoveredAxis(input.mouseX, input.mouseY);

        /* Inicio de arrastre (edge detection del botón derecho) */
        boolean mouseDown = Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        if (!this.dragging && mouseDown && !this.lastMouseDown && this.hoveredAxis != null)
        {
            this.dragging = true;
            this.activeAxis = this.hoveredAxis;
            this.dragStartX = input.mouseX;
            this.dragStartY = input.mouseY;
            this.lastX = input.mouseX;
            this.lastY = input.mouseY;
            this.accumDx = 0F;
            this.accumDy = 0F;
            if (this.target != null && this.target.getTransform() != null)
            {
                this.dragStart.copy(this.target.getTransform());
            }
        }

        /* Terminar arrastre */
        if (this.dragging && !mouseDown && this.lastMouseDown)
        {
            this.dragging = false;
            this.activeAxis = null;
        }

        /* Durante arrastre: aplicar delta */
        if (this.dragging && this.target != null && this.target.getTransform() != null)
        {
            /* Implementar bucle horizontal y vertical del cursor */
            GLFW.glfwGetCursorPos(Window.getWindow(), CURSOR_X, CURSOR_Y);
            MinecraftClient mc = MinecraftClient.getInstance();
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();

            double rawX = CURSOR_X[0];
            double rawY = CURSOR_Y[0];
            double fx = Math.ceil(w / (double) input.menu.width);
            double fy = Math.ceil(h / (double) input.menu.height);
            int border = 5;
            int borderPadding = border + 1;

            if (rawX <= border)
            {
                Window.moveCursor(w - borderPadding, (int) mc.mouse.getY());
                this.lastX = input.menu.width - (int) (borderPadding / fx);
                this.wrapChecker.mark();
            }
            else if (rawX >= w - border)
            {
                Window.moveCursor(borderPadding, (int) mc.mouse.getY());
                this.lastX = (int) (borderPadding / fx);
                this.wrapChecker.mark();
            }

            if (rawY <= border)
            {
                Window.moveCursor((int) mc.mouse.getX(), h - borderPadding);
                this.lastY = input.menu.height - (int) (borderPadding / fy);
                this.wrapChecker.mark();
            }
            else if (rawY >= h - border)
            {
                Window.moveCursor((int) mc.mouse.getX(), borderPadding);
                this.lastY = (int) (borderPadding / fy);
                this.wrapChecker.mark();
            }

            int stepX = input.mouseX - this.lastX;
            int stepY = input.mouseY - this.lastY;
            this.accumDx += stepX;
            this.accumDy += stepY;
            this.lastX = input.mouseX;
            this.lastY = input.mouseY;

            float factor = switch (this.mode)
            {
                case TRANSLATE -> 0.02F;
                case SCALE -> 0.01F;
                case ROTATE -> 0.3F;
            };

            float delta = this.accumDx * factor; /* Usamos movimiento horizontal para consistencia */

            Transform t = this.target.getTransform();

            if (this.mode == Mode.TRANSLATE)
            {
                float x = t.translate.x;
                float y = t.translate.y;
                float z = t.translate.z;

                if (this.activeAxis == Axis.X) x = this.dragStart.translate.x + delta;
                if (this.activeAxis == Axis.Y) y = this.dragStart.translate.y - this.accumDy * factor; /* Y hacia arriba */
                if (this.activeAxis == Axis.Z) z = this.dragStart.translate.z + delta;

                this.target.setT(null, x, y, z);
                this.target.setTransform(t);
            }
            else if (this.mode == Mode.SCALE)
            {
                float x = t.scale.x;
                float y = t.scale.y;
                float z = t.scale.z;

                if (this.activeAxis == Axis.X) x = clampScale(this.dragStart.scale.x + delta);
                if (this.activeAxis == Axis.Y) y = clampScale(this.dragStart.scale.y + delta);
                if (this.activeAxis == Axis.Z) z = clampScale(this.dragStart.scale.z + delta);

                this.target.setS(null, x, y, z);
                this.target.setTransform(t);
            }
            else if (this.mode == Mode.ROTATE)
            {
                /* Rotaciones en grados en UIPropTransform.setR */
                float rx = (float) Math.toDegrees(this.dragStart.rotate.x);
                float ry = (float) Math.toDegrees(this.dragStart.rotate.y);
                float rz = (float) Math.toDegrees(this.dragStart.rotate.z);

                if (this.activeAxis == Axis.X) rx = rx + delta * 10F;
                if (this.activeAxis == Axis.Y) ry = ry + delta * 10F;
                if (this.activeAxis == Axis.Z) rz = rz + delta * 10F;

                this.target.setR(null, rx, ry, rz);
                this.target.setTransform(t);
            }
        }

        this.lastMouseDown = mouseDown;
    }

    /**
     * Actualiza el gizmo posicionándolo en el centro proyectado de una matriz de origen 3D.
     *
     * Este método calcula la posición en pantalla del punto (0,0,0) transformado por la
     * matriz de origen, utilizando las matrices de proyección y vista activas y el viewport
     * proporcionado, y luego delega el resto del manejo de interacción al método de update
     * estándar.
     */
    public void update(UIContext input, Area viewport, Matrix4f origin, Matrix4f projection, Matrix4f view, UIPropTransform target)
    {
        this.target = target;
        if (viewport == null)
        {
            return;
        }

        if (origin != null && projection != null && view != null)
        {
            Matrix4f mvp = new Matrix4f(projection).mul(view).mul(origin);

            org.joml.Vector4f p = new org.joml.Vector4f(0, 0, 0, 1);
            mvp.transform(p);

            if (p.w != 0)
            {
                float ndcX = p.x / p.w;
                float ndcY = p.y / p.w;

                int px = viewport.x + (int) (((ndcX + 1F) * 0.5F) * viewport.w);
                int py = viewport.y + (int) (((-ndcY + 1F) * 0.5F) * viewport.h);

                this.centerX = px;
                this.centerY = py;

                /* Calcular endpoints de ejes (locales o globales) */
                float axisLen = 0.6F; // longitud más corta para que los handles queden cerca del pivote
                // Posición mundial del origen
                org.joml.Vector4f p0World = new org.joml.Vector4f(0, 0, 0, 1);
                origin.transform(p0World);

                // Direcciones en mundo para cada eje
                boolean useLocal = this.target != null && this.target.isLocal();

                org.joml.Vector4f dx = new org.joml.Vector4f(axisLen, 0, 0, useLocal ? 0 : 0);
                org.joml.Vector4f dy = new org.joml.Vector4f(0, axisLen, 0, useLocal ? 0 : 0);
                org.joml.Vector4f dz = new org.joml.Vector4f(0, 0, axisLen, useLocal ? 0 : 0);

                if (useLocal)
                {
                    // Transformar direcciones por la rotación local del hueso
                    origin.transform(dx);
                    origin.transform(dy);
                    origin.transform(dz);

                    // Al ser w=0, origin.transform nos da vector en mundo relativo al origen.
                    dx.x += p0World.x; dx.y += p0World.y; dx.z += p0World.z; dx.w = 1;
                    dy.x += p0World.x; dy.y += p0World.y; dy.z += p0World.z; dy.w = 1;
                    dz.x += p0World.x; dz.y += p0World.y; dz.z += p0World.z; dz.w = 1;
                }
                else
                {
                    // Global: sumar los ejes del mundo al origen
                    dx.x = p0World.x + axisLen; dx.y = p0World.y;            dx.z = p0World.z;            dx.w = 1;
                    dy.x = p0World.x;            dy.y = p0World.y + axisLen; dy.z = p0World.z;            dy.w = 1;
                    dz.x = p0World.x;            dz.y = p0World.y;            dz.z = p0World.z + axisLen; dz.w = 1;
                }

                // Proyectar a pantalla
                org.joml.Vector4f sx = new org.joml.Vector4f(dx);
                org.joml.Vector4f sy = new org.joml.Vector4f(dy);
                org.joml.Vector4f sz = new org.joml.Vector4f(dz);

                Matrix4f pv = new Matrix4f(projection).mul(view);
                pv.transform(sx);
                pv.transform(sy);
                pv.transform(sz);

                if (sx.w != 0)
                {
                    float ndcXx = sx.x / sx.w;
                    float ndcXy = sx.y / sx.w;
                    this.endXx = viewport.x + (int) (((ndcXx + 1F) * 0.5F) * viewport.w);
                    this.endXy = viewport.y + (int) (((-ndcXy + 1F) * 0.5F) * viewport.h);
                }

                if (sy.w != 0)
                {
                    float ndcYx = sy.x / sy.w;
                    float ndcYy = sy.y / sy.w;
                    this.endYx = viewport.x + (int) (((ndcYx + 1F) * 0.5F) * viewport.w);
                    this.endYy = viewport.y + (int) (((-ndcYy + 1F) * 0.5F) * viewport.h);
                }

                if (sz.w != 0)
                {
                    float ndcZx = sz.x / sz.w;
                    float ndcZy = sz.y / sz.w;
                    this.endZx = viewport.x + (int) (((ndcZx + 1F) * 0.5F) * viewport.w);
                    this.endZy = viewport.y + (int) (((-ndcZy + 1F) * 0.5F) * viewport.h);
                }
            }
            else
            {
                this.centerX = viewport.mx();
                this.centerY = viewport.my();
                // Fallback: handles alrededor del centro
                this.endXx = this.centerX + handleLen;
                this.endXy = this.centerY;
                this.endYx = this.centerX;
                this.endYy = this.centerY - handleLen;
                this.endZx = this.centerX;
                this.endZy = this.centerY + handleLen;
            }
        }
        else
        {
            this.centerX = viewport.mx();
            this.centerY = viewport.my();
            // Fallback: handles alrededor del centro
            this.endXx = this.centerX + handleLen;
            this.endXy = this.centerY;
            this.endYx = this.centerX;
            this.endYy = this.centerY - handleLen;
            this.endZx = this.centerX;
            this.endZy = this.centerY + handleLen;
        }

        /* Detectar eje hovered */
        this.hoveredAxis = detectHoveredAxis(input.mouseX, input.mouseY);

        /* Inicio de arrastre (edge detection del botón izquierdo) */
        boolean mouseDown = Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        if (!this.dragging && mouseDown && !this.lastMouseDown && this.hoveredAxis != null)
        {
            this.dragging = true;
            this.activeAxis = this.hoveredAxis;
            this.dragStartX = input.mouseX;
            this.dragStartY = input.mouseY;
            this.lastX = input.mouseX;
            this.lastY = input.mouseY;
            this.accumDx = 0F;
            this.accumDy = 0F;
            if (this.target != null && this.target.getTransform() != null)
            {
                this.dragStart.copy(this.target.getTransform());
            }
        }

        /* Terminar arrastre */
        if (this.dragging && !mouseDown && this.lastMouseDown)
        {
            this.dragging = false;
            this.activeAxis = null;
        }

        /* Durante arrastre: aplicar delta con bucle de mouse como en UIPropTransform */
        if (this.dragging && this.target != null && this.target.getTransform() != null)
        {
            boolean warped = false;

            if (this.wrapChecker.isTime())
            {
                GLFW.glfwGetCursorPos(Window.getWindow(), CURSOR_X, CURSOR_Y);
                MinecraftClient mc = MinecraftClient.getInstance();
                int w = mc.getWindow().getWidth();
                int h = mc.getWindow().getHeight();

                double rawX = CURSOR_X[0];
                double rawY = CURSOR_Y[0];
                double fx = Math.ceil(w / (double) input.menu.width);
                double fy = Math.ceil(h / (double) input.menu.height);
                int border = 5;
                int borderPadding = border + 1;

                if (rawX <= border)
                {
                    Window.moveCursor(w - borderPadding, (int) mc.mouse.getY());

                    this.lastX = input.menu.width - (int) (borderPadding / fx);
                    this.wrapChecker.mark();
                    warped = true;
                }
                else if (rawX >= w - border)
                {
                    Window.moveCursor(borderPadding, (int) mc.mouse.getY());

                    this.lastX = (int) (borderPadding / fx);
                    this.wrapChecker.mark();
                    warped = true;
                }

                if (rawY <= border)
                {
                    Window.moveCursor((int) mc.mouse.getX(), h - borderPadding);

                    this.lastY = input.menu.height - (int) (borderPadding / fy);
                    this.wrapChecker.mark();
                    warped = true;
                }
                else if (rawY >= h - border)
                {
                    Window.moveCursor((int) mc.mouse.getX(), borderPadding);

                    this.lastY = (int) (borderPadding / fy);
                    this.wrapChecker.mark();
                    warped = true;
                }
            }

            if (!warped)
            {
                int stepX = input.mouseX - this.lastX;
                int stepY = input.mouseY - this.lastY;
                this.accumDx += stepX;
                this.accumDy += stepY;
                this.lastX = input.mouseX;
                this.lastY = input.mouseY;
            }

            float factor = switch (this.mode)
            {
                case TRANSLATE -> 0.02F;
                case SCALE -> 0.01F;
                case ROTATE -> 0.3F;
            };

            float delta = this.accumDx * factor; /* Usamos movimiento horizontal para consistencia */

            Transform t = this.target.getTransform();

            if (this.mode == Mode.TRANSLATE)
            {
                float x = t.translate.x;
                float y = t.translate.y;
                float z = t.translate.z;

                if (this.activeAxis == Axis.X) x = this.dragStart.translate.x + delta;
                if (this.activeAxis == Axis.Y) y = this.dragStart.translate.y - this.accumDy * factor; /* Y hacia arriba */
                if (this.activeAxis == Axis.Z) z = this.dragStart.translate.z + delta;

                this.target.setT(null, x, y, z);
                this.target.setTransform(t);
            }
            else if (this.mode == Mode.SCALE)
            {
                float x = t.scale.x;
                float y = t.scale.y;
                float z = t.scale.z;

                if (this.activeAxis == Axis.X) x = clampScale(this.dragStart.scale.x + delta);
                if (this.activeAxis == Axis.Y) y = clampScale(this.dragStart.scale.y + delta);
                if (this.activeAxis == Axis.Z) z = clampScale(this.dragStart.scale.z + delta);

                this.target.setS(null, x, y, z);
                this.target.setTransform(t);
            }
            else if (this.mode == Mode.ROTATE)
            {
                /* Rotaciones en grados en UIPropTransform.setR */
                float rx = (float) Math.toDegrees(this.dragStart.rotate.x);
                float ry = (float) Math.toDegrees(this.dragStart.rotate.y);
                float rz = (float) Math.toDegrees(this.dragStart.rotate.z);

                if (this.activeAxis == Axis.X) rx = rx + delta * 10F;
                if (this.activeAxis == Axis.Y) ry = ry + delta * 10F;
                if (this.activeAxis == Axis.Z) rz = rz + delta * 10F;

                this.target.setR(null, rx, ry, rz);
                this.target.setTransform(t);
            }
        }

        this.lastMouseDown = mouseDown;
    }

    private float clampScale(float v)
    {
        return Math.max(0.001F, v);
    }

    /**
     * Renderizado 3D básico del gizmo directamente en el espacio del modelo.
     *
     * Se dibujan barras por eje y manejadores en los extremos:
     * - TRANSLATE: barras + pequeños cubos en extremos
     * - SCALE: barras + cubos más grandes en extremos
     * - ROTATE: tres anillos (XY, YZ, ZX) alrededor del pivote
     *
     * Nota: este método asume que el MatrixStack ya está multiplicado por
     * la matriz de origen del hueso/objeto.
     */
    public void render3D(MatrixStack stack)
    {
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        float length = 0.25F;     // longitud de cada eje
        float thickness = 0.01F;  // grosor de las barras

        // Barras por eje
        Draw.fillBoxTo(builder, stack, 0, 0, 0, length, 0, 0, thickness, 1F, 0F, 0F, 1F); // X
        Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, length, 0, thickness, 0F, 1F, 0F, 1F); // Y
        Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, 0, length, thickness, 0F, 0F, 1F, 1F); // Z

        // Manejadores en los extremos según el modo
        float cubeSmall = 0.03F;
        float cubeBig = 0.045F;

        if (this.mode == Mode.TRANSLATE)
        {
            drawEndCube(builder, stack, length, 0, 0, cubeSmall, 1F, 0F, 0F);
            drawEndCube(builder, stack, 0, length, 0, cubeSmall, 0F, 1F, 0F);
            drawEndCube(builder, stack, 0, 0, length, cubeSmall, 0F, 0F, 1F);
        }
        else if (this.mode == Mode.SCALE)
        {
            drawEndCube(builder, stack, length, 0, 0, cubeBig, 1F, 0F, 0F);
            drawEndCube(builder, stack, 0, length, 0, cubeBig, 0F, 1F, 0F);
            drawEndCube(builder, stack, 0, 0, length, cubeBig, 0F, 0F, 1F);
            // Cubo central (para dar referencia de pivote)
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall, 1F, 1F, 1F);
        }
        else if (this.mode == Mode.ROTATE)
        {
            float radius = 0.22F;
            drawRing3D(builder, stack, 'Z', radius, thickness, 1F, 0F, 0F); // XY
            drawRing3D(builder, stack, 'X', radius, thickness, 0F, 1F, 0F); // YZ
            drawRing3D(builder, stack, 'Y', radius, thickness, 0F, 0F, 1F); // ZX
        }

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableDepthTest();
        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.enableDepthTest();
    }

    private void drawEndCube(BufferBuilder builder, MatrixStack stack, float x, float y, float z, float s, float r, float g, float b)
    {
        stack.push();
        stack.translate(x, y, z);
        Draw.fillBox(builder, stack, -s, -s, -s, s, s, s, r, g, b, 1F);
        stack.pop();
    }

    /**
     * Dibuja un anillo formado por pequeños prismas a lo largo de un círculo.
     * axis: 'X' (plano YZ), 'Y' (plano ZX), 'Z' (plano XY)
     */
    private void drawRing3D(BufferBuilder builder, MatrixStack stack, char axis, float radius, float thickness, float r, float g, float b)
    {
        int segments = 64;
        double step = Math.PI * 2.0 / segments;

        float px1 = 0, py1 = 0, pz1 = 0;
        // punto inicial por eje
        switch (axis)
        {
            case 'Z':
                px1 = radius; py1 = 0; pz1 = 0; break;
            case 'X':
                px1 = 0; py1 = radius; pz1 = 0; break;
            case 'Y':
                px1 = radius; py1 = 0; pz1 = 0; break;
        }

        for (int i = 1; i <= segments; i++)
        {
            double ang = step * i;

            float px2, py2, pz2;
            if (axis == 'Z')
            {
                px2 = (float) (Math.cos(ang) * radius);
                py2 = (float) (Math.sin(ang) * radius);
                pz2 = 0F;
            }
            else if (axis == 'X')
            {
                px2 = 0F;
                py2 = (float) (Math.cos(ang) * radius);
                pz2 = (float) (Math.sin(ang) * radius);
            }
            else // 'Y'
            {
                px2 = (float) (Math.cos(ang) * radius);
                py2 = 0F;
                pz2 = (float) (Math.sin(ang) * radius);
            }

            Draw.fillBoxTo(builder, stack, px1, py1, pz1, px2, py2, pz2, thickness, r, g, b, 1F);

            px1 = px2; py1 = py2; pz1 = pz2;
        }
    }

    public void renderOverlay(UIRenderingContext context, Area viewport)
    {
        if (viewport == null)
        {
            return;
        }

        /* Asegurar que el overlay 2D no quede oculto por el depth buffer */
        RenderSystem.disableDepthTest();
        /* Evitar que un scissor activo recorte el overlay del gizmo */
        com.mojang.blaze3d.platform.GlStateManager._disableScissorTest();
        /* Forzar blending estándar para colores con alpha */
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        /* Indicador de estado */
        String label = switch (this.mode)
        {
            case TRANSLATE -> "Mover (T)";
            case ROTATE -> "Rotar (R)";
            case SCALE -> "Escalar (S)";
        };

        int lx = viewport.x + 10;
        int ly = viewport.y + 10;
        context.batcher.textCard(label, lx, ly, Colors.WHITE, Colors.A50);

        /* Depuración: dibujar un borde visible alrededor del viewport para confirmar overlay */
        int dbgColor = Colors.A50 | Colors.HIGHLIGHT;
        context.batcher.outline(viewport.x + 1, viewport.y + 1, viewport.ex() - 1, viewport.ey() - 1, dbgColor, 2);

        /* Si el gizmo 3D está habilitado, evitamos dibujar los manejadores 2D
         * para no crear inconsistencias visuales (las barras/cubos 3D son la
         * referencia principal). Mantengo etiqueta y borde de depuración. */
        if (BBSSettings.modelBlockGizmosEnabled.get())
        {
            // Centro del pivote como referencia mínima
            int cx3 = this.centerX;
            int cy3 = this.centerY;
            context.batcher.box(cx3 - 3, cy3 - 3, cx3 + 3, cy3 + 3, Colors.A50 | Colors.WHITE);

            RenderSystem.enableDepthTest();
            context.batcher.flush();
            return;
        }

        /* Render dependiendo del modo activo (modo 2D clásico) */
        int cx = this.centerX;
        int cy = this.centerY;
        float thickness = this.handleThickness;

        if (this.mode == Mode.TRANSLATE)
        {
            /* Líneas al pivote + píxeles en extremos para selección */
            int xColor = (Colors.A100 | Colors.RED);
            int yColor = (Colors.A100 | Colors.GREEN);
            int zColor = (Colors.A100 | Colors.BLUE);

            context.batcher.line(cx, cy, this.endXx, this.endXy, thickness, xColor);
            context.batcher.line(cx, cy, this.endYx, this.endYy, thickness, yColor);
            context.batcher.line(cx, cy, this.endZx, this.endZy, thickness, zColor);
            /* cabezas de flecha en cada handle */
            drawArrowHandle(context, cx, cy, this.endXx, this.endXy, xColor);
            drawArrowHandle(context, cx, cy, this.endYx, this.endYy, yColor);
            drawArrowHandle(context, cx, cy, this.endZx, this.endZy, zColor);
        }
        else if (this.mode == Mode.SCALE)
        {
            /* Líneas al pivote + cubos en extremos (squares con borde) */
            int xColor = (Colors.A100 | Colors.RED);
            int yColor = (Colors.A100 | Colors.GREEN);
            int zColor = (Colors.A100 | Colors.BLUE);

            context.batcher.line(cx, cy, this.endXx, this.endXy, thickness, xColor);
            context.batcher.line(cx, cy, this.endYx, this.endYy, thickness, yColor);
            context.batcher.line(cx, cy, this.endZx, this.endZy, thickness, zColor);

            /* cubos: cuadrados 8x8 con borde más claro */
            drawCubeHandle(context, this.endXx, this.endXy, xColor);
            drawCubeHandle(context, this.endYx, this.endYy, yColor);
            drawCubeHandle(context, this.endZx, this.endZy, zColor);
        }
        else if (this.mode == Mode.ROTATE)
        {
            /* Esferas lineales: tres anillos concéntricos alrededor del pivote */
            int xColor = (Colors.A100 | Colors.RED);
            int yColor = (Colors.A100 | Colors.GREEN);
            int zColor = (Colors.A100 | Colors.BLUE);

            drawRing(context, cx, cy, this.ringRX, thickness, xColor);
            drawRing(context, cx, cy, this.ringRY, thickness, yColor);
            drawRing(context, cx, cy, this.ringRZ, thickness, zColor);
        }

        /* Centro del pivote */
        context.batcher.box(cx - 3, cy - 3, cx + 3, cy + 3, Colors.A50 | Colors.WHITE);

        /* Consejos */
        String tip = switch (this.mode)
        {
            case TRANSLATE -> "Click izquierdo en eje para mover; T/R/S cambia modo.";
            case SCALE -> "Click en cubo para escalar; T/R/S cambia modo.";
            case ROTATE -> "Click en anillo para rotar; T/R/S cambia modo.";
        };
        int tw = context.batcher.getFont().getWidth(tip);
        context.batcher.textCard(tip, viewport.ex() - tw - 10, viewport.ey() - context.batcher.getFont().getHeight() - 10, Colors.WHITE, Colors.A50);

        /* Restaurar el depth test tras dibujar el overlay */
        RenderSystem.enableDepthTest();
        /* Hacer flush explícito en caso de estar fuera del ciclo normal de UI */
        context.batcher.flush();
    }

    /** Indica si el mouse está sobre algún handle del gizmo */
    public boolean isHoveringHandle()
    {
        return this.hoveredAxis != null;
    }

    private Axis detectHoveredAxis(int mx, int my)
    {
        if (this.mode == Mode.ROTATE)
        {
            /* Picking por distancia al anillo (radio +/- tolerancia) */
            int cx = this.centerX;
            int cy = this.centerY;
            double dx = mx - cx;
            double dy = my - cy;
            double d = Math.sqrt(dx * dx + dy * dy);
            int tol = Math.max(6, this.handleThickness * 2);

            if (Math.abs(d - this.ringRX) <= tol) return Axis.X;
            if (Math.abs(d - this.ringRY) <= tol) return Axis.Y;
            if (Math.abs(d - this.ringRZ) <= tol) return Axis.Z;
            return null;
        }

        /* Mover/Escalar: picking por proximidad a los endpoints */
        if (isNear(mx, my, endXx, endXy, hitRadius)) return Axis.X;
        if (isNear(mx, my, endYx, endYy, hitRadius)) return Axis.Y;
        if (isNear(mx, my, endZx, endZy, hitRadius)) return Axis.Z;

        return null;
    }

    private void drawRing(UIRenderingContext context, int cx, int cy, int radius, float thickness, int color)
    {
        int segments = 64;
        double step = Math.PI * 2.0 / segments;

        float x1 = cx + radius;
        float y1 = cy;
        for (int i = 1; i <= segments; i++)
        {
            double ang = step * i;
            float x2 = (float) (cx + Math.cos(ang) * radius);
            float y2 = (float) (cy + Math.sin(ang) * radius);
            context.batcher.line(x1, y1, x2, y2, thickness, color);
            x1 = x2;
            y1 = y2;
        }
    }

    private void drawCubeHandle(UIRenderingContext context, int x, int y, int color)
    {
        int size = 12;
        int half = size / 2;
        int fill = Colors.mulRGB(color, 0.8F);
        int border = Colors.setA(Colors.WHITE, 0.75F);

        context.batcher.box(x - half, y - half, x + half, y + half, fill);
        context.batcher.outline(x - half, y - half, x + half, y + half, border, 1);
    }

    /** Dibuja una cabeza de flecha orientada hacia el endpoint del eje */
    private void drawArrowHandle(UIRenderingContext context, int cx, int cy, int ex, int ey, int color)
    {
        float dx = ex - cx;
        float dy = ey - cy;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.0001F) return;

        float ndx = dx / len;
        float ndy = dy / len;

        /* Tamaño de la flecha: un poco mayor que el grosor de línea */
        float headLen = 16F;
        float headWidth = 12F;

        float bx = ex - ndx * headLen;
        float by = ey - ndy * headLen;

        /* Perpendicular para los lados de la flecha */
        float px = -ndy;
        float py = ndx;

        float lx = bx + px * (headWidth / 2F);
        float ly = by + py * (headWidth / 2F);
        float rx = bx - px * (headWidth / 2F);
        float ry = by - py * (headWidth / 2F);

        context.batcher.line(ex, ey, lx, ly, this.handleThickness + 1, color);
        context.batcher.line(ex, ey, rx, ry, this.handleThickness + 1, color);
    }

    private boolean isNear(int mx, int my, int x, int y, int r)
    {
        int dx = mx - x;
        int dy = my - y;
        return dx * dx + dy * dy <= r * r;
    }

    public void cycleMode(boolean forward)
    {
        Mode[] values = Mode.values();
        int i = this.mode.ordinal();
        i = (i + (forward ? 1 : -1) + values.length) % values.length;
        this.mode = values[i];
    }
}