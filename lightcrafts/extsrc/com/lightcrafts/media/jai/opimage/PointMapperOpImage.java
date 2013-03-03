/*
 * $RCSfile: PointMapperOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/11/21 22:49:40 $
 * $State: Exp $
 */

package com.lightcrafts.media.jai.opimage;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Map;
import com.lightcrafts.mediax.jai.NullOpImage;
import com.lightcrafts.mediax.jai.PlanarImage;

/**
 * A class which merely wraps another <code>PlanarImage</code> but
 * uses a supplied <code>AffineTransform</code> object for point mapping.
 */
public class PointMapperOpImage extends NullOpImage {
    private AffineTransform transform;
    private AffineTransform inverseTransform;

    public PointMapperOpImage(PlanarImage source, Map configuration,
                              AffineTransform transform)
        throws NoninvertibleTransformException {
        super(source, null, configuration, OP_COMPUTE_BOUND);

        if(transform == null) {
            throw new IllegalArgumentException("transform == null!");
        }
        
        this.transform = transform;
        this.inverseTransform = transform.createInverse();
    }

    public Point2D mapDestPoint(Point2D destPt, int sourceIndex) {
        if(sourceIndex != 0) {
            throw new IndexOutOfBoundsException("sourceIndex != 0!");
        }

        return inverseTransform.transform(destPt, null);
    }

    public Point2D mapSourcePoint(Point2D sourcePt, int sourceIndex) {
        if(sourceIndex != 0) {
            throw new IndexOutOfBoundsException("sourceIndex != 0!");
        }

        return inverseTransform.transform(sourcePt, null);
    }

    public synchronized void dispose() {
        getSourceImage(0).dispose();
        super.dispose();
    }
}
