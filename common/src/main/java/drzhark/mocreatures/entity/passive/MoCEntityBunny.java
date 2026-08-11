package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityBunny}. A small, tameable rabbit with five colour variants that,
 * once tamed and adult, auto-multiplies up to a world population cap (legacy "rabbits multiply, capped
 * at 12") rather than breeding from a fed item.
 */
public class MoCEntityBunny extends MoCAnimal {

    /** Legacy {@code bunnyLimit}: the world stops auto-multiplying bunnies beyond this population. */
    private static final int BUNNY_LIMIT = 12;

    /**
     * Legacy transient reproduce timers (not persisted): while a tamed adult bunny is not being carried
     * and the world bunny population is within the cap, {@code A} counts up to 1023, then {@code B} up to
     * 127; only when both are full and another eligible adult bunny is within 4 blocks is a baby born.
     * {@code A} starts at a random 0-63 offset so a warren does not reproduce in lock-step (legacy
     * {@code rand.nextInt(64)}).
     */
    private int bunnyReproduceTickerA;
    private int bunnyReproduceTickerB;

    public MoCEntityBunny(EntityType<? extends MoCEntityBunny> type, Level level) {
        super(type, level);
        this.bunnyReproduceTickerA = this.random.nextInt(64);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Legacy bunnies have no item-feed breeding (isBreedingItem == false) and no carrot tempt; they
        // reproduce only via the population-capped auto-multiply in customServerAiStep below.
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
    }

    /**
     * Legacy transient {@code pickedUp} flag, tracked so the landing behaviour fires exactly once on the
     * tick a thrown bunny touches down again (legacy {@code updateEntityActionState}:290-307).
     */
    private boolean wasCarried;

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Legacy onLivingUpdate: a non-adult bunny slowly ages (1/200 per tick) and becomes an adult once
        // its age reaches 100, after which it too may reproduce.
        if (!getIsAdult() && this.random.nextInt(200) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= 100) {
                setAdult(true);
            }
        }
        tickLanding(level);
        reproduce(level);
    }

    /** A carried bunny is set down with the {@code rabbitlift} sound (legacy {@code MoCEntityBunny}:179). */
    @Override
    protected @Nullable SoundEvent getPutDownSound() {
        return MoCSounds.RABBITLIFT.get();
    }

    /**
     * Legacy {@code updateEntityActionState}:290-307 — the thrown-bunny-as-bait trick. The first time a
     * bunny that was being carried touches the ground again it thumps down ({@code rabbitland}) and every
     * hostile mob within 12 blocks turns on it, so a player can lob a bunny to pull a mob off themselves.
     */
    private void tickLanding(ServerLevel level) {
        if (isBeingCarried()) {
            this.wasCarried = true;
            return;
        }
        if (!this.wasCarried || !this.onGround()) {
            return;
        }
        this.wasCarried = false;
        level.playSound(null, blockPosition(), MoCSounds.RABBITLAND.get(), SoundSource.NEUTRAL, 1.0F,
                ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F);
        for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                getBoundingBox().inflate(12.0D),
                m -> m instanceof net.minecraft.world.entity.monster.Enemy && m.isAlive())) {
            mob.setTarget(this);
        }
    }

    /**
     * Legacy {@code MoCEntityBunny.onUpdate} population-capped auto-multiply — the signature "rabbits
     * multiply, capped at 12" mechanic. A tamed adult bunny that is not being carried, while the world
     * bunny population is at or under the cap, fills a long timer ({@code A} to 1023) and then a short one
     * ({@code B} to 127); once both are full and another eligible adult bunny is within 4 blocks, a baby
     * bunny is born and both parents' timers reset. No item feeding is involved (legacy
     * {@code isBreedingItem} returned false).
     */
    private void reproduce(ServerLevel level) {
        if (!getIsTamed() || !getIsAdult() || isBeingCarried() || countBunnies(level) > BUNNY_LIMIT) {
            return;
        }
        if (this.bunnyReproduceTickerA < 1023) {
            this.bunnyReproduceTickerA++;
            return;
        }
        if (this.bunnyReproduceTickerB < 127) {
            this.bunnyReproduceTickerB++;
            return;
        }
        if (countBunnies(level) > BUNNY_LIMIT) {
            resetReproduceTimers();
            return;
        }
        for (MoCEntityBunny partner : level.getEntitiesOfClass(MoCEntityBunny.class,
                this.getBoundingBox().inflate(4.0D, 4.0D, 4.0D))) {
            if (partner == this || partner.isBeingCarried()
                    || partner.bunnyReproduceTickerA < 1023 || !partner.getIsAdult()) {
                continue;
            }
            MoCEntityBunny baby = MoCEntities.BUNNY.get().create(level, EntitySpawnReason.BREEDING);
            if (baby != null) {
                baby.setPos(getX(), getY(), getZ());
                baby.setAdult(false);
                level.addFreshEntity(baby);
                level.playSound(null, this.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL,
                        1.0F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F);
            }
            resetReproduceTimers();
            partner.resetReproduceTimers();
            break;
        }
    }

    /** Counts every bunny currently loaded in this level (legacy {@code World.countEntities(getClass())}). */
    private int countBunnies(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof MoCEntityBunny) {
                count++;
            }
        }
        return count;
    }

    /** Legacy {@code proceed()}: clear the short timer and re-stagger the long timer to a random 0-63 offset. */
    private void resetReproduceTimers() {
        this.bunnyReproduceTickerB = 0;
        this.bunnyReproduceTickerA = this.random.nextInt(64);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        // Legacy MoCEntityBunny overrides fall(float) to an empty body: bunnies take zero fall damage, so a
        // 4-health rabbit is never hurt (or killed) hopping off a ledge.
        return false;
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int k = this.random.nextInt(100);
            if (k <= 20) {
                setTypeMoC(1);
            } else if (k <= 40) {
                setTypeMoC(2);
            } else if (k <= 60) {
                setTypeMoC(3);
            } else if (k <= 80) {
                setTypeMoC(4);
            } else {
                setTypeMoC(5);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("bunnyb.png");
            case 3 -> modelTexture("bunnyc.png");
            case 4 -> modelTexture("bunnyd.png");
            case 5 -> modelTexture("bunnye.png");
            default -> modelTexture("bunny.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.RABBITHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.RABBITDEATH.get();
    }

    /**
     * So bunny-hats don't suffer damage. Mirrors legacy {@code attackEntityFrom}: while the bunny is being
     * carried/worn on the player's head (a passenger, i.e. {@code ridingEntity != null}) it takes no damage.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isBeingCarried()) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }
}
