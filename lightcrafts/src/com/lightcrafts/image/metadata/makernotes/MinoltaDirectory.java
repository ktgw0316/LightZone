/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.utils.bytebuffer.ArrayByteBuffer;
import com.lightcrafts.utils.NumberUtil;
import com.lightcrafts.utils.TextUtil;

import static com.lightcrafts.image.metadata.EXIFConstants.*;
import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.makernotes.MinoltaTags.*;
import static com.lightcrafts.image.types.JPEGConstants.*;

/**
 * A <code>MinoltaDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding Minolta-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class MinoltaDirectory extends MakerNotesDirectory
    implements FocalLengthProvider, LensProvider, ShutterSpeedProvider,
               PreviewImageProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public float getFocalLength() {
        final ImageMetaValue value = getValue( MINOLTA_CS_FOCAL_LENGTH );
        return value != null ? value.getFloatValue() : 0;
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;Minolta&quot;.
     */
    public String getName() {
        return "Minolta";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException
    {
        //
        // See the comment in MRWImageType.getPreviewImage().
        //
        final ImageMetaValue offsetValue =
            getValue( MINOLTA_PREVIEW_IMAGE_START );
        final ImageMetaValue lengthValue =
            getValue( MINOLTA_PREVIEW_IMAGE_LENGTH );
        if ( offsetValue == null || lengthValue == null )
            return null;
        final int offset = offsetValue.getIntValue();
        final int length = lengthValue.getIntValue();
        if ( offset <= 0 || length <= 0 )
            return null;

        final byte[] buf = imageInfo.getByteBuffer().getBytes(
            offset
                + 1 // JPEG_MARKER_BYTE
                + 1 // JPEG_SOI_MARKER
                + 1 // JPEG_MARKER_BYTE
                + 1 // JPEG_APP1_MARKER
                + 2 // APP1 length
                + EXIF_HEADER_START_SIZE,
            length
        );
        //
        // Patch the JPEG preview image.
        //
        if ( buf[1] == JPEG_SOI_MARKER && buf[2] == JPEG_MARKER_BYTE )
            buf[0] = JPEG_MARKER_BYTE;

        // TODO: verify that this is actually an sRGB image, what about Adobe RGB shooting, etc.?
        return JPEGImageType.getImageFromBuffer(
            new ArrayByteBuffer( buf ), new LongMetaValue( 0 ), 0, lengthValue,
            maxWidth, maxHeight
        );
    }

    /**
     * {@inheritDoc}
     */
    public float getShutterSpeed() {
        final ImageMetaValue value = getValue( MINOLTA_CS_SHUTTER_SPEED );
        return value != null ? value.getFloatValue() : 0;
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
     * Puts a key/value pair into this directory.  For a Minolta tag that has
     * subfields, expands the values into multiple key/value pairs.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The value to put.
     * @see #valueToString(ImageMetaValue)
     */
    public void putValue( Integer tagID, ImageMetaValue value ) {
        switch ( tagID ) {
            case MINOLTA_CAMERA_SETTINGS: {
                final String model = getOwningMetadata().getCameraMake( true );
                if ( model != null && model.contains( "DIMAGE X31" ) ) {
                    //
                    // These camera settings aren't for the DiIMAGE X31.
                    //
                    break;
                }
                // no break;
            }
            case MINOLTA_CAMERA_SETTINGS_OLD:
                explodeSubfields( tagID, 0, value, true );
                break;
            case MINOLTA_COMPRESSED_IMAGE_SIZE:
                if ( value.getLongValue() == 0 )
                    return;
                break;
            case MINOLTA_CS_APERTURE:
            case MINOLTA_CS_MAX_APERTURE: {
                final int n = value.getIntValue();
                value = new FloatMetaValue(
                    (float)NumberUtil.tenths( Math.pow( 2, n / 16.0 - 0.5 ) )
                );
                break;
            }
            case MINOLTA_CS_BRIGHTNESS: {
                final int n = value.getIntValue();
                value = new FloatMetaValue(
                    (float)NumberUtil.tenths( n / 8.0 - 6 )
                );
                break;
            }
            case MINOLTA_CS_COLOR_BALANCE_BLUE:
            case MINOLTA_CS_COLOR_BALANCE_GREEN:
            case MINOLTA_CS_COLOR_BALANCE_RED:
            case MINOLTA_CS_FOCAL_LENGTH: {
                final int n = value.getIntValue();
                value = new FloatMetaValue(
                    (float)NumberUtil.tenths( n / 256.0 )
                );
                break;
            }
            case MINOLTA_CS_COLOR_PROFILE: {
                final String model = getOwningMetadata().getCameraMake( true );
                if ( model != null && !model.contains( "DIMAGE 7HI" ) ) {
                    //
                    // This is for the DiMAGE 7Hi only.
                    //
                    break;
                }
                // no break;
            }
            case MINOLTA_CS_DATE: {
                final long n = value.getLongValue();
                //
                // Even though most Minolta maker note data is big-endian, the
                // date is little-endian.
                //
                final StringBuilder sb = new StringBuilder();
                sb.append( (n & 0xFFFF0000) >> 16 );
                sb.append( TextUtil.zeroPad( (int)((n & 0x0000FF00) >> 8), 10, 2 ) );
                sb.append( TextUtil.zeroPad( (int)(n & 0x000000FF), 10, 2 ) );
                try {
                    value = new DateMetaValue( sb.toString() );
                }
                catch ( IllegalArgumentException e ) {
                    return;
                }
                break;
            }
            case MINOLTA_CS_EXPOSURE_COMPENSATION: {
                final int n = value.getIntValue();
                value = new FloatMetaValue(
                    (float)NumberUtil.tenths( n / 3.0 - 2 )
                );
                break;
            }
            case MINOLTA_CS_FLASH_EXPOSURE_COMP: {
                final int n = value.getIntValue();
                value = new FloatMetaValue(
                    (float)NumberUtil.tenths( (n - 6) / 3.0 )
                );
                break;
            }
            case MINOLTA_CS_ISO: {
                final int n = value.getIntValue();
                value = new FloatMetaValue(
                    (float)NumberUtil.tenths(
                        Math.pow( 2, (n / 8.0 - 1) * 3.125 )
                    )
                );
                break;
            }
            case MINOLTA_CS_SHUTTER_SPEED: {
                final int n = value.getIntValue();
                value = new FloatMetaValue(
                    (float)Math.pow( 2, (48 - n) / 8.0 )
                );
                break;
            }
            case MINOLTA_CS_TIME: {
                final long n = value.getLongValue();
                //
                // Even though most Minolta maker note data is big-endian, the
                // time is little-endian.
                //
                final StringBuilder sb = new StringBuilder();
                sb.append( TextUtil.zeroPad( (int)((n & 0x00FF0000) >> 16), 10, 2 ) );
                sb.append( ':' );
                sb.append( TextUtil.zeroPad( (int)((n & 0x0000FF00) >> 8), 10, 2 ) );
                sb.append( ':' );
                sb.append( TextUtil.zeroPad( (int)(n & 0x000000FF), 10, 2 ) );
                value = new StringMetaValue( sb.toString() );
                break;
            }
            case MINOLTA_IMAGE_SIZE: {
                //
                // This tag (0x0103) is Quality on the DiMAGE A2 or 7Hi but
                // ImageSize for others (except the A200).  Since both quality
                // and image size information are obtainable elsewhere, just
                // ignore this tag because it's more trouble than it's worth.
                //
                return;
            }
        }
        super.putValue( tagID, value );
    }

    /**
     * {@inheritDoc}
     */
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case MINOLTA_COMPRESSED_IMAGE_SIZE:
                return TextUtil.quantify( value.getLongValue() );
            case MINOLTA_CS_FOCAL_LENGTH:
                return value.getStringValue() + "mm"; // TODO: localize "mm"
            case MINOLTA_CS_FOCUS_DISTANCE: {
                final int n = value.getIntValue();
                return n > 0 ? TextUtil.tenths( n / 1000.0 ) : "infinite";
            }
            case MINOLTA_CS_SHUTTER_SPEED:
                return MetadataUtil.shutterSpeedString( value.getFloatValue() );
            case MINOLTA_LENS_ID: {
                final String label = hasTagValueLabelFor( MINOLTA_LENS_ID );
                return label != null ? label : "unknown"; // TODO: localize
            }
            default:
                return super.valueToString( value );
        }
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImageMetaValue getLensNamesValue() {
        return getValue( MINOLTA_LENS_ID );
    }

    @Override
    protected ImageMetaValue getLongFocalValue() {
        return null; // TODO:
    }

    @Override
    protected ImageMetaValue getShortFocalValue() {
        return null; // TODO:
    }

    @Override
    protected ImageMetaValue getMaxApertureValue() {
        return getValue( MINOLTA_CS_MAX_APERTURE );
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
        return MinoltaTags.class;
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
     * This is where the actual labels for the tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.makernotes.MinoltaTags"
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
        add( MINOLTA_CAMERA_SETTINGS, "CameraSettings", META_UNDEFINED );
        add( MINOLTA_CAMERA_SETTINGS_OLD, "CameraSettingsOld", META_UNDEFINED );
        add( MINOLTA_COLOR_MODE, "ColorMode", META_ULONG );
        add( MINOLTA_COMPRESSED_IMAGE_SIZE, "CompressedImageSize", META_ULONG );
        add( MINOLTA_CS_APERTURE, "CSAperture", META_UNKNOWN );
        add( MINOLTA_CS_BRACKET_STEP, "CSBracketStep", META_UNKNOWN );
        add( MINOLTA_CS_BRIGHTNESS, "CSBrightness", META_UNKNOWN );
        add( MINOLTA_CS_BW_FILTER, "CSBWFilter", META_UNKNOWN );
        add( MINOLTA_CS_COLOR_BALANCE_BLUE, "CSColorBalanceBlue", META_UNKNOWN );
        add( MINOLTA_CS_COLOR_BALANCE_GREEN, "CSColorBalanceGreen", META_UNKNOWN );
        add( MINOLTA_CS_COLOR_BALANCE_RED, "CSColorBalanceRed", META_UNKNOWN );
        add( MINOLTA_CS_COLOR_FILTER, "CSColorFilter", META_UNKNOWN );
        add( MINOLTA_CS_COLOR_MODE, "CSColorMode", META_UNKNOWN );
        add( MINOLTA_CS_COLOR_PROFILE, "CSColorProfile", META_UNKNOWN );
        add( MINOLTA_CS_CONTRAST, "CSContrast", META_UNKNOWN );
        add( MINOLTA_CS_DATE, "CSDate", META_DATE );
        add( MINOLTA_CS_DEC_POSITION, "CSDecPosition", META_UNKNOWN );
        add( MINOLTA_CS_DIGITAL_ZOOM, "CSDigitalZoom", META_UNKNOWN );
        add( MINOLTA_CS_DRIVE_MODE, "CSDriveMode", META_UNKNOWN );
        add( MINOLTA_CS_EXPOSURE_COMPENSATION, "CSExposureCompensation", META_UNKNOWN );
        add( MINOLTA_CS_EXPOSURE_MODE, "CSExposureMode", META_UNKNOWN );
        add( MINOLTA_CS_FILE_NUMBER_MEMORY, "CSFileNumberMemory", META_UNKNOWN );
        add( MINOLTA_CS_FLASH_EXPOSURE_COMP, "CSFlashExposureComp", META_UNKNOWN );
        add( MINOLTA_CS_FLASH_FIRED, "CSFlashFired", META_UNKNOWN );
        add( MINOLTA_CS_FLASH_MODE, "CSFlashMode", META_UNKNOWN );
        add( MINOLTA_CS_FOCAL_LENGTH, "CSFocalLength", META_UNKNOWN );
        add( MINOLTA_CS_FOCUS_AREA, "CSFocusArea", META_UNKNOWN );
        add( MINOLTA_CS_FOCUS_DISTANCE, "CSFocusDistance", META_UNKNOWN );
        add( MINOLTA_CS_FOCUS_MODE, "CSFocusMode", META_UNKNOWN );
        add( MINOLTA_CS_FOLDER_NAME, "CSFolderName", META_UNKNOWN );
        add( MINOLTA_CS_IMAGE_SIZE, "CSImageSize", META_UNKNOWN );
        add( MINOLTA_CS_INTERNAL_FLASH, "CSInternalFlash", META_UNKNOWN );
        add( MINOLTA_CS_INTERVAL_LENGTH, "CSIntervalLength", META_UNKNOWN );
        add( MINOLTA_CS_INTERVAL_MODE, "CSIntervalMode", META_UNKNOWN );
        add( MINOLTA_CS_INTERVAL_NUMBER, "CSIntervalNumber", META_UNKNOWN );
        add( MINOLTA_CS_ISO, "CSISO", META_UNKNOWN );
        add( MINOLTA_CS_ISO_SETTING, "CSISOSetting", META_UNKNOWN );
        add( MINOLTA_CS_LAST_FILE_NUMBER, "CSLastFileNumber", META_UNKNOWN );
        add( MINOLTA_CS_MACRO_MODE, "CSMacroMode", META_UNKNOWN );
        add( MINOLTA_CS_MAX_APERTURE, "CSMaxAperture", META_UNKNOWN );
        add( MINOLTA_CS_METERING_MODE, "CSMeteringMode", META_UNKNOWN );
        add( MINOLTA_CS_MODEL, "CSModel", META_UNKNOWN );
        add( MINOLTA_CS_QUALITY, "CSQuality", META_UNKNOWN );
        add( MINOLTA_CS_SATURATION, "CSSaturation", META_UNKNOWN );
        add( MINOLTA_CS_SHARPNESS, "CSSharpness", META_UNKNOWN );
        add( MINOLTA_CS_SHUTTER_SPEED, "CSShutterSpeed", META_UNKNOWN );
        add( MINOLTA_CS_SPOT_FOCUS_X, "CSSpotFocusX", META_UNKNOWN );
        add( MINOLTA_CS_SPOT_FOCUS_Y, "CSSpotFocusY", META_UNKNOWN );
        add( MINOLTA_CS_SUBJECT_PROGRAM, "CSSubjectProgram", META_UNKNOWN );
        add( MINOLTA_CS_TIME, "CSTime", META_UNKNOWN );
        add( MINOLTA_CS_WHITE_BALANCE, "CSWhiteBalance", META_UNKNOWN );
        add( MINOLTA_CS_WIDE_FOCUS_ZONE, "CSWideFocusZone", META_UNKNOWN );
        add( MINOLTA_IMAGE_SIZE, "ImageSize", META_ULONG );
        add( MINOLTA_IMAGE_STABILIZATION, "ImageStabilization", META_ULONG );
        add( MINOLTA_LENS_ID, "LensID", META_ULONG );
        add( MINOLTA_MAKER_NOTES_VERSION, "MakerNotesVersion", META_UNKNOWN );
        add( MINOLTA_PREVIEW_IMAGE_LENGTH, "PreviewImageLength", META_ULONG );
        add( MINOLTA_PREVIEW_IMAGE_START, "PreviewImageStart", META_ULONG );
        add( MINOLTA_QUALITY, "Quality", META_ULONG );
        add( MINOLTA_QUALITY_2, "Quality2", META_ULONG );
        add( MINOLTA_SCENE_MODE, "SceneMode", META_ULONG );
        add( MINOLTA_ZONE_MATCHING, "ZoneMatching", META_ULONG );
    }
}
/* vim:set et sw=4 ts=4: */
