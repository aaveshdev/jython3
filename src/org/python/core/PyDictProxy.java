/* Copyright (c) 2008 Jython Developers */
package org.python.core;

import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;

/**
 * Readonly proxy for dictionaries (actually any mapping).
 *
 */
@ExposedType(name = "dictproxy", isBaseType = false)
public class PyDictProxy extends PyObject implements Traverseproc {

    /** The dict proxied to. */
    PyObject dict;

    public PyDictProxy(PyObject dict) {
        super();
        this.dict = dict;
    }

    @Override
    public PyObject __iter__() {
        return dict.__iter__();
    }

    @Override
    public PyObject __finditem__(PyObject key) {
        return dict.__finditem__(key);
    }

    @Override
    public int __len__() {
        return dict.__len__();
    }

    @ExposedMethod
    public PyObject dictproxy___getitem__(PyObject key) {
        return dict.__getitem__(key);
    }

    @ExposedMethod
    public boolean dictproxy___contains__(PyObject value) {
        return dict.__contains__(value);
    }

    @ExposedMethod
    public boolean dictproxy_has_key(PyObject key) {
        return dict.__contains__(key);
    }

    @ExposedMethod(defaults = "Py.None")
    public PyObject dictproxy_get(PyObject key, PyObject default_object) {
        return dict.invoke("get", key, default_object);
    }

    @ExposedMethod
    public PyObject dictproxy_keys() {
        return dict.invoke("keys");
    }

    @ExposedMethod
    public PyObject dictproxy_values() {
        return dict.invoke("values");
    }

    @ExposedMethod
    public PyObject dictproxy_items() {
        return dict.invoke("items");
    }

    @ExposedMethod
    public PyObject dictproxy_iterkeys() {
        return dict.invoke("iterkeys");
    }

    @ExposedMethod
    public PyObject dictproxy_itervalues() {
        return dict.invoke("itervalues");
    }

    @ExposedMethod
    public PyObject dictproxy_iteritems() {
        return dict.invoke("iteritems");
    }

    @ExposedMethod
    public PyObject dictproxy_copy() {
        return dict.invoke("copy");
    }

    @Override
    public int __cmp__(PyObject other) {
        return dictproxy___cmp__(other);
    }

    @ExposedMethod(type = MethodType.CMP)
    public int dictproxy___cmp__(PyObject other) {
        return dict._cmp(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject dictproxy___lt__(PyObject other) {
        return dict.__lt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject dictproxy___le__(PyObject other) {
        return dict.__le__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject dictproxy___eq__(PyObject other) {
        return dict.__eq__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject dictproxy___ne__(PyObject other) {
        return dict.__ne__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject dictproxy___gt__(PyObject other) {
        return dict.__gt__(other);
    }

    @ExposedMethod(type = MethodType.BINARY)
    public PyObject dictproxy___ge__(PyObject other) {
        return dict.__ge__(other);
    }

    @Override
    @ExposedMethod
    public PyString __str__() {
        return dict.__str__();
    }

    @Override
    public boolean isMappingType() {
        return true;
    }

    @Override
    public boolean isSequenceType() {
        return false;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        return dict == null ? 0 : visit.visit(dict, arg);
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && ob == dict;
    }
}
