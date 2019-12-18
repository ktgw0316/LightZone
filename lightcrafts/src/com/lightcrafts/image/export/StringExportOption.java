/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import java.io.IOException;

/**
 * An <code>StringExportOption</code> is-an {@link ImageExportOption} for
 * storing an option that has an string value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class StringExportOption extends ImageExportOption {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public final boolean equals( Object o ) {
        if ( !(o instanceof StringExportOption) )
            return false;
        final StringExportOption s = (StringExportOption)o;
        return  s.getName().equals( getName() ) &&
                s.getValue().equals( getValue() );
    }

    /**
     * Get the string value of this option.
     *
     * @return Returns said value.
     */
    public String getValue() {
        return m_value;
    }

    /**
     * Sets the string value of this option.
     *
     * @param newValue The new value.
     */
    public void setValue( String newValue ) {
        m_value = newValue;
    }

    @Deprecated
    public void save( XmlNode node ) {
        node = node.addChild( getName() );
        node.setAttribute( ValueTag, getValue() );
    }

    @Deprecated
    public void restore( XmlNode node ) throws XMLException {
        node = node.getChild( getName() );
        m_value = node.getAttribute( ValueTag );
    }

    /**
     * {@inheritDoc}
     */
    public void readFrom( ImageExportOptionReader r ) throws IOException {
        r.read( this );
    }

    /**
     * {@inheritDoc}
     */
    public void writeTo( ImageExportOptionWriter w ) throws IOException {
        w.write( this );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct an <code>StringExportOption</code>.
     *
     * @param name The name of this option.
     * @param defaultValue The default value for this option.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    protected StringExportOption( String name, String defaultValue,
                                  ImageExportOptions options ) {
        super( name, options );
        m_value = defaultValue;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The string value of this option.
     */
    private String m_value;
}
/* vim:set et sw=4 ts=4: */
