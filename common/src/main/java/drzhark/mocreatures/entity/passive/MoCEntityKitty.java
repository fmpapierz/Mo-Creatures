package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.entity.MoCBehavior;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityKitty}. A small tameable cat with eight colour variants.
 *
 * <p>A tamed kitty carries a small mood/state machine: it grows hungry over time (begging with its
 * ambient meow), naps at night, and plays when a wool ball is nearby. It also uses its furniture, just
 * like the legacy mod: a sleepy kitty walks to a nearby {@link MoCEntityKittyBed} and curls up in it,
 * and a kitty that "needs to go" seeks out a clean {@link MoCEntityLitterBox} and dirties it. The state
 * is synched so the renderer (and any future animation) can react to it. Wild (untamed) kitties stay
 * {@link #STATE_CALM}.
 */
public class MoCEntityKitty extends MoCAnimal {

    /** Content, wandering: the default mood. */
    public static final int STATE_CALM = 0;
    /** Has not been fed in a while: begs with its meow. */
    public static final int STATE_HUNGRY = 1;
    /** A wool ball is nearby (or the play goal is chasing one). */
    public static final int STATE_PLAYING = 2;
    /** Napping at night. */
    public static final int STATE_SLEEPING = 3;
    /**
     * Dangling upside down at the carrier's side, picked up by a lead click (legacy kittyState 14,
     * {@code MoCEntityKitty}:575-579). Keeps the legacy number; no collision with the mood states 0-3.
     */
    public static final int STATE_HELD_UPSIDE_DOWN = 14;
    /** Lying sideways on the carrier's shoulder, picked up empty-handed (legacy kittyState 15, :586-590). */
    public static final int STATE_ON_SHOULDER = 15;

    /** Ticks of contentment before a tamed kitty grows hungry (~2.5 min at 20 tps). */
    private static final int HUNGER_THRESHOLD = 3000;
    /** Ticks before a tamed kitty needs to relieve itself in a litter box (~3.5 min at 20 tps). */
    private static final int LITTER_THRESHOLD = 4000;
    /** How far a kitty will look for a bed or litter box, in blocks. */
    private static final double FURNITURE_RANGE = 12.0D;

    private static final EntityDataAccessor<Integer> KITTY_STATE =
            SynchedEntityData.defineId(MoCEntityKitty.class, EntityDataSerializers.INT);

    /** Server-side hunger accumulator; persisted so a reloaded kitty does not forget it is peckish. */
    private int hungerCounter;
    /** Server-side "needs the litter box" accumulator; persisted like hunger. */
    private int litterCounter;
    /** True once a tamed kitty has decided to nap for the night; cleared at dawn or when startled. */
    private boolean wantsToSleep;
    /** Cached furniture targets so the kitty walks to one steadily instead of re-scanning every tick. */
    @Nullable private MoCEntityKittyBed targetBed;
    @Nullable private MoCEntityLitterBox targetLitter;

    public MoCEntityKitty(EntityType<? extends MoCEntityKitty> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Legacy kitties do NOT flee when merely struck — they get 'upset' (state 13) and scratch back.
        // Strip the reactive PanicGoal MoCAnimal installs for animals so a struck kitty stands its ground
        // and retaliates (same approach the ported fox uses).
        this.goalSelector.removeAllGoals(g -> g instanceof PanicGoal);
        // Proactive avoidance instead: a kitty runs from scary monsters on sight (legacy getBoogey / runLikeHell).
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 6.0F, 1.4D, 1.6D));
        // A well-fed adult kitty looks for a mate and breeds into a litter of kittens. The data spec carries
        // no .breed(), so install the vanilla BreedGoal here — guarded so it is not double-installed if the
        // spec ever gains .breed() (in which case MoCAnimal.registerGoals already added it).
        if (!behavior().canBreed) {
            this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
        }
        // An 'upset' (struck) kitty scratches its attacker for 1 (ATTACK_DAMAGE); any hungry kitty (tamed or
        // wild) hunts small prey. Legacy attackEntityFrom set entityToAttack = attacker, and findPlayerToAttack
        // hunted small creatures (width/height <= 0.5, not players/monsters/kitties) within 10 blocks while
        // hungry — with no tamed check, so a tamed kitty that grew hungry hunted too.
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3D, true));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false,
                // Legacy findPlayerToAttack only hunted small prey when worldObj.difficultySetting > 0
                // (i.e. NOT Peaceful); on Peaceful a hungry kitty attacked nothing. Legacy getClosestTarget
                // also skipped every prey candidate while enableHunters was off (MoCEntityKitty:305), so the
                // predation predicate live-reads that master switch too — retaliation (HurtByTargetGoal
                // above) is creature-defence, not predation, and stays ungated.
                (living, serverLevel) -> this.isHungryMoC()
                        && serverLevel.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                        && drzhark.mocreatures.config.MoCConfig.get().enableHunters
                        && !(living instanceof Player)
                        && !(living instanceof Monster)
                        && !(living instanceof MoCEntityKitty)
                        && !(living instanceof MoCEntityKittyBed)
                        && !(living instanceof MoCEntityLitterBox)
                        && (living.getBbWidth() <= 0.5F || living.getBbHeight() <= 0.5F)));
        // Wool-ball play (unchanged from the base port kitty).
        this.goalSelector.addGoal(4, new drzhark.mocreatures.entity.MoCKittyPlayGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                // Legacy kitty dealt 1 damage per swing (attackEntityFrom(causeMobDamage, 1)).
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    // ------------------------------------------------------------------------- synched kitty state

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KITTY_STATE, STATE_CALM);
    }

    public int getKittyState() {
        return this.entityData.get(KITTY_STATE);
    }

    public void setKittyState(int state) {
        this.entityData.set(KITTY_STATE, state);
    }

    /**
     * True once this kitty has gone unfed long enough to be hungry (legacy {@code getIsHungry}). Drives a
     * wild kitty's small-prey hunting (see {@link #registerGoals}); a tamed kitty's hunger is cleared by
     * feeding in {@link #mobInteract}.
     */
    public boolean isHungryMoC() {
        return hungerCounter > HUNGER_THRESHOLD;
    }

    // --------------------------------------------------------------------------------- mood machine

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // ---- hunger accumulates every tick for EVERY kitty until it is fed (see mobInteract) ----
        // A tamed kitty begs (and is calmed by feeding); a WILD kitty that has gone hungry hunts small prey
        // (legacy findPlayerToAttack only returned prey while getIsHungry()). See the target goal in registerGoals.
        if (hungerCounter < Integer.MAX_VALUE) {
            hungerCounter++;
        }

        // Wild kitties have no furniture moods — they stay calm; their hunting is driven by the target goal.
        if (!getIsTamed()) {
            if (getKittyState() != STATE_CALM) {
                setKittyState(STATE_CALM);
            }
            seekDroppedFish(level);
            return;
        }

        // While carried, the pose IS the state (STATE_HELD_UPSIDE_DOWN / STATE_ON_SHOULDER): the mood
        // machine below must not overwrite it with a mood every tick. Hunger keeps accruing above and
        // matters again once the cat is set down (putDown restores STATE_CALM).
        if (isBeingCarried()) {
            return;
        }

        final boolean hungry = hungerCounter > HUNGER_THRESHOLD;
        if (hungry && getRandom().nextInt(200) == 0) {
            // Beg with the ambient meow so a nearby owner notices.
            SoundEvent beg = getAmbientSound();
            if (beg != null) {
                this.playSound(beg, 1.0F, getRandom().nextFloat() * 0.2F + 0.9F);
            }
        }

        // ---- the "needs the litter box" urge builds over time; using a box resets it ----
        if (litterCounter < Integer.MAX_VALUE) {
            litterCounter++;
        }
        final boolean needsLitter = litterCounter > LITTER_THRESHOLD;

        // ---- decide, once, whether the kitty wants to sleep this night ----
        updateSleepIntent(level);

        // ---- decide this tick's mood: HUNGRY > litter box > SLEEPING > PLAYING > CALM ----
        int newState;
        if (hungry) {
            newState = STATE_HUNGRY;
        } else if (needsLitter && useLitterBox(level)) {
            // Heading to / using a litter box; render as calm.
            newState = STATE_CALM;
        } else if (wantsToSleep || (getHealth() < getMaxHealth()
                && nearest(level, MoCEntityKittyBed.class, b -> b.getHasMilk() || b.getHasFood()) != null)) {
            // Curl up in a bed if one is reachable; otherwise nap where we stand. While still walking
            // to a distant bed we read as calm so the sleeping pose only shows once settled. Legacy state 3
            // (seek a milk/food-stocked bed to heal to full) was reachable day OR night — so a hurt tamed
            // kitty also walks to a stocked bed in daylight, not only when the night-sleep flag is set.
            newState = goToBedOrSleep(level) ? STATE_SLEEPING : STATE_CALM;
        } else if (isPlaying(level)) {
            newState = STATE_PLAYING;
        } else {
            newState = STATE_CALM;
        }

        // Leaving the sleeping state: stand back up, climb out of the bed, and forget it.
        if (getKittyState() == STATE_SLEEPING && newState != STATE_SLEEPING && isSitting()) {
            setSitting(false);
            if (this.getVehicle() instanceof MoCEntityKittyBed) {
                this.stopRiding();
            }
            targetBed = null;
        }

        if (getKittyState() != newState) {
            // Legacy states 12/18 (content/sleeping) emitted 'kittypurr'; purr as the kitty settles to sleep.
            if (newState == STATE_SLEEPING) {
                this.playSound(MoCSounds.KITTYPURR.get(), 1.0F, getRandom().nextFloat() * 0.2F + 0.9F);
            }
            setKittyState(newState);
        }
    }

    /**
     * A calm, tamed kitty has a small chance to decide to nap once night falls, and keeps that intent
     * until day (or until something wakes it — see {@link #hurtServer}). Holding the decision in a flag
     * (rather than re-rolling every tick) lets the kitty commit to walking all the way to its bed.
     */
    private void updateSleepIntent(ServerLevel level) {
        if (level.isBrightOutside()) {
            wantsToSleep = false; // daytime: always awake.
        } else if (!wantsToSleep && getKittyState() == STATE_CALM && getRandom().nextInt(400) == 0) {
            wantsToSleep = true; // night, content: drift off.
        }
    }

    /** Legacy stage-1 taming: a kitty only accepts a Medallion once it has eaten a dropped cooked fish. */
    @Override
    protected boolean requiresFeedingBeforeTaming() {
        return true;
    }

    /**
     * Legacy stage 1 of kitty taming ({@code MoCEntityKitty}:789-820). A hungry WILD kitty looks for a
     * cooked fish dropped on the ground within 10 blocks, pads over to it and eats it — only then will it
     * accept a Medallion. Without this a wild kitty is tamed by a single medallion click, skipping the
     * "win it over with food first" half of the mechanic that the big cat still has.
     */
    private void seekDroppedFish(ServerLevel level) {
        if (getHasEatenMoC() || hungerCounter <= HUNGER_THRESHOLD || getRandom().nextInt(10) != 0) {
            return;
        }
        net.minecraft.world.entity.item.ItemEntity nearestFish = null;
        double best = Double.MAX_VALUE;
        for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class, getBoundingBox().inflate(10.0D),
                e -> e.isAlive() && (e.getItem().is(net.minecraft.world.item.Items.COOKED_COD)
                        || e.getItem().is(net.minecraft.world.item.Items.COOKED_SALMON)))) {
            double d = item.distanceToSqr(this);
            if (d < best) {
                best = d;
                nearestFish = item;
            }
        }
        if (nearestFish == null) {
            return;
        }
        if (best >= 4.0D) {
            getNavigation().moveTo(nearestFish, 1.0D);
            return;
        }
        nearestFish.getItem().shrink(1);
        if (nearestFish.getItem().isEmpty()) {
            nearestFish.discard();
        }
        setHasEatenMoC(true);
        hungerCounter = 0;
        setHealth(getMaxHealth());
        level.playSound(null, blockPosition(), MoCSounds.EATING.get(),
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                1.0F + (getRandom().nextFloat() - getRandom().nextFloat()) * 0.2F);
    }

    /** Legacy kittens are born at edad 40 (MoCEntityKitty ctor) and grow to adult at 100. */
    @Override
    protected int newbornAge() {
        return 40;
    }

    /**
     * Walk to the nearest kitty bed and curl up in it; if none is nearby, just nap on the spot.
     *
     * @return {@code true} once the kitty is actually settled (sitting), {@code false} while it is
     *         still travelling to a distant bed.
     */
    private boolean goToBedOrSleep(ServerLevel level) {
        if (targetBed != null && !targetBed.isAlive()) {
            targetBed = null;
        }
        if (targetBed == null && getRandom().nextInt(20) == 0) {
            targetBed = nearest(level, MoCEntityKittyBed.class, b -> true);
        }
        if (targetBed != null && distanceToSqr(targetBed) >= 1.5D) {
            // Still heading to the bed — stay on our feet and keep pathing.
            if (isSitting()) {
                setSitting(false);
            }
            getNavigation().moveTo(targetBed, 1.0D);
            return false;
        }
        if (targetBed != null) {
            // Legacy kitty state 3->4: on reaching a bed stocked with milk or pet food and not already occupied,
            // the kitty CLIMBS IN (becomes the bed's passenger) — that is what triggers the bed's heal-to-full and
            // supply drain. An empty or occupied bed is just curled up on top of instead.
            if (this.getVehicle() != targetBed && targetBed.getPassengers().isEmpty()
                    && (targetBed.getHasMilk() || targetBed.getHasFood())) {
                this.startRiding(targetBed);
                // Legacy state 4 played a contextual feeding sound while nestled: 'kittyeatingm' drinking
                // from a milk-stocked bed, 'kittyeatingf' eating from a food-stocked bed.
                this.playSound((targetBed.getHasMilk() ? MoCSounds.KITTYEATINGM : MoCSounds.KITTYEATINGF).get(),
                        1.0F, 1.0F + (getRandom().nextFloat() - getRandom().nextFloat()) * 0.2F);
            } else if (!this.isPassenger()) {
                this.setPos(targetBed.getX(), targetBed.getY(), targetBed.getZ());
            }
        }
        if (!isSitting()) {
            setSitting(true);
        }
        getNavigation().stop();
        return true;
    }

    /**
     * Walk to the nearest clean litter box and dirty it; the urge then resets.
     *
     * @return {@code true} if the kitty has a litter box to head to (so this behaviour owns its
     *         movement this tick), {@code false} if there is no clean box within range.
     */
    private boolean useLitterBox(ServerLevel level) {
        if (targetLitter != null && (!targetLitter.isAlive() || targetLitter.getUsed())) {
            targetLitter = null;
        }
        if (targetLitter == null && getRandom().nextInt(20) == 0) {
            targetLitter = nearest(level, MoCEntityLitterBox.class, box -> !box.getUsed());
        }
        if (targetLitter == null) {
            return false;
        }
        if (distanceToSqr(targetLitter) < 2.25D) {
            targetLitter.setUsed(true);
            // Legacy played 'kittypoo' on a litter deposit; that clip isn't registered in the port, so use
            // the nearest shipped kitty clip (KITTYFOOD) as the contextual litter-use cue.
            this.playSound(MoCSounds.KITTYFOOD.get(), 1.0F,
                    1.0F + (getRandom().nextFloat() - getRandom().nextFloat()) * 0.2F);
            litterCounter = 0;
            targetLitter = null;
            getNavigation().stop();
        } else {
            getNavigation().moveTo(targetLitter, 1.0D);
        }
        return true;
    }

    /** Nearest matching furniture entity of the given type within {@link #FURNITURE_RANGE}, or null. */
    @Nullable
    private <T extends net.minecraft.world.entity.Entity> T nearest(
            ServerLevel level, Class<T> type, java.util.function.Predicate<T> filter) {
        return level.getEntitiesOfClass(type, getBoundingBox().inflate(FURNITURE_RANGE),
                        e -> e.isAlive() && filter.test(e)).stream()
                .min(java.util.Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
    }

    /**
     * The kitty is "playing" whenever a wool ball is within a few blocks — the same trigger the
     * {@link drzhark.mocreatures.entity.MoCKittyPlayGoal} uses to actually chase it. Guarded behind a
     * random gate so the entity scan only runs a few times per second.
     */
    private boolean isPlaying(ServerLevel level) {
        // If we are already flagged as playing, keep re-checking every tick so the state clears
        // promptly once the ball is gone; otherwise only probe occasionally to keep it cheap.
        if (getKittyState() != STATE_PLAYING && getRandom().nextInt(20) != 0) {
            return getKittyState() == STATE_PLAYING;
        }
        return !level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(6.0D),
                e -> e.isAlive() && e.getItem().is(MoCItems.WOOLBALL.get())).isEmpty();
    }

    // ------------------------------------------------------------------------------- interaction

    /**
     * The lead-carry pick-up (legacy :575-579) must intercept the click ABOVE {@code mobInteract}:
     * {@code Mob.interact} (mc262-ref Mob.java:1128-1137) runs {@code Entity.interact} before it ever
     * reaches {@code mobInteract}, and the {@code Leashable} branch there (Entity.java:2306-2321)
     * consumes ANY lead click on a leashable mob — it tied a leash to the kitty, shrank the lead, and
     * returned before the carry branch could run, so the "lead = hang upside down" mechanic never fired.
     *
     * <p>A sneaking player falls through to vanilla on purpose — sneaking is the carry system's put-down
     * trigger (a sneak pick-up would be released the next tick), and {@code Entity.interact}'s sneak
     * branch re-ties leashes held by the player. So does an already-leashed kitty, so the same click can
     * untie it. The lead is NOT consumed on pick-up — legacy never shrank the stack.</p>
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand, net.minecraft.world.phys.Vec3 location) {
        if (player.getItemInHand(hand).is(net.minecraft.world.item.Items.LEAD)
                && getIsTamed() && !isLeashed() && !player.isSecondaryUseActive()
                && canBeHandledBy(player)) {
            if (!isBeingCarried()) {
                if (!this.level().isClientSide() && toggleCarry(player, false)) {
                    setKittyState(STATE_HELD_UPSIDE_DOWN);
                    // A napping kitty has the synched sitting flag set; left set, the model would fold
                    // the limbs into the curled loaf pose while it dangles.
                    setSitting(false);
                }
                return InteractionResult.SUCCESS;
            }
            if (getCarrier() == player) {
                // Clicking the kitty you carry — with anything, the lead included — sets it back down.
                if (!this.level().isClientSide()) {
                    toggleCarry(player, false); // putDown() -> the putDown override restores STATE_CALM
                }
                return InteractionResult.SUCCESS;
            }
            // Someone else is carrying it: refuse, or Entity.interact would leash it off their head.
            return InteractionResult.PASS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // The legacy carry states (MoCEntityKitty:575-598), wired onto the port's carry system
        // (MoCAnimal.toggleCarry — the pet is pinned to the carrier, never a vanilla passenger).
        // The LEAD pick-up branch lives in interact() above (it must beat vanilla leashing).
        // Clicking a kitty you are carrying — with anything — sets it back down (legacy :592-598).
        if (getIsTamed() && isBeingCarried() && getCarrier() == player) {
            if (!this.level().isClientSide()) {
                toggleCarry(player, false); // putDown() -> the putDown override restores STATE_CALM
            }
            return InteractionResult.SUCCESS;
        }
        // An empty hand drapes a tamed kitty sideways over your shoulder (legacy :586-590). Nothing is
        // shadowed by claiming the empty-hand click: the kitty spec has no milk/ride branches in
        // MoCAnimal.mobInteract, so it previously fell through to vanilla Animal and did nothing.
        if (getIsTamed() && !isBeingCarried() && stack.isEmpty()) {
            if (!this.level().isClientSide() && toggleCarry(player, false)) {
                setKittyState(STATE_ON_SHOULDER);
                setSitting(false); // see the lead branch in interact(): no stale curled pose while carried
            }
            return InteractionResult.SUCCESS;
        }
        // A tamed kitty eats its food (cod / cooked cod / cake) itself. Handled BEFORE super and returning
        // SUCCESS so the item is consumed by the cat (satisfying hunger + healing) rather than eaten by the
        // player — right-clicking edible food on an entity otherwise falls through to the player's own eat.
        // A well-fed adult kitty then looks for a mate (legacy fed a content kitty into mate-search state 9),
        // so it enters love mode and the BreedGoal pairs two fed kitties into a litter of kittens.
        if (getIsTamed() && MoCBehavior.matches(behavior().healOrFood(), stack)) {
            if (!this.level().isClientSide()) {
                hungerCounter = 0;
                setKittyState(STATE_CALM);
                heal(getMaxHealth());
                if (getIsAdult() && canFallInLove()) {
                    setInLove(player);
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Every release path must clear the carry pose (legacy {@code changeKittyState(7)}, :592-598) — not
     * just the click-again branch in {@link #mobInteract} but also the sneak-release and carrier-offline
     * paths in {@code MoCAnimal.tickCarried}, or the cat would keep rendering upside down on the ground.
     */
    @Override
    public void putDown(@Nullable Player carrier) {
        super.putDown(carrier);
        setKittyState(STATE_CALM);
    }

    /**
     * Safety net: a carry pose without a carrier (e.g. the pose saved mid-carry and reloaded before the
     * carrier reappears) must not leave a grounded cat frozen in state 14/15 — those states are only
     * meaningful while actually carried.
     */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && !isBeingCarried()
                && (getKittyState() == STATE_HELD_UPSIDE_DOWN || getKittyState() == STATE_ON_SHOULDER)) {
            setKittyState(STATE_CALM);
        }
        // While dangling from a lead (state 14), stamp the carrier into the CLIENT-side registry the
        // player-model mixins read to still the carrying arm (see MoCLeadCarriers — entries expire on
        // their own within 2 ticks, so there is no unmark path to forget). Client branch only: on a
        // dedicated server the registry stays empty, and the class itself is plain Java (no client
        // imports), so referencing it from common entity code is loader-safe.
        if (this.level().isClientSide() && isBeingCarried() && getKittyState() == STATE_HELD_UPSIDE_DOWN
                && getCarrier() instanceof Player carrier) {
            drzhark.mocreatures.client.MoCLeadCarriers.mark(carrier.getId(), this.level().getGameTime());
        }
    }

    /**
     * Both carry poses hang at the carrier's SIDE (dangling from the lead hand / lying on the shoulder),
     * so they track the carrier's BODY yaw, not the look yaw the head-carried species use — panning the
     * camera in third person must not orbit the cat around the player. See
     * {@link MoCAnimal#carriedYaw} for why {@code yBodyRot} is the stable "which way the body points"
     * value on both sides.
     */
    @Override
    protected float carriedYaw(Player carrier) {
        if (getKittyState() == STATE_HELD_UPSIDE_DOWN || getKittyState() == STATE_ON_SHOULDER) {
            return carrier.yBodyRot;
        }
        return super.carriedYaw(carrier);
    }

    /**
     * Pin height for the two carry poses. Legacy rode the kitty as a real passenger at the player's mount
     * point — {@code posY + getMountedYOffset()} = feet + 1.8*0.75 = +1.35 — and its {@code getYOffset}
     * observer branch (:491-501) nudged that by −0.1 upside-down / +0.1 on-shoulder, so the render origin
     * sat at feet+1.25 / feet+1.45. With the carrier 1.8 tall, that is a sink of 0.55 / 0.35 below the
     * head top. The renderer's legacy pose transforms then hang the state-14 cat down from that origin
     * (chest height at the carrier's side) and lay the state-15 cat on top of it (the shoulder).
     */
    @Override
    public double carriedHeadSink() {
        return getKittyState() == STATE_HELD_UPSIDE_DOWN ? 0.55D : 0.35D;
    }

    /**
     * Kitties are never put into love mode by a plain hand-feed via vanilla's {@code isFood} path; only a
     * TAMED kitty fed its heal food (see {@link #mobInteract}) looks for a mate. Returning {@code false}
     * keeps vanilla from love-breeding an untamed wild kitty on cod — legacy wild kitties could not be bred.
     */
    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    /**
     * Legacy {@code MoCEntityKitty} gave birth to {@code rand.nextInt(3) + 1} kittens in one litter. Vanilla
     * breeding adds the single returned baby; spawn the extra 0-2 here so a mating yields a faithful 1-3
     * kitten litter, each with the legacy {@code mob.chickenplop} birth sound.
     */
    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        int extras = getRandom().nextInt(3); // 0-2 extra kittens on top of the one vanilla adds -> 1-3 total
        for (int i = 0; i < extras; i++) {
            if (super.getBreedOffspring(level, partner) instanceof MoCEntityKitty kitten) {
                kitten.snapTo(getX(), getY(), getZ(), getYRot(), 0.0F);
                level.addFreshEntity(kitten);
                this.playSound(SoundEvents.CHICKEN_EGG, 1.0F,
                        (getRandom().nextFloat() - getRandom().nextFloat()) * 0.2F + 1.0F);
            }
        }
        return super.getBreedOffspring(level, partner);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && getKittyState() == STATE_SLEEPING) {
            // Startle awake — and give up on napping for now.
            setKittyState(STATE_CALM);
            setSitting(false);
            wantsToSleep = false;
            targetBed = null;
        }
        return hurt;
    }

    // ------------------------------------------------------------------------------- persistence

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("KittyState", getKittyState());
        output.putInt("KittyHunger", hungerCounter);
        output.putInt("KittyLitter", litterCounter);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setKittyState(input.getIntOr("KittyState", STATE_CALM));
        hungerCounter = input.getIntOr("KittyHunger", 0);
        litterCounter = input.getIntOr("KittyLitter", 0);
    }

    // ------------------------------------------------------------------------------------ visuals

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 15) {
                setTypeMoC(1);
            } else if (i <= 30) {
                setTypeMoC(2);
            } else if (i <= 45) {
                setTypeMoC(3);
            } else if (i <= 60) {
                setTypeMoC(4);
            } else if (i <= 70) {
                setTypeMoC(5);
            } else if (i <= 80) {
                setTypeMoC(6);
            } else if (i <= 90) {
                setTypeMoC(7);
            } else {
                setTypeMoC(8);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("pussycatb.png");
            case 3 -> modelTexture("pussycatc.png");
            case 4 -> modelTexture("pussycatd.png");
            case 5 -> modelTexture("pussycate.png");
            case 6 -> modelTexture("pussycatf.png");
            case 7 -> modelTexture("pussycatg.png");
            case 8 -> modelTexture("pussycath.png");
            default -> modelTexture("pussycata.png");
        };
    }

    /**
     * The mood emoticon icon shown above a targeted, owned kitty when {@code displayPetIcons} is on
     * (legacy MoCRenderKitty's floating emoticon face). The legacy mod had ~19 emote states; the port
     * kitty only tracks four real states, so this maps those four to the matching shipped emoticon
     * faces (all under {@code textures/misc/}).
     */
    public Identifier getEmoticonTexture() {
        String tex = switch (getKittyState()) {
            case STATE_HUNGRY -> "emoticon3.png";   // begging for food
            case STATE_PLAYING -> "emoticon11.png"; // wants to play
            case STATE_SLEEPING -> "emoticon5.png"; // sleepy
            default -> "emoticon1.png";             // calm / neutral
        };
        return Identifier.fromNamespaceAndPath(drzhark.mocreatures.MoCreatures.MOD_ID, "textures/misc/" + tex);
    }

    // Legacy MoCEntityKitty branched its sounds on the kitten state (getKittyState()==10, a non-adult
    // baby): kittens grunted/hurt/died with the "kitten*" variants, grown cats with the "kitty*" ones.
    // The port models the baby state as !getIsAdult(), so branch on that.
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return (getIsAdult() ? MoCSounds.KITTYGRUNT : MoCSounds.KITTENGRUNT).get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return (getIsAdult() ? MoCSounds.KITTYHURT : MoCSounds.KITTENHURT).get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return (getIsAdult() ? MoCSounds.KITTYDYING : MoCSounds.KITTENDYING).get();
    }
}
