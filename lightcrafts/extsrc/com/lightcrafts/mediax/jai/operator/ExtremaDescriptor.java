/*
 * $RCSfile: ExtremaDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:35 $
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
 * An <code>OperationDescriptor</code> describing the "Extrema" operation.
 *
 * <p> The Extrema operation scans a specific region of a rendered
 * image and finds the maximum and minimum pixel values for each band
 * within that region of the image. The image data pass through this
 * operation unchanged.
 *
 * <p> The region-wise maximum and minimum pixel values may be obtained
 * as properties. Calling the <code>getProperty</code> method on this
 * operation with "extrema" as the property name retrieves both the
 * region-wise maximum and minimum pixel values. Calling it with
 * "maximum" as the property name retrieves the region-wise maximum
 * pixel value, and with "minimum" as the property name retrieves the
 * region-wise minimum pixel value. The return value for "extrema" has
 * type <code>double[2][#bands]</code>, and those for "maximum"
 * and "minimum" have type <code>double[#bands]</code>.
 *
 * <p> The region of interest (ROI) does not have to be a rectangle.
 * It may be <code>null</code>, in which case the entire image is
 * scanned to find the image-wise maximum and minimum pixel values
 * for each band.
 *
 * <p> The set of pixels scanned may be further reduced by
 * specifying the "xPeriod" and "yPeriod" parameters that represent
 * the sampling rate along each axis. These variables may not be
 * less than 1. However, they may be <code>null</code>, in which
 * case the sampling rate is set to 1; that is, every pixel in the
 * ROI is processed.
 *
 * <p> The <code>Boolean</code> parameter "saveLocations" indicates whether
 * the locations of the extrema will be computed.  If <code>TRUE</code>, the
 * locations are computed and stored in the properties "minLocations" and
 * "maxLocations" in the form of lists of run length codes.  Each run length
 * code is stored as a three-entry integer array <code>(xStart, yStart,
 * length)</code>.  Because the statistics are implemented on the
 * low-resolution image, this <code>length</code> is defined on the image
 * coordinate system of the low-resolution image.  Thus, the run length code
 * above means the pixels <code>(xStart, yStart), (xStart + xPeriod, yStart),
 * ..., (xStart + (length - 1) * xPeriod, yStart) </code> of the original
 * image have a value of the maximum or minimum, depending on whether this
 * run exists in the property "maxLocations" or "minLocations".  The run length
 * code is row-based, thus the run doesn't wrap on the image boundaries.
 * Runs are not guaranteed to be maximal, e.g. a run that crosses tile
 * boundaries might be broken at the boundary into multiple runs.  The order
 * of the runs is not guaranteed.
 *
 * <p> The value objects of the properties "minLocations" and "maxLocations"
 * are arrays of <code>java.util.List</code>.  Each array entry contains
 * the minimum/maximum locations of one band. The elements in
 * a list can be accessed using the iterator of
 * the <code>java.util.List</code>.  For example, the sample code below
 * demonstrates how to retrieve the minimum locations of the first band:
 * <pre>
 *	List minLocations = ((List[])extremaOp.getProperty("minLocations"))[0];
 *	Iterator iter = minLocations.iterator();
 *	while(iter.hasNext()) {
 *	    int[] runLength = (int[])iter.next();
 *	    int xStart = runLength[0];
 *          int yStart = runLength[1];
 *          int length = runLength[2];
 *      }
 * </pre>
 *
 * <p> In the implementation of this operator, the proper use of the parameter
 *  "saveLocations" also helps to keep the efficiency of the operator in the
 *  common case: when only the extremal values are computed.
 *
 * <p> The parameter "maxRuns" is the maximum number of run length codes that
 * the user would like to store.  It is defined to reduce memory
 * consumption on very large images.  If the parameter "saveLocations" is
 * <code>FALSE</code>, this parameter will be ignored.  If this parameter is
 * equal to <code> Integer.MAX_VALUE</code>, all the locations will be saved.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Extrema</td></tr>
 * <tr><td>LocalName</td>   <td>Extrema</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Finds the maximum and minimum pixel value
 *                              in each band of an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ExtremaDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The region of the image to scan.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The horizontal sampling rate,
 *                              may not be less than 1.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The vertical sampling rate,
 *                              may not be less than 1.</td></tr>
 * <tr><td>arg3Desc</td>    <td>Whether to store extrema locations.</td></tr>
 * <tr><td>arg4Desc</td>    <td>Maximum number of run length codes to store.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>    <th>Class Type</th>
 *                      <th>Default Value</th></tr>
 * <tr><td>roi</td>     <td>com.lightcrafts.mediax.jai.ROI</td>
 *                      <td>null</td>
 * <tr><td>xPeriod</td> <td>java.lang.Integer</td>
 *                      <td>1</td>
 * <tr><td>yPeriod</td> <td>java.lang.Integer</td>
 *                      <td>1</td>
 * <tr><td>saveLocations</td> <td>java.lang.Boolean</td>
 *                      <td>Boolean.FALSE</td>
 * <tr><td>maxRuns</td> <td>java.lang.Integer</td>
 *                      <td>1</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class ExtremaDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Extrema"},
        {"LocalName",   "Extrema"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("ExtremaDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ExtremaDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("ExtremaDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("ExtremaDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("ExtremaDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("ExtremaDescriptor4")},
        {"arg4Desc",    JaiI18N.getString("ExtremaDescriptor5")}
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "roi", "xPeriod", "yPeriod", "saveLocations", "maxRuns"
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.mediax.jai.ROI.class,
        java.lang.Integer.class,
        java.lang.Integer.class,
        java.lang.Boolean.class,
        java.lang.Integer.class
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        null, new Integer(1), new Integer(1),
	Boolean.FALSE, new Integer(1)
    };

    /** Constructor. */
    public ExtremaDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.
     */
    public Number getParamMinValue(int index) {
        if (index == 0 || index == 3) {
            return null;
        } else if (index == 1 || index == 2 || index == 4) {
            return new Integer(1);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }


    /**
     * Finds the maximum and minimum pixel value in each band of an image.
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
     * @param roi The region of the image to scan.
     * May be <code>null</code>.
     * @param xPeriod The horizontal sampling rate, may not be less than 1.
     * May be <code>null</code>.
     * @param yPeriod The vertical sampling rate, may not be less than 1.
     * May be <code>null</code>.
     * @param saveLocations Whether to store extrema locations.
     * May be <code>null</code>.
     * @param maxRuns Maximum number of run length codes to store.
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
                                    Boolean saveLocations,
                                    Integer maxRuns,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Extrema",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("roi", roi);
        pb.setParameter("xPeriod", xPeriod);
        pb.setParameter("yPeriod", yPeriod);
        pb.setParameter("saveLocations", saveLocations);
        pb.setParameter("maxRuns", maxRuns);

        return JAI.create("Extrema", pb, hints);
    }
}
