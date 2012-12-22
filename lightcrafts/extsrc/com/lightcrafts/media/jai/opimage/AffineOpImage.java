/*
 * $RCSfile: AffineOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:14 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import java.util.Map;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An OpImage class to perform (possibly filtered) affine mapping between
 * a source and destination image.
 *
 * The geometric relationship between source and destination pixels
 * is defined as the following (<code>x</code> and <code>y</code> denote
 * the source pixel coordinates; <code>x'</code> and <code>y'</code>
 * denote the destination pixel coordinates; <code>m</code> denotes the
 * 3x2 transform matrix):
 * <ul>
 * <code>
 * x' = m[0][0] * x + m[0][1] * y + m[0][2]
 * <br>
 * y' = m[1][0] * x + m[1][1] * y + m[1][2]
 * </code>
 * </ul>
 *
 */
class AffineOpImage extends GeometricOpImage {

    /**
     * Unsigned short Max Value
     */
    protected static final int USHORT_MAX = Short.MAX_VALUE - Short.MIN_VALUE;

    /**
     * The forward AffineTransform describing the image transformation.
     */
    protected AffineTransform f_transform;

    /**
     * The inverse AffineTransform describing the image transformation.
     */
    protected AffineTransform i_transform;

    /** The Interpolation object. */
    protected Interpolation interp;

    /** Store source & padded rectangle info */
    private Rectangle srcimg, padimg;

    /** The BorderExtender */
    protected BorderExtender extender;

    /** The true writable area */
    private Rectangle theDest;

    /** Cache the ImagingListener. */
    private ImagingListener listener;

    /**
     * Scanline walking : variables & constants
     */

    /** The fixed-point denominator of the fractional offsets. */
    protected static final int geom_frac_max = 0x100000;

    double m00, m10, flr_m00, flr_m10;
    double fracdx, fracdx1, fracdy, fracdy1;
    int incx, incx1, incy, incy1;
    int ifracdx, ifracdx1, ifracdy, ifracdy1;

    /**
     * Padding values for interpolation
     */
    public int lpad, rpad, tpad, bpad;

    /**
     * Computes floor(num/denom) using integer arithmetic.
     * denom must not be equal to 0.
     */
    protected static int floorRatio(long num, long denom) {
        if (denom < 0) {
            denom = -denom;
            num = -num;
        }

        if (num >= 0) {
            return (int)(num/denom);
        } else {
            return (int)((num - denom + 1)/denom);
        }
    }

    /**
     * Computes ceil(num/denom) using integer arithmetic.
     * denom must not be equal to 0.
     */
    protected static int ceilRatio(long num, long denom) {
        if (denom < 0) {
            denom = -denom;
            num = -num;
        }

        if (num >= 0) {
            return (int)((num + denom - 1)/denom);
        } else {
            return (int)(num/denom);
        }
    }

    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source,
                                            AffineTransform forward_tr) {

        ImageLayout newLayout;
        if (layout != null) {
            newLayout = (ImageLayout)layout.clone();
        } else {
            newLayout = new ImageLayout();
        }

        //
        // Get sx0,sy0 coordinates and width & height of the source
        //
        float sx0 = (float) source.getMinX();
        float sy0 = (float) source.getMinY();
        float sw = (float) source.getWidth();
        float sh = (float) source.getHeight();

        //
        // The 4 points (clockwise order) are
        //      (sx0, sy0),    (sx0+sw, sy0)
        //      (sx0, sy0+sh), (sx0+sw, sy0+sh)
        //
        Point2D[] pts = new Point2D[4];
        pts[0] = new Point2D.Float(sx0, sy0);
        pts[1] = new Point2D.Float((sx0+sw), sy0);
        pts[2] = new Point2D.Float((sx0+sw), (sy0+sh));
        pts[3] = new Point2D.Float(sx0, (sy0+sh));

        // Forward map
        forward_tr.transform(pts, 0, pts, 0, 4);

        float dx0 = Float.MAX_VALUE;
        float dy0 = Float.MAX_VALUE;
        float dx1 = -Float.MAX_VALUE;
        float dy1 = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float px = (float)pts[i].getX();
            float py = (float)pts[i].getY();

            dx0 = Math.min(dx0, px);
            dy0 = Math.min(dy0, py);
            dx1 = Math.max(dx1, px);
            dy1 = Math.max(dy1, py);
        }

        //
        // Get the width & height of the resulting bounding box.
        // This is set on the layout
        //
        int lw = (int)(dx1 - dx0);
        int lh = (int)(dy1 - dy0);

        //
        // Set the starting integral coordinate
        // with the following criterion.
        // If it's greater than 0.5, set it to the next integral value (ceil)
        // else set it to the integral value (floor).
        //
        int lx0, ly0;

        int i_dx0 = (int)Math.floor(dx0);
        if (Math.abs(dx0 - i_dx0) <= 0.5) {
            lx0 = i_dx0;
        } else {
            lx0 = (int) Math.ceil(dx0);
        }

        int i_dy0 = (int)Math.floor(dy0);
        if (Math.abs(dy0 - i_dy0) <= 0.5) {
            ly0 = i_dy0;
        } else {
            ly0 = (int) Math.ceil(dy0);
        }

        //
        // Create the layout
        //
        newLayout.setMinX(lx0);
        newLayout.setMinY(ly0);
        newLayout.setWidth(lw);
        newLayout.setHeight(lh);

        return newLayout;
    }

    /**
     * Constructs an AffineOpImage from a RenderedImage source,
     * AffineTransform, and Interpolation object.  The image
     * dimensions are determined by forward-mapping the source bounds.
     * The tile grid layout, SampleModel, and ColorModel are specified
     * by the image source, possibly overridden by values from the
     * ImageLayout parameter.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param transform the desired AffineTransform.
     * @param interp an Interpolation object.
     */
    public AffineOpImage(RenderedImage source,
                         BorderExtender extender,
                         Map config,
                         ImageLayout layout,
                         AffineTransform transform,
                         Interpolation interp,
			 double[] backgroundValues) {
        super(vectorize(source),
              layoutHelper(layout,
                           source,
                           transform),
              config,
              true,
              extender,
              interp,
	      backgroundValues);

        listener = ImageUtil.getImagingListener((java.awt.RenderingHints)config);

        // store the interp and extender objects
        this.interp = interp;

        // the extender
        this.extender = extender;

        // Store the padding values
        lpad = interp.getLeftPadding();
        rpad = interp.getRightPadding();
        tpad = interp.getTopPadding();
        bpad = interp.getBottomPadding();

        //
        // Store source bounds rectangle
        // and the padded rectangle (for extension cases)
        //
        srcimg = new Rectangle(getSourceImage(0).getMinX(),
                               getSourceImage(0).getMinY(),
                               getSourceImage(0).getWidth(),
                               getSourceImage(0).getHeight());
        padimg = new Rectangle(srcimg.x - lpad,
                               srcimg.y - tpad,
                               srcimg.width + lpad + rpad,
                               srcimg.height + tpad + bpad);

        if (extender == null) {
            //
            // Source has to be shrunk as per interpolation
            // as a result the destination produced could
            // be different from the layout
            //

            //
            // Get sx0,sy0 coordinates and width & height of the source
            //
            float sx0 = (float) srcimg.x;
            float sy0 = (float) srcimg.y;
            float sw = (float) srcimg.width;
            float sh = (float) srcimg.height;

            //
            // get padding amounts as per interpolation
            //
            float f_lpad = (float)lpad;
            float f_rpad = (float)rpad;
            float f_tpad = (float)tpad;
            float f_bpad = (float)bpad;

            //
            // As per pixel defined to be at (0.5, 0.5)
            //
            if (!(interp instanceof InterpolationNearest)) {
                f_lpad += 0.5;
                f_tpad += 0.5;
                f_rpad += 0.5;
                f_bpad += 0.5;
            }

            //
            // Shrink the source by padding amount prior to forward map
            // This is the maxmimum available source than can be mapped
            //
            sx0 += f_lpad;
            sy0 += f_tpad;
            sw -= (f_lpad + f_rpad);
            sh -= (f_tpad + f_bpad);

            //
            // The 4 points are (x0, y0),     (x0+w, y0)
            //                  (x0+w, y0+h), (x0, y0+h)
            //
            Point2D[] pts = new Point2D[4];
            pts[0] = new Point2D.Float(sx0, sy0);
            pts[1] = new Point2D.Float((sx0 + sw), sy0);
            pts[2] = new Point2D.Float((sx0 + sw), (sy0 + sh));
            pts[3] = new Point2D.Float(sx0, (sy0 + sh));

            // Forward map
            transform.transform(pts, 0, pts, 0, 4);

            float dx0 =  Float.MAX_VALUE;
            float dy0 =  Float.MAX_VALUE;
            float dx1 = -Float.MAX_VALUE;
            float dy1 = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                float px = (float)pts[i].getX();
                float py = (float)pts[i].getY();

                dx0 = Math.min(dx0, px);
                dy0 = Math.min(dy0, py);
                dx1 = Math.max(dx1, px);
                dy1 = Math.max(dy1, py);
            }

            //
            // The layout is the wholly contained integer area of the
            // corresponding floating point bounding box.
            // We cannot round the corners of the floating rect because it
            // would increase the size of the rect, so we need to ceil the
            // upper corner and floor the lower corner.
            //
            int lx0 = (int)Math.ceil(dx0);
            int ly0 = (int)Math.ceil(dy0);
            int lx1 = (int)Math.floor(dx1);
            int ly1 = (int)Math.floor(dy1);

            theDest = new Rectangle(lx0,
                                    ly0,
                                    lx1 - lx0,
                                    ly1 - ly0);
        } else {
            theDest = getBounds();
        }

        // Store the inverse and forward transforms.
        try {
            this.i_transform = transform.createInverse();
        } catch (Exception e) {
            String message = JaiI18N.getString("AffineOpImage0");
            listener.errorOccurred(message,
                                   new ImagingException(message, e),
                                   this, false);
//            throw new RuntimeException(JaiI18N.getString("AffineOpImage0"));
        }
        this.f_transform = (AffineTransform)transform.clone();

        //
        // Store the incremental values used in scanline walking.
        //
        m00 = i_transform.getScaleX(); // get m00
        flr_m00 = Math.floor(m00);
        fracdx = m00 - flr_m00;
        fracdx1 = 1.0F - fracdx;
        incx = (int) flr_m00; // Movement
        incx1 = incx + 1;     // along x
        ifracdx = (int) Math.round(fracdx * geom_frac_max);
        ifracdx1 = geom_frac_max - ifracdx;

        m10 = i_transform.getShearY(); // get m10
        flr_m10 = Math.floor(m10);
        fracdy = m10 - flr_m10;
        fracdy1 = 1.0F - fracdy;
        incy = (int) flr_m10; // Movement
        incy1 = incy + 1;     // along y
        ifracdy = (int) Math.round(fracdy * geom_frac_max);
        ifracdy1 = geom_frac_max - ifracdy;
    }

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

        Point2D dpt = (Point2D)destPt.clone();
        dpt.setLocation(dpt.getX() + 0.5, dpt.getY() + 0.5);

        Point2D spt = i_transform.transform(dpt, null);
        spt.setLocation(spt.getX() - 0.5, spt.getY() - 0.5);

        return spt;
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
     * @throws IllegalArgumentException if <code>destPt</code> is
     * <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Point2D spt = (Point2D)sourcePt.clone();
        spt.setLocation(spt.getX() + 0.5, spt.getY() + 0.5);

        Point2D dpt = f_transform.transform(spt, null);
        dpt.setLocation(dpt.getX() - 0.5, dpt.getY() - 0.5);

        return dpt;
    }

    /**
     * Forward map the source Rectangle.
     */
    protected Rectangle forwardMapRect(Rectangle sourceRect,
                                       int sourceIndex) {
        return f_transform.createTransformedShape(sourceRect).getBounds();
    }

    /**
     * Backward map the destination Rectangle.
     */
    protected Rectangle backwardMapRect(Rectangle destRect,
                                        int sourceIndex) {
        //
        // Backward map the destination to get the corresponding
        // source Rectangle
        //
        float dx0 = (float) destRect.x;
        float dy0 = (float) destRect.y;
        float dw = (float) (destRect.width);
        float dh = (float) (destRect.height);

        Point2D[] pts = new Point2D[4];
        pts[0] = new Point2D.Float(dx0, dy0);
        pts[1] = new Point2D.Float((dx0 + dw), dy0);
        pts[2] = new Point2D.Float((dx0 + dw), (dy0 + dh));
        pts[3] = new Point2D.Float(dx0, (dy0 + dh));

        i_transform.transform(pts, 0, pts, 0, 4);

        float f_sx0 =  Float.MAX_VALUE;
        float f_sy0 =  Float.MAX_VALUE;
        float f_sx1 = -Float.MAX_VALUE;
        float f_sy1 = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float px = (float)pts[i].getX();
            float py = (float)pts[i].getY();

            f_sx0 = Math.min(f_sx0, px);
            f_sy0 = Math.min(f_sy0, py);
            f_sx1 = Math.max(f_sx1, px);
            f_sy1 = Math.max(f_sy1, py);
        }

        int s_x0 = 0, s_y0 = 0, s_x1 = 0, s_y1 = 0;

        // Find the bounding box of the source rectangle
        if (interp instanceof InterpolationNearest) {
            s_x0 = (int) Math.floor(f_sx0);
            s_y0 = (int) Math.floor(f_sy0);

            // Fix for bug 4485920 was to add " + 0.05" to the following
            // two lines.  It should be noted that the fix was made based
            // on empirical evidence and tested thoroughly, but it is not
            // known whether this is the root cause.
            s_x1 = (int) Math.ceil(f_sx1 + 0.5);
            s_y1 = (int) Math.ceil(f_sy1 + 0.5);
        } else {
            s_x0 = (int) Math.floor(f_sx0 - 0.5);
            s_y0 = (int) Math.floor(f_sy0 - 0.5);
            s_x1 = (int) Math.ceil(f_sx1);
            s_y1 = (int) Math.ceil(f_sy1);
        }

        //
        // Return the new rectangle
        //
        return new Rectangle(s_x0,
                             s_y0,
                             s_x1 - s_x0,
                             s_y1 - s_y0);
    }

    /**
     * Backward map a destination coordinate (using inverse_transform)
     * to get the corresponding source coordinate.
     * We need not worry about interpolation here.
     *
     * @param destPt the destination point to backward map
     * @return source point result of the backward map
     */
    public void mapDestPoint(Point2D destPoint, Point2D srcPoint) {
        i_transform.transform(destPoint, srcPoint);
    }

    public Raster computeTile(int tileX, int tileY) {
        //
        // Create a new WritableRaster to represent this tile.
        //
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(sampleModel, org);

        //
        // Clip output rectangle to image bounds.
        //
        Rectangle rect = new Rectangle(org.x,
                                       org.y,
                                       tileWidth,
                                       tileHeight);

        //
        // Clip destination tile against the writable destination
        // area. This is either the layout or a smaller area if
        // no extension is specified.
        //
        Rectangle destRect = rect.intersection(theDest);
        Rectangle destRect1 = rect.intersection(getBounds());
        if ((destRect.width <= 0) || (destRect.height <= 0)) {
            // No area to write
	    if (setBackground)
		ImageUtil.fillBackground(dest, destRect1, backgroundValues);

            return dest;
        }

        //
        // determine the source rectangle needed to compute the destRect
        //
        Rectangle srcRect = mapDestRect(destRect, 0);
        if (extender == null) {
            srcRect = srcRect.intersection(srcimg);
        } else {
            srcRect = srcRect.intersection(padimg);
        }

        if (!(srcRect.width > 0 && srcRect.height > 0)) {
	    if (setBackground)
	        ImageUtil.fillBackground(dest, destRect1, backgroundValues);

            return dest;
        }


	if (!destRect1.equals(destRect)) {
	    // beware that estRect1 contains destRect
	    ImageUtil.fillBordersWithBackgroundValues(destRect1, destRect,
						      dest, backgroundValues);
	}

        Raster[] sources = new Raster[1];

        // Get the source data
        if (extender == null) {
            sources[0] = getSourceImage(0).getData(srcRect);
        } else {
            sources[0] = getSourceImage(0).getExtendedData(srcRect, extender);
        }

        // Compute destination tile
        computeRect(sources, dest, destRect);

        // Recycle the source tile
        if(getSourceImage(0).overlapsMultipleTiles(srcRect)) {
            recycleTile(sources[0]);
        }

        return dest;
    }
}
