/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2002 brian zimmer <mailto:bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.procedure;

import com.ziclix.python.sql.Procedure;
import com.ziclix.python.sql.PyCursor;
import org.python.core.Py;
import org.python.core.PyObject;

import java.sql.SQLException;

/**
 * Stored procedure support for SQLServer.
 *
 * @author brian zimmer
 */
public class SQLServerProcedure extends Procedure {

    public SQLServerProcedure(PyCursor cursor, PyObject name) throws SQLException {
        super(cursor, name);
    }

    protected PyObject getDefault() {
        return Py.None;
    }

    protected String getProcedureName() {

        StringBuffer proc = new StringBuffer();

        if (this.procedureSchema.__nonzero__()) {
            proc.append(this.procedureSchema.toString()).append(".");
        }

        return proc.append(this.procedureName.toString()).toString();
    }
}
