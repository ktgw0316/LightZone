/*
 * $RCSfile: CompositeDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:32 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.operator.CompositeDestAlpha;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Composite" operation.
 *
 * <p> The "Composite" operation combines two images based on their alpha
 * values at each pixel. It is done on a per-band basis, and the two source
 * images are expected to have the same number of bands and the same data
 * type. The destination image has the same data type as the two sources.
 *
 * <p> The <code>destAlpha</code> parameter indicates if the destination
 * image should have an extra alpha channel.  If this parameter is set to
 * <code>NO_DESTINATION_ALPHA</code>, then the destination image does not
 * include an alpha band, and it should have the same number of bands as
 * the two source images.  If it is set to <code>DESTINATION_ALPHA_FIRST</code>,
 * then the destination image has one extra band than the source images,
 * which represents the result alpha channel, and this band is the first
 * band (band 0) of the destination.  If it is set to
 * <code>DESTINATION_ALPHA_LAST</code>, then the destination image also
 * has the extra alpha channel, but this band is the last band of the
 * destination.
 *
 * <p> The destination pixel values may be viewed as representing a fractional
 * pixel coverage or transparency factor. Specifically, Composite implements
 * the Porter-Duff "over" rule (see <i>Computer Graphics</i>, July 1984 pp.
 * 253-259), in which the output color of a pixel with source value/alpha
 * tuples <code>(A, a)</code> and <code>(B, b)</code> is given by
 * <code>a*A + (1 - a)*(b*B)</code>.  The output alpha value is given
 * by <code>a + (1 - a)*b</code>.  For premultiplied sources tuples
 * <code>(a*A, a)</code> and <code>(b*B, b)</code>, the premultiplied output
 * value is simply <code>(a*A) + (1 - a)*(b*B)</code>. 
 *
 * <p> The color channels of the two source images are supplied via
 * <code>source1</code> and <code>source2</code>. The two sources must
 * be either both pre-multiplied by alpha or not. Alpha channel should
 * not be included in <code>source1</code> and <code>source2</code>.
 *
 * <p> The alpha channel of the first source images must be supplied
 * via the <code>source1Alpha</code> parameter. This parameter may not
 * be null. The alpha channel of the second source image may be supplied
 * via the <code>source2Alpha</code> parameter. This parameter may be
 * null, in which case the second source is considered completely opaque.
 * The alpha images should be single-banded, and have the same data type
 * as well as dimensions as their corresponding source images.
 *
 * <p> The <code>alphaPremultiplied</code> parameter indicates whether
 * or not the supplied alpha image is premultiplied to both the source
 * images. It also indicates whether the destination image color channels
 * have the alpha values multiplied to the pixel color values.
 *
 * <p> It should be noted that the <code>source1Alpha</code> and 
 * <code>source1Alpha</code> parameters are <code>RenderedImage</code>s in
 * the "rendered" mode and are <code>RenderableImage</code>s in the 
 * "renderable" mode.
 *
 * <p> The destination image is the combination of the two source images.
 * It has the color channels, and if specified, one additional alpha channel
 * (the band index depends on the value of the <code>destAlpha</code>
 * parameter). Whether alpha value is pre-multiplied to the color channels
 * also depend on the value of <code>alphaPremultiplied</code> (pre-multiplied
 * if true).
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>composite</td></tr>
 * <tr><td>LocallName</td>  <td>composite</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Composites two images based on an alpha mask.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jaiapi/com.lightcrafts.mediax.jai.operator.CompositeDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The alpha image for the first source.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The alpha image for the second source.</td></tr>
 * <tr><td>arg2Desc</td>    <td>True if alpha has been premultiplied to both
 *                              sources and the destination.</td></tr>
 * <tr><td>arg3Desc</td>    <td>Indicates if the destination image should
 *                              include an extra alpha channel, and if so,
 *                              should it be the first or last band.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr>
 *     <th>Name</th>
 *     <th COLSPAN=2>Class Type</th>
 *     <th>Default Value</th>
 * </tr>
 * <tr>
 *     <td ROWSPAN=2>source1Alpha</td>
 *     <td>Rendered mode</td>
 *     <td>java.awt.image.RenderedImage</td>
 *     <td ROWSPAN=2>NO_PARAMETER_DEFAULT</td>
 * </tr>
 * <tr>
 *     <td>Renderable mode</td>
 *     <td>java.awt.image.renderable.RenderableImage</td>
 *</tr>
 * <tr>
 *     <td ROWSPAN=2>source2Alpha</td>
 *     <td>Rendered mode</td>
 *     <td>java.awt.image.RenderedImage</td>
 *     <td ROWSPAN=2>null</td>
 * </tr>
 * <tr>
 *     <td>Renderable mode</td>
 *     <td>java.awt.image.renderable.RenderableImage</td>
 *</tr>
 * <tr>
 *     <td>alphaPremultiplied</td>
 *     <td COLSPAN=2>java.lang.Boolean</td>
 *     <td>false</td>
 * </tr>
 * <tr>
 *     <td>destAlpha</td>
 *     <td COLSPAN=2>com.lightcrafts.mediax.jai.operator.CompositeDestAlpha</td>
 *     <td>NO_DESTINATION_ALPHA</td>
 * </tr>
 * </table></p>
 *
 * @see CompositeDestAlpha
 * @see java.awt.image.ColorModel
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class CompositeDescriptor extends OperationDescriptorImpl {

    /** The destination image does not have the alpha channel. */
    public static final CompositeDestAlpha NO_DESTINATION_ALPHA =
        new CompositeDestAlpha("NO_DESTINATION_ALPHA", 0);

    /** The destination image has the channel, and it is the first band. */
    public static final CompositeDestAlpha DESTINATION_ALPHA_FIRST =
        new CompositeDestAlpha("DESTINATION_ALPHA_FIRST", 1);

    /** The destination image has the channel, and it is the last band. */
    public static final CompositeDestAlpha DESTINATION_ALPHA_LAST =
        new CompositeDestAlpha("DESTINATION_ALPHA_LAST", 2);

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    protected static final String[][] resources = {
        {"GlobalName",  "Composite"},
        {"LocalName",   "Composite"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("CompositeDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/CompositeDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion2")},
        {"arg0Desc",    JaiI18N.getString("CompositeDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("CompositeDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("CompositeDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("CompositeDescriptor4")}
    };

    private static final Class[][] sourceClasses = { 
	{
	    java.awt.image.RenderedImage.class, 
	    java.awt.image.RenderedImage.class
	}, 
	{
	    java.awt.image.renderable.RenderableImage.class, 
	    java.awt.image.renderable.RenderableImage.class
	}
    };

    /** The parameter class list for this operation. */
    private static final Class[][] paramClasses = {
	{
	    java.awt.image.RenderedImage.class,
	    java.awt.image.RenderedImage.class,
	    java.lang.Boolean.class,
	    CompositeDestAlpha.class
	}, 
	{
	    java.awt.image.renderable.RenderableImage.class,
	    java.awt.image.renderable.RenderableImage.class,
	    java.lang.Boolean.class,
	    CompositeDestAlpha.class
	}
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "source1Alpha", "source2Alpha",
        "alphaPremultiplied", "destAlpha"
    };

    /** The parameter default value list for this operation. */
    private static final Object[][] paramDefaults = {
	{
	    NO_PARAMETER_DEFAULT, null,
	    Boolean.FALSE, NO_DESTINATION_ALPHA
	}, 
	{
	    NO_PARAMETER_DEFAULT, null,
	    Boolean.FALSE, NO_DESTINATION_ALPHA
	}
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public CompositeDescriptor() {
        super(resources, 
	      supportedModes,
	      null,
	      sourceClasses,
	      paramNames,
	      paramClasses, 
	      paramDefaults, 
	      null);
    }

    /**
     * Validates the input sources and parameters.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the source image
     * <code>samplemodel</code>s have the same number of bands and
     * transfer type, and that the alpha images have the same bounds
     * as the corresponding sources and the correct transfer type.
     */
    public boolean validateArguments(String modeName,
				     ParameterBlock args,
                                     StringBuffer msg) {
        if (!super.validateArguments(modeName, args, msg)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

        RenderedImage src1 = args.getRenderedSource(0);
        RenderedImage src2 = args.getRenderedSource(1);

        SampleModel s1sm = src1.getSampleModel();
        SampleModel s2sm = src2.getSampleModel();
        if (s1sm.getNumBands() != s2sm.getNumBands() ||
            s1sm.getTransferType() != s2sm.getTransferType()) {
            msg.append(getName() + " " +
                       JaiI18N.getString("CompositeDescriptor8"));
            return false;
        }

        /* Validate Parameters. */
	RenderedImage afa1 = (RenderedImage)args.getObjectParameter(0);
        if (src1.getMinX() != afa1.getMinX() ||
            src1.getMinY() != afa1.getMinY() ||
            src1.getWidth() != afa1.getWidth() ||
            src1.getHeight() != afa1.getHeight()) {
            msg.append(getName() + " " +
                       JaiI18N.getString("CompositeDescriptor12"));
            return false;
        }

        SampleModel a1sm = afa1.getSampleModel();
        if (s1sm.getTransferType() != a1sm.getTransferType()) {
            msg.append(getName() + " " +
                       JaiI18N.getString("CompositeDescriptor13"));
            return false;
        }

	RenderedImage afa2 = (RenderedImage)args.getObjectParameter(1);
        if (afa2 != null) {
            if (src2.getMinX() != afa2.getMinX() ||
                src2.getMinY() != afa2.getMinY() ||
                src2.getWidth() != afa2.getWidth() ||
                src2.getHeight() != afa2.getHeight()) {
                msg.append(getName() + " " +
                           JaiI18N.getString("CompositeDescriptor15"));
                return false;
            }
        
            SampleModel a2sm = afa2.getSampleModel();
            if (s2sm.getTransferType() != a2sm.getTransferType()) {
                msg.append(getName() + " " +
                           JaiI18N.getString("CompositeDescriptor16"));
                return false;
            }
        }

        return true;
    }


    /**
     * Composites two images based on an alpha mask.
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
     * @param source1 <code>RenderedImage</code> source 1.
     * @param source1Alpha The alpha image for the first source.
     * @param source2Alpha The alpha image for the second source.
     * May be <code>null</code>.
     * @param alphaPremultiplied True if alpha has been premultiplied to both sources and the destination.
     * May be <code>null</code>.
     * @param destAlpha Indicates if the destination image should include an extra alpha channel, and if so, should it be the first or last band.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1Alpha</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    RenderedImage source1,
                                    RenderedImage source1Alpha,
                                    RenderedImage source2Alpha,
                                    Boolean alphaPremultiplied,
                                    CompositeDestAlpha destAlpha,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Composite",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        pb.setParameter("source1Alpha", source1Alpha);
        pb.setParameter("source2Alpha", source2Alpha);
        pb.setParameter("alphaPremultiplied", alphaPremultiplied);
        pb.setParameter("destAlpha", destAlpha);

        return JAI.create("Composite", pb, hints);
    }

    /**
     * Composites two images based on an alpha mask.
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
     * @param source1 <code>RenderableImage</code> source 1.
     * @param source1Alpha The alpha image for the first source.
     * @param source2Alpha The alpha image for the second source.
     * May be <code>null</code>.
     * @param alphaPremultiplied True if alpha has been premultiplied to both sources and the destination.
     * May be <code>null</code>.
     * @param destAlpha Indicates if the destination image should include an extra alpha channel, and if so, should it be the first or last band.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>source1Alpha</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                RenderableImage source1,
                                                RenderableImage source1Alpha,
                                                RenderableImage source2Alpha,
                                                Boolean alphaPremultiplied,
                                                CompositeDestAlpha destAlpha,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Composite",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);
        pb.setSource("source1", source1);

        pb.setParameter("source1Alpha", source1Alpha);
        pb.setParameter("source2Alpha", source2Alpha);
        pb.setParameter("alphaPremultiplied", alphaPremultiplied);
        pb.setParameter("destAlpha", destAlpha);

        return JAI.createRenderable("Composite", pb, hints);
    }
}
