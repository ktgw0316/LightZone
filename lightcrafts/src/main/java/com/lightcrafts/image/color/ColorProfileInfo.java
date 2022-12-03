/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.color;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;

import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * A <code>ColorProfileInfo</code> contains information for an ICC profile.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ColorProfileInfo implements Comparable {

    ////////// public ////////////////////////////////////////////////////////

    /**
     * Construct a <code>ColorProfileInfo</code>.
     *
     * @param name The name of the color profile.
     * @param path The full path to the color profile.
     */
    public ColorProfileInfo( String name, String path ) {
        if ( name == null || path == null )
            throw new IllegalArgumentException();
        m_name = name;
        m_path = path;
    }

    /**
     * Arrange the given color profiles for display in a menu.
     *
     * @param profiles A {@link Collection} of {@link ColorProfileInfo} objects
     * to arrange.
     * @return Returns a {@link List} of the color profiles arranged for
     * display in a menu.  Entries that are <code>null</code> indicate where a
     * menu seperator should be.
     */
    public static List<ColorProfileInfo>
    arrangeForMenu( Collection<ColorProfileInfo> profiles ) {
        if ( profiles == null ) {
            return new ArrayList<ColorProfileInfo>(); // empty list
        }
        //
        // Must sort the profiles by path first.
        //
        final TreeMap<String,String> sortedByPath =
            new TreeMap<String, String>();
        for ( ColorProfileInfo cpi : profiles ) {
            String ppath = cpi.getPath();
            String pname = cpi.getName();
            if ( pname.length() == 0 )
                pname = new File(ppath).getName();

            sortedByPath.put( ppath, pname );
        }

        //
        // Then clump the profiles by path.
        //
        final ArrayList<ColorProfileInfo> result =
            new ArrayList<ColorProfileInfo>();
        File currentDirectory = null;
        for ( Map.Entry<String,String> me : sortedByPath.entrySet() ) {
            final String path = me.getKey();
            final File file = new File( path );
            final File directory = file.getParentFile();
            if ( !directory.equals( currentDirectory ) ) {
                if ( currentDirectory != null )
                    result.add( null );
                currentDirectory = directory;
            }
            final String name = me.getValue();
            result.add( new ColorProfileInfo( name, path ) );
        }

        //
        // Finally sort each clump.
        //
        int start = -1;
        for ( int i = 0; i < result.size() - 1; ++i ) {
            if ( result.get( i ) == null ) {
                start = i;
                continue;
            }
            int end = result.size();
            for ( int j = i + 1; j < end; ++j )
                if ( result.get( j ) == null ) {
                    end = j;
                    break;
                }
            Collections.sort( result.subList( start + 1, end ) );
            start = end = i;
        }

        return result;
    }

    /**
     * Compares this <code>ColorProfileInfo</code> to another based on their
     * names.
     *
     * @param o The other object to compare to (presumed to be another
     * <code>ColorProfileInfo</code>).
     * @return Returns a negative integer, zero, or a positive integer as the
     * name of this <code>ColorProfileInfo</code> is less than, equal to, or
     * greater than the name of the given <code>ColorProfileInfo</code>.
     * @see #getName()
     */
    public int compareTo( Object o ) {
        if ( o == null )
            return 1;
        final ColorProfileInfo c = (ColorProfileInfo)o;
        return getName().toUpperCase().compareTo( c.getName().toUpperCase() );
    }

    /**
     * Checks for equality.  Two {@link ColorProfileInfo} objects are equal
     * only if their names and paths are equal.
     *
     * @param o The {@link Object} to compare to.
     * @return Returns <code>true</code> only if the given object is also a
     * <code>ColorProfileInfo</code> and it's equal to this one.
     */
    public boolean equals( Object o ) {
        if ( o instanceof ColorProfileInfo ) {
            final ColorProfileInfo c = (ColorProfileInfo)o;
            return m_name.equals( c.m_name ) && m_path.equals( c.m_path );
        }
        return false;
    }

    /**
     * Gets an {@link ICC_Profile} by name from the set of export profiles.
     *
     * @param name The name of the profile.
     * @return Returns the corresponding {@link ICC_Profile} or
     * <code>null</code> if no export profile has the fiven name.
     */
    public static ICC_Profile getExportICCProfileFor( String name ) {
        final Collection<ColorProfileInfo> exportProfiles =
            Platform.getPlatform().getExportProfiles();
        if (exportProfiles != null) {
            for ( ColorProfileInfo cpi : exportProfiles ) {
                if ( cpi.getName().equals( name ) ) {
                    return cpi.getICCProfile();
                }
            }
        }
        return null;
   }

    /**
     * Gets the actual {@link ICC_Profile} of this
     * <code>ColorProfileInfo</code>.
     *
     * @return Returns said {@link ICC_Profile}.
     */
    public synchronized ICC_Profile getICCProfile() {
        if ( m_iccProfile == null ) {
            InputStream in = null;
            try {
                in = new FileInputStream( m_path );
                m_iccProfile = ICC_Profile.getInstance( in );
            }
            catch ( IOException e ) {
                System.err.println( "Bad color profile path: " + m_path );
            }
            finally {
                try {
                    if ( in != null )
                        in.close();
                }
                catch ( IOException e ) {
                    // nothing
                }
            }
        }
        return m_iccProfile;
    }

    /**
     * Gets the name of this <code>ColorProfileInfo</code>.
     *
     * @return Returns said name.
     */
    public String getName() {
        return m_name;
    }

    /**
     * Gets the name of an {@link ICC_Profile}.
     *
     * @param profile The {@link ICC_Profile} to get the name of.
     * @return Returns the color profile name or <code>null</code> if the name
     * could not be determined.
     */
    public static String getNameOf( ICC_Profile profile ) {
        final byte[] descData =
            profile.getData( ICC_Profile.icSigProfileDescriptionTag );
        if ( descData == null )
            return null;
        final ByteBuffer buf = ByteBuffer.wrap(
            descData, PROFILE_NAME_LENGTH_OFFSET,
            descData.length - PROFILE_NAME_LENGTH_OFFSET
        );
        return ByteBufferUtil.getString( buf, buf.get() - 1, "UTF-8" );
    }

    /**
     * Gets the path of this <code>ColorProfileInfo</code>.
     *
     * @return Returns said path.
     */
    public String getPath() {
        return m_path;
    }

    /**
     * Gets the hash-code of this <code>ColorProfileInfo</code>.
     *
     * @return Returns said hash-code.
     */
    public int hashCode() {
        return m_name.hashCode() ^ m_path.hashCode();
    }

    /**
     * Gets the string representation of this <code>ColorProfileInfo</code>.
     *
     * @return Returns said string.
     */
    public String toString() {
        return m_name;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Inside the ICC profile tag signature 'desc', the 11th byte is the length
     * (plus 1) of the profile name.
     */
    private static final int PROFILE_NAME_LENGTH_OFFSET = 11;

    private ICC_Profile m_iccProfile;
    private final String m_name;
    private final String m_path;
}
/* vim:set et sw=4 ts=4: */
