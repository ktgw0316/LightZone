/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import java.io.IOException;
import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.lightcrafts.image.types.ImageType;

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
    public static native String getFolderPathOf( int folderID );

    /**
     * Hide the given file so that it doesn't show up in Windows Explorer.
     * Unlike *nix, files that start with a leading '.' are not automatically
     * hidden.  Files must be explicitly made hidden.
     *
     * @param fileName The name of the file to hide.
     */
    public static native void hideFile( String fileName ) throws IOException;

    /**
     * Checks whether the given {@link File} is a GUID.
     *
     * @param file The {@link File} to check.
     * @return Returns <code>true</code> only if the {@link File} is a GUID.
     */
    public static boolean isGUID( File file ) {
        return file.getName().startsWith( "::{" );
    }

    /**
     * Checks whether the given file is a Windows shortcut file.
     *
     * @param path The full path of the file to check.
     * @return Returns <code>true</code> only if the file is a shortcut file.
     */
    public static boolean isShortcut( String path ) {
        return path.endsWith( ".lnk" );
    }

    /**
     * Moves a set of files to the Recycle Bin.
     *
     * @param pathNames An array of the file(s) to move.  The file name(s) must
     * all be full paths.
     * @return Returns <code>true</code> only if the move succeeded.
     */
    public static native boolean moveToRecycleBin( String[] pathNames );

    /**
     * Display the native Windows open-file dialog to have the user select an
     * image file to open.
     *
     * @param initialDir The initial directory to open the dialog at.
     * @return Returns the selected file or <code>null</code> if the user
     * cancelled.
     * @see #openFile(String,String[],String[][])
     */
    public static String openFile( String initialDir ) throws IOException {
        final Collection<ImageType> types = ImageType.getAllTypes();

        Stream<String> displayStringsHead = Stream.of("All files");
        Stream<String> displayStringsRest =
                types.stream().map(ImageType::getName);
        final String[] displayStrings =
                Stream.concat(displayStringsHead, displayStringsRest)
                .toArray(String[]::new);

        Stream<String[]> extensionsStreamHead = Stream.of(new String[][]{{"*"}});
        Stream<String[]> extensionsStreamRest =
                types.stream().map(ImageType::getExtensions);
        final String[][] extensions =
                Stream.concat(extensionsStreamHead, extensionsStreamRest)
                .toArray(String[][]::new);

        return openFile( initialDir, displayStrings, extensions );
    }

    /**
     * Display the native Windows open-file dialog to have the user select a
     * file to open.
     *
     * @param initialDir The initial directory to open the dialog at.
     * @param displayStrings An array of the names of file types to be
     * displayed in a pop-up menu.
     * @param extensions For each display string, an array of filename
     * extension(s) for that string.
     * @return Returns the selected file or <code>null</code> if the user
     * cancelled.
     * @see #openFile(String)
     */
    public static String openFile( String initialDir,
                                   String[] displayStrings,
                                   String[][] extensions ) throws IOException {
        if ( displayStrings.length != extensions.length )
            throw new IllegalArgumentException();
        final String[] patterns = Stream.of(extensions)
                .map(extensionArray -> Stream.of(extensionArray)
                        .map(extension -> "*." + extension)
                        .collect(Collectors.joining(";"))
                )
                .toArray(String[]::new);
        return openFile( initialDir, displayStrings, patterns );
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
     * Display the native Windows open-file dialog to have the user select a
     * file to open.
     *
     * @param initialDir The initial directory to open the dialog at.
     * @param displayStrings An array of the names of file types to be
     * displayed in a pop-up menu.
     * @param patterns For each display string, a pattern of filename
     * extension(s) for that string.  Multiple extensions are seperated by
     * semicolons.
     * @return Returns the selected file or <code>null</code> if the user
     * cancelled.
     */
    private static native String openFile( String initialDir,
                                           String[] displayStrings,
                                           String[] patterns )
        throws IOException;

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
