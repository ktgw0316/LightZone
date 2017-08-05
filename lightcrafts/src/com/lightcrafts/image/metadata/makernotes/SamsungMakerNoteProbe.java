/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadata;

/**
 * {@code SamsungMakerNoteProbe} is-a {@link MakerNoteProbe} for determining
 * whether the maker notes are by Samsung.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class SamsungMakerNoteProbe extends MakerNoteProbe {

    /** The singleton instance of {@code SamsungMakerNoteProbe}. */
    static final MakerNoteProbe INSTANCE = new SamsungMakerNoteProbe();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends MakerNotesDirectory>
    match( ImageMetadata metadata ) {
        //
        // Many Samsung models use Pentax maker notes.
        //
        return matchUsingMake( metadata, "SAMSUNG", PentaxDirectory.class );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Constructs the singleton instance of {@code SamsungMakerNoteProbe}.
     */
    private SamsungMakerNoteProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
