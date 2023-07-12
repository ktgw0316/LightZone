/* Copyright (C) 2023- Masahiro Kitagawa */

package com.lightcrafts.image.types;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.utils.LRUHashMap;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        final ImageParam params = CR3ImageParser.getInstance(imageInfo).getPrvwParam();

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
        final ImageParam params = CR3ImageParser.getInstance(imageInfo).getThmbParam();

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
        final var tiffOffsets = CR3ImageParser.getInstance(imageInfo).getCmtTiffBufferOffsets();
        try {
            for (final var tiffOffset : tiffOffsets) {
                final var reader = new TIFFMetadataReader(imageInfo, buf.initialOffset(tiffOffset));
                final ImageMetadata metadata = reader.readMetadata();
                MetadataUtil.removePreviewMetadataFrom(metadata);
                MetadataUtil.removeWidthHeightFrom(metadata);
            }
        } finally {
            buf.initialOffset(0);
        }
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

    private record ImageParam(int offset, int length) { }

    private static class CR3ImageParser {
        private final boolean DEBUG = false;

        private CR3ImageParser(ImageInfo imageInfo) throws IOException {
            this.imageInfo = imageInfo;
            parse();
        }

        private final ImageInfo imageInfo;
        private static final LRUHashMap<ImageInfo, CR3ImageParser> cache = new LRUHashMap<>(100);

        public static CR3ImageParser getInstance(final ImageInfo imageInfo) throws IOException {
            if (cache.containsKey(imageInfo))
                return cache.get(imageInfo);

            final var instance = new CR3ImageParser(imageInfo);
            cache.put(imageInfo, instance);
            return instance;
        }

        // constants for the CR3's ISO/IEC 14496-12 file format

        private static final byte[] moovTag = "moov".getBytes(UTF_8);
        private static final byte[] ctboTag = "CTBO".getBytes(UTF_8);
        private static final byte[] thmbTag = "THMB".getBytes(UTF_8);
        private static final byte[] cmt1Tag = "CMT1".getBytes(UTF_8);
        private static final byte[] cmt2Tag = "CMT2".getBytes(UTF_8);
        private static final int boxLengthSize = 4;
        private static final int boxNameSize = 4;
        private static final int headerSize = boxLengthSize + boxNameSize;
        private static final int extendedTypeSize = 16;

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

        private final Set<Integer> cmtTiffBufferOffsets = new HashSet<>();
        private ImageParam prvwParam;
        private ImageParam thmbParam;

        @NotNull
        public CR3ImageType.ImageParam getPrvwParam() {
            return prvwParam;
        }

        @Nullable
        public CR3ImageType.ImageParam getThmbParam() {
            return thmbParam;
        }

        public Set<Integer> getCmtTiffBufferOffsets() {
            return cmtTiffBufferOffsets;
        }

        private void parse() throws IOException {
            final LCByteBuffer buf = imageInfo.getByteBuffer();
            final ByteOrder origOrder = buf.order();
            buf.order(ByteOrder.BIG_ENDIAN);

            int boxPos = 0;

            // Seek MOOV box.
            int moovBoxEnd = -1;
            while (boxPos < buf.limit()) {
                final var boxType = buf.getBytes(boxPos + boxLengthSize, boxNameSize);
                if (DEBUG)
                    System.out.println("boxType: " + new String(boxType, UTF_8) + ", pos: " + boxPos);
                if (Arrays.equals(boxType, moovTag)) {
                    moovBoxEnd = boxPos + (int) getBoxSize(buf, boxPos);
                    final var moovUuidTagPos = boxPos + headerSize;
                    // byte[] moovUuid = buf.getBytes(moovUuidTagPos + headerSize, extendedTypeSize);
                    boxPos = moovUuidTagPos + headerSize + extendedTypeSize; // skip uuid
                    break;
                }
                boxPos += getBoxSize(buf, boxPos);
            }

            // Check sub-boxes in the MOOV box.
            int thmbBoxPos = -1;
            int prvwBoxPos = -1;
            while (boxPos < moovBoxEnd) {
                final var boxType = buf.getBytes(boxPos + boxLengthSize, boxNameSize);
                if (DEBUG)
                    System.out.println("boxType: " + new String(boxType, UTF_8) + ", pos: " + boxPos);
                if (Arrays.equals(boxType, ctboTag)) {
                    // cf. https://github.com/lclevy/canon_cr3#ctbo
                    final int recodeSize = 20;
                    final int xpacketRecordPos = boxPos + headerSize + 4; // 4 for number of records
                    final int prvwRecordPos = xpacketRecordPos + recodeSize;
                    final long prvwUuidOffset = buf.getLong(prvwRecordPos + 4); // 4 for recode index
                    prvwBoxPos = (int) (prvwUuidOffset + headerSize + extendedTypeSize + 8); // 8 to skip unknown data
                } else if (Arrays.equals(boxType, cmt1Tag) || Arrays.equals(boxType, cmt2Tag)) {
                    cmtTiffBufferOffsets.add(boxPos + headerSize);
                } else if (Arrays.equals(boxType, thmbTag)) {
                    thmbBoxPos = boxPos;
                }
                boxPos += getBoxSize(buf, boxPos);
            }

            buf.order(origOrder);

            // cf. https://github.com/lclevy/canon_cr3#thmb-thumbnail
            final byte thmbVersion = buf.get(thmbBoxPos + 8);
            if (thmbBoxPos >= 0 && (thmbVersion == 0 || thmbVersion == 1)) {
                final int thmbLength = buf.getInt(thmbBoxPos + 16);
                final int thmbOffset = thmbBoxPos + 24;
                thmbParam = new ImageParam(thmbOffset, thmbLength);
            }

            // cf. https://github.com/lclevy/canon_cr3#prvw-preview
            if (prvwBoxPos >= 0) {
                final int prvwLength = buf.getInt(prvwBoxPos + 20);
                final int prvwOffset = prvwBoxPos + 24;
                prvwParam = new ImageParam(prvwOffset, prvwLength);
            }
        }
    }
}
/* vim:set et sw=4 ts=4: */
