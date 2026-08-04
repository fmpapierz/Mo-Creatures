package drzhark.mocreatures.entity.projectile;

import java.util.Optional;

import drzhark.mocreatures.entity.monster.MoCEntityGolem;
import drzhark.mocreatures.registry.MoCEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Faithful port of the legacy {@code MoCEntityThrowableRock}: a lightweight MISC entity that carries a
 * real {@link BlockState} and homes to the golem that summoned it, feeding it a cube on arrival. This
 * is the "vacuum" the golem uses to rebuild itself — {@link MoCEntityGolem#acquireRock} plucks a block
 * out of the world, spawns one of these carrying it, and the rock flies back into the golem's chest.
 *
 * <p>Behaviour (mirrors the legacy {@code behaviorType}):</p>
 * <ul>
 *   <li>{@code 2} — follow / absorb: accelerate toward the master golem; within ~1.5 blocks call
 *       {@link MoCEntityGolem#receiveRock(BlockState)} and vanish.</li>
 *   <li>{@code 4} — scatter (the golem is dying): fly outward and vanish after a short life.</li>
 * </ul>
 *
 * <p>Unlike the ballistic {@link MoCEntityRock}, this carries and renders an arbitrary block (via
 * {@link net.minecraft.network.syncher.EntityDataSerializers#OPTIONAL_BLOCK_STATE}). While airborne it
 * also deals 4 collateral damage per tick to any non-golem living entity in its motion-expanded path,
 * matching the legacy vacuum/scatter rocks.</p>
 */
public class MoCEntityThrowableRock extends Entity {

    /** Behaviour: home to the master golem and be absorbed. */
    public static final int BEHAVIOR_ABSORB = 2;
    /** Behaviour: scatter outward and expire (played when the golem crumbles). */
    public static final int BEHAVIOR_SCATTER = 4;

    /** The block this rock is carrying (synced for rendering). */
    private static final EntityDataAccessor<Optional<BlockState>> CARRIED =
            SynchedEntityData.defineId(MoCEntityThrowableRock.class, EntityDataSerializers.OPTIONAL_BLOCK_STATE);
    /** Homing behaviour selector (2 = absorb, 4 = scatter). */
    private static final EntityDataAccessor<Integer> BEHAVIOR =
            SynchedEntityData.defineId(MoCEntityThrowableRock.class, EntityDataSerializers.INT);
    /** Entity id of the master golem this rock homes to (legacy {@code masterID}). */
    private static final EntityDataAccessor<Integer> MASTER_ID =
            SynchedEntityData.defineId(MoCEntityThrowableRock.class, EntityDataSerializers.INT);

    /** Legacy homing accelerator: the smaller the value, the faster the rock closes on the golem. */
    private int acceleration = 100;
    /** Fuse: the rock drops its block as an item if it fails to reach the golem in time. */
    private int fuse = 250;

    public MoCEntityThrowableRock(EntityType<? extends MoCEntityThrowableRock> type, Level level) {
        super(type, level);
        // Fly cleanly to the golem without snagging on terrain or the golem's own hitbox; gravity is
        // never engine-applied (scatter rocks apply it manually in tick()).
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** Convenience server-side constructor used by the golem to launch a homing rock. */
    public MoCEntityThrowableRock(Level level, MoCEntityGolem master, double x, double y, double z,
            BlockState carried, int behavior) {
        this(MoCEntities.THROWABLE_ROCK.get(), level);
        this.setPos(x, y, z);
        this.setCarried(carried);
        this.setBehavior(behavior);
        this.setMasterId(master.getId());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CARRIED, Optional.empty());
        builder.define(BEHAVIOR, BEHAVIOR_ABSORB);
        builder.define(MASTER_ID, -1);
    }

    // ------------------------------------------------------------------ synced accessors

    public @Nullable BlockState getCarried() {
        return this.entityData.get(CARRIED).orElse(null);
    }

    public void setCarried(@Nullable BlockState state) {
        this.entityData.set(CARRIED, Optional.ofNullable(state));
    }

    public int getBehavior() {
        return this.entityData.get(BEHAVIOR);
    }

    public void setBehavior(int behavior) {
        this.entityData.set(BEHAVIOR, behavior);
    }

    public int getMasterId() {
        return this.entityData.get(MASTER_ID);
    }

    public void setMasterId(int id) {
        this.entityData.set(MASTER_ID, id);
    }

    private @Nullable MoCEntityGolem getMaster() {
        int id = getMasterId();
        if (id < 0) {
            return null;
        }
        Entity ent = this.level().getEntity(id);
        return ent instanceof MoCEntityGolem golem ? golem : null;
    }

    // ------------------------------------------------------------------ homing tick

    @Override
    public void tick() {
        super.tick();

        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        if (!(this.level() instanceof ServerLevel level)) {
            // Client only advances motion so the block visibly travels between sync packets.
            this.move(MoverType.SELF, this.getDeltaMovement());
            return;
        }

        // Fuse: if the rock never made it home, deposit its block back in the world as an item.
        if (--this.fuse <= 0) {
            dropCarried(level);
            this.discard();
            return;
        }

        MoCEntityGolem master = getMaster();
        int behavior = getBehavior();

        // Rock-damage code (legacy onEntityUpdate, all behaviours): while airborne the flying block
        // peppers any living entity in its motion-expanded path for 4 damage every tick, skipping the
        // master golem and any other golem. This is the golem's collateral "cubes damaging entities in
        // path" mechanic that runs for its whole vacuum/rebuild life.
        if (!this.onGround()) {
            DamageSource rockDamage = master != null
                    ? this.damageSources().mobAttack(master)
                    : this.damageSources().generic();
            AABB hitBox = this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D, 1.0D, 1.0D);
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != master && !(e instanceof MoCEntityGolem))) {
                victim.hurtServer(level, rockDamage, 4.0F);
            }
        }

        if (behavior == BEHAVIOR_ABSORB) {
            if (master == null || !master.isAlive()) {
                dropCarried(level);
                this.discard();
                return;
            }
            // Accelerate toward the golem (legacy: acceleration counts down to a floor of 10).
            if (--this.acceleration < 10) {
                this.acceleration = 10;
            }
            double dx = master.getX() - this.getX();
            double dz = master.getZ() - this.getZ();
            double distXZSq = dx * dx + dz * dz;
            if (distXZSq < 2.25D) { // within ~1.5 blocks horizontally
                master.receiveRock(getCarried());
                this.discard();
                return;
            }
            double speed = this.acceleration;
            double my = (master.getY() - this.getY()) / 20.0D + 0.15D;
            this.setDeltaMovement(dx / speed, my, dz / speed);
            this.move(MoverType.SELF, this.getDeltaMovement());
            return;
        }

        if (behavior == BEHAVIOR_SCATTER) {
            // The golem is crumbling: the rock drifts outward and expires quickly.
            if (this.acceleration > 30) {
                this.acceleration = 30;
            }
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x * 0.98D, motion.y - 0.04D, motion.z * 0.98D);
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.fuse < 220 || this.onGround()) {
                dropCarried(level);
                this.discard();
            }
            return;
        }

        // Fallback: fall like a normal rock.
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.98D, motion.y - 0.04D, motion.z * 0.98D);
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    /** Deposit the carried block as a ground item (fuse expiry / master lost). */
    private void dropCarried(ServerLevel level) {
        BlockState state = getCarried();
        if (state == null) {
            return;
        }
        Block block = state.getBlock();
        if (block == Blocks.AIR) {
            return;
        }
        ItemStack stack = new ItemStack(block);
        if (!stack.isEmpty()) {
            this.spawnAtLocation(level, stack);
        }
    }

    // ------------------------------------------------------------------ misc entity plumbing

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false; // an inert flying block cannot be attacked
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setBehavior(input.getIntOr("Behavior", BEHAVIOR_ABSORB));
        setMasterId(input.getIntOr("MasterID", -1));
        this.fuse = input.getIntOr("Fuse", 250);
        this.acceleration = input.getIntOr("Acceleration", 100);
        String key = input.getStringOr("Block", "");
        if (key.isEmpty()) {
            setCarried(null);
        } else {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(key));
            setCarried(block == null ? null : block.defaultBlockState());
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Behavior", getBehavior());
        output.putInt("MasterID", getMasterId());
        output.putInt("Fuse", this.fuse);
        output.putInt("Acceleration", this.acceleration);
        BlockState state = getCarried();
        String key = state == null ? "" : BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        output.putString("Block", key);
    }
}
