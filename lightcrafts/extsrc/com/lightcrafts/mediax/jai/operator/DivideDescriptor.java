/*
 * $RCSfile: DivideDescriptor.java,v $
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
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Divide" operation.
 *
 * <p> The Divide operation takes two rendered or renderable images,
 * and for every pair of pixels, one from each source image of the
 * corresponding position and band, divides the pixel from the first
 * source by the pixel from the second source. No additional parameters
 * are required for this operation.
 *
 * <p> In case of division by 0, if the numerator is 0, then the result
 * is set to 0; otherwise, the result is set to the maximum value
 * supported by the destination data type.
 *
 * <p> The two source images may have different number of bands and data
 * types. By default, the destination image bound is the intersection
 * of the two source image bounds. If the two sources don't intersect,
 * the destination will have a width and a height of 0.
 *
 * <p> The default number of bands of the destination image is the same
 * as the least number of bands of the sources, and the data type is the
 * biggest data type of the sources.
 *
 * <p> As a special case, if one of the source images has N bands (N >
 * 1), the other source has 1 band, and an <code>ImageLayout</code>
 * hint is provided containing a destination <code>SampleModel</code>
 * with K bands (1 < K <= N), then the single band of the 1-banded
 * source will be divided by or into to each of the first K bands of
 * the N-band source.
 *
 * <p> If the result of the operation underflows/overflows the
 * minimum/maximum value supported by the destination data type, then
 * it will be clamped to the minimum/maximum value respectively.
 *
 * <p> The destination pixel values are defined by the pseudocode:
 * <pre>
 * dst[x][y][dstBand] = srcs[0][x][y][src0Band]/srcs[1][x][y][src1Band];
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>divide</td></tr>
 * <tr><td>LocalName</td>   <td>divide</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Dividies one image by 
 *                              another image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/DivideDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * </table></p>
 *
 * <p> No parameters are needed for this operation.
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor */
public class DivideDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Divide"},
        {"LocalName",   "Divide"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("DivideDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/DivideDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")}
    };

    /** Constructor. */
    public DivideDescriptor() {
        super(resources, 2, null, null, null);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }


    /**
     * Divides one image by another image.
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
            new ParameterBlockJAI("Divide",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        return JAI.create("Divide", pb, hints);
    }

    /**
     * Divides one image by another image.
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
            new ParameterBlockJAI("Divide",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        return JAI.createRenderable("Divide", pb, hints);
    }
}
