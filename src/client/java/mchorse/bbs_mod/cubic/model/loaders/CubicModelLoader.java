package mchorse.bbs_mod.cubic.model.loaders;

import mchorse.bbs_mod.cubic.CubicLoader;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelData;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelMesh;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.obj.MeshOBJ;
import mchorse.bbs_mod.obj.MeshesOBJ;
import mchorse.bbs_mod.obj.OBJMaterial;
import mchorse.bbs_mod.obj.OBJParser;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.BoxPacker;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.PNGEncoder;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.resources.Pixels;
import org.joml.Vector2i;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CubicModelLoader implements IModelLoader
{
    @Override
    public ModelInstance load(String id, ModelManager models, Link model, Collection<Link> links, MapType config)
    {
        Link modelBBS = IModelLoader.getLink(model.combine("model.bbs.json"), links, ".bbs.json");
        Link modelTexture = IModelLoader.getLink(model.combine("model.png"), links, ".png");
        ModelInstance newModel = new ModelInstance(id, null, new Animations(models.parser), modelTexture);
        Map<String, MeshesOBJ> compile = this.tryLoadingOBJMeshes(models, model, IModelLoader.getLinks(links, ".obj"));
        Model theModel = null;

        try (InputStream stream = models.provider.getAsset(modelBBS))
        {
            CubicLoader loader = new CubicLoader();
            CubicLoader.LoadingInfo info = loader.load(models.parser, stream, modelBBS.path);

            if (info.model != null)
            {
                theModel = info.model;
            }

            if (info.animations != null)
            {
                for (Animation animation : info.animations.getAll())
                {
                    newModel.animations.add(animation);
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("Failed to load BBS file: " + modelBBS);
        }

        /* Construct the model from compiled data */
        if (!compile.isEmpty())
        {
            HashSet<String> declined = new HashSet<>();

            if (theModel == null)
            {
                theModel = new Model(models.parser);
                theModel.textureWidth = 1;
                theModel.textureHeight = 1;
            }

            this.tryMakingFlatMaterials(models.provider, links, model, newModel, theModel, compile);

            for (Map.Entry<String, MeshesOBJ> entry : compile.entrySet())
            {
                MeshesOBJ value = entry.getValue();
                ModelGroup group = theModel.getGroup(entry.getKey());

                if (group == null)
                {
                    group = new ModelGroup(entry.getKey());

                    theModel.topGroups.add(group);
                }

                for (MeshOBJ mesh : value.meshes)
                {
                    ModelMesh modelMesh = new ModelMesh();

                    modelMesh.baseData.fill(mesh, theModel.textureWidth, theModel.textureHeight);
                    group.meshes.add(modelMesh);
                }

                this.fillShapes(declined, value.shapes, group, theModel.textureWidth, theModel.textureHeight);
            }

            theModel.initialize();

            for (String s : declined)
            {
                System.out.println("Model \"" + model + "\" has shape keys \"" + s + "\" that have invalid shape keys (triangle count doesn't match)!");
            }
        }

        if (theModel == null || theModel.topGroups.isEmpty())
        {
            return null;
        }

        newModel.model = theModel;

        for (Animation animation : this.tryLoadingExternalAnimations(models, config).getAll())
        {
            newModel.animations.add(animation);
        }

        newModel.applyConfig(config);

        return newModel;
    }

    private void tryMakingFlatMaterials(AssetProvider provider, Collection<Link> links, Link model, ModelInstance newModel, Model theModel, Map<String, MeshesOBJ> compile)
    {
        Link paletteLink = model.combine("baked.png");
        File paletteFile = provider.getFile(paletteLink);

        /* Collect materials */
        Map<OBJMaterial, Pair<Pixels, Area>> pixels = new HashMap<>();
        List<OBJMaterial> materials = new ArrayList<>();

        for (MeshesOBJ value : compile.values())
        {
            for (MeshOBJ mesh : value.meshes)
            {
                if (mesh.material == null)
                {
                    continue;
                }

                if (!materials.contains(mesh.material))
                {
                    materials.add(mesh.material);
                }
            }
        }

        if (materials.isEmpty() || materials.get(0).useTexture)
        {
            return;
        }

        /* Read baked information and apply it */
        Link bakedOffsets = model.combine("baked.json");

        if (paletteFile.exists())
        {
            newModel.texture = paletteLink;

            try (InputStream stream = provider.getAsset(bakedOffsets))
            {
                MapType type = DataToString.mapFromString(IOUtils.readText(stream));
                ListType size = type.getList("size");
                MapType offsets = type.getMap("offsets");

                for (OBJMaterial material : materials)
                {
                    ListType list = offsets.getList(material.name);

                    if (list.size() >= 4)
                    {
                        pixels.putIfAbsent(material, new Pair<>(null, new Area(list.getInt(0), list.getInt(1), list.getInt(2), list.getInt(3))));
                    }
                }

                this.applyBakedOffsets(compile, pixels, new Vector2i(size.getInt(0), size.getInt(1)));
            }
            catch (Exception e)
            {}

            return;
        }

        /* Load pixels */
        for (OBJMaterial material : materials)
        {
            if (material.useTexture)
            {
                List<Link> materialTextures = IModelLoader.getLinks(links, (a) ->
                {
                    String string = a.toString();

                    return string.startsWith(model.toString()) && string.contains("/" + material.name + "/") && string.endsWith(".png");
                });

                Link link = materialTextures.get(0);

                try (InputStream stream = provider.getAsset(link))
                {
                    Pixels newPixels = Pixels.fromPNGStream(stream);
                    Area area = new Area();

                    pixels.put(material, new Pair<>(newPixels, area));
                    area.setSize(newPixels.width, newPixels.height);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                Pixels newPixels = Pixels.fromSize(1, 1);
                Area area = new Area();
                Color set = new Color().set(material.r, material.g, material.b, 1F);

                newPixels.setColor(0, 0, set);
                pixels.put(material, new Pair<>(newPixels, area));
                area.setSize(newPixels.width, newPixels.height);
            }
        }

        /* Pack boxes to occupy the least space */
        List<Area> boxes = new ArrayList<>();

        for (Pair<Pixels, Area> value : pixels.values()) 
        {
            boxes.add(value.b);
        }

        Vector2i size = BoxPacker.pack(boxes, 0);

        /* Bake all textures into a single texture and delete pixels */
        Pixels output = Pixels.fromSize(size.x, size.y);

        for (Pair<Pixels, Area> value : pixels.values())
        {
            output.draw(value.a, value.b.x, value.b.y);
            value.a.delete();
        }
        
        try
        {
            /* Write the baked texture to PNG */
            PNGEncoder.writeToFile(output, paletteFile);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        output.delete();

        this.applyBakedOffsets(compile, pixels, size);

        MapType data = new MapType();
        ListType dSize = new ListType();
        MapType dOffsets = new MapType();

        data.put("size", dSize);
        data.put("offsets", dOffsets);

        for (Map.Entry<OBJMaterial, Pair<Pixels, Area>> entry : pixels.entrySet())
        {
            ListType dOffset = new ListType();

            dOffset.addInt(entry.getValue().b.x);
            dOffset.addInt(entry.getValue().b.y);
            dOffset.addInt(entry.getValue().b.w);
            dOffset.addInt(entry.getValue().b.h);
            dOffsets.put(entry.getKey().name, dOffset);
        }

        dSize.addInt(size.x);
        dSize.addInt(size.y);

        DataToString.writeSilently(provider.getFile(bakedOffsets), data, true);

        newModel.texture = paletteLink;
        theModel.textureWidth = 1;
        theModel.textureHeight = 1;
    }

    private void applyBakedOffsets(Map<String, MeshesOBJ> compile, Map<OBJMaterial, Pair<Pixels, Area>> pixels, Vector2i size)
    {
        /* Remap UV coordinates */
        for (MeshesOBJ value : compile.values())
        {
            for (MeshOBJ mesh : value.meshes)
            {
                Pair<Pixels, Area> pair = pixels.get(mesh.material);

                for (int i = 0, c = mesh.triangles; i < c; i++)
                {
                    float u = mesh.texData[i * 2];
                    float v = mesh.texData[i * 2 + 1];

                    u = (u * pair.b.w + pair.b.x) / (float) size.x;
                    v = (v * pair.b.h + pair.b.y) / (float) size.y;

                    mesh.texData[i * 2] = u;
                    mesh.texData[i * 2 + 1] = v;
                }
            }
        }
    }

    /**
     * Load OBJ meshes from multiple files
     */
    private Map<String, MeshesOBJ> tryLoadingOBJMeshes(ModelManager models, Link model, List<Link> modelsOBJ)
    {
        Map<String, MeshesOBJ> compile = new HashMap<>();

        /* Load the base OBJ file */
        for (Link link : modelsOBJ)
        {
            String path = link.path.substring(model.path.length() + 1);

            if (path.contains("/"))
            {
                continue;
            }

            Link mtl = new Link(link.source, StringUtils.removeExtension(link.path) + ".mtl");

            try (InputStream stream = models.provider.getAsset(link))
            {
                InputStream mtlStream = null;

                try
                {
                    mtlStream = models.provider.getAsset(mtl);
                }
                catch (Exception e)
                {}

                OBJParser parser = new OBJParser(stream, mtlStream);

                parser.read();
                compile.putAll(parser.compile());

                if (mtlStream != null)
                {
                    mtlStream.close();
                }

                break;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        /* Load shapes from shapes folder */
        for (Link link : modelsOBJ)
        {
            if (!link.path.contains("/shapes/"))
            {
                continue;
            }

            try (InputStream stream = models.provider.getAsset(link))
            {
                OBJParser parser = new OBJParser(stream, null);
                String name = StringUtils.fileName(StringUtils.removeExtension(link.path));

                parser.read();

                Map<String, MeshesOBJ> compiled = parser.compile();

                for (Map.Entry<String, MeshesOBJ> entry : compiled.entrySet())
                {
                    MeshesOBJ meshesOBJ = compile.get(entry.getKey());

                    if (meshesOBJ == null)
                    {
                        compile.put(entry.getKey(), entry.getValue());
                    }
                    else
                    {
                        meshesOBJ.mergeShape(name, entry.getValue());
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return compile;
    }

    private void fillShapes(Set<String> declined, Map<String, List<MeshOBJ>> shapes, ModelGroup group, int tw, int th)
    {
        if (shapes == null)
        {
            return;
        }

        for (Map.Entry<String, List<MeshOBJ>> shapeEntry : shapes.entrySet())
        {
            int i = 0;

            for (MeshOBJ mesh : shapeEntry.getValue())
            {
                ModelMesh modelMesh = CollectionUtils.getSafe(group.meshes, i);
                ModelData data = new ModelData();

                data.fill(mesh, tw, th);

                if (
                    data.vertices.size() == modelMesh.baseData.vertices.size() &&
                    data.normals.size() == modelMesh.baseData.normals.size() &&
                    data.uvs.size() == modelMesh.baseData.uvs.size()
                ) {
                    modelMesh.data.put(shapeEntry.getKey(), data);
                }
                else
                {
                    declined.add(shapeEntry.getKey());
                }

                i += 1;
            }
        }
    }

    /**
     * Loading external animations mentioned in the config
     */
    private Animations tryLoadingExternalAnimations(ModelManager models, MapType config)
    {
        Animations animations = new Animations(models.parser);

        if (config == null)
        {
            return animations;
        }

        for (BaseType type : config.getList("animations"))
        {
            if (type.isString())
            {
                Link animationFile = Link.create(type.asString());

                try (InputStream asset = models.provider.getAsset(animationFile))
                {
                    CubicLoader loader = new CubicLoader();
                    CubicLoader.LoadingInfo info = loader.load(models.parser, asset, type.asString());

                    if (info.animations != null)
                    {
                        for (Animation animation : info.animations.getAll())
                        {
                            animations.add(animation);
                        }
                    }
                }
                catch (FileNotFoundException e)
                {
                    return new Animations(models.parser);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        return animations;
    }
}