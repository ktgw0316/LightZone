/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.utils.bytebuffer.ArrayByteBuffer;

/**
 * A <code>MRWImageInfo</code> is-an {@link AuxiliaryImageInfo} for holding
 * additional information for a Minolta raw image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class MRWImageInfo extends RawImageInfo {

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Construct an <code>MRWImageInfo</code>.
     *
     * @param imageInfo The image to construct the <code>MRWImageInfo</code>
     * for.
     */
    MRWImageInfo( ImageInfo imageInfo ) {
        super( imageInfo );
    }

    /**
     * Get the array containing the TIFF ("TTW") block from the MRW image.
     *
     * @return Returns said array.
     */
    ArrayByteBuffer getTIFFBlock() {
        return m_tiffBlock;
    }

    /**
     * Set the TIFF ("TTW") block from the MRW image.
     *
     * @param tiffBlock The array containing the TIFF block.
     */
    void setTIFFBlock( ArrayByteBuffer tiffBlock ) {
        m_tiffBlock = tiffBlock;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * This is the contents of the TIFF ("TTW") block from the MRW image.
     */
    private ArrayByteBuffer m_tiffBlock;
}
/* vim:set et sw=4 ts=4: */
