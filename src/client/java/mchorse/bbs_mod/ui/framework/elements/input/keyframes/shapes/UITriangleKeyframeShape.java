package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

public class UITriangleKeyframeShape extends AbstractUIKeyframeShape
{
    public UITriangleKeyframeShape(UIContext uiContext, BufferBuilder builder, Matrix4f matrix4f, int x, int y, int offset, int c)
    {
        super(uiContext, builder, matrix4f, x, y, offset * 2, c);
    }

    @Override
    public void renderKeyframe()
    {
        builder.vertex(matrix, x, y - offset, 0).color(c).next();
        builder.vertex(matrix, x - offset, y + offset, 0).color(c).next();
        builder.vertex(matrix, x + offset, y + offset, 0).color(c).next();
        builder.vertex(matrix, x + offset, y + offset, 0).color(c).next();
    }

    @Override
    public Icon getIcon() {
        return Icons.TRIANGLE;
    }
}
