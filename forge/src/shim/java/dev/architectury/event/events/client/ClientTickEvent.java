package dev.architectury.event.events.client;

import dev.architectury.event.Event;
import net.minecraft.client.Minecraft;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): client ticks, from Forge's ClientTickEvent. */
public interface ClientTickEvent {
    Event<Client> CLIENT_POST = listener ->
            net.minecraftforge.event.TickEvent.ClientTickEvent.Post.BUS.addListener(event ->
                    listener.tick(Minecraft.getInstance()));

    @FunctionalInterface
    interface Client {
        void tick(Minecraft instance);
    }
}
