package dev.architectury.impl;

import net.minecraftforge.eventbus.api.bus.BusGroup;

/**
 * Mo'Creatures' Forge stand-in for the parts of Architectury API its shared code calls. Architectury has no Forge
 * build after 1.20.4, so the Forge jar carries these few classes, implemented on Forge 65's own registries, events and
 * networking, under Architectury's names so the shared code compiles unchanged; the jar's shadowJar relocates the
 * whole dev.architectury package into drzhark.mocreatures.forge.arch, so it can never clash with a real Architectury.
 *
 * <p>The Forge entrypoint hands over the mod's bus group before anything registers.</p>
 */
public final class ForgeShim {
    private static BusGroup modBusGroup;

    private ForgeShim() {}

    public static void setModBusGroup(BusGroup group) {
        modBusGroup = group;
    }

    public static BusGroup modBusGroup() {
        if (modBusGroup == null) {
            throw new IllegalStateException("Mo'Creatures: the Forge entrypoint has not set the mod bus group yet");
        }
        return modBusGroup;
    }
}
