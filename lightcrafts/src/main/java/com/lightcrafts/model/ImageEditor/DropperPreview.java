/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.model.Preview;
import com.lightcrafts.model.Region;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.toolkit.ShadowFactory;
import com.lightcrafts.utils.LCMS;

import org.eclipse.imagen.IHSColorSpace;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

import static com.lightcrafts.model.ImageEditor.Locale.LOCALE;

/** A Preview that shows formatted text derived from a given
  * ImageEditorDisplay and a mouse location, like color channel values.
  * Listens to mouse motion on its ImageEditorDisplay component for
  * locations to probe.
  */
class DropperPreview extends Preview {

    private final ImageEditorEngine engine;       // The engine to probe
    private Point loc = new Point(0, 0);    // The current probe point
    private Color color = Color.GRAY;

    DropperPreview(ImageEditorEngine engine) {
        this.engine = engine;
    }

    @Override
    public String getName() {
        return LOCALE.get("Sampler_Name");
    }

    @Override
    public void setDropper(Point p) {
        if (p == null || engine == null)
            return;

        Color sample = engine.getPixelValue(p.x, p.y);
        if (sample != null) {
            loc = p;
            color = sample;
        }
        repaint();
    }

    @Override
    public void setRegion(Region region) {
    }

    @Override
    public void setSelected(Boolean selected) {
    }

    private static final IHSColorSpace ihsCS = IHSColorSpace.getInstance();

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

    private int red;
    private int green;
    private int blue;
    private double lightness;
    private double zone;
    private float[] ihsColor;
    private int L;
    private int a;
    private int b;
    private Color colorInSystemCS;

    private void update() {
        red = color.getRed();
        green = color.getGreen();
        blue = color.getBlue();
        lightness = calcLightness(red, green, blue);
        zone = calcZone(lightness);

        final var xyzColor = JAIContext.linearColorSpace.toCIEXYZ(new float[]{red / 255f, green / 255f, blue / 255f});
        ihsColor = ihsCS.fromCIEXYZ(xyzColor);

        short[] labColors = new short[3];
        final var ts = new LCMS.Transform(
                new LCMS.Profile(JAIContext.linearProfile), LCMS.TYPE_RGB_16,
                new LCMS.Profile(JAIContext.labProfile), LCMS.TYPE_Lab_16,
                LCMS.INTENT_RELATIVE_COLORIMETRIC, 0);
        ts.doTransform(new short[] {(short) (red * 256), (short) (green * 256), (short) (blue * 256)}, labColors);
        L = 100 * (0xffff & labColors[0]) / 0xffff;
        a = ((0xffff & labColors[1]) - 128 * 256) / 256;
        b = ((0xffff & labColors[2]) - 128 * 256) / 256;

        final var originalComponents = color.getRGBComponents(null);
        final var components = Functions.fromLinearToCS(JAIContext.systemColorSpace, originalComponents);
        colorInSystemCS = new Color(components[0], components[1], components[2]);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (loc == null || engine == null)
            return;

        update();

        final var g = (Graphics2D) graphics;

        g.setRenderingHints(aliasingRenderHints);
        g.setColor(LightZoneSkin.Colors.NeutralGray);

        final var bounds = getSize();
        final var minx = 0;
        final var miny = 0;
        final var width = bounds.width;
        final var height = bounds.height;

        g.fillRect(minx, miny, width, height);
        g.setColor(LightZoneSkin.Colors.ToolPanesForeground);

        final var font = new Font("Monospaced", Font.PLAIN, 13);
        g.setFont(font);

        final var layout = new TextLayout("ABC", font, g.getFontRenderContext());

        final var textHeight = (float) layout.getBounds().getHeight() + 5;

        final var separator = ": ";
        final var fm = getFontMetrics(font);

        class Graph {
            private void drawAlignedString(String name, int value, float x, float y) {
                drawAlignedString(name, Integer.toString(value), x, y);
            }

            private void drawAlignedString(String name, String value, float x, float y) {
                g.drawString(name + separator + value, x - fm.stringWidth(name), y);
            }
        }

        final var gg = new Graph();

        gg.drawAlignedString("x", loc.x, minx + 50, miny + 2 + textHeight);
        gg.drawAlignedString("y", loc.y, minx + 50, miny + 2 + 2 * textHeight);

        gg.drawAlignedString(LOCALE.get("Sampler_RedLabel"),   red,   minx + 50, 12 + 3 * textHeight);
        gg.drawAlignedString(LOCALE.get("Sampler_GreenLabel"), green, minx + 50, 12 + 4 * textHeight);
        gg.drawAlignedString(LOCALE.get("Sampler_BlueLabel"),  blue,  minx + 50, 12 + 5 * textHeight);

        final var gap = width - 130;
        gg.drawAlignedString(LOCALE.get("Sampler_LuminosityLabel"), (int) lightness, minx + gap + 50, miny + 2 + textHeight);

        final var format = new DecimalFormat("0.0");
        gg.drawAlignedString(LOCALE.get("Sampler_ZoneLabel"), format.format(zone), minx + gap + 50, miny + 2 + 2 * textHeight);

        gg.drawAlignedString(LOCALE.get("Sampler_IntensityLabel"),  (int) (100 * ihsColor[0]) + "%",                        minx + gap + 50, miny + 12 + 3 * textHeight);
        gg.drawAlignedString(LOCALE.get("Sampler_HueLabel"),        (int) (360 * (ihsColor[1] / (2 * Math.PI))) + "\u00B0", minx + gap + 50, miny + 12 + 4 * textHeight);
        gg.drawAlignedString(LOCALE.get("Sampler_SaturationLabel"), (int) (100 * ihsColor[2]) + "%",                        minx + gap + 50, miny + 12 + 5 * textHeight);

        gg.drawAlignedString("L", L, minx + 50, miny + 12 + 7 * textHeight);
        gg.drawAlignedString("a", a, minx + 50, miny + 12 + 8 * textHeight);
        gg.drawAlignedString("b", b, minx + 50, miny + 12 + 9 * textHeight);

        final var size = new Dimension(width - 20, 60);
        final var image = new BufferedImage(size.width-106, size.height-6, BufferedImage.TYPE_INT_RGB);

        final var imageG = image.getGraphics();
        imageG.setColor(colorInSystemCS);
        imageG.fillRect(0, 0, size.width-106, size.height-6);
        imageG.dispose();

        final var shadow = new ShadowFactory(3, 1.0f, Color.gray);
        shadow.setRenderingHint(ShadowFactory.KEY_BLUR_QUALITY, ShadowFactory.VALUE_BLUR_QUALITY_HIGH);
        final var shadowImage = shadow.createShadow(image);
        final var shadowImageG = shadowImage.getGraphics();
        shadowImageG.drawImage(image, 3, 2, null);
        shadowImageG.dispose();

        graphics.drawImage(shadowImage, minx + 110, (int) (miny + 12 + 6 * textHeight), null);

        graphics.setColor(Color.DARK_GRAY);
        graphics.drawRect(minx + 110 + 2, (int) (miny + 12 + 6 * textHeight) + 2, size.width-106, size.height-6);
    }
}
