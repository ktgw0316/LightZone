/*
 * $RCSfile: CompositeDestAlpha.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:32 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import com.lightcrafts.mediax.jai.EnumeratedParameter;

/**
 * Class used to represent the acceptable values of the "destAlpha"
 * parameter of the "Composite" operation.  Acceptable values for the
 * "destAlpha" parameter are defined in the <code>CompositeDescriptor</code>
 * by the constants <code>NO_DESTINATION_ALPHA</code>,
 * <code>DESTINATION_ALPHA_FIRST</code>, and
 * <code>DESTINATION_ALPHA_LAST</code>.
 *
 * @since JAI 1.1
 */
public final class CompositeDestAlpha extends EnumeratedParameter {
    CompositeDestAlpha(String name, int value) {
        super(name, value);
    }
}
