/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.libs.LibRaw;
import com.lightcrafts.utils.DCRaw;

/**
 * A <code>RawImageInfo</code> is-an {@link AuxiliaryImageInfo} for holding
 * additional information for a raw image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class RawImageInfo extends AuxiliaryImageInfo {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>RawImageInfo</code>.
     *
     * @param imageInfo The image to construct the <code>RawImageInfo</code>
     * for.
     */
    public RawImageInfo( ImageInfo imageInfo ) {
        m_dcRaw = new LibRaw( imageInfo.getFile().getAbsolutePath() );
    }

    /**
     * Get the {@link DCRaw} object for a raw image.
     *
     * @return Returns said {@link DCRaw} object.
     */
    public LibRaw getDCRaw() {
        return m_dcRaw;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final LibRaw m_dcRaw;
}
/* vim:set et sw=4 ts=4: */
