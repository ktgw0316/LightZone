/*
 * $RCSfile: FileStoreDescriptor.java,v $
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

import com.lightcrafts.media.jai.codec.ImageCodec;
import com.lightcrafts.media.jai.codec.ImageEncodeParam;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "FileStore" operation.
 *
 * The "FileStore" operation writes an image to a given file in a specified
 * format using the supplied encoding parameters.
 *
 * <p> In the default instance the <code>validateParameters()</code> method
 * checks for the named file to be writable if it already exists, else that
 * it can be created. If not, it will return <code>false</code>, causing 
 * <code>JAI.createNS()</code> to throw an
 * <code>IllegalArgumentException</code>.
 *
 * <p> In special cases such as an image being written to a remote system, 
 * the above check for existence of a file on the local system should be
 * bypassed. This can be accomplished by setting the <code>Boolean</code>
 * variable <code>checkFileLocally</code> to <code>FALSE</code> in the
 * <code>ParameterBlock</code>.

 * <p> The third parameter contains an instance of
 * <code>ImageEncodeParam</code> to be used during the decoding.  It
 * may be set to <code>null</code> in order to perform default
 * encoding, or equivalently may be omitted.  If
 * non-<code>null</code>, it must be of the correct class type for the
 * selected format.
 *
 * <p> The requested file path must be writable.
 *
 * <p><b> The classes in the <code>com.lightcrafts.media.jai.codec</code>
 * package are not a committed part of the JAI API.  Future releases
 * of JAI will make use of new classes in their place.  This
 * class will change accordingly.</b>
 * 
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>filestore</td></tr>
 * <tr><td>LocalName</td>   <td>filestore</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Stores an image to a file.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/FileStoreDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The path of the file to write to.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The format of the file.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The encoding parameters.</td></tr>
 * <tr><td>arg3Desc</td>    <td>Boolean specifying whether check for file creation / writing locally should be done.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>filename</td>      <td>java.lang.String</td>
 *                            <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>format</td>        <td>java.lang.String</td>
 *                            <td>"tiff"</td>
 * <tr><td>param</td>         <td>com.lightcrafts.media.jai.codec.ImageEncodeParam</td>
 *                            <td>null</td>
 * <tr><td>checkFileLocally</td> <td>java.lang.Boolean</td>
 *                            <td>TRUE</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class FileStoreDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and
     * specify the parameter list for the "FileStore" operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "FileStore"},
        {"LocalName",   "FileStore"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("FileStoreDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/FileStoreDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("FileStoreDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("FileStoreDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("FileStoreDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("FileStoreDescriptor11")}
    };

    /** The parameter names for the "FileStore" operation. */
    private static final String[] paramNames = {
        "filename", "format", "param", "checkFileLocally"
    };

    /** The parameter class types for the "FileStore" operation. */
    private static final Class[] paramClasses = {
        java.lang.String.class,
        java.lang.String.class,
        com.lightcrafts.media.jai.codec.ImageEncodeParam.class,
	java.lang.Boolean.class
    };

    /** The parameter default values for the "FileStore" operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT, "tiff", null, Boolean.TRUE
    };

    private static final String[] supportedModes = {
	"rendered"
    };

    /** Constructor. */
    public FileStoreDescriptor() {
        super(resources, supportedModes, 1,
	      paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input source and parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the format name is
     * recognized and is capable of encoding the source image using
     * the encoding parameter "param", if non-<code>null</code>,
     * ans that the output file path "filename" is writable.
     */
    public boolean validateArguments(String modeName,
				     ParameterBlock args,
                                     StringBuffer msg) {
        if (!super.validateArguments(modeName, args, msg)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

        // Retrieve the format.
        String format = (String)args.getObjectParameter(1);

        // Retrieve the associated ImageCodec.
        ImageCodec codec = ImageCodec.getCodec(format);

        // Check for null codec.
        if (codec == null) {
            msg.append(getName() + " " +
                       JaiI18N.getString("FileStoreDescriptor4"));
            return false;
        }

        // Retrieve the ImageEncodeParam object.
        ImageEncodeParam param =
            (ImageEncodeParam)args.getObjectParameter(2);

	RenderedImage src = args.getRenderedSource(0);

        // Verify that the image can be encoded with null parameters.
        if (!codec.canEncodeImage(src, param)) {
            msg.append(getName() + " " +
                       JaiI18N.getString("FileStoreDescriptor5"));
            return false;
        }

        // Retrieve the file path.
        String pathName = (String)args.getObjectParameter(0);
        if (pathName == null) {
            msg.append(getName() + " " +
                       JaiI18N.getString("FileStoreDescriptor6"));
            return false;
        }

        // Perform non-destructive test that the file
        // may be created and written.
	Boolean checkFile = (Boolean)args.getObjectParameter(3);
	if (checkFile.booleanValue()) {
	    try {
		File f = new File(pathName);
		if (f.exists()) {
		    if (!f.canWrite()) {
			// Cannot write to existing file.
			msg.append(getName() + " " +
				   JaiI18N.getString("FileStoreDescriptor7"));
			return false;
		    }
		} else {
		    if (!f.createNewFile()) {
			// Cannot create file.
			msg.append(getName() + " " +
				   JaiI18N.getString("FileStoreDescriptor8"));
			return false;
		    }
		    f.delete();
		}
	    } catch (IOException ioe) {
		// I/O exception during createNewFile().
		msg.append(getName() + " " +
			   JaiI18N.getString("FileStoreDescriptor9") + " " +
			   ioe.getMessage());
		return false;
	    } catch (SecurityException se) {
		// Security exception during exists(), canWrite(),
		// createNewFile(), or delete().
		msg.append(getName() + " " +
			   JaiI18N.getString("FileStoreDescriptor10") + " " +
			   se.getMessage());
		return false;
	    }
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
     * Stores an image to a file.
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
     * @param filename The path of the file to write to.
     * @param format The format of the file.
     * May be <code>null</code>.
     * @param param The encoding parameters.
     * May be <code>null</code>.
     * @param checkFileLocally Boolean specifying whether check for file 
     *                         creation / writing locally should be done. 
     *                         May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>filename</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    String filename,
                                    String format,
                                    ImageEncodeParam param,
				    Boolean checkFileLocally,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("FileStore",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("filename", filename);
        pb.setParameter("format", format);
        pb.setParameter("param", param);
        pb.setParameter("checkFileLocally", checkFileLocally);

        return JAI.create("FileStore", pb, hints);
    }
}
