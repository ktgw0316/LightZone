/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import java.io.IOException;

import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.utils.xml.XMLException;

/**
 * An <code>BooleanExportOption</code> is-an {@link ImageExportOption} for
 * storing an option that has a boolean value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class BooleanExportOption extends ImageExportOption {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public final boolean equals( Object o ) {
        if ( !(o instanceof BooleanExportOption) )
            return false;
        final BooleanExportOption b = (BooleanExportOption)o;
        return b.getName().equals( getName() ) && b.getValue() == getValue();
    }

    /**
     * Get the boolean value of this option.
     *
     * @return Returns said value.
     */
    public boolean getValue() {
        return m_value;
    }

    /**
     * Sets the boolean value of this option.
     *
     * @param newValue The new value.
     */
    public void setValue( boolean newValue ) {
        m_value = newValue;
    }

    /**
     * Sets the boolean value of this option.
     *
     * @param newValue The new value.  The boolean value is parsed from the
     * string.  Only the string "true" (ignoring case) and any string that can
     * be parsed as an integer and said integer is non-zero are considered
     * "true"; everything else is "false".
     */
    public void setValue( String newValue ) {
        if ( m_value = Boolean.valueOf( newValue ).booleanValue() )
            return;
        try {
            m_value = Integer.parseInt( newValue ) != 0;
        }
        catch ( NumberFormatException e ) {
            m_value = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void save( XmlNode node ) {
        node = node.addChild( getName() );
        String value = Boolean.toString( getValue() );
        node.setAttribute( ValueTag, value );
    }

    /**
     * {@inheritDoc}
     */
    public void restore( XmlNode node ) throws XMLException {
        node = node.getChild( getName() );
        m_value =
            Boolean.valueOf( node.getAttribute( ValueTag ) ).booleanValue();
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
     * Construct an <code>BooleanExportOption</code>.
     *
     * @param name The name of this option.
     * @param defaultValue The default value for this option.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    protected BooleanExportOption( String name, boolean defaultValue,
                                   ImageExportOptions options ) {
        super( name, options );
        m_value = defaultValue;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The boolean value of this option.
     */
    private boolean m_value;
}
/* vim:set et sw=4 ts=4: */
