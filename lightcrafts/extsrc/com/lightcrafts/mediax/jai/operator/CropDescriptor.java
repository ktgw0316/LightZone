/*
 * $RCSfile: CropDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:33 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
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
 * An <code>OperationDescriptor</code> describing the "Crop" operation.
 *
 * <p> The Crop operation takes one rendered or renderable image and
 * crops the image to a specified rectangular area.  The rectangular
 * area must not be empty, and must be fully contained with the source
 * image bounds.
 *
 * <p> For rendered images the supplied origin and dimensions are used to
 * determine the smallest rectangle with integral origin and dimensions which
 * encloses the rectangular area requested.
 *
 * <p> For renderable images the rectangular area is specified in
 * rendering-independent coordinates.  When the image is rendered this area
 * will be mapped to rendered image coordinates using the affine transform
 * supplied for the rendering.  The crop bounds in rendered coordinates are
 * defined to be the minimum bounding box of the rectangular area mapped to
 * rendered image coordinates.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Crop</td></tr>
 * <tr><td>LocalName</td>   <td>Crop</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Crops the pixel values of a rendered image
 *                              to a specified rectangle.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/CropDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The x origin for each band.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The y origin for each band.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The width for each band.</td></tr>
 * <tr><td>arg3Desc</td>    <td>The height for each band.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>   <th>Class Type</th>
 *                     <th>Default Value</th></tr>
 * <tr><td>x</td>      <td>Float</td>
 *                     <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>y</td>      <td>Float</td>
 *                     <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>width</td>  <td>Float</td>
 *                     <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>height</td> <td>Float</td>
 *                     <td>NO_PARAMETER_DEFAULT</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor */
public class CropDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Crop"},
        {"LocalName",   "Crop"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("CropDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/CropDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("CropDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("CropDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("CropDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("CropDescriptor4")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
	Float.class,
        Float.class,
        Float.class,
        Float.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "x", "y", "width", "height"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public CropDescriptor() {
        super(resources, supportedModes, 1,
		paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input source and parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "x", "y", "width",
     * and "height" form a rectangle that is not empty and that
     * is fully contained within the bounds of the source image.
     */
    public boolean validateArguments(String modeName,
				     ParameterBlock args,
                                     StringBuffer msg) {
        if (!super.validateArguments(modeName, args, msg)) {
            return false;
        }

	if (modeName.equalsIgnoreCase("rendered"))
	    return validateRenderedArgs(args, msg);

	if (modeName.equalsIgnoreCase("renderable"))
	    return validateRenderableArgs(args, msg);

	return true;
    }

    /**
     * Validates the input source and parameters in the rendered mode.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "x", "y", "width",
     * and "height" form a rectangle that is not empty and that
     * is fully contained within the bounds of the source image.
     */
    private boolean validateRenderedArgs(ParameterBlock args,
                                     StringBuffer msg) {

        // Get parameters.
        float x_req = args.getFloatParameter(0);
        float y_req = args.getFloatParameter(1);
        float w_req = args.getFloatParameter(2);
        float h_req = args.getFloatParameter(3);

        // Create required rectangle.
        Rectangle rect_req =
            (new Rectangle2D.Float(x_req, y_req, w_req, h_req)).getBounds();

        // Check for an empty rectangle.
        if(rect_req.isEmpty()) {
            msg.append(getName() + " " +
                       JaiI18N.getString("CropDescriptor5"));
            return false;
        }

        // Check for out-of-bounds
        RenderedImage src = (RenderedImage)args.getSource(0);
	
	Rectangle srcBounds = 
	  new Rectangle(src.getMinX(),
			src.getMinY(),
			src.getWidth(),
			src.getHeight());

        if (!srcBounds.contains(rect_req)) {
            msg.append(getName() + " " +
                       JaiI18N.getString("CropDescriptor6"));
            return false;
        }

        return true;
    }


    /**
     * Validates the input source and parameters in the renderable mode.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "x", "y", "width",
     * and "height" form a rectangle that is not empty and that
     * is fully contained within the bounds of the source image.
     */
    private boolean validateRenderableArgs(ParameterBlock args,
                                               StringBuffer msg) {
        // Get parameters.
        float x_req = args.getFloatParameter(0);
        float y_req = args.getFloatParameter(1);
        float w_req = args.getFloatParameter(2);
        float h_req = args.getFloatParameter(3);

        // Create required rectangle.
        Rectangle2D rect_req =
            new Rectangle2D.Float(x_req, y_req, w_req, h_req);

        // Check for an empty rectangle.
        if(rect_req.isEmpty()) {
            msg.append(getName() + " " +
                       JaiI18N.getString("CropDescriptor5"));
            return false;
        }

        // Check for out-of-bounds
        RenderableImage src = (RenderableImage)args.getSource(0);

        Rectangle2D rect_src =
            new Rectangle2D.Float(src.getMinX(),
                                  src.getMinY(),
                                  src.getWidth(),
                                  src.getHeight());

        if (!rect_src.contains(rect_req)) {
            msg.append(getName() + " " +
                       JaiI18N.getString("CropDescriptor6"));
            return false;
        }

        return true;
    }


    /**
     * Performs cropping to a specified bounding box.
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
     * @param x The x origin of the cropping operation.
     * @param y The y origin of the cropping operation.
     * @param width The width of the cropping operation.
     * @param height The height of the cropping operation.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>x</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>y</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>width</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>height</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Float x,
                                    Float y,
                                    Float width,
                                    Float height,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Crop",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("x", x);
        pb.setParameter("y", y);
        pb.setParameter("width", width);
        pb.setParameter("height", height);

        return JAI.create("Crop", pb, hints);
    }

    /**
     * Performs cropping to a specified bounding box.
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
     * @param x The x origin of the cropping operation.
     * @param y The y origin of the cropping operation.
     * @param width The width of the cropping operation.
     * @param height The height of the cropping operation.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>x</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>y</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>width</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>height</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                Float x,
                                                Float y,
                                                Float width,
                                                Float height,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Crop",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("x", x);
        pb.setParameter("y", y);
        pb.setParameter("width", width);
        pb.setParameter("height", height);

        return JAI.createRenderable("Crop", pb, hints);
    }
}
