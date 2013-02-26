/*
 * $RCSfile: FPXDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:35 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;
import com.lightcrafts.media.jai.codec.FPXDecodeParam;
import com.lightcrafts.media.jai.codec.SeekableStream;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "FPX" operation.
 *
 * The "FPX" operation reads an image from a FlashPix stream.
 *
 * <p> The second parameter contains an instance of
 * <code>FPXDecodeParam</code> to be used during the decoding.
 * It may be set to <code>null</code> in order to perform default
 * decoding, or equivalently may be omitted.
 *
 * <p><b> The classes in the <code>com.lightcrafts.media.jai.codec</code>
 * package are not a committed part of the JAI API.  Future releases
 * of JAI will make use of new classes in their place.  This
 * class will change accordingly.</b>
 * 
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>FPX</td></tr>
 * <tr><td>LocalName</td>   <td>FPX</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Reads an image from a FlashPix stream.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/FPXDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The SeekableStream to read from.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The FPXDecodeParam to use.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>stream</td>        <td>com.lightcrafts.media.jai.codec.SeekableStream</td>
 *                            <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>param</td>         <td>com.lightcrafts.media.jai.codec.FPXDecodeParam</td>
 *                            <td>null</td>
 * </table></p>
 *
 * @see com.lightcrafts.media.jai.codec.SeekableStream
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class FPXDescriptor extends OperationDescriptorImpl {

    /** Convenience name for the Max Resolution of an FPX image */
    public static final Integer MAX_RESOLUTION = new Integer(-1);

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "FPX" operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "FPX"},
        {"LocalName",   "FPX"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("FPXDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/FPXDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("FPXDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("FPXDescriptor2")}
    };

    /** The parameter names for the "FPX" operation. */
    private static final String[] paramNames = {
        "stream", "param"
    };

    /** The parameter class types for the "FPX" operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.media.jai.codec.SeekableStream.class,
        com.lightcrafts.media.jai.codec.FPXDecodeParam.class
    };

    /** The parameter default values for the "FPX" operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT, null
    };

    /** Constructor. */
    public FPXDescriptor() {
        super(resources, 0, paramClasses, paramNames, paramDefaults);
    }


    /**
     * Reads an image from a FlashPix stream.
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
     * @param param The FPXDecodeParam to use.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>stream</code> is <code>null</code>.
     */
    public static RenderedOp create(SeekableStream stream,
                                    FPXDecodeParam param,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("FPX",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setParameter("stream", stream);
        pb.setParameter("param", param);

        return JAI.create("FPX", pb, hints);
    }
}
