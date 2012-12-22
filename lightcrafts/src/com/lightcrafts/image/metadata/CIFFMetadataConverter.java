/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.metadata.values.*;

import static com.lightcrafts.image.metadata.CIFFTags.*;
import static com.lightcrafts.image.metadata.EXIFConstants.*;
import static com.lightcrafts.image.metadata.EXIFTags.*;
import static com.lightcrafts.image.metadata.TIFFTags.*;
import static com.lightcrafts.image.types.CIFFConstants.*;
import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * <code>CIFFMetadataConverter</code> is used to convert the metadata values in
 * a {@link CIFFDirectory} to those in a {@link TIFFDirectory} and/or an
 * {@link EXIFDirectory}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class CIFFMetadataConverter {

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Convert the metadata values in a {@link CIFFDirectory} to those in a
     * {@link TIFFDirectory} and possibly an {@link EXIFDirectory}.
     *
     * @param ciffDir The {@link CIFFDirectory} whose values to convert.
     * @param forJPEG Should be <code>true</code> if the converted metadata is
     * for a JPEG file; should be <code>false</code> for a TIFF file.
     */
    static ImageMetadata convert( CIFFDirectory ciffDir, boolean forJPEG ) {
        return new CIFFMetadataConverter( ciffDir, forJPEG ).convert();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a new <code>CIFFMetadataConverter</code>.
     *
     * @param ciffDir The {@link CIFFDirectory} whose values to convert.
     * @param forJPEG Should be <code>true</code> if the converted metadata is
     * for a JPEG file; should be <code>false</code> for a TIFF file.
     */
    private CIFFMetadataConverter( CIFFDirectory ciffDir, boolean forJPEG ) {
        m_ciffDir = ciffDir;
        m_metadata = new ImageMetadata();
        m_exifDir = m_metadata.getDirectoryFor( EXIFDirectory.class, true );

        if ( forJPEG ) {
            //
            // TIFF metadata inside JPEG files is actually in the EXIF metadata
            // in the EXIF/TIFF-overlapping tags, so just point tiffDir at
            // exifDir.
            //
            m_tiffDir = m_exifDir;
        } else
            m_tiffDir = m_metadata.getDirectoryFor( TIFFDirectory.class, true );

        m_tiffDir.putValue(
            TIFF_EXIF_IFD_POINTER,
            //
            // We just need an EXIF IFD pointer in the TIFF metadata
            // directory.  The actual value of the pointer isn't used so 0
            // is fine.
            //
            new UnsignedLongMetaValue( 0 )
        );
    }

    /**
     * Convert the metadata values in a {@link CIFFDirectory} to those in a
     * {@link TIFFDirectory} and possibly an {@link EXIFDirectory}.
     *
     * @return Returns a new {@link ImageMetadata} containing the converted
     * values.
     */
    private ImageMetadata convert() {

        ////////// TIFF metadata //////////////////////////////////////////////

        map( CIFF_II_COMPONENT_BIT_DEPTH, TIFF_BITS_PER_SAMPLE  , m_tiffDir );
        map( CIFF_II_IMAGE_HEIGHT       , TIFF_IMAGE_LENGTH     , m_tiffDir );
        map( CIFF_II_IMAGE_WIDTH        , TIFF_IMAGE_WIDTH      , m_tiffDir );
        map( CIFF_II_ROTATION           , TIFF_ORIENTATION      , m_tiffDir );
        map( CIFF_IMAGE_DESCRIPTION     , TIFF_IMAGE_DESCRIPTION, m_tiffDir );
        map( CIFF_OWNER_NAME            , TIFF_ARTIST           , m_tiffDir );
        map( CIFF_PI_IMAGE_HEIGHT       , TIFF_IMAGE_LENGTH     , m_tiffDir );
        map( CIFF_PI_IMAGE_WIDTH        , TIFF_IMAGE_WIDTH      , m_tiffDir );
        map( CIFF_SI_AUTO_ROTATE        , TIFF_ORIENTATION      , m_tiffDir );

        final String make = m_ciffDir.getCameraMake( false );
        if ( make != null )
            m_tiffDir.putValue( TIFF_MAKE, new StringMetaValue( make ) );
        final String model = m_ciffDir.getCameraModel();
        if ( model != null )
            m_tiffDir.putValue( TIFF_MODEL, new StringMetaValue( model ) );

        ////////// EXIF metadata //////////////////////////////////////////////

        map( CIFF_CAPTURED_TIME   , EXIF_DATE_TIME_ORIGINAL, m_exifDir );
        map( CIFF_COLOR_SPACE     , EXIF_COLOR_SPACE       , m_exifDir );
        map( CIFF_CS_CONTRAST     , EXIF_CONTRAST          , m_exifDir );
        map( CIFF_CS_EXPOSURE_MODE, EXIF_EXPOSURE_PROGRAM  , m_exifDir );
        map( CIFF_CS_FLASH_MODE   , EXIF_FLASH             , m_exifDir );
        map( CIFF_CS_ISO          , EXIF_ISO_SPEED_RATINGS , m_exifDir );
        map( CIFF_CS_METERING_MODE, EXIF_METERING_MODE     , m_exifDir );
        map( CIFF_CS_SATURATION   , EXIF_SATURATION        , m_exifDir );
        map( CIFF_CS_SHARPNESS    , EXIF_SHARPNESS         , m_exifDir );
        map( CIFF_FL_FOCAL_LENGTH , EXIF_FOCAL_LENGTH      , m_exifDir );
        map(
            CIFF_FL_FOCAL_PLANE_X_SIZE, EXIF_FOCAL_PLANE_X_RESOLUTION,
            m_exifDir
        );
        map(
            CIFF_FL_FOCAL_PLANE_Y_SIZE, EXIF_FOCAL_PLANE_Y_RESOLUTION,
            m_exifDir
        );
        map( CIFF_SI_FNUMBER      , EXIF_APERTURE_VALUE   , m_exifDir );
        map( CIFF_SI_ISO          , EXIF_ISO_SPEED_RATINGS, m_exifDir );
        map( CIFF_SI_SHUTTER_SPEED, EXIF_EXPOSURE_TIME    , m_exifDir );

        return m_metadata;
    }

    /**
     * Map the value of a particular tag to the value for another tag.
     *
     * @param fromTagID The tag ID to map from.
     * @param toTagID The tag ID to map to.
     * @param toDir The destination {@link ImageMetadataDirectory}.
     */
    private void map( int fromTagID, int toTagID,
                      ImageMetadataDirectory toDir ) {
        ImageMetaValue value = m_ciffDir.getValue( fromTagID );
        if ( value == null )
            return;

        boolean setOwner = true;
        switch ( fromTagID ) {
            case CIFF_CS_CONTRAST: {
                short n = value.getShortValue();
                switch ( n ) {
                    case -1:    // low
                        n = EXIF_CONTRAST_LOW_SATURATION;
                        break;
                    case 0:     // normal
                        // value maps as-is
                        break;
                    case 1:     // high
                        n = EXIF_CONTRAST_HARD;
                        break;
                    default:    // no equivalent
                        return;
                }
                value = new UnsignedShortMetaValue( n );
                break;
            }
            case CIFF_CS_EXPOSURE_MODE: {
                short n = value.getShortValue();
                switch ( n ) {
                    case 1:     // program
                        n = EXIF_EXPOSURE_PROGRAM_NORMAL;
                        break;
                    case 2:     // Tv priority
                        n = EXIF_EXPOSURE_PROGRAM_SHUTTER_PRIORITY;
                        break;
                    case 4:     // manual
                        n = EXIF_EXPOSURE_PROGRAM_MANUAL;
                        break;
                    case 5:     // A-DEP
                        n = EXIF_EXPOSURE_PROGRAM_ACTION;
                        break;
                    default:    // no equivalent
                        return;
                }
                value = new UnsignedShortMetaValue( n );
                break;
            }
            case CIFF_CS_FLASH_MODE: {
                short exifFlashMode = 0;
                switch ( value.getShortValue() ) {
                    case 0:     // flash not fired
                        // value maps as-is
                        break;

                    case 3:     // red-eye reduction + flash fired
                        exifFlashMode = 1 << 6 | 1;
                        break;

                    case 5:     // auto + red-eye reduction + flash fired
                        exifFlashMode = 1 << 6 | 1;
                    case 1:     // auto + flash did not fire
                        exifFlashMode |= 3 << 3;
                        break;

                    case 6:     // on + red-eye reduction + flash fired
                        exifFlashMode = 1 << 6;
                    case 2:     // on + flash fired
                        exifFlashMode |= 1 << 3 | 1;
                        break;

                    default:    // no equivalent
                        return;
                }
                value = new UnsignedShortMetaValue( exifFlashMode );
                break;
            }
            case CIFF_CS_ISO:
            case CIFF_SI_ISO: {
                final short apex = value.getShortValue();
                if ( apex == 0 )
                    return;
                value = new UnsignedShortMetaValue(
                    MetadataUtil.convertISOFromAPEX( apex )
                );
                break;
            }
            case CIFF_CS_METERING_MODE: {
                short n = value.getShortValue();
                switch ( n ) {
                    case 4:     // partial
                        n = EXIF_METERING_MODE_PARTIAL;
                        break;
                    case 5:     // center-weighted
                        n = EXIF_METERING_MODE_CENTER_WEIGHTED_AVERAGE;
                        break;
                    default:    // no equivalent
                        return;
                }
                value = new UnsignedShortMetaValue( n );
                break;
            }
            case CIFF_COLOR_SPACE:
                //
                // Value maps as-is.
                //
                value = new UnsignedShortMetaValue( value.getShortValue() );
                break;
            case CIFF_CS_SATURATION:
            case CIFF_CS_SHARPNESS: {
                short n = value.getShortValue();
                switch ( n ) {
                    case -1:    // low
                        n = EXIF_SHARPNESS_SOFT;
                        break;
                    case 0:     // normal
                        // value maps as-is
                        break;
                    case 1:     // high
                        n = EXIF_SHARPNESS_HARD;
                        break;
                }
                value = new UnsignedShortMetaValue( n );
                break;
            }
            case CIFF_FL_FOCAL_LENGTH:
            case CIFF_FL_FOCAL_PLANE_X_SIZE:
            case CIFF_FL_FOCAL_PLANE_Y_SIZE: {
                final int n = value.getIntValue();
                value = new UnsignedRationalMetaValue( n, 1 );
                break;
            }
            case CIFF_II_COMPONENT_BIT_DEPTH: {
                final int n = value.getIntValue();
                value = new UnsignedShortMetaValue( n, n, n );
                break;
            }
            case CIFF_II_ROTATION: {
                int orientation = value.getIntValue();
                if ( orientation < 0 )
                    orientation = 360 + orientation;
                switch ( orientation ) {
                    case 0:
                        orientation = TIFF_ORIENTATION_LANDSCAPE;
                        break;
                    case 90:
                        orientation = TIFF_ORIENTATION_90CCW;
                        break;
                    case 180:
                        orientation = TIFF_ORIENTATION_180;
                        break;
                    case 270:
                        orientation = TIFF_ORIENTATION_90CW;
                        break;
                    default:
                        //
                        // We got a value we don't know what to do with.
                        //
                        return;
                }
                value = new UnsignedShortMetaValue( orientation );
                break;
            }
            case CIFF_SI_AUTO_ROTATE: {
                int orientation = value.getIntValue();
                switch ( orientation ) {
                    case CIFF_AUTO_ROTATE_NONE:
                        orientation = TIFF_ORIENTATION_LANDSCAPE;
                        break;
                    case CIFF_AUTO_ROTATE_180:
                        orientation = TIFF_ORIENTATION_180;
                        break;
                    case CIFF_AUTO_ROTATE_90CCW:
                        orientation = TIFF_ORIENTATION_90CCW;
                        break;
                    case CIFF_AUTO_ROTATE_90CW:
                        orientation = TIFF_ORIENTATION_90CW;
                        break;
                    default:
                        //
                        // We got a value we don't know what to do with.
                        //
                        return;
                }
                value = new UnsignedShortMetaValue( orientation );
                break;
            }
            case CIFF_SI_FNUMBER: {
                final int apex = value.getIntValue();
                value = new UnsignedRationalMetaValue(
                    (int)(MetadataUtil.convertFStopFromAPEX( apex ) * 10), 10
                );
                break;
            }
            case CIFF_SI_SHUTTER_SPEED: {
                final float speed = m_ciffDir.getShutterSpeed();
                value = new RationalMetaValue( (int)(speed * 1000), 1000 );
                break;
            }

            default:
                setOwner = false;
        }

        toDir.putValue( toTagID, value, setOwner );
    }

    /**
     * The newly created {@link ImageMetadata} containing the values adapted
     * from the {@link CIFFDirectory}.
     */
    private final ImageMetadata m_metadata;

    private final CIFFDirectory m_ciffDir;
    private final ImageMetadataDirectory m_exifDir;
    private final ImageMetadataDirectory m_tiffDir;
}
/* vim:set et sw=4 ts=4: */
