/*
 * $RCSfile: LookupDescriptor.java,v $
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

import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.LookupTableJAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the "Lookup" operation.
 *
 * <p> The Lookup operation takes a rendered or renderable image and a
 * lookup table, and performs general table lookup by passing the
 * source image through the table.
 *
 * <p> The source may be a single- or multi-banded image of data types
 * <code>byte</code>, <code>ushort</code>, <code>short</code>, or
 * <code>int</code>. The lookup table may be single- or multi-banded
 * and of any JAI supported data types. The destination image must have
 * the same data type as the lookup table, and its number of bands is
 * determined based on the number of bands of the source and the table.
 * If the source is single-banded, the destination has the same number
 * of bands as the lookup table; otherwise, the destination has the
 * same number of bands as the source.
 *
 * <p> If either the source or the table is single-banded and the other
 * one is multi-banded, then the single band is applied to every band
 * of the multi-banded object. If both are multi-banded, then their
 * corresponding bands are matched up.
 *
 * <p> The table may have a set of offset values, one for each band. This
 * value is subtracted from the source pixel values before indexing into
 * the table data array.
 *
 * <p> It is the user's responsibility to make certain the lookup table
 * supplied is suitable for the source image. Specifically, the table
 * data covers the entire range of the source data. Otherwise, the result
 * of this operation is undefined.
 *
 * <p >By the nature of this operation, the destination may have a
 * different number of bands and/or data type from the source. The
 * <code>SampleModel</code> of the destination is created in accordance
 * with the actual lookup table used in a specific case.
 *
 * <p> The destination pixel values are defined by the pseudocode:
 * <ul>
 * <li> If the source image is single-banded and the lookup table is
 * single- or multi-banded, then the destination image has the same
 * number of bands as the lookup table:
 * <pre>
 * dst[x][y][b] = table[b][src[x][y][0] - offsets[b]]
 * </pre>
 * </li>
 *
 * <li> If the source image is multi-banded and the lookup table is
 * single-banded, then the destination image has the same number of
 * bands as the source image:
 * <pre>
 * dst[x][y][b] = table[0][src[x][y][b] - offsets[0]]
 * </pre>
 * </li>
 *
 * <li> If the source image is multi-banded and the lookup table is
 * multi-banded, with the same number of bands as the source image,
 * then the destination image will have the same number of bands as the
 * source image:
 * <pre>
 * dst[x][y][b] = table[b][src[x][y][b] - offsets[b]]
 * </pre>
 * </li>
 * </ul>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>Lookup</td></tr>
 * <tr><td>LocalName</td>   <td>Lookup</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Performs general table lookup on an
 *                              image.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/LookupDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * <tr><td>arg0Desc</td>    <td>The lookup table the source image
 *                              is passed through.</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>  <th>Class Type</th>
 *                    <th>Default Value</th></tr>
 * <tr><td>table</td> <td>com.lightcrafts.mediax.jai.LookupTableJAI</td>
 *                    <td>NO_PARAMETER_DEFAULT</td>
 * </table></p>
 *
 * @see com.lightcrafts.mediax.jai.LookupTableJAI
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class LookupDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "Lookup"},
        {"LocalName",   "Lookup"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("LookupDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/LookupDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")},
        {"arg0Desc",    JaiI18N.getString("LookupDescriptor1")}
    };

    /** The parameter class list for this operation. */
    private static final Class[] paramClasses = {
        com.lightcrafts.mediax.jai.LookupTableJAI.class
    };

    /** The parameter name list for this operation. */
    private static final String[] paramNames = {
        "table"
    };

    /** The parameter default value list for this operation. */
    private static final Object[] paramDefaults = {
        NO_PARAMETER_DEFAULT
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public LookupDescriptor() {
        super(resources, supportedModes, 1,
		paramNames, paramClasses, paramDefaults, null);
    }

    /**
     * Validates the input source.
     *
     * <p> In addition to the standard checks performed by the
     * superclass method, this method checks that the source image
     * is of integral data type.
     */
    protected boolean validateSources(String modeName,
				      ParameterBlock args,
                                      StringBuffer msg) {
        if (!super.validateSources(modeName, args, msg)) {
            return false;
        }

	if (!modeName.equalsIgnoreCase("rendered"))
	    return true;

	RenderedImage src = args.getRenderedSource(0);

        int dtype = src.getSampleModel().getDataType();

        if (dtype != DataBuffer.TYPE_BYTE &&
            dtype != DataBuffer.TYPE_USHORT &&
            dtype != DataBuffer.TYPE_SHORT &&
            dtype != DataBuffer.TYPE_INT) {
            msg.append(getName() + " " +
                       JaiI18N.getString("LookupDescriptor2"));
            return false;
        }

        return true;
    }


    /**
     * Performs general table lookup on an image.
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
     * @param table The lookup table the source image is passed through.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>table</code> is <code>null</code>.
     */
    public static RenderedOp create(RenderedImage source0,
                                    LookupTableJAI table,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Lookup",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("table", table);

        return JAI.create("Lookup", pb, hints);
    }

    /**
     * Performs general table lookup on an image.
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
     * @param table The lookup table the source image is passed through.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>table</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(RenderableImage source0,
                                                LookupTableJAI table,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("Lookup",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        pb.setParameter("table", table);

        return JAI.createRenderable("Lookup", pb, hints);
    }
}
