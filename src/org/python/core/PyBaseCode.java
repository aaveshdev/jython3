/*
 * Copyright (c) Corporation for National Research Initiatives
 * Copyright (c) Jython Developers
 */
package org.python.core;

import com.google.common.base.CharMatcher;

public abstract class PyBaseCode extends PyCode {

    public int co_argcount;
    int nargs;
    public int co_firstlineno = -1;
    public String co_varnames[];
    public String co_cellvars[];
    public int jy_npurecell; // internal: jython specific
    public String co_freevars[];
    public String co_filename;
    public CompilerFlags co_flags = new CompilerFlags();
    public int co_nlocals;
    public boolean varargs,  varkwargs;


    public boolean hasFreevars() {
        return co_freevars != null && co_freevars.length > 0;
    }

    public PyObject call(ThreadState ts, PyFrame frame, PyObject closure) {
//         System.err.println("tablecode call: "+co_name);
        if (ts.getSystemState() == null) {
            // Ensure that sys is set up
            ts.setSystemState(Py.defaultSystemState);
        }
        //System.err.println("got ts: "+ts+", "+ts.systemState);

        // Cache previously defined exception
        PyException previous_exception = ts.exception;

        // Push frame
        frame.f_back = ts.frame;
        if (frame.f_builtins == null) {
            if (frame.f_back != null) {
                frame.f_builtins = frame.f_back.f_builtins;
            } else {
                //System.err.println("ts: "+ts);
                //System.err.println("ss: "+ts.systemState);
                frame.f_builtins = ts.getSystemState().builtins;
            }
        }
        // nested scopes: setup env with closure
        // this should only be done once, so let the frame take care of it
        frame.setupEnv((PyTuple) closure);

        ts.frame = frame;

        // Handle trace function for debugging
        if (ts.tracefunc != null) {
            frame.f_lineno = co_firstlineno;
            frame.tracefunc = ts.tracefunc.traceCall(frame);
        }

        // Handle trace function for profiling
        if (ts.profilefunc != null) {
            ts.profilefunc.traceCall(frame);
        }

        PyObject ret;
        ThreadStateMapping.enterCall(ts);
        try {
            ret = interpret(frame, ts);
        } catch (Throwable t) {
            // Convert exceptions that occurred in Java code to PyExceptions
            PyException pye = Py.JavaError(t);
            pye.tracebackHere(frame);

            frame.f_lasti = -1;

            if (frame.tracefunc != null) {
                frame.tracefunc.traceException(frame, pye);
            }
            if (ts.profilefunc != null) {
                ts.profilefunc.traceException(frame, pye);
            }

            // Rethrow the exception to the next stack frame
            ts.exception = previous_exception;
            ts.frame = ts.frame.f_back;
            throw pye;
        } finally {
            ThreadStateMapping.exitCall(ts);
        }

        if (frame.tracefunc != null) {
            frame.tracefunc.traceReturn(frame, ret);
        }
        // Handle trace function for profiling
        if (ts.profilefunc != null) {
            ts.profilefunc.traceReturn(frame, ret);
        }

        // Restore previously defined exception
        ts.exception = previous_exception;

        ts.frame = ts.frame.f_back;
        return ret;
    }

    public PyObject call(ThreadState state, PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        if (co_argcount != 0 || varargs || varkwargs) {
            return call(state, Py.EmptyObjects, Py.NoKeywords, globals, defaults,
                        closure);
        }
        PyFrame frame = new PyFrame(this, globals);
        if (co_flags.isFlagSet(CodeFlag.CO_GENERATOR)) {
            return new PyGenerator(frame, closure);
        }
        return call(state, frame, closure);
    }

    public PyObject call(ThreadState state, PyObject arg1, PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        if (co_argcount != 1 || varargs || varkwargs) {
            return call(state, new PyObject[] {arg1},
                        Py.NoKeywords, globals, defaults, closure);
        }
        PyFrame frame = new PyFrame(this, globals);
        frame.f_fastlocals[0] = arg1;
        if (co_flags.isFlagSet(CodeFlag.CO_GENERATOR)) {
            return new PyGenerator(frame, closure);
        }
        return call(state, frame, closure);
    }

    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2, PyObject globals,
                         PyObject[] defaults, PyObject closure)
    {
        if (co_argcount != 2 || varargs || varkwargs) {
            return call(state, new PyObject[] {arg1, arg2},
                        Py.NoKeywords, globals, defaults, closure);
        }
        PyFrame frame = new PyFrame(this, globals);
        frame.f_fastlocals[0] = arg1;
        frame.f_fastlocals[1] = arg2;
        if (co_flags.isFlagSet(CodeFlag.CO_GENERATOR)) {
            return new PyGenerator(frame, closure);
        }
        return call(state, frame, closure);
    }

    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2, PyObject arg3,
                         PyObject globals, PyObject[] defaults,
                         PyObject closure)
    {
        if (co_argcount != 3 || varargs || varkwargs) {
            return call(state, new PyObject[] {arg1, arg2, arg3},
                        Py.NoKeywords, globals, defaults, closure);
        }
        PyFrame frame = new PyFrame(this, globals);
        frame.f_fastlocals[0] = arg1;
        frame.f_fastlocals[1] = arg2;
        frame.f_fastlocals[2] = arg3;
        if (co_flags.isFlagSet(CodeFlag.CO_GENERATOR)) {
            return new PyGenerator(frame, closure);
        }
        return call(state, frame, closure);
    }

    @Override
    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2,
            PyObject arg3, PyObject arg4, PyObject globals,
            PyObject[] defaults, PyObject closure) {
        if (co_argcount != 4 || varargs || varkwargs) {
            return call(state, new PyObject[]{arg1, arg2, arg3, arg4},
                        Py.NoKeywords, globals, defaults, closure);
        }
        PyFrame frame = new PyFrame(this, globals);
        frame.f_fastlocals[0] = arg1;
        frame.f_fastlocals[1] = arg2;
        frame.f_fastlocals[2] = arg3;
        frame.f_fastlocals[3] = arg4;
        if (co_flags.isFlagSet(CodeFlag.CO_GENERATOR)) {
            return new PyGenerator(frame, closure);
        }
        return call(state, frame, closure);
    }

    public PyObject call(ThreadState state, PyObject self, PyObject args[],
                         String keywords[], PyObject globals,
                         PyObject[] defaults, PyObject closure)
    {
        PyObject[] os = new PyObject[args.length+1];
        os[0] = self;
        System.arraycopy(args, 0, os, 1, args.length);
        return call(state, os, keywords, globals, defaults, closure);
    }

    public PyObject call(ThreadState state, PyObject args[], String kws[], PyObject globals,
                         PyObject[] defs, PyObject closure) {
        final PyFrame frame = new PyFrame(this, globals);
        final int argcount = args.length - kws.length;

        if ((co_argcount > 0) || varargs || varkwargs) {
            int i;
            int n = argcount;
            PyObject kwdict = null;
            final PyObject[] fastlocals = frame.f_fastlocals;
            if (varkwargs) {
                kwdict = new PyDictionary();
                i = co_argcount;
                if (varargs) {
                    i++;
                }
                fastlocals[i] = kwdict;
            }
            if (argcount > co_argcount) {
                if (!varargs) {
                    int defcount = defs != null ? defs.length : 0;
                    String msg = String.format("%.200s() takes %s %d %sargument%s (%d given)",
                                               co_name,
                                               defcount > 0 ? "at most" : "exactly",
                                               co_argcount,
                                               kws.length > 0 ? "" : "",
                                               co_argcount == 1 ? "" : "s",
                                               args.length);
                    throw Py.TypeError(msg);
                }
                n = co_argcount;
            }

            System.arraycopy(args, 0, fastlocals, 0, n);

            if (varargs) {
                PyObject[] u = new PyObject[argcount - n];
                System.arraycopy(args, n, u, 0, argcount - n);
                PyObject uTuple = new PyTuple(u);
                fastlocals[co_argcount] = uTuple;
            }
            for (i = 0; i < kws.length; i++) {
                String keyword = kws[i];
                PyObject value = args[i + argcount];
                int j;
                for (j = 0; j < co_argcount; j++) {
                    if (co_varnames[j].equals(keyword)) {
                        break;
                    }
                }
                if (j >= co_argcount) {
                    if (kwdict == null) {
                        throw Py.TypeError(String.format(
                                "%.200s() got an unexpected keyword argument '%.400s'",
                                co_name,
                                Py.newUnicode(keyword).encode("ascii", "replace")));
                    }
                    if (CharMatcher.ascii().matchesAllOf(keyword)) {
                        kwdict.__setitem__(keyword, value);
                    } else {
                        kwdict.__setitem__(Py.newUnicode(keyword), value);
                    }
                } else {
                    if (fastlocals[j] != null) {
                        throw Py.TypeError(String.format("%.200s() got multiple values for "
                                                         + "keyword argument '%.400s'",
                                                         co_name, keyword));
                    }
                    fastlocals[j] = value;
                }
            }
            if (argcount < co_argcount) {
                final int defcount = defs != null ? defs.length : 0;
                final int m = co_argcount - defcount;
                for (i = argcount; i < m; i++) {
                    if (fastlocals[i] == null) {
                        String msg =
                                String.format("%.200s() takes %s %d %sargument%s (%d given)",
                                              co_name,
                                              (varargs || defcount > 0) ? "at least" : "exactly",
                                              m,
                                              kws.length > 0 ? "" : "",
                                              m == 1 ? "" : "s",
                                              args.length);
                        throw Py.TypeError(msg);
                    }
                }
                if (n > m) {
                    i = n - m;
                } else {
                    i = 0;
                }
                for (; i < defcount; i++) {
                    if (fastlocals[m + i] == null) {
                        fastlocals[m + i] = defs[i];
                    }
                }
            }
        } else if ((argcount > 0) || (args.length > 0 && (co_argcount == 0 && !varargs && !varkwargs))) {
            throw Py.TypeError(String.format("%.200s() takes no arguments (%d given)",
                                             co_name, args.length));
        }

        if (co_flags.isFlagSet(CodeFlag.CO_GENERATOR)) {
            return new PyGenerator(frame, closure);
        }
        return call(state, frame, closure);
    }

    public String toString() {
        // Result must be convertible to a str (for __repr__()), but let's make it fully printable.
        String filename = PyString.encode_UnicodeEscape(co_filename, '"');
        return String.format("<code object %.100s at %s, file %.300s, line %d>",
                             co_name, Py.idstr(this), filename, co_firstlineno);
    }

    protected abstract PyObject interpret(PyFrame f, ThreadState ts);

    protected int getline(PyFrame f) {
         return f.f_lineno;
    }

    // returns the augmented version of CompilerFlags (instead of just as a bit vector int)
    public CompilerFlags getCompilerFlags() {
        return co_flags;
    }
}
