/*
 * $RCSfile: ScaleDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:43 $
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
 * "Scale" dynamically.
 */
class ScalePropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public ScalePropertyGenerator() {
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
            RenderedImage src = pb.getRenderedSource(0);
            Object property = src.getProperty("ROI");
            if (property == null ||
                property.equals(java.awt.Image.UndefinedProperty) ||
                !(property instanceof ROI)) {
                return java.awt.Image.UndefinedProperty;
            }
            ROI srcROI = (ROI)property;

            // Retrieve the Interpolation object.
            Interpolation interp = (Interpolation)pb.getObjectParameter(4);

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

            // Retrieve the scale factors and translation values.
            float sx = pb.getFloatParameter(0);
            float sy = pb.getFloatParameter(1);
            float tx = pb.getFloatParameter(2);
            float ty = pb.getFloatParameter(3);

            // Create an equivalent transform.
            AffineTransform transform =
                new AffineTransform(sx, 0.0, 0.0, sy, tx, ty);

            // Create the scaled ROI.
            ROI dstROI = srcROI.transform(transform);

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
 * An <code>OperationDescriptor</code> describing the "Scale" operation.
 *
 * <p> The "Scale" operation translates and resizes an image.  For each
 * pixel (x, y) of the destination, the source value at the fractional
 * subpixel position ((x - xTrans)/xScale, (y - yTrans)/yScale) is
 * constructed by means of an Interpolation object and written to the
 * destination.
 *
 * <p> When applying scale factors of scaleX, scaleY to a source image
 * with the upper left pixel at (srcMinX, srcMinY) and width of srcWidth
 * and height of srcHeight, the resulting image is defined to have the
 * following bounds:
 * 
 * <code>
 *       dstMinX = ceil(A), where A = srcMinX * scaleX - 0.5 + transX,
 *       dstMinY = ceil(B), where B = srcMinY * scaleY - 0.5 + transY,
 *       dstMaxX = ceil(C), where C = (srcMaxX + 1) * scaleX - 1.5 + transX
 *                          and srcMaxX = srcMinX + srcWidth - 1
 *       dstMaxY = ceil(D), where D = (srcMaxY + 1) * scaleY - 1.5 + transY
 *                          and srcMaxY = srcMinY + srcHeight - 1
 *       dstWidth = dstMaxX - dstMinX + 1
 *       dstHeight = dstMaxY - dstMinY + 1
 * </code>
 *
 * <p> In the case where source's upper left pixel is located is (0, 0),
 * the formulae simplify to
 *
 * <code>
 *       dstMinX = 0
 *       dstMinY = 0
 *       dstWidth = ceil (srcWidth * scaleX - 0.5 + transX)
 *       dstHeight = ceil (srcHeight * scaleY - 0.5 + transY)
 * </code>
 *
 * <p> In the case where the source's upper left pixel is located at (0, 0)
 * and the scaling factors are integers, the formulae further simplify to
 *
 * <code>
 *       dstMinX = 0
 *       dstMinY = 0
 *       dstWidth = ceil (srcWidth * scaleX + transX)
 *       dstWidth = ceil (srcHeight * scaleY + transY)
 * </code>
 *
 * <p> When interpolations which require padding the source such as Bilinear  
 * or Bicubic interpolation are specified, the source needs to be extended
 * such that it has the extra pixels needed to compute all the destination
 * pixels. This extension is performed via the <code>BorderExtender</code>
 * class. The type of Border Extension can be specified as a 
 * <code>RenderingHint</code> to the <code>JAI.create</code> method. 
 *
 * <p> If no Border Extension is specified, the source will not be extended. 
 * The scaled image size is still calculated according to the formula
 * specified above. However since there isn't enough source to compute all the
 * destination pixels, only that subset of the destination image's pixels,
 * which can be computed, will be written in the destination. The rest of the 
 * destination will not be written.
 *
 * <p> Specifying a scale factor of greater than 1 increases the size
 * of the image, specifying a scale factor between 0 and 1 (non-inclusive)
 * decreases the size of an image. An IllegalArgumentException will be thrown
 * if the specified scale factors are negative or equal to zero.
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
 * <p> "Scale" defines a PropertyGenerator that performs an identical
 * transformation on the "ROI" property of the source image, which can
 * be retrieved by calling the <code>getProperty</code> method with
 * "ROI" as the property name.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Scale</td></tr>
 * <tr><td>LocalName</td>   <td>Scale</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Resizes an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ScaleDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The X scale factor.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The Y scale factor.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The X translation.</td></tr>
 * <tr><td>arg3Desc</td>    <td>The Y translation.</td></tr>
 * <tr><td>arg4Desc</td>    <td>The interpolation method for
 *                              resampling.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>xScale</td>        <td>java.lang.Float</td>
 *                            <td>1.0F</td>
 * <tr><td>yScale</td>        <td>java.lang.Float</td>
 *                            <td>1.0F</td>
 * <tr><td>xTrans</td>        <td>java.lang.Float</td>
 *                            <td>0.0F</td>
 * <tr><td>yTrans</td>        <td>java.lang.Float</td>
 *                            <td>0.0F</td>
 * <tr><td>interpolation</td> <td>com.lightcrafts.mediax.jai.Interpolation</td>
 *                            <td>InterpolationNearest</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.Interpolation
 * @see com.lightcrafts.mediax.jai.BorderExtender
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class ScaleDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Scale"},
        {"LocalName",   "Scale"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("ScaleDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ScaleDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("ScaleDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("ScaleDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("ScaleDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("ScaleDescriptor4")},
        {"arg4Desc",    JaiI18N.getString("ScaleDescriptor5")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        java.lang.Float.class, java.lang.Float.class,
        java.lang.Float.class, java.lang.Float.class,
        com.lightcrafts.mediax.jai.Interpolation.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "xScale", "yScale", "xTrans", "yTrans", "interpolation"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        new Float(1.0F), new Float(1.0F),
        new Float(0.0F), new Float(0.0F),
        Interpolation.getInstance(Interpolation.INTERP_NEAREST)
    };

    /** Constructor. */
    public ScaleDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "Scale" operation.
     *
     * @return  An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new ScalePropertyGenerator();
        return pg;
    }

    /**
     * Validates the input parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "xScale" and "yScale"
     * are both greater than 0.
     */
    protected boolean validateParameters(ParameterBlock args,
                                         StringBuffer msg) {
        if (!super.validateParameters(args, msg)) {
            return false;
        }

        float xScale = args.getFloatParameter(0);
        float yScale = args.getFloatParameter(1);
        if (xScale <= 0 || yScale <= 0) {
            msg.append(getName() + " " +
                       JaiI18N.getString("ScaleDescriptor6"));
	    return false;
        }

        return true;
    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.
     *
     * <p> For the minimum value of "xScale" and "yScale", this method
     * returns 0.  However, the scale factors must be a positive floating
     * number and can not be 0.
     */
    public Number getParamMinValue(int index) {
        if (index == 0 || index == 1) {
            return new Float(0.0F);
        } else if (index == 2 || index == 3) {
            return new Float(-Float.MAX_VALUE);
        } else if (index == 4) {
            return null;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }


    /**
     * Resizes an image.
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
     * @param xScale The X scale factor.
     * May be <code>null</code>.
     * @param yScale The Y scale factor.
     * May be <code>null</code>.
     * @param xTrans The X translation.
     * May be <code>null</code>.
     * @param yTrans The Y translation.
     * May be <code>null</code>.
     * @param interpolation The interpolation method for resampling.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Float xScale,
                                    Float yScale,
                                    Float xTrans,
                                    Float yTrans,
                                    Interpolation interpolation,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Scale",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("xScale", xScale);
        pb.setParameter("yScale", yScale);
        pb.setParameter("xTrans", xTrans);
        pb.setParameter("yTrans", yTrans);
        pb.setParameter("interpolation", interpolation);

        return JAI.create("Scale", pb, hints);
    }

    /**
     * Resizes an image.
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
     * @param xScale The X scale factor.
     * May be <code>null</code>.
     * @param yScale The Y scale factor.
     * May be <code>null</code>.
     * @param xTrans The X translation.
     * May be <code>null</code>.
     * @param yTrans The Y translation.
     * May be <code>null</code>.
     * @param interpolation The interpolation method for resampling.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                Float xScale,
                                                Float yScale,
                                                Float xTrans,
                                                Float yTrans,
                                                Interpolation interpolation,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Scale",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("xScale", xScale);
        pb.setParameter("yScale", yScale);
        pb.setParameter("xTrans", xTrans);
        pb.setParameter("yTrans", yTrans);
        pb.setParameter("interpolation", interpolation);

        return JAI.createRenderable("Scale", pb, hints);
    }
}
