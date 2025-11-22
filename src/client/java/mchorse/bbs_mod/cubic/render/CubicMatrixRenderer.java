package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CubicMatrixRenderer implements ICubicRenderer
{
    public List<Matrix4f> matrices;
    public List<Matrix4f> origins;
    public String target;

    public CubicMatrixRenderer(Model model, String target)
    {
        this.matrices = new ArrayList<>();
        this.origins = new ArrayList<>();
        this.target = target;

        for (int i = 0; i < model.getAllGroupKeys().size(); i++)
        {
            this.matrices.add(new Matrix4f());
            this.origins.add(new Matrix4f());
        }
    }

    @Override
    public void applyGroupTransformations(MatrixStack stack, ModelGroup group)
    {
        ICubicRenderer.translateGroup(stack, group);
        ICubicRenderer.moveToGroupPivot(stack, group);

        /* Capture matrix at the group's pivot before rotation/scale */
        this.origins.get(group.index).set(stack.peek().getPositionMatrix());

        if (!Objects.equals(group.id, this.target))
        {
            ICubicRenderer.rotateGroup(stack, group);
        }

        ICubicRenderer.scaleGroup(stack, group);
        ICubicRenderer.moveBackFromGroupPivot(stack, group);
    }

    @Override
    public boolean renderGroup(BufferBuilder builder, MatrixStack stack, ModelGroup group, Model model)
    {
        this.matrices.get(group.index).set(stack.peek().getPositionMatrix());

        return false;
    }
}