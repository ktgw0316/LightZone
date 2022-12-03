/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandMergeDescriptor;
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.InvertDescriptor;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Construct icons from resources in a systematic way where scaling can be
 * enforced and errors can be handled.
 */
public class IconFactory {

    public static int StandardSize = 18;

    /**
     * The resource assumption is that icons are accessible through the
     * IconFactory's classloader, namespaced by the class of a corresonding
     * object (like a button that uses the icon) with an extra "resources"
     * name at the end.
     */
    public static Icon createIcon(Class clazz, String name) {
        return createIcon(clazz, name, StandardSize);
    }

    public static Icon createIcon(Class clazz, String name, int size) {
        try {
            URL url = clazz.getResource("resources/" + name);
            BufferedImage image = ImageIO.read(url);
            image = getScaledImage(image, size);
            return new ImageIcon(image);
        }
        catch (Throwable t) {   // IOException, null URL
            t.printStackTrace();
            throw new RuntimeException(
                "Couldn't read icon resource \"" + name + "\" " +
                "for class " + clazz.getName()
            );
        }
    }

    public static Icon createInvertedIcon(Class clazz, String name) {
        return createInvertedIcon(clazz, name, StandardSize);
    }

    public static Icon createInvertedIcon(Class clazz, String name, int size) {
        Icon icon = createIcon(clazz, name, size);
        if (icon != null) {
            icon = invertIcon(icon);
        }
        return icon;
    }

    public static Icon invertIcon(Icon icon) {
        if (icon == null)
            return null;
        
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        BufferedImage image =
            new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics g = image.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        image = invertImageWithAlpha(image);
        return new ImageIcon(image);
    }

    // Invert the color channels of the given BufferedImage but not the
    // alpha channel.  This works by assuming that band 3 is the alpha,
    // which appears to be true for BufferedImages read from resources by
    // ImageIO.read() and constructed above in invertIcon().
    private static BufferedImage invertImageWithAlpha(BufferedImage image) {
        RenderedOp alpha =
            BandSelectDescriptor.create(image, new int[] { 3 }, null);
        RenderedOp colors =
            BandSelectDescriptor.create(image, new int[] { 0, 1, 2 }, null);
        colors = InvertDescriptor.create(colors, null);
        RenderedOp op = BandMergeDescriptor.create(colors, alpha, null);
        return op.getAsBufferedImage();
    }
    
    public static BufferedImage getScaledImage(BufferedImage image, int limit) {
        int maxSize = Math.max(image.getWidth(), image.getHeight());
        if (maxSize > limit) {
            if (true) {
                final double scale = limit / (float) maxSize;
                final AffineTransform transform =
                        AffineTransform.getScaleInstance(scale, scale);
                final int interp = AffineTransformOp.TYPE_BILINEAR;
                final AffineTransformOp op = new AffineTransformOp(transform, interp);
                final BufferedImage rescaled = op.createCompatibleDestImage(image, null);
                op.filter(image, rescaled);
                image = rescaled;
            } else {
                BufferedImage rescaled = new BufferedImage(limit, limit, BufferedImage.TYPE_4BYTE_ABGR);
                Graphics2D g = (Graphics2D) rescaled.getGraphics();
                double scale = limit / (float) maxSize;
                AffineTransform transform =
                        AffineTransform.getScaleInstance(scale, scale);
                // g.setTransform(transform);
                g.drawRenderedImage(image, transform);
                g.dispose();
                image = rescaled;
            }
        }
        return image;
    }
}
