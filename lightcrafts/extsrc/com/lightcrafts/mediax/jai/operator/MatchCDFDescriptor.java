/*
 * $RCSfile: MatchCDFDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:38 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.Histogram;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "MatchCDF" operation.
 *
 * <p> The "MatchCDF" operation performs a piecewise linear mapping of the
 * pixel values of an image such that the Cumulative Distribution Function
 * (CDF) of the destination image matches as closely as possible a specified
 * Cumulative Distribution Function.  The desired CDF is described by an
 * array of the form <pre>float CDF[numBands][numBins[b]]</pre> where
 * <pre>numBins[b]</pre> denotes the number of bins in the histogram of the
 * source image for band <i>b</i>.  Each element in the array
 * <pre>CDF[b]</pre> must be non-negative, the array must represent a non-
 * decreasing sequence, and the last element of the array must be 1.0F.
 * The source image must have a <code>Histogram</code> object available via
 * its <code>getProperty()</code> method.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>MatchCDF</td></tr>
 * <tr><td>LocalName</td>   <td>MatchCDF</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Matches pixel values to a supplied CDF.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/MatchCDFDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The desired Cumulative Distribution Function.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>      <th>Class Type</th>
 *                        <th>Default Value</th></tr>
 * <tr><td>CDF</td>       <td>float[][]</td>
 *                        <td>CDF for histogram equalization</td>
 * </table></p>
 *
 * @see java.awt.image.DataBuffer
 * @see com.lightcrafts.mediax.jai.ImageLayout
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class MatchCDFDescriptor extends OperationDescriptorImpl {
    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "MatchCDF"},
        {"LocalName",   "MatchCDF"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("MatchCDFDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/MatchCDFDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    "The desired Cumulative Distribution Function."},
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        float[][].class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "CDF"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        null
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public MatchCDFDescriptor() {
        super(resources, supportedModes, 1,
		paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input sources and parameter.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the source image
     * contains a "histogram" property and that the "CDF" array
     * is appropriate for it.
     */
    public boolean validateArguments(String modeName,
				     ParameterBlock args,
                                     StringBuffer msg) {
        if (!super.validateArguments(modeName, args, msg)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

	// Get the source and the CDF array.
        RenderedImage src = args.getRenderedSource(0);

        float[][] CDF = (float[][])args.getObjectParameter(0);

        // Ensure that the Histogram is available and that the CDF array
	// is appropriate for it.
	Object prop = src.getProperty("histogram");
	if(prop == null || prop.equals(Image.UndefinedProperty)) {
            // Property is null or undefined.
            msg.append(getName() + " " +
                       JaiI18N.getString("MatchCDFDescriptor1"));
            return false;
	} else if(!(prop instanceof Histogram)) {
            // Property is not a Histogram.
            msg.append(getName() + " " +
                       JaiI18N.getString("MatchCDFDescriptor2"));
            return false;
	} else {
	    Histogram hist = (Histogram)prop;
	    int numBands = hist.getNumBands();

            if (CDF == null) {
                int[] numBins = hist.getNumBins();
                CDF = new float[numBands][];

                for (int b = 0; b < numBands; b++) {
                    CDF[b] = new float[numBins[b]];
                    for (int i = 0; i < numBins[b]; i++)
                        CDF[b][i] = (i + 1)/numBins[b];
                }
            }

	    if(CDF.length != numBands) {
                // CDF length does not match Histogram.
	        msg.append(getName() + " " +
			   JaiI18N.getString("MatchCDFDescriptor3"));
		return false;
	    }

	    for(int b = 0; b < numBands; b++) {
	        if(CDF[b].length != hist.getNumBins(b)) {
                    // Check that CDF length for this band matches Histogram.
		    msg.append(getName() + " " +
			       JaiI18N.getString("MatchCDFDescriptor4"));
		    return false;
		}
	    }

	    for(int b = 0; b < numBands; b++) {
                float[] CDFband = CDF[b];
                int length = CDFband.length;

                if(CDFband[length-1] != 1.0) {
                    // Last CDF array element value is not 1.0.
                    msg.append(getName() + " " +
                               JaiI18N.getString("MatchCDFDescriptor7"));
                    return false;
                }

                for(int i = 0; i < length; i++) {
                    if(CDFband[i] < 0.0F) {
                        // Negative CDF value.
                        msg.append(getName() + " " +
                                   JaiI18N.getString("MatchCDFDescriptor5"));
                        return false;
                    } else if(i != 0) {
                        if(CDFband[i] < CDFband[i-1]) {
                            // Decreasing sequence.
                            msg.append(getName() + " " +
                                       JaiI18N.getString("MatchCDFDescriptor6"));
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }


    /**
     * Matches pixel values to a supplied CDF.
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
     * @param CDF The desired Cumulative Distribution Function.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    float[][] CDF,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("MatchCDF",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("CDF", CDF);

        return JAI.create("MatchCDF", pb, hints);
    }

    /**
     * Matches pixel values to a supplied CDF.
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
     * @param CDF The desired Cumulative Distribution Function.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                float[][] CDF,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("MatchCDF",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("CDF", CDF);

        return JAI.createRenderable("MatchCDF", pb, hints);
    }
}
