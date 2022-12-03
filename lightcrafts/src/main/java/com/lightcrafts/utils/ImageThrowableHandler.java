/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.util.LinkedList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** This is a minimal Handler (in the sense of java.util.logging) designed
  * to monitor a Logger for logged Throwables.  I created it to use with
  * the image package where there is no way to predict whether an Exception
  * will be logged or thrown.
  * <p>
  * It works like an Enumeration.  To find out if any Throwables have been
  * logged, call <code>hasMoreThrowables()</code>.  To get the next Throwable
  * in order, call <code>nextThrowable()</code>.
  * <p>
  * @author Anton Kast [anton@lightcrafts.com]
  */

public class ImageThrowableHandler extends Handler {

    private static ImageThrowableHandler Instance = new ImageThrowableHandler();

    private LinkedList throwables = new LinkedList();

    private ImageThrowableHandler() {
        Logger logger = Logger.getLogger("com.lightcrafts.image.metadata");
        logger.addHandler(this);
    }

    /** Unlinks the cached List of Throwables so they may be GC'd.
     * @throws SecurityException
     */
    public void close() throws SecurityException {
        throwables = null;
    }

    /** This method does nothing, but is abstract in the base class.
      */
    public void flush() {
    }

    /** If the given LogRecord has a Throwable (as determined by
      * <code>LogRecord.getThrown()</code>), then this Throwable is
      * accumulated in the Throwables list.
      * @param record A LogRecord from a Logger which may include a Throwable.
      */
    public void publish(LogRecord record) {
        Throwable throwable = record.getThrown();
        throwables.add(throwable);
    }

    /** Get the next Throwable in the order they were thrown, or null if all
      * Throwables have been retrieved.
      * @return The next Throwable, or null if there currently is no next
      * Throwable.
      */
    public static Throwable nextThrowable() {
        if (hasMoreThrowables()) {
            return (Throwable) Instance.throwables.getFirst();
        }
        return null;
    }

    /** Test if <code>nextThrowable()</code> would return null.
      * @return True if there is another Throwable to retrieve, othewrise false.
      */
    public static boolean hasMoreThrowables() {
        return (! Instance.throwables.isEmpty());
    }
}
