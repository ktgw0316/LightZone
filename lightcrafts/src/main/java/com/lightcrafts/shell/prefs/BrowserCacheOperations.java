package com.lightcrafts.shell.prefs;

import com.lightcrafts.utils.filecache.FileCache;
import com.lightcrafts.utils.filecache.FileCacheFactory;

import java.io.IOException;
import java.util.prefs.Preferences;

public final class BrowserCacheOperations {

    private static final String PACKAGE = "/com/lightcrafts/utils/filecache";
    private static final String KEY = "CacheScope";
    private static final Preferences preferences =
        Preferences.userRoot().node(PACKAGE);

    private BrowserCacheOperations() {
    }

    public static boolean isLocalScope() {
        return preferences.getBoolean(KEY, false);
    }

    public static void setLocalScope(boolean local) {
        preferences.putBoolean(KEY, local);
    }

    public static void clearGlobalCache() throws IOException {
        FileCache cache = FileCacheFactory.getGlobalCache();
        if (cache != null) {
            cache.clear();
        }
        else {
            throw new IOException("Global cache does not exist");
        }
    }
}
