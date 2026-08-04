package drzhark.mocreatures.client.gui;

import drzhark.mocreatures.entity.IMoCEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * Full-screen Creaturepedia dossier — the 26.2 rewrite of the legacy {@code MoCGUICreaturePedia}.
 * Opened client-side when the player right-clicks a Mo'Creatures creature with the Creaturepedia item.
 *
 * <p>The client already has the entity, so this screen reads its fields directly (species name,
 * tamed/owner, variant {@code typeMoC}, age and health) and renders them as a titled panel of
 * labelled text lines with a Done button. It sends nothing to the server — it is a pure readout.
 *
 * <p>26.2 renders screens by extracting draw commands into a {@link GuiGraphicsExtractor} via
 * {@link #extractRenderState}; text is drawn with {@code centeredText}/{@code text} and rectangles
 * with {@code fill} (all colours are fully-opaque ARGB). See {@code MoCPetHud} for the same calls.
 */
public class MoCCreaturePediaScreen extends Screen {

    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 160;
    private static final int LINE_HEIGHT = 14;

    /** ARGB: translucent dark panel fill and a muted-gold border. */
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_BORDER = 0xFF7F5F1F;
    private static final int TITLE_COLOR = 0xFFFFD24A;
    private static final int TEXT_COLOR = 0xFFE0E0E0;

    private final String species;
    private final String[] lines;

    public MoCCreaturePediaScreen(String species, String[] lines) {
        super(Component.literal("Creaturepedia"));
        this.species = species;
        this.lines = lines;
    }

    /**
     * Builds a screen from a live Mo'Creatures entity. The entity must also be a {@link LivingEntity}
     * (every MoC creature is) so health can be read.
     */
    public static MoCCreaturePediaScreen of(LivingEntity entity, IMoCEntity moc) {
        String species = entity.getType().getDescription().getString();

        String state = moc.getIsTamed()
                ? "Tamed (owner: " + (moc.getOwnerName().isEmpty() ? "unknown" : moc.getOwnerName()) + ")"
                : "Wild";

        int hp = Math.round(entity.getHealth());
        int maxHp = Math.round(entity.getMaxHealth());

        String[] lines = new String[] {
                "Status: " + state,
                "Variant: type " + moc.getTypeMoC(),
                "Age: " + (moc.getIsAdult() ? "adult" : "young") + " (" + moc.getMoCAge() + ")",
                "Health: " + hp + " / " + maxHp,
        };
        return new MoCCreaturePediaScreen(species, lines);
    }

    private int panelLeft() {
        return this.width / 2 - PANEL_WIDTH / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int doneY = panelTop() + PANEL_HEIGHT - 28;
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(cx - 50, doneY, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Draw our panel first, then let super render the background dimming + widgets (Done button)
        // on top. Order relative to super does not matter for opaque text; the button stays clickable.
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int left = panelLeft();
        int top = panelTop();
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        // Panel backdrop.
        graphics.fill(left, top, right, bottom, PANEL_BG);
        // Border: four 1px edges (there is no renderOutline in 26.2).
        graphics.fill(left, top, right, top + 1, PANEL_BORDER);
        graphics.fill(left, bottom - 1, right, bottom, PANEL_BORDER);
        graphics.fill(left, top, left + 1, bottom, PANEL_BORDER);
        graphics.fill(right - 1, top, right, bottom, PANEL_BORDER);

        // Title (species name), centered.
        graphics.centeredText(this.font, Component.literal(this.species), cx, top + 12, TITLE_COLOR);

        // Info lines, left-aligned inside the panel.
        int textX = left + 16;
        int textY = top + 36;
        for (String line : this.lines) {
            graphics.text(this.font, Component.literal(line), textX, textY, TEXT_COLOR);
            textY += LINE_HEIGHT;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
