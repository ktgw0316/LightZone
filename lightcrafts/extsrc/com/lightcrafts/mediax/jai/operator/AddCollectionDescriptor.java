/*
 * $RCSfile: AddCollectionDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:28 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.operator;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import java.util.Collection;
import java.util.Iterator;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptorImpl;
import com.lightcrafts.mediax.jai.ParameterBlockJAI;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;

/**
 * An <code>OperationDescriptor</code> describing the
 * "AddCollection" operation.
 *
 * <p> The AddCollection operation takes a collection of rendered
 * or renderable source images, and adds every set of pixels, one from
 * each source image of the corresponding position and band. No
 * additional parameters are required.
 *
 * <p> There is no restriction on the actual class type used to
 * represent the source collection, but all elements of the collection
 * must be instances of <code>RenderedImage</code> or
 * <code>RenderableImage</code> depending on the mode. The number of
 * images in the collection may vary from 2 to n.  The source images
 * may have different numbers of bands and data types.
 *
 * <p> By default, the destination image bounds are the intersection
 * of all of the source image bounds. If any of the two sources are
 * completely disjoint, the destination will have a width and a height
 * of 0. The number of bands of the destination image is equal to
 * the minimum number of bands of all the sources, and the data type is
 * the biggest data type of all the sources. If the result of the
 * operation underflows/overflows the minimum/maximum value supported
 * by the destination data type, then it will be clamped to the
 * minimum/maximum value respectively.
 *
 * <p> The destination pixel values are defined by the pseudocode:
 * <pre>
 * dst[x][y][b] = 0;
 * for (int i = 0; i < numSources; i++) {
 *     dst[x][y][b] += srcs[i][x][y][b];
 * }
 * </pre>
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>GlobalName</td>  <td>AddCollection</td></tr>
 * <tr><td>LocalName</td>   <td>AddCollection</td></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>Adds a collection of rendered images.</td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/AddCollectionDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.0</td></tr>
 * </table></p>
 *
 * <p> No parameters are needed for this operation.
 *
 * @see java.awt.image.RenderedImage
 * @see java.awt.image.renderable.RenderableImage
 * @see java.util.Collection
 * @see com.lightcrafts.mediax.jai.OperationDescriptor
 */
public class AddCollectionDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation
     * and specify the parameter list for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName",  "AddCollection"},
        {"LocalName",   "AddCollection"},
        {"Vendor",      "com.lightcrafts.media.jai"},
        {"Description", JaiI18N.getString("AddCollectionDescriptor0")},
        {"DocURL",      "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/AddCollectionDescriptor.html"},
        {"Version",     JaiI18N.getString("DescriptorVersion")}
    };

    /** The source class list for this operation. */
    private static final Class[][] sourceClasses = {
        { java.util.Collection.class },
        { java.util.Collection.class }
    };

    private static final String[] supportedModes = {
	"rendered",
	"renderable"
    };

    /** Constructor. */
    public AddCollectionDescriptor() {
        super(resources, supportedModes, null, sourceClasses,
			    (ParameterListDescriptor)null);
    }

    /** Validates input source collection. */
    protected boolean validateSources(String modeName,
				      ParameterBlock args,
                                      StringBuffer msg) {
        if (!super.validateSources(modeName, args, msg)) {
            return false;
        }

        Collection col = (Collection)args.getSource(0);

        if (col.size() < 2) {
            msg.append(getName() + " " +
                       JaiI18N.getString("AddCollectionDescriptor1"));
            return false;
        }

        Iterator iter = col.iterator();
        if (modeName.equalsIgnoreCase(RenderedRegistryMode.MODE_NAME)) {
            while (iter.hasNext()) {
                Object o = iter.next();
                if (!(o instanceof RenderedImage)) {
                    msg.append(getName() + " " +
                               JaiI18N.getString("AddCollectionDescriptor2"));
                    return false;
                }
            }
        } else if (modeName.equalsIgnoreCase(RenderableRegistryMode.MODE_NAME)) {
            while (iter.hasNext()) {
                Object o = iter.next();
                if (!(o instanceof RenderableImage)) {
                    msg.append(getName() + " " +
                               JaiI18N.getString("AddCollectionDescriptor3"));
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Adds a collection of images.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#create(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderedOp
     *
     * @param source0 <code>Collection</code> source 0.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderedOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderedOp create(Collection source0,
                                    RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("AddCollection",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        return JAI.create("AddCollection", pb, hints);
    }

    /**
     * Adds a collection of images.
     *
     * <p>Creates a <code>ParameterBlockJAI</code> from all
     * supplied arguments except <code>hints</code> and invokes
     * {@link JAI#createRenderable(String,ParameterBlock,RenderingHints)}.
     *
     * @see JAI
     * @see ParameterBlockJAI
     * @see RenderableOp
     *
     * @param source0 <code>Collection</code> source 0.
     * @param hints The <code>RenderingHints</code> to use.
     * May be <code>null</code>.
     * @return The <code>RenderableOp</code> destination.
     * @throws IllegalArgumentException if <code>source0</code> is <code>null</code>.
     */
    public static RenderableOp createRenderable(Collection source0,
                                                RenderingHints hints)  {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("AddCollection",
                                  RenderableRegistryMode.MODE_NAME);

        pb.setSource("source0", source0);

        return JAI.createRenderable("AddCollection", pb, hints);
    }
}
