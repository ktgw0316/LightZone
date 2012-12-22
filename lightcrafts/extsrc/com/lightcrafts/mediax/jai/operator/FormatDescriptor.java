/*
 * $RCSfile: FormatDescriptor.java,v $
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
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Format" operation.
 *
 * <p> The "Format" operation performs reformatting on an image.  It
 * is capable of casting the pixel values of an image to a given data
 * type, replacing the SampleModel and ColorModel of an image, and
 * restructuring the image's tile grid layout.  The pixel values of
 * the destination image are defined by the pseudocode:
 *
 * <pre>dst[x][y][b] = cast(src[x][y][b], dataType)</pre>
 *
 * where "dataType" is one of the constants TYPE_BYTE, TYPE_SHORT,
 * TYPE_USHORT, TYPE_INT, TYPE_FLOAT, or TYPE_DOUBLE from
 * <code>java.awt.image.DataBuffer</code>.
 *
 * <p> The output SampleModel, ColorModel and tile grid layout are
 * specified by passing an ImageLayout object as a RenderingHint named
 * "ImageLayout".  The output image will have a SampleModel compatible
 * with the one specified in the layout hint wherever possible;
 * however, for output data types of <code>float</code> and
 * </code>double a <code>ComponentSampleModel</code> will be used
 * regardless of the value of the hint parameter.
 *
 * <p> One of the common uses of the format operator is to cast the 
 * pixel values of an image to a given data type. In such a case, if
 * the source image provided has an <code>IndexColorModel</code>, a
 * <code>RenderingHints</code> object for
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> with the value of
 * <code>Boolean.TRUE</code> will automatically be added to the 
 * configuration <code>Map</code> for the operation. This addition 
 * will only take place if a value for the 
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been
 * provided by the user. Note that the <code>configuration</code> Map
 * is cloned before the new hint is added to it. Due to the addition 
 * of this new <code>RenderingHint</code>, using the "format" operation
 * with source(s) that have an <code>IndexColorModel</code> will cause
 * the destination to have an expanded non-<code>IndexColorModel</code>
 * <code>ColorModel</code>. This expansion ensures that the conversion 
 * to a different data type, <code>ColorModel</code> or
 * <code>SampleModel</code> happens correctly such that the indices 
 * into the color map (for <code>IndexColorModel</code> images) are
 * not treated as pixel data.  If the format operator is not being used
 * to cast the pixel values of an image to a given data type, the 
 * expansion will not take place, the resultant image will still have
 * an <code>IndexColorModel</code>.
 *
 * <p> The ImageLayout may also specify a tile grid origin and size
 * which will be respected.
 *
 * <p> The typecasting performed by the <code>Format</code> function
 * is defined by the following set of expressions, dependent on the
 * data types of the source and destination.  Casting an image to its
 * current data type is a no-op.  See <a
 * href=http://java.sun.com/docs/books/jls/html/5.doc.html#25222> The
 * Java Language Specification</a> for the definition of type
 * conversions between primitive types.
 *
 * <p> In most cases, it is not necessary to explictly perform widening
 * typecasts since they will be performed automatically by image
 * operators when handed source images having different datatypes.
 *
 * <p><table border=1>
 * <tr><th>Source Type</th> <th>Destination Type</th> <th>Action</th></tr>
 * <tr><td>BYTE</td>   <td>SHORT</td>  <td>(short)(x & 0xff)</td></tr>
 * <tr><td>BYTE</td>   <td>USHORT</td> <td>(short)(x & 0xff)</td></tr>
 * <tr><td>BYTE</td>   <td>INT</td>    <td>(int)(x & 0xff)</td></tr>
 * <tr><td>BYTE</td>   <td>FLOAT</td>  <td>(float)(x & 0xff)</td></tr>
 * <tr><td>BYTE</td>   <td>DOUBLE</td> <td>(double)(x & 0xff)</td></tr>
 * <tr><td>SHORT</td>  <td>BYTE</td>   <td>(byte)clamp((int)x, 0, 255)</td></tr>
 * <tr><td>SHORT</td>  <td>USHORT</td> <td>(short)clamp((int)x, 0, 32767)</td></tr>
 * <tr><td>SHORT</td>  <td>INT</td>    <td>(int)x</td></tr>
 * <tr><td>SHORT</td>  <td>FLOAT</td>  <td>(float)x</td></tr>
 * <tr><td>SHORT</td>  <td>DOUBLE</td> <td>(double)x</td></tr>
 * <tr><td>USHORT</td> <td>BYTE</td>   <td>(byte)clamp((int)x & 0xffff, 0, 255)</td></tr>
 * <tr><td>USHORT</td> <td>SHORT</td>  <td>(short)clamp((int)x & 0xffff, 0, 32767)</td></tr>
 * <tr><td>USHORT</td> <td>INT</td>    <td>(int)(x & 0xffff)</td></tr>
 * <tr><td>USHORT</td> <td>FLOAT</td>  <td>(float)(x & 0xffff)</td></tr>
 * <tr><td>USHORT</td> <td>DOUBLE</td> <td>(double)(x & 0xffff)</td></tr>
 * <tr><td>INT</td>    <td>BYTE</td>   <td>(byte)clamp(x, 0, 255)</td></tr>
 * <tr><td>INT</td>    <td>SHORT</td>  <td>(short)clamp(x, -32768, 32767)</td></tr>
 * <tr><td>INT</td>    <td>USHORT</td> <td>(short)clamp(x, 0, 65535)</td></tr>
 * <tr><td>INT</td>    <td>FLOAT</td>  <td>(float)x</td></tr>
 * <tr><td>INT</td>    <td>DOUBLE</td> <td>(double)x</td></tr>
 * <tr><td>FLOAT</td>  <td>BYTE</td>   <td>(byte)clamp((int)x, 0, 255)</td></tr>
 * <tr><td>FLOAT</td>  <td>SHORT</td>  <td>(short)clamp((int)x, -32768, 32767)</td></tr>
 * <tr><td>FLOAT</td>  <td>USHORT</td> <td>(short)clamp((int)x, 0, 65535)</td></tr>
 * <tr><td>FLOAT</td>  <td>INT</td>    <td>(int)x</td></tr>
 * <tr><td>FLOAT</td>  <td>DOUBLE</td> <td>(double)x</td></tr>
 * <tr><td>DOUBLE</td> <td>BYTE</td>   <td>(byte)clamp((int)x, 0, 255)</td></tr>
 * <tr><td>DOUBLE</td> <td>SHORT</td>  <td>(short)clamp((int)x, -32768, 32767)</td></tr>
 * <tr><td>DOUBLE</td> <td>USHORT</td> <td>(short)clamp((int)x, 0, 65535)</td></tr>
 * <tr><td>DOUBLE</td> <td>INT</td>    <td>(int)x</td></tr>
 * <tr><td>DOUBLE</td> <td>FLOAT</td>  <td>(float)x</td></tr>
 * </table></p>
 *
 * The <code>clamp</code> function may be defined as:
 * <pre>
 * int clamp(int x, int low, int high) {
 *     return (x < low) ? low : ((x > high) ? high : x);
 * }
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Format</td></tr>
 * <tr><td>LocalName</td>   <td>Format</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Reformats an image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/FormatDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The output data type (from java.awt.image.DataBuffer).</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>      <th>Class Type</th>
 *                        <th>Default Value</th></tr>
 * <tr><td>dataType</td>  <td>java.lang.Integer</td>
 *                        <td>DataBuffer.TYPE_BYTE</td>
 * </table></p>
 *
 * @see java.awt.image.DataBuffer
 * @see com.lightcrafts.mediax.jai.ImageLayout
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class FormatDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Format"},
        {"LocalName",   "Format"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("FormatDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/FormatDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
	{"arg0Desc",    "The output data type (from java.awt.image.DataBuffer)."}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        java.lang.Integer.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "dataType"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        new Integer(DataBuffer.TYPE_BYTE)
    };

    /** Constructor. */
    public FormatDescriptor() {
        super(resources, 1, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.
     */
    public Number getParamMinValue(int index) {
        if (index == 0) {
            return new Integer(DataBuffer.TYPE_BYTE);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Returns the maximum legal value of a specified numeric parameter
     * for this operation.
     */
    public Number getParamMaxValue(int index) {
        if (index == 0) {
            return new Integer(DataBuffer.TYPE_DOUBLE);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }


    /**
     * Reformats an image.
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
     * @param dataType The output data type (from java.awt.image.DataBuffer).
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    Integer dataType,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Format",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("dataType", dataType);

        return JAI.create("Format", pb, hints);
    }

    /**
     * Reformats an image.
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
     * @param dataType The output data type (from java.awt.image.DataBuffer).
     * May be <code>null</code>.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                Integer dataType,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Format",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("dataType", dataType);

        return JAI.createRenderable("Format", pb, hints);
    }
}
