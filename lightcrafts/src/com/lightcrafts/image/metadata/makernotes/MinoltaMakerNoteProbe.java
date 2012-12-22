/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadata;

/**
 * <code>MinoltaMakerNoteProbe</code> is-a {@link MakerNoteProbe} for
 * determining whether a maker note is by Minolta.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class MinoltaMakerNoteProbe extends MakerNoteProbe {

    /** The singleton instance of <code>MinoltaMakerNoteProbe</code>. */
    static final MakerNoteProbe INSTANCE = new MinoltaMakerNoteProbe();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected Class<? extends MakerNotesDirectory>
    match( ImageMetadata metadata ) {
        return matchUsingMake( metadata, "MINOLTA", MinoltaDirectory.class );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>MinoltaMakerNoteProbe</code>.
     */
    private MinoltaMakerNoteProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
