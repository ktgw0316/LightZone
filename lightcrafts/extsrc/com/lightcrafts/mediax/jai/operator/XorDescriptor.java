/*
 * $RCSfile: XorDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:47 $
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
 * An <code>OperationDescriptor</code> describing the "Xor" operation.
 *
 * <p> The Xor operation takes two rendered or renderable images, and
 * performs bit-wise logical "xor" on every pair of pixels, one from
 * each source image of the corresponding position and band. No
 * additional parameters are required.
 *
 * <p> Both source images must have integral data types. The two
 * data types may be different.
 *
 * <p> Unless altered by an <code>ImageLayout</code> hint, the
 * destination image bound is the intersection of the two source image
 * bounds.  If the two sources don't intersect, the destination will
 * have a width and height of 0.  The number of bands of the
 * destination image is equal to the lesser number of bands of the
 * sources, and the data type is the smallest data type with
 * sufficient range to cover the range of both source data types.
 *
 * <p>The following matrix defines the "xor" operation.
 * <p><table border=1>
 * <caption>Logical "xor"</caption>
 * <tr align=center><th>src1</th> <th>src2</th> <th>Result</th></tr>
 * <tr align=center><td>0</td>    <td>0</td>    <td>0</td></tr>
 * <tr align=center><td>0</td>    <td>1</td>    <td>1</td></tr>
 * <tr align=center><td>1</td>    <td>0</td>    <td>1</td></tr>
 * <tr align=center><td>1</td>    <td>1</td>    <td>0</td></tr>
 * </table></p>
 *
 * <p> The destination pixel values are defined by the pseudocode:
 * <pre>
 * dst[x][y][b] = srcs[0][x][y][b] ^ srcs[0][x][y][b];
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Xor</td></tr>
 * <tr><td>LocalName</td>   <td>Xor</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Logically "xors" two images.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/XorDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * </table></p>
 *
 * <p> No parameters are needed for this operation.
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class XorDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Xor"},
        {"LocalName",   "Xor"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("XorDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/XorDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")}
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public XorDescriptor() {
        super(resources, supportedModes, 2, null, null, null, null);
    }

    /**
     * Validates the input sources.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the source images
     * are of integral data type.
     */
    protected boolean validateSources(String modeName,
				      ParameterBlock args,
                                      StringBuffer msg) {
        if (!super.validateSources(modeName, args, msg)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

        for (int i = 0; i < 2; i++) {
	    RenderedImage src = args.getRenderedSource(0);

            int dtype = src.getSampleModel().getDataType();

            if (dtype != DataBuffer.TYPE_BYTE &&
                dtype != DataBuffer.TYPE_USHORT &&
                dtype != DataBuffer.TYPE_SHORT &&
                dtype != DataBuffer.TYPE_INT) {
                msg.append(getName() + " " +
                           JaiI18N.getString("XorDescriptor1"));
                return false;
            }
        }

        return true;
    }


    /**
     * Logically "xors" two images.
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
     * @param source1 <code>RenderedImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    RenderedImage source1,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Xor",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        return JAI.create("Xor", pb, hints);
    }

    /**
     * Logically "xors" two images.
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
     * @param source1 <code>RenderableImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                RenderableImage source1,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Xor",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        return JAI.createRenderable("Xor", pb, hints);
    }
}
