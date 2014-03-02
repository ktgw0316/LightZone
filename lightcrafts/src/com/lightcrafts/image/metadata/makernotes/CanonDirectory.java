/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.metadata.values.LongMetaValue;
import com.lightcrafts.image.metadata.values.ShortMetaValue;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.utils.TextUtil;

import static com.lightcrafts.image.metadata.EXIFConstants.*;
import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.ImageOrientation.*;
import static com.lightcrafts.image.metadata.makernotes.CanonTags.*;
import static com.lightcrafts.image.metadata.TIFFTags.*;
import static com.lightcrafts.image.types.CIFFConstants.*;

/**
 * A <code>CanonDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding Canon-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class CanonDirectory extends MakerNotesDirectory implements
    ApertureProvider, ArtistProvider, ColorTemperatureProvider,
    FocalLengthProvider, ISOProvider, LensProvider, OrientationProvider,
    PreviewImageProvider, ShutterSpeedProvider, ThumbnailImageProvider,
    WidthHeightProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public float getAperture() {
        final ImageMetaValue value = getValue( CANON_SI_FNUMBER );
        return  value != null ?
                (float)MetadataUtil.convertFStopFromAPEX(
                    value.getIntValue()
                ) : 0;
    }

    /**
     * {@inheritDoc}
     */
    public String getArtist() {
        final ImageMetaValue value = getValue( CANON_OWNER_NAME );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    public int getColorTemperature() {
        ImageMetaValue value = getValue( CANON_PI_COLOR_TEMPERATURE );
        if ( value == null )
            value = getValue( CANON_CI_D30_COLOR_TEMPERATURE );
        if ( value == null )
            value = getValue( CANON_COLOR_TEMPERATURE );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public float getFocalLength() {
        final ImageMetaValue value = getValue( CANON_FL_FOCAL_LENGTH );
        return value != null ? value.getFloatValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getImageHeight() {
        final ImageMetaValue value = getValue( CANON_PI_IMAGE_HEIGHT );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getImageWidth() {
        final ImageMetaValue value = getValue( CANON_PI_IMAGE_WIDTH );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getISO() {
        ImageMetaValue value = getValue( CANON_SI_ISO );
        boolean isAPEX = true;
        if ( value == null ) {
            //
            // CANON_CS_ISO can be "Auto" that is not a number,
            // therefore it shouldn't be a default choice. 
            //
            value = getValue( CANON_CS_ISO );
            isAPEX = false;
        }
        if ( value == null )
            return 0;
        int iso = value.getIntValue();
        if ( isAPEX )
            iso = MetadataUtil.convertISOFromAPEX( iso );
        return iso >= 1 && iso <= 10000 ? iso : 0;
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;Canon&quot;.
     */
    public String getName() {
        return "Canon";
    }

    /**
     * {@inheritDoc}
     */
    public String getLens() {
        final ImageMetaValue lensValue = getValue( CANON_CS_LENS_TYPE );
        final String label = hasTagValueLabelFor( lensValue );
        if ( label != null )
            return label;
        return makeLensLabelFrom(
            getValue( CANON_CS_SHORT_FOCAL_LENGTH ),
            getValue( CANON_CS_LONG_FOCAL_LENGTH ),
            getValue( CANON_CS_FOCAL_UNITS_PER_MM )
        );
    }

    /**
     * {@inheritDoc}
     */
    public ImageOrientation getOrientation() {
        final ImageMetaValue value = getValue( CANON_SI_AUTO_ROTATE );
        if ( value != null ) {
            switch ( value.getIntValue() ) {
                //
                // The CIFF constants are the same for the CanonDirectory.
                //
                case CIFF_AUTO_ROTATE_NONE:
                    return ORIENTATION_LANDSCAPE;
                case CIFF_AUTO_ROTATE_90CCW:
                    return ORIENTATION_90CCW;
                case CIFF_AUTO_ROTATE_180:
                    return ORIENTATION_180;
                case CIFF_AUTO_ROTATE_90CW:
                    return ORIENTATION_90CW;
            }
        }
        return ORIENTATION_UNKNOWN;
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException
    {
        // TODO: verify that this is actually an sRGB image, what about Adobe RGB shooting, etc.?
        return JPEGImageType.getImageFromBuffer(
            imageInfo.getByteBuffer(),
            getValue( CANON_PII_IMAGE_START ),
            1       // JPEG_MARKER_BYTE
                + 1 // JPEG_SOI_MARKER
                + 1 // JPEG_MARKER_BYTE
                + 1 // JPEG_APP1_MARKER
                + 2 // APP1 length
                + EXIF_HEADER_START_SIZE,
            getValue( CANON_PII_IMAGE_LENGTH ),
            maxWidth, maxHeight
        );
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor( TIFFDirectory.class );
        if ( dir == null ) {
            //
            // This should never be null, but just in case ...
            //
            return null;
        }
        return JPEGImageType.getImageFromBuffer(
            imageInfo.getByteBuffer(),
            dir.getValue( TIFF_JPEG_INTERCHANGE_FORMAT ), 0,
            dir.getValue( TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH ),
            0, 0
        );
    }

    /**
     * {@inheritDoc}
     */
    public float getShutterSpeed() {
        final ImageMetaValue value = getValue( CANON_SI_EXPOSURE_TIME );
        return value != null ? getShutterSpeedFrom( value ) : 0;
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
     * Puts a key/value pair into this directory.  For a Canon tag that has
     * subfields, expands the values into multiple key/value pairs.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The value to put.
     * @see #valueToString(ImageMetaValue)
     */
    public void putValue( Integer tagID, final ImageMetaValue value ) {
        switch ( tagID ) {
            case CANON_CAMERA_SETTINGS:
            case CANON_COLOR_INFO_D30:
            case CANON_FOCAL_LENGTH:
            case CANON_PROCESSING_INFO:
            case CANON_SENSOR_INFO:
            case CANON_SHOT_INFO:
                explodeSubfields( tagID, 1, value, false );
                return;
            case CANON_COLOR_DATA:
            case CANON_COLOR_INFO:
            case CANON_FILE_INFO:
            case CANON_PICTURE_INFO:
            case CANON_PREVIEW_IMAGE_INFO:
                explodeSubfields( tagID, 1, value, true );
                return;
            case CANON_CUSTOM_FUNCTIONS:
                value.setNonDisplayable();
                if ( value instanceof LongMetaValue ) {
                    final long[] longs = ((LongMetaValue)value).getLongValues();
                    final int cfTagIDStart = tagID << 8;
                    for ( int i = 1; i < longs.length; ++i ) {
                        final int n = (int)longs[i];
                        putValue(
                            cfTagIDStart | (n >>> 8),
                            new ShortMetaValue( (short)(n & 0xFF) )
                        );
                    }
                }
                return;
            case CANON_FLASH_INFO:
                // TODO: handle this case
                return;
        }
        super.putValue( tagID, value );
    }

    /**
     * {@inheritDoc}
     */
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case CANON_CS_LENS_TYPE: {
                final String label = hasTagValueLabelFor( CANON_CS_LENS_TYPE );
                return label != null ? label : "unknown"; // TODO: localize
            }
            case CANON_CS_LONG_FOCAL_LENGTH:
            case CANON_CS_SHORT_FOCAL_LENGTH:
            case CANON_FL_FOCAL_LENGTH:
            case CANON_LI_FOCAL_LENGTH:
            case CANON_LI_LONG_FOCAL_LENGTH:
            case CANON_LI_SHORT_FOCAL_LENGTH:
                return value.getStringValue() + "mm"; // TODO: localize "mm"
            case CANON_PI_DIGITAL_GAIN:
                return TextUtil.tenths( value.getIntValue() / 10F );
            case CANON_SI_EXPOSURE_TIME:
            case CANON_SI_TARGET_EXPOSURE_TIME: {
                final float shutterSpeed = getShutterSpeedFrom( value );
                return MetadataUtil.shutterSpeedString( shutterSpeed );
            }
            case CANON_SI_FNUMBER:
            case CANON_SI_TARGET_APERTURE: {
                final int apex = value.getIntValue();
                return TextUtil.tenths(
                    MetadataUtil.convertFStopFromAPEX( apex )
                );
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
     * By default, the priority for maker notes directories is higher than
     * {@link ImageMetadataDirectory#getProviderPriorityFor(Class)} because
     * they have more detailed metadata about a given image.
     * <p>
     * However, an exception is made for {@link ShutterSpeedProvider} for Canon
     * because it yields weird values.
     *
     * @param provider The provider interface to get the priority for.
     * @return Returns said priority.
     */
    protected int getProviderPriorityFor(
        Class<? extends ImageMetadataProvider> provider )
    {
        return provider == ShutterSpeedProvider.class ? 0 :
            super.getProviderPriorityFor( provider );
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
        return CanonTags.class;
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
     * Gets the shutter speed from the given {@link ImageMetaValue}.
     *
     * @param value The {@link ImageMetaValue} to get the shutter speed from.
     * @return Returns the shutter speed.
     */
    private float getShutterSpeedFrom( ImageMetaValue value ) {
        final int apex = value.getIntValue();
        double speed = Math.exp(
            - MetadataUtil.convertAPEXToEV( apex ) * MetadataUtil.LN_2
        );
        final String model = getOwningMetadata().getCameraMake( true );
        if ( model != null ) {
            if ( model.contains( "20D"      ) ||
                 model.contains( "350D"     ) ||
                 model.contains( "REBEL XT" ) )
                speed *= 1000.0 / 32.0;
        }
        return (float)speed;
    }

    /**
     * This is where the actual labels for the tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.makernotes.CanonTags"
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
        add( CANON_CAMERA_SETTINGS, "CameraSettings", META_UNDEFINED );
        add( CANON_CD_VERSION, "CDVersion", META_SSHORT );
        add( CANON_CF_AF_ASSIST_LIGHT, "CFAssistLight", META_SSHORT );
        add( CANON_CF_AUTO_EXPOSURE_BRACKETING, "CFAutoExposureBracketing", META_SSHORT );
        add( CANON_CF_AUTO_EXPOSURE_LOCK_BUTTONS, "CFAutoExposureLockButtons", META_SSHORT );
        add( CANON_CF_FILL_FLASH_AUTO_REDUCTION, "CFFillFlashAutoReduction", META_SSHORT );
        add( CANON_CF_LENS_AUTO_FOCUS_STOP_BUTTON, "CFLensAutoFocusStopButton", META_SSHORT );
        add( CANON_CF_LONG_EXPOSURE_NOISE_REDUCTION, "CFLongExposureNoiseReduction", META_SSHORT );
        add( CANON_CF_MENU_BUTTON_RETURN_POSITION, "CFMenuButtonReturnPosition", META_SSHORT );
        add( CANON_CF_MIRROR_LOCKUP, "CFMirrorLockup", META_SSHORT );
        add( CANON_CF_SENSOR_CLEANING, "CFSensorCleaning", META_SSHORT );
        add( CANON_CF_SET_BUTTON_FUNCTION, "CFSetButtonFunction", META_SSHORT );
        add( CANON_CF_SHUTTER_CURTAIN_SYNC, "CFShutterCurtainSync", META_SSHORT );
        add( CANON_CF_SHUTTER_SPEED_IN_AV_MODE, "CFShutterSpeedInAvMode", META_SSHORT );
        add( CANON_CF_TV_AV_EXPOSURE_LEVEL, "CFTVAVExposureLevel", META_SSHORT );
        add( CANON_CI_COLOR_HUE, "CIColorHue", META_SSHORT );
        add( CANON_CI_COLOR_SPACE, "CIColorSpace", META_SSHORT );
        add( CANON_CI_D30_COLOR_MATRIX, "CID30ColorMatrix", META_SSHORT );
        add( CANON_CI_D30_COLOR_TEMPERATURE, "CID30ColorTemperature", META_USHORT );
        add( CANON_COLOR_INFO, "ColorInfo", META_UNDEFINED );
        add( CANON_COLOR_INFO_D30, "ColorInfoD30", META_UNDEFINED );
        add( CANON_COLOR_SPACE, "ColorSpace", META_USHORT );
        add( CANON_COLOR_TEMPERATURE, "ColorTemperature", META_USHORT );
        add( CANON_CS_AF_POINT_SELECTED, "CSAFPointSelected", META_SSHORT );
        add( CANON_CS_CONTINUOUS_DRIVE_MODE, "CSContinuousDriveMode", META_SSHORT );
        add( CANON_CS_CONTRAST, "CSContrast", META_SSHORT );
        add( CANON_CS_DIGITAL_ZOOM, "CSDigitalZoom", META_SSHORT );
        add( CANON_CS_EASY_SHOOTING_MODE, "CSEasyShootingMode", META_SSHORT );
        add( CANON_CS_EXPOSURE_MODE, "CSExposureMode", META_SSHORT );
        add( CANON_CS_FLASH_ACTIVITY, "CSFlashActivity", META_SSHORT );
        add( CANON_CS_FLASH_DETAILS, "CSFlashDetails", META_SSHORT );
        add( CANON_CS_FLASH_MODE, "CSFlashMode", META_SSHORT );
        add( CANON_CS_FOCAL_UNITS_PER_MM, "CSFocalUnitsPerMM", META_SSHORT );
        add( CANON_CS_FOCUS_MODE, "CSFocusMode", META_SSHORT );
        add( CANON_CS_FOCUS_MODE_G1, "CSFocusModeG1", META_SSHORT );
        add( CANON_CS_FOCUS_TYPE, "CSFocusType", META_SSHORT );
        add( CANON_CS_IMAGE_SIZE, "CSImageSize", META_SSHORT );
        add( CANON_CS_ISO, "CSISO", META_SSHORT );
        add( CANON_CS_LENS_TYPE, "CSLensType", META_SSHORT );
        add( CANON_CS_LONG_FOCAL_LENGTH, "CSLongFocalLength", META_SSHORT );
        add( CANON_CS_MACRO_MODE, "CSMacroMode", META_SSHORT );
        add( CANON_CS_METERING_MODE, "CSMeteringMode", META_SSHORT );
        add( CANON_CS_QUALITY, "CSQuality", META_SSHORT );
        add( CANON_CS_SATURATION, "CSSaturation", META_SSHORT );
        add( CANON_CS_SELF_TIMER_DELAY, "CSSelfTimerDelay", META_SSHORT );
        add( CANON_CS_SHARPNESS, "CSSharpness", META_SSHORT );
        add( CANON_CS_SHORT_FOCAL_LENGTH, "CSShortFocalLength", META_SSHORT );
        add( CANON_CS_ZOOMED_RESOLUTION, "CSZoomedResolution", META_SSHORT );
        add( CANON_CS_ZOOMED_RESOLUTION_BASE, "CSZoomedResolutionBase", META_SSHORT );
        add( CANON_CUSTOM_FUNCTIONS, "CustomFunctions", META_SSHORT );
        add( CANON_FILE_INFO, "FileInfo", META_UNDEFINED );
        add( CANON_FILE_LENGTH, "FileLength", META_ULONG );
        add( CANON_FIRMWARE_VERSION, "FirmwareVersion", META_STRING );
        add( CANON_FI_FILE_NUMBER, "FIFileNumber", META_ULONG );
        add( CANON_FI_SHUTTER_COUNT, "FIShutterCount", META_ULONG );
        add( CANON_FLASH_INFO, "FlashInfo", META_UNDEFINED );
        add( CANON_FL_FOCAL_LENGTH, "FLFocalLength", META_USHORT );
        add( CANON_FL_FOCAL_PLANE_X_SIZE, "FLFocalPlaneXSize", META_USHORT );
        add( CANON_FL_FOCAL_PLANE_Y_SIZE, "FLFocalPlaneYSize", META_USHORT );
        add( CANON_FOCAL_LENGTH, "FocalLength", META_UNDEFINED );
        add( CANON_IMAGE_NUMBER, "ImageNumber", META_ULONG );
        add( CANON_IMAGE_TYPE, "ImageType", META_STRING );
        add( CANON_LENS_INFO_1D, "LensInfo1D", META_UNDEFINED );
        add( CANON_LI_FOCAL_LENGTH, "LIFocalLength", META_USHORT );
        add( CANON_LI_LENS_TYPE, "LILensType", META_USHORT );
        add( CANON_LI_LONG_FOCAL_LENGTH, "LILongFocalLength", META_USHORT );
        add( CANON_LI_SHORT_FOCAL_LENGTH, "LIShortFocalLength", META_USHORT );
        add( CANON_MODEL_ID, "ModelID", META_ULONG );
        add( CANON_OWNER_NAME, "OwnerName", META_STRING );
        add( CANON_PI_AF_POINTS_USED, "PIAFPointsUsed", META_USHORT );
        add( CANON_PI_COLOR_TEMPERATURE, "PIColorTemperature", META_SSHORT );
        add( CANON_PI_DIGITAL_GAIN, "PIDigitalGain", META_SSHORT );
        add( CANON_PI_IMAGE_HEIGHT, "PIImageHeight", META_USHORT );
        add( CANON_PI_IMAGE_HEIGHT_AS_SHOT, "PIImageHeightAsShot", META_USHORT );
        add( CANON_PI_IMAGE_WIDTH, "PIImageWidth", META_USHORT );
        add( CANON_PI_IMAGE_WIDTH_AS_SHOT, "PIImageWidthAsShot", META_USHORT );
        add( CANON_PI_PICTURE_STYLE, "PIPictureStyle", META_SSHORT );
        add( CANON_PI_SENSOR_BLUE_LEVEL, "PISensorBlueLevel", META_SSHORT );
        add( CANON_PI_SENSOR_RED_LEVEL, "PISensorRedLevel", META_SSHORT );
        add( CANON_PI_TONE_CURVE, "PIToneCurve", META_SSHORT );
        add( CANON_PI_WB_SHIFT_AB, "PIWBShiftAB", META_SSHORT );
        add( CANON_PI_WB_SHIFT_GM, "PIWBShiftGM", META_SSHORT );
        add( CANON_PI_WHITE_BALANCE_BLUE, "PIWhiteBalanceBlue", META_SSHORT );
        add( CANON_PI_WHITE_BALANCE_RED, "PIWhiteBalanceRed", META_SSHORT );
        add( CANON_PI2_AF_AREA_MODE, "PI2AfAreaMode", META_USHORT );
        add( CANON_PICTURE_INFO, "PictureInfo", META_UNDEFINED );
        add( CANON_PICTURE_INFO2, "PictureInfo2", META_UNDEFINED );
        add( CANON_PII_FOCAL_PLANE_X_RESOLUTION, "PIIFocalPlaneXResolution", META_SRATIONAL );
        add( CANON_PII_FOCAL_PLANE_Y_RESOLUTION, "PIIFocalPlaneYResolution", META_SRATIONAL );
        add( CANON_PII_IMAGE_HEIGHT, "PIIImageHeight", META_ULONG );
        add( CANON_PII_IMAGE_LENGTH, "PIIImageLength", META_ULONG );
        add( CANON_PII_IMAGE_START, "PIIImageStart", META_ULONG );
        add( CANON_PII_IMAGE_WIDTH, "PIIImageWidth", META_ULONG );
        add( CANON_PREVIEW_IMAGE_INFO, "PreviewImageInfo", META_UNDEFINED );
        add( CANON_PROCESSING_INFO, "ColorProcessingInfo", META_UNDEFINED );
        add( CANON_SENSOR_INFO, "SensorInfo", META_UNDEFINED );
        add( CANON_SERIAL_NUMBER, "SerialNumber", META_ULONG );
        add( CANON_SHOT_INFO, "ShotInfo", META_UNDEFINED );
        add( CANON_SI_AEB_BRACKET_VALUE, "AEBBracketValue", META_SSHORT );
        add( CANON_SI_AF_POINT_USED, "SIAFPointUsed", META_SSHORT );
        add( CANON_SI_AUTO_EXPOSURE_BRACKETING, "SIAutoExposureBracketing", META_SSHORT );
        add( CANON_SI_AUTO_ROTATE, "SIAutoRotate", META_SSHORT );
        add( CANON_SI_BULB_DURATION, "SIBulbDuration", META_SSHORT );
        add( CANON_SI_EXPOSURE_COMPENSATION, "SIExposureCompensation", META_SSHORT );
        add( CANON_SI_EXPOSURE_TIME, "ExposureTime", META_SSHORT );
        add( CANON_SI_FLASH_BIAS, "SIFlashBias", META_SSHORT );
        add( CANON_SI_FNUMBER, "FNumber", META_SSHORT );
        add( CANON_SI_FOCUS_DISTANCE_LOWER, "FocusDistanceLower", META_SSHORT );
        add( CANON_SI_FOCUS_DISTANCE_LOWER, "SIFocusDistanceLower", META_SSHORT );
        add( CANON_SI_FOCUS_DISTANCE_UPPER, "SIFocusDistanceUpper", META_SSHORT );
        add( CANON_SI_ISO, "SIISO", META_SSHORT );
        add( CANON_SI_SENSOR_WIDTH, "SISensorWidth", META_SSHORT );
        add( CANON_SI_SENSOR_HEIGHT, "SISensorHeight", META_SSHORT );
        add( CANON_SI_SENSOR_LEFT_BORDER, "SISensorLeftBorder", META_SSHORT );
        add( CANON_SI_SENSOR_TOP_BORDER, "SISensorTopBorder", META_SSHORT );
        add( CANON_SI_SENSOR_RIGHT_BORDER, "SISensorRightBorder", META_SSHORT );
        add( CANON_SI_SENSOR_BOTTOM_BORDER, "SISensorBottomBorder", META_SSHORT );
        add( CANON_SI_SEQUENCE_NUMBER, "SISequenceNumber", META_SSHORT );
        add( CANON_SI_TARGET_APERTURE, "TargetAperture", META_SSHORT );
        add( CANON_SI_TARGET_EXPOSURE_TIME, "TargetExposure", META_SSHORT );
        add( CANON_SI_WHITE_BALANCE, "SIWhiteBalance", META_SSHORT );
        add( CANON_WHITE_BALANCE_TABLE, "WhiteBalanceTable", META_SSHORT );
    }
}
/* vim:set et sw=4 ts=4: */
