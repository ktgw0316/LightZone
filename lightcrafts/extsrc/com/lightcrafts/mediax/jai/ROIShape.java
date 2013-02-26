/*
 * $RCSfile: ROIShape.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/11/24 00:04:04 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

/**
 * A class representing a region of interest within an image as a
 * <code>Shape</code>.  Such regions are binary by definition.  Using
 * a <code>Shape</code> representation allows boolean operations to be
 * performed quickly and with compact storage.  If a
 * <code>PropertyGenerator</code> responsible for generating the
 * <code>ROI</code> property of a particular
 * <code>OperationDescriptor</code> (e.g., a warp) cannot reasonably
 * produce an <code>ROIShape</code> representing the region, it should
 * call <code>getAsImage()</code> on its sources and produce its
 * output <code>ROI</code> in image form.
 *
 */
public class ROIShape extends ROI {
    /** The internal Shape that defines this mask. */
    transient Shape theShape = null;

    /**
     * Calculate the point of intersection of two line segments.
     * This method assumes that the line segments do in fact intersect.
     *
     * @param x1 The abscissa of the first end point of the first segment.
     * @param y1 The ordinate of the first end point of the first segment.
     * @param x2 The abscissa of the second end point of the first segment.
     * @param y2 The ordinate of the second end point of the first segment.
     * @param u1 The abscissa of the first end point of the second segment.
     * @param v1 The ordinate of the first end point of the second segment.
     * @param u2 The abscissa of the second end point of the second segment.
     * @param v2 The ordinate of the second end point of the second segment.
     *
     * @return The point of intersection.
     */
    private static Point2D.Double getIntersection(double x1, double y1,
                                                  double x2, double y2,
                                                  double u1, double v1,
                                                  double u2, double v2) {
        double[][] a = new double[2][2];
        a[0][0] = y2 - y1;
        a[0][1] = x1 - x2;
        a[1][0] = v2 - v1;
        a[1][1] = u1 - u2;

        double[] c = new double[2];
        c[0] = y1*(x1 - x2) + x1*(y2 - y1);
        c[1] = v1*(u1 - u2) + u1*(v2 - v1);

        double det = a[0][0]*a[1][1] - a[0][1]*a[1][0];
        double tmp = a[0][0];
        a[0][0] = a[1][1]/det;
        a[0][1] = -a[0][1]/det;
        a[1][0] = -a[1][0]/det;
        a[1][1] = tmp/det;

        double x = a[0][0]*c[0] + a[0][1]*c[1];
        double y = a[1][0]*c[0] + a[1][1]*c[1];

        return new Point2D.Double(x, y);
    }

    /**
     * Convert a <code>Polygon</code> into a <code>LinkedList</code> of
     * <code>Rectangle</code>s representing run lengths of pixels contained
     * within the <code>Polygon</code>.
     *
     * @param clip The clipping <code>Rectangle</code>.
     * @param poly The <code>Polygon</code> to examine.
     * @return The <code>LinkedList</code> of run length
     *         <code>Rectangle</code>s.
     */
    private LinkedList polygonToRunLengthList(Rectangle clip,
                                              Polygon poly) {
        PolyShape ps = new PolyShape(poly, clip);
        return ps.getAsRectList();
    }

    /**
     * Converts a <code>LinkedList</code> of <code>Rectangle</code>s into an
     * array of integers representing a bit mask.
     *
     * @param rectangleList The list of <code>Rectangle</code>s.
     * @param clip The clipping <code>Rectangle</code>.
     * @param mask A two-dimensional array of ints at least
     *        (width + 31)/32 entries wide and (height) entries tall,
     *        or null.
     *
     * @return An integer array representing a bit mask.
     */
    private static int[][] rectangleListToBitmask(LinkedList rectangleList,
                                                  Rectangle clip,
                                                  int[][] mask) {
        int bitField = 0x80000000;

        // Determine the minimum required width of the bitmask in integers.
        int bitmaskIntWidth = (clip.width + 31)/32;

        // Construct bitmask array if argument is null.
        if (mask == null) {
	    mask = new int[clip.height][bitmaskIntWidth];
	} else if (mask.length < clip.height ||
                  mask[0].length < bitmaskIntWidth) {
            throw new RuntimeException(JaiI18N.getString("ROIShape0"));
        }

        // Iterate over the list of Rectangles.
        ListIterator rectangleIter = rectangleList.listIterator(0);
        while (rectangleIter.hasNext()) {
            // Only set bits corresponding to pixels in the clip Rectangle.
            Rectangle rect;
            if (clip.intersects(rect = (Rectangle)rectangleIter.next())) {
                rect = clip.intersection(rect);

                // Set the extremal indexes for the current Rectangle.
                int yMin = rect.y - clip.y;
                int xMin = rect.x - clip.x;
                int yMax = yMin + rect.height - 1;
                int xMax = xMin + rect.width - 1;

                // Set all bits within the current Rectangle.
                for (int y = yMin; y <= yMax; y++) {
                    int[] bitrow = mask[y];
                    for (int x = xMin; x <= xMax; x++) {
                        int index = x / 32;
                        int shift = x % 32;
                        bitrow[index] |= (bitField >>> shift);
                    }
                }
            }
        }

        return mask;
    }

    /**
     * Constructs an ROIShape from a Shape.
     *
     * @param s A Shape.
     *
     * @throws IllegalArgumentException if s is null.
     */
    public ROIShape(Shape s) {
        if(s == null) {
            throw new IllegalArgumentException(JaiI18N.getString("ROIShape2"));
        }
        theShape = s;
    }

    /**
     * Constructs an ROIShape from an Area.
     *
     * @param a An Area.
     */
    public ROIShape(Area a) {
        AffineTransform at = new AffineTransform(); // Identity
        PathIterator pi = a.getPathIterator(at);
        GeneralPath gp = new GeneralPath(pi.getWindingRule());
        gp.append(pi, false);

        theShape = gp;
    }

    /**
     * Instance inner class used for scan conversion of a polygonal
     * <code>Shape</code>.
     */
    private class PolyShape {
        /** A polygon which has yet to be classified as one
         * of the following types. */
        private static final int POLYGON_UNCLASSIFIED = 0;

        /** A degenerate polygon, i.e., all vertices equal or
          on the same line. */
        private static final int POLYGON_DEGENERATE = POLYGON_UNCLASSIFIED + 1;

        /** A convex polygon. */
        private static final int POLYGON_CONVEX = POLYGON_DEGENERATE + 1;

        /** A concave polygon (simple or non-simple). */
        private static final int POLYGON_CONCAVE = POLYGON_CONVEX + 1;

        /** The internal polygon. */
        private Polygon poly;

        /** The clipping <code>Rectangle</code>. */
        private Rectangle clip;

        /** The type of polygon. */
        private int type = POLYGON_UNCLASSIFIED;

        /** Flag indicating whether the supplied clipping <code>Rectangle</code> 
	 *  is inside the <code>Polygon</code>. 
	 */
        private boolean insidePolygon = false;

        /**
         * Constructs a new PolyShape. The <code>Polygon</code> argument is
         * clipped against the supplied <code>Rectangle</code>.
         *
         * @param polygon The <code>Polygon</code>.
         * @param clipRect The clipping <code>Rectangle</code>.
         */
        PolyShape(Polygon polygon, Rectangle clipRect) {
            // Cache the arguments.
            poly = polygon;
            clip = clipRect;

            // Determine whether the clipping Rectangle is inside the Polygon.
            insidePolygon = poly.contains(clipRect);
            type = POLYGON_UNCLASSIFIED;
        }

        /**
         * Inner class representing a polygon edge.
         */
        private class PolyEdge implements Comparator {
            /** X coordinate of intersection of edge with current scanline. */
            public double x;

            /** Change in X with respect to Y. */
            public double dx;

            /** The edge number: edge i goes from vertex i to vertex i+1. */
            public int i;

            /**
             * Construct a <code>PolyEdge</code> object.
             *
             * @param x X coordinate of edge intersection with scanline.
             * @param dx The change in X with respect to Y.
             * @param i The edge number.
             */
            PolyEdge(double x, double dx, int i) {
                this.x = x;
                this.dx = dx;
                this.i = i;
            }

            /**
             * Implementation of java.util.Comparator.compare. The argument
             * <code>Object</code>s are assumed to be <code>PolyEdge</code>s
             * and are sorted on the basis of their respective x components.
             *
             * @param o1 The first <code>PolyEdge</code> object.
             * @param o2 The second <code>PolyEdge</code> object.
             *
             * @return -1 if o1 < o2, 1 if o1 > o2, 0 if o1 == o2.
             */
            public int compare(Object o1, Object o2) {
                double x1 = ((PolyEdge)o1).x;
                double x2 = ((PolyEdge)o2).x;

                int returnValue;
                if (x1 < x2) {
                    returnValue = -1;
                } else if (x1 > x2) {
                    returnValue = 1;
                } else {
                    returnValue = 0;
                }

                return returnValue;
            }
        }

        /**
         * Perform scan conversion of the <code>PolyShape</code> to generate
         * a <code>LinkedList</code> of <code>Rectangle</code>s.
         *
         * @return A <code>LinkedList</code> of <code>Rectangle</code>s
         * representing the scan conversion of the <code>PolyShape</code>.
         */
        public LinkedList getAsRectList() {
            LinkedList rectList = new LinkedList();

            if (insidePolygon) {
                rectList.addLast((Object)poly.getBounds());
            } else {
                // Classify the polygon as one of the pre-defined types.
                classifyPolygon();

                // Perform scan conversion according to polygon type.
                switch(type) {
                case POLYGON_DEGENERATE:
                    rectList = null;
                    break;
                case POLYGON_CONVEX:
                    rectList = scanConvex(rectList);
                    break;
                case POLYGON_CONCAVE:
                    rectList = scanConcave(rectList);
                    break;
                default:
                    throw new RuntimeException(JaiI18N.getString("ROIShape1"));
                }
            }

            return rectList;
        }

        /**
         * Classify a <code>Polygon</code> as one of the pre-defined types
         * for this class.
         */
        private int classifyPolygon() {
            if (type != POLYGON_UNCLASSIFIED) {
                return type;
            }

            int n = poly.npoints;
            if (n < 3) {
                type = POLYGON_DEGENERATE;
                return type;

            } else if (poly.getBounds().contains(clip)) {
                type = POLYGON_CONVEX;
                return type;
            }

            // Cache references to Polygon vertices.
            int[] x = poly.xpoints;
            int[] y = poly.ypoints;

            // Calculate the sign of the angle between the first and
            // second directed segments.
            int previousSign = sgn((x[0] - x[1])*(y[1] - y[2]) -
                                   (x[1] - x[2])*(y[0] - y[1]));
            boolean allZero = (previousSign == 0);

            // Calculate the initial lexicographic direction.
            int previousDirection;
            if (x[0] < x[1]) {
                previousDirection = -1;
            } else if (x[0] > x[1]) {
                previousDirection = 1;
            } else if (y[0] < y[1]) {
                previousDirection = -1;
            } else if (y[0] > y[1]) {
                previousDirection = 1;
            } else {
                previousDirection = 0;
            }

            // Calculate signs of all angles between segments. If all angles
            // are zero then the vertices all lie on a line. If all non-zero
            // angles have the same sign then the polygon is convex. Otherwise
            // the polygon is concave unless the lexicographic direction
            // changes sign more than twice.
            int numDirectionChanges = 0;
            for (int i = 1; i < n; i++) {
                // Set the indices of the next two vertices.
                int j = (i + 1)%n;
                int k = (i + 2)%n;

                // Calculate the initial lexicographic direction.
                int currentDirection;
                if (x[i] < x[j]) {
                    currentDirection = -1;
                } else if (x[i] > x[j]) {
                    currentDirection = 1;
                } else if (y[i] < y[j]) {
                    currentDirection = -1;
                } else if (y[i] > y[j]) {
                    currentDirection = 1;
                } else {
                    currentDirection = 0;
                }

                // Increment the direction change counter if necessary.
                if (currentDirection != 0 &&
                   currentDirection == -previousDirection) {
                    numDirectionChanges++;
                }
                previousDirection = currentDirection;

                // Calculate the sign of the angle between the current and
                // next directed segments.
                int sign = sgn((x[i] - x[j])*(y[j] - y[k]) -
                               (x[j] - x[k])*(y[i] - y[j]));
                allZero = (allZero && sign == 0);
                if (!allZero) {
                    if (sign != 0 && sign == -previousSign) {
                        type = POLYGON_CONCAVE;
                        break;
                    } else if (sign != 0) { // Only cache non-zero signs.
                        previousSign = sign;
                    }
                }
            }

            if (type == POLYGON_UNCLASSIFIED) {
                if (allZero) { // All points on a line.
                    type = POLYGON_DEGENERATE;
                } else if (numDirectionChanges > 2) {
                    type = POLYGON_CONCAVE;
                } else {
                    type = POLYGON_CONVEX;
                }
            }

            return type;
        }

        /**
         * Calculate the sign of the argument.
         *
         * @param i The integer the sign of which is to be determined.
         *
         * @return 1 for positive, -1 for negative, and 0 for zero arguments.
         */
        private final int sgn(int i) {
            int sign;
            if (i > 0) {
                sign = 1;
            } else if (i < 0) {
                sign = -1;
            } else { // zero
                sign = 0;
            }
            return sign;
        }

        /**
         * Perform scan conversion of a convex polygon.
         *
         * @param rectList A <code>LinkedList</code>; may be null.
         *
         * @return A <code>LinkedList</code> of <code>Rectangle</code>s
         * representing the scan conversion of the convex polygon.
         */
        private LinkedList scanConvex(LinkedList rectList) {

            if (rectList == null) {
                rectList = new LinkedList();
            }

            // Find the index of the top vertex.
            int yMin = poly.ypoints[0];
            int topVertex = 0;
            int n = poly.npoints;
            for (int i = 1; i < n; i++) {
                if (poly.ypoints[i] < yMin) {
                    yMin = poly.ypoints[i];
                    topVertex = i;
                }
            }

            // Left and right vertex indices.
            int leftIndex = topVertex;
            int rightIndex = topVertex;

            // Number of vertices remaining.
            int numRemaining = n;

            // Current scan line.
            int y = yMin;

            // Lower end of left & right edges.
            int intYLeft = y - 1;
            int intYRight = intYLeft;

            // Copy points to double precision arrays where necessary.
            double[] px = intArrayToDoubleArray(poly.xpoints);
            int[] py = poly.ypoints;

            double[] leftX = new double[1];
            double[] leftDX = new double[1];
            double[] rightX = new double[1];
            double[] rightDX = new double[1];

            // Scan along lines using new edges along the left and right sides
            // as the line crosses new vertices.
            while (numRemaining > 0) {
                int i;

                // Advance the left edge.
                while (intYLeft <= y && numRemaining > 0) {
                    numRemaining--;
                    i = leftIndex - 1;
                    if (i < 0) i = n - 1;
                    intersectX(px[leftIndex], py[leftIndex], px[i], py[i],
                               y, leftX, leftDX);
                    intYLeft = py[i];
                    leftIndex = i;
                }

                // Advance the right edge.
                while (intYRight <= y && numRemaining > 0) {
                    numRemaining--;
                    i = rightIndex + 1;
                    if (i >= n) i = 0;
                    intersectX(px[rightIndex], py[rightIndex], px[i], py[i],
                               y, rightX, rightDX);
                    intYRight = py[i];
                    rightIndex = i;
                }

                // Process until end of left or right edge.
                while (y < intYLeft && y < intYRight) {
                    if (y >= clip.y && y < clip.getMaxY()) {
                        Rectangle rect;
                        if (leftX[0] <= rightX[0]) {
                            rect = scanSegment(y, leftX[0], rightX[0]);
                        } else {
                            rect = scanSegment(y, rightX[0], leftX[0]);
                        }
                        if (rect != null) {
                            rectList.addLast((Object)rect);
                        }
                    }
                    y++;
                    leftX[0] += leftDX[0];
                    rightX[0] += rightDX[0];
                }
            }

            return rectList;
        }

        /**
         * Return a <code>Rectangle</code> for the supplied line and
         * abscissa end points.
         *
         * @param y The line number.
         * @param leftX The left end point of the segment.
         * @param rightX The right end point of the segment.
         *
         * @return The run length <code>Rectangle</code> for the segment.
         */
        private Rectangle scanSegment(int y, double leftX, double rightX) {
            double x = leftX - 0.5;
            int xl = (x < clip.x) ? clip.x : (int)Math.ceil(x);
            int xr = (int)Math.floor(rightX - 0.5);
            if (xr >= clip.x + clip.width) xr = clip.x + clip.width - 1;
            if (xl > xr) return null;

            return new Rectangle(xl, y, xr - xl + 1, 1);
        }

        /**
         * For the line y + 0.5 calculate the intersection with the segment
         * (x1, y1) to (x2, y2) as well as the slope dx/dy at the point of
         * intersection.
         *
         * @param x1 Abscissa of first segment end point.
         * @param y1 Ordinate of first segment end point.
         * @param x2 Abscissa of second segment end point.
         * @param y2 Ordinate of second segment end point.
         * @param y The image line to intersect.
         * @param x The abscissa of the point of intersection.
         * @param dx The slope dx/dy of the point of intersection.
         */
        private void intersectX(double x1, int y1, double x2, int y2,
                                int y, double[] x, double[] dx) {
            int dy = y2 - y1;
            if (dy == 0) dy = 1;
            double frac = y - y1 + 0.5;

            dx[0] = (x2 - x1)/dy;
            x[0] = x1 + dx[0]*frac;
        }

        /**
         * Perform scan conversion of a concave polygon.
         *
         * @param rectList A <code>LinkedList</code>; may be null.
         *
         * @return A <code>LinkedList</code> of <code>Rectangle</code>s
         * representing the scan conversion of the concave polygon.
         */
        private LinkedList scanConcave(LinkedList rectList) {

            if (rectList == null) {
                rectList = new LinkedList();
            }

            int numVertices = poly.npoints;
            if (numVertices <= 0) return null;

            // Create y-sorted Vector of indices into vertex arrays.
            Vector indVector = new Vector();
            indVector.add(new Integer(0));
            for (int count = 1; count < numVertices; count++) {
                // Search entire array until
                // vertexY[index] >= vertexY[count]  or index == count.
                int index = 0;
                int value = poly.ypoints[count];
                while (index < count) {
                    int elt = ((Integer)(indVector.get(index))).intValue();
                    if (value <= poly.ypoints[elt]) break;
                    index++;
                }
                indVector.insertElementAt((Object)new Integer(count),
                                          index);
            }

            // Convert the Vector of indices to an array of same.
            int[] ind = vectorToIntArray(indVector);

            // Create a Vector of active edges.
            Vector activeEdges = new Vector(numVertices);

            // Initialize the range of lines to examine.
            int y0 = Math.max((int)clip.getMinY(),
                              (int)Math.ceil(poly.ypoints[ind[0]] - 0.5F));
            int y1 = Math.min((int)clip.getMaxY(),
                              (int)Math.floor(poly.ypoints[ind[numVertices-1]]
                                                          - 0.5F));

            // Loop over lines. The current line is at y + 0.5 in
            // continuous coordinates.
            int nextVertex = 0;
            for (int y = y0; y <= y1; y++) {
                // Check vertices between previous and current line.
                while (nextVertex < numVertices &&
                      poly.ypoints[ind[nextVertex]] <= y + 0.5F) {
                    int i = ind[nextVertex];

                    // Delete old (previous) edges and insert new
                    // (subsequent) edges if they cross current line.
                    int j = i > 0 ? i - 1 : numVertices - 1; // Previous vertex
                    if (poly.ypoints[j] <= y - 0.5F) {
                        deleteEdge(activeEdges, j);
                    } else if (poly.ypoints[j] > y + 0.5F) {
                        appendEdge(activeEdges, j, y);
                    }

                    j = i < numVertices - 1 ? i + 1 : 0; // Next vertex.
                    if (poly.ypoints[j] <= y - 0.5F) {
                        deleteEdge(activeEdges, i);
                    } else if (poly.ypoints[j] > y + 0.5F) {
                        appendEdge(activeEdges, i, y);
                    }

                    nextVertex++;
                }

                // Sort active edges by the edge X coordinate.
                Object[] edges = activeEdges.toArray();
                Arrays.sort(edges, (PolyEdge)edges[0]);

                // Extract run length Rectangles for current line.
                int numActive = activeEdges.size();
                for (int k = 0; k < numActive; k += 2) {
                    // Get left and right edges.
                    PolyEdge edge1 = (PolyEdge)edges[k];
                    PolyEdge edge2 = (PolyEdge)edges[k+1];

                    // Clip left end point.
                    int xl = (int)Math.ceil(edge1.x - 0.5);
                    if (xl < clip.getMinX()) {
                        xl = (int)clip.getMinX();
                    }

                    // Clip right end point.
                    int xr = (int)Math.floor(edge2.x - 0.5);
                    if (xr > clip.getMaxX()) {
                        xr = (int)clip.getMaxX();
                    }

                    // Create run length Rectangle.
                    if (xl <= xr) {
                        Rectangle r = new Rectangle(xl, y, xr - xl + 1, 1);
                        rectList.addLast((Object)r);
                    }

                    // Increment edge coordinates.
                    edge1.x += edge1.dx;
                    activeEdges.setElementAt((Object)edge1, k);
                    edge2.x += edge2.dx;
                    activeEdges.setElementAt((Object)edge2, k+1);
                }
            }

            return rectList;
        }

        /**
         * Delete a <code>PolyEdge</code> from the Vector of active edges.
         *
         * @param edges The <code>Vector</code> of <code>PolyEdge</code>s.
         * @param i The number of the edge to be deleted.
         */
        private void deleteEdge(Vector edges, int i) {
            int numActive = edges.size();
            int j;
            for (j = 0; j < numActive; j++) {
                PolyEdge edge = (PolyEdge)edges.get(j);
                if (edge.i == i) break;
            }
            if (j < numActive) {
                edges.removeElementAt(j);
            }
        }

        /**
         * Append a <code>PolyEdge</code> to the Vector of active edges.
         *
         * @param edges The <code>Vector</code> of <code>PolyEdge</code>s.
         * @param i The number of the edge to be appended.
         * @param y The y coordinate of the current scanline.
         */
        private void appendEdge(Vector edges, int i, int y) {
            int j = (i + 1)%poly.npoints;
            int ip;
            int iq;
            if (poly.ypoints[i] < poly.ypoints[j]) {
                ip = i;
                iq = j;
            } else {
                ip = j;
                iq = i;
            }
            double dx =
                (double)(poly.xpoints[iq] - poly.xpoints[ip])/
                (double)(poly.ypoints[iq] - poly.ypoints[ip]);
            double x = dx*(y + 0.5F - poly.ypoints[ip]) + poly.xpoints[ip];
            edges.add(new PolyEdge(x, dx, i));
        }

        /**
         * Convert an array of <code>int</code>s to an array of
         * <code>double</code>s.
         */
        private double[] intArrayToDoubleArray(int[] intArray) {
            int length = intArray.length;
            double[] doubleArray = new double[length];
            for (int i = 0; i < length; i++) {
                doubleArray[i] = intArray[i];
            }
            return doubleArray;
        }

        /**
         * Convert a <code>Vector</code> of <code>Integer</code>s to an array
         * of <code>int</code>s.
         *
         * @param vector A <code>Vector</code> of <code>Integer</code>s.
         *
         * @return The array of <code>int</code>s.
         */
        private int[] vectorToIntArray(Vector vector) {
            int size = vector.size();
            int[] array = new int[size];
            Object[] objects = vector.toArray();
            for (int i = 0; i < size; i++) {
                array[i] = ((Integer)objects[i]).intValue();
            }
            return array;
        }
    }
    
    /** Returns the bounds of the mask as a <code>Rectangle</code>. */
    public Rectangle getBounds() {
        return theShape.getBounds();
    }
    
    /** Returns the bounds of the mask as a <code>Rectangle2D</code>. */
    public Rectangle2D getBounds2D() {
        return theShape.getBounds2D();
    }
    
    /**
     * Returns <code>true</code> if the mask contains a given Point.
     *
     * @param p a Point specifying the coordinates of the pixel to be queried.
     * @return <code>true</code> if the pixel lies within the mask.
     *
     * @throws IllegalArgumentException is p is null.
     */
    public boolean contains(Point p) {
        if ( p == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return contains(p.x,p.y);
    }
    
    /**
     * Returns <code>true</code> if the mask contains a given Point2D.
     *
     * @param p A Point2D specifying the coordinates of the pixel
     *        to be queried.
     * @throws IllegalArgumentException is p is null.
     * @return <code>true</code> if the pixel lies within the mask.
     */
    public boolean contains(Point2D p) {
        if ( p == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return contains((int) p.getX(), (int) p.getY());
    }
    
    /**
     * Returns <code>true</code> if the mask contains the point (x, y).
     *
     * @param x An int specifying the X coordinate of the pixel to be queried.
     * @param y An int specifying the Y coordinate of the pixel to be queried.
     * @return <code>true</code> if the pixel lies within the mask.
     */
    public boolean contains(int x, int y) {
        return theShape.contains(x,y);
    }
    
    /**
     * Returns <code>true</code> if the mask contains the point (x, y).
     *
     * @param x A double specifying the X coordinate of the pixel
     *        to be queried.
     * @param y A double specifying the Y coordinate of the pixel
     *        to be queried.
     * @return <code>true</code> if the pixel lies within the mask.
     */
    public boolean contains(double x, double y) {
        return contains((int)x,(int)y);
    }

    /**
     * Returns <code>true</code> if a given <code>Rectangle</code> is
     * entirely included within the mask.
     *
     * @param rect A <code>Rectangle</code> specifying the region to
     *        be tested for inclusion.
     * @return <code>true</code> if the rectangle is
     *         entirely contained within the mask.
     * @throws IllegalArgumentException is rect is null.
     */
    public boolean contains(Rectangle rect) {
        if ( rect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return contains(new Rectangle2D.Float((float) rect.x,
                                              (float) rect.y,
                                              (float) rect.width,
                                              (float) rect.height));
    }
    
    /**
     * Returns <code>true</code> if a given <code>Rectangle2D</code>
     * is entirely included within the mask.
     *
     * @param rect A <code>Rectangle2D</code> specifying the region to
     *        be tested for inclusion.
     * @return <code>true</code> if the rectangle is entirely
     *         contained within the mask.
     * @throws IllegalArgumentException is rect is null.
     */
    public boolean contains(Rectangle2D rect) {
        if ( rect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return theShape.contains(rect);
    }
    
    /**
     * Returns <code>true</code> if a given rectangle (x, y, w, h) is entirely
     * included within the mask.
     *
     * @param x The int X coordinate of the upper left corner of the region.
     * @param y The int Y coordinate of the upper left corner of the region.
     * @param w The int width of the region.
     * @param h The int height of the region.
     * @return <code>true</code> if the rectangle is entirely
     *         contained within the mask.
     */
    public boolean contains(int x, int y, int w, int h) {
        return contains(new Rectangle2D.Float((float) x,
                                              (float) y,
                                              (float) w,
                                              (float) h));
    }
    
    /**
     * Returns <code>true</code> if a given rectangle (x, y, w, h) is entirely
     * included within the mask.
     *
     * @param x The double X coordinate of the upper left corner of the region.
     * @param y The double Y coordinate of the upper left corner of the region.
     * @param w The double width of the region.
     * @param h The double height of the region.
     * @return <code>true</code> if the rectangle is entirely contained
     *         within the mask.
     */
    public boolean contains(double x, double y, double w, double h) {
        return theShape.contains(x, y, w, h);
    }
    
    /**
     * Returns <code>true</code> if a given <code>Rectangle</code>
     * intersects the mask.
     *
     * @param r A <code>Rectangle</code> specifying the region to be tested for
     *        inclusion.
     * @return <code>true</code> if the rectangle intersects the mask.
     * @throws IllegalArgumentException is r is null.
     */
    public boolean intersects(Rectangle r) {
        if ( r == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return intersects(new Rectangle2D.Float((float) r.x,
                                                (float) r.y,
                                                (float) r.width,
                                                (float) r.height));
    }
    
    /**
     * Returns <code>true</code> if a given <code>Rectangle2D</code>
     * intersects the mask.
     *
     * @param r A <code>Rectangle2D</code> specifying the region to be
     *        tested for inclusion.
     * @return <code>true</code> if the rectangle intersects the mask.
     * @throws IllegalArgumentException is r is null.
     */
    public boolean intersects(Rectangle2D r) {
        if ( r == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return theShape.intersects(r);
    }
    
    /**
     * Returns <code>true</code> if a given rectangle (x, y, w, h)
     * intersects the mask.
     *
     * @param x The int X coordinate of the upper left corner of the region.
     * @param y The int Y coordinate of the upper left corner of the region.
     * @param w The int width of the region.
     * @param h The int height of the region.
     * @return <code>true</code> if the rectangle intersects the mask.
     */
    public boolean intersects(int x, int y, int w, int h) {
        return intersects(new Rectangle2D.Float((float) x,
                                                (float) y,
                                                (float) w,
                                                (float) h));
    }
    
    /**
     * Returns <code>true</code> if a given rectangle (x, y, w, h)
     * intersects the mask.
     *
     * @param x The double X coordinate of the upper left corner of the region.
     * @param y The double Y coordinate of the upper left corner of the region.
     * @param w The double width of the region.
     * @param h The double height of the region.
     * @return <code>true</code> if the rectangle intersects the mask.
     */
    public boolean intersects(double x, double y, double w, double h)  {
        return theShape.intersects(x, y, w, h);
    }
    
    /** 
     * Adds another mask to this one.
     * This operation may force this mask to be rendered.
     *
     * @param roi A ROI.
     * @throws IllegalArgumentException is roi is null.
     */
    public ROI add(ROI roi) {

	if (roi == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("ROIShape3"));
	}

        if (!(roi instanceof ROIShape)) {
            return super.add(roi);
        } else {
            ROIShape rois = (ROIShape) roi;
            Area a1 = new Area(theShape);
            Area a2 = new Area(rois.theShape);
            a1.add(a2);
            return new ROIShape(a1);
        }
    }
  
    /** 
     * Subtracts another mask from this one.
     * This operation may force this mask to be rendered.
     *
     * @param roi A ROI.
     * @throws IllegalArgumentException is roi is null.
     */
    public ROI subtract(ROI roi) {

	if (roi == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("ROIShape3"));
	}

        if (!(roi instanceof ROIShape)) {
            return super.subtract(roi);
        } else {
            ROIShape rois = (ROIShape)roi;
            Area a1 = new Area(theShape);
            Area a2 = new Area(rois.theShape);
            a1.subtract(a2);
            return new ROIShape(a1);
        }
    }
    
    /** 
     * Sets the mask to its intersection with another mask.
     * This operation may force this mask to be rendered.
     *
     * @param roi A ROI.
     * @throws IllegalArgumentException is roi is null.
     */
    public ROI intersect(ROI roi) {

	if (roi == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("ROIShape3"));
	}

        if (!(roi instanceof ROIShape)) {
            return super.intersect(roi);
        } else {
            ROIShape rois = (ROIShape)roi;
            Area a1 = new Area(theShape);
            Area a2 = new Area(rois.theShape);
            a1.intersect(a2);
            return new ROIShape(a1);
        }
    }
  
    /** 
     * Sets the mask to its exclusive-or with another mask.
     * This operation may force this mask to be rendered.
     *
     * @param roi A ROI.
     * @throws IllegalArgumentException is roi is null.
     */
    public ROI exclusiveOr(ROI roi) {

	if (roi == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("ROIShape3"));
	}

        if (!(roi instanceof ROIShape)) {
            return super.exclusiveOr(roi);
        } else {
            ROIShape rois = (ROIShape)roi;
            Area a1 = new Area(theShape);
            Area a2 = new Area(rois.theShape);
            a1.exclusiveOr(a2);
            return new ROIShape(a1);
        }
    }

    /**
     * Returns the internal Shape representation or null if a shape 
     * representation is not possible.  
     */
    public Shape getAsShape() {
        return theShape;
    }
    
    /**
     * Returns the shape as a <code>PlanarImage</code>. This requires
     * performing an antialiased rendering of the internal Shape.
     *
     * @return If the upper-left corner of the bounds of this
     * <code>ROIShape</code> is (0, 0), the returned image is a
     * <code>BufferedImage</code> of type TYPE_BYTE_BINARY wrapped as
     * a <code>PlanarImage</code>. Otherwise, the returned image is a
     * (bilevel) <code>TiledImage</code> whose <code>SampleModel</code>
     * is an instance of <code>MultiPixelPackedSampleModel</code>.
     */
    public PlanarImage getAsImage() {

        if (theImage != null)
	    return theImage;

	Rectangle r = theShape.getBounds();

	PlanarImage pi;
	Graphics2D g2d;

	if ((r.x == 0) && (r.y == 0)) {

	    BufferedImage bi =
		    new BufferedImage(r.width, r.height,
			    BufferedImage.TYPE_BYTE_BINARY);

	    pi = PlanarImage.wrapRenderedImage(bi);
	    g2d = bi.createGraphics();
	} else {

	    SampleModel sm =
		new MultiPixelPackedSampleModel(
			DataBuffer.TYPE_BYTE, r.width, r.height, 1);

	    // Create a TiledImage into which to write.
	    TiledImage ti = new TiledImage(
			    r.x, r.y, r.width, r.height, r.x, r.y,
			    sm, PlanarImage.createColorModel(sm));

	    // Create the associated TiledImageGraphics.
	    pi  = ti;
	    g2d = ti.createGraphics();
	}

	// Write the Shape into the TiledImageGraphics.
	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			     RenderingHints.VALUE_ANTIALIAS_ON);
	g2d.fill(theShape);

	theImage = pi; // Cache the output
        
        return theImage;
    }

    /**
     * Transforms the current contents of the <code>ROI</code> by a
     * given <code>AffineTransform</code>.
     *
     * @param at An <code>AffineTransform</code> object.
     * @throws IllegalArgumentException if at is null.
     */
    public ROI transform(AffineTransform at) {
        if ( at == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return new ROIShape(at.createTransformedShape(theShape));
    }

    /**
     * Returns a bitmask for a given rectangular region of the ROI
     * indicating whether the pixel is included in the region of
     * interest.  The results are packed into 32-bit integers, with
     * the MSB considered to lie on the left.  The last entry in each
     * row of the result may have bits that lie outside of the
     * requested rectangle.  These bits are guaranteed to be zeroed.
     *
     * <p> The <code>mask</code> array, if supplied, must be of length
     * equal to or greater than <code>height</code> and each of its
     * subarrays must have length equal to or greater than (width +
     * 31)/32.  If <code>null</code> is passed in, a suitable array
     * will be constructed.  If the mask is non-null but has
     * insufficient size, an exception will be thrown.
     *
     * @param x The X coordinate of the upper left corner of the rectangle.
     * @param y The Y coordinate of the upper left corner of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @param mask A two-dimensional array of ints at least
     *        (width + 31)/32 entries wide and (height) entries tall,
     *        or null.
     * @return A reference to the <code>mask</code> parameter, or
     *         to a newly constructed array if <code>mask</code> is
     *         <code>null</code>.
     */
    public int[][] getAsBitmask(int x, int y,
                                int width, int height,
                                int[][] mask) {
        // Get the un-merged Rectangle list (run length Rectangles only).
        LinkedList rectList = getAsRectangleList(x, y, width, height, false);

        if (rectList == null) {
            return null;
        }

        // Convert the Rectangle list to a bit mask.
        return rectangleListToBitmask(rectList,
                                      new Rectangle(x, y, width, height),
                                      mask);
    }

    /**
     * Returns a <code>LinkedList</code> of <code>Rectangle</code>s
     * for a given rectangular region of the ROI. The
     * <code>Rectangle</code>s in the list are merged into a minimal
     * set.
     *
     * @param x The X coordinate of the upper left corner of the rectangle.
     * @param y The Y coordinate of the upper left corner of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @return A <code>LinkedList</code> of <code>Rectangle</code>s.
     */
    public LinkedList getAsRectangleList(int x, int y,
                                         int width, int height) {
        return getAsRectangleList(x, y, width, height, true);
    }

    /**
     * Returns a <code>LinkedList</code> of <code>Rectangle</code>s for
     * a given rectangular region of the ROI.
     *
     * @param x The X coordinate of the upper left corner of the rectangle.
     * @param y The Y coordinate of the upper left corner of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @param mergeRectangles <code>true</code> if the <code>Rectangle</code>s
     *        are to be merged into a minimal set.
     * @return A <code>LinkedList</code> of <code>Rectangle</code>s.
     */
    protected LinkedList getAsRectangleList(int x, int y,
                                            int width, int height,
                                            boolean mergeRectangles) {
        LinkedList rectangleList = null;

        // Create a clipping Rectangle.
        Rectangle clip = new Rectangle(x, y, width, height);

        /// XXX bpb 1998/09/28 Is it really necessary to use Area.intersects()?
        /// Note that the Polygon.intersects() method appears not to be
        /// implemented. Shape.intersects() also appeared not to work in
        /// testing the trivial case below.
        if (!((new Area(theShape)).intersects(clip))) { // Null overlap.
            return null;
        } else if (theShape instanceof Rectangle2D) { // Trivial case.
            // Return a list consisting of a single Rectangle which is the
            // intersection of the clipping Rectangle with the internal
            // Shape of the ROIShape rounded to integer coordinates.
            Rectangle2D.Double dstRect = new Rectangle2D.Double();
            Rectangle2D.intersect((Rectangle2D)theShape, clip, dstRect);
            int rectX = (int)Math.round(dstRect.getMinX());
            int rectY = (int)Math.round(dstRect.getMinY());
            int rectW = (int)Math.round(dstRect.getMaxX() - rectX);
            int rectH = (int)Math.round(dstRect.getMaxY() - rectY);
            rectangleList = new LinkedList();
            rectangleList.addLast((Object)new Rectangle(rectX, rectY,
                                                        rectW, rectH));
        } else if (theShape instanceof Polygon) { // Polygon.
            rectangleList = polygonToRunLengthList(clip, (Polygon)theShape);
            if (mergeRectangles && rectangleList != null) {
                rectangleList = mergeRunLengthList(rectangleList);
            }
        } else { // Generic case.
            // Get the corresponding PlanarImage.
            getAsImage();

            // Call the super-class method.
            rectangleList =
                super.getAsRectangleList(x, y, width, height, mergeRectangles);
        }

        return rectangleList;
    }

    /**
      * Serialize the <code>ROIShape</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Create a serializable form of the Shape.
        LinkedList rectList = null;
        if (theShape == null) {
            rectList = new LinkedList(); // zero-size LinkedList
        } else {
            Rectangle r = getBounds();
            rectList = getAsRectangleList(r.x, r.y, r.width, r.height);
        }

        // Write serialized form to the stream.
        out.defaultWriteObject();
        out.writeObject(rectList);
    }

    /**
      * Deserialize the <code>ROIShape</code>.
      *
      * @param in The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        
        // Read serialized form from the stream.
        LinkedList rectList = null;
        in.defaultReadObject();
        rectList = (LinkedList)in.readObject();

        // Restore the transient Shape as an Area.
        Area a = new Area();
        int listSize = rectList.size();
        for (int i = 0; i < listSize; i++) {
            a.add(new Area((Rectangle)rectList.get(i)));
        }
        theShape = a;
    }
}
