/*
 * $RCSfile: GradientMagnitudeDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:36 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import com.lightcrafts.media.jai.util.AreaOpPropertyGenerator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.PropertyGenerator;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "GradientMagnitude"
 * operation.
 *
 * <p> The "GradientMagnitude" operation is an edge detector which computes
 * the magnitude of the image gradient vector in two orthogonal directions.
 *
 * <p> The result of the "GradientMagnitude" operation may be defined as:
 * <pre>
 * dst[x][y][b] = ((SH(x,y,b))^2 + (SV(x,y,b))^2 )^0.5
 * </pre>
 *
 * where SH(x,y,b) and SV(x,y,b) are the horizontal and vertical gradient
 * images generated from band <i>b</i> of the source image by correlating it
 * with the supplied orthogonal (horizontal and vertical) gradient masks.
 *
 * Origins set on the kernels will be ignored. The origins are assumed to be
 * width/2 & height/2.
 *
 * It should be noted that this operation automatically adds a
 * value of <code>Boolean.TRUE</code> for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> to the given
 * <code>configuration</code> so that the operation is performed
 * on the pixel values instead of being performed on the indices into
 * the color map if the source(s) have an <code>IndexColorModel</code>.
 * This addition will take place only if a value for the 
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been
 * provided by the user. Note that the <code>configuration</code> Map
 * is cloned before the new hint is added to it. The operation can be 
 * smart about the value of the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code>
 * <code>RenderingHints</code>, i.e. while the default value for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> is
 * <code>Boolean.TRUE</code>, in some cases the operator could set the
 * default. 
 *
 * <p><table align=center border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>GradientMagnitude</td></tr>
 * <tr><td>LocallName</td>  <td>GradientMagnitude</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Performs gradient magnitude edge detection
 *                              on an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jaiapi/com.lightcrafts.mediax.jai.operator.GradientMagnitudeDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>A gradient mask</td></tr>
 * <tr><td>arg1Desc</td>    <td>A gradient mask orthogonal to the first one.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>   <th>Class Type</th>
 *                     <th>Default Value</th></tr>
 * <tr><td>mask1</td>  <td>com.lightcrafts.mediax.jai.KernelJAI</td>
 *                     <td>KernalJAI.GRADIENT_MASK_SOBEL_HORIZONTAL</td>
 * <tr><td>mask2</td>  <td>com.lightcrafts.mediax.jai.KernelJAI</td>
 *                     <td>KernalJAI.GRADIENT_MASK_SOBEL_VERTICAL</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 * @see com.lightcrafts.mediax.jai.KernelJAI
 */
public class GradientMagnitudeDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for the GradientMagnitude operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "GradientMagnitude"},
        {"LocalName",   "GradientMagnitude"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("GradientMagnitudeDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jaiapi/com.lightcrafts.mediax.jai.operator.GradientMagnitudeDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    "A gradient mask."},
        {"arg1Desc",    "A gradient mask orthogonal to the first one."}
    };

    /** The parameter names for the GradientMagnitude operation. */
    private static final String[] paramNames = {
        "mask1",
        "mask2"
    };

    /** The parameter class types for the GradientMagnitude operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.mediax.jai.KernelJAI.class,
        com.lightcrafts.mediax.jai.KernelJAI.class
    };

    /** The parameter default values for the GradientMagnitude operation. */
    private static final Object[] paramDefaults = {
        KernelJAI.GRADIENT_MASK_SOBEL_HORIZONTAL,
        KernelJAI.GRADIENT_MASK_SOBEL_VERTICAL
    };

    /** Constructor for the GradientMagnitudeDescriptor. */
    public GradientMagnitudeDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Validates the input parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "mask1" and "mask2"
     * have the same dimensions.
     */
    protected boolean validateParameters(ParameterBlock args,
                                         StringBuffer msg) {
        if (!super.validateParameters(args, msg)) {
            return false;
        }

        KernelJAI h_kernel = (KernelJAI)args.getObjectParameter(0);
        KernelJAI v_kernel = (KernelJAI)args.getObjectParameter(1);

        /* Check if both kernels are equivalent in terms of dimensions. */
        if ((h_kernel.getWidth() != v_kernel.getWidth()) ||
            (h_kernel.getHeight() != v_kernel.getHeight())) {
            msg.append(getName() + " " +
                       JaiI18N.getString("GradientMagnitudeDescriptor1"));
                       return false;
        }
        
        return true;
    }

    /**
      * Returns an array of <code>PropertyGenerators</code> implementing
      * property inheritance for the "GradientMagnitude" operation.
      *
      * @return  An array of property generators.
      */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }


    /**
     * Computes the gradient of an image
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
     * @param mask1 A gradient mask.
     * May be <code>null</code>.
     * @param mask2 A gradient mask orthogonal to the first one.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    KernelJAI mask1,
                                    KernelJAI mask2,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("GradientMagnitude",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("mask1", mask1);
        pb.setParameter("mask2", mask2);

        return JAI.create("GradientMagnitude", pb, hints);
    }
}
