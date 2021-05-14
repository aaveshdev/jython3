package org.python.core;

import org.python.expose.ExposeAsSuperclass;

@Untraversable
public class PyBuiltinFunction extends PyBuiltinCallable implements ExposeAsSuperclass {

    protected PyBuiltinFunction(String name, String doc) {
        this(name, -1, -1, doc);
    }

    protected PyBuiltinFunction(String name, int minargs, int maxargs, String doc) {
        super(new DefaultInfo(name, minargs, maxargs));
        this.doc = doc == null ? null : doc;
    }

    public boolean isMappingType() {
        return false;
    }

    public boolean isNumberType() {
        return false;
    }

    public boolean isSequenceType() {
        return false;
    }

    public PyBuiltinCallable bind(PyObject self) {
        throw Py.TypeError("Can't bind a builtin function");
    }

    public String toString() {
        return "<built-in function " + info.getName() + ">";
    }
}
