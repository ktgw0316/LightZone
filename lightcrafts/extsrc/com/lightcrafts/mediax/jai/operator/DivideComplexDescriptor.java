/*
 * $RCSfile: DivideComplexDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:34 $
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
import com.lightcrafts.mediax.jai.PropertyGenerator;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "DivideComplex"
 * operation.
 *
 * <p> The "DivideComplex" operation divides two images representing
 * complex data.  The source images must each contain an even number of bands
 * with the even-indexed bands (0, 2, ...) representing the real and the
 * odd-indexed bands (1, 3, ...) the imaginary parts of each pixel.  The
 * destination image similarly contains an even number of bands with the
 * same interpretation and with contents defined by:
 *
 * <pre>
 * a = src0[x][y][2*k];
 * b = src0[x][y][2*k+1];
 * c = src1[x][y][2*k];
 * d = src1[x][y][2*k+1];
 *
 * dst[x][y][2*k] = (a*c + b*d)/(c^2 + d^2)
 * dst[x][y][2*k+1] = (b*c - a*d)/(c^2 + d^2)
 * </pre>
 *
 * where 0 <= <i>k</i> < numBands/2.
 *
 * By default, the number of bands of the destination image is the
 * the minimum of the number of bands of the two sources, and the
 * data type is the biggest data type of the sources.
 * However, the number of destination bands can be specified to be
 * M = 2*L through an <code>ImageLayout</code> hint, when 
 * one source image has 2 bands and the other has N = 2*K bands
 * where K > 1, with a natural restriction 1 <= L <= K.
 * In such a special case,
 * if the first source has 2 bands its single complex component
 * will be divided by each of the first L complex components of the second
 * source; if the second source has 2 bands its single complex component will
 * divide each of the L complex components of the first source.
 *
 * <p> If the result of the operation underflows/overflows the
 * minimum/maximum value supported by the destination data type, then it will
 * be clamped to the minimum/maximum value respectively.
 *
 * <p>"DivideComplex" defines a PropertyGenerator that sets the "COMPLEX"
 * property of the image to <code>java.lang.Boolean.TRUE</code>, which may
 * be retrieved by calling the <code>getProperty()</code> method with
 * "COMPLEX" as the property name.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>DivideComplex</td></tr>
 * <tr><td>LocalName</td>   <td>DivideComplex</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Compute the complex quotient of two images.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/DivideComplexDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * </table></p>
 *
 * <p> No parameters are needed for the "DivideComplex" operation.
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class DivideComplexDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "DivideComplex"},
        {"LocalName",   "DivideComplex"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("DivideComplexDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/DivideComplexDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")}
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public DivideComplexDescriptor() {
        super(resources, supportedModes, 2, null, null, null, null);
    }

    /**
     * Validates the input sources.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that both sources have
     * an even number of bands.
     */
    protected boolean validateSources(String modeName,
				      ParameterBlock args,
                                      StringBuffer msg) {
        if (!super.validateSources(modeName, args, msg)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

        RenderedImage src1 = args.getRenderedSource(0);
        RenderedImage src2 = args.getRenderedSource(1);

        if (src1.getSampleModel().getNumBands() % 2 != 0 ||
            src2.getSampleModel().getNumBands() % 2 != 0) {
            msg.append(getName() + " " +
                       JaiI18N.getString("DivideComplexDescriptor1"));
            return false;
        }

        return true;
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "DivideComplex" operation.
     *
     * @return  An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators(String modeName) {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new ComplexPropertyGenerator();
        return pg;
    }


    /**
     * Compute the complex quotient of two images.
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
     * @param source1 <code>RenderedImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    RenderedImage source1,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("DivideComplex",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        return JAI.create("DivideComplex", pb, hints);
    }

    /**
     * Compute the complex quotient of two images.
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
     * @param source1 <code>RenderableImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                RenderableImage source1,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("DivideComplex",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        return JAI.createRenderable("DivideComplex", pb, hints);
    }
}
