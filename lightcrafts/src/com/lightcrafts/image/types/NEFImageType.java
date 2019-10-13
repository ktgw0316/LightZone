/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.image.RenderedImage;
import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.makernotes.NikonDirectory;
import com.lightcrafts.image.metadata.values.LongMetaValue;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import static com.lightcrafts.image.metadata.EXIFTags.EXIF_JPEG_INTERCHANGE_FORMAT;
import static com.lightcrafts.image.metadata.EXIFTags.EXIF_JPEG_INTERCHANGE_FORMAT_LENGTH;
import static com.lightcrafts.image.metadata.makernotes.NikonTags.*;

/**
 * A <code>NEFImageType</code> is-an {@link RawImageType} for NEF (Nikon raw
 * Format) images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class NEFImageType extends RawImageType implements TagHandler {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>NEFImageType</code>. */
    public static final NEFImageType INSTANCE = new NEFImageType();

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
        return "NEF";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final RenderedImage image = TIFFImageType.getJPEGInterchangeImage(
            imageInfo, maxWidth, maxHeight
        );
        return  image != null &&
                // The following is a hack to support NRW files
                image.getWidth() > 300 &&
                image.getHeight() > 300 ?
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
        final ImageMetadata metadata = imageInfo.getMetadata();
        final ImageMetadataDirectory dir =
            metadata.getDirectoryFor( NikonDirectory.class );
        if ( dir != null ) {
            // TODO: verify that this is actually an sRGB image, what about Adobe RGB shooting, etc.?
            return JPEGImageType.getImageFromBuffer(
                imageInfo.getByteBuffer(),
                dir.getValue( NIKON_PREVIEW_IMAGE_OFFSET ), 0,
                dir.getValue( NIKON_PREVIEW_IMAGE_LENGTH ),
                0, 0
            );
        }
        return super.getThumbnailImage( imageInfo );
    }

    /**
     * {@inheritDoc}
     */
    public boolean handleTag( int tagID, int fieldType, int numValues,
                              int byteCount, int valueOffset,
                              int valueOffsetAdjustment, int subdirOffset,
                              ImageInfo imageInfo, LCByteBuffer buf,
                              ImageMetadataDirectory dir )
        throws IOException
    {
        if (!(dir instanceof NikonDirectory)) {
            return false;
        }
        switch ( tagID ) {
            case NIKON_PREVIEW_IMAGE_IFD_POINTER: {
                //
                // The NikonPreviewImage tag's value is an offset to a subEXIF
                // directory containing 7 metadata tags/values for the preview
                // image.  The 7 (in order) are: Compression, XResolution,
                // YResolution, ResolutionUnit, JPEGInterchangeFormat,
                // JPEGInterchangeFormatLength, and YCbCrPositioning.  Of
                // those, we only care about JPEGInterchangeFormat and
                // JPEGInterchangeFormatLength, i.e., the starting offset and
                // length of the preview JPEG image.
                //
                final EXIFMetadataReader reader = new EXIFMetadataReader(
                    imageInfo, buf, true
                );
                //
                // Create temporary ImageMetadata and EXIFDirectory objects to
                // read the metadata into because we don't want to overwrite
                // the values of the tags we're not interested in in the full-
                // sized image (if they exist).
                //
                final ImageMetadata tempMetadata = new ImageMetadata();
                final ImageMetadataDirectory tempDir =
                    tempMetadata.getDirectoryFor( EXIFDirectory.class, true );
                reader.readDirectory( subdirOffset, 0, tempDir );
                //
                // Now pluck out the tags' values we're interested in and
                // insert them (and only them) into the Nikon directory.
                //
                final LongMetaValue offsetValue =
                    (LongMetaValue)tempDir.getValue(
                        EXIF_JPEG_INTERCHANGE_FORMAT
                    );
                final LongMetaValue lengthValue =
                    (LongMetaValue)tempDir.getValue(
                        EXIF_JPEG_INTERCHANGE_FORMAT_LENGTH
                    );
                if ( offsetValue == null || lengthValue == null )
                    return true;
                final int offset = offsetValue.getIntValue();
                final int length = lengthValue.getIntValue();
                if ( offset <= 0 || length <= 0 )
                    return true;
                final ImageMetadataDirectory nikonDir =
                    imageInfo.getCurrentMetadata().getDirectoryFor(
                        NikonDirectory.class, true
                    );
                offsetValue.setLongValueAt( offset + valueOffsetAdjustment, 0 );
                nikonDir.putValue( NIKON_PREVIEW_IMAGE_OFFSET, offsetValue );
                nikonDir.putValue( NIKON_PREVIEW_IMAGE_LENGTH, lengthValue );
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        final TIFFMetadataReader reader = new TIFFMetadataReader( imageInfo );
        reader.setTagHandler( this );
        MetadataUtil.removePreviewMetadataFrom( reader.readMetadata() );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>NEFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private NEFImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for NEF files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "nef", "nrw"
    };
}
/* vim:set et sw=4 ts=4: */
