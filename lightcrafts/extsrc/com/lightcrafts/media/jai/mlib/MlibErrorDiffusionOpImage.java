/*
 * $RCSfile: MlibErrorDiffusionOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:55 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.mlib;

import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.ColorCube;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.mediax.jai.UntiledOpImage;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.sun.medialib.mlib.Image;
import com.sun.medialib.mlib.mediaLibImage;
import com.sun.medialib.mlib.mediaLibImageColormap;

/**
 * An OpImage class that performs an "ErrorDiffusion" operation.
 *
 */
final class MlibErrorDiffusionOpImage extends UntiledOpImage {
    /**
     * The integer-to-float scale factor for kernel elements.
     */
    private static final int KERNEL_SCALE_EXPONENT = 16;

    /**
     * The mediaLib colormap.
     */
    protected mediaLibImageColormap mlibColormap;

    /**
     * The scaled values of the error kernel in row major order.
     */
    protected int[] kernel;

    /**
     * The width of the kernel.
     */
    protected int kernelWidth;

    /**
     * The height of the kernel.
     */
    protected int kernelHeight;

    /**
     * The X position of the key element in the kernel.
     */
    protected int kernelKeyX;

    /**
     * The Y position of the key element in the kernel.
     */
    protected int kernelKeyY;

    /**
     * Scale factor to convert kernel from floating point to integer.
     */
    protected int kernelScale;

    /**
     * Force the destination image to be single-banded.
     */
    // Copied whole hog from ErrorDiffusionOpImage.
    static ImageLayout layoutHelper(ImageLayout layout,
                                    RenderedImage source,
                                    LookupTableJAI colormap) {
        // Create or clone the layout.
        ImageLayout il = layout == null ?
	    new ImageLayout() : (ImageLayout)layout.clone();

        // Force the destination and source origins and dimensions to coincide.
        il.setMinX(source.getMinX());
        il.setMinY(source.getMinY());
        il.setWidth(source.getWidth());
        il.setHeight(source.getHeight());

        // Get the SampleModel.
        SampleModel sm = il.getSampleModel(source);

        // Ensure an appropriate SampleModel.
        if(colormap.getNumBands() == 1 &&
           colormap.getNumEntries() == 2 &&
           !ImageUtil.isBinary(il.getSampleModel(source))) {
            sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                                 il.getTileWidth(source),
                                                 il.getTileHeight(source),
                                                 1);
            il.setSampleModel(sm);
        }

        // Make sure that this OpImage is single-banded.
        if (sm.getNumBands() != 1) {
            sm =
                RasterFactory.createComponentSampleModel(sm,
                                                         sm.getTransferType(),
                                                         sm.getWidth(),
                                                         sm.getHeight(),
                                                         1);
	    il.setSampleModel(sm);

            // Clear the ColorModel mask if needed.
            ColorModel cm = il.getColorModel(null);
            if(cm != null &&
               !JDKWorkarounds.areCompatibleDataModels(sm, cm)) {
                // Clear the mask bit if incompatible.
                il.unsetValid(ImageLayout.COLOR_MODEL_MASK);
            }
        }

        // Set an IndexColorModel on the image if:
        // a. none is provided in the layout;
        // b. source, destination, and colormap have byte data type;
        // c. the colormap has 3 bands.
        if((layout == null || !il.isValid(ImageLayout.COLOR_MODEL_MASK)) &&
           source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE &&
           sm.getDataType() == DataBuffer.TYPE_BYTE &&
           colormap.getDataType() == DataBuffer.TYPE_BYTE &&
           colormap.getNumBands() == 3) {
            ColorModel cm = source.getColorModel();
            if(cm == null ||
               (cm != null && cm.getColorSpace().isCS_sRGB())) {
                int size = colormap.getNumEntries();
                byte[][] cmap = new byte[3][256];
                for(int i = 0; i < 3; i++) {
                    byte[] band = cmap[i];
                    byte[] data = colormap.getByteData(i);
                    int offset = colormap.getOffset(i);
                    int end = offset + size;
                    for(int j = 0; j < offset; j++) {
                        band[j] = (byte)0;
                    }
                    for(int j = offset; j < end; j++) {
                        band[j] = data[j - offset];
                    }
                    for(int j = end; j < 256; j++) {
                        band[j] = (byte)0xFF;
                    }
                }

                il.setColorModel(new IndexColorModel(8, 256,
                                                     cmap[0], cmap[1],
                                                     cmap[2]));
            }
        }

	return il;
    }

    /**
     * Constructs an MlibErrorDiffusionOpImage.
     *
     * @param source    a RenderedImage.
     * @param config    configuration settings.
     * @param layout    an ImageLayout optionally containing the tile
     *                  grid layout, SampleModel, and ColorModel, or null.
     * @param colormap  the target color quantization mapping.
     * @param errorKernel the error diffusion coefficients.
     */
    public MlibErrorDiffusionOpImage(RenderedImage source,
                                     Map config,
                                     ImageLayout layout,
                                     LookupTableJAI colormap,
                                     KernelJAI errorKernel) {
        // Pass the ImageLayout up directly as it was pre-proceseed
        // in MlibErrorDiffusionRIF.
        super(source, config, layout);

        // Initialize the mediaLib colormap. It is implicitly assumed that
        // all data type and dimension checking was performed in the RIF.
        this.mlibColormap =
            Image.ColorDitherInit(colormap instanceof ColorCube ?
                                  ((ColorCube)colormap).getDimension() : null,
                                  Image.MLIB_BYTE,
                                  ImageUtil.isBinary(sampleModel) ?
                                  Image.MLIB_BIT : Image.MLIB_BYTE,
                                  colormap.getNumBands(),
                                  colormap.getNumEntries(),
                                  colormap.getOffset(),
                                  colormap.getByteData());

        // Initialize kernel constants.
        this.kernelWidth = errorKernel.getWidth();
        this.kernelHeight = errorKernel.getHeight();
        this.kernelKeyX = errorKernel.getXOrigin();
        this.kernelKeyY = errorKernel.getYOrigin();
        this.kernelScale = 0x1 << KERNEL_SCALE_EXPONENT;

        // Initialize the integral kernel coefficients.
        float[] kernelData = errorKernel.getKernelData();
        int numElements = kernelData.length;
        this.kernel = new int[numElements];
        for(int i = 0; i < numElements; i++) {
            kernel[i] = (int)(kernelData[i]*kernelScale);
        }

        /* XXX
        kernelWidth = kernelHeight = 3;
        kernelKeyX = kernelKeyY = 1;
        kernel = new int[] {0, 0, 0, 0, 0, 7, 3, 5, 1};
        kernelScale = 4;
        */
    }

    /**
     * Calculate the destination image from the source image.
     *
     * @param source The source Raster; should be the whole image.
     * @param dest The destination WritableRaster; should be the whole image.
     * @param destRect The destination Rectangle; should equal the destination
     * image bounds.
     */
    protected void computeImage(Raster[] sources,
                                WritableRaster dest,
                                Rectangle destRect) {

	Raster source = sources[0];

        int sourceFormatTag;
        int destFormatTag;
        if(ImageUtil.isBinary(dest.getSampleModel())) {
            // Hack: derive the source format tag as if it was writing to
            // a destination with the same layout as itself.
            sourceFormatTag =
                MediaLibAccessor.findCompatibleTag(sources, source);

            // Hard-code the destination format tag as we know that the image
            // has a bilevel layout.
            destFormatTag =
                dest.getSampleModel().getDataType() |
                MediaLibAccessor.BINARY |
                MediaLibAccessor.UNCOPIED;
        } else {
            sourceFormatTag = destFormatTag =
                MediaLibAccessor.findCompatibleTag(sources, dest);
        }

        MediaLibAccessor srcAccessor =
            new MediaLibAccessor(sources[0], destRect, sourceFormatTag, false);
        MediaLibAccessor dstAccessor =
            new MediaLibAccessor(dest, destRect, destFormatTag, true);

        mediaLibImage[] srcML = srcAccessor.getMediaLibImages();
        mediaLibImage[] dstML = dstAccessor.getMediaLibImages();

        Image.ColorErrorDiffusionMxN(dstML[0],
                                     srcML[0],
                                     kernel,
                                     kernelWidth,
                                     kernelHeight,
                                     kernelKeyX,
                                     kernelKeyY,
                                     KERNEL_SCALE_EXPONENT,
                                     mlibColormap);

        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }
}
