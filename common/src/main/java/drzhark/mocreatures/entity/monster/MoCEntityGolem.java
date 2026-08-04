package drzhark.mocreatures.entity.monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.entity.projectile.MoCEntityThrowableRock;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityGolem}. The golem's signature self-assembly: it is a walking pile
 * of 23 anatomical block "cubes" that it rebuilds by vacuuming nearby world blocks and sheds again when
 * struck. Phase 2/3 restores the legacy homing {@link MoCEntityThrowableRock}: instead of a direct
 * absorb, the golem plucks a block out of the world and a rock flies it back into the golem's open
 * chest ({@link #acquireRock}/{@link #receiveRock}). Each present cube now renders as its REAL absorbed
 * block and the power aura is drawn as a full-bright glowing shell — both implemented in
 * {@code MoCGolemRenderer} (GolemBlockCubeLayer + GolemAuraLayer); the golem's core head/chest keep the
 * {@code golemt.png} texture.
 *
 * <p>The 23 cube slots mirror the legacy layout:</p>
 * <pre>
 *   0-3   front chest (l_chest1, l_chest2, r_chest1, r_chest2)
 *   4     valuable back cube (seeded at spawn, never shed)
 *   5-8   back (l_back1, l_back2, r_back1, r_back2)
 *   9-11  left arm (l_shoulder, l_arm, l_hand)     [proximal -> distal]
 *   12-14 right arm (r_shoulder, r_arm, r_hand)    [proximal -> distal]
 *   15-17 left leg (l_thigh, l_knee, l_foot)       [proximal -> distal]
 *   18-20 right leg (r_thigh, r_knee, r_foot)      [proximal -> distal]
 *   21    groin
 *   22    butt
 * </pre>
 *
 * <p>The golem walks through a five-state life cycle ({@code golemState}): 0 spawned, 1 summoning
 * (rebuilding), 2 complete, 3 half-life (more dangerous), 4 dying.</p>
 */
public class MoCEntityGolem extends MoCMob {

    /** Number of anatomical cube slots. */
    public static final int CUBE_COUNT = 23;
    /** The valuable back cube — seeded at spawn and never shed. */
    private static final int VALUABLE_SLOT = 4;
    /** First arm slot / last arm slot (inclusive): slots 9-14. */
    private static final int ARM_FIRST = 9;
    private static final int ARM_LAST = 14;
    /** First leg slot / last leg slot (inclusive): slots 15-20. */
    private static final int LEG_FIRST = 15;
    private static final int LEG_LAST = 20;

    /** Golem life-cycle states. */
    private static final byte STATE_SPAWNED = 0;
    private static final byte STATE_SUMMONING = 1;
    private static final byte STATE_COMPLETE = 2;
    private static final byte STATE_HALF = 3;
    private static final byte STATE_DYING = 4;

    /** Hard clamp on a single hit — the legacy golem "could not be hit too hard". */
    private static final float MAX_HIT = 5.0F;
    /** Health ceiling the golem heals up to as it absorbs cubes. */
    private static final float HEALTH_CAP = 50.0F;
    /** Base movement speed at full leg complement; scaled down as legs are lost (0 with no legs). */
    private static final double BASE_SPEED = 0.25D;
    /** Ticks the golem lingers, crumbling, once it enters the dying state (legacy destroyed at dCounter > 140). */
    private static final int DYING_TICKS = 140;
    /** While the dying counter is below this the golem keeps grabbing & imploding nearby world blocks (legacy dCounter < 80). */
    private static final int DYING_SCATTER_UNTIL = 80;
    /** Mid-crumble groan point: play the golemdying sound once (legacy dCounter == 120). */
    private static final int DYING_SOUND_AT = 120;
    /** Ranged rock throw fires roughly every 40 ticks (2 s). */
    private static final int RANGED_INTERVAL = 40;
    /** Ranged engagement band (blocks): too close → melee, too far → ignore. */
    private static final double RANGED_MIN = 5.0D;
    private static final double RANGED_MAX = 16.0D;

    /** Blocks the valuable back cube may be seeded with. */
    private static final Block[] VALUABLES = {
            Blocks.GOLD_BLOCK, Blocks.IRON_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK
    };

    /**
     * Legacy {@code translateOre}+{@code generateBlock} whitelist: maps an absorbable world block to the
     * block the golem stores/drops for it. Only blocks in this map (plus the plank/log/leaf/wool tag
     * families handled in {@link #absorbedDropBlock}) can be vacuumed at all — everything else is rejected,
     * exactly as legacy {@code translateOre} returned {@code -1} for anything off the list. Ores are
     * <em>upgraded</em>: iron/gold ore drop the solid block, coal/lapis/redstone/diamond ore all drop
     * diamond ore, emerald ore drops the emerald block — the golem is a lucrative ore-refiner as in legacy.
     * A handful of legacy quirks are preserved (grass→dirt, mossy cobble→cobble, lapis block→wool,
     * furnace→fire[no item], snow→ice, jack-o-lantern/melon→pumpkin).
     */
    private static final Map<Block, Block> ABSORB_DROP = Map.ofEntries(
            Map.entry(Blocks.STONE, Blocks.STONE),
            Map.entry(Blocks.GRASS_BLOCK, Blocks.DIRT),
            Map.entry(Blocks.DIRT, Blocks.DIRT),
            Map.entry(Blocks.COBBLESTONE, Blocks.COBBLESTONE),
            Map.entry(Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE),
            Map.entry(Blocks.SAND, Blocks.SAND),
            Map.entry(Blocks.GRAVEL, Blocks.GRAVEL),
            Map.entry(Blocks.GOLD_ORE, Blocks.GOLD_BLOCK),
            Map.entry(Blocks.DEEPSLATE_GOLD_ORE, Blocks.GOLD_BLOCK),
            Map.entry(Blocks.GOLD_BLOCK, Blocks.GOLD_BLOCK),
            Map.entry(Blocks.IRON_ORE, Blocks.IRON_BLOCK),
            Map.entry(Blocks.DEEPSLATE_IRON_ORE, Blocks.IRON_BLOCK),
            Map.entry(Blocks.IRON_BLOCK, Blocks.IRON_BLOCK),
            Map.entry(Blocks.COAL_ORE, Blocks.DIAMOND_ORE),
            Map.entry(Blocks.DEEPSLATE_COAL_ORE, Blocks.DIAMOND_ORE),
            Map.entry(Blocks.LAPIS_ORE, Blocks.DIAMOND_ORE),
            Map.entry(Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DIAMOND_ORE),
            Map.entry(Blocks.REDSTONE_ORE, Blocks.DIAMOND_ORE),
            Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DIAMOND_ORE),
            Map.entry(Blocks.DIAMOND_ORE, Blocks.DIAMOND_ORE),
            Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DIAMOND_ORE),
            Map.entry(Blocks.EMERALD_ORE, Blocks.EMERALD_BLOCK),
            Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, Blocks.EMERALD_BLOCK),
            Map.entry(Blocks.EMERALD_BLOCK, Blocks.EMERALD_BLOCK),
            Map.entry(Blocks.DIAMOND_BLOCK, Blocks.DIAMOND_BLOCK),
            Map.entry(Blocks.GLASS, Blocks.GLASS),
            // Legacy grouped the lapis block and cloth/wool as one golem material (block IDs 22 & 35 both map to
            // golem-type 9), so an absorbed lapis block stays in the lapis family. (Colored-wool Block constants are
            // not visible on the common module's split-source classpath, so wool is represented by lapis here too.)
            Map.entry(Blocks.LAPIS_BLOCK, Blocks.LAPIS_BLOCK),
            Map.entry(Blocks.BRICKS, Blocks.BRICKS),
            Map.entry(Blocks.OBSIDIAN, Blocks.OBSIDIAN),
            Map.entry(Blocks.CRAFTING_TABLE, Blocks.CRAFTING_TABLE),
            Map.entry(Blocks.FURNACE, Blocks.FIRE),
            Map.entry(Blocks.SNOW, Blocks.ICE),
            Map.entry(Blocks.ICE, Blocks.ICE),
            Map.entry(Blocks.CACTUS, Blocks.CACTUS),
            Map.entry(Blocks.CLAY, Blocks.CLAY),
            Map.entry(Blocks.PUMPKIN, Blocks.PUMPKIN),
            Map.entry(Blocks.JACK_O_LANTERN, Blocks.PUMPKIN),
            Map.entry(Blocks.MELON, Blocks.PUMPKIN),
            Map.entry(Blocks.NETHERRACK, Blocks.NETHERRACK),
            Map.entry(Blocks.GLOWSTONE, Blocks.GLOWSTONE),
            Map.entry(Blocks.STONE_BRICKS, Blocks.STONE_BRICKS),
            Map.entry(Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS)
    );

    /** Client-visible bit-mask: bit {@code i} set iff cube {@code i} is present. */
    private static final EntityDataAccessor<Integer> CUBE_MASK =
            SynchedEntityData.defineId(MoCEntityGolem.class, EntityDataSerializers.INT);
    /** Client-visible life-cycle state (0-4). */
    private static final EntityDataAccessor<Byte> GOLEM_STATE =
            SynchedEntityData.defineId(MoCEntityGolem.class, EntityDataSerializers.BYTE);
    /** Client-visible throw windup flag: set while the golem is winding up / hurling a rock (drives the throwing pose). */
    private static final EntityDataAccessor<Boolean> GOLEM_THROWING =
            SynchedEntityData.defineId(MoCEntityGolem.class, EntityDataSerializers.BOOLEAN);
    /**
     * Client-visible block identities per cube, for the per-block cube rendering: a comma-joined list of
     * {@code CUBE_COUNT} numeric block ids ({@code 0} = empty; empty slots are hidden by the mask anyway).
     * Numeric ids are fine for a live sync since client and server share the same registry this session.
     */
    private static final EntityDataAccessor<String> CUBE_BLOCKS =
            SynchedEntityData.defineId(MoCEntityGolem.class, EntityDataSerializers.STRING);

    /**
     * Server-side authoritative store of which real block fills each cube ({@code null} = empty),
     * so a shed or dying golem drops the correct blocks. Never sent to the client — the client only
     * needs the presence bit-mask to show/hide model parts.
     */
    private final BlockState[] cubeBlocks = new BlockState[CUBE_COUNT];

    private int rangedCooldown;
    private int dyingCounter;
    /** Counts down while the throwing pose plays after a rock is hurled (legacy {@code tcounter}). */
    private int throwWindup;
    /** Guards one-time cube seeding for golems spawned outside the {@code finalizeSpawn} path (e.g. {@code /summon}). */
    private boolean seeded;

    public MoCEntityGolem(EntityType<? extends MoCEntityGolem> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH_CAP)
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CUBE_MASK, 0);
        builder.define(GOLEM_STATE, STATE_SPAWNED);
        builder.define(GOLEM_THROWING, false);
        builder.define(CUBE_BLOCKS, "");
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("golemt.png");
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.GOLEMGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        // Legacy getDeathSound() returns null: the golem plays its own golemdying (mid-crumble) and
        // golemexplode (on destroyGolem) sounds during the dying phase, not a vanilla death sound.
        return null;
    }

    /** Legacy playStepSound: the stone golem plays its heavy custom footstep sound instead of block steps. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(MoCSounds.GOLEMWALK.get(), 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ spawn / cube seeding

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        initGolemCubes();
        return data;
    }

    /** All cubes empty, then seed the valuable back cube with a random precious block. */
    private void initGolemCubes() {
        for (int i = 0; i < CUBE_COUNT; i++) {
            this.cubeBlocks[i] = null;
        }
        Block valuable = VALUABLES[this.random.nextInt(VALUABLES.length)];
        this.cubeBlocks[VALUABLE_SLOT] = valuable.defaultBlockState();
        this.seeded = true;
        rebuildMask();
        // Legacy getMoveSpeed() scaled by leg cubes every tick: a freshly spawned legless golem has 0 move
        // speed and stays frozen until it grows legs. Apply that immediately so it cannot wander at spawn.
        updateLegSpeed();
    }

    // ------------------------------------------------------------------ cube state accessors

    /** Client-facing presence mask: bit {@code i} set iff cube {@code i} is present. */
    public int getCubeMask() {
        return this.entityData.get(CUBE_MASK);
    }

    public int getGolemState() {
        return this.entityData.get(GOLEM_STATE);
    }

    private void setGolemState(int state) {
        this.entityData.set(GOLEM_STATE, (byte) state);
    }

    /** Client-facing throw flag: true while the golem is in its rock-throwing windup pose. */
    public boolean isThrowing() {
        return this.entityData.get(GOLEM_THROWING);
    }

    private void setThrowing(boolean throwing) {
        this.entityData.set(GOLEM_THROWING, throwing);
    }

    /** Recompute the synched presence mask + per-cube block ids from the authoritative {@link #cubeBlocks} array. */
    private void rebuildMask() {
        int mask = 0;
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < CUBE_COUNT; i++) {
            if (i > 0) {
                ids.append(',');
            }
            if (this.cubeBlocks[i] != null) {
                mask |= (1 << i);
                ids.append(BuiltInRegistries.BLOCK.getId(this.cubeBlocks[i].getBlock()));
            } else {
                ids.append('0');
            }
        }
        this.entityData.set(CUBE_MASK, mask);
        this.entityData.set(CUBE_BLOCKS, ids.toString());
    }

    /**
     * Client-facing per-cube block ids (comma-joined numeric block ids, {@code 0} = empty), for the
     * per-block cube renderer. Decoded by {@code MoCGolemRenderer}.
     */
    public String getCubeBlocksSync() {
        return this.entityData.get(CUBE_BLOCKS);
    }

    private boolean hasCube(int slot) {
        return this.cubeBlocks[slot] != null;
    }

    /** True if any cube slot is empty. */
    private boolean isMissingCubes() {
        for (int i = 0; i < CUBE_COUNT; i++) {
            if (this.cubeBlocks[i] == null) {
                return true;
            }
        }
        return false;
    }

    /** Count of present leg cubes (slots 15-20). */
    private int countLegCubes() {
        int count = 0;
        for (int i = LEG_FIRST; i <= LEG_LAST; i++) {
            if (hasCube(i)) {
                count++;
            }
        }
        return count;
    }

    /** True if the golem still owns at least one arm cube (slots 9-14). */
    private boolean hasAnyArmCube() {
        for (int i = ARM_FIRST; i <= ARM_LAST; i++) {
            if (hasCube(i)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ toughness

    /**
     * Faithful port of the legacy {@code attackEntityFrom} chest-armour puzzle. While the golem's chest is
     * still closed (no summoned rock hovering) AND at least one of the four chest cubes is present AND the
     * golem is not summoning, it is invulnerable to HP damage: the blow only <em>maybe</em> knocks one cube
     * loose (at difficulty-scaled odds — 100% easy / 50% normal / 33% hard) or is simply shrugged off. Real
     * damage is dealt only once the chest is exposed (a rock is inbound so the chest yawns open, or all four
     * chest cubes have been stripped), and even then it is clamped to {@link #MAX_HIT} so the golem can never
     * be one-shot. A summoning golem (state 1) and a dying golem (state 4) take no damage at all.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource src, float amount) {
        // A dying golem cannot be damaged — it self-destructs on its own crumble timer.
        if (getGolemState() == STATE_DYING) {
            return false;
        }

        int difficulty = level.getDifficulty().getId();
        boolean uncoveredChest = !hasCube(0) && !hasCube(1) && !hasCube(2) && !hasCube(3);

        // Chest still armoured (closed + at least one chest cube present) and not summoning:
        // absorb the blow entirely, only maybe shedding a cube. No HP is lost.
        if (!openChest() && !uncoveredChest && getGolemState() != STATE_SUMMONING) {
            if (difficulty > 0 && this.random.nextInt(difficulty) == 0) {
                destroyRandomGolemCube(level);
            } else {
                this.playSound(MoCSounds.TURTLEHURT.get(), 1.0F, 1.0F);
            }
            if (difficulty > 0 && src.getEntity() instanceof LivingEntity attacker && attacker != this) {
                this.setTarget(attacker);
            }
            return true;
        }

        // A summoning golem is invulnerable even with an exposed chest.
        if (getGolemState() == STATE_SUMMONING) {
            if (difficulty > 0 && src.getEntity() instanceof LivingEntity attacker && attacker != this) {
                this.setTarget(attacker);
            }
            return true;
        }

        // Chest exposed: take real damage, clamped so the golem can never be one-shot.
        boolean hurt = super.hurtServer(level, src, Math.min(amount, MAX_HIT));
        if (hurt && difficulty > 0 && src.getEntity() instanceof LivingEntity attacker && attacker != this) {
            this.setTarget(attacker);
        }
        return hurt;
    }

    /**
     * Knock a random present cube loose (legacy {@code destroyRandomGolemCube}): an outermost limb piece
     * peels first, the valuable back cube is never shed, and the freed (upgraded) block drops as a pickup.
     */
    private void destroyRandomGolemCube(ServerLevel level) {
        int slot = pickShedSlot();
        if (slot == -1) {
            return;
        }
        shedCube(level, slot);
        this.playSound(MoCSounds.GOLEMHURT.get(), 1.0F, 1.0F);
    }

    /**
     * Pick a present cube to shed, preferring a distal (outermost) limb piece so limbs peel from the
     * extremity inward. The valuable back cube (slot 4) is never shed.
     */
    private int pickShedSlot() {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < CUBE_COUNT; i++) {
            if (i != VALUABLE_SLOT && hasCube(i)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            return -1;
        }
        // Prefer a limb distal end (hand/foot) if any is present, so limbs shed outermost-first.
        int[] distal = {11, 14, 17, 20, 10, 13, 16, 19};
        for (int d : distal) {
            if (candidates.contains(d)) {
                return d;
            }
        }
        return candidates.get(this.random.nextInt(candidates.size()));
    }

    /** Empty a cube, drop its block as an item, and refresh the synched mask. */
    private void shedCube(ServerLevel level, int slot) {
        BlockState state = this.cubeBlocks[slot];
        if (state == null) {
            return;
        }
        this.cubeBlocks[slot] = null;
        rebuildMask();
        spawnUpgradedDrop(level, state);
        updateLegSpeed();
    }

    /** Drop a cube's block VERBATIM as a pickup item (used when a full golem spits a rejected rock back out). */
    private void spawnCubeDrop(ServerLevel level, BlockState state) {
        Block block = state.getBlock();
        ItemStack stack = new ItemStack(block);
        if (!stack.isEmpty()) {
            this.spawnAtLocation(level, stack);
        }
    }

    /**
     * Drop a stored cube as its legacy-upgraded block (ores become ingot/gem blocks via
     * {@link #absorbedDropBlock}) — used when a cube is shed by a hit or when the golem crumbles apart.
     */
    private void spawnUpgradedDrop(ServerLevel level, BlockState state) {
        Block dropBlock = absorbedDropBlock(state);
        if (dropBlock == null) {
            dropBlock = state.getBlock();
        }
        ItemStack stack = new ItemStack(dropBlock);
        if (!stack.isEmpty()) {
            this.spawnAtLocation(level, stack);
        }
    }

    /**
     * The block a given world block is absorbed/stored/dropped as, or {@code null} if the golem refuses to
     * absorb it. Mirrors legacy {@code translateOre} (whitelist) composed with {@code generateBlock} (the
     * ore→block upgrade). Plank/log/leaf/wool families are matched by tag so every wood/wool variant is
     * absorbable, collapsing to the canonical oak/white drop exactly as the metadata-losing legacy did.
     */
    private static @Nullable Block absorbedDropBlock(BlockState state) {
        Block mapped = ABSORB_DROP.get(state.getBlock());
        if (mapped != null) {
            return mapped;
        }
        if (state.is(BlockTags.PLANKS)) {
            return Blocks.OAK_PLANKS;
        }
        if (state.is(BlockTags.LOGS)) {
            return Blocks.OAK_LOG;
        }
        if (state.is(BlockTags.LEAVES)) {
            return Blocks.OAK_LEAVES;
        }
        if (state.is(BlockTags.WOOL)) {
            // Legacy stored cloth/wool as the lapis-family golem material (block ID 35 -> golem-type 9); wool
            // Block constants are also not resolvable on the common split-source classpath.
            return Blocks.LAPIS_BLOCK;
        }
        return null;
    }

    // ------------------------------------------------------------------ combat / assembly AI

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Seed cubes for golems that skipped finalizeSpawn (e.g. /summon) and were not NBT-loaded.
        if (!this.seeded) {
            initGolemCubes();
        }

        if (this.rangedCooldown > 0) {
            this.rangedCooldown--;
        }
        // Wind the throwing pose down; clear the synched flag when the windup ends.
        if (this.throwWindup > 0 && --this.throwWindup == 0) {
            setThrowing(false);
        }

        LivingEntity target = this.getTarget();

        // --- STATE MACHINE (faithful port of the legacy transitions) ---------------------------
        if (getGolemState() == STATE_SPAWNED) {
            // Wake up when a player wanders within 8 blocks.
            if (level.getNearestPlayer(this, 8.0D) != null) {
                setGolemState(STATE_SUMMONING);
            }
        }

        if (getGolemState() == STATE_SUMMONING && !isMissingCubes()) {
            setGolemState(STATE_COMPLETE);
        }

        if (getGolemState() > STATE_COMPLETE && getGolemState() != STATE_DYING && target == null) {
            setGolemState(STATE_SUMMONING);
        }

        // Re-bucket by health while engaged. Legacy `getGolemState() > 1`: a still-summoning golem
        // (state 1) is excluded so it keeps assembling instead of being promoted to complete/dying.
        if (getGolemState() > STATE_SUMMONING && target != null && this.random.nextInt(20) == 0) {
            float health = this.getHealth();
            if (health >= 30.0F) {
                setGolemState(STATE_COMPLETE);
            } else if (health >= 10.0F) {
                setGolemState(STATE_HALF);
            } else {
                setGolemState(STATE_DYING);
            }
        }

        // --- REBUILD: vacuum a nearby block in via a homing rock that flies into the open chest --
        if (getGolemState() != STATE_SPAWNED && getGolemState() != STATE_DYING && isMissingCubes()) {
            // Summon faster while actively rebuilding (state 1); otherwise the legacy difficulty/state-scaled
            // cadence (legacy: freq = 21 - state*difficulty, so rebuild speeds up on harder difficulty and
            // higher state). No extra cooldown — legacy had none.
            int freq = getGolemState() == STATE_SUMMONING ? 10
                    : Math.max(1, 21 - getGolemState() * level.getDifficulty().getId());
            if (this.random.nextInt(freq) == 0) {
                acquireRock(level, MoCEntityThrowableRock.BEHAVIOR_ABSORB);
            }
        }

        // --- DYING: freeze in place, chew up nearby terrain, groan mid-way, then explode -----
        if (getGolemState() == STATE_DYING) {
            this.setTarget(null);
            // Frozen while crumbling (legacy setPathToEntity(null) + isMovementCeased()==true for state 4):
            // stop pathing and cancel horizontal drift each tick so the stroll goal cannot wander it.
            this.getNavigation().stop();
            net.minecraft.world.phys.Vec3 dm = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, dm.y, 0.0D);

            this.dyingCounter++;
            // Grab and destroy a random nearby WORLD block, sending an imploding rock homing back toward the
            // golem (legacy acquireRock(4) while dCounter < 80 — identical to the rebuild grab, just a
            // behaviour-4 rock). The golem never sheds its OWN cubes here; those all drop at once in
            // destroyGolem(). A dying golem refuses to re-absorb, so receiveRock litters the grabbed block
            // back out as an item.
            if (this.dyingCounter < DYING_SCATTER_UNTIL && this.random.nextInt(3) == 0) {
                acquireRock(level, MoCEntityThrowableRock.BEHAVIOR_ABSORB);
            }
            // Mid-crumble groan, played once (legacy "golemdying" custom sound at dCounter == 120).
            if (this.dyingCounter == DYING_SOUND_AT) {
                this.playSound(MoCSounds.GOLEMDYING.get(), 1.0F, 1.0F);
            }
            // Final detonation: destroyGolem() scatters all remaining cubes + plays golemexplode
            // (legacy dCounter > 140).
            if (this.dyingCounter > DYING_TICKS) {
                destroyGolem(level);
            }
            return;
        }

        // --- RANGED: lob a real rock, consuming an arm cube in the process ---------------------
        if (target != null && target.isAlive() && hasAnyArmCube()
                && getGolemState() != STATE_SUMMONING) {
            double dist = this.distanceTo(target);
            if (this.rangedCooldown <= 0 && dist > RANGED_MIN && dist < RANGED_MAX) {
                this.rangedCooldown = RANGED_INTERVAL;
                throwRock(level, target);
            }
        }
    }

    /**
     * The vacuum (legacy {@code acquireRock}). Pick a random breakable world block in a wide radius, pluck
     * it out of the world (griefing permitting) and launch a homing {@link MoCEntityThrowableRock} carrying
     * it back toward the golem — the rock flies into the open chest and calls {@link #receiveRock(BlockState)}
     * on arrival. A rebuilding golem absorbs the block into a cube; a dying golem grabs and destroys terrain
     * the same way but refuses to re-absorb, so {@link #receiveRock} litters the block back out as an item.
     */
    private void acquireRock(ServerLevel level, int behavior) {
        BlockPos source = findAbsorbablePos(level);
        if (source == null) {
            return;
        }
        BlockState absorbed = level.getBlockState(source);

        MoCEntityThrowableRock rock = new MoCEntityThrowableRock(level, this,
                source.getX() + 0.5D, source.getY() + 0.5D, source.getZ() + 0.5D, absorbed, behavior);

        // Remove the source block from the world (mob griefing permitting) — the rock now carries it.
        if (level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            level.removeBlock(source, false);
        }
        level.addFreshEntity(rock);
    }

    /**
     * Deliver a vacuumed block into the first appropriate missing cube: chest cubes (0-3) first, then
     * limbs proximal-before-distal via {@link #firstMissingSlot()}. Heals a little and plays the attach
     * sound. If the golem is already full (or dying) the block is spat back out as a ground item.
     * Called from {@link MoCEntityThrowableRock} once the rock reaches the golem.
     */
    public void receiveRock(@Nullable BlockState carried) {
        if (!(this.level() instanceof ServerLevel level) || carried == null) {
            return;
        }

        // A crumbling golem never re-absorbs — the grabbed block just drops back into the world (legacy
        // dying golems grabbed terrain with behaviour-4 rocks that were never absorbed, only littered).
        if (getGolemState() == STATE_DYING) {
            spawnCubeDrop(level, carried);
            return;
        }

        int slot = firstMissingSlot();
        // Full golem, or a block off the legacy translateOre whitelist (absorbedDropBlock == null): the golem
        // refuses it, spitting the grabbed block back out verbatim as an item and grunting. Legacy receiveRock
        // took this same else-branch when the slot was -1 OR translateOre(ID) returned -1.
        if (slot == -1 || absorbedDropBlock(carried) == null) {
            spawnCubeDrop(level, carried);
            this.playSound(MoCSounds.TURTLEHURT.get(), 1.0F, 1.0F);
            return;
        }

        this.cubeBlocks[slot] = carried;
        rebuildMask();
        updateLegSpeed();

        // Heal by the difficulty setting (+1 easy / +2 normal / +3 hard) as it rebuilds, capped.
        int h = level.getDifficulty().getId();
        this.setHealth(Math.min(this.getHealth() + h, HEALTH_CAP));
        this.playSound(MoCSounds.GOLEMATTACH.get(), 0.6F, 1.0F);
    }

    /**
     * Faithful port of legacy {@code MoCTools.getRandomBlockCoords(this, 24D)}: sample random world blocks
     * in the legacy radius and return the position of a random breakable, exposed block at least ~10 blocks
     * away, or {@code null} if none is found. ANY solid breakable block qualifies — legacy {@code allowedBlock}
     * only skipped air/fluids/unbreakable/chests, NOT the translateOre whitelist — so the golem grief-destroys
     * arbitrary nearby terrain (sandstone, bookshelves, …) too; non-whitelisted blocks are rejected later in
     * {@link #receiveRock} and littered as items. The block is not removed here — the caller pulls it.
     */
    private @Nullable BlockPos findAbsorbablePos(ServerLevel level) {
        BlockPos origin = this.blockPosition();
        for (int attempt = 0; attempt < 64; attempt++) {
            // Legacy sampling ranges for distance 24: x/z = rand(24)-12 -> [-12,+11], y = rand(12)-6 -> [-6,+5].
            int dx = this.random.nextInt(24) - 12;
            int dy = this.random.nextInt(12) - 6;
            int dz = this.random.nextInt(24) - 12;
            // Legacy required the block be > 10 blocks away (spawnDist = dx*dx+dy*dy+dz*dz > 100).
            if (dx * dx + dy * dy + dz * dz <= 100) {
                continue;
            }
            BlockPos pos = origin.offset(dx, dy, dz);
            BlockState state = level.getBlockState(pos);
            if (!allowedBlock(level, pos, state)) {
                continue;
            }
            // Legacy required at least one exposed (air) face, so the golem chews surfaces rather than
            // hollowing out solid earth around itself.
            if (!hasExposedFace(level, pos)) {
                continue;
            }
            return pos;
        }
        return null;
    }

    /**
     * Legacy {@code MoCTools.allowedBlock}: any solid, breakable, non-chest block is grabbable. Legacy only
     * refused air, fluids (water/lava), bedrock and chests; the translateOre whitelist is applied later, not
     * here, so the golem happily rips out and destroys off-whitelist terrain during assembly/death.
     */
    private static boolean allowedBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.canBeReplaced()) {
            return false; // air, grass, snow layer, replaceable plants, fluids
        }
        if (!state.getFluidState().isEmpty()) {
            return false; // water / lava / waterlogged
        }
        Block block = state.getBlock();
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.ENDER_CHEST) {
            return false; // legacy skipped chests
        }
        return state.getDestroySpeed(level, pos) >= 0.0F; // reject bedrock / barrier / other unbreakable
    }

    /** True if any of the block's six faces is exposed to air (legacy neighbour-air check). */
    private static boolean hasExposedFace(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.above()).isAir()
                || level.getBlockState(pos.below()).isAir()
                || level.getBlockState(pos.north()).isAir()
                || level.getBlockState(pos.south()).isAir()
                || level.getBlockState(pos.east()).isAir()
                || level.getBlockState(pos.west()).isAir();
    }

    /**
     * The chest yawns open while the golem is missing cubes and a vacuumed rock is close by (within ~2
     * blocks). Drives the inhale pose on the client. Faithful port of the legacy {@code openChest()}.
     */
    public boolean openChest() {
        if (!isMissingCubes()) {
            return false;
        }
        AABB box = this.getBoundingBox().inflate(2.0D);
        return !this.level()
                .getEntitiesOfClass(MoCEntityThrowableRock.class, box, r -> r.isAlive())
                .isEmpty();
    }

    /**
     * The first cube slot that should be filled next: chest cubes (0-3) before anything else, then
     * for a limb the proximal piece before the distal, otherwise the lowest empty index.
     */
    private int firstMissingSlot() {
        // Chest first.
        for (int i = 0; i <= 3; i++) {
            if (!hasCube(i)) {
                return i;
            }
        }
        // Then every other slot in order, but for limbs skip a distal piece whose proximal neighbour
        // is still missing (fill shoulder before arm before hand, thigh before knee before foot).
        for (int i = 4; i < CUBE_COUNT; i++) {
            if (hasCube(i)) {
                continue;
            }
            if (isLimbSlot(i) && !proximalReady(i)) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private boolean isLimbSlot(int slot) {
        return (slot >= ARM_FIRST && slot <= ARM_LAST) || (slot >= LEG_FIRST && slot <= LEG_LAST);
    }

    /**
     * For a limb slot, true if its proximal neighbour is already present. Slots 9/12/15/18 are
     * proximal (always ready); 10/13/16/19 need their -1 neighbour; 11/14/17/20 need their -1
     * neighbour (which in turn required -2).
     */
    private boolean proximalReady(int slot) {
        if (slot == 9 || slot == 12 || slot == 15 || slot == 18) {
            return true; // proximal piece
        }
        return hasCube(slot - 1);
    }

    /** Scale movement speed by the fraction of leg cubes still present; with no legs the golem is immobile. */
    private void updateLegSpeed() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.setBaseValue(BASE_SPEED * (countLegCubes() / 6.0D));
    }

    /**
     * Hurl a real cobblestone rock toward the target, consuming one present arm cube (the thrown
     * rock represents that shed arm block). Preserves the port's rock shoot parameters.
     */
    private void throwRock(ServerLevel level, LivingEntity target) {
        // Consume a present arm cube, preferring a distal (hand) piece.
        int armSlot = pickArmSlot();
        if (armSlot == -1) {
            return;
        }
        this.cubeBlocks[armSlot] = null;
        rebuildMask();

        drzhark.mocreatures.entity.projectile.MoCEntityRock rock =
                new drzhark.mocreatures.entity.projectile.MoCEntityRock(level, this);
        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - rock.getY();
        double dz = target.getZ() - this.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        rock.shoot(dx, dy + horiz * 0.2D, dz, 1.6F, 8.0F);
        level.addFreshEntity(rock);
        this.playSound(MoCSounds.GOLEMSHOOT.get(), 1.0F, 1.0F);

        // Drive the throwing pose: arms/legs swing forward for the windup (legacy tcounter > 25).
        this.throwWindup = 15;
        setThrowing(true);
    }

    /** Pick a present arm cube (slots 9-14), preferring a distal hand then forearm. */
    private int pickArmSlot() {
        int[] order = {11, 14, 10, 13, 9, 12};
        for (int i : order) {
            if (hasCube(i)) {
                return i;
            }
        }
        return -1;
    }

    /** Drop every remaining cube block (including the valuable) and remove the golem. */
    private void destroyGolem(ServerLevel level) {
        dropAllCubes(level);
        this.playSound(MoCSounds.GOLEMEXPLODE.get(), 1.0F, 0.8F);
        this.discard();
    }

    /** Spawn every remaining cube's (upgraded) block as a pickup item, then clear the cube array + mask. */
    private void dropAllCubes(ServerLevel level) {
        for (int i = 0; i < CUBE_COUNT; i++) {
            if (this.cubeBlocks[i] != null) {
                spawnUpgradedDrop(level, this.cubeBlocks[i]);
                this.cubeBlocks[i] = null;
            }
        }
        rebuildMask();
    }

    /**
     * Safety net: if the golem is killed outright (a lethal clamped hit before the state machine flips
     * it to {@code dying}), still drop whatever cubes it was wearing.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        dropAllCubes(level);
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("GolemState", getGolemState());
        ValueOutput.ValueOutputList cubes = output.childrenList("GolemBlocks");
        for (int i = 0; i < CUBE_COUNT; i++) {
            ValueOutput child = cubes.addChild();
            child.putInt("Slot", i);
            BlockState state = this.cubeBlocks[i];
            String key = state == null ? "" : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            child.putString("Block", key);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setGolemState(input.getIntOr("GolemState", STATE_SPAWNED));
        for (int i = 0; i < CUBE_COUNT; i++) {
            this.cubeBlocks[i] = null;
        }
        for (ValueInput child : input.childrenListOrEmpty("GolemBlocks")) {
            int slot = child.getIntOr("Slot", -1);
            if (slot < 0 || slot >= CUBE_COUNT) {
                continue;
            }
            String key = child.getStringOr("Block", "");
            if (key.isEmpty()) {
                this.cubeBlocks[slot] = null;
            } else {
                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(key));
                this.cubeBlocks[slot] = block == null ? null : block.defaultBlockState();
            }
        }
        this.seeded = true;
        rebuildMask();
        updateLegSpeed();
    }
}
