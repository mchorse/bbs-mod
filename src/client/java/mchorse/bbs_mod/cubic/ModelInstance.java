package mchorse.bbs_mod.cubic;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.cubic.render.CubicCubeRenderer;
import mchorse.bbs_mod.cubic.render.CubicMatrixRenderer;
import mchorse.bbs_mod.cubic.render.CubicRenderer;
import mchorse.bbs_mod.cubic.render.CubicVAOBuilderRenderer;
import mchorse.bbs_mod.cubic.render.CubicVAORenderer;
import mchorse.bbs_mod.cubic.render.vao.BOBJModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.resources.LinkUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModelInstance implements IModelInstance
{
    public final String id;
    public IModel model;
    public Animations animations;
    public Link texture;

    /* Model's additional properties */
    public String poseGroup;
    public boolean procedural;
    public boolean culling = true;
    public boolean onCpu;
    public String anchorGroup = "";

    /* Look-at configuration (per model, from config.json) */
    public boolean lookAtConfigured = false;
    public String lookAtHeadBone = "head";
    public String lookAtAnchorBone = "anchor";
    public boolean lookAtAllowPitch = true;
    public float lookAtHeadLimitDeg = 45F;

    public Vector3f scale = new Vector3f(1F);
    public float uiScale = 1F;
    public Pose sneakingPose = new Pose();

    public List<ArmorSlot> itemsMain = new ArrayList<>();
    public List<ArmorSlot> itemsOff = new ArrayList<>();
    public Map<String, String> flippedParts = new HashMap<>();
    public Map<ArmorType, ArmorSlot> armorSlots = new HashMap<>();

    public ArmorSlot fpMain;
    public ArmorSlot fpOffhand;

    private Map<ModelGroup, ModelVAO> vaos = new HashMap<>();

    public ModelInstance(String id, IModel model, Animations animations, Link texture)
    {
        this.id = id;
        this.model = model;
        this.animations = animations;
        this.texture = texture;

        this.poseGroup = id;
    }

    @Override
    public IModel getModel()
    {
        return this.model;
    }

    @Override
    public Pose getSneakingPose()
    {
        return this.sneakingPose;
    }

    @Override
    public Animations getAnimations()
    {
        return this.animations;
    }

    public Map<ModelGroup, ModelVAO> getVaos()
    {
        return this.vaos;
    }

    public String getAnchor()
    {
        String anchor = this.model.getAnchor();

        if (this.anchorGroup.isEmpty() && !anchor.isEmpty())
        {
            return anchor;
        }

        return this.anchorGroup;
    }

    public void applyConfig(MapType config)
    {
        if (config == null)
        {
            return;
        }

        this.procedural = config.getBool("procedural", this.procedural);
        this.culling = config.getBool("culling", this.culling);
        this.onCpu = config.getBool("on_cpu", this.onCpu);
        this.poseGroup = config.getString("pose_group", this.poseGroup);

        if (config.has("texture"))
        {
            this.texture = LinkUtils.create(config.get("texture"));
        }
        if (config.has("items_main"))
        {
            ListType list = config.get("items_main").asList();

            for (BaseType type : list)
            {
                ArmorSlot slot = new ArmorSlot();

                slot.fromData(type);
                this.itemsMain.add(slot);
            }
        }
        if (config.has("items_off"))
        {
            ListType list = config.get("items_off").asList();

            for (BaseType type : list)
            {
                ArmorSlot slot = new ArmorSlot();

                slot.fromData(type);
                this.itemsOff.add(slot);
            }
        }
        if (config.has("ui_scale")) this.uiScale = config.getFloat("ui_scale");
        if (config.has("scale")) this.scale = DataStorageUtils.vector3fFromData(config.getList("scale"), new Vector3f(1F));
        if (config.has("sneaking_pose", BaseType.TYPE_MAP))
        {
            this.sneakingPose = new Pose();
            this.sneakingPose.fromData(config.getMap("sneaking_pose"));
        }
        if (config.has("anchor")) this.anchorGroup = config.getString("anchor");
        if (config.has("flipped_parts"))
        {
            MapType map = config.getMap("flipped_parts");

            for (String key : map.keys())
            {
                String string = map.getString(key);

                if (!string.trim().isEmpty())
                {
                    this.flippedParts.put(key, string);
                }
            }
        }
        if (config.has("armor_slots"))
        {
            MapType map = config.getMap("armor_slots");

            for (String key : map.keys())
            {
                try
                {
                    ArmorType type = ArmorType.valueOf(key.toUpperCase());
                    ArmorSlot slot = new ArmorSlot();

                    slot.fromData(map.getMap(key));
                    this.armorSlots.put(type, slot);
                }
                catch (Exception e)
                {}
            }
        }
        if (config.has("fp_main"))
        {
            this.fpMain = new ArmorSlot();
            this.fpMain.fromData(config.get("fp_main"));
        }
        if (config.has("fp_offhand"))
        {
            this.fpOffhand = new ArmorSlot();
            this.fpOffhand.fromData(config.get("fp_offhand"));
        }

        /* Optional look-at configuration */
        if (config.has("look_at", BaseType.TYPE_MAP))
        {
            this.lookAtConfigured = true;
            MapType lookAt = config.getMap("look_at");

            if (lookAt.has("head")) this.lookAtHeadBone = lookAt.getString("head", this.lookAtHeadBone);
            if (lookAt.has("anchor")) this.lookAtAnchorBone = lookAt.getString("anchor", this.lookAtAnchorBone);
            if (lookAt.has("pitch")) this.lookAtAllowPitch = lookAt.getBool("pitch", this.lookAtAllowPitch);
            if (lookAt.has("head_limit")) this.lookAtHeadLimitDeg = lookAt.getFloat("head_limit", this.lookAtHeadLimitDeg);
        }
    }

    public void setup()
    {
        if (this.model instanceof BOBJModel model)
        {
            MinecraftClient.getInstance().execute(model::setup);
        }

        /* VAOs should be only generated if there are no shape keys */
        if (!this.model.getShapeKeys().isEmpty())
        {
            return;
        }

        if (this.model instanceof Model model && !this.onCpu)
        {
            MinecraftClient.getInstance().execute(() ->
            {
                CubicRenderer.processRenderModel(new CubicVAOBuilderRenderer(this.vaos), null, new MatrixStack(), model);
            });
        }
    }

    public boolean isVAORendered()
    {
        return !this.vaos.isEmpty() || this.model instanceof BOBJModel;
    }

    public void delete()
    {
        for (ModelVAO value : this.vaos.values())
        {
            value.delete();
        }

        this.vaos.clear();
    }

    /* Rendering */

    public void fillStencilMap(StencilMap stencilMap, ModelForm form)
    {
        if (this.model instanceof Model model)
        {
            for (ModelGroup group : model.getOrderedGroups())
            {
                stencilMap.addPicking(form, group.id);
            }
        }
        else if (this.model instanceof BOBJModel model)
        {
            for (BOBJBone orderedBone : model.getArmature().orderedBones)
            {
                stencilMap.addPicking(form, orderedBone.name);
            }
        }
    }

    public void captureMatrices(Map<String, Matrix4f> bones, String target)
    {
        if (this.model instanceof Model model)
        {
            MatrixStack stack = new MatrixStack();
            CubicMatrixRenderer renderer = new CubicMatrixRenderer(model, target);

            CubicRenderer.processRenderModel(renderer, null, stack, model);

            for (ModelGroup group : model.getAllGroups())
            {
                Matrix4f matrix = new Matrix4f(renderer.matrices.get(group.index));

                matrix.translate(
                    group.initial.translate.x / 16,
                    group.initial.translate.y / 16,
                    group.initial.translate.z / 16
                );
                matrix.rotateY(MathUtils.PI);
                bones.put(group.id, matrix);

                /* Also provide origin matrix captured before rotation/scale at the group's pivot */
                Matrix4f origin = new Matrix4f(renderer.origins.get(group.index));
                origin.translate(
                    group.initial.translate.x / 8192,
                    group.initial.translate.y / 8192,
                    group.initial.translate.z / 8192
                );
                origin.rotateY(MathUtils.PI);
                bones.put(group.id + "#origin", origin);
            }
        }
        else if (this.model instanceof BOBJModel model)
        {
            model.getArmature().setupMatrices();

            for (BOBJBone orderedBone : model.getArmature().orderedBones)
            {
                Matrix4f value = new Matrix4f();

                value.rotateY(MathUtils.PI).mul(orderedBone.mat);
                bones.put(orderedBone.name, value);

                /* Origin matrix for BOBJ bones: parent transform + translation (no bone rotation/scale) */
                Matrix4f origin = new Matrix4f();
                origin.rotateY(MathUtils.PI);

                if (orderedBone.parentBone != null)
                {
                    origin.mul(new Matrix4f(orderedBone.parentBone.mat));
                }

                origin.translate(orderedBone.transform.translate);
                bones.put(orderedBone.name + "#origin", origin);
            }
        }
    }

    public void render(MatrixStack stack, Supplier<ShaderProgram> program, Color color, int light, int overlay, StencilMap stencilMap, ShapeKeys keys, Link defaultTexture)
    {
        if (this.model instanceof Model model)
        {
            boolean isVao = this.isVAORendered();
            CubicCubeRenderer renderProcessor = isVao
                ? new CubicVAORenderer(program.get(), this, light, overlay, stencilMap, keys, defaultTexture)
                : new CubicCubeRenderer(light, overlay, stencilMap, keys);

            renderProcessor.setColor(color.r, color.g, color.b, color.a);

            if (isVao)
            {
                CubicRenderer.processRenderModel(renderProcessor, null, stack, model);
            }
            else
            {
                RenderSystem.setShader(program);

                BufferBuilder builder = Tessellator.getInstance().getBuffer();

                builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
                CubicRenderer.processRenderModel(renderProcessor, builder, stack, model);
                BufferRenderer.drawWithGlobalProgram(builder.end());
            }
        }
        else if (this.model instanceof BOBJModel model)
        {
            BOBJModelVAO vao = model.getVao();

            if (vao != null)
            {
                stack.push();
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));

                vao.armature.setupMatrices();
                vao.updateMesh(stencilMap);
                vao.render(program.get(), stack, color.r, color.g, color.b, color.a, stencilMap, light, overlay);

                stack.pop();
            }
        }
    }
}
