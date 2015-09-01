/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.ImageMetaTagInfo;
import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.image.metadata.ImageMetaTags;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import static com.lightcrafts.image.metadata.EXIFConstants.EXIF_HEADER_START_SIZE;
import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.makernotes.FujiTags.*;

/**
 * A <code>FujiDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding Fuji-specific maker-note metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class FujiDirectory extends MakerNotesDirectory {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the maker-notes adjustments for Fuji.
     *
     * @param buf The {@link LCByteBuffer} the metadata is in.
     * @param offset The offset to the start of the maker-notes.
     * @return Returns said adjustments.
     * @throws IOException
     */
    public int[] getMakerNotesAdjustments( LCByteBuffer buf, int offset )
        throws IOException
    {
        if ( buf.getEquals( 0, "FUJIFILMCCD-RAW", "ASCII" ) )
            offset -= EXIF_HEADER_START_SIZE;
        //
        // The 12 bytes are:
        //
        //      0- 7: FUJIFILM
        //      8-11: unknown
        //
        return new int[]{ 12, offset };
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;Fuji&quot;.
     */
    public String getName() {
        return "Fuji";
    }

    /**
     * {@inheritDoc}
     */
    public ImageMetaTagInfo getTagInfoFor( Integer id ) {
        return m_tagsByID.get( id );
    }

    /**
     * {@inheritDoc}
     */
    public ImageMetaTagInfo getTagInfoFor( String name ) {
        return m_tagsByName.get( name );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Get the {@link ResourceBundle} to use for tags.
     *
     * @return Returns said {@link ResourceBundle}.
     */
    protected ResourceBundle getTagLabelBundle() {
        return m_tagBundle;
    }

    /**
     * {@inheritDoc}
     */
    protected Class<? extends ImageMetaTags> getTagsInterface() {
        return FujiTags.class;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Add the tag mappings.
     *
     * @param id The tag's ID.
     * @param name The tag's name.
     * @param type The tag's {@link ImageMetaType}.
     */
    private static void add( int id, String name, ImageMetaType type ) {
        final ImageMetaTagInfo tagInfo =
            new ImageMetaTagInfo( id, name, type, false );
        m_tagsByID.put( id, tagInfo );
        m_tagsByName.put( name, tagInfo );
    }

    /**
     * This is where the actual labels for the tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.makernotes.FujiTags"
    );

    /**
     * A mapping of tags by ID.
     */
    private static final Map<Integer,ImageMetaTagInfo> m_tagsByID =
        new HashMap<Integer,ImageMetaTagInfo>();

    /**
     * A mapping of tags by name.
     */
    private static final Map<String,ImageMetaTagInfo> m_tagsByName =
        new HashMap<String,ImageMetaTagInfo>();

    static {
        add( FUJI_AUTO_BRACKETING, "AutoBracketing", META_USHORT );
        add( FUJI_BLUR_WARNING, "BlurWarning", META_USHORT );
        add( FUJI_CONTRAST, "Contrast", META_USHORT );
        add( FUJI_EXPOSURE_WARNING, "ExposureWarning", META_USHORT );
        add( FUJI_FLASH_MODE, "FlashMode", META_USHORT );
        add( FUJI_FLASH_STRENGTH, "FlashStrength", META_USHORT );
        add( FUJI_FOCUS_MODE, "FocusMode", META_USHORT );
        add( FUJI_FOCUS_WARNING, "FocusWarning", META_USHORT );
        add( FUJI_MACRO_MODE, "MacroMode", META_USHORT );
        add( FUJI_PICTURE_MODE, "PictureMode", META_USHORT );
        add( FUJI_QUALITY, "Quality", META_STRING );
        add( FUJI_SATURATION, "Saturation", META_USHORT );
        add( FUJI_SHARPNESS, "Sharpness", META_USHORT );
        add( FUJI_SLOW_SYNC, "SlowSync", META_USHORT );
        add( FUJI_WHITE_BALANCE, "WhiteBalance", META_USHORT );
        add( FUJI_VERSION, "Version", META_UNDEFINED );
    }
}
/* vim:set et sw=4 ts=4: */
