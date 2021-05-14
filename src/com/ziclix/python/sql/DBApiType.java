/*
 * Jython Database Specification API 2.0
 *
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql;

import org.python.core.PyInteger;
import org.python.core.Untraversable;

/**
 * This class wraps the types from java.sql.Type in order for
 * PyCursor to differentiate between a regular PyInteger and
 * a SQL datatype value.
 *
 * @author brian zimmer
 */
@Untraversable
public final class DBApiType extends PyInteger {

    /**
     * Constructor DBApiType
     *
     * @param type
     */
    public DBApiType(int type) {
        super(type);
    }

    /**
     * Constructor DBApiType
     *
     * @param type
     */
    public DBApiType(Integer type) {
        super(type.intValue());
    }
}
