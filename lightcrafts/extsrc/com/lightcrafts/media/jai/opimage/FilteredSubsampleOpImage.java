/*
 * $RCSfile: FilteredSubsampleOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:26 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;

import com.lightcrafts.mediax.jai.ImageLayout;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.InterpolationBilinear;
import com.lightcrafts.mediax.jai.InterpolationBicubic;
import com.lightcrafts.mediax.jai.InterpolationBicubic2;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * <p> A class extending <code>GeometricOpImage</code> to
 * subsample and antialias filter images.  Image scaling operations
 * require rectilinear backwards mapping and padding by the resampling
 * and filter dimensions.
 *
 * <p> When applying scale factors of scaleX, scaleY to a source image
 * with width of src_width and height of src_height, the resulting image
 * is defined to have the following bounds:
 *
 * <code></pre>
 *       dst minX  = round(src minX  / scaleX) <br>
 *       dst minY  = round(src minY  / scaleY) <br>
 *       dst width  =  round(src width  / scaleX) <br>
 *       dst height =  round(src height / scaleY) <br>
 * </pre></code>
 *
 * <p> The applied filter is quadrant symmetric (typically antialias + resample). The
 * filter is product-separable, quadrant symmetric, and is defined by half of its
 * span. For example, if the input filter, qsFilter, was of size 3, it would have
 * width and height 5 and have the symmetric form:
 *   qs[2] qs[1] qs[0] qs[1] qs[2]
 * Because we have chosen to keep the filters in compact form we need
 * to keep track of parity.
 *
 * <p> A fully expanded 5 by 5 kernel has format (25 entries defined by
 * only 3 entries):
 *
 *   <code>
 *   <p align=center> qs[2]*qs[2]  qs[2]*qs[1]  qs[2]*qs[0]  qs[2]*qs[1]  qs[2]*qs[2] <br>
 *
 *                    qs[1]*qs[2]  qs[1]*qs[1]  qs[1]*qs[0]  qs[1]*qs[1]  qs[1]*qs[2] <br>
 *
 *                    qs[0]*qs[2]  qs[0]*qs[1]  qs[0]*qs[0]  qs[0]*qs[1]  qs[0]*qs[2] <br>
 *
 *                    qs[1]*qs[2]  qs[1]*qs[1]  qs[1]*qs[0]  qs[1]*qs[1]  qs[1]*qs[2] <br>
 *
 *                    qs[2]*qs[2]  qs[2]*qs[1]  qs[2]*qs[0]  qs[2]*qs[1]  qs[2]*qs[2]
 *   </p> </code>
 *
 * <p> Horizontal and vertical kernels representing convolved resample and qsFilter
 * kernels are computed from the input filter, the resample type, and because the
 * downsample factors affect resample weights, the downsample scale factors.  If the
 * scale factors are odd, then the resample kernel is unity.  Parity is used to
 * signify whether the symmetric kernel has a double center (even parity) or a
 * single center value (odd parity).
 *
 * <p> This operator is similar to the image scale operator.  Important
 * differences are described here.  The coordinate transformation differences
 * between the FilteredDownsampleOpImage and the ScaleOpImage operators can be
 * understood by comparing their mapping equations directly.
 *
 * <p> For the scale operator, the destination (D) to source (S) mapping
 * equations are given by
 *
 * <code>
 *   <p> xS = (xD - xTrans)/xScale <br>
 *       yS = (yD - yTrans)/yScale
 * </code>
 *
 * <p> The scale and translation terms are floating point values in D-frame
 * pixel units.  For scale this means that one S pixel maps to xScale
 * by yScale D-frame pixels.  The translation vector, (xTrans, yTrans),
 * is in D-frame pixel units.
 *
 * <p> The filtered downsample operator mapping equations are given by
 *
 * <code>
 *   <p> xS = xD*scaleX + (int)(hKernel.length/2) <br>
 *   yS = yD*scaleY + (int)(vKernel.length/2)
 * </code>
 *
 * <p> The mapping equations have the intended property that the convolution
 * kernel overlays the upper left source pixels for the upper left destination
 * pixel.
 *
 * <p> The downsample terms are restricted to positive integral values.
 * Geometrically, one D-frame pixel maps to scaleX by scaleY S-frame
 * pixels.  The combination of downsampling and filtering has performance
 * benefits over sequential operator usage in part due to the symmetry
 * constraints imposed by only allowing integer parameters for scaling and
 * only allowing separable symmetric filters.  With odd scale factors, D-frame
 * pixels map directly onto S-frame pixel centers.  With even scale factors,
 * D-frame pixels map squarely between S-frame pixel centers.  Below are
 * examples of even, odd, and combination cases.
 *
 *   <p>  s = S-frame pixel centers <br>
 *        d = D-frame pixel centers mapped to S-frame
 *   </p>
 *   <kbd>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>   d       d       d  </pre>
 *   <pre> s   s   s   s   s   s           s   d   s   s   d   s  </pre>
 *   <pre>  </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>   d       d       d  </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>  </pre>
 *   <pre> s   s   s   s   s   s           s   d   s   s   d   s  </pre>
 *   <pre>   d       d       d  </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>  </pre>
 *   <pre> Even scaleX/Y factors            Odd scaleX/Y factors  </pre>
 *   <pre>   </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>     d           d    </pre>
 *   <pre> s   s   s   s   s   s           s d s   s d s   s d s  </pre>
 *   <pre>   </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>     d           d    </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>   </pre>
 *   <pre> s   s   s   s   s   s           s d s   s d s   s d s  </pre>
 *   <pre>     d           d    </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>   </pre>
 * <pre>  Odd/even scaleX/Y factors      Even/odd scaleX/Y factors  </pre> <br>
 *   </kbd>
 *
 * <p> The convolution kernel is restricted to have quadrant symmetry (qs). This
 * type of symmetry is also product separable.  The qsFilter is specified by
 * a floating array.  If qsFilter[0], qsFilter[1], ... , qsFilter[qsFilter.len() - 1]
 * is the filter input, then the entire separable kernel is given by <br>
 *   qsFilter[qsFilter.len() - 1], ... , qsFilter[0], ... , qsFilter[qsFilter.len() - 1] <br>
 *
 * <p> The restriction of integer parameter constraints allows full product
 * separablity and symmetry when applying the combined resample and filter
 * convolution operations.
 *
 * <p> If Bilinear or Bicubic interpolation is specified, the source needs
 * to be extended such that it has the extra pixels needed to compute all
 * the destination pixels. This extension is performed via the
 * <code>BorderExtender</code> class. The type of border extension can be
 * specified as a <code>RenderingHint</code> to the <code>JAI.create</code>
 * method.
 *
 * <p> If no <code>BorderExtender</code> is specified, the source will
 * not be extended.  The scaled image size is still calculated
 * according to the formula specified above. However since there is not
 * enough source to compute all the destination pixels, only that
 * subset of the destination image's pixels which can be computed,
 * will be written in the destination. The rest of the destination
 * will be set to zeros.
 *
 * <p> The current implementation of this operator does not support
 * <code>MultiPixelPackedSampleModel</code> source data.  The Rendered Image
 * Factory for this operator, <code>FilteredSubsampleRIF</code>, will throw an
 * <code>IllegalArgumentException</code> for this type of input.
 *
 * @see GeometricOpImage
 */
public class FilteredSubsampleOpImage extends GeometricOpImage {

    /** <p> The horizontal downsample factor. */
    protected int scaleX;

    /** <p> The vertical downsample factor. */
    protected int scaleY;

    /** <p> Horizontal filter parity.  Rules: 0 => even, 1 => odd.  See hKernel */
    protected int hParity;

    /** <p> Vertical filter parity.  Rules: 0 => even, 1 => odd.  See vKernel */
    protected int vParity;

    /** <p> Compact form of combined resample and antialias filters
     *  used by computeRect method.  hKernel is the horizontal kernel.
     *
     *  <p> The symmetric filter is applied even or odd depending on filter
     *  parity.
     *
     *  <p> Expanded even kernel example (<code>hParity = 0</code>): <br>
     *  <code>
     *      hKernel[2] hKernel[1] hKernel[0] hKernel[0] hKernel[1] hKernel[2]
     *  </code>
     *
     *  <p> Expanded odd kernel example (<code>hParity = 1</code>): <br>
     *  <code>
     *      hKernel[2] hKernel[1] hKernel[0] hKernel[1] hKernel[2]
     *  </code>
     */
    protected float [] hKernel;

    /** <p> Compact form of combined resample and antialias filters
     *  used by computeRect method.  vKernel is the vertical kernel.
     *
     *  <p> The symmetric filter is applied even or odd depending on filter
     *  parity.
     *
     *  <p> Expanded even kernel example (<code>vParity = 0</code>): <br>
     *  <code>
     *      vKernel[2] vKernel[1] vKernel[0] vKernel[0] vKernel[1] vKernel[2]
     *  </code>
     *
     *  <p> Expanded odd kernel example (<code>vParity = 1</code>): <br>
     *  <code>
     *      vKernel[2] vKernel[1] vKernel[0] vKernel[1] vKernel[2]
     *  </code>
     */
    protected float [] vKernel;

    static final int numProc = Runtime.getRuntime().availableProcessors();

    /** <p> <code>convolveFullKernels</code> -- convolve two kernels and return the
     * result in a floating array.
     *
     * @param a floating kernel array.
     * @param b floating kernel array.
     * @return floating kernel array representing a*b (full convolution)
     */
    private static float [] convolveFullKernels(float [] a, float [] b) {
        int lenA = a.length;
        int lenB = b.length;
        float [] c = new float [lenA + lenB - 1];

        for (int k = 0 ; k < c.length ; k++)
            for (int j = Math.max(0, k-lenB+1) ; j<=Math.min(k, lenA-1) ; j++)
                c[k] += a[j] * b[k - j];

        return c;
    } // convolveFullKernels

    /** <p> <code>convolveSymmetricKernels</code> uses a symmetric representation
     * (partial kernels) of input and output kernels.  For example, with
     * aParity 1 (odd) and bParity 0 (even) the passed kernels a and b would
     * have form:
     * <code>
     * a:  a[lenA-1] ... a[1] a[0] a[1] ... a[lenA-1]
     * b:  b[lenB-1] ... b[1] b[0] b[0] b[1] ... b[lenB-1]
     * </code>
     *
     * (i.e., don't send symmetric parts but assumes parity controls filter
     * lengths).
     *
     * <p> It is possible to do this convolution without resorting to full
     * kernels but this is messy.  @see convolveFullKernels for details.
     *
     * <p> Further notes:
     * 1. The return kernel, <code>c</code>, has parity
     *    <code>1 + aParity + bParity mod 2</code>
     * 2. The reason for setting up the kernels this way is to enforce symmetry
     *    constraints.  (Design choice.)
     *
     * @param aParity int that is 0 or 1.
     * @param bParity int that is 0 or 1.
     * @param a floating partial kernel array.
     * @param b floating partial kernel array.
     * @return symmetric portion of floating array representing a*b (convolution).
     */
    private static float [] convolveSymmetricKernels(int aParity,
                                                     int bParity,
                                                     float [] a,
                                                     float [] b) {
        int lenA = a.length;
        int lenB = b.length;
        int lenTmpA = 2*lenA - aParity;
        int lenTmpB = 2*lenB - bParity;
        int lenTmpC = lenTmpA + lenTmpB - 1;
        float [] tmpA = new float [lenTmpA];
        float [] tmpB = new float [lenTmpB];
        float [] tmpC;
        float [] c = new float [(lenTmpC + 1)/2];

        // Construct "full" a
        for (int k=0 ; k<lenTmpA ; k++)
            tmpA[k] = a[Math.abs(k - lenA + (aParity - 1)*(k/lenA)+ 1)];

        // Construct "full" b
        for (int k=0 ; k<lenTmpB ; k++)
            tmpB[k] =  b[Math.abs(k - lenB + (bParity - 1)*(k/lenB) + 1)];

        // Convolve "full" a with "full" b to get a "full" tempC
        tmpC = convolveFullKernels(tmpA,tmpB);

        // Carve out and return the portion of c that holds
        int cParity = tmpC.length%2;
        for (int k=0 ; k<c.length ; k++)
            c[k] = tmpC[lenTmpC - c.length - k - 1 + cParity];

        return c;

    } // convolveSymmetricKernels

    /** <p> <code>combineFilters</code> based on <code>resampleType</code> and
     * input partial <code>qsFilter</code> (see above for details on qsFilter format).
     * Input <code>qsFilter</code> is restricted to have odd parity
     * (<code>qsFilter[0]</code> is at the center of the kernel).
     *
     * @param scaleFactor positive int representing the downsample factor.
     * @param resampleType int representing the interpolation type.
     * @param qsFilter floating partial kernel array (antialias filter).
     * @return floating partial kernel representing combined resample and qsFilter.
     */
    private static float [] combineFilters(int scaleFactor,
                                           int resampleType,
                                           float [] qsFilter) {

        // Odd scale factors imply no resample filter is required
        // return pointer to the qsFilter
        if ((scaleFactor%2) == 1) return qsFilter.clone();

        int qsParity = 1;
        int resampParity = 0; // Unless nearest neighbor case is selected (ignored)

        switch (resampleType) {
            case Interpolation.INTERP_NEAREST: // Return a copy of the qsFilter
               return qsFilter.clone();
            case Interpolation.INTERP_BILINEAR: // 2 by 2 resample filter
               float [] bilinearKernel = { 1.0F/2.0F };
                return convolveSymmetricKernels(
                        qsParity, resampParity, qsFilter, bilinearKernel);
            case Interpolation.INTERP_BICUBIC: // 4 by 4 resample filter
               float [] bicubicKernel = { 9.0F/16.0F, -1.0F/16.0F };
               return convolveSymmetricKernels(
                       qsParity, resampParity, qsFilter, bicubicKernel);
            case Interpolation.INTERP_BICUBIC_2: // alternate 4 by 4 resample filter
               float [] bicubic2Kernel = { 5.0F/8.0F, -1.0F/8.0F };
               return convolveSymmetricKernels(
                       qsParity, resampParity, qsFilter, bicubic2Kernel);
           default:
               throw new IllegalArgumentException(
                       JaiI18N.getString("FilteredSubsample0"));
        }


    } // combineFilters


    /** <p> <code>filterParity</code> -- Returns combined filter/resample parity.
     * This is odd only when we have an odd scale factor or we have nearest neighbor
     * filtering (no resample kernel needed).  <code>scaleFactor</code> was validated
     * by the constructor.  Possible return values are 0 or 1.
     *
     * @param scaleFactor positive int representing the downsample factor.
     * @param resampleType int representing the interpolation type.
     * @return int representing combined filter parity (0 or 1).
     */
    private static int filterParity(int scaleFactor, int resampleType) {

        // Test scale factor for oddness or nearest neighbor resampling
        if ((scaleFactor%2 == 1) ||
                (resampleType == Interpolation.INTERP_NEAREST)) return 1;

        // for all other cases we will be convolving an odd filter with an even
        // filter, thus producing an even filter
        return 0;

    } // filterParity

    /** <p> <code>layoutHelper</code> validates input and returns an
     *  <code>ImageLayout</code> object.
     *
     * @param source a RenderedImage object.
     * @param interp an Interpolation object.
     * @param scaleX an int downsample factor.
     * @param scaleY an int downsample factor.
     * @param filterSize an int representing the size of the combined
     *        filter and resample kernel.
     * @param il an ImageLayout object.
     * @return validated ImageLayout object.
     */
    private static ImageLayout layoutHelper(RenderedImage source,
                                            Interpolation interp,
                                            int scaleX,
                                            int scaleY,
                                            int filterSize,
                                            ImageLayout il) {

        if (scaleX < 1 || scaleY < 1 ) {
            throw new IllegalArgumentException(
                    JaiI18N.getString("FilteredSubsample1"));
        }
        if (filterSize < 1) {
            throw new IllegalArgumentException(
                    JaiI18N.getString("FilteredSubsample2"));
        }

        // Set the bounds to the scaled source bounds.
        Rectangle bounds =
            forwardMapRect(source.getMinX(), source.getMinY(),
                           source.getWidth(), source.getHeight(),
                           scaleX, scaleY);

        // If the user has supplied a layout, use it
        ImageLayout layout = (il == null) ?
            new ImageLayout(bounds.x, bounds.y, bounds.width, bounds.height) :
                           (ImageLayout)il.clone();

        // Override dimensions if user passed a hint
        if (il != null) {
            layout.setWidth(bounds.width);
            layout.setHeight(bounds.height);
            layout.setMinX(bounds.x);
            layout.setMinY(bounds.y);
        }

        return layout;

    } // setLayout

   /** <p> <code>FilteredSubsampleOpImage</code> constructs an OpImage representing
     * filtered integral subsampling.  The scale factors represent the ratio of
     * source to destination dimensions.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param config a Map object possibly holding tile cache information
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *         SampleModel, and ColorModel, or null.
     * @param interp a Interpolation object to use for resampling.
     * @param scaleX downsample factor along x axis.
     * @param scaleY downsample factor along y axis.
     * @param qsFilter symmetric filter coefficients (partial kernel).
     * @throws IllegalArgumentException if the interp type is not one of:
     *    INTERP_NEAREST, INTERP_BILINEAR, INTERP_BICUBIC, or INTERP_BICUBIC_2
     */
    public FilteredSubsampleOpImage(RenderedImage source,
                                    BorderExtender extender,
                                    Map config,
                                    ImageLayout layout,
                                    int scaleX,
                                    int scaleY,
                                    float [] qsFilter,
                                    Interpolation interp) {

        // Propagate to GeometricOpImage constructor
        super(vectorize(source),
                layoutHelper(source, interp, scaleX, scaleY, qsFilter.length, layout),
                config,   // Map object
                true,     // cobbleSources,
                extender, // extender
                interp,    // Interpolation object
                null);

        int resampleType;

        // Determine the interpolation type, if not supported throw exception
        if (interp instanceof InterpolationNearest) {
            resampleType = Interpolation.INTERP_NEAREST;
        } else if (interp instanceof InterpolationBilinear) {
            resampleType = Interpolation.INTERP_BILINEAR;
        } else if (interp instanceof InterpolationBicubic) {
            resampleType = Interpolation.INTERP_BICUBIC;
        } else if (interp instanceof InterpolationBicubic2) {
            resampleType = Interpolation.INTERP_BICUBIC_2;
        } else {
            throw new IllegalArgumentException(
                    JaiI18N.getString("FilteredSubsample3"));
        }

        // Construct combined anti-alias and resample kernels.
        this.hParity = filterParity(scaleX,resampleType);
        this.vParity = filterParity(scaleY,resampleType);
        this.hKernel = combineFilters(scaleX,resampleType,qsFilter);
        this.vKernel = combineFilters(scaleY,resampleType,qsFilter);

        this.scaleX = scaleX;
        this.scaleY = scaleY;

    } // FilteredSubsampleOpImage

    /**
     * Computes the source point corresponding to the supplied point.
     *
     * @param destPt the position in destination image coordinates
     * to map to source image coordinates.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>destPt</code>.
     *
     * @throws IllegalArgumentException if <code>destPt</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapDestPoint(Point2D destPt) {
        if (destPt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Point2D pt = (Point2D)destPt.clone();

        pt.setLocation(destPt.getX()*scaleX, destPt.getY()*scaleY);

        return pt;
    }

    /**
     * Computes the destination point corresponding to the supplied point.
     *
     * @param sourcePt the position in source image coordinates
     * to map to destination image coordinates.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>sourcePt</code>.
     *
     * @throws IllegalArgumentException if <code>sourcePt</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Point2D pt = (Point2D)sourcePt.clone();

        pt.setLocation(sourcePt.getX()/scaleX, sourcePt.getY()/scaleY);

        return pt;
    }

    /**
     * <p> Returns a conservative estimate of the destination region that
     * can potentially be affected by the pixels of a rectangle of a
     * given source.
     *
     * @param sourceRect The <code>Rectangle</code> in source coordinates.
     * @param sourceIndex The index of the source image.
     *
     * @return a <code>Rectangle</code> indicating the potentially
     *         affected destination region, or <code>null</code> if
     *         the region is unknown.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws NullPointerException if <code>sourceRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect,
                                   int sourceIndex) {
        if (sourceIndex != 0) { // this image only has one source
            throw new IllegalArgumentException(JaiI18N.getString("FilteredSubsample4"));
        }

        int xOffset = sourceRect.x + hKernel.length - hParity - scaleX/2;
        int yOffset = sourceRect.y + vKernel.length - vParity - scaleY/2;
        int rectWidth = sourceRect.width - 2*hKernel.length + hParity + 1;
        int rectHeight = sourceRect.height - 2*vKernel.length + vParity + 1;
        return forwardMapRect(xOffset, yOffset, rectWidth, rectHeight,
                              scaleX, scaleY);

    } // mapSourceRect

    /** <p> Forward map a source Rectangle into destination space.
     *
     * @param x source frame coordinate.
     * @param y source frame coordinate.
     * @param w source frame width.
     * @param h source frame height.
     * @param scaleX downsample factor.
     * @param scaleY downsample factor.
     * @return a <code>Rectangle</code> indicating the destination region.
     */
    private static Rectangle forwardMapRect(int x, int y, int w, int h,
                                            int scaleX, int scaleY) {
        float sx = 1.0F/scaleX;
        float sy = 1.0F/scaleY;

        x = Math.round(x*sx);
        y = Math.round(y*sy);

        return new Rectangle(x, y,
                             Math.round((x + w)*sx) - x,
                             Math.round((y + h)*sy) - y);
    } // forwardMapRect

    /** <p> Forward map a source Rectangle into destination space.
     *  Required by abstract GeometricOpImage
     *
     * @param srcRect a source Rectangle.
     * @param srcIndex int source index (0 for this operator)
     * @return a <code>Rectangle</code> indicating the destination region.
     */
    protected final Rectangle forwardMapRect(Rectangle srcRect,
                                             int srcIndex) {
        int x = srcRect.x;
        int y = srcRect.y;
        int w = srcRect.width;
        int h = srcRect.height;
        float sx = 1.0F/scaleX;
        float sy = 1.0F/scaleY;

        x = Math.round(x*sx);
        y = Math.round(y*sy);

        return new Rectangle(x, y,
                             Math.round((x + w)*sx) - x,
                             Math.round((y + h)*sy) - y);
    } // forwardMapRect

    /** <p> Backward map a destination Rectangle into source space.
     *
     * @param destRect a destination Rectangle.
     * @param srcIndex int source index (0 for this operator)
     * @return a <code>Rectangle</code> indicating the source region.
     */
    protected final Rectangle backwardMapRect(Rectangle destRect,
                                              int srcIndex) {
        int x = destRect.x;
        int y = destRect.y;
        int w = destRect.width;
        int h = destRect.height;

        return new Rectangle(x*scaleX, y*scaleY,
                             (x + w)*scaleX - x,
                             (y + h)*scaleY - y);
    } // backwardMapRect


    /**
     * <p> Returns a conservative estimate of the region of a specified
     * source that is required in order to compute the pixels of a
     * given destination rectangle.
     *
     * @param destRect The <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex The index of the source image.
     *
     * @return a <code>Rectangle</code> indicating the required source region.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws NullPointerException if <code>destRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapDestRect(Rectangle destRect,
                                 int sourceIndex) {
        if (sourceIndex != 0) { // this image only has one source
            throw new IllegalArgumentException(
                    JaiI18N.getString("FilteredSubsample4"));
        }
        int xOffset = destRect.x*scaleX - hKernel.length + hParity + scaleX/2;
        int yOffset = destRect.y*scaleY - vKernel.length + vParity + scaleY/2;
        int rectWidth = destRect.width*scaleX + 2*hKernel.length - hParity - 1;
        int rectHeight = destRect.height*scaleY + 2*vKernel.length - vParity - 1;
        return new Rectangle(xOffset, yOffset, rectWidth, rectHeight);

    } // mapDestRect

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

        // Get RasterAccessor tags (initialized in OpImage superclass).
        RasterFormatTag[] formatTags = getFormatTags();

        // Get destination accessor.
        RasterAccessor dst = new RasterAccessor(dest, destRect,
                                                formatTags[1],
                                                getColorModel());

        // Get source accessor.
        RasterAccessor src = new RasterAccessor(sources[0],
                                                mapDestRect(destRect, 0),
                                                formatTags[0],
                                                getSourceImage(0).getColorModel());

        switch (dst.getDataType()) {
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
            throw new IllegalArgumentException(
                    JaiI18N.getString("FilteredSubsample5"));
        }

        // If the RasterAccessor set up a temporary write buffer for the
        // operator, tell it to copy that data to the destination Raster.
        if (dst.isDataCopy()) {
            dst.clampDataArrays();
            dst.copyDataToRaster();
        }

    }  // computeRect

    /** <code>computeRectByte</code> filter subsamples byte pixel data.
     *
     * @param src RasterAccessor for source image.
     * @param dst RasterAccessor for output image.
     */
    protected void computeRectByte(RasterAccessor src, RasterAccessor dst) {

        // Get dimensions.
        final int dwidth = dst.getWidth();
        final int dheight = dst.getHeight();
        final int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        final byte dstDataArrays[][] = dst.getByteDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Get source data array references and strides.
        final byte srcDataArrays[][] = src.getByteDataArrays();
        final int srcBandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        // Compute reused numbers
        final int kernelNx = 2*hKernel.length - hParity;
        final int kernelNy = 2*vKernel.length - vParity;
        final int stepDown = (kernelNy - 1)*srcScanlineStride;
        final int stepRight = (kernelNx - 1)*srcPixelStride;

        final float vCtr = vKernel[0];
        final float hCtr = hKernel[0];

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dnumBands; band++) {
            final byte dstData[] = dstDataArrays[band];
            final byte srcData[] = srcDataArrays[band];
            final int srcScanlineOffset = srcBandOffsets[band];
            final int dstScanlineOffset = dstBandOffsets[band];

            // Step over source raster coordinates
            for (int h = 0; h < dheight; h++) {
                final int y = h;
                final int ySrc = h * scaleY;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dInd = dstScanlineOffset + y * dstScanlineStride;
                        for (int xSrc = 0; xSrc < scaleX*dwidth; xSrc += scaleX) {

                            int upLeft0  = xSrc*srcPixelStride + ySrc*srcScanlineStride + srcScanlineOffset;
                            int upRight0 = upLeft0 + stepRight;
                            int dnLeft0  = upLeft0 + stepDown;
                            int dnRight0 = upRight0 + stepDown;

                            // Exploit 4-fold symmetry
                            float sum = 0;

                            // Make the rectangle squeeze in the vertical direction
                            for (int iy = vKernel.length - 1; iy > vParity - 1; iy--) {
                                int upLeft  = upLeft0;
                                int upRight = upRight0;
                                int dnLeft  = dnLeft0;
                                int dnRight = dnRight0;

                                // Make the rectangle squeeze in the horizontal direction
                                for (int ix = hKernel.length - 1; ix > hParity - 1; ix--) {
                                    float kk = hKernel[ix]*vKernel[iy];
                                    sum += kk*((srcData[upLeft] &0xff) +
                                            (srcData[upRight]&0xff) +
                                            (srcData[dnLeft] &0xff) +
                                            (srcData[dnRight]&0xff));
                                    upLeft  += srcPixelStride;
                                    upRight -= srcPixelStride;
                                    dnLeft  += srcPixelStride;
                                    dnRight -= srcPixelStride;
                                } // ix
                                upLeft0  += srcScanlineStride;   // down a row
                                upRight0 += srcScanlineStride;
                                dnLeft0  -= srcScanlineStride;   // up a row
                                dnRight0 -= srcScanlineStride;
                            } // iy

                            // Compute the remaining 2-Fold symmetry portions (across and down as needed)

                            // Loop down the center (hParity is odd)
                            if (hParity == 1) {
                                int xUp = (xSrc + hKernel.length - 1)*srcPixelStride +
                                        ySrc*srcScanlineStride + srcScanlineOffset;
                                int xDown = xUp + stepDown;
                                int kInd = vKernel.length - 1;
                                while (xUp < xDown) {
                                    float kk = hCtr*vKernel[kInd--];
                                    sum += kk*((srcData[xUp]&0xff) +
                                            (srcData[xDown]&0xff));
                                    xUp   += srcScanlineStride;
                                    xDown -= srcScanlineStride;
                                }
                            } // hParity

                            // Loop across the center (vParity is odd), pick up the center if hParity was odd
                            if (vParity == 1) {
                                int xLeft = xSrc*srcPixelStride +
                                        (ySrc + vKernel.length - 1)*srcScanlineStride +
                                        srcScanlineOffset;
                                int xRight = xLeft + stepRight;
                                int kInd = hKernel.length - 1;
                                while (xLeft < xRight) {
                                    float kk = vCtr*hKernel[kInd--];
                                    sum += kk*((srcData[xLeft]&0xff) +
                                            (srcData[xRight]&0xff));
                                    xLeft  += srcPixelStride;
                                    xRight -= srcPixelStride;
                                } // while xLeft

                                // Grab the center pixel if hParity was odd also
                                if (hParity == 1) sum += vCtr*hCtr* (srcData[xLeft]&0xff);

                            } // if vParity

                            // Convert the sum to an output pixel
                            if (sum < 0.0)   sum = 0;
                            if (sum > 255.0) sum = 255;

                            dstData[dInd] = (byte)(sum + 0.5);

                            dInd += dstPixelStride;
                        } // for xSrc

                        return null;
                    }
                });
            } // for ySrc
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    } // computeRectByte

    /** <code>computeRectUShort</code> filter subsamples unsigned short pixel data.
     *
     * @param src RasterAccessor for source image.
     * @param dst RasterAccessor for output image.
     */
    protected void computeRectUShort(RasterAccessor src, RasterAccessor dst) {

        // Get dimensions.
        final int dwidth = dst.getWidth();
        final int dheight = dst.getHeight();
        final int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        final short dstDataArrays[][] = dst.getShortDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Get source data array references and strides.
        final short srcDataArrays[][] = src.getShortDataArrays();
        final int srcBandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        // Compute reused numbers
        final int kernelNx = 2*hKernel.length - hParity;
        final int kernelNy = 2*vKernel.length - vParity;
        final int stepDown = (kernelNy - 1)*srcScanlineStride;
        final int stepRight = (kernelNx - 1)*srcPixelStride;

        final float vCtr = vKernel[0];
        final float hCtr = hKernel[0];

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dnumBands; band++) {
            final short dstData[] = dstDataArrays[band];
            final short srcData[] = srcDataArrays[band];
            final int srcScanlineOffset = srcBandOffsets[band];
            final int dstScanlineOffset = dstBandOffsets[band];

            // Step over source raster coordinates
            for (int h = 0; h < dheight; h++) {
                final int y = h;
                final int ySrc = h * scaleY;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dInd = dstScanlineOffset + y * dstScanlineStride;
                        for (int xSrc = 0; xSrc < scaleX*dwidth; xSrc += scaleX) {

                            int upLeft0  = xSrc*srcPixelStride + ySrc*srcScanlineStride + srcScanlineOffset;
                            int upRight0 = upLeft0 + stepRight;
                            int dnLeft0  = upLeft0 + stepDown;
                            int dnRight0 = upRight0 + stepDown;

                            // Exploit 4-fold symmetry
                            float sum = 0;

                            // Make the rectangle squeeze in the vertical direction
                            for (int iy = vKernel.length - 1; iy > vParity - 1; iy--) {
                                int upLeft  = upLeft0;
                                int upRight = upRight0;
                                int dnLeft  = dnLeft0;
                                int dnRight = dnRight0;

                                // Make the rectangle squeeze in the horizontal direction
                                for (int ix = hKernel.length - 1; ix > hParity - 1; ix--) {
                                    float kk = hKernel[ix]*vKernel[iy];
                                    sum += kk*((srcData[upLeft] &0xffff) +
                                            (srcData[upRight]&0xffff) +
                                            (srcData[dnLeft] &0xffff) +
                                            (srcData[dnRight]&0xffff));
                                    upLeft  += srcPixelStride;
                                    upRight -= srcPixelStride;
                                    dnLeft  += srcPixelStride;
                                    dnRight -= srcPixelStride;
                                } // ix
                                upLeft0  += srcScanlineStride;   // down a row
                                upRight0 += srcScanlineStride;
                                dnLeft0  -= srcScanlineStride;   // up a row
                                dnRight0 -= srcScanlineStride;
                            } // iy

                            // Compute the remaining 2-Fold symmetry portions
                            // (across and down as needed)

                            // Loop down the center (hParity is odd)
                            if (hParity == 1) {
                                int xUp = (xSrc + hKernel.length - 1)*srcPixelStride +
                                        ySrc*srcScanlineStride + srcScanlineOffset;
                                int xDown = xUp + stepDown;
                                int kInd = vKernel.length - 1;
                                while (xUp < xDown) {
                                    float kk = hCtr*vKernel[kInd--];
                                    sum += kk*((srcData[xUp]  &0xffff) +
                                            (srcData[xDown]&0xffff));
                                    xUp   += srcScanlineStride;
                                    xDown -= srcScanlineStride;
                                }
                            } // hParity

                            // Loop across the center (vParity is odd), pick up the center if hParity was odd
                            if (vParity == 1) {
                                int xLeft = xSrc*srcPixelStride +
                                        (ySrc + vKernel.length - 1)*srcScanlineStride +
                                        srcScanlineOffset;
                                int xRight = xLeft + stepRight;
                                int kInd = hKernel.length - 1;
                                while (xLeft < xRight) {
                                    float kk = vCtr*hKernel[kInd--];
                                    sum += kk*((srcData[xLeft] &0xffff) +
                                            (srcData[xRight]&0xffff));
                                    xLeft  += srcPixelStride;
                                    xRight -= srcPixelStride;
                                } // while xLeft

                                // Grab the center pixel if hParity was odd also
                                if (hParity == 1) sum += vCtr*hCtr* (srcData[xLeft]&0xffff);

                            } // if vParity
                            int val = (int)(sum + 0.5);
                            dstData[dInd] = (short)(val > 0xffff ? 0xffff : (val < 0 ? 0 : val));

                            dInd += dstPixelStride;
                        } // for xSrc

                        return null;
                    }
                });
            } // for ySrc
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    } // computeRectUShort

    /** <code>computeRectShort</code> filter subsamples short pixel data.
     *
     * @param src RasterAccessor for source image.
     * @param dst RasterAccessor for output image.
     */
    protected void computeRectShort(RasterAccessor src, RasterAccessor dst) {

        // Get dimensions.
        final int dwidth = dst.getWidth();
        final int dheight = dst.getHeight();
        final int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        final short dstDataArrays[][] = dst.getShortDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Get source data array references and strides.
        final short srcDataArrays[][] = src.getShortDataArrays();
        final int srcBandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        // Compute reused numbers
        final int kernelNx = 2*hKernel.length - hParity;
        final int kernelNy = 2*vKernel.length - vParity;
        final int stepDown = (kernelNy - 1)*srcScanlineStride;
        final int stepRight = (kernelNx - 1)*srcPixelStride;

        final float vCtr = vKernel[0];
        final float hCtr = hKernel[0];

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dnumBands; band++) {
            final short dstData[] = dstDataArrays[band];
            final short srcData[] = srcDataArrays[band];
            final int srcScanlineOffset = srcBandOffsets[band];
            final int dstScanlineOffset = dstBandOffsets[band];

            // Step over source raster coordinates
            for (int h = 0; h < dheight; h++) {
                final int y = h;
                final int ySrc = h * scaleY;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dInd = dstScanlineOffset + y * dstScanlineStride;
                        for (int xSrc = 0 ; xSrc < scaleX*dwidth ; xSrc += scaleX) {

                            int upLeft0 = xSrc*srcPixelStride + ySrc*srcScanlineStride + srcScanlineOffset;
                            int upRight0 = upLeft0 + stepRight;
                            int dnLeft0 = upLeft0 + stepDown;
                            int dnRight0 = upRight0 + stepDown;

                            // Exploit 4-fold symmetry
                            float sum = 0;

                            // Make the rectangle squeeze in the vertical direction
                            for (int iy = vKernel.length - 1 ; iy > vParity - 1 ; iy--) {
                                int upLeft = upLeft0;
                                int upRight = upRight0;
                                int dnLeft = dnLeft0;
                                int dnRight = dnRight0;

                                // Make the rectangle squeeze in the horizontal direction
                                for (int ix = hKernel.length - 1 ; ix > hParity - 1 ; ix--) {
                                    float kk = hKernel[ix]*vKernel[iy];
                                    sum += kk*((int)(srcData[upLeft]) +
                                            (int)(srcData[upRight]) +
                                            (int)(srcData[dnLeft]) +
                                            (int)(srcData[dnRight]));
                                    upLeft += srcPixelStride;
                                    upRight -= srcPixelStride;
                                    dnLeft += srcPixelStride;
                                    dnRight -= srcPixelStride;
                                } // ix
                                upLeft0 += srcScanlineStride;   // down a row
                                upRight0 += srcScanlineStride;
                                dnLeft0 -= srcScanlineStride;   // up a row
                                dnRight0 -= srcScanlineStride;
                            } // iy

                            // Compute the remaining 2-Fold symmetry portions
                            // (across and down as needed)

                            // Loop down the center (hParity is odd)
                            if (hParity == 1) {
                                int xUp = (xSrc + hKernel.length - 1)*srcPixelStride +
                                        ySrc*srcScanlineStride + srcScanlineOffset;
                                int xDown = xUp + stepDown;
                                int kInd = vKernel.length - 1;
                                while (xUp < xDown) {
                                    float kk = hCtr*vKernel[kInd--];
                                    sum += kk*((int)(srcData[xUp]) +
                                            (int)(srcData[xDown]));
                                    xUp += srcScanlineStride;
                                    xDown -= srcScanlineStride;
                                }
                            } // hParity

                            // Loop across the center (vParity is odd), pick up the center if hParity was odd
                            if (vParity == 1) {
                                int xLeft = xSrc*srcPixelStride +
                                        (ySrc + vKernel.length - 1)*srcScanlineStride +
                                        srcScanlineOffset;
                                int xRight = xLeft + stepRight;
                                int kInd = hKernel.length - 1;
                                while (xLeft < xRight) {
                                    float kk = vCtr*hKernel[kInd--];
                                    sum += kk*((int)(srcData[xLeft]) +
                                            (int)(srcData[xRight]));
                                    xLeft += srcPixelStride;
                                    xRight -= srcPixelStride;
                                } // while xLeft

                                // Grab the center pixel if hParity was odd also
                                if (hParity == 1) sum += vCtr*hCtr*(int)(srcData[xLeft]);

                            } // if vParity

                            dstData[dInd] = ImageUtil.clampShort((int)(sum + 0.5));
                            dInd += dstPixelStride;

                        } // for xSrc

                        return null;
                    }
                });
            } // for ySrc
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    } // computeRectShort

    /** <code>computeRectInt</code> filter subsamples int pixel data.
     *
     * @param src RasterAccessor for source image.
     * @param dst RasterAccessor for output image.
     */
    protected void computeRectInt(RasterAccessor src, RasterAccessor dst) {

        // Get dimensions.
        final int dwidth = dst.getWidth();
        final int dheight = dst.getHeight();
        final int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        final int dstDataArrays[][] = dst.getIntDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Get source data array references and strides.
        final int srcDataArrays[][] = src.getIntDataArrays();
        final int srcBandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        // Compute reused numbers
        final int kernelNx = 2*hKernel.length - hParity;
        final int kernelNy = 2*vKernel.length - vParity;
        final int stepDown = (kernelNy - 1)*srcScanlineStride;
        final int stepRight = (kernelNx - 1)*srcPixelStride;

        final double vCtr = (double)vKernel[0];
        final double hCtr = (double)hKernel[0];

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dnumBands; band++) {
            final int dstData[] = dstDataArrays[band];
            final int srcData[] = srcDataArrays[band];
            final int srcScanlineOffset = srcBandOffsets[band];
            final int dstScanlineOffset = dstBandOffsets[band];

            // Step over source raster coordinates
            for (int h = 0; h < dheight; h++) {
                final int y = h;
                final int ySrc = h * scaleY;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dInd = dstScanlineOffset + y * dstScanlineStride;
                        for (int xSrc = 0 ; xSrc < scaleX*dwidth ; xSrc += scaleX) {

                            int upLeft0 = xSrc*srcPixelStride + ySrc*srcScanlineStride + srcScanlineOffset;
                            int upRight0 = upLeft0 + stepRight;
                            int dnLeft0 = upLeft0 + stepDown;
                            int dnRight0 = upRight0 + stepDown;

                            // Exploit 4-fold symmetry
                            double sum = 0;

                            // Make the rectangle squeeze in the vertical direction
                            for (int iy = vKernel.length - 1 ; iy > vParity - 1 ; iy--) {
                                int upLeft = upLeft0;
                                int upRight = upRight0;
                                int dnLeft = dnLeft0;
                                int dnRight = dnRight0;

                                // Make the rectangle squeeze in the horizontal direction
                                for (int ix = hKernel.length - 1 ; ix > hParity - 1 ; ix--) {
                                    double kk = hKernel[ix]*vKernel[iy];
                                    sum += kk*((long)srcData[upLeft] + (long)srcData[upRight] +
                                            (long)srcData[dnLeft] + (long)srcData[dnRight]);
                                    upLeft += srcPixelStride;
                                    upRight -= srcPixelStride;
                                    dnLeft += srcPixelStride;
                                    dnRight -= srcPixelStride;
                                } // ix
                                upLeft0 += srcScanlineStride;   // down a row
                                upRight0 += srcScanlineStride;
                                dnLeft0 -= srcScanlineStride;   // up a row
                                dnRight0 -= srcScanlineStride;
                            } // iy

                            // Compute the remaining 2-Fold symmetry portions
                            // (across and down as needed)

                            // Loop down the center (hParity is odd)
                            if (hParity == 1) {
                                int xUp = (xSrc + hKernel.length - 1)*srcPixelStride +
                                        ySrc*srcScanlineStride + srcScanlineOffset;
                                int xDown = xUp + stepDown;
                                int kInd = vKernel.length - 1;
                                while (xUp < xDown) {
                                    double kk = hCtr*vKernel[kInd--];
                                    sum += kk*((long)srcData[xUp] + (long)srcData[xDown]);
                                    xUp += srcScanlineStride;
                                    xDown -= srcScanlineStride;
                                }
                            } // hParity

                            // Loop across the center (vParity is odd), pick up the center if hParity was odd
                            if (vParity == 1) {
                                int xLeft = xSrc*srcPixelStride +
                                        (ySrc + vKernel.length - 1)*srcScanlineStride +
                                        srcScanlineOffset;
                                int xRight = xLeft + stepRight;
                                int kInd = hKernel.length - 1;
                                while (xLeft < xRight) {
                                    double kk = vCtr*hKernel[kInd--];
                                    sum += kk*((long)(srcData[xLeft]) + (long)(srcData[xRight]));
                                    xLeft += srcPixelStride;
                                    xRight -= srcPixelStride;
                                } // while xLeft

                                // Grab the center pixel if hParity was odd also
                                if (hParity == 1) sum += vCtr*hCtr*((long)srcData[xLeft]);

                            } // if vParity

                            dstData[dInd] = ImageUtil.clampInt((int)(sum + 0.5));

                            dInd += dstPixelStride;
                        } // for xSrc

                        return null;
                    }
                });
            } // for ySrc
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    } // computeRectInt

    /** <code>computeRectFloat</code> filter subsamples float pixel data.
     *
     * @param src RasterAccessor for source image.
     * @param dst RasterAccessor for output image.
     */
    protected void computeRectFloat(RasterAccessor src, RasterAccessor dst) {

        // Get dimensions.
        final int dwidth = dst.getWidth();
        final int dheight = dst.getHeight();
        final int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        final float dstDataArrays[][] = dst.getFloatDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Get source data array references and strides.
        final float srcDataArrays[][] = src.getFloatDataArrays();
        final int srcBandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        // Compute reused numbers
        final int kernelNx = 2*hKernel.length - hParity;
        final int kernelNy = 2*vKernel.length - vParity;
        final int stepDown = (kernelNy - 1)*srcScanlineStride;
        final int stepRight = (kernelNx - 1)*srcPixelStride;

        final double vCtr = (double)vKernel[0];
        final double hCtr = (double)hKernel[0];

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dnumBands; band++) {
            final float dstData[] = dstDataArrays[band];
            final float srcData[] = srcDataArrays[band];
            final int srcScanlineOffset = srcBandOffsets[band];
            final int dstScanlineOffset = dstBandOffsets[band];

            // Step over source raster coordinates
            for (int h = 0; h < dheight; h++) {
                final int y = h;
                final int ySrc = h * scaleY;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dInd = dstScanlineOffset + y * dstScanlineStride;
                        for (int xSrc = 0 ; xSrc < scaleX*dwidth ; xSrc += scaleX) {

                            int upLeft0 = xSrc*srcPixelStride + ySrc*srcScanlineStride + srcScanlineOffset;
                            int upRight0 = upLeft0 + stepRight;
                            int dnLeft0 = upLeft0 + stepDown;
                            int dnRight0 = upRight0 + stepDown;

                            // Exploit 4-fold symmetry
                            double sum = 0;

                            // Make the rectangle squeeze in the vertical direction
                            for (int iy = vKernel.length - 1 ; iy > vParity - 1 ; iy--) {
                                int upLeft = upLeft0;
                                int upRight = upRight0;
                                int dnLeft = dnLeft0;
                                int dnRight = dnRight0;

                                // Make the rectangle squeeze in the horizontal direction
                                for (int ix = hKernel.length - 1 ; ix > hParity - 1 ; ix--) {
                                    double kk = hKernel[ix]*vKernel[iy];
                                    sum += kk*((double)srcData[upLeft] + (double)srcData[upRight] +
                                            (double)srcData[dnLeft] + (double)srcData[dnRight]);
                                    upLeft += srcPixelStride;
                                    upRight -= srcPixelStride;
                                    dnLeft += srcPixelStride;
                                    dnRight -= srcPixelStride;
                                } // ix
                                upLeft0 += srcScanlineStride;   // down a row
                                upRight0 += srcScanlineStride;
                                dnLeft0 -= srcScanlineStride;   // up a row
                                dnRight0 -= srcScanlineStride;
                            } // iy

                            // Compute the remaining 2-Fold symmetry portions
                            // (across and down as needed)

                            // Loop down the center (hParity is odd)
                            if (hParity == 1) {
                                int xUp = (xSrc + hKernel.length - 1)*srcPixelStride +
                                        ySrc*srcScanlineStride + srcScanlineOffset;
                                int xDown = xUp + stepDown;
                                int kInd = vKernel.length - 1;
                                while (xUp < xDown) {
                                    double kk = hCtr*vKernel[kInd--];
                                    sum += kk*((double)srcData[xUp] + (double)srcData[xDown]);
                                    xUp += srcScanlineStride;
                                    xDown -= srcScanlineStride;
                                }
                            } // hParity

                            // Loop across the center (vParity is odd), pick up the center if hParity was odd
                            if (vParity == 1) {
                                int xLeft = xSrc*srcPixelStride +
                                        (ySrc + vKernel.length - 1)*srcScanlineStride +
                                        srcScanlineOffset;
                                int xRight = xLeft + stepRight;
                                int kInd = hKernel.length - 1;
                                while (xLeft < xRight) {
                                    double kk = vCtr*hKernel[kInd--];
                                    sum += kk*((double)(srcData[xLeft]) + (double)(srcData[xRight]));
                                    xLeft += srcPixelStride;
                                    xRight -= srcPixelStride;
                                } // while xLeft

                                // Grab the center pixel if hParity was odd also
                                if (hParity == 1) sum += vCtr*hCtr*((double)srcData[xLeft]);

                            } // if vParity

                            dstData[dInd] = ImageUtil.clampFloat(sum);

                            dInd += dstPixelStride;
                        } // for xSrc

                        return null;
                    }
                });
            } // for ySrc
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    } // computeRectFloat

    /** <code>computeRectDouble</code> filter subsamples double pixel data.
     *
     * @param src RasterAccessor for source image.
     * @param dst RasterAccessor for output image.
     */
    protected void computeRectDouble(RasterAccessor src, RasterAccessor dst) {

        // Get dimensions.
        final int dwidth = dst.getWidth();
        final int dheight = dst.getHeight();
        final int dnumBands = dst.getNumBands();

        // Get destination data array references and strides.
        final double dstDataArrays[][] = dst.getDoubleDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        // Get source data array references and strides.
        final double srcDataArrays[][] = src.getDoubleDataArrays();
        final int srcBandOffsets[] = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        // Compute reused numbers
        final int kernelNx = 2*hKernel.length - hParity;
        final int kernelNy = 2*vKernel.length - vParity;
        final int stepDown = (kernelNy - 1)*srcScanlineStride;
        final int stepRight = (kernelNx - 1)*srcPixelStride;

        final double vCtr = (double)vKernel[0];
        final double hCtr = (double)hKernel[0];

        ExecutorService threadPool = Executors.newFixedThreadPool(numProc);
        Collection<Callable<Void>> processes = new LinkedList<Callable<Void>>();
        for (int band = 0; band < dnumBands; band++) {
            final double dstData[] = dstDataArrays[band];
            final double srcData[] = srcDataArrays[band];
            final int srcScanlineOffset = srcBandOffsets[band];
            final int dstScanlineOffset = dstBandOffsets[band];

            // Step over source raster coordinates
            for (int h = 0; h < dheight; h++) {
                final int y = h;
                final int ySrc = h * scaleY;
                processes.add(new Callable<Void>() {
                    @Override
                    public Void call() {
                        int dInd = dstScanlineOffset + y * dstScanlineStride;
                        for (int xSrc = 0 ; xSrc < scaleX*dwidth ; xSrc += scaleX) {

                            int upLeft0 = xSrc*srcPixelStride + ySrc*srcScanlineStride + srcScanlineOffset;
                            int upRight0 = upLeft0 + stepRight;
                            int dnLeft0 = upLeft0 + stepDown;
                            int dnRight0 = upRight0 + stepDown;

                            // Exploit 4-fold symmetry
                            double sum = 0;

                            // Make the rectangle squeeze in the vertical direction
                            for (int iy = vKernel.length - 1 ; iy > vParity - 1 ; iy--) {
                                int upLeft = upLeft0;
                                int upRight = upRight0;
                                int dnLeft = dnLeft0;
                                int dnRight = dnRight0;

                                // Make the rectangle squeeze in the horizontal direction
                                for (int ix = hKernel.length - 1 ; ix > hParity - 1 ; ix--) {
                                    double kk = hKernel[ix]*vKernel[iy];
                                    sum += kk*(srcData[upLeft] + srcData[upRight] +
                                            srcData[dnLeft] + srcData[dnRight]);
                                    upLeft += srcPixelStride;
                                    upRight -= srcPixelStride;
                                    dnLeft += srcPixelStride;
                                    dnRight -= srcPixelStride;
                                } // ix
                                upLeft0 += srcScanlineStride;   // down a row
                                upRight0 += srcScanlineStride;
                                dnLeft0 -= srcScanlineStride;   // up a row
                                dnRight0 -= srcScanlineStride;
                            } // iy

                            // Compute the remaining 2-Fold symmetry portions
                            // (across and down as needed)

                            // Loop down the center (hParity is odd)
                            if (hParity == 1) {
                                int xUp = (xSrc + hKernel.length - 1)*srcPixelStride +
                                        ySrc*srcScanlineStride + srcScanlineOffset;
                                int xDown = xUp + stepDown;
                                int kInd = vKernel.length - 1;
                                while (xUp < xDown) {
                                    double kk = hCtr*vKernel[kInd--];
                                    sum += kk*(srcData[xUp] + srcData[xDown]);
                                    xUp += srcScanlineStride;
                                    xDown -= srcScanlineStride;
                                }
                            } // hParity

                            // Loop across the center (vParity is odd), pick up the center if hParity was odd
                            if (vParity == 1) {
                                int xLeft = xSrc*srcPixelStride +
                                        (ySrc + vKernel.length - 1)*srcScanlineStride +
                                        srcScanlineOffset;
                                int xRight = xLeft + stepRight;
                                int kInd = hKernel.length - 1;
                                while (xLeft < xRight) {
                                    double kk = vCtr*hKernel[kInd--];
                                    sum += kk*(srcData[xLeft] + srcData[xRight]);
                                    xLeft += srcPixelStride;
                                    xRight -= srcPixelStride;
                                } // while xLeft

                                // Grab the center pixel if hParity was odd also
                                if (hParity == 1) sum += vCtr*hCtr*srcData[xLeft];

                            } // if vParity

                            dstData[dInd] = sum;

                            dInd += dstPixelStride;
                        } // for xSrc

                        return null;
                    }
                });
            } // for ySrc
        }
        try {
            threadPool.invokeAll(processes);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            threadPool.shutdown();
        }
    } // computeRectDouble

} // class FilteredSubsampleOpImage
