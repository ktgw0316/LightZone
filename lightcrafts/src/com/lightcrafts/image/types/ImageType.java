/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import com.lightcrafts.mediax.jai.PlanarImage;

import org.w3c.dom.Document;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.xml.XMLUtil;

/**
 * An <code>ImageType</code> is an abstract base class used to get information
 * about and perform operations on various types of image files.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class ImageType {

    ////////// public ////////////////////////////////////////////////////////

    /**
     * Checks whether the application can export images to this image type.
     *
     * @return Returns <code>true</code> only if the application can export
     * images to this image type.
     * @see #newExportOptions()
     */
    public boolean canExport() {
        return false;
    }

    /**
     * Try to determine the type of image by its filename extension.
     *
     * @param file The {@link File} containing the image.
     * @return Returns the {@link ImageType} of the image or <code>null</code>
     * if the image type could not be determined.
     */
    public static ImageType determineTypeByExtensionOf( File file ) {
        final String extension = FileUtil.getExtensionOf( file );
        return extension != null ? findTypeFromExtension( extension ) : null;
    }

    /**
     * Find the <code>ImageType</code> that the given filename extension is an
     * extension for.
     *
     * @param extension The filename extension to find (without the leading
     * <code>'.'</code>).  The extension may be in either upper- or lower-case
     * (or any mixture thereof) because the search is done in a
     * case-insensitive manner.
     * @return Returns the associatied <code>ImageType</code> or
     * <code>null</code> if there is no <code>ImageType</code> uses the given
     * extension.
     */
    public static ImageType findTypeFromExtension( String extension ) {
        for ( ImageType t : m_types ) {
            if ( t instanceof LZNDocumentProvider &&
                 t != LZNImageType.INSTANCE )
                continue;
            if ( t.matchExtension( extension ) )
                return t;
        }
        return null;
    }

    /**
     * Get all the implemented <code>ImageType</code>s.
     *
     * @return Returns said <code>ImageType</code>s.
     */
    public static Collection<ImageType> getAllTypes() {
        return m_types;
    }

    /**
     * Gets the width and height of the image.
     *
     * @param imageInfo The image to get the dimension of.
     * @return Returns said dimension or <code>null</code> if unavailable.
     */
    public Dimension getDimension( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return null;
    }

    /**
     * Get the filename extension(s) that a given image file uses.  If more
     * than one is returned, the first one (array element 0) is the preferred
     * extension.
     *
     * @return Returns an array of filename extensions (without the
     * '<code>.</code>'s) and in all lower-case.
     */
    public abstract String[] getExtensions();

    /**
     * Get the ICC profile of an image.
     *
     * @param imageInfo The image to get the ICC profile from.
     * @return Returns the {@link ICC_Profile} or <code>null</code> if the
     * image doesn't have a color profile.
     */
    public ICC_Profile getICCProfile( ImageInfo imageInfo ) throws
        BadImageFileException, ColorProfileException, IOException,
        UnknownImageTypeException
    {
        return null;
    }

    /**
     * Gets the actual image data of an image.
     *
     * @param imageInfo The {@link ImageInfo} to get the actual image from.
     * @param thread The thread doing the getting.
     * @return Returns said image data.
     */
    public abstract PlanarImage getImage( ImageInfo imageInfo,
                                          ProgressThread thread )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException, UserCanceledException;

    /**
     * Look up an <code>ImageType</code> by its name.
     *
     * @param name The name of an <code>ImageType</code> as returned by
     * {@link #getName()}.
     * @return Returns the <code>ImageType</code> having the given name or
     * <code>null</code> if no such <code>ImageType</code> exists.
     */
    public static ImageType getImageTypeByName( String name ) {
        for ( ImageType t : m_types )
            if ( t.getName().equals( name ) )
                return t;
        return null;
    }

    /**
     * Returns the name of this image type.
     *
     * @return Returns said name.
     */
    public abstract String getName();

    /**
     * Gets the actual preview image data of an image.
     *
     * @param imageInfo The {@link ImageInfo} to get the actual preview image
     * from.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @return Returns said image data.
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        return null;
    }

    public boolean hasFastPreview() {
        return false;
    }

    /**
     * Gets the actual thumbnail image data of an image.
     *
     * @param imageInfo The {@link ImageInfo} to get the actual thumbnail image
     * from.
     * @return By default, returns <code>null</code> meaning there is no
     * thumbnail for a particular image type.
     */
    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        return null;
    }

    /**
     * Gets the XMP document embedded inside the image file, if any.
     *
     * @param imageInfo The image to read the XMP document from.
     * @return Returns said {@link Document} or <code>null</code> if none.
     */
    public Document getXMP( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return null;
    }

    /**
     * Creates an {@link AuxiliaryImageInfo} object for an image if it should
     * have one.
     *
     * @param imageInfo The image to create the {@link AuxiliaryImageInfo} for.
     * @return The default always returns <code>null</code>.
     */
    public AuxiliaryImageInfo newAuxiliaryInfo( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        return null;
    }

    /**
     * Creates a new instance of a class derived from
     * {@link ImageExportOptions} for this type of image.
     *
     * @return Returns an instance of a class derived from
     * {@link ImageExportOptions}.
     * @throws UnsupportedOperationException if this method is called for an
     * image type that can not be exported to.
     * @see #canExport()
     */
    public ImageExportOptions newExportOptions() {
        throw new UnsupportedOperationException(
            "can not export to " + getName()
        );
    }

    /**
     * Put (export) an image to a file.
     *
     * @param imageInfo The {@link ImageInfo} about the image to put.
     * @param image The actual image data.
     * @param options The {@link ImageExportOptions} to use.
     * @param lznData The LightZone transformation document for the image in
     * byte form.  This may be <code>null</code>.
     * @param thread The {@link ProgressThread} to use.
     * @throws UnsupportedOperationException if this method is called for an
     * image type that can not be exported to.
     * @see #canExport()
     * @deprecated
     */
    public final void putImage( ImageInfo imageInfo, PlanarImage image,
                                ImageExportOptions options, byte[] lznData,
                                ProgressThread thread ) throws IOException {
        final Document lznDoc = lznData != null ?
            XMLUtil.readDocumentFrom( new ByteArrayInputStream( lznData ) ) :
            null;
        putImage( imageInfo, image, options, lznDoc, thread );
    }

    /**
     * Put (export) an image to a file.
     *
     * @param imageInfo The {@link ImageInfo} about the image to put.
     * @param image The actual image data.
     * @param options The {@link ImageExportOptions} to use.
     * @param lznDoc The LightZone transformation document for the image.  This
     * may be <code>null</code>.
     * @param thread The {@link ProgressThread} to use.
     * @throws UnsupportedOperationException if this method is called for an
     * image type that can not be exported to.
     * @see #canExport()
     */
    public void putImage( ImageInfo imageInfo, PlanarImage image,
                          ImageExportOptions options, Document lznDoc,
                          ProgressThread thread ) throws IOException {
        throw new UnsupportedOperationException(
            "can not export to " + getName()
        );
    }

    /**
     * Reads all the metadata for a given image of a particular type.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public abstract void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException;

    /**
     * Writes all the metadata for a given image.
     *
     * @param imageInfo The image to write the metadata for.
     */
    public abstract void writeMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException;

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct an <code>ImageType</code> and add the instance to the global
     * static list of all <code>ImageType</code>s.
     */
    protected ImageType() {
        m_types.add( this );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * See if the given filename extension matches any of the filename
     * extension(s) of this <code>ImageType</code>.
     *
     * @param extension The filename extension to match.
     * @return Returns <code>true</code> only if the extension was matched by
     * this <code>ImageType</code>'s set of extensions.
     */
    private boolean matchExtension( String extension ) {
        final String extensionLC = extension.toLowerCase();
        for ( String e : getExtensions() )
            if ( extensionLC.equals( e ) )
                return true;
        return false;
    }

    /**
     * The global static list of all <code>ImageType</code>s.
     */
    private static final ArrayList<ImageType> m_types =
        new ArrayList<ImageType>();

    static {
        // TODO: is there a better way to do this?
        //noinspection UNUSED_SYMBOL
        final ImageType[] imageTypesToLoad = {
            ARWImageType.INSTANCE,
            CIFFImageType.INSTANCE,
            CR2ImageType.INSTANCE,
            DCRImageType.INSTANCE,
            DNGImageType.INSTANCE,
            ERFImageType.INSTANCE,
            FFFImageType.INSTANCE,
            JPEGImageType.INSTANCE,
            KDCImageType.INSTANCE,
            LZNImageType.INSTANCE,
            MOSImageType.INSTANCE,
            MRWImageType.INSTANCE,
            NEFImageType.INSTANCE,
            ORFImageType.INSTANCE,
            PanasonicRawImageType.INSTANCE,
            PEFImageType.INSTANCE,
            RAFImageType.INSTANCE,
            RW2ImageType.INSTANCE,
            SR2ImageType.INSTANCE,
            TIFFImageType.INSTANCE,
            // X3FImageType.INSTANCE,

            SidecarJPEGImageType.INSTANCE,
            SidecarTIFFImageType.INSTANCE,
            MultipageTIFFImageType.INSTANCE,
        };
    }
}
/* vim:set et sw=4 ts=4: */
