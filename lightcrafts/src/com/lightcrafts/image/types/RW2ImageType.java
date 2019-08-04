/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.makernotes.PanasonicDirectory;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.bytebuffer.LCMappedByteBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

import static com.lightcrafts.image.metadata.PanasonicRawTags.PANASONIC_JPEG_FROM_RAW;

/**
 * A {@code RW2ImageType} is-a {@link RawImageType} for Panasonic raw images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class RW2ImageType extends RawImageType implements TagHandler {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of {@code RW2ImageType}. */
    public static final RW2ImageType INSTANCE = new RW2ImageType();

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
        return "Panasonic";
    }

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
        if (!(dir instanceof PanasonicDirectory)) {
            return false;
        }
        switch ( tagID ) {
            case PANASONIC_JPEG_FROM_RAW: {
                // The embedded JPEG image has lots of metadata including the
                // maker notes -- read it.
                final File file = imageInfo.getFile();
                final LCMappedByteBuffer jpegBuf = new LCMappedByteBuffer(
                        file, valueOffset, byteCount, FileChannel.MapMode.READ_ONLY
                );
                try {
                    final LCByteBuffer exifBuf =
                            new EXIFSegmentFinder(file, valueOffset)
                            .getEXIFSegmentFrom(jpegBuf);
                    if (exifBuf != null) {
                        final EXIFMetadataReader reader =
                                new EXIFMetadataReader(imageInfo, exifBuf, false);
                        reader.readMetadata();
                    }
                } catch (BadImageFileException e) {
                    // e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

    // through dcraw, fast enough...
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
        final ImageMetadataReader reader = new TIFFMetadataReader(imageInfo);
        reader.setTagHandler(this);
        MetadataUtil.removePreviewMetadataFrom(reader.readMetadata());
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct an {@code RW2ImageType}.
     * The constructor is {@code private} so only the singleton instance
     * can be constructed.
     */
    private RW2ImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for Panasonic raw files.  All must
     * be lower case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "rw2"
    };
}
/* vim:set et sw=4 ts=4: */
