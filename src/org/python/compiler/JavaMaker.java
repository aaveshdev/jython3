// Copyright (c) Corporation for National Research Initiatives
package org.python.compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.python.core.PyObject;

public class JavaMaker extends ProxyMaker {

    public String pythonClass, pythonModule;

    PyObject methods;

    public JavaMaker(Class<?> superclass,
                     Class<?>[] interfaces,
                     String pythonClass,
                     String pythonModule,
                     String myClass,
                     PyObject methods) {
        super(myClass, superclass, interfaces);
        this.pythonClass = pythonClass;
        this.pythonModule = pythonModule;
        this.methods = methods;
    }

    @Override
    public void addConstructor(String name,
                               Class<?>[] parameters,
                               Class<?> ret,
                               String sig,
                               int access) throws Exception {
        /* Need a fancy constructor for the Java side of things */
        Code code = classfile.addMethod("<init>", sig, access);
        callSuper(code, "<init>", name, parameters, Void.TYPE, false);
        callInitProxy(parameters, code);
    }

    @Override
    public void addProxy() throws Exception {
        if (methods != null)
            super.addProxy();

        // _initProxy method
        Code code = classfile.addMethod("__initProxy__", makeSig("V", $objArr), Modifier.PUBLIC);

        code.visitVarInsn(ALOAD, 0);
        code.visitLdcInsn(pythonModule);
        code.visitLdcInsn(pythonClass);
        code.visitVarInsn(ALOAD, 1);
        code.visitMethodInsn(INVOKESTATIC, "org/python/core/Py", "initProxy",
            makeSig("V", $pyProxy, $str, $str, $objArr), false);
        code.visitInsn(RETURN);

    }

    @Override
    public void addMethod(Method method, int access) throws Exception {
        if (Modifier.isAbstract(access)) {
            // Maybe throw an exception here???
            super.addMethod(method, access);
        } else if (methods.__finditem__(method.getName().intern()) != null) {
            super.addMethod(method, access);
        } else if (Modifier.isProtected(method.getModifiers())) {
            addSuperMethod(method, access);
        }
    }
}
