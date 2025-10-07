package mchorse.bbs_mod.entity;

import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.network.ServerNetwork;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActorEntity extends LivingEntity implements IEntityFormProvider
{
    public static DefaultAttributeContainer.Builder createActorAttributes()
    {
        return LivingEntity.createLivingAttributes()
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1D)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1D)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED)
            .add(EntityAttributes.GENERIC_LUCK);
    }

    private boolean despawn;
    private MCEntity entity = new MCEntity(this);
    private Form form;

    private Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
    private List<ItemStack> recordedInventory;
    private int xpToDrop;

    public ActorEntity(EntityType<? extends LivingEntity> entityType, World world)
    {
        super(entityType, world);
    }

    public MCEntity getEntity()
    {
        return this.entity;
    }

    @Override
    public int getEntityId()
    {
        return this.getId();
    }

    @Override
    public Form getForm()
    {
        return this.form;
    }

    @Override
    public void setForm(Form form)
    {
        Form lastForm = this.form;

        this.form = form;

        if (!this.getWorld().isClient())
        {
            if (lastForm != null) lastForm.onDemorph(this);
            if (form != null) form.onMorph(this);
        }
    }

    @Override
    public boolean shouldRender(double distance)
    {
        double d = this.getBoundingBox().getAverageSideLength();

        if (Double.isNaN(d))
        {
            d = 1D;
        }

        return distance < (d * 256D) * (d * 256D);
    }

    @Override
    public Iterable<ItemStack> getHandItems()
    {
        return List.of(this.getEquippedStack(EquipmentSlot.MAINHAND), this.getEquippedStack(EquipmentSlot.OFFHAND));
    }

    @Override
    public Iterable<ItemStack> getArmorItems()
    {
        return List.of(this.getEquippedStack(EquipmentSlot.FEET), this.getEquippedStack(EquipmentSlot.LEGS), this.getEquippedStack(EquipmentSlot.CHEST), this.getEquippedStack(EquipmentSlot.HEAD));
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot)
    {
        return this.equipment.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack)
    {
        this.equipment.put(slot, stack == null ? ItemStack.EMPTY : stack);
    }

    @Override
    public Arm getMainArm()
    {
        return Arm.RIGHT;
    }

    @Override
    public void tick()
    {
        super.tick();

        this.tickHandSwing();

        if (this.form != null)
        {
            this.form.update(this.entity);
        }

        if (this.getWorld().isClient)
        {
            return;
        }
    }

    @Override
    public void checkDespawn()
    {
        super.checkDespawn();

        if (this.despawn)
        {
            this.discard();
        }
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player)
    {
        super.onStartedTrackingBy(player);

        ServerNetwork.sendEntityForm(player, this);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt)
    {
        super.readCustomDataFromNbt(nbt);

        this.despawn = nbt.getBoolean("despawn");
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt)
    {
        super.writeCustomDataToNbt(nbt);

        nbt.putBoolean("despawn", true);
    }

    /**
     * Allow Action playback to provide recorded inventory and optional XP to drop.
     */
    public void setRecordedInventory(List<ItemStack> inventory)
    {
        this.recordedInventory = inventory;
    }

    public void setXpToDrop(int xp)
    {
        this.xpToDrop = Math.max(0, xp);
    }

    @Override
    public void onDeath(DamageSource damageSource)
    {
        super.onDeath(damageSource);

        if (this.getWorld().isClient())
        {
            return;
        }

        // Drop equipped items
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            ItemStack stack = this.getEquippedStack(slot);
            if (stack != null && !stack.isEmpty())
            {
                this.dropStack(stack.copy());
                this.equipStack(slot, ItemStack.EMPTY);
            }
        }

        // Drop recorded inventory, if any
        if (this.recordedInventory != null && !this.recordedInventory.isEmpty())
        {
            for (ItemStack stack : this.recordedInventory)
            {
                if (stack != null && !stack.isEmpty())
                {
                    this.dropStack(stack.copy());
                }
            }
            this.recordedInventory = null;
        }

        // Drop some XP if specified
        if (this.xpToDrop > 0)
        {
            ExperienceOrbEntity.spawn((ServerWorld) this.getWorld(), this.getPos(), this.xpToDrop);
            this.xpToDrop = 0;
        }
    }
}