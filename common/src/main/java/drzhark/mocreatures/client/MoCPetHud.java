package drzhark.mocreatures.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.entity.IMoCEntity;
import drzhark.mocreatures.entity.passive.MoCEntityKitty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Client-only "pet HUD" overlay, reviving the legacy Mo'Creatures
 * {@code displayPetName} / {@code displayPetHealth} / {@code displayPetIcons} readouts.
 *
 * <p>The legacy mod projected these above <em>every</em> nearby tamed pet via a per-entity world-to-screen
 * projection. That is fiddly on the 26.2 GUI pipeline, so this port takes the scoped route: it reads the
 * entity the crosshair is pointing at ({@link Minecraft#crosshairPickEntity}) and, if it is one of this
 * mod's tamed creatures owned by the local player, draws a compact readout just above the crosshair.</p>
 *
 * <p>Styling mirrors vanilla: a translucent dark backdrop (like an entity nametag), the name in white, and
 * health as real vanilla heart icons — not raw numbers. The kitty mood emote only appears when the cat
 * actually has a mood to convey (i.e. not while calm), so a content pet reads as a clean nameplate rather
 * than a permanent speech bubble.</p>
 */
public final class MoCPetHud {

    /** ARGB white for the name line. */
    private static final int COLOR_NAME = 0xFFFFFFFF;
    /** Translucent dark backdrop, matching vanilla nametag / tooltip shading. */
    private static final int COLOR_BG = 0x66000000;
    /** Distance (px) the readout's bottom edge sits above the exact screen centre (the crosshair). */
    private static final int ABOVE_CROSSHAIR = 18;
    /** Inner padding (px) of the backdrop panel. */
    private static final int PAD = 3;
    /** Vanilla heart sprite size (px) and the horizontal pitch between hearts. */
    private static final int HEART = 9;
    private static final int HEART_PITCH = 8;
    /** Emote icon size (px). */
    private static final int ICON = 16;

    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");

    private MoCPetHud() {}

    /** Registers the HUD overlay handler. Call once from client init. */
    public static void register() {
        ClientGuiEvent.RENDER_HUD.register(MoCPetHud::onRenderHud);
    }

    private static void onRenderHud(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        MoCConfig cfg = MoCConfig.get();
        if (!cfg.displayPetName && !cfg.displayPetHealth && !cfg.displayPetIcons) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        // RENDER_HUD fires only after the in-game HUD is drawn (not while a screen is open),
        // so no explicit screen / hide-gui guard is needed here.

        Entity target = mc.crosshairPickEntity;
        if (!(target instanceof IMoCEntity moc) || !(target instanceof LivingEntity living)) {
            return;
        }
        // Only tamed pets owned by the local player.
        if (!moc.getIsTamed()) {
            return;
        }
        String owner = moc.getOwnerName();
        if (owner == null || owner.isEmpty() || !owner.equals(mc.player.getName().getString())) {
            return;
        }

        Font font = mc.font;
        int centerX = graphics.guiWidth() / 2;

        // ---- Gather what to draw ----
        Component name = target.hasCustomName() ? target.getCustomName() : target.getName();
        float health = Math.max(0.0F, living.getHealth());
        float maxHealth = Math.max(1.0F, living.getMaxHealth());

        boolean showName = cfg.displayPetName && name != null;
        boolean showHealth = cfg.displayPetHealth;
        // Mood emote: kitties only, and only when they actually have a (non-calm) mood to show.
        MoCEntityKitty moodKitty = null;
        if (cfg.displayPetIcons && target instanceof MoCEntityKitty k
                && k.getKittyState() != MoCEntityKitty.STATE_CALM) {
            moodKitty = k;
        }
        if (!showName && !showHealth && moodKitty == null) {
            return;
        }

        int hearts = Math.min(10, Mth.ceil(maxHealth / 2.0F));

        // ---- Layout: stacked (emote, name, hearts), bottom edge just above the crosshair ----
        int nameW = showName ? font.width(name) : 0;
        int heartsW = showHealth ? (hearts - 1) * HEART_PITCH + HEART : 0;
        int contentW = Math.max(Math.max(nameW, heartsW), moodKitty != null ? ICON : 0);
        int panelW = contentW + PAD * 2;

        int contentH = (moodKitty != null ? ICON + 1 : 0)
                + (showName ? font.lineHeight + 1 : 0)
                + (showHealth ? HEART : 0);
        int panelH = contentH + PAD * 2;

        int bottom = (graphics.guiHeight() / 2) - ABOVE_CROSSHAIR;
        int top = bottom - panelH;
        int left = centerX - panelW / 2;

        graphics.fill(left, top, left + panelW, top + panelH, COLOR_BG);

        int cy = top + PAD;
        if (moodKitty != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, moodKitty.getEmoticonTexture(),
                    centerX - ICON / 2, cy, 0.0F, 0.0F, ICON, ICON, ICON, ICON);
            cy += ICON + 1;
        }
        if (showName) {
            graphics.centeredText(font, name, centerX, cy, COLOR_NAME);
            cy += font.lineHeight + 1;
        }
        if (showHealth) {
            int hx = centerX - heartsW / 2;
            int hp = Math.round(health); // in half-heart units
            for (int i = 0; i < hearts; i++) {
                int x = hx + i * HEART_PITCH;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, x, cy, HEART, HEART);
                int hpAt = i * 2;
                if (hp >= hpAt + 2) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_FULL, x, cy, HEART, HEART);
                } else if (hp >= hpAt + 1) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_HALF, x, cy, HEART, HEART);
                }
            }
        }
    }
}
