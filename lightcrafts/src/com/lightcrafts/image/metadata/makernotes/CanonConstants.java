/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

/**
 * An <code>CanonConstants</code> defines some constants for Canon metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface CanonConstants {

    /**
     * Canon camera setting quality: normal.
     * This is one of the possible values for the
     * {@link CanonTags#CANON_CS_QUALITY} metadata tag.
     */
    short CANON_CS_QUALITY_NORMAL    = 2;

    /**
     * Canon camera setting quality: fine.
     * This is one of the possible values for the
     * {@link CanonTags#CANON_CS_QUALITY} metadata tag.
     */
    short CANON_CS_QUALITY_FINE      = 3;

    /**
     * Canon camera setting quality: RAW.
     * This is one of the possible values for the
     * {@link CanonTags#CANON_CS_QUALITY} metadata tag.
     */
    short CANON_CS_QUALITY_RAW       = 4;

    /**
     * Canon camera setting quality: super-fine.
     * This is one of the possible values for the
     * {@link CanonTags#CANON_CS_QUALITY} metadata tag.
     */
    short CANON_CS_QUALITY_SUPERFINE = 5;

}
/* vim:set et sw=4 ts=4: */
