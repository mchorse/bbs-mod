package mchorse.bbs_mod.entity;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.utils.MathUtils;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class GunProjectileEntity extends ProjectileEntity implements IEntityFormProvider
{
    private boolean despawn;
    private GunProperties properties = new GunProperties();
    private Form form;
    private IEntity stub = new StubEntity();
    private IEntity target = new MCEntity(this);

    private boolean stuck;
    private int lifeLeft;
    private int bounces;
    private BlockState stuckBlockState;
    private boolean impacted;

    public GunProjectileEntity(EntityType<? extends ProjectileEntity> type, World world)
    {
        super(type, world);
    }

    private void vanish()
    {
        this.discard();
        this.executeCommand(this.properties.cmdVanish);
    }

    private void impact()
    {
        if (this.getWorld().isClient)
        {
            return;
        }

        if (!this.impacted)
        {
            this.setForm(FormUtils.copy(this.properties.impactForm));

            for (ServerPlayerEntity otherPlayer : PlayerLookup.tracking(this))
            {
                ServerNetwork.sendEntityForm(otherPlayer, this);
            }

            this.impacted = true;
        }

        this.executeCommand(this.properties.cmdImpact);
    }

    private void executeCommand(String command)
    {
        if (!command.isEmpty())
        {
            this.getServer().getCommandManager().executeWithPrefix(this.getCommandSource(), command);
        }
    }

    @Override
    protected void initDataTracker()
    {}

    public GunProperties getProperties()
    {
        return this.properties;
    }

    public void setProperties(GunProperties properties)
    {
        this.properties = properties;
        this.bounces = properties.bounces;
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
        this.form = form;

        if (this.form != null)
        {
            this.form.playMain();
        }
    }

    public IEntity getEntity()
    {
        return this.properties.useTarget ? this.target : this.stub;
    }

    @Override
    protected int getPermissionLevel()
    {
        return 2;
    }

    @Override
    public boolean shouldReceiveFeedback()
    {
        return false;
    }

    @Override
    public boolean shouldRender(double distance)
    {
        return true;
    }

    @Override
    public void tick()
    {
        super.tick();

        this.getEntity().update();

        if (this.form != null)
        {
            this.form.update(this.getEntity());
        }

        if (!this.getWorld().isClient)
        {
            this.lifeLeft += 1;

            int ticking = this.properties.ticking;

            if (ticking > 0 && this.lifeLeft % ticking == 0)
            {
                this.executeCommand(this.properties.cmdTicking);
            }

            if (this.lifeLeft >= this.properties.lifeSpan)
            {
                this.vanish();
            }
        }

        /* Movement code */
        Vec3d v = this.getVelocity();

        if (this.prevPitch == 0F && this.prevYaw == 0F)
        {
            this.setYaw(MathUtils.toDeg((float) MathHelper.atan2(v.x, v.z)));
            this.setPitch(MathUtils.toDeg((float) MathHelper.atan2(v.y, v.horizontalLength())));

            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();
        }

        BlockPos blockPos = this.getBlockPos();
        BlockState blockState = this.getWorld().getBlockState(blockPos);
        Vec3d pos;

        if (this.isTouchingWaterOrRain() || blockState.isOf(Blocks.POWDER_SNOW))
        {
            this.extinguish();
        }

        if (this.stuck && this.properties.collideBlocks)
        {
            if (this.stuckBlockState != blockState && this.shouldFall())
            {
                this.fall();
            }
        }
        else
        {
            Vec3d oldPos = this.getPos();

            pos = oldPos.add(v);

            HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit, RaycastContext.ShapeType.COLLIDER);

            if (hitResult.getType() != HitResult.Type.MISS)
            {
                pos = hitResult.getPos();
            }

            EntityHitResult entityHitResult = ProjectileUtil.getEntityCollision(this.getWorld(), this, oldPos, pos, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0), this::canHit);

            if (entityHitResult != null)
            {
                hitResult = entityHitResult;
            }

            boolean canCollide =
                (this.properties.collideBlocks && hitResult.getType() == HitResult.Type.BLOCK) ||
                (this.properties.collideEntities && hitResult.getType() == HitResult.Type.ENTITY);

            if (canCollide)
            {
                this.onCollision(hitResult);

                this.velocityDirty = true;
            }

            v = this.getVelocity();

            double x = this.getX() + v.x;
            double y = this.getY() + v.y;
            double z = this.getZ() + v.z;
            double d = v.horizontalLength();

            this.setYaw(MathUtils.toDeg((float) MathHelper.atan2(v.x, v.z)));
            this.setPitch(MathUtils.toDeg((float) MathHelper.atan2(v.y, d)));
            this.setPitch(updateRotation(this.prevPitch, this.getPitch()));
            this.setYaw(updateRotation(this.prevYaw, this.getYaw()));

            float friction = this.properties.friction;
            float gravity = this.properties.gravity;

            if (this.isTouchingWater())
            {
                for (int particles = 0; particles < 4; ++particles)
                {
                    float hitbox = 0.25F;

                    this.getWorld().addParticle(ParticleTypes.BUBBLE, x - v.x * hitbox, y - v.y * hitbox, z - v.z * hitbox, v.x, v.y, v.z);
                }

                friction = 0.6F;
            }

            this.setVelocity(v.multiply(friction).subtract(0, gravity, 0));
            this.setPosition(x, y, z);
            this.checkBlockCollision();
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

    private boolean shouldFall()
    {
        return this.stuck && this.getWorld().isSpaceEmpty((new Box(this.getPos(), this.getPos())).expand(0.06));
    }

    private void fall()
    {
        Vec3d v = this.getVelocity();

        this.stuck = false;

        this.setVelocity(v.multiply(this.random.nextFloat() * 0.2F, this.random.nextFloat() * 0.2F, this.random.nextFloat() * 0.2F));
    }

    public void move(MovementType movementType, Vec3d movement)
    {
        super.move(movementType, movement);

        if (movementType != MovementType.SELF && this.shouldFall())
        {
            this.fall();
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult)
    {
        super.onEntityHit(entityHitResult);

        if (this.getWorld().isClient || this.properties.damage <= 0F)
        {
            return;
        }

        Entity entity = entityHitResult.getEntity();
        float length = (float)this.getVelocity().length();
        int damage = MathHelper.ceil(MathHelper.clamp(length * this.properties.damage, 0, Integer.MAX_VALUE));

        Entity owner = this.getOwner();
        DamageSource source = this.getDamageSources().magic();

        int fireTicks = entity.getFireTicks();
        boolean deflectsArrows = entity.getType().isIn(EntityTypeTags.DEFLECTS_ARROWS);

        if (this.isOnFire() && !deflectsArrows)
        {
            entity.setOnFireFor(5);
        }

        if (entity.damage(source, (float) damage))
        {
            if (entity instanceof LivingEntity livingEntity)
            {
                if (this.properties.knockback > 0)
                {
                    double resistanceFactor = Math.max(0D, 1D - livingEntity.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE));
                    Vec3d punchVector = this.getVelocity().multiply(1D).normalize().multiply(this.properties.knockback * 0.6D * resistanceFactor);

                    if (punchVector.lengthSquared() > 0D)
                    {
                        livingEntity.addVelocity(punchVector.x, 0.1D, punchVector.z);
                    }
                }

                if (owner instanceof LivingEntity)
                {
                    EnchantmentHelper.onUserDamaged(livingEntity, owner);
                    EnchantmentHelper.onTargetDamaged((LivingEntity)owner, livingEntity);
                }

                this.onHit(livingEntity);
            }
        }
        else if (deflectsArrows)
        {
            this.deflect();
        }
        else
        {
            entity.setFireTicks(fireTicks);
            this.setVelocity(this.getVelocity().multiply(-0.1D));
            this.setYaw(this.getYaw() + 180F);

            this.prevYaw += 180F;
        }
    }

    public void deflect()
    {
        float random = this.random.nextFloat() * 360F;

        this.setVelocity(this.getVelocity().rotateY(MathUtils.toRad(random)).multiply(0.5D));
        this.setYaw(this.getYaw() + random);

        this.prevYaw += random;
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult)
    {
        super.onBlockHit(blockHitResult);

        Vec3d velocity = blockHitResult.getPos().subtract(this.getX(), this.getY(), this.getZ());

        if (this.bounces > 0)
        {
            this.bounces -= 1;

            velocity = this.getVelocity();

            float damp = this.properties.bounceDamping;

            if (blockHitResult.getSide().getAxis() == Direction.Axis.X) velocity = velocity.multiply(-damp, damp, damp);
            if (blockHitResult.getSide().getAxis() == Direction.Axis.Y) velocity = velocity.multiply(damp, -damp, damp);
            if (blockHitResult.getSide().getAxis() == Direction.Axis.Z) velocity = velocity.multiply(damp, damp, -damp);
        }
        else
        {
            this.stuckBlockState = this.getWorld().getBlockState(blockHitResult.getBlockPos());
            this.stuck = true;

            if (this.properties.vanish)
            {
                this.vanish();
            }
        }

        this.setVelocity(velocity);

        Vec3d gravity = velocity.normalize().multiply(0.05D);

        this.setPos(this.getX() - gravity.x, this.getY() - gravity.y, this.getZ() - gravity.z);
        this.impact();
    }

    protected void onHit(LivingEntity target)
    {
        if (this.bounces <= 0 && this.properties.vanish)
        {
            this.vanish();
        }
        else
        {
            this.impact();
        }
    }

    @Override
    protected Entity.MoveEffect getMoveEffect()
    {
        return MoveEffect.NONE;
    }

    @Override
    public boolean isAttackable()
    {
        return false;
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player)
    {
        super.onStartedTrackingBy(player);
        ServerNetwork.sendEntityForm(player, this);
        ServerNetwork.sendGunProperties(player, this);
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
}