/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.awt.color.ICC_Profile;
import java.awt.image.RenderedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.File;

import com.lightcrafts.platform.PrinterLayer;
import com.lightcrafts.model.ImageEditor.ImageEditorEngine;
import com.lightcrafts.model.PrintSettings;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.image.libs.LCTIFFWriter;
import com.lightcrafts.image.metadata.TIFFTags;
import com.lightcrafts.jai.JAIContext;

public class MacOSXPrinterLayer implements PrinterLayer {

    public static final MacOSXPrinterLayer INSTANCE = new MacOSXPrinterLayer();

    public void initialize() { }

    public void dispose() { }

    public void setPageFormat( PageFormat pageFormat ) {
        MacOSXPrinter.setPageFormat( pageFormat );
    }

    public PageFormat getPageFormat() {
        return MacOSXPrinter.getPageFormat( false );
    }

    public PageFormat pageDialog( PageFormat format ) {
        MacOSXPrinter.setPageFormat( format );
        return MacOSXPrinter.getPageFormat( true );
    }

    public boolean printDialog() {
        return MacOSXPrinter.printDialog();
    }

    public void setJobName(String name) {
        jobName = name;
    }

    public void print(ImageEditorEngine engine, ProgressThread thread, PageFormat format, PrintSettings settings) throws PrinterException {
        ICC_Profile colorProfile = settings.getColorProfile() != null
                                   ? settings.getColorProfile()
                                   : JAIContext.sRGBColorProfile;

        MacOSXPrinter.setPageFormat( format );

        // In Cocoa there is no way of really controlling the print resolution,
        // we always print at full size and then rely on the printer to do the appropriate scaling...

        RenderedImage rendering = engine.getRendering(engine.getNaturalSize(),
                                                      colorProfile,
                                                      engine.getLCMSIntent(settings.getRenderingIntent()),
                                                      true);

        try {
            File spoolFile = File.createTempFile("LZPrintSpool", "tif");
            LCTIFFWriter writer = new LCTIFFWriter(spoolFile.getAbsolutePath(),
                                                   rendering.getWidth(),
                                                   rendering.getHeight());
            // No profile for Application Managed Colors
            if (colorProfile == JAIContext.sRGBColorProfile)
                writer.setByteField( TIFFTags.TIFF_ICC_PROFILE, colorProfile.getData());
            writer.putImageStriped(rendering, thread);

            if (!thread.isCanceled())
                MacOSXPrinter.print(jobName, spoolFile, settings.getPrintBounds());

            spoolFile.delete();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void cancelPrint() {

    }

    ////////// private ////////////////////////////////////////////////////////

    private MacOSXPrinterLayer() {
        
    }

    private String jobName;
}
/* vim:set et sw=4 ts=4: */
