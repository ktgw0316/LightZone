/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.utils.directory;

import java.nio.file.Path;

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
     */
    void directoryChanged(Path parentDir, Path file, String kind);
}
/* vim:set et sw=4 ts=4: */
