/*
 * $RCSfile: DFTDataNature.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:33 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import com.lightcrafts.mediax.jai.EnumeratedParameter;

/**
 * Class used to represent the acceptable values of the "dataNature"
 * parameter of the "DFT" and "IDFT" operations.  Acceptable values for the
 * "dataNature" parameter are defined in the <code>DFTDescriptor</code>
 * and <code>IDFTDescriptor</code> by the constants
 * <code>REAL_TO_COMPLEX</code>, <code>COMPLEX_TO_COMPLEX</code>, and
 * <code>COMPLEX_TO_REAL</code>.
 *
 * @since JAI 1.1
 */
public final class DFTDataNature extends EnumeratedParameter {
    DFTDataNature(String name, int value) {
        super(name, value);
    }
}
