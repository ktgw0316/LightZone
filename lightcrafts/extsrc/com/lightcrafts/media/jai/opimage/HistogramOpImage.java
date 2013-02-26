/*
 * $RCSfile: HistogramOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:27 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.Histogram;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.StatisticsOpImage;

/**
 * An <code>OpImage</code> implementing the "Histogram" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.HistogramDescriptor</code>.
 *
 * @see com.lightcrafts.mediax.jai.Histogram
 * @see com.lightcrafts.mediax.jai.operator.HistogramDescriptor
 * @see HistogramCRIF
 */
final class HistogramOpImage extends StatisticsOpImage {

    /** Number of bins per band. */
    private int[] numBins;

    /** The low value checked inclusive for each band. */
    private double[] lowValue;

    /** The high value checked exclusive for each band. */
    private double[] highValue;

    /** The number of bands of the source image. */
    private int numBands;

    private final boolean tileIntersectsROI(int tileX, int tileY) {
        if (roi == null) {      // ROI is entire tile
            return true;
        } else {
            return roi.intersects(tileXToX(tileX), tileYToY(tileY),
                                  tileWidth, tileHeight);
        }
    }

    /**
     * Constructs an <code>HistogramOpImage</code>.
     *
     * @param source  The source image.
     */
    public HistogramOpImage(RenderedImage source,
                            ROI roi,
                            int xStart,
                            int yStart,
                            int xPeriod,
                            int yPeriod,
                            int[] numBins,
                            double[] lowValue,
                            double[] highValue) {
        super(source, roi, xStart, yStart, xPeriod, yPeriod);

        numBands = source.getSampleModel().getNumBands();

        this.numBins = new int[numBands];
        this.lowValue = new double[numBands];
        this.highValue = new double[numBands];

        for (int b = 0; b < numBands; b++) {
            this.numBins[b] = numBins.length == 1 ?
                              numBins[0] : numBins[b];
            this.lowValue[b] = lowValue.length == 1 ?
                               lowValue[0] : lowValue[b];
            this.highValue[b] = highValue.length == 1 ?
                                highValue[0] : highValue[b];
        }
    }

    protected String[] getStatisticsNames() {
        String[] names = new String[1];
        names[0] = "histogram";
        return names;
    }

    protected Object createStatistics(String name) {
        if (name.equalsIgnoreCase("histogram")) {
            return new Histogram(numBins, lowValue, highValue);
        } else {
            return java.awt.Image.UndefinedProperty;
        }
    }

    protected void accumulateStatistics(String name,
                                        Raster source,
                                        Object stats) {
        Histogram histogram = (Histogram)stats;
        histogram.countPixels(source, roi, xStart, yStart, xPeriod, yPeriod);
    }
}
