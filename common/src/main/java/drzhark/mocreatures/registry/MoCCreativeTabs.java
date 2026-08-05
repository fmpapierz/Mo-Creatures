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
                        // One labelled egg per hatchable species AND coat variant, tagged with the legacy
                        // composite EggType so it hatches that exact creature. The plain unlabelled mocegg is
                        // already emitted by the ITEMS loop above as the blank "Spoiled Egg".
                        //
                        // Only the ids legacy actually gave a name are listed (MoCreatures.java:897-935).
                        // Deliberately omitted, because both would render as a second and third identical white
                        // "Spoiled Egg" alongside the plain item:
                        //   29 - turtle. Legacy has no turtle egg at all; nothing lays or drops one.
                        //   32 - nether/fire ostrich. Redundant: an ordinary ostrich egg incubated in the
                        //        Nether already hatches the fire ostrich (see MoCEntityEgg.hatch).
                        int[] eggTypes = {
                                1, 2, 3, 4, 5, 6, 7, 8, 9, 10,   // fishy variants
                                11,                              // shark
                                21, 22, 23, 24, 25, 26, 27, 28,  // snake variants
                                30, 31,                          // ostrich: wild / stolen (hatches tamed)
                                33,                              // komodo
                                41, 42, 43, 44, 45,              // scorpion variants
                                50, 51, 52, 53, 54,              // wyvern variants
                        };
                        for (int eggType : eggTypes) {
                            output.accept(drzhark.mocreatures.item.MoCThrownEggItem.createEgg(eggType));
                        }
                    })
                    .build());
}
