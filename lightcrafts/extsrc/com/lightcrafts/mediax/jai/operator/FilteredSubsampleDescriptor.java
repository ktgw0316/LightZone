/*
 * $RCSfile: FilteredSubsampleDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:36 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.operator;

import com.lightcrafts.media.jai.util.PropertyGeneratorImpl;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.InterpolationBicubic2;
import com.lightcrafts.mediax.jai.InterpolationBicubic;
import com.lightcrafts.mediax.jai.InterpolationBilinear;
import com.lightcrafts.mediax.jai.InterpolationNearest;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.ROIShape;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.WarpOpImage;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * <p> This property generator computes the properties for the operation
 * "FilteredSubsample" dynamically.
 */
class FilteredSubsamplePropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public FilteredSubsamplePropertyGenerator() {
        super(new String[] {"FilteredSubsample"},
              new Class[] {boolean.class},
              new Class[] {RenderedOp.class, RenderableOp.class});
    }

    /**
     * Returns the specified property.
     *
     * @param name  Property name.
     * @param opNode Operation node.
     */
    public Object getProperty(String name,
                              Object opNode) {
        validate(name, opNode);

        if(opNode instanceof RenderedOp &&
           name.equalsIgnoreCase("roi")) {
            RenderedOp op = (RenderedOp)opNode;

	    ParameterBlock pb = op.getParameterBlock();

	    // Retrieve the rendered source image and its ROI.
	    RenderedImage src = pb.getRenderedSource(0);
	    Object property = src.getProperty("ROI");
	    if (property == null ||
		property.equals(java.awt.Image.UndefinedProperty) ||
		!(property instanceof ROI)) {
		return null;
	    }
	    ROI srcROI = (ROI)property;

	    // Determine the effective source bounds.
	    Rectangle srcBounds = null;
	    PlanarImage dst = op.getRendering();
	    if(dst instanceof WarpOpImage && !((OpImage)dst).hasExtender(0)) {
		WarpOpImage warpIm = (WarpOpImage)dst;
		srcBounds =
		    new Rectangle(src.getMinX() + warpIm.getLeftPadding(),
				  src.getMinY() + warpIm.getTopPadding(),
				  src.getWidth() - warpIm.getWidth() + 1,
				  src.getHeight() - warpIm.getHeight() + 1);
	    } else {
		srcBounds = new Rectangle(src.getMinX(),
					  src.getMinY(),
					  src.getWidth(),
					  src.getHeight());
	    }

	    // If necessary, clip the ROI to the effective source bounds.
	    if(!srcBounds.contains(srcROI.getBounds())) {
		srcROI = srcROI.intersect(new ROIShape(srcBounds));
	    }

	    // Retrieve the scale factors
	    float sx = 1.0F/pb.getIntParameter(1);
	    float sy = 1.0F/pb.getIntParameter(2);

	    // Create an equivalent transform.
	    AffineTransform transform =
		new AffineTransform(sx, 0.0, 0.0, sy, 0, 0);

	    // Create the scaled ROI.
	    ROI dstROI = srcROI.transform(transform);

	    // Retrieve the destination bounds.
	    Rectangle dstBounds = op.getBounds();

	    // If necessary, clip the warped ROI to the destination bounds.
	    if(!dstBounds.contains(dstROI.getBounds())) {
		dstROI = dstROI.intersect(new ROIShape(dstBounds));
	    }

	    // Return the warped and possibly clipped ROI.
	    return dstROI;
	} else {
	    return null;
	}
    }

    /** Returns the valid property names for the operation "FilteredSubsample". */
    public String[] getPropertyNames() {
        String[] properties = new String[1];
        properties[0] = "roi";
        return(properties);
    }

}

/**
 * An <code>OperationDescriptor</code> describing the "FilteredSubsample"
 * operation.
 *
 * <p> The "FilteredSubsample" operation subsamples an image by integral
 * factors. The furnished scale factors express the ratio of the
 * source dimensions to the destination dimensions.  The input filter is
 * symmetric about the center pixel and is specified by values from the
 * center outward.  Both filter axes use the same input filter values.

 * <p> When applying scale factors of scaleX, scaleY to a source image
 * with width of src_width and height of src_height, the resulting image
 * is defined to have the following bounds:
 *
 * <code></pre>
 *       dst_minX  = round(src_minX  / scaleX) <br>
 *       dst_minY  = round(src_minY  / scaleY) <br>
 *       dst_width  =  round(src_width  / scaleX) <br>
 *       dst_height =  round(src_height / scaleY) <br>
 * </pre></code>
 *
 * <p> The input filter is quadrant symmetric (typically antialias). The
 * filter is product-separable, quadrant symmetric, and is defined by half of its
 * span. For example, if the input filter, qsFilter, was of size 3, it would have
 * width and height 5 and have the symmetric form: <br>
 *   <code> qs[2] qs[1] qs[0] qs[1] qs[2] </code> <br>
 *
 * <p> A fully expanded 5 by 5 kernel has the following format (25 entries
 * defined by only 3 entries):
 *
 *   <code>
 *   <p align=center> qs[2]*qs[2]  qs[2]*qs[1]  qs[2]*qs[0]  qs[2]*qs[1]  qs[2]*qs[2] <br>
 *
 *                    qs[1]*qs[2]  qs[1]*qs[1]  qs[1]*qs[0]  qs[1]*qs[1]  qs[1]*qs[2] <br>
 *
 *                    qs[0]*qs[2]  qs[0]*qs[1]  qs[0]*qs[0]  qs[0]*qs[1]  qs[0]*qs[2] <br>
 *
 *                    qs[1]*qs[2]  qs[1]*qs[1]  qs[1]*qs[0]  qs[1]*qs[1]  qs[1]*qs[2] <br>
 *
 *                    qs[2]*qs[2]  qs[2]*qs[1]  qs[2]*qs[0]  qs[2]*qs[1]  qs[2]*qs[2]
 *   </p> </code>
 *
 * <p> This operator is similar to the image scale operator.  Important
 * differences are described here.  The coordinate transformation differences
 * between the FilteredDownsampleOpImage and the ScaleOpImage operators can be
 * understood by comparing their mapping equations directly.
 *
 * <p> For the scale operator, the destination (D) to source (S) mapping
 * equations are given by
 *
 * <code>
 *   <p> xS = (xD - xTrans)/xScale <br>
 *       yS = (yD - yTrans)/yScale
 * </code>
 *
 * <p> The scale and translation terms are floating point values in D-frame
 * pixel units.  For scale this means that one S pixel maps to xScale
 * by yScale D-frame pixels.  The translation vector, (xTrans, yTrans),
 * is in D-frame pixel units.
 *
 * <p> The FilteredSubsample operator mapping equations are given by
 *
 * <code>
 *   <p> xS = xD*scaleX <br>
 *   yS = yD*scaleY
 * </code>
 *
 * <p> The scale terms for this operation are integral values in the
 * S-Frame; there are no translation terms for this operation.
 *
 * <p> The downsample terms are restricted to positive integral values.
 * Geometrically, one D-frame pixel maps to scaleX * scaleY S-frame
 * pixels.  The combination of downsampling and filtering has performance
 * benefits over sequential operator usage in part due to the symmetry
 * constraints imposed by only allowing integer parameters for scaling and
 * only allowing separable symmetric filters.  With odd scale factors, D-frame
 * pixels map directly onto S-frame pixel centers.  With even scale factors,
 * D-frame pixels map squarely between S-frame pixel centers.  Below are
 * examples of even, odd, and combination cases.
 *
 *   <p>  s = S-frame pixel centers <br>
 *        d = D-frame pixel centers mapped to S-frame
 *   </p>
 *   <kbd>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>   d       d       d  </pre>
 *   <pre> s   s   s   s   s   s           s   d   s   s   d   s  </pre>
 *   <pre>  </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>   d       d       d  </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>  </pre>
 *   <pre> s   s   s   s   s   s           s   d   s   s   d   s  </pre>
 *   <pre>   d       d       d  </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>  </pre>
 *   <pre> Even scaleX/Y factors            Odd scaleX/Y factors  </pre>
 *   <pre>   </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>     d           d    </pre>
 *   <pre> s   s   s   s   s   s           s d s   s d s   s d s  </pre>
 *   <pre>   </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>     d           d    </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>   </pre>
 *   <pre> s   s   s   s   s   s           s d s   s d s   s d s  </pre>
 *   <pre>     d           d    </pre>
 *   <pre> s   s   s   s   s   s           s   s   s   s   s   s  </pre>
 *   <pre>   </pre>
 * <pre>  Odd/even scaleX/Y factors      Even/odd scaleX/Y factors  </pre> <br>
 *   </kbd>
 *
 * <p> The convolution kernel is restricted to have quadrant symmetry (qs). This
 * type of symmetry is also product separable.  The qsFilter is specified by
 * a floating array.  If qsFilter[0], qsFilter[1], ... , 
 * qsFilter[qsFilter.length - 1]
 * is the filter input, then the entire separable kernel is given by <br>
 * qsFilter[qsFilter.length - 1], ... , qsFilter[0], ... , 
 * qsFilter[qsFilter.length - 1] <br>
 *
 * <p> The restriction of integer parameter constraints allows full product
 * separablity and symmetry when applying the combined resample and filter
 * convolution operations.
 *
 * <p> If Bilinear or Bicubic interpolation is specified, the source needs
 * to be extended such that it has the extra pixels needed to compute all
 * the destination pixels. This extension is performed via the
 * <code>BorderExtender</code> class. The type of border extension can be
 * specified as a <code>RenderingHint</code> to the <code>JAI.create</code>
 * method.
 *
 * <p> If no <code>BorderExtender</code> is specified, the source will
 * not be extended.  The output image size is still calculated
 * according to the formula specified above. However since there is not
 * enough source to compute all the destination pixels, only that
 * subset of the destination image's pixels which can be computed,
 * will be written in the destination. The rest of the destination
 * will be set to zeros.
 *
 * <p> It should be noted that this operation automatically adds a
 * value of <code>Boolean.TRUE</code> for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> to the given
 * <code>configuration</code> so that the operation is performed
 * on the pixel values instead of being performed on the indices into
 * the color map if the source(s) have an <code>IndexColorModel</code>.
 * This addition will take place only if a value for the 
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been
 * provided by the user. Note that the <code>configuration</code> Map
 * is cloned before the new hint is added to it. The operation can be 
 * smart about the value of the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code>
 * <code>RenderingHints</code>, i.e. while the default value for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> is
 * <code>Boolean.TRUE</code>, in some cases the operator could set the 
 * default.
 *
 * <p> "FilteredSubsample" defines a PropertyGenerator that performs an identical
 * transformation on the "ROI" property of the source image, which can
 * be retrieved by calling the <code>getProperty</code> method with
 * "ROI" as the property name.
 *
 * <p> One design purpose of this operation is anti-aliasing when 
 * downsampling.  The technique of anti-aliasing applies a good 
 * filter to the area of rendering to generate better results.  Generally, 
 * this filter is required to be (quadrant) symmetric and separable 
 * to obtain good performance.  The widely-used Gaussian filter 
 * satisfies all these requirements.  Thus, the default value for the 
 * parameter "qsFilter" is generated from a Gaussian kernel 
 * based on the following procedure:
 *
 * <p> Define the Gaussian function <code>G(x)</code> as
 * <p>  <code>G(x) = e<sup>-x<sup>2</sup>/(2s<sup>2</sup>)</sup>/(
 * (2pi)<sup>&#189;</sup>s)</code>,
 * <p>where <code>s</code> is the standard deviation, and <code>pi</code>
 * is the ratio of the circumference of a circle to its diameter.
 *
 * <p> For a one-dimensional Gaussian kernel with a size of <code>2N+1</code>, 
 *  the standard deviation of the Gaussian function to generate this kernel 
 *  is chosen as <code>N/3</code>.  The 
 *  one-dimensional Gaussian kernel <code>K<sub>N</sub>(1:2N+1)</code> is 
 *  <p><code>(G(-N)/S, G(-N+1)/S, ..., G(0),..., G(N-1)/S, G(N)/S), </code>
 * <p> where <code>S</code> is the summation of <code>G(-N), G(-N+1), ...,G(0), 
 * ..., G(N-1)</code>, and <code>G(N)</code>.  A two-dimensional Gaussian 
 * kernel with a size of <code>(2N+1) x (2N+1)</code> is constructed as the 
 * outer product of two <code>K<sub>N</sub></code>s: 
 * the <code>(i, j)</code>th element is 
 * <code>K<sub>N</sub>(i)K<sub>N</sub>(j)</code>.
 * The quadrant symmetric filter corresponding to the 
 * <code>(2N+1) x (2N+1)</code> Gaussian kernel is simply 
 * <p> <code>(G(0)/S, G(1)/S, ..., G(N)/S)</code>, or
 * <p> <code>(K<sub>N</sub>(N+1), ..., K<sub>N</sub>(2N+1)</code>.
 *
 * <p> Denote the maximum of the X and Y subsample factors as <code>M</code>.
 * If <code>M</code> is even, the default "qsFilter" is the quadrant symmetric 
 * filter derived from the two-dimensional <code>(M+1) x (M+1)</code>
 * Gaussian kernel. If <code>M</code> is odd, the default 
 * "qsFilter" is the quadrant symmetric filter derived from the 
 * two-dimensional <code>M x M</code> Gaussian kernel.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>FilteredSubsample</td></tr>
 * <tr><td>LocalName</td>   <td>FilteredSubsample</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Filters and subsamples an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/FilteredSubsample.html</td></tr>
 * <tr><td>Version</td>     <td>1.1</td></tr>
 * <tr><td>arg0Desc</td>    <td>The X subsample factor.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The Y subsample factor.</td></tr>
 * <tr><td>arg2Desc</td>    <td>Symmetric filter coefficients.</td></tr>
 * <tr><td>arg3Desc</td>    <td>The interpolation object for
 *                              resampling.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * <tr><td>scaleX</td>        <td>java.lang.Integer</td>
 *                            <td>2</td>
 * <tr><td>scaleY</td>        <td>java.lang.Integer</td>
 *                            <td>2</td>
 * <tr><td>qsFilter</td>      <td>java.lang.Float []</td>
 *                            <td>A quadrant symmetric filter 
 *				  generated from a Gaussian kernel 
 *				  as described above.</td>
 * <tr><td>interpolation</td> <td>com.lightcrafts.mediax.jai.Interpolation</td>
 *                            <td>InterpolationNearest</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.Interpolation
 * @see com.lightcrafts.mediax.jai.BorderExtender
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 *
 * @since JAI 1.1
 */
public class FilteredSubsampleDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "FilteredSubsample"},
        {"LocalName",   "FilteredSubsample"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("FilteredSubsampleDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/FilteredSubsampleDescriptor.html"},
        {"Version",     "1.0"},
        {"arg0Desc",    "The X subsample factor."},
        {"arg1Desc",    "The Y subsample factor."},
	{"arg2Desc",    "Symmetric filter coefficients."},
	{"arg3Desc",    "Interpolation object."}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        java.lang.Integer.class, java.lang.Integer.class,
	float[].class, Interpolation.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "scaleX", "scaleY", "qsFilterArray", "interpolation",
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        new Integer(2),
        new Integer(2),
        null,
        Interpolation.getInstance(Interpolation.INTERP_NEAREST)
    };

    private static final String[] supportedModes = {
	"rendered"
    };

    /** <p> Constructor. */
    public FilteredSubsampleDescriptor() {
        super(resources, supportedModes, 1,
              paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that "scaleX" and "scaleY"
     * are both greater than 0 and that the interpolation type
     * is one of 4 standard types: <br>
     * <p> <code>
     *    com.lightcrafts.mediax.jai.InterpolationNearest <br>
     *    com.lightcrafts.mediax.jai.InterpolationBilinear <br>
     *    com.lightcrafts.mediax.jai.InterpolationBicubic <br>
     *    com.lightcrafts.mediax.jai.InterpolationBicubic2
     * </code>
     */
    protected boolean validateParameters(String modeName,
					 ParameterBlock args,
                                         StringBuffer msg) {
	if (!super.validateParameters(modeName, args, msg))
	    return false;

        int scaleX = args.getIntParameter(0);
        int scaleY = args.getIntParameter(1);
        if (scaleX < 1 || scaleY < 1) {
            msg.append(getName() + " " +
                       JaiI18N.getString("FilteredSubsampleDescriptor1"));
            return false;
        }

	float[] filter = (float[])args.getObjectParameter(2);

	// if this parameter is null, generate the kernel based on the
	// procedure described above.
	if (filter == null) {
	    int m = scaleX > scaleY ? scaleX: scaleY;
	    if ((m & 1) == 0)
		m++;

	    double sigma = (m - 1) / 6.0;

	    // when m is 1, sigma is 0; will generate NaN.  Give any number
	    // to sigma will generate the correct kernel {1.0}
	    if (m == 1)
		sigma = 1.0;

	    filter = new float[m/2 + 1];
	    float sum = 0;

	    for (int i = 0; i < filter.length; i++) {
		filter[i] = (float)gaussian((double)i, sigma);
		if (i == 0)
		    sum += filter[i];
		else
		    sum += filter[i] * 2;
	    }

	    for (int i = 0; i < filter.length; i++) {
		filter[i] /= sum;
	    }

	    args.set(filter, 2);
	}

        Interpolation interp = (Interpolation)args.getObjectParameter(3);

        // Determine the interpolation type, if not supported throw exception
        if (!((interp instanceof InterpolationNearest)  ||
            (interp instanceof InterpolationBilinear) ||
            (interp instanceof InterpolationBicubic)  ||
            (interp instanceof InterpolationBicubic2))) {
           msg.append(getName() + " " +
                       JaiI18N.getString("FilteredSubsampleDescriptor2"));
           return false;
        }
        return true;

    } // validateParameters

    /** Computes the value of Gaussian function at <code>x</code>.
     * @param x The coordinate at where the value is computed.
     * @param sigma The standard deviation for the Gaussian function.
     */
    private double gaussian(double x, double sigma) {
	return Math.exp(-x*x/(2 * sigma * sigma)) / sigma / Math.sqrt(2 * Math.PI);
    }



    /**
     * Filters and subsamples an image.
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
     * @param scaleX The X subsample factor.
     * May be <code>null</code>.
     * @param scaleY The Y subsample factor.
     * May be <code>null</code>.
     * @param qsFilterArray Symmetric filter coefficients.
     * May be <code>null</code>.
     * @param interpolation Interpolation object.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Integer scaleX,
                                    Integer scaleY,
                                    float[] qsFilterArray,
                                    Interpolation interpolation,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("FilteredSubsample",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("scaleX", scaleX);
        pb.setParameter("scaleY", scaleY);
        pb.setParameter("qsFilterArray", qsFilterArray);
        pb.setParameter("interpolation", interpolation);

        return JAI.create("FilteredSubsample", pb, hints);
    }
} // FilteredSubsampleDescriptor
