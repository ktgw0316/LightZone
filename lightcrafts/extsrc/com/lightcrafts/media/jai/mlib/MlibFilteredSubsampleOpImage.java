/*
 * $RCSfile: MlibFilteredSubsampleOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:56 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import java.util.Map;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.media.jai.opimage.FilteredSubsampleOpImage;
import com.sun.medialib.mlib.*;

/**
 * <p> A class extending <code>FilteredSubsampleOpImage</code> to
 * subsample and antialias filter images using medialib.
 *
 * @see FilteredSubsampleOpImage
 */
final class MlibFilteredSubsampleOpImage extends FilteredSubsampleOpImage {

    protected double [] m_hKernel;
    protected double [] m_vKernel;

    private static final boolean DEBUG = false;

   /** <p> <code>MlibFilteredSubsampleOpImage</code> constructs an OpImage representing
     * filtered integral subsampling.  The scale factors represent the ratio of
     * source to destination dimensions.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param config a Map object possibly holding tile cache information
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *	  SampleModel, and ColorModel, or null.
     * @param interp a Interpolation object to use for resampling.
     * @param scaleX downsample factor along x axis.
     * @param scaleY downsample factor along y axis.
     * @param qsFilter symmetric filter coefficients (partial kernel).
     * @throws IllegalArgumentException if the interp type is not one of:
     *    INTERP_NEAREST, INTERP_BILINEAR, INTERP_BICUBIC, or INTERP_BICUBIC_2
     */
    public MlibFilteredSubsampleOpImage(RenderedImage source,
    				    BorderExtender extender,
				    Map config,
				    ImageLayout layout,
                                    int scaleX,
				    int scaleY,
				    float [] qsFilter,
				    Interpolation interp) {

        // Propagate to FilteredSubsampleOpImage constructor
        super(source,
              extender,
              config,
              layout,
              scaleX,
              scaleY,
              qsFilter,
              interp);

        // If enabled, print debug information
	if (DEBUG) System.out.println("Object: MlibFilteredSubsampleOpImage");

        // Copy floating arrays to medialib double arrays
        m_hKernel = new double [hKernel.length];
	m_vKernel = new double [vKernel.length];
        if (DEBUG) System.out.print("\n hParity: " + hParity + " hKernel: ");
        for (int i=0 ; i<hKernel.length ; i++) {
           m_hKernel[i] = (double)hKernel[i];
           if (DEBUG) System.out.print(" " + m_hKernel[i]);
        } // for i
        if (DEBUG) System.out.print("\n vParity: " + vParity + " vKernel: ");
	for (int j=0 ; j<vKernel.length ; j++) {
           m_vKernel[j] = (double)vKernel[j];
           if (DEBUG) System.out.print(" " + m_vKernel[j]);
        } // for j
        if (DEBUG) System.out.println("\n");

    } // MlibFilteredSubsampleOpImage
 
    /**
     * <p> Performs a combined subsample/filter operation on a specified rectangle.
     * The sources are cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster  containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
    public void computeRect(Raster [] sources,
                               WritableRaster dest,
                               Rectangle destRect) {

        // Get RasterAccessor tag
        int formatTag = MediaLibAccessor.findCompatibleTag(sources, dest);

        // Get destination accessor.
        MediaLibAccessor dst = new MediaLibAccessor(dest, destRect,  
                                                    formatTag);

        // Get source accessor.
        MediaLibAccessor src = new MediaLibAccessor(sources[0],
                                                    mapDestRect(destRect, 0),
                                                    formatTag);

        // Medialib requires translation terms (java version doesn't)
	int transX = m_hKernel.length - (scaleX + 1)/2 - (hParity*(1+scaleX))%2;
	int transY = m_vKernel.length - (scaleY + 1)/2 - (vParity*(1+scaleY))%2;
	mediaLibImage srcML[], dstML[];
        switch (dst.getDataType()) {
          case DataBuffer.TYPE_BYTE:
          case DataBuffer.TYPE_USHORT:
          case DataBuffer.TYPE_SHORT:
          case DataBuffer.TYPE_INT:
            srcML = src.getMediaLibImages();
            dstML = dst.getMediaLibImages();
            for (int i = 0 ; i < dstML.length; i++) {
                Image.FilteredSubsample(dstML[i], srcML[i],
                                        scaleX, scaleY,
                                        transX, transY,
                                        m_hKernel, m_vKernel,
                                        hParity, vParity,
                                        Constants.MLIB_EDGE_DST_NO_WRITE);
            } // for
            break;
          case DataBuffer.TYPE_FLOAT:
          case DataBuffer.TYPE_DOUBLE:
            srcML = src.getMediaLibImages();
            dstML = dst.getMediaLibImages();
            for (int i = 0 ; i < dstML.length; i++) {
                Image.FilteredSubsample_Fp(dstML[i], srcML[i],
                                           scaleX, scaleY,
                                           transX, transY,
                                           m_hKernel, m_vKernel,
                                           hParity, vParity,
                                           Constants.MLIB_EDGE_DST_NO_WRITE);
            } // for
            break;
          default:
            throw new IllegalArgumentException(
	        JaiI18N.getString("Generic2"));
        } // switch getDataType

        // If the RasterAccessor set up a temporary write buffer for the
        // operator, tell it to copy that data to the destination Raster.
        if (dst.isDataCopy()) {
            dst.clampDataArrays();
            dst.copyDataToRaster();
        } // if isDataCopy

    }  // computeRect

} // class MlibFilteredSubsampleOpImage
