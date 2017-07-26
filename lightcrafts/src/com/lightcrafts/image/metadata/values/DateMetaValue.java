/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.utils.LCArrays;
import com.lightcrafts.utils.TextUtil;
import com.lightcrafts.utils.xml.XMLUtil;

import static com.lightcrafts.image.metadata.ImageMetaType.META_DATE;
import static com.lightcrafts.image.metadata.XMPConstants.*;

/**
 * A <code>DateMetaValue</code> is-an {@link ImageMetaValue} for holding a date
 * metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class DateMetaValue extends ImageMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>DateMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public DateMetaValue() {
        // do nothing
    }

    /**
     * Construct a <code>DateMetaValue</code>.
     *
     * @param values The array of values.
     */
    public DateMetaValue( Date... values ) {
        m_value = values;
    }

    /**
     * Construct a <code>DataMetaValue</code>.
     *
     * @param value The number of milliseconds since epoch.
     */
    public DateMetaValue( long value ) {
        this( new Date( value ) );
    }

    /**
     * Construct a <code>DateMetaValue</code>.
     *
     * @param values The array of values.
     * @throws IllegalArgumentException if one of the strings can not be
     * parsed as a date.
     */
    public DateMetaValue( String... values ) {
        setValuesImpl( values );
    }

    /**
     * {@inheritDoc}
     */
    public DateMetaValue clone() {
        final DateMetaValue copy = (DateMetaValue)super.clone();
        copy.m_value = m_value.clone();
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Object o ) {
        if ( o instanceof DateMetaValue ) {
            final DateMetaValue rightVal = (DateMetaValue)o;
            final Date leftDate = getDateValue();
            final Date rightDate = rightVal.getDateValue();
            return leftDate.compareTo( rightDate );
        }
        return super.compareTo( o );
    }

    /**
     * Get the first native {@link Date} array value.
     *
     * @return Returns said value.
     */
    public Date getDateValue() {
        return getDateValueAt(0);
    }

    /**
     * Gets the {@link Date} value at the given index.
     *
     * @param index The index of the value to get.
     * @return Returns said value.
     */
    public Date getDateValueAt( int index ) {
        return m_value[ index ];
    }

    /**
     * Get the native {@link Date} array value.
     *
     * @return Returns said array.
     */
    public Date[] getDateValues() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    public long getLongValueAt(int index) {
        return getDateValueAt(index).getTime();
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_DATE</code>.
     */
    public ImageMetaType getType() {
        return META_DATE;
    }

    /**
     * {@inheritDoc}
     */
    public int getValueCount() {
        return m_value != null ? m_value.length : 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLegalValue( String value ) {
        try {
            parseValue( value );
            return true;
        }
        catch ( IllegalArgumentException e ) {
            return false;
        }
    }

    /**
     * Sets the {@link Date} value at the given index.
     *
     * @param newValue The new value.
     * @param index The index to set the value of.
     */
    public synchronized void setDateValueAt( Date newValue, int index ) {
        checkIsEditable();
        if ( m_value == null )
            m_value = new Date[ index + 1 ];
        else if ( index >= m_value.length )
            m_value = (Date[])LCArrays.resize( m_value, index + 1 );
        m_value[ index ] = newValue;
        dirty();
    }

    /**
     * {@inheritDoc}
     */
    public void setLongValue( long newValue ) {
        setDateValueAt( new Date( newValue ), 0 );
    }

    /**
     * {@inheritDoc}
     */
    public Element toXMP( Document xmpDoc, String nsURI, String prefix ) {
        final String tagName = getTagName();
        if ( tagName == null )
            return null;
        final Element tagElement =
            xmpDoc.createElementNS( nsURI, prefix + ':' + tagName );
        final Date[] values = getDateValues();
        if ( values.length == 1 )
            XMLUtil.setTextContentOf(
                tagElement,
                TextUtil.dateFormat( ISO_8601_DATE_FORMAT, m_value[0] )
            );
        else {
            final Element seqElement = XMLUtil.addElementChildTo(
                tagElement, XMP_RDF_NS, XMP_RDF_PREFIX + ":Seq"
            );
            for ( Date value : values ) {
                final Element listItem = XMLUtil.addElementChildTo(
                    seqElement, XMP_RDF_NS, XMP_RDF_PREFIX + ":li"
                );
                XMLUtil.setTextContentOf(
                    listItem,
                    TextUtil.dateFormat( ISO_8601_DATE_FORMAT, value )
                );
            }
        }
        return tagElement;
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new Date[ length ];
        for ( int i = 0; i < length; ++i )
            m_value[i] = new Date( in.readLong() );
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the date values (<code>long</code>) where each is the number of
     * milliseconds since epoch.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( Date value : m_value )
            out.writeLong( value.getTime() );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Parse and append a new value.
     *
     * @param newValue The new value.
     * @throws IllegalArgumentException if the {@link String} does not contain
     * a parsable date.
     */
    protected void appendValueImpl( String newValue ) {
        final Date newDate = parseValue( newValue );
        if ( m_value == null )
            m_value = new Date[] { newDate };
        else {
            m_value = (Date[])LCArrays.resize( m_value, m_value.length + 1 );
            m_value[ m_value.length - 1 ] = newDate;
        }
    }

    /**
     * Gets the values as an array of {@link String}.
     *
     * @return Returns said array.
     */
    protected String[] getValuesImpl() {
        if ( m_value == null )
            return null;
        final String[] value = new String[ m_value.length ];
        for ( int i = 0; i < m_value.length; ++i )
            value[i] = TextUtil.dateFormat( m_canonicalDateFormat, m_value[i] );
        return value;
    }

    /**
     * Parse and set the values.
     *
     * @param newValue The array of new values.
     * @throws IllegalArgumentException if any one of the {@link String}s do
     * not contain a parsable date.
     */
    protected void setValuesImpl( String[] newValue ) {
        if ( m_value == null || m_value.length != newValue.length )
            m_value = new Date[ newValue.length ];
        for ( int i = 0; i < newValue.length; ++i )
            m_value[i] = parseValue( newValue[i] );
    }

    /**
     * Convert this value to its {@link String} representation.  Multiple
     * values are separated by commas.
     *
     * @return Returns said string.
     */
    protected String toStringImpl() {
        if ( m_value == null )
            return null;
        final StringBuilder sb = new StringBuilder();
        boolean comma = false;
        for ( Date value : m_value ) {
            if ( !comma )
                comma = true;
            else
                sb.append( ',' );
            sb.append(
                TextUtil.dateFormat( m_canonicalDateFormat, value ).trim()
            );
        }
        return sb.toString();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Parse a date from a {@link String}.
     *
     * @param value The {@link String} to parse.
     * @return Returns a {link Date}.
     * @throws IllegalArgumentException if the {@link String} does not contain
     * a parsable date.
     */
    private static Date parseValue( String value ) {
        //
        // Try parsing the value using all the expected date formats until one
        // parses successfully.
        //
        for ( SimpleDateFormat format : m_dateFormats )
            try {
                return format.parse( value );
            }
            catch ( ParseException e ) {
                // ignore
            }
        throw new IllegalArgumentException();
    }

    /**
     * The date format to use by default because this is apparently the
     * format that cameras use for their date metadata.
     */
    private static final SimpleDateFormat m_canonicalDateFormat =
        new SimpleDateFormat( "yyyy:MM:dd HH:mm:ss" );

    /**
     * Date formats to be able to parse.
     */
    private static final SimpleDateFormat[] m_dateFormats = {
        ISO_8601_DATE_FORMAT,
        m_canonicalDateFormat,
        new SimpleDateFormat( "EEE MMM dd HH:mm:ss zzz yyyy" ),
        new SimpleDateFormat( "yyyyMMdd" ), // IPTC date format
        new SimpleDateFormat( "yyyy-MM-dd" ),
        new SimpleDateFormat( "yyyy/MM/dd" ),
        new SimpleDateFormat( "dd-MMM-yyyy" ),
        new SimpleDateFormat( "dd/MMM/yyyy" ),
        new SimpleDateFormat( "MMM dd, yyyy" )

    };

    private Date[] m_value;
}
/* vim:set et sw=4 ts=4: */
