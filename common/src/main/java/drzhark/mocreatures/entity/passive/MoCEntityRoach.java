package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityRoach}, a small insect that eats rotten flesh.
 *
 * <p>Like the 1.12.2 original, the roach is skittish: it flees on sight from nearby players and
 * larger creatures ({@code getBoogey} + {@code runLikeHell}) via ground pathing. On top of that it is
 * a legacy {@code MoCEntityInsect} flyer — occasionally (legacy {@code getFlyingFreq}=300) it spots a
 * looming creature within 4 blocks and <em>lifts off</em> (upward {@code motionY += 0.3} impulse plus a
 * synched airborne flag), then rarely settles back down (~1/50 per tick). This restores the startled
 * take-off/flutter that the ground-only port had dropped. The roach's rotten-flesh food attraction lives
 * in {@code MoCBehavior} and is untouched here.
 */
public class MoCEntityRoach extends MoCAnimal {

    /**
     * Synched: the roach is airborne. Legacy {@code MoCEntityInsect} tracked this on datawatcher 22 and
     * toggled it via {@code getFlyingFreq}=300 (take off near a large creature) / a rare 1/50 land chance;
     * ported here so the roach can leave the ground again (its legacy insect base was a flyer).
     */
    private static final EntityDataAccessor<Boolean> FLYING =
            SynchedEntityData.defineId(MoCEntityRoach.class, EntityDataSerializers.BOOLEAN);

    public MoCEntityRoach(EntityType<? extends MoCEntityRoach> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING, false);
    }

    public boolean getIsFlying() {
        return this.entityData.get(FLYING);
    }

    public void setIsFlying(boolean flying) {
        this.entityData.set(FLYING, flying);
    }

    /**
     * Legacy {@code onLivingUpdate}: the roach both takes flight from and flees on the ground from nearby
     * threats. Take-off (legacy {@code MoCEntityInsect}, {@code getFlyingFreq}=300) lifts it off with an
     * upward impulse when a large creature it can see is within 4 blocks; while airborne it rarely (~1/50)
     * settles again. When grounded it also bolts away from a nearby boogey (~1/10) via ground pathing.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        // Legacy MoCEntityInsect flight trigger (getFlyingFreq() == 300 for the roach): when grounded, a rare
        // chance to take off if a large living entity (bbox >= 0.4 x 0.4) it can see is within ~4 blocks — an
        // upward hop (motionY += 0.3) plus the airborne flag.
        if (!getIsFlying() && this.random.nextInt(300) == 0) {
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(4.0D), e -> e != this)) {
                if (other.getBbWidth() >= 0.4F && other.getBbHeight() >= 0.4F && this.hasLineOfSight(other)) {
                    Vec3 dm = getDeltaMovement();
                    setDeltaMovement(dm.x, dm.y + 0.3D, dm.z);
                    this.hurtMarked = true; // sync the impulse to clients (26.2 uses hurtMarked, not hasImpulse)
                    setIsFlying(true);
                    break;
                }
            }
        }
        // Legacy: a flying roach has a rare (~1/50) chance to settle back down each tick.
        if (getIsFlying() && this.random.nextInt(50) == 0) {
            setIsFlying(false);
        }

        // Legacy roach ground flee (gated on !getIsFlying, ~1/10 per tick): bolt away from any nearby boogey.
        if (!getIsFlying() && this.random.nextInt(10) == 0) {
            LivingEntity boogey = getBoogey(level, 3.0D);
            if (boogey != null) {
                runLikeHell(boogey);
            }
        }
    }

    /** Finds the nearest threat (see {@link #isBoogey(Entity)}) within {@code range} blocks, or {@code null}. */
    private @Nullable LivingEntity getBoogey(ServerLevel level, double range) {
        LivingEntity found = null;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(range, 4.0D, range), this::isBoogey)) {
            found = entity;
        }
        return found;
    }

    /**
     * Threats the roach flees from on sight. Mirrors the legacy {@code entitiesToInclude}: any living
     * entity that is not itself a roach and is reasonably large ({@code >= 0.5} wide or tall) — this
     * covers players and predators while ignoring other tiny insects.
     */
    private boolean isBoogey(Entity entity) {
        return entity != this && entity.isAlive()
                && !(entity instanceof MoCEntityRoach)
                && (entity instanceof Player
                    || entity.getBbWidth() >= 0.5F || entity.getBbHeight() >= 0.5F);
    }

    /**
     * Bolts away from {@code threat}: mirrors the legacy formula — take the bearing pointing away from the
     * threat (with a little jitter), project ~8 blocks along it, and path there at high speed (~1.5x).
     */
    private void runLikeHell(Entity threat) {
        double dx = this.getX() - threat.getX();
        double dz = this.getZ() - threat.getZ();
        double angle = Math.atan2(dx, dz) + (this.random.nextFloat() - this.random.nextFloat()) * 0.75D;
        double targetX = this.getX() + Math.sin(angle) * 8.0D;
        double targetZ = this.getZ() + Math.cos(angle) * 8.0D;
        this.getNavigation().moveTo(targetX, this.getY(), targetZ, 1.5D);
    }

    /** Legacy empty {@code fall(float)}: as a flyer the roach never takes fall damage (e.g. after lifting off). */
    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("roach.png");
    }
}
