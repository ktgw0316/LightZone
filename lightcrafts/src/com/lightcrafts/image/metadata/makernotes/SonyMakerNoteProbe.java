/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadata;

/**
 * {@code SonyMakerNoteProbe} is-a {@link MakerNoteProbe} for determining
 * whether the maker notes are by Sony.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class SonyMakerNoteProbe extends MakerNoteProbe {

    /** The singleton instance of {@code SonyMakerNoteProbe}. */
    static final MakerNoteProbe INSTANCE = new SonyMakerNoteProbe();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends MakerNotesDirectory>
    match( ImageMetadata metadata ) {
        return matchUsingMake( metadata, "SONY", SonyDirectory.class );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Constructs the singleton instance of {@code SonyMakerNoteProbe}.
     */
    private SonyMakerNoteProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
