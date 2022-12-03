/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.IOException;
import java.nio.ByteOrder;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.values.*;

import static com.lightcrafts.image.metadata.CIFFTags.*;
import static com.lightcrafts.image.metadata.ImageMetadataConstants.*;
import static com.lightcrafts.image.types.CIFFConstants.*;

/**
 * An <code>CIFFMetadataReader</code> is-an {@link ImageMetadataReader} for
 * reading CIFF metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <i>CIFF 1.0r4</i>, Canon Incorporated, December 1997.
 */
public final class CIFFMetadataReader extends ImageMetadataReader {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>CIFFMetadataReader</code>.
     *
     * @param imageInfo The CIFF image to read the metadata from.
     */
    public CIFFMetadataReader( ImageInfo imageInfo ) {
        super( imageInfo, imageInfo.getByteBuffer() );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Read the metadata from all directories.
     */
    protected void readAllDirectories() throws IOException {
        final int rootDirBlockOffset = m_buf.getInt( CIFF_SHORT_SIZE );
        final int rootDirBlockLen = m_buf.limit() - rootDirBlockOffset;
        final ImageMetadataDirectory dir =
            m_metadata.getDirectoryFor( CIFFDirectory.class, true );
        readDirectory( rootDirBlockOffset, rootDirBlockLen, dir );
    }

    /**
     * Read the image header.
     *
     * @throws BadImageFileException if the internal format of the image file
     * header isn't as it's expected to be.
     */
    protected void readHeader() throws BadImageFileException, IOException {
        m_buf.position( 0 );
        if ( m_buf.remaining() < CIFF_HEADER_SIZE )
            throw new BadImageFileException( m_imageInfo.getFile() );
        final int byteOrder = m_buf.getShort();
        if ( byteOrder == CIFF_LITTLE_ENDIAN )
            m_buf.order( ByteOrder.LITTLE_ENDIAN );
        else if ( byteOrder != CIFF_BIG_ENDIAN )
            throw new BadImageFileException( m_imageInfo.getFile() );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Calculate the position of a directory entry.
     *
     * @param dirEntryOffset The offset of the directory entry.
     * @param entry The entry to calculate the position of.
     * @return Returns said position.
     */
    private static int calcEntryPosition( int dirEntryOffset, int entry ) {
        return  dirEntryOffset + entry * CIFF_IFD_ENTRY_SIZE
                + CIFF_SHORT_SIZE;            // skip entryCount
    }

    /**
     * Returns the size of a field data type in bytes.
     *
     * @param fieldType The type of the field.
     * @return Returns said size.
     */
    private static int fieldSize( int fieldType ) {
        switch ( fieldType ) {
            case CIFF_FIELD_TYPE_ASCII:
            case CIFF_FIELD_TYPE_MIXED:
            case CIFF_FIELD_TYPE_UBYTE:
                return 1;
            case CIFF_FIELD_TYPE_USHORT:
                return CIFF_FIELD_SIZE_USHORT;
            case CIFF_FIELD_TYPE_ULONG:
                return CIFF_FIELD_SIZE_ULONG;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Read the metadata from a single directory.
     *
     * @param dirBlockOffset The offset from the beginning of the file to the
     * directory.
     * @param dirBlockLen The length of the directory block.
     * @param dir The metadata is put here.
     */
    private void readDirectory( int dirBlockOffset, int dirBlockLen,
                                ImageMetadataDirectory dir )
        throws IOException
    {
        final int dirEntryOffset = dirBlockOffset + m_buf.getInt(
            dirBlockOffset + dirBlockLen - CIFF_INT_SIZE
        );
        int entryCount = m_buf.getShort( dirEntryOffset );
        if ( entryCount > DIRECTORY_ENTRY_MAX_SANE_COUNT )
            entryCount = DIRECTORY_ENTRY_MAX_SANE_COUNT;
        for ( int entry = 0; entry < entryCount; ++entry ) {
            final int pos = calcEntryPosition( dirEntryOffset, entry );
            try {
                readDirectoryEntry( dirBlockOffset, pos, dir );
            }
            catch ( IOException e ) {
                throw e;
            }
            catch ( Exception e ) {
                logBadImageMetadata( e );
            }
        }
    }

    /**
     * Read the metadata from a single directory entry.
     *
     * @param blockOffset
     * @param offset The offset from the beginning of the file of the
     * directory entry.
     * @param dir The metadata is put here.
     * @return Returns <code>true</code> only if the entry was read
     * successfully.
     */
    private boolean readDirectoryEntry( int blockOffset, int offset,
                                        ImageMetadataDirectory dir )
        throws IOException
    {
        m_buf.position( offset );
        final int tagBits = m_buf.getUnsignedShort();
        if ( tagBits < 1 ) {
            logBadImageMetadata();
            return false;
        }
        int entryLen = m_buf.getInt();
        int valueOffset = m_buf.getInt();

        final int tagID = tagBits & CIFF_TAG_ID_MASK;
        final int dataFormat = tagBits & CIFF_DATA_TYPE_MASK;

        if ( (tagBits & CIFF_DATA_LOC_IN_ENTRY) != 0 ) {
            entryLen = 8;
            valueOffset = offset + CIFF_SHORT_SIZE;
        } else {
            valueOffset += blockOffset;
            switch ( tagID ) {
                case CIFF_JPG_FROM_RAW:
                    dir.putValue(
                        CIFF_PREVIEW_IMAGE_OFFSET,
                        new UnsignedLongMetaValue( valueOffset )
                    );
                    dir.putValue(
                        CIFF_PREVIEW_IMAGE_LENGTH,
                        new UnsignedLongMetaValue( entryLen )
                    );
                    return true;
                case CIFF_THUMBNAIL_IMAGE:
                    break;
            }
            switch ( dataFormat ) {
                case CIFF_FIELD_TYPE_HEAP1:
                case CIFF_FIELD_TYPE_HEAP2:
                    readDirectory( valueOffset, entryLen, dir );
                    return true;
                case CIFF_FIELD_TYPE_MIXED:
                    return true;
            }
        }

        final int numValues = entryLen / fieldSize( dataFormat );
        final ImageMetaValue value = readValue(
            tagID, dataFormat, valueOffset, numValues
        );
        if ( value != null )
            try {
                dir.putValue( tagID, value );
            }
            catch ( IllegalArgumentException e ) {
                logBadImageMetadata();
            }

        return true;
    }

    /**
     * Read the make and model from the CIFF metadata.
     *
     * @param offset The offset of the first character.
     * @param length The length of the string.
     * @return Returns the make and model seperated by a <code>null</code>
     * byte.
     * @see {@link CIFFTags#CIFF_MAKE_MODEL}
     */
    private String readMakeModel( int offset, int length ) throws IOException {
        final byte[] bytes = m_buf.getBytes( offset, length );
        //
        // Sometimes there's extra cruft at the end: trim it off.
        //
        while ( length > 0 && bytes[ length - 1 ] != 0 )
            --length;
        return length > 0 ? new String( bytes, 0, length ) : null;
    }

    /**
     * Parses a {@link String} out of the CIFF metadata.
     *
     * @param offset The offset of the first character.
     * @param maxLength The maximum length of the string.
     * @return Returns the parsed string.
     */
    private String readString( int offset, int maxLength ) throws IOException {
        // TODO: handle multiple strings
        int length = 0;
        while ( m_buf.get( offset + length ) != '\0' && length < maxLength )
            ++length;
        return m_buf.getString( offset, length, "UTF-8" );
    }

    /**
     * Read an IFD tag's value.
     *
     * @param tagID The value's tag ID.
     * @param fieldType The tag's type.
     * @param offset The offset of the value.
     * @param numValues The number of values.
     * @return Returns a new {@link ImageMetaValue}.
     */
    private ImageMetaValue readValue( int tagID, int fieldType, int offset,
                                      int numValues )
        throws IOException
    {
        //
        // Handle special-case tags.
        //
        switch ( tagID ) {
            case CIFF_CAPTURED_TIME:
                long seconds = m_buf.getInt( offset );
                final int tzOffset =
                    m_buf.getInt( offset + CIFF_FIELD_SIZE_ULONG );
                final int tzInfo =
                    m_buf.getInt( offset + CIFF_FIELD_SIZE_ULONG * 2 );
                if ( (tzInfo & 0x80000000) != 0 ) {
                    //
                    // tzOffset is valid only if tzInfo bit 31 is 1.
                    //
                    seconds += tzOffset;
                }
                return new DateMetaValue( seconds * 1000 );
        }

        switch ( fieldType ) {

            case CIFF_FIELD_TYPE_ASCII: {
                final String s;
                switch ( tagID ) {
                    case CIFF_MAKE_MODEL:
                        s = readMakeModel( offset, numValues );
                        break;
                    default:
                        s = readString( offset, numValues );
                }
                return s != null ? new StringMetaValue( s ) : null;
            }

            case CIFF_FIELD_TYPE_UBYTE: {
                final long[] values = new long[ numValues ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getUnsignedByte( offset + i );
                return new UnsignedByteMetaValue( values );
            }

            case CIFF_FIELD_TYPE_ULONG: {
                final long[] values = new long[ numValues ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getInt(
                        offset + i * CIFF_FIELD_SIZE_ULONG
                    );
                return new UnsignedLongMetaValue( values );
            }

            case CIFF_FIELD_TYPE_USHORT: {
                final long[] values = new long[ numValues ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getShort(
                        offset + i * CIFF_FIELD_SIZE_USHORT
                    );
                return new UnsignedShortMetaValue( values );
            }

            default:
                logBadImageMetadata();
                return null;
        }
    }
}
/* vim:set et sw=4 ts=4: */
