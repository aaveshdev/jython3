package org.python.core;

import java.io.Serializable;

/**
 * Handles all the index checking and manipulation for get, set and del operations on a sequence.
 */
public abstract class SequenceIndexDelegate implements Serializable {

    public abstract int len();

    public abstract PyObject getItem(int idx);

    public abstract void setItem(int idx, PyObject value);

    public abstract void delItem(int idx);

    public abstract PyObject getSlice(int start, int stop, int step);

    public abstract void setSlice(int start, int stop, int step, PyObject value);

    public abstract void delItems(int start, int stop);

    public abstract String getTypeName();

    public void checkIdxAndSetItem(PyObject idx, PyObject value) {
        if (idx.isIndex()) {
            checkIdxAndSetItem(idx.asIndex(Py.IndexError), value);
        } else if (idx instanceof PySlice) {
            checkIdxAndSetSlice((PySlice)idx, value);
        } else {
            throw Py.TypeError(getTypeName() + " indices must be integers");
        }
    }

    public void checkIdxAndSetSlice(PySlice slice, PyObject value) {
        int[] indices = slice.indicesEx(len());
        if ((slice.step != Py.None) && value.__len__() != indices[3]) {
            throw Py.ValueError(String.format("attempt to assign sequence of size %d to extended "
                                              + "slice of size %d", value.__len__(), indices[3]));
        }
        setSlice(indices[0], indices[1], indices[2], value);
    }

    public void checkIdxAndSetItem(int idx, PyObject value) {
        setItem(checkIdx(idx), value);
    }

    public void checkIdxAndDelItem(PyObject idx) {
        if (idx.isIndex()) {
            delItem(checkIdx(idx.asIndex(Py.IndexError)));
        } else if (idx instanceof PySlice) {
            PySlice slice = (PySlice) idx;
            delSlice(slice.indicesEx(len()));
        } else {
            throw Py.TypeError(getTypeName() + " indices must be integers");
        }
    }

    public PyObject checkIdxAndGetItem(PyObject idx) {
        PyObject res = checkIdxAndFindItem(idx);
        if (res == null) {
            throw Py.IndexError("index out of range: " + idx);
        }
        return res;
    }

    public PyObject checkIdxAndFindItem(PyObject idx) {
        if (idx.isIndex()) {
            return checkIdxAndFindItem(idx.asIndex(Py.IndexError));
        } else if (idx instanceof PySlice) {
            return getSlice((PySlice)idx);
        } else {
            throw Py.TypeError(getTypeName() + " indices must be integers");
        }
    }

    public PyObject getSlice(PySlice slice) {
        int[] indices = slice.indicesEx(len());
        return getSlice(indices[0], indices[1], indices[2]);
    }

    public PyObject checkIdxAndFindItem(int idx) {
        idx = fixindex(idx);
        if(idx == -1) {
            return null;
        } else {
            return getItem(idx);
        }
    }

    private int checkIdx(int idx) {
        int i = fixindex(idx);
        if (i == -1) {
            throw Py.IndexError(getTypeName() + " assignment index out of range");
        }
        return i;
    }

    int fixindex(int index) {
        int l = len();
        if(index < 0) {
            index += l;
        }
        if(index < 0 || index >= l) {
            return -1;
        } else {
            return index;
        }
    }

    /**
     * Implement the deletion of a slice. This method is called by
     * {@link #checkIdxAndDelItem(PyObject)} when the argument is a {@link PySlice}. The argument is
     * the return from {@link PySlice#indicesEx(int)}.
     *
     * @param indices containing [start, stop, step, count] of the slice to delete
     */
    protected void delSlice(int[] indices) {
        int p = indices[0], step = indices[2], count = indices[3];
        if (step > 1) {
            /*
             * Key observation: each deletion causes material to the right (not yet visited) to move
             * one place left, so the deletion pointer moves by step-1 not by step as might be
             * expected.
             */
            step = step - 1;
            for (; count > 0; --count, p += step) {
                delItem(p);
            }
        } else if (step < 1) {
            // Deletion pointer moves leftwards, and moving data is to the right
            for (; count > 0; --count, p += step) {
                delItem(p);
            }
        } else { // step == 1, since it is never == 0
            // Slice is contiguous: use range delete
            if (count > 0) {
                delItems(p, p + count);
            }
        }
    }
}
