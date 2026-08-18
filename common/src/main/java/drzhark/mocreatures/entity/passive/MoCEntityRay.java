package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAquatic;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Port of the legacy {@code MoCEntityRay}. An aquatic ray with mantray / stingray variants.
 *
 * <p>Type 1 is the harmless, rideable mantaray; type 2 is the stingray, which passively poisons nearby
 * swimming players every ~250 ticks and retaliates when struck (both faithful to DrZhark's 1.12.2 mod).
 */
public class MoCEntityRay extends MoCAquatic {

    public MoCEntityRay(EntityType<? extends MoCEntityRay> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // Legacy MoCEntityRay declares no attack value: it never overrides attackEntity(), so a struck
        // stingray deals no contact/melee damage - it only chases and applies its passive poison touch.
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Stingrays chase whoever hurt them (legacy set entityToAttack on the damage source) but deal NO
        // melee bite - the legacy ray has no attackEntity(), only its passive poison. So install a
        // hurt-target goal (the chase) without any MeleeAttackGoal. Mantarays never acquire a hurt-target
        // because it is cleared in customServerAiStep, so this stays inert for type 1.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        // Legacy MoCEntityRay.interact only mounts the player when getType() == 1 (mantaray); right-clicking a
        // stingray (type 2) fell into the else branch (mountEntity(null)) and did nothing useful. The port's
        // MoCAquatic registers "ray" as rideable for the whole species, so gate the empty-hand ride branch here:
        // only the mantaray is rideable, every other ray ignores an empty-hand right-click.
        if (getTypeMoC() != 1 && player.getItemInHand(hand).isEmpty()) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            // Legacy getCanSpawnHere was biome-based, not a flat 50/50: mantarays (type 1) spawned in oceans,
            // while any non-ocean biome (river/swamp/beach) forced the stingray (type 2). Reproduce that split
            // from the biome at the spawn position: ocean -> mantaray, everything else -> stingray.
            if (this.level().getBiome(this.blockPosition()).is(BiomeTags.IS_OCEAN)) {
                setTypeMoC(1); // mantaray
            } else {
                setTypeMoC(2); // stingray
            }
        }
        // Spawn ages (legacy ctors): a mantaray starts at edad 80-179 (MoCEntityMantaRay:13), a stingray at
        // 50-99, both as non-adults that grow in customServerAiStep. Rolled only while the age still sits at
        // the shared MoCAquatic default (adult, 50), so a saved or NBT-summoned ray keeps its own age.
        if (getIsAdult() && getMoCAge() == 50) {
            setAdult(false);
            setMoCAge(getTypeMoC() == 1 ? 80 + this.random.nextInt(100) : 50 + this.random.nextInt(50));
        }
    }

    /**
     * Load-time migration for stale saves: builds where the ray had no growth curve persisted every ray
     * with the shared MoCAquatic defaults (Adult:1b, AgeMoC:50). Under {@link #getSizeFactor} that shape
     * renders at 0.5x forever — an adult never re-enters the growth tick. In the current format an adult
     * ray always carries at least its species cap (a spawned ray rolls a non-adult age in selectType, and
     * maturing first raises the age to 180/90), so an adult at exactly the shared default can only be the
     * stale shape: snap it to its type's adult age. Mid-growth rays (Adult:0b) are left untouched.
     */
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (getIsAdult() && getMoCAge() == 50) {
            setMoCAge(getTypeMoC() == 1 ? 180 : 90);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Type-keyed growth (legacy base curve MoCEntityAquatic:410-416, rand(300)): a young ray ages one
        // step on a 1-in-300 tick and matures at its species cap — 180 for the mantaray
        // (MoCEntityMantaRay:23-25), 90 for the stingray (MoCEntityRay:102-104). MoCAquatic never runs
        // MoCBehavior.tickGrowth, so the curve lives here (like the fishy and small/medium fish).
        if (!getIsAdult() && this.random.nextInt(300) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= (getTypeMoC() == 1 ? 180 : 90)) {
                setAdult(true);
            }
        }

        // Only stingrays (type 2) on a non-peaceful difficulty are aggressive; everything else forgets any
        // attacker so mantarays stay harmless and rideable (legacy: type 1 / peaceful never retaliated).
        if (getTypeMoC() != 2 || level.getDifficulty() == Difficulty.PEACEFUL) {
            if (getTarget() != null) {
                setTarget(null);
            }
            return;
        }

        // Passive sting: every ~250 ticks, poison a nearby swimming player within ~2 blocks, skipping any
        // player riding a boat (legacy MoCreatures.poisonPlayer + a poison potion effect).
        if (this.tickCount % 250 == 0) {
            for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(2.0D),
                    p -> p.isInWater() && !(p.getVehicle() instanceof AbstractBoat))) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0), this);
            }
        }
    }

    /**
     * Legacy {@code MoCEntityRay.getSizeFactor} (:68-74): {@code edad * 0.01F} capped at 1.5 — a full-grown
     * mantaray (age 180) renders half again life size, while a stingray tops out at 0.9x (age 90).
     * {@code MoCMobRenderer.scale} multiplies this factor by the port's shared baby curve
     * ({@code adult ? 1 : 0.5 + 0.5 * min(age,100)/100}), so that curve is divided back out here to
     * reproduce the legacy factor exactly — the same divide-out {@code MoCEntityMediumFish.getSizeFactor}
     * uses. The divisor bottoms out at 0.5, never zero.
     */
    @Override
    public float getSizeFactor() {
        float legacy = Math.min(getMoCAge() * 0.01F, 1.5F);
        float sharedCurve = getIsAdult() ? 1.0F : 0.5F + 0.5F * Math.min(getMoCAge(), 100) / 100.0F;
        return legacy / sharedCurve;
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 1 -> modelTexture("mantray.png");
            case 2 -> modelTexture("stingray.png");
            default -> modelTexture("stingray.png");
        };
    }
}
