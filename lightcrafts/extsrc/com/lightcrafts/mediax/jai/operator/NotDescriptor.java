/*
 * $RCSfile: NotDescriptor.java,v $
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
 * An <code>OperationDescriptor</code> describing the "Not" operation.
 *
 * <p> The Not operation takes one rendered or renderable image, and
 * performs bit-wise logical "not" on every pixel from every band of
 * the source image.  No additional parameters are required.
 *
 * <p> The source image must have an integral data type.  By default,
 * the destination image bound, data type, and number of bands are the
 * same as the source image.
 *
 * <p>The following matrix defines the logical "not" operation.
 * <p><table border=1>
 * <caption>Logical "not"</caption>
 * <tr align=center><th>src</th> <th>Result</th></tr>
 * <tr align=center><td>1</td>   <td>0</td></tr>
 * <tr align=center><td>0</td>   <td>1</td></tr>
 * </table></p>
 *
 * <p> The destination pixel values are defined by the pseudocode:
 * <pre>
 * dst[x][y][b] = ~src[x][y][b];
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Not</td></tr>
 * <tr><td>LocalName</td>   <td>Not</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Logically "nots" an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/NotDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * </table></p>
 *
 * <p> No parameters are needed for this operation.
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class NotDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Not"},
        {"LocalName",   "Not"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("NotDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/NotDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")}
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public NotDescriptor() {
        super(resources, supportedModes, 1, null, null, null, null);
    }

    /**
     * Validates the input source.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the source image
     * is of integral data type.
     */
    protected boolean validateSources(String modeName,
				      ParameterBlock args,
                                      StringBuffer msg) {
        if (!super.validateSources(modeName, args, msg)) {
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
            msg.append(getName() + " " + JaiI18N.getString("NotDescriptor1"));
            return false;
        }

        return true;
    }


    /**
     * Logically "nots" an image.
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
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Not",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        return JAI.create("Not", pb, hints);
    }

    /**
     * Logically "nots" an image.
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
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Not",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        return JAI.createRenderable("Not", pb, hints);
    }
}
