/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadata;

/**
 * <code>OlympusMakerNoteProbe</code> is-a {@link MakerNoteProbe} for
 * determining whether a maker note is by Olympus.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class OlympusMakerNoteProbe extends MakerNoteProbe {

    /** The singleton instance of <code>OlympusMakerNoteProbe</code>. */
    static final MakerNoteProbe INSTANCE = new OlympusMakerNoteProbe();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected Class<? extends MakerNotesDirectory>
    match( ImageMetadata metadata ) {
        return matchUsingMake( metadata, "OLYMPUS", OlympusDirectory.class );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>OlympusMakerNoteProbe</code>.
     */
    private OlympusMakerNoteProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
