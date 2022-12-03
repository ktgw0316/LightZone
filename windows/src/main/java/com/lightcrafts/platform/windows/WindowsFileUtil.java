/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import com.lightcrafts.image.types.ImageType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Windows file utilities.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class WindowsFileUtil {

    ////////// public /////////////////////////////////////////////////////////

    public static final int FOLDER_APPDATA      = 0x001A;
    public static final int FOLDER_DESKTOP      = 0x0010;
    public static final int FOLDER_MY_DOCUMENTS = 0x0005;
    public static final int FOLDER_MY_PICTURES  = 0x0027;

    /**
     * Gets the full path to the given folder.
     *
     * @param folderID The ID of the folder to get the path of.
     * @return Returns said path or <code>null</code> if it could not be
     * determined.
     */
    static native String getFolderPathOf( int folderID );

    /**
     * Hide the given file so that it doesn't show up in Windows Explorer.
     * Unlike *nix, files that start with a leading '.' are not automatically
     * hidden.  Files must be explicitly made hidden.
     *
     * @param fileName The name of the file to hide.
     */
    static native void hideFile( String fileName ) throws IOException;

    /**
     * Checks whether the given {@link File} is a GUID.
     *
     * @param file The {@link File} to check.
     * @return Returns <code>true</code> only if the {@link File} is a GUID.
     */
    static boolean isGUID(@NotNull File file ) {
        return file.getName().startsWith( "::{" );
    }

    /**
     * Checks whether the given file is a Windows shortcut file.
     *
     * @param path The full path of the file to check.
     * @return Returns <code>true</code> only if the file is a shortcut file.
     */
    @Contract(pure = true)
    static boolean isShortcut(@NotNull String path ) {
        return path.endsWith( ".lnk" );
    }

    /**
     * Resolve a Windows shortcut file.
     *
     * @param path The absolute path of the shortcut to resolve.
     * @return Returns the resolved path (or the original path if it didn't
     * refer to a shortcut) or <code>null</code> if there was an error.
     */
    public static String resolveShortcut( String path ) {
        return isShortcut( path ) ? resolveShortcutImpl( path ) : path;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Resolve a Windows shortcut file.
     *
     * @param path The full path of a file that must be a shortcut.
     * @return Returns the resolved path or <code>null</code> if there was an
     * error.
     */
    private static native String resolveShortcutImpl( String path );

    static {
        System.loadLibrary( "Windows" );
    }
}
/* vim:set et sw=4 ts=4: */
