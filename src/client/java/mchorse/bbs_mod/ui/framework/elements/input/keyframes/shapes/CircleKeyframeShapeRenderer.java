package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

public class CircleKeyframeShapeRenderer implements IKeyframeShapeRenderer
{
    @Override
    public IKey getLabel()
    {
        return UIKeys.KEYFRAMES_SHAPES_CIRCLE;
    }

    @Override
    public Icon getIcon()
    {
        return Icons.CIRCLE;
    }

    @Override
    public void renderKeyframe(UIContext uiContext, BufferBuilder builder, Matrix4f matrix, int x, int y, int offset, int c)
    {
        final int NUM_SEGMENTS = 32;

        for (int i = 0; i < NUM_SEGMENTS; i++)
        {
            float angle1 = (float) Math.toRadians((360F / NUM_SEGMENTS) * i);
            float angle2 = (float) Math.toRadians((360F / NUM_SEGMENTS) * (i + 1));

            float cos1 = (float) Math.cos(angle1);
            float sin1 = (float) Math.sin(angle1);
            float cos2 = (float) Math.cos(angle2);
            float sin2 = (float) Math.sin(angle2);

            float innerRadius = (float) offset * 1.25F;

            float innerX1 = x + innerRadius * cos1;
            float innerY1 = y + innerRadius * sin1;
            float innerX2 = x + innerRadius * cos2;
            float innerY2 = y + innerRadius * sin2;

            float outerX1 = x;
            float outerY1 = y;
            float outerX2 = x;
            float outerY2 = y;

            builder.vertex(matrix, innerX1, innerY1, 0F).color(c).next();
            builder.vertex(matrix, outerX1, outerY1, 0F).color(c).next();
            builder.vertex(matrix, outerX2, outerY2, 0F).color(c).next();
            builder.vertex(matrix, innerX2, innerY2, 0F).color(c).next();
        }
    }

    @Override
    public void renderKeyframeBackground(UIContext uiContext, BufferBuilder builder, Matrix4f matrix, int x, int y, int offset, int c)
    {
        float centerSize = offset * 0.2f;
        float half = centerSize * 2;

        builder.vertex(matrix, x - half, y - half, 0F).color(c).next();
        builder.vertex(matrix, x - half, y + half, 0F).color(c).next();
        builder.vertex(matrix, x + half, y + half, 0F).color(c).next();
        builder.vertex(matrix, x + half, y - half, 0F).color(c).next();
    }
}