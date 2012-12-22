/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.IOException;
import java.util.Date;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.image.types.RawImageInfo;
import com.lightcrafts.utils.DCRaw;
import com.lightcrafts.utils.LightCraftsException;

import static com.lightcrafts.image.metadata.EXIFTags.*;

/**
 * An <code>DCRawMetadataReader</code> is-an {@link ImageMetadataReader} for
 * reading the metadata provided by dcraw.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class DCRawMetadataReader extends ImageMetadataReader {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>DCRawMetadataReader</code>.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public DCRawMetadataReader( ImageInfo imageInfo ) {
        super( imageInfo, null );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Read all metadata that, for this class, does nothing.
     */
    protected void readAllDirectories() throws IOException {
        final RawImageInfo info;
        try {
            info = (RawImageInfo)m_imageInfo.getAuxiliaryInfo();
        }
        catch ( LightCraftsException e ) {
            return;
        }
        final DCRaw dcraw = info.getDCRaw();

        final ImageMetadata metadata = m_imageInfo.getCurrentMetadata();
        final EXIFDirectory exifDir =
            (EXIFDirectory)metadata.getDirectoryFor(
                EXIFDirectory.class, true
            );

        final float aperture = dcraw.getAperture();
        if ( aperture > 0 )
            exifDir.putValue(
                EXIF_FNUMBER,
                new UnsignedRationalMetaValue( (int)(aperture * 10), 10 )
            );

        final Date captureDateTime = dcraw.getCaptureDateTime();
        if ( captureDateTime != null )
            exifDir.putValue(
                EXIF_DATE_TIME, new DateMetaValue( captureDateTime )
            );

        final float focalLength = dcraw.getFocalLength();
        if ( focalLength > 0 )
            exifDir.putValue(
                EXIF_FOCAL_LENGTH,
                new UnsignedRationalMetaValue( (int)(focalLength * 10), 10 )
            );

        final int width = dcraw.getImageWidth();
        final int height = dcraw.getImageHeight();
        if ( width > 0 && height > 0 ) {
            exifDir.putValue(
                EXIF_IMAGE_WIDTH, new UnsignedShortMetaValue( width )
            );
            exifDir.putValue(
                EXIF_IMAGE_HEIGHT, new UnsignedShortMetaValue( height )
            );
        }

        final int iso = dcraw.getISO();
        if ( iso > 0 )
            exifDir.putValue(
                EXIF_ISO_SPEED_RATINGS, new UnsignedShortMetaValue( iso )
            );

        final String make = dcraw.getMake();
        if ( make != null )
            exifDir.putValue( EXIF_MAKE, new StringMetaValue( make ) );

        final String model = dcraw.getModel();
        if ( model != null )
            exifDir.putValue( EXIF_MODEL, new StringMetaValue( model ) );

        final float shutterSpeed = dcraw.getShutterSpeed();
        if ( shutterSpeed > 0 )
            exifDir.putValue(
                EXIF_EXPOSURE_TIME,
                new RationalMetaValue( (int)(shutterSpeed * 10000), 10000 )
            );
    }

    /**
     * Read the image header that, for this class, does nothing.
     */
    protected void readHeader() {
        // do nothing
    }

}
/* vim:set et sw=4 ts=4: */
