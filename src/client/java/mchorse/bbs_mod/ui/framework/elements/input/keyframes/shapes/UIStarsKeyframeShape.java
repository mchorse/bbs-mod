package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

public class UIStarsKeyframeShape extends AbstractUIKeyframeShape implements IUIKeyframeShapeIndependantBackground
{
    private final int numBranches;
    private final Icon icon;

    public UIStarsKeyframeShape(UIContext uiContext, BufferBuilder builder, Matrix4f matrix4f, int x, int y, int offset, int c, int numBranches, Icon icon)
    {
        super(uiContext, builder, matrix4f, x, y, offset * 3, c);
        this.numBranches = numBranches;
        this.icon = icon;
    }

    @Override
    public void renderKeyframe()
    {
        float centerX = x;
        float centerY = y;
        float radius = offset;

        float innerRadius = 0f;
        float outerRadius = radius;
        float baseWidth = radius * 0.25f;
        float tipWidth = radius * 0.08f;

        for (int i = 0; i < numBranches; i++)
        {
            float angle = -90f + (360f / numBranches) * i;
            drawBranch(angle, innerRadius, outerRadius, baseWidth, tipWidth, centerX, centerY);
        }
    }

    private void drawBranch(float angleDegrees, float innerRadius, float outerRadius,
                            float baseWidth, float tipWidth, float centerX, float centerY)
    {
        float angle = (float) Math.toRadians(angleDegrees);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        float baseLeft_x = centerX + innerRadius * cos - baseWidth * sin;
        float baseLeft_y = centerY + innerRadius * sin + baseWidth * cos;
        float baseRight_x = centerX + innerRadius * cos + baseWidth * sin;
        float baseRight_y = centerY + innerRadius * sin - baseWidth * cos;

        float tipLeft_x = centerX + outerRadius * cos - tipWidth * sin;
        float tipLeft_y = centerY + outerRadius * sin + tipWidth * cos;
        float tipRight_x = centerX + outerRadius * cos + tipWidth * sin;
        float tipRight_y = centerY + outerRadius * sin - tipWidth * cos;

        builder.vertex(matrix, baseLeft_x, baseLeft_y, 0).color(c).next();
        builder.vertex(matrix, tipLeft_x, tipLeft_y, 0).color(c).next();
        builder.vertex(matrix, tipRight_x, tipRight_y, 0).color(c).next();
        builder.vertex(matrix, baseRight_x, baseRight_y, 0).color(c).next();
    }

    @Override
    public Icon getIcon()
    {
        return icon;
    }

    @Override
    public void renderKeyframeBackground()
    {
        float centerSize = offset * 0.2f;
        float half = centerSize * 1.25f;

        builder.vertex(matrix, x - half, y - half, 0).color(c).next();
        builder.vertex(matrix, x - half, y + half, 0).color(c).next();
        builder.vertex(matrix, x + half, y + half, 0).color(c).next();
        builder.vertex(matrix, x + half, y - half, 0).color(c).next();
    }
}
