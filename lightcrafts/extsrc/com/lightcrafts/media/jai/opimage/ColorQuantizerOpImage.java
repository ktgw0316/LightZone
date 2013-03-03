/*
 * $RCSfile: ColorQuantizerOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/05/10 01:03:22 $
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
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.PixelAccessor;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.mediax.jai.UnpackedImageData;

/**
 * An <code>OpImage</code> implementing the color quantization operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.ColorQuantizerDescriptor</code>.
 *
 * <p>This <code>OpImage</code> generates an optimal lookup table from the
 * source RGB image.  This lookup table can also be used as a parameter of
 * operators such as "errordiffusion" to convert the source image into
 * a color-indexed image.
 *
 * <p> This <code>OpImage</code> contains the pixels of the result images
 * from the nearest distance classification based on the lookup table
 * generated from this <code>OpImage</code>.
 *
 * @see com.lightcrafts.mediax.jai.KernelJAI
 * @see com.lightcrafts.mediax.jai.LookupTableJAI
 *
 * @JAI 1.1.2
 *
 */
abstract class ColorQuantizerOpImage extends PointOpImage {
    /**
     * Variables used in the optimized case of 3-band byte to 1-band byte
     * with a ColorCube color map and a Floyd-Steinberg kernel.
     */
    private static final int NBANDS = 3;
    private static final int NGRAYS = 256;

    /** Cache the <code>PixelAccessor</code> for computation. */
    protected PixelAccessor srcPA;

    /** Cache the source type. */
    protected int srcSampleType;

    protected boolean isInitialized = false;

    /** Cache the <code>PixelAccessor</code> for computation. */
    protected PixelAccessor destPA;

    /**
     * The color map which maps the <code>ErrorDiffusionOpImage</code> to
     * its source.
     */
    protected LookupTableJAI colorMap;

    /**
     * The expected maximum number of color, that is, the expected size of
     * the lookup table.
     */
    protected int maxColorNum;

    /** The subsample rate in the x direction. */
    protected int xPeriod;

    /** The subsample rate in y direction. */
    protected int yPeriod;

    /** The ROI used to define the data set for training. */
    protected ROI roi;

    /**
     * The number of bands in the source image.
     */
    private int numBandsSource;

    /**
     * Whether to check for skipped tiles.
     */
    protected boolean checkForSkippedTiles = false;

    /** Used by the subclasses to define the start pixel position. */
    final static int startPosition(int pos, int start, int period) {
        int t = (pos - start) % period;
        return t == 0 ? pos : pos + (period - t);
    }

    /**
     * Force the destination image to be single-banded.
     */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source) {
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

        // Make sure that this OpImage is single-banded.
        if (sm.getNumBands() != 1) {
            sm =
                RasterFactory.createComponentSampleModel(sm,
                                                         sm.getTransferType(),
                                                         sm.getWidth(),
                                                         sm.getHeight(),
                                                         1);
	    il.setSampleModel(sm);
        }

        il.setColorModel(null);

	return il;
    }

    /**
     * Constructs a ColorQuantizerOpImage object.
     *
     * <p>The image dimensions are derived from the source image. The tile
     * grid layout, SampleModel, and ColorModel may optionally be specified
     * by an ImageLayout object.
     *
     * @param source A RenderedImage.
     * @param config The rendering hints.
     * @param layout An ImageLayout optionally containing the tile grid layout,
     * SampleModel, and ColorModel, or null.
     * @param maxColorNum The expected maximum number of colors.
     */
    public ColorQuantizerOpImage(RenderedImage source,
                                 Map config,
                                 ImageLayout layout,
                                 int maxColorNum,
                                 ROI roi,
                                 int xPeriod,
                                 int yPeriod) {
	super(source, layoutHelper(layout, source), config, true);

        // Get the source sample model.
        SampleModel srcSampleModel = source.getSampleModel();

        // Cache the number of bands in the source.
        numBandsSource = srcSampleModel.getNumBands();

        this.maxColorNum = maxColorNum;
        this.xPeriod = xPeriod;
        this.yPeriod = yPeriod;
        this.roi = roi;
        this.checkForSkippedTiles =
            xPeriod > tileWidth || yPeriod > tileHeight;
    }

    protected void computeRect(Raster[] sources,
                            WritableRaster dest,
                            Rectangle destRect) {
        if (colorMap == null)
            train();

        if(!isInitialized) {
            srcPA = new PixelAccessor(getSourceImage(0));
            srcSampleType = srcPA.sampleType == PixelAccessor.TYPE_BIT ?
                DataBuffer.TYPE_BYTE : srcPA.sampleType;
            isInitialized = true;
        }

        UnpackedImageData uid =
            srcPA.getPixels(sources[0], destRect,
                            srcSampleType, false);
        Rectangle rect = uid.rect;
        byte[][] data = uid.getByteData();
        int srcLineStride = uid.lineStride;
        int srcPixelStride = uid.pixelStride;
        byte[] rBand = data[0];
        byte[] gBand = data[1];
        byte[] bBand = data[2];

        int lastLine = rect.height * srcLineStride + uid.bandOffsets[0];

        if (destPA == null)
            destPA = new PixelAccessor(this);

        UnpackedImageData destUid =
            destPA.getPixels(dest, destRect,
                             sampleModel.getDataType(), false);

        int destLineOffset = destUid.bandOffsets[0];
        int destLineStride = destUid.lineStride;
        byte[] d = destUid.getByteData(0);

        int[] currentPixel = new int[3];
        for (int lo = uid.bandOffsets[0]; lo < lastLine; lo += srcLineStride) {
            int lastPixel =
                lo + rect.width * srcPixelStride - uid.bandOffsets[0];
            int dstPixelOffset = destLineOffset;
            for (int po = lo - uid.bandOffsets[0]; po < lastPixel;
                 po += srcPixelStride) {
                d[dstPixelOffset] =
                    findNearestEntry(rBand[po + uid.bandOffsets[0]] & 0xff,
                                     gBand[po + uid.bandOffsets[1]] & 0xff,
                                     bBand[po + uid.bandOffsets[2]] & 0xff);

                dstPixelOffset += destUid.pixelStride;
            }
            destLineOffset += destLineStride;
        }
    }

    /** Returns one of the available statistics as a property. */
    public Object getProperty(String name) {
        int numBands = sampleModel.getNumBands();

        if (name.equals("JAI.LookupTable") ||
            name.equals("LUT")) {
            if (colorMap == null)
                train();
            return colorMap;
        }

        return super.getProperty(name);
    }

    protected abstract void train();

    public ColorModel getColorModel() {
        if (colorMap == null)
            train();
        if (colorModel == null)
            colorModel =
                new IndexColorModel(8, colorMap.getByteData(0).length,
                                    colorMap.getByteData(0),
                                    colorMap.getByteData(1),
                                    colorMap.getByteData(2));
        return colorModel;
    }

    protected byte findNearestEntry(int r, int g, int b) {
        byte[] red = colorMap.getByteData(0);
        byte[] green = colorMap.getByteData(1);
        byte[] blue = colorMap.getByteData(2);
        int index = 0;

        int dr = r - (red[0] & 0xFF);
        int dg = g - (green[0] & 0xFF);
        int db = b - (blue[0] & 0xFF);
        int minDistance = dr * dr + dg * dg + db * db;

        // Find the distance to each entry and set the result to
        // the index which is closest to the argument.
        for(int i = 1; i < red.length; i++) {
            dr = r - (red[i] & 0xFF);
            int distance = dr * dr;
            if (distance > minDistance)
                continue;
            dg = g - (green[i] & 0xFF);
            distance += dg * dg;

            if (distance > minDistance)
                continue;
            db = b - (blue[i] & 0xFF);
            distance += db * db;
            if(distance < minDistance) {
                minDistance = distance;
                index = i;
            }
        }
        return (byte)index;
    }
}
