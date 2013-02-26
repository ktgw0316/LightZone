/*
 * $RCSfile: PNMImageEncoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:38 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.OutputStream;
import com.lightcrafts.media.jai.codec.ImageEncoderImpl;
import com.lightcrafts.media.jai.codec.ImageEncodeParam;
import com.lightcrafts.media.jai.codec.PNMEncodeParam;

/**
 * An ImageEncoder for the PNM family of file formats.
 *
 * <p> The PNM file format includes PBM for monochrome images, PGM for
 * grey scale images, and PPM for color images. When writing the
 * source data out, the encoder chooses the appropriate file variant
 * based on the actual SampleModel of the source image. In case the
 * source image data is unsuitable for the PNM file format, for
 * example when source has 4 bands or float data type, the encoder
 * throws an Error.
 *
 * <p> The raw file format is used wherever possible, unless the
 * PNMEncodeParam object supplied to the constructor returns
 * <code>true</code> from its <code>getRaw()</code> method.
 *
 *
 * @since EA2
 */
public class PNMImageEncoder extends ImageEncoderImpl {

    private static final int PBM_ASCII  = '1';
    private static final int PGM_ASCII  = '2';
    private static final int PPM_ASCII  = '3';
    private static final int PBM_RAW    = '4';
    private static final int PGM_RAW    = '5';
    private static final int PPM_RAW    = '6';

    private static final int SPACE      = ' ';

    private static final String COMMENT = 
        "# written by com.lightcrafts.media.jai.codecimpl.PNMImageEncoder";

    private byte[] lineSeparator;

    private int variant;
    private int maxValue;

    public PNMImageEncoder(OutputStream output,
                           ImageEncodeParam param) {
        super(output, param);
        if (this.param == null) {
            this.param = new PNMEncodeParam();
        }
    }

    /**
     * Encodes a RenderedImage and writes the output to the
     * OutputStream associated with this ImageEncoder.
     */
    public void encode(RenderedImage im) throws IOException {
        int minX = im.getMinX();
        int minY = im.getMinY();
        int width = im.getWidth();
        int height = im.getHeight();
        int tileHeight = im.getTileHeight();
        SampleModel sampleModel = im.getSampleModel();
        ColorModel colorModel = im.getColorModel();

        String ls = (String)java.security.AccessController.doPrivileged(
               new sun.security.action.GetPropertyAction("line.separator"));
        lineSeparator = ls.getBytes();

        int dataType = sampleModel.getTransferType();
        if ((dataType == DataBuffer.TYPE_FLOAT) ||
            (dataType == DataBuffer.TYPE_DOUBLE)) {
            throw new RuntimeException(JaiI18N.getString("PNMImageEncoder0"));
        }

        // Raw data can only handle bytes, everything greater must be ASCII.
        int[] sampleSize = sampleModel.getSampleSize();
        int numBands = sampleModel.getNumBands();

        // Colormap populated for non-bilevel IndexColorModel only.
        byte[] reds = null;
        byte[] greens = null;
        byte[] blues = null;

        // Flag indicating that PB data should be inverted before writing.
        boolean isPBMInverted = false;

        if (numBands == 1) {
            if (colorModel instanceof IndexColorModel) {
                IndexColorModel icm = (IndexColorModel)colorModel;

                int mapSize = icm.getMapSize();
                if (mapSize < (1 << sampleSize[0])) {
                    throw new RuntimeException(
                        JaiI18N.getString("PNMImageEncoder1"));
                }

                if(sampleSize[0] == 1) {
                    variant = PBM_RAW;

                    // Set PBM inversion flag if 1 maps to a higher color
                    // value than 0: PBM expects white-is-zero so if this
                    // does not obtain then inversion needs to occur.
                    isPBMInverted =
                        (icm.getRed(1) + icm.getGreen(1) + icm.getBlue(1)) >
                        (icm.getRed(0) + icm.getGreen(0) + icm.getBlue(0));
                } else {
                    variant = PPM_RAW;

                    reds = new byte[mapSize];
                    greens = new byte[mapSize];
                    blues = new byte[mapSize];

                    icm.getReds(reds);
                    icm.getGreens(greens);
                    icm.getBlues(blues);
                }
            } else if (sampleSize[0] == 1) {
                variant = PBM_RAW;
            } else if (sampleSize[0] <= 8) {
                variant = PGM_RAW;
            } else {
                variant = PGM_ASCII;
            }
        } else if (numBands == 3) {
            if (sampleSize[0] <= 8 && sampleSize[1] <= 8 &&
                sampleSize[2] <= 8) {	// all 3 bands must be <= 8
                variant = PPM_RAW;
            } else {
                variant = PPM_ASCII;
            }
        } else {
            throw new RuntimeException(JaiI18N.getString("PNMImageEncoder2"));
        }

        // Read parameters
        if (((PNMEncodeParam)param).getRaw()) {
            if (!isRaw(variant)) {
                boolean canUseRaw = true;

                // Make sure sampleSize for all bands no greater than 8.
                for (int i = 0; i < sampleSize.length; i++) {
                    if (sampleSize[i] > 8) {
                        canUseRaw = false;
                        break;
                    }
                }

                if (canUseRaw) {
                    variant += 0x3;
                }
            }
        } else {
            if (isRaw(variant)) {
                variant -= 0x3;
            }
        }

        maxValue = (1 << sampleSize[0]) - 1;

        // Write PNM file.
        output.write('P');			// magic value
        output.write(variant);
        
        output.write(lineSeparator);
        output.write(COMMENT.getBytes());	// comment line
        
        output.write(lineSeparator);
        writeInteger(output, width);		// width
        output.write(SPACE);
        writeInteger(output, height);		// height
        
        // Writ esample max value for non-binary images
        if ((variant != PBM_RAW) && (variant != PBM_ASCII)) {
            output.write(lineSeparator);
            writeInteger(output, maxValue);
        }
        
        // The spec allows a single character between the
        // last header value and the start of the raw data.
        if (variant == PBM_RAW ||
            variant == PGM_RAW ||
            variant == PPM_RAW) {
            output.write('\n');
        }

        // Set flag for optimal image writing case: row-packed data with
        // correct band order if applicable.
        boolean writeOptimal = false;
        if (variant == PBM_RAW &&
            sampleModel.getTransferType() == DataBuffer.TYPE_BYTE &&
            sampleModel instanceof MultiPixelPackedSampleModel) {

            MultiPixelPackedSampleModel mppsm =
                (MultiPixelPackedSampleModel)sampleModel;

            // Must have left-aligned bytes with unity bit stride.
            if(mppsm.getDataBitOffset() == 0 &&
               mppsm.getPixelBitStride() == 1) {

                writeOptimal = true;
            }
        } else if ((variant == PGM_RAW || variant == PPM_RAW) &&
                   sampleModel instanceof ComponentSampleModel &&
                   !(colorModel instanceof IndexColorModel)) {

            ComponentSampleModel csm =
                (ComponentSampleModel)sampleModel;

            // Pixel stride must equal band count.
            if(csm.getPixelStride() == numBands) {
                writeOptimal = true;

                // Band offsets must equal band indices.
                if(variant == PPM_RAW) {
                    int[] bandOffsets = csm.getBandOffsets();
                    for(int b = 0; b < numBands; b++) {
                        if(bandOffsets[b] != b) {
                            writeOptimal = false;
                            break;
                        }
                    }
                }
            }
        }

        // Write using an optimal approach if possible.
        if(writeOptimal) {
            int bytesPerRow = variant == PBM_RAW ?
                (width + 7)/8 : width*sampleModel.getNumBands();
            int numYTiles = im.getNumYTiles();
	    Rectangle imageBounds = 
		new Rectangle(im.getMinX(), im.getMinY(),
			      im.getWidth(), im.getHeight());
	    Rectangle stripRect =
		new Rectangle(im.getMinX(), 
			      im.getMinTileY() * im.getTileHeight() + im.getTileGridYOffset(),
			      im.getWidth(), im.getTileHeight());

            byte[] invertedData = null;
            if(isPBMInverted) {
                invertedData = new byte[bytesPerRow];
            }

            // Loop over tiles to minimize cobbling.
            for(int j = 0; j < numYTiles; j++) {
                // Clamp the strip to the image bounds.
                if(j == numYTiles - 1) {
                    stripRect.height = im.getHeight() - stripRect.y;
                }

		Rectangle encodedRect = stripRect.intersection(imageBounds);
                // Get a strip of data.
                Raster strip = im.getData(encodedRect);

                // Get the data array.
                byte[] bdata =
                    ((DataBufferByte)strip.getDataBuffer()).getData();

                // Get the scanline stride.
                int rowStride = variant == PBM_RAW ?
                    ((MultiPixelPackedSampleModel)strip.getSampleModel()).getScanlineStride() :
                    ((ComponentSampleModel)strip.getSampleModel()).getScanlineStride();

                if(rowStride == bytesPerRow && !isPBMInverted) {
                    // Write the entire strip at once.
                    output.write(bdata, 0, bdata.length);
                } else {
                    // Write the strip row-by-row.
                    int offset = 0;
                    for(int i = 0; i < encodedRect.height; i++) {
                        if(isPBMInverted) {
                            for(int k = 0; k < bytesPerRow; k++) {
                                invertedData[k] =
                                    (byte)(~(bdata[offset+k]&0xff));
                            }
                            output.write(invertedData, 0, bytesPerRow);
                        } else {
                            output.write(bdata, offset, bytesPerRow);
                        }
                        offset += rowStride;
                    }
                }

                // Increment the strip origin.
                stripRect.y += tileHeight;
            }

            // Write all buffered bytes and return.
            output.flush();

            return;
        }

        // Buffer for up to 8 rows of pixels
        int[] pixels = new int[8*width*numBands];

        // Also allocate a buffer to hold the data to be written to the file,
        // so we can use array writes.
        byte[] bpixels = reds == null ?
                         new byte[8*width*numBands] : new byte[8*width*3];

        // The index of the sample being written, used to
        // place a line separator after every 16th sample in
        // ASCII mode.  Not used in raw mode.
        int count = 0;

        // Process 8 rows at a time so all but the last will have
        // a multiple of 8 pixels.  This simplifies PBM_RAW encoding.
        int lastRow = minY + height;
        for (int row = minY; row < lastRow; row += 8) {
            int rows = Math.min(8, lastRow - row);
            int size = rows*width*numBands;
            
            // Grab the pixels
            Raster src = im.getData(new Rectangle(minX, row, width, rows));
            src.getPixels(minX, row, width, rows, pixels);

            // Invert bits if necessary.
            if(isPBMInverted) {
                for(int k = 0; k < size; k++) {
                    pixels[k] ^= 0x00000001;
                }
            }
        
            switch (variant) {
            case PBM_ASCII:
            case PGM_ASCII:
                for (int i = 0; i < size; i++) {
                    if ((count++ % 16) == 0) {
                        output.write(lineSeparator);
                    } else {
                        output.write(SPACE);
                    }
                    writeInteger(output, pixels[i]);
                }
                output.write(lineSeparator);
                break;

            case PPM_ASCII:
                if (reds == null) {	// no need to expand
                    for (int i = 0; i < size; i++) {
                        if ((count++ % 16) == 0) {
                            output.write(lineSeparator);
                        } else {
                            output.write(SPACE);
                        }
                        writeInteger(output, pixels[i]);
                    }

                } else {
                    for (int i = 0; i < size; i++) {
                        if ((count++ % 16) == 0) {
                            output.write(lineSeparator);
                        } else {
                            output.write(SPACE);
                        }
                        writeInteger(output, (reds[pixels[i]] & 0xFF));
                        output.write(SPACE);
                        writeInteger(output, (greens[pixels[i]] & 0xFF));
                        output.write(SPACE);
                        writeInteger(output, (blues[pixels[i]] & 0xFF));
                    }
                }
                output.write(lineSeparator);
                break;
            
            case PBM_RAW:
                // 8 pixels packed into 1 byte, the leftovers are padded.
                int kdst = 0;
                int ksrc = 0;
                for (int i = 0; i < size/8; i++) {
                    int b = (pixels[ksrc++] << 7) |
                            (pixels[ksrc++] << 6) |
                            (pixels[ksrc++] << 5) |
                            (pixels[ksrc++] << 4) |
                            (pixels[ksrc++] << 3) |
                            (pixels[ksrc++] << 2) |
                            (pixels[ksrc++] << 1) |
                             pixels[ksrc++];
                    bpixels[kdst++] = (byte)b;
                }
            
                // Leftover pixels, only possible at the end of the file.
                if (size%8 > 0) {
                    int b = 0;
                    for (int i=0; i<size%8; i++) {
                        b |= pixels[size + i] << (7 - i);
                    }
                    bpixels[kdst++] = (byte)b;
                }
                output.write(bpixels, 0, (size+7)/8);

                break;
                
            case PGM_RAW:
                for(int i=0; i<size; i++) {
                    bpixels[i] = (byte)(pixels[i]);
                }
                output.write(bpixels, 0, size);
                break;

            case PPM_RAW:
                if (reds == null) {	// no need to expand
                    for (int i=0; i<size; i++) {
                        bpixels[i] = (byte)(pixels[i] & 0xFF);
                    }
                } else {
                    for (int i=0, j=0; i<size; i++) {
                        bpixels[j++] = reds[pixels[i]];
                        bpixels[j++] = greens[pixels[i]];
                        bpixels[j++] = blues[pixels[i]];
                    }
                }
                output.write(bpixels, 0, bpixels.length);
                break;
            }
        }
        
        // Force all buffered bytes to be written out.
        output.flush();
    }

    /** Writes an integer to the output in ASCII format. */
    private void writeInteger(OutputStream output, int i) throws IOException {
        output.write(Integer.toString(i).getBytes());
    }

    /** Writes a byte to the output in ASCII format. */
    private void writeByte(OutputStream output, byte b) throws IOException {
        output.write(Byte.toString(b).getBytes());
    }

    /** Returns true if file variant is raw format, false if ASCII. */
    private boolean isRaw(int v) {
        return (v >= PBM_RAW);
    }
}
