/*
 * $RCSfile: TransposeBinaryOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:46 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PlanarImage;
import java.util.Map;
import com.lightcrafts.mediax.jai.JAI;

/**
 * An OpImage class to perform a transpose (flip) of an image with a
 * single 1-bit channel, represented using a
 * MultiPixelPackedSampleModel and byte, short, or int DataBuffer.
 *
 * @since 1.0.1
 * @see TransposeOpImage
 */
final class TransposeBinaryOpImage extends TransposeOpImage {
    
    // Force the SampleModel and ColorModel to be the same as for
    // the source image.
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            SampleModel sm,
                                            ColorModel cm) {
        ImageLayout newLayout;
        if (layout != null) {
            newLayout = (ImageLayout)layout.clone();
        } else {
            newLayout = new ImageLayout();
        }

        newLayout.setSampleModel(sm);
        newLayout.setColorModel(cm);

        return newLayout;
    }

    // Since this operation deals with packed binary data, we do not need
    // to expand the IndexColorModel
    private static Map configHelper(Map configuration) {

	Map config;

	if (configuration == null) {
	    config = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL,
					Boolean.FALSE);
	} else {
	    
	    config = configuration;

	    if (!(config.containsKey(JAI.KEY_REPLACE_INDEX_COLOR_MODEL))) {
		RenderingHints hints = (RenderingHints)configuration;
		config = (RenderingHints)hints.clone();
		config.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
	    }
	}

	return config;
    }
    
    /**
     * Constructs an TransposeBinaryOpImage from a RenderedImage source,
     * and Transpose type.  The image dimensions are determined by
     * forward-mapping the source bounds.
     * The tile grid layout, SampleModel, and ColorModel are specified
     * by the image source, possibly overridden by values from the
     * ImageLayout parameter.
     * 
     * @param source a RenderedImage.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param type the desired Tranpose type.
     */
    public TransposeBinaryOpImage(RenderedImage source,
                                   Map config,
                                   ImageLayout layout,
                                   int type) {
        super(source, 
	      configHelper(config),
              layoutHelper(layout,
                           source.getSampleModel(),
                           source.getColorModel()),
              type); 
   }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        Raster source = sources[0];

        MultiPixelPackedSampleModel mppsm =
            (MultiPixelPackedSampleModel)source.getSampleModel();
        int srcScanlineStride = mppsm.getScanlineStride();

        int incr1 = 0, incr2 = 0, s_x = 0, s_y = 0;

        int bits = 8;
        int dataType = source.getSampleModel().getDataType();
        if (dataType == DataBuffer.TYPE_USHORT) {
            bits = 16;
        } else if (dataType == DataBuffer.TYPE_INT) {
            bits = 32;
        }

        PlanarImage src = getSource(0);
        int sMinX = src.getMinX();
        int sMinY = src.getMinY();
        int sWidth = src.getWidth();
        int sHeight = src.getHeight();
        int sMaxX = sMinX + sWidth - 1;
        int sMaxY = sMinY + sHeight - 1;

        // Backwards map starting point of destination rectangle
        int[] pt = new int[2];
        pt[0] = destRect.x;
        pt[1] = destRect.y;
        mapPoint(pt, sMinX, sMinY, sMaxX, sMaxY, type, false);
        s_x = pt[0];
        s_y = pt[1];

        // Determine source stride along dest row (incr1) and column (incr2)
        switch (type) {
        case 0: // FLIP_VERTICAL
            incr1 = 1;
            incr2 = -bits*srcScanlineStride;
            break;

        case 1: // FLIP_HORIZONTAL
            incr1 = -1;
            incr2 = bits*srcScanlineStride;
            break;

        case 2: // FLIP_DIAGONAL;
            incr1 = bits*srcScanlineStride;
            incr2 = 1;
            break;

        case 3: // FLIP_ANTIDIAGONAL
            incr1 = -bits*srcScanlineStride;
            incr2 = -1;
            break;

        case 4: // ROTATE_90
            incr1 = -bits*srcScanlineStride;
            incr2 = 1;
            break;

        case 5: // ROTATE_180
            incr1 = -1;
            incr2 = -bits*srcScanlineStride;
            break;

        case 6: // ROTATE_270
            incr1 = bits*srcScanlineStride;
            incr2 = -1;
            break;
        }
 
        switch (source.getSampleModel().getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(source,
                     dest,
                     destRect,
                     incr1, incr2, s_x, s_y);
            break;
            
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_USHORT:
            shortLoop(source,
                      dest,
                      destRect,
                      incr1, incr2, s_x, s_y);
            break;

        case DataBuffer.TYPE_INT:
            intLoop(source,
                    dest,
                    destRect,
                    incr1, incr2, s_x, s_y);
            break;
        }
    }

    private void byteLoop(Raster source,
                          WritableRaster dest,
                          Rectangle destRect,
                          int incr1, int incr2, int s_x, int s_y) {
        MultiPixelPackedSampleModel sourceSM = 
            (MultiPixelPackedSampleModel)source.getSampleModel();
        DataBufferByte sourceDB =
            (DataBufferByte)source.getDataBuffer();
        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();

        MultiPixelPackedSampleModel destSM = 
            (MultiPixelPackedSampleModel)dest.getSampleModel();
        DataBufferByte destDB =
            (DataBufferByte)dest.getDataBuffer();
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        byte[] sourceData = sourceDB.getData();
        int sourceDBOffset = sourceDB.getOffset();

        byte[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        int dx = destRect.x;
        int dy = destRect.y;
        int dwidth = destRect.width;
        int dheight = destRect.height;

        int sourceOffset = 
            8*(s_y - sourceTransY)*sourceScanlineStride +
            8*sourceDBOffset +
            (s_x - sourceTransX) +
            sourceDataBitOffset;

        int destOffset = 
            8*(dy - destTransY)*destScanlineStride +
            8*destDBOffset +
            (dx - destTransX) +
            destDataBitOffset;

        for (int j = 0; j < dheight; j++) {
            int sOffset = sourceOffset;
            int dOffset = destOffset;
            int selement, val, dindex, delement;

            int i = 0;
            while ((i < dwidth) && ((dOffset & 7) != 0)) {
                selement = sourceData[sOffset >> 3];
                val = (selement >> (7 - (sOffset & 7))) & 0x1;

                dindex = dOffset >> 3;
                int dshift = 7 - (dOffset & 7);
                delement = destData[dindex];
                delement |= val << dshift;
                destData[dindex] = (byte)delement;

                sOffset += incr1;
                ++dOffset;
                ++i;
            }
        
            dindex = dOffset >> 3;
            if ((incr1 & 7) == 0) {
                //
                // We are stepping along Y in the source so the shift
                // position for each source pixel is fixed for the entire
                // destination scanline.
                //
                int shift = 7 - (sOffset & 7);
                int offset = sOffset >> 3;
                int incr = incr1 >> 3;

                while (i < dwidth - 7) {
                    selement = sourceData[offset];
                    val = (selement >> shift) & 0x1;
                    delement = val << 7;
                    offset += incr;
                    
                    selement = sourceData[offset];
                    val = (selement >> shift) & 0x1;
                    delement |= val << 6;
                    offset += incr;
                    
                    selement = sourceData[offset];
                    val = (selement >> shift) & 0x1;
                    delement |= val << 5;
                    offset += incr;
                    
                    selement = sourceData[offset];
                    val = (selement >> shift) & 0x1;
                    delement |= val << 4;
                    offset += incr;
                    
                    selement = sourceData[offset];
                    val = (selement >> shift) & 0x1;
                    delement |= val << 3;
                    offset += incr;
                    
                    selement = sourceData[offset];
                    val = (selement >> shift) & 0x1;
                    delement |= val << 2;
                    offset += incr;
                    
                    selement = sourceData[offset];
                    val = (selement >> shift) & 0x1;
                    delement |= val << 1;
                    offset += incr;
                    
                    selement = sourceData[offset];
                    val = (selement >> shift) & 0x1;
                    delement |= val;
                    offset += incr;
                    
                    destData[dindex] = (byte)delement;
                    
                    sOffset += 8*incr1;
                    dOffset += 8;
                    i += 8;
                    ++dindex;
                }
            } else {
                //
                // If we are here, incr1 must be 1 or -1.
                // There are further optimization opportunites here.
                //
                while (i < dwidth - 7) {
                    selement = sourceData[sOffset >> 3];
                    val = (selement >> (7 - (sOffset & 7))) & 0x1;
                    delement = val << 7;
                    sOffset += incr1;
                    
                    selement = sourceData[sOffset >> 3];
                    val = (selement >> (7 - (sOffset & 7))) & 0x1;
                    delement |= val << 6;
                    sOffset += incr1;
                    
                    selement = sourceData[sOffset >> 3];
                    val = (selement >> (7 - (sOffset & 7))) & 0x1;
                    delement |= val << 5;
                    sOffset += incr1;
                    
                    selement = sourceData[sOffset >> 3];
                    val = (selement >> (7 - (sOffset & 7))) & 0x1;
                    delement |= val << 4;
                    sOffset += incr1;
                    
                    selement = sourceData[sOffset >> 3];
                    val = (selement >> (7 - (sOffset & 7))) & 0x1;
                    delement |= val << 3;
                    sOffset += incr1;
                    
                    selement = sourceData[sOffset >> 3];
                    val = (selement >> (7 - (sOffset & 7))) & 0x1;
                    delement |= val << 2;
                    sOffset += incr1;
                    
                    selement = sourceData[sOffset >> 3];
                    val = (selement >> (7 - (sOffset & 7))) & 0x1;
                    delement |= val << 1;
                    sOffset += incr1;
                    
                    selement = sourceData[sOffset >> 3];
                    val = (selement >> (7 - (sOffset & 7))) & 0x1;
                    delement |= val;
                    sOffset += incr1;
                    
                    destData[dindex] = (byte)delement;
                    
                    dOffset += 8;
                    i += 8;
                    ++dindex;
                }
            }

            while (i < dwidth) {
                selement = sourceData[sOffset >> 3];
                val = (selement >> (7 - (sOffset & 7))) & 0x1;

                dindex = dOffset >> 3;
                int dshift = 7 - (dOffset & 7);
                delement = destData[dindex];
                delement |= val << dshift;
                destData[dindex] = (byte)delement;

                sOffset += incr1;
                ++dOffset;
                ++i;
            }

            sourceOffset += incr2;
            destOffset += 8*destScanlineStride;
        }
    }

    private void shortLoop(Raster source,
                           Raster dest,
                           Rectangle destRect,
                           int incr1, int incr2, int s_x, int s_y) {
        MultiPixelPackedSampleModel sourceSM = 
            (MultiPixelPackedSampleModel)source.getSampleModel();
        DataBufferUShort sourceDB =
            (DataBufferUShort)source.getDataBuffer();
        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();

        MultiPixelPackedSampleModel destSM = 
            (MultiPixelPackedSampleModel)dest.getSampleModel();
        DataBufferUShort destDB =
            (DataBufferUShort)dest.getDataBuffer();
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        short[] sourceData = sourceDB.getData();
        int sourceDBOffset = sourceDB.getOffset();

        short[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        int dx = destRect.x;
        int dy = destRect.y;
        int dwidth = destRect.width;
        int dheight = destRect.height;

        int sourceOffset = 
            16*(s_y - sourceTransY)*sourceScanlineStride +
            16*sourceDBOffset +
            (s_x - sourceTransX) +
            sourceDataBitOffset;

        int destOffset = 
            16*(dy - destTransY)*destScanlineStride +
            16*destDBOffset +
            (dx - destTransX) +
            destDataBitOffset;

        for (int j = 0; j < dheight; j++) {
            int sOffset = sourceOffset;
            int dOffset = destOffset;
            int selement, val, dindex, delement;

            int i = 0;
            while ((i < dwidth) && ((dOffset & 15) != 0)) {
                selement = sourceData[sOffset >> 4];
                val = (selement >> (15 - (sOffset & 15))) & 0x1;

                dindex = dOffset >> 4;
                int dshift = 15 - (dOffset & 15);
                delement = destData[dindex];
                delement |= val << dshift;
                destData[dindex] = (short)delement;

                sOffset += incr1;
                ++dOffset;
                ++i;
            }
        
            dindex = dOffset >> 4;
            if ((incr1 & 15) == 0) {
                int shift = 15 - (sOffset & 5);
                int offset = sOffset >> 4;
                int incr = incr1 >> 4;

                while (i < dwidth - 15) {
                    delement = 0;
                    for (int b = 15; b >= 0; b--) {
                        selement = sourceData[offset];
                        val = (selement >> shift) & 0x1;
                        delement |= val << b;
                        offset += incr;
                    }
                    
                    destData[dindex] = (short)delement;

                    sOffset += 16*incr1;
                    dOffset += 16;
                    i += 16;
                    ++dindex;
                }
            } else {
                while (i < dwidth - 15) {
                    delement = 0;
                    for (int b = 15; b >= 0; b--) {
                        selement = sourceData[sOffset >> 4];
                        val = (selement >> (15 - (sOffset & 15))) & 0x1;
                        delement |= val << b;
                        sOffset += incr1;
                    }
                    
                    destData[dindex] = (short)delement;

                    dOffset += 15;
                    i += 16;
                    ++dindex;
                }
            }

            while (i < dwidth) {
                selement = sourceData[sOffset >> 4];
                val = (selement >> (15 - (sOffset & 15))) & 0x1;

                dindex = dOffset >> 4;
                int dshift = 15 - (dOffset & 15);
                delement = destData[dindex];
                delement |= val << dshift;
                destData[dindex] = (short)delement;

                sOffset += incr1;
                ++dOffset;
                ++i;
            }

            sourceOffset += incr2;
            destOffset += 16*destScanlineStride;
        }
    }

    private void intLoop(Raster source,
                         Raster dest,
                         Rectangle destRect,
                         int incr1, int incr2, int s_x, int s_y) {
        MultiPixelPackedSampleModel sourceSM = 
            (MultiPixelPackedSampleModel)source.getSampleModel();
        DataBufferInt sourceDB =
            (DataBufferInt)source.getDataBuffer();
        int sourceTransX = source.getSampleModelTranslateX();
        int sourceTransY = source.getSampleModelTranslateY();
        int sourceDataBitOffset = sourceSM.getDataBitOffset();
        int sourceScanlineStride = sourceSM.getScanlineStride();

        MultiPixelPackedSampleModel destSM = 
            (MultiPixelPackedSampleModel)dest.getSampleModel();
        DataBufferInt destDB =
            (DataBufferInt)dest.getDataBuffer();
        int destMinX = dest.getMinX();
        int destMinY = dest.getMinY();
        int destTransX = dest.getSampleModelTranslateX();
        int destTransY = dest.getSampleModelTranslateY();
        int destDataBitOffset = destSM.getDataBitOffset();
        int destScanlineStride = destSM.getScanlineStride();

        int[] sourceData = sourceDB.getData();
        int sourceDBOffset = sourceDB.getOffset();

        int[] destData = destDB.getData();
        int destDBOffset = destDB.getOffset();

        int dx = destRect.x;
        int dy = destRect.y;
        int dwidth = destRect.width;
        int dheight = destRect.height;

        int sourceOffset = 
            32*(s_y - sourceTransY)*sourceScanlineStride +
            32*sourceDBOffset +
            (s_x - sourceTransX) +
            sourceDataBitOffset;

        int destOffset = 
            32*(dy - destTransY)*destScanlineStride +
            32*destDBOffset +
            (dx - destTransX) +
            destDataBitOffset;

        for (int j = 0; j < dheight; j++) {
            int sOffset = sourceOffset;
            int dOffset = destOffset;
            int selement, val, dindex, delement;

            int i = 0;
            while ((i < dwidth) && ((dOffset & 31) != 0)) {
                selement = sourceData[sOffset >> 5];
                val = (selement >> (31 - (sOffset & 31))) & 0x1;

                dindex = dOffset >> 5;
                int dshift = 31 - (dOffset & 31);
                delement = destData[dindex];
                delement |= val << dshift;
                destData[dindex] = (int)delement;

                sOffset += incr1;
                ++dOffset;
                ++i;
            }
        
            dindex = dOffset >> 5;
            if ((incr1 & 31) == 0) {
                int shift = 31 - (sOffset & 5);
                int offset = sOffset >> 5;
                int incr = incr1 >> 5;

                while (i < dwidth - 31) {
                    delement = 0;
                    for (int b = 31; b >= 0; b--) {
                        selement = sourceData[offset];
                        val = (selement >> shift) & 0x1;
                        delement |= val << b;
                        offset += incr;
                    }
                    
                    destData[dindex] = (int)delement;

                    sOffset += 32*incr1;
                    dOffset += 32;
                    i += 32;
                    ++dindex;
                }
            } else {
                while (i < dwidth - 31) {
                    delement = 0;
                    for (int b = 31; b >= 0; b--) {
                        selement = sourceData[sOffset >> 5];
                        val = (selement >> (31 - (sOffset & 31))) & 0x1;
                        delement |= val << b;
                        sOffset += incr1;
                    }
                    
                    destData[dindex] = (int)delement;

                    dOffset += 31;
                    i += 32;
                    ++dindex;
                }
            }

            while (i < dwidth) {
                selement = sourceData[sOffset >> 5];
                val = (selement >> (31 - (sOffset & 31))) & 0x1;

                dindex = dOffset >> 5;
                int dshift = 31 - (dOffset & 31);
                delement = destData[dindex];
                delement |= val << dshift;
                destData[dindex] = (int)delement;

                sOffset += incr1;
                ++dOffset;
                ++i;
            }

            sourceOffset += incr2;
            destOffset += 32*destScanlineStride;
        }
    }
}
