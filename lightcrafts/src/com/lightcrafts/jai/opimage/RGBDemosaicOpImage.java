/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.utils.DCRaw;
import com.lightcrafts.jai.utils.Functions;

import java.awt.image.*;
import java.awt.*;
import java.util.Map;
import java.util.Arrays;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: Jun 28, 2007
 * Time: 11:18:24 AM
 */
public class RGBDemosaicOpImage extends AreaOpImage {
    private static final BorderExtender copyExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

    private final int rx, ry, gx, gy, bx, by;

    public RGBDemosaicOpImage(RenderedImage source, Map config, int rawFilters) {
        this(source, config, new ImageLayout(source), rawFilters);
    }

    public RGBDemosaicOpImage(RenderedImage source, Map config, ImageLayout layout, int rawFilters) {
        super(source, layout, config, true, copyExtender, 4, 4, 4, 4);

        switch (rawFilters) {
            case 0x16161616:
                rx=1; ry=1; gx=1; gy=0; bx=0; by=0;
                break;
            case 0x61616161:
                rx=1; ry=0; gx=0; gy=0; bx=0; by=1;
                break;
            case 0x49494949:
                rx=0; ry=1; gx=0; gy=0; bx=1; by=0;
                break;
            case 0x94949494:
                rx=0; ry=0; gx=1; gy=0; bx=1; by=1;
                break;
            default:
                rx=0; ry=0; gx=0; gy=0; bx=0; by=0;
                break;
        }
    }

    private ThreadLocal<Raster> tltmp = new ThreadLocal<Raster>();

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Rectangle imageRect = getBounds();
        imageRect.width -= 6;
        imageRect.height -= 6;
        destRect = destRect.intersection(imageRect);

        if (destRect.isEmpty())
            return;

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);
        Rectangle tmpRect = new Rectangle(destRect.x-4, destRect.y-4, destRect.width+8, destRect.height+8);

        Raster tmp;
        if ((tmp = tltmp.get()) == null || tmp.getWidth() != tmpRect.width || tmp.getHeight() != tmpRect.height) {
            tmp = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT,
                                                 tmpRect.width, tmpRect.height, 3,
                                                 new Point(tmpRect.x, tmpRect.y));
            tltmp.set(tmp);
        } else {
            Arrays.fill(((DataBufferUShort) tmp.getDataBuffer()).getData(), (short) 0);
            tmp = tmp.createTranslatedChild(tmpRect.x, tmpRect.y);
        }

        RasterAccessor srcAccessor =
                new RasterAccessor(source, srcRect, formatTags[0],
                                   getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
                new RasterAccessor(dest, destRect, formatTags[1],
                                   getColorModel());
        RasterAccessor tmpAccessor =
                new RasterAccessor(tmp, tmpRect, formatTags[1],
                                   getColorModel());

        assert (dstAccessor.getDataType() == DataBuffer.TYPE_USHORT);

        ushortLoop(srcAccessor, tmpAccessor);
        Functions.copyData(dest, tmp);

        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    protected void ushortLoop(RasterAccessor src, RasterAccessor dst) {
        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstScanlineStride = dst.getScanlineStride()/3;

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcScanlineStride = src.getScanlineStride();

        short destData[] = dstDataArrays[0];
        short srcData[] = srcDataArrays[0];

        int srcOffset = srcBandOffsets[0];

        int rOffset = dstBandOffsets[0];
        int gOffset = dstBandOffsets[1];
        int bOffset = dstBandOffsets[2];

        int dWidth = dst.getWidth();
        int dHeight = dst.getHeight();

        DCRaw.interpolateGreen(srcData, destData, dWidth, dHeight, srcScanlineStride, dstScanlineStride, srcOffset, rOffset, gOffset, bOffset, gx, gy, ry );
        DCRaw.interpolateRedBlue(destData, dWidth, dHeight, dstScanlineStride, rOffset, gOffset, bOffset, rx, ry, bx, by );
    }
}
