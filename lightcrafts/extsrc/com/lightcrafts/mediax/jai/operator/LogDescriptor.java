/*
 * $RCSfile: LogDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:38 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Log" operation.
 *
 * <p> The "Log" operation takes the natural logarithm of the pixel
 * values of an image.  The operation is done on a per-pixel, per-band
 * basis.  For integral data types, the result will be rounded and
 * clamped as needed.  The pixel values of the destination image are
 * defined as:
 * <pre>
 * dst[x][y][b] = java.lang.Math.log(src[x][y][b])
 * </pre>
 *
 * <p> For all integral data types, the log of 0 is set to 0.  For
 * signed integral data types (<code>short</code> and <code>int</code>),
 * the log of a negative pixel value is set to -1.
 *
 * <p> For all floating point data types ((<code>float</code> and
 * <code>double</code>), the log of 0 is set to <code>-Infinity</code>,
 * and the log of a negative pixel value is set to <code>NaN</code>.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Log</td></tr>
 * <tr><td>LocalName</td>   <td>Log</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Computes the natural logarithm of the pixel values
 *                              of an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/LogDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * </table></p>
 *
 * <p> No parameters are needed for the "Log" operation.
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class LogDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Log"},
        {"LocalName",   "Log"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("LogDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/LogDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")}
    };

    /** Constructor. */
    public LogDescriptor() {
        super(resources, 1, null, null, null);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }


    /**
     * Computes the natural logarithm of the pixel values of an image.
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
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Log",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        return JAI.create("Log", pb, hints);
    }

    /**
     * Computes the natural logarithm of the pixel values of an image.
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
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Log",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        return JAI.createRenderable("Log", pb, hints);
    }
}
