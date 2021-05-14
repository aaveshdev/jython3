/* Copyright (c) Jython Developers */
package org.python.modules._weakref;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "weakref")
public class ReferenceType extends AbstractReference {

    public static final PyType TYPE = PyType.fromClass(ReferenceType.class);

    public ReferenceType(PyType subType, ReferenceBackend gref, PyObject callback) {
        super(subType, gref, callback);
    }

    public ReferenceType(ReferenceBackend gref, PyObject callback) {
        this(TYPE, gref, callback);
    }

    @ExposedNew
    static final PyObject weakref___new__(PyNewWrapper new_, boolean init, PyType subtype,
                                          PyObject[] args, String[] keywords) {
        ArgParser ap = parseInitArgs("__new__", args, keywords);
        PyObject ob = ap.getPyObject(0);
        PyObject callback = ap.getPyObject(1, null);
        if (callback == Py.None) {
            callback = null;
        }

        ReferenceBackend gref = GlobalRef.newInstance(ob);
        if (new_.for_type == subtype) {
            // NOTE: CPython disallows weakrefs to many builtin types (e.g. dict, list)
            // and would check weakrefability here. We aren't as strict since the JVM can
            // weakref anything. Our types' needs_weakref flag only refers to whether it
            // has a __weakref__ descriptor, not weakrefability
            if (callback == null) {
                ReferenceType ret = (ReferenceType)gref.find(ReferenceType.class);
                if (ret != null) {
                    return ret;
                }
            }
            return new ReferenceType(gref, callback);
        } else {
            return new ReferenceTypeDerived(subtype, gref, callback);
        }
    }

    @ExposedMethod
    final void weakref___init__(PyObject[] args, String[] keywords) {
        // Just ensure at least one arg, leaving other args alone
        ArgParser ap = parseInitArgs("__init__", args, keywords);
        ap.getPyObject(0);
        int arglen = ap.getList(2).__len__();
        if (arglen > 2) {
            throw Py.TypeError(String.format("__init__ expected at most 2 arguments, got %d",
                    arglen));
        }
    }

    /**
     * Return an ArgParser setup to ignore keyword args (allowing them
     * to passthru).
     *
     * @param funcName the name of the caller
     * @param args {@link or.python.core.PyObject} array of args
     * @param keywords {@code String}-array of keywords
     * @return an {@link or.python.core.ArgParser} instance
     */
    private static ArgParser parseInitArgs(String funcName, PyObject[] args, String[] keywords) {
        if (keywords.length > 0) {
            int argc = args.length - keywords.length;
            PyObject[] justArgs = new PyObject[argc];
            System.arraycopy(args, 0, justArgs, 0, argc);
            args = justArgs;
        }
        return new ArgParser(funcName, args, Py.NoKeywords, Py.NoKeywords);
    }

    public PyObject __call__(PyObject args[], String keywords[]) {
        return weakref___call__(args, keywords);
    }

    @ExposedMethod
    final PyObject weakref___call__(PyObject args[], String keywords[]) {
        new ArgParser("__call__", args, keywords, Py.NoKeywords, 0);
        return Py.java2py(get());
    }

    public String toString() {
        PyObject obj = get();
        if (obj == null) {
            return String.format("<weakref at %s; dead>", Py.idstr(this));
        }

        PyObject nameObj = obj.__findattr__("__name__");
        if (nameObj != null) {
            return String.format("<weakref at %s; to '%.50s' at %s (%s)>", Py.idstr(this),
                                 obj.getType().fastGetName(), Py.idstr(obj), nameObj);
        }
        return String.format("<weakref at %s; to '%.50s' at %s>", Py.idstr(this),
                             obj.getType().fastGetName(), Py.idstr(obj));
    }
}
