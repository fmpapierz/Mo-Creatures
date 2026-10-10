package testclient;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.MethodModel;
import java.lang.classfile.MethodTransform;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * No loader error window in a test run (user rule: a failed launch is detected and handled, never left on the user's
 * screen). NeoForge's FML (26.2: fancymodloader 11) shows a modal "Fatal Error" box for every startup failure - it even
 * forces {@code java.awt.headless=false} first, and has no switch to turn the box off. In test runs this rewrites
 * {@code net.neoforged.fml.startup.FatalErrorReporting#reportFatalError(String)} to print the message to stderr and
 * exit with code 1, which is what FML does after the box is closed. The test scripts read the message from the log
 * (tools/testclient/launchwatch.py). Fabric and Quilt Loader have properties for the same thing; the agent sets them
 * (see {@link MinimizedWindowAgent}).
 */
final class FatalDialogTransformer implements ClassFileTransformer {
    private static final String TARGET = "net/neoforged/fml/startup/FatalErrorReporting";
    private static final String METHOD = "reportFatalError(Ljava/lang/String;)V";
    private static final ClassDesc SYSTEM = ClassDesc.of("java.lang.System");
    private static final ClassDesc PRINT_STREAM = ClassDesc.of("java.io.PrintStream");
    private static final DirectMethodHandleDesc CONCAT = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC,
            ClassDesc.of("java.lang.invoke.StringConcatFactory"), "makeConcatWithConstants",
            MethodTypeDesc.of(ConstantDescs.CD_CallSite, ConstantDescs.CD_MethodHandles_Lookup, ConstantDescs.CD_String,
                    ConstantDescs.CD_MethodType, ConstantDescs.CD_String, ConstantDescs.CD_Object.arrayType()));

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> redefined, ProtectionDomain domain,
                            byte[] bytes) {
        if (!TARGET.equals(className)) {
            return null;
        }
        try {
            boolean[] patched = {false};
            ClassTransform transform = (builder, element) -> {
                if (element instanceof MethodModel method
                        && METHOD.equals(method.methodName().stringValue() + method.methodType().stringValue())
                        && method.code().isPresent()) {
                    patched[0] = true;
                    builder.transformMethod(method, MethodTransform.transformingCode(new CodeTransform() {
                        @Override
                        public void atStart(CodeBuilder code) {
                            code.getstatic(SYSTEM, "err", PRINT_STREAM)
                                    .aload(0)
                                    .invokedynamic(DynamicCallSiteDesc.of(CONCAT, "makeConcatWithConstants",
                                            MethodTypeDesc.of(ConstantDescs.CD_String, ConstantDescs.CD_String),
                                            "[testclient] NeoForge fatal startup error (its dialog is suppressed in test runs):\n\u0001"))
                                    .invokevirtual(PRINT_STREAM, "println", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String))
                                    .iconst_1()
                                    .invokestatic(SYSTEM, "exit", MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_int))
                                    .return_();
                        }

                        @Override
                        public void accept(CodeBuilder code, CodeElement element) {
                            // the original body (the Swing / TinyFD dialog) is dropped
                        }
                    }));
                    return;
                }
                builder.with(element);
            };
            ClassFile classFile = ClassFile.of();
            byte[] result = classFile.transformClass(classFile.parse(bytes), transform);
            System.out.println(patched[0]
                    ? "[testclient] NeoForge fatal error dialog replaced by a log message and exit(1)"
                    : "[testclient] WARNING: FatalErrorReporting.reportFatalError(String) not found; NeoForge may open its error dialog");
            return patched[0] ? result : null;
        } catch (Throwable t) {
            System.err.println("[testclient] could not patch FatalErrorReporting: " + t);
            return null;
        }
    }
}
