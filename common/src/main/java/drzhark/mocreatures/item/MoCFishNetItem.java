package drzhark.mocreatures.item;

import java.util.function.Supplier;

import drzhark.mocreatures.entity.IMoCEntity;
import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.entity.MoCAquatic;
import drzhark.mocreatures.entity.passive.MoCEntityFishy;
import drzhark.mocreatures.entity.passive.MoCEntityJellyFish;
import drzhark.mocreatures.entity.passive.MoCEntityMediumFish;
import drzhark.mocreatures.entity.passive.MoCEntityRay;
import drzhark.mocreatures.entity.passive.MoCEntitySmallFish;
import drzhark.mocreatures.network.MoCNetwork;
import drzhark.mocreatures.registry.MoCDataComponents;
import drzhark.mocreatures.registry.MoCParticles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Port of the legacy fishnet pair ({@code MoCItems.fishnet} / {@code fishnetfull}, both
 * {@code MoCItemPetAmulet} with {@code maxStackSize = 1}). One class serves both forms, wired to each
 * other through the {@code counterpart} supplier like the special horse amulets:
 *
 * <ul>
 *   <li><b>Empty net</b> — right-click a small aquatic creature (or any tamed aquatic) to scoop it into
 *       a full net, preserving species, sub-type, owner, health, age, custom name and adult state. Legacy
 *       ran this through {@code MoCEntityTameableAquatic.processInteract}:248 gated on
 *       {@code canBeTrappedInNet()}.</li>
 *   <li><b>Full net</b> — right-click to release the stored creature in front of the player and get the
 *       empty net back ({@code MoCItemPetAmulet.onItemRightClick}, hand swap at :171).</li>
 * </ul>
 *
 * Capture and release mirror {@link MoCAmuletItem} (same {@code CAPTURED_CREATURE} component, same
 * vanish/star FX), so a netted creature round-trips exactly like an amulet-stored one.
 */
public class MoCFishNetItem extends Item {

    /** The other half of the pair: the full net for the empty form, the empty net for the full form. */
    private final Supplier<? extends Item> counterpart;
    /** Whether this is the full ({@code fishnetfull}) form, which only releases and never captures. */
    private final boolean full;

    public MoCFishNetItem(Properties properties, Supplier<? extends Item> counterpart, boolean full) {
        super(properties);
        this.counterpart = counterpart;
        this.full = full;
    }

    /**
     * Which creatures fit in the net. The small aquatic species go in wild or tamed — legacy overrode
     * {@code canBeTrappedInNet()} to {@code true} unconditionally (MoCEntityFishy:206, MoCEntitySmallFish:106,
     * MoCEntityMediumFish:152, MoCEntityRay:58, MoCEntityJellyFish:150) — while every other aquatic falls
     * back to the tamed-only base case ({@code MoCEntityAquatic.canBeTrappedInNet}:638-640).
     */
    private static boolean canBeNetted(MoCAquatic aquatic) {
        return aquatic instanceof MoCEntityFishy || aquatic instanceof MoCEntitySmallFish
                || aquatic instanceof MoCEntityMediumFish || aquatic instanceof MoCEntityRay
                || aquatic instanceof MoCEntityJellyFish || aquatic.getIsTamed();
    }

    // ---------------------------------------------------- empty net: capture (right-click a creature)

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (this.full || stack.has(MoCDataComponents.CAPTURED_CREATURE.get())) {
            return InteractionResult.PASS; // the full net only releases
        }
        if (!(target instanceof MoCAquatic aquatic) || !canBeNetted(aquatic)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        String customName = target.hasCustomName() ? target.getCustomName().getString() : "";
        // Stored tamed=true because a legacy release always came out tamed (MoCItemPetAmulet:101
        // setTamed(true)); the owner field records whether it was wild when netted ("" = wild) so the
        // release path knows to run the tame cap and name prompt.
        ItemStack fullNet = new ItemStack(this.counterpart.get());
        fullNet.set(MoCDataComponents.CAPTURED_CREATURE.get(),
                new CapturedCreature(id.toString(), aquatic.getTypeMoC(), true, aquatic.getOwnerName(),
                        target.getHealth(), aquatic.getMoCAge(), customName, 0, false, aquatic.getIsAdult()));
        // Legacy MoCEntityFXVanish puff at the creature's position on capture, as MoCAmuletItem.
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(MoCParticles.FX_VANISH.get(),
                    target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                    20, 0.3D, 0.4D, 0.3D, 0.02D);
        }
        target.discard();
        if (player.getAbilities().instabuild) {
            player.addItem(fullNet);
        } else {
            // Nets stack to 1, so the empty net in this hand simply becomes the full one.
            player.setItemInHand(hand, fullNet);
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------- full net: release (right-click)

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CapturedCreature captured = stack.get(MoCDataComponents.CAPTURED_CREATURE.get());
        if (captured == null) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(captured.typeId()));
            if (type != null) {
                Entity entity = type.create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
                if (entity instanceof IMoCEntity moc) {
                    Vec3 look = player.getLookAngle();
                    entity.snapTo(player.getX() + look.x * 2.0D, player.getY(), player.getZ() + look.z * 2.0D,
                            player.getYRot(), 0.0F);
                    moc.setTypeMoC(captured.variant());
                    moc.setMoCAge(captured.age());
                    moc.setAdult(captured.adult()); // release a captured juvenile as a juvenile (legacy setAdult)
                    if (!captured.owner().isEmpty()) {
                        moc.setTamed(captured.tamed());
                        moc.setOwnerName(captured.owner());
                    } else if (!MoCAnimal.exceedsTameCap(entity, player)) {
                        // Netted wild: legacy released it tamed to whoever opens the net and prompted for a
                        // name (MoCItemPetAmulet:167-168 tameWithName), cap-gated like every other taming path.
                        moc.setTamed(true);
                        moc.setOwnerName(player.getName().getString());
                        MoCNetwork.promptName(moc, player);
                    }
                    if (entity instanceof LivingEntity living && captured.health() > 0.0F) {
                        living.setHealth(captured.health());
                    }
                    if (!captured.customName().isEmpty()) {
                        entity.setCustomName(Component.literal(captured.customName()));
                        entity.setCustomNameVisible(true);
                    }
                    serverLevel.addFreshEntity(entity);
                    // Legacy MoCEntityFXStar sparkle burst at the spawned creature on release.
                    serverLevel.sendParticles(MoCParticles.FX_STAR.get(),
                            entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(),
                            25, 0.4D, 0.5D, 0.4D, 0.05D);
                    // Legacy MoCItemPetAmulet:171 hands back the empty form after a successful release.
                    player.setItemInHand(hand, new ItemStack(this.counterpart.get()));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
