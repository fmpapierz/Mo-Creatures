package drzhark.mocreatures.entity.monster;

import java.util.Optional;

import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.entity.projectile.MoCEntityRock;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityMiniGolem} (12.0.5). The little brother of {@link MoCEntityGolem}:
 * where the big golem is a 23-cube self-assembling puzzle boss, the mini golem is a compact 1x1 stone
 * brawler with exactly one trick — it rips a block out of the ground next to it, hoists it over its head
 * with both arms, and after a long wind-up hurls it at whoever it is angry with.
 *
 * <p><strong>The legacy loop</strong> (MoCEntityMiniGolem:76-146), reproduced here verbatim in
 * {@link #customServerAiStep(ServerLevel)}:</p>
 * <ol>
 *   <li>{@code angry} simply tracks "do I have an attack target" and is a pure render flag — it swaps the
 *       head and body to their red-hot skins (see {@code MoCModelMiniGolem}).</li>
 *   <li>While angry and empty-handed, a {@code rand.nextInt(30) == 0} roll per tick fires
 *       {@link #acquireRock(ServerLevel)} — on average once every 1.5 s.</li>
 *   <li>Holding a rock freezes the golem in place ({@code isMovementCeased()} returned
 *       {@code hasRock && target != null}, MoCEntityMiniGolem:118-121) and starts a 50-tick counter.</li>
 *   <li>At tick 50 the block is thrown if the target is within 48 blocks, and the golem is empty-handed
 *       again (MoCEntityMiniGolem:126-146).</li>
 * </ol>
 *
 * <p><strong>What changed in the port, and why.</strong> Legacy spawned a real
 * {@code MoCEntityThrowableRock} in "behaviour 1" (held) and then teleported it to {@code posY + 1.0}
 * every tick to fake a held block; when the timer elapsed it killed that entity and spawned a second,
 * "behaviour 0" (ballistic) one to actually fly at the target. The port's
 * {@link drzhark.mocreatures.entity.projectile.MoCEntityThrowableRock} is a homing vacuum rock bound to
 * the big golem (its constructor and homing tick both require a {@code MoCEntityGolem} master), so it
 * cannot serve as a mini golem prop. Instead:</p>
 * <ul>
 *   <li>the <em>held</em> block is synched state on the golem itself ({@link #HELD_ROCK}) and drawn above
 *       its head by {@code MoCMiniGolemRenderer}'s held-rock layer — the same picture with one fewer
 *       entity and none of the legacy "orphaned hovering rock" bug (a legacy golem that lost its target
 *       mid-hold left its behaviour-1 rock frozen in mid-air forever);</li>
 *   <li>the <em>thrown</em> block is a {@link MoCEntityRock}, the port's existing ballistic golem rock,
 *       with its item stack overridden to the block that was ripped up so it still visibly throws
 *       <em>that</em> block. Legacy rocks damaged everything in their flight path for 4 per tick and
 *       decayed into a dropped item; {@code MoCEntityRock} deals 5 on impact and bursts into particles.
 *       That also closes the legacy block-duplication hole (a legacy mini golem with
 *       {@code mobGriefing=false} still got a real block out of thin air and could drop it as an item).</li>
 * </ul>
 */
public class MoCEntityMiniGolem extends MoCMob {

    /** Ticks the block is held overhead before it is hurled (legacy {@code tcounter >= 50}). */
    private static final int HOLD_TICKS = 50;
    /** Per-tick odds of grabbing a block while angry and empty-handed (legacy {@code rand.nextInt(30) == 0}). */
    private static final int ACQUIRE_CHANCE = 30;
    /** The target must be inside this range for the held block to actually be thrown (legacy {@code < 48F}). */
    private static final double THROW_RANGE = 48.0D;
    /** Legacy {@code MoCTools.destroyRandomBlockWithIBlockState(this, 3D)} search distance. */
    private static final double SEARCH_DISTANCE = 3.0D;
    /** Legacy {@code ThrowStone(..., speedMod = 10D, height = 0.25D)} ballistics. */
    private static final double THROW_SPEED_DIVISOR = 10.0D;
    private static final double THROW_ARC = 0.25D;

    /** Client-visible anger flag: drives the red-hot head/body skins. Mirrors legacy {@code ANGRY}. */
    private static final EntityDataAccessor<Boolean> ANGRY =
            SynchedEntityData.defineId(MoCEntityMiniGolem.class, EntityDataSerializers.BOOLEAN);
    /** Client-visible carry flag: drives the arms-overhead pose. Mirrors legacy {@code HAS_ROCK}. */
    private static final EntityDataAccessor<Boolean> HAS_ROCK =
            SynchedEntityData.defineId(MoCEntityMiniGolem.class, EntityDataSerializers.BOOLEAN);
    /**
     * The actual block being hoisted, so the client can render the real thing (dirt looks like dirt, gold
     * like gold) and the server knows what to throw. Legacy stored this on the held rock entity.
     */
    private static final EntityDataAccessor<Optional<BlockState>> HELD_ROCK =
            SynchedEntityData.defineId(MoCEntityMiniGolem.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);

    /** Legacy {@code tcounter}: counts the overhead hold up to {@link #HOLD_TICKS}. */
    private int throwCounter;

    public MoCEntityMiniGolem(EntityType<? extends MoCEntityMiniGolem> type, Level level) {
        super(type, level);
    }

    /** Legacy applyEntityAttributes (MoCEntityMiniGolem:45-51): 15 HP, 0.25 speed, 2 attack damage. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    // No registerGoals() override: legacy initEntityAI (MoCEntityMiniGolem:37-43) was swim /
    // melee-attack / watch-player / nearest-player-target, and legacy MoCEntityMob's constructor
    // additionally installed an EntityAIWanderMoC2 at priority 4 that survived the initEntityAI
    // override. MoCMob.registerGoals() already lays down exactly that set (float, melee,
    // water-avoiding stroll, look-at-player, look-around, hurt-by and nearest-player targeting).

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANGRY, false);
        builder.define(HAS_ROCK, false);
        builder.define(HELD_ROCK, Optional.empty());
    }

    @Override
    public Identifier getTexture() {
        // Single-variant creature: legacy set texture = "minigolem.png" in the constructor and never
        // branched on type. The red "angry" skin is a second region of THIS sheet, selected by the model.
        return modelTexture("minigolem.png");
    }

    // ------------------------------------------------------------------ synched accessors

    /** True while the golem has an attack target — the client uses it to pick the red-hot skin. */
    public boolean getIsAngry() {
        return this.entityData.get(ANGRY);
    }

    public void setIsAngry(boolean angry) {
        this.entityData.set(ANGRY, angry);
    }

    /** True while a block is hoisted overhead — the client uses it for the arms-up pose. */
    public boolean getHasRock() {
        return this.entityData.get(HAS_ROCK);
    }

    public void setHasRock(boolean hasRock) {
        this.entityData.set(HAS_ROCK, hasRock);
    }

    /** The block currently held overhead, or {@code null} when empty-handed. */
    public @Nullable BlockState getHeldRock() {
        return this.entityData.get(HELD_ROCK).orElse(null);
    }

    private void setHeldRock(@Nullable BlockState state) {
        this.entityData.set(HELD_ROCK, Optional.ofNullable(state));
    }

    /**
     * Legacy {@code isMovementCeased()} (MoCEntityMiniGolem:118-121): a golem that is holding a rock AND
     * has someone to throw it at plants its feet and winds up instead of chasing.
     */
    public boolean isMovementCeased() {
        return getHasRock() && this.getTarget() != null;
    }

    // ------------------------------------------------------------------ AI

    /**
     * Faithful port of legacy {@code onLivingUpdate} (MoCEntityMiniGolem:76-99). Legacy ran this whole
     * block under {@code !world.isRemote}, which is exactly what {@code customServerAiStep} gives us.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        LivingEntity target = this.getTarget();
        setIsAngry(target != null);

        if (!getIsAngry() || target == null) {
            return;
        }

        if (!getHasRock() && this.random.nextInt(ACQUIRE_CHANCE) == 0) {
            acquireRock(level);
        }

        if (getHasRock()) {
            // Legacy cleared the path here and reported isMovementCeased() so its custom mover froze.
            // The 26.2 equivalent is to drop the path and kill the horizontal drift; travel() below
            // additionally refuses the walk input the MoveControl hands out after this method runs.
            this.getNavigation().stop();
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, motion.y, 0.0D);
            attackWithRock(level, target);
        }
    }

    /**
     * Legacy {@code isMovementCeased()} had teeth because legacy {@code MoCEntityMob} drove its own
     * movement. In 26.2 the {@code MoveControl} ticks <em>after</em> {@code customServerAiStep} and would
     * re-supply a walk input for the same tick's {@code travel}, so the freeze is enforced here instead:
     * a winding-up golem travels with zero input (gravity and friction still apply normally).
     */
    @Override
    public void travel(Vec3 input) {
        super.travel(isMovementCeased() ? Vec3.ZERO : input);
    }

    /**
     * Faithful port of legacy {@code acquireTRock} (MoCEntityMiniGolem:101-116) composed with
     * {@code MoCTools.destroyRandomBlockWithIBlockState(this, 3D)} (MoCTools:1086-1122).
     *
     * <p>Legacy sampled {@code distance^3 == 27} random positions in the 3x3x3 cube centred on the golem,
     * skipping the block it is standing on, and accepted the first one that was not air / water / bedrock
     * and had air directly above it (so it chews an exposed surface rather than tunnelling). The block was
     * removed from the world when {@code mobGriefing} was on. If nothing qualified, legacy set
     * {@code tcounter = 1} and stayed empty-handed — a quirk that is preserved because it means a failed
     * grab still nudges the wind-up counter off zero.</p>
     */
    private void acquireRock(ServerLevel level) {
        BlockPos source = findGrabbablePos(level);
        if (source == null) {
            this.throwCounter = 1;
            setHasRock(false);
            setHeldRock(null);
            return;
        }

        BlockState grabbed = level.getBlockState(source);
        // Legacy pulled the block out only under mobGriefing; without it the golem conjured a copy. Keeping
        // the grab either way preserves the mechanic, and since the port's thrown rock never turns back into
        // an item there is no duplication (see the class javadoc).
        if (level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            level.removeBlock(source, false);
        }
        setHeldRock(grabbed);
        setHasRock(true);
    }

    /**
     * Legacy {@code MoCTools.destroyRandomBlockWithIBlockState} sampling (MoCTools:1086-1101): up to
     * {@code distance^3} tries at {@code pos + rand(distance) - distance/2} on each axis. Returns the
     * position of a grabbable block, or {@code null} when the golem is standing somewhere it cannot dig.
     */
    private @Nullable BlockPos findGrabbablePos(ServerLevel level) {
        int span = (int) SEARCH_DISTANCE;           // 3
        int attempts = (int) (SEARCH_DISTANCE * SEARCH_DISTANCE * SEARCH_DISTANCE); // 27
        int originX = Mth.floor(this.getX());
        int originY = Mth.floor(this.getY());
        int originZ = Mth.floor(this.getZ());
        int halfSpan = span / 2;                    // 1

        for (int i = 0; i < attempts; i++) {
            BlockPos pos = new BlockPos(
                    originX + this.random.nextInt(span) - halfSpan,
                    originY + this.random.nextInt(span) - halfSpan,
                    originZ + this.random.nextInt(span) - halfSpan);

            // Legacy refused to pull the block directly under its own feet (MoCTools:1096-1098).
            if (pos.getY() == originY - 1 && pos.getX() == originX && pos.getZ() == originZ) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue; // legacy skipped AIR and WATER
            }
            if (state.getDestroySpeed(level, pos) < 0.0F) {
                continue; // legacy skipped BEDROCK; this also covers barriers and other unbreakables
            }
            if (!level.getBlockState(pos.above()).isAir()) {
                continue; // legacy required air directly above the target block
            }
            return pos;
        }
        return null;
    }

    /**
     * Faithful port of legacy {@code attackWithTRock} (MoCEntityMiniGolem:126-146). For the first 50 ticks
     * legacy did nothing but re-pin the held rock above the golem's head each tick (the port draws it
     * there instead, so those ticks are pure wind-up); on tick 50 the block is hurled if the target is
     * still within 48 blocks, and the golem empties its hands either way.
     */
    private void attackWithRock(ServerLevel level, LivingEntity target) {
        this.throwCounter++;
        if (this.throwCounter < HOLD_TICKS) {
            return;
        }

        if (target.isAlive() && this.distanceTo(target) < THROW_RANGE) {
            throwHeldRock(level, target);
        }

        setHasRock(false);
        setHeldRock(null);
        this.throwCounter = 0;
    }

    /**
     * Faithful port of legacy {@code MoCTools.ThrowStone(this, target, state, 10D, 0.25D)}
     * (MoCTools:1623-1656): the rock spawns at {@code posY + 0.5} and is given a velocity of
     * {@code (target - self) / 10} with {@code +0.25} of upward arc — an un-normalised lob, so a distant
     * target is hit by a much faster rock. {@link net.minecraft.world.entity.projectile.Projectile#shoot}
     * normalises then re-scales, so passing the vector's own length reproduces the legacy velocity exactly
     * while still setting the projectile's rotation properly.
     */
    private void throwHeldRock(ServerLevel level, LivingEntity target) {
        BlockState held = getHeldRock();

        MoCEntityRock rock = new MoCEntityRock(level, this);
        rock.setPos(this.getX(), this.getY() + 0.5D, this.getZ());

        // Throw the block it actually ripped up rather than the default cobblestone, when that block has an
        // item form at all (fire, for instance, does not — then the default cobblestone stands in).
        if (held != null) {
            Block block = held.getBlock();
            ItemStack stack = new ItemStack(block);
            if (!stack.isEmpty()) {
                rock.setItem(stack);
            }
        }

        double vx = (target.getX() - this.getX()) / THROW_SPEED_DIVISOR;
        double vy = (target.getY() - this.getY()) / THROW_SPEED_DIVISOR + THROW_ARC;
        double vz = (target.getZ() - this.getZ()) / THROW_SPEED_DIVISOR;
        float speed = (float) Math.sqrt((vx * vx) + (vy * vy) + (vz * vz));
        rock.shoot(vx, vy, vz, speed, 0.0F);

        level.addFreshEntity(rock);
        // Port addition: legacy MoCTools.ThrowStone was silent, so a 50-tick wind-up ended with no audible
        // cue at all. The golem family's own throw sound is reused, pitched up because this one is small.
        this.playSound(MoCSounds.GOLEMSHOOT.get(), 1.0F, 1.2F);
    }

    // ------------------------------------------------------------------ flavour

    /**
     * Legacy {@code isHarmedByDaylight()} returned true (MoCEntityMiniGolem:179-182): mini golems are
     * night spawns that catch fire on the open surface once the sun is up. {@link MoCMob} implements the
     * random-proc ignition legacy used.
     */
    @Override
    protected boolean burnsInDaylight() {
        return true;
    }

    /**
     * Legacy playStepSound (MoCEntityMiniGolem:159-162) routed through
     * {@code MoCTools.playCustomSound(this, ENTITY_GOLEM_WALK)}, which plays at volume 1.0 with a
     * +/-0.2 random pitch jitter (MoCTools:237-239) — reproduced exactly here, so the little golem
     * clunks with the same heavy stone footstep as its big brother instead of the block's step sound.
     */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(MoCSounds.GOLEMWALK.get(), 1.0F,
                1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
    }

    /** Legacy getAmbientSound -> {@code ENTITY_GOLEM_AMBIENT}, which resolves to the "golemgrunt" event. */
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.GOLEMGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.GOLEMHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.GOLEMDYING.get();
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Angry", getIsAngry());
        output.putBoolean("HasRock", getHasRock());
        output.putInt("ThrowCounter", this.throwCounter);
        BlockState held = getHeldRock();
        output.putString("HeldRock", held == null ? "" : BuiltInRegistries.BLOCK.getKey(held.getBlock()).toString());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setIsAngry(input.getBooleanOr("Angry", false));
        setHasRock(input.getBooleanOr("HasRock", false));
        this.throwCounter = input.getIntOr("ThrowCounter", 0);
        String key = input.getStringOr("HeldRock", "");
        if (key.isEmpty()) {
            setHeldRock(null);
            setHasRock(false);
        } else {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(key));
            setHeldRock(block == null ? null : block.defaultBlockState());
            if (block == null) {
                setHasRock(false);
            }
        }
    }
}
