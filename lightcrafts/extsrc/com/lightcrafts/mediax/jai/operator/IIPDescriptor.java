/*
 * $RCSfile: IIPDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:37 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import java.awt.RenderingHints;
import java.awt.color.ICC_Profile;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.renderable.ParameterBlock;
import java.net.URL;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "IIP" operation.
 *
 * <p> This operation provides client-side support of the Internet Imaging
 * Protocol (IIP) in both the rendered and renderable modes.  It creates
 * a <code>java.awt.image.RenderedImage</code> or a
 * <code>java.awt.image.renderable.RenderableImage</code> based on the
 * data received from the IIP server, and optionally applies a sequence
 * of operations to the created image.
 *
 * <p> The operations that may be applied and the order in which they are
 * applied are defined in section 2.2.1.1 of the Internet Imaging Protocol
 * Specification version 1.0.5.  Some or all of the requested operations
 * may be executed on the IIP server if it is determined that the server
 * supports such operations.  Any of the requested operations not supported
 * by the server will be executed on the host on which the operation chain
 * is rendered.
 *
 * <p> The processing sequence for the supplied operations is as follows:
 * <ul>
 * <li> filtering (blur or sharpen);
 * <li> tone and color correction ("color twist");
 * <li> contrast adjustment;
 * <li> selection of source rectangle of interest;
 * <li> spatial orientation (rendering-independent affine transformation);
 * <li> selection of destination rectangle of interest;
 * <li> rendering transformation (renderable mode only);
 * <li> transposition (rotation and/or mirroring).
 * </ul>
 *
 * <p> As indicated, the rendering transformation is performed only in
 * renderable mode processing.  This transformation is derived from the
 * <code>AffineTransform</code> supplied in the <code>RenderContext</code>
 * when rendering actually occurs.  Rendered mode processing creates
 * a <code>RenderedImage</code> which is the default rendering of the
 * <code>RenderableImage</code> created in renderable mode processing.
 *
 * <p> The "URL" parameter specifies the URL of the IIP image as a
 * <code>java.lang.String</code>.  It must represent a valid URL, and
 * include any required FIF or SDS commands.  It cannot be <code>null</code>.
 *
 * <p> The "subImages" parameter optionally indicates the sub-images to
 * be used by the server to get the images at each resolution level.  The
 * values in this <code>int</code> array cannot be negative.  If this
 * parameter is not specified, or if the array is too short (length is 0),
 * or if a negative value is specified, then this operation will use the
 * zeroth sub-image of the resolution level actually processed.
 *
 * <p> The "filter" parameter specifies a blur or sharpen operation: a
 * positive value indicates sharpen and a negative value blur.  A unit
 * step should produce a perceptible change in the image.  The default
 * value is 0 which signifies that no filtering will occur.
 *
 * <p> The "colorTwist" parameter represents a 4x4 matrix stored in row-major
 * order and should have an array length of at least 16.  If an array of
 * length greater than 16 is specified, all elements from index 16 and beyond
 * are ignored.  Elements 12, 13 and 14 must be 0.  This matrix will be
 * applied to the (possibly padded) data in an intermediate normalized
 * PhotoYCC color space with a premultiplied alpha channel.  This operation
 * will force an alpha channel to be added to the image if the last column
 * of the last row of the color twist matrix is not 1.0F.  Also, if the image
 * originally has a grayscale color space it will be cast up to RGB if
 * casting the data back to grayscale after applying the color twist matrix
 * would result in any loss of data.
 *
 * <p> The "contrast" parameter specifies a contrast enhancement operation
 * with increasing contrast for larger value. It must be greater than or equal
 * to 1.0F.  A value of 1.0F indicates no contrast adjustment.
 *
 * <p> The "sourceROI" parameter specifies the rectangle of interest in the
 * source image in rendering-independent coordinates.  The intersection of
 * this rectangle with the rendering-independent bounds of the source image
 * must equal itself.  The rendering-independent bounds of the source image
 * are defined to be (0.0F, 0.0F, r, 1.0F) where <i>r</i> is the aspect ratio
 * (width/height) of the source image.  Note that the source image will not
 * in fact be cropped to these limits but values outside of this rectangle
 * will be suppressed.
 *
 * <p> The "transform" parameter represents an affine backward mapping to be
 * applied in rendering-independent coordinates.  Note that the direction
 * of transformation is opposite to that of the <code>AffineTransform</code>
 * supplied in the <code>RenderContext</code> which is a forward mapping. The
 * default value of this transform is the identity mapping.  The supplied
 * <code>AffineTransform</code> must be invertible.
 *
 * <p> The "aspectRatio" parameter specifies the rendering-independent width
 * of the destination image and must be positive.  The rendering-independent
 * bounds of the destination image are (0.0F, 0.0F, aspectRatio, 1.0F).  If
 * this parameter is not provided the destination aspect ratio defaults to
 * that of the source.
 *
 * <p> The "destROI" parameter specifies the rectangle of interest in the
 * destination image in rendering-independent coordinates.  This rectangle must
 * have a non-empty intersection with the rendering-independent bounds of the
 * destination image but is not constrained to the destination image bounds.
 *
 * <p> A counterclockwise rotation may be applied to the destination image.
 * However, the angle is limited to 0, 90, 180, or 270 degrees.  By default,
 * the destination image is not rotated.
 *
 * <p> The "mirrorAxis" parameter may be <code>null</code>, in which case
 * no flipping is applied, or a <code>String</code> of "x", "X", "y", or
 * "Y".
 *
 * <p> The "ICCProfile" parameter may only be used with client-side processing
 * or with server-side processing if the connection protocol supports the
 * ability to transfer a profile.
 *
 * <p> The "JPEGQuality" and "JPEGTable" parameters are only used with
 * server-side processing.  If provided, JPEGQuality must be in the range
 * [0,100] and JPEGTable in [1,255].
 *
 * <p> There is no source image associated with this operation.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>IIP</td></tr>
 * <tr><td>LocalName</td>   <td>IIP</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Provides client support of the Internet
 *                              Imaging Protocol in the rendered and
 *                              renderable modes.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/IIPDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The URL of the IIP image.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The sub-images to be used by the server
 *                              for images at each resolution level.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The filtering value.</td></tr>
 * <tr><td>arg3Desc</td>    <td>The color twist matrix.</td></tr>
 * <tr><td>arg4Desc</td>    <td>The contrast value.</td></tr>
 * <tr><td>arg5Desc</td>    <td>The source rectangle of interest in
 *                              rendering-independent coordinates.</td></tr>
 * <tr><td>arg6Desc</td>    <td>The rendering-independent spatial orientation
 *                              transform.</td></tr>
 * <tr><td>arg7Desc</td>    <td>The aspect ratio of the destination
 *                              image.</td></tr>
 * <tr><td>arg8Desc</td>    <td>The destination rectangle of interest in
 *                              rendering-independent coordinates.</td></tr>
 * <tr><td>arg9Desc</td>    <td>The counterclockwise rotation angle to be
 *                              applied to the destination.</td></tr>
 * <tr><td>arg10Desc</td>   <td>The mirror axis.</td></tr>
 * <tr><td>arg11Desc</td>   <td>The ICC profile used to represent the color
 *                              space of the source image.</td></tr>
 * <tr><td>arg12Desc</td>   <td>The JPEG quality factor.</td></tr>
 * <tr><td>arg13Desc</td>   <td>The JPEG compression group index
 *                              number.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>        <th>Class Type</th>
 *                          <th>Default Value</th></tr>
 * <tr><td>URL</td>         <td>java.lang.String</td>
 *                          <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>subImages</td>   <td>int[]</td>
 *                          <td>{ 0 }</td>
 * <tr><td>filter</td>      <td>java.lang.Float</td>
 *                          <td>0.0F</td>
 * <tr><td>colorTwist</td>  <td>float[]</td>
 *                          <td>null</td>
 * <tr><td>contrast</td>    <td>java.lang.Float</td>
 *                          <td>1.0F</td>
 * <tr><td>sourceROI</td>   <td>java.awt.geom.Rectangle2D.Float</td>
 *                          <td>null</td>
 * <tr><td>transform</td>   <td>java.awt.geom.AffineTransform</td>
 *                          <td>identity transform</td>
 * <tr><td>aspectRatio</td> <td>java.lang.Float</td>
 *                          <td>null</td>
 * <tr><td>destROI</td>     <td>java.awt.geom.Rectangle2D.Float</td>
 *                          <td>null</td>
 * <tr><td>rotation</td>    <td>java.lang.Integer</td>
 *                          <td>0</td>
 * <tr><td>mirrorAxis</td>  <td>java.lang.String</td>
 *                          <td>null</td>
 * <tr><td>ICCProfile</td>  <td>java.awt.color.ICC_Profile</td>
 *                          <td>null</td>
 * <tr><td>JPEGQuality</td> <td>java.lang.Integer</td>
 *                          <td>null</td>
 * <tr><td>JPEGTable</td>   <td>java.lang.Integer</td>
 *                          <td>null</td>
 * </table></p>
 *
 * @see <a href="http://www.digitalimaging.org">Digital Imaging Group</a>
 * @see java.awt.image.RenderedImage
 * @see java.awt.image.renderable.RenderableImage
 * @see IIPResolutionDescriptor
 */
public class IIPDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "IIP"},
        {"LocalName",   "IIP"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("IIPDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/IIPDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("IIPDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("IIPDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("IIPDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("IIPDescriptor4")},
        {"arg4Desc",    JaiI18N.getString("IIPDescriptor5")},
        {"arg5Desc",    JaiI18N.getString("IIPDescriptor6")},
        {"arg6Desc",    JaiI18N.getString("IIPDescriptor7")},
        {"arg7Desc",    JaiI18N.getString("IIPDescriptor8")},
        {"arg8Desc",    JaiI18N.getString("IIPDescriptor9")},
        {"arg9Desc",    JaiI18N.getString("IIPDescriptor10")},
        {"arg10Desc",   JaiI18N.getString("IIPDescriptor11")},
        {"arg11Desc",   JaiI18N.getString("IIPDescriptor12")},
        {"arg12Desc",   JaiI18N.getString("IIPDescriptor13")},
        {"arg13Desc",   JaiI18N.getString("IIPDescriptor14")}
    };

    /** The parameter class types for this operation. */
    private static final Class[] paramClasses = {
        java.lang.String.class,			// arg0
        int[].class,				// arg1
        java.lang.Float.class,			// arg2
        float[].class,				// arg3
        java.lang.Float.class,			// arg4
        java.awt.geom.Rectangle2D.Float.class,	// arg5
        java.awt.geom.AffineTransform.class,	// arg6
        java.lang.Float.class,			// arg7
        java.awt.geom.Rectangle2D.Float.class,	// arg8
        java.lang.Integer.class,		// arg9
        java.lang.String.class,			// arg10
        java.awt.color.ICC_Profile.class,	// arg11
        java.lang.Integer.class,		// arg12
        java.lang.Integer.class			// arg13
    };

    /** The parameter names for this operation. */
    private static final String[] paramNames = {
        "URL",
        "subImages",
        "filter",
        "colorTwist",
        "contrast",
        "sourceROI",
        "transform",
        "aspectRatio",
        "destROI",
        "rotation",
        "mirrorAxis",
        "ICCProfile",
        "JPEGQuality",
        "JPEGTable"
    };

    /**
     * The parameter default values for this operation.  For those parameters
     * whose default value is <code>null</code>, an appropriate value is
     * chosen by the individual implementation.
     */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT,
        new int[] { 0 },
        new java.lang.Float(0.0F),
        null,
        new java.lang.Float(1.0F),
        null,
        new AffineTransform(),
        null,
        null,
        new Integer(0),
        null,
        null,
        null,
        null
    };

    /** Constructor. */
    public IIPDescriptor() {
        super(resources, 0, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Overrides super class's default implementation to return
     * <code>true</code> because this operation supports renderable mode.
     */
    public boolean isRenderableSupported() {
        return true;
    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.  If the supplied <code>index</code> does not
     * correspond to a numeric parameter, this method returns
     * <code>null</code>.
     *
     * @throws ArrayIndexOutOfBoundsException if <code>index</code> is less
     *         than 0 or greater than 13.
     */
    public Number getParamMinValue(int index) {
        if (index == 0 || index == 1 || index == 3 || index == 5 ||
            index == 6 || index == 8 || index == 10 || index == 11) {
            return null;
        } else if (index == 2) {
            return new java.lang.Float(-java.lang.Float.MAX_VALUE);
        } else if(index == 7) {
            return new java.lang.Float(0.0F);
        } else if (index == 4) {
            return new java.lang.Float(1.0F);
        } else if (index == 12 || index == 9) {
            return new Integer(0);
        } else if (index == 13) {
            return new Integer(1);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Returns the maximum legal value of a specified numeric parameter
     * for this operation.  If the supplied <code>index</code> does not
     * correspond to a numeric parameter, this method returns
     * <code>null</code>.
     *
     * @throws ArrayIndexOutOfBoundsException if <code>index</code> is less
     *         than 0 or greater than 13.
     */
    public Number getParamMaxValue(int index) {
        if (index == 0 || index == 1 || index == 3 || index == 5 ||
            index == 6 || index == 8 || index == 10 || index == 11) {
            return null;
        } else if (index == 2 || index == 4 || index == 7) {
            return new java.lang.Float(java.lang.Float.MAX_VALUE);
        } else if (index == 9) {
            return new Integer(270);
        } else if (index == 12) {
            return new Integer(100);
        } else if (index == 13) {
            return new Integer(255);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Validates the input parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that:
     * <ul>
     * <li> the supplied URL string specifies a valid protocol;
     * <li> the color twist, if not <code>null</code>, has an array
     *      length of at least 16 (all elements from index 16 and beyond are
     *      ignored and elements 12, 13, and 14 are set to 0);
     * <li> both the source and dest ROI, if not <code>null</code>, has
     *      a width and height greater than 0;
     * <li> the mirror axis, if not <code>null</code>, has a
     *      <code>String</code> of "x", "X", "y", or "Y";
     * <li> the destination rotation is one of the valid degrees
     *      (0, 90. 180, 270).
     * </ul>
     */
    protected boolean validateParameters(ParameterBlock args,
                                         StringBuffer msg) {
        if (!super.validateParameters(args, msg)) {
            return false;
        }

        try {
            new URL((String)args.getObjectParameter(0));
        } catch (Exception e) {
            msg.append(getName() + " " +
                       JaiI18N.getString("IIPDescriptor15"));
            return false;
        }

        int[] subImages = (int[]) args.getObjectParameter(1);
        if (subImages.length < 1) {
            args.set(paramDefaults[1], 1);
        }

        float[] colorTwist = (float[])args.getObjectParameter(3);
        if (colorTwist != null) {
            if (colorTwist.length < 16) {
                msg.append(getName() + " " +
                           JaiI18N.getString("IIPDescriptor16"));
                return false;
            }

            /* Make sure elements 12, 13, and 14 are 0. */
            colorTwist[12] = 0;
            colorTwist[13] = 0;
            colorTwist[14] = 0;
            args.set(colorTwist, 3);
        }

        float contrast = args.getFloatParameter(4);
        if(contrast < 1.0F) {
            msg.append(getName() + " " +
                       JaiI18N.getString("IIPDescriptor20"));
            return false;
        }

        java.awt.geom.Rectangle2D.Float sourceROI =
            (java.awt.geom.Rectangle2D.Float)args.getObjectParameter(5);
        if (sourceROI != null &&
            (sourceROI.getWidth() < 0.0 || sourceROI.getHeight() < 0.0)) {
            msg.append(getName() + " " +
                       JaiI18N.getString("IIPDescriptor17"));
            return false;
        }

        AffineTransform tf = (AffineTransform)args.getObjectParameter(6);
        if(tf.getDeterminant() == 0.0) {
            msg.append(getName() + " " +
                       JaiI18N.getString("IIPDescriptor24"));
            return false;
        }

        if(args.getObjectParameter(7) != null) {
            float aspectRatio = args.getFloatParameter(7);
            if(aspectRatio < 0.0F) {
                msg.append(getName() + " " +
                           JaiI18N.getString("IIPDescriptor21"));
                return false;
            }
        }

        java.awt.geom.Rectangle2D.Float destROI =
            (java.awt.geom.Rectangle2D.Float)args.getObjectParameter(8);
        if (destROI != null &&
            (destROI.getWidth() < 0.0 || destROI.getHeight() < 0.0)) {
            msg.append(getName() + " " +
                       JaiI18N.getString("IIPDescriptor17"));
            return false;
        }

        int rotation = args.getIntParameter(9);
        if (rotation != 0 && rotation != 90 &&
            rotation != 180 && rotation != 270) {
            msg.append(getName() + " " +
                       JaiI18N.getString("IIPDescriptor18"));
            return false;
        }

        String mirrorAxis = (String)args.getObjectParameter(10);
        if (mirrorAxis != null &&
            !mirrorAxis.equalsIgnoreCase("x") &&
            !mirrorAxis.equalsIgnoreCase("y")) {
            msg.append(getName() + " " +
                       JaiI18N.getString("IIPDescriptor19"));
            return false;
        }

        if(args.getObjectParameter(12) != null) {
            int JPEGQuality = args.getIntParameter(12);
            if(JPEGQuality < 0 || JPEGQuality > 100) {
                msg.append(getName() + " " +
                           JaiI18N.getString("IIPDescriptor22"));
                return false;
            }
        }

        if(args.getObjectParameter(13) != null) {
            int JPEGIndex = args.getIntParameter(13);
            if(JPEGIndex < 1 || JPEGIndex > 255) {
                msg.append(getName() + " " +
                           JaiI18N.getString("IIPDescriptor23"));
                return false;
            }
        }

        return true;
    }


    /**
     * Provides client support of the Internet Imaging Protocol in the rendered and renderable mode.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param URL The URL of the IIP image.
     * @param subImages The sub-images to be used by the server for images at each resolution level.
     * May be <code>null</code>.
     * @param filter The filtering value.
     * May be <code>null</code>.
     * @param colorTwist The color twist matrix.
     * May be <code>null</code>.
     * @param contrast The contrast value.
     * May be <code>null</code>.
     * @param sourceROI The source rectangle of interest in rendering-independent coordinates.
     * May be <code>null</code>.
     * @param transform The rendering-independent spatial orientation transform.
     * May be <code>null</code>.
     * @param aspectRatio The aspect ratio of the destination image.
     * May be <code>null</code>.
     * @param destROI The destination rectangle of interest in rendering-independent coordinates.
     * May be <code>null</code>.
     * @param rotation The counterclockwise rotation angle to be applied to the destination.
     * May be <code>null</code>.
     * @param mirrorAxis The mirror axis.
     * May be <code>null</code>.
     * @param ICCProfile The ICC profile used to represent the color space of the source image.
     * May be <code>null</code>.
     * @param JPEGQuality The JPEG quality factor.
     * May be <code>null</code>.
     * @param JPEGTable The JPEG compression group index number.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>URL</code> is <code>null</code>.
     */
    public static RenderedOp create(String URL,
                                    int[] subImages,
                                    Float filter,
                                    float[] colorTwist,
                                    Float contrast,
                                    Rectangle2D.Float sourceROI,
                                    AffineTransform transform,
                                    Float aspectRatio,
                                    Rectangle2D.Float destROI,
                                    Integer rotation,
                                    String mirrorAxis,
                                    ICC_Profile ICCProfile,
                                    Integer JPEGQuality,
                                    Integer JPEGTable,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("IIP",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setParameter("URL", URL);
        pb.setParameter("subImages", subImages);
        pb.setParameter("filter", filter);
        pb.setParameter("colorTwist", colorTwist);
        pb.setParameter("contrast", contrast);
        pb.setParameter("sourceROI", sourceROI);
        pb.setParameter("transform", transform);
        pb.setParameter("aspectRatio", aspectRatio);
        pb.setParameter("destROI", destROI);
        pb.setParameter("rotation", rotation);
        pb.setParameter("mirrorAxis", mirrorAxis);
        pb.setParameter("ICCProfile", ICCProfile);
        pb.setParameter("JPEGQuality", JPEGQuality);
        pb.setParameter("JPEGTable", JPEGTable);

        return JAI.create("IIP", pb, hints);
    }

    /**
     * Provides client support of the Internet Imaging Protocol in the rendered and renderable mode.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     *
     * @param URL The URL of the IIP image.
     * @param subImages The sub-images to be used by the server for images at each resolution level.
     * May be <code>null</code>.
     * @param filter The filtering value.
     * May be <code>null</code>.
     * @param colorTwist The color twist matrix.
     * May be <code>null</code>.
     * @param contrast The contrast value.
     * May be <code>null</code>.
     * @param sourceROI The source rectangle of interest in rendering-independent coordinates.
     * May be <code>null</code>.
     * @param transform The rendering-independent spatial orientation transform.
     * May be <code>null</code>.
     * @param aspectRatio The aspect ratio of the destination image.
     * May be <code>null</code>.
     * @param destROI The destination rectangle of interest in rendering-independent coordinates.
     * May be <code>null</code>.
     * @param rotation The counterclockwise rotation angle to be applied to the destination.
     * May be <code>null</code>.
     * @param mirrorAxis The mirror axis.
     * May be <code>null</code>.
     * @param ICCProfile The ICC profile used to represent the color space of the source image.
     * May be <code>null</code>.
     * @param JPEGQuality The JPEG quality factor.
     * May be <code>null</code>.
     * @param JPEGTable The JPEG compression group index number.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>URL</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(String URL,
                                                int[] subImages,
                                                Float filter,
                                                float[] colorTwist,
                                                Float contrast,
                                                Rectangle2D.Float sourceROI,
                                                AffineTransform transform,
                                                Float aspectRatio,
                                                Rectangle2D.Float destROI,
                                                Integer rotation,
                                                String mirrorAxis,
                                                ICC_Profile ICCProfile,
                                                Integer JPEGQuality,
                                                Integer JPEGTable,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("IIP",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setParameter("URL", URL);
        pb.setParameter("subImages", subImages);
        pb.setParameter("filter", filter);
        pb.setParameter("colorTwist", colorTwist);
        pb.setParameter("contrast", contrast);
        pb.setParameter("sourceROI", sourceROI);
        pb.setParameter("transform", transform);
        pb.setParameter("aspectRatio", aspectRatio);
        pb.setParameter("destROI", destROI);
        pb.setParameter("rotation", rotation);
        pb.setParameter("mirrorAxis", mirrorAxis);
        pb.setParameter("ICCProfile", ICCProfile);
        pb.setParameter("JPEGQuality", JPEGQuality);
        pb.setParameter("JPEGTable", JPEGTable);

        return JAI.createRenderable("IIP", pb, hints);
    }
}
