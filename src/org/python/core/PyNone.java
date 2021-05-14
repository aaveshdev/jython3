// Copyright (c) Corporation for National Research Initiatives
// Copyright (c) Jython Developers
package org.python.core;

import java.io.Serializable;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * The singleton None object.
 */
@Untraversable
@ExposedType(name = "NoneType", isBaseType = false)
public class PyNone extends PyObject implements Serializable {

    public static final PyType TYPE = PyType.fromClass(PyNone.class);

    private PyNone() {/* Not super(TYPE): TYPE is null until too late in bootstrapping. */}

    private static class Holder {

        /** The unique instance of PyNone. */
        static final PyNone INSTANCE = new PyNone();
    }

    static PyNone getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean __nonzero__() {
        return false;
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (c == PyObject.class) {
            return this;
        }
        if (c.isPrimitive()) {
            return Py.NoConversion;
        }
        // Java gets null
        return null;
    }

    @Override
    public String toString() {
        return NoneType_toString();
    }

    @ExposedMethod(names = "__repr__")
    final String NoneType_toString() {
        return "None";
    }

    @Override
    public String asStringOrNull(int index) {
        return null;
    }

    @Override
    public String asStringOrNull() {
        return null;
    }

    private Object writeReplace() {
        return new Py.SingletonResolver("None");
    }
}
