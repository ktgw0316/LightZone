/*
 * $RCSfile: MathJAI.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:01 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.util;

/**
 * A utility class to contain miscellaneous static methods.
 */
public class MathJAI {
    /**
     * Calculate the smallest positive power of 2 greater than or equal to
     * the provided parameter.
     *
     * @param n The value for which the next power of 2 is to be found.
     * @return The smallest power of 2 >= <i>n</i>.
     */
    public static final int nextPositivePowerOf2(int n) {
        if(n < 2) {
            return 2;
        }

        int power = 1;
        while(power < n) {
            power <<= 1;
        }

        return power;
    }

    /**
     * Determine whether the parameter is equal to a positive power of 2.
     *
     * @param n The value to check.
     * @return Whether <code>n</code> is a positive power of 2.
     */
    public static final boolean isPositivePowerOf2(int n) {
        return n == nextPositivePowerOf2(n);
    }
}
