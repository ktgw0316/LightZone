/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform;

import com.lightcrafts.model.ImageEditor.ImageEditorEngine;
import com.lightcrafts.model.PrintSettings;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.jai.JAIContext;

import javax.media.jai.*;
import javax.media.jai.operator.TransposeDescriptor;
import java.awt.print.*;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;

public class DefaultPrinterLayer implements PrinterLayer {
    private PageFormat lastPageFormat;
    private final PrinterJob printJob;

    DefaultPrinterLayer() {
        printJob = PrinterJob.getPrinterJob();
        lastPageFormat = printJob.defaultPage();
    }

    public void setPageFormat(PageFormat pageFormat) {
        // TODO: do we really need to validate?
        lastPageFormat = printJob.validatePage(pageFormat);
    }

    public void initialize() {
    }

    public void dispose() {
    }

    public PageFormat getPageFormat() {
        return lastPageFormat;
    }

    public PageFormat pageDialog(PageFormat pageFormat) {
        return lastPageFormat = printJob.pageDialog(pageFormat);
    }

    public boolean printDialog() {
        return printJob.printDialog();
    }

    public void setJobName(String name) {
        printJob.setJobName(name);
    }

    private Printer printer = null;

    public void print(ImageEditorEngine engine, ProgressThread thread,
                      PageFormat format, PrintSettings settings) throws PrinterException {
        printer = new Printer(engine, thread, format, settings, printJob);

        try {
            printer.doPrint();
        }
        finally {
            printer = null;
        }
    }

    public void cancelPrint() {
        Printer thePrinter = printer;

        if (thePrinter != null)
            thePrinter.cancelPrint();
    }

    public static PageFormat fixPageFormat(PageFormat format) {
        if (Platform.isMac()) {
            Paper paper = format.getPaper();
            paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight()); //no margins
            format.setPaper(paper);
            format = PrinterJob.getPrinterJob().validatePage(format);
        }
        return format;
    }

    private static class Printer implements Printable {
        private ImageEditorEngine engine;
        private ProgressThread thread;
        private PageFormat format;
        private PrintSettings settings;
        private PrinterJob printJob;
        private boolean printCancelled = false;
        private boolean fakeLandscape = false;

        private Point2D printOrigin = new Point2D.Double();

        private double PRINTER_RESOLUTION;

        private double printResolution;

        Printer(ImageEditorEngine engine, ProgressThread thread, PageFormat format,
                PrintSettings settings, PrinterJob printJob) {
            this.engine = engine;
            this.thread = thread;
            this.format = format;
            this.settings = settings;
            this.printJob = printJob;
            PRINTER_RESOLUTION = settings.getPixelsPerInch() / 72.0;
            printResolution = PRINTER_RESOLUTION;
        }

        void doPrint() throws PrinterException {
            printCancelled = false;

            /**
             * NOTE: Mac OS X has a bug in the landscape printing, this hack rotates te image and prints portrait anyway
             */
            int orientation = format.getOrientation();
            if (orientation != PageFormat.PORTRAIT && Platform.isMac())
                fakeLandscape = true;

            createRendering(settings, thread.getProgressIndicator());

            printImage = new Functions.sRGBWrapper(printImage);

            if (!printCancelled && !thread.isCanceled()) {
                if (fakeLandscape)
                    format.setOrientation(PageFormat.PORTRAIT);

                printJob.setPrintable(this, format);
                printJob.print();

                if (fakeLandscape) {
                    format.setOrientation(orientation);
                    fakeLandscape = false;
                }
            }
        }

        void cancelPrint() {
            if (!printCancelled) {
                printCancelled = true;
                printJob.cancel();
            }
        }

        private PlanarImage printImage = null;

        private static class PrintResolution {
            final double resolution;
            final double scale;

            PrintResolution(double resolution, double scale) {
                this.resolution = resolution;
                this.scale = scale;
            }
        }

        public static PrintResolution effectiveResolution(PrintSettings printSettings, Dimension dimension) {
            double printResolution = printSettings.getPixelsPerInch() / 72.0;

            double printScale = Math.min(printResolution * printSettings.getWidth() / dimension.getWidth(),
                                         printResolution * printSettings.getHeight() / dimension.getHeight());

            if (printScale > 1) {
                printResolution /= printScale;
                printScale = 1;
            }

            return new PrintResolution(printResolution, printScale);
        }

        private ProgressIndicator listener = null;

        public void createRendering(PrintSettings printSettings, ProgressIndicator listener) {
            this.listener = listener;

            Dimension dimension = engine.getNaturalSize();

            PrintResolution pr = effectiveResolution(printSettings, dimension);

            printResolution = pr.resolution;

            double printX = fakeLandscape ? printSettings.getY() : printSettings.getX();
            double printY = fakeLandscape ? printSettings.getX() : printSettings.getY();

            printOrigin.setLocation(printX, printY);

            System.out.println("print scale: " + pr.scale + ", print resolution: " + 72 * printResolution + " dpi");

            printImage = engine.getRendering(new Dimension((int) (pr.scale * dimension.width),
                                                           (int) (pr.scale * dimension.height)),
                                             printSettings.getColorProfile() != null
                                             ? printSettings.getColorProfile()
                                             : JAIContext.sRGBColorProfile,
                                             engine.getLCMSIntent(printSettings.getRenderingIntent()),
                                             true);

            if (printImage instanceof RenderedOp) {
                RenderedOp rop = (RenderedOp) printImage;
                rop.setRenderingHint(JAI.KEY_TILE_CACHE, JAIContext.defaultTileCache);
            }

            if (fakeLandscape) {
                ParameterBlock params = new ParameterBlock();
                params.addSource(printImage);
                params.add(TransposeDescriptor.ROTATE_90);
                printImage = JAI.create("Transpose", params, null);
            }

            if (printResolution != PRINTER_RESOLUTION) {
                double scale = PRINTER_RESOLUTION / printResolution;

                System.out.println("Uprezzing by " + scale * 100 + '%');

                AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);

                RenderingHints formatHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));

                // Do not recycle these tiles, the canvas will cache them
                // formatHints.add(new RenderingHints(JAI.KEY_CACHED_TILE_RECYCLING_ENABLED, Boolean.FALSE));

                Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
                ParameterBlock params = new ParameterBlock();
                params.addSource(printImage);
                params.add(xform);
                params.add(interp);
                printImage = JAI.create("Affine", params, formatHints);
            }

            if (!printCancelled) {
                System.out.println("print image bounds: " + printImage.getBounds());
                // JAIContext.defaultTileCache.flush();
            } else
                System.out.println("cancelled printing");
        }

        private boolean firstTime = true;

        // Implement the Printable interface
        public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
            if (pageIndex > 0) {
                listener = null;
                printImage.dispose();
                printImage = null;
                return NO_SUCH_PAGE;
            }

            System.out.println("print image bounds: " + printImage.getBounds());

            Graphics2D g2d = (Graphics2D) g;

            AffineTransform at = g2d.getTransform();
            g2d.scale(1./PRINTER_RESOLUTION, 1./PRINTER_RESOLUTION);
            g2d.translate(PRINTER_RESOLUTION * printOrigin.getX(), PRINTER_RESOLUTION * printOrigin.getY());

            g2d.setClip(printImage.getBounds());

            System.out.println("printing...");

            if (!firstTime)
                listener.setMaximum(printImage.getMaxTileX() * printImage.getMaxTileY());

            AffineTransform identity = new AffineTransform();

            // To minimize memory footprint we print one tile at a time, with a one-pixel overlap to avoid scaling artifacts

            try {
                if (!firstTime) {
                    for (int tileX = 0; !printCancelled && tileX < printImage.getNumXTiles(); tileX++)
                        for (int tileY = 0; !printCancelled && tileY < printImage.getNumYTiles(); tileY++) {
                            Raster tile = printImage.getTile(tileX, tileY);

                            BufferedImage tileImage = new BufferedImage(printImage.getColorModel(),
                                                                        (WritableRaster) tile.createTranslatedChild(0, 0),
                                                                        false, null);

                            g2d.drawRenderedImage(tileImage, AffineTransform.getTranslateInstance(tile.getMinX(),
                                                                                                  tile.getMinY()));

                            listener.incrementBy(1);
                        }
                } else
                    g2d.drawRenderedImage(printImage, identity);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!firstTime)
                listener.setIndeterminate(true);

            g2d.setTransform(at);

            System.out.println("...printed!");

            firstTime = false;

            return (PAGE_EXISTS);
        }
    }
}
