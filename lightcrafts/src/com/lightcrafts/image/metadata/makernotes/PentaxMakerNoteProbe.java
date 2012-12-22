/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadata;

/**
 * <code>PentaxMakerNoteProbe</code> is-a {@link MakerNoteProbe} for
 * determining whether a maker note is by Pentax.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class PentaxMakerNoteProbe extends MakerNoteProbe {

    /** The singleton instance of <code>PentaxMakerNoteProbe</code>. */
    static final MakerNoteProbe INSTANCE = new PentaxMakerNoteProbe();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected Class<? extends MakerNotesDirectory>
    match( ImageMetadata metadata ) {
        return matchUsingMake( metadata, "PENTAX", PentaxDirectory.class );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>PentaxMakerNoteProbe</code>.
     */
    private PentaxMakerNoteProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
