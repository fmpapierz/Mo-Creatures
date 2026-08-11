package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityBigCat}. A large feline with seven coat variants
 * (lion, lioness, panther, cheetah, tiger, snow leopard, white tiger).
 */
public class MoCEntityBigCat extends MoCAnimal {

    public MoCEntityBigCat(EntityType<? extends MoCEntityBigCat> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Wild-predation target selector (legacy findPlayerToAttack -> getClosestTarget): a HUNGRY, untamed
        // adult big cat also hunts nearby passive creatures and weaker rival big cats, not just players. All
        // the per-target exclusions and the big-cat rivalry (including the config-gated horse/wolf hunting)
        // live in canHunt(). The goal is installed unconditionally (registerGoals runs before the type/tamed
        // state is resolved); its predicate does the runtime gating. The base MoCAnimal already installs the
        // higher-priority player-target goal (priority 2) and the melee attack goal that runs prey down.
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, Animal.class, 10, true, false,
                (living, serverLevel) -> getIsHungry()
                        && serverLevel.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                        && canHunt(living)));
        // Legacy getClosestTarget also let a HUNGRY, TAMED, ADULT big cat hunt hostile mobs (EntityMob was only
        // eligible prey when tamed && adult). Cubs and untamed cats never take hostile prey — canHunt enforces
        // that too, but the predicate short-circuits the class scan here.
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.monster.Monster.class, 10, true, false,
                (living, serverLevel) -> getIsHungry()
                        && serverLevel.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                        && getIsTamed() && getIsAdult() && canHunt(living)));
    }

    /**
     * Legacy {@code getClosestTarget} predation filter: which nearby creature a HUNGRY big cat will hunt.
     * Mirrors the legacy exclusion list — a cub takes only small prey (bb &lt;= 0.5), hostile mobs are prey
     * only for a tamed adult, a tamed cat won't hunt another tamed Mo'Creature, never hunts elephants or the
     * kitty furniture (bed / litter box), and only hunts horses or wolves when the matching config flag is on
     * — plus the big-cat rivalry rules (adults only): a cat preys on another big cat only when it is at least
     * as healthy, never on a white tiger (type 7) nor on its own coat (unless it is a lioness), and a lioness
     * never turns on a lion (type 2 skips type 1 — she joins his pride instead).
     */
    private boolean canHunt(LivingEntity target) {
        if (target == this) {
            return false;
        }
        // A CUB only takes small prey: legacy skipped any target wider or taller than 0.5 for a non-adult.
        if (!getIsAdult() && (target.getBbWidth() > 0.5D || target.getBbHeight() > 0.5D)) {
            return false;
        }
        if (target instanceof MoCEntityElephant
                || target instanceof MoCEntityKittyBed
                || target instanceof MoCEntityLitterBox) {
            return false;
        }
        // Hostile mobs are prey only for a TAMED ADULT (legacy: EntityMob skipped unless tamed && adult).
        if (target instanceof net.minecraft.world.entity.monster.Monster
                && (!getIsTamed() || !getIsAdult())) {
            return false;
        }
        // A tamed cat won't turn on another tamed Mo'Creature (legacy MoCIMoCreature.getIsTamed() skip).
        if (getIsTamed() && target instanceof drzhark.mocreatures.entity.IMoCEntity moc && moc.getIsTamed()) {
            return false;
        }
        drzhark.mocreatures.config.MoCConfig cfg = drzhark.mocreatures.config.MoCConfig.get();
        if (target instanceof MoCEntityHorse && !cfg.attackHorses) {
            return false;
        }
        if (target instanceof net.minecraft.world.entity.animal.wolf.Wolf && !cfg.attackWolves) {
            return false;
        }
        if (target instanceof MoCEntityBigCat other) {
            // Big-cat rivalry is an adults-only affair (legacy skipped cub big-cat targets outright).
            if (!getIsAdult()) {
                return false;
            }
            int myType = getTypeMoC();
            int otherType = other.getTypeMoC();
            if ((getIsTamed() && other.getIsTamed())
                    || otherType == 7
                    || (myType != 2 && myType == otherType)
                    || (myType == 2 && otherType == 1)
                    || getHealth() < other.getHealth()) {
                return false;
            }
        }
        return true;
    }

    // Legacy hunger: a wild big cat only hunts players while HUNGRY (lions/tigers/white tigers commit always,
    // the shyer cats only 1/30), regaining its appetite over time. A tamed cat also defends its owner.
    private static final EntityDataAccessor<Boolean> HUNGRY =
            SynchedEntityData.defineId(MoCEntityBigCat.class, EntityDataSerializers.BOOLEAN);
    /** Synched: the maw is open for a roar/bite; drives the lower-jaw drop client-side. */
    private static final EntityDataAccessor<Boolean> OPEN_JAW =
            SynchedEntityData.defineId(MoCEntityBigCat.class, EntityDataSerializers.BOOLEAN);

    /** Server-side countdown holding the jaw open after a bite; decremented each tick. */
    private int jawTicks;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HUNGRY, true);
        builder.define(OPEN_JAW, false);
    }

    /** Legacy medallion taming needs a cub that has already eaten (see {@link #customServerAiStep}). */
    @Override
    protected boolean requiresFeedingBeforeTaming() {
        return true;
    }

    public boolean getIsHungry() {
        return this.entityData.get(HUNGRY);
    }

    public void setHungry(boolean hungry) {
        this.entityData.set(HUNGRY, hungry);
    }

    /** True for a few ticks after a bite — the client drops the lower jaw for the roar/bite pose. */
    public boolean getJawOpen() {
        return this.entityData.get(OPEN_JAW);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Hungry", getIsHungry());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setHungry(input.getBooleanOr("Hungry", true));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Legacy findPlayerToAttack()/attackEntityFrom() acquired a player/creature target and retaliated only
        // inside `worldObj.difficultySetting > 0`, so on Peaceful a WILD big cat stood down entirely — it took
        // no prey and never rounded on an attacker. Clear the target each tick for an untamed cat on Peaceful:
        // this suppresses the base MoCAnimal player-target goal and HurtByTargetGoal (which carry no difficulty
        // gate) as well; the newly Peaceful-gated prey goals no longer fire. The non-combat living updates below
        // (regen, cub maturation, feed-to-tame, pride grouping) still run, exactly as legacy onLivingUpdate ran
        // regardless of difficulty.
        if (!getIsTamed() && level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            setTarget(null);
        }
        RandomSource r = this.getRandom();
        // Keep the per-coat base stats applied (legacy getMaxHealth/getForce/getMoveSpeed/getAttackRange
        // varied every one per type). Covers spawn and the rare in-world type change (lioness -> lion below).
        applyTypeStats();
        if (this.jawTicks > 0 && --this.jawTicks == 0 && getJawOpen()) {
            this.entityData.set(OPEN_JAW, false); // relax the jaw after the bite
        }
        if (r.nextInt(300) == 0 && getHealth() < getMaxHealth()) {
            heal(1.0F); // slow regen (legacy)
        }
        // Cub maturation (legacy onLivingUpdate): a non-adult big cat slowly ages and turns adult at edad 100,
        // re-enabling the cub -> eat -> medallion taming path. Mirrors MoCEntityDeer fawn growth.
        if (!getIsAdult() && r.nextInt(250) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= 100) {
                setAdult(true);
            }
        }
        if (!getIsHungry() && !isSitting() && r.nextInt(200) == 0) {
            setHungry(true); // gets hungry again
        }
        // Only a hungry cat presses a hunt on a player; types 1/5/7 always commit, others 1/30, spending hunger.
        if (getTarget() instanceof Player) {
            if (!getIsHungry()) {
                setTarget(null);
            } else {
                int t = getTypeMoC();
                if ((t == 1 || t == 5 || t == 7) || r.nextInt(30) == 0) {
                    setHungry(false);
                } else {
                    setTarget(null);
                }
            }
        } else if (getIsHungry() && getTarget() != null) {
            // Locked onto prey (a non-player creature): spend the appetite, exactly as legacy
            // findPlayerToAttack set hungry=false once getClosestTarget returned a victim. The target
            // persists (the target goal's canContinueToUse does not re-check hunger), so the cat keeps
            // the chase and simply will not pick a NEW victim until it grows hungry again.
            setHungry(false);
        }
        // Signature POUNCE (legacy attackEntity): while a grounded cat is closing on its target from
        // 2-6 blocks out it randomly (1/50) launches a horizontal-plus-upward leap to close the gap
        // BEFORE it can bite (the melee bite only lands once it is adjacent). No adult/tamed gate — cubs
        // and tamed cats defending their owner pounce too.
        LivingEntity leapTarget = getTarget();
        if (leapTarget != null && this.onGround()) {
            float f = this.distanceTo(leapTarget);
            if (f > 2.0F && f < 6.0F && r.nextInt(50) == 0) {
                double dx = leapTarget.getX() - this.getX();
                double dz = leapTarget.getZ() - this.getZ();
                double horiz = Math.sqrt(dx * dx + dz * dz);
                if (horiz > 1.0E-4D) {
                    Vec3 v = this.getDeltaMovement();
                    // Legacy: motion = (delta/horiz)*0.5*0.8 + motion*0.2 ; motionY = 0.4
                    double mx = (dx / horiz) * 0.5D * 0.8D + v.x * 0.2D;
                    double mz = (dz / horiz) * 0.5D * 0.8D + v.z * 0.2D;
                    this.setDeltaMovement(mx, 0.4D, mz);
                    this.hurtMarked = true; // sync the impulse to clients (legacy hasImpulse)
                }
            }
        }
        // Owner defence: a tamed cat rounds on whatever recently attacked its owner (vanilla clears the
        // owner's last-attacker after ~100 ticks, so this only reacts to fresh attacks).
        if (getIsTamed() && getTarget() == null) {
            Player owner = findOwner(level);
            if (owner != null) {
                LivingEntity foe = owner.getLastHurtByMob();
                if (foe != null && foe.isAlive() && foe != this) {
                    setTarget(foe);
                }
            }
        }
        // Pride grouping: a lone lioness (type 2) among other adult big cats occasionally matures into a
        // lion (type 1) to head the pride (legacy checkNearBigKitties).
        if (getTypeMoC() == 2 && getIsAdult() && r.nextInt(500) == 0
                && !level.getEntitiesOfClass(MoCEntityBigCat.class, this.getBoundingBox().inflate(8.0D),
                        o -> o != this && o.getIsAdult()).isEmpty()) {
            setTypeMoC(1);
        }
        // Ground-food self-heal (legacy onLivingUpdate): a hurt big cat seeks a nearby dropped raw pork/fish,
        // eats it and fully heals. Legacy actually sought food whenever HUNGRY (health-independent); the port
        // narrows that to a self-heal, but a hungry cub that has NOT yet eaten must still seek meat so it can
        // set the medallion-taming "eaten" flag before it matures — otherwise wild cubs (spawned at full
        // health) never eat and big cats stay untameable.
        if ((getHealth() < getMaxHealth() || (!getIsAdult() && getIsHungry() && !getHasEatenMoC()))
                && r.nextInt(20) == 0) {
            net.minecraft.world.entity.item.ItemEntity food = null;
            double best = Double.MAX_VALUE;
            for (net.minecraft.world.entity.item.ItemEntity ie : level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, this.getBoundingBox().inflate(12.0D),
                    e -> e.isAlive() && (e.getItem().is(net.minecraft.world.item.Items.PORKCHOP)
                            || e.getItem().is(net.minecraft.world.item.Items.COD)))) {
                double d = ie.distanceToSqr(this);
                if (d < best) {
                    best = d;
                    food = ie;
                }
            }
            if (food != null) {
                if (best < 2.25D) {
                    food.getItem().shrink(1);
                    if (food.getItem().isEmpty()) {
                        food.discard();
                    }
                    setHealth(getMaxHealth());
                    // Legacy onLivingUpdate: a cub (age < 80) that ate meat sets the "eaten" flag, which the
                    // base-class MEDALLION taming gate now requires for big cats — without this a big cat is
                    // permanently untameable.
                    if (!getIsAdult() && getMoCAge() < 80) {
                        setHasEatenMoC(true);
                    }
                    // Legacy cleared hunger and played the "eating" sound (and did NOT age the cub from a meal).
                    setHungry(false);
                    level.playSound(null, blockPosition(), MoCSounds.EATING.get(),
                            net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                            1.0F + (r.nextFloat() - r.nextFloat()) * 0.2F);
                } else {
                    this.getNavigation().moveTo(food, 1.2D);
                }
            }
        }
    }

    /** The nearby player whose name matches this tamed cat's owner (name-based ownership), or null. */
    @Nullable
    private Player findOwner(ServerLevel level) {
        String owner = getOwnerName();
        if (owner == null || owner.isEmpty()) {
            return null;
        }
        Player p = level.getNearestPlayer(this, 24.0D);
        return (p != null && owner.equals(p.getName().getString())) ? p : null;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D);
    }

    @Override
    public void selectType() {
        boolean freshlySelected = false;
        if (getTypeMoC() == 0) {
            // Snow leopard (type 4) spawns only in snowy/frozen biomes (legacy checkSpawningBiome); in warm
            // biomes the same slot becomes a leopard (type 6), which was otherwise unreachable.
            if (this.level().getBiome(this.blockPosition()).value().getBaseTemperature() <= 0.15F) {
                setTypeMoC(4); // snow leopard
            } else {
                int i = this.random.nextInt(100);
                if (i <= 5) {
                    setTypeMoC(1);
                } else if (i <= 25) {
                    setTypeMoC(2);
                } else if (i <= 50) {
                    setTypeMoC(3);
                } else if (i <= 70) {
                    setTypeMoC(6); // leopard (warm-biome counterpart of the snow leopard)
                } else if (i <= 75) {
                    setTypeMoC(7);
                } else {
                    setTypeMoC(5);
                }
            }
            // Legacy constructor rolled 1-in-4 fresh big cats as CUBS (setAdult(false), edad 35). A wild cub
            // that eats a dropped raw pork/fish (while age < 80) sets the "eaten" flag — the sole prerequisite
            // for medallion-taming a big cat — and later matures back to an adult in customServerAiStep. Without
            // this, natural spawns are 100% adults, the eaten flag can never be set, and big cats are untameable.
            if (this.random.nextInt(4) == 0) {
                setAdult(false);
                setMoCAge(35);
            }
            freshlySelected = true;
        }
        // Derive the per-coat base stats from the chosen type (legacy varied health/force/speed/range per
        // coat), then — only for a freshly spawned cat — fill health to the type's max (legacy selectType:
        // health = getMaxHealth()). A cat loaded from disk keeps its stored health.
        applyTypeStats();
        if (freshlySelected) {
            setHealth(getMaxHealth());
        }
    }

    /**
     * Per-coat max health. Legacy per-biome outcome: the COLD-biome snow leopard had 25 hp and the common
     * warm cat 20 hp. The 26.2 texture remap (cold -> type 4, warm -> type 6) is kept, so the legacy type-4
     * and type-6 stat sets are swapped here to preserve those per-biome stats: type 4 (cold snow leopard) 25,
     * type 6 (warm leopard) 20. Others: lioness 30, panther 20, tiger 35, white tiger 40, lion 25.
     */
    private int getMaxHealthMoC() {
        return switch (getTypeMoC()) {
            case 2 -> 30;
            case 3, 6 -> 20; // panther, warm-biome leopard
            case 5 -> 35;
            case 7 -> 40;
            default -> 25; // types 1 (lion) and 4 (cold-biome snow leopard)
        };
    }

    /** Per-coat attack damage (legacy {@code getForce}): 5/5/4/3/6/3/8 for types 1-7. */
    private int getForceMoC() {
        return switch (getTypeMoC()) {
            case 3 -> 4;
            case 4, 6 -> 3;
            case 5 -> 6;
            case 7 -> 8;
            default -> 5; // types 1, 2
        };
    }

    /**
     * Per-coat move speed. Legacy per-biome outcome: the warm-biome cat was the quick 1.9 cat and the cold
     * snow leopard the slower 1.7. With the texture remap (cold -> type 4, warm -> type 6) the legacy type-4
     * and type-6 values are swapped: type 6 (warm leopard) 1.9, type 4 (cold snow leopard) 1.7. Scaled by 0.2
     * into the vanilla attribute range (same convention the dolphin uses for its per-colour speed tiers).
     */
    private double getMoveSpeedMoC() {
        return switch (getTypeMoC()) {
            case 3, 5 -> 1.6D;
            case 6 -> 1.9D;
            case 4, 7 -> 1.7D;
            default -> 1.4D; // types 1, 2
        };
    }

    /**
     * Per-coat aggro / attack range. Legacy per-biome outcome: the cold snow leopard spotted prey at 4, the
     * warm cat at 6. With the texture remap (cold -> type 4, warm -> type 6) the legacy type-4 and type-6
     * values are swapped: type 4 (cold snow leopard) 4, type 6 (warm leopard) 6. Others: lion/tiger 8, lioness
     * 4, panther 6, white tiger 10. Driven onto FOLLOW_RANGE so both the player-target and predation goals
     * detect at this radius.
     */
    private double getAttackRangeMoC() {
        return switch (getTypeMoC()) {
            case 2, 4 -> 4.0D;
            case 3, 6 -> 6.0D;
            case 1, 5 -> 8.0D;
            case 7 -> 10.0D;
            default -> 6.0D;
        };
    }

    /**
     * Keeps the per-coat base attributes (max health, attack damage, move speed, aggro range) in sync with
     * the current type. Guards every {@code setBaseValue} behind an equality check so it is a no-op once
     * applied, and only ever clamps health DOWN to a lowered max (the fresh-spawn full heal is done in
     * {@link #selectType()}), so a wounded cat is never silently topped up.
     */
    private void applyTypeStats() {
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            double want = getMaxHealthMoC();
            if (hp.getBaseValue() != want) {
                hp.setBaseValue(want);
                if (getHealth() > want) {
                    setHealth((float) want);
                }
            }
        }
        var atk = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            double want = getForceMoC();
            if (atk.getBaseValue() != want) {
                atk.setBaseValue(want);
            }
        }
        var spd = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            double want = getMoveSpeedMoC() * 0.2D;
            if (spd.getBaseValue() != want) {
                spd.setBaseValue(want);
            }
        }
        var range = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (range != null) {
            double want = getAttackRangeMoC();
            if (range.getBaseValue() != want) {
                range.setBaseValue(want);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 1 -> modelTexture("bcmalelion.png");
            case 2 -> modelTexture("bcfemalelion.png");
            case 3 -> modelTexture("bcpuma.png");        // panther slot: 256x256 feline-layout texture
            case 4 -> modelTexture("bcsnowleopard.png"); // cheetah slot: 256x256 feline-layout texture
            case 5 -> modelTexture("bctiger.png");
            case 6 -> modelTexture("bcleopard.png");
            case 7 -> modelTexture("bcwhitetiger.png");
            default -> modelTexture("bcmalelion.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return getIsAdult() ? MoCSounds.LIONGRUNT.get() : MoCSounds.CUBGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return getIsAdult() ? MoCSounds.LIONHURT.get() : MoCSounds.CUBHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return getIsAdult() ? MoCSounds.LIONDEATH.get() : MoCSounds.CUBDYING.get();
    }

    /**
     * The melee BITE. The base {@link MoCAnimal#doHurtTarget} applies the per-coat attack damage (legacy
     * {@code getForce}, now on the ATTACK_DAMAGE attribute); here we only gape the maw for the bite pose.
     * The signature gap-closing LEAP is launched from range in {@link #customServerAiStep} (legacy
     * {@code attackEntity} pounced BEFORE it was close enough to bite), not after this adjacent hit.
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            // Gape the maw for the bite (a short roar/bite pose).
            this.jawTicks = 8;
            if (!getJawOpen()) {
                this.entityData.set(OPEN_JAW, true);
            }
            // Legacy attackEntity: biting a non-player clears the fresh loot around the kill
            // (MoCTools.destroyDrops(this, 3D) — item entities younger than 50 ticks within 3 blocks),
            // gated on the destroyDrops config flag.
            if (!(target instanceof Player) && drzhark.mocreatures.config.MoCConfig.get().destroyDrops) {
                for (net.minecraft.world.entity.item.ItemEntity ie : level.getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class, this.getBoundingBox().inflate(3.0D))) {
                    if (ie.isAlive() && ie.tickCount < 50) {
                        ie.discard();
                    }
                }
            }
        }
        return hit;
    }

    /**
     * MEDALLION DROP ON DEATH. Legacy dropped a {@code medallion} when a tamed big cat died so its
     * owner could recover the pet's medallion. The base {@link MoCAnimal#dropCustomDeathLoot} already
     * spawns the data-driven claw drop; here we additionally spawn the medallion for a tamed cat.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        if (getIsTamed()) {
            this.spawnAtLocation(level, new ItemStack(MoCItems.MEDALLION.get(), 1));
        }
    }

    /**
     * Legacy {@code dropMyStuff} is an empty method body: releasing a tamed big cat with the Scroll of Freedom
     * dropped NOTHING. Kept as a no-op so the Scroll's generic MoCAnimal release still runs but no extra item is
     * handed back. (The on-death medallion in {@link #dropCustomDeathLoot} is the separate legacy-faithful drop.)
     */
    public void dropWornGear(ServerLevel level) {
        // no-op — mirrors legacy MoCEntityBigCat.dropMyStuff() {}
    }
}
