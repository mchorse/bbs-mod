package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

public interface IKeyframeShapeRenderer
{
    public IKey getLabel();

    public Icon getIcon();

    public void renderKeyframe(UIContext uiContext, BufferBuilder builder, Matrix4f matrix4f, int x, int y, int offset, int c);

    public default void renderKeyframeBackground(UIContext uiContext, BufferBuilder builder, Matrix4f matrix4f, int x, int y, int offset, int c)
    {}
}