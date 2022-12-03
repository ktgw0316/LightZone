/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.splash;

import com.lightcrafts.platform.Platform;

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

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }

        @Override
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

        // OS X 10.11 does not detect mouse click,
        // so we need a close button
        if (!Platform.isMac()) {
            setUndecorated(true);
        }

        comp.addMouseListener(
            new MouseAdapter() {
                @Override
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
