package drzhark.mocreatures.item;

import drzhark.mocreatures.entity.passive.MoCEntityEgg;
import drzhark.mocreatures.registry.MoCEntities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The {@code mocegg} item — the port of the legacy {@code MoCItemEgg}.
 *
 * <p>Legacy made one item carry every egg in the mod through its metadata: it was {@code setHasSubtypes(true)},
 * and {@code getUnlocalizedName(ItemStack)} appended the damage value, so meta 25 was a "Coral Snake Egg" and
 * meta 54 a "Mother Wyvern Egg", while the bare item name "Spoiled Egg" applied only to the blank meta-0 egg.
 * Item metadata no longer exists in 26.2, so the subtype rides in {@code CUSTOM_DATA} under {@code EggType} and
 * {@link #getName(ItemStack)} resolves it to the matching translation key. That keeps one registered item and
 * one icon, exactly as legacy had, while restoring the 33 distinct names.</p>
 *
 * <p>Right-click <em>places</em> the egg at the player's feet with a small random nudge, as legacy did — it is
 * not thrown. A wild ostrich egg (30) becomes a stolen egg (31) when placed, which is what makes the hatchling
 * tame.</p>
 */
public class MoCThrownEggItem extends Item {

    public MoCThrownEggItem(Properties properties) {
        super(properties);
    }

    /** The legacy composite egg id stored on the stack, or 0 for a blank "Spoiled Egg". */
    public static int eggTypeOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getIntOr("EggType", 0);
    }

    /**
     * Builds an egg stack of the given legacy composite id. Use this everywhere an egg is created so the stack
     * always carries both halves of its identity: {@code EggType} in custom data (which drives the name and
     * what hatches) and the species tint in {@code CUSTOM_MODEL_DATA} colour slot 0 (which drives the icon).
     */
    public static ItemStack createEgg(int eggType) {
        ItemStack stack = new ItemStack(drzhark.mocreatures.registry.MoCItems.MOCEGG.get());
        if (eggType > 0) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("EggType", eggType);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                    List.of(), List.of(), List.of(), List.of(eggColour(eggType))));
        }
        return stack;
    }

    /**
     * The icon tint for an egg subtype.
     *
     * <p>Legacy registered a single shared icon for every egg metadata value ({@code MoCItem.registerIcons}
     * uses the no-arg {@code getUnlocalizedName()}), so all 33 eggs looked identical and were told apart only
     * by name. Rather than invent 33 sprites, this keeps the one egg sprite and tints it — the same trick
     * vanilla uses for spawn eggs, and the same idea as legacy's own {@code MoCEggColour}, which derived a
     * creature's egg colours from the dominant colours of its texture. These values were sampled from each
     * species' own model texture in {@code textures/models/}, skipping the sheets' flat grey background and
     * the palette block the wyvern skins share.</p>
     */
    public static int eggColour(int eggType) {
        return switch (eggType) {
            case 1 -> 0x0046F9;  // fishy blue
            case 2 -> 0xF14F00;  // fishy orange
            case 3 -> 0x9AE6D6;  // fishy cyan
            case 4 -> 0xABFF47;  // fishy greeny
            case 5 -> 0x0D6201;  // fishy green
            case 6 -> 0x8D01AC;  // fishy purple
            case 7 -> 0xF8DE01;  // fishy yellow
            case 8 -> 0x553123;  // fishy striped
            case 9 -> 0xDAD28E;  // fishy yellowy
            case 10 -> 0xCA0000; // piranha
            case 11 -> 0x9498B4; // shark
            case 21 -> 0x0A3D00; // dark snake
            case 22 -> 0x3D3238; // spotted snake
            case 23 -> 0xEF4A00; // orange snake
            case 24 -> 0x50B600; // green snake
            case 25 -> 0xCFC92D; // coral snake
            case 26 -> 0x353D00; // cobra
            case 27 -> 0x654C00; // rattlesnake
            case 28 -> 0x8A4A24; // python
            case 30, 31 -> 0x9E6C63; // ostrich (wild and stolen share a name and a colour, as in legacy)
            case 33 -> 0x553014; // komodo dragon
            case 41 -> 0xAF6926; // dirt scorpion
            case 42 -> 0x14141A; // cave scorpion
            case 43 -> 0x510B01; // nether scorpion
            case 44 -> 0x0D435A; // frost scorpion
            case 45 -> 0x25210C; // undead scorpion
            case 50 -> 0x11540B; // jungle wyvern
            case 51 -> 0x4D5E00; // swamp wyvern
            case 52 -> 0x773512; // sand wyvern
            case 53 -> 0xF4B743; // savanna wyvern
            case 54 -> 0x9F240F; // mother wyvern
            default -> 0xFFFFFF; // spoiled / unnamed metas stay the plain white egg
        };
    }

    /**
     * Legacy {@code MoCItemEgg.getUnlocalizedName(ItemStack)} appended the metadata so each subtype had its own
     * name. Here the subtype comes from {@code EggType}; metas legacy never named (0, 12-20, 32, 34-40, 46-49)
     * keep the plain "Spoiled Egg".
     */
    @Override
    public Component getName(ItemStack stack) {
        int type = eggTypeOf(stack);
        return hasOwnName(type) ? Component.translatable("item.mocreatures.mocegg." + type) : super.getName(stack);
    }

    /**
     * The egg ids legacy gave a name to (MoCreatures.java:897-935). Everything else — 0, 12-20, 32, 34-40,
     * 46-49 and anything past 54 — kept the bare "Spoiled Egg", so those fall through to the base name.
     *
     * <p>This is decided from the id alone rather than by probing the language table, because
     * {@code Component.getString()} resolves against {@code Language.getInstance()}, which on a dedicated
     * server only ever holds vanilla's {@code en_us.json} — a lookup-based test would report every subtype as
     * "Spoiled Egg" in multiplayer while looking correct in single-player.</p>
     */
    private static boolean hasOwnName(int type) {
        return (type >= 1 && type <= 11)     // fishy 1-10, shark 11
                || (type >= 21 && type <= 28) // snakes
                || type == 30 || type == 31   // ostrich, stolen ostrich
                || type == 33                 // komodo
                || (type >= 41 && type <= 45) // scorpions
                || (type >= 50 && type <= 54); // wyverns
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            int type = eggTypeOf(stack);
            if (type == 30) {
                type = 31; // legacy: a placed ostrich egg becomes a stolen egg, so it hatches tamed
            }
            MoCEntityEgg egg = MoCEntities.EGG.get().create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (egg == null) {
                return InteractionResult.PASS;
            }
            if (type > 0) {
                egg.setEggType(type);
            } else {
                // Legacy: a blank egg is a spoiled fishy egg. It only ever hatches in water, and picks a random
                // fishy variant when it does; on land it is worthless.
                egg.setTypeMoC(MoCEntityEgg.TYPE_FISHY);
                egg.setVariant(0);
            }
            egg.setPos(player.getX(), player.getY(), player.getZ());
            RandomSource r = serverLevel.getRandom();
            egg.setDeltaMovement(egg.getDeltaMovement().add(new Vec3(
                    (r.nextFloat() - r.nextFloat()) * 0.3F,
                    r.nextFloat() * 0.05F,
                    (r.nextFloat() - r.nextFloat()) * 0.3F)));
            serverLevel.addFreshEntity(egg);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
