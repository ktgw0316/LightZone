/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;
import java.io.File;
import java.nio.BufferUnderflowException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;

import static com.lightcrafts.image.types.JPEGConstants.*;

/**
 * A <code>JPEGParser</code> parses the segments of a JPEG file.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class JPEGParser {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Parse all the segments from a JPEG image file.  A segment has the form:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td>0&nbsp;</td>
     *        <td>start byte: <code>0xFF</code></td>
     *      </tr>
     *      <tr>
     *        <td>1&nbsp;</td>
     *        <td>marker/ID</td>
     *      </tr>
     *      <tr>
     *        <td>2&#8211;3&nbsp;</td>
     *        <td>length (includes self)</td>
     *      </tr>
     *      <tr>
     *        <td>4...&nbsp;</td>
     *        <td>data (length &#8211; 2 bytes)</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     *
     * @param handler The {@link JPEGParserEventHandler} to use.
     * @param jpegFile The JPEG image file to parse.
     * @param buf {@link LCByteBuffer} containing the JPEG image file.
     */
    public static void parse( JPEGParserEventHandler handler, File jpegFile,
                              LCByteBuffer buf )
        throws BadImageFileException, IOException
    {
        try {
            buf.position( 2 );          // skip JPEG magic number
            while ( true ) {
                final byte b = buf.get();
                if ( b != JPEG_MARKER_BYTE )
                    throw new BadImageFileException(
                        jpegFile,
                        "JPEG marker byte (0xFF) expected; got: 0x"
                        + Integer.toHexString( b & 0x00FF )
                    );

                byte segID;
                do {                    // skip padding, if any
                    segID = buf.get();
                } while ( segID == JPEG_MARKER_BYTE );

                final int segLength;
                switch ( segID ) {

                    case JPEG_EOI_MARKER:
                        //
                        // We should never get here... but just in case.
                        //
                        return;

                    case JPEG_SOS_MARKER:
                        //
                        // The Start-of-Scan segment doesn't have a length.
                        //
                        segLength = 0;
                        break;

                    default:
                        //
                        // The segment length includes itself, so subtract 2 to
                        // get the actual length of the segment data.
                        //
                        segLength = buf.getUnsignedShort() - 2;
                        final int segRemaining = buf.remaining();
                        if ( segLength > segRemaining )
                            throw new BadImageFileException(
                                jpegFile,
                                "JPEG segment length (" + segLength
                                + ") > actual length (" + segRemaining + ')'
                            );
                        break;
                }

                final int savedPos = buf.position();
                final boolean continueParsing =
                    handler.gotSegment( segID, segLength, jpegFile, buf );
                if ( segID == JPEG_SOS_MARKER || !continueParsing )
                    break;
                buf.position( savedPos + segLength );
            }
        }
        catch ( BufferUnderflowException e ) {
            throw new BadImageFileException( jpegFile, e );
        }
        catch ( IllegalArgumentException e ) {
            throw new BadImageFileException( jpegFile, e );
        }
    }
}
/* vim:set et sw=4 ts=4: */
