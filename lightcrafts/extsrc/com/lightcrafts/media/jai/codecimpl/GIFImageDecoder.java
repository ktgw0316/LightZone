/*
 * $RCSfile: GIFImageDecoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2006/06/17 00:02:28 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import com.lightcrafts.media.jai.codec.ImageCodec;
import com.lightcrafts.media.jai.codec.ImageDecodeParam;
import com.lightcrafts.media.jai.codec.ImageDecoderImpl;
import com.lightcrafts.media.jai.codec.SeekableStream;
import com.lightcrafts.media.jai.codecimpl.ImagingListenerProxy;
import com.lightcrafts.media.jai.codecimpl.util.ImagingException;

/**
 * @since EA3
 */
public class GIFImageDecoder extends ImageDecoderImpl {

    // The global color table.
    private byte[] globalColorTable = null;

    // Whether the last page has been encountered.
    private boolean maxPageFound = false;

    // The maximum allowable page for reading.
    private int maxPage;

    // The previous page read.
    private int prevPage = -1;

    // The previous page on which getTile() was invoked in this object.
    private int prevSyncedPage = -1;

    // Map of Integer page numbers to RenderedImages.
    private HashMap images = new HashMap();

    /**
     * Read the overall stream header and return the global color map
     * or <code>null</code>.
     */
    private static byte[] readHeader(SeekableStream input) throws IOException {
        byte[] globalColorTable = null;
        try {
            // Skip the version string and logical screen dimensions.
            input.skipBytes(10);

            int packedFields = input.readUnsignedByte();
            boolean globalColorTableFlag = (packedFields & 0x80) != 0;
            int numGCTEntries = 1 << ((packedFields & 0x7) + 1);

            int backgroundColorIndex = input.readUnsignedByte();

            // Read the aspect ratio but ignore the returned value.
            input.read();

            if (globalColorTableFlag) {
                globalColorTable = new byte[3*numGCTEntries];
                input.readFully(globalColorTable);
            } else {
                globalColorTable = null;
            }
        } catch (IOException e) {
            String message = JaiI18N.getString("GIFImageDecoder0");
            ImagingListenerProxy.errorOccurred(message,
                                   new ImagingException(message, e),
                                   GIFImageDecoder.class, false);
//            throw new IOException(JaiI18N.getString("GIFImageDecoder0"));
        }

        return globalColorTable;
    }

    public GIFImageDecoder(SeekableStream input,
                           ImageDecodeParam param) {
        super(input, param);
    }

    public GIFImageDecoder(InputStream input,
                           ImageDecodeParam param) {
        super(input, param);
    }

    public int getNumPages() throws IOException {
        int page = prevPage + 1;

        while(!maxPageFound) {
            try {
                decodeAsRenderedImage(page++);
            } catch(IOException e) {
                // Ignore
            }
        }

        return maxPage + 1;
    }

    public synchronized RenderedImage decodeAsRenderedImage(int page)
        throws IOException {

        // Verify that the index is in range.
        if (page < 0 || (maxPageFound && page > maxPage)) {
            throw new IOException(JaiI18N.getString("GIFImageDecoder1"));
        }

        // Attempt to get the image from the cache.
        Integer pageKey = new Integer(page);
        if(images.containsKey(pageKey)) {
            return (RenderedImage)images.get(pageKey);
        }

        // If the zeroth image, set the global color table.
        if(prevPage == -1) {
            try {
                globalColorTable = readHeader(input);
            } catch(IOException e) {
                maxPageFound = true;
                maxPage = -1;
                throw e;
            }
        }

        // Force previous data to be read.
        if(page > 0) {
            for(int idx = prevSyncedPage + 1; idx < page; idx++) {
                RenderedImage im =
                    (RenderedImage)images.get(new Integer(idx));
                im.getTile(0, 0);
                prevSyncedPage = idx;
            }
        }

        // Read as many images as possible.
        RenderedImage image = null;
        while(prevPage < page) {
            int index = prevPage + 1;
            RenderedImage ri = null;
            try {
                ri = new GIFImage(input, globalColorTable);
                images.put(new Integer(index), ri);
                if(index < page) {
                    ri.getTile(0, 0);
                    prevSyncedPage = index;
                }
                prevPage = index;
                if(index == page) {
                    image = ri;
                    break;
                }
            } catch(IOException e) {
                maxPageFound = true;
                maxPage = prevPage;
                String message = JaiI18N.getString("GIFImage3");
                ImagingListenerProxy.errorOccurred(message,
                                   new ImagingException(message, e),
                                   this, false);
//                throw e;
            }
        }

        return image;
    }
}

/**
 * @since 1.1.1
 */
class GIFImage extends SimpleRenderedImage {
    // Constants used to control interlacing.
    private static final int[] INTERLACE_INCREMENT = { 8, 8, 4, 2, -1 };
    private static final int[] INTERLACE_OFFSET = { 0, 4, 2, 1, -1 };

    // The source stream.
    private SeekableStream input;

    // The interlacing flag.
    private boolean interlaceFlag = false;

    // Variables used by LZW decoding
    private byte[] block = new byte[255];
    private int blockLength = 0;
    private int bitPos = 0;
    private int nextByte = 0;
    private int initCodeSize;
    private int clearCode;
    private int eofCode;
    private int bitsLeft;

    // 32-bit lookahead buffer
    private int next32Bits = 0;

    // True if the end of the data blocks has been found,
    // and we are simply draining the 32-bit buffer
    private boolean lastBlockFound = false;

    // The current interlacing pass, starting with 0.
    private int interlacePass = 0;

    // The image's tile.
    private WritableRaster theTile = null;

    // Read blocks of 1-255 bytes, stop at a 0-length block
    private void skipBlocks() throws IOException {
        while (true) {
            int length = input.readUnsignedByte();
            if (length == 0) {
                break;
            }
            input.skipBytes(length);
        }
    }

    /**
     * Create a new <code>GIFImage</code>.  The input stream must
     * be positioned at the start of the image, i.e., not at the
     * start of the overall stream.
     *
     * @param input the stream from which to read.
     * @param globalColorTable the global colormap of <code>null</code>.
     *
     * @throws IOException.
     */
    GIFImage(SeekableStream input,
             byte[] globalColorTable) throws IOException {
        this.input = input;

        byte[] localColorTable = null;
        boolean transparentColorFlag = false;
        int transparentColorIndex = 0;

        // Read the image header initializing the local color table,
        // if any, and the transparent index, if any.

        try {
            long startPosition = input.getFilePointer();
            while (true) {
                int blockType = input.readUnsignedByte();
                if (blockType == 0x2c) { // Image Descriptor
                    // Skip image top and left position.
                    input.skipBytes(4);

                    width = input.readUnsignedShortLE();
                    height = input.readUnsignedShortLE();

                    int idPackedFields = input.readUnsignedByte();
                    boolean localColorTableFlag =
                        (idPackedFields & 0x80) != 0;
                    interlaceFlag = (idPackedFields & 0x40) != 0;
                    int numLCTEntries = 1 << ((idPackedFields & 0x7) + 1);

                    if (localColorTableFlag) {
                        // Read color table if any
                        localColorTable =
                            new byte[3*numLCTEntries];
                        input.readFully(localColorTable);
                    } else {
                        localColorTable = null;
                    }

                    // Now positioned at start of LZW-compressed pixels
                    break;
                } else if (blockType == 0x21) { // Extension block
                    int label = input.readUnsignedByte();

                    if (label == 0xf9) { // Graphics Control Extension
                        input.read(); // extension length
                        int gcePackedFields = input.readUnsignedByte();
                        transparentColorFlag =
                            (gcePackedFields & 0x1) != 0;

                        input.skipBytes(2); // delay time

                        transparentColorIndex
                            = input.readUnsignedByte();

                        input.read(); // terminator
                    } else if (label == 0x1) { // Plain text extension
                        // Skip content.
                        input.skipBytes(13);
                        // Read but do not save content.
                        skipBlocks();
                    } else if (label == 0xfe) { // Comment extension
                        // Read but do not save content.
                        skipBlocks();
                    } else if (label == 0xff) { // Application extension
                        // Skip content.
                        input.skipBytes(12);
                        // Read but do not save content.
                        skipBlocks();
                    } else {
                        // Skip over unknown extension blocks
                        int length = 0;
                        do {
                            length = input.readUnsignedByte();
                            input.skipBytes(length);
                        } while (length > 0);
                    }
                } else {
                    throw new IOException(JaiI18N.getString("GIFImage0")+" "+
                                          blockType + "!");
                }
            }
        } catch (IOException ioe) {
            throw new IOException(JaiI18N.getString("GIFImage1"));
        }

        // Set the image layout from the header information.

        // Set the image and tile grid origin to (0, 0).
        minX = minY = tileGridXOffset = tileGridYOffset = 0;

        // Force the image to have a single tile.
        tileWidth = width;
        tileHeight = height;

        byte[] colorTable;
        if (localColorTable != null) {
            colorTable = localColorTable;
        } else {
            colorTable = globalColorTable;
        }

        // Normalize color table length to 2^1, 2^2, 2^4, or 2^8
        int length = colorTable.length/3;
        int bits;
        if (length == 2) {
            bits = 1;
        } else if (length == 4) {
            bits = 2;
        } else if (length == 8 || length == 16) {
            // Bump from 3 to 4 bits
            bits = 4;
        } else {
            // Bump to 8 bits
            bits = 8;
        }
        int lutLength = 1 << bits;
        byte[] r = new byte[lutLength];
        byte[] g = new byte[lutLength];
        byte[] b = new byte[lutLength];

        // Entries from length + 1 to lutLength - 1 will be 0
        int rgbIndex = 0;
        for (int i = 0; i < length; i++) {
            r[i] = colorTable[rgbIndex++];
            g[i] = colorTable[rgbIndex++];
            b[i] = colorTable[rgbIndex++];
        }

        int[] bitsPerSample = new int[1];
        bitsPerSample[0] = bits;

        sampleModel =
            new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                                            width, height,
                                            1, width,
                                            new int[] {0});

        if (!transparentColorFlag) {
            if (ImageCodec.isIndicesForGrayscale(r, g, b))
                colorModel = ImageCodec.createComponentColorModel(sampleModel);
            else
                colorModel = new IndexColorModel(bits, r.length, r, g, b);
        } else {
            colorModel =
		new IndexColorModel(bits, r.length, r, g, b, transparentColorIndex);
        }
    }

    // BEGIN LZW CODE

    private void initNext32Bits() {
        next32Bits = block[0] & 0xff;
        next32Bits |= (block[1] & 0xff) << 8;
        next32Bits |= (block[2] & 0xff) << 16;
        next32Bits |= block[3] << 24;
        nextByte = 4;
    }

    // Load a block (1-255 bytes) at a time, and maintain
    // a 32-bit lookahead buffer that is filled from the left
    // and extracted from the right.
    private int getCode(int codeSize, int codeMask) throws IOException {
        //if (bitPos + codeSize > 32) {
        if (bitsLeft <= 0) {
            return eofCode; // No more data available
        }

        int code = (next32Bits >> bitPos) & codeMask;
        bitPos += codeSize;
	bitsLeft -= codeSize;

        // Shift in a byte of new data at a time
        while (bitPos >= 8 && !lastBlockFound) {
            next32Bits >>>= 8;
            bitPos -= 8;

            // Check if current block is out of bytes
            if (nextByte >= blockLength) {
                // Get next block size
                blockLength = input.readUnsignedByte();
		if (blockLength == 0) {
                    lastBlockFound = true;
		    if (bitsLeft < 0)
			return eofCode;
		    else
			return code;
                } else {
                    int left = blockLength;
                    int off = 0;
                    while (left > 0) {
                        int nbytes = input.read(block, off, left);
                        off += nbytes;
                        left -= nbytes;
                    }

		    bitsLeft += blockLength << 3;
		    nextByte = 0;
                }
            }

            next32Bits |= block[nextByte++] << 24;
        }

        return code;
    }

    private void initializeStringTable(int[] prefix,
                                       byte[] suffix,
                                       byte[] initial,
                                       int[] length) {
        int numEntries = 1 << initCodeSize;
  	for (int i = 0; i < numEntries; i++) {
            prefix[i] = -1;
            suffix[i] = (byte)i;
            initial[i] = (byte)i;
            length[i] = 1;
        }

        // Fill in the entire table for robustness against
        // out-of-sequence codes.
  	for (int i = numEntries; i < 4096; i++) {
            prefix[i] = -1;
            length[i] = 1;
        }
    }

    private Point outputPixels(byte[] string,
                               int len,
                               Point streamPos,
                               byte[] rowBuf) {
        if (interlacePass < 0 || interlacePass > 3) {
            return streamPos;
        }

        for (int i = 0; i < len; i++) {
            if (streamPos.x >= minX) {
                rowBuf[streamPos.x - minX] = string[i];
            }

            // Process end-of-row
            ++streamPos.x;
            if (streamPos.x == width) {
		theTile.setDataElements(minX, streamPos.y, width, 1, rowBuf);

                streamPos.x = 0;
                if (interlaceFlag) {
                    streamPos.y += INTERLACE_INCREMENT[interlacePass];
                    if (streamPos.y >= height) {
                        ++interlacePass;
                        if (interlacePass > 3) {
                            return streamPos;
                        }
                        streamPos.y = INTERLACE_OFFSET[interlacePass];
                    }
                } else {
                    ++streamPos.y;
                }
            }
        }

        return streamPos;
    }

    // END LZW CODE

    public synchronized Raster getTile(int tileX, int tileY) {

        // Should be a unique tile.
        if (tileX != 0 || tileY != 0) {
            throw new IllegalArgumentException(JaiI18N.getString("GIFImage2"));
        }

        // Return the tile if it's already computed.
        if (theTile != null) {
            return theTile;
        }

        // Initialize the destination image
        theTile =
            WritableRaster.createWritableRaster(sampleModel,
                                                sampleModel.createDataBuffer(),
                                                null);

        // Position in stream coordinates.
        Point streamPos = new Point(0, 0);

        // Allocate a row of memory.
        byte[] rowBuf = new byte[width];

        try {
            // Read and decode the image data, fill in theTile.
            this.initCodeSize = input.readUnsignedByte();

            // Read first data block
            this.blockLength = input.readUnsignedByte();
	    int left = blockLength;
            int off = 0;
            while (left > 0) {
                int nbytes = input.read(block, off, left);
                left -= nbytes;
                off += nbytes;
            }

            this.bitPos = 0;
            this.nextByte = 0;
            this.lastBlockFound = false;
	    this.bitsLeft = this.blockLength << 3;

            // Init 32-bit buffer
            initNext32Bits();

            this.clearCode = 1 << initCodeSize;
            this.eofCode = clearCode + 1;

            int code, oldCode = 0;

            int[] prefix = new int[4096];
            byte[] suffix = new byte[4096];
            byte[] initial = new byte[4096];
            int[] length = new int[4096];
            byte[] string = new byte[4096];

            initializeStringTable(prefix, suffix, initial, length);
            int tableIndex = (1 << initCodeSize) + 2;
            int codeSize = initCodeSize + 1;
            int codeMask = (1 << codeSize) - 1;

            while (true) {
                code = getCode(codeSize, codeMask);

                if (code == clearCode) {
                    initializeStringTable(prefix, suffix, initial, length);
                    tableIndex = (1 << initCodeSize) + 2;
                    codeSize = initCodeSize + 1;
                    codeMask = (1 << codeSize) - 1;
                    code = getCode(codeSize, codeMask);
                    if (code == eofCode) {
                        return theTile;
                    }
                } else if (code == eofCode) {
                    return theTile;
                } else {
                    int newSuffixIndex;
                    if (code < tableIndex) {
                        newSuffixIndex = code;
                    } else { // code == tableIndex
                        newSuffixIndex = oldCode;
                    }

                    int ti = tableIndex;
                    int oc = oldCode;

                    prefix[ti] = oc;
                    suffix[ti] = initial[newSuffixIndex];
                    initial[ti] = initial[oc];
                    length[ti] = length[oc] + 1;

                    ++tableIndex;
                    if ((tableIndex == (1 << codeSize)) &&
                        (tableIndex < 4096)) {
                        ++codeSize;
                        codeMask = (1 << codeSize) - 1;
                    }
                }

                // Reverse code
                int c = code;
                int len = length[c];
                for (int i = len - 1; i >= 0; i--) {
                    string[i] = suffix[c];
                    c = prefix[c];
                }

                outputPixels(string, len, streamPos, rowBuf);
                oldCode = code;
            }
        } catch (IOException e) {
            String message = JaiI18N.getString("GIFImage3");
            ImagingListenerProxy.errorOccurred(message,
                                   new ImagingException(message, e),
                                   this, false);
//            throw new RuntimeException(JaiI18N.getString("GIFImage3"));
        } finally {
            return theTile;
        }
    }

    public void dispose() {
        theTile = null;
    }
}
