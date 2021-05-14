/*
 *
 */
package org.python.modules.jffi;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
final class JITHandle {

    private static final int THRESHOLD = Integer.getInteger("jython.ctypes.compile.threshold", 100);
    private final JITSignature jitSignature;
    private volatile boolean compilationFailed = false;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final JITCompiler compiler;
    private WeakReference<Class<? extends Invoker>> compiledClassRef = null;

    JITHandle(JITCompiler compiler, JITSignature signature, boolean compilationFailed) {
        this.compiler = compiler;
        this.jitSignature = signature;
        this.compilationFailed = compilationFailed;
    }

    final boolean compilationFailed() {
        return compilationFailed;
    }

    final Invoker compile(com.kenai.jffi.Function function, NativeDataConverter resultConverter, NativeDataConverter[] parameterConverters) {
        if (compilationFailed || counter.incrementAndGet() < THRESHOLD) {
            return null;
        }

        Class<? extends Invoker> compiledClass;
        synchronized (this) {
            if (compiledClassRef == null || (compiledClass = compiledClassRef.get()) == null) {
                compiledClass = newInvokerClass(jitSignature);
                if (compiledClass == null) {
                    compilationFailed = true;
                    return null;
                }
                compiler.registerClass(this, compiledClass);
                compiledClassRef = new WeakReference<Class<? extends Invoker>>(compiledClass);
            }
        }

        try {
            Constructor<? extends Invoker> cons = compiledClass.getDeclaredConstructor(com.kenai.jffi.Function.class,
                    NativeDataConverter.class, NativeDataConverter[].class, Invoker.class);
            return cons.newInstance(function, resultConverter, parameterConverters,
                    createFallbackInvoker(function, jitSignature));
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    Class<? extends Invoker> newInvokerClass(JITSignature jitSignature) {

        JITMethodGenerator generator = null;
        JITMethodGenerator[] generators = {
            new FastIntMethodGenerator(),
            new FastLongMethodGenerator(),
            new FastNumericMethodGenerator(),};

        for (int i = 0; i < generators.length; i++) {
            if (generators[i].isSupported(jitSignature)) {
                generator = generators[i];
                break;
            }
        }

        if (generator == null) {
            return null;
        }

        return new AsmClassBuilder(generator, jitSignature).build();
    }
    
    
    static Invoker createFallbackInvoker(com.kenai.jffi.Function function, JITSignature signature) {
        NativeType[] parameterTypes = new NativeType[signature.getParameterCount()];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = signature.getParameterType(i);
        }

        return DefaultInvokerFactory.getFactory().createInvoker(function, parameterTypes, signature.getResultType());
    }
}
