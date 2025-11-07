package mchorse.bbs_mod.items;

import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.VanillaParticleForm;
import mchorse.bbs_mod.forms.forms.utils.ParticleSettings;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class GunProperties extends ModelProperties
{
    /* Gun properties */
    public boolean launch;
    public float launchPower;
    public boolean launchAdditive;
    public float scatterX;
    public float scatterY;
    public int projectiles;

    /* Projectile properties */
    public Form projectileForm;
    public final Transform projectileTransform = new Transform();
    public boolean useTarget;
    public int lifeSpan = 100;
    public float speed = 1F;
    public float friction = 0.99F;
    public float gravity = 0.05F;
    public boolean yaw = true;
    public boolean pitch = true;
    public int fadeIn;
    public int fadeOut;

    /* Impact properties */
    public Form impactForm;
    public int bounces;
    public float bounceDamping = 0.5F;
    public boolean vanish = true;
    public float damage;
    public float knockback;
    public boolean collideBlocks = true;
    public boolean collideEntities = true;

    /* Zoom */
    public Form zoomForm;
    public final Transform zoomTransform = new Transform();
    public String cmdZoomOn = "";
    public String cmdZoomOff = "";
    public Interpolation fovInterp = new Interpolation("interp", Interpolations.MAP);
    public int fovDuration = 10;
    public float fovTarget = 40F;

    /* Commands */
    public String cmdFiring = "";
    public String cmdImpact = "";
    public String cmdVanish = "";
    public String cmdTicking = "";
    public int ticking;

    public static GunProperties get(ItemStack stack)
    {
        NbtCompound nbt = stack.getNbt();
        GunProperties properties = new GunProperties();

        if (nbt == null)
        {
            setupDefault(properties);

            return properties;
        }

        BaseType data = DataStorageUtils.readFromNbtCompound(nbt, "GunData");

        if (data != null && data.isMap())
        {
            properties.fromData(data.asMap());
        }
        else
        {
            setupDefault(properties);
        }

        return properties;
    }

    private static void setupDefault(GunProperties properties)
    {
        ExtrudedForm form = new ExtrudedForm();
        VanillaParticleForm projectileForm = new VanillaParticleForm();
        ParticleSettings value = new ParticleSettings();

        Transform tp = properties.getTransformThirdPerson();
        Transform fp = properties.getTransformFirstPerson();

        value.particle = new Identifier("minecraft:falling_water");
        projectileForm.settings.set(value);
        projectileForm.frequency.set(1);
        projectileForm.offsetX.set(0.1F);
        projectileForm.offsetY.set(0.1F);
        projectileForm.offsetZ.set(0.1F);

        properties.useTarget = true;
        properties.projectileForm = projectileForm;

        form.transform.get().translate.set(0F, 0.5F, 0F);
        form.texture.set(Link.assets("textures/gun.png"));
        properties.setForm(form);

        fp.translate.set(0.25F, 0.125F, -0.25F);
        fp.rotate.y = -MathUtils.PI / 2;
        fp.rotate2.z = MathUtils.PI / 4;

        tp.translate.y = 0.375F;
        tp.translate.z = 0.125F;
        tp.scale.set(0.666F);
        tp.rotate.y = -MathUtils.PI / 2;
        tp.rotate2.z = MathUtils.PI / 4;
    }

    public void fromNetwork(PacketByteBuf buf)
    {
        BaseType type = DataStorageUtils.readFromPacket(buf);

        this.projectileTransform.fromData(type != null && type.isMap() ? type.asMap() : new MapType());
        this.useTarget = buf.readBoolean();
        this.lifeSpan = buf.readInt();
        this.speed = buf.readFloat();
        this.friction = buf.readFloat();
        this.gravity = buf.readFloat();
        this.yaw = buf.readBoolean();
        this.pitch = buf.readBoolean();
        this.fadeIn = buf.readInt();
        this.fadeOut = buf.readInt();

        this.bounces = buf.readInt();
        this.bounceDamping = buf.readFloat();
        this.vanish = buf.readBoolean();
        this.damage = buf.readFloat();
        this.knockback = buf.readFloat();
        this.collideBlocks = buf.readBoolean();
        this.collideEntities = buf.readBoolean();
    }

    public void toNetwork(PacketByteBuf buf)
    {
        DataStorageUtils.writeToPacket(buf, this.projectileTransform.toData());
        buf.writeBoolean(this.useTarget);
        buf.writeInt(this.lifeSpan);
        buf.writeFloat(this.speed);
        buf.writeFloat(this.friction);
        buf.writeFloat(this.gravity);
        buf.writeBoolean(this.yaw);
        buf.writeBoolean(this.pitch);
        buf.writeInt(this.fadeIn);
        buf.writeInt(this.fadeOut);

        buf.writeInt(this.bounces);
        buf.writeFloat(this.bounceDamping);
        buf.writeBoolean(this.vanish);
        buf.writeFloat(this.damage);
        buf.writeFloat(this.knockback);
        buf.writeBoolean(this.collideBlocks);
        buf.writeBoolean(this.collideEntities);
    }

    @Override
    public void update(IEntity entity)
    {
        super.update(entity);

        if (this.zoomForm != null)
        {
            this.zoomForm.update(entity);
        }
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);

        this.launch = data.getBool("launch");
        this.launchPower = data.getFloat("launchPower");
        this.launchAdditive = data.getBool("launchAdditive");
        this.scatterX = data.getFloat("scatterX");
        this.scatterY = data.getFloat("scatterY");
        this.projectiles = data.getInt("projectiles");

        this.projectileForm = FormUtils.fromData(data.get("projectileForm"));
        this.projectileTransform.fromData(data.getMap("projectileTransform"));
        this.useTarget = data.getBool("useTarget");
        this.lifeSpan = data.getInt("lifeSpan");
        this.speed = data.getFloat("speed", 1F);
        this.friction = data.getFloat("friction", 0.99F);
        this.gravity = data.getFloat("gravity", 0.05F);
        this.yaw = data.getBool("yaw", true);
        this.pitch = data.getBool("pitch", true);
        this.fadeIn = data.getInt("fadeIn");
        this.fadeOut = data.getInt("fadeOut");

        this.impactForm = FormUtils.fromData(data.get("impactForm"));
        this.bounces = data.getInt("bounces");
        this.bounceDamping = data.getFloat("bounceDamping", 0.5F);
        this.vanish = data.getBool("vanish", true);
        this.damage = data.getFloat("damage");
        this.knockback = data.getFloat("knockback");
        this.collideBlocks = data.getBool("collideBlocks", true);
        this.collideEntities = data.getBool("collideEntities", true);

        this.zoomForm = FormUtils.fromData(data.get("zoomForm"));
        this.zoomTransform.fromData(data.getMap("zoomTransform"));
        this.cmdZoomOn = data.getString("cmdZoomOn");
        this.cmdZoomOff = data.getString("cmdZoomOff");
        this.fovInterp.fromData(data.get("fovInterp"));
        this.fovDuration = data.getInt("fovDuration");
        this.fovTarget = data.getFloat("fovTarget");

        this.cmdFiring = data.getString("cmdFiring");
        this.cmdImpact = data.getString("cmdImpact");
        this.cmdVanish = data.getString("cmdVanish");
        this.cmdTicking = data.getString("cmdTicking");
        this.ticking = data.getInt("ticking");
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);

        data.putBool("launch", this.launch);
        data.putFloat("launchPower", this.launchPower);
        data.putBool("launchAdditive", this.launchAdditive);
        data.putFloat("scatterX", this.scatterX);
        data.putFloat("scatterY", this.scatterY);
        data.putInt("projectiles", this.projectiles);

        if (this.projectileForm != null) data.put("projectileForm", FormUtils.toData(this.projectileForm));
        data.put("projectileTransform", this.projectileTransform.toData());
        data.putBool("useTarget", this.useTarget);
        data.putInt("lifeSpan", this.lifeSpan);
        data.putFloat("speed", this.speed);
        data.putFloat("friction", this.friction);
        data.putFloat("gravity", this.gravity);
        data.putBool("yaw", this.yaw);
        data.putBool("pitch", this.pitch);
        data.putInt("fadeIn", this.fadeIn);
        data.putInt("fadeOut", this.fadeOut);

        if (this.impactForm != null) data.put("impactForm", FormUtils.toData(this.impactForm));
        data.putInt("bounces", this.bounces);
        data.putFloat("bounceDamping", this.bounceDamping);
        data.putBool("vanish", this.vanish);
        data.putFloat("damage", this.damage);
        data.putFloat("knockback", this.knockback);
        data.putBool("collideBlocks", this.collideBlocks);
        data.putBool("collideEntities", this.collideEntities);

        if (this.zoomForm != null) data.put("zoomForm", FormUtils.toData(this.zoomForm));
        data.put("zoomTransform", this.zoomTransform.toData());
        data.putString("cmdZoomOn", this.cmdZoomOn);
        data.putString("cmdZoomOff", this.cmdZoomOff);
        data.put("fovInterp", this.fovInterp.toData());
        data.putInt("fovDuration", this.fovDuration);
        data.putFloat("fovTarget", this.fovTarget);

        data.putString("cmdFiring", this.cmdFiring);
        data.putString("cmdImpact", this.cmdImpact);
        data.putString("cmdVanish", this.cmdVanish);
        data.putString("cmdTicking", this.cmdTicking);
        data.putInt("ticking", this.ticking);
    }
}