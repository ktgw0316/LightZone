/*
 * $RCSfile: PiecewiseDescriptor.java,v $
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
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Piecewise" operation.
 *
 * <p> The "Piecewise" operation performs a piecewise linear mapping of the
 * pixel values of an image.  The piecewise linear mapping is described by a
 * set of breakpoints which are provided as an array of the form
 * <pre>float breakPoints[N][2][numBreakPoints]</pre> where the value of
 * <i>N</i> may be either unity or the number of bands in the source image.
 * If <i>N</i> is unity then the same set of breakpoints will be applied to
 * all bands in the image.  The abscissas of the supplied breakpoints must
 * be monotonically increasing.
 *
 * <p> The pixel values of the destination image are defined by the pseudocode:
 *
 * <pre>
 * if (src[x][y][b] < breakPoints[b][0][0]) {
 *     dst[x][y][b] = breakPoints[b][1][0]);
 * } else if (src[x][y][b] > breakPoints[b][0][numBreakPoints-1]) {
 *     dst[x][y][b] = breakPoints[b][1][numBreakPoints-1]);
 * } else {
 *     int i = 0;
 *     while(breakPoints[b][0][i+1] < src[x][y][b]) {
 *         i++;
 *     }
 *     dst[x][y][b] = breakPoints[b][1][i] +
 *                        (src[x][y][b] - breakPoints[b][0][i])*
 *                        (breakPoints[b][1][i+1] - breakPoints[b][1][i])/
 *                        (breakPoints[b][0][i+1] - breakPoints[b][0][i]);
 * }
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Piecewise</td></tr>
 * <tr><td>LocalName</td>   <td>Piecewise</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Applies a piecewise pixel value mapping.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PiecewiseDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The breakpoint array.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>         <th>Class Type</th>
 *                           <th>Default Value</th></tr>
 * <tr><td>breakPoints</td>  <td>float[][][]</td>
 *                           <td>identity mapping on [0, 255]</td>
 * </table></p>
 *
 * @see java.awt.image.DataBuffer
 * @see com.lightcrafts.mediax.jai.ImageLayout
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class PiecewiseDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Piecewise"},
        {"LocalName",   "Piecewise"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("PiecewiseDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PiecewiseDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    "The breakpoint array."}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        float[][][].class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "breakPoints"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        new float[][][]{{{0.0f, 255.0f}, {0.0f, 255.0f}}}
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public PiecewiseDescriptor() {
        super(resources, supportedModes, 1,
		paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input source and parameter.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the number of bands
     * in "breakPoints" is either 1 or the number of bands in the
     * source image, the second breakpoint array dimension is 2,
     * the third dimension is the same for abscissas and ordinates,
     * and that the absicssas are monotonically increasing.
     */
    public boolean validateArguments(String modeName,
				     ParameterBlock args,
                                     StringBuffer msg) {
        if (!super.validateArguments(modeName, args, msg)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

	// Get the source and the breakpoint array.
        RenderedImage src = args.getRenderedSource(0);

        float[][][] breakPoints =
            (float[][][])args.getObjectParameter(0);

        // Ensure that the number of breakpoint bands is either 1 or
        // the number of bands in the source image, the second
        // breakpoint array dimension is 2, the third dimension is
        // the same for abscissas and ordinates, and that the absicssas
        // are monotonically increasing.
        if (breakPoints.length != 1 &&
            breakPoints.length != src.getSampleModel().getNumBands()) {
            // Number of breakpoints not 1 nor numBands.
            msg.append(getName() + " " +
                       JaiI18N.getString("PiecewiseDescriptor1"));
            return false;
        } else {
	    int numBands = breakPoints.length;
	    for (int b = 0; b < numBands; b++) {
	        if (breakPoints[b].length != 2) {
                    // Second breakpoint dimension not 2.
		    msg.append(getName() + " " +
			       JaiI18N.getString("PiecewiseDescriptor2"));
		    return false;
		} else if (breakPoints[b][0].length !=
                          breakPoints[b][1].length) {
                    // Differing numbers of abscissas and ordinates.
		    msg.append(getName() + " " +
			       JaiI18N.getString("PiecewiseDescriptor3"));
		    return false;
                }
	    }
	    for (int b = 0; b < numBands; b++) {
                int count = breakPoints[b][0].length - 1;
                float[] x = breakPoints[b][0];
                for (int i = 0; i < count; i++) {
                    if (x[i] >= x[i+1]) {
                        // Abscissas not monotonically increasing.
                        msg.append(getName() + " " +
                                   JaiI18N.getString("PiecewiseDescriptor4"));
                        return false;
                    }
                }
	    }
	}

        return true;
    }


    /**
     * Applies a piecewise pixel value mapping.
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
     * @param breakPoints The breakpoint array.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    float[][][] breakPoints,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Piecewise",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("breakPoints", breakPoints);

        return JAI.create("Piecewise", pb, hints);
    }

    /**
     * Applies a piecewise pixel value mapping.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     *
     * @param source0 <code>RenderableImage</code> source 0.
     * @param breakPoints The breakpoint array.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                float[][][] breakPoints,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Piecewise",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("breakPoints", breakPoints);

        return JAI.createRenderable("Piecewise", pb, hints);
    }
}
