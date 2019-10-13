/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

/**
 * An <code>ImageMetadataReader</code> is the abstract base class for all
 * image metadata readers, e.g., EXIF and IPTC readers.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class ImageMetadataReader {

    /**
     * The log level at which metadata exceptions are logged.
     */
    private static final Level LOG_LEVEL = Level.INFO;

    /**
     * The {@link Logger} to which {@link BadImageMetadataException}s are
     * logged.
     */
    private static final Logger m_logger =
        Logger.getLogger( "com.lightcrafts.image.metadata" );

    /**
     * Read all the metadata from an image file.
     *
     * @return Returns the read metadata.
     * @throws BadImageFileException if the internal format of the image file
     * isn't as it's expected to be.
     */
    public final ImageMetadata readMetadata()
        throws BadImageFileException, IOException
    {
        try {
            readHeader();
        } catch ( BadImageMetadataException e ) {
            // Assume that any other exception is the result of a bad image
            // file.
            throw new BadImageFileException( m_imageInfo.getFile(), e );
        }

        try {
            readAllDirectories();
        } catch ( RuntimeException e ) {
            // Assume that any other exception is the result of a bad image
            // file.  However, that said, ignore it and try to forge ahead
            // anyway.
            logBadImageMetadata( e );
        }
        return m_metadata;
    }

    /**
     * This is a convenience method for giving an {@link Exception} to the
     * logger.
     *
     * @param e The {@link Exception} to log.
     */
    private static void logException(Exception e) {
        final String msg = e.toString();
        if (msg != null) {
            m_logger.log(LOG_LEVEL, msg);
        }
    }

    /**
     * Set the {@link TagHandler} to use.
     *
     * @param handler The {@link TagHandler} to use.
     */
    public final void setTagHandler( TagHandler handler ) {
        m_tagHandler = handler;
    }

    /**
     * Construct an <code>ImageMetadataReader</code> and read the metadata.
     *
     * @param imageInfo The image to read the metadata from.
     * @param metadataBuf The {@link ByteBuffer} containing the raw binary
     * metadata from the image file.  Note that this need not be the same as
     * the {@link ByteBuffer} returned by {@link ImageInfo#getByteBuffer()}
     */
    protected ImageMetadataReader( ImageInfo imageInfo,
                                   LCByteBuffer metadataBuf ) {
        m_imageInfo = imageInfo;
        m_buf = metadataBuf;
        m_metadata = imageInfo.getCurrentMetadata();
    }

    /**
     * This is a convenience method for giving a
     * {@link BadImageMetadataException} to the logger.
     */
    void logBadImageMetadata() {
        logException( new BadImageMetadataException( m_imageInfo.getFile() ) );
    }

    /**
     * This is a convenience method for giving a
     * {@link BadImageMetadataException} to the logger.
     *
     * @param message An informational message.
     */
    void logBadImageMetadata( String message ) {
        logException(
            new BadImageMetadataException( m_imageInfo.getFile(), message )
        );
    }

    /**
     * This is a convenience method for giving a
     * {@link BadImageMetadataException} to the logger.
     *
     * @param cause The original exception.
     */
    void logBadImageMetadata( Throwable cause ) {
        /* logException(
            new BadImageMetadataException( m_imageInfo.getFile(), cause )
        ); */
    }

    /**
     * Read the image header.
     *
     * @throws BadImageFileException if the internal format of the image file
     * header isn't as it's expected to be.
     */
    protected abstract void readHeader()
        throws BadImageFileException, IOException;

    /**
     * Read all metadata.
     */
    protected abstract void readAllDirectories() throws IOException;

    /**
     * The {@link ByteBuffer} containing the raw bytes of the metadata from the
     * image file.
     */
    protected final LCByteBuffer m_buf;

    /**
     * The {@link ImageInfo} containing all relevant information about the
     * image file to read the metadata from.
     */
    protected final ImageInfo m_imageInfo;

    /**
     * The metadata is put here.
     */
    final ImageMetadata m_metadata;

    /**
     * The {@link TagHandler}, if any, to use.
     */
    TagHandler m_tagHandler;
}
/* vim:set et sw=4 ts=4: */
