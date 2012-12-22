/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.test;

import com.lightcrafts.model.CropBounds;
import com.lightcrafts.model.Scale;

import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.IOException;

class CropBoundsRenderer
    extends JComponent implements CropBoundsControls.Listener
{
    private Image image;
    private CropBounds crop;

    CropBoundsRenderer(String path) throws IOException {
        image = ImageIO.read(new File(path));
    }

    protected void paintComponent(Graphics graphics) {
        if (image != null) {
            int width = image.getWidth(this);
            int height = image.getHeight(this);
            graphics.drawImage(image, 0, 0, width, height, this);
        }
        if (crop != null) {

            Graphics2D g = (Graphics2D) graphics;
            g.setColor(Color.orange);

            Point2D ul = crop.getUpperLeft();
            Point2D ur = crop.getUpperRight();
            Point2D ll = crop.getLowerLeft();
            Point2D lr = crop.getLowerRight();

            GeneralPath path = new GeneralPath();
            path.moveTo((float) ul.getX(), (float) ul.getY());
            path.lineTo((float) ur.getX(), (float) ur.getY());
            path.lineTo((float) lr.getX(), (float) lr.getY());
            path.lineTo((float) ll.getX(), (float) ll.getY());
            path.closePath();

            g.draw(path);
        }
    }

    public Dimension getPreferredSize() {
        return new Dimension(image.getWidth(this), image.getHeight(this));
    }

    public void cropBoundsChanged(
        CropBounds bounds, Scale scale, boolean isChanging
    ) {
        crop = bounds;
        repaint();
    }
}
