/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.filecache;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * A <code>FileCacheKeyMapper</code> is used to map a cache key to a file on
 * disk.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface FileCacheKeyMapper {

    /**
     * Gets the cache directory.
     *
     * @return Returns said directory.
     */
    File getCacheDirectory();

    /**
     * Maps a key to a {@link File}.
     *
     * @param key The key to map.
     * @param ensurePathExists If <code>true</code>, ensure the path to the
     * returned file exists.
     * @return Returns said {@link File}.
     */
    File mapKeyToFile(@NotNull String key, boolean ensurePathExists );

}
/* vim:set et sw=4 ts=4: */
