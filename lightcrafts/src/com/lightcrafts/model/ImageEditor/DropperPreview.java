/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.model.Preview;
import com.lightcrafts.model.Region;
import com.lightcrafts.utils.ColorScience;
import com.lightcrafts.utils.LCMS;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.ui.toolkit.ShadowFactory;

import com.lightcrafts.mediax.jai.IHSColorSpace;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.font.TextLayout;
import java.text.DecimalFormat;

/** A Preview that shows formatted text derived from a given
  * ImageEditorDisplay and a mouse location, like color channel values.
  * Listens to mouse motion on its ImageEditorDisplay component for
  * locations to probe.
  */
class DropperPreview extends Preview {

    private ImageEditorEngine engine;       // The engine to probe
    private Point loc = new Point(0, 0);    // The current probe point
    private Color color = Color.GRAY;

    DropperPreview(ImageEditorEngine engine) {
        this.engine = engine;
    }

    public String getName() {
        return "Sampler";
    }

    public void setDropper(Point p) {
        if (p != null && engine != null) {
            Color sample = engine.getPixelValue(p.x, p.y);
            if (sample != null) {
                loc = p;
                color = sample;
            }
            repaint();
        }
    }

    public void setRegion(Region region) {
    }

    public void setSelected(Boolean selected) {
    }

    private static IHSColorSpace ihsCS = IHSColorSpace.getInstance();

    private static final RenderingHints aliasingRenderHints;

    static {
        aliasingRenderHints = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        aliasingRenderHints.put(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        aliasingRenderHints.put(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );
    }

    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;

        g.setRenderingHints(aliasingRenderHints);

        Dimension bounds = getSize();

        final int minx = 0;
        final int miny = 0;
        final int width = bounds.width;
        final int height = bounds.height;

        final int gap = width - 130;

        g.setColor(new Color(245, 245, 245));
        g.fillRect(minx, miny, width, height);
        g.setColor(Color.black);

        Font font = new Font("Monospaced", Font.PLAIN, 13);
        g.setFont(font);

        TextLayout layout = new TextLayout("ABC", font, g.getFontRenderContext());

        float textHeight = (float) layout.getBounds().getHeight() + 5;

        if (loc != null && engine != null) {
            g.drawString("    x: " + loc.x, minx + 10, miny + 2 + textHeight);
            g.drawString("    y: " + loc.y, minx + 10, miny + 2 + 2 * textHeight);

            int red = color.getRed();
            int green = color.getGreen();
            int blue = color.getBlue();

            g.drawString("  Red: " + red, minx + 10, miny + 12 + 3 * textHeight);
            g.drawString("Green: " + green, minx + 10, miny + 12 + 4 * textHeight);
            g.drawString(" Blue: " + blue, minx + 10, miny + 12 + 5 * textHeight);

            double lightness = ColorScience.Wr * red + ColorScience.Wg * green + ColorScience.Wb * blue;

            g.drawString("Luminosity: " + (int) lightness, minx + gap, miny + 2 + textHeight);

            double zone = Math.log(lightness) / (8 * Math.log(2));

            DecimalFormat format = new DecimalFormat("0.0");

            g.drawString("      Zone: " + format.format(10 * zone), minx + gap, miny + 2 + 2 * textHeight);

            float xyzColor[] = JAIContext.linearColorSpace.toCIEXYZ(new float[]{(float) (red / 255.),
                                                                                (float) (green / 255.),
                                                                                (float) (blue / 255.)});

            float ihsColor[] = ihsCS.fromCIEXYZ(xyzColor);

            g.drawString(" Intensity: " + (int) (100 * ihsColor[0]) + "%", minx + gap, miny + 12 + 3 * textHeight);
            g.drawString("       Hue: " + (int) (360 * (ihsColor[1] / (2 * Math.PI))) + "\u00B0", minx + gap, miny + 12 + 4 * textHeight);
            g.drawString("Saturation: " + (int) (100 * ihsColor[2]) + "%", minx + gap, miny + 12 + 5 * textHeight);

            // float labColor[] = JAIContext.labColorSpace.fromCIEXYZ(xyzColor);

            LCMS.Transform ts = new LCMS.Transform(new LCMS.Profile(JAIContext.linearProfile), LCMS.TYPE_RGB_16,
                                                  new LCMS.Profile(JAIContext.labProfile), LCMS.TYPE_Lab_16,
                                                  LCMS.INTENT_RELATIVE_COLORIMETRIC, 0);
            short labColors[] = new short[3];

            ts.doTransform(new short[] {(short) (red * 256), (short) (green * 256), (short) (blue * 256)}, labColors);

            int L = 100 * (0xffff & labColors[0]) / 0xffff;
            int a = ((0xffff & labColors[1]) - 128 * 256) / 256;
            int b = ((0xffff & labColors[2]) - 128 * 256) / 256;

            g.drawString("    L: " + L, minx + 10, miny + 12 + 7 * textHeight);
            g.drawString("    a: " + a, minx + 10, miny + 12 + 8 * textHeight);
            g.drawString("    b: " + b, minx + 10, miny + 12 + 9 * textHeight);

            float components[] = color.getRGBComponents(null);

            components = Functions.fromLinearToCS(JAIContext.systemColorSpace, components);

            Dimension size = new Dimension(width - 20, 60);

            BufferedImage image = new BufferedImage(size.width-106, size.height-6, BufferedImage.TYPE_INT_RGB);
            Graphics imageG = image.getGraphics();

            imageG.setColor(new Color(components[0], components[1], components[2]));
            imageG.fillRect(0, 0, size.width-106, size.height-6);
            imageG.dispose();

            ShadowFactory shadow = new ShadowFactory(3, 1.0f, Color.gray);
            shadow.setRenderingHint(ShadowFactory.KEY_BLUR_QUALITY, ShadowFactory.VALUE_BLUR_QUALITY_HIGH);
            BufferedImage shadowImage = shadow.createShadow(image);
            imageG = shadowImage.getGraphics();
            imageG.drawImage(image, 3, 2, null);
            imageG.dispose();

            graphics.drawImage(shadowImage, minx + 110, (int) (miny + 12 + 6 * textHeight), null);

            graphics.setColor(Color.DARK_GRAY);
            graphics.drawRect(minx + 110 + 2, (int) (miny + 12 + 6 * textHeight) + 2, size.width-106, size.height-6);
        }
    }
}
