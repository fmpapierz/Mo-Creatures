package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityBunny}. A small, tameable rabbit with five colour variants that,
 * once tamed and adult, auto-multiplies up to a world population cap (legacy "rabbits multiply, capped
 * at 12") rather than breeding from a fed item.
 *
 * <p>It gets about the way the legacy bunny did: by bounding. A bunny moving over the ground springs up
 * at most every 15 ticks ({@link #tickHop}), and each bound starts a client-side hop clock
 * ({@link #getHopCompletion}) that {@code MoCModelBunny} poses the leap from. The port originally had
 * neither, so its bunnies slid along the ground on the leg gait alone.</p>
 */
public class MoCEntityBunny extends MoCAnimal {

    /** Legacy {@code bunnyLimit}: the world stops auto-multiplying bunnies beyond this population. */
    private static final int BUNNY_LIMIT = 12;

    /**
     * Legacy 12.0.5 hop impulse, {@code motionY = 0.3D} ({@code MoCEntityBunny.onUpdate}:194-198). 26.2
     * keeps 1.12's air physics (move first, then gravity 0.08 and drag 0.98; mc262-ref
     * {@code LivingEntity.travelInAir}:2448-2477), so the arc matches legacy tick for tick: 8 ticks
     * airborne, apex about 0.70 blocks. It is also exactly the vanilla rabbit's hop:
     * {@code Rabbit.getJumpPower}:139-157 asks for {@code 0.3F / 0.42F} of the 0.42 base jump. 5.1.5 bounced
     * with 0.45 on every ground contact and no timer ({@code updateEntityActionState}:282-285), roughly a
     * 1.4-block bound. The 12.0.5 gait is the one kept here.
     */
    private static final double HOP_MOTION_Y = 0.3D;

    /** Legacy {@code jumpTimer = 15}: a moving bunny bounds at most once every 15 ticks (0.75 s). */
    private static final int HOP_INTERVAL_TICKS = 15;

    /**
     * Legacy's "is it moving" test, kept as written: either horizontal axis of the motion faster than 0.05
     * blocks/tick. {@code customServerAiStep} runs before this tick's {@code travel}, so it sees the
     * post-friction velocity the previous tick left behind. That is the same value legacy read at the end
     * of {@code onUpdate}. A strolling bunny carries about 0.11 blocks/tick (speed 0.30, squared through
     * {@code moveRelative}, times ground friction 0.546), and a panicking one about 0.24, so both bound.
     * A bunny standing still does not, and neither does one pressed straight into a wall: the collision
     * zeroes the blocked axis (mc262-ref {@code Entity.restituteMovementAfterCollisions}:803-813, bounciness
     * 0).
     */
    private static final double HOP_MIN_SPEED = 0.05D;

    /**
     * Length of the client hop clock. It runs longer than the 8-tick server arc because the client
     * replays that arc through 3-step position interpolation (mc262-ref
     * {@code InterpolationHandler.DEFAULT_INTERPOLATION_STEPS}:11) fed by updates at most 3 ticks apart.
     * The renderer then draws it a further tick late (see {@link #handleEntityEvent}). Traced for each of
     * the three possible update alignments, the drawn bound peaks 6-9 ticks after the hop event and lands
     * after 12-15. The spread depends on whether the take-off position arrives with the event or a tick
     * after it. A 12-tick clock started a tick late therefore spans the drawn flight: mid-flight falls at
     * tick 7 and the end at tick 13. The vanilla rabbit uses the same device, with 15 ticks for its own
     * animation ({@code Rabbit.JUMP_DURATION_IN_TICKS}:92).
     */
    private static final int HOP_ANIMATION_TICKS = 12;

    /**
     * Legacy transient reproduce timers (not persisted): while a tamed adult bunny is not being carried
     * and the world bunny population is within the cap, {@code A} counts up to 1023, then {@code B} up to
     * 127; only when both are full and another eligible adult bunny is within 4 blocks is a baby born.
     * {@code A} starts at a random 0-63 offset so a warren does not reproduce in lock-step (legacy
     * {@code rand.nextInt(64)}).
     */
    private int bunnyReproduceTickerA;
    private int bunnyReproduceTickerB;

    /**
     * Legacy {@code jumpTimer}, server-side and transient like legacy's. It counts down every tick and a
     * bound needs it at zero. Unlike legacy's {@code --jumpTimer} it stops at zero instead of running
     * negative forever.
     */
    private int hopCooldown;

    /**
     * Client hop clock, the vanilla rabbit's {@code jumpTicks}/{@code jumpDuration} pair
     * ({@code Rabbit}:105-106, advanced in {@code aiStep}:280-290). The server's hop broadcast starts it,
     * it is never saved or synched, and nothing on the server reads it. {@code hopDuration == 0} means the
     * bunny is not mid-bound.
     */
    private int hopTicks;
    private int hopDuration;

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
        tickHop(level);
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
     * The bunny's bounding gait, legacy 12.0.5 {@code MoCEntityBunny.onUpdate}:194-198, which the port had
     * dropped. The cooldown runs down every tick. Once it is out, a bunny on the ground moving faster than
     * {@link #HOP_MIN_SPEED} on either axis springs up at {@link #HOP_MOTION_Y}, keeping its horizontal
     * velocity, and then waits {@link #HOP_INTERVAL_TICKS} before the next bound. An idle bunny never hops.
     *
     * <p>Only the vertical component is set, so the bound never fights the pathfinder. Steering stays with
     * {@code MoveControl}, and the bunny coasts about a block through the air on its own momentum (air drag
     * 0.91, air control 0.02; mc262-ref {@code LivingEntity.travelInAir}:2448-2477,
     * {@code getFlyingSpeed}:2708-2710). {@code GroundPathNavigation} only follows its path on the ground
     * ({@code canUpdatePath}:34-36), and mid-air it advances a node the bunny passes over
     * ({@code PathNavigation.tick}:225-231). On landing it accepts a node within 1.0 block vertically
     * (:250-254, :419-421). Its stuck check wants 2.25 blocks per 100 ticks (:292-298), and a bounding
     * stroller covers about ten. A step up still gets the normal 0.42 jump, because
     * {@code jumpFromGround} takes {@code max(jumpPower, motion.y)} (mc262-ref {@code LivingEntity}:2369-2381).</p>
     *
     * <p><b>Why the client sees it.</b> A remote mob is never simulated on the client. Its {@code aiStep}
     * only replays interpolated position packets (mc262-ref {@code LivingEntity}:3019-3023, with
     * {@code travel} gated at :3108-3112), and a velocity packet just stores the vector
     * ({@code ClientPacketListener.handleSetEntityMotion}:622-628). So the POSITION sync is what makes a
     * server-side bound visible. {@code needsSync} is vanilla's flag for that: {@code jumpFromGround} sets it
     * ({@code LivingEntity}:2379), and so does the slime's hop ({@code AbstractCubeMob.jumpFromGround}:263-267).
     * It makes the very next tracker pass ship position and motion at once, instead of waiting out the
     * rest of the 3-tick update interval ({@code ServerEntity}:123, :175). That pass runs at the start of
     * the following server tick, ahead of the entity loop ({@code ServerLevel.tick}:400 vs :418-445).
     * Because take-off flips {@code onGround}, the sync is an absolute one (:151-159). {@code hurtMarked}
     * would only send the velocity (:223-226), which the client stores but never moves a remote bunny by.</p>
     *
     * <p>The broadcast is vanilla's {@code EntityEvent.JUMP}, the id the rabbit uses for the same purpose
     * ({@code Rabbit.jumpFromGround}:171-173, {@code handleEntityEvent}:443-452). No superclass of the
     * bunny handles id 1, and it starts the client hop clock that poses the model.</p>
     */
    private void tickHop(ServerLevel level) {
        if (this.hopCooldown > 0) {
            this.hopCooldown--;
        }
        if (this.hopCooldown > 0 || !canHop()) {
            return;
        }
        Vec3 motion = getDeltaMovement();
        if (Math.abs(motion.x) <= HOP_MIN_SPEED && Math.abs(motion.z) <= HOP_MIN_SPEED) {
            return;
        }
        setDeltaMovement(motion.x, HOP_MOTION_Y, motion.z);
        this.needsSync = true;
        this.hopCooldown = HOP_INTERVAL_TICKS;
        level.broadcastEntityEvent(this, EntityEvent.JUMP);
    }

    /**
     * Whether the bunny may take off. Legacy's test was only {@code onGround}. A legacy bunny riding its
     * carrier was never on the ground, but this port's carried bunny is not a passenger (see
     * {@code MoCAnimal.CARRIER}), so the carry is excluded explicitly rather than trusted to
     * {@code onGround}. The other gates make the bunny sit still when told to (the whip's
     * {@link #isSitting}), keep it from bouncing in a boat or cart, and keep it from bounding along a
     * shallow river bed or lava pool. They also stop a NoGravity bunny from launching itself into the sky,
     * since with no gravity a 0.3 impulse only decays by the 0.98 drag.
     */
    private boolean canHop() {
        return this.onGround() && !isBeingCarried() && !isSitting() && !isPassenger()
                && !isInLiquid() && !isNoGravity();
    }

    /**
     * Client end of the hop broadcast: restart the hop clock (vanilla {@code Rabbit.handleEntityEvent}:443-452).
     * The clock starts one tick behind the rabbit's, at -1, because the drawn bound trails the event.
     * The renderer draws the bunny at {@code lerp(partialTick, yOld, y)}, a tick behind its own tick
     * state (mc262-ref {@code EntityRenderer.extractRenderState}:166-167). The take-off position also
     * leaves the server at the next tracker pass, up to a tick after this event (see {@link #tickHop}).
     * Posing the real model against the drawn arc, for every update alignment and either arrival order:
     * a clock counted from 0 put the leap pose up to 1.8 px into the ground under a bunny still drawn
     * standing. Starting at -1 keeps it within 0.9 px of the plain walk gait's own ground contact; -2
     * does worse (1.2 px).
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == EntityEvent.JUMP) {
            this.hopTicks = -1;
            this.hopDuration = HOP_ANIMATION_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** Advances the hop clock once per tick, as vanilla {@code Rabbit.aiStep}:280-290 does. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.hopDuration > 0 && ++this.hopTicks >= this.hopDuration) {
            this.hopTicks = 0;
            this.hopDuration = 0;
        }
    }

    /**
     * How far through its current bound the bunny is, 0..1, interpolated to the frame. It is 0 when the
     * bunny is not hopping, and it is always 0 while the bunny is carried or riding, even if it was picked
     * up mid-bound. This is the vanilla {@code Rabbit.getJumpCompletion}:176-178 that
     * {@code RabbitRenderer.extractRenderState} copies into {@code RabbitRenderState.jumpCompletion}.
     * {@code MoCMobRenderer} copies it into {@code MoCEntityRenderState.bunnyHop} the same way.
     */
    public float getHopCompletion(float partialTick) {
        if (this.hopDuration == 0 || isBeingCarried() || isPassenger()) {
            return 0.0F;
        }
        return Mth.clamp((this.hopTicks + partialTick) / this.hopDuration, 0.0F, 1.0F);
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
