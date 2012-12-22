/*
 * $RCSfile: ColorQuantizerDescriptor.java,v $
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
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.util.Range;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * This <code>OperationDescriptor</code> defines the "ColorQuantizer"
 * operation.
 *
 * <p> This operation generates an optimal lookup table (LUT) based on
 * the provided 3-band RGB source image by executing a color
 * quantization algorithm.  This LUT is stored in the property
 * "JAI.LookupTable" that has a type of <code>LookupTableJAI</code>.
 * Thus, it can be retrieved by means of <code>getProperty</code>.
 * This LUT can be further utilized in other operations such as
 * "errordiffusion" to convert the 3-band RGB image into a high-quality
 * color-indexed image.  The computation of the LUT can be deferred by
 * defining a <code>DeferredProperty</code> from the property
 * "JAI.LookupTable" and providing that as the parameter value for
 * "errordiffusion".  This operation also creates a color-indexed
 * destination image based on the nearest distance classification (without
 * dithering).  However, the quality of this classification result may
 * not be as good as the result of "errordiffusion".
 *
 * <p> The supported source image data type is implementation-dependent.
 * For example, the Sun implementation will support only the byte type.
 *
 * <p> The data set used in the color quantization can be defined by
 * the optional parameters <code>xPeriod</code>, <code>yPeriod</code>
 * and <code>ROI</code>.  If these parameters are provided, the pixels in
 * the subsampled image (and in the ROI) will be used to compute the
 * LUT.
 *
 * <p> Three built-in color quantization algorithms are supported by
 * this operation: Paul Heckbert's median-cut algorithm, Anthony Dekker's
 * NeuQuant algorithm, and the Oct-Tree color quantization algorithm of
 * Gervautz and Purgathofer.
 *
 * <p> The median-cut color quantization computes the 3D color histogram
 * first, then chooses and divides the largest color cube (in number of pixels)
 * along the median, until the required number of clusters is obtained
 * or all the cubes are not separable.  The NeuQuant algorithm creates
 * the cluster centers using Kohonen's self-organizing neural network.
 * The Oct-Tree color quantization constructs an oct-tree of the
 * color histogram, then repeatedly merges the offspring into the parent
 * if they contain a number of pixels smaller than a threshold.  With the
 * equivalent parameters, the median-cut algorithm is the fastest, and the
 * NeuQuant algorithm is the slowest.  However, NeuQuant algorithm can
 * still generate a good result with a relatively high subsample rate, which
 * is useful for large images.
 * In these three algorithms, the Oct-Tree algorithm is the most space
 * consuming one.  For further details of these algorithms,
 * please refer to the following references:
 * <table border=1>
 *   <tr>
 *      <th>Algorithm</th>
 *      <th>References</th>
 *   </tr>
 *   <tr>
 *      <td>Median-Cut</td>
 *      <td>Color Image Quantization for Frame Buffer
 *	    Display,  Paul Heckbert, SIGGRAPH proceedings, 1982, pp. 297-307
 *      </td></tr>
 *  <tr>
 *      <td>NeuQuant</td>
 *      <td>Kohonen Neural Networks for Optimal Colour Quantization,
 *          Anthony Dekker, In <i>Network: Computation in Neural Systems</i>,
 *          Volume 5, Institute of Physics Publishing, 1994, pp 351-367.
 *      </td>
 *  </tr>
 *  <tr>
 *      <td>Oct-Tree</td>
 *      <td><i>Interactive Computer Graphics: Functional, Procedural, and
 *          Device-Level Methods</i> by Peter Burger and Duncan Gillis,
 *          Addison-Wesley, 1989, pp 345.
 *      </td>
 *  </tr>
 *</table>
 *
 * <p> The generated LUT may have fewer entries than expected. For
 * example, the source image might not have as many colors as expected.
 * In the oct-tree algorithm, all the offspring of a node are merged
 * if they contain a number of pixels smaller than a threshold. This
 * may result in slightly fewer colors than expected.
 *
 * <p> The learning procedure of the NeuQuant algorithm randomly goes
 * through all the pixels in the training data set.  To simplify and
 * speed up the implementation, the bounding rectangle of the
 * provided ROI may be used (by the implementation) to define the
 * training data set instead of the ROI itself.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>ColorQuantizer</td></tr>
 * <tr><td>LocalName</td>   <td>ColorQuantizer</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Generates an optimal LUT by executing a
 *                              color quantization algorithm, and a
 *                              color-indexed image by the nearest distance
 *                              classification.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ColorQuantizerDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.1</td></tr>
 * <tr><td>arg0Desc</td>    <td>The color quantization algorithm name. One of
 *                              ColorQuantizerDescriptor.MEDIANCUT,
 *                              ColorQuantizerDescriptor.NEUQUANT, or
 *                              ColorQuantizerDescriptor.OCTTREE</td></tr>
 * <tr><td>arg1Desc</td>    <td>The maximum color number, that is, the expected
 *                              number of colors in the result image.</td></tr>
 * <tr><td>arg2Desc</td>    <td>This is an algorithm-dependent parameter.  For
 *                              the median-cut color quantization, it is the
 *                              maximum size of the three-dimensional
 *                              histogram.
 *                              For the neuquant color quantization, it is the
 *                              number of cycles.  For the oct-tree color
 *                              quantization, it is the maximum size of the
 *                              oct-tree.</td></tr>
 * <tr><td>arg3Desc</td>    <td>The ROI in which the pixels are involved into
 *                              the color quantization.</td></tr>
 * <tr><td>arg4Desc</td>    <td>The subsample rate in x direction.</td></tr>
 * <tr><td>arg4Desc</td>    <td>The subsample rate in y direction.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>      <th>Class Type</th>
 *                        <th>Default Value</th></tr>
 * <tr><td>quantizationAlgorithm</td>
 *                        <td>com.lightcrafts.mediax.jai.operator.ColorQuantizerType</td>
 *                        <td>ColorQuantizerDescriptor.MEDIANCUT</td>
 * <tr><td>maxColorNum</td>       <td>java.lang.Integer</td>
 *                        <td>256</td>
 * <tr><td>upperBound</td>   <td>java.lang.Integer</td>
 *                        <td>32768 for median-cut, 100 for neuquant,
 *                        65536 for oct-tree</td>
 * <tr><td>roi</td>   <td>com.lightcrafts.mediax.jai.ROI</td>
 *                        <td>null</td>
 * <tr><td>xPeriod</td>   <td>java.lang.Integer</td>
 *                        <td>1</td>
 * <tr><td>yPeriod</td>   <td>java.lang.Integer</td>
 *                        <td>1</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.ROI
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 *
 * @since JAI 1.1.2
 */
public class ColorQuantizerDescriptor extends OperationDescriptorImpl {
    /** The predefined color quantization algorithms. */
    /** The pre-defined median-cut color quantization algorithm. */
    public static final ColorQuantizerType MEDIANCUT =
        new ColorQuantizerType("MEDIANCUT", 1);
    /** The pre-defined NeuQuant color quantization algorithm. */
    public static final ColorQuantizerType NEUQUANT =
        new ColorQuantizerType("NEUQUANT", 2);
    /** The pre-defined Oct-Tree color quantization algorithm. */
    public static final ColorQuantizerType OCTTREE =
        new ColorQuantizerType("OCTTREE", 3);

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "ColorQuantizer"},
        {"LocalName",   "ColorQuantizer"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("ColorQuantizerDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ColorQuantizerDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion2")},
        {"arg0Desc",    JaiI18N.getString("ColorQuantizerDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("ColorQuantizerDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("ColorQuantizerDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("ColorQuantizerDescriptor4")},
        {"arg4Desc",    JaiI18N.getString("ColorQuantizerDescriptor5")},
        {"arg5Desc",    JaiI18N.getString("ColorQuantizerDescriptor6")},
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "quantizationAlgorithm",
        "maxColorNum",
        "upperBound",
        "roi",
        "xPeriod",
        "yPeriod"
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.mediax.jai.operator.ColorQuantizerType.class,
        java.lang.Integer.class,
        java.lang.Integer.class,
        com.lightcrafts.mediax.jai.ROI.class,
        java.lang.Integer.class,
        java.lang.Integer.class
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        MEDIANCUT,
        new Integer(256),
        null,
        null,
        new Integer(1),
        new Integer(1)
    };

    private static final String[] supportedModes = {
        "rendered"
    };

    /** Constructor. */
    public ColorQuantizerDescriptor() {
        super(resources, supportedModes, 1,
              paramNames, paramClasses, paramDefaults, null);

    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.
     */
    public Range getParamValueRange(int index) {
        switch (index) {
        case 1:
        case 2:
        case 4:
        case 5:
            return new Range(Integer.class, new Integer(1), null);
        }
        return null;
    }

    /**
     * Returns <code>true</code> if this operation is capable of handling
     * the input parameters.
     *
     * <p> In addition to the default validations done in the super class,
     * this method verifies that the provided quantization algorithm is one of
     * the three predefined algorithms in this class.
     *
     * @throws IllegalArgumentException  If <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException  If <code>msg</code> is <code>null</code>
     *         and the validation fails.
     */
    protected boolean validateParameters(String modeName,
                                         ParameterBlock args,
                                         StringBuffer msg) {
        if ( args == null || msg == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (!super.validateParameters(modeName, args, msg))
            return false;

        ColorQuantizerType algorithm =
            (ColorQuantizerType)args.getObjectParameter(0);
        if (algorithm != MEDIANCUT && algorithm != NEUQUANT &&
            algorithm != OCTTREE) {
            msg.append(getName() + " " +
                       JaiI18N.getString("ColorQuantizerDescriptor7"));
            return false;
        }

        Integer secondOne = (Integer)args.getObjectParameter(2);
        if (secondOne == null) {
            int upperBound = 0;
            if (algorithm.equals(MEDIANCUT))
                upperBound = 32768;
            else if (algorithm.equals(NEUQUANT))   // set the cycle for train to 100
                upperBound = 100;
            else if (algorithm.equals(OCTTREE))    // set the maximum tree size to 65536
                upperBound = 65536;

            args.set(upperBound, 2);
        }

        return true;
    }

    /**
     * Color quantization on the provided image.
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
     * @param algorithm The algorithm to be chosen.  May be <code>null</code>.
     * @param maxColorNum The maximum color number.  May be <code>null</code>.
     * @param upperBound An algorithm-dependent parameter.  See the parameter
     *                   table above.  May be <code>null</code>.
     * @param roi The region of interest.  May be <code>null</code>.
     * @param xPeriod The X subsample rate.  May be <code>null</code>.
     * @param yPeriod The Y subsample rate.  May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    ColorQuantizerType algorithm,
                                    Integer maxColorNum,
                                    Integer upperBound,
                                    ROI roi,
                                    Integer xPeriod,
                                    Integer yPeriod,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("ColorQuantizer",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("quantizationAlgorithm", algorithm);
        pb.setParameter("maxColorNum", maxColorNum);
        pb.setParameter("upperBound", upperBound);
        pb.setParameter("roi", roi);
        pb.setParameter("xPeriod", xPeriod);
        pb.setParameter("yPeriod", yPeriod);

        return JAI.create("ColorQuantizer", pb, hints);
    }
}
