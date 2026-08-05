package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityTurtle}. A small, tameable turtle that can hide in its shell.
 */
public class MoCEntityTurtle extends MoCAnimal {

    public MoCEntityTurtle(EntityType<? extends MoCEntityTurtle> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    // Legacy defence: a turtle can pull into its shell (HIDING) and, when struck, may flip onto its back
    // (UPSIDE_DOWN) — during which it is immobile and vulnerable — before righting itself after a while.
    private static final EntityDataAccessor<Boolean> UPSIDE_DOWN =
            SynchedEntityData.defineId(MoCEntityTurtle.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HIDING =
            SynchedEntityData.defineId(MoCEntityTurtle.class, EntityDataSerializers.BOOLEAN);
    /** Ticks elapsed since the flip began (0..{@link #FLIP_DURATION}); drives the smooth mid-air roll. */
    private static final EntityDataAccessor<Integer> FLIP_TICKS =
            SynchedEntityData.defineId(MoCEntityTurtle.class, EntityDataSerializers.INT);
    /** How long the flip animation takes to complete — roughly the knockback airtime. */
    public static final int FLIP_DURATION = 10;
    /** Accumulates while flipped; once past a small random threshold the turtle rights itself. */
    private int flopcounter;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(UPSIDE_DOWN, false);
        builder.define(HIDING, false);
        builder.define(FLIP_TICKS, 0);
    }

    /** Flip animation progress 0..1: 0 = upright, 1 = fully on its back. */
    public float getFlipProgress() {
        return Math.min(1.0F, this.entityData.get(FLIP_TICKS) / (float) FLIP_DURATION);
    }

    public boolean getIsUpsideDown() {
        return this.entityData.get(UPSIDE_DOWN);
    }

    public void setIsUpsideDown(boolean flip) {
        this.entityData.set(UPSIDE_DOWN, flip);
    }

    public boolean getIsHiding() {
        return this.entityData.get(HIDING);
    }

    public void setIsHiding(boolean hide) {
        this.entityData.set(HIDING, hide);
    }

    /** Legacy {@code flipflop}: set/clear the upside-down state, stop hiding, and cancel any path. */
    private void flipflop(boolean flip) {
        setIsUpsideDown(flip);
        setIsHiding(false);
        this.flopcounter = 0;
        this.entityData.set(FLIP_TICKS, 0); // restart the roll animation
        this.getNavigation().stop();
    }

    @Override
    public Identifier getTexture() {
        // Legacy TMNT easter egg: a turtle NAMED one of the four Teenage Mutant Ninja Turtles wears a
        // special ninja-turtle shell texture. All four textures ship in resources
        // (turtled/turtlel/turtler/turtlem.png), so the swap is honoured here.
        if (hasCustomName() && getCustomName() != null) {
            String name = getCustomName().getString().toLowerCase(java.util.Locale.ROOT).trim();
            switch (name) {
                case "donatello" -> {
                    return modelTexture("turtled.png");
                }
                case "leonardo" -> {
                    return modelTexture("turtlel.png");
                }
                case "rafael", "raphael" -> {
                    return modelTexture("turtler.png");
                }
                case "michelangelo", "michaelangelo" -> {
                    return modelTexture("turtlem.png");
                }
                default -> {
                    // fall through to the normal turtle texture
                }
            }
        }
        return modelTexture("turtle.png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.TURTLEHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.TURTLEDYING.get();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        // While hiding in its shell, a blow is deflected entirely and the turtle may flip (1/10) onto its back.
        if (getIsHiding()) {
            if (level.getRandom().nextInt(10) == 0) {
                flipflop(true);
            }
            return false;
        }
        // Not hiding: the full blow lands (legacy applies no damage reduction here).
        boolean hurt = super.hurtServer(level, damageSource, amount);
        // A struck turtle has a 1/3 chance to be knocked onto its back (legacy); otherwise it snaps
        // into its shell. Either way, being hit provokes a defensive reaction (the user expected this).
        if (hurt && !getIsUpsideDown()) {
            if (level.getRandom().nextInt(3) == 0) {
                flipflop(true);
            } else if (!getIsHiding()) {
                setIsHiding(true);
            }
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        // Legacy Mo'Creatures has no turtle egg at all — turtles neither lay nor drop one, and the egg-meta
        // table (1-54) has no turtle entry. The passive egg-laying that used to be here was a port invention.

        if (this.level() instanceof ServerLevel sl) {
            // Advance the flip roll while upside-down so the renderer can play a smooth mid-air tumble.
            int ft = this.entityData.get(FLIP_TICKS);
            if (getIsUpsideDown()) {
                if (ft < FLIP_DURATION) {
                    this.entityData.set(FLIP_TICKS, ft + 1);
                }
            } else if (ft != 0) {
                this.entityData.set(FLIP_TICKS, 0);
            }

            if (getIsUpsideDown()) {
                // Flailing: an upside-down turtle rights itself after a short random while.
                if (!this.isPassenger() && sl.getRandom().nextInt(20) == 0
                        && ++this.flopcounter > sl.getRandom().nextInt(3) + 8) {
                    flipflop(false);
                    sl.playSound(null, blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
            } else if (!getIsTamed() && !tryGroundEat(sl)) {
                // A shy wild turtle pulls into its shell (hissing the first time) when ANY non-turtle
                // living entity that is >=0.5 wide OR tall and visible comes within 4 blocks — players
                // AND passive animals (legacy getBoogey(4D) + entitiesToInclude + canEntityBeSeen). A
                // creative/spectator player is exempted (port QoL); it emerges once the coast is clear.
                boolean threat = false;
                for (net.minecraft.world.entity.LivingEntity le : sl.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class, this.getBoundingBox().inflate(4.0D),
                        e -> !(e instanceof MoCEntityTurtle)
                                && (e.getBbWidth() >= 0.5F || e.getBbHeight() >= 0.5F)
                                && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                                && this.hasLineOfSight(e))) {
                    threat = true;
                    break;
                }
                if (threat) {
                    if (!getIsHiding()) {
                        sl.playSound(null, blockPosition(), MoCSounds.TURTLEHISSING.get(), SoundSource.NEUTRAL,
                                1.0F, ((sl.getRandom().nextFloat() - sl.getRandom().nextFloat()) * 0.2F) + 1.0F);
                        setIsHiding(true);
                    }
                    this.getNavigation().stop();
                } else if (getIsHiding()) {
                    setIsHiding(false);
                }
            }
        }
    }

    @Override
    public void travel(Vec3 input) {
        // Immobile while flipped onto its back or hidden in its shell (legacy isMovementCeased).
        super.travel(getIsUpsideDown() || getIsHiding() ? Vec3.ZERO : input);
    }

    /**
     * Legacy ground-eat taming (legacy {@code onLivingUpdate}): a wild turtle waddles to the nearest dropped melon
     * slice or sugar cane within ~10 blocks, eats it once adjacent, and is tamed to the nearest player. Turtles in
     * legacy could ONLY be tamed this way — never by hand-feeding. Returns {@code true} while pursuing or eating
     * food, so the caller skips the shy-hiding behaviour for that tick.
     */
    private boolean tryGroundEat(ServerLevel sl) {
        net.minecraft.world.entity.item.ItemEntity food = null;
        double bestSq = Double.MAX_VALUE;
        for (net.minecraft.world.entity.item.ItemEntity ie : sl.getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class, this.getBoundingBox().inflate(10.0D),
                e -> e.isAlive() && (e.getItem().is(Items.MELON_SLICE) || e.getItem().is(Items.SUGAR_CANE)))) {
            double d = this.distanceToSqr(ie);
            if (d < bestSq) {
                bestSq = d;
                food = ie;
            }
        }
        if (food == null) {
            return false;
        }
        setIsHiding(false);
        if (bestSq > 2.25D) {
            this.getNavigation().moveTo(food, 1.0D);
        } else {
            food.getItem().shrink(1);
            if (food.getItem().isEmpty()) {
                food.discard();
            }
            Player owner = sl.getNearestPlayer(this, 24.0D);
            if (owner != null) {
                setTamed(true);
                setOwnerName(owner.getName().getString());
            }
            setHealth(getMaxHealth());
            sl.playSound(null, blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Right-click an upside-down turtle to flip it back over (legacy: owners right their tamed turtle).
        if (getIsUpsideDown()) {
            if (!this.level().isClientSide()) {
                flipflop(false);
            }
            return InteractionResult.SUCCESS;
        }
        // Legacy: right-clicking an upright WILD (untamed) turtle flips it onto its back — a way to catch/immobilise
        // it (tamed turtles are made by ground-feeding melon/sugar cane, never by hand-feeding).
        if (!getIsTamed()) {
            if (!this.level().isClientSide()) {
                flipflop(true);
                this.level().playSound(null, blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL,
                        1.0F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // Legacy pick-up (mirrors MoCEntityBird/MoCEntityMouse): right-click a tamed turtle empty-handed to
        // carry it on your head (the turtle rides the player); right-click again to set it down with a small
        // hop. The shell/flip defence above still takes priority so a flipped turtle is righted, not carried.
        ItemStack stack = player.getItemInHand(hand);
        if (getIsTamed() && stack.isEmpty()) {
            if (!this.level().isClientSide()) {
                if (this.isPassenger() && this.getVehicle() == player) {
                    // Set down: dismount and hop off in the player's direction of travel (legacy motionY/2 + 0.2).
                    this.stopRiding();
                    Vec3 pv = player.getDeltaMovement();
                    this.setDeltaMovement(pv.x * 5.0D, (pv.y / 2.0D) + 0.2D, pv.z * 5.0D);
                    this.hurtMarked = true; // sync the impulse to clients (26.2 uses hurtMarked, not hasImpulse)
                } else if (!this.isVehicle() && !player.isPassenger()) {
                    // Carry on the player's head (legacy chickenplop on mount).
                    this.setYRot(player.getYRot());
                    this.startRiding(player);
                    this.level().playSound(null, this.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL,
                            1.0F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
}
