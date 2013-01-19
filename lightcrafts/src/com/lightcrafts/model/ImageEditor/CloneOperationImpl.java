/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.LCROIShape;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.*;

import javax.media.jai.*;
import com.lightcrafts.ui.editor.EditorMode;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.renderable.ParameterBlock;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class CloneOperationImpl extends BlendedOperation implements CloneOperation {
    public CloneOperationImpl(Rendering rendering) {
        super(rendering, type);
    }

    public EditorMode getPreferredMode() {
        return EditorMode.REGION;
    }

    public boolean neutralDefault() {
        return true;
    }

    public void setRegionInverted(boolean inverted) {
        // Inverted regions have no meaning for the Clone Tool
        // super.setRegionInverted(inverted);
    }

    static final OperationType type = new OperationTypeImpl("Clone");

    static PlanarImage buildCloner(Region region, Rendering rendering, PlanarImage back) {
        PlanarImage image = back;

        Collection contours = region.getContours();
        for (Iterator i = contours.iterator(); i.hasNext();) {
            Contour c = (Contour) i.next();
            // Protect from clone copy bug
            // TODO: Anton fixme
            if (!(c instanceof CloneContour))
                continue;

            CloneContour cloneContour = (CloneContour) c;

            // This is the mask for the clone operation:
            final Contour contour = cloneContour;
            final Point2D translation = contour.getTranslation();

            // And this is the extra point that says where to clone from:
            final Point2D clonePoint = cloneContour.getClonePoint();

            Point2D source = new Point2D.Double(clonePoint.getX() + (translation != null ? translation.getX() : 0),
                                                clonePoint.getY() + (translation != null ? translation.getY() : 0));

            source = rendering.getInputTransform().transform(source, null);

            final Rectangle bounds = contour.getOuterShape().getBounds();
            Point2D target = new Point2D.Double(bounds.getCenterX() + (translation != null ? translation.getX() : 0),
                                                bounds.getCenterY() + (translation != null ? translation.getY() : 0));
            target = rendering.getInputTransform().transform(target, null);

            int dx = (int) (target.getX() - source.getX());
            int dy = (int) (target.getY() - source.getY());

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(back);
            pb.add((float) dx);
            pb.add((float) dy);
            RenderedOp translated = JAI.create("Translate", pb, JAIContext.noCacheHint);

            pb = new ParameterBlock();
            pb.addSource(translated);
            pb.add((int) (dx > 0 ? dx : 0));
            pb.add((int) (dx < 0 ? -dx : 0));
            pb.add((int) (dy > 0 ? dy : 0));
            pb.add((int) (dy < 0 ? -dy : 0));
            pb.add(BorderExtender.createInstance(BorderExtender.BORDER_ZERO));
            RenderedOp border = JAI.create("Border", pb, JAIContext.noCacheHint);

            pb = new ParameterBlock();
            pb.addSource(border);
            pb.add((float) back.getMinX());
            pb.add((float) back.getMinY());
            pb.add((float) back.getWidth());
            pb.add((float) back.getHeight());
            RenderedOp crop = JAI.create("Crop", pb, JAIContext.noCacheHint);

            // Format retiles the image
            // TODO: this needs better understanding, can we just specify the layout for Crop?
            pb = new ParameterBlock();
            pb.addSource(crop);
            pb.add(back.getSampleModel().getDataType());
            RenderingHints formatHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, new ImageLayout(back));
            formatHints.add(JAIContext.noCacheHint);
            RenderedOp formatted = JAI.create("Format", pb, formatHints);

            Region r = new Region() {
                public Collection getContours() {
                    return Collections.singleton(contour);
                }

                public Shape getOuterShape() {
                    return contour.getOuterShape();
                }

                public float getWidth() {
                    return contour.getWidth();
                }

                public Point2D getTranslation() {
                    return contour.getTranslation();
                }
            };

            LCROIShape mask = new LCROIShape(r, rendering.getInputTransform());

            pb = new ParameterBlock();
            pb.addSource(formatted);
            pb.addSource(image);
            pb.add("Normal");
            pb.add(new Double(1));
            pb.add(mask);

            // RenderingHints blendHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, new ImageLayout(back));
            // NOTE: I guess that it is more efficient to cache the different cloned areas...
            // blendHints.add(JAIContext.noCacheHint);
            image = JAI.create("Blend", pb, null);
        }

        return image;
    }

    class Cloner extends BlendedTransform {
        Cloner(PlanarImage source) {
            super(source);
        }

        public PlanarImage setFront() {
            if (getRegion() != null)
                return buildCloner(getRegion(), rendering, back);
            else
                return back;
        }
    }

    protected void updateOp(Transform op) {
        op.update();
    }

    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new Cloner(source);
    }

    public OperationType getType() {
        return type;
    }
}
