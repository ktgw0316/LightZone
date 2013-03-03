/*
 * $RCSfile: AreaOpPropertyGenerator.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:59 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.util;

import java.awt.Rectangle;
import java.awt.image.renderable.ParameterBlock;
import com.lightcrafts.mediax.jai.AreaOpImage;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.ROIShape;

public class AreaOpPropertyGenerator extends PropertyGeneratorImpl {
    /** Constructor. */
    public AreaOpPropertyGenerator() {
        super(new String[] {"ROI"},
              new Class[] {ROI.class},
              new Class[] {RenderedOp.class});
    }

    /**
     * Returns the specified property in the rendered layer.
     *
     * @param name   Property name.
     * @param opNode Operation node.
     */
    public Object getProperty(String name, Object opNode) {
        validate(name, opNode);

        if(opNode instanceof RenderedOp &&
           name.equalsIgnoreCase("roi")) {
            RenderedOp op = (RenderedOp)opNode;

            ParameterBlock pb = op.getParameterBlock();

            // Retrieve the rendered source image and its ROI.
            PlanarImage src = (PlanarImage)pb.getRenderedSource(0);
            Object roiProperty = src.getProperty("ROI");
            if(roiProperty == null ||
               roiProperty == java.awt.Image.UndefinedProperty ||
               !(roiProperty instanceof ROI)) {
                return java.awt.Image.UndefinedProperty;
            }
            ROI roi = (ROI)roiProperty;

            // Determine the effective destination bounds.
            Rectangle dstBounds = null;
            PlanarImage dst = op.getRendering();
            if(dst instanceof AreaOpImage &&
               ((AreaOpImage)dst).getBorderExtender() == null) {
                AreaOpImage aoi = (AreaOpImage)dst;
                dstBounds =
                    new Rectangle(aoi.getMinX() + aoi.getLeftPadding(),
                                  aoi.getMinY() + aoi.getTopPadding(),
                                  aoi.getWidth() -
                                  aoi.getLeftPadding() -
                                  aoi.getRightPadding(),
                                  aoi.getHeight() -
                                  aoi.getTopPadding() -
                                  aoi.getBottomPadding());
            } else {
                dstBounds = dst.getBounds();
            }

            // If necessary, clip the ROI to the destination bounds.
            // XXX Is this desirable?
            if(!dstBounds.contains(roi.getBounds())) {
                roi = roi.intersect(new ROIShape(dstBounds));
            }

            return roi;
        }

        return java.awt.Image.UndefinedProperty;
    }
}
