/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.utils.awt.geom.HiDpi;
import javax.media.jai.JAI;
import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;

import javax.swing.*;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Point2D;

/**
 * A JComponent that renders images, which is the most this Editor
 * can do.  Implements Scrollable just to declare that it tracks viewport
 * width and height.  Returned from getImage().
 * <p>
 * Also shows a click-to-edit affordance superimposed on the image, since
 * these components are used to initialize real editors.
 */
class DisabledImageComponent extends JComponent implements Scrollable {

    private RenderedImage image;

    private BufferedImage button;

    DisabledImageComponent(RenderedImage image) {
        setImage(image);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    void setImage(RenderedImage image) {
        this.image = image;
        initButton();
        repaint();
    }

    private static AffineTransform identity = new AffineTransform();

    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        HiDpi.resetTransformScaleOf(g);

        g.setColor(LightZoneSkin.Colors.EditorBackground);
        Shape clip = g.getClip();
        g.fill(clip);

        if (image != null) {
            AffineTransform xform = getTransform();

            RenderedImage xformedImage = image;
            if (!xform.isIdentity()) {
                RenderingHints extenderHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                                  BorderExtender.createInstance(BorderExtender.BORDER_COPY));
                ParameterBlock params = new ParameterBlock();
                params.addSource(image);
                params.add(xform);
                params.add(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
                xformedImage = JAI.create("Affine", params, extenderHints);
            }

            g.drawRenderedImage(xformedImage, identity);

            AffineTransform buttonXform = getButtonTransform(xform);
            g.drawRenderedImage(button, buttonXform);
        }
    }

    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(image.getWidth(), image.getHeight());
        }
        else {
            return new Dimension();
        }
    }

    // Get the AffineTransform which maps the current image at the origin
    // to the center of our bounds.
    private AffineTransform getTransform() {
        // Compute the transform that maps into this size at the origin

        final Dimension bound = HiDpi.imageSpaceDimensionFrom(getSize());

        // Two possible scale factors, depending on the aspect ratio
        double hSpace = bound.width;
        double vSpace = bound.height;

        double sWide = hSpace / (double) image.getWidth();
        double sTall = vSpace / (double) image.getHeight();

        // The actual scale factor is the lesser
        double s = Math.min(sWide, sTall);

        if (s < 1) {
            // Scale to fit and translate to the center
            AffineTransform xform = AffineTransform.getScaleInstance(s, s);

            // Depending on the aspect ratio, center vertically or horizontally
            if (sWide < sTall) {
                // Center the image vertically
                double gap = Math.floor((vSpace - image.getHeight() * s) / 2);
                AffineTransform trans =
                    AffineTransform.getTranslateInstance(0, gap);
                xform.preConcatenate(trans);
            }
            else {
                // Center the image horizontally
                double gap = Math.floor((hSpace - image.getWidth() * s) / 2);
                AffineTransform trans =
                    AffineTransform.getTranslateInstance(gap, 0);
                xform.preConcatenate(trans);
            }
            return xform;
        }
        else {
            // Just translate to the center
            double hgap = Math.floor((hSpace - image.getWidth()) / 2);
            double vgap = Math.floor((vSpace - image.getHeight()) / 2);
            AffineTransform trans =
                AffineTransform.getTranslateInstance(hgap, vgap);
            return trans;
        }
    }

    // Get the AffineTransform which maps the button image at the origin
    // to the place where it should appear within our bounds.
    private AffineTransform getButtonTransform(AffineTransform imageXform) {
        // Compute the corners of the image in screen coordinates.
        Point2D imageUpperLeft = new Point2D.Double(0, 0);
        imageUpperLeft = imageXform.transform(imageUpperLeft, null);
        
        Point2D imageLowerRight = new Point2D.Double(
            image.getWidth(), image.getHeight()
        );
        imageLowerRight = imageXform.transform(imageLowerRight, null);

        double imageW = imageLowerRight.getX() - imageUpperLeft.getX();
        double imageH = imageLowerRight.getY() - imageUpperLeft.getY();
        double buttonW = button.getWidth();
        double buttonH = button.getHeight();

        // If the image is small compared to the button, then center the button.
        if ((imageW < 4 * buttonW) || (imageH < 4 * buttonH)) {
            Point2D center = new Point2D.Double(
                (imageUpperLeft.getX() + imageLowerRight.getX()) / 2,
                (imageUpperLeft.getY() + imageLowerRight.getY()) / 2
            );
            Point2D buttonLoc = new Point2D.Double(
                center.getX() - buttonW / 2, center.getY() - buttonH / 2
            );
            return AffineTransform.getTranslateInstance(
                buttonLoc.getX(), buttonLoc.getY()
            );
        }
        // Otherwise, inset the button from the lower right corner of the image.
        else {
            Point2D buttonLoc = new Point2D.Double(
                imageLowerRight.getX() - 2 * button.getWidth(),
                imageLowerRight.getY() - 2 * button.getHeight()
            );
            return AffineTransform.getTranslateInstance(
                buttonLoc.getX(), buttonLoc.getY()
            );
        }
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        return 0;
    }

    public int getScrollableBlockIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        return 0;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return true;
    }

    // Paint a label with a background and a border into the button image.
    private void initButton() {
        JLabel comp = new JLabel(LOCALE.get("ClickToEditMessage"));
        comp.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        comp.setFont(new Font("SansSerif", Font.PLAIN,
                (int) Math.floor(HiDpi.defaultTransform.getScaleX() * 11)));

        Dimension size = comp.getPreferredSize();
        comp.setSize(size);

        button = new BufferedImage(
            size.width + 1, size.height + 1, BufferedImage.TYPE_INT_ARGB
        );
        Shape roundRect = new RoundRectangle2D.Double(
            0, 0, size.width, size.height, 8, 8
        );
        Graphics2D g = (Graphics2D) button.getGraphics();
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
        );
        g.setColor(new Color(128, 128, 128, 128));
        g.fill(roundRect);
        comp.paint(g);
        g.setColor(Color.white);
        g.draw(roundRect);
        g.dispose();
    }
}
