/*
 * $RCSfile: FilterCRIF.java,v $
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
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.Arrays;
import com.lightcrafts.mediax.jai.CRIFImpl;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.KernelJAI;

/**
 * This CRIF implements rendering-independent filtering (blur/sharpen).
 *
 * @since 1.0
 * @see FilterDescriptor
 */
final class FilterCRIF extends CRIFImpl {
    /**
     * Step size of the filter parameter indicating a step from one kernel
     * size to the next.
     */
    private static final int STEPSIZE = 5;

    /**
     * Create a kernel given the filter parameter. Positive is blur,
     * negative sharpen.
     */
    private static final KernelJAI createKernel(double p) {
        int STEPSIZE = 5;

        if(p == 0.0) {
            return null;
        }

        double pAbs = Math.abs(p);
        int idx = ((int)pAbs) / STEPSIZE;
        double frac = (10.0F/STEPSIZE)*(pAbs - idx*STEPSIZE);
        double blend = 1.0/99.0*(Math.pow(10.0, 0.2*frac) - 1.0);

        // First create a low-pass kernel.
        int size;
        float[] data;
        if(idx*STEPSIZE == pAbs) {
            // The parameter is at the left end of an interval so no
            // blending of kernels is required.
            size = 2*idx + 1;
            data = new float[size*size];
            float val = 1.0F/(size*size);
            Arrays.fill(data, val);
        } else {
            // Create data for the left and right intervals and blend them.
            int size1 = 2*idx + 1;
            size = size1 + 2;
            data = new float[size*size];
            float val1 = (1.0F/(size1*size1))*(1.0F - (float)blend);
            int row = size;
            for(int j = 1; j < size - 1; j++) {
                for(int i = 1; i < size - 1; i++) {
                    data[row + i] = val1;
                }
                row += size;
            }
            float val2 = (1.0F/(size*size))*(float)blend;
            for(int i = 0; i < data.length; i++) {
                data[i] += val2;
            }
        }

        // For positive factor generate a high-pass kernel.
        if(p > 0.0) {
            // Subtract the low-pass kernel data from the image.
            for(int i = 0; i < data.length; i++) {
                data[i] *= -1.0;
            }
            data[data.length/2] += 2.0F;
        }

        return new KernelJAI(size, size, data);
    }

    /** Constructor. */
    public FilterCRIF() {
        super();
    }

    /**
     * Implementation of "RIF" create().
     */
    public RenderedImage create(ParameterBlock paramBlock,
                                RenderingHints renderHints) {
        KernelJAI kernel = createKernel(paramBlock.getFloatParameter(0));

        return kernel == null ? paramBlock.getRenderedSource(0):
            JAI.create("convolve", paramBlock.getRenderedSource(0), kernel);
    }

}
