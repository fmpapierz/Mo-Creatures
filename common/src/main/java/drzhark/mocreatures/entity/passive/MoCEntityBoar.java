package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityBoar}. A wild boar with an adult and a young texture variant.
 */
public class MoCEntityBoar extends MoCAnimal {

    public MoCEntityBoar(EntityType<? extends MoCEntityBoar> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Legacy young boars flee instead of fighting (isNotScared() == getIsAdult()): give them an extra
        // high-priority panic so a piglet bolts on the slightest provocation. Adults keep the base aggression
        // goals set up in MoCAnimal for hostile species (MeleeAttackGoal + player targeting, adult-gated).
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6D) {
            @Override
            public boolean canUse() {
                return !MoCEntityBoar.this.getIsAdult() && super.canUse();
            }
        });
        // Legacy findPlayerToAttack also hunted smaller non-player, non-mob living entities (getClosestTarget).
        // Restore that as small-animal prey targeting: adult, untamed boars only, and only creatures strictly
        // smaller than the boar (matching the legacy height/width test).
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this, Animal.class, 10, true, false,
                (living, serverLevel) -> !this.getIsTamed() && this.getIsAdult()
                        && !(living instanceof MoCEntityBoar)
                        && living.getBbHeight() < this.getBbHeight()
                        && living.getBbWidth() < this.getBbWidth()));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        Difficulty difficulty = level.getDifficulty();
        // Legacy onLivingUpdate scaled the boar's attack force by difficulty (peaceful: passive; easy: force 1;
        // normal/hard: force 2). On peaceful the legacy boar never engaged (findPlayerToAttack returned null and
        // retaliation was gated on difficultySetting > 0), so keep ALL boars passive there by dropping any target;
        // young boars flee via the panic goal regardless of difficulty. The base attack damage attribute is
        // re-derived each tick so the value tracks difficulty changes at runtime.
        if (difficulty == Difficulty.PEACEFUL) {
            setTarget(null);
        }
        AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            double force = switch (difficulty) {
                case PEACEFUL -> 1.0D;   // effectively unused; boars won't engage on peaceful (legacy ctor force 1)
                case EASY -> 1.0D;       // legacy force 1 (1 half-heart)
                default -> 2.0D;         // legacy force 2 (normal/hard: 1 heart)
            };
            if (attack.getBaseValue() != force) {
                attack.setBaseValue(force);
            }
        }
        // Legacy onLivingUpdate maturation: a young piglet slowly ages (edad++ on a rare 1-in-250 tick) from
        // its spawn age of 50 up to 100, at which point it becomes an adult (setAdult(true)). This is what turns
        // the 25% young-spawns (rolled in finalizeSpawn) into adults and lets the boarb.png/panic-goal young state
        // eventually resolve. Server-only, matching the legacy MoCreatures.isServer() gate.
        if (!getIsAdult() && this.random.nextInt(250) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= 100) {
                setAdult(true);
            }
        }
    }

    /**
     * Legacy {@code attackEntity}: when the boar bit a non-player, it cleared the fresh loot around the kill
     * (MoCTools.destroyDrops(this, 3D) — item entities younger than 50 ticks within 3 blocks), gated on the
     * destroyDrops config flag. Mirrors {@link MoCEntityBigCat#doHurtTarget}. The base {@link MoCAnimal#doHurtTarget}
     * applies the difficulty-scaled ATTACK_DAMAGE (legacy {@code force}).
     */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && !(target instanceof Player) && drzhark.mocreatures.config.MoCConfig.get().destroyDrops) {
            for (net.minecraft.world.entity.item.ItemEntity ie : level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, this.getBoundingBox().inflate(3.0D))) {
                if (ie.isAlive() && ie.tickCount < 50) {
                    ie.discard();
                }
            }
        }
        return hit;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        // Legacy MoCEntityBoar constructor: rand.nextInt(4)==0 -> setAdult(false), else setAdult(true). 25% of
        // naturally spawned boars are young piglets (boarb.png) that flee via the young-gated PanicGoal and grow
        // up over time in customServerAiStep. Age stays at the default 50 (legacy setEdad(50)); adults are the
        // default (ADULT defaults true), so only the young roll needs to flip it.
        if (this.random.nextInt(4) == 0) {
            setAdult(false);
        }
        return data;
    }

    @Override
    public Identifier getTexture() {
        if (getIsAdult()) {
            return modelTexture("boara.png");
        }
        return modelTexture("boarb.png");
    }

    // Legacy MoCEntityBoar grunted with vanilla pig sounds (getLivingSound/getHurtSound = "mob.pig.say",
    // getDeathSound = "mob.pig.death"). 26.2 reworked pig audio into the data-driven PigSoundVariant registry,
    // so there is no static SoundEvents.PIG_AMBIENT/HURT/DEATH anymore. The underlying sound EVENTS
    // (entity.pig.ambient/hurt/death) are still registered, so resolve them by key to stay faithful to the
    // original barnyard-boar audio. MoCSounds has no dedicated boar/pig entry to fall back on.
    private static @Nullable SoundEvent pigSound(String path) {
        return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(
                Identifier.withDefaultNamespace("entity.pig." + path));
    }

    private static final @Nullable SoundEvent PIG_AMBIENT = pigSound("ambient");
    private static final @Nullable SoundEvent PIG_HURT = pigSound("hurt");
    private static final @Nullable SoundEvent PIG_DEATH = pigSound("death");

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return PIG_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        // Legacy getHurtSound returned "mob.pig.say" — the pig AMBIENT oink, not the hurt squeal.
        return PIG_AMBIENT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return PIG_DEATH;
    }
}
