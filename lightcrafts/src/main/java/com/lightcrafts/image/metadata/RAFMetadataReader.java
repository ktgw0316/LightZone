/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;

import static com.lightcrafts.image.metadata.EXIFConstants.*;

/**
 * An <code>RAFMetadataReader</code> is-an {@link ImageMetadataReader} for
 * reading RAF (Fuji raw) metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class RAFMetadataReader extends ImageMetadataReader {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>RAFMetadataReader</code>.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public RAFMetadataReader( ImageInfo imageInfo ) {
        super( imageInfo, imageInfo.getByteBuffer() );
    }

    /**
     * Gets the offset from the beginning of the file to where the TIFF header
     * is.
     *
     * @return Returns said offset.
     */
    public int getTIFFOffset() {
        return m_tiffOffset;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Read the image header.
     *
     * @throws BadImageFileException if the internal format of the image file
     * header isn't as it's expected to be.
     */
    protected void readHeader() throws BadImageFileException, IOException {
        final String s = m_buf.getString( 0, 15, "ASCII" );
        if ( !s.equals( "FUJIFILMCCD-RAW" ) )
            throw new BadImageFileException( m_imageInfo.getFile() );

        //
        // The pointer to where the TIFF header starts is 84 bytes in.  The
        // value found there + 12 is the absolute position.
        //
        m_tiffOffset = m_buf.getInt( 84 )
            + 1 // JPEG_MARKER_BYTE
            + 1 // JPEG_SOI_MARKER
            + 1 // JPEG_MARKER_BYTE
            + 1 // JPEG_APP1_MARKER
            + 2 // sizeof( SHORT )
            + EXIF_HEADER_START_SIZE;

        //
        // Fuji metadata is just TIFF metadata, so, now that we've computed the
        // offset, just use a TIFFMetadataReader.
        //
        m_tiffMetadataReader = new TIFFMetadataReader( m_imageInfo );

        m_buf.initialOffset( m_tiffOffset );
        try {
            m_tiffMetadataReader.readHeader();
        }
        finally {
            m_buf.initialOffset( 0 );
        }
    }

    /**
     * Read the metadata from all directories.
     */
    protected void readAllDirectories() throws IOException {
        m_buf.initialOffset( m_tiffOffset );
        try {
            m_tiffMetadataReader.readAllDirectories();
        }
        finally {
            m_buf.initialOffset( 0 );
        }
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * RAF metadata is really TIFF metadata, so we use a
     * {@link TIFFMetadataReader} to read it.
     */
    private TIFFMetadataReader m_tiffMetadataReader;

    /**
     * The offset from the beginning of the file to where the TIFF header is.
     */
    private int m_tiffOffset;
}
/* vim:set et sw=4 ts=4: */
