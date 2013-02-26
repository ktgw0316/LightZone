/*
 * $RCSfile: MlibOrderedDitherOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:03 $
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
import com.lightcrafts.mediax.jai.ColorCube;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.sun.medialib.mlib.Image;
import com.sun.medialib.mlib.mediaLibImage;
import com.sun.medialib.mlib.mediaLibImageColormap;

/**
 * An OpImage that performs the OrderedDither operation on 1 image
 * through mediaLib.
 */
final class MlibOrderedDitherOpImage extends PointOpImage {

    /**
     * The integer-to-float scale factor for dither mask elements.
     */
    private static final int DMASK_SCALE_EXPONENT = 16;

    /**
     * The mediaLib colormap.
     */
    protected mediaLibImageColormap mlibColormap;

    /**
     * The scaled values of the dither mask.
     */
    protected int[][] dmask;

    /**
     * The width of the mask.
     */
    protected int dmaskWidth;

    /**
     * The height of the mask.
     */
    protected int dmaskHeight;

    /**
     * Scale factor to convert mask from floating point to integer.
     */
    protected int dmaskScale;

    /**
     * Force the destination image to be single-banded.
     */
    static ImageLayout layoutHelper(ImageLayout layout,
                                    RenderedImage source,
                                    ColorCube colormap) {
        ImageLayout il;
        if (layout == null) {
            il = new ImageLayout(source);
        } else {
            il = (ImageLayout)layout.clone();
        }

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
            // TODO: Force to SHORT or USHORT if FLOAT or DOUBLE?
            sm = RasterFactory.createComponentSampleModel(sm,
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
        // c. the colormap has 3 bands; and
        // d. the source ColorModel is either null or is non-null
        //    and has a ColorSpace equal to CS_sRGB.
        if((layout == null || !il.isValid(ImageLayout.COLOR_MODEL_MASK)) &&
           source.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE &&
           il.getSampleModel(null).getDataType() == DataBuffer.TYPE_BYTE &&
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
     * Constructs an MlibOrderedDitherOpImage. The image dimensions are copied
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout object.
     *
     * @param source    a RenderedImage.
     * @param layout    an ImageLayout optionally containing the tile
     *                  grid layout, SampleModel, and ColorModel, or null.
     */
    public MlibOrderedDitherOpImage(RenderedImage source,
                                    Map config,
                                    ImageLayout layout,
                                    ColorCube colormap,
                                    KernelJAI[] ditherMask) {
        // Construct as a PointOpImage.
	super(source, layoutHelper(layout, source, colormap),
              config, true);

        // Initialize the mediaLib colormap. It is implicitly assumed that
        // all data type and dimension checking was performed in the RIF.
        this.mlibColormap =
            Image.ColorDitherInit(colormap.getDimension(),
                                  Image.MLIB_BYTE,
                                  ImageUtil.isBinary(sampleModel) ?
                                  Image.MLIB_BIT : Image.MLIB_BYTE,
                                  colormap.getNumBands(),
                                  colormap.getNumEntries(),
                                  colormap.getOffset(),
                                  colormap.getByteData());

        // Initialize dither mask constants.
        this.dmaskWidth = ditherMask[0].getWidth();
        this.dmaskHeight = ditherMask[0].getHeight();
        this.dmaskScale = 0x1 << DMASK_SCALE_EXPONENT;

        int numMasks = ditherMask.length;
        this.dmask = new int[numMasks][];

        for(int k = 0; k < numMasks; k++) {
            KernelJAI mask = ditherMask[k];

            if(mask.getWidth() != dmaskWidth ||
               mask.getHeight() != dmaskHeight) {
                throw new IllegalArgumentException
                    (JaiI18N.getString("MlibOrderedDitherOpImage0"));
            }

            // Initialize the integral dither mask coefficients.
            float[] dmaskData = ditherMask[k].getKernelData();
            int numElements = dmaskData.length;
            this.dmask[k] = new int[numElements];
            int[] dm = this.dmask[k];
            for(int i = 0; i < numElements; i++) {
                dm[i] = (int)(dmaskData[i]*dmaskScale);
            }
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * OrderedDither the pixel values of a rectangle from the source.
     * The source is cobbled.
     *
     * @param sources   an array of sources, guarantee to provide all
     *                  necessary source data for computing the rectangle.
     * @param dest      a tile that contains the rectangle to be computed.
     * @param destRect  the rectangle within this OpImage to be processed.
     */
    protected void computeRect(Raster[] sources,
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

        Image.ColorOrderedDitherMxN(dstML[0],
                                    srcML[0],
                                    dmask,
                                    dmaskWidth,
                                    dmaskHeight,
                                    DMASK_SCALE_EXPONENT,
                                    mlibColormap);

        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }
}
