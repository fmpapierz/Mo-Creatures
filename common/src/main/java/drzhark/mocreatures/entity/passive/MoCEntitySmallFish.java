package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.entity.MoCAquatic;
import drzhark.mocreatures.entity.MoCSchoolGoal;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Port of the legacy {@code MoCEntitySmallFish} and the eight one-line subclasses that made up the species:
 * {@code MoCEntityAnchovy}, {@code MoCEntityAngelFish}, {@code MoCEntityAngler}, {@code MoCEntityClownFish},
 * {@code MoCEntityGoldFish}, {@code MoCEntityHippoTang}, {@code MoCEntityManderin} and
 * {@code MoCEntityPiranha}.
 *
 * <p>Legacy registered each of those as its own {@link EntityType} even though seven of the eight differed
 * only in the texture they returned and the metadata of the egg they dropped — {@code MoCEntityAnchovy} in
 * full is a constructor calling {@code setType(1)}, a {@code getTexture()} and a {@code getEggNumber()}. As
 * with {@link MoCEntityRay} (mantaray / stingray in one class), the port collapses the whole set into one
 * registered creature keyed by {@link #getTypeMoC()}:</p>
 *
 * <table>
 *   <caption>Sub-types</caption>
 *   <tr><td>1</td><td>anchovy</td><td>egg 80</td></tr>
 *   <tr><td>2</td><td>angelfish</td><td>egg 81</td></tr>
 *   <tr><td>3</td><td>angler</td><td>egg 82</td></tr>
 *   <tr><td>4</td><td>clownfish</td><td>egg 83</td></tr>
 *   <tr><td>5</td><td>goldfish</td><td>egg 84</td></tr>
 *   <tr><td>6</td><td>hippotang</td><td>egg 85</td></tr>
 *   <tr><td>7</td><td>manderin</td><td>egg 86</td></tr>
 *   <tr><td>8</td><td>piranha</td><td>egg 90</td></tr>
 * </table>
 *
 * <p><b>The two species that are not just a texture.</b> The <em>piranha</em> replaced the whole AI set: it
 * threw away the panic/flee/wander goals and installed a melee attack, a herd follow and a
 * nearest-player target goal, carried 6 HP instead of 4 and 2 points of attack damage, and reported
 * {@code isNotScared() == true} so nothing could frighten it ({@code MoCEntityPiranha}:26-52, 84-87). The
 * <em>angler</em> is the deep-water one: it is the only species besides the clownfish, hippotang and
 * manderin that legacy's spawn table refused to place in rivers ({@code MoCEntities}:372-381), which is the
 * whole of its difference from its siblings in 12.0.5 — it has no code of its own beyond texture and egg id.
 * Since one merged class can no longer be filtered per-species by the spawner, {@link #selectType()} now
 * applies those biome lists itself.</p>
 */
public class MoCEntitySmallFish extends MoCAquatic {

    public static final int TYPE_ANCHOVY = 1;
    public static final int TYPE_ANGELFISH = 2;
    public static final int TYPE_ANGLER = 3;
    public static final int TYPE_CLOWNFISH = 4;
    public static final int TYPE_GOLDFISH = 5;
    public static final int TYPE_HIPPOTANG = 6;
    public static final int TYPE_MANDERIN = 7;
    public static final int TYPE_PIRANHA = 8;

    /** Legacy {@code MoCEntitySmallFish.getMaxEdad()}:209-212 — a small fish finishes growing at 120. */
    private static final int MAX_AGE = 120;
    /** Legacy {@code applyEntityAttributes}:67-72 — 4 HP for the seven harmless species. */
    private static final double BASE_HEALTH = 4.0D;
    /** Legacy {@code MoCEntityPiranha.applyEntityAttributes}:54-59 — the piranha alone carries 6 HP. */
    private static final double PIRANHA_HEALTH = 6.0D;

    /**
     * Species legacy allowed in rivers: {@code Type.RIVER} appears only in the anchovy, angelfish and
     * goldfish spawn entries ({@code MoCEntities}:368-381).
     */
    private static final int[] RIVER_SPECIES = {TYPE_ANCHOVY, TYPE_ANGELFISH, TYPE_GOLDFISH};
    /** Species legacy allowed in oceans: everything except the goldfish, which is river/swamp/beach only. */
    private static final int[] OCEAN_SPECIES =
            {TYPE_ANCHOVY, TYPE_ANGELFISH, TYPE_ANGLER, TYPE_CLOWNFISH, TYPE_HIPPOTANG, TYPE_MANDERIN};
    /** Every species carries {@code Type.BEACH}, {@code Type.SWAMP} and {@code Type.WATER}, so all seven. */
    private static final int[] ALL_SPECIES = {TYPE_ANCHOVY, TYPE_ANGELFISH, TYPE_ANGLER, TYPE_CLOWNFISH,
            TYPE_GOLDFISH, TYPE_HIPPOTANG, TYPE_MANDERIN};
    /** Legacy spawn weight of each ordinary species ({@code MoCEntities}:368-381: SpawnListEntry(..., 12, 1, 6)). */
    private static final int SPECIES_WEIGHT = 12;
    /** Legacy spawn weight of the piranha ({@code MoCEntities}:360: SpawnListEntry(..., 4, 1, 3)) — far rarer. */
    private static final int PIRANHA_WEIGHT = 4;

    /**
     * Whether the shared egg tables understand the small-fish egg ids (legacy 80-86 for the seven fish and 90
     * for the piranha, from each subclass's {@code getEggNumber()}).
     *
     * <p>They do not yet: {@code MoCEntityEgg.setEggType} only decodes the 1-54 range the 5.1.5 egg table
     * used, and everything outside it falls through to its ostrich branch — so dropping an id-80 egg today
     * would give the player a nameless "Spoiled Egg" that hatches an <em>ostrich</em> out of the sea. Until
     * ids 80-86/90 are taught to {@code MoCEntityEgg.setEggType}/{@code getEggType} and to
     * {@code MoCThrownEggItem.hasOwnName}/{@code eggColour}, the egg half of the death drop is suppressed and
     * only the raw-fish half runs. Flip this to {@code true} in the same change that extends those tables.</p>
     */
    private static final boolean SMALL_FISH_EGGS_SUPPORTED = false;

    /**
     * Legacy {@code MoCEntityAquatic.outOfWater}:474-489 — how long this fish has been beached. It drives the
     * "stop swimming, then start flopping" behaviour of a fish thrown onto the shore.
     */
    private int outOfWater;

    public MoCEntitySmallFish(EntityType<? extends MoCEntitySmallFish> type, Level level) {
        super(type, level);
        // Legacy constructor (MoCEntitySmallFish:23-27): setEdad(70 + rand(30)). MoCEntityAquatic registered
        // its ADULT flag as FALSE by default (MoCEntityAquatic:106), so a wild small fish spawns as a
        // not-quite-grown 0.70-0.99x fish and finishes growing to full size in the world. The port's MoCAquatic
        // defaults ADULT to true instead, so both halves are restated here.
        setAdult(false);
        setMoCAge(70 + this.random.nextInt(30));
    }

    /**
     * Legacy {@code applyEntityAttributes}:67-72 gave every small fish 4 HP, and the piranha subclass raised
     * that to 6 and registered 2 points of attack damage. A merged class has a single attribute supplier, so
     * the shared base is the ordinary fish's and {@link #applyTypeAttributes()} lifts the max health once a
     * fish turns out to be a piranha. The attack damage is declared for everyone but only the piranha ever
     * runs an attack goal, exactly as {@link MoCEntityFishy} handles its own type-10 piranha.
     *
     * <p>Movement speed is the port's uniform aquatic 1.0 rather than legacy's 0.5. Legacy paired that 0.5
     * with a separate {@code getAIMoveSpeed()} of 0.10 that 26.2 has no counterpart for, and every other
     * ported aquatic (dolphin, fishy, ray, shark — all 0.5 in 12.0.5) was normalised to 1.0, so a 0.5 here
     * would leave small fish swimming at half the speed of the fishy they shoal beside.</p>
     */
    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    /** True for the one aggressive sub-type (legacy {@code MoCEntityPiranha}). */
    public boolean isPiranha() {
        return getTypeMoC() == TYPE_PIRANHA;
    }

    /**
     * Legacy piranha aggression gate. {@code MoCEntityPiranha} hunts through
     * {@code EntityAINearestAttackableTargetMoC(this, EntityPlayer.class, true)} with no adult or tamed test
     * of its own, but its (commented-out) {@code findPlayerToAttack} and its {@code attackEntityFrom}
     * retaliation both required a non-Peaceful difficulty and an untamed fish, so those are the gates kept
     * here. Deliberately NOT gated on adulthood: a small fish is only flagged adult at age 120, so an
     * {@code getIsAdult()} test would leave every freshly spawned piranha harmless for most of its life.
     */
    private boolean isPiranhaHostile() {
        return isPiranha() && !getIsTamed()
                && this.level().getDifficulty() != Difficulty.PEACEFUL;
    }

    /**
     * Legacy {@code isNotScared()}:214-217 — {@code getIsTamed()} for an ordinary small fish and a flat
     * {@code true} for the piranha; {@code EntityAIFleeFromEntityMoC.shouldExecute} refuses to run for
     * anything that reports it, and additionally bails out when an aquatic is out of the water.
     */
    private boolean canBeScared() {
        return !isPiranha() && !getIsTamed() && this.isInWater();
    }

    @Override
    protected void registerGoals() {
        // MoCAquatic contributes RandomSwimmingGoal(1.0, 10), the equivalent of legacy's
        // EntityAIWanderMoC2(this, 1.0D, 80) at priority 5. Legacy's piranha replaced initEntityAI outright and
        // therefore had NO wander goal at all — it drifted only while herding or chasing. The port leaves the
        // shared swim goal in place for it rather than reproducing a piranha that sits motionless when there is
        // nothing to eat and no shoal to join.
        super.registerGoals();

        // Legacy EntityAIPanicMoC(this, 1.3D) at priority 1 — ordinary fish bolt when hurt; the piranha, whose
        // initEntityAI never installed it, does not.
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D) {
            @Override
            public boolean canUse() {
                return !isPiranha() && super.canUse();
            }
        });

        // Legacy EntityAIFleeFromEntityMoC(this, entity -> entity.height > 0.3F || entity.width > 0.3F,
        // 2.0F, 0.6D, 1.5D) at priority 2: a small fish scatters away from anything bigger than it is within
        // two blocks, at 0.6 speed while far and 1.5 while close. Tamed fish and piranhas are exempt via
        // isNotScared(), and the legacy goal also refused to run while the fish was out of water.
        this.goalSelector.addGoal(2, new AvoidEntityGoal<LivingEntity>(this, LivingEntity.class,
                other -> other.getBbHeight() > 0.3F || other.getBbWidth() > 0.3F,
                2.0F, 0.6D, 1.5D, EntitySelector.NO_CREATIVE_OR_SPECTATOR) {
            @Override
            public boolean canUse() {
                return canBeScared() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return canBeScared() && super.canContinueToUse();
            }
        });

        // Legacy MoCEntityPiranha.initEntityAI:31-36 — EntityAIAttackMelee(this, 1.0D, true) at priority 3.
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                return isPiranhaHostile() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isPiranhaHostile() && super.canContinueToUse();
            }
        });

        // Legacy EntityAIFollowHerd(this, 0.6D, 4D, 20D, 1) at priority 4 — piranhas travel as a shoal.
        // MoCSchoolGoal is the port's equivalent (and what MoCEntityFishy already uses). One caveat of the
        // merge: MoCSchoolGoal groups by entity CLASS, so a piranha now counts its harmless cousins as
        // shoal-mates, where legacy's per-species classes kept the shoals separate. Harmless in practice — it
        // simply keeps piranhas drifting toward the fish schools they were hunting anyway.
        this.goalSelector.addGoal(4, new MoCSchoolGoal(this) {
            @Override
            public boolean canUse() {
                return isPiranha() && super.canUse();
            }
        });

        // Legacy targetTasks priority 1: EntityAINearestAttackableTargetMoC(this, EntityPlayer.class, true).
        // The in-water requirement comes from legacy's own findPlayerToAttack and matches the ported fishy
        // piranha: a piranha cannot reach someone standing dry on the bank.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, 16, true, false,
                (living, serverLevel) -> isPiranhaHostile() && living.isInWater()) {
            @Override
            public boolean canUse() {
                return isPiranhaHostile() && super.canUse();
            }
        });

        // Legacy MoCEntityPiranha.attackEntityFrom:71-87 set the attacker as the target on any non-Peaceful
        // difficulty, i.e. a struck piranha turns on whoever struck it. Only the piranha did this; the other
        // seven have no retaliation of any kind.
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return isPiranhaHostile() && super.canUse();
            }
        });
    }

    /**
     * Legacy {@code selectType()}:74-80 rolled a uniform 1-7 and left the biome filtering to the spawner,
     * which held a separate {@code SpawnListEntry} + biome-type list per species ({@code MoCEntities}:360-381).
     * With all eight merged behind one {@link EntityType} that table is gone, so the split is reproduced here
     * from the biome the fish spawned in:
     *
     * <ul>
     *   <li>rivers: anchovy, angelfish, goldfish (the only three legacy listed under {@code Type.RIVER});</li>
     *   <li>oceans: everything except the goldfish, which legacy never listed under {@code Type.OCEAN};</li>
     *   <li>anything else (swamps, beaches, ponds): all seven, which all carried
     *       {@code BEACH}/{@code SWAMP}/{@code WATER}.</li>
     * </ul>
     *
     * <p>The piranha is rolled against the others at legacy's own relative weights — 4 against 12 per
     * ordinary species — so it stays the rare one (~4.5% in open water, ~10% in a river where only three
     * species compete with it), and is suppressed entirely when the {@code spawnPiranhas} config flag is off,
     * the same gate {@link MoCEntityFishy} applies to its type-10 piranha.</p>
     */
    @Override
    public void selectType() {
        if (getTypeMoC() != 0) {
            return;
        }
        int[] pool;
        if (this.level().getBiome(this.blockPosition()).is(BiomeTags.IS_OCEAN)) {
            pool = OCEAN_SPECIES;
        } else if (this.level().getBiome(this.blockPosition()).is(BiomeTags.IS_RIVER)) {
            pool = RIVER_SPECIES;
        } else {
            pool = ALL_SPECIES;
        }
        boolean piranhas = MoCConfig.get().spawnPiranhas;
        int total = (pool.length * SPECIES_WEIGHT) + (piranhas ? PIRANHA_WEIGHT : 0);
        if (piranhas && this.random.nextInt(total) < PIRANHA_WEIGHT) {
            setTypeMoC(TYPE_PIRANHA);
        } else {
            setTypeMoC(pool[this.random.nextInt(pool.length)]);
        }
        // A piranha's larger health pool is applied by setTypeMoC; top it up so it does not spawn at 4/6.
        setHealth(getMaxHealth());
    }

    /**
     * Applies the one attribute that differs between sub-types. Hooked on {@link #setTypeMoC(int)} so it fires
     * on every route a type can arrive by — natural spawn, spawn egg, {@code /summon} and world load alike.
     */
    @Override
    public void setTypeMoC(int type) {
        super.setTypeMoC(type);
        applyTypeAttributes();
    }

    private void applyTypeAttributes() {
        AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        double target = isPiranha() ? PIRANHA_HEALTH : BASE_HEALTH;
        if (maxHealth.getBaseValue() == target) {
            return;
        }
        boolean wasFull = getHealth() >= (float) maxHealth.getBaseValue();
        maxHealth.setBaseValue(target);
        if (wasFull) {
            setHealth(getMaxHealth());
        }
    }

    /**
     * Reads the sub-type <em>before</em> handing over to the base reader. {@code LivingEntity.setHealth}
     * clamps to the max health in force at the moment it is called, so a full-health 6 HP piranha loaded
     * while the attribute still held the shared 4 HP base would be silently clipped to 4 and lose two hearts
     * on every world reload.
     */
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setTypeMoC(input.getIntOr("TypeMoC", getTypeMoC()));
        super.readAdditionalSaveData(input);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Legacy growth (MoCEntityAquatic.onLivingUpdate:409-415): a non-adult ages by one on a 1-in-300 tick
        // and is flagged adult on reaching getMaxEdad(). MoCAquatic does not run MoCBehavior.tickGrowth, so
        // (as with the shark) the curve lives in the entity.
        if (!getIsAdult() && this.random.nextInt(300) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= MAX_AGE) {
                setAdult(true);
            }
        }

        // Legacy onLivingUpdate:127-132 — a TAMED small fish quietly regenerates to full on a 1-in-100 tick.
        if (getIsTamed() && this.random.nextInt(100) == 0 && getHealth() < getMaxHealth()) {
            setHealth(getMaxHealth());
        }

        // Legacy MoCEntityAquatic.onLivingUpdate:474-489 plus isMovementCeased():194-197 — a beached fish stops
        // pathing after a second, then starts flopping: every 40 ticks past the 300-tick mark it hops and
        // scoots a little sideways. Legacy also dealt itself 1 point of drown damage on each flop; vanilla
        // WaterAnimal.handleAirSupply already drowns an out-of-water fish (2 damage a tick once its 300 ticks
        // of air run out), so that half is left to vanilla rather than double-damaging it.
        if (!this.isInWater()) {
            this.outOfWater++;
            if (this.outOfWater > 20) {
                getNavigation().stop();
            }
            if (this.outOfWater > 300 && (this.outOfWater % 40) == 0) {
                setDeltaMovement(getDeltaMovement().add(
                        (this.random.nextDouble() * 0.2D) - 0.1D, 0.3D,
                        (this.random.nextDouble() * 0.2D) - 0.1D));
                this.hurtMarked = true;
            }
        } else {
            this.outOfWater = 0;
        }

        // Only the piranha ever holds a target; the other seven have no attack goal, so make sure a stray
        // target (from a mod or a command) can never leave one of them shadowing a player.
        if (!isPiranha() && getTarget() != null) {
            setTarget(null);
        }
    }

    /**
     * Legacy {@code onLivingUpdate}:133-136 froze a beached fish's facing at its previous value so it did not
     * keep pivoting on the sand while lying on its side. Runs on both sides because it is what the renderer
     * reads.
     */
    @Override
    public void tick() {
        super.tick();
        if (!this.isInWater()) {
            setYRot(this.yRotO);
            this.yBodyRot = this.yBodyRotO = getYRot();
            this.yHeadRot = this.yBodyRot;
            setXRot(this.xRotO);
        }
    }

    /**
     * Legacy {@code dropFewItems}:110-121 (and the piranha's identical override with a different egg id): a
     * 70% chance of exactly one raw fish, otherwise {@code rand.nextInt(2)} — i.e. zero or one — species egg.
     * The two are mutually exclusive, so about 15% of kills leave nothing at all.
     *
     * <p>Implemented here rather than as a {@code MoCBehavior} drop spec because the spec list cannot express
     * a mutually-exclusive roll; the fishy, shark and komodo drops are special-cased the same way inside
     * {@code MoCBehavior.dropLoot}. The admin {@code destroyDrops} suppression is honoured exactly as that
     * method does ({@code destroyPassiveDrops} is scoped to {@code MoCAnimal} there, so it does not apply to
     * an aquatic). See {@link #SMALL_FISH_EGGS_SUPPORTED} for why the egg branch is currently inert.</p>
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        if (MoCConfig.get().destroyDrops) {
            return;
        }
        int roll = this.random.nextInt(100);
        if (roll < 70) {
            spawnAtLocation(level, new ItemStack(Items.COD, 1));
        } else if (SMALL_FISH_EGGS_SUPPORTED) {
            int eggs = this.random.nextInt(2);
            for (int i = 0; i < eggs; i++) {
                spawnAtLocation(level, drzhark.mocreatures.item.MoCThrownEggItem.createEgg(eggId()));
            }
        }
    }

    /**
     * The legacy composite egg id for this species: {@code getEggNumber()} returned 80-86 for anchovy through
     * manderin ({@code MoCEntityAnchovy}:19 onward) and 90 for the piranha
     * ({@code MoCEntityPiranha.dropFewItems}:96).
     */
    public int eggId() {
        int type = getTypeMoC();
        return type == TYPE_PIRANHA ? 90 : 79 + Math.max(1, Math.min(TYPE_MANDERIN, type));
    }

    /**
     * Legacy {@code getSizeFactor()}:139-142 is {@code edad * 0.01F} — a small fish spawns at 0.70-0.99x and
     * grows to a full 1.20x at its max age of 120, which is why they are visibly different sizes in a shoal.
     * {@code MoCMobRenderer.scale} already multiplies non-adults by its own shared {@code 0.5 + 0.5 * age/100}
     * curve, so that curve is divided back out here (the same correction {@link MoCEntityRaccoon} makes) and
     * the fish lands on exactly the legacy number instead of compounding the two.
     */
    @Override
    public float getSizeFactor() {
        float legacy = Math.max(1, Math.min(getMoCAge(), MAX_AGE)) * 0.01F;
        if (getIsAdult()) {
            return legacy;
        }
        float sharedCurve = 0.5F + (0.5F * Math.max(1, Math.min(getMoCAge(), 100)) * 0.01F);
        return legacy / sharedCurve;
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case TYPE_ANCHOVY -> modelTexture("smallfish_anchovy.png");
            case TYPE_ANGELFISH -> modelTexture("smallfish_angelfish.png");
            case TYPE_ANGLER -> modelTexture("smallfish_angler.png");
            case TYPE_GOLDFISH -> modelTexture("smallfish_goldfish.png");
            case TYPE_HIPPOTANG -> modelTexture("smallfish_hippotang.png");
            case TYPE_MANDERIN -> modelTexture("smallfish_manderin.png");
            case TYPE_PIRANHA -> modelTexture("smallfish_piranha.png");
            // Legacy getTexture()'s default arm was the clownfish, so an unset/out-of-range type shows one.
            default -> modelTexture("smallfish_clownfish.png");
        };
    }
}
