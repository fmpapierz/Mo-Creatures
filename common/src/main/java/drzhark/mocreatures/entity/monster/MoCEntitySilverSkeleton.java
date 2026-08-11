package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntitySilverSkeleton} (12.0.5
 * {@code entity/monster/MoCEntitySilverSkeleton.java}). A dual-katana samurai skeleton: an undead melee
 * monster that sprints at whatever player it has locked onto, swings one of its two silver blades on
 * every hit, burns in daylight and — roughly one kill in fifteen — leaves the Silver Sword behind.
 *
 * <p>Faithful legacy behaviour preserved here:</p>
 * <ul>
 *   <li><b>Stats</b> ({@code applyEntityAttributes:45-50}): 25 HP, movement speed 0.25, attack damage 2.</li>
 *   <li><b>AI</b> ({@code initEntityAI:37-42}): swim, melee attack with long memory, watch the player,
 *       and hunt the nearest player.</li>
 *   <li><b>Charge</b> ({@code onLivingUpdate:53-60} + {@code getAIMoveSpeed:121-126}): it sets the sprint
 *       flag whenever it holds a target — the model uses that for the forward run lean — and moves at
 *       0.35 while charging vs. 0.2 while idling.</li>
 *   <li><b>Alternating sword swings</b> ({@code startAttackAnimation:98-112} + {@code performAnimation:83-93}):
 *       every melee hit flips a coin and starts a 10-tick swing counter on the LEFT or RIGHT arm, which the
 *       legacy code broadcast to nearby clients with a {@code MoCMessageAnimation} packet so the model could
 *       animate the correct blade. The port replaces that bespoke packet with vanilla's entity-event
 *       broadcast (see {@link #EVENT_SWING_LEFT}); the counters themselves tick on both sides in
 *       {@link #aiStep()}, exactly as the legacy {@code onLivingUpdate} did.</li>
 *   <li><b>Undead</b> ({@code getCreatureAttribute:147-149} returned {@code EnumCreatureAttribute.UNDEAD}):
 *       26.2 has no {@code EnumCreatureAttribute} — undead-ness is the {@code minecraft:undead} entity-type
 *       tag, so the port ships {@code data/minecraft/tags/entity_type/undead.json} listing this mob. That one
 *       tag reproduces every legacy consequence at once: Smite bonus damage, Instant Health harming it,
 *       Instant Damage healing it, and immunity to Poison/Regeneration.</li>
 *   <li><b>Daylight</b> ({@code isHarmedByDaylight:157-159}): catches fire under open sky in daylight, via
 *       {@link MoCMob#burnsInDaylight()}.</li>
 *   <li><b>Sounds</b> ({@code 129-154}): vanilla skeleton ambient/hurt/death plus the quiet (0.15 volume)
 *       skeleton footstep.</li>
 *   <li><b>Drops</b> ({@code getDropItem:74-80}): a 1-in-10 Silver Sword, otherwise a bone — fed through
 *       vanilla's 0-2 copy count, so the sword actually lands on ~1 kill in 15. See
 *       {@link #dropCustomDeathLoot} for the full arithmetic and the Looting term.</li>
 * </ul>
 *
 * <p>The twin katanas are modelled geometry ({@code MoCModelSilverSkeleton}'s {@code SwordA/B/C} cubes)
 * rather than an equipped {@link ItemStack}, which is exactly how 12.0.5 did it. It is deliberately NOT
 * given a real Silver Sword in its main hand: the generic {@code MoCMobRenderer} carries no held-item
 * layer, so an equipped stack would be invisible, while its weapon attribute modifiers would silently push
 * the mob's damage from the legacy 2.0 up to ~7.0.</p>
 */
public class MoCEntitySilverSkeleton extends MoCMob {

    /**
     * Entity-event ids used to replay a sword swing on the clients that can see this mob — the modern
     * stand-in for the legacy {@code MoCMessageHandler.sendToAllAround(new MoCMessageAnimation(id, 1|2))}
     * within 64 blocks. Vanilla's highest allocated id is {@code EntityEvent.TNT_PRIME = 70}, so 71/72 are
     * free; unknown ids still fall through to {@code super.handleEntityEvent}.
     */
    private static final byte EVENT_SWING_LEFT = 71;
    private static final byte EVENT_SWING_RIGHT = 72;

    /** Legacy attack-animation window: the counter runs 1..10 ticks, then resets ({@code onLivingUpdate:62-68}). */
    private static final int SWING_LENGTH = 10;

    /**
     * Legacy {@code attackCounterLeft} / {@code attackCounterRight}: 0 while that arm is idle, otherwise
     * 1..10 counting out the swing. Driven on the server by {@link #doHurtTarget} and mirrored on the client
     * by the swing entity-event; both sides then advance it in {@link #aiStep()}, as the legacy
     * {@code onLivingUpdate} did on both sides too. Read by the renderer into the render state so
     * {@code MoCModelSilverSkeleton} can animate the matching blade.
     */
    private int leftSwingTick;
    private int rightSwingTick;

    public MoCEntitySilverSkeleton(EntityType<? extends MoCEntitySilverSkeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // Legacy applyEntityAttributes: 25 HP / 0.25 speed / 2 attack damage. The speed base is
                // re-derived every server tick in customServerAiStep (legacy getAIMoveSpeed), so 0.25 is
                // only the value a freshly-spawned skeleton carries for its first tick.
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    /**
     * Legacy {@code initEntityAI}: swim (0), melee attack (2) and watch-closest-player (8) on the goal
     * selector, nearest-attackable-player (1) on the target selector. {@link MoCMob#registerGoals()} already
     * installs all of that (plus a stroll goal and hurt-by-target retaliation, both sane modern additions to
     * a mob that legacy otherwise left rooted in place when idle) — the only difference is the melee goal's
     * {@code longMemory} flag, which legacy set to {@code true} so a skeleton keeps pathing to a player who
     * has broken line of sight instead of instantly forgetting them. Swap that one goal out.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(goal -> goal instanceof MeleeAttackGoal);
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
    }

    // ------------------------------------------------------------------ sword-swing animation

    /** Legacy {@code attackCounterLeft}: 0 = idle, 1..10 = swing progress of the left katana. */
    public int getLeftSwingTick() {
        return this.leftSwingTick;
    }

    /** Legacy {@code attackCounterRight}: 0 = idle, 1..10 = swing progress of the right katana. */
    public int getRightSwingTick() {
        return this.rightSwingTick;
    }

    /**
     * Legacy {@code onLivingUpdate:62-68}, which advanced both swing counters on the client and the server
     * (the client had been told to start one by the animation packet) and zeroed each once it passed 10.
     * {@code aiStep} is the modern both-sides equivalent of {@code onLivingUpdate}.
     */
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.leftSwingTick > 0 && ++this.leftSwingTick > SWING_LENGTH) {
            this.leftSwingTick = 0;
        }
        if (this.rightSwingTick > 0 && ++this.rightSwingTick > SWING_LENGTH) {
            this.rightSwingTick = 0;
        }
    }

    /**
     * Client end of the swing sync — the legacy {@code performAnimation(1|2)} that the
     * {@code MoCMessageAnimation} packet invoked, now driven by the vanilla entity-event broadcast.
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SWING_LEFT) {
            this.leftSwingTick = 1;
        } else if (id == EVENT_SWING_RIGHT) {
            this.rightSwingTick = 1;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /**
     * Legacy {@code attackEntityAsMob:115-118} called {@code startAttackAnimation()} before the hit itself,
     * so the blade sweeps whether or not the blow lands. {@code startAttackAnimation} coin-flipped the arm,
     * started that arm's counter server-side and told every client within 64 blocks to do the same.
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean left = this.random.nextBoolean();
        if (left) {
            this.leftSwingTick = 1;
        } else {
            this.rightSwingTick = 1;
        }
        // Vanilla's entity-event broadcast reaches exactly the players tracking this entity, which is the
        // modern equivalent of the legacy 64-block TargetPoint the animation packet was sent to.
        level.broadcastEntityEvent(this, left ? EVENT_SWING_LEFT : EVENT_SWING_RIGHT);
        return super.doHurtTarget(level, target);
    }

    // ------------------------------------------------------------------ charge / movement

    /**
     * Legacy {@code onLivingUpdate:53-60} + {@code getAIMoveSpeed:121-126}: a silver skeleton sprints while
     * it holds a target and walks otherwise, moving at 0.35 vs 0.2.
     *
     * <p>26.2 has no {@code getAIMoveSpeed} hook to override — mob movement reads the MOVEMENT_SPEED
     * attribute — and {@link net.minecraft.world.entity.LivingEntity#setSprinting} itself adds a
     * {@code +30% ADD_MULTIPLIED_TOTAL} "sprinting" modifier to that attribute. So the base value is set to
     * whatever makes the EFFECTIVE speed match legacy: {@code 0.35 / 1.3 = 0.2692} while sprinting, and a
     * plain {@code 0.2} while not. Keeping the sprint flag itself matters beyond speed — the model leans the
     * whole skeleton forward while sprinting, exactly as the legacy renderer did.</p>
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        boolean charging = this.getTarget() != null;
        if (charging != this.isSprinting()) {
            // Only on a change: setSprinting removes/re-adds an attribute modifier on every call.
            this.setSprinting(charging);
        }
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            double base = charging ? 0.35D / 1.3D : 0.2D;
            if (speed.getBaseValue() != base) {
                speed.setBaseValue(base);
            }
        }
    }

    // ------------------------------------------------------------------ appearance / sounds

    @Override
    public Identifier getTexture() {
        // Legacy ctor: this.texture = "silverskeleton.png" (single variant, no sub-types).
        return modelTexture("silverskeleton.png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    /** Legacy {@code playStepSound}: bones rattle quietly — the skeleton step cue at 0.15 volume. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SKELETON_STEP, 0.15F, 1.0F);
    }

    /** Legacy {@code isHarmedByDaylight() == true}: an undead that ignites under the open sky by day. */
    @Override
    protected boolean burnsInDaylight() {
        return true;
    }

    // ------------------------------------------------------------------ drops

    /**
     * Legacy {@code getDropItem:74-80} chose ONE item per kill — {@code rand.nextInt(10) == 0} gave
     * {@code MoCItems.silversword}, everything else a bone — and neither {@code MoCEntitySilverSkeleton}
     * nor its base {@code MoCEntityMob} overrode {@code dropFewItems}, so 1.12's
     * {@code EntityLiving.dropFewItems} consumed that single id and dropped {@code i} copies of it, where
     * {@code i = rand.nextInt(3)} plus {@code rand.nextInt(lootingModifier + 1)} when the killer wore Looting.
     *
     * <p><b>The real legacy sword rate is therefore 1 kill in 15 (~6.7%) bare-handed, not the 1-in-10 the
     * {@code getDropItem} line reads like</b>: a tenth of kills pick the sword, and only two thirds of those
     * roll a non-zero copy count. Looting can only rescue the {@code nextInt(3) == 0} third, giving
     * {@code 0.1 * (1 - 1/3 * 1/(L+1))} = 8.3% / 8.9% / 9.2% at Looting I/II/III. About a third of all kills
     * drop nothing at all, and the sword and the bones are always mutually exclusive. That rarity is legacy,
     * not a bug — over ten kills a player has a coin-flip chance of seeing one blade.</p>
     *
     * <p>The Looting term has to be applied by hand here: 26.2 passes {@code dropCustomDeathLoot} no looting
     * count (the enchantment is normally applied by a loot table's {@code enchanted_count_increase}
     * function, and this mob deliberately ships no loot table). See {@link #lootingLevel}.</p>
     *
     * <p>This hook does fire for a loot-table-less mob: {@code LivingEntity.dropAllDeathLoot} calls
     * {@code dropFromLootTable} and {@code dropCustomDeathLoot} back to back under a single
     * {@code shouldDropLoot} gate, and {@code dropFromLootTable} simply no-ops when the entity's default
     * table is absent ({@code ReloadableServerRegistries.getLootTable} falls back to {@code LootTable.EMPTY}).
     * Nothing here is gated on {@code hitByPlayer} either, matching legacy — a silver skeleton that burns to
     * death in the morning sun still leaves its bones.</p>
     *
     * <p>This lives here rather than in {@code MoCBehavior}'s spec table because that table can only express
     * type-agnostic loot — the same reason the ogre and the hell rat resolve their rolls in
     * {@code MoCBehavior.dropLoot}'s per-entity branches. {@code super} is still called first so the shared
     * monster-spawn-egg drop chance keeps working.</p>
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        // Server-admin loot suppression (legacy destroyDrops), honoured by MoCBehavior.dropLoot for every
        // other creature — a drop resolved outside that method has to check it itself.
        if (drzhark.mocreatures.config.MoCConfig.get().destroyDrops) {
            return;
        }
        Item drop = this.random.nextInt(10) == 0 ? MoCItems.SILVERSWORD.get() : Items.BONE;
        int count = this.random.nextInt(3); // vanilla dropFewItems copy count: 0-2, zero included
        int looting = lootingLevel(level, damageSource);
        if (looting > 0) {
            // Verbatim legacy dropFewItems: "if (lootingModifier > 0) i += rand.nextInt(lootingModifier + 1);"
            count += this.random.nextInt(looting + 1);
        }
        for (int i = 0; i < count; i++) {
            spawnAtLocation(level, new ItemStack(drop, 1));
        }
    }

    /**
     * The Looting level of whoever landed the killing blow — the {@code lootingModifier} that 1.12's
     * {@code EntityLivingBase.onDeath} handed to {@code dropFewItems} via
     * {@code ForgeHooks.getLootingLevel(this, cause.getTrueSource(), cause)}, which for a living killer was
     * just {@code EnchantmentHelper.getLootingModifier(killer)} over its held weapon.
     *
     * <p>26.2 has no such parameter and no {@code getMobLooting} helper, because Looting is a data-driven
     * enchantment now: resolve its holder out of the level's dynamic enchantment registry and ask
     * {@link EnchantmentHelper#getEnchantmentLevel} for the killer's
     * level. The vanilla {@code minecraft:looting} definition scopes its slots to the main hand, so this
     * reads the weapon actually swung, exactly as the legacy helper did. {@code DamageSource.getEntity()} is
     * the modern {@code getTrueSource()} — the shooter/attacker, not an arrow — so an indirect kill still
     * counts the bow's owner. A datapack that removed the enchantment outright yields 0 rather than throwing
     * in the middle of a death.</p>
     */
    private static int lootingLevel(ServerLevel level, DamageSource damageSource) {
        if (!(damageSource.getEntity() instanceof LivingEntity killer)) {
            return 0;
        }
        return level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.LOOTING)
                .map(holder -> EnchantmentHelper.getEnchantmentLevel(holder, killer))
                .orElse(0);
    }
}
