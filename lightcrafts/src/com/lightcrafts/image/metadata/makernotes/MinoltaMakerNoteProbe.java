/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.makernotes;

import lombok.NoArgsConstructor;

/**
 * <code>MinoltaMakerNoteProbe</code> is-a {@link MakerNoteProbe} for
 * determining whether a maker note is by Minolta.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
@NoArgsConstructor(staticName = "create")
final class MinoltaMakerNoteProbe extends MakerNoteProbe<MinoltaDirectory>
{
    @Override
    protected Class<MinoltaDirectory> getDirClass() {
        return MinoltaDirectory.class;
    }
}
/* vim:set et sw=4 ts=4: */
