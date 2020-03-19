/* Copyright (C) 2023- Masahiro Kitagawa */

package com.lightcrafts.image.types;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import org.jetbrains.annotations.NotNull;

import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * A <code>CR3ImageType</code> is-a {@link RawImageType} for CR3 (Canon Raw
 * version 3) images.
 */
public final class CR3ImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>CR3ImageType</code>. */
    public static final CR3ImageType INSTANCE = new CR3ImageType();

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
        return "CR3";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RenderedImage getPreviewImage(@NotNull ImageInfo imageInfo, int maxWidth, int maxHeight)
            throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final LCByteBuffer buf = imageInfo.getByteBuffer();
        final var reader = new CR3MetadataReader(imageInfo);
        final CR3MetadataReader.ImageParam params = reader.getPrvwParam();

        final RenderedImage image = JPEGImageType.getImageFromBuffer(
                buf, params.offset(), params.length(), null, maxWidth, maxHeight);
        return (image != null)
                ? image
                : super.getPreviewImage( imageInfo, maxWidth, maxHeight );
    }

    public boolean hasFastPreview() {
        return true;
    }

    static final boolean USE_EMBEDDED_PREVIEW = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public RenderedImage getThumbnailImage(@NotNull ImageInfo imageInfo)
            throws BadImageFileException, IOException, UnknownImageTypeException
    {
        if (!USE_EMBEDDED_PREVIEW)
            return getPreviewImage(imageInfo, 640, 480);

        final LCByteBuffer buf = imageInfo.getByteBuffer();
        final var reader = new CR3MetadataReader(imageInfo);
        final CR3MetadataReader.ImageParam params = reader.getThmbParam();

        final RenderedImage image = (params != null)
                ? JPEGImageType.getImageFromBuffer(buf, params.offset(), params.length(), null, 160, 120)
                : null;
        return (image != null)
                ? image
                : super.getPreviewImage(imageInfo, 160, 120);
    }


    /**
     * Reads all the metadata for a given CR3 image file.
     *
     * @param imageInfo The image to read the metadata from.
     */
    @Override
    public void readMetadata(@NotNull ImageInfo imageInfo)
        throws BadImageFileException, IOException
    {
        final var reader = new CR3MetadataReader(imageInfo);
        final ImageMetadata metadata = reader.readMetadata();
        MetadataUtil.removePreviewMetadataFrom(metadata);
        MetadataUtil.removeWidthHeightFrom(metadata);
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>CR3ImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private CR3ImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for CR3 files.  All must be lowercase
     * and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "cr3"
    };

}
/* vim:set et sw=4 ts=4: */
