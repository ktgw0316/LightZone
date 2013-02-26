/*
 * $RCSfile: AWTImageDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:28 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import java.awt.Image;
import java.awt.RenderingHints;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "AWTImage" operation.
 *
 * <p> The AWTImage operation converts a standard
 * <code>java.awt.Image</code> into a rendered image. By default, the
 * width and height of the image are the same as the original AWT
 * image. The sample model and color model are set according to the
 * AWT image data.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>AWTImage</td></tr>
 * <tr><td>LocalName</td>   <td>AWTImage</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Converts a <code>java.awt.Image</code>
 *                              into a rendered image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/AWTImageDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The AWT image to be converted.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>     <th>Class Type</th>
 *                       <th>Default Value</th></tr>
 * <tr><td>awtImage</td> <td>java.awt.Image</td>
 *                       <td>NO_PARAMETER_DEFAULT</td>
 * </table></p>
 *
 * @see java.awt.Image
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class AWTImageDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "AWTImage"},
        {"LocalName",   "AWTImage"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("AWTImageDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/AWTImageDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("AWTImageDescriptor1")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        java.awt.Image.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "awtImage"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT
    };

    /** Constructor. */
    public AWTImageDescriptor() {
        super(resources, 0, paramClasses, paramNames, paramDefaults);
    }


    /**
     * Converts a java.awt.Image into a rendered image.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param awtImage The AWT image to be converted.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>awtImage</code> is <code>null</code>.
     */
    public static RenderedOp create(Image awtImage,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("AWTImage",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setParameter("awtImage", awtImage);

        return JAI.create("AWTImage", pb, hints);
    }
}
