/*
 * $RCSfile: OrderedDitherDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:41 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.ColorCube;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "OrderedDither"
 * operation.
 * 
 * <p> The "OrderedDither" operation performs color quantization by
 * finding the nearest color to each pixel in a supplied color cube
 * and "shifting" the resulting index value by a pseudo-random amount
 * determined by the values of a supplied dither mask.
 *
 * <p> The dither mask is supplied as an array of <code>KernelJAI</code>
 * objects the length of which must equal the number of bands in the
 * image. Each element of the array is a <code>KernelJAI</code> object
 * which represents the dither mask matrix for the corresponding band.
 * All <code>KernelJAI</code> objects in the array must have the same
 * dimensions and contain floating point values greater than or equal
 * to 0.0 and less than or equal to 1.0.
 *
 * <p> For all integral data types, the source image samples are presumed
 * to occupy the full range of the respective types. For floating point data
 * types it is assumed that the data samples have been scaled to the range
 * [0.0, 1.0].
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>OrderedDither</td></tr>
 * <tr><td>LocalName</td>   <td>OrderedDither</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Performs ordered dither color quantization
 *                              using a specified color cube and
 *                              dither mask.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/OrderedDitherDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The color cube.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The dither mask.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>colorMap</td>      <td>com.lightcrafts.mediax.jai.ColorCube</td>
 *                            <td>ColorCube.BYTE_496</td>
 * <tr><td>ditherMask</td>   <td>com.lightcrafts.mediax.jai.KernelJAI[]</td>
 *                            <td>KernelJAI.DITHER_MASK_443</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.KernelJAI
 * @see com.lightcrafts.mediax.jai.ColorCube
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class OrderedDitherDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "OrderedDither"},
        {"LocalName",   "OrderedDither"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("OrderedDitherDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/OrderedDitherDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("OrderedDitherDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("OrderedDitherDescriptor2")}
    };

    /** The parameter names for the "OrderedDither" operation. */
    private static final String[] paramNames = {
        "colorMap", "ditherMask"
    };

    /** The parameter class types for the "OrderedDither" operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.mediax.jai.ColorCube.class,
        com.lightcrafts.mediax.jai.KernelJAI[].class
    };

    /** The parameter default values for the "OrderedDither" operation. */
    private static final Object[] paramDefaults = {
        ColorCube.BYTE_496,
        KernelJAI.DITHER_MASK_443
    };

    private static final String[] supportedModes = {
	"rendered"
    };

    /**
     * Method to check the validity of the color map parameter. The supplied
     * color cube must have the same data type and number of bands as the
     * source image.
     *
     * @param sourceImage The source image of the operation.
     * @param colorMap The color cube.
     * @param msg The buffer to which messages should be appended.
     *
     * @return Whether the color map is valid.
     */
    private static boolean isValidColorMap(RenderedImage sourceImage,
                                           ColorCube colorMap,
                                           StringBuffer msg) {
        SampleModel srcSampleModel = sourceImage.getSampleModel();

        if(colorMap.getDataType() != srcSampleModel.getTransferType()) {
            msg.append(JaiI18N.getString("OrderedDitherDescriptor3"));
            return false;
        } else if (colorMap.getNumBands() != srcSampleModel.getNumBands()) {
            msg.append(JaiI18N.getString("OrderedDitherDescriptor4"));
            return false;
        }

        return true;
    }

    /**
     * Method to check the validity of the dither mask parameter. The dither
     * mask is an array of <code>KernelJAI</code> objects wherein the number
     * of elements in the array must equal the number of bands in the source
     * image. Furthermore all kernels in the array must have the same width
     * and height. Finally all data elements of all kernels must be greater
     * than or equal to zero and less than or equal to unity.
     *
     * @param sourceImage The source image of the operation.
     * @param ditherMask The dither mask.
     * @param msg The buffer to which messages should be appended.
     *
     * @return Whether the dither mask is valid.
     */
    private static boolean isValidDitherMask(RenderedImage sourceImage,
                                             KernelJAI[] ditherMask,
                                             StringBuffer msg) {
        if(ditherMask.length != sourceImage.getSampleModel().getNumBands()) {
            msg.append(JaiI18N.getString("OrderedDitherDescriptor5"));
            return false;
        }

        int maskWidth = ditherMask[0].getWidth();
        int maskHeight = ditherMask[0].getHeight();
        for(int band = 0; band < ditherMask.length; band++) {
            if(ditherMask[band].getWidth() != maskWidth ||
               ditherMask[band].getHeight() != maskHeight) {
                msg.append(JaiI18N.getString("OrderedDitherDescriptor6"));
                return false;
            }
            float[] kernelData = ditherMask[band].getKernelData();
            for(int i = 0; i < kernelData.length; i++) {
                if(kernelData[i] < 0.0F || kernelData[i] > 1.0) {
                    msg.append(JaiI18N.getString("OrderedDitherDescriptor7"));
                    return false;
                }
            }
        }

        return true;
    }

    /** Constructor. */
    public OrderedDitherDescriptor() {
        super(resources, supportedModes, 1,
		paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input source and parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "colorMap"
     * and "ditherMask" are valid for the given source image.
     */
    public boolean validateArguments(String modeName,
				     ParameterBlock args,
                                     StringBuffer msg) {
        if (!super.validateArguments(modeName, args, msg)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

        // Retrieve the operation source and parameters.
        RenderedImage src = args.getRenderedSource(0);
        ColorCube colorMap = (ColorCube)args.getObjectParameter(0);
        KernelJAI[] ditherMask = (KernelJAI[])args.getObjectParameter(1);

        // Check color map validity.
        if (!isValidColorMap(src, colorMap, msg)) {
            return false;
        }

        // Check dither mask validity.
        if (!isValidDitherMask(src, ditherMask, msg)) {
            return false;
        }

        return true;
    }


    /**
     * Performs ordered dither color quantization using a specified color cube and dither mask.
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
     * @param colorMap The color cube.
     * May be <code>null</code>.
     * @param ditherMask The dither mask.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    ColorCube colorMap,
                                    KernelJAI[] ditherMask,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("OrderedDither",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("colorMap", colorMap);
        pb.setParameter("ditherMask", ditherMask);

        return JAI.create("OrderedDither", pb, hints);
    }
}
