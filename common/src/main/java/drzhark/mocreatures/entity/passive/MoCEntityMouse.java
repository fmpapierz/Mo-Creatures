package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityMouse}. A tiny passive critter with three colour variants.
 */
public class MoCEntityMouse extends MoCAnimal {

    public MoCEntityMouse(EntityType<? extends MoCEntityMouse> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 50) {
                setTypeMoC(1);
            } else if (i <= 80) {
                setTypeMoC(2);
            } else {
                setTypeMoC(3);
            }
        }
    }

    /**
     * Legacy {@code MoCEntityMouse.interact} (lines 204-225): a right-click (with any held item) carries the
     * mouse on the player and, when it is already being carried, hurls it away with a chicken-plop. Crucially the
     * legacy pick-up never called {@code setTamed} — a mouse is never a tamed/owned pet, so it keeps
     * despawning like a wild mob and never counts against the tamed-per-player cap. This override therefore
     * bypasses the base {@link MoCAnimal#mobInteract} PICKUP branch (which would tame the mouse) entirely and
     * restores the throw: on put-down it plays {@code SoundEvents.CHICKEN_EGG} (the 26.2 mapping of the legacy
     * {@code "mob.chickenplop"}) and flings the mouse at {@code player.deltaMovement * (5, 0.5, 5) + (0,0.5,0)}.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Legacy interact fired for ANY held item (there was no empty-hand gate) and always handled the
        // right-click, so intercept every hand here — this also fully bypasses the base PICKUP taming path.
        if (!this.level().isClientSide()) {
            // Shared carry toggle: pick up / put down + the legacy chickenplop and 5x throw impulse. The
            // `false` keeps the mouse from ever being tamed or owned, which legacy never did.
            toggleCarry(player, false);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Legacy {@code onLivingUpdate} proactive flee: roughly once every 15 ticks the mouse looks around
     * for a nearby large mob and, on <em>seeing</em> one, bolts away from it (legacy {@code getBoogey} +
     * {@code runLikeHell}). This is on top of the inherited {@link net.minecraft.world.entity.ai.goal.PanicGoal},
     * which only triggers once the mouse has already been hurt.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.random.nextInt(15) == 0) {
            LivingEntity boogey = getBoogey(level, 6.0D);
            if (boogey != null) {
                runLikeHell(boogey);
            }
        }
    }

    /** Finds the nearest scary mob (see {@link #isBoogey(Entity)}) within {@code range} blocks, or {@code null}. */
    private @Nullable LivingEntity getBoogey(ServerLevel level, double range) {
        LivingEntity found = null;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(range, 4.0D, range), this::isBoogey)) {
            found = entity;
        }
        return found;
    }

    /**
     * Mobs the mouse flees from on sight. Mirrors the legacy {@code entitiesToInclude}: any living entity
     * that is not another mouse and is at least half a block wide or tall (bbWidth &gt;= 0.5 or
     * bbHeight &gt;= 0.5) — i.e. essentially every non-mouse creature (cows, pigs, sheep, zombies,
     * skeletons, players, etc.), scattering the mouse away from it.
     */
    private boolean isBoogey(Entity entity) {
        return entity != this && entity.isAlive()
                && !(entity instanceof MoCEntityMouse)
                && entity instanceof LivingEntity
                && (entity.getBbWidth() >= 0.5F || entity.getBbHeight() >= 0.5F);
    }

    /**
     * Bolts away from {@code threat}: mirrors the legacy formula — take the bearing pointing away from the
     * threat (with a little jitter), project ~8 blocks along it, and path there at high speed (~1.6x).
     */
    private void runLikeHell(Entity threat) {
        double dx = this.getX() - threat.getX();
        double dz = this.getZ() - threat.getZ();
        double angle = Math.atan2(dx, dz) + (this.random.nextFloat() - this.random.nextFloat()) * 0.75D;
        double targetX = this.getX() + Math.sin(angle) * 8.0D;
        double targetZ = this.getZ() + Math.cos(angle) * 8.0D;
        this.getNavigation().moveTo(targetX, this.getY(), targetZ, 1.6D);
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("miceb.png");
            case 3 -> modelTexture("micew.png");
            default -> modelTexture("miceg.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.MICEGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.MICEHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.MICEDYING.get();
    }
}
