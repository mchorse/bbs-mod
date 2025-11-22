package mchorse.bbs_mod.cubic.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelUV;
import mchorse.bbs_mod.math.molang.MolangParser;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoModelParser
{
    public static Model parse(JsonObject object, MolangParser parser)
    {
        Model model = new Model(parser);

        object = object.get("minecraft:geometry").getAsJsonArray().get(0).getAsJsonObject();

        if (object.has("description"))
        {
            parseDescription(model, object.get("description").getAsJsonObject());
        }

        if (object.has("bones"))
        {
            parseBones(model, object.get("bones").getAsJsonArray());
        }

        model.initialize();

        return model;
    }

    private static void parseDescription(Model model, JsonObject object)
    {
        if (object.has("texture_width"))
        {
            model.textureWidth = object.get("texture_width").getAsInt();
        }

        if (object.has("texture_height"))
        {
            model.textureHeight = object.get("texture_height").getAsInt();
        }
    }

    private static void parseBones(Model model, JsonArray bones)
    {
        Map<String, List<String>> hierarchy = new HashMap<String, List<String>>();
        Map<String, ModelGroup> flatBones = new HashMap<>();

        for (JsonElement element : bones)
        {
            JsonObject boneElement = element.getAsJsonObject();
            ModelGroup bone = new ModelGroup(boneElement.get("name").getAsString());

            /* Fill hierarchy information */
            String parent = boneElement.has("parent") ? boneElement.get("parent").getAsString() : "";
            List<String> list = hierarchy.computeIfAbsent(parent, (k) -> new ArrayList<>());

            list.add(bone.id);

            /* Setup initial transformations */
            if (boneElement.has("pivot"))
            {
                parseVector(boneElement.get("pivot"), bone.initial.translate);
                bone.initial.pivot.set(bone.initial.translate);
            }

            if (boneElement.has("scale"))
            {
                parseVector(boneElement.get("scale"), bone.initial.scale);
            }

            if (boneElement.has("rotation"))
            {
                parseVector(boneElement.get("rotation"), bone.initial.rotate);

                bone.initial.rotate.x *= -1;
                bone.initial.rotate.y *= -1;
            }

            bone.initial.translate.x *= -1;
            bone.initial.pivot.x *= -1;

            /* Setup cubes */
            if (boneElement.has("cubes"))
            {
                parseCubes(model, bone, boneElement.get("cubes").getAsJsonArray());
            }

            flatBones.put(bone.id, bone);
        }

        /* Setup hierarchy */
        for (Map.Entry<String, List<String>> entry : hierarchy.entrySet())
        {
            if (entry.getKey().isEmpty())
            {
                continue;
            }

            ModelGroup bone = flatBones.get(entry.getKey());

            for (String child : entry.getValue())
            {
                bone.children.add(flatBones.get(child));
            }
        }

        List<String> topLevel = hierarchy.get("");

        if (topLevel != null)
        {
            for (String topLevelBone : topLevel)
            {
                model.topGroups.add(flatBones.get(topLevelBone));
            }
        }
    }

    private static void parseCubes(Model model, ModelGroup bone, JsonArray cubes)
    {
        for (JsonElement element : cubes)
        {
            bone.cubes.add(parseCube(model, element.getAsJsonObject()));
        }
    }

    private static ModelCube parseCube(Model model, JsonObject object)
    {
        ModelCube cube = new ModelCube();

        if (object.has("inflate"))
        {
            cube.inflate = object.get("inflate").getAsFloat();
        }

        parseVector(object.get("origin"), cube.origin);
        parseVector(object.get("size"), cube.size);

        cube.origin.x *= -1;
        cube.origin.x -= cube.size.x;

        if (object.has("pivot"))
        {
            parseVector(object.get("pivot"), cube.pivot);

            cube.pivot.x *= -1;
        }
        else
        {
            cube.pivot.set(cube.origin);
        }

        if (object.has("rotation"))
        {
            parseVector(object.get("rotation"), cube.rotate);

            cube.rotate.x *= -1;
            cube.rotate.y *= -1;
        }

        if (object.has("uv"))
        {
            boolean mirror = object.has("mirror") && object.get("mirror").getAsBoolean();

            parseUV(cube, object.get("uv"), mirror);
        }

        cube.generateQuads(model.textureWidth, model.textureHeight);

        return cube;
    }

    private static void parseUV(ModelCube cube, JsonElement element, boolean mirror)
    {
        if (element.isJsonArray())
        {
            Vector2f boxUV = new Vector2f();

            parseVector(element.getAsJsonArray(), boxUV);
            cube.setupBoxUV(boxUV, mirror);
        }
        else if (element.isJsonObject())
        {
            JsonObject sides = element.getAsJsonObject();

            if (sides.has("north")) cube.front = parseUVSide(sides.get("north").getAsJsonObject());
            if (sides.has("east")) cube.right = parseUVSide(sides.get("east").getAsJsonObject());
            if (sides.has("south")) cube.back = parseUVSide(sides.get("south").getAsJsonObject());
            if (sides.has("west")) cube.left = parseUVSide(sides.get("west").getAsJsonObject());
            if (sides.has("up"))
            {
                cube.top = parseUVSide(sides.get("up").getAsJsonObject());
                cube.top.size.mul(-1);
                cube.top.origin.sub(cube.top.size);
            }
            if (sides.has("down"))
            {
                cube.bottom = parseUVSide(sides.get("down").getAsJsonObject());
                cube.bottom.size.mul(-1);
                cube.bottom.origin.sub(cube.bottom.size);
            }
        }
    }

    private static ModelUV parseUVSide(JsonObject uvSide)
    {
        ModelUV uv = new ModelUV();

        parseVector(uvSide.get("uv"), uv.origin);
        parseVector(uvSide.get("uv_size"), uv.size);

        return uv;
    }

    private static void parseVector(JsonElement element, Vector3f vector)
    {
        JsonArray array = element.getAsJsonArray();

        vector.x = array.get(0).getAsFloat();
        vector.y = array.get(1).getAsFloat();
        vector.z = array.get(2).getAsFloat();
    }

    private static void parseVector(JsonElement element, Vector2f vector)
    {
        JsonArray array = element.getAsJsonArray();

        vector.x = array.get(0).getAsFloat();
        vector.y = array.get(1).getAsFloat();
    }
}