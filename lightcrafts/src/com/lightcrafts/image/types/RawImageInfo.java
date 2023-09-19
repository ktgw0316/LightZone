/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2023-     Masahiro Kitagawa */

package com.lightcrafts.image.types;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.libs.LibRaw;
import com.lightcrafts.utils.raw.DCRaw;
import com.lightcrafts.utils.raw.RawDecoder;

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
        final var absolutePath = imageInfo.getFile().getAbsolutePath();
        final var forceLibraw = Boolean.getBoolean("lightzone.force_libraw");
        if (!forceLibraw) {
            final var dcRaw = DCRaw.getInstanceFor(absolutePath);
            if (dcRaw.decodable()) {
                decoder = dcRaw;
                return;
            }
        }
        decoder = new LibRaw(absolutePath);
    }

    /**
     * Get the {@link RawDecoder} object for a raw image.
     *
     * @return Returns said {@link RawDecoder} object.
     */
    public RawDecoder getRawDecoder() {
        return decoder;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final RawDecoder decoder;
}
/* vim:set et sw=4 ts=4: */
