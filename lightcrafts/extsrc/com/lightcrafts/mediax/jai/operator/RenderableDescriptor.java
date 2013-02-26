/*
 * $RCSfile: RenderableDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:43 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;


/**
 * An <code>OperationDescriptor</code> describing the "Renderable" operation.
 *
 * <p> In renderable image mode the "Renderable" operation produces from a
 * <code>RenderedImage</code> source a <code>RenderableImage</code>
 * consisting of a "pyramid" of <code>RenderedImage</code>s at progressively
 * lower resolutions.  This operation does not support rendered image mode.
 *
 * <p> Lower resolution images are produced by invoking the chain of
 * operations specified via the "downSampler" parameter on the image at the
 * next higher resolution level of the pyramid.  The "downSampler" operation
 * chain must adhere to the specifications described for the constructors of
 * the <code>ImageMIPMap</code> class which accept this type of parameter.
 * The "downSampler" operation chain must reduce the image width and height at
 * each level of the pyramid.  The default operation chain for "downSampler"
 * is a low pass filtering implemented using a 5x5 separable kernel derived
 * from the one-dimensional kernel
 *
 * <pre>
 * [0.05 0.25 0.40 0.25 0.05]
 * </pre>
 *
 * followed by downsampling by 2.
 *
 * <p> The number of levels in the pyramid will be such that the maximum of
 * the width and height of the lowest resolution pyramid level is less than or
 * equal to the value of the "maxLowResDim" parameter which must be positive.
 *
 * <p> The minimum X and Y coordinates and height in rendering-independent
 * coordinates are supplied by the parameters "minX", "minY", and "height",
 * respectively.  The value of "height" must be positive.  It is not
 * necessary to supply a value for the rendering-independent width as this
 * is derived by multiplying the supplied height by the aspect ratio (width
 * divided by height) of the source <code>RenderedImage</code>.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Renderable</td></tr>
 * <tr><td>LocalName</td>   <td>Renderable</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Produces a RenderableImage from a RenderedImage.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/RenderableDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The operation chain used to derive the lower resolution images.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The maximum dimension of the lowest resolution pyramid level.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The minimum rendering-independent X coordinate of the destination.</td></tr>
 * <tr><td>arg3Desc</td>    <td>The minimum rendering-independent Y coordinate of the destination.</td></tr>
 * <tr><td>arg4Desc</td>    <td>The rendering-independent height.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>         <th>Class Type</th>
 *                           <th>Default Value</th></tr>
 * <tr><td>downSampler</td>  <td>RenderedOp</td>
 *                           <td>null</td>
 * <tr><td>maxLowResDim</td> <td>Integer</td>
 *                           <td>64</td>
 * <tr><td>minX</td>         <td>Float</td>
 *                           <td>0.0F</td>
 * <tr><td>minY</td>         <td>Float</td>
 *                           <td>0.0F</td>
 * <tr><td>height</td>       <td>Float</td>
 *                           <td>1.0F</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.ImageMIPMap
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class RenderableDescriptor extends OperationDescriptorImpl {
    // One-dimensional kernel from which to create the default separable
    // two-dimensional low-pass filter in the downsampler chain.
    private static final float[] DEFAULT_KERNEL_1D =
        {0.05F, 0.25F, 0.4F, 0.25F, 0.05F};

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Renderable"},
        {"LocalName",   "Renderable"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("RenderableDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/RenderableDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("RenderableDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("RenderableDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("RenderableDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("RenderableDescriptor4")},
        {"arg4Desc",    JaiI18N.getString("RenderableDescriptor5")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        RenderedOp.class, Integer.class, Float.class, Float.class, Float.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "downSampler", "maxLowResDim", "minX", "minY", "height"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        null, new Integer(64),
        new Float(0.0F), new Float(0.0F), new Float(1.0F)
    };

    /** Constructor. */
    public RenderableDescriptor() {
        super(resources,
              null, // rendered mode not supported -> null
              new Class[] {RenderedImage.class}, // renderable mode
              paramClasses, paramNames, paramDefaults);
    }

    /** Indicates that rendered operation is supported. */
    public boolean isRenderedSupported() {
        return false;
    }

    /** Indicates that renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    /** Validates input parameters in the renderable layer. */
    protected boolean validateParameters(ParameterBlock args,
                                         StringBuffer msg) {
        // Set the default down sampler if necessary.
        if(args.getNumParameters() == 0 ||
           args.getObjectParameter(0) == null) {
            // Create the down-sampler operation chain consisting of a 5x5
            // Gaussian filter followed by a subsampling by 2. Use a kernel
            // which satisfies the description in P. J. Burt and
            // E. H. Adelson, "The Laplacian pyramid as a compact image code", 
            // IEEE Transactions on Communications., pp. 532-540, 1983. 

            // Add the filtering operation.
            ParameterBlock pb = new ParameterBlock();
            KernelJAI kernel = new KernelJAI(DEFAULT_KERNEL_1D.length,
                                             DEFAULT_KERNEL_1D.length,
                                             DEFAULT_KERNEL_1D.length/2,
                                             DEFAULT_KERNEL_1D.length/2,
                                             DEFAULT_KERNEL_1D,
                                             DEFAULT_KERNEL_1D);
            pb.add(kernel);
            BorderExtender extender =
                BorderExtender.createInstance(BorderExtender.BORDER_COPY);
            RenderingHints hints =
                JAI.getDefaultInstance().getRenderingHints();
            if(hints == null) {
                hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, extender);
            } else {
                hints.put(JAI.KEY_BORDER_EXTENDER, extender);
            }
                                   
            RenderedOp filter = new RenderedOp("convolve", pb, hints);

            // Add the subsampling operation.
            pb = new ParameterBlock();
            pb.addSource(filter);
            pb.add(new Float(0.5F)).add(new Float(0.5F));
            pb.add(new Float(0.0F)).add(new Float(0.0F));
            pb.add(Interpolation.getInstance(Interpolation.INTERP_NEAREST));
            RenderedOp downSampler = new RenderedOp("scale", pb, null);
            args.set(downSampler, 0);
        }

        // Verify the generic integrity of the arguments and set defaults.
        if(!super.validateParameters(args, msg)) {
            return false;
        }

        // Make sure the maximum dimension and the height are both positive.
        if(args.getIntParameter(1) <= 0) {
            msg.append(getName() + " " +
                       JaiI18N.getString("RenderableDescriptor6"));
            return false;
        } else if(args.getFloatParameter(4) <= 0.0F) {
            msg.append(getName() + " " +
                       JaiI18N.getString("RenderableDescriptor7"));
            return false;
        }

        return true;
    }


    /**
     * Produces a RenderableImage from a RenderedImage.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     *
     * @param source0 <code>RenderedImage</code> source 0.
     * @param downSampler The operation chain used to derive the lower resolution images.
     * May be <code>null</code>.
     * @param maxLowResDim The maximum dimension of the lowest resolution pyramid level.
     * May be <code>null</code>.
     * @param minX The minimum rendering-independent X coordinate of the destination.
     * May be <code>null</code>.
     * @param minY The minimum rendering-independent Y coordinate of the destination.
     * May be <code>null</code>.
     * @param height The rendering-independent height.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderedImage source0,
                                                RenderedOp downSampler,
                                                Integer maxLowResDim,
                                                Float minX,
                                                Float minY,
                                                Float height,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Renderable",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("downSampler", downSampler);
        pb.setParameter("maxLowResDim", maxLowResDim);
        pb.setParameter("minX", minX);
        pb.setParameter("minY", minY);
        pb.setParameter("height", height);

        return JAI.createRenderable("Renderable", pb, hints);
    }
}
