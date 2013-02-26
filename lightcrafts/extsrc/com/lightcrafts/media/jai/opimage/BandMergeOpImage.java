/*
 * $RCSfile: BandMergeOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2006/06/19 18:33:35 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import java.util.Map;
import com.lightcrafts.mediax.jai.PixelAccessor;
import com.lightcrafts.mediax.jai.UnpackedImageData;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.media.jai.util.JDKWorkarounds;

/**
 * An <code>OpImage</code> implementing the "BandMerge" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.BandMergeDescriptor</code>.
 *
 * <p>This <code>OpImage</code> bandmerges the pixel values of two or
 * more source images.
 * 
 * The data type <code>byte</code> is treated as unsigned, with maximum
 * value as 255 and minimum value as 0.
 *
 * There is no attempt to rescale binary images to the approapriate
 * gray levels, such as 255 or 0. A lookup should be performed first
 * if so desired.
 *
 * @since JAI 1.1
 * @see com.lightcrafts.mediax.jai.operator.BandMergeDescriptor
 * @see BandMergeCRIF
 *
 */
class BandMergeOpImage extends PointOpImage {

    // list of ColorModels required for IndexColorModel support
    ColorModel[] colorModels;

    /**
     * Constructs a <code>BandMergeOpImage</code>.
     *
     * <p>The <code>layout</code> parameter may optionally contain the
     * tile grid layout, sample model, and/or color model. The image
     * dimension is determined by the intersection of the bounding boxes
     * of the source images.
     *
     * <p>The image layout of the first source image, <code>source1</code>,
     * is used as the fallback for the image layout of the destination
     * image. The destination number of bands is the sum of all source
     * image bands.
     *
     * @param sources  <code>Vector</code> of sources.
     * @param config   Configurable attributes of the image including
     *                 configuration variables indexed by
     *                 <code>RenderingHints.Key</code>s and image properties indexed
     *                 by <code>String</code>s or <code>CaselessStringKey</code>s.
     *                 This is simply forwarded to the superclass constructor.
     * @param layout   The destination image layout.
     */
    public BandMergeOpImage(Vector sources,
                            Map config,
                            ImageLayout layout) {        

        super(sources, layoutHelper(sources, layout), config, true);

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // get ColorModels for IndexColorModel support
        int numSrcs = sources.size();
        colorModels = new ColorModel[numSrcs];

        for ( int i = 0; i < numSrcs; i++ ) {
            colorModels[i] = ((RenderedImage)sources.get(i)).getColorModel();
        }
    }


    private static int totalNumBands(Vector sources) {
        int total = 0;

        for ( int i = 0; i < sources.size(); i++ ) {
            RenderedImage image = (RenderedImage) sources.get(i);

            if ( image.getColorModel() instanceof IndexColorModel ) {
               total += image.getColorModel().getNumComponents();
            } else {
               total += image.getSampleModel().getNumBands();
            }
        }

        return total;
    }

    private static ImageLayout layoutHelper(Vector sources,
                                            ImageLayout il) {

        ImageLayout layout = (il == null) ? new ImageLayout() : (ImageLayout)il.clone();

        int numSources = sources.size();

        // dest data type is the maximum of transfertype of source image
        // utilizing the monotonicity of data types.

        // dest number of bands = sum of source bands
        int destNumBands = totalNumBands(sources);

        int destDataType = DataBuffer.TYPE_BYTE;  // initialize
        RenderedImage srci = (RenderedImage)sources.get(0);
        Rectangle destBounds = new Rectangle(srci.getMinX(),  srci.getMinY(),
                                             srci.getWidth(), srci.getHeight());                                             
        for ( int i = 0; i < numSources; i++ ) {
            srci = (RenderedImage)sources.get(i);
            destBounds = destBounds.intersection(new Rectangle(srci.getMinX(), srci.getMinY(),
                                                 srci.getWidth(), srci.getHeight()));

            int typei = srci.getSampleModel().getTransferType();

            // NOTE: this depends on JDK ordering
            destDataType = typei > destDataType ? typei : destDataType;
        }

        SampleModel sm = layout.getSampleModel((RenderedImage)sources.get(0));

        if ( sm.getNumBands() < destNumBands ) {
            int[] destOffsets = new int[destNumBands];

            for(int i=0; i < destNumBands; i++) {
                destOffsets[i] = i;
            }

            // determine the proper width and height to use
            int destTileWidth = sm.getWidth();
            int destTileHeight = sm.getHeight();
            if(layout.isValid(ImageLayout.TILE_WIDTH_MASK))
            {
                destTileWidth =
                    layout.getTileWidth((RenderedImage)sources.get(0));
            }
            if(layout.isValid(ImageLayout.TILE_HEIGHT_MASK))
            {
                destTileHeight =
                    layout.getTileHeight((RenderedImage)sources.get(0));
            }
            
            sm = RasterFactory.createComponentSampleModel(sm,
                                                          destDataType,
                                                          destTileWidth,
                                                          destTileHeight,
                                                          destNumBands);


            layout.setSampleModel(sm);
        }

        ColorModel cm = layout.getColorModel(null);

        if ( cm != null &&
             !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
            // Clear the mask bit if incompatible.
            layout.unsetValid(ImageLayout.COLOR_MODEL_MASK);
        }

        return layout;
    }


    /**
     * BandMerges the pixel values of two source images within a specified
     * rectangle.
     *
     * @param sources   Cobbled sources, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {

        int destType = dest.getTransferType();

        switch(destType){
        case DataBuffer.TYPE_BYTE:
             byteLoop(sources, dest, destRect);
             break;
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
             shortLoop(sources, dest, destRect);
             break;
        case DataBuffer.TYPE_INT:
             intLoop(sources, dest, destRect);
             break;          
        case DataBuffer.TYPE_FLOAT:
             floatLoop(sources, dest, destRect);
             break;
        case DataBuffer.TYPE_DOUBLE:
             doubleLoop(sources, dest, destRect);
             break;
        default:
             throw new RuntimeException();
        }
    }


    private void byteLoop(Raster[] sources,
                          WritableRaster dest,
                          Rectangle destRect) {
        int nSrcs = sources.length;
        int[] snbands = new int[nSrcs];
        PixelAccessor[] pas = new PixelAccessor[nSrcs];

        for ( int i = 0; i < nSrcs; i++ ) {
            pas[i] = new PixelAccessor(sources[i].getSampleModel(), colorModels[i]);

            if ( colorModels[i] instanceof IndexColorModel ) {
                snbands[i] = colorModels[i].getNumComponents();
            } else {
                snbands[i] = sources[i].getNumBands();
            }
        }        

        int dnbands     = dest.getNumBands();
        int destType    = dest.getTransferType();
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest,
                                             destRect,    //liney,
                                             destType,
                                             true);

        byte[][] dstdata = (byte[][])dimd.data;

        for ( int sindex = 0, db = 0; sindex < nSrcs; sindex++ ) {

            UnpackedImageData simd =
                colorModels[sindex] instanceof IndexColorModel ?
                pas[sindex].getComponents(sources[sindex],
                                          destRect,
                                          sources[sindex].getSampleModel().getTransferType()) :
                pas[sindex].getPixels(sources[sindex],
                                      destRect,
                                      sources[sindex].getSampleModel().getTransferType(),
                                      false);

              
            int srcPixelStride = simd.pixelStride;
            int srcLineStride  = simd.lineStride;
            int dstPixelStride = dimd.pixelStride;
            int dstLineStride  = dimd.lineStride;
            int dRectWidth     = destRect.width;

            for ( int sb = 0; sb < snbands[sindex]; sb++, db++ ) {
                if ( db >= dnbands ) {
                     // exceeding destNumBands; should not have happened
                     break; 
                }

                byte[]   dstdatabandb = dstdata[db];
                byte[][] srcdata = (byte[][])simd.data;
                byte[]   srcdatabandsb = srcdata[sb];
                int srcstart = simd.bandOffsets[sb];
                int dststart = dimd.bandOffsets[db];

                for(int y = 0;
                    y < destRect.height;
                    y++, srcstart += srcLineStride, dststart += dstLineStride){
                    
                    for(int i=0, srcpos = srcstart, dstpos = dststart;
                        i < dRectWidth;
                        i++, srcpos += srcPixelStride, dstpos += dstPixelStride){
                          
                             dstdatabandb[dstpos] = srcdatabandsb[srcpos];
                    }
                }
            }
        }

        d.setPixels(dimd);
    }


    private void shortLoop(Raster[] sources,
                           WritableRaster dest,
                           Rectangle destRect) {
        int nSrcs = sources.length;
        int[] snbands = new int[nSrcs];
        PixelAccessor[] pas = new PixelAccessor[nSrcs];

        for ( int i = 0; i < nSrcs; i++ ) {
            pas[i] = new PixelAccessor(sources[i].getSampleModel(), colorModels[i]);

            if ( colorModels[i] instanceof IndexColorModel ) {
                snbands[i] = colorModels[i].getNumComponents();
            } else {
                snbands[i] = sources[i].getNumBands();
            }
        }        

        int dnbands     = dest.getNumBands();
        int destType    = dest.getTransferType();
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(),null);

        UnpackedImageData dimd = d.getPixels(dest,
                                             destRect,    //liney,
                                             destType,
                                             true);

        short[][] dstdata = (short[][])dimd.data;

        for ( int sindex = 0, db = 0; sindex < nSrcs; sindex++ ) {

            UnpackedImageData simd =
                colorModels[sindex] instanceof IndexColorModel ?
                pas[sindex].getComponents(sources[sindex],
                                          destRect,
                                          sources[sindex].getSampleModel().getTransferType()) :
                pas[sindex].getPixels(sources[sindex],
                                      destRect,
                                      sources[sindex].getSampleModel().getTransferType(),
                                      false);
              
            int srcPixelStride = simd.pixelStride;
            int srcLineStride  = simd.lineStride;
            int dstPixelStride = dimd.pixelStride;
            int dstLineStride  = dimd.lineStride;
            int dRectWidth     = destRect.width;

            for ( int sb = 0; sb < snbands[sindex]; sb++, db++ ) {
                if ( db < dnbands ) {
                    short[][] srcdata = (short[][])simd.data;
                    int srcstart = simd.bandOffsets[sb];
                    int dststart = dimd.bandOffsets[db];
                    for(int y = 0;
                        y < destRect.height;
                        y++, srcstart += srcLineStride, dststart += dstLineStride){
                        
                        for(int i=0, srcpos = srcstart, dstpos = dststart;
                            i < dRectWidth;
                            i++, srcpos += srcPixelStride, dstpos += dstPixelStride){
                          
                                dstdata[db][dstpos] = srcdata[sb][srcpos];
                        }
                    }
                }
            }
        }

        d.setPixels(dimd);
    }


    private void intLoop(Raster[] sources,
                         WritableRaster dest,
                         Rectangle destRect) {
        int nSrcs = sources.length;
        int[] snbands = new int[nSrcs];
        PixelAccessor[] pas = new PixelAccessor[nSrcs];

        for ( int i = 0; i < nSrcs; i++ ) {
            pas[i] = new PixelAccessor(sources[i].getSampleModel(), colorModels[i]);

            if ( colorModels[i] instanceof IndexColorModel ) {
                snbands[i] = colorModels[i].getNumComponents();
            } else {
                snbands[i] = sources[i].getNumBands();
            }
        }        

        int dnbands     = dest.getNumBands();
        int destType    = dest.getTransferType();
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(),null);

        UnpackedImageData dimd = d.getPixels(dest,
                                             destRect,    //liney,
                                             destType,
                                             true);

        int[][] dstdata = (int[][])dimd.data;

        for ( int sindex = 0, db = 0; sindex < nSrcs; sindex++ ) {

            UnpackedImageData simd =
                colorModels[sindex] instanceof IndexColorModel ?
                pas[sindex].getComponents(sources[sindex],
                                          destRect,
                                          sources[sindex].getSampleModel().getTransferType()) :
                pas[sindex].getPixels(sources[sindex],
                                      destRect,
                                      sources[sindex].getSampleModel().getTransferType(),
                                      false);

            int srcPixelStride = simd.pixelStride;
            int srcLineStride  = simd.lineStride;
            int dstPixelStride = dimd.pixelStride;
            int dstLineStride  = dimd.lineStride;
            int dRectWidth     = destRect.width;

            for ( int sb = 0; sb < snbands[sindex]; sb++, db++ ) {
                if ( db < dnbands ) {
                    int[][] srcdata = (int[][])simd.data;
                    int srcstart = simd.bandOffsets[sb];
                    int dststart = dimd.bandOffsets[db];
                    for(int y = 0;
                        y < destRect.height;
                        y++, srcstart += srcLineStride, dststart += dstLineStride){
                        
                        for(int i=0, srcpos = srcstart, dstpos = dststart;
                            i < dRectWidth;
                            i++, srcpos += srcPixelStride, dstpos += dstPixelStride){
                           
                                dstdata[db][dstpos] = srcdata[sb][srcpos];
                        }
                    }
                }
            }
        }

        d.setPixels(dimd);
    }


    private void floatLoop(Raster[] sources,
                           WritableRaster dest,
                           Rectangle destRect) {

        int nSrcs = sources.length;
        int[] snbands = new int[nSrcs];
        PixelAccessor[] pas = new PixelAccessor[nSrcs];

        for ( int i = 0; i < nSrcs; i++ ) {
            pas[i] = new PixelAccessor(sources[i].getSampleModel(), colorModels[i]);

            if ( colorModels[i] instanceof IndexColorModel ) {
                snbands[i] = colorModels[i].getNumComponents();
            } else {
                snbands[i] = sources[i].getNumBands();
            }
        }        

        int dnbands     = dest.getNumBands();
        int destType    = dest.getTransferType();
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest,
                                             destRect,    //liney,
                                             destType,
                                             true);

        float[][] dstdata = (float[][])dimd.data;

        for ( int sindex = 0, db = 0; sindex < nSrcs; sindex++ ) {

            UnpackedImageData simd =
                colorModels[sindex] instanceof IndexColorModel ?
                pas[sindex].getComponents(sources[sindex],
                                          destRect,
                                          sources[sindex].getSampleModel().getTransferType()) :
                pas[sindex].getPixels(sources[sindex],
                                      destRect,
                                      sources[sindex].getSampleModel().getTransferType(),
                                      false);

            int srcPixelStride = simd.pixelStride;
            int srcLineStride  = simd.lineStride;
            int dstPixelStride = dimd.pixelStride;
            int dstLineStride  = dimd.lineStride;
            int dRectWidth     = destRect.width;

            for ( int sb = 0; sb < snbands[sindex]; sb++, db++ ) {
                if ( db < dnbands ) {
                    float[][] srcdata = (float[][])simd.data;
                    int srcstart = simd.bandOffsets[sb];
                    int dststart = dimd.bandOffsets[db];
                    for(int y = 0;
                        y < destRect.height;
                        y++, srcstart += srcLineStride, dststart += dstLineStride){
                        
                        for(int i=0, srcpos = srcstart, dstpos = dststart;
                            i < dRectWidth;
                            i++, srcpos += srcPixelStride, dstpos += dstPixelStride){
                           
                                dstdata[db][dstpos] = srcdata[sb][srcpos];
                        }
                    }
                }
            }
        }

        d.setPixels(dimd);
    }

    private void doubleLoop(Raster[] sources,
                          WritableRaster dest,
                          Rectangle destRect) {

        int nSrcs = sources.length;
        int[] snbands = new int[nSrcs];
        PixelAccessor[] pas = new PixelAccessor[nSrcs];

        for ( int i = 0; i < nSrcs; i++ ) {
            pas[i] = new PixelAccessor(sources[i].getSampleModel(), colorModels[i]);

            if ( colorModels[i] instanceof IndexColorModel ) {
                snbands[i] = colorModels[i].getNumComponents();
            } else {
                snbands[i] = sources[i].getNumBands();
            }
        }        

        int dnbands     = dest.getNumBands();
        int destType    = dest.getTransferType();
        PixelAccessor d = new PixelAccessor(dest.getSampleModel(), null);

        UnpackedImageData dimd = d.getPixels(dest,
                                             destRect,    //liney,
                                             destType,
                                             true);

        double[][] dstdata = (double[][])dimd.data;

        for ( int sindex = 0, db = 0; sindex < nSrcs; sindex++ ) {

            UnpackedImageData simd =
                colorModels[sindex] instanceof IndexColorModel ?
                pas[sindex].getComponents(sources[sindex],
                                          destRect,
                                          sources[sindex].getSampleModel().getTransferType()) :
                pas[sindex].getPixels(sources[sindex],
                                      destRect,
                                      sources[sindex].getSampleModel().getTransferType(),
                                      false);

            int srcPixelStride = simd.pixelStride;
            int srcLineStride  = simd.lineStride;
            int dstPixelStride = dimd.pixelStride;
            int dstLineStride  = dimd.lineStride;
            int dRectWidth     = destRect.width;

            for ( int sb = 0; sb < snbands[sindex]; sb++, db++ ) {
                if ( db < dnbands ) {
                    double[][] srcdata = (double[][])simd.data;
                    int srcstart = simd.bandOffsets[sb];
                    int dststart = dimd.bandOffsets[db];
                    for(int y = 0;
                        y < destRect.height;
                        y++, srcstart += srcLineStride, dststart += dstLineStride){
                        
                        for(int i=0, srcpos = srcstart, dstpos = dststart;
                            i < dRectWidth;
                            i++, srcpos += srcPixelStride, dstpos += dstPixelStride){
                           
                                dstdata[db][dstpos] = srcdata[sb][srcpos];
                        }
                    }
                }
            }
        }

        d.setPixels(dimd);
    }
}
