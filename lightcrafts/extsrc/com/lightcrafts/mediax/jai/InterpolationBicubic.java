/*
 * $RCSfile: InterpolationBicubic.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:10 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

/**
 * A class representing bicubic interpolation.
 *
 * <p> InterpolationBicubic is a subclass of Interpolation that
 * performs interpolation using the piecewise cubic polynomial:
 *
 * <pre>
 * r(x) = (a + 2)|x|^3 - (a + 3)|x|^2         +  1 , 0 <= |x| < 1
 * r(x) =       a|x|^3 -      5a|x|^2 + 8a|x| - 4a , 1 <= |x| < 2
 * r(x) = 0                                        , otherwise
 * </pre>
 *
 * with 'a' set to -0.5.
 *
 * This definition is also sometimes known as "cubic convolution",
 * using the parameter 'a' recommended by Rifman.
 * (Reference: Digital Image Warping, George Wolberg, 1990, pp 129-131,
 *             IEEE Computer Society Press, ISBN 0-8186-8944-7)
 * 
 * <p> A neighborhood extending one sample to the left of and above the
 * central sample, and two samples to the right of and below the central
 * sample is required to perform bicubic interpolation.
 * 
 * <p> This implementation creates an <code>InterpolationTable</code>
 * whose integer coefficients have eight bits of precision to the 
 * right of the binary point.
 *
 * <p> The diagrams below illustrate the pixels involved in one-dimensional
 * interpolation. Point s0 is the interpolation kernel key position.
 * xfrac and yfrac, indicated by the dots, represent the point of interpolation
 * between two pixels. This value lies between 0.0 and 1.0 exclusive for
 * floating point and 0 and 2<sup>subsampleBits</sup> exclusive for integer
 * interpolations.
 *
 * <pre>
 * <b>
 *         Horizontal              Vertical
 *
 *    s_    s0 .  s1    s2            s_                             
 *             ^                                                     
 *            xfrac                   s0                        
 *                                     .< yfrac                  
 *                                    s1                         
 *                                                                      
 *                                    s2                         
 * </b>
 * </pre>
 *
 * <p> The diagram below illustrates the pixels involved in
 * two-dimensional interpolation.
 *                                                                             
 * <pre>
 * <b>
 *               s__    s_0    s_1    s_2                              
 *                                                                      
 *                                                                      
 *                                                                      
 *               s0_    s00    s01    s02                              
 *                                                                      
 *                          .             < yfrac                      
 *                                                                      
 *               s1_    s10    s11    s12                              
 *                                                                      
 *                                                                      
 *                                                                      
 *               s2_    s20    s21    s22                              
 *                          ^                                           
 *                         xfrac                                        
 * </b>
 * </pre>
 *
 * <p> The class is marked 'final' so that it may be more easily inlined.
 *
 * @see Interpolation
 */
public final class InterpolationBicubic extends InterpolationTable {

    private static final int PRECISION_BITS = 12;

    private static float[] dataHelper(int subsampleBits) {

	int one = 1 << subsampleBits;
	int arrayLength = one * 4;
	float tableValues[] = new float[arrayLength];
	float f;

	float onef = (float)one;
	float t;
	int count = 0;
	for (int i=0; i<one; i++) {
	    t = (float)i;
	    f = (i/onef);
	      
	    tableValues[count++] = bicubic(f + 1.0F);
	    tableValues[count++] = bicubic(f);
	    tableValues[count++] = bicubic(f-1.0F);
	    tableValues[count++] = bicubic(f-2.0F);
	    
	}

	return tableValues;
    }


    // The parameter "a" for the bicubic polynomial
    private static final float A  = -0.5F;

    // Define all of the polynomial coefficients in terms of "a"
    private static final float A3 = A + 2.0F;
    private static final float A2 = - (A + 3.0F);
    private static final float A0 = 1.0F;

    private static final float B3 = A;
    private static final float B2 = - (5.0F * A);
    private static final float B1 = 8.0F * A;
    private static final float B0 = - (4.0F * A);


    /** Returns the bicubic polynomial value at a certain value of x. */
    private static float bicubic(float x) {

	if (x<0) {
	    x = -x;
	}
	
        // Evaluate with Horner's rule
	if (x>=1) {
            return (((B3 * x) + B2) * x + B1) * x + B0;
	} else {
            return ((A3 * x) + A2) * x * x + A0;
	}

    }
    
    /**
     * Constructs an InterpolationBicubic with a given subsample
     * precision, in bits. This precision is applied to both axes.
     *
     * <p> This implementation creates an <code>InterpolationTable</code>
     * whose integer coefficients have eight bits of precision to the 
     * right of the binary point.
     *
     * @param subsampleBits the subsample precision.
     */
    public InterpolationBicubic(int subsampleBits) {
        super(1, 1, 4, 4, subsampleBits, subsampleBits,
              PRECISION_BITS,
              dataHelper(subsampleBits), null);
    }

}
