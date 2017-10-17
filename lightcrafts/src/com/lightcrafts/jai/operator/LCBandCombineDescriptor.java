/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: BandCombineDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:30 $
 * $State: Exp $
 */
package com.lightcrafts.jai.operator;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderableRegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "BandCombine" operation.
 *
 * <p> The BandCombing operation computes a set of arbitrary linear
 * combinations of the bands of a rendered or renderable source image,
 * using a specified matrix.  The matrix must a number of rows equal to
 * the number of desired destination bands and a number of columns equal to the
 * number of source bands plus one.  In other words, the array may be
 * constructed using the syntax:
 *
 * <pre>
 * double[][] matrix = new double[destBands][sourceBands + 1];
 * </pre>
 *
 * <p> The number of source bands used to determine the matrix dimensions
 * is given by <code>source.getSampleModel().getNumBands()</code> regardless
 * of the type of <code>ColorModel</code> the source has.
 *
 * <p> The extra column in the matrix contains constant values each of which
 * is added to the respective band of the destination. The transformation is
 * therefore defined by the pseudocode:
 *
 * <pre>
 * // s = source pixel
 * // d = destination pixel
 * for(int i = 0; i < destBands; i++) {
 *     d[i] = matrix[i][sourceBands];
 *     for(int j = 0; j < sourceBands; j++) {
 *         d[i] += matrix[i][j]*s[j];
 *     }
 * }
 * </pre>
 *
 * <p> If the result of the computation underflows/overflows the
 * minimum/maximum value supported by the destination image, then it
 * will be clamped to the minimum/maximum value respectively.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>BandCombine</td></tr>
 * <tr><td>LocalName</td>   <td>BandCombine</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts</td></tr>
 * <tr><td>Description</td> <td>Performs arbitrary interband linear combination
 *                              using a specified matrix.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BandCombineDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The matrix specifying the band combination.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>   <th>Class Type</th>
 *                     <th>Default Value</th></tr>
 * <tr><td>matrix</td> <td>double[][]</td>
 *                     <td>NO_PARAMETER_DEFAULT</td>
 * </table></p>
 *
 * @see javax.media.jai.OperationDescriptor
 */
public class LCBandCombineDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "LCBandCombine"},
        {"LocalName",   "LCBandCombine"},
        {"Vendor",      "com.lightcrafts.jai"},
        {"Description", "Faster BandCombine Operation"},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BandCombineDescriptor.html"},
        {"Version",     "1.0"},
        {"arg0Desc",    "An Image"}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        double[][].class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "matrix"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public LCBandCombineDescriptor() {
        super(resources, supportedModes, 1,
		paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input source and parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "matrix" has at
     * least 1 row and (source bands + 1) columns.
     *
     * <p> The number of source bands is considered to be equal to
     * <code>source.getSampleModel().getNumBands()</code>.
     */
    public boolean validateArguments(String modeName,
				     ParameterBlock args,
                                     StringBuffer message) {
        if (!super.validateArguments(modeName, args, message)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

	RenderedImage src = args.getRenderedSource(0);

        double[][] matrix = (double[][])args.getObjectParameter(0);
        SampleModel sm = src.getSampleModel();
        int rowLength = sm.getNumBands() + 1;

        if (matrix.length < 1) {
            message.append(getName())
                   .append(": ")
                   .append("bad matrix dimensions");
            return false;
        }

        for (double[] aMatrix : matrix) {
            if (aMatrix.length != rowLength) {
                message.append(getName())
                       .append(": ")
                       .append("bad matrix dimensions");
                return false;
            }
        }

        return true;
    }


    /**
     * Performs arbitrary interband linear combination using a specified matrix.
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
     * @param matrix The matrix specifying the band combination.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>matrix</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    double[][] matrix,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("LCBandCombine",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("matrix", matrix);

        return JAI.create("LCBandCombine", pb, hints);
    }

    /**
     * Performs arbitrary interband linear combination using a specified matrix.
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
     * @param matrix The matrix specifying the band combination.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>matrix</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                double[][] matrix,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("LCBandCombine",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("matrix", matrix);

        return JAI.createRenderable("LCBandCombine", pb, hints);
    }
}
