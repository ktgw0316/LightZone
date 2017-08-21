/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import java.io.IOException;
import java.util.*;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import static com.lightcrafts.image.metadata.ImageOrientation.*;
import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.makernotes.SonyTags.*;

/**
 * A {@code SonyDirectory} is-an {@link ImageMetadataDirectory} for holding
 * Sony-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class SonyDirectory extends MakerNotesDirectory implements
    ISOProvider, LensProvider, OrientationProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public int getISO() {
        final ImageMetaValue value = getValue( SONY_CS_ISO_SETTING );
        if ( value != null ) {
            final int n = value.getIntValue();
            if ( n > 0 )
                return (int)(Math.exp( (n / 8.0 - 6) * MetadataUtil.LN_2 ) * 100);
        }
        return 0;
    }

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
     * {@inheritDoc}
     */
    @Override
    public String getLens() {
        return hasTagValueLabelFor( getValue( SONY_LENS_TYPE ) );
    }

    /**
     * Gets the maker-notes adjustments for Sony.
     *
     * @param buf The {@link LCByteBuffer} the metadata is in.
     * @param offset The offset to the start of the maker-notes.
     * @param imageInfo The image.
     * @return Returns said adjustments.
     */
    public int[] getMakerNotesAdjustments( LCByteBuffer buf, int offset,
                                           ImageInfo imageInfo )
        throws IOException
    {
        if ( buf.getEquals( offset, "SONY", "ASCII" ) ) {
            //
            // These are the maker notes from a JPEG file.  The 10 bytes are:
            //
            //      0-4: "SONY "
            //      5-8: "DSC " or "CAM "
            //      9  : 0
            //
            return new int[]{ 10, offset };
        }
        //
        // The maker notes from a raw file don't have a header.
        //
        return new int[]{ 0, offset };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageOrientation getOrientation() {
        final ImageMetaValue value = getValue( SONY_CS_ROTATION );
        if ( value != null ) {
            switch ( value.getIntValue() ) {
                case 0:
                    return ORIENTATION_LANDSCAPE;
                case 1:
                    return ORIENTATION_90CW;
                case 2:
                    return ORIENTATION_90CCW;
            }
        }
        return ORIENTATION_UNKNOWN;
    }

    @Override
    public String getName() {
        return null;
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
     * Puts a key/value pair into this directory.  For a Sony tag that has
     * subfields, expands the values into multiple key/value pairs.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The value to put.
     * @see #valueToString(ImageMetaValue)
     */
    @Override
    public void putValue( Integer tagID, ImageMetaValue value ) {
        switch ( tagID ) {
            case SONY_CAMERA_SETTINGS:
                // TODO
                break;
        }
        super.putValue( tagID, value );
    }

    /**
     * {@inheritDoc}
     */
    public void setOrientation( ImageOrientation orientation ) {
        int value;
        switch ( orientation ) {
            case ORIENTATION_LANDSCAPE:
                value = 0;
                break;
            case ORIENTATION_90CW:
                value = 1;
                break;
            case ORIENTATION_90CCW:
                value = 2;
                break;
            default:
                removeValue( SONY_CS_ROTATION );
                return;
        }
        setValue( SONY_CS_ROTATION, value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case SONY_CS_BRIGHTNESS:
            case SONY_CS_CONTRAST:
            case SONY_CS_SATURATION:
            case SONY_CS_SHARPNESS:
            case SONY_CS_ZONE_MATCHING_VALUE:
                final int n = value.getIntValue() - 10;
                final String s = Integer.toString( n );
                return n > 0 ? '+' + s : s;
            case SONY_CS_ISO_SETTING:
                if ( value.getIntValue() == 0 )
                    return "auto";  // TODO: localize
                return Integer.toString( getISO() );
            case SONY_FULL_IMAGE_SIZE:
            case SONY_PREVIEW_IMAGE_SIZE:
                if ( value.getValueCount() == 2 ) {
                    // The height value is first so swap them.
                    return value.getIntValueAt( 1 ) + " x " + value.getIntValue();
                }
                break;
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
        return SonyTags.class;
    }

    /**
     * Gets the priority of this directory for providing the metadata supplied
     * by implementing the given provider interface.
     * <p>
     * The priority for {@link LensProvider} for Sony is higher than
     * the priority from {@link CoreDirectory}, but is lower than EXIF/TIFF
     * metadata,
     *
     * @param provider The provider interface to get the priority for.
     * @return Returns said priority.
     */
    @Override
    protected int getProviderPriorityFor(
            Class<? extends ImageMetadataProvider> provider )
    {
        return (provider == LensProvider.class)
                ? PROVIDER_PRIORITY_DEFAULT + 1
                : super.getProviderPriorityFor( provider );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Add the tag mappings.
     *
     * @param id The tag's ID.
     * @param name The tag's name.
     * @param type The tag's {@link ImageMetaType}.
     * @param isChangeable Whether the tag is user-changeable.
     */
    private static void add( int id, String name, ImageMetaType type,
                             boolean isChangeable ) {
        final ImageMetaTagInfo tagInfo =
            new ImageMetaTagInfo( id, name, type, isChangeable );
        m_tagsByID.put( id, tagInfo );
        m_tagsByName.put( name, tagInfo );
    }

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

    /**
     * This is where the actual labels for the Sony tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.makernotes.SonyTags"
    );

    static {
        add( SONY_ANTI_BLUR, "AntiBlur", META_USHORT, false );
        add( SONY_AUTO_HDR, "AutoHDR", META_ULONG, false );
        add( SONY_CAMERA_SETTINGS, "CameraSettings", META_USHORT, false );
        add( SONY_COLOR_COMPENSATION_FILTER, "ColorCompensationFilter", META_SLONG, false );
        add( SONY_COLOR_MODE, "ColorMode", META_ULONG, false );
        add( SONY_COLOR_REPRODUCTION, "ColorReproduction", META_STRING, false );
        add( SONY_COLOR_TEMPERATURE, "ColorTemperature", META_ULONG, false );
        add( SONY_CS_AF_AREA_MODE, "CSAFAreaMode", META_USHORT, false );
        add( SONY_CS_AF_ILLUMINATOR, "CSAFIlluminator", META_USHORT, false );
        add( SONY_CS_AF_WITH_SHUTTER, "CSAFWithShutter", META_USHORT, false );
        add( SONY_CS_ASPECT_RATIO, "CSAspectRatio", META_USHORT, false );
        add( SONY_CS_BRIGHTNESS, "CSBrightness", META_USHORT, false );
        add( SONY_CS_CONTRAST, "CSContrast", META_USHORT, false );
        add( SONY_CS_CREATIVE_STYLE, "CSCreativeStyle", META_USHORT, false );
        add( SONY_CS_DRIVE_MODE, "CSDriveMode", META_USHORT, false );
        add( SONY_CS_DYNAMIC_RANGE_OPTIMIZER_LEVEL, "CSDynamicRangeOptimizerLevel", META_USHORT, false );
        add( SONY_CS_EXPOSURE_LEVEL_INCREMENTS, "CSExposureLevelIncrements", META_USHORT, false );
        add( SONY_CS_EXPOSURE_PROGRAM, "CSExposureProgram", META_USHORT, false );
        add( SONY_CS_FLASH_MODE, "CSFlashMode", META_USHORT, false );
        add( SONY_CS_FOCUS_MODE, "CSFocusMode", META_USHORT, false );
        add( SONY_CS_HIGH_ISO_NOISE_REDUCTION, "CSHighISONoiseReduction", META_USHORT, false );
        add( SONY_CS_IMAGE_SIZE, "CSImageSize", META_USHORT, false );
        add( SONY_CS_IMAGE_STABILIZATION, "CSImageStabilization", META_USHORT, false );
        add( SONY_CS_IMAGE_STYLE, "CSImageStyle", META_USHORT, false );
        add( SONY_CS_ISO_SETTING, "CSISOSetting", META_USHORT, false );
        add( SONY_CS_LOCAL_AF_AREA_POINT, "CSLocalAFAreaPoint", META_USHORT, false );
        add( SONY_CS_LONG_EXPOSURE_NOISE_REDUCTION, "CSLongExposureNoiseReduction", META_USHORT, false );
        add( SONY_CS_METERING_MODE, "CSMeteringMode", META_USHORT, false );
        add( SONY_CS_PRIORITY_SETUP_SHUTTER_RELEASE, "CSPrioritySetupShutterRelease", META_USHORT, false );
        add( SONY_CS_QUALITY, "CSQuality", META_USHORT, false );
        add( SONY_CS_ROTATION, "CSRotation", META_USHORT, true );
        add( SONY_CS_SATURATION, "CSSaturation", META_USHORT, false );
        add( SONY_CS_SHARPNESS, "CSSharpness", META_USHORT, false );
        add( SONY_CS_WHITE_BALANCE_FINE_TUNE, "CSWhiteBalanceFineTune", META_SSHORT, false );
        add( SONY_CS_ZONE_MATCHING_VALUE, "CSZoneMatchingValue", META_USHORT, false );
        add( SONY_DYNAMIC_OPTIMIZER, "DynamicOptimizer", META_ULONG, false );
        add( SONY_DYNAMIC_RANGE_OPTIMIZER, "DynamicRangeOptimizer", META_USHORT, false );
        add( SONY_EXPOSURE_MODE, "ExposureMode", META_USHORT, false );
        add( SONY_FLASH_EXPOSURE_COMPENSATION, "FlashExposureCompensation", META_SRATIONAL, false );
        add( SONY_FULL_IMAGE_SIZE, "FullImageSize", META_ULONG, false );
        add( SONY_IMAGE_STABILIZATION, "ImageStabilization", META_ULONG, false );
        add( SONY_INTELLIGENT_AUTO, "IntelligentAuto", META_USHORT, false );
        add( SONY_LENS_TYPE, "LensType", META_ULONG, false );
        add( SONY_LONG_EXPOSURE_NOISE_REDUCTION, "LongExposureNoiseReduction", META_USHORT, false );
        add( SONY_MACRO, "Macro", META_USHORT, false );
        add( SONY_MINOLTA_MAKER_NOTE, "MinoltaMakerNote", META_ULONG, false );
        add( SONY_PREVIEW_IMAGE, "PreviewImage", META_UNDEFINED, false );
        add( SONY_PREVIEW_IMAGE_SIZE, "PreviewImageSize", META_USHORT, false );
        add( SONY_QUALITY, "Quality", META_ULONG, false );
        add( SONY_QUALITY_2, "Quality2", META_USHORT, false );
        add( SONY_SCENE_MODE, "SceneMode", META_USHORT, false );
        add( SONY_SHOT_INFO, "ShotInfo", META_USHORT, false );
        add( SONY_TELECONVERTER, "Teleconverter", META_USHORT, false );
        add( SONY_WHITE_BALANCE, "WhiteBalance", META_USHORT, false );
        add( SONY_WHITE_BALANCE_2, "WhiteBalance2", META_USHORT, false );
        add( SONY_ZONE_MATCHING, "ZoneMatching", META_USHORT, false );
    }
}
/* vim:set et sw=4 ts=4: */
