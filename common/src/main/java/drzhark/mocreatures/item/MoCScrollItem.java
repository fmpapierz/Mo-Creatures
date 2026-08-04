package drzhark.mocreatures.item;

import drzhark.mocreatures.entity.IMoCEntity;
import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.entity.passive.MoCEntityElephant;
import drzhark.mocreatures.entity.passive.MoCEntityHorse;
import drzhark.mocreatures.entity.passive.MoCEntityOstrich;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Mo'Creatures ownership scrolls, ported from the {@code MoCEntityAnimal.interact} branches:
 * <ul>
 *   <li>{@link Mode#FREEDOM} — the owner releases a tamed creature back to the wild.</li>
 *   <li>{@link Mode#OWNER} — an operator clears a creature's owner so it can be re-claimed.</li>
 *   <li>{@link Mode#SALE} — the owner clears ownership but keeps it tamed/named (claim by renaming).</li>
 * </ul>
 */
public class MoCScrollItem extends Item {

    public enum Mode { FREEDOM, OWNER, SALE }

    private final Mode mode;

    public MoCScrollItem(Mode mode, Properties properties) {
        super(properties);
        this.mode = mode;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof IMoCEntity moc) || !moc.getIsTamed()) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Only the admin reset-owner scroll (OWNER) can be disabled server-wide via config. Releasing your own
        // pet (FREEDOM) and selling it (SALE) always work, matching legacy MoCEntityAnimal.interact where only
        // scrollOfOwner was gated on enableResetOwnership; scrollOfSale cleared ownership unconditionally.
        if (mode == Mode.OWNER && !drzhark.mocreatures.config.MoCConfig.get().enableResetOwnership) {
            return InteractionResult.PASS;
        }
        boolean owner = moc.getOwnerName().equals(player.getName().getString());
        switch (mode) {
            case FREEDOM -> {
                if (!owner) {
                    return InteractionResult.PASS;
                }
                // Legacy released the creature's fitted gear back to the world (dropMyStuff) BEFORE untaming, so a
                // saddled/armoured/bagged mount does not silently swallow its equipment (unrecoverable otherwise).
                dropMyStuff((ServerLevel) player.level(), target, moc);
                moc.setTamed(false);
                moc.setOwnerName("");
                target.setCustomName(null);
                // Legacy freedom scroll released the creature back to the wild fully: clear any
                // sit/stay so it roams again, eject a rider off a mount, and detach it from anything
                // it is riding. With ownership cleared the follow-owner goal then stops on its own.
                if (moc instanceof MoCAnimal animal) {
                    animal.setSitting(false);
                }
                target.ejectPassengers();
                target.stopRiding();
                stack.shrink(1);
            }
            case OWNER -> {
                // Admin reset-owner tool: gate on creative mode (a clean stand-in for the legacy OP check).
                if (!player.getAbilities().instabuild) {
                    return InteractionResult.PASS;
                }
                moc.setOwnerName("");
                stack.shrink(1);
            }
            case SALE -> {
                if (!owner) {
                    return InteractionResult.PASS;
                }
                moc.setOwnerName(""); // stays tamed and keeps its name; claimable by re-naming
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Legacy {@code dropMyStuff()}: when a creature is released back to the wild, spit its fitted gear out as
     * items so the owner can recover it. Mirrors {@code MoCTools.dropSaddle} + {@code dropArmor}/{@code dropBags}
     * for the geared rideables: any saddled creature drops a horse-saddle; a horse additionally drops its worn
     * visible armour and empties its saddlebags; elephants and ostriches empty their saddlebags. The remaining
     * per-species WORN armour, whose tier→item maps live privately on the entity classes, is released by each
     * entity's own {@code dropWornGear}: the elephant's harness/garment/howdah/platform + tusks, the ostrich's
     * helmet + flag, and the big cat's medallion.
     */
    private static void dropMyStuff(ServerLevel level, LivingEntity target, IMoCEntity moc) {
        // Saddle: any rideable creature saddled through MoCAnimal drops a horse-saddle (legacy MoCTools.dropSaddle).
        if (moc instanceof MoCAnimal animal && animal.isSaddled()) {
            dropStack(level, target, new ItemStack(MoCItems.HORSESADDLE.get()));
            animal.setSaddled(false);
        }
        if (target instanceof MoCEntityHorse horse) {
            // Worn visible armour (legacy dropArmor: plays armoroff and drops the matching tier item).
            int tier = horse.getArmor();
            if (tier != 0) {
                ItemStack armor = switch (tier) {
                    case 1 -> new ItemStack(MoCItems.ARMORMETAL.get());
                    case 2 -> new ItemStack(MoCItems.ARMORGOLD.get());
                    case 3 -> new ItemStack(MoCItems.ARMORDIAMOND.get());
                    case 4 -> new ItemStack(MoCItems.HORSEARMORCRYSTAL.get());
                    default -> ItemStack.EMPTY;
                };
                if (!armor.isEmpty()) {
                    level.playSound(null, target.blockPosition(), MoCSounds.ARMOROFF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                    dropStack(level, target, armor);
                }
                horse.setArmor(0);
            }
            // Saddlebags (legacy dropBags + inventory scatter): drop the stored items and the chest block, then bare it.
            if (horse.hasChest()) {
                dropContainer(level, target, horse.getChest());
                dropStack(level, target, new ItemStack(Items.CHEST));
                horse.setHasChest(false);
            }
        } else if (target instanceof MoCEntityElephant elephant && elephant.hasChest()) {
            dropContainer(level, target, elephant.getChest());
            dropStack(level, target, new ItemStack(Items.CHEST));
            elephant.setStorage(0);
        } else if (target instanceof MoCEntityOstrich ostrich && ostrich.getIsChested()) {
            dropContainer(level, target, ostrich.getChest());
            dropStack(level, target, new ItemStack(Items.CHEST));
            ostrich.setIsChested(false);
        }
        // Per-species WORN armour the generic saddle/chest handling above cannot reach (its tier->item maps are
        // private on the entities): each releases its own gear (legacy dropMyStuff released these too) — the
        // elephant's harness/garment/howdah/platform + tusks, the ostrich's helmet + flag, the big cat's medallion.
        if (target instanceof MoCEntityElephant elephant) {
            elephant.dropWornGear(level);
        }
        if (target instanceof MoCEntityOstrich ostrich) {
            ostrich.dropWornGear(level);
        }
        if (target instanceof drzhark.mocreatures.entity.passive.MoCEntityBigCat bigCat) {
            bigCat.dropWornGear(level);
        }
    }

    /** Scatter every stack held by {@code container} into the world, then clear it (legacy dropInventory). */
    private static void dropContainer(ServerLevel level, LivingEntity target, SimpleContainer container) {
        if (container == null) {
            return;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (!s.isEmpty()) {
                dropStack(level, target, s);
            }
        }
        container.clearContent();
    }

    /** Spawn {@code stack} as a pickup-able item at {@code target}, with a small scatter (legacy dropCustomItem). */
    private static void dropStack(ServerLevel level, LivingEntity target, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity item = new ItemEntity(level, target.getX(), target.getY(), target.getZ(), stack);
        item.setPickUpDelay(10);
        item.setDeltaMovement((level.getRandom().nextDouble() - level.getRandom().nextDouble()) * 0.05D,
                (level.getRandom().nextDouble() - level.getRandom().nextDouble()) * 0.05D + 0.2D,
                (level.getRandom().nextDouble() - level.getRandom().nextDouble()) * 0.05D);
        level.addFreshEntity(item);
    }
}
