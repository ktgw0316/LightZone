/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.values.DateMetaValue;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.metadata.values.RationalMetaValue;
import com.lightcrafts.image.types.JPEGImageType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.*;

import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.ImageOrientation.ORIENTATION_UNKNOWN;
import static com.lightcrafts.image.metadata.TIFFTags.*;
import static com.lightcrafts.image.metadata.XMPConstants.XMP_TIFF_NS;
import static com.lightcrafts.image.metadata.XMPConstants.XMP_TIFF_PREFIX;
import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * An <code>TIFFDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding TIFF metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <i>TIFF Revision 6.0</i>, Adobe Systems, Incorporated, June 1992.
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public class TIFFDirectory extends ImageMetadataDirectory implements
    ApertureProvider, ArtistProvider, BitsPerChannelProvider, CaptionProvider,
    CaptureDateTimeProvider, CopyrightProvider, FlashProvider,
    MakeModelProvider, OrientationProvider, RatingProvider, ResolutionProvider,
    ShutterSpeedProvider, ThumbnailImageProvider, TitleProvider,
    WidthHeightProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public float getAperture() {
        ImageMetaValue value = getValue( TIFF_FNUMBER );
        if ( value == null )
            value = getValue( TIFF_APERTURE_VALUE );
        if ( !(value instanceof RationalMetaValue) )
              return 0;
        return MetadataUtil.fixFStop( value.getFloatValue() );
    }

    /**
     * {@inheritDoc}
     */
    public String getArtist() {
        final ImageMetaValue value = getValue( TIFF_ARTIST );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    public int getBitsPerChannel() {
        final ImageMetaValue value = getValue( TIFF_BITS_PER_SAMPLE );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public String getCameraMake( boolean includeModel ) {
        return getCameraMake( TIFF_MAKE, TIFF_MODEL, includeModel );
    }

    /**
     * {@inheritDoc}
     */
    public String getCaption() {
        final ImageMetaValue value = getValue( TIFF_IMAGE_DESCRIPTION );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    public Date getCaptureDateTime() {
        final ImageMetaValue value = getValue( TIFF_DATE_TIME );
        return  value instanceof DateMetaValue ?
                ((DateMetaValue)value).getDateValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    public String getCopyright() {
        final ImageMetaValue value = getValue( TIFF_COPYRIGHT );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    public int getFlash() {
        final ImageMetaValue flashValue = getValue( TIFF_FLASH );
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
    public int getImageHeight() {
        final ImageMetaValue value = getValue( TIFF_IMAGE_LENGTH );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getImageWidth() {
        final ImageMetaValue value = getValue( TIFF_IMAGE_WIDTH );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;TIFF&quot;.
     */
    public String getName() {
        return "TIFF";
    }

    /**
     * {@inheritDoc}
     */
    public ImageOrientation getOrientation() {
        final ImageMetaValue value = getValue( TIFF_ORIENTATION );
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
    public int getRating() {
        final ImageMetaValue rating = getValue( TIFF_MS_RATING );
        return rating != null ? rating.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRating(int rating) {
        if (rating < 0 || rating > 5)
            throw new IllegalArgumentException("rating must be between 0-5");
        if (rating == 0)
            removeValue(TIFF_MS_RATING);
        else
            setValue(TIFF_MS_RATING, rating);
    }

    /**
     * {@inheritDoc}
     */
    public double getResolution() {
        final ImageMetaValue res = getValue( TIFF_X_RESOLUTION );
        return res != null ? res.getDoubleValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getResolutionUnit() {
        final ImageMetaValue unit = getValue( TIFF_RESOLUTION_UNIT );
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
    public float getShutterSpeed() {
        boolean isAPEX = false;
        ImageMetaValue value = getValue( TIFF_EXPOSURE_TIME );
        if ( value == null ) {
            value = getValue( TIFF_SHUTTER_SPEED_VALUE );
            isAPEX = true;
        }
        if ( !(value instanceof RationalMetaValue) )
            return 0;
        if ( isAPEX )
            return EXIFDirectory.calcShutterSpeedFromAPEX( value );
        return value.getFloatValue();
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
    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return JPEGImageType.getImageFromBuffer(
            imageInfo.getByteBuffer(),
            getValue( TIFF_JPEG_INTERCHANGE_FORMAT ), 0,
            getValue( TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH ),
            0, 0
        );
    }

    /**
     * {@inheritDoc}
     */
    public String getTitle() {
        final ImageMetaValue value = getValue( TIFF_DOCUMENT_NAME );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Element> toXMP( Document xmpDoc  ) {
        return toXMP( xmpDoc, XMP_TIFF_NS, XMP_TIFF_PREFIX );
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
    protected int getProviderPriorityFor(Class<? extends ImageMetadataProvider> p) {
        return PROVIDER_PRIORITY_DEFAULT + 5;
    }

    /**
     * {@inheritDoc}.
     */
    protected ResourceBundle getTagLabelBundle() {
        return m_tagBundle;
    }

    /**
     * {@inheritDoc}
     */
    protected Class<? extends ImageMetaTags> getTagsInterface() {
        return TIFFTags.class;
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
        "com.lightcrafts.image.metadata.TIFFTags"
    );

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

    static {
        add( TIFF_APERTURE_VALUE, "ApertureValue", META_URATIONAL, false );
        add( TIFF_ARTIST, "Artist", META_STRING, true );
        add( TIFF_BATTERY_LEVEL, "BatteryLevel", META_STRING, false );
        add( TIFF_BITS_PER_SAMPLE, "BitsPerSample", META_USHORT, false );
        add( TIFF_BRIGHTNESS_VALUE, "BrightnessValue", META_SRATIONAL, false );
        add( TIFF_CELL_LENGTH, "CellLength", META_USHORT, false );
        add( TIFF_CELL_WIDTH, "CellWidth", META_USHORT, false );
        add( TIFF_CLIP_PATH, "ClipPath", META_UBYTE, false );
        add( TIFF_COLOR_MAP, "ColorMap", META_USHORT, false );
        add( TIFF_COMPRESSION, "Compression", META_USHORT, false );
        add( TIFF_COPYRIGHT, "Copyright", META_STRING, true );
        add( TIFF_DATE_TIME, "DateTime", META_DATE, false );
        add( TIFF_DOCUMENT_NAME, "DocumentName", META_STRING, true );
        add( TIFF_DOT_RANGE, "DotRange", META_USHORT, false );
        add( TIFF_EXIF_IFD_POINTER, "EXIFIFDPointer", META_ULONG, false );
        add( TIFF_EXPOSURE_BIAS_VALUE, "ExposureBiasValue", META_SRATIONAL, false );
        add( TIFF_EXPOSURE_PROGRAM, "ExposureProgram", META_USHORT, false );
        add( TIFF_EXPOSURE_INDEX, "ExposureIndex", META_URATIONAL, false );
        add( TIFF_EXPOSURE_TIME, "ExposureTime", META_URATIONAL, false );
        add( TIFF_EXTRA_SAMPLES, "ExtraSamples", META_USHORT, false );
        add( TIFF_FILL_ORDER, "FillOrder", META_USHORT, false );
        add( TIFF_FLASH, "Flash", META_USHORT, false );
        add( TIFF_FLASH_ENERGY, "FlashEnergy", META_URATIONAL, false );
        add( TIFF_FNUMBER, "FNumber", META_URATIONAL, false );
        add( TIFF_FREE_BYTE_COUNTS, "FreeByteCounts", META_ULONG, false );
        add( TIFF_FREE_OFFSETS, "FreeOffsets", META_ULONG, false );
        add( TIFF_GPS_IFD_POINTER, "GPSInfoIfdPointer", META_ULONG, false );
        add( TIFF_GRAY_RESPONSE_CURVE, "GrayResponseCurve", META_USHORT, false );
        add( TIFF_GRAY_RESPONSE_UNIT, "GrayResponseUnit", META_USHORT, false );
        add( TIFF_HALFTONE_HINTS, "HalftoneHints", META_USHORT, false );
        add( TIFF_HOST_COMPUTER, "HostComputer", META_STRING, false );
        add( TIFF_ICC_PROFILE, "ICCProfile", META_UNDEFINED, false );
        add( TIFF_IMAGE_DESCRIPTION, "ImageDescription", META_STRING, true );
        add( TIFF_IMAGE_HISTORY, "ImageHistory", META_STRING, false );
        add( TIFF_IMAGE_ID, "ImageID", META_STRING, true );
        add( TIFF_IMAGE_LENGTH, "ImageLength", META_USHORT, false );
        add( TIFF_IMAGE_WIDTH, "ImageWidth", META_USHORT, false );
        add( TIFF_INDEXED, "Indexed", META_USHORT, false );
        add( TIFF_INK_NAMES, "InkNames", META_STRING, false );
        add( TIFF_INK_SET, "InkSet", META_USHORT, false );
        add( TIFF_JPEG_AC_TABLES, "JPEGACTables", META_ULONG, false );
        add( TIFF_JPEG_DC_TABLES, "JPEGDCTables", META_ULONG, false );
        add( TIFF_JPEG_INTERCHANGE_FORMAT, "JPEGInterchangeFormat", META_ULONG, false );
        add( TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH, "JPEGInterchangeFormatLength", META_ULONG, false );
        add( TIFF_JPEG_LOSSLESS_PREDICTORS, "JPEGLosslessPredictors", META_USHORT, false );
        add( TIFF_JPEG_POINT_TRANSFORMS, "JPEGPointTransforms", META_USHORT, false );
        add( TIFF_JPEG_PROC, "JPEGProc", META_USHORT, false );
        add( TIFF_JPEG_Q_TABLES, "JPEGQTables", META_ULONG, false );
        add( TIFF_JPEG_RESTART_INTERVAL, "JPEGRestartInterval", META_USHORT, false );
        add( TIFF_LIGHT_SOURCE, "LightSource", META_USHORT, false );
        add( TIFF_MAKE, "Make", META_STRING, false );
        add( TIFF_MAX_APERTURE_VALUE, "MaxApertureValue", META_URATIONAL, false );
        add( TIFF_MAX_SAMPLE_VALUE, "MaxSampleValue", META_USHORT, false );
        add( TIFF_METERING_MODE, "MeteringMode", META_USHORT, false );
        add( TIFF_MIN_SAMPLE_VALUE, "MinSampleValue", META_USHORT, false );
        add( TIFF_MODEL, "Model", META_STRING, false );
        add( TIFF_MS_RATING, "MSRating", META_USHORT, true );
        add( TIFF_NEW_SUBFILE_TYPE, "NewSubfileType", META_ULONG, false );
        add( TIFF_NUMBER_OF_INKS, "NumberOfInks", META_USHORT, false );
        add( TIFF_OPI_PROXY, "OPIProxy", META_USHORT, false );
        add( TIFF_ORIENTATION, "Orientation", META_USHORT, false );
        add( TIFF_PAGE_NAME, "PageName", META_STRING, false );
        add( TIFF_PAGE_NUMBER, "PageNumber", META_USHORT, false );
        add( TIFF_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation", META_USHORT, false );
        add( TIFF_PHOTOSHOP_IMAGE_RESOURCES, "PhotoshopImageResources", META_UBYTE, false );
        add( TIFF_PLANAR_CONFIGURATION, "PlanarConfiguration", META_USHORT, false );
        add( TIFF_PREDICTOR, "Predictor", META_USHORT, false );
        add( TIFF_PRIMARY_CHROMATICITIES, "PrimaryChromaticities", META_URATIONAL, false );
        add( TIFF_REFERENCE_BLACK_WHITE, "ReferenceBlackWhite", META_URATIONAL, false );
        add( TIFF_RESOLUTION_UNIT, "ResolutionUnit", META_USHORT, false );
        add( TIFF_RICH_TIFF_IPTC, "RichTIFFIPTC", META_UNDEFINED, false );
        add( TIFF_ROWS_PER_STRIP, "RowsPerStrip", META_USHORT, false );
        add( TIFF_SAMPLE_FORMAT, "SampleFormat", META_USHORT, false );
        add( TIFF_SAMPLES_PER_PIXEL, "SamplesPerPixel", META_USHORT, false );
        add( TIFF_SECURITY_CLASSIFICATION, "SecurityClassification", META_STRING, false );
        add( TIFF_SENSING_METHOD, "SensingMethod", META_USHORT, false );
        add( TIFF_SHUTTER_SPEED_VALUE, "ShutterSpeedValue", META_SRATIONAL, false );
        add( TIFF_SMAX_SAMPLE_VALUE, "SMaxSampleValue", META_SRATIONAL, false );
        add( TIFF_SMIN_SAMPLE_VALUE, "SMinSampleValue", META_SRATIONAL, false );
        add( TIFF_SOFTWARE, "Software", META_STRING, false );
        add( TIFF_SPECTRAL_SENSITIVITY, "SpectralSensitivity", META_STRING, false );
        add( TIFF_STRIP_BYTE_COUNTS, "StripByteCounts", META_ULONG, false );
        add( TIFF_STRIP_OFFSETS, "StripOffsets", META_ULONG, false );
        add( TIFF_SUBFILE_TYPE, "SubfileType", META_USHORT, false );
        add( TIFF_SUB_IFDS, "SubIFDs", META_ULONG, false );
        add( TIFF_SUBJECT_DISTANCE, "SubjectDistance", META_URATIONAL, false );
        add( TIFF_SUBJECT_LOCATION, "SubjectLocation", META_USHORT, false );
        add( TIFF_T4_OPTIONS, "T4Options", META_ULONG, false );
        add( TIFF_T6_OPTIONS, "T6Options", META_ULONG, false );
        add( TIFF_TARGET_PRINTER, "TargetPrinter", META_STRING, true );
        add( TIFF_THRESHHOLDING, "Threshholding", META_USHORT, false );
        add( TIFF_TIFF_EP_STANDARD_ID, "TIFFEPStandardID", META_UBYTE, false );
        add( TIFF_TILE_BYTE_COUNTS, "TileByteCounts", META_ULONG, false );
        add( TIFF_TILE_LENGTH, "TileLength", META_ULONG, false );
        add( TIFF_TILE_OFFSETS, "TileOffsets", META_ULONG, false );
        add( TIFF_TILE_WIDTH, "TileWidth", META_ULONG, false );
        add( TIFF_TRANSFER_FUNCTION, "TransferFunction", META_USHORT, false );
        add( TIFF_TRANSFER_RANGE, "TransferRange", META_USHORT, false );
        add( TIFF_WHITE_POINT, "WhitePoint", META_URATIONAL, false );
        add( TIFF_X_CLIP_PATH_UNITS, "XClipPathUnits", META_ULONG, false );
        add( TIFF_XMP_PACKET, "XMPPacket", META_UBYTE, false );
        add( TIFF_X_POSITION, "XPosition", META_URATIONAL, false );
        add( TIFF_X_RESOLUTION, "XResolution", META_URATIONAL, false );
        add( TIFF_YCBCR_COEFFICIENTS, "YCbCrCoefficients", META_URATIONAL, false );
        add( TIFF_YCBCR_POSITIONING, "YCbCrPositioning", META_USHORT, false );
        add( TIFF_YCBCR_SUBSAMPLING, "YCbCrSubSampling", META_USHORT, false );
        add( TIFF_Y_CLIP_PATH_UNITS, "YClipPathUnits", META_ULONG, false );
        add( TIFF_Y_POSITION, "YPosition", META_URATIONAL, false );
        add( TIFF_Y_RESOLUTION, "YResolution", META_URATIONAL, false );
    }
}
/* vim:set et sw=4 ts=4: */
