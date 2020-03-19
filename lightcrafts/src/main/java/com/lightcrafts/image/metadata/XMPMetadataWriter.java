/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.w3c.dom.Document;

import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.xml.XMLUtil;

import static com.lightcrafts.image.metadata.Locale.LOCALE;

/**
 * An <code>XMPMetadataWriter</code> is used to write XMP metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class XMPMetadataWriter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Merge the given {@link ImageMetadata} into an XMP file if it exists or
     * create a new XMP file if not.
     *
     * @param metadata The {@link ImageMetadata} to merge/write.
     * @param xmpFile The XMP {@link File} to merge/write to.
     */
    public static void mergeInto( ImageMetadata metadata, File xmpFile )
        throws IOException
    {
        if ( xmpFile.isFile() && !xmpFile.canWrite() )
            throw new IOException( LOCALE.get( "XMPPermissionDenied" ) );

        //
        // First, create a Document for the new metadata.
        //
        Document newXMPDoc = metadata.toXMP( true, false );
        try {
            final Document oldXMPDoc = XMLUtil.readDocumentFrom( xmpFile );
            //
            // Second, there's an existing XMP file: merge metadata into it.
            //
            newXMPDoc = XMPUtil.mergeMetadata( newXMPDoc, oldXMPDoc );
        }
        catch ( FileNotFoundException e ) {
            // ignore
        }
        //
        // Lastly, write the merged metadata back to the same file.
        //
        final File tempFile =
            File.createTempFile( "LZT", ".xmp", xmpFile.getParentFile() );
        try {
            XMLUtil.writeDocumentTo( newXMPDoc, tempFile );
            FileUtil.renameFile( tempFile, xmpFile );
        }
        finally {
            tempFile.delete();
        }
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct an <code>XMPMetadataWriter</code>.
     */
    private XMPMetadataWriter() {
        // nothing
    }

}
/* vim:set et sw=4 ts=4: */
