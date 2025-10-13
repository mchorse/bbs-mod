package mchorse.bbs_mod.forms.entities;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.AABB;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LimbAnimator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Interface that provides access to an "Entity" within forms for rendering
 * and updating.
 */
public interface IEntity
{
    public void setWorld(World world);

    public World getWorld();

    public Form getForm();

    public void setForm(Form form);

    public ItemStack getEquipmentStack(EquipmentSlot slot);

    public void setEquipmentStack(EquipmentSlot slot, ItemStack stack);

    public int getSelectedSlot();

    public boolean isSneaking();

    public void setSneaking(boolean sneaking);

    public boolean isSprinting();

    public void setSprinting(boolean sprinting);

    public boolean isOnGround();

    public void setOnGround(boolean ground);

    public void swingArm();

    public float getHandSwingProgress(float tickDelta);

    public int getAge();

    public void setAge(int ticks);

    public float getFallDistance();

    public void setFallDistance(float fallDistance);

    public int getHurtTimer();

    public void setHurtTimer(int hurtTimer);

    public double getX();

    public double getPrevX();

    public void setPrevX(double x);

    public double getY();

    public double getPrevY();

    public void setPrevY(double y);

    public double getZ();

    public double getPrevZ();

    public void setPrevZ(double z);

    public void setPosition(double x, double y, double z);

    public double getEyeHeight();

    public Vec3d getVelocity();

    public void setVelocity(float x, float y, float z);

    public float getYaw();

    public float getPrevYaw();

    public void setYaw(float yaw);

    public void setPrevYaw(float prevYaw);

    public float getHeadYaw();

    public float getPrevHeadYaw();

    public void setHeadYaw(float headYaw);

    public void setPrevHeadYaw(float prevHeadYaw);

    public float getPitch();

    public float getPrevPitch();

    public void setPitch(float pitch);

    public void setPrevPitch(float prevPitch);

    public float getBodyYaw();

    public float getPrevBodyYaw();

    public float getPrevPrevBodyYaw();

    public void setBodyYaw(float bodyYaw);

    public void setPrevBodyYaw(float prevBodyYaw);

    public void setPrevPrevBodyYaw(float prevPrevBodyYaw);

    public float[] getExtraVariables();

    public float[] getPrevExtraVariables();

    public AABB getPickingHitbox();

    public void update();

    public default void copy(IEntity entity)
    {
        this.setForm(entity.getForm());

        this.setSneaking(entity.isSneaking());
        this.setSprinting(entity.isSprinting());
        this.setOnGround(entity.isOnGround());
        this.setFallDistance(entity.getFallDistance());
        this.setHurtTimer(entity.getHurtTimer());

        this.setPrevX(entity.getPrevX());
        this.setPrevY(entity.getPrevY());
        this.setPrevZ(entity.getPrevZ());
        this.setPosition(entity.getX(), entity.getY(), entity.getZ());

        this.setPrevYaw(entity.getPrevYaw());
        this.setPrevHeadYaw(entity.getPrevHeadYaw());
        this.setPrevPitch(entity.getPrevPitch());
        this.setPrevBodyYaw(entity.getPrevBodyYaw());
        this.setPrevPrevBodyYaw(entity.getPrevPrevBodyYaw());

        this.setYaw(entity.getYaw());
        this.setHeadYaw(entity.getHeadYaw());
        this.setPitch(entity.getPitch());
        this.setBodyYaw(entity.getBodyYaw());

        this.setVelocity((float) entity.getVelocity().x, (float) entity.getVelocity().y, (float) entity.getVelocity().z);

        float[] extraVariables = this.getExtraVariables();
        float[] prevExtraVariables = this.getPrevExtraVariables();

        for (int i = 0; i < extraVariables.length; i++)
        {
            extraVariables[i] = entity.getExtraVariables()[i];
            prevExtraVariables[i] = entity.getPrevExtraVariables()[i];
        }
    }

    public LimbAnimator getLimbAnimator();

    public float getLimbPos(float tickDelta);

    public float getLimbSpeed(float tickDelta);

    /* Swimming */

    public float getLeaningPitch(float tickDelta);

    public boolean isTouchingWater();

    public EntityPose getEntityPose();

    public int getRoll();

    public boolean isFallFlying();

    public Vec3d getRotationVec(float transition);

    public Vec3d lerpVelocity(float transition);

    public boolean isUsingRiptide();
}