package dev.architectury.registry.level.entity;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): answered from Forge's SpawnPlacementRegisterEvent. */
public final class SpawnPlacementsRegistry {
    private SpawnPlacementsRegistry() {}

    public static <T extends Mob> void register(Supplier<? extends EntityType<T>> type, SpawnPlacementType placement,
            Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) {
        SpawnPlacementRegisterEvent.BUS.addListener(event -> event.register(type.get(), placement, heightmap, predicate,
                SpawnPlacementRegisterEvent.Operation.REPLACE));
    }
}
