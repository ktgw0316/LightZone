/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.makernotes.OlympusDirectory;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import java.io.IOException;
import java.util.Map;

import static com.lightcrafts.image.metadata.makernotes.OlympusTags.*;

/**
 * A <code>ORFImageType</code> is-a {@link RawImageType} for ORF (Olympus Raw
 * Format) images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ORFImageType extends RawImageType implements TagHandler {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>ORFImageType</code>. */
    public static final ORFImageType INSTANCE = new ORFImageType();

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getExtensions() {
        return EXTENSIONS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "ORF";
    }

    /**
     * {@inheritDoc}
     */
    /*
    @Override
    public RenderedImage getPreviewImage(ImageInfo imageInfo, int maxWidth,
                                         int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor(OlympusDirectory.class);

        // This should never be null, but just in case ...
        if ( dir == null ) {
            return null;
        }

        RenderedImage image = JPEGImageType.getImageFromBuffer(
                imageInfo.getByteBuffer(),
                dir.getValue(OLYMPUS_PREVIEW_IMAGE_START), 0,
                dir.getValue(OLYMPUS_PREVIEW_IMAGE_LENGTH),
                maxWidth, maxHeight
        );
        if (image != null) {
            return image;
        }

        ImageMetaValue valid = dir.getValue(OLYMPUS_PREVIEW_IMAGE_VALID);
        if (valid != null && valid.getIntValue() == 1) {
            image = JPEGImageType.getImageFromBuffer(
                    imageInfo.getByteBuffer(),
                    dir.getValue(OLYMPUS_PREVIEW_IMAGE_START_2), 0,
                    dir.getValue(OLYMPUS_PREVIEW_IMAGE_LENGTH_2),
                    maxWidth, maxHeight
            );
            if (image != null) {
                return image;
            }
        }

        valid = dir.getValue(OLYMPUS_CS_PREVIEW_IMAGE_VALID);
        if (valid != null && valid.getIntValue() == 1) {
            image = JPEGImageType.getImageFromBuffer(
                    imageInfo.getByteBuffer(),
                    dir.getValue(OLYMPUS_CS_PREVIEW_IMAGE_START), 0,
                    dir.getValue(OLYMPUS_CS_PREVIEW_IMAGE_LENGTH),
                    maxWidth, maxHeight
            );
            if (image != null) {
                return image;
            }
        }

        return super.getPreviewImage(imageInfo, maxWidth, maxHeight);
    }
    */

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handleTag( int tagID, int fieldType, int numValues,
                              int byteCount, int valueOffset,
                              int valueOffsetAdjustment, int subdirOffset,
                              ImageInfo imageInfo, LCByteBuffer buf,
                              ImageMetadataDirectory dir )
            throws IOException
    {
        if (!(dir instanceof OlympusDirectory)) {
            return false;
        }
        switch (tagID) {
            case OLYMPUS_CAMERA_SETTINGS:
            case OLYMPUS_CAMERA_TYPE:
            case OLYMPUS_EQUIPMENT:
            case OLYMPUS_MINOLTA_CAMERA_SETTINGS:
                //
                // Read the metadata into a separate directory in a separate
                // ImageMetadata object, then copy the metadata into the main
                // directory adjusting the tagIDs to avoid conflict.
                //
                final ImageMetadata tempMetadata = new ImageMetadata();
                final ImageMetadataDirectory tempDir =
                        tempMetadata.getDirectoryFor(EXIFDirectory.class, true);
                new EXIFMetadataReader(imageInfo, buf, true).readDirectory(
                        subdirOffset, valueOffsetAdjustment, tempDir
                );
                tagID <<= 16;
                for (Map.Entry<Integer,ImageMetaValue> kv : tempDir) {
                    dir.putValue(tagID | kv.getKey(), kv.getValue());
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean hasFastPreview() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        final ImageMetadataReader reader = new TIFFMetadataReader( imageInfo );
        reader.setTagHandler(this);
        MetadataUtil.removePreviewMetadataFrom( reader.readMetadata() );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>ORFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private ORFImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for ORF files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "orf"
    };
}
/* vim:set et sw=4 ts=4: */
