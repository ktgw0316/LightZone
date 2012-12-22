/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadata;

/**
 * <code>PanasonicMakerNoteProbe</code> is-a {@link MakerNoteProbe} for
 * determining whether a maker note is by Panasonic.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class PanasonicMakerNoteProbe extends MakerNoteProbe {

    /** The singleton instance of <code>PanasonicMakerNoteProbe</code>. */
    static final MakerNoteProbe INSTANCE = new PanasonicMakerNoteProbe();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected Class<? extends MakerNotesDirectory>
    match( ImageMetadata metadata ) {
        return matchUsingMake(
            metadata, "PANASONIC", PanasonicDirectory.class
        );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>PanasonicMakerNoteProbe</code>.
     */
    private PanasonicMakerNoteProbe() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
