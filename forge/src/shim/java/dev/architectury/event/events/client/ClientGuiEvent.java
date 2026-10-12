package dev.architectury.event.events.client;

import dev.architectury.event.Event;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): the HUD, as a layer on top of Forge's layered HUD. */
public interface ClientGuiEvent {
    AtomicInteger LAYERS = new AtomicInteger();

    Event<RenderHud> RENDER_HUD = listener -> {
        Identifier name = Identifier.fromNamespaceAndPath("mocreatures", "hud_" + LAYERS.getAndIncrement());
        AddGuiOverlayLayersEvent.BUS.addListener(event -> event.getLayeredDraw().add(name, listener::renderHud));
    };

    @FunctionalInterface
    interface RenderHud {
        void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
    }
}
