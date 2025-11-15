package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

public class UICircleKeyframeShape extends AbstractUIKeyframeShape implements IUIKeyframeShapeIndependantBackground
{
    private static final int NUM_SEGMENTS = 32;

    public UICircleKeyframeShape(UIContext uiContext, BufferBuilder builder, Matrix4f matrix4f, int x, int y, int offset, int c)
    {
        super(uiContext, builder, matrix4f, x, y, offset * 3, c);
    }

    @Override
    public void renderKeyframe()
    {
        float centerX = x;
        float centerY = y;
        float radius = offset;

        for (int i = 0; i < NUM_SEGMENTS; i++)
        {
            float angle1 = (360f / NUM_SEGMENTS) * i;
            float angle2 = (360f / NUM_SEGMENTS) * (i + 1);
            drawSegment(angle1, angle2, radius, centerX, centerY);
        }
    }

    private void drawSegment(float angle1Degrees, float angle2Degrees, float radius, float centerX, float centerY)
    {
        float angle1 = (float) Math.toRadians(angle1Degrees);
        float angle2 = (float) Math.toRadians(angle2Degrees);

        float cos1 = (float) Math.cos(angle1);
        float sin1 = (float) Math.sin(angle1);
        float cos2 = (float) Math.cos(angle2);
        float sin2 = (float) Math.sin(angle2);

        float innerRadius = radius * 0.3f;

        float innerX1 = centerX + innerRadius * cos1;
        float innerY1 = centerY + innerRadius * sin1;
        float innerX2 = centerX + innerRadius * cos2;
        float innerY2 = centerY + innerRadius * sin2;

        float outerX1 = centerX + radius * cos1;
        float outerY1 = centerY + radius * sin1;
        float outerX2 = centerX + radius * cos2;
        float outerY2 = centerY + radius * sin2;

        builder.vertex(matrix, innerX1, innerY1, 0).color(c).next();
        builder.vertex(matrix, outerX1, outerY1, 0).color(c).next();
        builder.vertex(matrix, outerX2, outerY2, 0).color(c).next();
        builder.vertex(matrix, innerX2, innerY2, 0).color(c).next();
    }

    @Override
    public Icon getIcon()
    {
        return Icons.CIRCLE;
    }

    @Override
    public void renderKeyframeBackground()
    {
        float centerSize = offset * 0.2f;
        float half = centerSize * 2;

        builder.vertex(matrix, x - half, y - half, 0).color(c).next();
        builder.vertex(matrix, x - half, y + half, 0).color(c).next();
        builder.vertex(matrix, x + half, y + half, 0).color(c).next();
        builder.vertex(matrix, x + half, y - half, 0).color(c).next();
    }
}