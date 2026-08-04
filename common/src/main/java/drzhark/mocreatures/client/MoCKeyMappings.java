package drzhark.mocreatures.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import drzhark.mocreatures.client.gui.MoCSettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only keybinding registration for Mo'Creatures. Registers a single mapping,
 * {@code key.mocreatures.settings} (default <kbd>F9</kbd> &mdash; the legacy MoCSettings key), via
 * Architectury's cross-loader {@link KeyMappingRegistry}, and hooks Architectury's
 * {@link ClientTickEvent#CLIENT_POST} to open {@link MoCSettingsScreen} when the key is pressed.
 *
 * <p>Reached client-side only, from {@link MoCreaturesClient#init()}. The {@code consumeClick()}
 * loop drains every queued press so held keys don't re-open the screen each tick.</p>
 */
public final class MoCKeyMappings {

    private MoCKeyMappings() {}

    /** The settings key. Category {@link KeyMapping.Category#MISC}; default GLFW key F9. */
    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.mocreatures.settings",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            KeyMapping.Category.MISC);

    /** Dismount the current Mo'Creatures mount (legacy Dismount key). Unbound by default (assign in Controls). */
    public static final KeyMapping DISMOUNT = new KeyMapping(
            "key.mocreatures.dismount",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KeyMapping.Category.MISC);

    /** Make the current Mo'Creatures mount jump (legacy Jump key). Unbound by default to avoid the space clash. */
    public static final KeyMapping MOUNT_JUMP = new KeyMapping(
            "key.mocreatures.mount_jump",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KeyMapping.Category.MISC);

    /** Registers the mappings and installs the client-tick handler. Call once from client init. */
    public static void register() {
        KeyMappingRegistry.register(OPEN_SETTINGS);
        KeyMappingRegistry.register(DISMOUNT);
        KeyMappingRegistry.register(MOUNT_JUMP);

        ClientTickEvent.CLIENT_POST.register(MoCKeyMappings::onClientTick);
    }

    private static void onClientTick(Minecraft mc) {
        // consumeClick() only returns true for presses queued while no screen was capturing input,
        // so this naturally fires only from in-world. Drain the queue; open at most once per tick.
        boolean opened = false;
        while (OPEN_SETTINGS.consumeClick()) {
            if (!opened && mc.player != null) {
                mc.setScreenAndShow(new MoCSettingsScreen());
                opened = true;
            }
        }
        // Mount keybinds: only act when riding a Mo'Creatures mount; the effect runs authoritatively server-side.
        while (DISMOUNT.consumeClick()) {
            if (mc.player != null && mc.player.getVehicle() instanceof drzhark.mocreatures.entity.IMoCEntity) {
                dev.architectury.networking.NetworkManager.sendToServer(
                        new drzhark.mocreatures.network.MoCDismountPayload(0));
            }
        }
        while (MOUNT_JUMP.consumeClick()) {
            if (mc.player != null && mc.player.getVehicle() instanceof drzhark.mocreatures.entity.IMoCEntity) {
                dev.architectury.networking.NetworkManager.sendToServer(
                        new drzhark.mocreatures.network.MoCJumpPayload(0));
            }
        }
    }
}
