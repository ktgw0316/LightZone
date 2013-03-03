/*
 * $RCSfile: ColorQuantizerType.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:31 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import com.lightcrafts.mediax.jai.EnumeratedParameter;

/**
 * <p>Class used to represent the acceptable values of the "quantizationAlgorithm"
 * parameter of the "ColorQuantizer" operation.  Acceptable values for the
 * "quantizationAlgorithm" parameter are defined in the
 * <code>ColorQuantizerDescriptor</code> by the constants
 * <code>MEDIANCUT</code>,
 * <code>NEUQUANT</code>, and
 * <code>OCTTREE</code>. </p>
 *
 * @see ColorQuantizerDescriptor
 * 
 * @since JAI 1.1.2
 */
public final class ColorQuantizerType extends EnumeratedParameter {
    ColorQuantizerType(String name, int value) {
        super(name, value);
    }
}
