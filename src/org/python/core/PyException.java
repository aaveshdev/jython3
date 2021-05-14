// Copyright (c) Corporation for National Research Initiatives
package org.python.core;
import java.io.*;

/**
 * A wrapper for all python exception. Note that the well-known python exceptions are <b>not</b>
 * subclasses of PyException. Instead the python exception class is stored in the <code>type</code>
 * field and value or class instance is stored in the <code>value</code> field.
 */
public class PyException extends RuntimeException implements Traverseproc
{

    /**
     * The python exception class (for class exception) or identifier (for string exception).
     */
    public PyObject type;

    /**
     * The exception instance (for class exception) or exception value (for string exception).
     */
    public PyObject value = Py.None;

    /** The exception traceback object. */
    public PyTraceback traceback;

    /**
     * Whether the exception was re-raised, such as when a traceback is specified to
     * 'raise', or via a 'finally' block.
     */
    private boolean isReRaise = false;

    private boolean normalized = false;

    public PyException() {
        this(Py.None, Py.None);
    }

    public PyException(PyObject type) {
        this(type, Py.None);
    }

    public PyException(PyObject type, PyObject value) {
        this(type, value, null);
    }

    public PyException(PyObject type, PyObject value, PyTraceback traceback) {
        this.type = type;
        this.value = value;
        if (traceback != null) {
            this.traceback = traceback;
            isReRaise = true;
        } else {
            PyFrame frame = Py.getFrame();
            if (frame != null && frame.tracefunc != null) {
                frame.tracefunc = frame.tracefunc.traceException(frame, this);
            }
        }
    }

    public PyException(PyObject type, String value) {
        this(type, Py.newStringOrUnicode(value));
    }

    private boolean printingStackTrace = false;
    @Override
    public void printStackTrace() {
        Py.printException(this);
    }

    @Override
    public Throwable fillInStackTrace() {
        return Options.includeJavaStackInExceptions ? super.fillInStackTrace() : this;
    }

    @Override
    public String getMessage() {
        normalize();
        return Py.formatException(type, value);
    }

    @Override
    public synchronized void printStackTrace(PrintStream s) {
        if (printingStackTrace) {
            super.printStackTrace(s);
        } else {
            try {
                /*
                 * Ensure that non-ascii characters are made printable. IOne would prefer to emit
                 * Unicode, but the output stream too often only accepts bytes. (s is not
                 * necessarily a console, e.g. during a doctest.)
                 */
                PyFile err = new PyFile(s);
                err.setEncoding("ascii", "backslashreplace");
                printingStackTrace = true;
                Py.displayException(type, value, traceback, err);
            } finally {
                printingStackTrace = false;
            }
        }
    }

    public synchronized void super__printStackTrace(PrintWriter w) {
        try {
            printingStackTrace = true;
            super.printStackTrace(w);
        } finally {
            printingStackTrace = false;
        }
    }

    @Override
    public synchronized String toString() {
        return Py.exceptionToString(type, value, traceback);
    }

    /**
     * Instantiates the exception value if it is not already an
     * instance.
     *
     */
    public void normalize() {
        if (normalized) {
            return;
        }
        PyObject inClass = null;
        if (isExceptionInstance(value)) {
            inClass = value.fastGetClass();
        }

        if (isExceptionClass(type)) {
            if (inClass == null || !Py.isSubClass(inClass, type)) {
                PyObject[] args;

                // Don't decouple a tuple into args when it's a
                // KeyError, pass it on through below
                if (value == Py.None) {
                    args = Py.EmptyObjects;
                } else if (value instanceof PyTuple && type != Py.KeyError) {
                    args = ((PyTuple)value).getArray();
                } else {
                    args = new PyObject[] {value};
                }

                value = type.__call__(args);
            } else if (inClass != type) {
                type = inClass;
            }
        }
        normalized = true;
    }

    /**
     * Register frame as having been visited in the traceback.
     *
     * @param here the current PyFrame
     */
    public void tracebackHere(PyFrame here) {
        tracebackHere(here, false);
    }

    /**
     * Register frame as having been visited in the traceback.
     *
     * @param here the current PyFrame
     * @param isFinally whether caller is a Python finally block
     */
    public void tracebackHere(PyFrame here, boolean isFinally) {
        if (!isReRaise && here != null) {
            // the frame is either inapplicable or already registered (from a finally)
            // during a re-raise
            traceback = new PyTraceback(traceback, here);
        }
        // finally blocks immediately tracebackHere: so they toggle isReRaise to skip the
        // next tracebackHere hook
        isReRaise = isFinally;
    }

    /**
     * Logic for the raise statement
     *
     * @param type the first arg to raise, a type or an instance
     * @param value the second arg, the instance of the class or arguments to its
     * constructor
     * @param traceback a traceback object
     * @return a PyException wrapper
     */
    public static PyException doRaise(PyObject type, PyObject value, PyObject traceback) {
        if (type == null) {
            ThreadState state = Py.getThreadState();
            type = state.exception.type;
            value = state.exception.value;
            traceback = state.exception.traceback;
        }

        if (traceback == Py.None) {
            traceback = null;
        } else if (traceback != null && !(traceback instanceof PyTraceback)) {
            throw Py.TypeError("raise: arg 3 must be a traceback or None");
        }

        if (value == null) {
            value = Py.None;
        }

        // Repeatedly, replace a tuple exception with its first item
        while (type instanceof PyTuple && ((PyTuple)type).size() > 0) {
            type = type.__getitem__(0);
        }

        if (isExceptionClass(type)) {
            PyException pye = new PyException(type, value, (PyTraceback)traceback);
            pye.normalize();
            if (!isExceptionInstance(pye.value)) {
                throw Py.TypeError(String.format(
                    "calling %s() should have returned an instance of BaseException, not '%s'",
                    pye.type, pye.value));
            }
            return pye;
        } else if (isExceptionInstance(type)) {
            // Raising an instance.  The value should be a dummy.
            if (value != Py.None) {
                throw Py.TypeError("instance exception may not have a separate value");
            } else {
                // Normalize to raise <class>, <instance>
                value = type;
                type = type.fastGetClass();
            }
        } else {
            // Not something you can raise.  You get an exception
            // anyway, just not what you specified :-)
            throw Py.TypeError("exceptions must be old-style classes or derived from "
                               + "BaseException, not " + type.getType().fastGetName());
        }

        if (Options.py3k_warning && type instanceof PyClass) {
            Py.DeprecationWarning("exceptions must derive from BaseException in 3.x");
        }

        return new PyException(type, value, (PyTraceback)traceback);
    }

    /**
     * Determine if this PyException is a match for exc.
     *
     * @param exc a PyObject exception type
     * @return true if a match
     */
    public boolean match(PyObject exc) {
        if (exc instanceof PyTuple) {
            for (PyObject item : ((PyTuple)exc).getArray()) {
                if (match(item)) {
                    return true;
                }
            }
            return false;
        }

        if (exc instanceof PyString) {
            Py.DeprecationWarning("catching of string exceptions is deprecated");
        } else if (Options.py3k_warning && !isPy3kExceptionClass(exc)) {
            Py.DeprecationWarning("catching classes that don't inherit from BaseException is not "
                                  + "allowed in 3.x");
        }

        normalize();
        // FIXME, see bug 737978
        //
        // A special case for IOError's to allow them to also match
        // java.io.IOExceptions.  This is a hack for 1.0.x until I can do
        // it right in 1.1
        if (exc == Py.IOError) {
            if (__builtin__.isinstance(value, PyType.fromClass(IOException.class))) {
                return true;
            }
        }
        // FIXME too, same approach for OutOfMemoryError
        if (exc == Py.MemoryError) {
            if (__builtin__.isinstance(value,
                                       PyType.fromClass(OutOfMemoryError.class))) {
                return true;
            }
        }

        if (isExceptionClass(type) && isExceptionClass(exc)) {
            try {
                return Py.isSubClass(type, exc);
            } catch (PyException pye) {
                // This function must not fail, so print the error here
                Py.writeUnraisable(pye, type);
                return false;
            }
        }

        return type == exc;
    }

    /**
     * Determine whether obj is a Python exception class
     *
     * @param obj a PyObject
     * @return true if an exception
     */
    public static boolean isExceptionClass(PyObject obj) {
        if (obj instanceof PyClass) {
            return true;
        }
        return isPy3kExceptionClass(obj);
    }

    /**
     * Determine whether obj is a Python 3 exception class
     *
     * @param obj a PyObject
     * @return true if an exception
     */
    private static boolean isPy3kExceptionClass(PyObject obj) {
        if (!(obj instanceof PyType)) {
            return false;
        }
        PyType type = ((PyType)obj);
        if (type.isSubType(PyBaseException.TYPE)) {
            return true;
        }
        return type.getProxyType() != null
                && Throwable.class.isAssignableFrom(type.getProxyType());
    }

    /**
     * Determine whether obj is an Python exception instance
     *
     * @param obj a PyObject
     * @return true if an exception instance
     */
    public static boolean isExceptionInstance(PyObject obj) {
        return obj instanceof PyInstance || obj instanceof PyBaseException
           || obj.getJavaProxy() instanceof Throwable;
    }

    /**
     * Get the name of the exception's class
     *
     * @param obj a PyObject exception
     * @return String exception name
     */
    public static String exceptionClassName(PyObject obj) {
        return obj instanceof PyClass ? ((PyClass)obj).__name__ : ((PyType)obj).fastGetName();
    }


    /* Traverseproc support */

    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retValue;
        if (type != null) {
            retValue = visit.visit(type, arg);
            if (retValue != 0) {
                return retValue;
            }
        } if (value != null) {
            retValue = visit.visit(value, arg);
            if (retValue != 0) {
                return retValue;
            }
        } if (traceback != null) {
            retValue = visit.visit(traceback, arg);
            if (retValue != 0) {
                return retValue;
            }
        }
        return 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
    	return ob != null && (type == ob || value == ob || traceback == ob);
    }
}
