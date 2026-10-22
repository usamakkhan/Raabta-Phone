package org.rabta.gradle;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Adapt Commons' upstream-brand checks to the independent Raabta package.
 *
 * Commons 6.2.0 otherwise calls showModdedAppWarning() for every non-Fossify
 * package, and refuses color customization after 100 runs. Only the two
 * package-name constants in BaseSimpleActivity are changed. The actual Android
 * package name, third-party source attribution, and all other library logic
 * remain untouched.
 */
public abstract class RaabtaCommonsVisitorFactory
        implements AsmClassVisitorFactory<RaabtaCommonsVisitorFactory.Parameters> {

    public interface Parameters extends InstrumentationParameters {}

    @Override
    public boolean isInstrumentable(ClassData classData) {
        return "org.fossify.commons.activities.BaseSimpleActivity"
                .equals(classData.getClassName());
    }

    @Override
    public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor nextClassVisitor) {
        return new ClassVisitor(Opcodes.ASM9, nextClassVisitor) {
            private boolean startupCheckAdapted;
            private boolean customizationCheckAdapted;

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor next = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!"onCreate".equals(name) && !"startCustomizationActivity".equals(name)) {
                    return next;
                }
                return new MethodVisitor(Opcodes.ASM9, next) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if ("onCreate".equals(name) && "org.fossify.".equals(value)) {
                            startupCheckAdapted = true;
                            super.visitLdcInsn("org.rabta.");
                        } else if ("startCustomizationActivity".equals(name)
                                && "yfissof".equals(value)) {
                            customizationCheckAdapted = true;
                            super.visitLdcInsn("atbar"); // reversed by Commons to "rabta"
                        } else {
                            super.visitLdcInsn(value);
                        }
                    }
                };
            }

            @Override
            public void visitEnd() {
                if (!startupCheckAdapted || !customizationCheckAdapted) {
                    throw new IllegalStateException(
                            "Commons package checks changed; review Raabta's warning adaptation before release");
                }
                super.visitEnd();
            }
        };
    }
}
