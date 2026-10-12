package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCFlyingInsect;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityFly}. A tiny flying insect.
 */
public class MoCEntityFly extends MoCFlyingInsect {

    public MoCEntityFly(EntityType<? extends MoCEntityFly> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.70D);
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("fly.png");
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        buzzNearPlayer(level, MoCSounds.FLY.get(), 55); // 12.0.5 MoCEntityFly: soundCount = 55
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null; // the buzz above, heard only up close
    }
}
