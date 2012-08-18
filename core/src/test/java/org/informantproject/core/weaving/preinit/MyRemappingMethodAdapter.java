/**
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.informantproject.core.weaving.preinit;

import javax.annotation.Nullable;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
// from org.objectweb.asm.commons.RemappingMethodAdapter
class MyRemappingMethodAdapter extends LocalVariablesSorter {

    private final MethodCollector remapper;

    MyRemappingMethodAdapter(int access, String desc, MethodCollector remapper) {
        super(Opcodes.ASM4, access, desc, new MethodVisitor(Opcodes.ASM4) {});
        this.remapper = remapper;
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        remapEntries(nLocal, local);
        remapEntries(nStack, stack);
    }

    private Object[] remapEntries(int n, Object[] entries) {
        for (int i = 0; i < n; i++) {
            if (entries[i] instanceof String) {
                Object[] newEntries = new Object[n];
                if (i > 0) {
                    System.arraycopy(entries, 0, newEntries, 0, i);
                }
                do {
                    Object t = entries[i];
                    newEntries[i++] = t instanceof String
                            ? remapper.mapType((String) t)
                            : t;
                } while (i < n);
                return newEntries;
            }
        }
        return entries;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        remapper.mapType(owner);
        remapper.mapFieldName(owner, name, desc);
        remapper.mapDesc(desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        remapper.mapType(owner);
        remapper.mapMethodName(owner, name, desc);
        remapper.mapMethodDesc(desc);
        remapper.addReferencedMethod(ReferencedMethod.from(owner, name, desc));
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        for (int i = 0; i < bsmArgs.length; i++) {
            bsmArgs[i] = remapper.mapValue(bsmArgs[i]);
        }
        remapper.mapInvokeDynamicMethodName(name, desc);
        remapper.mapMethodDesc(desc);
        remapper.mapValue(bsm);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        remapper.mapType(type);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        remapper.mapValue(cst);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        remapper.mapDesc(desc);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, @Nullable String type) {
        if (type != null) {
            remapper.mapType(type);
        }
    }

    @Override
    public void visitLocalVariable(String name, String desc, @Nullable String signature,
            Label start, Label end, int index) {

        remapper.mapDesc(desc);
        remapper.mapSignature(signature, true);
    }
}
