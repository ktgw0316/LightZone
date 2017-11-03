/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import java.util.*;
import java.io.IOException;
import java.nio.ByteOrder;

import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.TextUtil;

import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.makernotes.KodakTags.*;

/**
 * A <code>KodakDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding Kodak-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class KodakDirectory extends MakerNotesDirectory implements
    ApertureProvider, ISOProvider, WidthHeightProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public float getAperture() {
        final ImageMetaValue value = getValue( KODAK_FNUMBER );
        return value != null ? value.getFloatValue() / 100: 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getImageHeight() {
        final ImageMetaValue value = getValue( KODAK_IMAGE_HEIGHT );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getImageWidth() {
        final ImageMetaValue value = getValue( KODAK_IMAGE_WIDTH );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getISO() {
        final ImageMetaValue value = getValue( KODAK_ISO );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * Gets the maker-notes adjustments for Kodak.
     *
     * @param buf The {@link LCByteBuffer} the metadata is in.
     * @param offset The offset to the start of the maker-notes.
     * @return Returns said adjustments.
     */
    public int[] getMakerNotesAdjustments( LCByteBuffer buf, int offset ) {
        //
        // The 8 bytes are a compact form of the make.
        //
        return new int[]{ 8, offset };
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;Kodak&quot;.
     */
    public String getName() {
        return "Kodak";
    }

    /**
     * {@inheritDoc}
     */
    public ImageMetaTagInfo getTagInfoFor( Integer id ) {
        return m_tagsByID.get( id );
    }

    /**
     * {@inheritDoc}
     */
    public ImageMetaTagInfo getTagInfoFor( String name ) {
        return m_tagsByName.get( name );
    }

    /**
     * {@inheritDoc}
     */
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case KODAK_DIGITAL_ZOOM:
            case KODAK_TOTAL_ZOOM:
                return TextUtil.tenths( value.getIntValue() / 100F );
            case KODAK_EXPOSURE_TIME:
                return MetadataUtil.shutterSpeedString( value.getFloatValue() / 1E5 );
            case KODAK_EXPOSURE_COMPENSATION:
                return TextUtil.tenths( value.getIntValue() / 1000F );
            case KODAK_FNUMBER:
                return TextUtil.tenths( getAperture() );
            case KODAK_ISO_SETTING: {
                final int iso = value.getIntValue();
                return iso > 0 ? String.valueOf( iso ) : "auto";
            }
            case KODAK_MONTH_DAY_CREATED: {
                final int monthDay = value.getUnsignedShortValue();
                return (monthDay >>> 8) + "/" + (monthDay & 0xFF);
            }
            case KODAK_TIME_CREATED: {
                final int time = value.getIntValue();
                return  String.format(
                            "%02d:%02d:%02d.%02d",
                             (time >>> 24) & 0xFF, (time >>> 16) & 0xFF,
                             (time >>>  8) & 0xFF,  time         & 0xFF
                        );
            }
            default:
                return super.valueToString( value );
        }
    }

    ////////// protected //////////////////////////////////////////////////////

    @Override
    protected ImageMetaValue getLongFocalValue() {
        return null;
    }

    @Override
    protected ImageMetaValue getShortFocalValue() {
        return null;
    }

    @Override
    protected ImageMetaValue getMaxApertureValue() {
        return null;
    }

    /**
     * Get the {@link ResourceBundle} to use for tags.
     *
     * @return Returns said {@link ResourceBundle}.
     */
    protected ResourceBundle getTagLabelBundle() {
        return m_tagBundle;
    }

    /**
     * {@inheritDoc}
     */
    protected Class<? extends ImageMetaTags> getTagsInterface() {
        return KodakTags.class;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean readMakerNotes( LCByteBuffer buf, int offset,
                                      int byteCount ) throws IOException {
        final ByteOrder origOrder =
            buf.probeOrder( offset + KODAK_IMAGE_WIDTH );

        int tagID = 0;
        final int end = offset + byteCount;
        while ( offset < end && tagID <= KODAK_SHARPNESS ) {
            final int valueSize = readValue( buf, offset, tagID );
            tagID += valueSize;
            offset += valueSize;
        }

        buf.order( origOrder );
        return true;
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
     * Read a single metadata value.
     *
     * @param buf The buffer to read from.
     * @param offset The offset into the buffer where the value starts.
     * @param tagID The tag ID.
     * @return Returns the size of the value (in bytes).
     */
    private int readValue( LCByteBuffer buf, int offset, int tagID )
        throws IOException
    {
        switch ( tagID ) {

            case KODAK_MODEL: {
                final String model = buf.getString( offset, 8, "ASCII" ).trim();
                final ImageMetaValue value = new StringMetaValue( model );
                putValue( tagID, value );
                return 8;
            }

            default: {
                final ImageMetaTagInfo tag = getTagInfoFor( tagID );
                if ( tag == null )
                    return 1;
                final ImageMetaValue value = tag.createValue();
                final int size = tag.getSize();
                final long n;
                switch ( size ) {
                    case 1:
                        n = buf.getUnsignedByte( offset );
                        break;
                    case 2:
                        n = buf.getUnsignedShort( offset );
                        break;
                    case 4:
                        n = buf.getLong( offset );
                        break;
                    default:
                        throw new IllegalStateException( "bad size: " + size );
                }
                value.setLongValue( n );
                putValue( tagID, value );
                return size;
            }
        }
    }

    /**
     * This is where the actual labels for the tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.makernotes.KodakTags"
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
        add( KODAK_BURST_MODE, "BurstMode", META_UBYTE );
        add( KODAK_DIGITAL_ZOOM, "DigitalZoom", META_USHORT );
        add( KODAK_EXPOSURE_COMPENSATION, "ExposureCompensation", META_SSHORT );
        add( KODAK_EXPOSURE_TIME, "ExposureTime", META_ULONG );
        add( KODAK_FLASH_FIRED, "FlashFired", META_UBYTE );
        add( KODAK_FNUMBER, "FNumber", META_USHORT );
        add( KODAK_FOCUS_MODE, "FocusMode", META_UBYTE );
        add( KODAK_IMAGE_HEIGHT, "ImageHeight", META_USHORT );
        add( KODAK_IMAGE_WIDTH, "ImageWidth", META_USHORT );
        add( KODAK_ISO, "ISO", META_USHORT );
        add( KODAK_ISO_SETTING, "ISOSetting", META_USHORT );
        add( KODAK_METERING_MODE, "MeteringMode", META_USHORT );
        add( KODAK_MODEL, "Model", META_STRING );
        add( KODAK_MONTH_DAY_CREATED, "MonthDayCreated", META_USHORT );
        add( KODAK_QUALITY, "Quality", META_UBYTE );
        add( KODAK_SEQUENCE_NUMBER, "SequenceNumber", META_UBYTE );
        add( KODAK_SHARPNESS, "Sharpness", META_SSHORT );
        add( KODAK_SHUTTER_MODE, "ShutterMode", META_UBYTE );
        add( KODAK_TIME_CREATED, "TimeCreated", META_ULONG );
        add( KODAK_TOTAL_ZOOM, "TotalZoom", META_USHORT );
        add( KODAK_WHITE_BALANCE, "WhiteBalance", META_UBYTE );
        add( KODAK_YEAR_CREATED, "YearCreated", META_USHORT );

        add( KODAK_UNKNOWN_0x18, "Unknown0x18", META_USHORT );
        add( KODAK_UNKNOWN_0x26, "Unknown0x26", META_USHORT );
        add( KODAK_UNKNOWN_0x28, "Unknown0x28", META_ULONG );
        add( KODAK_UNKNOWN_0x2C, "Unknown0x2C", META_ULONG );
        add( KODAK_UNKNOWN_0x30, "Unknown0x30", META_ULONG );
        add( KODAK_UNKNOWN_0x34, "Unknown0x34", META_ULONG );
        add( KODAK_UNKNOWN_0x3A, "Unknown0x3A", META_USHORT );
        add( KODAK_UNKNOWN_0x3C, "Unknown0x3C", META_USHORT );
        add( KODAK_UNKNOWN_0x3E, "Unknown0x3E", META_USHORT );
        add( KODAK_UNKNOWN_0x42, "Unknown0x42", META_ULONG );
        add( KODAK_UNKNOWN_0x46, "Unknown0x46", META_ULONG );
        add( KODAK_UNKNOWN_0x4A, "Unknown0x4A", META_ULONG );
        add( KODAK_UNKNOWN_0x4E, "Unknown0x4E", META_ULONG );
        add( KODAK_UNKNOWN_0x52, "Unknown0x52", META_ULONG );
        add( KODAK_UNKNOWN_0x56, "Unknown0x56", META_ULONG );
        add( KODAK_UNKNOWN_0x5A, "Unknown0x5A", META_ULONG );
    }
}
/* vim:set et sw=4 ts=4: */
