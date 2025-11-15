package mchorse.bbs_mod.utils.keyframes;

public enum KeyframeShape
{

    SQUARE, CIRCLE, TRIANGLE, DIAMOND, TRI_STAR, FOUR_STAR, FIVE_STAR, SIX_STAR, HEART, THREE_DIMENSION_CUBE;


    public static KeyframeShape fromString(String s)
    {
        return switch (s.toUpperCase())
        {
            case "SQUARE" -> SQUARE;
            case "CIRCLE" -> CIRCLE;
            case "TRIANGLE" -> TRIANGLE;
            case "DIAMOND" -> DIAMOND;
            case "TRI_STAR" -> TRI_STAR;
            case "FOUR_STAR" -> FOUR_STAR;
            case "FIVE_STAR" -> FIVE_STAR;
            case "SIX_STAR" -> SIX_STAR;
            case "HEART" -> HEART;
            case "THREE_DIMENSION_CUBE" -> THREE_DIMENSION_CUBE;
            default -> SQUARE;
        };
    }

}
