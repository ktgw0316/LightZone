/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.operator;

import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;
import java.awt.image.RenderedImage;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 15, 2005
 * Time: 1:56:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class NOPDescriptor extends OperationDescriptorImpl {
    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "NOP"},
        {"LocalName",   "NOP"},
        {"Vendor",      "lightcrafts.com"},
        {"Description", "NOP"},
        {"DocURL",      "none"},
        {"Version",     "1.0"}
    };

    /** Constructor. */
    public NOPDescriptor() {
        super(resources, 1);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    public static RenderedOp create(RenderedImage source,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("NOP", RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source);
        return JAI.create("NOP", pb, hints);
    }
}
