/*
 * $RCSfile: ConstantOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:19 $
 * $State: Exp $
 */ 
package com.lightcrafts.media.jai.opimage;
import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.SampleModel;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An OpImage class to generate an image of constant color.
 *
 * <p> ConstantOpImage defines a constant PlanarImage.  It is implemented
 * as a subclass of PatternOpImage with a constant-colored pattern.
 *
 */
final class ConstantOpImage extends PatternOpImage {

    /** Creates a Raster defining tile (0, 0) of the master pattern. */
    private static Raster makePattern(SampleModel sampleModel,
                                      Number[] bandValues) {
        WritableRaster pattern = RasterFactory.createWritableRaster(
                                 sampleModel, new Point(0, 0));

        int width = sampleModel.getWidth();
        int height = sampleModel.getHeight();
        int dataType = sampleModel.getTransferType();
        int numBands = sampleModel.getNumBands();
	
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            int[] bvalues = new int[numBands];
            for (int i = 0; i < numBands; i++) {
                bvalues[i] = bandValues[i].intValue() & ImageUtil.BYTE_MASK;
            }

            /* Put the first scanline in with setPixels. */
            for (int x = 0; x < width; x++) {
                pattern.setPixel(x, 0, bvalues);
            }
            break;

        case DataBuffer.TYPE_USHORT:	// USHORT is less than 127
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            int[] ivalues = new int[numBands];
            for (int i = 0; i < numBands; i++) {
                ivalues[i] = bandValues[i].intValue();
            }

            /* Put the first scanline in with setPixels. */
            for (int x = 0; x < width; x++) {
                pattern.setPixel(x, 0, ivalues);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            float[] fvalues = new float[numBands];
            for (int i = 0; i < numBands; i++) {
                fvalues[i] = bandValues[i].floatValue();
            }

            /* Put the first scanline in with setPixels. */
            for (int x = 0; x < width; x++) {
                pattern.setPixel(x, 0, fvalues);
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[] dvalues = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                dvalues[i] = bandValues[i].doubleValue();
            }

            /* Put the first scanline in with setPixels. */
            for (int x = 0; x < width; x++) {
                pattern.setPixel(x, 0, dvalues);
            }
            break;
        }

        /* Copy the first line out. */
        Object odata = pattern.getDataElements(0, 0, width, 1, null);

        /* Use the first line to copy other rows. */
        for (int y = 1; y < height; y++) {
            pattern.setDataElements(0, y, width, 1, odata);
        }

        return pattern;
    }

    private static SampleModel makeSampleModel(int width, int height,
                                               Number[] bandValues) {
        int numBands = bandValues.length;
        int dataType;

        if (bandValues instanceof Byte[]) {
            dataType = DataBuffer.TYPE_BYTE;
        } else if (bandValues instanceof Short[]) {
            /* If all band values are positive, use UShort, else use Short. */
            dataType = DataBuffer.TYPE_USHORT;

            Short[] shortValues = (Short[])bandValues;
            for (int i = 0; i < numBands; i++) {
                if (shortValues[i].shortValue() < 0) {
                    dataType = DataBuffer.TYPE_SHORT;
                    break;
                }
            }
        } else if (bandValues instanceof Integer[]) {
            dataType = DataBuffer.TYPE_INT;
        } else if (bandValues instanceof Float[]) {
            dataType = DataBuffer.TYPE_FLOAT;
        } else if (bandValues instanceof Double[]) {
            dataType = DataBuffer.TYPE_DOUBLE;
        } else {
            dataType = DataBuffer.TYPE_UNDEFINED;
        }

        return RasterFactory.createPixelInterleavedSampleModel(
                             dataType, width, height, numBands);
        
    }

    private static Raster patternHelper(int width, int height,
                                        Number[] bandValues) {
        SampleModel sampleModel = makeSampleModel(width, height, bandValues);
        return makePattern(sampleModel, bandValues);
    }

    private static ColorModel colorModelHelper(Number[] bandValues) {
        SampleModel sampleModel = makeSampleModel(1, 1, bandValues);
        return PlanarImage.createColorModel(sampleModel);
    }

    /**
     * Constructs a ConstantOpImage from a set of sample values.  The
     * ImageLayout object must contain a complete set of information.
     *
     * @param layout an ImageLayout containing image bounds, tile
     *        layout, and SampleModel information.
     * @param bandValues an array of Numbers representing the values of 
     *        each image band.
     */
    public ConstantOpImage(int minX, int minY,
                           int width, int height,
                           int tileWidth, int tileHeight,
                           Number[] bandValues) {
        super(patternHelper(tileWidth, tileHeight, bandValues),
              colorModelHelper(bandValues),
              minX, minY, width, height);
    }
}
