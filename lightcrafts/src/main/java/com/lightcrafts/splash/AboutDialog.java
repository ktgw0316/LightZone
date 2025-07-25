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

    private static final Color m_textColor = new Color(204, 204, 204);
    private static final Font m_textFont = new Font("SansSerif", Font.PLAIN, 12);
    private static final Point m_textLoc = new Point(57, 107);

    private void drawText(String dynamicText) {
        final var splash = SplashScreen.getSplashScreen();
        final var g = splash.createGraphics();

        g.setColor(m_textColor);
        g.setFont(m_textFont);
        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        int y = m_textLoc.y;
        final var metrics = g.getFontMetrics(m_textFont);
        final int dy = -metrics.getHeight() - 5;
        String[] text = SplashImage.getDefaultSplashText(null);
        for (String s : text) {
            g.drawString(s, m_textLoc.x, y);
            y += dy;
        }
        g.drawString(dynamicText, m_textLoc.x, y);

        splash.update();
    }
}
