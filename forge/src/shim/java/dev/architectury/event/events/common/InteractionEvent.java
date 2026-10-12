package dev.architectury.event.events.common;

import dev.architectury.event.Event;
import dev.architectury.event.EventResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Forge stand-in (see dev.architectury.impl.ForgeShim): right clicks, from Forge's PlayerInteractEvent. An
 * interrupting answer cancels the click there, with SUCCESS for true and FAIL for false, as Architectury does.
 */
public interface InteractionEvent {
    Event<RightClickBlock> RIGHT_CLICK_BLOCK = listener -> PlayerInteractEvent.RightClickBlock.BUS.addListener(
            (java.util.function.Predicate<PlayerInteractEvent.RightClickBlock>) event ->
            InteractionEvent.cancel(event, listener.click(event.getEntity(), event.getHand(), event.getPos(),
                    event.getFace())));

    Event<RightClickItem> RIGHT_CLICK_ITEM = listener -> PlayerInteractEvent.RightClickItem.BUS.addListener(
            (java.util.function.Predicate<PlayerInteractEvent.RightClickItem>) event ->
            InteractionEvent.cancel(event, listener.click(event.getEntity(), event.getHand())));

    private static boolean cancel(PlayerInteractEvent event, EventResult result) {
        if (!result.interruptsFurtherEvaluation()) {
            return false;
        }
        event.setCancellationResult(Boolean.FALSE.equals(result.value()) ? InteractionResult.FAIL : InteractionResult.SUCCESS);
        return true;
    }

    @FunctionalInterface
    interface RightClickBlock {
        EventResult click(Player player, InteractionHand hand, BlockPos pos, Direction face);
    }

    @FunctionalInterface
    interface RightClickItem {
        EventResult click(Player player, InteractionHand hand);
    }
}
