package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityOgre}. A large hostile ogre with green, red (fire) and
 * blue (cave) variants, each in single- and multi-headed forms.
 *
 * <p>Ogres perform their signature ground smash on a cooldown when they have a living target in
 * melee range: they tear up the breakable blocks around their feet (mob-griefing permitting),
 * batter and knock back nearby entities, and — for the fire variant — set the area ablaze.
 */
public class MoCEntityOgre extends MoCMob {

    /** Ticks between consecutive smashes. */
    private static final int SMASH_COOLDOWN = 50;
    /** Maximum distance to the target that still permits a smash. */
    private static final double SMASH_REACH = 3.0D;

    /** Counts down each server AI step; a smash fires (and resets it) when it reaches zero. */
    private int smashCooldown;

    public MoCEntityOgre(EntityType<? extends MoCEntityOgre> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 35.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    /**
     * Legacy ogres only hunt players in the dark (legacy {@code findPlayerToAttack} gated on
     * {@code getBrightness < 0.5F}). The inherited {@link MoCMob} target AI adds an always-on
     * {@link NearestAttackableTargetGoal} that would make the ogre chase players in broad daylight —
     * and, being a combat-targeting goal, only within line of sight and out to the vanilla 32-block
     * follow range — so strip that goal here and re-add a darkness-gated variant that matches the
     * legacy detection rules: acquire the closest player within {@link #getAttackRange()} blocks
     * ({@code proxy.ogreAttackRange}, config default 12) with NO line-of-sight requirement. The config
     * describes this as the radius where ogres "smell" players, i.e. detection works through walls
     * (legacy {@code getClosestVulnerablePlayerToEntity(this, getAttackRange())} did no ray-cast).
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(g -> g instanceof NearestAttackableTargetGoal);
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false) {
            {
                // Legacy getClosestVulnerablePlayerToEntity did no line-of-sight check: ogres "smell"
                // players behind cover. Drop the combat LOS gate baked into forCombat() targetConditions
                // (mustSee=false above likewise skips the LOS re-check while pursuing).
                this.targetConditions.ignoreLineOfSight();
            }

            @Override
            protected double getFollowDistance() {
                // Legacy getAttackRange() returned proxy.ogreAttackRange (config default 12) as the
                // detection/pursuit radius; read it live so a /moc retune takes effect.
                return getAttackRange();
            }

            @Override
            public boolean canUse() {
                // Keep the distance test in lock-step with the (live-tunable) attack range —
                // targetConditions.range is otherwise baked once at construction.
                this.targetConditions.range(getFollowDistance());
                return isDarkEnoughToHunt() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isDarkEnoughToHunt() && super.canContinueToUse();
            }
        });
    }

    /** Legacy {@code findPlayerToAttack} only returned a target when {@code getBrightness < 0.5F}. */
    private boolean isDarkEnoughToHunt() {
        return this.getLightLevelDependentMagicValue() < 0.5F;
    }

    /**
     * The radius within which an ogre detects and keeps pursuing a player, from the dedicated
     * {@code ogreAttackRange} config (legacy {@code getAttackRange() = proxy.ogreAttackRange}, default 12).
     */
    private double getAttackRange() {
        return MoCConfig.get().ogreAttackRange;
    }

    @Override
    public void selectType() {
        // Legacy selectType(): any ogre spawned in the Nether (worldObj.provider.isHellWorld) is
        // ALWAYS a fire (red) ogre — setType(rand.nextInt(2)+3) — and fire-immune, overriding the
        // normal cave/fire/green config rolls. Fire immunity is handled by fireImmune() for types 3-4.
        if (this.level().dimension() == Level.NETHER) {
            setTypeMoC(this.random.nextInt(2) + 3);
            applyTypeHealth();
            return;
        }
        if (getTypeMoC() == 0) {
            // Config-driven variant split (legacy selectType read proxy.caveOgreChance / proxy.fireOgreChance;
            // defaults caveOgreChance=75, fireOgreChance=25). The higher the roll, the rarer the variant.
            MoCConfig cfg = MoCConfig.get();
            int j = this.random.nextInt(100);
            if (canCaveOgreSpawn() && j >= (100 - cfg.caveOgreChance)) {
                // cave ogre (blue) — only deep underground: no sky above and below y=50 (legacy canCaveOgreSpawn)
                setTypeMoC(this.random.nextInt(2) + 5);
            } else if (j >= (100 - cfg.fireOgreChance)) {
                // fire ogre (red)
                setTypeMoC(this.random.nextInt(2) + 3);
            } else {
                // green ogre
                setTypeMoC(this.random.nextInt(2) + 1);
            }
        }
        // Apply the type-dependent HP now that the variant is chosen (legacy getMaxHealth() + health =
        // getMaxHealth()): cave ogres (type > 4, blue) are the toughest at 50 HP, all others stay at 35.
        applyTypeHealth();
    }

    /**
     * Set the {@link Attributes#MAX_HEALTH} base value for the current variant and top the ogre off to it.
     * Cave ogres (types 5-6) get 50 HP; every other variant keeps the 35 HP from {@link #createAttributes()}.
     */
    private void applyTypeHealth() {
        double maxHealth = getTypeMoC() > 4 ? 50.0D : 35.0D;
        var attribute = this.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(maxHealth);
        }
        this.setHealth((float) maxHealth);
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 3, 4 -> modelTexture("ogrered.png");
            case 5, 6 -> modelTexture("ogreblue.png");
            default -> modelTexture("ogregreen.png");
        };
    }

    /** Fire ogres (types 3-4) are immune to fire and ignite their victims. */
    private boolean isFireOgre() {
        int type = getTypeMoC();
        return type == 3 || type == 4;
    }

    /** Cave ogres (types 5-6) hit a little harder and smash a wider radius. */
    private boolean isCaveOgre() {
        int type = getTypeMoC();
        return type == 5 || type == 6;
    }

    /** Cave ogres only spawn where the sky can't be seen and below y=50 (legacy {@code canCaveOgreSpawn}). */
    private boolean canCaveOgreSpawn() {
        return !this.level().canSeeSky(this.blockPosition()) && this.getY() < 50.0D;
    }

    @Override
    public boolean fireImmune() {
        return isFireOgre() || super.fireImmune();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (this.smashCooldown > 0) {
            this.smashCooldown--;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.smashCooldown <= 0
                && this.distanceTo(target) <= SMASH_REACH) {
            performSmash(level);
            this.smashCooldown = SMASH_COOLDOWN;
        }

        // Fire and cave ogres (type > 2) wither under bright daylight with open sky above (legacy ~5 HP per
        // proc, direct HP loss — a non-fire source so the fire-immune red ogre still burns).
        if (getTypeMoC() > 2 && level.isBrightOutside() && level.canSeeSky(this.blockPosition())) {
            float f = this.getLightLevelDependentMagicValue();
            if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F) {
                this.hurtServer(level, this.damageSources().generic(), 5.0F);
            }
        }
    }

    /**
     * The signature ogre attack: pulverise the surrounding terrain, batter everything nearby and —
     * for fire ogres — leave a ring of flames behind.
     */
    private void performSmash(ServerLevel level) {
        // Smash radius comes from the per-variant strength configs (legacy OgreStrength / CaveOgreStrength /
        // FireOgreStrength); defaults 2.5 / 3.0 / 2.0 truncate to the previous 2 / 3 / 2.
        drzhark.mocreatures.config.MoCConfig cfg = drzhark.mocreatures.config.MoCConfig.get();
        double strength = isCaveOgre() ? cfg.caveOgreStrength : isFireOgre() ? cfg.fireOgreStrength : cfg.ogreStrength;
        int radius = Math.max(1, (int) strength);
        boolean fire = isFireOgre();
        BlockPos origin = this.blockPosition();

        // Smash the breakable blocks ABOVE the ogre's feet, if mob griefing is enabled. Legacy
        // DestroyBlast only queued a block when its hardness (f4) was < 3F AND it sat above the
        // ogre (d10 > entity.posY): so obsidian (50), ore/metal blocks (>=3) and the ground the
        // ogre stands on/below are all left intact. Start dy at 0 (feet level and up, never below)
        // and cap the destroy speed at 3.0F to reproduce that soft-block-only, non-digging blast.
        if (level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(pos);
                        // Skip air, unbreakable blocks (bedrock has a destroy speed < 0) and any
                        // block at or above legacy's hardness ceiling of 3 (obsidian, ores, metal).
                        float hardness = state.getDestroySpeed(level, pos);
                        if (!state.isAir() && hardness >= 0.0F && hardness < 3.0F) {
                            level.destroyBlock(pos, true, this);
                        }
                    }
                }
            }
        }

        // Batter and ignite nearby living entities (never the ogre itself).
        AABB area = this.getBoundingBox().inflate(radius);
        DamageSource source = this.damageSources().mobAttack(this);
        float damage = isCaveOgre() ? 8.0F : 5.0F;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive())) {
            victim.hurtServer(level, source, damage);
            // Knock victims up and away from the ogre.
            double pushX = victim.getX() - this.getX();
            double pushZ = victim.getZ() - this.getZ();
            victim.push(pushX * 0.5D, 0.6D, pushZ * 0.5D);
            if (fire) {
                victim.igniteForSeconds(8.0F);
            }
        }

        // Fire ogres scatter a few flames around the crater rim.
        if (fire && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            for (int i = 0; i < 4; i++) {
                int fx = this.random.nextInt(radius * 2 + 1) - radius;
                int fz = this.random.nextInt(radius * 2 + 1) - radius;
                BlockPos firePos = origin.offset(fx, 0, fz);
                if (level.getBlockState(firePos).isAir()
                        && level.getBlockState(firePos.below()).isSolidRender()) {
                    level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());
                }
            }
        }

        // Quake! Smash sound plus a burst of explosion (or lava, for fire ogres) particles.
        level.playSound(null, origin, MoCSounds.DESTROY.get(), SoundSource.HOSTILE, 1.2F, 0.7F);
        level.sendParticles(fire ? ParticleTypes.LAVA : ParticleTypes.EXPLOSION,
                this.getX(), this.getY() + 0.2D, this.getZ(),
                12 + radius * 4, radius * 0.5D, 0.2D, radius * 0.5D, 0.05D);
    }

    /**
     * Interrupt any imminent ground smash and reschedule it a full {@link #SMASH_COOLDOWN} out.
     * Called when a dirty litter box lures the ogre onto itself (legacy cleared
     * {@code pendingSmashAttack}): the ogre shambles over to the box rather than smashing on the spot.
     */
    public void resetSmash() {
        this.smashCooldown = SMASH_COOLDOWN;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.OGRE.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.OGREHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.OGREDYING.get();
    }
}
