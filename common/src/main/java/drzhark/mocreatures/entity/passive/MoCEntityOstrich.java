package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityOstrich}. A large ground bird with multiple variants
 * (chick, female, male, albino, and several transformed types).
 */
public class MoCEntityOstrich extends MoCAnimal {

    public MoCEntityOstrich(EntityType<? extends MoCEntityOstrich> type, Level level) {
        super(type, level);
        // Legacy constructor randomized the first egg timer to 1000-1999 (rand.nextInt(1000)+1000) on every
        // spawn/creation; the NBT does not persist it, so it re-randomizes within that range each load.
        this.eggCounter = this.random.nextInt(1000) + 1000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D); // adult males/demons kick for 3 (legacy attackEntity)
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int j = this.random.nextInt(100);
            if (j <= 20) {
                setTypeMoC(1);
                // A chick has to actually BE young, or it matures on its first tick and the chick skin is
                // never seen. Legacy tracked this with edad alone (ctor setEdad(35)); the port keys growth
                // and render scale off the adult flag, so a chick is flagged non-adult at the same age.
                setAdult(false);
                setMoCAge(35);
            } else if (j <= 65) {
                setTypeMoC(2);
            } else if (j <= 95) {
                setTypeMoC(3);
            } else {
                setTypeMoC(4);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 1 -> modelTexture("ostrichc.png");
            case 2 -> modelTexture("ostrichb.png");
            case 4 -> modelTexture("ostrichd.png");
            case 5 -> modelTexture("ostriche.png");
            case 6 -> modelTexture("ostrichf.png");
            case 7 -> modelTexture("ostrichg.png");
            case 8 -> modelTexture("ostrichh.png");
            default -> modelTexture("ostricha.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        if (getTypeMoC() == 1) {
            return MoCSounds.OSTRICHCHICK.get();
        }
        return MoCSounds.OSTRICHGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.OSTRICHHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.OSTRICHDYING.get();
    }

    // --------------------------------------------------------------- whip-triggered sprint (legacy sprintCounter)
    // A ridden ostrich cruises at a per-type pace (types 3/4/5 faster). Cracking the whip on a ridden ostrich
    // kicks off a FIXED sprint cycle (legacy sprintCounter): ~200 ticks of a ~50% speed burst, then ~100 ticks
    // "out of breath" at HALF speed, before it resets and can be whipped again. It is NOT an input-held sprint.
    // Type 5 is fire-immune. SPRINT_PHASE mirrors the cycle to the client so ridden speed matches on both sides:
    // 0 = normal, 1 = boost (x1.5), 2 = exhausted (x0.5).
    private static final EntityDataAccessor<Integer> SPRINT_PHASE =
            SynchedEntityData.defineId(MoCEntityOstrich.class, EntityDataSerializers.INT);
    /** Legacy {@code sprintCounter}: 0 = idle; 1-199 boost; 200 neutral; 201-300 exhausted; resets to 0 past 300. */
    private int sprintCounter;
    /** Head-in-sand hiding toggle (legacy {@code getHiding()}): a hidden tamed ostrich crouches and won't move.
     *  Synched so the client can render the head-in-sand pose. */
    private static final EntityDataAccessor<Boolean> HIDING =
            SynchedEntityData.defineId(MoCEntityOstrich.class, EntityDataSerializers.BOOLEAN);
    /** Legacy {@code hidingCounter}: ticks a WILD ostrich has kept its head hidden; it auto-unhides past ~500. */
    private int hidingCounter;

    // Legacy equipment / egg-guard state, synched so the client can render helmet/flag/chest and pathing works.
    private static final EntityDataAccessor<Integer> HELMET =
            SynchedEntityData.defineId(MoCEntityOstrich.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLAG_COLOR =
            SynchedEntityData.defineId(MoCEntityOstrich.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_CHESTED =
            SynchedEntityData.defineId(MoCEntityOstrich.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> EGG_WATCHING =
            SynchedEntityData.defineId(MoCEntityOstrich.class, EntityDataSerializers.BOOLEAN);
    /** 9-slot storage granted by a chest (legacy {@code MoCAnimalChest("OstrichChest", 9)}). */
    private final SimpleContainer chest = new SimpleContainer(9);
    /** Ticks until the next egg is laid (legacy {@code eggCounter}); randomized 1000-1999 in the constructor. */
    private int eggCounter;

    /** Helmet tier worn (0 none; 1 leather,2 iron,3 gold,4 diamond,5 hide,6 fur,7 croc,9-12 scorpion). */
    public int getHelmet() {
        return this.entityData.get(HELMET);
    }

    public void setHelmet(int h) {
        this.entityData.set(HELMET, h);
    }

    /** Flag colour (0 none; 1-16 = {@link DyeColor} ordinal + 1). */
    public int getFlagColor() {
        return this.entityData.get(FLAG_COLOR);
    }

    public void setFlagColor(int c) {
        this.entityData.set(FLAG_COLOR, c);
    }

    public boolean getIsChested() {
        return this.entityData.get(IS_CHESTED);
    }

    public void setIsChested(boolean b) {
        this.entityData.set(IS_CHESTED, b);
    }

    public boolean getEggWatching() {
        return this.entityData.get(EGG_WATCHING);
    }

    public void setEggWatching(boolean b) {
        this.entityData.set(EGG_WATCHING, b);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPRINT_PHASE, 0);
        builder.define(HELMET, 0);
        builder.define(FLAG_COLOR, 0);
        builder.define(IS_CHESTED, false);
        builder.define(EGG_WATCHING, false);
        builder.define(HIDING, false);
    }

    @Override
    public void tick() {
        super.tick();
        // Types 5 (fire) and 6 (black-wyvern) are FLYERS (legacy isFlyer()). Once airborne they glide:
        // their descent is slowed sharply (legacy fall()/myFallSpeed(0.9) kept them aloft and immune to
        // fall damage), so a wing-flap jump gets and keeps them in the air. Runs on both sides so the
        // client sees the same glide; only kicks in while airborne and actually descending.
        if (isFlyer() && !this.onGround() && this.getDeltaMovement().y < 0.0D) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
            this.fallDistance = 0.0D; // never accrue fall damage while gliding
        }
        // Legacy onUpdate froze a hidden ostrich's facing; do it on both sides so the pose renders locked.
        if (getHiding() && !this.isVehicle()) {
            float lockedYaw = this.yRotO;
            this.setYRot(lockedYaw);
            this.yBodyRot = lockedYaw;
            this.yBodyRotO = lockedYaw;
        }
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        // Per-type max health (legacy getMaxHealth switch): chick (type 1) = 10, female (type 2) = 15,
        // every adult male / transformed type = 20. The attribute base defaults to 20 (createAttributes),
        // so we only rewrite the base when the current type demands a different value, then top up a chick
        // that was created at 20 so it isn't left over-healed. Cheap: the common case is a no-op compare.
        double wantMaxHealth = switch (getTypeMoC()) {
            case 1 -> 10.0D; // chick
            case 2 -> 15.0D; // female
            default -> 20.0D; // adult males, albino, and all transformed types
        };
        AttributeInstance maxHealthAttr = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null && maxHealthAttr.getBaseValue() != wantMaxHealth) {
            maxHealthAttr.setBaseValue(wantMaxHealth);
            if (getHealth() > wantMaxHealth) {
                setHealth((float) wantMaxHealth);
            }
        }
        // Chick maturation (legacy): a chick (type 1) that reaches full age re-rolls into a proper adult
        // variant. Without this a type-1 ostrich grows to full size but keeps the gear-less chick skin
        // (ostrichc) and can never be saddled/chested/helmeted (equip is gated on type > 1). Females are
        // the most common adult, then males, then the rare albino — matching the natural spawn spread.
        if (getTypeMoC() == 1 && getMoCAge() >= 100) {
            int r = level.getRandom().nextInt(100);
            setTypeMoC(r < 55 ? 2 : (r < 90 ? 3 : 4));
        }
        // Whip-triggered sprint cycle (legacy sprintCounter): once the whip sets sprintCounter = 1 on a ridden
        // ostrich it advances every tick and resets to 0 past 300, giving a fixed burst-then-recover cycle
        // rather than an input-held sprint. SPRINT_PHASE is synched so getCustomSpeed matches on the client.
        if (this.sprintCounter > 0 && ++this.sprintCounter > 300) {
            this.sprintCounter = 0;
        }
        int sprintPhase = (this.sprintCounter > 0 && this.sprintCounter < 200) ? 1
                : (this.sprintCounter > 200 ? 2 : 0);
        if (this.entityData.get(SPRINT_PHASE) != sprintPhase) {
            this.entityData.set(SPRINT_PHASE, sprintPhase);
        }

        // Head-in-sand hiding (legacy getHiding()/isMovementCeased): a hidden ostrich stays frozen. Mounting it
        // clears hiding; a WILD ostrich auto-unhides after ~500 ticks, a tamed one stays hidden until re-whipped.
        if (getHiding()) {
            if (this.isVehicle()) {
                setHiding(false);
            } else {
                this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
                this.getNavigation().stop();
                if (!getIsTamed() && ++this.hidingCounter > 500) {
                    setHiding(false);
                    this.hidingCounter = 0;
                }
            }
        } else {
            this.hidingCounter = 0;
        }

        // Tamed ostriches slowly regenerate (legacy ~1/300t).
        if (getIsTamed() && getHealth() < getMaxHealth() && level.getRandom().nextInt(300) == 0) {
            heal(1.0F);
        }

        // Unicorn ostrich (type 8) charge: while its whip-sprint is in the first ~150 ticks and it is ridden it
        // buckles nearby mobs (legacy MoCTools.buckleMobs -> 2 damage + a fixed "bigsmack" knockback), hitting
        // every living thing in range (including other ostriches) except its own rider.
        if (getTypeMoC() == 8 && this.isVehicle() && this.sprintCounter > 0 && this.sprintCounter < 150) {
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(2.0D),
                    e -> e != this && !this.hasPassenger(e))) {
                victim.hurtServer(level, this.damageSources().mobAttack(this), 2.0F);
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

        // Egg-laying + guarding (legacy): an adult female (type 2) not already guarding lays an egg when its
        // timer elapses and the world isn't crowded, pairing with the nearest male to guard it.
        if (getTypeMoC() == 2 && !getEggWatching() && --this.eggCounter <= 0) {
            // Overpopulation guard counts ALL loaded ostriches/eggs in this dimension (legacy
            // worldObj.countEntities), not just those within a local radius, so a spread-out flock is
            // still capped at 20 ostriches / 10 eggs world-wide.
            int ostrichCount = 0;
            int eggCount = 0;
            for (Entity e : level.getAllEntities()) {
                if (e instanceof MoCEntityOstrich) {
                    ostrichCount++;
                } else if (e instanceof MoCEntityEgg) {
                    eggCount++;
                }
            }
            if (ostrichCount < 20 && eggCount < 10) {
                MoCEntityOstrich male = getClosestMaleOstrich(8.0D);
                // Legacy gate: a tamed female lays ONLY with a male ostrich (type >= 3) within 8 blocks;
                // a wild female lays regardless of a male.
                if (!getIsTamed() || male != null) {
                    MoCEntityEgg egg = new MoCEntityEgg(MoCEntities.EGG.get(), level);
                    egg.setTypeMoC(MoCEntityEgg.TYPE_OSTRICH);
                    egg.setPos(this.getX(), this.getY(), this.getZ());
                    level.addFreshEntity(egg);
                    setEggWatching(true);
                    if (male != null) {
                        male.setEggWatching(true);
                    }
                    playChickenPlop();
                }
            }
            this.eggCounter = level.getRandom().nextInt(1000) + 1000;
        }

        // Egg protection: a guarding ostrich paths to its nearest egg; if the egg is gone it stops guarding
        // and (if wild, non-peaceful) rounds on the nearest player as the presumed egg-thief.
        if (getEggWatching()) {
            MoCEntityEgg nearestEgg = null;
            double best = Double.MAX_VALUE;
            for (MoCEntityEgg e : level.getEntitiesOfClass(MoCEntityEgg.class, this.getBoundingBox().inflate(8.0D))) {
                double d = e.distanceToSqr(this);
                if (d < best) {
                    best = d;
                    nearestEgg = e;
                }
            }
            if (nearestEgg != null) {
                if (best > 4.0D) {
                    this.getNavigation().moveTo(nearestEgg, 1.0D);
                }
            } else {
                setEggWatching(false);
                if (!getIsTamed() && level.getDifficulty() != Difficulty.PEACEFUL) {
                    Player thief = level.getNearestPlayer(this, 10.0D);
                    if (thief != null) {
                        setTarget(thief);
                    }
                }
            }
        }
    }

    /** Nearest adult male/demon ostrich (type &ge; 3) within {@code range}, for egg-guard pairing. */
    @Nullable
    private MoCEntityOstrich getClosestMaleOstrich(double range) {
        MoCEntityOstrich closest = null;
        double best = Double.MAX_VALUE;
        for (MoCEntityOstrich o : this.level().getEntitiesOfClass(MoCEntityOstrich.class,
                this.getBoundingBox().inflate(range), o -> o != this && o.getTypeMoC() >= 3)) {
            double d = o.distanceToSqr(this);
            if (d < best) {
                best = d;
                closest = o;
            }
        }
        return closest;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Legacy behaviour split by type: only males/albino/demon and the transformed types (type > 2) fight
        // back when hit; chicks (type 1) and unprovoked females (type 2) are "scared" and flee instead. A
        // female guarding a stolen egg (i.e. one that already has a target) counts as not-scared and attacks.
        // The base MoCAnimal installs an UNGATED PanicGoal; swap it for one that only fires for scared types so
        // an aggressive ostrich retaliates rather than fleeing.
        this.goalSelector.removeAllGoals(g -> g instanceof PanicGoal);
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5D) {
            @Override
            public boolean canUse() {
                return !isNotScared() && super.canUse();
            }

            @Override
            public void stop() {
                // Legacy onLivingUpdate (lines 543-546): a scared ostrich that has been fleeing (fleeingTick
                // winding down) stops and buries its head in the sand — the signature "scared ostriches run
                // and hide" behaviour. Legacy hid it regardless of tamed status; a wild one auto-unhides after
                // ~500t (tick()), while a tamed one stays hidden until re-whipped.
                super.stop();
                MoCEntityOstrich.this.setHiding(true);
            }
        });
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true) {
            @Override
            public boolean canUse() {
                return isNotScared() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isNotScared() && super.canContinueToUse();
            }
        });
        // Only an aggressive adult (type > 2) acquires its attacker as a target, and only off Peaceful
        // (legacy attackEntityFrom gated on getType() > 2 && difficultySetting > 0).
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                // Legacy attackEntityFrom returned WITHOUT setting a target when the attacker was the rider,
                // or when a tamed ostrich was struck by ANY player — so a tamed ostrich never rounds on its
                // owner and a mount never turns on its rider, even the aggressive types (male/albino/demon/
                // transformed). Mirror the BigCat guard so the owner can hit it without being kicked back.
                LivingEntity attacker = MoCEntityOstrich.this.getLastHurtByMob();
                if (attacker != null
                        && ((MoCEntityOstrich.this.getIsTamed() && attacker instanceof Player)
                                || attacker == MoCEntityOstrich.this.getControllingPassenger())) {
                    return false;
                }
                return getTypeMoC() > 2
                        && MoCEntityOstrich.this.level().getDifficulty() != Difficulty.PEACEFUL
                        && super.canUse();
            }
        });
    }

    /**
     * Legacy {@code isNotScared()}: an ostrich stands and fights only if it is an aggressive adult (type &gt; 2)
     * or a female (type 2) that already has a target (i.e. guarding a stolen egg). Chicks and unprovoked
     * females are scared and flee.
     */
    public boolean isNotScared() {
        return (getTypeMoC() == 2 && getTarget() != null) || getTypeMoC() > 2;
    }

    /**
     * Per-type base speed (legacy getCustomSpeed). The base is NOT pre-multiplied; the whip-triggered sprint
     * cycle layers a x1.5 burst (phase 1) or a x0.5 "out of breath" penalty (phase 2) on top.
     */
    private double getCustomSpeed() {
        double speed = switch (getTypeMoC()) {
            case 3 -> 1.1D;
            case 4 -> 1.3D;
            case 5 -> 1.4D;
            default -> 0.8D; // types 1, 2 and chicks
        };
        int phase = this.entityData.get(SPRINT_PHASE);
        if (phase == 1) {
            speed *= 1.5D; // sprint burst
        } else if (phase == 2) {
            speed *= 0.5D; // out of breath — a real slowdown
        }
        return speed;
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        // Fast runner: ~0.2 * customSpeed reads as a horse-plus gallop, quicker for the higher types.
        return (float) (0.2D * getCustomSpeed());
    }

    @Override
    public boolean fireImmune() {
        return getTypeMoC() == 5 || super.fireImmune(); // type 5 = fire ostrich
    }

    // --------------------------------------------------------------- flight (legacy isFlyer / fall / jump)
    /** Legacy {@code isFlyer()}: types 5 (fire) and 6 (black-wyvern) ostriches can take to the air. */
    public boolean isFlyer() {
        return getTypeMoC() == 5 || getTypeMoC() == 6;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        // A flyer never takes fall damage (legacy fall() returned early for isFlyer()).
        return !isFlyer() && super.causeFallDamage(fallDistance, multiplier, source);
    }

    @Override
    protected double getCustomJump() {
        // A flyer's Jump-key press is a powerful wing-flap that lifts it off the ground and, combined with
        // the glide in tick(), lets it stay airborne; other ostriches keep the normal mount hop.
        return isFlyer() ? 0.9D : super.getCustomJump();
    }

    @Override
    public void makeEntityJump() {
        // Base makeEntityJump only lifts a GROUNDED mount. A flyer (type 5/6) needs to re-flap in mid-air
        // to gain/keep altitude (legacy makeEntityJump ignored onGround for flyers), so apply the wing-flap
        // impulse directly when it's already airborne; the grounded takeoff still uses the base queued path.
        if (isFlyer() && !this.onGround()) {
            this.setDeltaMovement(getDeltaMovement().x, getCustomJump(), getDeltaMovement().z);
            this.hurtMarked = true; // sync the impulse to clients (26.2 idiom, matches MoCAnimal.aiStep)
            return;
        }
        super.makeEntityJump();
    }

    // --------------------------------------------------------------- whip effects (legacy MoCItemWhip)
    /** Legacy {@code getHiding()}: whether this ostrich has its head in the sand. */
    public boolean getHiding() {
        return this.entityData.get(HIDING);
    }

    /** Legacy {@code setHiding()}: toggle/set head-in-sand hiding (only meaningful while grounded and unridden). */
    public void setHiding(boolean h) {
        this.entityData.set(HIDING, h);
    }

    /**
     * Whip crack on a ridden ostrich: kick off a fresh sprint cycle (legacy {@code MoCItemWhip}: a ridden
     * ostrich with {@code sprintCounter == 0} gets {@code sprintCounter = 1}). {@link #tick()} then runs the
     * fixed burst-then-recover cycle; a crack mid-cycle is ignored until the counter resets to 0.
     */
    public void whipSprint() {
        if (this.isVehicle() && this.sprintCounter == 0) {
            this.sprintCounter = 1;
        }
    }

    /** Whip crack on a tamed, unridden ostrich: toggle its head-in-sand hiding (legacy behaviour). */
    public void whipToggleHiding() {
        setHiding(!getHiding());
    }

    // ------------------------------------------------------------- essence-vial transforms (legacy interact)
    // Feeding a tamed, non-chick ostrich one of the four essence vials transforms it into a special variant
    // (or, if it is already that variant, fully heals it), consuming the vial for a glass bottle. Faithful to
    // the legacy vial branches: vialdarkness->6, vialundead->7, viallight->8, vialnightmare(=essencefire)->5.
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (getIsTamed() && getTypeMoC() > 1) {
            // Sneak + empty hand strips gear one piece at a time: helmet first, then flag, then the chest
            // (dropping its contents + the chest item). This is the removal path the legacy mod lacked a
            // clear hook for; a plain (non-sneaking) empty hand still mounts a saddled ostrich as before.
            if (player.isShiftKeyDown() && stack.isEmpty()) {
                if (getHelmet() != 0) {
                    if (!this.level().isClientSide()) {
                        dropArmor(); // drops the worn helmet item and clears it (guards ServerLevel itself)
                        playChickenPlop();
                    }
                    return InteractionResult.SUCCESS;
                }
                if (getFlagColor() != 0) {
                    if (!this.level().isClientSide()) {
                        dropFlag();
                        playChickenPlop();
                    }
                    return InteractionResult.SUCCESS;
                }
                if (getIsChested()) {
                    if (this.level() instanceof ServerLevel sl) {
                        for (int i = 0; i < chest.getContainerSize(); i++) {
                            ItemStack s = chest.getItem(i);
                            if (!s.isEmpty()) {
                                spawnAtLocation(sl, s);
                            }
                        }
                        chest.clearContent();
                        spawnAtLocation(sl, new ItemStack(Items.CHEST));
                        setIsChested(false);
                        setFlagColor(0);
                        playChickenPlop();
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            int target = 0;
            if (stack.is(MoCItems.ESSENCEDARKNESS.get())) {
                target = 6;
            } else if (stack.is(MoCItems.ESSENCEUNDEAD.get())) {
                target = 7;
            } else if (stack.is(MoCItems.ESSENCELIGHT.get())) {
                target = 8;
            } else if (stack.is(MoCItems.ESSENCEFIRE.get())) {
                target = 5;
            }
            if (target != 0) {
                if (!this.level().isClientSide()) {
                    if (getTypeMoC() == target) {
                        setHealth(getMaxHealth());
                    } else {
                        setTypeMoC(target);
                    }
                    stack.shrink(1);
                    ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, bottle);
                    } else {
                        player.addItem(bottle);
                    }
                    this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_DRINK.value(),
                            SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }

            // Saddle: the mod horse-saddle makes a non-chick ostrich rideable (vanilla saddle via super).
            if (!isSaddled() && stack.is(MoCItems.HORSESADDLE.get())) {
                if (!this.level().isClientSide()) {
                    setSaddled(true);
                    consumeOne(player, stack);
                    playChickenPlop();
                }
                return InteractionResult.SUCCESS;
            }
            // Chest: fit a vanilla chest for 9-slot storage, handing back a key to open it.
            if (!getIsChested() && stack.is(Items.CHEST)) {
                if (!this.level().isClientSide()) {
                    setIsChested(true);
                    consumeOne(player, stack);
                    player.addItem(new ItemStack(MoCItems.KEY.get()));
                    playChickenPlop();
                }
                return InteractionResult.SUCCESS;
            }
            // Key opens the ostrich's 9-slot chest (vanilla one-row chest screen).
            if (getIsChested() && stack.is(MoCItems.KEY.get())) {
                if (!this.level().isClientSide() && player instanceof ServerPlayer sp) {
                    sp.openMenu(new SimpleMenuProvider(
                            (id, inv, p) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1,
                                    id, inv, this.chest, 1), getDisplayName()));
                }
                return InteractionResult.SUCCESS;
            }
            // Flag: colour a flag by applying wool (requires a fitted chest first, as in legacy).
            if (getIsChested()) {
                int flagColor = woolColor(stack);
                if (flagColor != 0) {
                    if (!this.level().isClientSide()) {
                        dropFlag();
                        setFlagColor(flagColor);
                        consumeOne(player, stack);
                        playChickenPlop();
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            // Helmet armour: fit a helmet (vanilla leather/iron/gold/diamond or mod hide/fur/croc/scorpion),
            // dropping any previous helmet.
            int ht = helmetTier(stack);
            if (ht != 0) {
                if (!this.level().isClientSide()) {
                    dropArmor();
                    setHelmet(ht);
                    consumeOne(player, stack);
                    playChickenPlop();
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    // ---------------------------------------------------------- helmet armour damage reduction (legacy)
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // A worn helmet subtracts a flat amount from incoming damage (min 1), matching legacy attackEntityFrom.
        if (getIsTamed() && getHelmet() != 0 && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            int j = switch (getHelmet()) {
                case 1 -> 1;
                case 2, 5, 6 -> 2;
                case 3, 7 -> 3;
                case 4, 9, 10, 11, 12 -> 4;
                default -> 0;
            };
            amount = Math.max(1.0F, amount - j);
        }
        return super.hurtServer(level, source, amount);
    }

    public SimpleContainer getChest() {
        return this.chest;
    }

    /** The item form of a worn helmet tier, for dropping when replaced or on death. */
    @Nullable
    private static Item helmetItem(int tier) {
        return switch (tier) {
            case 1 -> Items.LEATHER_HELMET;
            case 2 -> Items.IRON_HELMET;
            case 3 -> Items.GOLDEN_HELMET;
            case 4 -> Items.DIAMOND_HELMET;
            case 5 -> MoCItems.HIDEHELMET.get();
            case 6 -> MoCItems.FURHELMET.get();
            case 7 -> MoCItems.REPTILEHELMET.get();
            case 9 -> MoCItems.SCORPHELMETDIRT.get();
            case 10 -> MoCItems.SCORPHELMETFROST.get();
            case 11 -> MoCItems.SCORPHELMETCAVE.get();
            case 12 -> MoCItems.SCORPHELMETNETHER.get();
            default -> null;
        };
    }

    /** Drops the currently-worn helmet (if any) and clears it (legacy {@code dropArmor}). */
    private void dropArmor() {
        int tier = getHelmet();
        if (tier != 0 && this.level() instanceof ServerLevel level) {
            Item it = helmetItem(tier);
            if (it != null) {
                spawnAtLocation(level, new ItemStack(it));
            }
        }
        setHelmet(0);
    }

    /**
     * Legacy {@code dropMyStuff}: release the ostrich's worn gear — its helmet and its raised flag (wool) — as
     * items when the Scroll of Freedom frees it. The saddle and saddlebag chest are released separately by the
     * scroll's generic handling.
     */
    public void dropWornGear(ServerLevel level) {
        boolean any = false;
        if (getHelmet() != 0) {
            Item it = helmetItem(getHelmet());
            if (it != null) {
                spawnAtLocation(level, new ItemStack(it));
            }
            setHelmet(0);
            any = true;
        }
        if (getFlagColor() != 0) {
            Item wool = flagWool(getFlagColor());
            if (wool != null) {
                spawnAtLocation(level, new ItemStack(wool));
            }
            setFlagColor(0);
            any = true;
        }
        if (any) {
            level.playSound(null, blockPosition(), MoCSounds.ARMOROFF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    /** The wool item matching a stored flag colour (1-16), or {@code null} for no flag. */
    @Nullable
    private static Item flagWool(int color) {
        if (color < 1 || color > 16) {
            return null;
        }
        // Legacy stores flagColor = wool metadata, with white (meta 0) remapped to 16. Reverse that here.
        DyeColor dye = DyeColor.byId(color == 16 ? 0 : color);
        Item wool = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.withDefaultNamespace(dye.getName() + "_wool"));
        return wool == Items.AIR ? null : wool;
    }

    private void consumeOne(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private void playChickenPlop() {
        this.level().playSound(null, this.blockPosition(), SoundEvents.CHICKEN_EGG,
                SoundSource.NEUTRAL, 1.0F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F);
    }

    /** The flag colour (1-16) for a wool ItemStack, or 0 if the stack is not wool. */
    private static int woolColor(ItemStack stack) {
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !id.getPath().endsWith("_wool")) {
            return 0;
        }
        String name = id.getPath().substring(0, id.getPath().length() - "_wool".length());
        DyeColor dye = DyeColor.byName(name, null);
        if (dye == null) {
            return 0;
        }
        // Legacy: flagColor = wool metadata (== DyeColor id), except white (0) which maps to 16 so 0 can
        // still mean "no flag". The model's per-index flag textures are painted to match this ordering.
        return dye.getId() == 0 ? 16 : dye.getId();
    }

    /** The helmet tier for a helmet ItemStack (0 if not a supported helmet). */
    private static int helmetTier(ItemStack stack) {
        if (stack.is(Items.LEATHER_HELMET)) {
            return 1;
        }
        if (stack.is(Items.IRON_HELMET)) {
            return 2;
        }
        if (stack.is(Items.GOLDEN_HELMET)) {
            return 3;
        }
        if (stack.is(Items.DIAMOND_HELMET)) {
            return 4;
        }
        if (stack.is(MoCItems.HIDEHELMET.get())) {
            return 5;
        }
        if (stack.is(MoCItems.FURHELMET.get())) {
            return 6;
        }
        if (stack.is(MoCItems.REPTILEHELMET.get())) {
            return 7;
        }
        if (stack.is(MoCItems.SCORPHELMETDIRT.get())) {
            return 9;
        }
        if (stack.is(MoCItems.SCORPHELMETFROST.get())) {
            return 10;
        }
        if (stack.is(MoCItems.SCORPHELMETCAVE.get())) {
            return 11;
        }
        if (stack.is(MoCItems.SCORPHELMETNETHER.get())) {
            return 12;
        }
        return 0;
    }

    /** Drops the currently-worn flag (if any) and clears it (legacy {@code dropFlag}). */
    private void dropFlag() {
        Item wool = flagWool(getFlagColor());
        if (wool != null && this.level() instanceof ServerLevel level) {
            spawnAtLocation(level, new ItemStack(wool));
        }
        setFlagColor(0);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        Item helm = helmetItem(getHelmet());
        if (helm != null) {
            spawnAtLocation(level, new ItemStack(helm));
        }
        Item flag = flagWool(getFlagColor());
        if (flag != null) {
            spawnAtLocation(level, new ItemStack(flag));
        }
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (!s.isEmpty()) {
                spawnAtLocation(level, s);
            }
        }
        chest.clearContent();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Helmet", getHelmet());
        output.putInt("FlagColor", getFlagColor());
        output.putBoolean("Chested", getIsChested());
        output.putBoolean("Hiding", getHiding());
        ValueOutput.ValueOutputList items = output.childrenList("ChestItems");
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
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
        setHelmet(input.getIntOr("Helmet", 0));
        setFlagColor(input.getIntOr("FlagColor", 0));
        setIsChested(input.getBooleanOr("Chested", false));
        setHiding(input.getBooleanOr("Hiding", false));
        chest.clearContent();
        for (ValueInput child : input.childrenListOrEmpty("ChestItems")) {
            int slot = child.getIntOr("Slot", -1);
            if (slot >= 0 && slot < chest.getContainerSize()) {
                child.read("Item", ItemStack.CODEC).ifPresent(s -> chest.setItem(slot, s));
            }
        }
    }
}
