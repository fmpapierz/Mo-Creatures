package dev.architectury.event.events.common;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.Event;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Forge stand-in (see dev.architectury.impl.ForgeShim): from Forge's RegisterCommandsEvent. */
@FunctionalInterface
public interface CommandRegistrationEvent {
    Event<CommandRegistrationEvent> EVENT = listener -> RegisterCommandsEvent.BUS.addListener(event ->
            listener.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection()));

    void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registry,
            Commands.CommandSelection selection);
}
