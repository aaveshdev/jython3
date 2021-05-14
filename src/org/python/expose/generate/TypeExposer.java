package org.python.expose.generate;

import java.util.Collection;
import java.util.Set;

import org.python.core.BytecodeLoader;
import org.python.expose.BaseTypeBuilder;
import org.python.expose.ExposedType;
import org.python.expose.TypeBuilder;
import org.objectweb.asm.Type;
import org.python.util.Generic;

/**
 * Generates a subclass of TypeBuilder to expose a class with the {@link ExposedType} annotation as
 * a builtin Python type.
 */
public class TypeExposer extends Exposer {

    private Type baseType;

    private boolean isBaseType;

    private String doc;

    private Type onType;

    private String name;

    private Collection<MethodExposer> methods;

    private Collection<DescriptorExposer> descriptors;

    private int numNames;

    private Exposer ne;

    public TypeExposer(Type onType,
                       Type baseType,
                       boolean isBaseType,
                       String doc,
                       String name,
                       Collection<MethodExposer> methods,
                       Collection<DescriptorExposer> descriptors,
                       Exposer ne) {
        super(BaseTypeBuilder.class, makeGeneratedName(onType));
        this.baseType = baseType;
        this.isBaseType = isBaseType;
        this.doc = doc;
        this.onType = onType;
        this.name = name;
        this.methods = methods;
        this.descriptors = descriptors;
        Set<String> names = Generic.set();
        for(DescriptorExposer exposer : descriptors) {
            if(!names.add(exposer.getName())) {
                throwDupe(exposer.getName());
            }
        }
        for(MethodExposer method : methods) {
            String[] methNames = method.getNames();
            for(String methName : methNames) {
                if(!names.add(methName)) {
                    throwDupe(methName);
                }
            }
            numNames += methNames.length;
        }
        this.ne = ne;
    }

    private void throwDupe(String exposedName) {
        throw new InvalidExposingException("Only one item may be exposed on a type with a given name[name="
                + exposedName + ", class=" + onType.getClassName() + "]");
    }

    public static String makeGeneratedName(Type onType) {
        return onType.getClassName() + "$PyExposer";
    }

    public TypeBuilder makeBuilder() {
        BytecodeLoader.Loader l = new BytecodeLoader.Loader();
        if(ne != null) {
            ne.load(l);
        }
        for(DescriptorExposer de : descriptors) {
            de.load(l);
        }
        for(MethodExposer me : methods) {
            me.load(l);
        }
        Class<?> descriptor = load(l);
        try {
            return (TypeBuilder)descriptor.getDeclaredConstructor().newInstance();
        } catch(Exception e) {
            // If we're unable to create the generated class, the process is
            // definitely ill, but that shouldn't be the case most of the time
            // so make this a runtime exception
            throw new RuntimeException("Unable to create generated builder", e);
        }
    }

    public String getName() {
        return name;
    }

    protected void generate() {
        startConstructor();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(getName());
        mv.visitLdcInsn(onType);
        mv.visitLdcInsn(baseType);
        mv.visitLdcInsn(isBaseType);
        if (doc == null) {
            mv.visitInsn(ACONST_NULL);
        } else {
            mv.visitLdcInsn(doc);
        }
        mv.visitLdcInsn(numNames);
        mv.visitTypeInsn(ANEWARRAY, BUILTIN_METHOD.getInternalName());
        mv.visitVarInsn(ASTORE, 1);
        int i = 0;
        for(MethodExposer exposer : methods) {
            for(final String name : exposer.getNames()) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitLdcInsn(i++);
                instantiate(exposer.getGeneratedType(), new Instantiator(STRING) {

                    public void pushArgs() {
                        mv.visitLdcInsn(name);
                    }
                });
                mv.visitInsn(AASTORE);
            }
        }
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(descriptors.size());
        mv.visitTypeInsn(ANEWARRAY, DATA_DESCR.getInternalName());
        mv.visitVarInsn(ASTORE, 2);
        i = 0;
        for(DescriptorExposer desc : descriptors) {
            mv.visitVarInsn(ALOAD, 2);
            mv.visitLdcInsn(i++);
            instantiate(desc.getGeneratedType());
            mv.visitInsn(AASTORE);
        }
        mv.visitVarInsn(ALOAD, 2);
        if(ne != null) {
            instantiate(ne.getGeneratedType());
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        superConstructor(STRING, CLASS, CLASS, BOOLEAN, STRING, ABUILTIN_METHOD, ADATA_DESCR,
                         PYNEWWRAPPER);
        endConstructor();
    }
}
