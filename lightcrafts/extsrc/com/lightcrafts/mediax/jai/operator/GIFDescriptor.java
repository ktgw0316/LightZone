/*
 * $RCSfile: GIFDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:36 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import com.lightcrafts.media.jai.codec.SeekableStream;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "GIF" operation.
 *
 * <p> The "GIF" operation reads an image from a GIF stream.
 *
 * <p><b> The classes in the <code>com.lightcrafts.media.jai.codec</code>
 * package are not a committed part of the JAI API.  Future releases
 * of JAI will make use of new classes in their place.  This
 * class will change accordingly.</b>
 * 
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>GIF</td></tr>
 * <tr><td>LocalName</td>   <td>GIF</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Reads an image from a GIF stream.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/GIFDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The SeekableStream to read from.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>    <th>Class Type</th>
 *                      <th>Default Value</th></tr>
 * <tr><td>stream</td>  <td>com.lightcrafts.media.jai.codec.SeekableStream</td>
 *                      <td>NO_PARAMETER_DEFAULT</td>
 * </table></p>
 *
 * @see com.lightcrafts.media.jai.codec.SeekableStream
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class GIFDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "GIF" operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "GIF"},
        {"LocalName",   "GIF"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("GIFDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/GIFDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("GIFDescriptor1")},
    };

    /** The parameter names for the "GIF" operation. */
    private static final String[] paramNames = {
        "stream"
    };

    /** The parameter class types for the "GIF" operation. */
    private static final Class[] paramClasses = {
	com.lightcrafts.media.jai.codec.SeekableStream.class
    };

    /** The parameter default values for the "GIF" operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT
    };

    /** Constructor. */
    public GIFDescriptor() {
        super(resources, 0, paramClasses, paramNames, paramDefaults);
    }


    /**
     * Reads an image from a GIF stream.
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
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>stream</code> is <code>null</code>.
     */
    public static RenderedOp create(SeekableStream stream,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("GIF",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setParameter("stream", stream);

        return JAI.create("GIF", pb, hints);
    }
}
