package drzhark.mocreatures.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import drzhark.mocreatures.client.gui.MoCSettingsScreen;

/**
 * Mod Menu's config button for Mo'Creatures: opens the settings screen (toggles, creatures, sea life, admin) and
 * returns to Mod Menu when it closes. Registered as the {@code modmenu} entrypoint in fabric.mod.json and
 * quilt.mod.json. Mod Menu is a compile-only dependency: this class is loaded only when Mod Menu is installed.
 */
public final class MoCModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MoCSettingsScreen::new;
    }
}
