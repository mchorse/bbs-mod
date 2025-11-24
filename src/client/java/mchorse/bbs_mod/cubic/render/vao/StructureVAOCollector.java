package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.utils.CollectionUtils;
import net.minecraft.client.render.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

public class StructureVAOCollector implements VertexConsumer
{
    private final List<Float> positions = new ArrayList<>();
    private final List<Float> normals = new ArrayList<>();
    private final List<Float> texCoords = new ArrayList<>();
    private final List<Float> tangents = new ArrayList<>();

    private final Vertex[] quad = new Vertex[]{new Vertex(), new Vertex(), new Vertex(), new Vertex()};
    private int quadIndex = 0;

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        Vertex vertex = this.quad[this.quadIndex];

        vertex.x = (float) x;
        vertex.y = (float) y;
        vertex.z = (float) z;

        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v)
    {
        Vertex vertex = this.quad[this.quadIndex];

        vertex.u = u;
        vertex.v = v;

        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v)
    {
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v)
    {
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        Vertex vertex = this.quad[this.quadIndex];

        vertex.nx = x;
        vertex.ny = y;
        vertex.nz = z;

        return this;
    }

    @Override
    public void next()
    {
        this.quadIndex += 1;

        if (this.quadIndex == 4)
        {
            /* Triangulate quad: (0,1,2) and (0,2,3) */
            this.emitTriangle(this.quad[0], this.quad[1], this.quad[2]);
            this.emitTriangle(this.quad[0], this.quad[2], this.quad[3]);

            this.quadIndex = 0;
        }
    }

    private void emitTriangle(Vertex a, Vertex b, Vertex c)
    {
        this.positions.add(a.x);
        this.positions.add(a.y);
        this.positions.add(a.z);
        this.positions.add(b.x);
        this.positions.add(b.y);
        this.positions.add(b.z);
        this.positions.add(c.x);
        this.positions.add(c.y);
        this.positions.add(c.z);

        this.normals.add(a.nx);
        this.normals.add(a.ny);
        this.normals.add(a.nz);
        this.normals.add(b.nx);
        this.normals.add(b.ny);
        this.normals.add(b.nz);
        this.normals.add(c.nx);
        this.normals.add(c.ny);
        this.normals.add(c.nz);

        this.texCoords.add(a.u);
        this.texCoords.add(a.v);
        this.texCoords.add(b.u);
        this.texCoords.add(b.v);
        this.texCoords.add(c.u);
        this.texCoords.add(c.v);

        float[] t = computeTriangleTangent(a, b, c);

        this.tangents.add(t[0]);
        this.tangents.add(t[1]);
        this.tangents.add(t[2]);
        this.tangents.add(1F);
        this.tangents.add(t[0]);
        this.tangents.add(t[1]);
        this.tangents.add(t[2]);
        this.tangents.add(1F);
        this.tangents.add(t[0]);
        this.tangents.add(t[1]);
        this.tangents.add(t[2]);
        this.tangents.add(1F);
    }

    private float[] computeTriangleTangent(Vertex a, Vertex b, Vertex c)
    {
        float x1 = b.x - a.x, y1 = b.y - a.y, z1 = b.z - a.z;
        float x2 = c.x - a.x, y2 = c.y - a.y, z2 = c.z - a.z;
        float u1 = b.u - a.u, v1 = b.v - a.v;
        float u2 = c.u - a.u, v2 = c.v - a.v;

        float denom = (u1 * v2 - u2 * v1);

        if (Math.abs(denom) < 1e-8f)
        {
            float len = (float) Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1);

            if (len < 1E-8F)
            {
                return new float[]{1F, 0F, 0F};
            }

            return new float[]{x1 / len, y1 / len, z1 / len};
        }

        float f = 1F / denom;
        float tx = f * (v2 * x1 - v1 * x2);
        float ty = f * (v2 * y1 - v1 * y2);
        float tz = f * (v2 * z1 - v1 * z2);
        float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);

        if (len < 1E-8F)
        {
            return new float[]{1F, 0F, 0F};
        }

        return new float[]{tx / len, ty / len, tz / len};
    }

    @Override
    public void fixedColor(int red, int green, int blue, int alpha)
    {}

    @Override
    public void unfixColor()
    {}

    public ModelVAOData toData()
    {
        return new ModelVAOData(
            CollectionUtils.toArray(this.positions),
            CollectionUtils.toArray(this.normals),
            CollectionUtils.toArray(this.tangents),
            CollectionUtils.toArray(this.texCoords)
        );
    }

    private static class Vertex
    {
        float x, y, z;
        float nx, ny, nz;
        float u, v;
    }
}