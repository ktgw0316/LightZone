/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.types.MRWImageType;
import com.lightcrafts.utils.bytebuffer.ArrayByteBuffer;

/**
 * An <code>MRWMetadataReader</code> is-an {@link ImageMetadataReader} for
 * reading MRW (Minolta raw) metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MRWMetadataReader extends ImageMetadataReader {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>MRWMetadataReader</code>.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public MRWMetadataReader( ImageInfo imageInfo ) {
        super( imageInfo, imageInfo.getByteBuffer() );
    }

    /**
     * Gets the TIFF ("TTW") block from the MRW image.
     *
     * @return Returns said block.
     */
    public ArrayByteBuffer getTIFFBlock() {
        return m_tiffBlock;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Read the image header.
     *
     * @throws BadImageFileException if the internal format of the image file
     * header isn't as it's expected to be.
     */
    protected void readHeader() throws BadImageFileException, IOException {
        m_buf.position( 0 );
        //
        // Each block is of the form:
        //
        //      0nnn ssss data
        //
        // where "nnn" is the 3-letter block name, "ssss" is the 4-byte block
        // size (always big-endian), and "data" is the block data.
        //
        // The first block ("MRM") is a special case in that its size is the
        // total size of all blocks; hence, we ignore its size and move forward
        // to the next block that is immediately after.
        //
        if ( m_buf.get() != 0 )
            throw new BadImageFileException( m_imageInfo.getFile() );
        if ( !m_buf.getEquals( "MRM", "ASCII" ) )
            throw new BadImageFileException( m_imageInfo.getFile() );
        int blockSize = m_buf.getInt();

        while ( true ) {
            if ( m_buf.get() != 0 )
                throw new BadImageFileException( m_imageInfo.getFile() );
            final String blockName = m_buf.getString( 3, "ASCII" );
            blockSize = m_buf.getInt();

            if ( blockName.equals( "PRD" ) ) {
                // TODO
                m_buf.skipBytes( blockSize );
                continue;
            }

            if ( blockName.equals( "TTW" ) ) {
                //
                // The TTW block contains an embedded TIFF file (header and
                // metadata) but without the actual image data.  All the
                // offsets are relative to its start, so this makes it easy to
                // read it: just extract the block and treat it like a TIFF
                // file.
                //
                m_tiffBlock =
                    new ArrayByteBuffer( m_buf.getBytes( blockSize ) );
                m_tiffMetadataReader =
                    new TIFFMetadataReader( m_imageInfo, m_tiffBlock );
                m_tiffMetadataReader.readHeader();
                break;
            }

            m_buf.skipBytes( blockSize );
        }
    }

    /**
     * Read the metadata from all directories.
     */
    protected void readAllDirectories() throws IOException {
        m_tiffMetadataReader.readAllDirectories();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The entire contents of the TIFF ("TTW") block from the MRW image.  This
     * is reused later by
     * {@link MRWImageType#getPreviewImage(ImageInfo,int,int)}.
     */
    private ArrayByteBuffer m_tiffBlock;

    /**
     * MRW metadata is really TIFF metadata, so we use a
     * {@link TIFFMetadataReader} to read it.
     */
    private TIFFMetadataReader m_tiffMetadataReader;
}
/* vim:set et sw=4 ts=4: */
