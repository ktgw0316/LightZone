package com.lightcrafts.jai.operator;

import org.eclipse.imagen.*;
import org.eclipse.imagen.media.util.AreaOpPropertyGenerator;
import org.eclipse.imagen.registry.RenderedRegistryMode;

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
     * {@link ImageN#create(String, ParameterBlockImageN, RenderingHints)}.
     *
     * @see ImageN
     * @see ParameterBlockImageN
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
        final var pb = new ParameterBlockImageN("FastBoxFilter", RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("width", width);
        pb.setParameter("height", height);
        pb.setParameter("xKey", xKey);
        pb.setParameter("yKey", yKey);

        return ImageN.create("FastBoxFilter", pb, hints);
    }
}
