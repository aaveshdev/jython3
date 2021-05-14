/* Copyright (c) Jython Developers */
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * The builtin python bool. It would be nice if it didn't extend PyInteger,
 * but too hard to avoid pre-Python 2.2 semantics here.
 */
@Untraversable
@ExposedType(name = "bool", isBaseType = false, doc = BuiltinDocs.bool_doc)
public class PyBoolean extends PyInteger {

    public static final PyType TYPE = PyType.fromClass(PyBoolean.class);

    private final boolean value;

    public boolean getBooleanValue() {
        return value;
    }

    @Override
    public int getValue() {
        return getBooleanValue() ? 1 : 0;
    }

    public PyBoolean(boolean value) {
        super(TYPE, value ? 1 : 0); // XXX is this necessary?
        this.value = value;
    }

    @ExposedNew
    public static PyObject bool_new(PyNewWrapper new_, boolean init, PyType subtype,
                                    PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("bool", args, keywords, new String[] {"x"}, 0);
        PyObject obj = ap.getPyObject(0, null);
        if (obj == null) {
            return Py.False;
        }
        return obj.__nonzero__() ? Py.True : Py.False;
    }

    @Override
    public String toString() {
        return bool_toString();
    }

    @ExposedMethod(names = {"__str__", "__repr__"}, doc = BuiltinDocs.bool___str___doc)
    final String bool_toString() {
        return getBooleanValue() ? "True" : "False";
    }

    @Override
    public int hashCode() {
        return bool___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.bool___hash___doc)
    final int bool___hash__() {
        return getBooleanValue() ? 1 : 0;
    }

    @Override
    public boolean __nonzero__() {
        return bool___nonzero__();
    }

    @ExposedMethod(doc = BuiltinDocs.bool___nonzero___doc)
    final boolean bool___nonzero__() {
        return getBooleanValue();
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (c == Boolean.TYPE || c == Boolean.class || c == Object.class ) {
            return Boolean.valueOf(getBooleanValue());
        }
        if (c == Integer.TYPE || c == Number.class || c == Integer.class) {
            return Integer.valueOf(getValue());
        }
        if (c == Byte.TYPE || c == Byte.class) {
            return Byte.valueOf((byte)(getValue()));
        }
        if (c == Short.TYPE || c == Short.class) {
            return Short.valueOf((short)(getValue()));
        }
        if (c == Long.TYPE || c == Long.class) {
            return Long.valueOf(getValue());
        }
        if (c == Float.TYPE || c == Float.class) {
            return Float.valueOf(getValue());
        }
        if (c == Double.TYPE || c == Double.class) {
            return Double.valueOf(getValue());
        }
        return super.__tojava__(c);
    }

    @Override
    public PyObject __and__(PyObject right) {
        return bool___and__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bool___and___doc)
    final PyObject bool___and__(PyObject right) {
    	if (right instanceof PyBoolean) {
	        return Py.newBoolean(getBooleanValue() & ((PyBoolean) right).getBooleanValue());
    	} else if (right instanceof PyInteger) {
            return Py.newInteger(getValue() & ((PyInteger)right).getValue());
        } else {
	    	return null;
	    }
    }

    @Override
    public PyObject __xor__(PyObject right) {
        return bool___xor__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bool___xor___doc)
    final PyObject bool___xor__(PyObject right) {
    	if (right instanceof PyBoolean) {
	        return Py.newBoolean(getBooleanValue() ^ ((PyBoolean) right).getBooleanValue());
    	} else if (right instanceof PyInteger) {
            return Py.newInteger(getValue() ^ ((PyInteger)right).getValue());
        } else {
	    	return null;
	    }
    }

    @Override
    public PyObject __or__(PyObject right) {
        return bool___or__(right);
    }

    @ExposedMethod(type = MethodType.BINARY, doc = BuiltinDocs.bool___or___doc)
    final PyObject bool___or__(PyObject right) {
    	if (right instanceof PyBoolean) {
	        return Py.newBoolean(getBooleanValue() | ((PyBoolean) right).getBooleanValue());
    	} else if (right instanceof PyInteger) {
            return Py.newInteger(getValue() | ((PyInteger)right).getValue());
        } else {
	    	return null;
	    }
    }

    @Override
    public PyObject __neg__() {
        return bool___neg__();
    }

    @ExposedMethod(doc = BuiltinDocs.bool___neg___doc)
    final PyObject bool___neg__() {
        return Py.newInteger(getBooleanValue() ? -1 : 0);
    }

    @Override
    public PyObject __pos__() {
        return bool___pos__();
    }

    @ExposedMethod(doc = BuiltinDocs.bool___pos___doc)
    final PyObject bool___pos__() {
        return Py.newInteger(getValue());
    }

    @Override
    public PyObject __abs__() {
        return bool___abs__();
    }

    @ExposedMethod(doc = BuiltinDocs.bool___abs___doc)
    final PyObject bool___abs__() {
        return Py.newInteger(getValue());
    }
}
