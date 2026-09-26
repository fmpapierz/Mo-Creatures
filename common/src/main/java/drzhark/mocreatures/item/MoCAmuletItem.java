package drzhark.mocreatures.item;

import drzhark.mocreatures.entity.IMoCEntity;
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
 * Mo'Creatures amulet: right-click a tamed creature to capture it into the amulet; right-click in
 * the air to release the captured creature. Species, sub-type/coat and ownership are preserved.
 */
public class MoCAmuletItem extends Item {

    public MoCAmuletItem(Properties properties) {
        super(properties);
    }

    /**
     * Captures a tamed creature into the amulet.
     *
     * <p>The creature is written to the amulet actually in the player's hand, never to {@code stack}: in creative
     * mode {@code Player.interactOn} hands this method a COPY of the held stack (mc262-ref Player.java:836-838, so
     * creative players never use items up), and writing to it stored the pet in a throwaway copy — the creature
     * vanished and the amulet in hand stayed empty, so it could never be released again. In survival the two are
     * the same stack. The amulet stacks to one (legacy {@code MoCItemPetAmulet.maxStackSize = 1}), so the pet can
     * never end up on a whole stack of amulets at once.</p>
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.has(MoCDataComponents.CAPTURED_CREATURE.get())) {
            return InteractionResult.PASS; // already holding a creature
        }
        if (target instanceof IMoCEntity moc && !moc.getIsTamed()) {
            // Legacy silently ignored a wild creature; say why nothing happened.
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(Component.translatable("message.mocreatures.amulet_tamed_only"));
            }
            return InteractionResult.SUCCESS;
        }
        if (target instanceof IMoCEntity moc && moc.getIsTamed()) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
            String customName = target.hasCustomName() ? target.getCustomName().getString() : "";
            // Preserve a horse's armour tier and any creature's saddled/rideable state through the round-trip.
            int armor = target instanceof drzhark.mocreatures.entity.passive.MoCEntityHorse h ? h.getArmor() : 0;
            boolean saddled = target instanceof drzhark.mocreatures.entity.MoCAnimal a && a.isSaddled();
            held.set(MoCDataComponents.CAPTURED_CREATURE.get(),
                    new CapturedCreature(id.toString(), moc.getTypeMoC(), true, moc.getOwnerName(),
                            target.getHealth(), moc.getMoCAge(), customName, armor, saddled, moc.getIsAdult()));
            // A grey puff where the pet was, the vanilla mob-vanish POOF spread over its body (LivingEntity
            // .makePoofParticles). Legacy showed nothing at all here; the port's first puff used the fx_vanish
            // sprite, a 21-texel speck that was effectively invisible.
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        20, target.getBbWidth() * 0.5D, target.getBbHeight() * 0.5D, target.getBbWidth() * 0.5D, 0.02D);
            }
            target.discard();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

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
                    moc.setTamed(captured.tamed());
                    moc.setOwnerName(captured.owner());
                    moc.setMoCAge(captured.age());
                    moc.setAdult(captured.adult()); // release a captured juvenile as a juvenile (legacy setAdult)
                    if (entity instanceof drzhark.mocreatures.entity.passive.MoCEntityHorse h) {
                        h.setArmor(captured.armor());
                    }
                    if (entity instanceof drzhark.mocreatures.entity.MoCAnimal a) {
                        a.setSaddled(captured.saddled());
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
                    stack.remove(MoCDataComponents.CAPTURED_CREATURE.get());
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
