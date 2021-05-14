/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;

import org.python.core.ClassDictInit;
import org.python.core.Py;
import org.python.core.PyBuiltinMethodSet;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyUnicode;
import org.python.core.Traverseproc;
import org.python.core.Visitproc;

import com.ziclix.python.sql.util.PyArgParser;
import org.python.core.ContextManager;
import org.python.core.ThreadState;


/**
 * These objects represent a database cursor, which is used to manage the
 * context of a fetch operation.
 *
 * @author brian zimmer
 */
public class PyCursor extends PyObject implements ClassDictInit, WarningListener,
        ContextManager, Traverseproc {

    /** Field fetch */
    protected Fetch fetch;

    /** Field closed */
    private boolean closed;

    /** Field arraysize */
    protected int arraysize;

    /** Field softspace */
    protected int softspace;

    /** Field rsType */
    protected PyObject rsType;

    /** Field rsConcur */
    protected PyObject rsConcur;

    /** Field warnings */
    protected PyObject warnings;

    /** Field warnings */
    protected PyObject lastrowid;

    /** Field updatecount */
    protected PyObject updatecount;

    /** Field dynamicFetch */
    protected boolean dynamicFetch;

    /** Field connection */
    protected PyConnection connection;

    /** Field datahandler */
    protected DataHandler datahandler;

    /** Field statement */
    protected PyStatement statement;

    // they are stateless instances, so we only need to instantiate it once
    private static final DataHandler DATAHANDLER = DataHandler.getSystemDataHandler();

    /**
     * Create the cursor with a static fetch.
     *
     * @param connection
     */
    PyCursor(PyConnection connection) {
        this(connection, false);
    }

    /**
     * Create the cursor, optionally choosing the type of fetch (static or dynamic).
     * If dynamicFetch is true, then use a dynamic fetch.
     *
     * @param connection
     * @param dynamicFetch
     */
    PyCursor(PyConnection connection, boolean dynamicFetch) {
        this.arraysize = 1;
        this.softspace = 0;
        this.closed = false;
        this.rsType = Py.None;
        this.rsConcur = Py.None;
        this.connection = connection;
        this.datahandler = DATAHANDLER;
        this.dynamicFetch = dynamicFetch;

        // constructs the appropriate Fetch among other things
        this.clear();
    }

    /**
     * Create the cursor, optionally choosing the type of fetch (static or dynamic).
     * If dynamicFetch is true, then use a dynamic fetch.
     * rsType and rsConcur are used to create the Statement if both are non-None
     *
     * @param connection
     * @param dynamicFetch
     * @param rsType
     * @param rsConcur
     */
    PyCursor(PyConnection connection, boolean dynamicFetch, PyObject rsType, PyObject rsConcur) {
        this(connection, dynamicFetch);
        this.rsType = rsType;
        this.rsConcur = rsConcur;
    }

    /** Field __methods__ */
    protected static PyList __methods__;

    /** Field __members__ */
    protected static PyList __members__;

    static {
        PyObject[] m = new PyObject[9];

        m[0] = new PyString("close");
        m[1] = new PyString("execute");
        m[2] = new PyString("executemany");
        m[3] = new PyString("fetchone");
        m[4] = new PyString("fetchall");
        m[5] = new PyString("fetchmany");
        m[6] = new PyString("callproc");
        m[7] = new PyString("next");
        m[8] = new PyString("write");
        __methods__ = new PyList(m);
        m = new PyObject[11];
        m[0] = new PyString("arraysize");
        m[1] = new PyString("rowcount");
        m[2] = new PyString("rownumber");
        m[3] = new PyString("description");
        m[4] = new PyString("datahandler");
        m[5] = new PyString("warnings");
        m[6] = new PyString("lastrowid");
        m[7] = new PyString("updatecount");
        m[8] = new PyString("softspace");
        m[9] = new PyString("closed");
        m[10] = new PyString("connection");
        __members__ = new PyList(m);
    }

    /**
     * String representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return String.format("<PyCursor object at %s>", Py.idstr(this));
    }

    /**
     * Sets the attribute name to value.
     *
     * @param name
     * @param value
     */
    @Override
    public void __setattr__(String name, PyObject value) {
        if ("arraysize".equals(name)) {
            this.arraysize = value.asInt();
        } else if ("softspace".equals(name)) {
            this.softspace = value.asInt();
        } else if ("datahandler".equals(name)) {
            this.datahandler = (DataHandler)value.__tojava__(DataHandler.class);
        } else {
            super.__setattr__(name, value);
        }
    }

    /**
     * Gets the value of the attribute name.
     *
     * @param name
     * @return the attribute for the given name
     */
    @Override
    public PyObject __findattr_ex__(String name) {
        if ("arraysize".equals(name)) {
            return Py.newInteger(arraysize);
        } else if ("softspace".equals(name)) {
            return Py.newInteger(softspace);
        } else if ("__methods__".equals(name)) {
            return __methods__;
        } else if ("__members__".equals(name)) {
            return __members__;
        } else if ("description".equals(name)) {
            return this.fetch.description;
        } else if ("rowcount".equals(name)) {
            return Py.newInteger(this.fetch.rowcount);
        } else if ("rownumber".equals(name)) {
            int rn = this.fetch.rownumber;
            return (rn < 0) ? Py.None : Py.newInteger(rn);
        } else if ("warnings".equals(name)) {
            return warnings;
        } else if ("lastrowid".equals(name)) {
            return lastrowid;
        } else if ("updatecount".equals(name)) {
            return updatecount;
        } else if ("datahandler".equals(name)) {
            return Py.java2py(this.datahandler);
        } else if ("dynamic".equals(name)) {
            return this.dynamicFetch ? Py.One : Py.Zero;
        } else if ("connection".equals(name)) {
            return this.connection;
        } else if ("closed".equals(name)) {
            return Py.newBoolean(closed);
        } else if ("callproc".equals(name)) {
            try {
                // dynamically decide on the the attribute based on the driver
                if (!getMetaData().supportsStoredProcedures()) {
                    return null;
                }
            } catch (Throwable t) {}
        }

        return super.__findattr_ex__(name);
    }

    /**
     * Initializes the object's namespace.
     *
     * @param dict
     */
    static public void classDictInit(PyObject dict) {
        dict.__setitem__("fetchmany", new CursorFunc("fetchmany", 0, 0, 1, "fetch specified number of rows"));
        dict.__setitem__("close", new CursorFunc("close", 1, 0, "close the cursor"));
        dict.__setitem__("fetchall", new CursorFunc("fetchall", 2, 0, "fetch all results"));
        dict.__setitem__("fetchone", new CursorFunc("fetchone", 3, 0, "fetch the next result"));
        dict.__setitem__("nextset", new CursorFunc("nextset", 4, 0, "return next set or None"));
        dict.__setitem__("execute", new CursorFunc("execute", 5, 1, 4, "execute the sql expression"));
        dict.__setitem__("setinputsizes", new CursorFunc("setinputsizes", 6, 1, "not implemented"));
        dict.__setitem__("setoutputsize", new CursorFunc("setoutputsize", 7, 1, 2, "not implemented"));
        dict.__setitem__("callproc", new CursorFunc("callproc", 8, 1, 4, "executes a stored procedure"));
        dict.__setitem__("executemany", new CursorFunc("executemany", 9, 1, 3, "execute sql with the parameter list"));
        dict.__setitem__("scroll", new CursorFunc("scroll", 10, 1, 2, "scroll the cursor in the result set to a new position according to mode"));
        dict.__setitem__("write", new CursorFunc("write", 11, 1, "execute the sql written to this file-like object"));
        dict.__setitem__("prepare", new CursorFunc("prepare", 12, 1, "prepare the sql statement for later execution"));
        dict.__setitem__("__enter__", new CursorFunc("__enter__", 13, 0, 0, "__enter__"));
        dict.__setitem__("__exit__", new CursorFunc("__exit__", 14, 3, 3, "__exit__"));

        // hide from python
        dict.__setitem__("classDictInit", null);
        dict.__setitem__("toString", null);
        dict.__setitem__("getDataHandler", null);
        dict.__setitem__("warning", null);
        dict.__setitem__("fetch", null);
        dict.__setitem__("statement", null);
        dict.__setitem__("dynamicFetch", null);
        dict.__setitem__("getPyClass", null);
        dict.__setitem__("rsConcur", null);
        dict.__setitem__("rsType", null);
    }

    /**
     * Delete the cursor.
     *
     */
    public void __del__() {
        close();
    }

    /**
     * Close the cursor now (rather than whenever __del__ is called).
     * The cursor will be unusable from this point forward; an Error
     * (or subclass) exception will be raised if any operation is
     * attempted with the cursor.
     *
     */
    public void close() {
        try {
            this.clear();
            this.connection.remove(this);
        } finally {
            this.closed = true;
        }
    }

    /**
     * Returns an iteratable object.
     *
     * @return PyObject
     *
     * @since Jython 2.2, DB API 2.0+
     */
    @Override
    public PyObject __iter__() {
        return this;
    }

    /**
     * Returns the next row from the currently executing SQL statement
     * using the same semantics as .fetchone().  A StopIteration
     * exception is raised when the result set is exhausted for Python
     * versions 2.2 and later.
     *
     * @return PyObject
     *
     * @since Jython 2.2, DB API 2.0+
     */
    public PyObject next() {
        PyObject row = __iternext__();
        if (row == null) {
            throw Py.StopIteration("");
        }
        return row;
    }

    /**
     * Return the next element of the sequence that this is an iterator
     * for. Returns null when the end of the sequence is reached.
     *
     * @since Jython 2.2
     *
     * @return PyObject
     */
    @Override
    public PyObject __iternext__() {
        PyObject row = fetchone();
        return row.__nonzero__() ? row : null;
    }

    /**
     * Return ths DatabaseMetaData for the current connection.
     *
     * @return DatabaseMetaData
     *
     * @throws SQLException
     */
    protected DatabaseMetaData getMetaData() throws SQLException {
        return this.connection.connection.getMetaData();
    }

    /**
     * Return the currently bound DataHandler.
     *
     * @return DataHandler
     */
    public DataHandler getDataHandler() {
        return this.datahandler;
    }

    /**
     * Prepare a statement ready for executing.
     *
     * @param sql the sql to execute or a prepared statement
     * @param maxRows max number of rows to be returned
     * @param prepared if true, prepare the statement, otherwise create a normal statement
     *
     * @return PyStatement
     */
    private PyStatement prepareStatement(PyObject sql, PyObject maxRows, boolean prepared) {
        PyStatement stmt = null;

        if (sql == Py.None) {
            return null;
        }

        try {
            if (sql instanceof PyStatement) {
                stmt = (PyStatement)sql;
            } else {
                Statement sqlStatement = null;
                String sqlString =
                        sql instanceof PyUnicode ? sql.toString() : sql.__str__().toString();

                if (sqlString.trim().length() == 0) {
                    return null;
                }

                boolean normal = (this.rsType == Py.None && this.rsConcur == Py.None);

                if (normal) {
                    if (prepared) {
                        sqlStatement = this.connection.connection.prepareStatement(sqlString);
                    } else {
                        sqlStatement = this.connection.connection.createStatement();
                    }
                } else {
                    int t = this.rsType.asInt();
                    int c = this.rsConcur.asInt();

                    if (prepared) {
                        sqlStatement = this.connection.connection.prepareStatement(sqlString, t,
                                                                                   c);
                    } else {
                        sqlStatement = this.connection.connection.createStatement(t, c);
                    }
                }

                int style = prepared
                        ? PyStatement.STATEMENT_PREPARED : PyStatement.STATEMENT_STATIC;

                stmt = new PyStatement(sqlStatement, sqlString, style);
            }

            if (maxRows != Py.None) {
                stmt.statement.setMaxRows(maxRows.asInt());
            }
        } catch (AbstractMethodError e) {
            throw zxJDBC.makeException(zxJDBC.NotSupportedError,
                                       zxJDBC.getString("nodynamiccursors"));
        } catch (PyException e) {
            throw e;
        } catch (Throwable e) {
            throw zxJDBC.makeException(e);
        }

        return stmt;
    }

    /**
     * This method is optional since not all databases provide stored procedures.
     *
     * Call a stored database procedure with the given name. The sequence of parameters
     * must contain one entry for each argument that the procedure expects. The result of
     * the call is returned as modified copy of the input sequence. Input parameters are
     * left untouched, output and input/output parameters replaced with possibly new values.
     *
     * The procedure may also provide a result set as output. This must then be made available
     * through the standard fetchXXX() methods.
     *
     * @param name
     * @param params
     * @param bindings
     * @param maxRows
     */
    public void callproc(PyObject name, final PyObject params, PyObject bindings,
                         PyObject maxRows) {
        this.clear();

        try {
            if (getMetaData().supportsStoredProcedures()) {
                if (isSeqSeq(params)) {
                    throw zxJDBC.makeException(zxJDBC.NotSupportedError,
                                               "sequence of sequences is not supported");
                }

                final Procedure procedure = datahandler.getProcedure(this, name);
                Statement stmt = procedure.prepareCall(this.rsType, this.rsConcur);

                if (maxRows != Py.None) {
                    stmt.setMaxRows(maxRows.asInt());
                }

                // get the bindings per the stored proc spec
                PyDictionary callableBindings = new PyDictionary();

                procedure.normalizeInput(params, callableBindings);

                // overwrite with any user specific bindings
                if (bindings instanceof PyDictionary) {
                    callableBindings.update(bindings);
                }

                this.statement = new PyStatement(stmt, procedure);

                this.execute(params, callableBindings);
            } else {
                throw zxJDBC.makeException(zxJDBC.NotSupportedError,
                                           zxJDBC.getString("noStoredProc"));
            }
        } catch (Throwable t) {
            if (statement != null) {
                statement.close();
            }
            throw zxJDBC.makeException(t);
        }
    }

    /**
     * Prepare a database operation (query or command) and then execute it against all
     * parameter sequences or mappings found in the sequence seq_of_parameters.
     * Modules are free to implement this method using multiple calls to the execute()
     * method or by using array operations to have the database process the sequence as
     * a whole in one call.
     *
     * The same comments as for execute() also apply accordingly to this method.
     *
     * Return values are not defined.
     *
     * @param sql
     * @param params
     * @param bindings
     * @param maxRows
     */
    public void executemany(PyObject sql, PyObject params, PyObject bindings, PyObject maxRows) {
        if (isSeq(params) && params.__len__() == 0) {
            //executemany with an empty params tuple is a no-op
            return;
        }
        execute(sql, params, bindings, maxRows);
    }

    /**
     * Prepare and execute a database operation (query or command).
     * Parameters may be provided as sequence or mapping and will
     * be bound to variables in the operation. Variables are specified
     * in a database-specific notation (see the module's paramstyle
     * attribute for details).
     *
     * A reference to the operation will be retained by the cursor.
     * If the same operation object is passed in again, then the cursor
     * can optimize its behavior. This is most effective for algorithms
     * where the same operation is used, but different parameters are
     * bound to it (many times).
     *
     * For maximum efficiency when reusing an operation, it is best to
     * use the setinputsizes() method to specify the parameter types and
     * sizes ahead of time. It is legal for a parameter to not match the
     * predefined information; the implementation should compensate, possibly
     * with a loss of efficiency.
     *
     * The parameters may also be specified as list of tuples to e.g. insert
     * multiple rows in a single operation, but this kind of usage is
     * deprecated: executemany() should be used instead.
     *
     * Return values are not defined.
     *
     * @param sql sql string or prepared statement
     * @param params params for a prepared statement
     * @param bindings dictionary of (param index : SQLType binding)
     * @param maxRows integer value of max rows
     */
    public void execute(final PyObject sql, PyObject params, PyObject bindings, PyObject maxRows) {
        int rowIndex = -1;
        this.clear();

        boolean hasParams = hasParams(params);
        PyStatement stmt = this.prepareStatement(sql, maxRows, hasParams);

        if (stmt == null) {
            return;
        }

        this.statement = stmt;

        try {
            synchronized (this.statement) {
                if (hasParams) {

                    // if we have a sequence of sequences, let's run through them and finish
                    if (isSeqSeq(params)) {

                        // [(3, 4)] or [(3, 4), (5, 6)]
                        rowIndex = 0;
                        for (int i = 0, len = params.__len__(); i < len; i++) {
                            PyObject param = params.__getitem__(i);

                            this.execute(param, bindings);
                            rowIndex++;
                        }
                    } else {
                        this.execute(params, bindings);
                    }
                } else {
                    // execute the sql string straight up
                    this.execute(Py.None, Py.None);
                }
            }
        } catch (Throwable t) {
            if (statement != null && !(sql instanceof PyStatement)) {
                // only close static, single-use statements
                statement.close();
            }
            throw zxJDBC.makeException(zxJDBC.Error, t, rowIndex);
        }
    }

    /**
     * Execute the current sql statement.  Some generic functionality such
     * as updating the lastrowid and updatecount occur as well.
     */
    protected void execute(PyObject params, PyObject bindings) {
        try {
            Statement stmt = this.statement.statement;
            this.datahandler.preExecute(stmt);

            // this performs the SQL execution and fetch per the Statement type
            this.statement.execute(this, params, bindings);

            this.updateAttributes(stmt.getUpdateCount());
            warning(new WarningEvent(this, stmt.getWarnings()));
            this.datahandler.postExecute(stmt);
        } catch (PyException e) {
            throw e;
        } catch (Throwable e) {
            throw zxJDBC.makeException(e);
        }
    }

    /**
     * Update the cursor's lastrowid and updatecount.
     *
     * @param updateCount The int value of updatecount
     * @throws SQLException
     */
    private void updateAttributes(int updateCount) throws SQLException {
        lastrowid = datahandler.getRowId(statement.statement);
        updatecount = updateCount < 0 ? Py.None : Py.newInteger(updateCount);
    }

    /**
     * Fetch the next row of a query result set, returning a single sequence,
     * or None when no more data is available.
     *
     * An Error (or subclass) exception is raised if the previous call to
     * executeXXX() did not produce any result set or no call was issued yet.
     *
     * @return a single sequence from the result set, or None when no more data is available
     */
    public PyObject fetchone() {
        ensureOpen();
        return this.fetch.fetchone();
    }

    /**
     * Fetch all (remaining) rows of a query result, returning them as a sequence
     * of sequences (e.g. a list of tuples). Note that the cursor's arraysize attribute
     * can affect the performance of this operation.
     *
     * An Error (or subclass) exception is raised if the previous call to executeXXX()
     * did not produce any result set or no call was issued yet.
     *
     * @return a sequence of sequences from the result set, or an empty sequence when
     *         no more data is available
     */
    public PyObject fetchall() {
        ensureOpen();
        return this.fetch.fetchall();
    }

    /**
     * Fetch the next set of rows of a query result, returning a sequence of
     * sequences (e.g. a list of tuples). An empty sequence is returned when
     * no more rows are available.
     *
     * The number of rows to fetch per call is specified by the parameter. If
     * it is not given, the cursor's arraysize determines the number of rows
     * to be fetched. The method should try to fetch as many rows as indicated
     * by the size parameter. If this is not possible due to the specified number
     * of rows not being available, fewer rows may be returned.
     *
     * An Error (or subclass) exception is raised if the previous call to executeXXX()
     * did not produce any result set or no call was issued yet.
     *
     * Note there are performance considerations involved with the size parameter.
     * For optimal performance, it is usually best to use the arraysize attribute.
     * If the size parameter is used, then it is best for it to retain the same value
     * from one fetchmany() call to the next.
     *
     * @param size
     * @return a sequence of sequences from the result set, or an empty sequence when
     *         no more data is available
     */
    public PyObject fetchmany(int size) {
        ensureOpen();
        return this.fetch.fetchmany(size);
    }

    /**
     * Move the result pointer to the next set if available.
     *
     * @return true if more sets exist, else None
     */
    public PyObject nextset() {
        ensureOpen();
        PyObject nextset = fetch.nextset();

        // If the fetch is exhausted and multiple ResultSets are supported, try addding a
        // next ResultSet. XXX: DynamicFetch currently isn't so tailored for this
        if (!nextset.__nonzero__() && connection.supportsMultipleResultSets && !dynamicFetch) {
            Statement stmt = statement.statement;
            try {
                boolean hasMoreResults;
                int updateCount = -1;
                if ((hasMoreResults = stmt.getMoreResults())
                    || (updateCount = stmt.getUpdateCount()) != -1) {
                    // Only call getUpdateCount once, per its docs
                    updateAttributes(!hasMoreResults ? updateCount : stmt.getUpdateCount());
                    fetch.add(stmt.getResultSet());
                    nextset = Py.One;
                }
            } catch (SQLException sqle) {
                throw zxJDBC.makeException(sqle);
            }
        }

        return nextset;
    }

    /**
     * Prepare a sql statement for later execution.
     *
     * @param sql The sql string to be prepared.
     *
     * @return A prepared statement usable with .executeXXX()
     */
    public PyStatement prepare(PyObject sql) {
        PyStatement s = this.prepareStatement(sql, Py.None, true);

        // add to the set of statements which are leaving our control
        this.connection.add(s);

        return s;
    }

    /**
     * Scroll the cursor in the result set to a new position according
     * to mode.
     *
     * If mode is 'relative' (default), value is taken as offset to
     * the current position in the result set, if set to 'absolute',
     * value states an absolute target position.
     *
     * An IndexError should be raised in case a scroll operation would
     * leave the result set. In this case, the cursor position is left
     * undefined (ideal would be to not move the cursor at all).
     *
     * Note: This method should use native scrollable cursors, if
     * available, or revert to an emulation for forward-only
     * scrollable cursors. The method may raise NotSupportedErrors to
     * signal that a specific operation is not supported by the
     * database (e.g. backward scrolling).
     *
     *
     * @param value
     * @param mode
     *
     */
    public void scroll(int value, String mode) {
        ensureOpen();
        this.fetch.scroll(value, mode);
    }

    /**
     * Adds a warning to the tuple and will follow the chain as necessary.
     *
     * @param event
     */
    public void warning(WarningEvent event) {
        if (this.warnings == Py.None) {
            this.warnings = new PyList();
        }

        SQLWarning warning = event.getWarning();
        while (warning != null) {

            // there are three parts: (reason, state, vendorCode)
            PyObject[] warn =
                Py.javas2pys(warning.getMessage(), warning.getSQLState(), warning.getErrorCode());

            // add the warning to the list
            ((PyList)this.warnings).append(new PyTuple(warn));

            warning = warning.getNextWarning();
        }
    }

    /**
     * Resets the cursor state. This includes flushing the warnings
     * and any previous results.
     *
     */
    protected void clear() {
        ensureOpen();

        this.warnings = Py.None;
        this.lastrowid = Py.None;
        this.updatecount = Py.newInteger(-1);

        try {
            this.fetch.close();
        } catch (Throwable e) {
            // ok
        } finally {
            this.fetch = Fetch.newFetch(this.datahandler, this.dynamicFetch);
            this.fetch.addWarningListener(this);
        }

        if (this.statement != null) {
            // Finally done with the Statement: only close it if we created it 
            try {
                if (!this.connection.contains(this.statement)) {
                    this.statement.close();
                }
            } finally {
                this.statement = null;
            }
        }
    }

    /**
     * Method isSeq
     *
     * @param object
     *
     * @return true for any PyList, PyTuple or java.util.List
     *
     */
    public static boolean isSeq(PyObject object) {
        if (object == null || object == Py.None) {
            return false;
        }

        if (object.__tojava__(List.class) != Py.NoConversion) {
            return true;
        }

        // originally checked for __getitem__ and __len__, but this is true for PyString
        // and we don't want to insert one character at a time
        return object instanceof PyList || object instanceof PyTuple;
    }

    /**
     * Method hasParams
     *
     * @param params
     *
     * @return boolean
     *
     */
    public static boolean hasParams(PyObject params) {
        if (Py.None == params) {
            return false;
        }

        boolean isSeq = isSeq(params);
        // the optional argument better be a sequence
        if (!isSeq) {
            throw zxJDBC.makeException(zxJDBC.ProgrammingError,
                                       zxJDBC.getString("optionalSecond"));
        }
        return params.__len__() > 0;
    }

    /**
     * Method isSeqSeq
     *
     * @param object
     *
     * @return true is a sequence of sequences
     *
     */
    public static boolean isSeqSeq(PyObject object) {
        if (isSeq(object) && (object.__len__() > 0)) {
            for (int i = 0; i < object.__len__(); i++) {
                if (!isSeq(object.__finditem__(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Throw a ProgrammingError if the cursor has been closed.
     */
    private void ensureOpen() {
        if (closed) {
            throw zxJDBC.makeException(zxJDBC.ProgrammingError, "cursor is closed");
        }
    }

    public PyObject __enter__(ThreadState ts) {
        return this;
    }

    public PyObject __enter__() {
        return this;
    }

    public boolean __exit__(ThreadState ts, PyException exception) {
        close();
        return false;
    }

    public boolean __exit__(PyObject type, PyObject value, PyObject traceback) {
        close();
        return false;
    }


    /* Traverseproc implementation */
    @Override
    public int traverse(Visitproc visit, Object arg) {        
        int retVal;
        if (fetch != null) {
            retVal = fetch.traverse(visit, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (rsType != null) {
            retVal = visit.visit(rsType, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (rsConcur != null) {
            retVal = visit.visit(rsConcur, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (warnings != null) {
            retVal = visit.visit(warnings, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (lastrowid != null) {
            retVal = visit.visit(lastrowid, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (updatecount != null) {
            retVal = visit.visit(updatecount, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        if (connection != null) {
            retVal = visit.visit(connection, arg);
            if (retVal != 0) {
                return retVal;
            }
        }
        return statement != null ? visit.visit(statement, arg) : 0;
    }

    @Override
    public boolean refersDirectlyTo(PyObject ob) {
        if (ob == null) {
            return false;
        } else if (ob == rsType || ob == rsConcur || ob == warnings || ob == lastrowid
                || ob == updatecount || ob == connection || ob == statement) {
            return true;
        } else {
            return fetch.refersDirectlyTo(ob);
        }
    }
}

class CursorFunc extends PyBuiltinMethodSet {

    CursorFunc(String name, int index, int argcount, String doc) {
        this(name, index, argcount, argcount, doc);
    }

    CursorFunc(String name, int index, int minargs, int maxargs, String doc) {
        super(name, index, minargs, maxargs, doc, PyCursor.class);
    }

    @Override
    public PyObject __call__() {
        PyCursor cursor = (PyCursor)__self__;
        switch (index) {
        case 0 :
            return cursor.fetchmany(cursor.arraysize);
        case 1 :
            cursor.close();
            return Py.None;
        case 2 :
            return cursor.fetchall();
        case 3 :
            return cursor.fetchone();
        case 4 :
            return cursor.nextset();
        case 13 :
            return cursor.__enter__();
        default :
            throw info.unexpectedCall(0, false);
        }
    }

    @Override
    public PyObject __call__(PyObject arg) {
        PyCursor cursor = (PyCursor)__self__;
        switch (index) {
        case 0 :
            return cursor.fetchmany(arg.asInt());
        case 5 :
            cursor.execute(arg, Py.None, Py.None, Py.None);
            return Py.None;
        case 6 :
        case 7 :
            return Py.None;
        case 8 :
            cursor.callproc(arg, Py.None, Py.None, Py.None);
            return Py.None;
        case 9 :
            cursor.executemany(arg, Py.None, Py.None, Py.None);
            return Py.None;
        case 10 :
            cursor.scroll(arg.asInt(), "relative");
            return Py.None;
        case 11 :
            cursor.execute(arg, Py.None, Py.None, Py.None);
            return Py.None;
        case 12 :
            return cursor.prepare(arg);
        default :
            throw info.unexpectedCall(1, false);
        }
    }

    @Override
    public PyObject __call__(PyObject arga, PyObject argb) {
        PyCursor cursor = (PyCursor)__self__;
        switch (index) {
        case 5 :
            cursor.execute(arga, argb, Py.None, Py.None);
            return Py.None;
        case 7 :
            return Py.None;
        case 8 :
            cursor.callproc(arga, argb, Py.None, Py.None);
            return Py.None;
        case 9 :
            cursor.executemany(arga, argb, Py.None, Py.None);
            return Py.None;
        case 10 :
            cursor.scroll(arga.asInt(), argb.toString());
            return Py.None;
        default :
            throw info.unexpectedCall(2, false);
        }
    }

    @Override
    public PyObject __call__(PyObject arga, PyObject argb, PyObject argc) {
        PyCursor cursor = (PyCursor)__self__;
        switch (index) {
        case 5 :
            cursor.execute(arga, argb, argc, Py.None);
            return Py.None;
        case 8 :
            cursor.callproc(arga, argb, argc, Py.None);
            return Py.None;
        case 9 :
            cursor.executemany(arga, argb, argc, Py.None);
            return Py.None;
        case 14 :
            return Py.newBoolean(cursor.__exit__(arga, argc, argc));
        default :
            throw info.unexpectedCall(3, false);
        }
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        PyCursor cursor = (PyCursor)__self__;
        PyArgParser parser = new PyArgParser(args, keywords);
        PyObject sql = parser.arg(0);
        PyObject params = parser.kw("params", Py.None);
        PyObject bindings = parser.kw("bindings", Py.None);
        PyObject maxrows = parser.kw("maxrows", Py.None);

        params = parser.numArg() >= 2 ? parser.arg(1) : params;
        bindings = parser.numArg() >= 3 ? parser.arg(2) : bindings;
        maxrows = parser.numArg() >= 4 ? parser.arg(3) : maxrows;

        switch (index) {
        case 5 :
            cursor.execute(sql, params, bindings, maxrows);
            return Py.None;
        case 8 :
            cursor.callproc(sql, params, bindings, maxrows);
            return Py.None;
        case 9 :
            cursor.executemany(sql, params, bindings, maxrows);
            return Py.None;
        default :
            throw info.unexpectedCall(args.length, true);
        }
    }
}
