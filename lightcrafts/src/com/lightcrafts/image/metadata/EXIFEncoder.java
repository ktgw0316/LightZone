/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;
import com.lightcrafts.utils.Rational;

import static com.lightcrafts.image.metadata.EXIFConstants.*;
import static com.lightcrafts.image.metadata.EXIFTags.*;
import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * An <code>EXIFEncoder</code> is used to encode EXIF metadata to a
 * {@link ByteBuffer} containing the raw bytes of a JPEG APP1 segment.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class EXIFEncoder {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Encode EXIF metadata to a {@link ByteBuffer} containing the raw bytes of
     * a JPEG APP1 segment.
     *
     * @param metadata The {@link ImageMetadata} to encode.
     * @param includeHeader If <code>true</code>, include the EXIF header.
     * @return Returns Returns said {@link ByteBuffer}.
     */
    public static ByteBuffer encode( ImageMetadata metadata,
                                     boolean includeHeader ) {
        final EXIFEncoder encoder = new EXIFEncoder( metadata, includeHeader );
        return encoder.encode();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct an <code>EXIFEncoder</code>.
     *
     * @param metadata The {link ImageMetadata} to adapt.
     * @param includeHeader If <code>true</code>, include the EXIF header.
     */
    private EXIFEncoder( ImageMetadata metadata, boolean includeHeader ) {
        m_metadata = metadata;
        m_includeHeader = includeHeader;
    }

    /**
     * Calculate the size of a directory if it were to be encoded inside of a
     * JPEG image.
     *
     * @param dir The {@link ImageMetadataDirectory} to calculate the size of.
     * @return Returns the size in bytes.
     */
    private int calcDirSize( ImageMetadataDirectory dir ) {
        int size = calcIFDSize( dir );
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i = dir.iterator();
              i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            final int tagID = me.getKey();
            if ( skipTag( tagID ) ) {
                size -= EXIF_IFD_ENTRY_SIZE;
                continue;
            }

            final ImageMetaValue imValue = me.getValue();
            int valueSize = calcValueSize( imValue );
            valueSize += valueSize & 1; // ensure all sizes are even

            if ( valueSize > TIFF_INLINE_VALUE_MAX_SIZE )
                size += valueSize;

            switch ( tagID ) {
                case EXIF_GPS_IFD_POINTER:
                    final ImageMetadataDirectory gpsDir =
                        m_metadata.getDirectoryFor( GPSDirectory.class );
                    if ( gpsDir != null )
                        size += calcDirSize( gpsDir );
                    break;
                case EXIF_IFD_POINTER:
                    final ImageMetadataDirectory subEXIFDir =
                        m_metadata.getDirectoryFor( SubEXIFDirectory.class );
                    if ( subEXIFDir != null )
                        size += calcDirSize( subEXIFDir );
                    break;
                case EXIF_INTEROPERABILITY_POINTER:
                    // TODO
                    break;
                case EXIF_MAKER_NOTE:
                    // TODO
                    break;
            }
        }
        return size;
    }

    /**
     * Calculate a delta for a directory's entry count.  This is needed since
     * we don't handle some tags so they must not be exported as part of the
     * metadata.  Hence, the count delta is the original count minus the number
     * of tags we don't handle.
     *
     * @param dir The {@link ImageMetadataDirectory} to calculate the entry
     * count detla of.
     * @return Returns said size.
     */
    private static int calcDirEntriesDelta( ImageMetadataDirectory dir ) {
        int delta = 0;
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i = dir.iterator();
              i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            final int tagID = me.getKey();
            if ( skipTag( tagID ) )
                --delta;
        }
        return delta;
    }

    /**
     * Calculate the size of a directory IFD without the size of the values
     * longer than 4 bytes.
     *
     * @param dir The {@link ImageMetadataDirectory} to calculate the size of.
     * @return Returns said size.
     */
    private static int calcIFDSize( ImageMetadataDirectory dir ) {
        return  EXIF_SHORT_SIZE         // number of entries
                + dir.size() * EXIF_IFD_ENTRY_SIZE
                + EXIF_INT_SIZE;        // next directory offset
    }

    /**
     * Calculate the encoded size of an {@link ImageMetaValue}.
     *
     * @param value The {@link ImageMetaValue} to calculate the size of.
     * @return Returns said size in bytes.
     */
    private static int calcValueSize( ImageMetaValue value ) {
        int size = 0;
        switch ( value.getType() ) {
            case META_DATE:
                //
                // A date type is our invention and doesn't exist in EXIF
                // metadata as a distinct type.  Instead, dates are stored as
                // strings.
                //
            case META_STRING:
                for ( String s : value.getValues() ) {
                    try {
                        final byte[] b = s.getBytes( "UTF-8" );
                        size += b.length + 1 /* for null */;
                    }
                    catch ( UnsupportedEncodingException e ) {
                        throw new IllegalStateException( e );
                    }
                }
                break;

            case META_UNDEFINED:
                final UndefinedMetaValue undefined = (UndefinedMetaValue)value;
                size = undefined.getUndefinedValue().length;
                break;

            default:
                size = value.getValueCount()
                    * EXIF_FIELD_SIZE[ value.getType().getTIFFConstant() ];
        }
        return size;
    }

    /**
     * Encode EXIF metadata to a {@link ByteBuffer} containing the raw bytes of
     * a JPEG APP1 segment.
     *
     * @return Returns Returns said {@link ByteBuffer}.
     */
    private ByteBuffer encode() {
        final ImageMetadataDirectory exifDir =
            m_metadata.getDirectoryFor( EXIFDirectory.class );
        return exifDir != null ? encodeEXIFDir( (EXIFDirectory)exifDir ) : null;
    }

    /**
     * Encode an {@link EXIFDirectory}'s values into a {@link ByteBuffer}
     * suitable for writing into a JPEG image file.
     *
     * @param dir The {@link EXIFDirectory} to encode.
     * @return Returns said {@link ByteBuffer}.
     */
    private ByteBuffer encodeEXIFDir( EXIFDirectory dir ) {
        final int entriesDelta = calcDirEntriesDelta( dir );
        m_nextEXIFBigValuePos =
            calcIFDSize( dir ) + entriesDelta * EXIF_IFD_ENTRY_SIZE;
        m_endPosPlus1 = calcDirSize( dir );
        if ( m_includeHeader ) {
            m_nextEXIFBigValuePos += EXIF_HEADER_SIZE;
            m_endPosPlus1 += EXIF_HEADER_SIZE;
        }
        final ByteBuffer buf = ByteBuffer.allocate( m_endPosPlus1 );
        final ByteOrder nativeOrder = ByteOrder.nativeOrder();
        buf.order( nativeOrder );
        if ( m_includeHeader ) {
            ByteBufferUtil.put( buf, "Exif\0\0", "ASCII" );
            buf.putShort(
                nativeOrder == ByteOrder.BIG_ENDIAN ?
                    TIFF_BIG_ENDIAN : TIFF_LITTLE_ENDIAN
            );
            buf.putShort( TIFF_MAGIC_NUMBER );
            buf.putInt( TIFF_HEADER_SIZE ); // 0th directory offset
        }
        encodeEXIFDir( dir, buf, entriesDelta );
        return buf;
    }

    /**
     * Encode an {@link EXIFDirectory}'s values into a {@link ByteBuffer}
     * suitable for writing into a JPEG image file.
     *
     * @param dir The {@link ImageMetadataDirectory} to encode.
     * @param buf The {@link ByteBuffer} to encode into.
     * @param entriesDelta The detla to add to the directory's entry count, if
     * any.
     */
    private void encodeEXIFDir( ImageMetadataDirectory dir, ByteBuffer buf,
                                int entriesDelta ) {
        buf.putShort( (short)(dir.size() + entriesDelta) );
        //
        // When written to a JPEG file, tag IDs must be in ascending order, so
        // sort them.
        //
        final Integer[] tagIDs =
            dir.getTagIDSet( false ).toArray( new Integer[]{ null } );
        Arrays.sort( tagIDs );

        for ( int tagID : tagIDs ) {
            if ( skipTag( tagID ) )
                continue;

            buf.putShort( (short)tagID );
            final ImageMetaValue imValue = dir.getValue( tagID );
            int valueSize = calcValueSize( imValue );

            switch ( imValue.getType() ) {
                case META_DATE:
                    //
                    // A date type is our invention and doesn't exist in JPEG
                    // metadata as a distinct type.  Instead, dates are stored
                    // as strings.
                    //
                case META_STRING:
                    buf.putShort( EXIF_FIELD_TYPE_STRING );
                    buf.putInt( valueSize );
                    break;

                case META_UNDEFINED:
                    buf.putShort( imValue.getType().getTIFFConstant() );
                    buf.putInt( valueSize );
                    break;

                default:
                    buf.putShort( imValue.getType().getTIFFConstant() );
                    buf.putInt( imValue.getValueCount() );
            }

            valueSize += valueSize & 1; // ensure all sizes are even

            int prevBufPos = 0;
            if ( valueSize > TIFF_INLINE_VALUE_MAX_SIZE ) {
                buf.putInt(
                    m_nextEXIFBigValuePos
                    - (m_includeHeader ? EXIF_HEADER_START_SIZE : 0)
                );
                prevBufPos = buf.position();
                buf.position( m_nextEXIFBigValuePos );
                m_nextEXIFBigValuePos += valueSize;
            }

            switch ( imValue.getType() ) {
                case META_SBYTE:
                case META_UBYTE: {
                    final long[] bytes =
                        ((LongMetaValue)imValue).getLongValues();
                    for ( long b : bytes )
                        buf.put( (byte)(b & 0xFF) );
                    for ( int j = bytes.length; j < TIFF_INLINE_VALUE_MAX_SIZE;
                          ++j )
                        buf.put( (byte)0 );
                    break;
                }
                case META_DATE:
                case META_STRING: {
                    try {
                        final String[] strings = imValue.getValues();
                        final byte[] b = strings[0].getBytes( "UTF-8" );
                        buf.put( b );
                        buf.put( (byte)0 );
                        for ( int j = b.length + 1 /* for null */;
                              j < TIFF_INLINE_VALUE_MAX_SIZE; ++j )
                            buf.put( (byte)0 );
                    }
                    catch ( UnsupportedEncodingException e ) {
                        throw new IllegalStateException( e );
                    }
                    break;
                }
                case META_SSHORT:
                case META_USHORT: {
                    final long[] shorts =
                        ((LongMetaValue)imValue).getLongValues();
                    for ( long s : shorts )
                        buf.putShort( (short)(s & 0xFFFF) );
                    if ( valueSize < TIFF_INLINE_VALUE_MAX_SIZE )
                        buf.putShort( (short)0 );
                    break;
                }
                case META_SLONG:
                case META_ULONG: {
                    final long[] longs =
                        ((LongMetaValue)imValue).getLongValues();
                    for ( long l : longs )
                        buf.putInt( (int)(l & 0x00000000FFFFFFFF) );
                    break;
                }
                case META_SRATIONAL:
                case META_URATIONAL: {
                    final Rational[] rats =
                        ((RationalMetaValue)imValue).getRationalValues();
                    for ( Rational r : rats ) {
                        buf.putInt( r.numerator() );
                        buf.putInt( r.denominator() );
                    }
                    break;
                }
                case META_FLOAT: {
                    final float[] floats =
                        ((FloatMetaValue)imValue).getFloatValues();
                    for ( float f : floats )
                        buf.putFloat( f );
                    break;
                }
                case META_DOUBLE: {
                    final double[] doubles =
                        ((DoubleMetaValue)imValue).getDoubleValues();
                    for ( double d : doubles )
                        buf.putDouble( d );
                    break;
                }
                case META_UNDEFINED: {
                    final byte[] bytes =
                        ((UndefinedMetaValue)imValue).getUndefinedValue();
                    buf.put( bytes );
                    for ( int j = bytes.length; j < TIFF_INLINE_VALUE_MAX_SIZE;
                          ++j )
                        buf.put( (byte)0 );
                    break;
                }
                default:
                    throw new IllegalStateException();
            }

            switch ( tagID ) {
                case EXIF_GPS_IFD_POINTER:
                    encodeSubEXIFDir( GPSDirectory.class, buf );
                    break;
                case EXIF_IFD_POINTER:
                    encodeSubEXIFDir( SubEXIFDirectory.class, buf );
                    break;
                case EXIF_INTEROPERABILITY_POINTER:
                    // TODO
                    break;
                case EXIF_MAKER_NOTE:
                    // TODO
                    break;
            }

            if ( valueSize > TIFF_INLINE_VALUE_MAX_SIZE )
                buf.position( prevBufPos );
        }
        buf.putInt( 0 );                // next directory offset
    }

    /**
     * Encode an {@link EXIFDirectory}'s values for a subdirectory into a
     * {@link ByteBuffer}.
     *
     * @param dirClass The {@link Class} of the {@link ImageMetadataDirectory}
     * to encode.
     * @param buf The {@link ByteBuffer} to use.
     */
    private void encodeSubEXIFDir(
        Class<? extends ImageMetadataDirectory> dirClass, ByteBuffer buf )
    {
        final ImageMetadataDirectory subDir =
            m_metadata.getDirectoryFor( dirClass );
        //
        // Subdirectories are encoded at the end of the buffer working
        // backwards.
        //
        m_endPosPlus1 -= calcDirSize( subDir );

        //
        // Back up and overwrite the subIFD's offset value that is the original
        // offset from the original image file since that value is now garbage.
        //
        buf.position( buf.position() - EXIF_INT_SIZE );
        buf.putInt(
            m_endPosPlus1 - (m_includeHeader ? EXIF_HEADER_START_SIZE : 0)
        );

        //
        // Remember the buffer position for the parent directory.
        //
        final int prevBufPos = buf.position();
        buf.position( m_endPosPlus1 );

        //
        // Remember the old m_nextEXIFBigValuePos and compute a new one for the
        // subdirectory.
        //
        final int origNextEXIFBigValuePos = m_nextEXIFBigValuePos;
        final int entriesDelta = calcDirEntriesDelta( subDir );
        m_nextEXIFBigValuePos =
            m_endPosPlus1 + calcIFDSize( subDir )
            + entriesDelta * EXIF_IFD_ENTRY_SIZE;

        encodeEXIFDir( subDir, buf, entriesDelta );

        //
        // Put everything back the way it was.
        //
        buf.position( prevBufPos );
        m_nextEXIFBigValuePos = origNextEXIFBigValuePos;
    }

    /**
     * Checks whether the given tag should be skipped.
     *
     * @param tagID The tag ID to check.
     * @return Returns <code>true</code> only if the tag should be skipped.
     */
    private static boolean skipTag( int tagID ) {
        switch ( tagID ) {
            case EXIF_COLOR_SPACE:
                //
                // We don't know what the color profile being used is, so
                // we can't know what this value should be.  Therefore,
                // just skip it.
                //
            case EXIF_INTEROPERABILITY_POINTER:
                //
                // We don't know what to do with this yet.
                //
            case EXIF_MAKER_NOTE:
                //
                // Doing this is extremely difficult.
                //
                return true;
            default:
                return false;
        }
    }

    /**
     * Initially, this is the position one past the end of the size of the
     * {@link ByteBuffer} that EXIF metadata is being encoded to.  When a
     * subdirectory is encountered, this value is decremented by the size of
     * the subdirectory and the subdirectory is encoded starting at the new
     * value.  Hence, subdirectories are encoded at the end of the buffer
     * working backwards.
     */
    private int m_endPosPlus1;

    /**
     * Whether to include the EXIF header in the encoding.
     */
    private final boolean m_includeHeader;

    /**
     * The {@link ImageMetadata} to encode.
     */
    private final ImageMetadata m_metadata;

    /**
     * EXIF metadata values larger than 4 bytes are stored seperately past all
     * the directory entries.  This is used to keep track of the next available
     * position for such a value.
     */
    private int m_nextEXIFBigValuePos;
}
/* vim:set et sw=4 ts=4: */
