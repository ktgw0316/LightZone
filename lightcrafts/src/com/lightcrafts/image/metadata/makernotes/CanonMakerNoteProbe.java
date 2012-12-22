/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadata;

/**
 * <code>CanonMakerNoteProbe</code> is-a {@link MakerNoteProbe} for determining
 * whether a maker note is by Canon.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class CanonMakerNoteProbe extends MakerNoteProbe {

    /** The singleton instance of <code>CanonMakerNoteProbe</code>. */
    static final MakerNoteProbe INSTANCE = new CanonMakerNoteProbe();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected Class<? extends MakerNotesDirectory>
    match( ImageMetadata metadata ) {
        return matchUsingMake( metadata, "CANON", CanonDirectory.class );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>CanonMakerNoteProbe</code>.
     */
    private CanonMakerNoteProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
