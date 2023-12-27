/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.lightcrafts.jai.operator;

import com.sun.media.jai.util.AreaOpPropertyGenerator;

import javax.media.jai.*;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.*;
import java.awt.image.RenderedImage;

public class FastBoxFilterDescriptor extends OperationDescriptorImpl {
    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
            {"GlobalName",  "FastBoxFilter"},
            {"LocalName",   "FastBoxFilter"},
            {"Vendor",      "com.lightcrafts"},
            {"Description", "Fast Box Filter"},
            {"DocURL",      "none"},
            {"Version",     "1.0"},
            {"arg0Desc",    "The width of the box"},
            {"arg1Desc",    "The height of the box"},
            {"arg2Desc",    "The X position of the key element"},
            {"arg3Desc",    "The Y position of the key element"}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
            java.lang.Integer.class, java.lang.Integer.class,
            java.lang.Integer.class, java.lang.Integer.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
            "width", "height", "xKey", "yKey"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
            3, null, null, null
    };

    /** Constructor. */
    public FastBoxFilterDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.
     */
    @Override
    public Number getParamMinValue(int index) {
        if (index == 0 || index == 1) {
            return 1;
        } else if (index == 2 || index == 3) {
            return Integer.MIN_VALUE;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "FastBoxFilter" operation.
     *
     * @return  An array of property generators.
     */
    @Override
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }


    /**
     * Performs special case convolution where each source pixel contributes equally to the intensity of the destination pixel.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param source0 <code>RenderedImage</code> source 0.
     * @param width The width of the box.
     * May be <code>null</code>.
     * @param height The height of the box.
     * May be <code>null</code>.
     * @param xKey The X position of the key element.
     * May be <code>null</code>.
     * @param yKey The Y position of the key element.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Integer width,
                                    Integer height,
                                    Integer xKey,
                                    Integer yKey,
                                    RenderingHints hints)  {
        final var pb = new ParameterBlockJAI("FastBoxFilter", RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("width", width);
        pb.setParameter("height", height);
        pb.setParameter("xKey", xKey);
        pb.setParameter("yKey", yKey);

        return JAI.create("FastBoxFilter", pb, hints);
    }
}
