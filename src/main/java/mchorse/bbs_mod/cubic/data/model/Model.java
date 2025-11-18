package mchorse.bbs_mod.cubic.data.model;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.CubicModelAnimator;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.MolangHelper;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.math.molang.MolangParser;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Model implements IMapSerializable, IModel
{
    public int textureWidth;
    public int textureHeight;

    public final MolangParser parser;

    /**
     * This list contains only the root groups of the model (and not all of the groups)
     */
    public List<ModelGroup> topGroups = new ArrayList<>();

    private Map<String, ModelGroup> namedGroups = new HashMap<>();
    private List<ModelGroup> orderedGroups = new ArrayList<>();
    private Set<String> shapeKeys = new HashSet<>();
    private int nextIndex;

    public Model(MolangParser parser)
    {
        this.parser = parser;
    }

    public void initialize()
    {
        this.nextIndex = 0;
        this.namedGroups.clear();
        this.orderedGroups.clear();
        this.shapeKeys.clear();

        this.fillGroups(this.topGroups, null);

        for (ModelGroup orderedGroup : this.orderedGroups)
        {
            for (ModelMesh mesh : orderedGroup.meshes)
            {
                this.shapeKeys.addAll(mesh.data.keySet());
            }
        }
    }

    private void fillGroups(List<ModelGroup> groups, ModelGroup parent)
    {
        for (ModelGroup group : groups)
        {
            this.namedGroups.put(group.id, group);
            this.orderedGroups.add(group);

            group.parent = parent;
            group.owner = this;
            group.index = this.nextIndex;
            this.nextIndex += 1;

            this.fillGroups(group.children, group);
        }
    }

    public List<ModelGroup> getOrderedGroups()
    {
        return this.orderedGroups;
    }

    public ModelGroup getGroup(String id)
    {
        return this.namedGroups.get(id);
    }

    /* IModel implementation */

    @Override
    public Pose createPose()
    {
        Pose pose = new Pose();

        for (String key : this.getAllGroupKeys())
        {
            PoseTransform poseTransform = pose.get(key);
            ModelGroup group = this.getGroup(key);

            poseTransform.copy(group.current);
            poseTransform.translate.sub(group.initial.translate);
            poseTransform.pivot.sub(group.initial.pivot);
            poseTransform.rotate.sub(group.initial.rotate);

            poseTransform.rotate.x = MathUtils.toRad(poseTransform.rotate.x);
            poseTransform.rotate.y = MathUtils.toRad(poseTransform.rotate.y);
            poseTransform.rotate.z = MathUtils.toRad(poseTransform.rotate.z);
        }

        return pose;
    }

    @Override
    public void resetPose()
    {
        for (ModelGroup orderedGroup : this.orderedGroups)
        {
            orderedGroup.reset();
        }
    }

    @Override
    public void applyPose(Pose pose)
    {
        if (pose.isEmpty())
        {
            return;
        }

        for (Map.Entry<String, PoseTransform> entry : pose.transforms.entrySet())
        {
            PoseTransform transform = entry.getValue();
            ModelGroup group = this.getGroup(entry.getKey());

            if (group == null)
            {
                continue;
            }

            if (transform.fix > 0F)
            {
                group.current.lerp(group.initial, transform.fix);
            }

            group.lighting = transform.lighting;
            group.color.copy(transform.color);
            group.current.translate.add(transform.translate);
            group.current.scale.add(transform.scale).sub(1, 1, 1);
            /* Para modelos/pose: el pivote de la pose debe sobreponer la posición
             * sin alterar el modelo original, y además actualizar el punto de rotación.
             * Aplicamos el delta de pivot a la traslación (override de posición)
             * y también al pivot actual (centro de rotación de la pose). */
            group.current.translate.add(transform.pivot);
            group.current.pivot.add(transform.pivot);
            group.current.rotate.add(
                (float) Math.toDegrees(transform.rotate.x),
                (float) Math.toDegrees(transform.rotate.y),
                (float) Math.toDegrees(transform.rotate.z)
            );
            group.current.rotate2.add(
                (float) Math.toDegrees(transform.rotate2.x),
                (float) Math.toDegrees(transform.rotate2.y),
                (float) Math.toDegrees(transform.rotate2.z)
            );
        }
    }

    @Override
    public Set<String> getShapeKeys()
    {
        return this.shapeKeys;
    }

    @Override
    public String getAnchor()
    {
        return !this.topGroups.isEmpty() ? this.topGroups.get(0).id : "";
    }

    @Override
    public Set<String> getAllGroupKeys()
    {
        return this.namedGroups.keySet();
    }

    @Override
    public Collection<String> getAllChildrenKeys(String key)
    {
        ModelGroup group = this.namedGroups.get(key);
        List<String> groups = new ArrayList<>();

        this.collectChildrenKeys(group, groups);

        return groups;
    }

    private void collectChildrenKeys(ModelGroup group, List<String> groups)
    {
        for (ModelGroup child : group.children)
        {
            groups.add(child.id);
            this.collectChildrenKeys(child, groups);
        }
    }

    @Override
    public Collection<ModelGroup> getAllGroups()
    {
        return this.namedGroups.values();
    }

    @Override
    public Collection<BOBJBone> getAllBOBJBones()
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getAdjacentGroups(String groupName)
    {
        ModelGroup group = this.getGroup(groupName);
        List<ModelGroup> groups = group.parent != null ? group.parent.children : this.topGroups;

        return groups.stream().map((g) -> g.id).toList();
    }

    @Override
    public Collection<String> getHierarchyGroups(String groupName)
    {
        ModelGroup group = this.getGroup(groupName);
        List<String> groups = new ArrayList<>();

        while (group != null)
        {
            groups.add(group.id);

            group = group.parent;
        }

        return groups;
    }

    @Override
    public void apply(IEntity target, Animation action, float tick, float blend, float transition, boolean skipInitial)
    {
        MolangHelper.setMolangVariables(this.parser, target, tick, transition);
        CubicModelAnimator.animate(this, action, tick, blend, skipInitial);
    }

    @Override
    public void postApply(IEntity target, Animation action, float tick, float transition)
    {
        MolangHelper.setMolangVariables(this.parser, target, tick, transition);
        CubicModelAnimator.postAnimate(this, action, tick);
    }

    /* Deserialization / Serialization */

    @Override
    public void fromData(MapType data)
    {
        ListType texture = data.getList("texture");

        this.textureWidth = texture.getInt(0);
        this.textureHeight = texture.getInt(1);

        MapType groups = data.getMap("groups");
        Map<String, List<String>> hierarchy = new HashMap<>();
        Map<String, ModelGroup> flatGroups = new HashMap<>();

        for (String key : groups.keys())
        {
            MapType groupElement = groups.getMap(key);
            ModelGroup group = new ModelGroup(key);

            /* Fill hierarchy information */
            String parent = groupElement.has("parent") ? groupElement.getString("parent") : "";
            List<String> list = hierarchy.computeIfAbsent(parent, (k) -> new ArrayList<>());

            list.add(group.id);

            group.fromData(groupElement);

            for (ModelCube cube : group.cubes)
            {
                cube.generateQuads(this.textureWidth, this.textureHeight);
            }

            flatGroups.put(group.id, group);
        }

        /* Setup hierarchy */
        for (Map.Entry<String, List<String>> entry : hierarchy.entrySet())
        {
            if (entry.getKey().isEmpty())
            {
                continue;
            }

            ModelGroup group = flatGroups.get(entry.getKey());

            for (String child : entry.getValue())
            {
                group.children.add(flatGroups.get(child));
            }
        }

        List<String> topLevel = hierarchy.get("");

        if (topLevel != null)
        {
            for (String rootGroup : topLevel)
            {
                this.topGroups.add(flatGroups.get(rootGroup));
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        ListType texture = new ListType();

        texture.addInt(this.textureWidth);
        texture.addInt(this.textureHeight);

        Map<String, String> parents = new HashMap<>();
        Collection<ModelGroup> allGroups = this.getAllGroups();

        for (ModelGroup parent : allGroups)
        {
            for (ModelGroup child : parent.children)
            {
                parents.put(child.id, parent.id);
            }
        }

        MapType groups = new MapType();

        for (ModelGroup group : allGroups)
        {
            MapType groupData = group.toData();
            String parentId = parents.get(group.id);

            if (parentId != null)
            {
                groupData.putString("parent", parentId);
            }

            groups.put(group.id, groupData);
        }

        data.put("texture", texture);
        data.put("groups", groups);
    }
}