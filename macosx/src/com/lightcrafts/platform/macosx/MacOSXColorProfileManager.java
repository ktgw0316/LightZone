/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.util.ArrayList;
import java.util.Collection;

import com.lightcrafts.utils.ColorProfileInfo;

/**
 * A <code>MacOSXColorProfileManager</code> is a class that is used to get
 * various color profiles.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MacOSXColorProfileManager {

    ////////// public /////////////////////////////////////////////////////////

    /** An abstract profile. */
    public static final int CM_ABSTRACT_CLASS       = 0x61627374; // 'abst'

    /** A color space profile. */
    public static final int CM_COLORSPACE_CLASS     = 0x73706163; // 'spac'

    /** A display device profile defined for a monitor. */
    public static final int CM_DISPLAY_CLASS        = 0x6D6E7472; // 'mntr'

    /** An input device profile defined for a scanner. */
    public static final int CM_INPUT_CLASS          = 0x73636E72; // 'scnr'

    /** A device link profile. */
    public static final int CM_LINK_CLASS           = 0x6C696E6B; // 'link'

    /** A named color space profile. */
    public static final int CM_NAMED_COLOR_CLASS    = 0x6E6D636C; // 'nmcl'

    /** An output device profile defined for a printer. */
    public static final int CM_OUTPUT_CLASS         = 0x70727472; // 'prtr'

    /**
     * Get all color profiles for a given class.
     *
     * @param profileClassID The color profile class ID.
     * @return Returns a {@link Collection} of {@link ColorProfileInfo}
     * objects.
     */
    public static Collection<ColorProfileInfo>
    getProfilesFor( int profileClassID ) {
        final String[] temp = searchProfilesForImpl( profileClassID );
        if ( temp == null )
            return null;
        final Collection<ColorProfileInfo> result =
            new ArrayList<ColorProfileInfo>();
        for ( int i = 0; i < temp.length - 1; i += 2 )
            result.add( new ColorProfileInfo( temp[i], temp[i+1] ) );
        return result;
    }

    /**
     * Gets the path to the system display profile.
     *
     * @return Returns said path.
     */
    public static native String getSystemDisplayProfilePath();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Search color profiles for a given class.
     *
     * @param profileClassID The color profile class ID.
     * @return Returns an array of {@link String} such that, for even
     * <i>ith</i> elements, element <i>i</i> is the profile name and element
     * <i>i+1</i> is the profile path.
     */
    private static native String[] searchProfilesForImpl( int profileClassID );

    static {
        System.loadLibrary( "MacOSX" );
    }
}
/* vim:set et sw=4 ts=4: */
