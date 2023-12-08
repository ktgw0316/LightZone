/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2023-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.values;

import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.utils.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    public DateMetaValue(ZonedDateTime... values) {
        m_value = values;
    }

    /**
     * Construct a <code>DataMetaValue</code>.
     *
     * @param value The number of milliseconds since epoch.
     */
    public DateMetaValue(long value) {
        this(zonedDateTimeFromEpochMillis(value));
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
        if (o instanceof DateMetaValue rightVal) {
            final var leftDate = getDateValue();
            final var rightDate = rightVal.getDateValue();
            return leftDate.compareTo( rightDate );
        }
        return super.compareTo( o );
    }

    /**
     * Get the first native {@link ZonedDateTime} array value.
     *
     * @return Returns said value.
     */
    public ZonedDateTime getDateValue() {
        return getDateValueAt(0);
    }

    /**
     * Gets the {@link ZonedDateTime} value at the given index.
     *
     * @param index The index of the value to get.
     * @return Returns said value.
     */
    public ZonedDateTime getDateValueAt( int index ) {
        return m_value[ index ];
    }

    /**
     * Get the native {@link ZonedDateTime} array value.
     *
     * @return Returns said array.
     */
    public ZonedDateTime[] getDateValues() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLongValueAt(int index) {
        return getDateValueAt(index).toInstant().toEpochMilli();
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
     * Sets the {@link ZonedDateTime} value at the given index.
     *
     * @param newValue The new value.
     * @param index The index to set the value of.
     */
    public synchronized void setDateValueAt(ZonedDateTime newValue, int index) {
        checkIsEditable();
        if ( m_value == null )
            m_value = new ZonedDateTime[index + 1];
        else if ( index >= m_value.length )
            m_value = Arrays.copyOf(m_value, index + 1);
        m_value[ index ] = newValue;
        dirty();
    }

    /**
     * {@inheritDoc}
     */
    public void setLongValue( long newValue ) {
        setDateValueAt(zonedDateTimeFromEpochMillis(newValue), 0);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXMP( Document xmpDoc, String nsURI, String prefix ) {
        final String tagName = getTagName();
        if ( tagName == null )
            return null;
        final var formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        final Element tagElement =
            xmpDoc.createElementNS( nsURI, prefix + ':' + tagName );
        final ZonedDateTime[] values = getDateValues();
        if (values.length == 1) {
            XMLUtil.setTextContentOf(tagElement, m_value[0].format(formatter));
        } else {
            final Element seqElement = XMLUtil.addElementChildTo(
                    tagElement, XMP_RDF_NS, XMP_RDF_PREFIX + ":Seq"
            );
            Arrays.stream(values).forEachOrdered(value -> {
                final Element listItem = XMLUtil.addElementChildTo(
                        seqElement, XMP_RDF_NS, XMP_RDF_PREFIX + ":li"
                );
                XMLUtil.setTextContentOf(listItem, value.format(formatter));
            });
        }
        return tagElement;
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new ZonedDateTime[length];
        for (int i = 0; i < length; i++) {
            m_value[i] = zonedDateTimeFromEpochMillis(in.readLong());
        }
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the date values (<code>long</code>) where each is the number of
     * milliseconds since epoch.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( ZonedDateTime value : m_value )
            out.writeLong( value.toInstant().toEpochMilli() );
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
        // TODO: This will be simplified if we declare the m_value as a non-null list.
        final ZonedDateTime newDateTime = parseValue( newValue );
        if ( m_value == null )
            m_value = new ZonedDateTime[] { newDateTime };
        else {
            m_value = Arrays.copyOf( m_value, m_value.length + 1 );
            m_value[ m_value.length - 1 ] = newDateTime;
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
        return Arrays.stream(m_value)
                .map(v -> v.format(m_canonicalDateFormatter))
                .toArray(String[]::new);
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
            m_value = new ZonedDateTime[ newValue.length ];
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
        return Arrays.stream(m_value)
                .map(v -> v.format(m_canonicalDateFormatter).trim())
                .collect(Collectors.joining(","));
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Parse a date from a {@link String}.
     *
     * @param value The {@link String} to parse.
     * @return Returns a {link ZonedDateTime}.
     * @throws IllegalArgumentException if the {@link String} does not contain
     * a parsable date.
     */
    private static @NotNull ZonedDateTime parseValue(String value) {
        //
        // Try parsing the value using all the expected date formats until one
        // parses successfully.
        //
        for (final var formatter : m_dateFormatters) {
            try {
                return ZonedDateTime.parse(value, formatter);
            } catch (DateTimeParseException e) {
                // ignore
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * The date format to use by default because this is apparently the
     * format that cameras use for their date metadata.
     */
    private static final DateTimeFormatter m_canonicalDateFormatter =
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    private static @NotNull ZonedDateTime zonedDateTimeFromEpochMillis(long epochMillis) {
        final var instant = Instant.ofEpochMilli(epochMillis);
        return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * Date formats to be able to parse.
     */
    private static final List<DateTimeFormatter> m_dateFormatters = Arrays.asList(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            m_canonicalDateFormatter,
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy"),
            DateTimeFormatter.BASIC_ISO_DATE, // IPTC date format
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MMM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MMM/yyyy"),
            DateTimeFormatter.ofPattern("MMM dd, yyyy")
    );

    private ZonedDateTime[] m_value;
}
/* vim:set et sw=4 ts=4: */
