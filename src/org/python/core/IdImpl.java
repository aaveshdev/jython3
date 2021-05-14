package org.python.core;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;

import org.python.util.Generic;

public class IdImpl {

    public static class WeakIdentityMap {

        private transient ReferenceQueue<Object> idKeys = new ReferenceQueue<Object>();

        private Map<WeakIdKey, Object> objHashcodeToPyId = Generic.map();

        @SuppressWarnings("element-type-mismatch")
        private void cleanup() {
            Object k;
            while ((k = idKeys.poll()) != null) {
                objHashcodeToPyId.remove(k);
            }
        }

        private class WeakIdKey extends WeakReference<Object> {
            private final int hashcode;

            WeakIdKey(Object obj) {
                super(obj, idKeys);
                hashcode = System.identityHashCode(obj);
            }

            @Override
            public int hashCode() {
                return hashcode;
            }

            @Override
            public boolean equals(Object other) {
                Object obj = get();
                if (obj != null) {
                    return obj == ((WeakIdKey)other).get();
                } else {
                    return this == other;
                }
            }
        }

        // Used by test_jy_internals
        public int _internal_map_size() {
            return objHashcodeToPyId.size();
        }

        public void put(Object key, Object val) {
            cleanup();
            objHashcodeToPyId.put(new WeakIdKey(key), val);
        }

        public Object get(Object key) {
            cleanup();
            return objHashcodeToPyId.get(new WeakIdKey(key));
        }

        public void remove(Object key) {
            cleanup();
            objHashcodeToPyId.remove(new WeakIdKey(key));
        }

    }

    private WeakIdentityMap idMap = new WeakIdentityMap();

    private long sequentialId;

    public synchronized long id(PyObject o) {
        Object id = JyAttribute.getAttr(o, JyAttribute.PY_ID_ATTR);
        if (id != null) {
            return ((Long) id).longValue();
        }
        Object javaProxy = o.getJavaProxy();
        long result;
        if (javaProxy != null) {
            result = java_obj_id(javaProxy);
        } else {
            result = java_obj_id(o);
        }
        JyAttribute.setAttr(o, JyAttribute.PY_ID_ATTR, result);
        return result;
    }

    public String idstr(PyObject o) {
        return String.format("0x%x", id(o));
    }

    public synchronized long java_obj_id(Object o) {
        Long cand = (Long)idMap.get(o);
        if (cand == null) {
            long new_id = ++sequentialId;
            idMap.put(o, new_id);
            return new_id;
        }
        return cand.longValue();
    }
}
