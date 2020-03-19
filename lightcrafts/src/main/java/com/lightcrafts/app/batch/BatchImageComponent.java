/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.batch;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.browser.model.ImageTask;
import com.lightcrafts.utils.filecache.FileCacheFactory;
import com.lightcrafts.utils.filecache.FileCache;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;

class BatchImageComponent extends JComponent {

    final static Dimension ImageSize = new Dimension(150, 150);

    private RenderedImage image;

    BatchImageComponent() {
        setLayout(null);
        setPreferredSize(ImageSize);
        setMinimumSize(ImageSize);
        setMaximumSize(ImageSize);
        setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.gray),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
            )
        );
    }

    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        if (image == null) {
            Dimension size = getSize();
            Insets insets = getInsets();
            g.setColor(LightZoneSkin.Colors.BrowserBackground);
            g.fillRect(
                insets.left,
                insets.top,
                size.width - insets.left - insets.right,
                size.height - insets.top - insets.bottom
            );
        }
        else {
            g.drawRenderedImage(image, getTransform());
        }
    }

    /*
        Get file preview from cache
     */
    void setCachedFile(File file) {
        if (file != null) {
            try {
                String key = ImageTask.getImageKey(file);
                FileCache cache = FileCacheFactory.get(file.getParentFile());
                File cachedFile = cache.getFileFor(key);

                if (cachedFile != null)
                    image = JPEGImageType.getImageFromInputStream(new FileInputStream(cachedFile), null,
                                                                  ImageTask.CacheImageSize, ImageTask.CacheImageSize);
            }
            catch (Throwable t) {
                // BadImageFileException
                // ColorProfileException
                // IOException
                // UnknownImageTypeException
                t.printStackTrace();
                image = null;
            }
        }
        repaint();
    }

    void setFile(File file) {
        if (file != null) {
            ImageInfo info = ImageInfo.getInstanceFor(file);
            try {
                image = info.getImage(null);
                image = Functions.systemColorSpaceImage(image);
            }
            catch (Throwable t) {
                // BadImageFileException
                // ColorProfileException
                // IOException
                // UnknownImageTypeException
                t.printStackTrace();
                image = null;
            }
        }
        repaint();
    }

    // Get the AffineTransform which maps a rectangle of the given image size
    // at the origin into this component's bounds, with Insets.
    private AffineTransform getTransform() {
        Dimension bound = getSize();
        Insets insets = getInsets();

        // First, compute the transform that maps into this size at the origin

        // Two possible scale factors, depending on the aspect ratio
        double hSpace = bound.width - insets.left - insets.right;
        double vSpace = bound.height - insets.top - insets.bottom;

        double sWide = hSpace / (double) image.getWidth();
        double sTall = vSpace / (double) image.getHeight();

        // The actual scale factor is the lesser
        double s = Math.min(sWide, sTall);

        AffineTransform xform = AffineTransform.getScaleInstance(s, s);

        // Depending on the aspect ratio, center vertically or horizontally
        if (sWide < sTall) {
            // Center the image vertically
            double gap = insets.top + (vSpace - image.getHeight() * s) / 2;
            AffineTransform trans =
                AffineTransform.getTranslateInstance(insets.left, gap);
            xform.preConcatenate(trans);
        }
        else {
            // Center the image horizontally
            double gap = insets.left + (hSpace - image.getWidth() * s) / 2;
            AffineTransform trans =
                AffineTransform.getTranslateInstance(gap, insets.top);
            xform.preConcatenate(trans);
        }
        return xform;
    }
}
