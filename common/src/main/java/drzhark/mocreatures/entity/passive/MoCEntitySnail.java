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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Port of the legacy {@code MoCEntitySnail}. A tiny snail with six colour variants that pulls into its
 * shell (a synched HIDING state) and freezes when a sizeable creature or player comes near, then emerges
 * once the coast is clear. Shell-less variants (types 5 and 6) never hide.
 */
public class MoCEntitySnail extends MoCAnimal {

    // Legacy defence: the snail retreats into its shell (HIDING) when a threat is close, going completely
    // still until it leaves. This is a synched flag so the render pose (a separate task) can react to it.
    private static final EntityDataAccessor<Boolean> HIDING =
            SynchedEntityData.defineId(MoCEntitySnail.class, EntityDataSerializers.BOOLEAN);

    public MoCEntitySnail(EntityType<? extends MoCEntitySnail> type, Level level) {
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
        builder.define(HIDING, false);
    }

    public boolean getIsHiding() {
        return this.entityData.get(HIDING);
    }

    public void setIsHiding(boolean hide) {
        this.entityData.set(HIDING, hide);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 17) {
                setTypeMoC(1);
            } else if (i <= 34) {
                setTypeMoC(2);
            } else if (i <= 55) {
                setTypeMoC(3);
            } else if (i <= 75) {
                setTypeMoC(4);
            } else if (i <= 90) {
                setTypeMoC(5);
            } else {
                setTypeMoC(6);
            }
        }
    }

    /**
     * Legacy {@code onLivingUpdate}: while a sizeable living entity (a predator or a player, anything both
     * wider and taller than 0.5 blocks) is within ~3 blocks and in line of sight, the snail hides and holds
     * still; otherwise it comes back out. Shell-less variants (types 5 and 6) can never hide.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        LivingEntity threat = getBoogey(level, 3.0D);
        if (threat != null) {
            if (!getIsHiding()) {
                setIsHiding(true);
            }
            this.getNavigation().stop();
        } else if (getIsHiding()) {
            setIsHiding(false);
        }

        // A snail without a shell has nowhere to hide.
        if (getIsHiding() && getTypeMoC() > 4) {
            setIsHiding(false);
        }
    }

    /**
     * Finds a nearby threat: any living entity that is not another snail and is strictly more than 0.5
     * blocks in BOTH width and height (so full-size mobs and players, not tiny critters), within
     * {@code range} blocks and visible. Mirrors the legacy {@code getBoogey}/{@code entitiesToInclude} pair.
     */
    private LivingEntity getBoogey(ServerLevel level, double range) {
        LivingEntity found = null;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(range, 4.0D, range), this::isBoogey)) {
            found = entity;
        }
        return found;
    }

    private boolean isBoogey(Entity entity) {
        return entity != this && entity.getClass() != this.getClass() && entity.isAlive()
                && (entity.getBbWidth() > 0.5F && entity.getBbHeight() > 0.5F)
                && this.hasLineOfSight(entity);
    }

    @Override
    public void travel(Vec3 input) {
        // Frozen while pulled into its shell (legacy isMovementCeased).
        super.travel(getIsHiding() ? Vec3.ZERO : input);
    }

    /** Legacy {@code isOnLadder}: the snail climbs any wall it pushes against. */
    @Override
    public boolean onClimbable() {
        return this.horizontalCollision;
    }

    /** Legacy empty {@code fall(float)}: the snail never takes fall damage. */
    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /** Legacy empty {@code jump()}: the snail never jumps. */
    @Override
    public void jumpFromGround() {
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("snailb.png");
            case 3 -> modelTexture("snailc.png");
            case 4 -> modelTexture("snaild.png");
            case 5 -> modelTexture("snaile.png");
            case 6 -> modelTexture("snailf.png");
            default -> modelTexture("snaila.png");
        };
    }
}
