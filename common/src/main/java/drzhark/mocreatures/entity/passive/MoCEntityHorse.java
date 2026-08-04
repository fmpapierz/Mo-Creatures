package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityHorse}. Faithfully reproduces the deep legacy horse: dozens of
 * coat variants + textures/sounds, the full Mendelian breeding-genetics table (incl. zorse/mule/zonky/
 * white-fairy), saddle-gated riding, visible armour (texture swap), saddlebag chest storage, the four
 * essence transforms with a ~5s morph, flying (pegasus/fairy/unicorn) with the star trail, ghost-horse
 * idle bob, undead decay wisp, special-horse amulet capture + death-drop, and ghost/maggot spawn-on-death.
 */
public class MoCEntityHorse extends MoCAnimal {

    /** Horse armour tier: 0 none, 1 metal, 2 gold, 3 diamond, 4 crystal (magic-horse only in legacy). */
    private static final EntityDataAccessor<Integer> ARMOR =
            SynchedEntityData.defineId(MoCEntityHorse.class, EntityDataSerializers.INT);
    /** Whether the horse has saddlebags fitted (legacy {@code getChestedHorse()}). */
    private static final EntityDataAccessor<Boolean> HAS_CHEST =
            SynchedEntityData.defineId(MoCEntityHorse.class, EntityDataSerializers.BOOLEAN);
    /** Synched grazing flag (legacy {@code getEating()}): head lowered to the ground to eat. */
    private static final EntityDataAccessor<Boolean> EATING =
            SynchedEntityData.defineId(MoCEntityHorse.class, EntityDataSerializers.BOOLEAN);
    /** Synched rearing flag (legacy {@code standCounter}>0): an untamed horse bucks up on its hind legs. */
    private static final EntityDataAccessor<Boolean> REARING =
            SynchedEntityData.defineId(MoCEntityHorse.class, EntityDataSerializers.BOOLEAN);
    /**
     * Synched whip-sprint phase (legacy {@code sprintCounter} tiers): 0 idle, 1 the ~150-tick 1.5x burst,
     * 2 the exhausted 0.5x phase. Synched so {@link #getCustomSpeed()} matches on the controlling client.
     */
    private static final EntityDataAccessor<Integer> SPRINT_PHASE =
            SynchedEntityData.defineId(MoCEntityHorse.class, EntityDataSerializers.INT);

    /** Server-side timers holding the graze / rear poses for a short while once triggered. */
    private int eatingTicks;
    private int rearingTicks;
    /**
     * Legacy {@code sprintCounter} (server-side, transient): a whip crack on a ridden non-nightmare horse sets
     * this to 1; {@link #tick()} then advances it (1-149 boost, 150 neutral, 151-300 exhausted) and resets it
     * to 0 past 300. Drives the sprint speed multiplier and the unicorn/fairy charge-buckle.
     */
    private int sprintCounter;

    /**
     * The saddlebag inventory (legacy {@code localhorsechest}). Persisted in NBT and dropped on death only
     * while {@link #hasChest()} is set. The backing store holds 27 slots, but only the legacy per-coat row
     * count (9/18/27) is presented to the player by {@link #openChest(Player)}.
     */
    private final SimpleContainer chest = new SimpleContainer(27);

    /**
     * Essence-morph animation counter (server-side). 0 = idle; while > 0 it ticks up and the actual
     * {@code setTypeMoC(transformTargetType)} coat-swap fires only once it passes 100 (~5 seconds),
     * with the {@code transform} sound played mid-way at 40. Mirrors the legacy {@code transformCounter}.
     */
    private int transformCounter;
    /** The coat type the pending essence morph will become (see {@link #transform(int)}). */
    private int transformTargetType;
    /**
     * Legacy {@code eatenpumpkin}: set true when a magic/special horse is re-fed its own essence at
     * full adult health. Gates special-type breeding — two special horses only produce a special foal
     * once both have eaten their essence, and the flag resets on both parents after breeding.
     */
    private boolean eatenPumpkin;
    /**
     * Legacy {@code temper} (only meaningful for wild zebras, legacy {@code getMaxTemper()==200}): a zebra
     * resists taming until it has been won over repeatedly. Each accepted feed raises this; the zebra only
     * tames once it reaches {@link #ZEBRA_MAX_TEMPER}. Ordinary horses ignore temper (they tame in one feed).
     */
    private int temper;
    /** Legacy zebra {@code getMaxTemper()}: a wild zebra needs this much accrued temper before it tames. */
    private static final int ZEBRA_MAX_TEMPER = 200;

    public MoCEntityHorse(EntityType<? extends MoCEntityHorse> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARMOR, 0);
        builder.define(HAS_CHEST, false);
        builder.define(EATING, false);
        builder.define(REARING, false);
        builder.define(SPRINT_PHASE, 0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Wild zebras (type 60) flee from any player who is NOT approaching on a spotted (16), cow (17),
        // zebra (60) or zorse (61) horse — the legacy way to tame a zebra was to ride one of those up to it
        // (legacy isZebraRunning / runLikeHell). Installed unconditionally; canUse() gates it to type 60.
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<Player>(
                this, Player.class, 8.0F, 1.6D, 1.8D,
                living -> {
                    if (living.getVehicle() instanceof MoCEntityHorse mount) {
                        int mt = mount.getTypeMoC();
                        return mt != 16 && mt != 17 && mt != 60 && mt != 61;
                    }
                    return true;
                }) {
            @Override
            public boolean canUse() {
                return MoCEntityHorse.this.getTypeMoC() == 60 && !MoCEntityHorse.this.getIsTamed()
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return MoCEntityHorse.this.getTypeMoC() == 60 && !MoCEntityHorse.this.getIsTamed()
                        && super.canContinueToUse();
            }
        });
    }

    /**
     * The nearby player a wild zebra should flee from — the closest player within 8 blocks, unless that
     * player is riding a spotted (16), cow (17), zebra (60) or zorse (61) horse (which does not spook it).
     * Returns {@code null} when there is no such threat. Mirrors legacy {@code isZebraRunning}.
     */
    private net.minecraft.world.entity.player.@Nullable Player zebraThreat() {
        Player nearest = this.level().getNearestPlayer(this, 8.0D);
        if (nearest == null) {
            return null;
        }
        if (nearest.getVehicle() instanceof MoCEntityHorse mount) {
            int mt = mount.getTypeMoC();
            if (mt == 16 || mt == 17 || mt == 60 || mt == 61) {
                return null;
            }
        }
        return nearest;
    }

    /** Grazing state (legacy {@code getEating()}); the client lowers the head to the ground. */
    public boolean getEating() {
        return this.entityData.get(EATING);
    }

    /** Start/stop grazing. The whip toggles this on a tamed, unridden horse (legacy whip-crack). */
    public void setEating(boolean eating) {
        if (eating != getEating()) {
            this.entityData.set(EATING, eating);
        }
        this.eatingTicks = eating ? 60 : 0;
    }

    /** Rearing state (legacy {@code standCounter}>0); the client rears the horse onto its hind legs. */
    public boolean getRearing() {
        return this.entityData.get(REARING);
    }

    public int getArmor() {
        return this.entityData.get(ARMOR);
    }

    public void setArmor(int tier) {
        this.entityData.set(ARMOR, tier);
    }

    /** Donkeys/mules/zonkies (types 65/66/67) render slightly smaller (legacy {@code type>64 -> *0.9}). */
    @Override
    public float getSizeFactor() {
        int t = getTypeMoC();
        return (t == 65 || t == 66 || t == 67) ? 0.9F : 1.0F;
    }

    /** True when saddlebags are fitted (legacy {@code getChestedHorse()}). */
    public boolean hasChest() {
        return this.entityData.get(HAS_CHEST);
    }

    public void setHasChest(boolean v) {
        this.entityData.set(HAS_CHEST, v);
    }

    public SimpleContainer getChest() {
        return this.chest;
    }

    // ------------------------------------------------------------------ armour / transform classifiers
    /** Ordinary (non-magic) horse coats — the only ones that may wear metal/gold/diamond armour. */
    public boolean isArmored() {
        return getTypeMoC() < 21;
    }

    /** Magic horses (unicorns, pegasi, fairies, bat/ghost horses) — the only ones that may wear crystal armour. */
    public boolean isMagicHorse() {
        int t = getTypeMoC();
        return t == 39 || t == 36 || t == 32 || t == 40 || (t >= 45 && t < 60) || t == 21 || t == 22;
    }

    /** Undead horses (undead / undead-unicorn / undead-pegasus and the three skeleton variants). */
    public boolean isUndead() {
        int t = getTypeMoC();
        return t == 23 || t == 24 || t == 25 || t == 26 || t == 27 || t == 28;
    }

    /** Ghost horses. */
    public boolean isGhost() {
        int t = getTypeMoC();
        return t == 21 || t == 22;
    }

    /** Nightmare horse. */
    public boolean isNightmare() {
        return getTypeMoC() == 38;
    }

    /**
     * Legacy {@code isUnicorned()}: the horned coats — unicorn (36), the fairy horses (45-59), and the
     * skeleton/undead unicorns (27/24) — which ram nearby mobs while charging (see the {@link #tick()} buckle).
     */
    public boolean isUnicornedCoat() {
        int t = getTypeMoC();
        return t == 36 || (t >= 45 && t < 60) || t == 27 || t == 24;
    }

    /**
     * Legacy {@code isBagger()}: the coats that can carry saddlebags — mules, donkeys, zonkeys, the
     * winged pegasus family (white/dark/undead/skeleton pegasi) and the fairy horses.
     */
    public boolean isBagger() {
        int t = getTypeMoC();
        return t == 65 || t == 66 || t == 67          // donkey / mule / zonky
                || t == 39 || t == 40                 // white / dark pegasus
                || t == 25 || t == 28                 // undead / skeleton pegasus
                || (t >= 45 && t < 60);               // fairy horses
    }

    /** Legacy {@code isPureBreed()}: the tier-3/4 ordinary coats (11-20) that can spawn a ghost foal on death. */
    public boolean isPureBreed() {
        int t = getTypeMoC();
        return t > 10 && t < 21;
    }

    /**
     * Drops the currently-worn armour item back into the world and resets the tier to 0. Server-side only.
     * Mirrors the legacy {@code dropArmor()} (which plays {@code armoroff} and spawns the matching item).
     */
    private void dropArmor() {
        if (this.level() instanceof ServerLevel server) {
            int tier = getArmor();
            if (tier == 0) {
                return;
            }
            this.level().playSound(null, blockPosition(), MoCSounds.ARMOROFF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            ItemStack drop = switch (tier) {
                case 1 -> new ItemStack(MoCItems.ARMORMETAL.get());
                case 2 -> new ItemStack(MoCItems.ARMORGOLD.get());
                case 3 -> new ItemStack(MoCItems.ARMORDIAMOND.get());
                case 4 -> new ItemStack(MoCItems.HORSEARMORCRYSTAL.get());
                default -> ItemStack.EMPTY;
            };
            if (!drop.isEmpty()) {
                this.spawnAtLocation(server, drop);
            }
            setArmor(0);
        }
    }

    /**
     * Begins a ~5-second essence morph into coat {@code t}. An unridden horse plays the animation
     * (armour is shed up front, {@link #transformCounter} starts and the coat swaps once it finishes
     * in {@link #tick()}); a ridden horse — which cannot safely animate mid-ride — transforms at once.
     * Mirrors the legacy {@code transform(int)}.
     */
    private void transform(int t) {
        this.transformTargetType = t;
        // Legacy transform(): only a horse that is NOT being ridden starts the morph (sheds its armour and
        // begins the ~5s animation). A ridden horse ignores the transform entirely — no coat change and no
        // animation — even though the essence that triggered it was still consumed by the caller.
        if (!this.isVehicle() && t != 0) {
            dropArmor();
            this.transformCounter = 1;
        }
    }

    // ------------------------------------------------------------------ flight (winged horse types)
    /** Legacy {@code isFlyer()}: pegasus, dark pegasus, fairy horses, bat horse, ghost, undead/skeleton pegasus. */
    public boolean isFlyer() {
        int t = getTypeMoC();
        return t == 39 || t == 40 || (t >= 45 && t < 60) || t == 32 || t == 21 || t == 25 || t == 28;
    }

    /**
     * Legacy {@code isFloater()}: the coats that drift gently down and take no fall damage — unicorn (36),
     * skeleton unicorn (27), undead unicorn (24) and the non-winged ghost horse (22).
     */
    public boolean isFloater() {
        int t = getTypeMoC();
        return t == 36 || t == 27 || t == 24 || t == 22;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        // Legacy fall(): flyers and floaters take no fall damage at all; every other coat of tier 3+
        // (type >= 10) takes only a third of it, while tier-1/2 coats take it in full.
        if (isFlyer() || isFloater()) {
            return false;
        }
        float mult = getTypeMoC() >= 10 ? multiplier / 3.0F : multiplier;
        // Legacy fall(): the rider takes the same (tier-reduced) fall damage the horse does — riding a
        // non-flyer off a cliff hurts the player too, not just the mount (mirrors MoCEntityElephant).
        net.minecraft.world.entity.LivingEntity rider = this.getControllingPassenger();
        if (rider != null) {
            int riderDmg = this.calculateFallDamage(fallDistance, mult);
            if (riderDmg > 0) {
                rider.hurt(source, (float) riderDmg);
            }
        }
        return super.causeFallDamage(fallDistance, mult, source);
    }

    @Override
    public void travel(net.minecraft.world.phys.Vec3 input) {
        // A ridden winged horse gallops normally on the ground, but takes to the air and flies — steered by
        // the rider's look (up to climb, down to dive; forward/back to move; no input hovers) — once it is
        // airborne or when the rider looks up to take off. Non-flyers and unridden horses ride normally.
        if (isFlyer() && this.isVehicle() && getControllingPassenger() instanceof Player rider
                && (!this.onGround() || rider.getXRot() < -25.0F)) {
            this.setYRot(rider.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.5F);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.setNoGravity(true);
            float fwd = rider.zza;
            float str = rider.xxa;
            float speed = (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 2.2D);
            if (fwd != 0.0F || str != 0.0F) {
                net.minecraft.world.phys.Vec3 look = rider.getLookAngle();
                double fwdScale = fwd > 0.0F ? speed : (fwd < 0.0F ? -speed * 0.4D : 0.0D);
                net.minecraft.world.phys.Vec3 forward = look.scale(fwdScale);
                net.minecraft.world.phys.Vec3 side =
                        new net.minecraft.world.phys.Vec3(look.z, 0.0D, -look.x).normalize().scale(str * speed * 0.5D);
                net.minecraft.world.phys.Vec3 desired = forward.add(side);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D).add(desired.scale(0.5D)));
            } else {
                net.minecraft.world.phys.Vec3 dm = this.getDeltaMovement();
                this.setDeltaMovement(dm.x * 0.7D, Math.max(dm.y * 0.8D, -0.04D), dm.z * 0.7D);
            }
            this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            // Ground movement always has gravity (this clears the flight no-gravity on landing too).
            this.setNoGravity(false);
            super.travel(input);
        }
    }

    /** Nightmare (38), dark pegasus (40), bat horse (32) and the undead/skeleton horses (23-28) are fiery. */
    @Override
    public boolean fireImmune() {
        int t = getTypeMoC();
        return t == 38 || t == 40 || t == 32 || (t >= 23 && t <= 28) || super.fireImmune();
    }

    @Override
    public void tick() {
        super.tick();
        // Essence morph animation: server drives the counter; the coat only swaps once it finishes (~5s),
        // with the transform sound played mid-way. Guarded server-side so typeMoC (synched) drives the coat.
        if (!this.level().isClientSide() && this.transformCounter > 0) {
            this.transformCounter++;
            if (this.transformCounter == 40) {
                this.level().playSound(null, blockPosition(), MoCSounds.TRANSFORM.get(),
                        SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            if (this.transformCounter > 100) {
                setTypeMoC(this.transformTargetType);
                dropArmor();
                this.transformCounter = 0;
            }
        }
        // Keep per-tier health/jump attributes matched to the current coat (spawn + breeding/essence changes).
        if (!this.level().isClientSide()) {
            applyTypeStats();
        }
        // Undead decay (legacy onLivingUpdate): an adult undead horse slowly ages and, once fully rotted
        // (age >= 399), turns into its skeleton variant — undead 23 -> skeleton 26, undead unicorn 24 -> 27,
        // undead pegasus 25 -> skeleton pegasus 28.
        if (!this.level().isClientSide() && isUndead() && getTypeMoC() < 26 && getIsAdult()
                && this.random.nextInt(20) == 0) {
            if (this.random.nextInt(16) == 0) {
                setMoCAge(getMoCAge() + 1);
            }
            if (getMoCAge() >= 399) {
                setTypeMoC(getTypeMoC() + 3);
            }
        }
        // Foal maturation (legacy onLivingUpdate + ghost fast-path): a non-adult horse ages up over time and,
        // once grown, becomes a MoC-adult so it can breed and be pumpkin-fed. Ghost foals mature far faster
        // (aging ~1/7 ticks, adult at MoCAge 9) than ordinary foals (~1/200 ticks, adult at MoCAge 100).
        if (!this.level().isClientSide() && !getIsAdult()
                && this.random.nextInt(isGhost() ? 7 : 200) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= (isGhost() ? 9 : 100)) {
                setAdult(true);
            }
        }
        // Whip-triggered sprint (legacy sprintCounter): once the whip sets sprintCounter = 1 on a ridden horse
        // it advances every tick — a ~150-tick 1.5x burst, then a slow exhausted phase — and resets past 300.
        // SPRINT_PHASE is synched so getCustomSpeed matches on the controlling client. While the burst is
        // active a ridden unicorn/fairy buckles nearby mobs (legacy MoCTools.buckleMobs: 2 damage + knockback).
        if (!this.level().isClientSide()) {
            if (this.sprintCounter > 0 && ++this.sprintCounter > 300) {
                this.sprintCounter = 0;
            }
            int sprintPhase = (this.sprintCounter > 0 && this.sprintCounter < 150) ? 1
                    : (this.sprintCounter > 150 ? 2 : 0);
            if (this.entityData.get(SPRINT_PHASE) != sprintPhase) {
                this.entityData.set(SPRINT_PHASE, sprintPhase);
            }
            if (this.sprintCounter > 0 && this.sprintCounter < 150 && this.isVehicle() && isUnicornedCoat()
                    && this.level() instanceof ServerLevel sprintLevel) {
                for (net.minecraft.world.entity.LivingEntity victim : sprintLevel.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class, this.getBoundingBox().inflate(2.0D),
                        ent -> ent != this && !this.hasPassenger(ent))) {
                    victim.hurtServer(sprintLevel, this.damageSources().mobAttack(this), 2.0F);
                    // Legacy MoCTools.bigsmack(this, victim, 0.6F): halve the victim's momentum, then shove it away.
                    double dx = this.getX() - victim.getX();
                    double dz = this.getZ() - victim.getZ();
                    double f = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
                    double force = 0.6D;
                    double vy = Math.min(force, (victim.getDeltaMovement().y * 0.5D) + force);
                    victim.setDeltaMovement((victim.getDeltaMovement().x * 0.5D) - (dx / f) * force, vy,
                            (victim.getDeltaMovement().z * 0.5D) - (dz / f) * force);
                    victim.hurtMarked = true;
                }
            }
        }
        // Graze / rear pose driving (server-authoritative; the flags are synched to animate the model).
        if (!this.level().isClientSide()) {
            // Rearing: an untamed horse with a rider bucks up on its hind legs to throw them (legacy standCounter).
            if (!getIsTamed() && this.isVehicle()) {
                if (this.rearingTicks <= 0 && this.random.nextInt(20) == 0) {
                    this.rearingTicks = 12;
                }
            }
            if (this.rearingTicks > 0) {
                this.rearingTicks--;
                if (!getRearing()) {
                    this.entityData.set(REARING, true);
                }
            } else if (getRearing()) {
                this.entityData.set(REARING, false);
            }
            // Grazing: a calm tamed horse standing still on the ground occasionally lowers its head to eat.
            if (this.eatingTicks <= 0 && getIsTamed() && !this.isVehicle() && this.onGround()
                    && !getRearing() && this.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4D
                    && this.random.nextInt(300) == 0) {
                setEating(true);
            }
            if (this.eatingTicks > 0) {
                this.eatingTicks--;
                if (this.eatingTicks == 0 && getEating()) {
                    this.entityData.set(EATING, false);
                }
            }
        }
        boolean fx = drzhark.mocreatures.config.MoCConfig.get().particleFX;
        // Nightmare horses smoulder — flame particles drift up from the hooves (purely cosmetic, client-side).
        if (fx && getTypeMoC() == 38 && this.level().isClientSide() && this.getRandom().nextInt(3) == 0) {
            double w = this.getBbWidth();
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                    this.getX() + (this.getRandom().nextDouble() - 0.5D) * w,
                    this.getY() + 0.1D,
                    this.getZ() + (this.getRandom().nextDouble() - 0.5D) * w,
                    0.0D, 0.0D, 0.0D);
        }
        // Magic horses (pegasus / unicorn / fairy) leave a trail of stars while moving (legacy StarFX).
        if (fx && isMagicHorse() && this.level().isClientSide()) {
            net.minecraft.world.phys.Vec3 dm = getDeltaMovement();
            if ((dm.x * dm.x + dm.z * dm.z) > 0.003D && this.getRandom().nextInt(2) == 0) {
                double w = this.getBbWidth();
                this.level().addParticle(drzhark.mocreatures.registry.MoCParticles.FX_STAR.get(),
                        this.getX() + (this.getRandom().nextDouble() - 0.5D) * w,
                        this.getY() + this.getBbHeight() * 0.5D,
                        this.getZ() + (this.getRandom().nextDouble() - 0.5D) * w,
                        0.0D, 0.05D, 0.0D);
            }
        }
        // Whip-sprint star trail (legacy StarFX during sprintCounter 1-149): any galloping horse — magic or
        // not — trails stars while its whip-sprint burst (SPRINT_PHASE 1) is active.
        if (fx && this.level().isClientSide() && this.entityData.get(SPRINT_PHASE) == 1
                && this.tickCount % 2 == 0) {
            double w = this.getBbWidth();
            this.level().addParticle(drzhark.mocreatures.registry.MoCParticles.FX_STAR.get(),
                    this.getX() + (this.getRandom().nextDouble() - 0.5D) * w,
                    this.getY() + this.getBbHeight() * 0.5D,
                    this.getZ() + (this.getRandom().nextDouble() - 0.5D) * w,
                    0.0D, 0.05D, 0.0D);
        }
        // Undead / skeleton horses shed a greenish decay wisp as they rot (legacy UndeadFX).
        if (fx && isUndead() && this.level().isClientSide() && this.getRandom().nextInt(4) == 0) {
            double w = this.getBbWidth();
            this.level().addParticle(drzhark.mocreatures.registry.MoCParticles.FX_UNDEAD.get(),
                    this.getX() + (this.getRandom().nextDouble() - 0.5D) * w,
                    this.getY() + this.getBbHeight() * 0.6D,
                    this.getZ() + (this.getRandom().nextDouble() - 0.5D) * w,
                    0.0D, 0.02D, 0.0D);
        }
    }

    private static int armorTier(ItemStack stack) {
        if (stack.is(MoCItems.ARMORMETAL.get())) return 1;
        if (stack.is(MoCItems.ARMORGOLD.get())) return 2;
        if (stack.is(MoCItems.ARMORDIAMOND.get())) return 3;
        if (stack.is(MoCItems.HORSEARMORCRYSTAL.get())) return 4;
        return 0;
    }

    /** True if {@code stack} is one of the four transformation essences. */
    private static boolean isEssence(ItemStack stack) {
        return stack.is(MoCItems.ESSENCEUNDEAD.get()) || stack.is(MoCItems.ESSENCEFIRE.get())
                || stack.is(MoCItems.ESSENCEDARKNESS.get()) || stack.is(MoCItems.ESSENCELIGHT.get());
    }

    /**
     * Legacy dye-to-fairy mapping: the coloured-fairy coat a given dye turns a white fairy (type 50) into,
     * or 0 if {@code stack} is not a mapped dye. Magenta, gray, light-gray and brown have no fairy variant
     * (legacy no-ops). In 26.2 the individual dye items live in the {@link Items#DYE} colour collection.
     */
    private static int fairyTypeForDye(ItemStack stack) {
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.ORANGE))) return 59;
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE))) return 51;
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.YELLOW))) return 48;
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.LIME))) return 53;
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.PINK))) return 52;
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.CYAN))) return 57;
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.PURPLE))) return 49;
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.BLUE))) return 56;
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.GREEN))) return 58;
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.RED))) return 55;
        if (stack.is(Items.DYE.pick(net.minecraft.world.item.DyeColor.BLACK))) return 54;
        return 0;
    }

    /**
     * Legacy {@code getInventorySize()} row count for this coat's saddlebags: the black pegasus (40) carries
     * 2 rows (18 slots), donkeys/mules/zonkies (types &gt; 64) carry 3 rows (27 slots), and every other
     * bagger (winged pegasi, fairies) carries a single row (9 slots).
     */
    private int chestRows() {
        int t = getTypeMoC();
        if (t == 40) {
            return 2;
        }
        if (t > 64) {
            return 3;
        }
        return 1;
    }

    /**
     * Opens the saddlebag inventory for {@code player} at the legacy per-coat size (9/18/27 slots),
     * server-side. The shared 27-slot backing store is presented with only {@link #chestRows()} rows.
     */
    private void openChest(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            int rows = chestRows();
            net.minecraft.world.inventory.MenuType<?> menuType = switch (rows) {
                case 2 -> net.minecraft.world.inventory.MenuType.GENERIC_9x2;
                case 3 -> net.minecraft.world.inventory.MenuType.GENERIC_9x3;
                default -> net.minecraft.world.inventory.MenuType.GENERIC_9x1;
            };
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ChestMenu(menuType, id, inv, this.chest, rows), getDisplayName()));
        }
    }

    /** Whether this coat can be trapped in a special amulet (legacy {@code isAmuletHorse}). */
    public boolean isAmuletHorse() {
        int t = getTypeMoC();
        return (t >= 48 && t < 60) || t == 40 || t == 39 || t == 21 || t == 22 || t == 26 || t == 27 || t == 28;
    }

    /** Captures this horse into a full amulet (storing its coat + name), consuming the empty amulet. */
    private void captureIntoAmulet(Player player, ItemStack empty, net.minecraft.world.item.Item fullItem) {
        ItemStack full = new ItemStack(fullItem);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("HorseType", getTypeMoC());
        if (hasCustomName() && getCustomName() != null) {
            tag.putString("HorseName", getCustomName().getString());
        }
        // Preserve live state so MoCSpecialAmuletItem restores it on release (legacy amulet round-trip).
        tag.putFloat("Health", getHealth());
        tag.putInt("MoCAge", getMoCAge());
        tag.putBoolean("Rideable", isSaddled());
        tag.putInt("Armor", getArmor());
        tag.putBoolean("Adult", getIsAdult());
        full.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
        if (!player.getAbilities().instabuild) {
            empty.shrink(1);
        }
        player.addItem(full);
        this.level().playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                SoundSource.NEUTRAL, 0.2F, 1.0F);
        this.discard();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Wild zebras (type 60) bolt from a nearby player and refuse to be handled while fleeing. The legacy
        // taming trick is to approach one while riding a spotted/cow/zebra/zorse horse (see zebraThreat).
        if (getTypeMoC() == 60 && !getIsTamed() && zebraThreat() != null) {
            return InteractionResult.PASS;
        }
        // Legacy zebra temper (getMaxTemper()==200): a wild zebra does NOT tame from a single feed the way an
        // ordinary horse does. Each accepted feed of its normal food raises temper; the zebra only submits and
        // tames once temper reaches ZEBRA_MAX_TEMPER. This intercepts the base FEED taming so ONLY zebras resist
        // (ordinary horses fall through to super and tame in one feed as before).
        if (getTypeMoC() == 60 && !getIsTamed() && isFood(stack)) {
            if (!this.level().isClientSide()) {
                if (exceedsTameCap(player)) {
                    return InteractionResult.SUCCESS;
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.temper += 40;
                heal(getMaxHealth());
                if (this.temper >= ZEBRA_MAX_TEMPER) {
                    setTamed(true);
                    setOwnerName(player.getName().getString());
                    setAdult(true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        // A sneaking empty-handed owner opens the saddlebags (in addition to the key trigger below), so
        // storage is reachable without the key. Gated on sneaking so a plain empty-hand interaction
        // still mounts a rideable bagger horse via MoCAnimal's mount path.
        if (getIsTamed() && hasChest() && stack.isEmpty() && player.isShiftKeyDown()) {
            if (!this.level().isClientSide()) {
                openChest(player);
            }
            return InteractionResult.SUCCESS;
        }
        if (getIsTamed() && !stack.isEmpty()) {
            // --- special-horse amulets: an empty amulet captures a matching tamed special horse (skeleton/
            //     fairy/pegasus/ghost) into its full amulet, which stores the coat for release (legacy). ---
            if (isAmuletHorse()) {
                int t = getTypeMoC();
                net.minecraft.world.item.Item full = null;
                if ((t == 26 || t == 27 || t == 28) && stack.is(MoCItems.AMULETBONE.get())) {
                    full = MoCItems.AMULETBONEFULL.get();
                } else if (t > 47 && t < 60 && stack.is(MoCItems.AMULETFAIRY.get())) {
                    full = MoCItems.AMULETFAIRYFULL.get();
                } else if ((t == 39 || t == 40) && stack.is(MoCItems.AMULETPEGASUS.get())) {
                    full = MoCItems.AMULETPEGASUSFULL.get();
                } else if ((t == 21 || t == 22) && stack.is(MoCItems.AMULETGHOST.get())) {
                    full = MoCItems.AMULETGHOSTFULL.get();
                }
                if (full != null) {
                    if (!this.level().isClientSide()) {
                        captureIntoAmulet(player, stack, full);
                    }
                    return InteractionResult.SUCCESS;
                }
            }

            // --- horse armour: metal/gold/diamond only on ordinary horses, crystal only on magic horses ---
            int tier = armorTier(stack);
            if (tier > 0 && getArmor() != tier) {
                boolean allowed = (tier <= 3 && isArmored()) || (tier == 4 && isMagicHorse());
                if (allowed) {
                    if (!this.level().isClientSide()) {
                        // Replacing a different existing tier drops the worn armour first (dropArmor resets to 0);
                        // the "put on" sound only plays when going from bare (tier 0).
                        boolean wasBare = getArmor() == 0;
                        dropArmor();
                        setArmor(tier);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        if (wasBare) {
                            this.level().playSound(null, blockPosition(), MoCSounds.ARMORPUT.get(),
                                    SoundSource.NEUTRAL, 1.0F, 1.0F);
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
                // tier not allowed for this horse -> fall through (do not apply)
            }

            // --- saddlebags: fit a vanilla chest onto any tamed bagger horse (legacy has no adult gate,
            //     so a tamed baby donkey/mule/pegasus/fairy can be chested too), granting a key ---
            if (!hasChest() && isBagger() && stack.is(Items.CHEST)) {
                if (!this.level().isClientSide()) {
                    setHasChest(true);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    player.addItem(new ItemStack(MoCItems.KEY.get()));
                    this.level().playSound(null, blockPosition(), MoCSounds.ARMORPUT.get(),
                            SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }

            // --- key opens the saddlebags (vanilla three-row chest screen) ---
            if (hasChest() && stack.is(MoCItems.KEY.get())) {
                if (!this.level().isClientSide()) {
                    openChest(player);
                }
                return InteractionResult.SUCCESS;
            }

            // --- transformation essences ---
            if (isEssence(stack)) {
                if (this.level() instanceof ServerLevel server) {
                    // Type-changing essences now start a ~5-second morph animation via transform(): the coat
                    // swaps (and armour is shed, the transform sound plays) only once the counter finishes in
                    // tick(). Heal-only branches (no type change) still apply immediately.
                    if (stack.is(MoCItems.ESSENCEUNDEAD.get())) {
                        applyEssenceUndead();
                    } else if (stack.is(MoCItems.ESSENCEFIRE.get())) {
                        applyEssenceFire();
                    } else if (stack.is(MoCItems.ESSENCEDARKNESS.get())) {
                        applyEssenceDarkness();
                    } else { // ESSENCELIGHT
                        applyEssenceLight(server);
                    }

                    // Consume one essence and give back a single empty bottle (creative keeps its stack).
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                        player.addItem(new ItemStack(Items.GLASS_BOTTLE));
                    }
                    this.level().playSound(null, blockPosition(), MoCSounds.DRINKING.get(),
                            SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }

            // --- dye a tamed white fairy horse (type 50) into one of the eleven coloured fairy variants
            //     (legacy dye-powder interaction); consumes one dye and plays the eating sound. ---
            if (getTypeMoC() == 50) {
                int target = fairyTypeForDye(stack);
                if (target != 0) {
                    if (!this.level().isClientSide()) {
                        transform(target);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                        }
                        this.level().playSound(null, blockPosition(), MoCSounds.EATING.get(),
                                SoundSource.NEUTRAL, 1.0F, 1.0F);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        // --- pumpkin / mushroom stew / cake: feed an ADULT ordinary horse (tamed OR wild) to heal it and
        //     flag it ready to breed (legacy eatenpumpkin gate has NO tamed check). Magic/undead horses
        //     refuse this food. Lives outside the tamed block so a wild adult horse can be pre-flagged. ---
        if (getIsAdult() && !isMagicHorse() && !isUndead()
                && (stack.is(net.minecraft.world.level.block.Blocks.PUMPKIN.asItem())
                    || stack.is(Items.MUSHROOM_STEW) || stack.is(Items.CAKE))) {
            if (!this.level().isClientSide()) {
                this.eatenPumpkin = true;
                setHealth(getMaxHealth());
                boolean stew = stack.is(Items.MUSHROOM_STEW);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    if (stew) {
                        player.addItem(new ItemStack(Items.BOWL));
                    }
                }
                this.level().playSound(null, blockPosition(), MoCSounds.EATING.get(),
                        SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Essence of Undead: heals undead/ghost coats to full, otherwise turns the horse undead by category. */
    private void applyEssenceUndead() {
        if (isUndead() || isGhost()) {
            // Re-feeding an adult undead/ghost at full health flags it ready for special breeding.
            if (getIsAdult() && getHealth() == getMaxHealth()) {
                this.eatenPumpkin = true;
            }
            setHealth(getMaxHealth());
        }
        int t = getTypeMoC();
        if (t == 39 || t == 32 || t == 40) {
            transform(25); // undead pegasus
        } else if (t == 36 || (t > 47 && t < 60)) {
            transform(24); // undead unicorn
        } else if (t < 21 || t == 60 || t == 61) {
            transform(23); // undead horse
        }
    }

    /** Essence of Fire: heals a nightmare to full; a zorse (61) becomes a nightmare (38). */
    private void applyEssenceFire() {
        if (isNightmare()) {
            if (getIsAdult() && getHealth() == getMaxHealth()) {
                this.eatenPumpkin = true;
            }
            setHealth(getMaxHealth());
        }
        if (getTypeMoC() == 61) {
            transform(38); // nightmare
        }
    }

    /** Essence of Darkness: heals a bat horse; zorse -> bat horse; pegasus -> dark pegasus. */
    private void applyEssenceDarkness() {
        if (getTypeMoC() == 32) {
            if (getIsAdult() && getHealth() == getMaxHealth()) {
                this.eatenPumpkin = true;
            }
            setHealth(getMaxHealth());
        }
        if (getTypeMoC() == 61) {
            transform(32); // bat horse
        }
        if (getTypeMoC() == 39) {
            transform(40); // dark pegasus
        }
    }

    /** Essence of Light: heals magic horses; reverses the dark transformations and rejuvenates the undead. */
    private void applyEssenceLight(ServerLevel server) {
        if (isMagicHorse()) {
            if (getIsAdult() && getHealth() == getMaxHealth()) {
                this.eatenPumpkin = true;
            }
            setHealth(getMaxHealth());
        }
        if (isNightmare()) {
            transform(36); // unicorn
        }
        if (getTypeMoC() == 32 && getY() > 128.0D) {
            transform(39); // pegasus
        }
        // return undead horses to pristine (fresh adult) condition
        if (isUndead() && getIsAdult()) {
            setMoCAge(50); // reset to a fresh-adult age
            if (getTypeMoC() > 26) {
                setTypeMoC(getTypeMoC() - 3); // skeleton unicorn/pegasus (27/28) -> undead unicorn/pegasus (24/25)
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // Legacy attackEntityFrom: a horse ignores damage dealt by its own rider, and a wolf attacker has
        // its target cleared and deals no damage (wolves can never hurt horses).
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        if (attacker != null && this.hasPassenger(attacker)) {
            return false;
        }
        if (attacker instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf) {
            wolf.setTarget(null);
            return false;
        }
        // Legacy flat armour mitigation applied to ALL damage sources: subtract (armourTier + 2), clamped to
        // >= 0. A bare horse (tier 0) still shrugs off 2 damage; metal -3, gold -4, diamond -5, crystal -6.
        amount -= (getArmor() + 2);
        if (amount < 0.0F) {
            amount = 0.0F;
        }
        return super.hurtServer(level, source, amount);
    }

    /**
     * Death loot. Beyond the vanilla drops this: (1) empties the saddlebags into the world and drops the
     * chest item back (legacy {@code dropBags} + inventory scatter); (2) spawns a small brood of maggots
     * from a rotting undead/skeleton carcass (legacy {@code spawnMaggots}); and (3) gives a slain tamed,
     * non-ghost magic/purebred horse a 25% chance to leave behind a tamed baby ghost horse bearing the
     * owner (legacy {@code onDeath}). Mirrors {@link MoCEntityElephant#dropCustomDeathLoot}.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);

        // (0) Coat-specific drop (legacy getDropItemId + vanilla dropFewItems): each special coat drops 0-2
        //     copies of a SINGLE id — its signature on the 1-in-4 rare roll (unicorn horn, heartfire/
        //     heartdarkness), else the coat's fall-through: leather for unicorn/fairy/nightmare/bat (so the
        //     signature and leather are MUTUALLY EXCLUSIVE), rotten flesh for undead. Pegasi always drop
        //     feathers, skeletons bone, ghosts ghast tears. Ordinary coats leave `id` null so their default
        //     0-2 leather is emitted by MoCBehavior.dropLoot instead. (MoCBehavior must suppress its leather
        //     for every non-ordinary coat handled here — 21/22/23/24/25/26/32/36/38/39/40/50-59 — so leather
        //     is never double-dropped.)
        boolean rare = this.random.nextInt(4) == 0;
        int coat = getTypeMoC();
        net.minecraft.world.item.Item id = null;
        if (coat == 36 || (coat >= 50 && coat < 60)) {
            id = rare ? MoCItems.UNICORNHORN.get() : Items.LEATHER;             // unicorn / fairy: horn @1/4 else leather
        } else if (coat == 39 || coat == 40) {
            id = Items.FEATHER;                                                 // white / dark pegasus: always feather
        } else if (coat == 38) {
            id = (rare && level.dimension() == Level.NETHER)
                    ? MoCItems.HEARTFIRE.get() : Items.LEATHER;                 // nightmare: heartfire @1/4 in Nether else leather
        } else if (coat == 32) {
            id = rare ? MoCItems.HEARTDARKNESS.get() : Items.LEATHER;           // bat horse: heartdarkness @1/4 else leather
        } else if (coat == 26) {
            id = Items.BONE;                                                    // skeleton
        } else if (coat == 23 || coat == 24 || coat == 25) {
            id = rare ? MoCItems.HEARTUNDEAD.get() : Items.ROTTEN_FLESH;        // undead: heartundead @1/4 else rotten flesh
        } else if (coat == 21 || coat == 22) {
            id = Items.GHAST_TEAR;                                              // ghost
        }
        if (id != null) {
            int drops = this.random.nextInt(3); // 0-2 copies of the single id (legacy vanilla dropFewItems)
            for (int k = 0; k < drops; k++) {
                spawnAtLocation(level, new ItemStack(id));
            }
        }

        // (1) Saddlebags: scatter the stored items and drop the chest block, then bare the horse.
        if (hasChest()) {
            for (int i = 0; i < this.chest.getContainerSize(); i++) {
                ItemStack s = this.chest.getItem(i);
                if (!s.isEmpty()) {
                    spawnAtLocation(level, s);
                }
            }
            this.chest.clearContent();
            spawnAtLocation(level, new ItemStack(Items.CHEST));
            setHasChest(false);
        }

        // (2) Rotting undead coats spew maggots (legacy onDeath: && binds tighter than ||, so this is
        //     ((1/10) && undead-23) || undead-unicorn-24 || undead-pegasus-25): 24 and 25 ALWAYS, 23 at
        //     1/10, and the skeleton coats (26/27/28) never.
        if ((coat == 23 && this.random.nextInt(10) == 0) || coat == 24 || coat == 25) {
            spawnMaggots(level);
        }

        // (3) A tamed, non-ghost magic/purebred horse has a 25% chance to leave a tamed baby ghost horse.
        if (getIsTamed() && (isMagicHorse() || isPureBreed()) && !isGhost() && this.random.nextInt(4) == 0) {
            spawnGhostFoal(level);
        }

        // (4) A tamed special horse (skeleton/ghost/fairy/pegasus) leaves its FULL amulet on death, storing its
        //     coat + name so the owner can release the same horse again (legacy dropAmulet safety-net).
        if (getIsTamed() && isAmuletHorse()) {
            net.minecraft.world.item.Item amulet = properFullAmulet();
            if (amulet != null) {
                ItemStack full = new ItemStack(amulet);
                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                tag.putInt("HorseType", getTypeMoC());
                if (hasCustomName() && getCustomName() != null) {
                    tag.putString("HorseName", getCustomName().getString());
                }
                // Preserve live state so MoCSpecialAmuletItem restores it on release (legacy amulet round-trip).
                tag.putFloat("Health", getHealth());
                tag.putInt("MoCAge", getMoCAge());
                tag.putBoolean("Rideable", isSaddled());
                tag.putInt("Armor", getArmor());
                tag.putBoolean("Adult", getIsAdult());
                full.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(tag));
                spawnAtLocation(level, full);
            }
        }
    }

    /** The full special-horse amulet item matching this horse's coat, or {@code null} if not an amulet horse. */
    private net.minecraft.world.item.Item properFullAmulet() {
        int t = getTypeMoC();
        if (t == 26 || t == 27 || t == 28) {
            return MoCItems.AMULETBONEFULL.get();
        }
        if (t > 47 && t < 60) {
            return MoCItems.AMULETFAIRYFULL.get();
        }
        if (t == 39 || t == 40) {
            return MoCItems.AMULETPEGASUSFULL.get();
        }
        if (t == 21 || t == 22) {
            return MoCItems.AMULETGHOSTFULL.get();
        }
        return null;
    }

    /** Spawns a small brood (1-4) of maggots at the carcass (legacy {@code MoCTools.spawnMaggots}). */
    private void spawnMaggots(ServerLevel level) {
        int count = 1 + this.random.nextInt(4);
        for (int i = 0; i < count; i++) {
            MoCEntityMaggot maggot = MoCEntities.MAGGOT.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (maggot != null) {
                maggot.setPos(getX(), getY(), getZ());
                maggot.setYRot(this.random.nextFloat() * 360.0F);
                level.addFreshEntity(maggot);
            }
        }
    }

    /**
     * Spawns a tamed baby ghost horse at the carcass, inheriting this horse's owner. A flyer leaves a
     * winged ghost (type 21); otherwise a wingless ghost (type 22). Legacy {@code onDeath}.
     */
    private void spawnGhostFoal(ServerLevel level) {
        MoCEntityHorse ghost = MoCEntities.HORSE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (ghost == null) {
            return;
        }
        ghost.setPos(getX(), getY(), getZ());
        ghost.setYRot(getYRot());
        ghost.setTypeMoC(isFlyer() ? 21 : 22);
        ghost.setTamed(true);
        ghost.setOwnerName(getOwnerName());
        ghost.setAdult(false);
        ghost.setMoCAge(1); // freshly-born ghost foal (legacy setEdad(1))
        level.addFreshEntity(ghost);
        level.playSound(null, blockPosition(), MoCSounds.APPEARMAGIC.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("HorseArmor", getArmor());
        output.putInt("TransformCounter", this.transformCounter);
        output.putInt("TransformType", this.transformTargetType);
        output.putInt("Temper", this.temper);
        output.putBoolean("EatenPumpkin", this.eatenPumpkin);
        output.putBoolean("ChestedHorse", hasChest());
        ValueOutput.ValueOutputList items = output.childrenList("ChestItems");
        for (int i = 0; i < this.chest.getContainerSize(); i++) {
            ItemStack s = this.chest.getItem(i);
            if (!s.isEmpty()) {
                ValueOutput child = items.addChild();
                child.putInt("Slot", i);
                child.store("Item", ItemStack.CODEC, s);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setArmor(input.getIntOr("HorseArmor", 0));
        this.transformCounter = input.getIntOr("TransformCounter", 0);
        this.transformTargetType = input.getIntOr("TransformType", 0);
        this.temper = input.getIntOr("Temper", 0);
        this.eatenPumpkin = input.getBooleanOr("EatenPumpkin", false);
        setHasChest(input.getBooleanOr("ChestedHorse", false));
        this.chest.clearContent();
        for (ValueInput child : input.childrenListOrEmpty("ChestItems")) {
            int slot = child.getIntOr("Slot", -1);
            if (slot >= 0 && slot < this.chest.getContainerSize()) {
                child.read("Item", ItemStack.CODEC).ifPresent(s -> this.chest.setItem(slot, s));
            }
        }
    }

    /** True for the magic/undead/special coats that only breed true once both parents have eaten their essence. */
    public static boolean isSpecialCoat(int t) {
        return t >= 21;
    }

    /**
     * Coat genetics — a faithful port of the legacy {@code HorseGenetics} table: same-coat parents breed
     * true; zebra/donkey crosses give zorse/mule/zonky; a rare (magic/undead) coat crossed with an ordinary
     * one yields the ordinary coat; two different rare coats give a random ordinary coat; a unicorn + white
     * pegasus both vanish and leave a white fairy; and ordinary crosses use the ~40-entry hybrid-coat table
     * (with a 25%/25% either-parent chance unless {@code easyBreeding} is on).
     */
    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        MoCEntityHorse foal = MoCEntities.HORSE.get().create(level, EntitySpawnReason.BREEDING);
        if (foal != null) {
            MoCEntityHorse other = (partner instanceof MoCEntityHorse mate) ? mate : null;
            int coat = (other != null) ? horseGenetics(this, other) : this.getTypeMoC();
            foal.setTypeMoC(coat);
            foal.setAdult(false);
            foal.setAge(-24000); // vanilla baby age -> renders as a foal and grows up
            // Legacy onLivingUpdate breeding (setOwner + setTamed(true)): a bred foal is a TAMED pet owned by
            // the parent's owner, so it inherits the owner and — being tamed — persists instead of despawning.
            foal.setTamed(true);
            foal.setOwnerName(this.getOwnerName());
            // Legacy: both parents' eatenpumpkin breeding gate resets after producing a foal (computed above,
            // so the special-coat inheritance inside horseGenetics still sees the true flags first).
            this.eatenPumpkin = false;
            if (other != null) {
                other.eatenPumpkin = false;
            }
        }
        return foal;
    }

    /**
     * Legacy {@code ReadyforParenting}: horses only breed while tamed, adult, not being ridden or riding, and
     * having eaten a pumpkin / their own essence — and never as an undead, ghost, zorse (61) or mule/zonky
     * (type &gt;= 66). Both partners must qualify, so sterile hybrids and undead/ghost pairs cannot breed.
     */
    @Override
    public boolean canMate(Animal other) {
        if (!(other instanceof MoCEntityHorse mate)) {
            return false;
        }
        if (!readyForParenting(this) || !readyForParenting(mate)) {
            return false;
        }
        return super.canMate(other);
    }

    private static boolean readyForParenting(MoCEntityHorse h) {
        int t = h.getTypeMoC();
        return !h.isVehicle() && !h.isPassenger() && h.getIsTamed() && h.eatenPumpkin && h.getIsAdult()
                && !h.isUndead() && !h.isGhost() && t != 61 && t < 66;
    }

    /** Legacy {@code HorseGenetics}: the foal's coat type from the two parents' coats. */
    private int horseGenetics(MoCEntityHorse a, MoCEntityHorse b) {
        boolean easy = drzhark.mocreatures.config.MoCConfig.get().easyBreeding;
        int typeA = a.getTypeMoC();
        int typeB = b.getTypeMoC();
        if (typeA == typeB) {
            return typeA; // identical coats breed true
        }
        if ((typeA == 60 && typeB < 21) || (typeB == 60 && typeA < 21)) {
            return 61; // zebra + ordinary -> zorse
        }
        if ((typeA == 65 && typeB < 21) || (typeB == 65 && typeA < 21)) {
            return 66; // donkey + ordinary -> mule
        }
        if ((typeA == 60 && typeB == 65) || (typeB == 60 && typeA == 65)) {
            return 67; // zebra + donkey -> zonky
        }
        if ((typeA > 20 && typeB < 21) || (typeB > 20 && typeA < 21)) {
            return Math.min(typeA, typeB); // rare + ordinary -> the ordinary coat
        }
        if ((typeA == 36 && typeB == 39) || (typeB == 36 && typeA == 39)) {
            a.discard(); // unicorn + white pegasus: both vanish, leaving a white fairy
            b.discard();
            return 50;
        }
        if (typeA > 20 && typeB > 20) {
            // Two DIFFERENT rare/magic coats always dilute to a random ordinary coat 1-5 (legacy HorseGenetics:
            // typeA>20 && typeB>20 && typeA!=typeB -> rand.nextInt(5)+1). Identical rare coats already bred true
            // at the top of this method, so there is no eatenPumpkin-gated special inheritance for a mixed pair.
            return this.random.nextInt(5) + 1;
        }
        int chance = this.random.nextInt(4) + 1;
        if (!easy) {
            if (chance == 1) {
                return typeA; // 25% first parent
            }
            if (chance == 2) {
                return typeB; // 25% second parent
            }
        }
        int result = hybridCoat(Math.min(typeA, typeB), Math.max(typeA, typeB));
        return result != 0 ? result : typeA; // fall back to the first parent when the pair isn't tabled
    }

    /** The hybrid-coat table for ordinary parents, keyed by the sorted (lo,hi) coat pair. 0 = not tabled. */
    private static int hybridCoat(int lo, int hi) {
        return switch (lo * 100 + hi) {
            case 102 -> 6;  case 103 -> 2;  case 104 -> 7;  case 105 -> 9;  case 107 -> 12;
            case 108 -> 7;  case 109 -> 13; case 111 -> 12; case 112 -> 13; case 117 -> 16;
            case 204 -> 3;  case 205 -> 4;  case 207 -> 8;  case 208 -> 3;  case 212 -> 6;
            case 216 -> 13; case 217 -> 12;
            case 304 -> 8;  case 305 -> 8;  case 306 -> 2;  case 307 -> 11; case 309 -> 8;
            case 312 -> 11; case 316 -> 11; case 317 -> 11;
            case 406 -> 3;  case 407 -> 8;  case 409 -> 7;  case 411 -> 7;  case 412 -> 7;
            case 413 -> 7;  case 416 -> 13; case 417 -> 5;
            case 506 -> 4;  case 507 -> 4;  case 508 -> 4;  case 511 -> 17; case 512 -> 13;
            case 513 -> 16; case 516 -> 17;
            case 608 -> 2;  case 617 -> 7;
            case 716 -> 13;
            case 811 -> 7;  case 812 -> 7;  case 813 -> 7;  case 816 -> 7;  case 817 -> 7;
            case 916 -> 13;
            case 1116 -> 13; case 1117 -> 7;
            case 1216 -> 13;
            case 1317 -> 9;
            default -> 0;
        };
    }

    // --------------------------------------------------------------- whip effects (legacy MoCItemWhip)
    /**
     * Whip crack on a ridden horse (legacy: a ridden nightmare belched a fiery burst — {@code
     * setNightmareInt(250)} — while any other ridden horse got a sprint kick, {@code sprintCounter = 1}).
     * Faithful achievable subset: a nightmare (type 38) emits a flame burst AND surges forward; any other
     * ridden horse just surges forward. Returns {@code true} when the horse was ridden (crack consumed);
     * {@code false} for an unridden horse, so the whip falls back to its default sit/stay toggle (the
     * legacy unridden branch toggled an eating state the port lacks). Server-side.
     */
    public boolean whipCrack() {
        if (!this.isVehicle()) {
            return false; // unridden: let the whip apply its default sit/stay toggle
        }
        // Legacy whip: a ridden nightmare belches a fiery gallop (nightmareInt) while any OTHER ridden horse
        // kicks off a sprint (sprintCounter = 1) — the sustained 1.5x charge + unicorn/fairy buckle from tick().
        if (!isNightmare() && this.sprintCounter == 0) {
            this.sprintCounter = 1;
        }
        float yaw = getYRot() * ((float) Math.PI / 180.0F);
        double push = isNightmare() ? 1.1D : 0.9D;
        double vx = -Math.sin(yaw) * push;
        double vz = Math.cos(yaw) * push;
        setDeltaMovement(getDeltaMovement().add(vx, 0.0D, vz));
        this.hurtMarked = true;
        // Nightmare: a fiery burst (legacy nightmareInt gallop). Flame particles + a whiff of fire on the
        // ground ahead, played server-side so all nearby clients see it.
        if (isNightmare() && this.level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    getX() + vx, getY() + getBbHeight() * 0.5D, getZ() + vz,
                    30, 0.4D, 0.4D, 0.4D, 0.05D);
        }
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    /** Legacy {@code getMaxHealth()} per coat tier (15/20/25 ordinary, 30 magic, 40 black-pegasus, etc.). */
    private double maxHealthForType() {
        int t = getTypeMoC();
        if (t < 6) return 15.0D;                 // tier 1
        if (t < 11) return 20.0D;                // tier 2
        if (t < 26) return 25.0D;                // tiers 3-4 + ghost/undead
        if (t < 30) return 15.0D;                // skeleton
        if (t < 40) return 30.0D;                // magic (unicorn/pegasus/bat)
        if (t == 40) return 40.0D;               // black pegasus
        return 20.0D;                            // fairies / zebra / donkey / etc.
    }

    /** Legacy {@code getCustomSpeed()} per coat tier (0.9 tier1 -> 1.3 fairies; ghost/donkey slower), with the
     *  whip-sprint multiplier applied (1.5x during the burst phase, 0.5x while exhausted). */
    private double getCustomSpeed() {
        int t = getTypeMoC();
        double s;
        if (t < 6) s = 0.9D;
        else if (t < 11) s = 1.0D;
        else if (t < 16) s = 1.1D;
        else if (t < 21) s = 1.2D;
        else if (t < 26) s = 0.8D;                 // ghost / undead
        else if (t < 30) s = 1.0D;                 // skeleton
        else if (t > 30 && t < 40) s = 1.2D;       // magic (unicorn / pegasus / bat) — bug horse (30) falls through
        else if (t > 40 && t < 60) s = 1.3D;       // fairies — black pegasus (40) falls through to 0.8
        else if (t == 60 || t == 61) s = 1.1D;     // zebra / zorse
        else if (t == 65) s = 0.7D;                // donkey
        else if (t > 65) s = 0.9D;                 // mule / zonky
        else s = 0.8D;                             // black pegasus / bug horse
        // Whip-sprint (legacy getCustomSpeed): a 1.5x burst (phase 1) then a 0.5x exhausted phase (phase 2).
        int phase = this.entityData.get(SPRINT_PHASE);
        if (phase == 1) {
            s *= 1.5D;
        } else if (phase == 2) {
            s *= 0.5D;
        }
        return s;
    }

    /** Legacy {@code getCustomJump()} per coat tier (0.4 tier1 -> 0.6 pegasus/fairies). */
    @Override
    protected double getCustomJump() {
        int t = getTypeMoC();
        if (t < 6) return 0.4D;
        if (t < 11) return 0.45D;
        if (t < 16) return 0.5D;
        if (t < 21) return 0.55D;
        if (t < 26) return 0.45D;                // ghost / undead
        if (t < 30) return 0.5D;                 // skeleton
        if (t < 40) return 0.55D;                // magic
        if (t < 60) return 0.6D;                 // black pegasus + fairies
        return 0.45D;                            // zebra / donkey
    }

    /** Ridden ground speed scales with the coat tier (legacy getCustomSpeed); flight uses travel()'s own model. */
    @Override
    protected float getRiddenSpeed(net.minecraft.world.entity.player.Player rider) {
        return (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * getCustomSpeed());
    }

    /** Keeps the per-tier MAX_HEALTH attribute in sync with the current coat (auto-applies on spawn and after
     * any breeding/essence coat change, without hooking every setTypeMoC call site). Ridden speed and jump are
     * driven live by {@link #getRiddenSpeed}/{@link #getCustomJump}, so they need no attribute sync. */
    private void applyTypeStats() {
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            double want = maxHealthForType();
            if (hp.getBaseValue() != want) {
                boolean wasFull = this.getHealth() >= this.getMaxHealth();
                hp.setBaseValue(want);
                if (wasFull) {
                    this.setHealth(this.getMaxHealth());
                }
            }
        }
    }

    @Override
    public net.minecraft.world.entity.@Nullable SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, EntitySpawnReason spawnReason,
            net.minecraft.world.entity.@Nullable SpawnGroupData groupData) {
        // Legacy checkSpawningBiome: zebras (type 60) spawn preferentially in Plains (~1/3), overriding the
        // flat zebraChance distribution used in every other biome. Set BEFORE super's selectType() runs, which
        // is a no-op once a coat is already chosen.
        if (getTypeMoC() == 0
                && level.getBiome(blockPosition()).is(net.minecraft.world.level.biome.Biomes.PLAINS)
                && this.random.nextInt(3) == 0) {
            setTypeMoC(60);
        }
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            // ~20% of wild horses spawn as foals (legacy MoCEntityHorse.selectType).
            if (this.random.nextInt(5) == 0) {
                setAdult(false);
            }
            // Faithful legacy distribution: ordinary coats 1-5, then donkey (65) and — scaled by the
            // zebraChance config — zebra (60). With the default zebraChance=5 that is ~9% donkey / ~5% zebra.
            int j = this.random.nextInt(100);
            int i = drzhark.mocreatures.config.MoCConfig.get().zebraChance;
            if (j <= 18 - i) {
                setTypeMoC(1);
            } else if (j <= 36 - i) {
                setTypeMoC(2);
            } else if (j <= 54 - i) {
                setTypeMoC(3);
            } else if (j <= 72 - i) {
                setTypeMoC(4);
            } else if (j <= 90 - i) {
                setTypeMoC(5);
            } else if (j <= 99 - i) {
                setTypeMoC(65); // donkey
            } else {
                setTypeMoC(60); // zebra
            }
        }
    }

    @Override
    public Identifier getTexture() {
        String base = switch (getTypeMoC()) {
            case 1 -> "horsewhite";
            case 2 -> "horsecreamy";
            case 3 -> "horsebrown";
            case 4 -> "horsedarkbrown";
            case 5 -> "horseblack";
            case 6 -> "horsebrightcreamy";
            case 7 -> "horsespeckled";
            case 8 -> "horsepalebrown";
            case 9 -> "horsegrey";
            case 11 -> "horsepinto";
            case 12 -> "horsebrightpinto";
            case 13 -> "horsepalespeckles";
            case 16 -> "horsespotted";
            case 17 -> "horsecow";
            case 21 -> "horseghost";
            case 22 -> "horseghostb";
            case 23 -> "horseundead";
            case 24 -> "horseundeadunicorn";
            case 25 -> "horseundeadpegasus";
            case 26 -> "horseskeleton";
            case 27 -> "horseunicornskeleton";
            case 28 -> "horsepegasusskeleton";
            case 30 -> "horsebug";
            case 32 -> "horsebat";
            case 36 -> "horseunicorn";
            case 38 -> "horsenightmare";
            case 39 -> "horsepegasus";
            case 40 -> "horsedarkpegasus";
            case 48 -> "horsefairyyellow";
            case 49 -> "horsefairypurple";
            case 50 -> "horsefairywhite";
            case 51 -> "horsefairyblue";
            case 52 -> "horsefairypink";
            case 53 -> "horsefairylightgreen";
            case 54 -> "horsefairyblack";
            case 55 -> "horsefairyred";
            case 56 -> "horsefairydarkblue";
            case 57 -> "horsefairycyan";
            case 58 -> "horsefairygreen";
            case 59 -> "horsefairyorange";
            case 60 -> "horsezebra";
            case 61 -> "horsezorse";
            case 65 -> "horsedonkey";
            case 66 -> "horsemule";
            case 67 -> "horsezonky";
            default -> "horsebug";
        };
        // Nightmare fire flicker (legacy animateTextures): an unarmoured nightmare (type 38) cycles through
        // horsenightmare1-5 for animated flames when the config is on; static horsenightmare.png when off.
        if (getTypeMoC() == 38 && getArmor() == 0
                && drzhark.mocreatures.config.MoCConfig.get().animateTextures) {
            return modelTexture("horsenightmare" + (((this.tickCount / 3) % 5) + 1) + ".png");
        }
        // Armoured coats are a full-texture swap: ordinary horses take metal/gold/diamond, magic horses crystal.
        // The (isArmored() || isMagicHorse()) gate is required so a coat that has no armoured variant (e.g. a
        // zebra) never requests a non-existent texture.
        if (getArmor() > 0 && (isArmored() || isMagicHorse())) {
            String suffix = switch (getArmor()) {
                case 1 -> "metal";
                case 2 -> "gold";
                case 3 -> "diamond";
                case 4 -> "crystaline";
                default -> "";
            };
            return modelTexture(base + suffix + ".png");
        }
        return modelTexture(base + ".png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        int type = getTypeMoC();
        if (type == 60 || type == 61) {
            return MoCSounds.ZEBRAGRUNT.get();
        }
        if (type >= 65 && type <= 67) {
            return MoCSounds.DONKEYGRUNT.get();
        }
        if (type > 20 && type < 26) {
            return MoCSounds.HORSEGRUNTUNDEAD.get();
        }
        if (type == 21 || type == 22) {
            return MoCSounds.HORSEGRUNTGHOST.get();
        }
        return MoCSounds.HORSEGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        int type = getTypeMoC();
        if (type == 60 || type == 61) {
            return MoCSounds.ZEBRAHURT.get();
        }
        if (type >= 65 && type <= 67) {
            return MoCSounds.DONKEYHURT.get();
        }
        if (type > 20 && type < 26) {
            return MoCSounds.HORSEHURTUNDEAD.get();
        }
        if (type == 21 || type == 22) {
            return MoCSounds.HORSEHURTGHOST.get();
        }
        return MoCSounds.HORSEHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        int type = getTypeMoC();
        if (type == 60 || type == 61) {
            return MoCSounds.ZEBRAHURT.get();
        }
        if (type >= 65 && type <= 67) {
            return MoCSounds.DONKEYDYING.get();
        }
        if (type > 20 && type < 26) {
            return MoCSounds.HORSEDYINGUNDEAD.get();
        }
        if (type == 21 || type == 22) {
            return MoCSounds.HORSEDYINGGHOST.get();
        }
        return MoCSounds.HORSEDYING.get();
    }
}
