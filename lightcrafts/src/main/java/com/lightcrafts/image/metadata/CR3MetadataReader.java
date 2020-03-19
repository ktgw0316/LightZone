package com.lightcrafts.image.metadata;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CR3MetadataReader extends ImageMetadataReader {

    private final boolean DEBUG = false;

    public record ImageParam(int offset, int length) {
    }

    public CR3MetadataReader(ImageInfo imageInfo) throws IOException {
        super(imageInfo, imageInfo.getByteBuffer());
        m_imageInfo = imageInfo;
        parse();
    }

    @Override
    protected void readHeader() throws BadImageFileException, IOException {
    }

    @Override
    protected void readAllDirectories() throws IOException {
        final LCByteBuffer buf = m_imageInfo.getByteBuffer();
        try {
            for (final var p : cmtTiffBufferParams) {
                buf.initialOffset(p.offset);
                final var tiffReader = new TIFFMetadataReader(m_imageInfo, buf, p.dirClass);
                tiffReader.readMetadata();
            }
            MetadataUtil.removePreviewMetadataFrom(m_metadata);
            MetadataUtil.removeWidthHeightFrom(m_metadata);
        } catch (BadImageFileException ignored) {
        } finally {
            buf.initialOffset(0);
        }
    }

    private final ImageInfo m_imageInfo;

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
    private static long getBoxSize(LCByteBuffer buf, long boxPos)
            throws IOException {
        final int boxSize = buf.getInt((int) boxPos);
        return switch (boxSize) {
            case 0 -> buf.limit() - boxPos; // box extends to end of file
            case 1 -> buf.getLong((int) (boxPos + headerSize));
            default -> boxSize;
        };
    }

    private record CmtTiffBufferParam(Class<? extends ImageMetadataDirectory> dirClass, int offset) {
    }

    private final List<CmtTiffBufferParam> cmtTiffBufferParams = new ArrayList<>();
    private ImageParam prvwParam;
    private ImageParam thmbParam;

    @NotNull
    public ImageParam getPrvwParam() {
        return prvwParam;
    }

    @Nullable
    public ImageParam getThmbParam() {
        return thmbParam;
    }

    private void parse() throws IOException {
        final LCByteBuffer buf = m_imageInfo.getByteBuffer();
        final ByteOrder origOrder = buf.order();
        buf.order(ByteOrder.BIG_ENDIAN);

        long boxPos = 0;

        // Seek MOOV box.
        long moovBoxEnd = -1;
        while (boxPos < buf.limit()) {
            final var boxType = buf.getBytes((int) (boxPos + boxLengthSize), boxNameSize);
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
        long thmbBoxPos = -1;
        long prvwBoxPos = -1;
        while (boxPos < moovBoxEnd) {
            final var boxType = buf.getBytes((int) (boxPos + boxLengthSize), boxNameSize);
            if (DEBUG)
                System.out.println("boxType: " + new String(boxType, UTF_8) + ", pos: " + boxPos);
            if (Arrays.equals(boxType, ctboTag)) {
                // cf. https://github.com/lclevy/canon_cr3#ctbo
                final int recodeSize = 20;
                final long xpacketRecordPos = boxPos + headerSize + 4; // 4 for number of records
                final long prvwRecordPos = xpacketRecordPos + recodeSize;
                final long prvwUuidOffset = buf.getLong((int) (prvwRecordPos + 4)); // 4 for recode index
                prvwBoxPos = prvwUuidOffset + headerSize + extendedTypeSize + 8; // 8 to skip unknown data
            } else if (Arrays.equals(boxType, cmt1Tag)) {
                cmtTiffBufferParams.add(new CmtTiffBufferParam(TIFFDirectory.class, (int) (boxPos + headerSize)));
            } else if (Arrays.equals(boxType, cmt2Tag)) {
                cmtTiffBufferParams.add(new CmtTiffBufferParam(EXIFDirectory.class, (int) (boxPos + headerSize)));
            } else if (Arrays.equals(boxType, thmbTag)) {
                thmbBoxPos = boxPos;
            }
            boxPos += getBoxSize(buf, boxPos);
        }

        buf.order(origOrder);

        // cf. https://github.com/lclevy/canon_cr3#thmb-thumbnail
        final byte thmbVersion = buf.get((int) (thmbBoxPos + 8));
        if (thmbBoxPos >= 0 && (thmbVersion == 0 || thmbVersion == 1)) {
            final int thmbLength = buf.getInt((int) (thmbBoxPos + 16));
            final long thmbOffset = thmbBoxPos + 24;
            thmbParam = new ImageParam((int) thmbOffset, thmbLength);
        }

        // cf. https://github.com/lclevy/canon_cr3#prvw-preview
        if (prvwBoxPos >= 0) {
            final int prvwLength = buf.getInt((int) (prvwBoxPos + 20));
            final long prvwOffset = prvwBoxPos + 24;
            prvwParam = new ImageParam((int) prvwOffset, prvwLength);
        }
    }
}
