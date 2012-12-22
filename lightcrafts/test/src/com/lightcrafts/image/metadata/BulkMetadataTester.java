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
 * A <code>BulkMetadataTester</code> is-a {@link TestCase} for bulk testing of
 * reading metadata from lots of images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class BulkMetadataTester extends TestCase {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>BulkMetadataTester</code>.
     *
     * @param name The name of this test.
     */
    public BulkMetadataTester( String name ) {
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
            makeTestFor( new File( System.getProperty( "testDir" ) ) );
        }
        catch ( Throwable t ) {
            t.printStackTrace();
            System.exit( -1 );
        }
        return m_suite;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>ReadMetadataTestCase</code> is-a {@link TestCase} for reading
     * the metadata from an image file.
     */
    private static final class ReadMetadataTestCase extends TestCase {

        /**
         * Construct a <code>ReadMetadataTestCase</code>.
         *
         * @param imageInfo The image file to read the metadata from.
         */
        ReadMetadataTestCase( ImageInfo imageInfo ) {
            super( imageInfo.getFile().getAbsolutePath() );
            m_imageInfo = imageInfo;
        }

        /**
         * Run a test by reading the metadata for an image file.  If no
         * exceptions are thrown, assume the test passed.
         */
        public void runTest() throws Exception {
            m_imageInfo.getMetadata();
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
            for ( File f : file.listFiles() )
                makeTestFor( f );
            return;
        }
        if ( file.isHidden() || !file.isFile() )
            return;

        final ImageInfo imageInfo = ImageInfo.getInstanceFor( file );
        try {
            imageInfo.getImageType();
            m_suite.addTest( new ReadMetadataTestCase( imageInfo ) );
        }
        catch ( UnknownImageTypeException e ) {
            // ignore
        }
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
