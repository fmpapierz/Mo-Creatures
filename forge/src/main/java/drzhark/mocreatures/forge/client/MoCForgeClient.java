package drzhark.mocreatures.forge.client;

import drzhark.mocreatures.client.gui.MoCSettingsScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only Forge hooks, kept out of the mod class so a dedicated server never loads a client class. */
public final class MoCForgeClient {

    private MoCForgeClient() {
    }

    /** The Mods screen's Config button opens the settings screen, which returns to the Mods screen when closed. */
    public static void registerConfigScreen(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(MoCSettingsScreen::new));
    }
}
