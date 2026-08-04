package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

/**
 * Port of the legacy {@code MoCEntityCrab}. A small ambient crab with two colour variants.
 */
public class MoCEntityCrab extends MoCAnimal {

    /** Synched: the crab has its claws raised defensively (fleeing a predator or pinching back). */
    private static final EntityDataAccessor<Boolean> CLAWS_UP =
            SynchedEntityData.defineId(MoCEntityCrab.class, EntityDataSerializers.BOOLEAN);

    public MoCEntityCrab(EntityType<? extends MoCEntityCrab> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLAWS_UP, false);
    }

    /** Whether the crab is holding its claws up defensively (drives the raised-claw pose client-side). */
    public boolean getClawsUp() {
        return this.entityData.get(CLAWS_UP);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // A crab pinches back when attacked, but doesn't hunt unprovoked.
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.2D, true));
        // Legacy runLikeHell: a crab scuttles away in a panic when hurt.
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.PanicGoal(this, 1.6D));
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Raise the claws while fleeing (panicking) or pinching back at a target.
        boolean up = getTarget() != null || (this.getLastHurtByMob() != null && this.tickCount - this.getLastHurtByMobTimestamp() < 40);
        if (up != getClawsUp()) {
            this.entityData.set(CLAWS_UP, up);
        }
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(this.random.nextInt(2) + 1);
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("crabb.png");
            default -> modelTexture("craba.png");
        };
    }

    /** Crabs render at 0.7x (legacy {@code getSizeFactor} = 0.7f * edad*0.01, clamped by the age curve). */
    @Override
    public float getSizeFactor() {
        return 0.7F;
    }
}
