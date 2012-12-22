/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.directory;

import java.io.File;

/**
 * A <code>DirectoryListener</code> receives notifications about a collection
 * of directories being monitored by {@link DirectoryMonitor}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface DirectoryListener {

    /**
     * This method is called whenever the given directory has changed or been
     * deleted.
     *
     * @param dir The affected directory.
     */
    void directoryChanged( File dir );

}
/* vim:set et sw=4 ts=4: */
