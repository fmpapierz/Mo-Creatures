package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.entity.IMoCManticore;
import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityManticore} — the WILD manticore: a winged, scorpion-tailed big cat
 * that hunts players, flies, and stings with a venom that depends on where it was born.
 *
 * <p><b>Four coats, chosen by where it spawns</b> (legacy {@code checkSpawningBiome} + {@code selectType},
 * {@code MoCEntityManticore:61-90}): the Nether breeds the red manticore (type 1), snowy/frozen biomes the
 * blue one (3), and everywhere else it is an even coin-flip between the dark (2) and green (4) coats. The
 * coat is not cosmetic — it picks the sting (below) and the death drop.
 *
 * <p><b>The sting</b> ({@code applyEnchantments:330-359}). On roughly one melee hit in five the manticore
 * throws its scorpion tail forward (a 50-tick animation, the first 15 of which are the visible strike) and
 * injects its coat's venom: green/dark POISON, blue SLOWNESS, red sets the victim alight — but only outside
 * the Nether, where fire would be a poor threat. Every other hit just gapes the maw. The legacy calls into
 * {@code MoCreatures.poisonPlayer/freezePlayer/burnPlayer} alongside the potion effect are NOT ported
 * because they are empty {@code // TODO 4FIX} stubs in 12.0.5 itself (they used to tint the player's HUD).
 *
 * <p><b>Flight.</b> Legacy declared {@code isFlyer()} and let {@code MoCEntityMob}'s custom flying wander
 * carry the manticore between {@code minFlyingHeight() = 1} and {@code maxFlyingHeight() = 10} blocks off
 * the ground. This port has no flying-monster base class, and the one large flyer that does exist
 * ({@code MoCEntityWyvern}) documents that hanging a {@code FlyingMoveControl} off a big entity makes it
 * spin in place on the ground. So the manticore keeps ordinary ground pathing for the chase and flies the
 * way a pouncing cat with wings would: it beats its wings and SWOOPS at prey from several blocks out, then
 * glides down (the legacy {@code motionY *= 0.6} slow-fall, which sat commented out in 12.0.5 precisely
 * because the flying pathfinder made it redundant) and never takes fall damage. See {@link #tickFlight}.
 *
 * <p><b>Undead riders</b> ({@code MoCTools.findMobRider}, called on a 1-in-200 tick): a manticore with an
 * empty back picks up a nearby skeleton or zombie, which then rides it — the source of the flying skeletons
 * that made legacy Nether trips memorable.
 */
public class MoCEntityManticore extends MoCMob implements IMoCManticore {

    /** Synched: a wing beat is in progress; the client sweeps the wings at full amplitude. */
    private static final EntityDataAccessor<Boolean> FLAPPING =
            SynchedEntityData.defineId(MoCEntityManticore.class, EntityDataSerializers.BOOLEAN);
    /** Synched: the sting is mid-strike (legacy {@code swingingTail()}), so the barb whips forward. */
    private static final EntityDataAccessor<Boolean> STINGING =
            SynchedEntityData.defineId(MoCEntityManticore.class, EntityDataSerializers.BOOLEAN);
    /** Synched: the maw is open (legacy {@code mouthCounter}), dropping the lower jaw. */
    private static final EntityDataAccessor<Boolean> OPEN_JAW =
            SynchedEntityData.defineId(MoCEntityManticore.class, EntityDataSerializers.BOOLEAN);

    /**
     * Legacy {@code rareItemDropChance}, whose 12.0.5 config default is 25 ("a 25% chance to drop a rare
     * item"). The port's {@code MoCConfig} has no equivalent knob, so the legacy default is inlined here
     * exactly as the other ported rare drops (horse 1-in-4, ostrich 1-in-3) inline theirs.
     */
    private static final int RARE_ITEM_DROP_CHANCE = 25;

    /** Legacy {@code wingFlapCounter}: runs 1..20; the flap sound fires on tick 5 of the beat. */
    private int wingFlapCounter;
    /** Legacy {@code poisontimer}: runs 1..50 while a sting is being delivered. */
    private int poisonTimer;
    /** Legacy {@code isPoisoning}: a sting is in progress (server-side truth behind {@link #STINGING}). */
    private boolean poisoning;
    /** Legacy {@code mouthCounter}, collapsed to a countdown that closes the jaw again. */
    private int jawTicks;

    public MoCEntityManticore(EntityType<? extends MoCEntityManticore> type, Level level) {
        super(type, level);
    }

    /**
     * Legacy {@code applyEntityAttributes}: 40 health, movement speed 0.4 (deliberately fast — a manticore
     * is meant to run a player down) and a 6-damage bite on top of the venom.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAPPING, false);
        builder.define(STINGING, false);
        builder.define(OPEN_JAW, false);
    }

    // ------------------------------------------------------------------------------- coat / textures

    /**
     * Legacy {@code checkSpawningBiome} + {@code selectType} ({@code MoCEntityManticore:61-90}). Legacy ran
     * the biome check on EVERY selectType call and let it overwrite the coat; here it only fires for an
     * unassigned coat (type 0) so a spawn-egg / NBT / egg-hatched manticore keeps the coat it was given.
     * The frost threshold matches the one the ported frost scorpion uses for the same legacy SNOWY test.
     */
    @Override
    public void selectType() {
        if (getTypeMoC() != 0) {
            return;
        }
        if (this.level().dimension() == Level.NETHER) {
            setTypeMoC(1); // red / nether manticore
        } else if (this.level().getBiome(this.blockPosition()).value().getBaseTemperature() <= 0.05F) {
            setTypeMoC(3); // blue / frost manticore
        } else {
            // Legacy: (rand.nextInt(2) * 2) + 2 -> an even split between the dark (2) and green (4) coats.
            setTypeMoC((this.random.nextInt(2) * 2) + 2);
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 1 -> modelTexture("bcmanticore.png");      // nether / red
            case 2 -> modelTexture("bcmanticoredark.png");  // cave / dark
            case 3 -> modelTexture("bcmanticoreblue.png");  // frost / blue
            default -> modelTexture("bcmanticoregreen.png"); // type 4 and fallback
        };
    }

    /** Legacy {@code getSizeFactor() == 1.4F}: a manticore renders half again as large as a lion. */
    @Override
    public float getSizeFactor() {
        return 1.4F;
    }

    // --------------------------------------------------------------------------- resistances / flight

    /** Legacy constructor set {@code isImmuneToFire = true} for every coat, not just the Nether one. */
    @Override
    public boolean fireImmune() {
        return true;
    }

    /** Legacy {@code fall()} is an empty override: a flyer is never hurt by hitting the ground. */
    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /**
     * Legacy {@code isHarmedByDaylight() == true}. Purely cosmetic in practice, in 12.0.5 as much as here:
     * {@code MoCEntityMob} answers a daylight proc with {@code setFire(8)}, and the manticore is
     * unconditionally fire-immune, so a manticore caught in the open smoulders without ever taking damage.
     */
    @Override
    protected boolean burnsInDaylight() {
        return true;
    }

    // ----------------------------------------------------------------------------- animation plumbing

    @Override
    public boolean isWingFlapping() {
        return this.entityData.get(FLAPPING);
    }

    @Override
    public boolean isStingStriking() {
        return this.entityData.get(STINGING);
    }

    @Override
    public boolean getJawOpen() {
        return this.entityData.get(OPEN_JAW);
    }

    /** Legacy {@code openMouth()}: gapes the maw for a roar or a bite. Server-side only. */
    private void openJaw() {
        if (this.level().isClientSide()) {
            return;
        }
        this.jawTicks = 30; // legacy mouthCounter ran to 30
        if (!getJawOpen()) {
            this.entityData.set(OPEN_JAW, true);
        }
    }

    /**
     * Legacy {@code wingFlap()}: starts a 20-tick beat (and broadcast it to nearby clients as animation 3;
     * here the {@link #FLAPPING} synched flag does that job). A beat already in progress is not restarted.
     */
    private void wingFlap() {
        if (this.wingFlapCounter == 0) {
            this.wingFlapCounter = 1;
            this.entityData.set(FLAPPING, true);
        }
    }

    /** Legacy {@code setPoisoning(true)}: begins the 50-tick sting cycle (the strike is its first 15 ticks). */
    private void startSting() {
        this.poisoning = true;
        this.poisonTimer = 0;
        this.entityData.set(STINGING, true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        // Animation counters are transient in legacy too (plain fields, never written to NBT); only the
        // sting state is worth persisting so a manticore saved mid-sting doesn't keep the pose forever.
        output.putBoolean("Poisoning", this.poisoning);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.poisoning = input.getBooleanOr("Poisoning", false);
    }

    // ------------------------------------------------------------------------------------- AI tick

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // --- jaw (legacy mouthCounter 1..30) ---
        if (this.jawTicks > 0 && --this.jawTicks == 0 && getJawOpen()) {
            this.entityData.set(OPEN_JAW, false);
        }

        // --- wing beat (legacy onUpdate:202-204 + onLivingUpdate:238-249) ---
        // The beat runs 20 ticks; its downstroke (tick 5) is what you hear.
        if (this.wingFlapCounter > 0 && ++this.wingFlapCounter > 20) {
            this.wingFlapCounter = 0;
            this.entityData.set(FLAPPING, false);
        }
        if (this.wingFlapCounter == 5) {
            level.playSound(null, blockPosition(), MoCSounds.WINGFLAP.get(), SoundSource.HOSTILE, 1.0F,
                    1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
        }
        // Airborne manticores beat constantly (legacy rolled 1-in-5 per tick while off the ground, and again
        // on a speed-derived 1-in-5..1-in-25); a grounded one stretches its wings now and then (1-in-500).
        if (!this.onGround() && this.random.nextInt(5) == 0) {
            wingFlap();
        } else if (this.onGround() && this.random.nextInt(500) == 0) {
            wingFlap();
        }

        // --- sting cycle (legacy onLivingUpdate:251-260) ---
        if (this.poisoning) {
            this.poisonTimer++;
            if (this.poisonTimer == 1) {
                level.playSound(null, blockPosition(), MoCSounds.SCORPIONSTING.get(), SoundSource.HOSTILE,
                        1.0F, 1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
            }
            // Legacy swingingTail(): only the first 15 of the 50 ticks are the visible forward strike.
            if (this.poisonTimer >= 15 && isStingStriking()) {
                this.entityData.set(STINGING, false);
            }
            if (this.poisonTimer > 50) {
                this.poisonTimer = 0;
                this.poisoning = false;
            }
        }

        tickFlight(level);

        // --- undead rider (legacy onLivingUpdate:266-268 -> MoCTools.findMobRider) ---
        if (!this.isVehicle() && this.random.nextInt(200) == 0) {
            findMobRider(level);
        }
    }

    /**
     * The manticore's flight, as close as this port can get to the legacy flying wander (see the class
     * javadoc for why a {@code FlyingMoveControl} is not used):
     *
     * <ul>
     *   <li><b>Swoop.</b> With prey 3-16 blocks away and its feet on the ground, a 1-in-40 tick roll beats
     *       the wings and launches the manticore in an arc at the target — the winged cousin of the big
     *       cat's pounce ({@code MoCEntityBigCat}'s legacy {@code attackEntity} leap), with extra lift when
     *       the prey is above it.</li>
     *   <li><b>Idle hover.</b> Unoccupied, it occasionally (1-in-300) lifts off just to be airborne, which
     *       is what the legacy wander did continuously between heights 1 and 10.</li>
     *   <li><b>Glide.</b> Descending, vertical speed is damped to 60% — literally the legacy
     *       {@code motionY *= 0.6D} slow-fall, which 12.0.5 left commented out only because its flying
     *       pathfinder already kept the manticore up. Without it a swoop would read as a face-plant.</li>
     * </ul>
     */
    private void tickFlight(ServerLevel level) {
        LivingEntity target = getTarget();
        if (this.onGround()) {
            if (target != null && target.isAlive()) {
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                double horiz = Math.sqrt((dx * dx) + (dz * dz));
                if (horiz > 3.0D && horiz < 16.0D && this.random.nextInt(40) == 0) {
                    wingFlap();
                    double climb = Math.max(0.0D, (target.getY() - this.getY()) * 0.06D);
                    this.setDeltaMovement((dx / horiz) * 0.55D, 0.5D + Math.min(climb, 0.35D),
                            (dz / horiz) * 0.55D);
                    this.hurtMarked = true; // force the velocity sync (26.2's replacement for hasImpulse)
                }
            } else if (this.random.nextInt(300) == 0) {
                wingFlap();
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.45D, 0.0D));
                this.hurtMarked = true;
            }
            return;
        }
        // Airborne: glide rather than drop, and lean toward prey so a swoop actually lands on it.
        Vec3 dm = this.getDeltaMovement();
        double vy = dm.y < 0.0D ? dm.y * 0.6D : dm.y;
        double vx = dm.x;
        double vz = dm.z;
        if (target != null && target.isAlive()) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double horiz = Math.sqrt((dx * dx) + (dz * dz));
            if (horiz > 0.5D) {
                vx += (dx / horiz) * 0.02D;
                vz += (dz / horiz) * 0.02D;
            }
        }
        this.setDeltaMovement(vx, vy, vz);
    }

    /**
     * Port of legacy {@code MoCTools.findMobRider} ({@code MoCTools:1716-1732}) as the manticore used it: a
     * riderless manticore looks in a 4x2x4 box around itself for the first skeleton or zombie that is not
     * already riding something, and seats it. (Legacy also accepted its own silver skeleton, which this port
     * does not have.) The mount attachment point comes from the entity type's {@code passengerAttachments}.
     */
    private void findMobRider(ServerLevel level) {
        for (Monster candidate : level.getEntitiesOfClass(Monster.class,
                this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D),
                m -> m != this && m.isAlive() && !m.isPassenger()
                        && (m instanceof AbstractSkeleton || m instanceof Zombie))) {
            candidate.startRiding(this);
            break; // legacy seats exactly one rider, then breaks out of the scan
        }
    }

    // -------------------------------------------------------------------------------------- combat

    /**
     * Legacy {@code applyEnchantments} ({@code MoCEntityManticore:330-359}), which 1.12 called after every
     * landed melee hit. Roughly one hit in five (and never while a sting is still running) delivers the
     * coat's venom; the rest merely bare the teeth.
     */
    @Override
    protected void applyHitEffects(LivingEntity target) {
        if (!this.poisoning && this.random.nextInt(5) == 0) {
            startSting();
            switch (getTypeMoC()) {
                case 3 -> // blue / frost: chills the victim stiff
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 0), this);
                case 1 -> {
                    // Red / nether: sets the victim alight — but legacy skipped this inside the Nether
                    // (`!world.provider.doesWaterVaporize()`), where a burn is no threat at all.
                    if (this.level().dimension() != Level.NETHER) {
                        target.igniteForSeconds(15.0F);
                    }
                }
                default -> // green (4) and dark (2): venom
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0), this);
            }
        } else {
            openJaw();
        }
    }

    // -------------------------------------------------------------------------------------- sounds
    // Legacy reused the lion cries for the manticore (its own manticore*.ogg cues are commented out in
    // 12.0.5) and gaped the maw on each one.

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        openJaw();
        return MoCSounds.LIONGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        openJaw();
        return MoCSounds.LIONHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        openJaw();
        return MoCSounds.LIONDEATH.get();
    }

    // --------------------------------------------------------------------------------------- drops

    /**
     * Legacy {@code dropFewItems} + {@code getDropItem} ({@code MoCEntityManticore:403-444}), which nest two
     * separate {@code rareItemDropChance} rolls:
     *
     * <ol>
     *   <li>The FIRST roll replaces the whole drop with a manticore egg. Legacy dropped
     *       {@code mocegg} metadata {@code getType() + 61}, which hatched a coat-matched
     *       {@link drzhark.mocreatures.entity.passive.MoCEntityManticorePet}. The port's egg entity decodes
     *       only the legacy composite ids 1-54 ({@code MoCEntityEgg.setEggType}) and has no 62-65 range, so
     *       an egg dropped with that id would silently hatch an OSTRICH. The faithful substitute — an item
     *       that yields a tameable baby manticore — is the manticore-pet spawn egg, which spawns exactly the
     *       cub the legacy egg hatched. The one thing lost is coat inheritance: the pet re-rolls its coat.</li>
     *   <li>Failing that, it drops 0-2 copies (vanilla {@code dropFewItems}) of ONE coat-keyed item, itself
     *       chosen by a second rare roll: the coat's scorpion sting, else the coat's chitin.</li>
     * </ol>
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        if (MoCConfig.get().destroyDrops) {
            return; // server-admin loot suppression, same gate MoCBehavior.dropLoot applies
        }
        if (this.random.nextInt(100) < RARE_ITEM_DROP_CHANCE) {
            SpawnEggItem.byId(MoCEntities.MANTICORE_PET.get()).ifPresent(
                    holder -> spawnAtLocation(level, new ItemStack(holder.value())));
            return; // legacy: the egg REPLACES the ordinary drop
        }
        boolean rare = this.random.nextInt(100) < RARE_ITEM_DROP_CHANCE;
        Item drop = switch (getTypeMoC()) {
            case 1 -> rare ? MoCItems.SCORPSTINGNETHER.get() : MoCItems.CHITINNETHER.get();
            case 2 -> rare ? MoCItems.SCORPSTINGCAVE.get() : MoCItems.CHITINBLACK.get();
            case 3 -> rare ? MoCItems.SCORPSTINGFROST.get() : MoCItems.CHITINFROST.get();
            case 4 -> rare ? MoCItems.SCORPSTINGDIRT.get() : MoCItems.CHITIN.get();
            default -> MoCItems.CHITIN.get();
        };
        int n = this.random.nextInt(3); // vanilla dropFewItems: 0-2 copies, zero included
        for (int i = 0; i < n; i++) {
            spawnAtLocation(level, new ItemStack(drop, 1));
        }
    }

    /**
     * The manticore always flies itself. Vanilla's {@link net.minecraft.world.entity.Mob} hands the reins to
     * any {@code Mob} passenger (that is how a vanilla spider jockey's skeleton steers its spider), which
     * would let the picked-up skeleton drive — but in 1.12 {@code EntityLiving.getControllingPassenger()}
     * returned null unless a mount opted in, so a legacy manticore carried its undead rider as cargo and
     * chose its own course. Returning null keeps that. Legacy {@code getIsRideable()} is likewise a hard
     * {@code false} here: only {@link drzhark.mocreatures.entity.passive.MoCEntityManticorePet} takes a
     * player. The rider's seat height comes from the entity type's {@code passengerAttachments}, replacing
     * legacy {@code updatePassenger}/{@code getMountedYOffset}.
     */
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        // One undead rider at a time (legacy findMobRider seated exactly one).
        return this.getPassengers().isEmpty();
    }
}
