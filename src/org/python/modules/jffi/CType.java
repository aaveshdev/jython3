
package org.python.modules.jffi;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.python.core.Py;
import org.python.core.PyNewWrapper;
import org.python.core.PyObject;
import org.python.core.PyType;
import org.python.core.Untraversable;
import org.python.core.Traverseproc;
import org.python.core.Visitproc;
import org.python.expose.ExposeAsSuperclass;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;

@ExposedType(name = "jffi.Type", base = PyObject.class)
public abstract class CType extends PyObject {
    public static final PyType TYPE = PyType.fromClass(CType.class);
    static {
        TYPE.fastGetDict().__setitem__("Array", Array.TYPE);
        TYPE.fastGetDict().__setitem__("Pointer", Pointer.TYPE);
    }
    public static final CType VOID = primitive(NativeType.VOID);
    public static final CType BOOL = primitive(NativeType.BOOL);
    public static final CType BYTE = primitive(NativeType.BYTE);
    public static final CType UBYTE = primitive(NativeType.UBYTE);
    public static final CType SHORT = primitive(NativeType.SHORT);
    public static final CType USHORT = primitive(NativeType.USHORT);
    public static final CType INT = primitive(NativeType.INT);
    public static final CType UINT = primitive(NativeType.UINT);
    public static final CType LONGLONG = primitive(NativeType.LONGLONG);
    public static final CType ULONGLONG = primitive(NativeType.ULONGLONG);
    public static final CType LONG = primitive(NativeType.LONG);
    public static final CType ULONG = primitive(NativeType.ULONG);
    public static final CType FLOAT = primitive(NativeType.FLOAT);
    public static final CType DOUBLE = primitive(NativeType.DOUBLE);
    public static final CType POINTER = primitive(NativeType.POINTER);
    public static final CType STRING = primitive(NativeType.STRING);

    final NativeType nativeType;

    /** The <tt>MemoryOp</tt> used to read/write items of this type */
    private final MemoryOp memoryOp;

    CType(NativeType type, MemoryOp memoryOp) {
        this.nativeType = type;
        this.memoryOp = memoryOp;
    }

    public NativeType getNativeType() {
        return nativeType;
    }

    abstract com.kenai.jffi.Type jffiType();
    
    MemoryOp getMemoryOp() {
        return memoryOp;
    }

    public final int alignment() {
        return jffiType().alignment();
    }

    public final int size() {
        return jffiType().size();
    }    

    @ExposedMethod(names={"size"})
    public final PyObject pysize() {
        return Py.newInteger(size());
    }

    @ExposedMethod(names={"alignment"})
    public final PyObject pyalignment() {
        return Py.newInteger(alignment());
    }

    static final CType primitive(NativeType type) {
        CType t = new Builtin(type);
        CType.TYPE.fastGetDict().__setitem__(type.name(), t);
        return t;
    }

    @Untraversable
    static final class Builtin extends CType implements ExposeAsSuperclass {
        public Builtin(NativeType type) {
            super(type, MemoryOp.getMemoryOp(type));
        }

        @Override
        public final String toString() {
            return "<jffi.Type." + nativeType.name() + ">";
        }

        final com.kenai.jffi.Type jffiType() {
            return NativeType.jffiType(nativeType);
        }
    }

    @Untraversable
    @ExposedType(name = "jffi.Type.Custom", base = CType.class)
    static class Custom extends CType {
        final com.kenai.jffi.Type jffiType;
        
        public Custom(NativeType type, com.kenai.jffi.Type jffiType, MemoryOp op) {
            super(type, op);
            this.jffiType = jffiType;
        }

        final com.kenai.jffi.Type jffiType() {
            return jffiType;
        }
    }

    static CType typeOf(PyObject obj) {
        if (obj instanceof CType) {
            return (CType) obj;
        } else if (obj == Py.None) {
            return CType.VOID;
        }

        PyObject jffi_type = obj.__getattr__("_jffi_type");
        if (!(jffi_type instanceof CType)) {
            throw Py.TypeError("invalid _jffi_type");
        }
        return (CType) jffi_type;
    }

    @ExposedType(name = "jffi.Type.Array", base = CType.class)
    static final class Array extends CType.Custom implements Traverseproc {
        public static final PyType TYPE = PyType.fromClass(Array.class);
        final CType componentType;
        final PyType pyComponentType;
        final MemoryOp componentMemoryOp;
        
        @ExposedGet
        public final int length;

        public Array(PyType pyComponentType, CType componentType, int length) {
            super(NativeType.ARRAY, new com.kenai.jffi.Array(Util.jffiType(componentType), length), null);
            this.pyComponentType = pyComponentType;
            this.componentType = componentType;
            this.componentMemoryOp = getComponentMemoryOp((PyType) pyComponentType, componentType);
            this.length = length;
        }

        @ExposedNew
        public static PyObject Array_new(PyNewWrapper new_, boolean init, PyType subtype,
                PyObject[] args, String[] keywords) {

            if (args.length != 2) {
                throw Py.TypeError(String.format("__init__() takes exactly 2 arguments (%d given)", args.length));
            }

            if (!(args[0] instanceof PyType)) {
                throw Py.TypeError("invalid component type");
            }
            
            return new Array((PyType) args[0], typeOf(args[0]), args[1].asInt());
        }

        @Override
        public int __len__() {
            return length;
        }

        @Override
        public final String toString() {
            return String.format("<ctypes.Array elem_type=%s length=%d>", pyComponentType.toString(), length);
        }

        static final MemoryOp getComponentMemoryOp(PyType pyComponentType, CType componentType) {
            if (pyComponentType.isSubType(ScalarCData.TYPE)) {
                return componentType.getMemoryOp();
            } else if (pyComponentType.isSubType(Structure.TYPE)) {
                return new MemoryOp.StructOp(pyComponentType);
            } else {
                throw Py.TypeError("only scalar and struct types supported");
            }
        }


        /* Traverseproc implementation */
        @Override
        public int traverse(Visitproc visit, Object arg) {
            if (componentType != null) {
                int res = visit.visit(componentType, arg);
                if (res != 0) {
                    return res;
                }
            }
            return pyComponentType != null ? visit.visit(pyComponentType, arg) : 0;
        }

        @Override
        public boolean refersDirectlyTo(PyObject ob) {
            return ob != null && (ob == componentType || ob == pyComponentType);
        }
    }


    @ExposedType(name = "jffi.Type.Pointer", base = CType.class)
    final static class Pointer extends Custom implements Traverseproc {
        public static final PyType TYPE = PyType.fromClass(Pointer.class);
        private static final ConcurrentMap<PyObject, Pointer> typeCache
                = new ConcurrentHashMap<PyObject, Pointer>();

        final CType componentType;
        final PyType pyComponentType;
        final MemoryOp componentMemoryOp;

        Pointer(PyType subtype, PyType pyComponentType, CType componentType) {
            super(NativeType.POINTER, com.kenai.jffi.Type.POINTER, new MemoryOp.PointerOp(subtype, CType.POINTER));
            this.pyComponentType = pyComponentType;
            this.componentType = componentType;

            if (pyComponentType.isSubType(ScalarCData.TYPE)) {
                this.componentMemoryOp = new ScalarOp(componentType.getMemoryOp(), pyComponentType);
            } else if (pyComponentType.isSubType(Structure.TYPE)) {
                this.componentMemoryOp = new MemoryOp.StructOp(pyComponentType);
            } else {
                throw Py.TypeError("pointer only supported for scalar types");
            }

        }

        @ExposedNew
        public static PyObject Pointer_new(PyNewWrapper new_, boolean init, PyType subtype,
                PyObject[] args, String[] keywords) {

            if (args.length != 1) {
                throw Py.TypeError(String.format("__init__() takes exactly 1 argument (%d given)", args.length));
            }

            Pointer p = typeCache.get(args[0]);
            if (p != null) {
                return p;
            }

            if (!(args[0] instanceof PyType)) {
                throw Py.TypeError("expected ctypes class");
            }

            p = new Pointer(subtype, (PyType) args[0], typeOf(args[0]));
            typeCache.put(args[0], p);
            
            return p;
        }

        @Override
        public final String toString() {
            return String.format("<jffi.Type.Pointer component_type=%s>", componentType.toString());
        }

        private static final class ScalarOp extends MemoryOp {
            private final MemoryOp op;
            private final PyType type;

            public ScalarOp(MemoryOp op, PyType type) {
                this.op = op;
                this.type = type;
            }

            public final void put(Memory mem, long offset, PyObject value) {
                op.put(mem, offset, value);
            }

            public final PyObject get(Memory mem, long offset) {
                PyObject result = type.__call__(op.get(mem, offset));
                //
                // Point the CData to the backing memory so all value gets/sets
                // update the same memory this pointer points to
                //
                if (result instanceof ScalarCData) {
                        ((ScalarCData) result).setReferenceMemory(mem);
                }
                return result;
            }
        }


        /* Traverseproc implementation */
        @Override
        public int traverse(Visitproc visit, Object arg) {
            if (componentType != null) {
                int res = visit.visit(componentType, arg);
                if (res != 0) {
                    return res;
                }
            }
            return pyComponentType != null ? visit.visit(pyComponentType, arg) : 0;
        }

        @Override
        public boolean refersDirectlyTo(PyObject ob) {
            return ob != null && (ob == componentType || ob == pyComponentType);
        }
    }
}
