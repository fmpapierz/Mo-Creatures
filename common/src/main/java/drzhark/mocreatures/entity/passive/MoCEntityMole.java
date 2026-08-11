package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityMole} (1.12.2 {@code entity/passive/MoCEntityMole.java}).
 *
 * <p>The mole is a small, un-tameable surface digger whose entire personality is one four-state machine
 * held in a synched {@code MOLE_STATE} datawatcher (legacy line 29, comment at line 58):</p>
 *
 * <table>
 *   <caption>Legacy mole states</caption>
 *   <tr><th>state</th><th>meaning</th><th>render pitch</th><th>render Y offset</th></tr>
 *   <tr><td>0</td><td>outside — ordinary surface critter</td><td>0&deg;</td><td>0</td></tr>
 *   <tr><td>1</td><td>digging in — nose-down, immobile</td><td>-45&deg;</td><td>0.3</td></tr>
 *   <tr><td>2</td><td>underground — hidden, untouchable, invulnerable</td><td>0&deg;</td><td>1.0</td></tr>
 *   <tr><td>3</td><td>"pick-a-boo" — head poking back out, immobile</td><td>60&deg;</td><td>0.1</td></tr>
 * </table>
 *
 * <p>The legacy transitions ({@code onLivingUpdate}:151-193) are reproduced verbatim in
 * {@link #tickBurrow}, including their odd evaluation order and their per-tick random rolls:</p>
 * <ol>
 *   <li>{@code rand.nextInt(10)==0 && state==1} &rarr; state 2 (a dig completes after ~10 ticks).</li>
 *   <li>{@code state!=1 && state!=2 && isOnDirt()} and a visible "boogey" within 4 blocks &rarr; state 1
 *       plus a path clear — this is the mole's <em>only</em> defence, which is why legacy gave it no
 *       {@code EntityAIPanic}.</li>
 *   <li>{@code rand.nextInt(20)==0 && state==2} and <em>no</em> boogey within 4 blocks &rarr; state 3.</li>
 *   <li>{@code state!=0 && !isOnDirt()} &rarr; state 0 (it can only burrow into diggable ground).</li>
 *   <li>{@code rand.nextInt(30)==0 && state==3} &rarr; back to state 2.</li>
 *   <li>{@code setSprinting(state==1 || state==2)} — legacy's "digging fx": the sprint flag is what makes
 *       vanilla spray ground-block particles from under the entity. 26.2 still does this
 *       ({@code Entity.canSpawnSprintParticle} + {@code spawnSprintParticle} in {@code baseTick}), and
 *       unlike 1.12 it no longer requires the entity to be moving — so a stationary digging mole now
 *       throws up dirt continuously, which is exactly the effect legacy was reaching for.</li>
 * </ol>
 *
 * <p>The consequences of state 2 are ported one-for-one: it cannot be hit
 * ({@code canBeCollidedWith} &rarr; {@link #isPickable()}), cannot be shoved
 * ({@code canBePushed} &rarr; {@link #isPushable()}), shoves nobody
 * ({@code collideWithEntity} &rarr; {@link #doPush(Entity)}), takes no damage
 * ({@code attackEntityFrom} &rarr; {@link #hurtServer}), is flatly invulnerable
 * ({@code isEntityInvulnerable} &rarr; {@link #isInvulnerableTo}) and never suffocates
 * ({@code isEntityInsideOpaqueBlock} &rarr; {@link #isInWall()}).</p>
 *
 * <p>Legacy {@code digForward()} (lines 74-95) — which would have tunnelled the mole through solid dirt
 * along its look vector — is dead code in 12.0.5: it is {@code @SuppressWarnings("unused")} and its only
 * call site is commented out at lines 182-184. It is deliberately <em>not</em> ported; the mole's real
 * hitbox stays on the surface and only its rendering sinks, which is why it never suffocates.</p>
 *
 * <p><b>Item stealing.</b> See {@link #tickTheft} — this is a reconstruction, not a port; 12.0.5 contains
 * no item-pickup code for the mole at all (verified by grep over the whole legacy tree).</p>
 */
public class MoCEntityMole extends MoCAnimal {

    /** Legacy {@code MOLE_STATE} datawatcher: 0 outside / 1 digging / 2 underground / 3 pick-a-boo. */
    private static final EntityDataAccessor<Integer> MOLE_STATE =
            SynchedEntityData.defineId(MoCEntityMole.class, EntityDataSerializers.INT);

    /** The item this mole has snatched and taken underground; dropped again when it dies. */
    private ItemStack stolenItem = ItemStack.EMPTY;
    /** Ticks before this mole will look for another item to steal (see {@link #tickTheft}). */
    private int stealCooldown;

    public MoCEntityMole(EntityType<? extends MoCEntityMole> type, Level level) {
        super(type, level);
    }

    /** Legacy {@code applyEntityAttributes}: 10 max health, 0.2 movement speed. No attack damage — it never fights. */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MOLE_STATE, 0); // legacy entityInit: state starts at 0 (outside)
    }

    /**
     * Legacy {@code initEntityAI} is a three-goal list: {@code EntityAISwimming} (1),
     * {@code EntityAIWanderMoC2(1.0)} (2) and {@code EntityAIWatchClosest(EntityPlayer, 8.0F)} (4). The
     * shared {@link MoCAnimal#registerGoals()} already supplies a {@code FloatGoal}, a stroll goal and a
     * look-around goal, so only two corrections are needed:
     * <ul>
     *   <li>Strip the inherited {@link PanicGoal}. Legacy deliberately gave the mole no panic AI — a
     *       frightened mole <em>burrows</em> (state 1) instead of bolting, and a panicking mole would run
     *       out of the {@code isMovementCeased()} freeze the burrow depends on.</li>
     *   <li>Swap the base 6-block {@code LookAtPlayerGoal} for the legacy 8-block one.</li>
     * </ul>
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(g -> g instanceof PanicGoal);
        this.goalSelector.removeAllGoals(g -> g instanceof LookAtPlayerGoal);
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    // ------------------------------------------------------------------------------- state accessors

    /** Legacy {@code getState()}: 0 outside / 1 digging / 2 underground / 3 pick-a-boo. */
    public int getState() {
        return this.entityData.get(MOLE_STATE);
    }

    /** Legacy {@code setState(int)}. */
    public void setState(int state) {
        this.entityData.set(MOLE_STATE, state);
    }

    /** Convenience: true while the mole is fully buried (legacy state 2), which is its invulnerable state. */
    public boolean isUnderground() {
        return getState() == 2;
    }

    /**
     * Legacy {@code pitchRotationOffset()} (lines 115-131), in degrees. The legacy renderer
     * ({@code MoCRenderMoC.adjustPitch}) fed this to {@code glRotatef(f, -1F, 0F, 0F)}, i.e. a rotation
     * about the <em>negative</em> X axis — so a NEGATIVE value tips the snout down (digging in) and a
     * POSITIVE value tips it up (peeking out). Kept static so the client model can reuse the same table
     * from the render state without duplicating it.
     */
    public static float renderPitch(int state) {
        return switch (state) {
            case 1 -> -45.0F; // diving nose-first into the ground
            case 3 -> 60.0F;  // head tilted up out of the hole
            default -> 0.0F;  // 0 outside, 2 underground: level
        };
    }

    /**
     * Legacy {@code getAdjustedYOffset()} (lines 133-148), in blocks. The legacy model applied it as
     * {@code glTranslatef(0F, yOffset, 0F)} inside the already-flipped model space, i.e. it sinks the
     * <em>rendering</em> downward by this many blocks; the entity's real position and hitbox never move.
     */
    public static float renderYOffset(int state) {
        return switch (state) {
            case 1 -> 0.3F; // half-buried while digging in
            case 2 -> 1.0F; // a full block below the surface: invisible
            case 3 -> 0.1F; // just the head showing
            default -> 0.0F;
        };
    }

    /** Instance form of {@link #renderPitch(int)} (legacy {@code pitchRotationOffset}). */
    public float pitchRotationOffset() {
        return renderPitch(getState());
    }

    /** Instance form of {@link #renderYOffset(int)} (legacy {@code getAdjustedYOffset}). */
    public float getAdjustedYOffset() {
        return renderYOffset(getState());
    }

    // ---------------------------------------------------------------------------------- digging rules

    /**
     * Legacy {@code isOnDirt()} (lines 62-68): samples the single block at
     * {@code (floor(posX), floor(boundingBox.minY - 0.5), floor(posZ))} and asks
     * {@code isDiggableBlock(Block.getIdFromBlock(block))}, which accepted 1.12 block ids
     * {@code 2 | 3 | 12} — grass block, dirt and sand.
     *
     * <p>Because the legacy test looked only at the numeric id and ignored metadata, id 3 also covered
     * <em>coarse dirt</em> (meta 1) and <em>podzol</em> (meta 2), and id 12 also covered <em>red sand</em>
     * (meta 1). Those four 1.12 metas are separate blocks in 26.2, so all six are listed here — that is the
     * faithful translation, not a widening. Mud, mycelium, rooted dirt, gravel and the {@code minecraft:dirt}
     * block tag are deliberately NOT accepted: none of them were id 2/3/12.</p>
     */
    public boolean isOnDirt() {
        BlockPos pos = new BlockPos(
                Mth.floor(this.getX()),
                Mth.floor(this.getBoundingBox().minY - 0.5D),
                Mth.floor(this.getZ()));
        BlockState state = this.level().getBlockState(pos);
        return state.is(Blocks.GRASS_BLOCK)   // 1.12 id 2
                || state.is(Blocks.DIRT)      // 1.12 id 3, meta 0
                || state.is(Blocks.COARSE_DIRT) // 1.12 id 3, meta 1
                || state.is(Blocks.PODZOL)    // 1.12 id 3, meta 2
                || state.is(Blocks.SAND)      // 1.12 id 12, meta 0
                || state.is(Blocks.RED_SAND); // 1.12 id 12, meta 1
    }

    /**
     * Legacy {@code getBoogey(4D)} on {@code MoCEntityAnimal}:298-318 — scan the mole's bounding box
     * inflated by {@code (4, 4, 4)} and return a living entity that is (a) not another mole
     * ({@code entity.getClass() != this.getClass()}) and (b) at least half a block wide OR tall
     * ({@code width >= 0.5 || height >= 0.5}). Legacy returned the LAST match in iteration order, not the
     * nearest, and the mole's own {@code onLivingUpdate} additionally required {@code canEntityBeSeen}
     * before it would burrow — so the visibility test is applied by the caller, exactly as in legacy.
     */
    private @Nullable LivingEntity getBoogey(ServerLevel level, double range) {
        LivingEntity found = null;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(range, 4.0D, range), this::isBoogey)) {
            found = entity;
        }
        return found;
    }

    private boolean isBoogey(Entity entity) {
        return entity != this && entity.isAlive()
                && !(entity instanceof MoCEntityMole)
                && entity instanceof LivingEntity
                && (entity.getBbWidth() >= 0.5F || entity.getBbHeight() >= 0.5F);
    }

    // ------------------------------------------------------------------------------------- AI ticking

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Item theft runs first purely so that a mole actively closing on a dropped stack is not scared
        // into the ground by the very player who dropped it before it can reach the item. Mirrors the way
        // MoCEntityTurtle.tryGroundEat suppresses its own hide-in-shell reaction for that tick.
        boolean busyStealing = tickTheft(level);
        tickBurrow(level, busyStealing);

        // Legacy MoCEntityAnimal.onLivingUpdate:327-329 cleared the path every tick isMovementCeased() was
        // true; for the mole that is states 1 and 3 (legacy isMovementCeased override, lines 195-198).
        if (isMovementCeased()) {
            this.getNavigation().stop();
        }
    }

    /**
     * The legacy state machine, transition-for-transition and in the legacy evaluation order
     * ({@code MoCEntityMole.onLivingUpdate}:154-192). {@code busyStealing} suppresses only the
     * threat-triggered dig-in, so that the theft behaviour below can actually complete.
     */
    private void tickBurrow(ServerLevel level, boolean busyStealing) {
        // 1) A dig-in completes: ~1 tick in 10 while digging, drop through into the ground.
        if (this.random.nextInt(10) == 0 && getState() == 1) {
            setState(2);
        }

        // 2) Something big and visible is within 4 blocks while standing on diggable ground: start digging.
        if (getState() != 2 && getState() != 1 && isOnDirt() && !busyStealing) {
            LivingEntity boogey = getBoogey(level, 4.0D);
            if (boogey != null && this.hasLineOfSight(boogey)) {
                setState(1);
                this.getNavigation().stop(); // legacy getNavigator().clearPath()
            }
        }

        // 3) Underground and the coast is clear: pop the head out for a look (~1 tick in 20).
        if (this.random.nextInt(20) == 0 && getState() == 2 && getBoogey(level, 4.0D) == null) {
            setState(3);
            this.getNavigation().stop();
        }

        // 4) No longer over diggable ground: surface immediately. This is what stops a mole staying
        //    "underground" after it has wandered onto stone, a path block or a player's floor.
        if (getState() != 0 && !isOnDirt()) {
            setState(0);
        }

        // 5) Done peeking (~1 tick in 30): duck back under.
        if (this.random.nextInt(30) == 0 && getState() == 3) {
            setState(2);
        }

        // 6) Digging fx: the sprint flag drives vanilla's ground-block particle spray.
        setSprinting(getState() == 1 || getState() == 2);
    }

    /**
     * Legacy {@code isMovementCeased()} (lines 195-198): the mole is frozen while digging in (1) and while
     * peeking out (3). It is NOT frozen while fully underground (2) — a buried mole still wanders, it is
     * simply drawn a block lower and cannot be touched.
     */
    public boolean isMovementCeased() {
        return getState() == 1 || getState() == 3;
    }

    @Override
    public void travel(Vec3 input) {
        super.travel(isMovementCeased() ? Vec3.ZERO : input);
    }

    // ------------------------------------------------------------------------------------ item theft

    /**
     * <b>Reconstruction, not a port.</b> Mo'Creatures 12.0.5's {@code MoCEntityMole} contains no
     * item-handling code whatsoever — no {@code EntityItem} lookup, no {@code getClosestEntityItem} call,
     * no inventory (verified by reading the whole class and grepping {@code Mole} across the legacy tree).
     * The "moles steal things you drop" behaviour lives only in the mod's documentation/lore, so it is
     * rebuilt here from that description in the spirit of the legacy code, using the same primitives the
     * legacy base class offered ({@code MoCEntityAnimal.getClosestEntityItem}) and the same
     * walk-up-then-consume shape as the ported {@code MoCEntityTurtle.tryGroundEat}.
     *
     * <p>Rules, chosen to be recoverable rather than destructive:</p>
     * <ul>
     *   <li>Only a fully surfaced mole (state 0) that is not already carrying something steals, and only
     *       once every 200 ticks ({@code stealCooldown}). States 1 and 3 are {@code isMovementCeased()}
     *       poses, so a mole in them could path but never actually walk to the item.</li>
     *   <li>It targets the nearest live {@link ItemEntity} within 8 blocks whose pick-up delay has expired
     *       ({@code !hasPickUpDelay()}), so freshly-thrown items and block-break drops the player is still
     *       collecting are left alone for half a second.</li>
     *   <li>It paths to the item at 1.2x speed; within 1.5 blocks it takes the <em>whole</em> stack,
     *       plays the vanilla pickup sound and immediately dives (state 1) if it is over diggable ground.</li>
     *   <li>The stolen stack is persisted in save data and is dropped again when the mole dies, so nothing
     *       is ever permanently lost — kill the thief and you get your item back.</li>
     * </ul>
     *
     * @return {@code true} while the mole is pursuing or taking an item, so the caller can skip the
     *         threat-triggered burrow for this tick.
     */
    private boolean tickTheft(ServerLevel level) {
        if (this.stealCooldown > 0) {
            this.stealCooldown--;
            return false;
        }
        if (!this.stolenItem.isEmpty() || getState() != 0) {
            return false;
        }
        ItemEntity target = null;
        double bestSq = Double.MAX_VALUE;
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(8.0D),
                e -> e.isAlive() && !e.hasPickUpDelay() && !e.getItem().isEmpty())) {
            double d = this.distanceToSqr(ie);
            if (d < bestSq) {
                bestSq = d;
                target = ie;
            }
        }
        if (target == null) {
            return false;
        }
        if (bestSq > 2.25D) { // 1.5 blocks
            this.getNavigation().moveTo(target, 1.2D);
            return true;
        }
        this.stolenItem = target.getItem().copy();
        target.discard();
        this.stealCooldown = 200;
        level.playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6F,
                ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.6F);
        // Bolt for cover with the loot: the dig-in is only possible over diggable ground, and step 4 of
        // tickBurrow will bounce it straight back to state 0 if it is not.
        if (isOnDirt()) {
            setState(1);
        }
        this.getNavigation().stop();
        return true;
    }

    /** The stack this mole is currently carrying off, or {@link ItemStack#EMPTY}. */
    public ItemStack getStolenItem() {
        return this.stolenItem;
    }

    /**
     * Returns the loot the mole took with it. Spawned directly rather than through the {@code MoCBehavior}
     * drop table, so the {@code destroyDrops} / {@code destroyPassiveDrops} server flags — which exist to
     * suppress <em>farmed</em> loot — can never swallow a player's own stolen property.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        if (!this.stolenItem.isEmpty()) {
            this.spawnAtLocation(level, this.stolenItem.copy());
            this.stolenItem = ItemStack.EMPTY;
        }
    }

    // ------------------------------------------------------------------------- underground invincibility

    /**
     * Legacy {@code attackEntityFrom} (lines 200-206): a mole in state 2 simply refuses the blow
     * ({@code return false}) instead of forwarding it to super.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isUnderground()) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    /**
     * Legacy {@code isEntityInvulnerable} (lines 239-245): an underground mole is invulnerable to
     * <em>everything</em>. One deliberate concession: damage types tagged
     * {@code minecraft:bypasses_invulnerability} (the void and {@code /kill}) still land, so an operator can
     * always remove a mole that has parked itself under a player and latched its own state 2 shut. Legacy had
     * no such escape hatch because 1.12's {@code DamageSource.OUT_OF_WORLD} check lived in the super call it
     * was skipping.
     */
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (isUnderground() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        return super.isInvulnerableTo(level, source);
    }

    /**
     * Legacy {@code canBeCollidedWith()} (lines 208-211) &rarr; 26.2 {@code isPickable()}: an underground
     * mole cannot be selected by a raytrace, so arrows, melee swings and right-clicks pass through it. (26.2's
     * separate {@code Entity.canBeCollidedWith(Entity)} is the hard push-out-of-the-way test used by boats and
     * shulkers and is already {@code false} for every mob, so it is left alone.)
     */
    @Override
    public boolean isPickable() {
        return !isUnderground() && super.isPickable();
    }

    /** Legacy {@code canBePushed()} (lines 213-216): nothing can shove a buried mole around. */
    @Override
    public boolean isPushable() {
        return !isUnderground() && super.isPushable();
    }

    /**
     * Legacy {@code collideWithEntity(Entity)} (lines 218-224): and the buried mole pushes nobody either,
     * so players and mobs walk straight over the spot without feeling it.
     */
    @Override
    protected void doPush(Entity entity) {
        if (!isUnderground()) {
            super.doPush(entity);
        }
    }

    /**
     * Legacy {@code isEntityInsideOpaqueBlock()} (lines 226-232) &rarr; 26.2 {@code isInWall()}: never report
     * a buried mole as stuck in a block, so it takes no suffocation damage. In this port the guard is belt
     * and braces — {@code digForward} is not ported, so the mole's real position always stays on the surface
     * and only its rendering sinks — but it is kept for exact parity and to stay correct if a future change
     * ever moves the hitbox underground.
     */
    @Override
    public boolean isInWall() {
        if (isUnderground()) {
            return false;
        }
        return super.isInWall();
    }

    // -------------------------------------------------------------------------------------- persistence

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("MoleState", getState());
        if (!this.stolenItem.isEmpty()) {
            output.store("StolenItem", ItemStack.CODEC, this.stolenItem);
        }
        output.putInt("StealCooldown", this.stealCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setState(input.getIntOr("MoleState", 0));
        this.stolenItem = input.read("StolenItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.stealCooldown = input.getIntOr("StealCooldown", 0);
    }

    // ------------------------------------------------------------------------------- appearance & sound

    /**
     * The legacy mole has a single texture ({@code getTexture()} returns {@code "mole.png"} unconditionally)
     * and no sub-types, so it just claims type 1 like every other single-variant Mo'Creature.
     */
    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(1);
        }
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("mole.png");
    }

    /** Legacy {@code getAmbientSound()} returns {@code null} — the mole is silent until you hurt it. */
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    /** Legacy {@code getHurtSound} reuses the mod's rabbit hurt sample ({@code MoCSoundEvents.ENTITY_RABBIT_HURT}). */
    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.RABBITHURT.get();
    }

    /** Legacy {@code getDeathSound} reuses the mod's rabbit death sample ({@code MoCSoundEvents.ENTITY_RABBIT_DEATH}). */
    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.RABBITDEATH.get();
    }
}
