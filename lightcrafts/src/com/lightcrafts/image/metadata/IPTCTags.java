/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import static com.lightcrafts.image.metadata.IPTCConstants.*;

/**
 * An <code>IPTCTags</code> defines the constants used for IPTC metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface IPTCTags extends ImageMetaTags {

    ////////// IPTC Information Interchange Model (IIM)  //////////////////////

    /**
     * Contains preferably the name of the person who created the content of
     * this news object, a photographer for photos, a graphic artist for
     * graphics, or a writer for textual news. If it is not appropriate to add
     * the name of a person the name of a company or organisation could be
     * applied as well.
     * <p>
     * Type: ASCII.
     * @see #IPTC_CREATOR
     */
    int IPTC_BY_LINE                            = IPTC_RECORD_APP << 8 | 0x50;

    /**
     * Contains the job title of the person who created the content of this
     * news object. As this is sort of a qualifier the Creator element has to
     * be filled in as mandatory prerequisite for using Creator's Jobtitle.
     * <p>
     * Type: ASCII.
     * @see #IPTC_CREATOR_JOBTITLE
     */
    int IPTC_BY_LINE_TITLE                      = IPTC_RECORD_APP << 8 | 0x55;

    /**
     * A textual description, including captions of the news object's content,
     * particularly used where the object is not text.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CAPTION_ABSTRACT                   = IPTC_RECORD_APP << 8 | 0x78;

    /**
     * To denote the category of a story.  Composition: one, two or three alpha
     * characters, upper and lower case in any combination, e.g., "Fin" for
     * financial or "Pol" for political, etc.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CATEGORY                           = IPTC_RECORD_APP << 8 | 0x0F;

    /**
     * Name of the city the content is focussing on -- either the place shown
     * in visual media or referenced by text or audio media. This element is at
     * the third level of a top-down geographical hierarchy.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CITY                               = IPTC_RECORD_APP << 8 | 0x5A;

    /**
     * If the value of this tag is 0x1B 0x25 0x47, strings in IPTC_RECORD_APP
     * tags are decoded as UTF-8. 
     * <p>
     * Type: ASCII.
     */
    int IPTC_CODED_CHARACTER_SET                = IPTC_RECORD_ENV << 8 | 0x5A;

    /**
     * Identifies the person or organisation that can provide further
     * background information on the object data.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CONTACT                            = IPTC_RECORD_APP << 8 | 0x76;

    /**
     * Indicates the code of a country/geographical location referenced by the
     * object.  Where ISO has established an appropriate country code under ISO
     * 3166, that code will be used.  When ISO 3166 does not adequately provide
     * for identification of a location or a country, e.g., ships at sea,
     * space, IPTC will assign an appropriate three-character code under the
     * provisions of ISO 3166 to avoid conflicts.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CONTENT_LOCATION_CODE              = IPTC_RECORD_APP << 8 | 0x1A;

    /**
     * Provides a full, publishable name of a country/geographical location
     * referenced by the content of the object, according to guidelines of the
     * provider.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CONTENT_LOCATION_NAME              = IPTC_RECORD_APP << 8 | 0x1B;

    /**
     * Contains any necessary copyright notice for claiming the intellectual
     * property for this news ject and should identify the current owner of the
     * copyright for the news object. Other tities like the creator of the news
     * object may be added. Notes on usage rights should be ovided in
     * {@link #IPTC_RIGHTS_USAGE_TERMS}.
     * <p>
     * Type: ASCII.
     */
    int IPTC_COPYRIGHT_NOTICE                   = IPTC_RECORD_APP << 8 | 0x74;

    /**
     * Indicates the code of the country/primary location where the
     * intellectual property of the object data was created, e.g., a photo was
     * taken, an event occurred.
     * <p>
     * Type: ASCII.
     */
    int IPTC_COUNTRY_PRIMARY_LOCATION_CODE      = IPTC_RECORD_APP << 8 | 0x64;

    /**
     * Provides full, publishable, name of the country/primary location where
     * the intellectual property of the object data was created, according to
     * guidelines of the provider.
     * <p>
     * Type: ASCII.
     */
    int IPTC_COUNTRY_PRIMARY_LOCATION_NAME      = IPTC_RECORD_APP << 8 | 0x65;

    /**
     * Identifies the provider of the news object, who is not necessarily the
     * owner/creator.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CREDIT                             = IPTC_RECORD_APP << 8 | 0x6E;

    /**
     * Designates the date and optionally the time the intellectual content of
     * the news object was created rather than the date of the creation of the
     * physical representation. If no time is given the value should default to
     * 00:00:00.
     * <p>
     * Type: ASCII.
     */
    int IPTC_DATE_CREATED                       = IPTC_RECORD_APP << 8 | 0x37;

    /**
     * Uses the format CCYYMMDD (century, year, month, day) as defined in ISO
     * 8601 to indicate year, month and day the service sent the material.
     * <p>
     * Type: ASCII.
     */
    int IPTC_DATE_SENT                          = IPTC_RECORD_ENV << 8 | 0x46;

    /**
     * This is to accommodate some providers who require routing information
     * above the appropriate OSI layers.
     * <p>
     * Type: ASCII.
     */
    int IPTC_DESTINATION                        = IPTC_RECORD_ENV << 8 | 0x05;

    /**
     * Represented in the form CCYYMMDD to designate the date the digital
     * representation of the objectdata was created.  Follows ISO 8601
     * standard.  Thus a photo taken during the American Civil War would carry
     * a Digital Creation Date within the past several years rather than the
     * date where the image was captured on film, glass plate or other
     * substrate during that epoch (1861-1865).
     * <p>
     * Type: ASCII.
     */
    int IPTC_DIGITAL_CREATION_DATE              = IPTC_RECORD_APP << 8 | 0x3E;

    /**
     * Represented in the form HHMMSS+/-HHMM to designate the time the digital
     * representation of the objectdata was created.  Follows ISO 8601
     * standard.
     * <p>
     * Type: ASCII.
     */
    int IPTC_DIGITAL_CREATION_TIME              = IPTC_RECORD_APP << 8 | 0x3F;

    /**
     * Status of the object data, according to the practice of the provider.
     * <p>
     * Type: ASCII.
     */
    int IPTC_EDIT_STATUS                        = IPTC_RECORD_APP << 8 | 0x07;

    /**
     * The characters form a number that will be unique.
     * <p>
     * Type: ASCII.
     */
    int IPTC_ENVELOPE_NUMBER                    = IPTC_RECORD_ENV << 8 | 0x28;

    /**
     * Specifies the envelope handling priority and not the editorial urgency
     * (see #IPTC_URGENCY).  1' indicates the most urgent, '5' the normal
     * urgency, and '8' the least urgent copy. The numeral '9' indicates a User
     * Defined Priority. The numeral '0' is reserved for future use.
     * <p>
     * Type: ASCII
     */
    int IPTC_ENVELOPE_PRIORITY                  = IPTC_RECORD_ENV << 8 | 0x3C;

    /**
     * Designates in the form CCYYMMDD the latest date the provider or owner
     * intends the object data to be used.  Follows ISO 8601 standard.
     * <p>
     * Type: ASCII.
     */
    int IPTC_EXPIRATION_DATE                    = IPTC_RECORD_APP << 8 | 0x25;

    /**
     * Designates in the form HHMMSS+/-HHMM the latest time the provider or
     * owner intends the object data to be used.  Follows ISO 8601 standard.
     * <p>
     * Type: ASCII.
     */
    int IPTC_EXPIRATION_TIME                    = IPTC_RECORD_APP << 8 | 0x26;

    /**
     * Identifies object data that recurs often and predictably.  Enables users
     * to immediately find or recall such an object.
     * <p>
     * Type: ASCII.
     */
    int IPTC_FIXTURE_IDENTIFIER                 = IPTC_RECORD_APP << 8 | 0x16;

    /**
     * A publishable entry providing a synopsis of the contents of the news
     * object. Headline is not the same as Title.
     * <p>
     * Type: ASCII.
     */
    int IPTC_HEADLINE                           = IPTC_RECORD_APP << 8 | 0x69;

    /**
     * Keywords to express the subject of the content. Keywords may be free
     * text and don't have to be taken from a controlled vocabulary. Values
     * from the controlled vocabulary IPTC Subject Codes must go to the
     * {@link #IPTC_SUBJECT_CODE} element.
     * <p>
     * Type: ASCII.
     */
    int IPTC_KEYWORDS                           = IPTC_RECORD_APP << 8 | 0x19;

    /**
     * Describes the major national language of the object, according to the
     * 2-letter codes of ISO 639:1988.  Does not define or imply any coded
     * character set, but is used for internal routing, e.g., to various
     * editorial desks.
     * <p>
     * Type: ASCII.
     */
    int IPTC_LANGUAGE_IDENTIFIER                = IPTC_RECORD_APP << 8 | 0x83;

    /**
     * Defines the nature of the object independent of the subject.
     * <p>
     * Type: ASCII.
     */
    int IPTC_OBJECT_ATTRIBUTE_REFERENCE         = IPTC_RECORD_APP << 8 | 0x04;

    /**
     * News cycle:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td><code>a</code> = &nbsp;</td><td>morning</td></tr>
     *      <tr><td><code>p</code> = &nbsp;</td><td>evening</td></tr>
     *      <tr><td><code>b</code> = &nbsp;</td><td>both</td></tr>
     *    </table>
     *  </blockquote>
     * Type: ASCII.
     */
    int IPTC_OBJECT_CYCLE                       = IPTC_RECORD_APP << 8 | 0x4B;

    /**
     * A shorthand reference for the news object. While a technical identifer
     * goes to an identifier element, Title holds a short verbal and human
     * readable name. Title is not the same as {@link #IPTC_HEADLINE}.
     * <p>
     * Type: ASCII.
     */
    int IPTC_OBJECT_NAME                        = IPTC_RECORD_APP << 8 | 0x05;

    /**
     * Number or identifier for the purpose of improved workflow handling. This
     * ID should be added by the creator or provider for transmission and
     * routing purposes only and should have no significance for archiving.
     * <p>
     * Type: ASCII.
     */
    int IPTC_ORIGINAL_TRANSMISSION_REFERENCE    = IPTC_RECORD_APP << 8 | 0x67;

    /**
     * Identifies the type of program used to originate the object data.
     * <p>
     * Type: ASCII.
     */
    int IPTC_ORIGINATING_PROGRAM                = IPTC_RECORD_APP << 8 | 0x41;

    /**
     * Allows a provider to identify subsets of its overall service. Used to
     * provide receiving organisation data on which to select, route, or
     * otherwise handle data.
     * <p>
     * Type: ASCII
     */
    int IPTC_PRODUCT_ID                         = IPTC_RECORD_ENV << 8 | 0x32;

    /**
     * Used to identify the version of the program in
     * {@link #IPTC_ORIGINATING_PROGRAM}.
     * <p>
     * Type: ASCII.
     */
    int IPTC_PROGRAM_VERSION                    = IPTC_RECORD_APP << 8 | 0x42;

    /**
     * Name of the subregion of a country -- either called province or state or
     * anything else -- the content is focussing on -- either the subregion
     * shown in visual media or referenced by text or audio media. This element
     * is at the second level of a top-down geographical hierarchy.
     * <p>
     * Type: ASCII.
     */
    int IPTC_PROVINCE_STATE                     = IPTC_RECORD_APP << 8 | 0x5F;

    /**
     * The version of IPTC metadata.
     * <p>
     * Type: Unsigned short.
     */
    int IPTC_RECORD_VERSION                     = IPTC_RECORD_APP << 8;

    /**
     * The release date of the news obejct.
     * <p>
     * Type: ASCII.
     */
    int IPTC_RELEASE_DATE                       = IPTC_RECORD_APP << 8 | 0x1E;

    /**
     * The release time of the news object.
     * <p>
     * Type: ASCII.
     */
    int IPTC_RELEASE_TIME                       = IPTC_RECORD_APP << 8 | 0x23;

    /**
     * Identifies the provider and product.
     * <p>
     * Type: ASCII.
     */
    int IPTC_SERVICE_IDENTIFIER                 = IPTC_RECORD_ENV << 8 | 0x1E;

    /**
     * Identifies the original owner of the copyright for the intellectual
     * content of the news object.  This could be an agency, a member of an
     * agency or an individual. Source could be different from
     * {@link #IPTC_CREATOR} and from the entities in the
     * {@link #IPTC_COPYRIGHT_NOTICE}.
     * <p>
     * Type: ASCII.
     */
    int IPTC_SOURCE                             = IPTC_RECORD_APP << 8 | 0x73;

    /**
     * Any of a number of instructions from the provider or creator to the
     * receiver of the news object which might include any of the following:
     * embargoes (NewsMagazines OUT) and other restrictions not covered by the
     * {@link #IPTC_RIGHTS_USAGE_TERMS} field; information regarding the
     * original means of capture (scanning notes, colourspace info) or other
     * specific text information that the user may need for accurate
     * reproduction; additional permissions or credits required when
     * publishing.
     * <p>
     * Type: ASCII.
     */
    int IPTC_SPECIAL_INSTRUCTIONS               = IPTC_RECORD_APP << 8 | 0x28;

    /**
     * Identifies the location within a city from which the object data
     * originates according to guidelines established by the provider.
     * <p>
     * Type: ASCII.
     */
    int IPTC_SUBLOCATION                        = IPTC_RECORD_APP << 8 | 0x5C;

    /**
     * Categories supplemental to {@link #IPTC_CATEGORY}.
     * <p>
     * Type: ASCII.
     */
    int IPTC_SUPPLEMENTAL_CATEGORIES            = IPTC_RECORD_APP << 8 | 0x14;

    /**
     * The time the news object was created.
     * <p>
     * Type: ASCII.
     */
    int IPTC_TIME_CREATED                       = IPTC_RECORD_APP << 8 | 0x3C;

    /**
     * Uses the format HHMMSS+/-HHMM where HHMMSS refers to local hour, minute
     * and seconds and HHMM refers to hours and minutes ahead (+) or behind (-)
     * Universal Coordinated Time as described in ISO 8601.  This is the time
     * the service sent the material.
     * <p>
     * Type: ASCII.
     */
    int IPTC_TIME_SENT                          = IPTC_RECORD_ENV << 8 | 0x50;

    /**
     * UNO Unique Name of Object, providing eternal, globally unique
     * identification for objects as specified in the IIM, independent of
     * provider and for any media form.  The provider must ensure the UNO is
     * unique.  Objects with the same UNO are identical.
     * <p>
     * Type: ASCII.
     */
    int IPTC_UNO                                = IPTC_RECORD_ENV << 8 | 0x64;

    /**
     * To indicate the editorial urgency of a story.  Composition: one numeral
     * from a scale ranging from 1 for the most urgent, 5 for normal, and 8 for
     * the least urgent.
     * <p>
     * Type: Unsigned byte.
     */
    int IPTC_URGENCY                            = IPTC_RECORD_APP << 8 | 0x0A;

    /**
     * Identifier or the name of the person involved in writing, editing or
     * correcting the description of the news object.
     * <p>
     * Type: ASCII.
     */
    int IPTC_WRITER_EDITOR                      = IPTC_RECORD_APP << 8 | 0x7A;

    ////////// New for IPTC XMP Core //////////////////////////////////////////

    /**
     * The creator's contact information.  This is actually comprised of all
     * the <code>IPTC_CI_</code> tags and doesn't have a value itself.
     */
    int IPTC_CREATOR_CONTACT_INFO               = 0xF100;

    /**
     * The contact information address part. Comprises an optional company name
     * and all required information to locate the building or postbox to which
     * mail should be sent. To that end, the address is a multiline field.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CI_ADDRESS                         = 0xF101;

    /**
     * The contact information city part.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CI_CITY                            = 0xF102;

    /**
     * The contact information country part.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CI_COUNTRY                         = 0xF103;

    /**
     * The contact information email address part. Multiple email addresses can
     * be given, separated by a comma.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CI_EMAILS                          = 0xF104;

    /**
     * The contact information phone number part. Multiple numbers can be
     * given, separated by a comma.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CI_PHONES                          = 0xF105;

    /**
     * The contact information part denoting the local postal code.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CI_POSTAL_CODE                     = 0xF106;

    /**
     * The contact information part denoting regional information like state or
     * province.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CI_STATE_PROVINCE                  = 0xF107;

    /**
     * The contact information web address part. Multiple addresses can be
     * given, separated by a comma.
     * <p>
     * Type: ASCII.
     */
    int IPTC_CI_WEB_URLS                        = 0xF108;

    /**
     * Free text instructions on how this news object can be legally used.
     * <p>
     * Type: ASCII.
     */
    int IPTC_RIGHTS_USAGE_TERMS                 = 0xF204;

    /**
     * Describes the scene of a photo content. Specifies one ore more terms
     * from the IPTC "Scene-NewsCodes". Each Scene is represented as a string
     * of 6 digits in an unordered list.
     * <p>
     * Type: ASCII.
     */
    int IPTC_SCENE                              = 0xF205;

    /**
     * Specifies one or more Subjects from the IPTC "Subject-NewsCodes"
     * taxonomy to categorize the content. Each Subject is represented as a
     * string of 8 digits in an unordered list.
     * <p>
     * Type: ASCII.
     */
    int IPTC_SUBJECT_CODE                       = 0xF206;

    ////////// New IPTC XMP Core names for original IPTC headers //////////////

    int IPTC_COUNTRY            = IPTC_COUNTRY_PRIMARY_LOCATION_NAME;
    int IPTC_COUNTRY_CODE       = IPTC_COUNTRY_PRIMARY_LOCATION_CODE;
    int IPTC_CREATOR            = IPTC_BY_LINE;
    int IPTC_CREATOR_JOBTITLE   = IPTC_BY_LINE_TITLE;
    int IPTC_DESCRIPTION        = IPTC_CAPTION_ABSTRACT;
    int IPTC_DESCRIPTION_WRITER = IPTC_WRITER_EDITOR;
    int IPTC_INSTRUCTIONS       = IPTC_SPECIAL_INSTRUCTIONS;
    int IPTC_INTELLECTUAL_GENRE = IPTC_OBJECT_ATTRIBUTE_REFERENCE;
    int IPTC_JOB_ID             = IPTC_ORIGINAL_TRANSMISSION_REFERENCE;
    int IPTC_LOCATION           = IPTC_SUBLOCATION;
    int IPTC_PROVIDER           = IPTC_CREDIT;
    int IPTC_TITLE              = IPTC_OBJECT_NAME;

}
/* vim:set et sw=4 ts=4: */
