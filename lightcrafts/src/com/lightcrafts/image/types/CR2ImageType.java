/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.ByteOrder;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import static com.lightcrafts.image.metadata.TIFFTags.*;

/**
 * A <code>CR2ImageType</code> is-a {@link RawImageType} for CR2 (Canon Raw
 * version 2) images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class CR2ImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>CR2ImageType</code>. */
    public static final CR2ImageType INSTANCE = new CR2ImageType();

    /**
     * {@inheritDoc}
     */
    public String[] getExtensions() {
        return EXTENSIONS;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "CR2";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final LCByteBuffer buf = imageInfo.getByteBuffer();
        //
        // The pointer to where the JPEG starts (tag 0x0111) is 98 bytes in.
        // The length of the JPEG (tag 0x0117) is 24 bytes after that.
        // Both are always in little-endian.
        //
        final ByteOrder origOrder = buf.order();
        buf.order( ByteOrder.LITTLE_ENDIAN );
        final int offset = (int) buf.getLong( 98 );
        final int length = (int) buf.getLong( 122 );
        buf.order( origOrder );

        final RenderedImage image = JPEGImageType.getImageFromBuffer(
            buf, offset, length, null, maxWidth, maxHeight
        );
        return ( image != null ) ?
            image : super.getPreviewImage( imageInfo, maxWidth, maxHeight );
    }

    public boolean hasFastPreview() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        if (!USE_EMBEDDED_PREVIEW)
            return getPreviewImage( imageInfo, 640, 480 );

        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor( TIFFDirectory.class );
        if ( dir == null ) {
            //
            // This should never be null, but just in case ...
            //
            return null;
        }
        //
        // Get a small jpeg image from IFD #1
        //
        return JPEGImageType.getImageFromBuffer(
            imageInfo.getByteBuffer(),
            dir.getValue( TIFF_JPEG_INTERCHANGE_FORMAT ), 0,
            dir.getValue( TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH ),
            160, 120
        );
    }

    /**
     * {@inheritDoc}
     */
    /* public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor( TIFFDirectory.class );
        if ( dir == null ) {
            //
            // This should never be null, but just in case ...
            //
            return null;
        }
        return ((ThumbnailImageProvider)dir).getThumbnailImage( imageInfo );
    } */

    /**
     * Reads all the metadata for a given CR2 image file.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        final TIFFMetadataReader reader = new TIFFMetadataReader( imageInfo );
        final ImageMetadata metadata = reader.readMetadata();
        MetadataUtil.removePreviewMetadataFrom( metadata );
        MetadataUtil.removeWidthHeightFrom( metadata );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>CR2ImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private CR2ImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for CR2 files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String EXTENSIONS[] = {
        "cr2"
    };
}
/* vim:set et sw=4 ts=4: */
