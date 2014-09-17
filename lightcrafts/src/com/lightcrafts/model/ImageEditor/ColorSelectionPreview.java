/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import static com.lightcrafts.model.ImageEditor.Locale.LOCALE;
import com.lightcrafts.model.*;
import com.lightcrafts.jai.utils.*;
import com.lightcrafts.jai.JAIContext;

import com.lightcrafts.mediax.jai.*;
import com.lightcrafts.ui.LightZoneSkin;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.geom.AffineTransform;
import java.lang.ref.SoftReference;

public class ColorSelectionPreview extends Preview implements PaintListener {
    static final boolean ADJUST_GRAYSCALE = true;
    final ImageEditorEngine engine;

    public String getName() {
        return LOCALE.get("ColorMask_Name");
    }

    public void setDropper(Point p) {
    }

    public void addNotify() {
        // This method gets called when this Preview is added.
        engine.update(null, false);
        super.addNotify();
    }

    public void removeNotify() {
        // This method gets called when this Preview is removed.
        super.removeNotify();
    }

    public void setRegion(Region region) {
        // Fabio: only draw yellow inside the region?
    }

    private SoftReference<PlanarImage> currentImage = new SoftReference<PlanarImage>(null);
    private Rectangle visibleRect = null;
    private BufferedImage preview = null;

    public void setSelected(Boolean selected) {
        if (!selected) {
            preview = null;
            currentImage = new SoftReference<PlanarImage>(null);
        }
    }
    
    protected void paintComponent(Graphics graphics) {
        // Fill in the background:
        Graphics2D g = (Graphics2D) graphics;
        Shape clip = g.getClip();
        g.setColor(LightZoneSkin.Colors.NeutralGray);
        g.fill(clip);

        if (preview == null) {
            PlanarImage image = currentImage.get();

            if (image == null)
                engine.update(null, false);
            else if (visibleRect != null && getHeight() > 1 && getWidth() > 1)
                preview = cropScaleGrayscale(visibleRect, image);
        }

        if (preview != null) {
            int dx, dy;
            AffineTransform transform = new AffineTransform();
            if (getSize().width > preview.getWidth())
                dx = (getSize().width - preview.getWidth()) / 2;
            else
                dx = 0;
            if (getSize().height > preview.getHeight())
                dy = (getSize().height - preview.getHeight()) / 2;
            else
                dy = 0;
            transform.setToTranslation(dx, dy);
            try {
                g.drawRenderedImage(preview, transform);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    ColorSelectionPreview(final ImageEditorEngine engine) {
        this.engine = engine;

        addComponentListener(
            new ComponentAdapter() {
                public void componentResized(ComponentEvent event) {
                    if (isShowing()) {
                        engine.update(null, false);
                    }
                }
            }
        );
    }

    private BufferedImage cropScaleGrayscale(Rectangle visibleRect, RenderedImage image) {
        Rectangle bounds = new Rectangle(image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());

        visibleRect = bounds.intersection(visibleRect);

        if (bounds.contains(visibleRect)) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add((float) visibleRect.x);
            pb.add((float) visibleRect.y);
            pb.add((float) visibleRect.width);
            pb.add((float) visibleRect.height);
            image = JAI.create("Crop", pb, JAIContext.noCacheHint);
        }

        Dimension previewSize = getSize();

        if (visibleRect.width > previewSize.width || visibleRect.height > previewSize.height) {
            float scale = Math.min(previewSize.width / (float) visibleRect.width, previewSize.height / (float) visibleRect.height);

            ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(scale);
            pb.add(scale);
            image = JAI.create("Scale", pb, JAIContext.noCacheHint);
        }

        image = Functions.toColorSpace(image, JAIContext.systemColorSpace, null);

        if (image.getSampleModel().getDataType() == DataBuffer.TYPE_USHORT)
            image = Functions.fromUShortToByte(image, null);

        return Functions.toFastBufferedImage(image);
    }

    public void paintDone(PlanarImage image, Rectangle visibleRect, boolean synchronous, long time) {
        if (image != null) {
            Dimension previewDimension = getSize();

/*
            assert (image.getColorModel().getColorSpace().isCS_sRGB()
                    || image.getColorModel().getColorSpace() == JAIContext.systemColorSpace)
                   && image.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE;
*/

            if (previewDimension.getHeight() > 1 && previewDimension.getWidth() > 1) {
                Operation op = engine.getSelectedOperation();
                if (op != null && op instanceof BlendedOperation && op.isActive() && !op.getColorSelection().isAllSelected()) {
                    PlanarImage selectionMask = ((BlendedOperation) op).getColorSelectionMask();
                    if (selectionMask != null)
                        image = selectionMask;
                }
                this.visibleRect = visibleRect;
                currentImage = new SoftReference<PlanarImage>(image);
                preview = null;
                repaint();
            }
        }
    }
}
