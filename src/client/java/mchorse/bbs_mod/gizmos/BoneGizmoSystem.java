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
import org.joml.Vector3f;
import org.joml.Vector4f;
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
            // Cuando el gizmo 3D está activo, usar picking 3D por rayos
            if (BBSSettings.modelBlockGizmosEnabled.get())
            {
                this.hoveredAxis = detectHoveredAxis3D(input, viewport, origin, projection, view);

                // El centro en pantalla para overlays mínimos (etiqueta/pivote)
                Matrix4f mvp = new Matrix4f(projection).mul(view).mul(origin);
                org.joml.Vector4f cp = new org.joml.Vector4f(0, 0, 0, 1);
                mvp.transform(cp);
                if (cp.w != 0)
                {
                    float ndcXc = cp.x / cp.w;
                    float ndcYc = cp.y / cp.w;
                    this.centerX = viewport.x + (int) (((ndcXc + 1F) * 0.5F) * viewport.w);
                    this.centerY = viewport.y + (int) (((-ndcYc + 1F) * 0.5F) * viewport.h);
                }

                // Iniciar/terminar arrastre basado en 3D hover
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

                if (this.dragging && !mouseDown && this.lastMouseDown)
                {
                    this.dragging = false;
                    this.activeAxis = null;
                }

                if (this.dragging && this.target != null && this.target.getTransform() != null)
                {
                    // Delta de mouse similar al modo 2D
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

                    float delta = this.accumDx * factor;
                    Transform t = this.target.getTransform();

                    if (this.mode == Mode.TRANSLATE)
                    {
                        float x = t.translate.x;
                        float y = t.translate.y;
                        float z = t.translate.z;

                        if (this.activeAxis == Axis.X) x = this.dragStart.translate.x + delta;
                        if (this.activeAxis == Axis.Y) y = this.dragStart.translate.y - this.accumDy * factor;
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

                this.lastMouseDown = Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT);
                return;
            }

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
        // Importante: el stack que llega aquí ya está multiplicado por la
        // matriz de origen del hueso (sin escala). Esa matriz incluye la rotación
        // acumulada del hueso respecto a sus padres; por lo tanto, NO aplicamos
        // rotación adicional del transform local aquí para evitar invertir o
        // duplicar la orientación del gizmo.

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        float length = 0.25F;     // longitud de cada eje
        float thickness = 0.02F;  // grosor de las barras (similar a coolerAxes)
        float outlinePad = 0.01F; // pad para contorno negro tipo coolerAxes
        float slabThick = 0.018F; // grosor de las losas de escala (a lo largo del eje)

        // Ajuste dinámico para asegurar que la barra toque el cubo en el extremo.
        // Usamos el tamaño del cubo del extremo + el grosor de la barra para
        // evitar gaps en perspectiva o por redondeos.
        float cubeSmall = 0.022F;
        float cubeBig = 0.045F;
        // Ajuste por modo: en TRANSLATE conectamos a la base de la flecha;
        // en SCALE nos internamos en el cubo para evitar gaps visuales.
        float connectFudge = (this.mode == Mode.TRANSLATE)
            ? 0.03F
            : (this.mode == Mode.SCALE ? slabThick : (cubeBig + thickness));

        /* Mostrar solo el eje activo durante el arrastre, como en DCCs */
        boolean showX = !this.dragging || this.activeAxis == Axis.X;
        boolean showY = !this.dragging || this.activeAxis == Axis.Y;
        boolean showZ = !this.dragging || this.activeAxis == Axis.Z;

        // Barras base por eje SOLO para ESCALAR
        // En TRASLADAR dibujamos barras específicas que llegan a los conos,
        // para evitar que sobrepasen.
        if (this.mode == Mode.SCALE)
        {
            // Resultado: X=rojo, Y=verde, Z=azul
            // Contorno negro detrás (ligeramente más grueso) para simular borde
            if (showX)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, length + connectFudge, 0, 0, thickness + outlinePad, 0F, 0F, 0F, 1F); // X outline
                Draw.fillBoxTo(builder, stack, 0, 0, 0, length + connectFudge, 0, 0, thickness, 1F, 0F, 0F, 1F); // X -> rojo
            }
            if (showY)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, length + connectFudge, 0, thickness + outlinePad, 0F, 0F, 0F, 1F); // Y outline
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, length + connectFudge, 0, thickness, 0F, 1F, 0F, 1F); // Y -> verde
            }
            if (showZ)
            {
                // Z -> azul (usar caja alineada para evitar inversión/offset)
                Draw.fillBox(builder, stack, -(thickness + outlinePad) / 2F, -(thickness + outlinePad) / 2F, 0F, (thickness + outlinePad) / 2F, (thickness + outlinePad) / 2F, length + connectFudge, 0F, 0F, 0F, 1F); // Z outline
                Draw.fillBox(builder, stack, -thickness / 2F, -thickness / 2F, 0F, thickness / 2F, thickness / 2F, length + connectFudge, 0F, 0F, 1F, 1F);
            }
        }

        // Manejadores en los extremos según el modo

        if (this.mode == Mode.TRANSLATE)
        {
            // Conos en las puntas de cada eje (estilo DCCs)
            float headLen = 0.08F;       // altura del cono (más grueso)
            float headWidth = 0.06F;     // diámetro aproximado de la base (más grueso)
            float headRadius = headWidth * 0.5F;

            // Igualar longitud visual de barras con ESCALAR y ajustar conos al nuevo extremo
            float lengthBar = length + connectFudge;   // mismo alcance visual que SCALE
            // Las barras deben llegar hasta la base del cono, dejando un pequeño margen
            float barEnd = lengthBar - headLen - 0.002F;

            boolean hx = (this.hoveredAxis == Axis.X);
            boolean hy = (this.hoveredAxis == Axis.Y);
            boolean hz = (this.hoveredAxis == Axis.Z);

            float txX = hx ? thickness * 1.5F : thickness;
            float txY = hy ? thickness * 1.5F : thickness;
            float txZ = hz ? thickness * 1.5F : thickness;

            // Contorno negro detrás de cada barra (ligeramente más grueso)
            if (showX)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, barEnd, 0, 0, txX + outlinePad, 0F, 0F, 0F, 1F); // X outline
                Draw.fillBoxTo(builder, stack, 0, 0, 0, barEnd, 0, 0, txX, 1F, 0F, 0F, 1F); // X -> rojo
            }
            if (showY)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, barEnd, 0, txY + outlinePad, 0F, 0F, 0F, 1F); // Y outline
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, barEnd, 0, txY, 0F, 1F, 0F, 1F); // Y -> verde
            }
            if (showZ)
            {
                Draw.fillBox(builder, stack, -(txZ + outlinePad) / 2F, -(txZ + outlinePad) / 2F, 0F, (txZ + outlinePad) / 2F, (txZ + outlinePad) / 2F, barEnd, 0F, 0F, 0F, 1F); // Z outline
                // Z -> azul (usar caja alineada para evitar inversión del eje)
                Draw.fillBox(builder, stack, -txZ / 2F, -txZ / 2F, 0F, txZ / 2F, txZ / 2F, barEnd, 0F, 0F, 1F, 1F);
            }

            // Conos en las puntas (con contorno negro ligeramente más grande)
            if (showX)
            {
                drawCone3D(builder, stack, 'X', lengthBar, headLen + outlinePad * 0.5F, headRadius + outlinePad, 0F, 0F, 0F, 1F); // contorno
                drawCone3D(builder, stack, 'X', lengthBar, headLen, headRadius, 1F, 0F, 0F, 1F);
            }
            if (showY)
            {
                drawCone3D(builder, stack, 'Y', lengthBar, headLen + outlinePad * 0.5F, headRadius + outlinePad, 0F, 0F, 0F, 1F); // contorno
                drawCone3D(builder, stack, 'Y', lengthBar, headLen, headRadius, 0F, 1F, 0F, 1F);
            }
            if (showZ)
            {
                drawCone3D(builder, stack, 'Z', lengthBar, headLen + outlinePad * 0.5F, headRadius + outlinePad, 0F, 0F, 0F, 1F); // contorno
                drawCone3D(builder, stack, 'Z', lengthBar, headLen, headRadius, 0F, 0F, 1F, 1F);
            }

            // Cubo de pivote en el origen como referencia (con contorno negro)
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall + outlinePad, 0F, 0F, 0F);
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall, 1F, 1F, 1F);

            // Halo suave en el eje hovered
            if (hx && showX)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, barEnd, 0, 0, thickness * 2F, 1F, 1F, 1F, 0.25F);
                drawCone3D(builder, stack, 'X', lengthBar, headLen, headRadius, 1F, 1F, 1F, 0.35F);
            }
            if (hy && showY)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, barEnd, 0, thickness * 2F, 1F, 1F, 1F, 0.25F);
                drawCone3D(builder, stack, 'Y', lengthBar, headLen, headRadius, 1F, 1F, 1F, 0.35F);
            }
            if (hz && showZ)
            {
                Draw.fillBox(builder, stack, -thickness, -thickness, 0F, thickness, thickness, barEnd, 1F, 1F, 1F, 0.25F);
                drawCone3D(builder, stack, 'Z', lengthBar, headLen, headRadius, 1F, 1F, 1F, 0.35F);
            }
        }
        else if (this.mode == Mode.SCALE)
        {
            boolean hx = (this.hoveredAxis == Axis.X);
            boolean hy = (this.hoveredAxis == Axis.Y);
            boolean hz = (this.hoveredAxis == Axis.Z);
            // Barras base ya están dibujadas arriba para SCALE.
            // Losas de extremo en cada eje (planos perpendiculares al eje)
            // X (rojo)
            if (showX)
            {
                stack.push();
                stack.translate(length, 0F, 0F);
                // contorno negro (ligeramente más grande)
                Draw.fillBox(builder, stack, -(slabThick + outlinePad), -(cubeBig + outlinePad), -(cubeBig + outlinePad), (slabThick + outlinePad), (cubeBig + outlinePad), (cubeBig + outlinePad), 0F, 0F, 0F, 1F);
                Draw.fillBox(builder, stack, -slabThick, -cubeBig, -cubeBig, slabThick, cubeBig, cubeBig, 1F, 0F, 0F, 1F);
                stack.pop();
            }
            // Y (verde)
            if (showY)
            {
                stack.push();
                stack.translate(0F, length, 0F);
                // contorno negro (ligeramente más grande)
                Draw.fillBox(builder, stack, -(cubeBig + outlinePad), -(slabThick + outlinePad), -(cubeBig + outlinePad), (cubeBig + outlinePad), (slabThick + outlinePad), (cubeBig + outlinePad), 0F, 0F, 0F, 1F);
                Draw.fillBox(builder, stack, -cubeBig, -slabThick, -cubeBig, cubeBig, slabThick, cubeBig, 0F, 1F, 0F, 1F);
                stack.pop();
            }
            // Z (azul)
            if (showZ)
            {
                stack.push();
                stack.translate(0F, 0F, length);
                // contorno negro (ligeramente más grande)
                Draw.fillBox(builder, stack, -(cubeBig + outlinePad), -(cubeBig + outlinePad), -(slabThick + outlinePad), (cubeBig + outlinePad), (cubeBig + outlinePad), (slabThick + outlinePad), 0F, 0F, 0F, 1F);
                Draw.fillBox(builder, stack, -cubeBig, -cubeBig, -slabThick, cubeBig, cubeBig, slabThick, 0F, 0F, 1F, 1F);
                stack.pop();
            }

            // Halo suave alrededor de la losa hovered (ligeramente más grande)
            if (hx && showX)
            {
                stack.push();
                stack.translate(length, 0, 0);
                Draw.fillBox(builder, stack, -(slabThick + 0.006F), -(cubeBig + 0.01F), -(cubeBig + 0.01F), (slabThick + 0.006F), (cubeBig + 0.01F), (cubeBig + 0.01F), 1F, 1F, 1F, 0.30F);
                stack.pop();
                // Resaltar también la barra X
                Draw.fillBoxTo(builder, stack, 0, 0, 0, length + connectFudge, 0, 0, thickness * 1.6F, 1F, 1F, 1F, 0.25F);
            }
            if (hy && showY)
            {
                stack.push();
                stack.translate(0, length, 0);
                Draw.fillBox(builder, stack, -(cubeBig + 0.01F), -(slabThick + 0.006F), -(cubeBig + 0.01F), (cubeBig + 0.01F), (slabThick + 0.006F), (cubeBig + 0.01F), 1F, 1F, 1F, 0.30F);
                stack.pop();
                // Resaltar también la barra Y
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, length + connectFudge, 0, thickness * 1.6F, 1F, 1F, 1F, 0.25F);
            }
            if (hz && showZ)
            {
                stack.push();
                stack.translate(0, 0, length);
                Draw.fillBox(builder, stack, -(cubeBig + 0.01F), -(cubeBig + 0.01F), -(slabThick + 0.006F), (cubeBig + 0.01F), (cubeBig + 0.01F), (slabThick + 0.006F), 1F, 1F, 1F, 0.30F);
                stack.pop();
                // Resaltar también la barra Z (alineada)
                Draw.fillBox(builder, stack, -thickness, -thickness, 0F, thickness, thickness, length + connectFudge, 1F, 1F, 1F, 0.25F);
            }

            // Cubo central (referencia de pivote) con contorno negro
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall + outlinePad, 0F, 0F, 0F);
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall, 1F, 1F, 1F);
        }
        else if (this.mode == Mode.ROTATE)
        {
            float radius = 0.22F;
            float sweep = 360F;
            float offZ = 0F;
            float offX = 0F;
            float offY = 0F;

            boolean hx = (this.hoveredAxis == Axis.X);
            boolean hy = (this.hoveredAxis == Axis.Y);
            boolean hz = (this.hoveredAxis == Axis.Z);

            /* Asegurar visibilidad por ambos lados del anillo: desactivar culling temporalmente */
            RenderSystem.disableCull();

            /* Cubo de pivote con contorno negro */
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall + outlinePad, 0F, 0F, 0F);
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall, 1F, 1F, 1F);

            // Anillos alrededor del pivote (Z, X, Y), ocultando los no activos durante arrastre
            if (showZ)
            {
                drawRingArc3D(builder, stack, 'Z', radius, thickness + outlinePad, 0F, 0F, 0F, offZ, sweep, false);
                drawRingArc3D(builder, stack, 'Z', radius, thickness, 0F, 0F, 1F, offZ, sweep, hz);
            }
            if (showX)
            {
                drawRingArc3D(builder, stack, 'X', radius, thickness + outlinePad, 0F, 0F, 0F, offX, sweep, false);
                drawRingArc3D(builder, stack, 'X', radius, thickness, 1F, 0F, 0F, offX, sweep, hx);
            }
            if (showY)
            {
                drawRingArc3D(builder, stack, 'Y', radius, thickness + outlinePad, 0F, 0F, 0F, offY, sweep, false);
                drawRingArc3D(builder, stack, 'Y', radius, thickness, 0F, 1F, 0F, offY, sweep, hy);
            }
        }

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableDepthTest();
        /* En rotación, reactivar culling después de dibujar */
        BufferRenderer.drawWithGlobalProgram(builder.end());
        if (this.mode == Mode.ROTATE) {
            RenderSystem.enableCull();
        }
        RenderSystem.enableDepthTest();

        // No se modifica el stack adicionalmente: la orientación del gizmo
        // depende únicamente de la matriz de origen aplicada antes de esta llamada.
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
    private void drawRingArc3D(BufferBuilder builder, MatrixStack stack, char axis, float radius, float thickness, float r, float g, float b, float startDeg, float sweepDeg, boolean highlight)
    {
        // Renderizar un toro (tubo) alrededor del pivote en el plano correspondiente.
        // radius: radio mayor del anillo, thickness: ancho visual equivalente al de barras.
        int segU = 96;           // segmentos a lo largo del anillo
        int segV = 24;           // segmentos alrededor de la sección del tubo
        double u0 = Math.toRadians(startDeg);
        double uStep = Math.toRadians(sweepDeg / (double) segU);
        double vStep = Math.PI * 2.0 / (double) segV;

        float tubeR = thickness * 0.5F; // usar la mitad como radio del tubo para igualar barras
        Matrix4f mat = stack.peek().getPositionMatrix();

        // Dibujar superficie del toro mediante quads triangulados
        for (int iu = 0; iu < segU; iu++)
        {
            double u1 = u0 + uStep * iu;
            double u2 = u0 + uStep * (iu + 1);

            for (int iv = 0; iv < segV; iv++)
            {
                double v1 = vStep * iv;
                double v2 = vStep * (iv + 1);

                float x11, y11, z11; // (u1, v1)
                float x12, y12, z12; // (u1, v2)
                float x21, y21, z21; // (u2, v1)
                float x22, y22, z22; // (u2, v2)

                if (axis == 'Z')
                {
                    x11 = (float) ((radius + tubeR * Math.cos(v1)) * Math.cos(u1));
                    y11 = (float) ((radius + tubeR * Math.cos(v1)) * Math.sin(u1));
                    z11 = (float) (tubeR * Math.sin(v1));

                    x12 = (float) ((radius + tubeR * Math.cos(v2)) * Math.cos(u1));
                    y12 = (float) ((radius + tubeR * Math.cos(v2)) * Math.sin(u1));
                    z12 = (float) (tubeR * Math.sin(v2));

                    x21 = (float) ((radius + tubeR * Math.cos(v1)) * Math.cos(u2));
                    y21 = (float) ((radius + tubeR * Math.cos(v1)) * Math.sin(u2));
                    z21 = (float) (tubeR * Math.sin(v1));

                    x22 = (float) ((radius + tubeR * Math.cos(v2)) * Math.cos(u2));
                    y22 = (float) ((radius + tubeR * Math.cos(v2)) * Math.sin(u2));
                    z22 = (float) (tubeR * Math.sin(v2));
                }
                else if (axis == 'X')
                {
                    y11 = (float) ((radius + tubeR * Math.cos(v1)) * Math.cos(u1));
                    z11 = (float) ((radius + tubeR * Math.cos(v1)) * Math.sin(u1));
                    x11 = (float) (tubeR * Math.sin(v1));

                    y12 = (float) ((radius + tubeR * Math.cos(v2)) * Math.cos(u1));
                    z12 = (float) ((radius + tubeR * Math.cos(v2)) * Math.sin(u1));
                    x12 = (float) (tubeR * Math.sin(v2));

                    y21 = (float) ((radius + tubeR * Math.cos(v1)) * Math.cos(u2));
                    z21 = (float) ((radius + tubeR * Math.cos(v1)) * Math.sin(u2));
                    x21 = (float) (tubeR * Math.sin(v1));

                    y22 = (float) ((radius + tubeR * Math.cos(v2)) * Math.cos(u2));
                    z22 = (float) ((radius + tubeR * Math.cos(v2)) * Math.sin(u2));
                    x22 = (float) (tubeR * Math.sin(v2));
                }
                else // 'Y'
                {
                    x11 = (float) ((radius + tubeR * Math.cos(v1)) * Math.cos(u1));
                    z11 = (float) ((radius + tubeR * Math.cos(v1)) * Math.sin(u1));
                    y11 = (float) (tubeR * Math.sin(v1));

                    x12 = (float) ((radius + tubeR * Math.cos(v2)) * Math.cos(u1));
                    z12 = (float) ((radius + tubeR * Math.cos(v2)) * Math.sin(u1));
                    y12 = (float) (tubeR * Math.sin(v2));

                    x21 = (float) ((radius + tubeR * Math.cos(v1)) * Math.cos(u2));
                    z21 = (float) ((radius + tubeR * Math.cos(v1)) * Math.sin(u2));
                    y21 = (float) (tubeR * Math.sin(v1));

                    x22 = (float) ((radius + tubeR * Math.cos(v2)) * Math.cos(u2));
                    z22 = (float) ((radius + tubeR * Math.cos(v2)) * Math.sin(u2));
                    y22 = (float) (tubeR * Math.sin(v2));
                }

                // Triángulos del quad
                builder.vertex(mat, x11, y11, z11).color(r, g, b, 1F).next();
                builder.vertex(mat, x12, y12, z12).color(r, g, b, 1F).next();
                builder.vertex(mat, x22, y22, z22).color(r, g, b, 1F).next();

                builder.vertex(mat, x11, y11, z11).color(r, g, b, 1F).next();
                builder.vertex(mat, x22, y22, z22).color(r, g, b, 1F).next();
                builder.vertex(mat, x21, y21, z21).color(r, g, b, 1F).next();
            }
        }

        // Halo suave opcional cuando está en hover: un toro blanco más fino
        if (highlight)
        {
            float hr = tubeR * 0.6F;
            float ha = 0.35F;
            for (int iu = 0; iu < segU; iu++)
            {
                double u1 = u0 + uStep * iu;
                double u2 = u0 + uStep * (iu + 1);
                for (int iv = 0; iv < segV; iv++)
                {
                    double v1 = vStep * iv;
                    double v2 = vStep * (iv + 1);

                    float x11, y11, z11;
                    float x12, y12, z12;
                    float x21, y21, z21;
                    float x22, y22, z22;

                    if (axis == 'Z')
                    {
                        x11 = (float) ((radius + hr * Math.cos(v1)) * Math.cos(u1));
                        y11 = (float) ((radius + hr * Math.cos(v1)) * Math.sin(u1));
                        z11 = (float) (hr * Math.sin(v1));

                        x12 = (float) ((radius + hr * Math.cos(v2)) * Math.cos(u1));
                        y12 = (float) ((radius + hr * Math.cos(v2)) * Math.sin(u1));
                        z12 = (float) (hr * Math.sin(v2));

                        x21 = (float) ((radius + hr * Math.cos(v1)) * Math.cos(u2));
                        y21 = (float) ((radius + hr * Math.cos(v1)) * Math.sin(u2));
                        z21 = (float) (hr * Math.sin(v1));

                        x22 = (float) ((radius + hr * Math.cos(v2)) * Math.cos(u2));
                        y22 = (float) ((radius + hr * Math.cos(v2)) * Math.sin(u2));
                        z22 = (float) (hr * Math.sin(v2));
                    }
                    else if (axis == 'X')
                    {
                        y11 = (float) ((radius + hr * Math.cos(v1)) * Math.cos(u1));
                        z11 = (float) ((radius + hr * Math.cos(v1)) * Math.sin(u1));
                        x11 = (float) (hr * Math.sin(v1));

                        y12 = (float) ((radius + hr * Math.cos(v2)) * Math.cos(u1));
                        z12 = (float) ((radius + hr * Math.cos(v2)) * Math.sin(u1));
                        x12 = (float) (hr * Math.sin(v2));

                        y21 = (float) ((radius + hr * Math.cos(v1)) * Math.cos(u2));
                        z21 = (float) ((radius + hr * Math.cos(v1)) * Math.sin(u2));
                        x21 = (float) (hr * Math.sin(v1));

                        y22 = (float) ((radius + hr * Math.cos(v2)) * Math.cos(u2));
                        z22 = (float) ((radius + hr * Math.cos(v2)) * Math.sin(u2));
                        x22 = (float) (hr * Math.sin(v2));
                    }
                    else // 'Y'
                    {
                        x11 = (float) ((radius + hr * Math.cos(v1)) * Math.cos(u1));
                        z11 = (float) ((radius + hr * Math.cos(v1)) * Math.sin(u1));
                        y11 = (float) (hr * Math.sin(v1));

                        x12 = (float) ((radius + hr * Math.cos(v2)) * Math.cos(u1));
                        z12 = (float) ((radius + hr * Math.cos(v2)) * Math.sin(u1));
                        y12 = (float) (hr * Math.sin(v2));

                        x21 = (float) ((radius + hr * Math.cos(v1)) * Math.cos(u2));
                        z21 = (float) ((radius + hr * Math.cos(v1)) * Math.sin(u2));
                        y21 = (float) (hr * Math.sin(v1));

                        x22 = (float) ((radius + hr * Math.cos(v2)) * Math.cos(u2));
                        z22 = (float) ((radius + hr * Math.cos(v2)) * Math.sin(u2));
                        y22 = (float) (hr * Math.sin(v2));
                    }

                    builder.vertex(mat, x11, y11, z11).color(1F, 1F, 1F, ha).next();
                    builder.vertex(mat, x12, y12, z12).color(1F, 1F, 1F, ha).next();
                    builder.vertex(mat, x22, y22, z22).color(1F, 1F, 1F, ha).next();

                    builder.vertex(mat, x11, y11, z11).color(1F, 1F, 1F, ha).next();
                    builder.vertex(mat, x22, y22, z22).color(1F, 1F, 1F, ha).next();
                    builder.vertex(mat, x21, y21, z21).color(1F, 1F, 1F, ha).next();
                }
            }
        }
    }

    /**
     * Puntas tipo flecha usando dos prismas inclinados que forman una "V".
     * Se dibujan en el plano perpendicular a cada eje.
     */
    private void drawArrowHead3D(BufferBuilder builder, MatrixStack stack, char axis, float baseLen, float headLen, float headWidth, float thickness, float r, float g, float b, float a)
    {
        if (axis == 'X')
        {
            Draw.fillBoxTo(builder, stack, baseLen, 0, 0, baseLen - headLen, +headWidth / 2F, 0, thickness + 0.004F, r, g, b, a);
            Draw.fillBoxTo(builder, stack, baseLen, 0, 0, baseLen - headLen, -headWidth / 2F, 0, thickness + 0.004F, r, g, b, a);
        }
        else if (axis == 'Y')
        {
            Draw.fillBoxTo(builder, stack, 0, baseLen, 0, +headWidth / 2F, baseLen - headLen, 0, thickness + 0.004F, r, g, b, a);
            Draw.fillBoxTo(builder, stack, 0, baseLen, 0, -headWidth / 2F, baseLen - headLen, 0, thickness + 0.004F, r, g, b, a);
        }
        else // 'Z'
        {
            Draw.fillBoxTo(builder, stack, 0, 0, baseLen, 0, +headWidth / 2F, baseLen - headLen, thickness + 0.004F, r, g, b, a);
            Draw.fillBoxTo(builder, stack, 0, 0, baseLen, 0, -headWidth / 2F, baseLen - headLen, thickness + 0.004F, r, g, b, a);
        }
    }

    /**
     * Cono 3D orientado a lo largo de un eje positivo.
     * baseLen: posición de la punta; headLen: altura del cono; radius: radio de la base.
     */
    private void drawCone3D(BufferBuilder builder, MatrixStack stack, char axis, float baseLen, float headLen, float radius, float r, float g, float b, float a)
    {
        Matrix4f mat = stack.peek().getPositionMatrix();
        int segments = 20;

        float ax, ay, az; // ápice
        float bx, by, bz; // centro de la base

        if (axis == 'X')
        {
            ax = baseLen; ay = 0F; az = 0F;
            bx = baseLen - headLen; by = 0F; bz = 0F;
        }
        else if (axis == 'Y')
        {
            ax = 0F; ay = baseLen; az = 0F;
            bx = 0F; by = baseLen - headLen; bz = 0F;
        }
        else // 'Z'
        {
            ax = 0F; ay = 0F; az = baseLen;
            bx = 0F; by = 0F; bz = baseLen - headLen;
        }

        for (int i = 0; i < segments; i++)
        {
            double a1 = (Math.PI * 2.0) * (double) i / (double) segments;
            double a2 = (Math.PI * 2.0) * (double) (i + 1) / (double) segments;

            float x1, y1, z1;
            float x2, y2, z2;

            if (axis == 'X')
            {
                x1 = bx;
                y1 = (float) (Math.cos(a1) * radius);
                z1 = (float) (Math.sin(a1) * radius);

                x2 = bx;
                y2 = (float) (Math.cos(a2) * radius);
                z2 = (float) (Math.sin(a2) * radius);
            }
            else if (axis == 'Y')
            {
                x1 = (float) (Math.cos(a1) * radius);
                y1 = by;
                z1 = (float) (Math.sin(a1) * radius);

                x2 = (float) (Math.cos(a2) * radius);
                y2 = by;
                z2 = (float) (Math.sin(a2) * radius);
            }
            else // 'Z'
            {
                x1 = (float) (Math.cos(a1) * radius);
                y1 = (float) (Math.sin(a1) * radius);
                z1 = bz;

                x2 = (float) (Math.cos(a2) * radius);
                y2 = (float) (Math.sin(a2) * radius);
                z2 = bz;
            }

            // Cara lateral: ápice -> p1 -> p2
            builder.vertex(mat, ax, ay, az).color(r, g, b, a).next();
            builder.vertex(mat, x1, y1, z1).color(r, g, b, a).next();
            builder.vertex(mat, x2, y2, z2).color(r, g, b, a).next();

            // Disco de base (opcional, ligeramente más transparente)
            float aa = Math.max(0F, a - 0.2F);
            builder.vertex(mat, bx, by, bz).color(r, g, b, aa).next();
            builder.vertex(mat, x2, y2, z2).color(r, g, b, aa).next();
            builder.vertex(mat, x1, y1, z1).color(r, g, b, aa).next();
        }
    }

    /**
     * Detección de eje hovered en 3D usando ray casting hacia cajas alineadas por eje.
     */
    private Axis detectHoveredAxis3D(UIContext input, Area viewport, Matrix4f origin, Matrix4f projection, Matrix4f view)
    {
        if (origin == null || projection == null || view == null) return null;

        // Convertir coordenadas del mouse a NDC
        float nx = (float) (((input.mouseX - viewport.x) / (double) viewport.w) * 2.0 - 1.0);
        float ny = (float) ( -(((input.mouseY - viewport.y) / (double) viewport.h) * 2.0 - 1.0) );

        // Calcular puntos near/far en mundo
        Matrix4f invPV = new Matrix4f(projection).mul(view).invert(new Matrix4f());
        Vector4f nearClip = new Vector4f(nx, ny, -1F, 1F);
        Vector4f farClip  = new Vector4f(nx, ny,  1F, 1F);

        Vector4f nearWorld = invPV.transform(new Vector4f(nearClip));
        Vector4f farWorld  = invPV.transform(new Vector4f(farClip));
        nearWorld.div(nearWorld.w);
        farWorld.div(farWorld.w);

        // Transformar el rayo a espacio local del origen
        Matrix4f invOrigin = new Matrix4f(origin).invert(new Matrix4f());
        Vector4f nearLocal4 = invOrigin.transform(new Vector4f(nearWorld));
        Vector4f farLocal4  = invOrigin.transform(new Vector4f(farWorld));
        Vector3f rayO = new Vector3f(nearLocal4.x, nearLocal4.y, nearLocal4.z);
        Vector3f rayF = new Vector3f(farLocal4.x, farLocal4.y, farLocal4.z);
        Vector3f rayD = rayF.sub(rayO, new Vector3f());
        rayD.normalize();
        // Importante: el rayo ya está en el espacio local del origen (incluye
        // la rotación acumulada del hueso). No aplicamos rotaciones adicionales
        // del transform local para evitar discrepancias entre render y picking.

        // Si estamos en rotación, delegar al método de picking por anillo
        if (this.mode == Mode.ROTATE)
        {
            return detectHoveredAxis3DRotate(rayO, rayD);
        }

        // Definir AABB por eje (mover/escalar)
        float length = 0.25F;
        // En escala el cubo del extremo es más grande; ampliamos sección transversal del AABB
        float thickness = (this.mode == Mode.SCALE) ? 0.045F : 0.015F;
        float fudge = (this.mode == Mode.TRANSLATE) ? 0.06F : ((this.mode == Mode.SCALE) ? 0.045F : 0.02F);

        float[] tx = rayBoxIntersect(rayO, rayD, new Vector3f(0F, -thickness/2F, -thickness/2F), new Vector3f(length + fudge, thickness/2F, thickness/2F));
        float[] ty = rayBoxIntersect(rayO, rayD, new Vector3f(-thickness/2F, 0F, -thickness/2F), new Vector3f(thickness/2F, length + fudge, thickness/2F));
        float[] tz = rayBoxIntersect(rayO, rayD, new Vector3f(-thickness/2F, -thickness/2F, 0F), new Vector3f(thickness/2F, thickness/2F, length + fudge));

        float bestT = Float.POSITIVE_INFINITY;
        Axis best = null;

        if (tx != null && tx[0] >= 0 && tx[0] < bestT) { bestT = tx[0]; best = Axis.X; }
        if (ty != null && ty[0] >= 0 && ty[0] < bestT) { bestT = ty[0]; best = Axis.Y; }
        if (tz != null && tz[0] >= 0 && tz[0] < bestT) { bestT = tz[0]; best = Axis.Z; }

        return best;
    }

    /** Picking 3D para el gizmo de rotación: intersección rayo-plano y banda alrededor del radio. */
    private Axis detectHoveredAxis3DRotate(Vector3f rayO, Vector3f rayD)
    {
        float radius = 0.22F;
        float thickness = 0.01F;        // radio del tubo (mitad del grosor de barras)
        float band = thickness * 0.95F; // tolerancia radial cercana al radio del tubo

        class Hit { Axis a; float t; }
        Hit hitBest = null;

        // Comprobación auxiliar
        java.util.function.BiFunction<Vector3f, Character, Hit> check = (n, c) -> {
            float denom = n.x * rayD.x + n.y * rayD.y + n.z * rayD.z;
            float t;
            float ix, iy, iz;

            if (Math.abs(denom) < 1e-5)
            {
                // Fallback: el rayo es casi paralelo al plano. Buscamos el punto
                // sobre el rayo que minimiza la distancia radial en el par de ejes
                // correspondiente al anillo.
                if (c == 'Z')
                {
                    float d2 = rayD.x * rayD.x + rayD.y * rayD.y;
                    if (d2 > 1e-8)
                    {
                        t = - (rayO.x * rayD.x + rayO.y * rayD.y) / d2;
                        if (t < 0) t = 0; // limitar a frente del rayo
                    }
                    else
                    {
                        t = 0; // dirección sin componente XY, usar origen
                    }
                    ix = rayO.x + rayD.x * t;
                    iy = rayO.y + rayD.y * t;
                    iz = rayO.z + rayD.z * t;
                }
                else if (c == 'X')
                {
                    float d2 = rayD.y * rayD.y + rayD.z * rayD.z;
                    if (d2 > 1e-8)
                    {
                        t = - (rayO.y * rayD.y + rayO.z * rayD.z) / d2;
                        if (t < 0) t = 0;
                    }
                    else { t = 0; }
                    ix = rayO.x + rayD.x * t;
                    iy = rayO.y + rayD.y * t;
                    iz = rayO.z + rayD.z * t;
                }
                else // 'Y'
                {
                    float d2 = rayD.x * rayD.x + rayD.z * rayD.z;
                    if (d2 > 1e-8)
                    {
                        t = - (rayO.x * rayD.x + rayO.z * rayD.z) / d2;
                        if (t < 0) t = 0;
                    }
                    else { t = 0; }
                    ix = rayO.x + rayD.x * t;
                    iy = rayO.y + rayD.y * t;
                    iz = rayO.z + rayD.z * t;
                }
            }
            else
            {
                // Intersección clásica rayo-plano (plano pasa por origen)
                t = - (n.x * rayO.x + n.y * rayO.y + n.z * rayO.z) / denom;
                if (t < 0) return null;
                ix = rayO.x + rayD.x * t;
                iy = rayO.y + rayD.y * t;
                iz = rayO.z + rayD.z * t;
            }

            // distancia radial en el plano correspondiente
            float radial;
            if (c == 'Z') { radial = (float) Math.sqrt(ix * ix + iy * iy); }
            else if (c == 'X') { radial = (float) Math.sqrt(iy * iy + iz * iz); }
            else { radial = (float) Math.sqrt(ix * ix + iz * iz); }

            if (radial >= (radius - band) && radial <= (radius + band))
            {
                Hit h = new Hit();
                h.a = (c == 'Z') ? Axis.Z : (c == 'X') ? Axis.X : Axis.Y;
                h.t = t;
                return h;
            }
            return null;
        };

        Hit hz = check.apply(new Vector3f(0F, 0F, 1F), 'Z');
        Hit hx = check.apply(new Vector3f(1F, 0F, 0F), 'X');
        Hit hy = check.apply(new Vector3f(0F, 1F, 0F), 'Y');

        Hit[] all = new Hit[] { hz, hx, hy };
        for (Hit h : all)
        {
            if (h == null) continue;
            if (hitBest == null || h.t < hitBest.t) hitBest = h;
        }
        return hitBest == null ? null : hitBest.a;
    }

    private boolean angleInArc(float angDeg, float startDeg, float sweepDeg)
    {
        if (sweepDeg >= 359.9F) return true; // círculo completo
        float a = normalizeDeg(angDeg);
        float s = normalizeDeg(startDeg);
        float e = normalizeDeg(startDeg + sweepDeg);
        if (s <= e) return a >= s && a <= e;
        // envolvente
        return a >= s || a <= e;
    }

    private float normalizeDeg(float deg)
    {
        float d = deg % 360F;
        if (d < 0) d += 360F;
        return d;
    }

    /**
     * Intersección rayo-AABB (slab method). Devuelve {tmin, tmax} o null si no intersecta.
     */
    private float[] rayBoxIntersect(Vector3f ro, Vector3f rd, Vector3f min, Vector3f max)
    {
        float tmin = (min.x - ro.x) / rd.x; float tmax = (max.x - ro.x) / rd.x;
        if (tmin > tmax) { float tmp = tmin; tmin = tmax; tmax = tmp; }

        float tymin = (min.y - ro.y) / rd.y; float tymax = (max.y - ro.y) / rd.y;
        if (tymin > tymax) { float tmp = tymin; tymin = tymax; tymax = tmp; }

        if ((tmin > tymax) || (tymin > tmax)) return null;
        if (tymin > tmin) tmin = tymin; if (tymax < tmax) tmax = tymax;

        float tzmin = (min.z - ro.z) / rd.z; float tzmax = (max.z - ro.z) / rd.z;
        if (tzmin > tzmax) { float tmp = tzmin; tzmin = tzmax; tzmax = tmp; }

        if ((tmin > tzmax) || (tzmin > tmax)) return null;
        if (tzmin > tmin) tmin = tzmin; if (tzmax < tmax) tmax = tzmax;

        if (tmax < 0) return null; // caja detrás del rayo
        return new float[] { tmin, tmax };
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
            /* Ocultar ejes no activos durante arrastre */
            boolean showX = !this.dragging || this.activeAxis == Axis.X;
            boolean showY = !this.dragging || this.activeAxis == Axis.Y;
            boolean showZ = !this.dragging || this.activeAxis == Axis.Z;
            /* Líneas al pivote + píxeles en extremos para selección */
            int xColor = (Colors.A100 | Colors.RED);
            int yColor = (Colors.A100 | Colors.GREEN);
            int zColor = (Colors.A100 | Colors.BLUE);

            boolean hx = (this.hoveredAxis == Axis.X);
            boolean hy = (this.hoveredAxis == Axis.Y);
            boolean hz = (this.hoveredAxis == Axis.Z);
            float txX = hx ? (thickness + 2F) : thickness;
            float txY = hy ? (thickness + 2F) : thickness;
            float txZ = hz ? (thickness + 2F) : thickness;

            // Contorno negro detrás de cada línea
            int black = Colors.A100;
            if (showX)
            {
                context.batcher.line(cx, cy, this.endXx, this.endXy, txX + 3F, black);
                context.batcher.line(cx, cy, this.endXx, this.endXy, txX, xColor);
                /* cabeza de flecha */
                drawArrowHandle(context, cx, cy, this.endXx, this.endXy, xColor);
            }
            if (showY)
            {
                context.batcher.line(cx, cy, this.endYx, this.endYy, txY + 3F, black);
                context.batcher.line(cx, cy, this.endYx, this.endYy, txY, yColor);
                drawArrowHandle(context, cx, cy, this.endYx, this.endYy, yColor);
            }
            if (showZ)
            {
                context.batcher.line(cx, cy, this.endZx, this.endZy, txZ + 3F, black);
                context.batcher.line(cx, cy, this.endZx, this.endZy, txZ, zColor);
                drawArrowHandle(context, cx, cy, this.endZx, this.endZy, zColor);
            }

            // Halo blanco suave en el eje hovered
            int halo = Colors.A100 | Colors.WHITE;
            if (hx && showX)
            {
                context.batcher.line(cx, cy, this.endXx, this.endXy, thickness + 4F, halo);
            }
            if (hy && showY)
            {
                context.batcher.line(cx, cy, this.endYx, this.endYy, thickness + 4F, halo);
            }
            if (hz && showZ)
            {
                context.batcher.line(cx, cy, this.endZx, this.endZy, thickness + 4F, halo);
            }
        }
        else if (this.mode == Mode.SCALE)
        {
            boolean showX = !this.dragging || this.activeAxis == Axis.X;
            boolean showY = !this.dragging || this.activeAxis == Axis.Y;
            boolean showZ = !this.dragging || this.activeAxis == Axis.Z;
            /* Líneas al pivote + cubos en extremos (squares con borde) */
            int xColor = (Colors.A100 | Colors.RED);
            int yColor = (Colors.A100 | Colors.GREEN);
            int zColor = (Colors.A100 | Colors.BLUE);

            boolean hx = (this.hoveredAxis == Axis.X);
            boolean hy = (this.hoveredAxis == Axis.Y);
            boolean hz = (this.hoveredAxis == Axis.Z);
            float txX = hx ? (thickness + 2F) : thickness;
            float txY = hy ? (thickness + 2F) : thickness;
            float txZ = hz ? (thickness + 2F) : thickness;

            // Contorno negro detrás de cada línea
            int black = Colors.A100;
            if (showX)
            {
                context.batcher.line(cx, cy, this.endXx, this.endXy, txX + 3F, black);
                context.batcher.line(cx, cy, this.endXx, this.endXy, txX, xColor);
                drawCubeHandle(context, this.endXx, this.endXy, xColor);
            }
            if (showY)
            {
                context.batcher.line(cx, cy, this.endYx, this.endYy, txY + 3F, black);
                context.batcher.line(cx, cy, this.endYx, this.endYy, txY, yColor);
                drawCubeHandle(context, this.endYx, this.endYy, yColor);
            }
            if (showZ)
            {
                context.batcher.line(cx, cy, this.endZx, this.endZy, txZ + 3F, black);
                context.batcher.line(cx, cy, this.endZx, this.endZy, txZ, zColor);
                drawCubeHandle(context, this.endZx, this.endZy, zColor);
            }

            int halo = Colors.A100 | Colors.WHITE;
            if (hx && showX)
            {
                int size = 12; int half = size / 2 + 3;
                context.batcher.box(this.endXx - half, this.endXy - half, this.endXx + half, this.endXy + half, halo);
                context.batcher.line(cx, cy, this.endXx, this.endXy, thickness + 4F, halo);
            }
            if (hy && showY)
            {
                int size = 12; int half = size / 2 + 3;
                context.batcher.box(this.endYx - half, this.endYy - half, this.endYx + half, this.endYy + half, halo);
                context.batcher.line(cx, cy, this.endYx, this.endYy, thickness + 4F, halo);
            }
            if (hz && showZ)
            {
                int size = 12; int half = size / 2 + 3;
                context.batcher.box(this.endZx - half, this.endZy - half, this.endZx + half, this.endZy + half, halo);
                context.batcher.line(cx, cy, this.endZx, this.endZy, thickness + 4F, halo);
            }
        }
        else if (this.mode == Mode.ROTATE)
        {
            boolean showX = !this.dragging || this.activeAxis == Axis.X;
            boolean showY = !this.dragging || this.activeAxis == Axis.Y;
            boolean showZ = !this.dragging || this.activeAxis == Axis.Z;
            /* Restaurar: tres anillos concéntricos alrededor del pivote (sin líneas radiales) */
            int xColor = (Colors.A100 | Colors.RED);
            int yColor = (Colors.A100 | Colors.GREEN);
            int zColor = (Colors.A100 | Colors.BLUE);
            if (showX) drawRing(context, cx, cy, this.ringRX, thickness, xColor);
            if (showY) drawRing(context, cx, cy, this.ringRY, thickness, yColor);
            if (showZ) drawRing(context, cx, cy, this.ringRZ, thickness, zColor);
        }

        /* Centro del pivote: dibujar un cuadrado más visible */
        int half = 5; // tamaño total 10px
        context.batcher.box(cx - half, cy - half, cx + half, cy + half, Colors.A100 | Colors.WHITE);

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

        /* Mover/Escalar: picking por proximidad a las líneas completas y a los endpoints */
        int cx = this.centerX;
        int cy = this.centerY;
        int tol = Math.max(6, this.handleThickness + 4);
        if (isNearLine(mx, my, cx, cy, endXx, endXy, tol) || isNear(mx, my, endXx, endXy, hitRadius)) return Axis.X;
        if (isNearLine(mx, my, cx, cy, endYx, endYy, tol) || isNear(mx, my, endYx, endYy, hitRadius)) return Axis.Y;
        if (isNearLine(mx, my, cx, cy, endZx, endZy, tol) || isNear(mx, my, endZx, endZy, hitRadius)) return Axis.Z;

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
            // contorno negro detrás
            context.batcher.line(x1, y1, x2, y2, thickness + 2F, Colors.A100);
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

        // contorno negro detrás
        int pad = 3;
        context.batcher.box(x - half - pad, y - half - pad, x + half + pad, y + half + pad, Colors.A100);
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

        // contorno negro detrás
        context.batcher.line(ex, ey, lx, ly, this.handleThickness + 4, Colors.A100);
        context.batcher.line(ex, ey, rx, ry, this.handleThickness + 4, Colors.A100);
        context.batcher.line(ex, ey, lx, ly, this.handleThickness + 1, color);
        context.batcher.line(ex, ey, rx, ry, this.handleThickness + 1, color);
    }

    /** Distancia punto-segmento en pantalla para hitbox a lo largo de las líneas */
    private boolean isNearLine(int mx, int my, int x1, int y1, int x2, int y2, int tol)
    {
        double vx = x2 - x1;
        double vy = y2 - y1;
        double wx = mx - x1;
        double wy = my - y1;
        double c1 = vx * wx + vy * wy;
        double c2 = vx * vx + vy * vy;
        double t = (c2 > 0) ? Math.max(0, Math.min(1, c1 / c2)) : 0;
        double px = x1 + t * vx;
        double py = y1 + t * vy;
        double dx = mx - px;
        double dy = my - py;
        double d = Math.sqrt(dx * dx + dy * dy);
        return d <= tol;
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