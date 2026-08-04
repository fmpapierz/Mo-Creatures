package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCFlyingInsect;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityDragonfly}. A small flying insect with four colour variants.
 */
public class MoCEntityDragonfly extends MoCFlyingInsect {

    public MoCEntityDragonfly(EntityType<? extends MoCEntityDragonfly> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FLYING_SPEED, 0.80D);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            int i = this.random.nextInt(100);
            if (i <= 25) {
                setTypeMoC(1);
            } else if (i <= 50) {
                setTypeMoC(2);
            } else if (i <= 75) {
                setTypeMoC(3);
            } else {
                setTypeMoC(4);
            }
        }
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 1 -> modelTexture("dragonflya.png");
            case 2 -> modelTexture("dragonflyb.png");
            case 3 -> modelTexture("dragonflyc.png");
            default -> modelTexture("dragonflyd.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return MoCSounds.DRAGONFLY.get();
    }
}
