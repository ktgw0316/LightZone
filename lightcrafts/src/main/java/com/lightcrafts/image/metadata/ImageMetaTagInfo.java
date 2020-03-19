/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.metadata.values.ImageMetaValue;

import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * An <code>ImageMetaTagInfo</code> contains information about a particular
 * image metadata tag.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class ImageMetaTagInfo implements Comparable {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>ImageMetaTagInfo</code>.
     *
     * @param id The tag's ID.
     * @param name The tag's name.
     * @param type The tag's {@link ImageMetaType}.
     * @param isChangeable Whether the tag is user-changeable.
     */
    public ImageMetaTagInfo( int id, String name, ImageMetaType type,
                             boolean isChangeable ) {
        m_id = id;
        m_isChangeable = isChangeable;
        m_name = name;
        m_type = type;
    }

    /**
     * Compares this <code>ImageMetaTagInfo</code> to another object.
     *
     * @param o The object, presumed to be another
     * <code>ImageMetaTagInfo</code>, to compare to.
     * @return Returns a negative integer, zero, or a positive integer as this
     * <code>ImageMetaTagInfo</code>'s ID is less than, equal to, or greater
     * than the other <code>ImageMetaTagInfo</code>'s ID.
     * @throws IllegalArgumentException if the other object is not an
     * <code>ImageMetaTagInfo</code>.
     */
    public int compareTo( Object o ) {
        if ( o instanceof ImageMetaTagInfo ) {
            final ImageMetaTagInfo rightTagInfo = (ImageMetaTagInfo)o;
            return rightTagInfo.getID() - getID();
        }
        throw new IllegalArgumentException(
            "Can not compare an ImageMetaTagInfo to a "
            + o.getClass().getName()
        );
    }

    /**
     * Create a new {@link ImageMetaValue} having this tag's ID and type.
     *
     * @return Returns said {@link ImageMetaValue}.
     */
    public ImageMetaValue createValue() {
        final ImageMetaValue value = ImageMetaValue.create( m_type );
        value.setIsChangeable( m_isChangeable );
        value.setOwningTagID( m_id );
        return value;
    }

    /**
     * Compares this <code>ImageMetaTagInfo</code> to another object for
     * equality.
     *
     * @param o The {@link Object} to compare to.
     * @return Returns <code>true</code> only if the other object is also an
     * <code>ImageMetaTagInfo</code> and its ID, name, and type are equal to
     * that of this <code>ImageMetaTagInfo</code>.
     */
    public boolean equals( Object o ) {
        if ( o == this )
            return true;
        if ( !(o instanceof ImageMetaTagInfo) )
            return false;
        final ImageMetaTagInfo t = (ImageMetaTagInfo)o;
        return  m_id == t.m_id && m_name.equals( t.m_name ) &&
                m_type == t.m_type;
    }

    /**
     * Gets the ID of this tag.
     *
     * @return Returns said ID.
     */
    public int getID() {
        return m_id;
    }

    /**
     * Gets this metadata tag's name.
     *
     * @return Returns said tag name.
     */
    public String getName() {
        return m_name;
    }

    /**
     * Gets this metadata tag's component size in bytes.
     *
     * @return Returns said size.
     */
    public int getSize() {
        switch ( m_type ) {
            case META_DATE:
                return 1;
            case META_UNKNOWN:
                return 0;
            default:
                return TIFF_FIELD_SIZE[ m_type.getTIFFConstant() ];
        }
    }

    /**
     * Gets this metadata tag's {@link ImageMetaType}.
     *
     * @return Returns said type.
     */
    public ImageMetaType getType() {
        return m_type;
    }

    /**
     * Gets this object's hash code.
     *
     * @return Returns said hash code.
     */
    public int hashCode() {
        return m_id;
    }

    /**
     * Gets whether this metadata tag is user-changeable.
     *
     * @return Returns <code>true</code> only if it is changeable.
     */
    public boolean isChangeable() {
        return m_isChangeable;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final boolean m_isChangeable;
    private final int m_id;
    private final String m_name;
    private final ImageMetaType m_type;
}
/* vim:set et sw=4 ts=4: */
