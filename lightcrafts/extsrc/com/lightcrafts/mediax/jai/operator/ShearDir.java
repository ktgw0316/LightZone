/*
 * $RCSfile: ShearDir.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:44 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import com.lightcrafts.mediax.jai.EnumeratedParameter;

/**
 * Class used to represent the acceptable values of the "shearDir"
 * parameter of the "Shear" operation.  Acceptable values for the
 * "shearDir" parameter are defined in the <code>ShearDescriptor</code>
 * by the constants <code>SHEAR_HORIZONTAL</code> and
 * <code>SHEAR_VERTICAL</code>.
 *
 * @since JAI 1.1
 */
public final class ShearDir extends EnumeratedParameter {
    ShearDir(String name, int value) {
        super(name, value);
    }
}
