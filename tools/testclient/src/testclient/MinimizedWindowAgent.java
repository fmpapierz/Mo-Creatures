package testclient;

import java.lang.instrument.Instrumentation;

/**
 * Java agent for unattended test clients (Windows): the game window never takes keyboard focus and sits minimized
 * in the taskbar, so a test launch never pulls focus away from whatever the user is working in.
 * <p>
 * It patches LWJGL's {@code org.lwjgl.glfw.GLFW} class, which no loader remaps, so the same agent works on Fabric,
 * Quilt, Forge and NeoForge, in development and in production (Minecraft 26.2 still opens a GLFW window):
 * <ul>
 * <li>{@code nglfwCreateWindow} (the native entry every {@code glfwCreateWindow} overload goes through) first sets
 * {@code GLFW_VISIBLE}, {@code GLFW_FOCUSED} and {@code GLFW_FOCUS_ON_SHOW} to false, so the window is created hidden
 * and unfocused. Hidden rather than minimized: Minecraft reads the framebuffer size right after creation, and a
 * minimized window reports 0x0.</li>
 * <li>{@code glfwSetWindowIconifyCallback} - the last call of Minecraft's {@code Window} constructor, after the
 * framebuffer size has been read - then shows the window minimized without activating it
 * ({@code ShowWindow(SW_SHOWMINNOACTIVE)}).</li>
 * <li>{@code glfwShowWindow} does the same instead of GLFW's own show, for windows shown explicitly (Minecraft creates
 * its window hidden and shows it once loading starts; a loader's early loading screen does the same). Instead, not as
 * well: GLFW's show ends with a focus whenever {@code GLFW_FOCUS_ON_SHOW} is set, and that activates even a minimized
 * window.</li>
 * <li>{@code glfwFocusWindow}, {@code glfwRestoreWindow}, {@code glfwMaximizeWindow} and
 * {@code glfwRequestWindowAttention} print a stack trace: Minecraft calls none of them, so one in a test log names
 * whatever brought the window up.</li>
 * </ul>
 * Each of the two shows prints the window's state afterwards ({@code [testclient] ... visible=1 iconified=1
 * focused=0}), so every test log records whether the window really stayed minimized.
 * Minecraft keeps rendering while minimized, so in-game screenshots still work, and {@link RemoteControl} lets a
 * test script take them (and run commands) with no input at all. Never part of the mod: only a test launch puts it
 * on the command line.
 */
public final class MinimizedWindowAgent {
    private MinimizedWindowAgent() {
    }

    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new GlfwTransformer());
        // Scriptable screenshots and commands for the minimized client, and the startup prompts (see RemoteControl).
        RemoteControl.start(instrumentation);
    }
}
