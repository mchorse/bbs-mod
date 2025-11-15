package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

public class UIFourStarKeyframeShape extends UIStarsKeyframeShape
{
    public UIFourStarKeyframeShape(UIContext uiContext, BufferBuilder builder, Matrix4f matrix4f, int x, int y, int offset, int c)
    {
        super(uiContext, builder, matrix4f, x, y, offset, c, 4, Icons.FOUR_STAR);
    }
}
