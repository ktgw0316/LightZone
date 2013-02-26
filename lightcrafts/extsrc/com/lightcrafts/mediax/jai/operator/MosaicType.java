/*
 * $RCSfile: MosaicType.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:40 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.operator;

import com.lightcrafts.mediax.jai.EnumeratedParameter;

/**
 * Class used to represent the acceptable values of the "mosaicType"
 * parameter of the "Mosaic" operation.  Acceptable values for the
 * "maskShape" parameter are defined in the {@link MosaicDescriptor}
 * by the constants {@link MosaicDescriptor#MOSAIC_TYPE_BLEND} and
 * {@link MosaicDescriptor#MOSAIC_TYPE_OVERLAY}.
 *
 * @since JAI 1.1.2
 */
public final class MosaicType extends EnumeratedParameter {
    MosaicType(String name, int value) {
        super(name, value);
    }
}

