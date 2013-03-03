/*
 * $RCSfile: AffineDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:29 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import com.lightcrafts.media.jai.util.PropertyGeneratorImpl;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.PropertyGenerator;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.ROIShape;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * This property generator computes the properties for the operation
 * "Affine" dynamically.
 */
class AffinePropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public AffinePropertyGenerator() {
        super(new String[] {"ROI"},
              new Class[] {ROI.class},
              new Class[] {RenderedOp.class});
    }

    /**
     * Returns the specified property in the rendered layer.
     *
     * @param name   Property name.
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
            RenderedImage src = pb.getRenderedSource(0);
            Object property = src.getProperty("ROI");
            if (property == null ||
                property.equals(java.awt.Image.UndefinedProperty) ||
                !(property instanceof ROI)) {
                return java.awt.Image.UndefinedProperty;
            }
            ROI srcROI = (ROI)property;

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
            if (!srcBounds.contains(srcROI.getBounds())) {
                srcROI = srcROI.intersect(new ROIShape(srcBounds));
            }

            // Retrieve the AffineTransform object.
            AffineTransform transform =
                (AffineTransform)pb.getObjectParameter(0);

            // Create the transformed ROI.
            ROI dstROI = srcROI.transform((AffineTransform)transform);

            // Retrieve the destination bounds.
            Rectangle dstBounds = op.getBounds();

            // If necessary, clip the transformed ROI to the
            // destination bounds.
            if (!dstBounds.contains(dstROI.getBounds())) {
                dstROI = dstROI.intersect(new ROIShape(dstBounds));
            }

            // Return the transformed and possibly clipped ROI.
            return dstROI;
        }

        return java.awt.Image.UndefinedProperty;
    }
}

/**
 * An <code>OperationDescriptor</code> describing the "Affine" operation.
 *
 * <p> The Affine operation performs (possibly filtered) affine
 * mapping on a rendered or renderable source image.
 *
 * <p> The relationship between the source and the destination pixels
 * is defined as follows.  For each pixel (x, y) of the destination,
 * the source value at the fractional subpixel position (x', y') is
 * constructed by means of an Interpolation object and written to the
 * destination.
 *
 * The mapping between the destination pixel (x, y) and the source
 * position (x', y') is given by:
 *
 * <pre>
 *    x' = m[0][0] * x + m[0][1] * y + m[0][2]
 *    y' = m[1][0] * x + m[1][1] * y + m[1][2]
 * </pre>
 *
 * where m is a 3x2 transform matrix that inverts the matrix supplied
 * as the "transform" argument.
 *
 * <p> When interpolations which require padding the source such as Bilinear
 * or Bicubic interpolation are specified, the source needs to be extended
 * such that it has the extra pixels needed to compute all the destination
 * pixels. This extension is performed via the <code>BorderExtender</code>
 * class. The type of Border Extension can be specified as a
 * <code>RenderingHint</code> to the <code>JAI.create</code> method.
 *
 * <p> The parameter, "backgroundValues", is defined to
 * fill the background with the user-specified background
 * values.  These background values will be translated into background
 * colors by the <code>ColorModel</code> when the image is displayed.
 * With the default value, <code>{0.0}</code>, of this parameter,
 * the background pixels are filled with 0s.  If the provided array
 * length is smaller than the number of bands, the first element of
 * the provided array is used for all the bands. If the provided values
 * are out of the data range of the destination image, they will be clamped
 * into the proper range.
 *
 * <p> If no BorderExtender is specified (is null), the source will
 * not be extended. The transformed image size is still the same as if
 * the source had been extended. However, since there is insufficient
 * source to compute all the destination pixels, only that subset of
 * the destination image's pixels which can be computed will be
 * written in the destination.  The rest of the destination will be
 * set to the user-specified background values.
 *
 * <p> It may be noted that the minX, minY, width and height hints as
 * specified through the <code>JAI.KEY_IMAGE_LAYOUT</code> hint in the
 * <code>RenderingHints</code> object are not honored, as this operator
 * calculates the destination image bounds itself. The other
 * <code>ImageLayout</code> hints, like tileWidth and tileHeight,
 * however are honored.
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
 * <p> "Affine" defines a PropertyGenerator that performs an identical
 * transformation on the "ROI" property of the source image, which can
 * be retrieved by calling the <code>getProperty</code> method with
 * "ROI" as the property name.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Affine</td></tr>
 * <tr><td>LocalName</td>   <td>Affine</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Performs interpolated affine transform on
 *                              an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/AffineDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The affine transform matrix.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The interpolation method.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>transform</td>     <td>java.awt.geom.AffineTransform</td>
 *                            <td>identity transform</td>
 * <tr><td>interpolation</td> <td>com.lightcrafts.mediax.jai.Interpolation</td>
 *                            <td>InterpolationNearest</td>
 * <tr><td>backgroundValues</td> <td>double[]</td>
 *                            <td>{0.0}</td>
 * </table></p>
 *
 * @see java.awt.geom.AffineTransform
 * @see com.lightcrafts.mediax.jai.Interpolation
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class AffineDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Affine"},
        {"LocalName",   "Affine"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("AffineDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/AffineDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("AffineDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("AffineDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("AffineDescriptor3")},
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        java.awt.geom.AffineTransform.class,
        com.lightcrafts.mediax.jai.Interpolation.class,
	double[].class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "transform", "interpolation", "backgroundValues"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        new AffineTransform(),
        Interpolation.getInstance(Interpolation.INTERP_NEAREST),
	new double[] {0.0}
    };

    /** Constructor. */
    public AffineDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "Affine" operation.
     *
     * @return  An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new AffinePropertyGenerator();
        return pg;
    }

    /**
     * Validates the input parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "transform" is
     * invertible.
     */
    protected boolean validateParameters(ParameterBlock args,
                                         StringBuffer message) {
        if (!super.validateParameters(args, message)) {
            return false;
        }

        AffineTransform transform =
                        (AffineTransform)args.getObjectParameter(0);
        try {
            AffineTransform itransform = transform.createInverse();
        } catch (java.awt.geom.NoninvertibleTransformException e) {
            message.append(getName() + " " +
                           JaiI18N.getString("AffineDescriptor4"));
            return false;
        }

        return true;
    }


    /**
     * Performs interpolated affine transform on an image.
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
     * @param transform The affine transform matrix.
     * May be <code>null</code>.
     * @param interpolation The interpolation method.
     * May be <code>null</code>.
     * @param backgroundValues The user-specified background values.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    AffineTransform transform,
                                    Interpolation interpolation,
                                    double[] backgroundValues,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Affine",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("transform", transform);
        pb.setParameter("interpolation", interpolation);
        pb.setParameter("backgroundValues", backgroundValues);

        return JAI.create("Affine", pb, hints);
    }

    /**
     * Performs interpolated affine transform on an image.
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
     * @param transform The affine transform matrix.
     * May be <code>null</code>.
     * @param interpolation The interpolation method.
     * May be <code>null</code>.
     * @param backgroundValues The user-specified background values.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                AffineTransform transform,
                                                Interpolation interpolation,
                                                double[] backgroundValues,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Affine",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("transform", transform);
        pb.setParameter("interpolation", interpolation);
        pb.setParameter("backgroundValues", backgroundValues);

        return JAI.createRenderable("Affine", pb, hints);
    }
}
