package dev.architectury.registry.level.entity;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): answered from Forge's EntityAttributeCreationEvent. */
public final class EntityAttributeRegistry {
    private EntityAttributeRegistry() {}

    public static void register(Supplier<? extends EntityType<? extends LivingEntity>> type,
            Supplier<AttributeSupplier.Builder> attribute) {
        EntityAttributeCreationEvent.BUS.addListener(event -> event.put(type.get(), attribute.get().build()));
    }
}
