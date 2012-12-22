/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadata;

/**
 * <code>NikonMakerNoteProbe</code> is-a {@link MakerNoteProbe} for determining
 * whether a maker note is by Nikon.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class NikonMakerNoteProbe extends MakerNoteProbe {

    /** The singleton instance of <code>NikonMakerNoteProbe</code>. */
    static final MakerNoteProbe INSTANCE = new NikonMakerNoteProbe();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected Class<? extends MakerNotesDirectory>
    match( ImageMetadata metadata ) {
        return matchUsingMake( metadata, "NIKON", NikonDirectory.class );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>NikonMakerNoteProbe</code>.
     */
    private NikonMakerNoteProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
