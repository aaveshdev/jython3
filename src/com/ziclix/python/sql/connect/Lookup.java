/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.connect;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.Untraversable;

import com.ziclix.python.sql.PyConnection;
import com.ziclix.python.sql.zxJDBC;
import com.ziclix.python.sql.util.PyArgParser;

/**
 * Establish a connection through a JNDI lookup.  The bound object can be either a
 * <code>DataSource</code>, <code>ConnectionPooledDataSource</code>,
 * <code>Connection</code> or a <code>String</code>.  If it's a <code>String</code> the
 * value is passed to the DriverManager to obtain a connection, otherwise the
 * <code>Connection</code> is established using the object.
 *
 * @author brian zimmer
 */
@Untraversable
public class Lookup extends PyObject {

    private static final PyString _doc =
            new PyString("establish a connection through a JNDI lookup");

    /**
     * Method __findattr__
     *
     * @param name
     * @return PyObject
     */
    @Override
    public PyObject __findattr_ex__(String name) {
        if ("__doc__".equals(name)) {
            return _doc;
        }

        return super.__findattr_ex__(name);
    }

    /**
     * Expects a single PyString argument which is the JNDI name of the bound Connection
     * or DataSource.  If any keywords are passed, an attempt is made to match the keyword
     * to a static final Field on javax.naming.Context.  If the Field is found, the value
     * of the Field is substituted as the key and the value of the keyword is the value
     * put in the Hashtable environment.  If the Field is not found, the key is the
     * keyword with no substitutions.
     */
    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        Object ref = null;
        Connection connection = null;
        Hashtable<String, Object> env = new Hashtable<String, Object>();

        // figure out the correct params
        PyArgParser parser = new PyArgParser(args, keywords);
        Object jndiName = parser.arg(0).__tojava__(String.class);

        if ((jndiName == null) || (jndiName == Py.NoConversion)) {
            throw zxJDBC.makeException(zxJDBC.DatabaseError, "lookup name is null");
        }

        // add any Context properties
        String[] kws = parser.kws();

        for (int i = 0; i < kws.length; i++) {
            String keyword = kws[i], fieldname = null;
            Object value = parser.kw(keyword).__tojava__(Object.class);

            try {
                Field field = Context.class.getField(keyword);

                fieldname = (String) field.get(Context.class);
            } catch (IllegalAccessException e) {
                throw zxJDBC.makeException(zxJDBC.ProgrammingError, e);
            } catch (NoSuchFieldException e) {
                fieldname = keyword;
            }

            env.put(fieldname, value);
        }

        InitialContext context = null;
        try {
            context = new InitialContext(env);
            ref = context.lookup((String) jndiName);
        } catch (NamingException e) {
            throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    // ok
                }
            }
        }

        if (ref == null) {
            throw zxJDBC.makeException(zxJDBC.ProgrammingError,
                                       "object [" + jndiName + "] not found in JNDI");
        }

        try {
            if (ref instanceof String) {
                connection = DriverManager.getConnection(((String) ref));
            } else if (ref instanceof Connection) {
                connection = (Connection) ref;
            } else if (ref instanceof DataSource) {
                connection = ((DataSource) ref).getConnection();
            } else if (ref instanceof ConnectionPoolDataSource) {
                connection =
                        ((ConnectionPoolDataSource) ref).getPooledConnection().getConnection();
            }
        } catch (SQLException e) {
            throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
        }

        try {
            if (connection == null || connection.isClosed()) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, "unable to establish connection");
            }

            return new PyConnection(connection);
        } catch (SQLException e) {
            throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
        }
    }

    /**
     * Method toString
     *
     * @return String
     */
    @Override
    public String toString() {
        return String.format("<lookup object at %s>", Py.idstr(this));
    }
}
