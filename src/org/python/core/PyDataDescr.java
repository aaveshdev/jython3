package org.python.core;

import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * Implements type checking and return type coercion for a data descriptor. A
 * subclass must at least implement invokeGet which is called in __get__
 * operations. If the descriptor supports setting and deleting, the subclass
 * must also override invokeSet and invokeDel respectively. When implementing
 * those methods, their respective implementsDescr* methods should be overriden
 * as well.
 */
@Untraversable
@ExposedType(name = "getset_descriptor", base = PyObject.class, isBaseType = false)
public abstract class PyDataDescr extends PyDescriptor {

    protected Class ofType;

    private String doc;

    /**
     * @param onType -
     *            the type the descriptor belongs to
     * @param name -
     *            the name of the descriptor on descriptor type
     * @param ofType -
     *            the type returned by the descriptor
     */
    public PyDataDescr(PyType onType, String name, Class ofType, String doc) {
        this(name, ofType, doc);
        setType(onType);
    }

    /**
     * This constructor does not initialize the type the descriptor belongs to. setType must be
     * called before this descriptor can be used.
     * 
     * @param name -
     *            the name of the descriptor on descriptor type
     * @param ofType -
     *            the type returned by the descriptor
     */
    public PyDataDescr(String name, Class ofType, String doc) {
        this.name = name;
        this.ofType = ofType;
        this.doc = doc;
    }
    
    /**
     * Sets the type the descriptor belongs to.
     */
    public void setType(PyType onType) {
        dtype = onType;
    }

    @Override
    public PyObject __get__(PyObject obj, PyObject type) {
        return getset_descriptor___get__(obj, type);
    }
    
    @ExposedMethod(defaults = "null")
    public PyObject getset_descriptor___get__(PyObject obj, PyObject type) {
        if(obj != null) {
            checkGetterType(obj.getType());
            return Py.java2py(invokeGet(obj));
        }
        return this;
    }

    public abstract Object invokeGet(PyObject obj);

    @Override
    public void __set__(PyObject obj, PyObject value) {
        getset_descriptor___set__(obj, value);
    }
    
    @ExposedMethod
    public void getset_descriptor___set__(PyObject obj, PyObject value) {
        checkGetterType(obj.getType());
        // XXX: We may want to special case value being PyUnicode and ofType being String
        // (then explicitly value.encode() first)
        Object converted = value.__tojava__(ofType);
        if(converted == Py.NoConversion) {
            throw Py.TypeError(String.format("unsupported type for assignment to %s: '%.200s'",
                                             name, value.getType().fastGetName()));
        }
        invokeSet(obj, converted);
    }

    public void invokeSet(PyObject obj, Object converted) {
        throw new UnsupportedOperationException("Must be overriden by a subclass");
    }

    @Override
    public void __delete__(PyObject obj) {
        getset_descriptor___delete__(obj);
    }
    
    @ExposedMethod
    public void getset_descriptor___delete__(PyObject obj) {
        if(obj != null) {
            checkGetterType(obj.getType());
            invokeDelete(obj);
        }
    }

    public void invokeDelete(PyObject obj) {
        throw new UnsupportedOperationException("Must be overriden by a subclass");
    }

    @Override
    public boolean isDataDescr() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("<attribute '%s' of '%s' objects>", name, dtype.fastGetName());
    }

    @ExposedGet(name = "__doc__")
    public String getDoc() {
        return doc;
    }

    /**
     * Return the name this descriptor is exposed as.
     *
     * @return a name String
     */
    @ExposedGet(name = "__name__")
    public String getName() {
        return name;
    }

    /**
     * Return the owner class of this descriptor.
     *
     * @return this descriptor's owner
     */
    @ExposedGet(name = "__objclass__")
    public PyObject getObjClass() {
        return dtype;
    }
}
