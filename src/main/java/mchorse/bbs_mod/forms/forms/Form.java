package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormArchitect;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.properties.AnchorProperty;
import mchorse.bbs_mod.forms.properties.BooleanProperty;
import mchorse.bbs_mod.forms.properties.FloatProperty;
import mchorse.bbs_mod.forms.properties.IFormProperty;
import mchorse.bbs_mod.forms.properties.IntegerProperty;
import mchorse.bbs_mod.forms.properties.StringProperty;
import mchorse.bbs_mod.forms.properties.TransformProperty;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Form implements IMapSerializable
{
    private Form parent;

    public final BooleanProperty visible = new BooleanProperty(this, "visible", true);
    public final BooleanProperty animatable = new BooleanProperty(this, "animatable", true);
    public final FloatProperty lighting = new FloatProperty(this, "lighting", 1F);
    public final StringProperty name = new StringProperty(this, "name", "");
    public final TransformProperty transform = new TransformProperty(this, "transform", new Transform());
    public final TransformProperty transformOverlay = new TransformProperty(this, "transform_overlay", new Transform());
    public final FloatProperty uiScale = new FloatProperty(this, "uiScale", 1F);
    public final BodyPartManager parts = new BodyPartManager(this);
    public final AnchorProperty anchor = new AnchorProperty(this, "anchor");
    public final BooleanProperty shaderShadow = new BooleanProperty(this, "shaderShadow", true);

    /* Hitbox properties */
    public final BooleanProperty hitbox = new BooleanProperty(this, "hitbox", false);
    public final FloatProperty hitboxWidth = new FloatProperty(this, "hitboxWidth", 0.5F);
    public final FloatProperty hitboxHeight = new FloatProperty(this, "hitboxHeight", 1.8F);
    public final FloatProperty hitboxSneakMultiplier = new FloatProperty(this, "hitboxSneakMultiplier", 0.9F);
    public final FloatProperty hitboxEyeHeight = new FloatProperty(this, "hitboxEyeHeight", 0.9F);

    /* Morphing properties */
    public final FloatProperty hp = new FloatProperty(this, "hp", 20F);
    public final FloatProperty speed = new FloatProperty(this, "movement_speed", 0.1F);
    public final FloatProperty stepHeight = new FloatProperty(this, "step_height", 0.5F);

    public final IntegerProperty hotkey = new IntegerProperty(this, "keybind", 0);

    protected Object renderer;
    protected String cachedID;
    protected final Map<String, IFormProperty> properties = new LinkedHashMap<>();

    public Form()
    {
        this.animatable.cantAnimate();
        this.name.cantAnimate();
        this.uiScale.cantAnimate();
        this.shaderShadow.cantAnimate();

        this.register(this.visible);
        this.register(this.animatable);
        this.register(this.lighting);
        this.register(this.name);
        this.register(this.transform);
        this.register(this.transformOverlay);
        this.register(this.uiScale);
        this.register(this.anchor);
        this.register(this.shaderShadow);

        this.hitbox.cantAnimate();
        this.hitboxWidth.cantAnimate();
        this.hitboxHeight.cantAnimate();
        this.hitboxSneakMultiplier.cantAnimate();
        this.hitboxEyeHeight.cantAnimate();

        this.register(this.hitbox);
        this.register(this.hitboxWidth);
        this.register(this.hitboxHeight);
        this.register(this.hitboxSneakMultiplier);
        this.register(this.hitboxEyeHeight);

        this.hp.cantAnimate();
        this.speed.cantAnimate();
        this.stepHeight.cantAnimate();

        this.register(this.hp);
        this.register(this.speed);
        this.register(this.stepHeight);

        this.hotkey.cantAnimate();

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

    protected void register(IFormProperty property)
    {
        if (this.properties.containsKey(property.getKey()))
        {
            throw new IllegalStateException("Property " + property.getKey() + " was already registered for form by ID " + this.getId() + "!");
        }

        this.properties.put(property.getKey(), property);
    }

    public Map<String, IFormProperty> getProperties()
    {
        return Collections.unmodifiableMap(properties);
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

    /* Update */

    public void update(IEntity entity)
    {
        this.parts.update(entity);

        for (IFormProperty property : this.properties.values())
        {
            property.update();
        }

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
    public void toData(MapType data)
    {
        data.put("bodyParts", this.parts.toData());

        for (IFormProperty property : this.properties.values())
        {
            data.put(property.getKey(), property.toData());
        }
    }

    @Override
    public void fromData(MapType data)
    {
        this.parts.fromData(data.getMap("bodyParts"));

        for (IFormProperty property : this.properties.values())
        {
            BaseType type = data.get(property.getKey());

            if (type != null)
            {
                property.fromData(type);
            }
        }
    }
}