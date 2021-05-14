/* Copyright (c) 2005-2008 Jython Developers */
package org.python.modules.time;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyList;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.Visitproc;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * struct_time of the time module.
 *
 */
@ExposedType(name = "time.struct_time", isBaseType = false)
public class PyTimeTuple extends PyTuple {

    @ExposedGet
    public PyObject tm_year, tm_mon, tm_mday, tm_hour, tm_min, tm_sec, tm_wday, tm_yday, tm_isdst;

    @ExposedGet
    public final int n_sequence_fields = 9, n_fields = 9, n_unnamed_fields = 0;
    
    public static final PyType TYPE = PyType.fromClass(PyTimeTuple.class);

    PyTimeTuple(PyObject... vals) {
        super(TYPE, vals);
        tm_year = vals[0];
        tm_mon =  vals[1];
        tm_mday = vals[2];
        tm_hour = vals[3];
        tm_min =  vals[4];
        tm_sec =  vals[5];
        tm_wday = vals[6];
        tm_yday = vals[7];
        tm_isdst =vals[8];
    }

    @ExposedNew
    static PyObject struct_time_new(PyNewWrapper wrapper, boolean init, PyType subtype,
                                    PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("struct_time", args, keywords, new String[] {"tuple"}, 1);
        PyObject obj = ap.getPyObject(0);
        if (obj instanceof PyTuple) {
            if (obj.__len__() != 9) {
                throw Py.TypeError("time.struct_time() takes a 9-sequence (1-sequence given)");
            }
            // tuples are immutable, so we can just use its underlying array
            return new PyTimeTuple(((PyTuple)obj).getArray());
        }
        else {
            PyList seq = new PyList(obj);
            if (seq.__len__() != 9) {
                throw Py.TypeError("time.struct_time() takes a 9-sequence (1-sequence given)");
            }        
            return new PyTimeTuple((PyObject[])seq.__tojava__(PyObject[].class));            
        }
    }

    public synchronized PyObject __eq__(PyObject o) {
        return struct_time___eq__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final synchronized PyObject struct_time___eq__(PyObject o) {
        if (getType() != o.getType() && !getType().isSubType(o.getType())) {
            return null;
        }
        int tl = __len__();
        int ol = o.__len__();
        if (tl != ol) {
            return Py.False;
        }
        int i = cmp(this, tl, o, ol);
        return (i < 0) ? Py.True : Py.False;
    }

    public synchronized PyObject __ne__(PyObject o) {
        return struct_time___ne__(o);
    }

    @ExposedMethod(type = MethodType.BINARY)
    final synchronized PyObject struct_time___ne__(PyObject o) {
        PyObject eq = struct_time___eq__(o);
        if (eq == null) {
            return null;
        }
        return eq.__not__();
    }

    /**
     * Used for pickling.
     *
     * @return a tuple of (class, tuple)
     */
    public PyObject __reduce__() {
        return struct_time___reduce__();
    }

    @ExposedMethod
    final PyObject struct_time___reduce__() {
        PyTuple newargs = __getnewargs__();
        return new PyTuple(getType(), newargs);
    }

    public PyTuple __getnewargs__() {
        return new PyTuple(new PyList(getArray()));
    }

    @Override
    public String toString() {
        return struct_time_toString();
    }

    @ExposedMethod(names = {"__str__", "__repr__"})
    final String struct_time_toString() {
        return String.format("time.struct_time(tm_year=%s, tm_mon=%s, tm_mday=%s, tm_hour=%s, tm_min=%s, tm_sec=%s, tm_wday=%s, tm_yday=%s, tm_isdst=%s)",
                             tm_year, tm_mon, tm_mday, tm_hour, tm_min, tm_sec, tm_wday, tm_yday, tm_isdst);
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal = super.traverse(visit, arg);
        if (retVal != 0) {
            return retVal;
        }
        if (tm_year != null) {
            retVal = visit.visit(tm_year, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (tm_mon != null) {
            retVal = visit.visit(tm_mon, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (tm_mday != null) {
            retVal = visit.visit(tm_mday, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (tm_hour != null) {
            retVal = visit.visit(tm_hour, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (tm_min != null) {
            retVal = visit.visit(tm_min, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (tm_sec != null) {
            retVal = visit.visit(tm_sec, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (tm_wday != null) {
            retVal = visit.visit(tm_wday, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (tm_yday != null) {
            retVal = visit.visit(tm_yday, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return tm_isdst != null ? visit.visit(tm_isdst, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (ob == tm_year || ob == tm_mon || ob == tm_mday
            || ob == tm_hour || ob == tm_min || ob == tm_sec || ob == tm_wday
            || ob == tm_yday || ob == tm_isdst || super.refersDirectlyTo(ob));
    }
}
