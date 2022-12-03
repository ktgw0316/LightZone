/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.print;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.PrinterLayer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.io.File;

public class PrintLayoutDialog extends JDialog {

    private PrintLayoutModel model;

    private PrintLayoutPanel panel;

    public PrintLayoutDialog(
        BufferedImage image, PrintLayoutModel model, Frame parent, String title
    ) {
        super(parent, title, true);
        this.model = model;
        panel = new PrintLayoutPanel(model, image);
        setContentPane(panel);
        setResizable(false);
    }

    public PageFormat getPageFormat() {
        return model.getPageFormat();
    }

    public void setPageFormat(PageFormat format) {
        model.setPageFormat(format);
    }

    public PrintLayoutModel getPrintLayout() {
        return model;
    }

    public void addPrintAction(ActionListener listener) {
        panel.addPrintAction(listener);
    }

    public void addCancelAction(ActionListener listener) {
        panel.addCancelAction(listener);
    }

    public void addDoneAction(ActionListener listener) {
        panel.addDoneAction(listener);
    }

    public void addPageSetupAction(ActionListener listener) {
        panel.addPageSetupAction(listener);
    }

    public static void main(final String[] args) throws Exception {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());

        BufferedImage image = ImageIO.read(new File(args[0]));

        int w = image.getWidth();
        int h = image.getHeight();
        final PrintLayoutDialog dialog = new PrintLayoutDialog(
            image,
            new PrintLayoutModel(w, h),
            null,
            "PrintLayoutDialog Test"
        );
        dialog.addPrintAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    System.out.println("print");
                }
            }
        );
        dialog.addPageSetupAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    PrinterLayer printer = Platform.getPlatform().getPrinterLayer();
                    PageFormat format = printer.pageDialog(null);

                    if (format != null)
                        dialog.setPageFormat(format);
                }
            }
        );
        dialog.addCancelAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    System.out.println("cancel");
                    dialog.dispose();
                }
            }
        );
        dialog.addDoneAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    System.out.println("done");
                    dialog.dispose();
                }
            }
        );
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
