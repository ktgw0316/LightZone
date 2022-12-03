/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.filecache;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.Version;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * A <code>PerUserFileCacheKeyMapper</code> is-a {@link FileCacheKeyMapper}
 * that creates a cache in a platform-specific directory within the user's
 * home directory.
 *
 * @author Paul J. Lucas [login@domain.com]
 */
public final class PerUserFileCacheKeyMapper implements FileCacheKeyMapper {

    /**
     * Create a <code>PerUserFileCacheKeyMapper</code>.
     */
    public static PerUserFileCacheKeyMapper create() throws IOException {
        if ( !m_createSucceeded )
            throw new IOException( "User file cache create failed." );
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public File getCacheDirectory() {
        return m_cacheDir;
    }

    /**
     * Map a key to a {@link File} by computing the MD5 hash of the directory
     * part of the key and using that as the subdirectory within the cache to
     * put the file.
     *
     * @param key The key to map.
     * @param ensurePathExists If <code>true</code>, ensure the path to the
     * returned file exists.
     * @return Returns said {@link File}.
     */
    public File mapKeyToFile(@NotNull String key, boolean ensurePathExists ) {
        final File keyAsFile = new File( key );
        String dirOfFile = keyAsFile.getParent();
        if ( dirOfFile == null )
            dirOfFile = System.getProperty( "user.dir" );
        final String subdirName = Integer.toHexString( dirOfFile.hashCode() );
        final File subdir = new File( m_cacheDir, subdirName );
        if ( ensurePathExists )
            subdir.mkdir();

        return new File(
            subdir, keyAsFile.getName() + FileCacheFilter.EXTENSION
        );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>PerUserFileCacheKeyMapper</code>.
     */
    private PerUserFileCacheKeyMapper() {
        // do nothing
    }

    /**
     * The singleton instance.
     */
    private static final PerUserFileCacheKeyMapper INSTANCE =
        new PerUserFileCacheKeyMapper();

    /**
     * The directory of the cache.
     */
    private static final File m_cacheDir;

    /**
     * A flag to indicate whether creating the cache was successful.
     */
    private static boolean m_createSucceeded;

    static {
        if ( Platform.isMac() ) {
            m_cacheDir = new File( System.getProperty( "user.home" ),
                "Library/Caches/" + Version.getApplicationName() );
        }
        else if ( Platform.isWindows() ) {
            m_cacheDir = new File( System.getenv( "APPDATA" ),
                Version.getApplicationName() + "\\Caches" );
        }
        else {
            m_cacheDir = new File( System.getProperty( "user.home" ),
                ".lzncache" );
        }
        m_createSucceeded = m_cacheDir.exists() || m_cacheDir.mkdirs();
    }
}
/* vim:set et sw=4 ts=4: */
