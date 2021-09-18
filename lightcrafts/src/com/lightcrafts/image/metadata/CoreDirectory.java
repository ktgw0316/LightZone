/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.image.types.AuxiliaryImageInfo;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.RawImageInfo;
import com.lightcrafts.utils.DCRaw;
import com.lightcrafts.utils.TextUtil;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.xml.XMLUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.color.ICC_Profile;
import java.io.File;
import java.util.*;

import static com.lightcrafts.image.metadata.CoreTags.*;
import static com.lightcrafts.image.metadata.ImageMetaType.*;
import static com.lightcrafts.image.metadata.ImageOrientation.ORIENTATION_LANDSCAPE;
import static com.lightcrafts.image.metadata.ImageOrientation.ORIENTATION_UNKNOWN;
import static com.lightcrafts.image.metadata.XMPConstants.*;

/**
 * A <code>CoreDirectory</code> is-an {@link ImageMetadataDirectory} for
 * holding core metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings({"CloneableClassWithoutClone"})
public final class CoreDirectory extends ImageMetadataDirectory implements
    ApertureProvider, ArtistProvider, CaptionProvider, CaptureDateTimeProvider,
    ColorTemperatureProvider, CopyrightProvider, FileDateTimeProvider,
    FlashProvider, FocalLengthProvider, ISOProvider, LensProvider,
    MakeModelProvider, OrientationProvider, OriginalWidthHeightProvider,
    RatingProvider, ShutterSpeedProvider, UrgencyProvider, WidthHeightProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Adds LightZone-specific metadata to an image's set of metadata.
     *
     * @param imageInfo The image to add metadata to.
     */
    public static void addMetadata( ImageInfo imageInfo ) {
        final ImageMetadata metadata = imageInfo.getCurrentMetadata();
        final CoreDirectory dir =
            (CoreDirectory)metadata.getDirectoryFor(
                CoreDirectory.class, true
            );
        dir.addAperture( metadata );
        dir.addCamera( imageInfo );
        dir.addCaptureDateTime( imageInfo );
        dir.addColorProfile( imageInfo );
        dir.addColorTemperature( metadata );
        dir.addFileInfo( imageInfo );
        dir.addFlash( metadata );
        dir.addFocalLength( metadata );
        dir.addImageDimensions( imageInfo );
        dir.addISO( metadata );
        dir.addLens( metadata );
        dir.addOrientation( metadata );
        dir.addResolution( metadata );
        dir.addShutterSpeed( metadata );

        dir.addRating( metadata );
        dir.addUrgency( metadata );
        syncEditableMetadata( metadata );
    }

    /**
     * Synchronize the core metadata for editable values.
     *
     * @param metadata The {@link ImageMetadata} to synchronize.
     */
    public static void syncEditableMetadata( ImageMetadata metadata ) {
        final CoreDirectory dir = (CoreDirectory)
            metadata.getDirectoryFor( CoreDirectory.class, true );
        dir.addArtist( metadata );
        dir.addCaption( metadata );
        dir.addCopyright( metadata );
        //dir.addRating( metadata );
        dir.addTitle( metadata );
    }

    /**
     * Synchronize the core metadata for image dimensions.
     *
     * @param metadata The {@link ImageMetadata} to synchronize.
     */
    public static void syncImageDimensions( ImageMetadata metadata ) {
        final CoreDirectory dir = (CoreDirectory)
            metadata.getDirectoryFor( CoreDirectory.class, true );
        dir.removeValue( CORE_IMAGE_WIDTH );
        dir.removeValue( CORE_IMAGE_HEIGHT );
        final int width = metadata.getImageWidth();
        final int height = metadata.getImageHeight();
        if ( width > 0 && height > 0 ) {
            dir.putValue(
                CORE_IMAGE_WIDTH, new UnsignedShortMetaValue( width )
            );
            dir.putValue(
                CORE_IMAGE_HEIGHT, new UnsignedShortMetaValue( height )
            );
        }
    }

    /**
     * Adds the original image orientation (the orientation embedded in the
     * metadata of the image file and not merged from any XMP file) to the
     * <code>CoreDirectory</code> of the given image's metadata.
     *
     * @param imageInfo The image to add the original orientation to.
     */
    public static void addOriginalOrientation( ImageInfo imageInfo ) {
        final ImageMetadata metadata = imageInfo.getCurrentMetadata();
        final CoreDirectory dir =
            (CoreDirectory)metadata.getDirectoryFor(
                CoreDirectory.class, true
            );
        final ImageOrientation orientation = getOrientationFrom( metadata );
        dir.putValue(
            CORE_ORIGINAL_ORIENTATION,
            new UnsignedShortMetaValue( orientation.getTIFFConstant() )
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getAperture() {
        final ImageMetaValue value = getValue( CORE_APERTURE );
        return value != null ? value.getFloatValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getArtist() {
        final ImageMetaValue value = getValue( CORE_ARTIST );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCaption() {
        final ImageMetaValue value = getValue( CORE_CAPTION );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getCaptureDateTime() {
        final ImageMetaValue value = getValue( CORE_CAPTURE_DATE_TIME );
        return value != null ? ((DateMetaValue)value).getDateValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColorTemperature() {
        final ImageMetaValue value = getValue( CORE_COLOR_TEMPERATURE );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCopyright() {
        final ImageMetaValue value = getValue( CORE_COPYRIGHT );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getFileDateTime() {
        final ImageMetaValue value = getValue( CORE_FILE_DATE_TIME );
        return value != null ? ((DateMetaValue)value).getDateValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getFlash() {
        final ImageMetaValue value = getValue( CORE_FLASH );
        return value != null ? value.getIntValue() : -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFocalLength() {
        final ImageMetaValue value = getValue( CORE_FOCAL_LENGTH );
        return value != null ? value.getFloatValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageHeight() {
        final ImageMetaValue value = getValue( CORE_IMAGE_HEIGHT );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImageWidth() {
        final ImageMetaValue value = getValue( CORE_IMAGE_WIDTH );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getISO() {
        final ImageMetaValue value = getValue( CORE_ISO );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLens() {
        final ImageMetaValue value = getValue( CORE_LENS );
        return value != null ? value.getStringValue() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCameraMake( boolean includeModel ) {
        final ImageMetaValue value = getValue( CORE_CAMERA );
        if ( value != null ) {
            final String makeModel = value.getStringValue();
            if ( includeModel )
                return makeModel;
            final int space = makeModel.indexOf( ' ' );
            if ( space <= 0 || space == makeModel.length() - 1 )
                return makeModel;
            return makeModel.substring( 0, space );
        }
        return null;
    }

    /**
     * Gets the name of this directory.
     *
     * @return Always returns &quot;Core&quot;.
     */
    @Override
    public String getName() {
        return "Core";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginalImageHeight() {
        final ImageMetaValue value = getValue( CORE_ORIGINAL_IMAGE_HEIGHT );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOriginalImageWidth() {
        final ImageMetaValue value = getValue( CORE_ORIGINAL_IMAGE_WIDTH );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * Gets the absolute path to the original image file.
     *
     * @return Returns said path.
     */
    public String getPath() {
        final ImageMetaValue dirValue  = getValue( CORE_DIR_NAME );
        final ImageMetaValue fileValue = getValue( CORE_FILE_NAME );
        return  dirValue.getStringValue() + File.separatorChar +
                fileValue.getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageOrientation getOrientation() {
        final ImageMetaValue value = getValue( CORE_IMAGE_ORIENTATION );
        if ( value != null )
            return ImageOrientation.getOrientationFor( value.getIntValue() );
        return ORIENTATION_UNKNOWN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRating() {
        final ImageMetaValue value = getValue( CORE_RATING );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRating(int rating) {
        if (rating < 0 || rating > 5)
            throw new IllegalArgumentException( "rating must be between 0-5" );
        // Do not remove value even if the rating is 0.
        setValue(CORE_RATING, rating);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getShutterSpeed() {
        final ImageMetaValue value = getValue( CORE_SHUTTER_SPEED );
        return value != null ? value.getFloatValue() : 0;
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
    public int getUrgency() {
        final ImageMetaValue value = getValue( CORE_URGENCY );
        return value != null ? value.getIntValue() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Element> toXMP( Document xmpDoc ) {
        return toXMP( xmpDoc, XMP_XAP_NS, XMP_XAP_PREFIX );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String valueToString( ImageMetaValue value ) {
        switch ( value.getOwningTagID() ) {
            case CORE_APERTURE:
                return TextUtil.tenthsNoDotZero( value.getFloatValue() );
            case CORE_FILE_SIZE:
                return TextUtil.quantify( value.getLongValue() );
            case CORE_FOCAL_LENGTH:
                return TextUtil.tenthsNoDotZero( value.getFloatValue() ) + "mm";
            case CORE_RATING:
                final int rating = value.getIntValue();
                if ( rating >= 1 && rating <= 5 )
                    return FIVE_STARS.substring( 5 - rating );
                if ( rating == 0 )
                    return "-";
                break;
            case CORE_SHUTTER_SPEED:
                return MetadataUtil.shutterSpeedString( value.getFloatValue() );
            case CORE_URGENCY:
                return Integer.toString(value.getIntValue());
        }
        return super.valueToString( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Gets the priority of this directory for providing the metadata supplied
     * by implementing the given provider interface.
     * <p>
     * The <code>CoreDirectory</code> is given the highest priority because its
     * considered authoritative.
     *
     * @param provider The provider interface to get the priority for.
     * @return Returns a priority guaranteed to be higher than all others.
     */
    @Override
    protected int getProviderPriorityFor(
        Class<? extends ImageMetadataProvider> provider )
    {
        return provider == LensProvider.class ? 0 : Integer.MAX_VALUE;
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
        return CoreTags.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<Element> toXMP( Document xmpDoc, String nsURI,
                                         String prefix ) {
        final Collection<Element> elements = new ArrayList<Element>( 2 );

        ////////// EXIF aux ///////////////////////////////////////////////////

        final String lens = getOwningMetadata().getLens();
        if ( lens != null ) {
            final Element auxRDFDescElement = XMPUtil.createRDFDescription(
                xmpDoc, XMP_EXIF_AUX_NS, XMP_EXIF_AUX_PREFIX
            );
            final Element lensElement = xmpDoc.createElementNS(
                XMP_EXIF_AUX_NS, XMP_EXIF_AUX_PREFIX + ":Lens"
            );
            XMLUtil.setTextContentOf( lensElement, lens );
            auxRDFDescElement.appendChild( lensElement );
            elements.add( auxRDFDescElement );
        }

        ////////// XAP ////////////////////////////////////////////////////////

        final Element xapRDFDescElement =
            XMPUtil.createRDFDescription( xmpDoc, XMP_XAP_NS, XMP_XAP_PREFIX );

        ////////// CreateDate

        final DateMetaValue captureDateValue =
            (DateMetaValue)getValue( CORE_CAPTURE_DATE_TIME );
        if ( captureDateValue != null ) {
            final Element createDateElement = xmpDoc.createElementNS(
                XMP_XAP_NS, XMP_XAP_PREFIX + ":CreateDate"
            );
            XMLUtil.setTextContentOf(
                createDateElement,
                TextUtil.dateFormat(
                    ISO_8601_DATE_FORMAT, captureDateValue.getDateValue()
                )
            );
            xapRDFDescElement.appendChild( createDateElement );
        }

        ////////// CreatorTool

        final Element creatorToolElement = xmpDoc.createElementNS(
            XMP_XAP_NS, XMP_XAP_PREFIX + ":CreatorTool"
        );
        XMLUtil.setTextContentOf(
            creatorToolElement,
            Version.getApplicationName() + ' ' + Version.getVersionName()
        );
        xapRDFDescElement.appendChild( creatorToolElement );

        ////////// MetadataDate & ModifyDate

        final Date now = new Date();
        final Element metadataDateElement = xmpDoc.createElementNS(
            XMP_XAP_NS, XMP_XAP_PREFIX + ":MetadataDate"
        );
        XMLUtil.setTextContentOf(
            metadataDateElement,
            TextUtil.dateFormat( ISO_8601_DATE_FORMAT, now )
        );
        xapRDFDescElement.appendChild( metadataDateElement );
        final Element modifyDateElement = xmpDoc.createElementNS(
            XMP_XAP_NS, XMP_XAP_PREFIX + ":ModifyDate"
        );
        XMLUtil.setTextContentOf(
            modifyDateElement,
            TextUtil.dateFormat( ISO_8601_DATE_FORMAT, now )
        );
        xapRDFDescElement.appendChild( modifyDateElement );

        ////////// Rating

        final ImageMetaValue rating = getValue( CORE_RATING );
        if ( rating != null ) {
            final Element ratingElement = xmpDoc.createElementNS(
                XMP_XAP_NS, XMP_XAP_PREFIX + ":Rating"
            );
            XMLUtil.setTextContentOf( ratingElement, rating.getStringValue() );
            xapRDFDescElement.appendChild( ratingElement );
        }

        ////////// Color Label (Urgency)

        final ImageMetaValue urgency = getValue( CORE_URGENCY );
        if (urgency != null) {
            final String urgencyString = urgency.getStringValue();

            final Element photoshopRDFDescElement = XMPUtil.createRDFDescription(
                    xmpDoc, XMP_PHOTOSHOP_NS, XMP_PHOTOSHOP_PREFIX
            );
            final Element photoshopUrgencyElement = xmpDoc.createElementNS(
                    XMP_PHOTOSHOP_NS, XMP_PHOTOSHOP_PREFIX + ":Urgency"
            );
            XMLUtil.setTextContentOf(photoshopUrgencyElement, urgencyString);
            photoshopRDFDescElement.appendChild(photoshopUrgencyElement);
            elements.add(photoshopRDFDescElement);

            final Element labelElement = xmpDoc.createElementNS(
                    XMP_XAP_NS, XMP_XAP_PREFIX + ":Label"
            );
            XMLUtil.setTextContentOf( labelElement, convertToColorName(urgencyString) );
            xapRDFDescElement.appendChild( labelElement );
        }

        elements.add( xapRDFDescElement );
        return elements;
    }

    ////////// private ////////////////////////////////////////////////////////

    @NotNull
    private String convertToColorName(@NotNull String c) {
        switch (c) {
            case "1":
                return "Red";
            case "3":
                return "Yellow";
            case "4":
                return "Green";
            case "5":
                return "Blue";
            case "6":
                return "Purple";
            default:
                return "";
        }
    }

    /**
     * Adds the tag mappings.
     *
     * @param id The tag's ID.
     * @param name The tag's name.
     * @param type The tag's {@link ImageMetaType}.
     * @param isChangeable Whether the tag is user-changeable.
     */
    private static void add( int id, String name, ImageMetaType type,
                             boolean isChangeable ) {
        final ImageMetaTagInfo tagInfo =
            new ImageMetaTagInfo( id, name, type, isChangeable );
        m_tagsByID.put( id, tagInfo );
        m_tagsByName.put( name, tagInfo );
    }

    /**
     * Adds aperture information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addAperture( ImageMetadata metadata ) {
        removeValue( CORE_APERTURE );
        final float aperture = metadata.getAperture();
        if ( aperture > 0 )
            putValue( CORE_APERTURE, new FloatMetaValue( aperture ) );
    }

    /**
     * Adds artist information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addArtist( ImageMetadata metadata ) {
        removeValue( CORE_ARTIST );
        final String artist = metadata.getArtist();
        if ( artist != null )
            putValue( CORE_ARTIST, new StringMetaValue( artist ) );
    }

    /**
     * Adds the camera make/model to the <code>CoreDirectory</code>'s metadata.
     *
     * @param imageInfo The {@link ImageMetadata} to add into.
     */
    private void addCamera( ImageInfo imageInfo ) {
        removeValue( CORE_CAMERA );
        final ImageMetadata metadata = imageInfo.getCurrentMetadata();
        String camera = metadata.getCameraMake( true );
        if ( camera == null ) {
            //
            // We didn't get the camera from metadata, so see if the image is a
            // raw image by virtue of having a RawImageInfo associated with it:
            // if so, get the camera from dcraw.
            //
            final AuxiliaryImageInfo auxInfo;
            try {
                auxInfo = imageInfo.getAuxiliaryInfo();
            }
            catch ( Exception e ) {
                return;
            }
            if ( auxInfo instanceof RawImageInfo ) {
                final DCRaw dcRaw = ((RawImageInfo)auxInfo).getDCRaw();
                camera = dcRaw.getCameraMake( true );
            }
        }
        if ( camera != null )
            putValue( CORE_CAMERA, new StringMetaValue( camera ) );
    }

    /**
     * Adds caption information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addCaption( ImageMetadata metadata ) {
        removeValue( CORE_CAPTION );
        final String caption = metadata.getCaption();
        if ( caption != null )
            putValue( CORE_CAPTION, new StringMetaValue( caption ) );
    }

    /**
     * Adds the capture date/time to the <code>CoreDirectory</code>'s metadata.
     *
     * @param imageInfo The {@link ImageMetadata} to add into.
     */
    private void addCaptureDateTime( ImageInfo imageInfo ) {
        removeValue( CORE_CAPTURE_DATE_TIME );
        final Date date = imageInfo.getCurrentMetadata().getCaptureDateTime();
        if ( date != null )
            putValue( CORE_CAPTURE_DATE_TIME, new DateMetaValue( date ) );
    }

    /**
     * Adds the name of the color profile used in the image.
     *
     * @param imageInfo The {@link ImageMetadata} to add into.
     */
    private void addColorProfile( ImageInfo imageInfo ) {
        removeValue( CORE_COLOR_PROFILE );
        try {
            final ICC_Profile profile =
                imageInfo.getImageType().getICCProfile( imageInfo );
            if ( profile == null )
                return;
            final String name = ColorProfileInfo.getNameOf( profile );
            if ( name != null )
                putValue( CORE_COLOR_PROFILE, new StringMetaValue( name ) );
        }
        catch ( Exception e ) {
            // ignore
        }
    }

    /**
     * Adds color temperature information to the <code>CoreDirectory</code>'s
     * metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addColorTemperature( ImageMetadata metadata ) {
        removeValue( CORE_COLOR_TEMPERATURE );
        final int temp = metadata.getColorTemperature();
        if ( temp > 0 )
            putValue(
                CORE_COLOR_TEMPERATURE, new UnsignedShortMetaValue( temp )
            );
    }

    /**
     * Adds copyright information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addCopyright( ImageMetadata metadata ) {
        removeValue( CORE_COPYRIGHT );
        final String copyright = metadata.getCopyright();
        if ( copyright != null )
            putValue( CORE_COPYRIGHT, new StringMetaValue( copyright ) );
    }

    /**
     * Adds file information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param imageInfo The image to obtain file information from.
     */
    private void addFileInfo( ImageInfo imageInfo ) {
        final File file = imageInfo.getFile().getAbsoluteFile();
        String parentDir = file.getParent();
        if ( parentDir == null )
            parentDir = System.getProperty( "user.dir" );
        putValue( CORE_DIR_NAME , new StringMetaValue( parentDir ) );
        putValue( CORE_FILE_NAME, new StringMetaValue( file.getName() ) );
        putValue( CORE_FILE_SIZE, new UnsignedLongMetaValue( file.length() ) );
        final Date date = new Date( file.lastModified() );
        putValue( CORE_FILE_DATE_TIME, new DateMetaValue( date ) );

        removeValue( CORE_ORIGINAL_IMAGE_HEIGHT );
        removeValue( CORE_ORIGINAL_IMAGE_WIDTH );
        try {
            final File origImageFile = imageInfo.getOriginalFile();
            if ( origImageFile != null ) {
                final ImageInfo origInfo =
                    ImageInfo.getInstanceFor( origImageFile );
                final ImageMetadata origMetadata = origInfo.getMetadata();
                putValue(
                    CORE_ORIGINAL_IMAGE_HEIGHT,
                    new UnsignedShortMetaValue( origMetadata.getImageHeight() )
                );
                putValue(
                    CORE_ORIGINAL_IMAGE_WIDTH,
                    new UnsignedShortMetaValue( origMetadata.getImageWidth() )
                );
            }
        }
        catch ( Exception e ) {
            // ignore
        }
    }

    /**
     * Adds flash information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addFlash( ImageMetadata metadata ) {
        removeValue( CORE_FLASH );
        final int flash = metadata.getFlash();
        if ( flash != -1 )
            putValue( CORE_FLASH, new UnsignedShortMetaValue( flash ) );
    }

    /**
     * Adds focal length information to the <code>CoreDirectory</code>'s
     * metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addFocalLength( ImageMetadata metadata ) {
        removeValue( CORE_FOCAL_LENGTH );
        final float focalLength = metadata.getFocalLength();
        if ( focalLength != 0 )
            putValue( CORE_FOCAL_LENGTH, new FloatMetaValue( focalLength ) );
    }

    /**
     * Adds image dimension information to the <code>CoreDirectory</code>'s
     * metadata.
     *
     * @param imageInfo The image to obtain file information from.
     */
    private void addImageDimensions( ImageInfo imageInfo ) {
        removeValue( CORE_IMAGE_WIDTH );
        removeValue( CORE_IMAGE_HEIGHT );
        final ImageMetadata metadata = imageInfo.getCurrentMetadata();
        int width = metadata.getImageWidth();
        int height = metadata.getImageHeight();

        if ( width == 0 || height == 0 ) {
            //
            // We didn't get the dimensions from metadata so try to get them
            // from the image directly.
            //
            try {
                final ImageType t = imageInfo.getImageType();
                final Dimension d = t.getDimension( imageInfo );
                if ( d != null ) {
                    width = d.width;
                    height = d.height;
                }
            }
            catch ( Exception e ) {
                // ignore
            }
        }

        if ( width > 0 && height > 0 ) {
            putValue(
                CORE_IMAGE_WIDTH, new UnsignedShortMetaValue( width )
            );
            putValue(
                CORE_IMAGE_HEIGHT, new UnsignedShortMetaValue( height )
            );
        }
    }

    /**
     * Adds ISO information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addISO( ImageMetadata metadata ) {
        removeValue( CORE_ISO );
        final int iso = metadata.getISO();
        if ( iso > 0 )
            putValue( CORE_ISO, new UnsignedShortMetaValue( iso ) );
    }

    /**
     * Adds lens information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addLens( ImageMetadata metadata ) {
        //removeValue( CORE_LENS );
        final String lens = metadata.getLens();
        if ( lens != null )
            putValue( CORE_LENS, new StringMetaValue( lens ) );
    }

    /**
     * Adds orientation information to the <code>CoreDirectory</code>'s
     * metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addOrientation( ImageMetadata metadata ) {
        removeValue( CORE_IMAGE_ORIENTATION );
        final ImageOrientation orientation = getOrientationFrom( metadata );
        putValue(
            CORE_IMAGE_ORIENTATION,
            new UnsignedShortMetaValue( orientation.getTIFFConstant() )
        );
    }

    /**
     * Adds rating information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addRating( ImageMetadata metadata ) {
        final int rating = metadata.getRating();
        if ( rating > 0 )
            putValue( CORE_RATING, new UnsignedShortMetaValue( rating ) );
    }

    /**
     * Adds urgency information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addUrgency( ImageMetadata metadata ) {
        final int urgency = metadata.getUrgency();
        if ( urgency > 0 )
            putValue( CORE_URGENCY, new UnsignedShortMetaValue( urgency ) );
    }

    /**
     * Adds resolution information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addResolution( ImageMetadata metadata ) {
        removeValue( CORE_IMAGE_RESOLUTION );
        final double res = metadata.getResolution();
        if ( res == 0 )
            return;
        final String unitString;
        switch ( metadata.getResolutionUnit() ) {
            case ResolutionProvider.RESOLUTION_UNIT_CM:
                unitString = "cm";      // TODO: localize
                break;
            case ResolutionProvider.RESOLUTION_UNIT_INCH:
                unitString = "inch";    // TODO: localize
                break;
            default:
                return;
        }
        final ImageMetaValue value = new StringMetaValue(
            TextUtil.tenths( res ) + " pixels/" + unitString
        );
        putValue( CORE_IMAGE_RESOLUTION, value );
    }

    /**
     * Adds shutter-speed information to the <code>CoreDirectory</code>'s
     * metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addShutterSpeed( ImageMetadata metadata ) {
        removeValue( CORE_SHUTTER_SPEED );
        final float value = metadata.getShutterSpeed();
        if ( value > 0 )
            putValue( CORE_SHUTTER_SPEED, new FloatMetaValue( value ) );
    }

    /**
     * Adds title information to the <code>CoreDirectory</code>'s metadata.
     *
     * @param metadata The {@link ImageMetadata} to add into.
     */
    private void addTitle( ImageMetadata metadata ) {
        removeValue( CORE_TITLE );
        final String value = metadata.getTitle();
        if ( value != null )
            putValue( CORE_TITLE, new StringMetaValue( value ) );
    }

    /**
     * Gets the image orientation from the given {@link ImageMetadata}.
     *
     * @param metadata The {@link ImageMetadata} to get the orientation from.
     * @return Returns said orientation or
     * {@link ImageOrientation#ORIENTATION_LANDSCAPE} if none.
     */
    private static ImageOrientation getOrientationFrom( ImageMetadata metadata ) {
        final ImageOrientation orientation = metadata.getOrientation();
        if ( orientation == ORIENTATION_UNKNOWN ) {
            //
            // We decree that there shall always be an orientation value.  If
            // there isn't one, just assume it's landscape.
            //
            return ORIENTATION_LANDSCAPE;
        }
        return orientation;
    }

    /** Used for the string value of the rating. */
    private static final String FIVE_STARS = "\u2605\u2605\u2605\u2605\u2605";

    /**
     * This is where the actual labels for the tags are.
     */
    private static final ResourceBundle m_tagBundle = ResourceBundle.getBundle(
        "com.lightcrafts.image.metadata.CoreTags"
    );

    /**
     * A mapping of tags by ID.
     */
    private static final Map<Integer,ImageMetaTagInfo> m_tagsByID =
        new HashMap<Integer,ImageMetaTagInfo>();

    /**
     * A mapping of tags by name.
     */
    private static final Map<String,ImageMetaTagInfo> m_tagsByName =
        new HashMap<String,ImageMetaTagInfo>();

    static {
        add( CORE_APERTURE, "Aperture", META_FLOAT, false );
        add( CORE_ARTIST, "Artist", META_STRING, true );
        add( CORE_CAMERA, "Camera", META_STRING, false );
        add( CORE_CAPTION, "Caption", META_STRING, true );
        add( CORE_CAPTURE_DATE_TIME, "CaptureDateTime", META_DATE, false );
        add( CORE_COLOR_PROFILE, "ColorProfile", META_STRING, false );
        add( CORE_COLOR_TEMPERATURE, "ColorTemperature", META_USHORT, false );
        add( CORE_COPYRIGHT, "Copyright", META_STRING, true );
        add( CORE_DIR_NAME, "DirName", META_STRING, false );
        add( CORE_FILE_DATE_TIME, "FileDateTime", META_DATE, false );
        add( CORE_FILE_NAME, "FileName", META_STRING, false );
        add( CORE_FILE_SIZE, "FileSize", META_ULONG, false );
        add( CORE_FLASH, "Flash", META_USHORT, false );
        add( CORE_FOCAL_LENGTH, "FocalLength", META_FLOAT, false );
        add( CORE_IMAGE_HEIGHT, "ImageHeight", META_USHORT, false );
        add( CORE_IMAGE_ORIENTATION, "ImageOrientation", META_USHORT, true );
        add( CORE_IMAGE_RESOLUTION, "ImageResolution", META_STRING, false );
        add( CORE_IMAGE_WIDTH, "ImageWidth", META_USHORT, false );
        add( CORE_ISO, "ISO", META_USHORT, false );
        add( CORE_LENS, "Lens", META_STRING, false );
        add( CORE_ORIGINAL_ORIENTATION, "OriginalOrientation", META_USHORT, false );
        add( CORE_RATING, "Rating", META_USHORT, true );
        add( CORE_SHUTTER_SPEED, "ShutterSpeed", META_FLOAT, false );
        add( CORE_URGENCY, "Urgency", META_USHORT, true );
    }
}
/* vim:set et sw=4 ts=4: */
