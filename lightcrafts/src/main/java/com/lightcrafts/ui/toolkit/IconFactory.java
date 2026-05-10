/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import org.eclipse.imagen.RenderedOp;
import org.eclipse.imagen.media.algebra.AlgebraDescriptor;
import org.eclipse.imagen.media.bandmerge.BandMergeDescriptor;
import org.eclipse.imagen.media.bandselect.BandSelectDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import static org.eclipse.imagen.media.algebra.AlgebraDescriptor.Operator.INVERT;

/**
 * Construct icons from resources in a systematic way where scaling can be
 * enforced and errors can be handled.
 */
public class IconFactory {

    private static final Logger logger = LoggerFactory.getLogger(IconFactory.class);

    public static int StandardSize = 18;

    /**
     * The resource assumption is that icons are accessible through the
     * IconFactory's classloader, namespaced by the class of a corresponding
     * object (like a button that uses the icon) with an extra "resources"
     * name at the end.
     */
    public static Icon createIcon(Class<?> clazz, String name) {
        return createIcon(clazz, name, StandardSize);
    }

    public static Icon createIcon(Class<?> clazz, String name, int size) {
        try {
            URL url = clazz.getResource("resources/" + name);
            BufferedImage image = ImageIO.read(Objects.requireNonNull(url));
            image = getScaledImage(image, size);
            return new ImageIcon(image);
        }
        catch (IOException | NullPointerException e) {   // IOException, null URL
            logger.error("Couldn't read icon resource \"{}\" for class {}", name, clazz.getName(), e);
            throw new RuntimeException(
                    "Couldn't read icon resource \"" + name + "\" " + "for class " + clazz.getName());
        }
    }

    public static Icon createInvertedIcon(Class<?> clazz, String name) {
        return createInvertedIcon(clazz, name, StandardSize);
    }

    public static Icon createInvertedIcon(Class<?> clazz, String name, int size) {
        Icon icon = createIcon(clazz, name, size);
        return invertIcon(icon);
    }

    public static Icon invertIcon(Icon icon) {
        if (icon == null)
            return null;

        final int width = icon.getIconWidth();
        final int height = icon.getIconHeight();
        var image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
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
        RenderedOp alpha = BandSelectDescriptor.create(image, new int[] { 3 }, null);
        RenderedOp colors = BandSelectDescriptor.create(image, new int[] { 0, 1, 2 }, null);
        colors = AlgebraDescriptor.create(INVERT, null, null, 0, null, colors);
        RenderedOp op = BandMergeDescriptor.create(null, 0, true, null, colors, alpha);
        return op.getAsBufferedImage();
    }

    public static BufferedImage getScaledImage(BufferedImage image, int limit) {
        int maxSize = Math.max(image.getWidth(), image.getHeight());
        if (maxSize > limit) {
            final double scale = limit / (double) maxSize;
            final var transform = AffineTransform.getScaleInstance(scale, scale);
            final int interp = AffineTransformOp.TYPE_BILINEAR;
            final AffineTransformOp op = new AffineTransformOp(transform, interp);
            final BufferedImage rescaled = op.createCompatibleDestImage(image, null);
            op.filter(image, rescaled);
            image = rescaled;
        }
        return image;
    }
}
