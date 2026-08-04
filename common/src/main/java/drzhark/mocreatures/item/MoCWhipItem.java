package drzhark.mocreatures.item;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.entity.passive.MoCEntityElephant;
import drzhark.mocreatures.entity.passive.MoCEntityHorse;
import drzhark.mocreatures.entity.passive.MoCEntityKitty;
import drzhark.mocreatures.entity.passive.MoCEntityOstrich;
import drzhark.mocreatures.entity.passive.MoCEntityWyvern;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The Whip — cracking it (right-click a block) commands your owned, tamed Mo'Creatures within 12
 * blocks; right-clicking a creature directly commands just that one. The default command is the
 * sit/stay toggle, but per-species effects (faithful to the legacy {@code MoCItemWhip}) fire first:
 * a ridden ostrich sprints and an unridden tamed one hides its head; a ridden elephant charges; a
 * ridden horse surges forward (a nightmare with a fiery burst); a grounded wyvern toggles sitting
 * (it ignores the crack in flight). Any creature the per-species layer doesn't consume falls back to
 * the sit/stay toggle. Server-side.
 */
public class MoCWhipItem extends Item {

    public MoCWhipItem(Properties properties) {
        super(properties);
    }

    private static boolean owns(Player player, MoCAnimal moc) {
        return moc.getIsTamed() && moc.getOwnerName().equals(player.getName().getString());
    }

    /** Visible confirmation that the sit/stand command registered. */
    private static void feedback(Level level, MoCAnimal moc) {
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(moc.isSitting()
                            ? net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER
                            : net.minecraft.core.particles.ParticleTypes.CLOUD,
                    moc.getX(), moc.getY() + moc.getBbHeight() * 0.6D, moc.getZ(), 6, 0.25D, 0.25D, 0.25D, 0.0D);
        }
    }

    /**
     * Applies the per-species whip effect to one owned, tamed creature (legacy {@code MoCItemWhip}
     * branches). Returns {@code true} when a species-specific effect consumed the crack; {@code false}
     * to let the caller fall back to the default sit/stay toggle. Runs server-side only.
     */
    private static boolean applySpeciesEffect(Level level, MoCAnimal moc) {
        // OSTRICH: a ridden ostrich sprints; a tamed, unridden ostrich toggles head-in-sand hiding.
        if (moc instanceof MoCEntityOstrich ostrich) {
            if (ostrich.isVehicle()) {
                ostrich.whipSprint();
            } else {
                ostrich.whipToggleHiding();
            }
            return true;
        }
        // ELEPHANT: a ridden elephant charges forward; an unridden one falls back to sit/stay.
        if (moc instanceof MoCEntityElephant elephant) {
            if (elephant.isVehicle()) {
                elephant.whipCharge();
                return true;
            }
            return false;
        }
        // HORSE: a ridden horse surges forward (nightmare adds a fiery burst); a tamed, unridden horse
        // toggles its grazing state (legacy whip-crack toggled eating on an unridden horse).
        if (moc instanceof MoCEntityHorse horse) {
            if (!horse.isVehicle() && horse.getIsTamed()) {
                horse.setEating(!horse.getEating());
                return true;
            }
            return horse.whipCrack();
        }
        // WYVERN: toggle sitting only when grounded — a wyvern in flight ignores the crack entirely
        // (returns true so it is NOT then sat down by the fallback).
        if (moc instanceof MoCEntityWyvern wyvern) {
            wyvern.whipToggleSit();
            return true;
        }
        // KITTY: a tamed kitty toggles its sit/stay state only when it is calm and awake. Legacy
        // MoCItemWhip gated the crack on the kitty's mood (getKittyState() > 2 && whipeable()), so a
        // sleeping, hungry (mid-eat) or otherwise busy kitty ignores the whip. The crack is consumed
        // either way (return true) so a non-calm kitty is NOT then sat down by the generic fallback.
        if (moc instanceof MoCEntityKitty kitty) {
            if (kitty.getKittyState() == MoCEntityKitty.STATE_CALM) {
                kitty.setSitting(!kitty.isSitting());
            }
            return true;
        }
        return false;
    }

    /**
     * Dispatch one crack to a creature. Try the per-species effect first; otherwise fall back to the
     * sit/stay toggle ONLY for big cats. Legacy {@code MoCItemWhip} branched on just six species
     * (BigCat, Horse, Kitty, Wyvern, Ostrich, Elephant); a tamed BigCat was the only one commanded via
     * a bare sit/stay toggle, while every other tamed MoC creature (bear, boar, deer, fox, goat, bird,
     * turkey, komodo, and an unridden elephant) was left untouched by the whip.
     */
    private static void command(Level level, MoCAnimal moc) {
        if (!applySpeciesEffect(level, moc)
                && moc instanceof drzhark.mocreatures.entity.passive.MoCEntityBigCat) {
            moc.setSitting(!moc.isSitting());
            feedback(level, moc);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            AABB box = player.getBoundingBox().inflate(12.0D);
            for (MoCAnimal moc : level.getEntitiesOfClass(MoCAnimal.class, box, m -> owns(player, m))) {
                command(level, moc);
            }
            // Legacy: cracking the whip near an untamed adult big cat provokes it into attacking the cracker
            // (skipped on Peaceful, where hostile targeting is disabled anyway).
            if (level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL) {
                for (drzhark.mocreatures.entity.passive.MoCEntityBigCat cat : level.getEntitiesOfClass(
                        drzhark.mocreatures.entity.passive.MoCEntityBigCat.class, box,
                        c -> !c.getIsTamed() && c.getIsAdult())) {
                    cat.setTarget(player);
                }
            }
            level.playSound(null, context.getClickedPos(), MoCSounds.WHIP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            // Legacy MoCItemWhip: each crack wears the whip by 1 durability.
            context.getItemInHand().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof MoCAnimal moc && owns(player, moc)) {
            if (!player.level().isClientSide()) {
                command(player.level(), moc);
                player.level().playSound(null, moc.blockPosition(), MoCSounds.WHIP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                // Legacy MoCItemWhip: each crack wears the whip by 1 durability.
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
