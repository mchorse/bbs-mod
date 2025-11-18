package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

public class TriangleKeyframeShapeRenderer implements IKeyframeShapeRenderer
{
    @Override
    public IKey getLabel()
    {
        return UIKeys.KEYFRAMES_SHAPES_TRIANGLE;
    }

    @Override
    public Icon getIcon() {
        return Icons.TRIANGLE;
    }

    @Override
    public void renderKeyframe(UIContext uiContext, BufferBuilder builder, Matrix4f matrix, int x, int y, int offset, int c)
    {
        float fOffset = offset * 1.75F;

        builder.vertex(matrix, x, y - fOffset, 0).color(c).next();
        builder.vertex(matrix, x - fOffset, y + fOffset, 0).color(c).next();
        builder.vertex(matrix, x + fOffset, y + fOffset, 0).color(c).next();
        builder.vertex(matrix, x + fOffset, y + fOffset, 0).color(c).next();
    }
}