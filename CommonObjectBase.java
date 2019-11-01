package com.xxx;

import java.util.logging.Logger;
import com.xxx.util.Util;

/**
 * This is a base class from which all xxx objects can inherit.   This is where functionality (such as logging)
 * needed by all xxx classes can be added in a single location.
 */
public class CommonObjectBase {

    /** logger for logging events to the log */
    protected Logger logger = Logger.getLogger(this.getClass().getPackage().getName());

    @Override
    /**
     * A default implementation of hashCode
     */
    public int hashCode() {
        return Util.reflectionHashCode(this, true);
    }

    @Override
    /**
     * A default implementation of equals
     */
    public boolean equals(Object obj) {
        return Util.reflectionEquals(this, obj, true);
    }

    @Override
    /**
     * A default implementation of toString()
     */
    public String toString() {
        return Util.stringify(this);
    }
}
