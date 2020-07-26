/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model.ImageEditor;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.model.Preview;
import com.lightcrafts.model.Region;

import javax.media.jai.Histogram;
import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;

import static com.lightcrafts.model.ImageEditor.Locale.LOCALE;

public class HistogramPreview extends Preview implements PaintListener {
    private int[][] bins = null;
    private double[][] controlPoints = null;
    private int currentFocusZone = -1;
    final ImageEditorEngine engine;

    HistogramPreview(final ImageEditorEngine engine) {
        this.engine = engine;
    }

    @Override
    public String getName() {
        return LOCALE.get("Histogram_Name");
    }

    @Override
    public void setDropper(Point p) {

    }

    @Override
    public void setRegion(Region region) {

    }

    @Override
    public void addNotify() {
        // This method gets called when this Preview is added.
        engine.update(null, false);
        super.addNotify();
    }

    @Override
    public void removeNotify() {
        // This method gets called when this Preview is removed.
        super.removeNotify();
    }

    public void setFocusedZone(int index, double[][] controlPoints) {
        // System.out.println("currentFocusZone: " + currentFocusZone);

        if (currentFocusZone != index || this.controlPoints != controlPoints) {
            currentFocusZone = index;
            this.controlPoints = controlPoints;
            repaint();
        }
    }

    private int binmax() {
        int max = 0;
        for (int[] bin : bins) {
            int numBins = bin.length;
            // Skip the first and last bins (pure black and pure white) from normalization
            for (int i = 5; i < numBins - 5; i++) {
                if (bin[i] > max)
                    max = bin[i];
            }
        }
        return (int) (1.1 * max);
    }

    @Override
    public void setSelected(Boolean selected) {
        if (!selected)
            bins = null;
    }

    @Override
    protected synchronized void paintComponent(Graphics gr) {
        Graphics2D g2d = (Graphics2D) gr;

        if (bins == null)
            engine.update(null, false);

        Dimension bounds = getSize();

        final float minx = 0;
        final float miny = 0;
        final float width = bounds.width;
        final float height = bounds.height - 18;

        g2d.setColor(Color.lightGray);
        g2d.fill(new Rectangle2D.Float(minx, miny, width, height + 18));

        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        if (bins != null) {
            final int max = binmax();

            class scaler {
                private int yscale(double y) {
                    return (int) (height - (height - 4) * (y / (double) max) + 0.5 + miny);
                }
            }

            scaler s = new scaler();

            for (int c = 0; c < bins.length; c++) {
                Color color = Color.BLACK;

                if (bins.length > 1)
                    switch (c) {
                        case 0:
                            color = Color.RED;
                            break;
                        case 1:
                            color = Color.GREEN;
                            break;
                        case 2:
                            color = Color.BLUE;
                            break;
                    }

                g2d.setColor(color);

                int numBins = bins[c].length;

                int zeroY = s.yscale(0);
                float xstep = (width+1) / numBins;

                GeneralPath gp = new GeneralPath();

                gp.moveTo(minx, zeroY);
                float lastx = minx;
                float lasty = zeroY;
                for (int i = 0; i < numBins; i++) {
                    int y = s.yscale(bins[c][i]);
                    float x = xstep * i + minx;
                    if (lasty != zeroY || y != zeroY) {
                        gp.lineTo(x, y);
                        lastx = x;
                        lasty = y;
                    } else {
                        gp.moveTo(x, y);
                    }
                }
                if (lasty != zeroY)
                    gp.lineTo(lastx, zeroY);

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                g2d.fill(gp);
                g2d.setComposite(AlphaComposite.SrcOver);
                g2d.draw(gp);
            }
        }

        float step = width / 16.0f;
        for (int i = 0; i < 16; i++) {
            if (i == currentFocusZone)
                g2d.setColor(Color.yellow);
            else {
                float color = (float) ((Math.pow(2, i * 8.0 / (16 - 1)) - 1) / 255.);
                float[] srgbColor = Functions.fromLinearToCS(JAIContext.systemColorSpace, new float[] {color, color, color});

                g2d.setColor(new Color((int) (255 * srgbColor[0]), (int) (255 * srgbColor[1]), (int) (255 * srgbColor[2])));
            }
            g2d.fill(new Rectangle2D.Float(minx + step * i, height + miny, step + 0.5f, 18));
        }
    }

    private static float[] logTable = new float[0x10000];

    static {
        for (int i = 0; i < 0x10000; i++)
            logTable[i] = (float) Math.log1p(i);
    }

    private synchronized void computeHistogram(Rectangle visibleRect, PlanarImage image) {
        int channels = image.getSampleModel().getNumBands();

        Histogram hist = new Histogram(256, 256, 512, channels);

        bins = hist.getBins();

        int[] pixel = null;

        int maxPixels = 256;
        int incX = visibleRect.width >= 2 * maxPixels ? visibleRect.width / maxPixels : 1;
        int incY = visibleRect.height >= 2 * maxPixels ? visibleRect.height / maxPixels : 1;

        double log2 = Math.log(2);

        int minTileX = image.XToTileX(visibleRect.x);
        int maxTileX = image.XToTileX(visibleRect.x + visibleRect.width - 1);
        int minTileY = image.YToTileY(visibleRect.y);
        int maxTileY = image.YToTileY(visibleRect.y + visibleRect.height - 1);

        for (int tx = minTileX; tx <= maxTileX; tx++) {
            for (int ty = minTileY; ty <= maxTileY; ty++) {
                Raster raster = image.getTile(tx, ty);

                int minX = Math.max(visibleRect.x, raster.getMinX());
                int maxX = Math.min(visibleRect.x + visibleRect.width, raster.getMinX() + raster.getWidth());
                int minY = Math.max(visibleRect.y, raster.getMinY());
                int maxY = Math.min(visibleRect.y + visibleRect.height, raster.getMinY() + raster.getHeight());

                for (int x = minX; x < maxX; x+=incX) {
                    for (int y = minY; y < maxY; y+=incY) {
                        pixel = raster.getPixel(x, y, pixel);
                        for (int c = 0; c < channels; c++) {
                            int v = (int) (511 * logTable[pixel[c]] / (16 * log2));
                            if (v > 255)
                                bins[c][v - 256]++;
                            else
                                bins[c][0]++;
                        }
                    }
                }
            }
        }

        bins = hist.getBins();
    }

    private class Histogrammer extends Thread {
        PlanarImage image;
        PlanarImage nextImage = null;
        Rectangle visibleRect;

        Histogrammer(Rectangle visibleRect, PlanarImage image) {
            super("Histogram Preview Histogrammer");
            this.visibleRect = visibleRect;
            this.image = image;
        }

        synchronized void nextView(Rectangle visibleRect, PlanarImage image) {
            this.visibleRect = visibleRect;
            nextImage = image;
        }

        synchronized private boolean getNextView() {
            if (nextImage != null) {
                image = nextImage;
                nextImage = null;
                return true;
            } else
                return false;
        }

        @Override
        public void run() {
            do {
                if (getSize().width > 0 && getSize().height > 0) {
                    computeHistogram(visibleRect, image);
                    repaint();
                }
            } while (getNextView());
        }
    }

    private Histogrammer histogrammer = null;

    @Override
    public void paintDone(PlanarImage image, Rectangle visibleRect, boolean synchronous, long time) {
        Dimension previewDimension = getSize();

        if (previewDimension.getHeight() > 1 && previewDimension.getWidth() > 1) {
            if (histogrammer == null || !histogrammer.isAlive()) {
                histogrammer = new Histogrammer(visibleRect, image);
                histogrammer.start();
            } else
                histogrammer.nextView(visibleRect, image);
        }
    }
}
