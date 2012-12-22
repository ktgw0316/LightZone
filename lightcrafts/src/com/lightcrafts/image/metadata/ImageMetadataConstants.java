/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

/**
 * An <code>ImageMetadataConstants</code> defines some constants for metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ImageMetadataConstants {

    /**
     * The maximum "sane" number of metadata directory entries.
     */
    int DIRECTORY_ENTRY_MAX_SANE_COUNT = 256;

    /**
     * The maximum "sane" size (in bytes) of a metadata entry.
     */
    int DIRECTORY_ENTRY_MAX_SANE_SIZE = 1024 * 128;

}
/* vim:set et sw=4 ts=4: */
