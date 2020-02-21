/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai;

import com.lightcrafts.image.color.ColorScience;

import javax.media.jai.LookupTableJAI;
import javax.media.jai.RasterFactory;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.image.WritableRaster;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.*;


/**
 * Special case of LookupTableJAI to apply the lookup on the lightness of the image.
 * Only works on InterleavedSampleModel and on 16 bit images with uniform types
 */

public class LightnessLookupTable extends LookupTableJAI {
    public LightnessLookupTable(short[] shorts, boolean b) {
        super(shorts, b);
    }

    @Override
    public WritableRaster lookup(Raster src, WritableRaster dst, Rectangle rect) {
        // Validate source.
        if (src == null) {
            throw new IllegalArgumentException("Null Source");
        }

        if (src.getNumBands() != 3)
            return super.lookup(src, dst, rect);

        SampleModel srcSampleModel = src.getSampleModel();
        if (!isIntegralDataType(srcSampleModel)) {
            throw
              new IllegalArgumentException("Source Data Type must be an Integral Data Type");
        }

        // Validate rectangle.
        if (rect == null) {
            rect = src.getBounds();
        } else {
            rect = rect.intersection(src.getBounds());
        }

        if (dst != null) {
            rect = rect.intersection(dst.getBounds());
        }

        // Validate destination.
        SampleModel dstSampleModel;
        if (dst == null) {	// create dst according to table
            dstSampleModel = getDestSampleModel(srcSampleModel,
                                                rect.width, rect.height);
            dst =
                RasterFactory.createWritableRaster(dstSampleModel,
                                                   new Point(rect.x, rect.y));
        } else {
            dstSampleModel = dst.getSampleModel();

            if (dstSampleModel.getTransferType() != getDataType() ||
                dstSampleModel.getNumBands() !=
                getDestNumBands(srcSampleModel.getNumBands())) {
                throw new
                  IllegalArgumentException("Incompatible Destination Image");
            }
        }

        // Add bit support?
        int sTagID = RasterAccessor.findCompatibleTag(null, srcSampleModel);
        int dTagID = RasterAccessor.findCompatibleTag(null, dstSampleModel);

        RasterFormatTag sTag = new RasterFormatTag(srcSampleModel,sTagID);
        RasterFormatTag dTag = new RasterFormatTag(dstSampleModel,dTagID);

        RasterAccessor s = new RasterAccessor(src, rect, sTag, null);
        RasterAccessor d = new RasterAccessor(dst, rect, dTag, null);

        /*int srcNumBands = s.getNumBands();
        int srcDataType = s.getDataType();

        int tblNumBands = getNumBands();
        int tblDataType = getDataType();*/

        int width = d.getWidth();
        int height = d.getHeight();
        int bands = d.getNumBands();
        // int dstDataType = d.getDataType();

        // Source information.
        int srcLineStride = s.getScanlineStride();
        int srcPixelStride = s.getPixelStride();
        int[] srcBandOffsets = s.getBandOffsets();

        short[][] srcData = s.getShortDataArrays();

        // Table information.
        int[] tblOffsets = getOffsets();

        short[][] tblData = getShortData();

        // Destination information.
        int dstLineStride = d.getScanlineStride();
        int dstPixelStride = d.getPixelStride();
        int[] dstBandOffsets = d.getBandOffsets();

        short[][] dstData = d.getShortDataArrays();

        int lowBandOffset = 0;
        for (int b = 0; b < bands; b++)
            if (srcBandOffsets[b] < srcBandOffsets[lowBandOffset])
                lowBandOffset = b;

        short[] sd = srcData[lowBandOffset];
        short[] dd = dstData[lowBandOffset];
        short[] td = tblData[0];

        int srcLineOffset = srcBandOffsets[lowBandOffset];
        int dstLineOffset = dstBandOffsets[lowBandOffset];
        int tblOffset = tblOffsets[0];

        for (int h = 0; h < height; h++) {
            int srcPixelOffset = srcLineOffset;
            int dstPixelOffset = dstLineOffset;

            srcLineOffset += srcLineStride;
            dstLineOffset += dstLineStride;

            final int scale = 0x4000;

            int minOffset = Math.min(srcBandOffsets[0], Math.min(srcBandOffsets[1], srcBandOffsets[2]));

            int wr = (int) (ColorScience.W[srcBandOffsets[0] - minOffset] * scale);
            int wg = (int) (ColorScience.W[srcBandOffsets[1] - minOffset] * scale);
            int wb = (int) (ColorScience.W[srcBandOffsets[2] - minOffset] * scale);

            for (int w = 0; w < width; w++) {
                int sr = sd[srcPixelOffset + 0] & 0xFFFF;
                int sg = sd[srcPixelOffset + 1] & 0xFFFF;
                int sb = sd[srcPixelOffset + 2] & 0xFFFF;

                int lum = (wr * sr + wg * sg + wb * sb) / scale;

                // prevent index out of bounds exceptions
                int index = lum - tblOffset;
                int val = td[index < 0 ? 0 : index >= 0xFFFF ? 0xFFFF : index] & 0xFFFF;

                // int mul = sg > 0 ? (scale * val) / sg : scale;
                int mul = lum > 0 ? (scale * val) / lum : scale;

                int prod = (mul * sr) / scale;
                dd[dstPixelOffset + 0] = prod > 0xFFFF ? (short) 0xFFFF : (short) prod;

                prod = (mul * sg) / scale;
                dd[dstPixelOffset + 1] = prod > 0xFFFF ? (short) 0xFFFF : (short) prod;

                prod = (mul * sb) / scale;
                dd[dstPixelOffset + 2] = prod > 0xFFFF ? (short) 0xFFFF : (short) prod;

                srcPixelOffset += srcPixelStride;
                dstPixelOffset += dstPixelStride;
            }
        }

        d.copyDataToRaster();

        return dst;
    }

    static final int clamp(int in, int maxVal) {
	return (in > maxVal ? maxVal : in);
    }
}
