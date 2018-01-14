/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.*;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.image.types.PEFImageType;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.TextUtil;

import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.makernotes.PentaxConstants.*;
import static com.lightcrafts.image.metadata.makernotes.PentaxTags.*;

/**
 * A <code>PentaxDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding Pentax-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class PentaxDirectory extends MakerNotesDirectory implements
    ApertureProvider, CaptureDateTimeProvider, FlashProvider, FocalLengthProvider,
    ISOProvider, LensProvider, PreviewImageProvider, ShutterSpeedProvider,
    WidthHeightProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public float getAperture() {
        final ImageMetaValue value = getValue( PENTAX_FNUMBER );
        return  value == null ? 0 :
                MetadataUtil.fixFStop( value.getIntValue() / 10F );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getCaptureDateTime() {
        final ImageMetaValue value = getValue( PENTAX_DATE );
        return  value instanceof DateMetaValue ?
                ((DateMetaValue)value).getDateValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFlash() {
        final ImageMetaValue value = getValue( PENTAX_FLASH_MODE );
        if ( value != null ) {
            int flashBits = 0;
            final int pentaxFlash = value.getIntValue();
            switch ( pentaxFlash ) {
                case PENTAX_FLASH_AUTO_FIRED:
                case PENTAX_FLASH_AUTO_FIRED_RED_EYE:
                case PENTAX_FLASH_ON_FIRED:
                    flashBits |= FLASH_FIRED_BIT;
                    break;
            }
            switch ( pentaxFlash ) {
                case PENTAX_FLASH_AUTO_FIRED:
                case PENTAX_FLASH_AUTO_FIRED_RED_EYE:
                case PENTAX_FLASH_AUTO_NO_FIRED:
                    flashBits |= FLASH_MODE_AUTO;
                    break;
                case PENTAX_FLASH_OFF_NO_FIRED:
                    flashBits |= FLASH_MODE_COMPULSORY_OFF;
                    break;
                case PENTAX_FLASH_ON_FIRED:
                case PENTAX_FLASH_ON_NO_FIRE:
                case PENTAX_FLASH_ON_RED_EYE:
                case PENTAX_FLASH_ON_SLOW_SYNC:
                case PENTAX_FLASH_ON_SLOW_SYNC_RED_EYE:
                case PENTAX_FLASH_ON_SOFT:
                case PENTAX_FLASH_ON_TRAILING_CURTAIN_SYNC:
                case PENTAX_FLASH_ON_WIRELESS_CONTROL:
                case PENTAX_FLASH_ON_WIRELESS_MASTER:
                    flashBits |= FLASH_MODE_COMPULSORY_ON;
                    break;
            }
            switch ( pentaxFlash ) {
                case PENTAX_FLASH_AUTO_FIRED_RED_EYE:
                case PENTAX_FLASH_ON_RED_EYE:
                case PENTAX_FLASH_ON_SLOW_SYNC_RED_EYE:
                    flashBits |= FLASH_RED_EYE_BIT;
                    break;
            }
            return flashBits;
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFocalLength() {
        final ImageMetaValue value = getValue( PENTAX_FOCAL_LENGTH );
        if ( value == null )
            return 0;
        final String make = getOwningMetadata().getCameraMake( true );
        return (make != null && m_focalLengthPattern.matcher(make).matches())
                ? value.getIntValue() / 10F
                : value.getIntValue() / 100F;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageHeight() {
        final ImageMetaValue value = getValue( PENTAX_RAW_IMAGE_SIZE );
        if ( value != null && value.getValueCount() == 2 ) {
            try {
                return Integer.parseInt( value.getValues()[1] );
            }
            catch ( NumberFormatException e ) {
                // ignore
            }
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageWidth() {
        final ImageMetaValue value = getValue( PENTAX_RAW_IMAGE_SIZE );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getISO() {
        final String label = hasTagValueLabelFor( PENTAX_ISO );
        if ( label != null )
            try {
                return Integer.parseInt( label );
            }
            catch ( NumberFormatException e ) {
                // ignore
            }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLens() {
        return hasTagValueLabelFor( PENTAX_LENS_TYPE );
    }

    /**
     * Gets the maker-notes adjustments for Pentax.
     *
     * @param buf The {@link LCByteBuffer} the metadata is in.
     * @param offset The offset to the start of the maker-notes.
     * @return Returns said adjustments.
     */
    @Override
    public int[] getMakerNotesAdjustments( LCByteBuffer buf, int offset )
            throws IOException
    {
        if ( buf.getEquals( offset, "AOC", "ASCII" ) ) {
            //
            // These are the maker notes from a JPEG file.  The 6 bytes are:
            //
            //      0-2: "AOC"
            //      3  : 0
            //      4-5: "MM"
            //
            return new int[]{ 6, offset };
        }
        final String s = buf.getString( offset, 7, "ASCII" ).toUpperCase();
        if (s.equals("PENTAX ") || s.equals("SAMSUNG") || s.startsWith("RICOH")) {
            //
            // These are the maker notes from a DNG file.  Note that the space
            // after PENTAX is correct.
            //
            return new int[]{ 10, offset };
        }
        throw new IOException( "unknown maker notes header" );
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;Pentax&quot;.
     */
    @Override
    public String getName() {
        return "Pentax";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return PEFImageType.INSTANCE.getPreviewImage(
            imageInfo, maxWidth, maxHeight
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getShutterSpeed() {
        final ImageMetaValue value = getValue( PENTAX_EXPOSURE_TIME );
        return value != null ? value.getFloatValue() * 1E-5F : 0F;
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
     * Puts a key/value pair into this directory.  For a Pentax tag that has
     * subfields, expands the values into multiple key/value pairs.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The value to put.
     * @see #valueToString(ImageMetaValue)
     */
    @Override
    public void putValue( Integer tagID, ImageMetaValue value ) {
        switch ( tagID ) {
            case PENTAX_CONTRAST:
            case PENTAX_SATURATION:
            case PENTAX_SHARPNESS:
                //
                // The *ist D cameras have a second value of 0 -- remove it.
                //
                if ( value.getValueCount() > 1 )
                    value = new UnsignedShortMetaValue( value.getIntValue() );
                break;
            case PENTAX_DATE: {
                if ( !(value instanceof UndefinedMetaValue) ||
                        value.getValueCount() != 4 )
                    return;
                final byte[] buf =
                    ((UndefinedMetaValue)value).getUndefinedValue();
                final Calendar cal = new GregorianCalendar(
                    ((int)buf[0] & 0xFF) << 8 | (int)buf[1] & 0xFF,
                    buf[2] - 1, buf[3]
                );
                value = new DateMetaValue( cal.getTime() );
                break;
            }
            case PENTAX_TIME: {
                if ( !(value instanceof UndefinedMetaValue) ||
                        value.getValueCount() < 3 )
                    return;
                //
                // Rather than display separate date/time fields, just add the
                // time to the date field.
                //
                final ImageMetaValue dateValue = getValue( PENTAX_DATE );
                if (dateValue == null) {
                    return;
                }
                final Date date = ((DateMetaValue)dateValue).getDateValue();
                final byte[] buf =
                    ((UndefinedMetaValue)value).getUndefinedValue();
                date.setTime(
                    date.getTime() +
                    (buf[0] * 60 * 60 + buf[1] * 60 + buf[2]) * 1000
                );
                return;
            }
            case PENTAX_IMAGE_SIZE:
                if ( value.getValueCount() == 2 ) {
                    //
                    // Some Pentax cameras have 2 values for this.  Since our
                    // value labeling scheme doesn't support this, we combine
                    // the 2 values into a single value.
                    //
                    final String[] values = value.getValues();
                    try {
                        final int v0 = Integer.parseInt( values[0] );
                        final int v1 = Integer.parseInt( values[1] );
                        value = new UnsignedShortMetaValue( v0 << 8 | v1 );
                    }
                    catch ( NumberFormatException e ) {
                        return;
                    }
                }
                break;
            case PENTAX_LENS_TYPE: {
                final int count = value.getValueCount();
                if (count < 2 || 4 < count) {
                    return;
                }
                //
                // Pentax Cameras use 2 values for this: a lens group and a
                // lens ID within that group.  Since our value labeling scheme
                // doesn't support this, we combine the 2 values into a single
                // value.
                //
                final int lensGroupID = value.getIntValueAt(0);
                final int lensID      = value.getIntValueAt(1);
                final int lensLabelID = lensGroupID << 8 | lensID;
                value = new UnsignedShortMetaValue( lensLabelID );
                break;
            }
        }
        super.putValue( tagID, value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case PENTAX_BLUE_BALANCE:
            case PENTAX_RED_BALANCE:
                return TextUtil.tenths( value.getIntValue() / 256F );
            case PENTAX_EXPOSURE_COMPENSATION:
                return TextUtil.tenths( (value.getIntValue() - 50) / 10F );
            case PENTAX_EXPOSURE_TIME:
                return MetadataUtil.shutterSpeedString( getShutterSpeed() );
            case PENTAX_FNUMBER:
                return TextUtil.tenths( getAperture() );
            case PENTAX_FOCAL_LENGTH:         // TODO: localize "mm"
                return TextUtil.tenths( getFocalLength() ) + "mm";
            case PENTAX_PREVIEW_IMAGE_SIZE:
            case PENTAX_RAW_IMAGE_SIZE: {
                final String[] values = value.getValues();
                if ( values.length == 2 )
                    return values[0] + " x " + values[1];
                break;
            }
            case PENTAX_LENS_TYPE:
                return getLens();
        }
        return super.valueToString( value );
    }

    ////////// protected //////////////////////////////////////////////////////

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
        return PentaxTags.class;
    }

    /**
     * Gets the priority of this directory for providing the metadata supplied
     * by implementing the given provider interface.
     * <p>
     * The priority for {@link LensProvider} for Pentax is higher than
     * the priority from {@link CoreDirectory} and EXIF/TIFF metadata.
     *
     * @param provider The provider interface to get the priority for.
     * @return Returns said priority.
     */
    @Override
    protected int getProviderPriorityFor(
            Class<? extends ImageMetadataProvider> provider )
    {
        return (provider == LensProvider.class)
                ? PROVIDER_PRIORITY_DEFAULT + 20
                : super.getProviderPriorityFor( provider );
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
     * This pattern is used to determine how to calculate the focal length.
     */
    private static final Pattern m_focalLengthPattern =
        Pattern.compile( ".*\\bOPTIO (?:30|33WR|43WR|450|550|555|750Z|X)\\b.*" );

    /**
     * A mapping of tags by ID.
     */
    private static final Map<Integer,ImageMetaTagInfo> m_tagsByID =
        new HashMap<Integer, ImageMetaTagInfo>();

    /**
     * A mapping of tags by name.
     */
    private static final Map<String,ImageMetaTagInfo> m_tagsByName =
        new HashMap<String, ImageMetaTagInfo>();

    /**
     * This is where the actual labels for the Pentax tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.makernotes.PentaxTags"
    );

    static {
        add( PENTAX_AF_POINT_SELECTED, "AFPointSelected", META_USHORT );
        add( PENTAX_AUTO_AF_POINT, "AutoAFPoint", META_USHORT );
        add( PENTAX_BLACK_POINT, "BlackPoint", META_USHORT );
        add( PENTAX_BLUE_BALANCE, "BlueBalance", META_USHORT );
        add( PENTAX_CONTRAST, "Contrast", META_USHORT );
        add( PENTAX_DATA_DUMP, "DataDump", META_UNDEFINED );
        add( PENTAX_DATE, "Date", META_DATE );
        add( PENTAX_DESTINATION_CITY, "DestinationCity", META_USHORT );
        add( PENTAX_DESTINATION_CITY_CODE, "DestinationCityCode", META_UNDEFINED );
        add( PENTAX_DESTINATION_DST, "DestinationDST", META_USHORT );
        add( PENTAX_DIGITAL_ZOOM, "DigitalZoom", META_USHORT );
        add( PENTAX_EXPOSURE_COMPENSATION, "ExposureCompensation", META_USHORT );
        add( PENTAX_EXPOSURE_TIME, "ExposureTime", META_USHORT );
        add( PENTAX_FLASH_MODE, "FlashMode", META_USHORT );
        add( PENTAX_FNUMBER, "FNumber", META_USHORT );
        add( PENTAX_FOCAL_LENGTH, "FocalLength", META_USHORT );
        add( PENTAX_FOCUS_MODE, "FocusMode", META_USHORT );
        add( PENTAX_FOCUS_POSITION, "FocusPosition", META_USHORT );
        add( PENTAX_FRAME_NUMBER, "FrameNumber", META_USHORT );
        add( PENTAX_HOME_TOWN_CITY, "HomeTownCity", META_USHORT );
        add( PENTAX_HOME_TOWN_CITY_CODE, "HomeTownCityCode", META_UNDEFINED );
        add( PENTAX_HOME_TOWN_DST, "HomeTownDST", META_USHORT );
        add( PENTAX_IMAGE_SIZE, "ImageSize", META_USHORT );
        add( PENTAX_ISO, "ISO", META_USHORT );
        add( PENTAX_LENS_TYPE, "LensType", META_UBYTE );
        add( PENTAX_LIGHT_READING, "LightReading", META_SSHORT );
        add( PENTAX_METERING_MODE, "MeteringMode", META_USHORT );
        add( PENTAX_MODEL_ID, "ModelID", META_ULONG );
        add( PENTAX_MODEL_TYPE, "ModelType", META_USHORT );
        add( PENTAX_PICTURE_MODE, "PictureMode", META_USHORT );
        add( PENTAX_PREVIEW_IMAGE_DATA, "PreviewImageData", META_UNDEFINED );
        add( PENTAX_PREVIEW_IMAGE_LENGTH, "PreviewImageLength", META_ULONG );
        add( PENTAX_PREVIEW_IMAGE_SIZE, "PreviewImageSize", META_USHORT );
        add( PENTAX_PREVIEW_IMAGE_START, "PreviewImageStart", META_ULONG );
        add( PENTAX_PRINT_IMAGE_MATCHING, "PrintImageMatching", META_UNDEFINED );
        add( PENTAX_QUALITY, "Quality", META_USHORT );
        add( PENTAX_RAW_IMAGE_SIZE, "RawImageSize", META_USHORT );
        add( PENTAX_RED_BALANCE, "RedBalance", META_USHORT );
        add( PENTAX_SATURATION, "Saturation", META_USHORT );
        add( PENTAX_SHARPNESS, "Sharpness", META_USHORT );
        add( PENTAX_TIME, "Time", META_UNDEFINED );
        add( PENTAX_TONE_CURVE, "ToneCurve", META_UNDEFINED );
        add( PENTAX_TONE_CURVES, "ToneCurves", META_UNDEFINED );
        add( PENTAX_VERSION, "Version", META_UBYTE );
        add( PENTAX_WHITE_BALANCE, "WhiteBalance", META_USHORT );
        add( PENTAX_WHITE_BALANCE_MODE, "WhiteBalanceMode", META_USHORT );
        add( PENTAX_WHITE_POINT, "WhitePoint", META_USHORT );
        add( PENTAX_WORLD_TIME_LOCATION, "WorldTimeLocation", META_USHORT );
    }

    @Override
    protected ImageMetaValue getLongFocalValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected ImageMetaValue getShortFocalValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected ImageMetaValue getMaxApertureValue() {
        // TODO Auto-generated method stub
        return null;
    }
}
/* vim:set et sw=4 ts=4: */
