/*
 * $RCSfile: SubsampleAverageDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:44 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.operator;

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
import com.lightcrafts.mediax.jai.util.Range;

/**
 * This property generator computes the properties for the operation
 * "SubsampleAverage" dynamically.
 *
 * @since JAI 1.1.2
 */
class SubsampleAveragePropertyGenerator implements PropertyGenerator {

    /** Constructor. */
    public SubsampleAveragePropertyGenerator() {}

    /**
     * Returns the valid property names for the operation "SubsampleAverage".
     * This is equal to the array <code>{"ROI"}</code>.
     */
    public String[] getPropertyNames() {
        String[] properties = new String[1];
        properties[0] = "ROI";
        return(properties);
    }

    /**
     * Returns the expected class which is <code>ROI.class</code> if
     * <code>propertyName</code> and <code>null</code> otherwise.
     */
    public Class getClass(String propertyName) {
        if(propertyName == null) {
            throw new IllegalArgumentException
                (JaiI18N.getString("SubsampleAveragePropertyGenerator0"));
        } else if(propertyName.equalsIgnoreCase("roi")) {
            return ROI.class;
        }

        return null;
    }

    /**
     * Determines whether properties can be generated from the supplied node.
     */
    public boolean canGenerateProperties(Object opNode) {
        if(opNode == null) {
            throw new IllegalArgumentException
                (JaiI18N.getString("SubsampleAveragePropertyGenerator1"));
        }

        return opNode instanceof RenderedOp;
    }


    /**
     * Returns the specified property for the supplied node.
     *
     * @param name   Property name.
     * @param opNode Operation node.
     */
    public Object getProperty(String name,
                              Object opNode) {
        if(name == null || opNode == null) {
            throw new IllegalArgumentException
                (JaiI18N.getString("SubsampleAveragePropertyGenerator2"));
        } else if(!canGenerateProperties(opNode)) {
            throw new IllegalArgumentException
                (opNode.getClass().getName()+
                 JaiI18N.getString("SubsampleAveragePropertyGenerator3"));
        }

        return opNode instanceof RenderedOp ?
            getProperty(name, (RenderedOp)opNode) : null;
    }

    /**
     * Returns the specified property.
     *
     * @param name Property name.
     * @param op   Operation node.
     */
    public Object getProperty(String name,
                              RenderedOp op) {
        if(name == null || op == null) {
            throw new IllegalArgumentException
                (JaiI18N.getString("SubsampleAveragePropertyGenerator4"));
        }

        if (name.equals("roi")) {
            ParameterBlock pb = op.getParameterBlock();

            // Retrieve the rendered source image and its ROI.
            PlanarImage src = (PlanarImage)pb.getRenderedSource(0);
            Object property = src.getProperty("ROI");
            if (property == null ||
                property.equals(java.awt.Image.UndefinedProperty) ||
                !(property instanceof ROI)) {
                return null;
            }
            ROI srcROI = (ROI)property;

            // Determine the effective source bounds.
            Rectangle srcBounds = null;
            PlanarImage dst = op.getRendering();
            if(dst instanceof GeometricOpImage &&
               ((GeometricOpImage)dst).getBorderExtender() == null) {
                GeometricOpImage geomIm = (GeometricOpImage)dst;
                Interpolation interp = geomIm.getInterpolation();
                srcBounds =
                    new Rectangle(src.getMinX() + interp.getLeftPadding(),
                                  src.getMinY() + interp.getTopPadding(),
                                  src.getWidth() - interp.getWidth() + 1,
                                  src.getHeight() - interp.getHeight() + 1);
            } else {
                srcBounds = src.getBounds();
            }

            // If necessary, clip the ROI to the effective source bounds.
            if(!srcBounds.contains(srcROI.getBounds())) {
                srcROI = srcROI.intersect(new ROIShape(srcBounds));
            }

            // Retrieve the scale factors and translation values.
            double sx = pb.getDoubleParameter(0);
            double sy = pb.getDoubleParameter(1);

            // Create an equivalent transform.
            AffineTransform transform =
                new AffineTransform(sx, 0.0, 0.0, sy, 0, 0);

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
        } else {
            return null;
        }
    }

    /**
     * Returns null.
     *
     * @param name Property name.
     * @param op   Operation node.
     */
    public Object getProperty(String name,
                              RenderableOp op) {
        if(name == null || op == null) {
            throw new IllegalArgumentException
                (JaiI18N.getString("SubsampleAveragePropertyGenerator2"));
        }

        return null;
    }
}

/**
 * An <code>OperationDescriptor</code> describing the "SubsampleAverage"
 * operation.  "SubsampleAverage" supports the rendered and renderable modes.
 *
 * <p>
 * The "SubsampleAverage" operation subsamples an image by averaging
 * over a moving window.  The scale factors supplied to the operation are
 * forward mapping coefficients representing the geometric transformation
 * from source to destination image coordinates.  For example, if both
 * scale factors were equal to 0.5, the operation would produce an output
 * image of half the size of the input image in both dimensions.  Both
 * scale factors must be in the range <code>(0.0,&nbsp;1.0]</code> or an
 * exception will be thrown when the operation is created.  The default
 * value of the vertical scale factor is the value of the horizontal scale
 * factor.  If both scale factors equal <core>1.0</code>, the source
 * image is returned directly.
 * </p>
 *
 * <p>
 * The size of the moving window or <i>block</i> over which source pixels are
 * averaged varies as a function of the scale factors and is defined as
 * <pre>
 *     int blockX = (int)Math.ceil(1.0/scaleX);
 *     int blockY = (int)Math.ceil(1.0/scaleY);
 * </pre>
 * </p>
 *
 * <p>
 * For a given destination pixel <code>(dstX,&nbsp;dstY)</code>, the upper
 * left corner <code>(srcX,&nbsp;srcY)</code> of the source block over which
 * averaging occurs is defined as
 * <pre>
 *     int srcX = (int)Math.floor((dstX - dstMinX)/scaleX + 0.5) + srcMinX;
 *     int srcY = (int)Math.floor((dstY - dstMinY)/scaleY + 0.5) + srcMinY;
 * </pre>
 * where <code>(srcMinX,&nbsp;srcMinY)</code> are the image coordinates of the
 * upper left pixel of the source and <code>(dstMinX,&nbsp;dstMinY)</code> are
 * the image coordinates of the upper left pixel of the destination.
 * </p>
 *
 * <p>
 * The destination image bounds are defined as
 * <pre>
 *    int dstMinX = (int)Math.floor(srcMinX*scaleX);
 *    int dstMinY = (int)Math.floor(srcMinY*scaleY);
 *    int dstWidth = (int)(srcWidth*scaleX);
 *    int dstHeight = (int)(srcHeight*scaleY);
 * </pre>
 * where <code>(srcWidth,&nbsp;srcHeight)</code> and
 * <code>(dstWidth,&nbsp;dstHeight)</code> are the source and destination
 * image dimensions, respectively.
 * </p>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>SubsampleAverage</td></tr>
 * <tr><td>LocalName</td>   <td>SubsampleAverage</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Subsamples an image by averaging over a moving window.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/SubsampleAverageDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The X scale factor.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The Y scale factor.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>scaleX</td>        <td>java.lang.Double</td>
 *                            <td>0.5</td>
 * <tr><td>scaleY</td>        <td>java.lang.Double</td>
 *                            <td>scaleX</td>
 * </table></p>
 *
 * @see FilteredSubsampleDescriptor
 * @see SubsampleBinaryToGrayDescriptor
 * @see ScaleDescriptor
 *
 * @since JAI 1.1.2
 */
public class SubsampleAverageDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "SubsampleAverage"},
        {"LocalName",   "SubsampleAverage"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("SubsampleAverageDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/SubsampleAverageDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("SubsampleAverageDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("SubsampleAverageDescriptor2")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        java.lang.Double.class, java.lang.Double.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "scaleX", "scaleY"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        new Double(0.5), null
    };

    /** The allowable <code>Range</code>s of parameter values. */
    private static final Object[] validParamValues = {
        new Range(Double.class, new Double(Double.MIN_VALUE), new Double(1.0)),
        new Range(Double.class, new Double(Double.MIN_VALUE), new Double(1.0))
    };

    /** Constructor. */
    public SubsampleAverageDescriptor() {
        super(resources,
              new String[] {RenderedRegistryMode.MODE_NAME,
                            RenderableRegistryMode.MODE_NAME},
              1,
              paramNames,
              paramClasses,
              paramDefaults,
              validParamValues);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "SubsampleAverage" operation.
     */
    public PropertyGenerator[] getPropertyGenerators(String modeName) {
        if(modeName == null) {
            throw new IllegalArgumentException
                (JaiI18N.getString("SubsampleAverageDescriptor3"));
        }

        if(!RenderedRegistryMode.MODE_NAME.equalsIgnoreCase(modeName)) {
            PropertyGenerator[] pg = new PropertyGenerator[1];
            pg[0] = new SubsampleAveragePropertyGenerator();
            return pg;
        }

        return null;
    }

    /**
     * Validates the input parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method sets "scaleX" to "scaleY"
     * if the latter is not provided in <code>args</code>.
     */
    protected boolean validateParameters(String modeName,
                                         ParameterBlock args,
                                         StringBuffer msg) {
        if (!super.validateParameters(modeName, args, msg)) {
            return false;
        }

        if(args.getNumParameters() < 2 || args.getObjectParameter(1) == null) {
            args.set(args.getObjectParameter(0), 1);
        }

        return true;
    }


    /**
     * Subsamples an image by averaging over a moving window.
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
     * @param scaleX The X scale factor.
     * May be <code>null</code>.
     * @param scaleY The Y scale factor.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Double scaleX,
                                    Double scaleY,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("SubsampleAverage",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("scaleX", scaleX);
        pb.setParameter("scaleY", scaleY);

        return JAI.create("SubsampleAverage", pb, hints);
    }

    /**
     * Subsamples an image by averaging over a moving window.
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
     * @param scaleX The X scale factor.
     * May be <code>null</code>.
     * @param scaleY The Y scale factor.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                Double scaleX,
                                                Double scaleY,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("SubsampleAverage",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("scaleX", scaleX);
        pb.setParameter("scaleY", scaleY);

        return JAI.createRenderable("SubsampleAverage", pb, hints);
    }
}
