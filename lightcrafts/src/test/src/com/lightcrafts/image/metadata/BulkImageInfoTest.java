/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2021-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * A <code>BulkImageInfoTest</code> is for bulk testing of
 * reading metadata and sub-images from lots of images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class BulkImageInfoTest {

    /**
     * Builds a {@link MethodSource} comprising one {@link ImageInfo} per image file
     * starting at the present working directory and recursively traversing it.
     *
     * @return Returns {@link List} of said {@link ImageInfo}.
     */
    private static List<ImageInfo> source() {
        final var file = new File(System.getProperty("testDir"));
        List<ImageInfo> params = new ArrayList<>();
        try {
            params = listImageInfo(file, params);
        }
        catch ( Throwable t ) {
            t.printStackTrace();
            System.exit( -1 );
        }
        return params;
    }

    /**
     * Run a test by reading the metadata for an image file.  If no
     * exceptions are thrown, assume the test passed.
     */
    @ParameterizedTest
    @MethodSource("source")
    public static void getMetadata(ImageInfo imageInfo) {
        assertThatCode(imageInfo::getMetadata).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("source")
    public static void getPreviewImage(ImageInfo imageInfo) {
        assertThatCode(imageInfo::getPreviewImage).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("source")
    public static void getThumbnailImage(ImageInfo imageInfo) {
        assertThatCode(imageInfo::getThumbnailImage).doesNotThrowAnyException();
    }

    /**
     * Make a {@link ImageInfo} for the given file.  If the file does not exist,
     * is not readable, is hidden, or is a file we don't know how to deal with,
     * it's skipped.  If the file is actually a directory, recursively make a
     * {@link ImageInfo} for all contained files (or subdirectories).
     *
     * @param file The file.
     */
    private static List<ImageInfo> listImageInfo(File file, List<ImageInfo> params)
            throws BadImageFileException, IOException
    {
        if ( !file.exists() ) {
            m_logger.log( Level.WARNING, "",
                new FileNotFoundException( file.getAbsolutePath() )
            );
            return params;
        }
        if ( !file.canRead() ) {
            m_logger.log( Level.WARNING, "",
                new IOException( file.getAbsolutePath() )
            );
            return params;
        }
        if ( file.isDirectory() ) {
            for ( File f : file.listFiles() )
                params = listImageInfo(f, params);
        }
        if ( file.isHidden() || !file.isFile() )
            return params;

        final var imageInfo = ImageInfo.getInstanceFor( file );
        try {
            imageInfo.getImageType();
            params.add(imageInfo);
        }
        catch ( UnknownImageTypeException e ) {
            // ignore
        }
        return params;
    }

    /**
     * The {@link Logger} to use.
     */
    private static final Logger m_logger =
        Logger.getLogger( "com.lightcrafts.image.metadata" );

    static {
        System.loadLibrary( "DCRaw" );
    }
}
/* vim:set et sw=4 ts=4: */
