/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.*;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.utils.Rational;
import com.lightcrafts.utils.bytebuffer.ArrayByteBuffer;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import static com.lightcrafts.image.metadata.ImageMetadataConstants.*;
import static com.lightcrafts.image.metadata.EXIFTags.*;
import static com.lightcrafts.image.metadata.TIFFTags.*;
import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * A <code>TIFFMetadataReader</code> is-an {@link ImageMetadataReader} for
 * reading TIFF metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class TIFFMetadataReader extends ImageMetadataReader {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>TIFFMetadataReader</code>.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public TIFFMetadataReader( ImageInfo imageInfo ) {
        this( imageInfo, imageInfo.getByteBuffer(), TIFFDirectory.class );
    }

    /**
     * Construct a <code>TIFFMetadataReader</code>.
     *
     * @param imageInfo The image to read the metadata from.
     * @param tiffBuf The {@link LCByteBuffer} containing TIFF metadata.
     */
    public TIFFMetadataReader( ImageInfo imageInfo, LCByteBuffer tiffBuf ) {
        this( imageInfo, tiffBuf, TIFFDirectory.class );
    }

    /**
     * Construct a <code>TIFFMetadataReader</code>.
     *
     * @param imageInfo The image to read the metadata from.
     * @param dirClass The class of the {@link ImageMetadataDirectory} class to
     * create.
     */
    public TIFFMetadataReader( ImageInfo imageInfo,
                               Class<? extends ImageMetadataDirectory> dirClass ) {
        this( imageInfo, imageInfo.getByteBuffer(), dirClass );
    }

    /**
     * Calculate an IFD entry's position.
     *
     * @param dirOffset The offset from the beginning of the file of the
     * directory.
     * @param entry The entry number: 0...<i>entryCount</i>-1.
     */
    public static int calcIFDEntryOffset( int dirOffset, int entry ) {
        return dirOffset + TIFF_SHORT_SIZE + entry * TIFF_IFD_ENTRY_SIZE;
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
        if ( m_buf.remaining() < TIFF_HEADER_SIZE )
            throw new BadImageFileException( m_imageInfo.getFile() );
        final int byteOrder = m_buf.getShort();
        if ( byteOrder == TIFF_LITTLE_ENDIAN )
            m_buf.order( ByteOrder.LITTLE_ENDIAN );
        else if ( byteOrder == TIFF_BIG_ENDIAN )
            m_buf.order( ByteOrder.BIG_ENDIAN );
        else
            throw new BadImageFileException( m_imageInfo.getFile() );
        //
        // The TIFF/EP specification allows values >= 42.
        //
        if ( m_buf.getUnsignedShort() < TIFF_MAGIC_NUMBER )
            throw new BadImageFileException( m_imageInfo.getFile() );
    }

    /**
     * Read the metadata from all directories.
     */
    protected void readAllDirectories() throws IOException {
        ImageMetaValue xmpValue = null;

        m_buf.position(
            TIFF_HEADER_SIZE
            - TIFF_INT_SIZE     // so we can read the 0th IFD offset below
        );
        int ifdOffset = m_buf.getInt();
        final Set<Integer> ifdOffsetSet = new HashSet<Integer>();
        for ( int dirIndex = 0; ifdOffset > 0; ++dirIndex ) {
            if ( !ifdOffsetSet.add( ifdOffset ) ) {
                //
                // There are some bad images in the wild where the next IFD
                // offset refers to a previous offset: catch this case to
                // prevent an infinite loop.
                //
                break;
            }

            final ImageMetadataDirectory dir =
                createDirectoryFor( m_dirClass, "IFD" + dirIndex );
            if ( dirIndex == 0 )
                m_metadata.putDirectory( dir );
            readDirectory( ifdOffset, dir );

            if ( dirIndex == 0 /* && isLightZoneLayeredTIFF() */ )
                xmpValue = dir.getValue( TIFF_XMP_PACKET );

            ifdOffset = m_buf.getInt();
            if ( ifdOffset >= m_buf.limit() )
                logBadImageMetadata();
        }

        final ImageMetadataDirectory dir = mergeDirectories();
        if ( dir == null )
            return;
        m_metadata.putDirectory( dir );

        patchEXIFImageSize( dir, EXIF_IMAGE_WIDTH, EXIF_IMAGE_HEIGHT );
        patchEXIFImageSize( dir, EXIF_PIXEL_X_DIMENSION, EXIF_PIXEL_Y_DIMENSION );

        if ( xmpValue != null )
            dir.putValue( TIFF_XMP_PACKET, xmpValue );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Checks whether the current TIFF file is a 2-page (layered) TIFF file
     * created by LightZone.
     *
     * @return Returns <code>true</code> only if the TIFF file is a LightZone
     * layers TIFF file.
     */
    private boolean isLightZoneLayeredTIFF() {
        final ImageMetadataDirectory tiffDir =
            m_metadata.getDirectoryFor( TIFFDirectory.class );
        if ( tiffDir == null )
            return false;
        ImageMetaValue value = tiffDir.getValue( TIFF_SOFTWARE );
        if ( value == null )
            return false;
        final String software = value.getStringValue();
        if ( software == null || !software.startsWith( "LightZone" ) )
            return false;
        value = tiffDir.getValue( TIFF_PAGE_NUMBER );
        if ( value == null || value.getValueCount() != 2 )
            return false;
        final String page[] = value.getValues();
        try {
            return Integer.parseInt( page[1] ) == 2;
        }
        catch ( NumberFormatException e ) {
            return false;
        }
    }

    /**
     * Construct a <code>TIFFMetadataReader</code>.
     *
     * @param imageInfo The image to read the metadata from.
     * @param tiffBuf The {@link LCByteBuffer} containing TIFF metadata.
     * @param dirClass The class of the {@link ImageMetadataDirectory} class to
     * create.
     */
    private TIFFMetadataReader( ImageInfo imageInfo, LCByteBuffer tiffBuf,
                                Class<? extends ImageMetadataDirectory> dirClass ) {
        super( imageInfo, tiffBuf );
        m_dirClass = dirClass;
    }

    /**
     * Calculate the offset for a metadata value.
     *
     * @param byteCount The number of bytes comprising the value.
     * @return Returns the offset of the value.
     */
    private int calcValueOffset( int byteCount ) throws IOException {
        if ( byteCount <= TIFF_INLINE_VALUE_MAX_SIZE ) {
            //
            // The value is "inlined" in the directory entry itself.
            //
            return m_buf.position();
        }
        final int offset = m_buf.getInt();
        if ( offset + byteCount > m_buf.limit() ) {
            //
            // Bogus offset and/or byteCount.
            //
            return -1;
        }
        return offset;
    }

    /**
     * Creates a new {@link ImageMetadataDirectory} of the given class and
     * stores a local mapping to it by the given name.
     *
     * @param dirClass The class of the {@link ImageMetadataDirectory} class to
     * create.
     * @param name The name for the {@link ImageMetadataDirectory}.
     * @return Returns a new {@link ImageMetadataDirectory} for the given
     * {@link Class}.
     */
    private ImageMetadataDirectory createDirectoryFor(
        Class<? extends ImageMetadataDirectory> dirClass, String name )
    {
        try {
            final ImageMetadataDirectory dir = dirClass.newInstance();
            dir.setOwningMetadata( m_metadata );
            m_dirMap.put( name, dir );
            return dir;
        }
        catch ( Exception e ) {
            throw new IllegalStateException( e );
        }
    }

    /**
     * Merges the metadata for all the directories into a single
     * {@link ImageMetadataDirectory} for the full-sized image (if any).
     *
     * @return Returns the merged {@link ImageMetadataDirectory} or
     * <code>null</code> if there are no directories.
     */
    private ImageMetadataDirectory mergeDirectories() {
        //
        // Handle degenerate cases.
        //
        switch ( m_dirMap.size() ) {
            case 0:
                return null;
            case 1:
                return m_dirMap.values().toArray( new ImageMetadataDirectory[0] )[0];
        }

        ImageMetadataDirectory mergedDir = null;
        ImageMetadataDirectory dirWithLargestImageSize = null;
        int largestWidth = 0;
        for ( Map.Entry<String,ImageMetadataDirectory>
              me : m_dirMap.entrySet() ) {
            final ImageMetadataDirectory dir = me.getValue();
            //
            // First, check to see if the directory contains the metadata for
            // the full-sized image.  If so, that's the one we want.
            //
            if ( MetadataUtil.isFullSizedImage( dir ) ) {
                mergedDir = dir;
                break;
            }
            //
            // As a fall-back if we don't find a directory that's known to be
            // the one for the full-sized image, make a note of the one that
            // contains the largest dimensions and assume it's the one for the
            // full-sized image.
            //
            final ImageMetaValue widthValue = dir.getValue( TIFF_IMAGE_WIDTH );
            if ( widthValue != null ) {
                final int width = widthValue.getIntValue();
                if ( width > largestWidth ) {
                    largestWidth = width;
                    dirWithLargestImageSize = dir;
                }
            }
        }

        if ( mergedDir == null )
            mergedDir = dirWithLargestImageSize;
        if ( mergedDir == null )
            return null;

        //
        // Now that we've found the directory for the full-sized image, copy
        // useful metadata from the other directories into it.
        //
        for ( Map.Entry<String,ImageMetadataDirectory>
              me : m_dirMap.entrySet() ) {
            final ImageMetadataDirectory dir = me.getValue();
            if ( dir != mergedDir )
                copyValuesFromTo( dir, mergedDir );
        }
        return mergedDir;
    }

    /**
     * When the <code>TIFFMetadataReader</code> is used to read metadata from
     * raw files, it's sometimes the case that the EXIF metadata describes the
     * preview (or thumbnail) image rather than the full-sized image.  Since
     * we're only interested in the full-sized image (in terms of metadata),
     * patch the EXIF metadata by copying the full-sized image dimensions from
     * the TIFF metadata (if present).
     *
     * @param tiffDir The TIFF directory to copy values from.
     * @param exifWidthTagID The EXIF image width tag ID.
     * @param exifHeightTagID The EXIF image height tag ID.
     */
    private void patchEXIFImageSize( ImageMetadataDirectory tiffDir,
                                     int exifWidthTagID, int exifHeightTagID ) {
        final ImageMetaValue tiffWidth  = tiffDir.getValue( TIFF_IMAGE_WIDTH  );
        final ImageMetaValue tiffHeight = tiffDir.getValue( TIFF_IMAGE_LENGTH );
        if ( tiffWidth == null || tiffHeight == null )
            return;

        final ImageMetadataDirectory exifDir =
            m_metadata.getDirectoryFor( EXIFDirectory.class );
        final ImageMetaValue exifWidth  = exifDir.getValue( exifWidthTagID  );
        final ImageMetaValue exifHeight = exifDir.getValue( exifHeightTagID );
        if ( exifWidth == null || exifHeight == null )
            return;

        final int width = tiffWidth.getIntValue();
        if ( width > exifWidth.getIntValue() ) {
            final int height = tiffHeight.getIntValue();
            ((LongMetaValue)exifWidth).setLongValueAt( width, 0 );
            ((LongMetaValue)exifHeight).setLongValueAt( height, 0 );
        }
    }

    /**
     * Read the metadata from a single directory.
     *
     * @param offset The offset from the beginning of the file of the
     * directory.
     * @param dir The metadata is put here.
     */
    private void readDirectory( int offset, ImageMetadataDirectory dir )
        throws IOException
    {
        int entryCount = m_buf.getUnsignedShort( offset );
        if ( entryCount > DIRECTORY_ENTRY_MAX_SANE_COUNT )
            entryCount = DIRECTORY_ENTRY_MAX_SANE_COUNT;
        for ( int entry = 0; entry < entryCount; ++entry ) {
            try {
                final int pos = calcIFDEntryOffset( offset, entry );
                readDirectoryEntry( pos, dir );
            }
            catch ( IOException e ) {
                throw e;
            }
            catch ( Exception e ) {
                logBadImageMetadata( e );
            }
        }
        //
        // Position the buffer immediately after the last entry so
        // readAllDirectories() can read the following offset to the next
        // directory, if any.
        //
        m_buf.position( calcIFDEntryOffset( offset, entryCount ) );
    }

    /**
     * Read the metadata from a single directory entry.
     *
     * @param offset The offset from the beginning of the file of the
     * directory entry.
     * @param dir The metadata is put here.
     */
    private void readDirectoryEntry( int offset, ImageMetadataDirectory dir )
        throws IOException
    {
        m_buf.position( offset );
        final int tagID = m_buf.getUnsignedShort();
        if ( tagID < 0 )
            return;
        final int fieldType = m_buf.getUnsignedShort();
        if ( fieldType <= 0 || fieldType >= TIFF_FIELD_SIZE.length ) {
            //
            // Unknown type: skip it.
            //
            return;
        }

        int numValues = m_buf.getInt();
        if ( numValues < 0 ) {
            logBadImageMetadata();
            return;
        }
        final int byteCount = numValues * TIFF_FIELD_SIZE[ fieldType ];
        if ( byteCount <= 0 )
            return;
        int valueOffset = calcValueOffset( byteCount );
        if ( valueOffset < 0 ) {
            logBadImageMetadata();
            return;
        }
        int subdirOffset = m_buf.getInt( valueOffset );

        if ( m_tagHandler != null ) {
            final boolean handledTag = m_tagHandler.handleTag(
                tagID, fieldType, numValues, byteCount, valueOffset, 0,
                subdirOffset, m_imageInfo, m_buf, dir
            );
            if ( handledTag )
                return;
        }

        switch ( tagID ) {
            case TIFF_EXIF_IFD_POINTER: {
                final EXIFMetadataReader reader = new EXIFMetadataReader(
                    m_imageInfo, m_buf, true
                );
                reader.setTagHandler( m_tagHandler );
                final ImageMetadataDirectory exifDir =
                    m_metadata.getDirectoryFor( EXIFDirectory.class, true );
                reader.readDirectory( subdirOffset, 0, exifDir );
                return;
            }
            case TIFF_GPS_IFD_POINTER: {
                final ImageMetadataDirectory gpsDir =
                    m_metadata.getDirectoryFor( GPSDirectory.class, true );
                readDirectory( subdirOffset, gpsDir );
                return;
            }
            case TIFF_RICH_TIFF_IPTC: {
                final byte[] iptcBuf = m_buf.getBytes( valueOffset, byteCount );
                final IPTCMetadataReader reader = new IPTCMetadataReader(
                    m_imageInfo, new ArrayByteBuffer( iptcBuf ),
                    TIFFImageType.INSTANCE
                );
                reader.readAllDirectories();
                return;
            }
            case TIFF_SUB_IFDS: {
                while ( true ) {
                    final ImageMetadataDirectory subDir = createDirectoryFor(
                        m_dirClass, "SubIFD" + m_subIFDIndex++
                    );
                    readDirectory( subdirOffset, subDir );
                    if ( --numValues == 0 )
                        break;
                    valueOffset += TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_IFD ];
                    subdirOffset = m_buf.getInt( valueOffset );
                }
                return;
            }
        }

        if ( byteCount > DIRECTORY_ENTRY_MAX_SANE_SIZE )
            return;
        final ImageMetaValue value = readValue(
            tagID, fieldType, valueOffset, numValues
        );
        if ( value != null )
            try {
                dir.putValue( tagID, value );
            }
            catch ( IllegalArgumentException e ) {
                logBadImageMetadata();
            }
    }

    /**
     * Read a {@link String} out of the TIFF metadata.
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
     * @param tagID The tag ID of the value.
     * @param fieldType The type of metadata.
     * @param offset The offset of the value.
     * @param numValues The number of values.
     * @return Returns an {@link ImageMetaValue} containing the IFD tag's
     * value.
     */
    private ImageMetaValue readValue( int tagID, int fieldType, int offset,
                                      int numValues ) throws IOException {
        switch ( fieldType ) {

            case TIFF_FIELD_TYPE_ASCII: {
                final String s = readString( offset, numValues );
                if ( s == null )
                    return null;
                switch ( tagID ) {
                    case TIFF_DATE_TIME:
                        //
                        // We elevate dates from mere Strings into Date types
                        // in their own right.
                        //
                        return new DateMetaValue( s );
                    default:
                        return new StringMetaValue( s );
                }
            }

            case TIFF_FIELD_TYPE_SBYTE: {
                final long[] values = new long[ numValues ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.get( offset + i );
                return new ByteMetaValue( values );
            }

            case TIFF_FIELD_TYPE_UBYTE: {
                final long[] values = new long[ numValues ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getUnsignedByte( offset + i );
                return new UnsignedByteMetaValue( values );
            }

            case TIFF_FIELD_TYPE_SLONG: {
                final long[] values = new long[ numValues ];
                final int valueSize = TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_SLONG ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getInt( offset + i * valueSize );
                return new LongMetaValue( values );
            }

            case TIFF_FIELD_TYPE_ULONG: {
                final long[] values = new long[ numValues ];
                final int valueSize = TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_ULONG ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getInt( offset + i * valueSize );
                return new UnsignedLongMetaValue( values );
            }

            case TIFF_FIELD_TYPE_SRATIONAL: {
                final Rational[] values = new Rational[ numValues ];
                final int valueSize =
                    TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_SRATIONAL ];
                final int longSize = TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_SLONG ];
                for ( int i = 0; i < numValues; ++i )
                    try {
                        final int pos = offset + i * valueSize;
                        values[i] = new Rational(
                            m_buf.getInt( pos ), m_buf.getInt( pos + longSize )
                        );
                    }
                    catch ( IllegalArgumentException e ) {
                        logBadImageMetadata();
                        return null;
                    }
                return new RationalMetaValue( values );
            }

            case TIFF_FIELD_TYPE_URATIONAL: {
                final Rational[] values = new Rational[ numValues ];
                final int valueSize =
                    TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_URATIONAL ];
                final int longSize = TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_ULONG ];
                for ( int i = 0; i < numValues; ++i )
                    try {
                        final int pos = offset + i * valueSize;
                        values[i] = new Rational(
                            m_buf.getInt( pos ), m_buf.getInt( pos + longSize )
                        );
                    }
                    catch ( IllegalArgumentException e ) {
                        logBadImageMetadata();
                        return null;
                    }
                return new UnsignedRationalMetaValue( values );
            }

            case TIFF_FIELD_TYPE_SSHORT: {
                final long[] values = new long[ numValues ];
                final int valueSize = TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_SSHORT ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getShort( offset + i * valueSize );
                return new ShortMetaValue( values );
            }

            case TIFF_FIELD_TYPE_USHORT: {
                final long[] values = new long[ numValues ];
                final int valueSize = TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_USHORT ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getUnsignedShort( offset + i * valueSize );
                return new UnsignedShortMetaValue( values );
            }

            case TIFF_FIELD_TYPE_FLOAT: {
                final float[] values = new float[ numValues ];
                final int valueSize = TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_FLOAT ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getFloat( offset + i * valueSize );
                return new FloatMetaValue( values );
            }

            case TIFF_FIELD_TYPE_DOUBLE: {
                final double[] values = new double[ numValues ];
                final int valueSize = TIFF_FIELD_SIZE[ TIFF_FIELD_TYPE_DOUBLE ];
                for ( int i = 0; i < numValues; ++i )
                    values[i] = m_buf.getDouble( offset + i * valueSize );
                return new DoubleMetaValue( values );
            }

            case TIFF_FIELD_TYPE_UNDEFINED:
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
     * Copy metadata values from a TIFF {@link ImageMetadataDirectory} that is
     * not for the full-sized image, e.g., a preview or a thumbnail, to the
     * TIFF {@link ImageMetadataDirectory} for the full-sized image.
     * <p>
     * Note that the copy is a partial copy, i.e., it doesn't copy all tags.
     * It only copies only those that could be useful for the full-sized image
     * (like ARTIST, MAKE, and MODEL), i.e., tags that aren't specific to the
     * non-full-sized image.
     *
     * @param fromDir The {@link ImageMetadataDirectory} to copy values from.
     * @param toDir The {@link ImageMetadataDirectory} to copy values to.
     */
    private static void copyValuesFromTo( ImageMetadataDirectory fromDir,
                                          ImageMetadataDirectory toDir ) {
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i = fromDir.iterator();
              i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            final int tagID = me.getKey();
            switch ( tagID ) {
                case TIFF_BITS_PER_SAMPLE:
                case TIFF_COMPRESSION:
                case TIFF_ICC_PROFILE:
                case TIFF_IMAGE_LENGTH:
                case TIFF_IMAGE_WIDTH:
                case TIFF_NEW_SUBFILE_TYPE:
                case TIFF_PHOTOMETRIC_INTERPRETATION:
                case TIFF_PLANAR_CONFIGURATION:
                case TIFF_PREDICTOR:
                case TIFF_PRIMARY_CHROMATICITIES:
                case TIFF_REFERENCE_BLACK_WHITE:
                case TIFF_ROWS_PER_STRIP:
                case TIFF_SAMPLE_FORMAT:
                case TIFF_SAMPLES_PER_PIXEL:
                case TIFF_SMAX_SAMPLE_VALUE:
                case TIFF_SMIN_SAMPLE_VALUE:
                case TIFF_STRIP_BYTE_COUNTS:
                case TIFF_STRIP_OFFSETS:
                case TIFF_SUB_IFDS:
                case TIFF_SUBFILE_TYPE:
                case TIFF_TILE_BYTE_COUNTS:
                case TIFF_TILE_LENGTH:
                case TIFF_TILE_OFFSETS:
                case TIFF_TILE_WIDTH:
                case TIFF_TRANSFER_FUNCTION:
                case TIFF_TRANSFER_RANGE:
                case TIFF_X_POSITION:
                case TIFF_X_RESOLUTION:
                case TIFF_YCBCR_COEFFICIENTS:
                case TIFF_YCBCR_POSITIONING:
                case TIFF_YCBCR_SUBSAMPLING:
                case TIFF_Y_POSITION:
                case TIFF_Y_RESOLUTION:
                    continue;
                default:
                    final ImageMetaValue toValue = toDir.getValue( tagID );
                    if ( toValue == null )
                        toDir.putValue( tagID, me.getValue() );
            }
        }
    }

    /**
     * The class of the {@link ImageMetadataDirectory} to create.
     */
    private final Class<? extends ImageMetadataDirectory> m_dirClass;

    /**
     * A map of directory names (e.g., "IFD0", "SubIFD1") to directories.
     */
    private final Map<String,ImageMetadataDirectory> m_dirMap =
        new HashMap<String,ImageMetadataDirectory>();

    /**
     * A sequential index used to form the names of the sub IFDs.
     */
    private int m_subIFDIndex;
}
/* vim:set et sw=4 ts=4: */
