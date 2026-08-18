package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The Ogre Prince — the never-released boss ogre. A hulking royal in three variants
 * (1 = green, 2 = fire, 3 = cave), each with its own signature move: the green prince lands
 * ravager-style launching blows, the fire prince sets its victims ablaze, and the cave prince
 * blinks after prey that tries to keep its distance. All three smash harder and wider than a
 * common ogre, hunt in any light, shrug off sunlight, wear a boss bar and never despawn.
 *
 * <p>Guaranteed unique drop by variant: green — builder hammer, fire — machete, cave — teleport staff.
 */
public class MoCEntityOgrePrince extends MoCEntityOgre {

    /** Ticks between consecutive smashes (same cadence as the common ogre). */
    private static final int SMASH_COOLDOWN = 50;
    /** Maximum distance to the target that still permits a smash. */
    private static final double SMASH_REACH = 3.0D;
    /** Ticks between the cave prince's chase teleports. */
    private static final int TELEPORT_COOLDOWN = 100;

    /** Counts down each server AI step; a smash fires (and resets it) when it reaches zero. */
    private int smashCooldown;
    /** Cave prince only: counts down between chase teleports. Deliberately not persisted. */
    private int teleportCooldown;

    /**
     * Boss bar, wired exactly like {@link net.minecraft.world.entity.boss.wither.WitherBoss}: created
     * GREEN at construction (the variant is not known yet) and recolored once {@link #selectType()}
     * rolls or {@link #readAdditionalSaveData} restores the type. No darkened screen, no boss music.
     */
    private final ServerBossEvent bossEvent = Util.make(
            new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
                    BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS),
            e -> e.setDarkenScreen(false));

    public MoCEntityOgrePrince(EntityType<? extends MoCEntityOgrePrince> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    /**
     * Princes roll their variant uniformly (1 green / 2 fire / 3 cave) with none of the common ogre's
     * config-chance or Nether special-casing. Deliberately does NOT call {@code super.selectType()}:
     * the base method ends in its private {@code applyTypeHealth()}, which would clamp the prince to
     * the common ogre's 35/50 HP — princes keep the 150 HP base from {@link #createAttributes()}.
     */
    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(this.random.nextInt(3) + 1);
        }
        updateBossBarColor();
    }

    /**
     * Prince-local variant remaps. These CANNOT override {@code MoCEntityOgre.isFireOgre/isCaveOgre}
     * (both are private there), so they are fresh same-named helpers: every prince-side mechanic
     * (smash strength, rim fire, on-hit burn, fire immunity) reads these, and no inherited code path
     * that consults the base private pair is live for a prince ({@link #customServerAiStep} and the
     * smash are reimplemented here, and {@link #fireImmune()} is overridden below).
     */
    private boolean isFireOgre() {
        return getTypeMoC() == 2;
    }

    private boolean isCaveOgre() {
        return getTypeMoC() == 3;
    }

    /**
     * Prince variant mapping for the base class' multi-head look clamps: only the fire prince (2) is
     * two-headed. Its tight ±7.5 pair has more yaw clearance than the ogre's three-head fan, so the
     * yaw clamp relaxes to 25° (the model is SAT-verified clean across ±30° yaw x ±30° pitch); the
     * green/cave princes are single-headed and keep the vanilla 75°/40° envelope.
     */
    @Override
    protected boolean isMultiHeaded() {
        return getTypeMoC() == 2;
    }

    @Override
    protected int multiHeadMaxYRot() {
        return 25;
    }

    @Override
    public boolean fireImmune() {
        return isFireOgre() || super.fireImmune();
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("ogreprincefire.png");
            case 3 -> modelTexture("ogreprincecave.png");
            default -> modelTexture("ogreprincegreen.png");
        };
    }

    /** The entity type is registered at 2.7 x 5.6 blocks; scale the (shared ogre) model to match. */
    @Override
    public float getSizeFactor() {
        return 1.4F;
    }

    /** A regal rumble: same ogre voice lines, pitched well down. */
    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    /**
     * Princes are ALWAYS aggressive: the common ogre's target goal (installed by
     * {@code super.registerGoals()}) is gated on the base class' private darkness check, so strip it
     * and re-add an equivalent ungated goal — same no-line-of-sight "smell" detection, but hunting in
     * any light and out to double the configured ogre range (16 blocks minimum).
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(g -> g instanceof NearestAttackableTargetGoal);
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false) {
            {
                // No line-of-sight requirement, matching the base ogre's through-walls detection
                // (mustSee=false above likewise skips the LOS re-check while pursuing).
                this.targetConditions.ignoreLineOfSight();
            }

            @Override
            protected double getFollowDistance() {
                return princeAttackRange();
            }

            @Override
            public boolean canUse() {
                // Keep the distance test in lock-step with the (live-tunable) attack range —
                // targetConditions.range is otherwise baked once at construction.
                this.targetConditions.range(getFollowDistance());
                return super.canUse();
            }
        });
    }

    /** Detection/pursuit radius: double the common ogre's configured range, never under 16 blocks. */
    private static double princeAttackRange() {
        return Math.max(16, MoCConfig.get().ogreAttackRange * 2);
    }

    /**
     * Reimplements the ogre server AI step rather than calling super: {@code MoCEntityOgre}'s version
     * would sunburn any type-&gt;2 ogre under open sky (princes never burn) and drives its own private
     * smash bookkeeping. The supers it would chain to contribute nothing a prince needs
     * ({@code MoCMob.burnsInDaylight()} is false and {@code Mob.customServerAiStep} is empty in 26.2).
     * Smash-trigger semantics are preserved verbatim: living target + cooldown elapsed + within reach.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (this.smashCooldown > 0) {
            this.smashCooldown--;
        }
        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.smashCooldown <= 0
                && this.distanceTo(target) <= SMASH_REACH) {
            performPrinceSmash(level);
            this.smashCooldown = SMASH_COOLDOWN;
        }

        // Cave prince signature: blink to prey that keeps its distance. Up to 8 candidate spots in a
        // 3-block square around the target; on success, announce with the appear-magic chime. The
        // cooldown is transient by design (not saved to NBT).
        if (isCaveOgre() && target != null && target.isAlive()
                && this.teleportCooldown <= 0 && this.distanceTo(target) > 6.0F) {
            for (int attempt = 0; attempt < 8; attempt++) {
                double tx = target.getX() + (this.random.nextInt(7) - 3);
                double tz = target.getZ() + (this.random.nextInt(7) - 3);
                if (this.randomTeleport(tx, target.getY(), tz, true)) {
                    level.playSound(null, this.blockPosition(), MoCSounds.APPEARMAGIC.get(),
                            SoundSource.HOSTILE, 1.0F, 1.0F);
                    this.teleportCooldown = TELEPORT_COOLDOWN;
                    break;
                }
            }
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /**
     * The royal ground smash: the common ogre's blast, bigger and harder. Radius is the variant's
     * config strength + 1.5 (defaults: green 4.0 / fire 3.5 / cave 4.5), entity damage a flat 12.
     * Fresh implementation copying the base structure — {@code MoCEntityOgre.performSmash} is private
     * and hard-wires the common ogre's radius/damage, so it cannot be reused or tuned via overrides.
     */
    private void performPrinceSmash(ServerLevel level) {
        MoCConfig cfg = MoCConfig.get();
        double strength = isCaveOgre() ? cfg.caveOgreStrength : isFireOgre() ? cfg.fireOgreStrength : cfg.ogreStrength;
        double radius = strength + 1.5D;
        int blockRadius = Math.max(1, (int) radius);
        boolean fire = isFireOgre();
        BlockPos origin = this.blockPosition();

        // Smash the breakable blocks from foot level up, if mob griefing is enabled. Same rules as the
        // common ogre: skip air, unbreakables (destroy speed < 0) and anything at hardness 3+
        // (obsidian, ores, metal blocks), and never dig below the prince's feet.
        if (level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            for (int dx = -blockRadius; dx <= blockRadius; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -blockRadius; dz <= blockRadius; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(pos);
                        float hardness = state.getDestroySpeed(level, pos);
                        if (!state.isAir() && hardness >= 0.0F && hardness < 3.0F) {
                            level.destroyBlock(pos, true, this);
                        }
                    }
                }
            }
        }

        // Batter, launch and (fire prince) ignite every living thing in the blast — never the prince.
        AABB area = this.getBoundingBox().inflate(radius);
        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive())) {
            victim.hurtServer(level, source, 12.0F);
            double pushX = victim.getX() - this.getX();
            double pushZ = victim.getZ() - this.getZ();
            victim.push(pushX * 0.5D, 0.6D, pushZ * 0.5D);
            if (fire) {
                victim.igniteForSeconds(8.0F);
            }
        }

        // Fire princes scatter a few flames around the crater rim, like their commoner kin.
        if (fire && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            for (int i = 0; i < 4; i++) {
                int fx = this.random.nextInt(blockRadius * 2 + 1) - blockRadius;
                int fz = this.random.nextInt(blockRadius * 2 + 1) - blockRadius;
                BlockPos firePos = origin.offset(fx, 0, fz);
                if (level.getBlockState(firePos).isAir()
                        && level.getBlockState(firePos.below()).isSolidRender()) {
                    level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                }
            }
        }

        // Quake! Same smash sound as the common ogre, pitched down for the bigger frame.
        level.playSound(null, origin, MoCSounds.DESTROY.get(), SoundSource.HOSTILE, 1.2F, 0.6F);
        level.sendParticles(fire ? ParticleTypes.LAVA : ParticleTypes.EXPLOSION,
                this.getX(), this.getY() + 0.2D, this.getZ(),
                12 + blockRadius * 4, radius * 0.5D, 0.2D, radius * 0.5D, 0.05D);
    }

    /**
     * Per-variant signature on a landed melee hit (invoked from {@code MoCMob.doHurtTarget}):
     * green — a ravager-style launching blow; fire — set the victim ablaze; cave — none (its
     * signature is the chase teleport in {@link #customServerAiStep}).
     */
    @Override
    protected void applyHitEffects(LivingEntity target) {
        switch (getTypeMoC()) {
            case 2 -> target.igniteForSeconds(8.0F);
            case 3 -> {
            }
            default -> {
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                double dd = Math.max(Math.sqrt(dx * dx + dz * dz), 0.001D);
                target.push(dx / dd * 1.8D, 0.4D, dz / dd * 1.8D);
            }
        }
    }

    // ------------------------------------------------------------------------------ boss bar plumbing

    private void updateBossBarColor() {
        this.bossEvent.setColor(switch (getTypeMoC()) {
            case 2 -> BossEvent.BossBarColor.RED;
            case 3 -> BossEvent.BossBarColor.BLUE;
            default -> BossEvent.BossBarColor.GREEN;
        });
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
        // The bar was constructed GREEN before the saved variant was known; recolor to match it.
        updateBossBarColor();
    }

    /** Bosses never despawn (wither pattern): discard on peaceful, otherwise stay fresh forever. */
    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL && !this.getType().isAllowedInPeaceful()) {
            this.discard();
        } else {
            this.noActionTime = 0;
        }
    }

    /**
     * Bosses are exempt from population culling: this flag makes {@code MoCMobCap.isRemovable}
     * skip the prince and excludes it from vanilla {@code NaturalSpawner} cap counting — without
     * it the mod's own farthest-first cap sweep could silently {@code discard()} a live boss.
     */
    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    /**
     * Guaranteed unique drop by variant. Deliberately does NOT call super: the prince gets neither the
     * {@code MoCBehavior} table loot nor {@code MoCMob}'s chance-based spawn-egg drop — this one prize
     * is its whole custom loot.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        Item prize = switch (getTypeMoC()) {
            case 2 -> MoCItems.MACHETE.get();
            case 3 -> MoCItems.STAFFTELEPORT.get();
            default -> MoCItems.BUILDERHAMMER2.get();
        };
        spawnAtLocation(level, new ItemStack(prize));
    }
}
