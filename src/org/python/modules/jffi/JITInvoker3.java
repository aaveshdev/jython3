package org.python.modules.jffi;

import org.python.core.PyObject;

abstract public class JITInvoker3 extends JITInvoker {
    public JITInvoker3(com.kenai.jffi.Function function, Invoker fallbackInvoker) {
        super(3, function, fallbackInvoker);
    }

    public final PyObject invoke() {
        return invalidArity(0);
    }

    public final PyObject invoke(PyObject arg1) {
        return invalidArity(1);
    }

    public final PyObject invoke(PyObject arg1, PyObject arg2) {
        return invalidArity(2);
    }

    public final PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4) {
        return invalidArity(4);
    }

    public final PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4, PyObject arg5) {
        return invalidArity(5);
    }

    public final PyObject invoke(PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4, PyObject arg5, PyObject arg6) {
        return invalidArity(6);
    }
}
