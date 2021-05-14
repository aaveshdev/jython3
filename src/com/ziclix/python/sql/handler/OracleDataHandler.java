/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleTypes;
import oracle.sql.BLOB;
import oracle.sql.ROWID;

import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyObject;

import com.ziclix.python.sql.DataHandler;
import com.ziclix.python.sql.FilterDataHandler;

/**
 * Oracle specific data handling.
 *
 * @author brian zimmer
 */
public class OracleDataHandler extends FilterDataHandler {

    /**
     * Default constructor for DataHandler filtering.
     */
    public OracleDataHandler(DataHandler datahandler) {
        super(datahandler);
    }

    /**
     * Method getMetaDataName
     *
     * @param name
     * @return String
     */
    @Override
    public String getMetaDataName(PyObject name) {
        String metaName = super.getMetaDataName(name);
        return (metaName == null) ? null : metaName.toUpperCase();
    }

    /**
     * Provide functionality for Oracle specific types, such as ROWID.
     */
    @Override
    public void setJDBCObject(PreparedStatement stmt, int index, PyObject object, int type)
        throws SQLException {
        if (DataHandler.checkNull(stmt, index, object, type)) {
            return;
        }

        switch (type) {

            case Types.DATE:
                // Oracle DATE is a timestamp with one second precision
                Timestamp timestamp = (Timestamp) object.__tojava__(Timestamp.class);
                if (timestamp != Py.NoConversion) {
                    stmt.setTimestamp(index, timestamp);
                } else {
                    super.setJDBCObject(stmt, index, object, type);
                }
                break;

            case Types.DECIMAL:
                // Oracle is annoying
                Object input = object.__tojava__(Double.class);

                if (input != Py.NoConversion) {
                    stmt.setDouble(index, ((Double) input).doubleValue());

                    break;
                }

                super.setJDBCObject(stmt, index, object, type);
                break;

            case Types.NUMERIC:
                super.setJDBCObject(stmt, index, object, Types.DOUBLE);
                break;

            case OracleTypes.ROWID:
                stmt.setString(index, (String) object.__tojava__(String.class));
                break;

            case OracleTypes.TIMESTAMPLTZ:
            case OracleTypes.TIMESTAMPTZ:
                // XXX: We should include time zone information, but cx_Oracle currently
                // doesn't either
                super.setJDBCObject(stmt, index, object, Types.TIMESTAMP);
                break;
                
            default :
                super.setJDBCObject(stmt, index, object, type);
        }
    }

    /**
     * Provide functionality for Oracle specific types, such as ROWID.
     */
    @Override
    public PyObject getPyObject(ResultSet set, int col, int type) throws SQLException {
        PyObject obj = Py.None;

        switch (type) {

            case Types.DATE:
                // Oracle DATE is a timestamp with one second precision
                obj = Py.newDatetime(set.getTimestamp(col));
                break;
                
            case Types.NUMERIC:
                // Oracle NUMBER encompasses all numeric types
                String number = set.getString(col);
                if (number == null) {
                    obj = Py.None;
                    break;
                }

                int scale;
                int precision;
                // Oracle's DML returning ResultSet doesn't support getMetaData
                try {
                    ResultSetMetaData metaData = set.getMetaData();
                    scale = metaData.getScale(col);
                    precision = metaData.getPrecision(col);
                } catch (SQLException sqle) {
                    scale = precision = 0;
                }

                if (scale == -127) {
                    if (precision == 0) {
                        // Unspecified precision. Normally an integer from a sequence but
                        // possibly any kind of number
                        obj = number.indexOf('.') == -1
                                ? PyInteger.TYPE.__call__(Py.newString(number))
                                : Py.newDecimal(number);
                    } else {
                        // Floating point binary precision
                        obj = Py.newFloat(set.getBigDecimal(col).doubleValue());
                    }
                } else {
                    // Decimal precision. A plain integer when without a scale. Maybe a
                    // plain integer when NUMBER(0,0) (scale and precision unknown,
                    // similar to NUMBER(0,-127) above)
                    obj = scale == 0 && (precision != 0 || number.indexOf('.') == -1)
                            ? PyInteger.TYPE.__call__(Py.newString(number))
                            : Py.newDecimal(number);
                }
                break;

            case Types.BLOB:
                BLOB blob = ((OracleResultSet) set).getBLOB(col);
                obj = blob == null ? Py.None : Py.java2py(read(blob.getBinaryStream()));
                break;

            case OracleTypes.TIMESTAMPLTZ:
            case OracleTypes.TIMESTAMPTZ:
                // XXX: We should include time zone information, but cx_Oracle currently
                // doesn't either
                obj = super.getPyObject(set, col, Types.TIMESTAMP);
                break;
                
            case OracleTypes.ROWID:
                ROWID rowid = ((OracleResultSet) set).getROWID(col);

                if (rowid != null) {
                    obj = Py.java2py(rowid.stringValue());
                }
                break;

            default :
                obj = super.getPyObject(set, col, type);
        }

        return set.wasNull() ? Py.None : obj;
    }

    /**
     * Called when a stored procedure or function is executed and OUT parameters
     * need to be registered with the statement.
     *
     * @param statement
     * @param index the JDBC offset column number
     * @param colType the column as from DatabaseMetaData (eg, procedureColumnOut)
     * @param dataType the JDBC datatype from Types
     * @param dataTypeName the JDBC datatype name
     * @throws SQLException
     */
    @Override
    public void registerOut(CallableStatement statement, int index, int colType, int dataType,
                            String dataTypeName) throws SQLException {

        if (dataType == Types.OTHER) {
            if ("REF CURSOR".equals(dataTypeName)) {
                statement.registerOutParameter(index, OracleTypes.CURSOR);

                return;
            } else if ("PL/SQL RECORD".equals(dataTypeName)) {
                statement.registerOutParameter(index, OracleTypes.CURSOR);

                return;
            }
        }

        super.registerOut(statement, index, colType, dataType, dataTypeName);
    }
}
