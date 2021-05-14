// Copyright 2000 Finn Bock
package org.python.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * An abstract helper class useful when implementing an iterator object. This implementation supply
 * a correct __iter__() and a next() method based on the __iternext__() implementation. The
 * __iternext__() method must be supplied by the subclass.
 *
 * If the implementation raises a StopIteration exception, it should be stored in stopException so
 * the correct exception can be thrown to preserve the line numbers in the traceback.
 */
public abstract class PyIterator extends PyObject implements Iterable<Object>, Traverseproc {

    protected PyException stopException;

    public PyIterator() {}

    public PyIterator(PyType subType) {
        super(subType);
    }

    public abstract PyObject __iternext__();

    public PyObject __iter__() {
        return this;
    }

    public static PyString __doc__next =
        new PyString("x.next() -> the next value, or raise StopIteration");

    /**
     * The exposed next method.
     *
     * Note that exposed derivable subclasses of PyIterator should override next to call
     * doNext(custom___iternext__), as __iternext__ is overridden by the Derived classes.
     *
     * @return a PyObject result
     */
    public PyObject next() {
        return doNext(__iternext__());
    }

    protected final PyObject doNext(PyObject ret) {
        if (ret == null) {
            if (stopException != null) {
                PyException toThrow = stopException;
                stopException = null;
                throw toThrow;
            }
            throw Py.StopIteration("");
        }
        return ret;
    }

    public Iterator<Object> iterator() {
        return new WrappedIterIterator<Object>(this) {
            @Override
            public Object next() {
                return getNext().__tojava__(Object.class);
            }
        };
    }

    @Override
    public Object __tojava__(Class<?> c) {
        if (c.isAssignableFrom(Iterable.class)) {
            return this;
        }
        if (c.isAssignableFrom(Iterator.class)) {
            return iterator();
        }
        if (c.isAssignableFrom(Collection.class)) {
            List<Object> list = new ArrayList<>();
            for (Object obj : this) {
                list.add(obj);
            }
            return list;
        }
        if (c.isArray()) {
            PyArray array = new PyArray(c.getComponentType(), this);
            return array.__tojava__(c);
        }
        return super.__tojava__(c);
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return stopException != null ? stopException.traverse(visit, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && stopException != null && stopException.refersDirectlyTo(ob);
    }
}
