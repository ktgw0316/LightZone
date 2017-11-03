/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.utils.TextUtil;

import static com.lightcrafts.image.metadata.EXIFTags.*;
import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.ImageOrientation.ORIENTATION_UNKNOWN;
import static com.lightcrafts.image.metadata.XMPConstants.*;
import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * An <code>EXIFDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding EXIF metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class EXIFDirectory extends ImageMetadataDirectory implements
    ApertureProvider, ArtistProvider, BitsPerChannelProvider,
    CaptionProvider, CaptureDateTimeProvider,
    CopyrightProvider, FlashProvider, FocalLengthProvider,
    ISOProvider, LensProvider, MakeModelProvider, OrientationProvider,
    RatingProvider, ResolutionProvider, ShutterSpeedProvider,
    WidthHeightProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * The encoding of the EXIF version number added to the any generated EXIF
     * directory.
     */
    public static final byte[] EXIF_VERSION = new byte[]{ '0','2','2','0' };

    /**
     * Calculates the shutter speed value from the APEX value of the
     * {@link EXIFTags#EXIF_SHUTTER_SPEED_VALUE} metadata value.
     *
     * @param value The {@link RationalMetaValue} that contains the APEX value
     * of the {@link EXIFTags#EXIF_SHUTTER_SPEED_VALUE} metadata tag.
     * @return Returns the shutter speed in seconds.
     */
    public static float calcShutterSpeedFromAPEX( ImageMetaValue value ) {
        final double apex = value.getDoubleValue();
        return (float)(1 / Math.exp( apex * Math.log(2) ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getAperture() {
        ImageMetaValue value = getValue( EXIF_FNUMBER );
        if ( value == null )
            value = getValue( EXIF_APERTURE_VALUE );
        if ( !(value instanceof RationalMetaValue) )
              return 0;
        return MetadataUtil.fixFStop( value.getFloatValue() );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getArtist() {
        final ImageMetaValue value = getValue( EXIF_ARTIST );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBitsPerChannel() {
        final ImageMetaValue value = getValue( EXIF_BITS_PER_SAMPLE );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCameraMake( boolean includeModel ) {
        return getCameraMake( EXIF_MAKE, EXIF_MODEL, includeModel );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCaption() {
        final ImageMetaValue value = getValue( EXIF_IMAGE_DESCRIPTION );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getCaptureDateTime() {
        ImageMetaValue value = getValue( EXIF_DATE_TIME_ORIGINAL );
        if ( value == null )
            value = getValue( EXIF_DATE_TIME_DIGITIZED );
        return  value instanceof DateMetaValue ?
                ((DateMetaValue)value).getDateValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCopyright() {
        final ImageMetaValue value = getValue( EXIF_COPYRIGHT );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFlash() {
        final ImageMetaValue flashValue = getValue( EXIF_FLASH );
        if ( flashValue != null ) {
            final int flashBits = flashValue.getIntValue();
            if ( (flashBits & TIFF_FLASH_NOT_PRESENT_BIT) == 0 )
                return flashBits;
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFocalLength() {
        final ImageMetaValue value = getValue( EXIF_FOCAL_LENGTH );
        return value != null ? value.getFloatValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageHeight() {
        return MetadataUtil.maxTagValue(
            this, EXIF_IMAGE_HEIGHT, EXIF_PIXEL_Y_DIMENSION
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageWidth() {
        return MetadataUtil.maxTagValue(
            this, EXIF_IMAGE_WIDTH, EXIF_PIXEL_X_DIMENSION
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getISO() {
        final ImageMetaValue value = getValue( EXIF_ISO_SPEED_RATINGS );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLens() {
        final ImageMetaValue model = getValue(EXIF_LENS_MODEL);
        if (model != null) {
            return model.getStringValue();
        }
        final ImageMetaValue info = getValue(EXIF_LENS_INFO);
        if (info != null) {
            return makeLensLabelFrom(info);
        }
        return null;
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;EXIF&quot;.
     */
    @Override
    public String getName() {
        return "EXIF";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageOrientation getOrientation() {
        final ImageMetaValue value = getValue( EXIF_ORIENTATION );
        if ( value != null )
            try {
                return ImageOrientation.getOrientationFor( value.getIntValue() );
            }
            catch ( IllegalArgumentException e ) {
                // ignore
            }
        return ORIENTATION_UNKNOWN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRating() {
        final ImageMetaValue rating = getValue( EXIF_MS_RATING );
        return rating != null ? rating.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getResolution() {
        final ImageMetaValue res = getValue( EXIF_X_RESOLUTION );
        return res != null ? res.getDoubleValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getResolutionUnit() {
        final ImageMetaValue unit = getValue( EXIF_RESOLUTION_UNIT );
        if ( unit != null ) {
            switch ( unit.getIntValue() ) {
                case TIFF_RESOLUTION_UNIT_CM:
                    return RESOLUTION_UNIT_CM;
                case TIFF_RESOLUTION_UNIT_INCH:
                    return RESOLUTION_UNIT_INCH;
            }
        }
        return RESOLUTION_UNIT_NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getShutterSpeed() {
        boolean isAPEX = false;
        ImageMetaValue value = getValue( EXIF_EXPOSURE_TIME );
        if ( value == null ) {
            value = getValue( EXIF_SHUTTER_SPEED_VALUE );
            isAPEX = true;
        }
        if ( !(value instanceof RationalMetaValue) )
            return 0;
        if ( isAPEX )
            return calcShutterSpeedFromAPEX( value );
        return value.getFloatValue();
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
     * {@inheritDoc}
     */
    public void setArtist( String artist ) {
        setValue( EXIF_ARTIST, artist );
    }

    /**
     * {@inheritDoc}
     */
    public void setCaption( String caption ) {
        setValue( EXIF_IMAGE_DESCRIPTION, caption );
    }

    /**
     * {@inheritDoc}
     */
    public void setCopyright( String copyright ) {
        setValue( EXIF_COPYRIGHT, copyright );
    }

    /**
     * {@inheritDoc}
     */
    public void setOrientation( ImageOrientation orientation ) {
        setValue( EXIF_ORIENTATION, orientation.getTIFFConstant() );
    }

    /**
     * {@inheritDoc}
     */
    public void setRating( int rating ) {
        if ( rating < 0 || rating > 5 )
            throw new IllegalArgumentException( "rating must be between 0-5" );
        setValue( EXIF_MS_RATING, rating );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Element> toXMP( Document xmpDoc  ) {
        return toXMP( xmpDoc, XMP_EXIF_NS, XMP_EXIF_PREFIX );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case EXIF_APERTURE_VALUE:
            case EXIF_FNUMBER: {
                if ( !(value instanceof RationalMetaValue) )
                      break;
                final double fStop = value.getDoubleValue();
                return Float.toString( MetadataUtil.fixFStop( fStop ) );
                //return TextUtil.tenths( fStop );
            }
            case EXIF_EXPOSURE_BIAS_VALUE:
                if ( !value.isNumeric() )
                    break;
                return MetadataUtil.convertBiasFromAPEX( value.getFloatValue() );
            case EXIF_EXPOSURE_TIME:
                if ( !value.isNumeric() )
                    break;
                return MetadataUtil.shutterSpeedString( value.getFloatValue() );
            case EXIF_FOCAL_LENGTH: {
                if ( !value.isNumeric() )
                    break;
                final double n = value.getDoubleValue();
                return TextUtil.tenths( n ) + "mm";     // TODO: localize "mm"
            }
            case EXIF_FOCAL_LENGTH_IN_35MM_FILM: {
                if ( !value.isNumeric() )
                    break;
                final int n = value.getIntValue();
                switch ( n ) {
                    case 0:
                        return "unknown";
                    default:
                        return n + "mm";                // TODO: localize "mm"
                }
            }
            case EXIF_SHUTTER_SPEED_VALUE: {
                if ( !(value instanceof RationalMetaValue) )
                    break;
                final double speed = calcShutterSpeedFromAPEX( value );
                return MetadataUtil.shutterSpeedString( speed );
            }
        }
        return super.valueToString( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Gets the priority of this directory for providing the metadata supplied
     * by implementing the given provider interface.
     * <p>
     * The priority is guaranteed to be higher than the default.
     *
     * @param p The provider interface to get the priority for.
     * @return Returns said priority.
     */
    @Override
    protected int getProviderPriorityFor( Class<? extends ImageMetadataProvider> p ) {
        return PROVIDER_PRIORITY_DEFAULT + 10;
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
        return EXIFTags.class;
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
     * This is where the actual labels for the tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.EXIFTags"
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
        add( EXIF_APERTURE_VALUE, "ApertureValue", META_URATIONAL, false );
        add( EXIF_ARTIST, "Artist", META_STRING, true );
        add( EXIF_BITS_PER_SAMPLE, "BitsPerSample", META_USHORT, false );
        add( EXIF_BRIGHTNESS_VALUE, "BrightnessValue", META_SRATIONAL, false );
        add( EXIF_CFA_PATTERN, "CFAPattern", META_UNDEFINED, false );
        add( EXIF_CFA_PATTERN_2, "CFAPattern2", META_UNDEFINED, false );
        add( EXIF_COLOR_SPACE, "ColorSpace", META_SSHORT, false );
        add( EXIF_COMPONENTS_CONFIGURATION, "ComponentsConfiguration", META_UNDEFINED, false );
        add( EXIF_COMPRESSED_BITS_PER_PIXEL, "CompressedBitsPerPixel", META_URATIONAL, false );
        add( EXIF_COMPRESSION, "Compression", META_USHORT, false );
        add( EXIF_CONTRAST, "Contrast", META_USHORT, false );
        add( EXIF_COPYRIGHT, "Copyright", META_STRING, true );
        add( EXIF_CUSTOM_RENDERED, "CustomRendered", META_USHORT, false );
        add( EXIF_DATE_TIME, "DateTime", META_DATE, false );
        add( EXIF_DATE_TIME_DIGITIZED, "DateTimeDigitized", META_DATE, false );
        add( EXIF_DATE_TIME_ORIGINAL, "DateTimeOriginal", META_DATE, false );
        add( EXIF_DEVICE_SETTING_DESCRIPTION, "DeviceSettingDescription", META_UNDEFINED, false );
        add( EXIF_DIGITAL_ZOOM_RATIO, "DigitalZoomRatio", META_URATIONAL, false );
        add( EXIF_DOCUMENT_NAME, "DocumentName", META_STRING, true );
        add( EXIF_EXIF_VERSION, "ExifVersion", META_UNDEFINED, false );
        add( EXIF_EXPOSURE_BIAS_VALUE, "ExposureBiasValue", META_SRATIONAL, false );
        add( EXIF_EXPOSURE_INDEX, "ExposureIndex", META_URATIONAL, false );
        add( EXIF_EXPOSURE_INDEX_2, "ExposureIndex2", META_URATIONAL, false );
        add( EXIF_EXPOSURE_MODE, "ExposureMode", META_SSHORT, false );
        add( EXIF_EXPOSURE_PROGRAM, "ExposureProgram", META_USHORT, false );
        add( EXIF_EXPOSURE_TIME, "ExposureTime", META_URATIONAL, false );
        add( EXIF_FILE_SOURCE, "FileSource", META_UNDEFINED, false );
        add( EXIF_FLASH, "Flash", META_USHORT, false );
        add( EXIF_FLASH_ENERGY, "FlashEnergy", META_URATIONAL, false );
        add( EXIF_FLASHPIX_VERSION, "FlashpixVersion", META_UNDEFINED, false );
        add( EXIF_FNUMBER, "FNumber", META_URATIONAL, false );
        add( EXIF_FOCAL_LENGTH, "FocalLength", META_URATIONAL, false );
        add( EXIF_FOCAL_LENGTH_IN_35MM_FILM, "FocalLengthIn_35mmFilm", META_USHORT, false );
        add( EXIF_FOCAL_PLANE_RESOLUTION_UNIT, "FocalPlaneResolutionUnit", META_USHORT, false );
        add( EXIF_FOCAL_PLANE_X_RESOLUTION, "FocalPlaneXResolution", META_URATIONAL, false );
        add( EXIF_FOCAL_PLANE_Y_RESOLUTION, "FocalPlaneYResolution", META_URATIONAL, false );
        add( EXIF_GAIN_CONTROL, "GainControl", META_USHORT, false );
        add( EXIF_GPS_IFD_POINTER, "GPSInfoIfdPointer", META_ULONG, false );
        add( EXIF_HOST_COMPUTER, "HostComputer", META_STRING, false );
        add( EXIF_ICC_PROFILE, "ICCProfile", META_UNDEFINED, false );
        add( EXIF_IFD_POINTER, "ExifIFDPointer", META_ULONG, false );
        add( EXIF_IMAGE_DESCRIPTION, "ImageDescription", META_STRING, true );
        add( EXIF_IMAGE_HEIGHT, "ImageHeight", META_USHORT, false );
        add( EXIF_IMAGE_WIDTH, "ImageWidth", META_USHORT, false );
        add( EXIF_INTEROPERABILITY_POINTER, "InteroperabilityPointer", META_ULONG, false );
        add( EXIF_ISO_SPEED_RATINGS, "ISOSpeedRatings", META_USHORT, false );
        add( EXIF_JPEG_INTERCHANGE_FORMAT, "JPEGInterchangeFormat", META_ULONG, false );
        add( EXIF_JPEG_INTERCHANGE_FORMAT_LENGTH, "JPEGInterchangeFormatLength", META_ULONG, false );
        add( EXIF_LIGHT_SOURCE, "LightSource", META_USHORT, false );
        add( EXIF_LENS_INFO, "LensInfo", META_URATIONAL, false );
        add( EXIF_LENS_MAKE, "LensMake", META_STRING, false );
        add( EXIF_LENS_MODEL, "LensModel", META_STRING, false );
        add( EXIF_MAKE, "Make", META_STRING, false );
        add( EXIF_MAKER_NOTE, "MakerNote", META_UNDEFINED, false );
        add( EXIF_MAX_APERTURE_VALUE, "MaxApertureValue", META_URATIONAL, false );
        add( EXIF_METERING_MODE, "MeteringMode", META_USHORT, false );
        add( EXIF_MODEL, "Model", META_STRING, false );
        add( EXIF_MS_RATING, "MSRating", META_USHORT, true );
        add( EXIF_NEW_SUBFILE_TYPE, "NewSubfileType", META_ULONG, false );
        add( EXIF_OECF, "OECF", META_UNDEFINED, false );
        add( EXIF_OECF_2, "OECF2", META_UNDEFINED, false );
        add( EXIF_ORIENTATION, "Orientation", META_USHORT, false );
        add( EXIF_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation", META_USHORT, false );
        add( EXIF_PIXEL_X_DIMENSION, "PixelXDimension", META_USHORT, false );
        add( EXIF_PIXEL_Y_DIMENSION, "PixelYDimension", META_USHORT, false );
        add( EXIF_PLANAR_CONFIGURATION, "PlanarConfiguration", META_USHORT, false );
        add( EXIF_PREDICTOR, "Predictor", META_USHORT, false );
        add( EXIF_PRIMARY_CHROMATICITIES, "PrimaryChromaticities", META_URATIONAL, false );
        add( EXIF_REFERENCE_BLACK_WHITE, "ReferenceBlackWhite", META_URATIONAL, false );
        add( EXIF_RELATED_SOUND_FILE, "RelatedSoundFile", META_STRING, false );
        add( EXIF_RESOLUTION_UNIT, "ResolutionUnit", META_USHORT, false );
        add( EXIF_ROWS_PER_STRIP, "RowsPerStrip", META_USHORT, false );
        add( EXIF_SAMPLES_PER_PIXEL, "SamplesPerPixel", META_USHORT, false );
        add( EXIF_SATURATION, "Saturation", META_USHORT, false );
        add( EXIF_SCENE_CAPTURE_TYPE, "SceneCaptureType", META_USHORT, false );
        add( EXIF_SCENE_TYPE, "SceneType", META_UNDEFINED, false );
        add( EXIF_SENSING_METHOD, "SensingMethod", META_USHORT, false );
        add( EXIF_SHARPNESS, "Sharpness", META_USHORT, false );
        add( EXIF_SHUTTER_SPEED_VALUE, "ShutterSpeedValue", META_SRATIONAL, false );
        add( EXIF_SOFTWARE, "Software", META_STRING, false );
        add( EXIF_SPATIAL_FREQUENCY_RESPONSE, "SpatialFrequencyResponse", META_UNDEFINED, false );
        add( EXIF_SPECTRAL_SENSITIVITY, "SpectralSensitivity", META_STRING, false );
        add( EXIF_STRIP_BYTE_COUNTS, "StripByteCounts", META_SSHORT, false );
        add( EXIF_STRIP_OFFSETS, "StripOffsets", META_USHORT, false );
        add( EXIF_SUBFILE_TYPE, "SubfileType", META_USHORT, false );
        add( EXIF_SUB_IFDS, "SubIFDS", META_ULONG, false );
        add( EXIF_SUBJECT_AREA, "SubjectArea", META_USHORT, false );
        add( EXIF_SUBJECT_DISTANCE, "SubjectDistance", META_URATIONAL, false );
        add( EXIF_SUBJECT_DISTANCE_RANGE, "SubjectDistanceRange", META_USHORT, false );
        add( EXIF_SUBJECT_LOCATION, "SubjectLocation", META_USHORT, false );
        add( EXIF_SUBSEC_TIME, "SubsecTime", META_STRING, false );
        add( EXIF_SUBSEC_TIME_DIGITIZED, "SubsecTimeDigitized", META_STRING, false );
        add( EXIF_SUBSEC_TIME_ORIGINAL, "SubsecTimeOriginal", META_STRING, false );
        add( EXIF_TILE_BYTE_COUNTS, "TileByteCounts", META_SSHORT, false );
        add( EXIF_TILE_LENGTH, "TileLength", META_USHORT, false );
        add( EXIF_TILE_OFFSETS, "TileOffsets", META_ULONG, false );
        add( EXIF_TRANSFER_FUNCTION, "TransferFunction", META_USHORT, false );
        add( EXIF_USER_COMMENT, "UserComment", META_UNDEFINED, false );
        add( EXIF_WHITE_BALANCE, "WhiteBalance", META_USHORT, false );
        add( EXIF_WHITE_POINT, "WhitePoint", META_URATIONAL, false );
        add( EXIF_X_RESOLUTION, "XResolution", META_URATIONAL, false );
        add( EXIF_YCBCR_COEFFICIENTS, "YCbCrCoefficients", META_URATIONAL, false );
        add( EXIF_YCBCR_POSITIONING, "YCbCrPositioning", META_USHORT, false );
        add( EXIF_YCBCR_SUBSAMPLING, "YCbCrSubSampling", META_USHORT, false );
        add( EXIF_Y_RESOLUTION, "YResolution", META_URATIONAL, false );
    }
}
/* vim:set et sw=4 ts=4: */
