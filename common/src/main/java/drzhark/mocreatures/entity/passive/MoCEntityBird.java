package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityBird}. A small bird with six colour variants.
 */
public class MoCEntityBird extends MoCAnimal {

    public MoCEntityBird(EntityType<? extends MoCEntityBird> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.6D);
    }

    // Two-stage seed taming (legacy): a wild bird first pecks seeds scattered on the ground (becoming
    // pre-tamed / approachable), after which hand-feeding it seeds tames and names it.
    private static final EntityDataAccessor<Boolean> PRE_TAMED =
            SynchedEntityData.defineId(MoCEntityBird.class, EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PRE_TAMED, false);
    }

    public boolean getPreTamed() {
        return this.entityData.get(PRE_TAMED);
    }

    public void setPreTamed(boolean pre) {
        this.entityData.set(PRE_TAMED, pre);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Startle and flee (upward, since it flies) from monsters (legacy flee-to-treetop).
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 1.2D, 1.5D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Legacy parachute (updateEntityActionState): while perched on a player, negate the carrier's fall
        // damage and clamp their downward velocity to -0.1 so the bird slows their descent every tick.
        if (getCarrier() instanceof Player p) {
            p.resetFallDistance();
            Vec3 pv = p.getDeltaMovement();
            if (pv.y < -0.1D) {
                p.setDeltaMovement(pv.x, -0.1D, pv.z);
            }
        }
        // Stage 1: an untamed wild bird hops to and pecks seeds scattered on the ground to become pre-tamed.
        if (!getPreTamed() && !getIsTamed()) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(8.0D),
                    it -> it.getItem().is(Items.WHEAT_SEEDS) && it.isAlive())) {
                // Legacy MoCEntityBird:456-465 required the bird to be within 1 block AND a 1-in-50 roll to
                // fire, making stage 1 a slow, repeated-approach courtship rather than a one-touch flag flip.
                if (this.distanceToSqr(item) < 1.0D && this.random.nextInt(50) == 0) {
                    item.getItem().shrink(1);
                    if (item.getItem().isEmpty()) {
                        item.discard();
                    }
                    setPreTamed(true);
                    level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5D, getZ(), 3, 0.2D, 0.2D, 0.2D, 0.0D);
                } else {
                    this.getNavigation().moveTo(item, 1.0D);
                }
                break;
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.WHEAT_SEEDS)) {
            // Stage 2: hand-feed a pre-tamed bird to tame + name it.
            if (!getIsTamed() && getPreTamed()) {
                if (!this.level().isClientSide()) {
                    // Legacy tameWithName enforced the per-player pet cap on every taming path.
                    if (exceedsTameCap(player)) {
                        return InteractionResult.SUCCESS;
                    }
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    setTamed(true);
                    setOwnerName(player.getName().getString());
                    // Legacy tameWithName prompted for a name the instant a creature was tamed.
                    drzhark.mocreatures.network.MoCNetwork.promptName(this, player);
                    // Legacy MoCEntityBird:342-353 does NOT heal on the stage-2 tame.
                }
                return InteractionResult.SUCCESS;
            }
            // Legacy heal food branch: feeding a tamed bird a seed always consumes one seed and fully heals it
            // (even at full health), playing an eating sound.
            if (getIsTamed()) {
                if (!this.level().isClientSide()) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    heal(getMaxHealth());
                    this.level().playSound(null, this.blockPosition(), MoCSounds.EATING.get(), SoundSource.NEUTRAL,
                            1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
        }
        // Legacy pick-up: right-click a tamed bird with any non-seed item (or empty hand) to perch it on your
        // head (bird rides the player); right-click again to toss it off with a small forward/upward impulse.
        if (getIsTamed() && !stack.is(Items.WHEAT_SEEDS)) {
            if (!this.level().isClientSide()) {
                // Shared carry toggle: perch on the carrier's head, or toss off with the legacy 5x impulse.
                // The bird is already tamed to reach this branch, so picking it up never tames.
                toggleCarry(player, false);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Legacy {@code setDead} guard: a tamed bird with remaining health refuses to die. Faithfully adapted
     * to 26.2 by clamping any would-be lethal damage so a tamed bird is always left with a sliver of health
     * (its owner must un-tame or otherwise remove it rather than have it killed outright).
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // A tamed bird that still has health cannot be killed outright — clamp lethal damage to leave 1 HP.
        if (getIsTamed() && getHealth() > 0.0F) {
            float lethal = getHealth() - 1.0F;
            if (amount >= lethal) {
                amount = Math.max(0.0F, lethal);
            }
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 15) {
                setTypeMoC(1);
            } else if (i <= 30) {
                setTypeMoC(2);
            } else if (i <= 45) {
                setTypeMoC(3);
            } else if (i <= 60) {
                setTypeMoC(4);
            } else if (i <= 75) {
                setTypeMoC(5);
            } else if (i <= 90) {
                setTypeMoC(6);
            } else {
                setTypeMoC(2);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 1 -> modelTexture("birdwhite.png");
            case 2 -> modelTexture("birdblack.png");
            case 3 -> modelTexture("birdgreen.png");
            case 5 -> modelTexture("birdyellow.png");
            case 6 -> modelTexture("birdred.png");
            default -> modelTexture("birdblue.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return switch (getTypeMoC()) {
            case 1 -> MoCSounds.BIRDWHITE.get();
            case 2 -> MoCSounds.BIRDBLACK.get();
            case 3 -> MoCSounds.BIRDGREEN.get();
            case 4 -> MoCSounds.BIRDBLUE.get();
            case 5 -> MoCSounds.BIRDYELLOW.get();
            default -> MoCSounds.BIRDRED.get();
        };
    }

    // Legacy referenced dedicated "birdhurt"/"birddying" sounds, but those OGGs never shipped (not in the
    // 12.0.5 jar), so we reuse the bird's own chirp — a hurt/dying bird calls out rather than falling silent.
    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return getAmbientSound();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return getAmbientSound();
    }
}
