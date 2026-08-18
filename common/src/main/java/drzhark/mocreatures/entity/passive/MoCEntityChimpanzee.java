package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.network.MoCNetwork;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The Chimpanzee — one of the Mo'Creatures mobs that were designed but never released upstream, so unlike
 * its neighbours in this package it has no legacy 1.12.2 source to port. It is built deliberately in the
 * {@link MoCEntityRaccoon} mold, as a gentle forest/jungle forager:
 *
 * <ul>
 *   <li><b>Tamed by hand-feeding anything edible</b> — the same widened {@code MoCTools.isItemEdible}
 *       facility the raccoon reproduces (any FOOD-component item, any of the four vanilla crop seeds,
 *       plus wheat / sugar / cake / egg). Feeding also fully heals, and nudges a youngster one step
 *       along its growth curve, exactly as the raccoon's feed path does.</li>
 *   <li><b>Breedable</b>, unlike the raccoon: {@code MoCBehavior}'s {@code breed()} flag installs the
 *       shared {@code BreedGoal}, and {@link #mobInteract} lets a fed, healthy, adult tamed chimpanzee
 *       fall through to {@code Animal.mobInteract}'s love-mode branch instead of swallowing the food as
 *       a no-op the way the (non-breeding) raccoon does. {@link MoCAnimal#getBreedOffspring} then builds
 *       the baby with {@code setAdult(false)} at newborn age.</li>
 *   <li><b>No hunting, stealing or retaliation goals.</b> The chimpanzee registers nothing of its own:
 *       its AI is exactly {@link MoCAnimal}'s defaults (float, sit, panic, breed, follow-owner, stroll,
 *       look) driven by the {@code reg("chimpanzee")} behaviour spec. The raccoon's hunt latch, kit-flee
 *       and hurt-by goals are deliberately NOT copied, and the raccoon has no TemptGoal so neither does
 *       the chimp. Not rideable, not milkable.</li>
 *   <li><b>Growth</b> mirrors the raccoon's curve verbatim (spec: spawn age 50-64, 1 wild spawn in 3 a
 *       youngster, growing on a rand(300) tick to adult at age 100).</li>
 * </ul>
 */
public class MoCEntityChimpanzee extends MoCAnimal {

    public MoCEntityChimpanzee(EntityType<? extends MoCEntityChimpanzee> type, Level level) {
        super(type, level);
    }

    /**
     * A mid-sized forager: 15 health (7.5 hearts), the brisk 0.3 movement speed the raccoon has, and a
     * nominal 2 attack damage. With no melee or target goals of its own the damage attribute is inert in
     * normal play, but it is registered so the stat exists if a future goal (or another mod) swings it.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    /**
     * The raccoon-style any-edible feed flow, adjusted for a species that CAN breed.
     *
     * <p>Hand an untamed chimpanzee any edible item and it is consumed, the chimp is tamed and named,
     * healed to full, and — if still young — nudged one step along its growth curve. A tamed chimp that
     * is hurt or still growing eats the same way (heal + growth nudge). But a tamed, healthy ADULT does
     * <em>not</em> swallow the food as a no-op the way {@link MoCEntityRaccoon#mobInteract} does: it
     * falls through to {@code Animal.mobInteract}, whose love-mode branch (mc262-ref Animal.java:139-148,
     * gated on {@code isFood} / {@code getAge() == 0} / {@code canFallInLove}) consumes the item and
     * starts breeding — that fall-through is the entire difference from the raccoon, and the reason the
     * chimpanzee pair you fed actually produces a baby.</p>
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isChimpanzeeEdible(stack)
                && (!getIsTamed() || getHealth() < getMaxHealth() || !getIsAdult())) {
            if (!this.level().isClientSide()) {
                if (!getIsTamed()) {
                    // Enforce the tamed-per-player cap; refuse without consuming the food.
                    if (exceedsTameCap(player)) {
                        return InteractionResult.SUCCESS;
                    }
                    setTamed(true);
                    setOwnerName(player.getName().getString());
                    // Legacy tameWithName prompted for a name the instant a creature was tamed.
                    MoCNetwork.promptName(this, player);
                    spawnHearts(7);
                } else {
                    spawnHearts(4);
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                heal(getMaxHealth());
                // Feeding nudges a youngster along its growth curve; it does NOT snap it to adulthood
                // (same rule as the raccoon's feed path).
                if (!getIsAdult() && getMoCAge() < 100) {
                    setMoCAge(getMoCAge() + 1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * The widened "anything edible" test, identical to {@code MoCEntityRaccoon.isRaccoonEdible} (itself a
     * port of legacy {@code MoCTools.isItemEdible}): any FOOD-component item, the four vanilla crop seeds
     * (legacy {@code ItemSeeds} no longer exists as a class), plus wheat / sugar / cake / egg.
     */
    private static boolean isChimpanzeeEdible(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.has(DataComponents.FOOD)
                || stack.is(Items.WHEAT_SEEDS) || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS) || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.WHEAT) || stack.is(Items.SUGAR) || stack.is(Items.CAKE) || stack.is(Items.EGG);
    }

    /**
     * Widen the vanilla food test to match {@link #isChimpanzeeEdible}, so the love-mode branch in
     * {@code Animal.mobInteract} (and anything else that asks) agrees with the feeding interaction above.
     */
    @Override
    public boolean isFood(ItemStack stack) {
        return isChimpanzeeEdible(stack);
    }

    /** Heart particles as feedback on taming/feeding (mirrors the base MoCAnimal taming/heal path). */
    private void spawnHearts(int count) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    count, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }

    @Override
    public Identifier getTexture() {
        // One coat, no sub-types (selectType() inherits MoCAnimal's default of type 1).
        return modelTexture("chimpanzee.png");
    }

    // ------------------------------------------------------------------------------------- sounds
    //
    // PLACEHOLDER AUDIO: the mod ships no chimpanzee sounds (the species never got upstream assets —
    // precedent: the Ent ships silent as a known gap). The vanilla panda's ambient/hurt/death set,
    // pitched up ~1.4x via getVoicePitch below, is a serviceable chatter stand-in until real chimp
    // audio exists.

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.PANDA_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PANDA_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return SoundEvents.PANDA_DEATH;
    }

    /**
     * Pitch the borrowed panda audio up so it reads as chimp chatter rather than a bear. Multiplying
     * {@code super.getVoicePitch()} (mc262-ref LivingEntity.java:2310-2314) keeps the vanilla per-call
     * jitter and the extra-high baby voice.
     */
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.4F;
    }
}
