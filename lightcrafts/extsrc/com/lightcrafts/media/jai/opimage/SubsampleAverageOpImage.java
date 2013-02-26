/*
 * $RCSfile: SubsampleAverageOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.3 $
 * $Date: 2005/08/26 23:51:40 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.InterpAverage;

public class SubsampleAverageOpImage extends GeometricOpImage {
    /* XXX
    public static void main(String[] args) throws Throwable {
        com.lightcrafts.mediax.jai.PlanarImage source =
            com.lightcrafts.mediax.jai.JAI.create("fileload", args[0]);
        double scaleX = args.length > 1 ?
            Double.valueOf(args[1]).doubleValue() : 0.25;
        double scaleY = args.length > 2 ?
            Double.valueOf(args[2]).doubleValue() : scaleX;

        source.getTiles();

        com.lightcrafts.mediax.jai.PlanarImage dest =
            new SubsampleAverageOpImage(source, null, null,
                                        scaleX, scaleY);
        long t1 = System.currentTimeMillis();
        dest.getTiles();
        long t2 = System.currentTimeMillis();
        System.out.println("Java time = "+(t2 - t1));

        com.lightcrafts.mediax.jai.PlanarImage destML =
            new MlibSubsampleAverageOpImage(source, null, null,
                                            scaleX, scaleY);
        long t3 = System.currentTimeMillis();
        destML.getTiles();
        long t4 = System.currentTimeMillis();
        System.out.println("Mlib time = "+(t4 - t3));

        RenderedImage diff = com.lightcrafts.mediax.jai.JAI.create("subtract",
                                   com.lightcrafts.mediax.jai.JAI.create("format", dest,
                                                              DataBuffer.TYPE_SHORT),
                                   com.lightcrafts.mediax.jai.JAI.create("format", destML,
                                                              DataBuffer.TYPE_SHORT));
        RenderedImage absDiff = com.lightcrafts.mediax.jai.JAI.create("absolute", diff);
        double[] maxima =
            (double[])com.lightcrafts.mediax.jai.JAI.create("extrema", absDiff).getProperty("maximum");
        for(int i = 0; i < maxima.length; i++) {
            System.out.println(maxima[i]);
        }

        System.out.println(source.getClass().getName()+": "+
                           new ImageLayout(source));
        System.out.println(dest.getClass().getName()+": "+
                           new ImageLayout(dest));
        System.out.println(destML.getClass().getName()+": "+
                           new ImageLayout(destML));

        java.awt.Frame frame = new java.awt.Frame("Mlib Sub-average Test");
        frame.setLayout(new java.awt.GridLayout(1, 2));
        com.lightcrafts.mediax.jai.widget.ScrollingImagePanel ps =
            new com.lightcrafts.mediax.jai.widget.ScrollingImagePanel(dest,
                                                           512, 512);
        com.lightcrafts.mediax.jai.widget.ScrollingImagePanel pd =
            new com.lightcrafts.mediax.jai.widget.ScrollingImagePanel(destML,
                                                           512, 512);
        frame.add(ps);
        frame.add(pd);
        frame.pack();
        frame.show();
    }
    */

    /** The horizontal scale factor. */
    protected double scaleX;

    /** The vertical scale factor. */
    protected double scaleY;

    /** Horizontal size of an averaging block. */
    protected int blockX;

    /** Vertical size of an averaging block. */
    protected int blockY;

    /** Source image minimum x coordinate. */
    protected int sourceMinX;

    /** Source image minimum y coordinate. */
    protected int sourceMinY;

    private static ImageLayout layoutHelper(RenderedImage source,
                                            double scaleX,
                                            double scaleY,
                                            ImageLayout il) {

        if(scaleX <= 0.0 || scaleX > 1.0) {
            throw new IllegalArgumentException
                (JaiI18N.getString("SubsampleAverageOpImage0"));
        } else if(scaleY <= 0.0 || scaleY > 1.0) {
            throw new IllegalArgumentException
                (JaiI18N.getString("SubsampleAverageOpImage1"));
        }

        ImageLayout layout = (il == null) ?
            new ImageLayout() : (ImageLayout)il.clone();

        layout.setMinX((int)Math.floor(source.getMinX()*scaleX));
        layout.setMinY((int)Math.floor(source.getMinY()*scaleY));
        layout.setWidth((int)(source.getWidth()*scaleX));
        layout.setHeight((int)(source.getHeight()*scaleY));

        return layout;
    }

    public SubsampleAverageOpImage(RenderedImage source,
                                   ImageLayout layout,
                                   Map config,
                                   double scaleX,
                                   double scaleY) {
        super(vectorize(source),
              layoutHelper(source, scaleX, scaleY, layout),
              config,
              true, // cobbleSources,
	      null, // BorderExtender
              new InterpAverage((int)Math.ceil(1.0/scaleX),
                                (int)Math.ceil(1.0/scaleY)),
              null);

        this.scaleX = scaleX;
        this.scaleY = scaleY;

        this.blockX = (int)Math.ceil(1.0/scaleX);
        this.blockY = (int)Math.ceil(1.0/scaleY);

        this.sourceMinX = source.getMinX();
        this.sourceMinY = source.getMinY();
    }

    public Point2D mapDestPoint(Point2D destPt) {
        if(destPt == null) {
            throw new IllegalArgumentException("destPt == null!");
        }

        Point2D pt = (Point2D)destPt.clone();
        pt.setLocation(sourceMinX + (destPt.getX() + 0.5 - minX)/scaleX - 0.5,
                       sourceMinY + (destPt.getY() + 0.5 - minY)/scaleY - 0.5);

        return pt;
    }

    public Point2D mapSourcePoint(Point2D sourcePt) {
        if(sourcePt == null) {
            throw new IllegalArgumentException("sourcePt == null!");
        }

        Point2D pt = (Point2D)sourcePt.clone();
        pt.setLocation(minX +
                       (sourcePt.getX() + 0.5 - sourceMinX)*scaleX - 0.5,
                       minY +
                       (sourcePt.getY() + 0.5 - sourceMinY)*scaleY - 0.5);

        return pt;
    }

    protected Rectangle backwardMapRect(Rectangle destRect,
                                        int sourceIndex) {
        if(destRect == null) {
            throw new IllegalArgumentException
                (JaiI18N.getString("Generic0"));
        } else if(sourceIndex != 0) {
            throw new IllegalArgumentException
                (JaiI18N.getString("Generic1"));
        }

        // Map the upper left pixel.
        Point2D p1 = mapDestPoint(new Point2D.Double(destRect.x,
                                                     destRect.y));

        // Map the lower right pixel.
        Point2D p2 =
            mapDestPoint(new Point2D.Double(destRect.x + destRect.width - 1,
                                            destRect.y + destRect.height - 1));

        // Determine the integral positions.
        int x1 = (int)Math.floor(p1.getX());
        int y1 = (int)Math.floor(p1.getY());
        int x2 = (int)Math.floor(p2.getX());
        int y2 = (int)Math.floor(p2.getY());

        // Return rectangle based on integral positions.
        return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    protected Rectangle forwardMapRect(Rectangle sourceRect,
                                       int sourceIndex) {
        if(sourceRect == null) {
            throw new IllegalArgumentException
                (JaiI18N.getString("Generic0"));
        } else if(sourceIndex != 0) {
            throw new IllegalArgumentException
                (JaiI18N.getString("Generic1"));
        }

        // Map the upper left pixel.
        Point2D p1 = mapSourcePoint(new Point2D.Double(sourceRect.x,
                                                       sourceRect.y));

        // Map the lower right pixel.
        Point2D p2 =
            mapSourcePoint(new Point2D.Double(sourceRect.x +
                                              sourceRect.width - 1,
                                              sourceRect.y +
                                              sourceRect.height - 1));

        // Determine the integral positions.
        int x1 = (int)Math.floor(p1.getX());
        int y1 = (int)Math.floor(p1.getY());
        int x2 = (int)Math.floor(p2.getX());
        int y2 = (int)Math.floor(p2.getY());

        // Return rectangle based on integral positions.
        return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    /**
     * Performs a subsampling operation on a specified rectangle.
     * The sources are cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster  containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    protected void computeRect(Raster [] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Get RasterAccessor tags (initialized in OpImage superclass).
        RasterFormatTag[] formatTags = getFormatTags();

        // Get destination accessor.
        RasterAccessor dst = new RasterAccessor(dest, destRect,  
                                                formatTags[1],
                                                getColorModel());

        // Backward map destination rectangle to source and clip to the
        // source image bounds (mapDestRect() does not clip automatically).
        Rectangle srcRect =
            mapDestRect(destRect, 0).intersection(sources[0].getBounds());

        // Get source accessor.
        RasterAccessor src =
            new RasterAccessor(sources[0],
                               srcRect,
                               formatTags[0], 
                               getSourceImage(0).getColorModel());

        switch(dst.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(src, dst);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(src, dst);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(src, dst);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(src, dst);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(src, dst);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(src, dst);
            break;
        default:
            throw new RuntimeException
                (JaiI18N.getString("Generic3"));
        }

        // If the RasterAccessor set up a temporary write buffer for the
        // operator, tell it to copy that data to the destination Raster.
        if (dst.isDataCopy()) {
            dst.clampDataArrays();
            dst.copyDataToRaster();
        }
    }

    private void computeRectByte(RasterAccessor src,
                                 RasterAccessor dst) {
        // Get dimensions.
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        byte[][] dstDataArrays = dst.getByteDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        // Get source data array references and strides.
        byte[][] srcDataArrays = src.getByteDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Compute scaled source strides.
        int[] srcPixelStrideScaled = new int[dwidth];
	for (int i = 0; i < dwidth; i++)
            srcPixelStrideScaled[i] = 
		(int)Math.floor(i/scaleX)*srcPixelStride;

        int[] srcScanlineStrideScaled = new int[dheight];
	for (int i = 0; i < dheight; i++)
            srcScanlineStrideScaled[i] =
		(int)Math.floor(i/scaleY)*srcScanlineStride;

        // Cache the product of the block dimensions.
        float denom = blockX*blockY;

        for (int k = 0; k < dnumBands; k++)  {
            byte[] dstData = dstDataArrays[k];
            byte[] srcData = srcDataArrays[k];
            int srcScanlineOffset0 = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
	    int srcScanlineOffset = srcScanlineOffset0;

            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset0 = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
		int srcPixelOffset = srcPixelOffset0;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageVerticalOffset = srcPixelOffset;
 
                    // Average the source over the scale-dependent window.
                    int sum = 0;
                    for (int u = 0; u < blockY; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < blockX; v++)  {
                            sum += (int)(srcData[imageOffset]&0xff);
                            imageOffset += srcPixelStride;
                        }
                        imageVerticalOffset += srcScanlineStride;
                    }
 
                    dstData[dstPixelOffset] =
                        ImageUtil.clampRoundByte(sum / denom);

                    srcPixelOffset = srcPixelOffset0 + srcPixelStrideScaled[i];
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset = srcScanlineOffset0 + srcScanlineStrideScaled[j];
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void computeRectUShort(RasterAccessor src,
                                   RasterAccessor dst) {
        // Get dimensions.
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        short[][] dstDataArrays = dst.getShortDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        // Get source data array references and strides.
        short[][] srcDataArrays = src.getShortDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Compute scaled source strides.
        int[] srcPixelStrideScaled = new int[dwidth];
        for (int i = 0; i < dwidth; i++)
            srcPixelStrideScaled[i] =
                (int)Math.floor(i/scaleX)*srcPixelStride;
            
        int[] srcScanlineStrideScaled = new int[dheight];
        for (int i = 0; i < dheight; i++)
            srcScanlineStrideScaled[i] =
                (int)Math.floor(i/scaleY)*srcScanlineStride;
            
        // Cache the product of the block dimensions.
        float denom = blockX*blockY;

        for (int k = 0; k < dnumBands; k++)  {
            short[] dstData = dstDataArrays[k];
            short[] srcData = srcDataArrays[k];
            int srcScanlineOffset0 = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            int srcScanlineOffset = srcScanlineOffset0;

            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset0 = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                int srcPixelOffset = srcPixelOffset0;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageVerticalOffset = srcPixelOffset;
 
                    // Average the source over the scale-dependent window.
                    long sum = 0;
                    for (int u = 0; u < blockY; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < blockX; v++)  {
                            sum += (long)(srcData[imageOffset]&0xffff);
                            imageOffset += srcPixelStride;
                        }
                        imageVerticalOffset += srcScanlineStride;
                    }
 
                    dstData[dstPixelOffset] =
                        ImageUtil.clampRoundUShort(sum / denom);

               	    srcPixelOffset = srcPixelOffset0 + srcPixelStrideScaled[i];
               	    dstPixelOffset += dstPixelStride;
           	}    
           	srcScanlineOffset = srcScanlineOffset0 + srcScanlineStrideScaled[j];
           	dstScanlineOffset += dstScanlineStride;
	    }
        }
    }

    private void computeRectShort(RasterAccessor src,
                                  RasterAccessor dst) {
        // Get dimensions.
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        short[][] dstDataArrays = dst.getShortDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        // Get source data array references and strides.
        short[][] srcDataArrays = src.getShortDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Compute scaled source strides.
        int[] srcPixelStrideScaled = new int[dwidth];
        for (int i = 0; i < dwidth; i++)
            srcPixelStrideScaled[i] =
                (int)Math.floor(i/scaleX)*srcPixelStride;
            
        int[] srcScanlineStrideScaled = new int[dheight];
        for (int i = 0; i < dheight; i++)
            srcScanlineStrideScaled[i] =
                (int)Math.floor(i/scaleY)*srcScanlineStride;
            
        // Cache the product of the block dimensions.
        float denom = blockX*blockY;

        for (int k = 0; k < dnumBands; k++)  {
            short[] dstData = dstDataArrays[k];
            short[] srcData = srcDataArrays[k];
            int srcScanlineOffset0 = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            int srcScanlineOffset = srcScanlineOffset0;

            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset0 = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                int srcPixelOffset = srcPixelOffset0;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageVerticalOffset = srcPixelOffset;
 
                    // Average the source over the scale-dependent window.
                    long sum = 0;
                    for (int u = 0; u < blockY; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < blockX; v++)  {
                            sum += srcData[imageOffset];
                            imageOffset += srcPixelStride;
                        }
                        imageVerticalOffset += srcScanlineStride;
                    }
 
                    dstData[dstPixelOffset] =
                        ImageUtil.clampRoundShort(sum / denom);

               	    srcPixelOffset = srcPixelOffset0 + srcPixelStrideScaled[i];
                    dstPixelOffset += dstPixelStride;
               }    
               srcScanlineOffset = srcScanlineOffset0 + srcScanlineStrideScaled[j];
               dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void computeRectInt(RasterAccessor src,
                                RasterAccessor dst) {
        // Get dimensions.
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        int[][] dstDataArrays = dst.getIntDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        // Get source data array references and strides.
        int[][] srcDataArrays = src.getIntDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Compute scaled source strides.
        int[] srcPixelStrideScaled = new int[dwidth];
        for (int i = 0; i < dwidth; i++)
            srcPixelStrideScaled[i] =
                (int)Math.floor(i/scaleX)*srcPixelStride;
            
        int[] srcScanlineStrideScaled = new int[dheight];
        for (int i = 0; i < dheight; i++)
            srcScanlineStrideScaled[i] =
                (int)Math.floor(i/scaleY)*srcScanlineStride;
            
        // Cache the product of the block dimensions.
        float denom = blockX*blockY;

        for (int k = 0; k < dnumBands; k++)  {
            int[] dstData = dstDataArrays[k];
            int[] srcData = srcDataArrays[k];
            int srcScanlineOffset0 = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            int srcScanlineOffset = srcScanlineOffset0;

            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset0 = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                int srcPixelOffset = srcPixelOffset0;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageVerticalOffset = srcPixelOffset;
 
                    // Average the source over the scale-dependent window.
                    double sum = 0;
                    for (int u = 0; u < blockY; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < blockX; v++)  {
                            sum += srcData[imageOffset];
                            imageOffset += srcPixelStride;
                        }
                        imageVerticalOffset += srcScanlineStride;
                    }
 
                    dstData[dstPixelOffset] =
                        ImageUtil.clampRoundInt(sum / denom);

               	    srcPixelOffset = srcPixelOffset0 + srcPixelStrideScaled[i];
               	    dstPixelOffset += dstPixelStride;
           	}    
                srcScanlineOffset = srcScanlineOffset0 + srcScanlineStrideScaled[j];
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void computeRectFloat(RasterAccessor src,
                                  RasterAccessor dst) {
        // Get dimensions.
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        float[][] dstDataArrays = dst.getFloatDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        // Get source data array references and strides.
        float[][] srcDataArrays = src.getFloatDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Compute scaled source strides.
        int[] srcPixelStrideScaled = new int[dwidth];
        for (int i = 0; i < dwidth; i++)
            srcPixelStrideScaled[i] =
                (int)Math.floor(i/scaleX)*srcPixelStride;
            
        int[] srcScanlineStrideScaled = new int[dheight];
        for (int i = 0; i < dheight; i++)
            srcScanlineStrideScaled[i] =
                (int)Math.floor(i/scaleY)*srcScanlineStride;
            
        // Cache the product of the block dimensions.
        float denom = blockX*blockY;

        for (int k = 0; k < dnumBands; k++)  {
            float[] dstData = dstDataArrays[k];
            float[] srcData = srcDataArrays[k];
            int srcScanlineOffset0 = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            int srcScanlineOffset = srcScanlineOffset0;

            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset0 = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                int srcPixelOffset = srcPixelOffset0;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageVerticalOffset = srcPixelOffset;
 
                    // Average the source over the scale-dependent window.
                    double sum = 0;
                    for (int u = 0; u < blockY; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < blockX; v++)  {
                            sum += srcData[imageOffset];
                            imageOffset += srcPixelStride;
                        }
                        imageVerticalOffset += srcScanlineStride;
                    }
 
                    dstData[dstPixelOffset] =
                        ImageUtil.clampFloat(sum / denom);

               	    srcPixelOffset = srcPixelOffset0 + srcPixelStrideScaled[i];
                    dstPixelOffset += dstPixelStride;
           	}    
           	srcScanlineOffset = srcScanlineOffset0 + srcScanlineStrideScaled[j];
           	dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void computeRectDouble(RasterAccessor src,
                                   RasterAccessor dst) {
        // Get dimensions.
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        double[][] dstDataArrays = dst.getDoubleDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        // Get source data array references and strides.
        double[][] srcDataArrays = src.getDoubleDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Compute scaled source strides.
        int[] srcPixelStrideScaled = new int[dwidth];
        for (int i = 0; i < dwidth; i++)
            srcPixelStrideScaled[i] =
                (int)Math.floor(i/scaleX)*srcPixelStride;
            
        int[] srcScanlineStrideScaled = new int[dheight];
        for (int i = 0; i < dheight; i++)
            srcScanlineStrideScaled[i] =
                (int)Math.floor(i/scaleY)*srcScanlineStride;
            
        // Cache the product of the block dimensions.
        double denom = blockX*blockY;

        for (int k = 0; k < dnumBands; k++)  {
            double[] dstData = dstDataArrays[k];
            double[] srcData = srcDataArrays[k];
            int srcScanlineOffset0 = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            int srcScanlineOffset = srcScanlineOffset0;

            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset0 = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                int srcPixelOffset = srcPixelOffset0;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageVerticalOffset = srcPixelOffset;
 
                    // Average the source over the scale-dependent window.
                    double sum = 0;
                    for (int u = 0; u < blockY; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < blockX; v++)  {
                            sum += srcData[imageOffset];
                            imageOffset += srcPixelStride;
                        }
                        imageVerticalOffset += srcScanlineStride;
                    }
 
                    dstData[dstPixelOffset] = sum / denom;

               	    srcPixelOffset = srcPixelOffset0 + srcPixelStrideScaled[i];
               	    dstPixelOffset += dstPixelStride;
           	}    
           	srcScanlineOffset = srcScanlineOffset0 + srcScanlineStrideScaled[j];
           	dstScanlineOffset += dstScanlineStride;
            }
        }
    }
}
