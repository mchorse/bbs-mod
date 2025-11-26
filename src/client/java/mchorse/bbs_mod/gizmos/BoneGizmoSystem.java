package mchorse.bbs_mod.gizmos;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.UIRenderingContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiFunction;

public class BoneGizmoSystem
{
    private static final BoneGizmoSystem INSTANCE = new BoneGizmoSystem();

    private Mode mode = Mode.TRANSLATE;
    private Mode hoveredSubMode = null;
    private Mode activeSubMode = null;

    private Plane hoveredPlane = null;
    private Plane activePlane = null;

    private Axis hoveredAxis = null;
    private Axis activeAxis = null;

    private boolean dragging = false;
    private boolean lastMouseDown = false;
    private boolean lastCtrlPressedWhileDragging = false;
    private boolean lastShiftPressedWhileDragging = false;
    private Transform dragStart = new Transform();
    private UIPropTransform target;

    private float rotateSign = 1F;
    private boolean useRotation2 = false;

    private float gizmoScale = 0.2F;
    private float minGizmoScale = 0.6F;
    private float maxGizmoScale = 10.0F;
    private float scaleSlope = 0.75F;

    private static final double[] CURSOR_X = new double[1];
    private static final double[] CURSOR_Y = new double[1];
    private final Timer wrapChecker = new Timer(30);
    private int lastX = 0;
    private int lastY = 0;
    private float accumDx = 0F;
    private float accumDy = 0F;

    private int centerX;
    private int centerY;
    private int handleLen = 100;
    private int handleThickness = 5;
    private int hitRadius = 10;

    private int ringRX = 50;
    private int ringRY = 70;
    private int ringRZ = 90;

    private int endXx, endXy;
    private int endYx, endYy;
    private int endZx, endZy;

    public static BoneGizmoSystem get()
    {
        return INSTANCE;
    }

    private static void drawRing(UIRenderingContext context, int cx, int cy, int radius, float thickness, int color)
    {
        int segments = 64;
        double step = Math.PI * 2D / segments;
        float x1 = cx + radius;
        float y1 = cy;

        for (int i = 1; i <= segments; i++)
        {
            double ang = step * i;
            float x2 = (float) (cx + Math.cos(ang) * radius);
            float y2 = (float) (cy + Math.sin(ang) * radius);

            context.batcher.line(x1, y1, x2, y2, thickness + 2F, Colors.A100);
            context.batcher.line(x1, y1, x2, y2, thickness, color);

            x1 = x2;
            y1 = y2;
        }
    }

    private static boolean isNearLine(int mx, int my, int x1, int y1, int x2, int y2, int tol)
    {
        return Intersectionf.distancePointLine(mx, my, x1, y1, x2, y2) <= tol;
    }

    private static boolean isNear(int mx, int my, int x, int y, int r)
    {
        return Vectors.TEMP_2F.set(mx, my).distanceSquared(x, y) <= r * r;
    }

    public void setMode(Mode mode)
    {
        this.mode = mode;
        this.hoveredSubMode = null;
        this.activeSubMode = null;
        this.hoveredPlane = null;
        this.activePlane = null;
    }

    public void toggleRotationChannel()
    {
        this.useRotation2 = !this.useRotation2;
    }

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
        this.hoveredAxis = detectHoveredAxis(input.mouseX, input.mouseY);

        boolean mouseDown = Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

        if (!this.dragging && mouseDown && !this.lastMouseDown && this.hoveredAxis != null)
        {
            this.dragging = true;
            this.activeAxis = this.hoveredAxis;
            this.activeSubMode = (this.mode == Mode.UNIVERSAL) ? (this.hoveredSubMode != null ? this.hoveredSubMode : Mode.ROTATE) : this.mode;
            this.activePlane = this.hoveredPlane;
            this.lastX = input.mouseX;
            this.lastY = input.mouseY;
            this.accumDx = 0F;
            this.accumDy = 0F;
            this.lastCtrlPressedWhileDragging = Window.isCtrlPressed();
            this.lastShiftPressedWhileDragging = Window.isShiftPressed();

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
                    warped = true;

                    this.wrapChecker.mark();
                }
                else if (rawX >= w - border)
                {
                    Window.moveCursor(borderPadding, (int) mc.mouse.getY());

                    this.lastX = (int) (borderPadding / fx);
                    warped = true;

                    this.wrapChecker.mark();
                }

                if (rawY <= border)
                {
                    Window.moveCursor((int) mc.mouse.getX(), h - borderPadding);

                    this.lastY = input.menu.height - (int) (borderPadding / fy);
                    warped = true;

                    this.wrapChecker.mark();
                }
                else if (rawY >= h - border)
                {
                    Window.moveCursor((int) mc.mouse.getX(), borderPadding);

                    this.lastY = (int) (borderPadding / fy);
                    warped = true;

                    this.wrapChecker.mark();
                }
            }

            boolean ctrlNow = Window.isCtrlPressed();
            boolean shiftNow = Window.isShiftPressed();
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
                this.accumDx += input.mouseX - this.lastX;
                this.accumDy += input.mouseY - this.lastY;
                this.lastX = input.mouseX;
                this.lastY = input.mouseY;
            }

            float factor = switch (this.mode)
            {
                case TRANSLATE -> 0.02F;
                case SCALE -> 0.01F;
                case ROTATE -> 0.3F;
                case UNIVERSAL ->
                {
                    Mode opInner = (this.activeSubMode != null) ? this.activeSubMode : Mode.TRANSLATE;

                    yield (opInner == Mode.TRANSLATE) ? 0.02F : (opInner == Mode.SCALE ? 0.01F : 0.3F);
                }
            };

            float delta = this.accumDx * factor;

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

                if (Window.isCtrlPressed())
                {
                    if (Window.isShiftPressed())
                    {
                        rz += this.accumDx * factor * 10F;
                        ry += -this.accumDy * factor * 10F;
                    }
                    else
                    {

                        ry += this.accumDx * factor * 10F;
                        rx += -this.accumDy * factor * 10F;
                    }
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

    public void update(UIContext input, Area viewport, Matrix4f origin, Matrix4f projection, Matrix4f view, UIPropTransform target)
    {
        this.target = target;

        if (viewport == null)
        {
            return;
        }

        if (origin != null && projection != null && view != null)
        {
            if (BBSSettings.gizmosEnabled.get())
            {
                this.hoveredAxis = detectHoveredAxis3D(input, viewport, origin, projection, view);

                Matrix4f mvp = new Matrix4f(projection).mul(view).mul(origin);
                Vector4f cp = new Vector4f();

                mvp.transform(cp);

                if (cp.w != 0)
                {
                    float ndcXc = cp.x / cp.w;
                    float ndcYc = cp.y / cp.w;

                    this.centerX = viewport.x + (int) (((ndcXc + 1F) * 0.5F) * viewport.w);
                    this.centerY = viewport.y + (int) (((-ndcYc + 1F) * 0.5F) * viewport.h);
                }

                try
                {
                    Vector4f camW4 = new Vector4f();
                    Vector4f camWorld = new Matrix4f(view).invert(new Matrix4f()).transform(camW4);
                    camWorld.div(camWorld.w);

                    Vector4f pivotW4 = new Vector4f();
                    Vector4f pivotWorld = new Matrix4f(origin).transform(pivotW4);
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

                boolean mouseDown = Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT);

                if (!this.dragging && mouseDown && !this.lastMouseDown && this.hoveredAxis != null)
                {
                    this.dragging = true;
                    this.activeAxis = this.hoveredAxis;
                    this.activeSubMode = (this.mode == Mode.UNIVERSAL) ? (this.hoveredSubMode != null ? this.hoveredSubMode : Mode.ROTATE) : this.mode;
                    this.activePlane = this.hoveredPlane;
                    this.lastX = input.mouseX;
                    this.lastY = input.mouseY;
                    this.accumDx = 0F;
                    this.accumDy = 0F;
                    this.lastCtrlPressedWhileDragging = Window.isCtrlPressed();
                    this.lastShiftPressedWhileDragging = Window.isShiftPressed();

                    if (this.target != null && this.target.getTransform() != null)
                    {
                        this.dragStart.copy(this.target.getTransform());
                    }

                    try
                    {
                        float nx = (float) (((input.mouseX - viewport.x) / (double) viewport.w) * 2.0 - 1.0);
                        float ny = (float) ( -(((input.mouseY - viewport.y) / (double) viewport.h) * 2.0 - 1.0) );
                        Matrix4f invPV = new Matrix4f(projection).mul(view).invert(new Matrix4f());
                        Vector4f nearClip = new Vector4f(nx, ny, -1F, 1F);
                        Vector4f farClip  = new Vector4f(nx, ny,  1F, 1F);
                        Vector4f nearWorld = invPV.transform(new Vector4f(nearClip));
                        Vector4f farWorld  = invPV.transform(new Vector4f(farClip));

                        nearWorld.div(nearWorld.w);
                        farWorld.div(farWorld.w);

                        Matrix4f invOrigin = new Matrix4f(origin).invert(new Matrix4f());
                        Vector4f nearLocal4 = invOrigin.transform(new Vector4f(nearWorld));
                        Vector4f farLocal4  = invOrigin.transform(new Vector4f(farWorld));
                        Vector3f rayO = new Vector3f(nearLocal4.x, nearLocal4.y, nearLocal4.z);
                        Vector3f rayF = new Vector3f(farLocal4.x, farLocal4.y, farLocal4.z);
                        Vector3f rayD = rayF.sub(rayO, new Vector3f());

                        rayD.normalize();

                        Vector3f n = switch (this.activeAxis)
                        {
                            case X -> new Vector3f(1F, 0F, 0F);
                            case Y -> new Vector3f(0F, 1F, 0F);
                            case Z -> new Vector3f(0F, 0F, 1F);
                        };

                        float denom = n.x * rayD.x + n.y * rayD.y + n.z * rayD.z;

                        this.rotateSign = (denom >= 0F) ? -1F : 1F;
                    }
                    catch (Throwable t)
                    {
                        this.rotateSign = 1F;
                    }
                }

                if (this.dragging && !mouseDown && this.lastMouseDown)
                {
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
                    boolean ctrlNow = Window.isCtrlPressed();
                    boolean shiftNow = Window.isShiftPressed();
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

                    int stepX = input.mouseX - this.lastX;
                    int stepY = input.mouseY - this.lastY;
                    this.accumDx += stepX;
                    this.accumDy += stepY;
                    this.lastX = input.mouseX;
                    this.lastY = input.mouseY;

                    Mode op = (this.mode == Mode.UNIVERSAL) ? (this.activeSubMode != null ? this.activeSubMode : Mode.ROTATE) : this.mode;
                    float factor = switch (op)
                    {
                        case TRANSLATE, UNIVERSAL -> 0.02F;
                        case SCALE -> 0.01F;
                        case ROTATE -> 0.3F;
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
                                case XY ->
                                {
                                    x = this.dragStart.translate.x + delta;
                                    y = this.dragStart.translate.y - this.accumDy * factor;
                                }
                                case ZX ->
                                {
                                    z = this.dragStart.translate.z + delta;
                                    x = this.dragStart.translate.x - this.accumDy * factor;
                                }
                                case YZ ->
                                {
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

                        if (Window.isCtrlPressed())
                        {
                            if (Window.isShiftPressed())
                            {
                                float zDelta = this.accumDx * factor * 10F;
                                float yDelta = -this.accumDy * factor * 10F;

                                rz = rz + zDelta;
                                ry = ry + yDelta;
                            }
                            else
                            {
                                float yDelta = this.accumDx * factor * 10F;
                                float xDelta = -this.accumDy * factor * 10F;

                                ry = ry + yDelta;
                                rx = rx + xDelta;
                            }
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
                }

                this.lastMouseDown = Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT);

                return;
            }

            Matrix4f mvp = new Matrix4f(projection).mul(view).mul(origin);
            Vector4f p = new Vector4f();

            mvp.transform(p);

            if (p.w != 0)
            {
                float ndcX = p.x / p.w;
                float ndcY = p.y / p.w;

                int px = viewport.x + (int) (((ndcX + 1F) * 0.5F) * viewport.w);
                int py = viewport.y + (int) (((-ndcY + 1F) * 0.5F) * viewport.h);

                this.centerX = px;
                this.centerY = py;

                float axisLen = 0.6F;
                Vector4f p0World = new Vector4f();

                origin.transform(p0World);

                boolean useLocal = this.target != null && this.target.isLocal();

                Vector4f dx = new Vector4f(axisLen, 0F, 0F, 1F);
                Vector4f dy = new Vector4f(0F, axisLen, 0F, 1F);
                Vector4f dz = new Vector4f(0F, 0F, axisLen, 1F);

                if (useLocal)
                {
                    origin.transform(dx);
                    origin.transform(dy);
                    origin.transform(dz);

                    dx.add(p0World.x, p0World.y, p0World.z, 0F);
                    dy.add(p0World.x, p0World.y, p0World.z, 0F);
                    dz.add(p0World.x, p0World.y, p0World.z, 0F);
                }
                else
                {
                    dx.set(p0World.x + axisLen, p0World.y, p0World.z, 1F);
                    dy.set(p0World.x, p0World.y + axisLen, p0World.z, 1F);
                    dz.set(p0World.x, p0World.y, p0World.z + axisLen, 1F);
                }

                Vector4f sx = new Vector4f(dx);
                Vector4f sy = new Vector4f(dy);
                Vector4f sz = new Vector4f(dz);
                Matrix4f pv = new Matrix4f(projection).mul(view);

                pv.transform(sx);
                pv.transform(sy);
                pv.transform(sz);

                if (sx.w != 0F)
                {
                    float ndcXx = sx.x / sx.w;
                    float ndcXy = sx.y / sx.w;

                    this.endXx = viewport.x + (int) (((ndcXx + 1F) * 0.5F) * viewport.w);
                    this.endXy = viewport.y + (int) (((-ndcXy + 1F) * 0.5F) * viewport.h);
                }

                if (sy.w != 0F)
                {
                    float ndcYx = sy.x / sy.w;
                    float ndcYy = sy.y / sy.w;

                    this.endYx = viewport.x + (int) (((ndcYx + 1F) * 0.5F) * viewport.w);
                    this.endYy = viewport.y + (int) (((-ndcYy + 1F) * 0.5F) * viewport.h);
                }

                if (sz.w != 0F)
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

            this.endXx = this.centerX + handleLen;
            this.endXy = this.centerY;
            this.endYx = this.centerX;
            this.endYy = this.centerY - handleLen;
            this.endZx = this.centerX;
            this.endZy = this.centerY + handleLen;
        }

        this.hoveredAxis = detectHoveredAxis(input.mouseX, input.mouseY);

        boolean mouseDown = Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT);

        if (!this.dragging && mouseDown && !this.lastMouseDown && this.hoveredAxis != null)
        {
            this.dragging = true;
            this.activeAxis = this.hoveredAxis;
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
                case UNIVERSAL ->
                {
                    Mode opInner = (this.activeSubMode != null) ? this.activeSubMode : Mode.TRANSLATE;

                    yield (opInner == Mode.TRANSLATE) ? 0.02F : (opInner == Mode.SCALE ? 0.01F : 0.3F);
                }
            };

            float delta = this.accumDx * factor;

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
                    if (this.activeAxis == Axis.Y) y = this.dragStart.translate.y - this.accumDy * factor;
                    if (this.activeAxis == Axis.Z) z = this.dragStart.translate.z + delta;
                }
                else
                {
                    switch (this.activePlane)
                    {
                        case XY ->
                        {
                            x = this.dragStart.translate.x + delta;
                            y = this.dragStart.translate.y - this.accumDy * factor;
                        }
                        case ZX ->
                        {
                            z = this.dragStart.translate.z + delta;
                            x = this.dragStart.translate.x - this.accumDy * factor;
                        }
                        case YZ ->
                        {
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
                float rx = (float) Math.toDegrees(this.dragStart.rotate.x);
                float ry = (float) Math.toDegrees(this.dragStart.rotate.y);
                float rz = (float) Math.toDegrees(this.dragStart.rotate.z);

                if (this.useRotation2)
                {
                    rx = (float) Math.toDegrees(this.dragStart.rotate2.x);
                    ry = (float) Math.toDegrees(this.dragStart.rotate2.y);
                    rz = (float) Math.toDegrees(this.dragStart.rotate2.z);
                }

                if (Window.isCtrlPressed())
                {
                    if (Window.isShiftPressed())
                    {
                        float zDelta = this.accumDx * factor * 10F;
                        float yDelta = -this.accumDy * factor * 10F;

                        rz = rz + zDelta;
                        ry = ry + yDelta;
                    }
                    else
                    {
                        float yDelta = this.accumDx * factor * 10F;
                        float xDelta = -this.accumDy * factor * 10F;

                        ry = ry + yDelta;
                        rx = rx + xDelta;
                    }
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

    private float clampScale(float v)
    {
        return Math.max(0.001F, v);
    }

    public void render3D(MatrixStack stack)
    {
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        float baseLength = 0.25F;
        float length = baseLength * this.gizmoScale;
        float thickness = 0.02F * this.gizmoScale;
        float outlinePad = 0.01F * this.gizmoScale;
        float slabThick = 0.018F * this.gizmoScale;

        float cubeSmall = 0.022F * this.gizmoScale;
        float cubeBig = 0.045F * this.gizmoScale;

        float connectFudge = this.mode == Mode.TRANSLATE ? 0.03F : (this.mode == Mode.SCALE ? slabThick : (cubeBig + thickness));

        boolean showX = !this.dragging || (this.activePlane == null && this.activeAxis == Axis.X);
        boolean showY = !this.dragging || (this.activePlane == null && this.activeAxis == Axis.Y);
        boolean showZ = !this.dragging || (this.activePlane == null && this.activeAxis == Axis.Z);

        if (this.mode == Mode.SCALE)
        {
            if (showX)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, length + connectFudge, 0, 0, thickness + outlinePad, 0F, 0F, 0F, 1F);
                Draw.fillBoxTo(builder, stack, 0, 0, 0, length + connectFudge, 0, 0, thickness, 1F, 0F, 0F, 1F);
            }

            if (showY)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, length + connectFudge, 0, thickness + outlinePad, 0F, 0F, 0F, 1F);
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, length + connectFudge, 0, thickness, 0F, 1F, 0F, 1F);
            }

            if (showZ)
            {
                Draw.fillBox(builder, stack, -(thickness + outlinePad) / 2F, -(thickness + outlinePad) / 2F, 0F, (thickness + outlinePad) / 2F, (thickness + outlinePad) / 2F, length + connectFudge, 0F, 0F, 0F, 1F);
                Draw.fillBox(builder, stack, -thickness / 2F, -thickness / 2F, 0F, thickness / 2F, thickness / 2F, length + connectFudge, 0F, 0F, 1F, 1F);
            }
        }

        if (this.mode == Mode.TRANSLATE)
        {
            float headLen = 0.08F * this.gizmoScale;
            float headWidth = 0.06F * this.gizmoScale;
            float headRadius = headWidth * 0.5F;
            float sphereR = 0.045F * this.gizmoScale;

            float lengthBar = length + connectFudge;
            float barEnd = lengthBar - headLen - 0.002F;

            boolean hx = this.hoveredAxis == Axis.X;
            boolean hy = this.hoveredAxis == Axis.Y;
            boolean hz = this.hoveredAxis == Axis.Z;

            float txX = hx ? thickness * 1.5F : thickness;
            float txY = hy ? thickness * 1.5F : thickness;
            float txZ = hz ? thickness * 1.5F : thickness;

            if (showX)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, barEnd, 0, 0, txX + outlinePad, 0F, 0F, 0F, 1F);
                Draw.fillBoxTo(builder, stack, 0, 0, 0, barEnd, 0, 0, txX, 1F, 0F, 0F, 1F);
            }

            if (showY)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, barEnd, 0, txY + outlinePad, 0F, 0F, 0F, 1F);
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, barEnd, 0, txY, 0F, 1F, 0F, 1F);
            }

            if (showZ)
            {
                Draw.fillBox(builder, stack, -(txZ + outlinePad) / 2F, -(txZ + outlinePad) / 2F, 0F, (txZ + outlinePad) / 2F, (txZ + outlinePad) / 2F, barEnd, 0F, 0F, 0F, 1F);
                Draw.fillBox(builder, stack, -txZ / 2F, -txZ / 2F, 0F, txZ / 2F, txZ / 2F, barEnd, 0F, 0F, 1F, 1F);
            }

            if (this.mode == Mode.TRANSLATE)
            {
                if (showX)
                {
                    drawCone3D(builder, stack, 'X', lengthBar, headLen + outlinePad * 0.5F, headRadius + outlinePad, 0F, 0F, 0F, 1F);
                    drawCone3D(builder, stack, 'X', lengthBar, headLen, headRadius, 1F, 0F, 0F, 1F);
                }

                if (showY)
                {
                    drawCone3D(builder, stack, 'Y', lengthBar, headLen + outlinePad * 0.5F, headRadius + outlinePad, 0F, 0F, 0F, 1F);
                    drawCone3D(builder, stack, 'Y', lengthBar, headLen, headRadius, 0F, 1F, 0F, 1F);
                }

                if (showZ)
                {
                    drawCone3D(builder, stack, 'Z', lengthBar, headLen + outlinePad * 0.5F, headRadius + outlinePad, 0F, 0F, 0F, 1F);
                    drawCone3D(builder, stack, 'Z', lengthBar, headLen, headRadius, 0F, 0F, 1F, 1F);
                }
            }

            drawEndCube(builder, stack, 0, 0, 0, cubeSmall + outlinePad, 0F, 0F, 0F);
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall, 1F, 1F, 1F);

            if (hx && showX)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, barEnd, 0, 0, thickness * 2F, 1F, 1F, 1F, 0.25F);

                if (this.mode == Mode.TRANSLATE) drawCone3D(builder, stack, 'X', lengthBar, headLen, headRadius, 1F, 1F, 1F, 0.35F);
                else drawSphere3D(builder, stack, 'X', lengthBar, sphereR, 1F, 1F, 1F, 0.35F);
            }

            if (hy && showY)
            {
                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, barEnd, 0, thickness * 2F, 1F, 1F, 1F, 0.25F);

                if (this.mode == Mode.TRANSLATE) drawCone3D(builder, stack, 'Y', lengthBar, headLen, headRadius, 1F, 1F, 1F, 0.35F);
                else drawSphere3D(builder, stack, 'Y', lengthBar, sphereR, 1F, 1F, 1F, 0.35F);
            }

            if (hz && showZ)
            {
                Draw.fillBox(builder, stack, -thickness, -thickness, 0F, thickness, thickness, barEnd, 1F, 1F, 1F, 0.25F);

                if (this.mode == Mode.TRANSLATE) drawCone3D(builder, stack, 'Z', lengthBar, headLen, headRadius, 1F, 1F, 1F, 0.35F);
                else drawSphere3D(builder, stack, 'Z', lengthBar, sphereR, 1F, 1F, 1F, 0.35F);
            }

            float offset = 0.08F * this.gizmoScale;
            float planeHalf = 0.020F * this.gizmoScale;
            float planeThick = 0.004F * this.gizmoScale;
            boolean hXY = (this.hoveredPlane == Plane.XY) || (this.activePlane == Plane.XY);
            boolean hZX = (this.hoveredPlane == Plane.ZX) || (this.activePlane == Plane.ZX);
            boolean hYZ = (this.hoveredPlane == Plane.YZ) || (this.activePlane == Plane.YZ);

            boolean showPlaneXY = !this.dragging || this.activePlane == Plane.XY;
            boolean showPlaneZX = !this.dragging || this.activePlane == Plane.ZX;
            boolean showPlaneYZ = !this.dragging || this.activePlane == Plane.YZ;

            if (showPlaneXY) { Draw.fillBox(builder, stack,
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

            if (showPlaneZX) { Draw.fillBox(builder, stack,
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

            if (showPlaneYZ) { Draw.fillBox(builder, stack,
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
        else if (this.mode == Mode.SCALE)
        {
            boolean hx = (this.hoveredAxis == Axis.X);
            boolean hy = (this.hoveredAxis == Axis.Y);
            boolean hz = (this.hoveredAxis == Axis.Z);

            if (showX)
            {
                stack.push();
                stack.translate(length, 0F, 0F);

                Draw.fillBox(builder, stack, -(slabThick + outlinePad), -(cubeBig + outlinePad), -(cubeBig + outlinePad), (slabThick + outlinePad), (cubeBig + outlinePad), (cubeBig + outlinePad), 0F, 0F, 0F, 1F);
                Draw.fillBox(builder, stack, -slabThick, -cubeBig, -cubeBig, slabThick, cubeBig, cubeBig, 1F, 0F, 0F, 1F);
                stack.pop();
            }

            if (showY)
            {
                stack.push();
                stack.translate(0F, length, 0F);

                Draw.fillBox(builder, stack, -(cubeBig + outlinePad), -(slabThick + outlinePad), -(cubeBig + outlinePad), (cubeBig + outlinePad), (slabThick + outlinePad), (cubeBig + outlinePad), 0F, 0F, 0F, 1F);
                Draw.fillBox(builder, stack, -cubeBig, -slabThick, -cubeBig, cubeBig, slabThick, cubeBig, 0F, 1F, 0F, 1F);
                stack.pop();
            }

            if (showZ)
            {
                stack.push();
                stack.translate(0F, 0F, length);

                Draw.fillBox(builder, stack, -(cubeBig + outlinePad), -(cubeBig + outlinePad), -(slabThick + outlinePad), (cubeBig + outlinePad), (cubeBig + outlinePad), (slabThick + outlinePad), 0F, 0F, 0F, 1F);
                Draw.fillBox(builder, stack, -cubeBig, -cubeBig, -slabThick, cubeBig, cubeBig, slabThick, 0F, 0F, 1F, 1F);
                stack.pop();
            }

            if (hx && showX)
            {
                stack.push();
                stack.translate(length, 0, 0);
                Draw.fillBox(builder, stack, -(slabThick + 0.006F), -(cubeBig + 0.01F), -(cubeBig + 0.01F), (slabThick + 0.006F), (cubeBig + 0.01F), (cubeBig + 0.01F), 1F, 1F, 1F, 0.30F);
                stack.pop();

                Draw.fillBoxTo(builder, stack, 0, 0, 0, length + connectFudge, 0, 0, thickness * 1.6F, 1F, 1F, 1F, 0.25F);
            }
            if (hy && showY)
            {
                stack.push();
                stack.translate(0, length, 0);
                Draw.fillBox(builder, stack, -(cubeBig + 0.01F), -(slabThick + 0.006F), -(cubeBig + 0.01F), (cubeBig + 0.01F), (slabThick + 0.006F), (cubeBig + 0.01F), 1F, 1F, 1F, 0.30F);
                stack.pop();

                Draw.fillBoxTo(builder, stack, 0, 0, 0, 0, length + connectFudge, 0, thickness * 1.6F, 1F, 1F, 1F, 0.25F);
            }
            if (hz && showZ)
            {
                stack.push();
                stack.translate(0, 0, length);
                Draw.fillBox(builder, stack, -(cubeBig + 0.01F), -(cubeBig + 0.01F), -(slabThick + 0.006F), (cubeBig + 0.01F), (cubeBig + 0.01F), (slabThick + 0.006F), 1F, 1F, 1F, 0.30F);
                stack.pop();

                Draw.fillBox(builder, stack, -thickness, -thickness, 0F, thickness, thickness, length + connectFudge, 1F, 1F, 1F, 0.25F);
            }

            drawEndCube(builder, stack, 0, 0, 0, cubeSmall + outlinePad, 0F, 0F, 0F);
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall, 1F, 1F, 1F);
        }
        else if (this.mode == Mode.UNIVERSAL)
        {
            Mode usingSub = this.dragging ? this.activeSubMode : null;

            boolean hx = this.dragging ? (this.activeAxis == Axis.X) : (this.hoveredAxis == Axis.X);
            boolean hy = this.dragging ? (this.activeAxis == Axis.Y) : (this.hoveredAxis == Axis.Y);
            boolean hz = this.dragging ? (this.activeAxis == Axis.Z) : (this.hoveredAxis == Axis.Z);

            float lengthBar = 0.25F + 0.03F;

            if (usingSub == null || usingSub == Mode.SCALE)
            {
                float slabGap = 0.02F;
                float slabOffX = lengthBar + slabGap;
                float slabOffY = lengthBar + slabGap;
                float slabOffZ = lengthBar + slabGap;

                if (showX)
                {
                    stack.push();
                    stack.translate(slabOffX, 0F, 0F);
                    Draw.fillBox(builder, stack, -(slabThick + outlinePad), -(cubeBig + outlinePad), -(cubeBig + outlinePad), (slabThick + outlinePad), (cubeBig + outlinePad), (cubeBig + outlinePad), 0F, 0F, 0F, 1F);
                    Draw.fillBox(builder, stack, -slabThick, -cubeBig, -cubeBig, slabThick, cubeBig, cubeBig, 1F, 0F, 0F, 0.75F);

                    if (hx)
                    {
                        Draw.fillBox(builder, stack, -(slabThick + 0.006F), -(cubeBig + 0.01F), -(cubeBig + 0.01F), (slabThick + 0.006F), (cubeBig + 0.01F), (cubeBig + 0.01F), 1F, 1F, 1F, 0.30F);
                    }

                    stack.pop();
                }

                if (showY)
                {
                    stack.push();
                    stack.translate(0F, slabOffY, 0F);
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

            if (usingSub == null || usingSub == Mode.ROTATE)
            {
                float radius = 0.22F; float thicknessRing = 0.01F; float sweep = 360F;
                RenderSystem.disableCull();
                drawEndCube(builder, stack, 0, 0, 0, 0.022F + outlinePad, 0F, 0F, 0F);
                drawEndCube(builder, stack, 0, 0, 0, 0.022F, 1F, 1F, 1F);
                if (showZ) { drawRingArc3D(builder, stack, 'Z', radius, thicknessRing + outlinePad, 0F, 0F, 0F, 0F, sweep, false); drawRingArc3D(builder, stack, 'Z', radius, thicknessRing, 0F, 0F, 1F, 0F, sweep, hz); }
                if (showX) { drawRingArc3D(builder, stack, 'X', radius, thicknessRing + outlinePad, 0F, 0F, 0F, 0F, sweep, false); drawRingArc3D(builder, stack, 'X', radius, thicknessRing, 1F, 0F, 0F, 0F, sweep, hx); }
                if (showY) { drawRingArc3D(builder, stack, 'Y', radius, thicknessRing + outlinePad, 0F, 0F, 0F, 0F, sweep, false); drawRingArc3D(builder, stack, 'Y', radius, thicknessRing, 0F, 1F, 0F, 0F, sweep, hy); }
                RenderSystem.enableCull();
            }

            float offset = 0.08F; float planeHalf = 0.020F; float planeThick = 0.004F;
            boolean hXY = (this.hoveredPlane == Plane.XY) || (this.activePlane == Plane.XY);
            boolean hZX = (this.hoveredPlane == Plane.ZX) || (this.activePlane == Plane.ZX);
            boolean hYZ = (this.hoveredPlane == Plane.YZ) || (this.activePlane == Plane.YZ);
            boolean showPlaneXY = !this.dragging || this.activePlane == Plane.XY;
            boolean showPlaneZX = !this.dragging || this.activePlane == Plane.ZX;
            boolean showPlaneYZ = !this.dragging || this.activePlane == Plane.YZ;

            if (showPlaneXY) Draw.fillBox(builder, stack, offset - planeHalf, offset - planeHalf, -planeThick, offset + planeHalf, offset + planeHalf, planeThick, 0F, 0F, 0F, 1F);
            if (showPlaneXY) Draw.fillBox(builder, stack, offset - (planeHalf - 0.002F), offset - (planeHalf - 0.002F), -(planeThick - 0.002F), offset + (planeHalf - 0.002F), offset + (planeHalf - 0.002F), (planeThick - 0.002F), 0F, 0F, 1F, 1F);
            if (hXY && showPlaneXY) Draw.fillBox(builder, stack, offset - (planeHalf + 0.004F), offset - (planeHalf + 0.004F), -(planeThick + 0.004F), offset + (planeHalf + 0.004F), offset + (planeHalf + 0.004F), (planeThick + 0.004F), 1F, 1F, 1F, 0.30F);

            if (showPlaneZX) Draw.fillBox(builder, stack, offset - planeHalf, -planeThick, offset - planeHalf, offset + planeHalf, planeThick, offset + planeHalf, 0F, 0F, 0F, 1F);
            if (showPlaneZX) Draw.fillBox(builder, stack, offset - (planeHalf - 0.002F), -(planeThick - 0.002F), offset - (planeHalf - 0.002F), offset + (planeHalf - 0.002F), (planeThick - 0.002F), offset + (planeHalf - 0.002F), 0F, 1F, 0F, 1F);
            if (hZX && showPlaneZX) Draw.fillBox(builder, stack, offset - (planeHalf + 0.004F), -(planeThick + 0.004F), offset - (planeHalf + 0.004F), offset + (planeHalf + 0.004F), (planeThick + 0.004F), offset + (planeHalf + 0.004F), 1F, 1F, 1F, 0.30F);

            if (showPlaneYZ) Draw.fillBox(builder, stack, -planeThick, offset - planeHalf, offset - planeHalf, planeThick, offset + planeHalf, offset + planeHalf, 0F, 0F, 0F, 1F);
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

            RenderSystem.disableCull();

            drawEndCube(builder, stack, 0, 0, 0, cubeSmall + outlinePad, 0F, 0F, 0F);
            drawEndCube(builder, stack, 0, 0, 0, cubeSmall, 1F, 1F, 1F);

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
        BufferRenderer.drawWithGlobalProgram(builder.end());

        if (this.mode == Mode.ROTATE)
        {
            RenderSystem.enableCull();
        }

        RenderSystem.enableDepthTest();
    }

    private void drawEndCube(BufferBuilder builder, MatrixStack stack, float x, float y, float z, float s, float r, float g, float b)
    {
        stack.push();
        stack.translate(x, y, z);

        Draw.fillBox(builder, stack, -s, -s, -s, s, s, s, r, g, b, 1F);

        stack.pop();
    }

    private static void drawRingArc3D(BufferBuilder builder, MatrixStack stack, char axis, float radius, float thickness, float r, float g, float b, float startDeg, float sweepDeg, boolean highlight)
    {
        int segU = 96;
        int segV = 24;
        double u0 = Math.toRadians(startDeg);
        double uStep = Math.toRadians(sweepDeg / (double) segU);
        double vStep = Math.PI * 2D / (double) segV;

        float tubeR = thickness * 0.5F;
        Matrix4f mat = stack.peek().getPositionMatrix();

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
                else
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

                builder.vertex(mat, x11, y11, z11).color(r, g, b, 1F).next();
                builder.vertex(mat, x12, y12, z12).color(r, g, b, 1F).next();
                builder.vertex(mat, x22, y22, z22).color(r, g, b, 1F).next();

                builder.vertex(mat, x11, y11, z11).color(r, g, b, 1F).next();
                builder.vertex(mat, x22, y22, z22).color(r, g, b, 1F).next();
                builder.vertex(mat, x21, y21, z21).color(r, g, b, 1F).next();
            }
        }

        if (highlight)
        {
            float hr = tubeR;
            float ha = 0.50F;

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
                    else
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
        else
        {
            Draw.fillBoxTo(builder, stack, 0, 0, baseLen, 0, +headWidth / 2F, baseLen - headLen, thickness + 0.004F, r, g, b, a);
            Draw.fillBoxTo(builder, stack, 0, 0, baseLen, 0, -headWidth / 2F, baseLen - headLen, thickness + 0.004F, r, g, b, a);
        }
    }

    private static void drawCone3D(BufferBuilder builder, MatrixStack stack, char axis, float baseLen, float headLen, float radius, float r, float g, float b, float a)
    {
        Matrix4f mat = stack.peek().getPositionMatrix();
        int segments = 20;

        float ax = 0F, ay = 0F, az = 0F;
        float bx = 0F, by = 0F, bz = 0F;

        if (axis == 'X')
        {
            ax = baseLen;
            bx = baseLen - headLen;
        }
        else if (axis == 'Y')
        {
            ay = baseLen;
            by = baseLen - headLen;
        }
        else
        {
            az = baseLen;
            bz = baseLen - headLen;
        }

        for (int i = 0; i < segments; i++)
        {
            double a1 = Math.PI * 2D * i / segments;
            double a2 = Math.PI * 2D * (i + 1) / segments;

            float x1 = bx, y1 = by, z1 = bz;
            float x2 = bx, y2 = by, z2 = bz;

            if (axis == 'X')
            {
                y1 = (float) (Math.cos(a1) * radius);
                z1 = (float) (Math.sin(a1) * radius);

                y2 = (float) (Math.cos(a2) * radius);
                z2 = (float) (Math.sin(a2) * radius);
            }
            else if (axis == 'Y')
            {
                x1 = (float) (Math.cos(a1) * radius);
                z1 = (float) (Math.sin(a1) * radius);

                x2 = (float) (Math.cos(a2) * radius);
                z2 = (float) (Math.sin(a2) * radius);
            }
            else
            {
                x1 = (float) (Math.cos(a1) * radius);
                y1 = (float) (Math.sin(a1) * radius);

                x2 = (float) (Math.cos(a2) * radius);
                y2 = (float) (Math.sin(a2) * radius);
            }

            float aa = Math.max(0F, a - 0.2F);

            builder.vertex(mat, ax, ay, az).color(r, g, b, a).next();
            builder.vertex(mat, x1, y1, z1).color(r, g, b, a).next();
            builder.vertex(mat, x2, y2, z2).color(r, g, b, a).next();

            builder.vertex(mat, bx, by, bz).color(r, g, b, aa).next();
            builder.vertex(mat, x2, y2, z2).color(r, g, b, aa).next();
            builder.vertex(mat, x1, y1, z1).color(r, g, b, aa).next();
        }
    }

    private static void drawSphere3D(BufferBuilder builder, MatrixStack stack, char axis, float baseLen, float radius, float r, float g, float b, float a)
    {
        Matrix4f mat = stack.peek().getPositionMatrix();
        int segU = 24;
        int segV = 36;

        float cx = 0F, cy = 0F, cz = 0F;
        if (axis == 'X') cx = baseLen;
        else if (axis == 'Y') cy = baseLen;
        else cz = baseLen;

        for (int iu = 0; iu < segU; iu++)
        {
            double u1 = Math.PI * (double) iu / (double) segU;
            double u2 = Math.PI * (double) (iu + 1) / (double) segU;

            for (int iv = 0; iv < segV; iv++)
            {
                double v1 = (Math.PI * 2.0) * (double) iv / (double) segV;
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

    private Axis detectHoveredAxis3D(UIContext input, Area viewport, Matrix4f origin, Matrix4f projection, Matrix4f view)
    {
        if (origin == null || projection == null || view == null)
        {
            return null;
        }

        this.hoveredPlane = null;

        float nx = (float) (((input.mouseX - viewport.x) / (double) viewport.w) * 2D - 1D);
        float ny = (float) -(((input.mouseY - viewport.y) / (double) viewport.h) * 2D - 1D);

        Matrix4f invPV = new Matrix4f(projection).mul(view).invert(new Matrix4f());
        Vector4f nearClip = new Vector4f(nx, ny, -1F, 1F);
        Vector4f farClip  = new Vector4f(nx, ny,  1F, 1F);

        Vector4f nearWorld = invPV.transform(new Vector4f(nearClip));
        Vector4f farWorld  = invPV.transform(new Vector4f(farClip));

        nearWorld.div(nearWorld.w);
        farWorld.div(farWorld.w);

        Matrix4f invOrigin = new Matrix4f(origin).invert(new Matrix4f());
        Vector4f nearLocal4 = invOrigin.transform(new Vector4f(nearWorld));
        Vector4f farLocal4  = invOrigin.transform(new Vector4f(farWorld));
        Vector3f rayO = new Vector3f(nearLocal4.x, nearLocal4.y, nearLocal4.z);
        Vector3f rayF = new Vector3f(farLocal4.x, farLocal4.y, farLocal4.z);
        Vector3f rayD = rayF.sub(rayO, new Vector3f());

        rayD.normalize();

        if (this.mode == Mode.ROTATE)
        {
            return detectHoveredAxis3DRotate(rayO, rayD);
        }

        if (this.mode == Mode.UNIVERSAL)
        {
            Axis rot = detectHoveredAxis3DRotate(rayO, rayD);

            if (rot != null)
            {
                this.hoveredSubMode = Mode.ROTATE;
                this.hoveredPlane = null;

                return rot;
            }

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

        float length = 0.25F * this.gizmoScale;
        float thickness = ((this.mode == Mode.SCALE) ? 0.045F : 0.015F) * this.gizmoScale;
        float fudge = ((this.mode == Mode.TRANSLATE) ? 0.06F : ((this.mode == Mode.SCALE) ? 0.045F : 0.02F)) * this.gizmoScale;

        float[] tx = rayBoxIntersect(rayO, rayD, new Vector3f(0F, -thickness/2F, -thickness/2F), new Vector3f(length + fudge, thickness/2F, thickness/2F));
        float[] ty = rayBoxIntersect(rayO, rayD, new Vector3f(-thickness/2F, 0F, -thickness/2F), new Vector3f(thickness/2F, length + fudge, thickness/2F));
        float[] tz = rayBoxIntersect(rayO, rayD, new Vector3f(-thickness/2F, -thickness/2F, 0F), new Vector3f(thickness/2F, thickness/2F, length + fudge));

        float bestT = Float.POSITIVE_INFINITY;
        Axis best = null;

        if (tx != null && tx[0] >= 0 && tx[0] < bestT) { bestT = tx[0]; best = Axis.X; }
        if (ty != null && ty[0] >= 0 && ty[0] < bestT) { bestT = ty[0]; best = Axis.Y; }
        if (tz != null && tz[0] >= 0 && tz[0] < bestT) { bestT = tz[0]; best = Axis.Z; }

        if (this.mode == Mode.TRANSLATE)
        {
            float po = 0.08F * this.gizmoScale; float ps = 0.020F * this.gizmoScale; float pt = 0.004F * this.gizmoScale;
            float[] pXY = rayBoxIntersect(rayO, rayD,
                    new Vector3f(po - ps, po - ps, -pt), new Vector3f(po + ps, po + ps, pt));
            float[] pZX = rayBoxIntersect(rayO, rayD,
                    new Vector3f(po - ps, -pt, po - ps), new Vector3f(po + ps, pt, po + ps));
            float[] pYZ = rayBoxIntersect(rayO, rayD,
                    new Vector3f(-pt, po - ps, po - ps), new Vector3f(pt, po + ps, po + ps));

            if (pXY != null && pXY[0] >= 0 && pXY[0] < bestT) { bestT = pXY[0]; best = Axis.X; this.hoveredPlane = Plane.XY; }
            if (pZX != null && pZX[0] >= 0 && pZX[0] < bestT) { bestT = pZX[0]; best = Axis.Z; this.hoveredPlane = Plane.ZX; }
            if (pYZ != null && pYZ[0] >= 0 && pYZ[0] < bestT) { bestT = pYZ[0]; best = Axis.Y; this.hoveredPlane = Plane.YZ; }
        }

        return best;
    }

    private Axis detectHoveredAxis3DRotate(Vector3f rayO, Vector3f rayD)
    {
        float radius = 0.22F * this.gizmoScale;
        float thickness = ((this.mode == Mode.ROTATE) ? 0.015F : 0.01F) * this.gizmoScale;
        float band = thickness * 0.5F + (0.002F * this.gizmoScale);

        class Hit { Axis a; float t; }

        Hit hitBest = null;
        BiFunction<Vector3f, Character, Hit> check = (n, c) ->
        {
            float denom = n.x * rayD.x + n.y * rayD.y + n.z * rayD.z;
            float t;
            float ix, iy, iz;

            if (Math.abs(denom) < 1e-5)
            {
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
                t = - (n.x * rayO.x + n.y * rayO.y + n.z * rayO.z) / denom;

                if (t < 0)
                {
                    return null;
                }

                ix = rayO.x + rayD.x * t;
                iy = rayO.y + rayD.y * t;
                iz = rayO.z + rayD.z * t;
            }

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

    private static float[] rayBoxIntersect(Vector3f ro, Vector3f rd, Vector3f min, Vector3f max)
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

        if (tmax < 0) return null;
        return new float[] { tmin, tmax };
    }

    public boolean isHoveringHandle()
    {
        return this.hoveredAxis != null;
    }

    private Axis detectHoveredAxis(int mx, int my)
    {
        if (this.mode == Mode.ROTATE)
        {
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

        int cx = this.centerX;
        int cy = this.centerY;
        int tol = Math.max(6, this.handleThickness + 4);

        if (this.mode == Mode.UNIVERSAL)
        {
            double dx = mx - cx; double dy = my - cy; double d = Math.sqrt(dx * dx + dy * dy);
            int tolRing = Math.max(6, this.handleThickness * 2);

            if (Math.abs(d - this.ringRX) <= tolRing)
            {
                this.hoveredSubMode = Mode.ROTATE;
                this.hoveredPlane = null;

                return Axis.X;
            }

            if (Math.abs(d - this.ringRY) <= tolRing)
            {
                this.hoveredSubMode = Mode.ROTATE;
                this.hoveredPlane = null;

                return Axis.Y;
            }

            if (Math.abs(d - this.ringRZ) <= tolRing)
            {
                this.hoveredSubMode = Mode.ROTATE;
                this.hoveredPlane = null;

                return Axis.Z;
            }

            if (isNear(mx, my, this.endXx, this.endXy, this.hitRadius))
            {
                this.hoveredSubMode = Mode.SCALE;
                this.hoveredPlane = null;

                return Axis.X;
            }

            if (isNear(mx, my, this.endYx, this.endYy, this.hitRadius))
            {
                this.hoveredSubMode = Mode.SCALE;
                this.hoveredPlane = null;

                return Axis.Y;
            }

            if (isNear(mx, my, this.endZx, this.endZy, this.hitRadius))
            {
                this.hoveredSubMode = Mode.SCALE;
                this.hoveredPlane = null;

                return Axis.Z;
            }

            this.hoveredSubMode = null; this.hoveredPlane = null; return null;
        }

        if (this.mode == Mode.TRANSLATE)
        {
            Vector2i cXY = planeCenterScreen(Plane.XY);
            Vector2i cZX = planeCenterScreen(Plane.ZX);
            Vector2i cYZ = planeCenterScreen(Plane.YZ);
            int pr = hitRadius;

            if (isNear(mx, my, cXY.x, cXY.y, pr))
            {
                this.hoveredPlane = Plane.XY;
                this.hoveredSubMode = Mode.TRANSLATE;

                return Axis.X;
            }

            if (isNear(mx, my, cZX.x, cZX.y, pr))
            {
                this.hoveredPlane = Plane.ZX;
                this.hoveredSubMode = Mode.TRANSLATE;

                return Axis.Z;
            }

            if (isNear(mx, my, cYZ.x, cYZ.y, pr))
            {
                this.hoveredPlane = Plane.YZ;
                this.hoveredSubMode = Mode.TRANSLATE;

                return Axis.Y;
            }

            this.hoveredPlane = null;
        }

        if (isNearLine(mx, my, cx, cy, this.endXx, this.endXy, tol) || isNear(mx, my, this.endXx, this.endXy, this.hitRadius)) return Axis.X;
        if (isNearLine(mx, my, cx, cy, this.endYx, this.endYy, tol) || isNear(mx, my, this.endYx, this.endYy, this.hitRadius)) return Axis.Y;
        if (isNearLine(mx, my, cx, cy, this.endZx, this.endZy, tol) || isNear(mx, my, this.endZx, this.endZy, this.hitRadius)) return Axis.Z;

        return null;
    }

    private Vector2i planeCenterScreen(Plane p)
    {
        float k = 0.55F;
        int cx = this.centerX, cy = this.centerY;

        return switch (p)
        {
            case XY -> new Vector2i((int) (cx + (this.endXx - cx) * k), (int) (cy + (this.endYy - cy) * k));
            case ZX -> new Vector2i((int) (cx + (this.endXx - cx) * k), (int) (cy + (this.endZy - cy) * k));
            case YZ -> new Vector2i((int) (cx + (this.endYx - cx) * k), (int) (cy + (this.endZy - cy) * k));
        };
    }

    public void cycleMode(boolean forward)
    {
        Mode[] order = new Mode[]{ Mode.TRANSLATE, Mode.ROTATE, Mode.SCALE };

        this.mode = order[MathUtils.cycler(this.mode.ordinal() + (forward ? 1 : -1), 0, order.length - 1)];
    }

    public enum Mode
    {
        TRANSLATE, ROTATE, SCALE, UNIVERSAL;
    }

    private enum Plane
    {
        XY, YZ, ZX;
    }
}