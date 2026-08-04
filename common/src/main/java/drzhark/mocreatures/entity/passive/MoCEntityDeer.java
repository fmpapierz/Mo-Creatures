package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityDeer}. A passive forest deer with two adult coats (antlered buck
 * and doe) plus spotted fawns that spawn young and grow up into an adult coat.
 */
public class MoCEntityDeer extends MoCAnimal {

    public MoCEntityDeer(EntityType<? extends MoCEntityDeer> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Skittish prey: deer bolt from players (hunters) and monsters well before being caught, faster than a
        // normal panic (legacy getBoogey proactive flee).
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.4D, 1.9D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 1.4D, 1.9D));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Running leap: while fleeing (moving fast on the ground) a deer occasionally springs into a hop
        // (legacy updateEntityActionState motionY = 0.6 when myMoveSpeed > 2 and moving).
        if (this.onGround()) {
            Vec3 dm = getDeltaMovement();
            if ((dm.x * dm.x + dm.z * dm.z) > 0.05D && this.random.nextInt(30) == 0) {
                setDeltaMovement(dm.x, 0.5D, dm.z);
                this.hurtMarked = true; // sync the impulse to clients (26.2 uses hurtMarked, not hasImpulse)
            }
        }
        // Fawn maturation (legacy onLivingUpdate): a fawn slowly ages and, once fully grown, becomes an
        // adult deer. Legacy did setAdult(true) then setType(rand.nextInt(1)), and nextInt(1) always yields
        // 0 -> type 0 renders deer.png (the antlered buck coat), so every grown fawn becomes a buck. Also
        // grows up bred babies (which spawn adult=false).
        if (!getIsAdult() && this.random.nextInt(50) == 0) {
            setMoCAge(getMoCAge() + 1);
            if (getMoCAge() >= 100) {
                setAdult(true);
                setTypeMoC(1);
            }
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        // Legacy MoCEntityDeer overrides fall(float) to an empty body: deer take zero fall damage. This
        // protects them during running leaps and cliff-fleeing panic.
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            // ~29% of naturally-spawned deer are spotted fawns: type-3 young that setAdult(false) and grow
            // up (legacy getTexture forced type-3 deer to setAdult(false)). Matches legacy selectType, where
            // i=rand(100): i>70 (29 values) -> type3 fawn; the rest are the two adult coats.
            if (this.random.nextInt(100) > 70) {
                setTypeMoC(3);
                setAdult(false);
                setMoCAge(20); // start small (rendered ~0.6x) and grow to full size (legacy fawn edad growth)
            } else {
                // Legacy adult coats (types 1 & 2) in the legacy ~29% / ~71% ratio: antlered buck (deer.png)
                // and doe (deerf.png).
                setTypeMoC(this.random.nextInt(100) <= 28 ? 1 : 2);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        // Young deer wear the spotted-fawn skin (legacy deerb.png fawn) whatever adult coat they grow into.
        if (!getIsAdult()) {
            return modelTexture("deerb.png");
        }
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("deerf.png");
            default -> modelTexture("deer.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        if (!getIsAdult()) {
            return MoCSounds.DEERBGRUNT.get();
        }
        return MoCSounds.DEERFGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.DEERHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.DEERDYING.get();
    }
}
