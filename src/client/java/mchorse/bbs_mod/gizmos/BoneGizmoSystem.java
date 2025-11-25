package mchorse.bbs_mod.gizmos;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.UIRenderingContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
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
    public enum Mode { TRANSLATE, ROTATE, SCALE, UNIVERSAL, PIVOT }
    /** Planos de desplazamiento para mover en dos ejes simultáneamente */
    private enum Plane { XY, YZ, ZX }

    private static final BoneGizmoSystem INSTANCE = new BoneGizmoSystem();

    /* Estado */
    private Mode mode = Mode.TRANSLATE;
    /* Submodo cuando UNIVERSAL está activo (determina qué operación aplica) */
    private Mode hoveredSubMode = null;
    private Mode activeSubMode = null;
    /* Controlador de plano cuando se usa TRANSLATE (planos XY/YZ/ZX) */
    private Plane hoveredPlane = null;
    private Plane activePlane = null;
    private Axis hoveredAxis = null;
    private Axis activeAxis = null;
    private boolean dragging = false;
    private boolean lastMouseDown = false;
    /* Track CTRL state during drag to preserve free-rotation edits */
    private boolean lastCtrlPressedWhileDragging = false;
    /* Track SHIFT state during drag to preserve free-rotation edits when CTRL is held */
    private boolean lastShiftPressedWhileDragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private Transform dragStart = new Transform();
    private UIPropTransform target;

    /* Signo de rotación dependiente del lado de la cámara (3D) */
    private float rotateSign = 1F;
    /* Usar canal secundario de rotación (R2) cuando esté activo */
    private boolean useRotation2 = false;

    /* Escalado dinámico del gizmo según distancia cámara–pivote */
    private float gizmoScale = 0.2F;
    private float minGizmoScale = 0.6F;
    private float maxGizmoScale = 10.0F;
    private float scaleSlope = 0.75F;

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
        this.hoveredSubMode = null;
        this.activeSubMode = null;
        this.hoveredPlane = null;
        this.activePlane = null;
    }

    /** Alterna entre aplicar rotación en R (principal) o R2 (secundario). */
    public void toggleRotationChannel()
    {
        this.useRotation2 = !this.useRotation2;
    }

    /** Indica si el gizmo está usando el canal de rotación secundario. */
    public boolean isUsingRotation2()
    {
        return this.useRotation2;
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

        /* Detectar eje hovered (y submodo/planos si corresponde) */
        this.hoveredAxis = detectHoveredAxis(input.mouseX, input.mouseY);

        /* Inicio de arrastre (edge detection del botón derecho) */
        boolean mouseDown = Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        if (!this.dragging && mouseDown && !this.lastMouseDown && this.hoveredAxis != null)
        {
            this.dragging = true;
            this.activeAxis = this.hoveredAxis;
        this.activeSubMode = (this.mode == Mode.UNIVERSAL) ? (this.hoveredSubMode != null ? this.hoveredSubMode : Mode.ROTATE) : this.mode;
            this.activePlane = this.hoveredPlane;
            this.dragStartX = input.mouseX;
            this.dragStartY = input.mouseY;
            this.lastX = input.mouseX;
            this.lastY = input.mouseY;
            this.accumDx = 0F;
            this.accumDy = 0F;
            this.lastCtrlPressedWhileDragging = isComboHeld(Keys.GIZMOS_FREE_ROTATE_XY);
            this.lastShiftPressedWhileDragging = isComboHeld(Keys.GIZMOS_FREE_ROTATE_ZY);
            if (this.target != null && this.target.getTransform() != null)
            {
                this.dragStart.copy(this.target.getTransform());
            }

            /* Inicio de arrastre: no capturamos/ocultamos el cursor; solo comenzamos acumulación */
        }

        /* Terminar arrastre */
        if (this.dragging && !mouseDown && this.lastMouseDown)
        {
            this.dragging = false;
            this.activeAxis = null;

            /* Fin de arrastre: sin cambios en el modo de cursor */
        }

        /* Durante arrastre: aplicar delta con bucle (mismo patrón que UIPropTransform) */
        if (this.dragging && this.target != null && this.target.getTransform() != null)
        {
            boolean warped = false;

            if (this.wrapChecker.isTime())
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

            /* If CTRL/SHIFT state toggled mid-drag, rebase rotation start to current values
             * so free-rotation edits are preserved when switching modes. SHIFT only matters
             * for free rotation when CTRL is held. */
            boolean ctrlNow = isComboHeld(Keys.GIZMOS_FREE_ROTATE_XY);
            boolean shiftNow = isComboHeld(Keys.GIZMOS_FREE_ROTATE_ZY);
            boolean ctrlChanged = ctrlNow != this.lastCtrlPressedWhileDragging;
            boolean shiftChanged = ctrlNow && (shiftNow != this.lastShiftPressedWhileDragging);
            if (ctrlChanged || shiftChanged)
            {
                Transform current = this.target.getTransform();
                if (current != null)
                {
                    if (this.useRotation2)
                    {
                        this.dragStart.rotate2.x = current.rotate2.x;
                        this.dragStart.rotate2.y = current.rotate2.y;
                        this.dragStart.rotate2.z = current.rotate2.z;
                    }
                    else
                    {
                        this.dragStart.rotate.x = current.rotate.x;
                        this.dragStart.rotate.y = current.rotate.y;
                        this.dragStart.rotate.z = current.rotate.z;
                    }
                }
                this.accumDx = 0F;
                this.accumDy = 0F;
                this.lastCtrlPressedWhileDragging = ctrlNow;
                this.lastShiftPressedWhileDragging = shiftNow;
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
                case PIVOT -> 0.02F;
                case UNIVERSAL -> {
                    Mode opInner = (this.activeSubMode != null) ? this.activeSubMode : Mode.TRANSLATE;
                    yield (opInner == Mode.TRANSLATE) ? 0.02F : (opInner == Mode.SCALE ? 0.01F : 0.3F);
                }
            };

            float delta = this.accumDx * factor; /* Usamos movimiento horizontal para consistencia */

            Transform t = this.target.getTransform();

        Mode op = (this.mode == Mode.UNIVERSAL) ? (this.activeSubMode != null ? this.activeSubMode : Mode.ROTATE) : this.mode;

            if (op == Mode.TRANSLATE)
            {
                float x = t.translate.x;
                float y = t.translate.y;
                float z = t.translate.z;

                if (this.activePlane == null)
                {
                    if (this.activeAxis == Axis.X) x = this.dragStart.translate.x + delta;
                    if (this.activeAxis == Axis.Y) y = this.dragStart.translate.y - this.accumDy * factor; /* Y hacia arriba */
                    if (this.activeAxis == Axis.Z) z = this.dragStart.translate.z + delta;
                }
                else
                {
                    // Controladores de plano: mapear horizontal/vertical a dos ejes
                    switch (this.activePlane)
                    {
                        case XY -> {
                            x = this.dragStart.translate.x + delta;
                            y = this.dragStart.translate.y - this.accumDy * factor;
                        }
                        case ZX -> {
                            z = this.dragStart.translate.z + delta;
                            x = this.dragStart.translate.x - this.accumDy * factor;
                        }
                        case YZ -> {
                            z = this.dragStart.translate.z + delta;
                            y = this.dragStart.translate.y - this.accumDy * factor;
                        }
                    }
                }

                this.target.setT(null, x, y, z);
                this.target.setTransform(t);
            }
            else if (op == Mode.SCALE)
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
            else if (op == Mode.ROTATE)
            {
                /* Rotaciones en grados en UIPropTransform.setR */
                float rx, ry, rz;
                if (this.useRotation2)
                {
                    rx = (float) Math.toDegrees(this.dragStart.rotate2.x);
                    ry = (float) Math.toDegrees(this.dragStart.rotate2.y);
                    rz = (float) Math.toDegrees(this.dragStart.rotate2.z);
                }
                else
                {
                    rx = (float) Math.toDegrees(this.dragStart.rotate.x);
                    ry = (float) Math.toDegrees(this.dragStart.rotate.y);
                    rz = (float) Math.toDegrees(this.dragStart.rotate.z);
                }

                /* Rotación libre basada en combos configurables (Keybinds): CTRL o CTRL+SHIFT
                 * usando acumulado de mouse para conservar continuidad durante el arrastre. */
                boolean xyHeld = isComboHeld(Keys.GIZMOS_FREE_ROTATE_XY);
                boolean zyHeld = isComboHeld(Keys.GIZMOS_FREE_ROTATE_ZY);

                if (zyHeld)
                {
                    float zDelta = this.accumDx * factor * 10F;    // horizontal -> Z (roll)
                    float yDelta = -this.accumDy * factor * 10F;   // vertical   -> Y
                    rz = rz + zDelta;
                    ry = ry + yDelta;
                }
                else if (xyHeld)
                {
                    float yDelta = this.accumDx * factor * 10F;    // horizontal -> Y (yaw)
                    float xDelta = -this.accumDy * factor * 10F;   // vertical   -> X (pitch)
                    ry = ry + yDelta;
                    rx = rx + xDelta;
                }
                else
                {
                    if (this.activeAxis == Axis.X) rx = rx + delta * 10F;
                    if (this.activeAxis == Axis.Y) ry = ry + delta * 10F;
                    if (this.activeAxis == Axis.Z) rz = rz + delta * 10F;
                }

                if (this.useRotation2)
                {
                    this.target.setR2(null, rx, ry, rz);
                }
                else
                {
                    this.target.setR(null, rx, ry, rz);
                }
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

                /* Escala del gizmo: dinámica (según distancia) o estática desde ajustes */
                if (BBSSettings.gizmoDynamic.get())
                {
                    try
                    {
                        org.joml.Vector4f camW4 = new org.joml.Vector4f(0, 0, 0, 1);
                        org.joml.Vector4f camWorld = new Matrix4f(view).invert(new Matrix4f()).transform(camW4);
                        camWorld.div(camWorld.w);

                        org.joml.Vector4f pivotW4 = new org.joml.Vector4f(0, 0, 0, 1);
                        org.joml.Vector4f pivotWorld = new Matrix4f(origin).transform(pivotW4);
                        pivotWorld.div(pivotWorld.w);

                        float dx = camWorld.x - pivotWorld.x;
                        float dy = camWorld.y - pivotWorld.y;
                        float dz = camWorld.z - pivotWorld.z;
                        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

                        float s = dist * scaleSlope;
                        if (s < minGizmoScale) s = minGizmoScale;
                        if (s > maxGizmoScale) s = maxGizmoScale;
                        this.gizmoScale = s;
                    }
                    catch (Throwable t)
                    {
                        this.gizmoScale = 1F;
                    }
                }
                else
                {
                    this.gizmoScale = clampScale(BBSSettings.gizmoScale.get());
                }

                // Iniciar/terminar arrastre basado en 3D hover
                boolean mouseDown = Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT);
                if (!this.dragging && mouseDown && !this.lastMouseDown && this.hoveredAxis != null)
                {
                    this.dragging = true;
                    this.activeAxis = this.hoveredAxis;
        this.activeSubMode = (this.mode == Mode.UNIVERSAL) ? (this.hoveredSubMode != null ? this.hoveredSubMode : Mode.ROTATE) : this.mode;
                    this.activePlane = this.hoveredPlane;
                    this.dragStartX = input.mouseX;
                    this.dragStartY = input.mouseY;
                    this.lastX = input.mouseX;
                    this.lastY = input.mouseY;
                    this.accumDx = 0F;
                    this.accumDy = 0F;
                    this.lastCtrlPressedWhileDragging = isComboHeld(Keys.GIZMOS_FREE_ROTATE_XY);
                    this.lastShiftPressedWhileDragging = isComboHeld(Keys.GIZMOS_FREE_ROTATE_ZY);
                    if (this.target != null && this.target.getTransform() != null)
                    {
                        this.dragStart.copy(this.target.getTransform());
                    }

                    // Determinar el signo de rotación según el lado de cámara respecto al plano del anillo
                    // Reconstruimos el rayo del mouse y evaluamos n·rayD para el eje activo
                    try
                    {
                        float nx = (float) (((input.mouseX - viewport.x) / (double) viewport.w) * 2.0 - 1.0);
                        float ny = (float) ( -(((input.mouseY - viewport.y) / (double) viewport.h) * 2.0 - 1.0) );
                        org.joml.Matrix4f invPV = new org.joml.Matrix4f(projection).mul(view).invert(new org.joml.Matrix4f());
                        org.joml.Vector4f nearClip = new org.joml.Vector4f(nx, ny, -1F, 1F);
                        org.joml.Vector4f farClip  = new org.joml.Vector4f(nx, ny,  1F, 1F);
                        org.joml.Vector4f nearWorld = invPV.transform(new org.joml.Vector4f(nearClip));
                        org.joml.Vector4f farWorld  = invPV.transform(new org.joml.Vector4f(farClip));
                        nearWorld.div(nearWorld.w);
                        farWorld.div(farWorld.w);
                        org.joml.Matrix4f invOrigin = new org.joml.Matrix4f(origin).invert(new org.joml.Matrix4f());
                        org.joml.Vector4f nearLocal4 = invOrigin.transform(new org.joml.Vector4f(nearWorld));
                        org.joml.Vector4f farLocal4  = invOrigin.transform(new org.joml.Vector4f(farWorld));
                        org.joml.Vector3f rayO = new org.joml.Vector3f(nearLocal4.x, nearLocal4.y, nearLocal4.z);
                        org.joml.Vector3f rayF = new org.joml.Vector3f(farLocal4.x, farLocal4.y, farLocal4.z);
                        org.joml.Vector3f rayD = rayF.sub(rayO, new org.joml.Vector3f());
                        rayD.normalize();

                        org.joml.Vector3f n = switch (this.activeAxis)
                        {
                            case X -> new org.joml.Vector3f(1F, 0F, 0F);
                            case Y -> new org.joml.Vector3f(0F, 1F, 0F);
                            case Z -> new org.joml.Vector3f(0F, 0F, 1F);
                        };

                        float denom = n.x * rayD.x + n.y * rayD.y + n.z * rayD.z;
                        // Si el rayo entra desde el lado positivo de la normal, invertir para mantener sentido visual
                        this.rotateSign = (denom >= 0F) ? -1F : 1F;
                    }
                    catch (Throwable t)
                    {
                        this.rotateSign = 1F;
                    }
                }

                if (this.dragging && !mouseDown && this.lastMouseDown)
                {
                    /* Finalizar arrastre y fijar el estado actual como base */
                    this.dragging = false;
                    this.activeAxis = null;
                    if (this.target != null && this.target.getTransform() != null)
                    {
                        this.dragStart.copy(this.target.getTransform());
                    }
                    this.accumDx = 0F;
                    this.accumDy = 0F;
                    this.lastX = input.mouseX;
                    this.lastY = input.mouseY;
                }

                if (this.dragging && this.target != null && this.target.getTransform() != null)
                {
                    /* Rebase rotation start if configurable combos toggled mid-drag to preserve free-rotation edits */
                    boolean ctrlNow = isComboHeld(Keys.GIZMOS_FREE_ROTATE_XY);
                    boolean shiftNow = isComboHeld(Keys.GIZMOS_FREE_ROTATE_ZY);
                    boolean ctrlChanged = ctrlNow != this.lastCtrlPressedWhileDragging;
                    boolean shiftChanged = ctrlNow && (shiftNow != this.lastShiftPressedWhileDragging);
                    if (ctrlChanged || shiftChanged)
                    {
                        Transform current = this.target.getTransform();
                        if (current != null)
                        {
                            if (this.useRotation2)
                            {
                                this.dragStart.rotate2.x = current.rotate2.x;
                                this.dragStart.rotate2.y = current.rotate2.y;
                                this.dragStart.rotate2.z = current.rotate2.z;
                            }
                            else
                            {
                                this.dragStart.rotate.x = current.rotate.x;
                                this.dragStart.rotate.y = current.rotate.y;
                                this.dragStart.rotate.z = current.rotate.z;
                            }
                        }
                        this.accumDx = 0F;
                        this.accumDy = 0F;
                        this.lastCtrlPressedWhileDragging = ctrlNow;
                        this.lastShiftPressedWhileDragging = shiftNow;
                    }

                    // Delta de mouse inmediato y acumulado para diferentes comportamientos
                    int stepX = input.mouseX - this.lastX;
                    int stepY = input.mouseY - this.lastY;
                    this.accumDx += stepX;
                    this.accumDy += stepY;
                    this.lastX = input.mouseX;
                    this.lastY = input.mouseY;

        Mode op = (this.mode == Mode.UNIVERSAL) ? (this.activeSubMode != null ? this.activeSubMode : Mode.ROTATE) : this.mode;
                    float factor = switch (op)
                    {
                        case TRANSLATE -> 0.02F;
                        case SCALE -> 0.01F;
                        case ROTATE -> 0.3F;
                        case UNIVERSAL -> 0.02F; // default
                        case PIVOT -> 0.02F;
                    };

                    float delta = this.accumDx * factor;
                    Transform t = this.target.getTransform();

                    if (op == Mode.TRANSLATE)
                    {
                        float x = t.translate.x;
                        float y = t.translate.y;
                        float z = t.translate.z;

                        if (this.activePlane == null)
                        {
                            if (this.activeAxis == Axis.X) x = this.dragStart.translate.x + delta;
                            if (this.activeAxis == Axis.Y) y = this.dragStart.translate.y - this.accumDy * factor;
                            if (this.activeAxis == Axis.Z) z = this.dragStart.translate.z + delta;
                        }
                        else
                        {
                            switch (this.activePlane)
                            {
                                case XY -> {
                                    x = this.dragStart.translate.x + delta;
                                    y = this.dragStart.translate.y - this.accumDy * factor;
                                }
                                case ZX -> {
                                    z = this.dragStart.translate.z + delta;
                                    x = this.dragStart.translate.x - this.accumDy * factor;
                                }
                                case YZ -> {
                                    z = this.dragStart.translate.z + delta;
                                    y = this.dragStart.translate.y - this.accumDy * factor;
                                }
                            }
                        }

                        this.target.setT(null, x, y, z);
                        this.target.setTransform(t);
                    }
                    else if (op == Mode.SCALE)
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
                    else if (op == Mode.ROTATE)
                    {
                        float rx, ry, rz;
                        if (this.useRotation2)
                        {
                            rx = (float) Math.toDegrees(this.dragStart.rotate2.x);
                            ry = (float) Math.toDegrees(this.dragStart.rotate2.y);
                            rz = (float) Math.toDegrees(this.dragStart.rotate2.z);
                        }
                        else
                        {
                            rx = (float) Math.toDegrees(this.dragStart.rotate.x);
                            ry = (float) Math.toDegrees(this.dragStart.rotate.y);
                            rz = (float) Math.toDegrees(this.dragStart.rotate.z);
                        }

                        boolean xyHeld = isComboHeld(Keys.GIZMOS_FREE_ROTATE_XY);
                        boolean zyHeld = isComboHeld(Keys.GIZMOS_FREE_ROTATE_ZY);

                        if (zyHeld)
                        {
                            float zDelta = this.accumDx * factor * 10F;    // Z
                            float yDelta = -this.accumDy * factor * 10F;   // Y
                            rz = rz + zDelta;
                            ry = ry + yDelta;
                        }
                        else if (xyHeld)
                        {
                            float yDelta = this.accumDx * factor * 10F;    // Y
                            float xDelta = -this.accumDy * factor * 10F;   // X
                            ry = ry + yDelta;
                            rx = rx + xDelta;
                        }
                        else
                        {
                            float d = delta * this.rotateSign * 10F;
                            if (this.activeAxis == Axis.X) rx = rx + d;
                            if (this.activeAxis == Axis.Y) ry = ry + d;
                            if (this.activeAxis == Axis.Z) rz = rz + d;
                        }

                        if (this.useRotation2)
                        {
                            this.target.setR2(null, rx, ry, rz);
                        }
                        else
                        {
                            this.target.setR(null, rx, ry, rz);
                        }
                        this.target.setTransform(t);
                    }
                    else if (op == Mode.PIVOT)
                    {
                        float x = t.pivot.x;
                        float y = t.pivot.y;
                        float z = t.pivot.z;

                        if (this.activePlane == null)
                        {
                            if (this.activeAxis == Axis.X) x = this.dragStart.pivot.x + delta;
                            if (this.activeAxis == Axis.Y) y = this.dragStart.pivot.y - this.accumDy * factor;
                            if (this.activeAxis == Axis.Z) z = this.dragStart.pivot.z + delta;
                        }
                        else
                        {
                            switch (this.activePlane)
                            {
                                case XY -> {
                                    x = this.dragStart.pivot.x + delta;
                                    y = this.dragStart.pivot.y - this.accumDy * factor;
                                }
                                case ZX -> {
                                    z = this.dragStart.pivot.z + delta;
                                    x = this.dragStart.pivot.x - this.accumDy * factor;
                                }
                                case YZ -> {
                                    z = this.dragStart.pivot.z + delta;
                                    y = this.dragStart.pivot.y - this.accumDy * factor;
                                }
                            }
                        }

                        this.target.setP(null, x, y, z);
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

        /* Detectar eje hovered (y submodo/planos si corresponde) */
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

            /* Inicio de arrastre: no capturamos/ocultamos el cursor; solo comenzamos acumulación */
        }

        /* Terminar arrastre */
        if (this.dragging && !mouseDown && this.lastMouseDown)
        {
            this.dragging = false;
            this.activeAxis = null;

            /* Fin de arrastre: sin cambios en el modo de cursor */
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
                case PIVOT -> 0.02F;
                case UNIVERSAL -> {
                    Mode opInner = (this.activeSubMode != null) ? this.activeSubMode : Mode.TRANSLATE;
                    yield (opInner == Mode.TRANSLATE) ? 0.02F : (opInner == Mode.SCALE ? 0.01F : 0.3F);
                }
            };

            float delta = this.accumDx * factor; /* Usamos movimiento horizontal para consistencia */

            Transform t = this.target.getTransform();

            Mode op = (this.mode == Mode.UNIVERSAL) ? (this.activeSubMode != null ? this.activeSubMode : Mode.TRANSLATE) : this.mode;

            if (op == Mode.TRANSLATE)
            {
                float x = t.translate.x;
                float y = t.translate.y;
                float z = t.translate.z;

                if (this.activePlane == null)
                {
                    if (this.activeAxis == Axis.X) x = this.dragStart.translate.x + delta;
                    if (this.activeAxis == Axis.Y) y = this.dragStart.translate.y - this.accumDy * factor; /* Y hacia arriba */
                    if (this.activeAxis == Axis.Z) z = this.dragStart.translate.z + delta;
                }
                else
                {
                    switch (this.activePlane)
                    {
                        case XY -> {
                            x = this.dragStart.translate.x + delta;
                            y = this.dragStart.translate.y - this.accumDy * factor;
                        }
                        case ZX -> {
                            z = this.dragStart.translate.z + delta;
                            x = this.dragStart.translate.x - this.accumDy * factor;
                        }
                        case YZ -> {
                            y = this.dragStart.translate.y + delta;
                            z = this.dragStart.translate.z - this.accumDy * factor;
                        }
                    }
                }

                this.target.setT(null, x, y, z);
                this.target.setTransform(t);
            }
            else if (op == Mode.SCALE)
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
            else if (op == Mode.ROTATE)
            {
                /* Rotaciones en grados en UIPropTransform.setR */
                float rx, ry, rz;
                if (this.useRotation2)
                {
                    rx = (float) Math.toDegrees(this.dragStart.rotate2.x);
                    ry = (float) Math.toDegrees(this.dragStart.rotate2.y);
                    rz = (float) Math.toDegrees(this.dragStart.rotate2.z);
                }
                else
                {
                    rx = (float) Math.toDegrees(this.dragStart.rotate.x);
                    ry = (float) Math.toDegrees(this.dragStart.rotate.y);
                    rz = (float) Math.toDegrees(this.dragStart.rotate.z);
                }

                boolean xyHeld3D = isComboHeld(Keys.GIZMOS_FREE_ROTATE_XY);
                boolean zyHeld3D = isComboHeld(Keys.GIZMOS_FREE_ROTATE_ZY);

                if (zyHeld3D)
                {
                    float zDelta = this.accumDx * factor * 10F;    // horizontal -> Z
                    float yDelta = -this.accumDy * factor * 10F;   // vertical   -> Y
                    rz = rz + zDelta;
                    ry = ry + yDelta;
                }
                else if (xyHeld3D)
                {
                    float yDelta = this.accumDx * factor * 10F;    // horizontal -> Y
                    float xDelta = -this.accumDy * factor * 10F;   // vertical   -> X
                    ry = ry + yDelta;
                    rx = rx + xDelta;
                }
                else
                {
                    if (this.activeAxis == Axis.X) rx = rx + delta * 10F;
                    if (this.activeAxis == Axis.Y) ry = ry + delta * 10F;
                    if (this.activeAxis == Axis.Z) rz = rz + delta * 10F;
                }

                if (this.useRotation2)
                {
                    this.target.setR2(null, rx, ry, rz);
                }
                else
                {
                    this.target.setR(null, rx, ry, rz);
                }
                this.target.setTransform(t);
            }
        }

        this.lastMouseDown = mouseDown;
    }

    /** Returns true when all keys in the combo are currently held down. */
    private boolean isComboHeld(KeyCombo combo)
    {
        if (combo == null)
        {
            return false;
        }

        for (int key : combo.keys)
        {
            if (!Window.isKeyPressed(key))
            {
                return false;
            }
        }

        return true;
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

        int design = BBSSettings.gizmoDesign.get();
        boolean blockbench = (design == 2);
        boolean thinDesign = (design == 1) || blockbench;
        float baseLength = blockbench ? 0.35F : 0.25F;
        float length = baseLength * this.gizmoScale;     // longitud de cada eje
        float thickness = (blockbench ? 0.010F : (thinDesign ? 0.012F : 0.02F)) * this.gizmoScale;   // barras más delgadas en Blockbench
        float outlinePad = (thinDesign ? 0F : 0.01F) * this.gizmoScale;          // contorno negro opcional (no en Classic/Blockbench)
        float slabThick = (blockbench ? 0.010F : (thinDesign ? 0.012F : 0.018F)) * this.gizmoScale;  // losas más delgadas en Blockbench

        // Ajuste dinámico para asegurar que la barra toque el cubo en el extremo.
        // Usamos el tamaño del cubo del extremo + el grosor de la barra para
        // evitar gaps en perspectiva o por redondeos.
        float cubeSmall = 0.022F * this.gizmoScale;
        float cubeBig = 0.045F * this.gizmoScale;
        // Ajuste por modo: en TRANSLATE conectamos a la base de la flecha;
        // en SCALE nos internamos en el cubo para evitar gaps visuales.
        float connectFudge = (this.mode == Mode.TRANSLATE)
            ? 0.03F
            : (this.mode == Mode.SCALE ? slabThick : (cubeBig + thickness));

        /* Mostrar solo el eje activo durante el arrastre; si se arrastra un plano,
           ocultar todas las flechas. */
        boolean showX = !this.dragging || (this.activePlane == null && this.activeAxis == Axis.X);
        boolean showY = !this.dragging || (this.activePlane == null && this.activeAxis == Axis.Y);
        boolean showZ = !this.dragging || (this.activePlane == null && this.activeAxis == Axis.Z);

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

        if (this.mode == Mode.TRANSLATE || this.mode == Mode.PIVOT)
        {
            // Conos en las puntas de cada eje (estilo DCCs)
            float headLen = (blockbench ? 0.06F : 0.08F) * this.gizmoScale;       // cono más delgado en Blockbench
            float headWidth = (blockbench ? 0.04F : 0.06F) * this.gizmoScale;     // base más estrecha en Blockbench
            float headRadius = headWidth * 0.5F;
            // Radio de esfera para modo PIVOT (ligeramente más pequeño por petición)
            float sphereR = 0.045F * this.gizmoScale;

            // Igualar longitud visual de barras con ESCALAR y ajustar conos al nuevo extremo
            float lengthBar = length + connectFudge;   // mismo alcance visual que SCALE
            // Las barras deben llegar hasta el manejador correspondiente (cono/esfera)
            float barEnd = lengthBar - (this.mode == Mode.PIVOT ? sphereR : headLen) - 0.002F;

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

            // Manejadores en la punta: flechas para TRANSLATE, esferas para PIVOT
            if (this.mode == Mode.TRANSLATE || (this.mode == Mode.PIVOT && blockbench))
            {
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
            }
            else // PIVOT normal: usar esferas (excepto Blockbench)
            {
                if (showX)
                {
                    // contorno negro (esfera ligeramente mayor)
                    drawSphere3D(builder, stack, 'X', lengthBar, sphereR + outlinePad, 0F, 0F, 0F, 1F);
                    drawSphere3D(builder, stack, 'X', lengthBar, sphereR, 1F, 0F, 0F, 1F);
                }
                if (showY)
                {
                    drawSphere3D(builder, stack, 'Y', lengthBar, sphereR + outlinePad, 0F, 0F, 0F, 1F);
                    drawSphere3D(builder, stack, 'Y', lengthBar, sphereR, 0F, 1F, 0F, 1F);
                }
                if (showZ)
                {
                    drawSphere3D(builder, stack, 'Z', lengthBar, sphereR + outlinePad, 0F, 0F, 0F, 1F);
                    drawSphere3D(builder, stack, 'Z', lengthBar, sphereR, 0F, 0F, 1F, 1F);
                }
            }

            // Cubo de pivote en el origen como referencia (con contorno negro)
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall + outlinePad, 0F, 0F, 0F);
            if (blockbench && this.mode == Mode.PIVOT)
            {
                // Color pivote Blockbench: #1bbbf5
                drawEndCube(builder, stack, 0, 0, 0, cubeSmall, 27F/255F, 187F/255F, 245F/255F);
            }
            else
            {
                drawEndCube(builder, stack, 0, 0, 0, cubeSmall, 1F, 1F, 1F);
            }

            // Halo suave en el eje hovered
            if (hx && showX)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, barEnd, 0, 0, thickness * 2F, 1F, 1F, 1F, 0.25F);
                if (this.mode == Mode.TRANSLATE || (this.mode == Mode.PIVOT && blockbench)) drawCone3D(builder, stack, 'X', lengthBar, headLen, headRadius, 1F, 1F, 1F, 0.35F);
                else drawSphere3D(builder, stack, 'X', lengthBar, sphereR, 1F, 1F, 1F, 0.35F);
            }
            if (hy && showY)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, barEnd, 0, thickness * 2F, 1F, 1F, 1F, 0.25F);
                if (this.mode == Mode.TRANSLATE || (this.mode == Mode.PIVOT && blockbench)) drawCone3D(builder, stack, 'Y', lengthBar, headLen, headRadius, 1F, 1F, 1F, 0.35F);
                else drawSphere3D(builder, stack, 'Y', lengthBar, sphereR, 1F, 1F, 1F, 0.35F);
            }
            if (hz && showZ)
            {
                Draw.fillBox(builder, stack, -thickness, -thickness, 0F, thickness, thickness, barEnd, 1F, 1F, 1F, 0.25F);
                if (this.mode == Mode.TRANSLATE || (this.mode == Mode.PIVOT && blockbench)) drawCone3D(builder, stack, 'Z', lengthBar, headLen, headRadius, 1F, 1F, 1F, 0.35F);
                else drawSphere3D(builder, stack, 'Z', lengthBar, sphereR, 1F, 1F, 1F, 0.35F);
            }
            // Controladores de plano como losas planas separadas del pivote y de las barras
            if (BBSSettings.gizmoPlanes.get())
            {
            float offset = 0.08F * this.gizmoScale;    // mitad de la distancia previa
            float planeHalf = 0.020F * this.gizmoScale; // semitamaño del cuadrado
            float planeThick = 0.004F * this.gizmoScale; // grosor perpendicular
            boolean hXY = (this.hoveredPlane == Plane.XY) || (this.activePlane == Plane.XY);
            boolean hZX = (this.hoveredPlane == Plane.ZX) || (this.activePlane == Plane.ZX);
            boolean hYZ = (this.hoveredPlane == Plane.YZ) || (this.activePlane == Plane.YZ);
            // Mostrar solo el plano activo durante el arrastre; si se arrastra una flecha, ocultar planos
            boolean showPlaneXY = !this.dragging || this.activePlane == Plane.XY;
            boolean showPlaneZX = !this.dragging || this.activePlane == Plane.ZX;
            boolean showPlaneYZ = !this.dragging || this.activePlane == Plane.YZ;

            // XY -> azul (perpendicular Z)
                if (!thinDesign && showPlaneXY) { Draw.fillBox(builder, stack,
                    offset - planeHalf, offset - planeHalf, -planeThick,
                    offset + planeHalf, offset + planeHalf,  planeThick,
                    0F, 0F, 0F, 1F); }
            if (showPlaneXY) { Draw.fillBox(builder, stack,
                offset - (planeHalf - 0.002F), offset - (planeHalf - 0.002F), -(planeThick - 0.002F),
                offset + (planeHalf - 0.002F), offset + (planeHalf - 0.002F),  (planeThick - 0.002F),
                0F, 0F, 1F, 1F); }
            if (hXY && showPlaneXY) Draw.fillBox(builder, stack,
                offset - (planeHalf + 0.004F), offset - (planeHalf + 0.004F), -(planeThick + 0.004F),
                offset + (planeHalf + 0.004F), offset + (planeHalf + 0.004F),  (planeThick + 0.004F),
                1F, 1F, 1F, 0.30F);

            // ZX -> verde (perpendicular Y)
                if (!thinDesign && showPlaneZX) { Draw.fillBox(builder, stack,
                    offset - planeHalf, -planeThick, offset - planeHalf,
                    offset + planeHalf,  planeThick, offset + planeHalf,
                    0F, 0F, 0F, 1F); }
            if (showPlaneZX) { Draw.fillBox(builder, stack,
                offset - (planeHalf - 0.002F), -(planeThick - 0.002F), offset - (planeHalf - 0.002F),
                offset + (planeHalf - 0.002F),  (planeThick - 0.002F), offset + (planeHalf - 0.002F),
                0F, 1F, 0F, 1F); }
            if (hZX && showPlaneZX) Draw.fillBox(builder, stack,
                offset - (planeHalf + 0.004F), -(planeThick + 0.004F), offset - (planeHalf + 0.004F),
                offset + (planeHalf + 0.004F),  (planeThick + 0.004F), offset + (planeHalf + 0.004F),
                1F, 1F, 1F, 0.30F);

            // YZ -> rojo (perpendicular X)
                if (!thinDesign && showPlaneYZ) { Draw.fillBox(builder, stack,
                    -planeThick, offset - planeHalf, offset - planeHalf,
                    planeThick, offset + planeHalf, offset + planeHalf,
                    0F, 0F, 0F, 1F); }
            if (showPlaneYZ) { Draw.fillBox(builder, stack,
                -(planeThick - 0.002F), offset - (planeHalf - 0.002F), offset - (planeHalf - 0.002F),
                 (planeThick - 0.002F), offset + (planeHalf - 0.002F), offset + (planeHalf - 0.002F),
                1F, 0F, 0F, 1F); }
                if (hYZ && showPlaneYZ) Draw.fillBox(builder, stack,
                    -(planeThick + 0.004F), offset - (planeHalf + 0.004F), offset - (planeHalf + 0.004F),
                     (planeThick + 0.004F), offset + (planeHalf + 0.004F), offset + (planeHalf + 0.004F),
                    1F, 1F, 1F, 0.30F);
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
        else if (this.mode == Mode.UNIVERSAL)
        {
            // Render combinado: losas (SCALE) y anillos (ROTATE). Sin TRANSLATE.
            Mode usingSub = this.dragging ? this.activeSubMode : null;

            boolean hx = this.dragging ? (this.activeAxis == Axis.X) : (this.hoveredAxis == Axis.X);
            boolean hy = this.dragging ? (this.activeAxis == Axis.Y) : (this.hoveredAxis == Axis.Y);
            boolean hz = this.dragging ? (this.activeAxis == Axis.Z) : (this.hoveredAxis == Axis.Z);

            float lengthBar = 0.25F + 0.03F;
            float headLen = 0.08F;
            float headRadius = 0.03F;

            float txX = hx ? thickness * 1.5F : thickness;
            float txY = hy ? thickness * 1.5F : thickness;
            float txZ = hz ? thickness * 1.5F : thickness;

            // Sin render de barras/conos de traslación en UNIVERSAL

            // Losas grandes en extremos (SCALE)
            if (usingSub == null || usingSub == Mode.SCALE)
            {
                // Separar las losas de la punta del cono para que exista espacio visible
                float slabGap = 0.02F; // espacio entre la punta del cono y la losa
                float slabOffX = lengthBar + slabGap;
                float slabOffY = lengthBar + slabGap;
                float slabOffZ = lengthBar + slabGap;

                if (showX)
                {
                    stack.push(); stack.translate(slabOffX, 0F, 0F);
                    Draw.fillBox(builder, stack, -(slabThick + outlinePad), -(cubeBig + outlinePad), -(cubeBig + outlinePad), (slabThick + outlinePad), (cubeBig + outlinePad), (cubeBig + outlinePad), 0F, 0F, 0F, 1F);
                    Draw.fillBox(builder, stack, -slabThick, -cubeBig, -cubeBig, slabThick, cubeBig, cubeBig, 1F, 0F, 0F, 0.75F);
                    // Halo/blanco suave cuando el eje X está seleccionado/hover
                    if (hx)
                    {
                        Draw.fillBox(builder, stack, -(slabThick + 0.006F), -(cubeBig + 0.01F), -(cubeBig + 0.01F), (slabThick + 0.006F), (cubeBig + 0.01F), (cubeBig + 0.01F), 1F, 1F, 1F, 0.30F);
                    }
                    stack.pop();
                }
                if (showY)
                {
                    stack.push(); stack.translate(0F, slabOffY, 0F);
                    Draw.fillBox(builder, stack, -(cubeBig + outlinePad), -(slabThick + outlinePad), -(cubeBig + outlinePad), (cubeBig + outlinePad), (slabThick + outlinePad), (cubeBig + outlinePad), 0F, 0F, 0F, 1F);
                    Draw.fillBox(builder, stack, -cubeBig, -slabThick, -cubeBig, cubeBig, slabThick, cubeBig, 0F, 1F, 0F, 0.75F);
                    if (hy)
                    {
                        Draw.fillBox(builder, stack, -(cubeBig + 0.01F), -(slabThick + 0.006F), -(cubeBig + 0.01F), (cubeBig + 0.01F), (slabThick + 0.006F), (cubeBig + 0.01F), 1F, 1F, 1F, 0.30F);
                    }
                    stack.pop();
                }
                if (showZ)
                {
                    stack.push(); stack.translate(0F, 0F, slabOffZ);
                    Draw.fillBox(builder, stack, -(cubeBig + outlinePad), -(cubeBig + outlinePad), -(slabThick + outlinePad), (cubeBig + outlinePad), (cubeBig + outlinePad), (slabThick + outlinePad), 0F, 0F, 0F, 1F);
                    Draw.fillBox(builder, stack, -cubeBig, -cubeBig, -slabThick, cubeBig, cubeBig, slabThick, 0F, 0F, 1F, 0.75F);
                    if (hz)
                    {
                        Draw.fillBox(builder, stack, -(cubeBig + 0.01F), -(cubeBig + 0.01F), -(slabThick + 0.006F), (cubeBig + 0.01F), (cubeBig + 0.01F), (slabThick + 0.006F), 1F, 1F, 1F, 0.30F);
                    }
                    stack.pop();
                }
            }

            // Anillos de rotación — iguales al gizmo de ROTATE
            if (usingSub == null || usingSub == Mode.ROTATE)
            {
                float radius = 0.22F; float thicknessRing = ((BBSSettings.gizmoDesign.get() == 1) ? 0.006F : 0.01F); float sweep = 360F;
                RenderSystem.disableCull();
                drawEndCube(builder, stack, 0, 0, 0, 0.022F + outlinePad, 0F, 0F, 0F);
                drawEndCube(builder, stack, 0, 0, 0, 0.022F, 1F, 1F, 1F);
                if (showZ) { drawRingArc3D(builder, stack, 'Z', radius, thicknessRing + outlinePad, 0F, 0F, 0F, 0F, sweep, false); drawRingArc3D(builder, stack, 'Z', radius, thicknessRing, 0F, 0F, 1F, 0F, sweep, hz); }
                if (showX) { drawRingArc3D(builder, stack, 'X', radius, thicknessRing + outlinePad, 0F, 0F, 0F, 0F, sweep, false); drawRingArc3D(builder, stack, 'X', radius, thicknessRing, 1F, 0F, 0F, 0F, sweep, hx); }
                if (showY) { drawRingArc3D(builder, stack, 'Y', radius, thicknessRing + outlinePad, 0F, 0F, 0F, 0F, sweep, false); drawRingArc3D(builder, stack, 'Y', radius, thicknessRing, 0F, 1F, 0F, 0F, sweep, hy); }
                RenderSystem.enableCull();
            }

            // Losas planas (UNIVERSAL): mismos offsets que TRANSLATE
            float offset = 0.08F; float planeHalf = 0.020F; float planeThick = 0.004F;
            boolean hXY = (this.hoveredPlane == Plane.XY) || (this.activePlane == Plane.XY);
            boolean hZX = (this.hoveredPlane == Plane.ZX) || (this.activePlane == Plane.ZX);
            boolean hYZ = (this.hoveredPlane == Plane.YZ) || (this.activePlane == Plane.YZ);
            boolean showPlaneXY = !this.dragging || this.activePlane == Plane.XY;
            boolean showPlaneZX = !this.dragging || this.activePlane == Plane.ZX;
            boolean showPlaneYZ = !this.dragging || this.activePlane == Plane.YZ;
            // XY -> azul
            if (!thinDesign && showPlaneXY) Draw.fillBox(builder, stack, offset - planeHalf, offset - planeHalf, -planeThick, offset + planeHalf, offset + planeHalf, planeThick, 0F, 0F, 0F, 1F);
            if (showPlaneXY) Draw.fillBox(builder, stack, offset - (planeHalf - 0.002F), offset - (planeHalf - 0.002F), -(planeThick - 0.002F), offset + (planeHalf - 0.002F), offset + (planeHalf - 0.002F), (planeThick - 0.002F), 0F, 0F, 1F, 1F);
            if (hXY && showPlaneXY) Draw.fillBox(builder, stack, offset - (planeHalf + 0.004F), offset - (planeHalf + 0.004F), -(planeThick + 0.004F), offset + (planeHalf + 0.004F), offset + (planeHalf + 0.004F), (planeThick + 0.004F), 1F, 1F, 1F, 0.30F);
            // ZX -> verde
            if (!thinDesign && showPlaneZX) Draw.fillBox(builder, stack, offset - planeHalf, -planeThick, offset - planeHalf, offset + planeHalf, planeThick, offset + planeHalf, 0F, 0F, 0F, 1F);
            if (showPlaneZX) Draw.fillBox(builder, stack, offset - (planeHalf - 0.002F), -(planeThick - 0.002F), offset - (planeHalf - 0.002F), offset + (planeHalf - 0.002F), (planeThick - 0.002F), offset + (planeHalf - 0.002F), 0F, 1F, 0F, 1F);
            if (hZX && showPlaneZX) Draw.fillBox(builder, stack, offset - (planeHalf + 0.004F), -(planeThick + 0.004F), offset - (planeHalf + 0.004F), offset + (planeHalf + 0.004F), (planeThick + 0.004F), offset + (planeHalf + 0.004F), 1F, 1F, 1F, 0.30F);
            // YZ -> rojo
            if (!thinDesign && showPlaneYZ) Draw.fillBox(builder, stack, -planeThick, offset - planeHalf, offset - planeHalf, planeThick, offset + planeHalf, offset + planeHalf, 0F, 0F, 0F, 1F);
            if (showPlaneYZ) Draw.fillBox(builder, stack, -(planeThick - 0.002F), offset - (planeHalf - 0.002F), offset - (planeHalf - 0.002F), (planeThick - 0.002F), offset + (planeHalf - 0.002F), offset + (planeHalf - 0.002F), 1F, 0F, 0F, 1F);
            if (hYZ && showPlaneYZ) Draw.fillBox(builder, stack, -(planeThick + 0.004F), offset - (planeHalf + 0.004F), offset - (planeHalf + 0.004F), (planeThick + 0.004F), offset + (planeHalf + 0.004F), offset + (planeHalf + 0.004F), 1F, 1F, 1F, 0.30F);
        }
        else if (this.mode == Mode.ROTATE)
        {
            float radius = 0.22F * this.gizmoScale;
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

        // Halo cuando está en hover: cubrir TODO el ancho del anillo
        if (highlight)
        {
            float hr = tubeR;      // usar todo el grosor del color
            float ha = 0.50F;      // más brillo
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
     * Esfera 3D centrada en el extremo positivo del eje indicado.
     * axis: 'X', 'Y', 'Z'; baseLen: posición del centro a lo largo del eje; radius: radio.
     */
    private void drawSphere3D(BufferBuilder builder, MatrixStack stack, char axis, float baseLen, float radius, float r, float g, float b, float a)
    {
        Matrix4f mat = stack.peek().getPositionMatrix();
        int segU = 24; // latitud
        int segV = 36; // longitud

        float cx = 0F, cy = 0F, cz = 0F;
        if (axis == 'X') cx = baseLen;
        else if (axis == 'Y') cy = baseLen;
        else cz = baseLen;

        for (int iu = 0; iu < segU; iu++)
        {
            double u1 = Math.PI * (double) iu / (double) segU;          // 0..π
            double u2 = Math.PI * (double) (iu + 1) / (double) segU;

            for (int iv = 0; iv < segV; iv++)
            {
                double v1 = (Math.PI * 2.0) * (double) iv / (double) segV; // 0..2π
                double v2 = (Math.PI * 2.0) * (double) (iv + 1) / (double) segV;

                float x11 = cx + (float) (radius * Math.sin(u1) * Math.cos(v1));
                float y11 = cy + (float) (radius * Math.cos(u1));
                float z11 = cz + (float) (radius * Math.sin(u1) * Math.sin(v1));

                float x12 = cx + (float) (radius * Math.sin(u1) * Math.cos(v2));
                float y12 = cy + (float) (radius * Math.cos(u1));
                float z12 = cz + (float) (radius * Math.sin(u1) * Math.sin(v2));

                float x21 = cx + (float) (radius * Math.sin(u2) * Math.cos(v1));
                float y21 = cy + (float) (radius * Math.cos(u2));
                float z21 = cz + (float) (radius * Math.sin(u2) * Math.sin(v1));

                float x22 = cx + (float) (radius * Math.sin(u2) * Math.cos(v2));
                float y22 = cy + (float) (radius * Math.cos(u2));
                float z22 = cz + (float) (radius * Math.sin(u2) * Math.sin(v2));

                builder.vertex(mat, x11, y11, z11).color(r, g, b, a).next();
                builder.vertex(mat, x21, y21, z21).color(r, g, b, a).next();
                builder.vertex(mat, x22, y22, z22).color(r, g, b, a).next();

                builder.vertex(mat, x11, y11, z11).color(r, g, b, a).next();
                builder.vertex(mat, x22, y22, z22).color(r, g, b, a).next();
                builder.vertex(mat, x12, y12, z12).color(r, g, b, a).next();
            }
        }
    }

    /**
     * Detección de eje hovered en 3D usando ray casting hacia cajas alineadas por eje.
     */
    private Axis detectHoveredAxis3D(UIContext input, Area viewport, Matrix4f origin, Matrix4f projection, Matrix4f view)
    {
        if (origin == null || projection == null || view == null) return null;
        // Reiniciar plano hovered por defecto en cada chequeo
        this.hoveredPlane = null;

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

        if (this.mode == Mode.UNIVERSAL)
        {
            // Priorizar anillos de rotación
            Axis rot = detectHoveredAxis3DRotate(rayO, rayD);
            if (rot != null) { this.hoveredSubMode = Mode.ROTATE; this.hoveredPlane = null; return rot; }

            // Solo los cubos de escala al final de cada eje
            float len = 0.25F * this.gizmoScale;
            float[] txS = rayBoxIntersect(rayO, rayD, new Vector3f(len - (0.02F * this.gizmoScale), -(0.045F * this.gizmoScale), -(0.045F * this.gizmoScale)), new Vector3f(len + (0.02F * this.gizmoScale), (0.045F * this.gizmoScale), (0.045F * this.gizmoScale)));
            float[] tyS = rayBoxIntersect(rayO, rayD, new Vector3f(-(0.045F * this.gizmoScale), len - (0.02F * this.gizmoScale), -(0.045F * this.gizmoScale)), new Vector3f((0.045F * this.gizmoScale), len + (0.02F * this.gizmoScale), (0.045F * this.gizmoScale)));
            float[] tzS = rayBoxIntersect(rayO, rayD, new Vector3f(-(0.045F * this.gizmoScale), -(0.045F * this.gizmoScale), len - (0.02F * this.gizmoScale)), new Vector3f((0.045F * this.gizmoScale), (0.045F * this.gizmoScale), len + (0.02F * this.gizmoScale)));

            float bt = Float.POSITIVE_INFINITY; Axis ba = null;
            if (txS != null && txS[0] >= 0 && txS[0] < bt) { bt = txS[0]; ba = Axis.X; }
            if (tyS != null && tyS[0] >= 0 && tyS[0] < bt) { bt = tyS[0]; ba = Axis.Y; }
            if (tzS != null && tzS[0] >= 0 && tzS[0] < bt) { bt = tzS[0]; ba = Axis.Z; }
            if (ba != null) { this.hoveredSubMode = Mode.SCALE; this.hoveredPlane = null; return ba; }

            this.hoveredSubMode = null; this.hoveredPlane = null; return null;
        }

        // Definir AABB por eje (mover/escalar)
        int design = BBSSettings.gizmoDesign.get();
        boolean blockbenchDesign = (design == 2);
        float length = (blockbenchDesign ? 0.35F : 0.25F) * this.gizmoScale; // igual al render
        // Grosor de picking más generoso en Blockbench para facilitar clics
        float thickness = ((this.mode == Mode.SCALE) ? 0.045F : ((blockbenchDesign && (this.mode == Mode.TRANSLATE || this.mode == Mode.PIVOT)) ? 0.025F : 0.015F)) * this.gizmoScale;
        float fudge = (((this.mode == Mode.TRANSLATE) || (this.mode == Mode.PIVOT)) ? 0.06F : ((this.mode == Mode.SCALE) ? 0.045F : 0.02F)) * this.gizmoScale;

        float[] tx = rayBoxIntersect(rayO, rayD, new Vector3f(0F, -thickness/2F, -thickness/2F), new Vector3f(length + fudge, thickness/2F, thickness/2F));
        float[] ty = rayBoxIntersect(rayO, rayD, new Vector3f(-thickness/2F, 0F, -thickness/2F), new Vector3f(thickness/2F, length + fudge, thickness/2F));
        float[] tz = rayBoxIntersect(rayO, rayD, new Vector3f(-thickness/2F, -thickness/2F, 0F), new Vector3f(thickness/2F, thickness/2F, length + fudge));

        float bestT = Float.POSITIVE_INFINITY;
        Axis best = null;

        if (tx != null && tx[0] >= 0 && tx[0] < bestT) { bestT = tx[0]; best = Axis.X; }
        if (ty != null && ty[0] >= 0 && ty[0] < bestT) { bestT = ty[0]; best = Axis.Y; }
        if (tz != null && tz[0] >= 0 && tz[0] < bestT) { bestT = tz[0]; best = Axis.Z; }

        // En modo TRANSLATE, también permitir picking de los planos cercanos al origen
        if (this.mode == Mode.TRANSLATE || this.mode == Mode.PIVOT)
        {
            float po = 0.08F * this.gizmoScale; float ps = 0.020F * this.gizmoScale; float pt = 0.004F * this.gizmoScale;
            float[] pXY = rayBoxIntersect(rayO, rayD,
                    new Vector3f(po - ps, po - ps, -pt), new Vector3f(po + ps, po + ps, pt));
            float[] pZX = rayBoxIntersect(rayO, rayD,
                    new Vector3f(po - ps, -pt, po - ps), new Vector3f(po + ps, pt, po + ps));
            float[] pYZ = rayBoxIntersect(rayO, rayD,
                    new Vector3f(-pt, po - ps, po - ps), new Vector3f(pt, po + ps, po + ps));
            if (BBSSettings.gizmoPlanes.get())
            {
                if (pXY != null && pXY[0] >= 0 && pXY[0] < bestT) { bestT = pXY[0]; best = Axis.X; this.hoveredPlane = Plane.XY; }
                if (pZX != null && pZX[0] >= 0 && pZX[0] < bestT) { bestT = pZX[0]; best = Axis.Z; this.hoveredPlane = Plane.ZX; }
                if (pYZ != null && pYZ[0] >= 0 && pYZ[0] < bestT) { bestT = pYZ[0]; best = Axis.Y; this.hoveredPlane = Plane.YZ; }
            }
        }

        return best;
    }

    /** Picking 3D para el gizmo de rotación: intersección rayo-plano y banda alrededor del radio. */
    private Axis detectHoveredAxis3DRotate(Vector3f rayO, Vector3f rayD)
    {
        float radius = 0.22F * this.gizmoScale;
        // Grosor coherente con el render
        boolean thinDesign = (BBSSettings.gizmoDesign.get() == 1);
        float baseThickness = ((this.mode == Mode.ROTATE) ? (thinDesign ? 0.008F : 0.015F) : (thinDesign ? 0.007F : 0.01F)) * this.gizmoScale;
        float thickness = baseThickness;        // ancho visual del anillo
        float band = thickness * 0.5F + (0.002F * this.gizmoScale); // cubrir todo el color del anillo

        class Hit { Axis a; float t; }
        Hit hitBest = null;

        // Comprobación auxiliar
        java.util.function.BiFunction<Vector3f, Character, Hit> check = (n, c) -> {
            float denom = n.x * rayD.x + n.y * rayD.y + n.z * rayD.z;
            float t;
            float ix, iy, iz;

            if (Math.abs(denom) < 1e-5)
            {
                // Fallback: resolver intersección del rayo con el círculo del anillo
                // en el par de ejes correspondiente mediante ecuación cuadrática.
                float ax, ay, dx, dy;
                if (c == 'Z') { ax = rayO.x; ay = rayO.y; dx = rayD.x; dy = rayD.y; }
                else if (c == 'X') { ax = rayO.y; ay = rayO.z; dx = rayD.y; dy = rayD.z; }
                else { ax = rayO.x; ay = rayO.z; dx = rayD.x; dy = rayD.z; }

                float A = dx * dx + dy * dy;
                float B = 2F * (ax * dx + ay * dy);
                float C = ax * ax + ay * ay - radius * radius;
                float disc = B * B - 4F * A * C;
                if (A > 1e-8 && disc >= 0F)
                {
                    float sqrt = (float) Math.sqrt(disc);
                    float t1 = (-B - sqrt) / (2F * A);
                    float t2 = (-B + sqrt) / (2F * A);
                    // Elegir el menor t positivo (frente del rayo)
                    t = Float.POSITIVE_INFINITY;
                    if (t1 >= 0F) t = Math.min(t, t1);
                    if (t2 >= 0F) t = Math.min(t, t2);
                    if (!Float.isFinite(t)) return null;
                }
                else
                {
                    return null;
                }

                ix = rayO.x + rayD.x * t;
                iy = rayO.y + rayD.y * t;
                iz = rayO.z + rayD.z * t;
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
            // Con gizmo 3D activo, no dibujamos el pixel/box de pivote 2D.
            RenderSystem.enableDepthTest();
            context.batcher.flush();
            return;
        }

        /* Render dependiendo del modo activo (modo 2D clásico) */
        int cx = this.centerX;
        int cy = this.centerY;
        float thickness = this.handleThickness;

        if (this.mode == Mode.TRANSLATE || this.mode == Mode.PIVOT)
        {
            /* Ocultar ejes no activos durante arrastre; si se arrastra un plano, ocultar todas las flechas */
            boolean showX = !this.dragging || (this.activePlane == null && this.activeAxis == Axis.X);
            boolean showY = !this.dragging || (this.activePlane == null && this.activeAxis == Axis.Y);
            boolean showZ = !this.dragging || (this.activePlane == null && this.activeAxis == Axis.Z);
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
                /* esfera en el extremo */
                drawSphereHandle(context, this.endXx, this.endXy, xColor, hx);
            }
            if (showY)
            {
                context.batcher.line(cx, cy, this.endYx, this.endYy, txY + 3F, black);
                context.batcher.line(cx, cy, this.endYx, this.endYy, txY, yColor);
                drawSphereHandle(context, this.endYx, this.endYy, yColor, hy);
            }
            if (showZ)
            {
                context.batcher.line(cx, cy, this.endZx, this.endZy, txZ + 3F, black);
                context.batcher.line(cx, cy, this.endZx, this.endZy, txZ, zColor);
                drawSphereHandle(context, this.endZx, this.endZy, zColor, hz);
            }

            if (BBSSettings.gizmoPlanes.get())
            {
                // Controladores de plano (cubos en overlay) usando colores XYZ
                int[] cXY = planeCenterScreen(Plane.XY);
                int[] cZX = planeCenterScreen(Plane.ZX);
                int[] cYZ = planeCenterScreen(Plane.YZ);
                int ps = 7;
                boolean hXY = (this.hoveredPlane == Plane.XY) || (this.activePlane == Plane.XY);
                boolean hZX = (this.hoveredPlane == Plane.ZX) || (this.activePlane == Plane.ZX);
                boolean hYZ = (this.hoveredPlane == Plane.YZ) || (this.activePlane == Plane.YZ);
                // XY -> azul (eje perpendicular Z)
                int colXY = Colors.A100 | Colors.BLUE;
                // ZX -> verde (eje perpendicular Y)
                int colZX = Colors.A100 | Colors.GREEN;
                // YZ -> rojo (eje perpendicular X)
                int colYZ = Colors.A100 | Colors.RED;
                boolean showPlaneXY = !this.dragging || this.activePlane == Plane.XY;
                boolean showPlaneZX = !this.dragging || this.activePlane == Plane.ZX;
                boolean showPlaneYZ = !this.dragging || this.activePlane == Plane.YZ;
                if (showPlaneXY) context.batcher.box(cXY[0] - ps, cXY[1] - ps, cXY[0] + ps, cXY[1] + ps, Colors.mulRGB(colXY, hXY ? 0.95F : 0.6F));
                if (showPlaneZX) context.batcher.box(cZX[0] - ps, cZX[1] - ps, cZX[0] + ps, cZX[1] + ps, Colors.mulRGB(colZX, hZX ? 0.95F : 0.6F));
                if (showPlaneYZ) context.batcher.box(cYZ[0] - ps, cYZ[1] - ps, cYZ[0] + ps, cYZ[1] + ps, Colors.mulRGB(colYZ, hYZ ? 0.95F : 0.6F));
                if (hXY && showPlaneXY) context.batcher.box(cXY[0] - (ps + 2), cXY[1] - (ps + 2), cXY[0] + (ps + 2), cXY[1] + (ps + 2), Colors.A50 | Colors.WHITE);
                if (hZX && showPlaneZX) context.batcher.box(cZX[0] - (ps + 2), cZX[1] - (ps + 2), cZX[0] + (ps + 2), cZX[1] + (ps + 2), Colors.A50 | Colors.WHITE);
                if (hYZ && showPlaneYZ) context.batcher.box(cYZ[0] - (ps + 2), cYZ[1] - (ps + 2), cYZ[0] + (ps + 2), cYZ[1] + (ps + 2), Colors.A50 | Colors.WHITE);
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
                drawRing(context, this.endXx, this.endXy, 10, 4F, halo);
                context.batcher.line(cx, cy, this.endXx, this.endXy, thickness + 4F, halo);
            }
            if (hy && showY)
            {
                drawRing(context, this.endYx, this.endYy, 10, 4F, halo);
                context.batcher.line(cx, cy, this.endYx, this.endYy, thickness + 4F, halo);
            }
            if (hz && showZ)
            {
                drawRing(context, this.endZx, this.endZy, 10, 4F, halo);
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
        else if (this.mode == Mode.UNIVERSAL)
        {
            // Gizmo unificado: mostrar solo el submodo/axis activo durante uso
            boolean showX = !this.dragging || this.activeAxis == Axis.X;
            boolean showY = !this.dragging || this.activeAxis == Axis.Y;
            boolean showZ = !this.dragging || this.activeAxis == Axis.Z;

            Mode usingSub = this.dragging ? (this.activeSubMode != null ? this.activeSubMode : Mode.TRANSLATE) : null;

            int xColor = (Colors.A100 | Colors.RED);
            int yColor = (Colors.A100 | Colors.GREEN);
            int zColor = (Colors.A100 | Colors.BLUE);

            // Durante uso, solo brilla el gizmo seleccionado
            boolean hx = this.dragging ? (this.activeAxis == Axis.X) : (this.hoveredAxis == Axis.X);
            boolean hy = this.dragging ? (this.activeAxis == Axis.Y) : (this.hoveredAxis == Axis.Y);
            boolean hz = this.dragging ? (this.activeAxis == Axis.Z) : (this.hoveredAxis == Axis.Z);

            float txX = hx ? (thickness + 2F) : thickness;
            float txY = hy ? (thickness + 2F) : thickness;
            float txZ = hz ? (thickness + 2F) : thickness;
            int black = Colors.A100;

            // En modo UNIVERSAL no dibujamos traslación (flechas/lineas)

            // Cubos en extremos (SCALE)
            if (usingSub == null || usingSub == Mode.SCALE)
            {
                if (showX) drawCubeHandle(context, this.endXx, this.endXy, xColor);
                if (showY) drawCubeHandle(context, this.endYx, this.endYy, yColor);
                if (showZ) drawCubeHandle(context, this.endZx, this.endZy, zColor);
            }

            // Anillos (ROTATE) igual al gizmo de rotación
            if (usingSub == null || usingSub == Mode.ROTATE)
            {
                int xRing = (Colors.A100 | Colors.RED);
                int yRing = (Colors.A100 | Colors.GREEN);
                int zRing = (Colors.A100 | Colors.BLUE);
                if (showX) drawRing(context, cx, cy, this.ringRX, thickness, xRing);
                if (showY) drawRing(context, cx, cy, this.ringRY, thickness, yRing);
                if (showZ) drawRing(context, cx, cy, this.ringRZ, thickness, zRing);
            }

            // Sin controladores de plano en modo UNIVERSAL

            // Halo blanco suave: solo el gizmo seleccionado cuando se usa
            int halo = Colors.A100 | Colors.WHITE;
            if (hx && showX) context.batcher.line(cx, cy, this.endXx, this.endXy, thickness + 4F, halo);
            if (hy && showY) context.batcher.line(cx, cy, this.endYx, this.endYy, thickness + 4F, halo);
            if (hz && showZ) context.batcher.line(cx, cy, this.endZx, this.endZy, thickness + 4F, halo);
        }

        /* Centro del pivote: dibujar un cuadrado más visible */
        int half = 5; // tamaño total 10px
        int designIdx = BBSSettings.gizmoDesign.get();
        boolean bbMode = (designIdx == 2) && (this.mode == Mode.PIVOT);
        int pivotColor = (Colors.A100 | (bbMode ? Colors.BLUE : Colors.WHITE));
        context.batcher.box(cx - half, cy - half, cx + half, cy + half, pivotColor);

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

        /* Mover/Escalar/Universal: picking por proximidad a líneas, endpoints y anillos */
        int cx = this.centerX;
        int cy = this.centerY;
        int tol = Math.max(6, this.handleThickness + 4);
        if (this.mode == Mode.UNIVERSAL)
        {
            // Solo rotación y escala en modo universal
            double dx = mx - cx; double dy = my - cy; double d = Math.sqrt(dx * dx + dy * dy);
            int tolRing = Math.max(6, this.handleThickness * 2);
            if (Math.abs(d - this.ringRX) <= tolRing) { this.hoveredSubMode = Mode.ROTATE; this.hoveredPlane = null; return Axis.X; }
            if (Math.abs(d - this.ringRY) <= tolRing) { this.hoveredSubMode = Mode.ROTATE; this.hoveredPlane = null; return Axis.Y; }
            if (Math.abs(d - this.ringRZ) <= tolRing) { this.hoveredSubMode = Mode.ROTATE; this.hoveredPlane = null; return Axis.Z; }

            // Endpoints: cubos -> SCALE
            if (isNear(mx, my, endXx, endXy, hitRadius)) { this.hoveredSubMode = Mode.SCALE; this.hoveredPlane = null; return Axis.X; }
            if (isNear(mx, my, endYx, endYy, hitRadius)) { this.hoveredSubMode = Mode.SCALE; this.hoveredPlane = null; return Axis.Y; }
            if (isNear(mx, my, endZx, endZy, hitRadius)) { this.hoveredSubMode = Mode.SCALE; this.hoveredPlane = null; return Axis.Z; }

            this.hoveredSubMode = null; this.hoveredPlane = null; return null;
        }

        // Modo normal: TRANSLATE o SCALE o PIVOT
        if (this.mode == Mode.TRANSLATE || this.mode == Mode.PIVOT)
        {
            // Incluir controladores de plano en modo TRANSLATE
            int[] cXY = planeCenterScreen(Plane.XY);
            int[] cZX = planeCenterScreen(Plane.ZX);
            int[] cYZ = planeCenterScreen(Plane.YZ);
            int pr = hitRadius;
            if (isNear(mx, my, cXY[0], cXY[1], pr)) { this.hoveredPlane = Plane.XY; this.hoveredSubMode = Mode.TRANSLATE; return Axis.X; }
            if (isNear(mx, my, cZX[0], cZX[1], pr)) { this.hoveredPlane = Plane.ZX; this.hoveredSubMode = Mode.TRANSLATE; return Axis.Z; }
            if (isNear(mx, my, cYZ[0], cYZ[1], pr)) { this.hoveredPlane = Plane.YZ; this.hoveredSubMode = Mode.TRANSLATE; return Axis.Y; }
            this.hoveredPlane = null;
        }

        if (isNearLine(mx, my, cx, cy, endXx, endXy, tol) || isNear(mx, my, endXx, endXy, hitRadius)) return Axis.X;
        if (isNearLine(mx, my, cx, cy, endYx, endYy, tol) || isNear(mx, my, endYx, endYy, hitRadius)) return Axis.Y;
        if (isNearLine(mx, my, cx, cy, endZx, endZy, tol) || isNear(mx, my, endZx, endZy, hitRadius)) return Axis.Z;

        return null;
    }

    /** Centro en pantalla para controladores de plano (basado en endpoints)
     *  Se usa un factor ~0.55 para que queden más cerca del pivote. */
    private int[] planeCenterScreen(Plane p)
    {
        float k = 0.55F; // proporcional desde el pivote hacia el extremo
        int cx = this.centerX, cy = this.centerY;
        return switch (p)
        {
            case XY -> new int[]{ (int)(cx + (endXx - cx) * k), (int)(cy + (endYy - cy) * k) };
            case ZX -> new int[]{ (int)(cx + (endXx - cx) * k), (int)(cy + (endZy - cy) * k) };
            case YZ -> new int[]{ (int)(cx + (endYx - cx) * k), (int)(cy + (endZy - cy) * k) };
        };
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

    /** Dibuja un "handle" circular para simular una esfera en el overlay 2D */
    private void drawSphereHandle(UIRenderingContext context, int x, int y, int color, boolean hovered)
    {
        int fill = Colors.mulRGB(color, 0.85F);
        int outline = Colors.A100; // negro con alpha
        int border = Colors.setA(Colors.WHITE, 0.75F);

        int r = 8; // radio base del "disco"
        // contorno negro suave alrededor
        drawRing(context, x, y, r + 2, 4F, outline);
        // cuerpo principal de la esfera (ring grueso parece disco)
        drawRing(context, x, y, r, 12F, fill);
        // borde blanco fino para definicion
        context.batcher.outline(x - r, y - r, x + r, y + r, border, 1);

        // halo adicional si está hovered
        if (hovered)
        {
            drawRing(context, x, y, r + 4, 3F, Colors.A100 | Colors.WHITE);
        }
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
        // Recorrer todos los modos con la tecla U
        Mode[] order = new Mode[]{ Mode.TRANSLATE, Mode.ROTATE, Mode.SCALE, Mode.PIVOT };
        int idx = 0;
        for (int i = 0; i < order.length; i++) { if (order[i] == this.mode) { idx = i; break; } }
        if (forward) { idx = (idx + 1) % order.length; }
        else { idx = (idx - 1 + order.length) % order.length; }
        this.mode = order[idx];
    }
}
