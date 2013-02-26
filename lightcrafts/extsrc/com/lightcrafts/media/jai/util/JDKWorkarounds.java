/*
 * $RCSfile: JDKWorkarounds.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:00 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.util;
import java.awt.*;
import java.awt.image.*;

//  Workaround Repository for JDK bugs.

public final class JDKWorkarounds {

    private JDKWorkarounds() {}

    /**
     * Faster implementation of setRect for bilevel Rasters
     * with a SinglePixelPackedSampleModel and DataBufferByte.
     * Based on sun.awt.image.BytePackedRaster.setDataElements
     * (JDK1.3 beta version), with improvements.
     */
    private static boolean setRectBilevel(WritableRaster dstRaster,
                                          Raster srcRaster,
                                          int dx, int dy) {
        int width  = srcRaster.getWidth();
        int height = srcRaster.getHeight();
        int srcOffX = srcRaster.getMinX();
        int srcOffY = srcRaster.getMinY();
        int dstOffX = dx+srcOffX;
        int dstOffY = dy+srcOffY;

        int dminX = dstRaster.getMinX();
        int dminY = dstRaster.getMinY();
        int dwidth = dstRaster.getWidth();
        int dheight = dstRaster.getHeight();

        // Clip to dstRaster
        if (dstOffX + width > dminX + dwidth) {
            width = dminX + dwidth - dstOffX;
        }
        if (dstOffY + height > dminY + dheight) {
            height = dminY + dheight - dstOffY;
        }

        //
        // This implementation works but is not as efficient as the one
        // below which is commented out. In terms of performance, cobbling
        // a 1728x2376 bit image with 128x144 tiles took the following
        // amount of time for four cases:
        //
        // WritableRaster.setRect() 19756
        // Aligned optimal case     5645
        // Unaligned optimal case   6644
        // Case using ImageUtil     7500
        //
        // So this case gives intermediate speed performance closer to the
        // optimal case than to the JDK. It will likely use more memory
        // however. On the other hand this approach covers all data types.
        //
        Rectangle rect = new Rectangle(dstOffX, dstOffY, width, height);
        byte[] binaryData = ImageUtil.getPackedBinaryData(srcRaster, rect);
        ImageUtil.setPackedBinaryData(binaryData, dstRaster, rect);

        /* XXX BEGIN: Commented out as it gives vertical lines in cobbling
           data. This gives optimal performance for the case of byte-to-byte
           data. For non-byte data the sub-optimal solution above (using
           the ImageUtil packed routines) should be used. Note that this
           commented out section includes a few bug fixes compared with the
           original code in the previous SCCS version. bpb 6/21/2000
        MultiPixelPackedSampleModel srcMPPSM =
            (MultiPixelPackedSampleModel)srcRaster.getSampleModel();
        MultiPixelPackedSampleModel dstMPPSM =
            (MultiPixelPackedSampleModel)dstRaster.getSampleModel();

        DataBufferByte srcDBB = (DataBufferByte)srcRaster.getDataBuffer();
        DataBufferByte dstDBB = (DataBufferByte)dstRaster.getDataBuffer();

        byte[] srcData = srcDBB.getData();
        byte[] dstData = dstDBB.getData();

        int srcTransX = srcRaster.getSampleModelTranslateX();
        int srcTransY = srcRaster.getSampleModelTranslateY();
        int srcDataBitOffset = srcMPPSM.getDataBitOffset();
        int srcScanlineStride = srcMPPSM.getScanlineStride();

        int srcYOffset = (srcOffY - srcTransY)*srcScanlineStride;
        int srcXOffset = srcDataBitOffset + (srcOffX - srcTransX);

        int dstTransX = dstRaster.getSampleModelTranslateX();
        int dstTransY = dstRaster.getSampleModelTranslateY();
        int dstDataBitOffset = dstMPPSM.getDataBitOffset();
        int dstScanlineStride = dstMPPSM.getScanlineStride();
        
        int dstYOffset = (dstOffY - dstTransY)*dstScanlineStride;
        int dstXOffset = dstDataBitOffset + (dstOffX - dstTransX);

        int inbit = srcYOffset*8 + srcXOffset;
        int outbit = dstYOffset*8 + dstXOffset;

        if ((inbit & 7) == (outbit & 7)) {
            // Aligned case
            int copybits = width;
            int bits = inbit & 7;
            if (bits != 0) {
                // Copy partial bytes on left
                int inbyte = inbit >> 3;
                int outbyte = outbit >> 3;
                int mask = 0xff >> bits;
                bits = 8 - bits;
                if (copybits < bits) {
                    mask &= (mask << (8 - copybits));
                    bits = copybits;
                }
                for (int j = 0; j < height; j++) {
                    int element = dstData[outbyte];
                    element &= ~mask;
                    element |= (srcData[inbyte] & mask);
                    dstData[outbyte] = (byte) element;
                    inbyte += srcScanlineStride;
                    outbyte += dstScanlineStride;
                }
                inbit += bits;
                outbit += bits;
                copybits -= bits;
            }
            if (copybits >= 8) {
                // Copy whole bytes
                int inbyte = inbit >> 3;
                int outbyte = outbit >> 3;
                int copybytes = copybits >> 3;
                
                if (copybytes == srcScanlineStride &&
                    srcScanlineStride == dstScanlineStride) {
                    System.arraycopy(srcData, inbyte,
                                     dstData, outbyte,
                                     srcScanlineStride*height);
                } else {
                    for (int j = 0; j < height; j++) {
                        System.arraycopy(srcData, inbyte,
                                         dstData, outbyte,
                                         copybytes);
                        inbyte += srcScanlineStride;
                        outbyte += dstScanlineStride;
                    }
                }
                bits = copybytes * 8;
                inbit += bits;
                outbit += bits;
                copybits -= bits;
            }
            if (copybits > 0) {
                // Copy partial bytes on right
                int inbyte = inbit >> 3;
                int outbyte = outbit >> 3;
                int mask = (0xff00 >> copybits) & 0xff;
                for (int j = 0; j < height; j++) {
                    int element = dstData[outbyte];
                    element &= ~mask;
                    element |= (srcData[inbyte] & mask);
                    dstData[outbyte] = (byte) element;
                    inbyte += srcScanlineStride;
                    outbyte += dstScanlineStride;
                }
            }
        } else {
            // Unaligned case
            for (int j = 0; j < height; j++) {
                int save_inbit = inbit;
                int save_outbit = outbit;
                int copybits = width;

                int inbyte, outbyte;
                int mask;
                
                int bits = outbit & 7;
                if (bits > 0) {
                    inbyte = inbit >> 8;
                    outbyte = outbit >> 8;
                    mask = 0xff >> bits;

                    if (copybits < bits) {
                        mask &= mask << (8 - copybits);
                        bits = copybits;
                    }
                    int element = dstData[outbyte];
                    element &= ~mask;
                    element |= (srcData[inbyte] & mask);
                    dstData[outbyte] = (byte) element;

                    inbit += bits;
                    outbit += bits;
                    copybits -= bits;
                }

                if (copybits == 0) {
                    continue;
                }

                int shift0 = inbit & 7;
                int shift1 = 7 - shift0;
                int mask1 = 0xff >>> shift1;

                inbyte = inbit >> 3;
                outbyte = outbit >> 3;

                int srcData0 = srcData[inbyte];
                int lastIndex = srcData.length - 1;
                while (copybits >= 8 && inbyte < lastIndex) {
                    int srcData1 = srcData[inbyte + 1];
                    int val = (srcData0 << shift0) |
                        ((srcData1 >>> shift1) & mask1);
                    srcData0 = srcData1;
                    dstData[outbyte] = (byte)val;
                    
                    ++inbyte;
                    ++outbyte;
                    inbit += 8;
                    outbit += 8;
                    copybits -= 8;
                }

                if (copybits > 0) {
                    mask = (0xff00 >> copybits) & 0xff;

                    int element = dstData[outbyte];
                    element &= ~mask;
                    element |= ((srcData[inbyte] << shift0) & mask);
                    dstData[outbyte] = (byte)(element & 0xFF);
                }

                inbit = save_inbit + 8*srcScanlineStride;
                outbit = save_outbit + 8*dstScanlineStride;
            }
        }
        XXX END */

        return true;
    }

    // Workarounds for WritableRaster.setRect bug (4250270) in JDK 1.2.
    // Also filed as bug 4250273 against JAI.

    public static void setRect(WritableRaster dstRaster, Raster srcRaster) {
        setRect(dstRaster, srcRaster, 0, 0);
    }

    public static void setRect(WritableRaster dstRaster, Raster srcRaster,
                        int dx, int dy) {
        // Special case for bilevel Rasters
        SampleModel srcSampleModel = srcRaster.getSampleModel();
        SampleModel dstSampleModel = dstRaster.getSampleModel();
        if (srcSampleModel instanceof MultiPixelPackedSampleModel &&
            dstSampleModel instanceof MultiPixelPackedSampleModel) {
            MultiPixelPackedSampleModel srcMPPSM =
                (MultiPixelPackedSampleModel)srcSampleModel;
            MultiPixelPackedSampleModel dstMPPSM =
                (MultiPixelPackedSampleModel)dstSampleModel;

            DataBuffer srcDB = srcRaster.getDataBuffer();
            DataBuffer dstDB = srcRaster.getDataBuffer();

            if (srcDB instanceof DataBufferByte &&
                dstDB instanceof DataBufferByte &&
                srcMPPSM.getPixelBitStride() == 1 &&
                dstMPPSM.getPixelBitStride() == 1) {
                if (setRectBilevel(dstRaster, srcRaster, dx, dy)) {
                    return;
                }
            }
        }

        // Use the regular JDK routines for everything else except
        // float and double images.
        int dataType = dstRaster.getSampleModel().getDataType();
        if (dataType != DataBuffer.TYPE_FLOAT &&
            dataType != DataBuffer.TYPE_DOUBLE) {
            dstRaster.setRect(dx, dy, srcRaster);
            return;
        }

        int width  = srcRaster.getWidth();
        int height = srcRaster.getHeight();
        int srcOffX = srcRaster.getMinX();
        int srcOffY = srcRaster.getMinY();
        int dstOffX = dx+srcOffX;
        int dstOffY = dy+srcOffY;

        int dminX = dstRaster.getMinX();
        int dminY = dstRaster.getMinY();
        int dwidth = dstRaster.getWidth();
        int dheight = dstRaster.getHeight();

        // Clip to dstRaster
        if (dstOffX + width > dminX + dwidth) {
            width = dminX + dwidth - dstOffX;
        }
        if (dstOffY + height > dminY + dheight) {
            height = dminY + dheight - dstOffY;
        }

        switch (srcRaster.getSampleModel().getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_INT:
            int[] iData = null;    
            for (int startY=0; startY < height; startY++) {
                // Grab one scanline at a time
                iData =
                    srcRaster.getPixels(srcOffX, srcOffY+startY, width, 1,
                                        iData);
                dstRaster.setPixels(dstOffX, dstOffY+startY, width, 1, iData);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
            float[] fData = null;    
            for (int startY=0; startY < height; startY++) {
                fData =
                    srcRaster.getPixels(srcOffX, srcOffY+startY, width, 1,
                                        fData);
                dstRaster.setPixels(dstOffX, dstOffY+startY, width, 1, fData);
            }
            break;

        case DataBuffer.TYPE_DOUBLE:
            double[] dData = null;    
            for (int startY=0; startY < height; startY++) {
                // Grab one scanline at a time
                dData =
                    srcRaster.getPixels(srcOffX, srcOffY+startY, width, 1,
                                        dData);
                dstRaster.setPixels(dstOffX, dstOffY+startY, width, 1, dData);
            }
            break;
        }
    }

    /**
     * Workaround for JDK 1.3 bug 4326636 (bpb 30 March 2000).
     *
     * Check whether the given SampleModel and ColorModel are compatible.
     *
     * This is required because in JDK 1.3 the ComponentColorModel
     * implementation of isCompatibleSampleModel() only checks whether
     * the SampleModel is a ComponentSampleModel with the same transferType
     * as the ColorModel. No check of the number of components or bit
     * depth is effected.
     *
     * @throws IllegalArgumentException if either parameter is null.
     */
    public static boolean areCompatibleDataModels(SampleModel sm,
                                                  ColorModel cm) {
        if(sm == null || cm == null) {
            throw new
                IllegalArgumentException(JaiI18N.getString("JDKWorkarounds0"));
        }

        // Call the method we should be using instead of this workaround.
        // This checks the compatibility of the transferType and possibly
        // other quantities.
        if(!cm.isCompatibleSampleModel(sm)) {
            return false;
        }

        // This if-block adds the tests performed in
        // ComponentColorModel.isCompatibleRaster() but not in
        // ComponentColorModel.isCompatibleSampleModel().
        // These tests might duplicate the implementation of some
        // subclasses of ComponentColorModel.
        if(cm instanceof ComponentColorModel) {
            // Check the number of samples per pixel.
            int numBands = sm.getNumBands();
            if (numBands != cm.getNumComponents()) {
                return false;
            }

            // Check adequate depth. This should work for
            // FloatDoubleColorModel as well because
            // SampleModel.getSampleSize() should return 32 or 64 as
            // it gets the size from the DataBuffer object and
            // ColorModel.getComponentSize() returns the number of bits
            // which are set to 32 or 64 as a function of the transferType.
            for (int b = 0; b < numBands; b++) {
                if (sm.getSampleSize(b) < cm.getComponentSize(b)) {
                    return false;
                }
            }
        }

        // Got this far so return true.
        return true;
    }
}
