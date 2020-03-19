/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: ErodeDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:35 $
 * $State: Exp $
 */
package com.lightcrafts.jai.operator;

import com.sun.media.jai.util.AreaOpPropertyGenerator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PropertyGenerator;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 *
 * An <code>OperationDescriptor</code> describing the "Erode" operation.
 *
 * <p> <b>Gray Scale Erosion</b>
 * is a spatial operation that computes
 * each output sample by subtracting elements of a kernel from the samples
 * surrounding a particular source sample.
 * The mathematical formulation for erosion operation is:
 *
 * <p> For a kernel K with a key position (xKey, yKey), the erosion
 * of image I at (x,y) is given by:
 * <pre>
 *     max{ f:  f + K(xKey+i, yKey+j) <= I(x+i,y+j): all (i,j)}
 *
 *      "all" possible (i,j) means that both I(x+i,y+j) and K(xKey+i, yKey+j)
 *      are in bounds. Otherwise, the value is set to 0.
 *      "f" represents all possible floats satisfying the restriction.
 *
 * </pre>
 * <p> Intuitively, the kernel is like an unbrella and the key point
 * is the handle. At every point, you try to push the umbrella up as high
 * as possible but still underneath the image surface. The final height
 * of the handle is the value after erosion. Thus if you want the image
 * to erode from the upper right to bottom left, the following would do.
 *
 * <p><center>
 * <table border=1>
 * <tr align=center><td>0</td><td>0</td><td>X</td> </tr>
 * <tr align=center><td>0</td><td>X</td><td>0</td> </tr>
 * <tr align=center><td><b>X</b></td><td>0</td><td>0</td> </tr>
 * </table></center>
 *
 * <p> Note that even if every entry of a kernel is zero,
 * the erosion changes the image. Different key positions
 * will also lead to different erosion results for such zero kernels.
 *
 * <p> Pseudo code for the erosion operation is as follows.
 * Assuming the kernel K is of size M rows x N cols
 * and the key position is (xKey, yKey).
 *
 * <pre>
 *
 * // erosion
 * for every dst pixel location (x,y){
 *    tmp = infinity;
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *          if((x+i, y+j) are in bounds of src){
 *             tmp = min{tmp, src[x + i][y + j] - K[xKey + i][yKey + j]};
 *          }
 *       }
 *    }
 *    dst[x][y] = tmp;
 *    if (dst[x][y] == infinity)
 *        dst[x][y] = 0;
 * }
 * </pre>
 *
 * <p> The kernel cannot be bigger in any dimension than the image data.
 *
 * <p> <b>Binary Image Erosion</b>
 * requires the kernel to be binary, that is, to have values 0 and 1
 * for each kernel entry.
 * Intuitively, binary erosion slides the kernel
 * key position and place it at every point (x,y) in the src image.
 * The dst value at this position is set to 1 if the entire kernel lies
 * within the image bounds and the src image value is 1
 * wherever the corresponding kernel value is 1."
 * Otherwise, the value after erosion at (x,y) is set to 0.
 * Erosion usually shrinks images, but it can fill holes
 * with kernels like
 * <pre> [1 0 1] </pre>
 * and the key position at the center.
 *
 * <p> Pseudo code for the binary erosion operation is as follows.
 *
 * <pre>
 * // erosion
 * for every dst pixel location (x,y){
 *    dst[x][y] = 1;
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *         if((x+i,y+j) is out of bounds of src ||
 *             src(x+i, y+j)==0 && Key(xKey+i, yKey+j)==1){
 *            dst[x][y] = 0; break;
 *          }
 *       }
 *    }
 * }
 *
 * The following can be used as references for the underlying
 * connection between these two algorithms.
 *
 * <p> Reference: An Introduction to Nonlinear Image Processing,
 * by Edward R. Bougherty and Jaakko Astola,
 * Spie Optical Engineering Press, 1994.
 *
 * It should be noted that this operation automatically adds a
 * value of <code>Boolean.TRUE</code> for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> to the given
 * <code>configuration</code> so that the operation is performed
 * on the pixel values instead of being performed on the indices into
 * the color map if the source(s) have an <code>IndexColorModel</code>.
 * This addition will take place only if a value for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been
 * provided by the user. Note that the <code>configuration</code> Map
 * is cloned before the new hint is added to it. The operation can be
 * smart about the value of the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code>
 * <code>RenderingHints</code>, i.e. while the default value for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> is
 * <code>Boolean.TRUE</code>, in some cases the operator could set the
 * default.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Erode</td></tr>
 * <tr><td>LocalName</td>   <td>Erode</td></tr>
 * <tr><td>Vendor</td>      <td>com.sun.media.jai</td></tr>
 * <tr><td>Description</td> <td>Performs kernel based Erode on
 *                              an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forD
evelopers/jai-apidocs/javax/media/jai/operator/ErodeDescriptor.html</td
></tr>
 * <tr><td>Version</td>     <td>1.1</td></tr>
 * <tr><td>arg0Desc</td>    <td>The erode kernel.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>kernel</td>        <td>javax.media.jai.KernelJAI</td>
 *                            <td>NO_PARAMETER_DEFAULT</td>
 * </table></p>
 *
 * </pre>
 *
 *
 * @see javax.media.jai.KernelJAI
 *
 * @since JAI 1.1
 */

public class LCErodeDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for a Erode operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "LCErode"},
        {"LocalName",   "LCErode"},
        {"Vendor",      "com.lightcrafts.jai"},
        {"Description", "Binary Erode Algorithm for arbitrary images"},
        {"DocURL",      "none"},
        {"Version",     "1.0"},
        {"arg0Desc",    "The Image to Erode"}
    };

    /** The parameter names for the Erode operation. */
    private static final String[] paramNames = {
        "kernel"
    };

    /** The parameter class types for the Erode operation. */
    private static final Class[] paramClasses = {
        javax.media.jai.KernelJAI.class
    };

    /** The parameter default values for the Erode operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT
    };

    /** Constructor. */
    public LCErodeDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
      * Returns an array of <code>PropertyGenerators</code> implementing
      * property inheritance for the "Erode" operation.
      *
      * @return  An array of property generators.
      */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }


    /**
     * Performs binary kernel based Erode operation on the image.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,java.awt.image.renderable.ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param source0 <code>RenderedImage</code> source 0.
     * @param kernel The binary convolution kernel.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>kernel</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    KernelJAI kernel,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("LCErode",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("kernel", kernel);

        return JAI.create("LCErode", pb, hints);
    }
}
