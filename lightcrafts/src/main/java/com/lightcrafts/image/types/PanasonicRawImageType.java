/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadataReader;
import com.lightcrafts.image.metadata.MetadataUtil;
import com.lightcrafts.image.metadata.TIFFMetadataReader;

/**
 * A <code>PanasonicRawImageType</code> is-a {@link RawImageType} for RAW
 * (Panasonic raw) images.
 * <p>
 * Note: Panasonic, in their infinite wisdom, decided to use a
 * <code>.RAW</code> extension for their raw files rather than something
 * sensible like <code>.PRW</code>.  This causes two problems:
 *  <ol>
 *    <li>
 *      This class should be called <code>RAWImageType</code> to conform to the
 *      convention used by other classes derived from {@link ImageType}, but
 *      doing so would make it differ only in case to {@link RawImageType} and
 *      that's problematic on filesystems that are case-preserving but not
 *      case-sensitive, e.g., HFS+ for Mac OS&nbsp;X.
 *    </li>
 *    <li>
 *      As of right now, there are no other supported raw image files that have
 *      a <code>.RAW</code> extension, but, if there ever are, more code will
 *      have to be written that will peek into a <code>.RAW</code> file and
 *      determine which type of raw file it really is.
 *    </li>
 *  </ol>
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class PanasonicRawImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>PanasonicRawImageType</code>. */
    public static final PanasonicRawImageType INSTANCE =
        new PanasonicRawImageType();

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
        return "Panasonic";
    }

    // through dcraw, fast enough...
    public boolean hasFastPreview() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        final ImageMetadataReader reader = new TIFFMetadataReader( imageInfo );
        MetadataUtil.removePreviewMetadataFrom( reader.readMetadata() );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>PanasonicRawImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private PanasonicRawImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for Panasonic raw files.  All must
     * be lower case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "raw"
    };
}
/* vim:set et sw=4 ts=4: */
