/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import com.lightcrafts.image.ImageInfo;

/**
 * A <code>RAFImageInfo</code> is-an {@link AuxiliaryImageInfo} for holding
 * additional information for a Fuji raw image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class RAFImageInfo extends RawImageInfo {

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Construct an <code>RAFImageInfo</code>.
     *
     * @param imageInfo The image to construct the <code>RAFImageInfo</code>
     * for.
     */
    RAFImageInfo( ImageInfo imageInfo ) {
        super( imageInfo );
    }

    /**
     * Gets the offset from the beginning of the file to where the TIFF header
     * is.
     *
     * @return Returns said offset.
     */
    int getTIFFOffset() {
        return m_tiffOffset;
    }

    /**
     * Sets the offset from the beginning of the file to where the TIFF header
     * is.
     *
     * @param tiffOffset The offset.
     */
    void setTIFFOffset( int tiffOffset ) {
        m_tiffOffset = tiffOffset;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The offset from the beginning of the file to where the TIFF header is.
     */
    private int m_tiffOffset;
}
/* vim:set et sw=4 ts=4: */
