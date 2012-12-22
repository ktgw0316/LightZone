/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import static com.lightcrafts.image.types.AdobeConstants.*;

/**
 * An <code>AdobeResourceParser</code> parses Adobe resource blocks.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class AdobeResourceParser {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Parse Adobe resource blocks.
     *
     * @param handler The {@link AdobeResourceParserEventHandler} to use.
     * @param file The {@link File} being parsed.
     * @param buf The {@link LCByteBuffer} containing the resource block data.
     */
    public static void parse( AdobeResourceParserEventHandler handler,
                              File file, LCByteBuffer buf )
        throws BadImageFileException, IOException
    {
        while ( buf.position() < buf.limit() - ADOBE_RESOURCE_BLOCK_MIN_SIZE ) {
            try {
                if ( !buf.getEquals( PHOTOSHOP_CREATOR_CODE, "ASCII" ) )
                    return;
            }
            catch ( BufferUnderflowException e ) {
                return;
            }
            try {
                final int blockID = buf.getUnsignedShort();
                final int nameLen = buf.get();
                final String name = nameLen > 0 ?
                    buf.getString( nameLen, "ASCII" ) : "";
                if ( ((nameLen + 1) & 1) != 0 )
                    buf.get();
                int dataLength = buf.getInt();

                final int savedPos = buf.position();
                final boolean continueParsing = handler.gotResource(
                    blockID, name, dataLength, file, buf
                );
                if ( !continueParsing )
                    return;

                dataLength += dataLength & 1;
                buf.position( savedPos + dataLength );
            }
            catch ( BufferUnderflowException e ) {
                throw new BadImageFileException( file, e );
            }
        }
    }

}
/* vim:set et sw=4 ts=4: */
