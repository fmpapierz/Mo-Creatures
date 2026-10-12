package drzhark.mocreatures.neoforge.client;

import drzhark.mocreatures.client.gui.MoCSettingsScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only NeoForge hooks, kept out of the mod class so a dedicated server never loads a client class. */
public final class MoCNeoForgeClient {

    private MoCNeoForgeClient() {
    }

    /** The Mods screen's Config button opens the settings screen, which returns to the Mods screen when closed. */
    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (mod, parent) -> new MoCSettingsScreen(parent));
    }
}
