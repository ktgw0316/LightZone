/*
 * $RCSfile: MlibHistogramOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:57 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.mlib;

import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.Histogram;
import com.lightcrafts.mediax.jai.StatisticsOpImage;
import java.util.Iterator;
import java.util.TreeMap;
import com.sun.medialib.mlib.Image;
import com.sun.medialib.mlib.mediaLibImage;

/**
 * An <code>OpImage</code> implementing the "Histogram" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.HistogramDescriptor</code>.
 *
 * @see com.lightcrafts.mediax.jai.Histogram
 * @see com.lightcrafts.mediax.jai.operator.HistogramDescriptor
 */
final class MlibHistogramOpImage extends StatisticsOpImage {

    /** Number of bins per band. */
    private int[] numBins;

    /** The low value checked inclusive for each band. */
    private double[] lowValueFP;

    /** The high value checked exclusive for each band. */
    private double[] highValueFP;

    /** The low value checked inclusive for each band. */
    private int[] lowValue;

    /** The high value checked exclusive for each band. */
    private int[] highValue;

    /** The number of bands of the source image. */
    private int numBands;

    private int[] bandIndexMap;

    private boolean reorderBands = false;

    /**
     * Constructs an <code>MlibHistogramOpImage</code>.
     *
     * @param source  The source image.
     */
    public MlibHistogramOpImage(RenderedImage source,
                                int xPeriod,
                                int yPeriod,
                                int[] numBins,
                                double[] lowValueFP,
                                double[] highValueFP) {
        super(source,
              null, // ROI
              source.getMinX(), // xStart
              source.getMinY(), // yStart
              xPeriod, yPeriod);

        // Save the band count.
        numBands = sampleModel.getNumBands();

        // Allocate memory to copy parameters.
        this.numBins = new int[numBands];
        this.lowValueFP = new double[numBands];
        this.highValueFP = new double[numBands];

        // Copy parameters.
        for (int b = 0; b < numBands; b++) {
            this.numBins[b] = numBins.length == 1 ?
                              numBins[0] : numBins[b];
            this.lowValueFP[b] = lowValueFP.length == 1 ?
                               lowValueFP[0] : lowValueFP[b];
            this.highValueFP[b] = highValueFP.length == 1 ?
                                highValueFP[0] : highValueFP[b];
        }

        // Convert low values to integers. ceil() is used because the
        // pixel values are integral and the comparison is inclusive
        // so a floor() might include unwanted values if the low
        // value is floating point.
        this.lowValue = new int[this.lowValueFP.length];
        for(int i = 0; i < this.lowValueFP.length; i++) {
            this.lowValue[i] = (int)Math.ceil(this.lowValueFP[i]);
        }

        // Convert high values to integers. ceil() is used because the
        // pixel values are integral and the comparison is exclusive
        // so a floor might cause desired values to be excluded as
        // only those through floor(high) - 1 would be included.
        this.highValue = new int[this.highValueFP.length];
        for(int i = 0; i < this.highValueFP.length; i++) {
            this.highValue[i] = (int)Math.ceil(this.highValueFP[i]);
        }

        // Set up the band re-index map if needed.
        if(numBands > 1) {
            ComponentSampleModel csm = (ComponentSampleModel)sampleModel;

            TreeMap indexMap = new TreeMap();

            // Determine whether there is more than one bank.
            int[] indices = csm.getBankIndices();
            boolean checkBanks = false;
            for(int i = 1; i < numBands; i++) {
                if(indices[i] != indices[i-1]) {
                    checkBanks = true;
                    break;
                }
            }

            // Check the banks for ordering.
            if(checkBanks) {
                for(int i = 0; i < numBands; i++) {
                    indexMap.put(new Integer(indices[i]), new Integer(i));
                }

                bandIndexMap = new int[numBands];
                Iterator bankIter = indexMap.keySet().iterator();
                int k = 0;
                while(bankIter.hasNext()) {
                    int idx =
                        ((Integer)indexMap.get(bankIter.next())).intValue();
                    if(idx != k) {
                        reorderBands = true;
                    }
                    bandIndexMap[k++] = idx;
                }
            }

            // If band re-ordering not needed on basis of bank indices
            // then check ordering of band offsets.
            if(!reorderBands) {
                indexMap.clear();

                if(bandIndexMap == null) {
                    bandIndexMap = new int[numBands];
                }

                int[] offsets = csm.getBandOffsets();
                for(int i = 0; i < numBands; i++) {
                    indexMap.put(new Integer(offsets[i]), new Integer(i));
                }

                Iterator offsetIter = indexMap.keySet().iterator();
                int k = 0;
                while(offsetIter.hasNext()) {
                    int idx =
                        ((Integer)indexMap.get(offsetIter.next())).intValue();
                    if(idx != k) {
                        reorderBands = true;
                    }
                    bandIndexMap[k++] = idx;
                }
            }
        }
    }

    protected String[] getStatisticsNames() {
        String[] names = new String[1];
        names[0] = "histogram";
        return names;
    }

    protected Object createStatistics(String name) {
        if (name.equalsIgnoreCase("histogram")) {
            return new Histogram(numBins, lowValueFP, highValueFP);
        } else {
            return java.awt.Image.UndefinedProperty;
        }
    }

    protected void accumulateStatistics(String name,
                                        Raster source,
                                        Object stats) {
        // Get the JAI histogram.
        Histogram histogram = (Histogram)stats;
        int numBands = histogram.getNumBands();
        int[][] histJAI = histogram.getBins();

        // Get the tile bounds.
        Rectangle tileRect = source.getBounds();

        // Get the tile bins.
        int[][] histo;
        if(!reorderBands && tileRect.equals(getBounds())) {
            // Entire image: use the global histogram bins directly.
            histo = histJAI;
        } else {
            // Sub-image: save results for this tile only.
            histo = new int[numBands][];
            for(int i = 0; i < numBands; i++) {
                histo[i] = new int[histogram.getNumBins(i)];
            }
        }

        // Get the mlib image.
        int formatTag = MediaLibAccessor.findCompatibleTag(null, source);
        MediaLibAccessor accessor =
            new MediaLibAccessor(source, tileRect, formatTag);
        mediaLibImage[] img  = accessor.getMediaLibImages();

        // Determine the offset within the tile.
        int offsetX = (xPeriod - ((tileRect.x - xStart) % xPeriod)) % xPeriod;
        int offsetY = (yPeriod - ((tileRect.y - yStart) % yPeriod)) % yPeriod;

        if(histo == histJAI) {
            synchronized(histogram) {
                // Compute the histogram into the global array.
                Image.Histogram2(histo, img[0], lowValue, highValue,
                                 offsetX, offsetY, xPeriod, yPeriod);
            }
        } else {
            // Compute the histogram into the local array.
            Image.Histogram2(histo, img[0], lowValue, highValue,
                             offsetX, offsetY, xPeriod, yPeriod);

            // Accumulate values if not using the global histogram.
            synchronized(histogram) {
                for(int i = 0; i < numBands; i++) {
                    int numBins = histo[i].length;
                    int[] binsBandJAI = reorderBands ?
                        histJAI[bandIndexMap[i]] : histJAI[i];
                    int[] binsBand = histo[i];
                    for(int j = 0; j < numBins; j++) {
                        binsBandJAI[j] += binsBand[j];
                    }
                }
            }
        }
    }
}
