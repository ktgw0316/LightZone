/*
 * $RCSfile: BMPImageEncoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:35 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl;
import java.io.OutputStream;
import java.io.IOException;
import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.Rectangle;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import com.lightcrafts.media.jai.codec.ImageEncoderImpl;
import com.lightcrafts.media.jai.codec.ImageEncodeParam;
import com.lightcrafts.media.jai.codec.BMPEncodeParam;
import com.lightcrafts.media.jai.codec.SeekableOutputStream;
/**
 * An ImageEncoder for the various versions of the BMP image file format.
 *
 * Unless specified otherwise by the BMPDecodeParam object passed to the
 * constructor, Version 3 will be the default version used.
 * 
 * <p>If the image to be encoded has an IndexColorModel and can be encoded
 * using upto 8 bits per pixel, the image will be written out as a Palette
 * color image with an appropriate number of bits per pixel. For example an
 * image having a 256 color IndexColorModel will be written out as a Palette
 * image with 8 bits per pixel while one with a 16 color palette will be 
 * written out as a Palette image with 4 bits per pixel. For all other images,
 * the 24 bit image format will be used.
 *
 *
 * @since EA2
 */
public class BMPImageEncoder extends ImageEncoderImpl {

    private OutputStream output;
    private int version;
    private boolean isCompressed, isTopDown;
    private int w, h;
    private int compImageSize = 0;

    /**
     * An ImageEncoder for the BMP file format.
     *
     * @param output       The OutputStream to write to.
     * @param param        The BMPEncodeParam object.
     */
    public BMPImageEncoder(OutputStream output, ImageEncodeParam param) {

	super(output, param);
     
	this.output = output;

	BMPEncodeParam bmpParam;
	if (param == null) {
	    // Use default valued BMPEncodeParam
	    bmpParam = new BMPEncodeParam();
	} else {
	    bmpParam = (BMPEncodeParam)param;
	}

	this.version = bmpParam.getVersion();
	this.isCompressed = bmpParam.isCompressed();
	if(isCompressed && !(output instanceof SeekableOutputStream)){
	    throw new 
		IllegalArgumentException(JaiI18N.getString("BMPImageEncoder6"));
	}  
	
	this.isTopDown = bmpParam.isTopDown();
    }

    /**
     * Encodes a RenderedImage and writes the output to the
     * OutputStream associated with this ImageEncoder.
     */
    public void encode(RenderedImage im) throws IOException {

	// Get image dimensions
	int minX = im.getMinX();
        int minY = im.getMinY();
	w = im.getWidth();
	h = im.getHeight();

	// Default is using 24 bits per pixel.
	int bitsPerPixel = 24;
	boolean isPalette = false;
	int paletteEntries = 0;
	IndexColorModel icm = null;
 
	SampleModel sm = im.getSampleModel();
	int numBands = sm.getNumBands();

	ColorModel cm = im.getColorModel();

	if (numBands != 1 && numBands != 3) {
	    throw new 
		IllegalArgumentException(JaiI18N.getString("BMPImageEncoder1"));
	}
	
	int sampleSize[] = sm.getSampleSize();
	if (sampleSize[0] > 8) {
	    throw new RuntimeException(JaiI18N.getString("BMPImageEncoder2"));
	}

	for (int i=1; i<sampleSize.length; i++) {
	    if (sampleSize[i] != sampleSize[0]) {
		throw 
		    new RuntimeException(JaiI18N.getString("BMPImageEncoder3"));
	    }
	}

	// Float and Double data cannot be written in a BMP format.
	int dataType = sm.getTransferType();
        if (dataType != DataBuffer.TYPE_BYTE &&
            !CodecUtils.isPackedByteImage(im)) {
            throw new RuntimeException(JaiI18N.getString("BMPImageEncoder0"));
        }

	// Number of bytes that a scanline for the image written out will have.
	int destScanlineBytes = w * numBands;
	int compression = 0;


	byte r[] = null, g[] = null, b[] = null, a[] = null;

	if (cm instanceof IndexColorModel) {

	    isPalette = true;
	    icm = (IndexColorModel)cm;
	    paletteEntries = icm.getMapSize();

	    if (paletteEntries <= 2) {

		bitsPerPixel = 1;
		destScanlineBytes = (int)Math.ceil((double)w/8.0);

	    } else if (paletteEntries <= 16) {

		bitsPerPixel = 4;
		destScanlineBytes = (int)Math.ceil((double)w/2.0);

	    } else if (paletteEntries <= 256) {

		bitsPerPixel = 8;

	    } else {

		// Cannot be written as a Palette image. So write out as 
		// 24 bit image.
		bitsPerPixel = 24;
		isPalette = false;
		paletteEntries = 0;
		destScanlineBytes = w * 3;
	    }

	    if (isPalette == true) {

		r = new byte[paletteEntries];
		g = new byte[paletteEntries];
		b = new byte[paletteEntries];
		a = new byte[paletteEntries];
		
		icm.getAlphas(a);
		icm.getReds(r);
		icm.getGreens(g);
		icm.getBlues(b);
	    }

	} else {
	    
	    // Grey scale images
	    if (numBands == 1) {
		
		isPalette = true;
		paletteEntries = 256;
		//		int sampleSize[] = sm.getSampleSize();
		bitsPerPixel = sampleSize[0];
		
		destScanlineBytes = (int)Math.ceil((double)(w * bitsPerPixel) /
						   8.0);

		r = new byte[256];
		g = new byte[256];
		b = new byte[256];
		a = new byte[256];
		
		for (int i = 0; i < 256; i++) {
		    r[i] = (byte)i;
		    g[i] = (byte)i;
		    b[i] = (byte)i;
		    //Fix 4672486: BMPEncoder writes wrong alpha lut into 
		    // stream for gray-scale image
		    a[i] = (byte)255;
		}
	    } else if (sm instanceof SinglePixelPackedSampleModel) {
		bitsPerPixel = DataBuffer.getDataTypeSize(sm.getDataType());
		destScanlineBytes = w * bitsPerPixel + 7 >> 3;
	    }
	}

	// actual writing of image data
	int fileSize = 0;
	int offset = 0;
	int headerSize = 0;
	int imageSize = 0;
	int xPelsPerMeter = 0;
	int yPelsPerMeter = 0;
	int colorsUsed = 0;
	int colorsImportant = paletteEntries;
	int padding = 0;

	// Calculate padding for each scanline
	int remainder = destScanlineBytes % 4;
	if (remainder != 0) {
	    padding = 4 - remainder;
	}
	
	switch (version) {	    
	case BMPEncodeParam.VERSION_2:
	    offset = 26 + paletteEntries * 3;
	    headerSize = 12;
	    imageSize = (destScanlineBytes + padding) * h;
	    fileSize = imageSize + offset;
	    throw new 
		RuntimeException(JaiI18N.getString("BMPImageEncoder5"));
	    //break;
	    
	case BMPEncodeParam.VERSION_3:
	    // FileHeader is 14 bytes, BitmapHeader is 40 bytes, 
	    // add palette size and that is where the data will begin
	    if (isCompressed &&	bitsPerPixel == 8) {
		compression = 1;
	    } else if (isCompressed && bitsPerPixel == 4) {
		compression = 2;
	    }
	    offset = 54 + paletteEntries * 4;

	    imageSize = (destScanlineBytes + padding) * h;
	    fileSize = imageSize + offset;
	    headerSize = 40;
	    break;
		
	case BMPEncodeParam.VERSION_4:
	    headerSize = 108;
	    throw new
		RuntimeException(JaiI18N.getString("BMPImageEncoder5"));
	    // break;
	}

	int redMask = 0, blueMask = 0, greenMask = 0;
	if (cm instanceof DirectColorModel) {
	    redMask = ((DirectColorModel)cm).getRedMask();
	    greenMask = ((DirectColorModel)cm).getGreenMask();
	    blueMask = ((DirectColorModel)cm).getBlueMask();
	    destScanlineBytes = w;
	    compression = 3;
	    fileSize += 12;
	    offset += 12;
	}

	writeFileHeader(fileSize, offset);
	
	writeInfoHeader(headerSize, bitsPerPixel);

	// compression
	writeDWord(compression);
	
	// imageSize
	writeDWord(imageSize);

	// xPelsPerMeter
	writeDWord(xPelsPerMeter);
	
	// yPelsPerMeter
	writeDWord(yPelsPerMeter);
	
	// Colors Used
	writeDWord(colorsUsed);
	
	// Colors Important
	writeDWord(colorsImportant);

	if (compression == 3) {
	    writeDWord(redMask);
	    writeDWord(greenMask);
	    writeDWord(blueMask);
	}

	if (compression == 3) {
            for (int i = 0; i < h; i++) {
                int row = minY + i;

                if (!isTopDown)
                    row = minY + h - i -1;

                // Get the pixels
                Rectangle srcRect =
                    new Rectangle(minX, row, w, 1);
                Raster src = im.getData(srcRect);

                SampleModel sm1 = src.getSampleModel();
                int pos = 0;
                int startX = srcRect.x - src.getSampleModelTranslateX();
                int startY = srcRect.y - src.getSampleModelTranslateY();
                if (sm1 instanceof SinglePixelPackedSampleModel) {
                    SinglePixelPackedSampleModel sppsm =
                        (SinglePixelPackedSampleModel)sm1;
                    pos = sppsm.getOffset(startX, startY);
                }

                switch(dataType) {
                    case DataBuffer.TYPE_SHORT:
                    short[] sdata =
                        ((DataBufferShort)src.getDataBuffer()).getData();
		    for (int m = 0; m < sdata.length; m++)
			writeWord(sdata[m]);
                    break;

                    case DataBuffer.TYPE_USHORT:
                    short[] usdata =
                        ((DataBufferUShort)src.getDataBuffer()).getData();
		    for (int m = 0; m < usdata.length; m++)
			writeWord(usdata[m]);
                    break;

                    case DataBuffer.TYPE_INT:
                    int[] idata =
                        ((DataBufferInt)src.getDataBuffer()).getData();
		    for (int m = 0; m < idata.length; m++)
			writeDWord(idata[m]);
                    break;
                }
	    }
	    return;
	}

	// palette
	if (isPalette == true) {

	    // write palette
	    switch(version) {

		// has 3 field entries
	    case BMPEncodeParam.VERSION_2:

		for (int i=0; i<paletteEntries; i++) {
		    output.write(b[i]);
		    output.write(g[i]);
		    output.write(r[i]);
		}
		break;

		// has 4 field entries
	    default:

		for (int i=0; i<paletteEntries; i++) {
		    output.write(b[i]);
		    output.write(g[i]);
		    output.write(r[i]);
		    output.write(a[i]);
		}
		break;
	    }
	    
	} // else no palette
	
	// Writing of actual image data

	int scanlineBytes = w * numBands;

	// Buffer for up to 8 rows of pixels
	int[] pixels = new int[8 * scanlineBytes];

        // Also create a buffer to hold one line of the data
        // to be written to the file, so we can use array writes.
        byte[] bpixels = new byte[destScanlineBytes];

	int l;

	if (!isTopDown) {
	    // Process 8 rows at a time so all but the first will have a
	    // multiple of 8 rows.
	    int lastRow = minY + h;

	    for (int row = (lastRow-1); row >= minY; row -= 8) {
		// Number of rows being read
		int rows = Math.min(8, row - minY + 1);
	
		// Get the pixels
		Raster src = im.getData(new Rectangle(minX, row - rows + 1, 
						      w, rows));

		src.getPixels(minX, row - rows + 1, w, rows, pixels);
		
		l = 0;		
		
		// Last possible position in the pixels array
		int max = scanlineBytes * rows - 1;

		for (int i=0; i<rows; i++) {

		    // Beginning of each scanline in the pixels array    
		    l = max - (i+1) * scanlineBytes + 1;

		    writePixels(l, scanlineBytes, bitsPerPixel, pixels,
				bpixels, padding, numBands, icm);
		}
	

	    }
	    
	} else {
	    // Process 8 rows at a time so all but the last will have a
	    // multiple of 8 rows.
	    int lastRow = minY + h;

	    for (int row = minY; row < lastRow; row += 8) {
		int rows = Math.min(8, lastRow - row);
	
		// Get the pixels
		Raster src = im.getData(new Rectangle(minX, row, 
						      w, rows));
		src.getPixels(minX, row, w, rows, pixels);

		l=0;		
		for (int i=0; i<rows; i++) {
		    
		    writePixels(l, scanlineBytes, bitsPerPixel, pixels, 
				bpixels, padding, numBands, icm);
		}
	
	    }

	}
	
	if(isCompressed &&( bitsPerPixel == 4 || bitsPerPixel == 8)){
	    // Write the RLE EOF marker and 
	    output.write(0);
	    output.write(1);
	    incCompImageSize(2);
	    // update the file/image Size
	    imageSize = compImageSize;
	    fileSize = compImageSize + offset;
	    writeSize(fileSize, 2);
	    writeSize(imageSize, 34);
	}

	
    }
        
    private void writePixels(int l, int scanlineBytes, int bitsPerPixel, 
			     int pixels[], byte bpixels[], 
                             int padding, int numBands,
			     IndexColorModel icm) throws IOException {
	
	int pixel = 0;
        int k = 0;
	switch (bitsPerPixel) {

	case 1:

	    for (int j=0; j<scanlineBytes/8; j++) {
		bpixels[k++] = (byte)((pixels[l++]  << 7) |
                                      (pixels[l++]  << 6) |
                                      (pixels[l++]  << 5) |
                                      (pixels[l++]  << 4) |
                                      (pixels[l++]  << 3) |
                                      (pixels[l++]  << 2) |
                                      (pixels[l++]  << 1) |
                                       pixels[l++]);
	    }
	    
            // Partially filled last byte, if any
            if (scanlineBytes%8 > 0) {
                pixel = 0;
                for (int j=0; j<scanlineBytes%8; j++) {
                    pixel |= (pixels[l++] << (7 - j));
                }
                bpixels[k++] = (byte)pixel;
            }
            output.write(bpixels, 0, (scanlineBytes+7)/8); 

	    break;
	    
	case 4:
	    if (isCompressed){
		byte[] bipixels = new byte[scanlineBytes];
		for (int h=0; h<scanlineBytes; h++) {
		    bipixels[h] = (byte)pixels[l++];
		} 
		encodeRLE4(bipixels, scanlineBytes);
	    }else {
		for (int j=0; j<scanlineBytes/2; j++) {
		    pixel = (pixels[l++] << 4) | pixels[l++];
		    bpixels[k++] = (byte)pixel;	
		}
		// Put the last pixel of odd-length lines in the 4 MSBs
		if ((scanlineBytes%2) == 1) {
		    pixel = pixels[l] << 4;
		    bpixels[k++] = (byte)pixel;
		}
		output.write(bpixels, 0, (scanlineBytes+1)/2);
	    }
		break;
	    
	case 8:
	    if(isCompressed) {
		for (int h=0; h<scanlineBytes; h++) {
		    bpixels[h] = (byte)pixels[l++];
		} 
		encodeRLE8(bpixels, scanlineBytes);
	    }else {
		for (int j=0; j<scanlineBytes; j++) {
		    bpixels[j] = (byte)pixels[l++];
		}
		output.write(bpixels, 0, scanlineBytes); 
	    }	
	    break;
	    
	case 24:
	    if (numBands == 3) {
		for (int j=0; j<scanlineBytes; j+=3) {
		    // Since BMP needs BGR format
                    bpixels[k++] = (byte)(pixels[l+2]);
                    bpixels[k++] = (byte)(pixels[l+1]);
                    bpixels[k++] = (byte)(pixels[l]);
		    l+=3;
		}
                output.write(bpixels, 0, scanlineBytes);
	    } else {
		// Case where IndexColorModel had > 256 colors.  
		int entries = icm.getMapSize();

		byte r[] = new byte[entries];
		byte g[] = new byte[entries];
		byte b[] = new byte[entries];
		
		icm.getReds(r);
		icm.getGreens(g);
		icm.getBlues(b);
		int index;
		
		for (int j=0; j<scanlineBytes; j++) {
		    index = pixels[l];
                    bpixels[k++] = b[index];
                    bpixels[k++] = g[index];
                    bpixels[k++] = b[index];
		    l++;
		}
                output.write(bpixels, 0, scanlineBytes*3);
	    }
	    break;
	    
	}

	// Write out the padding
	if (!(isCompressed && (bitsPerPixel == 8 || bitsPerPixel == 4))){
	    for(k=0; k<padding; k++) {
		output.write(0);
	    }
	}
    }    
    
    private void encodeRLE8(byte[] bpixels, int scanlineBytes)
	throws IOException{
	
	int runCount = 1, absVal = -1, j = -1;
	byte runVal = 0, nextVal =0 ;
	
	runVal = bpixels[++j];
	byte[] absBuf = new byte[256];
	
	while (j < scanlineBytes-1) {
	    nextVal = bpixels[++j];
	    if (nextVal == runVal ){
		if(absVal >= 3 ){ 
		    /// Check if there was an existing Absolute Run
		    output.write(0);
		    output.write(absVal);
		    incCompImageSize(2);
		    for(int a=0; a<absVal;a++){
			output.write(absBuf[a]);
			incCompImageSize(1);
		    }
		    if (!isEven(absVal)){
			//Padding
			output.write(0);
			incCompImageSize(1);
		    }
		}
		else if(absVal > -1){
		    /// Absolute Encoding for less than 3
		    /// treated as regular encoding 
		    /// Do not include the last element since it will
		    /// be inclued in the next encoding/run
		    for (int b=0;b<absVal;b++){
			output.write(1);
			output.write(absBuf[b]);
			incCompImageSize(2);
		    }
		}
		absVal = -1;
		runCount++;
		if (runCount == 256){
		    /// Only 255 values permitted
		    output.write(runCount-1);
		    output.write(runVal);
		    incCompImageSize(2);
		    runCount = 1;
		}
	    }
	    else {
		if (runCount > 1){
		    /// If there was an existing run 
		    output.write(runCount);
		    output.write(runVal);
		    incCompImageSize(2);
		} else if (absVal < 0){
		    // First time.. 
		    absBuf[++absVal] = runVal;
		    absBuf[++absVal] = nextVal;
		} else if (absVal < 254){
		    //  0-254 only
		    absBuf[++absVal] = nextVal;
		} else {
		    output.write(0);
		    output.write(absVal+1);
		    incCompImageSize(2);
		    for(int a=0; a<=absVal;a++){
			output.write(absBuf[a]);
			incCompImageSize(1);
		    } 
		    // padding since 255 elts is not even
		    output.write(0);
		    incCompImageSize(1);
		    absVal = -1;
		}
		runVal = nextVal;
		runCount = 1;
	    }
	    
	    if (j == scanlineBytes-1){ // EOF scanline
		// Write the run
		if (absVal == -1){
		    output.write(runCount);
		    output.write(runVal);
		    incCompImageSize(2);
		    runCount = 1;
		}
		else {
		    // write the Absolute Run
		    if(absVal >= 2){
			output.write(0);
			output.write(absVal+1);
			incCompImageSize(2);
			for(int a=0; a<=absVal;a++){
			    output.write(absBuf[a]);
			    incCompImageSize(1);
			}
			if (!isEven(absVal+1)){
			    //Padding
			    output.write(0);
			    incCompImageSize(1);
			}
			
		    }
		    else if(absVal > -1){
			for (int b=0;b<=absVal;b++){
			    output.write(1);
			    output.write(absBuf[b]);
			    incCompImageSize(2);
			}
		    }   
		}
		/// EOF scanline 

		output.write(0);
		output.write(0);
		incCompImageSize(2);
	    }
	}
    }

    private void encodeRLE4(byte[] bipixels, int scanlineBytes) 
	throws IOException {

	int runCount=2, absVal=-1, j=-1, pixel=0, q=0;
	byte runVal1=0, runVal2=0, nextVal1=0, nextVal2=0;
	byte[] absBuf = new byte[256];

	
	runVal1 = bipixels[++j];
	runVal2 = bipixels[++j];
	
	while (j < scanlineBytes-2){
	    nextVal1 = bipixels[++j];
	    nextVal2 = bipixels[++j];
	    
	    if (nextVal1 == runVal1 ) {
		
		//Check if there was an existing Absolute Run
		if(absVal >= 4){
		    output.write(0);
		    output.write(absVal - 1);
		    incCompImageSize(2);
		    // we need to exclude  last 2 elts, similarity of
		    // which caused to enter this part of the code
		    for(int a=0; a<absVal-2;a+=2){
			pixel = (absBuf[a] << 4) | absBuf[a+1];
			output.write((byte)pixel);
			incCompImageSize(1);
		    }
		    // if # of elts is odd - read the last element
		    if(!(isEven(absVal-1))){
			q = absBuf[absVal-2] << 4| 0;
			output.write(q);
			incCompImageSize(1);
		    }
		    // Padding to word align absolute encoding
		    if ( !isEven((int)Math.ceil((absVal-1)/2)) ) {
			output.write(0);
			incCompImageSize(1);
		    }
		} else if (absVal > -1){
		    output.write(2);
		    pixel = (absBuf[0] << 4) | absBuf[1];
		    output.write(pixel);
		    incCompImageSize(2);
		}
		absVal = -1;
		
		if (nextVal2 == runVal2){
		    // Even runlength
		    runCount+=2;
		    if(runCount == 256){
			output.write(runCount-1);
			pixel = ( runVal1 << 4) | runVal2;
			output.write(pixel);
			incCompImageSize(2);
			runCount =2;
			if(j< scanlineBytes - 1){
			    runVal1 = runVal2;
			    runVal2 = bipixels[++j];
			} else {
			    output.write(01);
			    int r = runVal2 << 4 | 0;
			    output.write(r);
			    incCompImageSize(2);
			    runCount = -1;/// Only EOF required now
			}
		    }
		} else {
		    // odd runlength and the run ends here
		    // runCount wont be > 254 since 256/255 case will
		    // be taken care of in above code. 
		    runCount++;
		    pixel = ( runVal1 << 4) | runVal2;
		    output.write(runCount);
		    output.write(pixel);
		    incCompImageSize(2);
		    runCount = 2;
		    runVal1 = nextVal2;
		    // If end of scanline
		    if (j < scanlineBytes -1){
			runVal2 = bipixels[++j];
		    }else {
			output.write(01);
			int r = nextVal2 << 4 | 0;
			output.write(r);
			incCompImageSize(2);
			runCount = -1;/// Only EOF required now
		    }
		    
		}
	    } else{
		// Check for existing run
		if (runCount > 2){
		    pixel = ( runVal1 << 4) | runVal2;
		    output.write(runCount);
		    output.write(pixel);
		    incCompImageSize(2);
		} else if (absVal < 0){ // first time
		    absBuf[++absVal] = runVal1;
		    absBuf[++absVal] = runVal2;
		    absBuf[++absVal] = nextVal1;
		    absBuf[++absVal] = nextVal2;
		} else if (absVal < 253){ // only 255 elements
		    absBuf[++absVal] = nextVal1;
		    absBuf[++absVal] = nextVal2;
		} else {
		    output.write(0);
		    output.write(absVal+1);
		    incCompImageSize(2);
		    for(int a=0; a<absVal;a+=2){
			pixel = (absBuf[a] << 4) | absBuf[a+1];
			output.write((byte)pixel);
			incCompImageSize(1);
		    }
		    // Padding for word align
		    // since it will fit into 127 bytes
		    output.write(0);
		    incCompImageSize(1);
		    absVal = -1;
		}
		
		runVal1 = nextVal1;
		runVal2 = nextVal2;
		runCount = 2;
	    }
	    // Handle the End of scanline for the last 2 4bits
	    if (j >= scanlineBytes-2 ) {
		if (absVal == -1 && runCount >= 2){
		    if (j == scanlineBytes-2){
			if(bipixels[++j] == runVal1){
			    runCount++;
			    pixel = ( runVal1 << 4) | runVal2;
			    output.write(runCount);
			    output.write(pixel);
			    incCompImageSize(2);
			} else {
			    pixel = ( runVal1 << 4) | runVal2;
			    output.write(runCount);
			    output.write(pixel);
			    output.write(01);
			    pixel =  bipixels[j]<<4 |0;
			    output.write(pixel);
			    int n = bipixels[j]<<4|0;
			    incCompImageSize(4); 
			}
		    } else {
			output.write(runCount);
			pixel =( runVal1 << 4) | runVal2 ;
			output.write(pixel);	
			incCompImageSize(2);
		    }
		} else if(absVal > -1){
		    if (j == scanlineBytes-2){
			absBuf[++absVal] = bipixels[++j];
		    }
		    if (absVal >=2){
			output.write(0);
			output.write(absVal+1);
			incCompImageSize(2);
			for(int a=0; a<absVal;a+=2){
			    pixel = (absBuf[a] << 4) | absBuf[a+1];
			    output.write((byte)pixel);
			    incCompImageSize(1);
			}
			if(!(isEven(absVal+1))){
			    q = absBuf[absVal] << 4|0;
			    output.write(q);
			    incCompImageSize(1);
			}
			
			// Padding
			if ( !isEven((int)Math.ceil((absVal+1)/2)) ) {
			    output.write(0);
			    incCompImageSize(1);
			}
			
		    } else {
			switch (absVal){
			case 0:
			    output.write(1);
			    int n = absBuf[0]<<4 | 0;
			    output.write(n);
			    incCompImageSize(2);
			    break;
			case 1:
			    output.write(2);
			    pixel = (absBuf[0] << 4) | absBuf[1];
			    output.write(pixel);
			    incCompImageSize(2);
			    break;
			}
		    }
		    
		}
		output.write(0);
		output.write(0);
		incCompImageSize(2);
	    }
	}
    }
    

    private synchronized void incCompImageSize(int value){
	compImageSize = compImageSize + value;
    }
    private boolean isEven(int number) {
	return (number%2 == 0 ? true : false);
    }
    private void writeFileHeader(int fileSize, int offset) throws IOException {
	// magic value
	output.write('B');                     
	output.write('M');
	
	// File size
	writeDWord(fileSize);
	
	// reserved1 and reserved2
	output.write(0);
	output.write(0);
	output.write(0);
	output.write(0);
	
	// offset to image data
	writeDWord(offset);
    }

    
    private void writeInfoHeader(int headerSize, int bitsPerPixel) 
	throws IOException {

	// size of header
	writeDWord(headerSize);
	
	// width
	writeDWord(w);
	
	// height
	writeDWord(h);
	
	// number of planes
	writeWord(1);
	
	// Bits Per Pixel
	writeWord(bitsPerPixel);	
    }
    
    // Methods for little-endian writing
    public void writeWord(int word) throws IOException {
	output.write(word & 0xff);
	output.write((word & 0xff00) >> 8);
    }
    
    public void writeDWord(int dword) throws IOException {
	output.write(dword & 0xff);
	output.write((dword & 0xff00) >> 8);
	output.write((dword & 0xff0000) >> 16);
	output.write((dword & 0xff000000) >> 24);
    } 
    private void writeSize(int dword, int offset) throws IOException {
	((SeekableOutputStream)output).seek(offset);
	writeDWord(dword);
    }
}

