/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.Rational;
import com.lightcrafts.utils.TextUtil;
import com.lightcrafts.utils.xml.ElementPrefixFilter;

import static com.lightcrafts.image.metadata.EXIFTags.TIFFCommonTags;
import static com.lightcrafts.image.metadata.ImageMetadata.unexportedTags;
import com.lightcrafts.image.metadata.providers.ImageMetadataProvider;

/**
 * An <code>ImageMetadataDirectory</code> contains all the metadata
 * (key/value pairs) for a particular &quot;directory&quot; of metadata, e.g.,
 * EXIF or IPTC.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class ImageMetadataDirectory
    implements Cloneable, Externalizable {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Clears all metadata.
     */
    public synchronized void clear() {
        m_tagIDToValueMap.clear();
    }

    /**
     * Clears the edited status of this directory and all
     * {@link ImageMetaValue}s in it.
     */
    public synchronized void clearEdited() {
        for ( ImageMetaValue value : m_tagIDToValueMap.values() )
            value.clearEdited();
    }

    /**
     * Performs a shallow clone.
     *
     * @return Returns said clone.
     */
    public final ImageMetadataDirectory clone() {
        final ImageMetadataDirectory copy;
        try {
            copy = getClass().newInstance();
        }
        catch ( Throwable t ) {
            throw new RuntimeException( t );
        }
        copy.setOwningMetadata( m_owningMetadata );
        //
        // A shallow clone isn't really the right thing to do here, but it's a
        // lot easier than doing a deep clone.
        //
        // The reason it isn't right is because ImageMetaValues point back to
        // their owning ImageMetadataDirectory.  Doing a shallow clone has the
        // ImageMetaValues in the copy pointing back to the original.
        //
        // Currently this is harmless because ImageMetaValue only uses methods
        // that don't rely on data, only behavior.
        //
        synchronized( this ) {
            copy.m_tagIDToValueMap.putAll( m_tagIDToValueMap );
        }
        return copy;
    }

    /**
     * Gets the maker-notes adjustments for a file.  The adjustments are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0.&nbsp;</td>
     *        <td>The start-of-directory offset adjustment.</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1.&nbsp;</td>
     *        <td>The larger-than-4-byte-value offset adjustment.</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     *
     * @param buf The {@link LCByteBuffer} the metadata is in.
     * @param offset The offset to the start of the maker-notes.
     * @return By default, returns <code>null</code>.
     */
    public int[] getMakerNotesAdjustments( LCByteBuffer buf, int offset )
        throws IOException
    {
        return null;
    }

    /**
     * Gets the name of this directory, e.g. &quot;EXIF&quot;.
     *
     * @return Returns said name.
     */
    public abstract String getName();

    /**
     * Gets the {@link ImageMetadata} to which this directory belongs.
     *
     * @return Returns said {@link ImageMetadata}.
     */
    public final ImageMetadata getOwningMetadata() {
        return m_owningMetadata;
    }

    /**
     * Gets the {@link Set} of metadata tag IDs of this directory.
     *
     * @param all If <code>true</code>, all the directory's tags are returned;
     * if <code>false</code>, only the tags that currently have values are
     * returned.
     * @return Returns a {@link Set} of {@link Integer} tag IDs.
     */
    public final Set<Integer> getTagIDSet( boolean all ) {
        if ( !all )
            synchronized ( this ) {
                return m_tagIDToValueMap.keySet();
            }

        final Class<? extends ImageMetaTags> tagsInterface = getTagsInterface();
        if ( tagsInterface == null )
            return new HashSet<Integer>();
        assert tagsInterface.isInterface();

        final Set<Integer> tagSet = new HashSet<Integer>();
        final Field[] fields = tagsInterface.getFields();
        for ( Field field : fields ) {
            try {
                if ( field.getType().equals( int.class ) )
                    tagSet.add( field.getInt( null ) );
            }
            catch ( IllegalAccessException e ) {
                // It's an interface -- won't ever happen
            }
            catch ( IllegalArgumentException e ) {
                // It's an interface -- won't ever happen
            }
        }
        return tagSet;
    }

    /**
     * Gets the {@link ImageMetaTagInfo} for a given tag ID.
     *
     * @param id The tag's ID.
     * @return Returns the {@link ImageMetaTagInfo} having the given ID or
     * <code>null</code> if no tag has the given ID.
     */
    public abstract ImageMetaTagInfo getTagInfoFor( Integer id );

    /**
     * Gets the {@link ImageMetaTagInfo} for a given tag name.
     *
     * @param name The tag's name.
     * @return Returns the {@link ImageMetaTagInfo} having the given name
     * or <code>null</code> if no tag has the given name.
     */
    public abstract ImageMetaTagInfo getTagInfoFor( String name );

    /**
     * Gets the label for the given metadata tag ID.  A tag's <i>label</i> is
     * the localized string that is to be displayed in the user-interface.
     *
     * @param tagID The metadata tag ID.
     * @return If a label for the given tag ID is found, returns said label;
     * otherwise returns the tag ID as a hexadecimal string.
     * @see #getTagLabelFor(int,boolean)
     * @see #getTagNameFor(int)
     * @see #getTagNameFor(int,boolean)
     */
    public final String getTagLabelFor( int tagID ) {
        return getTagLabelFor( tagID, true );
    }

    /**
     * Gets the label for the given metadata tag ID.  A tag's <i>label</i> is
     * the localized string that is to be displayed in the user-interface.
     *
     * @param tagID The metadata tag ID.
     * @param returnHexKey Applies only if a label for the given tag ID is not
     * found.  If <code>true</code>, returns the tag ID as a hexadecimal
     * string; otherwise returns <code>null</code>.
     * @return If a label for the given tag ID is found, returns said label;
     * otherwise, if <code>returnHexKey</code> is <code>true</code>, returns
     * the tag ID as a hexadecimal string; otherwise returns <code>null</code>.
     * @see #getTagLabelFor(int)
     * @see #getTagNameFor(int)
     * @see #getTagNameFor(int,boolean)
     */
    public final String getTagLabelFor( int tagID, boolean returnHexKey ) {
        final String key = TextUtil.zeroPad( tagID, 16, 4 );
        try {
            return getTagLabelBundle().getString( key );
        }
        catch ( MissingResourceException e ) {
            return returnHexKey ? key : null;
        }
    }

    /**
     * Gets the name for the given metadata tag ID.  A tag's <i>name</i> is the
     * English-only string that's used in specifications.
     *
     * @param tagID The metadata tag ID.
     * @return Returns the label for the given metadata tag ID or the tag ID
     * as a 4-digit hexadecimal string if the given tag ID has no name.
     * @see #getTagLabelFor(int)
     * @see #getTagLabelFor(int,boolean)
     * @see #getTagNameFor(int,boolean)
     */
    public final String getTagNameFor( int tagID ) {
        return getTagNameFor( tagID, true );
    }

    /**
     * Gets the name for the given metadata tag ID.  A tag's <i>name</i> is the
     * English-only string that's used in specifications.
     *
     * @param tagID The metadata tag ID.
     * @param returnHexKey If <code>true</code> and the tag has no name, a
     * 4-digit hexadecimal string is returned for the name.
     * @return Returns the label for the given metadata tag ID or the hex
     * value if the given tag ID has no label and <code>hexTagID</code> is
     * <code>true</code>, otherwise <code>null</code>.
     * @see #getTagLabelFor(int)
     * @see #getTagLabelFor(int,boolean)
     * @see #getTagNameFor(int)
     */
    public final String getTagNameFor( int tagID, boolean returnHexKey ) {
        final ImageMetaTagInfo tagInfo = getTagInfoFor( tagID );
        if ( tagInfo != null )
            return tagInfo.getName();
        return returnHexKey ? TextUtil.zeroPad( tagID, 16, 4 ) : null;
    }

    /**
     * Get the label for the tag ID's value.  A tag ID's value's <i>label</i>
     * is the localized string that is to be displayed in the user-interface.
     *
     * @param tagID The metadata tag ID.
     * @param value The metadata tag ID's value.
     * @return Returns the label for the given metadata tag ID's value or the
     * value itself converted to {@link String} if the given tag ID has no
     * label.
     * @see #getTagValueLabelFor(int,String)
     * @see #hasTagValueLabelFor(int)
     * @see #hasTagValueLabelFor(int,int)
     * @see #hasTagValueLabelFor(ImageMetaValue)
     * @see #hasTagValueLabelFor(int,ImageMetaValue)
     */
    public final String getTagValueLabelFor( int tagID, long value ) {
        return getTagValueLabelFor( tagID, Long.toString( value ) );
    }

    /**
     * Get the label for the tag ID's value.  A tag ID's value's <i>label</i>
     * is the localized string that is to be displayed in the user-interface.
     *
     * @param tagID The metadata tag ID.
     * @param value The metadata tag ID's value.
     * @return Returns the label for the given metadata tag ID's value or the
     * value itself if the given tag ID has no label.
     * @see #getTagValueLabelFor(int,String)
     * @see #hasTagValueLabelFor(int)
     * @see #hasTagValueLabelFor(int,int)
     * @see #hasTagValueLabelFor(ImageMetaValue)
     * @see #hasTagValueLabelFor(int,ImageMetaValue)
     */
    public final String getTagValueLabelFor( int tagID, String value ) {
        final String key = TextUtil.zeroPad( tagID, 16, 4 ) + '>' + value;
        try {
            return getTagLabelBundle().getString( key );
        }
        catch ( MissingResourceException e ) {
            return value;
        }
    }

    /**
     * Gets all the values, and optionally the corresponding labels for the
     * values, that a given tag is known to have.
     *
     * @param tagID The metadata tag ID.
     * @param includeLabels If <code>false</code>, just the values are
     * returned; if <code>true</code>, each value is followed by an
     * <code>'='</code> and the label for that value.
     * @return Returns said values in an arbitrary order or <code>null</code>
     * if either the given tag doesn't exist or it has no values.
     */
    public final Collection<String> getTagValuesFor( int tagID,
                                                     boolean includeLabels ) {
        final String keyPrefix = TextUtil.zeroPad( tagID, 16, 4 ) + '>';
        Collection<String> values = null;
        for ( Enumeration<String> keys = getTagLabelBundle().getKeys();
              keys.hasMoreElements(); ) {
            final String key = keys.nextElement();
            if ( key.startsWith( keyPrefix ) ) {
                if ( values == null )
                    values = new HashSet<String>();
                final int separatorPos = key.indexOf( '>' );
                String value = key.substring( separatorPos + 1 );
                if ( includeLabels )
                    value += '=' + getTagValueLabelFor( tagID, value );
                values.add( value );
            }
        }
        return values;
    }

    /**
     * Gets the metadata value for a given tag ID (key).  If the tag ID isn't
     * found in this directory, each directory along the static parent chain is
     * checked also.
     *
     * @param tagID The metadata tag ID (the key).
     * @return Returns the value indexed by the tag ID or <code>null</code> if
     * there is no value assosiated with the given tag ID.
     * @see #getStaticParent()
     */
    public final synchronized ImageMetaValue getValue( Integer tagID ) {
        ImageMetadataDirectory dir = this;
        while ( dir != null ) {
            final ImageMetaValue value = dir.m_tagIDToValueMap.get( tagID );
            if ( value != null )
                return value;
            dir = dir.getStaticParent();
        }
        return null;
    }

    /**
     * Get the label for the tag ID's value.  A tag ID's value's <i>label</i>
     * is the localized string that is to be displayed in the user-interface.
     *
     * @param tagID The metadata tag ID.
     * @return Returns the label for the given metadata tag ID's value or a
     * <code>null</code> if the given tag ID has no label.
     * @see #getTagValueLabelFor(int,long)
     * @see #getTagValueLabelFor(int,String)
     * @see #hasTagValueLabelFor(int,int)
     * @see #hasTagValueLabelFor(ImageMetaValue)
     * @see #hasTagValueLabelFor(int,ImageMetaValue)
     */
    public final String hasTagValueLabelFor( int tagID ) {
        final ImageMetaValue value = getValue( tagID );
        if ( value == null )
            return null;
        final String key =
            TextUtil.zeroPad( tagID, 16, 4 ) + '>' + value.getStringValue();
        try {
            return getTagLabelBundle().getString( key );
        }
        catch ( MissingResourceException e ) {
            return null;
        }
    }

    /**
     * Gets the label for the tag ID's value.  A tag ID's value's <i>label</i>
     * is the localized string that is to be displayed in the user-interface.
     *
     * @param tagID The metadata tag ID.
     * @param value The metadata tag ID's value.
     * @return Returns the label for the given metadata tag ID's value or a
     * <code>null</code> if the given tag ID has no label.
     * @see #getTagValueLabelFor(int,long)
     * @see #getTagValueLabelFor(int,String)
     * @see #hasTagValueLabelFor(int)
     * @see #hasTagValueLabelFor(ImageMetaValue)
     * @see #hasTagValueLabelFor(int,ImageMetaValue)
     */
    public final String hasTagValueLabelFor( int tagID, int value ) {
        final String valueString = Integer.toString( value );
        final String key = TextUtil.zeroPad( tagID, 16, 4 ) + '>' + valueString;
        try {
            return getTagLabelBundle().getString( key );
        }
        catch ( MissingResourceException e ) {
            return null;
        }
    }

    /**
     * Gets the label for the given {@link ImageMetaValue}'s integer value.  An
     * {@link ImageMetaValue}'s <i>label</i> is the localized string that is to
     * be displayed in the user-interface.
     *
     * @param value The {@link ImageMetaValue}.
     * @return Returns the label for the value or <code>null</code> if no label
     * exists for it.
     * @see #getTagValueLabelFor(int,long)
     * @see #getTagValueLabelFor(int,String)
     * @see #hasTagValueLabelFor(int)
     * @see #hasTagValueLabelFor(int,int)
     * @see #hasTagValueLabelFor(int,ImageMetaValue)
     */
    protected static String hasTagValueLabelFor( ImageMetaValue value ) {
        if ( value != null )
            return hasTagValueLabelFor( value.getOwningTagID(), value );
        return null;
    }

    /**
     * Gets the label for the tag ID's value.  A tag ID's value's <i>label</i>
     * is the localized string that is to be displayed in the user-interface.
     *
     * @param tagID The metadata tag ID.
     * @param value The metadata tag ID's value.
     * @return Returns the label for the value or <code>null</code> if no label
     * exists for it.
     * @see #getTagValueLabelFor(int,long)
     * @see #getTagValueLabelFor(int,String)
     * @see #hasTagValueLabelFor(int)
     * @see #hasTagValueLabelFor(int,int)
     * @see #hasTagValueLabelFor(ImageMetaValue)
     */
    protected static String hasTagValueLabelFor( int tagID,
                                                 ImageMetaValue value ) {
        if ( value != null ) {
            //
            // Just because there is apparently detailed information about the
            // lens doesn't mean there actually is.  Specifically, we have to
            // test whether the actual integer value has a label in it's
            // directory's properties file.  If it doesn't, we don't want to
            // use the value, so return null and hope that later code will set
            // it.
            //
            final ImageMetadataDirectory dir = value.getOwningDirectory();
            return dir.hasTagValueLabelFor( tagID, value.getIntValue() );
        }
        return null;
    }

    /**
     * Returns whether any {@link ImageMetaValue} in this directory has been
     * changed.
     *
     * @return Returns <code>true</code> only if at least one
     * {@link ImageMetaValue} has been changed.
     * @see #clearEdited()
     * @see ImageMetaValue#isEdited()
     */
    public final synchronized boolean isChanged() {
        for ( ImageMetaValue value : m_tagIDToValueMap.values() )
            if ( value.isEdited() )
                return true;
        return false;
    }

    /**
     * Returns whether this directory is empty, i.e., has no metadata.
     *
     * @return Returns <code>true</code> only if the directory is empty.
     */
    public final synchronized boolean isEmpty() {
        return m_tagIDToValueMap.isEmpty();
    }

    /**
     * Checks whether the given value for the given tag is a legal parameter to
     * {@link #setValue(Integer,String...)}.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The value to check.
     * @return Returns <code>true</code> only if the value is legal.
     */
    public boolean isLegalValue( Integer tagID, String value ) {
        final ImageMetaTagInfo tag = getTagInfoFor( tagID );
        if ( tag == null )
            throw new IllegalArgumentException( "unknown tag " + tagID );
        //
        // If the string is null or the empty string, it's "legal" because a
        // null or empty value is one way to delete a tag.
        //
        if ( value == null || value.length() == 0 )
            return true;

        return tag.createValue().isLegalValue( value );
    }

    /**
     * Returns an {@link Iterator} over the key/value pairs of metadata in this
     * directory.
     *
     * @return Returns said {@link Iterator}.
     */
    public final synchronized Iterator<Map.Entry<Integer,ImageMetaValue>>
    iterator() {
        return m_tagIDToValueMap.entrySet().iterator();
    }

    /**
     * Merge the metadata from another {@link ImageMetadataDirectory} object
     * into this one.
     *
     * @param fromDir The other {@link ImageMetadataDirectory} to merge from.
     */
    public void mergeFrom( ImageMetadataDirectory fromDir ) {
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i =
              fromDir.iterator(); i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            putValue( me.getKey(), me.getValue() );
        }
    }

    /**
     * Parses an XMP XML element in order to read XMP metadata.
     *
     * @param tagInfo The {@link ImageMetaTagInfo} describing the associated
     * image metadata tag information for the given element.
     * @param element The {@link Element} to parse.
     * @param dirPrefixFilter The {@link ElementPrefixFilter} to use.
     * @return Returns <code>true</code> only if the element was parsed.
     */
    public boolean parseXMP( ImageMetaTagInfo tagInfo, Element element,
                             ElementPrefixFilter dirPrefixFilter ) {
        return false;
    }

    /**
     * Puts a key/value pair into this directory.
     *
     * This method should be used if a value is being put into the directory
     * for the first time, e.g., to populate it from metadata parsed from an
     * image.  To change a value at some later time, use
     * {@link #setValue(Integer,int)} or {@link #setValue(Integer,String...)}
     * instead.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The {@link ImageMetaValue} to put.
     * @see #putValue(Integer,ImageMetaValue,boolean)
     */
    public void putValue( Integer tagID, ImageMetaValue value ) {
        putValue( tagID, value, true );
    }

    /**
     * Puts a key/value pair into this directory.
     *
     * This method should be used if a value is being put into the directory
     * for the first time, e.g., to populate it from metadata parsed from an
     * image.  To change a value at some later time, use
     * {@link #setValue(Integer,int)} or {@link #setValue(Integer,String...)}
     * instead.
     *
     * @param tagID The metadata tag ID (the key).
     * @param value The {@link ImageMetaValue} to put.
     * @param setOwner If <code>true</code>, set the owning
     * {@link ImageMetadataDirectory} and tag ID.
     * @see #putValue(Integer,ImageMetaValue)
     */
    public final void putValue( Integer tagID, ImageMetaValue value,
                                boolean setOwner ) {
        if ( value == null )
            throw new IllegalArgumentException( "null value" );
        if ( value.getValueCount() == 0 )
            throw new IllegalArgumentException( "value has 0 values" );

        if ( value instanceof StringMetaValue ) {
            //
            // If a StringMetaValue's string value is null, don't bother adding
            // it to the metadata: throw any new value away and remove any old
            // value.
            //
            final StringMetaValue stringValue = (StringMetaValue)value;
            final String s = stringValue.getStringValue();
            if ( s == null ) {
                removeValue( tagID );
                return;
            }
        }

        final ImageMetaTagInfo tag = getTagInfoFor( tagID );
        if ( tag != null ) {
/*
            if ( !value.getType().isCompatibleWith( tag.getType() ) )
                throw new IllegalArgumentException();
*/
            value.setIsChangeable( tag.isChangeable() );
        }

        if ( setOwner ) {
            value.setOwningDirectory( this );
            value.setOwningTagID( tagID );
        }
        synchronized( this ) {
            m_tagIDToValueMap.put( tagID, value );
        }
    }

    /**
     * Remove all string values that are empty.
     */
    public final void removeAllEmptyStringValues() {
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i = iterator();
              i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> e = i.next();
            final ImageMetaValue value = e.getValue();
            if ( value instanceof StringMetaValue ) {
                final String s = value.getStringValue();
                if ( s == null || s.length() == 0 )
                    i.remove();
            }
        }
    }

    /**
     * Removes the metadata value for a given tag ID (key).  If the tag ID
     * isn't found in this directory, each directory along the static parent
     * chain is checked also.
     *
     * @param tagID The metadata tag ID (the key).
     * @return Returns the value associated with the key if it was present;
     * <code>null</code> otherwise.
     */
    public final synchronized ImageMetaValue removeValue( Integer tagID ) {
        ImageMetadataDirectory dir = this;
        while ( dir != null ) {
            final ImageMetaValue value = dir.m_tagIDToValueMap.remove( tagID );
            if ( value != null )
                return value;
            dir = dir.getStaticParent();
        }
        return null;
    }

    /**
     * Sets an integral value.  Unlike
     * {@link #putValue(Integer,ImageMetaValue)}, this method marks the value
     * as "dirty."
     *
     * @param tagID The metadata tag ID (the key).
     * @param newValue The new value.
     * @see #clearEdited()
     * @see #isChanged()
     * @see #setValue(Integer,String...)
     */
    public final void setValue( Integer tagID, int newValue ) {
        final ImageMetaTagInfo tag = getTagInfoFor( tagID );
        if ( tag == null )
            throw new IllegalArgumentException( "unknown tag " + tagID );

        synchronized ( this ) {
            ImageMetaValue value = m_tagIDToValueMap.get( tagID );
            if ( value == null ) {
                value = tag.createValue();
                value.setOwningDirectory( this );
                value.setIntValue( newValue );
                putValue( tagID, value );
            } else
                value.setIntValue( newValue );
        }
    }

    /**
     * Sets a string value.  Unlike {@link #putValue(Integer,ImageMetaValue)},
     * this method marks the value as "dirty."
     *
     * @param tagID The metadata tag ID (the key).
     * @param newValues The new values.
     * @see #clearEdited()
     * @see #isChanged()
     * @see #setValue(Integer,int)
     */
    public final void setValue( Integer tagID, String... newValues ) {
        final ImageMetaTagInfo tag = getTagInfoFor( tagID );
        if ( tag == null )
            throw new IllegalArgumentException( "unknown tag " + tagID );

/*
        if ( newValues == null || newValues.length == 0 ||
             newValues.length == 1 &&
                 (newValues[0] == null || newValues[0].length() == 0) ) {
            removeValue( tagID );
            return;
        }
*/

        synchronized ( this ) {
            ImageMetaValue value = m_tagIDToValueMap.get( tagID );
            if ( value == null ) {
                value = tag.createValue();
                value.setOwningDirectory( this );
                value.setValues( newValues );
                putValue( tagID, value );
            } else
                value.setValues( newValues );

            CoreDirectory.syncEditableMetadata( getOwningMetadata() );
        }
    }

    /**
     * Checks whether the metadata having the given tag ID should be displayed
     * to an end-user.
     *
     * @param tagID The metadata tag ID.
     * @return Returns <code>true</code> only if the tag should be didplayed to
     * an end-user.
     */
    public final boolean shouldDisplayTag( int tagID ) {
        final String label = getTagLabelFor( tagID, false );
        return label != null && !label.endsWith( "-X" );
    }

    /**
     * Returns the number of key/value pairs.
     *
     * @return Returns said number.
     */
    public final synchronized int size() {
        return m_tagIDToValueMap.size();
    }

    /**
     * Convert all the metadata into a single {@link String} for debugging
     * purposes.
     *
     * @return Returns said {@link String}.
     */
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append( getName() );
        sb.append( "\n----------------------------------------\n" );
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i = iterator();
              i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            sb.append( getTagNameFor( me.getKey() ) );
            sb.append( '=' );
            sb.append( me.getValue() );
            sb.append( '\n' );
        }
        return sb.toString();
    }

    /**
     * Creates the <code>rdf:Description</code> {@link Element}(s) and its
     * contents containing the XMP from of the metadata in this directory.
     *
     * @param xmpDoc The XMP document to create new elements for.
     * @return Returns said element(s) or <code>null</code> if the directory
     * can not be converted to XMP.
     */
    public Collection<Element> toXMP( Document xmpDoc ) {
        return null;
    }

    /**
     * This method allows an <code>ImageMetadataDirectory</code> to alter the
     * <code>toString()</code> value of an {@link ImageMetaValue}.
     *
     * @param value The {@link ImageMetaValue} whose value to convert to a
     * {@link String}.
     * @return The default reformats {@link Rational} numbers and returns them;
     * for all other types, returns <code>null</code> meaning no value
     * alteration is to be made.
     */
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getType() ) {
            case META_SRATIONAL:
            case META_URATIONAL:
                //
                // Print rational numbers better.
                //
                final Rational r =
                    ((RationalMetaValue)value).getRationalValue();
                if ( r.isInteger() )
                    return Integer.toString( r.intValue() );
                if ( r.numerator() > r.denominator() )
                    return TextUtil.tenths( r );
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        for ( int size = in.readShort(); size > 0; --size ) {
            final int tagID = in.readInt();
            final ImageMetaType type =
                ImageMetaType.getTypeFor( in.readShort() );
            final ImageMetaValue value = ImageMetaValue.create( type );
            value.readExternal( in );
            putValue( tagID, value );
        }
    }

    /**
     * @serialData The number of directory entries (<code>short</code>)
     * followed by each directory entry comprising the tag ID
     * (<code>int</code>), the metadata type (<code>short</code), and the
     * {@link ImageMetaValue}.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeShort( size() );
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i = iterator();
              i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            final int tagID = me.getKey();
            final ImageMetaValue value = me.getValue();
            out.writeInt( tagID );
            out.writeShort( value.getType().getTIFFConstant() );
            value.writeExternal( out );
        }
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Move those tags that are common between TIFF and EXIF metadata from one
     * directory to another.
     *
     * @param sourceDir The <code>ImageMetadataDirectory</code> to move values
     * from.
     * @param targetDir The <code>ImageMetadataDirectory</code> to move values
     * to.
     */
    static void moveValuesFromTo( ImageMetadataDirectory sourceDir,
                                  ImageMetadataDirectory targetDir ) {
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>>
              i = sourceDir.iterator(); i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            final int tagID = me.getKey();
            if (TIFFCommonTags.contains(tagID)
                    && !unexportedTags.contains(tagID)) {
                targetDir.putValue(tagID, me.getValue());
                i.remove();
            }
        }
    }

    /**
     * Sets the {@link ImageMetadata} to which this directory belongs.
     *
     * @param metadata The owning {@link ImageMetadata}.
     */
    final void setOwningMetadata( ImageMetadata metadata ) {
        m_owningMetadata = metadata;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * &quot;Explode&quot; a tag's value that has subfields into individual
     * {@link ImageMetaValue}s.
     *
     * @param tagID The tag ID of the field to be exploded.
     * @param startIndex The array index of the first value to start from.
     * @param value The {@link ImageMetaValue} (assumed to be a either a
     * {@link LongMetaValue} or an {@link UndefinedMetaValue) whose values are
     * to be exploded.
     * @param asLong If <code>true</code>, explode the values as
     * {@link LongMetaValue}s rather than {@link ShortMetaValue}s.
     */
    protected final void explodeSubfields( int tagID, int startIndex,
                                           ImageMetaValue value,
                                           boolean asLong ) {
        value.setNonDisplayable();
        //
        // Our convention for generating a unique tag ID for subfields is to
        // left-shift the original tag ID, then add sequential integers.
        //
        tagID <<= 8;
        tagID += startIndex;
        switch ( value.getType() ) {
            case META_UNDEFINED:
                final byte[] bytes =
                    ((UndefinedMetaValue)value).getUndefinedValue();
                //
                // To date, the only supported camera that uses an undefined
                // metadata value for its maker notes is Minolta.  Despite
                // being undefined, the bytes need to be treated as contiguous
                // EXIF "long" (4-byte) values and always big-endian.  The easy
                // way to do this is to wrap the undefined value inside a
                // ByteBuffer.
                //
                final ByteBuffer buf = ByteBuffer.wrap( bytes );
                for ( int i = startIndex; i < bytes.length / 4; ++i, ++tagID ) {
                    final int n = buf.getInt();
                    if ( n >= 0 )
                        putValue( tagID, new LongMetaValue( n ) );
                }
                break;
            default:
                if ( value.isNumeric() ) {
                    final long[] longs = ((LongMetaValue)value).getLongValues();
                    for ( int i = startIndex; i < longs.length; ++i, ++tagID ) {
                        final long n = longs[i];
                        value = asLong ?
                            new LongMetaValue( n ) :
                            new ShortMetaValue( (short)n );
                        putValue( tagID, value );
                    }
                }
                break;
        }
    }

    /**
     * Gets the camera make, and possibly model, of the camera used.
     *
     * @param makeTagID The tag ID for the make.
     * @param modelTagID The tag ID for the model.
     * @param includeModel If <code>true</code>, the model is included.
     * @return Returns the make (and possibly model) converted to uppercase and
     * seperated by a space or <code>null</code> if not available.
     */
    protected final String getCameraMake( int makeTagID, int modelTagID,
                                          boolean includeModel ) {
        final ImageMetaValue makeValue = getValue( makeTagID );
        if ( makeValue == null )
            return null;
        String make = makeValue.getStringValue();
        if ( includeModel ) {
            final ImageMetaValue modelValue = getValue( modelTagID );
            if ( modelValue != null )
                make = MetadataUtil.undupMakeModel(
                    make, modelValue.toString()
                );
        }
        return make.toUpperCase().trim();
    }

    /**
     * Gets the priority of this directory for providing the metadata supplied
     * by implementing the given provider interface.
     *
     * @param provider The provider interface to get the priority for.
     * @return The default always returns 1.
     * @see ImageMetadata#findProvidersOf(Class)
     */
    protected int getProviderPriorityFor(
        Class<? extends ImageMetadataProvider> provider )
    {
        return 1;
    }

    /**
     * Get the {@link ResourceBundle} to use for tag labels.
     *
     * @return Returns said {@link ResourceBundle}.
     */
    protected abstract ResourceBundle getTagLabelBundle();

    /**
     * Returns the {@link Class} for the tags <code>interface</code> for this
     * directory.
     *
     * @return Returns said {@link Class}.
     */
    protected abstract Class<? extends ImageMetaTags> getTagsInterface();

    /**
     * Make a lens value label from the short and long focal lengths of a lens
     * as well as the apertures at said focal lengths.
     *
     * @param value The lens data as either a {@link RationalMetaValue} or an
     * {@link UnsignedRationalMetaValue}.  There must be exactly 4 values:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0.&nbsp;</td><td>short focal length</td></tr>
     *      <tr><td>1.&nbsp;</td><td>long focal length</td></tr>
     *      <tr><td>2.&nbsp;</td><td>aperture at short focal length</td></tr>
     *      <tr><td>3.&nbsp;</td><td>aperture at long focal length</td></tr>
     *    </table>
     *  </blockquote>
     * @return Returns the lens label of the form "18-55mm F4.0-5.0" or
     * <code>null</code> if the data isn't in the expected format.
     */
    protected static String makeLensLabelFrom( ImageMetaValue value ) {
        switch ( value.getType() ) {
            case META_SRATIONAL:
            case META_URATIONAL:
                final Rational[] lensValues =
                    ((RationalMetaValue)value).getRationalValues();
                if ( lensValues.length != 4 )
                    break;
                final StringBuilder sb = new StringBuilder();
                sb.append( TextUtil.tenthsNoDotZero( lensValues[0] ) );
                if ( lensValues[0].compareTo( lensValues[1] ) != 0 ) {
                    sb.append( '-' );
                    sb.append( TextUtil.tenthsNoDotZero( lensValues[1] ) );
                }
                sb.append( "mm F" );
                sb.append( TextUtil.tenthsNoDotZero( lensValues[2] ) );
                if ( lensValues[2].compareTo( lensValues[3] ) != 0 ) {
                    sb.append( '-' );
                    sb.append( TextUtil.tenthsNoDotZero( lensValues[3] ) );
                }
                return sb.toString();
            default:
                break;
        }
        return null;
    }

    /**
     * Make a lens value label from the short and long focal lengths of a lens,
     * e.g., "18-55mm".
     *
     * @param shortFocalLenValue The {@link ImageMetaValue} containing the
     * short focal length of the lens.
     * @param longFocalLenValue The {@link ImageMetaValue} containing the
     * long focal length of the lens.
     * @param unitsPerMMValue The {@link ImageMetaValue} containing the units
     * per mm.
     * @return Returns said label or <code>null</code> if no label could be
     * made.
     */
    protected static String makeLensLabelFrom(
        ImageMetaValue shortFocalLenValue, ImageMetaValue longFocalLenValue,
        ImageMetaValue unitsPerMMValue )
    {
        if ( shortFocalLenValue != null && longFocalLenValue != null ) {
            double shortFocalLen = shortFocalLenValue.getIntValue();
            if ( shortFocalLen > 0 ) {
                double longFocalLen  = longFocalLenValue.getIntValue();
                if ( unitsPerMMValue != null ) {
                    final int unitsPerMM = unitsPerMMValue.getIntValue();
                    if ( unitsPerMM > 0 ) {
                        //
                        // We have to adjust the focal lengths by the number of
                        // "units" in a millimeter in order to get millimeters.
                        //
                        shortFocalLen /= unitsPerMM;
                        longFocalLen /= unitsPerMM;
                    }
                }
                final StringBuilder sb = new StringBuilder();
                sb.append( TextUtil.tenthsNoDotZero( shortFocalLen ) );
                if ( longFocalLen > shortFocalLen ) {
                    sb.append( '-' );
                    sb.append( TextUtil.tenthsNoDotZero( longFocalLen ) );
                }
                sb.append( "mm" );          // TODO: localize "mm"
                return sb.toString();
            }
        }
        return null;
    }

    /**
     * Reads the maker notes from the given buffer.
     *
     * @param buf The buffer to read from.
     * @param offset The offset into the buffer where the maker notes data
     * starts.
     * @param byteCount The number of bytes of maker notes data.
     * @return Returns <code>true</code> only if the maker notes were read and
     * the default maker notes reading code should not be used.
     */
    protected boolean readMakerNotes( LCByteBuffer buf, int offset,
                                      int byteCount ) throws IOException {
        return false;
    }

    /**
     * Convert this directory's metadata values to XMP.
     *
     * @param xmpDoc The XMP document create elements within.
     * @param nsURI The XML namespace URI to use.
     * @param prefix The XML namespace prefix to use for new elements.
     * @return Returns the <code>rdf:Description</code> element(s) containing
     * this directory's metadata.
     */
    protected Collection<Element> toXMP( Document xmpDoc, String nsURI,
                                         String prefix ) {
        Element rdfDescElement = null;
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i = iterator();
              i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            final ImageMetaValue value = me.getValue();
            final Element valueElement = value.toXMP( xmpDoc, nsURI, prefix );
            if ( valueElement != null ) {
                if ( rdfDescElement == null )
                    rdfDescElement = XMPUtil.createRDFDescription(
                        xmpDoc, nsURI, prefix
                    );
                rdfDescElement.appendChild( valueElement );
            }
        }
        if ( rdfDescElement != null ) {
            final Collection<Element> elements = new ArrayList<Element>( 1 );
            elements.add( rdfDescElement );
            return elements;
        }
        return null;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Gets the static parent directory of this directory, if any.  The
     * <i>static parent</i> directory is an instance of a Java class that is a
     * superclass.  For example, an instance of {@link EXIFDirectory} can be
     * the static parent of an {@link SubEXIFDirectory} because the
     * {@link SubEXIFDirectory} class is derived from the {@link EXIFDirectory}
     * class.
     *
     * @return Returns the static parent directory or <code>null</code> if
     * none.
     */
    private ImageMetadataDirectory getStaticParent() {
        final Class<? extends ImageMetadataDirectory> superClass =
            getSuperClassOf( getClass() );
        return  superClass != null && m_owningMetadata != null ?
                m_owningMetadata.getDirectoryFor( superClass ) : null;
    }

    /**
     * Gets the <code>ImageMetadataDirectory</code> superclass of the given
     * <code>ImageMetadataDirectory</code> class.  As a special case and
     * convenience, if the given class has no superclass other than either
     * <code>ImageMetadataDirectory</code> or {@link Object}, <code>null</code>
     * is returned instead since returning either isn't useful.
     *
     * @param dirClass The {@link Class} to get the superclass of.
     * @return Returns the superclass of the given class or <code>null</code>
     * if none (other than {@link Object}.
     */
    private static Class<? extends ImageMetadataDirectory> getSuperClassOf(
        Class<? extends ImageMetadataDirectory> dirClass )
    {
        final Class superClass = dirClass.getSuperclass();
        //noinspection unchecked
        return  superClass == ImageMetadataDirectory.class ||
                superClass == Object.class ? null : superClass;
    }

    /**
     * The {@link ImageMetadata} to which this directory belongs.
     */
    private ImageMetadata m_owningMetadata;

    /**
     * A {@link Map} of tag IDs to {@link ImageMetaValue}s.
     */
    private final HashMap<Integer,ImageMetaValue> m_tagIDToValueMap =
        new HashMap<Integer,ImageMetaValue>();
}
/* vim:set et sw=4 ts=4: */
