/*
 * $RCSfile: ShearDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:44 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import com.lightcrafts.media.jai.util.PropertyGeneratorImpl;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.EnumeratedParameter;
import com.lightcrafts.mediax.jai.GeometricOpImage;
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
import com.lightcrafts.mediax.jai.operator.ShearDir;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * This property generator computes the properties for the operation
 * "Shear" dynamically.
 */
class ShearPropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public ShearPropertyGenerator() {
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

            // Set the nearest neighbor interpolation object.
            Interpolation interpNN = interp instanceof InterpolationNearest ?
                interp :
                Interpolation.getInstance(Interpolation.INTERP_NEAREST);

            // Retrieve the operation parameters.
            float sv = pb.getFloatParameter(0);
            EnumeratedParameter shearDir =
                (EnumeratedParameter)pb.getObjectParameter(1);
            float tx = pb.getFloatParameter(2);
            float ty = pb.getFloatParameter(3);

            // Create an equivalent transform.
            AffineTransform transform =
                new AffineTransform(1.0,
                                    shearDir == ShearDescriptor.SHEAR_VERTICAL ? sv : 0,
                                    shearDir == ShearDescriptor.SHEAR_HORIZONTAL ? sv : 0,
                                    1.0,
                                    shearDir == ShearDescriptor.SHEAR_HORIZONTAL ? tx : 0,
                                    shearDir == ShearDescriptor.SHEAR_VERTICAL ? ty : 0);

            // Create the sheared ROI.
            ROI dstROI = srcROI.transform(transform);

            // Retrieve the destination bounds.
            Rectangle dstBounds = op.getBounds();

            // If necessary, clip the sheared ROI to the destination bounds.
            if(!dstBounds.contains(dstROI.getBounds())) {
                dstROI = dstROI.intersect(new ROIShape(dstBounds));
            }

            // Return the sheared and possibly clipped ROI.
            return dstROI;
        }

        return java.awt.Image.UndefinedProperty;
    }
}

/**
 * An <code>OperationDescriptor</code> describing the "Shear" operation.
 *
 * <p> The "Shear" operation shears an image either horizontally or
 * vertically.  For each pixel (x, y) of the destination, the source
 * value at the fractional subpixel position (x', y') is constructed by
 * means of an Interpolation object and written to the destination.
 *
 * <p> If the "shearDir" parameter is equal to SHEAR_HORIZONTAL
 * then <code>x' = (x - xTrans - y*shear)</code> and <code>y' = y</code>.
 * If the "shearDir" parameter is equal to SHEAR_VERTICAL
 * then <code>x' = x</code> and <code>y' = (y - yTrans - x*shear)</code>.
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
 * <p> "Shear" defines a PropertyGenerator that performs an identical
 * transformation on the "ROI" property of the source image, which can
 * be retrieved by calling the <code>getProperty</code> method with
 * "ROI" as the property name.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>shear</td></tr>
 * <tr><td>LocalName</td>   <td>shear</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Shears an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ShearDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The shear value.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The shear direction.</td></tr>
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
 * <tr><td>shear</td>         <td>java.lang.Float</td>
 *                            <td>0.0F</td>
 * <tr><td>shearDir</td>      <td>com.lightcrafts.mediax.jai.operator.ShearDir</td>
 *                            <td>SHEAR_HORIZONTAL</td>
 * <tr><td>xTrans</td>        <td>java.lang.Float</td>
 *                            <td>0.0F</td>
 * <tr><td>yTrans</td>        <td>java.lang.Float</td>
 *                            <td>0.0F</td>
 * <tr><td>interpolation</td> <td>com.lightcrafts.mediax.jai.Interpolation</td>
 *                            <td>InterpolationNearest</td>
 * <tr><td>backgroundValues</td> <td>double[]</td>
 *                            <td>{0.0}</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.Interpolation
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 * @see ShearDir
 */
public class ShearDescriptor extends OperationDescriptorImpl {

    public static final ShearDir SHEAR_HORIZONTAL =
        new ShearDir("SHEAR_HORIZONTAL", 0);
    public static final ShearDir SHEAR_VERTICAL =
        new ShearDir("SHEAR_VERTICAL", 1);

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "Shear" operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Shear"},
        {"LocalName",   "Shear"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("ShearDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ShearDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion2")},
        {"arg0Desc",    JaiI18N.getString("ShearDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("ShearDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("ShearDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("ShearDescriptor4")},
        {"arg4Desc",    JaiI18N.getString("ShearDescriptor5")},
        {"arg5Desc",    JaiI18N.getString("ShearDescriptor6")}
    };

    /** The parameter names for the "Shear" operation. */
    private static final String[] paramNames = {
        "shear", "shearDir", "xTrans", "yTrans", "interpolation",
	"backgroundValues"
    };

    /** The parameter class types for the "Shear" operation. */
    private static final Class[] paramClasses = {
        java.lang.Float.class, com.lightcrafts.mediax.jai.operator.ShearDir.class,
        java.lang.Float.class, java.lang.Float.class,
        com.lightcrafts.mediax.jai.Interpolation.class,
	double[].class
    };

    /** The parameter default values for the "Shear" operation. */
    private static final Object[] paramDefaults = {
        new Float(0.0F), SHEAR_HORIZONTAL,
        new Float(0.0F), new Float(0.0F),
        Interpolation.getInstance(Interpolation.INTERP_NEAREST),
	new double[] {0.0}
    };

    /** Constructor. */
    public ShearDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "Shear" operation.
     *
     * @return  An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new ShearPropertyGenerator();
        return pg;
    }


    /**
     * Shears an image.
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
     * @param shear The shear value.
     * May be <code>null</code>.
     * @param shearDir The shear direction.
     * May be <code>null</code>.
     * @param xTrans The X translation.
     * May be <code>null</code>.
     * @param yTrans The Y translation.
     * May be <code>null</code>.
     * @param interpolation The interpolation method for resampling.
     * May be <code>null</code>.
     * @param backgroundValues The user-specified background values.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Float shear,
                                    ShearDir shearDir,
                                    Float xTrans,
                                    Float yTrans,
                                    Interpolation interpolation,
                                    double[] backgroundValues,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Shear",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("shear", shear);
        pb.setParameter("shearDir", shearDir);
        pb.setParameter("xTrans", xTrans);
        pb.setParameter("yTrans", yTrans);
        pb.setParameter("interpolation", interpolation);
        pb.setParameter("backgroundValues", backgroundValues);

        return JAI.create("Shear", pb, hints);
    }
}
