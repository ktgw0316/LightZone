/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.makernotes;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.providers.FlashProvider;
import com.lightcrafts.image.metadata.providers.ISOProvider;
import com.lightcrafts.image.metadata.providers.LensProvider;
import com.lightcrafts.image.metadata.providers.PreviewImageProvider;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.image.types.ORFImageType;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.TextUtil;

import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.makernotes.OlympusConstants.*;
import static com.lightcrafts.image.metadata.makernotes.OlympusTags.*;

/**
 * An {@code OlympusDirectory} is-an {@link ImageMetadataDirectory} for
 * holding Olympus-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class OlympusDirectory extends MakerNotesDirectory implements
        FlashProvider, ISOProvider, LensProvider, PreviewImageProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the state of the flash at the time the image was captured.  The
     * value returned is the value of {@link TIFFTags#TIFF_FLASH TIFF_FLASH}.
     * <p>
     * Note that Olympus cameras don't seem to say whether the flash actually
     * fired.
     *
     * @return Returns the flash state or -1 if it's unavailable.
     */
    @Override
    public int getFlash() {
        int flashBits = FLASH_NOT_PRESENT_BIT;

        ImageMetaValue flashValue = getValue( OLYMPUS_FLASH_DEVICE );
        if ( flashValue != null && flashValue.getIntValue() > 0 )
            flashBits &= ~FLASH_NOT_PRESENT_BIT;

        flashValue = getValue( OLYMPUS_E_FLASH_TYPE );
        if ( flashValue != null && flashValue.getIntValue() > 0 )
            flashBits &= ~FLASH_NOT_PRESENT_BIT;

        flashValue = getValue( OLYMPUS_CS_FLASH_MODE );
        if ( flashValue != null ) {
            flashBits &= ~FLASH_NOT_PRESENT_BIT;
            final int olympusFlashBits = flashValue.getIntValue();
            if ( (olympusFlashBits & OLYMPUS_FLASH_COMPULSORY_BIT) != 0 )
                flashBits |= 1 << 3;
            if ( (olympusFlashBits & OLYMPUS_FLASH_RED_EYE_BIT) != 0 )
                flashBits |= FLASH_RED_EYE_BIT;
        }
        return flashBits != FLASH_NOT_PRESENT_BIT ? flashBits : -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getISO() {
        final ImageMetaValue value = getValue(OLYMPUS_ISO);
        if (value == null || hasTagValueLabelFor(value) != null) {
            return 0;
        }
        return value.getIntValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLens() {
        final String label = hasTagValueLabelFor(OLYMPUS_E_LENS_TYPE);
        if (label != null) {
            return label;
        }
        return makeLensLabelFrom(
                getValue(OLYMPUS_E_MIN_FOCAL_LENGTH),
                getValue(OLYMPUS_E_MIN_FOCAL_LENGTH),
                null
        );
    }

    /**
     * Gets the maker-notes adjustments for Olympus.
     *
     * @param buf The {@link LCByteBuffer} the metadata is in.
     * @param offset The offset to the start of the maker-notes.
     * @return Returns said adjustments.
     */
    @Override
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
    @Override
    public String getName() {
        return "Olympus";
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * Puts a key/value pair into this directory.  For a Olympus tag that has
     * subfields, expands the values into multiple key/value pairs.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The value to put.
     * @see #valueToString(ImageMetaValue)
     */
    @Override
    public void putValue( Integer tagID, ImageMetaValue value ) {
        switch ( tagID ) {
            case OLYMPUS_CS_NOISE_FILTER:
                value = new ShortMetaValue( value.getIntValue() );
                break;
            case OLYMPUS_E_EXTENDER_TYPE:
                if ( value.getValueCount() == 6 ) {
                    final int make = value.getIntValueAt( 0 );
                    final int model = value.getIntValueAt( 2 );
                    final int id = make * 100 + model;
                    value = new UnsignedLongMetaValue( id );
                }
                break;
            case OLYMPUS_E_LENS_TYPE:
                if ( value.getValueCount() == 6 ) {
                    final int make = value.getIntValueAt( 0 );
                    final int model = value.getIntValueAt( 2 );
                    final int submodel = value.getIntValueAt( 3 );
                    final int id = make * 10000 + model * 100 + submodel;
                    value = new LongMetaValue( id );
                }
                break;
            case OLYMPUS_MINOLTA_CAMERA_SETTINGS:
            case OLYMPUS_MINOLTA_CAMERA_SETTINGS_OLD:
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
    @Override
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case OLYMPUS_BLUE_BALANCE:
            case OLYMPUS_RED_BALANCE:
                return TextUtil.tenths( value.getIntValue() / 256.0 );
            case OLYMPUS_CS_MANOMETER_PRESSURE:
                return TextUtil.tenths( value.getIntValue() / 10.0 ) + " kPa";
            case OLYMPUS_E_FOCAL_PLANE_DIAGONAL:
                return TextUtil.tenths( value.getDoubleValue() ) + "mm";
            case OLYMPUS_E_MAX_APERTURE_AT_CUR_FOCAL:
            case OLYMPUS_E_MAX_APERTURE_AT_MAX_FOCAL:
            case OLYMPUS_E_MAX_APERTURE_AT_MIN_FOCAL: {
                final int n = value.getIntValue();
                final double d = Math.pow( Math.sqrt(2), n / 256.0 );
                return "F/" + TextUtil.tenths( d );
            }
            case OLYMPUS_E_MAX_FOCAL_LENGTH:
            case OLYMPUS_E_MIN_FOCAL_LENGTH:
                return value.getStringValue() + "mm";
            case OLYMPUS_MCS_CUSTOM_SATURATION: {
                final String model = getOwningMetadata().getCameraMake( true );
                if ( model != null && model.contains( "E-1" ) ) {
                    final long[] n = ((LongMetaValue)value).getLongValues();
                    n[0] -= n[1];
                    n[2] -= n[1];
                    return n[0] + " (0," + n[2] + ')';
                }
                // no break;
            }
            case OLYMPUS_MCS_CONTRAST_SETTING:
            case OLYMPUS_MCS_PM_CONTRAST:
            case OLYMPUS_MCS_PM_SATURATION:
            case OLYMPUS_MCS_PM_SHARPNESS:
            case OLYMPUS_MCS_SHARPNESS_SETTING: {
                final String[] values = value.getValues();
                if ( values.length != 3 )
                    return null;
                return values[0] + " (" + values[1] + ',' + values[2] + ')';
            }
            case OLYMPUS_MCS_PANORAMA_MODE: {
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
     * {@inheritDoc}
     */
    @Override
    protected ImageMetaValue getLongFocalValue() {
        return getValue( OLYMPUS_E_MAX_FOCAL_LENGTH );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImageMetaValue getShortFocalValue() {
        return getValue( OLYMPUS_E_MIN_FOCAL_LENGTH );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ImageMetaValue getMaxApertureValue() {
        return getValue( OLYMPUS_E_MAX_APERTURE_AT_MAX_FOCAL );
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
        add( OLYMPUS_AUTO_FOCUS_RESULT, "AutoFocusResult", META_USHORT );
        add( OLYMPUS_BLACK_AND_WHITE_MODE, "BlackAndWhiteMode", META_USHORT );
        add( OLYMPUS_BLACK_LEVEL, "BlackLevel", META_USHORT );
        add( OLYMPUS_BLUE_BALANCE, "BlueBalance", META_USHORT );
        add( OLYMPUS_BODY_FIRMWARE_VERSION, "BodyFirmwareVersion", META_STRING );
        add( OLYMPUS_BRIGHTNESS, "Brightness", META_SRATIONAL );
        add( OLYMPUS_CAMERA_ID, "CameraID", META_UNKNOWN );
        add( OLYMPUS_CAMERA_SETTINGS, "CameraSettings", META_UNDEFINED );
        add( OLYMPUS_CAMERA_TYPE, "CameraType", META_UNKNOWN );
        add( OLYMPUS_CCD_SCAN_MODE, "CCDScanMode", META_USHORT );
        add( OLYMPUS_COLOR_MATRIX_NUMBER, "ColorMatrixNumber", META_USHORT );
        add( OLYMPUS_COLOR_MODE, "ColorMode", META_ULONG );
        add( OLYMPUS_COMPRESSED_IMAGE_SIZE, "CompressedImageSize", META_ULONG );
        add( OLYMPUS_COMPRESSION_RATIO, "CompressionRatio", META_URATIONAL );
        add( OLYMPUS_CONTRAST, "Contrast", META_USHORT );
        add( OLYMPUS_CORING_FILTER, "CoringFilter", META_USHORT );
        add( OLYMPUS_CS_AE_LOCK, "CSAELock", META_USHORT );
        add( OLYMPUS_CS_AF_AREAS, "CSAFAreas", META_ULONG );
        add( OLYMPUS_CS_AF_POINT_SELECTED, "CSAFPointSelected", META_SRATIONAL );
        add( OLYMPUS_CS_AF_SEARCH, "CSAFSearch", META_USHORT );
        add( OLYMPUS_CS_ART_FILTER, "CSArtFilter", META_USHORT );
        add( OLYMPUS_CS_COLOR_SPACE, "CSColorSpace", META_USHORT );
        add( OLYMPUS_CS_COMPRESSION_FACTOR, "CSCompressionFactor", META_URATIONAL );
        add( OLYMPUS_CS_CONTRAST_SETTING, "CSContrastSetting", META_SSHORT );
        add( OLYMPUS_CS_CUSTOM_SATURATION, "CScustomSaturation", META_SSHORT );
        add( OLYMPUS_CS_DISTORTION_CORRECTION, "CSDistortionCorrection", META_USHORT );
        add( OLYMPUS_CS_DRIVE_MODE, "CSDriveMode", META_USHORT );
        add( OLYMPUS_CS_EXPOSURE_MODE, "CSExposureMode", META_USHORT );
        add( OLYMPUS_CS_EXPOSURE_SHIFT, "CSExposureShift", META_SRATIONAL );
        add( OLYMPUS_CS_EXTENDED_WB_DETECT, "CSExtendedWBDetect", META_USHORT );
        add( OLYMPUS_CS_FLASH_CONTROL_MODE, "CSFlashControlMode", META_USHORT );
        add( OLYMPUS_CS_FLASH_EXPOSURE_COMP, "CSFLashExposureComp", META_SRATIONAL );
        add( OLYMPUS_CS_FLASH_INTENSITY, "CSFlashIntensity", META_SRATIONAL );
        add( OLYMPUS_CS_FLASH_MODE, "CSFlashMode", META_USHORT );
        add( OLYMPUS_CS_FLASH_REMOTE_CONTROL, "CSFlashRemoteControl", META_USHORT );
        add( OLYMPUS_CS_FOCUS_MODE, "CSFocusMode", META_USHORT );
        add( OLYMPUS_CS_FOCUS_PROCESS, "CSfocusProcess", META_USHORT );
        add( OLYMPUS_CS_GRADATION, "CSGradation", META_SSHORT );
        add( OLYMPUS_CS_IMAGE_QUALITY_2, "CSImageQuality2", META_USHORT );
        add( OLYMPUS_CS_IMAGE_STABILIZATION, "CSImageStabilization", META_ULONG );
        add( OLYMPUS_CS_LEVEL_GAUGE_PITCH, "CSLevelGaugePitch", META_USHORT );
        add( OLYMPUS_CS_LEVEL_GAUGE_ROLL, "CSLevelGaugeRoll", META_USHORT );
        add( OLYMPUS_CS_MACRO_MODE, "CSMacroMode", META_USHORT );
        add( OLYMPUS_CS_MANOMETER_PRESSURE, "CSManometerPressure", META_USHORT );
        add( OLYMPUS_CS_MANUAL_FLASH_STRENGTH, "CSManualFlashStrength", META_USHORT );
        add( OLYMPUS_CS_METERING_MODE, "CSMeteringMode", META_USHORT );
        add( OLYMPUS_CS_MODIFIED_SATURATION, "CSModifiedSaturation", META_USHORT );
        add( OLYMPUS_CS_NOISE_FILTER, "CSNoiseFilter", META_SSHORT );
        add( OLYMPUS_CS_NOISE_REDUCTION, "CSNoiseReduction", META_USHORT );
        add( OLYMPUS_CS_PANORAMA_MODE, "CSPanoramaMode", META_USHORT );
        add( OLYMPUS_CS_PICTURE_MODE, "CSPictureMode", META_USHORT );
        add( OLYMPUS_CS_PM_BW_FILTER, "CSPMBWFilter", META_SSHORT );
        add( OLYMPUS_CS_PM_CONTRAST, "CSPMContrasT", META_SSHORT );
        add( OLYMPUS_CS_PM_HUE, "CSPMHue", META_SSHORT );
        add( OLYMPUS_CS_PM_SATURATION, "CSPMSaturation", META_SSHORT );
        add( OLYMPUS_CS_PM_SHARPNESS, "CSPMSharpness", META_SSHORT );
        add( OLYMPUS_CS_PM_TONE, "CSPMTone", META_SSHORT );
        add( OLYMPUS_CS_PREVIEW_IMAGE_LENGTH, "CSPreviewImageLength", META_ULONG );
        add( OLYMPUS_CS_PREVIEW_IMAGE_START, "CSPreviewImageStart", META_ULONG );
        add( OLYMPUS_CS_PREVIEW_IMAGE_VALID, "CSPreviewImageValid", META_ULONG );
        add( OLYMPUS_CS_SCENE_MODE, "CSSceneMode", META_USHORT );
        add( OLYMPUS_CS_SHADING_COMPENSATION, "CSShadingCompensation", META_USHORT );
        add( OLYMPUS_CS_SHARPNESS_SETTING, "CSSharpnessSetting", META_SSHORT );
        add( OLYMPUS_CS_VERSION, "CSVersion", META_UNDEFINED );
        add( OLYMPUS_CS_WHITE_BALANCE_2, "CSWhiteBalance2", META_USHORT );
        add( OLYMPUS_CS_WHITE_BALANCE_TEMP, "CSWhiteBalanceTemp", META_USHORT );
        add( OLYMPUS_DIGITAL_ZOOM, "DigitalZoom", META_URATIONAL );
        add( OLYMPUS_E_EXTENDER_FIRMWARE_VERSION, "EExtenderFirmwareVersion", META_ULONG );
        add( OLYMPUS_E_EXTENDER_MODEL, "EExtenderModel", META_STRING );
        add( OLYMPUS_E_EXTENDER_SERIAL_NUMBER, "EExtenderSerialNumber", META_STRING );
        add( OLYMPUS_E_EXTENDER_TYPE, "EExtenderType", META_UBYTE );
        add( OLYMPUS_E_FLASH_FIRMWARE_VERSION, "EFlashFirmwareVersion", META_ULONG );
        add( OLYMPUS_E_FLASH_MODEL, "EFlashModel", META_USHORT );
        add( OLYMPUS_E_FLASH_SERIAL_NUMBER, "EFlashSerialNumber", META_STRING );
        add( OLYMPUS_E_FLASH_TYPE, "EFlashType", META_USHORT );
        add( OLYMPUS_E_FOCAL_PLANE_DIAGONAL, "EFocalPlaneDiagonal", META_URATIONAL );
        add( OLYMPUS_E_INTERNAL_SERIAL_NUMBER, "EInternalSerialNumber", META_STRING );
        add( OLYMPUS_E_LENS_FIRMWARE_VERSION, "ELensFirmwareVersion", META_ULONG );
        add( OLYMPUS_E_LENS_PROPERTIES, "ELensProperties", META_USHORT );
        add( OLYMPUS_E_LENS_SERIAL_NUMBER, "ELensSerialNumber", META_STRING );
        add( OLYMPUS_E_LENS_TYPE, "ELensType", META_UBYTE );
        add( OLYMPUS_E_MAX_APERTURE_AT_CUR_FOCAL, "EMaxApertureAtCurFocal", META_USHORT );
        add( OLYMPUS_E_MAX_APERTURE_AT_MAX_FOCAL, "EMaxApertureAtMaxFocal", META_USHORT );
        add( OLYMPUS_E_MAX_APERTURE_AT_MIN_FOCAL, "EMaxApertureAtMinFocal", META_USHORT );
        add( OLYMPUS_E_MAX_FOCAL_LENGTH, "EMaxFocalLength", META_USHORT );
        add( OLYMPUS_E_MIN_FOCAL_LENGTH, "EMinFocalLength", META_USHORT );
        add( OLYMPUS_EPSON_IMAGE_HEIGHT, "EpsonImageHeight", META_USHORT );
        add( OLYMPUS_EPSON_IMAGE_WIDTH, "EpsonImageWidth", META_USHORT );
        add( OLYMPUS_EPSON_SOFTWARE, "EpsonSoftware", META_STRING );
        add( OLYMPUS_E_SERIAL_NUMBER, "ESerialNumber", META_STRING );
        add( OLYMPUS_E_VERSION, "EVersion", META_UNDEFINED );
        add( OLYMPUS_EXPOSURE_COMPENSATION, "ExposureCompensation", META_SRATIONAL );
        add( OLYMPUS_EXTERNAL_FLASH_BOUNCE, "ExternalFlashBounce", META_USHORT );
        add( OLYMPUS_EXTERNAL_FLASH_MODE, "ExternalFlashMode", META_USHORT );
        add( OLYMPUS_EXTERNAL_FLASH_ZOOM, "ExternalFlashZoom", META_USHORT );
        add( OLYMPUS_FIRMWARE, "Firmware", META_STRING );
        add( OLYMPUS_FLASH_CHARGE_LEVEL, "FlashChargeLevel", META_USHORT );
        add( OLYMPUS_FLASH_DEVICE, "FlashDevice", META_USHORT );
        add( OLYMPUS_FLASH_EXPOSURE_COMPENSATION, "FlashExposureCompensation", META_SRATIONAL );
        add( OLYMPUS_FLASH_MODE, "FlashMode", META_USHORT );
        add( OLYMPUS_FOCAL_PLANE_DIAGONAL, "FocalPlaneDiagonal", META_URATIONAL );
        add( OLYMPUS_FOCUS_MODE, "FocusMode", META_USHORT );
        add( OLYMPUS_FOCUS_RANGE, "FocusRange", META_USHORT );
        add( OLYMPUS_FOCUS_STEP_COUNT, "FocusStepCount", META_USHORT );
        add( OLYMPUS_IMAGE_HEIGHT, "ImageHeight", META_ULONG );
        add( OLYMPUS_IMAGE_SIZE, "ImageSize", META_ULONG );
        add( OLYMPUS_IMAGE_WIDTH, "ImageWidth", META_ULONG );
        add( OLYMPUS_INFINITY_LENS_STEP, "InfinityLensStep", META_USHORT );
        add( OLYMPUS_ISO, "ISO", META_SRATIONAL );
        add( OLYMPUS_LENS_DISTORTION_PARAMS, "LensDistortionParams", META_SSHORT );
        add( OLYMPUS_LENS_TEMPERATURE, "LensTemperature", META_SSHORT );
        add( OLYMPUS_LIGHT_CONDITION, "LightCondition", META_USHORT );
        add( OLYMPUS_LIGHT_VALUE_CENTER, "LightValueCenter", META_SRATIONAL );
        add( OLYMPUS_LIGHT_VALUE_PERIPHERY, "LightValuePeriphery", META_SRATIONAL );
        add( OLYMPUS_MACRO_MODE, "MacroMode", META_USHORT );
        add( OLYMPUS_MAKER_NOTES_VERSION, "MakerNotesVersion", META_UNDEFINED );
        add( OLYMPUS_MANUAL_FOCUS_DISTANCE, "ManualFocusDistance", META_URATIONAL );
        add( OLYMPUS_MCS_AE_LOCK, "MCSAELock", META_USHORT );
        add( OLYMPUS_MCS_AF_AREAS, "MCSAFAreas", META_ULONG );
        add( OLYMPUS_MCS_AF_SEARCH, "MCSAFSearch", META_USHORT );
        add( OLYMPUS_MCS_COLORSPACE, "MCSColorspace", META_USHORT );
        add( OLYMPUS_MCS_COMPRESSION_FACTOR, "MCSCompressionFactor", META_URATIONAL );
        add( OLYMPUS_MCS_CONTRAST_SETTING, "MCSContrastSetting", META_SSHORT );
        add( OLYMPUS_MCS_CUSTOM_SATURATION, "MCSCustomSaturation", META_SSHORT );
        add( OLYMPUS_MCS_DISTORTION_CORRECTION, "MCSDistortionCorrection", META_USHORT );
        add( OLYMPUS_MCS_EXPOSURE_MODE, "MCSExposureMode", META_USHORT );
        add( OLYMPUS_MCS_FLASH_EXPOSURE_COMPENSATION, "MCSFlashExposureCompensation", META_SRATIONAL );
        add( OLYMPUS_MCS_FLASH_MODE, "MCSFlashMode", META_USHORT );
        add( OLYMPUS_MCS_FOCUS_MODE, "MCSFocusMode", META_USHORT );
        add( OLYMPUS_MCS_FOCUS_PROCESS, "MCSFocusProcess", META_USHORT );
        add( OLYMPUS_MCS_GRADATION, "MCSGradation", META_USHORT );
        add( OLYMPUS_MCS_IMAGE_QUALITY_2, "MCSImageQuality2", META_USHORT );
        add( OLYMPUS_MCS_MACRO_MODE, "MCSMacroMode", META_USHORT );
        add( OLYMPUS_MCS_METERING_MODE, "MCSMeteringMode", META_USHORT );
        add( OLYMPUS_MCS_MODIFIED_SATURATION, "MCSModifiedSaturation", META_USHORT );
        add( OLYMPUS_MCS_NOISE_REDUCTION, "MCSNoiseReduction", META_USHORT );
        add( OLYMPUS_MCS_PANORAMA_MODE, "MCSPanoramaMode", META_USHORT );
        add( OLYMPUS_MCS_PICTURE_MODE, "MCSPictureMode", META_USHORT );
        add( OLYMPUS_MCS_PM_BW_FILTER, "MCSPMBWFilter", META_SSHORT );
        add( OLYMPUS_MCS_PM_CONTRAST, "MCSPMContrast", META_SSHORT );
        add( OLYMPUS_MCS_PM_HUE, "MCSPMHue", META_SSHORT );
        add( OLYMPUS_MCS_PM_SATURATION, "MCSPMSaturation", META_SSHORT );
        add( OLYMPUS_MCS_PM_SHARPNESS, "MCSPMSharpness", META_SSHORT );
        add( OLYMPUS_MCS_PM_TONE, "MCSPMTone", META_SSHORT );
        add( OLYMPUS_MCS_PREVIEW_IMAGE_LENGTH, "MCSPreviewImageLength", META_ULONG );
        add( OLYMPUS_MCS_PREVIEW_IMAGE_START, "MCSPreviewImageStart", META_ULONG );
        add( OLYMPUS_MCS_PREVIEW_IMAGE_VALID, "MCSPreviewImageValid", META_ULONG );
        add( OLYMPUS_MCS_SCENE_MODE, "MCSSceneMode", META_USHORT );
        add( OLYMPUS_MCS_SEQUENCE, "MCSSequence", META_USHORT );
        add( OLYMPUS_MCS_SHADING_COMPENSATION, "MCSShadingCompensation", META_USHORT );
        add( OLYMPUS_MCS_SHARPNESS_SETTING, "MCSSharpnessSetting", META_SSHORT );
        add( OLYMPUS_MCS_VERSION, "MCSVersion", META_UNDEFINED );
        add( OLYMPUS_MCS_WHITE_BALANCE, "MCSWhiteBalance", META_USHORT );
        add( OLYMPUS_MCS_WHITE_BALANCE_BRACKET, "MCSWhiteBalanceBracket", META_SSHORT );
        add( OLYMPUS_MCS_WHITE_BALANCE_TEMP, "MCSWhiteBalanceTemp", META_USHORT );
        add( OLYMPUS_MINOLTA_CAMERA_SETTINGS, "CameraSettings", META_UNDEFINED );
        add( OLYMPUS_MINOLTA_CAMERA_SETTINGS_OLD, "CameraSettingsOld", META_UNDEFINED );
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
        add( OLYMPUS_SCENE_DETECT, "SceneDetect", META_USHORT );
        add( OLYMPUS_SCENE_MODE, "SceneMode", META_USHORT );
        add( OLYMPUS_SENSOR_TEMPERATURE, "SensorTemperature", META_SSHORT );
        add( OLYMPUS_SERIAL_NUMBER, "SerialNumber", META_STRING );
        add( OLYMPUS_SERIAL_NUMBER_2, "SerialNumber2", META_STRING );
        add( OLYMPUS_SHARPNESS, "Sharpness", META_USHORT );
        add( OLYMPUS_SHARPNESS_FACTOR, "SharpnessFactor", META_USHORT );
        add( OLYMPUS_SHUTTER_SPEED, "ShutterSpeed", META_SRATIONAL );
        add( OLYMPUS_SPECIAL_MODE, "SpecialMode", META_ULONG );
        add( OLYMPUS_TEXT_INFO, "TextInfo", META_UNDEFINED );
        add( OLYMPUS_WHITE_BALANCE_BIAS, "WhiteBalanceBias", META_USHORT );
        add( OLYMPUS_WHITE_BALANCE_BRACKET, "WhiteBalanceBracket", META_USHORT );
        add( OLYMPUS_WHITE_BALANCE_MODE, "WhiteBalanceMode", META_USHORT );
        add( OLYMPUS_WHITE_BOARD, "WhiteBoard", META_USHORT );
        add( OLYMPUS_ZOOM_STEP_COUNT, "ZoomStepCount", META_USHORT );
    }
}
/* vim:set et sw=4 ts=4: */
