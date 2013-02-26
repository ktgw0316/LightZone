/*
 * $RCSfile: HistogramDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:36 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * This <code>OperationDescriptor</code> defines the "Histogram"
 * operation.
 *
 * <p> A histogram of an image is represented by a list of "bins"
 * where each bin is the total number of pixel samples of the image
 * whose values lie within a given range.  This data are encapulated
 * in the <code>com.lightcrafts.mediax.jai.Histogram</code> object, and may be
 * retrieved by calling the <code>getProperty</code> method on this
 * operator with "<i>histogram</i>" as the property name.
 *
 * <p> At a request for the histogram property, this operator scans
 * the specific region of the source image, generates the pixel count
 * data, and returns an instance of the <code>Histogram</code> class
 * where the data are stored.  The source image's pixels are unchanged
 * by this operator.
 *
 * <p> The region-of-interest (ROI), within which the pixels are counted,
 * does not have to be a rectangle.  It may be <code>null</code>, in
 * which case the entire image is scanned to accumulate the histogram.
 *
 * <p> The set of pixels scanned may be further reduced by specifying
 * the "xPeriod" and "yPeriod" parameters that represent the sampling
 * rate along the two axis.  These variables may not be less than 1.
 * If they are not set, the default value of 1 is used so that every
 * pixel within the ROI is counted.
 *
 * <p> The three arguments, <code>numBins</code>, <code>lowValue</code>,
 * and <code>highValue</code>, define the type of the histogram to be
 * generated.  Please see the <code>Histogram</code> specification for
 * their detailed descriptions.  The three arrays must either have an
 * array length of 1, in which case the same value is applied to all
 * bands of the source image, or an array length that equals to the
 * number of bands of the source image, in which case each value is
 * applied to its corresponding band.  The <code>numBins</code> must
 * all be greater than 0, and each <code>lowValue</code> must be less
 * than its corresponding <code>highValue</code>.  Note that the default
 * values of these three parameters are specific to the case wherein
 * the image data are of type <code>byte</code>.  For other image data
 * types the values of these parameters should be supplied explicitely.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Histogram</td></tr>
 * <tr><td>LocalName</td>   <td>Histogram</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Generates a histogram based on the pixel values
 *                              within a specific region of an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/HistogramDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.1</td></tr>
 * <tr><td>arg0Desc</td>    <td>The region of the image to be scanned.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The horizontal sampling rate;
 *                              may not be less than 1.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The vertical sampling rate;
 *                              may not be less than 1.</td></tr>
 * <tr><td>arg3Desc</td>    <td>The number of bins for each band.</td></tr>
 * <tr><td>arg4Desc</td>    <td>The lowest inclusive pixel value to be
 *                              checked for each band.</td></tr>
 * <tr><td>arg5Desc</td>    <td>The highest exclusive pixel value to be
 *                              checked for each band.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>      <th>Class Type</th>
 *                        <th>Default Value</th></tr>
 * <tr><td>roi</td>       <td>com.lightcrafts.mediax.jai.ROI</td>
 *                        <td>null</td>
 * <tr><td>xPeriod</td>   <td>java.lang.Integer</td>
 *                        <td>1</td>
 * <tr><td>yPeriod</td>   <td>java.lang.Integer</td>
 *                        <td>1</td>
 * <tr><td>numBins</td>   <td>int[]</td>
 *                        <td>{256}</td>
 * <tr><td>lowValue</td>  <td>double[]</td>
 *                        <td>{0.0}</td>
 * <tr><td>highValue</td> <td>double[]</td>
 *                        <td>{256.0}</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.Histogram
 * @see com.lightcrafts.mediax.jai.ROI
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class HistogramDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Histogram"},
        {"LocalName",   "Histogram"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("HistogramDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/HistogramDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion2")},
        {"arg0Desc",    JaiI18N.getString("HistogramDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("HistogramDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("HistogramDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("HistogramDescriptor4")},
        {"arg4Desc",    JaiI18N.getString("HistogramDescriptor5")},
        {"arg5Desc",    JaiI18N.getString("HistogramDescriptor6")}
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "roi",
        "xPeriod",
        "yPeriod",
        "numBins",
        "lowValue",
        "highValue"
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.mediax.jai.ROI.class,
        java.lang.Integer.class,
        java.lang.Integer.class,
        int[].class,
        double[].class,
        double[].class
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        null,
        new Integer(1),
        new Integer(1),
        new int[] {256},
        new double[] {0.0},
        new double[] {256.0}
    };

    /** Constructor. */
    public HistogramDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.
     */
    public Number getParamMinValue(int index) {
        switch (index) {
        case 1:
        case 2:
            return new Integer(1);
        case 0:
        case 3:
        case 4:
        case 5:
            return null;
        default:
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Returns <code>true</code> if this operation is capable of handling
     * the input parameters.
     *
     * <p> In addition to the default validations done in the super class,
     * this method verifies that each element of <code>numBins</code> is
     * greater than 0, and each <code>lowValue</code> is less than its
     * corresponding <code>highValue</code>.
     *
     * @throws IllegalArgumentException  If <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException  If <code>msg</code> is <code>null</code>
     *         and the validation fails.
     */
    protected boolean validateParameters(ParameterBlock args,
                                         StringBuffer msg) {

        if ( args == null || msg == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (!super.validateParameters(args, msg)) {
            return false;
        }

        int[] numBins = (int[])args.getObjectParameter(3);
        double[] lowValue = (double[])args.getObjectParameter(4);
        double[] highValue = (double[])args.getObjectParameter(5);

        int l1 = numBins.length;
        int l2 = lowValue.length;
        int l3 = highValue.length;

        int length = Math.max(l1, Math.max(l2, l3));

        for (int i = 0; i < length; i++) {
            if (i < l1 && numBins[i] <= 0) {
                msg.append(getName() + " " +
                       JaiI18N.getString("HistogramDescriptor7"));
                return false;
            }

            double l = i < l2 ? lowValue[i] : lowValue[0];
            double h = i < l3 ? highValue[i] : highValue[0];

            if (l >= h) {
                msg.append(getName() + " " +
                       JaiI18N.getString("HistogramDescriptor8"));
                return false;
            }
        }

        return true;
    }


    /**
     * Generates a histogram based on the pixel values within a specific region of an image.
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
     * @param roi The region of the image to be scanned.
     * May be <code>null</code>.
     * @param xPeriod The horizontal sampling rate; may not be less than 1.
     * May be <code>null</code>.
     * @param yPeriod The vertical sampling rate; may not be less than 1.
     * May be <code>null</code>.
     * @param numBins The number of bins for each band.
     * May be <code>null</code>.
     * @param lowValue The lowest inclusive pixel value to be checked for each band.
     * May be <code>null</code>.
     * @param highValue The highest exclusive pixel value to be checked for each band.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    ROI roi,
                                    Integer xPeriod,
                                    Integer yPeriod,
                                    int[] numBins,
                                    double[] lowValue,
                                    double[] highValue,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Histogram",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("roi", roi);
        pb.setParameter("xPeriod", xPeriod);
        pb.setParameter("yPeriod", yPeriod);
        pb.setParameter("numBins", numBins);
        pb.setParameter("lowValue", lowValue);
        pb.setParameter("highValue", highValue);

        return JAI.create("Histogram", pb, hints);
    }
}
