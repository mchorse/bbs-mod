package mchorse.bbs_mod.utils;

import org.joml.Vector3i;

import java.util.Collection;

public class MathUtils
{
    public static final float PI = (float) Math.PI;

    public static float toRad(float degrees)
    {
        return degrees / 180F * PI;
    }

    public static float toDeg(float rad)
    {
        return rad / PI * 180F;
    }

    public static int clamp(int x, int min, int max)
    {
        return x < min ? min : (x > max ? max : x);
    }

    public static float clamp(float x, float min, float max)
    {
        return x < min ? min : (x > max ? max : x);
    }

    public static double clamp(double x, double min, double max)
    {
        return x < min ? min : (x > max ? max : x);
    }

    public static long clamp(long x, long min, long max)
    {
        return x < min ? min : (x > max ? max : x);
    }

    public static int cycler(int x, Collection collection)
    {
        return cycler(x, 0, collection.size() - 1);
    }

    public static int cycler(int x, int min, int max)
    {
        return x < min ? max : (x > max ? min : x);
    }

    public static float cycler(float x, float min, float max)
    {
        return x < min ? max : (x > max ? min : x);
    }

    public static double cycler(double x, double min, double max)
    {
        return x < min ? max : (x > max ? min : x);
    }

    public static int gridIndex(int x, int y, int size, int width)
    {
        x = x / size;
        y = y / size;

        return x + y * width / size;
    }

    public static int gridRows(int count, int size, int width)
    {
        double x = count * size / (double) width;

        return count <= 0 ? 1 : (int) Math.ceil(x);
    }

    /**
     * Converts given value to chunk coordinate (helps with negative values)
     */
    public static int toChunk(float x, int chunkSize)
    {
        return (int) ((x < 0 ? x - (chunkSize - 1) : x) / chunkSize);
    }

    /**
     * Converts given value to chunk coordinate (helps with negative values)
     */
    public static int toChunk(double x, int chunkSize)
    {
        return (int) ((x < 0 ? x - (chunkSize - 1) : x) / chunkSize);
    }

    /**
     * Converts given index into a 3D block coordinate
     */
    public static Vector3i toBlock(int i, int w, int h, Vector3i vector)
    {
        int c = i % (w * h);
        int z = i / (w * h);
        int y = c / w;
        int x = c % w;

        return vector.set(x, y, z);
    }

    /**
     * Normalize given angle (in degrees) to be in -180 to 180 number range
     */
    public static float normalizeDegrees(float angle)
    {
        return normalizeAngle(angle, 180);
    }

    /**
     * Normalize given angle (in radians) to be in -pi to pi number range
     */
    public static float normalizeRadians(float angle)
    {
        return normalizeAngle(angle, PI);
    }

    private static float normalizeAngle(float angle, float halfCircle)
    {
        if (Float.isNaN(angle))
        {
            angle = 0;
        }

        angle %= halfCircle * 2;

        if (angle > halfCircle)
        {
            return -halfCircle + (angle - halfCircle);
        }

        return halfCircle + (angle + halfCircle);
    }

    /**
     * Wrap/normalize given radian angle to 0..2PI.
     */
    public static float wrapToCircle(float rad)
    {
        float circle = PI * 2;

        if (rad >= 0)
        {
            return rad % circle;
        }

        float times = (float) Math.ceil(rad / -circle);

        return rad + circle * times;
    }

    /**
     * Whether segments a and b are intersecting.
     *
     *     an          ax
     *     [ ---------- ]
     *               bn            bx
     *               [ ------------ ]
     *
     *               an            ax
     *               [ ------------ ]
     *     bn          bx
     *     [ ---------- ]
     */
    public static boolean isInside(double an, double ax, double bn, double bx)
    {
        return an < bx && bn < ax;
    }

    public static int remapIndex(int old, int from, int to)
    {
        if (from == to) return old;

        if (from < to)
        {
            /* Moving item down: [from+1..to] shift left by 1 */
            if (old == from) return to;
            if (old > from && old <= to) return old - 1;

            return old;
        }
        else
        {
            /* from > to: moving item up: [to..from-1] shift right by 1 */
            if (old == from) return to;
            if (old >= to && old < from) return old + 1;

            return old;
        }
    }
}