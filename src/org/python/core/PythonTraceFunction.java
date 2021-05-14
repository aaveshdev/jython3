// Copyright (c) Corporation for National Research Initiatives
package org.python.core;


class PythonTraceFunction extends TraceFunction implements Traverseproc {

    PyObject tracefunc;

    PythonTraceFunction(PyObject tracefunc) {
        this.tracefunc = tracefunc;
    }

    private TraceFunction safeCall(PyFrame frame, String label, PyObject arg) {
        synchronized(imp.class) {
            synchronized(this) {
                ThreadState ts = Py.getThreadState();
                if(ts.tracing)
                    return null;
                if(tracefunc == null)
                    return null;
                PyObject ret = null;
                try {
                    ts.tracing = true;
                    ret = tracefunc.__call__(frame, new PyString(label), arg);
                } catch(PyException exc) {
                    frame.tracefunc = null;
                    ts.tracefunc = null;
                    ts.profilefunc = null;
                    throw exc;
                } finally {
                    ts.tracing = false;
                }
                if(ret == tracefunc)
                    return this;
                if(ret == Py.None)
                    return null;
                return new PythonTraceFunction(ret);
            }
        }
    }

    public TraceFunction traceCall(PyFrame frame) {
        return safeCall(frame, "call", Py.None);
    }

    public TraceFunction traceReturn(PyFrame frame, PyObject ret) {
        return safeCall(frame, "return", ret);
    }

    public TraceFunction traceLine(PyFrame frame, int line) {
        return safeCall(frame, "line", Py.None);
    }

    public TraceFunction traceException(PyFrame frame, PyException exc) {
        // We must avoid passing a null to a PyTuple
        PyObject safeTraceback = exc.traceback == null ? Py.None : exc.traceback;
        return safeCall(frame, "exception",
                new PyTuple(exc.type, exc.value, safeTraceback));
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return tracefunc == null ? 0 : visit.visit(tracefunc, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && ob == tracefunc;
    }
}
