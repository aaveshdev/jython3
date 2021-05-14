/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.connect;

import java.sql.*;
import java.util.*;

import org.python.core.*;
import com.ziclix.python.sql.*;
import com.ziclix.python.sql.util.*;

/**
 * Connect using DriverManager.
 *
 * @author brian zimmer
 */
@Untraversable
public class Connect extends PyObject {

    private static final PyString _doc = new PyString("establish a connection through java.sql.DriverManager");

    /**
     * Default empty constructor.
     */
    public Connect() {
    }

    public PyObject __findattr_ex__(String name) {
        if ("__doc__".equals(name)) {
            return _doc;
        }
        return super.__findattr_ex__(name);
    }

    /**
     * Establish a connection through DriverManager.
     */
    public PyObject __call__(PyObject[] args, String[] keywords) {

        Connection c = null;
        PyArgParser parser = new PyArgParser(args, keywords);
        Object arg = parser.arg(0).__tojava__(Connection.class);

        if (arg == Py.NoConversion) {
            Properties props = new Properties();
            String url = (String) parser.arg(0).__tojava__(String.class);
            String user = (String) parser.arg(1).__tojava__(String.class);
            String password = (String) parser.arg(2).__tojava__(String.class);
            String driver = (String) parser.arg(3).__tojava__(String.class);

            if (url == null) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, "no url specified");
            }

            if (driver == null) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, "no driver specified");
            }

            // the value can't be null
            props.put("user", (user == null) ? "" : user);
            props.put("password", (password == null) ? "" : password);

            String[] kws = parser.kws();

            for (int i = 0; i < kws.length; i++) {
                Object value = parser.kw(kws[i]).__tojava__(Object.class);

                props.put(kws[i], value);
            }

            try {
                Class.forName(driver);
            } catch (Throwable e) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, "driver [" + driver + "] not found");
            }

            try {
                c = DriverManager.getConnection(url, props);
            } catch (SQLException e) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
            }
        } else {
            c = (Connection) arg;
        }

        try {
            if ((c == null) || c.isClosed()) {
                throw zxJDBC.makeException(zxJDBC.DatabaseError, "unable to establish connection");
            }

            return new PyConnection(c);
        } catch (SQLException e) {
            throw zxJDBC.makeException(zxJDBC.DatabaseError, e);
        }
    }

    /**
     * Method toString
     *
     * @return String
     */
    public String toString() {
        return "<connect object instance at " + Py.id(this) + ">";
    }
}
