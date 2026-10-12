package dev.architectury.event.events.common;

import dev.architectury.event.Event;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): level ticks, from Forge's LevelTickEvent. */
public interface TickEvent {
    Event<LevelTick<ServerLevel>> SERVER_LEVEL_POST = listener ->
            net.minecraftforge.event.TickEvent.LevelTickEvent.Post.BUS.addListener(event -> {
                if (event.level() instanceof ServerLevel level) {
                    listener.tick(level);
                }
            });

    @FunctionalInterface
    interface LevelTick<T extends Level> {
        void tick(T level);
    }
}
