package testclient;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Keeps an unattended test client moving and lets a script drive it without keyboard or mouse input.
 * <p>
 * Every half second it answers the two startup prompts that would otherwise stall a minimized client (user rule):
 * <ul>
 * <li>the first-start accessibility/narrator onboarding screen - pressed "Continue" ({@code onClose()}), never Enter,
 * which would turn the narrator on;</li>
 * <li>the "Experimental Settings" warning - "I know what I'm doing!" on vanilla's backup screen (shown when a world with
 * mod datapack registries is opened), "Proceed" on the world-creation ConfirmScreen. Only screens whose title mentions
 * Experimental are answered, so no other confirmation is ever touched.</li>
 * </ul>
 * It also watches {@code <game dir>/testclient/inbox} for {@code *.txt} command files (processed in name order, then
 * deleted), one command per line: {@code screenshot <name>} (saves {@code screenshots/<name>.png} from the main render
 * target - the game keeps rendering while minimized), {@code cmd <command>} (runs it as the local player; the world
 * needs cheats), {@code attack} (a left click on the nearest living entity within 4 blocks), {@code report} (logs the
 * player's swing progress, swing ticks and attack strength) and {@code quit}.
 * <p>
 * Light Level Overlay additions: {@code presskey <GLFW key code>} (a key press as the game's keyboard handler reports
 * it: {@code KeyMapping.click} for every mapping bound to that key, e.g. 296 = F7), {@code keys <text>} (logs whether
 * a key mapping whose name contains the text is registered in the options, i.e. shows in the controls screen) and
 * {@code invoke <class> <static no-arg method>} (calls a mod method on the client thread and logs its result).
 * <p>
 * It logs {@code [testclient] player in world} each time the local player appears, which is what a test script waits
 * for: a client that joins a server never sees its own "joined the game" message (the server broadcasts it before
 * adding the player), so that line only shows up in singleplayer logs.
 * <p>
 * Everything goes through reflection, by Mojang names: Minecraft 26.x runs unobfuscated on every loader, in development
 * and in production alike.
 */
final class RemoteControl implements Runnable {
    private final Instrumentation instrumentation;
    /** The prompt last answered, so one screen is never answered twice (proceeding twice would load a world twice). */
    private Object answered;
    /** Whether the local player existed at the last look, so each arrival in a world is logged once. */
    private boolean inWorld;
    /** The screen at the last look, so each change is logged once. */
    private Object lastScreen;
    /** When the last heartbeat was printed. */
    private long lastHeartbeat;

    RemoteControl(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    static void start(Instrumentation instrumentation) {
        Thread thread = new Thread(new RemoteControl(instrumentation), "testclient-remote");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        Path inbox = Path.of("testclient", "inbox");
        Class<?> minecraft = null;
        int ticks = 0;
        while (true) {
            try {
                Thread.sleep(500);
                ticks++;
                if (minecraft == null && ticks % 4 == 0) {
                    minecraft = findLoadedClass("net.minecraft.client.Minecraft");
                }
                if (minecraft == null) {
                    continue;
                }
                answerPrompts(minecraft);
                announcePlayer(minecraft);
                heartbeat(minecraft);
                if (!Files.isDirectory(inbox)) {
                    continue;
                }
                List<Path> files;
                try (Stream<Path> listing = Files.list(inbox)) {
                    files = listing.filter(p -> p.getFileName().toString().endsWith(".txt")).sorted().toList();
                }
                for (Path file : files) {
                    List<String> lines = Files.readAllLines(file);
                    Files.delete(file);
                    for (String line : lines) {
                        handle(minecraft, line.trim());
                    }
                }
            } catch (Throwable t) {
                System.err.println("[testclient] remote control: " + t);
            }
        }
    }

    /**
     * Every 5 seconds: "[testclient] heartbeat t=<epoch s> ticks=<client ticks> fps=<fps> inWorld=<bool> screen=<class>
     * title='<title>'". The test scripts' watcher (launchwatch.stall_check) reads it: no heartbeat means the JVM is
     * frozen, ticks that stop moving mean the game thread hangs, the same screen for too long means it is stuck.
     */
    private void heartbeat(Class<?> minecraftClass) throws Exception {
        long now = System.currentTimeMillis();
        if (now - lastHeartbeat < 5000) {
            return;
        }
        lastHeartbeat = now;
        Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
        if (minecraft == null) {
            return;
        }
        long ticks = -1;
        try {
            openPackage(minecraftClass);
            Field field = minecraftClass.getDeclaredField("clientTickCount");
            field.setAccessible(true);
            ticks = ((Number) field.get(minecraft)).longValue();
        } catch (Throwable ignored) {
            // older versions name it differently; the watcher then relies on the other signals
        }
        Object fps = minecraftClass.getMethod("getFps").invoke(minecraft);
        Object screen = currentScreen(minecraftClass, minecraft);
        boolean player = minecraftClass.getField("player").get(minecraft) != null;
        String title = screen == null ? "" : screenTitle(screen).replace('\'', '"');
        System.out.println("[testclient] heartbeat t=" + now / 1000 + " ticks=" + ticks + " fps=" + fps + " inWorld=" + player
                + " screen=" + (screen == null ? "none" : screen.getClass().getName()) + " title='" + title + "'");
    }

    private void announcePlayer(Class<?> minecraftClass) throws Exception {
        Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
        boolean now = minecraft != null && minecraftClass.getField("player").get(minecraft) != null;
        if (now && !inWorld) {
            System.out.println("[testclient] player in world");
        }
        inWorld = now;
    }

    private Object currentScreen(Class<?> minecraftClass, Object minecraft) throws Exception {
        // 26.2 keeps the active screen on Minecraft.gui (Gui#screen()), no longer on Minecraft itself.
        Object gui = minecraftClass.getField("gui").get(minecraft);
        return gui == null ? null : gui.getClass().getMethod("screen").invoke(gui);
    }

    /** Buttons that move on past a warning, as the game labels them (test clients run in English). */
    private static final Set<String> CONTINUE_LABELS = Set.of("Proceed", "Continue", "I Know What I'm Doing!",
            "Proceed to main menu", "Proceed Anyway", "Continue Anyway");
    /** Screens that mean the run has failed: they are reported, never clicked through. */
    private static final List<String> ERROR_SCREENS = List.of(
            "net.minecraft.client.gui.screens.DisconnectedScreen",
            "net.minecraft.client.gui.screens.DatapackLoadFailureScreen",
            "net.minecraft.client.gui.screens.OutOfMemoryScreen",
            "net.minecraft.client.gui.screens.RecoverWorldDataScreen",
            "net.minecraft.client.gui.screens.ErrorScreen",
            "com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen");
    /** Warning screens that are continued past with their own "continue" button. */
    private static final List<String> WARNING_SCREENS = List.of(
            "net.minecraft.client.gui.screens.multiplayer.WarningScreen",
            "net.minecraft.client.gui.screens.options.UnsupportedGraphicsWarningScreen",
            "net.minecraft.client.gui.screens.NoticeWithLinkScreen");

    /**
     * Keeps a test client from ever sitting on a screen that waits for a click (user rule: no loader may get stuck on
     * a warning screen). Every screen gets one decision, logged with a fixed prefix the test scripts read
     * (tools/testclient/launchwatch.py):
     * <ul>
     * <li>"[testclient] WARNING SCREEN continued: ..." - a known warning, moved past the way a player would:
     * the narrator onboarding (Continue, never Enter), every backup prompt ("Experimental Settings", old/new world
     * version: load without backup, test worlds are copies), the experimental-features confirmation on world
     * creation, NeoForge/Forge's mod-loading screen when it only has WARNINGS ("Proceed to main menu"), the
     * multiplayer warning, the unsupported-graphics warning and link notices. Each one is a finding for the
     * report, not something to hide.</li>
     * <li>"[testclient] ERROR SCREEN ..." - disconnected, datapack load failure, out of memory, world recovery,
     * any ErrorScreen, NeoForge/Forge's mod-loading screen with ERRORS: never clicked through; the script fails the
     * run with the screen's text.</li>
     * <li>anything else is left alone and named in "[testclient] screen -> ..." lines and in the heartbeat; if
     * it stays up, the script's watcher reports the run STUCK on it.</li>
     * </ul>
     */
    private void answerPrompts(Class<?> minecraftClass) throws Exception {
        Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
        if (minecraft == null) {
            return;
        }
        Object screen = currentScreen(minecraftClass, minecraft);
        if (screen != lastScreen) {
            lastScreen = screen;
            System.out.println("[testclient] screen -> " + describeScreen(screen));
        }
        if (screen == null || screen == answered) {
            return;
        }
        Class<?> screenBase = findSuper(screen.getClass(), "net.minecraft.client.gui.screens.Screen");
        if (screenBase == null) {
            return;
        }
        openPackage(screenBase);
        openPackage(screen.getClass());
        Method execute = minecraftClass.getMethod("execute", Runnable.class);
        String title = screenTitle(screen);

        if (findSuper(screen.getClass(), "net.minecraft.client.gui.screens.AccessibilityOnboardingScreen") != null) {
            answered = screen;
            System.out.println("[testclient] WARNING SCREEN continued: accessibility/narrator onboarding (Continue, narrator stays off)");
            Method onClose = screenBase.getMethod("onClose");
            execute.invoke(minecraft, (Runnable) () -> invokeQuietly(onClose, screen));
            return;
        }

        // NeoForge's and Forge's mod-loading screen: errors fail the run, warnings only are continued past.
        if (screen.getClass().getName().endsWith(".LoadingErrorScreen")) {
            answered = screen;
            List<?> errors = (List<?>) readField(screen, "modLoadErrors");
            List<?> warnings = (List<?>) readField(screen, "modLoadWarnings");
            if (errors != null && !errors.isEmpty()) {
                System.out.println("[testclient] ERROR SCREEN mod loading errors: " + errors
                        + (warnings == null || warnings.isEmpty() ? "" : " | warnings: " + warnings));
                return;
            }
            System.out.println("[testclient] WARNING SCREEN continued: mod loading warnings: " + warnings);
            Object next = readField(screen, "nextScreenTask");
            if (next instanceof Runnable task) {
                execute.invoke(minecraft, (Runnable) () -> {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        System.err.println("[testclient] loading-warnings continue failed: " + t);
                    }
                });
            } else {
                pressContinue(minecraft, execute, screen, title);
            }
            return;
        }

        for (String error : ERROR_SCREENS) {
            if (findSuper(screen.getClass(), error) != null) {
                answered = screen;
                System.out.println("[testclient] ERROR SCREEN " + describeScreen(screen) + " text: " + screenText(screen));
                return;
            }
        }

        Class<?> backup = findSuper(screen.getClass(), "net.minecraft.client.gui.screens.BackupConfirmScreen");
        if (backup != null) {
            Field field = backup.getDeclaredField("onProceed");
            field.setAccessible(true);
            Object listener = field.get(screen);
            // the Listener's single method proceed(boolean backup, boolean eraseCache), found by shape not by name;
            // called through the public interface because the implementation is a lambda in a closed package
            Method proceed = null;
            for (Method m : field.getType().getMethods()) {
                if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == boolean.class && m.getParameterTypes()[1] == boolean.class) {
                    proceed = m;
                }
            }
            if (proceed == null) {
                return;
            }
            answered = screen;
            System.out.println("[testclient] WARNING SCREEN continued: \"" + title + "\" (load without backup)");
            Method call = proceed;
            execute.invoke(minecraft, (Runnable) () -> invokeQuietly(call, listener, false, false));
            return;
        }

        Class<?> experimentalFeatures = findSuper(screen.getClass(),
                "net.minecraft.client.gui.screens.worldselection.ConfirmExperimentalFeaturesScreen");
        Class<?> confirm = findSuper(screen.getClass(), "net.minecraft.client.gui.screens.ConfirmScreen");
        if (experimentalFeatures != null || (confirm != null && title.contains("Experimental"))) {
            Class<?> owner = experimentalFeatures != null ? experimentalFeatures : confirm;
            Field field = owner.getDeclaredField("callback");
            field.setAccessible(true);
            Object callback = field.get(screen);
            answered = screen;
            System.out.println("[testclient] WARNING SCREEN continued: \"" + title + "\" (proceed)");
            Method accept = field.getType().getMethod("accept", boolean.class);
            execute.invoke(minecraft, (Runnable) () -> invokeQuietly(accept, callback, true));
            return;
        }

        for (String warning : WARNING_SCREENS) {
            if (findSuper(screen.getClass(), warning) != null) {
                answered = screen;
                pressContinue(minecraft, execute, screen, title);
                return;
            }
        }
    }

    /** Presses the screen's own continue-type button (Button.onPress), the way a click would. */
    private void pressContinue(Object minecraft, Method execute, Object screen, String title) throws Exception {
        Class<?> buttonClass = Class.forName("net.minecraft.client.gui.components.Button", false, screen.getClass().getClassLoader());
        openPackage(buttonClass);
        Field onPress = buttonClass.getDeclaredField("onPress");
        onPress.setAccessible(true);
        for (Object child : (List<?>) screen.getClass().getMethod("children").invoke(screen)) {
            if (!buttonClass.isInstance(child)) {
                continue;
            }
            Object message = child.getClass().getMethod("getMessage").invoke(child);
            String label = String.valueOf(message.getClass().getMethod("getString").invoke(message));
            if (CONTINUE_LABELS.contains(label)) {
                Object handler = onPress.get(child);
                Method press = null;
                for (Method m : onPress.getType().getMethods()) {
                    if (m.getParameterCount() == 1 && !m.isDefault() && !java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                        press = m;
                    }
                }
                System.out.println("[testclient] WARNING SCREEN continued: \"" + title + "\" via \"" + label + "\" ("
                        + screen.getClass().getName() + ")");
                Method call = press;
                execute.invoke(minecraft, (Runnable) () -> invokeQuietly(call, handler, child));
                return;
            }
        }
        System.out.println("[testclient] screen has no continue button, left alone: " + describeScreen(screen));
    }

    private static Object readField(Object target, String name) {
        for (Class<?> k = target.getClass(); k != null; k = k.getSuperclass()) {
            try {
                Field field = k.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                // look further up
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private String screenTitle(Object screen) {
        try {
            Class<?> screenBase = findSuper(screen.getClass(), "net.minecraft.client.gui.screens.Screen");
            Field titleField = screenBase.getDeclaredField("title");
            titleField.setAccessible(true);
            Object title = titleField.get(screen);
            return title == null ? "" : String.valueOf(title.getClass().getMethod("getString").invoke(title));
        } catch (Throwable t) {
            return "?";
        }
    }

    /** Every text-like field of the screen (its message, reason, details), for an error report. */
    private String screenText(Object screen) {
        StringBuilder out = new StringBuilder();
        for (Class<?> k = screen.getClass(); k != null && !k.getName().equals("net.minecraft.client.gui.screens.Screen"); k = k.getSuperclass()) {
            for (Field field : k.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(screen);
                    if (value == null) {
                        continue;
                    }
                    String text;
                    try {
                        text = String.valueOf(value.getClass().getMethod("getString").invoke(value));
                    } catch (NoSuchMethodException notAComponent) {
                        if (!(value instanceof CharSequence)) {
                            continue;
                        }
                        text = value.toString();
                    }
                    if (!text.isBlank()) {
                        out.append(field.getName()).append('=').append(text.replace('\n', ' ')).append("; ");
                    }
                } catch (Throwable ignored) {
                    // not readable, skip it
                }
            }
        }
        return out.toString();
    }

    private String describeScreen(Object screen) {
        return screen == null ? "none" : screen.getClass().getName() + " \"" + screenTitle(screen) + "\"";
    }

    private static void invokeQuietly(Method method, Object target, Object... args) {
        try {
            method.invoke(target, args);
        } catch (Throwable t) {
            System.err.println("[testclient] " + method.getName() + " failed: " + t);
        }
    }

    /** The first class in the hierarchy with that name. */
    private static Class<?> findSuper(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            if (k.getName().equals(name)) {
                return k;
            }
        }
        return null;
    }

    /** Opens the game package to this agent, in case the loader put Minecraft in a named module (Forge, NeoForge). */
    private void openPackage(Class<?> gameClass) {
        Module module = gameClass.getModule();
        if (module.isNamed() && !module.isOpen(gameClass.getPackageName(), RemoteControl.class.getModule())) {
            instrumentation.redefineModule(module, Set.of(), Map.of(),
                    Map.of(gameClass.getPackageName(), Set.of(RemoteControl.class.getModule())), Set.of(), Map.of());
        }
    }

    /**
     * Light Level Overlay: writes what the overlay currently holds around the player - one line per spawnable block,
     * "x y z blockLight skyLight danger spawnableNow currentLight" - using only the mod's public API
     * (LightLevelOverlayClient.getCache().getVisibleEntries, SpawnRules.of(level)). Works on release jars, in
     * multiplayer too, so a release check can compare the overlay's verdicts with what a test scene must give.
     */
    private void dumpOverlay(Class<?> minecraftClass, Object minecraft, String file) throws Exception {
        ClassLoader gameLoader = minecraftClass.getClassLoader();
        Class<?> clientClass = loadModClass("com.lightleveloverlay.LightLevelOverlayClient");
        Class<?> rulesClass = loadModClass("com.lightleveloverlay.data.SpawnRules");
        Class<?> entryClass = loadModClass("com.lightleveloverlay.data.LightDataEntry");
        Class<?> levelClass = Class.forName("net.minecraft.world.level.Level", false, gameLoader);
        Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity", false, gameLoader);
        Class<?> vec3iClass = Class.forName("net.minecraft.core.Vec3i", false, gameLoader);
        for (Class<?> c : new Class<?>[]{clientClass, rulesClass, entryClass, levelClass, entityClass, vec3iClass}) {
            openPackage(c);
        }
        Object player = minecraftClass.getField("player").get(minecraft);
        Object level = minecraftClass.getField("level").get(minecraft);
        if (player == null || level == null) {
            System.err.println("[testclient] overlaydump: no player yet");
            return;
        }
        double x = (Double) entityClass.getMethod("getX").invoke(player);
        double y = (Double) entityClass.getMethod("getY").invoke(player);
        double z = (Double) entityClass.getMethod("getZ").invoke(player);
        Object cache = clientClass.getMethod("getCache").invoke(null);
        java.util.List<?> entries = (java.util.List<?>) cache.getClass()
                .getMethod("getVisibleEntries", double.class, double.class, double.class).invoke(cache, x, y, z);
        Object rules = rulesClass.getMethod("of", levelClass).invoke(null, level);
        Method danger = rulesClass.getMethod("danger", entryClass);
        Method now = rulesClass.getMethod("canSpawnNow", entryClass);
        Method current = rulesClass.getMethod("currentLight", entryClass);
        Method pos = entryClass.getMethod("pos");
        Method block = entryClass.getMethod("blockLight");
        Method sky = entryClass.getMethod("skyLight");
        Method getX = vec3iClass.getMethod("getX"), getY = vec3iClass.getMethod("getY"), getZ = vec3iClass.getMethod("getZ");
        StringBuilder out = new StringBuilder();
        for (Object entry : entries) {
            Object p = pos.invoke(entry);
            out.append(getX.invoke(p)).append(' ').append(getY.invoke(p)).append(' ').append(getZ.invoke(p)).append(' ')
                    .append(block.invoke(entry)).append(' ').append(sky.invoke(entry)).append(' ')
                    .append(danger.invoke(rules, entry)).append(' ').append(now.invoke(rules, entry)).append(' ')
                    .append(current.invoke(rules, entry)).append('\n');
        }
        Path target = Path.of(file);
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        Files.writeString(target, out.toString());
        System.out.println("[testclient] overlaydump " + entries.size() + " entries, rules=" + rules + " -> " + file);
    }

    /**
     * A mod class by name, loaded if need be through the class loader of an already loaded class from the same
     * top-level package (the mod's own loader: Knot on Fabric/Quilt, the module layer on Forge/NeoForge).
     */
    private Class<?> loadModClass(String name) throws ClassNotFoundException {
        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) {
            return loaded;
        }
        String[] segments = name.split("\\.");
        String prefix = segments[0] + "." + segments[1] + ".";
        for (Class<?> c : instrumentation.getAllLoadedClasses()) {
            if (c.getName().startsWith(prefix) && c.getClassLoader() != null) {
                return Class.forName(name, true, c.getClassLoader());
            }
        }
        throw new ClassNotFoundException(name + " (no loaded class under " + prefix + ")");
    }

    private Class<?> findLoadedClass(String name) {
        for (Class<?> c : instrumentation.getAllLoadedClasses()) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    private void handle(Class<?> minecraftClass, String line) throws Exception {
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
        Method execute = minecraftClass.getMethod("execute", Runnable.class);
        if (line.equals("quit")) {
            // a clean shutdown (saves the world), so a finished run never leaves a client behind
            Method stop = minecraftClass.getMethod("stop");
            System.out.println("[testclient] quitting");
            execute.invoke(minecraft, (Runnable) () -> invokeQuietly(stop, minecraft));
            return;
        }
        if (line.startsWith("screenshot ")) {
            String name = line.substring("screenshot ".length()).trim();
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    ClassLoader loader = minecraftClass.getClassLoader();
                    Class<?> screenshot = Class.forName("net.minecraft.client.Screenshot", true, loader);
                    Class<?> renderTarget = Class.forName("com.mojang.blaze3d.pipeline.RenderTarget", true, loader);
                    File gameDirectory = (File) minecraftClass.getField("gameDirectory").get(minecraft);
                    Object gameRenderer = minecraftClass.getField("gameRenderer").get(minecraft);
                    Object target = gameRenderer.getClass().getMethod("mainRenderTarget").invoke(gameRenderer);
                    Consumer<Object> done = message -> System.out.println("[testclient] screenshot " + name + " saved");
                    screenshot.getMethod("grab", File.class, String.class, renderTarget, int.class, Consumer.class)
                            .invoke(null, gameDirectory, name + ".png", target, 1, done);
                } catch (Throwable t) {
                    System.err.println("[testclient] screenshot " + name + " failed: " + t);
                }
            });
        } else if (line.startsWith("cmd ")) {
            String command = line.substring("cmd ".length()).trim();
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    Object player = minecraftClass.getField("player").get(minecraft);
                    if (player == null) {
                        System.err.println("[testclient] no player yet for /" + command);
                        return;
                    }
                    Object connection = player.getClass().getField("connection").get(player);
                    connection.getClass().getMethod("sendCommand", String.class).invoke(connection, command);
                    System.out.println("[testclient] ran /" + command);
                } catch (Throwable t) {
                    System.err.println("[testclient] /" + command + " failed: " + t);
                }
            });
        } else if (line.equals("attack")) {
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    attackNearest(minecraftClass, minecraft);
                } catch (Throwable t) {
                    System.err.println("[testclient] attack failed: " + t);
                }
            });
        } else if (line.equals("report")) {
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    Object player = minecraftClass.getField("player").get(minecraft);
                    if (player == null) {
                        System.err.println("[testclient] no player yet for report");
                        return;
                    }
                    Class<?> living = Class.forName("net.minecraft.world.entity.LivingEntity", false, minecraftClass.getClassLoader());
                    float strength = (Float) player.getClass().getMethod("getAttackStrengthScale", float.class).invoke(player, 0.0F);
                    System.out.println("[testclient] report attackAnim=" + living.getField("attackAnim").getFloat(player)
                            + " swingTime=" + living.getField("swingTime").getInt(player) + " attackStrength=" + strength);
                } catch (Throwable t) {
                    System.err.println("[testclient] report failed: " + t);
                }
            });
        } else if (line.startsWith("presskey ")) {
            int code = Integer.parseInt(line.substring("presskey ".length()).trim());
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    ClassLoader loader = minecraftClass.getClassLoader();
                    Class<?> keyMapping = Class.forName("net.minecraft.client.KeyMapping", true, loader);
                    Class<?> keyClass = Class.forName("com.mojang.blaze3d.platform.InputConstants$Key", true, loader);
                    Class<?> typeClass = Class.forName("com.mojang.blaze3d.platform.InputConstants$Type", true, loader);
                    Object keysym = typeClass.getField("KEYSYM").get(null);
                    Object key = typeClass.getMethod("getOrCreate", int.class).invoke(keysym, code);
                    keyMapping.getMethod("click", keyClass).invoke(null, key);
                    System.out.println("[testclient] pressed key " + code);
                } catch (Throwable t) {
                    System.err.println("[testclient] presskey " + code + " failed: " + t);
                }
            });
        } else if (line.startsWith("keys ")) {
            String wanted = line.substring("keys ".length()).trim();
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    Object options = minecraftClass.getField("options").get(minecraft);
                    Object[] mappings = (Object[]) options.getClass().getField("keyMappings").get(options);
                    boolean found = false;
                    for (Object mapping : mappings) {
                        String name = String.valueOf(mapping.getClass().getMethod("getName").invoke(mapping));
                        if (name.contains(wanted)) {
                            found = true;
                            System.out.println("[testclient] key mapping registered: " + name + " = "
                                    + mapping.getClass().getMethod("saveString").invoke(mapping));
                        }
                    }
                    if (!found) {
                        System.out.println("[testclient] key mapping missing: " + wanted);
                    }
                } catch (Throwable t) {
                    System.err.println("[testclient] keys " + wanted + " failed: " + t);
                }
            });
        } else if (line.startsWith("overlaydump ")) {
            String file = line.substring("overlaydump ".length()).trim();
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    dumpOverlay(minecraftClass, minecraft, file);
                } catch (Throwable t) {
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    System.err.println("[testclient] overlaydump failed: " + cause);
                    cause.printStackTrace();
                }
            });
        } else if (line.startsWith("interact ")) {
            // Mo'Creatures: a right click on the nearest entity whose type id contains the argument
            String type = line.substring("interact ".length()).trim();
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    interactNearest(minecraftClass, minecraft, type);
                } catch (Throwable t) {
                    System.err.println("[testclient] interact failed: " + t);
                }
            });
        } else if (line.equals("overlay")) {
            // Mo'Creatures: logs the action-bar text (Hud.overlayMessageString)
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    Object gui = minecraftClass.getField("gui").get(minecraft);
                    Object hud = gui.getClass().getField("hud").get(gui);
                    openPackage(hud.getClass());
                    Field field = hud.getClass().getDeclaredField("overlayMessageString");
                    field.setAccessible(true);
                    Object message = field.get(hud);
                    String text = message == null ? "" : String.valueOf(message.getClass().getMethod("getString").invoke(message));
                    System.out.println("[testclient] overlay=" + text);
                } catch (Throwable t) {
                    System.err.println("[testclient] overlay failed: " + t);
                }
            });
        } else if (line.equals("use")) {
            // Mo'Creatures: a real right click with whatever the crosshair is on (Minecraft#startUseItem)
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    openPackage(minecraftClass);
                    Method use = minecraftClass.getDeclaredMethod("startUseItem");
                    use.setAccessible(true);
                    use.invoke(minecraft);
                    Object hit = minecraftClass.getField("hitResult").get(minecraft);
                    System.out.println("[testclient] used item, crosshair on " + (hit == null ? "nothing"
                            : hit.getClass().getMethod("getType").invoke(hit)));
                } catch (Throwable t) {
                    System.err.println("[testclient] use failed: " + t);
                }
            });
        } else if (line.startsWith("vehicle ")) {
            // Mo'Creatures: calls a no-argument method on the player's mount and logs the result
            String name = line.substring("vehicle ".length()).trim();
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    Object player = minecraftClass.getField("player").get(minecraft);
                    Object vehicle = player == null ? null : player.getClass().getMethod("getVehicle").invoke(player);
                    if (vehicle == null) {
                        System.out.println("[testclient] vehicle none");
                        return;
                    }
                    Object result = vehicle.getClass().getMethod(name).invoke(vehicle);
                    System.out.println("[testclient] vehicle " + name + "=" + result);
                } catch (Throwable t) {
                    System.err.println("[testclient] vehicle " + name + " failed: " + t);
                }
            });
        } else if (line.equals("closescreen")) {
            // Mo'Creatures: closes the open screen the way Escape does (Screen#onClose), e.g. the "Name your pet"
            // screen a horse tamed by riding opens, which would otherwise swallow every later click
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    Object screen = currentScreen(minecraftClass, minecraft);
                    if (screen != null) {
                        screen.getClass().getMethod("onClose").invoke(screen);
                    }
                    System.out.println("[testclient] closed screen " + (screen == null ? "none" : describeScreen(screen)));
                } catch (Throwable t) {
                    System.err.println("[testclient] closescreen failed: " + t);
                }
            });
        } else if (line.startsWith("look ")) {
            // Mo'Creatures: sets the local player's look direction (yaw, pitch in degrees) on the client, which is
            // what aims a following "use"; logs what it was before
            String[] parts = line.substring("look ".length()).trim().split("\\s+");
            float yaw = Float.parseFloat(parts[0]);
            float pitch = Float.parseFloat(parts[1]);
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    Object player = minecraftClass.getField("player").get(minecraft);
                    Class<?> entity = Class.forName("net.minecraft.world.entity.Entity", false,
                            minecraftClass.getClassLoader());
                    Object wasYaw = entity.getMethod("getYRot").invoke(player);
                    Object wasPitch = entity.getMethod("getXRot").invoke(player);
                    entity.getMethod("setYRot", float.class).invoke(player, yaw);
                    entity.getMethod("setXRot", float.class).invoke(player, pitch);
                    entity.getField("yRotO").setFloat(player, yaw);
                    entity.getField("xRotO").setFloat(player, pitch);
                    System.out.println("[testclient] look yaw=" + yaw + " pitch=" + pitch + " (was yaw=" + wasYaw
                            + " pitch=" + wasPitch + ")");
                } catch (Throwable t) {
                    System.err.println("[testclient] look failed: " + t);
                }
            });
        } else if (line.startsWith("camera ")) {
            // Mo'Creatures: FIRST_PERSON, THIRD_PERSON_BACK or THIRD_PERSON_FRONT (what F5 cycles through)
            String mode = line.substring("camera ".length()).trim();
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    ClassLoader loader = minecraftClass.getClassLoader();
                    Class<?> cameraType = Class.forName("net.minecraft.client.CameraType", true, loader);
                    Object options = minecraftClass.getField("options").get(minecraft);
                    options.getClass().getMethod("setCameraType", cameraType)
                            .invoke(options, cameraType.getField(mode).get(null));
                    System.out.println("[testclient] camera " + mode);
                } catch (Throwable t) {
                    System.err.println("[testclient] camera " + mode + " failed: " + t);
                }
            });
        } else if (line.startsWith("invoke ")) {
            String[] parts = line.substring("invoke ".length()).trim().split("\\s+");
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    Class<?> target = loadModClass(parts[0]);
                    openPackage(target);
                    Method method = target.getMethod(parts[1]);
                    Object result = method.invoke(null);
                    System.out.println("[testclient] invoked " + parts[0] + "." + parts[1] + (result == null ? "" : " -> " + result));
                } catch (Throwable t) {
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    System.err.println("[testclient] invoke " + String.join(" ", parts) + " failed: " + cause);
                    cause.printStackTrace();
                }
            });
        } else {
            System.err.println("[testclient] unknown remote command: " + line);
        }
    }

    /**
     * What a left click with the crosshair on an entity does ({@code Minecraft#startAttack}): the game mode's attack
     * (the attack packet, then the client's own prediction) and a main-hand swing. The target is the nearest other
     * living entity within 4 blocks, so no aiming is needed.
     */
    private void attackNearest(Class<?> minecraftClass, Object minecraft) throws Exception {
        Object player = minecraftClass.getField("player").get(minecraft);
        Object level = minecraftClass.getField("level").get(minecraft);
        if (player == null || level == null) {
            System.err.println("[testclient] no player yet for attack");
            return;
        }
        ClassLoader loader = minecraftClass.getClassLoader();
        Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity", false, loader);
        Class<?> livingClass = Class.forName("net.minecraft.world.entity.LivingEntity", false, loader);
        Class<?> playerClass = Class.forName("net.minecraft.world.entity.player.Player", false, loader);
        Class<?> handClass = Class.forName("net.minecraft.world.InteractionHand", false, loader);
        Method distanceTo = entityClass.getMethod("distanceTo", entityClass);
        Object target = null;
        float nearest = 4.0F;
        for (Object entity : (Iterable<?>) level.getClass().getMethod("entitiesForRendering").invoke(level)) {
            if (entity != player && livingClass.isInstance(entity)) {
                float distance = (Float) distanceTo.invoke(player, entity);
                if (distance < nearest) {
                    nearest = distance;
                    target = entity;
                }
            }
        }
        if (target == null) {
            System.err.println("[testclient] attack: no living entity within 4 blocks");
            return;
        }
        Object gameMode = minecraftClass.getField("gameMode").get(minecraft);
        gameMode.getClass().getMethod("attack", playerClass, entityClass).invoke(gameMode, player, target);
        player.getClass().getMethod("swing", handClass).invoke(player, handClass.getField("MAIN_HAND").get(null));
        System.out.println("[testclient] attacked " + target.getClass().getSimpleName() + " at " + nearest + " blocks");
    }

    private void interactNearest(Class<?> minecraftClass, Object minecraft, String type) throws Exception {
        Object player = minecraftClass.getField("player").get(minecraft);
        Object level = minecraftClass.getField("level").get(minecraft);
        if (player == null || level == null) {
            System.err.println("[testclient] no player yet for interact");
            return;
        }
        ClassLoader loader = minecraftClass.getClassLoader();
        Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity", false, loader);
        Class<?> entityTypeClass = Class.forName("net.minecraft.world.entity.EntityType", false, loader);
        Class<?> playerClass = Class.forName("net.minecraft.world.entity.player.Player", false, loader);
        Class<?> handClass = Class.forName("net.minecraft.world.InteractionHand", false, loader);
        Class<?> hitClass = Class.forName("net.minecraft.world.phys.EntityHitResult", false, loader);
        Method distanceTo = entityClass.getMethod("distanceTo", entityClass);
        Method getType = entityClass.getMethod("getType");
        Method getKey = entityTypeClass.getMethod("getKey", entityTypeClass);
        Object target = null;
        float nearest = 6.0F;
        for (Object entity : (Iterable<?>) level.getClass().getMethod("entitiesForRendering").invoke(level)) {
            if (entity == player || !String.valueOf(getKey.invoke(null, getType.invoke(entity))).contains(type)) {
                continue;
            }
            float distance = (Float) distanceTo.invoke(player, entity);
            if (distance < nearest) {
                nearest = distance;
                target = entity;
            }
        }
        if (target == null) {
            System.err.println("[testclient] interact: no " + type + " within 6 blocks");
            return;
        }
        Object hand = handClass.getField("MAIN_HAND").get(null);
        Object hit = hitClass.getConstructor(entityClass).newInstance(target);
        Object gameMode = minecraftClass.getField("gameMode").get(minecraft);
        Object result = gameMode.getClass().getMethod("interact", playerClass, entityClass, hitClass, handClass)
                .invoke(gameMode, player, target, hit, hand);
        player.getClass().getMethod("swing", handClass).invoke(player, hand);
        System.out.println("[testclient] interacted with " + getKey.invoke(null, getType.invoke(target)) + " at " + nearest
                + " blocks: " + result);
    }
}
