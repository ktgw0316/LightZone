/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.image.types.AdobeResourceParserEventHandler;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGAPPDParser;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.lightcrafts.image.metadata.IPTCConstants.*;
import static com.lightcrafts.image.metadata.IPTCTags.*;
import static com.lightcrafts.image.types.AdobeConstants.PHOTOSHOP_IPTC_RESOURCE_ID;

/**
 * An <code>IPTCMetadataReader</code> is-an {@link ImageMetadataReader} for
 * reading IPTC metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class IPTCMetadataReader extends ImageMetadataReader
    implements AdobeResourceParserEventHandler {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>IPTCMetadataReader</code>.
     *
     * @param imageInfo The image to read the metadata from.
     * @param segBuf The {@link LCByteBuffer} containing the raw binary
     * metadata from the image file.  For TIFF images, this is the IPTC
     * metadata directly; for JPEG images, it's a set of Adobe resource blocks,
     * one of which is the IPTC metadata.
     * @param fromType The type of image the IPTC metadata is being read from.
     */
    public IPTCMetadataReader( ImageInfo imageInfo, LCByteBuffer segBuf,
                               ImageType fromType ) {
        super( imageInfo, segBuf );
        m_fromType = fromType;
        if ( !(fromType instanceof JPEGImageType) ) {
            //
            // For non-JPEG images, the entire contents of the buffer are the
            // IPTC metadata.
            //
            m_iptcStartPos = m_buf.position();
            m_iptcLength = m_buf.remaining();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean gotResource( int blockID, String name, int dataLength,
                                File file, LCByteBuffer buf ) {
        if ( blockID == PHOTOSHOP_IPTC_RESOURCE_ID ) {
            //
            // We've found the IPTC resource block as one of the set of Adobe
            // resource blocks: note its start position and length.
            //
            m_iptcStartPos = m_buf.position();
            m_iptcLength = dataLength;
            return false;
        }
        return true;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Read the metadata from all directories.
     */
    @Override
    protected void readAllDirectories() throws IOException {
        if ( m_iptcLength == 0 ) {
            //
            // We never found an IPTC resource block among the set of Adobe
            // resource blocks; hence the image has no IPTC metadata.
            //
            return;
        }
        final ImageMetadataDirectory dir =
            m_metadata.getDirectoryFor( IPTCDirectory.class, true );
        //
        // IPTC metadata has only one directory.
        //
        readDirectory( m_iptcStartPos, dir );
    }

    /**
     * Read the image header.
     */
    @Override
    protected void readHeader() throws BadImageFileException, IOException {
        if ( m_fromType instanceof JPEGImageType ) {
            //
            // We have to parse the APPD segment we were given that contains a
            // set of Adobe resource blocks, one of which may be an IPTC block.
            //
            JPEGAPPDParser.parse( this, m_imageInfo.getFile(), m_buf );
        }
        //
        // For other image types, there is nothing to do since the buffer
        // contains the IPTC metadata directly with no header of any kind.
        //
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Read the metadata from a single directory.
     *
     * @param offset The offset from the beginning of the file of the
     * directory.
     * @param dir The metadata is put here.
     */
    private void readDirectory( int offset, ImageMetadataDirectory dir )
        throws IOException
    {
        m_buf.position( offset );
        while ( offset < m_buf.limit() ) {
            if ( !readDirectoryEntry( offset, dir ) )
                break;
            offset = m_buf.position();
        }
    }

    /**
     * Read the metadata from a single directory entry.
     *
     * @param offset The offset from the beginning of the file of the
     * directory entry.
     * @param dir The metadata is put here.
     * @return Returns <code>true</code> only if the entry was read
     * successfully.
     */
    private boolean readDirectoryEntry( int offset,
                                        ImageMetadataDirectory dir )
        throws IOException
    {
        m_buf.position( offset );
        if ( m_buf.remaining() < IPTC_ENTRY_HEADER_SIZE ) {
            //
            // There needs to be at least 5 bytes remaining for a tag.
            //
            return false;
        }
        if ( m_buf.get() != IPTC_TAG_START_BYTE )
            return false;

        final int tagID = m_buf.getUnsignedShort();
        int byteCount = m_buf.getUnsignedShort();

        if ( (byteCount & 0x8000) != 0 ) {
            //
            // Handle the "Extended DataSet Tag."
            //
            int byteCountLength = byteCount & 0x7FFF;
            byteCount = 0;
            while ( byteCountLength-- > 0 )
                byteCount = (byteCount << 8) | m_buf.getUnsignedByte();
        }

        if ( byteCount > m_buf.remaining() ) {
            logBadImageMetadata();
            return false;
        }

        final ImageMetaValue value = readValue( dir, tagID, byteCount );
        if ( value != null ) {
            try {
                dir.putValue(tagID, value);
            } catch (IllegalArgumentException e) {
                logBadImageMetadata();
            }
        }
        return true;
    }

    /**
     * Read a value (or values) for a given IPTC tag.
     *
     * @param dir The {@link ImageMetadataDirectory} to read a value for.
     * @param tagID The ID of the tag that &quot;owns&quot; this value.
     * @param byteCount The number of bytes in the value.
     */
    private ImageMetaValue readValue( ImageMetadataDirectory dir, int tagID,
                                      int byteCount ) throws IOException {
        switch ( tagID ) {
            case IPTC_RECORD_VERSION:
                return new UnsignedShortMetaValue((short)(m_buf.get() << 8 | m_buf.get()));

            case IPTC_URGENCY:
                return new UnsignedByteMetaValue( m_buf.get() );

            case IPTC_DATE_CREATED:
            case IPTC_DATE_SENT:
            case IPTC_DIGITAL_CREATION_DATE:
            case IPTC_EXPIRATION_DATE:
            case IPTC_RELEASE_DATE:
                return (byteCount >= IPTC_DATE_SIZE)
                        ? new DateMetaValue(m_buf.getString(byteCount, "UTF-8"))
                        : null;

            case IPTC_DIGITAL_CREATION_TIME:
            case IPTC_EXPIRATION_TIME:
            case IPTC_RELEASE_TIME:
            case IPTC_TIME_CREATED:
            case IPTC_TIME_SENT:
            default:
                if ( byteCount < 1 )
                    return null;

                String s = getStringValue(dir, byteCount);
                if (s.isEmpty())
                    return null;

                final ImageMetaValue oldValue = dir.getValue( tagID );
                if ( oldValue == null )
                    return new StringMetaValue( s );
                final boolean old = oldValue.setIsChangeable( true );
                oldValue.appendValue( s );
                oldValue.setIsChangeable( old );
                return oldValue;
        }
    }

    private String getStringValue(ImageMetadataDirectory dir, int byteCount) throws IOException {
        final ImageMetaValue oldCharset = dir.getValue(IPTC_CODED_CHARACTER_SET);
        final byte[] utf8 = {0x1B, 0x25, 0x47}; // ESC, "%", "G"
        String charset =
                (oldCharset != null && Arrays.equals(oldCharset.getStringValue().getBytes(), utf8))
                        ? "UTF-8"
                        : "ISO-8859-1";
        String s = m_buf.getString(byteCount, charset);
        final int nullByte = s.indexOf('\0');
        return (nullByte < 0) ? s : s.substring(0, nullByte);
    }

    /**
     * The type of image we're parsing IPTC metadata from.
     */
    private final ImageType m_fromType;

    private int m_iptcStartPos;
    private int m_iptcLength;
}
/* vim:set et sw=4 ts=4: */
