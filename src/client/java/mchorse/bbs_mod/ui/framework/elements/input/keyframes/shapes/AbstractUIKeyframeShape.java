package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.ui.framework.UIContext;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

public abstract class AbstractUIKeyframeShape implements IUIKeyframeShape
{

    protected UIContext context;
    protected BufferBuilder builder;
    protected Matrix4f matrix;
    protected int x;
    protected int y;
    protected int offset;
    protected int c;

    public AbstractUIKeyframeShape(UIContext uiContext, BufferBuilder builder, Matrix4f matrix4f, int x, int y, int offset, int c) {
        this.context = uiContext;
        this.builder = builder;
        this.matrix = matrix4f;
        this.x = x;
        this.y = y;
        this.offset = offset;
        this.c = c;
    }

    public UIContext getContext() {
        return context;
    }

    public void setContext(UIContext context) {
        this.context = context;
    }

    public BufferBuilder getBuilder() {
        return builder;
    }

    public void setBuilder(BufferBuilder builder) {
        this.builder = builder;
    }

    public Matrix4f getMatrix() {
        return matrix;
    }

    public void setMatrix(Matrix4f matrix) {
        this.matrix = matrix;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }
}
