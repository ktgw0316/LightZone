/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.providers.LensProvider;
import com.lightcrafts.image.metadata.providers.OrientationProvider;
import com.lightcrafts.image.metadata.providers.ImageMetadataProvider;

/**
 * A <code>MakerNotesDirectory</code> is-an {@link ImageMetadataDirectory} that
 * currently only serves as a common base class for all maker notes metadata
 * directories.  This allows <code>instanceof</code> to be used to determine if
 * a directory is for maker notes.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public abstract class MakerNotesDirectory extends ImageMetadataDirectory {

    /**
     * Gets the priority of this directory for providing the metadata supplied
     * by implementing the given provider interface.
     * <p>
     * By default, the priority for maker notes directories is higher than
     * {@link ImageMetadataDirectory#getProviderPriorityFor(Class)} because
     * they have more detailed metadata about a given image.
     * <p>
     * However, an exception is made for {@link OrientationProvider} and
     * {@link LensProvider} because
     * orientation from EXIF/TIFF metadata (when merged from an XMP file), must
     * take priority.
     *
     * @param provider The provider interface to get the priority for.
     * @return Returns said priority.
     */
    protected int getProviderPriorityFor(
        Class<? extends ImageMetadataProvider> provider )
    {
        if (provider == OrientationProvider.class)
            return 0;
        if (provider == LensProvider.class)
            return 5;
        return 100;
    }

}
/* vim:set et sw=4 ts=4: */
