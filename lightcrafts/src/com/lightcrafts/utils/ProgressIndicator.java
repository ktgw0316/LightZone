/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/**
 * A <code>ProgressIndicator</code> is used to give feedback to the user about
 * the progress of some lengthy task.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ProgressIndicator {

    /**
     * Advance the progress bar of a determinate progress indicator by the
     * given delta.  If the progress indicator is indeterminate, this does
     * nothing.
     *
     * @param delta The amount to increment the progress indicator by.
     */
    void incrementBy( int delta );

    /**
     * Sets whether the progress indicator is of the indeterminate type.
     *
     * @param indeterminate If <code>true</code>, makes this indicator an
     * interminate progress indicator.
     */
    void setIndeterminate( boolean indeterminate );

    /**
     * Set the maximum value.  If the progress indicator is indeterminate, this
     * does nothing.
     *
     * @param maxValue The new maximum value.
     */
    void setMaximum( int maxValue );

    /**
     * Set the minimum value.  If the progress indicator is indeterminate, this
     * does nothing.
     *
     * @param minValue The new minimum value.
     */
    void setMinimum( int minValue );

}
/* vim:set et sw=4 ts=4: */
