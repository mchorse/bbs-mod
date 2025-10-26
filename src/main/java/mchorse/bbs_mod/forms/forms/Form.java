package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormArchitect;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.forms.states.AnimationStates;
import mchorse.bbs_mod.forms.values.ValueAnchor;
import mchorse.bbs_mod.settings.values.IValueNotifier;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Form implements IMapSerializable, IValueNotifier
{
    private Form parent;

    public final ValueBoolean visible = new ValueBoolean("visible", true);
    public final ValueBoolean animatable = new ValueBoolean("animatable", true);
    public final ValueString trackName = new ValueString("track_name", "");
    public final ValueFloat lighting = new ValueFloat("lighting", 1F);
    public final ValueString name = new ValueString("name", "");
    public final ValueTransform transform = new ValueTransform("transform", new Transform());
    public final ValueTransform transformOverlay = new ValueTransform("transform_overlay", new Transform());
    public final ValueFloat uiScale = new ValueFloat("uiScale", 1F);
    public final ValueAnchor anchor = new ValueAnchor("anchor", new Anchor());
    public final ValueBoolean shaderShadow = new ValueBoolean("shaderShadow", true);

    public final List<ValueTransform> additionalTransforms = new ArrayList<>();

    /* Hitbox properties */
    public final ValueBoolean hitbox = new ValueBoolean("hitbox", false);
    public final ValueFloat hitboxWidth = new ValueFloat("hitboxWidth", 0.5F);
    public final ValueFloat hitboxHeight = new ValueFloat("hitboxHeight", 1.8F);
    public final ValueFloat hitboxSneakMultiplier = new ValueFloat("hitboxSneakMultiplier", 0.9F);
    public final ValueFloat hitboxEyeHeight = new ValueFloat("hitboxEyeHeight", 0.9F);

    /* Morphing properties */
    public final ValueFloat hp = new ValueFloat("hp", 20F);
    public final ValueFloat speed = new ValueFloat("movement_speed", 0.1F);
    public final ValueFloat stepHeight = new ValueFloat("step_height", 0.5F);

    public final ValueInt hotkey = new ValueInt("keybind", 0);

    public final BodyPartManager parts = new BodyPartManager(this);
    public final AnimationStates states = new AnimationStates("states");

    protected Object renderer;
    protected String cachedID;
    protected final Map<String, BaseValueBasic> properties = new LinkedHashMap<>();

    public Form()
    {
        this.animatable.invisible();
        this.trackName.invisible();
        this.name.invisible();
        this.uiScale.invisible();
        this.shaderShadow.invisible();

        this.register(this.visible);
        this.register(this.animatable);
        this.register(this.trackName);
        this.register(this.lighting);
        this.register(this.name);
        this.register(this.transform);
        this.register(this.transformOverlay);

        for (int i = 0; i < BBSSettings.recordingPoseTransformOverlays.get(); i++)
        {
            ValueTransform valueTransform = new ValueTransform("transform_overlay" + i, new Transform());

            this.additionalTransforms.add(valueTransform);
            this.register(valueTransform);
        }

        this.register(this.uiScale);
        this.register(this.anchor);
        this.register(this.shaderShadow);

        this.hitbox.invisible();
        this.hitboxWidth.invisible();
        this.hitboxHeight.invisible();
        this.hitboxSneakMultiplier.invisible();
        this.hitboxEyeHeight.invisible();

        this.register(this.hitbox);
        this.register(this.hitboxWidth);
        this.register(this.hitboxHeight);
        this.register(this.hitboxSneakMultiplier);
        this.register(this.hitboxEyeHeight);

        this.hp.invisible();
        this.speed.invisible();
        this.stepHeight.invisible();

        this.register(this.hp);
        this.register(this.speed);
        this.register(this.stepHeight);

        this.hotkey.invisible();

        this.register(this.hotkey);
    }

    public Object getRenderer()
    {
        return this.renderer;
    }

    public void setRenderer(Object renderer)
    {
        this.renderer = renderer;
    }

    protected void register(BaseValueBasic property)
    {
        if (this.properties.containsKey(property.getId()))
        {
            throw new IllegalStateException("Property " + property.getId() + " was already registered for form by ID " + this.getId() + "!");
        }

        this.properties.put(property.getId(), property);
        property.setParent(this);
    }

    public Map<String, BaseValueBasic> getProperties()
    {
        return Collections.unmodifiableMap(this.properties);
    }

    /**
     * Only body parts can set form's parent.
     */
    void setParent(Form parent)
    {
        this.parent = parent;
    }

    public Form getParent()
    {
        return this.parent;
    }

    /* Morphing */

    public void onMorph(LivingEntity entity)
    {
        float hp = this.hp.get();
        float speed = this.speed.get();
        float stepHeight = this.stepHeight.get();

        if (hp != 20F)
        {
            entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(hp);
            entity.setHealth(hp);
        }
        if (speed != 0.1F) entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        if (stepHeight != 0.5F) entity.setStepHeight(stepHeight);
    }

    public void onDemorph(LivingEntity entity)
    {
        entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(20F);
        entity.setHealth(20F);
        entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1F);
        entity.setStepHeight(0.5F);
    }

    /* ID and display name */

    public String getId()
    {
        if (this.cachedID == null)
        {
            this.cachedID = BBSMod.getForms().getType(this).toString();
        }

        return this.cachedID;
    }

    public String getIdOrName()
    {
        String name = this.name.get();

        return name.isEmpty() ? this.getId() : name;
    }

    public final String getDisplayName()
    {
        String name = this.name.get();

        if (!name.isEmpty())
        {
            return name;
        }

        return this.getDefaultDisplayName();
    }

    protected String getDefaultDisplayName()
    {
        return this.getId();
    }

    public String getTrackName(String property)
    {
        String s = this.trackName.get();

        if (!s.isEmpty())
        {
            if (property.isEmpty())
            {
                return s;
            }

            int slash = property.lastIndexOf('/');
            String last = slash == -1 ? property : property.substring(slash + 1);

            return s + (StringUtils.isInteger(last) ? "" : "/" + last);
        }

        return property;
    }

    /* Update */

    public void update(IEntity entity)
    {
        this.parts.update(entity);

        if (this.renderer instanceof ITickable)
        {
            ((ITickable) this.renderer).tick(entity);
        }
    }

    /* Data comparison and (de)serialization */

    public final Form copy()
    {
        FormArchitect forms = BBSMod.getForms();

        return forms.fromData(forms.toData(this));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof Form)
        {
            Form form = (Form) obj;

            if (!this.parts.equals(form.parts))
            {
                return false;
            }

            if (this.properties.size() != form.properties.size())
            {
                return false;
            }

            for (String key : this.properties.keySet())
            {
                if (!this.properties.get(key).equals(form.properties.get(key)))
                {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void preNotify(int flag)
    {}

    @Override
    public void postNotify(int flag)
    {}

    @Override
    public void toData(MapType data)
    {
        data.put("bodyParts", this.parts.toData());
        data.put("states", this.states.toData());

        for (BaseValue property : this.properties.values())
        {
            data.put(property.getId(), property.toData());
        }
    }

    @Override
    public void fromData(MapType data)
    {
        this.parts.fromData(data.getMap("bodyParts"));
        this.states.fromData(data.getMap("states"));

        for (BaseValue property : this.properties.values())
        {
            BaseType type = data.get(property.getId());

            if (type != null)
            {
                property.fromData(type);
            }
        }
    }
}