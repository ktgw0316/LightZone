/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.metadata.providers.ImageMetadataProvider;

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
    @Override
    public String getName() {
        return "SubEXIF";
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Gets the priority of this directory for providing the metadata supplied
     * by implementing the given provider interface.
     * <p>
     * The priority is guaranteed to be higher than the priority for
     * {@link EXIFDirectory}.
     *
     * @param p The provider interface to get the priority for.
     * @return Returns said priority.
     */
    @Override
    protected int getProviderPriorityFor(Class<? extends ImageMetadataProvider> p) {
        return super.getProviderPriorityFor(p) + 1;
    }
}
/* vim:set et sw=4 ts=4: */
