/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.filecache;

import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.prefs.Preferences;

/**
 * A single source for FileCache, allowing policy to be applied for cache
 * instantiation.  The current policy ensures that the cache is:
 * <ol>
 *   <li>a singleton instance;</li>
 *   <li>limited to 5GB;</li>
 *   <li>in a specific location; and</li>
 *   <li>respecting all applicable user preferences.</li>
 * </ol>
 */
public class FileCacheFactory {

    /**
     * The global limit on cache size, in bytes.
     */
    public final static long MaxSize = 5l * 1024 * 1024 * 1024;

    private final static Preferences Prefs =
        Preferences.userRoot().node("/com/lightcrafts/utils/filecache");

    private final static String ScopeKey = "CacheScope";

    /**
     * Get a FileCache, either the global FileCache instance if the global
     * option is preferred, or a local one appropriate to the given File
     * if the local option is preferred.  This will return null if there is
     * a problem creating the cache.
     */
    public static synchronized FileCache get(File file) {
        boolean local = Prefs.getBoolean(ScopeKey, false);
        if (! local) {
            return getGlobalCache();
        }
        else {
            return getLocalCache(file);
        }
    }

    /**
     * Get the one global cache, bypassing the user preference to use a local
     * cache instead.
     */
    public static FileCache getGlobalCache() {
        if (GlobalInstance == null) {
            try {
                FileCacheKeyMapper mapper = PerUserFileCacheKeyMapper.create();
                GlobalInstance = new FileCache(MaxSize, mapper);
            }
            catch (IOException e) {
                System.err.print("Couldn't create FileCache: ");
                System.err.println(e.getMessage());
            }
        }
        return GlobalInstance;
    }

    /**
     * Get a directory local cache, bypassing the user preference to use the
     * global cache instead.
     */
    public static FileCache getLocalCache(File file) {
        final String cacheDirName = "LightZone Previews";
        if (file.isFile()) {
            file = file.getParentFile();
        }
        else if (file.getName().equals(cacheDirName)) {
            return null;
        }
        file = new File(file, cacheDirName);
        FileCache cache = LocalInstances.get(file);
        if (cache != null) {
            return cache;
        }
        try {
            FileCacheKeyMapper mapper = LocalFileCacheKeyMapper.create(file);
            if (mapper != null) {
                cache = new FileCache(MaxSize, mapper);
                LocalInstances.put(file, cache);
                return cache;
            }
            else {
                System.err.print("Couldn't create LocalFileCacheKeyMapper");
                return null;
            }
        }
        catch (IOException e) {
            System.err.print("Couldn't create FileCache: ");
            System.err.println(e.getMessage());
            return null;
        }
    }

    private static FileCache GlobalInstance;

    private static HashMap<File, FileCache> LocalInstances =
        new HashMap<File, FileCache>();
}
