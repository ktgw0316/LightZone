/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform;

import java.awt.Frame;

import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.thread.CancelableThread;

/**
 * An <code>ProgressDialog</code> is a platform-indepentent API to display a
 * dialog to the user that contains a progress indicator.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
public interface ProgressDialog extends ProgressIndicator {

    /**
     * Gets whatever {@link Throwable} the {@link CancelableThread} may have
     * thrown during its execution.
     *
     * @return Returns said {@link Throwable} or <code>null</code> if none.
     */
    Throwable getThrown();

    /**
     * Shows a dialog to the user containing a determinate progress indicator.
     *
     * @param parent The parent window.
     * @param thread The {@link CancelableThread} to run while showing the
     * progress dialog.
     * @param message The message to display in the progress dialog.
     * @param minValue The minimum value of the progress indicator.
     * @param maxValue The maximum value of the progress indicator.
     * @param hasCancelButton If <code>true</code>, the dialog will contain an
     * enabled Cancel button the user can click to terminate the
     * {@link CancelableThread} prematurely.
     */
    void showProgress( Frame parent, CancelableThread thread, String message,
                       int minValue, int maxValue, boolean hasCancelButton );

    /**
     * Shows a dialog to the user containing an indeterminate progress
     * indicator.
     *
     * @param parent The parent window.
     * @param thread The {@link CancelableThread} to run while showing the
     * progress dialog.
     * @param message The message to display in the progress dialog.
     * @param hasCancelButton If <code>true</code>, the dialog will contain an
     * enabled Cancel button the user can click to terminate the
     * {@link CancelableThread} prematurely.
     */
    void showProgress( Frame parent, CancelableThread thread, String message,
                       boolean hasCancelButton );

}
/* vim:set et sw=4 ts=4: */
