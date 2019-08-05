/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.color.ICC_Profile;
import java.awt.image.RenderedImage;
import java.io.*;
import javax.media.jai.PlanarImage;

import org.w3c.dom.Document;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.libs.*;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.utils.xml.XMLUtil;

import static com.lightcrafts.image.libs.LCJPEGConstants.CS_RGB;

/**
 * <code>LznImageType</code> is the <code>ImageType</code> for LZN files,
 * the XML documents representing saved LightZone editor state.
 *
 * @author Anton Kast [anton@lightcrafts.com]
 */
public final class LZNImageType extends ImageType
    implements LZNDocumentProvider {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>LZNImageType</code>. */
    public static final LZNImageType INSTANCE = new LZNImageType();

    /**
     * <code>ExportOptions</code> are {@link ImageExportOptions} for LZN files.
     */
    public static final class ExportOptions extends ImageExportOptions {
        /**
         * Construct an <code>ExportOptions</code>.
         */
        public ExportOptions() {
            super( INSTANCE );
        }
    }

    /**
     * Return the single file extension associated with this ImageType,
     * "lzn".
     */
    public String[] getExtensions() {
        return EXTENSIONS;
    }

    /**
     * Get the color profile of LZN images, which is interpreted as the
     * specialized linear color profile used internally by the LightZone
     * editor.
     */
    public ICC_Profile getICCProfile( ImageInfo imageInfo ) {
        return JAIContext.linearProfile;
    }

    /**
     * Fetch the image from an LZN file by parsing it as XML, locating its
     * cache node, and interpreting the cache contents as image data.
     */
    public PlanarImage getImage( ImageInfo imageInfo, ProgressThread thread )
        throws BadImageFileException, IOException, UserCanceledException
    {
        final XmlDocument xml = getDocument( imageInfo );
        final XmlNode cache = getCacheNode( xml );
        final byte[] bytes = cache.getData();
        if (bytes == null) {
            // no cache data in the file
            throw new BadImageFileException( imageInfo.getFile() );
        }
        try {
            final InputStream in = new ByteArrayInputStream( bytes );
            final LCImageDataProvider provider =
                new InputStreamImageDataProvider( in );
            final LCJPEGReader reader = new LCJPEGReader( provider );
            return reader.getImage( thread, null );
        }
        catch ( LCImageLibException e ) {
            throw new BadImageFileException( imageInfo.getFile() );
        }
    }

    /**
     * {@inheritDoc}
     */
    public Document getLZNDocument( ImageInfo imageInfo ) throws IOException {
        return XMLUtil.readDocumentFrom( imageInfo.getFile() );
    }

    /**
     * {@inheritDoc}
     */
    public ExportOptions newExportOptions() {
        return new ExportOptions();
    }

    /**
     * Write the given RenderedImage into the cache area of the LZN file
     * referenced by the given ImageInfo.  This modifies the image file.
     * @param imageInfo The image file to alter.
     * @param image The image to put into the file's cache node.
     * @throws IOException If the file cannot be parsed as XML, or if there
     * is an authentic IO problem during the write.
     */
    public void putImage( ImageInfo imageInfo, RenderedImage image )
        throws IOException
    {
        try {
            final ByteArrayOutputStream buf = new ByteArrayOutputStream();
            final LCImageDataReceiver receiver =
                new OutputStreamImageDataReceiver( buf );
            final LCJPEGWriter writer = new LCJPEGWriter(
                receiver, 1024,
                image.getWidth(), image.getHeight(),
                image.getSampleModel().getNumBands(),
                CS_RGB,
                90
            );
            writer.putImage( image );

            final XmlDocument xml = getDocument( imageInfo );
            final XmlNode cache = getCacheNode( xml );
            cache.setData( buf.toByteArray() );

            try (OutputStream out = new FileOutputStream(imageInfo.getFile())) {
                xml.write(out);
            }
        }
        catch ( LCImageLibException e ) {
            throw new IOException( e.getMessage() );
        }
    }

    /**
     * Returns the name, "LZN".
     */
    public String getName() {
        return "LZN";
    }

    /**
     * Reads all the metadata for an LZN file.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public void readMetadata( ImageInfo imageInfo )
        throws IOException
    {
        // TODO
    }

    /**
     * Writes the metadata for an LZN file.
     *
     * @param imageInfo The image to write the metadata for.
     */
    public void writeMetadata( ImageInfo imageInfo )
        throws IOException
    {
        // TODO
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct an <code>LZNImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private LZNImageType() {
        // do nothing
    }

    /**
     * Get the XmlDocument of an LZN image file by parsing it as XML.
     * @param info The image file to parse.
     * @return The XML parse result
     * @throws IOException If XML parsing fails.
     */
    private static XmlDocument getDocument( ImageInfo info )
        throws IOException
    {
        final InputStream in = new FileInputStream( info.getFile() );
        return new XmlDocument( in );
    }

    /**
     * Locate the cache node in an LZN XmlDocument structure, or create a
     * new cache node if none can be found.
     * @param xml An XmlDocument parsed from an LZN file.
     * @return The XmlNode containing the LZN cache data, or a new cache node
     * if none was present.
     */
    private static XmlNode getCacheNode( XmlDocument xml ) {
        // Parse the LZN file and locate the cache node:
        final XmlNode root = xml.getRoot();
        try {
            return root.getChild(CacheTag);
        }
        catch (XMLException e) {
            return root.addChild(CacheTag);
        }
    }

    /**
     * All the possible filename extensions for LZN files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "lzn"
    };

    /**
     * Read and write cached image data.  See #putCachedImage and
     * #getCachedImage.
     */
    private static final String CacheTag = "Cache";
}
/* vim:set et sw=4 ts=4: */
