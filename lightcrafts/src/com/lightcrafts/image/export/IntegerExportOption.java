/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import java.io.IOException;

/**
 * An <code>IntegerExportOption</code> is-an {@link ImageExportOption} for
 * storing an option that has an integer value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class IntegerExportOption extends ImageExportOption {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public final boolean equals( Object o ) {
        if ( !(o instanceof IntegerExportOption) )
            return false;
        final IntegerExportOption i = (IntegerExportOption)o;
        return i.getName().equals( getName() ) && i.getValue() == getValue();
    }

    /**
     * Get the integer value of this option.
     *
     * @return Returns said value.
     */
    public int getValue() {
        return m_value;
    }

    /**
     * Checks whether the given value is legal.
     *
     * @param value The value to check.
     * @return Returns <code>true</code> only if the value is valid.
     */
    public abstract boolean isLegalValue( int value );

    /**
     * Sets the integer value of this option.
     *
     * @param newValue The new value.
     */
    public final void setValue( int newValue ) {
        /* temporary removal
        if ( !isLegalValue( newValue ) )
            throw new IllegalArgumentException( Integer.toString( newValue ) );
        */
        m_value = newValue;
    }

    /**
     * Sets the integer value of this option.
     *
     * @param newValue The new value.  The integer value is parsed from the
     * string.
     * @throws NumberFormatException if the new value does not contain a
     * parsable integer.
     */
    public final void setValue( String newValue ) {
        setValue( Integer.parseInt( newValue ) );
    }

    @Deprecated
    public void save( XmlNode node ) {
        node = node.addChild( getName() );
        final String value = Integer.toString( getValue() );
        node.setAttribute( ValueTag, value );
    }

    @Deprecated
    public void restore( XmlNode node ) throws XMLException {
        node = node.getChild( getName() );
        m_value = Integer.parseInt( node.getAttribute( ValueTag ) );
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
     * Construct an <code>IntegerExportOption</code>.
     *
     * @param name The name of this option.
     * @param defaultValue The default value for this option.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    protected IntegerExportOption( String name, int defaultValue,
                                   ImageExportOptions options ) {
        super( name, options );
        setValue( defaultValue );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The integer value of this option.
     */
    private int m_value;
}
/* vim:set et sw=4 ts=4: */
