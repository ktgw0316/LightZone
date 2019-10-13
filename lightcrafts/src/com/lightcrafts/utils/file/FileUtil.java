/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2013-     Masahiro Kitagawa */

package com.lightcrafts.utils.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.channels.FileChannel;

import com.lightcrafts.platform.Platform;
import org.jetbrains.annotations.NotNull;

/**
 * A <code>FileUtil</code> is a set of utility functions for files.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class FileUtil {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Checks whether the given directory contains at least one {@link File}
     * that would be accepted by the given {@link FileFilter}.
     *
     * @param dir The directory to check.
     * @param filter The {@link FileFilter} to use, or <code>null</code>.
     * @return Returns <code>true</code> only if the directory contains at
     * at least one {@link File} that has been accepted by the given
     * {@link FileFilter}.
     */
    public static boolean containsAtLeastOne(File dir, @NotNull FileFilter filter)
            throws IOException {
        final Platform platform = Platform.getPlatform();
        final Path dirPath = platform.isSpecialFile(dir).toPath();
        return Files.list(dirPath)
                .map(Path::toFile)
                .map(platform::isSpecialFile)
                .anyMatch(filter::accept);
    }

    /**
     * Copy the contents of a {@link File}.
     *
     * @param source The {@link File} to copy from.
     * @param target The {@link File} to copy to.
     * @throws IOException if anything goes wrong.
     */
    public static void copyFile( File source, File target ) throws IOException {
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }

    /**
     * Delete an array of {@link File}s.  For files that are actually
     * directories (and not {@link SmartFolder}s), they are also deleted only
     * if they are either empty or <code>recursive</code> is <code>true</code>.
     *
     * @param files The {@link File}s to delete.
     * @param filter The {@link FileFilter} to use; may be <code>null</code>.
     * @param recursive If <code>true</code>, also delete all enountered
     * subdirectories and their contents.
     * @return Returns <code>true</code> only if all the files were deleted
     * successfully and there were no I/O errors.
     */
    public static boolean delete( File[] files, FileFilter filter,
                                  boolean recursive ) {
        boolean result = true;
        if ( files != null )
            for ( File file : files ) {
                if ( file.isDirectory() && !(file instanceof SmartFolder) &&
                     recursive ) {
                    final File[] subFiles = listFiles( file, filter, true );
                    if ( subFiles == null || subFiles.length > 0 &&
                         !delete( subFiles, filter, true ) )
                        result = false;
                }
                if ( !file.delete() )
                    result = false;
            }
        return result;
    }

    /**
     * Decodes a filename that was encoded by {@link #encodeFilename(String)}.
     *
     * @param name The filename to be decoded.
     * @return Returns the decoded filename.
     * @see #encodeFilename(String)
     */
    public static String decodeFilename( String name ) {
        int from = 0;
        while ( true ) {
            final int i = name.indexOf( '%', from );
            if ( i >= 0 && i < name.length() - 2 ) {
                try {
                    final String hex = name.substring( i + 1, i + 3 );
                    final int ascii = Integer.parseInt( hex, 16 );
                    final String to = String.valueOf( (char)ascii );
                    name = name.substring( 0, i ) + to + name.substring( i+3 );
                }
                catch ( NumberFormatException e ) {
                    // ignore
                }
                from = i + 1;
            } else
                return name;
        }
    }

    /**
     * Encodes a filename such that each illeal character is replaced by a 3
     * character sequence composed of a '%' followed by the ASCII code for the
     * illegal character expressed in hexadecimal.  Additionally, the '%'
     * character, although not illegal, is also encoded.  This encoding scheme
     * is similar to URL encoding.
     * <p>
     * The set of illegal characters is the union of all the illegal characters
     * for Linux, Mac OS X, and Windows.
     *
     * @param name The filename to encode.
     * @return Returns the encoded filename.
     * @see #decodeFilename(String)
     */
    public static String encodeFilename( String name ) {
        //
        // Unfortunately, Java's String class doesn't have any equivalent of
        // the strpbrk(3) C standard library function.  :-(
        //
        for ( int i = 0; i < ILLEGAL_FILENAME_CHARS.length(); ++i ) {
            final char c = ILLEGAL_FILENAME_CHARS.charAt( i );
            if ( name.indexOf( c ) >= 0 ) {
                final String from = String.valueOf( c );
                //noinspection UnnecessaryBoxing
                final String to = String.format( "%%%02X", new Integer( c ) );
                name = name.replace( from, to );
            }
        }
        return name;
    }

    /**
     * Gets the last access time of the given file.
     *
     * @param file The {@link File} to get the last access time for.
     * @return Returns the number of milliseconds since epoch of the last
     * access time.
     * @throws IOException if the file doesn't exist or the access time could
     * not be obtained.
     */
    public static long getLastAccessTimeOf( File file ) throws IOException {
        return Files.readAttributes(file.toPath().toAbsolutePath(), BasicFileAttributes.class)
                .lastAccessTime().toMillis();
    }

    /**
     * Gets the extension (the part of the file's name after the
     * <code>'.'</code>) of the given file.
     *
     * @param file The {@link File} to get the extension of.
     * @return Returns the extension (without the <code>'.'</code>) or
     * <code>null</code> if the file has no extension.
     * @see #replaceExtensionOf(File,String)
     * @see #replaceExtensionOf(String,String)
     * @see #trimExtensionOf(File)
     * @see #trimExtensionOf(String)
     */
    public static String getExtensionOf( File file ) {
        final String fileName = file.getName();
        final int dot = fileName.lastIndexOf( '.' );
        if ( dot <= 0 || dot == fileName.length() - 1 )
            return null;
        return fileName.substring( dot + 1 );
    }

    /**
     * Gets a {@link File} in the file's directory that doesn't collide with
     * any existing files by appending or inserting a numbered suffix.
     * <p>
     * For example, if the file <code>/tmp/foo.jpg</code> exists, returns
     * <code>/tmp/foo-1.jpg</code>; if <code>/tmp/foo-1.jpg</code> exists,
     * returns <code>/tmp/foo-2.jpg</code>; and so on.
     *
     * @param file The {@link File} to start with.
     * @return Returns said file.  Note that if the given file doesn't exist,
     * returns that file.
     */
    public static File getNoncollidingFileFor( File file ) {
        if ( !file.exists() )
            return file;
        final String name = file.getName();
        final Matcher m = NUMBERED_FILE_PATTERN.matcher( name );

        final String basename;
        final int next;
        if (m.matches()) {
            next = Integer.parseInt(m.group(1)) + 1;
            basename = name.substring(0, m.start(1));
        } else {
            next = 1;
            basename = trimExtensionOf(name);
        }
        return getNoncollidingFileFor(file.getParentFile(), basename, next, getExtensionOf(file));
    }

    /**
     * Gets a {@link File} in the file's directory that doesn't collide with
     * any existing files by appending a numbered suffix and a "temp" extension
     * <p>
     * For example, if the file <code>/tmp/foo.jpg</code> exists, returns
     * <code>/tmp/foo.jpg-1.temp</code>; if <code>/tmp/foo.jpg-1.temp</code> exists,
     * returns <code>/tmp/foo.jpg-2.temp</code>; and so on.
     *
     * @param file The {@link File} to start with.
     * @return Returns said file.  Note that if the given file doesn't exist,
     * returns that file.
     */
    private static File getNoncollidingTempFileFor(File file) {
        return !file.exists()
                ? file
                : getNoncollidingFileFor(file.getParentFile(), file.getName(), 1, "temp");
    }

    private static File getNoncollidingFileFor(File parent, String basename,
                                               int index, String extension) {
        final File file = new File(parent, basename + "-" + index + "." + extension);
        return !file.exists()
                ? file
                : getNoncollidingFileFor(parent, basename, ++index, extension);
    }

    /**
     * Gets the platform's temporary directory.
     *
     * @return Returns said directory.
     * @throws IOException if anything goes wrong.
     */
    public static File getTempDir() throws IOException {
        File temp = null;
        try {
            temp = File.createTempFile( "LZTemp", null );
            return temp.getParentFile();
        }
        finally {
            if ( temp != null && !temp.delete()) {
                temp.deleteOnExit();
            }
        }
    }

    /**
     * Inserts (if necessary) the given suffix into the filename just before
     * the extension.
     *
     * @param file The {@link File} whose name to insert the suffix into.
     * @param suffix The suffix to insert.
     * @return Returns the new filename.
     * @see #insertSuffix(String,String)
     */
    public static String insertSuffix( File file, String suffix ) {
        return insertSuffix( file.getAbsolutePath(), suffix );
    }

    /**
     * Inserts (if necessary) the given suffix into the filename just before
     * the extension.
     *
     * @param fileName The filename whose name to insert the suffix into.
     * @param suffix The suffix to insert.
     * @return Returns the new filename if the original file did not contain
     * the suffix, or the original filename if it already did.
     * @see #insertSuffix(File,String)
     */
    public static String insertSuffix( String fileName, String suffix ) {
        final int dot = fileName.lastIndexOf( '.' );
        if ( dot <= 0 || dot == fileName.length() - 1 )
            return null;
        final int suffixBegin = dot - suffix.length();
        if ( suffixBegin >= 0 ) {
            final String beforeDot = fileName.substring( suffixBegin, dot );
            if ( beforeDot.equals( suffix ) )
                return fileName;
        }
        return  fileName.substring( 0, dot ) + suffix +
                fileName.substring( dot );
    }

    /**
     * Checks whether the given {@link File} is a non-hidden, traversable
     * folder (including {@link SmartFolder}s).  This should be used instead of
     * {@link File#isDirectory()} for folders that are presented to the user.
     *
     * @param file The {@link File} to check.
     * @return Returns the rusult of {@link Platform#isSpecialFile(File)} only
     * if the given {@link File} is a non-hidden, traversable folder; otherwise
     * returns <code>null</code>.
     */
    public static File isFolder( File file ) {
        if ( file.isHidden() )
            return null;
        final Platform platform = Platform.getPlatform();
        file = platform.isSpecialFile( file );
        if ( file.isFile() )
            return null;
        if ( file instanceof SmartFolder ) {
            //
            // We must test for SmartFolders explicitly because they're not
            // considered "traversable" by Java.
            //
            return null;
        }
        return platform.getFileSystemView().isTraversable( file ) ? file : null;
    }

    /**
     * Gets an array of file in the given directory.
     * <p>
     * This method needs to be used rather than
     * {@link File#listFiles(FileFilter)} because, for some reason, the latter
     * method doesn't include things like "My Computer" under Windows.
     *
     * @param dir The directory to get the list of child files of.
     * @return Returns said array or <code>null</code> if there was an I/O
     * error.
     * @see #listFiles(File,FileFilter,boolean)
     */
    public static File[] listFiles( File dir ) {
        return listFiles( dir, null, false );
    }

    /**
     * Gets an array of file in the given directory.
     * <p>
     * This method needs to be used rather than
     * {@link File#listFiles(FileFilter)} because, for some reason, the latter
     * method doesn't include things like "My Computer" under Windows.
     *
     * @param dir The directory to get the list of child files of.
     * @param filter The {@link FileFilter} to use, or <code>null</code>.
     * @param includeDirs If <code>true</code>, include directories regardless
     * of the filter.  If the filter is <code>null</code>, this parameter is
     * ignored.
     * @return Returns said array or <code>null</code> if there was an I/O
     * error.
     * @see #listFiles(File)
     */
    public static File[] listFiles( File dir, FileFilter filter,
                                    boolean includeDirs ) {
        dir = Platform.getPlatform().isSpecialFile( dir );
        final File[] files = dir.listFiles(filter);
        if (! includeDirs || files == null)
            return files;

        final File[] dirs = dir.listFiles(dirFilter);
        if (dirs == null)
            return null;
        File[] dirsAndFiles = new File[files.length + dirs.length];
        System.arraycopy(files, 0, dirsAndFiles, 0, files.length);
        System.arraycopy(dirs,  0, dirsAndFiles, files.length, dirs.length);
        return dirsAndFiles;
    }

    /**
     * Reads an entire file into a {@link String}.
     *
     * @param file The {@link File} to read.
     * @return Returns the entire contents of the file as a {@link String}.
     * @see #readEntireStream(InputStream)
     */
    public static String readEntireFile( File file ) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return readEntireStream(is);
        }
    }

    /**
     * Reads the entire contents of an {@link InputStream} into a
     * {@link String} using the UTF-8 encoding.
     *
     * @param in The {@link InputStream} to read.  It is not closed.
     * @return Returns the entire contents of the file.
     * @see #readEntireFile(File)
     */
    public static String readEntireStream( InputStream in ) throws IOException {
        final byte[] buf = new byte[ 1024 ];
        int bytesRead;
        final StringBuilder sb = new StringBuilder();
        while ( (bytesRead = in.read( buf )) > 0 )
            sb.append( new String( buf, 0, bytesRead, "UTF-8" ) );
        return sb.toString();
    }

    /**
     * Renames a {@link File}.  Unlike {@link File#renameTo(File)}, this method
     * throws {@link IOException} if the rename fails.
     *
     * @param from The {@link File} to rename.
     * @param to The {@link File} to rename to.
     * @throws IOException if the rename fails.
     */
    public static void renameFile( File from, File to ) throws IOException {
        File backup = null;
        try {
            if (to.exists()) {
                //
                // Windows doesn't allow renaming a file to an existing file, so we
                // have to move it first.
                //
                backup = getNoncollidingTempFileFor(to);
                if (!to.renameTo(backup)) {
                    throw new IOException("Failed to backup " + to.getName());
                }
            }
            if (!from.renameTo(to)) {
                String msg = "Failed to rename " + from.getName() + " to " + to.getName();
                if (backup != null && !backup.renameTo(to)) {
                    msg += ", and failed to recover the " + from.getName()
                            + " from backup " + backup.getName();
                }
                throw new IOException(msg);
            }
            if (backup != null && !backup.delete()) {
                backup.deleteOnExit();
            }
        }
        catch (SecurityException e) {
            throw  new IOException(e);
        }
    }

    /**
     * Replaces the extension (the part of the file's name after the
     * <code>'.'</code>) with a new extension.
     *
     * @param file The {@link File} to replace the extension of.
     * @param newExtension The new extension (without the <code>'.'</code>).
     * @return Returns a new filename with the extension replaced or
     * <code>null</code> if the file has no extension.
     * @see #getExtensionOf(File)
     * @see #replaceExtensionOf(String,String)
     * @see #trimExtensionOf(File)
     */
    public static String replaceExtensionOf( File file, String newExtension ) {
        return replaceExtensionOf( file.getAbsolutePath(), newExtension );
    }

    /**
     * Replaces the extension (the part of the file's name after the
     * <code>'.'</code>) with a new extension.
     *
     * @param fileName The name of the file to replace the extension of.
     * @param newExtension The new extension (without the <code>'.'</code>).
     * @return Returns a new filename with the extension replaced or
     * <code>null</code> if the file has no extension.
     * @see #getExtensionOf(File)
     * @see #replaceExtensionOf(File,String)
     * @see #trimExtensionOf(File)
     */
    public static String replaceExtensionOf( String fileName,
                                             String newExtension ) {
        final int dot = fileName.lastIndexOf( '.' );
        if ( dot <= 0 || dot == fileName.length() - 1 )
            return null;
        return fileName.substring( 0, dot + 1 ) + newExtension;
    }

    /**
     * Resolves a {@link File} if it's an alias.
     *
     * @param file The {@link File} that may be an alias to resolve.
     * @return Returns a resolved {@link File}, or the original {@link File} if
     * it didn't refer to an alias or if there was an error.
     */
    public static File resolveAliasFile( File file ) {
        if ( file != null ) {
            final String resolvedPath =
                Platform.getPlatform().resolveAliasFile( file );
            if ( resolvedPath != null &&
                 !resolvedPath.equals( file.getAbsolutePath() ) )
                file = new File( resolvedPath );
        }
        return file;
    }

    /**
     * Touch (update the modification time) the given file.
     *
     * @param file The {@link File} to touch.
     */
    public static boolean touch( File file ) {
        return file.setLastModified( System.currentTimeMillis() );
    }

    /**
     * Trim the extension (the part of the file's name after the
     * <code>'.'</code>).
     *
     * @param file The {@link File} to trim the extension from.
     * @return Returns a new filename with the extension removed or the
     * original filename if there was no extension.
     * @see #getExtensionOf(File)
     * @see #replaceExtensionOf(File,String)
     * @see #replaceExtensionOf(String,String)
     * @see #trimExtensionOf(String)
     */
    public static String trimExtensionOf( File file ) {
        return trimExtensionOf( file.getAbsolutePath() );
    }

    /**
     * Trim the extension (the part of the file's name after the
     * <code>'.'</code>).
     *
     * @param fileName The filename to trim the extension from.
     * @return Returns a new filename with the extension removed or the
     * original filename if there was no extension.
     * @see #getExtensionOf(File)
     * @see #replaceExtensionOf(File,String)
     * @see #replaceExtensionOf(String,String)
     * @see #trimExtensionOf(File)
     */
    public static String trimExtensionOf( String fileName ) {
        final int dot = fileName.lastIndexOf( '.' );
        return dot >= 1 ? fileName.substring( 0, dot ) : fileName;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The set of characters that are illegal in filenames comprising Linux,
     * Mac OS X, and Windows.  Additionally, the '%' character, although not
     * illegal, is included first so it will be encoded first by
     * {@link #encodeFilename(String)}.
     */
    private static final String ILLEGAL_FILENAME_CHARS = "%\"*/:<>?\\|";

    private static final Pattern NUMBERED_FILE_PATTERN =
        Pattern.compile( "^.*-(\\d+)\\.[a-z]{3,4}$" );

    private static final FileFilter dirFilter = new FileFilter() {
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };
}
/* vim:set et sw=4 ts=4: */
