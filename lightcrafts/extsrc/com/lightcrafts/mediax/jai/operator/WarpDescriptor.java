/*
 * $RCSfile: WarpDescriptor.java,v $
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
import com.lightcrafts.media.jai.util.PropertyGeneratorImpl;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.PropertyGenerator;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.ROIShape;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.Warp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * This property generator computes the properties for the operation
 * "Warp" dynamically.
 */
class WarpPropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public WarpPropertyGenerator() {
        super(new String[] {"ROI"},
              new Class[] {ROI.class},
              new Class[] {RenderedOp.class});
    }

    /**
     * Returns the specified property.
     *
     * @param name  Property name.
     * @param opNode Operation node.
     */
    public Object getProperty(String name,
                              Object opNode) {
        validate(name, opNode);

        if(opNode instanceof RenderedOp &&
           name.equalsIgnoreCase("roi")) {
            RenderedOp op = (RenderedOp)opNode;

            ParameterBlock pb = op.getParameterBlock();

            // Retrieve the rendered source image and its ROI.
            RenderedImage src = (RenderedImage)pb.getRenderedSource(0);
            Object property = src.getProperty("ROI");
            if (property == null ||
                property.equals(java.awt.Image.UndefinedProperty) ||
                !(property instanceof ROI)) {
                return java.awt.Image.UndefinedProperty;
            }

            // Return undefined also if source ROI is empty.
            ROI srcROI = (ROI)property;
            if (srcROI.getBounds().isEmpty()) {
                return java.awt.Image.UndefinedProperty;
            }

            // Retrieve the Interpolation object.
            Interpolation interp = (Interpolation)pb.getObjectParameter(1);

            // Determine the effective source bounds.
            Rectangle srcBounds = null;
            PlanarImage dst = op.getRendering();
            if (dst instanceof GeometricOpImage &&
                ((GeometricOpImage)dst).getBorderExtender() == null) {
                srcBounds =
                    new Rectangle(src.getMinX() + interp.getLeftPadding(),
                                  src.getMinY() + interp.getTopPadding(),
                                  src.getWidth() - interp.getWidth() + 1,
                                  src.getHeight() - interp.getHeight() + 1);
            } else {
                srcBounds = new Rectangle(src.getMinX(),
					  src.getMinY(),
					  src.getWidth(),
					  src.getHeight());
            }

            // If necessary, clip the ROI to the effective source bounds.
            if(!srcBounds.contains(srcROI.getBounds())) {
                srcROI = srcROI.intersect(new ROIShape(srcBounds));
            }

            // Set the nearest neighbor interpolation object.
            Interpolation interpNN = interp instanceof InterpolationNearest ?
                interp :
                Interpolation.getInstance(Interpolation.INTERP_NEAREST);

            // Retrieve the Warp object.
            Warp warp = (Warp)pb.getObjectParameter(0);

            // Create the warped ROI.
            ROI dstROI = new ROI(JAI.create("warp", srcROI.getAsImage(),
                                            warp, interpNN));

            // Retrieve the destination bounds.
            Rectangle dstBounds = op.getBounds();

            // If necessary, clip the warped ROI to the destination bounds.
            if(!dstBounds.contains(dstROI.getBounds())) {
                dstROI = dstROI.intersect(new ROIShape(dstBounds));
            }

            // Return the warped and possibly clipped ROI.
            return dstROI;
        }

        return java.awt.Image.UndefinedProperty;
    }
}

/**
 * An <code>OperationDescriptor</code> describing the "Warp" operation.
 *
 * <p> The "Warp" operation performs (possibly filtered) general
 * warping on an image.
 *
 * <p> The destination bounds may be specified by an {@link ImageLayout}
 * hint provided via a {@link RenderingHints} supplied to the operation. If
 * no bounds are so specified, then the destination bounds will be set to
 * the minimum bounding rectangle of the forward mapped source bounds
 * calculated using {@link Warp#mapSourceRect(Rectangle)} or, failing that,
 * {@link Warp#mapSourcePoint(Point2D)} applied to the vertices of the
 * source bounds. If forward mapping by both methods is not viable, then
 * an approximate affine mapping will be created and used to determine the
 * destination bounds by forward mapping the source bounds. If this approach
 * also fails, then the destination bounds will be set to the source bounds.
 *
 * <p> "Warp" defines a PropertyGenerator that
 * performs an identical transformation on the "ROI" property of the
 * source image, which can be retrieved by calling the
 * <code>getProperty</code> method with "ROI" as the property name.
 *
 * <p> The parameter, "backgroundValues", is defined to
 * fill the background with the user-specified background
 * values.  These background values will be translated into background
 * colors by the <code>ColorModel</code> when the image is displayed.
 * With the default value, <code>{0.0}</code>, of this parameter,
 * the background pixels are filled with 0s.  If the provided array
 * length is smaller than the number of bands, the first element of
 * the provided array is used for all the bands.  If the provided values
 * are out of the data range of the destination image, they will be clamped
 * into the proper range.
 *
 * <p> It should be noted that this operation automatically adds a
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
 * <tr><td>GlobalName</td>  <td>Warp</td></tr>
 * <tr><td>LocalName</td>   <td>Warp</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Warps an image according
 *                              to a specified Warp object.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/WarpDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The Warp object.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The interpolation method.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>warp</td>          <td>com.lightcrafts.mediax.jai.Warp</td>
 *                            <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>interpolation</td> <td>com.lightcrafts.mediax.jai.Interpolation</td>
 *                            <td>InterpolationNearest</td>
 * <tr><td>backgroundValues</td> <td>double[]</td>
 *                            <td>{0.0}</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.Interpolation
 * @see com.lightcrafts.mediax.jai.Warp
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class WarpDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "Warp" operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Warp"},
        {"LocalName",   "Warp"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("WarpDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/WarpDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("WarpDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("WarpDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("WarpDescriptor3")}
    };

    /** The parameter names for the "Warp" operation. */
    private static final String[] paramNames = {
        "warp", "interpolation", "backgroundValues"
    };

    /** The parameter class types for the "Warp" operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.mediax.jai.Warp.class, com.lightcrafts.mediax.jai.Interpolation.class,
	double[].class
    };

    /** The parameter default values for the "Warp" operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT,
        Interpolation.getInstance(Interpolation.INTERP_NEAREST),
	new double[] {0.0}
    };

    /** Constructor. */
    public WarpDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "Warp" operation.
     *
     * @return  An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new WarpPropertyGenerator();
        return pg;
    }


    /**
     * Warps an image according to a specified Warp object.
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
     * @param warp The warp object.
     * @param interpolation The interpolation method.
     * May be <code>null</code>.
     * @param backgroundValues The user-specified background values.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>warp</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Warp warp,
                                    Interpolation interpolation,
                                    double[] backgroundValues,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Warp",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("warp", warp);
        pb.setParameter("interpolation", interpolation);
        pb.setParameter("backgroundValues", backgroundValues);

        return JAI.create("Warp", pb, hints);
    }
}
