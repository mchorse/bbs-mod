package mchorse.bbs_mod.cubic.data.model;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.ArrayList;
import java.util.List;

public class ModelGroup implements IMapSerializable
{
    public final String id;
    public Model owner;
    public ModelGroup parent;
    public List<ModelGroup> children = new ArrayList<>();
    public List<ModelCube> cubes = new ArrayList<>();
    public List<ModelMesh> meshes = new ArrayList<>();
    public boolean visible = true;
    public int index = -1;

    public float lighting = 0F;
    public Color color = new Color().set(1F, 1F, 1F);
    /** Optional texture override applied at render-time (e.g., per-bone). */
    public Link textureOverride;
    public Transform initial = new Transform();
    public Transform current = new Transform();

    public ModelGroup(String id)
    {
        this.id = id;
    }

    public void reset()
    {
        this.lighting = 0F;
        this.color.set(1F, 1F, 1F);
        this.textureOverride = null;
        this.current.copy(this.initial);
    }

    @Override
    public void fromData(MapType data)
    {
        /* Setup initial transformations */
        if (data.has("origin")) this.initial.translate.set(DataStorageUtils.vector3fFromData(data.getList("origin")));
        if (data.has("rotate")) this.initial.rotate.set(DataStorageUtils.vector3fFromData(data.getList("rotate")));
        if (data.has("pivot")) this.initial.pivot.set(DataStorageUtils.vector3fFromData(data.getList("pivot")));
        else this.initial.pivot.set(this.initial.translate);

        /* Setup cubes and meshes */
        if (data.has("cubes"))
        {
            for (BaseType element : data.getList("cubes"))
            {
                ModelCube cube = new ModelCube();

                cube.fromData((MapType) element);

                this.cubes.add(cube);
            }

        }

        if (data.has("meshes"))
        {
            for (BaseType element : data.getList("meshes"))
            {
                ModelMesh mesh = new ModelMesh();

                mesh.fromData((MapType) element);

                this.meshes.add(mesh);
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        data.put("origin", DataStorageUtils.vector3fToData(this.initial.translate));
        data.put("rotate", DataStorageUtils.vector3fToData(this.initial.rotate));
        data.put("pivot", DataStorageUtils.vector3fToData(this.initial.pivot));

        if (!this.cubes.isEmpty())
        {
            ListType list = new ListType();

            for (ModelCube cube : this.cubes)
            {
                list.add(cube.toData());
            }

            data.put("cubes", list);
        }

        if (!this.meshes.isEmpty())
        {
            ListType list = new ListType();

            for (ModelMesh mesh : this.meshes)
            {
                list.add(mesh.toData());
            }

            data.put("meshes", list);
        }
    }
}