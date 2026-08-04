package drzhark.mocreatures.client.gui;

import drzhark.mocreatures.config.MoCConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * In-game Mo'Creatures settings screen (the modern equivalent of the legacy F9 {@code MoCSettings}
 * GUI). Lists every {@code public boolean} flag on {@link MoCConfig} as an on/off toggle button laid
 * out in a simple two-column grid. Toggling a button writes the flag straight back onto the live
 * {@link MoCConfig} singleton; the <em>Done</em> button persists everything via
 * {@link MoCConfig#save()} and closes.
 *
 * <p>Opened client-side from {@link drzhark.mocreatures.client.MoCKeyMappings} when the
 * {@code key.mocreatures.settings} key (default F9) is pressed. Kept intentionally lightweight and
 * dependency-free so it compiles unchanged on both Fabric and NeoForge.</p>
 */
public class MoCSettingsScreen extends Screen {

    /** One toggleable boolean flag: a label, a live read of the field, and a write back to it. */
    private record Flag(String label, BooleanSupplier getter, Consumer<Boolean> setter) {}

    private final List<Flag> flags = new ArrayList<>();
    /** Insta-spawner field: type a Mo'Creatures entity id and click Insta-Spawn (legacy F9 InstaSpawner). */
    private EditBox spawnField;
    /** Biome-group editor fields: an entity id + a group name, added/removed via the buttons (legacy CustomSpawner). */
    private EditBox bgEntityField;
    private EditBox bgGroupField;

    public MoCSettingsScreen() {
        super(Component.literal("Mo'Creatures Settings"));
        MoCConfig c = MoCConfig.get();
        // Every public boolean field on MoCConfig, in the file's declared order.
        flags.add(new Flag("Elephant Bulldozer",   () -> c.elephantBulldozer,   v -> c.elephantBulldozer = v));
        flags.add(new Flag("Easy Breeding",         () -> c.easyBreeding,        v -> c.easyBreeding = v));
        flags.add(new Flag("Easy Harvesting",       () -> c.easyHarvesting,      v -> c.easyHarvesting = v));
        flags.add(new Flag("Attack Dolphins",       () -> c.attackDolphins,      v -> c.attackDolphins = v));
        flags.add(new Flag("Attack Wolves",         () -> c.attackWolves,        v -> c.attackWolves = v));
        flags.add(new Flag("Attack Horses",         () -> c.attackHorses,        v -> c.attackHorses = v));
        flags.add(new Flag("Spawn Piranhas",        () -> c.spawnPiranhas,       v -> c.spawnPiranhas = v));
        flags.add(new Flag("Modify Vanilla Spawns", () -> c.modifyVanillaSpawns, v -> c.modifyVanillaSpawns = v));
        flags.add(new Flag("Enable Ownership",      () -> c.enableOwnership,     v -> c.enableOwnership = v));
        flags.add(new Flag("Reset Ownership",       () -> c.enableResetOwnership, v -> c.enableResetOwnership = v));
        flags.add(new Flag("Static Bed",            () -> c.staticBed,           v -> c.staticBed = v));
        flags.add(new Flag("Static Litter",         () -> c.staticLitter,        v -> c.staticLitter = v));
        flags.add(new Flag("Animate Textures",      () -> c.animateTextures,     v -> c.animateTextures = v));
        flags.add(new Flag("Destroy Drops",         () -> c.destroyDrops,        v -> c.destroyDrops = v));
        flags.add(new Flag("Destroy Passive Drops", () -> c.destroyPassiveDrops, v -> c.destroyPassiveDrops = v));
        flags.add(new Flag("Display Pet Name",      () -> c.displayPetName,      v -> c.displayPetName = v));
        flags.add(new Flag("Display Pet Health",    () -> c.displayPetHealth,    v -> c.displayPetHealth = v));
        flags.add(new Flag("Display Pet Icons",     () -> c.displayPetIcons,     v -> c.displayPetIcons = v));
        flags.add(new Flag("Particle FX",           () -> c.particleFX,          v -> c.particleFX = v));
    }

    @Override
    protected void init() {
        final int cols = 2;
        final int btnW = 150;
        final int btnH = 20;
        final int hGap = 8;
        final int vGap = 4;
        final int gridW = cols * btnW + (cols - 1) * hGap;
        final int left = (this.width - gridW) / 2;
        final int top = 40;

        for (int i = 0; i < flags.size(); i++) {
            Flag flag = flags.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = left + col * (btnW + hGap);
            int y = top + row * (btnH + vGap);

            CycleButton<Boolean> toggle = CycleButton.onOffBuilder(flag.getter().getAsBoolean())
                    .create(x, y, btnW, btnH, Component.literal(flag.label()),
                            (button, value) -> flag.setter().accept(value));
            this.addRenderableWidget(toggle);
        }

        int rows = (flags.size() + cols - 1) / cols;
        int gridBottom = top + rows * (btnH + vGap) + 6;

        // Insta-spawner row: an entity-id field + a Spawn button that runs "/moc spawn <id>" (legacy F9
        // InstaSpawner). Kept above Done and clamped so it stays on-screen on short displays.
        int spawnY = Math.min(gridBottom, this.height - 2 * btnH - 14);
        this.spawnField = new EditBox(this.font, left, spawnY, btnW, btnH, Component.literal("entity id"));
        this.spawnField.setHint(Component.literal("entity id e.g. wyvern"));
        this.spawnField.setMaxLength(48);
        this.addRenderableWidget(this.spawnField);
        this.addRenderableWidget(Button.builder(Component.literal("Insta-Spawn"), b -> instaSpawn())
                .bounds(left + btnW + hGap, spawnY, btnW, btnH).build());

        // Biome-group editor row: entity id + group + Add/Remove (runs /moc biomegroup ...). Groups:
        // forest, arctic, normal, mountain, jungle, desert, swamp.
        int bgY = Math.min(spawnY + btnH + vGap, this.height - btnH - btnH - 8);
        int fW = 88;
        int sbW = 62;
        this.bgEntityField = new EditBox(this.font, left, bgY, fW, btnH, Component.literal("entity"));
        this.bgEntityField.setHint(Component.literal("entity"));
        this.bgEntityField.setMaxLength(48);
        this.addRenderableWidget(this.bgEntityField);
        this.bgGroupField = new EditBox(this.font, left + fW + 4, bgY, fW, btnH, Component.literal("group"));
        this.bgGroupField.setHint(Component.literal("biome group"));
        this.bgGroupField.setMaxLength(16);
        this.addRenderableWidget(this.bgGroupField);
        this.addRenderableWidget(Button.builder(Component.literal("+Biome"), b -> biomeGroup(true))
                .bounds(left + 2 * (fW + 4), bgY, sbW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("-Biome"), b -> biomeGroup(false))
                .bounds(left + 2 * (fW + 4) + sbW + 4, bgY, sbW, btnH).build());

        int doneY = Math.min(bgY + btnH + 6, this.height - btnH - 6);
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds((this.width - 200) / 2, doneY, 200, btnH).build());
    }

    /** Runs the {@code /moc spawn <id>} command for the entity id typed into the insta-spawner field. */
    private void instaSpawn() {
        if (this.minecraft != null && this.minecraft.player != null && this.spawnField != null) {
            String id = this.spawnField.getValue().trim().toLowerCase(java.util.Locale.ROOT);
            if (!id.isEmpty()) {
                this.minecraft.player.connection.sendCommand("moc spawn " + id);
            }
        }
    }

    /** Runs {@code /moc biomegroup <entity> add|remove <group>} from the biome-group editor fields. */
    private void biomeGroup(boolean add) {
        if (this.minecraft == null || this.minecraft.player == null
                || this.bgEntityField == null || this.bgGroupField == null) {
            return;
        }
        String id = this.bgEntityField.getValue().trim().toLowerCase(java.util.Locale.ROOT);
        String group = this.bgGroupField.getValue().trim().toLowerCase(java.util.Locale.ROOT);
        if (!id.isEmpty() && !group.isEmpty()) {
            this.minecraft.player.connection.sendCommand(
                    "moc biomegroup " + id + (add ? " add " : " remove ") + group);
        }
    }

    @Override
    public void onClose() {
        // Persist all live flag values (the toggles already wrote them onto the singleton).
        MoCConfig.get().save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
