/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.handler;

import com.ziclix.python.sql.FilterDataHandler;
import com.ziclix.python.sql.DataHandler;

import java.util.Map;
import java.sql.Statement;
import java.sql.SQLException;
import java.lang.reflect.Method;

import org.python.core.PyObject;
import org.python.core.Py;
import org.python.util.Generic;

/**
 * Handle the rowid methods since the API is not available until JDBC 3.0.
 *
 * @author brian zimmer
 */
public abstract class RowIdHandler extends FilterDataHandler {

  private static Map<Class<?>, Object> ROWIDS = Generic.map();
  private static Object CHECKED = new Object();

  public RowIdHandler(DataHandler handler) {
    super(handler);
  }

  /**
   * Return the name of the method that returns the last row id.  The
   * method can take no arguments but the return type is flexible and
   * will be figured out by the Jython runtime system.
   * @return name of the method that returns the last row id
   */
  protected abstract String getRowIdMethodName();

  /**
   * Return the row id of the last insert statement.
   * @param stmt
   * @return an object representing the last row id
   * @throws SQLException
   */
  @Override
public PyObject getRowId(Statement stmt) throws SQLException {

    Class<?> c = stmt.getClass();
    Object o = ROWIDS.get(c);

    if (o == null) {
      synchronized (ROWIDS) {
        try {
          o = c.getMethod(getRowIdMethodName(), (Class[])null);
          ROWIDS.put(c, o);
        } catch (Throwable t) {
          ROWIDS.put(c, CHECKED);
        }
      }
    }

    if (!(o == null || o == CHECKED)) {
      try {
        return Py.java2py(((Method) o).invoke(stmt, (Object[])null));
      } catch (Throwable t) {}
    }

    return super.getRowId(stmt);
  }

}
