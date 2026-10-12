package drzhark.mocreatures.client.gui;

import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.registry.MoCSpawns;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * In-game Mo'Creatures settings screen (the modern equivalent of the legacy F9 {@code MoCSettings}
 * GUI), in three pages switched by the button next to Done:
 * <ul>
 *   <li><b>Toggles</b>: every {@code public boolean} flag on {@link MoCConfig} as an on/off button in a
 *       two-column grid, written straight onto the live {@link MoCConfig} singleton.</li>
 *   <li><b>Creatures</b>: where Mo'Creatures sea life spawns ({@code aquaticPool}: its own pool or vanilla's,
 *       see {@code MoCAquaticSpawner}) and how many live near each player, then every naturally spawning
 *       creature with an on/off switch ({@code spawn.<id>.enabled}) and a weight box ({@code spawn.<id>.frequency};
 *       empty = 12.0.5's own per-biome weights). Off stops a creature's natural spawns at once; weights apply at
 *       the next world load (sea life in the own pool: at once). Also written straight onto {@link MoCConfig}, so
 *       in singleplayer the game's own server follows them; on a multiplayer server only its own config counts.</li>
 *   <li><b>Admin</b>: the legacy GUI's remaining editing surfaces, each wired through the
 *       permission-checked {@code /moc} command tree: an insta-spawner ({@code /moc spawn}), a biome-group
 *       editor ({@code /moc biomegroup}), a numeric tunables row cycling through every {@code /moc setnumber}
 *       name, and a per-entity spawn-rate row ({@code /moc spawnrate <id> frequency|min|max <value>}).</li>
 * </ul>
 * {@link #applyPageLayout()} lays each page out on a fixed row pitch and scrolls rather than overlaps when the
 * window is too short for a whole page. <em>Done</em> persists everything via {@link MoCConfig#save()}.
 *
 * <p>Opened from {@link drzhark.mocreatures.client.MoCKeyMappings} ({@code key.mocreatures.settings}, default F9)
 * and from each loader's mod list: Mod Menu on Fabric and Quilt, the Mods screen's Config button on NeoForge and
 * Forge, which pass the screen to return to. Kept dependency-free so it compiles unchanged on every loader.</p>
 */
public class MoCSettingsScreen extends Screen {

    /** One toggleable boolean flag: a label, a live read of the field, and a write back to it. */
    private record Flag(String label, BooleanSupplier getter, Consumer<Boolean> setter) {}

    /**
     * The numeric tunables accepted by {@code /moc setnumber}, in the same order as
     * {@code MoCCommand.SUPPORTED_NUMBERS} (keep the two lists in sync).
     */
    private static final String[] NUMBER_NAMES = {
            "ogreStrength", "caveOgreStrength", "fireOgreStrength", "ogreAttackRange",
            "caveOgreChance", "fireOgreChance", "sharkStrength", "zebraChance",
            "wyvernEggDropChance", "monsterEggDropChance", "maxMobs",
            "maxWaterMobs", "aquaticsPerPlayer", "maxTamed", "maxOPTamed"};

    private static final String[] PAGE_NAMES = {"Toggles", "Creatures", "Admin"};

    /** Shared widget metrics: every row is {@code ROW_H} tall on a {@code ROW_H + ROW_GAP} pitch. */
    private static final int BTN_W = 150;
    private static final int ROW_H = 20;
    private static final int H_GAP = 8;
    private static final int ROW_GAP = 4;
    /** Top edge of the row area; rows flow down from here toward the bottom bar. */
    private static final int ROW_TOP = 40;
    /** A creature's on/off switch and weight box share one {@link #BTN_W} column. */
    private static final int CREATURE_BTN_W = 116;
    private static final int WEIGHT_W = BTN_W - CREATURE_BTN_W - 2;

    /** The screen to return to (a mod list), or null to close to the game. */
    private final @Nullable Screen parent;
    private final List<Flag> flags = new ArrayList<>();
    /** Insta-spawner field: type a Mo'Creatures entity id and click Insta-Spawn (legacy F9 InstaSpawner). */
    private EditBox spawnField;
    /** Biome-group editor fields: an entity id + a group name, added/removed via the buttons (legacy CustomSpawner). */
    private EditBox bgEntityField;
    private EditBox bgGroupField;
    /** Numeric editor: the selected {@code /moc setnumber} name (a field so it survives re-init on resize). */
    private String numName = NUMBER_NAMES[0];
    private EditBox numValueField;
    /** Spawn-rate editor fields: entity id + selected column (frequency/min/max) + value. */
    private EditBox srEntityField;
    private String srFieldName = "frequency";
    private EditBox srValueField;
    /** Which page is showing, an index into {@link #PAGE_NAMES} (a field so it survives resize). */
    private int page;
    /** Row-quantized scroll offset of the current page: the index of its first visible row. */
    private int scrollRow;
    /** Highest valid {@link #scrollRow} for the current page at the current window size. */
    private int maxScrollRow;
    /** Each page's widget rows, top to bottom; {@link #applyPageLayout()} shows/positions them. */
    private final List<AbstractWidget[]> toggleRows = new ArrayList<>();
    private final List<AbstractWidget[]> creatureRows = new ArrayList<>();
    private final List<AbstractWidget[]> adminRows = new ArrayList<>();
    /** Edit-box text mirrored out through responders so typed input survives the resize re-init. */
    private String spawnText = "";
    private String bgEntityText = "";
    private String bgGroupText = "";
    private String srEntityText = "";
    private String srValueText = "";

    public MoCSettingsScreen() {
        this(null);
    }

    public MoCSettingsScreen(@Nullable Screen parent) {
        super(Component.literal("Mo'Creatures Settings"));
        this.parent = parent;
        MoCConfig c = MoCConfig.get();
        // Every public boolean field on MoCConfig, in the file's declared order.
        flags.add(new Flag("Elephant Bulldozer",   () -> c.elephantBulldozer,   v -> c.elephantBulldozer = v));
        flags.add(new Flag("Easy Breeding",         () -> c.easyBreeding,        v -> c.easyBreeding = v));
        flags.add(new Flag("Easy Harvesting",       () -> c.easyHarvesting,      v -> c.easyHarvesting = v));
        flags.add(new Flag("Attack Dolphins",       () -> c.attackDolphins,      v -> c.attackDolphins = v));
        flags.add(new Flag("Attack Wolves",         () -> c.attackWolves,        v -> c.attackWolves = v));
        flags.add(new Flag("Attack Horses",         () -> c.attackHorses,        v -> c.attackHorses = v));
        flags.add(new Flag("Enable Hunters",        () -> c.enableHunters,       v -> c.enableHunters = v));
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
        this.toggleRows.clear();
        this.creatureRows.clear();
        this.adminRows.clear();

        final int gridW = 2 * BTN_W + H_GAP;
        final int left = (this.width - gridW) / 2;

        // Page 1 - the toggle grid, two flags per row. All rows are created at y = 0; applyPageLayout()
        // below assigns real positions and hides whichever page (or overflow rows) should not show.
        for (int i = 0; i < flags.size(); i += 2) {
            AbstractWidget[] row = new AbstractWidget[Math.min(2, flags.size() - i)];
            for (int col = 0; col < row.length; col++) {
                Flag flag = flags.get(i + col);
                row[col] = this.addRenderableWidget(CycleButton.onOffBuilder(flag.getter().getAsBoolean())
                        .create(left + col * (BTN_W + H_GAP), 0, BTN_W, ROW_H, Component.literal(flag.label()),
                                (button, value) -> flag.setter().accept(value)));
            }
            this.toggleRows.add(row);
        }

        initCreaturesPage(left, gridW);

        // Page 3, row 1 - insta-spawner: an entity-id field + a Spawn button that runs "/moc spawn <id>"
        // (legacy F9 InstaSpawner).
        this.spawnField = new EditBox(this.font, left, 0, BTN_W, ROW_H, Component.literal("entity id"));
        this.spawnField.setHint(Component.literal("entity id e.g. wyvern"));
        this.spawnField.setMaxLength(48);
        this.spawnField.setValue(this.spawnText);
        this.spawnField.setResponder(text -> this.spawnText = text);
        this.adminRows.add(new AbstractWidget[]{
                this.addRenderableWidget(this.spawnField),
                this.addRenderableWidget(Button.builder(Component.literal("Insta-Spawn"), b -> instaSpawn())
                        .bounds(left + BTN_W + H_GAP, 0, BTN_W, ROW_H).build())});

        // Page 3, row 2 - biome-group editor: entity id + group + Add/Remove (runs /moc biomegroup ...).
        // Groups: forest, arctic, normal, mountain, jungle, desert, swamp.
        int fW = 88;
        int sbW = 62;
        this.bgEntityField = new EditBox(this.font, left, 0, fW, ROW_H, Component.literal("entity"));
        this.bgEntityField.setHint(Component.literal("entity"));
        this.bgEntityField.setMaxLength(48);
        this.bgEntityField.setValue(this.bgEntityText);
        this.bgEntityField.setResponder(text -> this.bgEntityText = text);
        this.bgGroupField = new EditBox(this.font, left + fW + 4, 0, fW, ROW_H, Component.literal("group"));
        this.bgGroupField.setHint(Component.literal("biome group"));
        this.bgGroupField.setMaxLength(16);
        this.bgGroupField.setValue(this.bgGroupText);
        this.bgGroupField.setResponder(text -> this.bgGroupText = text);
        this.adminRows.add(new AbstractWidget[]{
                this.addRenderableWidget(this.bgEntityField),
                this.addRenderableWidget(this.bgGroupField),
                this.addRenderableWidget(Button.builder(Component.literal("+Biome"), b -> biomeGroup(true))
                        .bounds(left + 2 * (fW + 4), 0, sbW, ROW_H).build()),
                this.addRenderableWidget(Button.builder(Component.literal("-Biome"), b -> biomeGroup(false))
                        .bounds(left + 2 * (fW + 4) + sbW + 4, 0, sbW, ROW_H).build())});

        // Page 3, row 3 - numeric tunables: a dropdown cycling through every /moc setnumber name + a
        // value field + Set (runs "/moc setnumber <name> <value>"; the legacy GUI's numeric text boxes).
        // The value field re-fills with the live MoCConfig value whenever the dropdown cycles.
        int valW = 90;
        this.numValueField = new EditBox(this.font, left + BTN_W + 4, 0, valW, ROW_H, Component.literal("value"));
        this.numValueField.setHint(Component.literal("value"));
        this.numValueField.setMaxLength(16);
        this.numValueField.setValue(currentNumber(this.numName));
        this.adminRows.add(new AbstractWidget[]{
                this.addRenderableWidget(CycleButton.<String>builder(Component::literal, this.numName)
                        .withValues(NUMBER_NAMES)
                        .displayOnlyValue()
                        .create(left, 0, BTN_W, ROW_H, Component.literal("Setting"), (button, value) -> {
                            this.numName = value;
                            this.numValueField.setValue(currentNumber(value));
                        })),
                this.addRenderableWidget(this.numValueField),
                this.addRenderableWidget(Button.builder(Component.literal("Set"), b -> sendSetNumber())
                        .bounds(left + BTN_W + 4 + valW + 4, 0, 56, ROW_H).build())});

        // Page 3, row 4 - per-entity spawn rates: entity id + frequency|min|max + value + Set (runs
        // "/moc spawnrate <id> <field> <value>"; legacy per-entity frequency/min/max editing).
        int srFieldW = 72;
        int srValW = 62;
        this.srEntityField = new EditBox(this.font, left, 0, fW, ROW_H, Component.literal("entity"));
        this.srEntityField.setHint(Component.literal("entity"));
        this.srEntityField.setMaxLength(48);
        this.srEntityField.setValue(this.srEntityText);
        this.srEntityField.setResponder(text -> this.srEntityText = text);
        this.srValueField = new EditBox(this.font, left + fW + 4 + srFieldW + 4, 0, srValW, ROW_H,
                Component.literal("value"));
        this.srValueField.setHint(Component.literal("value"));
        this.srValueField.setMaxLength(6);
        this.srValueField.setValue(this.srValueText);
        this.srValueField.setResponder(text -> this.srValueText = text);
        this.adminRows.add(new AbstractWidget[]{
                this.addRenderableWidget(this.srEntityField),
                this.addRenderableWidget(CycleButton.<String>builder(Component::literal, this.srFieldName)
                        .withValues("frequency", "min", "max")
                        .displayOnlyValue()
                        .create(left + fW + 4, 0, srFieldW, ROW_H, Component.literal("Field"),
                                (button, value) -> this.srFieldName = value)),
                this.addRenderableWidget(this.srValueField),
                this.addRenderableWidget(Button.builder(Component.literal("Set"), b -> sendSpawnRate())
                        .bounds(left + fW + 4 + srFieldW + 4 + srValW + 4, 0, 56, ROW_H).build())});

        // Bottom bar - the page switch and Done, always the bottom-most widgets on every page. Switching
        // the page only toggles visibility (no widget rebuild), so typed text and focus stay sane.
        int doneY = this.height - ROW_H - 6;
        this.addRenderableWidget(Button.builder(pageButtonLabel(), b -> {
            this.page = (this.page + 1) % PAGE_NAMES.length;
            this.scrollRow = 0;
            b.setMessage(pageButtonLabel());
            applyPageLayout();
        }).bounds(this.width / 2 - 100, doneY, 98, ROW_H).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(this.width / 2 + 2, doneY, 98, ROW_H).build());

        applyPageLayout();
    }

    /**
     * Page 2 - creatures: the sea-life pool and its per-player count, a note row, then every naturally spawning
     * creature, two per row, each an on/off switch plus a weight box.
     */
    private void initCreaturesPage(int left, int gridW) {
        MoCConfig c = MoCConfig.get();
        boolean ownAtStart = MoCSpawns.aquaticsInOwnPool();
        this.creatureRows.add(new AbstractWidget[]{
                this.addRenderableWidget(CycleButton.<String>builder(
                                value -> Component.literal((value.equals("own") ? "Own pool" : "Vanilla's")
                                        + (value.equals("own") != ownAtStart ? " (restart)" : "")),
                                c.aquaticPool)
                        .withValues("own", "vanilla")
                        .create(left, 0, BTN_W, ROW_H, Component.literal("Sea life"),
                                (button, value) -> c.aquaticPool = value)),
                this.addRenderableWidget(new StringWidget(left + BTN_W + H_GAP, 0, BTN_W - WEIGHT_W - 2, ROW_H,
                        Component.literal("Near each player:"), this.font)),
                this.addRenderableWidget(numberBox(left + gridW - WEIGHT_W, Integer.toString(c.aquaticsPerPlayer),
                        "10", Component.literal("Sea life near each player"), text -> {
                            if (!text.isEmpty()) {
                                c.aquaticsPerPlayer = Integer.parseInt(text);
                            }
                        }))});
        boolean remote = this.minecraft != null && this.minecraft.getSingleplayerServer() == null
                && this.minecraft.getConnection() != null;
        this.creatureRows.add(new AbstractWidget[]{
                this.addRenderableWidget(new StringWidget(left, 0, gridW, ROW_H, Component.literal(remote
                        ? "On a server only the server's own config counts."
                        : "Off: no new spawns. Weight: empty = 12.0.5's."), this.font))});

        List<String> ids = new ArrayList<>(MoCConfig.naturalCreatureIds());
        for (int i = 0; i < ids.size(); i += 2) {
            List<AbstractWidget> row = new ArrayList<>();
            for (int col = 0; col < 2 && i + col < ids.size(); col++) {
                String id = ids.get(i + col);
                int x = left + col * (BTN_W + H_GAP);
                row.add(this.addRenderableWidget(CycleButton.onOffBuilder(c.spawnEnabled(id))
                        .create(x, 0, CREATURE_BTN_W, ROW_H, Component.translatable("entity.mocreatures." + id),
                                (button, value) -> c.setSpawnEnabled(id, value))));
                Integer weight = c.spawnOverride(id, "frequency");
                row.add(this.addRenderableWidget(numberBox(x + CREATURE_BTN_W + 2,
                        weight == null ? "" : weight.toString(), "auto",
                        Component.translatable("entity.mocreatures." + id).append(" weight"), text -> {
                            if (text.isEmpty()) {
                                c.clearSpawnValue(id, "frequency");
                            } else {
                                c.setSpawnValue(id, "frequency", Integer.parseInt(text));
                            }
                        })));
            }
            this.creatureRows.add(row.toArray(new AbstractWidget[0]));
        }
    }

    /**
     * A small box taking 0-4 digits (anything else typed is dropped); {@code onChange} gets each new value.
     * {@code name} is what the narrator reads for it.
     */
    private EditBox numberBox(int x, String value, String hint, Component name, Consumer<String> onChange) {
        EditBox box = new EditBox(this.font, x, 0, WEIGHT_W, ROW_H, name);
        box.setMaxLength(4);
        box.setHint(Component.literal(hint));
        box.setValue(value);
        box.setResponder(text -> {
            String digits = text.chars().filter(Character::isDigit)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
            if (!digits.equals(text)) {
                box.setValue(digits); // comes back here with the digits
                return;
            }
            onChange.accept(digits);
        });
        return box;
    }

    /** Label of the page-switch button: names the page it will switch to. */
    private Component pageButtonLabel() {
        int next = (this.page + 1) % PAGE_NAMES.length;
        return Component.literal(next == 0 ? "< " + PAGE_NAMES[0] : PAGE_NAMES[next] + " >");
    }

    /**
     * Lays the current page's rows out top-down from {@link #ROW_TOP} on a fixed pitch and hides the
     * other pages entirely. Rows that cannot fit above the bottom bar are hidden instead of squeezed
     * (hidden widgets accept no clicks, keys, or focus), and {@link #mouseScrolled} pages them back
     * into view - so no window size or GUI scale can make widgets overlap.
     */
    private void applyPageLayout() {
        List<List<AbstractWidget[]>> pages = List.of(this.toggleRows, this.creatureRows, this.adminRows);
        List<AbstractWidget[]> shown = pages.get(this.page);
        for (List<AbstractWidget[]> other : pages) {
            if (other != shown) {
                for (AbstractWidget[] row : other) {
                    for (AbstractWidget widget : row) {
                        widget.visible = false;
                    }
                }
            }
        }
        int pitch = ROW_H + ROW_GAP;
        int availH = this.height - ROW_H - 6 - ROW_GAP - ROW_TOP; // between ROW_TOP and the bottom bar
        // How many whole rows fit above the bottom bar; 0 on absurdly short windows (nothing shows,
        // which still beats overlapping the bottom bar).
        int visibleRows = Math.max(0, (availH + ROW_GAP) / pitch);
        this.maxScrollRow = visibleRows == 0 ? 0 : Math.max(0, shown.size() - visibleRows);
        this.scrollRow = Math.max(0, Math.min(this.scrollRow, this.maxScrollRow));
        for (int r = 0; r < shown.size(); r++) {
            boolean visible = r >= this.scrollRow && r < this.scrollRow + visibleRows;
            for (AbstractWidget widget : shown.get(r)) {
                widget.visible = visible;
                if (visible) {
                    widget.setY(ROW_TOP + (r - this.scrollRow) * pitch);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (super.mouseScrolled(x, y, scrollX, scrollY)) {
            return true;
        }
        if (this.maxScrollRow > 0 && scrollY != 0.0D) {
            int target = Math.max(0, Math.min(this.scrollRow - (int) Math.signum(scrollY), this.maxScrollRow));
            if (target != this.scrollRow) {
                this.scrollRow = target;
                applyPageLayout();
            }
            return true;
        }
        return false;
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

    /** Runs {@code /moc setnumber <name> <value>} for the selected tunable and the typed value. */
    private void sendSetNumber() {
        if (this.minecraft == null || this.minecraft.player == null || this.numValueField == null) {
            return;
        }
        String raw = this.numValueField.getValue().trim();
        if (raw.isEmpty()) {
            return;
        }
        try {
            Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            // Not a number: revert the field to the live config value instead of sending garbage.
            this.numValueField.setValue(currentNumber(this.numName));
            return;
        }
        this.minecraft.player.connection.sendCommand("moc setnumber " + this.numName + " " + raw);
    }

    /** Runs {@code /moc spawnrate <entity> <frequency|min|max> <value>} from the spawn-rate row. */
    private void sendSpawnRate() {
        if (this.minecraft == null || this.minecraft.player == null
                || this.srEntityField == null || this.srValueField == null) {
            return;
        }
        String id = this.srEntityField.getValue().trim().toLowerCase(java.util.Locale.ROOT);
        String raw = this.srValueField.getValue().trim();
        if (id.isEmpty() || raw.isEmpty()) {
            return;
        }
        try {
            Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            this.srValueField.setValue("");
            return;
        }
        this.minecraft.player.connection.sendCommand(
                "moc spawnrate " + id + " " + this.srFieldName + " " + raw);
    }

    /**
     * Live value of a {@code /moc setnumber} tunable read straight off {@link MoCConfig}, formatted
     * for the value field. Mirrors {@code MoCCommand.readNumber} (keep the switches in sync).
     */
    private static String currentNumber(String name) {
        MoCConfig c = MoCConfig.get();
        switch (name) {
            case "ogreStrength":         return fmt(c.ogreStrength);
            case "caveOgreStrength":     return fmt(c.caveOgreStrength);
            case "fireOgreStrength":     return fmt(c.fireOgreStrength);
            case "ogreAttackRange":      return Integer.toString(c.ogreAttackRange);
            case "caveOgreChance":       return Integer.toString(c.caveOgreChance);
            case "fireOgreChance":       return Integer.toString(c.fireOgreChance);
            case "sharkStrength":        return Integer.toString(c.sharkStrength);
            case "zebraChance":          return Integer.toString(c.zebraChance);
            case "wyvernEggDropChance":  return Integer.toString(c.wyvernEggDropChance);
            case "monsterEggDropChance": return Integer.toString(c.monsterEggDropChance);
            case "maxMobs":              return Integer.toString(c.maxMobs);
            case "maxWaterMobs":         return Integer.toString(c.maxWaterMobs);
            case "aquaticsPerPlayer":    return Integer.toString(c.aquaticsPerPlayer);
            case "maxTamed":             return Integer.toString(c.maxTamed);
            case "maxOPTamed":           return Integer.toString(c.maxOPTamed);
            default:                     return "";
        }
    }

    /** Formats a double tunable: integer-valued doubles print without a trailing {@code .0}. */
    private static String fmt(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    @Override
    public void onClose() {
        // Persist all live values (the widgets already wrote them onto the singleton).
        MoCConfig.get().save();
        if (this.parent != null && this.minecraft != null) {
            this.minecraft.setScreenAndShow(this.parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
