/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.metadata.makernotes.MakerNotesDirectory;
import com.lightcrafts.image.metadata.makernotes.MakerNoteProbe;
import com.lightcrafts.image.metadata.values.ImageMetaValue;

import static com.lightcrafts.image.metadata.EXIFTags.*;
import static com.lightcrafts.image.metadata.ImageMetadataConstants.DIRECTORY_ENTRY_MAX_SANE_SIZE;

/**
 * An <code>EXIFMetadataReader</code> is-an {@link ImageMetadataReader} that
 * reads EXIF metadata directories.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class EXIFMetadataReader extends ImageMetadataReader
    implements EXIFParserEventHandler {

    ////////// public /////////////////////////////////////////////////////////

    /**
     *
     * @param imageInfo
     * @param exifSegBuf The {@link LCByteBuffer} containing the raw bytes of
     * @param isSubdirectory
     */
    public EXIFMetadataReader( ImageInfo imageInfo, LCByteBuffer exifSegBuf,
                               boolean isSubdirectory ) {
        super( imageInfo, exifSegBuf );
        m_exifParser = new EXIFParser(
            this, imageInfo, exifSegBuf, isSubdirectory
        );
    }

    /**
     * Read the metadata from a single directory.
     *
     * @param offset The offset from the beginning of the file of the
     * directory.
     * @param valueOffsetAdjustment The larger-than-4-byte-value offset
     * adjustment.
     * @param dir The metadata is put here.
     */
    public void readDirectory( int offset, int valueOffsetAdjustment,
                               ImageMetadataDirectory dir ) throws IOException {
        m_exifParser.parseDirectory( offset, valueOffsetAdjustment, dir );
    }

    ////////// EXIFMetadataReader methods /////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAllDirectories() throws IOException {
        m_exifParser.parseAllDirectories();
        mergeEXIFDirectories();
        readMakerNotes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readHeader() throws BadImageFileException, IOException {
        m_exifParser.parseHeader();
    }

    /**
     * Reads the maker notes metadata, if any.  This should be called after
     * calling {@link #readDirectory(int,int,ImageMetadataDirectory)} but
     * <b>not</b> after calling {@link #readAllDirectories()} -- the latter
     * calls it automatically.
     */
    public void readMakerNotes() throws IOException {
        if ( m_makerNotesByteCount > 0 ) {
            final Class<? extends MakerNotesDirectory> dirClass =
                    MakerNoteProbe.determineMakerNotesFrom( m_metadata );
            if ( dirClass != null )
                readMakerNotes(
                        m_makerNotesValueOffset, m_makerNotesByteCount, dirClass
                );
        }
    }

    ////////// EXIFParserEventHandler methods /////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public void gotBadMetadata( String message ) {
        logBadImageMetadata( message );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gotBadMetadata( Throwable cause ) {
        logBadImageMetadata( cause );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageMetadataDirectory gotDirectory() {
        ++m_ifdIndex;
        return createDirectoryFor( EXIFDirectory.class, "IFD" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gotTag( int tagID, int fieldType, int numValues, int byteCount,
                        int valueOffset, int valueOffsetAdjustment,
                        int subdirOffset, ImageInfo imageInfo, LCByteBuffer buf,
                        ImageMetadataDirectory dir )
        throws IOException
    {
        if ( m_tagHandler != null ) {
            final boolean handledTag = m_tagHandler.handleTag(
                    tagID, fieldType, numValues, byteCount, valueOffset,
                    valueOffsetAdjustment, subdirOffset, imageInfo, buf, dir
            );
            if ( handledTag )
                return;
        }
        switch ( tagID ) {
            case EXIF_GPS_IFD_POINTER:
                final ImageMetadataDirectory gpsDir =
                    m_metadata.getDirectoryFor( GPSDirectory.class, true );
                m_exifParser.parseDirectory( subdirOffset, 0, gpsDir );
                return;
            case EXIF_IFD_POINTER:
                m_exifParser.parseDirectory( subdirOffset, 0, dir );
                return;
            case EXIF_INTEROPERABILITY_POINTER:
                // TODO: handle this case
                return;
            case EXIF_MAKER_NOTE:
                //
                // We have to defer reading maker notes until after all the
                // rest of the metadata is read since reading the maker notes
                // requires probing.  That in turn requires tags like the
                // camera make to be present.
                //
                m_makerNotesByteCount = byteCount;
                m_makerNotesValueOffset = valueOffset;
                return;
            case EXIF_USER_COMMENT:
                // TODO: handle this case
                return;
        }

        if ( byteCount > DIRECTORY_ENTRY_MAX_SANE_SIZE )
            return;
        final ImageMetaValue value = m_exifParser.parseValue(
            tagID, fieldType, valueOffset, numValues
        );
        if ( value != null )
            try {
                dir.putValue( tagID, value );
            }
            catch ( IllegalArgumentException e ) {
                gotBadMetadata( e );
            }
    }

    ////////// protected //////////////////////////////////////////////////////

    /** The {@link EXIFParser} in use. */
    protected final EXIFParser m_exifParser;

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Copy metadata values from a TIFF {@link ImageMetadataDirectory} that is
     * not for the full-sized image, e.g., a preview or a thumbnail, to the
     * TIFF {@link ImageMetadataDirectory} for the full-sized image.
     * <p>
     * Note that the copy is a partial copy, i.e., it doesn't copy all tags.
     * It only copies only those that could be useful for the full-sized image
     * (like ARTIST, MAKE, and MODEL), i.e., tags that aren't specific to the
     * non-full-sized image.
     *
     * @param fromDir The {@link ImageMetadataDirectory} to copy values from.
     * @param toDir The {@link ImageMetadataDirectory} to copy values to.
     */
    private static void copyValuesFromTo( ImageMetadataDirectory fromDir,
                                          ImageMetadataDirectory toDir ) {
        for (final Map.Entry<Integer, ImageMetaValue> me : fromDir) {
            final int tagID = me.getKey();
            switch (tagID) {
                case EXIF_BITS_PER_SAMPLE:
                case EXIF_CFA_PATTERN:
                case EXIF_COLOR_SPACE:
                case EXIF_COMPONENTS_CONFIGURATION:
                case EXIF_COMPRESSED_BITS_PER_PIXEL:
                case EXIF_COMPRESSION:
                case EXIF_GPS_IFD_POINTER:
                case EXIF_ICC_PROFILE:
                case EXIF_IFD_POINTER:
                case EXIF_IMAGE_HEIGHT:
                case EXIF_IMAGE_WIDTH:
                case EXIF_INTEROPERABILITY_POINTER:
                case EXIF_MAKER_NOTE:
                case EXIF_NEW_SUBFILE_TYPE:
                case EXIF_OECF:
                case EXIF_ORIENTATION:
                case EXIF_PHOTOMETRIC_INTERPRETATION:
                case EXIF_PIXEL_X_DIMENSION:
                case EXIF_PIXEL_Y_DIMENSION:
                case EXIF_PLANAR_CONFIGURATION:
                case EXIF_PREDICTOR:
                case EXIF_PRIMARY_CHROMATICITIES:
                case EXIF_REFERENCE_BLACK_WHITE:
                case EXIF_RESOLUTION_UNIT:
                case EXIF_ROWS_PER_STRIP:
                case EXIF_SAMPLES_PER_PIXEL:
                case EXIF_STRIP_BYTE_COUNTS:
                case EXIF_STRIP_OFFSETS:
                case EXIF_SUBFILE_TYPE:
                case EXIF_SUB_IFDS:
                case EXIF_TILE_BYTE_COUNTS:
                case EXIF_TILE_LENGTH:
                case EXIF_TILE_OFFSETS:
                case EXIF_TRANSFER_FUNCTION:
                case EXIF_X_RESOLUTION:
                case EXIF_YCBCR_COEFFICIENTS:
                case EXIF_YCBCR_POSITIONING:
                case EXIF_YCBCR_SUBSAMPLING:
                case EXIF_Y_RESOLUTION:
                    continue;
                default:
                    final ImageMetaValue toValue = toDir.getValue(tagID);
                    if (toValue == null)
                        toDir.putValue(tagID, me.getValue());
            }
        }
    }

    /**
     * Creates a new {@link ImageMetadataDirectory} of the given class and
     * stores a local mapping to it by the given name.
     *
     * @param dirClass The class of the {@link ImageMetadataDirectory} class to
     * create.
     * @param name The name for the {@link ImageMetadataDirectory}.
     * @return Returns a new {@link ImageMetadataDirectory} for the given
     * {@link Class}.
     */
    private ImageMetadataDirectory createDirectoryFor(
        Class<? extends ImageMetadataDirectory> dirClass, String name )
    {
        try {
            final ImageMetadataDirectory dir = dirClass.newInstance();
            dir.setOwningMetadata( m_metadata );
            m_dirMap.put( name + m_ifdIndex, dir );
            return dir;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException( e );
        } catch (InstantiationException e) {
            throw new IllegalStateException( e );
        } catch ( RuntimeException e ) {
            throw new IllegalStateException( e );
        }
    }

    /**
     * Merge the values of the EXIF directories together.
     */
    private void mergeEXIFDirectories() {
        final ImageMetadataDirectory ifd0 = m_dirMap.get( "IFD0" );
        if ( ifd0 == null )
            return;
        for ( Map.Entry<String,ImageMetadataDirectory>
              me : m_dirMap.entrySet() ) {
            final ImageMetadataDirectory dir = me.getValue();
            if ( !(dir instanceof EXIFDirectory) || dir == ifd0 )
                continue;
            copyValuesFromTo( dir, ifd0 );
        }

        m_metadata.putDirectory( ifd0 );
    }

    /**
     * Read a camera manufacturer's maker notes.
     *
     * @param offset The offset from the beginning of the buffer of the maker
     * @param byteCount The total number of bytes for the maker notes data.
     * @param dirClass The {@link Class} of the maker notes directory.
     */
    public void readMakerNotes(int offset, int byteCount,
                               Class<? extends MakerNotesDirectory> dirClass )
        throws IOException
    {
        final ImageMetadataDirectory dir =
                m_metadata.getDirectoryFor( dirClass, true );
        int valueOffsetAdjustment = 0;
        final int[] adjustments = dir.getMakerNotesAdjustments( m_buf, offset );
        if ( adjustments != null ) {
            offset += adjustments[0];
            valueOffsetAdjustment = adjustments[1];
        }
        if ( !dir.readMakerNotes( m_buf, offset, byteCount ) ) {
            final ByteOrder origOrder = m_buf.probeOrder( offset );
            m_exifParser.parseDirectory( offset, valueOffsetAdjustment, dir );
            m_buf.order( origOrder );
        }
    }

    /**
     * A map of directory names (e.g., "IFD0", "SubIFD1") to directories.
     */
    private final Map<String,ImageMetadataDirectory> m_dirMap =
        new HashMap<String,ImageMetadataDirectory>();

    /**
     * A sequential index used to form the names of the IFDs.
     */
    private int m_ifdIndex = -1;

    /** The number of bytes in the maker notes data. */
    private int m_makerNotesByteCount;

    /** The offset of the maker notes data. */
    private int m_makerNotesValueOffset;
}
/* vim:set et sw=4 ts=4: */
