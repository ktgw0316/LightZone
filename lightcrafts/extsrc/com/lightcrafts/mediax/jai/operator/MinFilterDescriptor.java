/*
 * $RCSfile: MinFilterDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:40 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import com.lightcrafts.media.jai.util.AreaOpPropertyGenerator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.PropertyGenerator;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.operator.MinFilterShape;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "MinFilter" operation.
 *
 * <p> The "MinFilter" operation is a non-linear filter which is
 * useful for removing isolated lines or pixels while preserving the
 * overall appearance of an image. The filter is implemented by moving
 * a mask over the image. For each position of the mask, the
 * center pixel is replaced by the min of the pixel values covered
 * by the mask.
 *
 * <p> There are several shapes possible for the mask.  The
 * MinFilter operation supports three shapes, as follows:
 *
 * <p> Square Mask:
 * <pre>
 *                       x x x
 *                       x x x
 *                       x x x
 * </pre>
 *
 * <p> Plus Mask:
 * <pre>
 *                         x
 *                       x x x
 *                         x
 * </pre>
 *
 * <p> X Mask:
 * <pre>
 *                       x   x
 *                         x
 *                       x   x
 * </pre>
 *
 * <p>Example:
 * <pre>
 * 	SeekableStream s = new FileSeekableStream(new File(imagefilename); 
 *	ParameterBlock pb = new ParameterBlock();
 *      pb.add(s);
 *      RenderedImage src = (RenderedImage)JAI.create("stream", pb);
 *
 *      pb = new ParameterBlock();
 *      pb.addSource(src);
 *      pb.add(MaxFilterDescriptor.MIN_MASK_PLUS);    // mask Type
 *      pb.add(new Integer(5));                       // mask size
 *      
 *      RenderedImage dst = (RenderedImage)JAI.create("minfilter", pb);
 * </pre>
 * <p> A RenderingHints can also be added to the above.
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
 * <p><table align=center border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>MinFilter</td></tr>
 * <tr><td>LocallName</td>  <td>MinFilter</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Performs min filtering on an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jaiapi/com.lightcrafts.mediax.jai.operator.MinFilterDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The shape of the mask to be used for Min Filtering.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The size (width/height) of the mask to be used in Min Filtering.</td></tr>
 * </table></p>
 *
 * <p><table align=center border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>   <th>Class Type</th>
 *                     <th>Default Value</th></tr>
 * <tr><td>maskShape</td> <td>com.lightcrafts.mediax.jai.operator.MinFilterShape</td>
 *                     <td>MIN_MASK_SQUARE</td>
 * <tr><td>maskSize</td> <td>java.lang.Integer</td>
 *                     <td>3</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 * @see MinFilterShape
 *
 * @since JAI 1.1
 */
public class MinFilterDescriptor extends OperationDescriptorImpl {

    /**
     * Default 3x3 Windows
     */

    /** Square shaped mask. */
    public static final MinFilterShape MIN_MASK_SQUARE =
        new MinFilterShape("MIN_MASK_SQUARE", 1);

    /** Plus shaped mask. */
    public static final MinFilterShape MIN_MASK_PLUS =
        new MinFilterShape("MIN_MASK_PLUS", 2);

    /** X shaped mask. */
    public static final MinFilterShape MIN_MASK_X =
        new MinFilterShape("MIN_MASK_X", 3);

    /** Separable square mask. */
    public static final MinFilterShape MIN_MASK_SQUARE_SEPARABLE =
        new MinFilterShape("MIN_MASK_SQUARE_SEPARABLE", 4);

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "MinFilter"},
        {"LocalName",   "MinFilter"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("MinFilterDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jaiapi/com.lightcrafts.mediax.jai.operator.MinFilterDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion2")},
        {"arg0Desc",    JaiI18N.getString("MinFilterDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("MinFilterDescriptor2")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        MinFilterShape.class, java.lang.Integer.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "maskShape","maskSize"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        MIN_MASK_SQUARE, new Integer(3)
    };

    /** Constructor for the MinFilterDescriptor. */
    public MinFilterDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.
     */
    public Number getParamMinValue(int index) {
        if (index == 0) {
            return null;
        } else if (index == 1){
            return new Integer(1);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Returns the maximum legal value of a specified numeric parameter
     * for this operation.
     */
    public Number getParamMaxValue(int index) {
        if (index == 0) {
            return null;
        } else if (index == 1){
            return new Integer(Integer.MAX_VALUE);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
      * Returns an array of <code>PropertyGenerators</code> implementing
      * property inheritance for the "MinFilter" operation.
      *
      * @return  An array of property generators.
      */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }


    /**
     * Performs min filtering on an image.
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
     * @param maskShape The shape of the mask to be used for Min Filtering.
     * May be <code>null</code>.
     * @param maskSize The size (width/height) of the mask to be used in Min Filtering.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    MinFilterShape maskShape,
                                    Integer maskSize,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("MinFilter",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("maskShape", maskShape);
        pb.setParameter("maskSize", maskSize);

        return JAI.create("MinFilter", pb, hints);
    }
}
