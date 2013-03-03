/*
 * $RCSfile: TransposeType.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:46 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import com.lightcrafts.mediax.jai.EnumeratedParameter;

/**
 * Class used to represent the acceptable values of the "type"
 * parameter of the "Transpose" operation.  Acceptable values for the
 * "type" parameter are defined in the <code>TransposeDescriptor</code>
 * by the constants <code>FLIP_VERTICAL</code>, <code>FLIP_HORIZONTAL</code>,
 * <code>FLIP_DIAGONAL</code>, <code>FLIP_ANTIDIAGONAL</code>,
 * <code>ROTATE_90</code>, <code>ROTATE_180</code>, and
 * <code>ROTATE_270</code>.
 *
 * @since JAI 1.1
 */
public final class TransposeType extends EnumeratedParameter {
    TransposeType(String name, int value) {
        super(name, value);
    }
}
