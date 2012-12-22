/*
 * $RCSfile: XorConstDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:46 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "XorConst" operation.
 *
 * <p> The XorConst operation takes one rendered or renderable image
 * and an array of integer constants, and performs a bit-wise logical
 * "xor" between every pixel in the same band of the source and the
 * constant from the corresponding array entry. If the number of
 * constants supplied is less than the number of bands of the
 * destination, then the constant from entry 0 is applied to all the
 * bands. Otherwise, a constant from a different entry is applied to
 * each band.
 *
 * <p> The source image must have an integral data type. By default,
 * the destination image bound, data type, and number of bands are the
 * same as the source image.
 *
 * <p>The following matrix defines the "xor" operation.
 * <p><table border=1>
 * <caption>Logical "xor"</caption>
 * <tr align=center><th>src</th> <th>const</th> <th>Result</th></tr>
 * <tr align=center><td>0</td>   <td>0</td>     <td>0</td></tr>
 * <tr align=center><td>0</td>   <td>1</td>     <td>1</td></tr>
 * <tr align=center><td>1</td>   <td>0</td>     <td>1</td></tr>
 * <tr align=center><td>1</td>   <td>1</td>     <td>0</td></tr>
 * </table></p>
 *
 * <p> The destination pixel values are defined by the pseudocode:
 * <pre>
 * if (constants.length < dstNumBands) {
 *     dst[x][y][b] = src[x][y][b] ^ constants[0];
 * } else {
 *     dst[x][y][b] = src[x][y][b] ^ constants[b];
 * }
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>XorConst</td></tr>
 * <tr><td>LocalName</td>   <td>XorConst</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Logically "xors" an image
 *                              with constants.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/XorConstDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The constants to logically "xor" with.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>      <th>Class Type</th>
 *                        <th>Default Value</th></tr>
 * <tr><td>constants</td> <td>int[]</td>
 *                        <td>{0}</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class XorConstDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "XorConst"},
        {"LocalName",   "XorConst"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("XorConstDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/XorConstDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("XorConstDescriptor1")}
    };

    /**
     * The parameter class list for this operation.
     * The number of constants provided should be either 1, in which case
     * this same constant is applied to all the source bands; or the same
     * number as the source bands, in which case one contant is applied
     * to each band.
     */
    private static final Class[] paramClasses = {
        int[].class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "constants"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        new int[] {0}
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public XorConstDescriptor() {
        super(resources, supportedModes, 1,
		paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input source and parameter.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the source image has
     * an integral data type and that "constants" has length at least 1.
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

        int dtype = src.getSampleModel().getDataType();

        if (dtype != DataBuffer.TYPE_BYTE &&
            dtype != DataBuffer.TYPE_USHORT &&
            dtype != DataBuffer.TYPE_SHORT &&
            dtype != DataBuffer.TYPE_INT) {
            message.append(getName() + " " +
                           JaiI18N.getString("XorConstDescriptor2"));
            return false;
        }

        int length = ((int[])args.getObjectParameter(0)).length;
        if (length < 1) {
            message.append(getName() + " " +
                           JaiI18N.getString("XorConstDescriptor3"));
            return false;
        }

        return true;
    }


    /**
     * Logically "xors" an image with constants.
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
     * @param constants The constants to logically "xor" with.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    int[] constants,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("XorConst",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("constants", constants);

        return JAI.create("XorConst", pb, hints);
    }

    /**
     * Logically "xors" an image with constants.
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
     * @param constants The constants to logically "xor" with.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                int[] constants,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("XorConst",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("constants", constants);

        return JAI.createRenderable("XorConst", pb, hints);
    }
}
