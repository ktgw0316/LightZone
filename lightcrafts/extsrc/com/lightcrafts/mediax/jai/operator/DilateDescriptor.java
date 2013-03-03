/*
 * $RCSfile: DilateDescriptor.java,v $
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
import com.lightcrafts.media.jai.util.AreaOpPropertyGenerator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.PropertyGenerator;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Dilate" operation.
 *
 * Dilation for gray scale images can be charaterized by "slide, add and max",
 * while for binary images by "slide and set". As always, the kernel
 * is expected to come with a key position.
 * 
 * <p> Dilation, unlike convolution and most neighborhood operations,
 * actually can grow the image region. But to conform with other
 * image neighborhood operations, the border pixels are set to 0.
 * For a 3 x 3 kernel with the key point at the center, there will
 * be a pixel wide 0 stripe around the border.
 *
 * <p>When applied to multi-band images the dilation operator processes
 * each band independently using the methodology which would be applied
 * to single band images of the same data type.
 *
 * <p> <b> Gray scale dilation</b> is a spatial operation that computes
 * each output sample by adding elements of a kernel to the samples
 * surrounding a particular source sample and taking the maximum.
 * A mathematical expression is:
 *
 * <p> For a kernel K with a key position (xKey,yKey), the dilation
 * of image I at (x,y) is given by:
 * <pre>
 *     max{ I(x-i, y-j) + K(xKey+i, yKey+j): some (i,j) restriction }
 *  
 *      where the (i,j) restriction means:
 *      all possible (i,j) so that both I(x-i,y-j) and K(xKey+i, yKey+j)
 *      are defined, that is, these indices are in bounds.
 *
 * </pre> 
 * <p>Intuitively in 2D, the kernel is like
 * an umbrella and the key point is the handle. When the handle moves
 * all over the image surface, the upper outbounds of all the umbrella
 * positions is the dilation. Thus if you want the image to dilate in
 * the upper right direction, the following kernel would do with
 * the bold face key position.
 *
 * <p><center>
 * <table border=1>
 * <tr align=center><td>0</td><td>0</td><td>50</td> </tr>
 * <tr align=center><td>0</td><td>50</td><td>0</td> </tr>
 * <tr align=center><td><b>0</b></td><td>0</td><td>0</td> </tr>
 * </table></center>
 *
 * <p> Note also that zero kernel have effects on the dilation!
 * That is because of the "max" in the add and max process. Thus
 * a 3 x 1 zero kernel with the key position at the bottom of the kernel
 * dilates the image upwards.
 * 
 * <p> 
 * After the kernel is rotated 180 degrees, Pseudo code for dilation operation
 * is as follows. Of course, you should provide the kernel in its
 * (unrotated) original form. Assuming the kernel K is of size M rows x N cols
 * and the key position is (xKey, yKey).
 * 
 * <pre>
 * // gray-scale dilation:
 * for every dst pixel location (x,y){
 *    dst[x][y] = -infinity;
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *          if((x+i, y+j) are in bounds of src &&
 *	      (xKey+i, yKey+j) are in bounds of K){
 *             tmp = src[x + i][y + j]+ K[xKey + i][yKey + j];
 *	       dst[x][y] = max{tmp, dst[x][y]};
 *          }
 *       }
 *    }
 * }
 * </pre>
 *
 * <p> The kernel cannot be bigger in any dimension than the image data.
 *
 * <p> <b>Binary Image Dilation</b>
 * requires the kernel K to be binary, that is, have values 0 or 1
 * for all kernel entries.
 * Intuitively, starting from dst image being a duplicate of src,
 * binary dilation slides the kernel K to place the key position
 * at every non-zero point (x,y) in src image and set dst positions
 * under ones of K to 1.
 *  
 * <p> After the kernel is rotated 180 degrees, the pseudo code for
 * dilation operation is as follows. (Of course, you should provide
 * the kernel in its original unrotated form.)
 * 
 * <pre>
 * 
 * // binary dilation
 * for every dst pixel location (x,y){
 *    dst[x][y] = src[x][y];
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *         if(src[x+i,y+i]==1 && Key(xKey+i, yKey+j)==1){
 *            dst[x][y] = 1; break;
 *          }
 *       }
 *    }
 * }
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
 * <tr><td>GlobalName</td>  <td>Dilate</td></tr>
 * <tr><td>LocalName</td>   <td>Dilate</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Performs kernel based Dilate on
 *                              an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forD
evelopers/jai-apidocs/javax/media/jai/operator/DilateDescriptor.html</td
></tr>
 * <tr><td>Version</td>     <td>1.1</td></tr>
 * <tr><td>arg0Desc</td>    <td>The dilate kernel.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>kernel</td>        <td>com.lightcrafts.mediax.jai.KernelJAI</td>
 *                            <td>NO_PARAMETER_DEFAULT</td>
 * </table></p>
 *
 * </pre>
 * <p> Reference: An Introduction to Nonlinear Image Processing,
 * by Edward R. Bougherty and Jaakko Astola,
 * Spie Optical Engineering Press, 1994.
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 * @see com.lightcrafts.mediax.jai.KernelJAI
 * @see com.lightcrafts.mediax.jai.operator.ErodeDescriptor
 *
 * @since JAI 1.1
 */
public class DilateDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for a Dilate operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Dilate"},
        {"LocalName",   "Dilate"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("DilateDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jaiapi/<br>com.lightcrafts.mediax.jai.operator.DilateDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("DilateDescriptor1")}
    };

    /** The parameter names for the Dilate operation. */
    private static final String[] paramNames = {
        "kernel"
    };

    /** The parameter class types for the Dilate operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.mediax.jai.KernelJAI.class
    };

    /** The parameter default values for the Dilate operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT
    };

    /** Constructor. */
    public DilateDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
      * Returns an array of <code>PropertyGenerators</code> implementing
      * property inheritance for the "Dilate" operation.
      *
      * @return  An array of property generators.
      */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }


    /**
     * Performs binary kernel based Dilate operation on the image.
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
            new ParameterBlockJAI("Dilate",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("kernel", kernel);

        return JAI.create("Dilate", pb, hints);
    }
}
