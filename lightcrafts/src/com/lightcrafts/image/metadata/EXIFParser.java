/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.Rational;

import static com.lightcrafts.image.metadata.EXIFConstants.*;
import static com.lightcrafts.image.metadata.EXIFTags.*;
import static com.lightcrafts.image.metadata.ImageMetadataConstants.*;
import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * An <code>EXIFParser</code> is used to parse raw EXIF metadata from an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class EXIFParser {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>EXIFParser</code>.
     *
     * @param handler The {@link EXIFParserEventHandler} to use.
     * @param imageInfo The image.
     * @param exifSegBuf The {@link ByteBuffer} containing the raw binary EXIF
     * metadata from the image file.  Note that this is a subset of the
     * {@link ByteBuffer} of the entire image file.
     * @param isSubdirectory This is <code>true</code> only if we're reading a
     * subdirectory.
     */
    public EXIFParser(EXIFParserEventHandler handler, ImageInfo imageInfo,
               LCByteBuffer exifSegBuf, boolean isSubdirectory) {
        m_buf = exifSegBuf;
        m_handler = handler;
        m_imageInfo = imageInfo;
        m_isSubdirectory = isSubdirectory;
    }

    /**
     * Parse the metadata from all directories.
     */
    public void parseAllDirectories() throws IOException {
        int ifdOffset = EXIF_HEADER_START_SIZE + m_buf.getInt();
        final Set<Integer> ifdOffsetSet = new HashSet<Integer>();
        while ( true ) {
            if ( ifdOffset == 0 )
                break;
            if ( ifdOffset < 0 ) {
                m_handler.gotBadMetadata( "IFD offset < 0" );
                break;
            }
            if ( !ifdOffsetSet.add( ifdOffset ) ) {
                //
                // There are some bad images in the wild where the next IFD
                // offset refers to a previous offset: catch this case to
                // prevent an infinite loop.
                //
                break;
            }
            final ImageMetadataDirectory dir = m_handler.gotDirectory();
            parseDirectory( ifdOffset, 0, dir );
            if ( m_stop )
                break;
            ifdOffset = m_buf.getInt();
            if ( ifdOffset > 0 ) {
                ifdOffset += EXIF_HEADER_START_SIZE;
                if ( ifdOffset >= m_buf.limit() )
                    m_handler.gotBadMetadata( "IFD offset >= EXIF limit" );
            }
        }
    }

    /**
     * Parse the metadata from a single directory.
     *
     * @param offset The offset from the beginning of the file of the
     * directory.
     * @param valueOffsetAdjustment The larger-than-4-byte-value offset
     * adjustment.
     * @param dir The {@link ImageMetadataDirectory}to parse metadata for.
     */
    public void parseDirectory( int offset, int valueOffsetAdjustment,
                                ImageMetadataDirectory dir ) throws IOException {
        int entryCount = m_buf.getUnsignedShort( offset );
        if ( entryCount > DIRECTORY_ENTRY_MAX_SANE_COUNT )
            entryCount = DIRECTORY_ENTRY_MAX_SANE_COUNT;
        for ( int entry = 0; entry < entryCount; ++entry ) {
            try {
                final int pos = calcIFDEntryPosition( offset, entry );
                parseDirectoryEntry( pos, valueOffsetAdjustment, dir );
            }
            catch ( RuntimeException e ) {
                m_handler.gotBadMetadata( e );
            }
            if ( m_stop )
                return;
        }
        //
        // Position the buffer immediately after the last entry so
        // readAllDirectories() can read the following offset to the next
        // directory, if any.
        //
        m_buf.position( calcIFDEntryPosition( offset, entryCount ) );
    }

    /**
     * Parse the EXIF header.
     *
     * @throws BadImageFileException if the header isn't as it's expected to be.
     */
    public void parseHeader() throws BadImageFileException, IOException {
        m_buf.position( 0 );
        if ( m_buf.remaining() < EXIF_HEADER_SIZE )
            throw new BadImageFileException( m_imageInfo.getFile() );
        if ( !m_buf.getEquals( "Exif", "ASCII" ) )
            throw new BadImageFileException( m_imageInfo.getFile() );
        m_buf.skipBytes( 2 );

        final int byteOrder = m_buf.getShort();
        if ( byteOrder == TIFF_LITTLE_ENDIAN )
            m_buf.order( ByteOrder.LITTLE_ENDIAN );
        else if ( byteOrder == TIFF_BIG_ENDIAN )
            m_buf.order( ByteOrder.BIG_ENDIAN );
        else
            throw new BadImageFileException( m_imageInfo.getFile() );

        if ( m_buf.getUnsignedShort() != TIFF_MAGIC_NUMBER )
            throw new BadImageFileException( m_imageInfo.getFile() );
    }

    /**
     * Parse a value (or values) for a given EXIF tag.
     *
     * @param tagID The ID of the tag that &quot;owns&quot; this value.
     * @param fieldType The type of value.
     * @param offset The offset of the first byte of the value.
     * @param numValues The number of values.
     * @return Returns a new {@link ImageMetaValue}.
     */
    public ImageMetaValue parseValue( int tagID, int fieldType, int offset,
                                      int numValues ) throws IOException {
        switch ( fieldType ) {

            case EXIF_FIELD_TYPE_STRING: {
                final String s = parseString( offset, numValues );
                switch ( tagID ) {
                    case EXIF_DATE_TIME:
                    case EXIF_DATE_TIME_DIGITIZED:
                    case EXIF_DATE_TIME_ORIGINAL:
                        //
                        // We elevate dates from mere Strings into Date types
                        // in their own right.
                        //
                        return new DateMetaValue( s );
                    default:
                        return new StringMetaValue( s );
                }
            }

            case EXIF_FIELD_TYPE_SBYTE: {
                final long[] values = new long[ numValues ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.get( offset + i );
                return new ByteMetaValue( values );
            }

            case EXIF_FIELD_TYPE_UBYTE: {
                final long[] values = new long[ numValues ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getUnsignedByte( offset + i );
                return new UnsignedByteMetaValue( values );
            }

            case EXIF_FIELD_TYPE_SLONG: {
                final long[] values = new long[ numValues ];
                final int valueSize = EXIF_FIELD_SIZE[ EXIF_FIELD_TYPE_SLONG ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getInt( offset + i * valueSize );
                return new LongMetaValue( values );
            }

            case EXIF_FIELD_TYPE_ULONG: {
                final long[] values = new long[ numValues ];
                final int valueSize = EXIF_FIELD_SIZE[ EXIF_FIELD_TYPE_ULONG ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getInt( offset + i * valueSize );
                return new UnsignedLongMetaValue( values );
            }

            case EXIF_FIELD_TYPE_SRATIONAL: {
                final Rational[] values = new Rational[ numValues ];
                final int valueSize =
                    EXIF_FIELD_SIZE[ EXIF_FIELD_TYPE_SRATIONAL ];
                final int longSize = EXIF_FIELD_SIZE[ EXIF_FIELD_TYPE_SLONG ];
                for ( int i = 0; i < numValues; ++i )
                    try {
                        final int pos = offset + i * valueSize;
                        values[i] = new Rational(
                            m_buf.getInt( pos ), m_buf.getInt( pos + longSize )
                        );
                    }
                    catch ( IllegalArgumentException e ) {
                        m_handler.gotBadMetadata( e );
                        return null;
                    }
                return new RationalMetaValue( values );
            }

            case EXIF_FIELD_TYPE_URATIONAL: {
                final Rational[] values = new Rational[ numValues ];
                final int valueSize =
                    EXIF_FIELD_SIZE[ EXIF_FIELD_TYPE_URATIONAL ];
                final int longSize = EXIF_FIELD_SIZE[ EXIF_FIELD_TYPE_ULONG ];
                for ( int i = 0; i < numValues; ++i )
                    try {
                        final int pos = offset + i * valueSize;
                        values[i] = new Rational(
                            m_buf.getInt( pos ), m_buf.getInt( pos + longSize )
                        );
                    }
                    catch ( IllegalArgumentException e ) {
                        m_handler.gotBadMetadata( e );
                        return null;
                    }
                return new UnsignedRationalMetaValue( values );
            }

            case EXIF_FIELD_TYPE_SSHORT: {
                final long[] values = new long[ numValues ];
                final int valueSize = EXIF_FIELD_SIZE[ EXIF_FIELD_TYPE_SSHORT ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getShort( offset + i * valueSize );
                return new ShortMetaValue( values );
            }

            case EXIF_FIELD_TYPE_USHORT: {
                final long[] values = new long[ numValues ];
                final int valueSize = EXIF_FIELD_SIZE[ EXIF_FIELD_TYPE_USHORT ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getUnsignedShort( offset + i * valueSize );
                return new UnsignedShortMetaValue( values );
            }

            case EXIF_FIELD_TYPE_FLOAT: {
                final float[] values = new float[ numValues ];
                final int valueSize = EXIF_FIELD_SIZE[ TIFF_FIELD_TYPE_FLOAT ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getFloat( offset + i * valueSize );
                return new FloatMetaValue( values );
            }

            case EXIF_FIELD_TYPE_DOUBLE: {
                final double[] values = new double[ numValues ];
                final int valueSize = EXIF_FIELD_SIZE[ TIFF_FIELD_TYPE_DOUBLE ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getDouble( offset + i * valueSize );
                return new DoubleMetaValue( values );
            }

            case EXIF_FIELD_TYPE_UNDEFINED:
                return new UndefinedMetaValue(
                    m_buf.getBytes( offset, numValues )
                );

            default:
                throw new IllegalStateException(
                    String.format( "unknown field type (0x%x) for tag ID 0x%x",
                                   fieldType, tagID )
                );
        }
    }

    /**
     * Stop parsing immediately.
     */
    public void stopParsing() {
        m_stop = true;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Calculate an IFD's entry position.
     *
     * @param ifdOffset The offset of the start of the IFD.
     * @param entry The entry number within the currenf IFD to calculate the
     * position of.
     * @return Returns said position.
     */
    public static int calcIFDEntryPosition(int ifdOffset, int entry) {
        return ifdOffset + EXIF_SHORT_SIZE + entry * EXIF_IFD_ENTRY_SIZE;
    }

    /**
     * Calculate the offset of a metadata value.
     *
     * @param byteCount The number of bytes comprising the value.
     * @param valueOffsetAdjustment The larger-than-4-byte-value offset
     * adjustment.
     * @return Returns the offset of the value.
     */
    private int calcValueOffset( int byteCount, int valueOffsetAdjustment )
        throws IOException
    {
        if ( byteCount <= TIFF_INLINE_VALUE_MAX_SIZE ) {
            //
            // The value in the directory entry itself.
            //
            return m_buf.position();
        }
        final int offset = m_buf.getInt()
            + (m_isSubdirectory ? 0 : EXIF_HEADER_START_SIZE)
            + valueOffsetAdjustment;
        if ( offset + byteCount > m_buf.limit() ) {
            //
            // Bogus offset and/or byteCount.
            //
            return -1;
        }
        return offset;
    }

    /**
     * Parse the metadata from a single directory entry.
     *
     * @param offset The offset from the beginning of the file of the
     * directory entry.
     * @param valueOffsetAdjustment The larger-than-4-byte-value offset
     * adjustment.
     * @param dir The {@link ImageMetadataDirectory}to parse metadata for.
     */
    private void parseDirectoryEntry( int offset, int valueOffsetAdjustment,
                                      ImageMetadataDirectory dir )
        throws IOException
    {
        m_buf.position( offset );
        final int tagID = m_buf.getUnsignedShort();
        if ( tagID < 0 )
            return;
        final int fieldType = m_buf.getUnsignedShort();
        if ( fieldType <= 0 || fieldType >= EXIF_FIELD_SIZE.length ) {
            m_handler.gotBadMetadata(
                String.format( "unknown field type (0x%x) for tag ID 0x%x",
                               fieldType, tagID )
            );
            return;
        }

        final int numValues = m_buf.getInt();
        if ( numValues < 0 ) {
            m_handler.gotBadMetadata(
                String.format( "numValues (0x%x) < 0 for tag ID 0x%x",
                               numValues, tagID )
            );
            return;
        }
        final int byteCount = numValues * EXIF_FIELD_SIZE[ fieldType ];
        if ( byteCount <= 0 )
            return;
        final int valueOffset =
            calcValueOffset( byteCount, valueOffsetAdjustment );
        if ( valueOffset < 0 ) {
            m_handler.gotBadMetadata(
                String.format( "valueOffset < 0 for tag ID 0x%x", tagID )
            );
            return;
        }

        final int subdirOffset =
            (m_isSubdirectory ? 0 : EXIF_HEADER_START_SIZE)
                + m_buf.getInt( valueOffset )
                + valueOffsetAdjustment;

        m_handler.gotTag(
            tagID, fieldType, numValues, byteCount, valueOffset,
            valueOffsetAdjustment, subdirOffset, m_imageInfo, m_buf, dir
        );
    }

    /**
     * Parse a <code>String</code> out of EXIF metadata by scanning for an
     * embedded null byte and stopping there if found.
     *
     * @param offset The offset of the first character.
     * @param maxLength The maximum length of the string.
     * @return Returns the parsed string.
     */
    private String parseString( int offset, int maxLength ) throws IOException {
        int length = 0;
        while ( length < maxLength && m_buf.get( offset + length ) != '\0' )
            ++length;
        return m_buf.getString( offset, length, "UTF-8" );
    }

    /**
     * This is a special case of {@link #parseString(int,int)} that handles
     * EXIF UserComment reading.  The UserComment starts with 8 bytes that
     * specify the character set.  Currently, only ASCII and Unicode are
     * supported.
     *
     * @param offset The offset of the first character.
     * @param count The number of characters.
     * @param fieldType The field type.
     * @return Returns the UserComment string or <code>null</code> if it can't
     * be read.
     */
    private String parseUserComment( int offset, int count, int fieldType )
        throws IOException
    {
        count *= EXIF_FIELD_SIZE[ fieldType ];
        //
        // At least Olympus cameras pad the comment with trailing spaces --
        // remove these first.
        //
        final byte[] bytes = m_buf.getBytes( offset, count );
        while ( count > 0 )
            if ( bytes[ count - 1 ] == ' ' )
                --count;
            else
                break;

        if ( count > 8 ) {
            count -= 8;
            final String charsetCode = new String( bytes, 0, 8 ).toUpperCase();
            if ( "ASCII\0\0\0".equals( charsetCode ) )
                return new String( bytes, 8, count );
            if ( "UNICODE\0".equals( charsetCode  ) )
                try {
                    return new String(
                        bytes, 8, count,
                        m_buf.order() == ByteOrder.LITTLE_ENDIAN ?
                            "UTF-16LE" : "UTF-16BE"
                    );
                }
                catch ( UnsupportedEncodingException e ) {
                    // should never happen, but just in case ....
                }
            // TODO: handle JIS encoding
        }
        return null;
    }

    /**
     * The {@link LCByteBuffer} containing the raw EXIF metadata to be parsed.
     */
    private final LCByteBuffer m_buf;

    /**
     * The {@link EXIFParserEventHandler} in use.
     */
    private final EXIFParserEventHandler m_handler;

    /**
     * The image the EXIF metadata was read from.
     */
    private final ImageInfo m_imageInfo;

    /**
     * This is <code>true</code> only if we're reading an EXIF subdirectory.
     */
    private final boolean m_isSubdirectory;

    /**
     * Set to <code>true</code> to stop the parser immediately.
     */
    private boolean m_stop;
}
/* vim:set et sw=4 ts=4: */
