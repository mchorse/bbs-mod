package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;

public class CubicRenderer
{
    /**
     * Process/render given model
     *
     * This method recursively goes through all groups in the model, and
     * applies given render processor. Processor may return true from its
     * sole method which means that iteration should be halted.
     */
    public static boolean processRenderModel(ICubicRenderer renderProcessor, BufferBuilder builder, MatrixStack stack, Model model)
    {
        for (ModelGroup group : model.topGroups)
        {
            if (processRenderRecursively(renderProcessor, builder, stack, model, group))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Apply the render processor, recursively
     */
    private static boolean processRenderRecursively(ICubicRenderer renderProcessor, BufferBuilder builder, MatrixStack stack, Model model, ModelGroup group)
    {
        stack.push();
        renderProcessor.applyGroupTransformations(stack, group);

        if (group.visible)
        {
            if (renderProcessor.renderGroup(builder, stack, group, model))
            {
                stack.pop();

                return true;
            }
        }

        for (ModelGroup childGroup : group.children)
        {
            if (processRenderRecursively(renderProcessor, builder, stack, model, childGroup))
            {
                stack.pop();

                return true;
            }
        }

        stack.pop();

        return false;
    }

    public static boolean processRenderModelAlwaysOnTop(ICubicRenderer renderProcessor, BufferBuilder builder, MatrixStack stack, Model model)
    {
        for (ModelGroup group : model.topGroups)
        {
            if (processRenderRecursivelyAlwaysOnTop(renderProcessor, builder, stack, model, group))
            {
                return true;
            }
        }

        return false;
    }

    private static boolean processRenderRecursivelyAlwaysOnTop(ICubicRenderer renderProcessor, BufferBuilder builder, MatrixStack stack, Model model, ModelGroup group)
    {
        stack.push();
        renderProcessor.applyGroupTransformations(stack, group);

        if (group.visible && group.alwaysOnTop)
        {
            if (renderProcessor.renderGroup(builder, stack, group, model))
            {
                stack.pop();
                return true;
            }
        }

        for (ModelGroup childGroup : group.children)
        {
            if (processRenderRecursivelyAlwaysOnTop(renderProcessor, builder, stack, model, childGroup))
            {
                stack.pop();
                return true;
            }
        }

        stack.pop();
        return false;
    }


    /**
     * Process/render a specific group by name
     *
     * This method finds the target group and renders it with proper transformations
     * from all parent groups applied.
     */
    public static boolean processRenderGroup(ICubicRenderer renderProcessor, BufferBuilder builder, MatrixStack stack, Model model, String groupName)
    {
        ModelGroup targetGroup = model.getGroup(groupName);

        if (targetGroup == null)
        {
            return false;
        }

        // Build the hierarchy path from root to target
        java.util.List<ModelGroup> hierarchy = new java.util.ArrayList<>();
        ModelGroup current = targetGroup;

        while (current != null)
        {
            hierarchy.add(0, current);
            current = current.parent;
        }

        // Apply transformations from root to target
        for (int i = 0; i < hierarchy.size(); i++)
        {
            ModelGroup group = hierarchy.get(i);
            stack.push();
            renderProcessor.applyGroupTransformations(stack, group);
        }

        // Render the target group
        boolean result = false;
        if (targetGroup.visible)
        {
            result = renderProcessor.renderGroup(builder, stack, targetGroup, model);
        }

        // Pop all transformations
        for (int i = 0; i < hierarchy.size(); i++)
        {
            stack.pop();
        }

        return result;
    }
}