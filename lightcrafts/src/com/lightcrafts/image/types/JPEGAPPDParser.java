/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import static com.lightcrafts.image.types.AdobeConstants.*;

/**
 * A <code>JPEGAPPDParser</code> is-an {@link AdobeResourceParser} that parses
 * the APPD segment of a JPEG file that usually contains IPTC metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class JPEGAPPDParser extends AdobeResourceParser {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Parse a JPEG APPD segment.
     *
     * @param handler The {@link AdobeResourceParserEventHandler} to use.
     * @param jpegFile The {@link File} being parsed.
     * @param buf The {@link LCByteBuffer} containing the resource block data.
     */
    public static void parse( AdobeResourceParserEventHandler handler,
                              File jpegFile, LCByteBuffer buf )
        throws BadImageFileException, IOException
    {
        try {
            //
            // A JPEG APPD segment is a set of Adobe resource blocks preceeded
            // by a "Photoshop 3.0" header.
            //
            if ( !buf.getEquals( PHOTOSHOP_3_IDENT, "ASCII" ) ||
                  buf.get() != 0 )      // terminating null
                return;
            AdobeResourceParser.parse( handler, jpegFile, buf );
        }
        catch ( BufferUnderflowException e ) {
            throw new BadImageFileException( jpegFile, e );
        }
    }

}
/* vim:set et sw=4 ts=4: */
