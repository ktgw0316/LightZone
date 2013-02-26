/*
 * $RCSfile: PhaseDescriptor.java,v $
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
 * An <code>OperationDescriptor</code> describing the "Phase" operation.
 *
 * <p> The "Phase" operation computes the phase angle of each pixel of a
 * complex image.  The source image must have an even number of bands, with
 * the even bands (0, 2, ...) representing the real parts and the odd bands
 * (1, 3, ...) the imaginary parts of each complex pixel.  The destination
 * image has at most half the number of bands of the source image with each
 * sample in a pixel representing the phase angle of the corresponding complex
 * source sample.  The angular values of the destination image are defined
 * for a given sample by the pseudocode:
 *
 * <pre>dst[x][y][b] = Math.atan2(src[x][y][2*b+1], src[x][y][2*b])</pre>
 *
 * where the number of bands <i>b</i> varies from zero to one less than the
 * number of bands in the destination image.
 *
 * <p> For integral image datatypes, the result will be rounded and
 * scaled so the the "natural" arctangent range [-PI, PI) is remapped into
 * the range [0, MAX_VALUE); the result for floating point image datatypes
 * is the value returned by the atan2() method.
 *
 * <p>"Phase" defines a PropertyGenerator that sets the "COMPLEX"
 * property of the image to <code>java.lang.Boolean.FALSE</code>, which may
 * be retrieved by calling the <code>getProperty()</code> method with
 * "COMPLEX" as the property name.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Phase</td></tr>
 * <tr><td>LocalName</td>   <td>Phase</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Computes the phase angle of each pixel of
 *                              an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PhaseDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * </table></p>
 *
 * <p> No parameters are needed for the "Phase" operation.
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class PhaseDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Phase"},
        {"LocalName",   "Phase"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("PhaseDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PhaseDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")}
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public PhaseDescriptor() {
        super(resources, supportedModes, 1, null, null, null, null);
    }

    /**
     * Validates the input source.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the source image
     * has an even number of bands.
     */
    protected boolean validateSources(String modeName,
				      ParameterBlock args,
                                      StringBuffer msg) {
        if (!super.validateSources(modeName, args, msg)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

	RenderedImage src = args.getRenderedSource(0);

        int bands = src.getSampleModel().getNumBands();

        if (bands % 2 != 0) {
            msg.append(getName() + " " +
                       JaiI18N.getString("PhaseDescriptor1"));
            return false;
        }

        return true;
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "Phase" operation.
     *
     * @return  An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators(String modeName) {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new ComplexPropertyGenerator();
        return pg;
    }


    /**
     * Computes the phase angle of each pixel of an image.
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
            new ParameterBlockJAI("Phase",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        return JAI.create("Phase", pb, hints);
    }

    /**
     * Computes the phase angle of each pixel of an image.
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
            new ParameterBlockJAI("Phase",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        return JAI.createRenderable("Phase", pb, hints);
    }
}
