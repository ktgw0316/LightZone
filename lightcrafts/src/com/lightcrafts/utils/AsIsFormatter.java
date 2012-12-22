/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * <code>AsIsFormatter</code> is-a {@link Formatter} that does no formatting of
 * the log message and returns it as-is.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class AsIsFormatter extends Formatter {

    /**
     * &quot;Format&quot; a {@link LogRecord}.
     *
     * @param rec The {@link LogRecord} to format.
     * @return Returns the {@link LogRecord}'s message only appended with a
     * newline.
     */
    public String format( LogRecord rec ) {
        final Throwable t = rec.getThrown();
        return rec.getMessage() + (t != null ? t.toString() : "") + "\n";
    }

}
/* vim:set et sw=4 ts=4: */
