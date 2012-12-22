/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

/**
 * A <code>SubEXIFDirectory</code> is-an {@link EXIFDirectory} for holding EXIF
 * metadata information that's supposed to reside in an EXIF subIFD.  (Why?  I
 * don't know, but it seems to need to be that way.)
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class SubEXIFDirectory extends EXIFDirectory {

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;SubEXIF&quot;.
     */
    public String getName() {
        return "SubEXIF";
    }

}
/* vim:set et sw=4 ts=4: */
