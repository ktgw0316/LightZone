/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.test;

import com.lightcrafts.model.CropBounds;
import com.lightcrafts.model.Scale;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

/**
 * A simple CroppedImageRenderer that works, because it is so simple.
 */

class TestCroppedImageRenderer
    extends JComponent implements CroppedImageRenderer
{
    private Image image;

    private AffineTransform xform;
    double width;
    double height;

    TestCroppedImageRenderer(String path) throws IOException {
        image = ImageIO.read(new File(path));
        xform = new AffineTransform();
        setOpaque(false);
    }

    public Component getComponent() {
        return this;
    }

    public void cropBoundsChanged(
        CropBounds crop, Scale scale, boolean isChanging
    ) {
        if (! isChanging) {
            updateTransform(crop, scale);
            repaint();
        }
    }

    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;

        if (width == 0) {
            width = image.getWidth(this);
        }
        if (height == 0) {
            height = image.getHeight(this);
        }
        g.setClip(0, 0, (int) width, (int) height);

        g.setTransform(xform);

        int w = image.getWidth(this);
        int h = image.getHeight(this);

        g.drawImage(image, 0, 0, w, h, this);
    }

    private void updateTransform(CropBounds crop, Scale scale) {

        crop = getScaledCrop(crop, scale);

        // Compute the affine transform which will show the cropped part of
        // the image at the origin at the given Scale:

        double factor = scale.getFactor();

        AffineTransform scaling =
            AffineTransform.getScaleInstance(factor, factor);

        double angle = crop.getAngle();

        Point2D center = crop.getCenter();

        AffineTransform rotation = AffineTransform.getRotateInstance(
            - angle, center.getX(), center.getY()
        );
        double x = center.getX() - crop.getWidth() / 2;
        double y = center.getY() - crop.getHeight() / 2;

        AffineTransform translation = AffineTransform.getTranslateInstance(
            -x, -y
        );
        xform = compose(translation, compose(rotation, scaling));

        width = crop.getWidth();
        height = crop.getHeight();
    }

    private static CropBounds getScaledCrop(CropBounds crop, Scale scale) {
        Point2D center = crop.getCenter();
        double width = crop.getWidth();
        double height = crop.getHeight();
        double angle = crop.getAngle();

        double factor = scale.getFactor();

        center = new Point2D.Double(
            factor * center.getX(), factor * center.getY()
        );
        width *= factor;
        height *= factor;

        return new CropBounds(center, width, height, angle);
    }

    private static AffineTransform compose(
        AffineTransform left, AffineTransform right
    ) {
        AffineTransform result = (AffineTransform) left.clone();
        result.concatenate(right);
        return result;
    }
}
