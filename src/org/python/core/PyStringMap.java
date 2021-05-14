/*
 * Copyright (c) Corporation for National Research Initiatives
 * Copyright (c) Jython Developers
 */
package org.python.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.AbstractSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.python.core.AbstractDict.ValuesIter;
import org.python.core.AbstractDict.KeysIter;
import org.python.core.AbstractDict.ItemsIter;
import org.python.core.PyMapSet.PySetIter;
import org.python.expose.ExposedClassMethod;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedNew;
import org.python.expose.ExposedType;
import org.python.expose.MethodType;
import org.python.util.Generic;

/**
 * Special fast dict implementation for __dict__ instances. Allows interned String keys in addition
 * to PyObject unlike PyDictionary.
 */
@ExposedType(name = "stringmap", base = PyObject.class, isBaseType = false)
public class PyStringMap extends AbstractDict implements Traverseproc {

    /**
     * TYPE computed lazily, PyStringMap is used early in the bootstrap process and
     * statically calling fromClass(PyStringMap.class) is unsafe.
     */
    private static PyType lazyType;

    private final ConcurrentMap<Object, PyObject> table;

    public ConcurrentMap<Object, PyObject> getMap() {
        return table;
    }

    public PyStringMap() {
        this(4);
    }

    public PyStringMap(int capacity) {
        super(getLazyType());
        table = new ConcurrentHashMap<Object, PyObject>(capacity, Generic.CHM_LOAD_FACTOR,
                                                        Generic.CHM_CONCURRENCY_LEVEL);
    }

    public PyStringMap(Map<Object, PyObject> map) {
        this(Math.max((int) (map.size() / Generic.CHM_LOAD_FACTOR) + 1,
                      Generic.CHM_INITIAL_CAPACITY));
        table.putAll(map);
    }

    public PyStringMap(PyObject elements[]) {
        this(elements.length);
        for (int i = 0; i < elements.length; i += 2) {
            __setitem__(elements[i], elements[i + 1]);
        }
    }

    private static PyType getLazyType() {
        if (lazyType == null) {
            lazyType = PyType.fromClass(PyStringMap.class);
        }
        return lazyType;
    }

    @ExposedNew
    final static PyObject stringmap_new(PyNewWrapper new_, boolean init, PyType subtype,
                                        PyObject[] args, String[] keywords) {
        PyStringMap map = new PyStringMap();
        map.stringmap_update(args, keywords);
        return map;
    }

    public static PyObject fromkeys(PyObject keys) {
        return fromkeys(keys, Py.None);
    }

    public static PyObject fromkeys(PyObject keys, PyObject value) {
        return stringmap_fromkeys(TYPE, keys, value);
    }

    @ExposedClassMethod(defaults = "Py.None", doc = BuiltinDocs.dict_fromkeys_doc)
    static PyObject stringmap_fromkeys(PyType type, PyObject keys, PyObject value) {
        PyObject d = type.__call__();
        for (PyObject o : keys.asIterable()) {
            d.__setitem__(o, value);
        }
        return d;
    }

    @Override
    public int __len__() {
        return stringmap___len__();
    }

    @ExposedMethod(doc = BuiltinDocs.dict___len___doc)
    final int stringmap___len__() {
        return table.size();
    }

    @Override
    public boolean __nonzero__() {
        return table.size() != 0;
    }

    @Override
    public PyObject __finditem__(String key) {
        if (key == null) {
            return null;
        }
        return table.get(key);
    }

    @Override
    public PyObject __finditem__(PyObject key) {
        if (key instanceof PyString) {
            return __finditem__(((PyString)key).internedString());
        }
        return table.get(key);
    }

    public PyObject __getitem__(String key) {
        PyObject o = __finditem__(key);
        if (null == o) {
            throw Py.KeyError(key);
        } else {
            return o;
        }
    }

    @Override
    public PyObject __getitem__(PyObject key) {
        return stringmap___getitem__(key);
    }

    @ExposedMethod(doc = BuiltinDocs.dict___getitem___doc)
    final PyObject stringmap___getitem__(PyObject key) {
        if (key instanceof PyString) {
            return __getitem__(((PyString)key).internedString());
        } else {
            PyObject o = __finditem__(key);
            if (null == o) {
                throw Py.KeyError(key);
            } else {
                return o;
            }
        }
    }

    @Override
    public PyObject __iter__() {
        return stringmap___iter__();
    }

    @ExposedMethod(doc = BuiltinDocs.dict___iter___doc)
    final PyObject stringmap___iter__() {
        return stringmap_iterkeys();
    }

    @Override
    public void __setitem__(String key, PyObject value) {
        if (value == null) {
            table.remove(key);
        } else {
            table.put(key, value);
        }
    }

    @Override
    public void __setitem__(PyObject key, PyObject value) {
        stringmap___setitem__(key, value);
    }

    @ExposedMethod(doc = BuiltinDocs.dict___setitem___doc)
    final void stringmap___setitem__(PyObject key, PyObject value) {
        if (value == null) {
            table.remove(pyToKey(key));
        } else if (key instanceof PyString) {
            __setitem__(((PyString)key).internedString(), value);
        } else {
            table.put(key, value);
        }
    }

    @Override
    public void __delitem__(String key) {
        Object ret = table.remove(key);
        if (ret == null) {
            throw Py.KeyError(key);
        }
    }

    @Override
    public void __delitem__(PyObject key) {
        stringmap___delitem__(key);
    }

    @ExposedMethod(doc = BuiltinDocs.dict___delitem___doc)
    final void stringmap___delitem__(PyObject key) {
        if (key instanceof PyString) {
            __delitem__(((PyString)key).internedString());
        } else {
            Object ret = table.remove(key);
            if (ret == null) {
                throw Py.KeyError(key);
            }
        }
    }

    /**
     * Remove all items from the dictionary.
     */
    public void clear() {
        stringmap_clear();
    }

    @ExposedMethod(doc = BuiltinDocs.dict_clear_doc)
    final void stringmap_clear() {
        table.clear();
    }

    @Override
    public String toString() {
        return stringmap_toString();
    }

    @ExposedMethod(names = {"__repr__", "__str__"}, doc = BuiltinDocs.dict___str___doc)
    final String stringmap_toString() {
        ThreadState ts = Py.getThreadState();
        if (!ts.enterRepr(this)) {
            return "{...}";
        }
        StringBuilder buf = new StringBuilder("{");
        for (Entry<Object, PyObject> entry : table.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof String) {
                /* This is a bit complicated, but prevents us to duplicate
                   PyString#__repr__ logic here. */
                buf.append(new PyString((String)key).__repr__().toString());
            } else {
                buf.append(((PyObject)key).__repr__().toString());
            }
            buf.append(": ");
            buf.append(entry.getValue().__repr__().toString());
            buf.append(", ");
        }
        if (buf.length() > 1) {
            buf.delete(buf.length() - 2, buf.length());
        }
        buf.append("}");
        ts.exitRepr(this);
        return buf.toString();
    }

    @Override
    public int __cmp__(PyObject other) {
        return stringmap___cmp__(other);
    }

    @ExposedMethod(type = MethodType.CMP, doc = BuiltinDocs.dict___cmp___doc)
    final int stringmap___cmp__(PyObject other) {
        if (!(other instanceof AbstractDict)) {
            return -2;
        }
        int an = __len__();
        int bn = other.__len__();
        if (an < bn) {
            return -1;
        }
        if (an > bn) {
            return 1;
        }
        PyList akeys = keys();
        PyList bkeys = ((AbstractDict) other).keys();
        akeys.sort();
        bkeys.sort();
        for (int i = 0; i < bn; i++) {
            PyObject akey = akeys.pyget(i);
            PyObject bkey = bkeys.pyget(i);
            int c = akey._cmp(bkey);
            if (c != 0) {
                return c;
            }
            PyObject avalue = __finditem__(akey);
            PyObject bvalue = other.__finditem__(bkey);
            c = avalue._cmp(bvalue);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    /**
     * Return true if the key exist in the dictionary.
     */
    public boolean has_key(String key) {
        return table.containsKey(key);
    }

    public boolean has_key(PyObject key) {
        return stringmap_has_key(key);
    }

    @ExposedMethod(doc = BuiltinDocs.dict_has_key_doc)
    final boolean stringmap_has_key(PyObject key) {
        return table.containsKey(pyToKey(key));
    }

    @Override
    public boolean __contains__(PyObject o) {
        return stringmap___contains__(o);
    }

    @ExposedMethod(doc = BuiltinDocs.dict___contains___doc)
    final boolean stringmap___contains__(PyObject o) {
        return stringmap_has_key(o);
    }

    /**
     * Return this[key] if the key exists in the mapping, defaultObj is returned otherwise.
     *
     * @param key
     *            the key to lookup in the mapping.
     * @param defaultObj
     *            the value to return if the key does not exists in the mapping.
     */
    public PyObject get(PyObject key, PyObject defaultObj) {
        return stringmap_get(key, defaultObj);
    }

    @ExposedMethod(defaults = "Py.None", doc = BuiltinDocs.dict_get_doc)
    final PyObject stringmap_get(PyObject key, PyObject defaultObj) {
        PyObject obj = __finditem__(key);
        return obj == null ? defaultObj : obj;
    }

    /**
     * Return this[key] if the key exists in the mapping, None is returned otherwise.
     *
     * @param key
     *            the key to lookup in the mapping.
     */
    public PyObject get(PyObject key) {
        return stringmap_get(key, Py.None);
    }

    /**
     * Return a shallow copy of the dictionary.
     */
    public PyStringMap copy() {
        return stringmap_copy();
    }

    @ExposedMethod(doc = BuiltinDocs.dict_copy_doc)
    final PyStringMap stringmap_copy() {
        return new PyStringMap(table);
    }

    public void update(PyObject other) {
        stringmap_update(new PyObject[] {other}, Py.NoKeywords);
    }

    /**
     * Insert all the key:value pairs from <code>dict</code> into this mapping.
     */
    @ExposedMethod(doc = BuiltinDocs.dict_update_doc)
    final void stringmap_update(PyObject[] args, String[] keywords) {
        int nargs = args.length - keywords.length;
        if (nargs > 1) {
            throw PyBuiltinCallable.DefaultInfo.unexpectedCall(nargs, false, "update", 0, 1);
        }
        if (nargs == 1) {
            PyObject arg = args[0];
            if (arg.__findattr__("keys") != null) {
                merge(arg);
            } else {
                mergeFromSeq(arg);
            }
        }
        for (int i = 0; i < keywords.length; i++) {
            __setitem__(keywords[i], args[nargs + i]);
        }
    }

    /**
     * Merge another PyObject that supports keys() with this
     * dict.
     *
     * @param other a PyObject with a keys() method
     */
    private void merge(PyObject other) {
        if (other instanceof PyStringMap) {
            table.putAll(((PyStringMap)other).table);
        } else if (other instanceof PyDictionary) {
            mergeFromKeys(other, ((PyDictionary)other).keys());
        } else {
            mergeFromKeys(other, other.invoke("keys"));
        }
    }

    /**
     * Merge another PyObject that supports keys() with this
     * dict.
     *
     * @param other a PyObject with a keys() method
     * @param override if true, the value from other is used on key-collision
     */
    public void merge(PyObject other, boolean override) {
        synchronized(table) {
            if (override) {
                merge(other);
            } else {
                if (other instanceof PyStringMap) {
                    Set<Map.Entry<Object, PyObject>> entrySet =
                            ((PyStringMap)other).table.entrySet();
                    for (Map.Entry<Object, PyObject> ent: entrySet) {
                        if (!table.containsKey(ent.getKey())) {
                            table.put(ent.getKey(), ent.getValue());
                        }
                    }
                } else if (other instanceof PyDictionary) {
                    mergeFromKeys(other, ((PyDictionary)other).keys(), override);
                } else {
                    mergeFromKeys(other, other.invoke("keys"), override);
                }
            }
        }
    }

    /**
     * Merge another PyObject via its keys() method
     *
     * @param other a PyObject with a keys() method
     * @param keys the result of other's keys() method
     */
    private void mergeFromKeys(PyObject other, PyObject keys) {
        for (PyObject key : keys.asIterable()) {
            __setitem__(key, other.__getitem__(key));
        }
    }

    /**
     * Merge another PyObject via its keys() method
     *
     * @param other a PyObject with a keys() method
     * @param keys the result of other's keys() method
     * @param override if true, the value from other is used on key-collision
     */
    public void mergeFromKeys(PyObject other, PyObject keys, boolean override) {
        synchronized(table) {
            if (override) {
                mergeFromKeys(other, keys);
            } else {
                for (PyObject key : keys.asIterable()) {
                    if (!__contains__(key)) {
                        __setitem__(key, other.__getitem__(key));
                    }
                }
            }
        }
    }

    /**
     * Merge any iterable object producing iterable objects of length
     * 2 into this dict.
     *
     * @param other another PyObject
     */
    private void mergeFromSeq(PyObject other) {
        PyObject pairs = other.__iter__();
        PyObject pair;

        for (int i = 0; (pair = pairs.__iternext__()) != null; i++) {
            try {
                pair = PySequence.fastSequence(pair, "");
            } catch(PyException pye) {
                if (pye.match(Py.TypeError)) {
                    throw Py.TypeError(String.format("cannot convert dictionary update sequence "
                                                     + "element #%d to a sequence", i));
                }
                throw pye;
            }
            int n;
            if ((n = pair.__len__()) != 2) {
                throw Py.ValueError(String.format("dictionary update sequence element #%d "
                                                  + "has length %d; 2 is required", i, n));
            }
            __setitem__(pair.__getitem__(0), pair.__getitem__(1));
        }
    }

    /**
     * Merge any iterable object producing iterable objects of length
     * 2 into this dict.
     *
     * @param other another PyObject
     * @param override if true, the value from other is used on key-collision
     */
    public void mergeFromSeq(PyObject other, boolean override) {
        synchronized(table) {
            if (override) {
                mergeFromSeq(other);
            } else {
                PyObject pairs = other.__iter__();
                PyObject pair;
        
                for (int i = 0; (pair = pairs.__iternext__()) != null; i++) {
                    try {
                        pair = PySequence.fastSequence(pair, "");
                    } catch(PyException pye) {
                        if (pye.match(Py.TypeError)) {
                            throw Py.TypeError(String.format("cannot convert dictionary update sequence "
                                                             + "element #%d to a sequence", i));
                        }
                        throw pye;
                    }
                    int n;
                    if ((n = pair.__len__()) != 2) {
                        throw Py.ValueError(String.format("dictionary update sequence element #%d "
                                                          + "has length %d; 2 is required", i, n));
                    }
                    if (!__contains__(pair.__getitem__(0))) {
                        __setitem__(pair.__getitem__(0), pair.__getitem__(1));
                    }
                }
            }
        }
    }

    /**
     * Return this[key] if the key exist, otherwise insert key with a None value and return None.
     *
     * @param key
     *            the key to lookup in the mapping.
     */
    public PyObject setdefault(PyObject key) {
        return setdefault(key, Py.None);
    }

    /**
     * Return this[key] if the key exist, otherwise insert key with the value of failobj and return
     * failobj
     *
     * @param key
     *            the key to lookup in the mapping.
     * @param failobj
     *            the default value to insert in the mapping if key does not already exist.
     */
    public PyObject setdefault(PyObject key, PyObject failobj) {
        return stringmap_setdefault(key, failobj);
    }

    @ExposedMethod(defaults = "Py.None", doc = BuiltinDocs.dict_setdefault_doc)
    final PyObject stringmap_setdefault(PyObject key, PyObject failobj) {
        Object internedKey = (key instanceof PyString) ? ((PyString)key).internedString() : key;
        PyObject oldValue = table.putIfAbsent(internedKey, failobj);
        return oldValue == null ? failobj : oldValue;
    }

    /**
     * Return a random (key, value) tuple pair and remove the pair from the mapping.
     */
    public PyObject popitem() {
        return stringmap_popitem();
    }

    @ExposedMethod(doc = BuiltinDocs.dict_popitem_doc)
    final PyObject stringmap_popitem() {
        Iterator<Entry<Object, PyObject>> it = table.entrySet().iterator();
        if (!it.hasNext()) {
            throw Py.KeyError("popitem(): dictionary is empty");
        }
        PyTuple tuple = itemTuple(it.next());
        it.remove();
        return tuple;
    }

    /* not correct - we need to determine size and remove at the same time! */
    public PyObject pop(PyObject key) {
        if (table.size() == 0) {
            throw Py.KeyError("pop(): dictionary is empty");
        }
        return stringmap_pop(key, null);
    }

    public PyObject pop(PyObject key, PyObject failobj) {
        return stringmap_pop(key, failobj);
    }

    @ExposedMethod(defaults = "null", doc = BuiltinDocs.dict_pop_doc)
    final PyObject stringmap_pop(PyObject key, PyObject failobj) {
        PyObject value = table.remove(pyToKey(key));
        if (value == null) {
            if (failobj == null) {
                throw Py.KeyError(key);
            } else {
                return failobj;
            }
        }
        return value;
    }

    /**
     * Return a copy of the mappings list of (key, value) tuple pairs.
     */
    public PyList items() {
        return stringmap_items();
    }

    @ExposedMethod(doc = BuiltinDocs.dict_items_doc)
    final PyList stringmap_items() {
        return new PyList(stringmap_iteritems());
    }

    private PyTuple itemTuple(Entry<Object, PyObject> entry) {
        return new PyTuple(keyToPy(entry.getKey()), entry.getValue());
    }

    /**
     * Return a copy of the mappings list of keys. We have to take in account that we could be
     * storing String or PyObject objects
     */
    public PyList keys() {
        return stringmap_keys();
    }

    @ExposedMethod(doc = BuiltinDocs.dict_keys_doc)
    final PyList stringmap_keys() {
        Object[] keys = table.keySet().toArray();
        PyObject[] keyArray = new PyObject[keys.length];
        int i = 0;
        for (Object key : keys) {
            keyArray[i++] = keyToPy(key);
        }
        return new PyList(keyArray);
    }

    /**
     * Return a copy of the mappings list of values.
     */
    public PyList values() {
        return stringmap_values();
    }

    @ExposedMethod(doc = BuiltinDocs.dict_values_doc)
    final PyList stringmap_values() {
        return new PyList(table.values());
    }

    /**
     * return an iterator over (key, value) pairs
     */
    public PyObject iteritems() {
        return stringmap_iteritems();
    }

    @ExposedMethod(doc = BuiltinDocs.dict_iteritems_doc)
    final PyObject stringmap_iteritems() {
        return new StringMapItemsIter(table.entrySet());
    }

    /**
     * return an iterator over the keys
     */
    public PyObject iterkeys() {
        return stringmap_iterkeys();
    }

    @ExposedMethod(doc = BuiltinDocs.dict_iterkeys_doc)
    final PyObject stringmap_iterkeys() {
        /* Python allows one to change the dict while iterating over it, including
           deletion. Java does not. Can we resolve with CHM? */
        return new StringMapKeysIter(table.keySet());
    }

    /**
     * return an iterator over the values
     */
    public PyObject itervalues() {
        return stringmap_itervalues();
    }

    @ExposedMethod(doc = BuiltinDocs.dict_itervalues_doc)
    final PyObject stringmap_itervalues() {
        return new StringMapValuesIter(table.values());
    }

    @Override
    public int hashCode() {
        return stringmap___hash__();
    }

    @ExposedMethod(doc = BuiltinDocs.dict___hash___doc)
    final int stringmap___hash__() {
        throw Py.TypeError(String.format("unhashable type: '%.200s'", getType().fastGetName()));
    }

    @Override
    public boolean isMappingType() {
        return true;
    }

    @Override
    public boolean isSequenceType() {
        return false;
    }

    private class StringMapValuesIter extends ValuesIter {

        public StringMapValuesIter(Collection<PyObject> c) {
            super(c);
        }

        @Override
        public PyObject __iternext__() {
            check(table.size());
            if (!iterator.hasNext()) {
                return null;
            }
            return iterator.next();
        }
    }

    private class StringMapKeysIter extends KeysIter<Object> {

        public StringMapKeysIter(Set<Object> s) {
            super(s);
        }

        @Override
        public PyObject __iternext__() {
            check(table.size());
            if (!iterator.hasNext()) {
                return null;
            }
            return keyToPy(iterator.next());
        }
    }

    private class StringMapItemsIter extends ItemsIter<Object> {

        public StringMapItemsIter(Set<Entry<Object, PyObject>> s) {
            super(s);
        }

        @Override
        public PyObject __iternext__() {
            check(table.size());
            if (!iterator.hasNext()) {
                return null;
            }
            return itemTuple(iterator.next());
        }
    }

    private static PyObject keyToPy(Object objKey){
        if (objKey instanceof String) {
            return PyString.fromInterned((String)objKey);
        } else {
            return (PyObject)objKey;
        }
    }

    private static Object pyToKey(PyObject pyKey) {
        if (pyKey instanceof PyString) {
            return ((PyString)pyKey).internedString();
        } else {
            return pyKey;
        }
    }

    private static class PyStringMapKeySetWrapper extends AbstractSet<PyObject>
    {
        Set<Object> backend;

        PyStringMapKeySetWrapper(Set<Object> backend) {
            this.backend = backend;
        }

        class PyStringMapKeySetIter implements Iterator<PyObject> {
            Iterator<Object> itr;

            PyStringMapKeySetIter(Iterator<Object> itr) {
                this.itr = itr;
            }

            public boolean hasNext() {
                return itr.hasNext();
            }

            public PyObject next() {
                return keyToPy(itr.next());
            }

            public void remove() {
                itr.remove();
            }
        }

        @Override
        public Iterator<PyObject> iterator() {
            return new PyStringMapKeySetIter(backend.iterator());
        }

        @Override
        public int size() {
            return backend.size();
        }
    }

    public Set<PyObject> pyKeySet() {
        return new PyStringMapKeySetWrapper(table.keySet());
    }

    /** @see java.util.Map#entrySet() */
    public Set entrySet() {
        return new PyMapEntrySet(getMap().entrySet());
    }

    /**
     * Returns a dict_keys on the dictionary's keys
     */
    @ExposedMethod(doc = BuiltinDocs.dict_viewkeys_doc)
    public PyObject viewkeys() {
        return super.viewkeys();
    }

    /**
     * Returns a dict_items on the dictionary's items
     */
    @ExposedMethod(doc = BuiltinDocs.dict_viewitems_doc)
    public PyObject viewitems() {
        return super.viewitems();
    }

    /**
     * Returns a dict_values on the dictionary's values
     */
    @ExposedMethod(doc = BuiltinDocs.dict_viewvalues_doc)
    public PyObject viewvalues() {
        return super.viewvalues();
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {
        int retVal;
        Object key;
        PyObject value;
        for (Map.Entry<Object, PyObject> ent: table.entrySet()) {
            key = ent.getKey();
            value = ent.getValue();
            if (key instanceof PyObject) {
                retVal = visit.visit((PyObject) key, arg);
                if (retVal != 0) return retVal;
            }
            if (value != null) {
                retVal = visit.visit(value, arg);
                if (retVal != 0) return retVal;
            }
        }
        return 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        return ob != null && (table.containsKey(ob) || table.containsValue(ob));
    }
}
