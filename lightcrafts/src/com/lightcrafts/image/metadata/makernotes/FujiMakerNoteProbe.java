/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadata;

/**
 * <code>FujiMakerNoteProbe</code> is-a {@link MakerNoteProbe} for determining
 * whether a maker note is by Fuji.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class FujiMakerNoteProbe extends MakerNoteProbe {

    /** The singleton instance of <code>FujiMakerNoteProbe</code>. */
    static final MakerNoteProbe INSTANCE = new FujiMakerNoteProbe();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected Class<? extends MakerNotesDirectory>
    match( ImageMetadata metadata ) {
        return matchUsingMake( metadata, "FUJIFILM", FujiDirectory.class );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>FujiMakerNoteProbe</code>.
     */
    private FujiMakerNoteProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
