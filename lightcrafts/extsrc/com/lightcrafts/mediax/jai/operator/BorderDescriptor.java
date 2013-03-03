/*
 * $RCSfile: BorderDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:30 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.OperationNode;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Border" operation.
 *
 * <p> The Border operation adds a border around a rendered image. The
 * size of the border is specified in pixels by the left, right, top,
 * and bottom padding parameters, corresponding to the four sides of
 * the source image. These paddings may not be less than 0.
 *
 * <p>The pixel values of the added border area will be set according to
 * the algorithm of the <code>BorderExtender</code> passed as a parameter.
 * The <code>BorderExtender</code>s provide the ability to extend the
 * border by:
 * <ul>
 * <li>filling it with zeros (<code>BorderExtenderZero</code>);
 * <li>filling it with constants (<code>BorderExtenderConstant</code>);
 * <li>copying the edge and corner pixels (<code>BorderExtenderCopy</code>);
 * <li>reflecting about the edges of the image
 *     (<code>BorderExtenderReflect</code>); or,
 * <li>"wrapping" the image plane toroidally, that is, joining opposite
 * edges of the image (<code>BorderExtenderWrap</code>).
 * </ul>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Border</td></tr>
 * <tr><td>LocalName</td>   <td>Border</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Adds a border around an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BorderDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The image's left padding.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The image's right padding.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The image's top padding.</td></tr>
 * <tr><td>arg3Desc</td>    <td>The image's bottom padding.</td></tr>
 * <tr><td>arg4Desc</td>    <td>The border extender.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>       <th>Class Type</th>
 *                         <th>Default Value</th></tr>
 * <tr><td>leftPad</td>    <td>java.lang.Integer</td>
 *                         <td>0</td>
 * <tr><td>rightPad</td>   <td>java.lang.Integer</td>
 *                         <td>0</td>
 * <tr><td>topPad</td>     <td>java.lang.Integer</td>
 *                         <td>0</td>
 * <tr><td>bottomPad</td>  <td>java.lang.Integer</td>
 *                         <td>0</td>
 * <tr><td>type</td>       <td>com.lightcrafts.mediax.jai.BorderExtender</td>
 *                         <td>com.lightcrafts.mediax.jai.BorderExtenderZero</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class BorderDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Border"},
        {"LocalName",   "Border"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("BorderDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/BorderDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion2")},
        {"arg0Desc",    JaiI18N.getString("BorderDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("BorderDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("BorderDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("BorderDescriptor4")},
        {"arg4Desc",    JaiI18N.getString("BorderDescriptor5")},
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "leftPad", "rightPad", "topPad", "bottomPad", "type"
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        java.lang.Integer.class, java.lang.Integer.class,
        java.lang.Integer.class, java.lang.Integer.class,
        com.lightcrafts.mediax.jai.BorderExtender.class
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        new Integer(0), new Integer(0), new Integer(0), new Integer(0),
        BorderExtender.createInstance(BorderExtender.BORDER_ZERO)
    };

    /** Constructor. */
    public BorderDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Calculates the region over which two distinct renderings
     * of the "Border" operation may be expected to differ.
     *
     * <p> The operation returns a <code>Shape</code> or <code>null</code>
     * in the rendered mode and <code>null</code> in all other modes.
     *
     * @param modeName The name of the mode.
     * @param oldParamBlock The previous sources and parameters.
     * @param oldHints The previous hints.
     * @param newParamBlock The current sources and parameters.
     * @param newHints The current hints.
     * @param node The affected node in the processing chain (ignored).
     *
     * @return The region over which the data of two renderings of this
     *         operation may be expected to be invalid or <code>null</code>
     *         if there is no common region of validity.
     *         A non-<code>null</code> empty region indicates that the
     *         operation would produce identical data over the bounds of the
     *         old rendering although perhaps not over the area occupied by
     *         the <i>tiles</i> of the old rendering.
     *
     * @throws IllegalArgumentException if <code>modeName</code>
     *         is <code>null</code> or if the operation requires either
     *         sources or parameters and either <code>oldParamBlock</code>
     *         or <code>newParamBlock</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>oldParamBlock</code> or
     *         <code>newParamBlock</code> do not contain sufficient sources
     *         or parameters for the operation in question.
     */
    public Object getInvalidRegion(String modeName,
                                   ParameterBlock oldParamBlock,
                                   RenderingHints oldHints,
                                   ParameterBlock newParamBlock,
                                   RenderingHints newHints,
                                   OperationNode node) {
        if ((modeName == null) ||
            ((getNumSources() > 0 || getNumParameters() > 0) &&
             (oldParamBlock == null || newParamBlock == null))) {

            throw new IllegalArgumentException(JaiI18N.getString("BorderDescriptor6"));
        }
	
	int numSources = getNumSources();

	if ((numSources > 0) &&
            (oldParamBlock.getNumSources() != numSources ||
             newParamBlock.getNumSources() != numSources)) {

            throw new IllegalArgumentException(JaiI18N.getString("BorderDescriptor7"));

        }
	
	int numParams = getParameterListDescriptor(modeName).getNumParameters();

	if ((numParams > 0) &&
            (oldParamBlock.getNumParameters() != numParams ||
             newParamBlock.getNumParameters() != numParams)) {

            throw new IllegalArgumentException(JaiI18N.getString("BorderDescriptor8"));
        }

        // Return null if the RenderingHints, source, left padding, or
        // top padding changed.
        if(!modeName.equalsIgnoreCase(RenderedRegistryMode.MODE_NAME) ||
           (oldHints == null && newHints != null) ||
           (oldHints != null && newHints == null) ||
           (oldHints != null && !oldHints.equals(newHints)) ||
           !oldParamBlock.getSource(0).equals(newParamBlock.getSource(0)) ||
           oldParamBlock.getIntParameter(0) != // left pad
           newParamBlock.getIntParameter(0) ||
           oldParamBlock.getIntParameter(2) != // top pad
           newParamBlock.getIntParameter(2)) {
            return null;
        }

        Shape invalidRegion = null;

        if(!oldParamBlock.getObjectParameter(4).equals(
                newParamBlock.getObjectParameter(4))) {
            // BorderExtender changed.

            // Get source and the left and top padding.
            RenderedImage src = oldParamBlock.getRenderedSource(0);
            int leftPad = oldParamBlock.getIntParameter(0);
            int topPad = oldParamBlock.getIntParameter(2);

            // Get source bounds.
            Rectangle srcBounds =
                new Rectangle(src.getMinX(), src.getMinY(),
                              src.getWidth(), src.getHeight());

            // Get destination bounds.
            Rectangle dstBounds =
                new Rectangle(srcBounds.x - leftPad,
                              srcBounds.y - topPad,
                              srcBounds.width + leftPad +
                              oldParamBlock.getIntParameter(1),
                              srcBounds.height + topPad +
                              oldParamBlock.getIntParameter(3));

            // Determine invalid area by subtracting source bounds.
            Area invalidArea = new Area(dstBounds);
            invalidArea.subtract(new Area(srcBounds));
            invalidRegion = invalidArea;

        } else if((newParamBlock.getIntParameter(1) <   // new R < old R
            oldParamBlock.getIntParameter(1) &&
            newParamBlock.getIntParameter(3) <=  // new B <= old B
            oldParamBlock.getIntParameter(3)) ||
           (newParamBlock.getIntParameter(3) <   // new B < old B
            oldParamBlock.getIntParameter(3) &&
            newParamBlock.getIntParameter(1) <=  // new R <= old R
            oldParamBlock.getIntParameter(1))) {
            // One or both right and bottom padding decreased.

            // Get source and the left and top padding.
            RenderedImage src = oldParamBlock.getRenderedSource(0);
            int leftPad = oldParamBlock.getIntParameter(0);
            int topPad = oldParamBlock.getIntParameter(2);

            // Get source bounds.
            Rectangle srcBounds =
                new Rectangle(src.getMinX(), src.getMinY(),
                              src.getWidth(), src.getHeight());

            // Get old destination bounds.
            Rectangle oldBounds =
                new Rectangle(srcBounds.x - leftPad,
                              srcBounds.y - topPad,
                              srcBounds.width + leftPad +
                              oldParamBlock.getIntParameter(1),
                              srcBounds.height + topPad +
                              oldParamBlock.getIntParameter(3));

            // Get new destination bounds.
            Rectangle newBounds =
                new Rectangle(srcBounds.x - leftPad,
                              srcBounds.y - topPad,
                              srcBounds.width + leftPad +
                              newParamBlock.getIntParameter(1),
                              srcBounds.height + topPad +
                              newParamBlock.getIntParameter(3));

            // Determine invalid area by subtracting new from old bounds.
            Area invalidArea = new Area(oldBounds);
            invalidArea.subtract(new Area(newBounds));
            invalidRegion = invalidArea;

        } else {
            // Either nothing changed or one or both of the right and bottom
            // padding was increased.
            invalidRegion = new Rectangle();
        }

        return invalidRegion;
    }


    /**
     * Adds a border around an image.
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
     * @param leftPad The image's left padding.
     * May be <code>null</code>.
     * @param rightPad The image's right padding.
     * May be <code>null</code>.
     * @param topPad The image's top padding.
     * May be <code>null</code>.
     * @param bottomPad The image's bottom padding.
     * May be <code>null</code>.
     * @param type The border type.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Integer leftPad,
                                    Integer rightPad,
                                    Integer topPad,
                                    Integer bottomPad,
                                    BorderExtender type,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Border",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("leftPad", leftPad);
        pb.setParameter("rightPad", rightPad);
        pb.setParameter("topPad", topPad);
        pb.setParameter("bottomPad", bottomPad);
        pb.setParameter("type", type);

        return JAI.create("Border", pb, hints);
    }
}
