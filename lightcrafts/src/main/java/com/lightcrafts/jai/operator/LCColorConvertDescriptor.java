/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: LCColorConvertDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/03/28 17:45:12 $
 * $State: Exp $
 */

package com.lightcrafts.jai.operator;

import javax.media.jai.EnumeratedParameter;
import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderableRegistryMode;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.*;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;

/**
 * An <code>OperationDescriptor</code> describing the "ColorConvert" operation.
 *
 * <p> The "ColorConvert" operation performs a pixel-by-pixel color
 * conversion of the data in a rendered or renderable source image.
 *
 * <p> The data are treated as having no alpha channel, i.e., all bands are
 * color bands.  The color space of the source image is specified by the
 * <code>ColorSpace</code> object of the source image <code>ColorModel</code>
 * which must not be <code>null</code>.  The color space of the destination
 * image is specified by the <code>ColorSpace</code> of the "colorModel"
 * parameter which must be a <code>ColorModel</code>.  If a
 * <code>ColorModel</code> is suggested via the <code>RenderingHints</code>
 * it is ignored.
 *
 * <p> The calculation pathway is selected to optimize performance and
 * accuracy based on which <code>ColorSpace</code> subclasses are used to
 * represent the source and destination color spaces.  The subclass
 * categories are <code>ICC_ColorSpace</code>, <code>ColorSpaceJAI</code>,
 * and generic <code>ColorSpace</code>, i.e., one which is not an instance
 * of either the two aforementioned subclasses.  Note that in the Sun
 * Microsystems implementation, an <code>ICC_ColorSpace</code> instance
 * is what is returned by <code>ColorSpace.getInstance()</code>.
 *
 * <p> Integral data are assumed to occupy the full range of the respective
 * data type; floating point data are assumed to be normalized to the range
 * [0.0,1.0].
 *
 * <p> By default, the destination image bounds, data type, and number of
 * bands are the same as those of the source image.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>LCColorConvert</td></tr>
 * <tr><td>LocalName</td>   <td>LCColorConvert</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts</td></tr>
 * <tr><td>Description</td> <td>Convert the color space of an image.<td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ColorConvertDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The destination <code>ColorModel</code>.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>      <th>Class Type</th>
 *                        <th>Default Value</th></tr>
 * <tr><td>colorModel</td> <td>java.awt.image.ColorModel</td>
 *                        <td>NO_PARAMETER_DEFAULT</td>
 * </table></p>
 *
 * @see javax.media.jai.OperationDescriptor
 * @see java.awt.color.ColorSpace
 * @see java.awt.color.ICC_ColorSpace
 * @see java.awt.image.ColorModel
 * @see javax.media.jai.ColorSpaceJAI
 * @see javax.media.jai.IHSColorSpace
 */
public class LCColorConvertDescriptor extends OperationDescriptorImpl {

    public static final class RenderingIntent extends EnumeratedParameter {
        RenderingIntent(String name, int value) {
            super(name, value);
        }
    }

    public static final RenderingIntent DEFAULT = new RenderingIntent("DEFAULT", -1);
    public static final RenderingIntent PERCEPTUAL = new RenderingIntent("PERCEPTUAL", ICC_Profile.icPerceptual);
    public static final RenderingIntent SATURATION = new RenderingIntent("SATURATION", ICC_Profile.icSaturation);
    public static final RenderingIntent RELATIVE_COLORIMETRIC = new RenderingIntent("RELATIVE_COLORIMETRIC", ICC_Profile.icRelativeColorimetric);
    public static final RenderingIntent ABSOLUTE_COLORIMETRIC = new RenderingIntent("ABSOLUTE_COLORIMETRIC", ICC_Profile.icAbsoluteColorimetric);

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "LCColorConvert"},
        {"LocalName",   "LCColorConvert"},
        {"Vendor",      "com.lightcrafts"},
        {"Description", LCJaiI18N.getString("LCColorConvertDescriptor0")},
        {"DocURL",      "none"},
        {"Version",     LCJaiI18N.getString("DescriptorVersion2")},
        {"arg0Desc",    LCJaiI18N.getString("LCColorConvertDescriptor1")}
    };

    /**
     * The parameter class list for this operation.
     */
    private static final Class[] paramClasses = {
        java.awt.image.ColorModel.class,
        RenderingIntent.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "colorModel",
        "renderingIntent"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT,
        DEFAULT
    };

    /** Constructor. */
    public LCColorConvertDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    /**
     * Convert the color space of an image.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link javax.media.jai.JAI#create(String,java.awt.image.renderable.ParameterBlock,java.awt.RenderingHints)}.
     *
     * @see javax.media.jai.JAI
     * @see javax.media.jai.ParameterBlockJAI
     * @see javax.media.jai.RenderedOp
     *
     * @param source0 <code>RenderedImage</code> source 0.
     * @param colorModel The destination color space.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>colorModel</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    ColorModel colorModel,
                                    RenderingIntent renderingIntent,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("LCColorConvert",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("colorModel", colorModel);
        pb.setParameter("renderingIntent", renderingIntent);

        return JAI.create("LCColorConvert", pb, hints);
    }

    /**
     * Convert the color space of an image.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,java.awt.image.renderable.ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see javax.media.jai.RenderableOp
     *
     * @param source0 <code>RenderableImage</code> source 0.
     * @param colorModel The destination color space.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>colorModel</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                ColorModel colorModel,
                                                RenderingIntent renderingIntent,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("LCColorConvert",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("colorModel", colorModel);
        pb.setParameter("renderingIntent", renderingIntent);

        return JAI.createRenderable("LCColorConvert", pb, hints);
    }
}
