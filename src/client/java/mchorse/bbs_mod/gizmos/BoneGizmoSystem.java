package mchorse.bbs_mod.gizmos;

import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.UIRenderingContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.Timer;
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
    private int handleLen = 60;
    private int handleThickness = 3;
    private int hitRadius = 6;

    /* Endpoints proyectados en pantalla para cada eje */
    private int endXx, endXy;
    private int endYx, endYy;
    private int endZx, endZy;

    public static BoneGizmoSystem get()
    {
        return INSTANCE;
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
            /* Implementar bucle horizontal del cursor como en el panel de transform */
            GLFW.glfwGetCursorPos(Window.getWindow(), CURSOR_X, CURSOR_Y);
            MinecraftClient mc = MinecraftClient.getInstance();
            int w = mc.getWindow().getWidth();

            double rawX = CURSOR_X[0];
            double fx = Math.ceil(w / (double) input.menu.width);
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
            int dx = input.mouseX - this.dragStartX;
            int dy = input.mouseY - this.dragStartY;
            float factor = switch (this.mode)
            {
                case TRANSLATE -> 0.02F;
                case SCALE -> 0.01F;
                case ROTATE -> 0.3F;
            };

            float delta = dx * factor; /* Usamos movimiento horizontal para consistencia */

            Transform t = this.target.getTransform();

            if (this.mode == Mode.TRANSLATE)
            {
                float x = t.translate.x;
                float y = t.translate.y;
                float z = t.translate.z;

                if (this.activeAxis == Axis.X) x = this.dragStart.translate.x + delta;
                if (this.activeAxis == Axis.Y) y = this.dragStart.translate.y - dy * factor; /* Y hacia arriba */
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

    public void renderOverlay(UIRenderingContext context, Area viewport)
    {
        if (viewport == null)
        {
            return;
        }

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

        /* Renderizar ejes 2D en el centro */
        int cx = this.centerX;
        int cy = this.centerY;

        // Grosor de línea en px (escala UI)
        float thickness = this.handleThickness;

        // X rojo
        int xColor = (this.hoveredAxis == Axis.X || this.activeAxis == Axis.X) ? (Colors.A100 | Colors.RED) : (Colors.A75 | Colors.RED);
        context.batcher.line(cx, cy, this.endXx, this.endXy, thickness, xColor);
        context.batcher.box(this.endXx - 4, this.endXy - 4, this.endXx + 4, this.endXy + 4, xColor);

        // Y verde
        int yColor = (this.hoveredAxis == Axis.Y || this.activeAxis == Axis.Y) ? (Colors.A100 | Colors.GREEN) : (Colors.A75 | Colors.GREEN);
        context.batcher.line(cx, cy, this.endYx, this.endYy, thickness, yColor);
        context.batcher.box(this.endYx - 4, this.endYy - 4, this.endYx + 4, this.endYy + 4, yColor);

        // Z azul
        int zColor = (this.hoveredAxis == Axis.Z || this.activeAxis == Axis.Z) ? (Colors.A100 | Colors.BLUE) : (Colors.A75 | Colors.BLUE);
        context.batcher.line(cx, cy, this.endZx, this.endZy, thickness, zColor);
        context.batcher.box(this.endZx - 4, this.endZy - 4, this.endZx + 4, this.endZy + 4, zColor);

        // Centro
        context.batcher.box(cx - 3, cy - 3, cx + 3, cy + 3, Colors.A50 | Colors.WHITE);

        /* Consejos */
        String tip = "Click izquierdo en eje para arrastrar; T/R/S cambia modo.";
        int tw = context.batcher.getFont().getWidth(tip);
        context.batcher.textCard(tip, viewport.ex() - tw - 10, viewport.ey() - context.batcher.getFont().getHeight() - 10, Colors.WHITE, Colors.A50);
    }

    /** Indica si el mouse está sobre algún handle del gizmo */
    public boolean isHoveringHandle()
    {
        return this.hoveredAxis != null;
    }

    private Axis detectHoveredAxis(int mx, int my)
    {
        /* Regiones de hit simples alrededor de cada handle */
        // X handle
        if (isNear(mx, my, endXx, endXy, hitRadius)) return Axis.X;
        // Y handle
        if (isNear(mx, my, endYx, endYy, hitRadius)) return Axis.Y;
        // Z handle
        if (isNear(mx, my, endZx, endZy, hitRadius)) return Axis.Z;

        return null;
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