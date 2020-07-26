/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageOrientation;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;

import javax.imageio.ImageIO;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.TransposeType;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;

/**
 * A static method that gets a preview image from a File that is in a fixed
 * color space, with a fixed sample model, and that is no larger than a certain
 * fixed size in either dimension.
 * <p>
 * For presentation, consider also the method constrainImage(), to get to a
 * final scale, and rotate(), to account for rotation encoded in metadata or
 * commanded by the user.
 */
class Thumbnailer {

    // Get a preview from the given image File in a fixed size and format.
    static RenderedImage getImage(File file, int maxImageSize) {
        return getImage(file, maxImageSize, true);
    }

    static RenderedImage getImage(File file, int maxImageSize, boolean colorConvert) {
        RenderedImage image = null;
        ImageInfo info = ImageInfo.getInstanceFor(file);
        ImageFileStrategy[] strategies = ImageFileStrategy.Strategies;
        for (ImageFileStrategy strategy : strategies) {
            image = strategy.getImage(info, maxImageSize);
            if (image != null) {
                break;
            }
        }
        if (image == null) {
            return null;
        }
        image = maybeRetile(image);
        image = maybeBandSelect(image);

        if (image.getWidth() > maxImageSize ||
            image.getHeight() > maxImageSize) {
            image = Thumbnailer.constrainImage(image, maxImageSize);
        }
        if (colorConvert)
            image = maybeConvertColors(image);
        image = maybeConvertBitDepth(image);
        // image = maybeRunPipeline(image);

        return image;
    }

    // Scale the given image so it fits in a square of the given size.
    static RenderedImage constrainImage(RenderedImage image, int size) {
        float scaleFactor = scaleFactor(image, size);
        if (scaleFactor != 1) {
            image = scaleImage(image, scaleFactor);
            // image = maybeRunPipeline(image);
        }
        return image;
    }

    // Rotate the given image according to the given orientation metadata.
    static RenderedImage rotate(RenderedImage image, ImageMetadata meta) {
        if (meta != null) {
            ImageOrientation orient = meta.getOrientation();
            TransposeType transpose = orient.getCorrection();
            if (transpose != null) {
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(image);
                pb.add(transpose);
                image = JAI.create(
                    "Transpose", pb, null
                );
            }
        }
        return image;
    }

    // Rotate the given image clockwise by 90 degrees times the given multiplier,
    // then flip it horizontally and vertically.
    static RenderedImage rotateNinetyTimesThenFlip(
            RenderedImage image, int multiple,
            boolean horizontal, boolean vertical) {
        while (multiple < 0) {
            multiple += 4;
        }
        // Get the counter-clockwise rotated orientation,
        // then get the correction for the orientation.
        ImageOrientation reversed;
        switch (multiple % 4) {
            case 1:
                reversed = ImageOrientation.ORIENTATION_90CCW;
                break;
            case 2:
                reversed = ImageOrientation.ORIENTATION_180;
                break;
            case 3:
                reversed = ImageOrientation.ORIENTATION_90CW;
                break;
            default:
                reversed = ImageOrientation.ORIENTATION_LANDSCAPE;
        }
        if (horizontal) {
            reversed = reversed.getHFlip();
        }
        if (vertical) {
            reversed = reversed.getVFlip();
        }
        final TransposeType transpose = reversed.getCorrection();
        if (transpose != null) {
            final ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            pb.add(transpose);
            image = JAI.create("Transpose", pb, null);
        }
        return image;
    }

    private static float scaleFactor(RenderedImage img, int xSize) {
        int ySize = heightFromWidth(xSize);
        int width = img.getWidth();
        int height = img.getHeight();

        if (width != xSize || height != ySize)
            return Math.min(xSize / (float) width, ySize / (float) height);
        else
            return 1;
    }

    private static final Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);

    private static int heightFromWidth(int width) {
        return width; // 2 * width / 3;
    }

    private static PlanarImage scaleImage(RenderedImage source, float scale) {
        if (scale == 1)
            return PlanarImage.wrapRenderedImage(source);

        float scaleX = (float) Math.floor(scale * source.getWidth()) / (float) source.getWidth();
        float scaleY = (float) Math.floor(scale * source.getHeight()) / (float) source.getHeight();

        ParameterBlock params = new ParameterBlock();
        params.addSource(source);
        params.add(AffineTransform.getScaleInstance(scaleX, scaleY));
        params.add(interpolation);
        RenderingHints hints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
            BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        return JAI.create("Affine", params, hints);
    }

    // If the image is a buffered image and it's bigger than 1024 pixels,
    // then retile it.
    private static RenderedImage maybeRetile(RenderedImage image) {
        if (image instanceof BufferedImage) {
            if ((image.getWidth() > 1024 || image.getHeight() > 1024)) {
                ColorModel colors = image.getColorModel();
                SampleModel samples = colors.createCompatibleSampleModel(
                    JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT
                );
                ImageLayout layout = new ImageLayout(
                    0, 0,
                    JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                    samples, colors
                );
                RenderingHints formatHints = new RenderingHints(
                    JAI.KEY_IMAGE_LAYOUT, layout
                );
                ParameterBlock pb = new ParameterBlock();
                pb.addSource(image);
                pb.add(image.getSampleModel().getDataType());
                image = JAI.create("Format", pb, formatHints);
            }
        }
        return image;
    }

    // If an image has transparency, then add a bandselect to its pipeline.
    private static RenderedImage maybeBandSelect(RenderedImage image) {
        ColorModel colors = image.getColorModel();
        boolean hasAlpha = colors.hasAlpha();
        if (hasAlpha) {
            ParameterBlock pb = new ParameterBlock();
            pb.addSource(image);
            if (image.getColorModel().getNumColorComponents() == 3) {
                pb.add(new int[] {0, 1, 2});
            }
            else {
                pb.add(new int[] {0});
            }
            image = JAI.create("bandselect", pb, null);
        }
        return image;
    }

    // If the image is not in sRGB, then put it there.
    private static RenderedImage maybeConvertColors(RenderedImage image) {
        if ( image.getColorModel().getColorSpace() != null
            && !image.getColorModel().getColorSpace().isCS_sRGB() )
            image = Functions.toColorSpace(image, JAIContext.sRGBColorSpace, null);
        return image;
    }

    // If the image is using short colors, then scale them down to bytes.
    private static RenderedImage maybeConvertBitDepth(RenderedImage image) {
        SampleModel sample = image.getSampleModel();
        int type = sample.getDataType();
        if (type == DataBuffer.TYPE_USHORT) {
            image = Functions.fromUShortToByte(image, null);
        }
        return image;
    }

    public static void main(String[] args) throws IOException {
        RenderedImage image = ImageIO.read(new File(args[0]));

        int size = 500;
        final RenderedImage thumb = constrainImage(image, size);

        JComponent comp = new JComponent() {
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics;
                g.drawRenderedImage(thumb, new AffineTransform());
            }
            public Dimension getPreferredSize() {
                return new Dimension(thumb.getWidth(), thumb.getHeight());
            }
        };
        Border line = BorderFactory.createLineBorder(Color.red);
        Border empty = BorderFactory.createEmptyBorder(20, 20, 20, 20);
        Border compound = BorderFactory.createCompoundBorder(empty, line);

        JPanel container = new JPanel(new BorderLayout());
        container.add(comp);
        container.setBorder(compound);

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(container);

        JFrame frame = new JFrame("Thumbnailer");
        frame.setContentPane(panel);
        frame.setSize(400, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
