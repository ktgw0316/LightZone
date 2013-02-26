/*
 * $RCSfile: MosaicDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:40 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.operator;

import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Mosaic" operation
 * in the rendered mode.
 *
 * <p>
 * The "Mosaic" operation creates a mosaic of two or more source images.
 * This operation could be used for example to assemble a set of
 * overlapping geospatially rectified images into a contiguous
 * image. It could also be used to create a montage of photographs such
 * as a panorama.
 * </p>
 *
 * <p>
 * All source images are assumed to have been geometrically mapped into
 * a common coordinate space. The origin <code>(minX,&nbsp;minY)</code> of
 * each image is therefore taken to represent the location of the respective
 * image in the common coordinate system of the source images. This
 * coordinate space will also be that of the destination image.
 * </p>
 *
 * <p>
 * All source images must have the same data type and sample size for all
 * bands. The destination will have the same data type, sample size, and
 * number of bands and color components as the sources.
 * </p>
 *
 * <p>
 * The destination layout may be specified by an {@link ImageLayout} hint
 * provided via a {@link RenderingHints} supplied to the operation. If this
 * hint contains a setting for the image bounds (origin and dimensions), it
 * will be used even if it does not intersect the union of the bounds of
 * the sources; otherwise the image bounds will be set to the union of all
 * source image bounds. If the data type or sample size specified by the layout
 * hint do not match those of the sources, then this portion of the hint will be
 * ignored.
 * </p>
 *
 * <p>
 * It is permissible that the number of source images be initially zero. In
 * this case a non-<code>null</code> <code>ImageLayout</code> must be
 * supplied with valid width and height and a non-<code>null</code>
 * <code>SampleModel</code>. The destination data type, sample size, number
 * of bands, and image bounds will all be determined by the
 * <code>ImageLayout</code>.
 * </p>
 *
 * <p>
 * If <code>sourceAlpha</code> is non-<code>null</code>, then any non-
 * <code>null</code> elements of the array must be single-band images
 * with the same data type and sample size as the sources.
 * </p>
 *
 * <p>
 * The source threshold array parameter has maximum dimensions as
 * <code>double[NUM_SOURCES][NUM_BANDS]</code>. Default values of the
 * thresholds actually used are defined as follows:
 * <ul>
 * <li>The default value of <code>sourceThreshold[0][0]</code> is
 * <code>1.0</code>.</li>
 * <li>If <code>sourceThreshold[i] != null</code> and
 * <code>sourceThreshold[i].length < NUM_BANDS</code>, then set
 * <code>sourceThreshold[i][j] = sourceThreshold[i][0]</code> for all
 * <code>1 <= j < NUM_BANDS</code>.</li>
 * <li>If <code>sourceThreshold[i] == null</code> then set
 * <code>sourceThreshold[i] = sourceThreshold[0]</code>.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The background value array parameter has maximum dimensions as
 * <code>double[NUM_BANDS]</code>. Default values of the
 * background actually used are defined as follows:
 * <ul>
 * <li>The default value of <code>backgroundValues[0]</code> is
 * <code>0.0</code>.</li>
 * <li>If <code>backgroundValues.length < NUM_BANDS</code>, then set
 * <code>backgroundValues[j] = backgroundValues[0]</code> for all
 * <code>1 <= j < NUM_BANDS</code>.</li>
 * </ul>
 * The default behavior therefore is to set the background to zero.
 * </p>
 *
 * <p>
 * If a given destination position <tt>(x,&nbsp;y)</tt> is within the bounds
 * of <tt>M</tt> source images, then the destination pixel value
 * <tt>D(x,&nbsp;y)</tt> is computed using an algorithm selected on the
 * basis of the <code>mosaicType</code> parameter value. If the destination
 * position is not within any source image, then the destination pixel value
 * is set to the specified background value.
 * </p>
 *
 * <p>
 * If the <code>mosaicType</code> parameter value is
 * <code>MOSAIC_TYPE_BLEND</code>, then the destination pixel value
 * is computed as:
 * <pre>
 * double[][][] s; // source pixel values
 * double[][][] w; // derived source weight values
 * double[][] d;   // destination pixel values
 *
 * double weightSum = 0.0;
 * for(int i = 0; i < M; i++) {
 *     weightSum += w[i][x][y];
 * }
 *
 * if(weightSum != 0.0) {
 *     double sourceSum = 0.0;
 *     for(int i = 0; i < M; i++) {
 *         sourceSum += s[i][x][y]*w[i][x][y];
 *     }
 *     d[x][y] = sourceSum / weightSum;
 * } else {
 *     d[x][y] = background;
 * }
 * </pre>
 * where the index <tt>i</tt> is over the sources which contain
 * <tt>(x,&nbsp;y)</tt>. The destination pixel value is therefore a
 * blend of the source pixel values at the same position.
 * </p>
 *
 * <p>
 * If the <code>mosaicType</code> parameter value is
 * <code>MOSAIC_TYPE_OVERLAY</code>, then the destination pixel value
 * is computed as:
 * <pre>
 * d[x][y] = background;
 * for(int i = 0; i < M; i++) {
 *     if(w[i][x][y] != 0.0) {
 *         d[x][y] = s[i][x][y];
 *         break;
 *     }
 * }
 * </pre>
 * The destination pixel value is therefore the value of the first source
 * pixel at the same position for which the derived weight value at the same
 * position is non-zero.
 * </p>
 *
 * <p>
 * The derived weight values for the <tt>i</tt>th source are determined from
 * the corresponding <code>sourceAlpha</code>, <code>sourceROI</code>, and
 * <code>sourceThreshold</code> parameters as follows where for
 * illustration purposes it is assumed that any alpha values range over
 * <tt>[0.0,&nbsp;1.0]</tt> with <tt>1.0</tt> being opaque:
 * <pre>
 * // Set flag indicating whether to interpret alpha values as bilevel.
 * boolean isAlphaBitmask =
 *     !(mosaicType.equals(MOSAIC_TYPE_BLEND) &&
 *       sourceAlpha != null &&
 *       !(sourceAlpha.length < NUM_SOURCES));
 * if(!isAlphaBitmask) {
 *     for(int i = 0; i < NUM_SOURCES; i++) {
 *         if(sourceAlpha[i] == null) {
 *             isAlphaBitmask = true;
 *             break;
 *         }
 *     }
 * }
 *
 * // Derive source weights from the supplied parameters.
 * w[i][x][y] = 0.0;
 * if(sourceAlpha != null && sourceAlpha[i] != null) {
 *     w[i][x][y] = sourceAlpha[i][x][y];
 *     if(isAlphaBitmask && w[i][x][y] > 0.0) {
 *         w[i][x][y] = 1.0;
 *     }
 * } else if(sourceROI != null && sourceROI[i] != null &&
 *           sourceROI[i].contains(x,y)) {
 *     w[i][x][y] = 1.0;
 * } else if(s[i][x][y] >= sourceThreshold[i]) { // s[i][x][y] = source value
 *     w[i][x][y] = 1.0;
 * }
 * </pre>
 * </p>
 *
 * <p>
 * As illustrated above, the interpretation of the alpha values will vary
 * depending on the values of the parameters supplied to the operation. If
 * and only if <code>mosaicType</code> equals <code>MOSAIC_TYPE_BLEND</code>
 * and an alpha mask is available for each source will the alpha values be
 * treated as arbitrary values as for {@link Transparency#TRANSLUCENT}. In
 * all other cases the alpha values will be treated as bilevel values
 * as for {@link Transparency#BITMASK}.
 * </p>
 *
 * <p>
 * It should be remarked that the <code>MOSAIC_TYPE_BLEND</code> algorithm
 * applied when the weights are treated as bilevel values is equivalent to
 * averaging all non-transparent source pixels at a given position. This
 * in effect intrinsically provides a third category of mosaicking. The
 * available categories are summarized in the following table.
 * <table border=1>
 * <caption><b>Mosaic Categories</b></caption>
 * <tr><th>Mosaic Type</th>
 *     <th>Transparency Type</th>
 *     <th>Category</th></tr>
 * <tr><td><code>MOSAIC_TYPE_BLEND</code></td>
 *     <td><code>BITMASK</code></td>
 *     <td>Average</td></tr>
 * <tr><td><code>MOSAIC_TYPE_BLEND</code></td>
 *     <td><code>TRANSLUCENT</code></td>
 *     <td>Alpha Blend</td></tr>
 * <tr><td><code>MOSAIC_TYPE_OVERLAY</code></td>
 *     <td><code>BITMASK || TRANSLUCENT</code></td>
 *     <td>Superposition</td></tr>
 * </table>
 * </p>
 *
 * <p><table border=1>
 * <caption><b>Resource List</b></caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Mosaic</td></tr>
 * <tr><td>LocalName</td>   <td>Mosaic</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Creates a mosaic of two or more rendered images.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/MosaicDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>Mosaicking type.</td></tr>
 * <tr><td>arg1Desc</td>    <td>Source alpha masks.</td></tr>
 * <tr><td>arg2Desc</td>    <td>Source region of interest masks.</td></tr>
 * <tr><td>arg3Desc</td>    <td>Source threshold values.</td></tr>
 * <tr><td>arg4Desc</td>    <td>Destination background value.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption><b>Parameter List</b></caption>
 * <tr><th>Name</th>             <th>Class Type</th>
 *                               <th>Default Value</th></tr>
 * <tr><td>mosaicType</td>       <td>com.lightcrafts.mediax.jai.operator.MosaicType</td>
 *                               <td>MOSAIC_TYPE_OVERLAY</td>
 * <tr><td>sourceAlpha</td>      <td>com.lightcrafts.mediax.jai.PlanarImage[]</td>
 *                               <td>null</td>
 * <tr><td>sourceROI</td>        <td>com.lightcrafts.mediax.jai.ROI[]</td>
 *                               <td>null</td>
 * <tr><td>sourceThreshold</td>  <td>double[][]</td>
 *                               <td>double[][] {{1.0}}</td>
 * <tr><td>backgroundValues</td> <td>double[]</td>
 *                               <td>double[] {0.0}</td>
 * </table></p>
 *
 * @since JAI 1.1.2
 */
public class MosaicDescriptor extends OperationDescriptorImpl {

    /**
     * Destination pixel equals alpha blend of source pixels.
     */
    public static final MosaicType MOSAIC_TYPE_BLEND =
        new MosaicType("MOSAIC_TYPE_BLEND", 1);

    /**
     * Destination pixel equals first opaque source pixel.
     */
    public static final MosaicType MOSAIC_TYPE_OVERLAY =
        new MosaicType("MOSAIC_TYPE_OVERLAY", 0);

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Mosaic"},
        {"LocalName",   "Mosaic"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("MosaicDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/MosaicDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("MosaicDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("MosaicDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("MosaicDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("MosaicDescriptor4")},
        {"arg4Desc",    JaiI18N.getString("MosaicDescriptor5")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.mediax.jai.operator.MosaicType.class,
        com.lightcrafts.mediax.jai.PlanarImage[].class,
        com.lightcrafts.mediax.jai.ROI[].class,
        double[][].class,
        double[].class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "mosaicType",
        "sourceAlpha",
        "sourceROI",
        "sourceThreshold",
        "backgroundValues"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        MOSAIC_TYPE_OVERLAY,
        null,
        null,
        new double[][] {{1.0}},
        new double[] {0.0}
    };

    /** Constructor. */
    public MosaicDescriptor() {
        super(resources,
              new String[] {RenderedRegistryMode.MODE_NAME},
              0,
              paramNames,
              paramClasses,
              paramDefaults,
              null);
    }

    /**
     * Creates a mosaic of two or more rendered images.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param sources <code>RenderedImage</code> sources.
     * @param mosaicType Mosaicking type.
     * May be <code>null</code>.
     * @param sourceAlpha 
     * May be <code>null</code>.
     * @param sourceAlpha Source alpha masks.
     * May be <code>null</code>.
     * @param sourceROI Source region of interest masks.
     * May be <code>null</code>.
     * @param sourceThreshold Source threshold values.
     * May be <code>null</code>.
     * @param backgroundValues Destination background value.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if any source is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage[] sources,
                                    MosaicType mosaicType,
                                    PlanarImage[] sourceAlpha,
                                    ROI[] sourceROI,
                                    double[][] sourceThreshold,
                                    double[] backgroundValues,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Mosaic",
                                  RenderedRegistryMode.MODE_NAME);

        int numSources = sources.length;
        for(int i = 0; i < numSources; i++) {
            pb.addSource(sources[i]);
        }

        pb.setParameter("mosaicType", mosaicType);
        pb.setParameter("sourceAlpha", sourceAlpha);
        pb.setParameter("sourceROI", sourceROI);
        pb.setParameter("sourceThreshold", sourceThreshold);
        pb.setParameter("backgroundValues", backgroundValues);

        return JAI.create("Mosaic", pb, hints);
    }
}
