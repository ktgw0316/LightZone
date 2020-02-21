/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadata;

import lombok.NoArgsConstructor;
import lombok.val;

/**
 * <code>KodakMakerNoteProbe</code> is-a {@link MakerNoteProbe} for determining
 * whether a maker note is by Kodak.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
@NoArgsConstructor(staticName = "create")
final class KodakMakerNoteProbe extends MakerNoteProbe<KodakDirectory>
{
    @Override
    protected Class<KodakDirectory> getDirClass() {
        return KodakDirectory.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<KodakDirectory>
    match(ImageMetadata metadata) {
        if (matchUsingMake(metadata) == null) {
            return null;
        }
        val cameraMakeModel = metadata.getCameraMake(true);
        if (cameraMakeModel == null) {
            return null;
        }
        for (val model : m_models) {
            if (cameraMakeModel.contains(model)) {
                return KodakDirectory.class;
            }
        }
        return null;
    }

    /**
     * The Kodak camera models that we know how to parse the maker notes for.
     * All must be in upper-case.
     */
    private static final String[] m_models = {
        "CX6330",
        "CX7330",
        "CX7430",
        "CX7525",
        "CX7530",
        "DC4800",
        "DC4900",
        "DX3500",
        "DX3600",
        "DX3900",
        "DX4330",
        "DX4530",
        "DX4900",
        "DX6340",
        "DX6440",
        "DX6490",
        "DX7440",
        "DX7590",
        "DX7630",
        "EASYSHARE-ONE",
        "LS420",
        "LS443",
        "LS633",
        "LS743",
        "LS753",
        "Z700",
        "Z730",
        "Z740",
        "Z7560",
        "Z760",
    };

}
/* vim:set et sw=4 ts=4: */
