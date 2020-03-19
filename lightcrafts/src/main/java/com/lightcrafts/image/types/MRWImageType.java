/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.image.RenderedImage;
import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.MRWMetadataReader;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.metadata.makernotes.MinoltaDirectory;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.utils.bytebuffer.ArrayByteBuffer;

import static com.lightcrafts.image.types.JPEGConstants.*;
import static com.lightcrafts.image.metadata.makernotes.MinoltaTags.*;

/**
 * A <code>MRWImageType</code> is-a {@link RawImageType} for MRW (Minolta RaW)
 * images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MRWImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>MRWImageType</code>. */
    public static final MRWImageType INSTANCE = new MRWImageType();

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
        return "MRW";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        if (!USE_EMBEDDED_PREVIEW)
            return super.getPreviewImage(imageInfo, maxWidth, maxHeight);

        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor( MinoltaDirectory.class );
        if ( dir == null ) {
            //
            // This should never be null, but just in case ...
            //
            return null;
        }
        //
        // We can't just call createWritableRasterFrom() directly using the
        // ImageInfo.getByteBuffer() because Minolta is being annoying by
        // deliberately obfuscating the preview image: they change the first
        // byte of the JPEG from FF to some other value (I've seen 02).
        //
        // We have to redo a lot of what createWritableRasterFrom() does here
        // so we can get a hold of the buffer and change the fist byte back to
        // FF.  Because we have to modify it, it can't be a FileByteBuffer, so
        // we have to copy it into an ArrayByteBuffer.
        //
        final ImageMetaValue offsetValue =
            dir.getValue( MINOLTA_PREVIEW_IMAGE_START );
        final ImageMetaValue lengthValue =
            dir.getValue( MINOLTA_PREVIEW_IMAGE_LENGTH );
        if ( offsetValue == null || lengthValue == null )
            return null;
        final int offset = offsetValue.getIntValue();
        final int length = lengthValue.getIntValue();
        if ( offset <= 0 || length <= 0 )
            return null;
        //
        // To conserve resources, we reuse an ArrayByteBuffer used during the
        // reading of metadata and stored in MRWImageInfo.
        //
        final MRWImageInfo info = (MRWImageInfo)imageInfo.getAuxiliaryInfo();
        final ArrayByteBuffer buf = info.getTIFFBlock();
        //
        // Patch the JPEG preview image.
        //
        if ( buf.get( offset + 1 ) == JPEG_SOI_MARKER &&
             buf.get( offset + 2 ) == JPEG_MARKER_BYTE )
            buf.put( offset, JPEG_MARKER_BYTE );
        //
        // Finally, we can call createWritableRasterFrom(), but on the patched
        // buffer.
        //
        return JPEGImageType.getImageFromBuffer(
            buf, offsetValue, 0, lengthValue, maxWidth, maxHeight
        );
    }

    public boolean hasFastPreview() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public MRWImageInfo newAuxiliaryInfo( ImageInfo imageInfo ) {
        return new MRWImageInfo( imageInfo );
    }

    /**
     * {@inheritDoc}
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final MRWMetadataReader reader = new MRWMetadataReader( imageInfo );
        reader.readMetadata();
        //
        // Store the TIFF ("TTW") block inside the MRWImageInfo so it can be
        // used later in getPreviewImage().
        //
        final MRWImageInfo info = (MRWImageInfo)imageInfo.getAuxiliaryInfo();
        info.setTIFFBlock( reader.getTIFFBlock() );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct an <code>MRWImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private MRWImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for MRW files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "mrw"
    };
}
/* vim:set et sw=4 ts=4: */
