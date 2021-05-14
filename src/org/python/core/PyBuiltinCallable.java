/* Copyright (c) Jython Developers */
package org.python.core;

import java.io.Serializable;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

@Untraversable
@ExposedType(name = "builtin_function_or_method", isBaseType = false)
public abstract class PyBuiltinCallable extends PyObject {

    protected Info info;

    protected String doc;

    protected PyBuiltinCallable(PyType type, Info info) {
        super(type);
        this.info = info;
    }

    protected PyBuiltinCallable(Info info) {
        this.info = info;
    }

    /**
     * Returns a new instance of this type of PyBuiltinFunction bound to self
     */
    abstract public PyBuiltinCallable bind(PyObject self);

    @ExposedGet(name = "__name__")
    public PyObject fastGetName() {
        return Py.newString(this.info.getName());
    }

    @ExposedGet(name = "__doc__")
    public String getDoc() {
        return doc;
    }

    @ExposedGet(name = "__module__")
    public PyObject getModule() {
        return Py.None;
    }

    @ExposedGet(name = "__call__")
    public PyObject makeCall() {
        return this;
    }

    @ExposedGet(name = "__self__")
    public PyObject getSelf() {
        return Py.None;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    @Override
    public String toString() {
        PyObject self = getSelf();
        if (self == null) {
            return String.format("<built-in function %s>", info.getName());
        } else {
            return String.format("<built-in method %s of %s object at %s>", info.getName(),
                                 self.getType().fastGetName(), Py.idstr(self));
        }
    }

    public interface Info extends Serializable {

        String getName();

        int getMaxargs();

        int getMinargs();

        PyException unexpectedCall(int nargs, boolean keywords);
    }

    public static class DefaultInfo implements Info {

        private String name;

        private int maxargs, minargs;

        public DefaultInfo(String name, int minargs, int maxargs) {
            this.name = name;
            this.minargs = minargs;
            this.maxargs = maxargs;
        }

        public DefaultInfo(String name) {
            this(name, -1, -1);
        }

        public String getName() {
            return name;
        }

        public int getMaxargs() {
            return maxargs;
        }

        public int getMinargs() {
            return minargs;
        }

        public static boolean check(int nargs, int minargs, int maxargs) {
            if (nargs < minargs) {
                return false;
            }
            if (maxargs != -1 && nargs > maxargs) {
                return false;
            }
            return true;
        }

        public static PyException unexpectedCall(int nargs, boolean keywords, String name,
                                                 int minargs, int maxargs) {
            if (keywords) {
                return Py.TypeError(name + "() takes no keyword arguments");
            }

            String argsblurb;
            if (minargs == maxargs) {
                if (minargs == 0) {
                    argsblurb = "no arguments";
                } else if (minargs == 1) {
                    argsblurb = "exactly one argument";
                } else {
                    argsblurb = minargs + " arguments";
                }
            } else if (maxargs == -1) {
                return Py.TypeError(String.format("%s() requires at least %d arguments (%d) given",
                                                  name, minargs, nargs));
            } else if (minargs <= 0) {
                argsblurb = "at most " + maxargs + " arguments";
            } else {
                argsblurb = minargs + "-" + maxargs + " arguments";
            }
            return Py.TypeError(String.format("%s() takes %s (%d given)", name, argsblurb, nargs));
        }

        public PyException unexpectedCall(int nargs, boolean keywords) {
            return unexpectedCall(nargs, keywords, name, minargs, maxargs);
        }
    }
}
