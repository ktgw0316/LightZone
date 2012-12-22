/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.splash;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

public class AboutDialog extends JDialog {

    private class AboutComponent extends JComponent {

        private BufferedImage image;

        AboutComponent() {
            String[] text = SplashImage.getDefaultSplashText("");
            image = new SplashImage(text);
        }

        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }

        protected void paintComponent(Graphics graphics) {
            graphics.drawImage(image, 0, 0, null);
        }
    }

    public AboutDialog(JFrame owner) {
        super(owner);
        JComponent comp = new AboutComponent();
        getContentPane().add(comp);
        setModal(true);
        setResizable(false);
        setUndecorated(true);

        comp.addMouseListener(
            new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    dispose();
                }
            }
        );
    }

    public void centerOnScreen() {
        pack();
        setLocationRelativeTo(null);
    }
}
