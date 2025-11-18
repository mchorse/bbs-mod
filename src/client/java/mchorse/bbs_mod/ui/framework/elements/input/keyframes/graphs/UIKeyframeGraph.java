package mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.line.LineBuilder;
import mchorse.bbs_mod.graphics.line.SolidColorLineRenderer;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Scale;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

public class UIKeyframeGraph implements IUIKeyframeGraph
{
    private UIKeyframes keyframes;

    private UIKeyframeSheet sheet;

    private final Scale yAxis;

    public UIKeyframeGraph(UIKeyframes keyframes, UIKeyframeSheet sheet)
    {
        this.keyframes = keyframes;
        this.sheet = sheet;

        this.yAxis = new Scale(this.keyframes.area, ScrollDirection.VERTICAL).inverse();
    }

    /* Graphing */

    public int toGraphY(double value)
    {
        return (int) this.yAxis.to(value);
    }

    public double fromGraphY(int mouseY)
    {
        return this.yAxis.from(mouseY);
    }

    /**
     * Whether given mouse coordinates are near the given point?
     */
    private boolean isNear(double x, double y, int mouseX, int mouseY)
    {
        return Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2) < 25D;
    }

    public void resetViewY(UIKeyframeSheet current)
    {
        this.yAxis.set(0, 2);

        KeyframeChannel channel = current.channel;
        List<Keyframe> keyframes = channel.getKeyframes();
        int c = keyframes.size();

        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        if (c > 1)
        {
            for (int i = 0; i < c; i++)
            {
                Keyframe frame = keyframes.get(i);

                minY = Math.min(minY, frame.getY(i));
                maxY = Math.max(maxY, frame.getY(i));
            }
        }
        else
        {
            minY = -10;
            maxY = 10;

            if (c == 1)
            {
                minY = maxY = channel.get(0).getY(0);
            }
        }

        if (Math.abs(maxY - minY) < 0.01F)
        {
            /* Centerize */
            this.yAxis.setShift(minY);
            this.yAxis.anchor(0.5F);
        }
        else
        {
            /* Spread apart vertically */
            this.yAxis.viewOffset(minY, maxY, this.keyframes.area.h, 30);
        }
    }

    @Override
    public void resetView()
    {
        this.keyframes.resetViewX();
        this.resetViewY(this.sheet);
    }

    @Override
    public List<UIKeyframeSheet> getSheets()
    {
        return Collections.singletonList(this.sheet);
    }

    @Override
    public void selectByX(int mouseX)
    {
        List keyframes = this.sheet.channel.getKeyframes();

        for (int i = 0; i < keyframes.size(); i++)
        {
            Keyframe keyframe = (Keyframe) keyframes.get(i);
            int x = this.keyframes.toGraphX(keyframe.getTick());
            int y = this.toGraphY(keyframe.getFactory().getY(keyframe.getValue()));

            if (this.isNear(x, y, mouseX, 0))
            {
                this.sheet.selection.add(i);
            }
        }

        this.pickSelected();
    }

    @Override
    public void selectInArea(Area area)
    {
        List keyframes = this.sheet.channel.getKeyframes();

        for (int i = 0; i < keyframes.size(); i++)
        {
            Keyframe keyframe = (Keyframe) keyframes.get(i);
            int x = this.keyframes.toGraphX(keyframe.getTick());
            int y = this.toGraphY(keyframe.getFactory().getY(keyframe.getValue()));

            if (area.isInside(x, y))
            {
                this.sheet.selection.add(i);
            }
        }

        this.pickSelected();
    }

    @Override
    public UIKeyframeSheet getSheet(int mouseY)
    {
        return this.sheet;
    }

    @Override
    public boolean addKeyframe(int mouseX, int mouseY)
    {
        float tick = (float) this.keyframes.fromGraphX(mouseX);
        UIKeyframeSheet sheet = this.sheet;

        if (!Window.isShiftPressed())
        {
            tick = Math.round(tick);
        }

        if (sheet != null)
        {
            this.addKeyframe(sheet, tick, sheet.channel.getFactory().yToValue(this.fromGraphY(mouseY)));
        }

        return sheet != null;
    }

    @Override
    public Pair<Keyframe, KeyframeType> findKeyframe(int mouseX, int mouseY)
    {
        List keyframes = this.sheet.channel.getKeyframes();

        for (int i = 0; i < keyframes.size(); i++)
        {
            Keyframe keyframe = (Keyframe) keyframes.get(i);
            int x = this.keyframes.toGraphX(keyframe.getTick());
            int y = this.toGraphY(keyframe.getFactory().getY(keyframe.getValue()));

            if (this.isNear(x, y, mouseX, mouseY))
            {
                return new Pair<>(keyframe, KeyframeType.REGULAR);
            }

            int lx = this.keyframes.toGraphX(keyframe.getTick() - keyframe.lx);
            int ly = this.toGraphY(keyframe.getFactory().getY(keyframe.getValue()) + keyframe.ly);

            if (this.isNear(lx, ly, mouseX, mouseY))
            {
                return new Pair<>(keyframe, KeyframeType.LEFT_HANDLE);
            }

            int rx = this.keyframes.toGraphX(keyframe.getTick() + keyframe.rx);
            int ry = this.toGraphY(keyframe.getFactory().getY(keyframe.getValue()) + keyframe.ry);

            if (this.isNear(rx, ry, mouseX, mouseY))
            {
                return new Pair<>(keyframe, KeyframeType.RIGHT_HANDLE);
            }
        }

        return null;
    }

    @Override
    public void pickKeyframe(Keyframe keyframe)
    {
        this.keyframes.pickKeyframe(keyframe);
    }

    @Override
    public void selectKeyframe(Keyframe keyframe)
    {
        this.clearSelection();

        UIKeyframeSheet sheet = this.getSheet(keyframe);

        if (sheet != null)
        {
            sheet.selection.add(keyframe);
            this.pickKeyframe(keyframe);

            double x = keyframe.getTick();
            int y = this.toGraphY(keyframe.getFactory().getY(keyframe.getValue()));

            this.keyframes.getXAxis().shiftIntoMiddle(x);
            this.yAxis.shiftIntoMiddle(y);
        }
    }

    @Override
    public void resize()
    {}

    @Override
    public boolean mouseClicked(UIContext context)
    {
        return false;
    }

    @Override
    public void mouseReleased(UIContext context)
    {}

    @Override
    public void mouseScrolled(UIContext context)
    {
        if (context.mouseWheelHorizontal != 0)
        {
            double offsetX = (25F * BBSSettings.scrollingSensitivityHorizontal.get() * context.mouseWheelHorizontal) / this.keyframes.getXAxis().getZoom();

            this.keyframes.getXAxis().setShift(this.keyframes.getXAxis().getShift() - offsetX);
        }
        else
        {
            boolean x = Window.isShiftPressed();
            boolean y = Window.isCtrlPressed();
            boolean none = !x && !y;

            /* Scaling X */
            if (x && !y || none)
            {
                if (context.mouseWheel != 0D)
                {
                    this.keyframes.getXAxis().zoomAnchor(Scale.getAnchorX(context, this.keyframes.area), Math.copySign(this.keyframes.getXAxis().getZoomFactor(), context.mouseWheel));
                }
            }

            /* Scaling Y */
            if (y && !x || none)
            {
                if (context.mouseWheel != 0D)
                {
                    this.yAxis.zoomAnchor(Scale.getAnchorY(context, this.keyframes.area), Math.copySign(this.yAxis.getZoomFactor(), context.mouseWheel));
                }
            }
        }
    }

    @Override
    public void handleMouse(UIContext context, int lastX, int lastY)
    {
        if (this.keyframes.isNavigating())
        {
            int mouseX = context.mouseX;
            int mouseY = context.mouseY;
            double offsetX = (mouseX - lastX) / this.keyframes.getXAxis().getZoom();
            double offsetY = -(mouseY - lastY) / this.yAxis.getZoom();

            this.keyframes.getXAxis().setShift(this.keyframes.getXAxis().getShift() - offsetX);
            this.yAxis.setShift(this.yAxis.getShift() - offsetY);
        }
    }

    @Override
    public void dragKeyframes(UIContext context, Pair<Keyframe, KeyframeType> type, int originalX, int originalY, float originalT, Object originalV)
    {
        if (type == null)
        {
            return;
        }

        IKeyframeFactory factory = this.sheet.channel.getFactory();
        Keyframe keyframe = type.a;

        if (type.b == KeyframeType.REGULAR)
        {
            float offsetX = (float) this.keyframes.fromGraphX(originalX) - originalT;
            double offsetY = this.fromGraphY(originalY) - factory.getY(originalV);

            float fx = (float) this.keyframes.fromGraphX(context.mouseX) - offsetX;
            Object fy = factory.yToValue(this.fromGraphY(context.mouseY) - offsetY);

            if (!Window.isShiftPressed())
            {
                fx = Math.round(this.keyframes.fromGraphX(context.mouseX) - offsetX);
            }

            this.setTick(fx, false);
            this.setValue(fy, false);
        }
        else if (type.b == KeyframeType.LEFT_HANDLE)
        {
            keyframe.lx = -(float) ((this.keyframes.fromGraphX(context.mouseX)) - keyframe.getTick());
            keyframe.ly = (float) (this.fromGraphY(context.mouseY) - factory.getY(originalV));

            if (!Window.isShiftPressed())
            {
                keyframe.rx = keyframe.lx;
                keyframe.ry = -keyframe.ly;
            }
        }
        else if (type.b == KeyframeType.RIGHT_HANDLE)
        {
            keyframe.rx = (float) ((this.keyframes.fromGraphX(context.mouseX)) - keyframe.getTick());
            keyframe.ry = (float) (this.fromGraphY(context.mouseY) - factory.getY(originalV));

            if (!Window.isShiftPressed())
            {
                keyframe.lx = keyframe.rx;
                keyframe.ly = -keyframe.ry;
            }
        }

        this.keyframes.triggerChange();
    }

    @Override
    public void render(UIContext context)
    {
        this.renderGrid(context);
        this.renderGraph(context);
    }

    /**
     * Render grid that allows easier to see where are specific ticks
     */
    protected void renderGrid(UIContext context)
    {
        /* Draw horizontal grid */
        Area area = this.keyframes.area;
        int mult = this.keyframes.getXAxis().getMult();
        int hx = this.keyframes.getDuration() / mult;
        int ht = (int) this.keyframes.fromGraphX(area.x);

        for (int j = Math.max(ht / mult, 0); j <= hx; j++)
        {
            int x = this.keyframes.toGraphX(j * mult);

            if (x >= area.ex())
            {
                break;
            }

            String label = this.keyframes.getConverter() == null ? String.valueOf(j * mult) : this.keyframes.getConverter().format(j * mult);

            context.batcher.box(x, area.y, x + 1, area.ey(), Colors.setA(Colors.WHITE, 0.25F));
            context.batcher.text(label, x + 4, area.y + 4);
        }

        /* Draw vertical grid */
        int ty = (int) this.fromGraphY(area.ey());
        int by = (int) this.fromGraphY(area.y - 12);

        int min = Math.min(ty, by) - 1;
        int max = Math.max(ty, by) + 1;
        mult = this.yAxis.getMult();

        min -= min % mult + mult;
        max -= max % mult - mult;

        for (int j = 0, c = (max - min) / mult; j < c; j++)
        {
            int y = this.toGraphY(min + j * mult);

            if (y > area.ey())
            {
                continue;
            }

            context.batcher.box(area.x, y, area.ex(), y + 1, Colors.setA(Colors.WHITE, 0.25F));
            context.batcher.text(String.valueOf(min + j * mult), area.x + 4, y + 4);
        }

        /* Render where the keyframe will be duplicated or added */
        if (!area.isInside(context))
        {
            return;
        }

        float currentTick = (float) this.keyframes.fromGraphX(context.mouseX);

        if (this.keyframes.isStacking())
        {
            UIKeyframeSheet current = this.sheet;
            List<Keyframe> selected = current.selection.getSelected();
            IKeyframeFactory factory = current.channel.getFactory();
            float mMin = Integer.MAX_VALUE;
            float mMax = Integer.MIN_VALUE;

            for (Keyframe keyframe : selected)
            {
                mMin = Math.min(keyframe.getTick(), mMin);
                mMax = Math.max(keyframe.getTick(), mMax);
            }

            float length = mMax - mMin + this.keyframes.getStackOffset();
            int times = (int) Math.max(1, Math.ceil((currentTick - mMax) / length));
            float x = 0;

            for (int i = 0; i < times; i++)
            {
                for (Keyframe keyframe : selected)
                {
                    int y = (int) this.yAxis.to(factory.getY(keyframe.getValue()));
                    float tick = mMax + this.keyframes.getStackOffset() + (keyframe.getTick() - mMin) + x;

                    this.renderPreviewKeyframe(context, current, tick, y, Colors.YELLOW);
                }

                x += length;
            }
        }
        else if (Window.isCtrlPressed())
        {
            UIKeyframeSheet sheet = this.getSheet(context.mouseY);

            if (sheet != null)
            {
                float tick = currentTick;

                if (!Window.isShiftPressed())
                {
                    tick = Math.round(tick);
                }

                this.renderPreviewKeyframe(context, sheet, tick, context.mouseY, Colors.WHITE);
            }
        }
        else if (Window.isAltPressed())
        {
            UIKeyframeSheet current = this.sheet;
            List<Keyframe> selected = current.selection.getSelected();
            IKeyframeFactory factory = current.channel.getFactory();

            for (int i = 0; i < selected.size(); i++)
            {
                Keyframe first = selected.get(0);
                Keyframe keyframe = selected.get(i);
                int y = (int) this.yAxis.to(factory.getY(keyframe.getValue()));

                this.renderPreviewKeyframe(context, current, currentTick + (keyframe.getTick() - first.getTick()), y, Colors.YELLOW);
            }
        }
    }

    private void renderPreviewKeyframe(UIContext context, UIKeyframeSheet sheet, double tick, int y, int color)
    {
        int x = this.keyframes.toGraphX(tick);
        float a = (float) Math.sin(context.getTickTransition() / 2D) * 0.1F + 0.5F;

        context.batcher.box(x - 3, y - 3, x + 3, y + 3, Colors.setA(color, a));
    }

    /**
     * Render the graph
     */
    @SuppressWarnings({"rawtypes", "IntegerDivisionInFloatingPointContext"})
    protected void renderGraph(UIContext context)
    {
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        Matrix4f matrix = context.batcher.getContext().getMatrices().peek().getPositionMatrix();

        UIKeyframeSheet sheet = this.sheet;
        List keyframes = sheet.channel.getKeyframes();
        KeyframeSegment segment = new KeyframeSegment();

        /* Render graph */
        LineBuilder lineBuilder = new LineBuilder(0.7F);

        for (int i = 0; i < keyframes.size(); i++)
        {
            Keyframe frame = (Keyframe) keyframes.get(i);
            Keyframe prev = i > 0 ? (Keyframe) keyframes.get(i - 1) : null;
            int x = this.keyframes.toGraphX(frame.getTick());
            int y = this.toGraphY(sheet.channel.getFactory().getY(frame.getValue()));

            if (i == 0 && x > this.keyframes.area.x)
            {
                lineBuilder.add(this.keyframes.area.x, y);
            }

            if (prev != null)
            {
                IInterp interp = prev.getInterpolation().getInterp();
                int px = this.keyframes.toGraphX(prev.getTick());
                int py = this.toGraphY(sheet.channel.getFactory().getY(prev.getValue()));

                if (interp == Interpolations.CONST)
                {
                    lineBuilder.add(x, py);
                    lineBuilder.push();
                }
                else if (interp != Interpolations.LINEAR)
                {
                    float steps = 50F;

                    for (int j = 1; j <= steps; j++)
                    {
                        float a = j / steps;

                        segment.setup(prev, frame, prev.getTick() + a * (frame.getTick() - prev.getTick()));

                        float interpolate = this.toGraphY((float) frame.getFactory().getY(segment.createInterpolated()));

                        lineBuilder.add(Lerps.lerp(px, x, a), interpolate);
                    }
                }
            }

            lineBuilder.add(x, y);

            if (i == keyframes.size() - 1 && x < this.keyframes.area.ex())
            {
                lineBuilder.add(this.keyframes.area.ex(), y);
            }

            boolean add = false;

            if (frame.getInterpolation().getInterp() == Interpolations.BEZIER)
            {
                int rx = this.keyframes.toGraphX(frame.getTick() + frame.rx);
                int ry = this.toGraphY(sheet.channel.getFactory().getY(frame.getValue()) + frame.ry);

                lineBuilder.push();
                lineBuilder.add(x, y);
                lineBuilder.add(rx, ry);

                add = true;
            }

            if (prev != null && prev.getInterpolation().getInterp() == Interpolations.BEZIER)
            {
                int lx = this.keyframes.toGraphX(frame.getTick() - frame.lx);
                int ly = this.toGraphY(sheet.channel.getFactory().getY(frame.getValue()) + frame.ly);

                lineBuilder.push();
                lineBuilder.add(x, y);
                lineBuilder.add(lx, ly);

                add = true;
            }

            if (add)
            {
                lineBuilder.push();
                lineBuilder.add(x, y);
            }
        }

        lineBuilder.render(context.batcher, SolidColorLineRenderer.get(Colors.COLOR.set(Colors.setA(sheet.color, 1F))));

        /* Render track bars (horizontal lines) */
        builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        /* Draw keyframe handles (outer) */
        int forcedIndex = 0;

        for (int i = 0; i < keyframes.size(); i++)
        {
            Keyframe frame = (Keyframe) keyframes.get(i);
            Keyframe prev = i > 0 ? (Keyframe) keyframes.get(i - 1) : null;
            float tick = frame.getTick();
            int x1 = this.keyframes.toGraphX(tick);
            int x2 = this.keyframes.toGraphX(tick + frame.getDuration());
            int y = this.toGraphY(sheet.channel.getFactory().getY(frame.getValue()));

            /* Render custom duration markers */
            if (x1 != x2)
            {
                int y1 = y - 8 + (forcedIndex % 2 == 1 ? -4 : 0);
                int color = sheet.selection.has(i) ? Colors.WHITE :  Colors.setA(Colors.mulRGB(sheet.color, 0.9F), 0.75F);

                context.batcher.fillRect(builder, matrix, x1, y1 - 2, 1, 5, color, color, color, color);
                context.batcher.fillRect(builder, matrix, x2, y1 - 2, 1, 5, color, color, color, color);
                context.batcher.fillRect(builder, matrix, x1 + 1, y1, x2 - x1, 1, color, color, color, color);

                forcedIndex += 1;
            }

            boolean isPointHover = this.isNear(this.keyframes.toGraphX(frame.getTick()), y, context.mouseX, context.mouseY);
            boolean toRemove = Window.isCtrlPressed() && isPointHover;

            if (this.keyframes.isSelecting())
            {
                isPointHover = isPointHover || this.keyframes.getGrabbingArea(context).isInside(x1, y);
            }

            int c = (sheet.selection.has(i) || isPointHover ? Colors.WHITE : sheet.color) | Colors.A100;

            if (toRemove)
            {
                c = Colors.RED | Colors.A100;
            }

            this.renderSquare(context, builder, matrix, x1, y, toRemove ? 4 : 3, c);

            if (frame.getInterpolation().getInterp() == Interpolations.BEZIER)
            {
                int rx = this.keyframes.toGraphX(frame.getTick() + frame.rx);
                int ry = this.toGraphY(sheet.channel.getFactory().getY(frame.getValue()) + frame.ry);

                this.renderSquare(context, builder, matrix, rx, ry, 3, c);
            }

            if (prev != null && prev.getInterpolation().getInterp() == Interpolations.BEZIER)
            {
                int lx = this.keyframes.toGraphX(frame.getTick() - frame.lx);
                int ly = this.toGraphY(sheet.channel.getFactory().getY(frame.getValue()) + frame.ly);

                this.renderSquare(context, builder, matrix, lx, ly, 3, c);
            }
        }

        /* Render keyframe handles (inner) */
        for (int j = 0; j < keyframes.size(); j++)
        {
            Keyframe frame = (Keyframe) keyframes.get(j);
            Keyframe prev = j > 0 ? (Keyframe) keyframes.get(j - 1) : null;
            int y = this.toGraphY(sheet.channel.getFactory().getY(frame.getValue()));

            Color keyframeColor = frame.getColor();
            int kc = keyframeColor != null ? keyframeColor.getRGBColor() : 0;
            int c = sheet.selection.has(j) ? Colors.ACTIVE : 0;

            this.renderSquare(context, builder, matrix, this.keyframes.toGraphX(frame.getTick()), y, 2, kc | c | Colors.A100);

            if (frame.getInterpolation().getInterp() == Interpolations.BEZIER)
            {
                int rx = this.keyframes.toGraphX(frame.getTick() + frame.rx);
                int ry = this.toGraphY(sheet.channel.getFactory().getY(frame.getValue()) + frame.ry);

                this.renderSquare(context, builder, matrix, rx, ry, 2, c | Colors.A100);
            }

            if (prev != null && prev.getInterpolation().getInterp() == Interpolations.BEZIER)
            {
                int lx = this.keyframes.toGraphX(frame.getTick() - frame.lx);
                int ly = this.toGraphY(sheet.channel.getFactory().getY(frame.getValue()) + frame.ly);

                this.renderSquare(context, builder, matrix, lx, ly, 2, c | Colors.A100);
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    protected void renderSquare(UIContext context, BufferBuilder builder, Matrix4f matrix, int x, int y, int offset, int c)
    {
        context.batcher.fillRect(builder, matrix, x - offset, y - offset, offset * 2, offset * 2, c, c, c, c);
    }

    @Override
    public void postRender(UIContext context)
    {}

    @Override
    public void saveState(MapType extra)
    {
        extra.putDouble("y_min", this.yAxis.getMinValue());
        extra.putDouble("y_max", this.yAxis.getMaxValue());
    }

    @Override
    public void restoreState(MapType extra)
    {
        this.yAxis.view(extra.getDouble("y_min"), extra.getDouble("y_max"));
    }
}