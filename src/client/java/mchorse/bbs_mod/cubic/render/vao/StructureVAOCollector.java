package mchorse.bbs_mod.cubic.render.vao;

import net.minecraft.client.render.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects block model vertices emitted via VertexConsumer and converts quads to triangles,
 * producing arrays suitable for {@link ModelVAO} upload.
 */
public class StructureVAOCollector implements VertexConsumer
{
    private static class Vtx
    {
        float x, y, z;
        float nx, ny, nz;
        float u, v;
    }

    private final List<Float> positions = new ArrayList<>();
    private final List<Float> normals = new ArrayList<>();
    private final List<Float> texCoords = new ArrayList<>();
    private final List<Float> tangents = new ArrayList<>();

    private final Vtx[] quad = new Vtx[4];
    private int quadIndex = 0;

    // working per-vertex state until next()
    private float vx, vy, vz;
    private float vnx, vny, vnz;
    private float vu, vv;

    public StructureVAOCollector()
    {
        for (int i = 0; i < 4; i++) quad[i] = new Vtx();
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        this.vx = (float) x;
        this.vy = (float) y;
        this.vz = (float) z;
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        // Per-vertex color is not used; global color is provided via shader attribute.
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v)
    {
        this.vu = u;
        this.vv = v;
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v)
    {
        // Overlay provided via shader attribute; ignore per-vertex overlay.
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v)
    {
        // Lightmap provided via shader attribute; ignore per-vertex light.
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        this.vnx = x;
        this.vny = y;
        this.vnz = z;
        return this;
    }

    @Override
    public void next()
    {
        Vtx v = quad[quadIndex];
        v.x = vx; v.y = vy; v.z = vz;
        v.nx = vnx; v.ny = vny; v.nz = vnz;
        v.u = vu; v.v = vv;

        quadIndex++;

        if (quadIndex == 4)
        {
            // Triangulate quad: (0,1,2) and (0,2,3)
            emitTriangle(quad[0], quad[1], quad[2]);
            emitTriangle(quad[0], quad[2], quad[3]);
            quadIndex = 0;
        }
    }

    private void emitTriangle(Vtx a, Vtx b, Vtx c)
    {
        // Positions
        positions.add(a.x); positions.add(a.y); positions.add(a.z);
        positions.add(b.x); positions.add(b.y); positions.add(b.z);
        positions.add(c.x); positions.add(c.y); positions.add(c.z);

        // Normals
        normals.add(a.nx); normals.add(a.ny); normals.add(a.nz);
        normals.add(b.nx); normals.add(b.ny); normals.add(b.nz);
        normals.add(c.nx); normals.add(c.ny); normals.add(c.nz);

        // UVs
        texCoords.add(a.u); texCoords.add(a.v);
        texCoords.add(b.u); texCoords.add(b.v);
        texCoords.add(c.u); texCoords.add(c.v);

        // Tangents (per-triangle)
        // Compute tangent vector from positions and UVs
        float[] t = computeTriangleTangent(a, b, c);
        // Write tangent with w=1 for each vertex of the triangle
        tangents.add(t[0]); tangents.add(t[1]); tangents.add(t[2]); tangents.add(1F);
        tangents.add(t[0]); tangents.add(t[1]); tangents.add(t[2]); tangents.add(1F);
        tangents.add(t[0]); tangents.add(t[1]); tangents.add(t[2]); tangents.add(1F);
    }

    private float[] computeTriangleTangent(Vtx a, Vtx b, Vtx c)
    {
        float x1 = b.x - a.x, y1 = b.y - a.y, z1 = b.z - a.z;
        float x2 = c.x - a.x, y2 = c.y - a.y, z2 = c.z - a.z;
        float u1 = b.u - a.u, v1 = b.v - a.v;
        float u2 = c.u - a.u, v2 = c.v - a.v;

        float denom = (u1 * v2 - u2 * v1);
        if (Math.abs(denom) < 1e-8f)
        {
            // Fallback: use a normalized edge as tangent
            float len = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);
            if (len < 1e-8f) return new float[]{1F, 0F, 0F};
            return new float[]{x1 / len, y1 / len, z1 / len};
        }

        float f = 1.0f / denom;
        float tx = f * (v2 * x1 - v1 * x2);
        float ty = f * (v2 * y1 - v1 * y2);
        float tz = f * (v2 * z1 - v1 * z2);

        float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (len < 1e-8f) return new float[]{1F, 0F, 0F};
        return new float[]{tx / len, ty / len, tz / len};
    }

    @Override
    public void fixedColor(int red, int green, int blue, int alpha)
    {
        // no-op
    }

    @Override
    public void unfixColor()
    {
        // no-op
    }

    public ModelVAOData toData()
    {
        float[] v = toArray(positions);
        float[] n = toArray(normals);
        float[] t = toArray(tangents);
        float[] uv = toArray(texCoords);
        return new ModelVAOData(v, n, t, uv);
    }

    private static float[] toArray(List<Float> list)
    {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }
}