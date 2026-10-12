package dev.architectury.registry.client.keymappings;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): answered from Forge's RegisterKeyMappingsEvent. */
public final class KeyMappingRegistry {
    private KeyMappingRegistry() {}

    public static void register(KeyMapping mapping) {
        RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(mapping));
    }
}
