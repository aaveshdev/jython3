/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2003 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import org.python.core.PyObject;

/**
 * Provide an extensible way to create dates for zxJDBC.
 *
 * @author brian zimmer
 */
public interface DateFactory {

    /**
     * This function constructs an object holding a date value.
     *
     * @param year to set
     * @param month to set
     * @param day to set
     * @return date as PyObject
     */
    public PyObject Date(int year, int month, int day);

    /**
     * This function constructs an object holding a time value.
     *
     * @param hour to set
     * @param minute to set
     * @param second to set
     * @return time as PyObject
     */
    public PyObject Time(int hour, int minute, int second);

    /**
     * This function constructs an object holding a time stamp value.
     *
     * @param year to set
     * @param month to set
     * @param day to set
     * @param hour to set
     * @param minute to set
     * @param second to set
     * @return time stamp as PyObject
     */
    public PyObject Timestamp(int year, int month, int day, int hour, int minute, int second);

    /**
     * This function constructs an object holding a date value from the
     * given ticks value (number of seconds since the epoch; see the
     * documentation of the standard Python <i>time</i> module for details).
     * <p>
     * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
     * while the Java Date object returns time in milliseconds since the epoch.
     * This module adheres to the python API and will therefore use time in
     * seconds rather than milliseconds, so adjust any Java code accordingly.
     *
     * @param ticks number of seconds since the epoch
     * @return PyObject
     */
    public PyObject DateFromTicks(long ticks);

    /**
     * This function constructs an object holding a time value from the
     * given ticks value (number of seconds since the epoch; see the
     * documentation of the standard Python <i>time</i> module for details).
     * <p>
     * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
     * while the Java Date object returns time in milliseconds since the epoch.
     * This module adheres to the python API and will therefore use time in
     * seconds rather than milliseconds, so adjust any Java code accordingly.
     *
     * @param ticks number of seconds since the epoch
     * @return PyObject
     */
    public PyObject TimeFromTicks(long ticks);

    /**
     * This function constructs an object holding a time stamp value from
     * the given ticks value (number of seconds since the epoch; see the
     * documentation of the standard Python <i>time</i> module for details).
     * <p>
     * <i>Note:</i> The DB API 2.0 spec calls for time in seconds since the epoch
     * while the Java Date object returns time in milliseconds since the epoch.
     * This module adheres to the python API and will therefore use time in
     * seconds rather than milliseconds, so adjust any Java code accordingly.
     *
     * @param ticks number of seconds since the epoch
     * @return PyObject
     */
    public PyObject TimestampFromTicks(long ticks);

}
