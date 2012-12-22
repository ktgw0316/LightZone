/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.thread;

import com.lightcrafts.utils.ProgressIndicator;

/**
 * A <code>ProgressThread</code> is-a {@link CancelableThread} that takes a
 * {@link ProgressIndicator} to update.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class ProgressThread extends CancelableThread {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets this <code>ProgressThread</code>'s {@link ProgressIndicator}.
     *
     * @return Retuns said {@link ProgressIndicator}.
     */
    public final ProgressIndicator getProgressIndicator() {
        return m_progressIndicator;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct a <code>ProgressThread</code>.
     *
     * @param indicator The {@link ProgressIndicator} to use.
     */
    protected ProgressThread( ProgressIndicator indicator ) {
        m_progressIndicator = indicator;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final ProgressIndicator m_progressIndicator;
}
/* vim:set et sw=4 ts=4: */
