/*
 * $RCSfile: PeriodicShiftDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:42 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import java.awt.RenderingHints;
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
 * An <code>OperationDescriptor</code> describing the "PeriodicShift"
 * operation.
 *
 * <p> The destination image of the "PeriodicShift" operation is the
 * infinite periodic extension of the source image with horizontal and
 * vertical periods equal to the image width and height, respectively,
 * shifted by a specified amount along each axis and clipped to the
 * bounds of the source image.  Thus for each band <i>b</i> the destination
 * image sample at location <i>(x,y)</i> is defined by:
 *
 * <pre>
 * if(x < width - shiftX) {
 *     if(y < height - shiftY) {
 *         dst[x][y][b] = src[x + shiftX][y + shiftY][b];
 *     } else {
 *         dst[x][y][b] = src[x + shiftX][y - height + shiftY][b];
 *     }
 * } else {
 *     if(y < height - shiftY) {
 *         dst[x][y][b] = src[x - width + shiftX][y + shiftY][b];
 *     } else {
 *         dst[x][y][b] = src[x - width + shiftX][y - height + shiftY][b];
 *     }
 * }
 * </pre>
 *
 * where <i>shiftX</i> and <code>shiftY</code> denote the translation factors
 * along the <i>X</i> and <i>Y</i> axes, respectively.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>PeriodicShift</td></tr>
 * <tr><td>LocalName</td>   <td>PeriodicShift</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Computes the periodic translation of an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PeriodicShiftDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The displacement in the X direction.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The displacement in the Y direction.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>      <th>Class Type</th>
 *                        <th>Default Value</th></tr>
 * <tr><td>shiftX</td>    <td>java.lang.Integer</td>
 *                        <td>sourceWidth/2</td>
 * <tr><td>shiftY</td>    <td>java.lang.Integer</td>
 *                        <td>sourceHeight/2</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class PeriodicShiftDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "PeriodicShift"},
        {"LocalName",   "PeriodicShift"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("PeriodicShiftDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PeriodicShiftDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("PeriodicShiftDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("PeriodicShiftDescriptor2")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
	java.lang.Integer.class,
	java.lang.Integer.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "shiftX", "shiftY"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        null, null
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public PeriodicShiftDescriptor() {
        super(resources, supportedModes, 1,
		paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "shiftX" and
     * "shiftY" are between 0 and the source image width and
     * height, respectively.
     */
    public boolean validateArguments(String modeName,
				     ParameterBlock args,
                                     StringBuffer msg) {
        if (!super.validateArguments(modeName, args, msg)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

        RenderedImage src = args.getRenderedSource(0);

        // Set non-static default values based on source
        if (args.getObjectParameter(0) == null) {
            args.set(new Integer(src.getWidth()/2), 0);
        }
        if (args.getObjectParameter(1) == null) {
            args.set(new Integer(src.getHeight()/2), 1);
        }

        int shiftX = args.getIntParameter(0);
        int shiftY = args.getIntParameter(1);
        if (shiftX < 0 || shiftX >= src.getWidth() || 
            shiftY < 0 || shiftY >= src.getHeight()) {
            msg.append(getName() + " " +
                       JaiI18N.getString("PeriodicShiftDescriptor3"));
            return false;
        }

        return true;
    }


    /**
     * Computes the periodic translation of an image.
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
     * @param shiftX The displacement in the X direction.
     * May be <code>null</code>.
     * @param shiftY The displacement in the Y direction.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Integer shiftX,
                                    Integer shiftY,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("PeriodicShift",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("shiftX", shiftX);
        pb.setParameter("shiftY", shiftY);

        return JAI.create("PeriodicShift", pb, hints);
    }

    /**
     * Computes the periodic translation of an image.
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
     * @param shiftX The displacement in the X direction.
     * May be <code>null</code>.
     * @param shiftY The displacement in the Y direction.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                Integer shiftX,
                                                Integer shiftY,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("PeriodicShift",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("shiftX", shiftX);
        pb.setParameter("shiftY", shiftY);

        return JAI.createRenderable("PeriodicShift", pb, hints);
    }
}
