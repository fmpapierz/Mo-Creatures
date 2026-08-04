package drzhark.mocreatures.client;

import drzhark.mocreatures.client.gui.MoCCreaturePediaScreen;
import drzhark.mocreatures.client.gui.MoCNameScreen;
import drzhark.mocreatures.entity.IMoCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Client-only helper. Referenced lazily via Architectury's EnvExecutor from common code so the client
 * classes it touches are never loaded on a dedicated server.
 */
public final class MoCClientHelper {

    private MoCClientHelper() {
    }

    public static void openNameScreen(int entityId, String current) {
        Minecraft.getInstance().setScreenAndShow(new MoCNameScreen(entityId, current));
    }

    /**
     * Opens the full-screen Creaturepedia dossier for the given Mo'Creatures entity. Resolves the
     * entity from the client level by id (the client already has it) and shows the read-only screen.
     */
    public static void openCreaturePedia(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity instanceof LivingEntity living && entity instanceof IMoCEntity moc) {
            mc.setScreenAndShow(MoCCreaturePediaScreen.of(living, moc));
        }
    }
}
