/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.ResourceBundle;

import com.lightcrafts.image.metadata.values.DateMetaValue;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.metadata.values.UnsignedShortMetaValue;
import com.lightcrafts.image.metadata.values.UnsignedLongMetaValue;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.utils.Rational;
import com.lightcrafts.utils.TextUtil;

import static com.lightcrafts.image.metadata.CIFFTags.*;
import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.ImageOrientation.*;
import static com.lightcrafts.image.types.CIFFConstants.*;

/**
 * A <code>CIFFDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding CIFF metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <i>CIFF 1.0r4</i>, Canon Incorporated, December 1997.
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class CIFFDirectory extends ImageMetadataDirectory implements
    ApertureProvider, ArtistProvider, BitsPerChannelProvider,
    CaptureDateTimeProvider, ColorTemperatureProvider, FocalLengthProvider,
    ISOProvider, LensProvider, MakeModelProvider, OrientationProvider,
    ShutterSpeedProvider, WidthHeightProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Convert the metadata values in this <code>CIFFDirectory</code> to those
     * in a {@link TIFFDirectory} and/or an {@link EXIFDirectory}.
     *
     * @param forJPEG Should be <code>true</code> if the converted metadata is
     * for a JPEG file; should be <code>false</code> for a TIFF file.
     * @return Returns a new {@link ImageMetadata} containing the converted
     * values.
     */
    public ImageMetadata convertMetadata( boolean forJPEG ) {
        return CIFFMetadataConverter.convert( this, forJPEG );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getAperture() {
        final ImageMetaValue value = getValue( CIFF_SI_FNUMBER );
        return  value != null ?
                (float)MetadataUtil.convertFStopFromAPEX( value.getIntValue() ) : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getArtist() {
        final ImageMetaValue value = getValue( CIFF_OWNER_NAME );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBitsPerChannel() {
        final ImageMetaValue value = getValue( CIFF_II_COMPONENT_BIT_DEPTH );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCameraMake( boolean includeModel ) {
        final ImageMetaValue value = getValue( CIFF_MAKE_MODEL );
        if ( value != null ) {
            String makeModel = value.getStringValue();
            final int makeNullPos = makeModel.indexOf( '\0' );
            if ( makeNullPos >= 0 ) {
                if ( includeModel ) {
                    final int modelPos = makeNullPos + 1;
                    int endPos = makeModel.indexOf( '\0', modelPos );
                    if ( endPos == -1 )
                        endPos = makeModel.length();
                    makeModel = MetadataUtil.undupMakeModel(
                        makeModel.substring( 0, makeNullPos ),
                        makeModel.substring( modelPos, endPos )
                    );
                }
                if ( !includeModel )
                    makeModel = makeModel.substring( 0, makeNullPos );
            }
            return makeModel.toUpperCase().trim();
        }
        return null;
    }

    /**
     * Gets the camera model only.
     *
     * @return Returns said model or <code>null</code> if unavailable.
     */
    public String getCameraModel() {
        final ImageMetaValue value = getValue( CIFF_MAKE_MODEL );
        if ( value == null )
            return null;
        String makeModel = value.getStringValue();
        final int nullPos = makeModel.indexOf( '\0' );
        if ( nullPos == -1 )
            return null;
        final int modelPos = nullPos + 1;
        int endPos = makeModel.indexOf( '\0', modelPos );
        if ( endPos == -1 )
            endPos = makeModel.length();
        makeModel = makeModel.substring( modelPos, endPos );
        return makeModel.toUpperCase();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getCaptureDateTime() {
        final ImageMetaValue value = getValue( CIFF_CAPTURED_TIME );
        return  value instanceof DateMetaValue ?
                ((DateMetaValue)value).getDateValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColorTemperature() {
        final ImageMetaValue value = getValue( CIFF_COLOR_TEMPERATURE );
        return value != null ? value.getIntValue() : 0;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFocalLength() {
        final ImageMetaValue value = getValue( CIFF_FL_FOCAL_LENGTH );
        return value != null ? value.getFloatValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageHeight() {
        ImageMetaValue value = getValue( CIFF_II_IMAGE_HEIGHT );
        if ( value == null )
            value = getValue( CIFF_PI_IMAGE_HEIGHT );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageWidth() {
        ImageMetaValue value = getValue( CIFF_II_IMAGE_WIDTH );
        if ( value == null )
            value = getValue( CIFF_PI_IMAGE_WIDTH );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getISO() {
        final ImageMetaValue value = getValue( CIFF_SI_ISO );
        if ( value != null )
            return MetadataUtil.convertISOFromAPEX( value.getIntValue() );
        final String isoLabel = hasTagValueLabelFor( CIFF_CS_ISO );
        if ( isoLabel != null )
            try {
                //
                // The reason for using parseInt() is because most of the
                // labels are really just the true integer ISO values: the
                // metadata value is just a key for a look-up table.  However,
                // a couple of the labels are strings like "auto".  Hence, if
                // we can parse an integer successfully, that's the true ISO
                // value; if not, we return 0.
                //
                return Integer.parseInt( isoLabel );
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
        final ImageMetaValue lensValue = getValue( CIFF_CS_LENS_TYPE );
        final String label = hasTagValueLabelFor( lensValue );
        if ( label != null )
            return label;
        return makeLensLabelFrom(
            getValue( CIFF_CS_SHORT_FOCAL_LENGTH ),
            getValue( CIFF_CS_LONG_FOCAL_LENGTH ),
            getValue( CIFF_CS_FOCAL_UNITS_PER_MM )
        );
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;CIFF&quot;.
     */
    @Override
    public String getName() {
        return "CIFF";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageOrientation getOrientation() {
        int orientation;
        ImageMetaValue value = getValue( CIFF_II_ROTATION );
        if ( value != null ) {
            orientation = value.getIntValue();
            if ( orientation < 0 )
                orientation += 360;
            switch ( orientation ) {
                case 0:
                    return ORIENTATION_LANDSCAPE;
                case 90:
                    return ORIENTATION_90CCW;
                case 180:
                    return ORIENTATION_180;
                case 270:
                    return ORIENTATION_90CW;
            }
        }
        value = getValue( CIFF_SI_AUTO_ROTATE );
        if ( value != null )
            switch ( value.getIntValue() ) {
                case CIFF_AUTO_ROTATE_NONE:
                    return ORIENTATION_LANDSCAPE;
                case CIFF_AUTO_ROTATE_90CCW:
                    return ORIENTATION_90CCW;
                case CIFF_AUTO_ROTATE_180:
                    return ORIENTATION_180;
                case CIFF_AUTO_ROTATE_90CW:
                    return ORIENTATION_90CW;
            }
        return ORIENTATION_UNKNOWN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getShutterSpeed() {
        final ImageMetaValue value = getValue( CIFF_SI_SHUTTER_SPEED );
        if ( value == null )
            return 0;
        final int apex = value.getIntValue();
        final Rational speed = MetadataUtil.convertShutterSpeedFromAPEX( apex );
        return speed.floatValue();
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
     * Puts a key/value pair into this directory.  For a Canon tag that has
     * subfields, expands the values into multiple key/value pairs.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The value to put.
     * @see #valueToString(ImageMetaValue)
     */
    @Override
    public void putValue( Integer tagID, ImageMetaValue value ) {
        switch ( tagID ) {
            case CIFF_BASE_ISO:
                //
                // The base ISO has "junk" values past the value of interest
                // (which is the first), so discard them.
                //
                value = new UnsignedShortMetaValue(
                    value.getUnsignedShortValue()
                );
                break;
            case CIFF_CAMERA_SETTINGS:
            case CIFF_CUSTOM_FUNCTIONS:
            case CIFF_SHOT_INFO:
            case CIFF_SENSOR_INFO:
                explodeSubfields( tagID, 1, value, false );
                return;
            case CIFF_CS_ISO:
                if ( value.getIntValue() == 0 )
                    return;             // Use CIFF_SI_ISO instead.
                break;
            case CIFF_IMAGE_FORMAT:
                //
                // The image format has "junk" values past the value of
                // interest (which is the first), so discard them.
                //
                value = new UnsignedLongMetaValue( value.getLongValue() );
                break;
            case CIFF_SI_ISO:
                if ( value.getIntValue() == 0 )
                    return;             // Use EXIF_ISO_SPEED_RATINGS instead.
                break;
            case CIFF_FOCAL_LENGTH:
            case CIFF_PICTURE_INFO:
                explodeSubfields( tagID, 1, value, true );
                return;
            case CIFF_IMAGE_INFO:
                explodeSubfields( tagID, 0, value, false );
                return;
            case CIFF_WHITE_BALANCE_TABLE:
                // TODO: handle this case
                return;
        }
        super.putValue( tagID, value );
    }

    /**
     * This method allows an <code>ImageMetadataDirectory</code> to alter the
     * <code>toString()</code> value of an {@link ImageMetaValue}.
     * <p>
     * In the case of the <code>CIFFDirectory</code>, some tags have subfields
     * and those subfields' integer values mean something.  This overridden
     * method substitutes what the values mean.
     *
     * @param value The {@link ImageMetaValue} whose value to convert to a
     * {@link String}.
     * @return Returns a string if said string is to be used for the
     * <code>toString()</value> of the {@link ImageMetaValue};
     * <code>null</code> otherwise.
     * @see #putValue(Integer,ImageMetaValue)
     */
    @Override
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case CIFF_CS_LONG_FOCAL_LENGTH:
            case CIFF_CS_SHORT_FOCAL_LENGTH:
            case CIFF_FL_FOCAL_LENGTH:
                return value.getStringValue() + "mm";   // TODO: localize "mm"
            case CIFF_MAKE_MODEL:
                return getCameraMake( true );
            case CIFF_SI_BULB_DURATION: {
                final int n = value.getIntValue();
                return TextUtil.tenths( n / 10F );
            }
            case CIFF_SI_FNUMBER: {
                final int apex = value.getIntValue();
                return TextUtil.tenths(
                    MetadataUtil.convertFStopFromAPEX( apex )
                );
            }
            case CIFF_SI_ISO: {
                final int apex = value.getIntValue();
                return Integer.toString(
                    MetadataUtil.convertISOFromAPEX( apex )
                );
            }
            case CIFF_SI_SHUTTER_SPEED: {
                final int apex = value.getIntValue();
                final Rational speed =
                    MetadataUtil.convertShutterSpeedFromAPEX( apex );
                return MetadataUtil.shutterSpeedString( speed.doubleValue() );
            }
            default:
                return super.valueToString( value );
        }
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Gets the priority of this directory for providing the metadata supplied
     * by implementing the given provider interface.
     * <p>
     * The priority is guaranteed to be lower than the priority for
     * {@link EXIFDirectory} and {@link TIFFDirectory}.
     *
     * @param provider The provider interface to get the priority for.
     * @return Returns said priority.
     */
    @Override
    protected int getProviderPriorityFor(
        Class<? extends ImageMetadataProvider> provider )
    {
        return PROVIDER_PRIORITY_DEFAULT - 10;
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
        return CIFFTags.class;
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
        "com.lightcrafts.image.metadata.CIFFTags"
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
        add( CIFF_BASE_ISO, "BaseISO", META_USHORT );
        add( CIFF_BODY_ID, "SerialNumber", META_ULONG );
        add( CIFF_CAMERA_OBJECT, "CameraObject", META_UNDEFINED );
        add( CIFF_CAMERA_SETTINGS, "CameraSettings", META_UNDEFINED );
        add( CIFF_CAMERA_SPECIFICATION, "CameraSpecification", META_UNDEFINED );
        add( CIFF_CAPTURED_TIME, "CapturedTime", META_DATE );
        add( CIFF_COLOR_SPACE, "ColorSpace", META_USHORT );
        add( CIFF_COLOR_TEMPERATURE, "ColorTemperature", META_USHORT );
        add( CIFF_CS_AF_POINT_SELECTED, "CSAFPointSelected", META_USHORT );
        add( CIFF_CS_CONTINUOUS_DRIVE_MODE, "CSContinuousDriveMode", META_USHORT );
        add( CIFF_CS_CONTRAST, "CSContrast", META_USHORT );
        add( CIFF_CS_DIGITAL_ZOOM, "CSDigitalZoom", META_USHORT );
        add( CIFF_CS_EASY_SHOOTING_MODE, "CSEasyShootingMode", META_USHORT );
        add( CIFF_CS_EXPOSURE_MODE, "CSExposureMode", META_USHORT );
        add( CIFF_CS_FLASH_ACTIVITY, "CSFlashActivity", META_USHORT );
        add( CIFF_CS_FLASH_DETAILS, "CSFlashDetails", META_USHORT );
        add( CIFF_CS_FLASH_MODE, "CSFlashMode", META_USHORT );
        add( CIFF_CS_FOCAL_UNITS_PER_MM, "CSFocalUnitsPerMM", META_USHORT );
        add( CIFF_CS_FOCUS_MODE, "CSFocusMode", META_USHORT );
        add( CIFF_CS_FOCUS_MODE_G1, "CSFocusModeG1", META_USHORT );
        add( CIFF_CS_FOCUS_TYPE, "CSFocusType", META_USHORT );
        add( CIFF_CS_IMAGE_SIZE, "CSImageSize", META_USHORT );
        add( CIFF_CS_ISO, "CSISO", META_USHORT );
        add( CIFF_CS_LENS_TYPE, "CSLensType", META_USHORT );
        add( CIFF_CS_LONG_FOCAL_LENGTH, "CSLongFocalLength", META_USHORT );
        add( CIFF_CS_MACRO_MODE, "CSMacroMode", META_USHORT );
        add( CIFF_CS_METERING_MODE, "CSMeteringMode", META_USHORT );
        add( CIFF_CS_QUALITY, "CSQuality", META_USHORT );
        add( CIFF_CS_SATURATION, "CSSaturation", META_USHORT );
        add( CIFF_CS_SELF_TIMER_DELAY, "CSSelfTimerDelay", META_USHORT );
        add( CIFF_CS_SHARPNESS, "CSSharpness", META_USHORT );
        add( CIFF_CS_SHORT_FOCAL_LENGTH, "CSShortFocalLength", META_USHORT );
        add( CIFF_CS_ZOOMED_RESOLUTION, "CSZoomedResolution", META_USHORT );
        add( CIFF_CS_ZOOMED_RESOLUTION_BASE, "CSZoomedResolutionBase", META_USHORT );
        add( CIFF_CUSTOM_FUNCTIONS, "CustomFunctions", META_UNDEFINED );
        add( CIFF_DECODER_TABLE, "DecoderTable", META_ULONG );
        add( CIFF_EXIF_INFORMATION, "EXIFInformation", META_UNDEFINED );
        add( CIFF_EXPOSURE_INFO, "ExposureInfo", META_ULONG );
        add( CIFF_FILE_DESCRIPTION, "FileDescription", META_STRING );
        add( CIFF_FILE_NUMBER, "FileNumber", META_ULONG );
        add( CIFF_FIRMWARE_VERSION, "FirmwareVersion", META_STRING );
        add( CIFF_FLASH_INFO, "FlashInfo", META_ULONG );
        add( CIFF_FL_FOCAL_LENGTH, "FLFocalLength", META_USHORT );
        add( CIFF_FL_FOCAL_PLANE_X_SIZE, "FLFocalPlaneXSize", META_USHORT );
        add( CIFF_FL_FOCAL_PLANE_Y_SIZE, "FLFocalPlaneYSize", META_USHORT );
        add( CIFF_FOCAL_LENGTH, "FocalLength", META_UNDEFINED );
        add( CIFF_II_COLOR_BIT_DEPTH, "IIColorBitDepth", META_ULONG );
        add( CIFF_II_COLOR_BW, "IIColorBw", META_ULONG );
        add( CIFF_II_COMPONENT_BIT_DEPTH, "IIComponentBitDepTH", META_ULONG );
        add( CIFF_II_IMAGE_HEIGHT, "IIImageHeight", META_ULONG );
        add( CIFF_II_IMAGE_WIDTH, "IIImageWidth", META_ULONG );
        add( CIFF_II_PIXEL_ASPECT_RATIO, "IIPixelAspectRatio", META_ULONG );
        add( CIFF_II_ROTATION, "IIRotation", META_ULONG );
        add( CIFF_IMAGE_DESCRIPTION, "ImageDescription", META_UNDEFINED );
        add( CIFF_IMAGE_FILE_NAME, "ImageFileName", META_STRING );
        add( CIFF_IMAGE_FORMAT, "ImageFormat", META_ULONG );
        add( CIFF_IMAGE_INFO, "ImageInfo", META_ULONG );
        add( CIFF_IMAGE_PROPS, "ImageProps", META_UNDEFINED );
        add( CIFF_IMAGE_TYPE, "ImageType", META_STRING );
        add( CIFF_JPG_FROM_RAW, "JPGFromRaw", META_UNDEFINED );
        add( CIFF_MAKE_MODEL, "MakeModel", META_STRING );
        add( CIFF_MEASURED_EV, "MeasuredEv", META_ULONG );
        add( CIFF_MEASURED_INFO, "MeasuredInfo", META_UNDEFINED );
        add( CIFF_OWNER_NAME, "OwnerName", META_STRING );
        add( CIFF_PICTURE_INFO, "PictureInfo", META_UNDEFINED );
        add( CIFF_PI_AF_POINTS_USED, "PIAfPointsUsed", META_USHORT );
        add( CIFF_PI_IMAGE_HEIGHT, "PIImageHeight", META_USHORT );
        add( CIFF_PI_IMAGE_HEIGHT_AS_SHOT, "PIImageHeightAsShot", META_USHORT );
        add( CIFF_PI_IMAGE_WIDTH, "PIImageWidth", META_USHORT );
        add( CIFF_PI_IMAGE_WIDTH_AS_SHOT, "PIImageWidthAsShot", META_USHORT );
        add( CIFF_PREVIEW_IMAGE_LENGTH, "PreviewImageLength", META_ULONG );
        add( CIFF_PREVIEW_IMAGE_OFFSET, "PreviewImageOffset", META_ULONG );
        add( CIFF_RAW_DATA, "RawData", META_UNDEFINED );
        add( CIFF_RECORD_ID, "RecordID", META_ULONG );
        add( CIFF_RELEASE_SETTING, "ReleaseSetting", META_USHORT );
        add( CIFF_ROM_OPERATION_MODE, "ROMOperationMode", META_STRING );
        add( CIFF_SELF_TIMER_TIME, "SelfTimerTime", META_ULONG );
        add( CIFF_SENSOR_INFO, "SensorInfo", META_UNDEFINED );
        add( CIFF_SHOOTING_RECORD, "ShootingRecord", META_UNDEFINED );
        add( CIFF_SHOT_INFO, "ShotInfo", META_UNDEFINED );
        add( CIFF_SHUTTER_RELEASE_METHOD, "ShutterReleaseMethod", META_USHORT );
        add( CIFF_SHUTTER_RELEASE_TIMING, "ShutterReleaseTiming", META_USHORT );
        add( CIFF_SI_AF_POINT_USED, "SIAfPointUsed", META_USHORT );
        add( CIFF_SI_AUTO_EXPOSURE_BRACKETING, "SIAutoExposureBracketing", META_USHORT );
        add( CIFF_SI_AUTO_ROTATE, "SIAutoRotate", META_USHORT );
        add( CIFF_SI_BULB_DURATION, "SIBulbDuration", META_USHORT );
        add( CIFF_SI_EXPOSURE_COMPENSATION, "SIExposureCompensation", META_USHORT );
        add( CIFF_SI_FLASH_BIAS, "SIFlashBias", META_USHORT );
        add( CIFF_SI_FNUMBER, "FNumber", META_USHORT );
        add( CIFF_SI_FOCUS_DISTANCE_LOWER, "SIFocusDistanceLower", META_USHORT );
        add( CIFF_SI_FOCUS_DISTANCE_UPPER, "SIFocusDistanceUpper", META_USHORT );
        add( CIFF_SI_ISO, "SIISO", META_USHORT );
        add( CIFF_SI_SEQUENCE_NUMBER, "SISequenceNumber", META_USHORT );
        add( CIFF_SI_SHUTTER_SPEED, "SIShutterSpeed", META_USHORT );
        add( CIFF_SI_WHITE_BALANCE, "SIWhiteBalance", META_USHORT );
        add( CIFF_SSI_SENSOR_BOTTOM_BORDER, "SSISensorBottomBorder", META_USHORT );
        add( CIFF_SSI_SENSOR_HEIGHT, "SSISensorHeight", META_USHORT );
        add( CIFF_SSI_SENSOR_LEFT_BORDER, "SSISensorLeftBorder", META_USHORT );
        add( CIFF_SSI_SENSOR_RIGHT_BORDER, "SSISensorRightBorder", META_USHORT );
        add( CIFF_SSI_SENSOR_TOP_BORDER, "SSISensorTopBorder", META_USHORT );
        add( CIFF_SSI_SENSOR_WIDTH, "SSISensorWidth", META_USHORT );
        add( CIFF_TARGET_DISTANCE_SETTING, "TargetDistanceSetting", META_ULONG );
        add( CIFF_TARGET_IMAGE_TYPE, "TargetImageType", META_USHORT );
        add( CIFF_THUMBNAIL_FILE_NAME, "ThumbnailFileName", META_STRING );
        add( CIFF_THUMBNAIL_IMAGE, "ThumbnailImage", META_UNDEFINED );
        add( CIFF_USER_COMMENT, "UserComment", META_STRING );
        add( CIFF_WHITE_BALANCE_TABLE, "WhiteBalanceTable", META_UNDEFINED );
    }
}
/* vim:set et sw=4 ts=4: */
