/*
 * $RCSfile: BoxFilterDescriptor.java,v $
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
import com.lightcrafts.media.jai.util.AreaOpPropertyGenerator;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.PropertyGenerator;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "BoxFilter" operation.
 *
 * <p> The "BoxFilter" operation determines the intensity of a pixel
 * in an image by averaging the source pixels within a rectangular
 * area around the pixel. This is a special case of the convolution
 * operation, in which each source pixel contributes the same weight
 * to the destination pixel. The pixel values of the destination image
 * are defined by the pseudocode:
 *
 * <pre>
 *     int count = width * height; // # of pixels in the box
 *     for (int b = 0; b < numBands; b++) {
 *         int total = 0;
 *         for (int j = -yKey; j < -yKey + height; j++) {
 *             for (int i = -xKey; i < -xKey + width; i++) {
 *                 total += src[x+i][y+j][b];
 *             }
 *         }
 *         dst[x][y][b] = (total + count/2) / count; // round
 *     }
 * </pre>
 *
 * <p> Convolution, like any neighborhood operation, leaves a band of
 * pixels around the edges undefined.  For example, for a 3x3 kernel
 * only four kernel elements and four source pixels contribute to the
 * convolution pixel at the corners of the source image.  Pixels that
 * do not allow the full kernel to be applied to the source are not
 * included in the destination image.  A "Border" operation may be used
 * to add an appropriate border to the source image in order to avoid
 * shrinkage of the image boundaries.
 *
 * <p> The kernel may not be bigger in any dimension than the image data.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>BoxFilter</td></tr>
 * <tr><td>LocalName</td>   <td>BoxFilter</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Performs special case convolution where each
 *                              source pixel contributes equally to the
 *                              intensity of the destination pixel.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BoxFilterDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The width of the box.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The height of the box.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The X position of the key element.</td></tr>
 * <tr><td>arg3Desc</td>    <td>The Y position of the key element.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>   <th>Class Type</th>
 *                     <th>Default Value</th></tr>
 * <tr><td>width</td>  <td>java.lang.Integer</td>
 *                     <td>3</td>
 * <tr><td>height</td> <td>java.lang.Integer</td>
 *                     <td>width</td>
 * <tr><td>xKey</td>   <td>java.lang.Integer</td>
 *                     <td>width/2</td>
 * <tr><td>yKey</td>   <td>java.lang.Integer</td>
 *                     <td>height/2</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class BoxFilterDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "BoxFilter"},
        {"LocalName",   "BoxFilter"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("BoxFilterDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BoxFilterDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("BoxFilterDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("BoxFilterDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("BoxFilterDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("BoxFilterDescriptor4")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        java.lang.Integer.class, java.lang.Integer.class,
        java.lang.Integer.class, java.lang.Integer.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "width", "height", "xKey", "yKey"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        new Integer(3), null, null, null
    };

    /** Constructor. */
    public BoxFilterDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.
     */
    public Number getParamMinValue(int index) {
        if (index == 0 || index == 1) {
            return new Integer(1);
        } else if (index == 2 || index == 3) {
            return new Integer(Integer.MIN_VALUE);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    protected boolean validateParameters(ParameterBlock args,
                                         StringBuffer msg) {
        // The number of parameters supplied.
        int argNumParams = args.getNumParameters();

        if(argNumParams > 0 &&
           args.getObjectParameter(0) instanceof Integer) {
            Object obj;
            if(argNumParams < 2) {
                obj = args.getObjectParameter(0);
                if(obj instanceof Integer) {
                    // set height to width
                    args.add(obj);
                }
            }

            if(argNumParams < 3) {
                obj = args.getObjectParameter(0);
                if(obj instanceof Integer) {
                    // set xKey to width/2
                    args.add(((Integer)obj).intValue()/2);
                }
            }

            if(argNumParams < 4) {
                obj = args.getObjectParameter(1);
                if(obj instanceof Integer) {
                    // set yKey to height/2
                    args.add(((Integer)obj).intValue()/2);
                }
            }
        }

        return super.validateParameters(args, msg);
    }

    /**
      * Returns an array of <code>PropertyGenerators</code> implementing
      * property inheritance for the "BoxFilter" operation.
      *
      * @return  An array of property generators.
      */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AreaOpPropertyGenerator();
        return pg;
    }


    /**
     * Performs special case convolution where each source pixel contributes equally to the intensity of the destination pixel.
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
     * @param width The width of the box.
     * May be <code>null</code>.
     * @param height The height of the box.
     * May be <code>null</code>.
     * @param xKey The X position of the key element.
     * May be <code>null</code>.
     * @param yKey The Y position of the key element.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Integer width,
                                    Integer height,
                                    Integer xKey,
                                    Integer yKey,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("BoxFilter",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("width", width);
        pb.setParameter("height", height);
        pb.setParameter("xKey", xKey);
        pb.setParameter("yKey", yKey);

        return JAI.create("BoxFilter", pb, hints);
    }
}
