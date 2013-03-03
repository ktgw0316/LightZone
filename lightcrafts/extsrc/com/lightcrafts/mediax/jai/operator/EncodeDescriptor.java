/*
 * $RCSfile: EncodeDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:34 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import com.lightcrafts.media.jai.codec.ImageCodec;
import com.lightcrafts.media.jai.codec.ImageEncodeParam;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.OutputStream;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Encode" operation.
 *
 * The "Encode" operation writes an image to a given <code>OutputStream</code>
 * in a specified format using the supplied encoding parameters.
 *
 * <p> The third parameter contains an instance of
 * <code>ImageEncodeParam</code> to be used during the decoding.  It
 * may be set to <code>null</code> in order to perform default
 * encoding, or equivalently may be omitted.  If
 * non-<code>null</code>, it must be of the correct class type for the
 * selected format.
 *
 * <p><b> The classes in the <code>com.lightcrafts.media.jai.codec</code>
 * package are not a committed part of the JAI API.  Future releases
 * of JAI will make use of new classes in their place.  This
 * class will change accordingly.</b>
 * 
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>encode</td></tr>
 * <tr><td>LocalName</td>   <td>encode</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Stores an image to an OutputStream.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/EncodeDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The OutputStream to write to.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The format of the created file.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The encoding parameters.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>stream</td>        <td>java.io.OutputStream</td>
 *                            <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>format</td>        <td>java.lang.String</td>
 *                            <td>"tiff"</td>
 * <tr><td>param</td>         <td>com.lightcrafts.media.jai.codec.ImageEncodeParam</td>
 *                            <td>null</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class EncodeDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "Encode" operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Encode"},
        {"LocalName",   "Encode"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("EncodeDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/EncodeDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("EncodeDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("EncodeDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("EncodeDescriptor3")}
    };

    /** The parameter names for the "Encode" operation. */
    private static final String[] paramNames = {
        "stream", "format", "param"
    };

    /** The parameter class types for the "Encode" operation. */
    private static final Class[] paramClasses = {
        java.io.OutputStream.class,
        java.lang.String.class,
        com.lightcrafts.media.jai.codec.ImageEncodeParam.class
    };

    /** The parameter default values for the "Encode" operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT, "tiff", null
    };

    private static final String[] supportedModes = {
	"rendered"
    };

    /** Constructor. */
    public EncodeDescriptor() {
        super(resources, supportedModes, 1,
		paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input source and parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the format name is
     * recognized and is capable of encoding the source image using
     * the encoding parameter "param", if non-<code>null</code>.
     */
    public boolean validateArguments(String modeName,
				     ParameterBlock args,
                                     StringBuffer msg) {

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

        // Fool the superclass method if length < 3
        if (args.getNumParameters() < 3) {
            args = (ParameterBlock)args.clone();
            args.set(null, 2);
        }

        if (!super.validateArguments(modeName, args, msg)) {
            return false;
        }

        // Retrieve the format.
        String format = (String)args.getObjectParameter(1);

        // Retrieve the associated ImageCodec.
        ImageCodec codec = ImageCodec.getCodec(format);

        // Check for null codec.
        if (codec == null) {
            msg.append(getName() + " " +
                       JaiI18N.getString("EncodeDescriptor4"));
            return false;
        }

        // Retrieve the ImageEncodeParam object.
        ImageEncodeParam param =
            (ImageEncodeParam)args.getObjectParameter(2);

	RenderedImage src = args.getRenderedSource(0);

        // Verify that the image can be encoded with the given parameters.
        if (!codec.canEncodeImage(src, param)) {
            msg.append(getName() + " " +
                       JaiI18N.getString("EncodeDescriptor5"));
            return false;
        }

        return true;
    }

    /**
     * Returns true indicating that the operation should be rendered
     * immediately during a call to <code>JAI.create()</code>.
     *
     * @see com.lightcrafts.mediax.jai.OperationDescriptor
     */
    public boolean isImmediate() {
        return true;
    }


    /**
     * Stores an image to an OutputStream.
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
     * @param stream The OutputStream to write to.
     * @param format The format of the created file.
     * May be <code>null</code>.
     * @param param The encoding parameters.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>stream</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    OutputStream stream,
                                    String format,
                                    ImageEncodeParam param,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Encode",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("stream", stream);
        pb.setParameter("format", format);
        pb.setParameter("param", param);

        return JAI.create("Encode", pb, hints);
    }
}
