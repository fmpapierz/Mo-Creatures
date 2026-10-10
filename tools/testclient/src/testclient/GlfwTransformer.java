package testclient;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * The GLFW patches described on {@link MinimizedWindowAgent}, written with the JDK's {@code java.lang.classfile} API
 * (Java 25 no longer carries the internal copy of ASM that the 1.21.x agent used).
 * <p>
 * All patches go through one class transform that looks each method up in a table. Do not chain per-method
 * transforms with {@code ClassTransform.andThen}: on JDK 25 two chained {@code transformingMethods} transforms leave
 * the class unchanged, without any error (the first version of this agent patched nothing that way).
 */
final class GlfwTransformer implements ClassFileTransformer {
    private static final ClassDesc GLFW = ClassDesc.of("org.lwjgl.glfw.GLFW");
    private static final ClassDesc GLFW_NATIVE_WIN32 = ClassDesc.of("org.lwjgl.glfw.GLFWNativeWin32");
    private static final ClassDesc USER32 = ClassDesc.of("org.lwjgl.system.windows.User32");
    private static final MethodTypeDesc WINDOW_HINT = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int, ConstantDescs.CD_int);
    private static final MethodTypeDesc GET_WIN32_WINDOW = MethodTypeDesc.of(ConstantDescs.CD_long, ConstantDescs.CD_long);
    private static final MethodTypeDesc SHOW_WINDOW = MethodTypeDesc.of(ConstantDescs.CD_boolean, ConstantDescs.CD_long, ConstantDescs.CD_int);
    private static final MethodTypeDesc GET_WINDOW_ATTRIB = MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_long, ConstantDescs.CD_int);
    private static final ClassDesc SYSTEM = ClassDesc.of("java.lang.System");
    private static final ClassDesc PRINT_STREAM = ClassDesc.of("java.io.PrintStream");
    private static final DirectMethodHandleDesc CONCAT = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC,
            ClassDesc.of("java.lang.invoke.StringConcatFactory"), "makeConcatWithConstants",
            MethodTypeDesc.of(ConstantDescs.CD_CallSite, ConstantDescs.CD_MethodHandles_Lookup, ConstantDescs.CD_String,
                    ConstantDescs.CD_MethodType, ConstantDescs.CD_String, ConstantDescs.CD_Object.arrayType()));
    private static final int GLFW_ICONIFIED = 0x00020002;
    private static final int GLFW_FOCUSED = 0x00020001;
    private static final int GLFW_VISIBLE = 0x00020004;
    private static final int GLFW_FOCUS_ON_SHOW = 0x0002000C;
    private static final int SW_SHOWMINNOACTIVE = 7;
    /** GLFW calls that restore or activate a window; Minecraft makes none of them, so each one prints its caller. */
    private static final String[] TRACED = {"glfwFocusWindow(J)V", "glfwRestoreWindow(J)V", "glfwMaximizeWindow(J)V",
            "glfwRequestWindowAttention(J)V"};

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> redefined, ProtectionDomain domain,
                            byte[] bytes) {
        if (!"org/lwjgl/glfw/GLFW".equals(className)) {
            return null;
        }
        try {
            Map<String, CodeTransform> patches = new LinkedHashMap<>();
            patches.put("nglfwCreateWindow(IIJJJ)J", atStart(GlfwTransformer::createHiddenAndUnfocused));
            patches.put("glfwSetWindowIconifyCallback(JLorg/lwjgl/glfw/GLFWWindowIconifyCallbackI;)Lorg/lwjgl/glfw/GLFWWindowIconifyCallback;",
                    beforeReturn(code -> showMinimizedNoActivate(code, "window created")));
            // Replaced, not prefixed: GLFW's own show (SW_SHOWNA, then a focus if GLFW_FOCUS_ON_SHOW is set) must not run
            // at all; the window has been shown minimized already.
            patches.put("glfwShowWindow(J)V", replaceBody(code -> showMinimizedNoActivate(code, "glfwShowWindow")));
            for (String traced : TRACED) {
                patches.put(traced, atStart(GlfwTransformer::dumpStack));
            }
            TreeSet<String> missing = new TreeSet<>(patches.keySet());
            ClassTransform transform = (builder, element) -> {
                if (element instanceof MethodModel method) {
                    String key = method.methodName().stringValue() + method.methodType().stringValue();
                    CodeTransform patch = patches.get(key);
                    if (patch != null && method.code().isPresent()) {
                        missing.remove(key);
                        builder.transformMethod(method, MethodTransform.transformingCode(patch));
                        return;
                    }
                }
                builder.with(element);
            };
            ClassFile classFile = ClassFile.of();
            byte[] patched = classFile.transformClass(classFile.parse(bytes), transform);
            if (missing.isEmpty()) {
                System.out.println("[testclient] patched " + patches.size() + " GLFW methods: the game window will open minimized and unfocused");
            } else {
                System.err.println("[testclient] WARNING: GLFW methods not found, the game window may come to the front: " + missing);
            }
            return patched;
        } catch (Throwable t) {
            System.err.println("[testclient] could not patch GLFW window creation: " + t);
            return null;
        }
    }

    /** Emits {@code code} before the method's first instruction. */
    private static CodeTransform atStart(Consumer<CodeBuilder> code) {
        return new CodeTransform() {
            @Override
            public void atStart(CodeBuilder builder) {
                code.accept(builder);
            }

            @Override
            public void accept(CodeBuilder builder, CodeElement element) {
                builder.with(element);
            }
        };
    }

    /** Replaces a void method's body with {@code code} followed by a return. */
    private static CodeTransform replaceBody(Consumer<CodeBuilder> code) {
        return new CodeTransform() {
            @Override
            public void atStart(CodeBuilder builder) {
                code.accept(builder);
                builder.return_();
            }

            @Override
            public void accept(CodeBuilder builder, CodeElement element) {
                // the original body is dropped
            }
        };
    }

    /** Emits {@code code} before every reference return. */
    private static CodeTransform beforeReturn(Consumer<CodeBuilder> code) {
        return (builder, element) -> {
            if (element instanceof ReturnInstruction ret && ret.opcode() == Opcode.ARETURN) {
                code.accept(builder);
            }
            builder.with(element);
        };
    }

    /** Creates the window hidden and unfocused (hidden, not minimized: a minimized window reports a 0x0 framebuffer). */
    private static void createHiddenAndUnfocused(CodeBuilder code) {
        for (int hint : new int[]{GLFW_VISIBLE, GLFW_FOCUSED, GLFW_FOCUS_ON_SHOW}) {
            code.ldc(hint).iconst_0().invokestatic(GLFW, "glfwWindowHint", WINDOW_HINT);
        }
    }

    /**
     * Stack-neutral: ShowWindow(glfwGetWin32Window(window in local 0), SW_SHOWMINNOACTIVE), then prints the window's
     * state as GLFW sees it, so a test log shows whether the window really is minimized.
     */
    private static void showMinimizedNoActivate(CodeBuilder code, String where) {
        code.getstatic(SYSTEM, "out", PRINT_STREAM)
                .lload(0)
                .invokestatic(GLFW_NATIVE_WIN32, "glfwGetWin32Window", GET_WIN32_WINDOW)
                .bipush(SW_SHOWMINNOACTIVE)
                .invokestatic(USER32, "ShowWindow", SHOW_WINDOW);
        for (int attrib : new int[]{GLFW_VISIBLE, GLFW_ICONIFIED, GLFW_FOCUSED}) {
            code.lload(0).ldc(attrib).invokestatic(GLFW, "glfwGetWindowAttrib", GET_WINDOW_ATTRIB);
        }
        code.invokedynamic(DynamicCallSiteDesc.of(CONCAT, "makeConcatWithConstants",
                        MethodTypeDesc.of(ConstantDescs.CD_String, ConstantDescs.CD_boolean, ConstantDescs.CD_int,
                                ConstantDescs.CD_int, ConstantDescs.CD_int),
                        "[testclient] " + where + ": ShowWindow(SW_SHOWMINNOACTIVE) wasVisible=\u0001 -> visible=\u0001"
                                + " iconified=\u0001 focused=\u0001"))
                .invokevirtual(PRINT_STREAM, "println", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String));
    }

    private static void dumpStack(CodeBuilder code) {
        code.invokestatic(ClassDesc.of("java.lang.Thread"), "dumpStack", MethodTypeDesc.of(ConstantDescs.CD_void));
    }
}
