package dev.architectury.registry.client.level.entity;

import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): answered from Forge's RegisterLayerDefinitions event. */
public final class EntityModelLayerRegistry {
    private EntityModelLayerRegistry() {}

    public static void register(ModelLayerLocation location, Supplier<LayerDefinition> definition) {
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(event ->
                event.registerLayerDefinition(location, definition));
    }
}
