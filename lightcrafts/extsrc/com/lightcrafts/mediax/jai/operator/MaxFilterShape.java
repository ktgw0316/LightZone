/*
 * $RCSfile: MaxFilterShape.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:39 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import com.lightcrafts.mediax.jai.EnumeratedParameter;

/**
 * Class used to represent the acceptable values of the "maskShape"
 * parameter of the "MaxFilter" operation.  Acceptable values for the
 * "maskShape" parameter are defined in the <code>MaxFilterDescriptor</code>
 * by the constants <code>MAX_MASK_SQUARE</code>, <code>MAX_MASK_PLUS</code>,
 * <code>MAX_MASK_X</code>, and
 * <code>MAX_MASK_SQUARE_SEPARABLE</code>.
 *
 * @since JAI 1.1
 */
public final class MaxFilterShape extends EnumeratedParameter {
    MaxFilterShape(String name, int value) {
        super(name, value);
    }
}
