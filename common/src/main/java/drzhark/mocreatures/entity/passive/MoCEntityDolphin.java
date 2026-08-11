package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAquatic;
import drzhark.mocreatures.registry.MoCEntities;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityDolphin}. A tameable, rideable dolphin with six colour variants.
 *
 * <p>Faithful to DrZhark's 1.12.2 behaviour beyond the plain re-skin:
 * <ul>
 *   <li><b>Per-type swim speed.</b> The legacy {@code getCustomSpeed()} gave each colour a distinct
 *       speed (1.5&rarr;6.5). That is reproduced here via {@link #getCustomSpeed()} and applied to the
 *       {@code MOVEMENT_SPEED} attribute (scaled into the vanilla attribute range) in
 *       {@link #applyTypeStats()}, kept in sync every server tick like the horse coat tiers.</li>
 *   <li><b>Fish eating &amp; shark hunting.</b> A wild or hungry dolphin swims to and eats dropped
 *       raw-fish item entities floating in the water (healing to full), exactly as the legacy
 *       {@code getClosestFish} loop did; it never hunts live fish. Its only entity target is an untamed
 *       {@link MoCEntityShark}, gated behind the {@code attackDolphins} config (legacy default false) via a
 *       {@link NearestAttackableTargetGoal} + {@link MeleeAttackGoal}.</li>
 *   <li><b>Fish-lure taming / feeding.</b> A {@link TemptGoal} draws the dolphin toward a player holding
 *       raw or cooked fish (its heal foods), and feeding it fish marks it as fed for breeding.</li>
 *   <li><b>Breeding.</b> Two tamed, adult, recently-fed dolphins near each other gestate and spawn a baby,
 *       reproducing the legacy gestation loop (vanilla {@code BreedGoal} is unavailable on a
 *       {@link WaterAnimal}, which is not an {@code Animal}).</li>
 * </ul>
 */
public class MoCEntityDolphin extends MoCAquatic {

    private static final EntityDataAccessor<Boolean> IS_HUNGRY =
            SynchedEntityData.defineId(MoCEntityDolphin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_EATEN =
            SynchedEntityData.defineId(MoCEntityDolphin.class, EntityDataSerializers.BOOLEAN);

    /** Legacy gestation counter (incremented while a fed pair is together, spawns a calf past 50). */
    private int gestationTime;

    public MoCEntityDolphin(EntityType<? extends MoCEntityDolphin> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                // The dolphin's bite (legacy attackEntity dealt a flat 5 damage to untamed sharks it hunts).
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                // Required by the dolphin's TemptGoal: WaterAnimal.createMobAttributes() omits TEMPT_RANGE (only
                // Animal.createAnimalAttributes() supplies it), so TemptGoal.canUse() would throw "Can't find
                // attribute minecraft:tempt_range" and crash the server the instant a dolphin ticks.
                .add(Attributes.TEMPT_RANGE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_HUNGRY, false);
        builder.define(HAS_EATEN, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Lure: swim toward a player holding raw/cooked fish (the dolphin's heal foods). Used both to
        // approach for feeding/taming and to gather a breeding pair. Not scared off (canScare = false).
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D,
                stack -> stack.is(Items.COD) || stack.is(Items.COOKED_COD), false));
        // Bite the current attack target (an untamed shark set by the config-gated goal below, or an
        // attacker acquired via the HurtByTargetGoal retaliation), reproducing the legacy 5-damage
        // attackEntity bite. No target is set otherwise, so it stays idle.
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.4D, true));
        // Retaliation: a struck dolphin (wild or tamed) turns and bites its attacker, faithful to the legacy
        // attackEntityFrom which set entityToAttack = attacker on any hit. The MeleeAttackGoal then delivers
        // the bite (legacy attackEntity dealt a flat 5 to the acquired target).
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Cross-species hunting: an adult dolphin hunts nearby UNTAMED sharks, faithful to the legacy
        // findPlayerToAttack -> FindTarget (which only ever targeted untamed sharks). Gated behind the
        // attackDolphins config (legacy default false) and non-peaceful difficulty, and never while ridden.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this, MoCEntityShark.class, 10, true, false,
                (living, serverLevel) -> drzhark.mocreatures.config.MoCConfig.get().attackDolphins
                        && getIsAdult() && !this.isVehicle()
                        && this.level().getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                        && living instanceof MoCEntityShark shark && !shark.getIsTamed()));
    }

    /**
     * Per-type swim speed, faithful to the legacy {@code getCustomSpeed()} (1.5&rarr;6.5 for types 1&rarr;6).
     * The rarer, deeper-coloured dolphins swim markedly faster.
     */
    public double getCustomSpeed() {
        return switch (getTypeMoC()) {
            case 2 -> 2.5D;
            case 3 -> 3.5D;
            case 4 -> 4.5D;
            case 5 -> 5.5D;
            case 6 -> 6.5D;
            default -> 1.5D;
        };
    }

    /**
     * Keeps the MOVEMENT_SPEED attribute in sync with the current colour's {@link #getCustomSpeed()}. The
     * legacy speeds were raw move values; scaled by 0.2 they land in a sensible vanilla attribute range
     * (~0.3 for the common dolphin up to ~1.3 for the fastest). Called every server tick so a colour change
     * from breeding is reflected without hooking every setTypeMoC call site (same approach as the horse).
     */
    private void applyTypeStats() {
        var attr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) {
            double want = getCustomSpeed() * 0.2D;
            if (attr.getBaseValue() != want) {
                attr.setBaseValue(want);
            }
        }
    }

    public boolean getIsHungry() {
        return this.entityData.get(IS_HUNGRY);
    }

    public void setIsHungry(boolean flag) {
        this.entityData.set(IS_HUNGRY, flag);
    }

    public boolean getHasEaten() {
        return this.entityData.get(HAS_EATEN);
    }

    public void setHasEaten(boolean flag) {
        this.entityData.set(HAS_EATEN, flag);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Cooked cod on a TAMED ADULT dolphin is the breeding trigger, and it must work at ANY health.
        // Legacy MoCEntityDolphin.interact:381-391 is `if (fishCooked && getIsTamed() && getIsAdult())` with
        // no health condition at all. Delegating to super instead does not work, because the shared aquatic
        // feed branch (MoCAquatic:107) is gated on `getHealth() < getMaxHealth()` — it exists to heal a hurt
        // dolphin, not to feed a healthy one. A full-health dolphin therefore matched nothing, super returned
        // PASS, and HasEaten could never be set; worse, on a non-Success result the client's startUseItem
        // falls through to USING the held item, so the player ate their own cod. Same defect the fishy had.
        if (stack.is(Items.COOKED_COD) && getIsTamed() && getIsAdult()) {
            if (!this.level().isClientSide()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                heal(25.0F);          // legacy: cooked fish healed +25 on a tamed adult
                setHasEaten(true);
                setIsHungry(false);
                this.level().playSound(null, blockPosition(), drzhark.mocreatures.registry.MoCSounds.EATING.get(),
                        net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
            return InteractionResult.SUCCESS;
        }
        // Raw cod (and cooked cod on a wild or juvenile dolphin) still goes through the shared heal path:
        // it satisfies hunger and raises a wild dolphin's temper, but never flags it ready to breed —
        // legacy healed +15 for raw fish without ever calling setHasEaten.
        boolean fedFish = (stack.is(Items.COD) || stack.is(Items.COOKED_COD))
                && getHealth() < getMaxHealth();
        InteractionResult result = super.mobInteract(player, hand);
        if (fedFish && !this.level().isClientSide() && result.consumesAction()) {
            setIsHungry(false);
            // Legacy MoCEntityDolphin.interact:339-367 raised temper by 25 per fish, capped just below
            // getMaxTemper(). Temper is what shortens the ride break-in roll in MoCAquatic — without it a
            // wild dolphin's submit odds stay pinned at their worst value no matter how much fish you feed.
            if (!getIsTamed()) {
                setTemper(Math.min(getMaxTemper() - 1, getTemper() + 25));
            }
        }
        return result;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Keep the per-colour swim speed applied (covers spawn + post-breeding colour changes).
        applyTypeStats();

        // Grow babies up and occasionally become hungry (legacy onLivingUpdate: slow ageing + random hunger).
        if (!getIsAdult() && this.random.nextInt(50) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= 150) {
                setAdult(true);
            }
        }
        if (!getIsHungry() && this.random.nextInt(100) == 0) {
            setIsHungry(true);
        }

        eatDroppedFish(level);

        breed(level);
    }

    /**
     * Legacy dropped-fish eating (from {@code onLivingUpdate}): a wild (untamed) or hungry dolphin, while not
     * being ridden, swims toward the nearest dropped raw-fish item floating in the water and, once within
     * reach, occasionally snaps it up (removing the item and healing to full). Faithful to the legacy
     * {@code getClosestFish}, which only recognised raw fish ({@code Item.fishRaw} = {@link Items#COD}) that
     * {@code isInWater}. The dolphin never hunts live fish. (The port has no temper stat, so the legacy
     * temper-gain on eating has no equivalent.)
     */
    private void eatDroppedFish(ServerLevel level) {
        if (this.isVehicle() || (getIsTamed() && !getIsHungry())) {
            return;
        }
        net.minecraft.world.entity.item.ItemEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (net.minecraft.world.entity.item.ItemEntity fish : level.getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                this.getBoundingBox().inflate(12.0D, 12.0D, 12.0D),
                it -> it.isAlive() && it.isInWater() && it.getItem().is(Items.COD))) {
            double dsq = this.distanceToSqr(fish);
            if (dsq < best) {
                best = dsq;
                nearest = fish;
            }
        }
        if (nearest == null) {
            return;
        }
        // Legacy ate within a 2-block reach; otherwise it kept swimming toward the fish.
        if (best < 4.0D) {
            if (this.random.nextInt(20) == 0) {
                nearest.discard();
                this.setHealth(this.getMaxHealth());
            }
        } else {
            this.getNavigation().moveTo(nearest, 1.2D);
        }
    }

    /** True when a dolphin is eligible to parent: tamed, adult, recently fed and not being ridden. */
    private boolean readyForParenting(MoCEntityDolphin dolphin) {
        return !dolphin.isVehicle() && dolphin.getIsTamed() && dolphin.getHasEaten() && dolphin.getIsAdult();
    }

    /**
     * Legacy gestation-based breeding: when this dolphin and a nearby partner are both tamed, adult and fed,
     * a gestation counter climbs and eventually spawns a baby whose colour is inherited from the parents.
     * Vanilla {@link net.minecraft.world.entity.ai.goal.BreedGoal} cannot be used here because a
     * {@link WaterAnimal} is not an {@code Animal}, so the legacy loop is reproduced directly.
     */
    private void breed(ServerLevel level) {
        if (!readyForParenting(this)) {
            return;
        }
        // Don't overcrowd: skip if more than a couple of dolphins are already clustered here (legacy cap).
        if (level.getEntitiesOfClass(MoCEntityDolphin.class, this.getBoundingBox().inflate(8.0D, 2.0D, 8.0D)).size() > 2) {
            return;
        }
        for (MoCEntityDolphin partner : level.getEntitiesOfClass(MoCEntityDolphin.class,
                this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D))) {
            if (partner == this || !readyForParenting(partner)) {
                continue;
            }
            if (this.random.nextInt(100) == 0) {
                gestationTime++;
            }
            if (gestationTime <= 50) {
                continue;
            }
            MoCEntityDolphin baby = MoCEntities.DOLPHIN.get().create(level, EntitySpawnReason.BREEDING);
            if (baby != null) {
                baby.setPos(getX(), getY(), getZ());
                baby.setMoCAge(35);
                baby.setAdult(false);
                // Legacy setTypeInt(genetics): set the type then selectType(). When genetics() falls back to 0
                // (mixed parents that failed the luck rolls), selectType() re-randomises the calf's colour
                // (weighted 1-6); for an inherited 1-6 type selectType() is a no-op.
                int g = genetics(this, partner);
                baby.setTypeMoC(g);
                if (g == 0) {
                    baby.selectType();
                }
                level.addFreshEntity(baby);
            }
            setHasEaten(false);
            partner.setHasEaten(false);
            gestationTime = 0;
            partner.gestationTime = 0;
            break;
        }
    }

    /** Legacy colour inheritance: same-type parents breed true, otherwise a chance at a blended/rarer calf. */
    private int genetics(MoCEntityDolphin a, MoCEntityDolphin b) {
        if (a.getTypeMoC() == b.getTypeMoC()) {
            return a.getTypeMoC();
        }
        int sum = a.getTypeMoC() + b.getTypeMoC();
        if (sum < 5 && this.random.nextInt(3) == 0) {
            return sum;
        }
        if ((sum == 5 || sum == 6) && this.random.nextInt(10) == 0) {
            return sum;
        }
        // Legacy Genetics() returns 0 in this fallback; the caller's setTypeInt(0) then re-rolls the colour
        // via selectType() rather than defaulting to the common blue dolphin (type 1).
        return 0;
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 35) {
                setTypeMoC(1);
            } else if (i <= 60) {
                setTypeMoC(2);
            } else if (i <= 85) {
                setTypeMoC(3);
            } else if (i <= 96) {
                setTypeMoC(4);
            } else if (i <= 98) {
                setTypeMoC(5);
            } else {
                setTypeMoC(6);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("dolphin2.png");
            case 3 -> modelTexture("dolphin3.png");
            case 4 -> modelTexture("dolphin4.png");
            case 5 -> modelTexture("dolphin5.png");
            case 6 -> modelTexture("dolphin6.png");
            default -> modelTexture("dolphin.png");
        };
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("IsHungry", getIsHungry());
        output.putBoolean("HasEaten", getHasEaten());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setIsHungry(input.getBooleanOr("IsHungry", false));
        setHasEaten(input.getBooleanOr("HasEaten", false));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.DOLPHIN.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.DOLPHINHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.DOLPHINDYING.get();
    }
}
