package mchorse.bbs_mod.cubic.model.bobj;

import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.MolangHelper;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.render.vao.BOBJModelSimpleVAO;
import mchorse.bbs_mod.cubic.render.vao.BOBJModelVAO;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BOBJModel implements IModel
{
    private BOBJArmature armature;
    private BOBJLoader.CompiledData meshData;

    private BOBJModelVAO vao;
    private boolean simple;

    public BOBJModel(BOBJArmature armature, BOBJLoader.CompiledData meshData, boolean simple)
    {
        this.armature = armature;
        this.meshData = meshData;
        this.simple = simple;
    }

    public BOBJArmature getArmature()
    {
        return this.armature;
    }

    public BOBJLoader.CompiledData getMeshData()
    {
        return this.meshData;
    }

    public BOBJModelVAO getVao()
    {
        return this.vao;
    }

    public void delete()
    {
        if (this.vao != null)
        {
            this.vao.delete();

            this.vao = null;
        }
    }

    public void setup()
    {
        this.vao = this.simple
            ? new BOBJModelSimpleVAO(this.meshData)
            : new BOBJModelVAO(this.meshData);

        this.armature.setupMatrices();
    }

    @Override
    public Pose createPose()
    {
        Pose pose = new Pose();

        for (String key : this.getAllGroupKeys())
        {
            PoseTransform poseTransform = pose.get(key);
            BOBJBone group = this.armature.bones.get(key);

            poseTransform.copy(group.transform);
        }

        return pose;
    }

    @Override
    public void resetPose()
    {
        for (BOBJBone orderedBone : this.armature.orderedBones)
        {
            orderedBone.reset();
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
            BOBJBone bone = this.armature.bones.get(entry.getKey());

            if (bone == null)
            {
                continue;
            }

            if (transform.fix > 0F)
            {
                bone.transform.lerp(Transform.DEFAULT, transform.fix);
            }

            // TODO: bone.lighting = transform.lighting;
            // TODO: bone.color.copy(transform.color);
            bone.transform.translate.add(transform.translate);
            bone.transform.scale.add(transform.scale).sub(1, 1, 1);
            bone.transform.rotate.add(transform.rotate);
            bone.transform.rotate2.add(transform.rotate2);
        }
    }

    @Override
    public Set<String> getShapeKeys()
    {
        return Set.of();
    }

    @Override
    public String getAnchor()
    {
        for (BOBJBone orderedBone : this.armature.orderedBones)
        {
            if (orderedBone.parentBone != null)
            {
                return orderedBone.name;
            }
        }

        return "";
    }

    @Override
    public Collection<String> getAllGroupKeys()
    {
        return this.armature.bones.keySet();
    }

    @Override
    public Collection<String> getAllChildrenKeys(String key)
    {
        BOBJBone group = this.armature.bones.get(key);
        List<String> groups = new ArrayList<>();

        this.collectChildrenKeys(group, groups);

        return groups;
    }

    private void collectChildrenKeys(BOBJBone group, List<String> groups)
    {
        for (BOBJBone bone : this.armature.orderedBones)
        {
            if (bone.parentBone == group)
            {
                groups.add(bone.name);
                this.collectChildrenKeys(bone, groups);
            }
        }
    }

    @Override
    public Collection<ModelGroup> getAllGroups()
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<BOBJBone> getAllBOBJBones()
    {
        return Collections.unmodifiableList(this.armature.orderedBones);
    }

    @Override
    public Collection<String> getAdjacentGroups(String groupName)
    {
        List<String> groups = new ArrayList<>();

        for (BOBJBone orderedBone : this.armature.orderedBones)
        {
            if (orderedBone.parent.equals(groupName))
            {
                groups.add(orderedBone.name);
            }
        }

        return groups;
    }

    @Override
    public Collection<String> getHierarchyGroups(String groupName)
    {
        BOBJBone group = this.armature.bones.get(groupName);
        List<String> groups = new ArrayList<>();

        while (group != null)
        {
            groups.add(group.name);

            group = group.parentBone;
        }

        return groups;
    }

    @Override
    public void apply(IEntity target, Animation action, float tick, float blend, float transition, boolean skipInitial)
    {
        MolangHelper.setMolangVariables(action.parser, target, tick, transition);
        BOBJModelAnimator.animate(this, action, tick, blend, skipInitial);
    }

    @Override
    public void postApply(IEntity target, Animation action, float tick, float transition)
    {
        MolangHelper.setMolangVariables(action.parser, target, tick, transition);
        BOBJModelAnimator.postAnimate(this, action, tick);
    }
}