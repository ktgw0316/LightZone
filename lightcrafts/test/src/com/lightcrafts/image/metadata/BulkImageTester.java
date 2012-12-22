/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.*;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;

/**
 * An <code>BulkImageTester</code> is-a {@link TestCase} for bulk testing of
 * reading of lots of images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class BulkImageTester extends TestCase {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>BulkImageTester</code>.
     *
     * @param name The name of this test.
     */
    public BulkImageTester( String name ) {
        super( name );
    }

    /**
     * Builds a {@link TestSuite} comprising one test per image file starting
     * at the present working directory and recursively traversing it.
     *
     * @return Returns said suite.
     */
    public static Test suite() {
        try {
            makeTestFor( new File( "." ) );
        }
        catch ( Throwable t ) {
            t.printStackTrace();
            System.exit( -1 );
        }
        return m_suite;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>ReadImageTestCase</code> is-a {@link TestCase} for reading the
     * images from an image file.
     */
    private static final class ReadImageTestCase extends TestCase {

        /**
         * Construct a <code>ReadImageTestCase</code>.
         *
         * @param imageInfo The image file to read the metadata from.
         */
        ReadImageTestCase( ImageInfo imageInfo ) {
            super( imageInfo.getFile().getAbsolutePath() );
            m_imageInfo = imageInfo;
        }

        /**
         * Run a test by reading the metadata for an image file.  If no
         * exceptions are thrown, assume the test passed.
         */
        public void runTest() throws Exception {
            m_imageInfo.getThumbnailImage();
            m_imageInfo.getPreviewImage();
            //m_imageInfo.getImage();
            assertTrue( true );
        }

        /**
         * The image to read the metadata for.
         */
        private final ImageInfo m_imageInfo;
    }

    /**
     * Make a {@link TestCase} for the given file.  If the file does not exist,
     * is not readable, is hidden, or is a file we don't know how to deal with,
     * it's skipped.  If the file is actually a directory, recursively make a
     * {@link TestCase} for all contained files (or subdirectories).
     *
     * @param file The file.
     */
    private static void makeTestFor( File file )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        if ( !file.exists() ) {
            m_logger.log( Level.WARNING, "",
                new FileNotFoundException( file.getAbsolutePath() )
            );
            return;
        }
        if ( !file.canRead() ) {
            m_logger.log( Level.WARNING, "",
                new IOException( file.getAbsolutePath() )
            );
            return;
        }
        if ( file.isDirectory() ) {
            final File[] files = file.listFiles();
            for ( int i = 0; i < files.length; ++i )
                makeTestFor( files[i] );
            return;
        }
        if ( file.isHidden() || !file.isFile() )
            return;

        final ImageInfo imageInfo = ImageInfo.getInstanceFor( file );
        if ( imageInfo.getImageType() != null )
            m_suite.addTest( new ReadImageTestCase( imageInfo ) );
    }

    /**
     * The {@link Logger} to use.
     */
    private static final Logger m_logger =
        Logger.getLogger( "com.lightcrafts.image.metadata" );

    /**
     * The {@link TestSuite} we build comprising one test per image file.
     */
    private static final TestSuite m_suite = new TestSuite();

    static {
        System.loadLibrary( "DCRaw" );
    }
}
/* vim:set et sw=4 ts=4: */
