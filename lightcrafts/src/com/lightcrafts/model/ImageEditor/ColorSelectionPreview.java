/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.model.Operation;
import com.lightcrafts.model.Preview;
import com.lightcrafts.model.Region;
import com.lightcrafts.ui.LightZoneSkin;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.lang.ref.SoftReference;

import static com.lightcrafts.model.ImageEditor.Locale.LOCALE;

public class ColorSelectionPreview extends Preview implements PaintListener {
    static final boolean ADJUST_GRAYSCALE = true;
    final ImageEditorEngine engine;

    @Override
    public String getName() {
        return LOCALE.get("ColorMask_Name");
    }

    @Override
    public void setDropper(Point p) {
    }

    @Override
    public void addNotify() {
        // This method gets called when this Preview is added.
        engine.update(null, false);
        super.addNotify();
    }

    @Override
    public void removeNotify() {
        // This method gets called when this Preview is removed.
        super.removeNotify();
    }

    @Override
    public void setRegion(Region region) {
        // Fabio: only draw yellow inside the region?
    }

    private SoftReference<PlanarImage> currentImage = new SoftReference<PlanarImage>(null);
    private Rectangle visibleRect = null;
    private BufferedImage preview = null;

    @Override
    public void setSelected(Boolean selected) {
        if (!selected) {
            preview = null;
            currentImage = new SoftReference<PlanarImage>(null);
        }
    }

    @Override
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

        if (preview == null)
            return;

        AffineTransform transform = new AffineTransform();
        final int dx = (getSize().width > preview.getWidth())
                ? (getSize().width - preview.getWidth()) / 2
                : 0;
        final int dy = (getSize().height > preview.getHeight())
                ? (getSize().height - preview.getHeight()) / 2
                : 0;
        transform.setToTranslation(dx, dy);
        try {
            g.drawRenderedImage(preview, transform);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    ColorSelectionPreview(final ImageEditorEngine engine) {
        this.engine = engine;

        addComponentListener(
            new ComponentAdapter() {
                @Override
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

    @Override
    public void paintDone(PlanarImage image, Rectangle visibleRect, boolean synchronous, long time) {
        if (image == null)
            return;

        Dimension previewDimension = getSize();

/*
        assert(image.getColorModel().getColorSpace().isCS_sRGB()
              || image.getColorModel().getColorSpace() == JAIContext.systemColorSpace)
              && image.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE;
*/

        if (previewDimension.getHeight() <= 1 || previewDimension.getWidth() <= 1)
            return;

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
