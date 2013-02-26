/*
 * $RCSfile: ClampDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:31 $
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
 * An <code>OperationDescriptor</code> describing the "Clamp" operation.
 *
 * <p> The Clamp operation takes one rendered or renderable source
 * image, and sets all the pixels whose value is below a "low" value
 * to that low value and all the pixels whose value is above a "high"
 * value to that high value.  The pixels whose value is between the
 * "low" value and the "high" value are left unchanged.
 *
 * <p> A different set of "low" and "high" values may be applied to each
 * band of the source image, or the same set of "low" and "high" values
 * may be applied to all bands of the source. If the number of "low" and
 * "high" values supplied is less than the number of bands of the source,
 * then the values from entry 0 are applied to all the bands. Each "low"
 * value must be less than or equal to its corresponding "high" value.
 *
 * <p> The destination pixel values are defined by the pseudocode:
 * <pre>
 * lowVal = (low.length < dstNumBands) ?
 *          low[0] : low[b];
 * highVal = (high.length < dstNumBands) ?
 *           high[0] : high[b];
 *
 * if (src[x][y][b] < lowVal) {
 *     dst[x][y][b] = lowVal;
 * } else if (src[x][y][b] > highVal) {
 *     dst[x][y][b] = highVal;
 * } else {
 *     dst[x][y][b] = src[x][y][b];
 * }
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Clamp</td></tr>
 * <tr><td>LocalName</td>   <td>Clamp</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Clamps the pixel values of an image
 *                              to a specified range.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ClampDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The lower boundary for each band.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The upper boundary for each band.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th> <th>Class Type</th>
 *                   <th>Default Value</th></tr>
 * <tr><td>low</td>  <td>double[]</td>
 *                   <td>{0.0}</td>
 * <tr><td>high</td> <td>double[]</td>
 *                   <td>{255.0}</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class ClampDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Clamp"},
        {"LocalName",   "Clamp"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("ClampDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ClampDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("ClampDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("ClampDescriptor2")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
	double[].class, double[].class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "low", "high"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        new double[] {0.0}, new double[] {255.0}
    };

    /** Constructor. */
    public ClampDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    /**
     * Validates the input parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "low" and "high"
     * have length at least 1 and that each "low" value is less than
     * or equal to the corresponding "high" value.
     */
    protected boolean validateParameters(ParameterBlock args,
                                         StringBuffer msg) {
        if (!super.validateParameters(args, msg)) {
            return false;
        }

        double[] low = (double[])args.getObjectParameter(0);
        double[] high = (double[])args.getObjectParameter(1);

        if (low.length < 1 || high.length < 1) {
            msg.append(getName() + " " +
                       JaiI18N.getString("ClampDescriptor3"));
            return false;
        }

        int length = Math.min(low.length, high.length);
        for (int i = 0; i < length; i++) {
            if (low[i] > high[i]) {
                msg.append(getName() + " " +
                          JaiI18N.getString("ClampDescriptor4"));
                return false;
            }
        }

        return true;
    }


    /**
     * Clamps the pixel values of an image to a specified range.
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
     * @param low The lower boundary for each band.
     * May be <code>null</code>.
     * @param high The upper boundary for each band.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    double[] low,
                                    double[] high,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Clamp",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("low", low);
        pb.setParameter("high", high);

        return JAI.create("Clamp", pb, hints);
    }

    /**
     * Clamps the pixel values of an image to a specified range.
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
     * @param low The lower boundary for each band.
     * May be <code>null</code>.
     * @param high The upper boundary for each band.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                double[] low,
                                                double[] high,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Clamp",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("low", low);
        pb.setParameter("high", high);

        return JAI.createRenderable("Clamp", pb, hints);
    }
}
