/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.operator;

import com.lightcrafts.media.jai.util.AreaOpPropertyGenerator;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;
import java.awt.image.RenderedImage;
import java.awt.*;

public class BilateralFilterDescriptor extends OperationDescriptorImpl {
    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for a Convolve operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "BilateralFilter"},
        {"LocalName",   "BilateralFilter"},
        {"Vendor",      "com.lightcrafts.jai"},
        {"Description", "Bilateral Filter"},
        {"Version",     "1.0"},
        {"arg0Desc",    "an image..."}
    };

    private static final String[] paramNames = {
        "sigma_d", "sigma_r", "luminosity"
    };

    private static final Class[] paramClasses = {
        Float.class, Float.class, Boolean.class
    };

    private static final Object[] paramDefaults = {
        2f, 4f, Boolean.FALSE
    };

    public BilateralFilterDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }


    public static RenderedOp create(RenderedImage source,
                                    float sigma_d, float sigma_r, boolean luminosity,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("BilateralFilter", RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source);

        pb.setParameter("sigma_d", sigma_d);
        pb.setParameter("sigma_r", sigma_r);
        pb.setParameter("luminosity", luminosity);

        return JAI.create("BilateralFilter", pb, hints);
    }
}
