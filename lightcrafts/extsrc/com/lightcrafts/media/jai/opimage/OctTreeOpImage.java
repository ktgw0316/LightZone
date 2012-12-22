/*
 * $RCSfile: OctTreeOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/05/10 01:03:23 $
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
 * based on the oct-tree algorithm.
 *
 * An efficient color quantization algorithm, adapted from the C++
 * implementation quantize.c in <a
 * href="http://www.imagemagick.org/">ImageMagick</a>. The pixels for
 * an image are placed into an oct tree. The oct tree is reduced in
 * size, and the pixels from the original image are reassigned to the
 * nodes in the reduced tree.<p>
 *
 * Here is the copyright notice from ImageMagick:
 *
 * <pre>
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%  Permission is hereby granted, free of charge, to any person obtaining a    %
%  copy of this software and associated documentation files ("ImageMagick"),  %
%  to deal in ImageMagick without restriction, including without limitation   %
%  the rights to use, copy, modify, merge, publish, distribute, sublicense,   %
%  and/or sell copies of ImageMagick, and to permit persons to whom the       %
%  ImageMagick is furnished to do so, subject to the following conditions:    %
%                                                                             %
%  The above copyright notice and this permission notice shall be included in %
%  all copies or substantial portions of ImageMagick.                         %
%                                                                             %
%  The software is provided "as is", without warranty of any kind, express or %
%  implied, including but not limited to the warranties of merchantability,   %
%  fitness for a particular purpose and noninfringement.  In no event shall   %
%  E. I. du Pont de Nemours and Company be liable for any claim, damages or   %
%  other liability, whether in an action of contract, tort or otherwise,      %
%  arising from, out of or in connection with ImageMagick or the use or other %
%  dealings in ImageMagick.                                                   %
%                                                                             %
%  Except as contained in this notice, the name of the E. I. du Pont de       %
%  Nemours and Company shall not be used in advertising or otherwise to       %
%  promote the sale, use or other dealings in ImageMagick without prior       %
%  written authorization from the E. I. du Pont de Nemours and Company.       %
%                                                                             %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
</pre>
 *
 * In this <code>OpImage</code>, two significant bugs in the original code are
 * fix: (1) The computation of tree depth; (2) The computation of the pixels
 * on each node.
 *
 * @see com.lightcrafts.mediax.jai.operator.ExtremaDescriptor
 * @see ExtremaCRIF
 */
public class OctTreeOpImage extends ColorQuantizerOpImage {
    /** The size of the histogram. */
    private int treeSize;

    private int maxTreeDepth = 8;

    // these are precomputed in advance
    private int squares[];

    {
        squares = new int[(maxColorNum << 1) + 1];
        for (int i= -maxColorNum; i <= maxColorNum; i++) {
            squares[i + maxColorNum] = i * i;
        }
    }

    /**
     * Constructs an <code>OctTreeOpImage</code>.
     *
     * @param source  The source image.
     */
    public OctTreeOpImage(RenderedImage source,
                            Map config,
                            ImageLayout layout,
                            int maxColorNum,
                            int upperBound,
                            ROI roi,
                            int xPeriod,
                            int yPeriod) {
        super(source, config, layout, maxColorNum, roi, xPeriod, yPeriod);

        colorMap = null;
        this.treeSize = upperBound;
    }

    protected synchronized void train() {
        Cube cube = new Cube(getSourceImage(0), maxColorNum);
        cube.constructTree();
        cube.reduction();
        cube.assignment();

        colorMap = new LookupTableJAI(cube.colormap);
        setProperty("LUT", colorMap);
        setProperty("JAI.LookupTable", colorMap);
    }

    class Cube {
        PlanarImage source;
        int max_colors;
        byte[][] colormap = new byte[3][];

        Node root;
        int depth;

        // counter for the number of colors in the cube. this gets
        // recalculated often.
        int colors;

        // counter for the number of nodes in the tree
        int nodes;

        Cube(PlanarImage source, int max_colors) {
            this.source = source;
            this.max_colors = max_colors;

            int i = max_colors;
            // tree_depth = log max_colors
            //                 2
            for (depth = 0; i != 0; depth++) {
                i >>>= 1;
            }

            if (depth > maxTreeDepth) {
                depth = maxTreeDepth;
            } else if (depth < 2) {
                depth = 2;
            }

            root = new Node(this);
        }

        void constructTree() {
            if (roi == null)
                roi = new ROIShape(source.getBounds());

            // Cycle throw all source tiles.
            int minTileX = source.getMinTileX();
            int maxTileX = source.getMaxTileX();
            int minTileY = source.getMinTileY();
            int maxTileY = source.getMaxTileY();
            int xStart = source.getMinX();
            int yStart = source.getMinY();

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
                        // construct the tree
                        constructTree(source.getData(tileRect));
                    }
                }
            }
        }

        private void constructTree(Raster source) {
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
                    constructTreeByte(uid);
                    break;
                }
            }
        }

        /*
         * Procedure Classification begins by initializing a color
         * description tree of sufficient depth to represent each
         * possible input color in a leaf. However, it is impractical
         * to generate a fully-formed color description tree in the
         * classification phase for realistic values of cmax. If
         * colors components in the input image are quantized to k-bit
         * precision, so that cmax= 2k-1, the tree would need k levels
         * below the root node to allow representing each possible
         * input color in a leaf. This becomes prohibitive because the
         * tree's total number of nodes is 1 + sum(i=1,k,8k).
         *
         * A complete tree would require 19,173,961 nodes for k = 8,
         * cmax = 255. Therefore, to avoid building a fully populated
         * tree, QUANTIZE: (1) Initializes data structures for nodes
         * only as they are needed; (2) Chooses a maximum depth for
         * the tree as a function of the desired number of colors in
         * the output image (currently log2(colormap size)).
         *
         * For each pixel in the input image, classification scans
         * downward from the root of the color description tree. At
         * each level of the tree it identifies the single node which
         * represents a cube in RGB space containing It updates the
         * following data for each such node:
         *
         *   number_pixels : Number of pixels whose color is contained
         *   in the RGB cube which this node represents;
         *
         *   unique : Number of pixels whose color is not represented
         *   in a node at lower depth in the tree; initially, n2 = 0
         *   for all nodes except leaves of the tree.
         *
         *   total_red/green/blue : Sums of the red, green, and blue
         *   component values for all pixels not classified at a lower
         *   depth. The combination of these sums and n2 will
         *   ultimately characterize the mean color of a set of pixels
         *   represented by this node.
         */
        private void constructTreeByte(UnpackedImageData uid) {
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
                    int red   = rBand[po + uid.bandOffsets[0]] & 0xff;
                    int green = gBand[po + uid.bandOffsets[1]] & 0xff;
                    int blue  = bBand[po + uid.bandOffsets[2]] & 0xff;

                    // a hard limit on the number of nodes in the tree
                    if (nodes > treeSize) {
                        root.pruneLevel();
                        --depth;
                    }

                    // walk the tree to depth, increasing the
                    // number_pixels count for each node
                    Node node = root;
                    for (int level = 1; level <= depth; ++level) {
                        int id = ((red   > node.mid_red   ? 1 : 0) |
                                  ((green > node.mid_green ? 1 : 0) << 1) |
                                  ((blue  > node.mid_blue  ? 1 : 0) << 2));
                        if (node.child[id] == null) {
                            node = new Node(node, id, level);
                        } else
                            node = node.child[id];
                        node.number_pixels ++;
                    }

                    ++node.unique;
                    node.total_red   += red;
                    node.total_green += green;
                    node.total_blue  += blue;
                }
            }
        }

        /*
         * reduction repeatedly prunes the tree until the number of
         * nodes with unique > 0 is less than or equal to the maximum
         * number of colors allowed in the output image.
         *
         * When a node to be pruned has offspring, the pruning
         * procedure invokes itself recursively in order to prune the
         * tree from the leaves upward.  The statistics of the node
         * being pruned are always added to the corresponding data in
         * that node's parent.  This retains the pruned node's color
         * characteristics for later averaging.
         */
        void reduction() {
            int totalSamples = (source.getWidth() + xPeriod -1) / xPeriod *
                              (source.getHeight() + yPeriod -1) / yPeriod;
            int threshold = Math.max(1,  totalSamples/ (max_colors * 8));
            while (colors > max_colors) {
                colors = 0;
                threshold = root.reduce(threshold, Integer.MAX_VALUE);
            }
        }

        /*
         * Procedure assignment generates the output image from the
         * pruned tree. The output image consists of two parts: (1) A
         * color map, which is an array of color descriptions (RGB
         * triples) for each color present in the output image; (2) A
         * pixel array, which represents each pixel as an index into
         * the color map array.
         *
         * First, the assignment phase makes one pass over the pruned
         * color description tree to establish the image's color map.
         * For each node with n2 > 0, it divides Sr, Sg, and Sb by n2.
         * This produces the mean color of all pixels that classify no
         * lower than this node. Each of these colors becomes an entry
         * in the color map.
         *
         * Finally, the assignment phase reclassifies each pixel in
         * the pruned tree to identify the deepest node containing the
         * pixel's color. The pixel's value in the pixel array becomes
         * the index of this node's mean color in the color map.
         */
        void assignment() {
            colormap = new byte[3][colors];

            colors = 0;
            root.colormap();
        }

        /**
         * A single Node in the tree.
         */
        class Node {
            Cube cube;

            // parent node
            Node parent;

            // child nodes
            Node child[];
            int nchild;

            // our index within our parent
            int id;
            // our level within the tree
            int level;
            // our color midpoint
            int mid_red;
            int mid_green;
            int mid_blue;

            // the pixel count for this node and all children
            int number_pixels;

            // the pixel count for this node
            int unique;
            // the sum of all pixels contained in this node
            int total_red;
            int total_green;
            int total_blue;

            // used to build the colormap
            int color_number;

            Node(Cube cube) {
                this.cube = cube;
                this.parent = this;
                this.child = new Node[8];
                this.id = 0;
                this.level = 0;

                this.number_pixels = Integer.MAX_VALUE;

                this.mid_red   = (maxColorNum + 1) >> 1;
                this.mid_green = (maxColorNum + 1) >> 1;
                this.mid_blue  = (maxColorNum + 1) >> 1;
            }

            Node(Node parent, int id, int level) {
                this.cube = parent.cube;
                this.parent = parent;
                this.child = new Node[8];
                this.id = id;
                this.level = level;

                // add to the cube
                ++cube.nodes;
                if (level == cube.depth) {
                    ++cube.colors;
                }

                // add to the parent
                ++parent.nchild;
                parent.child[id] = this;

                // figure out our midpoint
                int bi = (1 << (maxTreeDepth - level)) >> 1;
                mid_red   = parent.mid_red   + ((id & 1) > 0 ? bi : -bi);
                mid_green = parent.mid_green + ((id & 2) > 0 ? bi : -bi);
                mid_blue  = parent.mid_blue  + ((id & 4) > 0 ? bi : -bi);
            }

            /**
             * Remove this child node, and make sure our parent
             * absorbs our pixel statistics.
             */
            void pruneChild() {
                --parent.nchild;
                parent.unique += unique;
                parent.total_red     += total_red;
                parent.total_green   += total_green;
                parent.total_blue    += total_blue;
                parent.child[id] = null;
                --cube.nodes;
                cube = null;
                parent = null;
            }

            /**
             * Prune the lowest layer of the tree.
             */
            void pruneLevel() {
                if (nchild != 0) {
                    for (int id = 0; id < 8; id++) {
                        if (child[id] != null) {
                            child[id].pruneLevel();
                        }
                    }
                }
                if (level == cube.depth) {
                    pruneChild();
                }
            }

            /**
             * Remove any nodes that have fewer than threshold
             * pixels. Also, as long as we're walking the tree:
             *
             *  - figure out the color with the fewest pixels
             *  - recalculate the total number of colors in the tree
             */
            int reduce(int threshold, int next_threshold) {
                if (nchild != 0) {
                    for (int id = 0; id < 8; id++) {
                        if (child[id] != null) {
                            next_threshold = child[id].reduce(threshold, next_threshold);
                        }
                    }
                }

                if (number_pixels <= threshold) {
                    pruneChild();
                } else {
                    if (unique != 0) {
                        cube.colors++;
                    }

                    if (number_pixels < next_threshold) {
                        next_threshold = number_pixels;
                    }
                }

                return next_threshold;
            }

            /*
             * colormap traverses the color cube tree and notes each
             * colormap entry. A colormap entry is any node in the
             * color cube tree where the number of unique colors is
             * not zero.
             */
            void colormap() {
                if (nchild != 0) {
                    for (int id = 0; id < 8; id++) {
                        if (child[id] != null) {
                            child[id].colormap();
                        }
                    }
                }
                if (unique != 0) {
                    cube.colormap[0][cube.colors] =
                        (byte)((total_red   + (unique >> 1)) / unique);
                    cube.colormap[1][cube.colors] =
                        (byte)((total_green + (unique >> 1)) / unique);
                    cube.colormap[2][cube.colors] =
                        (byte)((total_blue  + (unique >> 1)) / unique);
                    color_number = cube.colors++;
                }
            }
        }
    }
}
