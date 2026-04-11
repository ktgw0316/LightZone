/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import com.lightcrafts.platform.PrinterLayer;
import com.lightcrafts.model.ImageEditor.ImageEditorEngine;
import com.lightcrafts.model.PrintSettings;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.jai.JAIContext;
import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.BorderExtender;
import org.eclipse.imagen.Interpolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(WindowsPrinterLayer.class);

    public void initialize() {
        WindowsPrintManager.initDefaultPrinter();
    }

    public void dispose() {
        WindowsPrintManager.dispose();
    }

    public void setPageFormat(PageFormat pageFormat) {
        WindowsPrintManager.setPageFormat(pageFormat);
        logger.debug("Setting PageFormat");
        dumpPage(pageFormat);
    }

    public PageFormat getPageFormat() {
        PageFormat pf = WindowsPrintManager.getPageFormat();
        if (pf != null) {
            logger.debug("Printer PageFormat");
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

        logger.debug("Our PageFormat");
        dumpPage(format);

        Dimension naturalSize = engine.getNaturalSize();

        logger.debug("settings x: {}, y: {}, width: {}, height: {}",
                     settings.getX(), settings.getY(), settings.getWidth(), settings.getHeight());

        logger.debug("resolution: {}", resolution);

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
            logger.debug("Uprezzing by {}%", xMagnification * 100);

            AffineTransform xform = AffineTransform.getScaleInstance(xMagnification, yMagnification);

            RenderingHints formatHints = new RenderingHints(ImageN.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));

            Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2);
            ParameterBlock params = new ParameterBlock();
            params.addSource(printImage);
            params.add(xform);
            params.add(interp);
            printImage = ImageN.create("Affine", params, formatHints);
        }

        Point location = new Point((int) (settings.getX() * resolution.getWidth() / 72.0),
                                   (int) (settings.getY() * resolution.getHeight() / 72.0));

        WindowsPrintManager.print(printImage, location, jobName != null ? jobName : "Unittled", thread );

        logger.debug("printImage: {}", printImage);

        logger.debug("location: {}", location);
    }

    public void cancelPrint() {

    }

    public static void dumpPage(PageFormat pageFormat) {
        logger.debug("page area w:{}, h: {}, o: {}",
                     pageFormat.getWidth(), pageFormat.getHeight(), pageFormat.getOrientation());

        logger.debug("imageable area x: {}, y: {}, w: {}, h: {}",
                     pageFormat.getImageableX(), pageFormat.getImageableY(),
                     pageFormat.getImageableWidth(), pageFormat.getImageableHeight());

        Paper paper = pageFormat.getPaper();

        logger.debug("paper area w:{}, h: {}", paper.getWidth(), paper.getHeight());
        logger.debug("imageable area x: {}, y: {}, w: {}, h: {}",
                     paper.getImageableX(), paper.getImageableY(),
                     paper.getImageableWidth(), paper.getImageableHeight());
    }
}
