/*
 * $RCSfile: IIPResolutionDescriptor.java,v $
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
import java.awt.image.renderable.ParameterBlock;
import java.net.URL;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "IIPResolution"
 * operation.
 *
 * <p> This operation provides client-side support of the Internet Imaging
 * Protocol (IIP) in the rendered mode.  It is resolution-specific.  It
 * requests from the IIP server an image at a particular resolution level,
 * and creates a <code>java.awt.image.RenderedImage</code> based on the
 * data received from the server.  Once the <code>RenderedImage</code>
 * is created, the resolution level cannot be changed.
 *
 * <p> The layout of the created <code>RenderedImage</code> is set as
 * follows:
 * <ul>
 * <li> <code>minX</code>, <code>minY</code>, <code>tileGridXOffset</code>,
 *      and <code>tileGridYOffset</code> are set to 0;
 * <li> <code>width</code> and <code>height</code> are determined based
 *      on the specified resolution level;
 * <li> <code>tileWidth</code> and <code>tileHeight</code> are set to 64;
 * <li> <code>sampleModel</code> is of the type
 *      <code>java.awt.image.PixelInterleavedSampleModel</code> with byte
 *      data type and the appropriate number of bands;
 * <li> <code>colorModel</code> is of the type
 *      <code>java.awt.image.ComponentColorModel</code>, with the
 *      <code>ColorSpace</code> set to sRGB, PhotoYCC, or Grayscale, depending
 *      on the color space of the remote image; if an alpha channel is
 *      present, it will be premultiplied.
 * </ul>
 *
 * <p> The "URL" parameter specifies the URL of the IIP image as a
 * <code>java.lang.String</code>.  It must represent a valid URL, and
 * include any required FIF or SDS commands.  It cannot be <code>null</code>.
 *
 * <p> The "resolution" parameter specifies the resolution level of the
 * requested IIP image from the server.  The lowest resolution level is
 * 0, with larger integers representing higher resolution levels.  If the
 * requested resolution level does not exist, the nearest resolution level
 * is used.  If this parameter is not specified, it is set to the default
 * value <code>IIPResolutionDescriptor.MAX_RESOLUTION</code> which indicates
 * the highest resolution level.
 *
 * <p> The "subImage" parameter indicates the sub-image to be used by the
 * server to get the image at the specified resolution level.  This parameter
 * cannot be negative.  If this parameter is not specified, it is set to
 * the default value 0.
 *
 * <p> There is no source image associated with this operation.
 *
 * <p> If available from the IIP server certain properties may be set on the
 * <code>RenderedImage</code>.  The names of properties and the class types
 * of their associated values are listed in the following table.
 *
 * <p><table border=1>
 * <caption>Property List</caption>
 * <tr><th>Property Name</th>     <th>Property Value Class Type</th>
 * <tr><td>affine-transform</td>  <td>java.awt.geom.AffineTransform</td>
 * <tr><td>app-name</td>          <td>java.lang.String</td>
 * <tr><td>aspect-ratio</td>      <td>java.lang.Float</td>
 * <tr><td>author</td>            <td>java.lang.String</td>
 * <tr><td>colorspace</td>        <td>int[]</td>
 * <tr><td>color-twist</td>       <td>float[16]</td>
 * <tr><td>comment</td>           <td>java.lang.String</td>
 * <tr><td>contrast-adjust</td>   <td>java.lang.Float</td>
 * <tr><td>copyright</td>         <td>java.lang.String</td>
 * <tr><td>create-dtm</td>        <td>java.lang.String</td>
 * <tr><td>edit-time</td>         <td>java.lang.String</td>
 * <tr><td>filtering-value</td>   <td>java.lang.Float</td>
 * <tr><td>iip</td>               <td>java.lang.String</td>
 * <tr><td>iip-server</td>        <td>java.lang.String</td>
 * <tr><td>keywords</td>          <td>java.lang.String</td>
 * <tr><td>last-author</td>       <td>java.lang.String</td>
 * <tr><td>last-printed</td>      <td>java.lang.String</td>
 * <tr><td>last-save-dtm</td>     <td>java.lang.String</td>
 * <tr><td>max-size</td>          <td>int[2]</td>
 * <tr><td>resolution-number</td> <td>java.lang.Integer</td>
 * <tr><td>rev-number</td>        <td>java.lang.String</td>
 * <tr><td>roi-iip</td>           <td>java.awt.geom.Rectangle2D.Float</td>
 * <tr><td>subject</td>           <td>java.lang.String</td>
 * <tr><td>title</td>             <td>java.lang.String</td>
 * </table></p>
 *
 * For information on the significance of each of the above properties please
 * refer to the IIP specification.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>IIPResolution</td></tr>
 * <tr><td>LocalName</td>   <td>IIPResolution</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Provides client-side support of the Internet
 *                              Imaging Protocol in the rendered mode.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/IIPResolutionDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The URL of the IIP image.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The resolution level to request.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The sub-image to be used by the
 *                              server.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>       <th>Class Type</th>
 *                         <th>Default Value</th></tr>
 * <tr><td>URL</td>        <td>java.lang.String</td>
 *                         <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>resolution</td> <td>java.lang.Integer</td>
 *                         <td>IIPResolutionDescriptor.MAX_RESOLUTION</td>
 * <tr><td>subImage</td>   <td>java.lang.Integer</td>
 *                         <td>0</td>
 * </table></p>
 *
 * @see <a href="http://www.digitalimaging.org">Digital Imaging Group</a>
 * @see java.awt.image.RenderedImage
 * @see IIPDescriptor
 */
public class IIPResolutionDescriptor extends OperationDescriptorImpl {

    /** Convenience name for Max Resolution of an image on an IIP server. */
    public static final Integer MAX_RESOLUTION = new Integer(Integer.MAX_VALUE);

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "IIPResolution"},
        {"LocalName",   "IIPResolution"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("IIPResolutionDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/IIPResolutionDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("IIPResolutionDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("IIPResolutionDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("IIPResolutionDescriptor3")}
    };

    /** The parameter class types for this operation. */
    private static final Class[] paramClasses = {
        java.lang.String.class,
        java.lang.Integer.class,
        java.lang.Integer.class
    };

    /** The parameter names for this operation. */
    private static final String[] paramNames = {
        "URL",
        "resolution",
        "subImage"
    };

    /** The parameter default values for this operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT,
        MAX_RESOLUTION,
        new Integer(0)
    };

    /** Constructor. */
    public IIPResolutionDescriptor() {
        super(resources, 0, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.  If the supplied <code>index</code> does not
     * correspond to a numeric parameter, this method returns
     * <code>null</code>.
     *
     * @return An <code>Integer</code> of value 0 if <code>index</code>
     *         is 1 or 2, or <code>null</code> if <code>index</code> is 0.
     *
     * @throws ArrayIndexOutOfBoundsException if <code>index</code> is less
     *         than 0 or greater than 2.
     */
    public Number getParamMinValue(int index) {
        if (index == 0) {
            return null;
        } else if (index == 1 || index == 2) {
            return new Integer(0);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Validates the input parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the supplied URL
     * string specifies a valid protocol.
     */
    protected boolean validateParameters(ParameterBlock args,
                                         StringBuffer msg) {
        if (!super.validateParameters(args, msg)) {
            return false;
        }

        try {
            new URL((String)args.getObjectParameter(0));
        } catch (Exception e) {
            /* Use the same error message as IIPDescriptor. */
            msg.append(getName() + " " +
                       JaiI18N.getString("IIPDescriptor15"));
            return false;
        }

        return true;
    }


    /**
     * Provides client support of the Internet Imaging Protocol in the rendered mode.
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
     * @param resolution The resolution level to request.
     * May be <code>null</code>.
     * @param subImage The sub-image to be used by the server.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>URL</code> is <code>null</code>.
     */
    public static RenderedOp create(String URL,
                                    Integer resolution,
                                    Integer subImage,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("IIPResolution",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setParameter("URL", URL);
        pb.setParameter("resolution", resolution);
        pb.setParameter("subImage", subImage);

        return JAI.create("IIPResolution", pb, hints);
    }
}
