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

    private void answerPrompts(Class<?> minecraftClass) throws Exception {
        Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
        if (minecraft == null) {
            return;
        }
        Object screen = currentScreen(minecraftClass, minecraft);
        if (screen == null || screen == answered) {
            return;
        }
        Class<?> screenBase = findSuper(screen.getClass(), "net.minecraft.client.gui.screens.Screen");
        if (screenBase == null) {
            return;
        }
        openPackage(screenBase);
        Method execute = minecraftClass.getMethod("execute", Runnable.class);

        if (findSuper(screen.getClass(), "net.minecraft.client.gui.screens.AccessibilityOnboardingScreen") != null) {
            answered = screen;
            System.out.println("[testclient] continuing past the accessibility/narrator onboarding screen");
            Method onClose = screenBase.getMethod("onClose");
            execute.invoke(minecraft, (Runnable) () -> invokeQuietly(onClose, screen));
            return;
        }

        Field titleField = screenBase.getDeclaredField("title");
        titleField.setAccessible(true);
        Object title = titleField.get(screen);
        String text = title == null ? "" : String.valueOf(title.getClass().getMethod("getString").invoke(title));
        if (!text.contains("Experimental")) {
            return;
        }
        Class<?> backup = findSuper(screen.getClass(), "net.minecraft.client.gui.screens.BackupConfirmScreen");
        Class<?> confirm = findSuper(screen.getClass(), "net.minecraft.client.gui.screens.ConfirmScreen");
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
            System.out.println("[testclient] accepting \"" + text + "\" (load without backup)");
            Method call = proceed;
            execute.invoke(minecraft, (Runnable) () -> invokeQuietly(call, listener, false, false));
        } else if (confirm != null) {
            Field field = confirm.getDeclaredField("callback");
            field.setAccessible(true);
            Object callback = field.get(screen);
            answered = screen;
            System.out.println("[testclient] accepting \"" + text + "\"");
            Method accept = field.getType().getMethod("accept", boolean.class);
            execute.invoke(minecraft, (Runnable) () -> invokeQuietly(accept, callback, true));
        }
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
        } else if (line.startsWith("interact ")) {
            String type = line.substring("interact ".length()).trim();
            execute.invoke(minecraft, (Runnable) () -> {
                try {
                    interactNearest(minecraftClass, minecraft, type);
                } catch (Throwable t) {
                    System.err.println("[testclient] interact failed: " + t);
                }
            });
        } else if (line.equals("overlay")) {
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

    /**
     * What a right click with the crosshair on an entity does ({@code Minecraft#startUseItem}'s ENTITY case): the game
     * mode's interact (the interact packet, then the client's own prediction) with the main hand. The target is the
     * nearest entity within 6 blocks whose type id contains {@code type}, so no aiming is needed.
     */
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
