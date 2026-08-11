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
 * The Whip — cracking it (right-click a block) commands every owned, tamed Mo'Creature within 12
 * blocks; right-clicking one creature directly commands just that one.
 *
 * <p>An unridden creature toggles between following its owner and staying put; a horse told to wait
 * grazes and an ostrich buries its head, the legacy flourishes for those two. A creature you are RIDING
 * is urged on instead: an ostrich sprints, an elephant charges, and a horse surges forward (a nightmare
 * with a fiery burst). A wyvern in flight and a kitty that is not calm ignore the crack, as in legacy.
 * Server-side.</p>
 */
public class MoCWhipItem extends Item {

    public MoCWhipItem(Properties properties) {
        super(properties);
    }

    /**
     * Whether the whip may command this creature. Legacy {@code MoCItemWhip}:55-63 skipped a creature only
     * when ownership is enforced AND it belongs to somebody else — an unowned or wild creature always fell
     * through to the species branches (which is how a wild, half-broken ostrich or elephant could still be
     * whip-sprinted). With {@code enableOwnership} off, the whip commanded everything.
     */
    private static boolean commandable(Player player, MoCAnimal moc) {
        if (!drzhark.mocreatures.config.MoCConfig.get().enableOwnership) {
            return true;
        }
        String owner = moc.getOwnerName();
        return owner == null || owner.isEmpty() || owner.equals(player.getName().getString());
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
     * Legacy {@code MoCItemWhip.whipFX}: a puff of smoke and flame where the lash strikes the ground.
     * Spawned server-side so every nearby client sees the crack, not just the whipping player.
     */
    private static void whipFX(Level level, net.minecraft.core.BlockPos pos) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.22D;
        double z = pos.getZ() + 0.5D;
        double r = 0.27D;
        double[][] offsets = { { -r, 0.0D }, { r, 0.0D }, { 0.0D, -r }, { 0.0D, r }, { 0.0D, 0.0D } };
        for (double[] o : offsets) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE, x + o[0], y, z + o[1],
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, x + o[0], y, z + o[1],
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /**
     * Toggles a tamed creature between following its owner and staying put, and tells the owner which it
     * is now doing. This is the generic command every tamed Mo'Creature answers to — the port gives every
     * tamed creature a follow-the-owner AI ({@code MoCFollowOwnerGoal}), so every one of them needs a way
     * to be told to wait somewhere. A creature that is currently being ridden or carried is left alone.
     */
    private static void toggleStay(Level level, MoCAnimal moc, @org.jspecify.annotations.Nullable Player player) {
        if (moc.isVehicle() || moc.isPassenger()) {
            return;
        }
        moc.setSitting(!moc.isSitting());
        moc.getNavigation().stop();
        feedback(level, moc);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    moc.isSitting() ? "message.mocreatures.staying" : "message.mocreatures.following",
                    moc.getDisplayName()));
        }
    }

    /**
     * The mount commands: a crack while the creature is being RIDDEN urges it on rather than ordering it
     * about (legacy {@code MoCItemWhip}). A ridden ostrich and elephant break into a sprint/charge, and a
     * ridden horse surges forward — a nightmare with a fiery burst. Runs server-side only.
     */
    private static void applyRiddenEffect(MoCAnimal moc) {
        if (moc instanceof MoCEntityOstrich ostrich) {
            ostrich.whipSprint();
        } else if (moc instanceof MoCEntityElephant elephant) {
            elephant.whipCharge();
        } else if (moc instanceof MoCEntityHorse horse) {
            horse.whipCrack();
        }
    }

    /**
     * The legacy cosmetic flourish that goes with a stay order for the two species that had one: a horse
     * told to wait drops its head to graze ({@code setEating}) and an ostrich buries its head in the sand
     * ({@code setHiding}) — both legacy {@code MoCItemWhip} branches for an unridden, tamed animal, now
     * driven by the stay state rather than toggled independently of it.
     */
    private static void applyStayPose(MoCAnimal moc, boolean staying) {
        if (moc instanceof MoCEntityHorse horse) {
            horse.setEating(staying);
        } else if (moc instanceof MoCEntityOstrich ostrich) {
            ostrich.setHiding(staying);
        }
    }

    /**
     * Dispatch one crack to a creature.
     *
     * <p>Legacy {@code MoCItemWhip} branched on six species (BigCat, Horse, Kitty, Wyvern, Ostrich,
     * Elephant) and left every other tamed creature untouched, because legacy pets had no follow-owner AI
     * to switch off — they simply wandered. This port gives every tamed creature
     * {@link drzhark.mocreatures.entity.MoCFollowOwnerGoal}, so the sit/stay toggle that legacy reserved
     * for the big cat now applies to every tamed Mo'Creature (bunny, goat, bear, fox, deer, turkey,
     * komodo, elephant, horse, ...). Without it a tamed bunny could never be told to wait.</p>
     *
     * <p>The one legacy refusal kept verbatim: a wyvern ignores the whip in flight, because it cannot sit
     * mid-air. (The legacy kitty gate {@code getKittyState() > 2 && whipeable()} excluded only the
     * pre-adoption states and "upset"; the port's redesigned four-state model has no equivalent of either,
     * so a tamed kitty simply obeys.)</p>
     */
    private static void command(Level level, MoCAnimal moc, @org.jspecify.annotations.Nullable Player player) {
        if (moc.isVehicle()) {
            applyRiddenEffect(moc); // ridden: urge the mount on instead of ordering it to wait
            return;
        }
        if (moc.isPassenger() || !moc.getIsTamed()) {
            return; // being carried, or wild: there is no pet to order about
        }
        if (moc instanceof MoCEntityWyvern wyvern && (wyvern.isWyvernFlying() || !wyvern.onGround())) {
            return; // a wyvern in flight ignores the crack (legacy: it cannot sit mid-air)
        }
        toggleStay(level, moc, player);
        applyStayPose(moc, moc.isSitting());
    }

    /**
     * Legacy {@code MoCItemWhip.onItemUse}:45 only let the whip crack when the lash could actually reach
     * the ground in front of you: not the underside of a block, the clicked block solid, the space above it
     * clear, and neither block a sign. Anything else was a complete no-op — no sound, no particles, and no
     * durability spent.
     */
    private static boolean canCrackAt(Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.core.Direction face) {
        if (face == net.minecraft.core.Direction.DOWN) {
            return false;
        }
        net.minecraft.world.level.block.state.BlockState clicked = level.getBlockState(pos);
        net.minecraft.world.level.block.state.BlockState above = level.getBlockState(pos.above());
        if (clicked.isAir() || !above.isAir()) {
            return false;
        }
        return !(clicked.getBlock() instanceof net.minecraft.world.level.block.SignBlock)
                && !(above.getBlock() instanceof net.minecraft.world.level.block.SignBlock);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!canCrackAt(level, context.getClickedPos(), context.getClickedFace())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            AABB box = player.getBoundingBox().inflate(12.0D);
            for (MoCAnimal moc : level.getEntitiesOfClass(MoCAnimal.class, box, m -> commandable(player, m))) {
                command(level, moc, null); // area crack: no per-creature chat spam
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
            whipFX(level, context.getClickedPos());
            // Legacy: a deep, quiet crack played at the PLAYER — volume 0.5, pitch 0.4/((rand*0.4)+0.8),
            // i.e. randomised between 0.33 and 0.5, not the full-volume 1.0/1.0 of an ordinary item sound.
            level.playSound(null, player.getX(), player.getY(), player.getZ(), MoCSounds.WHIP.get(),
                    SoundSource.PLAYERS, 0.5F, 0.4F / ((player.getRandom().nextFloat() * 0.4F) + 0.8F));
            // Legacy MoCItemWhip: each crack wears the whip by 1 durability.
            context.getItemInHand().hurtAndBreak(1, player,
                    context.getHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof MoCAnimal moc && commandable(player, moc)) {
            if (!player.level().isClientSide()) {
                // Legacy kept the crack sound and the durability cost in the block-crack path only: every
                // per-species right-click branch (MoCEntityBigCat:691, MoCEntityHorse:1848, MoCEntityKitty:697,
                // MoCEntityOstrich:710, MoCEntityWyvern:357) applied its effect silently and for free. So
                // commanding one creature at a time does not burn through the whip's 24 uses.
                command(player.level(), moc, player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
