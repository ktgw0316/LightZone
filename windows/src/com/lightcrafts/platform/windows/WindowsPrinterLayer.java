/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import com.lightcrafts.platform.PrinterLayer;
import com.lightcrafts.model.ImageEditor.ImageEditorEngine;
import com.lightcrafts.model.PrintSettings;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.jai.JAIContext;
import javax.media.jai.PlanarImage;
import javax.media.jai.JAI;
import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;

import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.Paper;
import java.awt.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.geom.AffineTransform;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Feb 6, 2007
 * Time: 3:38:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class WindowsPrinterLayer implements PrinterLayer {

    public void initialize() {
        WindowsPrintManager.initDefaultPrinter();
    }

    public void dispose() {
        WindowsPrintManager.dispose();
    }

    public void setPageFormat(PageFormat pageFormat) {
        WindowsPrintManager.setPageFormat(pageFormat);
        System.out.println("Setting PageFormat");
        dumpPage(pageFormat);
    }

    public PageFormat getPageFormat() {
        PageFormat pf = WindowsPrintManager.getPageFormat();
        if (pf != null) {
            System.out.println("Printer PageFormat");
            dumpPage(pf);
        }
        return pf;
    }

    public PageFormat pageDialog(PageFormat pageFormat) {
        WindowsPrintManager.setPageFormat(pageFormat);
        WindowsPrintManager.showPageSetupDialog(null);
        return getPageFormat();
    }

    public boolean printDialog() {
        return WindowsPrintManager.showPrintDialog(null);
    }

    private String jobName = null;
    public void setJobName(String name) {
        jobName = name;
    }

    public void print(ImageEditorEngine engine, ProgressThread thread, PageFormat format, PrintSettings settings) throws PrinterException {
        WindowsPrintManager.setPageFormat(format);
        Dimension resolution = WindowsPrintManager.getPrinterResolution();

        System.out.println("Our PageFormat");
        dumpPage(format);

        Dimension naturalSize = engine.getNaturalSize();

        System.out.println("settings x: " + settings.getX() + ", y: " + settings.getY() + ", width: " + settings.getWidth() + ", height: " + settings.getHeight());

        System.out.println("resolution: " + resolution);

        Dimension targetSize = new Dimension((int) (settings.getWidth() * resolution.getWidth() / 72.0),
                                             (int) (settings.getHeight() * resolution.getHeight() / 72.0));

        double xMagnification = targetSize.getWidth() / naturalSize.getWidth();
        double yMagnification = targetSize.getHeight() / naturalSize.getHeight();

        PlanarImage printImage = engine.getRendering(new Dimension((int) (naturalSize.width * (xMagnification < 1 ? xMagnification : 1)),
                                                                   (int) (naturalSize.height * (yMagnification < 1 ? yMagnification : 1))),
                                             settings.getColorProfile() != null
                                             ? settings.getColorProfile()
                                             : JAIContext.sRGBColorProfile,
                                             ImageEditorEngine.getLCMSIntent(settings.getRenderingIntent()),
                                             true);

        if (xMagnification > 1 || yMagnification > 1) {
            System.out.println("Uprezzing by " + xMagnification * 100 + '%');

            AffineTransform xform = AffineTransform.getScaleInstance(xMagnification, yMagnification);

            RenderingHints formatHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));

            Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
            ParameterBlock params = new ParameterBlock();
            params.addSource(printImage);
            params.add(xform);
            params.add(interp);
            printImage = JAI.create("Affine", params, formatHints);
        }

        Point location = new Point((int) (settings.getX() * resolution.getWidth() / 72.0),
                                   (int) (settings.getY() * resolution.getHeight() / 72.0));

        WindowsPrintManager.print(printImage, location, jobName != null ? jobName : "Unittled", thread );

        System.out.println("printImage: " + printImage);

        System.out.println("location: " + location);
    }

    public void cancelPrint() {

    }

    public static void dumpPage(PageFormat pageFormat) {
        System.out.println("page area w:" + pageFormat.getWidth() + ", h: " + pageFormat.getHeight() + ", o: " + pageFormat.getOrientation());

        System.out.println("imageable area x: " +
                           pageFormat.getImageableX() + ", y: " + pageFormat.getImageableY() + ", w: " +
                           pageFormat.getImageableWidth() + ", h: " + pageFormat.getImageableHeight());

        Paper paper = pageFormat.getPaper();

        System.out.println("paper area w:" + paper.getWidth() + ", h: " + paper.getHeight());
        System.out.println("imageable area x: " +
                           paper.getImageableX() + ", y: " + paper.getImageableY() + ", w: " +
                           paper.getImageableWidth() + ", h: " + paper.getImageableHeight());
    }
}
