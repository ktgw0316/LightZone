/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image;

import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.ResourceBundle;
import javax.media.jai.PlanarImage;

import org.w3c.dom.Document;

import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.types.*;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.editor.LightweightDocument;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.bytebuffer.LCReopenableMappedByteBuffer;
import com.lightcrafts.utils.bytebuffer.SoftChunkyFileByteBuffer;
import com.lightcrafts.utils.CloseableManager;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.xml.XmlDocument;

/**
 * An <code>ImageInfo</code> holds various information about an image in a
 * single object.  By having all the information in a single object, it alone
 * can be passed around as a parameter to methods and each method can get
 * access to the information it needs.  This is handy for two reasons:
 *  <ol>
 *    <li>
 *      Methods that need several pieces of information don't require long
 *      parameter lists.
 *    </li>
 *    <li>
 *      Methods that call other methods don't need to have a union of all the
 *      parameters that all the called methods need.
 *    </li>
 *  </ol>
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ImageInfo {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Checks whether the metadata for this image can be written based on file
     * and directory permissions.  A write can still fail for other reasons.
     *
     * @return Returns <code>true</code> only if writing metadata for this
     * image can be written.
     */
    public boolean canWriteMetadata() {
        //
        // To simplify things, we assume that if the directory the image is in
        // can't be written to that the metadata can't be written either.
        //
        // Under Windows, however, there's no such thing as a non-writable
        // directory that the current user owns.  Therefore, you'd think that
        // File.canWrite() would always true for all directories.
        //
        // However, due to a bug in Java:
        //
        //      http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4939819
        //
        // File.canWrite() returns false for the "My Documents" and the "My
        // Pictures" folders (and quite possibly any folder under "My
        // Documents").
        //
        // Therefore, under Windows, we don't do this check at all.
        //
        if ( !Platform.isWindows() && !m_imageFile.getParentFile().canWrite() )
            return false;
        try {
            final ImageType t = getImageType();
            //
            // TODO: if this code proves useful, refactor so that ImageType has
            // its own canWriteMetadata() method so we can take out the special
            // case for JPEGs below.
            //
            if ( t instanceof JPEGImageType ) {
                //
                // Metadata for JPEGs is possibly written back to the file
                // itself.
                //
                return m_imageFile.canWrite();
            }
            //
            // For TIFF and raw images, metadata is written to sidecar XMP
            // files.
            //
            final File xmpFile = new File( getXMPFilename() );
            return !xmpFile.exists() || xmpFile.canWrite();
        }
        catch ( Exception e ) {
            return false;
        }
    }

    /**
     * Close all open {@link LCByteBuffer}s.
     */
    public static void closeAll() throws IOException {
        ImageFileManager.INSTANCE.closeAllBut( 0 );
    }

    /**
     * Check this <code>ImageInfo</code> against another object for equality.
     *
     * @param o The {@link Object} to check against.
     * @return Returns <code>true</code> only if the other object is also an
     * <code>ImageInfo</code> and their files are equal.
     */
    public boolean equals( Object o ) {
        if ( o instanceof ImageInfo ) {
            final ImageInfo otherInfo = (ImageInfo)o;
            return m_imageFile.equals( otherInfo.m_imageFile );
        }
        return false;
    }

    /**
     * Gets this <code>ImageInfo</code>'s hash code.
     *
     * @return Returns said hash code.
     */
    public int hashCode() {
        return m_imageFile.hashCode() + 1;
    }

    /**
     * Gets the {@link AuxiliaryImageInfo} object for an image, if any.
     *
     * @return Returns said {@link AuxiliaryImageInfo} or <code>null</code> if
     * there is none.
     */
    public synchronized AuxiliaryImageInfo getAuxiliaryInfo()
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        if ( m_auxInfo == null )
            m_auxInfo = getImageType().newAuxiliaryInfo( this );
        return m_auxInfo;
    }

    /**
     * Gets the {@link LCByteBuffer} containing the raw bytes of the entire
     * image file as read from disk.
     *
     * @return Returns said {@link LCByteBuffer}
     */
    public synchronized LCByteBuffer getByteBuffer() {
        if ( m_byteBuffer == null )
            //noinspection ConstantConditions
            if ( USE_MMAP )
                m_byteBuffer = new LCReopenableMappedByteBuffer(
                    m_imageFile, ImageFileManager.INSTANCE
                );
            else
                m_byteBuffer = new SoftChunkyFileByteBuffer(
                    m_imageFile, ImageFileManager.INSTANCE
                );
        return m_byteBuffer;
    }

    private static final boolean USE_MMAP = false;

    /**
     * Gets the current {@link ImageType} for this image.  Note that this does
     * <i>not</i> make any effort to determine the image type; it merely
     * returns the current {@link ImageType}.
     *
     * @return Returns said {@link ImageType} or <code>null</code> if it has
     * not yet been determined.
     * @see #getCurrentImageType()
     */
    public ImageType getCurrentImageType() {
        return m_imageType;
    }

    /**
     * Gets the current {@link ImageMetadata} for this image.  Note that this
     * does <i>not</i> actually read the metadata; it merely returns the
     * current {@link ImageMetadata} object.
     *
     * @return Returns this image's {@link ImageMetadata} object.
     * @see #getMetadata()
     */
    public synchronized ImageMetadata getCurrentMetadata() {
        if ( m_metadata == null )
            m_metadata = new ImageMetadata();
        return m_metadata;
    }

    /**
     * Gets the {@link File} the image resides in.
     *
     * @return Returns said file.
     * @see #getOriginalFile()
     */
    public File getFile() {
        return m_imageFile;
    }

    /**
     * Gets the actual image data of an image.
     *
     * @param thread The thread that will do the getting.
     * @return Returns said image data.
     */
    public PlanarImage getImage( ProgressThread thread )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException, UserCanceledException
    {
        return getImage( thread, false );
    }

    /**
     * Gets the actual image data of an image.
     *
     * @param thread The thread that will do the getting.
     * @param read2ndTIFFImage If <code>true</code> and the image file is a
     * TIFF file and it has a second TIFF image, read it instead of the first
     * image.  If the file is not a TIFF file, this flag has no effect.
     * @return Returns said image data.
     */
    public synchronized PlanarImage getImage( ProgressThread thread,
                                              boolean read2ndTIFFImage )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException, UserCanceledException
    {
        PlanarImage strongRef = m_imageRef != null ? m_imageRef.get() : null;
        if ( strongRef == null ) {
            final ImageType t = getImageType();
            if ( t instanceof TIFFImageType )
                strongRef = TIFFImageType.getImage(
                    this, thread, read2ndTIFFImage
                );
            else
                strongRef = t.getImage( this, thread );
            if ( strongRef != null )
                m_imageRef = new WeakReference<PlanarImage>( strongRef );
        }
        return strongRef;
    }

    /**
     * Gets the {@link ImageType} of an image.  Note that this can be slow for
     * images in files having a <code>.TIF</code> filename extension because
     * some are really raw images and so must be probed to determine their
     * true {@link ImageType}.
     * <p>
     * If you merely and quickly want to know whether an image file is
     * supported, use {@link ImageType#determineTypeByExtensionOf(File)}.
     *
     * @return Returns said {@link ImageType}.
     * @see #getCurrentImageType()
     */
    public synchronized ImageType getImageType()
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        if ( m_imageType == null ) {
            determinePreliminaryImageType();
            determineFinalImageType();
        }
        return m_imageType;
    }

    /**
     * Gets the <code>ImageInfo</code> instance for the given {@link File}.
     *
     * @param file The file the image resides in.
     * @return Returns said <code>ImageInfo</code>.
     */
    public static ImageInfo getInstanceFor( File file ) {
        return new ImageInfo( file );
    }

    /**
     * Gets the {@link ImageMetadata} object for this image, reading the
     * metadata from the image file if necessary.
     *
     * @return Returns this image's {@link ImageMetadata} object.
     */
    public synchronized ImageMetadata getMetadata()
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        getCurrentMetadata();
        if ( m_metadata.isEmpty() ) {
            //
            // Add the PlaceHolderDirectory to prevent possible infinite loops.
            // It is removed later by the call to removeEmptyDirectories().
            //
            m_metadata.putDirectory( PlaceHolderDirectory.INSTANCE );

            determinePreliminaryImageType();
            m_imageType.readMetadata( this );
            determineFinalImageType();
            CoreDirectory.addOriginalOrientation( this );

            final ImageMetadata xmpMetadata = readXMPMetadata();
            if ( xmpMetadata != null )
                m_metadata.mergeFrom( xmpMetadata );

            CoreDirectory.addMetadata( this );
            m_metadata.removeAllEmptyDirectories();
            m_metadata.clearEdited();
        }
        return m_metadata;
    }

    /**
     * Gets the original image {@link File} that this LZN image file refers to,
     * if any.
     *
     * @return Returns said {@link File} or <code>null</code> if this image is
     * an original itself and thus doesn't have an original image file.
     * @see #getFile()
     */
    public synchronized File getOriginalFile()
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        if ( m_originalImageFile == null ) {
            final ImageType t = getImageType();
            if ( t instanceof LZNDocumentProvider ) {
                final Document lznDoc =
                    ((LZNDocumentProvider)t).getLZNDocument( this );
                final XmlDocument xmlDocument = new XmlDocument( lznDoc );
                final LightweightDocument lwDoc =
                    new LightweightDocument( m_imageFile, xmlDocument );
                final File originalFile = lwDoc.getImageFile();
                if ( !m_imageFile.equals( originalFile ) )
                    m_originalImageFile = originalFile;
            }
        }
        return m_originalImageFile;
    }

    /**
     * Gets the actual preview image data of an image.
     *
     * @return Returns said image data or <code>null</code> is this image has
     * no such image data.
     */
    public RenderedImage getPreviewImage()
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        return getPreviewImage( 0, 0 );
    }

    /**
     * Gets the actual preview image data of an image.
     *
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @return Returns said image data or <code>null</code> is this image has
     * no such image data.
     */
    public synchronized RenderedImage getPreviewImage( int maxWidth,
                                                       int maxHeight )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        RenderedImage strongRef = m_previewImageRef != null ?
            m_previewImageRef.get() : null;
        if ( strongRef == null ) {
            final ImageType t = getImageType();
            strongRef = t.getPreviewImage( this, maxWidth, maxHeight );
            if ( strongRef != null )
                m_previewImageRef =
                    new WeakReference<RenderedImage>( strongRef );
        }
        return strongRef;
    }

    /**
     * Gets the actual thumbnail image data of an image.
     *
     * @return Returns said image data or <code>null</code> is this image has
     * no such image data.
     */
    public synchronized RenderedImage getThumbnailImage()
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        RenderedImage strongRef = m_thumbnailImageRef != null ?
            m_thumbnailImageRef.get() : null;
        if ( strongRef == null ) {
            final ImageType t = getImageType();
            strongRef = t.getThumbnailImage( this );
            if ( strongRef != null )
                m_thumbnailImageRef =
                    new WeakReference<RenderedImage>( strongRef );
        }
        return strongRef;
    }

    /**
     * Gets the full path to the XMP sidecar file for this image file.
     *
     * @return Returns said filename.
     */
    public String getXMPFilename()
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ImageType t = getImageType();
        final String fileName = m_imageFile.getAbsolutePath();

        final int dot = fileName.lastIndexOf( '.' );
        if ( dot <= 0 || dot == fileName.length() - 1 )
            throw new UnknownImageTypeException( "no filename extension" );

        if ( t instanceof RawImageType )
            return fileName.substring( 0, dot + 1 ) + "xmp";

        return  fileName.substring( 0, dot ) +
                '_' + fileName.substring( dot + 1 ) + ".xmp";
    }

    /**
     * Sets this image's {@link ImageMetadata}.
     *
     * @param newMetadata The new {@link ImageMetadata}.
     */
    public void setMetadata( ImageMetadata newMetadata ) {
        m_metadata = newMetadata;
    }

    /**
     * Gets the {@link String} representation of this <code>ImageInfo</code>.
     *
     * @return Returns the name of the image file.
     */
    public String toString() {
        return m_imageFile.toString();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * An <code>ImageFileManager</code> is-a {@link CloseableManager} for
     * managing the number of open image files.
     * <p>
     * Because an {@link ImageInfo} must keep a <code>FileByteBuffer</code>
     * (and its associated {@link RandomAccessFile}) open, the file descriptor
     * remains in use until for a while and the system can run low on file
     * descriptors.  Also, under Windows, you can't rename or delete a file
     * that's open.
     * <p>
     * Therefore, an <code>ImageFileManager</code> limits the number of open
     * files to a small number.  When a new {@link RandomAccessFile} is
     * requested, an old one is closed.
     */
    private static final class ImageFileManager implements CloseableManager {

        ////////// public /////////////////////////////////////////////////////

        /**
         * {@inheritDoc}
         */
        public void manage( Closeable closeable ) throws IOException {
            synchronized ( m_closeableList ) {
                closeAllBut( MAX_OPEN_FILES );
                m_closeableList.add( closeable );
            }
        }

        ////////// package ////////////////////////////////////////////////////

        /** The singleton instance. */
        static final ImageFileManager INSTANCE = new ImageFileManager();

        /**
         * Close all {@link Closeable}s except for <i>n</i> of them.
         *
         * @param n The number of {@link Closeable}s to keep open at most.
         */
        void closeAllBut( int n ) throws IOException {
            synchronized ( m_closeableList ) {
                while ( m_closeableList.size() > n ) {
                    m_closeableList.removeFirst().close();
                }
            }
        }

        ////////// private ////////////////////////////////////////////////////

        /**
         * The maximum number of {@link RandomAccessFile}s to keep open.
         */
        private static final int MAX_OPEN_FILES = 2;

        /**
         * A collection of all the {@link Closeable}s in use.
         */
        private final LinkedList<Closeable> m_closeableList =
            new LinkedList<Closeable>();
    }

    /**
     * A <code>PlaceHolderDirectory</code> is-an {@link ImageMetadataDirectory}
     * that's used as a place-holder while {@link ImageInfo#getMetadata()} is
     * executing.  The singleton instance of <code>PlaceHolderDirectory</code>
     * is added to the {@link ImageMetadata} object being constructed so that
     * it's not empty.  This prevents an infinite loop (and subsequent stack
     * overflow) for TIFF files that are probed to see if they're really raw
     * files by a {@link TrueImageTypeProvider} that uses metadata.
     */
    @SuppressWarnings({"CloneableClassWithoutClone"})
    private static final class PlaceHolderDirectory
        extends ImageMetadataDirectory
    {
        static final PlaceHolderDirectory INSTANCE = new PlaceHolderDirectory();

        public String getName() {
            return "PlaceHolder";
        }
        public ImageMetaTagInfo getTagInfoFor( Integer id ) {
            throw new UnsupportedOperationException();
        }
        public ImageMetaTagInfo getTagInfoFor( String name ) {
            throw new UnsupportedOperationException();
        }
        protected ResourceBundle getTagLabelBundle() {
            throw new UnsupportedOperationException();
        }
        protected Class<? extends ImageMetaTags> getTagsInterface() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Constructs an <code>ImageInfo</code>.
     *
     * @param file The file the image resides in.
     */
    private ImageInfo( File file ) {
        m_imageFile = FileUtil.resolveAliasFile( file );
    }

    /**
     * Determines the final {@link ImageType} for this image.
     *
     * @see #determinePreliminaryImageType()
     */
    private void determineFinalImageType()
        throws BadImageFileException, IOException
    {
        if ( m_imageType instanceof TrueImageTypeProvider ) {
            final TrueImageTypeProvider p = (TrueImageTypeProvider)m_imageType;
            final ImageType trueType = p.getTrueImageTypeOf( this );
            if ( trueType != null )
                m_imageType = trueType;
        }
        if ( m_metadata != null )
            m_metadata.setImageType( m_imageType );
    }

    /**
     * Determines the preliminary {@link ImageType} for this image based solely
     * on the image file's filename extension.  Unfortunately, this may not be
     * correct because of the special case of some raw images being in files
     * having a <code>.TIF</code> extension.
     * <p>
     * The reason that determining the {@link ImageType} has to be split into
     * two parts is to prevent an infinite loop during final {@link ImageType}
     * determination that can use an image's metadata that in turn needs to
     * know the {@link ImageType} in order to read the metadata.  (The
     * preliminary {@link ImageType} is good enough for reading metadata since
     * all raw images in files having a <code>.TIF</code> extension use TIFF
     * metadata.)
     *
     * @throws UnknownImageTypeException if the image type could not be
     * determined.
     * @see #determineFinalImageType()
     */
    private void determinePreliminaryImageType()
        throws UnknownImageTypeException
    {
        if ( m_imageType == null ) {
            m_imageType = ImageType.determineTypeByExtensionOf( m_imageFile );
            if ( m_imageType == null )
                throw new UnknownImageTypeException( m_imageFile.getName() );
        }
    }

    /**
     * Reads the {@link ImageMetadata} from a sidecar XMP file, if any.
     *
     * @return Returns said {@link ImageMetadata} or <code>null</code> if there
     * is no sidecar XMP file.
     */
    private ImageMetadata readXMPMetadata()
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final String xmpFilename = getXMPFilename();
        if ( xmpFilename != null ) {
            try {
                return XMPMetadataReader.readFrom( new File( xmpFilename ) );
            }
            catch ( FileNotFoundException e ) {
                // ignore
            }
        }
        return null;
    }

    private AuxiliaryImageInfo m_auxInfo;
    private LCByteBuffer m_byteBuffer;
    private final File m_imageFile;
    private WeakReference<PlanarImage> m_imageRef;
    private ImageType m_imageType;
    private ImageMetadata m_metadata;
    private File m_originalImageFile;
    private WeakReference<RenderedImage> m_previewImageRef;
    private WeakReference<RenderedImage> m_thumbnailImageRef;
}
/* vim:set et sw=4 ts=4: */
