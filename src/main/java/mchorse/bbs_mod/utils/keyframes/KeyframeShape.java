package mchorse.bbs_mod.utils.keyframes;

public enum KeyframeShape
{
    SQUARE, CIRCLE, TRIANGLE, DIAMOND, TRI_STAR, FOUR_STAR, FIVE_STAR, SIX_STAR;

    public static KeyframeShape fromString(String s)
    {
        s = s.toUpperCase();

        for (KeyframeShape value : values())
        {
            if (value.name().equals(s))
            {
                return value;
            }
        }

        return SQUARE;
    }
}