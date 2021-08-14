/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.makernotes.MakerNotesDirectory;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.image.metadata.values.*;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.utils.LightCraftsException;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.*;
import java.util.*;

import static com.lightcrafts.image.metadata.CoreTags.*;
import static com.lightcrafts.image.metadata.EXIFConstants.EXIF_SUBEXIF_TAG_ID_START;
import static com.lightcrafts.image.metadata.EXIFTags.*;
import static com.lightcrafts.image.metadata.IPTCTags.*;
import static com.lightcrafts.image.metadata.ImageOrientation.ORIENTATION_LANDSCAPE;
import static com.lightcrafts.image.metadata.ImageOrientation.ORIENTATION_UNKNOWN;
import static com.lightcrafts.image.metadata.TIFFTags.*;
import static com.lightcrafts.image.metadata.XMPConstants.XMP_DC_NS;
import static com.lightcrafts.image.metadata.XMPConstants.XMP_DC_PREFIX;

/**
 * <code>ImageMetadata</code> contains one zero or more &quot;directories&quot;
 * (EXIF, IPTC, etc.) of metadata for an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class ImageMetadata implements
    ApertureProvider, BitsPerChannelProvider, CaptionProvider,
    CaptureDateTimeProvider, Cloneable, ColorTemperatureProvider,
    CopyrightProvider, Externalizable, FileDateTimeProvider, FlashProvider,
    FocalLengthProvider, GPSProvider, ISOProvider, LensProvider, MakeModelProvider,
    OrientationProvider, OriginalWidthHeightProvider, RatingProvider,
    ResolutionProvider, ShutterSpeedProvider, TitleProvider,
    WidthHeightProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a new <code>ImageMetadata</code>.
     */
    public ImageMetadata() {
        // do nothing
    }

    /**
     * Construct a new <code>ImageMetadata</code>.
     *
     * @param imageType The {@link ImageType} of the image this metadata is
     * for.
     */
    public ImageMetadata( ImageType imageType ) {
        m_imageType = imageType;
    }

    /**
     * Clear all metadata.
     */
    public void clear() {
        m_classToDirMap.clear();
    }

    /**
     * Clears the edited flag for all {@link ImageMetaValue}s.
     */
    public void clearEdited() {
        for ( ImageMetadataDirectory dir : m_classToDirMap.values() )
            dir.clearEdited();
    }

    /**
     * Clears the rating of the image.
     *
     * @see #getRating()
     * @see #setRating(int)
     */
    public void clearRating() {
        setRating( 0 );
    }

    /**
     * Perform a deep clone of this object.
     *
     * @return Returns said clone.
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    public Object clone() {
        final ImageMetadata copy = new ImageMetadata( getImageType() );
        for ( Map.Entry<Class,ImageMetadataDirectory> me
              : m_classToDirMap.entrySet() ) {
            final Class dirClass = me.getKey();
            final ImageMetadataDirectory dir = me.getValue();
            copy.m_classToDirMap.put( dirClass, dir.clone() );
        }
        return copy;
    }

    /**
     * Compares this <code>ImageMetadata</code> to another for equality.  Two
     * <code>ImageMetadata</code> objects are considered equal if their paths
     * are equal.
     * <p>
     * Note: I don't consider this the correct test for equality, but this is
     * what Tom did and now other code relies upon it.
     *
     * @param object The {@link Object} to compare to.
     * @return Returns <code>true</code> only if the other object is also an
     * <code>ImageMetadata</code> and the two objects are equal.
     * @see #hashCode()
     */
    public boolean equals( Object object ) {
        if ( object == this )
            return true;
        if ( object instanceof ImageMetadata ) {
            final ImageMetadata thatMD = (ImageMetadata)object;
            final String thisPath = getPath();
            final String thatPath = thatMD.getPath();
            return  thisPath == null ? thatPath == null :
                    thisPath.equals( thatPath );
        }
        return false;
    }

    /**
     * Find an {@link ImageMetadataDirectory} that implements the given
     * provider interface.
     *
     * @param provider The provider interface to find.
     * @return Returns an {@link ImageMetadataDirectory} that implements the
     * given provider interface or <code>null</code> if none is found.
     * @see #findProvidersOf(Class)
     */
    public ImageMetadataDirectory findProviderOf( Class provider ) {
        for ( ImageMetadataDirectory dir : getDirectories() )
            if ( provider.isInstance( dir ) )
                return dir;
        return null;
    }

    /**
     * Find all instances of {@link ImageMetadataDirectory} that implement the
     * given provider interface.
     *
     * @param provider The provider interface to find.
     * @return Returns an ordered {@link Collection} of all instances of
     * {@link ImageMetadataDirectory} that implement the given provider
     * interface.
     * @see #findProviderOf(Class)
     */
    public Collection<ImageMetadataDirectory>
    findProvidersOf( Class<? extends ImageMetadataProvider> provider ) {
        final ArrayList<ImageMetadataDirectory> providers =
            new ArrayList<ImageMetadataDirectory>();
        for ( ImageMetadataDirectory dir : getDirectories() )
            if ( provider.isInstance( dir ) )
                providers.add( dir );
        Collections.sort( providers, new ProviderComparator( provider ) );
        return providers;
    }

    /**
     * {@inheritDoc}
     */
    public float getAperture() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( ApertureProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final float value = ((ApertureProvider)dir).getAperture();
            if ( value > 0 )
                return value;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public String getArtist() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( ArtistProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final String value = ((ArtistProvider)dir).getArtist();
            if ( value != null )
                return value;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getBitsPerChannel() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( BitsPerChannelProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final int value = ((BitsPerChannelProvider)dir).getBitsPerChannel();
            if ( value > 0 )
                return value;
        }
        return 0;
    }

    /**
     * Gets the make and model of the camera used.
     *
     * @param includeModel If <code>true</code>, the model is included.
     * @return Returns the make (and possibly model) converted to uppercase and
     * seperated by a space or <code>null</code> if not available.
     */
    public final String getCameraMake( boolean includeModel ) {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( MakeModelProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final String make =
                ((MakeModelProvider)dir).getCameraMake( includeModel );
            if ( make != null )
                return make;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getCaption() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( CaptionProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final String value = ((CaptionProvider)dir).getCaption();
            if ( value != null )
                return value;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Date getCaptureDateTime() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( CaptureDateTimeProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final Date date =
                ((CaptureDateTimeProvider)dir).getCaptureDateTime();
            if ( date != null )
                return date;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getColorTemperature() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( ColorTemperatureProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final int temp =
                ((ColorTemperatureProvider)dir).getColorTemperature();
            if ( temp > 0 )
                return temp;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public String getCopyright() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( CopyrightProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final String value = ((CopyrightProvider)dir).getCopyright();
            if ( value != null )
                return value;
        }
        return null;
    }

    /**
     * Gets all the directories (if any) of metadata.
     *
     * @return Returns a {@link Collection} of said directories.
     */
    public Collection<ImageMetadataDirectory> getDirectories() {
        return m_classToDirMap.values();
    }

    /**
     * Gets the {@link ImageMetadataDirectory} instance for a given
     * {@link ImageMetadataDirectory} {@link Class}.
     *
     * @param dirClass The {@link Class} of a class derived from
     * {@link ImageMetadataDirectory}.
     * @return Returns the {@link ImageMetadataDirectory} for the given
     * {@link Class} if found; <code>null</code> otherwise.
     */
    public ImageMetadataDirectory getDirectoryFor(
        Class<? extends ImageMetadataDirectory> dirClass )
    {
        return getDirectoryFor( dirClass, false );
    }

    /**
     * Gets the {@link ImageMetadataDirectory} instance for a given
     * {@link ImageMetadataDirectory} {@link Class}.
     *
     * @param dirClass The sought {@link Class} of a class derived from
     * {@link ImageMetadataDirectory}.
     * @param create If <code>true</code>, creates the
     * {@link ImageMetadataDirectory} if it doesn't exist.
     * @return Returns the {@link ImageMetadataDirectory} for the given
     * {@link Class} if found or created; <code>null</code> otherwise.
     */
    public ImageMetadataDirectory getDirectoryFor(
        Class<? extends ImageMetadataDirectory> dirClass, boolean create )
    {
        synchronized ( m_classToDirMap ) {
            ImageMetadataDirectory dir = m_classToDirMap.get( dirClass );
            if ( dir == null && create ) {
                try {
                    dir = dirClass.newInstance();
                    dir.setOwningMetadata( this );
                }
                catch ( Exception e ) {
                    throw new IllegalStateException( e );
                }
                m_classToDirMap.put( dirClass, dir );
            }
            return dir;
        }
    }

    /**
     * Gets the absolute {@link File} of the image.
     *
     * @return Returns said {@link File} or <code>null</code> if unavailable.
     * @see #getPath()
     */
    public File getFile() {
        final String path = getPath();
        return path != null ? new File( path ) : null;
    }

    /**
     * {@inheritDoc}
     */
    public Date getFileDateTime() {
        final CoreDirectory dir =
            (CoreDirectory)getDirectoryFor( CoreDirectory.class );
        return dir != null ? dir.getFileDateTime() : null;
    }

    /**
     * {@inheritDoc}
     */
    public int getFlash() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( FlashProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final int flash = ((FlashProvider)dir).getFlash();
            if ( flash != -1 )
                return flash;
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public float getFocalLength() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( FocalLengthProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final float value = ((FocalLengthProvider)dir).getFocalLength();
            if ( value > 0 )
                return value;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double getGPSLatitude() {
        final Collection<ImageMetadataDirectory> dirs =
                findProvidersOf( GPSProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final Double latitude = ((GPSProvider)dir).getGPSLatitude();
            if ( latitude != null )
                return latitude;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double getGPSLongitude() {
        final Collection<ImageMetadataDirectory> dirs =
                findProvidersOf( GPSProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final Double longitude = ((GPSProvider)dir).getGPSLongitude();
            if ( longitude != null )
                return longitude;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGPSLatitudeDMS() {
        final Collection<ImageMetadataDirectory> dirs =
                findProvidersOf( GPSProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final String value = ((GPSProvider)dir).getGPSLatitudeDMS();
            if (! value.isEmpty())
                return value;
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGPSLongitudeDMS() {
        final Collection<ImageMetadataDirectory> dirs =
                findProvidersOf( GPSProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final String value = ((GPSProvider)dir).getGPSLongitudeDMS();
            if (! value.isEmpty())
                return value;
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public int getImageHeight() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( WidthHeightProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final int height = ((WidthHeightProvider)dir).getImageHeight();
            if ( height > 0 )
                return height;
        }
        return 0;
    }

    /**
     * Gets the {@link ImageType} of the image this metadata is for.
     *
     * @return Returns said type or <code>null</code> if it could not be
     * determined.
     */
    public synchronized ImageType getImageType() {
        if ( m_imageType == null ) {
            final File file = getFile();
            if ( file != null ) {
                final ImageInfo info = ImageInfo.getInstanceFor( file );
                try {
                    m_imageType = info.getImageType();
                }
                catch ( IOException e ) {
                    // ignore
                }
                catch ( LightCraftsException e ) {
                    // ignore
                }
            }
        }
        return m_imageType;
    }

    /**
     * {@inheritDoc}
     */
    public int getImageWidth() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( WidthHeightProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final int width = ((WidthHeightProvider)dir).getImageWidth();
            if ( width > 0 )
                return width;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getOriginalImageHeight() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( OriginalWidthHeightProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final int height =
                ((OriginalWidthHeightProvider)dir).getOriginalImageHeight();
            if ( height > 0 )
                return height;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getOriginalImageWidth() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( OriginalWidthHeightProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final int width =
                ((OriginalWidthHeightProvider)dir).getOriginalImageWidth();
            if ( width > 0 )
                return width;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getISO() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( ISOProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final int iso = ((ISOProvider)dir).getISO();
            if ( iso > 0 )
                return iso;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public String getLens() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( LensProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final String lens = ((LensProvider)dir).getLens();
            if ( lens != null )
                return lens;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ImageOrientation getOrientation() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( OrientationProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final ImageOrientation orientation =
                ((OrientationProvider)dir).getOrientation();
            if ( orientation != ORIENTATION_UNKNOWN )
                return orientation;
        }
        return ORIENTATION_UNKNOWN;
    }

    /**
     * Gets the original orientation of an image, that is the orientation
     * embedded in its own metadata and not merged from any XMP file.
     *
     * @return Returns said orientation.
     */
    public ImageOrientation getOriginalOrientation() {
        final ImageMetaValue value =
            getValue( CoreDirectory.class, CORE_ORIGINAL_ORIENTATION );
        return  value != null ?
                    ImageOrientation.getOrientationFor( value.getIntValue() ) :
                    ORIENTATION_LANDSCAPE;
    }

    /**
     * Gets the absolute path to the original image file.
     *
     * @return Returns said path or <code>null</code> if unavailable.
     * @see #getFile()
     */
    public synchronized String getPath() {
        final CoreDirectory coreDir =
            (CoreDirectory)getDirectoryFor( CoreDirectory.class );
        return coreDir != null ? coreDir.getPath() : null;
    }

    /**
     * Gets the rating of this image.
     *
     * @return Returns said rating in the range 1-5 if it's rated, otherwise
     * returns 0.
     * @see #clearRating()
     * @see #setRating(int)
     */
    public int getRating() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( RatingProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final int rating = ((RatingProvider)dir).getRating();
            if ( rating != 0 )
                return rating;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public double getResolution() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( ResolutionProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final double value = ((ResolutionProvider)dir).getResolution();
            if ( value > 0 )
                return value;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getResolutionUnit() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( ResolutionProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final int value = ((ResolutionProvider)dir).getResolutionUnit();
            if ( value != RESOLUTION_UNIT_NONE )
                return value;
        }
        return RESOLUTION_UNIT_NONE;
    }

    /**
     * {@inheritDoc}
     */
    public float getShutterSpeed() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( ShutterSpeedProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final float speed = ((ShutterSpeedProvider)dir).getShutterSpeed();
            if ( speed > 0 )
                return speed;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public String getTitle() {
        final Collection<ImageMetadataDirectory> dirs =
            findProvidersOf( TitleProvider.class );
        for ( ImageMetadataDirectory dir : dirs ) {
            final String value = ((TitleProvider)dir).getTitle();
            if ( value != null )
                return value;
        }
        return null;
    }

    /**
     * Gets the value for a particular tag ID in a particular
     * {@link ImageMetadataDirectory}.  If the tag ID isn't found in the given
     * directory, each directory along the static parent chain is checked
     * also.
     *
     * @param dirClass The {@link Class} of a class derived from
     * {@link ImageMetadataDirectory}.
     * @param tagID The ID of the tag to get the value for.
     * @return Returns the relevant {@link ImageMetaValue} or <code>null</code>
     * if there is no value for the given tag ID in the given
     * {@link ImageMetadataDirectory} or any static parent thereof.
     * @see ImageMetadataDirectory#getStaticParent()
     */
    public ImageMetaValue getValue(
        Class<? extends ImageMetadataDirectory> dirClass, int tagID )
    {
        final ImageMetadataDirectory dir = getDirectoryFor( dirClass );
        return dir != null ? dir.getValue( tagID ) : null;
    }

    /**
     * Returns the hash code for this <code>ImageMetadata</code>.
     * <p>
     * Note: I don't consider using the path to be the correct way to compute
     * the hash code, but this is what Tom did and now hother code relies upon
     * it.
     *
     * @return Returns said hash code.
     * @see #equals(Object)
     */
    public int hashCode() {
        final String path = getPath();
        return path != null ? path.hashCode() : super.hashCode();
    }

    /**
     * Returns whether there is some metadata.
     *
     * @return Returns <code>true</code> only if there is no metadata.
     */
    public boolean isEmpty() {
        return m_classToDirMap.isEmpty();
    }

    /**
     * Merge the metadata from another {@link ImageMetadata} object into this
     * one.
     *
     * @param fromMetadata The other {@link ImageMetadata} to merge from.
     */
    public void mergeFrom( ImageMetadata fromMetadata ) {
        for ( ImageMetadataDirectory fromDir : fromMetadata.getDirectories() ) {
            final ImageMetadataDirectory toDir =
                getDirectoryFor( fromDir.getClass(), true );
            toDir.mergeFrom( fromDir );
        }

        //
        // As a special case, if the "from" metadata has an orientation,
        // replace the orientation in EXIF and TIFF directories, otherwise
        // there will be inconsistent orientations.
        //
        final ImageOrientation orientation = fromMetadata.getOrientation();
        if ( orientation != ORIENTATION_UNKNOWN ) {
            final ImageMetaValue value =
                new UnsignedShortMetaValue( orientation.getTIFFConstant() );
            putValue( EXIFDirectory.class, EXIF_ORIENTATION, value, false );
            putValue( TIFFDirectory.class, TIFF_ORIENTATION, value, false );
        }
    }

    /**
     * Prepare the EXIF, subEXIF, and TIFF directories for export.  This means
     * adding some fields, setting the relevant fields for the correct image
     * width/height, and removing others that are no longer correct given the
     * changes.
     * <p>
     * The original metadata is unchanged; instead, a modified clone is
     * returned.
     *
     * @param exportImageType The type of image that will be exported to.  It
     * must be either {@link JPEGImageType#INSTANCE} or
     * {@link TIFFImageType#INSTANCE}.
     * @param includeOrientation If <code>true</code>, include orientation
     * metadata.
     * @return Returns the prepared <code>ImageMetadata</code>.
     * @see #prepForExport(ImageType,int,int,int,int,boolean)
     * @see #prepForXMP(boolean)
     */
    public ImageMetadata prepForExport( ImageType exportImageType,
                                        boolean includeOrientation ) {
        final ImageMetaValue widthValue =
            getValue( CoreDirectory.class, CORE_IMAGE_WIDTH );
        final ImageMetaValue heightValue =
            getValue( CoreDirectory.class, CORE_IMAGE_HEIGHT );
        final int width  = widthValue  != null ? widthValue.getIntValue()  : 0;
        final int height = heightValue != null ? heightValue.getIntValue() : 0;
        return prepForExport(
            exportImageType, width, height,
            (int)getResolution(), getResolutionUnit(), includeOrientation
        );
    }

    /**
     * Prepare the EXIF, subEXIF, and TIFF directories for export.  This means
     * adding some fields, setting the relevant fields for the correct image
     * width/height, and removing others that are no longer correct given the
     * changes.
     * <p>
     * The original metadata is unchanged; instead, a modified clone is
     * returned.
     *
     * @param exportImageType The type of image that will be exported to.  It
     * must be either {@link JPEGImageType#INSTANCE} or
     * {@link TIFFImageType#INSTANCE}.
     * @param imageWidth The exported image width.
     * @param imageHeight The exported image height.
     * @param resolution The resolution (in pixels per unit).
     * @param resolutionUnit The resolution unit; must be either
     * {@link #RESOLUTION_UNIT_CM} or {@link #RESOLUTION_UNIT_INCH}.
     * @param includeOrientation If <code>true</code>, include orientation
     * metadata.
     * @return Returns the prepared <code>ImageMetadata</code>.
     * @see #prepForExport(ImageType,boolean)
     * @see #prepForXMP(boolean)
     */
    public ImageMetadata prepForExport( ImageType exportImageType,
                                        int imageWidth, int imageHeight,
                                        int resolution, int resolutionUnit,
                                        boolean includeOrientation ) {
        final boolean toJPEG = exportImageType == JPEGImageType.INSTANCE;
        //
        // We clone the metadata first because we don't want to modify the
        // original.
        //
        final ImageMetadata metadata = (ImageMetadata)clone();

        ImageMetadataDirectory tiffDir =
            metadata.getDirectoryFor( TIFFDirectory.class );

        final ImageMetadataDirectory ciffDir =
            metadata.getDirectoryFor( CIFFDirectory.class );
        if ( ciffDir != null ) {
            //
            // If there's CIFF metadata, convert it to TIFF/EXIF, then merge it
            // into the existing metadata.
            //
            final ImageMetadata ciffMetadata =
                ((CIFFDirectory)ciffDir).convertMetadata( toJPEG );
            metadata.mergeFrom( ciffMetadata );
            metadata.removeDirectory( CIFFDirectory.class );
        } else {
            final ImageMetadataDirectory dngDir =
                    metadata.getDirectoryFor(DNGDirectory.class);
            if (dngDir != null) {
                if (tiffDir == null) {
                    tiffDir = metadata.getDirectoryFor(TIFFDirectory.class, true);
                }
                tiffDir.mergeFrom(dngDir);
                metadata.removeDirectory(DNGDirectory.class);
            }
        }

        final ImageMetadataDirectory exifDir =
            metadata.getDirectoryFor( EXIFDirectory.class, true );

        if ( toJPEG ) {
            //
            // If a TIFF directory is present, move its EXIF-overlapping tag
            // values to the EXIF directory.
            //
            if ( tiffDir != null )
                ImageMetadataDirectory.moveValuesFromTo( tiffDir, exifDir );

            //
            // TIFF metadata inside JPEG files is actually in the EXIF metadata
            // in the EXIF/TIFF-overlapping tags, so just point tiffDir at
            // exifDir.
            //
            tiffDir = exifDir;
        } else {
            if ( tiffDir == null ) {
                //
                // There's still no TIFF metadata: create it.
                //
                tiffDir = metadata.getDirectoryFor( TIFFDirectory.class, true );
            }

            //
            // EXIF metadata inside TIFF files likes to be in a single
            // directory.  If an EXIF subdirectory exists, move all of its
            // values over to the main EXIF directory.
            //
            final ImageMetadataDirectory subEXIFDir =
                metadata.getDirectoryFor( SubEXIFDirectory.class );
            if ( subEXIFDir != null ) {
                for ( Iterator<Map.Entry<Integer,ImageMetaValue>>
                      i = subEXIFDir.iterator(); i.hasNext(); ) {
                    final Map.Entry<Integer,ImageMetaValue> me = i.next();
                    exifDir.putValue( me.getKey(), me.getValue() );
                }
                metadata.removeDirectory( SubEXIFDirectory.class );
            }
        }

        ////////// Values that are always put.

        exifDir.putValue(
            EXIF_EXIF_VERSION,
            new UndefinedMetaValue( EXIFDirectory.EXIF_VERSION )
        );
        tiffDir.putValue(
            TIFF_HOST_COMPUTER,
            new StringMetaValue(
                System.getProperty( "os.name" ) + ' ' +
                System.getProperty( "os.version" )
            )
        );
        tiffDir.putValue(
            TIFF_SOFTWARE, new StringMetaValue( Version.getApplicationName() )
        );

        ////////// Orientation.

        exifDir.removeValue( EXIF_ORIENTATION );
        if ( includeOrientation )
            tiffDir.putValue(
                TIFF_ORIENTATION,
                new UnsignedShortMetaValue(
                    metadata.getOrientation().getTIFFConstant()
                )
            );

        ////////// Rating.

        exifDir.removeValue( EXIF_MS_RATING );
        final int rating = getRating();
        if ( rating > 0 )
            tiffDir.putValue(
                TIFF_MS_RATING, new UnsignedShortMetaValue( rating )
            );
        else
            tiffDir.removeValue( TIFF_MS_RATING );

        ////////// Image width/height.

        exifDir.removeValue( EXIF_PIXEL_X_DIMENSION );
        exifDir.removeValue( EXIF_PIXEL_Y_DIMENSION );
        if ( imageWidth > 0 && imageHeight > 0 ) {
            UnsignedShortMetaValue widthValue =
                new UnsignedShortMetaValue( imageWidth );
            UnsignedShortMetaValue heightValue =
                new UnsignedShortMetaValue( imageHeight );
            tiffDir.putValue( TIFF_IMAGE_WIDTH, widthValue );
            tiffDir.putValue( TIFF_IMAGE_LENGTH, heightValue );
            if ( exifDir != tiffDir ) {
                //
                // We must create distinct values since these need to have an
                // EXIFDirectory as their owner while not altering the owner of
                // the values added to the TIFFDirectory.
                //
                widthValue = new UnsignedShortMetaValue( imageWidth );
                heightValue = new UnsignedShortMetaValue( imageHeight );
                exifDir.putValue( EXIF_IMAGE_WIDTH, widthValue );
                exifDir.putValue( EXIF_IMAGE_HEIGHT, heightValue );
            }
        } else {
            tiffDir.removeValue( TIFF_IMAGE_WIDTH );
            tiffDir.removeValue( TIFF_IMAGE_LENGTH );
        }

        ////////// Image resolution.

        if ( resolution > 0 && resolutionUnit != RESOLUTION_UNIT_NONE ) {
            final UnsignedRationalMetaValue xResolutionValue =
                new UnsignedRationalMetaValue( resolution, 1 );
            final UnsignedRationalMetaValue yResolutionValue =
                new UnsignedRationalMetaValue( resolution, 1 );
            tiffDir.putValue( TIFF_X_RESOLUTION, xResolutionValue );
            tiffDir.putValue( TIFF_Y_RESOLUTION, yResolutionValue );
            tiffDir.putValue(
                TIFF_RESOLUTION_UNIT,
                new UnsignedShortMetaValue( resolutionUnit )
            );
        }

        ////////// Copy overlapping IPTC metadata.

        final ImageMetadataDirectory iptcDir =
            metadata.getDirectoryFor( IPTCDirectory.class );
        if ( iptcDir != null ) {
            final int[][] iptcMap = {
                { IPTC_BY_LINE         , TIFF_ARTIST            },
                { IPTC_COPYRIGHT_NOTICE, TIFF_COPYRIGHT         },
                { IPTC_OBJECT_NAME     , TIFF_DOCUMENT_NAME     },
                { IPTC_CAPTION_ABSTRACT, TIFF_IMAGE_DESCRIPTION }
            };
            for ( int[] tagIDs : iptcMap ) {
                final ImageMetaValue value = iptcDir.getValue( tagIDs[0] );
                if ( value != null ) {
                    //
                    // It's necessary to clone the value so each has the right
                    // owning directory and therefore will get the right tag
                    // name for XMP export.
                    //
                    tiffDir.putValue( tagIDs[1], value.clone() );
                }
            }

            ////////// Always use UTF-8 to write IPTC metadata.

            String value = "";
            try {
                byte[] utf8Marker = {0x1B, 0x25, 0x47}; // ESC, "%", "G"
                value = new String( utf8Marker, "ASCII" );
            } catch (UnsupportedEncodingException e) {
                // This should never occur
            }
            iptcDir.putValue(
                IPTC_CODED_CHARACTER_SET, new StringMetaValue( value )
            );
        }

        ////////// Remove other metadata because it makes no sense to export.

        for (int tagID : unexportedTags) {
            exifDir.removeValue(tagID);
        }

        if ( toJPEG ) {
            //
            // EXIF metadata inside JPEG files likes to be split across a main
            // and a subdirectory.  In particular, the tags having IDs > 0x829A
            // (EXIF_EXPOSURE_TIME), except for 0x8769 (EXIF_IFD_POINTER) and
            // 0x8825 (EXIF_GPS_IFD_POINTER), need to be in the subdirectory.
            //
            ImageMetadataDirectory subEXIFDir = null;
            for ( Iterator<Map.Entry<Integer,ImageMetaValue>>
                  i = exifDir.iterator(); i.hasNext(); ) {
                final Map.Entry<Integer,ImageMetaValue> me = i.next();
                final int tagID = me.getKey();
                switch ( tagID ) {
                    case EXIF_GPS_IFD_POINTER:
                    case EXIF_IFD_POINTER:
                        continue;
                }
                if ( tagID >= EXIF_SUBEXIF_TAG_ID_START ) {
                    if ( subEXIFDir == null )
                        subEXIFDir = metadata.getDirectoryFor(
                            SubEXIFDirectory.class, true
                        );
                    subEXIFDir.putValue( tagID, me.getValue() );
                    i.remove();
                }
            }
            if ( subEXIFDir != null ) {
                //
                // We need a sub-EXIF IFD pointer in the EXIF directory.  (The
                // real value is filled-in by the EXIF encoder.)
                //
                exifDir.putValue(
                    EXIF_IFD_POINTER, new UnsignedLongMetaValue( 0 )
                );
            }

            //
            // JPEG doesn't have a TIFF directory.
            //
            metadata.removeDirectory( TIFFDirectory.class );
        }

        //
        // See if GPS metadata is present: if so, add a GPS IFD pointer to the
        // EXIF directory.  (The real value is filled-in by the EXIF encoder.)
        //
        final ImageMetadataDirectory gpsDir =
            metadata.getDirectoryFor( GPSDirectory.class );
        if ( gpsDir != null )
            exifDir.putValue(
                EXIF_GPS_IFD_POINTER, new UnsignedLongMetaValue( 0 )
            );

        //
        // Remove all maker notes since we currently don't export them.
        //
        for ( Iterator<ImageMetadataDirectory> i =
              metadata.getDirectories().iterator(); i.hasNext(); ) {
            final ImageMetadataDirectory dir = i.next();
            if ( dir instanceof MakerNotesDirectory )
                i.remove();
        }

        CoreDirectory.syncEditableMetadata( metadata );
        CoreDirectory.syncImageDimensions( metadata );

        return metadata;
    }

    /**
     * Prepare the EXIF, subEXIF, and TIFF directories for export as XMP.  This
     * means adding some fields, setting the relevant fields for the correct
     * image width/height, and removing others that are no longer correct given
     * the changes.
     * <p>
     * The original metadata is unchanged; instead, a modified clone is
     * returned.
     *
     * @param useActualOrientation If <code>true</code>, use the actual
     * orientation of the image; otherwise just use landscape.
     * @return Returns the prepared <code>ImageMetadata</code>.
     * @see #prepForExport(ImageType,boolean)
     */
    public ImageMetadata prepForXMP( boolean useActualOrientation ) {
        final ImageMetadata metadata =
            prepForExport( TIFFImageType.INSTANCE, false );

        //
        // Remove tags that make no sense in XMP.
        //
        metadata.removeValues(
            EXIFDirectory.class,
            EXIF_IFD_POINTER,
            EXIF_GPS_IFD_POINTER,
            EXIF_ICC_PROFILE,
            EXIF_MS_RATING,
            EXIF_ORIENTATION,
            EXIF_SUB_IFDS
        );
        metadata.removeValues(
            TIFFDirectory.class,
            TIFF_EXIF_IFD_POINTER,
            TIFF_GPS_IFD_POINTER,
            TIFF_ICC_PROFILE,
            TIFF_MS_RATING,
            TIFF_RICH_TIFF_IPTC,
            TIFF_SUB_IFDS,
            TIFF_XMP_PACKET
        );

        //
        // If we are to use the actual orientation, use the authoritative value
        // in the Core directory; otherwise, just use landscape.
        //
        final ImageOrientation orientation = useActualOrientation ?
            metadata.getOrientation() :
            ImageOrientation.ORIENTATION_LANDSCAPE;

        metadata.putValue(
            TIFFDirectory.class, TIFF_ORIENTATION,
            new UnsignedShortMetaValue( orientation.getTIFFConstant() )
        );

        return metadata;
    }

    /**
     * Puts the given {@link ImageMetadataDirectory} into this
     * <code>ImageMetadata</code> replacing any previous one.
     *
     * @param dir The {@link ImageMetadataDirectory} to put.
     * @return Returns the previous {@link ImageMetadataDirectory} or
     * <code>null</code> if there was no previos directory.
     */
    public ImageMetadataDirectory putDirectory( ImageMetadataDirectory dir ) {
        final Class dirClass = dir.getClass();
        return m_classToDirMap.put( dirClass, dir );
    }

    /**
     * Puts a key/value pair into the given {@link ImageMetadataDirectory}.
     *
     * @param dirClass The class of the {@link ImageMetadataDirectory}
     * @param tagID The metadata tag ID (the key).
     * @param value The {@link ImageMetaValue} to put.
     * @return Returns <code>true</code> only if the directory existed (or was
     * created) and the value was put into it.
     * @see #putValue(Class,int,ImageMetaValue,boolean)
     */
    public boolean putValue( Class<? extends ImageMetadataDirectory> dirClass,
                             int tagID, ImageMetaValue value ) {
        return putValue( dirClass, tagID, value, true );
    }

    /**
     * Puts a key/value pair into the given {@link ImageMetadataDirectory}.
     *
     * @param dirClass The class of the {@link ImageMetadataDirectory}
     * @param tagID The metadata tag ID (the key).
     * @param value The {@link ImageMetaValue} to put.
     * @param create If <code>true</code>, creates the
     * {@link ImageMetadataDirectory} if it doesn't exist.
     * @return Returns <code>true</code> only if the directory existed (or was
     * created) and the value was put into it.
     * @see #putValue(Class,int,ImageMetaValue)
     */
    public boolean putValue( Class<? extends ImageMetadataDirectory> dirClass,
                             int tagID, ImageMetaValue value, boolean create ) {
        final ImageMetadataDirectory dir = getDirectoryFor( dirClass, create );
        if ( dir != null ) {
            dir.putValue( tagID, value );
            return true;
        }
        return false;
    }

    /**
     * Remove the given {@link ImageMetadataDirectory}.
     *
     * @param dirClass The {@link Class} of the {@link ImageMetadataDirectory}
     * to remove.
     * @return Returns the removed {@link ImageMetadataDirectory} or
     * <code>null</code> if there was no such directory to remove.
     */
    public ImageMetadataDirectory removeDirectory( Class dirClass ) {
        return m_classToDirMap.remove( dirClass );
    }

    /**
     * Remove all directories that have no metadata.
     */
    public void removeAllEmptyDirectories() {
        for ( Iterator<Map.Entry<Class,ImageMetadataDirectory>>
              i = m_classToDirMap.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry<Class,ImageMetadataDirectory> me = i.next();
            final ImageMetadataDirectory dir = me.getValue();
            if ( dir.isEmpty() )
                i.remove();
        }
    }

    /**
     * Remove all string values that are empty in all directories.
     */
    public void removeAllEmptyStringValues() {
        for ( ImageMetadataDirectory dir : m_classToDirMap.values() )
            dir.removeAllEmptyStringValues();
        CoreDirectory.syncEditableMetadata( this );
    }

    /**
     * Removes the values for a set of tag IDs in a particular
     * {@link ImageMetadataDirectory}.  If a tag ID isn't found in the given
     * directory, each directory along the static parent chain is checked
     * also.
     *
     * @param dirClass The {@link Class} of a class derived from
     * {@link ImageMetadataDirectory}.
     * @param tagIDs The set of IDs of the tags to remove.
     * @see ImageMetadataDirectory#getStaticParent()
     */
    public void removeValues( Class<? extends ImageMetadataDirectory> dirClass,
                              int... tagIDs ) {
        final ImageMetadataDirectory dir = getDirectoryFor( dirClass );
        if ( dir != null )
            for ( int tagID : tagIDs )
                dir.removeValue( tagID );
    }

    /**
     * Sets the {@link ImageType} of the image this metadata is for.
     *
     * @param imageType The new {@link ImageType}.
     */
    public synchronized void setImageType( ImageType imageType ) {
        m_imageType = imageType;
    }

    /**
     * Sets the orientation of the image.
     *
     * @param orientation The new {@link ImageOrientation}.
     */
    public void setOrientation( ImageOrientation orientation ) {
        final ImageMetadataDirectory dir =
            getDirectoryFor( CoreDirectory.class, true );
        dir.setValue( CORE_IMAGE_ORIENTATION, orientation.getTIFFConstant() );
    }

    /**
     * Sets the rating of the image.
     *
     * @param rating The rating; must be in the range 0-5.
     * @see #clearRating()
     * @see #getRating()
     */
    @Override
    public void setRating( int rating ) {
        if ( rating < 0 || rating > 5 )
            throw new IllegalArgumentException( "rating must be between 0-5" );
        findProvidersOf(RatingProvider.class)
                .forEach(dir -> ((RatingProvider) dir).setRating(rating));
    }

    /**
     * Convert all the metadata into a single {@link String} for debugging
     * purposes.
     *
     * @return Returns said {@link String}.
     */
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for ( ImageMetadataDirectory dir : getDirectories() ) {
            sb.append( dir.toString() );
            sb.append( '\n' );
        }
        return sb.toString();
    }

    /**
     * Convert all the metadata into an XMP XML document.
     *
     * @param useActualOrientation If <code>true</code>, use the actual
     * orientation of the image; otherwise just use landscape.
     * @param includeXMPPacket If <code>true</code>, XMP packet processing
     * instructions are included in the new document.
     * @return Returns said document.
     */
    public Document toXMP(boolean useActualOrientation, boolean includeXMPPacket) {
        return toXMP(useActualOrientation, includeXMPPacket, null);
    }

    /**
     * Convert all the metadata into an XMP XML document.
     *
     * @param useActualOrientation If <code>true</code>, use the actual
     * orientation of the image; otherwise just use landscape.
     * @param includeXMPPacket If <code>true</code>, XMP packet processing
     * instructions are included in the new document.
     * @param dirClass The set of directories to include or <code>null</code>
     * for all.
     * @return Returns said document.
     */
    public Document toXMP( boolean useActualOrientation,
                           boolean includeXMPPacket,
                           Class<? extends ImageMetadataDirectory> dirClass)
    {
        final ImageMetadata metadata = prepForXMP( useActualOrientation );
        final Document doc = XMPUtil.createEmptyXMPDocument( includeXMPPacket );
        metadata.toXMP( doc, dirClass );
        return doc;
    }

    /**
     * Convert all the metadata into an XMP XML document.
     *
     * @param xmpDoc The XMP XML document to use.
     * @param dirClass The set of directories to include or <code>null</code>
     * for all.
     */
    private void toXMP( Document xmpDoc,
                       Class<? extends ImageMetadataDirectory> dirClass ) {
        final Element rdfElement = XMPUtil.getRDFElementOf( xmpDoc );
        for ( ImageMetadataDirectory dir : getDirectories() ) {
            if (dirClass == null || dirClass == dir.getClass()) {
                final Collection<Element> rdfDescElements = dir.toXMP(xmpDoc);
                if (rdfDescElements != null)
                    for (Element element : rdfDescElements)
                        rdfElement.appendChild(element);
            }
        }
        final Element dcRDFDescElement = toDublinCoreXMP( xmpDoc );
        if ( dcRDFDescElement != null )
            rdfElement.appendChild( dcRDFDescElement );
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        for ( int count = in.readShort(); count > 0; --count ) {
            try {
                final Class dirClass = Class.forName( in.readUTF() );
                //noinspection unchecked
                getDirectoryFor( dirClass, true ).readExternal( in );
            }
            catch ( ClassNotFoundException e ) {
                throw new IOException(e);
            }
        }
    }

    /**
     * @serialData The number of {@link ImageMetadataDirectory} objects
     * (<code>short</code>) followed by pairs of each directory's class's name
     * (<code>String</code>) and {@link ImageMetadataDirectory}.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeShort( m_classToDirMap.size() );
        for ( Map.Entry<Class,ImageMetadataDirectory>
              me : m_classToDirMap.entrySet() ) {
            final Class dirClass = me.getKey();
            final ImageMetadataDirectory dir = me.getValue();
            out.writeUTF( dirClass.getName() );
            dir.writeExternal( out );
        }
    }

    static final List<Integer> unexportedTags = Arrays.asList(
            EXIF_CFA_PATTERN,
            EXIF_COMPONENTS_CONFIGURATION,
            EXIF_COMPRESSED_BITS_PER_PIXEL,
            EXIF_IFD_POINTER, // re-added later if needed
            EXIF_INTEROPERABILITY_POINTER,
            EXIF_JPEG_INTERCHANGE_FORMAT,
            EXIF_JPEG_INTERCHANGE_FORMAT_LENGTH,
            EXIF_MAKER_NOTE,
            EXIF_SPATIAL_FREQUENCY_RESPONSE,
            EXIF_SUBJECT_AREA,
            TIFF_CELL_LENGTH,
            TIFF_CELL_WIDTH,
            TIFF_CLIP_PATH,
            TIFF_COLOR_MAP,
            TIFF_COMPRESSION,
            TIFF_DOT_RANGE,
            TIFF_EXTRA_SAMPLES,
            TIFF_FILL_ORDER,
            TIFF_FREE_BYTE_COUNTS,
            TIFF_FREE_OFFSETS,
            TIFF_GRAY_RESPONSE_CURVE,
            TIFF_GRAY_RESPONSE_UNIT,
            TIFF_HALFTONE_HINTS,
            TIFF_INDEXED,
            TIFF_JPEG_AC_TABLES,
            TIFF_JPEG_DC_TABLES,
            TIFF_JPEG_INTERCHANGE_FORMAT,
            TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH,
            TIFF_JPEG_LOSSLESS_PREDICTORS,
            TIFF_JPEG_POINT_TRANSFORMS,
            TIFF_JPEG_PROC,
            TIFF_JPEG_Q_TABLES,
            TIFF_JPEG_RESTART_INTERVAL,
            TIFF_LIGHTZONE,
            TIFF_NEW_SUBFILE_TYPE,
            TIFF_OPI_PROXY,
            TIFF_PHOTOSHOP_IMAGE_RESOURCES,
            TIFF_PLANAR_CONFIGURATION,
            TIFF_PREDICTOR,
            TIFF_PRIMARY_CHROMATICITIES,
            TIFF_REFERENCE_BLACK_WHITE,
            TIFF_ROWS_PER_STRIP,
            TIFF_SAMPLE_FORMAT,
            TIFF_SAMPLES_PER_PIXEL,
            TIFF_STRIP_BYTE_COUNTS,
            TIFF_SUBFILE_TYPE,
            TIFF_STRIP_OFFSETS,
            TIFF_SUB_IFDS,
            TIFF_T4_OPTIONS,
            TIFF_T6_OPTIONS,
            TIFF_THRESHHOLDING,
            TIFF_TILE_BYTE_COUNTS,
            TIFF_TILE_OFFSETS,
            TIFF_TRANSFER_FUNCTION,
            TIFF_TRANSFER_RANGE,
            TIFF_WHITE_POINT,
            TIFF_X_CLIP_PATH_UNITS,
            TIFF_X_POSITION,
            TIFF_YCBCR_COEFFICIENTS,
            TIFF_YCBCR_POSITIONING,
            TIFF_YCBCR_SUBSAMPLING,
            TIFF_Y_CLIP_PATH_UNITS,
            TIFF_Y_POSITION
    );

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>ProviderComparator</code> is-a {@link Comparator} for comparing
     * {@link ImageMetadataDirectory} objects by their priority for being a
     * provider for some metadata.
     */
    private static final class ProviderComparator
        implements Comparator<ImageMetadataDirectory> {

        ////////// public /////////////////////////////////////////////////////

        /**
         * Compares two {@link ImageMetadataDirectory} objects for priority
         * order in providing metadata as a particular
         * {@link ImageMetadataProvider}.
         *
         * @param dir1 The first {@link ImageMetadataDirectory} to be compared.
         * @param dir2 The second {@link ImageMetadataDirectory} to be compared.
         * @return Returns a negative integer, zero, or a positive integer as
         * the first directory's provider priority is greater than, equal to,
         * or less than the second.
         */
        public int compare( ImageMetadataDirectory dir1,
                            ImageMetadataDirectory dir2 ) {
            return  dir2.getProviderPriorityFor( m_provider ) -
                    dir1.getProviderPriorityFor( m_provider );
        }

        ////////// package ////////////////////////////////////////////////////

        /**
         * Construct a <code>ProviderComparator</code>.
         *
         * @param provider The provider interface to use for comparisons.
         */
        ProviderComparator( Class<? extends ImageMetadataProvider> provider ) {
            m_provider = provider;
        }

        ////////// private ////////////////////////////////////////////////////

        private final Class<? extends ImageMetadataProvider> m_provider;
    }

    /**
     * Convert relevant metadata into an XMP Dublin Core RDF element.
     *
     * @param xmpDoc The XMP XML document to use.
     * @return Returns said {@link Element} or <code>null</code>
     */
    private Element toDublinCoreXMP( Document xmpDoc ) {
        final String artist = getArtist();
        final String description = getCaption();
        final String rights = getCopyright();
        final String title = getTitle();
        if ( artist == null && description == null && rights == null &&
             title == null )
            return null;

        final Element dcRDFDescElement =
            XMPUtil.createRDFDescription( xmpDoc, XMP_DC_NS, XMP_DC_PREFIX );

        if ( artist != null ) {
            final Element creatorElement = xmpDoc.createElementNS(
                XMP_DC_NS, XMP_DC_PREFIX + ":creator"
            );
            XMLUtil.setTextContentOf( creatorElement, artist );
            dcRDFDescElement.appendChild( creatorElement );
        }

        if ( description != null ) {
            final Element descriptionElement = xmpDoc.createElementNS(
                XMP_DC_NS, XMP_DC_PREFIX + ":description"
            );
            XMLUtil.setTextContentOf( descriptionElement, description );
            dcRDFDescElement.appendChild( descriptionElement );
        }

        if ( rights != null ) {
            final Element rightsElement = xmpDoc.createElementNS(
                XMP_DC_NS, XMP_DC_PREFIX + ":rights"
            );
            XMLUtil.setTextContentOf( rightsElement, rights );
            dcRDFDescElement.appendChild( rightsElement );
        }

        if ( title != null ) {
            final Element titleElement = xmpDoc.createElementNS(
                XMP_DC_NS, XMP_DC_PREFIX + ":title"
            );
            XMLUtil.setTextContentOf( titleElement, title );
            dcRDFDescElement.appendChild( titleElement );
        }

        return dcRDFDescElement;
    }

    /**
     * The map from directory classes to instances thereof.
     */
    private final Map<Class,ImageMetadataDirectory> m_classToDirMap =
        new HashMap<Class,ImageMetadataDirectory>();

    /**
     * The type of the image file this metadata is for.
     */
    private ImageType m_imageType;

    ////////// main (for testing) /////////////////////////////////////////////

    public static void main( String[] args ) throws Exception {
        ImageMetadata metadata;
        try (FileOutputStream fos = new FileOutputStream("/tmp/out");
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            final ImageInfo info = ImageInfo.getInstanceFor(new File(args[0]));
            metadata = info.getMetadata();
            metadata.writeExternal(oos);
        }
        try (FileInputStream fis = new FileInputStream("/tmp/out");
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            metadata = new ImageMetadata();
            metadata.readExternal(ois);
        }
        System.out.println( metadata.toString() );
    }
}
/* vim:set et sw=4 ts=4: */
