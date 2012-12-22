/*
 * $RCSfile: MedianCutOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/05/10 01:03:22 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.PixelAccessor;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.ROIShape;
import com.lightcrafts.mediax.jai.UnpackedImageData;

/**
 * An <code>OpImage</code> implementing the "ColorQuantizer" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.ExtremaDescriptor</code>
 * based on the median-cut algorithm.
 *
 * @see com.lightcrafts.mediax.jai.operator.ExtremaDescriptor
 * @see ExtremaCRIF
 */
public class MedianCutOpImage extends ColorQuantizerOpImage {
    /** The size of the histogram. */
    private int histogramSize;

    /** The counts of the colors. */
    private int[] counts;

    /** The colors for the color histogram. */
    private int[] colors;

    /** The partition of the RGB color space.  Each cube contains one
     *  cluster.
     */
    private Cube[] partition;

    /** The maximum number of bits to contains 32768 colors. */
    private int bits = 8;

    /** The mask to generate the low bits colors from the original colors. */
    private int mask;

    /** The histgram hash. */
    HistogramHash histogram;

    /**
     * Constructs an <code>MedianCutOpImage</code>.
     *
     * @param source  The source image.
     */
    public MedianCutOpImage(RenderedImage source,
                            Map config,
                            ImageLayout layout,
                            int maxColorNum,
                            int upperBound,
                            ROI roi,
                            int xPeriod,
                            int yPeriod) {
        super(source, config, layout, maxColorNum, roi, xPeriod, yPeriod);

        colorMap = null;
        this.histogramSize = upperBound;
    }

    protected synchronized void train() {
        PlanarImage source = getSourceImage(0);
        if (roi == null)
            roi = new ROIShape(source.getBounds());

        // Cycle throw all source tiles.
        int minTileX = source.getMinTileX();
        int maxTileX = source.getMaxTileX();
        int minTileY = source.getMinTileY();
        int maxTileY = source.getMaxTileY();
        int xStart = source.getMinX();
        int yStart = source.getMinY();

        histogram = new HistogramHash(histogramSize);

        while(true) {
            histogram.init();
            int oldbits = bits;
            mask = (255 << 8 - bits) & 255;
            mask = mask | (mask << 8) | (mask << 16);

            for (int y = minTileY; y <= maxTileY; y++) {
                for (int x = minTileX; x <= maxTileX; x++) {
                    // Determine the required region of this tile.
                    // (Note that getTileRect() instersects tile and
                    // image bounds.)
                    Rectangle tileRect = source.getTileRect(x, y);

                    // Process if and only if within ROI bounds.
                    if (roi.intersects(tileRect)) {

                        // If checking for skipped tiles determine
                        // whether this tile is "hit".
                        if (checkForSkippedTiles &&
                            tileRect.x >= xStart &&
                            tileRect.y >= yStart) {
                            // Determine the offset within the tile.
                            int offsetX =
                                (xPeriod - ((tileRect.x - xStart) % xPeriod)) %
                                xPeriod;
                            int offsetY =
                                (yPeriod - ((tileRect.y - yStart) % yPeriod)) %
                                yPeriod;

                            // Continue with next tile if offset
                            // is larger than either tile dimension.
                            if (offsetX >= tileRect.width ||
                                offsetY >= tileRect.height) {
                                continue;
                            }
                        }

                        // add the histogram.
                        computeHistogram(source.getData(tileRect));
                        if (histogram.isFull())
                            break;
                    }
                }

                if (histogram.isFull())
                    break;
            }


            if (oldbits == bits) {
                counts = histogram.getCounts();
                colors = histogram.getColors();
                break;
            }
        }

        medianCut(maxColorNum);
        setProperty("LUT", colorMap);
        setProperty("JAI.LookupTable", colorMap);
    }

    private void computeHistogram(Raster source) {
        if(!isInitialized) {
            srcPA = new PixelAccessor(getSourceImage(0));
            srcSampleType = srcPA.sampleType == PixelAccessor.TYPE_BIT ?
                DataBuffer.TYPE_BYTE : srcPA.sampleType;
            isInitialized = true;
        }

        Rectangle srcBounds = getSourceImage(0).getBounds().intersection(
                                                  source.getBounds());

        LinkedList rectList;
        if (roi == null) {	// ROI is the whole Raster
            rectList = new LinkedList();
            rectList.addLast(srcBounds);
        } else {
            rectList = roi.getAsRectangleList(srcBounds.x,
                                              srcBounds.y,
                                              srcBounds.width,
                                              srcBounds.height);
            if (rectList == null) {
                return; // ROI does not intersect with Raster boundary.
            }
        }

        ListIterator iterator = rectList.listIterator(0);
        int xStart = source.getMinX();
        int yStart = source.getMinY();

        while (iterator.hasNext()) {
            Rectangle rect = srcBounds.intersection((Rectangle)iterator.next());
            int tx = rect.x;
            int ty = rect.y;

            // Find the actual ROI based on start and period.
            rect.x = startPosition(tx, xStart, xPeriod);
            rect.y = startPosition(ty, yStart, yPeriod);
            rect.width = tx + rect.width - rect.x;
            rect.height = ty + rect.height - rect.y;

            if (rect.isEmpty()) {
                continue;	// no pixel to count in this rectangle
            }

            UnpackedImageData uid = srcPA.getPixels(source, rect,
                                                    srcSampleType, false);
            switch (uid.type) {
            case DataBuffer.TYPE_BYTE:
                computeHistogramByte(uid);
                break;
            }
        }
    }

    private void computeHistogramByte(UnpackedImageData uid) {
        Rectangle rect = uid.rect;
        byte[][] data = uid.getByteData();
        int lineStride = uid.lineStride;
        int pixelStride = uid.pixelStride;
        byte[] rBand = data[0];
        byte[] gBand = data[1];
        byte[] bBand = data[2];

        int lineInc = lineStride * yPeriod;
        int pixelInc = pixelStride * xPeriod;

        int lastLine = rect.height * lineStride;

        for (int lo = 0; lo < lastLine; lo += lineInc) {
            int lastPixel = lo + rect.width * pixelStride;

            for (int po = lo; po < lastPixel; po += pixelInc) {
                int p = ((rBand[po + uid.bandOffsets[0]] & 0xff)<<16) |
                        ((gBand[po + uid.bandOffsets[1]] & 0xff) <<8) |
                        (bBand[po + uid.bandOffsets[2]] & 0xff);
                if (!histogram.insert(p & mask)) {
                    bits--;
                    return;
                }
            }
        }
    }

    /** Applies the Heckbert's median-cut algorithm to partition the color
     *  space into <code>maxcubes</code> cubes. The centroids
     *  of each cube are are used to create a color table.
     */
    public void medianCut(int expectedColorNum) {
        int k;
        int num, width;

        Cube cubeA, cubeB;

        // Creates the first color cube
        partition = new Cube[expectedColorNum];
        int numCubes = 0;
        Cube cube = new Cube();
        int numColors = 0;
        for (int i=0; i < histogramSize; i++) {
            if (counts[i] != 0) {
                numColors++;
                cube.count = cube.count + counts[i];
            }
        }

        cube.lower = 0; cube.upper = numColors-1;
        cube.level = 0;
        shrinkCube(cube);
        partition[numCubes++] = cube;

        //Partition the cubes until the expected number of cubes are reached, or
        // cannot further partition
        while (numCubes < expectedColorNum) {
            // Search the list of cubes for next cube to split, the lowest level cube
            int level = 255;
            int splitableCube = -1;

            for (k=0; k < numCubes; k++) {
                if (partition[k].lower != partition[k].upper
                    && partition[k].level < level) {
                    level = partition[k].level;
                    splitableCube = k;
                }
            }

            // no more cubes to split
            if (splitableCube == -1)
                break;

            // Find longest dimension of this cube: 0 - red, 1 - green, 2 - blue
            cube = partition[splitableCube];
	    level = cube.level;

            // Weigted with luminosities
            int lr = 77 * (cube.rmax - cube.rmin);
            int lg = 150 * (cube.gmax - cube.gmin);
            int lb = 29 * (cube.bmax - cube.bmin);

            int longDimMask = 0;
            if (lr >= lg && lr >= lb) longDimMask = 0xFF0000;
            if (lg >= lr && lg >= lb) longDimMask = 0xFF00;
            if (lb >= lr && lb >= lg) longDimMask = 0xFF;

            // Sort along "longdim"
            quickSort(colors, cube.lower, cube.upper, longDimMask);

            // Find median
            int count = 0;
            int median = cube.lower;
            for (; median <= cube.upper - 1; median++) {
                if (count >= cube.count/2) break;
                count = count + counts[median];
            }

            // Now split "cube" at the median and add the two new
            // cubes to the list of cubes.
            cubeA = new Cube();
            cubeA.lower = cube.lower;
            cubeA.upper = median-1;
            cubeA.count = count;
            cubeA.level = cube.level + 1;
            shrinkCube(cubeA);
            partition[splitableCube] = cubeA;             // add in old slot

            cubeB = new Cube();
            cubeB.lower = median;
            cubeB.upper = cube.upper;
            cubeB.count = cube.count - count;
            cubeB.level = cube.level + 1;
            shrinkCube(cubeB);
            partition[numCubes++] = cubeB;             // add in new slot */
        }

        // creates the lookup table and the inverse mapping
        createLUT(numCubes);
    }

    /** Shrinks the provided <code>Cube</code> to a smallest contains
     *  the same colors defined in the histogram.
     */
    private void shrinkCube(Cube cube) {
        int rmin = 255;
        int rmax = 0;
        int gmin = 255;
        int gmax = 0;
        int bmin = 255;
        int bmax = 0;
        for (int i=cube.lower; i<=cube.upper; i++) {
            int color = colors[i];
            int r = color >> 16;
            int g = (color >> 8) & 255;
            int b = color & 255;
            if (r > rmax) rmax = r;
            else if (r < rmin) rmin = r;

            if (g > gmax) gmax = g;
            else if (g < gmin) gmin = g;

            if (b > bmax) bmax = b;
            else if (b < bmin) bmin = b;
        }

        cube.rmin = rmin; cube.rmax = rmax;
        cube.gmin = gmin; cube.gmax = gmax;
        cube.bmin = bmin; cube.bmax = bmax;
    }


    /** Creates the lookup table and computes the inverse mapping. */
    private void createLUT(int ncubes) {
        if (colorMap == null) {
            colorMap = new LookupTableJAI(new byte[3][ncubes]);
        }

        byte[] rLUT = colorMap.getByteData(0);
        byte[] gLUT = colorMap.getByteData(1);
        byte[] bLUT = colorMap.getByteData(2);

        float scale = 255.0f / (mask & 255);

        for (int k=0; k < ncubes; k++) {
            Cube cube = partition[k];
            float rsum = 0.0f, gsum = 0.0f, bsum = 0.0f;
            int r, g, b;
            for (int i=cube.lower; i<=cube.upper; i++) {
                int color = colors[i];
                r = color >> 16;
                rsum += (float)r*(float)counts[i];
                g = (color >> 8) & 255;
                gsum += (float)g*(float)counts[i];
                b = color & 255;
                bsum += (float)b*(float)counts[i];
            }

            // Update the color map
            rLUT[k] = (byte)(rsum/(float)cube.count * scale);
            gLUT[k] = (byte)(gsum/(float)cube.count * scale);
            bLUT[k] = (byte)(bsum/(float)cube.count * scale);
        }
    }

    void quickSort(int a[], int lo0, int hi0, int longDimMask) {
   // Based on the QuickSort method by James Gosling from Sun's SortDemo applet

      int lo = lo0;
      int hi = hi0;
      int mid, t;

      if ( hi0 > lo0) {
         mid = a[ ( lo0 + hi0 ) / 2 ] & longDimMask;
         while( lo <= hi ) {
            while( ( lo < hi0 ) && ( (a[lo] & longDimMask) < mid ) )
               ++lo;
            while( ( hi > lo0 ) && ( (a[hi] & longDimMask) > mid ) )
               --hi;
            if( lo <= hi ) {
              t = a[lo];
              a[lo] = a[hi];
              a[hi] = t;

              t = counts[lo];
              counts[lo] = counts[hi];
              counts[hi] = t;

               ++lo;
               --hi;
            }
         }
         if( lo0 < hi )
            quickSort( a, lo0, hi, longDimMask);
         if( lo < hi0 )
            quickSort( a, lo, hi0, longDimMask);
      }
   }
}

class Cube {            // structure for a cube in color space
    int  lower;         // one corner's index in histogram
    int  upper;         // another corner's index in histogram
    int  count;         // cube's histogram count
    int  level;         // cube's level
    int  rmin, rmax;
    int  gmin, gmax;
    int  bmin, bmax;

    Cube() {
        count = 0;
    }
}

/** A hash table for the color histogram.  This is based on the first
 *  hashtable algorithm I learnt.
 */
class HistogramHash {
    int capacity;
    int[] colors;
    int[] counts;
    int size;
    int hashsize;
    boolean packed = false;
    int[] newColors;
    int[] newCounts;

    public HistogramHash(int capacity) {
        this.capacity = capacity;
        this.hashsize = capacity * 4 / 3;
        this.colors = new int[hashsize];
        this.counts = new int[hashsize];
    }

    void init() {
        this.size = 0;
        this.packed = false;
        for (int i = 0; i < hashsize; i++) {
            colors[i] = -1;
            counts[i] = 0;
        }
    }

    boolean insert(int node) {
        int hashPos = hashCode(node);
        if (colors[hashPos] == -1) {
            colors[hashPos] = node;
            counts[hashPos]++;
            size++;
            return size <= capacity;
        } else if (colors[hashPos] == node) {
            counts[hashPos]++;
            return size <= capacity;
        } else {
            for (int next = hashPos + 1; next != hashPos; next++) {
                next %= hashsize;
                if (colors[next] == -1) {
                    colors[next] = node;
                    counts[next]++;
                    size++;
                    return size <= capacity;
                } else if (colors[next] == node) {
                    counts[next]++;
                    return size <= capacity;
                }
            }
        }
        return size <= capacity;
    }

    boolean isFull() {
        return size > capacity;
    }

    void put(int node, int value) {
        int hashPos = hashCode(node);
        if (colors[hashPos] == -1) {
            colors[hashPos] = node;
            counts[hashPos] = value;
            size++;
            return;
        } else if (colors[hashPos] == node) {
            counts[hashPos] = value;
            return;
        } else {
            for (int next = hashPos + 1; next != hashPos; next++) {
                next %= hashsize;
                if (colors[next] == -1) {
                    colors[next] = node;
                    counts[next] = value;
                    size++;
                    return;
                } else if (colors[next] == node) {
                    counts[next] = value;
                    return;
                }
            }
        }
        return;
    }

    int get(int node) {
        int hashPos = hashCode(node);
        if (colors[hashPos] == node) {
            return counts[hashPos];
        } else {
            for (int next = hashPos + 1; next != hashPos; next++) {
                next %= hashsize;
                if (colors[next] == node) {
                    return counts[next];
                }
            }
        }
        return -1;
    }

    int[] getCounts() {
        if (!packed)
            pack();
        return newCounts;
    }

    int[] getColors() {
        if (!packed)
            pack();
        return newColors;
    }

    void pack() {
        newColors = new int[capacity];
        newCounts = new int[capacity];

        for (int i = 0, j = 0; i < hashsize; i++) {
            if (colors[i] != -1) {
                newColors[j] = colors[i];
                newCounts[j] = counts[i];
                j++;
            }
        }

        packed = true;
    }

    int hashCode(int value) {
        return ((value >> 16) * 33023 + ((value >> 8) & 255) * 30013
                + (value & 255) * 27011) % hashsize ;

    }
}
