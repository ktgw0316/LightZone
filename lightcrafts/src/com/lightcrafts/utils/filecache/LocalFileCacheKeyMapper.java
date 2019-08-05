/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.filecache;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * A <code>LocalFileCacheKeyMapper</code> is-a {@link FileCacheKeyMapper} that
 * creates a cache in specified directory.
 *
 * @author Paul J. Lucas [login@domain.com]
 */
public final class LocalFileCacheKeyMapper implements FileCacheKeyMapper {

    /**
     * Creates a <code>LocalFileCacheKeyMapper</code>.
     *
     * @param cacheDir The directory to put the mapped files in.
     * @return Returns a <code>LocalFileCacheKeyMapper</code> or
     * <code>null</code> if none could be created.
     */
    public static LocalFileCacheKeyMapper create( File cacheDir ) {
        if ( !cacheDir.exists() && !cacheDir.mkdirs() )
            return null;
        return new LocalFileCacheKeyMapper( cacheDir );
    }

    /**
     * {@inheritDoc}
     */
    public File getCacheDirectory() {
        return m_cacheDir;
    }

    /**
     * {@inheritDoc}
     */
    public File mapKeyToFile(@NotNull String key, boolean ensurePathExists ) {
        final int sep = key.lastIndexOf( File.separatorChar );
        if ( sep >= 0 )
            key = key.substring( sep + 1 );
        return new File( m_cacheDir, key + FileCacheFilter.EXTENSION );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>LocalFileCacheKeyMapper</code>.
     *
     * @param cacheDir The directory to put the mapped files in.
     */
    private LocalFileCacheKeyMapper( File cacheDir ) {
        m_cacheDir = cacheDir;
    }

    /**
     * The directory of the cache.
     */
    private final File m_cacheDir;
}
/* vim:set et sw=4 ts=4: */
