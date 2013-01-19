/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import javax.swing.*;
import javax.swing.border.Border;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.renderable.ParameterBlock;
import java.awt.print.PageFormat;
import java.io.File;
import java.io.IOException;
import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

// Show a scaled image inside a rectangle representing paper bounds for
// printing.

class PreviewComponent extends JComponent {

    private static final Dimension PreferredSize = new Dimension(400, 200);

    private BufferedImage image;
    private PageFormat format;
    private Rectangle2D rect;

    PreviewComponent(BufferedImage image, PageFormat format) {
        this.image = image;
        this.format = format;
        setOpaque(false);
        Border empty = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        setBorder(empty);
        setPreferredSize(PreferredSize);
    }

    void setPageFormat(PageFormat format) {
        this.format = format;
        repaint();
    }

    void setImageRect(Rectangle2D rect) {
        this.rect = (Rectangle2D) rect.clone();
        repaint();
    }

    public boolean imageUpdate(
        Image img, final int infoflags, int x, int y, final int w, final int h
    ) {
        boolean result = super.imageUpdate(img, infoflags, x, y, w, h);
        if (img.equals(image)) {
            if ((infoflags & (WIDTH | HEIGHT)) != 0) {
                revalidate();
                repaint();
            }
        }
        return result;
    }

    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;

        double paperW = format.getWidth();
        double paperH = format.getHeight();

        double imageX = rect.getX();
        double imageY = rect.getY();
        double imageW = rect.getWidth();
        double imageH = rect.getHeight();

        // Fill the paper:
        Point paperUL = paperToScreen(0, 0);
        Point paperLR = paperToScreen(paperW, paperH);
        Rectangle paperBounds = new Rectangle(
            paperUL.x, paperUL.y, paperLR.x - paperUL.x, paperLR.y - paperUL.y
        );
        Color oldColor = g.getColor();
        g.setColor(Color.white);
        g.fill(paperBounds);

        // Draw the imageable area:
        Point imageableUL = paperToScreen(
            format.getImageableX(), format.getImageableY()
        );
        Point imageableLR = paperToScreen(
            format.getImageableX() + format.getImageableWidth(),
            format.getImageableY() + format.getImageableHeight()
        );
        Rectangle imageableArea = new Rectangle(
            imageableUL.x,
            imageableUL.y,
            imageableLR.x - imageableUL.x,
            imageableLR.y - imageableUL.y
        );
        g.setColor(Color.lightGray);
        g.draw(imageableArea);
        g.setColor(oldColor);

        // Set the clip to the imageable area:
        Shape oldClip = g.getClip();
        g.setClip(imageableArea);

        // Determine the image boundary:
        Point imageUL = paperToScreen(imageX, imageY);
        Point imageLR = paperToScreen(imageX + imageW, imageY + imageH);
        Rectangle imageArea = new Rectangle(
            imageUL.x, imageUL.y, imageLR.x - imageUL.x, imageLR.y - imageUL.y
        );
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Draw the image:
        Rectangle imageBounds = getImageBounds(imageArea);
        double scale = Math.min(imageBounds.width / (double) image.getWidth(), imageBounds.height / (double) image.getHeight());

        /* g.drawImage(
            image,
            imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height,
            this
        ); */

        // We use JAI to do the scaling because drawImage with a transform sometimes fails on windows
        if (scale > 0) {
            AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
            transform.preConcatenate(AffineTransform.getTranslateInstance(imageBounds.x, imageBounds.y));
            RenderingHints formatHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                            BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            ParameterBlock params = new ParameterBlock();
            params.addSource(image);
            params.add(transform);
            params.add(interp);
            RenderedOp scaled = JAI.create("Affine", params, formatHints);

            g.drawRenderedImage(scaled, new AffineTransform());

            scaled.dispose();
        }

        g.setClip(oldClip);
    }

    // Convert a location on paper (points) to a location on screen (pixels):

    private Point paperToScreen(double paperX, double paperY) {
        Dimension size = getSize();
        Insets insets = getInsets();

        double renderW = size.width - insets.left - insets.right;
        double renderH = size.height - insets.top - insets.bottom;

        double paperW = format.getWidth();
        double paperH = format.getHeight();

        double paperAspect = paperW / paperH;
        double renderAspect = renderW / renderH;

        double pixelsPerPt;
        double renderX = 0;
        double renderY = 0;
        if (paperAspect > renderAspect) {
            pixelsPerPt = renderW / paperW;
            renderY = renderH / 2 - pixelsPerPt * paperH / 2;
        }
        else {
            pixelsPerPt = renderH / paperH;
            renderX = renderW / 2 - pixelsPerPt * paperW / 2;
        }
        double screenX = renderX + pixelsPerPt * paperX;
        double screenY = renderY + pixelsPerPt * paperY;

        return new Point((int) Math.round(screenX), (int) Math.round(screenY));
    }

    private Rectangle getImageBounds(Rectangle area) {
        // Call the static getImageBounds(), using the aspect ratio of the
        // current image, and convert the result to an integer Rectangle:
        double aspect = image.getWidth(null) / (double) image.getHeight(null);
        return (Rectangle) getImageBounds(area, aspect);
    }

    static Rectangle2D getImageBounds(Rectangle2D area, double aspect) {

        // Figure out where an image should appear, given an imageable area
        // and the image aspect ratio.

        double areaW = area.getWidth();
        double areaH = area.getHeight();

        Rectangle2D bounds = (Rectangle2D) area.clone();

        if (areaW / areaH > aspect) {   // imageable area is wider
            double imageH = areaH;
            double imageW = aspect * imageH;
            double centerX = area.getX() + area.getWidth() / 2;
            bounds.setRect(
                centerX - imageW / 2,
                bounds.getY(),
                imageW,
                bounds.getHeight()
            );
        }
        else {                          // imageable area is taller
            double imageW = areaW;
            double imageH = (int) Math.round(imageW / aspect);
            double centerY = area.getY() + area.getHeight() / 2;
            bounds.setRect(
                bounds.getX(),
                centerY - imageH / 2,
                bounds.getWidth(),
                imageH
            );
        }
        return bounds;
    }

    public static void main(String[] args) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PageFormat format = new PageFormat();

        JFrame frame = new JFrame("PreviewComponent Test");
        frame.setContentPane(new PreviewComponent(image, format));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 400, 400);
        frame.setVisible(true);
    }
}
