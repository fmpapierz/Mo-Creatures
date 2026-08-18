package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityWWolf}. A hostile wild wolf with several coat variants.
 */
public class MoCEntityWWolf extends MoCMob {

    public MoCEntityWWolf(EntityType<? extends MoCEntityWWolf> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Optional cross-species hunting, each gated on its config flag (legacy getClosestTarget exclusions).
        drzhark.mocreatures.config.MoCConfig cfg = drzhark.mocreatures.config.MoCConfig.get();
        if (cfg.attackWolves) {
            this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                    this, net.minecraft.world.entity.animal.wolf.Wolf.class, 10, true, false,
                    (living, sl) -> this.getIsAdult()));
        }
        if (cfg.attackHorses) {
            this.targetSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                    this, drzhark.mocreatures.entity.passive.MoCEntityHorse.class, 10, true, false,
                    (living, sl) -> this.getIsAdult()));
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void customServerAiStep(net.minecraft.server.level.ServerLevel level) {
        super.customServerAiStep(level);

        // Legacy getAttackStrength scaled the wild wolf's bite by difficulty: `difficultySetting == 1` (EASY)
        // returned 2, everything ELSE (peaceful/normal/hard) returned 3. Re-derive the ATTACK_DAMAGE base each
        // tick so it tracks runtime difficulty changes.
        net.minecraft.world.entity.ai.attributes.AttributeInstance attack =
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            double force = level.getDifficulty() == net.minecraft.world.Difficulty.EASY ? 2.0D : 3.0D;
            if (attack.getBaseValue() != force) {
                attack.setBaseValue(force);
            }
        }

        // Legacy findPlayerToAttack only hunted players when the local light was dark (getBrightness < 0.5F);
        // in bright light the wild wolf drops any player it is chasing. MoCMob adds an always-on player target
        // goal, so re-impose the darkness gate here by clearing a player target once it becomes bright.
        net.minecraft.world.entity.LivingEntity target = this.getTarget();
        if (target instanceof net.minecraft.world.entity.player.Player
                && this.getLightLevelDependentMagicValue() >= 0.5F) {
            this.setTarget(null);
        }

        // Legacy: with no current target, a 1/80 tick roll makes the wolf hunt a nearby passive animal
        // (getClosestTarget scan), excluding bigcats/bears/cows/other mobs/players.
        if (this.getTarget() == null && this.random.nextInt(80) == 0) {
            net.minecraft.world.entity.LivingEntity prey = findPrey(level);
            if (prey != null) {
                this.setTarget(prey);
            }
        }
    }

    /**
     * Legacy {@code attackEntity}: after biting, a wild wolf that hit a non-player wiped the fresh loot
     * around the kill ({@code MoCTools.destroyDrops(this, 3D)} — item entities younger than 50 ticks
     * within 3 blocks) so wolves can't be used to farm animal drops. Gated on the destroyDrops config flag.
     */
    @Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && !(target instanceof net.minecraft.world.entity.player.Player)
                && drzhark.mocreatures.config.MoCConfig.get().destroyDrops) {
            for (net.minecraft.world.entity.item.ItemEntity ie : level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, this.getBoundingBox().inflate(3.0D))) {
                if (ie.isAlive() && ie.tickCount < 50) {
                    ie.discard();
                }
            }
        }
        return hit;
    }

    /**
     * Legacy {@code getClosestTarget}: nearest visible creature within range that is not a
     * player/monster/bigcat/bear/cow. Legacy scanned every living entity and
     * excluded {@code EntityPlayer} and {@code EntityMob}, so scan the broad {@code Mob} set here (players
     * are never {@code Mob}s) and drop anything that is a {@code Monster} — that covers vanilla monsters,
     * every MoCMob monster and other MoCEntityWWolf (a wolf must not hunt another wild wolf), plus this wolf
     * itself.
     */
    private net.minecraft.world.entity.@Nullable LivingEntity findPrey(net.minecraft.server.level.ServerLevel level) {
        drzhark.mocreatures.config.MoCConfig cfg = drzhark.mocreatures.config.MoCConfig.get();
        // Legacy enableHunters (MoCProxy.java:314-316): the creature-vs-creature predation master switch.
        // With hunters off the 1/80 random-prey roll yields nothing; the darkness-gated PLAYER hunt above
        // is untouched — legacy never tied player targeting to this flag.
        if (!cfg.enableHunters) {
            return null;
        }
        net.minecraft.world.entity.LivingEntity closest = null;
        double closestSq = -1.0D;
        for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(
                net.minecraft.world.entity.Mob.class, this.getBoundingBox().inflate(10.0D))) {
            // Skip players (never Mobs), this wolf, other MoCEntityWWolf and any MoCMob/vanilla monster.
            if (mob instanceof net.minecraft.world.entity.monster.Monster) {
                continue;
            }
            // Legacy getClosestTarget honoured the same two config gates as the dedicated target goals: a wild
            // wolf never hunts vanilla wolves unless attackWolves is on, nor MoC horses unless attackHorses is on
            // (both default off). Without these the 1/80 random-prey roll would hunt wolves/horses the dedicated
            // goals were told to leave alone — including a player's tamed vanilla dog (never an IMoCEntity, so the
            // tamed guard below can't spare it).
            if ((mob instanceof net.minecraft.world.entity.animal.wolf.Wolf && !cfg.attackWolves)
                    || (mob instanceof drzhark.mocreatures.entity.passive.MoCEntityHorse && !cfg.attackHorses)) {
                continue;
            }
            // Spare bigcats, bears and cows: legacy getClosestTarget excluded EntityCow alongside
            // MoCEntityBigCat/MoCEntityBear. Legacy had NO tamed-creature exclusion, so a wild wolf still
            // hunts tamed MoC passives (matching legacy) — do not re-add a tamed guard here.
            if (mob instanceof drzhark.mocreatures.entity.passive.MoCEntityBigCat
                    || mob instanceof drzhark.mocreatures.entity.passive.MoCEntityBear
                    || mob instanceof net.minecraft.world.entity.animal.cow.Cow) {
                continue;
            }
            if (!this.hasLineOfSight(mob)) {
                continue;
            }
            double distSq = this.distanceToSqr(mob);
            // Legacy getClosestTarget(this, 10D) capped acceptance at d2 < (d*d) == 100, i.e. a 10-block
            // spherical radius; the AABB inflate(10) alone lets in corner candidates up to ~17 blocks.
            if (distSq >= 100.0D) {
                continue;
            }
            if (closestSq == -1.0D || distSq < closestSq) {
                closestSq = distSq;
                closest = mob;
            }
        }
        return closest;
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            // Legacy checkSpawningBiome seeded the timber/snow wolf (type 3) ONLY in cold biomes
            // (Taiga/TaigaHills/Ice Plains/Ice Mountains/Frozen Ocean/Frozen River) before selectType ran;
            // legacy selectType itself never rolled type 3. Reproduce that: in cold regions spawn a timber
            // wolf, elsewhere roll the four temperate coats 1/2/5/4 at ~25% each (i<=25->1, <=50->2, <=75->5,
            // else->4).
            var biome = this.level().getBiome(this.blockPosition());
            if (biome.is(net.minecraft.tags.BiomeTags.IS_TAIGA)
                    || biome.value().getBaseTemperature() <= 0.15F) {
                setTypeMoC(3); // timber (snow) wolf
            } else {
                int i = this.random.nextInt(100);
                if (i <= 25) {
                    setTypeMoC(1);
                } else if (i <= 50) {
                    setTypeMoC(2);
                } else if (i <= 75) {
                    setTypeMoC(5);
                } else {
                    setTypeMoC(4);
                }
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 1 -> modelTexture("wolfblack.png");
            case 3 -> modelTexture("wolftimber.png");
            case 4 -> modelTexture("wolfdark.png");
            case 5 -> modelTexture("wolfbright.png");
            default -> modelTexture("wolfwild.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.WOLFGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.WOLFHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.WOLFDEATH.get();
    }
}
