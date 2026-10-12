package dev.architectury.registry.client.level.entity;

import java.util.function.Supplier;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): answered from Forge's RegisterRenderers event. */
public final class EntityRendererRegistry {
    private EntityRendererRegistry() {}

    public static <T extends Entity> void register(Supplier<? extends EntityType<? extends T>> type,
            EntityRendererProvider<T> provider) {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> event.registerEntityRenderer(type.get(), provider));
    }
}
