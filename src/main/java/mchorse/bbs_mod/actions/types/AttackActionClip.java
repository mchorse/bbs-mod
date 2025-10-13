package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AttackActionClip extends ActionClip
{
    public final ValueFloat damage = new ValueFloat("damage", 0F);

    public AttackActionClip()
    {
        super();

        this.add(this.damage);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        float damage = this.damage.get();

        if (damage <= 0F)
        {
            return;
        }

        this.applyPositionRotation(player, replay, tick);

        double distance = 6D;
        HitResult blockHit = player.raycast(distance, 1F, false);
        Vec3d origin = player.getCameraPosVec(1F);
        Vec3d rotation = player.getRotationVec(1F);
        Vec3d direction = origin.add(rotation.x * distance, rotation.y * distance, rotation.z * distance);

        double newDistance = blockHit != null ? blockHit.getPos().squaredDistanceTo(origin) : distance * distance;
        Box box = player.getBoundingBox().stretch(rotation.multiply(distance)).expand(1, 1, 1);
        EntityHitResult enittyHit = ProjectileUtil.raycast(actor == null ? player : actor, origin, direction, box, entity -> !entity.isSpectator() && entity.canHit(), newDistance);

        if (enittyHit != null)
        {
            Entity entity = enittyHit.getEntity();

            if (entity != null)
            {
                entity.damage(player.getWorld().getDamageSources().mobAttack(player), damage);
            }
        }
    }

    @Override
    protected Clip create()
    {
        return new AttackActionClip();
    }
}