package drzhark.mocreatures.fabric.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the "Warning! These settings are using experimental features" confirmation shown when
 * creating a world. Mo'Creatures registers a custom dimension (Wyvern Lair) + worldgen, which stamps the
 * world's registries with {@code Lifecycle.experimental()}; vanilla {@code WorldOpenFlows.confirmWorldCreation}
 * then interrupts world creation with the experimental ConfirmScreen. Since these features are intended and
 * stable within this mod, run the proceed-action directly and skip the warning. Client-only. The NeoForge
 * counterpart is {@code drzhark.mocreatures.neoforge.mixin.WorldOpenFlowsMixin}.
 */
@Mixin(WorldOpenFlows.class)
public class WorldOpenFlowsMixin {

    @Inject(method = "confirmWorldCreation", at = @At("HEAD"), cancellable = true)
    private static void mocreatures$skipExperimentalWarning(Minecraft minecraft, CreateWorldScreen screen,
            Lifecycle lifecycle, Runnable action, boolean skipWarnings, CallbackInfo ci) {
        action.run();
        ci.cancel();
    }
}
