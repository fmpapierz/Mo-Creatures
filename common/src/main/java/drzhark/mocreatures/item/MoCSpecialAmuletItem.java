package drzhark.mocreatures.item;

import java.util.function.Supplier;

import drzhark.mocreatures.entity.passive.MoCEntityHorse;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Port of the legacy full special-horse amulets ({@code amuletbonefull / amuletfairyfull / amuletpegasusfull /
 * amuletghostfull}). Right-clicking a block releases the captured horse — recreated at its stored coat (and
 * name), tamed to the player — and returns the matching empty amulet. The empty amulets do the reverse capture
 * via {@link MoCEntityHorse#mobInteract}.
 */
public class MoCSpecialAmuletItem extends Item {

    private final Supplier<? extends Item> emptyForm;

    public MoCSpecialAmuletItem(Properties properties, Supplier<? extends Item> emptyForm) {
        super(properties);
        this.emptyForm = emptyForm;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            ItemStack stack = context.getItemInHand();
            int type = 0;
            String name = "";
            // Full captured state round-trips exactly as legacy MoCItemAmulet did (health/edad/rideable/
            // armor/adult). Tags are read with legacy-fallback defaults so a coat-only or legacy amulet
            // still releases an adult, full-health, bare, unsaddled horse (legacy creatureType==0 path).
            float health = 0.0F;
            int mocAge = -1;
            boolean saddled = false;
            int armor = 0;
            boolean adult = true;
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            if (cd != null) {
                CompoundTag tag = cd.copyTag();
                type = tag.getIntOr("HorseType", 0);
                name = tag.getStringOr("HorseName", "");
                health = tag.getFloatOr("Health", 0.0F);
                mocAge = tag.getIntOr("MoCAge", -1);
                saddled = tag.getBooleanOr("Rideable", false);
                armor = tag.getIntOr("Armor", 0);
                adult = tag.getBooleanOr("Adult", true);
            }
            MoCEntityHorse horse = MoCEntities.HORSE.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
            if (horse != null) {
                BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
                horse.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                if (type != 0) {
                    horse.setTypeMoC(type);
                }
                horse.setAdult(adult);
                if (mocAge >= 0) {
                    horse.setMoCAge(mocAge);
                }
                horse.setSaddled(saddled);
                horse.setArmor(armor);
                horse.setTamed(true);
                horse.setOwnerName(player.getName().getString());
                if (!name.isEmpty()) {
                    horse.setCustomName(Component.literal(name));
                }
                serverLevel.addFreshEntity(horse);
                // Legacy MoCItemAmulet played the "appearmagic" cue on every release; the port signals the
                // release with an FX_STAR sparkle burst, matching MoCAmuletItem's release-FX convention.
                serverLevel.sendParticles(MoCParticles.FX_STAR.get(),
                        horse.getX(), horse.getY() + horse.getBbHeight() * 0.5D, horse.getZ(),
                        25, 0.4D, 0.5D, 0.4D, 0.05D);
                if (health > 0.0F) {
                    horse.setHealth(Math.min(health, horse.getMaxHealth()));
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                player.addItem(new ItemStack(emptyForm.get()));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
