package mchorse.bbs_mod.actions;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.DataPath;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionPlayer
{
    public Film film;
    public int tick;
    public boolean playing = true;
    public int countdown;
    public int exception;
    public boolean syncing;
    public boolean stopDamage = true;

    private ServerPlayerEntity serverPlayer;
    private ServerWorld world;
    private int duration;

    private Map<String, LivingEntity> actors = new HashMap<>();
    private List<ItemStack> originalInventory = null;
    private int originalSelectedSlot = 0;

    public ActionPlayer(ServerPlayerEntity serverPlayer, ServerWorld world, Film film, int tick, int countdown, int exception)
    {
        this.world = world;
        this.film = film;
        this.tick = tick;
        this.countdown = countdown;
        this.exception = exception;
        this.serverPlayer = serverPlayer;

        this.duration = film.camera.calculateDuration();

        // Backup the player's original inventory for first-person mode
        this.backupPlayerInventory();

        this.updateReplayEntities();
    }

    public void updateReplayEntities()
    {
        for (LivingEntity entity : this.actors.values())
        {
            if (!entity.isPlayer())
            {
                entity.discard();
            }
        }

        this.actors.clear();

        List<Replay> list = this.film.replays.getList();

        for (int i = 0; i < list.size(); i++)
        {
            Replay replay = list.get(i);
            boolean isActor = !replay.actor.get();

            if (replay.fp.get())
            {
                isActor = false;
            }

            if (i == this.exception || isActor || !replay.enabled.get())
            {
                continue;
            }

            if (replay.fp.get() && this.serverPlayer != null)
            {
                this.actors.put(replay.getId(), this.serverPlayer);
            }
            else
            {
                ActorEntity actor = new ActorEntity(BBSMod.ACTOR_ENTITY, this.world);

                actor.setForm(FormUtils.copy(replay.form.get()));

                this.apply(actor, replay, this.tick, false);
                this.actors.put(replay.getId(), actor);
                this.world.spawnEntity(actor);
            }
        }

        for (ServerPlayerEntity player : this.world.getPlayers())
        {
            ServerNetwork.sendActors(player, this.film.getId(), this.actors);
        }
    }

    public ServerWorld getWorld()
    {
        return this.world;
    }

    public void apply(LivingEntity actor, Replay replay, float tick, boolean ticking)
    {
        double x = replay.keyframes.x.interpolate(tick);
        double y = replay.keyframes.y.interpolate(tick);
        double z = replay.keyframes.z.interpolate(tick);
        float yawHead = replay.keyframes.headYaw.interpolate(tick).floatValue();
        float yawBody = replay.keyframes.bodyYaw.interpolate(tick).floatValue();
        float pitch = replay.keyframes.pitch.interpolate(tick).floatValue();

        Vec3d pos = actor.getPos();

        if (ticking)
        {
            actor.move(MovementType.SELF, new Vec3d(x - pos.x, y - pos.y, z - pos.z));
        }

        actor.setPosition(x, y, z);
        actor.setYaw(yawHead);
        actor.setHeadYaw(yawHead);
        actor.setPitch(pitch);
        actor.setBodyYaw(yawBody);
        actor.setSneaking(replay.keyframes.sneaking.interpolate(tick) > 0);
        actor.setOnGround(replay.keyframes.grounded.interpolate(tick) > 0);
        
        // Only apply equipment changes to ActorEntity instances, not to real players
        // This prevents inventory duplication issues in first person mode
        if (!(actor instanceof ServerPlayerEntity))
        {
            actor.equipStack(EquipmentSlot.MAINHAND, replay.keyframes.mainHand.interpolate(tick, ItemStack.EMPTY));
            actor.equipStack(EquipmentSlot.OFFHAND, replay.keyframes.offHand.interpolate(tick, ItemStack.EMPTY));
            actor.equipStack(EquipmentSlot.HEAD, replay.keyframes.armorHead.interpolate(tick, ItemStack.EMPTY));
            actor.equipStack(EquipmentSlot.CHEST, replay.keyframes.armorChest.interpolate(tick, ItemStack.EMPTY));
            actor.equipStack(EquipmentSlot.LEGS, replay.keyframes.armorLegs.interpolate(tick, ItemStack.EMPTY));
            actor.equipStack(EquipmentSlot.FEET, replay.keyframes.armorFeet.interpolate(tick, ItemStack.EMPTY));

            // Provide recorded inventory to ActorEntity so it can drop it on death
            if (actor instanceof mchorse.bbs_mod.entity.ActorEntity actorEntity)
            {
                java.util.List<ItemStack> recordedInventory = replay.keyframes.inventory.interpolate(tick);
                if (recordedInventory != null && !recordedInventory.isEmpty())
                {
                    actorEntity.setRecordedInventory(recordedInventory);
                }

                // Set recorded XP so actor drops the same experience on death
                Double xp = replay.keyframes.experience.interpolate(tick);
                if (xp != null)
                {
                    actorEntity.setXpToDrop(xp.intValue());
                }
            }
        }
        
        // Apply hotbar selection and inventory to real players (for first-person mode)
        if (actor instanceof ServerPlayerEntity player)
        {
            int selectedSlot = replay.keyframes.hotbarSelection.interpolate(tick).intValue();
            if (selectedSlot >= 0 && selectedSlot < 9)
            {
                player.getInventory().selectedSlot = selectedSlot;
            }
            
            // Apply the recorded inventory
            List<ItemStack> recordedInventory = replay.keyframes.inventory.interpolate(tick);
            if (recordedInventory != null && !recordedInventory.isEmpty())
            {
                for (int i = 0; i < Math.min(recordedInventory.size(), player.getInventory().size()); i++)
                {
                    player.getInventory().setStack(i, recordedInventory.get(i).copy());
                }
            }
        }

        actor.fallDistance = replay.keyframes.fall.interpolate(tick).floatValue();
    }

    public boolean tick()
    {
        if (this.countdown > 0)
        {
            this.countdown -= 1;

            return false;
        }

        for (Map.Entry<String, LivingEntity> entry : this.actors.entrySet())
        {
            Replay replay = (Replay) this.film.replays.get(entry.getKey());

            if (replay != null)
            {
                this.apply(entry.getValue(), replay, this.tick, true);
            }
        }

        if (!this.playing)
        {
            return false;
        }

        if (this.tick >= 0)
        {
            this.applyAction();
        }

        this.tick += 1;

        return !this.syncing ? this.tick >= this.duration : false;
    }

    private void applyAction()
    {
        SuperFakePlayer fakePlayer = SuperFakePlayer.get(this.world);
        List<Replay> list = this.film.replays.getList();

        for (int i = 0; i < list.size(); i++)
        {
            if (i == this.exception)
            {
                continue;
            }

            Replay replay = list.get(i);

            if (!replay.enabled.get())
            {
                continue;
            }

            LivingEntity actor = this.actors.get(replay.getId());

            replay.applyActions(actor, fakePlayer, this.film, this.tick);
        }
    }

    public void syncData(DataPath key, BaseType data)
    {
        BaseValue baseValue = this.film.getRecursively(key);

        if (baseValue != null)
        {
            baseValue.fromData(data);

            if (baseValue.getId().equals("actor") || baseValue.getId().equals("enabled") || baseValue.getId().equals("replays"))
            {
                this.updateReplayEntities();
            }
        }
    }

    public void goTo(int tick)
    {
        this.goTo(this.tick, tick);
    }

    public void goTo(int from, int tick)
    {
        for (Map.Entry<String, LivingEntity> entry : this.actors.entrySet())
        {
            Replay replay = (Replay) this.film.replays.get(entry.getKey());

            if (replay != null)
            {
                this.apply(entry.getValue(), replay, this.tick, false);
            }
        }

        if (from != tick)
        {
            this.tick = from;

            while (this.tick != tick)
            {
                this.tick += this.tick > tick ? -1 : 1;

                this.applyAction();
            }
        }
    }

    public void stop()
    {
        // Restore the player's original inventory for first-person mode
        this.restorePlayerInventory();
        
        for (LivingEntity value : this.actors.values())
        {
            if (!value.isPlayer())
            {
                value.discard();
            }
        }
    }

    public void toggle()
    {
        this.playing = !this.playing;
    }

    private void backupPlayerInventory()
    {
        if (this.serverPlayer != null)
        {
            this.originalInventory = new ArrayList<>();
            for (int i = 0; i < this.serverPlayer.getInventory().size(); i++)
            {
                this.originalInventory.add(this.serverPlayer.getInventory().getStack(i).copy());
            }
            this.originalSelectedSlot = this.serverPlayer.getInventory().selectedSlot;
        }
    }

    private void restorePlayerInventory()
    {
        if (this.serverPlayer != null && this.originalInventory != null)
        {
            // Clear all inventory slots first to remove any ghost items
            for (int i = 0; i < this.serverPlayer.getInventory().size(); i++)
            {
                this.serverPlayer.getInventory().setStack(i, ItemStack.EMPTY);
            }
            
            // Restore original inventory
            for (int i = 0; i < Math.min(this.originalInventory.size(), this.serverPlayer.getInventory().size()); i++)
            {
                this.serverPlayer.getInventory().setStack(i, this.originalInventory.get(i).copy());
            }
            this.serverPlayer.getInventory().selectedSlot = this.originalSelectedSlot;
        }
    }
}