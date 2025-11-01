package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReplayKeyframes extends ValueGroup
{
    public static final String GROUP_POSITION = "position";
    public static final String GROUP_ROTATION = "rotation";
    public static final String GROUP_LEFT_STICK = "lstick";
    public static final String GROUP_RIGHT_STICK = "rstick";
    public static final String GROUP_TRIGGERS = "triggers";
    public static final String GROUP_EXTRA1 = "extra1";
    public static final String GROUP_EXTRA2 = "extra2";

    public static final List<String> CURATED_CHANNELS = Arrays.asList("x", "y", "z", "pitch", "yaw", "headYaw", "bodyYaw", "sneaking", "sprinting", "item_main_hand", "item_off_hand", "item_head", "item_chest", "item_legs", "item_feet", "selected_slot", "stick_lx", "stick_ly", "stick_rx", "stick_ry", "trigger_l", "trigger_r", "extra1_x", "extra1_y", "extra2_x", "extra2_y", "grounded", "damage", "vX", "vY", "vZ");

    public final KeyframeChannel<Double> x = new KeyframeChannel<>("x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> y = new KeyframeChannel<>("y", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> z = new KeyframeChannel<>("z", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double> vX = new KeyframeChannel<>("vX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> vY = new KeyframeChannel<>("vY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> vZ = new KeyframeChannel<>("vZ", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double> yaw = new KeyframeChannel<>("yaw", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> pitch = new KeyframeChannel<>("pitch", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> headYaw = new KeyframeChannel<>("headYaw", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> bodyYaw = new KeyframeChannel<>("bodyYaw", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double> sneaking = new KeyframeChannel<>("sneaking", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> sprinting = new KeyframeChannel<>("sprinting", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> grounded = new KeyframeChannel<>("grounded", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> fall = new KeyframeChannel<>("fall", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> damage = new KeyframeChannel<>("damage", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double> stickLeftX = new KeyframeChannel<>("stick_lx", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> stickLeftY = new KeyframeChannel<>("stick_ly", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> stickRightX = new KeyframeChannel<>("stick_rx", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> stickRightY = new KeyframeChannel<>("stick_ry", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> triggerLeft = new KeyframeChannel<>("trigger_l", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> triggerRight = new KeyframeChannel<>("trigger_r", KeyframeFactories.DOUBLE);

    /* Miscellaneous animatable keyframe channels */
    public final KeyframeChannel<Double> extra1X = new KeyframeChannel<>("extra1_x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> extra1Y = new KeyframeChannel<>("extra1_y", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> extra2X = new KeyframeChannel<>("extra2_x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> extra2Y = new KeyframeChannel<>("extra2_y", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<ItemStack> mainHand = new KeyframeChannel<>("item_main_hand", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> offHand = new KeyframeChannel<>("item_off_hand", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> armorHead = new KeyframeChannel<>("item_head", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> armorChest = new KeyframeChannel<>("item_chest", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> armorLegs = new KeyframeChannel<>("item_legs", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> armorFeet = new KeyframeChannel<>("item_feet", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<Integer> selectedSlot = new KeyframeChannel<>("selected_slot", KeyframeFactories.INTEGER);

    public ReplayKeyframes(String id)
    {
        super(id);

        this.add(this.x);
        this.add(this.y);
        this.add(this.z);
        this.add(this.vX);
        this.add(this.vY);
        this.add(this.vZ);
        this.add(this.yaw);
        this.add(this.pitch);
        this.add(this.headYaw);
        this.add(this.bodyYaw);
        this.add(this.sneaking);
        this.add(this.sprinting);
        this.add(this.grounded);
        this.add(this.fall);
        this.add(this.damage);
        this.add(this.stickLeftX);
        this.add(this.stickLeftY);
        this.add(this.stickRightX);
        this.add(this.stickRightY);
        this.add(this.triggerLeft);
        this.add(this.triggerRight);
        this.add(this.extra1X);
        this.add(this.extra1Y);
        this.add(this.extra2X);
        this.add(this.extra2Y);

        this.add(this.mainHand);
        this.add(this.offHand);
        this.add(this.armorHead);
        this.add(this.armorChest);
        this.add(this.armorLegs);
        this.add(this.armorFeet);
        this.add(this.selectedSlot);
    }

    public List<KeyframeChannel<?>> getChannels()
    {
        ArrayList<KeyframeChannel<?>> channels = new ArrayList<>();

        for (BaseValue baseValue : this.getAll())
        {
            if (baseValue instanceof KeyframeChannel<?> channel)
            {
                channels.add(channel);
            }
        }

        return channels;
    }

    public void shift(float tick)
    {
        for (KeyframeChannel<?> channel : this.getChannels())
        {
            for (Keyframe<?> keyframe : channel.getKeyframes())
            {
                keyframe.setTick(keyframe.getTick() + tick);
            }
        }
    }

    public void copyOver(ReplayKeyframes keyframes, int tick)
    {
        for (KeyframeChannel<?> channel : this.getChannels())
        {
            BaseValue keyframe = keyframes.get(channel.getId());

            if (keyframe instanceof KeyframeChannel<?> keyframeChannel)
            {
                channel.copyOver(keyframeChannel, tick);
            }
        }
    }

    public void record(int tick, IEntity entity, List<String> groups)
    {
        boolean empty = groups == null || groups.isEmpty();
        boolean position = empty || groups.contains(GROUP_POSITION);
        boolean rotation = empty || groups.contains(GROUP_ROTATION);
        boolean leftStick = empty || groups.contains(GROUP_LEFT_STICK);
        boolean rightStick = empty || groups.contains(GROUP_RIGHT_STICK);
        boolean triggers = empty || groups.contains(GROUP_TRIGGERS);
        boolean extra1 = empty || groups.contains(GROUP_EXTRA1);
        boolean extra2 = empty || groups.contains(GROUP_EXTRA2);

        /* Position and rotation */
        if (position)
        {
            this.x.insert(tick, entity.getX());
            this.y.insert(tick, entity.getY());
            this.z.insert(tick, entity.getZ());

            this.vX.insert(tick, entity.getVelocity().x);
            this.vY.insert(tick, entity.getVelocity().y);
            this.vZ.insert(tick, entity.getVelocity().z);

            this.fall.insert(tick, (double) entity.getFallDistance());
        }

        this.sneaking.insert(tick, entity.isSneaking() ? 1D : 0D);
        this.sprinting.insert(tick, entity.isSprinting() ? 1D : 0D);
        this.grounded.insert(tick, entity.isOnGround() ? 1D : 0D);
        this.damage.insert(tick, (double) entity.getHurtTimer());

        if (rotation)
        {
            this.yaw.insert(tick, (double) entity.getYaw());
            this.pitch.insert(tick, (double) entity.getPitch());
            this.headYaw.insert(tick, (double) entity.getHeadYaw());
            this.bodyYaw.insert(tick, (double) entity.getBodyYaw());
        }

        float[] sticks = entity.getExtraVariables();

        if (leftStick)
        {
            this.stickLeftX.insert(tick, (double) sticks[0]);
            this.stickLeftY.insert(tick, (double) sticks[1]);
        }

        if (rightStick)
        {
            this.stickRightX.insert(tick, (double) sticks[2]);
            this.stickRightY.insert(tick, (double) sticks[3]);
        }

        if (triggers)
        {
            this.triggerLeft.insert(tick, (double) sticks[4]);
            this.triggerRight.insert(tick, (double) sticks[5]);
        }

        if (extra1)
        {
            this.extra1X.insert(tick, (double) sticks[6]);
            this.extra1Y.insert(tick, (double) sticks[7]);
        }

        if (extra2)
        {
            this.extra2X.insert(tick, (double) sticks[8]);
            this.extra2Y.insert(tick, (double) sticks[9]);
        }

        if (empty)
        {
            this.mainHand.insert(tick, entity.getEquipmentStack(EquipmentSlot.MAINHAND).copy());
            this.offHand.insert(tick, entity.getEquipmentStack(EquipmentSlot.OFFHAND).copy());
            this.armorHead.insert(tick, entity.getEquipmentStack(EquipmentSlot.HEAD).copy());
            this.armorChest.insert(tick, entity.getEquipmentStack(EquipmentSlot.CHEST).copy());
            this.armorLegs.insert(tick, entity.getEquipmentStack(EquipmentSlot.LEGS).copy());
            this.armorFeet.insert(tick, entity.getEquipmentStack(EquipmentSlot.FEET).copy());
            this.selectedSlot.insert(tick, entity.getSelectedSlot());
        }
    }

    public void apply(int tick, IEntity entity)
    {
        this.apply(tick, entity, null);
    }

    /**
     * Apply a frame at given tick on the given entity.
     */
    public void apply(int tick, IEntity entity, List<String> groups)
    {
        boolean empty = groups == null || groups.isEmpty();
        boolean position = empty || !groups.contains(GROUP_POSITION);
        boolean rotation = empty || !groups.contains(GROUP_ROTATION);
        boolean leftStick = empty || !groups.contains(GROUP_LEFT_STICK);
        boolean rightStick = empty || !groups.contains(GROUP_RIGHT_STICK);
        boolean triggers = empty || !groups.contains(GROUP_TRIGGERS);
        boolean extra1 = empty || !groups.contains(GROUP_EXTRA1);
        boolean extra2 = empty || !groups.contains(GROUP_EXTRA2);

        if (position)
        {
            entity.setVelocity(this.vX.interpolate(tick).floatValue(), this.vY.interpolate(tick).floatValue(), this.vZ.interpolate(tick).floatValue());
            entity.setFallDistance(this.fall.interpolate(tick).floatValue());

            KeyframeSegment<Double> x = this.x.findSegment(tick);
            Vector2d xx = this.getPrev(x, this.x.interpolate(tick - 1), tick);
            KeyframeSegment<Double> y = this.y.findSegment(tick);
            Vector2d yy = this.getPrev(y, this.y.interpolate(tick - 1), tick);
            KeyframeSegment<Double> z = this.z.findSegment(tick);
            Vector2d zz = this.getPrev(z, this.z.interpolate(tick - 1), tick);

            entity.setPosition(xx.x, yy.x, zz.x);
            entity.setPrevX(xx.y);
            entity.setPrevY(yy.y);
            entity.setPrevZ(zz.y);
        }

        if (rotation)
        {
            KeyframeSegment<Double> yaw = this.yaw.findSegment(tick);
            Vector2d yyaw = this.getPrev(yaw, this.yaw.interpolate(tick - 1), tick);
            KeyframeSegment<Double> pitch = this.pitch.findSegment(tick);
            Vector2d ppitch = this.getPrev(pitch, this.pitch.interpolate(tick - 1), tick);
            KeyframeSegment<Double> headYaw = this.headYaw.findSegment(tick);
            Vector2d hheadYaw = this.getPrev(headYaw, this.headYaw.interpolate(tick - 1), tick);
            KeyframeSegment<Double> bodyYaw = this.bodyYaw.findSegment(tick);
            Vector2d bbodyYaw = this.getPrev(bodyYaw, this.bodyYaw.interpolate(tick - 1), tick);

            entity.setYaw((float) yyaw.x);
            entity.setPitch((float) ppitch.x);
            entity.setHeadYaw((float) hheadYaw.x);
            entity.setBodyYaw((float) bbodyYaw.x);

            entity.setPrevYaw((float) yyaw.y);
            entity.setPrevPitch((float) ppitch.y);
            entity.setPrevHeadYaw((float) hheadYaw.y);
            entity.setPrevBodyYaw((float) bbodyYaw.y);
        }

        /* Motion and fall distance */
        entity.setSneaking(this.sneaking.interpolate(tick) != 0D);
        entity.setSprinting(this.sprinting.interpolate(tick) != 0D);
        entity.setOnGround(this.grounded.interpolate(tick) != 0D);
        entity.setHurtTimer(this.damage.interpolate(tick).intValue());

        float[] sticks = entity.getExtraVariables();

        if (leftStick)
        {
            sticks[0] = this.stickLeftX.interpolate(tick).floatValue();
            sticks[1] = this.stickLeftY.interpolate(tick).floatValue();
        }

        if (rightStick)
        {
            sticks[2] = this.stickRightX.interpolate(tick).floatValue();
            sticks[3] = this.stickRightY.interpolate(tick).floatValue();
        }

        if (triggers)
        {
            sticks[4] = this.triggerLeft.interpolate(tick).floatValue();
            sticks[5] = this.triggerRight.interpolate(tick).floatValue();
        }

        if (extra1)
        {
            sticks[6] = this.extra1X.interpolate(tick).floatValue();
            sticks[7] = this.extra1Y.interpolate(tick).floatValue();
        }

        if (extra2)
        {
            sticks[8] = this.extra2X.interpolate(tick).floatValue();
            sticks[9] = this.extra2Y.interpolate(tick).floatValue();
        }

        entity.setEquipmentStack(EquipmentSlot.MAINHAND, this.mainHand.interpolate(tick));
        entity.setEquipmentStack(EquipmentSlot.OFFHAND, this.offHand.interpolate(tick));
        entity.setEquipmentStack(EquipmentSlot.HEAD, this.armorHead.interpolate(tick));
        entity.setEquipmentStack(EquipmentSlot.CHEST, this.armorChest.interpolate(tick));
        entity.setEquipmentStack(EquipmentSlot.LEGS, this.armorLegs.interpolate(tick));
        entity.setEquipmentStack(EquipmentSlot.FEET, this.armorFeet.interpolate(tick));
    }

    /**
     * Force teleportation for the previous keyframe being constant
     */
    private Vector2d getPrev(KeyframeSegment<Double> frame, double prev, int tick)
    {
        if (frame == null)
        {
            return new Vector2d(prev, prev);
        }

        IInterp interp = frame.a.getInterpolation().getInterp();
        Double interpolated = frame.createInterpolated();

        /*  */
        if (interp == Interpolations.CONST || interp == Interpolations.STEP)
        {
            if (interpolated != null)
            {
                prev = interpolated;
            }

            return new Vector2d(prev, prev);
        }

        if (frame.preA != frame.a && frame.a.getTick() == tick && (frame.preA.getInterpolation().getInterp() == Interpolations.CONST || frame.preA.getInterpolation().getInterp() == Interpolations.STEP))
        {
            if (interpolated != null)
            {
                prev = interpolated;
            }

            return new Vector2d(prev, prev);
        }

        return new Vector2d(interpolated == null ? prev : interpolated, prev);
    }
}