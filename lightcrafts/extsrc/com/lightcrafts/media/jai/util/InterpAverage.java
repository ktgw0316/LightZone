/*
 * $RCSfile: InterpAverage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/09 01:42:36 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.util;

import com.lightcrafts.mediax.jai.Interpolation;

/**
 * An <code>Interpolation</code> class which performs simple averaging of
 * all pixels within a specified neighborhood.  It is used by the
 * "SubsampleAverage" operation implementations.
 *
 * @since JAI 1.1.2
 */
public class InterpAverage extends Interpolation {
    /**
     * Creates an <code>InterpAverage</code> instance having the supplied
     * dimensions.  The left and top padding are
     * <code>(blockX&nbsp;-&nbsp;1)/2</code> and
     * <code>(blockY&nbsp;-&nbsp;1)/2</code>, respectively. The
     * <code>subsampleBitsH</code> and <code>subsampleBitsV</code> instance
     * variables are set to 32.
     *
     * @param blockX The width of the interpolation block.
     * @param blockY The height of the interpolation block.
     *
     * @throws IllegalArgumentException if either parameter is non-positive.
     */
    public InterpAverage(int blockX, int blockY) {
        super(blockX, blockY,
              (blockX - 1)/2, blockX - 1 - (blockX - 1)/2,
              (blockY - 1)/2, blockY - 1 - (blockY - 1)/2,
              32, 32);

        if(blockX <= 0 || blockY <= 0) {
            throw new IllegalArgumentException("blockX <= 0 || blockY <= 0");
        }
    }

    /**
     * Returns the average of all elements in <code>samples</code>;
     * <code>xfrac</code> is ignored.
     */
    public int interpolateH(int[] samples, int xfrac) {
        int numSamples = samples.length;
        double total = 0.0;
        for(int i = 0; i < numSamples; i++) {
            total += samples[i]/numSamples;
        }
        return (int)(total + 0.5);
    }

    /**
     * Returns the average of all elements in <code>samples</code>;
     * <code>xfrac</code> is ignored.
     */
    public float interpolateH(float[] samples, float xfrac) {
        int numSamples = samples.length;
        float total = 0.0F;
        for(int i = 0; i < numSamples; i++) {
            total += samples[i]/numSamples;
        }
        return total;
    }

    /**
     * Returns the average of all elements in <code>samples</code>;
     * <code>xfrac</code> is ignored.
     */
    public double interpolateH(double[] samples, float xfrac) {
        int numSamples = samples.length;
        double total = 0.0;
        for(int i = 0; i < numSamples; i++) {
            total += samples[i]/numSamples;
        }
        return total;
    }
}
