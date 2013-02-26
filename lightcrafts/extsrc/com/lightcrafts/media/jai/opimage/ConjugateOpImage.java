/*
 * $RCSfile: ConjugateOpImage.java,v $
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
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterFactory;
import java.util.Map;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/// Testing
/// import com.lightcrafts.mediax.jai.JAI;
/// import com.lightcrafts.mediax.jai.TiledImage;

/**
 * An <code>OpImage</code> implementing the "conjugate" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.ConjugateDescriptor</code>.
 *
 * <p> Note that this operation requires a signed destination image so that
 * the SampleModel is overriden to force byte and unsigned short types to
 * short and integer, respectively.
 *
 * @since EA4
 */
final class ConjugateOpImage extends PointOpImage {

    /**
     * Force the destination image to have a signed data type.
     */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source) {
        // Create or clone the layout.
        ImageLayout il = layout == null ?
            new ImageLayout() : (ImageLayout)layout.clone();

        // Get the reference SampleModel.
        SampleModel sm = il.getSampleModel(source);

        // Get the data type.
        int dataType = sm.getTransferType();

        // Determine whether the destination requires a different data type
        // and set it if so.
        boolean createNewSampleModel = false;
        if(dataType == DataBuffer.TYPE_BYTE) {
            dataType = DataBuffer.TYPE_SHORT;
            createNewSampleModel = true;
        } else if(dataType == DataBuffer.TYPE_USHORT) {
            dataType = DataBuffer.TYPE_INT;
            createNewSampleModel = true;
        }

        // Create a new SampleModel for the destination if necessary.
        if(createNewSampleModel) {
            sm = RasterFactory.createComponentSampleModel(sm, dataType,
                                                          sm.getWidth(),
                                                          sm.getHeight(),
                                                          sm.getNumBands());

            il.setSampleModel(sm);

            // Check ColorModel.
            ColorModel cm = il.getColorModel(null);
            if(cm != null &&
               !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
                // Clear the mask bit if incompatible.
                il.unsetValid(ImageLayout.COLOR_MODEL_MASK);
            }
        }

        return il;
    }

    /**
     * Constructs a ConjugateOpImage. The image dimensions are copied
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout object.
     * The destination data type must be the smallest appropriate signed
     * data type.
     *
     * @param source    a RenderedImage.
     * @param layout    an ImageLayout optionally containing the tile
     *                  grid layout, SampleModel, and ColorModel, or null.
     */
    public ConjugateOpImage(RenderedImage source,
                            Map config,
                            ImageLayout layout) {
        super(source, layoutHelper(layout, source), config, true);

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Calculates the complex conjugate of the source image.
     * The sources are cobbled.
     *
     * @param sources   an array of sources, guarantee to provide all
     *                  necessary source data for computing the rectangle.
     * @param dest      a tile that contains the rectangle to be computed.
     * @param destRect  the rectangle within this OpImage to be processed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor srcAccessor = 
            new RasterAccessor(source,srcRect,
                               formatTags[0], 
                               getSourceImage(0).getColorModel());

        RasterAccessor dstAccessor = 
            new RasterAccessor(dest,destRect, 
                               formatTags[1], getColorModel());

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor,dstAccessor);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor,dstAccessor);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor,dstAccessor);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor,dstAccessor);
            break;
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        default:
            throw new RuntimeException(JaiI18N.getString("ConjugateOpImage0"));
        }

        // If the RasterAccessor object set up a temporary buffer for the 
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void shortLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        for (int k = 0; k < dnumBands; k++)  {
            boolean isRealPart = k % 2 == 0;
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            if(isRealPart) { // real part: copy value
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++)  {
                        dstData[dstPixelOffset] = srcData[srcPixelOffset];
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            } else { // imaginary part: negate value
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++)  {
                        dstData[dstPixelOffset] =
                            ImageUtil.clampShort(-srcData[srcPixelOffset]);
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    // identical to byteLoops, except datatypes have changed.  clumsy,
    // but there's no other way in Java
    private void intLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        int srcDataArrays[][] = src.getIntDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        for (int k = 0; k < dnumBands; k++)  {
            boolean isRealPart = k % 2 == 0;
            int dstData[] = dstDataArrays[k];
            int srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            if(isRealPart) { // real part: copy value
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++)  {
                        dstData[dstPixelOffset] = srcData[srcPixelOffset];
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            } else { // imaginary part: negate value
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++)  {
                        dstData[dstPixelOffset] = -srcData[srcPixelOffset];
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void floatLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        float srcDataArrays[][] = src.getFloatDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        for (int k = 0; k < dnumBands; k++)  {
            boolean isRealPart = k % 2 == 0;
            float dstData[] = dstDataArrays[k];
            float srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            if(isRealPart) { // real part: copy value
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++)  {
                        dstData[dstPixelOffset] = srcData[srcPixelOffset];
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            } else { // imaginary part: negate value
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++)  {
                        dstData[dstPixelOffset] = -srcData[srcPixelOffset];
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

    private void doubleLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        double srcDataArrays[][] = src.getDoubleDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        for (int k = 0; k < dnumBands; k++)  {
            boolean isRealPart = k % 2 == 0;
            double dstData[] = dstDataArrays[k];
            double srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            if(isRealPart) { // real part: copy value
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++)  {
                        dstData[dstPixelOffset] = srcData[srcPixelOffset];
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            } else { // imaginary part: negate value
                for (int j = 0; j < dheight; j++)  {
                    int srcPixelOffset = srcScanlineOffset;
                    int dstPixelOffset = dstScanlineOffset;
                    for (int i = 0; i < dwidth; i++)  {
                        dstData[dstPixelOffset] = -srcData[srcPixelOffset];
                        srcPixelOffset += srcPixelStride;
                        dstPixelOffset += dstPixelStride;
                    }
                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
            }
        }
    }

//     public static void main(String[] args) {
//         int[] dataTypes =
//             new int[] {DataBuffer.TYPE_BYTE, DataBuffer.TYPE_SHORT,
//                            DataBuffer.TYPE_USHORT, DataBuffer.TYPE_INT,
//                            DataBuffer.TYPE_FLOAT, DataBuffer.TYPE_DOUBLE};

//         for(int dt = 0; dt < dataTypes.length; dt++) {
//             WritableRaster wr =
//                 RasterFactory.createBandedRaster(dataTypes[dt],
//                                                  5, 5, 4, null);
//             int k = 0;
//             for(int y = 0; y < 5; y++) {
//                 for(int x = 0; x < 5; x++) {
//                     for(int z = 0; z < 4; z++) {
//                         wr.setSample(x, y, z, k++);
//                     }
//                 }
//             }
//             SampleModel sm =
//                 RasterFactory.createBandedSampleModel(dataTypes[dt],
//                                                       2, 2, 4, null, null);
//             TiledImage ti = new TiledImage(0, 0, 5, 5, 0, 0, sm, null);
//             ti.setData(wr);
//             RenderedImage ri = JAI.create("conjugate", ti);
//             Raster r = ri.getData();
//             for(int y = 0; y < 5; y++) {
//                 for(int x = 0; x < 5; x++) {
//                     for(int z = 0; z < 4; z++) {
//                         System.out.print(r.getSampleDouble(x, y, z)+" ");
//                     }
//                     System.out.print(" : ");
//                 }
//                 System.out.println("");
//             }
//             System.out.println("");
//         }
//     }
}
