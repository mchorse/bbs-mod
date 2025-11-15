package mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeShape;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

import java.util.*;

public class UIKeyframeShapeFactory
{
    private static final Map<KeyframeShape, ShapeFactory> SHAPES = new HashMap<KeyframeShape, ShapeFactory>();

    static {

        SHAPES.put(KeyframeShape.SQUARE, UISquareKeyframeShape::new);
        SHAPES.put(KeyframeShape.TRIANGLE, UITriangleKeyframeShape::new);
        SHAPES.put(KeyframeShape.DIAMOND, UIDiamondKeyframeShape::new);
        SHAPES.put(KeyframeShape.TRI_STAR, UITriStarKeyframeShape::new);
        SHAPES.put(KeyframeShape.FOUR_STAR, UIFourStarKeyframeShape::new);
        SHAPES.put(KeyframeShape.FIVE_STAR, UIFiveStarKeyframeShape::new);
        SHAPES.put(KeyframeShape.SIX_STAR, UISixStarKeyframeShape::new);
        SHAPES.put(KeyframeShape.CIRCLE, UICircleKeyframeShape::new);

        /*
        SHAPES.put(KeyframeShape.HEART, UIHeartKeyframeShape::new);
        */
    }

    public static IUIKeyframeShape createShape(KeyframeShape shape, UIContext context,
                                               BufferBuilder builder, Matrix4f matrix,
                                               int x, int y, int offset, int c) {
        ShapeFactory factory = SHAPES.get(shape);
        if (factory == null) {
            factory = SHAPES.get(KeyframeShape.SQUARE);
        }
        return factory.create(context, builder, matrix, x, y, offset, c);
    }

    public static Map<KeyframeShape, IUIKeyframeShape> getAllShapes()
    {
        Map<KeyframeShape, IUIKeyframeShape> shapes = new HashMap<>();

        for(Map.Entry<KeyframeShape, ShapeFactory> sh : SHAPES.entrySet())
        {
            IUIKeyframeShape keyframeShape = sh.getValue().create(null, null, null, 0, 0, 0, 0);
            shapes.put(sh.getKey(), keyframeShape);
        }

        return shapes;
    }

    @FunctionalInterface
    public interface ShapeFactory
    {
        IUIKeyframeShape create(UIContext context, BufferBuilder builder, Matrix4f matrix,
                                int x, int y, int offset, int c);
    }



}
