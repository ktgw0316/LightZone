/*
 * $RCSfile: PolarToComplexDescriptor.java,v $
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
import com.lightcrafts.mediax.jai.PropertyGenerator;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "PolarToComplex"
 * operation.
 *
 * <p> The "PolarToComplex" operation creates an image with complex-valued
 * pixels from two images the respective pixel values of which represent the
 * magnitude (modulus) and phase of the corresponding complex pixel in the
 * destination image.  The source images should have the same number of bands.
 * The first source image contains the magnitude values and the second source
 * image the phase values.  The destination will have twice as many bands with
 * the even-indexed bands (0, 2, ...) representing the real and the
 * odd-indexed bands (1, 3, ...) the imaginary parts of each pixel.  The
 * pixel values of the destination image are defined for a given complex
 * sample by the pseudocode:
 *
 * <pre>
 * dst[x][y][2*b]   = src0[x][y][b]*Math.cos(src1[x][y][b])
 * dst[x][y][2*b+1] = src0[x][y][b]*Math.sin(src1[x][y][b])
 * </pre>
 *
 * where the index <i>b</i> varies from zero to one less than the number
 * of bands in the source images.
 *
 * <p> For phase images with integral data type, it is assumed that the actual
 * phase angle is scaled from the range [-PI, PI] to the range [0, MAX_VALUE]
 * where MAX_VALUE is the maximum value of the data type in question.
 *
 * <p>"PolarToComplex" defines a PropertyGenerator that sets the "COMPLEX"
 * property of the image to <code>java.lang.Boolean.TRUE</code>, which may
 * be retrieved by calling the <code>getProperty()</code> method with
 * "COMPLEX" as the property name.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>PolarToComplex</td></tr>
 * <tr><td>LocalName</td>   <td>PolarToComplex</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Computes a complex image from a magnitude and a phase image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PolarToComplexDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * </table></p>
 *
 * <p> No parameters are needed for the "PolarToComplex" operation.
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 * @see com.lightcrafts.mediax.jai.operator.PhaseDescriptor
 */
public class PolarToComplexDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "PolarToComplex"},
        {"LocalName",   "PolarToComplex"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("PolarToComplexDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PolarToComplexDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")}
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public PolarToComplexDescriptor() {
        super(resources, supportedModes, 2, null, null, null, null);
    }

    /**
     * Validates the input sources.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the source images
     * have the same number of bands.
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

        if (src1.getSampleModel().getNumBands() !=
            src2.getSampleModel().getNumBands()) {
            msg.append(getName() + " " +
                       JaiI18N.getString("PolarToComplexDescriptor1"));
            return false;
        }

        return true;
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "Conjugate" operation.
     *
     * @return  An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators(String modeName) {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new ComplexPropertyGenerator();
        return pg;
    }


    /**
     * Computes a complex image from a magnitude and a phase image.
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
            new ParameterBlockJAI("PolarToComplex",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        return JAI.create("PolarToComplex", pb, hints);
    }

    /**
     * Computes a complex image from a magnitude and a phase image.
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
            new ParameterBlockJAI("PolarToComplex",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        return JAI.createRenderable("PolarToComplex", pb, hints);
    }
}
