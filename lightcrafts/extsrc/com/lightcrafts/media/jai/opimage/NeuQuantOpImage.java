/*
 * $RCSfile: NeuQuantOpImage.java,v $
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
import java.awt.image.RenderedImage;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.iterator.RandomIter;
import com.lightcrafts.mediax.jai.iterator.RandomIterFactory;
import com.lightcrafts.mediax.jai.ROI;

/**
 * An <code>OpImage</code> implementing the "ColorQuantizer" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.ExtremaDescriptor</code>
 * based on the median-cut algorithm.
 *
 * This is based on a java-version of Anthony Dekker's implementation of
 * NeuQuant Neural-Net Quantization Algorithm
 *
 * NEUQUANT Neural-Net quantization algorithm by Anthony Dekker, 1994.
 * See "Kohonen neural networks for optimal colour quantization"
 * in "Network: Computation in Neural Systems" Vol. 5 (1994) pp 351-367.
 * for a discussion of the algorithm.
 *
 * Any party obtaining a copy of these files from the author, directly or
 * indirectly, is granted, free of charge, a full and unrestricted irrevocable,
 * world-wide, paid up, royalty-free, nonexclusive right and license to deal
 * in this software and documentation files (the "Software"), including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons who receive
 * copies from any such party to do so, with the only requirement being
 * that this copyright notice remain intact.
 *
 * @see com.lightcrafts.mediax.jai.operator.ExtremaDescriptor
 * @see ExtremaCRIF
 */
public class NeuQuantOpImage extends ColorQuantizerOpImage {
    /** four primes near 500 - assume no image has a length so large
     * that it is divisible by all four primes
     */
    protected static final int prime1 = 499;
    protected static final int prime2 = 491;
    protected static final int prime3 = 487;
    protected static final int prime4 = 503;

    /* minimum size for input image */
    protected static final int minpicturebytes = (3 * prime4);

    /** The size of the histogram. */
    private int ncycles;

    /* Program Skeleton
      ----------------
      [select samplefac in range 1..30]
      [read image from input file]
      pic = (unsigned char*) malloc(3*width*height);
      initnet(pic,3*width*height,samplefac);
      learn();
      unbiasnet();
      [write output image header, using writecolourmap(f)]
      inxbuild();
      write output image using inxsearch(b,g,r)      */

    /* Network Definitions
      ------------------- */

    private final int maxnetpos = maxColorNum - 1;
    private final int netbiasshift = 4; /* bias for colour values */

    /* defs for freq and bias */
    private final int intbiasshift = 16; /* bias for fractions */
    private final int intbias = 1 << intbiasshift;
    private final int gammashift = 10;   /* gamma = 1024 */
    private final int gamma = 1 << gammashift;
    private final int betashift = 10;
    private final int beta = intbias >> betashift; /* beta = 1/1024 */
    private final int betagamma = intbias << (gammashift - betashift);

    /* defs for decreasing radius factor */
    private final int initrad = maxColorNum >> 3;
    private final int radiusbiasshift = 6;      /* at 32.0 biased by 6 bits */
    private final int radiusbias = 1 << radiusbiasshift;
    private final int initradius = initrad * radiusbias; /* and decreases by a */
    private final int radiusdec = 30;           /* factor of 1/30 each cycle */

    /* defs for decreasing alpha factor */
    private final int alphabiasshift = 10;      /* alpha starts at 1.0 */
    private final int initalpha = 1 << alphabiasshift;

    private int alphadec; /* biased by 10 bits */

    /* radbias and alpharadbias used for radpower calculation */
    private final int radbiasshift = 8;
    private final int radbias = 1 << radbiasshift;
    private final int alpharadbshift = alphabiasshift + radbiasshift;
    private final int alpharadbias = 1 << alpharadbshift;

   //   typedef int pixel[4];                /* BGRc */
    private int[][] network; /* the network itself - [maxColorNum][4] */

    private int[] netindex = new int[256];  /* for network lookup - really 256 */

    private int[] bias = new int[maxColorNum];  /* bias and freq arrays for learning */
    private int[] freq = new int[maxColorNum];
    private int[] radpower = new int[initrad]; /* radpower for precomputation */

    /**
     * Constructs an <code>NeuQuantOpImage</code>.
     *
     * @param source  The source image.
     */
    public NeuQuantOpImage(RenderedImage source,
                            Map config,
                            ImageLayout layout,
                            int maxColorNum,
                            int upperBound,
                            ROI roi,
                            int xPeriod,
                            int yPeriod) {
        super(source, config, layout, maxColorNum, roi, xPeriod, yPeriod);

        colorMap = null;
        this.ncycles = upperBound;
    }

    protected synchronized void train() {

        // intialize the network
        network = new int[maxColorNum][];
        for (int i = 0; i < maxColorNum; i++) {
           network[i] = new int[4];
           int[] p = network[i];
           p[0] = p[1] = p[2] = (i << (netbiasshift + 8)) / maxColorNum;
           freq[i] = intbias / maxColorNum; /* 1/maxColorNum */
           bias[i] = 0;
        }

        PlanarImage source = getSourceImage(0);
        Rectangle rect = source.getBounds();

        if (roi != null)
            rect = roi.getBounds();

        RandomIter iterator = RandomIterFactory.create(source, rect);

        int samplefac = xPeriod * yPeriod;
        int startX = rect.x / xPeriod;
        int startY = rect.y / yPeriod;
        int offsetX = rect.x % xPeriod;
        int offsetY = rect.y % yPeriod;
        int pixelsPerLine = (rect.width - 1) / xPeriod + 1;
        int numSamples =
            pixelsPerLine * ((rect.height - 1) / yPeriod + 1);

        if (numSamples < minpicturebytes)
            samplefac = 1;

        alphadec = 30 + ((samplefac - 1) / 3);
        int pix = 0;

        int delta = numSamples / ncycles;
        int alpha = initalpha;
        int radius = initradius;

        int rad = radius >> radiusbiasshift;
        if (rad <= 1)
            rad = 0;
        for (int i = 0; i < rad; i++)
            radpower[i] = alpha * (((rad * rad - i * i) * radbias) / (rad * rad));

        int step;
        if (numSamples < minpicturebytes)
            step = 3;
        else if ((numSamples % prime1) != 0)
            step = 3 * prime1;
        else {
            if ((numSamples % prime2) != 0)
                step = 3 * prime2;
            else {
                if ((numSamples % prime3) != 0)
                    step = 3 * prime3;
                else
                    step = 3 * prime4;
            }
        }

        int[] pixel = new int[3];

        for (int i = 0; i < numSamples;) {
            int y = (pix / pixelsPerLine + startY) * yPeriod + offsetY;
            int x = (pix % pixelsPerLine + startX) * xPeriod + offsetX;

            try {
            iterator.getPixel(x, y, pixel);
            } catch (Exception e) {
                continue;
            }

            int b = pixel[2] << netbiasshift;
            int g = pixel[1] << netbiasshift;
            int r = pixel[0] << netbiasshift;

            int j = contest(b , g, r);

            altersingle(alpha, j, b , g, r);
            if (rad != 0)
                alterneigh(rad, j, b , g, r); /* alter neighbours */

            pix += step;
            if (pix >= numSamples)
                pix -= numSamples;

            i++;
            if (i % delta == 0) {
                alpha -= alpha / alphadec;
                radius -= radius / radiusdec;
                rad = radius >> radiusbiasshift;
                if (rad <= 1)
                    rad = 0;
                for (j = 0; j < rad; j++)
                    radpower[j] = alpha * (((rad * rad - j * j) * radbias) / (rad * rad));
            }
        }

        unbiasnet();
        inxbuild();
        createLUT();
        setProperty("LUT", colorMap);
        setProperty("JAI.LookupTable", colorMap);
    }

    private void createLUT() {
        colorMap = new LookupTableJAI(new byte[3][maxColorNum]);
        byte[][] map = colorMap.getByteData();
        int[] index = new int[maxColorNum];
        for (int i = 0; i < maxColorNum; i++)
            index[network[i][3]] = i;
        for (int i = 0; i < maxColorNum; i++) {
            int j = index[i];
            map[2][i] = (byte) (network[j][0]);
            map[1][i] = (byte) (network[j][1]);
            map[0][i] = (byte) (network[j][2]);
        }
    }

    /** Insertion sort of network and building of netindex[0..255]
     *  (to do after unbias)
     */
    private void inxbuild() {
        int previouscol = 0;
        int startpos = 0;
        for (int i = 0; i < maxColorNum; i++) {
            int[] p = network[i];
            int smallpos = i;
            int smallval = p[1]; /* index on g */
            /* find smallest in i..maxColorNum-1 */
            int j;
            for (j = i + 1; j < maxColorNum; j++) {
                int[] q = network[j];
                if (q[1] < smallval) { /* index on g */
                    smallpos = j;
                    smallval = q[1]; /* index on g */
                }
            }
            int[] q = network[smallpos];
            /* swap p (i) and q (smallpos) entries */
            if (i != smallpos) {
                j = q[0];    q[0] = p[0];    p[0] = j;
                j = q[1];    q[1] = p[1];    p[1] = j;
                j = q[2];    q[2] = p[2];    p[2] = j;
                j = q[3];    q[3] = p[3];    p[3] = j;
            }
            /* smallval entry is now in position i */
            if (smallval != previouscol) {
                netindex[previouscol] = (startpos + i) >> 1;
                for (j = previouscol + 1; j < smallval; j++)
                    netindex[j] = i;
                previouscol = smallval;
                startpos = i;
            }
        }
        netindex[previouscol] = (startpos + maxnetpos) >> 1;
        for (int j = previouscol + 1; j < 256; j++)
            netindex[j] = maxnetpos; /* really 256 */
    }

    /** Search for BGR values 0..255 (after net is unbiased) and
     *  return colour index
     */
    protected byte findNearestEntry(int r, int g, int b) {
        int bestd = 1000; /* biggest possible dist is 256*3 */
        int best = -1;
        int i = netindex[g]; /* index on g */
        int j = i - 1; /* start at netindex[g] and work outwards */

        while (i < maxColorNum || j >= 0) {
           if (i < maxColorNum) {
              int[] p = network[i];
              int dist = p[1] - g; /* inx key */
              if (dist >= bestd)
                  i = maxColorNum; /* stop iter */
              else {
                  i++;
                  if (dist < 0)
                      dist = -dist;
                  int a = p[0] - b;
                  if (a < 0)
                      a = -a;
                  dist += a;
                  if (dist < bestd) {
                      a = p[2] - r;
                      if (a < 0)
                          a = -a;
                      dist += a;
                      if (dist < bestd) {
                          bestd = dist;
                          best = p[3];
                      }
                  }
              }
          }

          if (j >= 0) {
              int[] p = network[j];
              int dist = g - p[1]; /* inx key - reverse dif */
              if (dist >= bestd)
                  j = -1; /* stop iter */
              else {
                  j--;
                  if (dist < 0)
                      dist = -dist;
                  int a = p[0] - b;
                  if (a < 0)
                      a = -a;
                  dist += a;
                  if (dist < bestd) {
                      a = p[2] - r;
                      if (a < 0)
                      a = -a;
                      dist += a;
                      if (dist < bestd) {
                          bestd = dist;
                          best = p[3];
                      }
                  }
              }
          }
      }
      return (byte)best;
   }

    /** Unbias network to give byte values 0..255 and record
     *  position i to prepare for sort.
     */
    private void unbiasnet() {
        for (int i = 0; i < maxColorNum; i++) {
            network[i][0] >>= netbiasshift;
            network[i][1] >>= netbiasshift;
            network[i][2] >>= netbiasshift;
            network[i][3] = i; /* record colour no */
        }
    }

   /** Move adjacent neurons by precomputed
    *  alpha*(1-((i-j)^2/[r]^2)) in radpower[|i-j|]
    */

   private void alterneigh(int rad, int i, int b, int g, int r) {
      int lo = i - rad;
      if (lo < -1)
         lo = -1;
      int hi = i + rad;
      if (hi > maxColorNum)
         hi = maxColorNum;

      int j = i + 1;
      int k = i - 1;
      int m = 1;
      while ((j < hi) || (k > lo)) {
         int a = radpower[m++];
         if (j < hi) {
            int[] p = network[j++];
//            try {
               p[0] -= (a * (p[0] - b)) / alpharadbias;
               p[1] -= (a * (p[1] - g)) / alpharadbias;
               p[2] -= (a * (p[2] - r)) / alpharadbias;
//            } catch (Exception e) {} // prevents 1.3 miscompilation
         }
         if (k > lo) {
            int[] p = network[k--];
//            try {
               p[0] -= (a * (p[0] - b)) / alpharadbias;
               p[1] -= (a * (p[1] - g)) / alpharadbias;
               p[2] -= (a * (p[2] - r)) / alpharadbias;
//            } catch (Exception e) {}
         }
      }
   }


   /** Move neuron i towards biased (b,g,r) by factor alpha. */
   private void altersingle(int alpha, int i, int b, int g, int r) {
      /* alter hit neuron */
      int[] n = network[i];
      n[0] -= (alpha * (n[0] - b)) / initalpha;
      n[1] -= (alpha * (n[1] - g)) / initalpha;
      n[2] -= (alpha * (n[2] - r)) / initalpha;
   }


   /** Search for biased BGR values. */
   private int contest(int b, int g, int r) {
      /* finds closest neuron (min dist) and updates freq */
      /* finds best neuron (min dist-bias) and returns position */
      /* for frequently chosen neurons, freq[i] is high and bias[i] is negative */
      /* bias[i] = gamma*((1/maxColorNum)-freq[i]) */
      int bestd = ~(((int) 1) << 31);
      int bestbiasd = bestd;
      int bestpos = -1;
      int bestbiaspos = bestpos;

      for (int i = 0; i < maxColorNum; i++) {
         int[] n = network[i];
         int dist = n[0] - b;
         if (dist < 0)
            dist = -dist;
         int a = n[1] - g;
         if (a < 0)
            a = -a;
         dist += a;
         a = n[2] - r;
         if (a < 0)
            a = -a;
         dist += a;
         if (dist < bestd) {
            bestd = dist;
            bestpos = i;
         }
         int biasdist = dist - ((bias[i]) >> (intbiasshift - netbiasshift));
         if (biasdist < bestbiasd) {
            bestbiasd = biasdist;
            bestbiaspos = i;
         }
         int betafreq = (freq[i] >> betashift);
         freq[i] -= betafreq;
         bias[i] += (betafreq << gammashift);
      }
      freq[bestpos] += beta;
      bias[bestpos] -= betagamma;
      return (bestbiaspos);
   }
}
