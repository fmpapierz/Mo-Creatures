package drzhark.mocreatures.registry;

import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.item.CapturedCreature;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.registries.DeferredRegister;

/** Custom data components (currently: the amulet's captured-creature payload). */
public final class MoCDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(MoCreatures.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    private MoCDataComponents() {}

    public static final RegistrySupplier<DataComponentType<CapturedCreature>> CAPTURED_CREATURE =
            COMPONENTS.register("captured_creature", () -> DataComponentType.<CapturedCreature>builder()
                    .persistent(CapturedCreature.CODEC)
                    .networkSynchronized(CapturedCreature.STREAM_CODEC)
                    .build());
}
