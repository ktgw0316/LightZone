/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.lightcrafts.jai.LCROIShape;
import com.lightcrafts.mediax.jai.*;

import java.awt.image.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 1, 2005
 * Time: 9:54:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class BlendOpImage extends PointOpImage {

    /* Source 1 band increment */
    private int s1bd = 1;

    /* Source 2 band increment */
    private int s2bd = 1;

    /* Bilevel data flag. */
    private boolean areBinarySampleModels = false;

    private int blendModeId;

    private double opacity;

    private ROI mask;

    private final RenderedImage colorSelection;

    public enum BlendingMode {
        NORMAL      ("Normal",      0),
        AVERAGE     ("Average",     1),
        MULTIPLY    ("Multiply",    2),
        SCREEN      ("Screen",      3),
        DARKEN      ("Darken",      4),
        LIGHTEN     ("Lighten",     5),
        DIFFERENCE  ("Difference",  6),
        NEGATION    ("Negation",    7),
        EXCLUSION   ("Exclusion",   8),
        OVERLAY     ("Overlay",     9),
        HARD_LIGHT  ("Hard Light",  10),
        SOFT_LIGHT  ("Soft Light",  11),
        COLOR_DODGE ("Color Dodge", 12),
        COLOR_BURN  ("Color Burn",  13),
        SOFT_DODGE  ("Soft Dodge",  14),
        SOFT_BURN   ("Soft Burn",   15),
        /*
        SOFT_LIGHT2 ("Soft Light 2", 16),
        SOFT_LIGHT3 ("Soft Light 3", 17),
        SOFT_LIGHT4 ("Soft Light 4", 18),
        */
        SHADOWS     ("Shadows",     19),
        MID_HILIGHTS("Mid+Hilights",20),
        MIDTONES    ("Midtones",    21);

        BlendingMode(String value, int id) {
            this.name = value;
            this.id = id;
        }

        private final String name;
        private final int id;

        public String getName() { return name; }
    }

    static Map<String, BlendingMode> modes = new HashMap<String, BlendingMode>();

    static {
        for (BlendingMode b : BlendingMode.values())
            modes.put(b.name, b);
    }

    public static Set<String> availableModes() {
        return modes.keySet();
    }

    public BlendOpImage(RenderedImage source1,
                        RenderedImage source2,
                        String blendingMode,
                        Double opacity,
                        ROI mask,
                        RenderedImage colorSelection,
                        Map config,
		        ImageLayout layout) {
        super(source1, source2, layout, config, true);

        if (source1.getSampleModel().getDataType() != DataBuffer.TYPE_USHORT
            || source2.getSampleModel().getDataType() != DataBuffer.TYPE_USHORT) {
            throw new RuntimeException("Unsupported data type, only USHORT allowed.");
        }

        BlendingMode mode = modes.get(blendingMode);

        if (mode != null) {
            blendModeId = mode.id;
        } else {
            String className = this.getClass().getName();
            throw new RuntimeException(className +
                                       " unrecognized blending mode: " + blendingMode);
        }

        this.opacity = opacity.floatValue();

        if(ImageUtil.isBinary(getSampleModel()) &&
           ImageUtil.isBinary(source1.getSampleModel()) &&
           ImageUtil.isBinary(source2.getSampleModel())) {
            // Binary processing case: RasterAccessor
            areBinarySampleModels = true;
        } else {
            // Get the source band counts.
            int numBands1 = source1.getSampleModel().getNumBands();
            int numBands2 = source2.getSampleModel().getNumBands();

            // Handle the special case of adding a single band image to
            // each band of a multi-band image.
            int numBandsDst;
            if(layout != null && layout.isValid(ImageLayout.SAMPLE_MODEL_MASK)) {
                SampleModel sm = layout.getSampleModel(null);
                numBandsDst = sm.getNumBands();

                // One of the sources must be single-banded and the other must
                // have at most the number of bands in the SampleModel hint.
                if(numBandsDst > 1 &&
                   ((numBands1 == 1 && numBands2 > 1) ||
                    (numBands2 == 1 && numBands1 > 1))) {
                    // Clamp the destination band count to the number of
                    // bands in the multi-band source.
                    numBandsDst = Math.min(Math.max(numBands1, numBands2),
                                           numBandsDst);

                    // Create a new SampleModel if necessary.
                    if(numBandsDst != sampleModel.getNumBands()) {
                        sampleModel =
                            RasterFactory.createComponentSampleModel(
                                sm,
                                sampleModel.getTransferType(),
                                sampleModel.getWidth(),
                                sampleModel.getHeight(),
                                numBandsDst);

                        if(colorModel != null &&
                           !JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                                   colorModel)) {
                            colorModel =
                                ImageUtil.getCompatibleColorModel(sampleModel,
                                                                  config);
                        }
                    }

                    // Set the source band increments.
                    s1bd = numBands1 == 1 ? 0 : 1;
                    s2bd = numBands2 == 1 ? 0 : 1;
                }
            }
        }

        this.mask = mask;

        // TODO: ensure that the geometry and tiling of the sources and that of the color selection match
        this.colorSelection = colorSelection;

        // Do not set flag to permit in-place operation, we dpn't produce unique tiles.
        // permitInPlaceOperation();
    }

    // We can return source tiles directly
    public boolean computesUniqueTiles() {
        return false;
    }

    public boolean hasMask() {
        return mask != null;
    }

    public Raster getTile(int tileX, int tileY) {
        Rectangle destRect = this.getTileRect(tileX, tileY);

        if (hasMask())
            if (!mask.intersects(destRect)) {
                if (opacity > 0)
                    return this.getSourceImage(1).getTile(tileX, tileY);
                // Not a good idea, can come out with teh wrong number of bands
                /* else if (opacity == -1)
                    return this.getSourceImage(0).getTile(tileX, tileY); */
            }

        return super.getTile(tileX, tileY);
    }

    ColorModel grayCm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                false, false,
                                                Transparency.OPAQUE,
                                                DataBuffer.TYPE_BYTE);

    /**
     * Adds the pixel values of two source images within a specified
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
        if(areBinarySampleModels) {
            String className = this.getClass().getName();
            throw new RuntimeException(className +
                                       " does not implement computeRect" +
                                       " for binary data");
        }

        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor s1 = new RasterAccessor(sources[0], destRect,
                                               formatTags[0],
                                               getSourceImage(0).getColorModel());
        RasterAccessor s2 = new RasterAccessor(sources[1], destRect,
                                               formatTags[1],
                                               getSourceImage(1).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect,
                                              formatTags[2], getColorModel());

        Raster roi = null;
        if (hasMask()) {
            LCROIShape lcROI = (LCROIShape) mask;

            if (lcROI.intersects(destRect))
                roi = lcROI.getData(destRect);
            else {
                if (opacity > 0) {
                    assert dest.getBounds().equals(sources[1].getBounds());

                    // dest.setRect(sources[1]);
                    JDKWorkarounds.setRect(dest, sources[1], 0, 0);

                    return;
                }
                // Not a good idea, can come out with teh wrong number of bands
                /* else if (opacity == -1) {
                    assert dest.getBounds().equals(sources[0].getBounds());

                    // dest.setRect(sources[0]);
                    JDKWorkarounds.setRect(dest, sources[0], 0, 0);

                    return;
                } */
            }
        }

        RasterAccessor m = null;
        if (roi != null) {
            SampleModel roiSM = roi.getSampleModel();
            int roiFormatTagID = RasterAccessor.findCompatibleTag(null, roiSM);
            RasterFormatTag roiFormatTag = new RasterFormatTag(roiSM, roiFormatTagID);
            m = new RasterAccessor(roi, destRect, roiFormatTag, grayCm);
        }

        RasterAccessor cs = null;
        if (colorSelection != null) {
            int tilex = sources[0].getMinX() / sources[0].getWidth();
            int tiley = sources[0].getMinY() / sources[0].getHeight();
            Raster csRaster = colorSelection.getTile(tilex, tiley);
            if (csRaster == null) {
                csRaster = colorSelection.getData(destRect);
            }
            SampleModel csRasterSM = csRaster.getSampleModel();
            int csRasterFormatTagID = RasterAccessor.findCompatibleTag(null, csRasterSM);
            RasterFormatTag csRasterFormatTag = new RasterFormatTag(csRasterSM, csRasterFormatTagID);
            cs = new RasterAccessor(csRaster, destRect, csRasterFormatTag, grayCm);
        }

        switch (d.getDataType()) {
            case DataBuffer.TYPE_USHORT:
                computeRectUShort(s1, s2, m, cs, d);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported data type: " + d.getDataType());
        }

        if (d.needsClamping()) {
            d.clampDataArrays();
        }
        d.copyDataToRaster();
    }

    // TODO: This code only works with Pixel Interleaved sources enforce it!

    private void computeRectUShort(RasterAccessor src1,
                                   RasterAccessor src2,
                                   RasterAccessor mask,
                                   RasterAccessor colorSelection,
                                   RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        short[][] s1Data = src1.getShortDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        short[][] s2Data = src2.getShortDataArrays();

        int mLineStride = 0;
        int mPixelStride = 0;
        int mBandOffset = 0;
        byte[] mData = null;
        if (mask != null) {
            mLineStride = mask.getScanlineStride();
            mPixelStride = mask.getPixelStride();
            mBandOffset = mask.getBandOffsets()[0];
            mData = mask.getByteDataArrays()[0];
        }

        int csLineStride = 0;
        int csPixelStride = 0;
        int csBandOffset = 0;
        byte[] csData = null;
        if (colorSelection != null) {
            csLineStride = colorSelection.getScanlineStride();
            csPixelStride = colorSelection.getPixelStride();
            csBandOffset = colorSelection.getBandOffsets()[0];
            csData = colorSelection.getByteDataArrays()[0];
        }

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        int intOpacity = (int) (0xFFFF * opacity + 0.5);

        short[] s1 = s1Data[0];
        short[] s2 = s2Data[0];
        byte[] m = mData;
        byte[] cs = csData;
        short[] d = dData[0];

        int s1LineOffset = s1BandOffsets[0];
        int s2LineOffset = s2BandOffsets[0];
        int mLineOffset = mBandOffset;
        int csLineOffset = csBandOffset;
        int dLineOffset = dBandOffsets[0];

        PixelBlender.cUShortLoopCS(s1, s2, d, m, cs,
                                   bands, s1bd, s2bd,
                                   s1LineOffset, s2LineOffset, dLineOffset, mLineOffset, csLineOffset,
                                   s1LineStride, s2LineStride, dLineStride, mLineStride, csLineStride,
                                   s1PixelStride, s2PixelStride, dPixelStride, mPixelStride, csPixelStride,
                                   dheight, dwidth, intOpacity, blendModeId);
    }
}
