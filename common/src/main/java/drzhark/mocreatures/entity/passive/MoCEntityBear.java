package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityBear}. A large land animal with brown, black, panda and polar variants.
 */
public class MoCEntityBear extends MoCAnimal {

    public MoCEntityBear(EntityType<? extends MoCEntityBear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Legacy findPlayerToAttack: only the POLAR bear (type 4) ever proactively targets a player. Its
        // brown-bear sub-clause `f < 0.0F && getType() == 1` is dead code (getBrightness is never negative),
        // so brown (1), black (2) and panda (3) never HUNT players — they only retaliate when struck. The base
        // MoCAnimal installs a generic player-target goal for every untamed adult bear (the bear spec is
        // .hostile()); replace it with a polar-only one so brown/black bears stop chasing players unprovoked,
        // while their HurtByTargetGoal retaliation (kept from the base) still fires.
        this.targetSelector.removeAllGoals(
                goal -> goal instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal);
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false,
                (living, serverLevel) -> !getIsTamed() && getIsAdult() && getTypeMoC() == 4));
        // Legacy findPlayerToAttack mob-branch (`rand.nextInt(80) == 0 && getType() != 3`): brown/black/polar
        // bears also prey on nearby SMALLER passive creatures (getClosestEntityLiving within 10 blocks, ~1/80
        // per tick). canHunt mirrors the bear's legacy entitiesToIgnore; pandas (3) never hunt.
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, Animal.class, 80, true, false,
                (living, serverLevel) -> getTypeMoC() != 3 && canHunt(living)));
        // Legacy Bear.isNotScared() == (getType() != 3): only the PANDA is timid — when struck it keeps its
        // vanilla fleeingTick and runs away. The base skips PanicGoal for every bear (the spec is .hostile()),
        // so give the panda (and only the panda) its own panic goal; brown/black/polar stay fearless and fight.
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.PanicGoal(this, 1.5D) {
            @Override
            public boolean canUse() {
                return getTypeMoC() == 3 && super.canUse();
            }
        });
    }

    /**
     * Legacy bear {@code entitiesToIgnore} (base filter + the bear's own override): which nearby creature a
     * bear will prey on. The {@code Animal.class} scan already keeps out players, hostile {@code Monster}s
     * being unlisted, and non-living entities; here we additionally skip itself and other bears, big cats,
     * elephants, the kitty furniture and MoC eggs, any creature at least as wide OR tall as the bear (bears
     * take only smaller prey), a tamed MoC creature while this bear is itself tamed, and horses/wolves unless
     * the matching config flag is enabled.
     */
    private boolean canHunt(net.minecraft.world.entity.LivingEntity target) {
        if (target == this) {
            return false;
        }
        // Legacy size gate: skip anything at least as wide OR tall as the bear (entity >= this.width/height).
        if (target.getBbWidth() >= this.getBbWidth() || target.getBbHeight() >= this.getBbHeight()) {
            return false;
        }
        if (target instanceof MoCEntityBear
                || target instanceof MoCEntityBigCat
                || target instanceof MoCEntityElephant
                || target instanceof MoCEntityKittyBed
                || target instanceof MoCEntityLitterBox
                || target instanceof MoCEntityEgg) {
            return false;
        }
        // Bears never hunt hostile mobs (legacy EntityMob skip).
        if (target instanceof net.minecraft.world.entity.monster.Monster) {
            return false;
        }
        // A tamed bear won't turn on another tamed Mo'Creature (legacy tamed-MoCIMoCreature skip).
        if (getIsTamed() && target instanceof drzhark.mocreatures.entity.IMoCEntity moc && moc.getIsTamed()) {
            return false;
        }
        drzhark.mocreatures.config.MoCConfig cfg = drzhark.mocreatures.config.MoCConfig.get();
        if (target instanceof MoCEntityHorse && !cfg.attackHorses) {
            return false;
        }
        if (target instanceof net.minecraft.world.entity.animal.wolf.Wolf && !cfg.attackWolves) {
            return false;
        }
        return true;
    }

    // Legacy bear pose state machine (drives the model pose in the render batch): 0 on all fours, 1 reared up
    // on hind legs, 2 sitting.
    public static final int STATE_FOURS = 0;
    public static final int STATE_STANDING = 1;
    public static final int STATE_SITTING = 2;
    private static final EntityDataAccessor<Integer> BEAR_STATE =
            SynchedEntityData.defineId(MoCEntityBear.class, EntityDataSerializers.INT);
    /** Counts up while reared on hind legs; the bear drops back to all fours after a short while. */
    private int standingCounter;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BEAR_STATE, STATE_FOURS);
    }

    public int getBearState() {
        return this.entityData.get(BEAR_STATE);
    }

    public void setBearState(int state) {
        this.entityData.set(BEAR_STATE, state);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Legacy panda (bear type 3) never fights: findPlayerToAttack returns null for it and attackEntityFrom
        // leaves entityToAttack unset (its `type != 3` gate), so a struck panda does not retaliate — instead,
        // being the one "scared" bear (isNotScared() == false), it flees via the panda-only PanicGoal added in
        // registerGoals. The base still installs generic hostile target/retaliate goals, so drop any target a
        // panda acquires each tick, neutralising the NearestAttackableTargetGoal hunt and the HurtByTargetGoal
        // retaliation that the MeleeAttackGoal would otherwise read (leaving only the flee).
        if (getTypeMoC() == 3) {
            setTarget(null);
        }
        // Keep the per-type max health applied (covers spawn + the type chosen in selectType).
        applyTypeStats();
        // Pandas and young cubs plop down to sit; a sitting bear eventually gets back up.
        if ((getTypeMoC() == 3 || (!getIsAdult() && getMoCAge() < 60)) && this.random.nextInt(300) == 0) {
            setBearState(STATE_SITTING);
        }
        if (getBearState() == STATE_SITTING && this.random.nextInt(800) == 0) {
            setBearState(STATE_FOURS);
        }
        // Adult non-panda bears rear up on their hind legs near a player, then drop back after a moment.
        if (this.standingCounter == 0 && getBearState() != STATE_SITTING && getIsAdult() && getTypeMoC() != 3
                && this.random.nextInt(500) == 0 && level.getNearestPlayer(this, 8.0D) != null) {
            setBearState(STATE_STANDING);
            this.standingCounter = 1;
        }
        if (this.standingCounter > 0 && ++this.standingCounter > 75) {
            this.standingCounter = 0;
            if (getBearState() == STATE_STANDING) {
                setBearState(STATE_FOURS);
            }
        }
        // Legacy panda auto-heal (onLivingUpdate): a live, on-fours panda (type 3) seeks the nearest dropped
        // sugar cane (Item.reed) or sugar (Item.sugar) within 12 blocks, paths to it, and on contact (<2
        // blocks) eats it — full-heals to getMaxHealth() and plays the eating sound. Lets a hurt wild panda
        // recover without hand-feeding, matching the legacy mod.
        if (getTypeMoC() == 3 && isAlive() && getBearState() != STATE_SITTING) {
            ItemEntity nearest = null;
            double nearestSq = Double.MAX_VALUE;
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(12.0D),
                    it -> it.isAlive() && (it.getItem().is(Items.SUGAR_CANE) || it.getItem().is(Items.SUGAR)))) {
                double dsq = this.distanceToSqr(item);
                if (dsq < nearestSq) {
                    nearestSq = dsq;
                    nearest = item;
                }
            }
            if (nearest != null) {
                if (nearestSq < 4.0D) { // within 2 blocks: eat and full-heal
                    nearest.discard();
                    this.playSound(MoCSounds.EATING.get(), 1.0F,
                            1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
                    setHealth(getMaxHealth());
                } else {
                    this.getNavigation().moveTo(nearest, 1.0D);
                }
            }
        }
        // A posed bear (reared up OR sitting) is rooted in place: stop pathing AND cancel horizontal drift
        // each tick, so a hostile bear that stands/sits can't slide or "drag" across the floor chasing you
        // while in the pose. It resumes normal movement once it drops back onto all fours.
        if (getBearState() != STATE_FOURS) {
            this.getNavigation().stop();
            net.minecraft.world.phys.Vec3 dm = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, dm.y, 0.0D);
        }
    }

    /** Per-type max health (legacy {@code getMaxHealth}): polar 25, brown 20, black &amp; panda 15. */
    private int getMaxHealthMoC() {
        return switch (getTypeMoC()) {
            case 2, 3 -> 15; // black, panda
            case 4 -> 25;    // polar
            default -> 20;   // brown (types 0/1)
        };
    }

    /**
     * Per-type melee damage (legacy {@code getAttackStrength}) scaled by the world difficulty ordinal
     * ({@code difficultySetting}): brown 2*d, black 1*d, polar 3*d, panda 1 (fixed), unresolved type 0 -> 2.
     * On Hard (d=3): brown 6, black 3, polar 9, panda 1; on Easy (d=1) brown 2.
     */
    private int getAttackStrengthMoC() {
        int d = this.level().getDifficulty().getId();
        return switch (getTypeMoC()) {
            case 3 -> 1;     // panda (fixed, does not scale with difficulty)
            case 4 -> 3 * d; // polar
            case 2 -> 1 * d; // black
            case 1 -> 2 * d; // brown
            default -> 2;    // unresolved type 0
        };
    }

    /**
     * Keeps the MAX_HEALTH attribute in sync with the current type's {@link #getMaxHealthMoC()} value.
     * The port's base attribute is a flat 20; legacy instead had per-type max health and set
     * {@code health = getMaxHealth()} in selectType. Applied every server tick (same approach as the
     * dolphin's speed tiers) so the type chosen at spawn is reflected without hooking every setTypeMoC
     * call site. Heals up to the new max when the base value rises (e.g. a polar bear), matching legacy.
     */
    private void applyTypeStats() {
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            double want = getMaxHealthMoC();
            if (hp.getBaseValue() != want) {
                hp.setBaseValue(want);
                if (getHealth() < want) {
                    setHealth((float) want);
                }
            }
        }
        // Keep melee damage in sync with the current type AND world difficulty (legacy getAttackStrength,
        // which the vanilla MeleeAttackGoal reads via ATTACK_DAMAGE). Pandas stay at a flat 1; the others
        // scale with difficulty, so polar out-hits brown out-hits black.
        var atk = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            double wantAtk = getAttackStrengthMoC();
            if (atk.getBaseValue() != wantAtk) {
                atk.setBaseValue(wantAtk);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Pandas (type 3) are tamed by hand-feeding sugar cane or a sugar lump (legacy panda-only taming).
        if (getTypeMoC() == 3 && !getIsTamed()
                && (stack.is(Items.SUGAR_CANE) || stack.is(MoCItems.SUGARLUMP.get()))) {
            if (!this.level().isClientSide()) {
                // Legacy routed EVERY tame through MoCTools.tameWithName, which enforced the per-player pet
                // cap; refuse without consuming the food when the player is already at their limit.
                if (exceedsTameCap(player)) {
                    return InteractionResult.SUCCESS;
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                setTamed(true);
                setOwnerName(player.getName().getString());
                // Legacy tameWithName prompted for a name the instant a creature was tamed.
                drzhark.mocreatures.network.MoCNetwork.promptName(this, player);
                heal(getMaxHealth());
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            // Polar bears spawn in snowy/frozen biomes (legacy checkSpawningBiome). Elsewhere: brown/black/panda.
            if (this.level().getBiome(this.blockPosition()).value().getBaseTemperature() <= 0.15F) {
                setTypeMoC(4); // polar bear
                return;
            }
            int i = this.random.nextInt(100);
            if (i <= 40) {
                setTypeMoC(1);
            } else if (i <= 80) {
                setTypeMoC(2);
            } else {
                setTypeMoC(3);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("bearblack.png");
            case 3 -> modelTexture("bearpanda.png");
            case 4 -> modelTexture("bearpolar.png");
            default -> modelTexture("bearbrowm.png");
        };
    }

    /**
     * Per-type render scale (legacy {@code getBearSize}): the burly polar (1.4x) and brown (1.2x) bears
     * tower over the smaller black bears (0.9x) and pandas (0.8x); the unresolved type 0 falls back to 1.0x.
     */
    @Override
    public float getSizeFactor() {
        return switch (getTypeMoC()) {
            case 1 -> 1.2F; // brown
            case 2 -> 0.9F; // black
            case 3 -> 0.8F; // panda
            case 4 -> 1.4F; // polar
            default -> 1.0F;
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.BEARGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.BEARHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.BEARDEATH.get();
    }
}
