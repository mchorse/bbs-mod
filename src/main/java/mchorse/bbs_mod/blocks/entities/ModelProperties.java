package mchorse.bbs_mod.blocks.entities;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.render.model.json.ModelTransformationMode;

public class ModelProperties implements IMapSerializable
{
    private Form form;
    private Form formThirdPerson;
    private Form formInventory;
    private Form formFirstPerson;

    private final Transform transform = new Transform();
    private final Transform transformThirdPerson = new Transform();
    private final Transform transformInventory = new Transform();
    private final Transform transformFirstPerson = new Transform();

    private boolean enabled = true;
    private boolean global;
    private boolean shadow;
    private boolean lookAt; // Face the player/camera when rendering

    /* Runtime-only state for continuous look-at yaw unwrapping (not serialized) */
    private boolean lookYawInitialized;
    private float lookYawLastAbs;
    private float lookYawContinuous;

    public Form getForm()
    {
        return this.form;
    }

    private Form processForm(Form form)
    {
        if (form != null)
        {
            form.playMain();
        }

        return form;
    }

    public void setForm(Form form)
    {
        this.form = this.processForm(form);
    }

    public Form getFormThirdPerson()
    {
        return this.formThirdPerson;
    }

    public void setFormThirdPerson(Form form)
    {
        this.formThirdPerson = this.processForm(form);
    }

    public Form getFormInventory()
    {
        return this.formInventory;
    }

    public void setFormInventory(Form form)
    {
        this.formInventory = this.processForm(form);
    }

    public Form getFormFirstPerson()
    {
        return this.formFirstPerson;
    }

    public void setFormFirstPerson(Form form)
    {
        this.formFirstPerson = this.processForm(form);
    }

    public Transform getTransform()
    {
        return this.transform;
    }

    public Transform getTransformThirdPerson()
    {
        return this.transformThirdPerson;
    }

    public Transform getTransformInventory()
    {
        return this.transformInventory;
    }

    public Transform getTransformFirstPerson()
    {
        return this.transformFirstPerson;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isGlobal()
    {
        return this.global;
    }

    public void setGlobal(boolean global)
    {
        this.global = global;
    }

    public boolean isShadow()
    {
        return this.shadow;
    }

    public void setShadow(boolean shadow)
    {
        this.shadow = shadow;
    }

    public boolean isLookAt()
    {
        return this.lookAt;
    }

    public void setLookAt(boolean lookAt)
    {
        this.lookAt = lookAt;
    }

    /* Runtime helpers */
    public boolean isLookYawInitialized()
    {
        return this.lookYawInitialized;
    }

    public void initLookYaw(float yawAbs)
    {
        this.lookYawInitialized = true;
        this.lookYawLastAbs = yawAbs;
        this.lookYawContinuous = yawAbs;
    }

    public float updateLookYawContinuous(float yawAbs)
    {
        if (!this.lookYawInitialized)
        {
            initLookYaw(yawAbs);
            return this.lookYawContinuous;
        }

        float d = yawAbs - this.lookYawLastAbs;
        while (d > Math.PI) d -= (float) (Math.PI * 2);
        while (d < -Math.PI) d += (float) (Math.PI * 2);

        this.lookYawContinuous += d;
        this.lookYawLastAbs = yawAbs;

        return this.lookYawContinuous;
    }

    public void resetLookYaw()
    {
        this.lookYawInitialized = false;
        this.lookYawLastAbs = 0F;
        this.lookYawContinuous = 0F;
    }

    /**
     * Rebasea el yaw continuo al yaw base actual, evitando saltos tras un ciclo.
     * Mantiene la inicialización y actualiza el último yaw absoluto.
     */
    public void snapLookYawToBase(float yawAbs, float baseYaw)
    {
        this.lookYawInitialized = true;
        this.lookYawLastAbs = yawAbs;
        this.lookYawContinuous = baseYaw;
    }

    public Form getForm(ModelTransformationMode mode)
    {
        Form form = this.form;

        if (mode == ModelTransformationMode.GUI && this.formInventory != null)
        {
            form = this.formInventory;
        }
        else if ((mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND) && this.formThirdPerson != null)
        {
            form = this.formThirdPerson;
        }
        else if ((mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND || mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND) && this.formFirstPerson != null)
        {
            form = this.formFirstPerson;
        }

        return form;
    }

    public Transform getTransform(ModelTransformationMode mode)
    {
        Transform transform = this.transformThirdPerson;

        if (mode == ModelTransformationMode.GUI)
        {
            transform = this.transformInventory;
        }
        else if (mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND || mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND)
        {
            transform = this.transformFirstPerson;
        }
        else if (mode == ModelTransformationMode.GROUND)
        {
            transform = this.transform;
        }

        return transform;
    }

    @Override
    public void fromData(MapType data)
    {
        this.form = this.processForm(FormUtils.fromData(data.getMap("form")));
        this.formThirdPerson = this.processForm(FormUtils.fromData(data.getMap("formThirdPerson")));
        this.formInventory = this.processForm(FormUtils.fromData(data.getMap("formInventory")));
        this.formFirstPerson = this.processForm(FormUtils.fromData(data.getMap("formFirstPerson")));

        this.transform.fromData(data.getMap("transform"));
        this.transformThirdPerson.fromData(data.getMap("transformThirdPerson"));
        this.transformInventory.fromData(data.getMap("transformInventory"));
        this.transformFirstPerson.fromData(data.getMap("transformFirstPerson"));

        if (data.has("enabled")) this.enabled = data.getBool("enabled");
        this.shadow = data.getBool("shadow");
        this.global = data.getBool("global");
        this.lookAt = data.getBool("look_at");
    }

    @Override
    public void toData(MapType data)
    {
        data.put("form", FormUtils.toData(this.form));
        data.put("formThirdPerson", FormUtils.toData(this.formThirdPerson));
        data.put("formInventory", FormUtils.toData(this.formInventory));
        data.put("formFirstPerson", FormUtils.toData(this.formFirstPerson));

        data.put("transform", this.transform.toData());
        data.put("transformThirdPerson", this.transformThirdPerson.toData());
        data.put("transformInventory", this.transformInventory.toData());
        data.put("transformFirstPerson", this.transformFirstPerson.toData());

        data.putBool("enabled", this.enabled);
        data.putBool("shadow", this.shadow);
        data.putBool("global", this.global);
        data.putBool("look_at", this.lookAt);
    }

    public void update(IEntity entity)
    {
        if (this.form != null)
        {
            this.form.update(entity);
        }

        if (this.formThirdPerson != null)
        {
            this.formThirdPerson.update(entity);
        }

        if (this.formInventory != null)
        {
            this.formInventory.update(entity);
        }

        if (this.formFirstPerson != null)
        {
            this.formFirstPerson.update(entity);
        }
    }
}
