package org.python.modules;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyArray;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.Untraversable;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@Untraversable
@ExposedType(name = "struct.Struct", base = PyObject.class)
public class PyStruct extends PyObject {
    public static final PyType TYPE = PyType.fromClass(PyStruct.class);
    
    @ExposedGet
    public final String format;
    
    @ExposedGet
    public final int size;
    
    private final struct.FormatDef[] format_def;

    @ExposedGet(name = "__class__")
    @Override
    public PyType getType() {
        return TYPE;
    }

    public PyStruct(PyString format) {
        this(TYPE, format);
    }

    public PyStruct(PyType type, PyString format) {
        super(type);
        this.format = format.toString();
        this.format_def = struct.whichtable(this.format);
        this.size = struct.calcsize(this.format, this.format_def);
    }

    @ExposedNew
    final static PyObject Struct___new__ (PyNewWrapper new_, boolean init,
            PyType subtype, PyObject[] args, String[] keywords) {
        ArgParser ap = new ArgParser("Struct", args, keywords, new String[] {"format"}, 1);

        PyObject format = ap.getPyObject(0);
        if (!(format instanceof PyString)) {
            throw Py.TypeError("coercing to Unicode: need string, '"
                    + format.getType().fastGetName() + "' type found");
        }

        return new PyStruct(TYPE, (PyString) format);
    }

    @ExposedMethod
    public String pack(PyObject[] args, String[] kwds) {
        return struct.pack(format, format_def, size, 0, args).toString();
    }
    
    @ExposedMethod
    final void pack_into(PyObject[] args, String[] kwds) {
        struct.pack_into(format, format_def, size, 0, args);
    }
  
    @ExposedMethod
    public PyTuple unpack(PyObject source) {
        String s;
        if (source instanceof PyString)
            s = source.toString();
        else if (source instanceof PyArray) 
            s = ((PyArray)source).tostring();
        else
            throw Py.TypeError("unpack of a str or array");
        if (size != s.length()) 
            throw struct.StructError("unpack str size does not match format");
        return struct.unpack(format_def, size, format, new struct.ByteStream(s));
    }
    
    // xxx - also support byte[], java.nio.(Byte)Buffer at some point?
    @ExposedMethod(defaults = {"0"})
    public PyTuple unpack_from(PyObject string, int offset) {
        String s = string.toString();
        if (size >= (s.length() - offset + 1))
            throw struct.StructError("unpack_from str size does not match format");
        return struct.unpack(format_def, size, format, new struct.ByteStream(s, offset));
    }
}
