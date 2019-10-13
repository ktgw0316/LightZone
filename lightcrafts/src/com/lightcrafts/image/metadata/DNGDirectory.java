/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.lightcrafts.image.metadata.providers.FocalLengthProvider;
import com.lightcrafts.image.metadata.providers.ISOProvider;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.metadata.values.ByteMetaValue;
import com.lightcrafts.image.metadata.providers.LensProvider;

import static com.lightcrafts.image.metadata.DNGTags.*;
import static com.lightcrafts.image.metadata.EXIFTags.*;
import static com.lightcrafts.image.metadata.ImageMetaType.*;

/**
 * An <code>DNGDirectory</code> extends {@link TIFFDirectory} for holding DNG
 * metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class DNGDirectory extends TIFFDirectory implements
        LensProvider, FocalLengthProvider, ISOProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCameraMake( boolean includeModel ) {
        //
        // First try the TIFF Make (and Model) fields.
        //
        final String make = super.getCameraMake( includeModel );
        if ( make == null && includeModel ) {
            //
            // If there was no TIFF Make field, and the model was requested,
            // try the DNG-specific UniqueCameraModel field.
            //
            final ImageMetaValue value = getValue( DNG_UNIQUE_CAMERA_MODEL );
            if ( value != null )
                return value.getStringValue().toUpperCase().trim();
        }
        return make;
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
    public int getISO() {
        final ImageMetaValue value = getValue( EXIF_ISO_SPEED_RATINGS );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLens() {
        ImageMetaValue lens = getValue( DNG_LENS_INFO );
        if (lens == null) {
            lens = getValue( EXIF_LENS_MODEL );
        }
        return lens != null ? lens.toString() : null;
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;DNG&quot;.
     */
    @Override
    public String getName() {
        return "DNG";
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
    @Override
    public String valueToString( ImageMetaValue value ) {
        final int tagID = value.getOwningTagID();
        switch ( tagID ) {
            case DNG_LENS_INFO: {
                final String lensLabel = makeLensLabelFrom( value );
                if ( lensLabel != null )
                    return lensLabel;
                break;
            }
            case DNG_LOCALIZED_CAMERA_MODEL:
            case DNG_ORIGINAL_RAW_FILE_NAME:
                switch ( value.getType() ) {
                    case META_SBYTE:
                    case META_UBYTE:
                        try {
                            return new String(
                                ((ByteMetaValue)value).getByteValues(), "UTF-8"
                            );
                        }
                        catch ( UnsupportedEncodingException e ) {
                            // should never happen
                        }
                }
                break;
        }
        return super.valueToString( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}.
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
        return DNGTags.class;
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
    @SuppressWarnings({"MethodOverridesPrivateMethodOfSuperclass"})
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
        "com.lightcrafts.image.metadata.DNGTags"
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
        add( DNG_ACTIVE_AREA, "ActiveArea", META_USHORT, false );
        add( DNG_ANALOG_BALANCE, "AnalogBalance", META_URATIONAL, false );
        add( DNG_ANTI_ALIAS_STRENGTH, "AntiAliasStrength", META_URATIONAL, false );
        add( DNG_AS_SHOT_ICC_PROFILE, "AsShotICCProfile", META_UNDEFINED, false );
        add( DNG_AS_SHOT_NEUTRAL, "AsShotNeutral", META_URATIONAL, false );
        add( DNG_AS_SHOT_PRE_PROFILE_MATRIX, "AsShotPreProfileMatrix", META_SRATIONAL, false );
        add( DNG_AS_SHOT_WHITE_XY, "AsShotWhiteXY", META_URATIONAL, false );
        add( DNG_BACKWARD_VERSION, "BackwardVersion", META_UBYTE, false );
        add( DNG_BASELINE_EXPOSURE, "BaselineExposure", META_SRATIONAL, false );
        add( DNG_BASELINE_NOISE, "BaselineNoise", META_URATIONAL, false );
        add( DNG_BASELINE_SHARPNESS, "BaselineSharpness", META_URATIONAL, false );
        add( DNG_BAYER_GREEN_SPLIT, "BayerGreenSplit", META_ULONG, false );
        add( DNG_BEST_QUALITY_SCALE, "BestQualityScale", META_URATIONAL, false );
        add( DNG_BLACK_LEVEL, "BlackLevel", META_URATIONAL, false );
        add( DNG_BLACK_LEVEL_DELTA_H, "BlackLevelDeltaH", META_SRATIONAL, false );
        add( DNG_BLACK_LEVEL_DELTA_V, "BlackLevelDeltaV", META_SRATIONAL, false );
        add( DNG_BLACK_LEVEL_REPEAT_DIM, "BlackLevelRepeatDim", META_USHORT, false );
        add( DNG_CALIBRATION_ILLUMINANT_1, "CalibrationIlluminant1", META_USHORT, false );
        add( DNG_CALIBRATION_ILLUMINANT_2, "CalibrationIlluminant2", META_USHORT, false );
        add( DNG_CAMERA_CALIBRATION_1, "CameraCalibration1", META_SRATIONAL, false );
        add( DNG_CAMERA_CALIBRATION_2, "CameraCalibration2", META_SRATIONAL, false );
        add( DNG_CAMERA_SERIAL_NUMBER, "CameraSerialNumber", META_STRING, false );
        add( DNG_CFA_LAYOUT, "CFALayout", META_USHORT, false );
        add( DNG_CFA_PLANE_COLOR, "CFAPlaneColor", META_UBYTE, false );
        add( DNG_CHROMA_BLUR_RADIUS, "ChromaBlurRadius", META_URATIONAL, false );
        add( DNG_COLOR_MATRIX_1, "ColorMatrix1", META_SRATIONAL, false );
        add( DNG_COLOR_MATRIX_2, "ColorMatrix2", META_SRATIONAL, false );
        add( DNG_CURRENT_ICC_PROFILE, "CurrentICCProfile", META_UNDEFINED, false );
        add( DNG_CURRENT_PRE_PROFILE_MATRIX, "CurrentPreProfileMatrix", META_SRATIONAL, false );
        add( DNG_DEFAULT_CROP_ORIGIN, "DefaultCropOrigin", META_URATIONAL, false );
        add( DNG_DEFAULT_CROP_SIZE, "DefaultCropSize", META_URATIONAL, false );
        add( DNG_DEFAULT_SCALE, "DefaultScale", META_URATIONAL, false );
        add( DNG_LENS_INFO, "LensInfo", META_URATIONAL, false );
        add( DNG_LINEARIZATION_TABLE, "LinearizationTable", META_USHORT, false );
        add( DNG_LINEAR_RESPONSE_LIMIT, "LinearResponseLimit", META_URATIONAL, false );
        add( DNG_LOCALIZED_CAMERA_MODEL, "LocalizedCameraModel", META_STRING, false );
        add( DNG_MAKER_NOTE_SAFETY, "MakerNoteSafety", META_USHORT, false );
        add( DNG_MASKED_AREAS, "MaskedAreas", META_ULONG, false );
        add( DNG_ORIGINAL_RAW_FILE_DATA, "OriginalRawFileData", META_UNDEFINED, false );
        add( DNG_ORIGINAL_RAW_FILE_NAME, "OriginalRawFileName", META_STRING, false );
        add( DNG_PRIVATE_DATA, "PrivateData", META_UBYTE, false );
        add( DNG_RAW_DATA_UNIQUE_ID, "RawDataUniqueID", META_UBYTE, false );
        add( DNG_REDUCTION_MATRIX_1, "ReductionMatrix1", META_SRATIONAL, false );
        add( DNG_REDUCTION_MATRIX_2, "ReductionMatrix2", META_SRATIONAL, false );
        add( DNG_SHADOW_SCALE, "ShadowScale", META_URATIONAL, false );
        add( DNG_UNIQUE_CAMERA_MODEL, "UniqueCameraModel", META_STRING, false );
        add( DNG_VERSION, "Version", META_UBYTE, false );
        add( DNG_WHITE_LEVEL, "WhiteLevel", META_ULONG, false );

        ////////// Copied from TIFF ///////////////////////////////////////////

        add( DNG_ARTIST, "Artist", META_STRING, true );
        add( DNG_BITS_PER_SAMPLE, "BitsPerSample", META_USHORT, false );
        add( DNG_CELL_LENGTH, "CellLength", META_USHORT, false );
        add( DNG_CELL_WIDTH, "CellWidth", META_USHORT, false );
        add( DNG_COLOR_MAP, "ColorMap", META_USHORT, false );
        add( DNG_COMPRESSION, "Compression", META_USHORT, false );
        add( DNG_COPYRIGHT, "Copyright", META_STRING, true );
        add( DNG_DATE_TIME, "DateTime", META_DATE, false );
        add( DNG_DOCUMENT_NAME, "DocumentName", META_STRING, true );
        add( DNG_DOT_RANGE, "DotRange", META_USHORT, false );
        add( DNG_EXIF_IFD_POINTER, "EXIFIFDPointer", META_ULONG, false );
        add( DNG_EXTRA_SAMPLES, "ExtraSamples", META_USHORT, false );
        add( DNG_FILL_ORDER, "FillOrder", META_USHORT, false );
        add( DNG_FREE_BYTE_COUNTS, "FreeByteCounts", META_ULONG, false );
        add( DNG_FREE_OFFSETS, "FreeOffsets", META_ULONG, false );
        add( DNG_GRAY_RESPONSE_CURVE, "GrayResponseCurve", META_USHORT, false );
        add( DNG_GRAY_RESPONSE_UNIT, "GrayResponseUnit", META_USHORT, false );
        add( DNG_HALFTONE_HINTS, "HalftoneHints", META_USHORT, false );
        add( DNG_HOST_COMPUTER, "HostComputer", META_STRING, false );
        add( DNG_ICC_PROFILE, "ICCProfile", META_UNDEFINED, false );
        add( DNG_IMAGE_DESCRIPTION, "ImageDescription", META_STRING, true );
        add( DNG_IMAGE_ID, "ImageID", META_STRING, true );
        add( DNG_IMAGE_LENGTH, "ImageLength", META_USHORT, false );
        add( DNG_IMAGE_WIDTH, "ImageWidth", META_USHORT, false );
        add( DNG_INDEXED, "Indexed", META_USHORT, false );
        add( DNG_INK_NAMES, "InkNames", META_STRING, false );
        add( DNG_INK_SET, "InkSet", META_USHORT, false );
        add( DNG_JPEG_AC_TABLES, "JPEGACTables", META_ULONG, false );
        add( DNG_JPEG_DC_TABLES, "JPEGDCTables", META_ULONG, false );
        add( DNG_JPEG_INTERCHANGE_FORMAT, "JPEGInterchangeFormat", META_ULONG, false );
        add( DNG_JPEG_INTERCHANGE_FORMAT_LENGTH, "JPEGInterchangeFormatLength", META_ULONG, false );
        add( DNG_JPEG_LOSSLESS_PREDICTORS, "JPEGLosslessPredictors", META_USHORT, false );
        add( DNG_JPEG_POINT_TRANSFORMS, "JPEGPointTransforms", META_USHORT, false );
        add( DNG_JPEG_PROC, "JPEGProc", META_USHORT, false );
        add( DNG_JPEG_Q_TABLES, "JPEGQTables", META_ULONG, false );
        add( DNG_JPEG_RESTART_INTERVAL, "JPEGRestartInterval", META_USHORT, false );
        add( DNG_MAKE, "Make", META_STRING, false );
        add( DNG_MAX_SAMPLE_VALUE, "MaxSampleValue", META_USHORT, false );
        add( DNG_MIN_SAMPLE_VALUE, "MinSampleValue", META_USHORT, false );
        add( DNG_MODEL, "Model", META_STRING, false );
        add( DNG_NEW_SUBFILE_TYPE, "NewSubfileType", META_ULONG, false );
        add( DNG_NUMBER_OF_INKS, "NumberOfInks", META_USHORT, false );
        add( DNG_OPI_PROXY, "OPIProxy", META_USHORT, false );
        add( DNG_ORIENTATION, "Orientation", META_USHORT, false );
        add( DNG_PAGE_NAME, "PageName", META_STRING, false );
        add( DNG_PAGE_NUMBER, "PageNumber", META_USHORT, false );
        add( DNG_PHOTOMETRIC_INTERPRETATION, "PhotometricInterpretation", META_USHORT, false );
        add( DNG_PLANAR_CONFIGURATION, "PlanarConfiguration", META_USHORT, false );
        add( DNG_PREDICTOR, "Predictor", META_USHORT, false );
        add( DNG_PRIMARY_CHROMATICITIES, "PrimaryChromaticities", META_URATIONAL, false );
        add( DNG_REFERENCE_BLACK_WHITE, "ReferenceBlackWhite", META_URATIONAL, false );
        add( DNG_RESOLUTION_UNIT, "ResolutionUnit", META_USHORT, false );
        add( DNG_RICH_TIFF_IPTC, "RichTIFFIPTC", META_UNDEFINED, false );
        add( DNG_ROWS_PER_STRIP, "RowsPerStrip", META_USHORT, false );
        add( DNG_SAMPLE_FORMAT, "SampleFormat", META_USHORT, false );
        add( DNG_SAMPLES_PER_PIXEL, "SamplesPerPixel", META_USHORT, false );
        add( DNG_SMAX_SAMPLE_VALUE, "SMaxSampleValue", META_SRATIONAL, false );
        add( DNG_SMIN_SAMPLE_VALUE, "SMinSampleValue", META_SRATIONAL, false );
        add( DNG_SOFTWARE, "Software", META_STRING, false );
        add( DNG_STRIP_BYTE_COUNTS, "StripByteCounts", META_ULONG, false );
        add( DNG_STRIP_OFFSETS, "StripOffsets", META_ULONG, false );
        add( DNG_SUBFILE_TYPE, "SubfileType", META_USHORT, false );
        add( DNG_T4_OPTIONS, "T4Options", META_ULONG, false );
        add( DNG_T6_OPTIONS, "T6Options", META_ULONG, false );
        add( DNG_TARGET_PRINTER, "TargetPrinter", META_STRING, true );
        add( DNG_THRESHHOLDING, "Threshholding", META_USHORT, false );
        add( DNG_TILE_BYTE_COUNTS, "TileByteCounts", META_ULONG, false );
        add( DNG_TILE_LENGTH, "TileLength", META_ULONG, false );
        add( DNG_TILE_OFFSETS, "TileOffsets", META_ULONG, false );
        add( DNG_TILE_WIDTH, "TileWidth", META_ULONG, false );
        add( DNG_TRANSFER_FUNCTION, "TransferFunction", META_USHORT, false );
        add( DNG_TRANSFER_RANGE, "TransferRange", META_USHORT, false );
        add( DNG_WHITE_POINT, "WhitePoint", META_URATIONAL, false );
        add( DNG_X_CLIP_PATH_UNITS, "XClipPathUnits", META_ULONG, false );
        add( DNG_XMP_PACKET, "XMPPacket", META_SBYTE, false );
        add( DNG_X_POSITION, "XPosition", META_URATIONAL, false );
        add( DNG_X_RESOLUTION, "XResolution", META_URATIONAL, false );
        add( DNG_YCBCR_COEFFICIENTS, "YCbCrCoefficients", META_URATIONAL, false );
        add( DNG_YCBCR_POSITIONING, "YCbCrPositioning", META_USHORT, false );
        add( DNG_YCBCR_SUBSAMPLING, "YCbCrSubSampling", META_USHORT, false );
        add( DNG_Y_CLIP_PATH_UNITS, "YClipPathUnits", META_ULONG, false );
        add( DNG_Y_POSITION, "YPosition", META_URATIONAL, false );
        add( DNG_Y_RESOLUTION, "YResolution", META_URATIONAL, false );

        ////////// Copied from EXIF ///////////////////////////////////////////

        add( EXIF_FOCAL_LENGTH, "FocalLength", META_URATIONAL, false );
        add( EXIF_ISO_SPEED_RATINGS, "ISOSpeedRatings", META_USHORT, false );
        add( EXIF_LENS_MODEL, "LensModel", META_STRING, false );
    }
}
/* vim:set et sw=4 ts=4: */
