package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityGoat}. A passive goat with seven sub-types
 * (kid, three females and three males).
 */
public class MoCEntityGoat extends MoCAnimal {

    public MoCEntityGoat(EntityType<? extends MoCEntityGoat> type, Level level) {
        super(type, level);
    }

    // Legacy MoCEntityGoat overrides fall(float) with an empty body -> goats take ZERO fall damage
    // (they are jumping mountain animals whose jump() gives a large upward impulse). Restore that immunity.
    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D); // headbutt damage for upset males (legacy attackEntity -> 3)
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Billy-goat charge (legacy types 5-7, i.e. type > 4): grown males get upset, then CHARGE and headbutt.
        // registerGoals runs from the constructor BEFORE finalizeSpawn/selectType assigns the sub-type, so
        // getTypeMoC() is still 0 here — we therefore always install the goals but gate each one's canUse on
        // type > 4, so only grown males ever activate them. Females and kids stay passive.
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, true) {
            @Override
            public boolean canUse() {
                return MoCEntityGoat.this.getTypeMoC() > 4 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return MoCEntityGoat.this.getTypeMoC() > 4 && super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return MoCEntityGoat.this.getTypeMoC() > 4 && super.canUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, MoCEntityGoat.class, 10, true, false,
                (living, serverLevel) -> living instanceof MoCEntityGoat goat && goat != this && goat.getTypeMoC() > 4) {
            @Override
            public boolean canUse() {
                return MoCEntityGoat.this.getTypeMoC() > 4 && super.canUse();
            }
        });
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Kid maturation (legacy onLivingUpdate: a type-1 kid past age 70 re-rolls to rand.nextInt(6) + 2 = an
        // adult type 2-7). Without this a kid grows to full size but keeps the kid skin and can never be milked
        // (milking is gated on type). Mirror the ostrich chick-maturation gate (adult OR age >= 100).
        if (getTypeMoC() == 1 && (getIsAdult() || getMoCAge() >= 100)) {
            setTypeMoC(level.getRandom().nextInt(6) + 2);
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        // Legacy MoCEntityGoat.attackEntity: a male headbutting ANOTHER goat is a harmless spar — it
        // calls MoCTools.bigsmack (knockback ONLY, force 0.4) and NEVER entity.attackEntityFrom, so two
        // upset males shove each other around but neither loses HP. Intercept BEFORE super so the 3.0
        // ATTACK_DAMAGE is never applied to a goat victim; only non-goat targets take the real headbutt.
        if (getTypeMoC() > 4 && target instanceof MoCEntityGoat victim) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            double dist = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
            // Lighter shove than a hit on a player (legacy bigsmack force 0.4 vs 0.8), knockback only.
            victim.setDeltaMovement(victim.getDeltaMovement().scale(0.5D)
                    .add(dx / dist * 0.4D, 0.4D, dz / dist * 0.4D));
            victim.hurtMarked = true; // sync the shove impulse to clients
            this.playSound(MoCSounds.GOATSMACK.get(), 1.0F,
                    1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            return true;
        }
        boolean hit = super.doHurtTarget(level, target);
        // On a connecting headbutt against a NON-goat, an upset male shoves the victim away (legacy
        // MoCTools.bigsmack force 0.8) on top of the 3.0 headbutt damage super already applied.
        if (hit && getTypeMoC() > 4 && target instanceof LivingEntity victim) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            double dist = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
            victim.push(dx / dist * 0.8D, 0.35D, dz / dist * 0.8D);
            victim.hurtMarked = true; // sync the shove impulse to clients
        }
        return hit;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Legacy: trying to MILK a MALE goat (type > 4) with an empty bucket gives no milk — the upset
        // male HEADBUTTS the offending player instead (legacy interact: setUpset + target the player).
        // The superclass' milk branch is gated on canBeMilked() (false for males), so without this the
        // right-click silently does nothing. Intercept before super: shove the player back and play the
        // smack sound. Females (types 2-4) still milk and kids/others fall through to super unchanged.
        if (getTypeMoC() > 4 && stack.is(Items.BUCKET)) {
            if (this.level() instanceof ServerLevel) {
                double dx = player.getX() - this.getX();
                double dz = player.getZ() - this.getZ();
                double dist = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
                player.push(dx / dist * 0.8D, 0.35D, dz / dist * 0.8D);
                player.hurtMarked = true; // sync the shove impulse to the client
                this.playSound(MoCSounds.GOATSMACK.get(), 1.0F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
            return InteractionResult.SUCCESS;
        }
        // Legacy goats were tamed/healed with ANY edible (MoCEntityAnimal.isItemEdible = any ItemFood or
        // ItemSeeds, plus wheat/sugar/cake/egg), not just the short data-spec food list. The spec's
        // specific foods (wheat/sugar/cake/egg/wheat-seeds) are already handled by super; here we WIDEN
        // to the FOOD-component items and the vanilla seeds the list omits (bread, apples, carrots,
        // cooked/raw meats, melon/pumpkin/beetroot seeds, ...). Mirrors the base FEED-taming / heal paths.
        if (isGoatEdible(stack)) {
            final boolean server = !this.level().isClientSide();
            // Feed an untamed goat any edible to tame it.
            if (!getIsTamed()) {
                if (server) {
                    // Enforce the tamed-per-player cap; refuse without consuming the food.
                    if (exceedsTameCap(player)) {
                        return InteractionResult.SUCCESS;
                    }
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    setTamed(true);
                    setOwnerName(player.getName().getString());
                    setAdult(true);
                    heal(getMaxHealth());
                    spawnHearts(7);
                }
                return InteractionResult.SUCCESS;
            }
            // Feed a tamed goat any edible: legacy consumed one item and set health = max whenever a
            // tamed goat was fed an edible (even at full health). Swallowing it here also keeps a
            // full-health goat out of vanilla love-mode on the widened (non-spec) food.
            if (server) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                heal(getMaxHealth());
                spawnHearts(4);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Legacy {@code MoCEntityAnimal.isItemEdible}: any {@code ItemFood} or {@code ItemSeeds}, plus wheat,
     * sugar, cake and egg. The wheat/sugar/cake/egg/wheat-seeds specifics are the data-spec goat foods
     * (handled by {@code super.mobInteract}); this widens to the FOOD-component items and the vanilla
     * seeds the short spec list omits, so a goat can be tamed/healed with bread, an apple, cooked meat, etc.
     */
    private static boolean isGoatEdible(ItemStack stack) {
        return stack.has(DataComponents.FOOD)
                || stack.is(Items.WHEAT_SEEDS) || stack.is(Items.MELON_SEEDS)
                || stack.is(Items.PUMPKIN_SEEDS) || stack.is(Items.BEETROOT_SEEDS);
    }

    /** Heart particles as feedback on taming/feeding (mirrors the base MoCAnimal taming/heal path). */
    private void spawnHearts(int count) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() * 0.5D, getZ(),
                    count, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 15) {
                setTypeMoC(1);
                setAdult(false); // a naturally-spawned kid must grow up before it re-rolls to an adult type
            } else if (i <= 30) {
                setTypeMoC(2);
            } else if (i <= 45) {
                setTypeMoC(3);
            } else if (i <= 60) {
                setTypeMoC(4);
            } else if (i <= 75) {
                setTypeMoC(5);
            } else if (i <= 90) {
                setTypeMoC(6);
            } else {
                setTypeMoC(7);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("goat2.png");
            case 3 -> modelTexture("goat3.png");
            case 4 -> modelTexture("goat4.png");
            case 5 -> modelTexture("goat5.png");
            case 6 -> modelTexture("goat6.png");
            default -> modelTexture("goat1.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.GOATGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.GOATHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.GOATDYING.get();
    }

    /**
     * Legacy goats treated ANY edible item as food ({@code isItemEdible}) rather than the short species food list, so
     * widen the food test to also accept anything carrying a FOOD component (in addition to the specific foods).
     */
    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        return super.isFood(stack) || stack.has(net.minecraft.core.component.DataComponents.FOOD);
    }
}
