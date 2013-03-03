/*
 * $RCSfile: PatternDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:42 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Pattern" operation.
 *
 * <p> The "Pattern" operation defines a tiled image consisting of a
 * repeated pattern. The width and height of the destination image
 * must be specified. The tileWidth and tileHeight are equal to pattern's
 * width and height. Each tile of the destination image will be defined
 * by a reference to a shared instance of the pattern.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>pattern</td></tr>
 * <tr><td>LocalName</td>   <td>pattern</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Defines an image with a repeated
 *                              pattern.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PatternDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The width of the image in pixels.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The height of the image in pixels.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>    <th>Class Type</th>
 *                      <th>Default Value</th></tr>
 * <tr><td>width</td>   <td>java.lang.Integer</td>
 *                      <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>height</td>  <td>java.lang.Integer</td>
 *                      <td>NO_PARAMETER_DEFAULT</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class PatternDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * for the "Pattern" operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Pattern"},
        {"LocalName",   "Pattern"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("PatternDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PatternDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("PatternDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("PatternDescriptor2")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
	java.lang.Integer.class,
        java.lang.Integer.class,
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "width", "height"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT
    };

    /** Constructor. */
    public PatternDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    public Number getParamMinValue(int index) {
        if (index == 0 || index == 1) {
            return new Integer(1);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }


    /**
     * Defines an image with a repeated pattern.
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
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>width</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>height</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Integer width,
                                    Integer height,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Pattern",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("width", width);
        pb.setParameter("height", height);

        return JAI.create("Pattern", pb, hints);
    }
}
