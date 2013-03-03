/*
 * $RCSfile: AddConstToCollectionOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:12 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.CollectionImage;

/**
 * An <code>OpImage</code> implementing the "AddConstToCollection" operation.
 *
 * @see com.lightcrafts.mediax.jai.operator.AddConstToCollectionDescriptor
 * @see AddConstToCollectionCIF
 *
 *
 * @since EA4
 */
final class AddConstToCollectionOpImage extends CollectionImage {

    /**
     * Constructor.
     *
     * @param sourceCollection  A collection of rendered images.
     * @param hints  Optionally contains destination image layout.
     * @param constants  The constants to be added, stored as reference.
     */
    public AddConstToCollectionOpImage(Collection sourceCollection,
                                       RenderingHints hints,
                                       double[] constants) {
        /**
         * Try to create a new instance of the sourceCollection to be
         * used to store output images. If failed, use a Vector.
         */
        try {
            imageCollection =
                (Collection)sourceCollection.getClass().newInstance();
        } catch (Exception e) {
            imageCollection = new Vector();
        }

        Iterator iter = sourceCollection.iterator();
        while (iter.hasNext()) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(iter.next());
            pb.add(constants);

            imageCollection.add(JAI.create("AddConst", pb, hints));
        }
    }
}
