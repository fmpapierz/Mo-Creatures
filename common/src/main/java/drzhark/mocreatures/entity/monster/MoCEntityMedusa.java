package drzhark.mocreatures.entity.monster;

import drzhark.mocreatures.entity.MoCMob;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Medusa — the never-released gorgon, built from scratch for the port. A hostile mythological
 * monster in three equal-chance variants (1 green / 2 tan / 3 blue) with two signature moves:
 * meeting her eyes stiffens the beholder (a heavy slow + weakness pulse — deliberately NOT the
 * legend's petrification, which would be a death sentence in melee range), and her serpent hair
 * poisons whatever she lands a blow on.
 *
 * <p>She keeps the standard {@link MoCMob} melee + player-targeting AI and normal monster despawn
 * rules — a rare overworld night terror, not a boss.
 */
public class MoCEntityMedusa extends MoCMob {

    /** Ticks between gaze checks (the stare is sampled, not evaluated every tick). */
    private static final int GAZE_CHECK_INTERVAL = 10;
    /** Ticks between successful gaze procs, so the effect pulses rather than restacking. */
    private static final int GAZE_COOLDOWN = 40;
    /** Maximum distance at which meeting her eyes has any power. */
    private static final double GAZE_RANGE = 12.0D;
    /** The stare cone EnderMan uses for its "is the player looking at me" test. */
    private static final double GAZE_CONE = 0.025D;

    /** Counts down between gaze procs. Transient by design (not saved to NBT). */
    private int gazeCooldown;

    public MoCEntityMedusa(EntityType<? extends MoCEntityMedusa> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    /** Three equal-chance variants: 1 = green, 2 = tan, 3 = blue. */
    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(this.random.nextInt(3) + 1);
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("medusatan.png");
            case 3 -> modelTexture("medusablue.png");
            default -> modelTexture("medusagreen.png");
        };
    }

    /**
     * The stiffening gaze. Every {@value #GAZE_CHECK_INTERVAL} ticks (cooldown permitting), if the
     * current target is a player within {@value #GAZE_RANGE} blocks who is looking her in the eyes,
     * the player seizes up: Slowness IV + Weakness I for 3 seconds, announced by a low stony chime
     * at the victim. The look test is EnderMan's own {@code isLookingAtMe} view-vector dot-product
     * (same {@value #GAZE_CONE} cone, distance-adjusted, opaque-blocks line-of-sight from the
     * player's eye to hers) minus the carved-pumpkin disguise exemption — a pumpkin on your head
     * will not save you from Medusa. The {@value #GAZE_COOLDOWN}-tick cooldown makes a held stare
     * pulse (re-proccing just before the 60-tick effects lapse) instead of restacking every check.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (this.gazeCooldown > 0) {
            this.gazeCooldown--;
        }

        if (this.gazeCooldown <= 0 && this.tickCount % GAZE_CHECK_INTERVAL == 0
                && this.getTarget() instanceof Player player && player.isAlive()
                && this.distanceToSqr(player) < GAZE_RANGE * GAZE_RANGE
                && this.isLookingAtMe(player, GAZE_CONE, true, false, this.getEyeY())) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3), this);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), this);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.HOSTILE, 0.8F, 0.5F);
            this.gazeCooldown = GAZE_COOLDOWN;
        }
    }

    /** Her serpent hair strikes with every landed blow: a snake bite's poison. */
    @Override
    protected void applyHitEffects(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), this);
    }

    /**
     * Riches and writhing hair: always 1-2 emeralds, plus a 1-in-8 chance of one random snake egg
     * (the mod's {@code mocegg} with EggType 21-28 — dark snake through python, matching what
     * {@code MoCEntitySnake} lays). Super keeps the standard chance-based spawn-egg drop.
     */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        spawnAtLocation(level, new ItemStack(Items.EMERALD, 1 + this.random.nextInt(2)));
        if (this.random.nextInt(8) == 0) {
            spawnAtLocation(level, drzhark.mocreatures.item.MoCThrownEggItem.createEgg(21 + this.random.nextInt(8)));
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.SNAKEHISS.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.SNAKEHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.SNAKEDYING.get();
    }
}
