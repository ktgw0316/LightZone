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
import com.lightcrafts.image.metadata.providers.PreviewImageProvider;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.image.types.ORFImageType;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.TextUtil;

import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.makernotes.OlympusTags.*;

/**
 * A <code>OlympusDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding Olympus-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class OlympusDirectory extends MakerNotesDirectory implements
    PreviewImageProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the maker-notes adjustments for Olympus.
     *
     * @param buf The {@link LCByteBuffer} the metadata is in.
     * @param offset The offset to the start of the maker-notes.
     * @return Returns said adjustments.
     * @throws IOException 
     */
    public int[] getMakerNotesAdjustments( LCByteBuffer buf, int offset )
        throws IOException
    {
        if ( buf.getEquals( offset, "OLYMPUS", "ASCII" ) )
            return new int[]{ 12, offset };
        if ( buf.getEquals( offset, "OLYMP", "ASCII" ) )
            return new int[]{ 8, offset };
        throw new IOException( "unknown maker notes header" );
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;Olympus&quot;.
     */
    public String getName() {
        return "Olympus";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return ORFImageType.INSTANCE.getPreviewImage(
            imageInfo, maxWidth, maxHeight
        );
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
     * Puts a key/value pair into this directory.  For a Olympus tag that has
     * subfields, expands the values into multiple key/value pairs.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The value to put.
     * @see #valueToString(ImageMetaValue)
     */
    public void putValue( Integer tagID, ImageMetaValue value ) {
        switch ( tagID ) {
            case OLYMPUS_CAMERA_SETTINGS:
            case OLYMPUS_CAMERA_SETTINGS_OLD:
                explodeSubfields( tagID, 0, value, true );
                break;
            case OLYMPUS_ISO: {
                final float n = value.getFloatValue();
                value = new UnsignedShortMetaValue(
                    (int)(100 * Math.pow( 2, n - 5 ))
                );
                break;
            }
            case OLYMPUS_WHITE_BALANCE_MODE:
                final long[] v = ((LongMetaValue)value).getLongValues();
                final int n;
                switch ( v.length ) {
                    case 1:
                        n = (int)v[0];
                        break;
                    case 2:
                        n = (int)(v[0] * 10 + v[1]);
                        break;
                    default:
                        return;
                }
                value = new UnsignedShortMetaValue( n );
                break;
        }
        super.putValue( tagID, value );
    }

    /**
     * {@inheritDoc}
     */
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case OLYMPUS_BLUE_BALANCE:
            case OLYMPUS_RED_BALANCE:
                return TextUtil.tenths( value.getFloatValue() / 256 );
            case OLYMPUS_CS_CUSTOM_SATURATION: {
                final String model = getOwningMetadata().getCameraMake( true );
                if ( model != null && model.contains( "E-1" ) ) {
                    final long[] n = ((LongMetaValue)value).getLongValues();
                    n[0] -= n[1];
                    n[2] -= n[1];
                    return n[0] + " (0," + n[2] + ')';
                }
                // no break;
            }
            case OLYMPUS_CS_CONTRAST_SETTING:
            case OLYMPUS_CS_PM_CONTRAST:
            case OLYMPUS_CS_PM_SATURATION:
            case OLYMPUS_CS_PM_SHARPNESS:
            case OLYMPUS_CS_SHARPNESS_SETTING: {
                final String[] values = value.getValues();
                if ( values.length != 3 )
                    return null;
                return values[0] + " (" + values[1] + ',' + values[2] + ')';
            }
            case OLYMPUS_CS_PANORAMA_MODE: {
                if ( value.getValueCount() != 2 )
                    break;
                final int tagID = value.getOwningTagID();
                return  getTagValueLabelFor( tagID, value.getIntValue() )
                        + ", " + value.getValues()[1];
            }
            case OLYMPUS_FOCAL_PLANE_DIAGONAL:
                return value.getStringValue() + "mm";   // TODO: localize
        }
        return super.valueToString( value );
    }

    ////////// protected //////////////////////////////////////////////////////

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
        return OlympusTags.class;
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
        "com.lightcrafts.image.metadata.makernotes.OlympusTags"
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
        add( OLYMPUS_APERTURE, "Aperture", META_SRATIONAL );
        add( OLYMPUS_BLACK_AND_WHITE_MODE, "BlackAndWhiteMode", META_USHORT );
        add( OLYMPUS_BLACK_LEVEL, "BlackLevel", META_USHORT );
        add( OLYMPUS_BLUE_BALANCE, "BlueBalance", META_USHORT );
        add( OLYMPUS_BRIGHTNESS, "Brightness", META_SRATIONAL );
        add( OLYMPUS_CAMERA_ID, "CameraID", META_UNKNOWN );
        add( OLYMPUS_CAMERA_SETTINGS, "CameraSettings", META_UNDEFINED );
        add( OLYMPUS_CAMERA_SETTINGS_OLD, "CameraSettingsOld", META_UNDEFINED );
        add( OLYMPUS_CAMERA_TYPE, "CameraType", META_UNKNOWN );
        add( OLYMPUS_CCD_SCAN_MODE, "CCDScanMode", META_USHORT );
        add( OLYMPUS_COLOR_MODE, "ColorMode", META_ULONG );
        add( OLYMPUS_COMPRESSED_IMAGE_SIZE, "CompressedImageSize", META_ULONG );
        add( OLYMPUS_COMPRESSION_RATIO, "CompressionRatio", META_URATIONAL );
        add( OLYMPUS_CONTRAST, "Contrast", META_USHORT );
        add( OLYMPUS_CORING_FILTER, "CoringFilter", META_USHORT );
        add( OLYMPUS_CS_PREVIEW_IMAGE_VALID, "CSPreviewImageValid", META_ULONG );
        add( OLYMPUS_CS_AE_LOCK, "CSAELock", META_USHORT );
        add( OLYMPUS_CS_AF_AREAS, "CSAFAreas", META_ULONG );
        add( OLYMPUS_CS_AF_SEARCH, "CSAFSearch", META_USHORT );
        add( OLYMPUS_CS_COLORSPACE, "CSColorspace", META_USHORT );
        add( OLYMPUS_CS_COMPRESSION_FACTOR, "CSCompressionFactor", META_URATIONAL );
        add( OLYMPUS_CS_CONTRAST_SETTING, "CSContrastSetting", META_SSHORT );
        add( OLYMPUS_CS_CUSTOM_SATURATION, "CSCustomSaturation", META_SSHORT );
        add( OLYMPUS_CS_DISTORTION_CORRECTION, "CSDistortionCorrection", META_USHORT );
        add( OLYMPUS_CS_EXPOSURE_MODE, "CSExposureMode", META_USHORT );
        add( OLYMPUS_CS_FLASH_EXPOSURE_COMPENSATION, "CSFlashExposureCompensation", META_SRATIONAL );
        add( OLYMPUS_CS_FLASH_MODE, "CSFlashMode", META_USHORT );
        add( OLYMPUS_CS_FOCUS_MODE, "CSFocusMode", META_USHORT );
        add( OLYMPUS_CS_FOCUS_PROCESS, "CSFocusProcess", META_USHORT );
        add( OLYMPUS_CS_GRADATION, "CSGradation", META_USHORT );
        add( OLYMPUS_CS_IMAGE_QUALITY_2, "CSImageQuality2", META_USHORT );
        add( OLYMPUS_CS_MACRO_MODE, "CSMacroMode", META_USHORT );
        add( OLYMPUS_CS_METERING_MODE, "CSMeteringMode", META_USHORT );
        add( OLYMPUS_CS_MODIFIED_SATURATION, "CSModifiedSaturation", META_USHORT );
        add( OLYMPUS_CS_NOISE_REDUCTION, "CSNoiseReduction", META_USHORT );
        add( OLYMPUS_CS_PANORAMA_MODE, "CSPanoramaMode", META_USHORT );
        add( OLYMPUS_CS_PICTURE_MODE, "CSPictureMode", META_USHORT );
        add( OLYMPUS_CS_PM_BW_FILTER, "CSPMBWFilter", META_SSHORT );
        add( OLYMPUS_CS_PM_CONTRAST, "CSPMContrast", META_SSHORT );
        add( OLYMPUS_CS_PM_HUE, "CSPMHue", META_SSHORT );
        add( OLYMPUS_CS_PM_SATURATION, "CSPMSaturation", META_SSHORT );
        add( OLYMPUS_CS_PM_SHARPNESS, "CSPMSharpness", META_SSHORT );
        add( OLYMPUS_CS_PM_TONE, "CSPMTone", META_SSHORT );
        add( OLYMPUS_CS_PREVIEW_IMAGE_LENGTH, "CSPreviewImageLength", META_ULONG );
        add( OLYMPUS_CS_PREVIEW_IMAGE_START, "CSPreviewImageStart", META_ULONG );
        add( OLYMPUS_CS_SCENE_MODE, "CSSceneMode", META_USHORT );
        add( OLYMPUS_CS_SEQUENCE, "CSSequence", META_USHORT );
        add( OLYMPUS_CS_SHADING_COMPENSATION, "CSShadingCompensation", META_USHORT );
        add( OLYMPUS_CS_SHARPNESS_SETTING, "CSSharpnessSetting", META_SSHORT );
        add( OLYMPUS_CS_WHITE_BALANCE, "CSWhiteBalance", META_USHORT );
        add( OLYMPUS_CS_WHITE_BALANCE_BRACKET, "CSWhiteBalanceBracket", META_SSHORT );
        add( OLYMPUS_CS_WHITE_BALANCE_TEMP, "CSWhiteBalanceTemp", META_USHORT );
        add( OLYMPUS_CS_VERSION, "CSVersion", META_UNDEFINED );
        add( OLYMPUS_DIGITAL_ZOOM, "DigitalZoom", META_URATIONAL );
        add( OLYMPUS_EPSON_IMAGE_HEIGHT, "EpsonImageHeight", META_USHORT );
        add( OLYMPUS_EPSON_IMAGE_WIDTH, "EpsonImageWidth", META_USHORT );
        add( OLYMPUS_EPSON_SOFTWARE, "EpsonSoftware", META_STRING );
        add( OLYMPUS_EXPOSURE_COMPENSATION, "ExposureCompensation", META_SRATIONAL );
        add( OLYMPUS_EXTERNAL_FLASH_BOUNCE, "ExternalFlashBounce", META_USHORT );
        add( OLYMPUS_EXTERNAL_FLASH_MODE, "ExternalFlashMode", META_USHORT );
        add( OLYMPUS_EXTERNAL_FLASH_ZOOM, "ExternalFlashZoom", META_USHORT );
        add( OLYMPUS_FLASH_CHARGE_LEVEL, "FlashChargeLevel", META_USHORT );
        add( OLYMPUS_FLASH_DEVICE, "FlashDevice", META_USHORT );
        add( OLYMPUS_FLASH_EXPOSURE_COMPENSATION, "FlashExposureCompensation", META_SRATIONAL );
        add( OLYMPUS_FLASH_MODE, "FlashMode", META_USHORT );
        add( OLYMPUS_FOCAL_PLANE_DIAGONAL, "FocalPlaneDiagonal", META_URATIONAL );
        add( OLYMPUS_FOCUS_MODE, "FocusMode", META_USHORT );
        add( OLYMPUS_FOCUS_STEP_COUNT, "FocusStepCount", META_USHORT );
        add( OLYMPUS_IMAGE_HEIGHT, "ImageHeight", META_ULONG );
        add( OLYMPUS_IMAGE_SIZE, "ImageSize", META_ULONG );
        add( OLYMPUS_IMAGE_WIDTH, "ImageWidth", META_ULONG );
        add( OLYMPUS_INFINITY_LENS_STEP, "InfinityLensStep", META_USHORT );
        add( OLYMPUS_ISO, "ISO", META_SRATIONAL );
        add( OLYMPUS_LENS_DISTORTION_PARAMS, "LensDistortionParams", META_SSHORT );
        add( OLYMPUS_LENS_TEMPERATURE, "LensTemperature", META_SSHORT );
        add( OLYMPUS_MACRO_MODE, "MacroMode", META_USHORT );
        add( OLYMPUS_MAKER_NOTES_VERSION, "MakerNotesVersion", META_UNDEFINED );
        add( OLYMPUS_MANUAL_FOCUS_DISTANCE, "ManualFocusDistance", META_URATIONAL );
        add( OLYMPUS_NEAR_LENS_STEP, "NearLensStep", META_USHORT );
        add( OLYMPUS_NOISE_REDUCTION, "NoiseReduction", META_USHORT );
        add( OLYMPUS_ONE_TOUCH_WHITE_BALANCE, "OneTouchWhiteBalance", META_USHORT );
        add( OLYMPUS_PREVIEW_IMAGE_LENGTH, "PreviewImageLength", META_ULONG );
        add( OLYMPUS_PREVIEW_IMAGE_LENGTH_2, "PreviewImageLength2", META_ULONG );
        add( OLYMPUS_PREVIEW_IMAGE_START, "PreviewImageStart", META_ULONG );
        add( OLYMPUS_PREVIEW_IMAGE_START_2, "PreviewImageStart2", META_ULONG );
        add( OLYMPUS_PREVIEW_IMAGE_VALID, "PreviewImageValid", META_ULONG );
        add( OLYMPUS_QUALITY, "Quality", META_USHORT );
        add( OLYMPUS_QUALITY_2, "Quality2", META_USHORT );
        add( OLYMPUS_RED_BALANCE, "RedBalance", META_USHORT );
        add( OLYMPUS_SENSOR_TEMPERATURE, "SensorTemperature", META_SSHORT );
        add( OLYMPUS_SERIAL_NUMBER, "SerialNumber", META_STRING );
        add( OLYMPUS_SERIAL_NUMBER_2, "SerialNumber2", META_STRING );
        add( OLYMPUS_SHARPNESS, "Sharpness", META_USHORT );
        add( OLYMPUS_SHARPNESS_FACTOR, "SharpnessFactor", META_USHORT );
        add( OLYMPUS_SHUTTER_SPEED, "ShutterSpeed", META_SRATIONAL );
        add( OLYMPUS_TEXT_INFO, "TextInfo", META_UNDEFINED );
        add( OLYMPUS_WHITE_BALANCE_MODE, "WhiteBalanceMode", META_USHORT );
        add( OLYMPUS_ZOOM_STEP_COUNT, "ZoomStepCount", META_USHORT );
    }
}
/* vim:set et sw=4 ts=4: */
