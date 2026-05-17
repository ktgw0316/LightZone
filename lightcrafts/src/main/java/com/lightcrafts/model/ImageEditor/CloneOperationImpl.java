/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2026-     Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.LCROIShape;
import com.lightcrafts.jai.utils.Transform;
import com.lightcrafts.model.*;
import com.lightcrafts.ui.editor.EditorMode;
import org.eclipse.imagen.*;
import org.eclipse.imagen.media.border.BorderDescriptor;
import org.eclipse.imagen.media.crop.CropDescriptor;
import org.eclipse.imagen.media.format.FormatDescriptor;
import org.eclipse.imagen.media.translate.TranslateDescriptor;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.renderable.ParameterBlock;
import java.util.Collection;
import java.util.Collections;

import static com.lightcrafts.ui.help.HelpConstants.HELP_TOOL_CLONE;

public class CloneOperationImpl extends BlendedOperation implements CloneOperation {
    CloneOperationImpl(Rendering rendering) {
        super(rendering, type);
        setHelpTopic(HELP_TOOL_CLONE);
    }

    public EditorMode getPreferredMode() {
        return EditorMode.REGION;
    }

    @Override
    public boolean neutralDefault() {
        return true;
    }

    @Override
    public void setRegionInverted(boolean inverted) {
        // Inverted regions have no meaning for the Clone Tool
        // super.setRegionInverted(inverted);
    }

    static final OperationType type = new OperationTypeImpl("Clone");

    static PlanarImage buildCloner(Region region, Rendering rendering, PlanarImage back) {
        PlanarImage image = back;

        Collection<Contour> contours = region.getContours();
        for (final Contour c : contours) {
            // Protect from clone copy bug
            // TODO: Anton fixme
            if (!(c instanceof CloneContour cloneContour))
                continue;

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

            final int dx = (int) (target.getX() - source.getX());
            final int dy = (int) (target.getY() - source.getY());

            RenderedOp translated = TranslateDescriptor.create(back, (float) dx, (float) dy, null, JAIContext.noCacheHint);
            RenderedOp border = BorderDescriptor.create(
                    translated, Math.max(dx, 0), Math.max(-dx, 0), Math.max(dy, 0), Math.max(-dy, 0),
                    BorderExtender.createInstance(BorderExtender.BORDER_ZERO), JAIContext.noCacheHint);
            RenderedOp crop = CropDescriptor.create(border,
                    (float) back.getMinX(), (float) back.getMinY(),
                    (float) back.getWidth(), (float) back.getHeight(),
                    null, null, null, JAIContext.noCacheHint);

            // Format retiles the image
            // TODO: this needs better understanding, can we just specify the layout for Crop?
            final var dataType = back.getSampleModel().getDataType();
            final var formatHints = new RenderingHints(ImageN.KEY_IMAGE_LAYOUT, new ImageLayout(back));
            RenderedOp formatted = FormatDescriptor.create(crop, dataType, formatHints);

            Region r = new Region() {
                @Override
                public Collection<Contour> getContours() {
                    return Collections.singleton(contour);
                }

                @Override
                public Shape getOuterShape() {
                    return contour.getOuterShape();
                }

                @Override
                public float getWidth() {
                    return contour.getWidth();
                }

                @Override
                public Point2D getTranslation() {
                    return contour.getTranslation();
                }
            };

            LCROIShape mask = new LCROIShape(r, rendering.getInputTransform());

            final var pb = new ParameterBlock();
            pb.addSource(formatted)
              .addSource(image)
              .add("Normal")
              .add(1.0)
              .add(mask);

            // RenderingHints blendHints = new RenderingHints(ImageN.KEY_IMAGE_LAYOUT, new ImageLayout(back));
            // NOTE: I guess that it is more efficient to cache the different cloned areas...
            // blendHints.add(JAIContext.noCacheHint);
            image = ImageN.create("Blend", pb, null);
        }

        return image;
    }

    private class Cloner extends BlendedTransform {
        Cloner(PlanarImage source) {
            super(source);
        }

        @Override
        public PlanarImage setFront() {
            if (getRegion() != null)
                return buildCloner(getRegion(), rendering, back);
            else
                return back;
        }
    }

    @Override
    protected void updateOp(Transform op) {
        op.update();
    }

    @Override
    protected BlendedTransform createBlendedOp(PlanarImage source) {
        return new Cloner(source);
    }

    @Override
    public OperationType getType() {
        return type;
    }
}
