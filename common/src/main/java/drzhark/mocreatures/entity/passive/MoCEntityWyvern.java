package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityWyvern}. A large, rideable winged dragon that actually FLIES:
 * wild wyverns wander the air (flight move-control + flight navigation, mirroring the insect flyers),
 * and a tamed, saddled wyvern is a flying mount steered by the rider's look direction (pitch sets
 * altitude — look up to climb, down to dive; forward to fly, back to brake; no input hovers).
 */
public class MoCEntityWyvern extends MoCAnimal {

    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(MoCEntityWyvern.class, EntityDataSerializers.BOOLEAN);
    /** Worn barding tier (legacy getArmorType): 0 none, 1 iron, 2 gold, 3 diamond. */
    private static final EntityDataAccessor<Integer> ARMOR_TYPE =
            SynchedEntityData.defineId(MoCEntityWyvern.class, EntityDataSerializers.INT);
    /** Storage-bag fitted (legacy getIsChested) — shows the storage geometry. */
    private static final EntityDataAccessor<Boolean> CHESTED =
            SynchedEntityData.defineId(MoCEntityWyvern.class, EntityDataSerializers.BOOLEAN);
    /** Server-side countdown of an in-progress wild flight (ticks); 0 = grounded/idle. */
    private int wildFlyTimer;
    /** Server-side countdown of an ambient ground stroll (ticks); 0 = not strolling. */
    private int walkTimer;
    /** Cooldown (ticks) between wild bites; 0 = ready. */
    private int attackCooldown;
    /**
     * Fitted storage inventory (legacy 14-slot {@code MoCAnimalChest}). Sized to a clean two-row vanilla
     * chest screen (18 slots), mirroring how the ostrich/horse present their animal chests. Persisted in
     * NBT and dropped on death only while {@link #getIsChested()} is set.
     */
    private final SimpleContainer chest = new SimpleContainer(18);
    /** Current wild aggression target (legacy {@code entityToAttack}); transient, server-side only. */
    private @Nullable LivingEntity huntTarget;

    public MoCEntityWyvern(EntityType<? extends MoCEntityWyvern> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        // Legacy MoCEntityWyvern:40-52 forces EVERY wyvern to start non-adult at edad 50-99. Every
        // saddle/ride/barding gate is written against adulthood, so a wyvern that spawned adult would be
        // instantly rideable — and the class's own growth tick would only ever run on egg-hatched ones.
        setAdult(false);
        setMoCAge(50 + this.random.nextInt(50));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
        builder.define(ARMOR_TYPE, 0);
        builder.define(CHESTED, false);
    }

    /** Worn barding tier (0 none, 1 iron, 2 gold, 3 diamond) — drives the model's armour geometry. */
    public int getArmorType() {
        return this.entityData.get(ARMOR_TYPE);
    }

    public void setArmorType(int tier) {
        this.entityData.set(ARMOR_TYPE, tier);
    }

    /** Whether the wyvern is carrying a storage bag (legacy getIsChested). */
    public boolean getIsChested() {
        return this.entityData.get(CHESTED);
    }

    public void setIsChested(boolean chested) {
        this.entityData.set(CHESTED, chested);
    }

    /** Drops the currently-worn barding item back into the world and clears the tier (legacy dropArmor). */
    private void dropArmor() {
        if (this.level().isClientSide()) {
            return;
        }
        int tier = getArmorType();
        if (tier > 0) {
            // Legacy dropArmor plays the "armor off" cue whenever a non-zero tier is removed/replaced.
            this.level().playSound(null, blockPosition(), MoCSounds.ARMOROFF.get(),
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        net.minecraft.world.item.Item item = switch (tier) {
            case 1 -> MoCItems.ARMORMETAL.get();
            case 2 -> MoCItems.ARMORGOLD.get();
            case 3 -> MoCItems.ARMORDIAMOND.get();
            default -> null;
        };
        if (item != null) {
            spawnAtLocation((net.minecraft.server.level.ServerLevel) this.level(),
                    new net.minecraft.world.item.ItemStack(item));
        }
        setArmorType(0);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source,
            boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        // Worn barding drops when the wyvern is slain.
        if (getArmorType() > 0) {
            dropArmor();
        }
        // Fitted saddle returns on death (legacy dropMyStuff -> MoCTools.dropSaddle: drops one horsesaddle when
        // rideable, then clears the flag), so a player who saddled a tamed wyvern gets the saddle back.
        if (isSaddled()) {
            spawnAtLocation(level, new ItemStack(MoCItems.HORSESADDLE.get()));
            setSaddled(false);
        }
        // Storage: scatter the stored contents and drop the chest block back (legacy dropMyStuff).
        if (getIsChested()) {
            for (int i = 0; i < this.chest.getContainerSize(); i++) {
                ItemStack s = this.chest.getItem(i);
                if (!s.isEmpty()) {
                    spawnAtLocation(level, s);
                }
            }
            this.chest.clearContent();
            spawnAtLocation(level, new ItemStack(Items.CHEST));
            setIsChested(false);
        }
        // Legacy dropFewItems opens with `if (!flag) return;` (flag = recently hit by a player), so the
        // coat-matched egg drops ONLY when a player recently damaged/killed the wyvern — never on a purely
        // environmental death (drowning, lava, suffocation, another mod's mob). Inside the Wyvern Lair it then
        // rolls a 1-in-10 chance (1-in-3 for a type-5 mother); the config wyvernEggDropChance (%) scales the
        // lesser-wyvern rate and gates the drop on/off (0 = never). The egg carries the parent's coat
        // (composite metadata type+49).
        int chance = drzhark.mocreatures.config.MoCConfig.get().wyvernEggDropChance;
        boolean inLair = level.dimension().equals(drzhark.mocreatures.item.MoCStaffPortalItem.WYVERN_LAIR);
        if (hitByPlayer && inLair && chance > 0) {
            boolean drop = getTypeMoC() == 5
                    ? this.random.nextInt(3) == 0        // mothers: legacy 1-in-3
                    : this.random.nextInt(100) < chance; // lesser: config %, legacy base rate 1-in-10
            if (drop) {
                ItemStack egg = new ItemStack(MoCItems.MOCEGG.get());
                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                tag.putInt("EggType", getTypeMoC() + 49); // legacy fishyegg meta: preserves the parent's coat
                egg.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(tag));
                spawnAtLocation(level, egg);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("ArmorType", getArmorType());
        output.putBoolean("Chested", getIsChested());
        net.minecraft.world.level.storage.ValueOutput.ValueOutputList items = output.childrenList("ChestItems");
        for (int i = 0; i < this.chest.getContainerSize(); i++) {
            ItemStack s = this.chest.getItem(i);
            if (!s.isEmpty()) {
                net.minecraft.world.level.storage.ValueOutput child = items.addChild();
                child.putInt("Slot", i);
                child.store("Item", ItemStack.CODEC, s);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        setArmorType(input.getIntOr("ArmorType", 0));
        setIsChested(input.getBooleanOr("Chested", false));
        this.chest.clearContent();
        for (net.minecraft.world.level.storage.ValueInput child : input.childrenListOrEmpty("ChestItems")) {
            int slot = child.getIntOr("Slot", -1);
            if (slot >= 0 && slot < this.chest.getContainerSize()) {
                child.read("Item", ItemStack.CODEC).ifPresent(s -> this.chest.setItem(slot, s));
            }
        }
        // Fix 1: max health follows the coat (type>=5 -> 80, else 40) without over-healing a wounded wyvern.
        applyTypeHealth(false);
    }

    /** Synched flight state (stable, hysteresis-free) — drives the wing-flap + leg-tuck animation. */
    public boolean isWyvernFlying() {
        return this.entityData.get(FLYING);
    }

    private void setWyvernFlying(boolean flying) {
        if (this.entityData.get(FLYING) != flying) {
            this.entityData.set(FLYING, flying);
        }
    }

    /**
     * Faithful port of the legacy {@code getCustomSpeed()}: a ridden wyvern moves at {@code 2.0} (the
     * lesser types 1-4) or {@code 3.0} (the larger types 5+, e.g. mother/light/dark); a wild one at
     * {@code 0.8}. The legacy 1.12.2 ride model accumulated momentum
     * ({@code motion += rider.motion * getCustomSpeed()} then {@code * 0.8} friction), so its terminal
     * speed was ~4x the per-tick add. The {@code SPEED_CALIBRATION} factor in travel() reproduces that
     * same terminal velocity in 26.2's different physics (an exact 1:1 port isn't possible).
     */
    private double getCustomSpeed() {
        if (this.isVehicle()) {
            return getTypeMoC() < 5 ? 2.0D : 3.0D;
        }
        return 0.8D;
    }

    /** Converts the legacy getCustomSpeed multiplier into a 26.2 per-tick flight velocity (tunable). */
    private static final double SPEED_CALIBRATION = 0.64D;

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D) // lesser wyvern base; type>=5 raised to 80 by applyTypeHealth
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.70D) // legacy getFlyingSpeed()
                .add(Attributes.ATTACK_DAMAGE, 5.0D); // lesser wyvern bite; type>=5 raised to 10 by applyTypeHealth
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Deliberately NO flying-wander goal: the FlyingMoveControl thrashes a large entity in place on
        // the ground (it keeps re-orienting toward an unreachable flight waypoint), which read as the
        // wyvern spinning/turning rapidly while "walking". A wild wyvern instead stands calmly when
        // grounded and moves only via the custom cruise (see travel/updateFlightState); ridden flight is
        // rider-controlled. (LookAt/RandomLookAround were also removed — they fought the rider's steering.)
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false; // a flyer never takes fall damage
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    // ------------------------------------------------------------ rideable flight
    @Override
    public void travel(Vec3 input) {
        if (this.isAlive() && this.isVehicle() && getControllingPassenger() instanceof Player rider) {
            // Steer to the rider's look (halved pitch keeps the nose from pointing straight up/down).
            this.setYRot(rider.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(rider.getXRot() * 0.5F);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();

            this.setNoGravity(true);
            float fwd = rider.zza;
            float str = rider.xxa;
            // Speed from the legacy getCustomSpeed (2.0 types 1-4 / 3.0 types 5+), calibrated to the
            // legacy momentum terminal velocity — so larger wyverns fly 1.5x faster, just like the original.
            float speed = (float) (getCustomSpeed() * SPEED_CALIBRATION);
            if (fwd != 0.0F || str != 0.0F) {
                Vec3 look = rider.getLookAngle();
                // Forward ONLY when actually pressing W/S — fwd==0 must add no forward motion, otherwise
                // strafing alone drifts diagonally. Side uses the LEFT vector to match xxa (A/left = +1),
                // so D strafes right and A strafes left.
                double fwdScale = fwd > 0.0F ? speed : (fwd < 0.0F ? -speed * 0.4D : 0.0D);
                Vec3 forward = look.scale(fwdScale);
                Vec3 side = new Vec3(look.z, 0.0D, -look.x).normalize().scale(str * speed * 0.5D);
                Vec3 desired = forward.add(side);
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D).add(desired.scale(0.5D)));
            } else {
                // No input: dampen horizontal speed and sink very slowly (a gentle hover).
                Vec3 dm = this.getDeltaMovement();
                this.setDeltaMovement(dm.x * 0.7D, Math.max(dm.y * 0.8D, -0.04D), dm.z * 0.7D);
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else if (this.wildFlyTimer > 0) {
            // Wild cruise: powered, gravity-free level flight in the facing direction (the vanilla flight
            // goal alone can't keep this large entity aloft, so the flight is driven directly here).
            this.setNoGravity(true);
            float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
            Vec3 fwd = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
            float speed = (float) (getCustomSpeed() * SPEED_CALIBRATION); // wild getCustomSpeed = 0.8
            // Climb if the ground is close below; otherwise cruise level.
            double vy = this.level().noCollision(this, this.getBoundingBox().move(0.0D, -2.5D, 0.0D)) ? 0.0D : 0.08D;
            Vec3 desired = new Vec3(fwd.x * speed, vy, fwd.z * speed);
            this.setDeltaMovement(this.getDeltaMovement().scale(0.6D).add(desired.scale(0.4D)));
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else if (this.huntTarget != null && this.huntTarget.isAlive()) {
            // Wild ground pursuit toward the current target (yaw set in updateAggression). Gravity on and
            // driven directly like the ambient stroll, so the flight move-control can't thrash the entity.
            this.setNoGravity(false);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.9F);
            super.travel(new Vec3(0.0D, 0.0D, 1.0D));
        } else if (this.walkTimer > 0) {
            // Ambient ground stroll: walk forward in the facing direction under normal gravity. Driven
            // directly (own speed + a forward travel vector) so the dormant flight move-control never
            // gets a chance to thrash the large entity into a spin while wandering on the ground.
            this.setNoGravity(false);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.6F);
            super.travel(new Vec3(0.0D, 0.0D, 1.0D));
        } else {
            this.setNoGravity(false);
            super.travel(input);
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Flight-state machine runs server-side every tick (tick() fires even while ridden, unlike the AI
        // step), then the FLYING flag syncs to the client to drive the animation.
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            updateAggression(serverLevel); // acquire/refresh the wild hunt target before the flight state reads it
            updateFlightState();
            // Maturation (legacy onLivingUpdate edad growth): an egg-hatched YOUNG wyvern ages up and turns adult,
            // which is what unlocks riding, saddling and barding (all gated on getIsAdult()). Without this a hatched
            // wyvern would stay non-adult forever and be permanently un-rideable. Naturally-spawned wyverns spawn
            // adult. Legacy advances age on a rare 1-in-500 tick roll (NOT every tick, so growth takes tens of
            // minutes), and the adult threshold is getMaxAge() = 100 for lesser coats but 180 for greater (type>=5).
            if (!getIsAdult() && this.random.nextInt(500) == 0) {
                setMoCAge(getMoCAge() + 1);
                if (getMoCAge() >= (getTypeMoC() >= 5 ? 180 : 100)) {
                    setAdult(true);
                }
            }
        }
    }

    /**
     * Wild wyverns actively hunt and retaliate (legacy {@code findPlayerToAttack} + {@code attackEntityFrom}):
     * on non-peaceful difficulty an untamed adult targets the closest vulnerable player within 10 blocks, or —
     * on a rare 1-in-500 roll when no player is near — the closest smaller non-hostile creature within 8 blocks
     * (legacy {@code getClosestEntityLiving}), or whoever last struck it; it then stalks toward the target on
     * the ground (movement applied in {@link #travel(Vec3)}) and bites — with a chance of venom — once within
     * reach. Deliberately navigation-free so it can't reintroduce the grounded-spin thrash the flight fixes
     * removed. A TAMED wyvern never seeks prey, but — faithful to the legacy {@code attackEntityFrom} — still
     * rounds on any non-rider, non-player attacker (only a PLAYER striking a tamed wyvern is shrugged off),
     * unless it is currently sitting or ridden (legacy {@code isMovementCeased}).
     */
    private void updateAggression(net.minecraft.server.level.ServerLevel level) {
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (!getIsAdult() || level.getDifficulty() == Difficulty.PEACEFUL) {
            this.huntTarget = null;
            return;
        }
        // Keep the current target while it stays valid and within ~12 blocks; otherwise re-acquire.
        LivingEntity target = this.huntTarget;
        if (target == null || !target.isAlive() || invalidPrey(target) || this.distanceToSqr(target) > 144.0D) {
            target = null;
        }
        if (getIsTamed()) {
            // Legacy attackEntityFrom sets entityToAttack for ANY non-rider, non-player attacker even when
            // tamed (a PLAYER hitting a tamed wyvern is ignored). Don't retaliate while sitting or ridden —
            // the mount/pet stays put (legacy isMovementCeased). Retaliation only; a tamed wyvern never hunts.
            if (target == null && !isVehicle() && !isSitting()) {
                LivingEntity last = getLastHurtByMob();
                if (last != null && last.isAlive() && !(last instanceof Player)
                        && this.distanceToSqr(last) <= 144.0D) {
                    target = last;
                }
            }
        } else {
            if (target == null) {
                // Nearest VULNERABLE player within 10 blocks (legacy getClosestVulnerablePlayerToEntity(this, 10)).
                double best = 100.0D; // 10^2
                for (Player p : level.players()) {
                    if (!p.isAlive() || invalidPrey(p)) {
                        continue;
                    }
                    double d = this.distanceToSqr(p);
                    if (d < best) {
                        best = d;
                        target = p;
                    }
                }
            }
            if (target == null && this.random.nextInt(500) == 0) {
                // Legacy findPlayerToAttack fallback: when no vulnerable player is near, a rare 1-in-500 roll
                // acquires the closest smaller, non-hostile creature within 8 blocks (getClosestEntityLiving),
                // so a wild wyvern occasionally hunts passing animals rather than only players.
                target = closestSmallPrey(level, 8.0D);
            }
            if (target == null) {
                LivingEntity last = getLastHurtByMob(); // retaliation: whoever struck this wild wyvern
                if (last != null && last.isAlive() && !invalidPrey(last) && this.distanceToSqr(last) <= 144.0D) {
                    target = last;
                }
            }
        }
        this.huntTarget = target;
        if (target != null) {
            // Face the target so the ground-pursuit branch in travel() drives toward it.
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            this.setYRot((float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            // Bite in reach (legacy attackEntity range f < 3.0 -> distSqr < 9.0), on a 20-tick cooldown.
            if (this.attackCooldown <= 0 && this.distanceToSqr(target) < 9.0D) {
                doHurtTarget(level, target);
                this.attackCooldown = 20;
            }
        }
    }

    /**
     * Closest small, non-hostile creature within {@code range} that a wild wyvern can prey on — the modern
     * analogue of legacy {@code getClosestEntityLiving} filtered through the wyvern's {@code entitiesToIgnore}:
     * it skips other wyverns, players and hostile {@link net.minecraft.world.entity.monster.Monster}s (legacy
     * {@code EntityMob}), anything at least as large as the wyvern, and anything out of line of sight.
     */
    private @Nullable LivingEntity closestSmallPrey(net.minecraft.server.level.ServerLevel level, double range) {
        net.minecraft.world.phys.AABB area = this.getBoundingBox().inflate(range);
        LivingEntity best = null;
        double bestDist = range * range;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != this && e.isAlive()
                        && !(e instanceof MoCEntityWyvern)
                        && !(e instanceof Player)
                        && !(e instanceof net.minecraft.world.entity.monster.Monster)
                        && e.getBbWidth() < this.getBbWidth()
                        && e.getBbHeight() < this.getBbHeight())) {
            double d = this.distanceToSqr(e);
            if (d < bestDist && this.hasLineOfSight(e)) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    /** A player target is invalid prey when spectating or in creative (legacy vulnerable-player filter). */
    private boolean invalidPrey(LivingEntity entity) {
        if (entity instanceof Player p) {
            return p.isSpectator() || p.getAbilities().instabuild;
        }
        return false;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof net.minecraft.world.entity.LivingEntity victim) {
            // Greater wyverns (mother / light / dark, type >= 5) bite for 10, lesser for 5 — legacy attackEntity
            // dealt ONE hit of `dmg` (5, or 10 for type>=5). That full value is delivered by the single
            // super.doHurtTarget hit via the per-coat ATTACK_DAMAGE base set in applyTypeHealth; a second
            // same-tick bonus hit is NOT issued here because the victim's invulnerability window would absorb it.
            // ~1/3 chance to inject venom, with the wyvern-poisoning cry (legacy attackEntity).
            if (this.random.nextInt(3) == 0) {
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.POISON, 200, 0), this);
                this.level().playSound(null, blockPosition(),
                        drzhark.mocreatures.registry.MoCSounds.WYVERNPOISONING.get(),
                        net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        }
        return hit;
    }

    private void updateFlightState() {
        if (this.isVehicle()) {
            // Ridden: stop AI navigation so the flight move-control can't fight the rider's steering;
            // flight pose whenever airborne.
            this.wildFlyTimer = 0;
            this.walkTimer = 0;
            this.getNavigation().stop();
            setWyvernFlying(!this.onGround());
            return;
        }
        if (this.wildFlyTimer > 0) {
            // Sustained aerial cruise. Keep the flight move-control out of it (nav stopped) so it can't
            // steer toward a waypoint while travel() also sets heading — that fight produced a spin.
            this.wildFlyTimer--;
            this.walkTimer = 0;
            this.getNavigation().stop();
            setWyvernFlying(true);
            if (this.getRandom().nextInt(50) == 0) {
                this.setYRot(this.getYRot() + (this.getRandom().nextFloat() - 0.5F) * 50.0F);
            }
            if (this.wildFlyTimer == 0) {
                this.setNoGravity(false); // glide back down and land
            }
            return;
        }

        setWyvernFlying(false);
        if (getIsTamed()) {
            return; // tamed wyverns stay put unless ridden
        }
        if (this.huntTarget != null && this.huntTarget.isAlive()) {
            // Actively hunting: pursuit movement is applied in travel(); don't start a random flight/stroll.
            this.walkTimer = 0;
            this.getNavigation().stop();
            return;
        }
        if (this.walkTimer > 0) {
            // Ambient ground stroll in progress (movement applied in travel()); drift heading gently.
            this.walkTimer--;
            this.getNavigation().stop();
            if (this.getRandom().nextInt(30) == 0) {
                this.setYRot(this.getYRot() + (this.getRandom().nextFloat() - 0.5F) * 40.0F);
            }
            return;
        }
        // Idle on the ground: occasionally take off into a cruise, or begin a ground stroll.
        if (this.onGround()) {
            int r = this.getRandom().nextInt(160);
            if (r == 0) {
                this.wildFlyTimer = 160 + this.getRandom().nextInt(200);
                this.getNavigation().stop();
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.5D, 0.0D)); // legacy wing-flap motionY
                this.hurtMarked = true;
            } else if (r <= 3) {
                this.walkTimer = 40 + this.getRandom().nextInt(60);
                this.setYRot(this.getRandom().nextFloat() * 360.0F);
                this.getNavigation().stop();
            }
        }
    }

    // ------------------------------------------------------------ essence-feeding evolution
    /** True if {@code stack} is one of the wyvern transformation essences. */
    private static boolean isWyvernEssence(ItemStack stack) {
        return stack.is(MoCItems.ESSENCELIGHT.get()) || stack.is(MoCItems.ESSENCEUNDEAD.get())
                || stack.is(MoCItems.ESSENCEDARKNESS.get());
    }

    /**
     * Faithful port of the legacy {@code interact} essence/vial feeding on a TAMED wyvern. The
     * texture switch maps coat types as: 1 jungle, 2 swamp/mix, 3 sand, 4 savanna/sun (default),
     * 5 mother, 6 undead, 7 light, 8 dark.
     *
     * <ul>
     *   <li>Feeding {@link MoCItems#ESSENCELIGHT} to a tamed adult lesser wyvern (type &lt; 5) does NOT
     *       change the wyvern: it lays a {@link MoCEntityEgg} of its OWN coat (legacy {@code viallight}
     *       reproduction — egg id {@code getType()+49}), consuming the vial for a glass bottle.</li>
     *   <li>On a mature/type-5 (mother) wyvern: {@code ESSENCEUNDEAD -> undead} (6),
     *       {@code ESSENCELIGHT -> light} (7), {@code ESSENCEDARKNESS -> dark} (8), matching the legacy
     *       {@code vialundead}/{@code viallight}/{@code vialdarkness} coat swaps.</li>
     * </ul>
     *
     * The type-5 coat swaps consume one essence, return a glass bottle, play the transform sound, and
     * rescale max health for the new coat. A held {@link MoCItems#KEY} opens the fitted storage; a chest
     * fits the storage bag (granting the key). Non-matching interactions defer to the base class.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // KEY opens the fitted storage (vanilla two-row chest screen), whatever else may be true.
        if (getIsTamed() && getIsChested() && stack.is(MoCItems.KEY.get())) {
            if (!this.level().isClientSide()) {
                openChest(player);
            }
            return InteractionResult.SUCCESS;
        }

        if (getIsTamed() && isWyvernEssence(stack)) {
            int type = getTypeMoC();
            if (type < 5) {
                // Lesser wyvern + Essence of Light (adult only, legacy edad > 90): lay a same-coat egg —
                // a reproduction mechanic. The wyvern KEEPS its coat (no mutation to the mother line).
                if (stack.is(MoCItems.ESSENCELIGHT.get()) && getIsAdult()) {
                    if (this.level() instanceof ServerLevel sl) {
                        MoCEntityEgg egg = new MoCEntityEgg(MoCEntities.EGG.get(), sl);
                        egg.setEggType(getTypeMoC() + 49); // coat-matched wyvern egg (composite id 50-54)
                        egg.setPos(player.getX(), player.getY(), player.getZ());
                        egg.setDeltaMovement(
                                (this.random.nextFloat() - this.random.nextFloat()) * 0.3D,
                                this.random.nextFloat() * 0.05D,
                                (this.random.nextFloat() - this.random.nextFloat()) * 0.3D);
                        sl.addFreshEntity(egg);
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            player.addItem(new ItemStack(Items.GLASS_BOTTLE));
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
            } else if (type == 5) {
                // Mature mother wyvern branches into the three special coats.
                int target = -1;
                if (stack.is(MoCItems.ESSENCEUNDEAD.get())) {
                    target = 6; // undead
                } else if (stack.is(MoCItems.ESSENCELIGHT.get())) {
                    target = 7; // light
                } else if (stack.is(MoCItems.ESSENCEDARKNESS.get())) {
                    target = 8; // dark
                }
                if (target != -1) {
                    if (this.level() instanceof ServerLevel) {
                        setTypeMoC(target);
                        applyTypeHealth(true); // stays a greater wyvern -> 80 HP
                        // Consume one essence and give back a single empty bottle (creative keeps its stack).
                        if (!player.getAbilities().instabuild) {
                            stack.shrink(1);
                            player.addItem(new ItemStack(Items.GLASS_BOTTLE));
                        }
                        this.level().playSound(null, blockPosition(), MoCSounds.TRANSFORM.get(),
                                SoundSource.NEUTRAL, 1.0F, 1.0F);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        // BARDING: a tamed adult wyvern (legacy edad > 90) fitted with the horse-armour items wears iron/gold/
        // diamond barding (legacy reused horsearmormetal/gold/diamond); a chest fits the storage bag. Re-fitting
        // a different tier drops the old set first.
        if (getIsTamed() && getIsAdult() && this.level() instanceof ServerLevel sl) {
            int newTier = stack.is(MoCItems.ARMORMETAL.get()) ? 1
                    : stack.is(MoCItems.ARMORGOLD.get()) ? 2
                    : stack.is(MoCItems.ARMORDIAMOND.get()) ? 3 : 0;
            if (newTier > 0 && getArmorType() != newTier) {
                boolean wasBare = getArmorType() == 0;
                dropArmor();
                setArmorType(newTier);
                if (wasBare) {
                    sl.playSound(null, blockPosition(), MoCSounds.ARMORPUT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            if (stack.is(Items.CHEST) && !getIsChested()) {
                setIsChested(true);
                player.addItem(new ItemStack(MoCItems.KEY.get())); // the key opens the storage GUI (legacy)
                sl.playSound(null, blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0F,
                        ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F); // legacy chickenplop
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    /** Opens the fitted 18-slot storage for {@code player} using the vanilla two-row chest screen (server-side). */
    private void openChest(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x2,
                            id, inv, this.chest, 2), getDisplayName()));
        }
    }

    // --------------------------------------------------------------- whip effect (legacy MoCItemWhip)
    /**
     * Whip crack on a tamed wyvern: toggle sitting, but ONLY when grounded (legacy {@code !isOnAir()}).
     * A wyvern in flight ignores the crack so it can't be dropped out of the sky. Returns {@code true}
     * when the toggle was applied (grounded), {@code false} otherwise. Server-side.
     */
    public boolean whipToggleSit() {
        if (this.isWyvernFlying() || !this.onGround()) {
            return false; // airborne: cannot sit mid-flight
        }
        setSitting(!isSitting());
        return true;
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 25) {
                setTypeMoC(1);
            } else if (i <= 50) {
                setTypeMoC(2);
            } else if (i <= 75) {
                setTypeMoC(3);
            } else if (i <= 98) {
                setTypeMoC(4);
            } else {
                setTypeMoC(5);
            }
        }
        // Fix 1: greater wyverns (type>=5) have 80 HP, lesser 40 (legacy dynamic getMaxHealth); heal a
        // freshly-selected mother/light/dark to full.
        applyTypeHealth(true);
    }

    /**
     * Faithful port of the legacy dynamic {@code getMaxHealth()} and the {@code attackEntity} bite damage:
     * greater wyverns (mother/light/dark, coat type &gt;= 5) have 80 max health and bite for 10, lesser types
     * 40 health and bite for 5. Re-applied after any coat change; when the health cap rises the wyvern is
     * healed to full (a newly-grown mother is at full strength), but not on a plain NBT load (which must
     * preserve a wounded wyvern's saved health). Server-side only.
     */
    private void applyTypeHealth(boolean healOnIncrease) {
        if (this.level().isClientSide()) {
            return;
        }
        AttributeInstance attr = getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            double target = getTypeMoC() >= 5 ? 80.0D : 40.0D;
            double current = attr.getBaseValue();
            if (current != target) {
                attr.setBaseValue(target);
                if (healOnIncrease && target > current) {
                    setHealth((float) target);
                }
            }
        }
        // Fix (cycle-3): per-coat bite damage (legacy attackEntity `dmg = 5; if (getType()>=5) dmg = 10`). The
        // ATTACK_DAMAGE base can't be raised in the static createAttributes(), so set it per coat here — the
        // single super.doHurtTarget hit then lands the full 10 for greater wyverns (no absorbed second hit).
        AttributeInstance atk = getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            double atkTarget = getTypeMoC() >= 5 ? 10.0D : 5.0D;
            if (atk.getBaseValue() != atkTarget) {
                atk.setBaseValue(atkTarget);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 1 -> modelTexture("wyvernjungle.png");
            case 2 -> modelTexture("wyvernmix.png");
            case 3 -> modelTexture("wyvernsand.png");
            case 5 -> modelTexture("wyvernmother.png");
            case 6 -> modelTexture("wyvernundead.png");
            case 7 -> modelTexture("wyvernlight.png");
            case 8 -> modelTexture("wyverndark.png");
            default -> modelTexture("wyvernsun.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.WYVERNGRUNT.get();
    }

    /** Played when a wild wyvern throws its rider off while being broken in. */
    @Override
    protected @Nullable SoundEvent getAngrySound() {
        return MoCSounds.WYVERNHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.WYVERNHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.WYVERNDYING.get();
    }
}
