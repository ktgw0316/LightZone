/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.types.JPEGParser;
import com.lightcrafts.image.types.JPEGParserEventHandler;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.bytebuffer.LCMappedByteBuffer;

import static com.lightcrafts.image.types.JPEGConstants.JPEG_APP1_MARKER;

/**
 * An {@link EXIFSegmentFinder} is-a {@link JPEGParserEventHandler} that
 * finds the EXIF segment of a JPEG file.
 */
public final class EXIFSegmentFinder implements JPEGParserEventHandler {

    ////////// public ////////////////////////////////////////////////////////

    /**
     * Constructs an {@code EXIFSegmentFinder}.
     * If using this constructor, you should be using
     * {@link #getEXIFSegment()}.
     *
     * @param jpegFile The JPEG image file to find the EXIF segment within.
     */
    public EXIFSegmentFinder( File jpegFile ) {
       this( jpegFile, 0 );
    }

    /**
     * Constructs an {@code EXIFSegmentFinder}.
     * If using this constructor, you should be using
     * {@link #getEXIFSegmentFrom(LCByteBuffer)}.
     *
     * @param imageFile The image file to find the EXIF segment within.
     * @param offset The offset from the beginning of the file where the
     * passed-in {@link LCByteBuffer} for
     * {@link #getEXIFSegmentFrom(LCByteBuffer)} starts.
     */
    public EXIFSegmentFinder( File imageFile, int offset ) {
        m_imageFile = imageFile;
        m_offset = offset;
    }

    /**
     * Gets the EXIF segment, if any, of the JPEG file.
     *
     * @return Returns an {@link LCMappedByteBuffer} mapped to the EXIF
     * segment or {@code null} if there is no EXIF segment.
     * @see #getEXIFSegmentFrom(LCByteBuffer)
     */
    public LCMappedByteBuffer getEXIFSegment()
            throws IOException, BadImageFileException {
        try (LCMappedByteBuffer buf = new LCMappedByteBuffer(m_imageFile)) {
            return getEXIFSegmentFrom(buf);
        }
    }

    /**
     * Gets the EXIF segment, if any, from the JPEG image in the given buffer.
     *
     * @param buf An {@link LCByteBuffer} containing the bytes of an entire
     * JPEG image.
     * @return Returns an {@link LCMappedByteBuffer} mapped to the EXIF
     * segment or {@code null} if there is no EXIF segment.
     * @see #getEXIFSegment()
     */
    public LCMappedByteBuffer getEXIFSegmentFrom( LCByteBuffer buf )
            throws IOException, BadImageFileException {
        JPEGParser.parse( this, m_imageFile, buf );
        return m_exifSegBuf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean gotSegment( byte segID, int segLength, File jpegFile,
                               LCByteBuffer buf ) throws IOException {
        if ( segID == JPEG_APP1_MARKER &&
             buf.getEquals( "Exif", "ASCII" ) ) {
            m_exifSegBuf = new LCMappedByteBuffer(
                m_imageFile, m_offset + buf.position() - 4, segLength - 4,
                FileChannel.MapMode.READ_WRITE
            );
            return false;
        }
        return true;
    }

    ////////// private ///////////////////////////////////////////////////////

    /** The EXIF segment data is put here. */
    private LCMappedByteBuffer m_exifSegBuf;

    private final File m_imageFile;

    private final int m_offset;
}
/* vim:set et sw=4 ts=4: */
