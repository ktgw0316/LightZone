/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2023-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.values;

import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.utils.xml.XMLUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static com.lightcrafts.image.metadata.ImageMetaType.META_DATE;
import static com.lightcrafts.image.metadata.XMPConstants.XMP_RDF_NS;
import static com.lightcrafts.image.metadata.XMPConstants.XMP_RDF_PREFIX;

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
    public DateMetaValue(LocalDateTime... values) {
        m_value = Arrays.asList(values);
    }

    /**
     * Construct a <code>DataMetaValue</code>.
     *
     * @param value The number of milliseconds since epoch.
     */
    public DateMetaValue(long value) {
        this(localDateTimeFrom(value));
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
    @Override
    public DateMetaValue clone() {
        final DateMetaValue copy = (DateMetaValue)super.clone();
        copy.m_value = List.copyOf(m_value);
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo( Object o ) {
        if (o instanceof DateMetaValue rightVal) {
            final var leftDate = getDateValue();
            final var rightDate = rightVal.getDateValue();
            return leftDate.compareTo( rightDate );
        }
        return super.compareTo( o );
    }

    /**
     * Get the first native {@link LocalDateTime} array value.
     *
     * @return Returns said value.
     */
    public LocalDateTime getDateValue() {
        return getDateValueAt(0);
    }

    /**
     * Gets the {@link LocalDateTime} value at the given index.
     *
     * @param index The index of the value to get.
     * @return Returns said value.
     */
    public LocalDateTime getDateValueAt( int index ) {
        return m_value.get(index);
    }

    /**
     * Get the native {@link LocalDateTime} array value.
     *
     * @return Returns said array.
     */
    public List<LocalDateTime> getDateValues() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLongValueAt(int index) {
        return epocMilliFrom(getDateValueAt(index));
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_DATE</code>.
     */
    @Override
    public ImageMetaType getType() {
        return META_DATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getValueCount() {
        return m_value.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * Sets the {@link LocalDateTime} value at the given index.
     *
     * @param newValue The new value.
     * @param index The index to set the value of.
     */
    public synchronized void setDateValueAt(LocalDateTime newValue, int index) {
        checkIsEditable();
        if (index >= m_value.size())
            m_value.addAll(Collections.nCopies(index + 1 - m_value.size(), null));
        m_value.set(index, newValue);
        dirty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLongValue( long newValue ) {
        setDateValueAt(localDateTimeFrom(newValue), 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMP( Document xmpDoc, String nsURI, String prefix ) {
        final String tagName = getTagName();
        if ( tagName == null )
            return null;
        final var formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        final Element tagElement =
            xmpDoc.createElementNS( nsURI, prefix + ':' + tagName );
        final var values = getDateValues();
        if (values.size() == 1) {
            XMLUtil.setTextContentOf(tagElement, m_value.get(0).format(formatter));
        } else {
            final Element seqElement = XMLUtil.addElementChildTo(
                    tagElement, XMP_RDF_NS, XMP_RDF_PREFIX + ":Seq"
            );
            values.forEach(value -> {
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
    @Override
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = LongStream.range(0, length)
                .map(i -> {
                    try {
                        return in.readLong();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .mapToObj(DateMetaValue::localDateTimeFrom)
                .collect(Collectors.toList());
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the date values (<code>long</code>) where each is the number of
     * milliseconds since epoch.
     */
    @Override
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( var value : m_value )
            out.writeLong(epocMilliFrom(value));
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Parse and append a new value.
     *
     * @param newValue The new value.
     * @throws IllegalArgumentException if the {@link String} does not contain
     * a parsable date.
     */
    @Override
    protected void appendValueImpl( String newValue ) {
        final LocalDateTime newDateTime = parseValue(newValue);
        m_value.add(newDateTime);
    }

    /**
     * Gets the values as an array of {@link String}.
     *
     * @return Returns said array.
     */
    @Override
    protected String @NotNull [] getValuesImpl() {
        return m_value.stream()
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
    @Override
    protected void setValuesImpl( String[] newValue ) {
        m_value = Arrays.stream(newValue)
                .map(DateMetaValue::parseValue)
                .collect(Collectors.toList());
    }

    /**
     * Convert this value to its {@link String} representation.  Multiple
     * values are separated by commas.
     *
     * @return Returns said string.
     */
    @Override
    protected String toStringImpl() {
        return m_value.stream()
                .map(v -> v.format(m_canonicalDateFormatter).trim())
                .collect(Collectors.joining(","));
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Parse a date from a {@link String}.
     *
     * @param value The {@link String} to parse.
     * @return Returns a {link LocalDateTime}.
     * @throws IllegalArgumentException if the {@link String} does not contain
     * a parsable date.
     */
    private static @NotNull LocalDateTime parseValue(String value) {
        //
        // Try parsing the value using all the expected date formats until one
        // parses successfully.
        //
        for (final var formatter : m_dateFormatters) {
            try {
                return LocalDateTime.parse(value, formatter);
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

    private static @NotNull LocalDateTime localDateTimeFrom(long epochMillis) {
        final var instant = Instant.ofEpochMilli(epochMillis);
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static long epocMilliFrom(@NotNull LocalDateTime value) {
        return value.toInstant(ZoneOffset.UTC).toEpochMilli();
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

    @NotNull
    private List<LocalDateTime> m_value = new ArrayList<>();
}
/* vim:set et sw=4 ts=4: */
