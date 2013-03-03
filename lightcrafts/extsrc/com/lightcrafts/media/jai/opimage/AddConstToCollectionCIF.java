/*
 * $RCSfile: AddConstToCollectionCIF.java,v $
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import com.lightcrafts.mediax.jai.CollectionImage;
import com.lightcrafts.mediax.jai.CollectionImageFactory;
import com.lightcrafts.mediax.jai.CollectionOp;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RenderedOp;

/**
 * A <code>CIF</code> supporting the "AddConstToCollection" operation.
 *
 * @see com.lightcrafts.mediax.jai.operator.AddConstToCollectionDescriptor
 * @see AddConstToCollectionOpImage
 *
 *
 * @since EA4
 */
public class AddConstToCollectionCIF implements CollectionImageFactory {

    /** Constructor. */
    public AddConstToCollectionCIF() {}

    /**
     * Creates a new instance of <code>AddConstToCollectionOpImage</code>.
     *
     * @param args   Input source collection and constants
     * @param hints  Optionally contains destination image layout.
     */
    public CollectionImage create(ParameterBlock args,
                                  RenderingHints hints) {
        return new AddConstToCollectionOpImage(
                   (Collection)args.getSource(0),
                   hints,
                   (double[])args.getObjectParameter(0));
    }

    /**
     * Updates an instance of <code>AddConstToCollectionOpImage</code>.
     */
    public CollectionImage update(ParameterBlock oldParamBlock,
                                  RenderingHints oldHints,
                                  ParameterBlock newParamBlock,
                                  RenderingHints newHints,
                                  CollectionImage oldRendering,
                                  CollectionOp op) {
        CollectionImage updatedCollection = null;

        if(oldParamBlock.getObjectParameter(0).equals(newParamBlock.getObjectParameter(0)) &&
           (oldHints == null ? newHints == null : oldHints.equals(newHints))) {

            // Retrieve the old and new sources and the parameters.
            Collection oldSource = (Collection)oldParamBlock.getSource(0);
            Collection newSource = (Collection)newParamBlock.getSource(0);
            double[] constants = (double[])oldParamBlock.getObjectParameter(0);

            // Construct a Collection of common sources.
            Collection commonSources = new ArrayList();
            Iterator it = oldSource.iterator();
            while(it.hasNext()) {
                Object oldElement = it.next();
                if(newSource.contains(oldElement)) {
                    commonSources.add(oldElement);
                }
            }

            if(commonSources.size() != 0) {
                // Construct a Collection of the RenderedOp nodes that
                // will be retained in the new CollectionImage.
                ArrayList commonNodes = new ArrayList(commonSources.size());
                it = oldRendering.iterator();
                while(it.hasNext()) {
                    RenderedOp node = (RenderedOp)it.next();
                    PlanarImage source = (PlanarImage)node.getSourceImage(0);
                    if(commonSources.contains(source)) {
                        commonNodes.add(node);
                    }
                }

                // Create a new CollectionImage.
                updatedCollection =
                    new AddConstToCollectionOpImage(newSource, newHints,
                                                    constants);

                // Remove from the new CollectionImage all nodes that
                // are common with the old CollectionImage.
                ArrayList newNodes = new ArrayList(oldRendering.size() -
                                                   commonSources.size());
                it = updatedCollection.iterator();
                while(it.hasNext()) {
                    RenderedOp node = (RenderedOp)it.next();
                    PlanarImage source = (PlanarImage)node.getSourceImage(0);
                    if(commonSources.contains(source)) {
                        it.remove();
                    }
                }

                // Add all the common nodes to the new CollectionImage.
                it = commonNodes.iterator();
                while(it.hasNext()) {
                    updatedCollection.add(it.next());
                }
            }
        }

        return updatedCollection;
    }
}
