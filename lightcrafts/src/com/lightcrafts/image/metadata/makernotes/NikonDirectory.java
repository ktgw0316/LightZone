/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.ImageMetaTagInfo;
import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.image.metadata.ImageMetaTags;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.image.types.NEFImageType;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.NumberUtil;
import com.lightcrafts.utils.TextUtil;

import static com.lightcrafts.image.metadata.makernotes.NikonConstants.*;
import static com.lightcrafts.image.metadata.makernotes.NikonTags.*;
import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * A <code>NikonDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding Nikon-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class NikonDirectory extends MakerNotesDirectory implements
    FocalLengthProvider, ISOProvider, PreviewImageProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFocalLength() {
        final ImageMetaValue value = getValue( NIKON_LD21_FOCAL_LENGTH );
        return value != null ? value.getFloatValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getISO() {
        ImageMetaValue value = getValue( NIKON_ISO );
        if ( value == null )
            value = getValue( NIKON_ISO_D70 );
        if ( value == null )
            value = getValue( NIKON_ISO_D70_2 );
        if ( value == null )
            value = getValue( NIKON_II_ISO2 );
        if ( value == null )
            value = getValue( NIKON_II_ISO );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLens() {
        final ImageMetaValue value = getValue(NIKON_LENS);
        if (value != null) {
            final String name = valueToString(value);
            if (name != null) {
                return name;
            }
        }
        return super.getLens();
    }

    /**
     * Gets the maker-notes adjustments for Nikon.
     *
     * @param buf The {@link LCByteBuffer} the metadata is in.
     * @param offset The offset to adjust.
     * @return If the maker-notes are either versions 1 or 2, returns said
     * adjustments; otherwise returns <code>null</code>.
     */
    @Override
    public int[] getMakerNotesAdjustments( LCByteBuffer buf, int offset )
        throws IOException
    {
        final byte[] header = buf.getBytes( offset, 7 );
        if ( Arrays.equals( header, "Nikon\0\2".getBytes( "ASCII" ) ) )
            return new int[]{
                NIKON_MAKER_NOTES_HEADER_SIZE + TIFF_HEADER_SIZE,
                offset + NIKON_MAKER_NOTES_HEADER_SIZE
            };
        else if ( Arrays.equals( header, "Nikon\0\1".getBytes( "ASCII" ) ) )
            return new int[]{ TIFF_HEADER_SIZE, 0 };
        return null;
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;Nikon&quot;.
     */
    @Override
    public String getName() {
        return "Nikon";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return NEFImageType.INSTANCE.getPreviewImage(
            imageInfo, maxWidth, maxHeight
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageMetaTagInfo getTagInfoFor( Integer id ) {
        return m_tagsByID.get( id );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageMetaTagInfo getTagInfoFor( String name ) {
        return m_tagsByName.get( name );
    }

    /**
     * Puts a key/value pair into this directory.  Additionally, handle all the
     * special cases for Nikon.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The value to put.
     */
    @Override
    public void putValue( Integer tagID, ImageMetaValue value ) {
switch_tagID:
        switch ( tagID ) {
            case NIKON_ISO:
            case NIKON_ISO_D70:
            case NIKON_ISO_D70_2: {
                if ( !(value instanceof UnsignedShortMetaValue) ) {
                    //
                    // Apparently some Nikon cameras use the ISO tag values for
                    // other things, e.g., the Nikon Coolpix 3500 has the tag
                    // for NIKON_ISO_D70 (0x000F) as a string.  Since we
                    // currently don't know what to do with such cases, ignore
                    // the metadata.
                    //
                    return;
                }
                final long[] values = ((LongMetaValue)value).getLongValues();
                if ( values.length > 1 ) {
                    //
                    // The ISO value for some Nikon cameras is actually 2
                    // integers.  The first always seems to be 0 so remove it.
                    //
                    value = new UnsignedShortMetaValue( (int)values[1] );
                }
                break;
            }
            case NIKON_ISO_INFO: {
                final byte[] data = ((UndefinedMetaValue)value).getUndefinedValue();
                explodeSubfields( tagID, data, 0 );
                return;
            }
            case NIKON_II_ISO:
            case NIKON_II_ISO2: {
                final double n = value.getIntValue();
                final int iso = (int)(100 * Math.pow( 2, n / 12 - 5 ) + 0.5);
                if ( iso <= 0 )
                    return;
                value = new UnsignedLongMetaValue( iso );
                break;
            }
            case NIKON_LD21_AF_APERTURE:
            case NIKON_LD21_APERTURE_AT_MIN_FOCAL:
            case NIKON_LD21_APERTURE_AT_MAX_FOCAL:
            case NIKON_LD21_EFFECTIVE_MAX_APERTURE: {
                final int n = value.getUnsignedByteValue();
                value = new FloatMetaValue(
                    (float)NumberUtil.tenths( Math.pow( 2, n / 24.0 ) )
                );
                break;
            }
            case NIKON_LD21_FOCAL_LENGTH:
            case NIKON_LD1X_MIN_FOCAL_LENGTH:
            case NIKON_LD1X_MAX_FOCAL_LENGTH: {
                final int n = value.getUnsignedByteValue();
                value = new FloatMetaValue(
                    (float)NumberUtil.tenths( 5 * Math.pow( 2, n / 24.0 ) )
                );
                break;
            }
            case NIKON_LD21_FOCUS_DISTANCE: {
                //
                // Apparently, the Nikon D70 (at least) already has this as a
                // string, so make sure the value is numeric before continuing.
                //
                if ( value instanceof UnsignedByteMetaValue ) {
                    final int n = value.getUnsignedByteValue();
                    value = new StringMetaValue(
                        TextUtil.tenths( Math.pow( 10, n / 40.0 ) ) + "cm"
                    );
                }
                break;
            }
            case NIKON_LD21_LENS_FSTOPS: {
                final int n = value.getUnsignedByteValue();
                value = new FloatMetaValue(
                    (float)NumberUtil.tenths( n / 12.0 )
                );
                break;
            }
            case NIKON_LENS_DATA: {
                final byte[] data =
                    ((UndefinedMetaValue)value).getUndefinedValue();
                final byte[] versionBuf = new byte[4];
                System.arraycopy( data, 0, versionBuf, 0, 4 );
                final int version =
                    versionBuf[0] - '0' << 12 |
                    versionBuf[1] - '0' <<  8 |
                    versionBuf[2] - '0' <<  4 |
                    versionBuf[3] - '0';
                super.putValue(
                    NIKON_LD_VERSION, new UnsignedShortMetaValue( version )
                );
                switch ( version ) {
                    case 0x0100:
                    case 0x0101:
                        //
                        // Since we support two versions of Nikon lens data, we
                        // use NIKON_LENS_DATA << 8 | 0x10 for these versions.
                        //
                        explodeSubfields( tagID << 8 | 0x10, data, 4 );
                        break;
                    case 0x0201:
                    case 0x0204:
                        //
                        // These versions of lens data are encrypted. To decrypt
                        // it, the serial-number and shutter-count metadata are
                        // needed but they might not have been encountered yet
                        // so just increment a counter for now.
                        //
                        ++m_decryptCount;
                        break switch_tagID;
                }
                return;
            }
            case NIKON_LENS_TYPE: {
                //
                // Apparently, the Nikon D70 (at least) already has this as a
                // string, so make sure the value is numeric before continuing.
                //
                if ( value instanceof UnsignedByteMetaValue ) {
                    //
                    // Replace the lens type's bit value with an array of
                    // labels, one for each '1' bit.
                    //
                    final int lensType = value.getUnsignedByteValue();
                    final String[] labels = explodeBits( tagID, lensType );
                    value = new StringMetaValue( labels );
                }
                break;
            }
            case NIKON_SERIAL_NUMBER:
            case NIKON_SHUTTER_COUNT:
                ++m_decryptCount;
                break;
            case NIKON_SHOOTING_MODE: {
                //
                // Apparently, the Nikon D70 (at least) already has this as a
                // string, so make sure the value is numeric before continuing.
                //
                if ( value.isNumeric() ) {
                    //
                    // Replace the shooting mode's bit value with an array of
                    // labels, one for each '1' bit.
                    //
                    int shootingMode = value.getIntValue();
                    if ( (shootingMode & 0x87) == 0 ) {
                        //
                        // The ShootingMode is complicated.  Rather than try to
                        // explain it myself, here's the original comment taken
                        // from the ExifTool's Nikon.pm file:
                        //
                        //      The (new?) bit 5 seriously complicates our life
                        //      here: after firmware B's 1.03, bit 5 turns on
                        //      when you ask for BUT DO NOT USE the long-range
                        //      noise reduction feature, probably because even
                        //      not using it, it still slows down your drive
                        //      operation to 50% (1.5fps max not 3fps).  But no
                        //      longer does !$val alone indicate single-frame
                        //      operation.
                        //
                        if ( shootingMode == 0 )
                            shootingMode = 1 << 3;  // bit 3 = "single frame"
                        else
                            shootingMode |= 1 << 3;
                    }
                    final String[] labels = explodeBits( tagID, shootingMode );
                    value = new StringMetaValue( labels );
                }
                break;
            }
        }
        super.putValue( tagID, value );

        if ( m_decryptCount == 3 ) {
            //
            // We've got all the pieces needed to decrypt the encrypted lens
            // data so do it now.
            //
            final ImageMetaValue lensDataValue = removeValue(NIKON_LENS_DATA);
            final ImageMetaValue serialNumberValue = getValue(NIKON_SERIAL_NUMBER);
            final ImageMetaValue shutterCountValue = getValue(NIKON_SHUTTER_COUNT);
            if (lensDataValue != null && serialNumberValue != null
                    && shutterCountValue != null) {
                final byte[] lensData =
                        ((UndefinedMetaValue) lensDataValue).getUndefinedValue();
                final long serialNumber = serialNumberValue.getLongValue();
                final long shutterCount = shutterCountValue.getLongValue();
                decrypt(lensData, 4, serialNumber, shutterCount);
                m_decryptCount = Integer.MIN_VALUE; // never do this "if" again
                //
                // Since we support two version of Nikon lens data, we use
                // NIKON_LENS_DATA << 8 | 0x21 for this version.
                //
                // TODO: support tag version 0x0204
                explodeSubfields(NIKON_LENS_DATA << 8 | 0x21, lensData, 4);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case NIKON_CROP_HIGH_SPEED: {
                if ( !(value instanceof LongMetaValue) )
                    return "?";
                final long[] v = ((LongMetaValue)value).getLongValues();
                if ( v.length != 7 )
                    return "?";
                if ( v[0] == 0 )
                    return getTagValueLabelFor( NIKON_CROP_HIGH_SPEED, v[0] );
                return  v[1] + 'x' + v[2] + " -> " +
                        v[3] + 'x' + v[4] + " @ " +
                        v[5] + ',' + v[6];
            }
            case NIKON_FLASH_EXPOSURE_BRACKET_VALUE:
            case NIKON_FLASH_EXPOSURE_COMPENSATION: {
                if ( !(value instanceof LongMetaValue) )
                    return "?";
                final long n = value.getLongValue() >>> 24;
                return TextUtil.tenths( n / 6.0 );
            }
            case NIKON_LD21_FOCAL_LENGTH:
                return value.getStringValue() + "mm";   // TODO: localize "mm"
            case NIKON_LD1X_LENS_ID:
            case NIKON_LD21_LENS_ID: {
                final String label = hasTagValueLabelFor( NIKON_LD1X_LENS_ID );
                return label != null ? label : "unknown"; // TODO: localize
            }
            case NIKON_LD1X_MAX_FOCAL_LENGTH:
            case NIKON_LD1X_MIN_FOCAL_LENGTH:
            case NIKON_LD21_MAX_FOCAL_LENGTH:
            case NIKON_LD21_MIN_FOCAL_LENGTH: {
                final int n = value.getUnsignedByteValue();
                return TextUtil.tenths( 5 * Math.pow( 2, n / 24.0 ) ) + "mm"; // TODO: localize
            }
            case NIKON_LENS: {
                final String lensLabel = makeLensLabelFrom( value );
                if ( lensLabel != null )
                    return lensLabel;
                break;
            }
            case NIKON_LENS_FSTOPS: {
                if ( !(value instanceof UndefinedMetaValue) )
                    return "?";
                final byte[] b = ((UndefinedMetaValue)value).getUndefinedValue();
                final float f = b[2] != 0 ? b[0] * (b[1] / (float)b[2]) : 0;
                return TextUtil.tenths( f );
            }
        }
        return super.valueToString( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImageMetaValue getLensNamesValue() {
        //
        // Here, we always use the NIKON_LD1X_LENS_ID tag ID because the
        // resources file doesn't have the lens labels duplicated for
        // NIKON_LD21_LENS_ID.  This is done to eliminate redundancy since the
        // labels are the same for both versions.
        //
        return getValue( NIKON_LD1X_LENS_ID );
    }

    @Override
    protected ImageMetaValue getLongFocalValue() {
        return getLensData( NIKON_LD1X_MAX_FOCAL_LENGTH,
                            NIKON_LD21_MAX_FOCAL_LENGTH,
                            NIKON_LD24_MAX_FOCAL_LENGTH );
    }

    @Override
    protected ImageMetaValue getShortFocalValue() {
        return getLensData( NIKON_LD1X_MIN_FOCAL_LENGTH,
                            NIKON_LD21_MIN_FOCAL_LENGTH,
                            NIKON_LD24_MIN_FOCAL_LENGTH );
    }

    @Override
    protected ImageMetaValue getMaxApertureValue() {
        return getLensData( NIKON_LD1X_APERTURE_AT_MAX_FOCAL,
                            NIKON_LD21_EFFECTIVE_MAX_APERTURE,
                            NIKON_LD24_EFFECTIVE_MAX_APERTURE );
    }

    /**
     * Gets the priority of this directory for providing the metadata supplied
     * by implementing the given provider interface.
     * <p>
     * The priority for {@link ShutterSpeedProvider} for Nikon is the lowest
     * because it yields weird values.
     *
     * @param provider The provider interface to get the priority for.
     * @return Returns said priority.
     */
    @Override
    protected int getProviderPriorityFor(
        Class<? extends ImageMetadataProvider> provider )
    {
        return (provider == FocalLengthProvider.class)
                ? PROVIDER_PRIORITY_MIN
                : super.getProviderPriorityFor( provider );
    }

    /**
     * Get the {@link ResourceBundle} to use for tags.
     *
     * @return Returns said {@link ResourceBundle}.
     */
    @Override
    protected ResourceBundle getTagLabelBundle() {
        return m_tagBundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends ImageMetaTags> getTagsInterface() {
        return NikonTags.class;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Add the tag mappings.
     *
     * @param id The tag's ID.
     * @param name The tag's name.
     * @param type The tag's {@link ImageMetaType}.
     */
    private static void add( int id, String name, ImageMetaType type ) {
        final ImageMetaTagInfo tagInfo =
            new ImageMetaTagInfo( id, name, type, false );
        m_tagsByID.put( id, tagInfo );
        m_tagsByName.put( name, tagInfo );
    }

    /**
     * Count the number of '1' bits in an integer.
     *
     * @param n The integer.
     * @return Returns the number of '1' bits.
     */
    private static int count1BitsOf( int n ) {
        int count = 0;
        while ( n != 0 ) {
            ++count;
            n &= n - 1;
        }
        return count;
    }

    /**
     * Decrypt an encrypted data block.
     *
     * @param buf The array containing the binary data to be decrypted.
     * @param offset The offset into <code>buf</code> at which to start.
     * @param serialNumber The camera serial number.
     * @param shutterCount The number of photos that have been taken by the
     * camera.
     * @see <a href="http://owl.phy.queensu.ca/~phil/exiftool/">ExifTool</a>.
     */
    private static void decrypt( byte[] buf, int offset, long serialNumber,
                                 long shutterCount ) {
        //
        // Below is the original Perl code taken from ExifTool:
        // lib/Image/ExifTool/Nikon.pm Decrypt()
        //
/*
        my ($dataPt, $serial, $count, $start, $len) = @_;
        $start or $start = 0;
        my $end = $len ? $start + $len : length($$dataPt);
        my $i;
        my $key = 0;
        for ($i=0; $i<4; ++$i) {
            $key ^= ($count >> ($i*8)) & 0xff;
        }
        my $ci = $xlat[0][$serial & 0xff];
        my $cj = $xlat[1][$key];
        my $ck = 0x60;
        my @data = unpack('C*',$$dataPt);
        for ($i=$start; $i<$end; ++$i) {
            $cj = ($cj + $ci * $ck) & 0xff;
            $ck = ($ck + 1) & 0xff;
            $data[$i] ^= $cj;
        }
        return pack('C*',@data);
*/
        int key = 0;
        for ( int i = 0; i < 4; ++i )
            key ^= (shutterCount >>> i * 8) & 0xFF;

        final int ci = m_decrypt[0][ (int)(serialNumber & 0xFF) ] & 0xFF;
        int cj = m_decrypt[1][ key ] & 0xFF;
        int ck = 0x60;
        for ( int i = offset; i < buf.length; ++i ) {
            cj = (cj + ci * ck) & 0xFF;
            ck = (ck + 1) & 0xFF;
            buf[i] ^= cj;
        }
    }

    /**
     * &quot;Explode&quot; a tag's value that is a set of bits into an array of
     * strings where each element is the label for the corresponding bit.
     *
     * @param tagID The metadata tag ID.
     * @param bits The bits to explode.
     * @return Returns said array.
     */
    private String[] explodeBits( int tagID, int bits ) {
        final int num1Bits = count1BitsOf( bits );
        final String[] labels = new String[ num1Bits ];
        for ( int bit = 0, i = 0; bit < 32; ++bit ) {
            if ( (bits & (1 << bit)) != 0 ) {
                //noinspection AssignmentToForLoopParameter
                labels[i++] = getTagValueLabelFor( tagID, bit );
            }
        }
        return labels;
    }

    /**
     * &quot;Explode&quot; a tag's value that has subfields into individual
     * {@link ImageMetaValue}s.
     *
     * @param tagID The tag ID of the field to be exploded.
     * @param buf The array containing the binary data to be exploded.
     * @param offset The offset into <code>buf</code> at which to start.
     */
    private void explodeSubfields( int tagID, byte[] buf, int offset ) {
        tagID <<= 8;
        tagID += offset;
        for ( int i = offset; i < buf.length; ++i )
            putValue( tagID++, new UnsignedByteMetaValue( buf[i] & 0xFF ) );
    }

    /**
     * Get lens metadata value depending on the version of the lens metadata.
     *
     * @param tag0100 The tag ID for versions 0x0100 and 0x0101.
     * @param tag0201 The tag ID for version 0x0201.
     * @param tag0204 The tag ID for version 0x0204.
     * @return Returns the metadata or <code>null</code> if there is no such
     * metadata.
     */
    private ImageMetaValue getLensData( int tag0100, int tag0201, int tag0204 ) {
        final ImageMetaValue version = getValue( NIKON_LD_VERSION );
        if ( version != null )
            switch ( version.getUnsignedShortValue() ) {
                case 0x0100: // D100, D1X
                case 0x0101: // D70, D70s
                    return getValue( tag0100 );
                case 0x0201: // D200, D2Hs, D2X, D2Xs
                case 0x0202: // D40, D40X, D80
                case 0x0203: // D300
                    return getValue( tag0201 );
                case 0x0204: // D90, D7000
                    return getValue( tag0204 );
                case 0x0400: // 1J1, 1V1
                    // TODO:
                default:
                    break;
            }
        return null;
    }

    /**
     * A counter used to know when an encrypted data block can be decrypted,
     * i.e., when all needed metadata has been obtained.
     * @see #decrypt(byte[],int,long,long shutterCount)
     */
    private int m_decryptCount;

    /**
     * A two-dimensional table of data used to decrypt encrypted data blocks.
     * @see #decrypt(byte[],int,long,long shutterCount)
     */
    private static final byte[][] m_decrypt = {
        {
            (byte)0xc1, (byte)0xbf, (byte)0x6d, (byte)0x0d, (byte)0x59,
            (byte)0xc5, (byte)0x13, (byte)0x9d, (byte)0x83, (byte)0x61,
            (byte)0x6b, (byte)0x4f, (byte)0xc7, (byte)0x7f, (byte)0x3d,
            (byte)0x3d, (byte)0x53, (byte)0x59, (byte)0xe3, (byte)0xc7,
            (byte)0xe9, (byte)0x2f, (byte)0x95, (byte)0xa7, (byte)0x95,
            (byte)0x1f, (byte)0xdf, (byte)0x7f, (byte)0x2b, (byte)0x29,
            (byte)0xc7, (byte)0x0d, (byte)0xdf, (byte)0x07, (byte)0xef,
            (byte)0x71, (byte)0x89, (byte)0x3d, (byte)0x13, (byte)0x3d,
            (byte)0x3b, (byte)0x13, (byte)0xfb, (byte)0x0d, (byte)0x89,
            (byte)0xc1, (byte)0x65, (byte)0x1f, (byte)0xb3, (byte)0x0d,
            (byte)0x6b, (byte)0x29, (byte)0xe3, (byte)0xfb, (byte)0xef,
            (byte)0xa3, (byte)0x6b, (byte)0x47, (byte)0x7f, (byte)0x95,
            (byte)0x35, (byte)0xa7, (byte)0x47, (byte)0x4f, (byte)0xc7,
            (byte)0xf1, (byte)0x59, (byte)0x95, (byte)0x35, (byte)0x11,
            (byte)0x29, (byte)0x61, (byte)0xf1, (byte)0x3d, (byte)0xb3,
            (byte)0x2b, (byte)0x0d, (byte)0x43, (byte)0x89, (byte)0xc1,
            (byte)0x9d, (byte)0x9d, (byte)0x89, (byte)0x65, (byte)0xf1,
            (byte)0xe9, (byte)0xdf, (byte)0xbf, (byte)0x3d, (byte)0x7f,
            (byte)0x53, (byte)0x97, (byte)0xe5, (byte)0xe9, (byte)0x95,
            (byte)0x17, (byte)0x1d, (byte)0x3d, (byte)0x8b, (byte)0xfb,
            (byte)0xc7, (byte)0xe3, (byte)0x67, (byte)0xa7, (byte)0x07,
            (byte)0xf1, (byte)0x71, (byte)0xa7, (byte)0x53, (byte)0xb5,
            (byte)0x29, (byte)0x89, (byte)0xe5, (byte)0x2b, (byte)0xa7,
            (byte)0x17, (byte)0x29, (byte)0xe9, (byte)0x4f, (byte)0xc5,
            (byte)0x65, (byte)0x6d, (byte)0x6b, (byte)0xef, (byte)0x0d,
            (byte)0x89, (byte)0x49, (byte)0x2f, (byte)0xb3, (byte)0x43,
            (byte)0x53, (byte)0x65, (byte)0x1d, (byte)0x49, (byte)0xa3,
            (byte)0x13, (byte)0x89, (byte)0x59, (byte)0xef, (byte)0x6b,
            (byte)0xef, (byte)0x65, (byte)0x1d, (byte)0x0b, (byte)0x59,
            (byte)0x13, (byte)0xe3, (byte)0x4f, (byte)0x9d, (byte)0xb3,
            (byte)0x29, (byte)0x43, (byte)0x2b, (byte)0x07, (byte)0x1d,
            (byte)0x95, (byte)0x59, (byte)0x59, (byte)0x47, (byte)0xfb,
            (byte)0xe5, (byte)0xe9, (byte)0x61, (byte)0x47, (byte)0x2f,
            (byte)0x35, (byte)0x7f, (byte)0x17, (byte)0x7f, (byte)0xef,
            (byte)0x7f, (byte)0x95, (byte)0x95, (byte)0x71, (byte)0xd3,
            (byte)0xa3, (byte)0x0b, (byte)0x71, (byte)0xa3, (byte)0xad,
            (byte)0x0b, (byte)0x3b, (byte)0xb5, (byte)0xfb, (byte)0xa3,
            (byte)0xbf, (byte)0x4f, (byte)0x83, (byte)0x1d, (byte)0xad,
            (byte)0xe9, (byte)0x2f, (byte)0x71, (byte)0x65, (byte)0xa3,
            (byte)0xe5, (byte)0x07, (byte)0x35, (byte)0x3d, (byte)0x0d,
            (byte)0xb5, (byte)0xe9, (byte)0xe5, (byte)0x47, (byte)0x3b,
            (byte)0x9d, (byte)0xef, (byte)0x35, (byte)0xa3, (byte)0xbf,
            (byte)0xb3, (byte)0xdf, (byte)0x53, (byte)0xd3, (byte)0x97,
            (byte)0x53, (byte)0x49, (byte)0x71, (byte)0x07, (byte)0x35,
            (byte)0x61, (byte)0x71, (byte)0x2f, (byte)0x43, (byte)0x2f,
            (byte)0x11, (byte)0xdf, (byte)0x17, (byte)0x97, (byte)0xfb,
            (byte)0x95, (byte)0x3b, (byte)0x7f, (byte)0x6b, (byte)0xd3,
            (byte)0x25, (byte)0xbf, (byte)0xad, (byte)0xc7, (byte)0xc5,
            (byte)0xc5, (byte)0xb5, (byte)0x8b, (byte)0xef, (byte)0x2f,
            (byte)0xd3, (byte)0x07, (byte)0x6b, (byte)0x25, (byte)0x49,
            (byte)0x95, (byte)0x25, (byte)0x49, (byte)0x6d, (byte)0x71,
            (byte)0xc7
        },
        {
            (byte)0xa7, (byte)0xbc, (byte)0xc9, (byte)0xad, (byte)0x91,
            (byte)0xdf, (byte)0x85, (byte)0xe5, (byte)0xd4, (byte)0x78,
            (byte)0xd5, (byte)0x17, (byte)0x46, (byte)0x7c, (byte)0x29,
            (byte)0x4c, (byte)0x4d, (byte)0x03, (byte)0xe9, (byte)0x25,
            (byte)0x68, (byte)0x11, (byte)0x86, (byte)0xb3, (byte)0xbd,
            (byte)0xf7, (byte)0x6f, (byte)0x61, (byte)0x22, (byte)0xa2,
            (byte)0x26, (byte)0x34, (byte)0x2a, (byte)0xbe, (byte)0x1e,
            (byte)0x46, (byte)0x14, (byte)0x68, (byte)0x9d, (byte)0x44,
            (byte)0x18, (byte)0xc2, (byte)0x40, (byte)0xf4, (byte)0x7e,
            (byte)0x5f, (byte)0x1b, (byte)0xad, (byte)0x0b, (byte)0x94,
            (byte)0xb6, (byte)0x67, (byte)0xb4, (byte)0x0b, (byte)0xe1,
            (byte)0xea, (byte)0x95, (byte)0x9c, (byte)0x66, (byte)0xdc,
            (byte)0xe7, (byte)0x5d, (byte)0x6c, (byte)0x05, (byte)0xda,
            (byte)0xd5, (byte)0xdf, (byte)0x7a, (byte)0xef, (byte)0xf6,
            (byte)0xdb, (byte)0x1f, (byte)0x82, (byte)0x4c, (byte)0xc0,
            (byte)0x68, (byte)0x47, (byte)0xa1, (byte)0xbd, (byte)0xee,
            (byte)0x39, (byte)0x50, (byte)0x56, (byte)0x4a, (byte)0xdd,
            (byte)0xdf, (byte)0xa5, (byte)0xf8, (byte)0xc6, (byte)0xda,
            (byte)0xca, (byte)0x90, (byte)0xca, (byte)0x01, (byte)0x42,
            (byte)0x9d, (byte)0x8b, (byte)0x0c, (byte)0x73, (byte)0x43,
            (byte)0x75, (byte)0x05, (byte)0x94, (byte)0xde, (byte)0x24,
            (byte)0xb3, (byte)0x80, (byte)0x34, (byte)0xe5, (byte)0x2c,
            (byte)0xdc, (byte)0x9b, (byte)0x3f, (byte)0xca, (byte)0x33,
            (byte)0x45, (byte)0xd0, (byte)0xdb, (byte)0x5f, (byte)0xf5,
            (byte)0x52, (byte)0xc3, (byte)0x21, (byte)0xda, (byte)0xe2,
            (byte)0x22, (byte)0x72, (byte)0x6b, (byte)0x3e, (byte)0xd0,
            (byte)0x5b, (byte)0xa8, (byte)0x87, (byte)0x8c, (byte)0x06,
            (byte)0x5d, (byte)0x0f, (byte)0xdd, (byte)0x09, (byte)0x19,
            (byte)0x93, (byte)0xd0, (byte)0xb9, (byte)0xfc, (byte)0x8b,
            (byte)0x0f, (byte)0x84, (byte)0x60, (byte)0x33, (byte)0x1c,
            (byte)0x9b, (byte)0x45, (byte)0xf1, (byte)0xf0, (byte)0xa3,
            (byte)0x94, (byte)0x3a, (byte)0x12, (byte)0x77, (byte)0x33,
            (byte)0x4d, (byte)0x44, (byte)0x78, (byte)0x28, (byte)0x3c,
            (byte)0x9e, (byte)0xfd, (byte)0x65, (byte)0x57, (byte)0x16,
            (byte)0x94, (byte)0x6b, (byte)0xfb, (byte)0x59, (byte)0xd0,
            (byte)0xc8, (byte)0x22, (byte)0x36, (byte)0xdb, (byte)0xd2,
            (byte)0x63, (byte)0x98, (byte)0x43, (byte)0xa1, (byte)0x04,
            (byte)0x87, (byte)0x86, (byte)0xf7, (byte)0xa6, (byte)0x26,
            (byte)0xbb, (byte)0xd6, (byte)0x59, (byte)0x4d, (byte)0xbf,
            (byte)0x6a, (byte)0x2e, (byte)0xaa, (byte)0x2b, (byte)0xef,
            (byte)0xe6, (byte)0x78, (byte)0xb6, (byte)0x4e, (byte)0xe0,
            (byte)0x2f, (byte)0xdc, (byte)0x7c, (byte)0xbe, (byte)0x57,
            (byte)0x19, (byte)0x32, (byte)0x7e, (byte)0x2a, (byte)0xd0,
            (byte)0xb8, (byte)0xba, (byte)0x29, (byte)0x00, (byte)0x3c,
            (byte)0x52, (byte)0x7d, (byte)0xa8, (byte)0x49, (byte)0x3b,
            (byte)0x2d, (byte)0xeb, (byte)0x25, (byte)0x49, (byte)0xfa,
            (byte)0xa3, (byte)0xaa, (byte)0x39, (byte)0xa7, (byte)0xc5,
            (byte)0xa7, (byte)0x50, (byte)0x11, (byte)0x36, (byte)0xfb,
            (byte)0xc6, (byte)0x67, (byte)0x4a, (byte)0xf5, (byte)0xa5,
            (byte)0x12, (byte)0x65, (byte)0x7e, (byte)0xb0, (byte)0xdf,
            (byte)0xaf, (byte)0x4e, (byte)0xb3, (byte)0x61, (byte)0x7f,
            (byte)0x2f
        }
    };

    /**
     * This is where the actual labels for the Nikon tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.makernotes.NikonTags"
    );

    /**
     * A mapping of tags by ID.
     */
    private static final Map<Integer,ImageMetaTagInfo> m_tagsByID =
        new HashMap<Integer,ImageMetaTagInfo>();

    /**
     * A mapping of tags by name.
     */
    private static final Map<String,ImageMetaTagInfo> m_tagsByName =
        new HashMap<String,ImageMetaTagInfo>();

    static {
        add( NIKON_AF_POINT, "AFPoint", META_ULONG );
        add( NIKON_AF_RESPONSE, "AFResponse", META_STRING );
        add( NIKON_AUTO_BRACKET_RELEASE, "AutoBracketRelease", META_USHORT );
        add( NIKON_AUXILIARY_LENS, "AuxiliaryLens", META_STRING );
        add( NIKON_COLOR_HUE, "ColorHue", META_STRING );
        add( NIKON_COLOR_MODE, "ColorMode", META_STRING );
        add( NIKON_COLOR_SPACE, "ColorSpace", META_USHORT );
        add( NIKON_CROP_HIGH_SPEED, "CropHighSpeed", META_STRING );
        add( NIKON_DIGITAL_ZOOM, "DigitalZoom", META_URATIONAL );
        add( NIKON_EXPOSURE_BRACKET_VALUE, "ExposureBracketValue", META_URATIONAL );
        add( NIKON_EXPOSURE_DIFFERENCE, "ExposureDifference", META_UNDEFINED );
        add( NIKON_FIRMWARE_VERSION, "FirmwareVersion", META_STRING );
        add( NIKON_FLASH_EXPOSURE_BRACKET_VALUE, "FlashExposureBracketValue", META_ULONG );
        add( NIKON_FLASH_EXPOSURE_COMPENSATION, "FlashExposureCompensation", META_ULONG );
        add( NIKON_FLASH_MODE, "FlashMode", META_UBYTE );
        add( NIKON_FLASH_SETTING, "FlashSetting", META_STRING );
        add( NIKON_FLASH_TYPE, "FlashType", META_STRING );
        add( NIKON_FOCUS_MODE, "FocusMode", META_STRING );
        add( NIKON_HIGH_ISO_NOISE_REDUCTION, "HighISONoiseReduction", META_USHORT );
        add( NIKON_HUE_ADJUSTMENT, "HueAdjustment", META_SSHORT );
        add( NIKON_IMAGE_ADJUSTMENT, "ImageAdjustment", META_STRING );
        add( NIKON_IMAGE_DATA_SIZE, "ImageDataSize", META_ULONG );
        add( NIKON_IMAGE_OPTIMIZATION, "ImageOptimization", META_UNKNOWN );
        add( NIKON_IMAGE_PROCESSING, "ImageProcessing", META_STRING );
        add( NIKON_IMAGE_STABILIZATION, "ImageStabilization", META_STRING );
        add( NIKON_ISO, "ISO", META_USHORT );
        add( NIKON_ISO_D70, "ISO_D70", META_USHORT );
        add( NIKON_ISO_D70_2, "ISO_D70_2", META_USHORT );
        add( NIKON_LD_VERSION, "LDVersion", META_UNKNOWN );
        add( NIKON_LD1X_APERTURE_AT_MAX_FOCAL, "LD1XApertureAtMaxFocal", META_UNKNOWN );
        add( NIKON_LD1X_APERTURE_AT_MIN_FOCAL, "LD1XApertureAtMinFocal", META_UNKNOWN );
        add( NIKON_LD1X_LENS_FSTOPS, "LD1XLensFstops", META_UNKNOWN );
        add( NIKON_LD1X_LENS_ID, "LD1XLensId", META_UNKNOWN );
        add( NIKON_LD1X_MAX_FOCAL_LENGTH, "LD1XMaxFocalLength", META_UNKNOWN );
        add( NIKON_LD1X_MCU_VERSION, "LD1XMcuVersion", META_UNKNOWN );
        add( NIKON_LD21_MIN_FOCAL_LENGTH, "LD21MinFocalLength", META_UNKNOWN );
        add( NIKON_LD21_AF_APERTURE, "LD21AFAperture", META_UNKNOWN );
        add( NIKON_LD21_APERTURE_AT_MAX_FOCAL, "LD21ApertureAtMaxFocal", META_UNKNOWN );
        add( NIKON_LD21_APERTURE_AT_MIN_FOCAL, "LD21ApertureAtMinFocal", META_UNKNOWN );
        add( NIKON_LD21_EFFECTIVE_MAX_APERTURE, "LD21EffectiveMaxAperture", META_UNKNOWN );
        add( NIKON_LD21_FOCAL_LENGTH, "LD21FocalLength", META_UNKNOWN );
        add( NIKON_LD21_FOCUS_DISTANCE, "LD21FocusDistance", META_UNKNOWN );
        add( NIKON_LD21_FOCUS_POSITION, "LD21FocusPosition", META_UNKNOWN );
        add( NIKON_LD21_LENS_FSTOPS, "LD21LensFstops", META_UNKNOWN );
        add( NIKON_LD21_LENS_ID, "LD21LensId", META_UNKNOWN );
        add( NIKON_LD21_MAX_FOCAL_LENGTH, "LD21MaxFocalLength", META_UNKNOWN );
        add( NIKON_LD21_MCU_VERSION, "LD21McuVersion", META_UNKNOWN );
        add( NIKON_LD21_MIN_FOCAL_LENGTH, "LD21MinFocalLength", META_UNKNOWN );
        add( NIKON_LENS, "Lens", META_URATIONAL );
        add( NIKON_LENS_DATA, "LensData", META_UNDEFINED );
        add( NIKON_LENS_FSTOPS, "LensFStops", META_UNDEFINED );
        add( NIKON_LENS_TYPE, "LensType", META_UBYTE );
        add( NIKON_LIGHT_SOURCE, "LightSource", META_STRING );
        add( NIKON_MANUAL_FOCUS_DISTANCE, "ManualFocusDistance", META_URATIONAL );
        add( NIKON_NOISE_REDUCTION, "NoiseReduction", META_STRING );
        add( NIKON_PREVIEW_IMAGE_IFD_POINTER, "PreviewImageIFDPointer", META_ULONG );
        add( NIKON_QUALITY, "Quality", META_STRING );
        add( NIKON_SATURATION, "Saturation", META_STRING );
        add( NIKON_SCENE_MODE, "SceneMode", META_STRING );
        add( NIKON_SENSOR_PIXEL_SIZE, "SensorPixelSize", META_URATIONAL );
        add( NIKON_SERIAL_NUMBER, "SerialNumber", META_STRING );
        add( NIKON_SERIAL_NUMBER_2, "SerialNumber2", META_UNKNOWN );
        add( NIKON_SHARPENING, "Sharpening", META_STRING );
        add( NIKON_SHOOTING_MODE, "ShootingMode", META_USHORT );
        add( NIKON_SHUTTER_COUNT, "ShutterCount", META_ULONG );
        add( NIKON_TONE_COMPENSATION, "ToneCompensation", META_STRING );
        add( NIKON_VARI_PROGRAM, "VariProgram", META_STRING );
        add( NIKON_WHITE_BALANCE, "WhiteBalance", META_STRING );
        add( NIKON_WHITE_BALANCE_FINE_TUNE, "WhiteBalanceFineTune", META_USHORT );
    }
}
/* vim:set et sw=4 ts=4: */
