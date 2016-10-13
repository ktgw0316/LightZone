/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.utils.Rational;
import com.lightcrafts.utils.TextUtil;

import static com.lightcrafts.image.metadata.EXIFTags.*;
import static com.lightcrafts.image.metadata.TIFFTags.*;

/**
 * <code>MetadataUtil</code> contains various utilities for image metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MetadataUtil {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Constant used by {@link #convertFStopFromAPEX(int)}.
     */
    public static final double HALF_LOG_2 = Math.log( 2 ) / 2.0;

    /**
     * The natural logarithm of 2; used by various value-conversion functions.
     */
    public static final double LN_2 = Math.log( 2 );

    /**
     * Convert a value expressed in APEX (Additive System of Photographic
     * Exposure) units to one expressed in EV (exposure value) units.
     *
     * @param apex The APEX value to convert.
     * @return Returns the value expressed in EV units.
     */
    public static double convertAPEXToEV( int apex ) {

        // Temporarily make the number positive.
        final int sign;
        if ( apex < 0 ) {
            apex = -apex;
            sign = -1;
        } else
            sign = 1;

        // The fractional part is stored in the lower 5 bits.
        final int frac = apex & 0x1F;
        apex &= ~0x1FL;

        // Need to special-case 1/3 and 2/3.
        final double dFrac;
        switch ( frac ) {
            case 0x0C: // code for 1/3
                dFrac = 32.0 / 3;
                break;
            case 0x14: // code for 2/3
                dFrac = 64.0 / 3;
                break;
            default:
                dFrac = frac;
        }

        return sign * (apex + dFrac) / 32;
    }

    /**
     * Convert a bias value expressed in APEX (Additive System of Photographic
     * Exposure) units to a printable string.
     *
     * @param apex The APEX value to convert.
     * @return Returns the string representation of the bias value.
     */
    public static String convertBiasFromAPEX( double apex ) {
        if ( apex == 0 )
            return "0";
        apex *= 1.00001;                    // avoid round-off errors
        if ( (int)apex / apex > 0.999 )
            return "+" + (int)apex;
        if ( (int)(apex*2) / (apex*2) > 0.999 )
            return '+' + (int)(apex * 2) + "/2";
        if ( (int)(apex*3) / (apex*3) > 0.999 )
            return '+' + (int)(apex * 3) + "/3";
        return TextUtil.tenths( apex );
    }

    /**
     * Convert an F-Stop number expressed in APEX (Additive System of
     * Photographic Exposure) units to an ordinary floating-point number, e.g.,
     * 3.5.
     *
     * @param apex The APEX value to convert.
     * @return Returns the F-Stop as a floating-point value.
     */
    public static double convertFStopFromAPEX( int apex ) {
        final double n = Math.exp( convertAPEXToEV( apex ) * HALF_LOG_2 );
        return fixFStop( n );
        //return (int)(n * 10) / 10.0;
    }

    /**
     * Convert an ISO value expressed in APEX (Additive System of Photographic
     * Exposure) units to an integer.
     *
     * @param apex The APEX value to convert.
     * @return Returns the ISO as an integer.
     */
    public static int convertISOFromAPEX( int apex ) {
        final double n = Math.exp( convertAPEXToEV( apex ) * LN_2 ) * 100 / 32;
        return (int)(n + 0.5);
    }

    /**
     * Convert a shutter speed value expressed in APEX (Additive System of
     * Photographic Exposure) units to a {@link Rational} number, e.g., 1/60.
     *
     * @param apex The APEX value to convert.
     * @return Returns the shutter speed as a {@link Rational} value.
     */
    public static Rational convertShutterSpeedFromAPEX( int apex ) {
        final double n = Math.exp( - convertAPEXToEV( apex ) * LN_2 );
        return n > 0 && n < 1 ?
            new Rational( 1, (int)fixShutterSpeed( 1/n ) ) :
            //new Rational( 1, (int)(1/n + 0.5) ) :
            new Rational( (int)(n * 10), 10 );
    }

    /**
     * Convert a value expressed in EV (exposure value) units to one expressed
     * in APEX (Additive System of Photographic Exposure) units.
     *
     * @param ev The EV value to convert.
     * @return Returns the value expressed in APEX units.
     */
    public static int convertEVToAPEX( float ev ) {
        final int sign;
        if ( ev < 0 ) {                 // temporarily make the number positive
            ev = -ev;
            sign = -1;
        } else
            sign = 1;
        final int val = (int)ev;
        double frac = ev - val;
        if ( Math.abs( frac - 0.33 ) < 0.05 )
            frac = 0x0C;
        else if ( Math.abs( frac - 0.67 ) < 0.05 )
            frac = 0x14;
        else
            frac = (int)(frac * 32 + 0.5);
        return (int)(sign * (val * 32 + frac));
    }

    /**
     * Fixes a given F-Stop value.  F-Stops have discreet &quot;legal&quot;
     * values.  The returned F-Stop value is one of the legal values that is
     * closest to the given value.
     *
     * @param fStop The F-Stop value to be fixed.
     * @return Returns the fixed F-Stop value to the nearest tenth.
     */
    public static float fixFStop( double fStop ) {
        return fixValue( fStop, FSTOPS );
    }

    /**
     * Fixes a given shutter speed value.  Shutter speeds have discreet
     * &quot;legal&quot; values.  The returned shutter speed value is one of
     * the legal values that is closest to the given value.
     *
     * @param speed The shutter speed value to be fixed.  It is assumed to be
     * the reciprocal of the actual value, e.g., 25 means 1/25 second.
     * @return Returns the fixed shutter speed value to the nearest tenth.
     */
    public static float fixShutterSpeed( double speed ) {
        return fixValue( speed, SHUTTER_SPEED );
    }

    /**
     * Checks whether the metadata in the given {@link ImageMetadataDirectory}
     * is for the full-sized image by checking for the
     * <code>NEW_SUBFILE_TYPE</code> and </code>SUBFILE_TYPE</code> metadata
     * tags and the relevant values.
     *
     * @param dir The {@link ImageMetadataDirectory} to check.
     * @return Returns <code>true</code> only if the metadata in the given
     * {@link ImageMetadataDirectory} is for the full-sized image.
     */
    public static boolean isFullSizedImage( ImageMetadataDirectory dir ) {
        ImageMetaValue value = dir.getValue( TIFF_NEW_SUBFILE_TYPE );
        if ( value != null && value.getIntValue() == 0 /* full-size */ )
            return true;
        value = dir.getValue( TIFF_SUBFILE_TYPE );
        return value != null && value.getIntValue() == 1 /* full size */;
    }

    /**
     * Gets the maximum value of the metadata having the given tags.
     *
     * @param dir The {@link ImageMetadataDirectory} to get the values from.
     * @param tagID1 The ID of the first tag.
     * @param tagID2 The ID of the second tag.
     * @return Returns said maximum or 0 if the metadata doesn't have a value
     * for either tag.
     */
    public static int maxTagValue( ImageMetadataDirectory dir, int tagID1,
                                   int tagID2 ) {
        final ImageMetaValue v1 = dir.getValue( tagID1 );
        final ImageMetaValue v2 = dir.getValue( tagID2 );
        if ( v1 == null )
            return v2 != null ? v2.getIntValue() : 0;
        if ( v2 == null )
            return v1.getIntValue();
        return Math.max( v1.getIntValue(), v2.getIntValue() );
    }

    /**
     * Remove all metadata that can only be for the preview image.
     *
     * @param metadata the {@link ImageMetadata} to remove the preview metadata
     * from.
     * @see #removeWidthHeightFrom(ImageMetadata)
     */
    public static void removePreviewMetadataFrom( ImageMetadata metadata ) {
        final ImageMetadataDirectory exifDir =
            metadata.getDirectoryFor( EXIFDirectory.class );
        if ( exifDir != null ) {
            exifDir.removeValue( EXIF_BITS_PER_SAMPLE );
            exifDir.removeValue( EXIF_COMPRESSED_BITS_PER_PIXEL );
            exifDir.removeValue( EXIF_COMPRESSION );
            exifDir.removeValue( EXIF_NEW_SUBFILE_TYPE );
            exifDir.removeValue( EXIF_PIXEL_X_DIMENSION );
            exifDir.removeValue( EXIF_PIXEL_Y_DIMENSION );
            exifDir.removeValue( EXIF_RESOLUTION_UNIT );
            exifDir.removeValue( EXIF_ROWS_PER_STRIP );
            exifDir.removeValue( EXIF_SAMPLES_PER_PIXEL );
            exifDir.removeValue( EXIF_SUBFILE_TYPE );
            exifDir.removeValue( EXIF_X_RESOLUTION );
            exifDir.removeValue( EXIF_Y_RESOLUTION );
        }

        final ImageMetadataDirectory tiffDir =
            metadata.getDirectoryFor( TIFFDirectory.class );
        if ( tiffDir != null ) {
            tiffDir.removeValue( TIFF_BITS_PER_SAMPLE );
            tiffDir.removeValue( TIFF_COMPRESSION );
            tiffDir.removeValue( TIFF_NEW_SUBFILE_TYPE );
            tiffDir.removeValue( TIFF_PHOTOMETRIC_INTERPRETATION );
            tiffDir.removeValue( TIFF_PLANAR_CONFIGURATION );
            tiffDir.removeValue( TIFF_RESOLUTION_UNIT );
            tiffDir.removeValue( TIFF_ROWS_PER_STRIP );
            tiffDir.removeValue( TIFF_SAMPLES_PER_PIXEL );
            tiffDir.removeValue( TIFF_SUBFILE_TYPE );
            tiffDir.removeValue( TIFF_X_RESOLUTION );
            tiffDir.removeValue( TIFF_Y_RESOLUTION );
        }
    }

    /**
     * Remove all width and height metadata (because it's not for the
     * full-sized image).
     *
     * @param metadata the {@link ImageMetadata} to remove the image width and
     * height metadata from.
     * @see #removePreviewMetadataFrom(ImageMetadata).
     */
    public static void removeWidthHeightFrom( ImageMetadata metadata ) {
        final ImageMetadataDirectory exifDir =
            metadata.getDirectoryFor( EXIFDirectory.class );
        if ( exifDir != null ) {
            exifDir.removeValue( EXIF_IMAGE_WIDTH );
            exifDir.removeValue( EXIF_IMAGE_HEIGHT );
            exifDir.removeValue( EXIF_PIXEL_X_DIMENSION );
            exifDir.removeValue( EXIF_PIXEL_Y_DIMENSION );
        }

        final ImageMetadataDirectory tiffDir =
            metadata.getDirectoryFor( TIFFDirectory.class );
        if ( tiffDir != null ) {
            tiffDir.removeValue( TIFF_IMAGE_WIDTH );
            tiffDir.removeValue( TIFF_IMAGE_LENGTH );
        }
    }

    /**
     * Gets the printable string value for a shutter speed.
     *
     * @param speed The shutter speed value.
     * @return Returns said string.
     */
    public static String shutterSpeedString( double speed ) {
        if ( speed < 1 )
            return "1/" + (int)fixShutterSpeed( 1/speed );
        return TextUtil.tenths( speed );
    }

    /**
     * Some camera manufacturers duplicate the "make" at the beginning of the
     * "model" metadata field so that, when concatenated, you get something
     * like "Canon Canon EOS 10D".  This is dumb, so this method takes the
     * make and model strings and returns a single make/model string without a
     * duplicate make.
     *
     * @param make The make the camera.
     * @param model The model of the camera.
     * @return Returns the make and model with a duplicate make removed (if it
     * was duplicated) or the make and model concatenated with a space (if not
     * duplicated).
     */
    public static String undupMakeModel( String make, String model ) {
        make = make.trim();
        model = model.trim();
        final String MAKE  = make.toUpperCase();
        final String MODEL = model.toUpperCase();
        if ( MODEL.contains( MAKE ) ) {
            //
            // Case 1: The model contains the make, e.g., "Canon EOS 10D"
            // contains "Canon", so just return the model.
            //
            return model;
        }

        final int spacePos = MODEL.indexOf( ' ' );
        if ( spacePos > 1 )
            if ( MAKE.contains( MODEL.substring( 0, spacePos ) ) ) {
                //
                // Case 2: The make contains the first word of the model.  This
                // case is needed for at least Nikon because their make has the
                // word "Corporation" in it, e.g., "Nikon Corporation", so this
                // kind of make won't be in the model.
                //
                // However, if the make contains the first word of the model,
                // e.g., "Nikon Coproration" contains "Nikon" (the first word
                // of "Nikon D2X"), then just return the model.
                //
                return model;
            }

        //
        // If we get here, assume the make isn't duplicated.
        //
        return make + ' ' + model;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Fix a value using the legal values in the given table.  The returned
     * value is one of the legal values that is closest to the given value.
     *
     * @param value The value to fix.
     * @param table The table of legal values to use.
     * @return Returns the fixed value.
     */
    private static float fixValue( double value, float[] table ) {
        value = (int)(value * 10 + 0.5) / 10.0;
        int min = 0;
        int max = table.length - 1;
        while ( min < max ) {
            final int i = (min + max) / 2;
            final double valueAtI = table[i];
            if ( value == valueAtI )
                return (float)value;
            if ( value < valueAtI )
                max = i - 1;
            else
                min = i + 1;
        }

        double smallestDeltaSoFar = 9999;
        int bestI = 0;
        for ( int i = max - 1; i <= min + 1; ++i )
            if ( i >= 0 && i < table.length ) {
                final double delta = Math.abs( table[i] - value );
                if ( delta < smallestDeltaSoFar ) {
                    smallestDeltaSoFar = delta;
                    bestI = i;
                }
            }
        return table[ bestI ];
    }

    private static final float[] FSTOPS = {
          0.0F, // if the value is unknown or brighter than f/1.
          1.0F,
          1.1F,
          1.2F,
          1.3F,
          1.4F,
          1.6F,
          1.8F,
          2.0F,
          2.2F,
          2.5F,
          2.8F,
          3.2F,
          3.5F, // There exists a Nikon 28mm @ f/3.5.
          3.6F,
          4.0F,
          4.5F,
          5.0F,
          5.7F,
          6.3F,
          7.1F,
          8.0F,
          9.0F,
         10.1F,
         11.0F, // Really 11.3, but commonly called f/11.
         12.7F,
         14.3F,
         16.0F,
         18.0F,
         20.2F,
         22.0F, // Really 22.6, but commonly called f/22.
         25.4F,
         28.5F,
         32.0F,
         45.0F,
         64.0F,
         90.0F,
        125.0F,
        180.0F
    };

    /**
     * These values are the reciprocal of the actual value, e.g., 25 means
     * 1/25 second.
     */
    private static final float[] SHUTTER_SPEED = {
           0.0F, // if the value is unknown or longer than 1 second.
           1.0F,
           2.0F,
           2.5F,
           3.2F,
           4.0F,
           5.0F,
           6.4F,
           8.0F,
          10.0F,
          12.0F,
          15.0F,
          20.0F,
          25.0F,
          30.0F,
          40.0F,
          50.0F,
          60.0F,
          80.0F,
         100.0F,
         125.0F,
         160.0F,
         200.0F,
         250.0F,
         320.0F,
         400.0F,
         500.0F,
         640.0F,
         800.0F,
        1000.0F,
        1250.0F,
        1600.0F,
        2000.0F
    };
}
/* vim:set et sw=4 ts=4: */
