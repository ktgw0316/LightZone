/*
 * $RCSfile: ColorModelFactory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:06 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Interface defining a callback which may be used to create a
 * <code>ColorModel</code> for the rendering of a node in an
 * operation chain.  The value corresponding to the key
 * {@link JAI#KEY_COLOR_MODEL_FACTORY} in a configuration
 * mapping must be of type <code>ColorModelFactory</code>.  This
 * configuration variable is recognized by the constructor
 * {@link OpImage#OpImage(Vector,ImageLayout,Map,boolean)}.
 *
 * @since JAI 1.1.2
 */
public interface ColorModelFactory {
    /**
     * Create a <code>ColorModel</code> given the image
     * <code>SampleModel</code> and configuration variables.
     * When invoked in the context of
     * {@link OpImage#OpImage(Vector,ImageLayout,Map,boolean)},
     * the <code>SampleModel</code> will be that of the
     * <code>OpImage</code> and the source list and configuration
     * mapping will be those which were supplied to the
     * <code>OpImage</code> constructor.
     *
     * <p>The implementing class should in general ensure that the
     * <code>ColorModel</code> created is compatible with the
     * supplied <code>SampleModel</code>.  If it is known a priori
     * that compatibility is verified by the object which invokes this
     * method, then such compatibility verification might be
     * safely omitted.</p>
     *
     * @param sampleModel The <code>SampleModel</code> to which the
     *        <code>ColorModel</code> to be created must correspond;
     *        may <b>not</b> be <code>null</code>.
     * @param sources A <code>List</code> of <code>RenderedImage</code>s;
     *        may be <code>null</code>.
     * @param configuration A configuration mapping; may be
     *        <code>null</code>.
     * @return A new <code>ColorModel</code> or <code>null</code> if it
     *         is not possible for the <code>ColorModelFactory</code>
     *         to create a <code>ColorModel</code> for the supplied
     *         parameters.
     * @exception IllegalArgumentException if <code>sampleModel</code>
     * is <code>null</code>.
     */
    ColorModel createColorModel(SampleModel sampleModel,
                                List sources,
                                Map configuration);
}
