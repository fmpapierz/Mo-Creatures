package drzhark.mocreatures.entity.passive;

import java.util.List;

import drzhark.mocreatures.entity.IMoCEntity;
import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.network.MoCNetwork;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityRaccoon} (1.12.2 {@code entity/passive/MoCEntityRaccoon.java}).
 *
 * <p>The raccoon is a small, single-coat ({@code raccoon.png}) tameable animal with a two-sided
 * temperament that is entirely keyed off adulthood, because legacy
 * {@code MoCEntityRaccoon.isNotScared()} (line 156) simply returns {@code getIsAdult()}:</p>
 * <ul>
 *   <li><b>Kits</b> (non-adults) are timid. {@code EntityAIFleeFromPlayer(this, 1.0D, 4D)} (legacy line 48)
 *       bolts from any player within 4 blocks — the goal short-circuits to {@code false} for anything that
 *       is {@code isNotScared()}, so only kits ever run. They also trail the nearest grown raccoon
 *       ({@code EntityAIFollowAdult}, legacy line 50) and never retaliate when struck.</li>
 *   <li><b>Adults</b> are opportunistic little predators. {@code EntityAIHunt(this, EntityAnimal.class, true)}
 *       (legacy line 54) lets a grown raccoon stalk and bite any animal it out-sizes, and
 *       {@code attackEntityFrom} (legacy lines 66-80) makes it round on whatever hurt it.</li>
 * </ul>
 *
 * <p>Legacy fed it through {@code MoCEntityTameableAnimal}, so it is tamed by hand-feeding — and unusually,
 * by hand-feeding <em>anything edible</em> ({@code MoCTools.isItemEdible}: any food item, any seed, plus
 * wheat / sugar / cake / egg), not a short species-specific list. That widening is reproduced in
 * {@link #mobInteract} the same way {@link MoCEntityGoat} reproduces its own.</p>
 */
public class MoCEntityRaccoon extends MoCAnimal {

    /**
     * Legacy {@code MoCEntityAnimal.huntingCounter} (1.12.2 {@code MoCEntityAnimal}:338-344, 1154-1162).
     *
     * <p>Mo'Creatures predators do not hunt continuously: once every ~500 ticks a creature that
     * {@code isReadyToHunt()} latches a hunting window open ({@code huntingCounter = rand.nextInt(30) + 1}),
     * the counter then ticks up each tick and the window shuts once it passes 50 — so a raccoon actually
     * hunts for a 20-49 tick burst roughly every 25 seconds and ignores prey the rest of the time. Without
     * this latch a {@code NearestAttackableTargetGoal} would have the raccoon killing every chicken it walks
     * past, which is emphatically not how the legacy mod played.</p>
     *
     * <p>Transient (like the legacy field, which was never written to NBT): a reloaded raccoon simply starts
     * out of its hunting window.</p>
     */
    private int huntingCounter;

    public MoCEntityRaccoon(EntityType<? extends MoCEntityRaccoon> type, Level level) {
        super(type, level);
    }

    /**
     * Legacy {@code applyEntityAttributes} (lines 57-64): 8 health (4 hearts), 1 attack damage (half a
     * heart per nip) and a brisk 0.3 movement speed — noticeably faster than the 0.25 most Mo'Creatures
     * land animals get, which is what lets a raccoon actually run prey down.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Legacy task 3: EntityAIFleeFromPlayer(this, 1.0D, 4D). The goal begins by returning false for any
        // IMoCEntity that isNotScared(), and the raccoon's isNotScared() == getIsAdult(), so this is a
        // KIT-ONLY flee: a young raccoon that sees a player inside 4 blocks picks a random spot and runs for
        // it at speed 1.0. Adults ignore players entirely (they have the hunt/retaliate goals instead).
        // MoCAnimal's shared PanicGoal already covers "hurt -> run"; this is the proactive avoidance on top.
        this.goalSelector.addGoal(3, new AvoidEntityGoal<Player>(this, Player.class, 4.0F, 1.0D, 1.0D) {
            @Override
            public boolean canUse() {
                return !MoCEntityRaccoon.this.getIsAdult() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !MoCEntityRaccoon.this.getIsAdult() && super.canContinueToUse();
            }
        });

        // Legacy task 4: EntityAIFollowAdult(this, 1.0D) — a kit trails the nearest grown raccoon. Vanilla's
        // FollowParentGoal cannot stand in for this: it gates on the VANILLA baby age (getAge() < 0), and a
        // naturally-spawned Mo'Creatures kit is a full-age vanilla mob that is merely !getIsAdult(), so the
        // vanilla goal would never fire. See FollowAdultGoal below for the faithful reimplementation.
        this.goalSelector.addGoal(4, new FollowAdultGoal(this, 1.0D));

        // Legacy task 5: EntityAIAttackMelee(this, 1.0D, true). MoCAnimal only installs a melee goal for
        // species flagged wildHostile (a raccoon is not one — it does not hunt PLAYERS), so the raccoon needs
        // its own in order to act on the hunt/revenge targets below.
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));

        // Legacy attackEntityFrom (lines 66-80): a struck raccoon rounds on its attacker, but ONLY when
        //   entity != this                       -> vanilla HurtByTargetGoal already refuses self-damage
        //   isNotScared()                        -> getIsAdult(): kits never fight back, they flee
        //   super.shouldAttackPlayers()          -> !getIsTamed() && difficulty != PEACEFUL
        // and it returns early without targeting when the attacker is riding it / being ridden by it.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!MoCEntityRaccoon.this.getIsAdult() || MoCEntityRaccoon.this.getIsTamed()
                        || MoCEntityRaccoon.this.level().getDifficulty() == Difficulty.PEACEFUL) {
                    return false;
                }
                // Legacy isRidingOrBeingRiddenBy(entity) bail-out: never turn on a passenger or a mount.
                Entity attacker = MoCEntityRaccoon.this.getLastHurtByMob();
                if (attacker != null && attacker.hasIndirectPassenger(MoCEntityRaccoon.this)) {
                    return false;
                }
                if (attacker != null && MoCEntityRaccoon.this.hasIndirectPassenger(attacker)) {
                    return false;
                }
                return super.canUse();
            }
        });

        // Legacy targetTask 1: EntityAIHunt(this, EntityAnimal.class, true) — a checkSight hunt against
        // vanilla EntityAnimal (i.e. other passive animals, never players and never hostiles). The stack of
        // gates it inherits is reproduced in the selector below:
        //   EntityAIHunt.shouldExecute              -> getIsHunting()          (the 20-49 tick burst latch)
        //   EntityAINearestAttackableTargetMoC:88   -> !isMovementCeased() && isNotScared()
        //   EntitiAITargetMoC.isSuitableTarget:67   -> canAttackTarget(target)
        //   EntitiAITargetMoC.isSuitableTarget:72   -> a TAMED raccoon never attacks another tamed pet
        // and MoCEntityRaccoon.canAttackTarget (line 161) additionally excludes other raccoons.
        // chance 10 / checkSight true / onlyNearby false mirror the legacy EntityAIHunt defaults.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Animal.class, 10, true, false,
                (living, serverLevel) -> isHunting()
                        && isReadyToHunt()
                        && !(living instanceof MoCEntityRaccoon)
                        && canAttackTarget(living)
                        && !(getIsTamed() && living instanceof IMoCEntity prey && prey.getIsTamed())));
    }

    /**
     * Legacy {@code MoCEntityAnimal.canAttackTarget} (1.12.2 {@code MoCEntityAnimal}:1116): a Mo'Creatures
     * animal only hunts prey it out-sizes — its own height AND width must be greater than or equal to the
     * target's. That is the whole reason a raccoon eats chickens and bunnies but leaves cows alone.
     * {@code MoCEntityRaccoon.canAttackTarget} (line 161) adds the "never another raccoon" clause, which is
     * applied in the target selector.
     */
    private boolean canAttackTarget(net.minecraft.world.entity.LivingEntity target) {
        return this.getBbHeight() >= target.getBbHeight() && this.getBbWidth() >= target.getBbWidth();
    }

    /**
     * Legacy {@code MoCEntityRaccoon.isReadyToHunt} (line 166): {@code getIsAdult() && !isMovementCeased()}.
     * {@code isMovementCeased()} is {@code getIsSitting() || isBeingRidden()} on the legacy base
     * ({@code MoCEntityAnimal}:1150).
     */
    private boolean isReadyToHunt() {
        return getIsAdult() && !isSitting() && !isVehicle();
    }

    /** Whether the legacy hunting burst window is currently open — see {@link #huntingCounter}. */
    public boolean isHunting() {
        return this.huntingCounter != 0;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level); // MoCBehavior.tickGrowth handles the legacy edad++ growth curve

        // Legacy MoCEntityAnimal.onLivingUpdate:338-344, the hunting-burst latch, including its leading
        // `MoCreatures.proxy.enableHunters` gate (default TRUE): with hunters disabled the window never
        // opens, and the burst is the only thing that arms the hunt target goal, so the raccoon takes no prey.
        if (drzhark.mocreatures.config.MoCConfig.get().enableHunters
                && isReadyToHunt() && !isHunting() && this.random.nextInt(500) == 0) {
            this.huntingCounter = this.random.nextInt(30) + 1;
        }
        if (isHunting() && ++this.huntingCounter > 50) {
            this.huntingCounter = 0;
            // Legacy left the acquired target alone when the window shut (the target goal simply stopped
            // re-acquiring), so the raccoon finishes the chase it started rather than forgetting mid-pounce.
        }
    }

    /**
     * Legacy {@code MoCEntityAnimal.onKillEntity} (1.12.2 {@code MoCEntityAnimal}:1173-1177): killing a
     * NON-player wiped the fresh loot around the corpse ({@code MoCTools.destroyDrops(this, 3D)} — item
     * entities younger than 50 ticks within 3 blocks), so predators could not be penned up as a free
     * drop farm. Mirrors {@link MoCEntityFox#doHurtTarget}; gated on the {@code destroyDrops} config flag.
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && !(target instanceof Player) && drzhark.mocreatures.config.MoCConfig.get().destroyDrops) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(3.0D))) {
                if (item.isAlive() && item.tickCount < 50) {
                    item.discard();
                }
            }
        }
        return hit;
    }

    /**
     * Legacy {@code processInteract} (lines 82-110). Hand it ANY edible item and it is consumed, the raccoon
     * is tamed and named ({@code MoCTools.tameWithName}), its health is set to maximum, and — if it is still
     * a kit below age 100 — it grows one step. That is the same shape as {@link MoCAnimal}'s data-driven
     * {@code Tame.FEED} branch; the only thing that needs overriding is the breadth of "edible", because
     * legacy tested {@code MoCTools.isItemEdible} rather than a fixed food list.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isRaccoonEdible(stack)) {
            if (!this.level().isClientSide()) {
                if (!getIsTamed()) {
                    // Enforce the tamed-per-player cap; refuse without consuming the food.
                    if (exceedsTameCap(player)) {
                        return InteractionResult.SUCCESS;
                    }
                    setTamed(true);
                    setOwnerName(player.getName().getString());
                    // Legacy tameWithName prompted for a name the instant a creature was tamed.
                    MoCNetwork.promptName(this, player);
                    spawnHearts(7);
                } else {
                    spawnHearts(4);
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                // Legacy setHealth(getMaxHealth()) ran unconditionally, even at full health, and the food was
                // consumed either way — so feeding an already-tamed, healthy raccoon is a (wasteful) no-op
                // rather than falling through to vanilla love mode.
                heal(getMaxHealth());
                // Legacy: !getIsAdult() && getEdad() < 100 -> setEdad(getEdad() + 1). Feeding nudges a kit
                // along its growth curve; it does NOT snap it to adulthood.
                if (!getIsAdult() && getMoCAge() < 100) {
                    setMoCAge(getMoCAge() + 1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Legacy {@code MoCTools.isItemEdible} (1.12.2 {@code MoCTools}:1696-1699):
     * {@code (item instanceof ItemFood) || (item instanceof ItemSeeds) || wheat || sugar || cake || egg}.
     * In 26.2 "is a food" is the {@code FOOD} data component, and {@code ItemSeeds} no longer exists as a
     * class, so the four vanilla crop seeds are enumerated instead. Mirrors {@code MoCEntityGoat.isGoatEdible}.
     */
    private static boolean isRaccoonEdible(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.has(DataComponents.FOOD)
                || stack.is(Items.WHEAT_SEEDS) || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.WHEAT) || stack.is(Items.SUGAR) || stack.is(Items.CAKE) || stack.is(Items.EGG);
    }

    /**
     * Widen the vanilla food test to match {@link #isRaccoonEdible} so tempt/breed-style checks agree with
     * the interaction above (the raccoon itself cannot breed — legacy never gave it a {@code createChild}).
     */
    @Override
    public boolean isFood(ItemStack stack) {
        return isRaccoonEdible(stack);
    }

    /** Heart particles as feedback on taming/feeding (mirrors the base MoCAnimal taming/heal path). */
    private void spawnHearts(int count) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    count, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }

    /**
     * Legacy {@code getSizeFactor} (lines 137-143): an adult renders at 0.85x and a kit at
     * {@code 0.85F * edad * 0.01F}, i.e. linearly from its spawn age (50-64 -> 0.43x-0.54x) up to the full
     * 0.85x at age 100. {@code MoCMobRenderer.scale} already multiplies non-adults by its own shared
     * {@code 0.5 + 0.5 * age/100} curve, so that curve is divided back out here to land on exactly the
     * legacy number rather than compounding the two.
     */
    @Override
    public float getSizeFactor() {
        if (getIsAdult()) {
            return 0.85F;
        }
        // Floor the age at 1 so a stray age-0 raccoon is merely tiny rather than scaled to nothing (legacy
        // guarded the same case in MoCEntityAnimal.onLivingUpdate: "if (getEdad() == 0) setEdad(...)").
        float age = Math.max(1, Math.min(getMoCAge(), 100)) * 0.01F;
        return (0.85F * age) / (0.5F + (0.5F * age));
    }

    /**
     * Legacy {@code getTalkInterval()} (line 146) returned 400 against a vanilla default of 80 — raccoons
     * chitter rarely. 26.2's equivalent hook is {@code Mob.getAmbientSoundInterval()}, consumed by exactly
     * the same {@code random.nextInt(1000) < ambientSoundTime++} mechanic.
     */
    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    public Identifier getTexture() {
        // Legacy sets this.texture = "raccoon.png" in the constructor and never varies it: one coat,
        // no sub-types (selectType() inherits MoCAnimal's default of type 1).
        return modelTexture("raccoon.png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.RACCOONGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.RACCOONHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.RACCOONDYING.get();
    }

    /**
     * Faithful port of legacy {@code drzhark.mocreatures.entity.ai.EntityAIFollowAdult}: a non-adult
     * Mo'Creatures animal trails the nearest ADULT of its own class.
     *
     * <p>Vanilla's {@link net.minecraft.world.entity.ai.goal.FollowParentGoal} is not a substitute because it
     * keys off the vanilla baby age ({@code getAge() < 0}); Mo'Creatures youth is the separate
     * {@code getIsAdult()} flag, and a naturally-spawned kit has a perfectly ordinary vanilla age. The legacy
     * numbers are preserved exactly: an 8x4x8 search box, start following only when the adult is at least 3
     * blocks away (9 distance-squared), keep following out to 16 blocks (256 distance-squared), and repath
     * every 10 ticks.</p>
     */
    private static final class FollowAdultGoal extends Goal {

        private static final double DONT_FOLLOW_IF_CLOSER_THAN_SQR = 9.0D;
        private static final double GIVE_UP_BEYOND_SQR = 256.0D;

        private final MoCEntityRaccoon child;
        private final double speedModifier;
        private @Nullable MoCEntityRaccoon adult;
        private int timeToRecalcPath;

        FollowAdultGoal(MoCEntityRaccoon child, double speedModifier) {
            this.child = child;
            this.speedModifier = speedModifier;
        }

        @Override
        public boolean canUse() {
            if (this.child.getIsAdult()) {
                return false;
            }
            List<MoCEntityRaccoon> nearby = this.child.level().getEntitiesOfClass(MoCEntityRaccoon.class,
                    this.child.getBoundingBox().inflate(8.0D, 4.0D, 8.0D));
            MoCEntityRaccoon closest = null;
            double closestDistSqr = Double.MAX_VALUE;
            for (MoCEntityRaccoon candidate : nearby) {
                if (candidate == this.child || !candidate.getIsAdult()) {
                    continue;
                }
                double distSqr = this.child.distanceToSqr(candidate);
                if (distSqr <= closestDistSqr) {
                    closestDistSqr = distSqr;
                    closest = candidate;
                }
            }
            // Legacy bailed out when the nearest adult was already within 3 blocks — no point walking to a
            // parent you are standing next to.
            if (closest == null || closestDistSqr < DONT_FOLLOW_IF_CLOSER_THAN_SQR) {
                return false;
            }
            this.adult = closest;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.child.getIsAdult() || this.adult == null || !this.adult.isAlive()) {
                return false;
            }
            double distSqr = this.child.distanceToSqr(this.adult);
            return distSqr >= DONT_FOLLOW_IF_CLOSER_THAN_SQR && distSqr <= GIVE_UP_BEYOND_SQR;
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
        }

        @Override
        public void stop() {
            // Legacy resetTask() only dropped the reference; it deliberately did NOT clear the navigation
            // path, and this goal reserves no Goal.Flag (legacy setMutexBits was never called), so it shares
            // the navigator with the stroll goal exactly as it did in 1.12.
            this.adult = null;
        }

        @Override
        public void tick() {
            if (this.adult != null && --this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                this.child.getNavigation().moveTo(this.adult, this.speedModifier);
            }
        }
    }
}
