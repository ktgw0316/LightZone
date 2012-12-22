/*
 * $RCSfile: PNGDescriptor.java,v $
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
import com.lightcrafts.media.jai.codec.PNGDecodeParam;
import com.lightcrafts.media.jai.codec.SeekableStream;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "PNG" operation.
 *
 * <p> The "PNG" operation reads a standard PNG version 1.1 input stream.
 * The PNG (Portable Network Graphics) specification may be
 * found at <a href="http://www.cdrom.com/pub/png/spec">
 * <code>http://www.cdrom.com/pub/png/spec</code></a>.
 *
 * <p> The "PNG" operation implements the entire PNG specification,
 * but provides access only to the final, high-resolution version of
 * interlaced images.
 *
 * <p> The second parameter contains an instance of
 * <code>PNGDecodeParam</code> to be used during the decoding.
 * It may be set to <code>null</code> in order to perform default
 * decoding, or equivalently may be omitted.
 *
 * <p> The documentation for <code>PNGDecodeParam</code> describes the
 * possible output formats of PNG images after decoding.
 *
 * <p><b> The classes in the <code>com.lightcrafts.media.jai.codec</code>
 * package are not a committed part of the JAI API.  Future releases
 * of JAI will make use of new classes in their place.  This
 * class will change accordingly.</b>
 * 
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>PNG</td></tr>
 * <tr><td>LocalName</td>   <td>PNG</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Reads an image from a PNG stream.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PNGDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The SeekableStream to read from.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The PNGDecodeParam to use.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>    <th>Class Type</th>
 *                      <th>Default Value</th></tr>
 * <tr><td>stream</td>  <td>com.lightcrafts.media.jai.codec.SeekableStream</td>
 *                      <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>param</td>   <td>com.lightcrafts.media.jai.codec.PNGDecodeParam</td>
 *                      <td>null</td>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Properties</caption>
 * <tr><th>Property Name</th> <th>Class</th> <th>Comment</th></tr>
 *
 * <tr><td>file_type</td>          <td>String</td>
 * <td>"PNG v. 1.0"</td> </tr>
 * <tr><td>background_color</td>   <td>java.awt.Color</td>
 * <td>The suggested background color.</td> </tr>
 * <tr><td>significant_bits</td>   <td>int[]</td>
 * <td>The number of significant bits stored in the file.</td> </tr>
 * <tr><td>bit_depth</td>          <td>Integer</td>
 * <td>The bit depth of the file</td> </tr>
 * <tr><td>color_type</td>         <td>String</td>
 * <td>One of "Grayscale", "Truecolor", "Index",
 *     "Grayscale with alpha" or "Truecolor with alpha"</td> </tr>
 * <tr><td>interlace_method</td>   <td>String</td>
 * <td>"None" or "Adam7"</td> </tr>
 * <tr><td>white_point_x</td>      <td>Float</td>
 * <td>The CIE X coordinate of the white point, if known.</td> </tr>
 * <tr><td>white_point_y</td>      <td>Float</td>
 * <td>The CIE Y coordinate of the white point, if known.</td> </tr>
 * <tr><td>red_x</td>              <td>Float</td>
 * <td>The CIE X coordinate of the red primary, if known.</td> </tr>
 * <tr><td>red_y</td>              <td>Float</td>
 * <td>The CIE Y coordinate of the red primary, if known.</td> </tr>
 * <tr><td>green_x</td>            <td>Float</td>
 * <td>The CIE X coordinate of the green primary, if known.</td> </tr>
 * <tr><td>green_y</td>            <td>Float</td>
 * <td>The CIE Y coordinate of the green primary, if known.</td> </tr>
 * <tr><td>blue_x</td>             <td>Float</td>  
 * <td>The CIE X coordinate of the blue primary, if known.</td> </tr>
 * <tr><td>blue_y</td>             <td>Float</td>
 * <td>The CIE Y coordinate of the blue primary, if known.</td> </tr>
 * <tr><td>gamma</td>              <td>Float</td>
 * <td>The image gamma, if known.</td> </tr>
 * <tr><td>x_pixels_per_unit</td>  <td>Integer</td>
 * <td>The number of horizontal pixels per unit.</td> </tr>
 * <tr><td>y_pixels_per_unit</td>  <td>Integer</td>
 * <td>The number of vertical pixels per unit.</td> </tr>
 * <tr><td>pixel_aspect_ratio</td> <td>Float</td>
 * <td>The width of a pixel divided by its height.</td> </tr>
 * <tr><td>pixel_units</td>        <td>String</td>
 * <td>"Meters" or <code>null</code></td> </tr>
 * <tr><td>timestamp</td>          <td>java.util.Date</td>
 * <td>The creation or modification time of the image.</td> </tr>
 * <tr><td>text:*</td>             <td>String</td>
 * <td>The value of a tEXt chunk.</td> </tr>
 * <tr><td>ztext:*</td>             <td>String</td>
 * <td>The value of a zTXt chunk (not yet implemented).</td> </tr>
 * <tr><td>chunk:*</td>            <td>byte[]</td>
 * <td>The contents of any non-standard chunks.</td> </tr>
 * </table></p>
 *
 * @see com.lightcrafts.media.jai.codec.PNGDecodeParam
 * @see com.lightcrafts.media.jai.codec.SeekableStream
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class PNGDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "PNG" operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "PNG"},
        {"LocalName",   "PNG"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("PNGDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/PNGDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("PNGDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("PNGDescriptor2")},
    };

    /** The parameter names for the "PNG" operation. */
    private static final String[] paramNames = {
        "stream", "param"
    };

    /** The parameter class types for the "PNG" operation. */
    private static final Class[] paramClasses = {
	com.lightcrafts.media.jai.codec.SeekableStream.class,
        com.lightcrafts.media.jai.codec.PNGDecodeParam.class
    };

    /** The parameter default values for the "PNG" operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT, null
    };

    /** Constructor. */
    public PNGDescriptor() {
        super(resources, 0, paramClasses, paramNames, paramDefaults);
    }


    /**
     * Reads a standard JFIF (PNG) file.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param stream The SeekableStream to read from.
     * @param param The PNGDecodeParam to use.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>stream</code> is <code>null</code>.
     */
    public static RenderedOp create(SeekableStream stream,
                                    PNGDecodeParam param,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("PNG",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setParameter("stream", stream);
        pb.setParameter("param", param);

        return JAI.create("PNG", pb, hints);
    }
}
