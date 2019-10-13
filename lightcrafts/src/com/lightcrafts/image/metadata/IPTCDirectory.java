/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.nio.ByteBuffer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.lightcrafts.image.metadata.values.DateMetaValue;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;
import com.lightcrafts.utils.xml.ElementPrefixFilter;
import com.lightcrafts.utils.xml.XMLUtil;

import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.IPTCConstants.*;
import static com.lightcrafts.image.metadata.IPTCTags.*;
import static com.lightcrafts.image.metadata.IPTCTagInfo.*;
import static com.lightcrafts.image.metadata.XMPConstants.*;
import static com.lightcrafts.image.types.AdobeConstants.*;

/**
 * An <code>IPTCDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding IPTC metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class IPTCDirectory extends ImageMetadataDirectory implements
    ArtistProvider, CaptureDateTimeProvider, CopyrightProvider, TitleProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Encode an {@link IPTCDirectory}'s values into a byte array suitable for
     * writing into a JPEG image file.
     *
     * @param includePhotoshopHeader If <code>true</code>, encode the Photoshop
     * header as is needed in JPEG files.
     * @return Returns said byte array.
     */
    public byte[] encode( boolean includePhotoshopHeader ) {
        //
        // The binary encoding of IPTC metadata should not have empty string
        // values, so we need to remove them; but we don't want to alter this
        // directory, so make a clone and alter it instead.
        //
        final ImageMetadataDirectory iptcCopy = clone();
        iptcCopy.removeAllEmptyStringValues();
        if ( iptcCopy.isEmpty() )
            return null;
        final ByteBuffer buf =
            ((IPTCDirectory)iptcCopy).encodeImpl( includePhotoshopHeader );
        return buf.array();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getArtist() {
        final ImageMetaValue value = getValue( IPTC_CREATOR );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getCaptureDateTime() {
        final ImageMetaValue value = getValue( IPTC_DATE_CREATED );
        return  value instanceof DateMetaValue ?
                ((DateMetaValue)value).getDateValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCopyright() {
        final ImageMetaValue value = getValue( IPTC_COPYRIGHT_NOTICE );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;IPTC&quot;.
     */
    @Override
    public String getName() {
        return "IPTC";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageMetaTagInfo getTagInfoFor( Integer id ) {
        return m_tagsByID.get( id );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageMetaTagInfo getTagInfoFor( String name ) {
        return m_tagsByName.get( name );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        final ImageMetaValue value = getValue( IPTC_OBJECT_NAME );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLegalValue( Integer tagID, String value ) {
        if ( !super.isLegalValue( tagID, value ) )
            return false;
        if ( value == null || value.length() == 0 )
            return true;

        switch ( tagID ) {

            case IPTC_CATEGORY:
                final int categoryLength = value.length();
                return categoryLength >= 1 && categoryLength <= 3;

            case IPTC_COUNTRY_CODE:
                for ( String countryCode : java.util.Locale.getISOCountries() )
                    if ( value.equalsIgnoreCase( countryCode ) )
                        return true;
                return false;

            case IPTC_ENVELOPE_PRIORITY:
                try {
                    final int priority = Integer.parseInt( value );
                    return priority >= 1 && priority <= 9;
                }
                catch ( NumberFormatException e ) {
                    return false;
                }

            case IPTC_LANGUAGE_IDENTIFIER:
                for ( String langCode : java.util.Locale.getISOLanguages() )
                    if ( value.equalsIgnoreCase( langCode ) )
                        return true;
                return false;

            case IPTC_OBJECT_CYCLE:
                return value.length() == 1 && "apb".contains( value );

            case IPTC_SCENE:
                try {
                    Integer.parseInt( value );
                    if ( value.length() != 6 )
                        return false;
                    return getTagValuesFor( tagID, false ).contains( value );
                }
                catch ( NumberFormatException e ) {
                    return false;
                }

            case IPTC_SUBJECT_CODE:
                try {
                    Integer.parseInt( value );
                    if ( value.length() != 8 )
                        return false;
                    return getTagValuesFor( tagID, false ).contains( value );
                }
                catch ( NumberFormatException e ) {
                    return false;
                }

            case IPTC_URGENCY:
                try {
                    final int urgency = Integer.parseInt( value );
                    return urgency >= 1 && urgency <= 8;
                }
                catch ( NumberFormatException e ) {
                    return false;
                }

            case IPTC_DIGITAL_CREATION_TIME:
            case IPTC_EXPIRATION_TIME:
            case IPTC_RELEASE_TIME:
            case IPTC_TIME_CREATED:
            case IPTC_TIME_SENT:
                // TODO

            case IPTC_INTELLECTUAL_GENRE:
            case IPTC_SUPPLEMENTAL_CATEGORIES:
                // TODO

            default:
                return true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean parseXMP( ImageMetaTagInfo tagInfo, Element element,
                             ElementPrefixFilter dirPrefixFilter ) {
        if ( tagInfo.getID() != IPTC_CREATOR_CONTACT_INFO )
            return false;
        final Node[] children =
            XMLUtil.getChildrenOf( element, dirPrefixFilter );
        XMPMetadataReader.parseElements( children, dirPrefixFilter, this );
        return true;
    }

    /**
     * Checks whether the given tag is one of the IPTC XMP Core Creator Contact
     * Info tags.
     *
     * @param tagID The tag ID to check.
     * @return Returns <code>true</code> only if the tag is one of the Creator
     * Contact Info tags.
     */
    public static boolean tagIsCreatorContactInfo( int tagID ) {
        return (tagID & 0xFF00) == IPTC_CREATOR_CONTACT_INFO;
    }

    /**
     * Checks whether the tag is one of the original Information Interchange
     * Model (IIM) tags.
     *
     * @param tagID The tag ID.
     * @return Returns <code>true</code> only if the tag is one of the IIM
     * tags.
     */
    public static boolean tagIsIIM( int tagID ) {
        return tagID < 0x0A00;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Element> toXMP( Document xmpDoc ) {
        return toXMP( xmpDoc, XMP_IPTC_NS, XMP_IPTC_PREFIX );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case IPTC_DATE_CREATED:
            case IPTC_DATE_SENT:
            case IPTC_DIGITAL_CREATION_DATE:
            case IPTC_EXPIRATION_DATE:
            case IPTC_RELEASE_DATE: {
                //
                // IPTC dates don't have times (those are stored seperately --
                // see below), so we chop off the "00:00:00" from the Date
                // value.
                //
                final String s = value.getStringValue();
                if ( s.length() != 19 )             // YYYY:MM:DD 00:00:00
                    break;
                if ( !s.endsWith( "00:00:00" ) )
                    break;
                return s.substring( 0, 11 );
            }
            case IPTC_DIGITAL_CREATION_TIME:
            case IPTC_EXPIRATION_TIME:
            case IPTC_RELEASE_TIME:
            case IPTC_TIME_CREATED:
            case IPTC_TIME_SENT: {
                final String s = value.getStringValue();
                if ( s.length() != 11 )             // HHMMSS+HHMM
                    break;
                return  s.substring( 0, 2 ) + ':' + // HH
                        s.substring( 2, 4 ) + ':' + // MM
                        s.substring( 4, 6 ) + ' ' + // SS
                        s.substring( 6 );           // GMT offset
            }

        }
        return super.valueToString( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getProviderPriorityFor(
        Class<? extends ImageMetadataProvider> provider )
    {
        return  provider == ArtistProvider.class    ||
                provider == CaptionProvider.class   ||
                provider == CopyrightProvider.class ||
                provider == TitleProvider.class ?
                    PROVIDER_PRIORITY_DEFAULT + 1000 :
                    super.getProviderPriorityFor( provider );
    }

    /**
     * Get the {@link ResourceBundle} to use for tags.
     *
     * @return Returns said {@link ResourceBundle}.
     */
    @Override
    protected ResourceBundle getTagLabelBundle() {
        return m_tagBundle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends ImageMetaTags> getTagsInterface() {
        return IPTCTags.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<Element> toXMP( Document xmpDoc, String nsURI,
                                         String prefix ) {
        Element rdfDescElement = null;
        //
        // First we have to scan through all the metadata values to see if at
        // least one of them is one of the Creator Contact Info values because
        // they need special handling.
        //
        Element cciElement = null;
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i = iterator();
              i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            final int tagID = me.getKey();
            if ( tagIsCreatorContactInfo( tagID ) ) {
                rdfDescElement = XMPUtil.createRDFDescription(
                    xmpDoc, nsURI, prefix
                );
                cciElement = XMLUtil.addElementChildTo(
                    rdfDescElement, nsURI, prefix + ':' + "CreatorContactInfo"
                );
                cciElement.setAttribute(
                    XMP_RDF_PREFIX + ":parseType", "Resource"
                );
                rdfDescElement.appendChild( cciElement );
                break;
            }
        }

        //
        // Now go through the metadata values for real and convert them to XMP.
        // If a given metadata value is one of the Creator Contact Info values,
        // make its parent the Creator Contact Info element rather than the
        // given parent.
        //
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i = iterator();
              i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            final int tagID = me.getKey();
            ImageMetaValue value = me.getValue();

            //
            // Only these tags are encoded as part of IPTC for XMP Core.
            //
            switch ( tagID ) {
                case IPTC_COUNTRY_CODE:
                case IPTC_INTELLECTUAL_GENRE:
                case IPTC_LOCATION:
                case IPTC_SCENE:
                case IPTC_SUBJECT_CODE:
                    break;
                default:
                    if ( !tagIsCreatorContactInfo( tagID ) )
                        continue;
            }

/*
            //
            // In XMP, IPTC time fields are merged into the corresponding date
            // field.
            //
            switch ( tagID ) {
                case IPTC_DATE_CREATED:
                    value = mergeDateTime( value, IPTC_TIME_CREATED );
                    break;
                case IPTC_DATE_SENT:
                    value = mergeDateTime( value, IPTC_TIME_SENT );
                    break;
                case IPTC_DIGITAL_CREATION_DATE:
                    value = mergeDateTime( value, IPTC_DIGITAL_CREATION_TIME );
                    break;
                case IPTC_EXPIRATION_DATE:
                    value = mergeDateTime( value, IPTC_EXPIRATION_TIME );
                    break;
                case IPTC_RELEASE_DATE:
                    value = mergeDateTime( value, IPTC_RELEASE_TIME );
                    break;
                case IPTC_DIGITAL_CREATION_TIME:
                case IPTC_EXPIRATION_TIME:
                case IPTC_RELEASE_TIME:
                case IPTC_TIME_CREATED:
                case IPTC_TIME_SENT:
                    continue;
            }

*/
            final Element valueElement = value.toXMP( xmpDoc, nsURI, prefix );
            if ( valueElement != null ) {
                final Element parent;
                if ( tagIsCreatorContactInfo( tagID ) )
                    parent = cciElement;
                else {
                    if ( rdfDescElement == null )
                        rdfDescElement = XMPUtil.createRDFDescription(
                            xmpDoc, nsURI, prefix
                        );
                    parent = rdfDescElement;
                }
                //noinspection ConstantConditions
                parent.appendChild( valueElement );
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
     * Add the tag mappings.
     *
     * @param id The tag's ID.
     * @param name The tag's name.
     * @param type The tag's {@link ImageMetaType}.
     * @param isChangeable Whether the tag is user-changeable.
     * @param tagAttributes A bit-mask specifying the tag attributes.
     */
    private static void add( int id, String name, ImageMetaType type,
                             boolean isChangeable, int tagAttributes ) {
        final IPTCTagInfo tagInfo =
            new IPTCTagInfo( id, name, type, isChangeable, tagAttributes );
        m_tagsByID.put( id, tagInfo );
        m_tagsByName.put( name, tagInfo );
    }

    /**
     * Calculate the size of this IPTC directory if if were to be encoded
     * inside a JPEG image.
     *
     * @return Returns the size in bytes.
     */
    private int calcEncodedIPTCSize() {
        int size = 0;
        for ( Iterator<Map.Entry<Integer,ImageMetaValue>> i = iterator();
              i.hasNext(); ) {
            final Map.Entry<Integer,ImageMetaValue> me = i.next();
            final ImageMetaValue imValue = me.getValue();
            switch ( imValue.getType() ) {
                case META_SBYTE:
                case META_UBYTE:
                    size += IPTC_ENTRY_HEADER_SIZE + 1;
                    break;
                case META_DATE:
                    size += IPTC_ENTRY_HEADER_SIZE + IPTC_DATE_SIZE;
                    break;
                case META_SSHORT:
                case META_USHORT:
                    size += IPTC_ENTRY_HEADER_SIZE + IPTC_SHORT_SIZE;
                    break;
                case META_STRING:
                    for ( String s : imValue.getValues() ) {
                        try {
                            final byte[] b = s.getBytes( "UTF-8" );
                            size += IPTC_ENTRY_HEADER_SIZE + b.length;
                        }
                        catch ( UnsupportedEncodingException e ) {
                            throw new IllegalStateException( e );
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
        return size;
    }

    /**
     * Encode an {@link IPTCDirectory}'s values into a {@link ByteBuffer}
     * suitable for writing into a JPEG image file.
     *
     * @param includePhotoshopHeader If <code>true</code>, encode the Photoshop
     * header as is needed in JPEG files.
     * @return Returns said {@link ByteBuffer}.
     */
    private ByteBuffer encodeImpl( boolean includePhotoshopHeader ) {
        final int encodedIPTCSize = calcEncodedIPTCSize();
        int bufSize = encodedIPTCSize;

        ////////// Encode Photoshop IPTC header, if wanted ////////////////////

        if ( includePhotoshopHeader )
            bufSize += IPTC_JPEG_HEADER_SIZE;

        //
        // Ensure the buffer size is padded to be an even number of bytes.
        //
        final ByteBuffer buf = ByteBuffer.allocate( bufSize + (bufSize & 1) );

        if ( includePhotoshopHeader ) {
            ByteBufferUtil.put( buf, PHOTOSHOP_3_IDENT, "ASCII" );
            buf.put( (byte)0 );
            ByteBufferUtil.put( buf, PHOTOSHOP_CREATOR_CODE, "ASCII" );
            buf.putShort( PHOTOSHOP_IPTC_RESOURCE_ID );
            buf.putShort( (short)0 );   // resource name (empty)
            buf.putInt( encodedIPTCSize );
        }

        ////////// Now encode all the IPTC tags ///////////////////////////////

        final Integer[] tagIDs =
            getTagIDSet( false ).toArray( new Integer[]{ null } );
        Arrays.sort( tagIDs );

        for ( int tagID : tagIDs ) {
            if ( !tagIsIIM( tagID ) ) {
                //
                // Non-IIM tags can't be encoded into binary form.
                //
                continue;
            }
            final ImageMetaValue imValue = getValue( tagID );
            switch ( imValue.getType() ) {
                case META_SBYTE:
                case META_UBYTE: {
                    encodeTag( buf, tagID );
                    buf.putShort( (short)1 );
                    buf.put( imValue.getStringValue().getBytes()[0] );
                    break;
                }
                case META_DATE: {
                    encodeTag( buf, tagID );
                    String date = imValue.getStringValue();
                    date =  date.substring( 0, 4 ) +    // YYYY
                            date.substring( 5, 7 ) +    // MM
                            date.substring( 8, 10 );    // DD
                    encodeString( buf, date );
                    break;
                }
                case META_SSHORT:
                case META_USHORT: {
                    encodeTag( buf, tagID );
                    buf.putShort( (short)2 );
                    buf.putShort( imValue.getShortValue() );
                    break;
                }
                case META_STRING: {
                    for ( String s : imValue.getValues() ) {
                        encodeTag( buf, tagID );
                        encodeString( buf, s );
                    }
                    break;
                }
                default:
                    throw new IllegalStateException();
            }
        }

        if ( (bufSize & 1) != 0 ) {
            //
            // The number of encoded bytes is odd: pad to make even.
            //
            buf.put( (byte)0 );
        }

        return buf;
    }

    /**
     * Encode a {@link String}.
     *
     * @param buf The {@link ByteBuffer} to use.
     * @param s The {@link String} to encode.
     */
    private static void encodeString( ByteBuffer buf, String s ) {
        try {
            final byte[] b = s.getBytes( "UTF-8" );
            buf.putShort( (short)b.length );
            buf.put( b );
        }
        catch ( UnsupportedEncodingException e ) {
            throw new IllegalStateException( e );
        }
    }

    /**
     * Encode an {@link IPTCDirectory} tag byte sequence suitable for writing
     * into a JPEG image file.
     *
     * @param buf The {@link ByteBuffer} to use.
     * @param tagID The tag ID to encode.
     */
    private static void encodeTag( ByteBuffer buf, int tagID ) {
        buf.put( IPTC_TAG_START_BYTE );
        buf.putShort( (short)tagID );
    }

    /**
     * Merges the given {@link DateMetaValue} and a time into a new
     * {@link DateMetaValue}.
     *
     * @param date The starting {@link DateMetaValue}.  It is not modified.
     * @param timeTagID The ID of the tag of the {@link ImageMetaValue} that
     * contains a time in the form HHMMSS+HHMM.  (This is the form of all IPTC
     * time values.)
     * @return Returns a new {@link DateMetaValue} that is the date of the old
     * {@link DateMetaValue} plus the time.
     */
    private ImageMetaValue mergeDateTime( ImageMetaValue date, int timeTagID ) {
        final ImageMetaValue timeValue = getValue( timeTagID );
        if ( timeValue == null )
            return date;
        final String timeString = timeValue.getStringValue();
        if ( timeString.length() != 11 )
            return date;

        try {
            final int hh = Integer.parseInt( timeString.substring( 0, 2 ) );
            final int mm = Integer.parseInt( timeString.substring( 2, 4 ) );
            final int ss = Integer.parseInt( timeString.substring( 4, 6 ) );

            int zh = Integer.parseInt( timeString.substring( 7, 9 ) );
            int zm = Integer.parseInt( timeString.substring( 9, 11 ) );
            //
            // We have to adjust the hour and minute by the timezone offset and
            // we need to do this based on GMT.
            //
            // For an offset like -08:00 (i.e., somewhere on the west coast of
            // the USA), we need to add 8 hours so midnight on the west coast
            // is 8am GMT.  To get this result, we don't have to do anything to
            // the sign of zh and zm.
            //
            // For an offset like +08:00 (i.e., somewhere on the west coast of
            // Australia), we need to substract 8 hours.  To get this result,
            // we negate zh and zm.
            //
            switch ( timeString.charAt( 6 ) ) {
                case '+':
                    zh = -zh;
                    zm = -zm;
                    break;
                case '-':
                    break;
                default:
                    return date;
            }

            final int delta = ((hh+zh) * 60 * 60 + (mm+zm) * 60 + ss) * 1000;

            final DateMetaValue newDateValue = (DateMetaValue)date.clone();
            final Date newDate = newDateValue.getDateValue();
            newDate.setTime( newDate.getTime() + delta );
            return newDateValue;
        }
        catch ( NumberFormatException e ) {
            return date;
        }
    }

    /**
     * Add a second {@link IPTCTagInfo} for an IIM IPTC tag's XMP information.
     *
     * @param id The tag ID.
     * @param name The XMP name.
     * @see #add(int,String,ImageMetaType,boolean,int)
     */
    private static void xmp( int id, String name ) {
        final IPTCTagInfo iimTagInfo = m_tagsByID.get( id );
        final IPTCTagInfo xmpTagInfo = new IPTCTagInfo(
            id, name, iimTagInfo.getType(), iimTagInfo.isChangeable(),
            iimTagInfo.getAttributes()
        );
        m_tagsByID.put( id, xmpTagInfo );
        m_tagsByName.put( name, iimTagInfo );
    }

    /**
     * This is where the actual labels for the tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.IPTCTags"
    );

    /**
     * A mapping of tags by ID.
     */
    private static final Map<Integer,IPTCTagInfo> m_tagsByID =
        new HashMap<Integer,IPTCTagInfo>();

    /**
     * A mapping of tags by name.
     */
    private static final Map<String,IPTCTagInfo> m_tagsByName =
        new HashMap<String,IPTCTagInfo>();

    static {
        //
        // These are the original IPTC tags having IDs.
        //
        add( IPTC_BY_LINE, "ByLine", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_BY_LINE_TITLE, "ByLineTitle", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_CAPTION_ABSTRACT, "Caption/Abstract", META_STRING, true, 0 );
        add( IPTC_CATEGORY, "Category", META_STRING, true, 0 );
        add( IPTC_CITY, "City", META_STRING, true, 0 );
        add( IPTC_CODED_CHARACTER_SET, "CodedCharacterSet", META_STRING, true, 0 );
        add( IPTC_CONTACT, "Contact", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_CONTENT_LOCATION_CODE, "ContentLocationCode", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_CONTENT_LOCATION_NAME, "ContentLocationName", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_COPYRIGHT_NOTICE, "CopyrightNotice", META_STRING, true, 0 );
        add( IPTC_COUNTRY_PRIMARY_LOCATION_CODE, "CountryPrimaryLocationCode", META_STRING, true, 0 );
        add( IPTC_COUNTRY_PRIMARY_LOCATION_NAME, "CountryPrimaryLocationName", META_STRING, true, 0 );
        add( IPTC_CREDIT, "Credit", META_STRING, true, 0 );
        add( IPTC_DATE_CREATED, "DateCreated", META_DATE, true, 0 );
        add( IPTC_DATE_SENT, "DateSent", META_DATE, true, 0 );
        add( IPTC_DESTINATION, "Destination", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_DIGITAL_CREATION_DATE, "DigitalCreationDate", META_DATE, true, 0 );
        add( IPTC_DIGITAL_CREATION_TIME, "DigitalCreationTime", META_STRING, true, 0 );
        add( IPTC_EDIT_STATUS, "EditStatus", META_STRING, true, 0 );
        add( IPTC_ENVELOPE_NUMBER, "EnvelopeNumber", META_STRING, true, 0 );
        add( IPTC_ENVELOPE_PRIORITY, "EnvelopePriority", META_UBYTE, true, 0 );
        add( IPTC_EXPIRATION_DATE, "ExpirationDate", META_DATE, true, 0 );
        add( IPTC_EXPIRATION_TIME, "ExpirationTime", META_STRING, true, 0 );
        add( IPTC_FIXTURE_IDENTIFIER, "FixtureIdentifier", META_STRING, true, 0 );
        add( IPTC_HEADLINE, "Headline", META_STRING, true, 0 );
        add( IPTC_KEYWORDS, "Keywords", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_LANGUAGE_IDENTIFIER, "LanguageIdentifier", META_STRING, true, 0 );
        add( IPTC_OBJECT_ATTRIBUTE_REFERENCE, "ObjectAttributeReference", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_OBJECT_CYCLE, "ObjectCycle", META_STRING, true, 0 );
        add( IPTC_OBJECT_NAME, "ObjectName", META_STRING, true, 0 );
        add( IPTC_ORIGINAL_TRANSMISSION_REFERENCE, "OriginalTransmissionReference", META_STRING, true, 0 );
        add( IPTC_ORIGINATING_PROGRAM, "OriginatingProgram", META_STRING, false, 0 );
        add( IPTC_PRODUCT_ID, "ProgramID", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_PROGRAM_VERSION, "ProgramVersion", META_STRING, false, 0 );
        add( IPTC_PROVINCE_STATE, "Province-State", META_STRING, true, 0 );
        add( IPTC_RECORD_VERSION, "RecordVersion", META_USHORT, false, 0 );
        add( IPTC_RELEASE_DATE, "ReleaseDate", META_DATE, true, 0 );
        add( IPTC_RELEASE_TIME, "ReleaseTime", META_STRING, true, 0 );
        add( IPTC_SERVICE_IDENTIFIER, "ServiceIdentifier", META_STRING, true, 0 );
        add( IPTC_SOURCE, "Source", META_STRING, true, 0 );
        add( IPTC_SPECIAL_INSTRUCTIONS, "SpecialInstructions", META_STRING, true, 0 );
        add( IPTC_SUBLOCATION, "Sublocation", META_STRING, true, 0 );
        add( IPTC_SUPPLEMENTAL_CATEGORIES, "SupplementalCategories", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_TIME_CREATED, "TimeCreated", META_STRING, true, 0 );
        add( IPTC_TIME_SENT, "TimeSent", META_STRING, true, 0 );
        add( IPTC_UNO, "UNO", META_STRING, true, 0 );
        add( IPTC_URGENCY, "Urgency", META_UBYTE, true, 0 );
        add( IPTC_WRITER_EDITOR, "Writer/Editor", META_STRING, true, IPTC_TAG_MULTIVALUE );

        //
        // These are the new IPTC tags as part of the IPTC core schema for XMP.
        //
        add( IPTC_CI_ADDRESS, "CiAdrExtadr", META_STRING, true, IPTC_TAG_MULTILINE );
        add( IPTC_CI_CITY, "CiAdrCity", META_STRING, true, 0 );
        add( IPTC_CI_COUNTRY, "CiAdrCtry", META_STRING, true, 0 );
        add( IPTC_CI_EMAILS, "CiEmailWork", META_STRING, true, 0 );
        add( IPTC_CI_PHONES, "CiTelWork", META_STRING, true, 0 );
        add( IPTC_CI_POSTAL_CODE, "CiAdrPcode", META_STRING, true, 0 );
        add( IPTC_CI_STATE_PROVINCE, "CiAdrRegion", META_STRING, true, 0 );
        add( IPTC_CI_WEB_URLS, "CiUrlWork", META_STRING, true, 0 );
        add( IPTC_COUNTRY_CODE, "CountryCode", META_STRING, true, 0 );
        add( IPTC_CREATOR_CONTACT_INFO, "CreatorContactInfo", META_UNKNOWN, false, 0 );
        add( IPTC_INTELLECTUAL_GENRE, "IntellectualGenre", META_STRING, true, 0 );
        add( IPTC_LOCATION, "Location", META_STRING, true, 0 );
        add( IPTC_RIGHTS_USAGE_TERMS, "RightsUsageTerms", META_STRING, true, 0 );
        add( IPTC_SCENE, "Scene", META_STRING, true, IPTC_TAG_MULTIVALUE );
        add( IPTC_SUBJECT_CODE, "SubjectCode", META_STRING, true, IPTC_TAG_MULTIVALUE );

        //
        // These are the IPTC tags that have new names under the IPTC core
        // schema for XMP.
        //
        xmp( IPTC_COUNTRY, "Country" );
        xmp( IPTC_COUNTRY_CODE, "CountryCode" );
        xmp( IPTC_CREATOR, "Creator" );
        xmp( IPTC_CREATOR_JOBTITLE, "CreatorJobtitle" );
        xmp( IPTC_DESCRIPTION, "Description" );
        xmp( IPTC_DESCRIPTION_WRITER, "DescriptionWriter" );
        xmp( IPTC_INSTRUCTIONS, "Instructions" );
        xmp( IPTC_INTELLECTUAL_GENRE, "IntellectualGenre" );
        xmp( IPTC_JOB_ID, "JobID" );
        xmp( IPTC_LOCATION, "Location" );
        xmp( IPTC_PROVIDER, "Provider" );
        xmp( IPTC_TITLE, "Title" );

        //
        // These are the IPTC tags that have new names under the Dublin Core
        // schema for XMP.
        //
        xmp( IPTC_COPYRIGHT_NOTICE, "rights" );
        xmp( IPTC_CREATOR, "creator" );
        xmp( IPTC_DESCRIPTION, "description" );
        xmp( IPTC_TITLE, "title" );
    }
}
/* vim:set et sw=4 ts=4: */
