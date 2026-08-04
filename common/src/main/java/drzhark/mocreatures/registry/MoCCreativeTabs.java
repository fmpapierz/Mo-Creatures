package drzhark.mocreatures.registry;

import drzhark.mocreatures.MoCreatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.architectury.registry.registries.DeferredRegister;

/**
 * The single "Mo'Creatures" creative tab, populated with every item registered by the mod.
 */
public final class MoCCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(MoCreatures.MOD_ID, Registries.CREATIVE_MODE_TAB);

    private MoCCreativeTabs() {}

    public static final RegistrySupplier<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.mocreatures"))
                    .icon(() -> MoCItems.BUNNY_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        MoCItems.ITEMS.forEach(holder -> output.accept(holder.get()));
                        // Deterministic thrown-egg subtypes: one labelled egg per hatchable species AND coat
                        // variant, each tagged with the legacy composite EggType so it hatches that exact
                        // creature (the plain mocegg above stays random). Table (legacy MoCEntityEgg): fishy
                        // 1-10, shark 11, snakes 21-28, turtle 29, ostrich 30-32, komodo 33, scorpions 41-45,
                        // wyverns 50-54.
                        int[] eggTypes = {
                                1, 2, 3, 4, 5, 6, 7, 8, 9, 10,   // fishy variants
                                11,                              // shark
                                21, 22, 23, 24, 25, 26, 27, 28,  // snake variants
                                29,                              // turtle
                                30, 31, 32,                      // ostrich: wild / stolen (hatches tamed) / nether (hatches fire ostrich)
                                33,                              // komodo
                                41, 42, 43, 44, 45,              // scorpion variants
                                50, 51, 52, 53, 54,              // wyvern variants
                        };
                        for (int eggType : eggTypes) {
                            net.minecraft.world.item.ItemStack egg =
                                    new net.minecraft.world.item.ItemStack(MoCItems.MOCEGG.get());
                            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                            tag.putInt("EggType", eggType);
                            egg.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                    net.minecraft.world.item.component.CustomData.of(tag));
                            output.accept(egg);
                        }
                    })
                    .build());
}
