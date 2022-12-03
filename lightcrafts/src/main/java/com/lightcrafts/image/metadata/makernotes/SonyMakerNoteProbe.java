/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.makernotes;

import lombok.NoArgsConstructor;

/**
 * {@code SonyMakerNoteProbe} is-a {@link MakerNoteProbe} for determining
 * whether the maker notes are by Sony.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
@NoArgsConstructor(staticName = "create")
final class SonyMakerNoteProbe extends MakerNoteProbe<SonyDirectory>
{
    @Override
    protected Class<SonyDirectory> getDirClass() {
        return SonyDirectory.class;
    }
}
/* vim:set et sw=4 ts=4: */

