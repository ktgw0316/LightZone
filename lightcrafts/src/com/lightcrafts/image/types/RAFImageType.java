/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.ByteOrder;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import static com.lightcrafts.image.metadata.TIFFTags.*;

/**
 * A <code>RAFImageType</code> is-a {@link RawImageType} for RAF (Fuji raw)
 * images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class RAFImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>RAFImageType</code>. */
    public static final RAFImageType INSTANCE = new RAFImageType();

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
        return "RAF";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException
    {
        final LCByteBuffer buf = imageInfo.getByteBuffer();
        //
        // The pointer to where the JPEG starts is 84 bytes in.  The length of
        // the JPEG is 4 bytes after that.  Both are always in big-endian.
        //
        final ByteOrder origOrder = buf.order();
        buf.order( ByteOrder.BIG_ENDIAN );
        final int offset = buf.getInt( 84 );
        final int length = buf.getInt( 88 );
        buf.order( origOrder );
        return JPEGImageType.getImageFromBuffer(
            buf, offset, length, null, 0, 0
        );
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
        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor( TIFFDirectory.class );
        if ( dir == null ) {
            //
            // This should never be null, but just in case ...
            //
            return null;
        }
        final RAFImageInfo rafInfo = (RAFImageInfo)imageInfo.getAuxiliaryInfo();
        return JPEGImageType.getImageFromBuffer(
            imageInfo.getByteBuffer(),
            dir.getValue( TIFF_JPEG_INTERCHANGE_FORMAT ),
            rafInfo.getTIFFOffset(),
            dir.getValue( TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH ),
            0, 0
        );
    }

    /**
     * {@inheritDoc}
     */
    public RAFImageInfo newAuxiliaryInfo( ImageInfo imageInfo ) {
        return new RAFImageInfo( imageInfo );
    }

    /**
     * {@inheritDoc}
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final RAFMetadataReader reader = new RAFMetadataReader( imageInfo );
        final ImageMetadata metadata = reader.readMetadata();
        MetadataUtil.removePreviewMetadataFrom( metadata );
        MetadataUtil.removeWidthHeightFrom( metadata );

        final RAFImageInfo info = (RAFImageInfo)imageInfo.getAuxiliaryInfo();
        info.setTIFFOffset( reader.getTIFFOffset() );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct an <code>RAFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private RAFImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for RAF files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "raf"
    };
}
/* vim:set et sw=4 ts=4: */
