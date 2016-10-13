/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: AddDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:29 $
 * $State: Exp $
 */
package com.lightcrafts.jai.operator;

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
 * An <code>OperationDescriptor</code> describing the "Add" operation.
 *
 * <p> The Add operation takes two rendered or renderable source
 * images, and adds every pair of pixels, one from each source image
 * of the corresponding position and band. No additional parameters
 * are required.
 *
 * <p> The two source images may have different numbers of bands and
 * data types. By default, the destination image bounds are the
 * intersection of the two source image bounds.  If the sources don't
 * intersect, the destination will have a width and height of 0.
 *
 * <p> The default number of bands of the destination image is equal
 * to the smallest number of bands of the sources, and the data type
 * is the smallest data type with sufficient range to cover the range
 * of both source data types (not necessarily the range of their
 * sums).
 *
 * <p> As a special case, if one of the source images has N bands (N >
 * 1), the other source has 1 band, and an <code>ImageLayout</code>
 * hint is provided containing a destination <code>SampleModel</code>
 * with K bands (1 < K <= N), then the single band of the 1-banded
 * source is added to each of the first K bands of the N-band source.
 *
 * <p> If the result of the operation underflows/overflows the
 * minimum/maximum value supported by the destination data type, then
 * it will be clamped to the minimum/maximum value respectively.
 *
 * <p> The destination pixel values are defined by the pseudocode:
 * <pre>
 * dst[x][y][dstBand] = clamp(srcs[0][x][y][src0Band] +
 *                            srcs[1][x][y][src1Band]);
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Add</td></tr>
 * <tr><td>LocalName</td>   <td>Add</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Adds two images.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/AddDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * </table></p>
 *
 * <p> No parameters are needed for this operation.
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor */
public class UnSharpMaskDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "LCUnSharpMask"},
        {"LocalName",   "LCUnSharpMask"},
        {"Vendor",      "com.lightcrafts.jai"},
        {"Description", "UnSharp Mask Operation"},
        {"DocURL",      "none"},
        {"Version",     "1.0"}
    };

    /**
     * The parameter class list for this operation.
     * The number of constants provided should be either 1, in which case
     * this same constant is applied to all the source bands; or the same
     * number as the source bands, in which case one contant is applied
     * to each band.
     */
    private static final Class[] paramClasses = {
        Double.class, Integer.class
    };

    /**
     * The parameter name list for this operation.
     */
    private static final String[] paramNames = {
        "gain", "threshold"
    };

    /**
     * The parameter default value list for this operation.
     */
    private static final Object[] paramDefaults = {
         1.0, 0
    };

    /** Constructor. */
    public UnSharpMaskDescriptor() {
        super(resources, 2, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }


    /**
     * Adds two images.
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
     * @param source1 <code>RenderedImage</code> source 1.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    RenderedImage source1,
                                    double gain, int threshold,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("LCUnSharpMask",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);
        pb.setParameter("gain", gain);
        pb.setParameter("threshold", threshold);

        return JAI.create("LCUnSharpMask", pb, hints);
    }

    /**
     * Adds two images.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,java.awt.image.renderable.ParameterBlock,RenderingHints)}.
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
                                                double gain, int threshold,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("LCUnSharpMask",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);
        pb.setParameter("gain", gain);
        pb.setParameter("threshold", threshold);

        return JAI.createRenderable("LCUnSharpMask", pb, hints);
    }
}
