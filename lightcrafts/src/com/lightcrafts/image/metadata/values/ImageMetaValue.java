/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.utils.xml.XMLUtil;

import static com.lightcrafts.image.metadata.XMPConstants.*;

/**
 * An <code>ImageMetaValue</code> contains a metadata value extracted from an
 * image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class ImageMetaValue implements
    Cloneable, Comparable, Externalizable {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Parse and append a new value.
     *
     * @param newValue The new value.
     * @throws IllegalArgumentException if the {@link String} is an illegal
     * value for the given underlying type.
     */
    public final synchronized void appendValue( String newValue ) {
        if ( !m_isEditable )
            throw new IllegalStateException();
        appendValueImpl( newValue );
        m_isEdited = true;
        clearCache();
    }

    /**
     * Clears the changed flag.
     *
     * @see #isEdited()
     */
    public void clearEdited() {
        m_isEdited = false;
    }

    /**
     * {@inheritDoc}
     */
    public ImageMetaValue clone() {
        try {
            return (ImageMetaValue)super.clone();
        }
        catch ( CloneNotSupportedException e  ) {
            //
            // CloneNotSupportedException as a checked exception is dumb.
            //
            throw new IllegalStateException( e );
        }
    }

    /**
     * Compares this <code>ImageMetaValue</code> to another object.  By
     * default, a string comparison is done.
     *
     * @param o The object, presumed to be another <code>ImageMetaValue</code>,
     * to compare to.
     * @return Returns a negative integer, zero, or a positive integer as this
     * <code>ImageMetaValue</code> is less than, equal to, or greater than the
     * other <code>ImageMetaValue</code>.
     * @throws IllegalArgumentException if the other object is not an
     * <code>ImageMetaValue</code>.
     * @see #compareTo(String)
     */
    public int compareTo( Object o ) {
        if ( o instanceof ImageMetaValue ) {
            final ImageMetaValue rightValue = (ImageMetaValue)o;
            final String leftString = getStringValue();
            final String rightString = rightValue.getStringValue();
            if ( leftString == null )
                return rightString == null ? 0 : -1;
            if ( rightString == null )
                return 1;
            return leftString.compareTo( rightString );
        }
        throw new IllegalArgumentException(
            "Can not compare an ImageMetaValue to a " + o.getClass().getName()
        );
    }

    /**
     * Compares this <code>ImageMetaValue</code> to a {@link String}.  By
     * default, a string comparison is done.
     *
     * @param s The {@link String} to compare to.
     * @return Returns a negative integer, zero, or a positive integer as this
     * <code>ImageMetaValue</code> is less than, equal to, or greater than the
     * string.
     * @see #compareTo(Object)
     */
    public int compareTo( String s ) {
        final String leftString = getStringValue();
        if ( leftString == null )
            return s == null ? 0 : -1;
        return leftString.compareTo( s );
    }

    /**
     * Creates a new, empty instance of a class derived from
     * <code>ImageMetaValue</code> based on the given type.
     *
     * @param type The {@link ImageMetaType} of the instance to create.
     * @return Returns a new instance of the requested type.
     */
    public static ImageMetaValue create( ImageMetaType type ) {
        switch ( type ) {
            case META_DATE:
                return new DateMetaValue();
            case META_DOUBLE:
                return new DoubleMetaValue();
            case META_FLOAT:
                return new FloatMetaValue();
            case META_SBYTE:
                return new ByteMetaValue();
            case META_SLONG:
                return new LongMetaValue();
            case META_SRATIONAL:
                return new RationalMetaValue();
            case META_SSHORT:
                return new ShortMetaValue();
            case META_STRING:
                return new StringMetaValue();
            case META_UNDEFINED:
                return new UndefinedMetaValue();
            case META_UBYTE:
                return new UnsignedByteMetaValue();
            case META_ULONG:
                return new UnsignedLongMetaValue();
            case META_URATIONAL:
                return new UnsignedRationalMetaValue();
            case META_USHORT:
                return new UnsignedShortMetaValue();
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Gets this metadata value as a <code>byte</code>.
     *
     * @return Returns said value.
     */
    public final int getByteValue() {
        return (byte)getLongValue();
    }

    /**
     * Gets this metadata value as a <code>double</code>.
     *
     * @return Returns said value.
     */
    public double getDoubleValue() {
        return getLongValue();
    }

    /**
     * Gets this metadata value as a <code>float</code>.
     *
     * @return Returns said value.
     */
    public float getFloatValue() {
        return (float)getDoubleValue();
    }

    /**
     * Gets this metadata value as an <code>int</code>.
     *
     * @return Returns said value.
     */
    public final int getIntValue() {
        return getIntValueAt(0);
    }

    /**
     * Gets the metadata value at the given index as a {@code long}.
     *
     * @param index The index of the value to get.
     * @return Returns said value.
     */
    public int getIntValueAt(int index) {
        return (int) getLongValueAt(index);
    }

    /**
     * Gets this metadata value as a <code>long</code>.
     *
     * @return Returns said value.
     */
    public final long getLongValue() {
        return getLongValueAt(0);
    }

    /**
     * Gets the metadata value at the given index as a {@code long}.
     *
     * @param index The index of the value to get.
     * @return Returns said value.
     */
    public abstract long getLongValueAt(int index);

    /**
     * Returns the {@link ImageMetadataDirectory} to which this
     * <code>ImageMetaValue</code> belongs.
     *
     * @return Returns said {@link ImageMetadataDirectory}.
     */
    public final ImageMetadataDirectory getOwningDirectory() {
        return m_owningDirectory;
    }

    /**
     * Returns the tag ID that this <code>ImageMetaValue</code> is a value for.
     *
     * @return Returns said tag ID.
     */
    public final int getOwningTagID() {
        return m_owningTagID;
    }

    /**
     * Gets this metadata value as a <code>short</code>.
     *
     * @return Returns said value.
     */
    public final short getShortValue() {
        return (short)getLongValue();
    }

    /**
     * Gets this metadata value as a <code>String</code>.
     *
     * @return Returns said value.
     */
    public final String getStringValue() {
        return getStringValueAt(0);
    }

    /**
     * Gets this metadata value at the given index as a <code>String</code>.
     *
     * @return Returns said value.
     */
    public final String getStringValueAt(int index) {
        final String[] values = getValues();
        return values != null ? values[index] : null;
    }

    /**
     * Gets this metadata value's tag name.
     *
     * @return Returns said tag name or <code>null</code> if it either has
     * no owning {@link ImageMetadataDirectory} or said directory has no such
     * tag.
     */
    public final String getTagName() {
        final ImageMetadataDirectory dir = getOwningDirectory();
        return  dir != null ?
                dir.getTagNameFor( getOwningTagID(), false ) : null;
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Returns said type.
     * @see #isNumeric()
     */
    public abstract ImageMetaType getType();

    /**
     * Gets this metadata value as an unsigned <code>byte</code>.
     *
     * @return Returns said value.
     */
    public final short getUnsignedByteValue() {
        return (short)(getLongValue() & 0x000000FF);
    }

    /**
     * Gets this metadata value as an unsigned <code>byte</code>.
     *
     * @return Returns said value.
     */
    public final int getUnsignedShortValue() {
        return (int)(getLongValue() & 0x0000FFFF);
    }

    /**
     * Gets the number of values.
     *
     * @return Returns said number.
     */
    public abstract int getValueCount();

    /**
     * Gets the values as an array of {@link String}.
     *
     * @return Returns said array.
     */
    public final synchronized String[] getValues() {
        if ( m_valuesCache == null )
            m_valuesCache = getValuesImpl();
        return m_valuesCache;
    }

    /**
     * Returns whether this value should be displayed to the user.
     *
     * @return Returns <code>true</code> only if this value is displayable.
     */
    public final boolean isDisplayable() {
        return m_isDisplayable;
    }

    /**
     * Gets whether this metadata value is editable.
     *
     * @return Returns <code>true</code> only if it's editable.
     */
    public final boolean isEditable() {
        return m_isEditable;
    }

    /**
     * Returns whether this value has ever been edited.
     *
     * @return Returns <code>true</code> only if it has.
     * @see #clearEdited()
     */
    public final boolean isEdited() {
        return m_isEdited;
    }

    /**
     * Checks whether the given value is a legal value as a parameter to
     * {@link #setValues(String...)}.
     *
     * @param value The value to check.
     * @return Returns <code>true</code> only if the value is legal.
     */
    public boolean isLegalValue( String value ) {
        if ( m_owningDirectory != null )
            return m_owningDirectory.isLegalValue( m_owningTagID, value );
        return true;
    }

    /**
     * Returns true if this metadata value is null or its length is 0.
     *
     * @return Returns true if this metadata value is null or its length is 0,
     * otherwise false
     */
    public boolean isEmpty() {
        String value = getStringValue();
        return value == null || value.isEmpty();
    }

    /**
     * Returns whether this image metadata value is numeric.
     *
     * @return Returns <code>false</code> by default.
     * @see #getType()
     */
    public boolean isNumeric() {
        return false;
    }

    /**
     * Sets this metadata value from a <code>byte</code>.
     *
     * @param newValue The new value.
     */
    public final void setByteValue( byte newValue ) {
        setLongValue( newValue );
    }

    /**
     * Sets this metadata value from a <code>double</code>.
     *
     * @param newValue The new value.
     */
    public void setDoubleValue( double newValue ) {
        setLongValue( (long)newValue );
    }

    /**
     * Sets this metadata value from a <code>float</code>.
     *
     * @param newValue The new value.
     */
    public final void setFloatValue( float newValue ) {
        setDoubleValue( newValue );
    }

    /**
     * Sets this metadata value from an <code>int</code>.
     *
     * @param newValue The new value.
     */
    public final void setIntValue( int newValue ) {
        setLongValue( newValue );
    }

    /**
     * Sets whether this metadata is changeable.
     *
     * @param isChangeable The new changeable value.
     * @return Returns the old changeable value.
     */
    public final boolean setIsChangeable( boolean isChangeable ) {
        final boolean old = m_isEditable;
        m_isEditable = isChangeable;
        return old;
    }

    /**
     * Sets this metadata value from a <code>long</code>.
     *
     * @param newValue The new value.
     */
    public abstract void setLongValue( long newValue );

    /**
     * Sets this metadata value as &quot;non-displayable.&quot;  This is done
     * for {@link UndefinedMetaValue}s, values that are IFD pointers, or
     * contain subvalues that need to be expanded.  Once set, it can't be unset
     * (nor should there ever be a reason to).
     */
    public final void setNonDisplayable() {
        m_isDisplayable = false;
    }

    /**
     * Set the {@link ImageMetadataDirectory} to which this
     * <code>ImageMetaValue</code>'s belongs.
     *
     * @param dir The owning {@link ImageMetadataDirectory}.
     */
    public final void setOwningDirectory( ImageMetadataDirectory dir ) {
        m_owningDirectory = dir;
    }

    /**
     * Set the tag ID that this <code>ImageMetaValue</code> is a value for.
     *
     * @param tagID The owning {@link ImageMetadataDirectory}.
     */
    public final void setOwningTagID( int tagID ) {
        m_owningTagID = tagID;
    }

    /**
     * Sets this metadata value from a <code>short</code>.
     *
     * @param newValue The new value.
     */
    public void setShortValue( short newValue ) {
        setLongValue( newValue );
    }

    /**
     * Parse and set the values.
     *
     * @param newValues The array of new values.
     * @throws IllegalArgumentException if any one of the {@link String}s are
     * an illegal value for the given underlying type.
     * @see #isLegalValue(String)
     */
    public final synchronized void setValues( String... newValues ) {
        checkIsEditable();
        for ( String value : newValues )
            if ( !isLegalValue( value ) )
                throw new IllegalArgumentException( value );
        setValuesImpl( newValues );
        dirty();
    }

    /**
     * Convert this value to its {@link String} representation.  Multiple
     * values are separated by commas.
     *
     * @return Returns said {@link String}.
     */
    public final synchronized String toString() {
        if ( m_toStringCache == null ) {
            final ImageMetadataDirectory dir = getOwningDirectory();
            if ( dir != null ) {
                //
                // First, consult the the owning directory to see if this
                // metadata value needs special-handling.
                //
                m_toStringCache = dir.valueToString( this );
                if ( m_toStringCache == null ) {
                    //
                    // The owning directory didn't create a string for it
                    // because it didn't need special-handling, so revert to
                    // the ordinary way to create its string.
                    //
                    m_toStringCache = toStringImpl();
                }
            }
        }
        return m_toStringCache;
    }

    /**
     * Convert this value to is {@link String} representation but without
     * doing any {@link ImageMetadataDirectory} value consultation.
     *
     * @return Returns said string.
     */
    public final String toStringWithoutDirectoryConsult() {
        return toStringImpl();
    }

    /**
     * Convert this value to its XMP XML element representation.
     *
     * @param xmpDoc The {@link Document} to create the elements as part of.
     * @param nsURI The XML namespace URI to use.
     * @param prefix The XML namespace prefix to use.
     * @return Returns said XMP XML element or <code>null</code> if this value
     * can not be converted to an XMP XML element.
     */
    public Element toXMP( Document xmpDoc, String nsURI, String prefix ) {
        final String tagName = getTagName();
        if ( tagName == null )
            return null;
        Element tagElement = null;
        final String[] values = getValues();
        if ( values.length == 1 ) {
            //
            // The "if" below is commented out (for now) otherwise you can
            // never delete IPTC metadata from a photo if there's only one
            // value.
            //
            //if ( values[0].length() > 0 ) {
                tagElement =
                    xmpDoc.createElementNS( nsURI, prefix + ':' + tagName );
                XMLUtil.setTextContentOf( tagElement, values[0] );
            //}
        } else if ( values.length > 1 ) {
            tagElement =
                xmpDoc.createElementNS( nsURI, prefix + ':' + tagName );
            final Element seqElement = XMLUtil.addElementChildTo(
                tagElement, XMP_RDF_NS, XMP_RDF_PREFIX + ":Seq"
            );
            for ( String value : values ) {
                final Element listItem = XMLUtil.addElementChildTo(
                    seqElement, XMP_RDF_NS, XMP_RDF_PREFIX + ":li"
                );
                XMLUtil.setTextContentOf( listItem, value );
            }
        }
        return tagElement;
    }

    /**
     * Reconstitutes this <code>ImageMetaValue</code> from the externalized
     * form.
     *
     * @param in The {@link ObjectInput} to read from.
     */
    public abstract void readExternal( ObjectInput in ) throws IOException;

    /**
     * Writes this <code>ImageMetaValue</code> to an externalized form.
     *
     * @param out The {@link ObjectOutput} to write to.
     */
    public abstract void writeExternal( ObjectOutput out ) throws IOException;

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct an <code>ImageMetaValue</code>.
     */
    protected ImageMetaValue() {
        m_isEditable = false;
        m_isDisplayable = true;
    }

    /**
     * Parse and append a new value.
     *
     * @param newValue The new value.
     * @throws IllegalArgumentException if the {@link String} is an illegal
     * value for the given underlying type.
     */
    protected abstract void appendValueImpl( String newValue );

    /**
     * Checks whether this value is editable.
     *
     * @throws IllegalStateException if the value is not editable.
     */
    protected final void checkIsEditable() {
        if ( !m_isEditable )
            throw new IllegalStateException();
    }

    /**
     * Clear the caches used for this object.
     */
    protected final synchronized void clearCache() {
        m_toStringCache = null;
        m_valuesCache = null;
    }

    /**
     * Marks this value as "dirty", i.e., having been changed.
     */
    protected void dirty() {
        m_isEdited = true;
        clearCache();
    }

    /**
     * Gets the values as an array of {@link String}.
     *
     * @return Returns said array.
     */
    protected abstract String[] getValuesImpl();

    /**
     * Parse and set the values.
     *
     * @param newValue The array of new values.
     * @throws IllegalArgumentException if any one of the {@link String}s are
     * an illegal value for the given underlying type.
     */
    protected abstract void setValuesImpl( String[] newValue );

    /**
     * Convert this value to its {@link String} representation.  Multiple
     * values are separated by commas.
     *
     * @return Returns said string.
     */
    protected abstract String toStringImpl();

    /**
     * Reads the header information from the externalized form.
     *
     * @param in The {@link ObjectInput} to read from.
     * @return Returns the number of values.
     */
    protected final int readHeader( ObjectInput in ) throws IOException {
        m_isEditable = in.readBoolean();
        m_isDisplayable = in.readBoolean();
        return in.readInt();
    }

    /**
     * Writes the header information to the externalized form.
     *
     * @param out The {@link ObjectOutput} to write to.
     */
    protected final void writeHeader( ObjectOutput out ) throws IOException {
        out.writeBoolean( m_isEditable );
        out.writeBoolean( m_isDisplayable );
        out.writeInt( getValueCount() );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * This is <code>true</code> if this value should be displayed to the user.
     * Values that should not be displayed include {@link UndefinedMetaValue}s,
     * values that are IFD pointers, or contain subvalues that need to be
     * expanded.
     */
    private boolean m_isDisplayable;

    /**
     * This is <code>true</code> only if this metadata value is allowed to be
     * edited.
     */
    private boolean m_isEditable;

    /**
     * This is <code>true</code> only if the current value has been edited
     * from the original value.
     */
    private boolean m_isEdited;

    /**
     * The {@link ImageMetadataDirectory} to which this
     * <code>ImageMetaValue</code>'s owning belongs.
     */
    private ImageMetadataDirectory m_owningDirectory;

    /**
     * The tag ID that this <code>ImageMetaValue</code> is a value for.
     */
    private int m_owningTagID;

    /**
     * A cache of the {@link #toString()} representation of the values.
     */
    private String m_toStringCache;

    /**
     * A cache of the string representations of the values.
     */
    private String[] m_valuesCache;
}
/* vim:set et sw=4 ts=4: */
