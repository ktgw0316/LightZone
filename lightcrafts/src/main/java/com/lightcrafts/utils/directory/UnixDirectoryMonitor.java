/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.utils.directory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * A <code>UnixDirectoryMonitor</code> is-a {@link DirectoryMonitor} for
 * monitoring directories of a Unix system (Linux, Mac OS X, etc.).
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class UnixDirectoryMonitor extends DirectoryMonitor {

    private static final Logger logger = LoggerFactory.getLogger(UnixDirectoryMonitor.class);

    public UnixDirectoryMonitor() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException ignored) {
        }
        start();
    }

    /**
     * Add a directory to be monitored.  Adding the same directory more than
     * once is guaranteed to be harmless.
     *
     * @param directory The directory to be monitored.
     */
    @Override
    public void addDirectory(File directory) {
        try {
            final Path dir = directory.toPath();
            WatchKey watchKey = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watchKeyMap.put(watchKey, dir);
            logger.debug("UnixDirectoryMonitor: added {}", dir);
        } catch (InvalidPathException | IOException ignored) {
        }
    }

    /**
     * Remove a directory from being monitored.
     *
     * @param directory The directory to remove.
     * @return Returns <code>true</code> only if the directory was being
     * monitored and thus removed.
     */
    @Override
    public boolean removeDirectory(File directory) {
        boolean removed;
        synchronized (watchKeyMap) {
            removed = watchKeyMap.keySet().stream()
                    .filter(key -> getPathFor(key).equals(directory.toPath()))
                    .anyMatch(key -> watchKeyMap.remove(key) != null);
        }
        if (removed) {
            logger.debug("UnixDirectoryMonitor: removed {}", directory);
        }
        return removed;
    }

    @Override
    Path getPathFor(WatchKey key) {
        return watchKeyMap.get(key);
    }

    private final Map<WatchKey, Path> watchKeyMap = new HashMap<>();
}
/* vim:set et sw=4 ts=4: */
