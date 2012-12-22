/*
 * $RCSfile: ImageFunctionDescriptor.java,v $
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
import com.lightcrafts.media.jai.util.PropertyGeneratorImpl;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.ImageFunction;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.PropertyGenerator;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * This property generator computes the properties for the operation
 * "ImageFunction" dynamically.
 */
class ImageFunctionPropertyGenerator extends PropertyGeneratorImpl {

    /** Constructor. */
    public ImageFunctionPropertyGenerator() {
        super(new String[] {"COMPLEX"},
              new Class[] {Boolean.class},
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

        if (name.equalsIgnoreCase("complex")) {
            if(opNode instanceof RenderedOp) {
                RenderedOp op = (RenderedOp)opNode;
                ParameterBlock pb = op.getParameterBlock();
                ImageFunction imFunc = (ImageFunction)pb.getObjectParameter(0);
                return imFunc.isComplex() ? Boolean.TRUE : Boolean.FALSE;
            } else if(opNode instanceof RenderableOp) {
                RenderableOp op = (RenderableOp)opNode;
                ParameterBlock pb = op.getParameterBlock();
                ImageFunction imFunc = (ImageFunction)pb.getObjectParameter(0);
                return imFunc.isComplex() ? Boolean.TRUE : Boolean.FALSE;
            }
        }

        return java.awt.Image.UndefinedProperty;
    }
}


/**
 * An <code>OperationDescriptor</code> describing the "ImageFunction"
 * operation.
 *
 * <p> The "ImageFunction" operation generates an image on the basis of
 * a functional description provided by an object which is an instance of
 * a class which implements the <code>ImageFunction</code> interface.
 * The <i>(x,y)</i> coordinates passed to the <code>getElements()</code>
 * methods of the <code>ImageFunction</code> object are derived by applying
 * an optional translation and scaling to the X- and Y-coordinates of the
 * image.  The image X- and Y-coordinates as usual depend on the values of
 * the minimum X- and Y- coordinates of the image which need not be zero.
 * Specifically, the function coordinates passed to <code>getElements()</code>
 * are calculated from the image coordinates as:
 *
 * <pre>
 * functionX = xScale*(imageX - xTrans);
 * functionY = yScale*(imageY - yTrans);
 * </pre>
 *
 * This implies that the pixel at coordinates <i>(xTrans,yTrans)</i> will
 * be assigned the value of the function at <i>(0,0)</i>.
 *
 * <p> The number of bands in the destination image must be equal to the
 * value returned by the <code>getNumElements()</code> method of the
 * <code>ImageFunction</code> unless the <code>isComplex()</code> method
 * of the <code>ImageFunction</code> returns <code>true</code> in which
 * case it will be twice that.  The data type of the destination image is
 * determined by the <code>SampleModel</code> specified by an
 * <code>ImageLayout</code> object provided via a hint.  If no layout hint
 * is provided, the data type will default to single-precision floating point.
 * The double precision floating point form of the <code>getElements()</code>
 * method of the <code>ImageFunction</code> will be invoked if and only if
 * the data type is specified to be <code>double</code>.  For all other data
 * types the single precision form of <code>getElements()</code> will be
 * invoked and the destination sample values will be clamped to the data type
 * of the image.
 *
 * <p> The width and height of the image are provided explicitely as
 * parameters.  These values override the width and height specified via
 * an <code>ImageLayout</code> if such is provided.
 *
 * <p>"ImageFunction" defines a PropertyGenerator that sets the "COMPLEX"
 * property of the image to <code>java.lang.Boolean.TRUE</code> or
 * <code>java.lang.Boolean.FALSE</code> depending on whether the
 * <code>isComplex()</code> method of the <code>ImageFunction</code>
 * parameter returns <code>true</code> or <code>false</code>, respectively.
 * This property may be retrieved by calling the <code>getProperty()</code>
 * method with "COMPLEX" as the property name.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>ImageFunction</td></tr>
 * <tr><td>LocalName</td>   <td>ImageFunction</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Generates an image from a functional description.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ImageFunctionDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The functional description.</td></tr>
 * <tr><td>arg1Desc</td>    <td>The image width.</td></tr>
 * <tr><td>arg2Desc</td>    <td>The image height.</td></tr>
 * <tr><td>arg3Desc</td>    <td>The X scale factor.</td></tr>
 * <tr><td>arg4Desc</td>    <td>The Y scale factor.</td></tr>
 * <tr><td>arg5Desc</td>    <td>The X translation.</td></tr>
 * <tr><td>arg6Desc</td>    <td>The Y translation.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>      <th>Class Type</th>
 *                        <th>Default Value</th></tr>
 * <tr><td>function</td>  <td>com.lightcrafts.mediax.jai.ImageFunction</td>
 *                        <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>width</td>     <td>java.lang.Integer</td>
 *                        <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>height</td>    <td>java.lang.Integer</td>
 *                        <td>NO_PARAMETER_DEFAULT</td>
 * <tr><td>xScale</td>    <td>java.lang.Float</td>
 *                        <td>1.0F</td>
 * <tr><td>yScale</td>    <td>java.lang.Float</td>
 *                        <td>1.0F</td>
 * <tr><td>xTrans</td>    <td>java.lang.Float</td>
 *                        <td>0.0F</td>
 * <tr><td>yTrans</td>    <td>java.lang.Float</td>
 *                        <td>0.0F</td>
 * </table></p>
 *
 * @see java.awt.geom.AffineTransform
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 * @see com.lightcrafts.mediax.jai.ImageFunction
 */
public class ImageFunctionDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "ImageFunction"},
        {"LocalName",   "ImageFunction"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("ImageFunctionDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/ImageFunctionDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("ImageFunctionDescriptor1")},
        {"arg1Desc",    JaiI18N.getString("ImageFunctionDescriptor2")},
        {"arg2Desc",    JaiI18N.getString("ImageFunctionDescriptor3")},
        {"arg3Desc",    JaiI18N.getString("ImageFunctionDescriptor4")},
        {"arg4Desc",    JaiI18N.getString("ImageFunctionDescriptor5")},
        {"arg5Desc",    JaiI18N.getString("ImageFunctionDescriptor6")},
        {"arg6Desc",    JaiI18N.getString("ImageFunctionDescriptor7")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
	com.lightcrafts.mediax.jai.ImageFunction.class,
        java.lang.Integer.class, java.lang.Integer.class,
	java.lang.Float.class, java.lang.Float.class,
        java.lang.Float.class, java.lang.Float.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "function", "width", "height", "xScale", "yScale", "xTrans", "yTrans"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT, NO_PARAMETER_DEFAULT,
        new Float(1.0F), new Float(1.0F), // unity scale
        new Float(0.0F), new Float(0.0F)  // zero translation
    };

    /** Constructor. */
    public ImageFunctionDescriptor() {
        super(resources, 0, paramClasses, paramNames, paramDefaults);
    }

    /**
     * Returns an array of <code>PropertyGenerators</code> implementing
     * property inheritance for the "ImageFunction" operation.
     *
     * @return  An array of property generators.
     */
    public PropertyGenerator[] getPropertyGenerators() {
        PropertyGenerator[] pg = new PropertyGenerator[1];
        pg[0] = new ImageFunctionPropertyGenerator();
        return pg;
    }


    /**
     * Generates an image from a functional description.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param function The functional description.
     * @param width The image width.
     * @param height The image height.
     * @param xScale The X scale factor.
     * May be <code>null</code>.
     * @param yScale The Y scale factor.
     * May be <code>null</code>.
     * @param xTrans The X translation.
     * May be <code>null</code>.
     * @param yTrans The Y translation.
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>function</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>width</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>height</code> is <code>null</code>.
     */
    public static RenderedOp create(ImageFunction function,
                                    Integer width,
                                    Integer height,
                                    Float xScale,
                                    Float yScale,
                                    Float xTrans,
                                    Float yTrans,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("ImageFunction",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setParameter("function", function);
        pb.setParameter("width", width);
        pb.setParameter("height", height);
        pb.setParameter("xScale", xScale);
        pb.setParameter("yScale", yScale);
        pb.setParameter("xTrans", xTrans);
        pb.setParameter("yTrans", yTrans);

        return JAI.create("ImageFunction", pb, hints);
    }
}
