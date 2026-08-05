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
     * Synched cobra-hood flag: a type-6 cobra flares its hood while a player lingers close (legacy
     * {@code getNearPlayer()} drove the Wing1..5 hood cubes for {@code typeI == 6}). The client reads this
     * to open the hood geometry.
     */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> HOOD =
            net.minecraft.network.syncher.SynchedEntityData.defineId(
                    MoCEntitySnake.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Server-side warning timer: counts up while a wild snake hisses at a near player before it may bite. */
    private int hissCounter;

    public MoCEntitySnake(EntityType<? extends MoCEntitySnake> type, Level level) {
        super(type, level);
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
        builder.define(HOOD, false);
    }

    public boolean isPissed() {
        return this.entityData.get(PISSED);
    }

    public void setPissed(boolean pissed) {
        this.entityData.set(PISSED, pissed);
    }

    /** True while a cobra (type 6) is flaring its hood at a nearby player (drives the hood geometry). */
    public boolean isHoodFlared() {
        return this.entityData.get(HOOD);
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
        // venomous ones only strike prey during the rare pissed window.
        this.targetSelector.addGoal(3,
                new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                        this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                        (living, serverLevel) -> !getIsTamed() && getTypeMoC() >= 3 && isPissed()
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
     * lingering within ~2 blocks hisses a warning; the longer the player crowds it, the more likely it is to
     * suddenly get pissed and start striking. Once the player leaves it calms back down. Small/shy (type &lt; 3)
     * and tamed snakes never warm up to players at all.
     */
    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);

        if (isHarmlessToPlayers()) {
            // Shy or tamed: never threatens players, and never stays pissed at — or targets — one.
            this.hissCounter = 0;
            if (this.getTarget() instanceof net.minecraft.world.entity.player.Player) {
                setPissed(false);
                this.setTarget(null);
            }
            if (isHoodFlared()) {
                this.entityData.set(HOOD, false);
            }
            return;
        }

        net.minecraft.world.entity.player.Player nearest =
                level.getNearestPlayer(this, 5.0D);
        // Legacy onUpdate hiss/pissed build-up required difficultySetting > 0 (line 421): on Peaceful a snake
        // never warms up to a player, so it never hisses and never snaps into the pissed state.
        boolean crowded = canBeAggressive() && nearest != null && getIsAdult()
                && !nearest.isCreative() && !nearest.isSpectator()
                && this.distanceToSqr(nearest) < 5.0D; // legacy getNearPlayer: distP < 5 (~2.24 blocks)

        // Cobra (type 6): rear up and flare the hood while a player lingers within ~4 blocks (legacy nearplayer).
        boolean hood = getTypeMoC() == 6 && nearest != null && this.distanceToSqr(nearest) < 16.0D;
        if (hood != isHoodFlared()) {
            this.entityData.set(HOOD, hood);
        }

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
