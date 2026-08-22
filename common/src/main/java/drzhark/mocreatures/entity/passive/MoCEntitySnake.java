package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntitySnake}. A snake with eight colour/species variants.
 */
public class MoCEntitySnake extends MoCAnimal {

    /**
     * "Pissed" flag: a wild venomous snake only bites once it has been provoked — either by hissing a
     * warning at a lingering nearby player (legacy {@code onUpdate} hiss-then-random-pissed) or by being
     * struck (legacy {@code attackEntityFrom}). Small/shy snakes (type &lt; 3) and tamed snakes never enter
     * this state against players. Synched so the (future) hiss/bite animation can read it client-side.
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> PISSED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    MoCEntitySnake.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    /**
     * Synched climbing flag: legacy {@code isClimbing() = isOnLadder() && motionY > 0.01F} (legacy
     * {@code MoCEntitySnake}:200-202), which the model reads to arc the snake's front half up the wall.
     * It cannot be derived client-side: a remote mob is not {@code isLocalInstanceAuthoritative()}, so
     * {@code travel()}/{@code move()} never run for it (mc262-ref {@code LivingEntity}:3110-3112) and
     * {@code horizontalCollision} — the port's {@link #onClimbable()} — is never set on the client
     * (mc262-ref {@code Entity}:762). Hence a synched bit, set once per AI step.
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> CLIMBING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    MoCEntitySnake.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /**
     * Entity-event id that replays a bite on every client tracking this snake. It is the modern stand-in for
     * the legacy {@code MoCMessageHandler.sendToAllAround(new MoCMessageAnimation(getEntityId(), 0))} that
     * legacy {@code setBiting(true)} broadcast from the server (legacy {@code MoCEntitySnake}:472-478) and
     * that the receiving client turned back into {@code performAnimation(0)} — i.e. {@code setBiting(true)}
     * on its own copy of the snake (legacy {@code MoCMessageHandler}:81-89). Vanilla's highest allocated id
     * is {@code EntityEvent.TNT_PRIME = 70} (mc262-ref {@code EntityEvent}:65), so 73 is free; unknown ids
     * still fall through to {@code super.handleEntityEvent}.
     */
    private static final byte EVENT_BITE = 73;

    /** Server-side warning timer: counts up while a wild snake hisses at a near player before it may bite. */
    private int hissCounter;

    /**
     * Purely cosmetic animation timers, ticked client-side only and never synched — exactly as the legacy
     * snake kept them (legacy {@code onUpdate}:267-342 runs its whole timer block inside
     * {@code if (world.isRemote)}). They drive the model's tongue flick, jaw gape, rattle and the amplitude
     * of the secondary body wave; nothing on the server ever reads them, so there is nothing to sync.
     */
    private float fTongue;
    private float fMouth;
    private float fRattle;
    /** Legacy {@code movInt} (0-9, legacy field :58): the secondary body wave's amplitude, re-rolled ~1/50 ticks. */
    private int movInt;
    /** Client mirror of legacy {@code isNearPlayer} (set in legacy {@code onLivingUpdate}:424-447). */
    private boolean nearPlayerClient;
    /**
     * Client mirror of legacy {@code isBiting} (legacy field :53): true for the handful of ticks a bite
     * animation is playing. Started by {@link #EVENT_BITE} and cleared by the {@link #bodySwing} ramp below,
     * exactly as legacy's client did — legacy started it from the animation packet and ran the ramp that
     * ends it inside {@code onUpdate}'s {@code if (world.isRemote)} block (legacy :327-340).
     *
     * <p>Deliberately client-only here. Legacy also set it on the server and never cleared it there (the
     * ramp that calls {@code setBiting(false)} is client-side), which permanently latched the server's
     * {@code getNearPlayer()} true and made legacy :357 hiss forever after a snake's first bite. This port's
     * server side computes its own {@code crowded} test instead and never reads {@code getNearPlayer()}, so
     * keeping the flag off the server drops that legacy bug without changing anything visible.</p>
     */
    private boolean biting;
    /**
     * Legacy {@code bodyswing} (legacy field :60, initialised to {@code 2F} in the legacy constructor :67):
     * how far the reared front third is thrown forward, which the model multiplies into its z offset
     * (legacy {@code MoCModelSnake}:154 + :205). It sits at 2.0 at rest, drops 0.5 a tick through a bite and
     * resets to 2.5 — legacy's own reset value, not the 2.0 it starts at — when the bite ends (legacy
     * :331-339). Client-side only, for the reason given on {@link #biting}.
     */
    private float bodySwing = 2.0F;

    public MoCEntitySnake(EntityType<? extends MoCEntitySnake> type, Level level) {
        super(type, level);
        // legacy constructor :68 — every snake starts with its own secondary-wave amplitude
        this.movInt = this.random.nextInt(10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PISSED, false);
        builder.define(CLIMBING, false);
    }

    public boolean isPissed() {
        return this.entityData.get(PISSED);
    }

    public void setPissed(boolean pissed) {
        this.entityData.set(PISSED, pissed);
    }

    /**
     * Snakes that will never target or bite a player: the two small shy variants (type &lt; 3) and any tamed
     * snake. Mirrors the legacy {@code (getType() < 3 || getIsTamed())} guard in {@code attackEntity} plus the
     * {@code isNotScared() = getType() > 2 && getEdad() > 50} test used by {@code findPlayerToAttack}.
     */
    private boolean isHarmlessToPlayers() {
        return getTypeMoC() < 3 || getIsTamed();
    }

    /**
     * Retaliation gate for the {@code HurtByTargetGoal} (legacy {@code attackEntityFrom} lines 716-731 combined
     * with {@code attackEntity} line 645). {@code attackEntityFrom} returned immediately for {@code getType() < 3},
     * so a shy snake never turns on its attacker. For any venomous snake (type &gt;= 3) being struck set
     * {@code entityToAttack = attacker} — the {@code getIsTamed()} check was deliberately commented out — but
     * {@code attackEntity} then cleared that target only when the attacker was an {@code EntityPlayer} AND the
     * snake was shy or tamed. Net effect: a WILD venomous snake bites back any attacker; a TAMED venomous snake
     * bites back a non-player mob but not a player.
     */
    private boolean canRetaliate() {
        if (getTypeMoC() < 3) {
            return false; // shy snakes never retaliate (legacy attackEntityFrom early return for type < 3)
        }
        if (!getIsTamed()) {
            return true; // wild venomous snake turns on whatever struck it, player or mob
        }
        // Tamed venomous snake: bite back a mob, but not a player (legacy attackEntity cleared the player target).
        return !(getLastHurtByMob() instanceof net.minecraft.world.entity.player.Player);
    }

    /**
     * Legacy gated every snake player-aggression path on {@code worldObj.difficultySetting > 0}: on Peaceful
     * (difficultySetting 0) a snake never hisses, gets pissed, retaliates, or targets/poisons a player
     * ({@code onUpdate} line 421, {@code attackEntityFrom} line 726, {@code findPlayerToAttack} line 742 all
     * require {@code difficultySetting > 0}). On Peaceful a legacy snake is completely harmless to players.
     */
    private boolean canBeAggressive() {
        return this.level().getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL;
    }

    /**
     * Legacy shy-snake flee gate ({@code onLivingUpdate} else-branch, line 624): a snake that is
     * {@code !isNotScared()} — a small shy variant (type &lt; 3) or any not-yet-grown snake (legacy
     * {@code edad <= 50}, i.e. not an adult here) — and is untamed runs away when a player gets close.
     * ({@code isNotScared() == getType() > 2 && edad > 50}.)
     */
    private boolean isShyFleer() {
        return !getIsTamed() && (getTypeMoC() < 3 || !getIsAdult());
    }

    /**
     * Rework the base hostile goals (installed by {@link MoCAnimal#registerGoals()} because the snake behaviour
     * is {@code hostile}) to match the legacy snake:
     * <ul>
     *   <li>the player hunt fires only for a wild, adult, provoked ({@link #isPissed()}), non-shy snake;</li>
     *   <li>the retaliation ({@code HurtByTargetGoal}) fires for any venomous snake (type &gt;= 3) via
     *       {@link #canRetaliate()} — a wild one bites back any attacker, a tamed one bites back a non-player mob
     *       but never a player — while a struck shy snake (type &lt; 3) never turns on its attacker; legacy
     *       {@code attackEntityFrom} set no retaliation target for {@code getType() < 3} and {@code attackEntity}
     *       cleared only a player target when the snake was shy or tamed;</li>
     *   <li>a small-prey hunt is added so wild venomous snakes still predate mice/birds/other tiny fauna, but
     *       only while provoked (type &gt;= 3 &amp;&amp; {@link #isPissed()}, mirroring legacy {@code attackEntity}'s
     *       {@code if (!isPissed()) return;}); shy snakes never predate (legacy {@code findPlayerToAttack}
     *       {@code getClosestEntityLiving(8D)} + {@code entitiesToIgnore}).</li>
     * </ul>
     * {@link #hurtServer} flips a wild venomous snake pissed when it is hit.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(
                goal -> goal instanceof net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
                        || goal instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal);
        // Retaliation (legacy attackEntityFrom + attackEntity): a venomous snake (type >= 3) turns on whatever
        // struck it — a wild one bites back any attacker, a tamed one bites back a non-player mob but not a
        // player (see canRetaliate). Shy snakes (type < 3) never acquire an attacker as a target.
        this.targetSelector.addGoal(1,
                new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this) {
                    @Override
                    public boolean canUse() {
                        // Legacy attackEntityFrom retaliation was gated on difficultySetting > 0 (line 726):
                        // on Peaceful a struck snake never turns on its attacker.
                        return canBeAggressive() && canRetaliate() && super.canUse();
                    }

                    @Override
                    public boolean canContinueToUse() {
                        return canRetaliate() && super.canContinueToUse();
                    }
                });
        // Pissed-gated player hunt: only a wild, adult, provoked, non-shy snake goes after a player.
        this.targetSelector.addGoal(2,
                new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                        this, net.minecraft.world.entity.player.Player.class, true) {
                    @Override
                    public boolean canUse() {
                        // Legacy findPlayerToAttack player targeting was gated on difficultySetting > 0
                        // (line 742): on Peaceful a snake never acquires a player as a target.
                        return canBeAggressive() && !isHarmlessToPlayers() && getIsAdult() && isPissed()
                                && super.canUse();
                    }

                    @Override
                    public boolean canContinueToUse() {
                        return !isHarmlessToPlayers() && isPissed() && super.canContinueToUse();
                    }
                });
        // Opportunistic predation on small fauna (legacy findPlayerToAttack getClosestEntityLiving(8D) plus
        // entitiesToIgnore, which ignores anything with BOTH height > 0.5 AND width > 0.5, other snakes, and the
        // super set — i.e. prey is any small living thing, at least one dimension <= 0.5). Legacy attackEntity
        // (line 652) bailed on {@code if (!isPissed()) return;} for EVERY target, so a snake only ever bites
        // prey while provoked, and a shy variant (type < 3) — which can never become pissed — never predates at
        // all. Gate the hunt on a wild, venomous (type >= 3), pissed snake so shy snakes leave prey alone and
        // venomous ones only strike prey during the rare pissed window. The whole prey hunt additionally sits
        // behind the enableHunters master switch (legacy MoCProxy.java:314-316, default true); the player hunt
        // and retaliation goals above are not predation and stay ungated.
        this.targetSelector.addGoal(3,
                new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                        this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                        (living, serverLevel) -> drzhark.mocreatures.config.MoCConfig.get().enableHunters
                                && !getIsTamed() && getTypeMoC() >= 3 && isPissed()
                                && !(living.getBbHeight() > 0.5F && living.getBbWidth() > 0.5F)
                                && !(living instanceof MoCEntitySnake)
                                && !(living instanceof net.minecraft.world.entity.player.Player)));
        // Shy snakes flee (legacy onLivingUpdate else-branch, line 624: a !isNotScared(), untamed snake sets
        // fleeingTick = 40 when a player is within ~1.73 blocks). Because the snake behaviour is wildHostile,
        // MoCAnimal.registerGoals installs no PanicGoal, so add a gated flee goal here for the small shy
        // variants (types 1-2) and any not-yet-grown snake. Installed always; canUse() gates it via isShyFleer().
        this.goalSelector.addGoal(2,
                new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<net.minecraft.world.entity.player.Player>(
                        this, net.minecraft.world.entity.player.Player.class, 4.0F, 1.2D, 1.5D) {
                    @Override
                    public boolean canUse() {
                        return isShyFleer() && super.canUse();
                    }

                    @Override
                    public boolean canContinueToUse() {
                        return isShyFleer() && super.canContinueToUse();
                    }
                });
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int k = this.random.nextInt(100);
            if (k <= 12) {
                setTypeMoC(1);
            } else if (k <= 25) {
                setTypeMoC(2);
            } else if (k <= 37) {
                setTypeMoC(3);
            } else if (k <= 50) {
                setTypeMoC(4);
            } else if (k <= 62) {
                setTypeMoC(5);
            } else if (k <= 75) {
                setTypeMoC(6);
            } else if (k <= 87) {
                setTypeMoC(7);
            } else {
                setTypeMoC(8);
            }
            // Legacy checkSpawningBiome (lines 826-905) overrode the uniform roll with biome-specific type
            // tables and rejected snakes entirely in cold biomes / rattlesnakes outside the desert. Reproduce
            // those tables here — selectType cannot reject a spawn, so cold biomes fall back to a shy variant
            // and a non-desert rattlesnake becomes an ordinary brown snake (as the port already did):
            //   desert          -> rattlesnake (7) or spotted (2)          [legacy l<5 ? 7 : 2]
            //   windswept hills -> dark (1) / coral (5) / cobra (6)        [legacy l<4 ? 1 : l<7 ? 5 : 6]
            //   swampland       -> python (8) / green (4) / dark (1)       [legacy l<4 ? 8 : l<8 ? 4 : 1]
            //   taiga / frozen  -> shy dark (1) or spotted (2)             [legacy: no spawn at all]
            var biome = this.level().getBiome(this.blockPosition());
            int l = this.random.nextInt(10);
            if (biome.is(net.minecraft.tags.BiomeTags.IS_TAIGA)
                    || biome.value().getBaseTemperature() <= 0.15F) {
                setTypeMoC(l < 5 ? 1 : 2); // cold: legacy rejected the spawn — fall back to a shy snake
            } else if (biome.is(net.minecraft.world.level.biome.Biomes.DESERT)) {
                setTypeMoC(l < 5 ? 7 : 2); // desert: rattlesnake or spotted
            } else if (biome.is(net.minecraft.world.level.biome.Biomes.WINDSWEPT_HILLS)
                    || biome.is(net.minecraft.world.level.biome.Biomes.WINDSWEPT_FOREST)
                    || biome.is(net.minecraft.world.level.biome.Biomes.WINDSWEPT_GRAVELLY_HILLS)) {
                setTypeMoC(l < 4 ? 1 : (l < 7 ? 5 : 6)); // hills: dark / coral / cobra
            } else if (biome.is(net.minecraft.world.level.biome.Biomes.SWAMP)
                    || biome.is(net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP)) {
                setTypeMoC(l < 4 ? 8 : (l < 8 ? 4 : 1)); // swamp: python / green / dark
            } else if (getTypeMoC() == 7) {
                // Rattlesnake only spawns in the desert (legacy rejected type 7 elsewhere); here it becomes a
                // plain brown snake instead of being turned away.
                setTypeMoC(1);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("snake2.png");
            case 3 -> modelTexture("snake3.png");
            case 4 -> modelTexture("snake4.png");
            case 5 -> modelTexture("snake5.png");
            case 6 -> modelTexture("snake6.png");
            case 7 -> modelTexture("snake7.png");
            case 8 -> modelTexture("snake8.png");
            default -> modelTexture("snake1.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.SNAKEHISS.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.SNAKEHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.SNAKEDYING.get();
    }

    // -------------------------------------------------- legacy snake physics (water / swim / climb / fall)

    /** Legacy {@code canBreatheUnderwater() = true}: a snake never drowns and takes no air-supply damage. */
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /** Legacy {@code swimmerEntity() = true}: a snake paths freely through water as well as over land. */
    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation(this, level);
    }

    /** Legacy {@code isOnLadder() = isCollidedHorizontally}: a snake climbs walls it runs into. */
    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    /** Legacy {@code fall()} was an empty override: a snake takes no fall damage. */
    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /**
     * Per-variant render size (legacy {@code getSizeF()}:239-261, {@code getEdad() * 0.01F * factor}). The
     * age half of that product is the shared growth curve {@code MoCMobRenderer.scale} already applies; this
     * supplies the per-type factor, without which every variant rendered at the same size — a python is half
     * again as big as a plain snake and a coral is barely more than half of one.
     */
    @Override
    public float getSizeFactor() {
        return switch (getTypeMoC()) {
            case 1, 2 -> 0.8F; // small shy snakes
            case 5 -> 0.6F;    // coral
            case 6 -> 1.1F;    // cobra
            case 7 -> 0.9F;    // rattlesnake
            case 8 -> 1.5F;    // python
            default -> 1.0F;   // orange (3), green (4)
        };
    }

    // -------------------------------------------------- legacy client-side animation state (model inputs)

    /**
     * Client-side cosmetic timers, ported from the {@code if (world.isRemote)} block of legacy
     * {@code onUpdate} (:267-345). Nothing here is synched or authoritative — the server ticks the same
     * fields to its own values and simply never renders them, the same arrangement
     * {@link MoCEntityFishBowl#tick()} uses for its swim rotation.
     *
     * <p>Not ported from that block: the {@code fRattle == 1.0F} rattle sound (:286-289 — {@code fRattle}
     * steps 0.1, 0.3, 0.5 … and so never equals 1.0, making the branch dead), the {@code hissCounter}
     * jaw-gape at :364/:368, whose counter lives server-side only in this port, and the end-of-bite
     * {@code ENTITY_SNAKE_SNAP} sound at :335, which legacy played client-locally at the bottom of the bite
     * ramp; every other sound in this port is broadcast from the server, so it is left out rather than
     * introducing the one client-local exception.</p>
     */
    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // Resolved FIRST: legacy computed it in onLivingUpdate (:424-447), which super.onUpdate() ran at
            // legacy :265 — ahead of the client block at :267 holding the rattle chance and the movInt
            // re-roll below. Computing it last would feed both a one-tick-stale flag.
            //
            // The closest player within 12 blocks, at a TRUE distance under 5 blocks, on a snake that is not
            // scared of players. Legacy measured that with MoCTools.getSqDistanceTo (legacy :426), which
            // despite the name returns Math.sqrt of the squared distance (legacy MoCTools:327-332), so its
            // "distP < 5D" is 5 blocks and not the 2.24 a squared comparison against 5 would give. Hence 25.0.
            //
            // getNearestPlayer(entity, range) applies EntitySelector.NO_SPECTATORS (mc262-ref
            // EntityGetter:91-97), so a CREATIVE player still rears the snake. That is deliberate and legacy
            // faithful: legacy's client-side block (:424-447) has no isCreative() test anywhere in it. The
            // server-side hiss test in customServerAiStep does exclude creative, so the two halves knowingly
            // disagree — a creative player gets the pose but not the hiss. Do not "fix" one to match the other.
            net.minecraft.world.entity.player.Player nearest = this.level().getNearestPlayer(this, 12.0D);
            this.nearPlayerClient = nearest != null && isNotScared() && this.distanceToSqr(nearest) < 25.0D;

            // Tongue flick: ~1/50 ticks it starts at 0.1 and runs up in 0.2 steps to 8.1, then rests at 0.
            if (this.fTongue != 0.0F) {
                this.fTongue += 0.2F;
                if (this.fTongue > 8.0F) {
                    this.fTongue = 0.0F;
                }
            }
            // Jaw gape: ~1/100 ticks, 0.1 up in 0.1 steps to 0.6, then closed.
            if (this.fMouth != 0.0F) {
                this.fMouth += 0.1F;
                if (this.fMouth > 0.5F) {
                    this.fMouth = 0.0F;
                }
            }
            // Rattle (type 7 only), same shape as the tongue timer.
            if (getTypeMoC() == 7 && this.fRattle != 0.0F) {
                this.fRattle += 0.2F;
                if (this.fRattle > 8.0F) {
                    this.fRattle = 0.0F;
                }
            }
            if (this.random.nextInt(50) == 0 && this.fTongue == 0.0F) {
                this.fTongue = 0.1F;
            }
            if (this.random.nextInt(100) == 0 && this.fMouth == 0.0F) {
                this.fMouth = 0.1F;
            }
            // legacy :308-319 fed this from getNearPlayer(), bite half included, not the bare near-player flag.
            if (getTypeMoC() == 7 && this.random.nextInt(getNearPlayer() ? 30 : 100) == 0) {
                this.fRattle = 0.1F;
            }
            // Change of movement pattern: re-roll the secondary wave amplitude (legacy :323-325).
            if (!isResting() && !isPickedUp() && this.random.nextInt(50) == 0) {
                this.movInt = this.random.nextInt(10);
            }
            // Bite animation (legacy :327-340), which legacy ran here, client-side, off the isBiting flag the
            // server had broadcast. The ramp is what ends the bite: from 2.0 it takes five ticks to fall below
            // zero, then bodySwing resets to legacy's 2.5 and the snake drops out of the reared pose.
            if (this.biting) {
                this.bodySwing -= 0.5F;
                this.fMouth = 0.3F; // jaw pinned open for the strike, overriding the gape timer above
                if (this.bodySwing < 0.0F) {
                    this.bodySwing = 2.5F;
                    this.fMouth = 0.0F;
                    this.biting = false;
                }
            }
        }

        // legacy :343-345, deliberately outside the client-only block: a carried snake's body straightens
        // out to the primary wave alone.
        if (isPickedUp()) {
            this.movInt = 0;
        }
    }

    /** Legacy {@code isNotScared()}:188-193 — an adult of a venomous/large variant stands its ground. */
    public boolean isNotScared() {
        return getTypeMoC() > 2 && getIsAdult();
    }

    /** Legacy {@code pickedUp()}:161-163 ({@code getRidingEntity() != null}); here also the carried pose. */
    public boolean isPickedUp() {
        return isPassenger() || isBeingCarried();
    }

    /**
     * Legacy {@code isResting()}:204-206. The legacy test read {@code motionX}/{@code motionZ}, which a
     * 1.12.2 client kept current for remote mobs; here {@code getDeltaMovement()} is stale on the client, so
     * the same quantity is measured from the interpolated position instead ({@code xOld}/{@code zOld} are
     * refreshed every client tick, mc262-ref {@code ClientLevel}:470-474). The first term is legacy's full
     * {@link #getNearPlayer()}, bite half included, not the bare near-player flag.
     */
    public boolean isResting() {
        return !getNearPlayer() && onGround()
                && Math.abs(getX() - this.xOld) < 0.01D && Math.abs(getZ() - this.zOld) < 0.01D;
    }

    /**
     * Legacy {@code getNearPlayer()}:208-210 = {@code isNearPlayer || isBiting()}, both halves reproduced
     * literally: {@link #nearPlayerClient} and the {@link #biting} window the {@link #EVENT_BITE} broadcast
     * opens.
     *
     * <p>It deliberately does NOT read {@link net.minecraft.world.entity.LivingEntity#swinging}. That flag is
     * set by {@code MeleeAttackGoal}'s {@code swing()} but cleared only by {@code updateSwingTime()}, which
     * in 26.2 runs for {@code Monster}, {@code Player}, {@code RemotePlayer} and {@code Mannequin} only
     * (mc262-ref {@code Monster}:43, {@code Player}:454, {@code RemotePlayer}:67, {@code Mannequin}:179) —
     * never for an {@code Animal}. Reading it latched a snake into the reared, hooded, rattling pose forever
     * after its first bite. The bite window here ends itself through the {@link #bodySwing} ramp.</p>
     */
    public boolean getNearPlayer() {
        return this.nearPlayerClient || this.biting;
    }

    /**
     * Legacy {@code entitysnake.bodyswing}, which the legacy model read straight off the entity (legacy
     * {@code MoCModelSnake}:154) and multiplied into the reared front third's forward throw (:205).
     */
    public float getBodySwing() {
        return this.bodySwing;
    }

    /**
     * Client end of the bite sync: legacy's {@code performAnimation(0)}, which the {@code MoCMessageAnimation}
     * packet invoked to {@code setBiting(true)} on the client's copy of the snake (legacy
     * {@code MoCEntitySnake}:463-466), now driven by the vanilla entity-event broadcast.
     */
    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BITE) {
            this.biting = true;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** Legacy {@code isClimbing()}:200-202, read from the synched {@link #CLIMBING} bit (see its javadoc). */
    public boolean isClimbing() {
        return this.entityData.get(CLIMBING);
    }

    /** Legacy {@code getMovInt()}:212-214 — the secondary body wave's amplitude, 0-9. */
    public int getMovInt() {
        return this.movInt;
    }

    /** Legacy {@code getfTongue()}:389-391 — 0 when retracted, else 0.1-8.1 through a flick. */
    public float getFTongue() {
        return this.fTongue;
    }

    /** Legacy {@code getfMouth()}:397-399 — 0 when shut, else 0.1-0.5 through a gape. */
    public float getFMouth() {
        return this.fMouth;
    }

    /** Legacy {@code getfRattle()}:405-407 — 0 when still, else 0.1-8.1 through a rattle. */
    public float getFRattle() {
        return this.fRattle;
    }

    /**
     * Venomous variants are types 3-7 (orange, green, coral, cobra, rattlesnake), matching the legacy
     * {@code getType() > 2 && getType() < 8} test. Types 1-2 are shy harmless snakes and type 8 is the
     * python — a big non-venomous constrictor.
     */
    private boolean isVenomous() {
        int t = getTypeMoC();
        return t >= 3 && t <= 7;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity target) {
        // Legacy attackEntityAsMob:459-460 called setBiting(true) BEFORE the hit itself, so the strike plays
        // whether or not the blow lands, and setBiting broadcast it to every client within 64 blocks. Vanilla's
        // entity-event broadcast reaches exactly the players tracking this entity, which is the modern
        // equivalent of that TargetPoint. The flag itself is set only on the receiving clients — see biting.
        level.broadcastEntityEvent(this, EVENT_BITE);
        boolean hit = super.doHurtTarget(level, target);
        // Legacy venom (attackEntity, line 668): a venomous variant's bite injects poison on roughly half its
        // hits, but ONLY against a player — the legacy check was gated on {@code entity instanceof EntityPlayer},
        // so a mob the snake bit (e.g. prey or a retaliation victim) was never poisoned. The python constrictor
        // (type 8) and shy snakes apply no status effect at all.
        if (hit && isVenomous() && this.random.nextInt(2) == 0
                && target instanceof net.minecraft.world.entity.player.Player victim) {
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, 120, 0), this);
        }
        return hit;
    }

    /**
     * Hiss-then-bite warning (legacy {@code onUpdate}): a wild, adult, non-shy snake that finds a player
     * lingering within ~2.24 blocks hisses a warning; the longer the player crowds it, the more likely it is
     * to suddenly get pissed and start striking. Once the player leaves it calms back down. Small/shy
     * (type &lt; 3) and tamed snakes never warm up to players at all.
     *
     * <p>That ~2.24 is narrower than legacy's 5 blocks, and narrower than the client-side near-player test
     * in {@link #tick()} that rears the snake's neck. See the comment on {@code crowded} below: the gap is
     * deliberate, so a snake visibly rears at a player well before it starts hissing at them.</p>
     */
    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);

        // Legacy isClimbing():200-202 — only the server simulates movement, so publish the flag for the model.
        boolean climbing = onClimbable() && this.getDeltaMovement().y > 0.01D;
        if (climbing != isClimbing()) {
            this.entityData.set(CLIMBING, climbing);
        }

        if (isHarmlessToPlayers()) {
            // Shy or tamed: never threatens players, and never stays pissed at — or targets — one.
            this.hissCounter = 0;
            if (this.getTarget() instanceof net.minecraft.world.entity.player.Player) {
                setPissed(false);
                this.setTarget(null);
            }
            return;
        }

        net.minecraft.world.entity.player.Player nearest =
                level.getNearestPlayer(this, 5.0D);
        // Legacy onUpdate hiss/pissed build-up required difficultySetting > 0 (line 421): on Peaceful a snake
        // never warms up to a player, so it never hisses and never snaps into the pissed state.
        //
        // DELIBERATE DIVERGENCE, twice over — do not "align" this with the client-side near-player test in
        // tick(). (a) Radius: legacy's hiss ran off getNearPlayer(), i.e. 5 BLOCKS; this squared comparison
        // against 5.0 is ~2.24 blocks, so a player has to crowd the snake far more closely before it starts
        // warming up. That is this port's pre-existing gameplay tuning and is left as it stands. (b) Creative:
        // this half additionally excludes creative players, which legacy's block never did — see tick().
        boolean crowded = canBeAggressive() && nearest != null && getIsAdult()
                && !nearest.isCreative() && !nearest.isSpectator()
                && this.distanceToSqr(nearest) < 5.0D;

        if (crowded && !isPissed()) {
            // Warn: hiss for a while, then randomly snap into the pissed state and commit to biting.
            this.hissCounter++;
            if (this.hissCounter % 25 == 0) {
                level.playSound(null, this.blockPosition(), MoCSounds.SNAKEHISS.get(),
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                        1.0F + ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F));
            }
            if (this.hissCounter > 100 && this.random.nextInt(50) == 0) {
                setPissed(true);
                this.hissCounter = 0;
            }
        } else if (nearest == null || this.distanceToSqr(nearest) > 144.0D) {
            // Player gone (>12 blocks): calm down and forget any player it was angry at.
            this.hissCounter = 0;
            if (this.getTarget() instanceof net.minecraft.world.entity.player.Player) {
                setPissed(false);
                this.setTarget(null);
            }
        }
    }

    /**
     * Retaliation (legacy {@code attackEntityFrom}): striking a wild snake (type &gt;= 3, untamed) instantly
     * makes it pissed so it bites back through the gated {@code HurtByTargetGoal} installed in
     * {@link #registerGoals()}. Shy small snakes (type &lt; 3) and tamed snakes just take the hit and flee.
     */
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
                              net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // Legacy attackEntityFrom (lines 716-731): for a venomous snake (type >= 3) being struck on
        // difficultySetting > 0 (line 726) set pissed = true; the getIsTamed() check was deliberately commented
        // out, so a TAMED venomous snake also gets pissed — but attackEntity then only bit back a non-player
        // attacker (a player attacker had its target cleared). Mirror that: a wild venomous snake gets pissed at
        // any attacker, a tamed one only when the attacker is not a player.
        if (hurt && canBeAggressive() && getTypeMoC() >= 3
                && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker
                && attacker != this
                && (!getIsTamed() || !(attacker instanceof net.minecraft.world.entity.player.Player))) {
            setPissed(true);
        }
        return hurt;
    }

    /**
     * Legacy on-death loot ({@code dropFewItems}, line 761): a grown snake ({@code getEdad() > 60}, i.e. an
     * adult here) drops {@code rand.nextInt(3)} — 0, 1 or 2 — snake eggs keyed to its own variant. The legacy
     * composite egg id was {@code getType() + 20} (21-28), which the port's {@link MoCEntityEgg#setEggType}
     * decodes back into a snake egg of this snake's sub-type. This replaces the port's earlier non-legacy live
     * egg-laying, restoring the faithful eggs-drop-only-on-death behaviour.
     */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.damagesource.DamageSource source, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        if (!getIsAdult()) {
            return;
        }
        int count = this.random.nextInt(3); // 0, 1 or 2 eggs
        for (int i = 0; i < count; i++) {
            // legacy composite snake-egg id (21-28), keyed to this snake's own variant
            spawnAtLocation(level, drzhark.mocreatures.item.MoCThrownEggItem.createEgg(getTypeMoC() + 20));
        }
    }
}
