package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityAnt} (1.12.2 {@code entity/ambient/MoCEntityAnt.java}).
 *
 * <p><b>Base class choice.</b> The legacy ant extends {@code MoCEntityInsect}, which is the shared base for
 * BOTH the flying and the crawling Mo'Creatures insects — the flying half is switched on by
 * {@code isFlyer()}, and the ant overrides {@code isFlyer()} and {@code getIsFlying()} to a hard
 * {@code false} (legacy {@code MoCEntityAnt}:104-117). It therefore never takes off, never uses
 * {@code navigatorFlyer} ({@code MoCEntityInsect.getNavigator} only swaps in the flying navigator while
 * {@code getIsFlying()}), and is a purely ground-bound crawler. So this port extends {@link MoCAnimal},
 * NOT {@code MoCFlyingInsect} — exactly like the port's other crawling ex-{@code MoCEntityInsect} species
 * ({@link MoCEntityRoach}, {@link MoCEntityCricket}, {@link MoCEntityMaggot}, {@link MoCEntitySnail}).</p>
 *
 * <p><b>The ant's one real mechanic: it hauls food home.</b> Legacy {@code onLivingUpdate}:49-94 is a
 * two-phase loop driven by a single synched boolean ({@code FOUND_FOOD}, datawatcher-registered in
 * {@code entityInit}):</p>
 * <ol>
 *   <li><b>Forage.</b> While it is not carrying anything, the ant scans for the closest edible dropped
 *       item within 8 blocks ({@code MoCTools.getClosestFood(this, 8D)}) that is not already being carried
 *       by another ant ({@code getRidingEntity() == null}). Further than 1 block away it turns to face the
 *       item ({@code faceLocation(..., 30F)}) and paths straight at it ({@code getMyOwnPath}); closer than
 *       1 block it "picks it up".</li>
 *   <li><b>Pick up and carry.</b> Pick-up is legacy {@code exchangeItem}: the found item entity is killed
 *       and an identical fresh one is spawned on top of the ant, then {@code setHasFood(true)}. On the
 *       following ticks the carrying branch makes that item literally <em>ride</em> the ant
 *       ({@code entityitem.startRiding(this)}), which is how the classic "ant carrying a porkchop on its
 *       back" visual works — the item is a passenger, rendered by vanilla at the ant's passenger
 *       attachment point. The exchange exists because it guarantees a brand-new item entity with no
 *       boarding cooldown and no pickup delay, so the mount always succeeds. If the cargo is ever lost
 *       (picked up by a player, despawned, merged) the ant clears its flag and goes foraging again.</li>
 * </ol>
 *
 * <p>A laden ant is slow: legacy overrode {@code getAIMoveSpeed()} to 0.05 while carrying and 0.15
 * otherwise — a hard constant that bypasses the 0.28 {@code MOVEMENT_SPEED} attribute the ant also sets.
 * The 26.2 equivalent of {@code getAIMoveSpeed()} is {@link net.minecraft.world.entity.LivingEntity#getSpeed()}
 * (both are the value {@code travel} feeds to {@code moveRelative}), so that is what is overridden here.</p>
 *
 * <p>Ants are not tameable, have no sub-types (one texture, {@code ant.png}), make no sounds and drop
 * nothing — legacy {@code MoCEntityAnt} declares no {@code getDropItemId}, no sound overrides and no
 * {@code selectType}.</p>
 */
public class MoCEntityAnt extends MoCAnimal {

    /**
     * Synched: this ant has found and is hauling a piece of food. Legacy {@code FOUND_FOOD}
     * ({@code MoCEntityAnt}:17), registered in {@code entityInit} and deliberately NOT written to NBT —
     * an ant reloaded from disk re-derives it from whatever is (or is no longer) riding it.
     */
    private static final EntityDataAccessor<Boolean> HAS_FOOD =
            SynchedEntityData.defineId(MoCEntityAnt.class, EntityDataSerializers.BOOLEAN);

    public MoCEntityAnt(EntityType<? extends MoCEntityAnt> type, Level level) {
        super(type, level);
    }

    /**
     * Legacy attributes: {@code MoCEntityInsect.applyEntityAttributes} sets 4.0 max health and 0.25 move
     * speed, and {@code MoCEntityAnt.applyEntityAttributes}:38 raises movement speed to 0.28. (The
     * attribute only feeds the move control's target speed — the actual travel speed is clamped by
     * {@link #getSpeed()} below, exactly as legacy {@code getAIMoveSpeed()} did.)
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D);
    }

    /**
     * Legacy {@code initEntityAI} gives the ant its own faster wander — {@code EntityAIWanderMoC2(this, 1.2D)}
     * at priority 1, on top of the 1.0 wander {@code MoCEntityInsect} installs. Swap the shared 1.0 stroll
     * {@link MoCAnimal} adds for a 1.2 one so ants bustle about at the legacy pace.
     *
     * <p>Note the deliberate absence of any "stop wandering while carrying a passenger" rule: legacy
     * {@code EntityAIWanderMoC2}:41 explicitly exempts {@code MoCEntityAnt} from the
     * {@code isBeingRidden() -> don't wander} check so a laden ant keeps walking. Vanilla's
     * {@code RandomStrollGoal} only bails on a <em>controlling</em> passenger
     * ({@code hasControllingPassenger()}), and {@link MoCAnimal#getControllingPassenger()} only ever returns
     * a Player on a rideable creature — an {@link ItemEntity} passenger is not one — so the vanilla goal
     * already behaves the way the legacy exemption made it behave.</p>
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(g -> g instanceof WaterAvoidingRandomStrollGoal);
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.2D));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_FOOD, false);
    }

    /** Legacy {@code getHasFood()}: this ant is hauling a piece of food on its back. */
    public boolean getHasFood() {
        return this.entityData.get(HAS_FOOD);
    }

    /** Legacy {@code setHasFood(boolean)}. */
    public void setHasFood(boolean hasFood) {
        this.entityData.set(HAS_FOOD, hasFood);
    }

    /**
     * Legacy {@code MoCEntityAnt.onLivingUpdate}:49-94, transcribed step for step. The whole loop was
     * server-side for the forage half ({@code !world.isRemote}); the carry half ran on both sides in
     * legacy but only ever does anything meaningful on the server (it spawns/mounts entities), so it lives
     * in {@code customServerAiStep} here.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // ---- Phase 1: forage. Walk to the nearest edible dropped item within 8 blocks. --------------
        if (!getHasFood()) {
            ItemEntity food = getClosestFood(level, 8.0D);
            if (food == null || !food.isAlive()) {
                return;                                  // legacy returned outright — no wander override
            }
            if (!food.isPassenger()) {                   // legacy getRidingEntity() == null: not another ant's cargo
                float distance = this.distanceTo(food);
                if (distance > 1.0F) {
                    // Legacy faceLocation(floor(posX), floor(posY), floor(posZ), 30F) — turn toward the
                    // food's block, then path at it (getMyOwnPath -> getPathToEntityLiving + setPath(1.0)).
                    this.getLookControl().setLookAt(food.getX(), food.getY(), food.getZ(), 30.0F, 30.0F);
                    this.getNavigation().moveTo(food, 1.0D);
                    return;
                }
                if (distance < 1.0F) {
                    exchangeItem(level, food);
                    setHasFood(true);
                    return;
                }
            }
        }

        // ---- Phase 2: carry. Make the food ride the ant; drop the flag if the cargo is gone. --------
        if (getHasFood()) {
            if (!this.isVehicle()) {
                ItemEntity food = getClosestFood(level, 2.0D);
                if (food != null && !food.isPassenger()) {
                    food.startRiding(this);
                    return;
                }
                // Nothing left to shoulder (eaten, despawned, merged or picked up) — forage again.
                if (!this.isVehicle()) {
                    setHasFood(false);
                }
            }
        }
    }

    /**
     * Legacy {@code MoCTools.getClosestFood(entity, d)}: the nearest {@link ItemEntity} holding an edible
     * item whose squared distance is under {@code range}, searched in a box inflated by {@code range} on
     * every axis (the legacy {@code expand(d, d, d)} box, plus the same squared-distance filter it applied).
     */
    private @Nullable ItemEntity getClosestFood(ServerLevel level, double range) {
        ItemEntity closest = null;
        double closestSqr = -1.0D;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                this.getBoundingBox().inflate(range), e -> e.isAlive() && isAntFood(e.getItem()))) {
            double sqr = item.distanceToSqr(this.getX(), this.getY(), this.getZ());
            if (sqr < range * range && (closestSqr == -1.0D || sqr < closestSqr)) {
                closestSqr = sqr;
                closest = item;
            }
        }
        return closest;
    }

    /**
     * Legacy {@code exchangeItem}:96-102 — kill the found item entity and spawn an identical fresh one
     * right on top of the ant ({@code posY + 0.2}). The swap matters: the replacement is a brand-new
     * {@link ItemEntity} with a zero {@code boardingCooldown} and no pickup delay, so the
     * {@code startRiding} on the next tick is guaranteed to be accepted (vanilla
     * {@code Entity.canRide} refuses while a boarding cooldown is running, and any item a player has just
     * thrown carries one).
     */
    private void exchangeItem(ServerLevel level, ItemEntity food) {
        ItemEntity cargo = new ItemEntity(level, this.getX(), this.getY() + 0.2D, this.getZ(),
                food.getItem().copy());
        food.discard();
        level.addFreshEntity(cargo);
    }

    /**
     * Legacy {@code MoCTools.isItemEdible}: any {@code ItemFood} or {@code ItemSeeds}, plus wheat, sugar,
     * cake and egg. In 26.2 "is a food" is the {@code minecraft:food} data component, which covers every
     * former {@code ItemFood} (including the seed-foods carrot and potato); the remaining vanilla seeds and
     * the four explicitly-named non-food items are enumerated, matching {@code MoCEntityGoat.isGoatEdible}.
     *
     * <p>This is only the pick-up filter. It is deliberately NOT wired to {@link #isFood(ItemStack)}:
     * legacy {@code MoCEntityAnt.isMyFavoriteFood} fed nothing but {@code MoCEntityAmbient.followPlayer()},
     * a private method that is never called, so it had no gameplay effect — whereas overriding
     * {@code isFood} here would push a hand-fed ant into vanilla love mode, which legacy ants never had
     * (they cannot breed).</p>
     */
    private static boolean isAntFood(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.has(DataComponents.FOOD)
                    || stack.is(Items.WHEAT) || stack.is(Items.SUGAR)
                    || stack.is(Items.CAKE) || stack.is(Items.EGG)
                    || stack.is(Items.WHEAT_SEEDS) || stack.is(Items.MELON_SEEDS)
                    || stack.is(Items.PUMPKIN_SEEDS) || stack.is(Items.BEETROOT_SEEDS));
    }

    /**
     * Legacy {@code getAIMoveSpeed()}:119-125 — a hard 0.05 while hauling food, 0.15 otherwise, overriding
     * the {@code MOVEMENT_SPEED} attribute entirely. {@code LivingEntity.getSpeed()} is the 26.2 stand-in:
     * the move control writes the attribute-derived speed into it each tick and {@code travel} reads it
     * back, so returning a constant here reproduces the legacy clamp exactly (a laden ant crawls).
     */
    @Override
    public float getSpeed() {
        return getHasFood() ? 0.05F : 0.15F;
    }

    /**
     * Legacy {@code MoCEntityInsect.isOnLadder()}:152-154 returns {@code collidedHorizontally}, i.e. an
     * insect treats <em>anything it walks into</em> as a ladder and crawls up it. {@code onClimbable()} is
     * the 26.2 name for that hook, and {@code horizontalCollision} the 26.2 name for the field.
     */
    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    /**
     * Legacy {@code MoCEntityInsect.jump()}:160-162 is an empty override — an insect never jumps; it climbs
     * (see {@link #onClimbable()}) whenever an obstacle blocks it. Kept faithful: with the climb hook above
     * the ant still clears blocks, it just walks up them rather than hopping.
     */
    @Override
    public void jumpFromGround() {
    }

    /**
     * Legacy {@code MoCEntityInsect.canTriggerWalking()}:164-167 returns false — a 0.2-block insect makes no
     * footstep sounds and triggers nothing by walking. {@code canTriggerWalking} no longer exists in 26.2;
     * its replacement is {@code getMovementEmission()}, and {@code MovementEmission.NONE} is the exact
     * equivalent (no step sounds, and — the 1.13+ addition — no movement game events either, which is what
     * vanilla itself gives {@link ItemEntity} and the other things that should not wake a sculk sensor).
     */
    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    /** Legacy {@code MoCEntityInsect.fall(float, float)}:132-134 is empty: insects take no fall damage. */
    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("ant.png");
    }
}
