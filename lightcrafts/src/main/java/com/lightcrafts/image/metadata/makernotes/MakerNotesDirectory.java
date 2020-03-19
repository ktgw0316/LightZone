/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.providers.LensProvider;
import com.lightcrafts.image.metadata.providers.OrientationProvider;
import com.lightcrafts.image.metadata.providers.ImageMetadataProvider;
import com.lightcrafts.image.metadata.values.ImageMetaValue;

/**
 * A <code>MakerNotesDirectory</code> is-an {@link ImageMetadataDirectory} that
 * currently only serves as a common base class for all maker notes metadata
 * directories.  This allows <code>instanceof</code> to be used to determine if
 * a directory is for maker notes.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public abstract class MakerNotesDirectory extends ImageMetadataDirectory implements
        LensProvider {

    /**
     * Gets the priority of this directory for providing the metadata supplied
     * by implementing the given provider interface.
     * <p>
     * By default, the priority for {@link OrientationProvider} is lower
     * because the orientation from EXIF/TIFF metadata (when merged from an XMP
     * file), must take priority.
     *
     * @param provider The provider interface to get the priority for.
     * @return Returns said priority.
     */
    @Override
    protected int getProviderPriorityFor(
        Class<? extends ImageMetadataProvider> provider )
    {
        return (provider == OrientationProvider.class)
                ? PROVIDER_PRIORITY_MIN
                : PROVIDER_PRIORITY_DEFAULT;
    }

    abstract protected ImageMetaValue getLongFocalValue();
    abstract protected ImageMetaValue getShortFocalValue();
    abstract protected ImageMetaValue getMaxApertureValue();

    protected ImageMetaValue getLensNamesValue() {
        return null;
    }

    protected ImageMetaValue getFocalUnitsPerMMValue() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLens() {
        final ImageMetaValue shortFocalValue = getShortFocalValue();
        String shortFocalLabel = hasTagValueLabelFor( shortFocalValue );
        if (shortFocalLabel != null)
            shortFocalLabel = shortFocalLabel.replaceAll("\\.0$", "");

        final ImageMetaValue longFocalValue = getLongFocalValue();
        String longFocalLabel = hasTagValueLabelFor( longFocalValue );
        if (longFocalLabel != null)
            longFocalLabel = longFocalLabel.replaceAll("\\.0$", "");

        final ImageMetaValue maxApertureValue = getMaxApertureValue();
        String maxApertureLabel = hasTagValueLabelFor( maxApertureValue );
        if (maxApertureLabel != null)
            maxApertureLabel = maxApertureLabel.replaceAll("\\.0$", "");

        final ImageMetaValue lensNamesValue = getLensNamesValue();
        final String lensNamesLabel = hasTagValueLabelFor( lensNamesValue );

        if ( lensNamesLabel != null ) {
            // Check third party lenses
            final String[] names = lensNamesLabel.split(" or ");
            if (names.length == 1) {
                return names[0];
            }

            for (String name : names) {
                String focal = null;
                String maxAperture = null;
                final String[] segments = lensNamesLabel.split(" ");

                for (String segment : segments) {
                    if(segment.endsWith("mm")) {
                        focal = segment.substring(0, segment.length() - 2);
                    }
                    else if (segment.startsWith("F/")) {
                        maxAperture = segment.substring(2, segment.length()).split("-", 2)[0];
                    }
                }

                if (focal == null || maxAperture == null)
                    continue;

                final String[] focals = focal.split("-", 2);
                if (focals[0].equals(shortFocalLabel) &&
                    (focals.length == 1 || focals[1].equals(longFocalLabel))) {
                    if (maxAperture.equals(maxApertureLabel))
                        return name;
                }
            }
        }
        return makeLensLabelFrom(
            shortFocalValue,
            longFocalValue,
            getFocalUnitsPerMMValue()
        );
    }
}
/* vim:set et sw=4 ts=4: */
