/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: ErodeDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:35 $
 * $State: Exp $
 */
package com.lightcrafts.jai.operator;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

public class RawAdjustmentsDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for a Erode operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "RawAdjustments"},
        {"LocalName",   "RawAdjustments"},
        {"Vendor",      "com.lightcrafts.jai"},
        {"Description", "Raw Adjustments"},
        {"DocURL",      "none"},
        {"Version",     "1.0"},
        {"arg0Desc",    "The Raw Image to Adjust"}
    };

    /** The parameter names for the Erode operation. */
    private static final String[] paramNames = {
        "exposure", "colorTemperature", "cameraRGB"
    };

    /** The parameter class types for the Erode operation. */
    private static final Class[] paramClasses = {
        Float.class, Float.class, float[][].class
    };

    /** The parameter default values for the Erode operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT
    };

    /** Constructor. */
    public RawAdjustmentsDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Performs RAW adjustment operation on the image.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,java.awt.image.renderable.ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param source0 <code>RenderedImage</code> source 0.
     * @param exposure The exposure.
     * @param colorTemperature The color temperature.
     * @param cameraRGB The camera RGB matrix.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>kernel</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    float exposure,
                                    float colorTemperature,
                                    float[][] cameraRGB,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("RawAdjustments", RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setParameter("exposure", exposure);
        pb.setParameter("colorTemperature", colorTemperature);
        pb.setParameter("cameraRGB", cameraRGB);

        return JAI.create("LCErode", pb, hints);
    }
}
