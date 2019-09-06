/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;

import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.makernotes.MakerNoteProbe;
import com.lightcrafts.image.metadata.makernotes.MakerNotesDirectory;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import org.w3c.dom.Document;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;

import static com.lightcrafts.image.metadata.DNGTags.DNG_PRIVATE_DATA;

/**
 * A <code>DNGImageType</code> is-a {@link RawImageType} for DNG (Digital
 * NeGative) images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class DNGImageType extends RawImageType implements TagHandler {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>DNGImageType</code>. */
    public static final DNGImageType INSTANCE = new DNGImageType();

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
        return "DNG";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document getXMP( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return TIFFImageType.getXMP( imageInfo, DNGDirectory.class );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handleTag(int tagID, int fieldType, int numValues,
                             int byteCount,
                             int valueOffset, int valueOffsetAdjustment,
                             int subdirOffset, ImageInfo imageInfo,
                             LCByteBuffer buf, ImageMetadataDirectory dir)
            throws IOException
    {
        if (!(dir instanceof DNGDirectory)) {
            return false;
        }
        switch (tagID) {
            case DNG_PRIVATE_DATA:
                // This is used for maker notes by some camera vendors.
                final ImageMetadata metadata = imageInfo.getCurrentMetadata();
                final Class<? extends MakerNotesDirectory> notesClass =
                        MakerNoteProbe.determineMakerNotesFrom(metadata);
                if (notesClass != null) {
                    final EXIFMetadataReader reader =
                            new EXIFMetadataReader(imageInfo, buf, true);
                    reader.readMakerNotes(valueOffset, byteCount, notesClass);
                }
                return true;
            default:
                return false;
        }
    }

    /**
     * Reads all the metadata for a given DNG image.
     *
     * @param imageInfo The image to read the metadata from.
     */
    @Override
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final TIFFMetadataReader reader =
            new TIFFMetadataReader( imageInfo, DNGDirectory.class );
        reader.setTagHandler( this );
        final ImageMetadata metadata = reader.readMetadata();
        final Document xmpDoc = getXMP( imageInfo );
        if ( xmpDoc != null ) {
            final ImageMetadata xmpMetadata =
                XMPMetadataReader.readFrom( xmpDoc );
            metadata.mergeFrom( xmpMetadata );
        }
    }

    // through DCRaw, should be fast enough...
    @Override
    public boolean hasFastPreview() {
        return true;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>DNGImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private DNGImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for DNG files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "dng"
    };
}
/* vim:set et sw=4 ts=4: */
