/*
 * $RCSfile: ErrorDiffusionDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:35 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "ErrorDiffusion"
 * operation.
 * 
 * <p> The "ErrorDiffusion" operation performs color quantization by
 * finding the nearest color to each pixel in a supplied color map
 * and "diffusing" the color quantization error below and to the right
 * of the pixel.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>ErrorDiffusion</td></tr>
 * <tr><td>LocalName</td>   <td>ErrorDiffusion</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Performs error diffusion color quantization
 *                              using a specified color map and
 *                              error filter.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ErrorDiffusionDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The color map.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The error filter kernel.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>colorMap</td>          <td>com.lightcrafts.mediax.jai.LookupTableJAI</td>
 *                            <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>errorKernel</td>   <td>com.lightcrafts.mediax.jai.KernelJAI</td>
 *                            <td>com.lightcrafts.mediax.jai.KernelJAI.ERROR_FILTER_FLOYD_STEINBERG</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.LookupTableJAI
 * @see com.lightcrafts.mediax.jai.KernelJAI
 * @see com.lightcrafts.mediax.jai.ColorCube
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class ErrorDiffusionDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "ErrorDiffusion" operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "ErrorDiffusion"},
        {"LocalName",   "ErrorDiffusion"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("ErrorDiffusionDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ErrorDiffusionDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("ErrorDiffusionDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("ErrorDiffusionDescriptor2")}
    };

    /** The parameter names for the "ErrorDiffusion" operation. */
    private static final String[] paramNames = {
        "colorMap", "errorKernel"
    };

    /** The parameter class types for the "ErrorDiffusion" operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.mediax.jai.LookupTableJAI.class,
        com.lightcrafts.mediax.jai.KernelJAI.class
    };

    /** The parameter default values for the "ErrorDiffusion" operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT,
        // Default error filter to Floyd-Steinberg.
        KernelJAI.ERROR_FILTER_FLOYD_STEINBERG
    };

    /** Constructor. */
    public ErrorDiffusionDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }


    /**
     * Performs error diffusion color quantization using a specified color map and error filter.
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
     * @param colorMap The color map.
     * @param errorKernel The error filter kernel.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>colorMap</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    LookupTableJAI colorMap,
                                    KernelJAI errorKernel,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("ErrorDiffusion",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("colorMap", colorMap);
        pb.setParameter("errorKernel", errorKernel);

        return JAI.create("ErrorDiffusion", pb, hints);
    }
}
