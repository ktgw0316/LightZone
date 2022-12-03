/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx.test;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.PrinterLayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MacPageFormatTest {

    static void getPageFormat() {
        Platform platform = Platform.getPlatform();
        PrinterLayer printer = platform.getPrinterLayer();
        printer.getPageFormat();
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    JButton button = new JButton("getPageFormat()");
                    button.addActionListener(
                        new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                getPageFormat();
                            }
                        }
                    );
                    JDialog dialog = new JDialog();
                    dialog.setContentPane(button);
                    dialog.setModal(true);
                    dialog.pack();
                    dialog.setLocationRelativeTo(null);
                    dialog.setVisible(true);

                    System.out.println("setVisible() returned");
                }
            }
        );
    }
}
