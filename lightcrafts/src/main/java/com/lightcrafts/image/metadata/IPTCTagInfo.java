/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

/**
 * An <code>IPTCTagInfo</code> is-an <code>ImageMetaTagInfo</code> that adds
 * additional information for IPTC tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class IPTCTagInfo extends ImageMetaTagInfo {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Tag attribute: multi-line.
     * <p>
     * If a tag has this attribute, it allows a multi-lined value, e.g., for an
     * address.
     */
    public static final int IPTC_TAG_MULTILINE  = 0x0001;

    /**
     * Tag atrribute: multi-value.
     * <p>
     * If a tag has this attribute, it allows multiple values, e.g., keywords.
     */
    public static final int IPTC_TAG_MULTIVALUE = 0x0002;

    /**
     * Construct an <code>ImageMetaTagInfo</code>.
     *
     * @param id The tag's ID.
     * @param name The tag's name.
     * @param type The tag's {@link ImageMetaType}.
     * @param isChangeable Whether the tag is user-changeable.
     * @param tagAttributes A bit-mask specifying the tag attributes.
     */
    public IPTCTagInfo( int id, String name, ImageMetaType type,
                        boolean isChangeable, int tagAttributes ) {
        super( id, name, type, isChangeable );
        m_tagAttributes = tagAttributes;
    }

    /**
     * Gets the tag's attributes.
     *
     * @return Returns a bit-mask comprising the tag's attributes.
     */
    public int getAttributes() {
        return m_tagAttributes;
    }

    /**
     * Gets whether this tag allows a multi-lined value.
     *
     * @return Returns <code>true</code> only if it does.
     */
    public boolean isMultiLined() {
        return (m_tagAttributes & IPTC_TAG_MULTILINE) != 0;
    }

    /**
     * Gets whether this tag allows multiple values.
     *
     * @return Returns <code>true</code> only if it does.
     */
    public boolean isMultiValued() {
        return (m_tagAttributes & IPTC_TAG_MULTIVALUE) != 0;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final int m_tagAttributes;
}
/* vim:set et sw=4 ts=4: */
