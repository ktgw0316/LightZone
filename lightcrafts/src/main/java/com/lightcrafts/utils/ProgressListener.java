/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/**
 * A <code>ProgressListener</code> listens for when/if a progress dialog is
 * canceled by the user.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ProgressListener {

    /**
     * This is called if the user clicks Cancel.
     */
    void progressCancelled();

}
/* vim:set et sw=4 ts=4: */
