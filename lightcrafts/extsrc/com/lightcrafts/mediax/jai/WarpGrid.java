/*
 * $RCSfile: WarpGrid.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:24 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.geom.Point2D;


/**
 * A regular grid-based description of an image warp.
 *
 * <p> The mapping from destination pixels to source positions is
 * described by bilinear interpolation within a rectilinear grid of
 * points with known mappings.
 *
 * <p> Given a destination pixel coordinate (x, y) that lies within
 * a cell having corners at (x0, y0), (x1, y0), (x0, y1) and (x1, y1),
 * with source coordinates defined at each respective corner equal
 * to (sx0, sy0), (sx1, sy1), (sx2, sy2) and (sx3, sy3), the
 * source position (sx, sy) that maps onto (x, y) is given by the formulas:
 *
 * <pre>
 * xfrac = (x - x0)/(x1 - x0)
 * yfrac = (y - y0)/(y1 - y0)
 *
 * s = sx0 + (sx1 - sx0)*xfrac
 * t = sy0 + (sy1 - sy0)*xfrac
 *
 * u = sx2 + (sx3 - sx2)*xfrac
 * v = sy2 + (sy3 - sy2)*xfrac
 *
 * sx = s + (u - s)*yfrac
 * sy = t + (v - t)*yfrac
 * </pre>
 *
 * <p> In other words, the source x and y values are interpolated
 * horizontally along the top and bottom edges of the grid cell,
 * and the results are interpolated vertically:
 *
 * <pre>
 * (x0, y0) ->            (x1, y0) ->
 *   (sx0, sy0)             (sx1, sy1)
 *    +------------+---------+
 *    |            |\        |
 *    |            | (s, t)  |
 *    |            |         |
 *    |            |         |
 *    |            |         |
 *    |            |         |
 *    | (x, y) ->  |         |
 *    |  (sx, sy)--+         |
 *    |            |         |
 *    |            |         |
 *    |            | (u, v)  |
 *    |            |/        |
 *    +------------+---------+
 * (x0, y1) ->          (x1, y1) ->
 *   (sx2, sy2)           (sx3, sy3)
 * </pre>
 *
 * <p> Points outside the bounds of the cells defining the grid warp will
 * be mapped to the source image using the identity transformation.
 *
 * <p> WarpGrid is marked final so that it may be more easily inlined.
 *
 */
public final class WarpGrid extends Warp {

    private int xStart;
    private int yStart;

    private int xEnd;
    private int yEnd;

    private int xStep;
    private int yStep;

    private int xNumCells;
    private int yNumCells;

    private float[] xWarpPos;
    private float[] yWarpPos;

    /**
     * @param xStart 
     * @param xStep 
     * @param xNumCells  
     * @param yStart 
     * @param yStep 
     * @param yNumCells  
     * @param warpPositions 
     */
    private void initialize(int xStart, int xStep, int xNumCells,
                            int yStart, int yStep, int yNumCells,
                            float[] warpPositions) {
        this.xStart = xStart;
        this.yStart = yStart;

        this.xEnd = xStart + xStep * xNumCells;
        this.yEnd = yStart + yStep * yNumCells;

        this.xStep = xStep;
        this.yStep = yStep;

        this.xNumCells = xNumCells;
        this.yNumCells = yNumCells;

        int xNumGrids = xNumCells + 1;
        int yNumGrids = yNumCells + 1;

        int numNodes = yNumGrids*xNumGrids;

        xWarpPos = new float[numNodes];
        yWarpPos = new float[numNodes];

        int index = 0;
        for (int idx = 0; idx < numNodes; idx++) {
            xWarpPos[idx] = warpPositions[index++];
            yWarpPos[idx] = warpPositions[index++];
        }
    }

    /**
     * Constructs a WarpGrid with a given grid-based transform mapping
     * destination pixels into source space.  Note that this is
     * a backward mapping as opposed to the forward mapping used in
     * AffineOpImage.
     *
     * <p> The grid is defined by a set of equal-sized cells.
     * The grid starts at (xStart, yStart).  Each cell has width
     * equal to xStep and height equal to yStep, and there are
     * xNumCells cells horizontally and yNumCells cells vertically.
     *
     * <p> The local mapping within each cell is defined by
     * the values in the table parameter.  This parameter must
     * contain 2*(xNumCells + 1)*(yNumCells + 1) values, which
     * alternately contain the source X and Y coordinates to which
     * each destination grid intersection point maps.
     * The cells are enumerated in row-major order, that is,
     * all the grid points along a row are enumerated first, then
     * the grid points for the next row are enumerated, and so on.
     *
     * <p> As an example, suppose xNumCells is equal to 2 and
     * yNumCells is equal 1.  Then the order of the data in table
     * would be:
     *
     * <pre>
     * x00, y00, x10, y10, x20, y20, x01, y01, x11, y11, x21, y21
     * </pre>
     *
     * for a total of 2*(2 + 1)*(1 + 1) = 12 elements.
     *
     * @param xStart the minimum X coordinate of the grid.
     * @param xStep the horizontal spacing between grid cells.
     * @param xNumCells the number of grid cell columns.
     * @param yStart the minimum Y coordinate of the grid.
     * @param yStep the vertical spacing between grid cells.
     * @param yNumCells the number of grid cell rows.
     * @param warpPositions a float array of length 2*(xNumCells + 1)*
     *        (yNumCells + 1) containing the warp positions at the
     *        grid points, in row-major order.
     * @throws IllegalArgumentException if the length of warpPositions is incorrect
     */
    public WarpGrid(int xStart, int xStep, int xNumCells,
                    int yStart, int yStep, int yNumCells,
                    float[] warpPositions) {
        if (warpPositions.length != 2 * (xNumCells + 1) * (yNumCells + 1)) {
            throw new IllegalArgumentException(JaiI18N.getString("WarpGrid0"));
        }

        initialize(xStart, xStep, xNumCells,
                   yStart, yStep, yNumCells,
                   warpPositions);
    }

    /**
     * Constructs a WarpGrid object by sampling the displacements
     * given by another Warp object of any kind.
     *
     * <p> The grid is defined by a set of equal-sized cells.
     * The grid starts at (xStart, yStart).  Each cell has width
     * equal to xStep and height equal to yStep, and there are
     * xNumCells cells horizontally and yNumCells cells vertically.
     *
     * @param master the Warp object used to initialize the grid
     *        displacements.
     * @param xStart the minimum X coordinate of the grid.
     * @param xStep the horizontal spacing between grid cells.
     * @param xNumCells the number of grid cell columns.
     * @param yStart the minimum Y coordinate of the grid.
     * @param yStep the vertical spacing between grid cells.
     * @param yNumCells the number of grid cell rows.
     */
    public WarpGrid(Warp master,
                    int xStart, int xStep, int xNumCells,
                    int yStart, int yStep, int yNumCells) {
        int size = 2 * (xNumCells + 1) * (yNumCells + 1);

        float[] warpPositions = new float[size];
        warpPositions = master.warpSparseRect(xStart, yStart,
                                              xNumCells * xStep + 1, // width
                                              yNumCells * yStep + 1, // height
                                              xStep, yStep,
                                              warpPositions);

        initialize(xStart, xStep, xNumCells,
                   yStart, yStep, yNumCells,
                   warpPositions);
    }

    /** Returns the minimum X coordinate of the grid. */
    public int getXStart() {
        return xStart;
    }

    /** Returns the minimum Y coordinate of the grid. */
    public int getYStart() {
        return yStart;
    }

    /** Returns the horizontal spacing between grid cells. */
    public int getXStep() {
        return xStep;
    }

    /** Returns the vertical spacing between grid cells. */
    public int getYStep() {
        return yStep;
    }

    /** Returns the number of grid cell columns. */
    public int getXNumCells() {
        return xNumCells;
    }

    /** Returns the number of grid cell rows. */
    public int getYNumCells() {
        return yNumCells;
    }

    /** Returns the horizontal warp positions at the grid points. */
    public float[] getXWarpPos() {
        return xWarpPos;
    }

    /** Returns the vertical warp positions at the grid points. */
    public float[] getYWarpPos() {
        return yWarpPos;
    }

    /**
     * Copies source to destination, no warpping.
     *
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @param periodX
     * @param periodY
     * @param offset
     * @param stride
     * @param destRect
     * @return An array of <code>float</code>s.
     * @throws IllegalArgumentException if destRect is null
     * @throws ArrayBoundsException if destRect is too small
     */
    private float[] noWarpSparseRect(int x1, int x2,
                                     int y1, int y2,
                                     int periodX, int periodY,
                                     int offset, int stride,
                                     float[] destRect) {

        if ( destRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        for (int j = y1; j <= y2; j += periodY) {
            int index = offset;
            offset += stride;

            for (int i = x1; i <= x2; i += periodX) {
                destRect[index++] = i;
                destRect[index++] = j;
            }
        }

        return destRect;
    }

    /**
     * Computes the source subpixel positions for a given rectangular
     * destination region, subsampled with an integral period.
     *
     * <p> Points outside the bounds of the cells defining the grid warp will
     * be mapped to the source image using the identity transformation.

     * @param x  The minimum X coordinate of the destination region.
     * @param y  The minimum Y coordinate of the destination region.
     * @param width  The width of the destination region.
     * @param height  The height of the destination region.
     * @param periodX  The horizontal sampling period.
     * @param periodY  The vertical sampling period.
     * @param destRect  An int array containing at least
     *        2*((width+periodX-1)/periodX)*((height+periodY-1)/periodY)
     *        elements, or <code>null</code>.  If <code>null</code>, a
     *        new array will be constructed.
     *
     * @return a reference to the destRect parameter if it is
     *         non-<code>null</code>, or a new int array of length
     *         2*width*height otherwise.
     * @throws ArrayBoundsException if destRect is too small
     */
    public float[] warpSparseRect(int x, int y,
                                  int width, int height,
                                  int periodX, int periodY,
                                  float[] destRect) {
        // Number of points (x, y) per scanline
        int stride = 2 * ((width + periodX - 1) / periodX);

        if (destRect == null) {
            destRect = new float[stride * ((height + periodY - 1) / periodY)];
        }

        int x1 = x;			// first x point
        int x2 = x + width - 1;		// last x point
        int y1 = y;			// first y point
        int y2 = y + height - 1;	// last y point

        if (y1 >= yEnd || y2 < yStart || x1 >= xEnd || x2 < xStart) {
            // destRect is completely outside of warp grid
            return noWarpSparseRect(x1, x2, y1, y2, periodX, periodY,
                                    0, stride, destRect);
        }

        if (y1 < yStart) {	// the rectangle above the warp grid area
            int periods = (yStart - y1 + periodY - 1) / periodY;
            noWarpSparseRect(x1, x2, y1, yStart - 1, periodX, periodY,
                             0, stride, destRect);
            y1 += periods * periodY;
        }

        if (y2 >= yEnd) {	// the rectangle below the warp grid area
            int periods = (yEnd - y + periodY - 1) / periodY;
            noWarpSparseRect(x1, x2, y + periods * periodY, y2,
                             periodX, periodY,
                             periods * stride, stride, destRect);
            // One period up should be inside warp grid
            y2 = y + (periods - 1) * periodY;
        }

        if (x1 < xStart) {	// the rectangle left of the warp grid area
            int periods = (xStart - x1 + periodX - 1) / periodX;
            noWarpSparseRect(x1, xStart - 1, y1, y2, periodX, periodY,
                             (y1 - y) / periodY * stride, stride, destRect);
            x1 += periods * periodX;
        }

        if (x2 >= xEnd) {	// the rectangle right of the warp grid area
            int periods = (xEnd - x + periodX - 1) / periodX;
            noWarpSparseRect(x + periods * periodX, x2, y1, y2,
                             periodX, periodY,
                             (y1 - y) / periodY * stride + periods * 2,
                             stride, destRect);
            // One period left should be inside warp grid
            x2 = x + (periods - 1) * periodX;
        }

        //
        // Now the rectangle is within warp grid, that is
        // xStart <= x1 <= x2 < xEnd and yStart <= y1 <= y2 < yEnd.
        //
        // address = s0(1-x)(1-y) + s1x(1-y) + s2(1-x)y + s3xy
        //

        // A table stores the number of points inside each cell
        int[] cellPoints = new int[xNumCells];
        for (int i = x1; i <= x2; i += periodX) {
            cellPoints[(i - xStart) / xStep]++;
        }

        int offset = (y1 - y) / periodY * stride + (x1 - x) / periodX * 2;

        // Store the number of horizontal grid nodes.
        int xNumGrids = xNumCells + 1;

        // Fractional step in X.
        float deltaX = (float)periodX/(float)xStep;

        // The rectangle within the warp grid
        for (int j = y1; j <= y2; j += periodY) {
            int index = offset;
            offset += stride;

            int yCell = (j - yStart) / yStep;
            int yGrid = yStart + yCell * yStep;
            float yFrac = (float)(j + 0.5F - yGrid) / (float)yStep;

            // Cache some values to avoid two multiplications per x loop.
            float deltaTop = (1.0F - yFrac)*deltaX;
            float deltaBottom = yFrac*deltaX;

            int i = x1;
            while (i <= x2) {
                // Entering a new cell, set up
                int xCell = (i - xStart) / xStep;
                int xGrid = xStart + xCell * xStep;
                float xFrac = (float)(i + 0.5F - xGrid) / (float)xStep;

                int nodeOffset = yCell*xNumGrids + xCell;
                float wx0 = xWarpPos[nodeOffset];
                float wy0 = yWarpPos[nodeOffset];
                float wx1 = xWarpPos[++nodeOffset];
                float wy1 = yWarpPos[nodeOffset];
                nodeOffset += xNumCells; // NB: xNumCells == xNumGrids - 1
                float wx2 = xWarpPos[nodeOffset];
                float wy2 = yWarpPos[nodeOffset];
                float wx3 = xWarpPos[++nodeOffset];
                float wy3 = yWarpPos[nodeOffset];

                float s = wx0 + (wx1 - wx0) * xFrac;
                float t = wy0 + (wy1 - wy0) * xFrac;
                float u = wx2 + (wx3 - wx2) * xFrac;
                float v = wy2 + (wy3 - wy2) * xFrac;

                float wx = s + (u - s) * yFrac;
                float wy = t + (v - t) * yFrac;

                // Delta in x and y.
                float dx = (wx1 - wx0)*deltaTop + (wx3 - wx2)*deltaBottom;
                float dy = (wy1 - wy0)*deltaTop + (wy3 - wy2)*deltaBottom;

                // The points inside the current cell
                int nPoints = cellPoints[xCell];
                for (int k = 0; k < nPoints; k++) {
                    destRect[index++] = wx - 0.5F;
                    destRect[index++] = wy - 0.5F;

                    wx += dx;
                    wy += dy;
                    i += periodX;
                }
            }
        }

        return destRect;
    }

    /**
     * Computes the source point corresponding to the supplied point.
     *
     * <p>This method returns the value of <code>pt</code> in the following
     * code snippet:
     *
     * <pre>
     * float[] sxy = warpSparseRect((int)destPt.getX(), (int)destPt.getY(),
     *                              2, 2, 1, 1, null);
     *
     * double wtRight  = destPt.getX() - (int)destPt.getX();
     * double wtLeft   = 1.0 - wtRight;
     * double wtBottom = destPt.getY() - (int)destPt.getY();
     * double wtTop    = 1.0 - wtBottom;
     *
     * Point2D pt = (Point2D)destPt.clone();
     * pt.setLocation((sxy[0]*wtLeft + sxy[2]*wtRight)*wtTop +
     *                (sxy[4]*wtLeft + sxy[6]*wtRight)*wtBottom,
     *                (sxy[1]*wtLeft + sxy[3]*wtRight)*wtTop +
     *                (sxy[5]*wtLeft + sxy[7]*wtRight)*wtBottom);
     * </pre>
     * </p>
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

        float[] sxy = warpSparseRect((int)destPt.getX(), (int)destPt.getY(),
                                     2, 2, 1, 1, null);

        double wtRight  = destPt.getX() - (int)destPt.getX();
        double wtLeft   = 1.0 - wtRight;
        double wtBottom = destPt.getY() - (int)destPt.getY();
        double wtTop    = 1.0 - wtBottom;

        Point2D pt = (Point2D)destPt.clone();
        pt.setLocation((sxy[0]*wtLeft + sxy[2]*wtRight)*wtTop +
                       (sxy[4]*wtLeft + sxy[6]*wtRight)*wtBottom,
                       (sxy[1]*wtLeft + sxy[3]*wtRight)*wtTop +
                       (sxy[5]*wtLeft + sxy[7]*wtRight)*wtBottom);

        return pt;
    }
}
