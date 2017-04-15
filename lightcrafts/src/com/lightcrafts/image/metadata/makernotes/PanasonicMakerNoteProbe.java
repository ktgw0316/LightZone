/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.makernotes;

import lombok.NoArgsConstructor;

/**
 * <code>PanasonicMakerNoteProbe</code> is-a {@link MakerNoteProbe} for
 * determining whether a maker note is by Panasonic.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
@NoArgsConstructor (staticName = "create")
final class PanasonicMakerNoteProbe extends MakerNoteProbe<PanasonicDirectory>
{
    @Override
    protected Class<PanasonicDirectory> getDirClass() {
        return PanasonicDirectory.class;
    }
}
/* vim:set et sw=4 ts=4: */
