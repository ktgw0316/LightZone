/* Copyright (C) 2023- Masahiro Kitagawa */

package com.lightcrafts.image.types;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

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
        final ImageParams params = parsePrvwBox(buf);

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
        final ImageParams params = parseThmbBox(buf);

        final RenderedImage image = (params != null)
                ? JPEGImageType.getImageFromBuffer(buf, params.offset, params.length, null, 160, 120)
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
        final LCByteBuffer buf = imageInfo.getByteBuffer();
        final int cmt1BoxPos = parseCmt1Box(buf);
        final var reader = new TIFFMetadataReader(
                imageInfo, buf.initialOffset(cmt1BoxPos + headerSize));
        final ImageMetadata metadata = reader.readMetadata();
        MetadataUtil.removePreviewMetadataFrom(metadata);
        MetadataUtil.removeWidthHeightFrom(metadata);
        buf.initialOffset(0); // TODO: Is this necessary?
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

    // constants for the CR3's ISO/IEC 14496-12 file format

    private static final byte[] moovTag = "moov".getBytes(UTF_8);
    private static final byte[] uuidTag = "uuid".getBytes(UTF_8);
    private static final byte[] ctboTag = "CTBO".getBytes(UTF_8);
    private static final byte[] prvwTag = "PRVW".getBytes(UTF_8);
    private static final byte[] thmbTag = "THMB".getBytes(UTF_8);
    private static final byte[] cmt1Tag = "CMT1".getBytes(UTF_8);
    private static final int headerSize = 8;
    private static final int extendedTypeSize = 16;
    private static final byte[] prvwUuid = new byte[] {
            (byte) 0xea, (byte) 0xf4, 0x2b, 0x5e,
            0x1c, (byte) 0x98, 0x4b, (byte) 0x88,
            (byte) 0xb9, (byte) 0xfb, (byte) 0xb7, (byte) 0xdc,
            0x40, 0x6e, 0x4d, 0x16};

    /**
     * get a box size of the ISO/IEC 14496-12 file format
     */
    private static long getBoxSize(LCByteBuffer buf, int boxPos)
            throws IOException {
        final int boxSize = buf.getInt(boxPos);
        return switch (boxSize) {
            case 0 -> buf.limit() - boxPos; // box extends to end of file
            case 1 -> buf.getLong(boxPos + headerSize);
            default -> boxSize;
        };
    }

    private record ImageParams(int offset, int length) { }

    @NotNull
    private static CR3ImageType.ImageParams parsePrvwBox(@NotNull LCByteBuffer buf) throws IOException {
        final ByteOrder origOrder = buf.order();
        buf.order(ByteOrder.BIG_ENDIAN);

        int prvwBoxPos = -1;
        for (int boxPos = 0; boxPos < buf.limit();) {
            final var boxType = buf.getBytes(boxPos + 4, 4);
            if (Arrays.equals(boxType, prvwTag)) {
                prvwBoxPos = boxPos;
                break;
            } else if (Arrays.equals(boxType, moovTag)) {
                boxPos += headerSize;
            } else if (Arrays.equals(boxType, ctboTag)) {
                // cf. https://github.com/lclevy/canon_cr3#ctbo
                final int recodeSize = 20;
                final long prvwUuidOffset = buf.getLong(boxPos + headerSize + 4 + recodeSize + 4);
                boxPos = (int) prvwUuidOffset;
            } else if (Arrays.equals(boxType, uuidTag)) {
                byte[] uuid = buf.getBytes(boxPos + headerSize, extendedTypeSize);
                boxPos += headerSize + extendedTypeSize;
                if (Arrays.equals(uuid, prvwUuid)) {
                    boxPos += 8; // to skip unknown data
                }
            } else {
                // Just skip other boxes.
                boxPos += getBoxSize(buf, boxPos);
            }
        }

        // cf. https://github.com/lclevy/canon_cr3#prvw-preview
        final int length = buf.getInt(prvwBoxPos + 20);
        final int offset = prvwBoxPos + 24;

        buf.order( origOrder );
        return new ImageParams(offset, length);
    }

    @Nullable
    private static ImageParams parseThmbBox(LCByteBuffer buf) throws IOException {
        final ByteOrder origOrder = buf.order();
        buf.order(ByteOrder.BIG_ENDIAN);

        int thmbBoxPos = -1;
        for (int boxPos = 0; boxPos < buf.limit();) {
            final var boxType = buf.getBytes(boxPos + 4, 4);
            if (Arrays.equals(boxType, thmbTag)) {
                thmbBoxPos = boxPos;
                break;
            } else if (Arrays.equals(boxType, moovTag)) {
                boxPos += headerSize;
            } else if (Arrays.equals(boxType, uuidTag)) {
                boxPos += headerSize + extendedTypeSize;
            } else {
                // Just skip other boxes.
                boxPos += getBoxSize(buf, boxPos);
            }
        }

        // cf. https://github.com/lclevy/canon_cr3#thmb-thumbnail
        final byte version = buf.get(thmbBoxPos + 8);
        final int length = buf.getInt(thmbBoxPos + 16);
        final int offset = thmbBoxPos + 24;

        buf.order(origOrder);
        if (thmbBoxPos < 0 || (version != 0 && version != 1))
            return null;
        return new ImageParams(offset, length);
    }

    private static int parseCmt1Box(LCByteBuffer buf) throws IOException {
        final ByteOrder origOrder = buf.order();
        buf.order(ByteOrder.BIG_ENDIAN);

        int cmt1BoxPos = -1;
        for (int boxPos = 0; boxPos < buf.limit();) {
            final var boxType = buf.getBytes(boxPos + 4, 4);
            if (Arrays.equals(boxType, cmt1Tag)) {
                cmt1BoxPos = boxPos;
                break;
            } else if (Arrays.equals(boxType, moovTag)) {
                boxPos += headerSize;
            } else if (Arrays.equals(boxType, uuidTag)) {
                boxPos += headerSize + extendedTypeSize;
            } else {
                // Just skip other boxes.
                boxPos += getBoxSize(buf, boxPos);
            }
        }

        buf.order(origOrder);
        return cmt1BoxPos;
    }

}
/* vim:set et sw=4 ts=4: */
